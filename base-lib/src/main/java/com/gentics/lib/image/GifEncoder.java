/*
 * @author norbert
 * @date 18.09.2006
 * @version $Id: GifEncoder.java,v 1.3 2009-12-16 16:12:23 herbert Exp $
 */
package com.gentics.lib.image;

import java.io.ByteArrayOutputStream;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.media.jai.PlanarImage;

import com.sun.media.imageioimpl.plugins.gif.GIFImageWriterSpi;
import com.sun.media.imageioimpl.plugins.gif.GIFImageWriter;

import org.jmage.ApplicationContext;
import org.jmage.ImageRequest;
import org.jmage.encoder.CodecException;
import org.jmage.encoder.ImageEncoder;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * Image encoder to write gif images
 */
public class GifEncoder implements ImageEncoder {

	private static NodeLogger logger = NodeLogger.getNodeLogger(GifEncoder.class);

	/**
	 * Create an instance of the encoder
	 */
	public GifEncoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#canHandle(org.jmage.ImageRequest)
	 */
	public boolean canHandle(ImageRequest request) {
		String encodingFormat = ObjectTransformer.getString(request.getEncodingFormat(), "").toLowerCase();

		return "gif".equalsIgnoreCase(encodingFormat);
	}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#initialize(org.jmage.ImageRequest)
	 */
	public void initialize(ImageRequest request) throws CodecException {}

	/* (non-Javadoc)
	 * @see org.jmage.encoder.ImageEncoder#createFrom(org.jmage.ImageRequest)
	 */
	public byte[] createFrom(ImageRequest request) throws CodecException {
		if (logger.isDebugEnabled()) {
			logger.debug("Encoding gif image");
		}

		try {
			// BufferedImage buffered = null;
			PlanarImage image = request.getImage();
			// ColorModel colorModel = image.getColorModel();
			// if (logger.isDebugEnabled()) {
			// logger.debug("original image has color model " + colorModel.toString());
			// }
			//
			// GifEncoderHelper encoder = null;
			// if (colorModel instanceof IndexColorModel) {
			// IndexColorModel m = (IndexColorModel) colorModel;
			// byte[] r = new byte[m.getMapSize()];
			// byte[] g = new byte[m.getMapSize()];
			// byte[] b = new byte[m.getMapSize()];
			// m.getReds(r);
			// m.getGreens(g);
			// m.getBlues(b);
			// for(int i = 0 ; i < r.length ; i ++) {
			// //                    System./**/out./**/printf("%2x, %2x, %2x\n", r[i], g[i], b[i]);
			// //                    System./**/out.printf(r[i] + "," + g[i] + "," + b[i]);
			// }
			//
			//
			// buffered = request.getImage().getAsBufferedImage();
			// try {
			// encoder = new GifEncoderHelper(buffered);
			// encoder.setTransparentPixel(((IndexColorModel)colorModel).getTransparentPixel());
			// } catch(IllegalArgumentException e) {
			// logger
			// .warn(
			// "Encoding of original image failed, doing fallback to image copy with new color palette",
			// e);
			// }
			// }
			//
			// // when no encoder has been set, do the fallback to image copy
			// if (encoder == null) {
			// // create an indexed 8-bit image
			// if (colorModel instanceof IndexColorModel) {
			// // create the image with the original color palette
			// buffered = new BufferedImage(image.getWidth(), image.getHeight(),
			// BufferedImage.TYPE_BYTE_INDEXED, (IndexColorModel) colorModel);
			// } else {
			// // create the image with a defaul color palette
			// buffered = new BufferedImage(image.getWidth(), image.getHeight(),
			// BufferedImage.TYPE_BYTE_INDEXED);
			// }
			// // draw the original image into the indexed image
			// buffered.getGraphics().drawImage(image.getAsBufferedImage(), 0, 0,
			// image.getWidth(), image.getHeight(), null);
			//
			// encoder = new GifEncoderHelper(buffered);
			// if (colorModel instanceof IndexColorModel) {
			// logger
			// .warn("Image was encoded with new color palette, so transparency might be lost");
			// }
			// }

			ByteArrayOutputStream dest = new ByteArrayOutputStream();
			// ImageIO.write(image.getAsBufferedImage(), "gif", dest);
			// encoder.write(dest);

			GIFImageWriterSpi gifSPI = new GIFImageWriterSpi();
			final ImageOutputStream ios = ImageIO.createImageOutputStream(dest);
			final ImageWriter gifWriter = new GIFImageWriter(gifSPI);
			final ImageWriteParam iwp = gifWriter.getDefaultWriteParam();
            
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			// iwp.setCompressionType(compression);
			// iwp.setCompressionQuality(compressionRate);
            
			gifWriter.setOutput(ios);
			gifWriter.write(null, new IIOImage(convertToTransparent(image.getAsBufferedImage()), null, null), iwp);
			ios.flush();
			gifWriter.dispose();
            
			return dest.toByteArray();
		} catch (Exception e) {
			logger.error("Error while encoding gif image", e);
			throw new CodecException("Error while encoding gif image: " + e.getLocalizedMessage());
		}
	}
    
	private BufferedImage convertToTransparent(BufferedImage src) {

		// performance boost
		if (src.getAlphaRaster() == null) {
			return src;
		}
        
		ColorModel cm = new DirectColorModel(24, 0x00ff0000, // Red
				0x0000ff00, // Green
				0x000000ff, // Blue
				0x10000// Alpha
				);
        
		WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
		BufferedImage custom = new BufferedImage(cm, wr, false, null);        
        
		custom.setData(src.getData());
		return custom;
	}
    
	/* (non-Javadoc)
	 * @see org.jmage.Configurable#configure(org.jmage.ApplicationContext)
	 */
	public void configure(ApplicationContext context) {}
}
