package javacv;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import io.humble.video.PixelFormat;

// Source: https://github.com/csanuragjain/converter/tree/master/ImageToMovie
// Edited
public class ImageToMovieDNX {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		String imgPath = "./scratched-and-scraped-metal-texture-2_1.jpg";
		String vidPath = "./output.mxf";

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
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vidPath, 1920, 1080);
		try {
			recorder.setFrameRate(25);
			recorder.setInterleaved(true);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_DNXHD);
			recorder.setVideoBitrate(120000000);
			recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV422P);
			recorder.setFormat("mxf");
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
