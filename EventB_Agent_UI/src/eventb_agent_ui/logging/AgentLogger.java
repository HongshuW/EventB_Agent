package eventb_agent_ui.logging;

import java.io.IOException;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class AgentLogger {

	private static MessageConsole findConsole(String name) {
		IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
		for (IConsole c : mgr.getConsoles())
			if (name.equals(c.getName()))
				return (MessageConsole) c;

		MessageConsole mc = new MessageConsole(name, null);
		mgr.addConsoles(new IConsole[] { mc });
		return mc;
	}

	public static void log(String msg) throws IOException {
		MessageConsole c = findConsole("Event-B Agent");
		try (MessageConsoleStream out = c.newMessageStream()) {
			out.println(msg);
		}
	}

}
