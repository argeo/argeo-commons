package org.argeo.cms.ui.script;

import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.ui.CmsUiProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.framework.BundleContext;

class ScriptUi implements CmsUiProvider {
	private final static Log log = LogFactory.getLog(ScriptUi.class);

	private boolean development = true;
	private ScriptEngine scriptEngine;

	private URL appUrl;
	// private BundleContext bundleContext;
	// private String path;

	// private Bindings bindings;
	// private String script;

	public ScriptUi(BundleContext bundleContext,ScriptEngine scriptEngine, String path) {
		this.scriptEngine = scriptEngine;
////		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
//		ClassLoader bundleCl = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
//		ClassLoader originalCcl = Thread.currentThread().getContextClassLoader();
//		try {
////			Thread.currentThread().setContextClassLoader(bundleCl);
////			scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
////			scriptEngine.put(CmsScriptRwtApplication.BC, bundleContext);
//			scriptEngine = CmsScriptRwtApplication.loadScriptEngine(originalCcl, bundleCl);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			Thread.currentThread().setContextClassLoader(originalCcl);
//		}
		this.appUrl = bundleContext.getBundle().getEntry(path);
		load();
	}

	private void load() {
//		try (Reader reader = new InputStreamReader(appUrl.openStream())) {
//			scriptEngine.eval(reader);
//		} catch (IOException | ScriptException e) {
//			log.warn("Cannot execute " + appUrl, e);
//		}

		try {
			scriptEngine.eval("load('" + appUrl + "')");
		} catch (ScriptException e) {
			log.warn("Cannot execute " + appUrl, e);
		}

	}

	// public ScriptUiProvider(ScriptEngine scriptEngine, String script) throws
	// ScriptException {
	// super();
	// this.scriptEngine = scriptEngine;
	// this.script = script;
	// bindings = scriptEngine.createBindings();
	// scriptEngine.eval(script, bindings);
	// }

	@Override
	public Control createUi(Composite parent, Node context) throws RepositoryException {
		long begin = System.currentTimeMillis();
		// if (bindings == null) {
		// bindings = scriptEngine.createBindings();
		// try {
		// scriptEngine.eval(script, bindings);
		// } catch (ScriptException e) {
		// log.warn("Cannot evaluate script", e);
		// }
		// }
		// Bindings bindings = scriptEngine.createBindings();
		// bindings.put("parent", parent);
		// bindings.put("context", context);
		// URL appUrl = bundleContext.getBundle().getEntry(path);
		// try (Reader reader = new InputStreamReader(appUrl.openStream())) {
		// scriptEngine.eval(reader,bindings);
		// } catch (IOException | ScriptException e) {
		// log.warn("Cannot execute " + appUrl, e);
		// }

		if (development)
			load();

		Invocable invocable = (Invocable) scriptEngine;
		try {
			invocable.invokeFunction("createUi", parent, context);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long duration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace(appUrl + " UI in " + duration + " ms");
		return null;
	}

}
