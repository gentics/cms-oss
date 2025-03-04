/*
 * @author norbert
 * @date 29.05.2009
 * @version $Id: AccessControl.java,v 1.2 2009-12-16 16:12:18 herbert Exp $
 */
package com.gentics.api.portalnode.security;

import java.security.PrivilegedAction;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface for implementation of access control for all parts of the Portal
 * (including the initialization). When configured in the servlet init parameter
 * "accesscontrol.class", an instance of this class is instantiated and used for
 * every access of the PortalServlet's methods {@link HttpServlet#init()},
 * {@link HttpServlet#service(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse)}
 * and {@link HttpServlet#destroy()}. When the secured servlet is destroyed,
 * the access control instance is destroyed itself, by calling the
 * {@link #destroy()} method.
 */
public interface AccessControl {

	/**
	 * Initialize the access control instance. This is done first when the
	 * PortalServlet is initialized. When this method throws an exception, the
	 * servlet initialization itself will fail with this exception.
	 * @param config servlet configuration
	 * @throws ServletException in case of errors
	 */
	public abstract void init(ServletConfig config) throws ServletException;

	/**
	 * Run the given action with the necessary privileges. This will include
	 * everything done by the portal during
	 * <ul>
	 * <li>Initialization in {@link HttpServlet#init()} (after this instance
	 * was initialized)</li>
	 * <li>Request handling in
	 * {@link HttpServlet#service(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse)}</li>
	 * <li>Shutting down in (@link {@link HttpServlet#destroy()} (before this
	 * instance is destroyed)</li>
	 * </ul>
	 * @param action privileged action
	 * @param request servlet request, when a request is handled, null for the
	 *        initialization
	 * @param response servlet response, when a request is handled, null for the
	 *        initialization
	 * @return object that was returned by the privileged action
	 * @throws ServletException in case of errors
	 */
	public abstract Object runPrivileged(PrivilegedAction action, HttpServletRequest request,
			HttpServletResponse response) throws ServletException;

	/**
	 * Destroy the access control instance, free all resources.
	 * This method is called from the method {@link HttpServlet#destroy()}.
	 */
	public abstract void destroy();
}
