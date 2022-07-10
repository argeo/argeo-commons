package org.argeo.eclipse.ui.specific;

import java.awt.image.BufferedImage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class BufferedImageDisplay extends Composite {
	private static final long serialVersionUID = 4541163690514461514L;
	private BufferedImage image;

	public BufferedImageDisplay(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND);
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

}
