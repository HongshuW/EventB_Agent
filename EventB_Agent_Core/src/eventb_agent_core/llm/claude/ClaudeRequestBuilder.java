package eventb_agent_core.llm.claude;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;

public class ClaudeRequestBuilder extends RequestBuilder {

	@Override
	protected Map<String, Object> getSchema() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequest(String prompt) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", Constants.CLAUDE_MODEL);

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		requestMessage.put("content", prompt);
		request.put("messages", Arrays.asList(requestMessage));

		request.put("max_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException {
		URL url = new URL(apiEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("x-api-key", apiKey);
		conn.setRequestProperty("anthropic-version", Constants.ANTHROPIC_VERSION);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

}
