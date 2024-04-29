package com.gentics.lib.image;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;
import org.jmage.ApplicationContext;
import org.jmage.ImageRequest;
import org.jmage.encoder.CodecException;
import org.jmage.encoder.ImageEncoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ImageEncoder to write WebP files with configurable quality.
 */
public class WEBPEncoder implements ImageEncoder {

	private static NodeLogger logger = NodeLogger.getNodeLogger(WEBPEncoder.class);

	public final static String WEBP_QUALITY = "WEBP_QUALITY";
	public final static String WEBP_LOSSLESS = "WEBP_LOSSLESS";

	protected ImageWriteParam writeParams;

	@Override
	public boolean canHandle(ImageRequest imageRequest) {
		String encodingFormat = ObjectTransformer.getString(imageRequest.getEncodingFormat(), "").toLowerCase();

		return "webp".equals(encodingFormat);
	}

	@Override
	public void initialize(ImageRequest request) throws CodecException {
		Double quality = ObjectTransformer.getDouble(request.getFilterChainProperties().getProperty(WEBP_QUALITY), null);
		Boolean lossless = ObjectTransformer.getBoolean(request.getFilterChainProperties().getProperty(WEBP_LOSSLESS), null);

		writeParams = ImageIO.getImageWritersByMIMEType("image/webp").next().getDefaultWriteParam();

		if (quality != null) {
			writeParams.setCompressionQuality(quality.floatValue());
		}

		if (lossless != null) {
			writeParams.setCompressionType(lossless.booleanValue() ? "Lossless" : "Lossy");
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Initialized %s WebP encoder with quality %f", writeParams.getCompressionType(), writeParams.getCompressionQuality()));
		}
	}

	@Override
	public byte[] createFrom(ImageRequest imageRequest) throws CodecException {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);
			ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

			writer.setOutput(imageOutputStream);
			writer.write(null, new IIOImage(imageRequest.getImage().getAsBufferedImage(), null, null), writeParams);

			// Auto-closing broke the tests for some reason, so the imageOutputStream is closed manually.
			imageOutputStream.close();

			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void configure(ApplicationContext applicationContext) {
	}
}
