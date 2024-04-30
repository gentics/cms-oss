package com.gentics.lib.image;

import java.io.IOException;

import org.jmage.ApplicationContext;
import org.jmage.ImageRequest;
import org.jmage.encoder.CodecException;
import org.jmage.encoder.ImageEncoder;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

/**
 * ImageEncoder to write WebP files with configurable quality.
 */
public class WEBPEncoder implements ImageEncoder {

	private static NodeLogger logger = NodeLogger.getNodeLogger(WEBPEncoder.class);

	public final static String WEBP_QUALITY = "WEBP_QUALITY";
	public final static String WEBP_LOSSLESS = "WEBP_LOSSLESS";

	protected int quality = -1;

	protected Boolean lossless;

	@Override
	public boolean canHandle(ImageRequest imageRequest) {
		String encodingFormat = ObjectTransformer.getString(imageRequest.getEncodingFormat(), "").toLowerCase();

		return "webp".equals(encodingFormat);
	}

	@Override
	public void initialize(ImageRequest request) throws CodecException {
		Double q = ObjectTransformer.getDouble(request.getFilterChainProperties().getProperty(WEBP_QUALITY), -1);
		if (q >= 0.) {
			q = q * 100;
			quality = q.intValue();
			quality = Math.min(100, quality);
			quality = Math.max(0, quality);
		}

		lossless = ObjectTransformer.getBoolean(request.getFilterChainProperties().getProperty(WEBP_LOSSLESS), null);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Initialized %s WebP encoder with quality %d", ObjectTransformer.getBoolean(lossless, false) ? "lossless" : "lossy", quality));
		}
	}

	@Override
	public byte[] createFrom(ImageRequest imageRequest) throws CodecException {
		try {
			WebpWriter writer = WebpWriter.DEFAULT.withZ(9);
			if (lossless == Boolean.TRUE) {
				writer = writer.withLossless();
			}
			if (quality >= 0) {
				writer = writer.withQ(quality);
			}

			ImmutableImage image = ImmutableImage.fromAwt(imageRequest.getImage().getAsBufferedImage());
			return image.forWriter(writer).bytes();
		} catch (IOException e) {
			throw new CodecException(e.getLocalizedMessage());
		}
	}

	@Override
	public void configure(ApplicationContext applicationContext) {
	}
}
