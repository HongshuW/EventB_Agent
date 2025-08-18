package eventb_agent_core.exception;

public class ReachMaxAttemptException extends Exception {

	public String componentName;
	public String poName;

	public ReachMaxAttemptException(String componentName) {
		super(componentName + ": reached maximum number of allowed attempts.");
		this.componentName = componentName;
	}

	public ReachMaxAttemptException(String componentName, String poName) {
		this(componentName);
		this.poName = poName;
	}

}
