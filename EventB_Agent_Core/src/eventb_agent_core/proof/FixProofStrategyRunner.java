package eventb_agent_core.proof;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
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
import org.json.JSONObject;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.utils.proof.PredicateUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class FixProofStrategyRunner {

	private IPOSequent poSequent;
	private IMachineRoot machineRoot;
	private String poOwnerName;

	private static final String PO_OWNER_NAME = "POFixer"; // share the same name

	public FixProofStrategyRunner(IPOSequent poSequent, IMachineRoot machineRoot) {
		this(poSequent, machineRoot, PO_OWNER_NAME);
	}

	public FixProofStrategyRunner(IPOSequent poSequent, IMachineRoot machineRoot, String poOwnerName) {
		this.poSequent = poSequent;
		this.machineRoot = machineRoot;
		this.poOwnerName = poOwnerName;
	}

	/**
	 * Run auto provers and SMT solvers.
	 * 
	 * @throws CoreException
	 */
	public void runAutoProvers() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		// auto provers
//		applyLasoo();
		applyPostTactic();
		if (ProofUtils.isDischarged(machineRoot, poOwnerName)) {
			ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
			return;
		}

		// SMT solvers
//		node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
//		applyLasoo();
//		ITactic smt = new SMTAutoTactic();
//		smt.apply(node, null);
//
//		IProofTreeNode lastNode = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
//		while (lastNode != node) {
//			applyLasoo();
//			smt.apply(lastNode, null);
//			node = lastNode;
//			lastNode = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
//		}
//
//		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
//		return;
	}

	public void applySMT() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
		ITactic smt = new SMTAutoTactic();
		smt.apply(node, null);
		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applyLasoo() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
		Tactics.lasoo().apply(node, null);
		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public Object addHypothesis(String hypothesis) throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		// insert lemma
		ITactic insertLemmaTactic = Tactics.insertLemma(hypothesis);
		return insertLemmaTactic.apply(node, null);
	}

	public Object addAbstractExpression(String hypothesis) throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		// insert lemma
		ITactic abstractExpressionTactic = Tactics.abstrExpr(hypothesis);
		return abstractExpressionTactic.apply(node, null);
	}

	public Object applyProofTactic(String predicate, int nodeID, ProofFixingStrategies strategy)
			throws RodinDBException {
		String reasonerID = "org.eventb.core.seqprover";
		switch (strategy) {
		case cardinalityDefinition:
			reasonerID += ".cardDefRewrites";
			break;
		case disjunctionToImplication:
			reasonerID += ".disjToImplRewrites";
			break;
		case doubleImplication:
			reasonerID += ".doubleImplHypRewrites";
			break;
		case equalCardinality:
			reasonerID += ".equalCardRewrites";
			break;
		case equivalence:
			reasonerID += ".eqvRewrites";
			break;
		case finiteDefinition:
			reasonerID += ".finiteDefRewrites";
			break;
		case functionalImageDefinition:
			reasonerID += ".funImgSimplifies";
			break;
		case implicationAnd:
			reasonerID += ".impAndRewrites";
			break;
		case implicationOr:
			reasonerID += ".impOrRewrites";
			break;
		case inclusionSetMinus:
			reasonerID += ".inclusionSetMinusLeftRewrites"; // or inclusionSetMinusRightRewrites
			break;
		case removeInclusion:
			reasonerID += ".ri";
			break;
		case removeMembership:
			reasonerID += ".rmL2";
			break;
		case removeNegation:
			reasonerID += ".rn";
			break;
		case setEqual:
			reasonerID += ".setEqlRewrites";
			break;
		case setMinus:
			reasonerID += ".setMinusRewrites";
			break;
		case strictInclusion:
			reasonerID += ".sir";
			break;
		case relationOverwriteDefinition:
			reasonerID += ".relOvrRewrites";
		}
		return applyProofTactic(predicate, nodeID, reasonerID);
	}

	private Object applyProofTactic(String predicate, int nodeID, String reasonerID) throws RodinDBException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getProofTreeNode(proofAttempt.getProofTree(), nodeID);

		Predicate predInNode = PredicateUtils.getPredicate(node, predicate);
		List<IPosition> posList = new ArrayList<>();
		posList.add(IPosition.ROOT);
		IPosition left = IPosition.ROOT.getFirstChild();
		IPosition right = left.getNextSibling();
		posList.add(left);
		posList.add(right);
		posList.add(left.getFirstChild());
		posList.add(left.getFirstChild().getNextSibling());

		for (int i = 0; i < posList.size(); i++) {
			IPosition pos = posList.get(i);
			AbstractManualRewrites.Input input = new AbstractManualRewrites.Input(predInNode, pos);
			IReasoner reasoner = SequentProver.getReasonerRegistry().getReasonerDesc(reasonerID).getInstance();
			ITactic tac = BasicTactics.reasonerTac(reasoner, input);
			Object result = tac.apply(node, null);
			if (result == null || i == posList.size() - 1) {
				return result;
			}
		}

		return null;
	}

	public void applyPostTactic() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		while (node != null) {
			ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(machineRoot);
			basicTactics.apply(node, null);
			IProofTreeNode lastNode = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);
			if (node == lastNode) {
				break;
			}
			node = lastNode;
		}

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public Object caseDistinction(String expression) throws CoreException {
		final IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		ITactic directCases = Tactics.doCase(expression);
		return directCases.apply(node, null);
	}

	public Object instantiation(String predicate, JSONArray instantiations) throws RodinDBException {
		final IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastUndischargedNodeFromTree(proofAttempt);

		String[] instantiationsArray = new String[instantiations.length()];

		for (int i = 0; i < instantiations.length(); i++) {
			JSONObject inst = instantiations.getJSONObject(i);
			String name = inst.getString(SchemaKeys.CONST_NAME);
			instantiationsArray[i] = name;
		}

		Predicate predInNode = PredicateUtils.getPredicate(node, predicate);
		if (predInNode == null) {
			predInNode = node.getSequent().goal();
		}

		if (Tactics.allD_applicable(predInNode)) {
			ITactic forAllTactic = Tactics.allmpD(predInNode, instantiationsArray);
			Object result = forAllTactic.apply(node, null);
			return result;
		}

		if (Tactics.exF_applicable(predInNode)) {
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
