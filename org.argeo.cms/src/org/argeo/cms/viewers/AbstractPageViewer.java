package org.argeo.cms.viewers;

import java.util.Observable;
import java.util.Observer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsException;
import org.argeo.cms.widgets.ScrolledPage;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
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
public abstract class AbstractPageViewer extends ContentViewer implements
		Observer {
	private static final long serialVersionUID = 5438688173410341485L;

	private final static Log log = LogFactory.getLog(AbstractPageViewer.class);

	private final boolean readOnly;
	/** The basis for the layouts, typically a ScrolledPage. */
	private final Composite page;
	private final CmsEditable cmsEditable;

	private MouseListener mouseListener;
	private FocusListener focusListener;

	private EditablePart edited;
	private ISelection selection = StructuredSelection.EMPTY;

	protected AbstractPageViewer(Section parent, int style,
			CmsEditable cmsEditable) {
		// read only at UI level
		readOnly = SWT.READ_ONLY == (style & SWT.READ_ONLY);

		this.cmsEditable = cmsEditable == null ? CmsEditable.NON_EDITABLE
				: cmsEditable;
		if (this.cmsEditable instanceof Observable)
			((Observable) this.cmsEditable).addObserver(this);

		if (cmsEditable.canEdit()) {
			mouseListener = createMouseListener();
			focusListener = createFocusListener();
		}
		page = findPage(parent);
	}

	/**
	 * Can be called to simplify the called to isModelInitialized() and
	 * initModel()
	 */
	protected void initModelIfNeeded(Node node) {
		try {
			if (!isModelInitialized(node))
				if (getCmsEditable().canEdit()) {
					initModel(node);
					node.getSession().save();
				}
		} catch (Exception e) {
			throw new CmsException("Cannot initialize model", e);
		}
	}

	/** Called if user can edit and model is not initialized */
	protected Boolean isModelInitialized(Node node) throws RepositoryException {
		return true;
	}

	/** Called if user can edit and model is not initialized */
	protected void initModel(Node node) throws RepositoryException {
	}

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

	@Override
	public void update(Observable o, Object arg) {
		if (o == cmsEditable)
			editingStateChanged(cmsEditable);
	}

	/** To be overridden in order to provide the actual refresh */
	protected void refresh(Control control) throws RepositoryException {
	}

	/** To be overridden.Save the edited part. */
	protected void save(EditablePart part) throws RepositoryException {
	}

	/** Prepare the edited part */
	protected void prepare(EditablePart part, Object caretPosition) {
	}

	/** Notified when the editing state changed. Does nothing, to be overridden */
	protected void editingStateChanged(CmsEditable cmsEditable) {
	}

	@Override
	public void refresh() {
		try {
			if (cmsEditable.canEdit() && !readOnly)
				mouseListener = createMouseListener();
			else
				mouseListener = null;
			refresh(getControl());
			layout(getControl());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot refresh", e);
		}
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		this.selection = selection;
	}

	protected void updateContent(EditablePart part) throws RepositoryException {
	}

	// LOW LEVEL EDITION
	protected void edit(EditablePart part, Object caretPosition) {
		try {
			if (edited == part)
				return;

			if (edited != null && edited != part) {
				EditablePart previouslyEdited = edited;
				try {
					stopEditing(true);
				} catch (Exception e) {
					notifyEditionException(e);
					edit(previouslyEdited, caretPosition);
					return;
				}
			}

			part.startEditing();
			updateContent(part);
			prepare(part, caretPosition);
			edited = part;
			layout(part.getControl());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot edit " + part, e);
		}
	}

	private void stopEditing(Boolean save) throws RepositoryException {
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

		if (save)
			save(edited);

		edited.stopEditing();
		updateContent(edited);
		layout(((EditablePart) edited).getControl());
		edited = null;
	}

	// METHODS AVAILABLE TO EXTENDING CLASSES
	protected void saveEdit() {
		try {
			if (edited != null)
				stopEditing(true);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot stop editing", e);
		}
	}

	protected void cancelEdit() {
		try {
			if (edited != null)
				stopEditing(false);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot cancel editing", e);
		}
	}

	/** Layout this controls from the related base page. */
	public void layout(Control... controls) {
		page.layout(controls);
	}

	/**
	 * Find the first {@link EditablePart} in the parents hierarchy of this
	 * control
	 */
	protected EditablePart findDataParent(Control parent) {
		if (parent instanceof EditablePart) {
			return (EditablePart) parent;
		}
		if (parent.getParent() != null)
			return findDataParent(parent.getParent());
		else
			throw new CmsException("No data parent found");
	}

	// UTILITIES
	/** Check whether the edited part is in a proper state */
	protected void checkEdited() {
		if (edited == null || (edited instanceof Widget)
				&& ((Widget) edited).isDisposed())
			throw new CmsException(
					"Edited should not be null or disposed at this stage");
	}

	/** Persist all changes. */
	protected void persistChanges(Session session) throws RepositoryException {
		session.save();
		session.refresh(false);
		// TODO notify that changes have been persisted
	}

	/** Convenience method using a Node in order to save the underlying session. */
	protected void persistChanges(Node anyNode) throws RepositoryException {
		persistChanges(anyNode.getSession());
	}

	/** Notify edition exception */
	protected void notifyEditionException(Throwable e) {
		Throwable eToLog = e;
		if (e instanceof IllegalArgumentException)
			if (e.getCause() instanceof SAXParseException)
				eToLog = e.getCause();
		log.error(eToLog.getMessage());
		if (log.isTraceEnabled())
			log.trace("Full stack of " + eToLog.getMessage(), e);
		// TODO Light error notification popup
	}

	// GETTERS / SETTERS
	public boolean isReadOnly() {
		return readOnly;
	}

	protected EditablePart getEdited() {
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

	@Override
	public ISelection getSelection() {
		return selection;
	}
}