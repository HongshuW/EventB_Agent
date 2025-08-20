package eventb_agent_core.llminteractor;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rodinp.core.RodinDBException;

import de.prob.core.Animator;
import de.prob.core.command.GetFullTraceCommand;
import de.prob.core.command.GetFullTraceCommand.TraceResult;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.core.command.ModelCheckingCommand;
import de.prob.core.command.ModelCheckingCommand.Result;
import de.prob.core.command.ModelCheckingResult;
import de.prob.exceptions.ProBException;
import eventb_agent_core.evaluation.EvaluationManager;
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

		// get parameters for model checking
		JSONArray modelCheckingParams = getModelCheckingParameters(modelJSON);
		List<String> opts = new ArrayList<>();
		for (int i = 0; i < modelCheckingParams.length(); i++) {
			String paramVal = modelCheckingParams.getString(i);
			String param = paramVal.split("=")[0];
			String val = paramVal.split("=")[1];
			opts.add("-p");
			opts.add(param);
			opts.add(val);
		}

		StringBuilder resultString = new StringBuilder();

		// load animator and model
		Animator animator = Animator.getAnimator();
		try {
			LoadEventBModelCommand.load(animator, machineRoot);
		} catch (ProBException e) {
			EvaluationManager.setErrorToLatestAction(e.getMessage());
			resultString.append("\nError: ");
			resultString.append(e.getMessage().replace("\n", "\\n"));
			return fixBasedOnModelCheckingResults(modelJSON, resultString.toString());
		}

		ModelCheckingResult<Result> modelCheckingResult = null;
		try {
			modelCheckingResult = ModelCheckingCommand.modelcheck(animator, 10000, opts);
		} catch (ProBException e) {
			EvaluationManager.setErrorToLatestAction(e.getMessage());
			resultString.append("\nError: ");
			resultString.append(e.getMessage().replace("\n", "\\n"));
			return fixBasedOnModelCheckingResults(modelJSON, resultString.toString());
		}

		if (modelCheckingResult != null) {
			Result result = modelCheckingResult.getResult();
			EvaluationManager.setErrorToLatestAction(result.name());
			if (!result.equals(Result.ok) && !result.equals(Result.ok_not_all_nodes_considered)) {
				resultString.append(result.name());
				resultString.append("\nWorked: ");
				resultString.append(String.valueOf(modelCheckingResult.getWorked()));
				resultString.append("\nNumber of states: ");
				resultString.append(String.valueOf(modelCheckingResult.getNumStates()));
				resultString.append("\nNumber of transitions: ");
				resultString.append(String.valueOf(modelCheckingResult.getNumTransitions()));
				TraceResult traceResult;
				try {
					traceResult = GetFullTraceCommand.getTrace(animator);
					resultString.append("\nTrace: ");
					List<String> operations = traceResult.getOperations();
					for (String op : operations) {
						resultString.append(op + ",");
					}
				} catch (ProBException e) {
					EvaluationManager.setErrorToLatestAction(e.getMessage());
					resultString.append("\nError: ");
					resultString.append(e.getMessage().replace("\n", "\\n"));
				}
				return fixBasedOnModelCheckingResults(modelJSON, resultString.toString());
			}
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
