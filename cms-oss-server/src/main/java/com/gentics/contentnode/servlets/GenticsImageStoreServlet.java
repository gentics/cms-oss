package com.gentics.contentnode.servlets;

import com.gentics.contentnode.security.AccessControlService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gentics.api.imagestore.RequestDecorator;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.image.CNGenticsImageStore;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Servlet for resizing images for Content.Node
 */
public class GenticsImageStoreServlet extends HttpServlet {
	private static final long serialVersionUID = -6959646122427248553L;

	/**
	 * Pattern for URL to binary content
	 */
	public final static String filePattern = "/rest/file/content/load/(?<id>[^/]+)";

	/**
	 * Compiled pattern for Crop and Resize Url of an image in the CMS
	 */
	public final static Pattern CROP_AND_RESIZE_URL = Pattern.compile(
			"/(?<width>[^/]+)/(?<height>[^/]+)/cropandresize/(?<mode>[^/]+)/(?<topleftx>[^/]+)/(?<toplefty>[^/]+)/(?<cropwidth>[^/]+)/(?<cropheight>[^/]+)"
					+ filePattern);

	/**
	 * Compiled pattern for a Resize Url of an image in the CMS
	 */
	public final static Pattern RESIZE_URL = Pattern
			.compile("/(?<width>[^/]+)/(?<height>[^/]+)/(?<mode>[^/]+)" + filePattern);

	/**
	 * buffer size for writing the image to the response
	 */
	public final static int BUFFER_SIZE = 4096;

	/**
	 * Default value for parallel resize operations
	 */
	public final static int DEFAULT_SEMAPHORE_PERMITS = 10;

	/**
	 * logger
	 */
	protected NodeLogger logger;
    
	/**
	 * request decorator class name as displayed in do 24
	 */
	public final static String REQUEST_DECORATOR = "gis_request_decorator";

	/**
	 * Config parameter for parallel GIS requests
	 */
	public final static String SEMAPHORE_PERMITS = "gis_requests";

	/**
	 * Will be instantiated with a class given by an optional servlet parameter which enables the user
	 * to modify header information and data in requests to the GenticsImageStore
	 * 
	 * May be null.
	 */
	private volatile RequestDecorator requestDecoratorClass = null;

	private NodeConfigRuntimeConfiguration runtimeConfiguration;

	private AccessControlService accessControlService;

	/**
	 * Semaphore for locking resize operations
	 */
	private Semaphore semaphore;

