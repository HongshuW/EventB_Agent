package eventb_agent_ui.utils;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eventb.core.IAction;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.IConstant;
import org.eventb.core.IConvergenceElement;
import org.eventb.core.IEvent;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;

public class CreateModelUtils {

	/* context methods */

	public static void initiateContext(IInternalElement rodinRoot, IProgressMonitor pMonitor, LLMResponseParser parser,
			JSONObject json) throws RodinDBException {

		System.out.println(json);

		// parse response
		List<String> extendedContexts = parser.getExtends(json);
		List<String> sets = parser.getSets(json);
		List<String> constants = parser.getConstants(json);
		List<String[]> axioms = parser.getAxioms(json);

		// TODO: add this later
//		addExtendsChildren(rodinRoot, pMonitor, extendedContexts);
		addSetsChildren(rodinRoot, pMonitor, sets);
		addConstantsChildren(rodinRoot, pMonitor, constants);
		addAxiomsChildren(rodinRoot, pMonitor, axioms);
	}

	private static void addExtendsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> extendedContexts) throws RodinDBException {
		for (String contextIndentifier : extendedContexts) {
			IExtendsContext extendedContext = internalElement.createChild(IExtendsContext.ELEMENT_TYPE, null, pMonitor);
			extendedContext.setAbstractContextName(contextIndentifier, pMonitor);
		}
	}

	private static void addSetsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String> sets)
			throws RodinDBException {
		for (String setIdentifier : sets) {
			ICarrierSet set = internalElement.createChild(ICarrierSet.ELEMENT_TYPE, null, pMonitor);
			set.setIdentifierString(setIdentifier, pMonitor);
		}
	}

	private static void addConstantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> constants) throws RodinDBException {
		for (String constIdentifier : constants) {
			IConstant constant = internalElement.createChild(IConstant.ELEMENT_TYPE, null, pMonitor);
			constant.setIdentifierString(constIdentifier, pMonitor);
		}
	}

	private static void addAxiomsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> axioms) throws RodinDBException {
		for (String[] labeledAxiom : axioms) {
			IAxiom axiom = internalElement.createChild(IAxiom.ELEMENT_TYPE, null, pMonitor);
			axiom.setLabel(labeledAxiom[0], pMonitor);
			axiom.setPredicateString(labeledAxiom[1], pMonitor);
		}
	}

	/* machine methods */

	public static void initiateMachine(IInternalElement rodinRoot, IProgressMonitor pMonitor,
			LLMResponseParser parser, JSONObject json)
			throws RodinDBException {

		System.out.println(json);

		// parse response
		List<String> refines = parser.getRefines(json);
		List<String> sees = parser.getSees(json);
		List<String> variables = parser.getVariables(json);
		List<String[]> invariants = parser.getInvariants(json);
		List<String[]> variants = parser.getVariants(json);
		List<Map<String, Object>> events = parser.getEvents(json);

		// TODO: add this later
//		addRefineMachineChildren(rodinRoot, pMonitor, refines);
		addSeeContextChildren(rodinRoot, pMonitor, sees);
		addVariablesChildren(rodinRoot, pMonitor, variables);
		addInvariantsChildren(rodinRoot, pMonitor, invariants);
		addVariantsChildren(rodinRoot, pMonitor, variants);
		addEventsChildren(rodinRoot, pMonitor, events);

//		// init event
//		final IEvent init = rodinRoot.createChild(IEvent.ELEMENT_TYPE, null, pMonitor);
//		init.setLabel(IEvent.INITIALISATION, pMonitor);
//		init.setConvergence(IConvergenceElement.Convergence.ORDINARY, pMonitor);
//		init.setExtended(false, pMonitor);
	}

	private static void addRefineMachineChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> refines) throws RodinDBException {
		for (String refIdentifier : refines) {
			IRefinesMachine refinedMachine = internalElement.createChild(IRefinesMachine.ELEMENT_TYPE, null, pMonitor);
			refinedMachine.setAbstractMachineName(refIdentifier, pMonitor);
		}
	}

	private static void addSeeContextChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String> sees)
			throws RodinDBException {
		for (String seeIdentifer : sees) {
			ISeesContext seenContext = internalElement.createChild(ISeesContext.ELEMENT_TYPE, null, pMonitor);
			seenContext.setSeenContextName(seeIdentifer, pMonitor);
		}
	}

	private static void addVariablesChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> variables) throws RodinDBException {
		for (String varIndentifier : variables) {
			IVariable variable = internalElement.createChild(IVariable.ELEMENT_TYPE, null, pMonitor);
			variable.setIdentifierString(varIndentifier, pMonitor);
		}
	}

	private static void addInvariantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> invariants) throws RodinDBException {
		for (String[] labeledInvariant : invariants) {
			IInvariant invariant = internalElement.createChild(IInvariant.ELEMENT_TYPE, null, pMonitor);
			invariant.setLabel(labeledInvariant[0], pMonitor);
			invariant.setPredicateString(labeledInvariant[1], pMonitor);
		}
	}

	private static void addVariantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> variants) throws RodinDBException {
		for (String[] labeledVariant : variants) {
			IVariant variant = internalElement.createChild(IVariant.ELEMENT_TYPE, null, pMonitor);
			variant.setLabel(labeledVariant[0], pMonitor);
			variant.setExpressionString(labeledVariant[1], pMonitor);
		}
	}

	/* helper methods for events */

	private static void addEventsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<Map<String, Object>> events) throws RodinDBException {
		for (Map<String, Object> eventInfo : events) {
			IEvent event = internalElement.createChild(IEvent.ELEMENT_TYPE, null, pMonitor);
			for (String key : eventInfo.keySet()) {
				if (key.equals(SchemaKeys.EVENT_NAME))
					event.setLabel((String) eventInfo.get(key), pMonitor);
				// TODO: add this later
//				if (key.equals(SchemaKeys.REFINES))
//					addRefinesEventsChildren(event, pMonitor, (List<String>) eventInfo.get(key));
				if (key.equals(SchemaKeys.ANY))
					addParametersChildren(event, pMonitor, (List<String>) eventInfo.get(key));
				if (key.equals(SchemaKeys.WHERE))
					addGuardsChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
				if (key.equals(SchemaKeys.WITH))
					addWitnessesChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
				if (key.equals(SchemaKeys.THEN))
					addActionsChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
			}
			event.setConvergence(IConvergenceElement.Convergence.ORDINARY, pMonitor);
			event.setExtended(false, pMonitor);
		}
	}

	private static void addRefinesEventsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> refines) throws RodinDBException {
		for (String refIdentifier : refines) {
			IRefinesEvent refinedEvent = internalElement.createChild(IRefinesEvent.ELEMENT_TYPE, null, pMonitor);
			refinedEvent.setAbstractEventLabel(refIdentifier, pMonitor);
		}
	}

	private static void addParametersChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> parameters) throws RodinDBException {
		for (String pIndentifier : parameters) {
			IParameter parameter = internalElement.createChild(IParameter.ELEMENT_TYPE, null, pMonitor);
			parameter.setIdentifierString(pIndentifier, pMonitor);
		}
	}

	private static void addGuardsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String[]> guards)
			throws RodinDBException {
		for (String[] guard : guards) {
			IGuard labeledGuard = internalElement.createChild(IGuard.ELEMENT_TYPE, null, pMonitor);
			labeledGuard.setLabel(guard[0], pMonitor);
			labeledGuard.setPredicateString(guard[1], pMonitor);
		}
	}

	private static void addWitnessesChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> witnesses) throws RodinDBException {
		for (String[] witness : witnesses) {
			IWitness labeledWitness = internalElement.createChild(IWitness.ELEMENT_TYPE, null, pMonitor);
			labeledWitness.setLabel(witness[0], pMonitor);
			labeledWitness.setPredicateString(witness[1], pMonitor);
		}
	}

	private static void addActionsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String[]> actions)
			throws RodinDBException {
		for (String[] action : actions) {
			IAction labeledAction = internalElement.createChild(IAction.ELEMENT_TYPE, null, pMonitor);
			labeledAction.setLabel(action[0], pMonitor);
			labeledAction.setAssignmentString(action[1], pMonitor);
		}
	}

}
