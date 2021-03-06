package org.argeo.transaction.simple;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.Xid;

/**
 * Simple implementation of an XA {@link TransactionManager} and
 * {@link UserTransaction}.
 */
public class SimpleTransactionManager implements TransactionManager, UserTransaction {
	private ThreadLocal<SimpleTransaction> current = new ThreadLocal<SimpleTransaction>();

	private Map<Xid, SimpleTransaction> knownTransactions = Collections
			.synchronizedMap(new HashMap<Xid, SimpleTransaction>());
	private SyncRegistry syncRegistry = new SyncRegistry();

	@Override
	public void begin() throws NotSupportedException, SystemException {
		if (getCurrent() != null)
			throw new NotSupportedException("Nested transactions are not supported");
		SimpleTransaction transaction = new SimpleTransaction(this);
		knownTransactions.put(transaction.getXid(), transaction);
		current.set(transaction);
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().commit();
	}

	@Override
	public int getStatus() throws SystemException {
		if (getCurrent() == null)
			return Status.STATUS_NO_TRANSACTION;
		return getTransaction().getStatus();
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return getCurrent();
	}

	protected SimpleTransaction getCurrent() throws SystemException {
		SimpleTransaction transaction = current.get();
		if (transaction == null)
			return null;
		int status = transaction.getStatus();
		if (Status.STATUS_COMMITTED == status || Status.STATUS_ROLLEDBACK == status) {
			current.remove();
			return null;
		}
		return transaction;
	}

	void unregister(Xid xid) {
		knownTransactions.remove(xid);
	}

	@Override
	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		if (getCurrent() != null)
			throw new IllegalStateException("Transaction " + current.get() + " already registered");
		current.set((SimpleTransaction) tobj);
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().rollback();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		if (getCurrent() == null)
			throw new IllegalStateException("No transaction registered with the current thread.");
		getCurrent().setRollbackOnly();
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Transaction suspend() throws SystemException {
		Transaction transaction = getCurrent();
		current.remove();
		return transaction;
	}

	public TransactionSynchronizationRegistry getTsr() {
		return syncRegistry;
	}

	private class SyncRegistry implements TransactionSynchronizationRegistry {
		@Override
		public Object getTransactionKey() {
			try {
				SimpleTransaction transaction = getCurrent();
				if (transaction == null)
					return null;
				return getCurrent().getXid();
			} catch (SystemException e) {
				throw new IllegalStateException("Cannot get transaction key", e);
			}
		}

		@Override
		public void putResource(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getResource(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void registerInterposedSynchronization(Synchronization sync) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getTransactionStatus() {
			try {
				return getStatus();
			} catch (SystemException e) {
				throw new IllegalStateException("Cannot get status", e);
			}
		}

		@Override
		public boolean getRollbackOnly() {
			try {
				return getStatus() == Status.STATUS_MARKED_ROLLBACK;
			} catch (SystemException e) {
				throw new IllegalStateException("Cannot get status", e);
			}
		}

		@Override
		public void setRollbackOnly() {
			try {
				getCurrent().setRollbackOnly();
			} catch (Exception e) {
				throw new IllegalStateException("Cannot set rollback only", e);
			}
		}

	}
}
