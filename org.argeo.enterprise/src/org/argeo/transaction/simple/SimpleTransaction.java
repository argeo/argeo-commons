package org.argeo.transaction.simple;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/** Simple implementation of an XA {@link Transaction}. */
class SimpleTransaction implements Transaction, Status {
	private final Xid xid;
	private int status = Status.STATUS_ACTIVE;
	private final List<XAResource> xaResources = new ArrayList<XAResource>();

	private final SimpleTransactionManager transactionManager;

	public SimpleTransaction(SimpleTransactionManager transactionManager) {
		this.xid = new UuidXid();
		this.transactionManager = transactionManager;
	}

	@Override
	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		status = STATUS_PREPARING;
		for (XAResource xaRes : xaResources) {
			if (status == STATUS_MARKED_ROLLBACK)
				break;
			try {
				xaRes.prepare(xid);
			} catch (XAException e) {
				status = STATUS_MARKED_ROLLBACK;
				error("Cannot prepare " + xaRes + " for " + xid, e);
			}
		}
		if (status == STATUS_MARKED_ROLLBACK) {
			rollback();
			throw new RollbackException();
		}
		status = STATUS_PREPARED;

		status = STATUS_COMMITTING;
		for (XAResource xaRes : xaResources) {
			if (status == STATUS_MARKED_ROLLBACK)
				break;
			try {
				xaRes.commit(xid, false);
			} catch (XAException e) {
				status = STATUS_MARKED_ROLLBACK;
				error("Cannot prepare " + xaRes + " for " + xid, e);
			}
		}
		if (status == STATUS_MARKED_ROLLBACK) {
			rollback();
			throw new RollbackException();
		}

		// complete
		status = STATUS_COMMITTED;
		clearResources(XAResource.TMSUCCESS);
		transactionManager.unregister(xid);
	}

	@Override
	public synchronized void rollback() throws IllegalStateException, SystemException {
		status = STATUS_ROLLING_BACK;
		for (XAResource xaRes : xaResources) {
			try {
				xaRes.rollback(xid);
			} catch (XAException e) {
				error("Cannot rollback " + xaRes + " for " + xid, e);
			}
		}

		// complete
		status = STATUS_ROLLEDBACK;
		clearResources(XAResource.TMFAIL);
		transactionManager.unregister(xid);
	}

	@Override
	public synchronized boolean enlistResource(XAResource xaRes)
			throws RollbackException, IllegalStateException, SystemException {
		if (xaResources.add(xaRes)) {
			try {
				xaRes.start(getXid(), XAResource.TMNOFLAGS);
				return true;
			} catch (XAException e) {
				error("Cannot enlist " + xaRes, e);
				return false;
			}
		} else
			return false;
	}

	@Override
	public synchronized boolean delistResource(XAResource xaRes, int flag)
			throws IllegalStateException, SystemException {
		if (xaResources.remove(xaRes)) {
			try {
				xaRes.end(getXid(), flag);
			} catch (XAException e) {
				error("Cannot delist " + xaRes, e);
				return false;
			}
			return true;
		} else
			return false;
	}

	protected void clearResources(int flag) {
		for (XAResource xaRes : xaResources)
			try {
				xaRes.end(getXid(), flag);
			} catch (XAException e) {
				error("Cannot end " + xaRes, e);
			}
		xaResources.clear();
	}

	protected void error(Object obj, Exception e) {
		System.err.println(obj);
		e.printStackTrace();
	}

	@Override
	public synchronized int getStatus() throws SystemException {
		return status;
	}

	@Override
	public void registerSynchronization(Synchronization sync)
			throws RollbackException, IllegalStateException, SystemException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		status = STATUS_MARKED_ROLLBACK;
	}

	@Override
	public int hashCode() {
		return xid.hashCode();
	}

	Xid getXid() {
		return xid;
	}

}
