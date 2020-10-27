package org.argeo.cms.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;

import org.eclipse.rap.rwt.RWT;

/** Base class for {@link CmsApp}s. */
public abstract class AbstractCmsApp implements CmsApp {
	private Map<String, CmsTheme> themes = Collections.synchronizedMap(new HashMap<>());

	private List<CmsAppListener> cmsAppListeners = new ArrayList<>();

	private Repository repository;

	protected abstract String getThemeId(String uiName);

	@Override
	public CmsTheme getTheme(String uiName) {
		String themeId = getThemeId(uiName);
		if (themeId == null)
			return null;
		return themes.get(themeId);
	}

	protected boolean allThemesAvailable() {
		boolean themeMissing = false;
		uiNames: for (String uiName : getUiNames()) {
			String themeId = getThemeId(uiName);
			if (RWT.DEFAULT_THEME_ID.equals(themeId))
				continue uiNames;
			if (!themes.containsKey(themeId)) {
				themeMissing = true;
				break uiNames;
			}
		}
		return !themeMissing;
	}

	public void addTheme(CmsTheme theme, Map<String, String> properties) {
		themes.put(theme.getThemeId(), theme);
		if (allThemesAvailable())
			for (CmsAppListener listener : cmsAppListeners)
				listener.themingUpdated();
	}

	public void removeTheme(CmsTheme theme, Map<String, String> properties) {
		themes.remove(theme.getThemeId());
	}

	@Override
	public void addCmsAppListener(CmsAppListener listener) {
		cmsAppListeners.add(listener);
		if (allThemesAvailable())
			listener.themingUpdated();
	}

	@Override
	public void removeCmsAppListener(CmsAppListener listener) {
		cmsAppListeners.remove(listener);
	}

	protected Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
