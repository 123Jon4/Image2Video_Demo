package jcodec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jcodec.api.awt.AWTSequenceEncoder;

public class Image2VideoJCodec {
	public static void main(String[] args) {
		String imgPath = "./scratched-and-scraped-metal-texture-2_1.jpg";
		String vidPath = "./output.mp4";
		int framerate = 25;
		int duration = 2;

		File imgFile = new File(imgPath);
		File outputFile = new File(vidPath);

		BufferedImage img = null;
		try {
			img = ImageIO.read(imgFile);
			BufferedImage bgrScreen = convertToType(img, BufferedImage.TYPE_3BYTE_BGR);
			AWTSequenceEncoder enc = AWTSequenceEncoder.create25Fps(outputFile);
			int frames = framerate * duration;

			for (int frame = 0; frame < frames; frame += 1) {
				enc.encodeImage(bgrScreen);
				System.out.println("Encoded frame: " + frame);
			}

			enc.finish();
		} catch (IOException e) {
		}
	}

	public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
		BufferedImage image;

		// if the source image is already the target type, return the source image

		if (sourceImage.getType() == targetType)
			image = sourceImage;

		// otherwise create a new image of the target type and draw the new
		// image

		else {
			image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
			image.getGraphics().drawImage(sourceImage, 0, 0, null);
		}

		return image;
	}
}
