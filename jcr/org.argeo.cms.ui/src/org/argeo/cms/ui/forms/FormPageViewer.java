package org.argeo.cms.ui.forms;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.api.cms.ux.CmsEditable;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.viewers.AbstractPageViewer;
import org.argeo.cms.ui.viewers.EditablePart;
import org.argeo.cms.ui.viewers.Section;
import org.argeo.cms.ui.viewers.SectionPart;
import org.argeo.cms.ui.widgets.EditableImage;
import org.argeo.cms.ui.widgets.Img;
import org.argeo.cms.ui.widgets.StyledControl;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadEvent;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadListener;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Manage life cycle of a form page that is linked to a given node */
public class FormPageViewer extends AbstractPageViewer {
	private final static CmsLog log = CmsLog.getLog(FormPageViewer.class);
	private static final long serialVersionUID = 5277789504209413500L;

	private final Section mainSection;

	// TODO manage within the CSS
	private Integer labelColWidth = null;
	private int rowLayoutHSpacing = 8;

	// Context cached in the viewer
	// The reference to translate from text to calendar and reverse
	private DateFormat dateFormat = new SimpleDateFormat(FormUtils.DEFAULT_SHORT_DATE_FORMAT);
	private CmsImageManager<Control, Node> imageManager;
	private FileUploadListener fileUploadListener;

	public FormPageViewer(Section mainSection, int style, CmsEditable cmsEditable) throws RepositoryException {
		super(mainSection, style, cmsEditable);
		this.mainSection = mainSection;

		if (getCmsEditable().canEdit()) {
			fileUploadListener = new FUL();
		}
	}

	@Override
	protected void prepare(EditablePart part, Object caretPosition) {
		if (part instanceof Img) {
			((Img) part).setFileUploadListener(fileUploadListener);
		}
	}

	/** To be overridden.Save the edited part. */
	protected void save(EditablePart part) throws RepositoryException {
		Node node = null;
		if (part instanceof EditableMultiStringProperty) {
			EditableMultiStringProperty ept = (EditableMultiStringProperty) part;
			// SWT : View
			List<String> values = ept.getValues();
			// JCR : Model
			node = ept.getNode();
			String propName = ept.getPropertyName();
			if (values.isEmpty()) {
				if (node.hasProperty(propName))
					node.getProperty(propName).remove();
			} else {
				node.setProperty(propName, values.toArray(new String[0]));
			}
			// => Viewer : Controller
		} else if (part instanceof EditablePropertyString) {
			EditablePropertyString ept = (EditablePropertyString) part;
			// SWT : View
			String txt = ((Text) ept.getControl()).getText();
			// JCR : Model
			node = ept.getNode();
			String propName = ept.getPropertyName();
			if (EclipseUiUtils.isEmpty(txt)) {
				if (node.hasProperty(propName))
					node.getProperty(propName).remove();
			} else {
				setPropertySilently(node, propName, txt);
				// node.setProperty(propName, txt);
			}
			// node.getSession().save();
			// => Viewer : Controller
		} else if (part instanceof EditablePropertyDate) {
			EditablePropertyDate ept = (EditablePropertyDate) part;
			Calendar cal = FormUtils.parseDate(dateFormat, ((Text) ept.getControl()).getText());
			node = ept.getNode();
			String propName = ept.getPropertyName();
			if (cal == null) {
				if (node.hasProperty(propName))
					node.getProperty(propName).remove();
			} else {
				node.setProperty(propName, cal);
			}
			// node.getSession().save();
			// => Viewer : Controller
		}
		// TODO: make this configurable, sometimes we do not want to save the
		// current session at this stage
		if (node != null && node.getSession().hasPendingChanges()) {
			JcrUtils.updateLastModified(node, true);
			node.getSession().save();
		}
	}

