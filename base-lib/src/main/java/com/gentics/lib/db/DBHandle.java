package com.gentics.lib.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang.mutable.MutableBoolean;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Created by IntelliJ IDEA. User: andreas Date: 27.02.2004 Time: 14:51:04 To
 * change this template use File | Settings | File Templates.
 */
public class DBHandle {
	public static final int CONNECTOR_TYPE_GENERIC = 1;

	public static final int CONNECTOR_TYPE_NAMING = 2;

	public final static String DEFAULT_CONTENTSTATUS_NAME = "contentstatus";

	public final static String DEFAULT_CONTENTOBJECT_NAME = "contentobject";

	public final static String DEFAULT_CONTENTATTRIBUTETYPE_NAME = "contentattributetype";

	public final static String DEFAULT_CONTENTMAP_NAME = "contentmap";

	public final static String DEFAULT_CONTENTATTRIBUTE_NAME = "contentattribute";

	public final static String DEFAULT_CHANNEL_NAME = "channel";

	private static NodeLogger logger = NodeLogger.getNodeLogger(DBHandle.class);

	/**
	 * name of the contentstatus table or null for the default value
	 * {@link #DEFAULT_CONTENTSTATUS_NAME}
	 */
	private String contentStatusName = null;

	/**
	 * name of the contentobject table or null for the default value
	 * {@link #DEFAULT_CONTENTOBJECT_NAME}
	 */
	private String contentObjectName = null;

	/**
	 * name of the contentattributetype table or null for the default value
	 * {@link #DEFAULT_CONTENTATTRIBUTETYPE_NAME}
	 */
	private String contentAttributeTypeName = null;

	/**
	 * name of the contentmap table or null for the default value
	 * {@link #DEFAULT_CONTENTMAP_NAME}
	 */
	private String contentMapName = null;

	/**
	 * name of the contentattribute table or null for the default value
	 * {@link #DEFAULT_CONTENTATTRIBUTE_NAME}
	 */
	private String contentAttributeName = null;

	/**
	 * Name of the channel table or null for the default value {@link #DEFAULT_CHANNEL_NAME}
	 */
	private String channelName = null;

	private String name;

	private String description;

	private int type;

	/**
	 * preferred concat operator of the underlying database (null if database
	 * does not support concat by operator)
	 */
	private String preferredConcatOperator = null;

	/**
	 * preferred concat function of the underlying database (null if database
	 * does not support concat by function)
	 */
	private String preferredConcatFunction = null;
    
	/**
	 * StringTruncator which is used truncate the data if it is longer than the db column. 
	 */
	private StringLengthManipulator stringLengthManipulator = null;

	/**
	 * True if the DB stores uppercase identifiers
	 */
	private boolean upperCaseIdentifiers = false;

	/**
	 * True if the DB supports batch updates, false if not
	 */
	private boolean batchUpdates = false;

	/**
	 * Fetch size, that is used for statements. 0 is the default value.
	 */
	private int fetchSize = 0;

	/**
	 * No LIMIT clause (or similar) is supported. - or it was not yet tested.
	 */
	public static final int SUPPORTED_LIMIT_CLAUSE_UNSUPPORTED = 0;
    
	/**
	 * SELECT xxx FROM xxx LIMIT start,count
	 * (mysql syntax, sql99)
	 */
	public static final int SUPPORTED_LIMIT_CLAUSE_LIMIT = 1;
    
	/**
	 * SELECT xxx FROM xxx WHERE ROWNUM >= start AND ROWNUM <= start+count
	 * (oracle syntax)
	 */
	public static final int SUPPORTED_LIMIT_CLAUSE_ROWNUM = 2;

	public final static Integer FOURTYTWOINT = new Integer(42);
	public final static String FOURTYTWOSTRING = FOURTYTWOINT.toString();
	public final static String FOURTYTWONAME = "fourtytwo";
	public final static String VERYLONGTEXT = StringUtils.repeat("a", 255);
	public final static String VERYLONGTEXTNAME = "verylongtext";

