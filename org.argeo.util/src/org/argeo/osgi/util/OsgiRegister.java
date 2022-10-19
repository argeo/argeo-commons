package org.argeo.osgi.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class OsgiRegister {
	private final BundleContext bundleContext;
	private Executor executor;

	private CompletableFuture<Void> shutdownStarting = new CompletableFuture<Void>();

	public OsgiRegister(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		// TODO experiment with dedicated executors
		this.executor = ForkJoinPool.commonPool();
	}

	public <T> void set(T obj, Class<T> clss, Map<String, Object> attributes, Class<?>... classes) {
		CompletableFuture<ServiceRegistration<?>> srf = new CompletableFuture<ServiceRegistration<?>>();
		CompletableFuture<T> postRegistration = CompletableFuture.supplyAsync(() -> {
			List<String> lst = new ArrayList<>();
			lst.add(clss.getName());
			for (Class<?> c : classes) {
				lst.add(c.getName());
			}
			ServiceRegistration<?> sr = bundleContext.registerService(lst.toArray(new String[lst.size()]), obj,
					new Hashtable<String, Object>(attributes));
			srf.complete(sr);
			return obj;
		}, executor);
//		Singleton<T> singleton = new Singleton<T>(clss, postRegistration);

//		shutdownStarting. //
//				thenCompose(singleton::prepareUnregistration). //
//				thenRunAsync(() -> {
//					try {
//						srf.get().unregister();
//					} catch (InterruptedException | ExecutionException e) {
//						e.printStackTrace();
//					}
//				}, executor);
//		return singleton;
	}

	public void shutdown() {
		shutdownStarting.complete(null);
	}
}
