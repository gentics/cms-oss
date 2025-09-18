package com.gentics.contentnode.factory.object;

import static com.gentics.contentnode.factory.Trx.supply;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.scheduler.ExecutionModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleFollow;
import com.gentics.contentnode.rest.model.scheduler.ScheduleInterval;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.scheduler.InternalSchedulerTask;
import com.gentics.contentnode.scheduler.InternalSchedulerTaskService;
import com.gentics.contentnode.scheduler.SimpleScheduler;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.mail.MailSender;
import com.gentics.lib.util.FileUtil;

/**
 * Factory for scheduler tasks, schedules and executions
 */
@DBTables({
	@DBTable(clazz = SchedulerTask.class, name = "scheduler_task"),
	@DBTable(clazz = SchedulerSchedule.class, name = "scheduler_schedule") })
public class SchedulerFactory extends AbstractFactory {
	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;
	public final static String SCHEDULER_SUSPEND_NAME = "sync_system:scheduler_suspend";

	public static final String FAILURE_MAIL_TO_PARAM = "scheduler_job_failure_email.to";
	public static final String FAILURE_MAIL_FROM_PARAM = "scheduler_job_failure_email.from";
	public static final String FAILURE_MAIL_SUBJECT_PARAM = "scheduler_job_failure_email.subject";
	public static final String FAILURE_MAIL_IS_HTML_PARAM = "scheduler_job_failure_email.is_html";
	public static final String FAILURE_MAIL_BODY_PARAM = "scheduler_job_failure_email.body";

	private static final int TASK_STATUS_FAILED = 255;
	private static final int TASK_STATUS_ABORTED = 254;

	private static ThreadFactory threadFactory = new PrefixedThreadFactory("scheduler-executor");

	/**
	 * Loader for the implementations of {@link InternalSchedulerTaskService}
	 */
	private final static ServiceLoaderUtil<InternalSchedulerTaskService> taskProviderLoader = ServiceLoaderUtil
			.load(InternalSchedulerTaskService.class);

	/**
	 * Set of IDs of currently running schedules (only used on master).
	 */
	private final Set<Integer> runningSchedules = Collections.synchronizedSet(new HashSet<>());

	/**
	 * List containing instances of {@link Consumer} for executions which should be finished, but could not
	 * (due to an error when creating a transaction, which is most likely caused by an unavailable database)
	 */
	private final List<Consumer<Transaction>> pendingFinishes = new ArrayList<>();

	/**
	 * Set of IDs of Schedules waiting to be executed.
	 */
	private final Queue<Integer> executionQueue = new ArrayDeque<>();

	/**
	 * Executor service for scheduler executions.
	 */
	private ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

	/**
	 * Currently running executor future.
	 */
	private ScheduledFuture<?> schedulerFuture;

