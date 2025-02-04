package com.gentics.contentnode.rest.resource.impl.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.lib.log.NodeLogger;

/**
 * Controller for background jobs, that can be started/stopped and monitored over the (internal) REST API
 */
public class JobController {
	/**
	 * Job name
	 */
	private String name;

	/**
	 * Name of the logger
	 */
	private String loggerName;

	/**
	 * Flag, that is set for jobs that use a single transaction
	 */
	private boolean singleTransaction = true;

	/**
	 * Operator containing the job execution code
	 */
	private com.gentics.contentnode.etc.Operator operation;

	/**
	 * Future of the last started job
	 */
	private Future<JobStatus> running;

	/**
	 * Progress supplier (returns null by default)
	 */
	private Supplier<JobProgress> progressSupplier = () -> null;

	/**
	 * Create instance of job controller
	 * @param name name
	 * @param loggerName logger name
	 * @param operation job operation
	 */
	public JobController(String name, String loggerName, com.gentics.contentnode.etc.Operator operation) {
		this.name = name;
		this.loggerName = loggerName;
		this.operation = operation;
	}

	/**
	 * Get logger name
	 * 
	 * @return
	 */
	protected String getLoggerName() {
		return loggerName;
	}

	/**
	 * Overwrite an operator from a subclass.
	 * 
	 * @param operation
	 */
	protected void setOperation(com.gentics.contentnode.etc.Operator operation) {
		this.operation = operation;
	}

	/**
	 * Set single transaction flag
	 * @param singleTransaction flag
	 * @return fluent API
	 */
	public JobController setSingleTransaction(boolean singleTransaction) {
		this.singleTransaction = singleTransaction;
		return this;
	}

	/**
	 * Set a progress supplier
	 * @param progressSupplier progress supplier
	 * @return fluent API
	 */
	public JobController setProgressSupplier(Supplier<JobProgress> progressSupplier) {
		this.progressSupplier = progressSupplier;
		return this;
	}

	/**
	 * Get the job status of the background job
	 * @return job status
	 */
	public JobStatus getJobStatus() {
		if (running != null) {
			try {
				return running.get(1, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				// job is currently running
				return new JobStatus(I18NHelper.get("job_running", name), true).setProgress(progressSupplier.get());
			} catch (CancellationException e) {
				// job has been cancelled
				JobStatus status = new JobStatus().setProgress(progressSupplier.get());
				status.setRunning(false);
				status.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Job was cancelled"));
				status.addMessage(new Message(Type.INFO, I18NHelper.get("job_cancelled", name)));
				return status;
			} catch (Exception e) {
				JobStatus status = new JobStatus().setProgress(progressSupplier.get());
				status.setRunning(false);
				status.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
				status.addMessage(new Message(Type.CRITICAL, I18NHelper.get("job_error", name)));
				return status;
			}
		} else {
			// job is not running
			return new JobStatus(I18NHelper.get("job_not_running", name), false).setProgress(progressSupplier.get());
		}
	}

	/**
	 * Start the job
	 * @return job status
	 */
	public synchronized JobStatus start() {
		if (running != null && !running.isDone()) {
			return new JobStatus(I18NHelper.get("job_running", name), true).setProgress(progressSupplier.get());
		} else {
			running = Operator.getExecutor().submit(new Callable<JobStatus>() {
				@Override
				public JobStatus call() throws Exception {
					if (singleTransaction) {
						try (Trx trx = new Trx(); LogCollector logColl = new LogCollector()) {
							ContentNodeHelper.setLanguageId(2);
							operation.operate();

							JobStatus status = new JobStatus().setProgress(progressSupplier.get());
							status.setRunning(false);
							status.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
							status.setMessages(logColl.appender.messages);
							return status;
						} catch (Exception e) {
							NodeLogger.getNodeLogger(getClass()).error(String.format("Error while executing %s job", name), e);
							throw e;
						}
					} else {
						try (LogCollector logColl = new LogCollector()) {
							ContentNodeHelper.setLanguageId(2);
							operation.operate();

							JobStatus status = new JobStatus().setProgress(progressSupplier.get());
							status.setRunning(false);
							status.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
							status.setMessages(logColl.appender.messages);
							return status;
						} catch (Exception e) {
							NodeLogger.getNodeLogger(getClass()).error(String.format("Error while executing %s job", name), e);
							throw e;
						}
					}
				}
			});

			return getJobStatus();
		}
	}

	/**
	 * Stop the job (if running)
	 * @return generic response
	 */
	public GenericResponse stop() {
		if (running != null && !running.isDone()) {
			if (!running.cancel(true)) {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.FAILURE, "Failed to cancel running job"));
			} else {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully cancelled running job"));
			}
		} else {
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Job not running"));
		}
	}

	/**
	 * AutoClosable that adds an appender to the logger
	 */
	protected class LogCollector implements AutoCloseable {
		/**
		 * Logger
		 */
		protected NodeLogger logger;

		/**
		 * Appender
		 */
		protected MessageAppender appender;

		/**
		 * Create an instance. add the appender to the logger
		 */
		public LogCollector() {
			logger = NodeLogger.getNodeLogger(loggerName);
			appender = new MessageAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build());
			NodeLogger.addAppenderToConfig(appender, logger);
			logger.addAppender(appender);
		}

		@Override
		public void close() throws Exception {
			// remove the appender
			NodeLogger.removeAppenderFromConfig(appender.getName(), logger);
		}
	}

	/**
	 * Appender implementation that logs to messages
	 */
	protected class MessageAppender extends AbstractAppender {
		/**
		 * List of logged messages
		 */
		protected List<Message> messages = new ArrayList<Message>();

		/**
		 * Create an instance
		 * @param name appender name
		 * @param layout appender layout
		 */
		public MessageAppender(Layout<? extends Serializable> layout) {
			super(UUID.randomUUID().toString(), null, layout, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(new Message(level2Type(event.getLevel()), event.getMessage().getFormattedMessage(), null, event.getTimeMillis()));
		}

		/**
		 * Convert the log level to a message type
		 * @param level log level
		 * @return message type
		 */
		protected Type level2Type(Level level) {
			if (level.isMoreSpecificThan(Level.ERROR)) {
				return Type.CRITICAL;
			} else if (level.isMoreSpecificThan(Level.WARN)) {
				return Type.WARNING;
			} else {
				return Type.INFO;
			}
		}
	}
}
