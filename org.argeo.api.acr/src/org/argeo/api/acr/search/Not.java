package org.argeo.api.acr.search;

/** Negates the provided constraint. */
public class Not implements Constraint {
	final Constraint negated;

	public Not(Constraint negated) {
		this.negated = negated;
	}

	public Constraint getNegated() {
		return negated;
	}

}
