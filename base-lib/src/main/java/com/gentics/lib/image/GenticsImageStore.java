package com.gentics.lib.image;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;
import com.gentics.lib.util.FileUtil;
import javax.activation.MimetypesFileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jmage.ApplicationContext;
import org.jmage.ConfigurationException;
import org.jmage.ImageRequest;
import org.jmage.JmageException;
import org.jmage.dispatcher.RequestDispatcher;

public class GenticsImageStore {

	/**
	 * Limit of the resizing factor, if resizing is with factor &lt; this limit it is done in steps
	 */
	protected double factorLimit = -1;

	/**
	 * flag which decides what happens if resizing of an image fails
	 * true: only an error message is logged and the original image is returned
	 * false: an exception is thrown
	 * TODO: There is no setter for this value, is this intended?
	 */
	protected boolean useOriginalIfResizeFails = true;

	protected Double jpegQuality;

	protected Double webpQuality;

	protected Boolean webpLossless;

	/**
	 * Optional semaphore to use for locking of resizing operations
	 */
	protected Semaphore semaphore;

	/**
	 * Optional try timeout
	 */
	protected long tryTimeout;

	/**
	 * Optional try timeout unit
	 */
	protected TimeUnit tryTimeoutUnit;

	/**
	 * Timeout in ms for loading the image via URL
	 */
	protected int loadTimeout = 60 * 1000;

	/**
	 * name of the cache region for resized images
	 */
	public final static String CACHEREGION = "gentics-content-imagestorecache";

	/**
	 * Pattern to determine the filetype from the contenttype
	 */
	protected final static Pattern IMAGE_TYPE_PATTERN = Pattern.compile("image/(.+)");

	/**
	 * key to decide if crop and resize functionality should be enabled
	 */
	public final static String CROP_AND_RESIZE_MODE_KEY = "cropandresize";

	/**
	 * Key that is used to switch to the smart resize feature
	 */
	public final static String SMART_MODE_KEY = "smart";

	/**
	 * buffer size for writing the image to the response
	 */
	public final static int BUFFER_SIZE = 4096;

	/**
	 * Parameter name of the "secret" for resize validation
	 */
	public final static String SECRET_PARAMETER = "secret";

	/**
	 * Parameter name of the "validation" request parameter for the resize validation
	 */
	public final static String VALIDATION_PARAMETER = "validation";

	/**
	 * Pattern for the pathinfo for resizing an image
	 */
	public final static Pattern PATH_INFO_PATTERN = Pattern.compile("(/([^/]+)/([^/]+)/([^/]+)).*");

	/**
	 * Group Index for the full resizing path
	 */
	public final static int PATH_INFO_PATTERN_FULL = 1;

	/**
	 * Group Index for the width
	 */
	public final static int PATH_INFO_PATTERN_WIDTH = 2;

	/**
	 * Group Index for the height
	 */
	public final static int PATH_INFO_PATTERN_HEIGHT = 3;

	/**
	 * Group index for the mode
	 */
	public final static int PATH_INFO_PATTERN_MODE = 4;

	/**
	 * pattern for the extended pathinfo for resizing and cropping an image
	 */
	public final static Pattern PATH_INFO_EXTENDED_PATTERN = Pattern.compile("(/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)).*");

	public final static int PATH_INFO_PATTERN_EXTMODE = 5;

	public final static int PATH_INFO_PATTERN_TOPLEFT_X = 6;

	public final static int PATH_INFO_PATTERN_TOPLEFT_Y = 7;

	public final static int PATH_INFO_PATTERN_CROPWIDTH = 8;

	public final static int PATH_INFO_PATTERN_CROPHEIGHT = 9;

	public static NodeLogger logger = NodeLogger.getNodeLogger(GenticsImageStore.class);

	public final static byte[] JFIF_HEADER = {(byte)0xff, (byte)0xd8, (byte)0xff, (byte)0xe1};

