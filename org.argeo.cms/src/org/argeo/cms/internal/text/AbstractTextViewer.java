package org.argeo.cms.internal.text;

import static javax.jcr.Property.JCR_TITLE;
import static org.argeo.cms.CmsUtils.fillWidth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Observer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsException;
import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsNames;
import org.argeo.cms.CmsSession;
import org.argeo.cms.CmsTypes;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.IdentityTextInterpreter;
import org.argeo.cms.TextInterpreter;
import org.argeo.cms.text.Img;
import org.argeo.cms.text.Paragraph;
import org.argeo.cms.text.TextSection;
import org.argeo.cms.viewers.AbstractPageViewer;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.NodePart;
import org.argeo.cms.viewers.PropertyPart;
import org.argeo.cms.viewers.Section;
import org.argeo.cms.viewers.SectionPart;
import org.argeo.cms.widgets.EditableImage;
import org.argeo.cms.widgets.EditableText;
import org.argeo.cms.widgets.StyledControl;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.addons.fileupload.FileDetails;
import org.eclipse.rap.addons.fileupload.FileUploadEvent;
import org.eclipse.rap.addons.fileupload.FileUploadHandler;
import org.eclipse.rap.addons.fileupload.FileUploadListener;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/** Base class for text viewers and editors. */
public abstract class AbstractTextViewer extends AbstractPageViewer implements
		CmsNames, KeyListener, Observer {
	private static final long serialVersionUID = -2401274679492339668L;
	private final static Log log = LogFactory.getLog(AbstractTextViewer.class);

	private final Section mainSection;

	private TextInterpreter textInterpreter = new IdentityTextInterpreter();
	private CmsImageManager imageManager = CmsSession.current.get()
			.getImageManager();

	private FileUploadListener fileUploadListener;
	private TextContextMenu styledTools;

	private final boolean flat;

	protected AbstractTextViewer(Section parent, int style,
			CmsEditable cmsEditable) {
		super(parent, style, cmsEditable);
		flat = SWT.FLAT == (style & SWT.FLAT);

		if (getCmsEditable().canEdit()) {
			fileUploadListener = new FUL();
			styledTools = new TextContextMenu(this, parent.getDisplay());
		}
		this.mainSection = parent;
		initModelIfNeeded(mainSection.getNode());
		// layout(this.mainSection);
	}

	@Override
	public Control getControl() {
		return mainSection;
	}

	protected void refresh(Control control) throws RepositoryException {
		if (!(control instanceof Section))
			return;
		Section section = (Section) control;
		if (section instanceof TextSection) {
			CmsUtils.clear(section);
			Node node = section.getNode();
			TextSection textSection = (TextSection) section;
			if (node.hasProperty(Property.JCR_TITLE)) {
				if (section.getHeader() == null)
					section.createHeader();
				if (node.hasProperty(Property.JCR_TITLE)) {
					SectionTitle title = newSectionTitle(textSection, node);
					title.setLayoutData(CmsUtils.fillWidth());
					updateContent(title);
				}
			}

			for (NodeIterator ni = node.getNodes(CMS_P); ni.hasNext();) {
				Node child = ni.nextNode();
				final SectionPart sectionPart;
				if (child.isNodeType(CmsTypes.CMS_IMAGE)
						|| child.isNodeType(NodeType.NT_FILE)) {
					sectionPart = newImg(textSection, child);
				} else if (child.isNodeType(CmsTypes.CMS_STYLED)) {
					sectionPart = newParagraph(textSection, child);
				} else {
					sectionPart = newSectionPart(textSection, child);
					if (sectionPart == null)
						throw new CmsException("Unsupported node " + child);
					// TODO list node types in exception
				}
				if (sectionPart instanceof Control)
					((Control) sectionPart).setLayoutData(CmsUtils.fillWidth());
			}

			if (!flat)
				for (NodeIterator ni = section.getNode().getNodes(CMS_H); ni
						.hasNext();) {
					Node child = ni.nextNode();
					if (child.isNodeType(CmsTypes.CMS_SECTION)) {
						TextSection newSection = new TextSection(section,
								SWT.NONE, child);
						newSection.setLayoutData(CmsUtils.fillWidth());
						refresh(newSection);
					}
				}
		} else {
			for (Section s : section.getSubSections().values())
				refresh(s);
		}
		// section.layout();
	}

	/** To be overridden in order to provide additional SectionPart types */
	protected SectionPart newSectionPart(TextSection textSection, Node node) {
		return null;
	}

	// CRUD
	protected Paragraph newParagraph(TextSection parent, Node node)
			throws RepositoryException {
		Paragraph paragraph = new Paragraph(parent, parent.getStyle(), node);
		updateContent(paragraph);
		paragraph.setLayoutData(fillWidth());
		paragraph.setMouseListener(getMouseListener());
		return paragraph;
	}

	protected Img newImg(TextSection parent, Node node)
			throws RepositoryException {
		Img img = new Img(parent, parent.getStyle(), node) {
			private static final long serialVersionUID = 1297900641952417540L;

			@Override
			protected void setContainerLayoutData(Composite composite) {
				composite.setLayoutData(CmsUtils.grabWidth(SWT.CENTER,
						SWT.DEFAULT));
			}

			@Override
			protected void setControlLayoutData(Control control) {
				control.setLayoutData(CmsUtils.grabWidth(SWT.CENTER,
						SWT.DEFAULT));
			}
		};
		img.setLayoutData(CmsUtils.grabWidth(SWT.CENTER, SWT.DEFAULT));
		updateContent(img);
		img.setMouseListener(getMouseListener());
		return img;
	}

	protected SectionTitle newSectionTitle(TextSection parent, Node node)
			throws RepositoryException {
		SectionTitle title = new SectionTitle(parent.getHeader(),
				parent.getStyle(), node.getProperty(JCR_TITLE));
		updateContent(title);
		title.setMouseListener(getMouseListener());
		return title;
	}

	protected SectionTitle prepareSectionTitle(Section newSection,
			String titleText) throws RepositoryException {
		Node sectionNode = newSection.getNode();
		if (!sectionNode.hasProperty(JCR_TITLE))
			sectionNode.setProperty(Property.JCR_TITLE, "");
		getTextInterpreter().write(sectionNode.getProperty(Property.JCR_TITLE),
				titleText);
		if (newSection.getHeader() == null)
			newSection.createHeader();
		SectionTitle sectionTitle = newSectionTitle((TextSection) newSection,
				sectionNode);
		return sectionTitle;
	}

	protected void updateContent(EditablePart part) throws RepositoryException {
		if (part instanceof SectionPart) {
			SectionPart sectionPart = (SectionPart) part;
			Node partNode = sectionPart.getNode();

			if (part instanceof StyledControl
					&& (sectionPart.getSection() instanceof TextSection)) {
				TextSection section = (TextSection) sectionPart.getSection();
				StyledControl styledControl = (StyledControl) part;
				if (partNode.isNodeType(CmsTypes.CMS_STYLED)) {
					String style = partNode.hasProperty(CMS_STYLE) ? partNode
							.getProperty(CMS_STYLE).getString() : section
							.getDefaultTextStyle();
					styledControl.setStyle(style);
				}
			}
			// use control AFTER setting style, since it may have been reset

			if (part instanceof EditableText) {
				EditableText paragraph = (EditableText) part;
				if (paragraph == getEdited())
					paragraph.setText(textInterpreter.read(partNode));
				else
					paragraph.setText(textInterpreter.raw(partNode));
			} else if (part instanceof EditableImage) {
				EditableImage editableImage = (EditableImage) part;
				imageManager.load(partNode, part.getControl(),
						editableImage.getPreferredImageSize());
			}
		} else if (part instanceof SectionTitle) {
			SectionTitle title = (SectionTitle) part;
			title.setStyle(title.getSection().getTitleStyle());
			// use control AFTER setting style
			if (title == getEdited())
				title.setText(textInterpreter.read(title.getProperty()));
			else
				title.setText(textInterpreter.raw(title.getProperty()));
		}
	}

	// OVERRIDDEN FROM PARENT VIEWER
	@Override
	protected void save(EditablePart part) throws RepositoryException {
		if (part instanceof EditableText) {
			EditableText et = (EditableText) part;
			String text = ((Text) et.getControl()).getText();

			String[] lines = text.split("[\r\n]+");
			assert lines.length != 0;
			saveLine(part, lines[0]);
			if (lines.length > 1) {
				ArrayList<Control> toLayout = new ArrayList<Control>();
				if (part instanceof Paragraph) {
					Paragraph currentParagraph = (Paragraph) et;
					Section section = currentParagraph.getSection();
					Node sectionNode = section.getNode();
					Node currentParagraphN = currentParagraph.getNode();
					for (int i = 1; i < lines.length; i++) {
						Node newNode = sectionNode.addNode(CMS_P);
						newNode.addMixin(CmsTypes.CMS_STYLED);
						saveLine(newNode, lines[i]);
						// second node was create as last, if it is not the next
						// one, it
						// means there are some in between and we can take the
						// one at
						// index+1 for the re-order
						if (newNode.getIndex() > currentParagraphN.getIndex() + 1) {
							sectionNode.orderBefore(p(newNode.getIndex()),
									p(currentParagraphN.getIndex() + 1));
						}
						Paragraph newParagraph = newParagraph(
								(TextSection) section, newNode);
						newParagraph.moveBelow(currentParagraph);
						toLayout.add(newParagraph);

						currentParagraph = newParagraph;
						currentParagraphN = newNode;
					}
				}
				// TODO or rather return the created paragarphs?
				layout(toLayout.toArray(new Control[toLayout.size()]));
			}
		}
	}

	protected void saveLine(EditablePart part, String line) {
		if (part instanceof NodePart) {
			saveLine(((NodePart) part).getNode(), line);
		} else if (part instanceof PropertyPart) {
			saveLine(((PropertyPart) part).getProperty(), line);
		} else {
			throw new CmsException("Unsupported part " + part);
		}
	}

	protected void saveLine(Item item, String line) {
		line = line.trim();
		textInterpreter.write(item, line);
	}

	@Override
	protected void prepare(EditablePart part, Object caretPosition) {
		Control control = part.getControl();
		if (control instanceof Text) {
			Text text = (Text) control;
			if (caretPosition != null)
				if (caretPosition instanceof Integer)
					text.setSelection((Integer) caretPosition);
				else if (caretPosition instanceof Point) {
					// TODO find a way to position the caret at the right place
				}
			text.setData(RWT.ACTIVE_KEYS, new String[] { "BACKSPACE", "ESC",
					"TAB", "SHIFT+TAB", "ALT+ARROW_LEFT", "ALT+ARROW_RIGHT",
					"ALT+ARROW_UP", "ALT+ARROW_DOWN", "RETURN", "CTRL+RETURN",
					"ENTER", "DELETE" });
			text.setData(RWT.CANCEL_KEYS, new String[] { "ALT+ARROW_LEFT",
					"ALT+ARROW_RIGHT" });
			text.addKeyListener(this);
		} else if (part instanceof Img) {
			((Img) part).setFileUploadListener(fileUploadListener);
		}
	}

	// REQUIRED BY CONTEXT MENU
	void setParagraphStyle(Paragraph paragraph, String style) {
		try {
			Node paragraphNode = paragraph.getNode();
			paragraphNode.setProperty(CMS_STYLE, style);
			paragraphNode.getSession().save();
			updateContent(paragraph);
			layout(paragraph);
		} catch (RepositoryException e1) {
			throw new CmsException("Cannot set style " + style + " on "
					+ paragraph, e1);
		}
	}

	void deletePart(SectionPart paragraph) {
		try {
			Node paragraphNode = paragraph.getNode();
			Section section = paragraph.getSection();
			Session session = paragraphNode.getSession();
			paragraphNode.remove();
			session.save();
			if (paragraph instanceof Control)
				((Control) paragraph).dispose();
			layout(section);
		} catch (RepositoryException e1) {
			throw new CmsException("Cannot delete " + paragraph, e1);
		}
	}

	String getRawParagraphText(Paragraph paragraph) {
		return textInterpreter.raw(paragraph.getNode());
	}

	// COMMANDS
	protected void splitEdit() {
		checkEdited();
		try {
			if (getEdited() instanceof Paragraph) {
				Paragraph paragraph = (Paragraph) getEdited();
				Text text = (Text) paragraph.getControl();
				int caretPosition = text.getCaretPosition();
				String txt = text.getText();
				String first = txt.substring(0, caretPosition);
				String second = txt.substring(caretPosition);
				Node firstNode = paragraph.getNode();
				Node sectionNode = firstNode.getParent();
				firstNode.setProperty(CMS_CONTENT, first);
				Node secondNode = sectionNode.addNode(CMS_P);
				secondNode.addMixin(CmsTypes.CMS_STYLED);
				// second node was create as last, if it is not the next one, it
				// means there are some in between and we can take the one at
				// index+1 for the re-order
				if (secondNode.getIndex() > firstNode.getIndex() + 1) {
					sectionNode.orderBefore(p(secondNode.getIndex()),
							p(firstNode.getIndex() + 1));
				}

				// if we die in between, at least we still have the whole text
				// in the first node
				textInterpreter.write(secondNode, second);
				textInterpreter.write(firstNode, first);

				Paragraph secondParagraph = paragraphSplitted(paragraph,
						secondNode);
				edit(secondParagraph, 0);
			} else if (getEdited() instanceof SectionTitle) {
				SectionTitle sectionTitle = (SectionTitle) getEdited();
				Text text = (Text) sectionTitle.getControl();
				String txt = text.getText();
				int caretPosition = text.getCaretPosition();
				Section section = sectionTitle.getSection();
				Node sectionNode = section.getNode();
				Node paragraphNode = sectionNode.addNode(CMS_P);
				paragraphNode.addMixin(CmsTypes.CMS_STYLED);
				textInterpreter.write(paragraphNode,
						txt.substring(caretPosition));
				textInterpreter.write(
						sectionNode.getProperty(Property.JCR_TITLE),
						txt.substring(0, caretPosition));
				sectionNode.orderBefore(p(paragraphNode.getIndex()), p(1));
				sectionNode.getSession().save();

				Paragraph paragraph = sectionTitleSplitted(sectionTitle,
						paragraphNode);
				// section.layout();
				edit(paragraph, 0);
			}
		} catch (RepositoryException e) {
			throw new CmsException("Cannot split " + getEdited(), e);
		}
	}

	protected void mergeWithPrevious() {
		checkEdited();
		try {
			Paragraph paragraph = (Paragraph) getEdited();
			Text text = (Text) paragraph.getControl();
			String txt = text.getText();
			Node paragraphNode = paragraph.getNode();
			if (paragraphNode.getIndex() == 1)
				return;// do nothing
			Node sectionNode = paragraphNode.getParent();
			Node previousNode = sectionNode
					.getNode(p(paragraphNode.getIndex() - 1));
			String previousTxt = textInterpreter.read(previousNode);
			textInterpreter.write(previousNode, previousTxt + txt);
			paragraphNode.remove();
			sectionNode.getSession().save();

			Paragraph previousParagraph = paragraphMergedWithPrevious(
					paragraph, previousNode);
			edit(previousParagraph, previousTxt.length());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot stop editing", e);
		}
	}

	protected void mergeWithNext() {
		checkEdited();
		try {
			Paragraph paragraph = (Paragraph) getEdited();
			Text text = (Text) paragraph.getControl();
			String txt = text.getText();
			Node paragraphNode = paragraph.getNode();
			Node sectionNode = paragraphNode.getParent();
			NodeIterator paragraphNodes = sectionNode.getNodes(CMS_P);
			long size = paragraphNodes.getSize();
			if (paragraphNode.getIndex() == size)
				return;// do nothing
			Node nextNode = sectionNode
					.getNode(p(paragraphNode.getIndex() + 1));
			String nextTxt = textInterpreter.read(nextNode);
			textInterpreter.write(paragraphNode, txt + nextTxt);

			Section section = paragraph.getSection();
			Paragraph removed = (Paragraph) section.getSectionPart(nextNode
					.getIdentifier());

			nextNode.remove();
			sectionNode.getSession().save();

			paragraphMergedWithNext(paragraph, removed);
			edit(paragraph, txt.length());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot stop editing", e);
		}
	}

	protected synchronized void upload(EditablePart part) {
		try {
			if (part instanceof SectionPart) {
				SectionPart sectionPart = (SectionPart) part;
				Node partNode = sectionPart.getNode();
				int partIndex = partNode.getIndex();
				Section section = sectionPart.getSection();
				Node sectionNode = section.getNode();

				if (part instanceof Paragraph) {
					Node newNode = sectionNode.addNode(CMS_P, NodeType.NT_FILE);
					newNode.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
					JcrUtils.copyBytesAsFile(sectionNode,
							p(newNode.getIndex()), new byte[0]);
					if (partIndex < newNode.getIndex() - 1) {
						// was not last
						sectionNode.orderBefore(p(newNode.getIndex()),
								p(partIndex - 1));
					}
					// sectionNode.orderBefore(p(partNode.getIndex()),
					// p(newNode.getIndex()));
					sectionNode.getSession().save();
					Img img = newImg((TextSection) section, newNode);
					edit(img, null);
					layout(img.getControl());
				} else if (part instanceof Img) {
					if (getEdited() == part)
						return;
					edit(part, null);
					layout(part.getControl());
				}
			}
		} catch (RepositoryException e) {
			throw new CmsException("Cannot upload", e);
		}
	}

	protected void deepen() {
		if (flat)
			return;
		checkEdited();
		try {
			if (getEdited() instanceof Paragraph) {
				Paragraph paragraph = (Paragraph) getEdited();
				Text text = (Text) paragraph.getControl();
				String txt = text.getText();
				Node paragraphNode = paragraph.getNode();
				Section section = paragraph.getSection();
				Node sectionNode = section.getNode();
				// main title
				if (section == mainSection && section instanceof TextSection
						&& paragraphNode.getIndex() == 1
						&& !sectionNode.hasProperty(JCR_TITLE)) {
					SectionTitle sectionTitle = prepareSectionTitle(section,
							txt);
					edit(sectionTitle, 0);
					return;
				}
				Node newSectionNode = sectionNode.addNode(CMS_H,
						CmsTypes.CMS_SECTION);
				sectionNode.orderBefore(h(newSectionNode.getIndex()), h(1));

				int paragraphIndex = paragraphNode.getIndex();
				String sectionPath = sectionNode.getPath();
				String newSectionPath = newSectionNode.getPath();
				while (sectionNode.hasNode(p(paragraphIndex + 1))) {
					Node parag = sectionNode.getNode(p(paragraphIndex + 1));
					sectionNode.getSession().move(
							sectionPath + '/' + p(paragraphIndex + 1),
							newSectionPath + '/' + CMS_P);
					SectionPart sp = section.getSectionPart(parag
							.getIdentifier());
					if (sp instanceof Control)
						((Control) sp).dispose();
				}
				// create property
				newSectionNode.setProperty(Property.JCR_TITLE, "");
				getTextInterpreter().write(
						newSectionNode.getProperty(Property.JCR_TITLE), txt);

				TextSection newSection = new TextSection(section,
						section.getStyle(), newSectionNode);
				newSection.setLayoutData(CmsUtils.fillWidth());
				newSection.moveBelow(paragraph);

				// dispose
				paragraphNode.remove();
				paragraph.dispose();

				refresh(newSection);
				newSection.getParent().layout();
				layout(newSection);
				newSectionNode.getSession().save();
			} else if (getEdited() instanceof SectionTitle) {
				SectionTitle sectionTitle = (SectionTitle) getEdited();
				Section section = sectionTitle.getSection();
				Section parentSection = section.getParentSection();
				if (parentSection == null)
					return;// cannot deepen main section
				Node sectionN = section.getNode();
				Node parentSectionN = parentSection.getNode();
				if (sectionN.getIndex() == 1)
					return;// cannot deepen first section
				Node previousSectionN = parentSectionN.getNode(h(sectionN
						.getIndex() - 1));
				NodeIterator subSections = previousSectionN.getNodes(CMS_H);
				int subsectionsCount = (int) subSections.getSize();
				previousSectionN.getSession().move(
						sectionN.getPath(),
						previousSectionN.getPath() + "/"
								+ h(subsectionsCount + 1));
				section.dispose();
				TextSection newSection = new TextSection(section,
						section.getStyle(), sectionN);
				refresh(newSection);
				previousSectionN.getSession().save();
			}
		} catch (RepositoryException e) {
			throw new CmsException("Cannot deepen " + getEdited(), e);
		}
	}

	protected void undeepen() {
		if (flat)
			return;
		checkEdited();
		try {
			if (getEdited() instanceof Paragraph) {
				upload(getEdited());
			} else if (getEdited() instanceof SectionTitle) {
				SectionTitle sectionTitle = (SectionTitle) getEdited();
				Section section = sectionTitle.getSection();
				Node sectionNode = section.getNode();
				Section parentSection = section.getParentSection();
				if (parentSection == null)
					return;// cannot undeepen main section

				// choose in which section to merge
				Section mergedSection;
				if (sectionNode.getIndex() == 1)
					mergedSection = section.getParentSection();
				else {
					Map<String, Section> parentSubsections = parentSection
							.getSubSections();
					ArrayList<Section> lst = new ArrayList<Section>(
							parentSubsections.values());
					mergedSection = lst.get(sectionNode.getIndex() - 1);
				}
				Node mergedNode = mergedSection.getNode();
				boolean mergedHasSubSections = mergedNode.hasNode(CMS_H);

				// title as paragraph
				Node newParagrapheNode = mergedNode.addNode(CMS_P);
				newParagrapheNode.addMixin(CmsTypes.CMS_STYLED);
				if (mergedHasSubSections)
					mergedNode.orderBefore(p(newParagrapheNode.getIndex()),
							h(1));
				String txt = getTextInterpreter().read(
						sectionNode.getProperty(Property.JCR_TITLE));
				getTextInterpreter().write(newParagrapheNode, txt);
				// move
				NodeIterator paragraphs = sectionNode.getNodes(CMS_P);
				while (paragraphs.hasNext()) {
					Node p = paragraphs.nextNode();
					SectionPart sp = section.getSectionPart(p.getIdentifier());
					if (sp instanceof Control)
						((Control) sp).dispose();
					mergedNode.getSession().move(p.getPath(),
							mergedNode.getPath() + '/' + CMS_P);
					if (mergedHasSubSections)
						mergedNode.orderBefore(p(p.getIndex()), h(1));
				}

				Iterator<Section> subsections = section.getSubSections()
						.values().iterator();
				// NodeIterator sections = sectionNode.getNodes(CMS_H);
				while (subsections.hasNext()) {
					Section subsection = subsections.next();
					Node s = subsection.getNode();
					mergedNode.getSession().move(s.getPath(),
							mergedNode.getPath() + '/' + CMS_H);
					subsection.dispose();
				}

				// remove section
				section.getNode().remove();
				section.dispose();

				refresh(mergedSection);
				mergedSection.getParent().layout();
				layout(mergedSection);
				mergedNode.getSession().save();
			}
		} catch (RepositoryException e) {
			throw new CmsException("Cannot undeepen " + getEdited(), e);
		}
	}

	// UI CHANGES
	protected Paragraph paragraphSplitted(Paragraph paragraph, Node newNode)
			throws RepositoryException {
		Section section = paragraph.getSection();
		updateContent(paragraph);
		Paragraph newParagraph = newParagraph((TextSection) section, newNode);
		newParagraph.setLayoutData(CmsUtils.fillWidth());
		newParagraph.moveBelow(paragraph);
		layout(paragraph.getControl(), newParagraph.getControl());
		return newParagraph;
	}

	protected Paragraph sectionTitleSplitted(SectionTitle sectionTitle,
			Node newNode) throws RepositoryException {
		updateContent(sectionTitle);
		Paragraph newParagraph = newParagraph(sectionTitle.getSection(),
				newNode);
		// we assume beforeFirst is not null since there was a sectionTitle
		newParagraph.moveBelow(sectionTitle.getSection().getHeader());
		layout(sectionTitle.getControl(), newParagraph.getControl());
		return newParagraph;
	}

	protected Paragraph paragraphMergedWithPrevious(Paragraph removed,
			Node remaining) throws RepositoryException {
		Section section = removed.getSection();
		removed.dispose();

		Paragraph paragraph = (Paragraph) section.getSectionPart(remaining
				.getIdentifier());
		updateContent(paragraph);
		layout(paragraph.getControl());
		return paragraph;
	}

	protected void paragraphMergedWithNext(Paragraph remaining,
			Paragraph removed) throws RepositoryException {
		removed.dispose();
		updateContent(remaining);
		layout(remaining.getControl());
	}

	// UTILITIES
	protected String p(Integer index) {
		StringBuilder sb = new StringBuilder(6);
		sb.append(CMS_P).append('[').append(index).append(']');
		return sb.toString();
	}

	protected String h(Integer index) {
		StringBuilder sb = new StringBuilder(5);
		sb.append(CMS_H).append('[').append(index).append(']');
		return sb.toString();
	}

	// GETTERS / SETTERS
	public Section getMainSection() {
		return mainSection;
	}

	public boolean isFlat() {
		return flat;
	}

	public TextInterpreter getTextInterpreter() {
		return textInterpreter;
	}

	// KEY LISTENER
	@Override
	public void keyPressed(KeyEvent e) {
		if (log.isTraceEnabled())
			log.trace(e);

		if (getEdited() == null)
			return;
		boolean altPressed = (e.stateMask & SWT.ALT) != 0;
		boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
		boolean ctrlPressed = (e.stateMask & SWT.CTRL) != 0;

		// Common
		if (e.keyCode == SWT.ESC) {
			cancelEdit();
		} else if (e.character == '\r') {
			splitEdit();
		} else if (e.character == 'S') {
			if (ctrlPressed)
				saveEdit();
		} else if (e.character == '\t') {
			if (!shiftPressed) {
				deepen();
			} else if (shiftPressed) {
				undeepen();
			}
		} else {
			if (getEdited() instanceof Paragraph) {
				Paragraph paragraph = (Paragraph) getEdited();
				Section section = paragraph.getSection();
				if (altPressed && e.keyCode == SWT.ARROW_RIGHT) {
					edit(section.nextSectionPart(paragraph), 0);
				} else if (altPressed && e.keyCode == SWT.ARROW_LEFT) {
					edit(section.previousSectionPart(paragraph), 0);
				} else if (e.character == SWT.BS) {
					Text text = (Text) paragraph.getControl();
					int caretPosition = text.getCaretPosition();
					if (caretPosition == 0) {
						mergeWithPrevious();
					}
				} else if (e.character == SWT.DEL) {
					Text text = (Text) paragraph.getControl();
					int caretPosition = text.getCaretPosition();
					int charcount = text.getCharCount();
					if (caretPosition == charcount) {
						mergeWithNext();
					}
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	// MOUSE LISTENER
	@Override
	protected MouseListener createMouseListener() {
		return new ML();
	}

	private class ML extends MouseAdapter {
		private static final long serialVersionUID = 8526890859876770905L;

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (e.button == 1) {
				Control source = (Control) e.getSource();
				if (getCmsEditable().canEdit()) {
					if (getCmsEditable().isEditing()
							&& !(getEdited() instanceof Img)) {
						if (source == mainSection)
							return;
						EditablePart part = findDataParent(source);
						upload(part);
					} else {
						getCmsEditable().startEditing();
					}
				}
			}
		}

		@Override
		public void mouseDown(MouseEvent e) {
			if (getCmsEditable().isEditing()) {
				if (e.button == 1) {
					Control source = (Control) e.getSource();
					EditablePart composite = findDataParent(source);
					Point point = new Point(e.x, e.y);
					if (!(composite instanceof Img))
						edit(composite, source.toDisplay(point));
				} else if (e.button == 3) {
					EditablePart composite = findDataParent((Control) e
							.getSource());
					if (styledTools != null)
						styledTools.show(composite, new Point(e.x, e.y));
				}
			}
		}

		private EditablePart findDataParent(Control parent) {
			if (parent instanceof EditablePart) {
				return (EditablePart) parent;
			}
			if (parent.getParent() != null)
				return findDataParent(parent.getParent());
			else
				throw new CmsException("No data parent found");
		}

		@Override
		public void mouseUp(MouseEvent e) {
		}
	}

	// FILE UPLOAD LISTENER
	private class FUL implements FileUploadListener {
		public void uploadProgress(FileUploadEvent event) {
			// TODO Monitor upload progress
		}

		public void uploadFailed(FileUploadEvent event) {
			throw new CmsException("Upload failed " + event,
					event.getException());
		}

		public void uploadFinished(FileUploadEvent event) {
			for (FileDetails file : event.getFileDetails()) {
				if (log.isDebugEnabled())
					log.debug("Received: " + file.getFileName());
			}
			mainSection.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					saveEdit();
				}
			});
			FileUploadHandler uploadHandler = (FileUploadHandler) event
					.getSource();
			uploadHandler.dispose();
		}
	}
}