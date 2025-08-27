package eventb_agent_core.llminteractor;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.json.JSONObject;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.RodinUtils;
import eventb_agent_core.utils.llm.ParserUtils;

/**
 * This class interacts with the llm to create an Event-B model.
 */
public class ModelCreator extends AbstractLLMInteractor {

	public ModelCreator(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	/**
	 * Synthesize the Event-B model based on the refinement step. Return the created
	 * model in JSON.
	 * 
	 * @param refinementStep
	 * @return the created model in JSON.
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws ReachMaxAttemptException
	 */
	public JSONObject synthesizeModel(RefinementStep refinementStep)
			throws InvocationTargetException, InterruptedException, ReachMaxAttemptException {
		String refinementID = String.valueOf(refinementStep.getRefinementNo());
		String systemDesc = refinementStep.getModelDesc();
		String systemReqs = refinementStep.getSysReqString();
		JSONObject response = getLLMResponse(new String[] { refinementID, systemDesc, systemReqs },
				LLMRequestTypes.SYNTHESIS);

		return response;
	}

	/**
	 * Refine the given Event-B model. Return the refined model in JSON.
	 * 
	 * @param projectName
	 * @param fileNames
	 * @param previousSysDesc
	 * @param refinementStep
	 * @return the refined model in JSON.
	 * @throws CoreException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws ReachMaxAttemptException
	 */
	public JSONObject refineModel(final String projectName, String[] fileNames, String previousSysDesc,
			String previousSysReq, RefinementStep refinementStep)
			throws CoreException, InvocationTargetException, InterruptedException, ReachMaxAttemptException {

		// retrieve model in JSON form
		IRodinProject rodinProject = RodinUtils.getRodinProject(projectName);
		IRodinFile ctxFile = rodinProject.getRodinFile(fileNames[0]);
		IRodinFile mchFile = rodinProject.getRodinFile(fileNames[1]);
		IContextRoot contextRoot = (IContextRoot) ctxFile.getRoot();
		IMachineRoot machineRoot = (IMachineRoot) mchFile.getRoot();
		String modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);

		String refinementID = String.valueOf(refinementStep.getRefinementNo());
		String refineSysDesc = refinementStep.getModelDesc();
		String refineSysReq = refinementStep.getSysReqString();
		String gluingInvs = refinementStep.getGluingInvariantsString();

		JSONObject response = getLLMResponse(new String[] { refinementID, previousSysDesc, previousSysReq,
				ParserUtils.reverseLex(modelJSON), refineSysDesc, refineSysReq, gluingInvs },
				LLMRequestTypes.REFINE_MODEL);

		return response;
	}

}
