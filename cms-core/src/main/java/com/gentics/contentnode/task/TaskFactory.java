package com.gentics.contentnode.task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.rest.model.task.TaskObject;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;

/**
 * Database access for stored tasks
 */
public final class TaskFactory {
	/**
	 * Table name (<code>task</code> is used by the scheduler)
	 */
	final static String TABLE_NAME = "user_task";

	private final static String COLUMNS = "source, type, title, description, status, due_at, assignee_user_id, "
			+ "assignee_group_id, origin_group_id, creator_id, creator_name, cdate, edate, completed_at, objects, details, "
			+ "external_key, genaix_session_id, genaix_plan_id, genaix_instruction";

	private final static String SELECT_CLAUSE = "SELECT id, %s FROM %s".formatted(COLUMNS, TABLE_NAME);

	private final static String INSERT_SQL = "INSERT INTO %s (%s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			.formatted(TABLE_NAME, COLUMNS);

	private final static String UPDATE_SQL = ("UPDATE %s SET type = ?, title = ?, description = ?, status = ?, due_at = ?, "
			+ "assignee_user_id = ?, assignee_group_id = ?, origin_group_id = ?, edate = ?, completed_at = ?, objects = ?, "
			+ "details = ?, genaix_session_id = ?, genaix_plan_id = ?, genaix_instruction = ? WHERE id = ?").formatted(TABLE_NAME);

	private final static ObjectMapper MAPPER = new ObjectMapper();

	private final static DBUtils.HandleSelectResultSet<StoredTask> ROW_HANDLER = rs -> {
		StoredTask task = new StoredTask();
		task.id = rs.getInt("id");
		task.source = TaskSource.valueOf(rs.getString("source"));
		task.type = rs.getString("type");
		task.title = rs.getString("title");
		task.description = rs.getString("description");
		task.status = TaskStatus.valueOf(rs.getString("status"));
		task.dueAt = getInteger(rs, "due_at");
		task.assigneeUserId = getInteger(rs, "assignee_user_id");
		task.assigneeGroupId = getInteger(rs, "assignee_group_id");
		task.originGroupId = getInteger(rs, "origin_group_id");
		task.creatorId = getInteger(rs, "creator_id");
		task.creatorName = rs.getString("creator_name");
		task.cdate = rs.getInt("cdate");
		task.edate = rs.getInt("edate");
		task.completedAt = getInteger(rs, "completed_at");
		task.objects = fromJson(rs.getString("objects"), new TypeReference<List<TaskObject>>() {});
		task.details = fromJson(rs.getString("details"), new TypeReference<Map<String, Object>>() {});
		task.externalKey = rs.getString("external_key");
		task.genaixSessionId = rs.getString("genaix_session_id");
		task.genaixPlanId = rs.getString("genaix_plan_id");
		task.genaixInstruction = rs.getString("genaix_instruction");
		return task;
	};

	private TaskFactory() {
	}

	/**
	 * Load a stored task
	 * @param id task ID
	 * @return optional task
	 * @throws NodeException
	 */
	public static Optional<StoredTask> get(int id) throws NodeException {
		return DBUtils.select(SELECT_CLAUSE + " WHERE id = ?", pst -> pst.setInt(1, id), DBUtils.getFirst(ROW_HANDLER));
	}

	/**
	 * Load the task with the given external key
	 * @param source source
	 * @param externalKey external key
	 * @return optional task
	 * @throws NodeException
	 */
	public static Optional<StoredTask> getByExternalKey(TaskSource source, String externalKey) throws NodeException {
		return DBUtils.select(SELECT_CLAUSE + " WHERE source = ? AND external_key = ?", pst -> {
			pst.setString(1, source.name());
			pst.setString(2, externalKey);
		}, DBUtils.getFirst(ROW_HANDLER));
	}

