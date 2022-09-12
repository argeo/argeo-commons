package org.argeo.cms.swt.acr;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsEditable;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.swt.SwtEditablePart;
import org.argeo.cms.swt.widgets.ScrolledPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.xml.sax.SAXParseException;

/** Base class for viewers related to a page */
public abstract class AbstractPageViewer {

	private final static CmsLog log = CmsLog.getLog(AbstractPageViewer.class);

	private final boolean readOnly;
	/** The basis for the layouts, typically a ScrolledPage. */
	private final Composite page;
	private final CmsEditable cmsEditable;

	private MouseListener mouseListener;
	private FocusListener focusListener;

	private SwtEditablePart edited;
//	private ISelection selection = StructuredSelection.EMPTY;

	private Subject viewerSubject;

	protected AbstractPageViewer(Composite parent, int style, CmsEditable cmsEditable) {
		// read only at UI level
		readOnly = SWT.READ_ONLY == (style & SWT.READ_ONLY);

		this.cmsEditable = cmsEditable == null ? CmsEditable.NON_EDITABLE : cmsEditable;
//		if (this.cmsEditable instanceof Observable)
//			((Observable) this.cmsEditable).addObserver(this);

		if (cmsEditable.canEdit()) {
			mouseListener = createMouseListener();
			focusListener = createFocusListener();
		}
		page = findPage(parent);
//		accessControlContext = AccessController.getContext();
		viewerSubject = CurrentUser.getCmsSession().getSubject();
	}

	public abstract Control getControl();

//	/**
//	 * Can be called to simplify the called to isModelInitialized() and initModel()
//	 */
//	protected void initModelIfNeeded(Node node) {
//		try {
//			if (!isModelInitialized(node))
//				if (getCmsEditable().canEdit()) {
//					initModel(node);
//					node.getSession().save();
//				}
//		} catch (RepositoryException e) {
//			throw new JcrException("Cannot initialize model", e);
//		}
//	}
//
//	/** Called if user can edit and model is not initialized */
//	protected Boolean isModelInitialized(Node node) throws RepositoryException {
//		return true;
//	}
//
//	/** Called if user can edit and model is not initialized */
//	protected void initModel(Node node) throws RepositoryException {
//	}

	/** Create (retrieve) the MouseListener to use. */
	protected MouseListener createMouseListener() {
		return new MouseAdapter() {
			private static final long serialVersionUID = 1L;
		};
	}

	/** Create (retrieve) the FocusListener to use. */
	protected FocusListener createFocusListener() {
		return new FocusListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void focusLost(FocusEvent event) {
			}

			@Override
			public void focusGained(FocusEvent event) {
			}
		};
	}

	protected Composite findPage(Composite composite) {
		if (composite instanceof ScrolledPage) {
			return (ScrolledPage) composite;
		} else {
			if (composite.getParent() == null)
				return composite;
			return findPage(composite.getParent());
		}
	}

	public void layoutPage() {
		if (page != null)
			page.layout(true, true);
	}

	protected void showControl(Control control) {
		if (page != null && (page instanceof ScrolledPage))
			((ScrolledPage) page).showControl(control);
	}

//	@Override
//	public void update(Observable o, Object arg) {
//		if (o == cmsEditable)
//			editingStateChanged(cmsEditable);
//	}

	/** To be overridden in order to provide the actual refresh */
	protected void refresh(Control control) {
	}

	/** To be overridden.Save the edited part. */
	protected void save(SwtEditablePart part) {
	}

	/** Prepare the edited part */
	protected void prepare(SwtEditablePart part, Object caretPosition) {
	}

	/** Notified when the editing state changed. Does nothing, to be overridden */
	protected void editingStateChanged(CmsEditable cmsEditable) {
	}

	public void refresh() {
		// TODO check actual context in order to notice a discrepancy
		Subject viewerSubject = getViewerSubject();
		Subject.doAs(viewerSubject, (PrivilegedAction<Void>) () -> {
			if (cmsEditable.canEdit() && !readOnly)
				mouseListener = createMouseListener();
			else
				mouseListener = null;
			refresh(getControl());
			// layout(getControl());
			if (!getControl().isDisposed())
				layoutPage();
			return null;
		});
	}

