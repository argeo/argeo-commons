package org.argeo.cms.swt;

import java.util.HashMap;
import java.util.Map;

import org.argeo.api.cms.ux.CmsIcon;
import org.argeo.api.cms.ux.CmsStyle;
import org.argeo.api.cms.ux.CmsTheme;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/** SWT utilities. */
public class CmsSwtUtils {
	/*
	 * THEME AND VIEW
	 */

	public static CmsSwtTheme getCmsTheme(Composite parent) {
		CmsSwtTheme theme = (CmsSwtTheme) parent.getData(CmsTheme.class.getName());
		if (theme == null) {
			// find parent shell
			Shell topShell = parent.getShell();
			while (topShell.getParent() != null)
				topShell = (Shell) topShell.getParent();
			theme = (CmsSwtTheme) topShell.getData(CmsTheme.class.getName());
			parent.setData(CmsTheme.class.getName(), theme);
		}
		return theme;
	}

	public static void registerCmsTheme(Shell shell, CmsTheme theme) {
		// find parent shell
		Shell topShell = shell;
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		// check if already set
		if (topShell.getData(CmsTheme.class.getName()) != null) {
			CmsTheme registeredTheme = (CmsTheme) topShell.getData(CmsTheme.class.getName());
			throw new IllegalArgumentException(
					"Theme " + registeredTheme.getThemeId() + " already registered in this shell");
		}
		topShell.setData(CmsTheme.class.getName(), theme);
	}

	public static CmsView getCmsView(Control parent) {
		if (parent.isDisposed())
			return null;
		// find parent shell
		Shell topShell = parent.getShell();
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		return (CmsView) topShell.getData(CmsView.class.getName());
	}

	public static void registerCmsView(Shell shell, CmsView view) {
		// find parent shell
		Shell topShell = shell;
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		// check if already set
		if (topShell.getData(CmsView.class.getName()) != null) {
			CmsView registeredView = (CmsView) topShell.getData(CmsView.class.getName());
			throw new IllegalArgumentException("Cms view " + registeredView + " already registered in this shell");
		}
		shell.setData(CmsView.class.getName(), view);
	}

	/*
	 * EVENTS
	 */

	/** Sends an event via {@link CmsView#sendEvent(String, Map)}. */
	public static void sendEventOnSelect(Control control, String topic, Map<String, Object> properties) {
		SelectionListener listener = (Selected) (e) -> {
			getCmsView(control.getParent()).sendEvent(topic, properties);
		};
		if (control instanceof Button) {
			((Button) control).addSelectionListener(listener);
		} else
			throw new UnsupportedOperationException("Control type " + control.getClass() + " is not supported.");
	}

