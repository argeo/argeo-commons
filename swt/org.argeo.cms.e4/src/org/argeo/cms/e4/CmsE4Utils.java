package org.argeo.cms.e4;

import java.util.List;

import org.argeo.cms.swt.CmsException;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/** Static utilities simplifying recurring Eclipse 4 patterns. */
public class CmsE4Utils {
	/** Open an editor based on its id. */
	public static void openEditor(EPartService partService, String editorId, String key, String state) {
		for (MPart part : partService.getParts()) {
			String id = part.getPersistedState().get(key);
			if (id != null && state.equals(id)) {
				partService.showPart(part, PartState.ACTIVATE);
				return;
			}
		}

		// new part
		MPart part = partService.createPart(editorId);
		if (part == null)
			throw new CmsException("No editor found with id " + editorId);
		part.getPersistedState().put(key, state);
		partService.showPart(part, PartState.ACTIVATE);
	}

	/** Dynamically creates an handled menu item from a command ID. */
	public static MHandledMenuItem createHandledMenuItem(EModelService modelService, MApplication app,
			String commandId) {
		MCommand command = findCommand(modelService, app, commandId);
		if (command == null)
			return null;
		MHandledMenuItem handledItem = modelService.createModelElement(MHandledMenuItem.class);
		handledItem.setCommand(command);
		return handledItem;

	}

	/**
	 * Finds a command by ID.
	 * 
	 * @return the {@link MCommand} or <code>null</code> if not found.
	 */
	public static MCommand findCommand(EModelService modelService, MApplication app, String commandId) {
		List<MCommand> cmds = modelService.findElements(app, null, MCommand.class, null);
		for (MCommand cmd : cmds) {
			if (cmd.getElementId().equals(commandId)) {
				return cmd;
			}
		}
		return null;
	}

	/** Dynamically creates a direct menu item from a class. */
	public static MDirectMenuItem createDirectMenuItem(EModelService modelService, Class<?> clss, String label) {
		MDirectMenuItem dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
		dynamicItem.setLabel(label);
		Bundle bundle = FrameworkUtil.getBundle(clss);
		dynamicItem.setContributionURI("bundleclass://" + bundle.getSymbolicName() + "/" + clss.getName());
		return dynamicItem;
	}

	/** Singleton. */
	private CmsE4Utils() {
	}

}
