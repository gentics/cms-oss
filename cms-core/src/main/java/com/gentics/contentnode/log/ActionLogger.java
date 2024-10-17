package com.gentics.contentnode.log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.response.log.ActionLogEntry;
import com.gentics.contentnode.rest.resource.parameter.ActionLogParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class for handling the log command
 */
public class ActionLogger {
	/**
	 * Ttype of log actions
	 */
	public final static int LOGACTION_TYPE = 8;

	/**
	 * Logger for security events
	 */
	public final static NodeLogger securityLogger = NodeLogger.getNodeLogger("security");

	/**
	 * Logged types
	 */
	public final static List<TypePerms> LOGGED_TYPES = Arrays.asList(TypePerms.user, TypePerms.group, TypePerms.role,
			TypePerms.inboxmessage, TypePerms.actionlog, TypePerms.language, TypePerms.crfragment,
			TypePerms.contentrepository, TypePerms.construct, TypePerms.part, TypePerms.constructcategory,
			TypePerms.datasource, TypePerms.objproptype, TypePerms.objprop, TypePerms.objtag, TypePerms.tasktemplate, TypePerms.task, TypePerms.job, TypePerms.node,
			TypePerms.folder, TypePerms.page, TypePerms.pagecontent, TypePerms.template, TypePerms.image, TypePerms.file, TypePerms.form, TypePerms.devtooladmin);

	public final static int CREATE = 338;
	public final static int EDIT = 339;
	public final static int DEL = 340;
	public final static int MOVE = 387;
	public final static int PERM = 336;
	public final static int LOGIN = 341;
	public final static int LOGOUT = 343;
	public final static int PAGEPUB = 342;
	public final static int VIEW = 344;
	public final static int DIRT = 345;
	public final static int IMPORT = 346;
	public final static int RESTORE = 347;
	public final static int GENERATE = 348;
	public final static int COPY = 349;
	public final static int MODIFY = 350;
	public final static int PAGEOFFLINE = 351;
	public final static int NOTIFY = 352;
	public final static int DELALLVERSIONS = 353;
	public final static int PURGELOGS = 354;
	public final static int PURGEMESSAGES = 355;
	public final static int INBOXCREATE = 356;
	public final static int MAINTENANCE = 357;
	public final static int DEBUG = 358;
	public final static int VERSION = 359;
	public final static int MAJORVERSION = 360;
	public final static int LOCK = 361;
	public final static int UNLOCK = 362;
	public final static int WASTEBIN = 363;
	public final static int WASTEBINRESTORE = 364;

	public final static int MC_HIDE = 401;
	public final static int MC_UNHIDE = 402;

	public final static int LOGIN_FAILED = 500;
	public final static int ACCESS_DENIED = 501;

	public final static int PUBLISH_RUN = 666;
	public final static int PUBLISH_NODE_START = 665;
	public final static int PUBLISH_START = 664;

	public final static int FUM_START = 700;
	public final static int FUM_ACCEPTED = 701;
	public final static int FUM_DENIED = 702;
	public final static int FUM_POSTPONED = 703;
	public final static int FUM_ERROR = 704;

	public final static int DEVTOOL_SYNC_START = 750;
	public final static int DEVTOOL_SYNC_END = 751;

	/**
	 * 
	 */
	public final static int PAGETIME = 11001;

	public final static int PAGEQUEUE = 11002;

	public final static int[] OBJECTCHANGINGCOMMANDS = new int[] { MOVE, COPY, CREATE, EDIT, MODIFY, RESTORE, DIRT, PAGEPUB};

	/**
	 * Resultset Handler to get List of Logs
	 */
	protected final static HandleSelectResultSet<List<Log>> GET_LOG_LIST = rs -> {
		List<Log> logs = new ArrayList<>();
		while (rs.next()) {
			logs.add(new Log(rs));
		}
		return logs;
	};

