package eventb_agent_core.llm;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LLMModels {
	GPT4_1_MINI("GPT 4.1 mini"),
	CLAUDE("Claude"),
	GEMINI("Gemini");

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

	@Override
	public String toString() {
		return stringValue;
	}

}