	@Override
	protected void updateContent(EditablePart part) throws RepositoryException {
		if (part instanceof EditableMultiStringProperty) {
			EditableMultiStringProperty ept = (EditableMultiStringProperty) part;
			// SWT : View
			Node node = ept.getNode();
			String propName = ept.getPropertyName();
			List<String> valStrings = new ArrayList<String>();
			if (node.hasProperty(propName)) {
				Value[] values = node.getProperty(propName).getValues();
				for (Value val : values)
					valStrings.add(val.getString());
			}
			ept.setValues(valStrings);
		} else if (part instanceof EditablePropertyString) {
			// || part instanceof EditableLink
			EditablePropertyString ept = (EditablePropertyString) part;
			// JCR : Model
			Node node = ept.getNode();
			String propName = ept.getPropertyName();
			if (node.hasProperty(propName)) {
				String value = node.getProperty(propName).getString();
				ept.setText(value);
			} else
				ept.setText("");
			// => Viewer : Controller
		} else if (part instanceof EditablePropertyDate) {
			EditablePropertyDate ept = (EditablePropertyDate) part;
			// JCR : Model
			Node node = ept.getNode();
			String propName = ept.getPropertyName();
			if (node.hasProperty(propName))
				ept.setText(dateFormat.format(node.getProperty(propName).getDate().getTime()));
			else
				ept.setText("");
		} else if (part instanceof SectionPart) {
			SectionPart sectionPart = (SectionPart) part;
			Node partNode = sectionPart.getNode();
			// use control AFTER setting style, since it may have been reset
			if (part instanceof EditableImage) {
				EditableImage editableImage = (EditableImage) part;
				imageManager().load(partNode, part.getControl(), editableImage.getPreferredImageSize());
			}
		}
	}

	// FILE UPLOAD LISTENER
	protected class FUL implements FileUploadListener {

		public FUL() {
		}

		public void uploadProgress(FileUploadEvent event) {
			// TODO Monitor upload progress
		}

		public void uploadFailed(FileUploadEvent event) {
			throw new IllegalStateException("Upload failed " + event, event.getException());
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
			FileUploadHandler uploadHandler = (FileUploadHandler) event.getSource();
			uploadHandler.dispose();
		}
	}

	// FOCUS OUT LISTENER
	protected FocusListener createFocusListener() {
		return new FocusOutListener();
	}

	private class FocusOutListener implements FocusListener {
		private static final long serialVersionUID = -6069205786732354186L;

		@Override
		public void focusLost(FocusEvent event) {
			saveEdit();
		}

