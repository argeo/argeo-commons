package org.argeo.eclipse.ui.specific;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;

public class BufferedImageDisplay extends Composite {
	private BufferedImage image;

	public BufferedImageDisplay(Composite parent, int style) {
		super(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(this);
		frame.setLayout(new BorderLayout());
		frame.add(new JPanel() {
			private static final long serialVersionUID = 8924410573598922364L;

			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (image != null)
					g.drawImage(image, 0, 0, this);
			}

		}, BorderLayout.CENTER);
		frame.setVisible(true);
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

}
