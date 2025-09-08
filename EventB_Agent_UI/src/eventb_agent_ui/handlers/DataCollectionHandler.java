package eventb_agent_ui.handlers;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eventb.core.IAxiom;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariant;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.errorinfo.CompilationErrorInfoExtractor;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.proof.POManager;
import eventb_agent_core.refinement.Requirement;
import eventb_agent_core.refinement.SystemRequirements;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.RodinUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class DataCollectionHandler extends AbstractHandler implements IHandler {

//	private String GROUP = "ablation_norefine_proofstrategy";
	private String GROUP = "test";

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

				IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
				String datasetPath = prefs.get(AgentPreferenceInitializer.PREF_DATASET_LOC, "");
				String inputPath = datasetPath + File.separator + project.getName() + ".json";

				SystemRequirements systemReqs = new SystemRequirements(Path.of(inputPath));
				List<Requirement> requirements = systemReqs.getRequirements();
				Map<String, Integer> coveredRequirements = new HashMap<>();
				Map<String, Integer> coveredButNotDischarged = new HashMap<>();

				/* Data */
				int dischargedPOCount = 0;
				int totalPOCount = 0;

				int coveredRequirementCount = 0;
				int fulfilledRequirementCount = 0;
				int totalRequirementCount = 0;

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

					/* ====== Important: Baseline Only ====== */
					POManager poManager = new POManager();
//					poManager.runAutoProvers(machineRoot);

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
					List<IPOSequent> undischargedPOs = poManager.getOpenPOs(machineRoot);
					IPOSequent[] POs = poManager.getAllPOs(machineRoot);
					dischargedPOCount = POs.length - undischargedPOs.size();
					totalPOCount = POs.length;
					System.out.println("Discharged POs: " + String.valueOf(dischargedPOCount));
					System.out.println("Total POs: " + String.valueOf(totalPOCount));
					System.out.println();

					/* Requirement Coverage Rate & Fulfillment Rate */

					/* 1. requirements have errors => not covered */
					Set<String> erroneousRequirements = new HashSet<>();
					addUncoveredReqs(machineMarkers, machineRoot, true, requirements, erroneousRequirements);
					addUncoveredReqs(contextMarkers, contextRoot, false, requirements, erroneousRequirements);

					/* 2. No errors, no POs => assume to be covered and fulfilled. */
					IAxiom[] axioms = contextRoot.getAxioms();
					IInvariant[] invariants = machineRoot.getInvariants();
					IVariant[] variants = machineRoot.getVariants();
					addCoveredReqs(axioms, requirements, coveredRequirements);
					addCoveredReqs(invariants, requirements, coveredRequirements);
					addCoveredReqs(variants, requirements, coveredRequirements);

					/* 3. No errors, POs generated => covered. */
					IPOSequent[] contextPOs = poManager.getAllPOs(contextRoot);
					addUnfulfilledReqs(POs, requirements, coveredRequirements, coveredButNotDischarged, machineRoot);
					addUnfulfilledReqs(contextPOs, requirements, coveredRequirements, coveredButNotDischarged,
							contextRoot);

					/* Compute fulfilled requirements */
					Map<String, Integer> finalCovered = new HashMap<>();
					for (String covered : coveredRequirements.keySet()) {
						if (erroneousRequirements.contains(covered)) {
							continue;
						}
						finalCovered.put(covered, coveredRequirements.get(covered));
					}

					Map<String, Integer> fulfilledRequirements = new HashMap<>();
					for (String covered : coveredRequirements.keySet()) {
						if (erroneousRequirements.contains(covered)) {
							continue;
						}
						if (coveredButNotDischarged.containsKey(covered)) {
							continue;
						}
						fulfilledRequirements.put(covered, coveredRequirements.get(covered));
					}

					fulfilledRequirementCount = fulfilledRequirements.size();
					coveredRequirementCount = finalCovered.size();
					totalRequirementCount = requirements.size();

					System.out.println("Fulfilled requirements: " + String.valueOf(fulfilledRequirementCount));
					System.out.println("Covered requirements: " + String.valueOf(coveredRequirementCount));
					System.out.println("All requirements: " + String.valueOf(totalRequirementCount));
					System.out.println();

					String outputPath = "C:\\Users\\admin\\Downloads\\data_analysis\\" + GROUP + ".txt";
					write(outputPath, project.getName(), totalPOCount, dischargedPOCount,
							totalRequirementCount, coveredRequirementCount, fulfilledRequirementCount);

//					int allPOsAboutRequirements = 0;
//					int dischargedPOsAboutRequirements = 0;
//					for (String covered : coveredRequirements.keySet()) {
//						allPOsAboutRequirements += coveredRequirements.get(covered);
//					}
//					for (String fulfilled : fulfilledRequirements.keySet()) {
//						dischargedPOsAboutRequirements += fulfilledRequirements.get(fulfilled);
//					}
//
//					System.out.println("Discharged POs relevant to requirements: "
//							+ String.valueOf(dischargedPOsAboutRequirements));
//					System.out.println("All POs about requirements: " + String.valueOf(allPOsAboutRequirements));
//					System.out.println();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private void write(String path, String projectName, int totalPOCount, int dischargedPOCount,
			int totalRequirementCount, int coveredRequirementCount, int fulfilledRequirementCount) {
		StringBuilder contents = new StringBuilder();
		contents.append("" + ",");
		contents.append(projectName + ",");
		contents.append(String.valueOf(totalPOCount) + ",");
		contents.append(String.valueOf(dischargedPOCount) + ",");
		contents.append(String.valueOf(totalRequirementCount) + ",");
		contents.append(String.valueOf(coveredRequirementCount) + ",");
		contents.append(String.valueOf(fulfilledRequirementCount) + ",\n");

		try (FileWriter writer = new FileWriter(path, true)) {
			writer.append(contents.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private String getRequirementID(String label, List<Requirement> requirements) {
		for (Requirement req : requirements) {
			if (label.contains(req.getRequirementID())) {
				return req.getRequirementID();
			}
		}
		return null;
	}

	private void addReqID(String reqID, Map<String, Integer> map) {
		if (!map.containsKey(reqID)) {
			map.put(reqID, 0);
		}
		map.put(reqID, map.get(reqID) + 1);
	}

	private void addUncoveredReqs(IMarker[] markers, IEventBRoot root, boolean isMachine,
			List<Requirement> requirements, Set<String> erroneousRequirements) throws RodinDBException {
		for (IMarker marker : markers) {
			int markerSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
			if (markerSeverity == IMarker.SEVERITY_ERROR) {
				String handle = (String) marker.getAttribute("element", null);
				CompilationErrorInfoExtractor infoExtractor = new CompilationErrorInfoExtractor(handle);
				List<IInternalElement> erroneousElements = new ArrayList<>();
				if (isMachine) {
					erroneousElements = infoExtractor.getErroneousElementsFromMachine((IMachineRoot) root);
				} else {
					erroneousElements = infoExtractor.getErroneousElementsFromContext((IContextRoot) root);
				}

				for (IInternalElement element : erroneousElements) {
					String label = "";
					if (element instanceof IInvariant) {
						label = ((IInvariant) element).getLabel();
					} else if (element instanceof IVariant) {
						label = ((IVariant) element).getLabel();
					} else if (element instanceof IAxiom) {
						label = ((IAxiom) element).getLabel();
					} else {
						continue;
					}
					String matchingReqID = getRequirementID(label, requirements);
					if (matchingReqID != null) {
						erroneousRequirements.add(matchingReqID);
					}
				}
			}
		}
	}

	private void addCoveredReqs(ILabeledElement[] elements, List<Requirement> requirements,
			Map<String, Integer> coveredRequirements) throws RodinDBException {
		for (ILabeledElement element : elements) {
			String matchingReqID = getRequirementID(((ILabeledElement) element).getLabel(), requirements);
			if (matchingReqID != null) {
				// requirement is covered
				addReqID(matchingReqID, coveredRequirements);
			}
		}
	}

	private void addUnfulfilledReqs(IPOSequent[] POs, List<Requirement> requirements, Map<String, Integer> covered,
			Map<String, Integer> notDischarged, IEventBRoot root) throws RodinDBException {
		Set<String> notDischargedSet = new HashSet<>();
		for (IPOSequent po : POs) {
			String poName = po.getElementName();
			String matchingReqID = getRequirementID(poName, requirements);
			if (matchingReqID != null) {
				addReqID(matchingReqID, covered);
				if (!ProofUtils.isDischarged(root, poName)) {
					/* 4. No errors, POs not discharged => not fulfilled. */
					notDischargedSet.add(matchingReqID);
					addReqID(matchingReqID, notDischarged);
				}
			}
		}

		for (Requirement req : requirements) {
			String reqID = req.getRequirementID();
			if (notDischarged.containsKey(reqID) && !notDischargedSet.contains(reqID)) {
				/* requirement wasn't fulfilled in previous rounds, but is fulfilled now. */
				notDischarged.remove(reqID);
			}
		}
	}
}
