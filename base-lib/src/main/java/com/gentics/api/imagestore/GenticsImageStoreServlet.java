/*
 * @author norbert
 * @date 08.11.2006
 * @version $Id: GenticsImageStoreServlet.java,v 1.2 2010-11-09 09:58:59 clemens Exp $
 */
package com.gentics.api.imagestore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FilenameUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.lib.image.GenticsImageStore;
import com.gentics.lib.image.GenticsImageStoreException;
import com.gentics.lib.log.NodeLogger;

/**
 * Servlet for resizing images for Content.Node
 */
public class GenticsImageStoreServlet extends HttpServlet {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 6346266651105426873L;

	/**
	 * buffer size for writing the image to the response
	 */
	public final static int BUFFER_SIZE = 4096;

	/**
	 * Portal basepath
	 */
	private String portalBasePath;

	/**
	 * Regex that will be used to determine the portal basepath
	 */
	private String portalBasePathRegEx;

	/**
	 * logger
	 */
	protected NodeLogger logger;

	/**
	 * Timeout in ms for reading the original image from the URL
	 */
	protected int loadTimeout = 60 * 1000;

	/**
	 * The GenticsImageStore by default fetches the subject image from the host of the request (usually
	 * identical to the host the GenticsImageStore runs on). However, in some network constellations
	 * the host name used to make the request to the GenticsImageStore is actually the host name of some
	 * front end web server for example. In such constellations, the host the request is made to may not
	 * be reachable from the host the GenticsImageStore runs on, and in such cases it is useful to be
	 * able to specify a custom urlPrefix which should include the scheme, host and optionally the port
	 * of the host where images should be fetched from. For example http://localhost:8080
	 *
	 * May be null.
	 */
	private volatile URI urlPrefix = null;

	/**
	 * Will be instantiated with a class given by an optional servlet parameter which enables the user
	 * to customize exactly how requests to images are mapped to the actual location where the image
	 * will be fetched from.
	 *
	 * May be null.
	 */
	private volatile ImageUriMapper uriMapper = null;

	/**
	 * Will be instantiated with a class given by an optional servlet parameter which enables the user
	 * to modify header information and data in requests to the GenticsImageStore
	 *
	 * May be null.
	 */
	private volatile RequestDecorator requestDecoratorClass = null;

	/**
	 * Configured secret for resize validation
	 */
	private volatile String secret = null;

