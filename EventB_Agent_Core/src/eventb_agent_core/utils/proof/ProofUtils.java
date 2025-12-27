package eventb_agent_core.utils.proof;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPORoot;
import org.eventb.core.IPRProof;
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

import eventb_agent_core.evaluation.EvaluationManager;
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

	public static IProofAttempt getProofAttempt(String poName, IMachineRoot machineRoot, String poOwnerName) {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);

		IProofAttempt proofAttempt = proofComponent.getProofAttempt(poName, poOwnerName);
		try {
			if (proofAttempt == null) {
				proofAttempt = proofComponent.createProofAttempt(poName, poOwnerName, null);
			} else if (proofAttempt.isBroken()) {
				proofAttempt.dispose();
				proofAttempt = proofComponent.createProofAttempt(poName, poOwnerName, null);
			}
		} catch (RodinDBException e) {
			// PO is discharged, do nothing
			EvaluationManager.setErrorToLatestAction("PO not found");
			EvaluationManager.endLatestAction();
		}

		return proofAttempt;
	}
	
	public static IProofTree getDefaultProofTree(String poName, IMachineRoot machineRoot) throws CoreException {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		
		IPRProof proof = proofComponent.getPRRoot().getProof(poName);
		IProofTree proofTree = proof.getProofTree(null);
		return proofTree;
	}

	public static void saveProofAttempt(IMachineRoot machineRoot, IProofAttempt proofAttempt) throws CoreException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);

		proofAttempt.commit(true, null);
		pc.save(null, true);

		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		bpo.save(null, true);
		bps.save(null, true);

		IPORoot poRoot = proofAttempt.getComponent().getPORoot();
		poRoot.getRodinFile().getResource().refreshLocal(IResource.DEPTH_INFINITE, null);

		if (isDischarged(machineRoot, proofAttempt.getName())) {
			proofAttempt.dispose();
		}
	}

	public static boolean isDischarged(IEventBRoot eventBRoot, String poName) throws RodinDBException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(eventBRoot);
		pc.save(null, true);

		IPSRoot ps = pc.getPSRoot();

		for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			if (poName.equals(st.getPOSequent().getElementName())) {
				return st.getConfidence() >= IConfidence.DISCHARGED_MAX;
			}
		}
		return true;
	}

	public static boolean isDischargedWithRefresh(IMachineRoot machineRoot, String poName) throws RodinDBException {

		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		pc.save(null, true);

		IPSRoot ps = pc.getPSRoot();

		for (IPSStatus st : ps.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			if (poName.equals(st.getPOSequent().getElementName())) {
				boolean isDischarged = st.getConfidence() >= IConfidence.DISCHARGED_MAX;
				int trial = 0;
				while (!isDischarged && trial < 3) {
					try {
						Thread.sleep(500);
						trial++;
						isDischarged = st.getConfidence() >= IConfidence.DISCHARGED_MAX;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				return isDischarged;
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
		if (node == null) {
			return new ProofNodeWrapper(node, currentID);
		}

		if (node.getChildNodes().length == 0) {
			ProofNodeWrapper wrapper = new ProofNodeWrapper(node, currentID);
			if (!(node.isClosed() || node.getConfidence() == IConfidence.DISCHARGED_MAX)) {
				nodes.add(wrapper);
			}
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
