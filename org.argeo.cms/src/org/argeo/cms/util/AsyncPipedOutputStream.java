package org.argeo.cms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * An output stream whose {@link #close()} method will wait for read actions to
 * be completed. It is meant to be used transparently as an
 * {@link OutputStream}, fulfilling the expectation that everything has been
 * done when the {@link #close()} method has returned.
 */
public class AsyncPipedOutputStream extends PipedOutputStream {
//	private final static Logger logger = System.getLogger(AsyncPipedOutputStream.class.getName());

	private CompletableFuture<Void> readingDone;

	private long timeout = 60 * 1000;

	/**
	 * Provides the actions which will read (and close) the related piped input
	 * stream. Reading starts immediately asynchronously, but the provided
	 * {@link InputStream} will block until data starts to be written to this output
	 * stream.
	 */
	public void asyncRead(Consumer<InputStream> readActions) {
		try {
			PipedInputStream in = new PipedInputStream(this);
			readingDone = CompletableFuture.runAsync(() -> {
				readActions.accept(in);
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create piped input stream", e);
		}
	}

	/**
	 * Closes this output stream immediately but then wait for the reading of the
	 * related input stream to be completed.
	 */
	@Override
	public void close() throws IOException {
		Objects.requireNonNull(readingDone, "Async read must have started");
		super.flush();
		super.close();
		readingDone.orTimeout(timeout, TimeUnit.MILLISECONDS).join();
//		logger.log(Logger.Level.DEBUG, "OUT waiting " + timeout);
//		try {
//			readingDone.get(timeout, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException | ExecutionException | TimeoutException e) {
//			logger.log(Logger.Level.ERROR, "Reading was not completed", e);
//		}
//		logger.log(Logger.Level.DEBUG, "OUT closed");
	}

	/**
	 * Sets the timeout in milliseconds when waiting for reading to be completed
	 * before returning in the {@link #close()} method.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

}
