package eventb_agent_core.utils.proof;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IEventBRoot;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.internal.core.pm.ProofManager;

public class ProofAttemptWrapper {

	private IProofAttempt proofAttempt;
	private IProofTreeNode node;
	private String poName;
	private IEventBRoot eventbRoot;

	public ProofAttemptWrapper(IProofAttempt proofAttempt, IProofTreeNode node, String poName, IEventBRoot eventbRoot) {
		this.proofAttempt = proofAttempt;
		this.node = node;
		this.poName = poName;
		this.eventbRoot = eventbRoot;
	}

	public void update() {
		if (this.proofAttempt.isDisposed()) {
			IProofAttempt newProofAttempt = getProofAttemptByPOName(poName, eventbRoot);
			IProofTreeNode root = newProofAttempt.getProofTree().getRoot();
			IProofTreeNode newNode = getNode(root, getIndexPath(node));

			this.proofAttempt = newProofAttempt;
			this.node = newNode;
		}
	}
	
	public IProofAttempt getProofAttempt() {
		return this.proofAttempt;
	}
	
	public IProofTreeNode getNode() {
		return this.node;
	}

	private List<Integer> getIndexPath(IProofTreeNode node) {
		List<Integer> path = new ArrayList<>();
		while (node.getParent() != null) {
			IProofTreeNode parent = node.getParent();
			IProofTreeNode[] siblings = parent.getChildNodes();
			for (int i = 0; i < siblings.length; i++) {
				if (siblings[i] == node) {
					path.add(0, i);
					break;
				}
			}
			node = parent;
		}
		return path;
	}

	private IProofTreeNode getNode(IProofTreeNode root, List<Integer> path) {
		IProofTreeNode cur = root;
		for (int idx : path) {
			cur = cur.getChildNodes()[idx];
		}
		return cur;
	}

	private IProofAttempt getProofAttemptByPOName(String poName, IEventBRoot root) {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(root);
		for (IProofAttempt proofAttempt : proofComponent.getProofAttempts()) {
			if (proofAttempt.isDisposed()) {
				continue;
			}
			if (poName.equals(proofAttempt.getName())) {
				return proofAttempt;
			}
		}
		return null;
	}

}
