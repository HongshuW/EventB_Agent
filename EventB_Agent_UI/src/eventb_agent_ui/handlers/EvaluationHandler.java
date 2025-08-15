package eventb_agent_ui.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.utils.Messages;
import org.json.JSONArray;
import org.json.JSONObject;

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
	private boolean enableRefinement;
	private boolean enableFixStrategy;

	public EvaluationHandler() {
		super();

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);

		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);

		datasetPath = prefs.get(AgentPreferenceInitializer.PREF_DATASET_LOC, "");
		enableRefinement = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_REF, false);
		enableFixStrategy = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_FIX, false);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("==========\nEvaluating Event-B Agent\nDataset Path:" + datasetPath + "\nEnable Refinement:"
				+ String.valueOf(enableRefinement) + "\nEnable Fix Strategy:" + String.valueOf(enableFixStrategy)
				+ "\n==========");

		File datasetFolder = new File(datasetPath);
		if (!datasetFolder.exists() || !datasetFolder.isDirectory()) {
			System.out.println("Invalid dataset folder path: " + datasetPath + "\nPlease specify a valid folder.");
			return null;
		}

		RefinementStrategyPlanner refinementStrategyPlanner = new RefinementStrategyPlanner(llmRequestSender,
				llmResponseParser);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		ModelWorkspaceInteractor modelWorkspaceInteractor = new ModelWorkspaceInteractor(llmRequestSender,
				llmResponseParser, enableFixStrategy, window, null);

		File[] files = datasetFolder.listFiles();
		if (files != null) {
			for (File file : files) {
				final String projectName = file.getName().split(".json")[0];
				// refinement steps
				SystemRequirements systemReqs = new SystemRequirements(file.toPath());
				JSONArray refinementSteps = new JSONArray();
				if (enableRefinement) {
					refinementSteps = refinementStrategyPlanner.getRefinementSteps(systemReqs.toString());
				} else {
					refinementSteps = refinementStrategyPlanner.getSingleRefinementStep(systemReqs.toString());
				}

				// create models
				ModelInfo previousModel = null;
				for (int i = 0; i < refinementSteps.length(); i++) {
					JSONObject refStepJSON = refinementSteps.getJSONObject(i);
					RefinementStep refinementStep = llmResponseParser.getRefinementStep(refStepJSON);

					try {
						previousModel = modelWorkspaceInteractor.createModel(projectName, refinementStep,
								previousModel);
					} catch (InterruptedException e) {
						return null;
					} catch (InvocationTargetException e) {
						Throwable realException = e.getTargetException();
						UIUtils.showError(Messages.title_error, realException.getMessage());
						return null;
					} catch (CoreException e) {
						return null;
					}
				}
			}
		}

		return null;
	}

}
