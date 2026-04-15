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
    private Text text;                // <- keep a reference
    private String editedContent;     // <- store result

    public ScrollableMessageDialog(Shell parentShell, String title, String messageContent) {
        super(parentShell, title, null, null, NONE, new String[] { "OK", "Cancel" }, 0);
        this.messageContent = messageContent;
        this.editedContent = messageContent;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        text.setText(messageContent);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        return container;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == OK && text != null && !text.isDisposed()) {
            editedContent = text.getText();
        }
        super.buttonPressed(buttonId);
    }

    public String getEditedContent() {
        return editedContent;
    }

    @Override
    protected boolean isResizable() { return true; }

    @Override
    protected Point getInitialSize() { return new Point(700, 500); }
}