	static {
		// register the factory classes
		try {
			registerFactoryClass("scheduler_task", SchedulerTask.TYPE_SCHEDULER_TASK, false, FactorySchedulerTask.class);
			registerFactoryClass("scheduler_schedule", SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, false, FactorySchedulerSchedule.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}
	/**
	 * Get the file collecting stdout for the given execution.
	 * If the parameter create is true, this method will try to create the file.
	 * If the file does not exist (or cannot be created), a warning will be logged and this method will just return null
	 * @param executionId execution ID
	 * @param create true to create the file (if it does not exist)
	 * @return the file, if it exists or null
	 */
	public static File getExecutionStdout(int executionId, boolean create) {
		File out = new File(getLogDirectory(), String.format("scheduler_execution_%d.out", executionId));
		return getExecutionLogFile(out, create);
	}

	/**
	 * Get the file collecting stderr for the given execution.
	 * If the parameter create is true, this method will try to create the file.
	 * If the file does not exist (or cannot be created), a warning will be logged and this method will just return null
	 * @param executionId exeuction ID
	 * @param create true to create the file (if it does not exist)
	 * @return the file, if it exists or null
	 */
	public static File getExecutionStderr(int executionId, boolean create) {
		File err = new File(getLogDirectory(), String.format("scheduler_execution_%d.err", executionId));
		return getExecutionLogFile(err, create);
	}

	/**
	 * Get the configured log directory
	 * @return log directory
	 */
	protected static File getLogDirectory() {
		return new File(ConfigurationValue.LOGS_PATH.get());
	}

	/**
	 * Get the given execution file, if it exists, is a file and can be written to (or can be created).
	 * Return null otherwise
	 * @param file file
	 * @param create true to create the file, if it does not exist
	 * @return the file, it it exists or null
	 */
	protected static File getExecutionLogFile(File file, boolean create) {
		if (!file.exists()) {
			if (create) {
				try {
					file.getParentFile().mkdirs();
					file.createNewFile();
					return file;
				} catch (IOException e) {
					logger.warn(String.format("Log file %s can not be created", file.getAbsolutePath()));
					return null;
				}
			} else {
				logger.debug(String.format("Log file %s does not exist", file.getAbsolutePath()));
				return null;
			}
		} else if (file.isDirectory()) {
			logger.warn(String.format("Log file %s is a directory", file.getAbsolutePath()));
			return null;
		} else if (!file.canWrite()) {
			logger.warn(String.format("Log file %s exists, but is not writable", file.getAbsolutePath()));
			return null;
		} else {
			return file;
		}
	}

	/**
	 * Get all internal scheduler tasks
	 * @return list of internal scheduler tasks
	 */
	public static List<InternalSchedulerTask> getInternalSchedulerTasks() {
		List<InternalSchedulerTask> list = new ArrayList<>();
		taskProviderLoader.forEach(service -> list.addAll(service.tasks()));
		return list;
	}

	/**
	 * Get the internal scheduler task by command
	 * @param command command
	 * @return internal scheduler task, or null if not found
	 */
	public static InternalSchedulerTask getInternalSchedulerTask(String command) {
		for (InternalSchedulerTask task : getInternalSchedulerTasks()) {
			if (StringUtils.isEqual(command, task.getCommand())) {
				return task;
			}
		}

		return null;
	}

	/**
	 * Initialize the scheduler factory.
	 * This will make sure, that all internal tasks are present in the database
	 * @throws NodeException
	 */
	public void initialize() throws NodeException {
		for (InternalSchedulerTask task : getInternalSchedulerTasks()) {
			Trx.operate(t -> {
				SchedulerTask schedulerTask = t.getObject(SchedulerTask.class,
						DBUtils.select("SELECT id FROM scheduler_task WHERE internal = ? AND command = ?", ps -> {
							ps.setBoolean(1, true);
							ps.setString(2, task.getCommand());
						}, DBUtils.firstInt("id")));

				if (schedulerTask == null) {
					schedulerTask = t.createObject(SchedulerTask.class);
					schedulerTask.setCommand(task.getCommand());
					schedulerTask.setName(task.getName());
					schedulerTask.setDescription(task.getDescription());
					schedulerTask.setInternal(true);
					schedulerTask.save();
				}
			});
		}

		startScheduler();
	}

	/**
	 * Check if the scheduler executor is running and optionally restart it if
	 * necessary.
	 *
	 * @param restart Whether to restart the scheduler executor if it is not
	 * 		already running.
	 * @return {@code true} when the executor was already running, {@code false}
	 * 		otherwise.
	 * @throws NodeException
	 */
	public boolean checkExecutor(boolean restart) throws NodeException {
		if (schedulerFuture == null || schedulerFuture.isDone()) {
			if (restart) {
				startScheduler();
			}

			return false;
		}

		return true;
	}

	/**
	 * Start the scheduler if it is not already running, and abort any
	 * executions in the database that are started but not finished (i.e. have
	 * no endtime timestamp).
	 *
	 * @see #abortRunningExecutions(Transaction)
	 */
	public synchronized void startScheduler() throws NodeException {
		if (schedulerFuture != null && !schedulerFuture.isDone()) {
			// Scheduler executor is running, nothing to do.
			return;
		}

		Trx.operate(this::abortRunningExecutions);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime nextMinute = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);

		schedulerFuture = SimpleScheduler.getExecutor("scheduler").scheduleAtFixedRate(
			this::performScheduling,
			Duration.between(now, nextMinute).toMillis(),
			Duration.ofMinutes(1).toMillis(), // Initial delay and period use the same time unit, so the minute is converted to milliseconds here.
			TimeUnit.MILLISECONDS);
	}

	/**
	 * Abort any executions that have no endtime timestamp yet.
	 *
	 * <p>
	 *     Call {@link #finishExecution(SchedulerSchedule, int, int, int, List, ExecutionModel, int) finishExecution()}
	 *     for every execution in the database that has endtime == 0. The output
	 *     will contain a message that the execution was aborted, and its result
	 *     status will be set to 254.
	 * </p>
	 *
	 * @param trx The current transation.
	 */
	private void abortRunningExecutions(Transaction trx) throws NodeException {
		Set<ExecutionModel> runningExecutions = DBUtils.select(
			"SELECT * FROM scheduler_execution WHERE endtime = 0",
			rs -> {
				Set<ExecutionModel> executions = new HashSet<>();

				while (rs.next()) {
					executions.add(ExecutionModel.fromDbResult(rs));
				}

				return executions;
			});

		for (ExecutionModel execution : runningExecutions) {
			SchedulerSchedule schedule = trx.getObject(SchedulerSchedule.class, execution.getScheduleId());
			String msg = String.format(
				"Execution %d of schedule %d (%s) aborted due to scheduler restart",
				execution.getId(),
				schedule.getId(),
				schedule.getName());

			logger.info(msg);

			finishExecution(
				schedule,
				execution.getId(),
				execution.getStartTime(),
				trx.getUnixTimestamp(),
				Collections.singletonList(msg),
				execution,
				TASK_STATUS_ABORTED);
		}
	}

	/**
	 * Check if the scheduler is currently disabled, and which schedules may
	 * be executed regardless.
	 *
	 * @param allowedJobIds Output parameter which will be filled with the IDs
	 * 		of schedules that may be executed even if the scheduler is currently
	 * 		suspended
	 * @return {@code true} when the scheduler is currently suspended, and
	 * 		{@code false} otherwise.
	 * @throws NodeException
	 */
	public boolean getStatus(Set<Integer> allowedJobIds) throws NodeException {
		boolean suspended = DBUtils.select(
			"SELECT * from nodesetup WHERE name = ?",
			ps -> ps.setString(1, SCHEDULER_SUSPEND_NAME),
			rs -> {
				if (rs.next()) {
					if (rs.getInt("intvalue") == 1) {
						String textValue = rs.getString("textvalue");

						if (allowedJobIds != null && !StringUtils.isEmpty(textValue)) {
							allowedJobIds.addAll(Stream.of(textValue.split(",")).map(Integer::parseInt).collect(Collectors.toSet()));
						}

						return true;
					}
				}

				return false;
			});


		return suspended;
	}

	/**
	 * Suspend the scheduler, except for the specified schedule IDs.
	 *
	 * @param allowRunIds IDs of schedules which can be executed even when
	 * 		the scheduler is suspended
	 * @throws NodeException
	 */
	public void suspend(Set<Integer> allowRunIds) throws NodeException {
		Map<String, Object> id = new HashMap<>();
		id.put("name", SCHEDULER_SUSPEND_NAME);

		Map<String, Object> data = new HashMap<>();
		data.put("intvalue", 1);
		data.put("textvalue", allowRunIds != null ? allowRunIds.stream().map(Object::toString).collect(Collectors.joining(",")) : "");

		DBUtils.updateOrInsert("nodesetup", id, data);
	}

	/**
	 * Resume the scheduler.
	 */
	public void resume() throws NodeException {
		DBUtils.executeUpdate("DELETE FROM nodesetup WHERE name = ? ", new Object[] { SCHEDULER_SUSPEND_NAME });
	}

	/**
	 * Get the IDs of currently running schedules as unmodifiable set
	 *
	 * @return The IDs of currently running schedules.
	 */
	public Set<Integer> getRunningSchedules() {
		return Collections.unmodifiableSet(runningSchedules);
	}

	/**
	 * Get a list of all due schedules.
	 *
	 * <p>
	 *     <strong>IMPORTANT:</strong> This method assumes that there is an
	 *     active transaction available.
	 * </p>
	 *
	 * @return A list of due schedules.
	 * @throws NodeException
	 */
	public List<SchedulerSchedule> getDueSchedules() throws NodeException {
		return getDueSchedules(false);
	}

	/**
	 * Get a list of all due schedules.
	 *
	 * <p>
	 *     <strong>IMPORTANT:</strong> This method assumes that there is an
	 *     active transaction available.
	 * </p>
	 *
	 * @param forTesting Whether the schedules are retrieved for testing (in
	 * 		which case the suspension status of the scheduler is ignored).
	 * @return A list of due schedules.
	 * @throws NodeException
	 */
	public List<SchedulerSchedule> getDueSchedules(boolean forTesting) throws NodeException {
		Set<Integer> allowedScheduleIds = new HashSet<>();
		boolean suspended = !forTesting && getStatus(allowedScheduleIds);

		if (suspended && allowedScheduleIds.isEmpty()) {
			logger.info("Scheduler is suspended");
			return Collections.emptyList();
		}

		List<SchedulerSchedule> dueSchedules = new ArrayList<>();
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> running = new HashSet<>(DBUtils.select(
			"SELECT scheduler_schedule_id id FROM scheduler_execution WHERE endtime = 0",
			DBUtils.IDLIST));
		List<SchedulerSchedule> schedules = t.getObjects(
			SchedulerSchedule.class,
			DBUtils.select("SELECT id FROM scheduler_schedule WHERE active = true", DBUtils.IDLIST));

		for (SchedulerSchedule schedule : schedules) {
			if ((!suspended || allowedScheduleIds.contains(schedule.getId()))
					&& !running.contains(schedule.getId())
					&& schedule.shouldExecute()) {
				dueSchedules.add(schedule);
			}
		}

		return dueSchedules;
	}

	/**
	 * Execute all due schedules.
	 *
	 * @see #getDueSchedules()
	 */
	public void performScheduling() {
		performScheduling(false);
	}

	/**
	 * Execute all due schedules.
	 *
	 * @see #getDueSchedules(boolean)
	 * @param forTesting Whether to perform scheduling for tesint, in which case
	 * 		the suspend status will be ignored.
	 */
	public void performScheduling(boolean forTesting) {
		// Executions must only be started on the master instance.
		if (!DistributionUtil.isTaskExecutionAllowed()) {
			return;
		}

		// first check whether there are pending finishes
		if (!pendingFinishes.isEmpty()) {
			synchronized (pendingFinishes) {
				for (Iterator<Consumer<Transaction>> i = pendingFinishes.iterator(); i.hasNext();) {
					Consumer<Transaction> consumer = i.next();

					try (Trx trx = new Trx()) {
						consumer.accept(trx.getTransaction());
						trx.success();
						i.remove();
					} catch (Throwable e) {
						logger.warn("Pending execution finish could not be performed");
					}
				}
			}
		}

		try (Trx trx = new Trx()) {
			for (SchedulerSchedule schedule : getDueSchedules(forTesting)) {
				executeNow(schedule);
			}
		} catch (Throwable e) {
			logger.error("Error while performing scheduling: " + e.getLocalizedMessage(), e);

			return;
		}
	}

	/**
	 * Checks if the ID of the given schedule is already in the
	 * {@link #executionQueue}.
	 *
	 * @param schedule The schedule to check.
	 * @return {@code true} if the given schedule is already in the
	 * 		execution queue, and {@code false} if it is.
	 */
	private boolean alreadyScheduled(SchedulerSchedule schedule) {
		synchronized (executionQueue) {
			return executionQueue.contains(schedule.getId());
		}
	}

	/**
	 * Execute the given schedule.
	 *
	 * <p>
	 *     <strong>Note</strong> that the only check this method performs if the
	 *     schedule should be executed at this time is if it is already in the
	 *     {@link #executionQueue}, all scheduling related checks must be done
	 *     beforehand.
	 * </p>
	 *
	 * <p>
	 *     The schedule is executed with the {@link #executor executor service}.
	 * </p>
	 *
	 * @param schedule The schedule to execute
	 * @return {@code true} if the schedule will be executed and {@code false}
	 * 		if it was already in the {@link #executionQueue}.
	 */
	public boolean executeNow(SchedulerSchedule schedule) {
		if (alreadyScheduled(schedule)) {
			return false;
		}

		executor.execute(() -> {
			Integer scheduleId = schedule.getId();
			boolean useExecutionQueue = !schedule.isParallel();

			if (useExecutionQueue) {
				synchronized (executionQueue) {
					executionQueue.add(scheduleId);

					while (!executionQueue.isEmpty() && !executionQueue.peek().equals(scheduleId)) {
						try {
							executionQueue.wait();
						} catch (InterruptedException e) {
							// Nothing to do really, but to start the loop again.
						}
					}

					if (executionQueue.isEmpty() || !executionQueue.peek().equals(scheduleId)) {
						// This schedule was removed from the execution queue from an unknown source. Abort the execution.

						logger.warn(String.format("Execution queue is empty, but schedule {%d} was not yet executed. Aborting execution.", scheduleId));

						return;
					}
				}
			}

			logger.info(String.format("Starting execution of schedule %d (%s)", scheduleId, schedule.getName()));

			int executionId;
			int startTimestamp;
			NodeConfig config;

			synchronized (runningSchedules) {
				try (Trx trx = new Trx()) {
					startTimestamp = trx.getTransaction().getUnixTimestamp();
					config = trx.getTransaction().getNodeConfig();

					List<Integer> ids = DBUtils.executeInsert("INSERT INTO scheduler_execution(scheduler_schedule_id, starttime) VALUES (?, ?)", new Object[] {
						scheduleId,
						startTimestamp
					});

					executionId = ids.get(0);

					DBUtils.update("UPDATE scheduler_schedule SET scheduler_execution_id = ? WHERE id = ?", executionId, scheduleId);

					logger.info(String.format("Executing schedule %d (%s)", scheduleId, schedule.getName()));
					trx.success();
				} catch (Throwable e) {
					logger.error(String.format("Could not create scheduler execution for schedule %d (%s): %s", scheduleId, schedule.getName(), e.getMessage()), e);

					if (useExecutionQueue) {
						executionQueue.remove(scheduleId);
						executionQueue.notifyAll();
					}

					return;
				}

				runningSchedules.add(scheduleId);
			}

			List<String> output = new ArrayList<>();
			ExecutionModel execution = new ExecutionModel();
			int resultStatus = TASK_STATUS_FAILED;

			try {
				SchedulerTask task = supply(() -> schedule.getSchedulerTask());
				task.validate();
				resultStatus = task.execute(executionId, output);
			} catch (Throwable e) {
				logger.error(String.format("Could not execute schedule %d (%s): %s", scheduleId, schedule.getName(), e.getMessage()), e);
				output.add(ExceptionUtils.getStackTrace(e));
			} finally {
				try (Trx trx = new Trx()) {
					finishExecution(schedule, executionId, startTimestamp, trx.getTransaction().getUnixTimestamp(), output, execution, resultStatus);

					logger.info(String.format("Finished schedule %d (%s)", scheduleId, schedule.getName()));
					trx.success();
				} catch (Throwable e) {
					// Can't really do anything here.
					logger.error(String.format("Could update execution for  schedule %d (%s): %s", scheduleId, schedule.getName(), e.getMessage()), e);

					int finalResultStatus = resultStatus;
					pendingFinishes.add(t -> {
						finishExecution(schedule, executionId, startTimestamp, t.getUnixTimestamp(), output, execution, finalResultStatus);
						logger.info(String.format("Finished schedule %d (%s)", scheduleId, schedule.getName()));
					});
				}
			}

			if (resultStatus != 0) {
				List<String> recipients = addAdminMail(config.getDefaultPreferences(), schedule.getNotificationEmail());

				if (!recipients.isEmpty()) {
					sendFailureEmails(recipients, schedule, execution.setLog(String.join("\n", output)), resultStatus, config);
				}
			}
		});

		return true;
	}

	/**
	 * Update the execution database entry with the results of the execution.
	 *
	 * @param schedule The executions schedule
	 * @param executionId The execution ID
	 * @param startTimestamp The start timestamp
	 * @param endTimestamp The end timestamp
	 * @param output The executions output
	 * @param execution The execution model
	 * @param resultStatus The result status
	 * @throws NodeException
	 */
	private void finishExecution(SchedulerSchedule schedule, int executionId, int startTimestamp, int endTimestamp, List<String> output, ExecutionModel execution, int resultStatus) throws NodeException {
		Integer scheduleId = schedule.getId();
		int duration = endTimestamp - startTimestamp;

		try {
			synchronized (runningSchedules) {
				DBUtils.executeUpdate(
					"UPDATE scheduler_execution SET endtime = ?, duration = ?, result = ?, log = ? WHERE id = ?",
					new Object[] { endTimestamp, duration, resultStatus, String.join("\n", output), executionId });

				runningSchedules.remove(scheduleId);
			}
		} finally {
			// Exception handling can occur further up, but we need the finally
			// block to make sure the execution queue is in a valid state.
			if (!schedule.isParallel()) {
				synchronized (executionQueue) {
					Integer nextInQueue = executionQueue.peek();

					if (scheduleId.equals(nextInQueue)) {
						executionQueue.poll();
					} else {
						String warning = String.format(
							"Schedule {%d} was not the head of the execution queue when its execution finished. Next in queue: {%d}",
							scheduleId,
							nextInQueue);

						logger.warn(warning);
					}

					// Either way, all waiting threads need to be notified, so they
					// can check if they are next in the queue.
					executionQueue.notifyAll();
				}
			}
		}

		Pair<Integer, Integer> stats = DBUtils.select(
			"SELECT COUNT(*) count, ROUND(AVG(endtime - starttime)) average FROM scheduler_execution WHERE scheduler_schedule_id = ?",
			stmt -> stmt.setInt(1, scheduleId),
			rs -> {
				if (rs.next()) {
					return Pair.of(rs.getInt("count"), rs.getInt("average"));
				}

				return Pair.of(0, 0);
			});

		DBUtils.executeUpdate(
			"UPDATE scheduler_schedule SET runs = ?, average_time = ? WHERE id = ?",
			new Object[] { stats.getLeft(), stats.getRight(), scheduleId });

		execution
			.setStartTime(startTimestamp)
			.setEndTime(endTimestamp)
			.setDuration(duration);
	}

	/**
	 * Send email notifications about the failed execution.
	 *
	 * <p>
	 * The {@code execution} object must contain the start and end timestamps as well as the execution log.
	 * </p>
	 *
	 * @param recipients
	 * @param schedule The schedule which execution failed
	 * @param execution The info about the failed execution
	 * @param resultStatus The executions result status
	 * @param config The node configuration
	 */
	private void sendFailureEmails(List<String> recipients, SchedulerSchedule schedule, ExecutionModel execution, int resultStatus, NodeConfig config) {
		NodePreferences prefs = config.getDefaultPreferences();
		String mailhost = prefs.getProperty(MessageSender.MAILHOST_PARAM);
		int mailPort = ObjectTransformer.getInt(prefs.getProperty(MessageSender.MAILPORT_PARAM), -1);
		String mailUsername = prefs.getProperty(MessageSender.MAILUSERNAME_PARAM);
		String mailPassword = prefs.getProperty(MessageSender.MAILPASSWORD_PARAM);
		String mailStartTls = prefs.getProperty(MessageSender.MAILSTARTTLS_PARAM);
		String returnPath = prefs.getProperty(MessageSender.MAILRETURNPATH_PARAM);

		if (mailhost == null) {
			mailhost = "localhost";
		}
		if (ObjectTransformer.isEmpty(returnPath)) {
			returnPath = null;
		}

		String command;

		try {
			command = supply(() -> schedule.getSchedulerTask().getCommand());
		} catch (NodeException e) {
			command = "N/A";
		}

		String start = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.ofInstant(Instant.ofEpochSecond(execution.getStartTime()), ZoneId.systemDefault()));
		String end = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.ofInstant(Instant.ofEpochSecond(execution.getEndTime()), ZoneId.systemDefault()));

