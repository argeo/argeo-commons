package org.argeo.cms.ui.forms;

/** Syles used */
public enum FormStyle {
	// Main
	form, title,
	// main part
	header, headerBtn, headerCombo, section, sectionHeader,
	// Property fields
	propertyLabel, propertyText, propertyMessage, errorMessage,
	// Date
	popupCalendar,
	// Buttons
	starred, unstarred, starOverlay, editOverlay, deleteOverlay, updateOverlay, deleteOverlaySmall, calendar, delete,
	// Contacts
	email, address, phone, website,
	// Social Media
	facebook, twitter, linkedIn, instagram;

	public String style() {
		return form.name() + '_' + name();
	}

	// TODO clean button style management
	public final static String BUTTON_SUFFIX = "_btn";
}
