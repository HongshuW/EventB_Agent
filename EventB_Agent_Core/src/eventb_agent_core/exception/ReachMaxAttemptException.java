package eventb_agent_core.exception;

public class ReachMaxAttemptException extends Exception {

	public String componentName;

	public ReachMaxAttemptException(String componentName) {
		super(componentName + ": reached maximum number of allowed attempts.");
		this.componentName = componentName;
	}

}
