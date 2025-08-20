package eventb_agent_core.exception;

public class InvalidCharacterException extends Exception {

	public InvalidCharacterException(String character) {
		super("LLM response contains invalid character:" + character);
	}

}
