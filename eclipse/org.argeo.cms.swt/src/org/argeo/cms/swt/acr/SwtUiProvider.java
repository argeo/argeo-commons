package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

@FunctionalInterface
public interface SwtUiProvider {
	Control createUiPart(Composite parent, Content context);
}
