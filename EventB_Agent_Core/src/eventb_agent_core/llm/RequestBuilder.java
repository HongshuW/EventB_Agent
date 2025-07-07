package eventb_agent_core.llm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import eventb_agent_core.utils.FileUtils;

/**
 * This class is responsible for generating LLM request.
 */
public abstract class RequestBuilder {

	public enum SchemaType {
		EventB, Proof
	};

	protected Map<String, Object> getSchema(SchemaType schemaType) throws IOException {
		Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "schemas",
				getSchemaFileNameFromType(schemaType));
		Map<String, Object> json = FileUtils.readOrderedJSON(path);

		return json;
	}

	protected abstract String getSchemaFileNameFromType(SchemaType schemaType);

	public abstract String getRequestWithSchema(String prompt, SchemaType schemaType) throws IOException;

	public abstract String getRequestPlain(String prompt) throws IOException;

	public abstract HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException;

}
