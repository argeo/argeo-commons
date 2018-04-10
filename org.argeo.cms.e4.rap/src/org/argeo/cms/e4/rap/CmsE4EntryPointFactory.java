package org.argeo.cms.e4.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.e4.E4EntryPointFactory;
import org.eclipse.rap.rwt.application.EntryPoint;

public class CmsE4EntryPointFactory extends E4EntryPointFactory {
	private Subject subject;

	public CmsE4EntryPointFactory(Subject subject, E4ApplicationConfig config) {
		super(config);
		this.subject = subject;
	}

	@Override
	public EntryPoint create() {
		// Subject subject = new Subject();
		EntryPoint ep = createEntryPoint();
		EntryPoint authEp = new EntryPoint() {

			@Override
			public int createUI() {
				return Subject.doAs(subject, new PrivilegedAction<Integer>() {

					@Override
					public Integer run() {
						return ep.createUI();
					}

				});
			}
		};
		return authEp;
	}

	protected EntryPoint createEntryPoint() {
		return super.create();
	}

}
