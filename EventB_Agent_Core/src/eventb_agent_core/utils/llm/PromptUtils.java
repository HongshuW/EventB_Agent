package eventb_agent_core.utils.llm;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PromptUtils {

	public static String removeSpecialChars(String originalString) {

		Map<String, String> conversionMap = new HashMap<>();
		conversionMap.put("⩥", "|>>");
		conversionMap.put("▷", "|>");
//		conversionMap.put("∪", "\\/");
//		conversionMap.put("∩", "/\\");
		conversionMap.put("↦", "|->");
		conversionMap.put("→", "-->");
//		conversionMap.put("⊄", "/<<:");
//		conversionMap.put("⊈", "/<:");
//		conversionMap.put("∉", "/:");
		conversionMap.put("⇔", "<=>");
		conversionMap.put("⇒", "=>");
//		conversionMap.put("∧", "&");
//		conversionMap.put("∀", "!");
//		conversionMap.put("∃", "#");
//		conversionMap.put("≠", "/=");
//		conversionMap.put("≤", "<=");
//		conversionMap.put("≥", ">=");
//		conversionMap.put("⊂", "<<:");
//		conversionMap.put("⊆", "<:");
		conversionMap.put("↔", "<->");
		conversionMap.put("⤖", ">->>");
		conversionMap.put("⇸", "+->");
		conversionMap.put("⤔", ">+>");
		conversionMap.put("↣", ">->");
		conversionMap.put("⤀", "+->>");
		conversionMap.put("↠", "-->>");
		conversionMap.put("∅", "{}");
		conversionMap.put("∖", "\\");
		conversionMap.put("⊗", "><");
		conversionMap.put("∥", "||");
		conversionMap.put("∼", "~");
		conversionMap.put("⩤", "<<|");
		conversionMap.put("◁", "<|");
//		conversionMap.put("λ", "%");
		conversionMap.put("‥", "..");
		conversionMap.put("·", ".");
		conversionMap.put("≔", ":=");
//		conversionMap.put(":∈", "::");
//		conversionMap.put(":∣", ":|");
		conversionMap.put("∣", "|");
//		conversionMap.put("ℕ", "NAT");
		conversionMap.put("ℙ", "POW");
//		conversionMap.put("ℤ", "INT");
//		conversionMap.put("⋂", "inter");
//		conversionMap.put("⋃", "union");
//		conversionMap.put("∨", "or");
//		conversionMap.put("¬", "not");
//		conversionMap.put("⊤", "true");
//		conversionMap.put("⊥", "false");

		for (Entry<String, String> entry : conversionMap.entrySet()) {
			originalString = originalString.replace(entry.getKey(), entry.getValue());
		}

		return originalString;
	}

}
