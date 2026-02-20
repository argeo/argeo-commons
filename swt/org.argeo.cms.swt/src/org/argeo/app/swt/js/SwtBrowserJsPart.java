package org.argeo.app.swt.js;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.js.JsClient;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * A part using a {@link Browser} and remote JavaScript components on the client
 * side.
 */
public class SwtBrowserJsPart implements JsClient {
	private final static CmsLog log = CmsLog.getLog(SwtBrowserJsPart.class);

	private final static String GLOBAL_THIS_ = "globalThis.";

	private final Browser browser;
	private final CompletableFuture<Boolean> readyStage = new CompletableFuture<>();

	/**
	 * Tasks that were requested before the context was ready. Typically
	 * configuration methods on the part while the user interfaces is being build.
	 */
	private List<PreReadyToDo> preReadyToDos = new ArrayList<>();

	public SwtBrowserJsPart(Composite parent, int style, String url) {
		CmsView cmsView = CmsSwtUtils.getCmsView(parent);
		this.browser = new Browser(parent, 0);
		if (parent.getLayout() instanceof GridLayout)
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// TODO other layouts

		URI u = cmsView.toBackendUri(url);
		browser.setUrl(u.toString());
		browser.addProgressListener(new ProgressListener() {
			static final long serialVersionUID = 1L;

			@Override
			public void completed(ProgressEvent event) {
				try {
					init();
					loadExtensions();
					// execute todos in order
					for (PreReadyToDo toDo : preReadyToDos) {
						toDo.run();
					}
					preReadyToDos.clear();
					readyStage.complete(true);
				} catch (Exception e) {
					log.error("Cannot initialise " + url + " in browser", e);
					readyStage.complete(false);
				}
			}

			@Override
			public void changed(ProgressEvent event) {
			}
		});
	}

	/*
	 * LIFECYCLE
	 */

	/**
	 * Called when the page has been loaded, typically in order to initialise
	 * JavaScript objects. One MUST use {@link #doExecute(String, Object...)} in
	 * order to do so, since the context is not yet considered ready and calls to
	 * {@link #evaluate(String, Object...)} will block.
	 */
	protected void init() {
	}

	/**
	 * To be overridden with calls to {@link #loadExtension(String)}.
	 */
	protected void loadExtensions() {

	}

	protected void loadExtension(String url) {
		URI u = CmsSwtUtils.getCmsView(getControl()).toBackendUri(url);
		browser.evaluate(String.format(Locale.ROOT, "import('%s')", u.toString()));
	}

	public CompletionStage<Boolean> getReadyStage() {
		return readyStage.minimalCompletionStage();
	}

	/*
	 * JAVASCRIPT ACCESS
	 */

	@Override
	public Object evaluate(String js, Object... args) {
		assert browser.getDisplay().equals(Display.findDisplay(Thread.currentThread())) : "Not the proper UI thread.";
		if (!readyStage.isDone())
			throw new IllegalStateException("Methods returning a result can only be called after UI initialisation.");
		if (browser.isDisposed())
			return null;
		Object result = browser.evaluate(String.format(Locale.ROOT, js, args));
		return result;
	}

	@Override
	public void execute(String js, Object... args) {
		String jsToExecute = String.format(Locale.ROOT, js, args);
		if (readyStage.isDone()) {
			if (browser.isDisposed())
				return;
			boolean success = browser.execute(jsToExecute);
			if (!success)
				throw new RuntimeException("JavaScript execution failed.");
		} else {
			PreReadyToDo toDo = new PreReadyToDo(jsToExecute);
			preReadyToDos.add(toDo);
		}
	}

	@Override
	public String createJsFunction(String name, Function<Object[], Object> toDo) {
		// browser functions must be directly on window (RAP specific)
		new BrowserFunction(browser, name) {

			@Override
			public Object function(Object[] arguments) {
				Object result = toDo.apply(arguments);
				return result;
			}

		};
		return "window." + name;
	}

	/**
	 * Directly executes, even if {@link #getReadyStage()} is not completed. Except
	 * in initialisation, {@link #evaluate(String, Object...)} should be used
	 * instead.
	 */
	protected void doExecute(String js, Object... args) {
		if (browser.isDisposed())
			return;
		browser.execute(String.format(Locale.ROOT, js, args));
	}

	@Override
	public String getJsVarName(String name) {
		return GLOBAL_THIS_ + name;
	}

	class PreReadyToDo implements Runnable {
		private String js;

		public PreReadyToDo(String js) {
			this.js = js;
		}

		@Override
		public void run() {
			if (browser.isDisposed())
				return;
			boolean success = browser.execute(js);
			if (!success && log.isTraceEnabled())
				log.error("Pre-ready JavaScript failed: " + js);
		}
	}

	/*
	 * ACCESSORS
	 */

	public Control getControl() {
		return browser;
	}

}
