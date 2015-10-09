package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.argeo.ArgeoException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Utilities around private keys and certificate, mostly wrapping BouncyCastle
 * implementations.
 */
public class PkiUtils {
	private final static String SECURITY_PROVIDER;
	static {
		// Security.addProvider(new BouncyCastleProvider());
		SECURITY_PROVIDER = "BC";
	}

	public static X509Certificate generateSelfSignedCertificate(
			KeyStore keyStore, X500Principal x500Principal, char[] keyPassword) {
		try {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA",
					SECURITY_PROVIDER);
			kpGen.initialize(1024, new SecureRandom());
			KeyPair pair = kpGen.generateKeyPair();
			Date notBefore = new Date(System.currentTimeMillis() - 10000);
			Date notAfter = new Date(
					System.currentTimeMillis() + 24L * 3600 * 1000);
			BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
					x500Principal, serial, notBefore, notAfter, x500Principal,
					pair.getPublic());
			ContentSigner sigGen = new JcaContentSignerBuilder(
					"SHA256WithRSAEncryption").setProvider(SECURITY_PROVIDER)
					.build(pair.getPrivate());
			X509Certificate cert = new JcaX509CertificateConverter()
					.setProvider(SECURITY_PROVIDER).getCertificate(
							certGen.build(sigGen));
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());

			keyStore.setKeyEntry(x500Principal.getName(), pair.getPrivate(),
					keyPassword, new Certificate[] { cert });
			return cert;
		} catch (Exception e) {
			throw new ArgeoException("Cannot generate self-signed certificate",
					e);
		}
	}

	public static KeyStore getKeyStore(File keyStoreFile,
			char[] keyStorePassword) {
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
			throw new ArgeoException("Cannot load keystore " + keyStoreFile, e);
		}
	}

	public static void saveKeyStore(File keyStoreFile, char[] keyStorePassword,
			KeyStore keyStore) {
		try {
			try (FileOutputStream fis = new FileOutputStream(keyStoreFile)) {
				keyStore.store(fis, keyStorePassword);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot save keystore " + keyStoreFile, e);
		}
	}

}
