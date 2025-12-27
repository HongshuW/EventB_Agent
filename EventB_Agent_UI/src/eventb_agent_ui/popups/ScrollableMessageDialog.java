package eventb_agent_ui.popups;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ScrollableMessageDialog extends MessageDialog {

	private final String messageContent;

	public ScrollableMessageDialog(Shell parentShell, String title, String messageContent) {
		// message = null so our custom area is the content
		super(parentShell, title, null, null, NONE, new String[] { "OK" }, 0);
		this.messageContent = messageContent;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		// Use GridLayout so GridData width/height hints actually apply
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Wrap long lines; vertical scroll appears as needed
		// (Remove SWT.WRAP and add SWT.H_SCROLL if you prefer horizontal scrolling)
		Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		text.setText(messageContent);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return container;
	}

	@Override
	protected boolean isResizable() {
		return true; // allow user resizing
	}

	@Override
	protected Point getInitialSize() {
		// Enforce a sensible default size (dialog can still be resized)
		return new Point(700, 500);
	}

}
