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
package org.argeo.jcr.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.crypto.AbstractKeyring;
import org.argeo.util.crypto.PBEKeySpecCallback;

/** JCR based implementation of a keyring */
public class JcrKeyring extends AbstractKeyring implements ArgeoNames {
	private Session session;

	/**
	 * When setup is called the session has not yet been saved and we don't want
	 * to save it since there maybe other data which would be inconsistent. So
	 * we keep a reference to this node which will then be used (an reset to
	 * null) when handling the PBE callback. We keep one per thread in case
	 * multiple users are accessing the same instance of a keyring.
	 */
	private ThreadLocal<Node> notYetSavedKeyring = new ThreadLocal<Node>() {

		@Override
		protected Node initialValue() {
			return null;
		}
	};

	@Override
	protected Boolean isSetup() {
		try {
			if (notYetSavedKeyring.get() != null)
				return true;

			Node userHome = JcrUtils.getUserHome(session);
			return userHome.hasNode(ARGEO_KEYRING);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check whether keyring is setup", e);
		}
	}

	@Override
	protected void setup(char[] password) {
		Binary binary = null;
		InputStream in = null;
		try {
			Node userHome = JcrUtils.getUserHome(session);
			if (userHome.hasNode(ARGEO_KEYRING))
				throw new ArgeoException("Keyring already setup");
			Node keyring = userHome.addNode(ARGEO_KEYRING);
			keyring.addMixin(ArgeoTypes.ARGEO_PBE_SPEC);

			// deterministic salt and iteration count based on username
			String username = session.getUserID();
			byte[] salt = new byte[8];
			byte[] usernameBytes = username.getBytes();
			for (int i = 0; i < salt.length; i++) {
				if (i < usernameBytes.length)
					salt[i] = usernameBytes[i];
				else
					salt[i] = 0;
			}
			in = new ByteArrayInputStream(salt);
			binary = session.getValueFactory().createBinary(in);
			keyring.setProperty(ARGEO_SALT, binary);

			Integer iterationCount = username.length() * 200;
			keyring.setProperty(ARGEO_ITERATION_COUNT, iterationCount);

			// default algo
			// TODO check if algo and key length are available, use DES if not
			keyring.setProperty(ARGEO_SECRET_KEY_FACTORY, "PBKDF2WithHmacSHA1");
			keyring.setProperty(ARGEO_KEY_LENGTH, 256l);
			keyring.setProperty(ARGEO_SECRET_KEY_ENCRYPTION, "AES");
			keyring.setProperty(ARGEO_CIPHER, "AES/CBC/PKCS5Padding");

			// encrypted password hash
			// IOUtils.closeQuietly(in);
			// JcrUtils.closeQuietly(binary);
			// byte[] btPass = hash(password, salt, iterationCount);
			// in = new ByteArrayInputStream(btPass);
			// binary = session.getValueFactory().createBinary(in);
			// keyring.setProperty(ARGEO_PASSWORD, binary);

			notYetSavedKeyring.set(keyring);
		} catch (Exception e) {
			throw new ArgeoException("Cannot setup keyring", e);
		} finally {
			JcrUtils.closeQuietly(binary);
			IOUtils.closeQuietly(in);
			// JcrUtils.discardQuietly(session);
		}
	}

	@Override
	protected void handleKeySpecCallback(PBEKeySpecCallback pbeCallback) {
		try {
			Node userHome = JcrUtils.getUserHome(session);
			Node keyring;
			if (userHome.hasNode(ARGEO_KEYRING))
				keyring = userHome.getNode(ARGEO_KEYRING);
			else if (notYetSavedKeyring.get() != null)
				keyring = notYetSavedKeyring.get();
			else
				throw new ArgeoException("Keyring not setup");

			pbeCallback.set(keyring.getProperty(ARGEO_SECRET_KEY_FACTORY)
					.getString(), JcrUtils.getBinaryAsBytes(keyring
					.getProperty(ARGEO_SALT)),
					(int) keyring.getProperty(ARGEO_ITERATION_COUNT).getLong(),
					(int) keyring.getProperty(ARGEO_KEY_LENGTH).getLong(),
					keyring.getProperty(ARGEO_SECRET_KEY_ENCRYPTION)
							.getString());

			if (notYetSavedKeyring.get() != null)
				notYetSavedKeyring.remove();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot handle key spec callback", e);
		}
	}

