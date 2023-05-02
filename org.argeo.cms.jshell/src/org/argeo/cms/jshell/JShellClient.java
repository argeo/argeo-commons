package org.argeo.cms.jshell;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.UUID;

public class JShellClient {
	public final static String STDIO = "stdio";
	public final static String STDERR = "stderr";
	public final static String CMDIO = "cmdio";

	private static String ttyConfig;

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			Path targetStateDirectory = Paths.get(args[0]);
			Path localBase = targetStateDirectory.resolve("jsh");
			if (Files.isSymbolicLink(localBase)) {
				localBase = localBase.toRealPath();
			}

			Console console = System.console();
			if (console != null) {
				toRawTerminal();
			}

			SocketPipeSource std = new SocketPipeSource();
			std.setInputStream(System.in);
			std.setOutputStream(System.out);

			UUID uuid = UUID.randomUUID();
			Path sessionDir = localBase.resolve(uuid.toString());
			Files.createDirectory(sessionDir);
			Path stdPath = sessionDir.resolve(JShellClient.STDIO);

			// wait for sockets to be available
			WatchService watchService = FileSystems.getDefault().newWatchService();
			sessionDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
			WatchKey key;
			watch: while ((key = watchService.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					Path path = sessionDir.resolve((Path) event.context());
					if (Files.isSameFile(stdPath, path)) {
						break watch;
					}
				}
			}
			watchService.close();

			UnixDomainSocketAddress stdSocketAddress = UnixDomainSocketAddress.of(stdPath);

			SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
			channel.connect(stdSocketAddress);

			std.forward(channel);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				stty(ttyConfig.trim());
			} catch (Exception e) {
				System.err.println("Exception restoring tty config");
			}
		}

	}

	private static void toRawTerminal() throws IOException, InterruptedException {

		ttyConfig = stty("-g");

		// set the console to be character-buffered instead of line-buffered
		stty("-icanon min 1");

		// disable character echoing
		stty("-echo");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> toOriginalTerminal(), "Reset terminal"));
	}

	private static void toOriginalTerminal() {
		if (ttyConfig != null)
			try {
				stty(ttyConfig.trim());
			} catch (Exception e) {
				System.err.println("Exception restoring tty config");
			}
	}

	/**
	 * Execute the stty command with the specified arguments against the current
	 * active terminal.
	 */
	private static String stty(final String args) throws IOException, InterruptedException {
		String cmd = "stty " + args + " < /dev/tty";

		return exec(new String[] { "sh", "-c", cmd });
	}

	/**
	 * Execute the specified command and return the output (both stdout and stderr).
	 */
	private static String exec(final String[] cmd) throws IOException, InterruptedException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		Process p = Runtime.getRuntime().exec(cmd);
		int c;
		InputStream in = p.getInputStream();

		while ((c = in.read()) != -1) {
			bout.write(c);
		}

		in = p.getErrorStream();

		while ((c = in.read()) != -1) {
			bout.write(c);
		}

		p.waitFor();

		String result = new String(bout.toByteArray());
		return result;
	}

//	void pipe() throws IOException {
//		// Set up Server Socket and bind to the port 8000
//		ServerSocketChannel server = ServerSocketChannel.open();
//		SocketAddress endpoint = new InetSocketAddress(8000);
//		server.socket().bind(endpoint);
//
//		server.configureBlocking(false);
//
//		// Set up selector so we can run with a single thread but multiplex between 2
//		// channels
//		Selector selector = Selector.open();
//		server.register(selector, SelectionKey.OP_ACCEPT);
//
//		ByteBuffer buffer = ByteBuffer.allocate(1024);
//
//		while (true) {
//			// block until data comes in
//			selector.select();
//
//			Set<SelectionKey> keys = selector.selectedKeys();
//
//			for (SelectionKey key : keys) {
//				if (!key.isValid()) {
//					// not valid or writable so skip
//					continue;
//				}
//
//				if (key.isAcceptable()) {
//					// Accept socket channel for client connection
//					ServerSocketChannel channel = (ServerSocketChannel) key.channel();
//					SocketChannel accept = channel.accept();
//					setupConnection(selector, accept);
//				} else if (key.isReadable()) {
//					try {
//						// Read into the buffer from the socket and then write the buffer into the
//						// attached socket.
//						SocketChannel recv = (SocketChannel) key.channel();
//						SocketChannel send = (SocketChannel) key.attachment();
//						recv.read(buffer);
//						buffer.flip();
//						send.write(buffer);
//						buffer.rewind();
//					} catch (IOException e) {
//						e.printStackTrace();
//
//						// Close sockets
//						if (key.channel() != null)
//							key.channel().close();
//						if (key.attachment() != null)
//							((SocketChannel) key.attachment()).close();
//					}
//				} else if (key.isWritable()) {
//
//				}
//			}
//
//			// Clear keys for next select
//			keys.clear();
//		}
//
//	}

