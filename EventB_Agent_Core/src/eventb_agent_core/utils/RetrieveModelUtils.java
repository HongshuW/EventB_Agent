package eventb_agent_core.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eventb.core.IAction;
import org.eventb.core.IAssignmentElement;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IExpressionElement;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPORoot;
import org.eventb.core.IPredicateElement;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.rodinp.core.IAttributeValue;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.utils.llm.ParserUtils;

public class RetrieveModelUtils {

	/* Retrieve model from proof tree. */

	public static IMachineRoot getMachineRoot(IProofTree tree) {
		IProofAttempt proofAttempt = (IProofAttempt) tree.getOrigin();
		IProofComponent proofComponent = proofAttempt.getComponent();
		IPORoot poRoot = proofComponent.getPORoot();
		IRodinFile rodinFile = poRoot.getRodinFile();
		IRodinProject rodinProject = rodinFile.getRodinProject();
		String fileName = rodinFile.getElementName().replaceFirst("\\.bpo$", ".bum");

		IRodinFile componentFile = rodinProject.getRodinFile(fileName);
		return (IMachineRoot) componentFile.getRoot();
	}

	public static IContextRoot getContextRoot(IProofTree tree) throws RodinDBException {
		IMachineRoot machineRoot = getMachineRoot(tree);
		return getContextRoot(machineRoot);
	}

	private static IContextRoot getContextRoot(IMachineRoot machineRoot) throws RodinDBException {
		ISeesContext[] contexts = machineRoot.getSeesClauses();
		if (contexts == null || contexts.length == 0) {
			return null;
		}
		ISeesContext firstContext = contexts[0];
		String fileName = firstContext.getSeenContextName() + ".buc";

		IRodinFile rodinFile = machineRoot.getRodinProject().getRodinFile(fileName);
		return (IContextRoot) rodinFile.getRoot();
	}

	/* Parse model into JSON. */

	public static String getModelJSON(IMachineRoot machineRoot, IContextRoot contextRoot) throws RodinDBException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> modelJSON = new LinkedHashMap<>();

		LinkedHashMap<String, Object> contextJSON = getContextJSON(contextRoot);
		LinkedHashMap<String, Object> machineJSON = getMachineJSON(machineRoot);

		modelJSON.put(SchemaKeys.CONTEXT_OBJ_KEY, contextJSON);
		modelJSON.put(SchemaKeys.MACHINE_OBJ_KEY, machineJSON);

		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(modelJSON);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String getComponentJSON(IInternalElement element, int markerStart, int markerEnd)
			throws RodinDBException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> json = new LinkedHashMap<>();
		IInternalElementType<? extends IInternalElement> type = element.getElementType();

