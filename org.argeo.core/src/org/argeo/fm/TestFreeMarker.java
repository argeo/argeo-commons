package org.argeo.fm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class TestFreeMarker {
	static String base = System.getProperty("user.home") + File.separator + "dev" + File.separator + "work"
			+ File.separator + "ftl";
	static Configuration cfg;
	static {
		try {
			cfg = new Configuration(Configuration.VERSION_2_3_28);
			cfg.setDirectoryForTemplateLoading(new File(base));
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setLogTemplateExceptions(false);
			cfg.setWrapUncheckedExceptions(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: <template name> (in " + base + ")");
		}
		String template = args[0];
		try {
			/* Create a data-model */
			Map<String, Object> root = new HashMap<>();
			root.put("user", "Big Joe");
			Product latest = new Product();
			latest.setUrl("products/greenmouse.html");
			latest.setName("green mouse");
			root.put("latestProduct", latest);

			root.put("message", "It's a test");

			Map<String, Animal> animals = new HashMap<>();
			animals.put("mouse", new Animal("small", 50));
			animals.put("elephant", new Animal("big", 2000));
			animals.put("dog", new Animal("medium", 150));
			root.put("animals", animals);

			/* Get the template (uses cache internally) */
			Template temp = cfg.getTemplate(template);

			/* Merge data-model with template */
			String target = base + File.separator + template + ".html";
			Writer out = new FileWriter(target);
			temp.process(root, out);
			out.flush();
			System.out.println("Wrote " + target);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
