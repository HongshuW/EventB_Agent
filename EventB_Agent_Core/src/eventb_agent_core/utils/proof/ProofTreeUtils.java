package eventb_agent_core.utils.proof;

import org.eventb.core.EventBPlugin;
import org.eventb.core.IEventBRoot;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.internal.core.pm.ProofManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.proof.ProofAttemptWrapper;

public class ProofTreeUtils {

	public static ProofAttemptWrapper getLatestProofAttemptWrapper(IProofAttempt proofAttempt, IProofTreeNode node,
			String poName, IEventBRoot eventbRoot) {
		ProofAttemptWrapper wrapper = new ProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
		wrapper.update();
		return wrapper;
	}

	public static void addHypothesis(IProofAttempt proofAttempt, IProofTreeNode node, String poName,
			IEventBRoot eventbRoot, String hypothesis, String... instantiations) throws RodinDBException {
		ProofAttemptWrapper wrapper = getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
		proofAttempt = wrapper.getProofAttempt();
		node = wrapper.getNode();

		// insert lemma
		ITactic insertLemmaTactic = Tactics.insertLemma(hypothesis);
		insertLemmaTactic.apply(node, null);

		// process hypothesis based on predicate type
		Predicate predicate = PredicateUtils.parsePredicate(eventbRoot, hypothesis);
		if (PredicateUtils.isForAllPredicate(predicate)) {
			for (IProofTreeNode childNode : node.getChildNodes()) {
				Predicate predicateInNode = PredicateUtils.getEquivalentPredicate(childNode, predicate);
				if (predicateInNode != null) {
					childNode.pruneChildren();
					ProofTreeUtils.initForAll(proofAttempt, childNode, eventbRoot, predicateInNode, instantiations);
					break;
				}
			}
		}
	}

	public static void initForAll(IProofAttempt proofAttempt, IProofTreeNode node, IEventBRoot eventbRoot,
			Predicate predicate, String... instantiations) throws RodinDBException {
		ITactic forAllTactic = Tactics.allmpD(predicate, instantiations);
		forAllTactic.apply(node, null);
	}

	public static void applyPostTacticAndSave(IProofAttempt proofAttempt, IProofTreeNode node, String poName,
			IEventBRoot eventbRoot) throws RodinDBException {
		ProofAttemptWrapper wrapper = getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
		proofAttempt = wrapper.getProofAttempt();
		node = wrapper.getNode();

		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(eventbRoot);
		basicTactics.apply(node, null);

		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		proofAttempt.commit(true, false, null);
	    bpo.save(null, true);
	    bps.save(null, true);
	}

	public static IProofTreeNode getLastNodeFromTree(IProofAttempt attempt) {
		IProofTree tree = attempt.getProofTree();
		if (tree == null || attempt.isDisposed()) {
			return null;
		}

		IProofTreeNode node = tree.getRoot();
		while (true) {
			IProofTreeNode[] kids = node.getChildNodes();
			if (kids.length == 0) {
				return node;
			}
			node = kids[kids.length - 1];
		}
	}

	public static IProofAttempt getProofAttempt(IProofTree tree, IEventBRoot root) {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(root);
		for (IProofAttempt proofAttempt : proofComponent.getProofAttempts()) {
			if (proofAttempt.isDisposed()) {
				continue;
			}
			if (tree == proofAttempt.getProofTree()) {
				return proofAttempt;
			}
		}
		return null;
	}

}