	/**
	 * Transform the mode from allowed short forms to the real mode names
	 * @param mode given mode
	 * @return transformed mode
	 */
	protected String transformMode(String mode) {
		if ("prop".equals(mode)) {
			return "proportional";
		} else if ("force".equals(mode)) {
			return "unproportional";
		} else {
			return mode;
		}
	}

	/**
	 * Sets the jpg quality value. Default is null
	 * @param jpegQuality
	 */
	public void setJPEGQuality(Double jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	public void setWebpQuality(Double webpQuality) {
		this.webpQuality = webpQuality;
	}

	public void setWebpLossless(Boolean webpLossless) {
		this.webpLossless = webpLossless;
	}

	/**
	 * Sets semaphore for locking (null for no locking)
	 * @param semaphore semaphore
	 * @param timeout timeout
	 * @param unit unit for timeout
	 */
	public void setSemaphore(Semaphore semaphore, long timeout, TimeUnit unit) {
		this.semaphore = semaphore;
		Objects.requireNonNull(unit, "TimeUnit must not be empty");
		this.tryTimeout = timeout;
		this.tryTimeoutUnit = unit;
	}

	/**
	 * Set the load timeout in ms
	 * @param loadTimeout load timeout in ms
	 */
	public void setLoadTimeout(int loadTimeout) {
		this.loadTimeout = loadTimeout;
	}

	/**
	 * Invokes the GIS and returns the resized image
	 *
	 * @param mode
	 * @param width
	 * @param height
	 * @param fileInformation a fileInformation object which MUST be backed by a file (it uses {@link FileInformation#getFilePath()})
	 * @return byte array containing the resized image data
	 * @throws ServletException
	 */
	protected FileInformation invokeGIS(String mode, String width, String height, boolean cropImage, String topleftx, String toplefty, String cwidth, String cheight,
			FileInformation fileInformation) throws ServletException {
		String filePath = fileInformation.getFilePath();

		mode = transformMode(mode);
		String filterChain = "smart".equals(mode) ? SmarterResizeFilter.class.getCanonicalName() : ResizeFilter.class.getCanonicalName();
		Properties filterChainProperties = new Properties();

		if (ObjectTransformer.getInt(width, -1) > 0) {
			filterChainProperties.put("WIDTH", width);
		}
		if (ObjectTransformer.getInt(height, -1) > 0) {
			filterChainProperties.put("HEIGHT", height);
		}
		filterChainProperties.put("MODE", mode);
		if (factorLimit > 0) {
			filterChainProperties.put("FACTORLIMIT", new Double(factorLimit));
		}

		Properties cropProperties = new Properties();
		if (cropImage) {
			cropProperties.put("cropandresize", "true");
			cropProperties.put("TOPLEFTX", topleftx);
			cropProperties.put("TOPLEFTY", toplefty);
			cropProperties.put("WIDTH", cwidth);
			cropProperties.put("HEIGHT", cheight);
		}

		if (jpegQuality != null) {
			filterChainProperties.put("JPEG_QUALITY", jpegQuality);
		}

		if (webpQuality != null) {
			filterChainProperties.put(WEBPEncoder.WEBP_QUALITY, webpQuality);
		}

		if (webpLossless != null) {
			filterChainProperties.put(WEBPEncoder.WEBP_LOSSLESS, webpLossless);
		}

		if (!StringUtils.isEmpty(filePath)) {
			filterChainProperties.put("filePath", filePath);
		}

		GenticsImageStoreResizeResponse response;

		boolean lock = false;
		Semaphore s = semaphore;
		TimeUnit unit = tryTimeoutUnit;
		long timeout = tryTimeout;
		try {
			if (s != null) {
				lock = s.tryAcquire(timeout, unit);
				if (!lock) {
					throw new NodeException(String.format("Timeout (%d %s) while waiting for semaphore", timeout, unit));
				}
			}
			try {
				response = handleResizeAction(filePath, null, null, null, cropImage, null, filterChain, filterChainProperties, cropProperties);
				if (response != null) {
					return response.getFileInformation();
				} else {
					throw new NodeException("Image resize failed with unknown reason.");
				}
			} catch (Exception e) {
				throw new NodeException("Image resize failed with internal reason.", e);
			}
		} catch (NodeException | InterruptedException e1) {
			String msg = "FilterImageAction failed on file {" + filePath + "}";

			if (useOriginalIfResizeFails) {
				logger.error(msg + " - using original image data.");
				// An error happened, so read in the file and store it in an FileInformation.
				// Note: this is done, so we don't store a FileInformation object in the cache which
				// contains only a reference to a file..
				File file = new File(filePath);
				byte[] bytes = new byte[(int) file.length()];

				FileInputStream is = null;

				try {
					is = new FileInputStream(file);
					// Read in the bytes
					int offset = 0;
					int numRead = 0;

					while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) > 0) {
						offset += numRead;
					}
				} catch (IOException e) {
					logger.fatal("Error while reading original file contents.");
					throw new ServletException("Error while reading original file contents.");
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {// not interrested in this exception
						}
					}
				}
				return new FileInformation(fileInformation.getFileName(), file.length(), fileInformation.getContentType(), bytes);
			} else {
				throw new ServletException(msg);
			}
		} finally {
			if (lock && s != null) {
				s.release();
			}
		}
	}

	/**
	 * Resize the given image using the given arguments.
	 * The file data can be located using the filePathValue, fileUri or by using the fileInputStream
	 *
	 * @param filePathValue
	 * @param encoding
	 * @param cropandresize
	 * @param fileURIValue
	 * @param fileInputStream
	 * @param originalFilename
	 * @param filterChain
	 * @param filterChainProperties The resize parameters
	 * @param cropProperties The crop parameters
	 * @return Never returns null
	 * @throws NodeException
	 */
	public static GenticsImageStoreResizeResponse handleResizeAction(String filePathValue, String fileURIValue, InputStream fileInputStream, String encoding, boolean cropandresize, String originalFilename, String filterChain, Properties filterChainProperties, Properties cropProperties) throws NodeException {
		URI fileURI = null;
		File tempFile = null;
		String fileName = null;

		// Try the different source options.
		// 1. Filesystem filepath
		if (StringUtils.isEmpty(filePathValue)) {
			// 2. File URI
			if (StringUtils.isEmpty(fileURIValue)) {
				// 3. Inputstream data
				if (fileInputStream != null) {
					try {
						// store the image into a file
						tempFile = File.createTempFile("tempFile", ".jpeg");
						FileOutputStream outStream = new FileOutputStream(tempFile);
						byte[] buffer = new byte[4096];
						int read = 0;

						while ((read = fileInputStream.read(buffer)) >= 0) {
							outStream.write(buffer, 0, read);
						}

						outStream.close();
						fileInputStream.close();

						fileURI = tempFile.toURI();
					} catch (IOException ex) {
						throw new NodeException("Could not save the image from stream to temp location.", ex);
					}
				} else {
					throw new NodeException("No source option was set. filePathValue, fileURIValue and fileInputStream were null");
				}
			} else {
				try {
					fileURI = new URI(fileURIValue);
				} catch (URISyntaxException e1) {
					throw new NodeException("Could not parse fileUriValue {" + fileURIValue + "}", e1);
				}
			}
			int lastSlash = Math.max(fileURI.toString().lastIndexOf('/'), fileURI.toString().lastIndexOf('\\'));
			fileName = fileURI.toString().substring(lastSlash + 1);

		} else {
			// create the file here
			File file = new File(filePathValue);

			fileURI = file.toURI();
			fileName = file.getName();
		}

		String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
		String imageType = null;

		// when a original filename has been given, we set it now
		fileName = ObjectTransformer.getString(originalFilename, fileName);

		// if the extension is not set we need to create a tempfile with the correct extension and repair the content type
		File tmpFile = null;
		if (encoding == null || "original".equals(encoding)) {
			Matcher m = IMAGE_TYPE_PATTERN.matcher(contentType);

			if (m.matches()) {
				imageType = m.group(1);
			} else {
				try {
					contentType = FileUtil.getMimeTypeByContent(new FileInputStream(new File(fileURI)), fileName);
					m = IMAGE_TYPE_PATTERN.matcher(contentType);
					if (m.matches()) {
						imageType = m.group(1);
						try {
							tmpFile = File.createTempFile("gtximagestore", "." + imageType);
							FileUtils.copyFile(new File(fileURI), tmpFile);
							fileURI = tmpFile.toURI();
						} catch (IOException e) {
							throw new NodeException("Could not create temporary image", e);
						}
					} else {
						throw new NodeException("Could not determine filetype of image" + fileName);
					}
				} catch (FileNotFoundException e) {
					throw new NodeException(e);
				}
			}
		} else {
			// the encoding needs to be changed
			imageType = encoding;
			int slash = encoding.indexOf('/');
			String pureEncoding = slash > 0 ? encoding.substring(0, slash) : encoding;

			// also change contentType and filename
			contentType = "image/" + pureEncoding;
			fileName = fileName.substring(0, fileName.lastIndexOf('.')) + "." + pureEncoding;
			if (logger.isDebugEnabled()) {
				logger.debug("Change image encoding to {" + encoding + "}. New contentType is {" + contentType + "}, new fileName is {" + fileName + "}");
			}
		}

		// now let JMage process the image
		ApplicationContext context = ApplicationContext.getContext();
		RequestDispatcher requestDispatcher = null;

		try {
			requestDispatcher = context.obtainRequestDispatcher();
		} catch (ConfigurationException e) {
			throw new NodeException(e);
		}

		try {
			ImageRequest imageRequest = new ImageRequest();
			imageRequest.setImageURI(fileURI);
			imageRequest.setEncodingFormat(imageType);

			if (cropandresize) {
				// PRECROP IMAGE
				String cropFilter = CropFilter.class.getCanonicalName();

				imageRequest.setFilterChainURI(new URI[] { new URI("chain:" + cropFilter), new URI("chain:" + filterChain)});
				Properties props = new Properties();
				props.put(cropFilter, cropProperties);
				props.put(filterChain, filterChainProperties);
				imageRequest.setFilterChainProperties(props);
			} else {
				// set the filter chain URI (the extending rotation filter)
				if (filterChain != null) {
					imageRequest.setFilterChainURI(new URI("chain:" + filterChain));
				}

				// set the filter chain properties
				imageRequest.setFilterChainProperties(filterChainProperties);
			}

			// dispatch the request
			requestDispatcher.dispatch(imageRequest);

			// get the result
			byte[] resultImage = imageRequest.getEncoded();

			GenticsImageStoreResizeResponse response = new GenticsImageStoreResizeResponse();
			response.setImageData(resultImage);
			response.setFileInformation(new FileInformation(fileName, resultImage.length, contentType, resultImage));
			return response;
		} catch (URISyntaxException e) {
			throw new NodeException("Syntax of given filterChain or cropFilter could not be parsed.", e);
		} catch (JmageException e) {
			throw new NodeException("Error while handling resize of {" + filePathValue + "}",e);
		} finally {
			if (tmpFile != null) {
				tmpFile.delete();
			}
			if (tempFile != null) {
				tempFile.delete();
			}

			try {
				context.releaseRequestDispatcher(requestDispatcher);
			} catch (ConfigurationException e) {
				throw new NodeException(e);
			}
		}
	}

	/**
	 * Create the cache key for the cropped and resized image
	 * @param fileId fileId of the file
	 * @param mode resizing mode
	 * @param width new width (may also be "auto")
	 * @param height (may also be "auto")
	 * @param eDate edate, if already set (or -1 if not yet set)
	 * @return cache key
	 */
	protected Object createCropCacheKey(String fileId, String mode, String width, String height, String topx, String topy, String cwidth, String cheight, int eDate) throws NodeException {
		mode = transformMode(mode);
		StringBuffer cacheKey = new StringBuffer();
		cacheKey.append(fileId).append("|").append(eDate).append("|").append(mode).append("|").append(width).append("|").append(height).append("|").append(CROP_AND_RESIZE_MODE_KEY).append("|").append(topx).append(",").append(topy).append("|").append(cwidth).append("|").append(
				cheight);
		return cacheKey.toString();
	}

	/**
	 * Create the cache key for the resized image
	 * @param fileId fileId of the file
	 * @param mode resizing mode
	 * @param width new width (may also be "auto")
	 * @param height (may also be "auto")
	 * @param eDate edate, if already set (or -1 if not yet set)
	 * @return cache key
	 */
	protected Object createCacheKey(String fileId, String mode, String width, String height, int eDate) {
		mode = transformMode(mode);
		StringBuffer cacheKey = new StringBuffer();
		cacheKey.append(fileId).append("|").append(eDate).append("|").append(mode).append("|").append(width).append("|").append(height);
		return cacheKey.toString();
	}

	/**
	 * Get the resized image from the cache or null if not cached
	 * @param cacheKey cache key
	 * @return resized image from the cache or null
	 */
	protected FileInformation getCachedImage(Object cacheKey) {
		try {
			PortalCache cache = PortalCache.getCache(CACHEREGION);

			if (cache == null) {
				logger.warn("Unable to retrieve cache for " + CACHEREGION);
				return null;
			}
			Object resizedImage = cache.get(cacheKey);

			if (resizedImage instanceof FileInformation) {
				return (FileInformation) resizedImage;
			} else {
				if (resizedImage != null) {
					// log warn message (cached object was not an image)
					logger.warn(
							"Error while fetching cached image {" + cacheKey + "}: cached object was {" + resizedImage.getClass().getName() + "}, but {"
							+ FileInformation.class.getName() + "} was expected.");
				}
				return null;
			}
		} catch (PortalCacheException e) {
			logger.warn("Error while fetching cached image {" + cacheKey + "}: ", e);
			return null;
		}
	}

	/**
	 * Get the image from the given url
	 * @param fileURL url
	 * @param cookies array of cookies to be sent to the URL when fetching the image (may be null or empty)
	 * @return image content
	 * @throws ServletException
	 * @throws GenticsImageStoreException
	 */
	protected FileInformation getImageFromURL(String fileURL, Cookie[] cookies, Map<String, String> headers) throws ServletException,
				IOException, GenticsImageStoreException {
		if (logger.isDebugEnabled()) {
			logger.debug("Fetching image from {" + fileURL + "}");
		}
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpGet request = new HttpGet(fileURL);

		String filename = "";
		// try to guess the filename
		String path = new URL(fileURL).getPath();

		if (path != null) {
			int lastPathSep = path.lastIndexOf('/');
			filename = path.substring(lastPathSep + 1);
		}

		// add the cookies as headers
		if (!ObjectTransformer.isEmpty(cookies)) {
			BasicCookieStore cookieStore = new BasicCookieStore();
			for (Cookie cookie : cookies) {
				BasicClientCookie clientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
				clientCookie.setComment(cookie.getComment());
				clientCookie.setDomain(ObjectTransformer.getString(cookie.getDomain(), ""));
				clientCookie.setPath(cookie.getPath());
				clientCookie.setSecure(cookie.getSecure());
				clientCookie.setVersion(cookie.getVersion());
				cookieStore.addCookie(clientCookie);
			}
			clientBuilder.setDefaultCookieStore(cookieStore);
		}

		addHeaders(request,headers);

		RequestConfig requestConfig = RequestConfig
				.copy(RequestConfig.DEFAULT)
				.setSocketTimeout(loadTimeout)
				.setConnectTimeout(loadTimeout)
				.build();

		CloseableHttpClient client = clientBuilder.setDefaultRequestConfig(requestConfig).build();
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new GenticsImageStoreException(response.getStatusLine()
					.getReasonPhrase(), response.getStatusLine()
					.getStatusCode());
		}

		byte[] content = IOUtils.toByteArray(response.getEntity().getContent());

		// Find out the correct mimetype
		String contentType = null;
		if (content != null && content.length > 0) {
			// If the http server returned an image mimetype, us that
			Header contentTypeHeader = response.getFirstHeader("Content-Type");
			if (contentTypeHeader != null) {
				contentType = contentTypeHeader.getValue();
			}

			if (contentType == null || !contentType.startsWith("image/")) {
				// No image mimetype was received, try to parse the file
				InputStream contentInputStream = new ByteArrayInputStream((byte[]) content);
				contentType = FileUtil.getMimeType(contentInputStream, filename);
			}
		} else {
			throw new GenticsImageStoreException("No content found for fileUrl: "+fileURL, HttpStatus.SC_BAD_REQUEST);
		}

		String extensionWithDot = FileUtil.getExtensionByMimeType(contentType);
		if (extensionWithDot == null) {
			extensionWithDot = ".bin";
		}

		// create a temporary file
		File tempFile = File.createTempFile("gtxImageStore", extensionWithDot);
		FileOutputStream out = new FileOutputStream(tempFile);
		out.write((byte[]) content);
		out.close();

		if (ObjectTransformer.isEmpty(contentType)) {
			return new FileInformation(tempFile);
		} else {
			return new FileInformation(tempFile.getName(), tempFile.length(), contentType, null, tempFile, false);
		}
	}

	/**
	 * Add headers to the image request.
	 * The Host-header must be derived automatically from the URL.
	 * Disable the cache and prevent getting "304 - Not Modified".
	 * @param request the request to add the headers
	 * @param headers the headers to add to the request
	 */
	private void addHeaders(HttpGet request, Map<String, String> headers) {
		//process headers
		for (String headerKey: headers.keySet()) {
			if (HttpHeaders.HOST.equalsIgnoreCase(headerKey)
				|| HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(headerKey)
				|| HttpHeaders.PRAGMA.equalsIgnoreCase(headerKey)
				|| HttpHeaders.EXPIRES.equalsIgnoreCase(headerKey)
				|| HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(headerKey)
				|| HttpHeaders.IF_NONE_MATCH.equalsIgnoreCase(headerKey)) {
				continue;
			}
			request.addHeader(headerKey, headers.get(headerKey));
		}
		request.addHeader(HttpHeaders.CACHE_CONTROL,"private, no-store, no-cache, must-revalidate");
		request.addHeader(HttpHeaders.PRAGMA, "no-cache");
		request.addHeader(HttpHeaders.EXPIRES, "0");
	}

	/**
	 * Put the resized image into the cache
	 * @param resizedImage resized image
	 * @param cacheKey cachekey
	 */
	protected void putImageIntoCache(FileInformation resizedImage, Object cacheKey) {
		try {
			PortalCache cache = PortalCache.getCache(CACHEREGION);

			if (cache == null) {
				logger.warn("Unable to put image into cache - Got no cache for region {" + CACHEREGION + "}");
				return;
			}
			cache.put(cacheKey, resizedImage);
		} catch (PortalCacheException e) {
			logger.warn("Error while caching the resized image.", e);
		}
	}

	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param cropandresize whether the image shall be cropped and resized
	 * @param topleftx x coordinate of the top left corner for cropping
	 * @param toplefty y coordinate of the top left corner for cropping
	 * @param cropwidth width for cropping
	 * @param cropheight height for cropping
	 * @param fileId file id as integer
	 * @param queryString query string
	 * @param cookies array of cookies to be added when fetching the image from the URL (may be null or empty)
	 * @return the fileinformation of the resized image
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	public FileInformation doResizing(String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty,
			String cropwidth, String cropheight, int fileId, String queryString, Cookie[] cookies, Map<String, String> headers) throws ServletException, IOException,
				NodeException {
		return doResizing(mode, width, height, cropandresize, topleftx, toplefty, cropwidth, cropheight, fileId + "", queryString, cookies, headers);
	}


	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param cropandresize whether the image shall be cropped and resized
	 * @param topleftx x coordinate of the top left corner for cropping
	 * @param toplefty y coordinate of the top left corner for cropping
	 * @param cropwidth width for cropping
	 * @param cropheight height for cropping
	 * @param fileUrl file id as string
	 * @param queryString query string (may be null)
	 * @param cookies array of cookies to be added when fetching the image from the URL (may be null or empty)
	 * @return the fileinformation of the resized image
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	public FileInformation doResizing(String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty,
			String cropwidth, String cropheight, String fileUrl, String queryString, Cookie[] cookies, Map<String, String> headers) throws ServletException, IOException,
				NodeException {

		// first try to get the image from the cache
		Object cacheKey = null;

		if (cropandresize) {
			cacheKey = createCropCacheKey(fileUrl, mode, width, height, topleftx, toplefty, cropwidth, cropheight, 0);
		} else {
			cacheKey = createCacheKey(fileUrl, mode, width, height, 0);
		}
		return doResizing(cacheKey, mode, width, height, cropandresize, topleftx, toplefty, cropwidth, cropheight, fileUrl, fileUrl, queryString, cookies, headers);

	}

	/**
	 * Do the resizing of an image and return the image
	 *
	 * @param cacheKey The used cacheKey for the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param cropandresize whether the image shall be cropped and resized
	 * @param topleftx x coordinate of the top left corner for cropping
	 * @param toplefty y coordinate of the top left corner for cropping
	 * @param cropwidth width for cropping
	 * @param cropheight height for cropping
	 * @param fileUrlForCacheKey file url as string
	 * @param queryString query string (may be null)
	 * @param cookies array of cookies to be added when fetching the image from the URL (may be null or empty)
	 * @return the fileinformation of the resized image
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	protected FileInformation doResizing(Object cacheKey, String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty,
			String cropwidth, String cropheight, String imageUrl, String fileUrlForCacheKey, String queryString, Cookie[] cookies, Map<String, String> headers) throws ServletException, IOException,
				NodeException {

		RuntimeProfiler.beginMark(JavaParserConstants.IMAGESTORE_DORESIZE);

		// Verify width/height parameters ...
		// Either two integers, or one integer and one 'auto'
		Integer widthInt = ObjectTransformer.getInteger(width, null);
		Integer heightInt = ObjectTransformer.getInteger(height, null);

		if (widthInt == null || heightInt == null) {
			if ((widthInt == null && heightInt == null) || (!"auto".equals(width) && !"auto".equals(height))) {
				throw new IllegalArgumentException(
						"Invalid width/height - One has to be a valid integer while the other might be 'auto'. width: {" + width + "} height: {" + height + "}");
			}
		}

		boolean tempImage = false;
		File srcImage = null;

		try {

			FileInformation cachedImage = null;

			// create the cachekey
			if (cacheKey == null) {
				logger.warn("Could not create cachekey for image {" + fileUrlForCacheKey + "}, not using cache.");
			} else {
				cachedImage = getCachedImage(cacheKey);
			}

			if (cachedImage == null) {
				// make a call to content.node and fetch the image (store
				// it temporarily and set the filePath)

				String completeURL = buildFileUrl(imageUrl, queryString);
				FileInformation tempFileInfo = getImageFromURL(completeURL, cookies, headers);
				srcImage = new File(tempFileInfo.getFilePath());
				// check whether we fetched an image
				boolean isImage = tempFileInfo.getContentType().startsWith("image");
				tempImage = true;
				if (isImage) {
					FileInformation resizedImage = invokeGIS(mode, width, height, cropandresize, topleftx, toplefty, cropwidth, cropheight,
							tempFileInfo);

					// put the resized image into the cache
					putImageIntoCache(resizedImage, cacheKey);
					return resizedImage;
				} else {
					logger.error("Did not fetch an image from url {" + completeURL + "}: response was of contenttype {" + tempFileInfo.getContentType() + "}");
					return tempFileInfo;
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Image was fetched from cache - key {" + cacheKey + "}");
				}
				// image was fetched from the cache
				return cachedImage;
				// writeImageToResponse(cachedImage, response);
			}
		} finally {
			// remove a temporary file
			if (tempImage && srcImage != null) {
				srcImage.delete();
			}
			RuntimeProfiler.endMark(JavaParserConstants.IMAGESTORE_DORESIZE);
		}
	}

	/**
	 * Builds the fileurl
	 *
	 * @param queryString
	 * @return
	 */
	private String buildFileUrl(String imageUrl, String queryString){
		if (!ObjectTransformer.isEmpty(queryString)) {
			imageUrl += "?" + queryString;
		}
		return imageUrl;
	}

	/**
	 * Do validation of the resize URL
	 * @param secret configured secret
	 * @param path validation path
	 * @param validation validation parameter
	 * @return true if the validation passes, false if not
	 * @throws NodeException
	 */
	public static boolean doValidation(String secret, String path, String validation) throws NodeException {
		if (ObjectTransformer.isEmpty(secret)) {
			return true;
		}
		if (ObjectTransformer.isEmpty(validation)) {
			return false;
		}
		try {
			StringBuffer toValidate = new StringBuffer(secret);

			toValidate.append(path);
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			md.update(toValidate.toString().getBytes("UTF-8"));
			byte[] digest = md.digest();

			return ObjectTransformer.encodeBinary(digest).equals(validation.toUpperCase());
		} catch (Exception e) {
			throw new NodeException("Error while doing validation", e);
		}
	}

	/**
	 * Do validation based on the given resize parameters
	 * @param secret secret
	 * @param width width
	 * @param height height
	 * @param mode mode
	 * @param extMode extended mode
	 * @param topleftX crop topleft X
	 * @param topleftY crop topleft Y
	 * @param cropWidth crop width
	 * @param cropHeight crop height
	 * @param validation validation string
	 * @return true if validation succeeds, false if it fails
	 * @throws NodeException
	 */
	public static boolean doValidation(String secret, int width, int height, String mode,
			String extMode, String topleftX, String topleftY, String cropWidth,
			String cropHeight, String validation) throws NodeException {
		if (ObjectTransformer.isEmpty(secret)) {
			return true;
		}
		if (ObjectTransformer.isEmpty(validation)) {
			return false;
		}

		StringBuffer path = new StringBuffer();

		// width
		path.append("/").append(width > 0 ? Integer.toString(width) : "auto");
		// height
		path.append("/").append(height > 0 ? Integer.toString(height) : "auto");
		// mode
		path.append("/").append(mode);
		if (CROP_AND_RESIZE_MODE_KEY.equals(mode)) {
			// ext mode
			path.append("/").append(extMode);
			path.append("/").append(topleftX);
			path.append("/").append(topleftY);
			path.append("/").append(cropWidth);
			path.append("/").append(cropHeight);
		}
		return doValidation(secret, path.toString(), validation);
	}

	/**
	 * Write the image to the given filepath, create all necessary directories
	 * @param image image to write
	 * @param filePath filepath
	 * @throws IOException
	 */
	protected void writeImage(FileInformation image, String filePath) throws IOException {
		File resizedFile = new File(filePath);

		resizedFile.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(resizedFile);
		InputStream in = image.getInputStream();

		if (in == null) {
			logger.warn("Unable to retrieve input stream - assuming file was empty.");
			// FileInformation will return null if a file hold in memory has 0 bytes.
			in = new ByteArrayInputStream(new byte[0]);
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = -1;

		while ((read = in.read(buffer)) > 0) {
			out.write(buffer, 0, read);
		}

		out.close();
		in.close();
	}
}
