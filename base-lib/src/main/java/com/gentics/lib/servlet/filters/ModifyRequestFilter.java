/*
 * @author norbert
 * @date 06.10.2005
 * @version $Id: ModifyRequestFilter.java,v 1.2 2005/10/31 13:00:27 norbert Exp $
 */
package com.gentics.lib.servlet.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Servlet filter that can add headers to the servlet request
 * @author norbert
 */
public class ModifyRequestFilter implements Filter {

	/**
	 * headers to be added to the request
	 */
	protected Properties addHeaders = null;

	protected File headersFile = null;

	protected long lastModificationTime = 0;

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
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
			FileInputStream in = null;

			try {
				in = new FileInputStream(headersFile);
				addHeaders = new Properties();
				addHeaders.load(in);
				lastModificationTime = headersFile.lastModified();
			} finally {
				if (in != null) {
					in.close();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
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
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {}

	protected class FilteredServletRequest extends HttpServletRequestWrapper {

		/**
		 * additional headers
		 */
		protected Properties additionalHeaders;

		/**
		 * Create a filtered servlet request with added headers
		 * @param request wrapped request
		 * @param headers additional headers
		 */
		public FilteredServletRequest(HttpServletRequest request, Properties headers) {
			super(request);
			additionalHeaders = headers;
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
		 */
		public String getHeader(String name) {
			if (additionalHeaders != null && additionalHeaders.containsKey(name)) {
				return additionalHeaders.getProperty(name);
			} else {
				return super.getHeader(name);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
		 */
		public Enumeration getHeaderNames() {
			if (additionalHeaders != null) {
				return new CombinedEnumeration(super.getHeaderNames(), additionalHeaders);
			} else {
				return super.getHeaderNames();
			}
		}

		public Enumeration getHeaders(String name) {
			if (additionalHeaders != null && additionalHeaders.containsKey(name)) {
				return new CollectionEnumeration(Collections.singletonList(name));
			} else {
				return super.getHeaders(name);
			}
		}

	}

	protected class CombinedEnumeration implements Enumeration {
		protected Enumeration enumeration;

		protected Iterator iterator;

		public CombinedEnumeration(Enumeration enumeration, Properties properties) {
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
		public Object nextElement() {
			return enumeration.hasMoreElements() ? enumeration.nextElement() : (iterator != null ? iterator.next() : null);
		}
	}

	protected class CollectionEnumeration implements Enumeration {
		protected Collection coll = null;

		protected Iterator it = null;

		public CollectionEnumeration(Collection coll) {
			this.coll = coll;
			it = coll.iterator();
		}

		public boolean hasMoreElements() {
			return it.hasNext();
		}

		public Object nextElement() {
			return it.next();
		}
	}
}
