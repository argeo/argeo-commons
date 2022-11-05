package org.argeo.api.cms.transaction;

/**
 * A minimalistic interface inspired by JTA user transaction in order to commit
 * units of work externally.
 */
public interface WorkTransaction {
	void begin();

	void commit();

	void rollback();

	boolean isNoTransactionStatus();
}
