package eventb_agent_core.llm;

import eventb_agent_core.llm.claude.ClaudeRequestBuilder;
import eventb_agent_core.llm.claude.ClaudeRequestSender;
import eventb_agent_core.llm.gemini.GeminiRequestBuilder;
import eventb_agent_core.llm.gemini.GeminiRequestSender;
import eventb_agent_core.llm.gpt.GPTRequestBuilder;
import eventb_agent_core.llm.gpt.GPTRequestSender;

public class LLMInstanceFactory {

	public static LLMRequestSender getRequestSender(LLMModels model) {
		switch (model) {
		case GPT4_1_MINI:
			return new GPTRequestSender(model);
		case CLAUDE:
			return new ClaudeRequestSender(model);
		case GEMINI:
			return new GeminiRequestSender(model);
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}
	
	public static RequestBuilder getRequestBuilder(LLMModels model) {
		switch (model) {
		case GPT4_1_MINI:
			return new GPTRequestBuilder();
		case CLAUDE:
			return new ClaudeRequestBuilder();
		case GEMINI:
			return new GeminiRequestBuilder();
		default:
			throw new IllegalArgumentException("Unhandled model: " + model);
		}
	}

}
