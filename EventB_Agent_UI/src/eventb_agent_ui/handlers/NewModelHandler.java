package eventb_agent_ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eventb_agent_ui.wizards.NewModelWizard;

public class NewModelHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		NewModelWizard wizard = new NewModelWizard();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.open();
		return null;
	}

}
