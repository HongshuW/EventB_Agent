package eventb_agent_core.llm;

import eventb_agent_core.utils.Constants;

public enum LLMRequestTypes {
	SYNTHESIS, FIX_PROOF;

	public String getPlaceHolder() {
		switch (this) {
		case SYNTHESIS:
			return Constants.SYS_DESC_PLACE_HOLDER;
		case FIX_PROOF:
			return Constants.PROOF_TREE_PLACE_HOLDER;
		default:
			return Constants.DEFAULT_PLACE_HOLDER;
		}
	}

	public boolean isStructuredRequest() {
		switch (this) {
		case SYNTHESIS:
			return true;
		case FIX_PROOF:
			return false;
		default:
			return false;
		}
	}

}
