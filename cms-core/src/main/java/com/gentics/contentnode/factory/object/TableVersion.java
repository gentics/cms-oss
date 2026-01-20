package com.gentics.contentnode.factory.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;

import gnu.trove.TObjectIntHashMap;

/**
 * This is a reduced variant of {@link com.gentics.lib.db.TableVersion}, that does not store the dbhandle, but always uses the current transaction.
 * Instances of it are therefore reusable.
 * Since the tables, for which instances of this class are used, are not expected to change in runtime, the metadata of columns are stored in static maps.
 */
public class TableVersion {
	/**
	 * Batch size for fetching versioned and current data
	 */
	public final static int FETCH_BATCH_SIZE = 2000;

	/**
	 * Static store of column names per table
	 */
	protected final static Map<String, List<String>> tableColumnsMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Static store of column data types per table
	 */
	protected final static Map<String, List<Integer>> tableColumnsDataTypesMap = Collections.synchronizedMap(new HashMap<>());

	private String table;

	/**
	 * Joins necessary to find the versioned records
	 */
	private List<Join> joins = new ArrayList<>();

	private String wherePart;

	private List<String> tableColumns;

	private List<String> nonVersionedColumns = new ArrayList<String>();

	private String columnsList;

	/**
	 * First part of the SQL Statement to insert a new record into the _nodeversion table with existing values
	 * if the table does not have nodeversion_autoupdate
	 */
	private String insertSQLStatementNoParams;

	/**
	 * Second part of the SQL Statement to insert a new record into the _nodeversion table with existing values
	 * if the table does not have nodeversion_autoupdate
	 */
	private String insertSQLStatementParams;

	/**
	 * Flag to mark whether the table is supposed to have an auto_increment column (named auto_id)
	 */
	private boolean autoIncrement = false;

	/**
	 * Processor for handling "restore version" operations
	 */
	private TableVersionRestoreProcessor restoreProcessor;

	/**
	 * Restore the version with given timestamp in all the given {@link TableVersion} instances, if at least one has a diff
	 * @param versions instances
	 * @param id id
	 * @param timestamp timestamp to restore
	 * @throws NodeException
	 */
	public static void restoreIfDiff(List<TableVersion> versions, Long id, int timestamp) throws NodeException {
		restoreIfDiff(versions, new Object[] { id }, timestamp);
	}

	/**
	 * Restore the version with given timestamp in all the given {@link TableVersion} instances, if at least one has a diff
	 * @param versions instances
	 * @param idParams id parameters
	 * @param timestamp timestamp to restore
	 * @throws NodeException
	 */
	public static void restoreIfDiff(List<TableVersion> versions, Object[] idParams, int timestamp) throws NodeException {
		if (hasDiff(versions, idParams, timestamp, -1)) {
			for (TableVersion v : versions) {
				v.restoreVersion(idParams, timestamp);
			}
		}
	}

