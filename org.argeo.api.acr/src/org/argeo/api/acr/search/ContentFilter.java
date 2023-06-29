package org.argeo.api.acr.search;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.argeo.api.acr.QNamed;

/** A constraint filtering based ona given composition (and/or). */
public abstract class ContentFilter<COMPOSITION extends Composition> implements Constraint {
	private Set<Constraint> constraintss = new HashSet<>();

	private COMPOSITION composition;

	boolean negateNextOperator = false;

	@SuppressWarnings("unchecked")
	ContentFilter(Class<COMPOSITION> clss) {
		if (clss == null)
			this.composition = null;
		else if (Intersection.class.isAssignableFrom(clss))
			this.composition = (COMPOSITION) new Intersection(this);
		else if (Union.class.isAssignableFrom(clss))
			this.composition = (COMPOSITION) new Union(this);
		else
			throw new IllegalArgumentException("Unkown composition " + clss);
	}

	/*
	 * LOGICAL OPERATORS
	 */

	public COMPOSITION all(Consumer<AndFilter> and) {
		AndFilter subFilter = new AndFilter();
		and.accept(subFilter);
		addConstraint(subFilter);
		return composition;
	}

	public COMPOSITION any(Consumer<OrFilter> or) {
		OrFilter subFilter = new OrFilter();
		or.accept(subFilter);
		addConstraint(subFilter);
		return composition;
	}

	public ContentFilter<COMPOSITION> not() {
		negateNextOperator = !negateNextOperator;
		return this;
	}

	/*
	 * NON WEBDAV
	 */
	public COMPOSITION isContentClass(QName... contentClass) {
		addConstraint(new IsContentClass(contentClass));
		return composition;
	}

	public COMPOSITION isContentClass(QNamed... contentClass) {
		addConstraint(new IsContentClass(contentClass));
		return composition;
	}

	/*
	 * COMPARISON OPERATORS
	 */

	public COMPOSITION eq(QName attr, Object value) {
		addConstraint(new Eq(attr, value));
		return composition;
	}

	public COMPOSITION eq(QNamed attr, Object value) {
		addConstraint(new Eq(attr.qName(), value));
		return composition;
	}

	/*
	 * UTILITIES
	 */
	protected void addConstraint(Constraint operator) {
		checkAddConstraint();
		Constraint operatorToAdd;
		if (negateNextOperator) {
			operatorToAdd = new Not(operator);
			negateNextOperator = false;
		} else {
			operatorToAdd = operator;
		}
		constraintss.add(operatorToAdd);
	}

	/** Checks that the root operator is not set. */
	private void checkAddConstraint() {
		if (composition == null && !constraintss.isEmpty())
			throw new IllegalStateException("An operator is already registered (" + constraintss.iterator().next()
					+ ") and no composition is defined");
	}

	/*
	 * ACCESSORs
	 */
	public Set<Constraint> getConstraints() {
		return constraintss;
	}

	public boolean isUnion() {
		return composition instanceof Union;
	}

	/*
	 * CLASSES
	 */

	public static class Not implements Constraint {
		final Constraint negated;

		public Not(Constraint negated) {
			this.negated = negated;
		}

		public Constraint getNegated() {
			return negated;
		}

	}

	public static class Eq implements Constraint {
		final QName prop;
		final Object value;

		public Eq(QName prop, Object value) {
			super();
			this.prop = prop;
			this.value = value;
		}

		public QName getProp() {
			return prop;
		}

		public Object getValue() {
			return value;
		}

	}

	public static class IsContentClass implements Constraint {
		final QName[] contentClasses;

		public IsContentClass(QName[] contentClasses) {
			this.contentClasses = contentClasses;
		}

		public IsContentClass(QNamed[] contentClasses) {
			this.contentClasses = new QName[contentClasses.length];
			for (int i = 0; i < contentClasses.length; i++)
				this.contentClasses[i] = contentClasses[i].qName();
		}

		public QName[] getContentClasses() {
			return contentClasses;
		}

	}

//	public static void main(String[] args) {
//		AndFilter filter = new AndFilter();
//		filter.eq(new QName("test"), "test").and().not().eq(new QName("type"), "integer");
//
//		OrFilter unionFilter = new OrFilter();
//		unionFilter.all((f) -> {
//			f.eq(DName.displayname, "").and().eq(DName.creationdate, "");
//		}).or().not().any((f) -> {
//			f.eq(DName.creationdate, "").or().eq(DName.displayname, "");
//		});
//
//	}

}
