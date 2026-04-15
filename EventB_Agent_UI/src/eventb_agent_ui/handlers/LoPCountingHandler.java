package eventb_agent_ui.handlers;

import java.util.ArrayList;
import java.util.Collections;
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

	private String[] simple = new String[] { "ch4_simple_file_transfer_protocol", "ch11_tree_shaped_network",
			"ClockProject", "1_division0", "1_square0", "Bakery", "BridgeModels", "ch2_car_on_bridge",
			"ch6_bounded_retransmission_protocol" };
	private String[] medium = new String[] { "LivenessModels", "1_square1", "ch3_mechanical_press_controller",
			"1_division1", "2_search_array", "2_search_matrix", "3_maxi1", "4_revarray", "90_gcd" };
	private String[] complex = new String[] { "3_maxi2", "3_mini1", "3_mini2", "5_partitioning", "8_sorting",
			"7_Inversing", "ch16_location_access_controller", "9_pointer", "6_binsearch" };

	private String[][] partitions = new String[][] { simple, medium, complex };

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IProject> openProjects = getOpenProjects();

		int maxLoP = Integer.MIN_VALUE;
		int minLoP = Integer.MAX_VALUE;
		List<Integer> LoP = new ArrayList<>();

		for (String[] partition : partitions) {

			int partitionMaxLoP = Integer.MIN_VALUE;
			int partitionMinLoP = Integer.MAX_VALUE;
			List<Integer> partitionLoP = new ArrayList<>();

			for (IProject project : openProjects) {
				try {

					String projectName = project.getName();

					boolean inPartition = false;
					for (String p : partition) {
						if (projectName.equals(p)) {
							inPartition = true;
							break;
						}
					}
					if (!inPartition) {
						continue;
					}

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
							IProofTree proofTree = ProofUtils.getDefaultProofTree(poName, machineRoot);
							if (proofTree == null) {
								proofTree = ProofUtils.getDefaultProofTreeForCountingLoP(poName, machineRoot);
								if (proofTree == null) {
									System.out.println(poName);
									continue;
								}
							}
							IProofTreeNode root = proofTree.getRoot();
							int nodeCount = getNodeCount(root);

							maxLoP = Math.max(maxLoP, nodeCount);
							minLoP = Math.min(minLoP, nodeCount);
							LoP.add(nodeCount);

							partitionMaxLoP = Math.max(partitionMaxLoP, nodeCount);
							partitionMinLoP = Math.min(partitionMinLoP, nodeCount);
							partitionLoP.add(nodeCount);
						}
					}

				} catch (CoreException e) {
				}
			}

			System.out.println("Partition Finished");
			double[] partitionQuartiles = quartiles(partitionLoP);
			System.out.println("partition min LoP: " + partitionMinLoP);
			System.out.println("partition 1st quartile LoP: " + partitionQuartiles[0]);
			System.out.println("partition median LoP: " + partitionQuartiles[1]);
			System.out.println("partition 3rd quartile LoP: " + partitionQuartiles[2]);
			System.out.println("partition max LoP: " + partitionMaxLoP);
		}

		double[] LoPQuartiles = quartiles(LoP);
		System.out.println("min LoP: " + minLoP);
		System.out.println("1st quartile LoP: " + LoPQuartiles[0]);
		System.out.println("median LoP: " + LoPQuartiles[1]);
		System.out.println("3rd quartile LoP: " + LoPQuartiles[2]);
		System.out.println("max LoP: " + maxLoP);

		return null;
	}

	private double median(List<Integer> list) {
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Empty input");
		}

		List<Integer> copy = new ArrayList<>(list); // avoid mutating original
		Collections.sort(copy);

		int n = copy.size();
		if (n % 2 == 1) {
			return copy.get(n / 2);
		} else {
			return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
		}
	}

	private double[] quartiles(List<Integer> list) {
		if (list == null || list.size() < 2) {
			throw new IllegalArgumentException("Need at least 2 elements");
		}

		List<Integer> xs = new ArrayList<>(list);
		Collections.sort(xs);

		int n = xs.size();
		double q2 = median(xs);

		List<Integer> lower = xs.subList(0, n / 2);
		List<Integer> upper = xs.subList((n + 1) / 2, n); // excludes median if odd

		double q1 = median(lower);
		double q3 = median(upper);

		return new double[] { q1, q2, q3 };
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
