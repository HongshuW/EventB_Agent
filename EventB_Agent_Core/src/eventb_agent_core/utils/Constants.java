package eventb_agent_core.utils;

public class Constants {

	public static final String PREF_NODE_ID = "eventb_agent";

	public static final String DEFAULT_MODEL = "GPT 4.1";

	/* OpenAI parameters */

	public static final String GPT_ENDPOINT = "https://api.openai.com/v1/responses";
	public static final String GPT_MODEL = "gpt-4.1";
	public static final String GPT_MINI_MODEL = "gpt-4.1-mini";
	public static final String GPT_5_MODEL = "gpt-5";
	public static final String GPT_5_MINI_MODEL = "gpt-5-mini";
	public static final String GPT_5_NANO_MODEL = "gpt-5-nano";
	public static final String GPT_O3_MODEL = "o3";
	public static final String GPT_O3_MINI_MODEL = "o3-mini";

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

	public static final String VERBOSITY = "low";
	public static final String REASONING = "medium";

	public static final String DEFAULT_PLACE_HOLDER = "{{default}}";
	public static final String REFINEMENT_ID_PLACE_HOLDER = "{{refinement_id}}";
	public static final String SYS_DESC_PLACE_HOLDER = "{{system_desc}}";
	public static final String SYS_REQ_PLACE_HOLDER = "{{system_req}}";
	public static final String MODEL_PLACE_HOLDER = "{{model}}";
	public static final String PO_NAME_PLACE_HOLDER = "{{po_name}}";
	public static final String PROOF_TREE_PLACE_HOLDER = "{{proof_tree}}";
	public static final String PROOF_TACTICS_PLACE_HOLDER = "{{proof_tactics}}";
	public static final String PREV_SYS_DESC_PLACE_HOLDER = "{{prev_system_desc}}";
	public static final String PREV_SYS_REQ_PLACE_HOLDER = "{{prev_system_req}}";
	public static final String GLUING_INVS_PLACE_HOLDER = "{{gluing_invs}}";
	public static final String ERRORS_PLACE_HOLDER = "{{errors}}";
	public static final String MODEL_CHECK_RESULT_PLACE_HOLDER = "{{model_checking_results}}";

	/* OpenAI response keys */

	public static final String FUNCTION_NAME = "name";
	public static final String FUNCTION_ARGS = "arguments";

}
