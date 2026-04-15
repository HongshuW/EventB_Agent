package eventb_agent_core.llm.gpt;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

/**
 * This class is responsible for sending requests to LLM and receiving the
 * responses.
 */
public class GPTRequestSender extends LLMRequestSender {

	public GPTRequestSender(LLMModels model) {
		super(model);
	}

	@Override
	protected String getAPIKey() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		return prefs.get(AgentPreferenceInitializer.PREF_GPT_KEY, "");
	}

	@Override
	protected String getAPIEndpoint() {
		return Constants.GPT_ENDPOINT;
	}

	@Override
	protected String getFileUploadAPIEndpoint() {
		return Constants.GPT_FILE_UPLOAD_ENDPOINT;
	}
}
