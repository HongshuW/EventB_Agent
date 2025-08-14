package eventb_agent_ui.workspaceinteractor;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.internal.ui.UIUtils;
import org.eventb.ui.EventBUIPlugin;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinDB;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.CompilationErrorFixer;
import eventb_agent_core.llminteractor.ModelCreator;
import eventb_agent_core.llminteractor.POFixer;
import eventb_agent_core.proof.POManager;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_ui.utils.CreateModelUtils;

public class ModelWorkspaceInteractor {

	private static String machineFileType = "bum";
	private static String contextFileType = "buc";

	private IRunnableContext runnableContext;
	private Shell shell;

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

	private ModelCreator modelCreator;

	public ModelWorkspaceInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser,
			IRunnableContext runnableContext) {
		this(llmRequestSender, llmResponseParser, runnableContext, null);
	}

	public ModelWorkspaceInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser,
			IRunnableContext runnableContext, Shell shell) {
		this.llmRequestSender = llmRequestSender;
		this.llmResponseParser = llmResponseParser;

		this.runnableContext = runnableContext;
		this.shell = shell;

		this.modelCreator = new ModelCreator(llmRequestSender, llmResponseParser);
	}

	/**
	 * Create model, save to workspace and return its information
	 * 
	 * @param projectName
	 * @param refinementStep
	 * @param isAbstractModel
	 * @return
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 * @throws CoreException
	 */
	public ModelInfo createModel(String projectName, RefinementStep refinementStep, ModelInfo previousModel)
			throws InvocationTargetException, InterruptedException, CoreException {
		String[] fileNames = new String[2];
		String sysDesc = previousModel == null ? "" : previousModel.getSystemDescription();
		String newSysDesc = refinementStep.getModelDesc();

		if (previousModel == null) {
			// synthesize abstract model
			JSONObject response = modelCreator.synthesizeModel(refinementStep);
			fileNames = saveModel(projectName, response);
			fixCompilationErrors(projectName, fileNames);
			fixPOs(projectName, fileNames);
			sysDesc = newSysDesc;
		} else {
			// refine the previous model
			JSONObject response = modelCreator.refineModel(projectName, fileNames, sysDesc, refinementStep);
			fileNames = saveModel(projectName, response);
			fixCompilationErrors(projectName, fileNames);
			fixPOs(projectName, fileNames);
			sysDesc += "\n" + newSysDesc;
		}

		return new ModelInfo(fileNames[0], fileNames[1], sysDesc);
	}

	public String[] saveModel(String projectName, JSONObject response)
			throws InvocationTargetException, InterruptedException {
		final String contextFileName = llmResponseParser.getContextName(response) + "." + contextFileType;
		final String machineFileName = llmResponseParser.getMachineName(response) + "." + machineFileType;
		JSONObject contextJSON = llmResponseParser.getContextJSON(response);
		JSONObject machineJSON = llmResponseParser.getMachineJSON(response);

		// save model
		IRunnableWithProgress saveModelOperation = new IRunnableWithProgress() {
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
		runnableContext.run(false, false, saveModelOperation);

		return new String[] { contextFileName, machineFileName };
	}

	private String[] fixCompilationErrors(String projectName, String[] fileNames)
			throws InvocationTargetException, InterruptedException {
		final String contextFileName = fileNames[0];
		final String machineFileName = fileNames[1];

		// fix compilation errors
		IRunnableWithProgress fixCompilationErrorOperation = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					CompilationErrorFixer compilationErrorFixer = new CompilationErrorFixer(llmRequestSender,
							llmResponseParser);
					JSONObject newModel = compilationErrorFixer.solveCompilationErrors(projectName, machineFileName,
							contextFileName, monitor);
					if (newModel != null) {
						String[] newFileNames = saveModel(projectName, newModel);
						fixCompilationErrors(projectName, newFileNames);
					} else {
						// save file without modification
						IRodinProject rodinProject = getRodinProject(projectName);
						final IRodinFile rodinFile = rodinProject.getRodinFile(machineFileName);
						rodinFile.save(monitor, true);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, fixCompilationErrorOperation);

		return new String[] { contextFileName, machineFileName };
	}

	private String[] fixPOs(String projectName, String[] fileNames)
			throws InvocationTargetException, InterruptedException {

		final String contextFileName = fileNames[0];
		final String machineFileName = fileNames[1];

		// fix POs
		IRunnableWithProgress fixPOsOperation = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					POManager poManager = new POManager();
					POFixer poFixer = new POFixer(llmRequestSender, llmResponseParser);

					IRodinProject rodinProject = getRodinProject(projectName);
					final IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();
					List<IPOSequent> pos = poManager.getOpenPOs(machineRoot);
					for (IPOSequent po : pos) {
						poFixer.autoFixPO(machineRoot, po);
					}
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, fixPOsOperation);

		return new String[] { contextFileName, machineFileName };
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
				rodinFile.create(true, pMonitor);
				final IInternalElement rodinRoot = rodinFile.getRoot();
				((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, pMonitor);
				if (rodinRoot instanceof IMachineRoot) {
					/* machine */
					CreateModelUtils.initiateMachine(rodinRoot, pMonitor, llmResponseParser, json);
				} else {
					/* context */
					CreateModelUtils.initiateContext(rodinRoot, pMonitor, llmResponseParser, json);
				}
				rodinFile.save(pMonitor, true);
			}

		}, monitor);

		monitor.worked(1);

		if (enableDisplay()) {
			monitor.setTaskName("Opening file for editing...");
			shell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					UIUtils.linkToEventBEditor(rodinProject.getRodinFile(fileName));
				}
			});
			monitor.worked(1);
		}

	}

	private IRodinProject getRodinProject(String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		createRodinProject(root.getWorkspace(), root, projectName, null);
		IResource resource = root.findMember(new Path(projectName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Project \"" + projectName + "\" does not exist.");
		}

		IRodinDB db = EventBUIPlugin.getRodinDatabase();
		// Creating a project handle
		final IRodinProject rodinProject = db.getRodinProject(projectName);

		return rodinProject;
	}

	private IRodinProject createRodinProject(IWorkspace ws, IWorkspaceRoot root, String name, IProgressMonitor mon)
			throws CoreException {

		IProject p = root.getProject(name);
		if (p.exists()) {
			return org.rodinp.core.RodinCore.valueOf(p);
		}

		p.create(mon);
		p.open(mon);

		// Ensure the Rodin nature is present so the Rodin/Event-B builders run
		IProjectDescription desc = p.getDescription();
		java.util.List<String> natures = new java.util.ArrayList<>();
		for (String nid : desc.getNatureIds())
			natures.add(nid);
		if (!natures.contains(org.rodinp.core.RodinCore.NATURE_ID)) {
			natures.add(org.rodinp.core.RodinCore.NATURE_ID);
			desc.setNatureIds(natures.toArray(String[]::new));
			p.setDescription(desc, mon);
		}

		return org.rodinp.core.RodinCore.valueOf(p);
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

	private boolean enableDisplay() {
		return this.shell != null;
	}

}
