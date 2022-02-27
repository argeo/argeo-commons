package org.argeo.osgi.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/** Simple implementation of an XA transaction. */
class SimpleTransaction<T>
//implements Transaction, Status 
{
	private final Xid xid;
	private T status;
	private final List<XAResource> xaResources = new ArrayList<XAResource>();

	private final SimpleTransactionManager transactionManager;
	private TransactionStatusAdapter<T> tsa;

	public SimpleTransaction(SimpleTransactionManager transactionManager, TransactionStatusAdapter<T> tsa) {
		this.tsa = tsa;
		this.status = tsa.getActiveStatus();
		this.xid = new UuidXid();
		this.transactionManager = transactionManager;
	}

	public synchronized void commit()
//			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
//			SecurityException, IllegalStateException, SystemException 
	{
		status = tsa.getPreparingStatus();
		for (XAResource xaRes : xaResources) {
			if (status.equals(tsa.getMarkedRollbackStatus()))
				break;
			try {
				xaRes.prepare(xid);
			} catch (XAException e) {
				status = tsa.getMarkedRollbackStatus();
				error("Cannot prepare " + xaRes + " for " + xid, e);
			}
		}
		if (status.equals(tsa.getMarkedRollbackStatus())) {
			rollback();
			throw new SimpleRollbackException();
		}
		status = tsa.getPreparedStatus();

		status = tsa.getCommittingStatus();
		for (XAResource xaRes : xaResources) {
			if (status.equals(tsa.getMarkedRollbackStatus()))
				break;
			try {
				xaRes.commit(xid, false);
			} catch (XAException e) {
				status = tsa.getMarkedRollbackStatus();
				error("Cannot prepare " + xaRes + " for " + xid, e);
			}
		}
		if (status.equals(tsa.getMarkedRollbackStatus())) {
			rollback();
			throw new SimpleRollbackException();
		}

		// complete
		status = tsa.getCommittedStatus();
		clearResources(XAResource.TMSUCCESS);
		transactionManager.unregister(xid);
	}

	public synchronized void rollback()
//			throws IllegalStateException, SystemException 
	{
		status = tsa.getRollingBackStatus();
		for (XAResource xaRes : xaResources) {
			try {
				xaRes.rollback(xid);
			} catch (XAException e) {
				error("Cannot rollback " + xaRes + " for " + xid, e);
			}
		}

		// complete
		status = tsa.getRolledBackStatus();
		clearResources(XAResource.TMFAIL);
		transactionManager.unregister(xid);
	}

	public synchronized boolean enlistResource(XAResource xaRes)
//			throws RollbackException, IllegalStateException, SystemException 
	{
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

	public synchronized boolean delistResource(XAResource xaRes, int flag)
//			throws IllegalStateException, SystemException 
	{
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

	public synchronized T getStatus()
//			throws SystemException 
	{
		return status;
	}

//	public void registerSynchronization(Synchronization sync)
//			throws RollbackException, IllegalStateException, SystemException {
//		throw new UnsupportedOperationException();
//	}

	public void setRollbackOnly()
//			throws IllegalStateException, SystemException 
	{
		status = tsa.getMarkedRollbackStatus();
	}

	@Override
	public int hashCode() {
		return xid.hashCode();
	}

	Xid getXid() {
		return xid;
	}

}
