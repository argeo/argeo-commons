package org.argeo.util;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Hashes the hashes of the files in a directory. */
public class DirH {

	private final static Charset charset = Charset.forName("UTF-16");
	private final static long bufferSize = 200 * 1024 * 1024;
	private final static String algorithm = "SHA";

	private final static byte EOL = (byte) '\n';
	private final static byte SPACE = (byte) ' ';

	private final int hashSize;

	private final byte[][] hashes;
	private final byte[][] fileNames;
	private final byte[] digest;
	private final byte[] dirName;

	/**
	 * @param dirName can be null or empty
	 */
	private DirH(byte[][] hashes, byte[][] fileNames, byte[] dirName) {
		if (hashes.length != fileNames.length)
			throw new IllegalArgumentException(hashes.length + " hashes and " + fileNames.length + " file names");
		this.hashes = hashes;
		this.fileNames = fileNames;
		this.dirName = dirName == null ? new byte[0] : dirName;
		if (hashes.length == 0) {// empty dir
			hashSize = 20;
			// FIXME what is the digest of an empty dir?
			digest = new byte[hashSize];
			Arrays.fill(digest, SPACE);
			return;
		}
		hashSize = hashes[0].length;
		for (int i = 0; i < hashes.length; i++) {
			if (hashes[i].length != hashSize)
				throw new IllegalArgumentException(
						"Hash size for " + new String(fileNames[i], charset) + " is " + hashes[i].length);
		}

		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			for (int i = 0; i < hashes.length; i++) {
				md.update(this.hashes[i]);
				md.update(SPACE);
				md.update(this.fileNames[i]);
				md.update(EOL);
			}
			digest = md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Cannot digest", e);
		}
	}

	public void print(PrintStream out) {
		out.print(DigestUtils.encodeHexString(digest));
		if (dirName.length > 0) {
			out.print(' ');
			out.print(new String(dirName, charset));
		}
		out.print('\n');
		for (int i = 0; i < hashes.length; i++) {
			out.print(DigestUtils.encodeHexString(hashes[i]));
			out.print(' ');
			out.print(new String(fileNames[i], charset));
			out.print('\n');
		}
	}

	public static DirH digest(Path dir) {
		try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
			List<byte[]> hs = new ArrayList<byte[]>();
			List<String> fNames = new ArrayList<>();
			for (Path file : files) {
				if (!Files.isDirectory(file)) {
					byte[] digest = DigestUtils.digestRaw(algorithm, file, bufferSize);
					hs.add(digest);
					fNames.add(file.getFileName().toString());
				}
			}

			byte[][] fileNames = new byte[fNames.size()][];
			for (int i = 0; i < fNames.size(); i++) {
				fileNames[i] = fNames.get(i).getBytes(charset);
			}
			byte[][] hashes = hs.toArray(new byte[hs.size()][]);
			return new DirH(hashes, fileNames, dir.toString().getBytes(charset));
		} catch (IOException e) {
			throw new RuntimeException("Cannot digest " + dir, e);
		}
	}

	public static void main(String[] args) {
		try {
			DirH dirH = DirH.digest(Paths.get("/home/mbaudier/tmp/"));
			dirH.print(System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
