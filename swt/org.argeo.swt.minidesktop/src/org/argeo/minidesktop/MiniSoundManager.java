package org.argeo.minidesktop;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class MiniSoundManager {
//	static AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);

	public static void main(String[] args) {
		try {
			Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
			for (Mixer.Info info : mixerInfos) {
				Mixer m = AudioSystem.getMixer(info);
				Line.Info[] sourceInfos = m.getSourceLineInfo();
				Line.Info[] targetInfos = m.getTargetLineInfo();
				if (sourceInfos.length != 0 || targetInfos.length != 0)
					System.out.println("\n\n## MIXER\n" + m.getMixerInfo().getName() + "\n## ");

				if (sourceInfos.length > 0)
					System.out.println("# Sources ");
				for (Line.Info lineInfo : sourceInfos) {
					System.out.println(info.getName() + " ---" + lineInfo);
					Line line = m.getLine(lineInfo);
					if (line instanceof SourceDataLine)
						System.out.println(SourceDataLine.class.getSimpleName());
				}
				if (targetInfos.length > 0)
					System.out.println("# Targets ");
				for (Line.Info lineInfo : targetInfos) {
					System.out.println(info.getName() + " ---" + lineInfo);
					Line line = m.getLine(lineInfo);
					if (line instanceof TargetDataLine)
						System.out.println(TargetDataLine.class.getSimpleName());
				}

//				Clip clip = AudioSystem.getClip();
//				clip.open(ais);
				//
//				Thread.sleep(4000);
			}
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

//	private TargetDataLine getTargetDataLineForRecord() throws LineUnavailableException {
//		TargetDataLine line;
//		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//		if (!AudioSystem.isLineSupported(info)) {
//			return null;
//		}
//		line = (TargetDataLine) AudioSystem.getLine(info);
//		line.open(format, line.getBufferSize());
//		return line;
//	}
//
//	private AudioInputStream convertToAudioIStream(final ByteArrayOutputStream out, int frameSizeInBytes) {
//		byte audioBytes[] = out.toByteArray();
//		ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
//		AudioInputStream audioStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);
////	    long milliseconds = (long) ((audioStream.getFrameLength() * 1000) / format.getFrameRate());
////	    duration = milliseconds / 1000.0;
//		return audioStream;
//	}

}
