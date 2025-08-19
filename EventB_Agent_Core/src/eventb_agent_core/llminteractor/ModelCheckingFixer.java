package eventb_agent_core.llminteractor;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rodinp.core.RodinDBException;

import de.prob.core.Animator;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.core.command.ModelCheckingCommand;
import de.prob.core.command.ModelCheckingCommand.Result;
import de.prob.core.command.ModelCheckingResult;
import de.prob.exceptions.ProBException;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.RetrieveModelUtils;

public class ModelCheckingFixer extends AbstractLLMInteractor {

	public ModelCheckingFixer(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	public JSONObject fixModelBasedOnProBResults(IContextRoot contextRoot, IMachineRoot machineRoot)
			throws ReachMaxAttemptException {

		String modelJSON = getModelString(contextRoot, machineRoot);
		JSONArray modelCheckingParams = getModelCheckingParameters(modelJSON);

		Animator animator = Animator.getAnimator();
		try {
			LoadEventBModelCommand.load(animator, machineRoot);
			List<String> opts = new ArrayList<>();
			StringBuilder setupConsts = new StringBuilder("setup_constants(");
			for (int i = 0; i < modelCheckingParams.length(); i++) {
				setupConsts.append(modelCheckingParams.getString(i));
				if (i < modelCheckingParams.length() - 1) {
					setupConsts.append(",");
				}
			}
			setupConsts.append(")");
			opts.add(setupConsts.toString());
			ModelCheckingResult<Result> modelCheckingResult = ModelCheckingCommand.modelcheck(animator, 10000, opts);
			Result result = modelCheckingResult.getResult();
			if (!result.equals(Result.ok) && !result.equals(Result.ok_not_all_nodes_considered)) {
				StringBuilder resultString = new StringBuilder(result.name());
				resultString.append("\nWorked: ");
				resultString.append(String.valueOf(modelCheckingResult.getWorked()));
				resultString.append("\nNumber of states: ");
				resultString.append(String.valueOf(modelCheckingResult.getNumStates()));
				resultString.append("\nNumber of transitions: ");
				resultString.append(String.valueOf(modelCheckingResult.getNumTransitions()));
				return fixBasedOnModelCheckingResults(modelJSON, resultString.toString());
			}
		} catch (ProBException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getModelString(IContextRoot contextRoot, IMachineRoot machineRoot) {
		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		return modelJSON;
	}

	private JSONArray getModelCheckingParameters(String modelJSON) throws ReachMaxAttemptException {
		JSONObject response = getLLMResponseWithTools(new String[] { modelJSON },
				LLMRequestTypes.MODEL_CHECKING_PARAMS);
		return response.getJSONObject(Constants.FUNCTION_ARGS).getJSONArray(SchemaKeys.MODEL_CHECKING_PARAMS);
	}

	private JSONObject fixBasedOnModelCheckingResults(String modelJSON, String results)
			throws ReachMaxAttemptException {
		return getLLMResponse(new String[] { modelJSON, results }, LLMRequestTypes.FIX_MODEL_CHECKING);
	}

}
