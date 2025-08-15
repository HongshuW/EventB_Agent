package eventb_agent_ui.popups;

import java.lang.reflect.InvocationTargetException;
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
import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.RefinementStrategyPlanner;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.utils.Constants;
import eventb_agent_ui.EventBAgentUIPlugin;
import eventb_agent_ui.exceptions.ReachMaxAttemptException;
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

	private boolean enableFixStrategy;
	private int maxAttempts;

	/**
	 * Constructor: This wizard needs a progress monitor.
	 */
	public NewModelWizard() {
		super();
		setNeedsProgressMonitor(true);

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		enableFixStrategy = prefs.getBoolean(AgentPreferenceInitializer.PREF_ENABLE_FIX, false);
		maxAttempts = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS, "5"));

		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);

		refinementStrategyPlanner = new RefinementStrategyPlanner(llmRequestSender, llmResponseParser);
		modelWorkspaceInteractor = new ModelWorkspaceInteractor(llmRequestSender, llmResponseParser, enableFixStrategy,
				maxAttempts, getContainer(), getShell());
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
		final String sysDesc = page.getSystemDesc();

		// refinement steps
		JSONArray refinementSteps = refinementStrategyPlanner.getRefinementSteps(sysDesc);

		// create models
		ModelInfo previousModel = null;
		for (int i = 0; i < refinementSteps.length(); i++) {
			JSONObject refStepJSON = refinementSteps.getJSONObject(i);
			RefinementStep refinementStep = llmResponseParser.getRefinementStep(refStepJSON);

			final String projectName = page.getProjectName();
			try {
				previousModel = modelWorkspaceInteractor.createModel(projectName, refinementStep, previousModel);
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
