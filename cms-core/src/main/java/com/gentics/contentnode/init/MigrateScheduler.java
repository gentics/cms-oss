package com.gentics.contentnode.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.scheduler.IntervalUnit;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleFollow;
import com.gentics.contentnode.rest.model.scheduler.ScheduleInterval;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.scheduler.InternalSchedulerTask;

import de.ailis.pherialize.Mixed;
import de.ailis.pherialize.MixedArray;
import de.ailis.pherialize.Pherialize;

/**
 * Init Job that migrates tasks to scheduler_tasks, jobs to scheduler_schedules and jobruns to scheduler_executions.
 * Old tables will not be removed (along with the old UI), so that customers can still view the old scheduler (and compare with the new scheduler jobs)
 */
public class MigrateScheduler extends InitJob {
	/**
	 * Pattern for command parameters
	 */
	public final static Pattern COMMAND_PARAM_PATTERN = Pattern.compile("%([^%]+)%");

	/**
	 * Pattern for commands of internal tasks
	 */
	public final static Pattern INTERNAL_TASK_COMMAND_PATTERN = Pattern.compile(".*/Node/\\.node/sh\\.php do=827 cmd=(?<cmd>[\\w]+).*");

	/**
	 * Parameter type "node"
	 */
	public final static int PARAM_TYPE_NODE = 1;

	/**
	 * Parameter type "folder"
	 */
	public final static int PARAM_TYPE_FOLDER = 2;

	/**
	 * Parameter type "text"
	 */
	public final static int PARAM_TYPE_TEXT = 3;

	/**
	 * Parameter type "page"
	 */
	public final static int PARAM_TYPE_PAGE = 4;

	/**
	 * Find the next position of the separator in the string (this method has been ported from PHP)
	 * @param string string to search
	 * @param separator separator to find
	 * @param start position to start
	 * @return found position (-1, if not found)
	 */
	protected static int findNonDupSeparator(String string, String separator, int start) {
		int pos = string.indexOf(separator, start);
		while (pos >= 0) {
			if (pos == 0) {
				return pos;
			}
			if (!StringUtils.equals(string.substring(pos - 1, pos), "\\")) {
				return pos;
			} else {
				pos = string.indexOf(separator, pos + 2);
			}
		}

		return pos;
	}

	/**
	 * Find the command parameters in the command (this methos has been ported from PHP)
	 * @param command command
	 * @return map of parameter names and optional the set of properties
	 */
	protected static Map<String, Set<String>> findCommandParams(String command) {
		Map<String, Set<String>> params = new HashMap<>();

		int start = findNonDupSeparator(command, "%", 0);
		int end = -1;
		while (start >= 0) {
			end = findNonDupSeparator(command, "%", start + 1);
			if (end == -1) {
				return params;
			}

			String p = command.substring(start + 1, end);
			int pos = p.indexOf(".");
			String name = null;
			String property = null;
			if (pos >= 0) {
				name = p.substring(0, pos);
				property = p.substring(pos + 1);
			} else {
				name = p;
				property = "";
			}
			Set<String> properties = params.computeIfAbsent(name, key -> new HashSet<>());
			if (!StringUtils.isBlank(property)) {
				properties.add(property);
			}

			start = findNonDupSeparator(command, "%", end + 1);
		}

		return params;
	}

	@Override
	public void execute() throws NodeException {
		// TODO optionally prevent "double migration"

		// suspend the scheduler
		SchedulerFactory factory = (SchedulerFactory) ContentNodeFactory.getInstance().getFactory().getObjectFactory(SchedulerSchedule.class);
		factory.suspend(null);
		TransactionManager.getCurrentTransaction().commit(false);

		// migrate tasks
		Map<Integer, Integer> taskMigrationMap = migrateTasks();

		// migrate jobs
		Map<Integer, Integer> jobMigrationMap = migrateJobs(taskMigrationMap);

		// migrate the jobruns
		migrateJobRuns(jobMigrationMap);
	}

