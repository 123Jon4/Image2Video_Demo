/*******************************************************************************
 * Copyright (c) 2014, Art Clarke.  All rights reserved.
 *  
 * This file is part of Humble-Video.
 *
 * Humble-Video is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Humble-Video is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Humble-Video.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package humble_video;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.humble.video.Codec;
import io.humble.video.Encoder;
import io.humble.video.Media;
import io.humble.video.MediaAudio;
import io.humble.video.MediaEncoded;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Muxer;
import io.humble.video.MuxerFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

/**
 * Records the contents of your computer screen to a media file for the passed
 * in duration. This is meant as a demonstration program to teach the use of the
 * Humble API.
 * <p>
 * Concepts introduced:
 * </p>
 * <ul>
 * <li>Muxer: A {@link Muxer} object is a container you can write media data
 * to.</li>
 * <li>Encoders: An {@link Encoder} object lets you convert {@link MediaAudio}
 * or {@link MediaPicture} objects into {@link MediaPacket} objects so they can
 * be written to {@link Muxer} objects.</li>
 * </ul>
 * 
 * <p>
 * To run from maven, do:
 * </p>
 * 
 * <pre>
 * mvn install exec:java -Dexec.mainClass="io.humble.video.demos.RecordAndEncodeVideo" -Dexec.args="filename.mp4"
 * </pre>
 * 
 * @author aclarke
 *
 */