	/**
	 * These are the statements to check for how non-characted need to be cast
	 * such that they can be compared with character data
	 */
	public final static String[][] TEXTCASTSTATEMENTS = new String[][] {
		new String[] {
			"VARCHAR", "SELECT CAST(? AS VARCHAR) " + FOURTYTWONAME + ", CAST(? AS VARCHAR) " + VERYLONGTEXTNAME, "false"}, new String[] {
			"VARCHAR", "SELECT CAST(? AS VARCHAR) " + FOURTYTWONAME + ", CAST(? AS VARCHAR) " + VERYLONGTEXTNAME + " FROM dual", "false"},
		new String[] {
			"VARCHAR(255)", "SELECT CAST(SUBSTR(?, 1, 255) AS VARCHAR(255)) " + FOURTYTWONAME + ", CAST(SUBSTR(?, 1, 255) AS VARCHAR(255)) " + VERYLONGTEXTNAME,
			"true"},
		new String[] {
			"VARCHAR(255)",
			"SELECT CAST(SUBSTR(?, 1, 255) AS VARCHAR(255)) " + FOURTYTWONAME + ", CAST(SUBSTR(?, 1, 255) AS VARCHAR(255)) " + VERYLONGTEXTNAME + " FROM dual",
			"true"}, new String[] {
			"VARCHAR(255)", "SELECT CAST(? AS VARCHAR(255)) " + FOURTYTWONAME + ", CAST(? AS VARCHAR(255)) " + VERYLONGTEXTNAME, "false"}, new String[] {
			"VARCHAR(255)", "SELECT CAST(? AS VARCHAR(255)) " + FOURTYTWONAME + ", CAST(? AS VARCHAR(255)) " + VERYLONGTEXTNAME + " FROM dual", "false"},
		new String[] {
			"CHAR", "SELECT CAST(? AS CHAR) " + FOURTYTWONAME + ", CAST(? AS CHAR) " + VERYLONGTEXTNAME, "false"}, new String[] {
			"CHAR", "SELECT CAST(? AS CHAR) " + FOURTYTWONAME + ", CAST(? AS CHAR) " + VERYLONGTEXTNAME + " FROM dual", "false"}
	};

	/**
	 * Possible Dummy statements
	 */
	public final static String[] DUMMY_STATEMENTS = { "SELECT 1", "SELECT 1 FROM dual"};

	/**
	 * Defines the supported LIMIT clause for sql statements, see
	 * SUPPORTED_LIMIT_CLAUSE_* constants.
	 */
	private int supportedLimitClause = SUPPORTED_LIMIT_CLAUSE_UNSUPPORTED;

	/**
	 * name of the datatype (eventually including length) for casting fields to texts, null if cast not possible
	 */
	private String textCastName = null;

	/**
	 * Flag to determine whether substr() must be used when casting
	 */
	private boolean substrWhenCasting = false;

	/**
	 * flag to mark handles that have their features already tested
	 */
	private boolean featuresTested = false;

	/**
	 * flag that is set when the table names are set (even if they are set to
	 * the default values)
	 */
	private boolean tableNamesSet = false;

	/**
	 * flag whether db meta data shall be cached or not
	 */
	private boolean cacheDBMetaData = false;

	/**
	 * DB schema to be used for accessing database metadata
	 */
	private String dbSchema = null;

	/**
	 * DB Specific dummy statement
	 */
	private String dummyStatement = null;

	/**
	 * SQL Handle
	 */
	private SQLHandle sqlHandle;

	/**
	 * Database product name
	 */
	private String databaseProductName;

