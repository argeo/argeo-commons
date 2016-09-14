package org.argeo.cms.text;

/** Styles references in the CSS. */
public interface TextStyles {
	/** The whole page area */
	public final static String TEXT_AREA = "text_area";
	/** Area providing controls for editing text */
	public final static String TEXT_EDITOR_HEADER = "text_editor_header";
	/** The styled composite for editing the text */
	public final static String TEXT_STYLED_COMPOSITE = "text_styled_composite";
	/** A section */
	public final static String TEXT_SECTION = "text_section";
	/** A paragraph */
	public final static String TEXT_PARAGRAPH = "text_paragraph";
	/** An image */
	public final static String TEXT_IMG = "text_img";
	/** The dialog to edit styled paragraph */
	public final static String TEXT_STYLED_TOOLS_DIALOG = "text_styled_tools_dialog";

	/*
	 * DEFAULT TEXT STYLES
	 */
	/** Default style for text body */
	public final static String TEXT_DEFAULT = "text_default";
	/** Fixed-width, typically code */
	public final static String TEXT_PRE = "text_pre";
	/** Quote */
	public final static String TEXT_QUOTE = "text_quote";
	/** Title */
	public final static String TEXT_TITLE = "text_title";
	/** Header (to be dynamically completed with the depth, e.g. text_h1) */
	public final static String TEXT_H = "text_h";

	/** Default style for images */
	public final static String TEXT_IMAGE = "text_image";

}
