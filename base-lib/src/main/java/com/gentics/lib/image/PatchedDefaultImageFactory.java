/*
 * @author norbert
 * @date 06.09.2006
 * @version $Id: PatchedDefaultImageFactory.java,v 1.2 2006-12-01 13:28:50 norbert Exp $
 */
package com.gentics.lib.image;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.jmage.resource.DefaultImageFactory;
import org.jmage.resource.ResourceException;

import com.gentics.lib.log.NodeLogger;
import com.sksamuel.scrimage.ImmutableImage;
import com.sun.media.jai.codec.FileSeekableStream;

/**
 * Patched Version of the DefaultImageFactory. This class modifies the loading of images such that it uses operation "stream" and forces the stream to be closed after
 * loading.
 */
public class PatchedDefaultImageFactory extends DefaultImageFactory {
	private static final String STREAMLOAD = "stream";

	NodeLogger logger = NodeLogger.getNodeLogger(PatchedDefaultImageFactory.class);
	private static final String CAUSE = ", cause: ";
	private static final String FILE_LOADED = " loaded image from file: ";
	private static final String SCHEME_ERROR = "unable to retrieve resource, could not handle scheme: ";
	private static final String FILE_LOADERROR = "unable to load image from file: ";
	private static final String URL_LOADERROR = "unable to load image from URL: ";
	private static final String URL_LOADED = " loaded image from url: ";
	private static final String URL_RESOURCE_RETRIEVED = " retrieved URL resource: ";
	private static final String STREAM = "stream";

	/**
	 * Create instance of the patched default image factory
	 */
	public PatchedDefaultImageFactory() {
		super();
		imageTypes.add("jpe");
		imageTypes.add("webp");
	}

	@Override
	public boolean canHandle(URI var1) {
		try {
			String var2 = var1.getPath().substring(var1.getPath().lastIndexOf(46) + 1).toLowerCase();
			String var3 = var1.getScheme();
			return this.imageTypes.contains(var2) && this.schemeTypes.contains(var3);
		} catch (Exception var4) {
			return false;
		}
	}
	/**
	 * Create an object resource from a resource URI
	 *
	 * @param resource
	 *            the resource URI
	 * @return PlanarImage
	 * @throws ResourceException
	 */
	public Object createFrom(URI resource) throws ResourceException {
		String scheme = resource.getScheme().toLowerCase();

		if ("file".equals(scheme)) {
			File file = new File(resource);

			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(file);
				BufferedImage image = ImageIO.read(fileInputStream);

				if (image == null) {
					ImmutableImage image2 = ImmutableImage.loader().fromFile(file);
					if (image2 != null) {
						image = image2.awt();
					}
				}

				return PlanarImage.wrapRenderedImage(image);
			} catch (Exception e) {
				String msg = "Could not load image from file.";

				logger.error(msg, e);
				throw new ResourceException(msg);
			} finally {
				if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {
					}
				}
			}
		}

		if ("http".equals(scheme)) {
			URL url = null;

			try {
				url = resource.toURL();
			} catch (MalformedURLException e) {
				throw new ResourceException(e.getMessage());
			}
			PlanarImage image = this.getURL(url);

			if (logger.isInfoEnabled()) {
				logger.info(URL_RESOURCE_RETRIEVED + url.toString());
			}
			return image;
		}
		throw new ResourceException(SCHEME_ERROR + scheme);
	}

	/**
	 * Get the image from an URL
	 *
	 * @param url
	 *            the url
	 * @return the image
	 * @throws ResourceException
	 */
	@Override
	protected PlanarImage getURL(URL url) throws ResourceException {
		PlanarImage image = null;
		final String errorMessage = URL_LOADERROR + url.toString();

		try {
			byte[] urlBytes = this.readFromUrl(url).toByteArray();
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(urlBytes));

			if (bufferedImage != null) {
				ImmutableImage image2 = ImmutableImage.loader().fromBytes(urlBytes);
				if (image2 != null) {
					bufferedImage = image2.awt();
				}
			}

			image = PlanarImage.wrapRenderedImage(bufferedImage);
			if (logger.isDebugEnabled()) {
				logger.debug(URL_LOADED + url.toString());
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error(errorMessage + CAUSE + e.getMessage());
			}
			throw new ResourceException(errorMessage);
		}

		// null? throw resourceexception
		if (image == null) {
			if (logger.isErrorEnabled()) {
				logger.error(errorMessage);
			}
			throw new ResourceException(errorMessage);
		}
		return image;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.jmage.resource.DefaultImageFactory#getAbsoluteFile(java.io.File)
	 */
	protected PlanarImage getAbsoluteFile(File file) {
		PlanarImage image = null;

		if (file.isAbsolute() && file.exists()) {
			try {
				InputStream fileStream = new FileSeekableStream(file);
				ParameterBlock pb = new ParameterBlock();

				pb.add(fileStream);
				pb.add(null);
				image = JAI.create(STREAMLOAD, pb, null);
				image.getData();
				fileStream.close();
				if (logger.isDebugEnabled()) {
					logger.debug(FILE_LOADED + file.getAbsolutePath());
				}
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error(FILE_LOADERROR, e);
				}
			}
		}
		return image;
	}
}
