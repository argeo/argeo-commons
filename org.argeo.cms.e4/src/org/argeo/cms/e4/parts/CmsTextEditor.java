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

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsTypes;
import org.argeo.cms.text.StandardTextEditor;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.jcr.JcrUtils;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class CmsTextEditor implements Observer {
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
			Node textNode = JcrUtils.getOrAdd(session.getRootNode(), "text", CmsTypes.CMS_TEXT);
			cmsEditable = new JcrVersionCmsEditable(textNode);
			if (session.hasPendingChanges())
				session.save();
			cmsEditable.addObserver(this);
			StandardTextEditor textEditor = new StandardTextEditor(parent, SWT.NONE, textNode, cmsEditable);
			mpart.setDirty(cmsEditable.isEditing());
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
