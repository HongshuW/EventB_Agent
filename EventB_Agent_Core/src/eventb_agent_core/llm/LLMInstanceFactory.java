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
		case GPT4_1_MINI:
			return new GPTRequestBuilder();
		case CLAUDE3_OPUS:
			return new ClaudeRequestBuilder();
		case GEMINI2_5_FLASH:
			return new GeminiRequestBuilder();
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}
	
	public static LLMResponseParser getResponseParser(LLMModels model) {
		switch (model) {
		case GPT4_1_MINI:
			return new GPTResponseParser();
		case CLAUDE3_OPUS:
			return new ClaudeResponseParser();
		case GEMINI2_5_FLASH:
			return new GeminiResponseParser();
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}

}
