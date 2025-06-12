package eventb_agent_ui.wizards;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IMachineRoot;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.utils.Messages;
import org.eventb.ui.EventBUIPlugin;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinDB;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_ui.EventBAgentUIPlugin;
import eventb_agent_ui.utils.CreateMachineUtils;
import eventb_agent_ui.utils.JSONUtils;

public class CreateMachineWizard extends Wizard implements INewWizard {

	public static final String WIZARD_ID = EventBAgentUIPlugin.AGENT_PLUGIN_ID + ".wizards.CreateMachine";

	// The wizard page.
	private CreateMachineWizardPage page;

	// The selection when the wizard is launched.
	private ISelection selection;

	private LLMResponseParser parser;

	/**
	 * Constructor: This wizard needs a progress monitor.
	 */
	public CreateMachineWizard() {
		super();
		setNeedsProgressMonitor(true);
		parser = new LLMResponseParser();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		page = new CreateMachineWizardPage(selection);
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
		final String projectName = page.getProjectName();
		final String prompt = page.getPrompt();

		JSONObject response = getLLMResponse(prompt);
//		JSONObject response = JSONUtils.readJSON("C:\\Users\\admin\\repos\\EventB_Agent\\resources\\exampleResponse.json");

		final String contextFileName = parser.getContextName(response) + "." + page.getContextFileType();
		final String machineFileName = parser.getMachineName(response) + "." + page.getMachineFileType();
		JSONObject contextJSON = parser.getContextJSON(response);
		JSONObject machineJSON = parser.getMachineJSON(response);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(projectName, contextFileName, monitor, contextJSON);
					doFinish(projectName, machineFileName, monitor, machineJSON);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			UIUtils.showError(Messages.title_error, realException.getMessage());
			return false;
		}
		return true;
	}

	private JSONObject getLLMResponse(String prompt) {
		LLMRequestSender llmRequestSender = new LLMRequestSender();

		String response;
		try {
			response = llmRequestSender.sendRequest(prompt);
			JSONObject obj = new JSONObject(response);
			String answer = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

			JSONObject answerJson = new JSONObject(answer);

			return answerJson;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * The worker method. It will find the project, create the file, and open the
	 * editor on the newly created file.
	 * <p>
	 * 
	 * @param projectName the name of the project
	 * @param fileName    the name of the file
	 * @param monitor     a progress monitor
	 * @throws CoreException a core exception when creating the new file
	 */
	void doFinish(String projectName, final String fileName, IProgressMonitor monitor, JSONObject json)
			throws CoreException {

		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(projectName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Project \"" + projectName + "\" does not exist.");
		}

		IRodinDB db = EventBUIPlugin.getRodinDatabase();
		// Creating a project handle
		final IRodinProject rodinProject = db.getRodinProject(projectName);

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pMonitor) throws CoreException {
				final IRodinFile rodinFile = rodinProject.getRodinFile(fileName);
				rodinFile.create(false, pMonitor);
				final IInternalElement rodinRoot = rodinFile.getRoot();
				((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, pMonitor);
				if (rodinRoot instanceof IMachineRoot) {
					/* machine */
					CreateMachineUtils.initiateMachine(rodinRoot, pMonitor, parser, json);
				} else {
					/* context */
					CreateMachineUtils.initiateContext(rodinRoot, pMonitor, parser, json);
				}
				rodinFile.save(null, true);
			}

		}, monitor);

		monitor.worked(1);

		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				UIUtils.linkToEventBEditor(rodinProject.getRodinFile(fileName));
			}
		});
		monitor.worked(1);
	}

	/**
	 * Throw a Core exception.
	 * <p>
	 * 
	 * @param message The message for displaying
	 * @throws CoreException a Core exception with the status contains the input
	 *                       message
	 */
	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "org.eventb.internal.ui", IStatus.OK, message, null);
		throw new CoreException(status);
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
