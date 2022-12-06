package org.argeo.cms.internal.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * Utilities around private keys and certificate, mostly wrapping BouncyCastle
 * implementations.
 */
class PkiUtils {
	public static KeyStore getKeyStore(Path keyStoreFile, char[] keyStorePassword, String keyStoreType) {
		try {
			KeyStore store = KeyStore.getInstance(keyStoreType);
			if (Files.exists(keyStoreFile)) {
				try (InputStream fis = Files.newInputStream(keyStoreFile)) {
					store.load(fis, keyStorePassword);
				}
			} else {
				store.load(null);
			}
			return store;
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException("Cannot load keystore " + keyStoreFile, e);
		}
	}

	public static void saveKeyStore(Path keyStoreFile, char[] keyStorePassword, KeyStore keyStore) {
		try {
			try (OutputStream fis = Files.newOutputStream(keyStoreFile)) {
				keyStore.store(fis, keyStorePassword);
			}
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException("Cannot save keystore " + keyStoreFile, e);
		}
	}

	public static void loadPrivateCertificatePem(KeyStore keyStore, String alias, Reader key, char[] keyPassword,
			BufferedInputStream cert) {
		Objects.requireNonNull(keyStore);
		Objects.requireNonNull(key);
		try {
			X509Certificate certificate = loadPemCertificate(cert);
			PrivateKey privateKey = loadPemPrivateKey(key, keyPassword);
			keyStore.setKeyEntry(alias, privateKey, keyPassword, new java.security.cert.Certificate[] { certificate });
		} catch (KeyStoreException e) {
			throw new RuntimeException("Cannot store PEM certificate", e);
		}
	}

	public static void loadTrustedCertificatePem(KeyStore keyStore, char[] keyStorePassword, BufferedInputStream cert) {
		try {
			X509Certificate certificate = loadPemCertificate(cert);
			TrustedCertificateEntry trustedCertificateEntry = new TrustedCertificateEntry(certificate);
			keyStore.setEntry(certificate.getSubjectX500Principal().getName(), trustedCertificateEntry, null);
		} catch (KeyStoreException e) {
			throw new RuntimeException("Cannot store PEM certificate", e);
		}
	}

	public static PrivateKey loadPemPrivateKey(Reader reader, char[] keyPassword) {
		try {
			StringBuilder key = new StringBuilder();
			try (BufferedReader in = new BufferedReader(reader)) {
				String line = in.readLine();
				if (!"-----BEGIN PRIVATE KEY-----".equals(line))
					throw new IllegalArgumentException("Not a PEM private key");
				lines: while ((line = in.readLine()) != null) {
					if ("-----END PRIVATE KEY-----".equals(line))
						break lines;
					key.append(line);
				}
			}

			byte[] encoded = Base64.getDecoder().decode(key.toString());

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
			throw new RuntimeException("Cannot load PEM key", e);
		}

	}

	public static X509Certificate loadPemCertificate(BufferedInputStream in) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
			@SuppressWarnings("unchecked")
			Collection<X509Certificate> certificates = (Collection<X509Certificate>) certificateFactory
					.generateCertificates(in);
			if (certificates.isEmpty())
				throw new IllegalArgumentException("No certificate found");
			if (certificates.size() != 1)
				throw new IllegalArgumentException(certificates.size() + " certificates found");
			return certificates.iterator().next();
		} catch (CertificateException e) {
			throw new IllegalStateException("cannot load certifciate", e);
		}
	}
}
