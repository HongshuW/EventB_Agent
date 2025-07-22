package eventb_agent_core.llm;

import java.nio.file.Path;
import java.nio.file.Paths;

import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;

public enum LLMRequestTypes {

	REFINE_STRATEGY, // Given requirements, produce a refine strategy
	SYNTHESIS, // Given system description, synthesize a model
	REFINE_MODEL, // Given previous system description, previous model, and new system
					// description, refine the model
	FIX_PROOF; // Retrieve proof tree to be used as part of the prompt

	public String getPrompt() {
		Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "prompts",
				getPromptFileNameFromType());
		return FileUtils.readText(path);
	}

	private String getPromptFileNameFromType() {
		switch (this) {
		case REFINE_STRATEGY:
			return "refine_strategy.txt";
		case SYNTHESIS:
			return "synthesize_machine.txt";
		case REFINE_MODEL:
			return "refine_model.txt";
		case FIX_PROOF:
			return "add_hypothesis.txt";
		default:
			return "synthesize_machine.txt";
		}
	}

	public String[] getPlaceHolders() {
		switch (this) {
		case REFINE_STRATEGY:
			return new String[] { Constants.SYS_DESC_PLACE_HOLDER };
		case SYNTHESIS:
			return new String[] { Constants.SYS_DESC_PLACE_HOLDER };
		case REFINE_MODEL:
			return new String[] { Constants.PREV_SYS_DESC_PLACE_HOLDER, Constants.MODEL_PLACE_HOLDER,
					Constants.SYS_DESC_PLACE_HOLDER };
		case FIX_PROOF:
			return new String[] { Constants.MODEL_PLACE_HOLDER, Constants.PROOF_TREE_PLACE_HOLDER };
		default:
			return new String[] { Constants.DEFAULT_PLACE_HOLDER };
		}
	}

	public boolean isStructuredRequest() {
		switch (this) {
		case REFINE_STRATEGY:
			return true;
		case SYNTHESIS:
			return true;
		case REFINE_MODEL:
			return true;
		case FIX_PROOF:
			return true;
		default:
			return false;
		}
	}

}
