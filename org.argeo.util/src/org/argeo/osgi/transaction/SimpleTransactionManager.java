package org.argeo.osgi.transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Simple implementation of an XA transaction manager.
 */
public class SimpleTransactionManager
// implements TransactionManager, UserTransaction 
		implements WorkControl, WorkTransaction {
	private ThreadLocal<SimpleTransaction<Integer>> current = new ThreadLocal<SimpleTransaction<Integer>>();

	private Map<Xid, SimpleTransaction<Integer>> knownTransactions = Collections
			.synchronizedMap(new HashMap<Xid, SimpleTransaction<Integer>>());
	private TransactionStatusAdapter<Integer> tsa = new JtaStatusAdapter();
//	private SyncRegistry syncRegistry = new SyncRegistry();

	/*
	 * WORK IMPLEMENTATION
	 */
	@Override
	public <T> T required(Callable<T> work) {
		T res;
		begin();
		try {
			res = work.call();
			commit();
		} catch (Exception e) {
			rollback();
			throw new SimpleRollbackException(e);
		}
		return res;
	}

	@Override
	public WorkContext getWorkContext() {
		return new WorkContext() {

			@Override
			public void registerXAResource(XAResource resource, String recoveryId) {
				getTransaction().enlistResource(resource);
			}
		};
	}

	/*
	 * WORK TRANSACTION IMPLEMENTATION
	 */

	@Override
	public boolean isNoTransactionStatus() {
		return tsa.getNoTransactionStatus().equals(getStatus());
	}

	/*
	 * JTA IMPLEMENTATION
	 */

	public void begin()
//			throws NotSupportedException, SystemException 
	{
		if (getCurrent() != null)
			throw new UnsupportedOperationException("Nested transactions are not supported");
		SimpleTransaction<Integer> transaction = new SimpleTransaction<Integer>(this, tsa);
		knownTransactions.put(transaction.getXid(), transaction);
		current.set(transaction);
	}

	public void commit()
//			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
//			SecurityException, IllegalStateException, SystemException 
	{
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().commit();
	}

	public int getStatus()
//			throws SystemException
	{
		if (getCurrent() == null)
			return tsa.getNoTransactionStatus();
		return getTransaction().getStatus();
	}

	public SimpleTransaction<Integer> getTransaction()
//			throws SystemException 
	{
		return getCurrent();
	}

	protected SimpleTransaction<Integer> getCurrent()
//			throws SystemException 
	{
		SimpleTransaction<Integer> transaction = current.get();
		if (transaction == null)
			return null;
		Integer status = transaction.getStatus();
		if (status.equals(tsa.getCommittedStatus()) || status.equals(tsa.getRolledBackStatus())) {
			current.remove();
			return null;
		}
		return transaction;
	}

	void unregister(Xid xid) {
		knownTransactions.remove(xid);
	}

	public void resume(SimpleTransaction<Integer> tobj)
//			throws InvalidTransactionException, IllegalStateException, SystemException 
	{
		if (getCurrent() != null)
			throw new IllegalStateException("Transaction " + current.get() + " already registered");
		current.set(tobj);
	}

	public void rollback()
//			throws IllegalStateException, SecurityException, SystemException 
	{
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().rollback();
	}

	public void setRollbackOnly()
//			throws IllegalStateException, SystemException 
	{
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().setRollbackOnly();
	}

	public void setTransactionTimeout(int seconds)
//			throws SystemException
	{
		throw new UnsupportedOperationException();
	}

	public SimpleTransaction<Integer> suspend()
//			throws SystemException
	{
		SimpleTransaction<Integer> transaction = getCurrent();
		current.remove();
		return transaction;
	}

//	public TransactionSynchronizationRegistry getTsr() {
//		return syncRegistry;
//	}
//
//	private class SyncRegistry implements TransactionSynchronizationRegistry {
//		@Override
//		public Object getTransactionKey() {
//			try {
//				SimpleTransaction transaction = getCurrent();
//				if (transaction == null)
//					return null;
//				return getCurrent().getXid();
//			} catch (SystemException e) {
//				throw new IllegalStateException("Cannot get transaction key", e);
//			}
//		}
//
//		@Override
//		public void putResource(Object key, Object value) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public Object getResource(Object key) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public void registerInterposedSynchronization(Synchronization sync) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public int getTransactionStatus() {
//			try {
//				return getStatus();
//			} catch (SystemException e) {
//				throw new IllegalStateException("Cannot get status", e);
//			}
//		}
//
//		@Override
//		public boolean getRollbackOnly() {
//			try {
//				return getStatus() == Status.STATUS_MARKED_ROLLBACK;
//			} catch (SystemException e) {
//				throw new IllegalStateException("Cannot get status", e);
//			}
//		}
//
//		@Override
//		public void setRollbackOnly() {
//			try {
//				getCurrent().setRollbackOnly();
//			} catch (Exception e) {
//				throw new IllegalStateException("Cannot set rollback only", e);
//			}
//		}
//
//	}
}
