package org.argeo.cms.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.util.PasswordEncryption;

public class PasswordBasedEncryptionTest extends TestCase {
	private final static Log log = LogFactory.getLog(PasswordBasedEncryptionTest.class);

	public void testEncryptDecrypt() {
		final String password = "test long password since they are safer";
		PasswordEncryption pbeEnc = new PasswordEncryption(password.toCharArray());
		String message = "Hello World!";
		log.info("Password:\t'" + password + "'");
		log.info("Message:\t'" + message + "'");
		byte[] encrypted = pbeEnc.encryptString(message);
		log.info("Encrypted:\t'" + Base64.getEncoder().encode(encrypted) + "'");
		PasswordEncryption pbeDec = new PasswordEncryption(password.toCharArray());
		InputStream in = null;
		in = new ByteArrayInputStream(encrypted);
		String decrypted = pbeDec.decryptAsString(in);
		log.info("Decrypted:\t'" + decrypted + "'");
		IOUtils.closeQuietly(in);
		assertEquals(message, decrypted);
	}

	public void testPBEWithMD5AndDES() throws Exception {
		String password = "test";
		String message = "Hello World!";

		byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee,
				(byte) 0x99 };

		int count = 1024;

		String cipherAlgorithm = "PBEWithMD5AndDES";
		String secretKeyAlgorithm = "PBEWithMD5AndDES";
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance(secretKeyAlgorithm);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
		SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
		Cipher ecipher = Cipher.getInstance(cipherAlgorithm);
		ecipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
		Cipher dcipher = Cipher.getInstance(cipherAlgorithm);
		dcipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

		byte[] encrypted = ecipher.doFinal(message.getBytes());
		byte[] decrypted = dcipher.doFinal(encrypted);
		assertEquals(message, new String(decrypted));

	}

	public void testPBEWithSHA1AndAES() throws Exception {
		String password = "test";
		String message = "Hello World!";

		byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee,
				(byte) 0x99 };
		byte[] iv = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee,
				(byte) 0x99, (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee,
				(byte) 0x99 };

		int count = 1024;
		// int keyLength = 256;
		int keyLength = 128;

		String cipherAlgorithm = "AES/CBC/PKCS5Padding";
		String secretKeyAlgorithm = "PBKDF2WithHmacSHA1";
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance(secretKeyAlgorithm);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, count, keyLength);
		SecretKey tmp = keyFac.generateSecret(pbeKeySpec);
		SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
		Cipher ecipher = Cipher.getInstance(cipherAlgorithm);
		ecipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));

		// decrypt
		keyFac = SecretKeyFactory.getInstance(secretKeyAlgorithm);
		pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, count, keyLength);
		tmp = keyFac.generateSecret(pbeKeySpec);
		secret = new SecretKeySpec(tmp.getEncoded(), "AES");
		// AlgorithmParameters params = ecipher.getParameters();
		// byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
		Cipher dcipher = Cipher.getInstance(cipherAlgorithm);
		dcipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

		byte[] encrypted = ecipher.doFinal(message.getBytes());
		byte[] decrypted = dcipher.doFinal(encrypted);
		assertEquals(message, new String(decrypted));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CipherOutputStream cipherOut = new CipherOutputStream(out, ecipher);
		cipherOut.write(message.getBytes());
		IOUtils.closeQuietly(cipherOut);
		byte[] enc = out.toByteArray();

		ByteArrayInputStream in = new ByteArrayInputStream(enc);
		CipherInputStream cipherIn = new CipherInputStream(in, dcipher);
		ByteArrayOutputStream dec = new ByteArrayOutputStream();
		IOUtils.copy(cipherIn, dec);
		assertEquals(message, new String(dec.toByteArray()));
	}
}
