package org.argeo.cms.swt.widgets;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.argeo.api.acr.Content;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.acr.ContentComposite;
import org.argeo.cms.ux.widgets.EditablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** A structured UI related to a JCR context. */
public class SwtSection extends ContentComposite {
	private static final long serialVersionUID = -5933796173755739207L;

	private final SwtSection parentSection;
	private Composite sectionHeader;
	private final Integer relativeDepth;

	public SwtSection(Composite parent, int style, Content node) {
		this(parent, findSection(parent), style, node);
	}

	public SwtSection(SwtSection section, int style, Content node) {
		this(section, section, style, node);
	}

	protected SwtSection(Composite parent, SwtSection parentSection, int style, Content node) {
		super(parent, style, node);
		this.parentSection = parentSection;
		if (parentSection != null) {
			relativeDepth = getProvidedContent().getDepth() - parentSection.getProvidedContent().getDepth();
		} else {
			relativeDepth = 0;
		}
		setLayout(CmsSwtUtils.noSpaceGridLayout());
	}

	public Map<String, SwtSection> getSubSections() {
		LinkedHashMap<String, SwtSection> result = new LinkedHashMap<String, SwtSection>();
		for (Control child : getChildren()) {
			if (child instanceof Composite) {
				collectDirectSubSections((Composite) child, result);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private void collectDirectSubSections(Composite composite, LinkedHashMap<String, SwtSection> subSections) {
		if (composite == sectionHeader || composite instanceof EditablePart)
			return;
		if (composite instanceof SwtSection) {
			SwtSection section = (SwtSection) composite;
			subSections.put(section.getProvidedContent().getSessionLocalId(), section);
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
		sectionHeader.setLayoutData(CmsSwtUtils.fillWidth());
		sectionHeader.setLayout(CmsSwtUtils.noSpaceGridLayout());
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
	public SwtSectionPart getSectionPart(String partId) {
		for (Control child : getChildren()) {
			if (child instanceof SwtSectionPart) {
				SwtSectionPart sectionPart = (SwtSectionPart) child;
				if (sectionPart.getPartId().equals(partId))
					return sectionPart;
			}
		}
		return null;
	}

	public SwtSectionPart nextSectionPart(SwtSectionPart sectionPart) {
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (sectionPart == children[i]) {
				for (int j = i + 1; j < children.length; j++) {
					if (children[i + 1] instanceof SwtSectionPart) {
						return (SwtSectionPart) children[i + 1];
					}
				}

//				if (i + 1 < children.length) {
//					Composite next = (Composite) children[i + 1];
//					return (SectionPart) next;
//				} else {
//					// next section
//				}
			}
		}
		return null;
	}

	public SwtSectionPart previousSectionPart(SwtSectionPart sectionPart) {
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (sectionPart == children[i])
				if (i != 0) {
					Composite previous = (Composite) children[i - 1];
					return (SwtSectionPart) previous;
				} else {
					// previous section
				}
		}
		return null;
	}

	@Override
	public String toString() {
		if (parentSection == null)
			return "Main section " + getContent();
		return "Section " + getContent();
	}

	public SwtSection getParentSection() {
		return parentSection;
	}

	public Integer getRelativeDepth() {
		return relativeDepth;
	}

	/** Recursively finds the related section in the parents (can be itself) */
	public static SwtSection findSection(Control control) {
		if (control == null)
			return null;
		if (control instanceof SwtSection)
			return (SwtSection) control;
		else
			return findSection(control.getParent());
	}
}
