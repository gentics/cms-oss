/*
 * @author alexander
 * @date 12.09.2007
 * @version $Id: AbstractContentRepositoryStructure.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ColumnDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ConstraintAction;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ConstraintDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.IndexDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.SQLDatatype;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.TableDefinition;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.DatabaseMetaDataHandler;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

public abstract class AbstractContentRepositoryStructure {

	/**
	 * the logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(AbstractContentRepositoryStructure.class);

	/**
	 * the dbhandle to connect to the content repository
	 */
	protected DBHandle databaseHandle;

	/**
	 * true if database stores uppercase identifiers, false if not
	 */
	protected boolean upperCase;

	/**
	 * true if datasource supports versioning, false if not
	 */
	protected boolean versioning;

	/**
	 * name of the handle
	 */
	protected String handleId;

	/**
	 * true if the datasource is a multichannelling datasource, false if not
	 */
	protected boolean multichannelling;

	/**
	 * The structure definition used to check the repository
	 */
	protected ContentRepositoryStructureDefinition structureDefinition;

	/**
	 * Get the list of SQL statements to create a table
	 * @param tableDefinition the table definition
	 * @return the SQL statements to create the table
	 */
	protected abstract List<String> getTableCreateStatement(TableDefinition tableDefinition);

	/**
	 * Get the list of SQL statements to add a column to a table
	 * @param columnDefinition the column definition
	 * @return the SQL statements to add the column
	 */
	protected abstract List<String> getColumnAddStatement(ColumnDefinition columnDefinition);

	/**
	 * Get the list of SQL statements to modify the column in the table
	 * @param oldColumn the current column definition
	 * @param newColumn the new column definition
	 * @return the SQL statements to modify the column
	 */
	protected abstract List<String> getColumnAlterStatement(ColumnDefinition oldColumn,
			ColumnDefinition newColumn);

	/**
	 * Get the list of SQL statements to add a new index
	 * @param indexDefinition
	 * @return
	 */
	protected abstract List<String> getIndexAddStatement(IndexDefinition indexDefinition);

	/**
	 * Get the list of SQL statements to drop an index
	 * @param indexDefinition
	 * @return
	 */
	protected abstract List<String> getIndexDropStatement(IndexDefinition indexDefinition);

	/**
	 * Get the list of SQL statements to create the given reference constraint
	 * @param existing existing constraint, that needs to be dropped first (may be null)
	 * @param reference reference constraint
	 * @return list of SQL statements
	 */
	protected abstract List<String> getConstraintCreateStatements(ConstraintDefinition existing, ConstraintDefinition reference);

	/**
	 * Create the list of reference tables
	 * @return a map containing all tables of the content repository
	 */
	public Map<String, TableDefinition> getReferenceTables() {
		if (structureDefinition != null) {
			return structureDefinition.getReferenceTables();
		} else {
			logger.error("Could not retrieve reference table structure.");
			return null;
		}
	}

	/**
	 * Create the list of reference constraints
	 * @return list of reference constraints
	 */
	public List<ConstraintDefinition> getReferenceConstraints() {
		if (structureDefinition != null) {
			return structureDefinition.getReferenceConstraints();
		} else {
			logger.error("Could not retrieve reference constraint structure.");
			return null;
		}
	}

	/**
	 * Get the reference table definition
	 * @param tableName the name of the table
	 * @return the reference table definition
	 */
	public TableDefinition getReferenceTable(String tableName) {
		if (tableName == null) {
			return null;
		}
		if (upperCase) {
			tableName = tableName.toUpperCase();
		}
		return (TableDefinition) getReferenceTables().get(tableName);
	}

	/**
	 * True if missing tables can be auto repaired
	 */
	protected boolean canAutorepairMissingTable = false;

	/**
	 * True if missing columns can be auto repaired
	 */
	protected boolean canAutorepairMissingColumn = false;

	/**
	 * True if missing indices can be auto repaired
	 */
	protected boolean canAutorepairMissingIndex = false;

	/**
	 * True if incorrent columns can be auto repaired
	 */
	protected boolean canAutorepairIncorrectColumn = false;

	/**
	 * True if missing indices can be autorepaired
	 */
	protected boolean canAutorepairIndex = false;

	/**
	 * Initialize the object
	 * @param handle the dbhandle
	 * @param handleId the name of the handle
	 * @param upperCase true if database stored uppercase identifiers, false if
	 *        not
	 * @param versioning true if database supports versioning, false if not
	 * @param multichannelling true to make the content repository multichannelling aware
	 */
	protected AbstractContentRepositoryStructure(DBHandle handle, String handleId,
			boolean upperCase, boolean versioning, boolean multichannelling) {
		this.databaseHandle = handle;
		this.handleId = handleId;
		this.upperCase = upperCase;
		this.versioning = versioning;
		this.multichannelling = multichannelling;
		this.structureDefinition = new ContentRepositoryStructureDefinition(databaseHandle, upperCase, versioning, multichannelling);
	}

	/**
	 * Create a new structure check object
	 * @param datasource the datasource
	 * @param handleId the handleid to the datasource
	 * @return a structure check object for the datasource
	 * @throws CMSUnavailableException
	 */
	public static AbstractContentRepositoryStructure getStructure(Datasource datasource,
			String handleId) throws CMSUnavailableException {
		if (datasource == null) {
			throw new CMSUnavailableException("Datasource for Content Repository Consistency Check must not be null.");
		}
		// only check cn datasources
		if (!(datasource instanceof CNDatasource) && !(datasource instanceof MCCRDatasource)) {
			throw new CMSUnavailableException("Content Repository Consistency Check can only check CNDatasources and MCCRDatasources.");
		}

		// check if datasource supports versioning
		boolean versioning = false;

		if (datasource instanceof VersioningDatasource) {
			versioning = ((VersioningDatasource) datasource).isVersioning();
		}

		// check if datasource supports multichannelling
		boolean multichannelling = (datasource instanceof MultichannellingDatasource);

		// get DBHandle for datasource
		DatasourceHandle datasourceHandle = datasource.getHandlePool().getHandle();

		if (!(datasourceHandle instanceof SQLHandle)) {
			throw new CMSUnavailableException("Content Repository Consistency Check was unable to get a valid SQLHandle from datasource.");
		}

		DBHandle dbHandle = ((SQLHandle) datasourceHandle).getDBHandle();

		if (dbHandle == null) {
			throw new CMSUnavailableException("Content Repository Consistency Check was unable to get valid DBHandle from datasource.");
		}

		return getStructure(dbHandle, handleId, versioning, multichannelling);
	}

	/**
	 * Create a new structure check object
	 * @param dbHandle the dbhandle to read the structure from
	 * @param handleId the handleif of the handle
	 * @param versioning true if datasource supports versioning and
	 *        corresponding tables should be checked, false if not
	 * @param multichannelling true to make the content repository multichannelling aware
	 * @return a structure check object for the handle
	 * @throws CMSUnavailableException
	 */
	public static AbstractContentRepositoryStructure getStructure(DBHandle dbHandle,
			String handleId, boolean versioning, boolean multichannelling) throws CMSUnavailableException {
		if (dbHandle == null) {
			throw new CMSUnavailableException("DBHandle for Content Repository Consistency Check must not be null.");
		}
		StructureDefinitionExtractor extractor = new StructureDefinitionExtractor(dbHandle, handleId, versioning, multichannelling);

		try {
			DB.handleDatabaseMetaData(dbHandle, extractor);
			return extractor.getStructure();
		} catch (SQLException | NodeException e) {
			throw new CMSUnavailableException("Could not create repository structure for given database handle.", e);
		}
	}

	/**
	 * Check the structure consistency of the repository
	 * @param autoRepair true if autorepair of incorrect structure should be
	 *        performed, false if not
	 * @return true if structure is valid, false if not
	 * @throws CMSUnavailableException
	 */
	public boolean checkStructureConsistency(boolean autoRepair) throws CMSUnavailableException {
		if (databaseHandle == null) {
			throw new CMSUnavailableException("Cannot check contentrepository structure, database handle is null.");
		}
		logger.info("Starting structure consistency check for database handle {" + handleId + "}.");
		Map<String, TableDefinition> referenceTables = getReferenceTables();
		Map<String, TableDefinition> checkTableDefinitions = getTableDefinitions(referenceTables);
		boolean check = checkStructureConsistency(checkTableDefinitions, referenceTables, getReferenceConstraints(), autoRepair);

		if (check) {
			logger.info("Finished structure consistency check for database handle {" + handleId + "}, structure valid.");
		} else {
			logger.error("Finished structure consistency check for database handle {" + handleId + "}, structure invalid.");
			logger.error(
					"Please find and use the latest content repository structure dumps in the directory documentation/database of your update/install package.");
		}
		return check;
	}

	/**
	 * Create the specified table
	 * @param referenceTable the table to create
	 * @return a list of open statements if any statements failed
	 */
	protected List<String> createTable(TableDefinition referenceTable) {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to autorepair table {" + referenceTable.getTableName() + "}.");
		}
		List<String> createStatements = getTableCreateStatement(referenceTable);

		for (Iterator<String> iterator = createStatements.iterator(); iterator.hasNext();) {
			String statement = iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error("Autorepair of table {" + referenceTable.getTableName() + "} failed. Statement {" + statement + "} failed.", sqle);
			}
		}
		return createStatements;
	}

	/**
	 * Create the specified column
	 * @param referenceColumn the column to create
	 * @return a list of open statements if any statements failed
	 */
	protected List<String> createColumn(ColumnDefinition referenceColumn) {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to autorepair column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "}.");
		}
		List<String> updateStatements = getColumnAddStatement(referenceColumn);

		// boolean success = true;
		for (Iterator<String> iterator = updateStatements.iterator(); iterator.hasNext();) {
			String statement = (String) iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error(
						"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "} failed. Statement {"
						+ statement + "} failed.",
						sqle);
			}
		}
		return updateStatements;
	}

	/**
	 * Alter the specified column
	 * @param checkColumn the column as it is currently in the database
	 * @param referenceColumn the column as it should be
	 * @return a list of open statements if any statements failed
	 */
	protected List<String> alterColumn(ColumnDefinition checkColumn, ColumnDefinition referenceColumn) {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to autorepair column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "}.");
		}
		List<String> updateStatement = getColumnAlterStatement(checkColumn, referenceColumn);

		for (Iterator<String> iterator = updateStatement.iterator(); iterator.hasNext();) {
			String statement = (String) iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error(
						"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "} failed. Statement {"
						+ statement + "} failed.",
						sqle);
			}
		}
		return updateStatement;
	}

	/**
	 * Create the specified index
	 * @param referenceIndex the definition of the index
	 * @return the open statements if any statements failed
	 */
	protected List<String> createIndex(IndexDefinition referenceIndex) {
		logger.debug("Trying to autorepair index {" + referenceIndex.getIndexName() + "} in table {" + referenceIndex.getTableName() + "}.");
		List<String> updateStatement = getIndexAddStatement(referenceIndex);

		for (Iterator<String> iterator = updateStatement.iterator(); iterator.hasNext();) {
			String statement = (String) iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error(
						"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + referenceIndex.getTableName() + "} failed. Statement {" + statement
						+ "} failed.",
						sqle);
			}
		}
		return updateStatement;
	}

	/**
	 * Drop the existing index
	 * @param conflictingIndex the conflicting existing index
	 * @return the open statements if any statements failed
	 */
	protected List<String> dropIndex(IndexDefinition conflictingIndex) {
		logger.debug("Trying to drop index {" + conflictingIndex.getIndexName() + "} in table {" + conflictingIndex.getTableName() + "}.");
		List<String> statements = getIndexDropStatement(conflictingIndex);

		for (Iterator<String> iterator = statements.iterator(); iterator.hasNext();) {
			String statement = (String) iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error(
						"Autorepair of index {" + conflictingIndex.getIndexName() + "} in table {" + conflictingIndex.getTableName() + "} failed. Statement {"
						+ statement + "} failed.",
						sqle);
			}
		}
		return statements;
	}

	/**
	 * Attempt to create the given constraint. If an existing constraint is given (not null), try to drop that first
	 * @param existing existing constraint (may be null)
	 * @param reference reference constraint
	 * @return the open statements if any statements failed
	 */
	protected List<String> createConstraint(ConstraintDefinition existing, ConstraintDefinition reference) {
		if (logger.isDebugEnabled()) {
			if (existing != null) {
				logger.debug("Trying to recreate constraint {" + reference.getConstraintName() + ".");
			} else {
				logger.debug("Trying to create constraint {" + reference.getConstraintName() + ".");
			}
		}
		List<String> statements = getConstraintCreateStatements(existing, reference);

		for (Iterator<String> iterator = statements.iterator(); iterator.hasNext();) {
			String statement = (String) iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error("Autorepair of constraint {" + reference.getConstraintName() + "} failed. Statement {" + statement + "} failed.", sqle);
			}
		}
		return statements;
	}

	/**
	 * Check the structure consistency of a given contentrepository
	 * @param checkTables the tables in the repository to check
	 * @param referenceTables the reference tables
	 * @param referenceConstraints the reference constraints
	 * @param autoRepair true if autorepair should be performed, false if not
	 * @return true if structure is consistent, false if not
	 * @throws CMSUnavailableException if database handle to perform checks on
	 *         is null
	 */
	protected boolean checkStructureConsistency(Map<String, TableDefinition> checkTables, Map<String, TableDefinition> referenceTables,
			List<ConstraintDefinition> referenceConstraints, boolean autoRepair) throws CMSUnavailableException {
		if (databaseHandle == null) {
			throw new CMSUnavailableException("Cannot check contentrepository structure, database handle is null.");
		}

		// store all statements that could not be performed
		List<String> openStatements = new Vector<String>();

		// all tables have passed the check
		boolean checkPassed = true;

		// iterate over all tables in the reference
		for (Iterator<TableDefinition> tableIterator = referenceTables.values().iterator(); tableIterator.hasNext();) {

			// get reference table definition and check table definition
			TableDefinition referenceTable = tableIterator.next();
			// TODO get the existing table from the checkTables map?
			TableDefinition checkTable = getTableDefinition(referenceTable.getTableName());

			boolean checkPassedTable = true;

			logger.debug("Starting consistency check for table {" + referenceTable.getTableName() + "}");

			if (checkTable == null) {
				// table is completely missing in repository
				logger.error("Table {" + referenceTable.getTableName() + "} is missing.");
				if (canAutorepairMissingTable()) {
					// autorepair of missing table is possible
					if (autoRepair) {
						// autorepair is enabled
						List<String> createTableStatements = createTable(referenceTable);

						if (createTableStatements.size() > 0) {
							checkPassedTable = false;
							openStatements.addAll(createTableStatements);
							logger.error("Autorepair of table {" + referenceTable.getTableName() + "} failed.");
						} else {
							logger.error("Autorepair of table {" + referenceTable.getTableName() + "} successful.");
						}
					} else {
						// autorepair is disabled
						logger.error("Autorepair is disabled. Table {" + referenceTable.getTableName() + "} cannot be repaired.");
						checkPassedTable = false;
						openStatements.addAll(getTableCreateStatement(referenceTable));
					}
				} else {
					// autorepair of missing table is impossible
					checkPassedTable = false;
					openStatements.addAll(getTableCreateStatement(referenceTable));
					logger.error("Cannot repair missing table {" + referenceTable.getTableName() + "}. Please create the table manually.");
				}
			} else {
				// table exists, check all primary keys in reference
				// if conflicting primary key found, drop it
				for (IndexDefinition checkIndex : checkTable.getIndices().values()) {
					// get reference index and check index definition
					IndexDefinition referenceIndex = referenceTable.getIndex(checkIndex.getColumnNames());

					if (checkIndex.isPrimary()) {
						if (referenceIndex == null || !referenceIndex.isPrimary()) {
							// index should not be primary key, drop it
							if (canAutorepairIndex()) {
								if (autoRepair) {
									List<String> alterPrimaryKeyStatements = dropIndex(checkIndex);

									if (alterPrimaryKeyStatements.size() > 0) {
										checkPassedTable = false;
										openStatements.addAll(alterPrimaryKeyStatements);
										logger.error(
												"Autorepair of primary key {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName() + "} failed.");
									} else {
										logger.error(
												"Autorepair of primary key {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName()
												+ "} successful.");
									}
								} else {
									// autorepair is disabled
									logger.error(
											"Autorepair is disabled. Primary key {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName()
											+ "} cannot be repaired.");
									openStatements.addAll(getIndexDropStatement(checkIndex));
								}
							} else {
								// autorepair of primary key is not possible
								checkPassedTable = false;
								openStatements.addAll(getIndexDropStatement(checkIndex));
								logger.error(
										"Cannot repair primary key {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName()
										+ "}. Please drop the conflicting primary key manually.");
							}
						}
					}
				}

				// table exists, check all columns in reference
				// re-read table definition in case of changes
				checkTable = getTableDefinition(referenceTable.getTableName());
				for (ColumnDefinition referenceColumn : referenceTable.getColumns().values()) {

					// get reference column and check column definition
					ColumnDefinition checkColumn = checkTable.getColumns().get(referenceColumn.getColumnName());

					if (checkColumn == null) {
						// column is completely missing
						logger.error("Column {" + referenceColumn.getColumnName() + "} in table {" + checkTable.getTableName() + "} is missing.");
						if (canAutorepairMissingColumn()) {
							// autorepair of missing column is possible
							if (autoRepair) {
								// autorepair is enabled
								List<String> createColumnStatements = createColumn(referenceColumn);

								if (createColumnStatements.size() > 0) {
									checkPassedTable = false;
									openStatements.addAll(createColumnStatements);
									logger.error(
											"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "} failed.");
								} else {
									logger.error(
											"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName()
											+ "} successful.");
								}
							} else {
								// autorepair is disabled
								logger.error(
										"Autorepair is disabled. Column {" + referenceColumn.getColumnName() + "} in table {" + referenceTable.getTableName()
										+ "} cannot be repaired.");
								checkPassedTable = false;
								openStatements.addAll(getColumnAddStatement(referenceColumn));
							}
						} else {
							// autorepair of missing column is impossible
							checkPassedTable = false;
							openStatements.addAll(getColumnAddStatement(referenceColumn));
							logger.error(
									"Cannot repair missing column {" + referenceColumn.getColumnName() + "} in table {" + checkTable.getTableName()
									+ "}. Please create the column manually:" + "\nTable:    " + checkTable.getTableName() + "\nColumn:   "
									+ referenceColumn.getColumnName() + "\nType:     " + referenceColumn.getDataType().getSqlTypeName() + "\nDefault:  "
									+ referenceColumn.getDefaultValue() + "\nNullable: " + referenceColumn.isNullable());
						}
					} else if (!referenceColumn.equals(checkColumn)) {
						// column exists, but has wrong format
						logger.error("Column {" + referenceColumn.getColumnName() + "} in table {" + checkTable.getTableName() + "} has wrong format.");
						if (canAutorepairIncorrectColumn()) {
							// autorepair of incorrect column is possible
							if (autoRepair) {
								// autorepair is enabled
								// check if indices need to be updated
								boolean dropIndices = needToRecreateIndex(referenceColumn, checkColumn);
								List<String> dropIndexStatements = new Vector<String>();

								if (dropIndices) {
									dropIndexStatements = dropAllIndicesForColumn(checkTable, checkColumn);
								}
								List<String> updateStatements = alterColumn(checkColumn, referenceColumn);

								if (updateStatements.size() > 0 || dropIndexStatements.size() > 0) {
									checkPassedTable = false;
									openStatements.addAll(updateStatements);
									openStatements.addAll(dropIndexStatements);
									logger.error(
											"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName() + "} failed.");
								} else {
									logger.error(
											"Autorepair of column {" + referenceColumn.getColumnName() + "} in table {" + referenceColumn.getTableName()
											+ "} successful.");
								}
							} else {
								// autorepair disabled
								logger.error(
										"Autorepair is disabled. Column {" + referenceColumn.getColumnName() + "} in table {" + referenceTable.getTableName()
										+ "} cannot be altered.");
								checkPassedTable = false;
								// check if indices need to be updated
								boolean dropIndices = needToRecreateIndex(referenceColumn, checkColumn);

								if (dropIndices) {
									openStatements.addAll(getDropAllIndicesForColumnStatements(checkTable, checkColumn));
								}
								openStatements.addAll(getColumnAlterStatement(checkColumn, referenceColumn));
							}
						} else {
							// autorepair of incorrect column is impossible
							checkPassedTable = false;
							// check if indices need to be updated
							boolean dropIndices = needToRecreateIndex(referenceColumn, checkColumn);

							if (dropIndices) {
								openStatements.addAll(getDropAllIndicesForColumnStatements(checkTable, checkColumn));
							}
							openStatements.addAll(getColumnAlterStatement(checkColumn, referenceColumn));
							logger.error(
									"Cannot repair incorrect column {" + referenceColumn.getColumnName() + "} in table {" + checkTable.getTableName()
									+ "}. Please repair the column manually:" + "\nTable:    " + checkTable.getTableName() + "\nColumn:   "
									+ referenceColumn.getColumnName() + "\nType:     " + referenceColumn.getDataType().getSqlTypeName() + "\nDefault:  "
									+ referenceColumn.getDefaultValue() + "\nNullable: " + referenceColumn.isNullable());
						}
					}
				}

				// table exists, check all indices in reference
				// re-read table definition in case of changes
				checkTable = getTableDefinition(referenceTable.getTableName());
				for (IndexDefinition referenceIndex : referenceTable.getIndices().values()) {

					// get reference index and check index definition
					IndexDefinition checkIndex = checkTable.getIndex(referenceIndex.getColumnNames());

					if (checkIndex == null) {
						// index is missing
						logger.error("Index {" + referenceIndex.getIndexName() + "} in table {" + checkTable.getTableName() + "} is missing.");
						if (canAutorepairMissingIndex()) {
							// autorepair of missing index is possible
							if (autoRepair) {
								// autorepair is enabled
								List<String> statements = createIndex(referenceIndex);

								if (statements.size() > 0) {
									checkPassedTable = false;
									openStatements.addAll(statements);
									logger.error(
											"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + referenceIndex.getTableName() + "} failed.");
								} else {
									logger.error(
											"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + referenceIndex.getTableName() + "} successful.");
								}
							} else {
								// autorepair is disabled
								openStatements.addAll(getIndexAddStatement(referenceIndex));
								checkPassedTable = false;
								logger.error(
										"Autorepair is disabled. Index {" + referenceIndex.getIndexName() + "} in table {" + referenceTable.getTableName()
										+ "} cannot be created.");
							}
						} else {
							// autorepair of index is impossible
							checkPassedTable = false;
							StringBuffer columnNames = new StringBuffer();

							for (Iterator<String> iterator = referenceIndex.getColumnNames().values().iterator(); iterator.hasNext();) {
								String column = (String) iterator.next();

								columnNames.append(column);
								if (iterator.hasNext()) {
									columnNames.append(",");
								}
							}
							openStatements.addAll(getIndexAddStatement(referenceIndex));
							logger.error(
									"Cannot repair missing index {" + referenceIndex.getIndexName() + "} in table {" + checkTable.getTableName()
									+ "}. Please create the index manually:" + "\nIndex:   " + referenceIndex.getIndexName() + "\nColumns: " + columnNames.toString());
						}
					} else if (!referenceIndex.equals(checkIndex)) {
						// index does not equal reference index
						logger.error("Index {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName() + "} is not configured correctly.");
						if (canAutorepairIndex()) {
							// autorepair of index is possible
							if (autoRepair) {
								// autorepair is enabled
								List<String> dropStatements = dropIndex(checkIndex);
								List<String> addStatements = createIndex(referenceIndex);

								if (dropStatements.size() > 0) {
									checkPassedTable = false;
									openStatements.addAll(dropStatements);
									logger.error(
											"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + checkIndex.getTableName() + "} failed.");
								}
								if (addStatements.size() > 0) {
									checkPassedTable = false;
									openStatements.addAll(dropStatements);
									logger.error(
											"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + checkIndex.getTableName() + "} failed.");
								} else {
									logger.error(
											"Autorepair of index {" + referenceIndex.getIndexName() + "} in table {" + checkIndex.getTableName() + "} successful.");
								}
							} else {
								// autorepair disabled
								logger.error(
										"Autorepair is disabled. Index {" + checkIndex.getTableName() + "} in table {" + checkIndex.getTableName()
										+ "} cannot be altered.");
								checkPassedTable = false;
								openStatements.addAll(getIndexDropStatement(checkIndex));
								openStatements.addAll(getIndexAddStatement(referenceIndex));
							}
						} else {
							// autorepair of incorrect column is impossible
							checkPassedTable = false;
							openStatements.addAll(getIndexDropStatement(checkIndex));
							openStatements.addAll(getIndexAddStatement(referenceIndex));
							logger.error(
									"Cannot repair incorrect index {" + checkIndex.getIndexName() + "} in table {" + checkIndex.getTableName()
									+ "}. Please repair the index manually:" + referenceIndex.toString());
						}
					}
				}
			}

			if (checkPassedTable) {
				logger.info("Finished structure consistency check for table {" + referenceTable.getTableName() + "}, structure valid.");
			} else {
				logger.error("Finished structure consistency check for table {" + referenceTable.getTableName() + "}, structure invalid.");
			}
			checkPassed &= checkPassedTable;
		}

		// iterate over all constraints
		for (ConstraintDefinition reference : referenceConstraints) {
			boolean checkPassedConstraint = true;

			// read the existing constraint
			ConstraintDefinition existing = getConstraintDefinition(reference.getTableName(), reference.getForeignTableName(), reference.getConstraintName());

			if (existing == null) {
				// constraint is missing
				logger.error("Constraint {" + reference.getConstraintName() + "} is missing.");
				if (autoRepair) {
					// autorepair is enabled
					List<String> statements = createConstraint(null, reference);

					if (statements.size() > 0) {
						checkPassedConstraint = false;
						openStatements.addAll(statements);
						logger.error("Autorepair of constraint {" + reference.getConstraintName() + " failed.");
					} else {
						logger.error("Autorepair of constraint {" + reference.getConstraintName() + "} successful.");
					}
				} else {
					openStatements.addAll(getConstraintCreateStatements(null, reference));
					checkPassedConstraint = false;
					logger.error("Autorepair is disabled. Constraint {" + reference.getConstraintName() + "} cannot be created.");
				}
			} else if (!reference.equals(existing)) {
				// constraint does not equal reference constraint
				logger.error("Constraint {" + reference.getConstraintName() + "} is not configured correctly.");
				if (autoRepair) {
					// autorepair is enabled
					List<String> fixStatements = createConstraint(existing, reference);

					if (fixStatements.size() > 0) {
						checkPassedConstraint = false;
						openStatements.addAll(fixStatements);
						logger.error("Autorepair of constraint {" + reference.getConstraintName() + "} failed.");
					} else {
						logger.error("Autorepair of constraint {" + reference.getConstraintName() + "} successful.");
					}
				} else {
					// autorepair disabled
					logger.error("Autorepair is disabled. Constraint {" + reference.getConstraintName() + "} cannot be altered.");
					checkPassedConstraint = false;
					openStatements.addAll(getConstraintCreateStatements(existing, reference));
				}
			}

			checkPassed &= checkPassedConstraint;
		}

		// if any of the checks failed
		if (!checkPassed) {
			StringBuffer buffer = new StringBuffer();

			for (String statement : openStatements) {
				buffer.append("\n").append(statement).append(";");
			}
			if (autoRepair) {
				// autorepair is enabled, but repair failed
				logger.error("Structure of contentrepository is invalid and autorepair failed.");
				if (buffer.length() > 0) {
					logger.error("Run the following statements to repair the structure of the contentrepository:\n" + buffer.toString());
				}
			} else {
				// autorepair is disabled, structure is invalid
				logger.error("Structure of contentrepository is invalid and autorepair is disabled.");
				if (buffer.length() > 0) {
					logger.error("Run the following statements to repair the structure of the contentrepository:\n" + buffer.toString());
				}

			}
		}

		return checkPassed;
	}

	/**
	 * Check data consistency for the contentrepository
	 * @param autoRepair true if autorepair of data is enabled, false if not
	 * @return true if repair was successful, false if not
	 * @throws CMSUnavailableException if databasehandle for contentrepository
	 *         is null
	 */
	public boolean checkDataConsistency(boolean autoRepair) throws CMSUnavailableException {
		if (databaseHandle == null) {
			throw new CMSUnavailableException("Cannot check contentrepository structure, database handle is null.");
		}

		logger.info("Starting data consistency check for database handle {" + handleId + "}.");
		boolean check = true;

		// check the id counter
		if (!multichannelling) {
			check &= checkIdCounter(handleId, autoRepair);
		}

		// check the quick columns
		check &= checkQuickColumns(autoRepair);
		// TODO
		// delete quick columns if not needed
		// also for nodeversion ??

		// check conflicts in contentattributetype
		if (!multichannelling) {
			check &= checkContentAttributeTypeConflicts();
		}

		if (check) {
			logger.info("Finished data consistency check for database handle {" + handleId + "}, data is valid.");
		} else {
			logger.error("Finished data consistency check for database handle {" + handleId + "}, data is invalid.");
		}
		return check;
	}

	/**
	 * Check if the change to the column makes a drop of all indices that
	 * include this column necessary
	 * @param referenceColumn the reference column definition
	 * @param checkColumn the current column definition
	 * @return true if all indices that include this column need to be recreated
	 */
	protected boolean needToRecreateIndex(ColumnDefinition referenceColumn,
			ColumnDefinition checkColumn) {
		// recreation of index is needed if the type of the column changes
		if (Collections.disjoint(referenceColumn.getDataType().getSqlType(), checkColumn.getDataType().getSqlType())) {
			return true;
		}
		return false;
	}

	/**
	 * Get a list of all SQL statements needed to drop all index definitions
	 * that include the column
	 * @param table the table for which to drop the indices
	 * @param column the column for which to drop the indices
	 * @return a list of all SQL statements
	 */
	protected List<String> getDropAllIndicesForColumnStatements(TableDefinition table,
			ColumnDefinition column) {
		// get all indices that contain this column
		Collection<IndexDefinition> indices = table.getAllIndices(column);
		// get all drop index statements
		List<String> dropIndexStatements = new Vector<String>();

		for (IndexDefinition indexDefinition : indices) {
			dropIndexStatements.addAll(getIndexDropStatement(indexDefinition));
		}
		return dropIndexStatements;
	}

	/**
	 * Drop all indices that contain the column
	 * @param table the table for which to drop the indices
	 * @param column the column for which to drop all indices
	 * @return a list of open statements if any statements failed
	 */
	protected List<String> dropAllIndicesForColumn(TableDefinition table, ColumnDefinition column) {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to autorepair indices for column {" + column.getColumnName() + "} in table {" + column.getTableName() + "}.");
		}
		List<String> dropIndexStatements = getDropAllIndicesForColumnStatements(table, column);

		for (Iterator<String> iterator = dropIndexStatements.iterator(); iterator.hasNext();) {
			String statement = iterator.next();

			try {
				DB.update(databaseHandle, statement, null, null, false);
				iterator.remove();
			} catch (SQLException sqle) {
				logger.error(
						"Autorepair of indices for column {" + column.getColumnName() + "} in table {" + column.getTableName() + "} failed. Statement {" + statement
						+ "} failed.",
						sqle);
			}
		}
		return dropIndexStatements;
	}

	/**
	 * Update data for quick columns
	 * @param quickColumn the quick column definition
	 * @param attributeType the attribute type
	 * @param attributeName the name of the attribute
	 * @return true if repair was sucessful, false if not
	 */
	protected boolean updateQuickColumnData(ColumnDefinition quickColumn, int attributeType,
			String attributeName) {
		// update values for the optimized column
		try {
			DB.update(databaseHandle,
					"UPDATE " + databaseHandle.getContentMapName() + " SET " + quickColumn.getColumnName() + " = (SELECT "
					+ DatatypeHelper.getTypeColumn(attributeType) + " FROM " + databaseHandle.getContentAttributeName() + " a WHERE a.contentid = "
					+ databaseHandle.getContentMapName() + ".contentid AND a.name = ? AND (a.sortorder = ? OR a.sortorder IS NULL))",
					new Object[] { attributeName, new Integer(1)});
			return true;
		} catch (SQLException sqle) {
			logger.error(
					"Could not update quick column data for quick column {" + quickColumn.getColumnName() + "} in table {" + quickColumn.getTableName() + "}.");
			return false;
		}
	}

	/**
	 * Check consistency of quick columns. Create missing quick columns for
	 * optimized values. Repair incorrect columns. Create necessary indices.
	 * @param autoRepair true if autorepair is enabled, false if not
	 * @return true if repair was sucessful, false if not
	 */
	protected boolean checkQuickColumns(boolean autoRepair) {
		logger.debug("Start checking quick columns.");
		List<String> openStatements = new Vector<String>();
		boolean complete = true;
		SimpleResultProcessor result = new SimpleResultProcessor();

		// check content map
		String contentMapName = databaseHandle.getContentMapName();

		if (upperCase) {
			contentMapName = contentMapName.toUpperCase();
		}

		try {
			TableDefinition contentMap = getTableDefinition(contentMapName);
			TableDefinition contentMapNodeversion = getTableDefinition("contentmap_nodeversion");

			// select all optimized values
			String typeColumn = "attributetype";

			if (multichannelling) {
				typeColumn = "type";
			}

			DB.query(databaseHandle,
					"SELECT DISTINCT name, quickname, " + typeColumn + " FROM " + databaseHandle.getContentAttributeTypeName() + " WHERE optimized = ?",
					new Object[] { Boolean.valueOf(true)}, result);
			// loop over attributetypes with set quick column
			for (SimpleResultRow row : result) {
				int attributeType = row.getInt(typeColumn);
				String quickName = row.getString("quickname");
				String attributeName = row.getString("name");

				// get quick column and index definition for given attribute
				// type
				Object[] reference = getQuickColumnDefinition(attributeType, quickName, false);
				ColumnDefinition referenceColumn = (ColumnDefinition) reference[0];
				IndexDefinition referenceIndex = (IndexDefinition) reference[1];

				// get quick column and index definition for given attribute
				// type in contentmap_nodeversion table
				Object[] referenceNodeversion = getQuickColumnDefinition(attributeType, quickName, true);
				ColumnDefinition referenceColumnNodeversion = versioning ? (ColumnDefinition) referenceNodeversion[0] : null;
				IndexDefinition referenceIndexNodeversion = versioning ? (IndexDefinition) referenceNodeversion[1] : null;

				// check quick column
				if (referenceColumn != null) {

					ColumnDefinition checkColumn = contentMap.getColumn(quickName);

					if (checkColumn != null) {
						if (!referenceColumn.equals(checkColumn)) {
							// quick column exists, but wrong format
							logger.error("Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} has wrong format.");
							if (autoRepair) {
								// autorepair is enabled
								List<String> statements = alterColumn(checkColumn, referenceColumn);

								if (statements.size() > 0) {
									complete = false;
									openStatements.addAll(statements);
									logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} failed.");
								} else {
									logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} successful.");
								}
							} else {
								// autorepair is disabled
								complete = false;
								openStatements.addAll(getColumnAlterStatement(checkColumn, referenceColumn));
								logger.error(
										"Autorepair is disabled. Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} not repaired.");
							}
						}
					} else {
						// quick column is missing
						logger.error("Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} is missing.");
						if (autoRepair) {
							// autorepair is enabled
							List<String> statements = createColumn(referenceColumn);

							if (statements.size() > 0) {
								complete = false;
								openStatements.addAll(statements);
								logger.error(
										"Autorepair of quick column structure {" + quickName + "} in table {" + referenceColumn.getTableName() + "} failed.");
							} else {
								// structure repair sucessful, try to repair
								// data
								logger.error(
										"Autorepair of quick column structure {" + quickName + "} in table {" + referenceColumn.getTableName() + "} successful.");

								boolean updateSuccessfull = updateQuickColumnData(referenceColumn, attributeType, attributeName);

								if (updateSuccessfull) {
									logger.error(
											"Autorepair of quick column data {" + quickName + "} in table {" + referenceColumn.getTableName() + "} successful.");
								} else {
									logger.error("Autorepair of quick column data {" + quickName + "} in table {" + referenceColumn.getTableName() + "} failed.");
									complete = false;
								}
							}
						} else {
							complete = false;
							openStatements.addAll(getColumnAddStatement(referenceColumn));
							logger.error(
									"Autorepair is disabled. Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} not repaired.");
						}
					}
				}

				// check quick column index
				if (referenceIndex != null) {
					Map<Integer, String> indexColumns = new HashMap<Integer, String>();

					indexColumns.put(new Integer(1), quickName);
					IndexDefinition checkIndex = (IndexDefinition) contentMap.getIndex(indexColumns);

					if (checkIndex == null) {
						// quick column index is missing
						logger.error("Quick column index for {" + quickName + "} is missing.");
						if (autoRepair) {
							// autorepair is enabled
							List<String> statements = createIndex(referenceIndex);

							if (statements.size() > 0) {
								complete = false;
								openStatements.addAll(statements);
								logger.error("Autorepair of index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName() + "} failed.");
							} else {
								logger.error(
										"Autorepair of index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName() + "} successful.");
							}
						} else {
							complete = false;
							openStatements.addAll(getIndexAddStatement(referenceIndex));
							logger.error(
									"Autorepair is disabled. Index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName()
									+ "} not repaired.");
						}
					}
				}

				// check quick column in nodeversion table
				if (referenceColumnNodeversion != null) {

					ColumnDefinition checkColumnNodeVersion = (ColumnDefinition) contentMapNodeversion.getColumns().get(quickName);

					if (checkColumnNodeVersion != null) {
						if (!referenceColumnNodeversion.equals(checkColumnNodeVersion)) {
							// quick column exists, but wrong format
							logger.error("Quick column {" + quickName + "} has wrong format.");

							if (autoRepair) {
								// autorepair is enabled
								List<String> statements = alterColumn(checkColumnNodeVersion, referenceColumnNodeversion);

								if (statements.size() > 0) {
									complete = false;
									openStatements.addAll(statements);
									logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} failed.");
								} else {
									logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} successful.");
								}
							} else {
								complete = false;
								openStatements.addAll(getColumnAlterStatement(checkColumnNodeVersion, referenceColumnNodeversion));
								logger.error(
										"Autorepair is disabled. Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} not repaired.");
							}
						}
					} else {
						// quick column is missing
						logger.error("Quick column {" + quickName + "} is missing in nodeversion.");

						if (autoRepair) {
							// autorepair is enabled
							List<String> statements = createColumn(referenceColumnNodeversion);

							if (statements.size() > 0) {
								complete = false;
								openStatements.addAll(statements);
								logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} failed.");
							} else {
								logger.error("Autorepair of quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} successful.");
							}
						} else {
							complete = false;
							openStatements.addAll(getColumnAddStatement(referenceColumnNodeversion));
							logger.error(
									"Autorepair is disabled. Quick column {" + quickName + "} in table {" + referenceColumn.getTableName() + "} not repaired.");
						}
					}
				}

				// check quick column index in nodeversion
				if (referenceIndexNodeversion != null) {

					Map<Integer, String> indexColumns = new HashMap<Integer, String>();

					indexColumns.put(new Integer(1), quickName);
					IndexDefinition checkIndex = (IndexDefinition) contentMapNodeversion.getIndex(indexColumns);

					if (checkIndex == null) {
						// quick column index is missing
						logger.error("Quick column index for {" + quickName + "} in nodeversion is missing.");

						if (autoRepair) {
							// autorepair is enabled
							List<String> statements = createIndex(referenceIndexNodeversion);

							if (statements.size() > 0) {
								complete = false;
								openStatements.addAll(statements);
								logger.error("Autorepair of index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName() + "} failed.");
							} else {
								logger.error(
										"Autorepair of index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName() + "} successful.");
							}
						} else {
							complete = false;
							openStatements.addAll(getIndexAddStatement(referenceIndexNodeversion));
							logger.error(
									"Autorepair is disabled. Index for quick column {" + quickName + "} in table {" + referenceIndex.getTableName()
									+ "} not repaired.");

						}
					}
				}

			}
		} catch (SQLException | CMSUnavailableException e) {
			complete = false;
			logger.error("Error while checking quick columns.", e);
		}

		if (complete) {
			logger.info("Finished checking quick columns, structure valid.");
		} else {
			logger.error("Finished checking quick columns, structure invalid.");
			StringBuffer buffer = new StringBuffer();

			for (String statement : openStatements) {
				buffer.append("\n").append(statement);
			}
			if (!autoRepair) {
				logger.error(
						"Structure of contentrepository is invalid. Run the following statements to repair the structure of the contentrepository:\n"
								+ buffer.toString());
			} else {
				logger.error(
						"Structure of contentrepository is invalid and autorepair failed. Run the following statements to repair the structure of the contentrepository:\n"
								+ buffer.toString());
			}
		}
		return complete;
	}

	/**
	 * Check the id_counter column in contentobject and contentmap for valid
	 * values
	 * @param handleId the handle id for the database handle
	 * @param autoRepair true if incorrent values should be repaired, false if
	 *        not
	 * @return true if repair was successful, false if not
	 */
	protected boolean checkIdCounter(String handleId, boolean autoRepair) {
		logger.debug("Starting consistency check for id counter.");
		SimpleResultProcessor proc = new SimpleResultProcessor();

		try {
			String statement = "SELECT id_counter, obj_type, max(obj_id) maxid FROM " + databaseHandle.getContentObjectName() + " LEFT JOIN "
					+ databaseHandle.getContentMapName() + " ON type = obj_type GROUP BY id_counter, obj_type";

			DB.query(databaseHandle, statement, null, proc);
		} catch (SQLException sqle) {
			logger.error("SQLException while checking id counter.", sqle);
			return false;
		}
		boolean success = true;

		// loop through all rows and check whether id_counter < than maxid
		List<Integer> objTypesToRepair = new Vector<Integer>();

		for (SimpleResultRow row : proc) {
			if (row.getInt("id_counter") < row.getInt("maxid")) {
				success = false;
				logger.warn(
						"Object type {" + row.getInt("obj_type") + "} has id_counter set to {" + row.getInt("id_counter")
						+ "} which is less than the maximum id of existing objects {" + row.getInt("maxid") + "} for datasource-handle {" + handleId + "}");
				objTypesToRepair.add(new Integer(row.getInt("obj_type")));
			}
		}

		if (!success) {
			// there are obj_types that have the id_counter set incorrect
			String repairStatement = "UPDATE " + databaseHandle.getContentObjectName() + " SET id_counter = (SELECT max(obj_id) FROM "
					+ databaseHandle.getContentMapName() + " WHERE type = obj_type) WHERE type in (" + StringUtils.repeat("?", objTypesToRepair.size(), ",") + ")";

			if (autoRepair) {
				logger.debug("Trying to repair incorrect set id_counter values for datasource-handle {" + handleId + "}");
				try {
					int repairedObjectTypes = DB.update(databaseHandle, repairStatement,
							(Object[]) objTypesToRepair.toArray(new Object[objTypesToRepair.size()]));

					logger.warn("Successfully repaired id_counter values for {" + repairedObjectTypes + "} object types in datasource-handle {" + handleId + "}");
					success = true;
				} catch (SQLException e) {
					logger.warn("Error while repairing id_counter values for datasource-handle {" + handleId + "}", e);
					success = false;
				}
			} else {
				logger.warn(
						"Table {" + databaseHandle.getContentObjectName() + "} in datasource-handle {" + handleId
						+ "} contains invalid values for column {id_counter}. Update values manually by performing {\n\t" + repairStatement + "\n}");
				success = false;
			}
		}

		if (success) {
			logger.info("Finished checking consistency of id counter, data valid.");
		} else {
			logger.error("Finished checking consistency of id counter, data invalid.");
		}
		return success;
	}

	/**
	 * Check contentattributetype table for conflicting definitions and linked
	 * objecttypes.
	 * @return true if structure is consistent, false otherwise.
	 */
	protected boolean checkContentAttributeTypeConflicts() {
		logger.debug("Starting consistency check for contentattributetype conflicts.");
		SimpleResultProcessor proc = new SimpleResultProcessor();

		try {
			String statement = "SELECT name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning FROM "
					+ databaseHandle.getContentAttributeTypeName();

			DB.query(databaseHandle, statement, null, proc);
		} catch (SQLException sqle) {
			logger.error("SQLException while checking contentattributetype conflicts.", sqle);
			return false;
		}
		boolean success = true;

		// loop through all rows and check whether any conflicting definitions
		// exist
		Map<String, Collection<ContentAttributeType>> contentAttributeTypesByName = new HashMap<String, Collection<ContentAttributeType>>();
		Map<Integer, Collection<ContentAttributeType>> contentAttributeTypesByObjecttype = new HashMap<Integer, Collection<ContentAttributeType>>();

		for (SimpleResultRow row : proc) {
			String name = row.getString("name");
			int attributetype = row.getInt("attributetype");
			int optimized = row.getInt("optimized");
			String quickname = row.getString("quickname");
			int multivalue = row.getInt("multivalue");
			int objecttype = row.getInt("objecttype");
			int linkedobjecttype = row.getInt("linkedobjecttype");
			String foreignlinkattribute = row.getString("foreignlinkattribute");
			String foreignlinkattributerule = row.getString("foreignlinkattributerule");
			int excludeversioning = row.getInt("excludeversioning");

			ContentAttributeType contentAttributeType = new ContentAttributeType(name, attributetype, optimized, quickname, multivalue, objecttype,
					linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, excludeversioning);

			Collection<ContentAttributeType> previous = contentAttributeTypesByName.get(name);

			if (previous != null) {
				for (ContentAttributeType previousAttributeType : previous) {
					if (!previousAttributeType.equals(contentAttributeType)) {
						logger.error(
								"Conflicting definitions in {" + databaseHandle.getContentAttributeTypeName() + "} found.\nConflicting types:\n"
								+ previousAttributeType.toString() + "\n" + contentAttributeType.toString());
						success = false;
					}
				}
				previous.add(contentAttributeType);
			} else {
				previous = new LinkedList<ContentAttributeType>();
				previous.add(contentAttributeType);
				contentAttributeTypesByName.put(name, previous);
			}

			Collection<ContentAttributeType> objecttypeCollection = contentAttributeTypesByObjecttype.get(new Integer(contentAttributeType.getObjecttype()));

			if (objecttypeCollection != null) {
				objecttypeCollection.add(contentAttributeType);
			} else {
				objecttypeCollection = new LinkedList<ContentAttributeType>();
				objecttypeCollection.add(contentAttributeType);
				contentAttributeTypesByObjecttype.put(new Integer(contentAttributeType.getObjecttype()), objecttypeCollection);
			}
		}

		// loop all contentattributetypes, check consistency of links
		for (Collection<ContentAttributeType> contentAttributeTypesWithName : contentAttributeTypesByName.values()) {
			for (ContentAttributeType contentAttributeType : contentAttributeTypesWithName) {
				// this attributetype links to another
				if (contentAttributeType.getAttributetype() == 2) {
					int linkedObjectType = contentAttributeType.getLinkedobjecttype();
					Collection<ContentAttributeType> objecttype = contentAttributeTypesByObjecttype.get(linkedObjectType);

					if (objecttype == null) {
						logger.error(
								"Contentattributetype with name {" + contentAttributeType.getName() + "} links to inexistant objecttype {" + linkedObjectType + "}.");
						success = false;

					}
				}

				// backlink
				if (contentAttributeType.getAttributetype() == 7) {
					// check if linked objecttype exists
					int linkedObjectType = contentAttributeType.getLinkedobjecttype();
					String foreignlinkattribute = contentAttributeType.getForeignlinkattribute();
					Collection<ContentAttributeType> objecttype = contentAttributeTypesByObjecttype.get(linkedObjectType);

					if (objecttype == null) {
						// linked objecttype does not exist
						logger.error(
								"Contentattributetype with name {" + contentAttributeType.getName() + "} links to inexistant objecttype {" + linkedObjectType + "}.");
						success = false;
					} else {
						boolean found = false;

						for (ContentAttributeType foreignlink : objecttype) {
							// linked objecttype needs to fulfill criteria
							if (foreignlinkattribute.equals(foreignlink.getName()) && foreignlink.getObjecttype() == linkedObjectType
									&& foreignlink.getLinkedobjecttype() == contentAttributeType.getObjecttype() && foreignlink.getAttributetype() == 2) {
								found = true;
							}
						}
						if (!found) {
							logger.error(
									"Contentattributetype with name {" + contentAttributeType.getName() + "} links to objecttype {" + linkedObjectType
									+ "} with foreignlinkattribute {" + foreignlinkattribute + "}, but such an attributetype does not exist.");
							success = false;
						}
					}
				}
			}
		}

		if (success) {
			logger.info("Finished checking consistency of contentattributetypes, data valid.");
		} else {
			logger.error("Finished checking consistency of contentattributetypes, data invalid.");
		}
		return success;
	}

	/**
	 * Read table definitions from given database handle
	 * @param referenceTables the reference tables
	 * @return a map containing the table definitions
	 * @throws CMSUnavailableException
	 */
	protected Map<String, TableDefinition> getTableDefinitions(Map<String, TableDefinition> referenceTables) throws CMSUnavailableException {
		Map<String, TableDefinition> checkMap = new HashMap<String, TableDefinition>();

		for (String tableName : referenceTables.keySet()) {
			TableDefinition tableDefinition = getTableDefinition(tableName);

			if (tableDefinition != null) {
				checkMap.put(tableName, tableDefinition);
			}
		}
		return checkMap;
	}

	/**
	 * Override to implement database specific behaviour.
	 * @param defaultValue the default value for a column
	 * @return The processed default value.
	 */
	protected String getDefaultValue(String defaultValue) {
		return defaultValue;
	}

	/**
	 * Override to implement database specific behavior.
	 * @param databaseMetaData the database meta data object
	 * @param upperCase true if database uses all uppercase letters, false if
	 *        not
	 * @return the schema name to use for the given databasemetadata object.
	 */
	protected String getSchemaName(DatabaseMetaData databaseMetaData, boolean upperCase) {
		if (databaseHandle != null) {
			return databaseHandle.getDbSchema();
		} else {
			return null;
		}
	}

	/**
	 * Read the table definition for the given table name from the repository
	 * @param tableName the name of the table to read
	 * @param upperCase true if database uses uppercase identifiers, false if
	 *        not
	 * @return the table definition read from the repository
	 * @throws CMSUnavailableException
	 */
	protected TableDefinition getTableDefinition(String tableName) throws CMSUnavailableException {
		TableDefinitionExtractor extractor = new TableDefinitionExtractor(tableName);

		try {
			DB.handleDatabaseMetaData(databaseHandle, extractor);
			return extractor.getTableDefinition();
		} catch (SQLException | NodeException e) {
			throw new CMSUnavailableException("Error while getting table definition", e);
		}
	}

	/**
	 * Get the constraint definition for the given data
	 * @param tableName table name
	 * @param foreignTableName foreign table name
	 * @param name constraint name
	 * @return constraint definition
	 * @throws CMSUnavailableException
	 */
	protected ConstraintDefinition getConstraintDefinition(String tableName, String foreignTableName, String name) throws CMSUnavailableException {
		ConstraintDefinitionExtractor extractor = new ConstraintDefinitionExtractor(tableName, foreignTableName, name);

		try {
			DB.handleDatabaseMetaData(databaseHandle, extractor);
			return extractor.getDefinition();
		} catch (SQLException | NodeException e) {
			throw new CMSUnavailableException("Error while getting constraint definition", e);
		}
	}

	/**
	 * @return true if consistency check for database supports adding a missing
	 *         table
	 */
	protected boolean canAutorepairMissingTable() {
		return this.canAutorepairMissingTable;
	}

	/**
	 * @return true if consistency check for database supports adding a missing
	 *         column
	 */
	protected boolean canAutorepairMissingColumn() {
		return this.canAutorepairMissingColumn;
	}

	/**
	 * @return true if consistency check for database supports adding a missing
	 *         index
	 */
	protected boolean canAutorepairMissingIndex() {
		return this.canAutorepairMissingIndex;
	}

	/**
	 * @return true if consistency check for database supports modifying an
	 *         existing column
	 */
	protected boolean canAutorepairIncorrectColumn() {
		return this.canAutorepairIncorrectColumn;
	}

	/**
	 * @return true if consistency check for database supports modifying
	 *         existing and conflicting primary key
	 */
	protected boolean canAutorepairIndex() {
		return this.canAutorepairIndex;
	}

	/**
	 * Get column and index definition for given quick column
	 * @param type the attribute type of the given attribute
	 * @param quickName the quickname for the attribute
	 * @param nodeVersion true if creating column definition for nodeversion
	 *        table, false if not
	 * @param upperCase true if database uses uppercase identifiers, false if
	 *        not
	 * @return the index and column definition for the given quick column
	 */
	protected Object[] getQuickColumnDefinition(int type, String quickName, boolean nodeVersion) {
		ColumnDefinition colDef = null;
		IndexDefinition indexDef = null;

		SQLDatatype dataType = null;

		switch (type) {
		case GenticsContentAttribute.ATTR_TYPE_TEXT:
			dataType = structureDefinition.getTextDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
			dataType = structureDefinition.getClobDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_OBJ:
			dataType = structureDefinition.getShorttextDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			dataType = structureDefinition.getIntegerDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_DATE:
			dataType = structureDefinition.getDateDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_LONG:
			dataType = structureDefinition.getLongDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			dataType = structureDefinition.getDoubleDatatype();
			break;

		case GenticsContentAttribute.ATTR_TYPE_BLOB:
			dataType = structureDefinition.getBlobDatatype();
			break;

		default:
			dataType = null;
			break;
		}
		if (dataType != null) {
			String tableName = databaseHandle.getContentMapName();

			if (nodeVersion) {
				tableName += "_nodeversion";
			}
			colDef = new ColumnDefinition(tableName, quickName, dataType, true, null, false, true, upperCase);

			String indexName = "idx_" + tableName + "_" + quickName;

			// don't create index for unsupported types
			if (dataType.supportsIndex()) {
				indexDef = new IndexDefinition(tableName, indexName, new ColumnDefinition[] { colDef}, false, false, upperCase);
			}
		}
		return new Object[] { colDef, indexDef};
	}

	/**
	 * Return the full structure dump for the contentrepository
	 * @return the full structure dump for the contentrepository
	 */
	public String getStructureDump() {
		Map<String, TableDefinition> referenceTables = getReferenceTables();
		StringBuffer structure = new StringBuffer();

		for (TableDefinition table : referenceTables.values()) {
			List<String> tableDefinition = getTableCreateStatement(table);

			for (String statement : tableDefinition) {
				structure.append(statement).append("\n");
			}
		}

		return structure.toString();
	}

	public static final class ContentAttributeType {
		private String name;

		private int attributetype;

		private int optimized;

		private String quickname;

		private int multivalue;

		private int objecttype;

		private int linkedobjecttype;

		private String foreignlinkattribute;

		private String foreignlinkattributerule;

		private int excludeversioning;

		public ContentAttributeType(String name, int attributetype, int optimized,
				String quickname, int multivalue, int objecttype, int linkedobjecttype,
				String foreignlinkattribute, String foreignlinkattributerule,
				int excludeversioning) {
			this.name = name;
			this.attributetype = attributetype;
			this.optimized = optimized;
			this.quickname = quickname;
			this.multivalue = multivalue;
			this.objecttype = objecttype;
			this.linkedobjecttype = linkedobjecttype;
			this.foreignlinkattribute = foreignlinkattribute;
			this.foreignlinkattributerule = foreignlinkattributerule;
			this.excludeversioning = excludeversioning;
		}

		public String getName() {
			return name;
		}

		public int getAttributetype() {
			return attributetype;
		}

		public int getOptimized() {
			return optimized;
		}

		public String getQuickname() {
			return quickname;
		}

		public int getMultivalue() {
			return multivalue;
		}

		public int getObjecttype() {
			return objecttype;
		}

		public int getLinkedobjecttype() {
			return linkedobjecttype;
		}

		public String getForeignlinkattribute() {
			return foreignlinkattribute;
		}

		public String getForeignlinkattributerule() {
			return foreignlinkattributerule;
		}

		public int getExcludeversioning() {
			return excludeversioning;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int hashCode = 0;

			hashCode += name != null ? name.hashCode() : 0;
			hashCode += attributetype;
			hashCode += optimized;
			hashCode += quickname != null ? quickname.hashCode() : 0;
			hashCode += multivalue;
			hashCode += linkedobjecttype;
			hashCode += foreignlinkattribute != null ? foreignlinkattribute.hashCode() : 0;
			hashCode += foreignlinkattributerule != null ? foreignlinkattributerule.hashCode() : 0;
			hashCode += excludeversioning;
			return hashCode;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			if (!(o instanceof ContentAttributeType)) {
				return false;
			}
			ContentAttributeType other = (ContentAttributeType) o;

			if (!this.name.equals(other.name)) {
				return false;
			}
			if (this.attributetype != other.attributetype) {
				return false;
			}
			if (this.optimized != other.optimized) {
				return false;
			}
			if (this.quickname != null && !this.quickname.equals(other.quickname)) {
				return false;
			}
			if (this.quickname == null && other.quickname != null) {
				return false;
			}
			if (this.multivalue != other.multivalue) {
				return false;
			}
			if (this.linkedobjecttype != other.linkedobjecttype) {
				return false;
			}
			if (this.foreignlinkattribute == null && other.foreignlinkattribute != null) {
				return false;
			}
			if (this.foreignlinkattribute != null && !this.foreignlinkattribute.equals(other.foreignlinkattribute)) {
				return false;
			}
			if (this.foreignlinkattributerule == null && other.foreignlinkattributerule != null) {
				return false;
			}
			if (this.foreignlinkattributerule != null && !this.foreignlinkattributerule.equals(other.foreignlinkattributerule)) {
				return false;
			}
			if (this.excludeversioning != other.excludeversioning) {
				return false;
			}
			return true;
		}

		public String toString() {
			return "ContentAttributeType {" + name + "}: attributetype {" + attributetype + "}, optimized {" + optimized + "}, quickname {" + quickname
					+ "}, multivalue {" + multivalue + "}, objecttype {" + objecttype + "}, linkedobjecttype{" + linkedobjecttype + "}" + ", foreignlinkattribute {"
					+ foreignlinkattribute + "}, foreignlinkattributerule {" + foreignlinkattributerule + "}, excludeversioning {" + excludeversioning + "}.";
		}
	}

	/**
	 * Table Definition extractor
	 */
	protected class TableDefinitionExtractor implements DatabaseMetaDataHandler {

		/**
		 * Table definition
		 */
		protected TableDefinition tableDefinition;

		/**
		 * Table name
		 */
		protected String tableName;

		/**
		 * Create an instance to extract the table definition for the given table
		 * @param tableName table name
		 */
		public TableDefinitionExtractor(String tableName) {
			this.tableName = tableName;
			tableDefinition = new TableDefinition(tableName, null, null, true, upperCase);
		}

		@Override
		public void handleMetaData(DatabaseMetaData metaData) throws SQLException {
			String tableSchema = getSchemaName(metaData, upperCase);

			// check if table exists
			try (ResultSet tables = metaData.getTables(null, tableSchema, tableDefinition.getTableName(), null)) {
				if (!tables.next()) {
					tableDefinition = null;
					return;
				}
			}

			// read all columns
			try (ResultSet columns = metaData.getColumns(null, tableSchema, tableDefinition.getTableName(), null)) {
				while (columns.next()) {
					String columnName = columns.getString("COLUMN_NAME");
					int sqlType = columns.getInt("DATA_TYPE");
					String sqlTypeName = columns.getString("TYPE_NAME");
					int columnSize = columns.getInt("COLUMN_SIZE");
					boolean nullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? true : false;
					String defaultValue = columns.getString("COLUMN_DEF");

					// pass default value to processing, some databases (especially
					// MSSQL) do nasty things when returning default values
					defaultValue = getDefaultValue(defaultValue);
					ColumnDefinition columnDefinition = new ColumnDefinition(tableName, columnName, new SQLDatatype(sqlType, sqlTypeName, columnSize, false),
							nullable, defaultValue, false, true, upperCase);

					tableDefinition.setColumn(columnDefinition);
				}
			}

			// read all primary keys
			try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, tableSchema, tableName)) {
				while (primaryKeys.next()) {
					String indexName = primaryKeys.getString("PK_NAME");
					String columnName = primaryKeys.getString("COLUMN_NAME");
					ColumnDefinition columnDefinition = tableDefinition.getColumn(columnName);
					if (columnDefinition == null) {
						// log warn and continue
						logger.warn(String.format("Could not find COLUMN_NAME '%s' for primary key '%s' of table '%s', ignoring it!", columnName, indexName,
								tableDefinition.getTableName()));
						continue;
					}
					IndexDefinition indexDefinition = tableDefinition.getIndex(indexName);

					if (indexDefinition != null) {
						indexDefinition.setColumnDefinition(columnDefinition, 1);
					} else {
						indexDefinition = new IndexDefinition(tableName, indexName, new ColumnDefinition[] { columnDefinition}, true, true, upperCase);
						tableDefinition.addIndex(indexDefinition);
					}
				}
			}

			// read all indices
			try (ResultSet indices = metaData.getIndexInfo(null, tableSchema, tableName, false, false)) {
				while (indices.next()) {
					String indexName = indices.getString("INDEX_NAME");
					int ordinalPosition = indices.getInt("ORDINAL_POSITION");
					String columnName = indices.getString("COLUMN_NAME");
					boolean nonUnique = indices.getBoolean("NON_UNIQUE");

					if (columnName == null) {
						continue;
					}
					ColumnDefinition columnDefinition = tableDefinition.getColumn(columnName);
					if (columnDefinition == null) {
						// log warn and continue
						logger.warn(String.format("Could not find COLUMN_NAME '%s' for index '%s' of table '%s', ignoring it!", columnName, indexName,
								tableDefinition.getTableName()));
						continue;
					}
					IndexDefinition indexDefinition = tableDefinition.getIndex(indexName);

					if (indexDefinition != null) {
						indexDefinition.setColumnDefinition(columnDefinition, ordinalPosition);
					} else {
						indexDefinition = new IndexDefinition(tableName, indexName, new ColumnDefinition[] { columnDefinition}, false, !nonUnique, upperCase);
						tableDefinition.addIndex(indexDefinition);
					}
				}
			}
		}

		/**
		 * Get the table definition
		 * @return table definition
		 */
		public TableDefinition getTableDefinition() {
			return tableDefinition;
		}
	}

	/**
	 * Structure Definition extractor
	 */
	protected static class StructureDefinitionExtractor implements DatabaseMetaDataHandler {

		/**
		 * Structure definition
		 */
		protected AbstractContentRepositoryStructure structure;

		/**
		 * DB Handle
		 */
		protected DBHandle dbHandle;

		/**
		 * Handle ID
		 */
		protected String handleId;

		/**
		 * Versioning
		 */
		protected boolean versioning;

		/**
		 * true to make a multichannelling aware contentrepository
		 */
		protected boolean multichannelling;

		/**
		 * Create an instance
		 * @param dbHandle db handle
		 * @param handleId handle ID
		 * @param versioning true for versioning
		 * @param multichannelling true to make multichannelling aware
		 */
		public StructureDefinitionExtractor(DBHandle dbHandle, String handleId, boolean versioning, boolean multichannelling) {
			this.dbHandle = dbHandle;
			this.handleId = handleId;
			this.versioning = versioning;
			this.multichannelling = multichannelling;
		}

		@Override
		public void handleMetaData(DatabaseMetaData metaData) throws SQLException, NodeException {
			boolean upperCase = metaData.storesUpperCaseIdentifiers();
			String databaseProductName = metaData.getDatabaseProductName();

			if (DatatypeHelper.MYSQL_NAME.equals(databaseProductName) || DatatypeHelper.MARIADB_NAME.equals(databaseProductName)) {
				structure = new MysqlContentRepositoryStructure(dbHandle, handleId, upperCase, versioning, multichannelling);
			} else if (DatatypeHelper.ORACLE_NAME.equals(databaseProductName)) {
				structure = new OracleContentRepositoryStructure(dbHandle, handleId, upperCase, versioning, multichannelling);
			} else if (DatatypeHelper.MSSQL_NAME.equals(databaseProductName)) {
				structure = new MssqlContentRepositoryStructure(dbHandle, handleId, upperCase, versioning, multichannelling);
			} else if (DatatypeHelper.HSQL_NAME.equals(databaseProductName)) {
				structure = new HsqlContentRepositoryStructure(dbHandle, handleId, upperCase, versioning, multichannelling);
			} else {
				throw new NodeException(String.format("Could not find structure definition for database with product name '%s'", databaseProductName));
			}
		}

		/**
		 * Get the DB specific structure implementation
		 * @return DB specific structure implementation
		 */
		public AbstractContentRepositoryStructure getStructure() {
			return structure;
		}
	}

	/**
	 * Constraint definition extractor
	 */
	protected class ConstraintDefinitionExtractor implements DatabaseMetaDataHandler {

		/**
		 * Table name
		 */
		protected String tableName;

		/**
		 * Foreign table name
		 */
		protected String foreignTableName;

		/**
		 * Constraint name
		 */
		protected String constraintName;

		/**
		 * Extracted definitiopn
		 */
		protected ConstraintDefinition definition;

		/**
		 * Create an instance
		 * @param tableName table name
		 * @param foreignTableName foreign table name
		 * @param constraintName constraint name
		 */
		public ConstraintDefinitionExtractor(String tableName, String foreignTableName, String constraintName) {
			this.tableName = tableName;
			this.foreignTableName = foreignTableName;
			this.constraintName = constraintName;
		}

		@Override
		public void handleMetaData(DatabaseMetaData metaData) throws SQLException {
			String tableSchema = getSchemaName(metaData, upperCase);
			try (ResultSet res = metaData.getCrossReference(null, tableSchema, foreignTableName, null, tableSchema, tableName)) {
				while (res.next()) {
					definition = new ConstraintDefinition(res.getString("FKTABLE_NAME"), res.getString("FKCOLUMN_NAME"), res.getString("PKTABLE_NAME"),
							res.getString("PKCOLUMN_NAME"), ConstraintAction.getAction(res.getInt("DELETE_RULE")), ConstraintAction.getAction(res.getInt("UPDATE_RULE")),
							upperCase);
					return;
				}
			}
		}

		/**
		 * Get the extracted constraint definition or null if not found
		 * @return constraint definition or null
		 */
		public ConstraintDefinition getDefinition() {
			return definition;
		}
	}
}
