package eventb_agent_core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ParserUtils {
	
	public static String addEscape(String originalString) {
		return originalString.replace("\\", "\\\\");
	}

	public static String lex(String originalString) {

		Map<String, String> prioritizedMap = new HashMap<>();
		prioritizedMap.put("||", "∥");

		Map<String, String> stringMap = new HashMap<>();
		stringMap.put("Z", "ℤ");
		stringMap.put("INT", "ℤ");
		stringMap.put("INTEGER", "ℤ");
		stringMap.put("N", "ℕ");
		stringMap.put("NAT", "ℕ");
		stringMap.put("NATURAL", "ℕ");
		stringMap.put("N1", "ℕ1");
		stringMap.put("NAT1", "ℕ1");
		stringMap.put("NATURAL1", "ℕ1");
		stringMap.put("true", "⊤");
		stringMap.put("false", "⊥");
		stringMap.put("or", "∨");
		stringMap.put("not", "¬");
		stringMap.put("UNION", "⋃");
		stringMap.put("INTER", "⋂");
		stringMap.put("circ", "∘");

		Map<String, String> specialCharsMap = new HashMap<>();
		specialCharsMap.put("\subseteq", "⊆");
		specialCharsMap.put("\notin", "∉");
		specialCharsMap.put("\rightarrow", "→");
		specialCharsMap.put("\\/", "∪");
		specialCharsMap.put("/\\", "∩");
		specialCharsMap.put("\forall", "∀");
		specialCharsMap.put("\neq", "≠");
		specialCharsMap.put("\times", "×");
		specialCharsMap.put("\u222a", "∪");
		specialCharsMap.put("\bullet", "·");
		specialCharsMap.put("\not=", "≠");
		specialCharsMap.put("\nat", "ℕ");
		specialCharsMap.put("\setminus", "∖");
		specialCharsMap.put("\triangleright", "▷");
		specialCharsMap.put("\tfun", "→");
		specialCharsMap.put("\bunion", "∪");
		specialCharsMap.put("{}", "∅");
		specialCharsMap.put("|", "∣");
		specialCharsMap.put(":=", "≔");
		specialCharsMap.put("!=", "≠");
		specialCharsMap.put("/=", "≠");
		specialCharsMap.put(">=", "≥");
		specialCharsMap.put("∣−>", "↦");
		specialCharsMap.put("|−>", "↦");
		specialCharsMap.put("-->>", "↠");
		specialCharsMap.put("&", "∧");
		specialCharsMap.put("<=>", "⇔");
		specialCharsMap.put(".", "·");
		specialCharsMap.put("#", "∃");
		specialCharsMap.put("POW(", "ℙ(");
		specialCharsMap.put("POW1(", "ℙ1(");
		specialCharsMap.put("P(", "ℙ(");
		specialCharsMap.put("P1(", "ℙ1(");
		specialCharsMap.put("/:", "∉");
		specialCharsMap.put("/<:", "⊈");
		specialCharsMap.put("/<<:", "⊄");
		specialCharsMap.put("..", "‥");
		specialCharsMap.put("><", "⊗");
		specialCharsMap.put(">+>", "⤔");
		specialCharsMap.put("+->>", "⤀");
		specialCharsMap.put(">->>", "⤖");
		specialCharsMap.put("%", "λ");
		specialCharsMap.put("::", ":∈");
		specialCharsMap.put(":|", ":∣");

		Map<String, String> regexMap = new HashMap<>();
		regexMap.put("\\|(?!->)([^|]+)\\|(?!->)", "card($1)"); // replace |...| with card(...)
		regexMap.put("\\∣([^∣]+)\\∣", "card($1)"); // replace ∣...∣ with card(...)
		regexMap.put("\\\\math(bb|bf|cal|rm|it|frak|tt)\\{([^}]*)\\}", "$2"); // replace \mathXX{...} with ...
		regexMap.put("\\\\text(|bf|it|tt|sf|rm|sc|sl|normal|up)\\{([^}]*)\\}", "$2"); // replace \textXX{...} with ...
		regexMap.put("\\s*//.*$", ""); // remove comments
		regexMap.put("(?<![|<>+\\\\-])-(?![|<>+\\\\-])", "−"); // replace "-" with "−"
		regexMap.put("(?<![<])=>", "⇒"); // replace "=>" with "⇒"
		regexMap.put("!(?![=])", "∀"); // replace "!" with "∀"
		regexMap.put("(?<![\\\\/=<>:\\|]):(?![\\\\/=<>:\\|])", "∈"); // replace ":" with "∈"
		regexMap.put("(?<![</])<:(?![\\\\/=<>:\\|])", "⊆"); // replace "<:" with "⊆"
		regexMap.put("(?<![/])<<:(?![\\\\/=<>:\\|])", "⊂"); // replace "<<:" with "⊂"
		regexMap.put("\\*{1,2}", "×"); // replace "*|**" with "×"
		regexMap.put("<=(?![>])", "≤"); // replace "<=" with "≤"
		regexMap.put("(?<![<])<->(?![>])", "↔"); // replace "<->" with "↔"
		regexMap.put("(?<![<])<\\|", "◁"); // replace "<|" with "◁"
		regexMap.put("<<\\|", "⩤"); // replace "<<|" with "⩤"
		regexMap.put("\\|>(?![>])", "▷"); // replace "|>" with "▷"
		regexMap.put("\\|>>", "⩥"); // replace "|>>" with "⩥"
		regexMap.put("\\+->(?![>])", "⇸"); // replace "+->" with "⇸"
		regexMap.put(">->(?![>])", "↣"); // replace ">->" with "↣"
		regexMap.put("-->(?![>])", "→"); // replace "-->" with "→"
		regexMap.put("\\\\+pfun", "⇸");
		regexMap.put("\\\\+subseteq", "⊆");
		regexMap.put("\\\\+in", "∈");
		regexMap.put("\\\\+mapsto", "↦");
		regexMap.put("\\\\+notin", "∉");
		regexMap.put("\\\\+rightarrow", "→");
		regexMap.put("\\\\+forall", "∀");
		regexMap.put("\\\\+neq", "≠");
		regexMap.put("\\\\+times", "×");
		regexMap.put("\\\\+u222a", "∪");
		regexMap.put("\\\\+land", "∧");
		regexMap.put("\\\\+bullet", "·");
		regexMap.put("\\\\+not=", "≠");
		regexMap.put("\\\\+cup", "∪");
		regexMap.put("\\\\+nat", "ℕ");
		regexMap.put("\\\\+emptyset", "∅");
		regexMap.put("\\\\+setminus", "∖");
		regexMap.put("\\\\+leq", "≤");
		regexMap.put("\\\\+geq", "≥");
		regexMap.put("\\\\+triangleright", "▷");
		regexMap.put("\\\\+mid", "∣");
		regexMap.put("\\\\+wedge", "∧");
		regexMap.put("\\\\+domsub", "⩥");
		regexMap.put("\\\\+lor", "∨");
		regexMap.put("\\\\+union", "∪");
		regexMap.put("\\\\+pinj", "↣");
		regexMap.put("\\\\+tfun", "→");
		regexMap.put("\\\\+ovl", "<+");
		regexMap.put("\\\\+override", "<+");
		regexMap.put("\\\\+upto", "‥");
		regexMap.put("\\\\+qdot", "·");
		regexMap.put("\\\\+implies", "⇒");
		regexMap.put("\\\\+domres", "⩤");
		regexMap.put("\\\\+bunion", "∪");
		regexMap.put("\\\\\\{", "{");
		regexMap.put("\\\\\\}", "}");

		Map<String, String> leastPrioritizedMap = new HashMap<>();
		leastPrioritizedMap.put("\\\\", "∖");

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

		for (Entry<String, String> entry : leastPrioritizedMap.entrySet()) {
			originalString = originalString.replace(entry.getKey(), entry.getValue());
		}

		return originalString;
	}

}
