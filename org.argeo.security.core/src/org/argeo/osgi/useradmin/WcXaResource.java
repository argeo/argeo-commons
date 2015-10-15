package org.argeo.osgi.useradmin;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** {@link XAResource} for a user directory being edited. */
class WcXaResource implements XAResource {
	private final static Log log = LogFactory.getLog(WcXaResource.class);

	private final AbstractUserDirectory userDirectory;

	private Map<Xid, UserDirectoryWorkingCopy> workingCopies = new HashMap<Xid, UserDirectoryWorkingCopy>();
	private Xid editingXid = null;
	private int transactionTimeout = 0;

	public WcXaResource(AbstractUserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Override
	public void start(Xid xid, int flags) throws XAException {
		if (editingXid != null)
			throw new UserDirectoryException("Already editing " + editingXid);
		UserDirectoryWorkingCopy wc = workingCopies.put(xid,
				new UserDirectoryWorkingCopy());
		if (wc != null)
			throw new UserDirectoryException(
					"There is already a working copy for " + xid);
		this.editingXid = xid;
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		checkXid(xid);

	}

	private UserDirectoryWorkingCopy wc(Xid xid) {
		return workingCopies.get(xid);
	}

	UserDirectoryWorkingCopy wc() {
		if (editingXid == null)
			return null;
		UserDirectoryWorkingCopy wc = workingCopies.get(editingXid);
		if (wc == null)
			throw new UserDirectoryException("No working copy found for "
					+ editingXid);
		return wc;
	}

	private void cleanUp(Xid xid) {
		// clean collections
		wc(xid).cleanUp();
		workingCopies.remove(xid);

		// clean IDs
		// userDirectory.clearEditingTransactionXid();
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		checkXid(xid);
		UserDirectoryWorkingCopy wc = wc(xid);
		if (wc.noModifications())
			return XA_RDONLY;
		try {
			userDirectory.prepare(wc);
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
			UserDirectoryWorkingCopy wc = wc(xid);
			if (wc.noModifications())
				return;
			if (onePhase)
				userDirectory.prepare(wc);
			userDirectory.commit(wc);
		} catch (Exception e) {
			log.error("Cannot commit " + xid, e);
			throw new XAException(XAException.XA_RBOTHER);
		} finally {
			cleanUp(xid);
		}
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		try {
			checkXid(xid);
			userDirectory.rollback(wc(xid));
		} catch (Exception e) {
			log.error("Cannot rollback " + xid, e);
			throw new XAException(XAException.XA_HEURMIX);
		} finally {
			cleanUp(xid);
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

	private void checkXid(Xid xid) throws XAException {
		if (xid == null)
			throw new XAException(XAException.XAER_OUTSIDE);
		if (!xid.equals(xid))
			throw new XAException(XAException.XAER_NOTA);
	}

}