	/**
	 * Check if at least one of the {@link TableVersion} instances has a diff between the given timestamps
	 * @param versions instances
	 * @param idParams id parameters
	 * @param timestamp1 first timestamp
	 * @param timestamp2 second timestamp
	 * @return true if a diff was found
	 * @throws NodeException
	 */
	public static boolean hasDiff(List<TableVersion> versions, Object[] idParams, int timestamp1, int timestamp2) throws NodeException {
		for (TableVersion v : versions) {
			if (!v.getDiff(idParams, timestamp1, timestamp2).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create an instance of the TableVersion
	 */
	public TableVersion() {
	}

	/**
	 * Gets the versioned columns of the given table.
	 * 
	 * The union of {@link #getVersionedColumns(String)} and {@link #getNonVersionedColumns(String)}
	 * is equal to {@link #getColumns(String)}.
	 * 
	 * @param table table name
	 * @return list of column names as strings
	 * @throws NodeException
	 */
	public List<String> getVersionedColumns(String table) throws NodeException {
		List<String> columns = new ArrayList<String>(getColumns(table));

		// now remove all columns that are not contained in the _nodeversion table
		columns.retainAll(getColumns(table + "_nodeversion"));
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Gets the columns in the given table that are not versioned.
	 *
	 * The union of {@link #getVersionedColumns(String)} and {@link #getNonVersionedColumns(String)}
	 * is equal to {@link #getColumns(String)}.
	 *
	 * If one or more columns are non-versioned, then a current version must exist so that
	 * the current value of the non-versioned column can be retrieved.
	 * 
	 * @param table
	 * 		  The table to get the non-versioned columns of.
	 * @return
	 * 		  A list of column names that are excluded from versioning.
	 */
	private List<String> getNonVersionedColumns(String table) throws NodeException {
		List<String> columns = new ArrayList<String>(getColumns(table));

		columns.removeAll(getVersionedColumns(table));
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Gets the columns in the given table. 
	 * 
	 * @param table
	 * 		The table to get the columns of
	 * @return
	 * 		A list of column names.
	 * @throws NodeException
	 * 		If something unexpected happens while querying the DB.
	 */
	private List<String> getColumns(String table) throws NodeException {
		return MiscUtils.unwrap(() -> tableColumnsMap.computeIfAbsent(table, key -> {
			return MiscUtils.wrap(() -> {
				try {
					return Collections.unmodifiableList(DB.getTableColumns(getHandle(), key));
				} catch (SQLException e) {
					throw new NodeException("Error while getting columns of table {" + key + "}", e);
				}
			});
		}));
	}

	/**
	 * Get the column datatypes for the table
	 * @param table table
	 * @return list of column datatypes
	 * @throws NodeException
	 */
	private List<Integer> getColumnDataTypes(String table) throws NodeException {
		return MiscUtils.unwrap(() -> tableColumnsDataTypesMap.computeIfAbsent(table, key -> {
			return MiscUtils.wrap(() -> {
				try {
					return DB.getColumnDataTypes(getHandle(), key);
				} catch (SQLException e) {
					throw new NodeException("Error while getting column datatypes", e);
				}
			});
		}));
	}

	/**
	 * Get the dbhandle
	 * @return dbhandle
	 */
	public DBHandle getHandle() throws NodeException {
		return TransactionManager.getCurrentTransaction().getDBHandle();
	}

	/**
	 * defines an optional table to use for joining. usefull when you want to
	 * version subtables in the meaning of entity relations. example: table
	 * product and table price are joined by price.productid, and you intend to
	 * version productprices by it's productid.
	 * @param joinTable the tablename related to your versiontable
	 * @param joinTableCol the column in joinTable used for joining. example:
	 *        product.id
	 * @param joinCol the column in joinTable used for joining. example:
	 *        productid
	 */
	public void setJoin(String joinTable, String joinTableCol, String joinCol) {
		joins.add(new Join(joinCol, joinTable, joinTableCol));
	}

	/**
	 * Add an optional table for joining
	 * @param join join information
	 */
	public void addJoin(Join join) {
		joins.add(join);
	}

	/**
	 * sets the table to version. table must have a table_nodeversion clone.
	 * table must have a unique column "id". required for all versioning
	 * operations.
	 * @param table name of databasetable. example: price
	 * @throws NodeException
	 */
	public void setTable(String table) throws NodeException {
		this.table = table;
		tableColumns = getVersionedColumns(table);
		nonVersionedColumns = getNonVersionedColumns(table);
		columnsList = "";
		boolean first = true;

		for (Iterator<String> i = tableColumns.iterator(); i.hasNext();) {
			if (first) {
				first = false;
			} else {
				columnsList += ",";
			}
			columnsList += i.next();
		}

		insertSQLStatementNoParams = "INSERT INTO `%s_nodeversion` (%s, nodeversiontimestamp, nodeversion_user, nodeversionlatest, nodeversionremoved) VALUES "
				.formatted(table, columnsList);
		insertSQLStatementParams = "(%s)".formatted(StringUtils.repeat("?", tableColumns.size() + 4, ","));
	}

	/**
	 * defines the where clause used to determine affected datasource rows. the
	 * dataset idParams are appended during operation. the actually used table
	 * is aliased with gentics_main
	 * @param wherePart example: "gentics_main.id = ?"
	 */
	public void setWherePart(String wherePart) {
		if (wherePart.equals("")) {
			wherePart = "1=1";
		}
		this.wherePart = wherePart;
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable, and the optional setJoin
	 * methods.
	 * @param id the unique id of the dataset.
	 * @param time the timestamp used for the new version.
	 * @param userId the userid used for the new version.
	 * @return true if a dataset has changed and a new version was created,
	 *         false if it's unchanged.
	 * @throws NodeException
	 */
	public boolean createVersion(String id, int time, String userId) throws NodeException {
		return createVersion(new Object[] { id}, time, userId, false, true, false);
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable and the optional setJoin
	 * methods. the version will get marked as "latest" version
	 * @param idParams the set of id parameters used in the wherePart,
	 *        identifying the dataset to be versioned
	 * @param time the timestamp used for the new version.
	 * @param userId the userId used for the new version.
	 * @return true if the datasaet has changed and a new version was created,
	 *         false if remained unchanged
	 * @throws NodeException
	 */
	public boolean createVersion(Object[] idParams, int time, String userId) throws NodeException {
		return createVersion(idParams, time, userId, false, true, false);
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable and the optional setJoin
	 * methods.
	 * @param idParams the set of id parameters used in the wherePart,
	 *        identifying the dataset to be versioned
	 * @param time the timestamp used for the new version.
	 * @param userId the userId used for the new version.
	 * @param futureOrPastChange true when this shall not be marked as latest
	 *        version
	 * @return true if the datasaet has changed and a new version was created,
	 *         false if remained unchanged
	 * @throws NodeException
	 */
	public boolean createVersion(Object[] idParams, int time, String userId,
			boolean futureOrPastChange) throws NodeException {
		return createVersion(idParams, time, userId, futureOrPastChange, true, false);
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable and the optional setJoin
	 * methods.
	 * @param idParams the set of id parameters used in the wherePart,
	 *        identifying the dataset to be versioned
	 * @param time the timestamp used for the new version.
	 * @param userId the userId used for the new version.
	 * @param futureOrPastChange true when this shall not be marked as latest
	 *        version
	 * @param autoupdate true when the autoupdate flag shall be set (for future
	 *        versions), false if not
	 * @return true if the datasaet has changed and a new version was created,
	 *         false if remained unchanged
	 * @throws NodeException
	 */
	public boolean createVersion(Object[] idParams, int time, String userId,
			boolean futureOrPastChange, boolean autoupdate) throws NodeException {
		return createVersion(idParams, time, userId, futureOrPastChange, true, autoupdate);
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable and the optional setJoin
	 * methods.
	 * @param idParams the set of id parameters used in the wherePart,
	 *        identifying the dataset to be versioned
	 * @param time the timestamp used for the new version.
	 * @param userId the userId used for the new version.
	 * @param futureOrPastChange true when this shall not be marked as latest
	 *        version
	 * @param doUpdateNext true when the next version (if existing) should be
	 *        updated, false if not (because this call is already made for
	 *        updating a "next" version)
	 * @param autoupdate true when the autoupdate flag shall be set (for future
	 *        versions), false if not
	 * @return true if the datasaet has changed and a new version was created,
	 *         false if remained unchanged
	 * @throws NodeException
	 */
	protected boolean createVersion(Object[] idParams, int time, String userId,
			boolean futureOrPastChange, boolean doUpdateNext, boolean autoupdate) throws NodeException {
		boolean newVersion = false;
		Integer timeInt = new Integer(time);

		// restriction to version at a timestamp (by subselect)
		String restrictToTimestampVersion = "nodeversiontimestamp = " + "(SELECT max(nodeversiontimestamp) FROM `" + getTable()
				+ "_nodeversion` WHERE id = orig.id AND nodeversiontimestamp <= " + time + " AND (nodeversionremoved = 0 OR nodeversionremoved > " + time + "))";

		// get rows of current version
		String sqlCurrentVersion = "SELECT gentics_main.id FROM " + getFromPart(false) + " WHERE " + getWherePart();

		try {
			// first get the version after the timestamp we will create a
			// version
			// TODO: make this more efficient (implement a method that gets only
			// the next version)
			SimpleResultProcessor nextVersionData = null;
			SimpleResultProcessor currentData = null;
			Version nextVersion = null;

			if (doUpdateNext) {
				Version[] versions = getVersions(idParams);

				for (int i = 0; i < versions.length; ++i) {
					if (versions[i].getTimestamp() > time) {
						nextVersion = versions[i];
						break;
					}
				}
			}
			if (nextVersion != null) {
				// get the data of the next version
				nextVersionData = getVersionData(idParams, nextVersion.getTimestamp());

				// ... and the data of the current version
				currentData = new SimpleResultProcessor();
				DB.query(getHandle(), "SELECT * FROM " + getFromPart(false) + " WHERE " + getWherePart(), idParams, currentData);
			}

			SimpleResultProcessor rsLastVersion = new SimpleResultProcessor();
			SimpleResultProcessor rsCurrentVersion = new SimpleResultProcessor();

			// get the current version
			DB.query(getHandle(), sqlCurrentVersion, idParams, rsCurrentVersion);

			// delete the versions which might be in the way
			if (rsCurrentVersion.size() > 0) {
				int currentVersionSize = rsCurrentVersion.size();
				Object[] deleteParams = new Object[currentVersionSize + 1];

				deleteParams[0] = timeInt;
				for (int i = 1; i <= currentVersionSize; ++i) {
					deleteParams[i] = rsCurrentVersion.getRow(i).getObject("id");
				}

				delete("nodeversiontimestamp = ? AND id IN (" + StringUtils.repeat("?", currentVersionSize, ",") + ")", deleteParams);
			}

			List<Integer> changedRecordIDs = getChangedRecordIDs(idParams, time);

			// DB.query(getHandle(), sqlLastVersion, idParams, rsLastVersion);
			rsLastVersion = getVersionData(idParams, time);

			// there are changed rows ...
			if (changedRecordIDs.size() > 0) {
				String changedIds = StringUtils.merge(changedRecordIDs.toArray(), ",");

				newVersion = true;

				// update the changed rows (not to be the latest versions any
				// more)
				if (!futureOrPastChange) {
					update("nodeversionlatest = ?", new Object[] { 0}, "id IN (" + changedIds + ")", null);
				}

				String insertQuery = null;
				Object[] insertParams = null;

				insertQuery = "INSERT INTO `" + table + "_nodeversion` (" + columnsList
						+ ",nodeversiontimestamp, nodeversion_user, nodeversionlatest, nodeversionremoved) " + "SELECT " + columnsList + ", ?, ?, ?, 0 FROM `"
						+ table + "` WHERE id in (" + changedIds + ")";
				insertParams = new Object[] { timeInt, userId, new Integer(futureOrPastChange ? 0 : 1)};

				DB.update(getHandle(), insertQuery, insertParams, null);
			}

			// there are last versions
			if (rsLastVersion.size() > 0) {
				StringBuffer lastVersionString = new StringBuffer();
				StringBuffer currentVersionString = new StringBuffer();
				boolean first = true;

				for (Iterator<SimpleResultRow> iter = rsLastVersion.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					if (first) {
						first = false;
					} else {
						lastVersionString.append(",");
					}
					lastVersionString.append(row.getInt("id"));
				}
				first = true;
				for (Iterator<SimpleResultRow> iter = rsCurrentVersion.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					if (first) {
						first = false;
					} else {
						currentVersionString.append(",");
					}
					currentVersionString.append(row.getInt("id"));
				}
				SimpleResultProcessor rsCount = new SimpleResultProcessor();
				String sqlCountForUpdate = "SELECT count(*) num FROM `" + table + "_nodeversion` " + " WHERE id IN (" + lastVersionString.toString() + ")";

				if (currentVersionString.length() > 0) {
					sqlCountForUpdate += " AND id NOT IN (" + currentVersionString.toString() + ")";
				}
				DB.query(this.getHandle(), sqlCountForUpdate, rsCount);
				if (rsCount.size() > 0 && rsCount.getRow(1).getInt("num") > 0) {
					newVersion = true;
				}

				// remove records that are no longer present in the new version
				// if(!futureChange) {
				// String sqlUpdateLastVersion="UPDATE
				// "+getTable()+"_nodeversion SET nodeversionremoved = "+time+",
				// nodeversionlatest = 0"+
				// " WHERE id IN ("+ lastVersionString.toString()+")";
				//
				// if(currentVersionString.length() > 0) {
				// sqlUpdateLastVersion += " AND id NOT IN
				// ("+currentVersionString.toString()+")";
				// }
				// DB.update(this.getHandle(),sqlUpdateLastVersion);
				// }
				// records that are no longer present in the new version have to
				// be copied to the new version anyway (and marked as removed)
				String sqlCopyRemoved = null;

				sqlCopyRemoved = "INSERT INTO `" + getTable() + "_nodeversion` (" + columnsList
						+ ", nodeversiontimestamp, nodeversionremoved, nodeversionlatest, nodeversion_user) ";
				sqlCopyRemoved += "SELECT " + columnsList + ", ?, ?, 0, ? FROM `" + getTable()
						+ "_nodeversion` orig WHERE id IN (" + lastVersionString.toString() + ") AND " + restrictToTimestampVersion;

				// sqlCopyRemoved += "SELECT "+columnsList+", ?, ?, 0,
				// nodeversion_user FROM "+getTable()+"_nodeversion WHERE id IN
				// ("+lastVersionString.toString()+") AND nodeversionlatest =
				// 1";
				if (currentVersionString.length() > 0) {
					sqlCopyRemoved += " AND id NOT IN (" + currentVersionString.toString() + ")";
				}
				Object[] params = null;

				params = new Object[] { timeInt, timeInt, userId};

				DB.update(this.getHandle(), sqlCopyRemoved, params, null);

				// when this is no future change, mark the removed records as no
				// longer latest
				// if(!futureOrPastChange) {
				String sqlUpdateLastVersion = "UPDATE `" + getTable() + "_nodeversion` SET nodeversionremoved = ?, nodeversionlatest = 0"
						+ " WHERE (nodeversionremoved = 0 OR nodeversionremoved > ?) AND nodeversiontimestamp <= ? AND id IN (" + lastVersionString.toString() + ")";

				if (currentVersionString.length() > 0) {
					sqlUpdateLastVersion += " AND id NOT IN (" + currentVersionString.toString() + ")";
				}
				DB.update(this.getHandle(), sqlUpdateLastVersion, new Object[] { timeInt, timeInt, timeInt}, null);
				// }
			}

			// when there is a next version, it has to be updated
			if (nextVersion != null) {
				// restore the data of the next version
				restoreData(idParams, nextVersionData);
				// update the version
				createVersion(idParams, nextVersion.getTimestamp(), userId, true, false, nextVersion.isAutoupdate());
				// restore the current data
				restoreData(idParams, currentData);
			}

			// when this is the entry point of creating versions, we set the
			// latest flags
			if (doUpdateNext) {
				setLatestFlag(idParams);
			}

		} catch (SQLException ex) {
			throw new NodeException("Error while creating new version in table {" + table + "}", ex);
		}

		return newVersion;
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable, and the optional setJoin
	 * methods.
	 * @param id the unique id of the dataset.
	 * @param time the timestamp used for the new version.
	 * @param userId the userid used for the new version.
	 * @param futureOrPastChange true when this is a future or past change
	 *        (version will not become latest), false if this version shall
	 *        become the latest version
	 * @return true if a dataset has changed and a new version was created,
	 *         false if it's unchanged.
	 * @throws NodeException
	 */

	public boolean createVersion(String id, int time, String userId, boolean futureOrPastChange) throws NodeException {
		return createVersion(new Object[] { id}, time, userId, futureOrPastChange, true, false);
	}

	/**
	 * creates a new version of a dataset if it has changed. the dataset is
	 * defined by the required setWherePart, setTable, and the optional setJoin
	 * methods.
	 * @param id the unique id of the dataset
	 * @param time the timestamp used for the new version.
	 * @param user the userId used for the new version.
	 * @return true if the dataset has changed and a new version was created,
	 *         false if not
	 * @throws NodeException
	 */
	public boolean createVersion(Long id, int time, String user) throws NodeException {
		return createVersion(new Object[] { id}, time, user);
	}

	/**
	 * Another implementation to create a new version. Does not support creating versions between other versions or in the future/past
	 * @param id unique id of the dataset
	 * @param time version timestamp
	 * @param userId user id
	 * @return true if a new version was created, false if not
	 * @throws NodeException
	 */
	public boolean createVersion2(Long id, int time, String userId) throws NodeException {
		return createVersion2(new Object[] {id}, time, userId);
	}

	/**
	 * Another implementation to create a new version. Does not support creating versions between other versions or in the future/past
	 * @param idParams id parameters
	 * @param time version timestamp
	 * @param userId user id
	 * @return true if a new version was created, false if not
	 * @throws NodeException
	 */
	public boolean createVersion2(Object[] idParams, int time, String userId) throws NodeException {
		try {
			// get the diff between the version to be written and the current version
			List<Diff> diff = getDiff(idParams, time, -1);

			// no diff, nothing to do
			if (diff.isEmpty()) {
				return false;
			}

			// get all existing versions
			Version[] versions = getVersions(idParams);

			// this will be a conflicting version (already existing with the same timestamp)
			Version conflicting = null;

			for (int i = 0; i < versions.length; ++i) {
				if (versions[i].getTimestamp() == time) {
					conflicting = versions[i];
					break;
				}
			}

			// remove conflicting nodeversion
			if (conflicting != null) {
				List<Integer> ids = new ArrayList<Integer>();
				for (Diff diffEntry : diff) {
					if (diffEntry.oldData != null) {
						ids.add(diffEntry.oldData.getInt("id"));
					}
				}
				if (!ids.isEmpty()) {
					Object[] deleteParams = new Object[1 + ids.size()];
					int index = 0;
					deleteParams[index++] = time;
					for (Integer id : ids) {
						deleteParams[index++] = id;
					}
					delete("nodeversiontimestamp = ? AND id IN (" + StringUtils.repeat("?", ids.size(), ",") + ")", deleteParams);
				}
			}

			BatchUpdater batchUpdater = new BatchUpdater();
			// iterate over the diff
			for (Diff rowDiff : diff) {
				switch (rowDiff.diffType) {
				case Diff.DIFFTYPE_ADD:
				case Diff.DIFFTYPE_MOD: {
					// insert new row
					Object[] insertParams = new Object[tableColumns.size() + 4];
					int index = 0;
					for (String col : tableColumns) {
						insertParams[index++] = rowDiff.newData.getObject(col);
					}
					insertParams[index++] = time;
					insertParams[index++] = userId;
					insertParams[index++] = 1;
					insertParams[index++] = 0;

					if (rowDiff.diffType == Diff.DIFFTYPE_MOD) {
						update("nodeversionlatest = ?", new Object[] {0}, "id = ?", new Object[] {rowDiff.id});
					}
					batchUpdater.add(insertSQLStatementNoParams, insertSQLStatementParams, Transaction.INSERT_STATEMENT, insertParams, null, null, null);
					break;
				}
				case Diff.DIFFTYPE_DEL: {
					// insert row as "deleted"
					Object[] deleteParams = new Object[tableColumns.size() + 4];
					int index = 0;
					for (String col : tableColumns) {
						deleteParams[index++] = rowDiff.oldData.getObject(col);
					}
					deleteParams[index++] = time;
					deleteParams[index++] = userId;
					deleteParams[index++] = 0;
					deleteParams[index++] = time;

					update("nodeversionlatest = ?, nodeversionremoved = ?", new Object[] { 0, time }, "id = ?", new Object[] { rowDiff.id });
					batchUpdater.add(insertSQLStatementNoParams, insertSQLStatementParams, Transaction.INSERT_STATEMENT, deleteParams, null, null, null);
					break;
				}
				}
			}
			batchUpdater.execute();

			return true;
		} catch (SQLException ex) {
			throw new NodeException("Error while creating new version in table {" + table + "}", ex);
		}
	}

	/**
	 * Get a list of IDs of records, that are different in the main table from the version, that is active at the given time
	 * @param idParams parameters necessary to identify the records
	 * @param time version timestamp
	 * @return list of record IDs
	 * @throws NodeException
	 */
	protected List<Integer> getChangedRecordIDs(Object[] idParams, int time) throws NodeException {
		if (time < 0) {
			return Collections.emptyList();
		}
		// get the diff between the current and the given version
		List<Diff> diffList = getDiff(idParams, time, -1);

		List<Integer> changedIds = new Vector<Integer>(diffList.size());

		for (Diff diff : diffList) {
			if (diff.getDiffType() != Diff.DIFFTYPE_DEL) {
				changedIds.add(diff.getId());
			}
		}

		return changedIds;
	}

	/**
	 * Get the list of id's of the current records
	 * @param idParams parameters defining the recordset
	 * @return list of id's
	 * @throws NodeException
	 */
	protected List<Integer> getCurrentIds(Object[] idParams) throws SQLException, NodeException {
		// get rows of current version
		String sqlCurrentVersion = "SELECT DISTINCT gentics_main.id FROM " + getFromPart(false) + " WHERE " + getWherePart();
		final List<Integer> ids = new ArrayList<>();

		DB.query(getHandle(), sqlCurrentVersion, idParams, new ResultProcessor() {

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
			 */
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
			}

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
			 */
			public void takeOver(ResultProcessor p) {}
		});

		return ids;
	}

	/**
	 * restores an older version of a dataset from versionclone table, to the
	 * working table. the dataset is defined by the required
	 * setWherePart,setTable, and the optional setJoin methods.
	 * @param idParams the unique idParams of the dataset.
	 * @param time the timestamp of the timespot to restore. timestamp must not
	 *        equal the timestamps of the versions.
	 */
	public void restoreVersion(Object[] idParams, int time) throws NodeException {
		try {
			// get ids of rows to restore
			String sql = "SELECT gentics_main.id, max(gentics_main.nodeversiontimestamp) nodeversionrtime" + " FROM " + getFromPart()
					+ " WHERE (nodeversionremoved >" + time + " OR nodeversionremoved = 0) AND nodeversiontimestamp <=" + time + " AND " + getWherePart()
					+ " GROUP BY id";

			SimpleResultProcessor rowsToRestore = new SimpleResultProcessor();
			SimpleResultProcessor rsVersions = new SimpleResultProcessor();

			DB.query(getHandle(), sql, idParams, rsVersions);

			if (restoreProcessor != null) {
				restoreProcessor.preRestore(this, rsVersions);
			}

			for (Iterator<SimpleResultRow> iter = rsVersions.iterator(); iter.hasNext();) {
				SimpleResultRow row = (SimpleResultRow) iter.next();
				SimpleResultProcessor data = new SimpleResultProcessor();

				fetchVersionedData(row.getInt("id"), row.getInt("nodeversionrtime"), data, false);
				rowsToRestore.merge(data);
				DB.update(getHandle(), "DELETE FROM `" + getTable() + "` WHERE id = ?", new Object[] { row.getInt("id") });
			}

			// Although we deleted the rows as we retrieved their data, there may be more rows in the current
			// version than in the version to be restored, so we have to make a general delete of all rows
			// for which the where condition is true. I think but am not 100% certain that this would also
			// delete the rows we deleted while fetching the data, making the delete above redundant.
			StringBuffer deleteSQL = new StringBuffer();

			if (!joins.isEmpty()) {
				// e.g. DELETE value FROM value, contenttag WHERE value.contenttag_id = contenttag.id AND contenttag.content_id = ?
				deleteSQL.append("DELETE gentics_main FROM `").append(table).append("` gentics_main");
				for (Join join : joins) {
					deleteSQL.append(", ").append(join.joinedTable);
				}
				deleteSQL.append(" WHERE ");
				for (Join join : joins) {
					deleteSQL.append(join.getJoinClause()).append(" AND ");
				}
				deleteSQL.append(getWherePart(false));
			} else {
				// e.g. DELETE FROM page WHERE id = ?
				deleteSQL.append("DELETE FROM `").append(table).append("` WHERE ").append(getWherePart(true));
			}
			DB.update(getHandle(), deleteSQL.toString(), idParams);

			insertRowsFromData(rowsToRestore);

			if (restoreProcessor != null) {
				restoreProcessor.postRestore(this, rsVersions);
			}
		} catch (SQLException ex) {
			throw new NodeException("Error while restoring version in table {" + table + "}", ex);
		}
	}

	/**
	 * restores an older version of a dataset from versionclone table, to the
	 * working table. the dataset is defined by the required
	 * setWherePart,setTable, and the optional setJoin methods.
	 * @param id the unique id of the dataset.
	 * @param time the timestamp of the timespot to restore. timestamp must not
	 *        equal the timestamps of the versions.
	 */
	public void restoreVersion(String id, int time) throws NodeException {
		restoreVersion(new Object[] { id}, time);
	}

	/**
	 * Gets the SQL statement to get the current data.
	 * 
	 * The statement will have one parameterized values:
	 * 1. the id of the object in the ..._nodeversion table.
	 * 
	 * @return
	 * 		The SQL statement to get the current data.
	 */
	private String getCurrentDataSelect() {
		return "SELECT " + makeColumnsSql(nonVersionedColumns) + " FROM `" + getTable() + "` WHERE id = ?";
	}

	/**
	 * Gets the SQL statement to get the versioned data.
	 * 
	 * The statement will have two parameterized values:
	 * 1. the id of the object in the ..._nodeversion table
	 * 2. the nodeversiontimestamp of the versioned data.
	 * 
	 * @return
	 * 		The SQL statement to the the versioned data.
	 */
	private String getVersionedDataSelect() {
		return " SELECT " + columnsList + " FROM `" + table + "_nodeversion` " + " WHERE " + table + "_nodeversion.id = ? AND " + table
				+ "_nodeversion.nodeversiontimestamp = ?";
	}

	/**
	 * Fetches a single row of versioned data for the given object id and puts it into the given result.
	 * 
	 * @param id
	 * 		  The id of the object to retrieve the versioned data for.
	 * @param nodeversionrtime
	 * 		  The timestamp of the version to retrieve.
	 * @param result
	 * 		  The SimpleResultProcesssor that will get a single row added to it.
	 * @param addNodeversionTimestamp true if the nodeversiontimestamps shall be added
	 * @throws SQLException
	 * 		If something unexpected happens while querying the DB. 
	 * @throws NodeException
	 * 		If something unexpected happens.
	 */
	private void fetchVersionedData(int id, int nodeversionrtime, SimpleResultProcessor result, boolean addNodeversionTimestamp) throws SQLException, NodeException {

		Object[] params = new Object[] { new Integer(id), new Integer(nodeversionrtime) };
		SimpleResultProcessor versionedData = new SimpleResultProcessor();

		DB.query(getHandle(), getVersionedDataSelect(), params, versionedData);

		Map<String, Object> data = new HashMap<String, Object>();

		SimpleResultRow versionedRow = versionedData.getRow(1);

		for (String versionedColumn : tableColumns) {
			data.put(versionedColumn, versionedRow.getObject(versionedColumn));
		}

		if (!nonVersionedColumns.isEmpty()) {
			SimpleResultProcessor currentData = new SimpleResultProcessor();

			DB.query(getHandle(), getCurrentDataSelect(), new Object[] { id }, currentData);
			if (currentData.size() > 0) {
				SimpleResultRow currentRow = currentData.getRow(1);

				for (String nonVersionedColumn : nonVersionedColumns) {
					data.put(nonVersionedColumn, currentRow.getObject(nonVersionedColumn));
				}
			}
		}

		if (addNodeversionTimestamp) {
			data.put("nodeversiontimestamp", nodeversionrtime);
		}
		addRowFromMap(result, data);
	}

	/**
	 * Fetch versioned data for the given records
	 * @param ids list of record ids
	 * @param versionTimestampMap map of record ids to timestamps
	 * @param retval result processor that will get the results added
	 * @param addNodeversionTimestamps true to add the nodeversion timestamps to the result
	 * @param addNonVersionedData true to add non versioned data
	 * @throws SQLException
	 */
	private void fetchVersionedData(List<Integer> ids, Map<Integer, Integer> versionTimestampMap, SimpleResultProcessor retval,
			boolean addNodeversionTimestamps, boolean addNonVersionedData) throws SQLException, NodeException {
		if (ids.isEmpty() || versionTimestampMap.isEmpty()) {
			return;
		}

		SimpleResultProcessor allRows = new SimpleResultProcessor();
		int start = 0;
		int total = ids.size();
		while (start < total) {
			int end = Math.min(start + FETCH_BATCH_SIZE, total);
			List<Integer> batch = ids.subList(start, end);
			allRows.append(fetchVersionedDataBatch(batch, versionTimestampMap));
			start += FETCH_BATCH_SIZE;
		}

		Map<Integer, Map<String, Object>> dataPerId = new HashMap<Integer, Map<String,Object>>();
		for (SimpleResultRow row : allRows) {
			int id = row.getInt("id");
			int timestamp = row.getInt("nodeversiontimestamp");

			if (versionTimestampMap.get(id) == timestamp) {
				Map<String, Object> data = new HashMap<String, Object>();
				for (String versionedColumn : tableColumns) {
					data.put(versionedColumn, row.getObject(versionedColumn));
				}

				if (addNodeversionTimestamps) {
					data.put("nodeversiontimestamp", timestamp);
				}

				dataPerId.put(id, data);
			}
		}

		if (addNonVersionedData && !nonVersionedColumns.isEmpty()) {
			// add non versioned data
			SimpleResultProcessor currentData = new SimpleResultProcessor();
			start = 0;
			total = ids.size();
			while (start < total) {
				int end = Math.min(start + FETCH_BATCH_SIZE, total);
				List<Integer> batch = ids.subList(start, end);
				currentData.append(fetchCurrentDataBatch(batch));
				start += FETCH_BATCH_SIZE;
			}

			for (SimpleResultRow row : currentData) {
				int id = row.getInt("id");

				Map<String, Object> data = dataPerId.get(id);
				if (data != null) {
					for (String nonVersionedColumn : nonVersionedColumns) {
						data.put(nonVersionedColumn, row.getObject(nonVersionedColumn));
					}
				}
			}
		}

		for (Integer id : ids) {
			Map<String, Object> data = dataPerId.get(id);
			if (data != null) {
				addRowFromMap(retval, data);
			}
		}
	}

	/**
	 * Fetch versioned data for the given batch of IDs
	 * @param ids record IDs
	 * @param versionTimestampMap map of version timestamps
	 * @return result processor containing the records
	 * @throws SQLException
	 */
	private SimpleResultProcessor fetchVersionedDataBatch(List<Integer> ids, Map<Integer, Integer> versionTimestampMap) throws SQLException, NodeException {
		Set<Integer> timestamps = new HashSet<>();
		for (int id : ids) {
			timestamps.add(versionTimestampMap.get(id));
		}

		String sql = String.format(
				"SELECT %s, nodeversiontimestamp FROM `%s_nodeversion` WHERE %s_nodeversion.id IN (%s) AND %s_nodeversion.nodeversiontimestamp IN (%s)",
				columnsList, table, table, StringUtils.repeat("?", ids.size(), ","), table,
				StringUtils.repeat("?", timestamps.size(), ","));

		Integer[] params = new Integer[ids.size() + timestamps.size()];
		int index = 0;
		for (int id : ids) {
			params[index++] = id;
		}
		for (int timestamp : timestamps) {
			params[index++] = timestamp;
		}
		SimpleResultProcessor tmp = new SimpleResultProcessor();
		DB.query(getHandle(), sql, params, tmp);
		return tmp;
	}

	/**
	 * Fetch current data for the given batch of IDs
	 * @param ids record IDs
	 * @return result processor containing the records
	 * @throws SQLException
	 */
	private SimpleResultProcessor fetchCurrentDataBatch(List<Integer> ids) throws SQLException, NodeException {
		String sql = String.format("SELECT id, %s FROM `%s` WHERE id IN (%s)", makeColumnsSql(nonVersionedColumns), table,
				StringUtils.repeat("?", ids.size(), ","));

		SimpleResultProcessor tmp = new SimpleResultProcessor();
		DB.query(getHandle(), sql, ids.toArray(), tmp);
		return tmp;
	}

	/**
	 * Adds a row to the given result from the given rowData.
	 * 
	 * @param result
	 * 		  The SimpleResultProcessor to which a new row should be added.
	 * @param rowData
	 * 		  The values for the row to be added indexed by column name.
	 */
	private void addRowFromMap(SimpleResultProcessor result, Map<String, Object> rowData) {
		TObjectIntHashMap nameMap = new TObjectIntHashMap(rowData.size());
		int columnIdx = 0;
		Object[] values = new Object[rowData.size()];

		for (Entry<String, Object> entry : rowData.entrySet()) {
			nameMap.put(entry.getKey(), columnIdx);
			values[columnIdx] = entry.getValue();
			columnIdx += 1;
		}
		result.addRow(nameMap, values);
	}

	/**
	 * restore data held in a result processor
	 * @param idParams idParams of the dataset
	 * @param data holding the data
	 * @throws Exception
	 */
	protected void restoreData(Object[] idParams, SimpleResultProcessor data) throws SQLException, NodeException {
		// first remove all existing data
		DB.update(getHandle(), "DELETE FROM `" + getTable() + "` WHERE " + getWherePart(true), idParams, null);
		insertRowsFromData(data);
	}

	/**
	 * Inserts a new row into this.table from the given data.
	 * 
	 * @param data
	 *		  The column values for the new rows will be taken from the given SimpleResultProcessor.
	 *		  Each row in the given data must contain a value for each value in the union of
	 *		  tableColumns + nonVersionedColumns.
	 * @throws SQLException
	 * 		If something unexpected happens.
	 */
	private void insertRowsFromData(SimpleResultProcessor data) throws SQLException, NodeException {
		List<String> allColumns = getColumns(table);
		List<Integer> columnTypes = getColumnDataTypes(table);

		String insertSql = "INSERT INTO `" + getTable() + "` (" + makeColumnsSql(allColumns) + ")" + " VALUES (" + makeQuestionMarks(allColumns.size()) + ")";

		List<Object> params = new ArrayList<Object>();

		for (Iterator<SimpleResultRow> iter = data.iterator(); iter.hasNext();) {
			SimpleResultRow row = iter.next();

			params.clear();
			for (Iterator<String> i = allColumns.iterator(); i.hasNext();) {
				String columnName = i.next();

				params.add(row.getObject(columnName));
			}
			DB.update(getHandle(), insertSql, params.toArray(), columnTypes, null, true);
		}
	}

	/**
	 * get versioned data at a given timestamp
	 * @param idData set of objects identifying the versioned records
	 * @param time timestamp for selecting the data (may be -1 to denote the
	 *        current version)
	 * @return resultprocessor holding the data at the given timestamp
	 */
	public SimpleResultProcessor getVersionData(Object[] idData, int time) throws NodeException {
		return getVersionData(idData, time, false, false, true);
	}

	/**
	 * get versioned data at a given timestamp TODO: rewrite statements using
	 * subqueries (should be more efficient)
	 * @param idData set of objects identifying the versioned records
	 * @param time timestamp for selecting the data (may be -1 to denote the
	 *        current version)
	 * @param sortIds true when the results shall be sorted by id, false if not
	 * @param addNodeversionTimestamps true when the nodeversiontimestamps shall be added
	 * @param addNonVersionedData true to add non versioned data, false if not
	 * @return resultprocessor holding the data at the given timestamp
	 */
	public SimpleResultProcessor getVersionData(Object[] idData, int time, boolean sortIds, boolean addNodeversionTimestamps, boolean addNonVersionedData)
			throws NodeException {
		SimpleResultProcessor retval = new SimpleResultProcessor();

		if (time < 0) {
			// fetch the data from the current version
			String sql = "SELECT gentics_main.* FROM " + getFromPart(false) + " WHERE " + getWherePart();

			if (sortIds) {
				sql += " ORDER BY gentics_main.id";
			}
			try {
				DB.query(getHandle(), sql, idData, retval);
			} catch (SQLException e) {
				throw new NodeException("Error while getting versioned data of table {" + table + "}", e);
			}
		} else {
			String sql = "SELECT gentics_main.id, max(gentics_main.nodeversiontimestamp) nodeversionrtime" + " FROM " + getFromPart()
					+ " WHERE (nodeversionremoved >" + time + " OR nodeversionremoved = 0) AND nodeversiontimestamp <=" + time + " AND (" + getWherePart()
					+ ") GROUP BY id";

			if (sortIds) {
				sql += " ORDER BY id";
			}

			try {
				SimpleResultProcessor rsVersions = new SimpleResultProcessor();
				DB.query(getHandle(), sql, idData, rsVersions);

				Map<Integer, Integer> versionTimestampMap = new HashMap<Integer, Integer>(rsVersions.size());
				List<Integer> ids = new ArrayList<Integer>(rsVersions.size());
				for (SimpleResultRow row : rsVersions) {
					versionTimestampMap.put(row.getInt("id"), row.getInt("nodeversionrtime"));
					ids.add(row.getInt("id"));
				}

				fetchVersionedData(ids, versionTimestampMap, retval, addNodeversionTimestamps, addNonVersionedData);
			} catch (SQLException ex) {
				throw new NodeException("Error while getting versioned data of table {" + table + "}", ex);
			}
		}

		return retval;
	}

	/**
	 * Restore a given version.
	 * @param id id of the record to be restored
	 * @param time time of the version to be restored
	 * @throws NodeException 
	 */
	public void restoreVersion(Long id, int time) throws NodeException {
		restoreVersion(new Object[] { id}, time);
	}

	/**
	 * get a list of existing versions of a dataset.
	 * @param idParams the unique idParams of the dataset.
	 * @return array of versions, sorted by timestamp asc.
	 * @throws NodeException
	 */
	public Version[] getVersions(Object[] idParams) throws NodeException {
		List<Version> versions = getVersionsList(idParams);

		return versions.toArray(new Version[versions.size()]);
	}

	/**
	 * get a list of existing versions of a dataset.
	 * @param idParams the unique idParams of the dataset.
	 * @return List of versions, sorted by timestamp asc.
	 */
	public List<Version> getVersionsList(Object[] idParams) throws NodeException {
		List<Version> versions = new ArrayList<>();

		String sql = "SELECT gentics_main.nodeversiontimestamp timestamp, gentics_main.nodeversion_user user_id, count(*) diffcount"
				+ " FROM " + getFromPart();

		sql += " WHERE " + wherePart + " GROUP BY gentics_main.nodeversiontimestamp, gentics_main.nodeversion_user"
				+ " ORDER BY gentics_main.nodeversiontimestamp";

		try {
			SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

			DB.query(getHandle(), sql, idParams, resultProcessor);

			for (Iterator<SimpleResultRow> iter = resultProcessor.iterator(); iter.hasNext();) {
				SimpleResultRow row = iter.next();
				Version version = new Version();

				version.setTimestamp(row.getInt("timestamp"));
				version.setUser(row.getString("user_id"));
				version.setDiffCount(row.getInt("diffcount"));
				version.setAutoupdate(false);
				versions.add(version);
			}
		} catch (SQLException ex) {
			throw new NodeException("Error while getting versions list of table {" + table + "}", ex);
		}
		return versions;
	}

	/**
	 * get a list of existing versions of a dataset.
	 * @param id the unique id of the dataset.
	 * @return array of versions, sorted by timestamp asc.
	 * @throws NodeException
	 */
	public Version[] getVersions(String id) throws NodeException {
		return getVersions(new Object[] { id});
	}

	public List<Version> getVersionsList(String id) throws NodeException {
		return getVersionsList(new Object[] { id});
	}

	public Version[] getVersions(Long id) throws NodeException {
		return getVersions(new Object[] { id});
	}

	public List<Version> getVersionsList(Long id) throws NodeException {
		return getVersionsList(new Object[] { id});
	}

	public String getTable() {
		return table;
	}

	/**
	 * Get all joins
	 * @return joins
	 */
	public List<Join> getJoins() {
		return joins;
	}

	public String getWherePart(boolean stripAlias) {
		if (stripAlias) {
			return wherePart.replaceAll("gentics_main\\.", "");
		} else {
			return wherePart;
		}
	}

	public String getWherePart() {
		return getWherePart(false);
	}

	/**
	 * get the from part for the nodeversion table
	 * @return from part (excluding the word "FROM")
	 */
	private String getFromPart() {
		return getFromPart(true);
	}

	/**
	 * get the from part for the original or nodeversion table
	 * @param versionTable true when the nodeversion table shall be used
	 * @return from part (excluding the word "FROM")
	 */
	private String getFromPart(boolean versionTable) {
		StringBuilder fromPart = new StringBuilder("`");

		fromPart.append(table);

		if (versionTable) {
			fromPart.append("_nodeversion");
		}

		fromPart.append("`")
			.append(" gentics_main");

		for (Join join : joins) {
			fromPart.append(" ").append(join.getLeftJoin());
		}

		return fromPart.toString();
	}

	/**
	 * create initial versions of all records without version
	 * @param timestamp timestamp of the initial version
	 * @param userId user id of the user for the initial version
	 * @return the number of records inserted into the version table
	 * @throws NodeException
	 */
	public int createInitialVersions(int timestamp, String userId) throws NodeException {
		return createInitialVersions(timestamp, userId, null, null);
	}

	/**
	 * create initial versions of all records without version, filtered by a
	 * given whereClause and parameters
	 * @param timestamp timestamp of the initial version
	 * @param userId user id of the user for the initial version
	 * @param whereClause where clause (may be null)
	 * @param params filter parameters
	 * @return the number of records inserted into the version table
	 */
	public int createInitialVersions(int timestamp, String userId, String whereClause,
			Object[] params) throws NodeException {
		Integer timeInt = new Integer(timestamp);
		String insertQuery = "INSERT INTO `" + table + "_nodeversion` (" + columnsList
				+ ",nodeversiontimestamp, nodeversion_user, nodeversionlatest, nodeversionremoved) "
				+ "SELECT " + columnsList + ", ?, ?, 1, 0 FROM `" + table + "` WHERE id in (select " + table + ".id from `" + table
				+ "` left join `" + table + "_nodeversion` on (" + table + ".id = " + table + "_nodeversion.id) where " + table + "_nodeversion.id is null";
		Object[] filterParams = null;

		if (whereClause != null) {
			insertQuery += " and (" + whereClause + ")";
			filterParams = new Object[2 + (params != null ? params.length : 0)];
			filterParams[0] = timeInt;
			filterParams[1] = userId;
			if (params != null) {
				System.arraycopy(params, 0, filterParams, 2, params.length);
			}
		} else {
			filterParams = new Object[] { timeInt, userId};
		}
		insertQuery += ")";
		try {
			return DB.update(getHandle(), insertQuery, filterParams, null);
		} catch (SQLException e) {
			throw new NodeException("error while creating initial versions in table {" + table + "}", e);
		}
	}

	/**
	 * Get the number of records without any versioning information
	 * @return number of records without any versioning information
	 * @throws SQLException
	 */
	public int getRecordsWithoutVersions() throws SQLException, NodeException {
		String countQuery = "select count(*) c from `" + table + "` left join `" + table + "_nodeversion` on (" + table + ".id = " + table
				+ "_nodeversion.id) where " + table + "_nodeversion.id is null";
		SimpleResultProcessor result = new SimpleResultProcessor();

		DB.query(getHandle(), countQuery, new Object[0], result);
		Iterator<SimpleResultRow> iter = result.iterator();

		if (iter.hasNext()) {
			SimpleResultRow row = (SimpleResultRow) iter.next();

			return row.getInt("c");
		} else {
			return 0;
		}
	}

	/**
	 * Set the "latest" flag to the newest (but past) version of every row, remove the flag from all other versions
	 * @param idParams ID parameters
	 * @throws SQLException
	 * @throws NodeException
	 */
	private void setLatestFlag(Object[] idParams) throws SQLException, NodeException {
		SimpleResultProcessor versionData = getVersionData(idParams, (int) ((System.currentTimeMillis() / 1000)), false, true, true);

		for (Iterator<SimpleResultRow> iter = versionData.iterator(); iter.hasNext();) {
			SimpleResultRow row = (SimpleResultRow) iter.next();

			update("nodeversionlatest = ?", new Object[] { 1 }, "id = ? AND nodeversiontimestamp = ?",
					new Object[] { row.getInt("id"), row.getInt("nodeversiontimestamp") });
			update("nodeversionlatest = ?", new Object[] { 0 }, "id = ? AND nodeversiontimestamp != ?",
					new Object[] { row.getInt("id"), row.getInt("nodeversiontimestamp") });
		}
	}

	/**
	 * Create a unique, current version of the selected dataset at the timestamp
	 * 0
	 * @param idParams parameters identifying the dataset
	 * @throws NodeException
	 */
	public void createUniqueVersion(Object[] idParams, String userId) throws NodeException {
		try {
			// remove all existing versions for the idparams
			delete(getWherePart(true), idParams);
			createVersion(idParams, 0, userId);
		} catch (SQLException ex) {
			throw new NodeException("Error while creation of unique version in table {" + table + "}", ex);
		}
	}

	/**
	 * Get the complete diff between two versions, each of the version
	 * timestamps may be -1 to denote the current version.
	 * @param timestamp1 first version timestamp
	 * @param timestamp2 second version timestamp
	 * @return list of different records
	 * @throws NodeException
	 */
	public List<Diff> getDiff(Object[] idParams, int timestamp1, int timestamp2) throws NodeException {
		SimpleResultProcessor firstData = getVersionData(idParams, timestamp1, true, false, false);
		SimpleResultProcessor secondData = getVersionData(idParams, timestamp2, true, false, false);
		List<Diff> diff = new Vector<Diff>();

		Iterator<SimpleResultRow> firstIt = firstData.iterator();
		Iterator<SimpleResultRow> secondIt = secondData.iterator();

		boolean getFirst = true;
		boolean getSecond = true;
		SimpleResultRow firstRow = null;
		SimpleResultRow secondRow = null;

		while (getFirst || getSecond) {
			if (getFirst) {
				getFirst = false;
				if (firstIt.hasNext()) {
					firstRow = firstIt.next();
				} else {
					firstRow = null;
				}
			}
			if (getSecond) {
				getSecond = false;
				if (secondIt.hasNext()) {
					secondRow = secondIt.next();
				} else {
					secondRow = null;
				}
			}

			if (firstRow == null && secondRow != null) {
				// secondRow is new
				diff.add(new Diff(secondRow.getInt("id"), Diff.DIFFTYPE_ADD, null, null, secondRow));
				getSecond = true;
			} else if (firstRow != null && secondRow == null) {
				// firstRow is removed
				diff.add(new Diff(firstRow.getInt("id"), Diff.DIFFTYPE_DEL, null, firstRow, null));
				getFirst = true;
			} else if (firstRow != null && secondRow != null) {
				// compare the ids
				int firstId = firstRow.getInt("id");
				int secondId = secondRow.getInt("id");

				if (firstId < secondId) {
					// firstRow is removed
					diff.add(new Diff(firstRow.getInt("id"), Diff.DIFFTYPE_DEL, null, firstRow, null));
					getFirst = true;
				} else if (firstId == secondId) {
					List<String> modifiedCols = new Vector<String>();

					// calc diff
					for (Iterator<String> iter = tableColumns.iterator(); iter.hasNext();) {
						String colName = iter.next();
						Object firstValue = firstRow.getObject(colName);
						Object secondValue = secondRow.getObject(colName);

						if (firstValue == null) {
							if (secondValue != null) {
								modifiedCols.add(colName);
							}
						} else {
							if (!firstValue.equals(secondValue)) {
								modifiedCols.add(colName);
							}
						}
					}

					// row was modified
					if (modifiedCols.size() > 0) {
						diff.add(new Diff(firstRow.getInt("id"), Diff.DIFFTYPE_MOD, modifiedCols.toArray(new String[modifiedCols.size()]), firstRow, secondRow));
					}

					getFirst = true;
					getSecond = true;
				} else {
					// secondRow is new
					diff.add(new Diff(secondRow.getInt("id"), Diff.DIFFTYPE_ADD, null, null, secondRow));
					getSecond = true;
				}
			}
		}

		return diff;
	}

	/**
	 * Purge Versions older than the given timestamp
	 * @param id id
	 * @param timestamp timestamp
	 * @throws NodeException
	 */
	public void purgeVersions(long id, int timestamp) throws NodeException {
		purgeVersions(new Object[] { id}, timestamp);
	}

	/**
	 * Purge Versions older than the given timestamp
	 * @param idParams id parameters
	 * @param timestamp timestamp
	 * @throws NodeException
	 */
	public void purgeVersions(Object[] idParams, int timestamp) throws NodeException {
		try {
			// first get all affected records
			// get ids of rows to restore
			StringBuffer sql = new StringBuffer("SELECT gentics_main.id, max(gentics_main.nodeversiontimestamp) nodeversionrtime");

			sql.append(" FROM ").append(getFromPart()).append(" WHERE (nodeversiontimestamp <= ").append(timestamp).append(" AND ");
			sql.append(getWherePart()).append(") GROUP BY id");

			SimpleResultProcessor rsVersions = new SimpleResultProcessor();

			DB.query(getHandle(), sql.toString(), idParams, rsVersions);

			// now move the latest records to the given timestamp
			String updateSQL = new StringBuffer("UPDATE `").append(getTable()).append("_nodeversion` SET nodeversiontimestamp = ? WHERE id = ? AND nodeversiontimestamp = ?").toString();
			List<Integer> ids = new Vector<Integer>(rsVersions.size());

			for (Iterator<SimpleResultRow> iter = rsVersions.iterator(); iter.hasNext();) {
				SimpleResultRow row = iter.next();

				// update the nodeversiontimestamp to be the correct one
				DB.update(getHandle(), updateSQL, new Object[] { timestamp, row.getInt("id"), row.getInt("nodeversionrtime")});
				ids.add(row.getInt("id"));
			}

			// finally remove too old versions
			if (ids.size() > 0) {
				String deleteSQL = new StringBuffer("DELETE FROM `").append(getTable()).append("_nodeversion` WHERE nodeversiontimestamp < ? AND id IN (").append(StringUtils.repeat("?", ids.size(), ",")).append(")").toString();
				Object[] params = new Object[ids.size() + 1];

				params[0] = timestamp;
				System.arraycopy(ids.toArray(), 0, params, 1, ids.size());
				DB.update(getHandle(), deleteSQL, params);
			}

		} catch (SQLException e) {
			throw new NodeException("Error while purging versions", e);
		} finally {}
	}

	/**
	 * Makes a concatenated SQL list containing the given number of question marks. 
	 * 
	 * @param count
	 * 		  The number of question marks the returned list should contain.
	 * @return
	 * 		  For example, if the given count is 3, "?, ?, ?".
	 */
	private String makeQuestionMarks(int count) {
		StringBuilder sql = new StringBuilder();
		boolean first = true;

		while (0 < count--) {
			if (first) {
				first = false;
			} else {
				sql.append(", ");
			}
			sql.append("?");
		}
		return sql.toString();
	}
    
	/**
	 * Makes a concatenated SQL list from the given columnNames and table.
	 * 
	 * @param columnNames
	 * 		  The column names that make up the list.
	 * @param table
	 * 		  An optional prefix to qualify the column names.
	 * @return
	 * 		  For example
	 * 			"name1, name2, name3"
	 * 		  or, if table is not null,
	 * 			"table.name1, table.name2, table.name3".
	 */
	private String makeColumnsSql(List<String> columnNames, String table) {
		boolean first = true;
		StringBuilder sql = new StringBuilder();

		for (String columnName : columnNames) {
			if (first) {
				first = false;
			} else {
				sql.append(", ");
			}
			if (null != table) {
				sql.append(table).append(".");
			}
			sql.append(columnName);
		}
		return sql.toString();
	}

	/**
	 * Like {@link #makeColumnsSql(List, String)} except that null is always passed
	 * so the column names in the returnd list will never be qualified.
	 */
	private String makeColumnsSql(List<String> columnNames) {
		return makeColumnsSql(columnNames, null);
	}

	/**
	 * Set whether the nodeversion table is supposed to contain a auto_increment column
	 * @param autoIncrement true if the table has the auto_id column
	 */
	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	/**
	 * Get true if the table is supposed to have a auto_increment column
	 * @return true for auto_id column
	 */
	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	/**
	 * Set a restore processor instance
	 * @param restoreProcessor restore processor instance
	 */
	public void setRestoreProcessor(TableVersionRestoreProcessor restoreProcessor) {
		this.restoreProcessor = restoreProcessor;
	}

	/**
	 * Perform an update on the _nodeversion table
	 * @param set set part of the SQL Statement (not including the SET keyword)
	 * @param setParams parameters used in the set part
	 * @param where where part of the SQL Statement (not including the WHERE keyword)
	 * @param whereParams parameters used in the where part
	 * @throws SQLException
	 */
	protected void update(String set, Object[] setParams, String where, Object[] whereParams) throws SQLException, NodeException {
		if (autoIncrement) {
			AutoIdCollector autoIds = new AutoIdCollector();

			DB.query(getHandle(), "SELECT auto_id FROM `" + table + "_nodeversion` WHERE " + where, whereParams, autoIds);
			if (autoIds.size() > 0) {
				DB.update(getHandle(),
						"UPDATE `" + table + "_nodeversion` SET " + set + " WHERE auto_id IN (" + StringUtils.repeat("?", autoIds.size(), ",") + ")",
						merge(setParams, autoIds.getObjectArray()));
			}
		} else {
			DB.update(getHandle(), "UPDATE `" + table + "_nodeversion` SET " + set + " WHERE " + where, merge(setParams, whereParams));
		}
	}

	/**
	 * Perform a delete from the _nodeversion table
	 * @param where where part
	 * @param whereParams
	 * @throws SQLException
	 */
	protected void delete(String where, Object[] whereParams) throws SQLException, NodeException {
		if (autoIncrement) {
			// when the _nodeversion table contains a auto_id column, we will use it to delete the entries
			AutoIdCollector autoIds = new AutoIdCollector();

			DB.query(getHandle(), "SELECT auto_id FROM `" + getTable() + "_nodeversion` WHERE " + where, whereParams, autoIds);
			if (autoIds.size() > 0) {
				DB.update(getHandle(), "DELETE FROM `" + getTable() + "_nodeversion` WHERE auto_id IN (" + StringUtils.repeat("?", autoIds.size(), ",") + ")",
						autoIds.getObjectArray());
			}
		} else {
			DB.update(getHandle(), "DELETE FROM `" + getTable() + "_nodeversion` WHERE " + where, whereParams, null);
		}
	}

	/**
	 * Merge the two given arrays into a single array
	 * @param array1 first array
	 * @param array2 second array
	 * @return merged array
	 */
	protected Object[] merge(Object[] array1, Object[] array2) {
		if (array1 == null) {
			array1 = new Object[0];
		}
		if (array2 == null) {
			array2 = new Object[0];
		}
		Object[] merged = new Object[array1.length + array2.length];

		if (array1.length > 0) {
			System.arraycopy(array1, 0, merged, 0, array1.length);
		}
		if (array2.length > 0) {
			System.arraycopy(array2, 0, merged, array1.length, array2.length);
		}
		return merged;
	}

	/**
	 * Class representing a versiondiff information
	 */
	public static class Diff {

		/**
		 * constant for difftype "add" (record was added)
		 */
		public final static int DIFFTYPE_ADD = 1;

		/**
		 * constant for difftype "mod" (record was modified)
		 */
		public final static int DIFFTYPE_MOD = 2;

		/**
		 * constant for difftype "del" (record was deleted)
		 */
		public final static int DIFFTYPE_DEL = 3;

		/**
		 * difftype
		 */
		protected int diffType;

		/**
		 * id of the record
		 */
		protected int id;

		/**
		 * modified columns (empty if {@link #diffType} is not
		 * {@link #DIFFTYPE_MOD}.
		 */
		protected String[] modColumns;

		/**
		 * data of the old record (null if {@link #diffType} is {@link #DIFFTYPE_ADD})
		 */
		protected SimpleResultRow oldData;

		/**
		 * data of the new record (null if {@link #diffType} is {@link #DIFFTYPE_DEL})
		 */
		protected SimpleResultRow newData;

		/**
		 * Create an instance of Diff
		 * @param id id of the record
		 * @param diffType difftype
		 * @param modColumns modified columns
		 * @param oldData data of the old record
		 * @param newData data of the new record
		 */
		public Diff(int id, int diffType, String[] modColumns, SimpleResultRow oldData, SimpleResultRow newData) {
			this.id = id;
			this.diffType = diffType;
			this.modColumns = modColumns;
			this.oldData = oldData;
			this.newData = newData;
		}

		/**
		 * Get the difftype
		 * @return difftype
		 */
		public int getDiffType() {
			return diffType;
		}

		/**
		 * Get the record id
		 * @return record id
		 */
		public int getId() {
			return id;
		}

		/**
		 * Get the modified columns (or null if {@link #diffType} is not
		 * {@link #DIFFTYPE_MOD}).
		 * @return modified columns
		 */
		public String[] getModColumns() {
			return modColumns;
		}
	}

	/**
	 * ResultProcessor implementation that will collect the auto_id entries from a query
	 */
	public static class AutoIdCollector implements ResultProcessor {

		/**
		 * Collected autoIds
		 */
		protected List<Integer> autoIds = new ArrayList<Integer>();

		/**
		 * Get the auto IDs
		 * @return list of auto IDs
		 */
		public List<Integer> getAutoIds() {
			return autoIds;
		}

		/**
		 * Get the size
		 * @return size
		 */
		public int size() {
			return autoIds.size();
		}

		/**
		 * Get as object array
		 * @return auto IDs as object array
		 */
		public Object[] getObjectArray() {
			return (Object[]) autoIds.toArray(new Object[autoIds.size()]);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		 */
		public void process(ResultSet rs) throws SQLException {
			while (rs.next()) {
				autoIds.add(rs.getInt("auto_id"));
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
		 */
		public void takeOver(ResultProcessor p) {}
	}

	/**
	 * Class for definition of a join, that is necessary to determine versioned records belonging to an object
	 */
	public static class Join {
		/**
		 * Table to which the other table is joined
		 */
		protected String table;

		/**
		 * Column to which the other table is joined
		 */
		protected String column;

		/**
		 * Joined table
		 */
		protected String joinedTable;

		/**
		 * Column by which the joined table is joined
		 */
		protected String joinedColumn;

		/**
		 * LEFT JOIN statement part
		 */
		protected String leftJoin;

		/**
		 * Join clause (e.g. gentics_main.id = contenttag.content_id)
		 */
		protected String joinClause;

		/**
		 * Create an instance for joining another table to the main table (alias gentics_main)
		 * @param column column of the main table
		 * @param joinedTable joined table
		 * @param joinedColumn column of the joined table
		 */
		public Join(String column, String joinedTable, String joinedColumn) {
			this("gentics_main", column, joinedTable, joinedColumn);
		}

		/**
		 * Create an instance for joining another table to a specified table (which may have been joined before)
		 * @param table name of the table
		 * @param column name of the column in the table
		 * @param joinedTable joined table
		 * @param joinedColumn column of the joined table
		 */
		public Join(String table, String column, String joinedTable, String joinedColumn) {
			this.table = table;
			this.column = column;
			this.joinedTable = joinedTable;
			this.joinedColumn = joinedColumn;
			constructClauses();
		}

		/**
		 * Get the join clause (something like e.g. "gentics_main.id = contenttag.content_id")
		 * There will be no trailing or leading spaces
		 * @return join clause
		 */
		public String getJoinClause() {
			return joinClause;
		}

		/**
		 * Get the LEFT JOIN clause (something like e.g. "LEFT JOIN contenttag ON gentics_main.id = contenttag.content_id")
		 * There will be no trailing or leading spaces
		 * @return LEFT JOIN clause
		 */
		public String getLeftJoin() {
			return leftJoin;
		}

		/**
		 * Construct {@link #leftJoin} and {@link #joinClause}
		 */
		protected void constructClauses() {
			StringBuilder str = new StringBuilder();
			str.append(table).append(".").append(column).append(" = ");
			str.append(joinedTable).append(".").append(joinedColumn);
			joinClause = str.toString();

			str = new StringBuilder();
			str.append("LEFT JOIN ").append(joinedTable).append(" on ").append(joinClause);
			leftJoin = str.toString();
		}
	}
}
