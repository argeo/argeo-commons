package org.argeo.util.transaction;

/** JTA transaction status. */
public class JtaStatusAdapter implements TransactionStatusAdapter<Integer> {
	private static final Integer STATUS_ACTIVE = 0;
	private static final Integer STATUS_COMMITTED = 3;
	private static final Integer STATUS_COMMITTING = 8;
	private static final Integer STATUS_MARKED_ROLLBACK = 1;
	private static final Integer STATUS_NO_TRANSACTION = 6;
	private static final Integer STATUS_PREPARED = 2;
	private static final Integer STATUS_PREPARING = 7;
	private static final Integer STATUS_ROLLEDBACK = 4;
	private static final Integer STATUS_ROLLING_BACK = 9;
//	private static final Integer STATUS_UNKNOWN = 5;

	@Override
	public Integer getActiveStatus() {
		return STATUS_ACTIVE;
	}

	@Override
	public Integer getPreparingStatus() {
		return STATUS_PREPARING;
	}

	@Override
	public Integer getMarkedRollbackStatus() {
		return STATUS_MARKED_ROLLBACK;
	}

	@Override
	public Integer getPreparedStatus() {
		return STATUS_PREPARED;
	}

	@Override
	public Integer getCommittingStatus() {
		return STATUS_COMMITTING;
	}

	@Override
	public Integer getCommittedStatus() {
		return STATUS_COMMITTED;
	}

	@Override
	public Integer getRollingBackStatus() {
		return STATUS_ROLLING_BACK;
	}

	@Override
	public Integer getRolledBackStatus() {
		return STATUS_ROLLEDBACK;
	}

	@Override
	public Integer getNoTransactionStatus() {
		return STATUS_NO_TRANSACTION;
	}

}
