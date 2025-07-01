package eventb_agent_core.llm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * This class is responsible for generating LLM request.
 */
public abstract class RequestBuilder {

	protected abstract Map<String, Object> getSchema() throws IOException;

	public abstract String getRequestWithSchema(String prompt) throws IOException;
	
	public abstract String getRequestPlain(String prompt) throws IOException;

	public abstract HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException;

}
