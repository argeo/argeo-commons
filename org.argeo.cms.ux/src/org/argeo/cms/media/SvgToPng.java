package org.argeo.cms.media;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.FilenameUtils;

public class SvgToPng {

	public void convertSvgDir(Path sourceDir, Path targetDir, int width) {
		System.out.println("##\n## " + width + "px - " + sourceDir+"\n##");
		try {
			if (targetDir == null)
				targetDir = sourceDir.getParent().resolve(Integer.toString(width));
			Files.createDirectories(targetDir);

			PNGTranscoder transcoder = new PNGTranscoder();
			// transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR,
			// Color.WHITE);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) width);

			for (Path source : Files.newDirectoryStream(sourceDir, "*.svg")) {
				String baseName = FilenameUtils.getBaseName(source.toString());
				Path target = targetDir.resolve(baseName + ".png");
				convertSvgFile(transcoder, source, target);
			}
		} catch (IOException | TranscoderException e) {
			throw new IllegalStateException("Cannot convert from " + sourceDir + " to " + targetDir, e);
		}

	}

	protected void convertSvgFile(ImageTranscoder transcoder, Path source, Path target)
			throws IOException, TranscoderException {
		try (Reader reader = Files.newBufferedReader(source); OutputStream out = Files.newOutputStream(target);) {
			TranscoderInput input = new TranscoderInput(reader);
//			BufferedImage image = transcoder.createImage(32, 32);
			TranscoderOutput output = new TranscoderOutput(out);
			transcoder.transcode(input, output);
			System.out.println(source.getFileName() + " -> " + target);
		}
	}

	public static void main(String[] args) throws Exception {

		Path path = Paths.get(args[0]);

		SvgToPng svgToPng = new SvgToPng();
		svgToPng.convertSvgDir(path, null, 16);
		svgToPng.convertSvgDir(path, null, 32);
		svgToPng.convertSvgDir(path, null, 64);
		svgToPng.convertSvgDir(path, null, 96);
	}
}
