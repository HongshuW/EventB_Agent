package eventb_agent_core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ParserUtils {

	public static String lex(String originalString) {

		Map<String, String> prioritizedMap = new HashMap<>();
		prioritizedMap.put("\\rightarrow", "→");
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
		stringMap.put("true", "⊤");
		stringMap.put("false", "⊥");
		stringMap.put("or", "∨");
		stringMap.put("not", "¬");
		stringMap.put("UNION", "⋃");
		stringMap.put("INTER", "⋂");
		stringMap.put("circ", "∘");

		Map<String, String> specialCharsMap = new HashMap<>();
		specialCharsMap.put("\\pfun", "⇸");
		specialCharsMap.put("\\subseteq", "⊆");
		specialCharsMap.put("\\notin", "∉");
		specialCharsMap.put("\\in", "∈");
		specialCharsMap.put("\\mapsto", "↦");
		specialCharsMap.put("\\/", "∪");
		specialCharsMap.put("/\\", "∩");
		specialCharsMap.put("\\", "∖");
		specialCharsMap.put("\\forall", "∀");
		specialCharsMap.put("\\neq", "≠");
		specialCharsMap.put("\\times", "×");
		specialCharsMap.put("\\u222a", "∪");
		specialCharsMap.put("{}", "∅");
		specialCharsMap.put("|", "∣");
		specialCharsMap.put(":=", "≔");
		specialCharsMap.put("!=", "≠");
		specialCharsMap.put("/=", "≠");
		specialCharsMap.put(">=", "≥");
		specialCharsMap.put("∣−>", "↦");
		specialCharsMap.put("-->>", "↠");
		specialCharsMap.put("&", "∧");
		specialCharsMap.put("<=>", "⇔");
		specialCharsMap.put(".", "·");
		specialCharsMap.put("#", "∃");
		specialCharsMap.put("POW(", "ℙ(");
		specialCharsMap.put("POW1(", "ℙ1(");
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
		regexMap.put("\\|([^|]+)\\|", "card($1)"); // replace |...| with card(...)
		regexMap.put("\\∣([^∣]+)\\∣", "card($1)"); // replace ∣...∣ with card(...)
		regexMap.put("\\s*//.*$", ""); // remove comments
		regexMap.put("(?<![|<>+\\\\-])-(?![|<>+\\\\-])", "−"); // replace "-" with "−"
		regexMap.put("\\\\u00[0-9A-Fa-f]*\\b", ""); // remove invalid unicode
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

		return originalString;

	}

}
