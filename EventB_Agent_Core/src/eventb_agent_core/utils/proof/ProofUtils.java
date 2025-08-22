package eventb_agent_core.utils.proof;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.IPSRoot;
import org.eventb.core.IPSStatus;
import org.eventb.core.ast.Predicate;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IConfidence;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.internal.core.pm.ProofManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.proof.ProofAttemptWrapper;

public class ProofUtils {

	public static ProofAttemptWrapper getLatestProofAttemptWrapper(IProofAttempt proofAttempt, IProofTreeNode node,
			String poName, IEventBRoot eventbRoot) {
		ProofAttemptWrapper wrapper = new ProofAttemptWrapper(proofAttempt, node, poName, eventbRoot);
		wrapper.update();
		return wrapper;
	}

	public static void initForAll(IProofAttempt proofAttempt, IProofTreeNode node, IEventBRoot eventbRoot,
			Predicate predicate, String... instantiations) throws RodinDBException {
		ITactic forAllTactic = Tactics.allmpD(predicate, instantiations);
		forAllTactic.apply(node, null);
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

	public static IProofAttempt getProofAttempt(IPOSequent poSequent, IMachineRoot machineRoot, String poOwnerName)
			throws RodinDBException {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		String poName = poSequent.getElementName();

		IProofAttempt proofAttempt = proofComponent.getProofAttempt(poName, poOwnerName);
		if (proofAttempt == null) {
			proofAttempt = proofComponent.createProofAttempt(poName, poOwnerName, null);
		}

		return proofAttempt;
	}

	public static void saveProofAttempt(IMachineRoot machineRoot, IProofAttempt proofAttempt) throws CoreException {
		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		proofAttempt.commit(false, false, null);
		bpo.save(null, true);
		bps.save(null, true);

		if (isDischarged(machineRoot, proofAttempt.getName())) {
			proofAttempt.dispose();
		}
	}

	public static boolean isDischarged(IMachineRoot machineRoot, String poName) throws RodinDBException, CoreException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		pc.save(null, true);

		IPSRoot psRoot = pc.getPSRoot();

		boolean found = false;

		for (IPSStatus st : psRoot.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			String stPOName = st.getPOSequent().getElementName();
			if (poName.equals(stPOName)) {
				found = true;

				// Treat broken/missing proofs as NOT discharged
				if (st.isBroken()) {
					return false;
				}

				int conf = st.getConfidence();

				// Consider discharged only at/above the discharged threshold
				if (conf < IConfidence.DISCHARGED_MAX) {
					return false;
				}
			}
		}

		return found;
	}

}
