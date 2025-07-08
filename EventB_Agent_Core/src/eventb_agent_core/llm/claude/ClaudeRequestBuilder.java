package eventb_agent_core.llm.claude;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;

public class ClaudeRequestBuilder extends RequestBuilder {

	public ClaudeRequestBuilder(LLMModels llmModel) {
		super(llmModel);
	}

	@Override
	protected String getSchemaFileNameFromType(SchemaType schemaType) {
		switch (schemaType) {
		case EventB:
			return "claude_eventb_schema.json";
		case Proof:
			return "claude_proof_schema.json";
		default:
			return "claude_eventb_schema.json";
		}
	}

	@Override
	public String getRequestWithSchema(String prompt, SchemaType schemaType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> jsonSchema = getSchema(schemaType);
		String schemaString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
		prompt += "\nFollow the grammar in your response:\n" + schemaString;

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", llmModel.getModelTypeAPI());

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		requestMessage.put("content", prompt);
		request.put("messages", Arrays.asList(requestMessage));

		request.put("max_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public String getRequestPlain(String prompt) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException {
		URL url = new URL(apiEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("x-api-key", apiKey);
		conn.setRequestProperty("anthropic-version", Constants.ANTHROPIC_VERSION);
		conn.setRequestProperty("content-type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

}
