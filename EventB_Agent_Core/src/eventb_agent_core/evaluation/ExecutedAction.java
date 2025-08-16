package eventb_agent_core.evaluation;

/**
 * This class records information of one attempt of an action. Call
 * {@code start()} and {@code finish()} to record information of the attempt.
 */
public class ExecutedAction {

	private ComponentType type;
	private String poName; // only set this if this is a proof action
	private int attemptID;
	private long time;
	private long tokens;
	private String error;

	public ExecutedAction(ComponentType type, int attemptID) {
		this.type = type;
		this.attemptID = attemptID;
	}

	public void start() {
		this.time = System.currentTimeMillis();
		this.tokens = 0;
		this.error = "";
	}

	public void finish() {
		long endTime = System.currentTimeMillis();
		time = endTime - time;
	}

	public void addTokens(long tokens) {
		this.tokens += tokens;
	}

	public int getAttemptID() {
		return attemptID;
	}

	public ComponentType getType() {
		return type;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getPoName() {
		return poName;
	}

	public void setPoName(String poName) {
		this.poName = poName;
	}

	@Override
	public String toString() {
		String delimiter = ",";
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(type);
		stringBuilder.append(delimiter);
		stringBuilder.append(poName);
		stringBuilder.append(delimiter);
		stringBuilder.append(attemptID);
		stringBuilder.append(delimiter);
		stringBuilder.append(time);
		stringBuilder.append(delimiter);
		stringBuilder.append(tokens);
		stringBuilder.append(delimiter);
		stringBuilder.append(error);
		stringBuilder.append(delimiter);

		return stringBuilder.toString();
	}

}
