package com.gentics.lib.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOSupplier;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

/**
 * Static helper class for handling images
 */
public final class ImageUtils {
	/**
	 * Private constructor
	 */
	private ImageUtils() {
	}

	/**
	 * Read the image from the stream supplied by the given supplier. The supplier
	 * might be called multiple times and should create a new {@link InputStream}
	 * instance upon every call.
	 * 
	 * @param streamSupplier stream supplier
	 * @return PlanarImage
	 * @throws IOException when reading the stream failed
	 */
	public final static PlanarImage read(IOSupplier<InputStream> streamSupplier) throws IOException {
		BufferedImage image;
		try (InputStream in = streamSupplier.get()) {
			image = ImageIO.read(in);
		}

		if (image == null) {
			try (InputStream in = streamSupplier.get()) {
				ImmutableImage image2 = ImmutableImage.loader().fromStream(in);
				if (image2 != null) {
					image = image2.awt();
				}
			}
		}

		if (image == null) {
			throw new IOException("Could not read image");
		}
		return PlanarImage.wrapRenderedImage(image);
	}

	/**
	 * Read the image from the given file
	 * @param file file
	 * @return planar image
	 * @throws IOException
	 */
	public final static PlanarImage read(File file) throws IOException {
		return read(() -> new FileInputStream(file));
	}

	/**
	 * Read the image from the given byte array
	 * @param data byte array
	 * @return planar image
	 * @throws IOException
	 */
	public final static PlanarImage read(byte[] data) throws IOException {
		return read(() -> new ByteArrayInputStream(data));
	}

	/**
	 * Write the image with the given format into the target output stream
	 * @param image image to encode
	 * @param formatName format name
	 * @param target target output stream
	 * @return true when writing succeeded, false if not
	 * @throws IOException
	 */
	public final static boolean write(PlanarImage image, String formatName, OutputStream target) throws IOException {
		if ("webp".equals(formatName)) {
			ImmutableImage iimage = ImmutableImage.fromAwt(image.getAsBufferedImage());
			try (ByteArrayInputStream in = iimage.forWriter(WebpWriter.DEFAULT).stream()) {
				IOUtils.copy(in, target);
			}
			return true;
		} else {
			return ImageIO.write(image.getAsBufferedImage(), formatName, target);
		}
	}
}
