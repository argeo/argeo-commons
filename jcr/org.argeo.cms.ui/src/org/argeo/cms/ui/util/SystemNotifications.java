package org.argeo.cms.ui.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.argeo.cms.swt.CmsException;
import org.argeo.cms.swt.CmsStyles;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Shell displaying system notifications such as exceptions */
public class SystemNotifications extends Shell implements CmsStyles,
		MouseListener {
	private static final long serialVersionUID = -8129377525216022683L;

	private Control source;

	public SystemNotifications(Control source) {
		super(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		this.source = source;

		// TODO UI
		// setLocation(source.toDisplay(source.getSize().x - getSize().x,
		// source.getSize().y));
		setLayout(new GridLayout());
		addMouseListener(this);

		addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = 5178980294808435833L;

			@Override
			public void shellDeactivated(ShellEvent e) {
				close();
				dispose();
			}
		});

	}

	public void notifyException(Throwable exception) {
		Composite pane = this;

		Label lbl = new Label(pane, SWT.NONE);
		lbl.setText(exception.getLocalizedMessage()
				+ (exception instanceof CmsException ? "" : "("
						+ exception.getClass().getName() + ")") + "\n");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lbl.addMouseListener(this);
		if (exception.getCause() != null)
			appendCause(pane, exception.getCause());

		StringBuilder mailToUrl = new StringBuilder("mailto:?");
		try {
			mailToUrl.append("subject=").append(
					URLEncoder.encode(
							"Exception "
									+ new SimpleDateFormat("yyyy-MM-dd hh:mm")
											.format(new Date()), "UTF-8")
							.replace("+", "%20"));

			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			IOUtils.closeQuietly(sw);

			// see
			// http://stackoverflow.com/questions/4737841/urlencoder-not-able-to-translate-space-character
			String encoded = URLEncoder.encode(sw.toString(), "UTF-8").replace(
					"+", "%20");
			mailToUrl.append("&amp;body=").append(encoded);
		} catch (UnsupportedEncodingException e) {
			mailToUrl.append("&amp;body=").append("Could not encode: ")
					.append(e.getMessage());
		}
		Label mailTo = new Label(pane, SWT.NONE);
		CmsSwtUtils.markup(mailTo);
		mailTo.setText("<a href=\"" + mailToUrl + "\">Send details</a>");
		mailTo.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));

		pack();
		layout();

		setLocation(source.toDisplay(source.getSize().x - getSize().x,
				source.getSize().y - getSize().y));
		open();
	}

	private void appendCause(Composite parent, Throwable e) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(" caused by: " + e.getLocalizedMessage() + " ("
				+ e.getClass().getName() + ")" + "\n");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lbl.addMouseListener(this);
		if (e.getCause() != null)
			appendCause(parent, e.getCause());
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}

	@Override
	public void mouseDown(MouseEvent e) {
		close();
		dispose();
	}

	@Override
	public void mouseUp(MouseEvent e) {
	}

}
