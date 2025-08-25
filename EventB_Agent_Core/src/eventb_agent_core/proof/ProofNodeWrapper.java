package eventb_agent_core.proof;

import org.eventb.core.seqprover.IProofTreeNode;

public class ProofNodeWrapper {

	public IProofTreeNode node;
	public int id;

	public ProofNodeWrapper(IProofTreeNode node, int id) {
		this.node = node;
		this.id = id;
	}

}