	/**
	 * Migrate all tasks
	 * @return map of oldTaskId -> newTaskId
	 * @throws NodeException
	 */
	protected Map<Integer, Integer> migrateTasks() throws NodeException {
		// read all old tasks
		List<OldTask> oldTasks = DBUtils.select("SELECT task.*, tasktemplate.command FROM task LEFT JOIN tasktemplate ON task.tasktemplate_id = tasktemplate.id WHERE task.migrated = false", rs -> {
			List<OldTask> tasks = new ArrayList<>();
			while (rs.next()) {
				tasks.add(new OldTask(rs));
			}
			return tasks;
		});

		Map<Integer, Integer> taskMigrationMap = new HashMap<>();
		try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, null, false, false)) {
			for (OldTask oldTask : oldTasks) {
				// attach parameters
				oldTask.attachParameters();

				// migrate task
				taskMigrationMap.put(oldTask.id, migrate(oldTask));
			}
		}

		return taskMigrationMap;
	}

	/**
	 * Migrate all jobs
	 * @param taskMigrationMap map of oldTaskId -> newTaskId
	 * @return map of oldJobId -> newScheduleId
	 * @throws NodeException
	 */
	protected Map<Integer, Integer> migrateJobs(Map<Integer, Integer> taskMigrationMap) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<OldJob> oldJobs = DBUtils.select("SELECT * FROM job WHERE migrated = false", rs -> {
			List<OldJob> jobs = new ArrayList<>();
			while (rs.next()) {
				jobs.add(new OldJob(rs));
			}
			return jobs;
		});

		Map<Integer, Integer> jobMigrationMap = new HashMap<>();
		Map<Integer, Set<Integer>> followUpJobs = new HashMap<>();
		for (OldJob oldJob : oldJobs) {
			jobMigrationMap.put(oldJob.id, migrate(oldJob, taskMigrationMap, followUpJobs));
		}

		// migrate possible followup job IDs
		for (Map.Entry<Integer, Set<Integer>> entry : followUpJobs.entrySet()) {
			Integer oldJobId = entry.getKey();
			Set<Integer> oldFollowedJobsIds = entry.getValue();

			Set<Integer> newFollowedScheduleIds = oldFollowedJobsIds.stream().map(oldId -> jobMigrationMap.get(oldId))
					.filter(newId -> newId != null).collect(Collectors.toSet());

			SchedulerSchedule schedule = t.getObject(SchedulerSchedule.class, jobMigrationMap.get(oldJobId), true);
			if (schedule != null && schedule.getScheduleData().getType() == ScheduleType.followup) {
				schedule.getScheduleData().getFollow().setScheduleId(newFollowedScheduleIds);
				schedule.save();
				t.commit(false);
			}
		}

		return jobMigrationMap;
	}

	/**
	 * Migrate all jobruns
	 * @param jobMigrationMap map of oldJobId -> newScheduleId
	 * @throws NodeException
	 */
	protected void migrateJobRuns(Map<Integer, Integer> jobMigrationMap) throws NodeException {
		List<OldJobRun> oldJobRuns = DBUtils.select("SELECT * FROM jobrun WHERE migrated = false", rs -> {
			List<OldJobRun> jobRuns = new ArrayList<>();
			while (rs.next()) {
				jobRuns.add(new OldJobRun(rs));
			}
			return jobRuns;
		});

		Map<Integer, Integer> jobRunMigrationMap = new HashMap<>();
		for (OldJobRun oldJobRun : oldJobRuns) {
			jobRunMigrationMap.put(oldJobRun.id, migrate(oldJobRun, jobMigrationMap));
		}

		// finally update the last execution IDs
		List<OldJob> oldJobs = DBUtils.select("SELECT * FROM job", rs -> {
			List<OldJob> jobs = new ArrayList<>();
			while (rs.next()) {
				jobs.add(new OldJob(rs));
			}
			return jobs;
		});

		for (OldJob oldJob : oldJobs) {
			int scheduleId = jobMigrationMap.getOrDefault(oldJob.id, 0);
			int executionId = jobRunMigrationMap.getOrDefault(oldJob.lastValidJobrunId, 0);
			if (scheduleId > 0 && executionId > 0) {
				DBUtils.update("UPDATE scheduler_schedule SET scheduler_execution_id = ? WHERE id = ?", executionId, scheduleId);
			}
		}
		TransactionManager.getCurrentTransaction().commit(false);
	}

	/**
	 * Migrate the task to a scheduler_task
	 * @param oldTask task to migrate
	 * @return ID of the migrated scheduler_task
	 * @throws NodeException
	 */
	protected int migrate(OldTask oldTask) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String parsedCommand = oldTask.getParsedCommand();
		Matcher matcher = INTERNAL_TASK_COMMAND_PATTERN.matcher(parsedCommand);
		int newTaskId = 0;
		boolean updateCreatorAndEditor = false;
		if (matcher.matches()) {
			// migrate to internal task
			String cmd = matcher.group("cmd");
			InternalSchedulerTask internalTaskCmd = SchedulerFactory.getInternalSchedulerTask(cmd);
			if (internalTaskCmd != null) {
				SchedulerTask newTask = t.getObject(SchedulerTask.class , DBUtils.select("SELECT id FROM scheduler_task WHERE internal = ? AND command = ?", ps -> {
					ps.setBoolean(1, true);
					ps.setString(2, internalTaskCmd.getCommand());
				}, DBUtils.firstInt("id")));

				if (newTask == null) {
					newTask = t.createObject(SchedulerTask.class);
					newTask.setInternal(true);
					newTask.setName(oldTask.name);
					newTask.setCommand(internalTaskCmd.getCommand());
					newTask.save();

					updateCreatorAndEditor = true;
				}
				newTaskId = newTask.getId();
			}
		} else {
			// migrate to external task
			SchedulerTask newTask = t.createObject(SchedulerTask.class);
			newTask.setInternal(false);
			newTask.setName(oldTask.name);
			newTask.setCommand(parsedCommand);
			newTask.save();
			newTaskId = newTask.getId();
			updateCreatorAndEditor = true;
		}

		if (updateCreatorAndEditor) {
			// update creation and editing user/time
			DBUtils.update("UPDATE scheduler_task SET creator = ?, cdate = ?, editor = ?, edate = ? WHERE id = ?", oldTask.creatorId, oldTask.cdate, oldTask.editorId, oldTask.edate, newTaskId);
			t.dirtObjectCache(SchedulerTask.class, newTaskId);
		}

		// migrate permissions
		DBUtils.update("INSERT IGNORE INTO perm (o_type, o_id, usergroup_id, perm) SELECT 160, ?, usergroup_id, perm FROM perm WHERE o_type = 37 and o_id = ?", newTaskId, oldTask.id); 

		DBUtils.update("UPDATE task SET migrated = true WHERE id = ?", oldTask.id);

		t.commit(false);

		return newTaskId;
	}

	/**
	 * Migrate the job to a scheduler_schedule
	 * @param oldJob job to migrate
	 * @param taskMigrationMap map of oldTaskId -> newTaskId
	 * @param followUpJobs map, which will be filled with jobIds for followup jobs
	 * @return ID of the migrated scheduler_schedule
	 * @throws NodeException
	 */
	protected int migrate(OldJob oldJob, Map<Integer, Integer> taskMigrationMap, Map<Integer, Set<Integer>> followUpJobs) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int newJobId = 0;

		SchedulerSchedule newSchedule = t.createObject(SchedulerSchedule.class);
		newSchedule.setName(oldJob.name);
		newSchedule.setDescription(oldJob.description);
		newSchedule.setActive(oldJob.active);
		newSchedule.setParallel(oldJob.parallel);
		if (!StringUtils.isBlank(oldJob.failedEmails)) {
			String[] eMails = StringUtils.split(oldJob.failedEmails, ",");
			for (int i = 0; i < eMails.length; i++) {
				eMails[i] = StringUtils.trim(eMails[i]);
			}
			newSchedule.setNotificationEmail(Arrays.asList(eMails));
		}

		ScheduleData scheduleData = new ScheduleData();
		scheduleData.setType(oldJob.type);

		if (oldJob.scheduleData != null) {
			if (oldJob.scheduleData.isArray()) {
				MixedArray dataArray = oldJob.scheduleData.toArray();
				if (dataArray.containsKey("start")) {
					scheduleData.setStartTimestamp(dataArray.getInt("start"));
				} else if (dataArray.containsKey("timeStamp")) {
					scheduleData.setStartTimestamp(dataArray.getInt("timeStamp"));
				}
				if (dataArray.containsKey("end")) {
					scheduleData.setEndTimestamp(dataArray.getInt("end"));
				}
				switch(scheduleData.getType()) {
				case followup:
					ScheduleFollow follow = new ScheduleFollow();
					follow.setOnlyAfterSuccess(ObjectTransformer.getBoolean(dataArray.get("onSuccess"), false));
					scheduleData.setFollow(follow);

					// the IDs of the jobs to follow will be stored in the map followUpJobs (because we possibly do not know the new IDs yet)
					MixedArray jobs = dataArray.getArray("jobs");
					Set<Integer> jobIdSet = new HashSet<>();
					for (Object id : jobs.values()) {
						jobIdSet.add(ObjectTransformer.getInteger(id, 0));
					}
					followUpJobs.put(oldJob.id, jobIdSet);
					break;
				case interval:
					ScheduleInterval interval = new ScheduleInterval();
					String scale = dataArray.getString("scale");
					switch (scale) {
					case "min":
						interval.setUnit(IntervalUnit.minute);
						break;
					case "std":
						interval.setUnit(IntervalUnit.hour);
						break;
					case "day":
						interval.setUnit(IntervalUnit.day);
						break;
					}
					interval.setValue(dataArray.getInt("every"));
					scheduleData.setInterval(interval);
					break;
				case manual:
					break;
				case once:
					break;
				}
			}
		}

		newSchedule.setScheduleData(scheduleData);
		newSchedule.setSchedulerTask(t.getObject(SchedulerTask.class, taskMigrationMap.get(oldJob.taskId)));
		newSchedule.save();
		newJobId = newSchedule.getId();

		// update creation and editing user/time and statistic data
		DBUtils.update(
				"UPDATE scheduler_schedule SET creator = ?, cdate = ?, editor = ?, edate = ?, runs = ?, average_time = ? WHERE id = ?",
				oldJob.creatorId, oldJob.cdate, oldJob.editorId, oldJob.edate, oldJob.jobRunCount, oldJob.jobRunAverage,
				newJobId);
		t.dirtObjectCache(SchedulerSchedule.class, newJobId);

		// migrate permissions
		DBUtils.update("INSERT IGNORE INTO perm (o_type, o_id, usergroup_id, perm) SELECT 161, ?, usergroup_id, perm FROM perm WHERE o_type = 39 and o_id = ?", newJobId, oldJob.id); 

		DBUtils.update("UPDATE job SET migrated = true WHERE id = ?", oldJob.id);

		t.commit(false);

		return newJobId;
	}

	/**
	 * Migrate the job run
	 * @param oldJobRun jobrun to migrate
	 * @param jobMigrationMap map of oldJobId -> newScheduleId
	 * @return ID of the migrated scheduler_execution
	 * @throws NodeException
	 */
	protected int migrate(OldJobRun oldJobRun, Map<Integer, Integer> jobMigrationMap) throws NodeException {
		List<Integer> ids = DBUtils.executeInsert(
				"INSERT INTO scheduler_execution (scheduler_schedule_id, starttime, endtime, duration, result, log) VALUES (?, ?, ?, ?, ?, ?)",
				new Object[] { jobMigrationMap.get(oldJobRun.jobId), oldJobRun.starttime, oldJobRun.endtime, oldJobRun.endtime - oldJobRun.starttime, oldJobRun.returnvalue, oldJobRun.output });

		DBUtils.update("UPDATE jobrun SET migrated = true WHERE id = ?", oldJobRun.id);

		return ids.get(0);
	}

	/**
	 * Class for old tasks
	 */
	protected static class OldTask {
		protected int id;

		protected int taskTemplateId;

		protected String name;

		protected String command;

		protected int creatorId;

		protected int cdate;

		protected int editorId;

		protected int edate;

		protected Map<String, OldTaskParam> params;

		/**
		 * Create an instance filled with data from the current result row
		 * @param rs result set
		 * @throws SQLException
		 */
		public OldTask(ResultSet rs) throws SQLException {
			this.id = rs.getInt("id");
			this.taskTemplateId = rs.getInt("tasktemplate_id");
			this.name = rs.getString("name");
			this.command = rs.getString("command");
			this.creatorId = rs.getInt("creator");
			this.cdate = rs.getInt("cdate");
			this.editorId = rs.getInt("editor");
			this.edate = rs.getInt("edate");
		}

		/**
		 * Attach the command parameters
		 * @throws NodeException
		 */
		public void attachParameters() throws NodeException {
			this.params = DBUtils.select(
					"SELECT value, paramtype, tasktemplateparam.name, taskparam.name pname FROM taskparam JOIN tasktemplateparam ON taskparam.templateparam_id = tasktemplateparam.id WHERE task_id = ?",
					ps -> {
						ps.setInt(1, id);
					}, rs -> {
						Map<String, OldTaskParam> params = new HashMap<>();
						while (rs.next()) {
							String name = rs.getString("name");
							params.computeIfAbsent(name, key -> new OldTaskParam()).setData(rs);
						}
						return params;
					});
		}

		/**
		 * Get the parsed command
		 * @return parsed command
		 * @throws NodeException
		 */
		public String getParsedCommand() throws NodeException {
			Map<String, Set<String>> pMap = findCommandParams(command);
			String parsedCommand = command;

			for (Map.Entry<String, Set<String>> entry : pMap.entrySet()) {
				String pName = entry.getKey();
				Set<String> pProperties = entry.getValue();
				OldTaskParam param = params.get(pName);

				if (param != null) {
					if (pProperties.isEmpty()) {
						parsedCommand = parsedCommand.replace("%" + pName + "%", param.getValue(null));
					} else {
						for (String pProp : pProperties) {
							String fullName = pName + "." + pProp;
							parsedCommand = parsedCommand.replace("%" + fullName + "%", param.getValue(pProp));
						}
					}
				}
			}

			return parsedCommand;
		}
	}

	/**
	 * Class for old task parameter
	 */
	protected static class OldTaskParam {
		protected int type;

		protected String name;

		protected String text;

		protected int oType;

		protected int oId;

		protected int objType;

		protected int objId;

		/**
		 * Set the data from the current result row
		 * @param rs result set
		 * @throws SQLException
		 */
		protected void setData(ResultSet rs) throws SQLException {
			this.type = rs.getInt("paramtype");
			this.name = rs.getString("name");

			String value = rs.getString("value");

			if (value != null) {
				switch (type) {
				case PARAM_TYPE_NODE:
				case PARAM_TYPE_FOLDER:
				case PARAM_TYPE_PAGE:
					String pName = rs.getString("pname");
					if ("c.u_type".equals(pName)) {
						oType = Integer.parseInt(value);
					} else if ("c.u_id".equals(pName)) {
						oId = Integer.parseInt(value);
					} else if ("c.u_obj_type".equals(pName)) {
						objType = Integer.parseInt(value);
					} else if ("c.u_obj_id".equals(pName)) {
						objId = Integer.parseInt(value);
					}
					break;
				case PARAM_TYPE_TEXT:
					text = value;
					break;
				}
			}
		}

		/**
		 * Get the value of the parameter (optionally resolve the given property)
		 * @param property optional property
		 * @return parameter value
		 * @throws NodeException
		 */
		protected String getValue(String property) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Resolvable nodeObject = null;
			switch (type) {
			case PARAM_TYPE_NODE:
				Folder folder = t.getObject(Folder.class, oId);
				if (folder != null) {
					nodeObject = folder.getNode();
				}
				break;
			case PARAM_TYPE_FOLDER:
				nodeObject = t.getObject(Folder.class, oId);
				break;
			case PARAM_TYPE_PAGE:
				nodeObject = t.getObject(Page.class, objId);
				break;
			case PARAM_TYPE_TEXT:
				return text;
			}

			if (nodeObject != null) {
				if (StringUtils.isBlank(property)) {
					property = "name";
				}
				return ObjectTransformer.getString(PropertyResolver.resolve(nodeObject, property), "");
			}

			return "";
		}
	}

	/**
	 * Class for old job
	 */
	protected static class OldJob {
		protected int id;

		protected String name;

		protected String description;

		protected int taskId;

		protected ScheduleType type;

		protected Mixed scheduleData;

		protected boolean active;

		protected int creatorId;

		protected int cdate;

		protected int editorId;

		protected int edate;

		protected boolean parallel;

		protected String failedEmails;

		protected int lastValidJobrunId;

		protected int jobRunCount;

		protected int jobRunAverage;

		/**
		 * Create an instance filled with data from the current result row
		 * @param rs result set
		 * @throws SQLException
		 */
		public OldJob(ResultSet rs) throws SQLException {
			id = rs.getInt("id");
			name = rs.getString("name");
			description = rs.getString("description");
			taskId = rs.getInt("task_id");
			switch (rs.getString("schedule_type")) {
			case "time":
				type = ScheduleType.once;
				break;
			case "int":
				type = ScheduleType.interval;
				break;
			case "followup":
				type = ScheduleType.followup;
				break;
			case "manual":
				type = ScheduleType.manual;
				break;
			}
			Mixed data = Pherialize.unserialize(rs.getString("schedule_data"));
			if (data.isArray()) {
				MixedArray arrayData = data.toArray();
				if (arrayData.containsKey("s")) {
					scheduleData = Pherialize.unserialize(arrayData.getString("s"));
				}
			}
			active = rs.getBoolean("status");
			creatorId = rs.getInt("creator");
			cdate = rs.getInt("cdate");
			editorId = rs.getInt("editor");
			edate = rs.getInt("edate");
			parallel = rs.getBoolean("parallel");
			failedEmails = rs.getString("failedemails");
			lastValidJobrunId = rs.getInt("last_valid_jobrun_id");
			jobRunCount = rs.getInt("jobruncount");
			jobRunAverage = rs.getInt("jobrunaverage");
		}
	}

	/**
	 * Class for old jobrun
	 */
	protected static class OldJobRun {
		protected int id;

		protected int jobId;

		protected int starttime;

		protected int endtime;

		protected int returnvalue;

		protected boolean valid;

		protected String output;

		/**
		 * Create instance filled with data from the current result row
		 * @param rs result set
		 * @throws SQLException
		 */
		public OldJobRun(ResultSet rs) throws SQLException {
			id = rs.getInt("id");
			jobId = rs.getInt("job_id");
			starttime = rs.getInt("starttime");
			endtime = rs.getInt("endtime");
			returnvalue = rs.getInt("returnvalue");
			valid = rs.getBoolean("valid");
			output = rs.getString("output");
		}
	}
}
