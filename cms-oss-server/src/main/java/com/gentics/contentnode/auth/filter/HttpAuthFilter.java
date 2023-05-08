package com.gentics.contentnode.auth.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;

/**
 * Authentication Filter for HTTP Authentication
 */
public class HttpAuthFilter extends AbstractSSOFilter {

	/**
	 * Names of the attributes fetched from the headers
	 */
	protected final static String[] ATTR_NAMES = { "firstname", "lastname", "email", "group"};

	@Override
	protected String getDefaultInitGroupExpression() {
		return "attr.group";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
		NodePreferences pref = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();

		if (pref.isFeature(Feature.HTTP_AUTH_LOGIN)) {
			Map httpAuthLogin = pref.getPropertyMap("http_auth_login");
			Map<String, Object> attributes = new HashMap<String, Object>();

			String login = getAuthParam(httpAuthLogin, httpServletRequest, "login");

			for (String attrName : ATTR_NAMES) {
				attributes.put(attrName, getAuthParam(httpAuthLogin, httpServletRequest, attrName));
			}

			// special treatment for group
			String group = ObjectTransformer.getString(attributes.get("group"), null);

			if (!StringUtils.isEmpty(group)) {
				String[] groups = StringUtils.splitString(group, ObjectTransformer.getString(httpAuthLogin.get("splitter"), ";"));

				attributes.put("group", groups);
			}

			if (!StringUtils.isEmpty(login)) {
				request = doSSOLogin(httpServletRequest, login, attributes);
			}
		}

		chain.doFilter(request, response);
	}

	/**
	 * Get the a header, as defined in the map
	 * @param httpAuthLogin map defining the header names
	 * @param request request
	 * @param name name of header value
	 * @return header value
	 */
	protected String getAuthParam(Map httpAuthLogin, HttpServletRequest request, String name) {
		String headerName = ObjectTransformer.getString(httpAuthLogin.get(name), null);

		if (StringUtils.isEmpty(headerName)) {
			return null;
		}

		if (headerName.startsWith("HTTP_")) {
			headerName = headerName.substring(5);
			return request.getHeader(headerName);
		} else {
			return null;
		}
	}
}
