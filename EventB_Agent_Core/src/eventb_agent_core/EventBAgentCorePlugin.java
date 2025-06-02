package eventb_agent_core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * This class controls the plug-in life cycle.
 */
public class EventBAgentCorePlugin extends Plugin {

	public static final String PLUGIN_ID = "EventB_Agent_Core";

	// The shared instance
	private static EventBAgentCorePlugin plugin;

	/**
	 * The constructor
	 */
	public EventBAgentCorePlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (isDebugging())
			configureDebugOptions();
	}

	private void configureDebugOptions() {
		// do nothing for the moment
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
	public static EventBAgentCorePlugin getDefault() {
		return plugin;
	}
}
