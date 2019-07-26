package org.argeo.ident;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

/**
 * A simple ident client, supporting authd OpenSSL encrypted username.
 * 
 * @see RFC 1413 https://tools.ietf.org/html/rfc1413
 */
public class IdentClient {
	private String host = "localhost";
	private int port = 113;

	private OpenSslDecryptor openSslDecryptor = new OpenSslDecryptor();
	private String identPassphrase = "changeit";

	public IdentClient(String host, String identPassphrase) {
		this(host, identPassphrase, 113);
	}

	public IdentClient(String host, String identPassphrase, int port) {
		this.host = host;
		this.identPassphrase = identPassphrase;
		this.port = port;
	}

	public String getUsername(int serverPort, int clientPort) {
		String answer;
		try (Socket socket = new Socket(host, port)) {
			String msg = serverPort + "," + clientPort + "\n";
			OutputStream out = socket.getOutputStream();
			out.write(msg.getBytes(StandardCharsets.US_ASCII));
			out.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			answer = reader.readLine();
		} catch (Exception e) {
			throw new RuntimeException("Cannot read from ident server on " + host + ":" + port, e);
		}
		StringTokenizer st = new StringTokenizer(answer, " :\n");
		String username = null;
		while (st.hasMoreTokens())
			username = st.nextToken();
		if (username.startsWith("[")) {
			String encrypted = username.substring(1, username.length() - 1);
			username = openSslDecryptor.decryptAuthd(encrypted, identPassphrase).trim();
		}
//		System.out.println(username);
		return username;
	}

	public void setOpenSslDecryptor(OpenSslDecryptor openSslDecryptor) {
		this.openSslDecryptor = openSslDecryptor;
	}

	public static void main(String[] args) {
		IdentClient identClient = new IdentClient("127.0.0.1", "changeit");
		String username = identClient.getUsername(7070, 55958);
		System.out.println(username);
	}
}
