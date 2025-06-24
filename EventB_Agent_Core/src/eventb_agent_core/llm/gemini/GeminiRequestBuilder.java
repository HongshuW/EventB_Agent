package eventb_agent_core.llm.gemini;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;

public class GeminiRequestBuilder extends RequestBuilder {

	@Override
	protected Map<String, Object> getSchema() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequest(String prompt) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("text", prompt);
		content.put("parts", Arrays.asList(requestMessage));

		request.put("contents", Arrays.asList(content));

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException {
		URL url = new URL(apiEndpoint + Constants.GEMINI_MODEL + ":generateContent?key=" + apiKey);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

}
