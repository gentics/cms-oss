/*
 * @author erwin @date 31.10.2003
 * @version $Id: DatatypeHelper.java,v 1.2 2010-09-28 17:01:27 norbert Exp $
 */

package com.gentics.lib.content;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DB.TableDefinition;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.CacheTimeoutListener;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.etc.TimedCache;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.DefaultRuleTree;

public class DatatypeHelper {
	/**
	 * Default Database Name
	 */
	public final static String DEFAULT_NAME = "default";

	/**
	 * MySQL Database Name
	 */
	public final static String MYSQL_NAME = "MySQL";

	/**
	 * MariaDB Database Name
	 */
	public final static String MARIADB_NAME = "MariaDB";

	/**
	 * Oracle SQL Server Database Name
	 */
	public final static String ORACLE_NAME = "Oracle";

	/**
	 * Microsoft SQL Server Database Name
	 */
	public final static String MSSQL_NAME = "Microsoft SQL Server";

	/**
	 * HSQL Database Name
	 */
	public final static String HSQL_NAME = "HSQL Database Engine";

	/**
	 * name of the field to exclude attributetypes and/or objecttypes from versioning
	 */
	public final static String EXCLUDE_VERSIONING_FIELD = "exclude_versioning";

	/**
	 * name of the field for storing attributes in filesystem
	 */
	public final static String FILESYSTEM_FIELD = "filesystem";

	/**
	 * map holding the database specific datatypes (for creation of missing columns)
	 * TODO: add all needed datatypes and database names into this map (in static initialization)
	 */
	public final static Map SQL_DATATYPES = new HashMap();

	/**
	 * map holding database specific statements for creation of indexes (keys
	 * are the database product names) the statements contain variables in
	 * velocity style: $table for the name of the table $name for the index name
	 * and $column for the name of the column (might also be a comma separated list of columns)
	 */
	public final static Map SQL_CREATEINDEX = new HashMap();
    
	/**
	 * map holding database specific statements for creation of indexes (keys
	 * are the database product names) with a specific column data length 
	 * (this is sometimes necessary if you're creating an index on a blob/text column)
	 * the statements contain variables in velocity style: 
	 * $table for the name of the table $name for the index name,
	 * $column for the name of the column (might also be a comma separated list of columns) and
	 * $length is the length of data that should be indexed
	 */
	public final static Map SQL_CREATEINDEX_WITH_LENGTH = new HashMap();
    
	/**
	 * map holding database specific statements for dropping an index (keys
	 * are the database product names) the statements contain variables in
	 * velocity style: $table for the name of the table $name for the index name
	 */
	public final static Map SQL_DROPINDEX = new HashMap();

	/**
	 * map holding database specific statement parts for defining defaults (keys
	 * are the database product names). the statement parts contain variables in
	 * velocity style: $table for the name of the table $name for the index name
	 */
	public final static Map SQL_DEFAULT = new HashMap();

	/**
	 * map holding database specific statements for dropping columns (keys
	 * are the database product names). the statements contain variables in
	 * velocity style: $table for the name of the table $name for the column name
	 */
	public final static Map SQL_DROPCOLUMN = new HashMap();

	/**
	 * Specialized class for column definition of the id_counter column.
	 * Contains check for values of the id_counter.
	 */
	protected static class IdCounterColumnDefinition extends DB.ColumnDefinition {

		/**
		 * Create the id_counter column definition
		 * @param tableName table name
		 * @param columnName column name
		 * @param sqlType sql type
		 * @param sqlTypeName sql name type
		 * @param nullable whether the column is nullable
		 * @param defaultValue default value
		 * @param required whether the column is required
		 */
		public IdCounterColumnDefinition(String tableName, String columnName, int sqlType, String sqlTypeName, boolean nullable, String defaultValue, boolean required) {
			super(tableName, columnName, sqlType, sqlTypeName, nullable, defaultValue, required);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.db.DB.ColumnDefinition#doSpecificTest(com.gentics.lib.db.DBHandle,
		 *      boolean, com.gentics.lib.log.NodeLogger)
		 */
		public boolean doSpecificTest(DBHandle dbHandle, String handleId, boolean autoRepair,
				NodeLogger logger) throws SQLException {
			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(dbHandle,
					"SELECT id_counter, obj_type, max(obj_id) maxid FROM " + dbHandle.getContentObjectName() + " LEFT JOIN " + dbHandle.getContentMapName()
					+ " ON type = obj_type GROUP BY id_counter, obj_type",
					null,
					proc);
			boolean success = true;

			// loop through all rows and check whether id_counter < than maxid
			List objTypesToRepair = new Vector();

			for (Iterator iter = proc.iterator(); iter.hasNext();) {
				SimpleResultRow row = (SimpleResultRow) iter.next();

				if (row.getInt("id_counter") < row.getInt("maxid")) {
					success = false;
					DB.forceInfoLog(
							"Object type {" + row.getInt("obj_type") + "} has id_counter set to {" + row.getInt("id_counter")
							+ "} which is less than the maximum id of existing objects {" + row.getInt("maxid") + "} for datasource-handle {" + handleId + "}, "
							+ dbHandle);
					objTypesToRepair.add(new Integer(row.getInt("obj_type")));
				}
			}

			if (!success) {
				// there are obj_types that have the id_counter set incorrect
				String repairStatement = "UPDATE " + dbHandle.getContentObjectName() + " SET id_counter = (SELECT max(obj_id) FROM "
						+ dbHandle.getContentMapName() + " WHERE type = obj_type) WHERE type in (" + StringUtils.repeat("?", objTypesToRepair.size(), ",") + ")";

				if (autoRepair) {
					DB.forceInfoLog("Trying to repair incorrect set id_counter values for datasource-handle {" + handleId + "}, " + dbHandle);
					try {
						int repairedObjectTypes = DB.update(dbHandle, repairStatement, (Object[]) objTypesToRepair.toArray(new Object[objTypesToRepair.size()]));

						DB.forceInfoLog(
								"Successfully repaired id_counter values for {" + repairedObjectTypes + "} object types in datasource-handle {" + handleId + "}, "
								+ dbHandle);
						success = true;
					} catch (SQLException e) {
						logger.error("Error while repairing id_counter values for datasource-handle {" + handleId + "}, " + dbHandle, e);
						success = false;
					}
				} else {
					logger.error(
							"Table {" + dbHandle.getContentObjectName() + "} in datasource-handle {" + handleId + "}, " + dbHandle
							+ " contains invalid values for column {id_counter}. Update values manually by performing {\n\t" + repairStatement + "\n}");
					success = false;
				}
			}
			return success;
		}
	}

