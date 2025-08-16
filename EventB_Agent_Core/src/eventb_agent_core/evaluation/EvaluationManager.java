package eventb_agent_core.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class records the overall time taken and the executed actions during
 * experiment.
 */
public class EvaluationManager {

	private long time; // time in milliseconds
	private List<ExecutedAction> actions;
	private int lastPOAction;

	private static EvaluationManager instance;

	private EvaluationManager() {
		reset();
	}

	private void reset() {
		this.time = 0;
		this.actions = new ArrayList<>();
		this.lastPOAction = -1;
	}

	public static EvaluationManager getDefaultInstance() {
		if (instance == null) {
			instance = new EvaluationManager();
		}
		return instance;
	}

	public static EvaluationManager resetDefaultInstance() {
		instance = new EvaluationManager();
		return instance;
	}

	/* timer */

	private static long getTime() {
		EvaluationManager manager = getDefaultInstance();
		return manager.time;
	}

	public static void startTimer() {
		EvaluationManager manager = getDefaultInstance();
		manager.time = System.currentTimeMillis();
	}

	public static void endTimer() {
		EvaluationManager manager = getDefaultInstance();
		long endTime = System.currentTimeMillis();
		manager.time = endTime - manager.time;
	}

	public static double getTimeInSeconds() {
		EvaluationManager manager = getDefaultInstance();
		return manager.time / 1000.0;
	}

	/* token counter */

	public static void addTokensToLatestAction(long tokens) {
		if (getLatestAction() == null) {
			return;
		}
		getLatestAction().addTokens(tokens);
	}

	/* attempt counter */

	public static int getAttemptsFromLatestAction() {
		if (getLatestAction() == null) {
			return -1;
		}
		return getLatestAction().getAttemptID();
	}
	
	public static int getAttemptsFromLatestProofAction() {
		if (getLatestProofAction() == null) {
			return -1;
		}
		return getLatestProofAction().getAttemptID();
	}

	/* error */

	public static void setErrorToLatestAction(String error) {
		if (getLatestAction() == null) {
			return;
		}
		getLatestAction().setError(error);
	}

	/* component type */

	public static ComponentType getComponentTypeFromLatestAction() {
		if (getLatestAction() == null) {
			return null;
		}
		return getLatestAction().getType();
	}

	/* proof */

	public static int getLastPOActionIndex() {
		EvaluationManager manager = getDefaultInstance();
		return manager.lastPOAction;
	}

	public static void setLastPOActionIndex() {
		EvaluationManager manager = getDefaultInstance();
		manager.lastPOAction = getActions().size() - 1;
	}

	public static String getPONameFromLatestProofAction() {
		if (getLatestProofAction() == null) {
			return null;
		}
		return getLatestProofAction().getPoName();
	}

	public static void setPONameToLatestAction(String poName) {
		if (getLatestAction() == null) {
			return;
		}
		getLatestAction().setPoName(poName);
	}

	/* actions */

	public static void addAndStartNewAction(ComponentType type, int attempt) {
		ExecutedAction action = new ExecutedAction(type, attempt);
		getActions().add(action);
		action.start();
	}

	public static void endLatestAction() {
		if (getLatestAction() == null) {
			return;
		}
		getLatestAction().finish();
	}

	private static List<ExecutedAction> getActions() {
		EvaluationManager manager = getDefaultInstance();
		return manager.actions;
	}

	private static ExecutedAction getLatestAction() {
		List<ExecutedAction> actions = getActions();
		if (actions.isEmpty()) {
			return null;
		}
		return actions.get(actions.size() - 1);
	}

	/**
	 * This function assumes that the list of actions is non-empty and contains at
	 * least one proof action.
	 * 
	 * @return
	 */
	private static ExecutedAction getLatestProofAction() {
		List<ExecutedAction> actions = getActions();
		return actions.get(getLastPOActionIndex());
	}

	/* write output */

	public static void write(String path, String projectName) {
		StringBuilder contents = new StringBuilder();
		EvaluationManager manager = getDefaultInstance();
		List<ExecutedAction> actions = manager.actions;
		for (ExecutedAction action : actions) {
			contents.append(action);
			contents.append("\n");
		}
		contents.append(getTime());
		contents.append("\n");

		try (FileWriter writer = new FileWriter(path + File.separator + projectName + ".txt")) {
			writer.write(contents.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
