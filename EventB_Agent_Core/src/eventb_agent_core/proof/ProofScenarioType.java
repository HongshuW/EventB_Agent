package eventb_agent_core.proof;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import eventb_agent_core.utils.FileUtils;

public enum ProofScenarioType {

	WD, INV, QUANT_INV, ADDED_HYP, CARD_WD, TRIVIAL_INV;

	public String getRules() {

		StringBuilder rules = new StringBuilder();

		int ruleID = 0;
		String previousRule = "";
		String[] files = getPromptFileNamesFromType();
		for (String file : files) {
			previousRule = "";
			Path path = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "prompts", file);
			List<Map<String, Object>> jsonArray = FileUtils.readOrderedJSONArray(path);
			for (Map<String, Object> rule : jsonArray) {
				if (!rule.get("id").equals(previousRule)) {
					previousRule = (String) rule.get("id");
					ruleID++;
				}
				rules.append(String.valueOf(ruleID) + rule.get("sub_id") + ". ");
				rules.append(rule.get("rule") + "\n");
			}
		}

		return rules.toString();
	}

	private String[] getPromptFileNamesFromType() {
		switch (this) {
		case WD:
			return new String[] { "general_rules.json", "wd_rules.json" };
		case INV:
			return new String[] { "inv_rules.json", "general_rules.json" };
		case QUANT_INV:
			return new String[] { "general_rules.json", "quantified_invariant.json" };
		case ADDED_HYP:
			return new String[] { "added_hyp_rules.json" };
		case CARD_WD:
			return new String[] { "card_wd_rules.json" };
		case TRIVIAL_INV:
			return new String[] { "general_rules.json" };
		default:
			return new String[] { "general_rules.json" };
		}
	}

}
