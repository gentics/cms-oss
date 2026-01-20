package com.gentics.contentnode.rest.resource.impl;

import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionLockManager;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract class for REST resources that provides basic 
 * functionality for interacting with GCN.
 * 
 * @author norbert
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public abstract class AbstractContentNodeResource {

	public static final String ACNR_SELF = "AbstractContentNodeResource";

	/**
	 * Instance of a ContentNodeFactory
	 */
	private ContentNodeFactory factory;

	/**
	 * Transaction that is initialized for the given session id in the request parameter
	 */
	protected Transaction transaction;

	/**
	 * Flag to mark whether the transaction was generated or set
	 */
	protected boolean createdTransaction = false;

	/**
	 * Logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Session secret stored as a cookie.
	 */
	private String sessionSecret;

	/**
	 * Request object
	 */
	@Context
	private HttpServletRequest request;

	/**
	 * Response object
	 */
	@Context
	private HttpServletResponse response;

	/**
	 * Jersey HTTP Context
	 */
	@Context
	private ContainerRequestContext context;

	/**
	 * Create an instance of the resource. If a current transaction is found, set it
	 */
	public AbstractContentNodeResource() {
		if ("true".equals(System.getProperty("com.gentics.contentnode.testmode"))) {
		transaction = TransactionManager.getCurrentTransactionOrNull();
		} else if (TransactionManager.getCurrentTransactionOrNull() != null) {
			logger.warn("Found unexpected current transaction in thread");
		}
	}

	/**
	 * Create an instance of the resource which will use the given transaction
	 * @param transaction transaction to use
	 */
	public AbstractContentNodeResource(Transaction transaction) {
		this.transaction = transaction;
	}

	/**
	 * Retrieves the session secret from the session secret cookie, if
	 * available.
	 */
	// can't use @ParamCookie since SESSION_SECRET_COOKIE_NAME isn't constant.
	@Context
	public void setSessionSecretFromCookie(HttpHeaders headers) {
		Map<String, Cookie> cookies = headers.getCookies();

		// cookies may be null
		if (null == cookies) {
			return;
		}
		Cookie sessionSecretCookie = cookies.get(SessionToken.SESSION_SECRET_COOKIE_NAME);

		if (null == sessionSecretCookie) {
			return;
		}
		this.sessionSecret = sessionSecretCookie.getValue();
	}

	public void setSessionSecret(String sessionSecret) {
		this.sessionSecret = sessionSecret;
	}

	public String getSessionSecret() {
		return sessionSecret;
	}

	/**
	 * Initializes the ContentNodeResource with all GCN specific objects
	 */
	@PostConstruct
	public void initialize() {
		// this is a hack to avoid unnecessary warnings logged, when any of the PostConstruct Methods throw an exception
		Logger jerseyErrorLogger = LogManager.getLogManager().getLogger("org.glassfish.jersey.internal.Errors");
		if (jerseyErrorLogger != null) {
			jerseyErrorLogger.setLevel(java.util.logging.Level.SEVERE);
		}

		factory = ContentNodeFactory.getInstance();

		if (context != null) {
			context.setProperty(ACNR_SELF, this);
			}
		try {
			createTransaction();
		} catch (InvalidSessionIdException e) {
			GenericResponse response = new GenericResponse();

			Transaction t = TransactionManager.getCurrentTransactionOrNull();

			if (t != null) {
				try {
					t.rollback();
				} catch (TransactionException ignored) {
				}
			}

			response.setResponseInfo(new ResponseInfo(ResponseCode.AUTHREQUIRED, "Invalid SID"));
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(response).build());
		} catch (NodeException e) {
			failWithGeneralError(e);
		}
	}

	/**
	 * Commits the transaction held by this ContentNodeResource.
	 * 
	 * @throws WebApplicationException if an error occurs during the commit
	 */
	public void commitTransaction() {
		try {
			if (createdTransaction && transaction.isOpen()) {
				transaction.commit();
			}
		} catch (TransactionException e) {
			failWithGeneralError(e);
		}
	}

	/**
	 * Create a new transaction, if none set
	 * @throws NodeException
	 */
	public void createTransaction() throws NodeException {
		if (transaction == null) {
			transaction = getFactory().startTransaction(true);
			createdTransaction = true;
		}
	}

	/**
	 * Roll back the transaction, but don't close it
	 */
	public void rollbackTransaction() {
		if (transaction != null) {
			try {
				transaction.rollback(false);
			} catch (TransactionException e) {
			}
		}
	}

	/**
	 * Get the ContentNodeFactory
	 * @return The ContentNodeFactory
	 */
	public ContentNodeFactory getFactory() {
		return factory;
	}

	/**
	 * Set a new transaction
	 * @param t transaction
	 */
	public void setTransaction(Transaction t) {
		transaction = t;
		createdTransaction = false;
		if (factory == null) {
			factory = ContentNodeFactory.getInstance();
		}
	}

	/**
	 * Get the transaction that is initialized for the given session id in the request parameter.
	 * @return Initialized transaction
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * Let the request fail with a general error
	 * @param e exception
	 * @throws WebApplicationException
	 */
	protected void failWithGeneralError(Exception e) throws WebApplicationException {
		logger.error("An error occurred while processing the request", e);
		I18nString m = new CNI18nString("rest.general.error");

		throw new WebApplicationException(
				Response.status(Status.OK).entity(new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()), new ResponseInfo(ResponseCode.FAILURE, "An error occurred while processing the request: " + e.getLocalizedMessage()))).build());
	}

	/**
	 * Get the servlet request
	 * @return servlet request
	 */
	public HttpServletRequest getRequest() {
		return request;
	}

	/**
	 * Get the servlet response
	 * @return servlet response
	 */
	public HttpServletResponse getResponse() {
		return response;
	}

	/**
	 * Get the job foreground time (in s). If requestedTime is not null, it will be returned as int,
	 * otherwise the configured time is returned (defaults to 5)
	 * @param requestedTime optional requested time
	 * @return foreground time
	 * @throws NodeException
	 */
	public static int getJobForegroundTime(Integer requestedTime) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		return ObjectTransformer.getInt(requestedTime,
				ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("backgroundjob_foreground_time"), 5));
	}

	/**
	 * Execute the given method with the given lockmanager under the given lock.
	 * This method will temporarily set the {@link #transaction} to the transaction created by the lock manager and will make sure, that NodeExceptions
	 * thrown inside the method will correctly be propagated to the caller
	 * @param lockManager lock manager
	 * @param lockKey lock key
	 * @param method method to execute
	 * @return return value
	 * @throws NodeException
	 */
	protected <T> T executeLocked(TransactionLockManager<T> lockManager, String lockKey, Supplier<T> method) throws NodeException {
		try {
			return lockManager.execute(lockKey, () -> {
				Transaction oldTransaction = getTransaction();

				try {
					transaction = TransactionManager.getCurrentTransaction();

					return method.supply();
				} catch (NodeException e) {
					throw new RuntimeException(e);
				} finally {
					transaction = oldTransaction;
				}
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw new NodeException(e);
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}
}
