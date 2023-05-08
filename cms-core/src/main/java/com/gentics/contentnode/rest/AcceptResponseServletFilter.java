/*
 * @author johannes2
 * @date Sep 9, 2010
 * @version $Id: AcceptResponseServletFilter.java,v 1.1.2.1 2011-03-23 14:05:08 johannes2 Exp $
 */
package com.gentics.contentnode.rest;

import java.io.IOException;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * ServletFilter to modify the accept headers requests that should be handled by
 * jax-rs implementation. The response will also be overwritten if needed.
 * @author johannes2
 */
public class AcceptResponseServletFilter implements Filter {

	private HashMap<String, String> map = new HashMap<String, String>();

	@SuppressWarnings("unchecked")
	public void init(FilterConfig config) throws ServletException {
		Enumeration<String> names = config.getInitParameterNames();

		while (names.hasMoreElements()) {
			String name = names.nextElement();

			map.put("." + name, config.getInitParameter(name));
		}
	}

	public void destroy() {}

	private static String getExtension(String path, HashMap<String, String> map) {
		int index = path.lastIndexOf('.');

		if (index == -1) {
			return "";
		}
		String extension = path.substring(index);

		// System.out.println("searching for: " + extension);
		if (map.get(extension) == null) {
			return "";
		}
		return extension;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		String contentTypeParameter = request.getParameter("filter-contenttype");

		// Only filter the response if a filter-contenttype parameter was
		// specified
		if (ObjectTransformer.isEmpty(contentTypeParameter)) {
			chain.doFilter(new RequestWrapper((HttpServletRequest) request), response);
		} else {
			chain.doFilter(new RequestWrapper((HttpServletRequest) request), new ResponseWrapper((HttpServletResponse) response, contentTypeParameter));
		}

	}

	/**
	 * Wrapper for the response. The content-type header parameter will be
	 * overwritten by the given contentTypeParameter parameter
	 * @author johannes2
	 */
	private class ResponseWrapper extends HttpServletResponseWrapper {

		private String contentTypeParameter;

		public ResponseWrapper(HttpServletResponse response, String contentTypeParameter) {
			super(response);
			this.contentTypeParameter = contentTypeParameter;
		}

		@Override
		public void addHeader(String name, String value) {

			if ("Content-Type".equals(name) && contentTypeParameter != null) {
				value = contentTypeParameter;
			}

			super.addHeader(name, value);
		}

	}

	/**
	 * Wrapper for the request. This wrapper will change the accept header of
	 * the response according to the ending of the request uri. (*.json ->
	 * application/json..)
	 * @author johannes2
	 */
	private class RequestWrapper extends HttpServletRequestWrapper {

		public RequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			String uri = super.getRequestURI();

			return uri.substring(0, uri.length() - AcceptResponseServletFilter.getExtension(uri, map).length());
		}

		@Override
		@SuppressWarnings("unchecked")
		public Enumeration getHeaders(String name) {
			// Overwrite the accept header get
			if ("accept".equals(name)) {
				String type = map.get(AcceptResponseServletFilter.getExtension(super.getRequestURI(), map));

				if (type != null) {
					Vector<String> values = new Vector<String>();

					values.add(type);
					return values.elements();
				}
			}
			return super.getHeaders(name);
		}
	}

}
