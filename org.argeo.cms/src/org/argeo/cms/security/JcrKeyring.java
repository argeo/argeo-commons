/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.cms.security;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.api.security.PBEKeySpecCallback;
import org.argeo.cms.ArgeoNames;
import org.argeo.cms.ArgeoTypes;
import org.argeo.cms.CmsException;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.jcr.JcrUtils;

/** JCR based implementation of a keyring */
public class JcrKeyring extends AbstractKeyring implements ArgeoNames {
	private final static Log log = LogFactory.getLog(JcrKeyring.class);
	/**
	 * Stronger with 256, but causes problem with Oracle JVM, force 128 in this case
	 */
	public final static Long DEFAULT_SECRETE_KEY_LENGTH = 256l;
	public final static String DEFAULT_SECRETE_KEY_FACTORY = "PBKDF2WithHmacSHA1";
	public final static String DEFAULT_SECRETE_KEY_ENCRYPTION = "AES";
	public final static String DEFAULT_CIPHER_NAME = "AES/CBC/PKCS5Padding";

	private Integer iterationCountFactor = 200;
	private Long secretKeyLength = DEFAULT_SECRETE_KEY_LENGTH;
	private String secretKeyFactoryName = DEFAULT_SECRETE_KEY_FACTORY;
	private String secretKeyEncryption = DEFAULT_SECRETE_KEY_ENCRYPTION;
	private String cipherName = DEFAULT_CIPHER_NAME;

	private final Repository repository;
	// TODO remove thread local session ; open a session each time
	private ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<Session>() {

		@Override
		protected Session initialValue() {
			return login();
		}

	};

	// FIXME is it really still needed?
	/**
	 * When setup is called the session has not yet been saved and we don't want to
	 * save it since there maybe other data which would be inconsistent. So we keep
	 * a reference to this node which will then be used (an reset to null) when
	 * handling the PBE callback. We keep one per thread in case multiple users are
	 * accessing the same instance of a keyring.
	 */
	// private ThreadLocal<Node> notYetSavedKeyring = new ThreadLocal<Node>() {
	//
	// @Override
	// protected Node initialValue() {
	// return null;
	// }
	// };

	public JcrKeyring(Repository repository) {
		this.repository = repository;
	}

	private Session session() {
		Session session = this.sessionThreadLocal.get();
		if (!session.isLive()) {
			session = login();
			sessionThreadLocal.set(session);
		}
		return session;
	}

	private Session login() {
		try {
			return repository.login(NodeConstants.HOME);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot login key ring session", e);
		}
	}

