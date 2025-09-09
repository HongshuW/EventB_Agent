package eventb_agent_ui.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.evaluation.ComponentType;
import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.RefinementStrategyPlanner;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.refinement.SystemRequirements;
import eventb_agent_core.utils.Constants;
import eventb_agent_ui.workspaceinteractor.ModelInfo;
import eventb_agent_ui.workspaceinteractor.ModelWorkspaceInteractor;

/**
 * This class runs the experiments for Event-B Agent.
 */
public class EvaluationHandler extends AbstractHandler implements IHandler {

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

	private String datasetPath;
	private String resultsPath;
	private boolean enableRefinement;
	private boolean enableFixStrategy;
	private int maxAttemptsSynth;
	private int maxAttemptsProof;

	private List<String> visitedProjects;

	public EvaluationHandler() {
		super();

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);

		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);

		datasetPath = prefs.get(AgentPreferenceInitializer.PREF_DATASET_LOC, "");
		resultsPath = prefs.get(AgentPreferenceInitializer.PREF_RESULTS_LOC, "");
		enableRefinement = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_REF, false);
		enableFixStrategy = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_FIX, false);
		maxAttemptsSynth = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_SYNTH, "5"));
		maxAttemptsProof = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_PROOF, "1"));

		visitedProjects = new ArrayList<>();
	}

	private void getVisitedProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			visitedProjects.add(project.getName());
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("==========\nEvaluating Event-B Agent\nDataset Path:" + datasetPath + "\nEnable Refinement:"
				+ String.valueOf(enableRefinement) + "\nEnable Fix Strategy:" + String.valueOf(enableFixStrategy)
				+ "\nMaximum Allowed Attempts for Synthesis:" + String.valueOf(maxAttemptsSynth)
				+ "\nMaximum Allowed Attempts for Proof:" + String.valueOf(maxAttemptsProof) + "\n==========");

		File datasetFolder = new File(datasetPath);
		if (!datasetFolder.exists() || !datasetFolder.isDirectory()) {
			System.out.println("Invalid dataset folder path: " + datasetPath + "\nPlease specify a valid folder.");
			return null;
		}

		getVisitedProjects();

		Job job = Job.create("Event-B Agent", (IProgressMonitor monitor) -> {

			File[] files = datasetFolder.listFiles();
			if (files != null) {
				for (File file : files) {

					EvaluationManager.resetDefaultInstance();
					EvaluationManager.startTimer();

					IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);

					final String projectName = file.getName().split(".json")[0];
					if (visitedProjects.contains(projectName)) {
						continue;
					}
					System.out.println("==========\nEvaluating `" + projectName + "`\n==========\n");

					/* refinement */
					EvaluationManager.addAndStartNewAction(ComponentType.REFINE, 0);

					RefinementStrategyPlanner refinementStrategyPlanner = new RefinementStrategyPlanner(
							llmRequestSender, llmResponseParser);
					SystemRequirements systemReqs = new SystemRequirements(file.toPath());
					JSONArray refinementSteps = new JSONArray();
					try {
						if (enableRefinement) {
							refinementSteps = refinementStrategyPlanner.getRefinementSteps(systemReqs.toString());
						} else {
							refinementSteps = refinementStrategyPlanner.getSingleRefinementStep(systemReqs.toString());
						}
					} catch (ReachMaxAttemptException e) {
						EvaluationManager.setErrorToLatestAction(e.getMessage());
					}

					EvaluationManager.endLatestAction();

					/* synthesis and repair loop */
					ModelInfo previousModel = null;
					for (int i = 0; i < refinementSteps.length(); i++) {
						JSONObject refStepJSON = refinementSteps.getJSONObject(i);
						RefinementStep refinementStep = llmResponseParser.getRefinementStep(refStepJSON, systemReqs);

						try {
							ModelWorkspaceInteractor modelWorkspaceInteractor = new ModelWorkspaceInteractor(
									llmRequestSender, llmResponseParser, enableFixStrategy, maxAttemptsSynth,
									maxAttemptsProof, window);
							previousModel = modelWorkspaceInteractor.createModel(projectName, refinementStep,
									previousModel);
						} catch (ReachMaxAttemptException e) {
							e.printStackTrace();
							EvaluationManager
									.setErrorToLatestAction(e.getMessage() == null ? e.toString() : e.getMessage());
							EvaluationManager.endLatestAction();
						} catch (Exception e) {
							e.printStackTrace();
							EvaluationManager
									.setErrorToLatestAction(e.getMessage() == null ? e.toString() : e.getMessage());
							EvaluationManager.endLatestAction();
							i--; // retry
						}
					}

					EvaluationManager.endTimer();
					EvaluationManager.write(resultsPath, projectName);
				}
			}
		});

		// hide all “job failed” prompts
		job.setUser(false);
		job.setSystem(true);
		job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);

		job.schedule();

		return null;
	}

}