public class ImageToVideoMultiThread {
	private static void generateVideoFromImage(String inputFilename, String outputFilename, String formatname,
			String codecname, int duration, int fps) throws AWTException, InterruptedException, IOException {
		/**
		 * Set up the AWT infrastructure to take screenshots of the desktop.
		 */
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(inputFilename));
		} catch (IOException e) {
		}
		final Rectangle screenbounds = new Rectangle(img.getWidth(), img.getHeight());
		System.out.println(screenbounds);

		final Rational framerate = Rational.make(1, fps);

		/**
		 * First we create a muxer using the passed in filename and formatname if given.
		 */
		final Muxer muxer = Muxer.make(outputFilename, null, formatname);

		/**
		 * Now, we need to decide what type of codec to use to encode video. Muxers have
		 * limited sets of codecs they can use. We're going to pick the first one that
		 * works, or if the user supplied a codec name, we're going to force-fit that in
		 * instead.
		 */
		final MuxerFormat format = muxer.getFormat();
		final Codec codec;
		if (codecname != null) {
			codec = Codec.findEncodingCodecByName(codecname);
		} else {
			codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
		}

		/**
		 * Now that we know what codec, we need to create an encoder
		 */
		Encoder encoder = Encoder.make(codec);

		/**
		 * Video encoders need to know at a minimum: width height pixel format Some also
		 * need to know frame-rate (older codecs that had a fixed rate at which video
		 * files could be written needed this). There are many other options you can set
		 * on an encoder, but we're going to keep it simpler here.
		 */
		encoder.setWidth(screenbounds.width);
		encoder.setHeight(screenbounds.height);
		// We are going to use 420P as the format because that's what most video formats
		// these days use
		final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
		encoder.setPixelFormat(pixelformat);
		encoder.setTimeBase(framerate);

		/**
		 * An annoynace of some formats is that they need global (rather than
		 * per-stream) headers, and in that case you have to tell the encoder. And since
		 * Encoders are decoupled from Muxers, there is no easy way to know this beyond
		 */
		if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
			encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);

		/** Open the encoder. */
		encoder.open(null, null);

		/** Add this stream to the muxer. */
		muxer.addNewStream(encoder);

		/** And open the muxer for business. */
		muxer.open(null, null);

		/**
		 * Next, we need to make sure we have the right MediaPicture format objects to
		 * encode data with. Java (and most on-screen graphics programs) use some
		 * variant of Red-Green-Blue image encoding (a.k.a. RGB or BGR). Most video
		 * codecs use some variant of YCrCb formatting. So we're going to have to
		 * convert. To do that, we'll introduce a MediaPictureConverter object later.
		 * object.
		 */
		MediaPictureConverter converter = null;
		final MediaPicture picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), pixelformat);
		picture.setTimeBase(framerate);

		/**
		 * Now begin our main loop of taking screen snaps. We're going to encode and
		 * then write out any resulting packets.
		 */
		final MediaPacket packet = MediaPacket.make();
		final BufferedImage screen = convertToType(img, BufferedImage.TYPE_3BYTE_BGR);
		converter = MediaPictureConverterFactory.createConverter(screen, picture);
		converter.toPicture(picture, screen, 0);
		double frames = duration / framerate.getDouble();
		int threads = 60;
		for (int thread = 0; thread < threads; thread++) {
			double startFrame = frames / threads * thread;
			double endFrame = frames / threads * (thread + 1);

			System.out.println("Thread: " + thread);
			System.out.println("Start frame: " + startFrame);
			System.out.println("End frame: " + endFrame);
			for (double i = startFrame; i < endFrame; i++) {
				/** Make the screen capture && convert image to TYPE_3BYTE_BGR */

				/**
				 * This is LIKELY not in YUV420P format, so we're going to convert it using some
				 * handy utilities.
				 */

				picture.setTimeStamp((long) i);

				do {
					encoder.encode(packet, picture);
					if (packet.isComplete())
						muxer.write(packet, false);
				} while (packet.isComplete());
			}
		}

		/**
		 * Encoders, like decoders, sometimes cache pictures so it can do the right
		 * key-frame optimizations. So, they need to be flushed as well. As with the
		 * decoders, the convention is to pass in a null input until the output is not
		 * complete.
		 */
		do {
			encoder.encode(packet, null);
			if (packet.isComplete())
				muxer.write(packet, false);
		} while (packet.isComplete());

		/** Finally, let's clean up after ourselves. */
		muxer.close();

	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws InterruptedException, IOException, AWTException {
		final Options options = new Options();
		options.addOption("h", "help", false, "displays help");
		options.addOption("v", "version", false, "version of this library");
		options.addOption(OptionBuilder.withArgName("format").withLongOpt("format").hasArg()
				.withDescription("muxer format to use. If unspecified, we will guess from filename").create("f"));
		options.addOption(OptionBuilder.withArgName("codec").withLongOpt("codec").hasArg()
				.withDescription("codec to use when encoding video; If unspecified, we will guess from format")
				.create("c"));
		options.addOption(OptionBuilder.withArgName("duration").withLongOpt("duration").hasArg()
				.withDescription("number of seconds of screenshot to record; defaults to 10.").create("d"));
		options.addOption(OptionBuilder.withArgName("framerate").withLongOpt("framerate").hasArg()
				.withDescription("number of pictures to take per second (i.e. the frame rate); defaults to 25")
				.create("fps"));

		final CommandLineParser parser = new org.apache.commons.cli.BasicParser();
		try {
			final CommandLine cmd = parser.parse(options, args);
			final String[] parsedArgs = cmd.getArgs();
			if (cmd.hasOption("version")) {
				// let's find what version of the library we're running
				final String version = io.humble.video_native.Version.getVersionInfo();
				System.out.println("Humble Version: " + version);
			} else if (cmd.hasOption("help") || parsedArgs.length != 2) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(ImageToVideoMultiThread.class.getCanonicalName() + " <filename>", options);
			} else {
				/**
				 * Read in some option values and their defaults.
				 */
				final int duration = Integer.parseInt(cmd.getOptionValue("duration", "60"));
				if (duration <= 0)
					throw new IllegalArgumentException("duration must be > 0");
				final int framerate = Integer.parseInt(cmd.getOptionValue("framerate", "25"));
				if (framerate <= 0)
					throw new IllegalArgumentException("snaps must be > 0");
				final String codecname = cmd.getOptionValue("codec");
				final String formatname = cmd.getOptionValue("format");
				final String inputFilename = cmd.getArgs()[0];
				final String outputFilename = cmd.getArgs()[1];

				System.out.println(inputFilename);
				System.out.println(outputFilename);

				generateVideoFromImage(inputFilename, outputFilename, formatname, codecname, duration, framerate);
			}
		} catch (ParseException e) {
			System.err.println("Exception parsing command line: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Convert a {@link BufferedImage} of any type, to {@link BufferedImage} of a
	 * specified type. If the source image is the same type as the target type, then
	 * original image is returned, otherwise new image of the correct type is
	 * created and the content of the source image is copied into the new image.
	 * 
	 * @param sourceImage
	 *            the image to be converted
	 * @param targetType
	 *            the desired BufferedImage type
	 * 
	 * @return a BufferedImage of the specifed target type.
	 * 
	 * @see BufferedImage
	 */

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

	public static class EncodeAndWriteThread implements Runnable {
		Thread t;

		private MediaPacket packet;
		private MediaPicture picture;
		private Encoder encoder;
		private Muxer muxer;

		public EncodeAndWriteThread(MediaPacket packet, MediaPicture picture, Encoder encoder, Muxer muxer) {
			this.packet = packet;
			this.picture = picture;
			this.encoder = encoder;
			this.muxer = muxer;

			t = new Thread(this);
			t.start();
		}

		public void run() {
			do {
				encoder.encode(packet, picture);
				if (packet.isComplete())
					muxer.write(packet, false);
			} while (packet.isComplete());
		}
	}

}
