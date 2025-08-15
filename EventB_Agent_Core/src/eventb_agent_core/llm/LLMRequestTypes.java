package eventb_agent_core.llm;

import java.nio.file.Path;
import java.nio.file.Paths;

import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;

public enum LLMRequestTypes {

	REFINE_STRATEGY, // Given requirements, produce a refine strategy
	SYNTHESIS, // Given system description, synthesize a model
	FIX_COMPILATION_ERRS, // Fix compilation errors in a model
	REFINE_MODEL, // Given previous system description, previous model, and new system
					// description, refine the model
	FIX_PROOF, // Retrieve proof tree to be used as part of the prompt
	FIX_PROOF_NO_STRATEGY; // Retrieve proof tree to be used as part of the prompt

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
		case FIX_COMPILATION_ERRS:
			return "fix_compilation_errors.txt";
		case REFINE_MODEL:
			return "refine_model.txt";
		case FIX_PROOF:
			return "fix_proof_with_strategy.txt";
		case FIX_PROOF_NO_STRATEGY:
			return "fix_proof_no_strategy.txt";
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
		case FIX_COMPILATION_ERRS:
			return new String[] { Constants.MODEL_PLACE_HOLDER, Constants.ERRORS_PLACE_HOLDER };
		case REFINE_MODEL:
			return new String[] { Constants.PREV_SYS_DESC_PLACE_HOLDER, Constants.MODEL_PLACE_HOLDER,
					Constants.SYS_DESC_PLACE_HOLDER };
		case FIX_PROOF:
		case FIX_PROOF_NO_STRATEGY:
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
		case FIX_COMPILATION_ERRS:
			return true;
		case REFINE_MODEL:
			return true;
		case FIX_PROOF:
			return false;
		case FIX_PROOF_NO_STRATEGY:
			return true;
		default:
			return false;
		}
	}

	public boolean areToolsEnabled() {
		switch (this) {
		case REFINE_STRATEGY:
			return false;
		case SYNTHESIS:
			return false;
		case FIX_COMPILATION_ERRS:
			return false;
		case REFINE_MODEL:
			return false;
		case FIX_PROOF:
			return true;
		case FIX_PROOF_NO_STRATEGY:
			return false;
		default:
			return false;
		}
	}

}
