package eventb_agent_core.llminteractor;

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
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.errorinfo.CompilationErrorInfoExtractor;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.RodinUtils;
import eventb_agent_core.utils.llm.ParserUtils;

public class CompilationErrorFixer extends AbstractLLMInteractor {

	public CompilationErrorFixer(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	public JSONObject solveCompilationErrors(String projectName, final String machineFileName,
			final String contextFileName, IProgressMonitor monitor) throws CoreException, ReachMaxAttemptException {

		monitor.beginTask("Fixing compilation errors in " + machineFileName, 2);
		IRodinProject rodinProject = RodinUtils.getRodinProject(projectName);

		JSONObject newModel = null;

		final IRodinFile machineFile = rodinProject.getRodinFile(machineFileName);
		final IRodinFile contextFile = rodinProject.getRodinFile(contextFileName);
		IMachineRoot machineRoot = (IMachineRoot) machineFile.getRoot();
		IContextRoot contextRoot = (IContextRoot) contextFile.getRoot();

		((IConfigurationElement) machineRoot).setConfiguration(DEFAULT_CONFIGURATION, null);
		((IConfigurationElement) contextRoot).setConfiguration(DEFAULT_CONFIGURATION, null);

		IResource machineResource = machineRoot.getResource();
		IResource contextResource = contextRoot.getResource();
		IMarker[] machineMarkers = machineResource.findMarkers("org.rodinp.core.problem", true,
				IResource.DEPTH_INFINITE);
		IMarker[] contextMarkers = contextResource.findMarkers("org.rodinp.core.problem", true,
				IResource.DEPTH_INFINITE);

		List<String> messages = new ArrayList<>();
		messages.addAll(getCompilationErrors(contextRoot, contextMarkers, true));
		messages.addAll(getCompilationErrors(machineRoot, machineMarkers, false));

		if (!messages.isEmpty()) {
			newModel = fixCompilationError(machineRoot, contextRoot, messages, machineResource);
		}

		return newModel;
	}

	private List<String> getCompilationErrors(IInternalElement componentRoot, IMarker[] markers, boolean isContext)
			throws CoreException {
		List<String> messages = new ArrayList<>();
		for (int i = 0; i < markers.length; i++) {
			IMarker marker = markers[i];
			int severity = marker.getAttribute(IMarker.SEVERITY, -1);
			if (severity == IMarker.SEVERITY_ERROR || severity == IMarker.SEVERITY_WARNING) {
				String message = (String) marker.getAttribute(IMarker.MESSAGE);

				String handle = (String) marker.getAttribute("element", null);
				if (handle != null) {
					CompilationErrorInfoExtractor infoExtractor = new CompilationErrorInfoExtractor(handle);
					List<IInternalElement> erroneousElements = new ArrayList<>();
					if (isContext) {
						erroneousElements = infoExtractor.getErroneousElementsFromContext((IContextRoot) componentRoot);
					} else {
						erroneousElements = infoExtractor.getErroneousElementsFromMachine((IMachineRoot) componentRoot);
					}

					if (erroneousElements == null) {
						messages.add(message);
						continue;
					} else if (erroneousElements.size() == 1) {
						// one element with error
						IInternalElement element = erroneousElements.get(0);
						String type = element.getElementType().getName();

						int markerStart = (int) marker.getAttribute(IMarker.CHAR_START, -1);
						int markerEnd = (int) marker.getAttribute(IMarker.CHAR_END, -1);
						String jsonStr = RetrieveModelUtils.getComponentJSON(element, markerStart, markerEnd);

						StringBuilder messageBuilder = new StringBuilder();
						messageBuilder.append(type + ": ");
						messageBuilder.append(jsonStr + " has the issue:\n");
						messageBuilder.append(message);
						messageBuilder.append("\n");

						messages.add(messageBuilder.toString());
					} else if (erroneousElements.size() == 2) {
						// one element from event has error
						IEvent event = (IEvent) erroneousElements.get(0);
						String eventName = event.getLabel();

						IInternalElement element = erroneousElements.get(1);
						String type = element.getElementType().getName();

						int markerStart = (int) marker.getAttribute(IMarker.CHAR_START, -1);
						int markerEnd = (int) marker.getAttribute(IMarker.CHAR_END, -1);
						String jsonStr = RetrieveModelUtils.getComponentJSON(element, markerStart, markerEnd);

						StringBuilder messageBuilder = new StringBuilder();
						messageBuilder.append(type + ": ");
						messageBuilder.append(jsonStr + " from event `");
						messageBuilder.append(eventName + "` has the issue:\n");
						messageBuilder.append(message);
						messageBuilder.append("\n");

						messages.add(messageBuilder.toString());
					}

				} else {
					messages.add(message);
				}
			}
		}
		return messages;
	}

	private JSONObject fixCompilationError(IMachineRoot machineRoot, IContextRoot contextRoot,
			List<String> errorMessages, IResource resource) throws CoreException, ReachMaxAttemptException {

		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		JSONObject response = getLLMResponse(
				new String[] { ParserUtils.reverseLex(modelJSON),
						ParserUtils.reverseLex(getErrorsPlaceHolderContent(errorMessages)) },
				LLMRequestTypes.FIX_COMPILATION_ERRS);

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
