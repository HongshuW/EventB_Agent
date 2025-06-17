package eventb_agent_ui.utils;

public class ErrorTypeUtils {

	/* error strings */

	private static final String TYPE_NOT_SPECIFIED = "does not have a type";

	public static CompilationErrorType getErrorType(String errorMessage) {
		if (errorMessage.contains(TYPE_NOT_SPECIFIED)) {
			return CompilationErrorType.TYPE_MISSING;
		}
		return null;
	}

}
