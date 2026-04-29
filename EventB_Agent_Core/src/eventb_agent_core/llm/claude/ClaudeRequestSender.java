package eventb_agent_core.llm.claude;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

public class ClaudeRequestSender extends LLMRequestSender {

	public ClaudeRequestSender(LLMModels model) {
		super(model);
	}

	@Override
	protected String getAPIKey() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		return prefs.get(AgentPreferenceInitializer.PREF_CLAUDE_KEY, "");
	}

	@Override
	protected String getAPIEndpoint() {
		return Constants.CLAUDE_ENDPOINT;
	}

	/**
	 * TODO: implement this later.
	 */
	@Override
	protected String getFileUploadAPIEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}

}
