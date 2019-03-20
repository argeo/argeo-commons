package org.argeo.cms.ui.fm;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.fm.jcr.JcrModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class FmUiProvider implements CmsUiProvider {
	static Configuration cfg;
	static {
		try {
			cfg = new Configuration(Configuration.VERSION_2_3_28);
			cfg.setDirectoryForTemplateLoading(new File(System.getProperty("user.home") + File.separator + "dev"
					+ File.separator + "work" + File.separator + "ftl"));
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setLogTemplateExceptions(false);
			cfg.setWrapUncheckedExceptions(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String template;

	public FmUiProvider(String template) {
		this.template = template;
	}

	@Override
	public Control createUi(Composite parent, Node context) throws RepositoryException {
		Browser browser = new Browser(parent, SWT.NONE);
		try {
			Map<String, Object> root = new HashMap<>();
			if (context != null)
				root.put("node", new JcrModel(context));

			Template temp = cfg.getTemplate(template);
			StringWriter out = new StringWriter();
			temp.process(root, out);
			browser.setText(out.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return browser;
	}

}
