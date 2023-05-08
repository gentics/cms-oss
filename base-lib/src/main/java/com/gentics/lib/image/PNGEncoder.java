/*
 * @author norbert
 * @date 18.09.2006
 * @version $Id: PNGEncoder.java,v 1.2 2009-12-16 16:12:23 herbert Exp $
 */
package com.gentics.lib.image;

import java.io.ByteArrayOutputStream;

import javax.media.jai.JAI;

import org.jmage.ApplicationContext;
import org.jmage.ImageRequest;
import org.jmage.encoder.CodecException;
import org.jmage.encoder.ImageEncoder;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;
import com.sun.media.jai.codec.PNGEncodeParam;

/**
 * ImageEncoder to write PNG files.
 */
public class PNGEncoder implements ImageEncoder {
	private static final String ENCODE = "encode";

	private static NodeLogger logger = NodeLogger.getNodeLogger(PNGEncoder.class);

	/**
	 * Create an instance of the encoder
	 */
	public PNGEncoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#canHandle(org.jmage.ImageRequest)
	 */
	public boolean canHandle(ImageRequest request) {
		String encodingFormat = ObjectTransformer.getString(request.getEncodingFormat(), "").toLowerCase();

		return "png".equalsIgnoreCase(encodingFormat);
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#initialize(org.jmage.ImageRequest)
	 */
	public void initialize(ImageRequest request) throws CodecException {}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#createFrom(org.jmage.ImageRequest)
	 */
	public byte[] createFrom(ImageRequest request) throws CodecException {
		PNGEncodeParam encodeParam = PNGEncodeParam.getDefaultEncodeParam(request.getImage());
		ByteArrayOutputStream dest = new ByteArrayOutputStream();

		JAI.create(ENCODE, request.getImage(), dest, "png", encodeParam);
		return dest.toByteArray();
	}

	/* (non-Javadoc)
	 * @see org.jmage.Configurable#configure(org.jmage.ApplicationContext)
	 */
	public void configure(ApplicationContext context) {}
}
