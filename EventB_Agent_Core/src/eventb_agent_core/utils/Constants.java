package eventb_agent_core.utils;

public class Constants {

	public static final String PREF_NODE_ID = "eventb_agent";

	/* OpenAI parameters */

	public static final String GPT_ENDPOINT = "https://api.openai.com/v1/responses";
	public static final String GPT_MODEL = "gpt-4.1-mini";

	/* Claude parameters */

	public static final String CLAUDE_ENDPOINT = "https://api.anthropic.com/v1/messages";
	public static final String CLAUDE_MODEL = "claude-opus-4-0";
	public static final String ANTHROPIC_VERSION = "2023-06-01";

	/* Gemini parameters */

	public static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";
	public static final String GEMINI_MODEL = "gemini-2.5-flash";

	/* General parameters for LLMs */

	public static final double TEMPERATURE = 0;
	public static final double TOP_P = 1;
	public static final int TOKEN_LIMIT = 2048;

	public static final String SYS_DESC_PLACE_HOLDER = "{{system_desc}}";

}
