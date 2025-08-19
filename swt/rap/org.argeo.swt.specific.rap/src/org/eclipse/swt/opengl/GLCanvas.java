package org.eclipse.swt.opengl;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/** Mock OpenGL canvas. Only used for compilation. */
public class GLCanvas extends Canvas {
	private static volatile GLCanvas current = null;

	private final GLData data;

	private static final long serialVersionUID = -4606284157996259356L;

	public GLCanvas(Composite parent, int style, GLData data) {
		super(parent, style);
		this.data = data;
	}

	public GLData getGLData() {
		return data;
	}

	public void setCurrent() {
		current(this);
	}

	public boolean isCurrent() {
		return this == current();
	}

	private synchronized void current(GLCanvas canvas) {
		current = canvas;
	}

	private synchronized GLCanvas current() {
		return current;
	}

	public void swapBuffers() {
		// does nothing
	}
}
