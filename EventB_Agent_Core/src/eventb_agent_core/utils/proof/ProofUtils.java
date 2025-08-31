package eventb_agent_core.utils.proof;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eventb.core.EventBPlugin;
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
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.proof.ProofAttemptWrapper;
import eventb_agent_core.proof.ProofNodeWrapper;

public class ProofUtils {

	private static final String PO_OWNER_NAME = "POFixer";

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

	public static IProofTreeNode getLastUndischargedNodeFromTree(IProofAttempt attempt) {
		IProofTree tree = attempt.getProofTree();
		if (tree == null || attempt.isDisposed()) {
			return null;
		}

		IProofTreeNode node = tree.getRoot();
		return getChildNodeHelper(node);
	}

	private static IProofTreeNode getChildNodeHelper(IProofTreeNode node) {
		if (node == null || node.isClosed() || node.getConfidence() == IConfidence.DISCHARGED_MAX) {
			return null;
		}

		IProofTreeNode[] children = node.getChildNodes();
		if (children.length == 0) {
			return node;
		}

		for (IProofTreeNode child : children) {
			IProofTreeNode lastNode = getChildNodeHelper(child);
			if (lastNode != null) {
				return lastNode;
			}
		}

		return null;
	}

	public static IProofTreeNode getLastUndischargedNodeFromTree(IProofTree tree) {
		if (tree == null) {
			return null;
		}

		IProofTreeNode node = tree.getRoot();
		return getChildNodeHelper(node);
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
		proofAttempt.commit(false, false, null);

		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		bpo.save(null, true);
		bps.save(null, true);

		if (isDischarged(machineRoot, proofAttempt.getName())) {
			proofAttempt.dispose();
		}
	}

	public static boolean isDischarged(IMachineRoot machineRoot, String poName) throws CoreException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		pc.save(null, true);

		IPSRoot ps = pc.getPSRoot();

		for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			if (poName.equals(st.getPOSequent().getElementName())) {
				if (st.isBroken() || st.getProof() == null) {
					return isDischargedWithRefresh(machineRoot, poName);
				}
				return st.getConfidence() >= IConfidence.DISCHARGED_MAX;
			}
		}
		return false;
	}

	private static boolean isDischargedWithRefresh(IMachineRoot machineRoot, String poName) throws RodinDBException {
		RodinCore.run((IWorkspaceRunnable) runnable -> {
			try {

				IProject project = machineRoot.getRodinProject().getProject();
				project.build(IncrementalProjectBuilder.FULL_BUILD, null);

				// Refresh Proof Component
				IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
				pc.save(null, true);

				// Get fresh PS root and find the PO
				IPSRoot ps = pc.getPSRoot();
				IPSStatus target = null;
				for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
					if (poName.equals(st.getPOSequent().getElementName())) {
						target = st;
						break;
					}
				}
				if (target == null) {
					return;
				} // PO not found; leave as not discharged

				// If status is broken, delete the obsolete proof and re-prove
				if (target.isBroken()) {
					if (target.getProof() != null && target.getProof().exists()) {
						target.getProof().delete(true, null);
					}

					// Re-fetch handles after deletion
					pc.save(null, true);
					ps = pc.getPSRoot();
					for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
						if (poName.equals(st.getPOSequent().getElementName())) {
							target = st;
							break;
						}
					}

					// Create a fresh attempt, apply auto post tactics, save
					IProofAttempt pa = pc.createProofAttempt(poName, PO_OWNER_NAME, null);
					IProofTreeNode root = pa.getProofTree().getRoot();

					ITactic auto = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(machineRoot);
					auto.apply(root, null);

					pa.commit(true, null);
					pa.dispose();
				}

				// Final refresh after proof changes
				project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
				pc.save(null, true);

			} catch (OperationCanceledException e) {
				// ignore
			}
		}, null, null);

		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		pc.save(null, true);
		IPSRoot ps = pc.getPSRoot();

		for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			if (poName.equals(st.getPOSequent().getElementName())) {
				if (st.isBroken() || st.getProof() == null) {
					return false;
				}
				return st.getConfidence() >= IConfidence.DISCHARGED_MAX;
			}
		}
		return false;
	}

	/**
	 * Node ID starts from 0.
	 * 
	 * @param tree
	 * @return
	 */
	public static String getProofTreeString(IProofTree tree) {
		StringBuilder treeInfo = new StringBuilder();
		String treeString = tree.toString();
		String[] nodes = treeString.split("\n");
		for (int i = 0; i < nodes.length; i++) {
			String node = nodes[i];
			if (i == nodes.length - 1) {
				treeInfo.append(node);
			} else {
				treeInfo.append("NODE_" + String.valueOf(i) + ": ");
				treeInfo.append(node + "\nNODE_" + String.valueOf(i) + "\n\n");
			}
		}
		return treeInfo.toString();
	}

	/**
	 * Node ID starts from 0.
	 * 
	 * @param tree
	 * @param nodeID
	 * @return
	 */
	public static IProofTreeNode getProofTreeNode(IProofTree tree, int nodeID) {
		ProofNodeWrapper targetNode = getNodeHelper(tree.getRoot(), 0, nodeID);
		return targetNode.node;
	}

	private static ProofNodeWrapper getNodeHelper(IProofTreeNode node, int currentID, int targetID) {
		if (currentID == targetID) {
			return new ProofNodeWrapper(node, currentID);
		}

		IProofTreeNode lastNode = null;
		for (IProofTreeNode childNode : node.getChildNodes()) {
			currentID++;
			ProofNodeWrapper nodeWrapper = getNodeHelper(childNode, currentID, targetID);
			if (nodeWrapper.id == targetID) {
				return nodeWrapper;
			}
			currentID = nodeWrapper.id;
			lastNode = nodeWrapper.node;
		}

		return new ProofNodeWrapper(lastNode, currentID);
	}

	public static List<ProofNodeWrapper> getUndischargedNodes(IProofTree tree) {
		List<ProofNodeWrapper> nodes = new ArrayList<>();
		getAllNodesHelper(tree.getRoot(), 0, nodes);
		return nodes;
	}

	private static ProofNodeWrapper getAllNodesHelper(IProofTreeNode node, int currentID,
			List<ProofNodeWrapper> nodes) {
		if (node == null || node.isClosed() || node.getConfidence() == IConfidence.DISCHARGED_MAX) {
			return new ProofNodeWrapper(node, currentID);
		}

		if (node.getChildNodes().length == 0) {
			ProofNodeWrapper wrapper = new ProofNodeWrapper(node, currentID);
			nodes.add(wrapper);
			return wrapper;
		}

		IProofTreeNode lastNode = null;
		for (IProofTreeNode childNode : node.getChildNodes()) {
			currentID++;
			ProofNodeWrapper nodeWrapper = getAllNodesHelper(childNode, currentID, nodes);
			currentID = nodeWrapper.id;
			lastNode = nodeWrapper.node;
		}

		return new ProofNodeWrapper(lastNode, currentID);
	}

}
