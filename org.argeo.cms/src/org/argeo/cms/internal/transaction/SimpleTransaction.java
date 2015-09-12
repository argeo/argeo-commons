package org.argeo.cms.internal.transaction;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class SimpleTransaction implements Transaction, Status {
	private final static Log log = LogFactory.getLog(SimpleTransaction.class);

	private final Xid xid;
	private int status = Status.STATUS_ACTIVE;
	private final List<XAResource> xaResources = new ArrayList<XAResource>();

	public SimpleTransaction() {
		xid = new UuidXid();
	}

	@Override
	public synchronized void commit() throws RollbackException,
			HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		status = STATUS_PREPARING;
		for (XAResource xaRes : xaResources) {
			if (status == STATUS_MARKED_ROLLBACK)
				break;
			try {
				xaRes.prepare(xid);
			} catch (XAException e) {
				status = STATUS_MARKED_ROLLBACK;
				log.error("Cannot prepare " + xaRes + " for " + xid, e);
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
				log.error("Cannot prepare " + xaRes + " for " + xid, e);
			}
		}
		if (status == STATUS_MARKED_ROLLBACK) {
			rollback();
			throw new RollbackException();
		}
		status = STATUS_COMMITTED;
	}

	@Override
	public synchronized boolean delistResource(XAResource xaRes, int flag)
			throws IllegalStateException, SystemException {
		return xaResources.remove(xaRes);
	}

	@Override
	public synchronized boolean enlistResource(XAResource xaRes)
			throws RollbackException, IllegalStateException, SystemException {
		return xaResources.add(xaRes);
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
	public synchronized void rollback() throws IllegalStateException,
			SystemException {
		status = STATUS_ROLLING_BACK;
		for (XAResource xaRes : xaResources) {
			try {
				xaRes.rollback(xid);
			} catch (XAException e) {
				log.error("Cannot rollback " + xaRes + " for " + xid, e);
			}
		}
		status = STATUS_ROLLEDBACK;
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		status = STATUS_MARKED_ROLLBACK;
	}

	@Override
	public int hashCode() {
		return xid.hashCode();
	}

	public Xid getXid() {
		return xid;
	}

}