	/**
	 * Convenience method to sends an event via
	 * {@link CmsView#sendEvent(String, Map)}.
	 */
	public static void sendEventOnSelect(Control control, String topic, String key, Object value) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(key, value);
		sendEventOnSelect(control, topic, properties);
	}

	/*
	 * ICONS
	 */
	/** Get a small icon from this theme. */
	public static Image getSmallIcon(CmsTheme theme, CmsIcon icon) {
		return ((CmsSwtTheme) theme).getSmallIcon(icon);
	}

	/** Get a big icon from this theme. */
	public static Image getBigIcon(CmsTheme theme, CmsIcon icon) {
		return ((CmsSwtTheme) theme).getBigIcon(icon);
	}

	/*
	 * LAYOUT INDEPENDENT
	 */
	/** Takes the most space possible, depending on parent layout. */
	public static void fill(Control control) {
		Layout parentLayout = control.getParent().getLayout();
		if (parentLayout == null)
			throw new IllegalStateException("Parent layout is not set");
		if (parentLayout instanceof GridLayout) {
			control.setLayoutData(fillAll());
		} else if (parentLayout instanceof FormLayout) {
			control.setLayoutData(coverAll());
		} else {
			throw new IllegalArgumentException("Unsupported parent layout  " + parentLayout.getClass().getName());
		}
	}

	/*
	 * GRID LAYOUT
	 */
	/** A {@link GridLayout} without any spacing and one column. */
	public static GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	/**
	 * A {@link GridLayout} without any spacing and multiple columns of unequal
	 * width.
	 */
	public static GridLayout noSpaceGridLayout(int columns) {
		return noSpaceGridLayout(new GridLayout(columns, false));
	}

	/** @return the same layout, with spaces removed. */
	public static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static GridData fillAll() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}

	public static GridData fillWidth() {
		return grabWidth(SWT.FILL, SWT.FILL);
	}

	public static GridData grabWidth(int horizontalAlignment, int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, true, false);
	}

	public static GridData fillHeight() {
		return grabHeight(SWT.FILL, SWT.FILL);
	}

	public static GridData grabHeight(int horizontalAlignment, int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, false, true);
	}

	/*
	 * ROW LAYOUT
	 */
	/** @return the same layout, with margins removed. */
	public static RowLayout noMarginsRowLayout(RowLayout rowLayout) {
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		rowLayout.marginLeft = 0;
		rowLayout.marginRight = 0;
		return rowLayout;
	}

	public static RowLayout noMarginsRowLayout(int type) {
		return noMarginsRowLayout(new RowLayout(type));
	}

	public static RowData rowData16px() {
		return new RowData(16, 16);
	}

	/*
	 * FORM LAYOUT
	 */
	public static FormData coverAll() {
		FormData fdLabel = new FormData();
		fdLabel.top = new FormAttachment(0, 0);
		fdLabel.left = new FormAttachment(0, 0);
		fdLabel.right = new FormAttachment(100, 0);
		fdLabel.bottom = new FormAttachment(100, 0);
		return fdLabel;
	}

	/*
	 * STYLING
	 */

	/** Style widget */
	public static <T extends Widget> T style(T widget, String style) {
		if (style == null || widget.isDisposed())
			return widget;// does nothing
		EclipseUiSpecificUtils.setStyleData(widget, style);
		if (widget instanceof Control) {
			CmsView cmsView = getCmsView((Control) widget);
			if (cmsView != null)
				cmsView.applyStyles(widget);
		}
		return widget;
	}

	/** Style widget */
	public static <T extends Widget> T style(T widget, CmsStyle style) {
		return style(widget, style.style());
	}

	/** Enable markups on widget */
	public static <T extends Widget> T markup(T widget) {
		EclipseUiSpecificUtils.setMarkupData(widget);
		return widget;
	}

	/** Disable markup validation. */
	public static <T extends Widget> T disableMarkupValidation(T widget) {
		EclipseUiSpecificUtils.setMarkupValidationDisabledData(widget);
		return widget;
	}

	/**
	 * Apply markup and set text on {@link Label}, {@link Button}, {@link Text}.
	 * 
	 * @param widget the widget to style and to use in order to display text
	 * @param txt    the object to display via its <code>toString()</code> method.
	 *               This argument should not be null, but if it is null and
	 *               assertions are disabled "<null>" is displayed instead; if
	 *               assertions are enabled the call will fail.
	 * 
	 * @see markup
	 */
	public static <T extends Widget> T text(T widget, Object txt) {
		assert txt != null;
		String str = txt != null ? txt.toString() : "<null>";
		markup(widget);
		if (widget instanceof Label)
			((Label) widget).setText(str);
		else if (widget instanceof Button)
			((Button) widget).setText(str);
		else if (widget instanceof Text)
			((Text) widget).setText(str);
		else
			throw new IllegalArgumentException("Unsupported widget type " + widget.getClass());
		return widget;
	}

	/** A {@link Label} with markup activated. */
	public static Label lbl(Composite parent, Object txt) {
		return text(new Label(parent, SWT.NONE), txt);
	}

	/** A read-only {@link Text} whose content can be copy/pasted. */
	public static Text txt(Composite parent, Object txt) {
		return text(new Text(parent, SWT.NONE), txt);
	}

	/** Dispose all children of a Composite */
	public static void clear(Composite composite) {
		if (composite.isDisposed())
			return;
		for (Control child : composite.getChildren())
			child.dispose();
	}

	/** Singleton. */
	private CmsSwtUtils() {
	}

}