		@Override
		public void focusGained(FocusEvent event) {
			// does nothing;
		}
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
					if (getCmsEditable().isEditing() && !(getEdited() instanceof Img)) {
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
					// EditablePart composite = findDataParent((Control) e
					// .getSource());
					// if (styledTools != null)
					// styledTools.show(composite, new Point(e.x, e.y));
				}
			}
		}

		protected synchronized void upload(EditablePart part) {
			if (part instanceof SectionPart) {
				if (part instanceof Img) {
					if (getEdited() == part)
						return;
					edit(part, null);
					layout(part.getControl());
				}
			}
		}
	}

	@Override
	public Control getControl() {
		return mainSection;
	}

	protected CmsImageManager<Control, Node> imageManager() {
		if (imageManager == null)
			imageManager = (CmsImageManager<Control, Node>) CmsSwtUtils.getCmsView(mainSection).getImageManager();
		return imageManager;
	}

	// LOCAL UI HELPERS
	protected Section createSectionIfNeeded(Composite body, Node node) throws RepositoryException {
		Section section = null;
		if (node != null) {
			section = new Section(body, SWT.NO_FOCUS, node);
			section.setLayoutData(CmsSwtUtils.fillWidth());
			section.setLayout(CmsSwtUtils.noSpaceGridLayout());
		}
		return section;
	}

	protected void createSimpleLT(Composite bodyRow, Node node, String propName, String label, String msg)
			throws RepositoryException {
		if (getCmsEditable().canEdit() || node.hasProperty(propName)) {
			createPropertyLbl(bodyRow, label);
			EditablePropertyString eps = new EditablePropertyString(bodyRow, SWT.WRAP | SWT.LEFT, node, propName, msg);
			eps.setMouseListener(getMouseListener());
			eps.setFocusListener(getFocusListener());
			eps.setLayoutData(CmsSwtUtils.fillWidth());
		}
	}

	protected void createMultiStringLT(Composite bodyRow, Node node, String propName, String label, String msg)
			throws RepositoryException {
		boolean canEdit = getCmsEditable().canEdit();
		if (canEdit || node.hasProperty(propName)) {
			createPropertyLbl(bodyRow, label);

			List<String> valueStrings = new ArrayList<String>();

			if (node.hasProperty(propName)) {
				Value[] values = node.getProperty(propName).getValues();
				for (Value value : values)
					valueStrings.add(value.getString());
			}

			// TODO use a drop down to display possible values to the end user
			EditableMultiStringProperty emsp = new EditableMultiStringProperty(bodyRow, SWT.SINGLE | SWT.LEAD, node,
					propName, valueStrings, new String[] { "Implement this" }, msg,
					canEdit ? getRemoveValueSelListener() : null);
			addListeners(emsp);
			// emsp.setMouseListener(getMouseListener());
			emsp.setStyle(FormStyle.propertyMessage.style());
			emsp.setLayoutData(CmsSwtUtils.fillWidth());
		}
	}

	protected Label createPropertyLbl(Composite parent, String value) {
		return createPropertyLbl(parent, value, SWT.NONE);
	}

	protected Label createPropertyLbl(Composite parent, String value, int vAlign) {
		// boolean isSmall = CmsView.getCmsView(parent).getUxContext().isSmall();
		Label label = new Label(parent, SWT.LEAD | SWT.WRAP);
		label.setText(value + " ");
		CmsSwtUtils.style(label, FormStyle.propertyLabel.style());
		GridData gd = new GridData(SWT.LEAD, vAlign, false, false);
		if (labelColWidth != null)
			gd.widthHint = labelColWidth;
		label.setLayoutData(gd);
		return label;
	}

	protected Label newStyledLabel(Composite parent, String style, String value) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(value);
		CmsSwtUtils.style(label, style);
		return label;
	}

	protected Composite createRowLayoutComposite(Composite parent) throws RepositoryException {
		Composite bodyRow = new Composite(parent, SWT.NO_FOCUS);
		bodyRow.setLayoutData(CmsSwtUtils.fillWidth());
		RowLayout rl = new RowLayout(SWT.WRAP);
		rl.type = SWT.HORIZONTAL;
		rl.spacing = rowLayoutHSpacing;
		rl.marginHeight = rl.marginWidth = 0;
		rl.marginTop = rl.marginBottom = rl.marginLeft = rl.marginRight = 0;
		bodyRow.setLayout(rl);
		return bodyRow;
	}

	protected Composite createAddImgComposite(final Section section, Composite parent, final Node parentNode)
			throws RepositoryException {

		Composite body = new Composite(parent, SWT.NO_FOCUS);
		body.setLayout(new GridLayout());

		FormFileUploadReceiver receiver = new FormFileUploadReceiver(section, parentNode, null);
		final FileUploadHandler currentUploadHandler = new FileUploadHandler(receiver);
		if (fileUploadListener != null)
			currentUploadHandler.addUploadListener(fileUploadListener);

		// Button creation
		final FileUpload fileUpload = new FileUpload(body, SWT.BORDER);
		fileUpload.setText("Import an image");
		fileUpload.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		fileUpload.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = 4869523412991968759L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				ServerPushSession pushSession = new ServerPushSession();
				pushSession.start();
				String uploadURL = currentUploadHandler.getUploadUrl();
				fileUpload.submit(uploadURL);
			}
		});

		return body;
	}

	protected class FormFileUploadReceiver extends FileUploadReceiver {

		private Node context;
		private Section section;
		private String name;

		public FormFileUploadReceiver(Section section, Node context, String name) {
			this.context = context;
			this.section = section;
			this.name = name;
		}

		@Override
		public void receive(InputStream stream, FileDetails details) throws IOException {

			if (name == null)
				name = details.getFileName();

			// TODO clean image name more carefully
			String cleanedName = name.replaceAll("[^a-zA-Z0-9-.]", "");
			// We add a unique prefix to workaround the cache issue: when
			// deleting and re-adding a new image with same name, the end user
			// browser will use the cache and the image will remain unchanged
			// for a while
			cleanedName = System.currentTimeMillis() % 100000 + "_" + cleanedName;

			imageManager().uploadImage(context, context, cleanedName, stream, details.getContentType());
			// TODO clean refresh strategy
			section.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						FormPageViewer.this.refresh(section);
						section.layout();
						section.getParent().layout();
					} catch (RepositoryException re) {
						throw new JcrException("Unable to refresh " + "image section for " + context, re);
					}
				}
			});
		}
	}

	protected void addListeners(StyledControl control) {
		control.setMouseListener(getMouseListener());
		control.setFocusListener(getFocusListener());
	}

	protected Img createImgComposite(Composite parent, Node node, Point preferredSize) throws RepositoryException {
		Img img = new Img(parent, SWT.NONE, node, new Cms2DSize(preferredSize.x, preferredSize.y)) {
			private static final long serialVersionUID = 1297900641952417540L;

			@Override
			protected void setContainerLayoutData(Composite composite) {
				composite.setLayoutData(CmsSwtUtils.grabWidth(SWT.CENTER, SWT.DEFAULT));
			}

			@Override
			protected void setControlLayoutData(Control control) {
				control.setLayoutData(CmsSwtUtils.grabWidth(SWT.CENTER, SWT.DEFAULT));
			}
		};
		img.setLayoutData(CmsSwtUtils.grabWidth(SWT.CENTER, SWT.DEFAULT));
		updateContent(img);
		addListeners(img);
		return img;
	}

	protected Composite addDeleteAbility(final Section section, final Node sessionNode, int topWeight,
			int rightWeight) {
		Composite comp = new Composite(section, SWT.NONE);
		comp.setLayoutData(CmsSwtUtils.fillAll());
		comp.setLayout(new FormLayout());

		// The body to be populated
		Composite body = new Composite(comp, SWT.NO_FOCUS);
		body.setLayoutData(EclipseUiUtils.fillFormData());

		if (getCmsEditable().canEdit()) {
			// the delete button
			Button deleteBtn = new Button(comp, SWT.FLAT);
			CmsSwtUtils.style(deleteBtn, FormStyle.deleteOverlay.style());
			FormData formData = new FormData();
			formData.right = new FormAttachment(rightWeight, 0);
			formData.top = new FormAttachment(topWeight, 0);
			deleteBtn.setLayoutData(formData);
			deleteBtn.moveAbove(body);

			deleteBtn.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 4304223543657238462L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (MessageDialog.openConfirm(section.getShell(), "Confirm deletion",
							"Are you really you want to remove this?")) {
						Session session;
						try {
							session = sessionNode.getSession();
							Section parSection = section.getParentSection();
							sessionNode.remove();
							session.save();
							refresh(parSection);
							layout(parSection);
						} catch (RepositoryException re) {
							throw new JcrException("Unable to delete " + sessionNode, re);
						}

					}

				}
			});
		}
		return body;
	}