	public static String getReadableCmd(int cmdDescId) {
		switch (cmdDescId) {
		case CREATE:
			return "create";

		case EDIT:
			return "edit";

		case DEL:
			return "del";

		case MOVE:
			return "move";

		case PERM:
			return "perm";

		case LOGIN:
			return "login";

		case LOGOUT:
			return "logout";

		case PAGEPUB:
			return "pagepub";

		case VIEW:
			return "view";

		case DIRT:
			return "dirt";

		case IMPORT:
			return "import";

		case RESTORE:
			return "restore";

		case GENERATE:
			return "generate";

		case COPY:
			return "copy";

		case MODIFY:
			return "modify";

		case PAGEOFFLINE:
			return "pageoffline";

		case NOTIFY:
			return "notify";

		case DELALLVERSIONS:
			return "delallversions";

		case PURGELOGS:
			return "purgelogs";

		case PURGEMESSAGES:
			return "purgemessages";

		case PUBLISH_RUN:
			return "publish run";

		case MC_HIDE:
			return "hide in channel";

		case MC_UNHIDE:
			return "unhide in channel";

		case FUM_START:
			return "FUM start";

		case FUM_ACCEPTED:
			return "FUM accepted";

		case FUM_DENIED:
			return "FUM denied";

		case FUM_POSTPONED:
			return "FUM postponed";

		case FUM_ERROR:
			return "FUM error";

		default:
			return "unknown command " + cmdDescId;
		}
	}

