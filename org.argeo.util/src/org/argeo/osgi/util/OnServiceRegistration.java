package org.argeo.osgi.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OnServiceRegistration<R> implements Future<R> {
	private BundleContext ownBundleContext = FrameworkUtil.getBundle(OnServiceRegistration.class).getBundleContext();

	private ServiceTracker<?, ?> st;

	private R result;
	private boolean cancelled = false;
	private Throwable exception;

	public <T> OnServiceRegistration(Class<T> clss, Function<T, R> function) {
		this(null, clss, function);
	}

	public <T> OnServiceRegistration(BundleContext bundleContext, Class<T> clss, Function<T, R> function) {
		st = new ServiceTracker<T, T>(bundleContext != null ? bundleContext : ownBundleContext, clss, null) {

			@Override
			public T addingService(ServiceReference<T> reference) {
				T service = super.addingService(reference);
				try {
					if (result != null)// we only want the first one
						return service;
					result = function.apply(service);
					return service;
				} catch (Exception e) {
					exception = e;
					return service;
				} finally {
					close();
				}
			}
		};
		st.open(bundleContext == null);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (result != null || exception != null || cancelled)
			return false;
		st.close();
		cancelled = true;
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return result != null || cancelled;
	}

	@Override
	public R get() throws InterruptedException, ExecutionException {
		st.waitForService(0);
		return tryGetResult();
	}

	@Override
	public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		st.waitForService(TimeUnit.MILLISECONDS.convert(timeout, unit));
		if (result == null)
			throw new TimeoutException("No result after " + timeout + " " + unit);
		return tryGetResult();
	}

	protected R tryGetResult() throws ExecutionException, CancellationException {
		if (cancelled)
			throw new CancellationException();
		if (exception != null)
			throw new ExecutionException(exception);
		if (result == null)// this should not happen
			try {
				throw new IllegalStateException("No result available");
			} catch (Exception e) {
				exception = e;
				throw new ExecutionException(e);
			}
		return result;
	}

}
