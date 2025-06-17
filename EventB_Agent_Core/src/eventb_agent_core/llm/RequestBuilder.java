package eventb_agent_core.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;

/**
 * This class is responsible for generating request.
 */
public class RequestBuilder {

	public RequestBuilder() {
	}

	public String getRequest(String prompt) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", Constants.GPT_MODEL);

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		content.put("type", "input_text");
		content.put("text", prompt);
		requestMessage.put("content", Arrays.asList(content));
		request.put("input", Arrays.asList(requestMessage));

		Map<String, Object> jsonSchema = getSchema();
		LinkedHashMap<String, Object> textFormat = new LinkedHashMap<>();
		textFormat.put("format", jsonSchema);
		request.put("text", textFormat);

		request.put("temperature", Constants.TEMPERATURE);
		request.put("top_p", Constants.TOP_P);
		request.put("max_output_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	public Map<String, Object> getSchema() throws IOException {
		Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "schemas",
				"eventb_schema.json");
		Map<String, Object> json = FileUtils.readOrderedJSON(path);

		return json;
	}

}
