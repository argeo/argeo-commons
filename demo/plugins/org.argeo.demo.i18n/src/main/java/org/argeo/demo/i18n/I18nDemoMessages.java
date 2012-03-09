/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	// Errors & warnings
	public String OpenDialog_Title;
	public String OpenDialog_Message;

	// Commands

	// Editor
	public String SimpleMultitabEditor_MultiSectionPageTitle;
	public String SimpleMultitabEditor_SimplePageTitle;

	// Pages
	public String MultiSectionPage_DescriptionSectionTitle;
	public String MultiSectionPage_DescriptionSectionTxt;
	public String MultiSectionPage_DetailsSectionTitle;
	public String MultiSectionPage_PopupTitle;
	public String MultiSectionPage_PopupText;
	public String SimplePage_DescriptionTxt;

	// Buttons
	public String MultiSectionPage_Btn1Lbl;
	public String MultiSectionPage_Btn2Lbl;
	public String MultiSectionPage_Btn3Lbl;

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
