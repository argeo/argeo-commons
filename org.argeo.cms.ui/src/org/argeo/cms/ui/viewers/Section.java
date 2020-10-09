package org.argeo.cms.ui.viewers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.cms.ui.widgets.JcrComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Section extends JcrComposite {
	private static final long serialVersionUID = -5933796173755739207L;

	private final Section parentSection;
	private Composite sectionHeader;
	private final Integer relativeDepth;

	public Section(Composite parent, int style, Node node) {
		this(parent, findSection(parent), style, node);
	}

	public Section(Section section, int style, Node node) {
		this(section, section, style, node);
	}

	protected Section(Composite parent, Section parentSection, int style, Node node) {
		super(parent, style, node);
		try {
			this.parentSection = parentSection;
			if (parentSection != null) {
				relativeDepth = getNode().getDepth() - parentSection.getNode().getDepth();
			} else {
				relativeDepth = 0;
			}
			setLayout(CmsUiUtils.noSpaceGridLayout());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot create section from " + node, e);
		}
	}

	public Map<String, Section> getSubSections() throws RepositoryException {
		LinkedHashMap<String, Section> result = new LinkedHashMap<String, Section>();
		for (Control child : getChildren()) {
			if (child instanceof Composite) {
				collectDirectSubSections((Composite) child, result);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private void collectDirectSubSections(Composite composite, LinkedHashMap<String, Section> subSections)
			throws RepositoryException {
		if (composite == sectionHeader || composite instanceof EditablePart)
			return;
		if (composite instanceof Section) {
			Section section = (Section) composite;
			subSections.put(section.getNodeId(), section);
			return;
		}

		for (Control child : composite.getChildren())
			if (child instanceof Composite)
				collectDirectSubSections((Composite) child, subSections);
	}

	public Composite createHeader() {
		return createHeader(this);
	}

	public Composite createHeader(Composite parent) {
		if (sectionHeader != null)
			sectionHeader.dispose();

		sectionHeader = new Composite(parent, SWT.NONE);
		sectionHeader.setLayoutData(CmsUiUtils.fillWidth());
		sectionHeader.setLayout(CmsUiUtils.noSpaceGridLayout());
		// sectionHeader.moveAbove(null);
		// layout();
		return sectionHeader;
	}

	public Composite getHeader() {
		if (sectionHeader != null && sectionHeader.isDisposed())
			sectionHeader = null;
		return sectionHeader;
	}

	// SECTION PARTS
	public SectionPart getSectionPart(String partId) {
		for (Control child : getChildren()) {
			if (child instanceof SectionPart) {
				SectionPart paragraph = (SectionPart) child;
				if (paragraph.getPartId().equals(partId))
					return paragraph;
			}
		}
		return null;
	}

	public SectionPart nextSectionPart(SectionPart sectionPart) {
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (sectionPart == children[i])
				if (i + 1 < children.length) {
					Composite next = (Composite) children[i + 1];
					return (SectionPart) next;
				} else {
					// next section
				}
		}
		return null;
	}

	public SectionPart previousSectionPart(SectionPart sectionPart) {
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (sectionPart == children[i])
				if (i != 0) {
					Composite previous = (Composite) children[i - 1];
					return (SectionPart) previous;
				} else {
					// previous section
				}
		}
		return null;
	}

	@Override
	public String toString() {
		if (parentSection == null)
			return "Main section " + getNode();
		return "Section " + getNode();
	}

	public Section getParentSection() {
		return parentSection;
	}

	public Integer getRelativeDepth() {
		return relativeDepth;
	}

	/** Recursively finds the related section in the parents (can be itself) */
	public static Section findSection(Control control) {
		if (control == null)
			return null;
		if (control instanceof Section)
			return (Section) control;
		else
			return findSection(control.getParent());
	}
}
