package eventb_agent_core.llm.schemas;

public class SchemaKeys {

	/* context keys */

	public static final String CONTEXT_OBJ_KEY = "context";
	public static final String CONTEXT = "CONTEXT";
	public static final String EXTENDS = "EXTENDS";
	public static final String SETS = "SETS";
	public static final String CONSTANTS = "CONSTANTS";
	public static final String AXIOMS = "AXIOMS";

	/* machine keys */

	public static final String MACHINE_OBJ_KEY = "machine";
	public static final String MACHINE = "MACHINE";
	public static final String REFINES = "REFINES";
	public static final String SEES = "SEES";
	public static final String VARIABLES = "VARIABLES";
	public static final String INVARIANTS = "INVARIANTS";
	public static final String VARIANTS = "VARIANTS";
	public static final String EVENTS = "EVENTS";

	/* internal keys for labeled predicates */

	public static final String LABEL = "label_name";
	public static final String PRED = "predicate";
	public static final String ASSIGN = "assignment";
	public static final String EXPR = "expression";
	public static final String CMT = "comments";

	/* internal keys for events */

	public static final String EVENT_NAME = "event_name";
	// REFINES defined above
	public static final String ANY = "ANY";
	public static final String WHERE = "WHERE";
	public static final String WITH = "WITH";
	public static final String THEN = "THEN";

	/* keys for refinement strategy */
	public static final String REF_STRATEGY = "refinement_strategy";
	public static final String REF_NO = "refinement_no";
	public static final String REQUIREMENT_IDS = "requirement_ids";
	public static final String MODEL_DESC = "model_description";
	public static final String GLUING_INVS = "gluing_invariants";
	public static final String SYMBOLS = "constants_and_variables";

	/* keys for proof fixing */
	public static final String PROOF_TACTIC = "proof_tactic";
	public static final String NODE_ID = "node_id";
	public static final String PRED_ID = "predicate_id";
	public static final String EXPLANATION = "explanation";
	public static final String MODIFICATION = "modification";
	public static final String HYP = "hypothesis";
	public static final String INV = "invariant";
	public static final String GRD = "guard";
	public static final String ACT = "action";
	public static final String INSTANTIATIONS = "instantiations";
	public static final String AXM_LABEL = "axiom_label";

	/* keys for model checking */
	public static final String MODEL_CHECKING_PARAMS = "parameters";

}