//	@Override
//	public void setSelection(ISelection selection, boolean reveal) {
//		this.selection = selection;
//	}

	protected void updateContent(SwtEditablePart part) {
	}

	// LOW LEVEL EDITION
	protected void edit(SwtEditablePart part, Object caretPosition) {
		if (edited == part)
			return;

		if (edited != null && edited != part) {
			SwtEditablePart previouslyEdited = edited;
			try {
				stopEditing(true);
			} catch (Exception e) {
				notifyEditionException(e);
				edit(previouslyEdited, caretPosition);
				return;
			}
		}

		part.startEditing();
		edited = part;
		updateContent(part);
		prepare(part, caretPosition);
		edited.getControl().addFocusListener(new FocusListener() {
			private static final long serialVersionUID = 6883521812717097017L;

			@Override
			public void focusLost(FocusEvent event) {
				stopEditing(true);
			}

			@Override
			public void focusGained(FocusEvent event) {
			}
		});

		layout(part.getControl());
		showControl(part.getControl());
	}

	protected void stopEditing(Boolean save) {
		if (edited instanceof Widget && ((Widget) edited).isDisposed()) {
			edited = null;
			return;
		}

		assert edited != null;
		if (edited == null) {
			if (log.isTraceEnabled())
				log.warn("Told to stop editing while not editing anything");
			return;
		}

		try {
			if (save)
				save(edited);

			edited.stopEditing();
			SwtEditablePart editablePart = edited;
			Control control = ((SwtEditablePart) edited).getControl();
			edited = null;
			// TODO make edited state management more robust
			updateContent(editablePart);
			layout(control);
		} finally {
			edited = null;
		}
	}

	// METHODS AVAILABLE TO EXTENDING CLASSES
	protected void saveEdit() {
		if (edited != null)
			stopEditing(true);
	}

	protected void cancelEdit() {
		if (edited != null)
			stopEditing(false);
	}

	/** Layout this controls from the related base page. */
	public void layout(Control... controls) {
		page.layout(controls);
	}

	/**
	 * Find the first {@link SwtEditablePart} in the parents hierarchy of this
	 * control
	 */
	protected SwtEditablePart findDataParent(Control parent) {
		if (parent instanceof SwtEditablePart) {
			return (SwtEditablePart) parent;
		}
		if (parent.getParent() != null)
			return findDataParent(parent.getParent());
		else
			throw new IllegalStateException("No data parent found");
	}

	// UTILITIES
	/** Check whether the edited part is in a proper state */
	protected void checkEdited() {
		if (edited == null || (edited instanceof Widget) && ((Widget) edited).isDisposed())
			throw new IllegalStateException("Edited should not be null or disposed at this stage");
	}

	/** Persist all changes. */
	protected void persistChanges(ContentSession session) {
//		session.save();
//		session.refresh(false);
		// TODO notify that changes have been persisted
	}

	/** Convenience method using a Node in order to save the underlying session. */
	protected void persistChanges(Content anyNode) {
		persistChanges(((ProvidedContent) anyNode).getSession());
	}

	/** Notify edition exception */
	protected void notifyEditionException(Throwable e) {
		Throwable eToLog = e;
		if (e instanceof IllegalArgumentException)
			if (e.getCause() instanceof SAXParseException)
				eToLog = e.getCause();
		log.error(eToLog.getMessage(), eToLog);
//		if (log.isTraceEnabled())
//			log.trace("Full stack of " + eToLog.getMessage(), e);
		// TODO Light error notification popup
	}

	protected Subject getViewerSubject() {
		return viewerSubject;
//		Subject res = null;
//		if (accessControlContext != null) {
//			res = Subject.getSubject(accessControlContext);
//		}
//		if (res == null)
//			throw new IllegalStateException("No subject associated with this viewer");
//		return res;
	}

	// GETTERS / SETTERS
	public boolean isReadOnly() {
		return readOnly;
	}

	protected SwtEditablePart getEdited() {
		return edited;
	}

	public MouseListener getMouseListener() {
		return mouseListener;
	}

	public FocusListener getFocusListener() {
		return focusListener;
	}

	public CmsEditable getCmsEditable() {
		return cmsEditable;
	}

//	@Override
//	public ISelection getSelection() {
//		return selection;
//	}
}