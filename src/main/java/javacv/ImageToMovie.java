package javacv;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

// Source: https://github.com/csanuragjain/converter/tree/master/ImageToMovie
// Edited
public class ImageToMovie {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		String imgPath = "./scratched-and-scraped-metal-texture-2_1.jpg";
		String vidPath = "./output.mp4";

		File file = new File(imgPath);

		convertJPGtoMovie(file, vidPath);
		System.out.println("Video has been created at " + vidPath);
	}

	public static void convertJPGtoMovie(File imgFile, String vidPath) {
		OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File("./scratched-and-scraped-metal-texture-2_1.jpg"));
		} catch (IOException e) {
		}
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vidPath, img.getWidth(), img.getHeight());
		try {
			recorder.setFrameRate(25);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
			recorder.setVideoBitrate(9000);
			recorder.setFormat("mp4");
			recorder.setVideoQuality(0); // maximum quality
			recorder.start();
			for (int i = 0; i < 25 * 2; i++) {
				recorder.record(grabberConverter.convert(cvLoadImage(imgFile.getAbsolutePath())));
			}
			recorder.stop();
		} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}
}
