package org.argeo.cms.bc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.CmsLog;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

/** Utilities around the BouncyCastle crypto library. */
public class BcUtils {
	private final static CmsLog log = CmsLog.getLog(BcUtils.class);

	private final static String BC_SECURITY_PROVIDER;
	static {
		Security.addProvider(new BouncyCastleProvider());
		BC_SECURITY_PROVIDER = "BC";
	}

	public static void createSelfSignedKeyStore(Path keyStorePath, char[] keyStorePassword, String keyStoreType) {
		// for (Provider provider : Security.getProviders())
		// System.out.println(provider.getName());
		// File keyStoreFile = keyStorePath.toFile();
		char[] keyPwd = Arrays.copyOf(keyStorePassword, keyStorePassword.length);
		if (!Files.exists(keyStorePath)) {
			try {
				Files.createDirectories(keyStorePath.getParent());
				KeyStore keyStore = getKeyStore(keyStorePath, keyStorePassword, keyStoreType);
				generateSelfSignedCertificate(keyStore,
						new X500Principal("CN=" + InetAddress.getLocalHost().getHostName() + ",OU=UNSECURE,O=UNSECURE"),
						1024, keyPwd);
				saveKeyStore(keyStorePath, keyStorePassword, keyStore);
				if (log.isDebugEnabled())
					log.debug("Created self-signed unsecure keystore " + keyStorePath);
			} catch (Exception e) {
				try {
					if (Files.size(keyStorePath) == 0)
						Files.delete(keyStorePath);
				} catch (IOException e1) {
					// silent
				}
				log.error("Cannot create keystore " + keyStorePath, e);
			}
		} else {
			throw new IllegalStateException("Keystore " + keyStorePath + " already exists");
		}
	}

	public static X509Certificate generateSelfSignedCertificate(KeyStore keyStore, X500Principal x500Principal,
			int keySize, char[] keyPassword) {
		try {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", BC_SECURITY_PROVIDER);
			kpGen.initialize(keySize, new SecureRandom());
			KeyPair pair = kpGen.generateKeyPair();
			Date notBefore = new Date(System.currentTimeMillis() - 10000);
			Date notAfter = new Date(System.currentTimeMillis() + 365 * 24L * 3600 * 1000);
			BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(x500Principal, serial, notBefore,
					notAfter, x500Principal, pair.getPublic());
			ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
					.setProvider(BC_SECURITY_PROVIDER).build(pair.getPrivate());
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC_SECURITY_PROVIDER)
					.getCertificate(certGen.build(sigGen));
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());

			keyStore.setKeyEntry(x500Principal.getName(), pair.getPrivate(), keyPassword, new Certificate[] { cert });
			return cert;
		} catch (GeneralSecurityException | OperatorCreationException e) {
			throw new RuntimeException("Cannot generate self-signed certificate", e);
		}
	}

	public static PrivateKey loadPemPrivateKey(Reader reader, char[] keyPassword) {
		try (PEMParser pemParser = new PEMParser(reader)) {
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BC_SECURITY_PROVIDER);
			Object object = pemParser.readObject();
			PrivateKeyInfo privateKeyInfo;
			if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
				if (keyPassword == null)
					throw new IllegalArgumentException("A key password is required");
				InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(keyPassword);
				privateKeyInfo = ((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv);
			} else if (object instanceof PrivateKeyInfo) {
				privateKeyInfo = (PrivateKeyInfo) object;
			} else {
				throw new IllegalArgumentException("Unsupported format for private key");
			}
			return converter.getPrivateKey(privateKeyInfo);
		} catch (IOException | OperatorCreationException | PKCSException e) {
			throw new RuntimeException("Cannot read private key", e);
		}
	}

	public static X509Certificate loadPemCertificate(Reader reader) {
		try (PEMParser pemParser = new PEMParser(reader)) {
			X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC_SECURITY_PROVIDER)
					.getCertificate(certHolder);
			return cert;
		} catch (IOException | CertificateException e) {
			throw new RuntimeException("Cannot read private key", e);
		}
	}

	private static KeyStore getKeyStore(Path keyStoreFile, char[] keyStorePassword, String keyStoreType) {
		try {
			KeyStore store = KeyStore.getInstance(keyStoreType, BC_SECURITY_PROVIDER);
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

	private static void saveKeyStore(Path keyStoreFile, char[] keyStorePassword, KeyStore keyStore) {
		try {
			try (OutputStream fis = Files.newOutputStream(keyStoreFile)) {
				keyStore.store(fis, keyStorePassword);
			}
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException("Cannot save keystore " + keyStoreFile, e);
		}
	}

	/** singleton */
	private BcUtils() {
	}
}
