package org.argeo.cms.e4.handlers;

import java.security.AccessController;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;

import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.e4.core.services.nls.ILocaleChangeService;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.ElementMatcher;
import org.eclipse.swt.SWT;

/** Integrate workbench with the locale provided at log in. */
public class LocaleAddon {
	private final static String STYLE_OVERRIDE = "styleOverride";

	// Right to left languages
	private final static String ARABIC = "ar";
	private final static String HEBREW = "he";

	@PostConstruct
	public void init(ILocaleChangeService localeChangeService, EModelService modelService, MApplication application) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Set<Locale> locales = subject.getPublicCredentials(Locale.class);
		if (!locales.isEmpty()) {
			Locale locale = locales.iterator().next();
			localeChangeService.changeApplicationLocale(locale);
			UiContext.setLocale(locale);

			if (locale.getLanguage().equals(ARABIC) || locale.getLanguage().equals(HEBREW)) {
				List<MWindow> windows = modelService.findElements(application, MWindow.class, EModelService.ANYWHERE,
						new ElementMatcher(null, null, (String) null));
				for (MWindow window : windows) {
					String currentStyle = window.getPersistedState().get(STYLE_OVERRIDE);
					int style = 0;
					if (currentStyle != null) {
						style = Integer.parseInt(currentStyle);
					}
					style = style | SWT.RIGHT_TO_LEFT;
					window.getPersistedState().put(STYLE_OVERRIDE, Integer.toString(style));
				}
			}
		}
	}
}
