package eventb_agent_core.proof;

public enum ProofFixingStrategies {

	addAbstractExpression, addHypothesesAsGuard, addHypothesesToContext, applyProofTactic, caseDistinction,
	instantiation, strengthenGuard, strengthenInvariant, // strategies with parameters
	cardinalityDefinition, disjunctionToImplication, doubleImplication, equalCardinality, equivalence, finiteDefinition,
	functionalImageDefinition, implicationAnd, implicationOr, inclusionSetMinus, removeInclusion, removeMembership,
	removeNegation, setEqual, setMinus, strictInclusion, //
	applySMT, applyLasoo, //
	others

}
