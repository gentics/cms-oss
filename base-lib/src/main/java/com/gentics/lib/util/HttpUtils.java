package com.gentics.lib.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper class that can be used to handle http requests, responses.
 * 
 * @author johannes2
 * 
 */
public final class HttpUtils {

	protected static NodeLogger logger = NodeLogger.getNodeLogger(HttpUtils.class);

	private final static Pattern QUOTED_FILENAMEPATTERN = Pattern.compile(".*filename=\"([^\"]+)\".*");
	private final static Pattern UNQUOTED_FILENAMEPATTERN = Pattern.compile(".*filename=([^\\s;]+).*");

	/**
	 * Returns the filename from the content-disposition header field of the given response.
	 * 
	 * @param response
	 * @return Null when the filename information can't be found or the content-disposition field is not set
	 */
	public static String getFilenameFromResponse(HttpResponse response) {

		Header[] headers = response.getHeaders("Content-Disposition");
		if (headers.length != 1) {
			return null;
		} else {
			String contentDisposition = headers[0].getValue();
			contentDisposition = ObjectTransformer.getString(ObjectTransformer.isEmpty(contentDisposition) ? null : contentDisposition, null);
			if (!StringUtils.isEmpty(contentDisposition)) {
				Matcher m = QUOTED_FILENAMEPATTERN.matcher(contentDisposition);

				if (m.matches()) {
					return m.group(1);
				} else {
					m = UNQUOTED_FILENAMEPATTERN.matcher(contentDisposition);
					if (m.matches()) {
						return m.group(1);
					}
				}
			}
		}
		return null;
	}

}
