package eventb_agent_core.llm;

import eventb_agent_core.llm.claude.ClaudeRequestBuilder;
import eventb_agent_core.llm.claude.ClaudeRequestSender;
import eventb_agent_core.llm.claude.ClaudeResponseParser;
import eventb_agent_core.llm.gemini.GeminiRequestBuilder;
import eventb_agent_core.llm.gemini.GeminiRequestSender;
import eventb_agent_core.llm.gemini.GeminiResponseParser;
import eventb_agent_core.llm.gpt.GPTRequestBuilder;
import eventb_agent_core.llm.gpt.GPTRequestSender;
import eventb_agent_core.llm.gpt.GPTResponseParser;

public class LLMInstanceFactory {

	public static LLMRequestSender getRequestSender(LLMModels model) {
		switch (model) {
		case GPT5_4_MINI:
		case GPT5:
		case GPT5_MINI:
		case GPT5_NANO:
		case GPT_O3:
		case GPT_O3_MINI:
		case GPT4_1:
		case GPT4_1_MINI:
			return new GPTRequestSender(model);
		case CLAUDE3_OPUS:
			return new ClaudeRequestSender(model);
		case GEMINI2_5_FLASH:
			return new GeminiRequestSender(model);
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}

	public static RequestBuilder getRequestBuilder(LLMModels model) {
		switch (model) {
		case GPT5_4_MINI:
		case GPT5:
		case GPT5_MINI:
		case GPT5_NANO:
		case GPT_O3:
		case GPT_O3_MINI:
		case GPT4_1:
		case GPT4_1_MINI:
			return new GPTRequestBuilder(model);
		case CLAUDE3_OPUS:
			return new ClaudeRequestBuilder(model);
		case GEMINI2_5_FLASH:
			return new GeminiRequestBuilder(model);
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}

	public static LLMResponseParser getResponseParser(LLMModels model) {
		switch (model) {
		case GPT5_4_MINI:
		case GPT5:
		case GPT5_MINI:
		case GPT5_NANO:
		case GPT_O3:
		case GPT_O3_MINI:
		case GPT4_1:
		case GPT4_1_MINI:
			return new GPTResponseParser(model);
		case CLAUDE3_OPUS:
			return new ClaudeResponseParser(model);
		case GEMINI2_5_FLASH:
			return new GeminiResponseParser(model);
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}

}
