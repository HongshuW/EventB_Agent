package eventb_agent_ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * This handler is responsible for calling LLMs to generate an Event-B Machine.
 */
public class CreateMachineHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Hello World.");
		return null;
	}

}
