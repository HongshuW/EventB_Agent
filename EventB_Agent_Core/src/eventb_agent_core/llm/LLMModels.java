package eventb_agent_core.llm;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import eventb_agent_core.utils.Constants;

public enum LLMModels {
	GPT5("GPT 5"), GPT5_MINI("GPT 5 mini"), GPT5_NANO("GPT 5 nano"), GPT4_1("GPT 4.1"), GPT4_1_MINI("GPT 4.1 mini"),
	CLAUDE3_OPUS("Claude 3 opus"), GEMINI2_5_FLASH("Gemini 2.5 flash");

	private final String stringValue;

	LLMModels(String stringValue) {
		this.stringValue = stringValue;
	}

	private static final Map<String, LLMModels> BY_STR_VAL = Arrays.stream(values())
			.collect(Collectors.toMap(LLMModels::toString, Function.identity()));

	public static LLMModels getLLMModel(String stringValue) {
		LLMModels model = BY_STR_VAL.get(stringValue);
		if (model == null) {
			throw new IllegalArgumentException("Unknown LLM model type: " + stringValue);
		}
		return model;
	}

	public String getModelTypeAPI() {
		switch (this) {
		case GPT5:
			return Constants.GPT_5_MODEL;
		case GPT5_MINI:
			return Constants.GPT_5_MINI_MODEL;
		case GPT5_NANO:
			return Constants.GPT_5_NANO_MODEL;
		case GPT4_1:
			return Constants.GPT_MODEL;
		case GPT4_1_MINI:
			return Constants.GPT_MINI_MODEL;
		case CLAUDE3_OPUS:
			return Constants.CLAUDE_MODEL;
		case GEMINI2_5_FLASH:
			return Constants.GEMINI_MODEL;
		default:
			return "";
		}
	}

	@Override
	public String toString() {
		return stringValue;
	}

}
