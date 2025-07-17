package eventb_agent_ui.wizards;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IVariable;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.utils.Messages;
import org.eventb.ui.EventBUIPlugin;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinDB;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;
import eventb_agent_ui.EventBAgentUIPlugin;
import eventb_agent_ui.utils.CompilationErrorType;
import eventb_agent_ui.utils.CreateModelUtils;
import eventb_agent_ui.utils.ErrorTypeUtils;

public class NewModelWizard extends Wizard implements INewWizard {

	public static final String WIZARD_ID = EventBAgentUIPlugin.AGENT_PLUGIN_ID + ".wizards.CreateMachine";

	// The wizard page.
	private NewModelWizardPage page;

	// The selection when the wizard is launched.
	private ISelection selection;

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

	/**
	 * Constructor: This wizard needs a progress monitor.
	 */
	public NewModelWizard() {
		super();
		setNeedsProgressMonitor(true);

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);
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
		final String projectName = page.getProjectName();
		final String prompt = page.getPrompt();
		final String sysDesc = page.getSystemDesc();

		JSONObject response = getLLMResponse(prompt, sysDesc);
//		java.nio.file.Path path = Paths.get(FileUtils.getAgentDirectoryPath(), "resources", "gemini_cart.json");
//		JSONObject response = FileUtils.readJSON(path);

		final String contextFileName = llmResponseParser.getContextName(response) + "." + page.getContextFileType();
		final String machineFileName = llmResponseParser.getMachineName(response) + "." + page.getMachineFileType();
		JSONObject contextJSON = llmResponseParser.getContextJSON(response);
		JSONObject machineJSON = llmResponseParser.getMachineJSON(response);

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

		// TODO: add this later for compilation error
//		IRunnableWithProgress op1 = new IRunnableWithProgress() {
//			@Override
//			public void run(IProgressMonitor monitor) throws InvocationTargetException {
//				try {
//					solveCompilationErrors(projectName, machineFileName, monitor);
//				} catch (CoreException e) {
//					throw new InvocationTargetException(e);
//				} finally {
//					monitor.done();
//				}
//			}
//		};
//		try {
//			getContainer().run(true, false, op1);
//		} catch (InterruptedException e) {
//			return false;
//		} catch (InvocationTargetException e) {
//			Throwable realException = e.getTargetException();
//			UIUtils.showError(Messages.title_error, realException.getMessage());
//			return false;
//		}
		return true;
	}

	private JSONObject getLLMResponse(String prompt, String systemDesc) {
		String response;
		try {
			response = llmRequestSender.sendRequest(prompt, new String[] { systemDesc },
					new LLMRequestTypes[] { LLMRequestTypes.SYNTHESIS });
			return llmResponseParser.getResponseContent(response);
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
	private void doFinish(String projectName, final String fileName, IProgressMonitor monitor, JSONObject json)
			throws CoreException {

		monitor.beginTask("Creating " + fileName, 2);
		IRodinProject rodinProject = getRodinProject(projectName);

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pMonitor) throws CoreException {
				final IRodinFile rodinFile = rodinProject.getRodinFile(fileName);
				rodinFile.create(false, pMonitor);
				final IInternalElement rodinRoot = rodinFile.getRoot();
				((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, pMonitor);
				if (rodinRoot instanceof IMachineRoot) {
					/* machine */
					CreateModelUtils.initiateMachine(rodinRoot, pMonitor, llmResponseParser, json);
				} else {
					/* context */
					CreateModelUtils.initiateContext(rodinRoot, pMonitor, llmResponseParser, json);
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

	private void solveCompilationErrors(String projectName, final String fileName, IProgressMonitor monitor)
			throws CoreException {

		monitor.beginTask("Fixing compilation errors in " + fileName, 2);
		IRodinProject rodinProject = getRodinProject(projectName);

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pMonitor) throws CoreException {
				final IRodinFile rodinFile = rodinProject.getRodinFile(fileName);
				final IInternalElement rodinRoot = rodinFile.getRoot();
				((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, pMonitor);

				IResource resource = rodinRoot.getResource();
				IMarker[] markers = resource.findMarkers("org.rodinp.core.problem", true, IResource.DEPTH_INFINITE); // org.rodinp.core.problem
				for (IMarker marker : markers) {
					int severity = marker.getAttribute(IMarker.SEVERITY, -1);
					if (severity == IMarker.SEVERITY_ERROR) {
						String message = (String) marker.getAttribute(IMarker.MESSAGE);
						int charStart = marker.getAttribute(IMarker.CHAR_START, -1);
						int charEnd = marker.getAttribute(IMarker.CHAR_END, -1);
						int lineNo = marker.getAttribute(IMarker.LINE_NUMBER, -1);

						System.out.println("Error: " + message);
						System.out.println("Char Start: " + charStart);
						System.out.println("Char End: " + charEnd);
						System.out.println("Line Number: " + lineNo);

						String handle = (String) marker.getAttribute("element", null);
						System.out.println(handle);
						if (handle != null) {
							IRodinElement element = RodinCore.valueOf(handle);
							fixCompilationError(element, message, resource);
						}
					}
				}

				rodinFile.save(null, true);
			}

		}, monitor);

		monitor.worked(1);
	}

	private IRodinProject getRodinProject(String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(projectName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Project \"" + projectName + "\" does not exist.");
		}

		IRodinDB db = EventBUIPlugin.getRodinDatabase();
		// Creating a project handle
		final IRodinProject rodinProject = db.getRodinProject(projectName);

		return rodinProject;
	}

	private void fixCompilationError(IRodinElement element, String errorMessage, IResource resource)
			throws CoreException {
		CompilationErrorType errorType = ErrorTypeUtils.getErrorType(errorMessage);
		if (errorType == CompilationErrorType.TYPE_MISSING) {
			if (element instanceof IVariable) {
				IVariable var = (IVariable) element;
				String identifierName = var.getIdentifierString();
				System.out.println(identifierName);
			}
		}
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
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