	/**
	 * Write a logcmd
	 * @param cmdDescId cmd description id
	 * @param oType object type
	 * @param oId object id
	 * @param oId2 object id
	 * @param info information
	 * @return The generated id of the log command entry in the database.
	 * @throws NodeException
	 */
	public static int logCmd(int cmdDescId, int oType, Integer oId, Integer oId2, String info) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> insertIds = DBUtils.executeInsert("INSERT INTO logcmd (user_id, cmd_desc_id, o_type, o_id, o_id2, info, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
				new Object[] {t.getUserId(), cmdDescId, oType, ObjectTransformer.getInt(oId, 0), ObjectTransformer.getInt(oId2, 0), info, t.getUnixTimestamp()});
		if (insertIds.size() > 0) {
			return insertIds.get(0);
		} else {
			return -1;
		}
	}

	/**
	 * Get the logcmd entry with the given id
	 * @param id id of the logcmd entry
	 * @return Log or null
	 * @throws NodeException
	 */
	public static Log getLogCmd(int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement st = null;
		ResultSet res = null;

		try {
			st = t.prepareStatement("SELECT * from logcmd WHERE id = ?");
			st.setInt(1, id);

			res = st.executeQuery();
			if (res.next()) {
				return new Log(res);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting logged cmd", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(st);
		}
	}

	/**
	 * Get a log entries for the given object (sorted by timestamp and id in ascending order)
	 * @param objType object type
	 * @param objId object ID
	 * @return list of log entries
	 * @throws NodeException
	 */
	public static List<Log> getLogCmdForObject(int objType, int objId) throws NodeException {
		return DBUtils.select("SELECT * FROM logcmd WHERE o_type = ? AND o_id = ? ORDER BY timestamp ASC, id ASC", pst -> {
			pst.setInt(1, objType);
			pst.setInt(2, objId);
		}, GET_LOG_LIST);
	}

	/**
	 * Get all logcmd entries
	 * @param cmdDescIds optional list of cmd desc IDs for filtering
	 * @return list of log entries
	 * @throws NodeException
	 */
	public static List<Log> getLogCmd(int... cmdDescIds) throws NodeException {
		String whereClause = cmdDescIds.length == 0 ? "" : " WHERE cmd_desc_id IN (" + StringUtils.repeat("?", ",", cmdDescIds.length) + ")";
		String sql = "SELECT * FROM logcmd " + whereClause + " ORDER BY timestamp ASC, id ASC";

		return DBUtils.select(sql, pst -> {
			for (int i = 0; i < cmdDescIds.length; i++) {
				pst.setInt(i + 1, cmdDescIds[i]);
			}
		}, GET_LOG_LIST);
	}

	/**
	 * write a logcmd
	 * @param cmd cmd description id
	 * @param type object type or null
	 * @param id if of modified obj
	 * @throws NodeException
	 */
	public static void log(int cmd, Class<? extends NodeObject> type, Integer id) throws NodeException {
		log(cmd, type, id, null, "");
	}

	/**
	 * write a logcmd
	 * @param cmd cmd description id
	 * @param type object type or null
	 * @param id if of modified obj
	 * @param id2 target object id
	 * @throws NodeException
	 */
	public static void log(int cmd, Class<? extends NodeObject> type, Integer id, Integer id2) throws NodeException {
		log(cmd, type, id, id2, "");
	}

	/**
	 * write a logcmd
	 * @param cmd cmd description id
	 * @param type object type or null
	 * @param id if of modified obj
	 * @param info info string
	 * @throws NodeException
	 */
	public static void log(int cmd, Class<? extends NodeObject> type, Integer id, String info) throws NodeException {
		log(cmd, type, id, null, info);
	}

	/**
	 * write a logcmd
	 * @param cmd cmd description id
	 * @param type object type or null
	 * @param id if of modified obj
	 * @param id2 id of target object
	 * @param info info text
	 * @throws NodeException
	 */
	public static void log(int cmd, Class<? extends NodeObject> type, Integer id, Integer id2, String info) throws NodeException {
		int ttype = 0;

		if (type != null) {
			ttype = TransactionManager.getCurrentTransaction().getTType(type);
		}
		logCmd(cmd, ttype, id, id2, info);
	}

	/**
	 * Get object changing commands
	 * @return list of object changing commands
	 */
	public static int[] getObjectChangingCommands() {
		return OBJECTCHANGINGCOMMANDS;
	}

	/**
	 * Log entry class
	 */
	public static class Log {
		/**
		 * Transform the log entry into its REST model
		 */
		public final static BiFunction<Log, ActionLogEntry, ActionLogEntry> NODE2REST = (log, model) -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			model.setId(log.getId());
			Action action = Action.getByCode(log.getCmdDescId());
			if (action != null) {
				model.setAction(Action.TRANSFORM2REST.apply(action));
			}

			if (action == Action.version || action == Action.majorversion || action == Action.restore) {
				// info contains the created/restored version
				model.setInfo(log.getInfo());
			} else if (log.getOType() == Page.TYPE_PAGE && log.getOId2() > 0) {
				// oid2 may contain the folder ID
				Folder folder = t.getObject(Folder.class, log.getOId2());
				if (folder != null) {
					model.setInfo(I18NHelper.getPath(folder));
				}
			} else {
				model.setInfo(log.getInfo());
			}

			model.setObjId(log.getOId());
			TypePerms type = TypePerms.get(Integer.toString(log.getOType()));
			if (type != null) {
				model.setType(TypePerms.TRANSFORM2REST.apply(type));
			}
			model.setTimestamp(log.getTimestamp());
			int userId = log.getUserId();
			if (userId > 0) {
				SystemUser user = t.getObject(SystemUser.class, userId);
				if (user != null) {
					model.setUser(String.format("%s %s", user.getLastname(), user.getFirstname()));
				} else {
					model.setUser("[internal]");
				}
			} else {
				model.setUser("[internal]");
			}
			return model;
		};

		/**
		 * Transform the log entry into its REST model
		 */
		public final static Function<Log, ActionLogEntry> TRANSFORM2REST = log -> {
			return NODE2REST.apply(log, new ActionLogEntry());
		};

		protected int id;

		protected int userId;

		protected int cmdDescId;

		protected int oType;

		protected int oId;

		protected int oId2;

		protected String info;

		protected int timestamp;

		/**
		 * Create empty log entry
		 */
		public Log() {
		}

		/**
		 * Create instance from resultset
		 * @param res resultset
		 * @throws SQLException
		 */
		public Log(ResultSet res) throws SQLException {
			id = res.getInt("id");
			userId = res.getInt("user_id");
			cmdDescId = res.getInt("cmd_desc_id");
			oType = res.getInt("o_type");
			oId = res.getInt("o_id");
			oId2 = res.getInt("o_id2");
			info = res.getString("info");
			timestamp = res.getInt("timestamp");
		}

		/**
		 * Entry ID
		 * @return ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * Set entry ID
		 * @param id ID
		 * @return fluent API
		 */
		public Log setId(int id) {
			this.id = id;
			return this;
		}

		/**
		 * User ID
		 * @return user ID
		 */
		public int getUserId() {
			return userId;
		}

		/**
		 * Set user ID
		 * @param userId ID
		 * @return fluent API
		 */
		public Log setUserId(int userId) {
			this.userId = userId;
			return this;
		}

		/**
		 * Command ID
		 * @return the cmdDescId
		 */
		public int getCmdDescId() {
			return cmdDescId;
		}

		/**
		 * Set command ID
		 * @param cmdDescId ID
		 * @return fluent API
		 */
		public Log setCmdDescId(int cmdDescId) {
			this.cmdDescId = cmdDescId;
			return this;
		}

		/**
		 * Get readable command description
		 * @return description
		 */
		public String getCmdDesc() {
			return ActionLogger.getReadableCmd(getCmdDescId());
		}

		/**
		 * @return the oType
		 */
		public int getOType() {
			return oType;
		}

		public Log setOType(int oType) {
			this.oType = oType;
			return this;
		}

		/**
		 * @return the oId
		 */
		public int getOId() {
			return oId;
		}

		public Log setOId(int oId) {
			this.oId = oId;
			return this;
		}

		/**
		 * @return the oId2
		 */
		public int getOId2() {
			return oId2;
		}

		public Log setOId2(int oId2) {
			this.oId2 = oId2;
			return this;
		}

		/**
		 * @return the info
		 */
		public String getInfo() {
			return info;
		}

		public Log setInfo(String info) {
			this.info = info;
			return this;
		}

		/**
		 * @return the timestamp
		 */
		public int getTimestamp() {
			return timestamp;
		}

		public Log setTimestamp(int timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		@Override
		public String toString() {
			return String.format("%s @ %d on %d.%d (%d), user %d, info %s", getReadableCmd(cmdDescId), timestamp, oType, oId, oId2, userId, info);
		}
	}

	/**
	 * Query for getting action log entries
	 */
	public static class LogQuery {

		private String user;

		private Set<Integer> actions = new HashSet<>();

		private Set<Integer> types = new HashSet<>();

		private Integer objId;

		private Integer startTimestamp;

		private Integer endTimestamp;

		private int start = 0;

		private int pageSize = -1;

		/**
		 * Set query parameters
		 * @param param parameter bean
		 * @return fluent API
		 */
		public LogQuery query(ActionLogParameterBean param) {
			if (param != null) {
				user = param.user;
				if (!ObjectTransformer.isEmpty(param.action)) {
					actions.addAll(param.action.stream().map(a -> {
						try {
							return Action.valueOf(a);
						} catch (Exception e) {
							return null;
						}
					}).filter(a -> a != null).map(Action::getCode).collect(Collectors.toSet()));
				}
				if (!ObjectTransformer.isEmpty(param.type)) {
					types.addAll(param.type.stream().map(t -> {
						try {
							return TypePerms.valueOf(t);
						} catch (Exception e) {
							return null;
						}
					}).filter(t -> t != null).map(TypePerms::type).collect(Collectors.toSet()));
				}
				objId = param.objId;
				startTimestamp = param.start;
				endTimestamp = param.end;
			}
			return this;
		}

		/**
		 * Set paging
		 * @param paging paging bean
		 * @return fluent API
		 */
		public LogQuery page(PagingParameterBean paging) {
			if (paging != null) {
				start = (Math.max(paging.page, 1) - 1) * paging.pageSize;
				pageSize = paging.pageSize;
			}
			return this;
		}

		/**
		 * Get the filtered, paged log entry list
		 * @return list
		 * @throws NodeException
		 */
		public List<Log> get() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT *");
			sql.append(getFromClause());
			sql.append(getWhereClause());

			sql.append(" ORDER BY logcmd.timestamp DESC, logcmd.id DESC");
			if (pageSize > 0) {
				sql.append(String.format(" LIMIT %d, %d", start, pageSize));
			}

			return DBUtils.select(sql.toString(), params(), GET_LOG_LIST);
		}

		/**
		 * Count the number of filtered log entries
		 * @return count
		 * @throws NodeException
		 */
		public int count() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT count(*) c");
			sql.append(getFromClause());
			sql.append(getWhereClause());

			return DBUtils.select(sql.toString(), params(), DBUtils.firstInt("c"));
		}

		/**
		 * Check whether there are more items (if paging is used)
		 * @param totalCount total count
		 * @return true if there are more items
		 */
		public boolean hasMore(int totalCount) {
			if (pageSize <= 0) {
				return false;
			} else {
				return totalCount > start + pageSize;
			}
		}

		/**
		 * Get parameter preparator
		 * @return preparator
		 */
		protected PrepareStatement params() {
			return stmt -> {
				int paramCounter = 0;
				if (!StringUtils.isBlank(user)) {
					String userPattern = "%" + user.toLowerCase() + "%";
					stmt.setString(++paramCounter, userPattern);
					stmt.setString(++paramCounter, userPattern);
					stmt.setString(++paramCounter, userPattern);
				}
				if (!ObjectTransformer.isEmpty(actions)) {
					for (int action : actions) {
						stmt.setInt(++paramCounter, action);
					}
				}
				if (!ObjectTransformer.isEmpty(types)) {
					for (int type : types) {
						stmt.setInt(++paramCounter, type);
					}
				}
				if (objId != null) {
					stmt.setInt(++paramCounter, objId);
				}
				if (startTimestamp != null) {
					stmt.setInt(++paramCounter, startTimestamp);
				}
				if (endTimestamp != null) {
					stmt.setInt(++paramCounter, endTimestamp);
				}
			};
		}

		/**
		 * Get the FROM clause
		 * @return clause
		 * @throws NodeException
		 */
		protected String getFromClause() throws NodeException {
			StringBuilder sql = new StringBuilder();
			sql.append(" FROM logcmd");

			if (!StringUtils.isBlank(user)) {
				sql.append(" LEFT JOIN systemuser ON systemuser.id = logcmd.user_id");
			}

			return sql.toString();
		}

		/**
		 * Get the WHERE clause
		 * @return clause
		 * @throws NodeException
		 */
		protected String getWhereClause() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			StringBuilder sql = new StringBuilder();
			sql.append(" WHERE logcmd.cmd_desc_id != 350");
			if (t.getUserId() != 1) {
				sql.append(" AND logcmd.user_id != 1");
			}

			if (!StringUtils.isBlank(user)) {
				sql.append(" AND (LOWER(systemuser.login) LIKE ? OR LOWER(systemuser.firstname) LIKE ? OR LOWER(systemuser.lastname) LIKE ?)");
			}

			if (!ObjectTransformer.isEmpty(actions)) {
				sql.append(" AND logcmd.cmd_desc_id IN (" + StringUtils.repeat("?", ",", actions.size()) + ")");
			}

			if (!ObjectTransformer.isEmpty(types)) {
				sql.append(" AND logcmd.o_type IN (" + StringUtils.repeat("?", ",", types.size()) + ")");
			}

			if (objId != null) {
				sql.append(" AND logcmd.o_id = ?");
			}

			if (startTimestamp != null) {
				sql.append(" AND logcmd.timestamp >= ?");
			}

			if (endTimestamp != null) {
				sql.append(" AND logcmd.timestamp <= ?");
			}

			return sql.toString();
		}
	}
}
