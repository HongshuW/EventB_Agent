package eventb_agent_ui.handlers;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ISeesContext;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

import eventb_agent_core.proof.POManager;
import eventb_agent_core.utils.RodinUtils;

public class DataCollectionHandler extends AbstractHandler implements IHandler {

	public DataCollectionHandler() {
		super();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("==========\nEvaluation Data Collection for Event-B Agent\n==========");

		List<IProject> openProjects = getOpenProjects();

		for (IProject project : openProjects) {
			try {
				System.out.println("==========\n" + project.getName() + "\n==========");
				IRodinProject rodinProject = RodinUtils.getRodinProject(project.getName());

				List<IFile> machineFiles = getMachineFiles(project);
				for (IFile file : machineFiles) {
					// machine file
					String machineFileName = file.getName();
					IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

					// context file
					// assume that a machine sees one context
					ISeesContext seesContext = machineRoot.getSeesClauses()[0];
					String contextFileName = seesContext.getSeenContextName() + ".buc";
					IRodinFile contextFile = rodinProject.getRodinFile(contextFileName);
					IContextRoot contextRoot = (IContextRoot) contextFile.getRoot();

					System.out.println("Processing: " + machineFileName + " and " + contextFileName);

					/* compilation errors */

					// get problem markers
					((IConfigurationElement) machineRoot).setConfiguration(DEFAULT_CONFIGURATION, null);
					((IConfigurationElement) contextRoot).setConfiguration(DEFAULT_CONFIGURATION, null);

					IResource machineResource = machineRoot.getResource();
					IResource contextResource = contextRoot.getResource();
					IMarker[] machineMarkers = machineResource.findMarkers("org.rodinp.core.problem", true,
							IResource.DEPTH_INFINITE);
					IMarker[] contextMarkers = contextResource.findMarkers("org.rodinp.core.problem", true,
							IResource.DEPTH_INFINITE);

					int compilationErrorCount = getCompilationErrorCount(machineMarkers, contextMarkers,
							IMarker.SEVERITY_ERROR);
					int compilationWarningCount = getCompilationErrorCount(machineMarkers, contextMarkers,
							IMarker.SEVERITY_WARNING);
					System.out.println("Compilation Errors:" + String.valueOf(compilationErrorCount));
					System.out.println("Compilation Warnings:" + String.valueOf(compilationWarningCount));
					System.out.println();

					/* PO Discharge Rate */
					POManager poManager = new POManager();
					List<IPOSequent> undischargedPOs = poManager.getOpenPOs(machineRoot);
					IPOSequent[] POs = poManager.getAllPOs(machineRoot);
					System.out.println("Discharged POs: " + String.valueOf(POs.length - undischargedPOs.size()));
					System.out.println("Total POs: " + String.valueOf(POs.length));
					System.out.println();
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private List<IProject> getOpenProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();

		List<IProject> openProjects = new ArrayList<>();

		for (IProject project : projects) {
			if (project.isOpen()) {
				openProjects.add(project);
			}
		}

		return openProjects;
	}

	private List<IFile> getMachineFiles(IProject project) throws CoreException {
		List<IFile> files = new ArrayList<>();

		if (project.isOpen()) {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource.getType() == IResource.FILE) {
						IFile file = (IFile) resource;
						if (file.getName().endsWith(".bum")) {
							files.add(file);
						}
					}
					return true;
				}
			});
		}

		return files;
	}

	private int getCompilationErrorCount(IMarker[] machineMarkers, IMarker[] contextMarkers, int severity)
			throws CoreException {
		int count = 0;
		for (IMarker marker : machineMarkers) {
			int markerSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
			if (markerSeverity == severity) {
				count++;
			}
		}
		for (IMarker marker : contextMarkers) {
			int markerSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
			if (markerSeverity == severity) {
				count++;
			}
		}
		return count;
	}

}