	/**
	 * Load the tasks of the user and the groups, including claimed group tasks
	 * @param userId user ID
	 * @param groupIds IDs of the user's groups
	 * @return list of tasks
	 * @throws NodeException
	 */
	public static List<StoredTask> listForRecipient(int userId, Collection<Integer> groupIds) throws NodeException {
		if (groupIds.isEmpty()) {
			return DBUtils.select(SELECT_CLAUSE + " WHERE assignee_user_id = ?", pst -> pst.setInt(1, userId),
					DBUtils.getAll(ROW_HANDLER));
		}
		String in = groupIds.stream().map(id -> "?").collect(Collectors.joining(", "));
		List<Integer> groups = new ArrayList<>(groupIds);
		return DBUtils.select(SELECT_CLAUSE + " WHERE assignee_user_id = ? OR assignee_group_id IN (%s) OR origin_group_id IN (%s)"
				.formatted(in, in), pst -> {
					int index = 1;
					pst.setInt(index++, userId);
					for (int round = 0; round < 2; round++) {
						for (int groupId : groups) {
							pst.setInt(index++, groupId);
						}
					}
				}, DBUtils.getAll(ROW_HANDLER));
	}

	/**
	 * Load the tasks the user created
	 * @param userId user ID
	 * @return list of tasks
	 * @throws NodeException
	 */
	public static List<StoredTask> listCreatedBy(int userId) throws NodeException {
		return DBUtils.select(SELECT_CLAUSE + " WHERE creator_id = ?", pst -> pst.setInt(1, userId), DBUtils.getAll(ROW_HANDLER));
	}

	/**
	 * Insert a new task
	 * @param task task, its ID is set
	 * @throws NodeException
	 */
	public static void insert(StoredTask task) throws NodeException {
		List<Integer> ids = DBUtils.executeInsert(INSERT_SQL,
				new Object[] { task.source.name(), task.type, task.title, task.description, task.status.name(), task.dueAt,
						task.assigneeUserId, task.assigneeGroupId, task.originGroupId, task.creatorId, task.creatorName,
						task.cdate, task.edate, task.completedAt, toJson(task.objects), toJson(task.details),
						task.externalKey, task.genaixSessionId, task.genaixPlanId, task.genaixInstruction });
		if (ids.size() != 1) {
			throw new NodeException("Error while creating task. Unexpected number of inserts: %d".formatted(ids.size()));
		}
		task.id = ids.get(0);
	}

	/**
	 * Write the changeable fields of a task
	 * @param task task
	 * @throws NodeException
	 */
	public static void update(StoredTask task) throws NodeException {
		DBUtils.executeUpdate(UPDATE_SQL,
				new Object[] { task.type, task.title, task.description, task.status.name(), task.dueAt, task.assigneeUserId,
						task.assigneeGroupId, task.originGroupId, task.edate, task.completedAt, toJson(task.objects),
						toJson(task.details), task.genaixSessionId, task.genaixPlanId, task.genaixInstruction, task.id });
	}

	/**
	 * Delete a task
	 * @param id task ID
	 * @throws NodeException
	 */
	public static void delete(int id) throws NodeException {
		DBUtils.update("DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME), id);
	}

	private static Integer getInteger(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

	private static String toJson(Object value) throws NodeException {
		if (value == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new NodeException("Error while serializing task data", e);
		}
	}

	private static <T> T fromJson(String json, TypeReference<T> type) throws SQLException {
		if (json == null) {
			return null;
		}
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new SQLException("Error while reading task data", e);
		}
	}

	/**
	 * Row of the table <code>user_task</code>
	 */
	public static class StoredTask {
		public int id;

		public TaskSource source;

		public String type;

		public String title;

		public String description;

		public TaskStatus status;

		public Integer dueAt;

		public Integer assigneeUserId;

		public Integer assigneeGroupId;

		/**
		 * The group a group task was sent to, kept after a member claimed it
		 */
		public Integer originGroupId;

		public Integer creatorId;

		public String creatorName;

		public int cdate;

		public int edate;

		public Integer completedAt;

		public List<TaskObject> objects;

		public Map<String, Object> details;

		public String externalKey;

		public String genaixSessionId;

		public String genaixPlanId;

		public String genaixInstruction;
	}
}
