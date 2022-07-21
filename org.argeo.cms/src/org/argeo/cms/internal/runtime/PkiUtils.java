package org.argeo.cms.internal.runtime;

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
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.CmsConstants;
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

/**
 * Utilities around private keys and certificate, mostly wrapping BouncyCastle
 * implementations.
 */
class PkiUtils {
	private final static CmsLog log = CmsLog.getLog(PkiUtils.class);

	final static String PKCS12 = "PKCS12";
	final static String JKS = "JKS";

	static final String DEFAULT_KEYSTORE_PATH = KernelConstants.DIR_PRIVATE + '/' + CmsConstants.NODE + ".p12";

	static final String DEFAULT_TRUSTSTORE_PATH = KernelConstants.DIR_PRIVATE + "/trusted.p12";

	static final String DEFAULT_PEM_KEY_PATH = KernelConstants.DIR_PRIVATE + '/' + CmsConstants.NODE + ".key";

	static final String DEFAULT_PEM_CERT_PATH = KernelConstants.DIR_PRIVATE + '/' + CmsConstants.NODE + ".crt";

	static final String IPA_PEM_CA_CERT_PATH = "/etc/ipa/ca.crt";

	static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

	private final static String SUN_SECURITY_PROVIDER;
	private final static String SUN_JSSE_SECURITY_PROVIDER;
	private final static String BC_SECURITY_PROVIDER;
	static {
		Security.addProvider(new BouncyCastleProvider());
		// BouncyCastle does not store trusted certificates properly
		// TODO report it
		BC_SECURITY_PROVIDER = "BC";
		SUN_SECURITY_PROVIDER = "SUN";
		SUN_JSSE_SECURITY_PROVIDER = "SunJSSE";
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

	public static KeyStore getKeyStore(Path keyStoreFile, char[] keyStorePassword, String keyStoreType) {
		try {
			KeyStore store = KeyStore.getInstance(keyStoreType, SUN_JSSE_SECURITY_PROVIDER);
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

//	public static byte[] pemToPKCS12(final String keyFile, final String cerFile, final String password)
//			throws Exception {
//		// Get the private key
//		FileReader reader = new FileReader(keyFile);
//
//		PEMReader pem = new PemReader(reader, new PasswordFinder() {
//			@Override
//			public char[] getPassword() {
//				return password.toCharArray();
//			}
//		});
//
//		PrivateKey key = ((KeyPair) pem.readObject()).getPrivate();
//
//		pem.close();
//		reader.close();
//
//		// Get the certificate
//		reader = new FileReader(cerFile);
//		pem = new PEMReader(reader);
//
//		X509Certificate cert = (X509Certificate) pem.readObject();
//
//		pem.close();
//		reader.close();
//
//		// Put them into a PKCS12 keystore and write it to a byte[]
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		KeyStore ks = KeyStore.getInstance("PKCS12");
//		ks.load(null);
//		ks.setKeyEntry("alias", (Key) key, password.toCharArray(), new java.security.cert.Certificate[] { cert });
//		ks.store(bos, password.toCharArray());
//		bos.close();
//		return bos.toByteArray();
//	}

	public static void loadPrivateCertificatePem(KeyStore keyStore, String alias, Reader key, char[] keyPassword,
			Reader cert) {
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

	public static void loadTrustedCertificatePem(KeyStore keyStore,char[] keyStorePassword, Reader cert) {
		try {
			X509Certificate certificate = loadPemCertificate(cert);
			TrustedCertificateEntry trustedCertificateEntry = new TrustedCertificateEntry(certificate);
			keyStore.setEntry(certificate.getSubjectX500Principal().getName(), trustedCertificateEntry, null);
		} catch (KeyStoreException e) {
			throw new RuntimeException("Cannot store PEM certificate", e);
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
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(SUN_SECURITY_PROVIDER)
					.getCertificate(certHolder);
			return cert;
		} catch (IOException | CertificateException e) {
			throw new RuntimeException("Cannot read private key", e);
		}
	}

	public static void main(String[] args) throws Exception {
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

}
