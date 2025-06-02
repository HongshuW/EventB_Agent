package eventb_agent_ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * This class controls the plug-in life cycle.
 */
public class EventBAgentUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String AGENT_PLUGIN_ID = "EventB_Agent_UI"; //$NON-NLS-1$

	// The shared instance
	private static EventBAgentUIPlugin plugin;

	/**
	 * The constructor
	 */
	public EventBAgentUIPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EventBAgentUIPlugin getDefault() {
		return plugin;
	}

}
