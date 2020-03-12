package org.argeo.util;

import java.io.Serializable;

/** The status of a test. */
public class TesterStatus implements Serializable {
	private static final long serialVersionUID = 6272975746885487000L;

	private Boolean passed = null;
	private final String uid;
	private Throwable throwable = null;

	public TesterStatus(String uid) {
		this.uid = uid;
	}

	/** For cloning. */
	public TesterStatus(String uid, Boolean passed, Throwable throwable) {
		this(uid);
		this.passed = passed;
		this.throwable = throwable;
	}

	public synchronized Boolean isRunning() {
		return passed == null;
	}

	public synchronized Boolean isPassed() {
		assert passed != null;
		return passed;
	}

	public synchronized Boolean isFailed() {
		assert passed != null;
		return !passed;
	}

	public synchronized void setPassed() {
		setStatus(true);
	}

	public synchronized void setFailed() {
		setStatus(false);
	}

	public synchronized void setFailed(Throwable throwable) {
		setStatus(false);
		setThrowable(throwable);
	}

	protected void setStatus(Boolean passed) {
		if (this.passed != null)
			throw new IllegalStateException("Passed status of test " + uid + " is already set (to " + passed + ")");
		this.passed = passed;
	}

	protected void setThrowable(Throwable throwable) {
		if (this.throwable != null)
			throw new IllegalStateException("Throwable of test " + uid + " is already set (to " + passed + ")");
		this.throwable = throwable;
	}

	public String getUid() {
		return uid;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TesterStatus) {
			TesterStatus other = (TesterStatus) o;
			// we don't check consistency for performance purposes
			// this equals() is supposed to be used in collections or for transfer
			return other.uid.equals(uid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uid.hashCode();
	}

	@Override
	public String toString() {
		return uid + "\t" + (passed ? "passed" : "failed");
	}

}
