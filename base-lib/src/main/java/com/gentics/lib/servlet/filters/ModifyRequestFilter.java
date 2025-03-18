package com.gentics.lib.servlet.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Servlet filter that can add headers to the servlet request
 * @author norbert
 */
public class ModifyRequestFilter implements Filter {

	/**
	 * headers to be added to the request
	 */
	protected Map<String, String> addHeaders = null;

	protected File headersFile = null;

	protected long lastModificationTime = 0;

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
	 */
	public void init(FilterConfig config) throws ServletException {
		String headersFileName = config.getInitParameter("headers");

		try {
			if (headersFileName != null && headersFileName.length() > 0) {
				headersFile = new File(config.getServletContext().getRealPath(headersFileName));
				readHeadersFromFile();
			}
		} catch (Exception e) {
			throw new ServletException("Error while reading from file " + headersFile, e);
		}
	}

	/**
	 * Check whether the file was modified since last read
	 * @return true if the file was modified, false if not
	 */
	protected boolean fileModified() {
		return headersFile != null && headersFile.lastModified() != lastModificationTime;
	}

	/**
	 * Read the headers from the file
	 * @throws IOException
	 */
	protected void readHeadersFromFile() throws IOException {
		if (headersFile != null && headersFile.canRead()) {
			try (FileInputStream in = new FileInputStream(headersFile)) {
				Properties prop = new Properties();
				prop.load(in);
				addHeaders = new HashMap<>();
				for (String name : prop.stringPropertyNames()) {
					addHeaders.put(name, prop.getProperty(name));
				}
				lastModificationTime = headersFile.lastModified();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest,
	 *      jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (fileModified()) {
			readHeadersFromFile();
		}
		// just add all the headers to the request
		if (request instanceof HttpServletRequest) {
			HttpServletRequest hRequest = (HttpServletRequest) request;
			HttpServletRequest wrapper = new FilteredServletRequest(hRequest, addHeaders);

			chain.doFilter(wrapper, response);
		} else {
			chain.doFilter(request, response);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jakarta.servlet.Filter#destroy()
	 */
	public void destroy() {}

	protected class FilteredServletRequest extends HttpServletRequestWrapper {

		/**
		 * additional headers
		 */
		protected Map<String, String> additionalHeaders;

		/**
		 * Create a filtered servlet request with added headers
		 * @param request wrapped request
		 * @param headers additional headers
		 */
		public FilteredServletRequest(HttpServletRequest request, Map<String, String> headers) {
			super(request);
			additionalHeaders = headers;
		}

		/*
		 * (non-Javadoc)
		 * @see jakarta.servlet.http.HttpServletRequest#getHeader(java.lang.String)
		 */
		public String getHeader(String name) {
			if (additionalHeaders != null && additionalHeaders.containsKey(name)) {
				return additionalHeaders.get(name);
			} else {
				return super.getHeader(name);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see jakarta.servlet.http.HttpServletRequest#getHeaderNames()
		 */
		public Enumeration<String> getHeaderNames() {
			if (additionalHeaders != null) {
				return new CombinedEnumeration(super.getHeaderNames(), additionalHeaders);
			} else {
				return super.getHeaderNames();
			}
		}

		public Enumeration<String> getHeaders(String name) {
			if (additionalHeaders != null && additionalHeaders.containsKey(name)) {
				return new CollectionEnumeration(Collections.singletonList(name));
			} else {
				return super.getHeaders(name);
			}
		}

	}

	protected class CombinedEnumeration implements Enumeration<String> {
		protected Enumeration<String> enumeration;

		protected Iterator<String> iterator;

		public CombinedEnumeration(Enumeration<String> enumeration, Map<String, String> properties) {
			this.enumeration = enumeration;
			if (properties != null) {
				iterator = properties.keySet().iterator();
			} else {
				iterator = null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements() {
			return enumeration.hasMoreElements() || (iterator != null && iterator.hasNext());
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Enumeration#nextElement()
		 */
		public String nextElement() {
			return enumeration.hasMoreElements() ? enumeration.nextElement() : (iterator != null ? iterator.next() : null);
		}
	}

	protected class CollectionEnumeration implements Enumeration<String> {
		protected Collection<String> coll = null;

		protected Iterator<String> it = null;

		public CollectionEnumeration(Collection<String> coll) {
			this.coll = coll;
			it = coll.iterator();
		}

		public boolean hasMoreElements() {
			return it.hasNext();
		}

		public String nextElement() {
			return it.next();
		}
	}
}
