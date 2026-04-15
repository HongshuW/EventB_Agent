package eventb_agent_ui.popups;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.utils.Messages;
import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.FileUploader;
import eventb_agent_core.llminteractor.RefinementStrategyPlanner;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.refinement.SystemRequirements;
import eventb_agent_core.utils.Constants;
import eventb_agent_ui.EventBAgentUIPlugin;
import eventb_agent_ui.workspaceinteractor.ModelInfo;
import eventb_agent_ui.workspaceinteractor.ModelWorkspaceInteractor;

/**
 * This class is the user interface for calling the agent to create a new
 * Event-B model.
 */
public class NewModelWizard extends Wizard implements INewWizard {

	public static final String WIZARD_ID = EventBAgentUIPlugin.AGENT_PLUGIN_ID + ".wizards.CreateMachine";

	// The wizard page.
	private NewModelWizardPage page;

	// The selection when the wizard is launched.
	private ISelection selection;

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

	private RefinementStrategyPlanner refinementStrategyPlanner;
	private ModelWorkspaceInteractor modelWorkspaceInteractor;
	private FileUploader fileUploader;

	private boolean enableRefinement;
	private boolean enableFixStrategy;
	private int maxAttemptsSynth;
	private int maxAttemptsProof;

	// Whether input form is PDF
	private boolean isPDFInput;

	private String refinementStrategyFileName = "refinement_strategy_ABZ_02_Feb";
	private int generationProgress = 1; // which refinement step should be generated next
	private String previousMachineName = "RoverMch_1.bum";
	private String previousContextName = "RoverCtx_1.buc";
	private String previousSysDesc = "Abstract mission model ensuring: never out of battery, all goals eventually visited, no collisions with obstacles. Do not use finite(...) to represent that goals will eventually be visited. Use a Boolean to record the finish status, once finishes, the set of remaining goals should be empty. The finish status should be FALSE initially. Abstract positions to be a set POS in the context, instead of coordinates.";
	private String previousReqs = "SL1\nSL2\nSL4";

	/**
	 * Constructor: This wizard needs a progress monitor.
	 */
	public NewModelWizard() {
		super();
		setNeedsProgressMonitor(true);

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		enableRefinement = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_REF, false);
		enableFixStrategy = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_FIX, false);
		maxAttemptsSynth = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_SYNTH, "5"));
		maxAttemptsProof = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_PROOF, "1"));
		isPDFInput = prefs.getBoolean(AgentPreferenceInitializer.PREF_IS_PDF_INPUT, false);

		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);

		refinementStrategyPlanner = new RefinementStrategyPlanner(llmRequestSender, llmResponseParser);
		modelWorkspaceInteractor = new ModelWorkspaceInteractor(llmRequestSender, llmResponseParser, enableFixStrategy,
				maxAttemptsSynth, maxAttemptsProof, getContainer(), getShell());
		fileUploader = new FileUploader(llmRequestSender, llmResponseParser);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		page = new NewModelWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will
	 * create an operation and run it using wizard as execution context.
	 * <p>
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		return performFinish(isPDFInput);
	}

	private JSONArray getInitialRefinementStrategy(String fileID, String sysDesc) {
		// refinement steps
		JSONArray refinementSteps = new JSONArray();

		try {
			if (refinementStrategyPlanner.hasRefinementStrategy(refinementStrategyFileName)) {
				refinementSteps = refinementStrategyPlanner.readRefinementStrategyFromFile(refinementStrategyFileName);
			} else {
				// TODO: fix this later (enable refinement)
				refinementSteps = isPDFInput ? refinementStrategyPlanner.getRefinementStepsWithFile(fileID)
						: refinementStrategyPlanner.getRefinementSteps(sysDesc);
				// save refinement plan to a file
				refinementStrategyPlanner.saveRefinementStrategyToFile(refinementSteps.toString(2),
						refinementStrategyFileName);
			}
		} catch (ReachMaxAttemptException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}

		return refinementSteps;
	}

	private JSONArray readRefinementStrategy() {
		JSONArray refinementSteps = new JSONArray();
		try {
			refinementSteps = refinementStrategyPlanner.readRefinementStrategyFromFile(refinementStrategyFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ScrollableMessageDialog popUp = new ScrollableMessageDialog(getShell(), "Refinement Strategy",
				refinementSteps.toString(2));
		popUp.open();

		String editedRefinementStrategy = popUp.getEditedContent();
		try {
			refinementStrategyPlanner.saveRefinementStrategyToFile(editedRefinementStrategy,
					refinementStrategyFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return refinementSteps;
	}

	private boolean performFinish(boolean isPDFInput) {
		String fileID = null;
		if (isPDFInput) {
			fileID = fileUploader.uploadFile(SystemRequirements.INPUT_PDF_PATH);
		}

		final String sysDesc = page.getSystemDesc();

		JSONArray refinementSteps = getInitialRefinementStrategy(fileID, sysDesc);

		// create models
		ModelInfo previousModel = null;
		for (int i = 0; i < refinementSteps.length(); i++) {
			if (i < generationProgress) {
				continue;
			}

			refinementSteps = readRefinementStrategy();

			JSONObject refStepJSON = refinementSteps.getJSONObject(i);
			RefinementStep refinementStep = llmResponseParser.getRefinementStep(refStepJSON, page.getSystemReqs());

			final String projectName = page.getProjectName();
			try {
				if (generationProgress != 0 && previousModel == null) {
					previousModel = new ModelInfo(previousContextName, previousMachineName, previousSysDesc, previousReqs);
				}
				previousModel = modelWorkspaceInteractor.createModel(projectName, refinementStep, previousModel,
						fileID);

				String message = "Covered Description:\n" + previousModel.getSystemDescription();
				message += "\n\nCovered Requirements:\n" + previousModel.getSystemRequirement();

//				ScrollableMessageDialog popUp = new ScrollableMessageDialog(getShell(), "Formal Model", message);
//				popUp.open();
			} catch (InterruptedException e) {
				return false;
			} catch (InvocationTargetException e) {
				Throwable realException = e.getTargetException();
				UIUtils.showError(Messages.title_error, realException.getMessage());
				return false;
			} catch (CoreException e) {
				return false;
			} catch (ReachMaxAttemptException e) {
				System.out.println(e);
				return false;
			}
		}
		return true;
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * <p>
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 *      org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection sel) {
		this.selection = sel;
	}

}
