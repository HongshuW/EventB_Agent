package eventb_agent_core.proof;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.IPosition;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.RelationalPredicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.core.seqprover.tactics.BasicTactics;
import org.eventb.internal.core.seqprover.eventbExtensions.rewriters.AbstractManualRewrites;
import org.eventb.internal.core.seqprover.eventbExtensions.rewriters.RemoveMembership;
import org.eventb.smt.core.internal.tactics.SMTAutoTactic;
import org.rodinp.core.RodinDBException;

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
		IProofTreeNode node = ProofUtils.getLastNodeFromTree(proofAttempt);

		// auto provers
		applyPostTactic();
		if (ProofUtils.isDischarged(machineRoot, poOwnerName)) {
			return;
		}

		// SMT solvers
		node = ProofUtils.getLastNodeFromTree(proofAttempt);
		ITactic smt = new SMTAutoTactic();
		smt.apply(node, null);

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void applyLasoo() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastNodeFromTree(proofAttempt);
		Tactics.lasoo().apply(node, null);
		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void addHypothesis(String hypothesis, String... instantiations) throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastNodeFromTree(proofAttempt);

		// insert lemma
		ITactic insertLemmaTactic = Tactics.insertLemma(hypothesis);
		insertLemmaTactic.apply(node, null);

		// process hypothesis based on predicate type
		Predicate predicate = PredicateUtils.parsePredicate(machineRoot, hypothesis);
		if (PredicateUtils.isForAllPredicate(predicate)) {
			for (IProofTreeNode childNode : node.getChildNodes()) {
				Predicate predicateInNode = PredicateUtils.getPredicate(childNode, predicate);
				if (predicateInNode != null) {
					childNode.pruneChildren();
					ProofUtils.initForAll(proofAttempt, childNode, machineRoot, predicateInNode, instantiations);
					break;
				}
			}
		}

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

	public void removeMembership(String predicate, int nodeID) throws RodinDBException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getProofTreeNode(proofAttempt.getProofTree(), nodeID);

		Predicate predInNode = PredicateUtils.getPredicate(node, predicate);
		IPosition pos;
		if (predInNode != null) {
			if (!RemoveMembership.DEFAULT.isApplicableTo(predInNode)) {
				return;
			}
			pos = findMembershipPositionRecursive(predInNode, IPosition.ROOT);
		} else {
			Predicate goal = node.getSequent().goal();
			if (!RemoveMembership.DEFAULT.isApplicableTo(goal)) {
				return;
			}
			pos = findMembershipPositionRecursive(goal, IPosition.ROOT);
		}

		AbstractManualRewrites.Input input = new AbstractManualRewrites.Input(predInNode, pos);
		ITactic tac = BasicTactics.reasonerTac(RemoveMembership.DEFAULT, input);
		Object result = tac.apply(node, null);
	}

	private IPosition findMembershipPositionRecursive(Predicate predicate, IPosition currentPos) {
		// Check if current predicate is a membership
		if (predicate instanceof RelationalPredicate) {
			RelationalPredicate relPred = (RelationalPredicate) predicate;
			if (relPred.getTag() == Predicate.IN || relPred.getTag() == Predicate.NOTIN
					|| relPred.getTag() == Predicate.SUBSET || relPred.getTag() == Predicate.SUBSETEQ) {
				return currentPos;
			}
		}

		// Recursively search in children
		for (int i = 0; i < predicate.getChildCount(); i++) {
			if (predicate.getChild(i) instanceof Predicate) {
				IPosition childPos = currentPos.getChildAtIndex(i);
				IPosition result = findMembershipPositionRecursive((Predicate) predicate.getChild(i), childPos);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	// TODO: update this later
//	public void addHypothesis(IProofAttempt proofAttempt, IProofTreeNode node, String poName, IEventBRoot eventbRoot,
//			String hypothesis, String... instantiations) throws RodinDBException {
//		ProofAttemptWrapper wrapper = ProofUtils.getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
//		proofAttempt = wrapper.getProofAttempt();
//		node = wrapper.getNode();
//
//		// insert lemma
//		ITactic insertLemmaTactic = Tactics.insertLemma(hypothesis);
//		insertLemmaTactic.apply(node, null);
//
//		// process hypothesis based on predicate type
//		Predicate predicate = PredicateUtils.parsePredicate(eventbRoot, hypothesis);
//		if (PredicateUtils.isForAllPredicate(predicate)) {
//			for (IProofTreeNode childNode : node.getChildNodes()) {
//				Predicate predicateInNode = PredicateUtils.getEquivalentPredicate(childNode, predicate);
//				if (predicateInNode != null) {
//					childNode.pruneChildren();
//					ProofUtils.initForAll(proofAttempt, childNode, eventbRoot, predicateInNode, instantiations);
//					break;
//				}
//			}
//		}
//	}

	// TODO: update this later
//	public void applyPostTacticAndSave(IProofAttempt proofAttempt, IProofTreeNode node, String poName,
//			IEventBRoot eventbRoot) throws RodinDBException {
//		ProofAttemptWrapper wrapper = ProofUtils.getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
//		proofAttempt = wrapper.getProofAttempt();
//		node = wrapper.getNode();
//
//		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(eventbRoot);
//		basicTactics.apply(node, null);
//
//		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
//		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
//		proofAttempt.commit(true, false, null);
//		bpo.save(null, true);
//		bps.save(null, true);
//
//		proofAttempt.dispose();
//	}

	public void applyPostTactic() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastNodeFromTree(proofAttempt);

		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(machineRoot);
		basicTactics.apply(node, null);

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

}
