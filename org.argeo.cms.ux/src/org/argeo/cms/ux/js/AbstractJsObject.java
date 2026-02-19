package org.argeo.cms.ux.js;

import java.util.Optional;

public abstract class AbstractJsObject {
	/**
	 * JavaScript expression returning a reference to the object. It can be either a
	 * variable or a function call. If it is not set the object is assumed to be a
	 * new.
	 */
	private String reference;

	private JsClient jsClient;

	private Object[] jsConstructorArgs;

//	public AbstractJsObject(JsClient jsClient, String reference) {
//		Objects.requireNonNull(jsClient);
//		Objects.requireNonNull(reference, "JS reference cannot be null");
//		this.jsClient = jsClient;
//		this.reference = reference;
//	}

	public AbstractJsObject(Object... args) {
		if (args.length == 2 && args[0] instanceof JsClient jsClient) {
			this.jsClient = jsClient;
			this.reference = args[1].toString();
		} else {
			this.jsConstructorArgs = args;
		}
	}

	public abstract String getJsPackage();

	public void create(JsClient jsClient, String varName) {
		if (!isNew())
			throw new IllegalStateException("JS object " + getJsClassName() + " is not new");
		if (isFunctionReference())
			throw new IllegalStateException(
					"JS object " + getJsClassName() + " cannot be created since it is a function reference");
		jsClient.execute(jsClient.getJsVarName(varName) + " = " + newJs() + ";");
		reference = varName;
		this.jsClient = jsClient;
	}

	public void delete() {
		if (isNew())
			throw new IllegalStateException(
					"JS object " + getJsClassName() + " cannot be deleted since it is anonymous");
		if (isFunctionReference())
			throw new IllegalStateException(
					"JS object " + getJsClassName() + " cannot be deleted since it is a function reference");
		jsClient.execute(reference + " = undefined; delete " + reference + ";");
	}

	public boolean isNew() {
		return reference == null;
	}

	public boolean isFunctionReference() {
		return !isNew() && !getReference().endsWith(")");
	}

	public String getReference() {
		return reference;
	}

	protected String getJsReference() {
		return jsClient.getJsVarName(reference);
	}

	protected String newJs() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(getJsClassName());
		sb.append("(");
		sb.append(JsClient.toJsArgs(jsConstructorArgs));
		sb.append(")");
		return sb.toString();
	}

	public String getJsClassName() {
		return getJsPackage() + "." + getClass().getSimpleName();
	}

	public Object callMethod(String methodName, Object... args) {
		return jsClient.callMethod(getJsReference(), methodName + "(" + JsClient.toJsArgs(args) + ")");
	}

	public void executeMethod(String methodName, Object... args) {
		jsClient.executeMethod(getJsReference(), methodName + "(" + JsClient.toJsArgs(args) + ")");
	}

	protected String getMethodName() {
		StackWalker walker = StackWalker.getInstance();
		Optional<String> methodName = walker.walk(frames -> {
			return frames.skip(1).findFirst().map(StackWalker.StackFrame::getMethodName);
		});
		return methodName.orElseThrow();
	}

	protected JsClient getJsClient() {
		return jsClient;
	}

	protected Object[] getJsConstructorArgs() {
		return jsConstructorArgs;
	}

	protected void setJsConstructorArgs(Object[] jsConstructorArgs) {
		this.jsConstructorArgs = jsConstructorArgs;
	}

}
