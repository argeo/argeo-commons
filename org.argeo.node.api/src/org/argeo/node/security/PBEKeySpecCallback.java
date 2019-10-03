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
package org.argeo.node.security;

import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

/**
 * All information required to set up a {@link PBEKeySpec} bar the password
 * itself (use a {@link PasswordCallback})
 */
public class PBEKeySpecCallback implements Callback {
	private String secretKeyFactory;
	private byte[] salt;
	private Integer iterationCount;
	/** Can be null for some algorithms */
	private Integer keyLength;
	/** Can be null, will trigger secret key encryption if not */
	private String secretKeyEncryption;

	private String encryptedPasswordHashCipher;
	private byte[] encryptedPasswordHash;

	public void set(String secretKeyFactory, byte[] salt,
			Integer iterationCount, Integer keyLength,
			String secretKeyEncryption) {
		this.secretKeyFactory = secretKeyFactory;
		this.salt = salt;
		this.iterationCount = iterationCount;
		this.keyLength = keyLength;
		this.secretKeyEncryption = secretKeyEncryption;
//		this.encryptedPasswordHashCipher = encryptedPasswordHashCipher;
//		this.encryptedPasswordHash = encryptedPasswordHash;
	}

	public String getSecretKeyFactory() {
		return secretKeyFactory;
	}

	public byte[] getSalt() {
		return salt;
	}

	public Integer getIterationCount() {
		return iterationCount;
	}

	public Integer getKeyLength() {
		return keyLength;
	}

	public String getSecretKeyEncryption() {
		return secretKeyEncryption;
	}

	public String getEncryptedPasswordHashCipher() {
		return encryptedPasswordHashCipher;
	}

	public byte[] getEncryptedPasswordHash() {
		return encryptedPasswordHash;
	}

}