	@Override
	public void init() throws ServletException {
		logger = NodeLogger.getNodeLogger(getClass());

		this.runtimeConfiguration = NodeConfigRuntimeConfiguration.getDefault();

		ServletConfig config = getServletConfig();
		Properties configurationProperties = runtimeConfiguration.getConfigurationProperties();

		semaphore = new Semaphore(ObjectTransformer.getInt(runtimeConfiguration.getNodeConfig().getDefaultPreferences().getProperty(SEMAPHORE_PERMITS),
				DEFAULT_SEMAPHORE_PERMITS), true);

		for (Enumeration<?> e = config.getInitParameterNames(); e.hasMoreElements();) {
			String name = e.nextElement().toString();

			configurationProperties.setProperty(name, config.getInitParameter(name));
		}

		String requestDecoratorClass = getServletConfig().getInitParameter(REQUEST_DECORATOR);

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

		// initialize access control ...
		accessControlService = new AccessControlService("genticsimagestore");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// make sure doGet and doPost do the same
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sessionId = request.getParameter(SessionToken.SESSION_ID_QUERY_PARAM_NAME) + getSessionSecretFromCookie(request.getCookies());

		if (ObjectTransformer.isEmpty(sessionId)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
			return;
		}

		try {
			SessionToken sessionToken = new SessionToken(sessionId);
			try (Trx trx = new Trx(sessionId, null); ) {
				Transaction t = trx.getTransaction();
				if (!sessionToken.authenticates(t.getSession())) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
					return;
				}

				String pathInfo = request.getPathInfo();
				if (StringUtils.isEmpty(pathInfo)) {
					// no path info found, so this is supposed to be a command
					doCommand(request, response);
				} else {
					Matcher cropAndResizeMatcher = CROP_AND_RESIZE_URL.matcher(pathInfo);
					Matcher resizeMatcher = RESIZE_URL.matcher(pathInfo);
					if (cropAndResizeMatcher.matches()) {
						String id = cropAndResizeMatcher.group("id");
						int nodeId = ObjectTransformer.getInt(request.getParameter("nodeId"), 0);

						ImageFile image = getImage(id, nodeId);
						doResizing(cropAndResizeMatcher.group("mode"),
								cropAndResizeMatcher.group("width"),
								cropAndResizeMatcher.group("height"),
								true,
								cropAndResizeMatcher.group("topleftx"),
								cropAndResizeMatcher.group("toplefty"),
								cropAndResizeMatcher.group("cropwidth"),
								cropAndResizeMatcher.group("cropheight"),
								image,
								response,
								true);
					} else if (resizeMatcher.matches()) {
						String id = resizeMatcher.group("id");
						int nodeId = ObjectTransformer.getInt(request.getParameter("nodeId"), 0);
						ImageFile image = getImage(id, nodeId);

						doResizing(resizeMatcher.group("mode"),
								resizeMatcher.group("width"),
								resizeMatcher.group("height"),
								false,
								null,
								null,
								null,
								null,
								image,
								response,
								true);
					} else {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized path " + pathInfo);
						return;
					}
				}
			} catch (NodeException e) {
				throw new ServletException("Error while resizing", e);
			}
		} catch (InvalidSessionIdException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
			return;
		}
	}

	/**
	 * Load the image with given ID and nodeId
	 * @param id image ID
	 * @param nodeId node ID
	 * @return image (if found and user has permission)
	 * @throws NodeException
	 */
	protected ImageFile getImage(String id, int nodeId) throws NodeException {
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return MiscUtils.load(ImageFile.class, id);
		}
	}

	/**
	 * Do the resizing of an image and return the image
	 * @param mode resizing mode
	 * @param width new width
	 * @param height new height
	 * @param image image to resize
	 * @param response servlet response
	 * @param viewPerm flag to check, whether the user has view permission on the image
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doResizing(String mode, String width, String height, boolean cropandresize, String topleftx, String toplefty, String cropwidth, String cropheight,
			ImageFile image, HttpServletResponse response, boolean viewPerm) throws ServletException,
				IOException {
		CNGenticsImageStore imageStore = new CNGenticsImageStore(runtimeConfiguration.getNodeConfig());
		imageStore.setSemaphore(semaphore, 1, TimeUnit.MINUTES);
		FileInformation fileInformation;

		try {
			fileInformation = imageStore.doResizing(mode, width, height, cropandresize, topleftx, toplefty, cropwidth,
					cropheight, image);
		} catch (NodeException e) {
			throw new ServletException("Error while trying to resize image.", e);
		}

		writeImageToResponse(fileInformation, response, viewPerm, image.getFilename());
	}

	/**
	 * Write the image into the servlet response
	 * @param image image data
	 * @param response servlet response
	 * @param viewPerm flag to check, whether the user has view permission on the image
	 * @param fileName optional filenam for the response header
	 * @throws IOException
	 */
	protected void writeImageToResponse(FileInformation image, HttpServletResponse response, boolean viewPerm, String fileName) throws IOException {
		response.setContentType(image.getContentType());
		response.setHeader("Content-Disposition", "inline; filename=" + ObjectTransformer.getString(fileName, image.getFileName()));

		// if the user is not allowed to view the image, disable caching in the browser
		if (!viewPerm) {
			response.setHeader("Expires:", "Mon, 01 Jan 1970 00:00:00 GMT");
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");
			response.setHeader("Pragma", "no-cache");
		}

		OutputStream out = response.getOutputStream();
		InputStream in = image.getInputStream();

		if (in == null) {
			logger.warn("Unable to retrieve input stream - assuming file was empty.");
			// FileInformation will return null if a file hold in memory has 0 bytes.
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

	/**
	 * Handle a command
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doCommand(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!accessControlService.verifyAccess(request, response)) {
			return;
		}
		String command = ObjectTransformer.getString(request.getParameter("cmd"), "");

		response.setContentType("text/plain");
		switch (command) {
		case "status":
			response.getWriter().print("OK");
			break;
		case "clearcache":
			CNGenticsImageStore.clearCache();
			response.getWriter().print("OK");
			break;
		case "cachestats":
			response.getWriter().print(CNGenticsImageStore.getCacheStats());
			break;
		default:
			throw new ServletException("Unknown command: " + command);
		}
	}

	/**
	 * Get the session secret cookie from the cookies
	 * @param cookies cookies
	 * @return value of the session secret cookie
	 */
	protected String getSessionSecretFromCookie(Cookie[] cookies) {
		// cookies may be null
		if (null == cookies) {
			return "";
		}

		for (Cookie cookie : cookies) {
			if (SessionToken.SESSION_SECRET_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return "";
	}
}
