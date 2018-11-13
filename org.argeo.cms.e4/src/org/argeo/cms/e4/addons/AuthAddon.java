package org.argeo.cms.e4.addons;

import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.argeo.cms.auth.CurrentUser;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;

public class AuthAddon {
	public final static String AUTH = "auth.";

	@PostConstruct
	void init(MApplication application) {
		Iterator<MWindow> windows = application.getChildren().iterator();
		windows: while (windows.hasNext()) {
			MWindow window = windows.next();
			// main window
			boolean windowVisible = process(window);
			if (!windowVisible) {
				windows.remove();
				continue windows;
			}
			// trim bars
			if (window instanceof MTrimmedWindow) {
				Iterator<MTrimBar> trimBars = ((MTrimmedWindow) window).getTrimBars().iterator();
				while (trimBars.hasNext()) {
					MTrimBar trimBar = trimBars.next();
					if (!process(trimBar)) {
						trimBars.remove();
					}
				}
			}
		}
	}

	protected boolean process(MUIElement element) {
		for (String tag : element.getTags()) {
			if (tag.startsWith(AUTH)) {
				String role = tag.substring(AUTH.length(), tag.length());
				if (!CurrentUser.isInRole(role)) {
					element.setVisible(false);
					element.setToBeRendered(false);
					return false;
				}
			}
		}

		// children
		if (element instanceof MElementContainer) {
			@SuppressWarnings("unchecked")
			MElementContainer<? extends MUIElement> container = (MElementContainer<? extends MUIElement>) element;
			Iterator<? extends MUIElement> children = container.getChildren().iterator();
			while (children.hasNext()) {
				MUIElement child = children.next();
				boolean visible = process(child);
				if (!visible)
					children.remove();
			}

			for (Object child : container.getChildren()) {
				if (child instanceof MUIElement) {
					boolean visible = process((MUIElement) child);
					if (!visible)
						container.getChildren().remove(child);
				}
			}
		}

		return true;
	}
}
