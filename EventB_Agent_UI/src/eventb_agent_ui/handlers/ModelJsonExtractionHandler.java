package eventb_agent_ui.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ISeesContext;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.RodinUtils;

public class ModelJsonExtractionHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("==========\nConverting of Model into JSON Format\n==========");

		List<IProject> openProjects = getOpenProjects();

		for (IProject project : openProjects) {
			try {
				System.out.println("==========\n" + project.getName() + "\n==========");
				IRodinProject rodinProject = RodinUtils.getRodinProject(project.getName());

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

				IFile lastMachine = machineFiles.get(machineFiles.size() - 1);

				String machineFileName = lastMachine.getName();
				IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
				IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();

				// context file
				// assume that a machine sees one context
				for (ISeesContext seesContext : machineRoot.getSeesClauses()) {
					String contextFileName = seesContext.getSeenContextName() + ".buc";
					IRodinFile contextFile = rodinProject.getRodinFile(contextFileName);
					IContextRoot contextRoot = (IContextRoot) contextFile.getRoot();

					ObjectMapper mapper = new ObjectMapper();
					LinkedHashMap<String, Object> contextJSON = RetrieveModelUtils.getContextJSON(contextRoot);
					String contextJSONString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextJSON);
					System.out.println(contextJSONString);
					System.out.println();
				}

				ObjectMapper mapper = new ObjectMapper();
				LinkedHashMap<String, Object> machineJSON = RetrieveModelUtils.getMachineJSON(machineRoot);
				String machineJSONString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(machineJSON);
				System.out.println(machineJSONString);
				System.out.println();

			} catch (Exception e) {
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

}
