/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.activemq;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Small Swing-based UI to pass user/name and password. TODO better integrate
 * with JAAS callbacks.
 */
public class UserPasswordDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -9052993072210981198L;
	private static String OK = "ok";

	private JTextField username = new JTextField("", 10);
	private JPasswordField password = new JPasswordField("", 10);

	private JButton okButton;
	private JButton cancelButton;

	public UserPasswordDialog() {
		setTitle("Credentials");
		setModal(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel p1 = new JPanel(new GridLayout(2, 2, 3, 3));
		p1.add(new JLabel("User"));
		p1.add(username);
		p1.add(new JLabel("Password"));
		password.setActionCommand(OK);
		password.addActionListener(this);
		p1.add(password);
		add("Center", p1);

		Panel p2 = new Panel();
		okButton = addButton(p2, "OK");
		okButton.setActionCommand(OK);
		cancelButton = addButton(p2, "Cancel");
		add("South", p2);
		setSize(240, 120);

		pack();
	}

	/** To be overridden */
	protected void useCredentials(String username, char[] password) {
		// does nothing
	}

	private JButton addButton(Container c, String name) {
		JButton button = new JButton(name);
		button.addActionListener(this);
		c.add(button);
		return button;
	}

	public final void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		if (source == okButton || evt.getActionCommand().equals(OK)) {
			char[] p = password.getPassword();
			useCredentials(username.getText(), p);
			Arrays.fill(p, '0');
			cleanUp();
		} else if (source == cancelButton)
			cleanUp();
	}

	private void cleanUp() {
		password.setText("");
		dispose();
	}

	public static void main(String[] args) {
		UserPasswordDialog dialog = new UserPasswordDialog() {
			private static final long serialVersionUID = -891646559691412088L;

			protected void useCredentials(String username, char[] password) {
				System.out.println(username + "/" + new String(password));
			}
		};
		dialog.setVisible(true);
		System.out.println("After show");
	}
}
