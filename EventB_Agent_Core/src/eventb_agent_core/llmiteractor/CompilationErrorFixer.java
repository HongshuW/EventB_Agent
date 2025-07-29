package eventb_agent_core.llmiteractor;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.RodinUtils;

public class CompilationErrorFixer extends AbstractLLMInteractor {

	public CompilationErrorFixer(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	public JSONObject solveCompilationErrors(String projectName, final String machineFileName,
			final String contextFileName, IProgressMonitor monitor) throws CoreException {

		monitor.beginTask("Fixing compilation errors in " + machineFileName, 2);
		IRodinProject rodinProject = RodinUtils.getRodinProject(projectName);

		JSONObject newModel = null;

//		RodinCore.run(new IWorkspaceRunnable() {
//
//			@Override
//			public void run(IProgressMonitor pMonitor) throws CoreException {
		final IRodinFile rodinFile = rodinProject.getRodinFile(machineFileName);
		final IInternalElement rodinRoot = rodinFile.getRoot();
		((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, null);

		IMachineRoot machineRoot = (IMachineRoot) rodinFile.getRoot();
		IContextRoot contextRoot = (IContextRoot) rodinProject.getRodinFile(contextFileName).getRoot();

		IResource resource = rodinRoot.getResource();
		IMarker[] markers = resource.findMarkers("org.rodinp.core.problem", true, IResource.DEPTH_INFINITE); // org.rodinp.core.problem
		List<String> messages = new ArrayList<>();
		for (int i = 0; i < markers.length; i++) {
			IMarker marker = markers[i];
			int severity = marker.getAttribute(IMarker.SEVERITY, -1);
			if (severity == IMarker.SEVERITY_ERROR) { // TODO: add SEVERITY_WARNING
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

					IEvent[] events = machineRoot.getEvents();
					IEvent event = events[0]; // event#1 as per the marker
					IParameter[] parameters = event.getParameters();

					IRodinElement element = RodinCore.valueOf(handle);

				}

				messages.add(message);
			}
		}
		if (!messages.isEmpty()) {
			newModel = fixCompilationError(machineRoot, contextRoot, messages, resource);
		}

//				rodinFile.save(null, true);
//			}
//		}, monitor);
//		monitor.worked(1);

		return newModel;
	}

	private JSONObject fixCompilationError(IMachineRoot machineRoot, IContextRoot contextRoot,
			List<String> errorMessages, IResource resource) throws CoreException {

//		CompilationErrorType errorType = CompilationErrorType.getCompilationErrorType(errorMessage);
//		if (errorType == CompilationErrorType.TYPE_MISSING) {
//			if (element instanceof IVariable) {
//				IVariable var = (IVariable) element;
//				String identifierName = var.getIdentifierString();
//				System.out.println(identifierName);
//			}
//		}

		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		JSONObject response = getLLMResponse(new String[] { modelJSON, getErrorsPlaceHolderContent(errorMessages) },
				LLMRequestTypes.FIX_COMPILATION_ERRS);

		System.out.println(response.toString(2));

//		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		return response;
	}

	private String getErrorsPlaceHolderContent(List<String> errorMessages) {
		StringBuilder content = new StringBuilder();
		for (String e : errorMessages) {
			content.append(e);
			content.append("\n");
		}
		return content.toString();
	}
}
