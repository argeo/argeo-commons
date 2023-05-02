package org.argeo.cms.jshell;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;

class SocketPipeMirror implements Closeable {
	private final Pipe inPipe;
	private final Pipe outPipe;

	private final InputStream in;
	private final OutputStream out;

	private Thread readInThread;
	private Thread writeOutThread;

	public SocketPipeMirror() throws IOException {
		inPipe = Pipe.open();
		outPipe = Pipe.open();
		in = Channels.newInputStream(inPipe.source());
		out = Channels.newOutputStream(outPipe.sink());
	}

	public void open(SocketChannel channel) {
		readInThread = new Thread(() -> {

			try {
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				while (!readInThread.isInterrupted() && channel.isConnected()) {
					if (channel.read(buffer) < 0)
						break;
					buffer.flip();
					inPipe.sink().write(buffer);
					buffer.rewind();
				}
			} catch (AsynchronousCloseException e) {
				// ignore
				// TODO make it cleaner
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "Read in");
		readInThread.start();

		writeOutThread = new Thread(() -> {

			try {
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				while (!writeOutThread.isInterrupted() && channel.isConnected()) {
					if (outPipe.source().read(buffer) < 0)
						break;
					buffer.flip();
					channel.write(buffer);
					buffer.rewind();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "Write out");
		writeOutThread.start();

	}

	@Override
	public void close() throws IOException {
		// TODO make it more robust
		readInThread.interrupt();
		writeOutThread.interrupt();
		in.close();
		out.close();
	}

	public InputStream getInputStream() {
		return in;
	}

	public OutputStream getOutputStream() {
		return out;
	}
}
