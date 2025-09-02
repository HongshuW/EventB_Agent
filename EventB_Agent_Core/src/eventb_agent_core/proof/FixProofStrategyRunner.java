package eventb_agent_core.proof;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ast.IPosition;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.IReasoner;
import org.eventb.core.seqprover.IReasonerRegistry;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.SequentProver;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.core.seqprover.tactics.BasicTactics;
import org.eventb.internal.core.seqprover.eventbExtensions.rewriters.AbstractManualRewrites;
import org.eventb.smt.core.internal.tactics.SMTAutoTactic;
import org.json.JSONArray;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.PredicateUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class FixProofStrategyRunner {

	private String poName;
	private IMachineRoot machineRoot;
	private String poOwnerName;

	private static final String PO_OWNER_NAME = "POFixer"; // share the same name

	public FixProofStrategyRunner(String poName, IMachineRoot machineRoot) {
		this(poName, machineRoot, PO_OWNER_NAME);
	}

	public FixProofStrategyRunner(String poName, IMachineRoot machineRoot, String poOwnerName) {
		this.poName = poName;
		this.machineRoot = machineRoot;
		this.poOwnerName = poOwnerName;
	}

	public IProofAttempt getProofAttempt() throws RodinDBException {
		return ProofUtils.getProofAttempt(poName, machineRoot, poOwnerName);
	}

	/**
	 * Run auto provers and SMT solvers.
	 * 
	 * @throws CoreException
	 */
	public void runAutoProvers() throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();

		// auto provers
		applyAutoTactic();
		if (ProofUtils.isDischarged(machineRoot, poOwnerName)) {
			ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
			return;
		}
	}

	public void applySMT(IProofTreeNode node) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		ITactic smt = new SMTAutoTactic();
		smt.apply(node, null);
		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applySMT() throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		applySMT(node);
	}

	public void applyLasoo(IProofTreeNode node) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		Tactics.lasoo().apply(node, null);
		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applyLasoo() throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
		applyLasoo(node);
	}

	public Object addHypothesis(Hypothesis hypothesis) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		// insert lemma
		String pred = ParserUtils.lex(hypothesis.getPredicate());
		ITactic insertLemmaTactic = Tactics.insertLemma(pred);
		Object result = insertLemmaTactic.apply(node, null);
		if (result != null) {
			return result;
		}

		// instantiations
		String[] instantiations = hypothesis.getInstantiations();
		if (instantiations == null || instantiations.length == 0) {
			return result;
		}
		IProofTreeNode[] children = node.getChildNodes();
		for (IProofTreeNode child : children) {
			Predicate predicate = PredicateUtils.parserPredicate(child, pred);
			Predicate predInNode = PredicateUtils.getPredicate(child, pred);
			Predicate goal = child.getSequent().goal();
			if (predInNode != null) {
				result = instantiation(pred, instantiations, child);
				if (result == null) {
					applyPostTactic(child);
				}
			} else if (goal.toString().equals(predicate.toString())) {
				// hyp in goal
				applySMT(child);
			} else {
				applyPostTactic(child);
			}
		}

		return result;
	}

	public Object addAbstractExpression(String hypothesis) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		// insert lemma
		ITactic abstractExpressionTactic = Tactics.abstrExpr(hypothesis);
		return abstractExpressionTactic.apply(node, null);
	}

	public Object applyProofTactic(int predicateID, int nodeID, ProofFixingStrategies strategy)
			throws RodinDBException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getProofTreeNode(proofAttempt.getProofTree(), nodeID);

		Predicate predInNode = null;
		if (predicateID != 0) {
			List<PredicateWrapper> predWrappers = PredicateUtils.getAllPredicates(node);
			for (PredicateWrapper predWrapper : predWrappers) {
				if (predWrapper.predicateID == predicateID) {
					predInNode = predWrapper.predicate;
					break;
				}
			}
		}
		boolean isGoal = false;
		if (predInNode == null) {
			isGoal = true;
			predInNode = node.getSequent().goal();
		}

		List<IPosition> positions = null;
		IPosition position = IPosition.ROOT;

		switch (strategy) {
		case arithmeticRewrite:
			positions = Tactics.arithGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.arithRewrites(isGoal ? null : predInNode, position).apply(node, null);
		case cardinalityDefinition:
			positions = Tactics.cardDefGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.cardDef(isGoal ? null : predInNode, position).apply(node, null);
		case conjunction:
			return Tactics.conjF(predInNode).apply(node, null);
		case disjunctionToImplication:
			positions = Tactics.disjToImplGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.disjToImpl(isGoal ? null : predInNode, position).apply(node, null);
		case equality:
			return Tactics.eqE(predInNode).apply(node, null);
		case equivalence:
			Object result = Tactics.eqvRL(predInNode).apply(node, null);
			if (result != null) {
				result = Tactics.eqvLR(predInNode).apply(node, null);
			}
			return result;
		case finiteDefinition:
			positions = Tactics.finiteDefGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.finiteDef(isGoal ? null : predInNode, position).apply(node, null);
		case functionalImageDefinition:
			positions = Tactics.funImgSimpGetPositions(predInNode, node.getSequent());
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return (isGoal ? Tactics.funImgGoal(predInNode, position) : Tactics.funImgSimplifies(predInNode, position)).apply(node, null);
		case implicationAnd:
			positions = Tactics.impAndGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.impAndRewrites(isGoal ? null : predInNode, position).apply(node, null);
		case implicationOr:
			positions = Tactics.impOrGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.impOrRewrites(isGoal ? null : predInNode, position).apply(node, null);
		case removeInclusion:
			positions = Tactics.riGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.removeInclusion(isGoal ? null : predInNode, position).apply(node, null);
		case removeMembership:
			positions = Tactics.rmGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.removeMembership(isGoal ? null : predInNode, position).apply(node, null);
		case removeNegation:
			positions = Tactics.rnGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.removeNeg(isGoal ? null : predInNode, position).apply(node, null);
		case setEqual:
			positions = Tactics.setEqlGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.setEqlRewrites(isGoal ? null : predInNode, position).apply(node, null);
		case setMinus:
			positions = Tactics.setMinusGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.setMinusRewrites(isGoal ? null : predInNode, position).apply(node, null);
		case strictInclusion:
			positions = Tactics.sirGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.removeStrictInclusion(isGoal ? null : predInNode, position).apply(node, null);
		case relationOverwriteDefinition:
			positions = Tactics.relOvrGetPositions(predInNode);
			if (positions != null && !positions.isEmpty()) {
				position = positions.get(0);
			}
			return Tactics.relOvr(isGoal ? null : predInNode, position).apply(node, null);
		}

		return null;
	}

	public void applyAutoTactic(IProofTreeNode node) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();

		ITactic autoTactics = EventBPlugin.getAutoPostTacticManager().getSelectedAutoTactics(machineRoot);
		ITactic basicTactics = BasicTactics.onAllPending(autoTactics);
		basicTactics.apply(node, null);

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applyAutoTactic() throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = proofAttempt.getProofTree().getRoot();

		applyAutoTactic(node);
	}

	private void applyPostTactic(IProofTreeNode node) throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();

		ITactic postTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(machineRoot);
		ITactic basicTactics = BasicTactics.onAllPending(postTactics);
		basicTactics.apply(node, null);

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applyPostTactic() throws CoreException {
		IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = proofAttempt.getProofTree().getRoot();

		applyPostTactic(node);
	}

	public Object caseDistinction(String expression) throws CoreException {
		final IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		ITactic directCases = Tactics.doCase(expression);
		return directCases.apply(node, null);
	}

	private Object instantiation(String predicate, String[] instantiationsArray, IProofTreeNode node)
			throws RodinDBException {

		Predicate predInNode = PredicateUtils.getPredicate(node, predicate);
		if (predInNode == null) {
			predInNode = node.getSequent().goal();
		}

		if (Tactics.allD_applicable(predInNode)) {
			node.pruneChildren();
			ITactic forAllTactic = Tactics.allmpD(predInNode, instantiationsArray);
			Object result = forAllTactic.apply(node, null);
			return result;
		}

		if (Tactics.exF_applicable(predInNode)) {
			node.pruneChildren();
			StringBuilder input = new StringBuilder();
			for (int i = 0; i < instantiationsArray.length; i++) {
				String inst = instantiationsArray[i];
				input.append(inst);
				if (i < instantiationsArray.length - 1) {
					input.append(",");
				}
			}
			ITactic existsTactic = Tactics.exF(predInNode, input.toString());
			Object result = existsTactic.apply(node, null);
			return result;
		}

		return null;
	}

	public Object instantiation(String predicate, JSONArray instantiations, int nodeID) throws RodinDBException {
		String[] instantiationsArray = new String[instantiations.length()];

		for (int i = 0; i < instantiations.length(); i++) {
			String inst = instantiations.getString(i);
			instantiationsArray[i] = inst;
		}

		final IProofAttempt proofAttempt = getProofAttempt();
		IProofTreeNode node = ProofUtils.getProofTreeNode(proofAttempt.getProofTree(), nodeID);
		return instantiation(predicate, instantiationsArray, node);
	}

	public List<String> getApplicableReasoners(IProofTreeNode node) {
		List<String> applicable = new ArrayList<>();
		IReasonerRegistry registry = SequentProver.getReasonerRegistry();
		String[] reasonerIds = registry.getRegisteredIDs();

		for (String reasonerId : reasonerIds) {
			try {
				AbstractManualRewrites.Input input = new AbstractManualRewrites.Input(null, IPosition.ROOT);

				// Create a tactic for this reasoner
				IReasoner reasoner = SequentProver.getReasonerRegistry().getReasonerDesc(reasonerId).getInstance();
				ITactic tactic = BasicTactics.reasonerTac(reasoner, input);

				// Test if it's applicable (create a copy of the node to test)
				Object rule = tactic.apply(node, null);

				if (rule == null) {
					applicable.add(reasonerId);
					node.pruneChildren();
				}
			} catch (Exception e) {
			}
		}

		return applicable;
	}

}