	/** The node must already exist at this path */
	@Override
	protected void encrypt(String path, InputStream unencrypted) {
		// should be called first for lazy initialization
		SecretKey secretKey = getSecretKey();

		Binary binary = null;
		InputStream in = null;
		// ByteArrayOutputStream out = null;
		// OutputStream encrypted = null;

		try {
			Cipher cipher = createCipher();
			if (!session.nodeExists(path))
				throw new ArgeoException("No node at " + path);
			Node node = session.getNode(path);
			node.addMixin(ArgeoTypes.ARGEO_ENCRYPTED);
			SecureRandom random = new SecureRandom();
			byte[] iv = new byte[16];
			random.nextBytes(iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
			// AlgorithmParameters params = cipher.getParameters();
			// byte[] iv =
			// params.getParameterSpec(IvParameterSpec.class).getIV();
			// if (iv != null)
			JcrUtils.setBinaryAsBytes(node, ARGEO_IV, iv);

			// out = new ByteArrayOutputStream();
			// // encrypted = new CipherOutputStream(out, cipher);
			// IOUtils.copy(unencrypted, out);
			// byte[] unenc = out.toByteArray();
			// byte[] crypted = cipher.doFinal(unenc);

			// Cipher decipher = createCipher();
			// decipher.init(Cipher.DECRYPT_MODE, secretKey, new
			// IvParameterSpec(
			// iv));
			// byte[] decrypted = decipher.doFinal(crypted);
			// System.out.println("Password :'" + new String(decrypted) + "'");

			// JcrUtils.setBinaryAsBytes(node, Property.JCR_DATA, crypted);

			in = new CipherInputStream(unencrypted, cipher);
			binary = session.getValueFactory().createBinary(in);
			node.setProperty(Property.JCR_DATA, binary);
		} catch (Exception e) {
			throw new ArgeoException("Cannot encrypt", e);
		} finally {
			// IOUtils.closeQuietly(out);
			// IOUtils.closeQuietly(encrypted);
			IOUtils.closeQuietly(unencrypted);
			IOUtils.closeQuietly(in);
			JcrUtils.closeQuietly(binary);
		}
	}

	@Override
	protected InputStream decrypt(String path) {
		// should be called first for lazy initialization
		SecretKey secretKey = getSecretKey();

		Binary binary = null;
		InputStream encrypted = null;

		try {
			Cipher cipher = createCipher();
			if (!session.nodeExists(path))
				throw new ArgeoException("No node at " + path);
			Node node = session.getNode(path);
			if (node.hasProperty(ARGEO_IV)) {
				byte[] iv = JcrUtils.getBinaryAsBytes(node
						.getProperty(ARGEO_IV));
				cipher.init(Cipher.DECRYPT_MODE, secretKey,
						new IvParameterSpec(iv));
			} else {
				cipher.init(Cipher.DECRYPT_MODE, secretKey);
			}

			// byte[] arr = JcrUtils.getBinaryAsBytes(node
			// .getProperty(Property.JCR_DATA));
			// byte[] arr2 = cipher.doFinal(arr);
			//
			// return new ByteArrayInputStream(arr2);

			binary = node.getProperty(Property.JCR_DATA).getBinary();
			encrypted = binary.getStream();
			return new CipherInputStream(encrypted, cipher);
		} catch (Exception e) {
			throw new ArgeoException("Cannot decrypt", e);
		} finally {
			IOUtils.closeQuietly(encrypted);
			JcrUtils.closeQuietly(binary);
		}
	}

	protected Cipher createCipher() {
		try {
			Node userHome = JcrUtils.getUserHome(session);
			if (!userHome.hasNode(ARGEO_KEYRING))
				throw new ArgeoException("Keyring not setup");
			Node keyring = userHome.getNode(ARGEO_KEYRING);
			Cipher cipher = Cipher.getInstance(keyring
					.getProperty(ARGEO_CIPHER).getString());
			return cipher;
		} catch (Exception e) {
			throw new ArgeoException("Cannot get cipher", e);
		}
	}

	public void changePassword(char[] oldPassword, char[] newPassword) {
		// TODO Auto-generated method stub

	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
