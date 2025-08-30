package eventb_agent_core.proof;

public enum ProofFixingStrategies {

	addAbstractExpression, addHypothesesToContext, applyProofTactic, caseDistinction, caseDistinctionBySplittingEvent,
	instantiation, strengthenGuard, strengthenInvariant, // strategies with parameters
	cardinalityDefinition, disjunctionToImplication, doubleImplication, equalCardinality, equivalence, finiteDefinition,
	functionalImageDefinition, implicationAnd, implicationOr, inclusionSetMinus, removeInclusion, removeMembership,
	removeNegation, setEqual, setMinus, strictInclusion, relationOverwriteDefinition, //
	applySMT, applyLasoo, //
	others

}
