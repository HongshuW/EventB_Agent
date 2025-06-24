package eventb_agent_core.llm.gemini;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;

public class GeminiRequestBuilder extends RequestBuilder {

	@Override
	protected Map<String, Object> getSchema() throws IOException {
		Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "schemas",
				"gemini_schema.json");
		Map<String, Object> json = FileUtils.readOrderedJSON(path);

		return json;
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
		
		Map<String, Object> jsonSchema = getSchema();
		LinkedHashMap<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", jsonSchema);
		request.put("generationConfig", generationConfig);

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
