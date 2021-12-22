package org.argeo.cms.swt.gcr;

import org.argeo.api.cms.MvcProvider;
import org.argeo.api.gcr.Content;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

@FunctionalInterface
public interface SwtUiProvider extends MvcProvider<Composite, Content, Control> {

}