		for (String recipient : recipients) {
			String mailBody = getMailBody(prefs, schedule.getName(), schedule.getId(), command, resultStatus, start, end, execution.getLog());
			MailSender sender = new MailSender()
				.setHost(mailhost)
				.setFrom(prefs.getProperty(FAILURE_MAIL_FROM_PARAM))
				.setTo(recipient)
				.setSubject(getMailSubject(prefs, schedule.getName(), schedule.getId()))
				.setEnvelopeFrom(returnPath);

			if (ObjectTransformer.getBoolean(prefs.getProperty(FAILURE_MAIL_IS_HTML_PARAM), false)) {
				sender.setBodyHTML(mailBody);
			} else {
				sender.setBodyText(mailBody);
			}

			if (mailPort > 0) {
				sender.setPort(mailPort);
			}

			if (!StringUtils.isEmpty(mailStartTls)) {
				sender.setStartTLS(Boolean.parseBoolean(mailStartTls));
			}

			if (!StringUtils.isEmpty(mailUsername)) {
				sender.setAuth(mailUsername, mailPassword);
			}

			try {
				sender.send();
			} catch (Throwable e) {
				String msg = String.format(
					"Could not send notification mail for failed execution of task %d (%s) to %s: %s",
					schedule.getId(),
					schedule.getName(),
					recipient,
					e.getMessage());

				logger.error(msg, e);
			}
		}
	}

	/**
	 * Add the admin mail address if it was configured.
	 *
	 * @param prefs The node preferences.
	 * @param recipients The current recipients.
	 *
	 * @return The recipients with the added admin mail.
	 */
	private List<String> addAdminMail(NodePreferences prefs, List<String> recipients) {
		String adminMail = prefs.getProperty(FAILURE_MAIL_TO_PARAM);

		if (!StringUtils.isEmpty(adminMail)) {
			recipients.add(adminMail);
		}

		return recipients;
	}

	/**
	 * Get the notification mail subject.
	 *
	 * <p>
	 *     This will use the configured mail subject if available or fall back
	 *     to "Scheduler Run Failed: SCHEDULE_NAME (ID: SCHEDULE_ID)". The
	 *     template can contain the placeholders {@code #name#} and {@code #id#}
	 *     which will be replaced by the schedule name and ID respectively.
	 * </p>
	 *
	 * @param prefs The node preferences.
	 * @param name The schedule name.
	 * @param id The schedule ID.
	 * @return The configured mail subject with replaced placeholders, or
	 * 		the fallback subject.
	 */
	private String getMailSubject(NodePreferences prefs, String name, Integer id) {
		String template = ObjectTransformer.getString(
			prefs.getProperty(FAILURE_MAIL_SUBJECT_PARAM),
			"Scheduler Run Failed: #name# (ID: #id#)");
		String scheduleId = id == null ? "" : id.toString();

		return template.replaceAll("#name#", name).replaceAll("#id#", scheduleId);
	}

	/**
	 * Get the notification mail body.
	 *
	 * <p>
	 *     This will use the configured mail subject if available or a fallback
	 *     message. The template can contain the following placeholders:
	 *     <ul>
	 *         <li><code>#name#</code>: The schedule name</li>
	 *         <li><code>#cmd#</code>: The executed command</li>
	 *         <li><code>#returnvalue#</code>: The execution result (an integer 0-255; 0 meaning success)</li>
	 *         <li><code>#starttime#</code>: The start timestamp of the execution</li>
	 *         <li><code>#endtime#</code>: The end timestamp of the execution</li>
	 *         <li><code>#output#</code>: The commands output</li>
	 *     </ul>
	 * </p>
	 *
	 * @param prefs The node preferences.
	 * @param name The schedule name.
	 * @param id The schedule ID.
	 * @param command The executed command.
	 * @param resultStatus The commands result status.
	 * @param start The start timestamp.
	 * @param end The end timestamp.
	 * @param log The command output.
	 * @return The notification mail body with replaced placeholders.
	 */
	private String getMailBody(NodePreferences prefs, String name, Integer id, String command, int resultStatus, String start, String end, String log) {
		String template = ObjectTransformer.getString(
			prefs.getProperty(FAILURE_MAIL_BODY_PARAM),
			"Scheduler execution failed '#name#' (#id#)\nCommand: #cmd#\nReturn value: #returnvalue#\nStart time: #starttime#\nEnd time: #endtime#\nOutput:\n#output#\n");

		return template
			.replaceAll("#name#", Matcher.quoteReplacement(name))
			.replaceAll("#id#", id == null ? "N/A" : id.toString())
			.replaceAll("#cmd#", Matcher.quoteReplacement(command))
			.replaceAll("#returnvalue#", String.valueOf(resultStatus))
			.replaceAll("#starttime#", Matcher.quoteReplacement(start))
			.replaceAll("#endtime#", Matcher.quoteReplacement(end))
			.replaceAll("#output#", Matcher.quoteReplacement(log));
	}

	/**
	 * Factory implementation of {@link SchedulerTask}
	 */
	private static class FactorySchedulerTask extends AbstractContentObject implements SchedulerTask {
		/**
		 * Serial Version UUID
		 */
		private static final long serialVersionUID = -4897017215752114452L;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("description")
		@Updateable
		protected String description;

		@DataField("command")
		@Updateable
		protected String command;

		@DataField("internal")
		protected boolean internal;

		@DataField("cdate")
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("creator")
		protected int creatorId = 0;

		@DataField("edate")
		@Updateable
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		protected int editorId = 0;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactorySchedulerTask(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info object info
		 */
		protected FactorySchedulerTask(Integer id, NodeObjectInfo info) {
			super(id, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @throws NodeException
		 */
		public FactorySchedulerTask(Integer id, NodeObjectInfo info, Map<String, Object> dataMap) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public NodeObject copy() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getCommand() {
			return command;
		}

		@Override
		public String getSanitizedCommand() throws NodeException {
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.INSECURE_SCHEDULER_COMMAND)) {
				return getCommand();
			} else {
				return ConfigurationValue.SCHEDULER_COMMANDS_PATH.get() + FileUtil.sanitizeName(getCommand(), Collections.emptyMap(), "", null);
			}
		}

		@Override
		public boolean isInternal() {
			return internal;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);
			assertNodeObjectNotNull(creator, creatorId, "Creator");
			return creator;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);
			assertNodeObjectNotNull(editor, editorId, "Editor");
			return editor;
		}

		@Override
		public void validate() throws NodeException {
			if (isInternal()) {
				return;
			} else if (NodeConfigRuntimeConfiguration.isFeature(Feature.INSECURE_SCHEDULER_COMMAND)) {
				return;
			} else {
				String sanitizedCommand = getSanitizedCommand();
				File commandFile = new File(sanitizedCommand);

				if (!commandFile.exists() || !commandFile.isFile() || !commandFile.canExecute()) {
					throw new RestMappedException(I18NHelper.get("scheduler_task.command.invalid.nofile", getCommand(),
							ConfigurationValue.SCHEDULER_COMMANDS_PATH.get())).setMessageType(Message.Type.CRITICAL)
									.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
				}
			}
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			// delete by adding to the delete list
			Collection<SchedulerTask> deleteList = ((SchedulerFactory)t.getObjectFactory(SchedulerTask.class)).getDeleteList(SchedulerTask.class);
			deleteList.add(this);
		}

		@Override
		public Object get(String key) {
			try {
				switch (key) {
				case "name":
					return getName();
				case "description":
					return getDescription();
				case "command":
					return getCommand();
				case "internal":
					return isInternal();
				case "cdate":
					return getCDate().getIntTimestamp();
				case "edate":
					return getEDate().getIntTimestamp();
				case "creator":
					return getCreator();
				case "editor":
					return getEditor();
				default:
					return super.get(key);
				}
			} catch (NodeException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return String.format("SchedulerTask {id: %d, name: %s}", id, name);
		}
	}

	/**
	 * Editable factory implementation of {@link SchedulerTask}
	 */
	private static class EditableFactorySchedulerTask extends FactorySchedulerTask {
		/**
		 * Serial Version UUID
		 */
		private static final long serialVersionUID = -1560513435700982636L;

		/**
		 * Flag to mark whether the object has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Create an empty instance of an editable value
		 * @param info
		 */
		public EditableFactorySchedulerTask(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor to create a copy of the given object
		 * @param task task to copy
		 * @param info info about the copy
		 * @param asNew true when the object shall be a new object, false for just the editable version of the object
		 */
		public EditableFactorySchedulerTask(SchedulerTask task, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : task.getId(), info, getDataMap(task));
			if (asNew) {
				this.modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public void setDescription(String description) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.description, description)) {
				this.description = description;
				this.modified = true;
			}
		}

		@Override
		public void setCommand(String command) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.command, command)) {
				this.command = command;
				this.modified = true;
			}
		}

		@Override
		public void setInternal(boolean internal) throws ReadOnlyException {
			if (this.internal != internal) {
				this.internal = internal;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = false;
			boolean isNew = isEmptyId(getId());
			SchedulerTask original = null;

			if (!isNew) {
				original = t.getObject(SchedulerTask.class, getId());
			}

			name = ObjectTransformer.getString(name, "").trim();
			description = ObjectTransformer.getString(description, "");

			// make the name unique
			setName(UniquifyHelper.makeUnique(name, MAX_NAME_LENGTH,
					"SELECT name FROM scheduler_task WHERE id != ? AND name = ?", SeparatorType.blank,
					ObjectTransformer.getInt(getId(), -1)));

			if (modified) {
				// object is modified, so update it
				isModified = true;

				// set creator data for new objects
				if (isNew) {
					creatorId = t.getUserId();
					cDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				// set the editor data
				editorId = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());

				saveFactoryObject(this);
				modified = false;
			}

			// logcmd, versions and trigger event
			if (isModified) {
				List<String> modifiedData = new ArrayList<>();

				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, SchedulerTask.TYPE_SCHEDULER_TASK, getId(), 0, "SchedulerTask.create");
					t.addTransactional(new TransactionalTriggerEvent(SchedulerTask.class, getId(), null, Events.CREATE));
				} else {
					String[] mod = getModifiedData(original, this);
					modifiedData.addAll(Arrays.asList(mod));

					ActionLogger.logCmd(ActionLogger.EDIT, SchedulerTask.TYPE_SCHEDULER_TASK, getId(), 0, "SchedulerTask.update");
					t.addTransactional(new TransactionalTriggerEvent(SchedulerTask.class, getId(), mod, Events.UPDATE));
				}

				t.dirtObjectCache(SchedulerTask.class, getId());
			}

			return isModified;
		}
	}

	/**
	 * Factory implementation of {@link SchedulerSchedule}
	 */
	private static class FactorySchedulerSchedule extends AbstractContentObject implements SchedulerSchedule {
		/**
		 * Serial Version UUID
		 */
		private static final long serialVersionUID = 5839009801198882066L;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("description")
		@Updateable
		protected String description;

		@DataField("scheduler_task_id")
		@Updateable
		protected int schedulerTaskId;

		@DataField("parallel")
		@Updateable
		protected boolean parallel;

		@DataField(value = "schedule_json", json = true)
		@Updateable
		protected ScheduleData scheduleData = new ScheduleData();

		@DataField("active")
		@Updateable
		protected boolean active;

		@DataField("notification_email_json")
		@Updateable
		protected List<String> notificationEmail = new ArrayList<>();

		@DataField("cdate")
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("creator")
		protected int creatorId = 0;

		@DataField("edate")
		@Updateable
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		protected int editorId = 0;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactorySchedulerSchedule(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info object info
		 */
		protected FactorySchedulerSchedule(Integer id, NodeObjectInfo info) {
			super(id, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @throws NodeException
		 */
		public FactorySchedulerSchedule(Integer id, NodeObjectInfo info, Map<String, Object> dataMap)
				throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public NodeObject copy() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public SchedulerTask getSchedulerTask() throws NodeException {
			SchedulerTask schedulerTask = TransactionManager.getCurrentTransaction().getObject(SchedulerTask.class, schedulerTaskId);
			assertNodeObjectNotNull(schedulerTask, schedulerTaskId, "SchedulerTask");
			return schedulerTask;
		}

		@Override
		public boolean isParallel() {
			return parallel;
		}

		@Override
		public ScheduleData getScheduleData() {
			return scheduleData;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public List<String> getNotificationEmail() {
			if (notificationEmail == null) {
				notificationEmail = new ArrayList<>();
			}

			return notificationEmail;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);
			assertNodeObjectNotNull(creator, creatorId, "Creator");
			return creator;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);
			assertNodeObjectNotNull(editor, editorId, "Editor");
			return editor;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			// delete by adding to the delete list
			Collection<SchedulerSchedule> deleteList = ((SchedulerFactory)t.getObjectFactory(SchedulerSchedule.class)).getDeleteList(SchedulerSchedule.class);
			deleteList.add(this);
		}

		@Override
		public Object get(String key) {
			try {
				switch (key) {
				case "name":
					return getName();
				case "description":
					return getDescription();
				case "task":
					return getSchedulerTask();
				case "taskId":
					return getSchedulerTask().getId();
				case "active":
					return isActive();
				case "notificationEmail":
					return getNotificationEmail();
				case "cdate":
					return getCDate().getIntTimestamp();
				case "edate":
					return getEDate().getIntTimestamp();
				case "creator":
					return getCreator();
				case "editor":
					return getEditor();
				default:
					return super.get(key);
				}
			} catch (NodeException e) {
				return null;
			}
		}

		@Override
		public boolean shouldExecute() throws NodeException {
			// do not execute, if inactive
			if (!active) {
				return false;
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			int timestamp = t.getUnixTimestamp();

			// do not execute before the given start timestamp (if any)
			if (scheduleData.getStartTimestamp() > 0 && scheduleData.getStartTimestamp() > timestamp) {
				return false;
			}

			// do not execute after the given end timestamp (if any)
			if (scheduleData.getEndTimestamp() > 0 && scheduleData.getEndTimestamp() < timestamp) {
				return false;
			}

			// get the last execution (if any)
			ExecutionModel execution = DBUtils.select(
					"SELECT e.* FROM scheduler_schedule s JOIN scheduler_execution e ON s.scheduler_execution_id = e.id WHERE s.id = ?",
					ps -> ps.setInt(1, getId()),
					rs -> rs.next() ? ExecutionModel.fromDbResult(rs) : null);

			// do not execute, if currently running
			if (execution != null && execution.isRunning()) {
				return false;
			}

			switch (scheduleData.getType()) {
			case followup: {
				ScheduleFollow follow = scheduleData.getFollow();
				Set<Integer> followIds = follow.getScheduleId();

				if (followIds.isEmpty()) {
					logger.warn(String.format(
						"Schedule %d has schedule type \"follow\", but no follow IDs are set; schedule can never be executed",
						id));

					return false;
				}

				String query =
					"SELECT e.result " +
						"FROM scheduler_schedule s " +
							"JOIN scheduler_execution e ON s.scheduler_execution_id = e.id " +
						"WHERE s.id IN (%s) " +
							"AND e.starttime > ? " +
							"AND e.endtime > 0";
				Boolean lastRunResult = DBUtils.select(
					String.format(query, String.join(", ", Collections.nCopies(followIds.size(), "?"))),
					stmt -> {
						int count = 1;

						for (Integer followId : followIds) {
							stmt.setInt(count++, followId);
						}

						stmt.setInt(count, execution == null ? 0 : execution.getStartTime());
					},
					rs -> {
						Boolean result = null;

						while (rs.next() && (result == null || !result)) {
							result = rs.getInt("result") == 0;
						}

						return result;
					});

				if (lastRunResult == null) {
					return false;
				}

				return !follow.isOnlyAfterSuccess() || lastRunResult;
			}
			case interval:
				// execute, if not executed before
				if (execution == null) {
					return true;
				}
				ScheduleInterval interval = scheduleData.getInterval();
				// do not execute, if no interval is given
				if (interval == null) {
					return false;
				}

				return interval.isDue(scheduleData.getStartTimestamp(), execution.getStartTime(), timestamp, ZoneId.systemDefault());
			case manual:
				// schedule is only executed manually
				return false;
			case once:
				// Execute if it has never been executed before, or if the schedule was modified since the last execution.
				return execution == null || execution.getEndTime() < getEDate().getIntTimestamp();
			default:
				return false;
			}
		}

		@Override
		public String toString() {
			try (Trx trx = new Trx()) {
				String typeDesc;
				ScheduleData data = getScheduleData();

				switch (data.getType()) {
				case interval:
					typeDesc = String.format(
						"interval (every %d %s)",
						data.getInterval().getValue(),
						data.getInterval().getUnit().name());

					break;

				case followup:
					typeDesc = String.format(
						"followup (%s; %s)",
						data.getFollow().isOnlyAfterSuccess() ? "after success" : "always",
						data.getFollow().getScheduleId().stream().map(Object::toString).collect(Collectors.joining(", ")));

					break;

				default:
					typeDesc = data.getType().name();
				}

				return String.format(
					"SchedulerSchedule {id: %d, start: %d, end: %d, type: %s, %stask: %s}",
					getId(),
					data.getStartTimestamp(),
					data.getStartTimestamp(),
					typeDesc,
					getSchedulerTask().isInternal() ? "internal " : "",
					getSchedulerTask().getCommand());
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		}


	}

	/**
	 * Editable factory implementation of {@link SchedulerSchedule}
	 */
	private static class EditableFactorySchedulerSchedule extends FactorySchedulerSchedule {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 5965534384958178032L;

		/**
		 * Flag to mark whether the object has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Create an empty instance of an editable value
		 * @param info
		 */
		public EditableFactorySchedulerSchedule(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor to create a copy of the given object
		 * @param schedulerSchedule task to copy
		 * @param info info about the copy
		 * @param asNew true when the object shall be a new object, false for just the editable version of the object
		 */
		public EditableFactorySchedulerSchedule(SchedulerSchedule schedulerSchedule, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : schedulerSchedule.getId(), info, getDataMap(schedulerSchedule));
			if (asNew) {
				this.modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public void setDescription(String description) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.description, description)) {
				this.description = description;
				this.modified = true;
			}
		}

		@Override
		public void setActive(boolean active) throws ReadOnlyException {
			if (this.active != active) {
				this.active = active;
				this.modified = true;
			}
		}

		@Override
		public void setNotificationEmail(List<String> notificationEmail) throws ReadOnlyException {
			if (!Objects.deepEquals(this.notificationEmail, notificationEmail)) {
				this.notificationEmail = notificationEmail;
				this.modified = true;
			}
		}

		@Override
		public void setParallel(boolean parallel) {
			if (this.parallel != parallel) {
				this.parallel = parallel;
				this.modified = true;
			}
		}

		@Override
		public void setSchedulerTask(SchedulerTask schedulerTask) throws ReadOnlyException {
			if (schedulerTask != null) {
				int schedulerTaskId = schedulerTask.getId();
				if (this.schedulerTaskId != schedulerTaskId) {
					this.schedulerTaskId = schedulerTaskId;
					this.modified = true;
				}
			}
		}

		@Override
		public void setScheduleData(ScheduleData scheduleData) throws ReadOnlyException {
			if (scheduleData != null) {
				if (!Objects.deepEquals(this.scheduleData, scheduleData)) {
					this.scheduleData = scheduleData;
					this.modified = true;
				}
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = false;
			boolean isNew = isEmptyId(getId());
			SchedulerSchedule original = null;

			if (!isNew) {
				original = t.getObject(SchedulerSchedule.class, getId());
			}

			name = ObjectTransformer.getString(name, "");
			description = ObjectTransformer.getString(description, "");

			// make the name unique
			setName(UniquifyHelper.makeUnique(name, MAX_NAME_LENGTH,
					"SELECT name FROM scheduler_schedule WHERE id != ? AND name = ?", SeparatorType.blank,
					ObjectTransformer.getInt(getId(), -1)));

			if (modified) {
				// object is modified, so update it
				isModified = true;

				// set creator data for new objects
				if (isNew) {
					creatorId = t.getUserId();
					cDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				// set the editor data
				editorId = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());

				saveFactoryObject(this);
				modified = false;
			}

			// logcmd, versions and trigger event
			if (isModified) {
				List<String> modifiedData = new ArrayList<>();

				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, getId(), 0, "SchedulerSchedule.create");
					t.addTransactional(new TransactionalTriggerEvent(SchedulerSchedule.class, getId(), null, Events.CREATE));
				} else {
					String[] mod = getModifiedData(original, this);
					modifiedData.addAll(Arrays.asList(mod));

					ActionLogger.logCmd(ActionLogger.EDIT, SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, getId(), 0, "SchedulerSchedule.update");
					t.addTransactional(new TransactionalTriggerEvent(SchedulerSchedule.class, getId(), mod, Events.UPDATE));
				}

				t.dirtObjectCache(SchedulerSchedule.class, getId());
			}

			return isModified;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (SchedulerTask.class.equals(clazz)) {
			return (T) new EditableFactorySchedulerTask(handle.createObjectInfo(SchedulerTask.class, true));
		} else if (SchedulerSchedule.class.equals(clazz)) {
			return (T) new EditableFactorySchedulerSchedule(handle.createObjectInfo(SchedulerSchedule.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs,
			List<Integer>[] idLists) throws SQLException, NodeException {
		if (SchedulerTask.class.equals(clazz)) {
			return (T) new FactorySchedulerTask(id, info, rs.getValues());
		} else if (SchedulerSchedule.class.equals(clazz)) {
			return (T) new FactorySchedulerSchedule(id, info, rs.getValues());
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info)
			throws NodeException, ReadOnlyException {
		if (object instanceof SchedulerTask) {
			return (T) new EditableFactorySchedulerTask((SchedulerTask) object, info, false);
		} else if (object instanceof SchedulerSchedule) {
			return (T) new EditableFactorySchedulerSchedule((SchedulerSchedule) object, info, false);
		} else {
			return null;
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, SchedulerSchedule.class)) {
			Collection<SchedulerSchedule> deleted = getDeleteList(SchedulerSchedule.class);

			for (SchedulerSchedule schedule : deleted) {
				// add logcmd
				ActionLogger.logCmd(ActionLogger.DEL, SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, schedule.getId(), 0, "SchedulerSchedule.delete");
			}

			// delete the schedules
			flushDelete("DELETE FROM scheduler_schedule WHERE id IN", SchedulerSchedule.class);
		}

		if (!isEmptyDeleteList(t, SchedulerTask.class)) {
			Collection<SchedulerTask> deleted = getDeleteList(SchedulerTask.class);

			for (SchedulerTask task : deleted) {
				// add logcmd
				ActionLogger.logCmd(ActionLogger.DEL, SchedulerTask.TYPE_SCHEDULER_TASK, task.getId(), 0, "SchedulerTask.delete");
			}

			// delete the tasks
			flushDelete("DELETE FROM scheduler_task WHERE id IN", SchedulerTask.class);
		}
	}
}
