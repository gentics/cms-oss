package com.gentics.contentnode.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.lib.log.NodeLogger;

/**
 * A basic abstract Servlet that should be extended by every Servlet that is directly called by a Gentics Content.Node user.
 * This Servlet provides basic methods to get the Content.Node context and to inform the user about critical errors.
 */
public abstract class ContentNodeUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ContentNodeUserServlet.class);

	/**
	 * Initializes the ContentNodeFactory and a transaction for the provided session id.
	 * If the provided session id was invalid the request will halt with an error message.<br /><br />
	 * 
	 * After initializing everything the method {@link #doGet(HttpServletRequest, HttpServletResponse, ContentNodeFactory)} is called.<br /><br />
	 * 
	 * Parameter:
	 * <ul>
	 * <li>sid: Session id that is used to initialize the transaction (mandatory)</li>
	 * <li>language: The language id that is set as current language (optional)</li>
	 * </ul>
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		ContentNodeFactory factory = ContentNodeFactory.getInstance();

		Transaction t = startAndAuthenticateTransaction(factory, request, response);

		if (null == t) {
			return;
		}

		doGet(request, response, factory, t);
        
		// Commit the transaction if it is still open
		if (t.isOpen()) {
			try {
				t.commit(true);
			} catch (TransactionException e) {
				throw new ServletException(e.getMessage(), e.getCause());
			}
		}
	}
    
	/**
	 * Starts a new transaction and ensures that the request is authentic.
	 * 
	 * @param factory the factory to start the transaction in.
	 * 
	 * @param request will be used to read the session ID and session secret
	 *   for authentication.
	 *   
	 * @param response will be used to display a halt() error.
	 * 
	 * @return a started transaction that will be associated with the
	 *   session specified by the session ID in the reques parameter.
	 *   The transaction's session will have a session secret.
	 *   If the transaction could not be started, null will be returned.
	 */
	protected Transaction startAndAuthenticateTransaction(
			ContentNodeFactory factory,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// the session token that identifies a session and authenticates the
		// user for the session.
		SessionToken token;

		try {
			token = new SessionToken(request);
		} catch (InvalidSessionIdException e) {
			String error = "Invalid session ID specified: `" + request.getParameter("sid") + "'";

			halt(error, response);
			logger.error(error);
			return null;
		}
		
		Transaction t = null;

		try {
			// we pass the token string and not the session ID, since
			// we must have the session secret available in the transaction,
			// so that the transaction can be authenticated by
			// {@link ContentNodeResource}. we also authenticate the session
			// below, which gets executed first, and catches all invalid
			// sessions.
			t = factory.startTransaction(token.toString(), true);
		} catch (NodeException e) {
			halt("Invalid sid provided.", response);
			logger.error("Invalid sessionId { " + token + " } provided");
			return null;
		}

		if (!token.authenticates(t.getSession())) {
			if (t != null) {
				try {
					t.rollback();
				} catch (TransactionException e) {
					logger.warn("Error while rolling back transaction with token: \"" + token + "\"");
				}
			}
			halt("The session can't be authenticated", response);
			logger.error("The session can't be authenticated with token: \"" + token + "\"");
			return null;
		}
        
		return t;
	}
    
	/**
	 * Extended doGet method that provides some extra parameters the implementing class can use.
	 * @param request The original HttpServeltRequest
	 * @param response The original HttpServeltResponse
	 * @param factory ContentNodeFactory that can for example be used to initialize new transactions.
	 * @param t A open, ready to use transaction initialized for the provided sessionId. If the transaction is not closed by the user it will be committed implicitly.
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	protected abstract void doGet(HttpServletRequest request, HttpServletResponse response, ContentNodeFactory factory, Transaction t) throws ServletException, IOException;
    
	/**
	 * Informs the user about a critical error that prevents him from continuing his work.
	 * If halt was called, the response stream will be closed and the Servlet should stop immediately processing the request.
	 * 
	 * TODO: Extend this function to provide a more beautiful message for the user and log the errors in the database, so that they can be displayed in the error log in GCN backend.
	 * Logging into the database should be optional. For example errors like permission violations don't need to be logged in the error log but still prevent the user from continue to work.
	 */
	public void halt(String message, HttpServletResponse response) throws IOException {
		PrintWriter writer = response.getWriter();

		writer.write(message);
		writer.close();
	}
}
