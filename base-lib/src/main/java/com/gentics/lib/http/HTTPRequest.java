/*
 * User: erwin Date: 30.04.2002 Time: 13:58:09
 */
package com.gentics.lib.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import com.gentics.lib.log.NodeLogger;

public class HTTPRequest {
	public static final int HTTP_OK = 200;

	public static final int HTTP_FORBIDDEN = 403;

	public static final int HTTP_NOT_FOUND = 404;

	private String d_errorMessage;

	private String d_contentType;

	private int d_errorCode;

	private Vector d_content = new Vector();

	private String d_fullContent = "";

	public HTTPRequest(String getURL) throws IOException {
		d_errorMessage = "";
		get(getURL);
	}

	private void get(String getURL) throws IOException {
		URL url = new URL(getURL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "*.*");

		// String ipAdr = java.net.InetAddress.getLocalHost().getHostAddress();

		// con.setRequestProperty("USER-AGENT", "Mozilla/4.0 (compatible; MSIE
		// 6.0b; Windows NT 4.0)" );

		// connect
		con.connect();

		// OK?
		d_errorCode = con.getResponseCode();
		if (d_errorCode == HTTP_OK) {
			d_contentType = con.getContentType();

			BufferedInputStream bin = new BufferedInputStream(con.getInputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(bin));
			String line;

			d_content = new Vector();
			d_fullContent = "";
			while ((line = in.readLine()) != null) {
				d_content.addElement(line);
				d_fullContent += line;
			}
		} else {
			d_errorMessage = "HTTP REQUEST returned: " + con.getResponseCode() + " " + con.getResponseMessage();
			d_errorMessage += "; url was: " + getURL;
			NodeLogger.getLogger(getClass()).error(d_errorMessage);
		}
	}

	public boolean isOK() {
		return d_errorCode == HTTP_OK;
	}

	public String getErrorMsg() {
		return d_errorMessage;
	}

	public String getContentType() {
		return d_contentType;
	}

	/**
	 * @return returns a Vector of Strings
	 */
	public Vector getContent() {
		return d_content;
	}

	public String getFullContent() {
		return d_fullContent;
	}
}
