package org.argeo.ident;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A simple ident client, supporting authd OpenSSL encrypted username.
 * 
 * @see RFC 1413 https://tools.ietf.org/html/rfc1413
 */
public class IdentClient {
	public final static int DEFAULT_IDENT_PORT = 113;
	public final static String AUTHD_PASSPHRASE_PATH = "/etc/ident.key";
	final static String NO_USER = "NO-USER";

	private final String host;
	private final int port;

	private OpenSslDecryptor openSslDecryptor = new OpenSslDecryptor();
	private String identPassphrase = null;

	public IdentClient(String host) {
		this(host, readPassphrase(AUTHD_PASSPHRASE_PATH), DEFAULT_IDENT_PORT);
	}

	public IdentClient(String host, Path passPhrasePath) {
		this(host, readPassphrase(passPhrasePath), DEFAULT_IDENT_PORT);
	}

	public IdentClient(String host, String identPassphrase) {
		this(host, identPassphrase, DEFAULT_IDENT_PORT);
	}

	public IdentClient(String host, String identPassphrase, int port) {
		this.host = host;
		this.identPassphrase = identPassphrase;
		this.port = port;
	}

	/** @return the username or null if none */
	public String getUsername(int serverPort, int clientPort) {
		String answer;
		try (Socket socket = new Socket(host, port)) {
			String msg = serverPort + "," + clientPort + "\n";
			OutputStream out = socket.getOutputStream();
			out.write(msg.getBytes(StandardCharsets.US_ASCII));
			out.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			answer = reader.readLine();
		} catch (ConnectException e) {
			System.err
					.println("Ident client is configured but no ident server available on " + host + " (port " + port + ")");
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Cannot read from ident server on " + host + " (port " + port + ")", e);
		}
		StringTokenizer st = new StringTokenizer(answer, " :\n");
		String username = null;
		while (st.hasMoreTokens())
			username = st.nextToken();

		if (username.equals(NO_USER))
			return null;

		if (identPassphrase != null && username.startsWith("[")) {
			String encrypted = username.substring(1, username.length() - 1);
			username = openSslDecryptor.decryptAuthd(encrypted, identPassphrase).trim();
		}
//		System.out.println(username);
		return username;
	}

	public void setOpenSslDecryptor(OpenSslDecryptor openSslDecryptor) {
		this.openSslDecryptor = openSslDecryptor;
	}

	public static String readPassphrase(String filePath) {
		return readPassphrase(Paths.get(filePath));
	}

	/** @return the first line of the file. */
	public static String readPassphrase(Path path) {
		if (!isPathAvailable(path))
			return null;
		List<String> lines;
		try {
			lines = Files.readAllLines(path);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read " + path, e);
		}
		if (lines.size() == 0)
			return null;
		String passphrase = lines.get(0);
		return passphrase;
	}

	public static boolean isDefaultAuthdPassphraseFileAvailable() {
		return isPathAvailable(Paths.get(AUTHD_PASSPHRASE_PATH));
	}

	public static boolean isPathAvailable(Path path) {
		if (!Files.exists(path))
			return false;
		if (!Files.isReadable(path))
			return false;
		return true;
	}

	public static void main(String[] args) {
		IdentClient identClient = new IdentClient("127.0.0.1", "changeit");
		String username = identClient.getUsername(7070, 55958);
		System.out.println(username);
	}
}
