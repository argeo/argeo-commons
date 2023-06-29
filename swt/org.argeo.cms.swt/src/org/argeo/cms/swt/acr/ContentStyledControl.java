package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.argeo.cms.swt.widgets.StyledControl;
import org.argeo.cms.ux.acr.ContentPart;
import org.eclipse.swt.widgets.Composite;

public abstract class ContentStyledControl extends StyledControl implements ContentPart {

	private static final long serialVersionUID = -5714246408818696583L;

	public ContentStyledControl(Composite parent, int swtStyle, Content content) {
		super(parent, swtStyle);
		setData(content);
	}

	@Override
	public Content getContent() {
		return (Content) getData();
	}

}
