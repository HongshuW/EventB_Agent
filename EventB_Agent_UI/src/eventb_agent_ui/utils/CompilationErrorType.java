package eventb_agent_ui.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompilationErrorType {

	UNEXPECTED_SUBFORMULA("Unexpected sub-formula"),
	
	TYPE_MISSING("does not have a type"), TYPE_MISMATCH(""),

	IDENT_UNDECLARED(""), VAR_UNDECLARED(""), CONST_UNDECLARED(""), SET_UNDECLARED("");

	private String errorMessage;

	CompilationErrorType(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	private static final Map<String, CompilationErrorType> BY_STR_VAL = Arrays.stream(values())
			.collect(Collectors.toMap(CompilationErrorType::toString, Function.identity()));

	public static CompilationErrorType getCompilationErrorType(String errorMessage) {
		CompilationErrorType errorType = null;
		for (String key : BY_STR_VAL.keySet()) {
			if (errorMessage.contains(key)) {
				errorType = BY_STR_VAL.get(key);
			}
		}
		if (errorType == null) {
			throw new IllegalArgumentException("Unknown error type: " + errorMessage);
		}
		return errorType;
	}

	@Override
	public String toString() {
		return errorMessage;
	}

}
