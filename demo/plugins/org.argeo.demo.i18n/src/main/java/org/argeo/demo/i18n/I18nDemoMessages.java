package org.argeo.demo.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Centralizes all internationalized labels accross current application.
 * Supports both RAP and RCP thanks to the NLSHelper. NOTE that the
 * corresponding NLSHelperImpl must be available.
 * 
 * thanks to {@link http
 * ://eclipsesource.com/en/info/rcp-rap-single-sourcing-guideline/}
 */
public class I18nDemoMessages extends NLS {

	private static final String BUNDLENAME = I18nDemoPlugin.ID + ".messages"; // $NON-NLS−1$

	public String SimpleMultitabEditor_MultiSectionPageTitle;
	public String SimpleMultitabEditor_SimplePageTitle;
	public String MultiSelectionPage_DescriptionSectionTitle;
	public String MultiSelectionPage_DetailsSectionTitle;
	public String testLbl;

	// Code that enable handling of concurrent multi sessions locales
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLENAME, I18nDemoMessages.class);
	}

	private I18nDemoMessages() {
	}

	public static I18nDemoMessages get() {
		return (I18nDemoMessages) NLSHelper.getMessages(BUNDLENAME,
				I18nDemoMessages.class);
	}
}