	private static final Pattern PARAM_REGEX = Pattern.compile("(.*?)=(.*)");

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.GenericServlet#init()
	 */
	public void init() throws ServletException {
		logger = NodeLogger.getNodeLogger(getClass());
		portalBasePath = ObjectTransformer.getString(getServletConfig().getInitParameter("portalBasePath"), null);
		portalBasePathRegEx = ObjectTransformer.getString(getServletConfig().getInitParameter("portalBasePathRegEx"), null);
		secret = ObjectTransformer.getString(getServletConfig().getInitParameter(GenticsImageStore.SECRET_PARAMETER), null);
		loadTimeout = ObjectTransformer.getInt(getServletConfig().getInitParameter("loadTimeout"), loadTimeout);

		String urlPrefix = getServletConfig().getInitParameter("urlPrefix");

		if (null != urlPrefix) {
			try {
				this.urlPrefix = new URI(urlPrefix);
			} catch (URISyntaxException e) {
				throw new ServletException(e);
			}
		}

		String uriMapperClass = getServletConfig().getInitParameter("uriMapper");

		if (null != uriMapperClass) {
			try {
				this.uriMapper = (ImageUriMapper) Class.forName(uriMapperClass).newInstance();
			} catch (InstantiationException e) {
				throw new ServletException(e);
			} catch (IllegalAccessException e) {
				throw new ServletException(e);
			} catch (ClassNotFoundException e) {
				throw new ServletException(e);
			}
		}

		String requestDecoratorClass = getServletConfig().getInitParameter("requestDecorator");

		if (null != requestDecoratorClass) {

			try {
				this.requestDecoratorClass = (RequestDecorator) Class.forName(requestDecoratorClass).newInstance();
			} catch (InstantiationException e) {
				throw new ServletException(e);
			} catch (IllegalAccessException e) {
				throw new ServletException(e);
			} catch (ClassNotFoundException e) {
				throw new ServletException(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
	 *      jakarta.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// make sure doGet and doPost do the same
		doGet(request, response);
	}

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
	 *      jakarta.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		String imageURL = null;

		// Use regex matching if possible
		if (portalBasePathRegEx != null) {
			Pattern portalBasePathPattern = Pattern.compile(portalBasePathRegEx);
			Matcher basePathUrlMatcher = portalBasePathPattern.matcher(pathInfo);

			if (basePathUrlMatcher.find()) {
				imageURL = basePathUrlMatcher.group();
				pathInfo = pathInfo.substring(0, basePathUrlMatcher.start());
			}

		} else {

			// Fall back to default portal base path
			if (portalBasePath == null) {
				portalBasePath = "/Portal.Node";
			}

			// Extract the part starting with the portal base path
			int portalBasePathIndex = pathInfo.indexOf(portalBasePath);

			if (portalBasePathIndex >= 0) {
				imageURL = pathInfo.substring(portalBasePathIndex);
				pathInfo = pathInfo.substring(0, portalBasePathIndex);
			}

		}

		if (imageURL == null) {
			logger.error("Imageurl could not be determined. Perhaps your configured base path {" + portalBasePath + "} is not matching the given imageurl {" + pathInfo + "}");
			response.sendError(HttpStatus.SC_BAD_REQUEST, "");
			return;
		}

		// paths have to match the pattern
		Matcher m = GenticsImageStore.PATH_INFO_PATTERN.matcher(pathInfo);

		if (m.matches()) {
			try {
				String mode = m.group(GenticsImageStore.PATH_INFO_PATTERN_MODE);

				if (GenticsImageStore.CROP_AND_RESIZE_MODE_KEY.equals(mode)) {
					// EXTRACT FURTHER VARIABLES
					Matcher extMatcher = GenticsImageStore.PATH_INFO_EXTENDED_PATTERN.matcher(pathInfo);

					if (extMatcher.matches()) {
						// do validation
						if (!ObjectTransformer.isEmpty(secret)
								&& !GenticsImageStore.doValidation(secret, extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_FULL),
								request.getParameter(GenticsImageStore.VALIDATION_PARAMETER))) {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							return;
						}

						doResizing(extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_EXTMODE),
								extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_WIDTH), extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_HEIGHT), true,
								extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_TOPLEFT_X), extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_TOPLEFT_Y),
								extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_CROPWIDTH), extMatcher.group(GenticsImageStore.PATH_INFO_PATTERN_CROPHEIGHT),
								request, response, imageURL);
					} else {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized path " + pathInfo);
					}
				} else {
					// do validation
					if (!ObjectTransformer.isEmpty(secret)
							&& !GenticsImageStore.doValidation(secret, m.group(GenticsImageStore.PATH_INFO_PATTERN_FULL),
							request.getParameter(GenticsImageStore.VALIDATION_PARAMETER))) {
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						return;
					}

					doResizing(m.group(GenticsImageStore.PATH_INFO_PATTERN_MODE), m.group(GenticsImageStore.PATH_INFO_PATTERN_WIDTH),
							m.group(GenticsImageStore.PATH_INFO_PATTERN_HEIGHT), false, null, null, null, null, request, response, imageURL);
				}
			} catch (IllegalArgumentException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized path " + pathInfo);
			} catch (GenticsImageStoreException e) {
				response.sendError(e.getStatusCode(), e.getMessage());
			} catch (NodeException e) {
				logger.error("Error while doing validation", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while doing validation");
			}
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized path " + pathInfo);
		}

	}

	/**
	 * Adds a parameter value pair to a given url
	 *
	 * @param url	input url
	 * @param name	name of the parameter to add
	 * @param value	value of the parameter to add
	 * @return	input url with added url parameter
	 */
	public String addParameter(String url, String name, String value) {
		// Don't add a pair if the name is null or empty
		if (name == null || name.length() == 0) {
			return url;
		}
		// Don't add a pair if the value is null or empty
		if (value == null || value.length() == 0) {
			return url;
		}

		int qpos = url.indexOf('?');
		int hpos = url.indexOf('#');
		char sep = qpos == -1 ? '?' : '&';
		String seg = sep + name + '=' + value;
		return hpos == -1 ? url + seg : url.substring(0, hpos) + seg
				+ url.substring(hpos);
	}

	/**
	 * Gets the absolute URI for a relative image path.
	 *
	 * If the urlPrefix servlet parameter is provided, it will be used to make the given imagePath absolute.
	 * If urlPrefix is not provided, the scheme, host and port of the given request will be used to make the given imagePath absolute.
	 * Additionally, if the urlMapper servlet parameter is provided, it will be given the now absolute imagePath
	 * and whatever it returns replaces the absoulte imagePath.
	 *
	 * @param request
	 * 		  The request to this servlet which fetches the image from the path specified with imagePath.
	 * @param imagePath
	 * 		  The relative path to an image.
	 * @return
	 * 		  The absolute URI for this image.
	 * @throws ServletException
	 * 		  If the given imagePath can not be made absolute because it is of an incorrect format.
	 */
	private URI toImageUri(HttpServletRequest request, String imagePath) throws ServletException {

		URL url;

		try {
			if (null != urlPrefix && null != urlPrefix.getScheme() && null != urlPrefix.getHost()) {
				if (-1 != urlPrefix.getPort()) {
					url = new URL(urlPrefix.getScheme(), urlPrefix.getHost(), urlPrefix.getPort(), imagePath);
				} else {
					url = new URL(urlPrefix.getScheme(), urlPrefix.getHost(), imagePath);
				}
			} else {
				url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), imagePath);
			}
		} catch (MalformedURLException e) {
			throw new ServletException(e);
		}

		URI imageUri;

		try {
			String fileId = url.toExternalForm();

			String cacheKeyQueryParameters = getServletConfig().getInitParameter("cacheKeyQueryParameters");

			if (cacheKeyQueryParameters != null	&& request.getQueryString() != null) {
				// Add additional get parameters to the fileId which will be used as cacheKey
				String[] parameterNames = cacheKeyQueryParameters.split(",");
				MultiMap halfParsedParams = halfParseQueryString(request.getQueryString());
				for (String parameterName : parameterNames) {
					@SuppressWarnings("unchecked")
					List<String> parameterValues = (List<String>) halfParsedParams
							.get(parameterName);
					if (parameterValues != null) {
						for (String value : parameterValues) {
							fileId = addParameter(fileId, parameterName, value);
						}
					}
				}
			}
			imageUri = new URI(fileId);
		} catch (URISyntaxException e) {
			throw new ServletException(e);
		}

		if (null != uriMapper) {
			imageUri = uriMapper.mapImageUri(request, imageUri);
		}

		return imageUri;
	}

	/**
	 * parse query string to extract parameters, but do not urldecode to avoid encoding issues
	 * @param queryString querystring of the URL
	 * @return MultiMap of query parameters & values, not urldecoded
	 */
	private MultiMap halfParseQueryString(String queryString) {
		@SuppressWarnings("rawtypes")
		MultiMap result = MultiValueMap.decorate(new TreeMap());
		String [] params = queryString.split("&",0);
		for (String p : params) {
			Matcher m = PARAM_REGEX.matcher(p);
			if (m.matches()) {
				result.put(m.group(1), m.group(2));
			}
		}
		return result;
	}

	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void doResizing(String mode, String width, String height, boolean cropandresize,
			String topleftx, String toplefty, String cropwidth, String cropheight,
			HttpServletRequest request, HttpServletResponse response, String imagePath) throws ServletException,
				IOException, NodeException {

		// get imageUri
		URI imageUri = toImageUri(request, imagePath);
		// get original filename - will be empty if there is no filename in imagePath
		String originalFileName = FilenameUtils.getName(imageUri.getPath());

		// create Map of headers
		Map<String, String> headers = new HashMap<String, String>(1);
		Enumeration<?> headerNames = request.getHeaderNames();

		if (!ObjectTransformer.isEmpty(headerNames)) {
			while (headerNames.hasMoreElements()) {
				String name = (String) headerNames.nextElement();
				String value = request.getHeader(name);

				headers.put(name, value);
			}
		}

		// create GenticsImageStoreRequest bean
		GenticsImageStoreRequest genticsImageStoreRequest = new GenticsImageStoreRequest();

		genticsImageStoreRequest.setCookies(request.getCookies());
		genticsImageStoreRequest.setHeaders(headers);
		genticsImageStoreRequest.setImageUri(imageUri.toString());
		genticsImageStoreRequest.setQueryString(request.getQueryString());

		// intercept GenticsImageStoreRequest with the configured request decorator, if enabled
		if (null != requestDecoratorClass) {
			requestDecoratorClass.decorateRequest(genticsImageStoreRequest, request);
		}

		GenticsImageStore imageStore = new GenticsImageStore();
		imageStore.setLoadTimeout(loadTimeout);
		Double jpegQuality = ObjectTransformer.getDouble(getServletConfig().getInitParameter("jpegquality"), null);
		if (jpegQuality != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Setting jpegQuality to " + jpegQuality);
			}

			imageStore.setJPEGQuality(jpegQuality);
		}

		Double webpQuality = ObjectTransformer.getDouble(getServletConfig().getInitParameter("webpquality"), null);

		if (webpQuality != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Setting webpQuality to " + webpQuality);
			}

			imageStore.setWebpQuality(webpQuality);
		}

		Boolean webpLossless = ObjectTransformer.getBoolean(getServletConfig().getInitParameter("webplossless"), null);

		if (webpLossless != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Setting webpLossless to " + webpLossless);
			}

			imageStore.setWebpLossless(webpLossless);
		}

		if (logger.isDebugEnabled()) {
			StringBuffer debugImageUri = new StringBuffer(genticsImageStoreRequest.getImageUri());

			if (genticsImageStoreRequest.getQueryString() != null) {
				debugImageUri.append(genticsImageStoreRequest.getQueryString());
			}
			logger.debug("URI to original image: " + debugImageUri.toString());
		}

		FileInformation fileInformation = imageStore.doResizing(mode, width,
				height, cropandresize, topleftx, toplefty, cropwidth,
				cropheight, genticsImageStoreRequest.getImageUri(),
				genticsImageStoreRequest.getQueryString(),
				genticsImageStoreRequest.getCookies(),
				genticsImageStoreRequest.getHeaders());
		writeImageToResponse(fileInformation, response, originalFileName);
	}

	/**
	 * Write the image into the servlet response
	 * @param image image data
	 * @param response servlet response
	 * @throws IOException
	 */
	protected void writeImageToResponse(FileInformation image, HttpServletResponse response, String originalFileName) throws IOException {
		response.setContentType(image.getContentType());
		String fileName = image.getFileName();
		if (!ObjectTransformer.isEmpty(originalFileName)) {
			fileName = originalFileName;
		}
		response.setHeader("Content-Disposition", "inline; filename=" + fileName);

		OutputStream out = response.getOutputStream();
		InputStream in = image.getInputStream();

		if (in == null) {
			logger.warn("Unable to retrieve input stream - assuming file was empty.");
			// FileInformation will return null if a file hold in memory has 0
			// bytes.
			in = new ByteArrayInputStream(new byte[0]);
		}
		// prepare buffer and read counter
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = 0;

		// pipe the input stream to the output stream
		while ((read = in.read(buffer)) >= 0) {
			out.write(buffer, 0, read);
		}

		// close the streams
		out.close();
		in.close();
	}
}
