package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.argeo.cms.CmsException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Utilities around private keys and certificate, mostly wrapping BouncyCastle
 * implementations.
 */
class PkiUtils {
	private final static String SECURITY_PROVIDER;
	static {
		Security.addProvider(new BouncyCastleProvider());
		SECURITY_PROVIDER = "BC";
	}

	public static X509Certificate generateSelfSignedCertificate(KeyStore keyStore, X500Principal x500Principal,
			int keySize, char[] keyPassword) {
		try {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", SECURITY_PROVIDER);
			kpGen.initialize(keySize, new SecureRandom());
			KeyPair pair = kpGen.generateKeyPair();
			Date notBefore = new Date(System.currentTimeMillis() - 10000);
			Date notAfter = new Date(System.currentTimeMillis() + 365 * 24L * 3600 * 1000);
			BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(x500Principal, serial, notBefore,
					notAfter, x500Principal, pair.getPublic());
			ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(SECURITY_PROVIDER)
					.build(pair.getPrivate());
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(SECURITY_PROVIDER)
					.getCertificate(certGen.build(sigGen));
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());

			keyStore.setKeyEntry(x500Principal.getName(), pair.getPrivate(), keyPassword, new Certificate[] { cert });
			return cert;
		} catch (Exception e) {
			throw new CmsException("Cannot generate self-signed certificate", e);
		}
	}

	public static KeyStore getKeyStore(File keyStoreFile, char[] keyStorePassword) {
		try {
			KeyStore store = KeyStore.getInstance("PKCS12", SECURITY_PROVIDER);
			if (keyStoreFile.exists()) {
				try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
					store.load(fis, keyStorePassword);
				}
			} else {
				store.load(null);
			}
			return store;
		} catch (Exception e) {
			throw new CmsException("Cannot load keystore " + keyStoreFile, e);
		}
	}

	public static void saveKeyStore(File keyStoreFile, char[] keyStorePassword, KeyStore keyStore) {
		try {
			try (FileOutputStream fis = new FileOutputStream(keyStoreFile)) {
				keyStore.store(fis, keyStorePassword);
			}
		} catch (Exception e) {
			throw new CmsException("Cannot save keystore " + keyStoreFile, e);
		}
	}

	public static void main(String[] args) {
		final String ALGORITHM = "RSA";
		final String provider = "BC";
		SecureRandom secureRandom = new SecureRandom();
		long begin = System.currentTimeMillis();
		for (int i = 512; i < 1024; i = i + 2) {
			try {
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM, provider);
				keyGen.initialize(i, secureRandom);
				keyGen.generateKeyPair();
			} catch (Exception e) {
				System.err.println(i + " : " + e.getMessage());
			}
		}
		System.out.println((System.currentTimeMillis() - begin) + " ms");

		// // String text = "a";
		// String text =
		// "testtesttesttesttesttesttesttesttesttesttesttesttesttesttest";
		// try {
		// System.out.println(text);
		// PrivateKey privateKey;
		// PublicKey publicKey;
		// char[] password = "changeit".toCharArray();
		// String alias = "CN=test";
		// KeyStore keyStore = KeyStore.getInstance("pkcs12");
		// File p12file = new File("test.p12");
		// p12file.delete();
		// if (!p12file.exists()) {
		// keyStore.load(null);
		// generateSelfSignedCertificate(keyStore, new X500Principal(alias),
		// 513, password);
		// try (OutputStream out = new FileOutputStream(p12file)) {
		// keyStore.store(out, password);
		// }
		// }
		// try (InputStream in = new FileInputStream(p12file)) {
		// keyStore.load(in, password);
		// privateKey = (PrivateKey) keyStore.getKey(alias, password);
		// publicKey = keyStore.getCertificateChain(alias)[0].getPublicKey();
		// }
		// // KeyPair key;
		// // final KeyPairGenerator keyGen =
		// // KeyPairGenerator.getInstance(ALGORITHM);
		// // keyGen.initialize(4096, new SecureRandom());
		// // long begin = System.currentTimeMillis();
		// // key = keyGen.generateKeyPair();
		// // System.out.println((System.currentTimeMillis() - begin) + " ms");
		// // keyStore.load(null);
		// // keyStore.setKeyEntry("test", key.getPrivate(), password, null);
		// // try(OutputStream out=new FileOutputStream(p12file)) {
		// // keyStore.store(out, password);
		// // }
		// // privateKey = key.getPrivate();
		// // publicKey = key.getPublic();
		//
		// Cipher encrypt = Cipher.getInstance(ALGORITHM);
		// encrypt.init(Cipher.ENCRYPT_MODE, publicKey);
		// byte[] encrypted = encrypt.doFinal(text.getBytes());
		// String encryptedBase64 =
		// Base64.getEncoder().encodeToString(encrypted);
		// System.out.println(encryptedBase64);
		// byte[] encryptedFromBase64 =
		// Base64.getDecoder().decode(encryptedBase64);
		//
		// Cipher decrypt = Cipher.getInstance(ALGORITHM);
		// decrypt.init(Cipher.DECRYPT_MODE, privateKey);
		// byte[] decrypted = decrypt.doFinal(encryptedFromBase64);
		// System.out.println(new String(decrypted));
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

}
