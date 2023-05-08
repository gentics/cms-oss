package com.gentics.api.tests.genticsimagestore.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;

import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test servlet, which will either try to serve one of the test images or will wait for the specified time
 */
public class ImageProviderServlet extends HttpServlet {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1139371650699275663L;

	private static final Pattern WAIT_PATTERN = Pattern.compile("^/wait/(?<timeout>[0-9]+)$");

	private static final Pattern FILE_PATTERN = Pattern.compile("^/(?<filename>.+)$");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Matcher m = WAIT_PATTERN.matcher(req.getPathInfo());
		if (m.matches()) {
			try {
				int timeout = Integer.parseInt(m.group("timeout"));
				Thread.sleep(timeout);
				return;
			} catch (InterruptedException e) {
				throw new ServletException(e);
			}
		}

		m = FILE_PATTERN.matcher(req.getPathInfo());
		if (m.matches()) {
			String fileName = m.group("filename");
			try (InputStream in = GenericTestUtils.getPictureResource(fileName)) {
				FileUtil.inputStreamToOutputStream(in, resp.getOutputStream());
			} catch (IOException e) {
				throw e;
			} catch (AssertionError e) {
				resp.sendError(HttpStatus.SC_NOT_FOUND);
			}
			return;
		}
	}
}
