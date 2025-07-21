package eventb_agent_ui.popups;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eventb_agent_core.proof.FixStrategy;

public class ProofStrategySelectionDialog extends Dialog {

	private Combo strategyCombo;
	private FixStrategy fixStrategy;

	public ProofStrategySelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Label label = new Label(container, SWT.NONE);
		label.setText("Choose a fixing strategy:");

		strategyCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		strategyCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		for (FixStrategy strategy : FixStrategy.values()) {
			strategyCombo.add(strategy.toString());
		}

		strategyCombo.select(0);

		return container;
	}

	@Override
	protected void okPressed() {
		int selectedIndex = strategyCombo.getSelectionIndex();
		if (selectedIndex >= 0) {
			fixStrategy = FixStrategy.values()[selectedIndex];
		}
		super.okPressed();
	}

	public FixStrategy getSelectedStrategy() {
		return fixStrategy;
	}

}