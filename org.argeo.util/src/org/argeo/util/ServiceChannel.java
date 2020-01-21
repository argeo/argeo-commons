package org.argeo.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** An {@link AsynchronousByteChannel} based on an {@link ExecutorService}. */
public class ServiceChannel implements AsynchronousByteChannel {
	private final ReadableByteChannel in;
	private final WritableByteChannel out;

	private boolean open = true;

	private ExecutorService executor;

	public ServiceChannel(ReadableByteChannel in, WritableByteChannel out, ExecutorService executor) {
		this.in = in;
		this.out = out;
		this.executor = executor;
	}

	@Override
	public Future<Integer> read(ByteBuffer dst) {
		return executor.submit(() -> in.read(dst));
	}

	@Override
	public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
		try {
			Future<Integer> res = read(dst);
			handler.completed(res.get(), attachment);
		} catch (Exception e) {
			handler.failed(e, attachment);
		}
	}

	@Override
	public Future<Integer> write(ByteBuffer src) {
		return executor.submit(() -> out.write(src));
	}

	@Override
	public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
		try {
			Future<Integer> res = write(src);
			handler.completed(res.get(), attachment);
		} catch (Exception e) {
			handler.failed(e, attachment);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		try {
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		open = false;
		notifyAll();
	}

	@Override
	public synchronized boolean isOpen() {
		return open;
	}

}
