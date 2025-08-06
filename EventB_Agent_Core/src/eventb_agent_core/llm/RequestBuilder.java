package eventb_agent_core.llm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eventb_agent_core.utils.FileUtils;

/**
 * This class is responsible for generating LLM request.
 */
public abstract class RequestBuilder {

	protected LLMModels llmModel;

	public RequestBuilder(LLMModels llmModel) {
		this.llmModel = llmModel;
	}

	protected Map<String, Object> getSchema(LLMRequestTypes requestType) throws IOException {
		Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "schemas",
				getSchemaFileNameFromType(requestType));
		Map<String, Object> json = FileUtils.readOrderedJSON(path);

		return json;
	}

	protected List<Map<String, Object>> getFunctionSchemas(LLMRequestTypes requestType) throws IOException {
		List<Map<String, Object>> functionSchemas = new ArrayList<>();
		String[] files = getFunctionFileNamesFromType(requestType);
		for (String file : files) {
			Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "schemas",
					"functions", file);
			Map<String, Object> json = FileUtils.readOrderedJSON(path);
			functionSchemas.add(json);
		}

		return functionSchemas;
	}

	protected abstract String getSchemaFileNameFromType(LLMRequestTypes requestType);

	protected abstract String[] getFunctionFileNamesFromType(LLMRequestTypes requestType);

	public abstract String getRequestWithSchema(String prompt, LLMRequestTypes requestType) throws IOException;

	public abstract String getRequestWithTools(String prompt, LLMRequestTypes requestType) throws IOException;

	public abstract String getRequestPlain(String prompt) throws IOException;

	public abstract HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException;

}
