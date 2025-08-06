package eventb_agent_core.utils;

public class Constants {

	public static final String PREF_NODE_ID = "eventb_agent";

	public static final String DEFAULT_MODEL = "GPT 4.1";

	/* OpenAI parameters */

	public static final String GPT_ENDPOINT = "https://api.openai.com/v1/responses";
	public static final String GPT_MODEL = "gpt-4.1";
	public static final String GPT_MINI_MODEL = "gpt-4.1-mini";

	/* Claude parameters */

	public static final String CLAUDE_ENDPOINT = "https://api.anthropic.com/v1/messages";
	public static final String CLAUDE_MODEL = "claude-3-opus-latest";
	public static final String ANTHROPIC_VERSION = "2023-06-01";

	/* Gemini parameters */

	public static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";
	public static final String GEMINI_MODEL = "gemini-2.5-flash";

	/* General parameters for LLMs */

	public static final double TEMPERATURE = 0;
	public static final double TOP_P = 1;
	public static final int TOKEN_LIMIT = 4096;

	public static final String DEFAULT_PLACE_HOLDER = "{{default}}";
	public static final String SYS_DESC_PLACE_HOLDER = "{{system_desc}}";
	public static final String MODEL_PLACE_HOLDER = "{{model}}";
	public static final String PROOF_TREE_PLACE_HOLDER = "{{proof_tree}}";
	public static final String PREV_SYS_DESC_PLACE_HOLDER = "{{prev_system_desc}}";
	public static final String ERRORS_PLACE_HOLDER = "{{errors}}";
	
	/* OpenAI response keys */
	
	public static final String FUNCTION_NAME = "name";
	public static final String FUNCTION_ARGS = "arguments";

}
