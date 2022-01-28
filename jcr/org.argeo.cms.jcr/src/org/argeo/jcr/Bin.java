package org.argeo.jcr;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * A {@link Binary} wrapper implementing {@link AutoCloseable} for ease of use
 * in try/catch blocks.
 */
public class Bin implements Binary, AutoCloseable {
	private final Binary wrappedBinary;

	public Bin(Property property) throws RepositoryException {
		this(property.getBinary());
	}

	public Bin(Binary wrappedBinary) {
		if (wrappedBinary == null)
			throw new IllegalArgumentException("Wrapped binary cannot be null");
		this.wrappedBinary = wrappedBinary;
	}

	// private static Binary getBinary(Property property) throws IOException {
	// try {
	// return property.getBinary();
	// } catch (RepositoryException e) {
	// throw new IOException("Cannot get binary from property " + property, e);
	// }
	// }

	@Override
	public void close() {
		dispose();
	}

	@Override
	public InputStream getStream() throws RepositoryException {
		return wrappedBinary.getStream();
	}

	@Override
	public int read(byte[] b, long position) throws IOException, RepositoryException {
		return wrappedBinary.read(b, position);
	}

	@Override
	public long getSize() throws RepositoryException {
		return wrappedBinary.getSize();
	}

	@Override
	public void dispose() {
		wrappedBinary.dispose();
	}

}