		if (type.equals(IInvariant.ELEMENT_TYPE) || type.equals(IGuard.ELEMENT_TYPE)
				|| type.equals(IWitness.ELEMENT_TYPE) || type.equals(IAxiom.ELEMENT_TYPE)) {
			json = getLabeledPredicate(element);
			if (markerStart >= 0 && markerEnd >= 0) {
				String newPred = ParserUtils.addMarker((String) json.get(SchemaKeys.PRED), markerStart, markerEnd);
				json.put(SchemaKeys.PRED, newPred);
			}
		} else if (type.equals(IVariant.ELEMENT_TYPE)) {
			json = getLabeledExpression(element);
			if (markerStart >= 0 && markerEnd >= 0) {
				String newExpr = ParserUtils.addMarker((String) json.get(SchemaKeys.EXPR), markerStart, markerEnd);
				json.put(SchemaKeys.EXPR, newExpr);
			}
		} else if (type.equals(IAction.ELEMENT_TYPE)) {
			json = getLabeledAssignment(element);
			if (markerStart >= 0 && markerEnd >= 0) {
				String newAction = ParserUtils.addMarker((String) json.get(SchemaKeys.ASSIGN), markerStart, markerEnd);
				json.put(SchemaKeys.ASSIGN, newAction);
			}
		} else {
			return "";
		}

		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "";
		}
	}

	private static LinkedHashMap<String, Object> getContextJSON(IContextRoot contextRoot) throws RodinDBException {
		LinkedHashMap<String, Object> contextJSON = new LinkedHashMap<>();

		String contextName = contextRoot.getComponentName();
		contextJSON.put(SchemaKeys.CONTEXT, contextName);

		addIdentifiers(contextRoot.getExtendsClauses(), contextJSON, SchemaKeys.EXTENDS);
		addIdentifiers(contextRoot.getCarrierSets(), contextJSON, SchemaKeys.SETS);
		addIdentifiers(contextRoot.getConstants(), contextJSON, SchemaKeys.CONSTANTS);
		addLabeledPredicates(contextRoot.getAxioms(), contextJSON, SchemaKeys.AXIOMS);

		return contextJSON;
	}

	private static LinkedHashMap<String, Object> getMachineJSON(IMachineRoot machineRoot) throws RodinDBException {
		LinkedHashMap<String, Object> machineJSON = new LinkedHashMap<>();

		String machineName = machineRoot.getComponentName();
		machineJSON.put(SchemaKeys.MACHINE, machineName);

		addIdentifiers(machineRoot.getRefinesClauses(), machineJSON, SchemaKeys.REFINES);
		addIdentifiers(machineRoot.getSeesClauses(), machineJSON, SchemaKeys.SEES);
		addIdentifiers(machineRoot.getVariables(), machineJSON, SchemaKeys.VARIABLES);
		addLabeledPredicates(machineRoot.getInvariants(), machineJSON, SchemaKeys.INVARIANTS);
		addLabeledExpressions(machineRoot.getVariants(), machineJSON, SchemaKeys.VARIANTS);
		addEvents(machineRoot.getEvents(), machineJSON);

		return machineJSON;
	}

	private static void addIdentifiers(IInternalElement[] elements, LinkedHashMap<String, Object> json,
			String schemaKey) throws RodinDBException {
		if (elements == null || elements.length == 0) {
			json.put(schemaKey, new ArrayList<>());
		} else {
			List<String> stringVals = new ArrayList<>();
			for (IInternalElement element : elements) {
				IAttributeValue value = element.getAttributeValues()[0];
				stringVals.add(value.getValue().toString());
			}
			json.put(schemaKey, stringVals);
		}
	}

	private static LinkedHashMap<String, Object> getLabeledPredicate(IInternalElement element) throws RodinDBException {
		String label = ((ILabeledElement) element).getLabel();
		String predicate = ((IPredicateElement) element).getPredicateString();
		LinkedHashMap<String, Object> elementJSON = new LinkedHashMap<>();
		elementJSON.put(SchemaKeys.LABEL, label);
		elementJSON.put(SchemaKeys.PRED, predicate);
		return elementJSON;
	}

	private static void addLabeledPredicates(IInternalElement[] elements, LinkedHashMap<String, Object> json,
			String schemaKey) throws RodinDBException {
		List<LinkedHashMap> labeledElements = new ArrayList<>();
		for (IInternalElement element : elements) {
			labeledElements.add(getLabeledPredicate(element));
		}
		json.put(schemaKey, labeledElements);
	}

	private static LinkedHashMap<String, Object> getLabeledExpression(IInternalElement element)
			throws RodinDBException {
		String label = ((ILabeledElement) element).getLabel();
		String expression = ((IExpressionElement) element).getExpressionString();
		LinkedHashMap<String, Object> elementJSON = new LinkedHashMap<>();
		elementJSON.put(SchemaKeys.LABEL, label);
		elementJSON.put(SchemaKeys.EXPR, expression);
		return elementJSON;
	}

	private static void addLabeledExpressions(IInternalElement[] elements, LinkedHashMap<String, Object> json,
			String schemaKey) throws RodinDBException {
		List<LinkedHashMap> labeledElements = new ArrayList<>();
		for (IInternalElement element : elements) {
			labeledElements.add(getLabeledExpression(element));
		}
		json.put(schemaKey, labeledElements);
	}

	private static LinkedHashMap<String, Object> getLabeledAssignment(IInternalElement element)
			throws RodinDBException {
		String label = ((ILabeledElement) element).getLabel();
		String assignment = ((IAssignmentElement) element).getAssignmentString();
		LinkedHashMap<String, Object> elementJSON = new LinkedHashMap<>();
		elementJSON.put(SchemaKeys.LABEL, label);
		elementJSON.put(SchemaKeys.ASSIGN, assignment);
		return elementJSON;
	}

	private static void addLabeledAssignments(IInternalElement[] elements, LinkedHashMap<String, Object> json,
			String schemaKey) throws RodinDBException {
		List<LinkedHashMap> labeledElements = new ArrayList<>();
		for (IInternalElement element : elements) {
			labeledElements.add(getLabeledAssignment(element));
		}
		json.put(schemaKey, labeledElements);
	}

	private static LinkedHashMap<String, Object> getEvent(IEvent event) throws RodinDBException {
		LinkedHashMap<String, Object> eventJSON = new LinkedHashMap<>();
		eventJSON.put(SchemaKeys.EVENT_NAME, event.getLabel());
		addIdentifiers(event.getRefinesClauses(), eventJSON, SchemaKeys.REFINES);
		addIdentifiers(event.getParameters(), eventJSON, SchemaKeys.ANY);
		addLabeledPredicates(event.getGuards(), eventJSON, SchemaKeys.WHERE);
		addLabeledPredicates(event.getWitnesses(), eventJSON, SchemaKeys.WITH);
		addLabeledAssignments(event.getActions(), eventJSON, SchemaKeys.THEN);
		return eventJSON;
	}

	private static void addEvents(IEvent[] events, LinkedHashMap<String, Object> json) throws RodinDBException {
		List<LinkedHashMap> eventList = new ArrayList<>();
		for (IEvent event : events) {
			eventList.add(getEvent(event));
		}
		json.put(SchemaKeys.EVENTS, eventList);
	}

}
