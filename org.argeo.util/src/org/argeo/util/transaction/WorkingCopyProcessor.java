package org.argeo.util.transaction;

public interface WorkingCopyProcessor<WC extends WorkingCopy<?, ?, ?>> {
	void prepare(WC wc);

	void commit(WC wc);

	void rollback(WC wc);
	
	WC newWorkingCopy();
}