	/**
	 * Get the table definitions for the contentrepository TODO: add all tables
	 * and columns here
	 * @param handle dbhandle
	 * @return table definitions
	 */
	public final static DB.TableDefinition[] getContentRepositoryTables(DBHandle handle) {
		String contentAttributeNodeversion = handle.getContentAttributeName() + "_nodeversion";

		return new DB.TableDefinition[] {
			new DB.TableDefinition(handle.getContentAttributeTypeName(),
					new DB.ColumnDefinition[] {
				new DB.ColumnDefinition(handle.getContentAttributeTypeName(), "exclude_versioning", Types.INTEGER, "INTEGER", false,
						"0", false)}, true),
			new DB.TableDefinition(handle.getContentObjectName(),
					new DB.ColumnDefinition[] {
				new DB.ColumnDefinition(handle.getContentObjectName(), "exclude_versioning", Types.INTEGER, "INTEGER", false, "0", false),
				new IdCounterColumnDefinition(handle.getContentObjectName(), "id_counter", Types.INTEGER, "INTEGER", false, "0", false)}, false),
			new DB.TableDefinition(handle.getContentAttributeName(),
					new DB.ColumnDefinition[] {
				new DB.ColumnDefinition(handle.getContentAttributeName(), "value_clob", Types.LONGVARCHAR, "CLOB", true, null, true),
				new DB.ColumnDefinition(handle.getContentAttributeName(), "value_long", Types.BIGINT, "LONG", true, null, true),
				new DB.ColumnDefinition(handle.getContentAttributeName(), "value_double", Types.DOUBLE, "DOUBLE", true, null, true),
				new DB.ColumnDefinition(handle.getContentAttributeName(), "value_date", Types.TIMESTAMP, "DATE", true, null, true)}, true),
			new DB.TableDefinition(contentAttributeNodeversion,
					new DB.ColumnDefinition[] {
				new DB.ColumnDefinition(contentAttributeNodeversion, "value_clob", Types.LONGVARCHAR, "CLOB", true, null, true),
				new DB.ColumnDefinition(contentAttributeNodeversion, "value_long", Types.BIGINT, "LONG", true, null, true),
				new DB.ColumnDefinition(contentAttributeNodeversion, "value_double", Types.DOUBLE, "DOUBLE", true, null, true),
				new DB.ColumnDefinition(contentAttributeNodeversion, "value_date", Types.TIMESTAMP, "DATE", true, null, true)}, false),
			new DB.TableDefinition(handle.getContentStatusName(),
					new DB.ColumnDefinition[] {

				/*
				 * new
				 * DB.ColumnDefinition(handle.getContentStatusName(),
				 * "name", Types.VARCHAR, "TEXT", false, "",
				 * true),
				 */
				new DB.ColumnDefinition(handle.getContentStatusName(), "intvalue", Types.INTEGER, "INTEGER", true, null, true),
				new DB.ColumnDefinition(handle.getContentStatusName(), "stringvalue", Types.LONGVARCHAR, "CLOB", true, null, true)}, false)};
	}

	/**
	 * Inner helper class for database specific datatypes
	 */
	public final static class SQLDatatype {

		/**
		 * sql type
		 */
		private int sqlType;

		/**
		 * alternative sql type, is set to {@link Integer#MIN_VALUE} when not used
		 */
		private int alternativeSQLType;

		/**
		 * sql datatype name
		 */
		private String sqlTypeName;

		/**
		 * Create an instance of the SQLDatatype
		 * @param sqlType sql type
		 * @param sqlTypeName name of the type
		 */
		public SQLDatatype(int sqlType, String sqlTypeName) {
			this(sqlType, Integer.MIN_VALUE, sqlTypeName);
		}

		/**
		 * Create an instance of the SQLDatatype
		 * @param sqlType sql type
		 * @param alternativeType alternative sql type
		 * @param sqlTypeName name of the type
		 */
		public SQLDatatype(int sqlType, int alternativeType, String sqlTypeName) {
			this.sqlType = sqlType;
			this.alternativeSQLType = alternativeType;
			this.sqlTypeName = sqlTypeName;
		}

		/**
		 * @return Returns the sqlType.
		 */
		public int getSqlType() {
			return sqlType;
		}

		/**
		 * @return Returns the sqlTypeName.
		 */
		public String getSqlTypeName() {
			return sqlTypeName;
		}

		/**
		 * Check whether the given sql type matches either this sql type or (if
		 * given) the alternative sql type
		 * @param givenType given type
		 * @return true when the types match, false if not
		 */
		public boolean matchesType(int givenType) {
			if (sqlType == givenType) {
				return true;
			} else {
				if (alternativeSQLType != Integer.MIN_VALUE && alternativeSQLType == givenType) {
					return true;
				} else {
					return false;
				}
			}
		}
	}

	/**
	 * Inner class to store features of the contentrepository
	 */
	public static class ContentRepositoryFeatures {

		/**
		 * whether the column for attributetypes contains the column to exclude
		 * from versioning
		 */
		private boolean attributeTypeExcludeVersioningColumn = false;

		/**
		 * whether the column for attributetypes contains the column to store
		 * data in the filesystem
		 */
		private boolean attributeTypeFilesystemColumn = false;

		/**
		 * whether the column for objecttypes contains the column to exclude
		 * from versioning
		 */
		private boolean objectTypeExcludeVersioningColumn = false;

		/**
		 * static object that serves as key for the stored features
		 */
		public static ContentRepositoryFeatures KEY_OBJECT = new ContentRepositoryFeatures();

		/**
		 * @return Returns the attributeTypeExcludeVersioningColumn.
		 */
		public boolean isAttributeTypeExcludeVersioningColumn() {
			return attributeTypeExcludeVersioningColumn;
		}

		/**
		 * @param attributeTypeExcludeVersioningColumn The
		 *        attributeTypeExcludeVersioningColumn to set.
		 */
		public void setAttributeTypeExcludeVersioningColumn(
				boolean attributeTypeExcludeVersioningColumn) {
			this.attributeTypeExcludeVersioningColumn = attributeTypeExcludeVersioningColumn;
		}

		/**
		 * Get true if the filesystem column exists for attribute types
		 * @return true if the column exists, false if not
		 */
		public boolean isAttributeTypeFilesystemColumn() {
			return attributeTypeFilesystemColumn;
		}

		/**
		 * Set true if the filesystem column exists for attribute types
		 * @param attributeTypeFilesystemColumn true if the column exists
		 */
		public void setAttributeTypeFilesystemColumn(
				boolean attributeTypeFilesystemColumn) {
			this.attributeTypeFilesystemColumn = attributeTypeFilesystemColumn;
		}

		/**
		 * @return Returns the objectTypeExcludeVersioningColumn.
		 */
		public boolean isObjectTypeExcludeVersioningColumn() {
			return objectTypeExcludeVersioningColumn;
		}

		/**
		 * @param objectTypeExcludeVersioningColumn The
		 *        objectTypeExcludeVersioningColumn to set.
		 */
		public void setObjectTypeExcludeVersioningColumn(
				boolean objectTypeExcludeVersioningColumn) {
			this.objectTypeExcludeVersioningColumn = objectTypeExcludeVersioningColumn;
		}
	}

	public static class AttributeType {
		int type;

		int linkedobjecttype;

		String foreignlinkedattribute;

		String foreignlinkattributerule;

		RuleTree foreignLinkAttributeRuleTree;
        
		String foreignLinkAttributeRuleTreeString;

		boolean multivalue;

		boolean optimized;

		String quickName;

		String name;

		/**
		 * flag to exclude the attributetype from versioning, defaults to false
		 */
		boolean excludeVersioning = false;

		/**
		 * Flag to mark attributetypes that write into the filesystem
		 */
		boolean filesystem = false;

		public AttributeType(String name, int type, boolean multivalue, boolean optimized, String quickName) {
			this.name = name;
			this.type = type;
			this.multivalue = multivalue;
			this.optimized = optimized;
			this.quickName = quickName;
			linkedobjecttype = 0;
			foreignlinkedattribute = null;
			foreignlinkattributerule = null;
			foreignLinkAttributeRuleTree = null;
			init();
		}

		private void init() {
			// if quickname == empty, use null
			if (quickName != null) {
				if (quickName.length() == 0) {
					quickName = null;
				}
			}
			if (type == 7) {
				foreignLinkAttributeRuleTree = createForeignLinkAttributeRuleTree(null);
			}
		}

		/**
		 * Create the foreign link rule tree
		 * @param startObject main object
		 * @return ruletree
		 */
		private RuleTree createForeignLinkAttributeRuleTree(Resolvable startObject) {
			if (type != 7) {
				return null;
			}

			if (foreignLinkAttributeRuleTree != null) {
				RuleTree tree = foreignLinkAttributeRuleTree.deepCopy();

				tree.addResolver("data", startObject);
				return tree;
			}
			String fetchRule = createForeignLinkAttributeRuleTreeString();

			RuleTree ruleTree = new DefaultRuleTree();

			// prepare a resolver for "data" to ensure correct parsing
			ruleTree.addResolver("data", startObject);
			try {
				ruleTree.parse(fetchRule);
			} catch (ParserException e) {
				NodeLogger.getLogger(getClass()).error("error while initializing attribute type", e);
			}
			this.foreignLinkAttributeRuleTree = ruleTree;

			return ruleTree.deepCopy();
		}

