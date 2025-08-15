package eventb_agent_core.utils.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ParserUtils {

	private static char INT = '\u2124';
	private static char NAT = '\u2115';
	private static char POW = '\u2119';
	private static char EMPTY_SET = '\u2205';

	private static char TRUE = '\u22A4';
	private static char FALSE = '\u22A5';
	private static char OR = '\u2228';
	private static char NOT = '\u00AC';
	private static char AND = '\u2227';

	private static char UNION = '\u22C3';
	private static char INTER = '\u22C2';
	private static char SMALL_UNION = '\u222A';
	private static char SMALL_INTER = '\u2229';
	private static char RANGE = '\u2025';
	private static char SET_MINUS = '\u2216';
	private static char TIMES = '\u00D7';
	private static char MINUS = '\u2212';
	private static char DOT = '\u00B7';
	private static char K_PRODUCT = '\u2297';
	private static char RIGHT_TRI = '\u25B7';
	private static char LEFT_TRI = '\u2265';
	private static char DOMAIN_ANTIRESTRICTION = '\u2254';
	private static char RANGE_ANTIRESTRICTION = '\u2A65';
	private static char VERTICAL_LINE = '\u2223';
	private static char CIRC = '\u2218';

	private static char RIGHT_ARROW = '\u2192';
	private static char ARROW1 = '\u21A0'; // -->>
	private static char ARROW2 = '\u2914'; // >+>
	private static char ARROW3 = '\u2900'; // +->>
	private static char ARROW4 = '\u2916'; // >->>
	private static char ARROW5 = '\u2260'; // <->
	private static char ARROW6 = '\u21F8'; // +->
	private static char ARROW7 = '\u21A3'; // >->
	private static char ARROW8 = '\u21A6'; // |->

	private static char IN = '\u2208';
	private static char SUBSET_EQ = '\u2286';
	private static char NOT_IN = '\u2209';
	private static char NOT_PROPER_SUBSET = '\u2284';
	private static char NOT_SUBSET = '\u2288';
	private static char PROPER_SUBSET = '\u2282';
	private static char NOT_EQ = '\u2260';
	private static char LEQ = '\u2264';
	private static char GEQ = '\u2265';
	private static char ASSIGN = '\u2254';

	private static char FOR_ALL = '\u2200';
	private static char IFF = '\u21D4';
	private static char EXISTS = '\u2203';
	private static char LAMBDA = '\u03BB';
	private static char IMPLIES = '\u21D2';

	private static Map<Character, String> reverseLexMap;

	private static void initMap() {
		reverseLexMap = new HashMap<>();
		reverseLexMap.put(INT, "INT");
		reverseLexMap.put(NAT, "NAT");
		reverseLexMap.put(POW, "POW");
		reverseLexMap.put(EMPTY_SET, "{}");
		reverseLexMap.put(TRUE, "true");
		reverseLexMap.put(FALSE, "false");
		reverseLexMap.put(OR, "\\lor");
		reverseLexMap.put(NOT, "\\neg");
		reverseLexMap.put(AND, "\\land");
		reverseLexMap.put(UNION, "UNION");
		reverseLexMap.put(INTER, "INTER");
		reverseLexMap.put(SMALL_UNION, "\\/");
		reverseLexMap.put(SMALL_INTER, "/\\");
		reverseLexMap.put(RANGE, "..");
		reverseLexMap.put(SET_MINUS, "\\");
		reverseLexMap.put(TIMES, "\\times");
		reverseLexMap.put(MINUS, "-");
		reverseLexMap.put(DOT, ".");
		reverseLexMap.put(K_PRODUCT, "><");
		reverseLexMap.put(RIGHT_TRI, "|>");
		reverseLexMap.put(LEFT_TRI, "<|");
		reverseLexMap.put(DOMAIN_ANTIRESTRICTION, "<<|");
		reverseLexMap.put(RANGE_ANTIRESTRICTION, "|>>");
		reverseLexMap.put(VERTICAL_LINE, "|");
		reverseLexMap.put(CIRC, "circ");
		reverseLexMap.put(RIGHT_ARROW, "-->");
		reverseLexMap.put(ARROW1, "-->>");
		reverseLexMap.put(ARROW2, ">+>");
		reverseLexMap.put(ARROW3, "+->>");
		reverseLexMap.put(ARROW4, ">->>");
		reverseLexMap.put(ARROW5, "<->");
		reverseLexMap.put(ARROW6, "+->");
		reverseLexMap.put(ARROW7, ">->");
		reverseLexMap.put(ARROW8, "|->");
		reverseLexMap.put(IN, "\\in");
		reverseLexMap.put(SUBSET_EQ, "\\subseteq");
		reverseLexMap.put(NOT_IN, "\\notin");
		reverseLexMap.put(NOT_PROPER_SUBSET, "/<<:");
		reverseLexMap.put(NOT_SUBSET, "/<:");
		reverseLexMap.put(PROPER_SUBSET, "<<:");
		reverseLexMap.put(NOT_EQ, "\\neq");
		reverseLexMap.put(LEQ, "<=");
		reverseLexMap.put(GEQ, ">=");
		reverseLexMap.put(ASSIGN, ":=");
		reverseLexMap.put(FOR_ALL, "\\forall");
		reverseLexMap.put(IFF, "<=>");
		reverseLexMap.put(EXISTS, "\\exists");
		reverseLexMap.put(LAMBDA, "%");
		reverseLexMap.put(IMPLIES, "=>");
	}

	public static String addEscape(String originalString) {
		return originalString.replace("\\", "\\\\");
	}

	public static String lex(String originalString) {

		System.out.println("Before processing: " + originalString);

		Map<String, String> prioritizedMap = new HashMap<>();
		prioritizedMap.put("||", "∥");

		Map<String, String> stringMap = new HashMap<>();
		stringMap.put("Z", String.valueOf(INT));
		stringMap.put("INT", String.valueOf(INT));
		stringMap.put("INTEGER", String.valueOf(INT));
		stringMap.put("N", String.valueOf(NAT));
		stringMap.put("NAT", String.valueOf(NAT));
		stringMap.put("NATURAL", String.valueOf(NAT));
		stringMap.put("N1", String.valueOf(NAT) + "1");
		stringMap.put("NAT1", String.valueOf(NAT) + "1");
		stringMap.put("NATURAL1", String.valueOf(NAT) + "1");
		stringMap.put("true", String.valueOf(TRUE));
		stringMap.put("false", String.valueOf(FALSE));
		stringMap.put("or", String.valueOf(OR));
		stringMap.put("not", String.valueOf(NOT));
		stringMap.put("UNION", String.valueOf(UNION));
		stringMap.put("INTER", String.valueOf(INTER));
		stringMap.put("circ", String.valueOf(CIRC));

		Map<String, String> specialCharsMap = new HashMap<>();
		specialCharsMap.put("\subseteq", String.valueOf(SUBSET_EQ));
		specialCharsMap.put("\notin", String.valueOf(NOT_IN));
		specialCharsMap.put("\rightarrow", String.valueOf(RIGHT_ARROW));
		specialCharsMap.put("\\/", String.valueOf(SMALL_UNION));
		specialCharsMap.put("/\\", String.valueOf(SMALL_INTER));
		specialCharsMap.put("\forall", String.valueOf(FOR_ALL));
		specialCharsMap.put("\neq", String.valueOf(NOT_EQ));
		specialCharsMap.put("\times", String.valueOf(TIMES));
		specialCharsMap.put("\u222a", String.valueOf(SMALL_UNION));
		specialCharsMap.put("\bullet", String.valueOf(DOT));
		specialCharsMap.put("\not=", String.valueOf(NOT_EQ));
		specialCharsMap.put("\nat", String.valueOf(NAT));
		specialCharsMap.put("\setminus", String.valueOf(SET_MINUS));
		specialCharsMap.put("\triangleright", String.valueOf(RIGHT_TRI));
		specialCharsMap.put("\tfun", String.valueOf(RIGHT_ARROW));
		specialCharsMap.put("\bunion", String.valueOf(SMALL_UNION));
		specialCharsMap.put("\rel", String.valueOf(ARROW5));
		specialCharsMap.put("\neg", String.valueOf(NOT));
		specialCharsMap.put("{}", String.valueOf(EMPTY_SET));
		specialCharsMap.put("|", String.valueOf(VERTICAL_LINE));
		specialCharsMap.put("!=", String.valueOf(NOT_EQ));
		specialCharsMap.put("/=", String.valueOf(NOT_EQ));
		specialCharsMap.put(">=", String.valueOf(GEQ));
		specialCharsMap.put("-->>", String.valueOf(ARROW1));
		specialCharsMap.put("&", String.valueOf(AND));
		specialCharsMap.put("<=>", String.valueOf(IFF));
		specialCharsMap.put(".", String.valueOf(DOT));
		specialCharsMap.put("#", String.valueOf(EXISTS));
		specialCharsMap.put("\\pow(", String.valueOf(POW) + "(");
		specialCharsMap.put("POW(", String.valueOf(POW) + "(");
		specialCharsMap.put("POW1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("P(", String.valueOf(POW) + "(");
		specialCharsMap.put("P1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("..", String.valueOf(RANGE));
		specialCharsMap.put("><", String.valueOf(K_PRODUCT));
		specialCharsMap.put(">+>", String.valueOf(ARROW2));
		specialCharsMap.put("+->>", String.valueOf(ARROW3));
		specialCharsMap.put(">->>", String.valueOf(ARROW4));
		specialCharsMap.put("%", String.valueOf(LAMBDA));

		Map<String, String> regexMap = new HashMap<>();
//		regexMap.put("\\|(?!->)([^|]+)\\|(?!->)", "card($1)"); // replace |...| with card(...)
		regexMap.put("\\∣([^∣]+)\\∣", "card($1)"); // replace ∣...∣ with card(...)
		regexMap.put("\\\\math(bb|bf|cal|rm|it|frak|tt)\\{([^}]*)\\}", "$2"); // replace \mathXX{...} with ...
		regexMap.put("\\\\text(|bf|it|tt|sf|rm|sc|sl|normal|up)\\{([^}]*)\\}", "$2"); // replace \textXX{...} with ...
		regexMap.put("\\s*//.*$", ""); // remove comments
		regexMap.put("(?<![|<>+\\\\-])-(?![|<>+\\\\-])", String.valueOf(MINUS)); // replace "-" with "−"
		regexMap.put("(?<![<])=>", String.valueOf(IMPLIES)); // replace "=>" with "⇒"
		regexMap.put("!(?![=])", String.valueOf(FOR_ALL)); // replace "!" with "∀"
		regexMap.put("\\*{1,2}", String.valueOf(TIMES)); // replace "*|**" with "×"
		regexMap.put("<=(?![>])", String.valueOf(LEQ)); // replace "<=" with "≤"
		regexMap.put("(?<![<])<->(?![>])", String.valueOf(ARROW5)); // replace "<->" with "↔"
		regexMap.put("(?<![<])<\\|", String.valueOf(LEFT_TRI)); // replace "<|" with "◁"
		regexMap.put("<<\\|", String.valueOf(DOMAIN_ANTIRESTRICTION)); // replace "<<| with "⩤"
		regexMap.put("\\|>(?![>])", String.valueOf(RIGHT_TRI)); // replace "|>" with "▷"
		regexMap.put("\\|>>", String.valueOf(RANGE_ANTIRESTRICTION)); // replace "|>>" with "⩥"
		regexMap.put("\\+->(?![>])", String.valueOf(ARROW6)); // replace "+->" with "⇸"
		regexMap.put(">->(?![>])", String.valueOf(ARROW7)); // replace ">->" with "↣"
		regexMap.put("-->(?![>])", String.valueOf(RIGHT_ARROW)); // replace "-->" with "→"
		regexMap.put("\\|->(?![>])", String.valueOf(ARROW8)); // replace "|->" with "↦"
		regexMap.put("\\\\+pfun", String.valueOf(ARROW6));
		regexMap.put("\\\\+subseteq", String.valueOf(SUBSET_EQ));
		regexMap.put("\\\\+in", String.valueOf(IN));
		regexMap.put("\\\\+mapsto", String.valueOf(ARROW8));
		regexMap.put("\\\\+notin", String.valueOf(NOT_IN));
		regexMap.put("\\\\+rightarrow", String.valueOf(RIGHT_ARROW));
		regexMap.put("\\\\+forall", String.valueOf(FOR_ALL));
		regexMap.put("\\\\+neq", String.valueOf(NOT_EQ));
		regexMap.put("\\\\+times", String.valueOf(TIMES));
		regexMap.put("\\\\+u222a", String.valueOf(SMALL_UNION));
		regexMap.put("\\\\+land", String.valueOf(AND));
		regexMap.put("\\\\+bullet", String.valueOf(DOT));
		regexMap.put("\\\\+not=", String.valueOf(NOT_EQ));
		regexMap.put("\\\\+cup", String.valueOf(SMALL_UNION));
		regexMap.put("\\\\+nat", String.valueOf(NAT));
		regexMap.put("\\\\+emptyset", String.valueOf(EMPTY_SET));
		regexMap.put("\\\\+setminus", String.valueOf(SET_MINUS));
		regexMap.put("\\\\+leq", String.valueOf(LEQ));
		regexMap.put("\\\\+geq", String.valueOf(GEQ));
		regexMap.put("\\\\+triangleright", String.valueOf(RIGHT_TRI));
		regexMap.put("\\\\+mid", String.valueOf(VERTICAL_LINE));
		regexMap.put("\\\\+wedge", String.valueOf(AND));
		regexMap.put("\\\\+domsub", String.valueOf(RANGE_ANTIRESTRICTION));
		regexMap.put("\\\\+lor", String.valueOf(OR));
		regexMap.put("\\\\+union", String.valueOf(SMALL_UNION));
		regexMap.put("\\\\+pinj", String.valueOf(ARROW7));
		regexMap.put("\\\\+tfun", String.valueOf(RIGHT_ARROW));
		regexMap.put("\\\\+ovl", "<+");
		regexMap.put("\\\\+override", "<+");
		regexMap.put("\\\\+upto", String.valueOf(RANGE));
		regexMap.put("\\\\+qdot", String.valueOf(DOT));
		regexMap.put("\\\\+implies", String.valueOf(IMPLIES));
		regexMap.put("\\\\+domres", String.valueOf(DOMAIN_ANTIRESTRICTION));
		regexMap.put("\\\\+bunion", String.valueOf(SMALL_UNION));
		regexMap.put("\\\\+rel", String.valueOf(ARROW5));
		regexMap.put("\\\\+neg", String.valueOf(NOT));
		regexMap.put("\\\\+exists", String.valueOf(EXISTS));
		regexMap.put("\\\\+leftrightarrow", String.valueOf(ARROW5));
		regexMap.put("\\\\\\{", "{");
		regexMap.put("\\\\\\}", "}");

		originalString = ParserUtils.processColon(originalString);

		for (Entry<String, String> entry : prioritizedMap.entrySet()) {
			originalString = originalString.replace(entry.getKey(), entry.getValue());
		}

		for (Entry<String, String> entry : regexMap.entrySet()) {
			originalString = originalString.replaceAll(entry.getKey(), entry.getValue());
		}

		for (Entry<String, String> entry : specialCharsMap.entrySet()) {
			originalString = originalString.replace(entry.getKey(), entry.getValue());
		}

		for (Entry<String, String> entry : stringMap.entrySet()) {
			originalString = originalString.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
		}

		originalString.replace("\\\\", String.valueOf(SET_MINUS));
		originalString.replace("\\", String.valueOf(SET_MINUS));

		System.out.println("After processing: " + originalString);

		return originalString;
	}

	private static String processColon(String originalString) {
		boolean hasQuestionMark = false;
		char[] chars = originalString.toCharArray();
		List<Character> newChars = new ArrayList<>();

		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '?') {
				hasQuestionMark = true;
				newChars.add(c);
			} else if (c == ':') {
				if (hasQuestionMark && isSingleColon(chars, i)) {
					hasQuestionMark = false;
					newChars.add(c);
				} else {
					if (isSingleColon(chars, i)) {
						newChars.add(IN);
						continue;
					}
					if (i < chars.length - 1) {
						if (chars[i + 1] == '=') {
							newChars.add(ASSIGN);
							i++;
							continue;
						} else if (chars[i + 1] == '|') {
							newChars.add(':');
							newChars.add(VERTICAL_LINE);
							i++;
							continue;
						} else if (chars[i + 1] == ':') {
							newChars.add(':');
							newChars.add(IN);
							i++;
							continue;
						}
					}
					if (i > 2 && chars[i - 1] == '<' && chars[i - 2] == '<' && chars[i - 3] == '/') {
						newChars.remove(newChars.size() - 1);
						newChars.remove(newChars.size() - 1);
						newChars.remove(newChars.size() - 1);
						newChars.add(NOT_PROPER_SUBSET);
						continue;
					}
					if (i > 1) {
						if (chars[i - 1] == '<' && chars[i - 2] == '/') {
							newChars.remove(newChars.size() - 1);
							newChars.remove(newChars.size() - 1);
							newChars.add(NOT_SUBSET);
							continue;
						} else if (chars[i - 1] == '<' && chars[i - 2] == '<') {
							newChars.remove(newChars.size() - 1);
							newChars.remove(newChars.size() - 1);
							newChars.add(PROPER_SUBSET);
							continue;
						}
					}
					if (i > 0) {
						if (chars[i - 1] == '/') {
							newChars.remove(newChars.size() - 1);
							newChars.add(NOT_IN);
							continue;
						} else if (chars[i - 1] == '<') {
							newChars.remove(newChars.size() - 1);
							newChars.add(SUBSET_EQ);
							continue;
						}
					}
					newChars.add(c);
				}
			} else {
				newChars.add(c);
			}
		}

		char[] newCharsArray = new char[newChars.size()];
		for (int i = 0; i < newChars.size(); i++) {
			newCharsArray[i] = newChars.get(i);
		}
		return new String(newCharsArray);
	}

	private static boolean isSingleColon(char[] characters, int index) {
		if (index < 0 || index >= characters.length || characters[index] != ':') {
			return false;
		}
		if (index == 0) {
			return !(characters[index + 1] == ':' || characters[index + 1] == IN || characters[index + 1] == '='
					|| characters[index + 1] == '|' || characters[index + 1] == VERTICAL_LINE);
		}
		if (index == characters.length - 1) {
			return !(characters[index - 1] == '/' || characters[index - 1] == '<' || characters[index - 1] == ':');
		}
		return !(characters[index + 1] == ':' || characters[index + 1] == IN || characters[index + 1] == '='
				|| characters[index + 1] == '|' || characters[index + 1] == VERTICAL_LINE
				|| characters[index - 1] == '/' || characters[index - 1] == '<' || characters[index - 1] == ':');
	}

	public static String reverseLex(String originalString) {
		if (reverseLexMap == null) {
			initMap();
		}
		StringBuilder newString = new StringBuilder();
		char[] characters = originalString.toCharArray();
		for (char c : characters) {
			if (reverseLexMap.containsKey(c)) {
				newString.append(reverseLexMap.get(c));
			} else {
				newString.append(String.valueOf(c));
			}
		}
		return newString.toString();
	}

	public static String addMarker(String originalString, int start, int end) {
		StringBuilder newString = new StringBuilder();
		newString.append(originalString.substring(0, start));
		newString.append("***");
		newString.append(originalString.substring(start, end));
		newString.append("***");
		newString.append(originalString.substring(end));
		return newString.toString();
	}

	static void show(char c) {
		System.out.printf("%c -> \\u%04X%n", c, (int) c);
	}

	public static void main(String[] args) {
		show('≤');
	}

}
