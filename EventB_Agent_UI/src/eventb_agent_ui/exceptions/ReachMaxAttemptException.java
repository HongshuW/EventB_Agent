package eventb_agent_ui.exceptions;

public class ReachMaxAttemptException extends Exception {

	public ReachMaxAttemptException(String componentName) {
		super(componentName + ": reached maximum number of allowed attempts.");
	}
	
}