		/**
		 * creates the string used in the "rule tree" (ie. expression) to filter
		 * the foreign linked objects.
		 */
		private String createForeignLinkAttributeRuleTreeString() {
			// prepare the rule for fetching the foreign objects
			String fetchRule = "object.obj_type == " + linkedobjecttype + " && object." + foreignlinkedattribute + " == data.contentid";

			// check for an eventually existing rule
			if (foreignlinkattributerule != null && foreignlinkattributerule.length() > 0) {
				fetchRule = "(" + fetchRule + ") && (" + foreignlinkattributerule + ")";
			}
			return fetchRule;
		}

		public String getForeignLinkAttributeRuleTreeString() {
			if (foreignLinkAttributeRuleTreeString != null) {
				return foreignLinkAttributeRuleTreeString;
			}
			return foreignLinkAttributeRuleTreeString = createForeignLinkAttributeRuleTreeString();
		}

		public AttributeType(String name, int type, boolean multivalue, boolean optimized, String quickName,
				int linkedobjecttype, String foreignlinkedattribute, String foreignlinkattributerule, boolean excludeVersioning, boolean filesystem) {
			this.name = name;
			this.type = type;
			this.multivalue = multivalue;
			this.optimized = optimized;
			this.linkedobjecttype = linkedobjecttype;
			this.foreignlinkedattribute = foreignlinkedattribute;
			this.foreignlinkattributerule = foreignlinkattributerule;
			this.quickName = quickName;
			this.excludeVersioning = excludeVersioning;
			this.filesystem = filesystem;
			init();
		}

		public int getType() {
			return type;
		}

		public boolean isMultivalue() {
			return multivalue;
		}

		public boolean isOptimized() {
			return optimized;
		}

		public String getQuickName() {
			return quickName;
		}

		/**
		 * @return returns type of linked object, 0 if not applicable.
		 */
		public int getLinkedObjectType() {
			return linkedobjecttype;
		}

		public String getForeignLinkedAttribute() {
			return foreignlinkedattribute;
		}

		public String getForeignLinkAttributeRule() {
			return foreignlinkattributerule;
		}

		/**
		 * Check whether the attributetype writes into the filesystem
		 * @return true for filesystem, false otherwise
		 */
		public boolean isFilesystem() {
			return filesystem;
		}

		public String getName() {
			return name;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof AttributeType) {
				AttributeType compType = (AttributeType) obj;

				return (compType.type == type && compType.linkedobjecttype == linkedobjecttype && compType.multivalue == multivalue
						&& compType.optimized == optimized && compType.filesystem == filesystem
						&& StringUtils.isEqual(ObjectTransformer.getString(compType.foreignlinkedattribute, ""),
						ObjectTransformer.getString(foreignlinkedattribute, "")));
			} else {
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int hashCode = type + linkedobjecttype + (multivalue ? 1 : -1) + (optimized ? 100 : -100) + (filesystem ? 1000 : -1000);

			hashCode += ObjectTransformer.getString(foreignlinkedattribute, "").hashCode();
			return hashCode;
		}

		/**
		 * Get a clone of the ruleTree for fetching foreign linked attributes
		 * @param startObject main object
		 * @return Returns the foreignLinkAttributeRuleTree.
		 */
		public RuleTree getForeignLinkAttributeRuleTree(Resolvable startObject) {
			// TODO: avoid parsing of ruletree here
			return createForeignLinkAttributeRuleTree(startObject);
		}
        
		public String toString() {
			StringBuffer sb = new StringBuffer();

			sb.append("type: ").append(type).append(", quickname: ").append(quickName).append(", optimized: ").append(optimized);
			sb.append(", multivalue: ").append(multivalue).append(", linkedobjecttype: ").append(linkedobjecttype);
			sb.append(", foreignlinkattribute: ").append(foreignlinkattributerule);
			sb.append(", foreignlinkattributerule: ").append(foreignlinkattributerule);
			return sb.toString();
		}

		/**
		 * Get the column that stores the data for this attribute type.
		 * This method takes into account, whether the attribute stores in the filesystem, or not
		 * @return name of the DB column
		 */
		public String getColumn() {
			if (filesystem) {
				return "value_text";
			} else {
				return DatatypeHelper.getTypeColumn(type);
			}
		}
	}

	/**
	 * Static inner class for the objecttypes
	 * @author norbert
	 */
	public static class ObjectType {
		int type;
		String name;
		boolean excludeVersioning = false;

		public ObjectType(int type, String name, boolean excludeVersioning) {
			this.type = type;
			this.name = name;
			this.excludeVersioning = excludeVersioning;
		}

		/**
		 * @return Returns the excludeVersioning.
		 */
		public boolean isExcludeVersioning() {
			return excludeVersioning;
		}

		/**
		 * @param excludeVersioning The excludeVersioning to set.
		 */
		public void setExcludeVersioning(boolean excludeVersioning) {
			this.excludeVersioning = excludeVersioning;
		}

		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name The name to set.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return Returns the type.
		 */
		public int getType() {
			return type;
		}

		/**
		 * @param type The type to set.
		 */
		public void setType(int type) {
			this.type = type;
		}

	}

	private static Map handledTypeCacheMap = Collections.synchronizedMap(new HashMap());

	private static String typeCacheError = "";

