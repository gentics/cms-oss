package com.gentics.contentnode.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base class for background jobs
 */
public abstract class AbstractBackgroundJob implements Callable<GenericResponse> {
	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(AbstractBackgroundJob.class);

	/**
	 * Messages
	 */
	private List<Message> messages = new ArrayList<>();

	/**
	 * Flag to mark, whether the job shall be interrupted
	 */
	protected boolean interrupted = false;

	/**
	 * Flag to mark, whether the job finished with success
	 */
	protected boolean success = true;

	/**
	 * Get the messages
	 * @return messages
	 */
	public List<Message> getMessages() {
		return messages;
	}

	/**
	 * Add a message
	 * @param message message to add
	 */
	public void addMessage(Message message) {
		messages.add(message);
	}

	/**
	 * Execute the job with default foreground time
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse execute() throws NodeException {
		return execute(null, null, null);
	}

	/**
	 * Execute the job with the optionally given foreground time
	 * @param requestedForegroundTime foreground time
	 * @param unit unit of the foreground time
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse execute(Integer requestedForegroundTime, TimeUnit unit) throws NodeException {
		return execute(requestedForegroundTime, unit, null);
	}

	/**
	 * Execute the job with optionally given foreground time and callback for putting the job into the background
	 * @param requestedForegroundTime foreground time
	 * @param unit unit of the foreground time
	 * @param backgroundCallback opional callback, which is called when the job is put into the background
	 * @return response
	 * @throws NodeException
	 */
	public GenericResponse execute(Integer requestedForegroundTime, TimeUnit unit, Runnable backgroundCallback) throws NodeException {
		long foregroundTimeMs = ObjectTransformer.getInt(
				NodeConfigRuntimeConfiguration.getPreferences().getProperty("backgroundjob_foreground_time"), 5) * 1000;

		if (requestedForegroundTime != null && unit != null) {
			foregroundTimeMs = unit.toMillis(requestedForegroundTime);
		}

		return Operator.executeLocked(getJobDescription(), foregroundTimeMs, null, this, null, backgroundCallback);
	}

	@Override
	public GenericResponse call() throws Exception {
		processAction();

		GenericResponse response = new GenericResponse();
		for (Message message : getMessages()) {
			response.addMessage(message);
		}

		if (success) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Job finished successfully"));
		} else {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Job finished with error"));
		}
		return response;
	}

	/**
	 * Set the internal interrupted flag
	 */
	public void interrupt() {
		interrupted = true;
	}

	/**
	 * Get the i18ned job description (to be used in messages to the user)
	 * @return job description as localized string
	 */
	public abstract String getJobDescription();

	/**
	 * Abstract method to process the action, needs to be implemented by the
	 * subclasses. When this method is called, the curren transaction (also
	 * stored in {@link AbstractUserActionJob#t}) is initialized with the
	 * permissions of the given user. The transaction will be rolled back or
	 * committed by the abstract base class, so there is no need to do this in
	 * the implementation of this method.
	 * @throws NodeException
	 */
	protected abstract void processAction() throws NodeException;

	/**
	 * If the job was interrupted, throw an exception
	 * @throws JobExecutionException
	 */
	protected void abortWhenInterrupted() throws NodeException {
		if (interrupted) {
			if (logger.isInfoEnabled()) {
				logger.info("Job was interrupted");
			}
			throw new NodeException("Job was interrupted");
		}
	}
}
