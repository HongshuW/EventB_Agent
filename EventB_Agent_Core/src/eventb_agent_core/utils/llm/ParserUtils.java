package eventb_agent_core.utils.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ParserUtils {

	public static char INT = '\u2124';
	private static char NAT = '\u2115';
	private static char POW = '\u2119';
	private static char EMPTY_SET = '\u2205';

	private static char TRUE = '\u22A4';
	private static char FALSE = '\u22A5';
	private static char OR = '\u2228';
	private static char NOT = '\u00AC';
	private static char AND = '\u2227';

	private static char GENERAL_UNION = '\u22C3';
	private static char GENERAL_INTER = '\u22C2';
	private static char UNION = '\u222A';
	private static char INTER = '\u2229';
	private static char UPTO = '\u2025';
	private static char SET_MINUS = '\u2216';
	private static char CARTESIAN_PRODUCT = '\u00D7';
	private static char MINUS = '\u2212';
	private static char DOT = '\u00B7';
	private static char TIMES = '\u2217';
	private static char DIRECT_PRODUCT = '\u2297';
	private static char RANGE_RESTRICTION = '\u25B7';
	private static char DOMAIN_RESTRICTION = '\u25C1';
	private static char DOMAIN_SUBTRACTION = '\u2A64';
	private static char RANGE_SUBTRACTION = '\u2A65';
	private static char BACKWARD_COMPOSITION = '\u2218';

	private static char RELATION = '\u2194'; // <->
	private static char TOTAL_SURJECTIVE_RELATION = '\uE102'; // <<->>
	private static char TOTAL_RELATION = '\uE100'; // <<->
	private static char SURJECTIVE_RELATION = '\uE101'; // <->>
	private static char TOTAL_FUNCTION = '\u2192'; // -->
	private static char PARTIAL_FUNCTION = '\u21F8'; // +->
	private static char TOTAL_SURJECTION = '\u21A0'; // -->>
	private static char PARTIAL_SURJECTION = '\u2900'; // +->>
	private static char TOTAL_INJECTION = '\u21A3'; // >->
	private static char PARTIAL_INJECTION = '\u2914'; // >+>
	private static char BIJECTION = '\u2916'; // >->>
	private static char MAP_LET = '\u21A6'; // |->

	public static char ELEMENT_OF = '\u2208';
	private static char SUBSET = '\u2286';
	private static char STRICT_SUBSET = '\u2282';
	private static char NOT_ELEMENT_OF = '\u2209';
	private static char NOT_SUBSET = '\u2288';
	private static char NOT_STRICT_SUBSET = '\u2284';

	private static char NOT_EQ = '\u2260';
	private static char LEQ = '\u2264';
	private static char GEQ = '\u2265';
	private static char ASSIGN = '\u2254';

	private static char FOR_ALL = '\u2200';
	private static char EQUIVALENCE = '\u21D4';
	private static char EXISTS = '\u2203';
	private static char LAMBDA = '\u03BB';
	private static char IMPLIES = '\u21D2';
	private static char SUCH_THAT = '\u2223';

	private static char RELATIONAL_OVERRIDING = '\uE103'; // <+
	private static char PARALLEL_PRODUCT = '\u2225'; // ||
	private static char TILDE = '\u223C'; // ~
	private static char EXPONENTIATION = '\u005E'; // ^
	private static char DIVISION = '\u00F7';
	private static char OF_TYPE = '\u2982';

	private static Map<Character, String> reverseLexMap;

	static void show(String s) {
		for (char c : s.toCharArray()) {
			System.out.printf("%c -> \\u%04X%n", c, (int) c);
		}
	}

	public static void main(String[] args) {
		show("∗");
	}

	private static void initMap() {
		reverseLexMap = new HashMap<>();
		reverseLexMap.put(INT, "INT");
		reverseLexMap.put(NAT, "NAT");
		reverseLexMap.put(POW, "POW");
		reverseLexMap.put(EMPTY_SET, "{}");
		reverseLexMap.put(TRUE, "\\top");
		reverseLexMap.put(FALSE, "\\bot");
		reverseLexMap.put(OR, "\\lor");
		reverseLexMap.put(NOT, "\\neg");
		reverseLexMap.put(AND, "\\land");
		reverseLexMap.put(GENERAL_UNION, "\\bigcup");
		reverseLexMap.put(GENERAL_INTER, "\\bigcap");
		reverseLexMap.put(UNION, "\\cup");
		reverseLexMap.put(INTER, "\\cap");
		reverseLexMap.put(UPTO, "..");
		reverseLexMap.put(SET_MINUS, "\\setminus");
		reverseLexMap.put(CARTESIAN_PRODUCT, "\\times");
		reverseLexMap.put(MINUS, "-");
		reverseLexMap.put(DOT, "\\cdot");
		reverseLexMap.put(DIRECT_PRODUCT, "><");
		reverseLexMap.put(RANGE_RESTRICTION, "\\rhd");
		reverseLexMap.put(DOMAIN_RESTRICTION, "\\lhd");
		reverseLexMap.put(DOMAIN_SUBTRACTION, "<<|");
		reverseLexMap.put(RANGE_SUBTRACTION, "|>>");
		reverseLexMap.put(SUCH_THAT, "\\mid");
		reverseLexMap.put(BACKWARD_COMPOSITION, "\\circ");
		reverseLexMap.put(TOTAL_FUNCTION, "\\rightarrow");
		reverseLexMap.put(TOTAL_SURJECTION, "-->>");
		reverseLexMap.put(PARTIAL_INJECTION, ">+>");
		reverseLexMap.put(PARTIAL_SURJECTION, "+->>");
		reverseLexMap.put(BIJECTION, ">->>");
		reverseLexMap.put(RELATION, "\\leftrightarrow");
		reverseLexMap.put(PARTIAL_FUNCTION, "\\rightharpoonup");
		reverseLexMap.put(TOTAL_INJECTION, ">->");
		reverseLexMap.put(MAP_LET, "\\mapsto");
		reverseLexMap.put(ELEMENT_OF, "\\in");
		reverseLexMap.put(SUBSET, "\\subseteq");
		reverseLexMap.put(NOT_ELEMENT_OF, "\\notin");
		reverseLexMap.put(NOT_STRICT_SUBSET, "\\nsubset");
		reverseLexMap.put(NOT_SUBSET, "\\nsubseteq");
		reverseLexMap.put(STRICT_SUBSET, "\\subset");
		reverseLexMap.put(NOT_EQ, "\\neq");
		reverseLexMap.put(LEQ, "\\leq");
		reverseLexMap.put(GEQ, "\\geq");
		reverseLexMap.put(ASSIGN, ":=");
		reverseLexMap.put(FOR_ALL, "\\forall");
		reverseLexMap.put(EQUIVALENCE, "\\Leftrightarrow");
		reverseLexMap.put(EXISTS, "\\exists");
		reverseLexMap.put(LAMBDA, "\\lambda");
		reverseLexMap.put(IMPLIES, "\\Rightarrow");
		reverseLexMap.put(TOTAL_SURJECTIVE_RELATION, "<<->>");
		reverseLexMap.put(TOTAL_RELATION, "<<->");
		reverseLexMap.put(SURJECTIVE_RELATION, "<->>");
		reverseLexMap.put(RELATIONAL_OVERRIDING, "\\oplus");
		reverseLexMap.put(PARALLEL_PRODUCT, "\\parallel");
		reverseLexMap.put(TILDE, "~");
		reverseLexMap.put(EXPONENTIATION, "^");
		reverseLexMap.put(DIVISION, "/");
		reverseLexMap.put(OF_TYPE, "\\oftype");
		reverseLexMap.put(TIMES, "*");
	}

	public static String addEscape(String originalString) {
		return originalString.replace("\\", "\\\\");
	}

	public static String lex(String originalString) {

		Map<String, String> prioritizedMap = new HashMap<>();
		prioritizedMap.put("||", String.valueOf(PARALLEL_PRODUCT));
		prioritizedMap.put("..", String.valueOf(UPTO));

		Map<String, String> stringMap = new HashMap<>();
		stringMap.put("Z", String.valueOf(INT));
		stringMap.put("INT", String.valueOf(INT));
		stringMap.put("INTEGER", String.valueOf(INT));
		stringMap.put("N", String.valueOf(NAT));
		stringMap.put("NAT", String.valueOf(NAT));
		stringMap.put("NATURAL", String.valueOf(NAT));
		stringMap.put("nat", String.valueOf(NAT));
		stringMap.put("N1", String.valueOf(NAT) + "1");
		stringMap.put("NAT1", String.valueOf(NAT) + "1");
		stringMap.put("NATURAL1", String.valueOf(NAT) + "1");
		stringMap.put("nat1", String.valueOf(NAT) + "1");
		stringMap.put("true", String.valueOf(TRUE));
		stringMap.put("false", String.valueOf(FALSE));
		stringMap.put("or", String.valueOf(OR));
		stringMap.put("not", String.valueOf(NOT));
		stringMap.put("UNION", String.valueOf(GENERAL_UNION));
		stringMap.put("INTER", String.valueOf(GENERAL_INTER));
		stringMap.put("circ", String.valueOf(BACKWARD_COMPOSITION));

		Map<String, String> specialCharsMap = new HashMap<>();
		specialCharsMap.put("\subseteq", String.valueOf(SUBSET));
		specialCharsMap.put("\subset ", String.valueOf(STRICT_SUBSET) + " ");
		specialCharsMap.put("\nsubset ", String.valueOf(NOT_STRICT_SUBSET) + " ");
		specialCharsMap.put("\nsubseteq", String.valueOf(NOT_SUBSET));
		specialCharsMap.put("\notin", String.valueOf(NOT_ELEMENT_OF));
		specialCharsMap.put("\rightarrow", String.valueOf(TOTAL_FUNCTION));
		specialCharsMap.put("\\/", String.valueOf(UNION));
		specialCharsMap.put("/\\", String.valueOf(INTER));
		specialCharsMap.put("\forall", String.valueOf(FOR_ALL));
		specialCharsMap.put("\neq", String.valueOf(NOT_EQ));
		specialCharsMap.put("\noteq", String.valueOf(NOT_EQ));
		specialCharsMap.put("\times", String.valueOf(CARTESIAN_PRODUCT));
		specialCharsMap.put("\bullet", String.valueOf(DOT));
		specialCharsMap.put("\not=", String.valueOf(NOT_EQ));
		specialCharsMap.put("\nat", String.valueOf(NAT));
		specialCharsMap.put("\setminus", String.valueOf(SET_MINUS));
		specialCharsMap.put("\triangleright", String.valueOf(RANGE_RESTRICTION));
		specialCharsMap.put("\triangleleft", String.valueOf(DOMAIN_RESTRICTION));
		specialCharsMap.put("\rhd", String.valueOf(RANGE_RESTRICTION));
		specialCharsMap.put("\tfun", String.valueOf(TOTAL_FUNCTION));
		specialCharsMap.put("\fun", String.valueOf(TOTAL_FUNCTION));
		specialCharsMap.put("\bunion", String.valueOf(UNION));
		specialCharsMap.put("\rel", String.valueOf(RELATION));
		specialCharsMap.put("\neg", String.valueOf(NOT));
		specialCharsMap.put("\rightharpoonup", String.valueOf(PARTIAL_FUNCTION));
		specialCharsMap.put("\binter", String.valueOf(INTER));
		specialCharsMap.put("\bigcup", String.valueOf(GENERAL_UNION));
		specialCharsMap.put("\bigcap", String.valueOf(GENERAL_INTER));
		specialCharsMap.put("\rangle", ">");
		specialCharsMap.put("\succ", String.valueOf(LEQ));
		specialCharsMap.put("\top", String.valueOf(TRUE));
		specialCharsMap.put("\bot", String.valueOf(FALSE));
		specialCharsMap.put("\bcmeq", String.valueOf(ASSIGN));
		specialCharsMap.put("\bmod", "mod");
		specialCharsMap.put("\bij", String.valueOf(EQUIVALENCE));
		specialCharsMap.put("{}", String.valueOf(EMPTY_SET));
		specialCharsMap.put("|", String.valueOf(SUCH_THAT));
		specialCharsMap.put("!=", String.valueOf(NOT_EQ));
		specialCharsMap.put("/=", String.valueOf(NOT_EQ));
		specialCharsMap.put(">=", String.valueOf(GEQ));
		specialCharsMap.put("-->>", String.valueOf(TOTAL_SURJECTION));
		specialCharsMap.put("&", String.valueOf(AND));
		specialCharsMap.put("<=>", String.valueOf(EQUIVALENCE));
		specialCharsMap.put(".", String.valueOf(DOT));
		specialCharsMap.put("#", String.valueOf(EXISTS));
		specialCharsMap.put("\\pow(", String.valueOf(POW) + "(");
		specialCharsMap.put("\\pow1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("POW(", String.valueOf(POW) + "(");
		specialCharsMap.put("POW1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("\\power(", String.valueOf(POW) + "(");
		specialCharsMap.put("\\power1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("P(", String.valueOf(POW) + "(");
		specialCharsMap.put("P1(", String.valueOf(POW) + "1(");
		specialCharsMap.put("><", String.valueOf(DIRECT_PRODUCT));
		specialCharsMap.put(">+>", String.valueOf(PARTIAL_INJECTION));
		specialCharsMap.put("+->>", String.valueOf(PARTIAL_SURJECTION));
		specialCharsMap.put(">->>", String.valueOf(BIJECTION));
		specialCharsMap.put("%", String.valueOf(LAMBDA));
		specialCharsMap.put("<<->>", String.valueOf(TOTAL_SURJECTIVE_RELATION));
		specialCharsMap.put("<<->", String.valueOf(TOTAL_RELATION));
		specialCharsMap.put("<->>", String.valueOf(SURJECTIVE_RELATION));
		specialCharsMap.put("<+", String.valueOf(RELATIONAL_OVERRIDING));
		specialCharsMap.put("~", String.valueOf(TILDE));
		specialCharsMap.put("^", String.valueOf(EXPONENTIATION));
		specialCharsMap.put("*", String.valueOf(TIMES));

		Map<String, String> regexMap = new HashMap<>();
//		regexMap.put("\\|(?!->)([^|]+)\\|(?!->)", "card($1)"); // replace |...| with card(...)
//		regexMap.put("\\∣([^∣]+)\\∣", "card($1)"); // replace ∣...∣ with card(...)
		regexMap.put("\\\\math(bb|bf|cal|rm|it|frak|tt|bin)\\{([^}]*)\\}", "$2"); // replace \mathXX{...} with ...
		regexMap.put("\\\\text(|bf|it|tt|sf|rm|sc|sl|normal|up)\\{([^}]*)\\}", "$2"); // replace \textXX{...} with ...
		regexMap.put("\\\\operatorname\\{([^}]*)\\}", "$2"); // replace \operatorname{...} with ...
		regexMap.put("\\s*//.*$", ""); // remove comments
		regexMap.put("(?<![|<>+\\\\-])-(?![|<>+\\\\-])", String.valueOf(MINUS)); // replace "-" with "−"
		regexMap.put("(?<![<])=>", String.valueOf(IMPLIES)); // replace "=>" with "⇒"
		regexMap.put("!(?![=])", String.valueOf(FOR_ALL)); // replace "!" with "∀"
		regexMap.put("\\*{2}", String.valueOf(CARTESIAN_PRODUCT)); // replace "**" with "×"
		regexMap.put("<=(?![>])", String.valueOf(LEQ)); // replace "<=" with "≤"
		regexMap.put("(?<![<])<->(?![>])", String.valueOf(RELATION)); // replace "<->" with "↔"
		regexMap.put("(?<![<])<\\|", String.valueOf(DOMAIN_RESTRICTION)); // replace "<|" with "◁"
		regexMap.put("<<\\|", String.valueOf(DOMAIN_SUBTRACTION)); // replace "<<| with "⩤"
		regexMap.put("\\|>(?![>])", String.valueOf(RANGE_RESTRICTION)); // replace "|>" with "▷"
		regexMap.put("\\|>>", String.valueOf(RANGE_SUBTRACTION)); // replace "|>>" with "⩥"
		regexMap.put("\\+->(?![>])", String.valueOf(PARTIAL_FUNCTION)); // replace "+->" with "⇸"
		regexMap.put(">->(?![>])", String.valueOf(TOTAL_INJECTION)); // replace ">->" with "↣"
		regexMap.put("-->(?![>])", String.valueOf(TOTAL_FUNCTION)); // replace "-->" with "→"
		regexMap.put("\\|->(?![>])", String.valueOf(MAP_LET)); // replace "|->" with "↦"
		regexMap.put("(?<![\\\\])/(?![=:<\\\\])", String.valueOf(DIVISION)); // replace "/" with DIVISION
		regexMap.put("\\\\+pfun", String.valueOf(PARTIAL_FUNCTION));
		regexMap.put("\\\\+subseteq", String.valueOf(SUBSET));
		regexMap.put("\\\\+subset(?!e)", String.valueOf(STRICT_SUBSET));
		regexMap.put("\\\\+nsubset(?!e)", String.valueOf(NOT_STRICT_SUBSET));
		regexMap.put("\\\\+nsubseteq", String.valueOf(NOT_SUBSET));
		regexMap.put("\\\\+in(?!j)", String.valueOf(ELEMENT_OF));
		regexMap.put("\\\\+inj", String.valueOf(TOTAL_INJECTION));
		regexMap.put("\\\\+mapsto", String.valueOf(MAP_LET));
		regexMap.put("\\\\+notin", String.valueOf(NOT_ELEMENT_OF));
		regexMap.put("\\\\+rightarrow", String.valueOf(TOTAL_FUNCTION));
		regexMap.put("\\\\+forall", String.valueOf(FOR_ALL));
		regexMap.put("\\\\+neq", String.valueOf(NOT_EQ));
		regexMap.put("\\\\+noteq", String.valueOf(NOT_EQ));
		regexMap.put("\\\\+times", String.valueOf(CARTESIAN_PRODUCT));
		regexMap.put("\\\\+land", String.valueOf(AND));
		regexMap.put("\\\\+lnot", String.valueOf(NOT));
		regexMap.put("\\\\+bullet", String.valueOf(DOT));
		regexMap.put("\\\\+not=", String.valueOf(NOT_EQ));
		regexMap.put("\\\\+cup", String.valueOf(UNION));
		regexMap.put("\\\\+cap", String.valueOf(INTER));
		regexMap.put("\\\\+nat", String.valueOf(NAT));
		regexMap.put("\\\\+emptyset", String.valueOf(EMPTY_SET));
		regexMap.put("\\\\+setminus", String.valueOf(SET_MINUS));
		regexMap.put("\\\\+leq", String.valueOf(LEQ));
		regexMap.put("\\\\+le(?!f|q)", String.valueOf(LEQ));
		regexMap.put("\\\\+succ", String.valueOf(LEQ));
		regexMap.put("\\\\+geq", String.valueOf(GEQ));
		regexMap.put("\\\\+ge(?!q)", String.valueOf(GEQ));
		regexMap.put("\\\\+prec", String.valueOf(GEQ));
		regexMap.put("\\\\+triangleright", String.valueOf(RANGE_RESTRICTION));
		regexMap.put("\\\\+rhd", String.valueOf(RANGE_RESTRICTION));
		regexMap.put("\\\\+lhd", String.valueOf(DOMAIN_RESTRICTION));
		regexMap.put("\\\\+mid", String.valueOf(SUCH_THAT));
		regexMap.put("\\\\+wedge", String.valueOf(AND));
		regexMap.put("\\\\+domsub", String.valueOf(RANGE_SUBTRACTION));
		regexMap.put("\\\\+lor", String.valueOf(OR));
		regexMap.put("\\\\+union", String.valueOf(UNION));
		regexMap.put("\\\\+pinj", String.valueOf(TOTAL_INJECTION));
		regexMap.put("\\\\+tfun", String.valueOf(TOTAL_FUNCTION));
		regexMap.put("\\\\+fun", String.valueOf(TOTAL_FUNCTION));
		regexMap.put("\\\\+ovl", String.valueOf(RELATIONAL_OVERRIDING));
		regexMap.put("\\\\+override", String.valueOf(RELATIONAL_OVERRIDING));
		regexMap.put("\\\\+oplus", String.valueOf(RELATIONAL_OVERRIDING));
		regexMap.put("\\\\+upto", String.valueOf(UPTO));
		regexMap.put("\\\\+qdot", String.valueOf(DOT));
		regexMap.put("\\\\+implies", String.valueOf(IMPLIES));
		regexMap.put("\\\\+limp", String.valueOf(IMPLIES));
		regexMap.put("\\\\+Rightarrow", String.valueOf(IMPLIES));
		regexMap.put("\\\\+domres", String.valueOf(DOMAIN_SUBTRACTION));
		regexMap.put("\\\\+bunion", String.valueOf(UNION));
		regexMap.put("\\\\+rel", String.valueOf(RELATION));
		regexMap.put("\\\\+neg", String.valueOf(NOT));
		regexMap.put("\\\\+exists", String.valueOf(EXISTS));
		regexMap.put("\\\\+leftrightarrow", String.valueOf(RELATION));
		regexMap.put("\\\\+cdot", String.valueOf(DOT));
		regexMap.put("\\\\+divide", String.valueOf(DIVISION));
		regexMap.put("\\\\+oftype", String.valueOf(OF_TYPE));
		regexMap.put("\\\\+iff", String.valueOf(EQUIVALENCE));
		regexMap.put("\\\\+Leftrightarrow", String.valueOf(EQUIVALENCE));
		regexMap.put("\\\\+lambda", String.valueOf(LAMBDA));
		regexMap.put("\\\\+rightharpoonup", String.valueOf(PARTIAL_FUNCTION));
		regexMap.put("\\\\+dom\\(", "dom(");
		regexMap.put("\\\\+binter", String.valueOf(INTER));
		regexMap.put("\\\\+bigcup", String.valueOf(GENERAL_UNION));
		regexMap.put("\\\\+bigcap", String.valueOf(GENERAL_INTER));
		regexMap.put("\\\\+langle", "<");
		regexMap.put("\\\\+rangle", ">");
		regexMap.put("\\\\+top", String.valueOf(TRUE));
		regexMap.put("\\\\+bot", String.valueOf(TRUE));
		regexMap.put("\\\\+circ", String.valueOf(BACKWARD_COMPOSITION));
		regexMap.put("\\\\+parallel", String.valueOf(PARALLEL_PRODUCT));
		regexMap.put("\\\\+bcmeq", String.valueOf(ASSIGN));
		regexMap.put("\\\\+div", String.valueOf(DIVISION));
		regexMap.put("\\\\+Longrightarrow", String.valueOf(IMPLIES));
		regexMap.put("\\\\+varnothing", String.valueOf(EMPTY_SET));
		regexMap.put("\\\\+NAT", String.valueOf(NAT));
		regexMap.put("\\\\+INT", String.valueOf(INT));
		regexMap.put("\\\\+BOOL", "BOOL");
		regexMap.put("\\\\+bmod", "mod");
		regexMap.put("\\\\+bij", String.valueOf(EQUIVALENCE));
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

		originalString.replace("\\\\ ", String.valueOf(SET_MINUS) + " ");
		originalString.replace("\\ ", String.valueOf(SET_MINUS) + " ");

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
						newChars.add(ELEMENT_OF);
						continue;
					}
					if (i < chars.length - 1) {
						if (chars[i + 1] == '=') {
							newChars.add(ASSIGN);
							i++;
							continue;
						} else if (chars[i + 1] == '|') {
							newChars.add(':');
							newChars.add(SUCH_THAT);
							i++;
							continue;
						} else if (chars[i + 1] == ':') {
							newChars.add(':');
							newChars.add(ELEMENT_OF);
							i++;
							continue;
						}
					}
					if (i > 2 && chars[i - 1] == '<' && chars[i - 2] == '<' && chars[i - 3] == '/') {
						newChars.remove(newChars.size() - 1);
						newChars.remove(newChars.size() - 1);
						newChars.remove(newChars.size() - 1);
						newChars.add(NOT_STRICT_SUBSET);
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
							newChars.add(STRICT_SUBSET);
							continue;
						}
					}
					if (i > 0) {
						if (chars[i - 1] == '/') {
							newChars.remove(newChars.size() - 1);
							newChars.add(NOT_ELEMENT_OF);
							continue;
						} else if (chars[i - 1] == '<') {
							newChars.remove(newChars.size() - 1);
							newChars.add(SUBSET);
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
			return !(characters[index + 1] == ':' || characters[index + 1] == ELEMENT_OF || characters[index + 1] == '='
					|| characters[index + 1] == '|' || characters[index + 1] == SUCH_THAT);
		}
		if (index == characters.length - 1) {
			return !(characters[index - 1] == '/' || characters[index - 1] == '<' || characters[index - 1] == ':');
		}
		return !(characters[index + 1] == ':' || characters[index + 1] == ELEMENT_OF || characters[index + 1] == '='
				|| characters[index + 1] == '|' || characters[index + 1] == SUCH_THAT || characters[index - 1] == '/'
				|| characters[index - 1] == '<' || characters[index - 1] == ':');
	}

	public static String reverseLex(String originalString) {
		return reverseLex(originalString, 0);
	}

	public static String reverseLex(String originalString, int spaceCount) {
		if (reverseLexMap == null) {
			initMap();
		}
		String space = "";
		for (int i = 0; i < spaceCount; i++) {
			space += " ";
		}

		StringBuilder newString = new StringBuilder();
		char[] characters = originalString.toCharArray();
		for (int i = 0; i < characters.length; i++) {
			char c = characters[i];
			if (c == POW || c == NAT || c == INT) {
				newString.append(reverseLexMap.get(c));
			} else if (reverseLexMap.containsKey(c)) {
				newString.append(space + reverseLexMap.get(c) + space);
			} else {
				newString.append(String.valueOf(c));
			}
		}
		return newString.toString();
	}

	public static boolean isValidCharacter(char c) {
		if (reverseLexMap == null) {
			initMap();
		}
		return reverseLexMap.containsKey(c);
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

}
