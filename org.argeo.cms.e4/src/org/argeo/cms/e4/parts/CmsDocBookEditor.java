package org.argeo.cms.e4.parts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.argeo.cms.CmsException;
import org.argeo.cms.text.DocumentTextEditor;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.docbook.DocBookNames;
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
			// session = repository.login();
			session = JcrUtils.loginOrCreateWorkspace(repository, "example");
//			Node textNode = JcrUtils.getOrAdd(session.getRootNode(), "article", DocBookTypes.ARTICLE);
//			if (textNode.isCheckedOut())
//				textNode.addMixin(NodeType.MIX_TITLE);

			String textNodeName = "docbook";
			if (session.getRootNode().hasNode(textNodeName))
				session.getRootNode().getNode(textNodeName).remove();

			Node textNode = JcrUtils.getOrAdd(session.getRootNode(), textNodeName, DocBookTypes.BOOK);
			Map<String, String> properties = mpart.getProperties();
			String defaultContentUri = properties.get("defaultContentUri");
			if (textNode.hasNode(DocBookNames.DBK_ARTICLE))
				textNode.getNode(DocBookNames.DBK_ARTICLE).remove();
			if (defaultContentUri != null && !textNode.hasNode(DocBookNames.DBK_ARTICLE))
				try {
					URL url = new URL(defaultContentUri);
					try (InputStream in = url.openStream()) {
						session.importXML(textNode.getPath(), in,
								ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
					}
					Node dbkNode = textNode.getNode(DocBookNames.DBK_ARTICLE);
					if (dbkNode.isNodeType(DocBookTypes.ARTICLE))
						System.out.println(dbkNode + " is an article");
				} catch (IOException e) {
					e.printStackTrace();
				}

			cmsEditable = new JcrVersionCmsEditable(textNode);
			if (session.hasPendingChanges())
				session.save();
			cmsEditable.addObserver(this);
			DocumentTextEditor textEditor = new DocumentTextEditor(parent, SWT.NONE,
					textNode.getNode(DocBookNames.DBK_ARTICLE), cmsEditable);
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
