package eventb_agent_ui.handlers;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.json.JSONObject;

import eventb_agent_core.llm.LLMRequestSender;

/**
 * This handler is responsible for calling LLMs to generate an Event-B Machine.
 */
public class CreateMachineHandler extends AbstractHandler {

	private LLMRequestSender llmRequestSender = new LLMRequestSender();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String prompt = "Generate an Event-B Machine.";
		String response;
		try {
			response = llmRequestSender.sendRequest(prompt);
			JSONObject obj = new JSONObject(response);
			String answer = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
			System.out.println(answer);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
