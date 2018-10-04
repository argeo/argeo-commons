package org.argeo.cms.e4.parts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.argeo.cms.CmsException;
import org.argeo.cms.util.CmsUtils;
import org.eclipse.nebula.widgets.richtext.RichTextEditor;
import org.eclipse.nebula.widgets.richtext.RichTextEditorConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class HtmlEditor {

	@PostConstruct
	public void createUI(Composite parent) {
		String toolbarGroups;
		String removeButtons;
		try {
			toolbarGroups = IOUtils.toString(HtmlEditor.class.getResourceAsStream("CkEditor-toolbarGroups.json"),
					StandardCharsets.UTF_8);
			removeButtons = IOUtils.toString(HtmlEditor.class.getResourceAsStream("CkEditor-removeButtons.js"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CmsException("Cannot configure toolbar", e);
		}
//		System.out.println(toolbarGroups);
//		System.out.println(removeButtons);
		RichTextEditorConfiguration richTextEditorConfig = new RichTextEditorConfiguration();
		richTextEditorConfig.setOption(RichTextEditorConfiguration.TOOLBAR_GROUPS, toolbarGroups);
		richTextEditorConfig.setOption(RichTextEditorConfiguration.REMOVE_BUTTONS, removeButtons);
//		richTextEditorConfig.setRemoveStyles(false);
//		richTextEditorConfig.setRemovePasteFromWord(true);
//		richTextEditorConfig.setRemovePasteText(false);

//		richTextEditorConfig.setToolbarCollapsible(true);
//		richTextEditorConfig.setToolbarInitialExpanded(false);
		
		final Display display = parent.getDisplay();
		Composite composite = new Composite(parent, SWT.NONE);
//		composite.setLayoutData(new Fill);
		composite.setLayout(new GridLayout());
		final RichTextEditor richTextEditor = new RichTextEditor(composite, richTextEditorConfig, SWT.BORDER);
		richTextEditor.setText("<a href='http://googl.com'>Google</a>");
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		richTextEditor.setLayoutData(layoutData);
		richTextEditor.setBackground(new Color(display, 247, 247, 247));
		Composite toolbar = new Composite(composite, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new GridLayout(3, false));
		Button showContent = new Button(toolbar, SWT.PUSH);
		showContent.setText("Show Content");
		showContent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showContent(parent, richTextEditor, false);
			}
		});
		Button showSource = new Button(toolbar, SWT.PUSH);
		showSource.setText("Show Source");
		showSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showContent(parent, richTextEditor, true);
			}
		});
		Button clearBtn = new Button(toolbar, SWT.NONE);
		clearBtn.setText("Clear");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				richTextEditor.setText("");
			}
		});

	}

	private static void showContent(Composite parent, RichTextEditor editor, boolean source) {
		int style = SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL;
		final Shell content = new Shell(parent.getShell(), style);
		content.setLayout(new GridLayout(1, true));
		String text = editor.getText();
		if (source) {
			content.setText("Rich Text Source");
			Text viewer = new Text(content, SWT.MULTI | SWT.WRAP);
			viewer.setLayoutData(new GridData(400, 400));
			viewer.setText(text);
			viewer.setEditable(false);
		} else {
			content.setText("Rich Text");
			Browser viewer = new Browser(content, SWT.NONE);
			viewer.setLayoutData(new GridData(400, 400));
			viewer.setText(text);
			viewer.setEnabled(false);
		}
		Button ok = new Button(content, SWT.PUSH);
		ok.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		ok.setText("OK");
		ok.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				content.dispose();
			}
		});
		content.setDefaultButton(ok);
		content.pack();
		Display display = parent.getDisplay();
		int left = (display.getClientArea().width / 2) - (content.getBounds().width / 2);
		content.setLocation(left, 40);
		content.open();
		ok.setFocus();
	}

}
