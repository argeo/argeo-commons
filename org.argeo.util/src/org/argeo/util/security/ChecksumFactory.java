package org.argeo.util.security;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.zip.Checksum;

import org.argeo.util.internal.UtilsException;

/** Allows to fine tune how files are read. */
public class ChecksumFactory {
	private int regionSize = 10 * 1024 * 1024;

	public byte[] digest(Path path, final String algo) {
		try {
			final MessageDigest md = MessageDigest.getInstance(algo);
			if (Files.isDirectory(path)) {
				long begin = System.currentTimeMillis();
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
						if (!Files.isDirectory(file)) {
							byte[] digest = digest(file, algo);
							md.update(digest);
						}
						return FileVisitResult.CONTINUE;
					}

				});
				byte[] digest = md.digest();
				long duration = System.currentTimeMillis() - begin;
				System.out.println(printBase64Binary(digest) + " " + path
						+ " (" + duration / 1000 + "s)");
				return digest;
			} else {
				long begin = System.nanoTime();
				long length = -1;
				try (FileChannel channel = (FileChannel) Files
						.newByteChannel(path);) {
					length = channel.size();
					long cursor = 0;
					while (cursor < length) {
						long effectiveSize = Math.min(regionSize, length
								- cursor);
						MappedByteBuffer mb = channel.map(
								FileChannel.MapMode.READ_ONLY, cursor,
								effectiveSize);
						// md.update(mb);
						byte[] buffer = new byte[1024];
						while (mb.hasRemaining()){
							mb.get(buffer);
							md.update(buffer);
						}

						// sub digest
						// mb.flip();
						// MessageDigest subMd =
						// MessageDigest.getInstance(algo);
						// subMd.update(mb);
						// byte[] subDigest = subMd.digest();
						// System.out.println(" -> " + cursor);
						// System.out.println(StreamUtils.encodeHexString(subDigest));
						// System.out.println(new BigInteger(1,
						// subDigest).toString(16));
						// System.out.println(new BigInteger(1, subDigest)
						// .toString(Character.MAX_RADIX));
						// System.out.println(printBase64Binary(subDigest));

						cursor = cursor + regionSize;
					}
					byte[] digest = md.digest();
					long duration = System.nanoTime() - begin;
					System.out.println(printBase64Binary(digest) + " "
							+ path.getFileName() + " (" + duration / 1000000
							+ "ms, " + (length / 1024) + "kB, "
							+ (length / (duration / 1000000)) * 1000
							/ (1024 * 1024) + " MB/s)");
					return digest;
				}
			}
		} catch (Exception e) {
			throw new UtilsException("Cannot digest " + path, e);
		}
	}

	/** Whether the file should be mapped. */
	protected boolean mapFile(FileChannel fileChannel) throws IOException {
		long size = fileChannel.size();
		if (size > (regionSize / 10))
			return true;
		return false;
	}

	public long checksum(Path path, Checksum crc) {
		final int bufferSize = 2 * 1024 * 1024;
		long begin = System.currentTimeMillis();
		try (FileChannel channel = (FileChannel) Files.newByteChannel(path);) {
			byte[] bytes = new byte[bufferSize];
			long length = channel.size();
			long cursor = 0;
			while (cursor < length) {
				long effectiveSize = Math.min(regionSize, length - cursor);
				MappedByteBuffer mb = channel.map(
						FileChannel.MapMode.READ_ONLY, cursor, effectiveSize);
				int nGet;
				while (mb.hasRemaining()) {
					nGet = Math.min(mb.remaining(), bufferSize);
					mb.get(bytes, 0, nGet);
					crc.update(bytes, 0, nGet);
				}
				cursor = cursor + regionSize;
			}
			return crc.getValue();
		} catch (Exception e) {
			throw new UtilsException("Cannot checksum " + path, e);
		} finally {
			long duration = System.currentTimeMillis() - begin;
			System.out.println(duration / 1000 + "s");
		}
	}

	public static void main(String... args) {
		ChecksumFactory cf = new ChecksumFactory();
		// Path path =
		// Paths.get("/home/mbaudier/apache-maven-3.2.3-bin.tar.gz");
		Path path;
		if (args.length > 0) {
			path = Paths.get(args[0]);
		} else {
			path = Paths
					.get("/home/mbaudier/Downloads/torrents/CentOS-7-x86_64-DVD-1503-01/"
							+ "CentOS-7-x86_64-DVD-1503-01.iso");
		}
		// long adler = cf.checksum(path, new Adler32());
		// System.out.format("Adler=%d%n", adler);
		// long crc = cf.checksum(path, new CRC32());
		// System.out.format("CRC=%d%n", crc);
		String algo = "SHA1";
		byte[] digest = cf.digest(path, algo);
		System.out.println(algo + " " + printBase64Binary(digest));
		System.out.println(algo + " " + new BigInteger(1, digest).toString(16));
		// String sha1 = printBase64Binary(cf.digest(path, "SHA1"));
		// System.out.format("SHA1=%s%n", sha1);
	}
}
