package org.argeo.eclipse.ui.jcr.editors;

import javax.jcr.query.Query;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class JcrQueryEditorInput implements IEditorInput {
	private final String query;
	private final String queryType;

	public JcrQueryEditorInput(String query, String queryType) {
		this.query = query;
		if (queryType == null)
			this.queryType = Query.JCR_SQL2;
		else
			this.queryType = queryType;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return query;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return query;
	}

	public String getQuery() {
		return query;
	}

	public String getQueryType() {
		return queryType;
	}

}
