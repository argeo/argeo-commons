package org.argeo.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Wrapper around a JCR repository which allows to simplify configuration and
 * intercept some actions. It exposes itself as a {@link Repository}.
 */
public abstract class JcrRepositoryWrapper implements Repository {
	// private final static Log log = LogFactory
	// .getLog(JcrRepositoryWrapper.class);

	// wrapped repository
	private Repository repository;

	private Map<String, String> additionalDescriptors = new HashMap<>();

	private Boolean autocreateWorkspaces = false;

	public JcrRepositoryWrapper(Repository repository) {
		setRepository(repository);
	}

	/**
	 * Empty constructor
	 */
	public JcrRepositoryWrapper() {
	}

	// /** Initializes */
	// public void init() {
	// }
	//
	// /** Shutdown the repository */
	// public void destroy() throws Exception {
	// }

	protected void putDescriptor(String key, String value) {
		if (Arrays.asList(getRepository().getDescriptorKeys()).contains(key))
			throw new IllegalArgumentException("Descriptor key " + key + " is already defined in wrapped repository");
		if (value == null)
			additionalDescriptors.remove(key);
		else
			additionalDescriptors.put(key, value);
	}

	/*
	 * DELEGATED JCR REPOSITORY METHODS
	 */

	public String getDescriptor(String key) {
		if (additionalDescriptors.containsKey(key))
			return additionalDescriptors.get(key);
		return getRepository().getDescriptor(key);
	}

	public String[] getDescriptorKeys() {
		if (additionalDescriptors.size() == 0)
			return getRepository().getDescriptorKeys();
		List<String> keys = Arrays.asList(getRepository().getDescriptorKeys());
		keys.addAll(additionalDescriptors.keySet());
		return keys.toArray(new String[keys.size()]);
	}

	/** Central login method */
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException, RepositoryException {
		Session session;
		try {
			session = getRepository(workspaceName).login(credentials, workspaceName);
		} catch (NoSuchWorkspaceException e) {
			if (autocreateWorkspaces && workspaceName != null)
				session = createWorkspaceAndLogsIn(credentials, workspaceName);
			else
				throw e;
		}
		processNewSession(session, workspaceName);
		return session;
	}

	public Session login() throws LoginException, RepositoryException {
		return login(null, null);
	}

	public Session login(Credentials credentials) throws LoginException, RepositoryException {
		return login(credentials, null);
	}

	public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return login(null, workspaceName);
	}

	/** Called after a session has been created, does nothing by default. */
	protected void processNewSession(Session session, String workspaceName) {
	}

	/**
	 * Wraps access to the repository, making sure it is available.
	 * 
	 * @deprecated Use {@link #getDefaultRepository()} instead.
	 */
	@Deprecated
	protected synchronized Repository getRepository() {
		return getDefaultRepository();
	}

	protected synchronized Repository getDefaultRepository() {
		return repository;
	}

	protected synchronized Repository getRepository(String workspaceName) {
		return getDefaultRepository();
	}

	/**
	 * Logs in to the default workspace, creates the required workspace, logs out,
	 * logs in to the required workspace.
	 */
	protected Session createWorkspaceAndLogsIn(Credentials credentials, String workspaceName)
			throws RepositoryException {
		if (workspaceName == null)
			throw new IllegalArgumentException("No workspace specified.");
		Session session = getRepository(workspaceName).login(credentials);
		session.getWorkspace().createWorkspace(workspaceName);
		session.logout();
		return getRepository(workspaceName).login(credentials, workspaceName);
	}

	public boolean isStandardDescriptor(String key) {
		return getRepository().isStandardDescriptor(key);
	}

	public boolean isSingleValueDescriptor(String key) {
		if (additionalDescriptors.containsKey(key))
			return true;
		return getRepository().isSingleValueDescriptor(key);
	}

	public Value getDescriptorValue(String key) {
		if (additionalDescriptors.containsKey(key))
			return new StrValue(additionalDescriptors.get(key));
		return getRepository().getDescriptorValue(key);
	}

	public Value[] getDescriptorValues(String key) {
		return getRepository().getDescriptorValues(key);
	}

	public synchronized void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setAutocreateWorkspaces(Boolean autocreateWorkspaces) {
		this.autocreateWorkspaces = autocreateWorkspaces;
	}

	protected static class StrValue implements Value {
		private final String str;

		public StrValue(String str) {
			this.str = str;
		}

		@Override
		public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
			return str;
		}

		@Override
		public InputStream getStream() throws RepositoryException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Binary getBinary() throws RepositoryException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getLong() throws ValueFormatException, RepositoryException {
			try {
				return Long.parseLong(str);
			} catch (NumberFormatException e) {
				throw new ValueFormatException("Cannot convert", e);
			}
		}

		@Override
		public double getDouble() throws ValueFormatException, RepositoryException {
			try {
				return Double.parseDouble(str);
			} catch (NumberFormatException e) {
				throw new ValueFormatException("Cannot convert", e);
			}
		}

		@Override
		public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
			try {
				return new BigDecimal(str);
			} catch (NumberFormatException e) {
				throw new ValueFormatException("Cannot convert", e);
			}
		}

		@Override
		public Calendar getDate() throws ValueFormatException, RepositoryException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean getBoolean() throws ValueFormatException, RepositoryException {
			try {
				return Boolean.parseBoolean(str);
			} catch (NumberFormatException e) {
				throw new ValueFormatException("Cannot convert", e);
			}
		}

		@Override
		public int getType() {
			return PropertyType.STRING;
		}

	}

}