	@Override
	protected synchronized Boolean isSetup() {
		Session session = null;
		try {
			// if (notYetSavedKeyring.get() != null)
			// return true;
			session = session();
			session.refresh(true);
			Node userHome = NodeUtils.getUserHome(session);
			return userHome.hasNode(ARGEO_KEYRING);
		} catch (RepositoryException e) {
			throw new ArgeoJcrException("Cannot check whether keyring is setup", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	@Override
	protected synchronized void setup(char[] password) {
		Binary binary = null;
		// InputStream in = null;
		try {
			session().refresh(true);
			Node userHome = NodeUtils.getUserHome(session());
			Node keyring;
			if (userHome.hasNode(ARGEO_KEYRING)) {
				throw new CmsException("Keyring already set up");
			} else {
				keyring = userHome.addNode(ARGEO_KEYRING);
			}
			keyring.addMixin(ArgeoTypes.ARGEO_PBE_SPEC);

			// deterministic salt and iteration count based on username
			String username = session().getUserID();
			byte[] salt = new byte[8];
			byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
			for (int i = 0; i < salt.length; i++) {
				if (i < usernameBytes.length)
					salt[i] = usernameBytes[i];
				else
					salt[i] = 0;
			}
			try (InputStream in = new ByteArrayInputStream(salt);) {
				binary = session().getValueFactory().createBinary(in);
			}
			keyring.setProperty(ARGEO_SALT, binary);

			Integer iterationCount = username.length() * iterationCountFactor;
			keyring.setProperty(ARGEO_ITERATION_COUNT, iterationCount);

			// default algo
			// TODO check if algo and key length are available, use DES if not
			keyring.setProperty(ARGEO_SECRET_KEY_FACTORY, secretKeyFactoryName);
			keyring.setProperty(ARGEO_KEY_LENGTH, secretKeyLength);
			keyring.setProperty(ARGEO_SECRET_KEY_ENCRYPTION, secretKeyEncryption);
			keyring.setProperty(ARGEO_CIPHER, cipherName);

			keyring.getSession().save();

			// encrypted password hash
			// IOUtils.closeQuietly(in);
			// JcrUtils.closeQuietly(binary);
			// byte[] btPass = hash(password, salt, iterationCount);
			// in = new ByteArrayInputStream(btPass);
			// binary = session().getValueFactory().createBinary(in);
			// keyring.setProperty(ARGEO_PASSWORD, binary);

			// notYetSavedKeyring.set(keyring);
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot setup keyring", e);
		} finally {
			JcrUtils.closeQuietly(binary);
			// IOUtils.closeQuietly(in);
			// JcrUtils.discardQuietly(session());
		}
	}

	@Override
	protected synchronized void handleKeySpecCallback(PBEKeySpecCallback pbeCallback) {
		Session session = null;
		try {
			session = session();
			session.refresh(true);
			Node userHome = NodeUtils.getUserHome(session);
			Node keyring;
			if (userHome.hasNode(ARGEO_KEYRING))
				keyring = userHome.getNode(ARGEO_KEYRING);
			// else if (notYetSavedKeyring.get() != null)
			// keyring = notYetSavedKeyring.get();
			else
				throw new ArgeoJcrException("Keyring not setup");

			pbeCallback.set(keyring.getProperty(ARGEO_SECRET_KEY_FACTORY).getString(),
					JcrUtils.getBinaryAsBytes(keyring.getProperty(ARGEO_SALT)),
					(int) keyring.getProperty(ARGEO_ITERATION_COUNT).getLong(),
					(int) keyring.getProperty(ARGEO_KEY_LENGTH).getLong(),
					keyring.getProperty(ARGEO_SECRET_KEY_ENCRYPTION).getString());

			// if (notYetSavedKeyring.get() != null)
			// notYetSavedKeyring.remove();
		} catch (RepositoryException e) {
			throw new ArgeoJcrException("Cannot handle key spec callback", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	/** The parent node must already exist at this path. */
	@Override
	protected synchronized void encrypt(String path, InputStream unencrypted) {
		// should be called first for lazy initialization
		SecretKey secretKey = getSecretKey(null);
		Cipher cipher = createCipher();

		// Binary binary = null;
		// InputStream in = null;
		try {
			session().refresh(true);
			Node node;
			if (!session().nodeExists(path)) {
				String parentPath = JcrUtils.parentPath(path);
				if (!session().nodeExists(parentPath))
					throw new ArgeoJcrException("No parent node of " + path);
				Node parentNode = session().getNode(parentPath);
				node = parentNode.addNode(JcrUtils.nodeNameFromPath(path));
			} else {
				node = session().getNode(path);
			}
			encrypt(secretKey, cipher, node, unencrypted);
			// node.addMixin(ArgeoTypes.ARGEO_ENCRYPTED);
			// SecureRandom random = new SecureRandom();
			// byte[] iv = new byte[16];
			// random.nextBytes(iv);
			// cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
			// JcrUtils.setBinaryAsBytes(node, ARGEO_IV, iv);
			//
			// try (InputStream in = new CipherInputStream(unencrypted, cipher);) {
			// binary = session().getValueFactory().createBinary(in);
			// node.setProperty(Property.JCR_DATA, binary);
			// session().save();
			// }
		} catch (RepositoryException e) {
			throw new ArgeoJcrException("Cannot encrypt", e);
		} finally {
			try {
				unencrypted.close();
			} catch (IOException e) {
				// silent
			}
			// IOUtils.closeQuietly(unencrypted);
			// IOUtils.closeQuietly(in);
			// JcrUtils.closeQuietly(binary);
			JcrUtils.logoutQuietly(session());
		}
	}

	protected synchronized void encrypt(SecretKey secretKey, Cipher cipher, Node node, InputStream unencrypted) {
		try {
			node.addMixin(ArgeoTypes.ARGEO_ENCRYPTED);
			SecureRandom random = new SecureRandom();
			byte[] iv = new byte[16];
			random.nextBytes(iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
			JcrUtils.setBinaryAsBytes(node, ARGEO_IV, iv);

			Binary binary = null;
			try (InputStream in = new CipherInputStream(unencrypted, cipher);) {
				binary = session().getValueFactory().createBinary(in);
				node.setProperty(Property.JCR_DATA, binary);
				session().save();
			} finally {
				JcrUtils.closeQuietly(binary);
			}
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot encrypt", e);
		} finally {
			try {
				unencrypted.close();
			} catch (IOException e) {
				// silent
			}
			// IOUtils.closeQuietly(unencrypted);
			// IOUtils.closeQuietly(in);
			// JcrUtils.closeQuietly(binary);
			// JcrUtils.logoutQuietly(session());
		}
	}

	@Override
	protected synchronized InputStream decrypt(String path) {
		Binary binary = null;
		// InputStream encrypted = null;
		try {
			session().refresh(true);
			if (!session().nodeExists(path)) {
				char[] password = ask();
				Reader reader = new CharArrayReader(password);
				return new ByteArrayInputStream(IOUtils.toByteArray(reader, StandardCharsets.UTF_8));
			} else {
				// should be called first for lazy initialisation
				SecretKey secretKey = getSecretKey(null);
				Cipher cipher = createCipher();
				Node node = session().getNode(path);
				return decrypt(secretKey, cipher, node);
			}
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot decrypt", e);
		} finally {
			// IOUtils.closeQuietly(encrypted);
			// IOUtils.closeQuietly(reader);
			JcrUtils.closeQuietly(binary);
			JcrUtils.logoutQuietly(session());
		}
	}

	protected synchronized InputStream decrypt(SecretKey secretKey, Cipher cipher, Node node)
			throws RepositoryException, GeneralSecurityException {
		if (node.hasProperty(ARGEO_IV)) {
			byte[] iv = JcrUtils.getBinaryAsBytes(node.getProperty(ARGEO_IV));
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
		} else {
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
		}

		Binary binary = node.getProperty(Property.JCR_DATA).getBinary();
		InputStream encrypted = binary.getStream();
		return new CipherInputStream(encrypted, cipher);
	}

	protected Cipher createCipher() {
		try {
			Node userHome = NodeUtils.getUserHome(session());
			if (!userHome.hasNode(ARGEO_KEYRING))
				throw new ArgeoJcrException("Keyring not setup");
			Node keyring = userHome.getNode(ARGEO_KEYRING);
			String cipherName = keyring.getProperty(ARGEO_CIPHER).getString();
			Provider securityProvider = getSecurityProvider();
			Cipher cipher;
			if (securityProvider == null)// TODO use BC?
				cipher = Cipher.getInstance(cipherName);
			else
				cipher = Cipher.getInstance(cipherName, securityProvider);
			return cipher;
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot get cipher", e);
		}
	}

	public synchronized void changePassword(char[] oldPassword, char[] newPassword) {
		// TODO make it XA compatible
		SecretKey oldSecretKey = getSecretKey(oldPassword);
		SecretKey newSecretKey = getSecretKey(newPassword);
		Session session = session();
		try {
			NodeIterator encryptedNodes = session.getWorkspace().getQueryManager()
					.createQuery("select * from [argeo:encrypted]", Query.JCR_SQL2).execute().getNodes();
			while (encryptedNodes.hasNext()) {
				Node node = encryptedNodes.nextNode();
				InputStream in = decrypt(oldSecretKey, createCipher(), node);
				encrypt(newSecretKey, createCipher(), node, in);
				if (log.isDebugEnabled())
					log.debug("Converted keyring encrypted value of " + node.getPath());
			}
		} catch (RepositoryException | GeneralSecurityException e) {
			throw new CmsException("Cannot change JCR keyring password", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	// public synchronized void setSession(Session session) {
	// this.session = session;
	// }

	public void setIterationCountFactor(Integer iterationCountFactor) {
		this.iterationCountFactor = iterationCountFactor;
	}

	public void setSecretKeyLength(Long keyLength) {
		this.secretKeyLength = keyLength;
	}

	public void setSecretKeyFactoryName(String secreteKeyFactoryName) {
		this.secretKeyFactoryName = secreteKeyFactoryName;
	}

	public void setSecretKeyEncryption(String secreteKeyEncryption) {
		this.secretKeyEncryption = secreteKeyEncryption;
	}

	public void setCipherName(String cipherName) {
		this.cipherName = cipherName;
	}

}