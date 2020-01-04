package org.argeo.cms.text;

import static org.argeo.cms.util.CmsUtils.fillWidth;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.CmsEditable;
import org.argeo.cms.viewers.Section;
import org.eclipse.swt.widgets.Composite;

/**
 * Manages hardcoded sections as an arbitrary hierarchy under the main section,
 * which contains no text and no title.
 */
public class CustomTextEditor extends AbstractTextViewer {
	private static final long serialVersionUID = 5277789504209413500L;

	public CustomTextEditor(Composite parent, int style, Node textNode,
			CmsEditable cmsEditable) throws RepositoryException {
		this(new Section(parent, style, textNode), style, cmsEditable);
	}

	public CustomTextEditor(Section mainSection, int style,
			CmsEditable cmsEditable) throws RepositoryException {
		super(mainSection, style, cmsEditable);
		mainSection.setLayoutData(fillWidth());
	}

	@Override
	public Section getMainSection() {
		return super.getMainSection();
	}
}
