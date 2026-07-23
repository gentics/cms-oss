package com.gentics.contentnode.rest.util;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.commons.collections.CollectionUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.exception.TranslationException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
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
public class RestCallable<T extends GenericResponse> implements Callable<T> {
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
	 * Session of the user
	 */
	protected Session session;

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
	 * Transaction timestamp
	 */
	protected long trxTimestamp;

	/**
	 * Wrapped callable
	 */
	protected Callable<T> wrapped;

	/**
	 * Response transformer
	 */
	protected Function<GenericResponse, T> responseTransformer;

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
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @throws NodeException
	 */
	public RestCallable(String description, Callable<T> wrapped, Function<GenericResponse, T> responseTransformer) throws NodeException {
		this(description, null, wrapped, responseTransformer);
	}

	/**
	 * Create an instance that will lock, if a lock is given
	 * 
	 * @param description
	 *            job description
	 * @param lock lock (may be null for no locking)
	 * @param wrapped
	 *            wrapped callable, containing the job code
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @throws NodeException
	 */
	public RestCallable(String description, Lock lock, Callable<T> wrapped, Function<GenericResponse, T> responseTransformer) throws NodeException {
		this.jobId = Operator.jobIdGenerator.getAndIncrement();
		this.description = description;
		this.wrapped = wrapped;
		this.responseTransformer = responseTransformer;
		this.lock = lock;

		Transaction t = TransactionManager.getCurrentTransaction();
		session = t.getSession();
		userId = t.getUserId();
		languageId = ContentNodeHelper.getLanguageId();
		trxTimestamp = t.getTimestamp();
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
	public Callable<T> getWrapped() {
		return wrapped;
	}

	@Override
	public T call() throws Exception {
		if (lock != null) {
			T response = lockManager.executeLocked(lock, new ReturnValueLockCallback<T>() {
				@Override
				public T doInLock() {
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
	protected T execute() throws Exception {
		ContentNodeHelper.setLanguageId(languageId);
		try (Trx trx = new Trx(session, userId)) {
			trx.getTransaction().setTimestamp(trxTimestamp);
			try {
				Operator.jobIsStarting(this);
				T result = wrapped.call();
				trx.success();
				return handleResponse(result);
			} catch (EntityNotFoundException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(responseTransformer.apply(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()))));
			} catch (InsufficientPrivilegesException e) {
				InsufficientPrivilegesMapper.log(e);
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(responseTransformer.apply(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()))));
			} catch (ReadOnlyException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(responseTransformer.apply(new GenericResponse(new com.gentics.contentnode.rest.model.response.Message(Type.CRITICAL, e.getLocalizedMessage()),
						new ResponseInfo(ResponseCode.FAILURE, e.getMessage()))));
			} catch (RestMappedException e) {
				if (throwNodeException && !background) {
					throw e;
				}
				return handleResponse(responseTransformer.apply(e.getRestResponse()));
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
	protected T handleResponse(final T response) throws NodeException {
		if (queueResult != null) {
			queueResult.handleResponse(response);
		} else if (background && userId != null) {
			TransactionManager.execute(new Executable() {
				@Override
				public void execute() throws NodeException {
					Transaction t = TransactionManager.getCurrentTransaction();
					MessageSender messageSender = new MessageSender();
					t.addTransactional(messageSender);
					if (response.getResponseInfo().getResponseCode() == ResponseCode.OK && CollectionUtils.isEmpty(response.getMessages())) {
						CNI18nString messageText = new CNI18nString("backgroundjob_finished_successfully");

						messageText.addParameter(description);
						Message message = new Message(1, userId, messageText.toString(), INSTANT_TIME);
						messageSender.sendMessage(message);
					}

					if (!CollectionUtils.isEmpty(response.getMessages())) {
						for (com.gentics.contentnode.rest.model.response.Message restMessage : response.getMessages()) {
							User sender = restMessage.getSender();
							int senderId = sender != null ? sender.getId() : 1;
							messageSender.sendMessage(new Message(senderId, userId, restMessage.getMessage(), INSTANT_TIME));
						}
					}
				}
			});
		}

		return response;
	}
}