	public DBHandle(String name, String description, int type, boolean cacheDBMetaData) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.cacheDBMetaData = cacheDBMetaData;
	}

	/**
	 * Set the table names (null for default values) or checks whether
	 * previously set table names are identical
	 * @param contentStatusName name for the contentstatus table or null/empty for the
	 *        default
	 * @param contentObjectName name for the contentobject table or null/empty for the
	 *        default
	 * @param contentAttributeTypeName name for the contentattributetype table
	 *        or null/empty for the default
	 * @param contentMapName name for the contentmap table or null/empty for the
	 *        default
	 * @param contentAttributeName name for the contentattribute table or null/empty
	 *        for the default
	 * @param channelName name for the channel table or null/empty for the default
	 * @throws DatasourceException when the names are already set but are
	 *         different than the given names
	 */
	public void setTableNames(String contentStatusName, String contentObjectName,
			String contentAttributeTypeName, String contentMapName, String contentAttributeName, String channelName) throws DatasourceException {
		// normalize the configured names
		contentStatusName = StringUtils.isEmpty(contentStatusName) ? null : contentStatusName;
		contentObjectName = StringUtils.isEmpty(contentObjectName) ? null : contentObjectName;
		contentAttributeTypeName = StringUtils.isEmpty(contentAttributeTypeName) ? null : contentAttributeTypeName;
		contentMapName = StringUtils.isEmpty(contentMapName) ? null : contentMapName;
		contentAttributeName = StringUtils.isEmpty(contentAttributeName) ? null : contentAttributeName;
		channelName = StringUtils.isEmpty(channelName) ? null : channelName;

		if (!tableNamesSet) {
			this.contentStatusName = contentStatusName;
			this.contentObjectName = contentObjectName;
			this.contentAttributeTypeName = contentAttributeTypeName;
			this.contentMapName = contentMapName;
			this.contentAttributeName = contentAttributeName;
			this.channelName = channelName;
			tableNamesSet = true;
		} else {
			if (!StringUtils.isEqual(this.contentStatusName, contentStatusName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for contentstatus table: {" + getDescriptiveName(this.contentStatusName) + "} vs. {"
						+ getDescriptiveName(contentStatusName) + "}");
			}
			if (!StringUtils.isEqual(this.contentObjectName, contentObjectName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for contentobject table: {" + getDescriptiveName(this.contentObjectName) + "} vs. {"
						+ getDescriptiveName(contentObjectName) + "}");
			}
			if (!StringUtils.isEqual(this.contentAttributeTypeName, contentAttributeTypeName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for contentattributetype table: {" + getDescriptiveName(this.contentAttributeTypeName) + "} vs. {"
						+ getDescriptiveName(contentAttributeTypeName) + "}");
			}
			if (!StringUtils.isEqual(this.contentMapName, contentMapName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for contentmap table: {" + getDescriptiveName(this.contentMapName) + "} vs. {"
						+ getDescriptiveName(contentMapName) + "}");
			}
			if (!StringUtils.isEqual(this.contentAttributeName, contentAttributeName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for contentattribute table: {" + getDescriptiveName(this.contentAttributeName) + "} vs. {"
						+ getDescriptiveName(contentAttributeName) + "}");
			}
			if (!StringUtils.isEqual(this.channelName, channelName)) {
				throw new DatasourceException(
						"Invalid configuration of tablename for channel table: {" + getDescriptiveName(this.channelName) + "} vs. {" + getDescriptiveName(channelName)
						+ "}");
			}
		}
	}

	/**
	 * Get the descriptive name for the configuration (the name itself or
	 * "(default)" when empty)
	 * @param configuredName configured table name
	 * @return descriptive name
	 */
	private final static String getDescriptiveName(String configuredName) {
		return StringUtils.isEmpty(configuredName) ? "(default)" : configuredName;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getType() {
		return type;
	}

	/**
	 * Test for database features (if not already done before)
	 *
	 */
	public void testFeatures() {
		if (!featuresTested) {
			testConcatFeatures();
			testTextCastFeature();
			testLimitClauses();
			testLengthSemantics();
			testDummyStatements();
			readDatabaseMetaData();
			featuresTested = true;
		}
	}
    
	/**
	 * Test which dummy statement works
	 */
	protected void testDummyStatements() {
		for (String dummyStatement : DUMMY_STATEMENTS) {
			try {
				DB.query(this, dummyStatement, null, new SimpleResultProcessor(), false);
				this.dummyStatement = dummyStatement;
				return;
			} catch (SQLException e) {}
		}
	}

	/**
	 * We use some heuritics to determine how to truncate strings. 
	 */
	protected void testLengthSemantics() {
		try {
			databaseProductName = DB.getDatabaseProductName(this);

			if (databaseProductName.equals(DatatypeHelper.MYSQL_NAME) || databaseProductName.equals(DatatypeHelper.MARIADB_NAME)) {
				// MySQL always uses character length semantics ever since 5.0.3
				// and we don't support anything lower than that.
				// Characters outside the BMP are not supported until 5.5.3
				// but only after switching to "utf8mb4" character set,
				// or after Oracle decides to switch the meaning of "utf8" from
				// "utf8mb3" to "utf8mb4". See
				// http://dev.mysql.com/doc/refman/5.5/en/charset-unicode-utf8mb3.html
				stringLengthManipulator = new SurrogatePairAwareCodePointTruncator();

			} else if (databaseProductName.equals(DatatypeHelper.MSSQL_NAME)) {
				stringLengthManipulator = new SurrogatePairAwareCodeUnitTruncator();

			} else if (databaseProductName.equals(DatatypeHelper.ORACLE_NAME)) {
				// Stupidly enough, Oracle has configurable length semantics for
				// for varchar2/char and fixed semantics for the deprecated
				// nchar/nvarchar2. We must test for NLS_LENGTH_SEMANTICS and
				// NLS_CHARACTERSET and set our SLM accordingly.
				// Notice that AL32UTF8 (which is UTF-8 as per spec) is the
				// only supported multibyte encoding atm.

				final MutableBoolean byteModeBuffer = new MutableBoolean(false);
				final StringBuffer selectedCharsetBuffer = new StringBuffer();
				DB.query(this, "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET'", new ResultProcessor() {

					public void takeOver(ResultProcessor p) {
					}

					public void process(ResultSet rs) throws SQLException {
						rs.next();
						selectedCharsetBuffer.append(rs.getString(1));
					}
				});
				DB.query(this, "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_LENGTH_SEMANTICS'", new ResultProcessor() {

					public void takeOver(ResultProcessor p) {
					}

					public void process(ResultSet rs) throws SQLException {
						rs.next();
						byteModeBuffer.setValue(rs.getString(1).toUpperCase().equals("BYTE"));
					}
				});

				String selectedCharset = selectedCharsetBuffer.toString().toUpperCase();
				boolean byteMode = byteModeBuffer.booleanValue();

				// All multibyte charactersets supported by oracle for storage
				// except for AL32UTF8
				String[] unsupportedCharsets = { "JA16EUC", "JA16EUCTILDE", "JA16SJIS", "JA16SJISTILDE", "KO16MSWIN949", "ZHS16GBK", "ZHT16HKSCS",
						"ZHT16MSWIN950", "ZHT32EUC", "JA16DBCS", "JA16EBCDIC930", "KO16DBCS", "JA16VMS", "KO16KSC5601", "KO16KSCCS", "ZHS16CGB231280",
						"ZHT16BIG5", "ZHT16CCDC", "ZHT16DBT", "ZHT16HKSCS31", "ZHT32SOPS", "ZHT32TRIS", "ZHS16DBCS", "ZHT16DBCS", "UTFE", "UTF8" };

				if (byteMode && Arrays.asList(unsupportedCharsets).contains(selectedCharset)) {
					// use defensive yet still unsafe truncator
					stringLengthManipulator = new ByteCountTruncator();
					logger.warn("Unsupported multibyte character set '" + selectedCharset
							+ "' detected in oracle database. Consider either using 'AL32UTF8' or switching NLS_LENGTH_SEMANTICS to 'CHAR'.");
				} else if (byteMode && selectedCharset.equals("AL32UTF8")) {
					stringLengthManipulator = new ByteCountTruncator();
				} else {
					// In char mode and 8 bit charsets we can use real character
					// counts
					stringLengthManipulator = new SurrogatePairAwareCodePointTruncator();
				}
			}
			if (stringLengthManipulator != null) {
				return;
			}
		} catch (SQLException e) {
		}

		// in doubt use byte-length (it's always shorter than character-length
		// and therefore safer)
		stringLengthManipulator = new ByteCountTruncator();
		logger.warn("Database with unknown char length semantics detected. Assuming UTF8-byte-length-semantics.");
	}

	/**
	 * Do the tests for concat features
	 */
	protected void testConcatFeatures() {
		SimpleResultProcessor rs = new SimpleResultProcessor();

		// test for concat function
		try {
			DB.query(this, "SELECT concat(?, ?) test", new String[] { "a", "b"}, rs, false);
			SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

			preferredConcatFunction = "ab".equals(row.getString("test")) ? "concat" : null;
		} catch (SQLException e) {// we ignore this exception
		}
		if (preferredConcatFunction == null) {
			try {
				DB.query(this, "SELECT concat(?, ?) test FROM dual", new String[] { "a", "b"}, rs, false);
				SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

				preferredConcatFunction = "ab".equals(row.getString("test")) ? "concat" : null;
			} catch (SQLException e) {// we ignore this exception
			}
		}

		// test for + operator
		try {
			DB.query(this, "SELECT ? + ? test", new String[] { "a", "b"}, rs, false);
			SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

			preferredConcatOperator = "ab".equals(row.getString("test")) ? "+" : null;
		} catch (SQLException e) {// we ignore this exception
		}
		if (preferredConcatOperator == null) {
			try {
				DB.query(this, "SELECT ? + ? test FROM dual", new String[] { "a", "b"}, rs, false);
				SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

				preferredConcatOperator = "ab".equals(row.getString("test")) ? "+" : null;
			} catch (SQLException e) {// we ignore this exception
			}
		}
		// test for || operator
		if (preferredConcatOperator == null) {
			try {
				DB.query(this, "SELECT ? || ? test", new String[] { "a", "b"}, rs, false);
				SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

				preferredConcatOperator = "ab".equals(row.getString("test")) ? "||" : null;
			} catch (SQLException e) {// we ignore this exception
			}
		}
		if (preferredConcatOperator == null) {
			try {
				DB.query(this, "SELECT ? || ? test FROM dual", new String[] { "a", "b"}, rs, false);
				SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

				preferredConcatOperator = "ab".equals(row.getString("test")) ? "||" : null;
			} catch (SQLException e) {// we ignore this exception
			}
		}
	}

	/**
	 * Do the tests for text case feature
	 */
	protected void testTextCastFeature() {
		SimpleResultProcessor rs = new SimpleResultProcessor();

		for (int i = 0; i < TEXTCASTSTATEMENTS.length && textCastName == null; i++) {
			try {
				DB.query(this, TEXTCASTSTATEMENTS[i][1], new Object[] { FOURTYTWOINT, VERYLONGTEXT}, rs, false);
				SimpleResultRow row = (SimpleResultRow) (rs.iterator().next());

				if (FOURTYTWOSTRING.equals(row.getString(FOURTYTWONAME)) && VERYLONGTEXT.equals(row.getString(VERYLONGTEXTNAME))) {
					textCastName = TEXTCASTSTATEMENTS[i][0];
					substrWhenCasting = ObjectTransformer.getBoolean(TEXTCASTSTATEMENTS[i][2], substrWhenCasting);
				}
			} catch (SQLException e) {// we ignore this exception
			}
		}
	}
    
	/**
	 * tests the kind of LIMIT clause the database is supporting.
	 */
	protected void testLimitClauses() {
		SimpleResultProcessor rs = new SimpleResultProcessor();

		try {
			DB.query(this, "SELECT 5 LIMIT 0,5", null, rs, false);
			supportedLimitClause = SUPPORTED_LIMIT_CLAUSE_LIMIT;
		} catch (SQLException e) {// ignoring exception
		}
        
		if (supportedLimitClause == SUPPORTED_LIMIT_CLAUSE_UNSUPPORTED) {
			try {
				DB.query(this, "SELECT 5 FROM dual LIMIT 0,5", null, rs, false);
				supportedLimitClause = SUPPORTED_LIMIT_CLAUSE_LIMIT;
			} catch (SQLException e) {// ignoring exception
			}
		}
		if (supportedLimitClause == SUPPORTED_LIMIT_CLAUSE_UNSUPPORTED) {
			try {
				DB.query(this, "SELECT 5 FROM dual WHERE ROWNUM >= 0 AND ROWNUM <= 5", null, rs, false);
				supportedLimitClause = SUPPORTED_LIMIT_CLAUSE_ROWNUM;
			} catch (SQLException e) {// ignoring exception
			}
		}
	}

	/**
	 * Read the database meta data
	 */
	protected void readDatabaseMetaData() {
		try {
			DB.handleDatabaseMetaData(this, new DatabaseMetaDataHandler() {

				/* (non-Javadoc)
				 * @see com.gentics.lib.db.DatabaseMetaDataHandler#handleMetaData(java.sql.DatabaseMetaData)
				 */
				public void handleMetaData(DatabaseMetaData metaData) throws SQLException {
					upperCaseIdentifiers = metaData.storesUpperCaseIdentifiers();
					batchUpdates = metaData.supportsBatchUpdates();
				}
			});
		} catch (Exception ignored) {}
	}

	/**
	 * Get preferred concat function or null if no concat function supported
	 * @return name of the preferred concat function or null
	 */
	public String getPreferredConcatFunction() {
		return preferredConcatFunction;
	}
    
	/**
	 * @see #SUPPORTED_LIMIT_CLAUSE_UNSUPPORTED
	 * @see #SUPPORTED_LIMIT_CLAUSE_LIMIT
	 * @see #SUPPORTED_LIMIT_CLAUSE_ROWNUM
	 * @return the type of supported limit clause.
	 */
	public int getSupportedLimitClause() {
		return supportedLimitClause;
	}

	/**
	 * Get preferred concat operator or null if no concat operator supported
	 * @return name of the preferred concat operator or null
	 */
	public String getPreferredConcatOperator() {
		return preferredConcatOperator;
	}

	/**
	 * Get the datatype name to be used in a CAST(field AS type) operation to
	 * case a field to a text (if CAST is supported anyway)
	 * @return datatype name to CAST to texts, or null if not supported
	 */
	public String getTextCastName() {
		return textCastName;
	}

	/**
	 * Check whether substr() shall be used when casting
	 * @return true if substr() shall be used, false if not
	 */
	public boolean isSubstrWhenCasting() {
		return substrWhenCasting;
	}

	/**
	 * Get the name of the "contentstatus" table
	 * @return name of the "contentstatus" table
	 */
	public String getContentStatusName() {
		return contentStatusName != null ? contentStatusName : DEFAULT_CONTENTSTATUS_NAME;
	}

	/**
	 * Get the name of the "contentobject" table
	 * @return name of the "contentobject" table
	 */
	public String getContentObjectName() {
		return contentObjectName != null ? contentObjectName : DEFAULT_CONTENTOBJECT_NAME;
	}

	/**
	 * Get the name of the "contentattributetype" table
	 * @return name of the "contentattributetype" table
	 */
	public String getContentAttributeTypeName() {
		return contentAttributeTypeName != null ? contentAttributeTypeName : DEFAULT_CONTENTATTRIBUTETYPE_NAME;
	}

	/**
	 * Get the name of the "contentmap" table
	 * @return name of the "contentmap" table
	 */
	public String getContentMapName() {
		return contentMapName != null ? contentMapName : DEFAULT_CONTENTMAP_NAME;
	}

	/**
	 * Get the name of the "contentattribute" table
	 * @return name of the "contentattribute" table
	 */
	public String getContentAttributeName() {
		return contentAttributeName != null ? contentAttributeName : DEFAULT_CONTENTATTRIBUTE_NAME;
	}

	/**
	 * Get the name of the "channel" table
	 * @return name of the "channel" table
	 */
	public String getChannelName() {
		return channelName != null ? channelName : DEFAULT_CHANNEL_NAME;
	}

	/**
	 * The db-specific implementation of StringTruncator
	 * @return the stringTruncator
	 */
	public StringLengthManipulator getStringLengthManipulator() {
		return stringLengthManipulator;
	}

	/**
	 * @param stringTruncator the stringTruncator to set
	 */
	public void setStringLengthManipulator(StringLengthManipulator stringLengthManipulator) {
		this.stringLengthManipulator = stringLengthManipulator;
	}

	/**
	 * Check whether db meta data shall be cached for this handle
	 * @return true when the meta data shall be cached, false if not
	 */
	public boolean isCacheDBMetaData() {
		return cacheDBMetaData;
	}

	/**
	 * Check whether the db stores uppercase identifiers
	 * @return true for uppercase identifiers
	 */
	public boolean isUpperCaseIdentifiers() {
		return upperCaseIdentifiers;
	}

	/**
	 * Check whether the DB supports batch updates
	 * @return true for support of batch updates, false otherwise
	 */
	public boolean supportsBatchUpdates() {
		return batchUpdates;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("DBHandle ").append(name);
		return buffer.toString();
	}

	/**
	 * Get the db schema (may be null)
	 * @return db schema
	 */
	public String getDbSchema() {
		return dbSchema;
	}

	/**
	 * Set the db schema
	 * @param dbSchema db schema (may be null)
	 */
	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}

	/**
	 * Get fetch size
	 * @return fetch size
	 */
	public int getFetchSize() {
		return fetchSize;
	}

	/**
	 * Set fetch size
	 * @param fetchSize fetch size
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Get the DB specific dummy statement (which can be used to keep the connection alive). Returns null, if no suitable dummy statement found
	 * @return DB specific dummy statement or null
	 */
	public String getDummyStatement() {
		return dummyStatement;
	}

	/**
	 * Set SQLHandle instance, this DBHandle is associated with
	 * @param sqlHandle SQLHandle instance
	 */
	public void setSqlHandle(SQLHandle sqlHandle) {
		this.sqlHandle = sqlHandle;
	}

	/**
	 * Get the SQLHandle instance, this DBHandle is associated with
	 * @return SQLHandle instance
	 */
	public SQLHandle getSqlHandle() {
		return sqlHandle;
	}

	/**
	 * Get the database product name
	 * @return database product name
	 */
	public String getDatabaseProductName() {
		return databaseProductName;
	}
}
