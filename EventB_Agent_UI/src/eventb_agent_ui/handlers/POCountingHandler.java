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
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

import eventb_agent_core.proof.POManager;
import eventb_agent_core.utils.RodinUtils;

/**
 * This handler counts the number of POs.
 */
public class POCountingHandler extends AbstractHandler implements IHandler {

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

		for (String[] partition : partitions) {

			double averageLastMachinePOs = 0;
			double averageTotalPOs = 0;
			int noProjects = 0;

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

					double lastMachinePOs = 0;
					double totalPOs = 0;
					for (IFile file : machineFiles) {
						// machine file
						String machineFileName = file.getName();
						IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
						IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

						POManager poManager = new POManager();
						IPOSequent[] POs = poManager.getAllPOs(machineRoot);

						lastMachinePOs = POs.length;
						totalPOs += POs.length;
					}

					System.out.println("Last machine POs: " + lastMachinePOs);
					System.out.println("Total POs: " + totalPOs);

					averageLastMachinePOs += lastMachinePOs;
					averageTotalPOs += totalPOs;
					noProjects += 1;
				} catch (CoreException e) {
				}
			}

			averageLastMachinePOs /= noProjects;
			averageTotalPOs /= noProjects;

			System.out.println("Average last machine POs: " + averageLastMachinePOs);
			System.out.println("Average total POs: " + averageTotalPOs);
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

}
