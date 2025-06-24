package eventb_agent_core.llm.gemini;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

public class GeminiRequestSender extends LLMRequestSender {

	public GeminiRequestSender(LLMModels model) {
		super(model);
	}

	@Override
	protected String getAPIKey() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		return prefs.get(AgentPreferenceInitializer.PREF_GEMINI_KEY, "");
	}

	@Override
	protected String getAPIEndpoint() {
		return Constants.GEMINI_ENDPOINT;
	}

}
