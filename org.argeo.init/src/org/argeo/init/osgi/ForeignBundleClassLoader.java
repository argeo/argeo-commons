package org.argeo.init.osgi;

import java.util.Optional;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A {@link ClassLoader} based on a {@link Bundle} from another OSGi runtime.
 */
class ForeignBundleClassLoader extends ClassLoader {// implements BundleReference {
	private BundleContext localBundleContext;
	private Bundle foreignBundle;

	public ForeignBundleClassLoader(BundleContext localBundleContext, Bundle foreignBundle) {
		super("Foreign bundle " + foreignBundle.toString(), Optional.ofNullable(foreignBundle.adapt(BundleWiring.class))
				.map((bw) -> bw.getClassLoader()).orElse(null));
		this.localBundleContext = localBundleContext;
		this.foreignBundle = foreignBundle;
	}

//		@Override
	protected Bundle getBundle() {
		return localBundleContext.getBundle(foreignBundle.getLocation());
	}

//		@Override
//		public URL getResource(String resName) {
//			URL res = super.getResource(resName);
//			return res;
//		}
//
//		@Override
//		protected URL findResource(String resName) {
//			Bundle localBundle = getBundle();
//			if (localBundle != null) {
//				URL res = localBundle.getEntry(resName);
//				if (res != null)
//					return res;
//			}
//			return null;
//		}

}
