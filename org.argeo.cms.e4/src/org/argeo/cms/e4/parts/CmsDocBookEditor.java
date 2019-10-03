package org.argeo.cms.e4.parts;

import java.util.Observable;
import java.util.Observer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.argeo.cms.CmsException;
import org.argeo.cms.text.DocumentTextEditor;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.docbook.DocBookTypes;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class CmsDocBookEditor implements Observer {
	@Inject
	Repository repository;

	@Inject
	private MPart mpart;

	Session session;
	JcrVersionCmsEditable cmsEditable;

	@PostConstruct
	public void createUI(Composite parent) {
		try {
			parent.setLayout(new GridLayout());
			session = repository.login();
			JcrUtils.loginOrCreateWorkspace(repository, "demo");
			Node textNode = JcrUtils.getOrAdd(session.getRootNode(), "article", DocBookTypes.ARTICLE);
			if (textNode.isCheckedOut())
				textNode.addMixin(NodeType.MIX_TITLE);
			cmsEditable = new JcrVersionCmsEditable(textNode);
			if (session.hasPendingChanges())
				session.save();
			cmsEditable.addObserver(this);
			DocumentTextEditor textEditor = new DocumentTextEditor(parent, SWT.NONE, textNode, cmsEditable);
			mpart.setDirty(isDirty());
		} catch (RepositoryException e) {
			throw new CmsException("Cannot create text editor", e);
		}
	}

	@PreDestroy
	public void dispose() {
		JcrUtils.logoutQuietly(session);
	}

	@Persist
	public void save() {
		try {
			session.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot save", e);
		}
		cmsEditable.stopEditing();
	}

	@Override
	public void update(Observable o, Object arg) {
		// CmsEditable cmsEditable = (CmsEditable) o;
		mpart.setDirty(isDirty());
	}

	boolean isDirty() {
		return cmsEditable.isEditing();
	}

}
