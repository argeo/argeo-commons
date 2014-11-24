package org.argeo.cms.internal.text;

import java.util.ArrayList;
import java.util.List;

import org.argeo.cms.CmsNames;
import org.argeo.cms.text.Paragraph;
import org.argeo.cms.text.TextStyles;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.SectionPart;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Dialog to edit a text part. */
class TextContextMenu extends Shell implements CmsNames, TextStyles {
	private final static String[] DEFAULT_TEXT_STYLES = {
			TextStyles.TEXT_DEFAULT, TextStyles.TEXT_PRE, TextStyles.TEXT_QUOTE };

	private final AbstractTextViewer textViewer;

	private static final long serialVersionUID = -3826246895162050331L;
	private List<StyleButton> styleButtons = new ArrayList<TextContextMenu.StyleButton>();

	private Label deleteButton, publishButton, editButton;

	private EditablePart currentTextPart;

	public TextContextMenu(AbstractTextViewer textViewer, Display display) {
		super(display, SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		this.textViewer = textViewer;
		setLayout(new GridLayout());
		setData(RWT.CUSTOM_VARIANT, TEXT_STYLED_TOOLS_DIALOG);

		StyledToolMouseListener stml = new StyledToolMouseListener();
		if (textViewer.getCmsEditable().isEditing()) {
			for (String style : DEFAULT_TEXT_STYLES) {
				StyleButton styleButton = new StyleButton(this, SWT.WRAP);
				styleButton.setData(RWT.CUSTOM_VARIANT, style);
				styleButton.setData(RWT.MARKUP_ENABLED, true);
				styleButton.addMouseListener(stml);
				styleButtons.add(styleButton);
			}

			// Delete
			deleteButton = new Label(this, SWT.NONE);
			deleteButton.setText("Delete");
			deleteButton.addMouseListener(stml);

			// Publish
			publishButton = new Label(this, SWT.NONE);
			publishButton.setText("Publish");
			publishButton.addMouseListener(stml);
		} else if (textViewer.getCmsEditable().canEdit()) {
			// Edit
			editButton = new Label(this, SWT.NONE);
			editButton.setText("Edit");
			editButton.addMouseListener(stml);
		}
		addShellListener(new ToolsShellListener());
	}

	public void show(EditablePart source, Point location) {
		if (isVisible())
			setVisible(false);

		this.currentTextPart = source;

		if (currentTextPart instanceof Paragraph) {
			final int size = 32;
			String text = textViewer
					.getRawParagraphText((Paragraph) currentTextPart);
			String textToShow = text.length() > size ? text.substring(0,
					size - 3) + "..." : text;
			for (StyleButton styleButton : styleButtons) {
				styleButton.setText(textToShow);
			}
		}
		pack();
		layout();
		if (source instanceof Control)
			setLocation(((Control) source).toDisplay(location.x, location.y));
		open();
	}

	class StyleButton extends Label {
		private static final long serialVersionUID = 7731102609123946115L;

		public StyleButton(Composite parent, int swtStyle) {
			super(parent, swtStyle);
		}

	}

	class StyledToolMouseListener extends MouseAdapter {
		private static final long serialVersionUID = 8516297091549329043L;

		@Override
		public void mouseDown(MouseEvent e) {
			Object eventSource = e.getSource();
			if (eventSource instanceof StyleButton) {
				StyleButton sb = (StyleButton) e.getSource();
				String style = sb.getData(RWT.CUSTOM_VARIANT).toString();
				textViewer
						.setParagraphStyle((Paragraph) currentTextPart, style);
			} else if (eventSource == deleteButton) {
				textViewer.deletePart((SectionPart) currentTextPart);
			} else if (eventSource == editButton) {
				textViewer.getCmsEditable().startEditing();
			} else if (eventSource == publishButton) {
				textViewer.getCmsEditable().stopEditing();
			}
			setVisible(false);
		}
	}

	class ToolsShellListener extends org.eclipse.swt.events.ShellAdapter {
		private static final long serialVersionUID = 8432350564023247241L;

		@Override
		public void shellDeactivated(ShellEvent e) {
			setVisible(false);
		}

	}
}
