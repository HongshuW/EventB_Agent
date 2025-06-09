package eventb_agent_core.utils;

public class ParserUtils {
	public static String lex(String originalString) {
		return originalString.replace(":=", "≔").replace(">=", "≥");
	}
}
