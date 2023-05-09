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

	private final String id;

	public SocketPipeMirror(String id) throws IOException {
		this.id = id;
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
		}, "JShell read " + id);
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
		}, "JShell write " + id);
		writeOutThread.start();

	}

	@Override
	public void close() throws IOException {
		// TODO make it more robust
//		readInThread.interrupt();
//		writeOutThread.interrupt();
		in.close();
		out.close();
		try {
			readInThread.join();
		} catch (InterruptedException e) {
			// silent
		}
		try {
			writeOutThread.join();
		} catch (InterruptedException e) {
			// silent
		}
	}

	public InputStream getInputStream() {
		return in;
	}

	public OutputStream getOutputStream() {
		return out;
	}
}
