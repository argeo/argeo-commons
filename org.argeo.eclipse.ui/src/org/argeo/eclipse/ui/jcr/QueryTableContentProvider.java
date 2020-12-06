package org.argeo.eclipse.ui.jcr;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** Content provider based on a JCR {@link Query}. */
public class QueryTableContentProvider implements IStructuredContentProvider {
	private static final long serialVersionUID = 760371460907204722L;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Query query = (Query) inputElement;
		try {
			NodeIterator nit = query.execute().getNodes();
			return JcrUtils.nodeIteratorToList(nit).toArray();
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

}
