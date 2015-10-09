package org.argeo.osgi.useradmin;

import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;

/** {@link XAResource} for a user directory being edited. */
class UserDirectoryWorkingCopy implements XAResource {
	private final static Log log = LogFactory
			.getLog(UserDirectoryWorkingCopy.class);
	private final String cacheName = getClass().getName();

	private final AbstractUserDirectory userDirectory;

	private Xid xid;
	private int transactionTimeout = 0;

	private Map<LdapName, DirectoryUser> newUsers = new HashMap<LdapName, DirectoryUser>();
	private Map<LdapName, Attributes> modifiedUsers = new HashMap<LdapName, Attributes>();
	private Map<LdapName, DirectoryUser> deletedUsers = new HashMap<LdapName, DirectoryUser>();

	public UserDirectoryWorkingCopy(AbstractUserDirectory userDirectory) {
		this.userDirectory = userDirectory;
		try {
			// FIXME Make it less bitronix dependant
			EhCacheXAResourceProducer.registerXAResource(cacheName, this);
		} catch (Exception e) {
			log.error("Cannot register resource to Bitronix", e);
		}
	}

	@Override
	public void start(Xid xid, int flags) throws XAException {
		this.xid = xid;
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		checkXid(xid);

	}

	private void cleanUp() {
		// clean collections
		newUsers.clear();
		newUsers = null;
		modifiedUsers.clear();
		modifiedUsers = null;
		deletedUsers.clear();
		deletedUsers = null;

		// clean IDs
		this.xid = null;
		userDirectory.clearEditingTransactionXid();

		try {
			// FIXME Make it less bitronix dependant
			EhCacheXAResourceProducer.unregisterXAResource(cacheName, this);
		} catch (Exception e) {
			log.error("Cannot unregister resource from Bitronix", e);
		}
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		checkXid(xid);
		if (noModifications())
			return XA_RDONLY;
		try {
			userDirectory.prepare(this);
		} catch (Exception e) {
			log.error("Cannot prepare " + xid, e);
			throw new XAException(XAException.XA_RBOTHER);
		}
		return XA_OK;
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		try {
			checkXid(xid);
			if (noModifications())
				return;
			if (onePhase)
				userDirectory.prepare(this);
			userDirectory.commit(this);
		} catch (Exception e) {
			log.error("Cannot commit " + xid, e);
			throw new XAException(XAException.XA_RBOTHER);
		} finally {
			cleanUp();
		}
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		try {
			checkXid(xid);
			userDirectory.rollback(this);
		} catch (Exception e) {
			log.error("Cannot rollback " + xid, e);
			throw new XAException(XAException.XA_HEURMIX);
		} finally {
			cleanUp();
		}
	}

	@Override
	public void forget(Xid xid) throws XAException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSameRM(XAResource xares) throws XAException {
		return xares == this;
	}

	@Override
	public Xid[] recover(int flag) throws XAException {
		return new Xid[0];
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return transactionTimeout;
	}

	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		transactionTimeout = seconds;
		return true;
	}

	Xid getXid() {
		return xid;
	}

	private void checkXid(Xid xid) throws XAException {
		if (this.xid == null)
			throw new XAException(XAException.XAER_OUTSIDE);
		if (!this.xid.equals(xid))
			throw new XAException(XAException.XAER_NOTA);
	}

	public boolean noModifications() {
		return newUsers.size() == 0 && modifiedUsers.size() == 0
				&& deletedUsers.size() == 0;
	}

	public Attributes getAttributes(LdapName dn) {
		if (modifiedUsers.containsKey(dn))
			return modifiedUsers.get(dn);
		return null;
	}

	public void startEditing(DirectoryUser user) {
		LdapName dn = user.getDn();
		if (modifiedUsers.containsKey(dn))
			throw new UserDirectoryException("Already editing " + dn);
		modifiedUsers.put(dn, (Attributes) user.getAttributes().clone());
	}

	public Map<LdapName, DirectoryUser> getNewUsers() {
		return newUsers;
	}

	public Map<LdapName, DirectoryUser> getDeletedUsers() {
		return deletedUsers;
	}

	public Map<LdapName, Attributes> getModifiedUsers() {
		return modifiedUsers;
	}

}
