package org.argeo.cms.ssh;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

@SuppressWarnings("restriction")
public class SshKeyPair {
	public final static String RSA_KEY_TYPE = "ssh-rsa";

	private PublicKey publicKey;
	private PrivateKey privateKey;
	private KeyPair keyPair;

	public SshKeyPair(KeyPair keyPair) {
		super();
		this.publicKey = keyPair.getPublic();
		this.privateKey = keyPair.getPrivate();
		this.keyPair = keyPair;
	}

	public SshKeyPair(PublicKey publicKey, PrivateKey privateKey) {
		super();
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.keyPair = new KeyPair(publicKey, privateKey);
	}

	public KeyPair asKeyPair() {
		return keyPair;
	}

	public String getPublicKeyAsOpenSshString() {
		return PublicKeyEntry.toString(publicKey);
	}

	public String getPrivateKeyAsPemString(char[] password) {
		try {
			Object obj;

			if (password != null) {
				JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(
						PKCS8Generator.PBE_SHA1_3DES);
				encryptorBuilder.setPasssword(password);
				OutputEncryptor oe = encryptorBuilder.build();
				JcaPKCS8Generator gen = new JcaPKCS8Generator(privateKey, oe);
				obj = gen.generate();
			} else {
				obj = privateKey;
			}

			StringWriter sw = new StringWriter();
			JcaPEMWriter pemWrt = new JcaPEMWriter(sw);
			pemWrt.writeObject(obj);
			pemWrt.close();
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException("Cannot convert private key", e);
		}
	}

	public static SshKeyPair loadOrGenerate(Path privateKeyPath, int size, char[] password) {
		try {
			SshKeyPair sshKeyPair;
			if (Files.exists(privateKeyPath)) {
//				String privateKeyStr = new String(Files.readAllBytes(privateKeyPath), StandardCharsets.US_ASCII);
				sshKeyPair = load(
						new InputStreamReader(Files.newInputStream(privateKeyPath), StandardCharsets.US_ASCII),
						password);
				// TOD make sure public key is consistemt
			} else {
				sshKeyPair = generate(size);
				Files.write(privateKeyPath,
						sshKeyPair.getPrivateKeyAsPemString(password).getBytes(StandardCharsets.US_ASCII));
				Path publicKeyPath = privateKeyPath.resolveSibling(privateKeyPath.getFileName() + ".pub");
				Files.write(publicKeyPath,
						sshKeyPair.getPublicKeyAsOpenSshString().getBytes(StandardCharsets.US_ASCII));
			}
			return sshKeyPair;
		} catch (IOException e) {
			throw new RuntimeException("Cannot read or write private key " + privateKeyPath, e);
		}
	}

	public static SshKeyPair generate(int size) {
		return generate(RSA_KEY_TYPE, size);
	}

	public static SshKeyPair generate(String keyType, int size) {
		try {
			KeyPair keyPair = KeyUtils.generateKeyPair(keyType, size);
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			return new SshKeyPair(publicKey, privateKey);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Cannot generate SSH key", e);
		}
	}

	public static SshKeyPair load(Reader reader, char[] password) {
		try (PEMParser pemParser = new PEMParser(reader)) {
			Object object = pemParser.readObject();
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter();// .setProvider("BC");
			KeyPair kp;
			if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
				// Encrypted key - we will use provided password
				PKCS8EncryptedPrivateKeyInfo ckp = (PKCS8EncryptedPrivateKeyInfo) object;
//				PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
				InputDecryptorProvider inputDecryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
						.build(password);
				PrivateKeyInfo pkInfo = ckp.decryptPrivateKeyInfo(inputDecryptorProvider);
				PrivateKey privateKey = converter.getPrivateKey(pkInfo);

				// generate public key
				RSAPrivateCrtKey privk = (RSAPrivateCrtKey) privateKey;
				RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privk.getModulus(),
						privk.getPublicExponent());
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

				kp = new KeyPair(publicKey, privateKey);
			} else {
				// Unencrypted key - no password needed
//				PKCS8EncryptedPrivateKeyInfo ukp = (PKCS8EncryptedPrivateKeyInfo) object;
				PEMKeyPair pemKp = (PEMKeyPair) object;
				kp = converter.getKeyPair(pemKp);
			}
			return new SshKeyPair(kp);
		} catch (Exception e) {
			throw new RuntimeException("Cannot load private key", e);
		}
	}

	public static void main(String args[]) {
		Path privateKeyPath = Paths.get(System.getProperty("user.dir") + "/id_rsa");
		SshKeyPair skp = SshKeyPair.loadOrGenerate(privateKeyPath, 1024, null);
		System.out.println("Public:\n" + skp.getPublicKeyAsOpenSshString());
		System.out.println("Private (plain):\n" + skp.getPrivateKeyAsPemString(null));
		System.out.println("Private (encrypted):\n" + skp.getPrivateKeyAsPemString("demo".toCharArray()));

		StringReader reader = new StringReader(skp.getPrivateKeyAsPemString(null));
		skp = SshKeyPair.load(reader, null);
		System.out.println("Public:\n" + skp.getPublicKeyAsOpenSshString());
		System.out.println("Private (plain):\n" + skp.getPrivateKeyAsPemString(null));
		System.out.println("Private (encrypted):\n" + skp.getPrivateKeyAsPemString("demo".toCharArray()));

		reader = new StringReader(skp.getPrivateKeyAsPemString("demo".toCharArray()));
		skp = SshKeyPair.load(reader, "demo".toCharArray());
		System.out.println("Public:\n" + skp.getPublicKeyAsOpenSshString());
		System.out.println("Private (plain):\n" + skp.getPrivateKeyAsPemString(null));
		System.out.println("Private (encrypted):\n" + skp.getPrivateKeyAsPemString("demo".toCharArray()));
	}

}