//	// LOCAL HELPERS FOR NODE MANAGEMENT
//	private Node getOrCreateNode(Node parent, String nodeName, String nodeType) throws RepositoryException {
//		Node node = null;
//		if (getCmsEditable().canEdit() && !parent.hasNode(nodeName)) {
//			node = JcrUtils.mkdirs(parent, nodeName, nodeType);
//			parent.getSession().save();
//		}
//
//		if (getCmsEditable().canEdit() || parent.hasNode(nodeName))
//			node = parent.getNode(nodeName);
//
//		return node;
//	}

	private SelectionListener getRemoveValueSelListener() {
		return new SelectionAdapter() {
			private static final long serialVersionUID = 9022259089907445195L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				Object source = e.getSource();
				if (source instanceof Button) {
					Button btn = (Button) source;
					Object obj = btn.getData(FormConstants.LINKED_VALUE);
					EditablePart ep = findDataParent(btn);
					if (ep != null && ep instanceof EditableMultiStringProperty) {
						EditableMultiStringProperty emsp = (EditableMultiStringProperty) ep;
						List<String> values = emsp.getValues();
						if (values.contains(obj)) {
							values.remove(values.indexOf(obj));
							emsp.setValues(values);
							try {
								save(emsp);
								// TODO workaround to force refresh
								edit(emsp, 0);
								cancelEdit();
							} catch (RepositoryException e1) {
								throw new JcrException("Unable to remove value " + obj, e1);
							}
							layout(emsp);
						}
					}
				}
			}
		};
	}

	protected void setPropertySilently(Node node, String propName, String value) throws RepositoryException {
		try {
			// TODO Clean this:
			// Format strings to replace \n
			value = value.replaceAll("\n", "<br/>");
			// Do not make the update if validation fails
			try {
				MarkupValidatorCopy.getInstance().validate(value);
			} catch (Exception e) {
				log.warn("Cannot set [" + value + "] on prop " + propName + "of " + node
						+ ", String cannot be validated - " + e.getMessage());
				return;
			}
			// TODO check if the newly created property is of the correct type,
			// otherwise the property will be silently created with a STRING
			// property type.
			node.setProperty(propName, value);
		} catch (ValueFormatException vfe) {
			log.warn("Cannot set [" + value + "] on prop " + propName + "of " + node + " - " + vfe.getMessage());
		}
	}
}