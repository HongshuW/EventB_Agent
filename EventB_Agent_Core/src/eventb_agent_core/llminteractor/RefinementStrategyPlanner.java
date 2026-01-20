package eventb_agent_core.llminteractor;

import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;

/**
 * This class interacts with the LLM to plan out the refinement strategy.
 */
public class RefinementStrategyPlanner extends AbstractLLMInteractor {

	public RefinementStrategyPlanner(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	public JSONArray getRefinementSteps(String sysDesc) throws ReachMaxAttemptException {
		JSONObject response = getLLMResponse(new String[] { sysDesc }, LLMRequestTypes.REFINE_STRATEGY);
		return llmResponseParser.getRefinementStepsJSONArray(response);
	}
	
	public JSONArray getSingleRefinementStep(JSONArray reqIDs) throws ReachMaxAttemptException {
//		JSONObject response = getLLMResponse(new String[] { sysDesc + "\n\nReturn one refinement step with all the requirements." }, LLMRequestTypes.REFINE_STRATEGY);
//		return llmResponseParser.getRefinementStepsJSONArray(response);
		
		JSONArray responseArray = new JSONArray();
		JSONObject response = new JSONObject();
		response.put("refinement_no", 1);
		response.put("gluing_invariants", new JSONArray());
		response.put("model_description", "");
		response.put("requirement_ids", reqIDs);
		responseArray.put(response);
		return responseArray;
	}
}
