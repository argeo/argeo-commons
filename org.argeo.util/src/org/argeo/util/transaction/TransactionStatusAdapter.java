package org.argeo.util.transaction;

/** Abstract the various approaches to represent transaction status. */
public interface TransactionStatusAdapter<T> {
	T getActiveStatus();

	T getPreparingStatus();

	T getMarkedRollbackStatus();

	T getPreparedStatus();

	T getCommittingStatus();

	T getCommittedStatus();

	T getRollingBackStatus();

	T getRolledBackStatus();

	T getNoTransactionStatus();
}