	private static Object updateCacheObject(DBHandle handle, Object o) {
		SimpleResultProcessor rs = null;
		HashMap typeMap = new HashMap();

		try {
			// we need to know, whether the datasource is mccr (because the meta attributes will be different)
			// therefore we check whether the table "channel" exists (bad hack)
			boolean mccrDatasource = DB.tableExists(handle, "channel");

			rs = new SimpleResultProcessor();
			DB.query(handle, "SELECT * FROM " + handle.getContentAttributeTypeName(), (ResultProcessor) rs);

			Iterator it = rs.iterator();
            
			while (it.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) it.next();
				String attributeName = row.getString("name");
				int attrTypeId = row.getInt("attributetype");

				if (attrTypeId == 0) {
					attrTypeId = row.getInt("type");
				}
				AttributeType newType = new AttributeType(attributeName, attrTypeId, row.getBoolean("multivalue"), row.getBoolean("optimized"),
						row.getString("quickname"), row.getInt("linkedobjecttype"), row.getString("foreignlinkattribute"), row.getString("foreignlinkattributerule"),
						row.getBoolean(EXCLUDE_VERSIONING_FIELD), row.getBoolean(FILESYSTEM_FIELD));
                
				// optimized and filesystem cannot both be set
				if (newType.isOptimized() && newType.isFilesystem()) {
					NodeLogger.getLogger(DatatypeHelper.class).error("Optimized and Filesystem cannot both be enabled.");
				}
                
				// when a type with the name already exists and is configured
				// differently, this is an error
				if (typeMap.containsKey(attributeName)) {
					AttributeType oldType = (AttributeType) typeMap.get(attributeName);

					if (!oldType.equals(newType)) {
						NodeLogger logger = NodeLogger.getNodeLogger(DatatypeHelper.class);

						if (logger.isDebugEnabled()) {
							logger.debug("Multiple different configurations for the attribute {" + attributeName + "} have been found!");
							logger.debug("Type 1: " + newType.toString());
							logger.debug("Type 2:" + oldType.toString());
						}
						NodeLogger.getLogger(DatatypeHelper.class).error(
								"attribute '" + attributeName + "' has multiple different configurations for " + handle + "!");
					}
				} else {
					typeMap.put(attributeName, newType);
				}

				// also put the attributetype with combined key into the map
				typeMap.put(new AttributeKey(row.getInt("objecttype"), attributeName), newType);
			}

			// add std columns
			if ("true".equals(System.getProperty("com.gentics.portalnode.datasource.allowsetobjtype"))) {
				Object objtype = typeMap.get("obj_type");

				typeMap.putAll(getDefaultColumnTypes(mccrDatasource));
				if (objtype != null) {
					typeMap.put("obj_type", objtype);
				}
			} else {
				typeMap.putAll(getDefaultColumnTypes(mccrDatasource));
			}

			// add the objecttypes
			rs.clear();
			if (DB.tableExists(handle, handle.getContentObjectName())) {
				DB.query(handle, "SELECT * FROM " + handle.getContentObjectName(), rs);
				for (Iterator iter = rs.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();
					ObjectType objectType = new ObjectType(row.getInt("type"), row.getString("name"), row.getBoolean(EXCLUDE_VERSIONING_FIELD));

					typeMap.put(new Integer(objectType.getType()), objectType);
				}
			}

			// add the features object
			ContentRepositoryFeatures features = new ContentRepositoryFeatures();

			features.setAttributeTypeExcludeVersioningColumn(DB.fieldExists(handle, handle.getContentAttributeTypeName(), EXCLUDE_VERSIONING_FIELD));
			features.setAttributeTypeFilesystemColumn(DB.fieldExists(handle, handle.getContentAttributeTypeName(), FILESYSTEM_FIELD));
			features.setObjectTypeExcludeVersioningColumn(DB.fieldExists(handle, handle.getContentObjectName(), EXCLUDE_VERSIONING_FIELD));
			typeMap.put(ContentRepositoryFeatures.KEY_OBJECT, features);

			return typeMap;
		} catch (SQLException e) {
			typeCacheError = e.getMessage();
			NodeLogger.getLogger(DatatypeHelper.class).error("Error while updating cache object for {" + handle + "}: " + e.getMessage(), e);
			return o;
		}
	}

	public static Map getDefaultQuickColumns() {
		Map map = new HashMap();

		map.put("contentid", "contentid");
		map.put("motherid", "motherid");

		map.put("obj_type", "obj_type");
		map.put("obj_id", "obj_id");
		map.put("mother_obj_id", "mother_obj_id");
		map.put("mother_obj_type", "mother_obj_type");
		map.put("updatetimestamp", "updatetimestamp");

		return map;
	}

	/**
	 * Meta attributes for CR Datasources
	 */
	private static final Map defaultColumnTypes;

	/**
	 * Meta attributes for MCCR Datasources
	 */
	private final static Map<String, AttributeType> defaultMCCRColumnTypes = new HashMap<String, DatatypeHelper.AttributeType>();

	static {
		Map map = new HashMap();

		if (!"true".equals(System.getProperty("com.gentics.portalnode.datasource.allowsetobjtype"))) {
			map.put("obj_type", new AttributeType("obj_type", 3, false, true, "obj_type"));
		}
		map.put("obj_id", new AttributeType("obj_id", 3, false, true, "obj_id"));
		map.put("mother_obj_id", new AttributeType("mother_obj_id", 3, false, true, "mother_obj_id"));
		map.put("mother_obj_type", new AttributeType("mother_obj_type", 3, false, true, "mother_obj_type"));
		map.put("updatetimestamp", new AttributeType("updatetimestamp", 3, false, true, "updatetimestamp"));

		map.put("contentid", new AttributeType("contentid", 1, false, true, "contentid"));
		map.put("motherid", new AttributeType("motherid", 1, false, true, "motherid"));

		defaultColumnTypes = map;

		// fill the meta attributes map for MCCR Datasources
		defaultMCCRColumnTypes.put("obj_type", new AttributeType("obj_type", 3, false, true, "obj_type"));
		defaultMCCRColumnTypes.put("obj_id", new AttributeType("obj_id", 3, false, true, "obj_id"));
		defaultMCCRColumnTypes.put("updatetimestamp", new AttributeType("updatetimestamp", 3, false, true, "updatetimestamp"));
		defaultMCCRColumnTypes.put("contentid", new AttributeType("contentid", 1, false, true, "contentid"));
		defaultMCCRColumnTypes.put("channel_id", new AttributeType("channel_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, true, "channel_id"));
		defaultMCCRColumnTypes.put("channelset_id", new AttributeType("channelset_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, true, "channelset_id"));

		// set the known datatypes for different database managers
		// datatypes for mysql
		Map mysqlMap = new HashMap();

		mysqlMap.put("SHORTTEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(32)"));
		mysqlMap.put("TEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(255)"));
		mysqlMap.put("INTEGER", new SQLDatatype(Types.INTEGER, "INTEGER"));
		mysqlMap.put("CLOB", new SQLDatatype(Types.LONGVARCHAR, "MEDIUMTEXT"));
		mysqlMap.put("LONG", new SQLDatatype(Types.BIGINT, "BIGINT"));
		mysqlMap.put("DOUBLE", new SQLDatatype(Types.DOUBLE, "DOUBLE"));
		mysqlMap.put("DATE", new SQLDatatype(Types.TIMESTAMP, "DATETIME"));
		mysqlMap.put("BLOB", new SQLDatatype(Types.BLOB, "longblob"));
		SQL_DATATYPES.put(MYSQL_NAME, mysqlMap);
		SQL_DATATYPES.put(MARIADB_NAME, mysqlMap);

		// datatypes for oracle
		Map oracleMap = new HashMap();

		oracleMap.put("SHORTTEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR2(32)"));
		oracleMap.put("TEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR2(255)"));
		oracleMap.put("INTEGER", new SQLDatatype(Types.DECIMAL, "INTEGER"));
		oracleMap.put("CLOB", new SQLDatatype(Types.CLOB, "CLOB"));
		oracleMap.put("LONG", new SQLDatatype(Types.DECIMAL, "NUMBER(20)"));
		oracleMap.put("DOUBLE", new SQLDatatype(Types.DECIMAL, "NUMBER"));
		oracleMap.put("DATE", new SQLDatatype(Types.DATE, Types.TIMESTAMP, "DATE"));
		oracleMap.put("BLOB", new SQLDatatype(Types.BLOB, "BLOB"));
		SQL_DATATYPES.put(ORACLE_NAME, oracleMap);

		// datatypes for hsql
		Map hsqlMap = new HashMap();

		hsqlMap.put("SHORTTEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(32)"));
		hsqlMap.put("TEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(255)"));
		hsqlMap.put("INTEGER", new SQLDatatype(Types.INTEGER, "INTEGER"));
		hsqlMap.put("CLOB", new SQLDatatype(Types.LONGVARCHAR, "LONGVARCHAR"));
		hsqlMap.put("LONG", new SQLDatatype(Types.BIGINT, "BIGINT"));
		hsqlMap.put("DOUBLE", new SQLDatatype(Types.DOUBLE, "DOUBLE"));
		hsqlMap.put("DATE", new SQLDatatype(Types.TIMESTAMP, "TIMESTAMP"));
		hsqlMap.put("BLOB", new SQLDatatype(Types.BLOB, "LONGVARBINARY"));
		SQL_DATATYPES.put(HSQL_NAME, hsqlMap);

		// datatypes for ms sql
		Map mssqlMap = new HashMap();

		mssqlMap.put("SHORTTEXT", new SQLDatatype(Types.VARCHAR, -9, "NVARCHAR(32)"));
		mssqlMap.put("NVARCHAR(32)", mssqlMap.get("SHORTTEXT"));
		mssqlMap.put("TEXT", new SQLDatatype(Types.VARCHAR, -9, "NVARCHAR(255)"));
		mssqlMap.put("NVARCHAR(255)", mssqlMap.get("TEXT"));
		mssqlMap.put("INTEGER", new SQLDatatype(Types.INTEGER, "INTEGER"));
		mssqlMap.put("CLOB", new SQLDatatype(Types.LONGVARCHAR, -16, "NTEXT"));
		mssqlMap.put("NTEXT", mssqlMap.get("CLOB"));
		mssqlMap.put("LONG", new SQLDatatype(Types.BIGINT, "BIGINT"));
		mssqlMap.put("DOUBLE", new SQLDatatype(Types.FLOAT, Types.DOUBLE, "FLOAT"));
		mssqlMap.put("DATE", new SQLDatatype(Types.TIMESTAMP, "DATETIME"));
		mssqlMap.put("BLOB", new SQLDatatype(Types.BLOB, "VARBINARY(max)"));
		SQL_DATATYPES.put(MSSQL_NAME, mssqlMap);

		// default datatypes
		Map defaultMap = new HashMap();

		defaultMap.put("SHORTTEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(32)"));
		defaultMap.put("TEXT", new SQLDatatype(Types.VARCHAR, "VARCHAR(255)"));
		defaultMap.put("INTEGER", new SQLDatatype(Types.INTEGER, "INTEGER"));
		defaultMap.put("CLOB", new SQLDatatype(Types.LONGVARCHAR, "LONGVARCHAR"));
		defaultMap.put("LONG", new SQLDatatype(Types.BIGINT, "BIGINT"));
		defaultMap.put("DOUBLE", new SQLDatatype(Types.DOUBLE, "DOUBLE"));
		defaultMap.put("DATE", new SQLDatatype(Types.TIMESTAMP, "TIMESTAMP"));
		defaultMap.put("BLOB", new SQLDatatype(Types.BLOB, "BLOB"));
		SQL_DATATYPES.put(DEFAULT_NAME, defaultMap);

		// index creation statements
		SQL_CREATEINDEX.put(MYSQL_NAME, "ALTER TABLE $table ADD INDEX $name ($column)");
		SQL_CREATEINDEX.put(MARIADB_NAME, "ALTER TABLE $table ADD INDEX $name ($column)");
		// default seems to be sufficient for most database engines (at least oracle, mssql, hsql)
		SQL_CREATEINDEX.put(DEFAULT_NAME, "CREATE INDEX $name ON $table ($column)");
        
		// index creation with data length
		// for mysql we must specify column length
		SQL_CREATEINDEX_WITH_LENGTH.put(MYSQL_NAME, "ALTER TABLE $table ADD INDEX $name ($column($length))");
		SQL_CREATEINDEX_WITH_LENGTH.put(MARIADB_NAME, "ALTER TABLE $table ADD INDEX $name ($column($length))");
		// afaik all other databases can live live without length
		// therefor we can use the normal statement for create index. 
		// since it contains no $length no substitution will take place
		SQL_CREATEINDEX_WITH_LENGTH.put(DEFAULT_NAME, SQL_CREATEINDEX.get(DEFAULT_NAME));
        
		// mysql uses syntax with " name ON table"
		SQL_DROPINDEX.put(MYSQL_NAME, "DROP INDEX $name ON $table");
		SQL_DROPINDEX.put(MARIADB_NAME, "DROP INDEX $name ON $table");
		// all other DBs use table.name or can use both (e.g. mssql)
		SQL_DROPINDEX.put(DEFAULT_NAME, "DROP INDEX $table.$name");

		// mssql will create the default value as constraint, so we give it a name, such that we can drop it afterwards
		SQL_DEFAULT.put(MSSQL_NAME, "CONSTRAINT default$table$name DEFAULT");
		// all other databases store the default in a way that we still can remove the column by just dropping it
		SQL_DEFAULT.put(DEFAULT_NAME, "DEFAULT");

		// for mssql we need to drop default constraints before a column can be dropped
		SQL_DROPCOLUMN.put(MSSQL_NAME, new String[] {
			"ALTER TABLE $table DROP CONSTRAINT default$table$name", "ALTER TABLE $table DROP COLUMN $name"});
		SQL_DROPCOLUMN.put(DEFAULT_NAME, new String[] { "ALTER TABLE $table DROP COLUMN $name"});
	}

	/**
	 * Convert the generic datatype for the given database into the database specific datatype
	 * @param databaseName database name (coming from the driver)
	 * @param genericDatatype generic datatype
	 * @return database specific datatype or null if no definition found
	 */
	public static SQLDatatype getDBSpecificSQLDatatype(String databaseName, String genericDatatype) {
		if (!SQL_DATATYPES.containsKey(databaseName)) {
			Map defaultTable = (Map) SQL_DATATYPES.get(DEFAULT_NAME);

			return (SQLDatatype) defaultTable.get(genericDatatype);
		} else {
			Map datatypeTable = (Map) SQL_DATATYPES.get(databaseName);

			if (!datatypeTable.containsKey(genericDatatype)) {
				return null;
			} else {
				return (SQLDatatype) datatypeTable.get(genericDatatype);
			}
		}
	}

	/**
	 * Get the database specific statement for creating the given index
	 * @param databaseName database product name
	 * @param tableName table name
	 * @param indexName index name
	 * @param indexColumn column name (or comma separated list of columns)
	 * @return creation statement
	 */
	public static String getDBSpecificIndexCreateStatement(String databaseName,
			String tableName, String indexName, String indexColumn) {
		String createStatement = null;

		if (!SQL_CREATEINDEX.containsKey(databaseName)) {
			createStatement = (String) SQL_CREATEINDEX.get(DEFAULT_NAME);
		} else {
			// find the specific statement, fill in the variables and return it
			createStatement = (String) SQL_CREATEINDEX.get(databaseName);
		}

		createStatement = createStatement.replaceAll("\\$table\\b", tableName).replaceAll("\\$name\\b", indexName).replaceAll("\\$column\\b", indexColumn);
		return createStatement;
	}
    
	/**
	 * Get the database specific statement for creating the given index
	 * @param databaseName database product name
	 * @param tableName table name
	 * @param indexName index name
	 * @param indexColumn column name (or comma separated list of columns)
	 * @param length the index will be created using only the first length
	 *        characters/bytes of data
	 * @return creation statement
	 */
	public static String getDBSpecificIndexCreateStatement(String databaseName,
			String tableName, String indexName, String indexColumn, int length) {
		String createStatement = null;

		if (!SQL_CREATEINDEX_WITH_LENGTH.containsKey(databaseName)) {
			createStatement = (String) SQL_CREATEINDEX_WITH_LENGTH.get(DEFAULT_NAME);
		} else {
			// find the specific statement, fill in the variables and return it
			createStatement = (String) SQL_CREATEINDEX_WITH_LENGTH.get(databaseName);
		}

		createStatement = createStatement.replaceAll("\\$table\\b", tableName).replaceAll("\\$name\\b", indexName).replaceAll("\\$column\\b", indexColumn).replaceAll(
				"\\$length\\b", Integer.toString(length));
		return createStatement;
	}
    
	/**
	 * Returns the database specific statement for dropping an index
	 * @param databaseName Database product name
	 * @param tableName Name of the table that contains the index
	 * @param indexName Name of the index you want to drop
	 * @return the drop index statement with given table und index name
	 */
	public static String getDBSpecificDropIndexStatement(String databaseName, String tableName, String indexName) {
		String dropStatement = null;

		if (!SQL_DROPINDEX.containsKey(databaseName)) {
			dropStatement = (String) SQL_DROPINDEX.get(DEFAULT_NAME);
		} else {
			// find the specific statement, fill in the variables and return it
			dropStatement = (String) SQL_DROPINDEX.get(databaseName);
		}

		dropStatement = dropStatement.replaceAll("\\$table\\b", tableName).replaceAll("\\$name\\b", indexName);
		return dropStatement;
	}

	/**
	 * Returns the database specific statement part for generating a default value
	 * @param databaseName Database product name
	 * @param tableName Name of the table that contains the column
	 * @param columnName Name of the column to create
	 * @return the statement part for generating a default value
	 */
	public static String getDBSpecificDefaultStatementPart(String databaseName, String tableName, String columnName) {
		String defaultPart = null;

		if (!SQL_DEFAULT.containsKey(databaseName)) {
			defaultPart = (String) SQL_DEFAULT.get(DEFAULT_NAME);
		} else {
			defaultPart = (String) SQL_DEFAULT.get(databaseName);
		}

		if (defaultPart != null) {
			defaultPart = defaultPart.replaceAll("\\$table\\b", tableName).replaceAll("\\$name\\b", columnName);
		}

		return defaultPart;
	}

	/**
	 * Returns the database specific statements for dropping a column
	 * @param databaseName Database product name
	 * @param tableName Name of the table that contains the column
	 * @param columnName Name of the column to drop
	 * @return statements to drop the column
	 */
	public static String[] getDBSpecificDropColumnStatements(String databaseName, String tableName, String columnName) {
		String[] dropColumn = null;

		if (!SQL_DROPCOLUMN.containsKey(databaseName)) {
			dropColumn = (String[]) SQL_DROPCOLUMN.get(DEFAULT_NAME);
		} else {
			dropColumn = (String[]) SQL_DROPCOLUMN.get(databaseName);
		}

		if (dropColumn != null) {
			String[] newDropColumn = new String[dropColumn.length];

			for (int i = 0; i < dropColumn.length; i++) {
				newDropColumn[i] = dropColumn[i].replaceAll("\\$table\\b", tableName).replaceAll("\\$name\\b", columnName);
			}
			dropColumn = newDropColumn;
		}

		return dropColumn;
	}

	/**
	 * Get the database specific schema name for the given DatabaseMetaData
	 * @param metaData the DatabaseMetaData object
	 * @return the schema name
	 */
	public static String getDBSpecificSchemaName(DatabaseMetaData metaData) {
		try {
			if (ORACLE_NAME.equals(metaData.getDatabaseProductName())) {
				return metaData.storesUpperCaseIdentifiers() ? metaData.getUserName().toUpperCase() : metaData.getUserName();
			}
		} catch (SQLException sqle) {// ignore sql exceptions
		}
		return null;
	}

	/**
	 * Get the database specific SQL comparison statement
	 * @param databaseName the database product name
	 * @param columnName the column name
	 * @param attributeType the attribute type
	 * @return the SQL comparison statement
	 */
	public static String getDBSpecificComparisonStatement(String databaseName,
			String columnName, int attributeType) {
		// currently, only blob in oracle needs special treatment
		if (attributeType == GenticsContentAttribute.ATTR_TYPE_BLOB) {
			if (ORACLE_NAME.equals(databaseName)) {
				return "dbms_lob.compare(" + columnName + ", ?) = 0";
			}
		} else if (attributeType == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG) {
			if (MSSQL_NAME.equals(databaseName)) {
				return columnName + " LIKE ?";
				// return " DataLength("+columnName+") != DataLength(?)";
				// return " Binary_Checksum("+columnName+") = Binary_Checksum(?)";
			} else if (ORACLE_NAME.equals(databaseName)) {
				return "dbms_lob.compare(" + columnName + ", ?) = 0";
			}
		}
		return columnName + " = ?";
	}

	/**
	 * Get the Column Definition for the given quickcolumn
	 * @param handle dbhandle
	 * @param databaseName database product name
	 * @param type attribute type
	 * @param quickName name of the quick column
	 * @return Column Definition or null if the given type does not support quick columns
	 * @throws NodeIllegalArgumentException if the column type is not known.
	 */
	public static DB.ColumnDefinition getQuickColumnDefinition(DBHandle handle, String databaseName, int type, String quickName) throws NodeIllegalArgumentException {
		DB.ColumnDefinition colDef = null;
		SQLDatatype datatype = null;

		switch (type) {
		case GenticsContentAttribute.ATTR_TYPE_TEXT:
			datatype = getDBSpecificSQLDatatype(databaseName, "TEXT");
			break;

		case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
			datatype = getDBSpecificSQLDatatype(databaseName, "CLOB");
			break;

		case GenticsContentAttribute.ATTR_TYPE_OBJ:
			datatype = getDBSpecificSQLDatatype(databaseName, "SHORTTEXT");
			break;

		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			datatype = getDBSpecificSQLDatatype(databaseName, "INTEGER");
			break;

		case GenticsContentAttribute.ATTR_TYPE_DATE:
			datatype = getDBSpecificSQLDatatype(databaseName, "DATE");
			break;

		case GenticsContentAttribute.ATTR_TYPE_LONG:
			datatype = getDBSpecificSQLDatatype(databaseName, "LONG");
			break;

		case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			datatype = getDBSpecificSQLDatatype(databaseName, "DOUBLE");
			break;

		case GenticsContentAttribute.ATTR_TYPE_BLOB:
		case GenticsContentAttribute.ATTR_TYPE_BINARY:
			datatype = getDBSpecificSQLDatatype(databaseName, "BLOB");
			break;

		default:
			throw new NodeIllegalArgumentException(
					"Unknown column type {" + type + "} - for quickName: {" + quickName + "} - databaseName: {" + databaseName + "}");
		}
		if (datatype != null) {
			colDef = new DB.ColumnDefinition(handle.getContentMapName(), quickName, datatype.getSqlType(), datatype.getSqlTypeName(), true, null, true);
		}
		return colDef;
	}

	/**
	 * Get the meta attribute columns for non-mccr contentrepository datasources
	 * @return map of meta attribute columns
	 */
	public static Map getDefaultColumnTypes() {
		return getDefaultColumnTypes(false);
	}

	/**
	 * Get the meta attribute columns
	 * @param mccrDatasource true for mccr, false if not
	 * @return map of meta attribute columns
	 */
	public static Map getDefaultColumnTypes(boolean mccrDatasource) {
		if (mccrDatasource) {
			return defaultMCCRColumnTypes;
		} else {
			return defaultColumnTypes;
		}
	}

	public static String getQuickColumn(DBHandle handle, String attrib) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		AttributeType type = ((AttributeType) typeMap.get(attrib));

		if (type == null) {
			return null;
		}
		return type.getQuickName();
	}

	private static AttributeType getType(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		AttributeType type = ((AttributeType) typeMap.get(attrib));

		// NOP: attrib may be given with prefix "object." which cannot be
		// resolved here, if this is the case, we try to search again without
		// the prefix
		// TODO remove this dirty hack
		if (type == null && attrib != null && attrib.startsWith("object.")) {
			// strip the prefix "object." and search again
			type = ((AttributeType) typeMap.get(attrib.substring(7)));
		}
		return type;
	}

	/**
	 * Get the attribute type for the given handle, objecttype and name
	 * @param handle db handle
	 * @param objectType object type
	 * @param attrib attribute name
	 * @return the attribute type or null if it does not exist
	 * @throws CMSUnavailableException
	 */
	private static AttributeType getType(DBHandle handle, int objectType, String attrib) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		return ((AttributeType) typeMap.get(new AttributeKey(objectType, attrib)));
	}

	private static ObjectType getObjectType(DBHandle handle, int objectType) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}

		ObjectType type = (ObjectType) typeMap.get(new Integer(objectType));

		return type;
	}

	private static ContentRepositoryFeatures getFeatures(DBHandle handle) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}

		return (ContentRepositoryFeatures) typeMap.get(ContentRepositoryFeatures.KEY_OBJECT);
	}

	public static boolean isMultivalue(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute '" + attrib + "'");
		}
		return type.isMultivalue();
	}

	/**
	 * Check whether the given attribute stores its values in the filesystem
	 * @param handle handle
	 * @param name attribute name
	 * @return true if the values are stored in the filesystem, false if not
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static boolean isFilesystem(DBHandle handle, String name) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, name);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute '" + name + "'");
		}
		return type.isFilesystem();
	}

	/**
	 * Check whether the given attribute for the handle is excluded from versioning
	 * @param handle handle
	 * @param objectType object type
	 * @param attrib name of the attribute to check
	 * @return true when the attribute is excluded from versioning, false if not
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static boolean isAttributeExcludeVersioning(DBHandle handle, int objectType,
			String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, objectType, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute {" + attrib + "} for objecttype {" + objectType + "}");
		}
		return type.excludeVersioning;
	}

	/**
	 * Check whether the given objecttype is excluded from versioning
	 * @param handle handle
	 * @param objectType object type
	 * @return true when the objecttype is excluded from versioning, false if
	 *         not
	 * @throws CMSUnavailableException
	 */
	public static boolean isObjecttypeExcludeVersioning(DBHandle handle, int objectType) throws CMSUnavailableException {
		ObjectType type = getObjectType(handle, objectType);

		return type == null ? false : type.isExcludeVersioning();
	}

	/**
	 * Get an array of all existing objecttypes
	 * @param handle database handle
	 * @return array of objecttypes
	 * @throws CMSUnavailableException
	 */
	public static int[] getObjecttypes(DBHandle handle) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}

		Set keys = typeMap.keySet();
		int[] objectTypes = new int[keys.size()];
		int i = 0;

		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object element = (Object) iter.next();

			if (element instanceof Integer) {
				objectTypes[i++] = ((Integer) element).intValue();
			}
		}
		int[] realObjectTypes = new int[i];

		System.arraycopy(objectTypes, 0, realObjectTypes, 0, i);

		return realObjectTypes;
	}

	/**
	 * Get the attribute definitions from the given handle
	 * @param handle db handle
	 * @param optimized TRUE for only optimized, FALSE for non optimized, null
	 *        for all
	 * @param multivalue TRUE for only multivalue, FALSE for only non
	 *        multivalue, null for all
	 * @param filesystem TRUE for only filesystem, FALSE for only non filesystem, null for all
	 * @param objectTypes object types to restrict, null for no restriction
	 * @param attributeTypes attribute types to restrict, null for no
	 *        restriction
	 * @param attributeNames list of allowed attribute names, null for no restriction
	 * @return list of (eventually restricted) attribute types
	 * @throws CMSUnavailableException
	 */
	public static AttributeType[] getAttributeTypes(DBHandle handle, Boolean optimized,
			Boolean multivalue, Boolean filesystem, int[] objectTypes, int[] attributeTypes, String[] attributeNames) throws CMSUnavailableException {
		TimedCache cache = getHelperCache(handle);

		if (cache == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}
		HashMap typeMap = (HashMap) cache.get();

		if (typeMap == null) {
			throw new CMSUnavailableException("Type-Cache is not filled: " + typeCacheError);
		}

		Collection collectedAttributeNames = new Vector();
		Collection collectedTypes = new Vector();

		for (Iterator iterator = typeMap.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();

			if (entry.getValue() instanceof AttributeType && entry.getKey() instanceof AttributeKey) {
				AttributeType type = (AttributeType) entry.getValue();
				AttributeKey key = (AttributeKey) entry.getKey();

				if (isAllowedType(attributeTypes, type.getType()) && isFlagAllowed(optimized, type.isOptimized())
						&& isFlagAllowed(multivalue, type.isMultivalue()) && isFlagAllowed(filesystem, type.isFilesystem())
						&& isAllowedType(objectTypes, key.objectType.intValue()) && isNameAllowed(attributeNames, key.attributeName)) {
					if (!collectedAttributeNames.contains(key.attributeName)) {
						collectedAttributeNames.add(key.attributeName);
						collectedTypes.add(type);
					}
				}
			}
		}

		return (AttributeType[]) collectedTypes.toArray(new AttributeType[collectedTypes.size()]);
	}

	/**
	 * Check whether the name is among the allowed names
	 * @param allowedNames list of allowed names, null for allowing all names
	 * @param name name to check, when set to null, this method returns false
	 * @return true when the name is allowed, false if not
	 */
	private static boolean isNameAllowed(String[] allowedNames, String name) {
		if (allowedNames == null) {
			return true;
		} else if (name == null) {
			return false;
		} else {
			for (int i = 0; i < allowedNames.length; i++) {
				if (name.equals(allowedNames[i])) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Check whether the flag (optimized or multivalue) is allowed
	 * @param allowedFlag whether the flag shall be set to TRUE, FALSE or is not
	 *        checked
	 * @param flag the current setting of the flag
	 * @return true when the flag is allowed, false if not
	 */
	private static boolean isFlagAllowed(Boolean allowedFlag, boolean flag) {
		if (allowedFlag == null) {
			return true;
		} else {
			return allowedFlag.booleanValue() == flag;
		}
	}

	/**
	 * Check whether the given type is one of the allowed types
	 * @param allowedTypes list of allowed types, null to allow all
	 * @param type type in question
	 * @return true if the type is allowed, false if not
	 */
	private static boolean isAllowedType(int[] allowedTypes, int type) {
		if (allowedTypes == null) {
			return true;
		} else {
			for (int i = 0; i < allowedTypes.length; i++) {
				if (type == allowedTypes[i]) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Get all attributetypes for a given objecttype
	 * @param handle dbhandle
	 * @param objecttype objecttype
	 * @return names of attributetypes for the given objecttype
	 * @throws CMSUnavailableException
	 */
	public static String[] getAttributetypes(DBHandle handle, int objecttype) throws CMSUnavailableException {
		SimpleResultProcessor rs = new SimpleResultProcessor();

		try {
			DB.query(handle, "SELECT name FROM " + handle.getContentAttributeTypeName() + " where objecttype = ?", new Object[] { new Integer(objecttype)}, rs);

			Iterator it = rs.iterator();
			List attributeNames = new Vector();

			while (it.hasNext()) {
				SimpleResultRow row = (SimpleResultRow) it.next();

				attributeNames.add(row.getString("name"));
			}

			return (String[]) attributeNames.toArray(new String[attributeNames.size()]);
		} catch (SQLException e) {
			throw new CMSUnavailableException("Error while fetching the attributetypes for objecttype {" + objecttype + "} for {" + handle + "}", e);
		}
	}

	/**
	 * Get the names of all attributetypes of the given objecttype that are excluded from versioning
	 * @param handle db handle
	 * @param objectType object type
	 * @return array of excluded attributes
	 */
	public static String[] getNonVersioningAttributes(DBHandle handle, int objectType) {
		Vector excludedAttributes = new Vector();

		try {
			if (DB.fieldExists(handle, handle.getContentAttributeTypeName(), EXCLUDE_VERSIONING_FIELD)) {
				SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

				DB.query(handle, "SELECT name from " + handle.getContentAttributeTypeName() + " where objecttype = ? and exclude_versioning = ?",
						new Object[] { new Integer(objectType), Boolean.TRUE}, resultProcessor);
				for (Iterator iter = resultProcessor.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					excludedAttributes.add(row.getString("name"));
				}
			}
		} catch (Exception ex) {
			NodeLogger.getLogger(DatatypeHelper.class).error(
					"Error while getting non-Versioning attributes for objecttype {" + objectType + "} for {" + handle + "}", ex);
		}
		return (String[]) excludedAttributes.toArray(new String[excludedAttributes.size()]);
	}

	/**
	 * clear all data collected in {@link #handledTypeCacheMap}. this method
	 * should be called while shutting down the application
	 */
	public final static void clear() {
		handledTypeCacheMap.clear();
	}

	private static TimedCache getHelperCache(final DBHandle handle) {
		TimedCache cache = (TimedCache) DatatypeHelper.handledTypeCacheMap.get(handle);

		if (cache != null) {
			return cache;
		} else {
			cache = new TimedCache(2 * 60 * 1000, new CacheTimeoutListener() {
				public Object updateCacheObject(Object o) {
					return DatatypeHelper.updateCacheObject(handle, o);
				}
			});
			DatatypeHelper.handledTypeCacheMap.put(handle, cache);
			return cache;
		}
	}

	public static int getDatatype(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}
		return type.getType();
	}

	/**
	 * get the linkedObjectType property for the attribute attrib
	 * @param handle database handle
	 * @param attrib name of the attribute
	 * @return linkedObjectType or 0 if attribute is no object link
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException when the attribute does not exist
	 */
	public static int getLinkedObjectType(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}
		return type.getLinkedObjectType();
	}

	/**
	 * get the name of the foreign link attribute if the attribute is a foreign
	 * object link
	 * @param handle database handle
	 * @param attrib name of the attribute
	 * @return name of the foreign link attribute or null if attribute is no
	 *         foreign link
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException when the attribute does not exist
	 */
	public static String getForeignLinkAttribute(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}

		return type.getForeignLinkedAttribute();
	}

	/**
	 * Get the ruleTree for the foreignlink attribute, if any was given.
	 * @param handle database handle
	 * @param attrib name of the attribute
	 * @param startObject main object
	 * @return ruleTree for the foreignlink attribute or null, if none given
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static RuleTree getForeignLinkAttributeRuleTree(DBHandle handle, String attrib, Resolvable startObject) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}

		return type.getForeignLinkAttributeRuleTree(startObject);
	}
    
	public static String getForeignLinkAttributeRuleTreeString(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}

		return type.getForeignLinkAttributeRuleTreeString();
	}

	/**
	 * Get the rule string for the foreignlink attribute, if any was given.
	 * @param handle database handle
	 * @param attrib name of the attribute
	 * @return rule for the foreignlink attribute or null, if none given
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static String getForeignLinkAttributeRuleString(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}

		return type.getForeignLinkAttributeRule();
	}

	/**
	 * Check whether a foreignlinkattribute rule is defined or not
	 * @param handle database handle
	 * @param attrib name of the attribute
	 * @return true when an attribute rule is defined, false if not
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static boolean hasForeignLinkAttributeRule(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}

		String foreignLinkAttributeRule = type.getForeignLinkAttributeRule();

		return foreignLinkAttributeRule != null && foreignLinkAttributeRule.length() > 0;
	}

	public static AttributeType getComplexDatatype(DBHandle handle, String attrib) throws CMSUnavailableException, NodeIllegalArgumentException {
		AttributeType type = getType(handle, attrib);

		if (type == null) {
			throw new NodeIllegalArgumentException("Could not find attribute " + attrib);
		}
		return type;
	}

	/**
	 * Get the name of the DB column, where the data of the given type is stored
	 * @param type attribute type
	 * @return name of the DB column or null if not found
	 */
	public static String getTypeColumn(int type) {
		switch (type) {
		case GenticsContentAttribute.ATTR_TYPE_BINARY:
			return "value_bin";

		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			return "value_int";

		case GenticsContentAttribute.ATTR_TYPE_OBJ:
			return "value_text";

		case GenticsContentAttribute.ATTR_TYPE_TEXT:
			return "value_text";

		case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
			return "value_clob";

		case GenticsContentAttribute.ATTR_TYPE_BLOB:
			return "value_blob";

		case GenticsContentAttribute.ATTR_TYPE_LONG:
			return "value_long";

		case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			return "value_double";

		case GenticsContentAttribute.ATTR_TYPE_DATE:
			return "value_date";

		case GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ:
			return "";
		}
		return null;
	}

	public static String[] getTypeColumns(int type) {
		switch (type) {
		case GenticsContentAttribute.ATTR_TYPE_ALL:
			// TODO !!! IMPORTANT !!!
			return new String[] {
				// "value_clob", "value_bin", "value_int", "value_text",
				// "value_blob"
				"value_bin", "value_int", "value_text", "value_blob", "value_clob", "value_long", "value_double", "value_date" };

		case GenticsContentAttribute.ATTR_TYPE_ALL_TEXT:
			// TODO !!! IMPORTANT !!!
			return new String[] {
				// "value_bin", "value_text"
				"value_clob", "value_text" };

		default:
			return new String[] { getTypeColumn(type) };
		}
	}

	/*
	 * public static String toDBValue( String value, int type ) { switch ( type ) {
	 * case GenticsContentAttribute.ATTR_TYPE_INTEGER: return value; case
	 * GenticsContentAttribute.ATTR_TYPE_BINARY: case
	 * GenticsContentAttribute.ATTR_TYPE_TEXT: case
	 * GenticsContentAttribute.ATTR_TYPE_OBJ: return "'" + DB.sqlSlashes(value) +
	 * "'"; } return value; }
	 */

	public static int compare(GenticsContentAttribute a1, GenticsContentAttribute a2,
			int dataType) throws CMSUnavailableException {
		int ret = 0;

		// TODO: add all datatypes here
		switch (dataType) {
		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			try {
				int i1 = Integer.parseInt(a1.getNextValue());
				int i2 = Integer.parseInt(a2.getNextValue());

				if (i1 < i2) {
					ret = -1;
				}
				if (i1 > i2) {
					ret = 1;
				}
			} catch (NumberFormatException nfe) {}
			break;

		case GenticsContentAttribute.ATTR_TYPE_TEXT:
			String s1 = a1.getNextValue();
			String s2 = a2.getNextValue();

			ret = s1.compareTo(s2);
			break;
		}
		return ret;
	}

	/**
	 * Check whether the given handle allows for excluding attributetypes from versioning
	 * @param handle db handle
	 * @return true when the exclusion of attributetypes from versioning is allowed, false if not
	 * @throws CMSUnavailableException
	 */
	public static boolean isAttributeExcludeVersioningColumn(DBHandle handle) throws CMSUnavailableException {
		ContentRepositoryFeatures features = getFeatures(handle);

		return features.isAttributeTypeExcludeVersioningColumn();
	}

	/**
	 * Check whether the given handle allows for excluding objecttypes from versioning
	 * @param handle db handle
	 * @return true when the exclusion of objecttypes from versioning is allowed, false if not
	 * @throws CMSUnavailableException
	 */
	public static boolean isObjectExcludeVersioningColumn(DBHandle handle) throws CMSUnavailableException {
		ContentRepositoryFeatures features = getFeatures(handle);

		return features.isObjectTypeExcludeVersioningColumn();
	}

	/**
	 * Check whether the given handle allows for setting the filesystem flag for attribute types
	 * @param handle db handle
	 * @return true if setting the flag is possible, false if not
	 * @throws CMSUnavailableException
	 */
	public static boolean isAttributeFilesystemColumn(DBHandle handle) throws CMSUnavailableException {
		ContentRepositoryFeatures features = getFeatures(handle);

		return features.isAttributeTypeFilesystemColumn();
	}

	/**
	 * Check the given db handle for all tables and columns required for the contentrepository
	 * @param handleId id of the checked handle
	 * @param handle database handle
	 * @param doRepair true when missing tables and columns shall be created automatically, false if not
	 * @return true when the handle passed all checks successfully (after eventually repairs), false if not
	 * @throws CMSUnavailableException
	 */
	public static boolean checkContentRepository(String handleId, DBHandle handle, boolean doRepair) throws CMSUnavailableException {
		NodeLogger logger = NodeLogger.getNodeLogger(DatatypeHelper.class);

		if (handle == null) {
			logger.error("Cannot check datasource-handle {" + handleId + "}");
			return false;
		}
		String errorMessage = "Error while checking datasource-handle {" + handleId + "} {" + handle + "}: ";
		boolean checkPassed = true;

		if (logger.isInfoEnabled()) {
			logger.info("Checking tables for handle {" + handleId + "} ...");
		}
		TableDefinition[] contentRepositoryTables = getContentRepositoryTables(handle);

		try {
			for (int i = 0; i < contentRepositoryTables.length; ++i) {
				if (logger.isInfoEnabled()) {
					logger.info("Check table " + contentRepositoryTables[i].getTableName());
				}
				boolean tableTest = DB.checkTable(handle, contentRepositoryTables[i], handleId, doRepair);

				checkPassed &= tableTest;
				if (!tableTest) {
					logger.error("Table {" + contentRepositoryTables[i].getTableName() + "} did not pass all required tests!");
				}
			}
		} catch (SQLException ex) {
			throw new CMSUnavailableException(errorMessage, ex);
		}

		if (!checkPassed) {
			logger.error("The database for the datasource-handle {" + handleId + "}, {" + handle + "} does not meet all requirements for a contentrepository!");
		}
		return checkPassed;
	}

	/**
	 * Inner helper class for attribute keys (contains the objecttype and attribute name)
	 */
	protected static class AttributeKey {
		protected Integer objectType;
		protected String attributeName;

		/**
		 * Generate instance of AttributeKey
		 * @param objectType objecttype
		 * @param attributeName attribute name
		 */
		public AttributeKey(int objectType, String attributeName) {
			this.objectType = new Integer(objectType);
			this.attributeName = attributeName;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof AttributeKey) {
				AttributeKey key = (AttributeKey) obj;

				return key.objectType.equals(objectType) && key.attributeName.equals(attributeName);
			} else {
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return objectType.hashCode() + attributeName.hashCode();
		}
	}
}
