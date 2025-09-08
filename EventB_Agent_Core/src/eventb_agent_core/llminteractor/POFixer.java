package eventb_agent_core.llminteractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eventb.core.IAction;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.internal.core.pm.ProofManager;
import org.eventb.internal.core.seqprover.ReasonerFailure;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.evaluation.ComponentType;
import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.proof.FixProofStrategyRunner;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.proof.ProofScenarioType;
import eventb_agent_core.proof.PredicateWrapper;
import eventb_agent_core.proof.ProofFixingStrategies;
import eventb_agent_core.proof.ProofNodeWrapper;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.PredicateUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class POFixer extends AbstractLLMInteractor {

	private static final String PO_OWNER_NAME = "POFixer";
	private static String reasonerMessage = null;

	private IProofAttempt proofAttempt;

	public POFixer(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	/**
	 * Fix model without pre-defined fix strategies.
	 * 
	 * @param machineRoot
	 * @param poSequent
	 * @return
	 * @throws RodinDBException
	 * @throws IOException
	 * @throws ReachMaxAttemptException
	 */
	public JSONObject autoFixPOWithoutStrategy(IMachineRoot machineRoot, IPOSequent poSequent)
			throws RodinDBException, IOException, ReachMaxAttemptException {
		String poName = poSequent.getElementName();
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poName, machineRoot, PO_OWNER_NAME);

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
		IContextRoot contextRoot = null;
		try {
			contextRoot = RetrieveModelUtils.getContextRoot(tree);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		if (tree != null) {
			String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
					ParserUtils.reverseLex(ProofUtils.getProofTreeString(tree), 1) };
			return getLLMResponse(placeHolderContents, LLMRequestTypes.FIX_PROOF_NO_STRATEGY);
		} else {
			System.out.println("Proof tree is null.");
			return null;
		}

	}

	/**
	 * Fix the model based on fix strategy.
	 * 
	 * @param machineRoot
	 * @param poSequent
	 * @param requestHistory TODO
	 * @throws Exception
	 */
	public void autoFixPO(IMachineRoot machineRoot, IPOSequent poSequent,
			List<LinkedHashMap<String, Object>> requestHistory) throws Exception {

		String poName = poSequent.getElementName();
		proofAttempt = ProofUtils.getProofAttempt(poName, machineRoot, PO_OWNER_NAME);

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
		IContextRoot contextRoot = null;
		try {
			contextRoot = RetrieveModelUtils.getContextRoot(tree);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		if (tree != null) {
			try {
				reasonerMessage = null;
				ProofScenarioType poType = getPOType(poName, tree, machineRoot, requestHistory);
				boolean shouldGetAllProofTactics = shouldGetAllProofTactics(poType);
				String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
						ParserUtils.reverseLex(ProofUtils.getProofTreeString(tree), 1),
						shouldGetAllProofTactics
								? getApplicableProofTactics(tree, poType != ProofScenarioType.TRIVIAL_INV)
								: getOtherApplicableFunctions() };
				JSONObject answerWrapper = getLLMResponseWithTools(placeHolderContents, LLMRequestTypes.FIX_PROOF,
						requestHistory, poType);
				JSONArray answer = answerWrapper.getJSONArray("result");
				int limit = 1;
				if (poType == ProofScenarioType.CARD_WD) {
					limit = 2;
				}
				for (int i = 0; i < Math.min(limit, answer.length()); i++) {
					JSONObject functionCall = answer.getJSONObject(i);
					modifyModel(functionCall, machineRoot, contextRoot, poSequent, tree, poType);
					llmRequestSender.getRequestBuilder().addRequestHistory(
							"The PO is not discharged. The Event-B model and proof tree are updated. What to do next?",
							reasonerMessage, requestHistory, functionCall);
				}
				if (!ProofUtils.isDischarged(machineRoot, poName)) {
					EvaluationManager.endLatestAction();
					EvaluationManager.repeatAndStartPrevoiusAction(maxAttemptsProof);
					EvaluationManager.setLastPOActionIndex();
					autoFixPO(machineRoot, poSequent, requestHistory);
				}
			} catch (ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
				throw new ReachMaxAttemptException(ComponentType.FIX_PROOF.name(), poName);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				throw e;
			}
		} else {
			System.out.println("Proof tree is null or discharged.");
		}

	}

	private void modifyModel(JSONObject answer, IMachineRoot machineRoot, IContextRoot contextRoot,
			IPOSequent poSequent, IProofTree tree, ProofScenarioType poType)
			throws CoreException, InterruptedException, ReachMaxAttemptException {
		String function = answer.getString(Constants.FUNCTION_NAME);
		JSONObject args = null;
		try {
			args = new JSONObject(answer.getString(Constants.FUNCTION_ARGS));
		} catch (JSONException e) {
			EvaluationManager.setErrorToLatestAction(e.getMessage());
			reasonerMessage = e.getMessage();
			return;
		}

		EvaluationManager.setErrorToLatestAction(function + ":" + args.toString());

		ProofFixingStrategies strategy = ProofFixingStrategies.valueOf(function);
		int nodeID = args.getInt(SchemaKeys.NODE_ID);
		FixProofStrategyRunner fixer = new FixProofStrategyRunner(poSequent.getElementName(), machineRoot);

		List<Hypothesis> hypotheses = new ArrayList<>();

		Object result = null;

		switch (strategy) {
		case applyProofTactic:
			String tacticString = args.getString(SchemaKeys.PROOF_TACTIC);
			ProofFixingStrategies tactic = ProofFixingStrategies.valueOf(tacticString);
			int predicateID = args.getInt(SchemaKeys.PRED_ID);
			result = fixer.applyProofTactic(predicateID, nodeID, tactic);
			finish(result, fixer);
			break;
		case addHypothesesToContext:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.HYP);
			addHypothesesToContext(contextRoot, hypotheses);
			waitForPORebuild(fixer);
			fixer.applyAutoTactic();
			if (poType != ProofScenarioType.CARD_WD) {
				for (Hypothesis hypothesis : hypotheses) {
					result = fixer.addHypothesis(hypothesis);
				}
			}
			fixer.runAutoProvers();
			break;
		case addHypothesesToGuard:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.GRD);
			String eventName = args.getString(SchemaKeys.EVENT_NAME);
			addHypothesesToGuard(machineRoot, hypotheses, eventName);
			waitForPORebuild(fixer);
			for (Hypothesis hypothesis : hypotheses) {
				result = fixer.instantiation(hypothesis, 0); // without auto proving, node ID is 0
				finish(result, fixer);
			}
			fixer.runAutoProvers();
			break;
		case addAbstractExpression:
			String expression = args.getJSONObject(SchemaKeys.EXPR).getString(SchemaKeys.EXPR);
			String expr = ParserUtils.lex(expression);
			result = fixer.addAbstractExpression(expr);
			finish(result, fixer);
			break;
		case applySMT:
			fixer.applySMT();
			finish(result, fixer);
			break;
		case applyLasoo:
			fixer.applyLasoo();
			finish(result, fixer);
			break;
		case caseDistinction:
			expression = args.getString(SchemaKeys.EXPR);
			expr = ParserUtils.lex(expression);
			result = fixer.caseDistinction(expr);
			finish(result, fixer);
			break;
		case fixThroughModelChecking:
			JSONArray parameters = args.getJSONArray(SchemaKeys.MODEL_CHECKING_PARAMS);
			ModelCheckingFixer modelCheckingFixer = new ModelCheckingFixer(llmRequestSender, llmResponseParser);
			String modelCheckingResult = modelCheckingFixer.getModelCheckingResult(machineRoot, parameters);
			if (modelCheckingResult != null) {
				reasonerMessage = modelCheckingResult;
			}
			break;
		case instantiation:
			String predicate = args.getString(SchemaKeys.PRED);
			String pred = ParserUtils.lex(predicate);
			JSONArray instantiations = args.getJSONArray(SchemaKeys.INSTANTIATIONS);
			result = fixer.instantiation(pred, instantiations, nodeID);
			finish(result, fixer);
			break;
		case selectHypothesisFromContext:
			String axiomLabel = args.getString(SchemaKeys.AXM_LABEL);
			IAxiom axiom = getAxiom(contextRoot, axiomLabel);
			pred = axiom == null ? "" : axiom.getPredicateString();
			instantiations = args.getJSONArray(SchemaKeys.INSTANTIATIONS);
			String[] insts = new String[instantiations.length()];
			for (int i = 0; i < instantiations.length(); i++) {
				insts[i] = ParserUtils.lex(instantiations.getString(i));
			}
			Hypothesis hyp = new Hypothesis(axiomLabel, pred, insts);
			result = fixer.addHypothesis(hyp);
			if (hyp.getPredicate().equals("")) {
				reasonerMessage = "The hypothesis doesn't exist in the context.";
			}
			fixer.runAutoProvers();
			break;
		case strengthenInvariant:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.INV);
			strengthenInvariants(machineRoot, hypotheses);
			waitForPORebuild(fixer);
			fixer.runAutoProvers();
			break;
		case strengthenGuard:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.GRD);
			eventName = args.getString(SchemaKeys.EVENT_NAME);
			String[] poNameElements = poSequent.getElementName().split("/");
			String grdInPO = poNameElements.length == 3 ? poNameElements[1] : null;
			strengthenGuard(machineRoot, hypotheses, eventName, grdInPO);
			waitForPORebuild(fixer);
			fixer.runAutoProvers();
			break;
		case updateAction:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.ACT, SchemaKeys.ASSIGN);
			eventName = args.getString(SchemaKeys.EVENT_NAME);
			updateAction(machineRoot, hypotheses, eventName);
			waitForPORebuild(fixer);
			fixer.runAutoProvers();
			break;
		default:
			finish(null, fixer);
		}
	}

	private ProofScenarioType getPOType(String poName, IProofTree tree, IMachineRoot machineRoot,
			List<LinkedHashMap<String, Object>> requestHistory) throws RodinDBException {

		String lastAction = getLastAction(requestHistory);

		if (lastAction.equals("addHypothesesToContext") || lastAction.equals("addHypothesesToGuard")) {
			return ProofScenarioType.ADDED_HYP;
		}
		if (poName.contains("gluing_inv_")) {
			return ProofScenarioType.GLUING_INV;
		} else if (poName.contains("EQL")) {
			return ProofScenarioType.EQL_PO;
		}

		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(tree);
		String predicateString = node.getSequent().goal().toString();
		predicateString = ParserUtils.reverseLex(predicateString);
		if (predicateString.equals("\\bot")) {
			return ProofScenarioType.CONTRADICT_GOAL;
		}

		String[] entries = poName.split("/");
		if (entries.length == 2) {
			if (entries[1].equals("WD")) {
				String invariantLabel = entries[0];
				IInvariant invariant = getInvariant(machineRoot, invariantLabel);
				if (invariant != null) {
					String invString = ParserUtils.reverseLex(invariant.getPredicateString());
					if (invString.startsWith("card(")) {
						return ProofScenarioType.CARD_WD;
					} else {
						return ProofScenarioType.WD;
					}
				}
			}
		} else if (entries.length == 3) {
			if (entries[2].equals("WD")) {
				return ProofScenarioType.WD;
			} else if (entries[2].equals("INV")) {
				String invariantLabel = entries[1];
				IInvariant invariant = getInvariant(machineRoot, invariantLabel);
				if (invariant != null) {
					String invString = ParserUtils.reverseLex(invariant.getPredicateString());
					if (invString.startsWith("\\forall") || invString.startsWith("\\exists")) {
						if (lastAction.equals("instantiation")) {
							return ProofScenarioType.TRIVIAL_INV;
						} else {
							return ProofScenarioType.QUANT_INV;
						}
					}
				}
				if (node != null) {
					Set<String> applicableTactics = getApplicableProofTacticsOfGoal(node);
					boolean onlyRmApplicable = applicableTactics.contains(ProofFixingStrategies.removeMembership.name())
							&& applicableTactics.size() == 1;
					if (predicateString.startsWith("{}") || onlyRmApplicable) {
						return ProofScenarioType.TRIVIAL_INV;
					} else if (predicateString.startsWith("\\exists")) {
						return ProofScenarioType.EXST_IN_GOAL;
					}
				}
			}
		}

		return ProofScenarioType.INV;
	}

	private String getLastAction(List<LinkedHashMap<String, Object>> requestHistory) {
		String lastAction = "";
		for (LinkedHashMap<String, Object> entry : requestHistory) {
			if (entry.containsKey("type") && entry.get("type").equals("function_call")) {
				lastAction = (String) entry.get("name");
			}
		}
		return lastAction;
	}

	private boolean shouldGetAllProofTactics(ProofScenarioType poType) {
		return poType == ProofScenarioType.INV || poType == ProofScenarioType.TRIVIAL_INV
				|| poType == ProofScenarioType.GLUING_INV;
	}

	private IAxiom getAxiom(IContextRoot contextRoot, String label) throws RodinDBException {
		IAxiom[] axioms = contextRoot.getAxioms();
		for (IAxiom axiom : axioms) {
			if (axiom.getLabel().equals(label)) {
				return axiom;
			}
		}
		return null;
	}

	private IInvariant getInvariant(IMachineRoot machineRoot, String label) throws RodinDBException {
		IInvariant[] invariants = machineRoot.getInvariants();
		for (IInvariant invariant : invariants) {
			if (invariant.getLabel().equals(label)) {
				return invariant;
			}
		}
		return null;
	}

	private IEvent getEvent(IMachineRoot machineRoot, String label) throws RodinDBException {
		IEvent[] events = machineRoot.getEvents();
		for (IEvent event : events) {
			if (event.getLabel().equals(label)) {
				return event;
			}
		}
		return null;
	}

	private IGuard getGuard(IEvent event, String label) throws RodinDBException {
		IGuard[] guards = event.getGuards();
		for (IGuard guard : guards) {
			if (guard.getLabel().equals(label)) {
				return guard;
			}
		}
		return null;
	}

	private IAction getAction(IEvent event, String label) throws RodinDBException {
		IAction[] actions = event.getActions();
		for (IAction action : actions) {
			if (action.getLabel().equals(label)) {
				return action;
			}
		}
		return null;
	}

	private void finish(Object result, FixProofStrategyRunner fixer) throws CoreException {
		if (result instanceof ReasonerFailure) {
			ReasonerFailure fail = (ReasonerFailure) result;
			reasonerMessage = fail.getReason();
		}

		fixer.runAutoProvers();
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
		} catch (OperationCanceledException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void waitForPORebuild(FixProofStrategyRunner fixer) throws RodinDBException, InterruptedException {
		Thread.sleep(1000);
		if (this.proofAttempt != null && (this.proofAttempt.isBroken() || this.proofAttempt.isDisposed())) {
			this.proofAttempt.dispose();
		}
		while (this.proofAttempt != null && (this.proofAttempt.isBroken() || this.proofAttempt.isDisposed())) {
			Thread.sleep(1000);
			this.proofAttempt = fixer.getProofAttempt();
		}
	}

	private void addHypothesesToContext(IContextRoot contextRoot, List<Hypothesis> hypotheses) throws CoreException {
		IRodinFile rodinFile = contextRoot.getRodinFile();

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IAxiom a = getAxiom(contextRoot, label);
				if (a == null) {
					// add new axiom
					IAxiom newAxiom = contextRoot.createChild(IAxiom.ELEMENT_TYPE, null, null);
					newAxiom.setLabel(label, null);
					newAxiom.setPredicateString(predicate, null);
				} else {
					a.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	private void addHypothesesToGuard(IMachineRoot machineRoot, List<Hypothesis> hypotheses, String targetEventName) {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		try {
			IEvent targetEvent = getEvent(machineRoot, targetEventName);
			if (targetEvent == null) {
				return;
			}

			for (int i = 0; i < hypotheses.size(); i++) {
				Hypothesis hyp = hypotheses.get(i);
				String label = hyp.getLabel();
				String predicate = ParserUtils.lex(hyp.getPredicate());

				IGuard targetGuard = getGuard(targetEvent, label);
				if (targetGuard == null) {
					IGuard guard = targetEvent.createChild(IGuard.ELEMENT_TYPE, null, null);
					guard.setLabel(label, null);
					guard.setPredicateString(predicate, null);
				} else {
					targetGuard.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

	}

	private void strengthenInvariants(IMachineRoot machineRoot, List<Hypothesis> hypotheses) {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IInvariant targetInvariant = getInvariant(machineRoot, label);
				if (targetInvariant == null) {
					// add new invariant
					IInvariant newInv = machineRoot.createChild(IInvariant.ELEMENT_TYPE, null, null);
					newInv.setLabel(label, null);
					newInv.setPredicateString(predicate, null);
				} else {
					targetInvariant.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	private void strengthenGuard(IMachineRoot machineRoot, List<Hypothesis> hypotheses, String eventName,
			String poGrdLabel) throws RodinDBException {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		IEvent targetEvent = getEvent(machineRoot, eventName);
		if (targetEvent == null) {
			return;
		}

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IGuard targetGuard = getGuard(targetEvent, label);
				if (targetGuard == null) {
					// add new guard
					IGuard previousGuard = getGuard(targetEvent, poGrdLabel);
					IGuard newGrd = targetEvent.createChild(IGuard.ELEMENT_TYPE, previousGuard, null);
					newGrd.setLabel(label, null);
					newGrd.setPredicateString(predicate, null);
				} else {
					targetGuard.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateAction(IMachineRoot machineRoot, List<Hypothesis> hypotheses, String eventName)
			throws RodinDBException {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		IEvent targetEvent = getEvent(machineRoot, eventName);
		if (targetEvent == null) {
			return;
		}

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String assignment = ParserUtils.lex(hyp.getPredicate());
			try {
				IAction targetAction = getAction(targetEvent, label);
				if (targetAction == null) {
					// add new action
					IAction newAct = targetEvent.createChild(IAction.ELEMENT_TYPE, null, null);
					newAct.setLabel(label, null);
					newAct.setAssignmentString(assignment, null);
				} else {
					targetAction.setAssignmentString(assignment, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	private String[] applicableFunctions = new String[] { "addAbstractExpression", "addHypothesesToContext",
			"addHypothesesToGuard", "caseDistinction", "fixThroughModelChecking", "instantiation",
			"selectHypothesisFromContext", "strengthenGuard", "strengthenInvariant", "updateAction", "applySMT" };

	public String getOtherApplicableFunctions() {
		StringBuilder applicable = new StringBuilder("applyProofTactic: None\n");
		for (String function : applicableFunctions) {
			applicable.append(function + ", ");
		}
		return applicable.toString();
	}

	public Set<String> getApplicableProofTacticsOfGoal(IProofTreeNode node) {
		Set<String> tactics = new HashSet<>();
		Predicate predicate = node.getSequent().goal();

		if (Tactics.conjF_applicable(predicate)) {
			tactics.add(ProofFixingStrategies.conjunction.name());
		}
		if (Tactics.eqE_applicable(predicate)) {
			tactics.add(ProofFixingStrategies.equality.name());
		}
		if (Tactics.eqv_applicable(predicate)) {
			tactics.add(ProofFixingStrategies.equivalence.name());
		}
		if (Tactics.isRemoveNegationApplicable(predicate)) {
			tactics.add(ProofFixingStrategies.removeNegation.name());
		}
		if (Tactics.isRemoveMembershipApplicable(predicate)) {
			tactics.add(ProofFixingStrategies.removeMembership.name());
		}

		if (Tactics.riGetPositions(predicate) != null && !Tactics.riGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.removeInclusion.name());
		}
		if (Tactics.sirGetPositions(predicate) != null && !Tactics.sirGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.strictInclusion.name());
		}
		if (Tactics.disjToImplGetPositions(predicate) != null && !Tactics.disjToImplGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.disjunctionToImplication.name());
		}
		if (Tactics.impAndGetPositions(predicate) != null && !Tactics.impAndGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.implicationAnd.name());
		}
		if (Tactics.impOrGetPositions(predicate) != null && !Tactics.impOrGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.implicationOr.name());
		}
		if (Tactics.setEqlGetPositions(predicate) != null && !Tactics.setEqlGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.setEqual.name());
		}
		if (Tactics.setMinusGetPositions(predicate) != null && !Tactics.setMinusGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.setMinus.name());
		}
		if (Tactics.relOvrGetPositions(predicate) != null && !Tactics.relOvrGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.relationOverwriteDefinition.name());
		}
		if (Tactics.partitionGetPositions(predicate) != null && !Tactics.partitionGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.partition.name());
		}
		if (Tactics.arithGetPositions(predicate) != null && !Tactics.arithGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.arithmeticRewrite.name());
		}
		if (Tactics.finiteDefGetPositions(predicate) != null && !Tactics.finiteDefGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.finiteDefinition.name());
		}
		if (Tactics.cardDefGetPositions(predicate) != null && !Tactics.cardDefGetPositions(predicate).isEmpty()) {
			tactics.add(ProofFixingStrategies.cardinalityDefinition.name());
		}
		if (Tactics.funImgSimpGetPositions(predicate, node.getSequent()) != null
				&& !Tactics.funImgSimpGetPositions(predicate, node.getSequent()).isEmpty()) {
			tactics.add(ProofFixingStrategies.functionalImageDefinition.name());
		}

		return tactics;
	}

	public String getApplicableProofTactics(IProofTree tree, boolean addOtherFunctions) {
		StringBuilder applicable = new StringBuilder("applyProofTactic: ");
		Set<Map<String, String>> applicableSet = new HashSet<>();

		List<ProofNodeWrapper> nodes = ProofUtils.getUndischargedNodes(tree);
		for (ProofNodeWrapper nodeWrapper : nodes) {
			List<PredicateWrapper> predicateWrappers = PredicateUtils.getAllPredicates(nodeWrapper.node);
			for (PredicateWrapper pred : predicateWrappers) {
				addTactics(nodeWrapper, pred, applicableSet);
			}
			addTactics(nodeWrapper, new PredicateWrapper(nodeWrapper.node.getSequent().goal(), 0, true), applicableSet);
		}

		if (applicableSet.isEmpty()) {
			applicable.append("None");
		}
		for (Map<String, String> tactic : applicableSet) {
			if (tactic != null) {
				applicable.append("{");
				for (String key : tactic.keySet()) {
					applicable.append("\"" + key + "\": ");
					applicable.append("\"" + tactic.get(key) + "\", ");
				}
				applicable.append("},");
			}
		}

		applicable.append("\n");
		if (addOtherFunctions) {
			for (String function : applicableFunctions) {
				applicable.append(function + ", ");
			}
		}

		return applicable.toString();
	}

	private void addTacticString(String tacticName, ProofNodeWrapper nodeWrapper, PredicateWrapper predWrapper,
			Set<Map<String, String>> applicableSet) {
		if (tacticName == "") {
			return;
		}
		Map<String, String> applicable = new HashMap<>();
		applicable.put("proof_tactic", tacticName);
		String predicate = ParserUtils.reverseLex(predWrapper.predicate.toString(), 1);
		applicable.put("predicate_id", String.valueOf(predWrapper.predicateID));
		applicable.put("predicate", predicate);
		applicable.put("node_id", String.valueOf(nodeWrapper.id));
		applicableSet.add(applicable);
	}

	private void addTactics(ProofNodeWrapper nodeWrapper, PredicateWrapper predWrapper,
			Set<Map<String, String>> applicableSet) {

		IProofTreeNode node = nodeWrapper.node;
		Predicate predicate = predWrapper.predicate;

		/* hypothesis */

		if (Tactics.conjF_applicable(predicate)) {
			addTacticString(ProofFixingStrategies.conjunction.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.eqE_applicable(predicate)) {
			addTacticString(ProofFixingStrategies.equality.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.eqv_applicable(predicate)) {
			addTacticString(ProofFixingStrategies.equivalence.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.isRemoveNegationApplicable(predicate)) {
			addTacticString(ProofFixingStrategies.removeNegation.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.isRemoveMembershipApplicable(predicate)) {
			addTacticString(ProofFixingStrategies.removeMembership.name(), nodeWrapper, predWrapper, applicableSet);
		}

		if (Tactics.riGetPositions(predicate) != null && !Tactics.riGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.removeInclusion.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.sirGetPositions(predicate) != null && !Tactics.sirGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.strictInclusion.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.disjToImplGetPositions(predicate) != null && !Tactics.disjToImplGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.disjunctionToImplication.name(), nodeWrapper, predWrapper,
					applicableSet);
		}
		if (Tactics.impAndGetPositions(predicate) != null && !Tactics.impAndGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.implicationAnd.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.impOrGetPositions(predicate) != null && !Tactics.impOrGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.implicationOr.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.setEqlGetPositions(predicate) != null && !Tactics.setEqlGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.setEqual.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.setMinusGetPositions(predicate) != null && !Tactics.setMinusGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.setMinus.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.relOvrGetPositions(predicate) != null && !Tactics.relOvrGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.relationOverwriteDefinition.name(), nodeWrapper, predWrapper,
					applicableSet);
		}
		if (Tactics.partitionGetPositions(predicate) != null && !Tactics.partitionGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.partition.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.arithGetPositions(predicate) != null && !Tactics.arithGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.arithmeticRewrite.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.finiteDefGetPositions(predicate) != null && !Tactics.finiteDefGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.finiteDefinition.name(), nodeWrapper, predWrapper, applicableSet);
		}
		if (Tactics.cardDefGetPositions(predicate) != null && !Tactics.cardDefGetPositions(predicate).isEmpty()) {
			addTacticString(ProofFixingStrategies.cardinalityDefinition.name(), nodeWrapper, predWrapper,
					applicableSet);
		}
		if (Tactics.funImgSimpGetPositions(predicate, node.getSequent()) != null
				&& !Tactics.funImgSimpGetPositions(predicate, node.getSequent()).isEmpty()) {
			addTacticString(ProofFixingStrategies.functionalImageDefinition.name(), nodeWrapper, predWrapper,
					applicableSet);
		}
	}

}
