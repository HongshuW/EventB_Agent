package eventb_agent_core.llm;

import eventb_agent_core.llm.RequestBuilder.SchemaType;
import eventb_agent_core.utils.Constants;

public enum LLMRequestTypes {

	SYNTHESIS, // Given system description, synthesize a model
	RETRIEVE_MODEL, // Retrieve a model to be used as part of the prompt
	FIX_PROOF; // Retrieve proof tree to be used as part of the prompt

	public String getPlaceHolder() {
		switch (this) {
		case SYNTHESIS:
			return Constants.SYS_DESC_PLACE_HOLDER;
		case RETRIEVE_MODEL:
			return Constants.MODEL_PLACE_HOLDER;
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
		case RETRIEVE_MODEL:
		case FIX_PROOF:
			return true;
		default:
			return false;
		}
	}

	public SchemaType getSchemaType() {
		switch (this) {
		case SYNTHESIS:
			return SchemaType.EventB;
		case RETRIEVE_MODEL:
		case FIX_PROOF:
			return SchemaType.Proof;
		default:
			return null;
		}
	}

}
