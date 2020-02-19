package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.xml.sax.ContentHandler;

public class CmsWorkspace implements Workspace {
	private String name;
	private Session session;

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
			AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
			throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
			PathNotFoundException, ItemExistsException, LockException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
			throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
			PathNotFoundException, ItemExistsException, LockException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
			AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void restore(Version[] versions, boolean removeExisting)
			throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
			InvalidItemStateException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public QueryManager getQueryManager() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeTypeManager getNodeTypeManager() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ObservationManager getObservationManager()
			throws UnsupportedRepositoryOperationException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public VersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getAccessibleWorkspaceNames() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException,
			ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, VersionException,
			PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
			LockException, AccessDeniedException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createWorkspace(String name)
			throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createWorkspace(String name, String srcWorkspace) throws AccessDeniedException,
			UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException,
			NoSuchWorkspaceException, RepositoryException {
		throw new UnsupportedOperationException();
	}

}
