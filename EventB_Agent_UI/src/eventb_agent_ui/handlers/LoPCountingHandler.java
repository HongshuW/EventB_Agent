package eventb_agent_ui.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

import eventb_agent_core.proof.POManager;
import eventb_agent_core.utils.RodinUtils;
import eventb_agent_core.utils.proof.ProofUtils;

/**
 * This handler computes lines of proofs.
 */
public class LoPCountingHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IProject> openProjects = getOpenProjects();

		double averageLoPPerProject = 0;
		double averageLoPTotal = 0;
		int noProjects = 0;
		int totalPOs = 0;

		int maxLoP = 0;

		for (IProject project : openProjects) {
			try {
				double averageProjectLoP = 0;
				int noPOs = 0;

				String projectName = project.getName();

				IRodinProject rodinProject = RodinUtils.getRodinProject(projectName);

				List<IFile> machineFiles = getMachineFiles(project);
				machineFiles.sort((Comparator<? super IFile>) new Comparator<IFile>() {
					@Override
					public int compare(IFile f1, IFile f2) {
						String name1 = f1.getName().split("\\.")[0];
						String name2 = f2.getName().split("\\.")[0];
						int last1 = name1.charAt(name1.length() - 1);
						int last2 = name2.charAt(name2.length() - 1);
						return Integer.compare(last1, last2);
					}
				});

				for (IFile file : machineFiles) {
					// machine file
					String machineFileName = file.getName();
					IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
					IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

					POManager poManager = new POManager();
					IPOSequent[] POs = poManager.getAllPOs(machineRoot);

					for (IPOSequent po : POs) {
						String poName = po.getElementName();
//						System.out.println(machineFileName + ":" + poName);
						IProofTree proofTree = ProofUtils.getDefaultProofTree(poName, machineRoot);
						if (proofTree == null) {
							System.out.println(poName);
							continue;
						}
						IProofTreeNode root = proofTree.getRoot();
						int nodeCount = getNodeCount(root);
//						System.out.println(nodeCount);

						if (nodeCount > maxLoP) {
							maxLoP = nodeCount;
						}

						averageProjectLoP += nodeCount;
						noPOs += 1;

						totalPOs += 1;
					}
				}

//				System.out.println("Number of POs: " + noPOs);

				averageLoPTotal += averageProjectLoP;
				averageProjectLoP /= noPOs;

				averageLoPPerProject += averageProjectLoP;
				noProjects += 1;

//				System.out.println("project: " + projectName);
//				System.out.println("average project LoP: " + averageProjectLoP);

			} catch (CoreException e) {
			}
		}

		averageLoPPerProject /= noProjects;
		averageLoPTotal /= totalPOs;

		System.out.println("average LoP per project: " + averageLoPPerProject);
		System.out.println("average LoP total: " + averageLoPTotal);

		System.out.println("max LoP: " + maxLoP);

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

	private int getNodeCount(IProofTreeNode root) {
		IProofTreeNode[] childNodes = root.getChildNodes();
		if (childNodes == null || childNodes.length == 0) {
			return 1;
		}
		int count = 1;
		for (IProofTreeNode child : childNodes) {
			count += getNodeCount(child);
		}
		return count;
	}

}
