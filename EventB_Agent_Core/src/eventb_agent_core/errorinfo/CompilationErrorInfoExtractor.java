package eventb_agent_core.errorinfo;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IAction;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.RodinCore;

/**
 * This class extracts information from compilation error.
 */
public class CompilationErrorInfoExtractor {

	private String handle;

	private String filePath;
	private String fileType;
	private String componentName;
	private IInternalElementType<? extends IInternalElement> firstElementType;
	private IInternalElementType<? extends IInternalElement> secondElementType;
	private String firstElementID;
	private String secondElementID;

	public CompilationErrorInfoExtractor(String handle) {
		this.handle = handle;
		extractInfoFromHandle(handle);
	}

	private void extractInfoFromHandle(String handle) {
		// <path to file>|
		// <machineFile OR contextFile>#<machine name OR context name>|
		// <internal element type>#<default id>
		// if element is event : <internal element type>#<default id>
		String[] entries = handle.split("\\|");

		filePath = entries[0];

		String fileTypeAndName = entries[1];
		String[] fileTypeAndNameEntries = fileTypeAndName.split("#");
		fileType = fileTypeAndNameEntries[0];
		componentName = fileTypeAndNameEntries[1];

		String elementTypeAndID = entries[2];
		String[] elementTypeAndIDEntries = elementTypeAndID.split("#");
		firstElementType = RodinCore.getInternalElementType(elementTypeAndIDEntries[0]);
		firstElementID = elementTypeAndIDEntries[1].length() == 1 ? elementTypeAndIDEntries[1]
				: elementTypeAndIDEntries[1].substring(1);

		if (entries.length > 3) {
			elementTypeAndID = entries[3];
			elementTypeAndIDEntries = elementTypeAndID.split("#");
			secondElementType = RodinCore.getInternalElementType(elementTypeAndIDEntries[0]);
			secondElementID = elementTypeAndIDEntries[1].length() == 1 ? elementTypeAndIDEntries[1]
					: elementTypeAndIDEntries[1].substring(1);
		}
	}

	public List<IInternalElement> getErroneousElementsFromMachine(IMachineRoot machineRoot) {
		List<IInternalElement> results = new ArrayList<>();
		if (firstElementType.equals(IInvariant.ELEMENT_TYPE)) {
			results.add(machineRoot.getInvariant(firstElementID));
		} else if (firstElementType.equals(IVariant.ELEMENT_TYPE)) {
			results.add(machineRoot.getVariant(firstElementID));
		} else if (firstElementType.equals(IEvent.ELEMENT_TYPE)) {
			results.add(machineRoot.getEvent(firstElementID));
			IInternalElement secondElement = getNestedInternalElement(machineRoot.getEvent(firstElementID));
			if (secondElement != null) {
				results.add(secondElement);
			}
		}
		return results;
	}

	public IInternalElement getErroneousElementFromContext(IContextRoot contextRoot) {
		// TODO: implement this later
		return null;
	}

	private IInternalElement getNestedInternalElement(IEvent event) {
		if (secondElementType == null) {
			return null;
		}
		if (secondElementType.equals(IRefinesEvent.ELEMENT_TYPE)) {
			return event.getRefinesClause(secondElementID);
		} else if (secondElementType.equals(IParameter.ELEMENT_TYPE)) {
			return event.getParameter(secondElementID);
		} else if (secondElementType.equals(IGuard.ELEMENT_TYPE)) {
			return event.getGuard(secondElementID);
		} else if (secondElementType.equals(IWitness.ELEMENT_TYPE)) {
			return event.getWitness(secondElementID);
		} else if (secondElementType.equals(IAction.ELEMENT_TYPE)) {
			return event.getAction(secondElementID);
		}
		return null;
	}

}
