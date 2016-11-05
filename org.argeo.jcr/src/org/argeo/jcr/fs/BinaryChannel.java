package org.argeo.jcr.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.jcr.JcrUtils;

public class BinaryChannel implements SeekableByteChannel {
	private final Node file;
	private Binary binary;
	private boolean open = true;

	private long position = 0;

	// private ByteBuffer toWrite;
	private FileChannel fc = null;

	public BinaryChannel(Node file) throws RepositoryException, IOException {
		this.file = file;
		// int capacity = 1024 * 1024;
		// this.toWrite = ByteBuffer.allocate(capacity);
		if (file.isNodeType(NodeType.NT_FILE)) {
			if (file.hasNode(Property.JCR_CONTENT)) {
				Node data = file.getNode(Property.JCR_CONTENT);
				this.binary = data.getProperty(Property.JCR_DATA).getBinary();
			} else {
				Node data = file.addNode(Property.JCR_CONTENT, NodeType.NT_RESOURCE);
				try (InputStream in = new ByteArrayInputStream(new byte[0])) {
					this.binary = data.getSession().getValueFactory().createBinary(in);
				}
			}
		} else {
			throw new IllegalArgumentException(
					"Unsupported file node " + file + " (" + file.getPrimaryNodeType() + ")");
		}
	}

	@Override
	public synchronized boolean isOpen() {
		return open;
	}

	@Override
	public synchronized void close() throws IOException {
		if (isModified()) {
			Binary newBinary=null;
			try {
				Session session = file.getSession();
				// byte[] arr = new byte[(int) position];
				// toWrite.flip();
				// toWrite.get(arr);
				fc.position(0);
				InputStream in = Channels.newInputStream(fc);
				newBinary = session.getValueFactory().createBinary(in);
				file.getNode(Property.JCR_CONTENT).setProperty(Property.JCR_DATA, newBinary);
				session.save();
				open = false;
			} catch (RepositoryException e) {
				throw new JcrFsException("Cannot close " + file, e);
			}finally{
				JcrUtils.closeQuietly(newBinary);
				IOUtils.closeQuietly(fc);
			}
		} else {
			clearReadState();
			open = false;
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (isModified()) {
			return fc.read(dst);
		} else {

			try {
				int read;
//				int capacity = dst.capacity();
				byte[] arr = dst.array();
				read = binary.read(arr, position);
				//dst.put(arr, 0, read);

				// try {
				// byte[] arr = dst.array();
				// read = binary.read(arr, position);
				// } catch (UnsupportedOperationException e) {
				// int capacity = dst.capacity();
				// byte[] arr = new byte[capacity];
				// read = binary.read(arr, position);
				// dst.put(arr);
				// }
				if (read != -1)
					position = position + read;
				return read;
			} catch (RepositoryException e) {
				throw new JcrFsException("Cannot read into buffer", e);
			}
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int written = getFileChannel().write(src);
		return written;
		// int byteCount = src.remaining();
		// if (toWrite.remaining() < byteCount)
		// throw new JcrFsException("Write buffer is full");
		// toWrite.put(src);
		// if (position < binarySize)
		// position = binarySize + byteCount;
		// else
		// position = position + byteCount;
		// return byteCount;
	}

	@Override
	public long position() throws IOException {
		if (isModified())
			return getFileChannel().position();
		else
			return position;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		if (isModified()) {
			getFileChannel().position(position);
		} else {
			this.position = newPosition;
		}
		return this;
	}

	@Override
	public long size() throws IOException {
		if (isModified()) {
			return getFileChannel().size();
		} else {
			try {
				return binary.getSize();
			} catch (RepositoryException e) {
				throw new JcrFsException("Cannot get size", e);
			}
		}
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		getFileChannel().truncate(size);
		// if (size != size())
		// throw new UnsupportedOperationException("Cannot truncate JCR
		// binary");
		return this;
	}

	private FileChannel getFileChannel() throws IOException {
		try {
			if (fc == null) {
				Path tempPath = Files.createTempFile(getClass().getSimpleName(), null);
				fc = FileChannel.open(tempPath, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SPARSE);
				ReadableByteChannel readChannel = Channels.newChannel(binary.getStream());
				fc.transferFrom(readChannel, 0, binary.getSize());
				clearReadState();
			}
			return fc;
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get temp file channel", e);
		}
	}

	private boolean isModified() {
		return fc != null;
	}
	
	private void clearReadState(){
		position = -1;
		JcrUtils.closeQuietly(binary);
		binary=null;
	}
}