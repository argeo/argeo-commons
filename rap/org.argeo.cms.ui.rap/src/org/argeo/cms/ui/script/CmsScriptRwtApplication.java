package org.argeo.cms.ui.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.jcr.Repository;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

public class CmsScriptRwtApplication implements ApplicationConfiguration {
	public final static String APP = "APP";
	public final static String BC = "BC";

	private final Log log = LogFactory.getLog(CmsScriptRwtApplication.class);

	BundleContext bundleContext;
	Repository repository;

	ScriptEngine engine;

	public void init(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		ClassLoader bundleCl = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
		ClassLoader originalCcl = Thread.currentThread().getContextClassLoader();
		try {
//			Thread.currentThread().setContextClassLoader(bundleCl);// GraalVM needs it to be before creating manager
//			ScriptEngineManager scriptEngineManager = new ScriptEngineManager(bundleCl);
//			engine = scriptEngineManager.getEngineByName("JavaScript");
//			if (engine == null) {// Nashorn
//				Thread.currentThread().setContextClassLoader(originalCcl);
//				scriptEngineManager = new ScriptEngineManager();
//				Thread.currentThread().setContextClassLoader(bundleCl);
//				engine = scriptEngineManager.getEngineByName("JavaScript");
//			}
			engine = loadScriptEngine(originalCcl, bundleCl);

			// Load script
			URL appUrl = bundleContext.getBundle().getEntry("cms/app.js");
			// System.out.println("Loading " + appUrl);
			// System.out.println("Loading " + appUrl.getHost());
			// System.out.println("Loading " + appUrl.getPath());

			CmsScriptApp app = new CmsScriptApp(engine);
			engine.put(APP, app);
			engine.put(BC, bundleContext);
			try (Reader reader = new InputStreamReader(appUrl.openStream())) {
				engine.eval(reader);
			} catch (IOException | ScriptException e) {
				throw new CmsException("Cannot execute " + appUrl, e);
			}

			if (log.isDebugEnabled())
				log.debug("CMS script app initialized from " + appUrl);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(originalCcl);
		}
	}

	public void destroy(BundleContext bundleContext) {
		engine = null;
	}

	@Override
	public void configure(Application application) {
		load(application);
	}

	void load(Application application) {
		CmsScriptApp app = getApp();
		app.apply(bundleContext, repository, application);
		if (log.isDebugEnabled())
			log.debug("CMS script app loaded to " + app.getWebPath());
	}

	CmsScriptApp getApp() {
		if (engine == null)
			throw new IllegalStateException("CMS script app is not initialized");
		return (CmsScriptApp) engine.get(APP);
	}

	void update() {

		try {
			bundleContext.getBundle().update();
		} catch (BundleException e) {
			e.printStackTrace();
		}
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	private static ScriptEngine loadScriptEngine(ClassLoader originalCcl, ClassLoader bundleCl) {
		Thread.currentThread().setContextClassLoader(bundleCl);// GraalVM needs it to be before creating manager
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager(bundleCl);
		ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
		if (engine == null) {// Nashorn
			Thread.currentThread().setContextClassLoader(originalCcl);
			scriptEngineManager = new ScriptEngineManager();
			Thread.currentThread().setContextClassLoader(bundleCl);
			engine = scriptEngineManager.getEngineByName("JavaScript");
		}
		return engine;
	}
}
