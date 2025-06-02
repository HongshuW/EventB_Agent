package eventb_agent_ui;

import static fr.systerel.explorer.ExplorerPlugin.NAVIGATOR_ID;
import static org.eventb.ui.EventBUIPlugin.RODIN_PROBLEM_VIEW_ID;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This class contains the perspective for the Event-B Agent. Update the
 * {@code defineLayout} method to adjust the views.
 */
public class EventBAgentPerspective implements IPerspectiveFactory {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui
	 * .IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		defineLayout(layout);
	}

	/**
	 * Define the initial (default) layout for this perspective.
	 * <p>
	 * 
	 * @param layout the page layout
	 */
	public void defineLayout(IPageLayout layout) {
		// Editors are placed for free.
		String editorArea = layout.getEditorArea();

		// Place the project explorer to left of editor area.
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
		left.addView(NAVIGATOR_ID);

		// Place the Problems / Task to the bottom of the editor area.
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, editorArea);
		bottom.addView(RODIN_PROBLEM_VIEW_ID);

		// Place the outline to right of editor area.
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.8f, editorArea);
		right.addView(IPageLayout.ID_OUTLINE);
	}

}
