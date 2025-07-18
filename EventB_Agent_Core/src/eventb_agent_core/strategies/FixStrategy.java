package eventb_agent_core.strategies;

import java.nio.file.Path;
import java.nio.file.Paths;

import eventb_agent_core.utils.FileUtils;

/**
 * This class defines all the strategies for fixing an existing Event-B model.
 */
public enum FixStrategy {
	ADD_AXIOM("Add axiom in context"), // Add hypothesis function
	STRENGTHEN_INV("Strengthen an invariant"), //
	ADD_HYP_GUARD("Add hypothesis in guard"); //

	private final String stringValue;

	FixStrategy(String stringValue) {
		this.stringValue = stringValue;
	}

	@Override
	public String toString() {
		return stringValue;
	}

	public Path getPromptPath() {
		String fileName = "";

		switch (this) {
		case ADD_AXIOM:
			fileName = "add_hypothesis";
			break;
		// TODO: add other cases
		default:
			fileName = "add_hypothesis";
			break;
		}

		return Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "prompts",
				fileName + ".txt");
	}

}
