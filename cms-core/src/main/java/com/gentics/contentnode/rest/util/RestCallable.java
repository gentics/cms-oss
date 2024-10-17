package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.TranslationException;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator.Lock;
import com.gentics.contentnode.rest.util.Operator.QueueResult;
import com.gentics.lib.i18n.CNI18nString;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import de.jkeylockmanager.manager.ReturnValueLockCallback;

/**
 * Implementation of a callable, that can be sent to the background
 */
public class RestCallable implements Callable<GenericResponse> {
	/**
	 * Instant message time
	 */
	public final static int INSTANT_TIME = 7200;

	/**
	 * Keyed lock to synchronize calls for the same lock
	 */
	private static final KeyLockManager lockManager = KeyLockManagers.newLock();

	/**
	 * Internal jobId
	 */
	protected long jobId;

	/**
	 * Job description
	 */
	protected String description;

	/**
	 * Session ID of the user
	 */
	protected String sessionId;

	/**
	 * User ID
	 */
	protected Integer userId;

	/**
	 * Flag to mark, whether the job was sent to the background
	 */
	protected boolean background = false;

	/**
	 * Language ID
	 */
	protected int languageId;

	/**
	 * Wrapped callable
	 */
	protected Callable<GenericResponse> wrapped;

	/**
	 * optional lock
	 */
	protected Lock lock;

	/**
	 * Caught exception
	 */
	protected Exception e;

	/**
	 * Shared queue result, if this callable is part of a queue
	 */
	protected QueueResult queueResult;

	/**
	 * Flag to throw caught NodeException
	 */
	protected boolean throwNodeException = false;

	/**
	 * Create an instance
	 * 
	 * @param description
	 *            job description
	 * @param wrapped
	 *            wrapped callable, containing the job code
	 * @throws NodeException
	 */
	public RestCallable(String description, Callable<GenericResponse> wrapped) throws NodeException {
		this(description, null, wrapped);
	}

	/**
	 * Create an instance that will lock, if a lock is given
	 * 
	 * @param description
	 *            job description
	 * @param lock lock (may be null for no locking)
	 * @param wrapped
	 *            wrapped callable, containing the job code
	 * @throws NodeException
	 */
	public RestCallable(String description, Lock lock, Callable<GenericResponse> wrapped) throws NodeException {
		this.jobId = Operator.jobIdGenerator.getAndIncrement();
		this.description = description;
		this.wrapped = wrapped;
		this.lock = lock;

		Transaction t = TransactionManager.getCurrentTransaction();
		sessionId = t.getSessionId();
		userId = t.getUserId();
		languageId = ContentNodeHelper.getLanguageId();
	}

	/**
	 * Set whether to throw a caught NodeException
	 * @param throwNodeException flag
	 */
	public void setThrowNodeException(boolean throwNodeException) {
		this.throwNodeException = throwNodeException;
	}

	/**
	 * Send this job to the background. Once the job is finished, instant
	 * messages will be sent to the user
	 */
	public void sendToBackground() {
		background = true;
	}

	/**
	 * Add this callable to a callable queue by setting the shared queue result.
	 * If the callable is part of a queue and is finished in background, the result is added to the queueResult instead of sent as message directly
	 * @param queueResult queue result
	 */
	public void addToQueue(QueueResult queueResult) {
		this.queueResult = queueResult;
	}

	/**
	 * Get the wrapped callable
	 * @return wrapped callable
	 */
	public Callable<GenericResponse> getWrapped() {
		return wrapped;
	}

	@Override
	public GenericResponse call() throws Exception {
		if (lock != null) {
			GenericResponse response = lockManager.executeLocked(lock, new ReturnValueLockCallback<GenericResponse>() {
				@Override
				public GenericResponse doInLock() {
					try {
						return execute();
					} catch (Exception e) {
						RestCallable.this.e = e;
						return null;
					}
				}
			});
			if (e != null) {
				throw e;
			}
			return response;
		} else {
			return execute();
		}
	}

	/**
	 * Execute the wrapped callable and return the response
	 * @return response
	 * @throws Exception
	 */
	protected GenericResponse execute() throws Exception {
		ContentNodeHelper.setLanguageId(languageId);
		try (Trx trx = new Trx(sessionId, userId)) {
			try {
				Operator.jobIsStarting(this);
				GenericResponse result = wrapped.call();
				trx.success();
				return handleResponse(result);
			} catch (EntityNotFoundException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage())));
			} catch (InsufficientPrivilegesException e) {
				InsufficientPrivilegesMapper.log(e);
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.PERMISSION, e.getMessage())));
			} catch (ReadOnlyException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.FAILURE, e.getMessage())));
			} catch (RestMappedException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(e.getRestResponse());
			} catch (Exception e) {
				Operator.logger.error("Error while '" + description + "'", e);
				if (background && userId != null) {
					TransactionManager.execute(new Executable() {
						@Override
						public void execute() throws NodeException {
							Transaction t = TransactionManager.getCurrentTransaction();
							MessageSender messageSender = new MessageSender();
							t.addTransactional(messageSender);

							CNI18nString messageText = new CNI18nString("backgroundjob_finished_with_errors");
							messageText.addParameter(description);
							var message = messageText.toString();

							if (e instanceof TranslationException) {
								message = e.getMessage();
							}

							messageSender.sendMessage(new Message(1, userId, message, INSTANT_TIME));
						}
					});
				}

				throw e;
			} finally {
				Operator.jobFinished(this);
			}
		}
	}

	/**
	 * Handle the given response. If the job was sent to the background, send
	 * instant messages
	 * 
	 * @param response
	 *            response to handle
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse handleResponse(final GenericResponse response) throws NodeException {
		if (queueResult != null) {
			queueResult.handleResponse(response);
		} else if (background && userId != null) {
			TransactionManager.execute(new Executable() {
				@Override
				public void execute() throws NodeException {
					Transaction t = TransactionManager.getCurrentTransaction();
					MessageSender messageSender = new MessageSender();
					t.addTransactional(messageSender);
					if (response.getResponseInfo().getResponseCode() == ResponseCode.OK && response.getMessages().isEmpty()) {
						CNI18nString messageText = new CNI18nString("backgroundjob_finished_successfully");

						messageText.addParameter(description);
						Message message = new Message(1, userId, messageText.toString(), INSTANT_TIME);
						messageSender.sendMessage(message);
					}

					for (com.gentics.contentnode.rest.model.response.Message restMessage : response.getMessages()) {
						User sender = restMessage.getSender();
						int senderId = sender != null ? sender.getId() : 1;
						messageSender.sendMessage(new Message(senderId, userId, restMessage.getMessage(), INSTANT_TIME));
					}
				}
			});
		}

		return response;
	}
}
