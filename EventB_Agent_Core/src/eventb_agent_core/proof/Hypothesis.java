package eventb_agent_core.proof;

import eventb_agent_core.utils.llm.ParserUtils;

/**
 * This is a corresponding class for hypothesis object in "add hypothesis"
 * schema.
 */
public class Hypothesis {

	private String label;
	private String predicate;
	private String[] instantiations;

	public Hypothesis(String label, String predicate) {
		this.label = label;
		this.predicate = predicate;
		this.instantiations = null;
	}

	public Hypothesis(String label, String predicate, String... instantiations) {
		this.label = label;
		this.predicate = predicate;
		this.instantiations = instantiations;
		lexInstantiations();
	}

	public String getLabel() {
		return label;
	}

	public String getPredicate() {
		return predicate;
	}

	public String[] getInstantiations() {
		return instantiations;
	}
	
	private void lexInstantiations() {
		if (instantiations == null) {
			return;
		}
		for (int i = 0; i < instantiations.length; i++) {
			instantiations[i] = ParserUtils.lex(instantiations[i]);
		}
	}

}
