package eventb_agent_core.proof;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.smt.core.internal.tactics.SMTAutoTactic;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.utils.proof.PredicateUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class FixProofStrategyRunner {

	private IPOSequent poSequent;
	private IMachineRoot machineRoot;
	private String poOwnerName;

	public FixProofStrategyRunner() {
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

	// TODO: update this later
	public void addHypothesis(IProofAttempt proofAttempt, IProofTreeNode node, String poName, IEventBRoot eventbRoot,
			String hypothesis, String... instantiations) throws RodinDBException {
		ProofAttemptWrapper wrapper = ProofUtils.getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
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
					ProofUtils.initForAll(proofAttempt, childNode, eventbRoot, predicateInNode, instantiations);
					break;
				}
			}
		}
	}

	// TODO: update this later
	public void applyPostTacticAndSave(IProofAttempt proofAttempt, IProofTreeNode node, String poName,
			IEventBRoot eventbRoot) throws RodinDBException {
		ProofAttemptWrapper wrapper = ProofUtils.getLatestProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
		proofAttempt = wrapper.getProofAttempt();
		node = wrapper.getNode();

		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(eventbRoot);
		basicTactics.apply(node, null);

		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		proofAttempt.commit(true, false, null);
		bpo.save(null, true);
		bps.save(null, true);

		proofAttempt.dispose();
	}

	public void applyPostTactic() throws CoreException {
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poOwnerName);
		IProofTreeNode node = ProofUtils.getLastNodeFromTree(proofAttempt);

		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(machineRoot);
		basicTactics.apply(node, null);

		ProofUtils.saveProofAttempt(machineRoot, proofAttempt);
	}

}