//	public static void mainX(String[] args) throws IOException, InterruptedException {
//		toRawTerminal();
//		try {
//			boolean client = true;
//			if (client) {
//				ReadableByteChannel inChannel;
//				WritableByteChannel outChannel;
//				inChannel = Channels.newChannel(System.in);
//				outChannel = Channels.newChannel(System.out);
//
//				SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
//				channel.connect(ioSocketAddress());
//
//				new Thread(() -> {
//
//					try {
//						ByteBuffer buffer = ByteBuffer.allocate(1024);
//						while (true) {
//							if (channel.read(buffer) < 0)
//								break;
//							buffer.flip();
//							outChannel.write(buffer);
//							buffer.rewind();
//						}
//						System.exit(0);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}, "Read out").start();
//
//				ByteBuffer buffer = ByteBuffer.allocate(1);
//				while (channel.isConnected()) {
//					if (inChannel.read(buffer) < 0)
//						break;
//					buffer.flip();
//					channel.write(buffer);
//					buffer.rewind();
//				}
//
//			} else {
//				ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
//				serverChannel.bind(ioSocketAddress());
//
//				SocketChannel channel = serverChannel.accept();
//
//				while (true) {
//					readSocketMessage(channel).ifPresent(message -> System.out.printf("[Client message] %s", message));
//					Thread.sleep(100);
//				}
//			}
//		} finally {
//			toOriginalTerminal();
//		}
//	}
//
//	private static Optional<String> readSocketMessage(SocketChannel channel) throws IOException {
//		ByteBuffer buffer = ByteBuffer.allocate(1024);
//		int bytesRead = channel.read(buffer);
//		if (bytesRead < 0)
//			return Optional.empty();
//
//		byte[] bytes = new byte[bytesRead];
//		buffer.flip();
//		buffer.get(bytes);
//		String message = new String(bytes);
//		return Optional.of(message);
//	}
//
//	public static void setupConnection(Selector selector, SocketChannel client) throws IOException {
//		// Connect to the remote server
//		SocketAddress address = new InetSocketAddress("192.168.1.74", 8000);
//		SocketChannel remote = SocketChannel.open(address);
//
//		// Make sockets non-blocking (should be better performance)
//		client.configureBlocking(false);
//		remote.configureBlocking(false);
//
//		client.register(selector, SelectionKey.OP_READ, remote);
//		remote.register(selector, SelectionKey.OP_READ, client);
//	}
//
//	static UnixDomainSocketAddress ioSocketAddress() throws IOException {
//		String system = "default";
//		String bundleSn = "org.argeo.slc.jshell";
//
//		String xdgRunDir = System.getenv("XDG_RUNTIME_DIR");
//		Path baseRunDir = Paths.get(xdgRunDir);
//		Path jshellSocketBase = baseRunDir.resolve("jshell").resolve(system).resolve(bundleSn);
//
//		Files.createDirectories(jshellSocketBase);
//
//		Path ioSocketPath = jshellSocketBase.resolve("io");
//
//		UnixDomainSocketAddress ioSocketAddress = UnixDomainSocketAddress.of(ioSocketPath);
//		System.out.println(ioSocketAddress);
//		return ioSocketAddress;
//	}

}

class SocketPipeSource {
	private ReadableByteChannel inChannel;
	private WritableByteChannel outChannel;

	private Thread readOutThread;

	public void forward(SocketChannel channel) throws IOException {
		readOutThread = new Thread(() -> {

			try {
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				while (true) {
					if (channel.read(buffer) < 0)
						break;
					buffer.flip();
					outChannel.write(buffer);
					buffer.rewind();
				}
				System.exit(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "Read out");
		readOutThread.start();

		// TODO make it smarter than a 1 byte buffer
		// we should recognize control characters
		// e.g ^C
//		int c = System.in.read();
//		if (c == 0x1B) {
//			break;
//		}

		ByteBuffer buffer = ByteBuffer.allocate(1);
		while (channel.isConnected()) {
			if (inChannel.read(buffer) < 0)
				break;
			buffer.flip();
			channel.write(buffer);
			buffer.rewind();
		}

		// end
		// TODO make it more robust
		try {
			// TODO add timeout
			readOutThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setInputStream(InputStream in) {
		inChannel = Channels.newChannel(in);
	}

	public void setOutputStream(OutputStream out) {
		outChannel = Channels.newChannel(out);
	}
}
