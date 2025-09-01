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
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.IPosition;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.IReasoner;
import org.eventb.core.seqprover.IReasonerRegistry;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.SequentProver;
import org.eventb.core.seqprover.tactics.BasicTactics;
import org.eventb.internal.core.pm.ProofManager;
import org.eventb.internal.core.seqprover.ReasonerFailure;
import org.eventb.internal.core.seqprover.eventbExtensions.rewriters.AbstractManualRewrites;
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
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.proof.FixProofStrategyRunner;
import eventb_agent_core.proof.Hypothesis;
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
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, PO_OWNER_NAME);

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
	 * @throws CoreException
	 * @throws ReachMaxAttemptException
	 */
	public void autoFixPO(IMachineRoot machineRoot, IPOSequent poSequent,
			List<LinkedHashMap<String, Object>> requestHistory) throws CoreException, ReachMaxAttemptException {

		String poName = poSequent.getElementName();
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, PO_OWNER_NAME);

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

		if (tree != null && !ProofUtils.isDischargedWithRefresh(machineRoot, poName, tree)) {
			try {
				reasonerMessage = null;
				String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
						ParserUtils.reverseLex(ProofUtils.getProofTreeString(tree), 1),
						getApplicableProofTactics(tree) };
				JSONObject answer = getLLMResponseWithTools(placeHolderContents, LLMRequestTypes.FIX_PROOF,
						requestHistory);
				modifyModel(answer, machineRoot, contextRoot, poSequent, tree);
				if (!ProofUtils.isDischargedWithRefresh(machineRoot, poName, tree)) {
					EvaluationManager.endLatestAction();
					EvaluationManager.repeatAndStartPrevoiusAction(maxAttemptsProof);
					EvaluationManager.setLastPOActionIndex();
					llmRequestSender.getRequestBuilder().addRequestHistory(
							"The PO is not discharged. The Event-B model and proof tree are updated. What to do next?",
							reasonerMessage, requestHistory, answer);
					autoFixPO(machineRoot, poSequent, requestHistory);
				}
			} catch (CoreException e) {
				System.out.println(e.getMessage());
			} catch (ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
				throw new ReachMaxAttemptException(ComponentType.FIX_PROOF.name(), poName);
			}
		} else {
			System.out.println("Proof tree is null or discharged.");
		}

	}

	private void modifyModel(JSONObject answer, IMachineRoot machineRoot, IContextRoot contextRoot,
			IPOSequent poSequent, IProofTree tree) throws CoreException {
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
		FixProofStrategyRunner fixer = new FixProofStrategyRunner(poSequent, machineRoot);

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
			for (Hypothesis hypothesis : hypotheses) {
				result = fixer.addHypothesis(hypothesis);
				finish(result, fixer);
			}
			break;
		case addHypothesesToGuard:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.GRD);
			String eventName = args.getString(SchemaKeys.EVENT_NAME);
			addHypothesesToGuard(machineRoot, hypotheses, eventName);
			for (Hypothesis hypothesis : hypotheses) {
				result = fixer.addHypothesis(hypothesis);
				finish(result, fixer);
			}
			break;
		case addAbstractExpression:
			String expression = args.getString(SchemaKeys.EXPR);
			String expr = ParserUtils.lex(expression);
			result = fixer.addAbstractExpression(expr);
			finish(result, fixer);
			break;
		case applySMT:
			fixer.applySMT();
			break;
		case applyLasoo:
			fixer.applyLasoo();
			break;
		case caseDistinction:
			expression = args.getString(SchemaKeys.EXPR);
			expr = ParserUtils.lex(expression);
			result = fixer.caseDistinction(expr);
			finish(result, fixer);
			break;
		case instantiation:
			String predicate = args.getString(SchemaKeys.PRED);
			String pred = ParserUtils.lex(predicate);
			JSONArray instantiations = args.getJSONArray(SchemaKeys.INSTANTIATIONS);
			result = fixer.instantiation(pred, instantiations, nodeID);
			finish(result, fixer);
			break;
		case strengthenInvariant:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.INV);
			strengthenInvariants(machineRoot, hypotheses);
			break;
		case strengthenGuard:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.GRD);
			eventName = args.getString(SchemaKeys.EVENT_NAME);
			String[] poNameElements = poSequent.getElementName().split("/");
			String grdInPO = poNameElements.length == 3 ? poNameElements[1] : null;
			strengthenGuard(machineRoot, hypotheses, eventName, grdInPO);
			break;
		default:
			fixer.applyPostTactic();
		}

	}

	private void finish(Object result, FixProofStrategyRunner fixer) throws CoreException {
		if (result instanceof ReasonerFailure) {
			ReasonerFailure fail = (ReasonerFailure) result;
			reasonerMessage = fail.getReason();
		}

		fixer.applyPostTactic();
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
		} catch (OperationCanceledException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addHypothesesToContext(IContextRoot contextRoot, List<Hypothesis> hypotheses) throws CoreException {
		IRodinFile rodinFile = contextRoot.getRodinFile();

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IAxiom[] axioms = contextRoot.getChildrenOfType(IAxiom.ELEMENT_TYPE);
				boolean axiomExists = false;
				for (IAxiom a : axioms) {
					if (a.getLabel().equals(label)) {
						// modify the existing axiom
						a.setPredicateString(predicate, null);
						axiomExists = true;
						break;
					}
				}
				if (!axiomExists) {
					// add new axiom
					IAxiom newAxiom = contextRoot.createChild(IAxiom.ELEMENT_TYPE, null, null);
					newAxiom.setLabel(label, null);
					newAxiom.setPredicateString(predicate, null);
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
			IEvent targetEvent = null;
			IEvent[] events = machineRoot.getChildrenOfType(IEvent.ELEMENT_TYPE);
			for (IEvent event : events) {
				if (event.getLabel().equals(targetEventName)) {
					targetEvent = event;
					break;
				}
			}
			if (targetEvent == null) {
				return;
			}

			for (int i = 0; i < hypotheses.size(); i++) {
				Hypothesis hyp = hypotheses.get(i);
				String label = hyp.getLabel();
				String predicate = ParserUtils.lex(hyp.getPredicate());

				boolean guardExists = false;
				IGuard[] guards = targetEvent.getGuards();
				for (IGuard g : guards) {
					if (g.getLabel().equals(label)) {
						guardExists = true;
						break;
					}
				}

				if (!guardExists) {
					IGuard guard = targetEvent.createChild(IGuard.ELEMENT_TYPE, null, null);
					guard.setLabel(label, null);
					guard.setPredicateString(predicate, null);

					rodinFile.save(null, false);
				}
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
				IInvariant[] invariants = machineRoot.getChildrenOfType(IInvariant.ELEMENT_TYPE);
				boolean invExists = false;
				for (IInvariant inv : invariants) {
					if (inv.getLabel().equals(label)) {
						// modify the existing invariant
						inv.setPredicateString(predicate, null);
						invExists = true;
						break;
					}
				}
				if (!invExists) {
					// add new invariant
					IInvariant newInv = machineRoot.createChild(IInvariant.ELEMENT_TYPE, null, null);
					newInv.setLabel(label, null);
					newInv.setPredicateString(predicate, null);
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

		IEvent targetEvent = null;
		IEvent[] events = machineRoot.getChildrenOfType(IEvent.ELEMENT_TYPE);
		for (IEvent event : events) {
			if (event.getLabel().equals(eventName) || event.getLabel() == eventName) {
				targetEvent = event;
				break;
			}
		}
		if (targetEvent == null) {
			return;
		}

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IGuard[] guards = targetEvent.getChildrenOfType(IGuard.ELEMENT_TYPE);
				boolean grdExists = false;
				for (IGuard grd : guards) {
					if (grd.getLabel().equals(label)) {
						// modify the existing guard
						grd.setPredicateString(predicate, null);
						grdExists = true;
						break;
					}
				}
				if (!grdExists) {
					// add new invariant
					IGuard previousGuard = null;
					for (IGuard grd : guards) {
						if (grd.getLabel().equals(poGrdLabel)) {
							previousGuard = grd;
							break;
						}
					}
					IGuard newGrd = targetEvent.createChild(IGuard.ELEMENT_TYPE, previousGuard, null);
					newGrd.setLabel(label, null);
					newGrd.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	public String getApplicableProofTactics(IProofTree tree) {
		StringBuilder applicable = new StringBuilder("applyProofTactic: ");
		Set<Map<String, String>> applicableSet = new HashSet<>();

		List<ProofNodeWrapper> nodes = ProofUtils.getUndischargedNodes(tree);
		for (ProofNodeWrapper nodeWrapper : nodes) {
			IReasonerRegistry registry = SequentProver.getReasonerRegistry();
			String[] reasonerIds = registry.getRegisteredIDs();

			for (String reasonerId : reasonerIds) {
				try {
					List<PredicateWrapper> predicateWrappers = PredicateUtils.getAllPredicates(nodeWrapper.node);
					for (PredicateWrapper pred : predicateWrappers) {
						addTactics(nodeWrapper, pred, reasonerId, applicableSet, IPosition.ROOT);
					}
					addTactics(nodeWrapper, new PredicateWrapper(nodeWrapper.node.getSequent().goal(), 0, true),
							reasonerId, applicableSet, IPosition.ROOT);
				} catch (Exception e) {
					// skip
				}
			}
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
		String[] applicableFunctions = new String[] { "addAbstractExpression", "addHypothesesToContext",
				"addHypothesesToGuard", "caseDistinction", "caseDistinctionBySplittingEvent", "instantiation",
				"strengthenGuard", "strengthenInvariant", "applySMT" };
		for (String function : applicableFunctions) {
			applicable.append(function + ", ");
		}
		return applicable.toString();
	}

	private void addTactics(ProofNodeWrapper nodeWrapper, PredicateWrapper predWrapper, String reasonerId,
			Set<Map<String, String>> applicableSet, IPosition position) {
		IProofTreeNode node = nodeWrapper.node;
		int id = nodeWrapper.id;

		List<IPosition> posList = new ArrayList<>();
		posList.add(position);
		IPosition left = position.getFirstChild();
		IPosition right = left.getNextSibling();
		posList.add(left);
		posList.add(right);
		posList.add(left.getFirstChild());
		posList.add(left.getFirstChild().getNextSibling());

		Predicate pred = predWrapper.isGoal ? null : predWrapper.predicate;

		while (!posList.isEmpty()) {
			IPosition pos = posList.remove(0);
			AbstractManualRewrites.Input input = new AbstractManualRewrites.Input(pred, pos);

			// Create a tactic for this reasoner
			IReasoner reasoner = SequentProver.getReasonerRegistry().getReasonerDesc(reasonerId).getInstance();
			ITactic tactic = BasicTactics.reasonerTac(reasoner, input);

			// Test if it's applicable (create a copy of the node to test)
			Object rule = tactic.apply(node, null);

			if (rule == null) {
				String tacticName = getTacticName(reasonerId);
				if (tacticName == "") {
					return;
				}
				Map<String, String> applicable = new HashMap<>();
				applicable.put("proof_tactic", tacticName);
				String predicate = ParserUtils.reverseLex(predWrapper.predicate.toString(), 1);
				applicable.put("predicate_id", String.valueOf(predWrapper.predicateID));
				applicable.put("predicate", predicate);
				applicable.put("node_id", String.valueOf(id));
				applicableSet.add(applicable);
				node.pruneChildren();
				return;
			}
		}
	}

	private String getTacticName(String reasonerId) {

		if (reasonerId.contains(".ri")) {
			return ProofFixingStrategies.removeInclusion.name();
		} else if (reasonerId.contains(".rm")) {
			return ProofFixingStrategies.removeMembership.name();
		} else if (reasonerId.contains(".cardDefRewrites")) {
			return ProofFixingStrategies.cardinalityDefinition.name();
		} else if (reasonerId.contains(".disjToImplRewrites")) {
			return ProofFixingStrategies.disjunctionToImplication.name();
		} else if (reasonerId.contains(".doubleImplHypRewrites")) {
			return ProofFixingStrategies.doubleImplication.name();
		} else if (reasonerId.contains(".equalCardRewrites")) {
			return ProofFixingStrategies.equalCardinality.name();
		} else if (reasonerId.contains(".eqvRewrites")) {
			return ProofFixingStrategies.equivalence.name();
		} else if (reasonerId.contains(".finiteDefRewrites")) {
			return ProofFixingStrategies.finiteDefinition.name();
		} else if (reasonerId.contains(".funImgSimplifies")) {
			return ProofFixingStrategies.functionalImageDefinition.name();
		} else if (reasonerId.contains(".impAndRewrites")) {
			return ProofFixingStrategies.implicationAnd.name();
		} else if (reasonerId.contains(".impOrRewrites")) {
			return ProofFixingStrategies.implicationOr.name();
		} else if (reasonerId.contains(".inclusionSetMinusLeftRewrites")) {
			return ProofFixingStrategies.inclusionSetMinus.name();
		} else if (reasonerId.contains(".rn")) {
			return ProofFixingStrategies.removeNegation.name();
		} else if (reasonerId.contains(".setEqlRewrites")) {
			return ProofFixingStrategies.setEqual.name();
		} else if (reasonerId.contains(".setMinusRewrites")) {
			return ProofFixingStrategies.setMinus.name();
		} else if (reasonerId.contains(".sir")) {
			return ProofFixingStrategies.strictInclusion.name();
		} else if (reasonerId.contains(".relOvrRewrites")) {
			return ProofFixingStrategies.relationOverwriteDefinition.name();
		}

		return "";
	}

}
