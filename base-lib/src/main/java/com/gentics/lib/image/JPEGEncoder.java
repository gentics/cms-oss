/*
 * @author norbert
 * @date 18.09.2006
 * @version $Id: JPEGEncoder.java,v 1.4 2009-12-16 16:12:23 herbert Exp $
 */
package com.gentics.lib.image;

import java.awt.image.ColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;

import org.jmage.ApplicationContext;
import org.jmage.ImageRequest;
import org.jmage.encoder.CodecException;
import org.jmage.encoder.ImageEncoder;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * ImageEncoder to write JPEG files with configurable quality
 */
public class JPEGEncoder implements ImageEncoder {
	private static final String ENCODE = "encode";

	public final static String JPEG_QUALITY = "JPEG_QUALITY";

	public final static float DEFAULT_QUALITY = 0.9f;

	protected JPEGImageWriteParam encodeParam;

	private static NodeLogger logger = NodeLogger.getNodeLogger(JPEGEncoder.class);

	private final static Pattern ENCODINGPATTERN = Pattern.compile("jpe?g(/([0-9\\.]*))?");

	/**
	 * Create an instance of the encoder
	 */
	public JPEGEncoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#canHandle(org.jmage.ImageRequest)
	 */
	public boolean canHandle(ImageRequest request) {
		String encodingFormat = ObjectTransformer.getString(request.getEncodingFormat(), "").toLowerCase();
		Matcher m = ENCODINGPATTERN.matcher(encodingFormat);

		return m.matches();
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#initialize(org.jmage.ImageRequest)
	 */
	public void initialize(ImageRequest request) throws CodecException {
		String encodingFormat = ObjectTransformer.getString(request.getEncodingFormat(), "").toLowerCase();
		float confQuality = (float) ObjectTransformer.getDouble(request.getFilterChainProperties().getProperty(JPEG_QUALITY), DEFAULT_QUALITY);

		if (logger.isDebugEnabled()) {
			logger.debug("Requested encoding: " + encodingFormat);
		}
		Matcher m = ENCODINGPATTERN.matcher(encodingFormat);

		if (m.matches()) {
			float quality = (float) ObjectTransformer.getDouble(m.group(2), confQuality);

			encodeParam = new JPEGImageWriteParam(null);
			encodeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			encodeParam.setCompressionQuality(quality);

			if (logger.isDebugEnabled()) {
				logger.debug("Initialized JPEG encoder with quality " + quality);
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Encoding does not match requested pattern");
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#createFrom(org.jmage.ImageRequest)
	 */
	public byte[] createFrom(ImageRequest request) throws CodecException {
		if (logger.isDebugEnabled()) {
			ColorModel cm = request.getImage().getColorModel();

			logger.debug("colorSpace type: " + cm.getColorSpace().getType());
			if (encodeParam == null) {
				logger.debug("Encoding image (colorModel {" + cm + "}) with default quality");
			} else {
				logger.debug("Encoding image (colorModel {" + cm + "}) with quality " + encodeParam.getCompressionQuality());
			}
		}

		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream)) {
			final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
			// Specifies where the jpg image has to be written
			writer.setOutput(imageOutputStream);
	
			// Writes the file with given compression level
			// from your JPEGImageWriteParam instance
			writer.write(null, new IIOImage(request.getImage().getAsBufferedImage(), null, null), encodeParam);
			
			return byteArrayOutputStream.toByteArray();
		} catch (Exception e) {
			throw new CodecException("Error while encoding jpeg image: " + e.getLocalizedMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.jmage.Configurable#configure(org.jmage.ApplicationContext)
	 */
	public void configure(ApplicationContext context) {}
}
