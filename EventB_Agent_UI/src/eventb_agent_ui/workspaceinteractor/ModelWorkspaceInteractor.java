package eventb_agent_ui.workspaceinteractor;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.IPSRoot;
import org.eventb.core.pm.IProofComponent;
import org.eventb.internal.core.pm.ProofManager;
import org.eventb.internal.ui.UIUtils;
import org.eventb.ui.EventBUIPlugin;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinDB;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import eventb_agent_core.evaluation.ComponentType;
import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.CompilationErrorFixer;
import eventb_agent_core.llminteractor.ModelCheckingFixer;
import eventb_agent_core.llminteractor.ModelCreator;
import eventb_agent_core.llminteractor.POFixer;
import eventb_agent_core.proof.POManager;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.utils.RodinUtils;
import eventb_agent_core.utils.proof.ProofUtils;
import eventb_agent_ui.utils.CreateModelUtils;

public class ModelWorkspaceInteractor {

	private static String machineFileType = "bum";
	private static String contextFileType = "buc";
	private static String MARKER_EVENTB = "org.eventb.core.problem";
	private static String MARKER_RODIN = "org.rodinp.core.problem";

	private IRunnableContext runnableContext;
	private Shell shell;

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

	private ModelCreator modelCreator;

	private boolean enableFixStrategy;
	private int maxAttemptsSynth;
	private int maxAttemptsProof;

	private Set<String> visitedPOs;

	public ModelWorkspaceInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser,
			boolean enableFixStrategy, int maxAttemptsSynth, int maxAttemptsProof, IRunnableContext runnableContext) {
		this(llmRequestSender, llmResponseParser, enableFixStrategy, maxAttemptsSynth, maxAttemptsProof,
				runnableContext, null);
	}

	public ModelWorkspaceInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser,
			boolean enableFixStrategy, int maxAttemptsSynth, int maxAttemptsProof, IRunnableContext runnableContext,
			Shell shell) {
		this.llmRequestSender = llmRequestSender;
		this.llmResponseParser = llmResponseParser;

		this.runnableContext = runnableContext;
		this.shell = shell;

		this.modelCreator = new ModelCreator(llmRequestSender, llmResponseParser);

		this.enableFixStrategy = enableFixStrategy;
		this.maxAttemptsSynth = maxAttemptsSynth;
		this.maxAttemptsProof = maxAttemptsProof;

		this.visitedPOs = new HashSet<>();
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
	 * @throws ReachMaxAttemptException
	 */
	public ModelInfo createModel(String projectName, RefinementStep refinementStep, ModelInfo previousModel)
			throws InvocationTargetException, InterruptedException, CoreException, ReachMaxAttemptException {
		EvaluationManager.addAndStartNewAction(ComponentType.SYNTHESIS, 0);

		String[] fileNames = new String[2];
		String sysDesc = "";
		if (previousModel != null) {
			sysDesc = previousModel.getSystemDescription();
			fileNames[0] = previousModel.getContextFileName();
			fileNames[1] = previousModel.getMachineFileName();
		}
		String newSysDesc = refinementStep.getModelDesc();

		if (previousModel == null) {
			// synthesize abstract model
			JSONObject response = new JSONObject();
			try {
				response = modelCreator.synthesizeModel(refinementStep);
			} catch (ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
				EvaluationManager.setErrorToLatestAction(e.getMessage());
			}
			fileNames = saveModel(projectName, response);
			EvaluationManager.endLatestAction();

			fixCompilationErrors(projectName, fileNames);
			fixBasedOnModelCheckingResults(projectName, fileNames);

			runAutoProvers(projectName, fileNames);
			fixPOs(projectName, fileNames, null);
			sysDesc = newSysDesc;
		} else {
			// refine the previous model
			JSONObject response = new JSONObject();
			try {
				response = modelCreator.refineModel(projectName, fileNames, sysDesc, refinementStep);
			} catch (ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
				EvaluationManager.setErrorToLatestAction(e.getMessage());
			}
			fileNames = saveModel(projectName, response);
			EvaluationManager.endLatestAction();

			fixCompilationErrors(projectName, fileNames);
			fixBasedOnModelCheckingResults(projectName, fileNames);

			runAutoProvers(projectName, fileNames);
			fixPOs(projectName, fileNames, null);
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
				// context
				try {
					doFinish(projectName, contextFileName, monitor, contextJSON);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}

				// machine
				try {
					doFinish(projectName, machineFileName, monitor, machineJSON);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}

				// save and wait for compilation error markers
				try {
					IRodinProject rodinProject = getRodinProject(projectName);
					IRodinFile contextFile = rodinProject.getRodinFile(contextFileName);
					buildAndWaitForMarkers(contextFile.getResource(), monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
				try {
					IRodinProject rodinProject = getRodinProject(projectName);
					IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					buildAndWaitForMarkers(machineFile.getResource(), monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, saveModelOperation);

		return new String[] { contextFileName, machineFileName };
	}

	private void buildAndWaitForMarkers(IFile rodinFile, IProgressMonitor mon)
			throws CoreException, InterruptedException {

		// 1) Ensure the file exists and is saved
		rodinFile.refreshLocal(IResource.DEPTH_ZERO, mon);

		// 2) Set up a latch that releases when marker deltas arrive for this file
		CountDownLatch latch = new CountDownLatch(1);
		IResourceChangeListener listener = new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getType() != IResourceChangeEvent.POST_BUILD) {
					return;
				}

				IMarkerDelta[] deltasEB = event.findMarkerDeltas(MARKER_EVENTB, true);
				IMarkerDelta[] deltasRodin = event.findMarkerDeltas(MARKER_RODIN, true);

				if (hasDeltaFor(rodinFile, deltasEB) || hasDeltaFor(rodinFile, deltasRodin)) {
					latch.countDown();
				}
			}

			private boolean hasDeltaFor(IFile file, IMarkerDelta[] deltas) {
				if (deltas == null)
					return false;
				for (IMarkerDelta d : deltas) {
					if (file.equals(d.getResource()))
						return true;
				}
				return false;
			}
		};

		// 3) Register listener
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_BUILD);

		try {
			// 4) Trigger the Rodin/Eclipse builders (this schedules the Event-B static
			// checker)
			rodinFile.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, mon);

			// 5) Wait for marker creation/update (with timeout as a safety net)
			latch.await(100, TimeUnit.MILLISECONDS);

		} finally {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		}
	}

	private void buildAndWaitForPOs(IRodinFile rFile, IProgressMonitor monitor)
			throws CoreException, InterruptedException {

		IFile src = rFile.getResource();

		// Compute sibling .bpo / .bps files
		IPath base = src.getFullPath().removeFileExtension();
		IFile bpo = ResourcesPlugin.getWorkspace().getRoot().getFile(base.addFileExtension("bpo"));
		IFile bps = ResourcesPlugin.getWorkspace().getRoot().getFile(base.addFileExtension("bps"));

		// 1) Wait for .bpo to be ADDED/CHANGED
		CountDownLatch poLatch = new CountDownLatch(1);
		IResourceChangeListener poListener = event -> {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE)
				return;
			IResourceDelta root = event.getDelta();
			if (root == null)
				return;
			try {
				root.accept(delta -> {
					IResource r = delta.getResource();
					if (bpo.equals(r)
							&& (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED)) {
						poLatch.countDown();
					}
					return true;
				});
			} catch (CoreException e) {
				poLatch.countDown();
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(poListener, IResourceChangeEvent.POST_CHANGE);
		try {
			// Kick builders (static checker → POG)
			src.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			// Fast-path: already exists?
			if (!bpo.exists()) {
				poLatch.await(100, TimeUnit.MILLISECONDS);
			}
			bpo.refreshLocal(IResource.DEPTH_ZERO, monitor);
		} finally {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(poListener);
		}

		// 2) Ensure .bps exists by touching the proof component (lazy-creates .bps)
		try {
			IProofComponent pc = ProofManager.getDefault().getProofComponent((IEventBRoot) rFile.getRoot());
			IPSRoot psRoot = pc.getPSRoot(); // this guarantees .bps on disk
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private int getNextAttemptID(ComponentType currentType) throws ReachMaxAttemptException {
		if (EvaluationManager.getComponentTypeFromLatestAction() != currentType) {
			// if last action has different type, this is the first attempt => no error
			return 0;
		}
		int attempt = EvaluationManager.getAttemptsFromLatestAction();
		if (attempt >= maxAttemptsSynth - 1) {
			throw new ReachMaxAttemptException(currentType.name());
		}

		return attempt + 1;
	}

	private int getNextAttemptIDForFixProof(String poName) throws ReachMaxAttemptException {
		if (EvaluationManager.getLastPOActionIndex() == -1) {
			// no proof action before
			return 0;
		} else {
			String previousPOName = EvaluationManager.getPONameFromLatestProofAction();
			if (poName == null || (!poName.equals(previousPOName) && poName != previousPOName)) {
				// different PO
				return 0;
			}

			int attempt = EvaluationManager.getAttemptsFromLatestProofAction();
			if (attempt >= maxAttemptsProof - 1) {
				visitedPOs.add(poName);
				throw new ReachMaxAttemptException(ComponentType.FIX_PROOF.name());
			}

			return attempt + 1;
		}
	}

	private String[] fixCompilationErrors(String projectName, String[] fileNames)
			throws InvocationTargetException, InterruptedException, ReachMaxAttemptException {
		// throw exception and stop if exceeds limit
		int newAttemptID = getNextAttemptID(ComponentType.FIX_COMPILATION);
		EvaluationManager.addAndStartNewAction(ComponentType.FIX_COMPILATION, newAttemptID);

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
					EvaluationManager.endLatestAction();

					if (newModel != null) {
//						System.out.println(newModel.toString(2));
						String[] newFileNames = saveModel(projectName, newModel);
						fixCompilationErrors(projectName, newFileNames);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (ReachMaxAttemptException e) {
					System.out.println(e.getMessage());
					EvaluationManager.setErrorToLatestAction(e.getMessage());
				} finally {
					// save file without modification
					try {
						IRodinProject rodinProject = getRodinProject(projectName);
						final IRodinFile rodinFile = rodinProject.getRodinFile(machineFileName);
						rodinFile.save(null, true);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, fixCompilationErrorOperation);

		return new String[] { contextFileName, machineFileName };
	}

	private void fixBasedOnModelCheckingResults(String projectName, String[] fileNames)
			throws InvocationTargetException, InterruptedException, ReachMaxAttemptException {

		// throw exception and stop if exceeds limit
		int newAttemptID = getNextAttemptID(ComponentType.FIX_MODEL_CHECKING);
		EvaluationManager.addAndStartNewAction(ComponentType.FIX_MODEL_CHECKING, newAttemptID);

		final String contextFileName = fileNames[0];
		final String machineFileName = fileNames[1];

		IRunnableWithProgress modelCheckingOperation = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IRodinProject rodinProject = RodinUtils.getRodinProject(projectName);

					final IRodinFile contextFile = rodinProject.getRodinFile(contextFileName);
					final IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IContextRoot contextRoot = (IContextRoot) contextFile.getRoot();
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

					ModelCheckingFixer fixer = new ModelCheckingFixer(llmRequestSender, llmResponseParser);
					JSONObject newModel = fixer.fixModelBasedOnProBResults(contextRoot, machineRoot);
					EvaluationManager.endLatestAction();

					if (newModel != null) {
//						System.out.println(newModel.toString(2));
						String[] newFileNames = saveModel(projectName, newModel);
						fixBasedOnModelCheckingResults(projectName, newFileNames);
					}
				} catch (CoreException | ReachMaxAttemptException e) {
					System.out.println(e.getMessage());
					EvaluationManager.setErrorToLatestAction(e.getMessage());
				} finally {
					// save file without modification
					try {
						IRodinProject rodinProject = getRodinProject(projectName);
						final IRodinFile rodinFile = rodinProject.getRodinFile(machineFileName);
						rodinFile.save(null, true);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, modelCheckingOperation);
	}

	private IPOSequent getPO(List<IPOSequent> pos, String poName) {
		for (IPOSequent po : pos) {
			if (visitedPOs.contains(po.getElementName())) {
				continue;
			}
			if (poName == null) {
				return po;
			}
			String otherPOName = po.getElementName();
			if (otherPOName.equals(poName) || otherPOName == poName) {
				return po;
			}
		}
		return null;
	}

	private void runAutoProvers(String projectName, String[] fileNames)
			throws InvocationTargetException, InterruptedException {
		final String machineFileName = fileNames[1];

		// fix POs
		IRunnableWithProgress fixPOsOperation = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IRodinProject rodinProject = getRodinProject(projectName);
					final IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

					// wait for bps file to be generated
					try {
						buildAndWaitForPOs(machineFile, monitor);
					} finally {
						monitor.done();
					}

					POManager poManager = new POManager();
					POFixer poFixer = new POFixer(llmRequestSender, llmResponseParser);

					int totalPOCount = poManager.getAllPOs(machineRoot).length;
					int undischargedPOCount = poManager.getOpenPOs(machineRoot).size();
					System.out.println(totalPOCount);
					System.out.println(undischargedPOCount);

					poManager.runAutoProvers(machineRoot);
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		runnableContext.run(false, false, fixPOsOperation);
	}

	private String[] fixPOs(String projectName, String[] fileNames, String poName)
			throws InvocationTargetException, InterruptedException, ReachMaxAttemptException {

		// throw exception and stop if exceeds limit
		int newAttemptID = getNextAttemptIDForFixProof(poName);

		final String contextFileName = fileNames[0];
		final String machineFileName = fileNames[1];

		// fix POs
		IRunnableWithProgress fixPOsOperation = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IRodinProject rodinProject = getRodinProject(projectName);
					final IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

					// wait for bps file to be generated
					try {
						buildAndWaitForPOs(machineFile, monitor);
					} finally {
						monitor.done();
					}

					POManager poManager = new POManager();
					POFixer poFixer = new POFixer(llmRequestSender, llmResponseParser);

					List<IPOSequent> pos = poManager.getOpenPOs(machineRoot);

					if (pos.isEmpty()) {
						EvaluationManager.addAndStartNewAction(ComponentType.FIX_PROOF, newAttemptID);
						EvaluationManager.setErrorToLatestAction("All POs discharged.");
						EvaluationManager.endLatestAction();
					}

					IPOSequent undischargedPO = getPO(pos, poName);
					if (undischargedPO != null) {
						String undischargedPOName = undischargedPO.getElementName();
						System.out.println(undischargedPOName);

						EvaluationManager.addAndStartNewAction(ComponentType.FIX_PROOF, newAttemptID);
						EvaluationManager.setPONameToLatestAction(undischargedPOName);
						EvaluationManager.setLastPOActionIndex();

						if (enableFixStrategy) {
							poFixer.autoFixPO(machineRoot, undischargedPO);
							EvaluationManager.endLatestAction();

							if (ProofUtils.isDischarged(machineRoot, undischargedPOName)) {
								visitedPOs.add(undischargedPOName);
								EvaluationManager.setErrorToLatestAction("PO discharged");
								fixPOs(projectName, fileNames, null);
							} else {
								fixPOs(projectName, fileNames, undischargedPOName);
							}
						} else {
							JSONObject newModel = poFixer.autoFixPOWithoutStrategy(machineRoot, undischargedPO);
							if (newModel != null) {
								saveModel(projectName, newModel);
								EvaluationManager.endLatestAction();

//								fixCompilationErrors(projectName, fileNames);
								if (ProofUtils.isDischarged(machineRoot, undischargedPOName)) {
									visitedPOs.add(undischargedPOName);
									EvaluationManager.setErrorToLatestAction("PO discharged");
									fixPOs(projectName, fileNames, null);
								} else {
									fixPOs(projectName, fileNames, undischargedPOName);
								}
							}
						}
					}

				} catch (ReachMaxAttemptException e) {
					System.out.println(e.getMessage());
					EvaluationManager.setErrorToLatestAction(e.getMessage());
					visitedPOs.add(e.poName == null ? poName : e.poName);
					try {
						fixPOs(projectName, fileNames, null);
					} catch (InvocationTargetException | InterruptedException | ReachMaxAttemptException e1) {
						e1.printStackTrace();
						throw new InvocationTargetException(e);
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

		monitor.setTaskName("Creating " + fileName);
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
