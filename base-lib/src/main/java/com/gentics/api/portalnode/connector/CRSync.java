/*
 * @author raoul
 * @date 17.03.2006
 * @version $Id: CRSync.java,v 1.19 2010-09-28 17:01:30 norbert Exp $
 */
package com.gentics.api.portalnode.connector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.content.GenticsContentObjectImpl;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager.ObjectTypeDiff;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.PoolConnection;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * CRSync synchronizes two given ContentRepositories based on two CNDatasources.<br>
 * <br>
 * CRSync tries this by only syncing the changes.<br>
 * If an intelligent sync is not possible, CRSync syncs the complete source
 * repository to the target repository completly overwriting the target
 * repository.<br>
 * More help is available in the command line help (CRSync -help).
 */
public class CRSync {

	/**
	 * this is the default batchsize
	 */
	private static final int SYNC_AT_ONE_TIME = 100;

	/**
	 * this is the number of objects, which are checked for obsoletion in one step
	 */
	private static final int CHECK_OBSOLETE_BATCHSIZE = 1000;

	// use this to disable the automagical caching of attributes - fetch them
	// every time!
	private static final int PREFETCH_ATTRIBUTE_TRESHHOLD = -1;

	private static final String PARAM_DELETE = "delete";

	private static final String PARAM_ALLOWALTERTABLE = "allowaltertable";

	private static final String PARAM_ALLOWEMPTY = "allowempty";

	private static final String PARAM_TEST = "test";

	private static final String PARAM_TRANSACTION = "transaction";

	private static final String PARAM_SANITYCHECK2 = "sanitycheck2";

	private static final String PARAM_AUTOREPAIR2 = "autorepair2";

	private static final String PARAM_BATCHSIZE = "batchsize";

	private static final String PARAM_DELETION_BATCHSIZE = "deletionbatchsize";

	private static final String PARAM_DATAMODIFIER = "datamodifier";
    
	private static final String PARAM_DISABLELOBOPTIMIZATION = "disableloboptimization";

	private static final String PARAM_IGNOREOPTIMIZED = "ignoreoptimized";
    
	private static final String PARAM_FORCE_CONTENTIDS = "forcecontentids";

	/**
	 * use this logger for test-run output, use method .info() only
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(CRSync.class);

	/**
	 * logger for output of progress info
	 */
	private static NodeLogger progressLogger = NodeLogger.getNodeLogger(CRSync.class.getName() + "Progress");

	/**
	 * source contentrepository/datasource
	 */
	private CNDatasource sourceDS = null;

	/**
	 * target contentrepository/datasource
	 */
	private CNWriteableDatasource targetDS = null;

	private String rule = "";

	// added, modified, removed ObjectTypes (e.g. 10007 -> page)
	private int addedObjectTypes = 0;

	private int modifiedObjectTypes = 0;

	private int removedObjectTypes = 0;

	// added, modified, removed AttributeTypes (e.g. name, folder_id, node_id,
	// editor)
	private int addedAttributeTypes = 0;

	private int modifiedAttributeTypes = 0;

	private int removedAttributeTypes = 0;

	/**
	 * Flag to initiate a dry-/testRun
	 */
	private boolean test = false;

	/**
	 * Flag to force (structural) changes and force continue on empty source
	 * repository
	 */
	private boolean allowAlterTable = false;

	/**
	 * allowempty, default false<br> abort on empty (no data, objects or
	 * attributetypes) source cr, continue when true.<br>
	 */
	private boolean allowEmpty = false;

	/**
	 * Flag to remove all Objects not matching a given rule e.g. rule=(node_id ==
	 * 1) -> remove all (node_id <> 1)
	 */
	private boolean delete = false;

	private long targetUpdateTS = -1;

	/**
	 * Use transactions in source database
	 */
	private boolean sourceTransaction = false;

	/**
	 * Use transactions in target database
	 */
	private boolean targetTransaction = false;

	/**
	 * this is the batchize for syncing objects
	 */
	private int batchSize = SYNC_AT_ONE_TIME;

	/**
	 * this is the batchsize for checking objects for obsoletion
	 */
	private int obsoletionCheckBatchSize = CHECK_OBSOLETE_BATCHSIZE;

	// private boolean useLobStreams = false;
	private boolean useLobStreams = true;

	/**
	 * The data modifier
	 */
	private CRSyncModifier dataModifier = null;

	/**
	 * flag to ignore optimized attributes.
	 * If set:
	 * <ul>
	 *   <li>new attributetypes will never be optimized</li>
	 *   <li>optimized flag and quickname column are ignored while comparing attributetypes</li>
	 *   <li>sync of data is done consistent for attributetypes which differ in the optimized flag</li>
	 * </ul>
	 */
	private boolean ignoreOptimized = false;

	private String[] forceContentIds;

	/**
	 * Read the commandline parameters and start the sync. For details and
	 * available parameters, see commandline help (-help).
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		// disable the portal cache (we don't need it and want no ugly exception
		// when not configured)
		System.setProperty("com.gentics.portalnode.portalcache", "false");

		// init CommandLineParser and CommandLine
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
			line = parser.parse(createOptions(false), args);
		} catch (ParseException e) {
			logger.fatal("Invalid arguments found.");
			logger.fatal(e.getMessage());
		}

		// empty line or help message
		if (line == null) {
			printHelpAndExit();
		}
		if (line.hasOption("help")) {
			printHelpAndExit();
		}

		Datasource sourceDS = null;
		WriteableDatasource targetDS = null;
		boolean sourceTransaction = false;
		boolean targetTransaction = false;
		String ruleString = null;
		CRSyncModifier modifier = null;
		boolean disableLobStreams = line.hasOption(PARAM_DISABLELOBOPTIMIZATION);
        
		try {
    
			// try to read rule String
			if (line.hasOption("rule")) {
				ruleString = line.getOptionValue("rule");
			}
    
			// try to read source properties
			logger.info("Creating connection to source content repository.");
			try {
				sourceDS = parseDSFromArgs(line, "source");
			} catch (FileNotFoundException e) {
				logger.debug("Source properties file not found: ", e);
				logFatalMessageAndExit(e.getMessage(), e);
			}
    
			// try to read target properties
			// TODO pass test and nostrucuturechange parameter to datasource for
			// crtable syntax correction.
			logger.info("Creating connection to target content repository.");
			try {
				targetDS = (CNWriteableDatasource) parseDSFromArgs(line, "target");
			} catch (FileNotFoundException e) {
				logger.debug("Target properties file not found: ", e);
				logFatalMessageAndExit(e.getMessage(), e);
			}
    
			// read transaction parameter
			String parameter = line.getOptionValue(PARAM_TRANSACTION);

			if ("source".equals(parameter)) {
				sourceTransaction = true;
			} else if ("target".equals(parameter)) {
				targetTransaction = true;
			} else if ("both".equals(parameter)) {
				sourceTransaction = true;
				targetTransaction = true;
			}
        
			// read data modifier from from command line
			String dataModifierClassName = line.getOptionValue(PARAM_DATAMODIFIER);

			if (!StringUtils.isEmpty(dataModifierClassName)) {
				Class dataModifierClass = null;

				try {
					dataModifierClass = Class.forName(dataModifierClassName);
				} catch (ClassNotFoundException cnfe) {
					logFatalMessageAndExit("ClassNotFoundException while loading modifier class.", cnfe);
				}
				if (!CRSyncModifier.class.isAssignableFrom(dataModifierClass)) {
					logFatalMessageAndExit("The data modifier class does not implement com.gentics.api.portalnode.connector.CRSyncModifier");
				}
				try {
					modifier = (CRSyncModifier) dataModifierClass.newInstance();
				} catch (IllegalAccessException iae) {
					logFatalMessageAndExit("IllegalAccessException while instatiating the data modifier class.", iae);
				} catch (InstantiationException ie) {
					logFatalMessageAndExit("InstantiationException while instatiating the data modifier class.", ie);
				}
			}
		} catch (Throwable e) {
			logger.fatal("Error while initializing cr sync.", e);
			try {
				if (sourceDS != null) {
					sourceDS.getHandlePool().close();
				}
				if (targetDS != null) {
					targetDS.getHandlePool().close();
				}
			} finally {
				System.exit(1);
			}
		}

		// only process if sourceds and targetds are set
		if (sourceDS == null) {
			logFatalMessageAndExit("No Source-Datasource given.");
		}
		if (targetDS == null) {
			logFatalMessageAndExit("No Target-Datasource given.");
		}
        
		String[] forceContentIds = null;

		if (line.hasOption(PARAM_FORCE_CONTENTIDS)) {
			try {
				BufferedReader input = new BufferedReader(new FileReader(line.getOptionValue(PARAM_FORCE_CONTENTIDS)));
				String contentidline = null;
				ArrayList contentIds = new ArrayList();

				while ((contentidline = input.readLine()) != null) {
					contentIds.add(contentidline);
				}
				forceContentIds = (String[]) contentIds.toArray(new String[contentIds.size()]);
			} catch (Exception e) {
				logFatalMessageAndExit("Error while parsing file to force objects to be synchronized {" + line.getOptionValue(PARAM_FORCE_CONTENTIDS) + "}", e);
			}
		}

		try {
			CRSync cs = new CRSync(sourceDS, targetDS, ruleString, line.hasOption(PARAM_TEST), line.hasOption(PARAM_ALLOWEMPTY),
					line.hasOption(PARAM_ALLOWALTERTABLE), line.hasOption(PARAM_DELETE), sourceTransaction, targetTransaction,
					ObjectTransformer.getInt(line.getOptionValue(PARAM_BATCHSIZE), SYNC_AT_ONE_TIME), modifier);

			cs.setForceContentIds(forceContentIds);
			cs.setObsoletionCheckBatchSize(ObjectTransformer.getInt(line.getOptionValue(PARAM_DELETION_BATCHSIZE), CHECK_OBSOLETE_BATCHSIZE));
			cs.setUseLobStreams(!disableLobStreams);
			cs.setIgnoreOptimized(line.hasOption(PARAM_IGNOREOPTIMIZED));
			String returnMessage = cs.doSync();

			logger.info(returnMessage);
		} catch (UnexptectedEmptySourceException e) {
			logger.fatal("Found empty Source-ContentRepository but -allowempty was not set.");
			logger.debug("", e);
			System.exit(1);
		} catch (UnexpectedAlterTableException e) {
			logger.fatal("CRSync wanted to change the table-structure but -allowaltertable was not set.");
			logger.debug("", e);
			System.exit(1);
		} catch (NodeException e) {
			// NodeException (unanticipated errors)
			logger.fatal("Error while syncing ContentRepositories.");
			logger.fatal("", e);
			System.exit(1);
		} catch (Exception e) {
			// IllegalArgumentException (?)
			logger.fatal("Error while syncing ContentRepositories.");
			logger.fatal("", e);
			System.exit(1);
		} catch (Throwable e) {
			logger.fatal("Error while syncing content repositories.", e);
			System.exit(1);
		} finally {
			sourceDS.getHandlePool().close();
			targetDS.getHandlePool().close();
			System.exit(0);
		}
	}

	private void setForceContentIds(String[] forceContentIds) {
		this.forceContentIds = forceContentIds;
	}

	/**
	 * Set the batchsize for the obsoletion check.
	 * @param batchSize new batchsize
	 */
	public void setObsoletionCheckBatchSize(int batchSize) {
		obsoletionCheckBatchSize = batchSize;
	}

	/**
	 * Set the flag for ignoring the optimized flag of attributetypes
	 * @param ignoreOptimized true when optimized flags shall be ignored, false if not
	 */
	public void setIgnoreOptimized(boolean ignoreOptimized) {
		this.ignoreOptimized = ignoreOptimized;
	}

	/**
	 * Parse Datasource Information from CommandLine line evaluate -prefix
	 * bevore -prefix_*
	 * @param line Commandline to parse Datasource Information from
	 * @param dsName Prefix of the Datasource ("e.g. target_* or source_*)
	 * @return Created Datasource
	 * @throws FileNotFoundException
	 */
	static private CNWriteableDatasource parseDSFromArgs(CommandLine line, String dsName) throws FileNotFoundException {
		Map handleProps = null;

		// check for "source" parameter
		if (line.hasOption(dsName)) {
			String file = line.getOptionValue(dsName);
			Properties dsProp = new Properties();

			try {
				FileInputStream st = new FileInputStream(file);

				if (st != null) {
					dsProp.load(st);
				}
			} catch (IOException e) {
				throw new FileNotFoundException("" + file);
			}

			handleProps = (Map) dsProp;
		} else if (line.hasOption(dsName + "_url") && line.hasOption(dsName + "_driverClass") && line.hasOption(dsName + "_username")) {
			// check for source.url, source.driverClass, source.username,
			// source.passwd
			handleProps = new HashMap();
			handleProps.put("url", line.getOptionValue(dsName + "_url"));
			handleProps.put("driverClass", line.getOptionValue(dsName + "_driverClass"));
			handleProps.put("username", line.getOptionValue(dsName + "_username"));
			String passwd = "";

			if (line.hasOption(dsName + "_passwd")) {
				passwd = line.getOptionValue(dsName + "_passwd");
			}
			handleProps.put("passwd", passwd);
			handleProps.put("type", "jdbc");

		} else {
			return null;
		}

		// read datasource properties from file
		Map dsProps = new HashMap();

		if (line.hasOption(dsName + "_ds")) {
			String file = line.getOptionValue(dsName + "_ds");
			Properties datasourceProperties = new Properties();

			try {
				FileInputStream st = new FileInputStream(file);

				if (st != null) {
					datasourceProperties.load(st);
				}
			} catch (IOException e) {
				throw new FileNotFoundException("" + file);
			}

			dsProps.putAll(datasourceProperties);
		}

		// honor test mode for target, and never change source.
		if ("target".equals(dsName) && line.hasOption("test")) {
			dsProps.put("autorepair", "false");
		} else if ("source".equals(dsName)) {
			dsProps.put("autorepair", "false");
			dsProps.put("sanitycheck", "false");
		}

		// set sanitycheck2
		if (line.hasOption(PARAM_SANITYCHECK2)) {
			dsProps.put("sanitycheck2", "true");
		}
		if (line.hasOption(dsName + "_" + PARAM_SANITYCHECK2)) {
			dsProps.put("sanitycheck2", "true");
		}
		if (line.hasOption(dsName + "_" + PARAM_AUTOREPAIR2)) {
			dsProps.put("autorepair2", "true");
		}

		// always use the compatibility feature to return also invalid links
		dsProps.put(CNDatasource.ILLEGALLINKSNOTNULL, "true");

		CNWriteableDatasource ds = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(handleProps, dsProps);

		return ds;
	}

	/**
	 * Private helper method to print the help screen and exit
	 */
	static private void printHelpAndExit() {
		HelpFormatter formatter = new HelpFormatter();

		System.out.println("Example: CRSync -target target.properties \\\n" + "                -source source.properties \\\n" + "                -test");
		System.out.println(
				"Example: CRSync -source source.properties \\\n" + "                -target_url jdbc:mariadb://localhost:3306/crsynctarget \\\n"
				+ "                -target_driverClass com.mysql.jdbc.Driver \\\n" + "                -target_username root \\\n"
				+ "                -target_passwd secret \\\n" + "                -allowAlterTable \\\n" + "                -allowEmpty \\\n"
				+ "                -delete");
		System.out.println("");

		formatter.printHelp("CRSync", createOptions(true));

		System.exit(0);
	}

	static private void logFatalMessageAndExit(String message) {
		logFatalMessageAndExit(message, null);
	}

	static private void logFatalMessageAndExit(String message, Exception exception) {
		logger.fatal(message, exception);
		System.exit(1);
	}

	/**
	 * Private helper method to create all options
	 * @param hideUndocumented true to hide undocumented features (for help)
	 * @return
	 */
	static private Options createOptions(boolean hideUndocumented) {
		// options.addOption(OptionBuilder.withArgName( "property=value" )
		// .hasArg()
		// .withValueSeparator()
		// .withDescription( "use value for given property" )
		// .create( "D" ));

		Options options = new Options();

		options.addOption("source", true, "source properties file OR use source_* arguments instead.");
		options.addOption("source_url", true, "source datasource url");
		options.addOption("source_driverClass", true, "source datasource driverClass");
		options.addOption("source_username", true, "source datasource username");
		options.addOption(OptionBuilder.withArgName("password").hasOptionalArg().withDescription("source datasource password").create("source_passwd"));
		// options.addOption("source_passwd", true, "source datasource password");
		options.addOption("source_ds", true, "source datasource properties file");

		options.addOption("target", true, "target properties file OR use target_* arguments");
		options.addOption("target_url", true, "target datasource url");
		options.addOption("target_driverClass", true, "target datasource driverClass");
		options.addOption("target_username", true, "target datasource username");
		options.addOption(OptionBuilder.withArgName("password").hasOptionalArg().withDescription("target datasource password").create("target_passwd"));
		// options.addOption("target_passwd", true, "target datasource password");
		options.addOption("target_ds", true, "target datasource properties file");

		options.addOption("rule", true,
				"rule to use for sync. Important note for usage with the delete flag: comparisons on columns with NULL values is not supported, when the delete flag is set. This is because negations on NULL values are not supported in most databases. So take care that all attributes used in the rule have values in the whole Content Repository, and none of them is NULL.");

		// flaags
		options.addOption(PARAM_TEST, false, "dry run and tell changes");
		options.addOption(PARAM_ALLOWALTERTABLE, false,
				"allow structural changes (quick columns). Without this flag the sync will fail when the table structure differs. Note that the source database might be locked during altering the sql table structure. Also note that data might be lost when altering the target database due to structure incompatibilities.");
		options.addOption(PARAM_ALLOWEMPTY, false,
				"allow empty source-repository. Without this flag the sync will fail when the source is empty, to prevent unintended deletions on productive environments.");
		options.addOption(PARAM_DELETE, false,
				"when using a rule, remove all other data from target which do not match the given rule. Note that deleted source objects will always (with our without this flag) be removed from target when no rule is given, or the rule matches the objects.");
		options.addOption(PARAM_TRANSACTION, true, "enable transaction for datasource, possible values: none (default), source, target, both.");

		options.addOption("help", false, "help");

		options.addOption(PARAM_BATCHSIZE, true,
				"maximum number of objects sync'ed or deleted in a single step (default: " + SYNC_AT_ONE_TIME
				+ "). reduce this number if the generated SQL statement become too large for the database. A higher value will speedup crsync but needs more memory. You will need at least enough memory to store your batchsize count of objects in memory. (You can exclude Text Long and Binary Content attributes sizes from your object size)");

		options.addOption(PARAM_DATAMODIFIER, true,
				"specify a class that implements com.gentics.api.portalnode.connector.CRSyncModifier to modify objects before syncing");

		options.addOption(PARAM_SANITYCHECK2, false,
				"enable extended sanity check for source and target repository. When an incompatibility is found in either the source or the target, the sync will fail.");

		options.addOption(PARAM_IGNOREOPTIMIZED, false,
				"ignore optimized flag for attributetypes. This allows different quick columns in source and target content repositories.");

		// undocumented features below
		if (!hideUndocumented) {
			options.addOption(PARAM_DISABLELOBOPTIMIZATION, false, "uses a stream for large objects instead of reading it into memory.");
			options.addOption("source_" + PARAM_SANITYCHECK2, false, "sanity check 2 for source database.");
			options.addOption("target_" + PARAM_SANITYCHECK2, false, "sanity check 2 for target datasource.");
			options.addOption("source_" + PARAM_AUTOREPAIR2, false, "auto repair 2 for source database.");
			options.addOption("target_" + PARAM_AUTOREPAIR2, false, "auto repair 2 for target datasource.");
			options.addOption(PARAM_DELETION_BATCHSIZE, true,
					"maximum number of objects checked for obsoletion in a single step (default: " + CHECK_OBSOLETE_BATCHSIZE + ").");
            
			options.addOption(PARAM_FORCE_CONTENTIDS, true, "expects a filename containing contentids (one per line) of objects which are forced to be synced.");
		}

		return options;
	}

	/**
	 * Initialize the sync with source and target datasource, leave flags to
	 * default values (false). For details see commandline help (-help).
	 * @param source source datasource
	 * @param target target datasource
	 * @param rule optional rule to restrict sync'ed objects
	 * @throws NodeException when the sync cannot be initialized
	 */
	public CRSync(Datasource source, WriteableDatasource target, String rule) throws NodeException {
		this(source, target, rule, false, false, false, false, SYNC_AT_ONE_TIME);
	}

	/**
	 * Initialize the sync with source and target datasource, leave flags to
	 * default values (false). For details see commandline help (-help).
	 * @param source source datasource
	 * @param target target datasource
	 * @param rule optional rule to restrict sync'ed objects
	 * @param test test - only simulate the changes
	 * @param allowEmpty allow empty Source-Repository
	 * @param allowAlterTable allow structural changes to contentrepository
	 * @param delete remove all objects not matching the given rule in targetDS
	 * @param batchSize batch size
	 * @throws DatasourceException when the sync cannot be initialized
	 */
	public CRSync(Datasource source, WriteableDatasource target, String rule, boolean test,
			boolean allowEmpty, boolean allowAlterTable, boolean delete, int batchSize) throws DatasourceException {
		this(source, target, rule, test, allowEmpty, allowAlterTable, delete, false, false, batchSize, null);
	}

	/**
	 * Initialize the sync with source and target datasource. For details see
	 * commandline help (-help).
	 * @param source source datasource
	 * @param target target datasource
	 * @param rule optional rule to restrict sync'ed objects
	 * @param test test - only simulate the changes
	 * @param allowEmpty allow empty Source-Repository
	 * @param allowAlterTable allow structural changes to contentrepository
	 * @param delete remove all objects not matching the given rule in targetDS
	 * @param sourceTransaction use transactions in source database
	 * @param targetTransaction use transactions in target database
	 * @param batchSize batch size (maximum number of objects to be synced at once)
	 * @param modifier modify objects before storing them
	 * @throws DatasourceException no source or target datasource given
	 */
	public CRSync(Datasource source, WriteableDatasource target, String rule, boolean test,
			boolean allowEmpty, boolean allowAlterTable, boolean delete,
			boolean sourceTransaction, boolean targetTransaction, int batchSize,
			CRSyncModifier modifier) throws DatasourceException {

		if (source == null) {
			throw new DatasourceException("No Source found");
		}
		if (target == null) {
			throw new DatasourceException("No Target Datasource");
		}

		// empty rule
		if ("".equals(rule)) {
			rule = null;
		}

		// save information
		this.sourceDS = (CNDatasource) source;
		this.targetDS = (CNWriteableDatasource) target;
		// we don't want to repair the id counter on every insert, but will do
		// this later with a single statement
		this.targetDS.setRepairIDCounterOnInsert(false);
		this.rule = rule;

		this.test = test;
		this.allowEmpty = allowEmpty;
		this.allowAlterTable = allowAlterTable;
		this.delete = delete;

		this.sourceTransaction = sourceTransaction;
		this.targetTransaction = targetTransaction;

		this.dataModifier = modifier;
		this.batchSize = batchSize;
	}

	/**
	 * Search for a given ObjectTypeBean in a Collection of ObjectTypeBeans used
	 * to search sourceObjectTypeBean to a matching targetObjectTypeBean in all
	 * targetObjectTypeBeans
	 * @param haystack Collection of ObjectTypeBeans
	 * @param needle ObjectTypeBean to search for
	 * @return found ObjectType been out of the given collection.
	 */
	private ObjectTypeBean findObjectTypeBeanInCollection(Collection haystack,
			ObjectTypeBean needle) {

		for (Iterator it = haystack.iterator(); it.hasNext();) {
			ObjectTypeBean otb = (ObjectTypeBean) it.next();

			if (needle.equals(otb)) {
				return otb;
			}
		}

		return null;
	}
    
	public void setUseLobStreams(boolean useLobStreams) {
		this.useLobStreams = useLobStreams;
	}

	/**
	 * private helper method to sync contentrepository (table-)structure
	 * @throws NodeException
	 */
	private void syncCRStructure() throws NodeException {
		// check whether optimized flags will be ignored
		if (ignoreOptimized) {
			logger.info(PARAM_IGNOREOPTIMIZED + " flag set: 'optimized' flag will be ignored for all contentattributetypes");
		}

		// get all ObjectTypes from source - collection of ObjectTypeBean
		Collection sourceObjectTypesArray = loadObjectTypesFromDatasource(sourceDS, allowEmpty);

		// check for emtpy sourceDS
		if (sourceObjectTypesArray.size() == 0 && !allowEmpty) {
			throw new UnexptectedEmptySourceException("Did not find any ObjectTypes in the Source-Repository.");
		}

		// load all ObjectTypes from target - collection of ObjectTypeBean
		Collection targetObjectTypesArray = loadObjectTypesFromDatasource(targetDS, true);

		// Collection of ObjectTypes which have changed Attributes
		Collection changedObjectTypesArray = new ArrayList();

		for (Iterator it = sourceObjectTypesArray.iterator(); it.hasNext();) {

			// load ObjectType from source
			ObjectTypeBean sourceObjectType = (ObjectTypeBean) it.next();

			// find the (source)ObjectType in the targetDS AND
			// get the diff between two objecttypes (modified/added/deleted
			// attributetypes)
			ObjectTypeBean targetObjectType = findObjectTypeBeanInCollection(targetObjectTypesArray, sourceObjectType);

			// no target Object found -> add ContentObject
			if (targetObjectType == null) {
				// TODO gti shouldn't this be done by
				// ObjectManagementManager.saveObjectType(getDBHandleFromDatasource(target),
				// sourceObjectType, true); - NOP
				ObjectManagementManager.createNewObject(GenticsContentFactory.getHandle(targetDS), sourceObjectType.getType().toString(), sourceObjectType.getName());

				logger.info("New Object: " + sourceObjectType.getName());
				addedObjectTypes += 1;

				// re-load targetObjectType Collection and
				// try to find targetObjectType
				targetObjectType = findObjectTypeBeanInCollection(loadObjectTypesFromDatasource(targetDS, true), sourceObjectType);
			}

			ObjectTypeDiff diff = null;

			if (targetObjectType != null) {
				diff = ObjectManagementManager.getTypeDiff(targetObjectType, sourceObjectType, ignoreOptimized);
			}

			// TODO gti create boolean diff.hasChangedAttributes() - nop
			// and replace this check
			if (diff != null) {
				if (targetObjectType == null || diff.getAddedAttributeTypes().size() != 0 || diff.getDeletedAttributeTypes().size() != 0
						|| diff.getModifiedAttributeTypes().size() != 0) {

					logger.info("Modified Object: " + sourceObjectType.getName());
					modifiedObjectTypes += 1;

					logger.info("Added AttributeTypes: " + diff.getAddedAttributeTypes().toString());
					addedAttributeTypes += diff.getAddedAttributeTypes().size();

					logger.info("Modified AttributeTypes: " + diff.getModifiedAttributeTypes().toString());
					modifiedAttributeTypes += diff.getModifiedAttributeTypes().size();

					logger.info("Removed AttributeTypes: " + diff.getDeletedAttributeTypes().toString());
					removedAttributeTypes += diff.getDeletedAttributeTypes().size();

					// if there are changed attributes add them to
					// changedObjectTypesArray

					changedObjectTypesArray.add(sourceObjectType);
					// save the objecttype (to the target ds)

					if (!test) {
						try {
							ObjectManagementManager.saveObjectType(targetDS, sourceObjectType, true, allowAlterTable, ignoreOptimized);
							// save does not delete the deletedAttributeTypes
							// do this manually as in AdministrationPortlet
							// (told so by nop)
							ObjectAttributeBean[] deletedAttributeTypes = (ObjectAttributeBean[]) diff.getDeletedAttributeTypes().toArray(
									new ObjectAttributeBean[diff.getDeletedAttributeTypes().size()]);

							ObjectManagementManager.deleteAttributeTypes(GenticsContentFactory.getHandle(targetDS), deletedAttributeTypes, allowAlterTable);
						} catch (ObjectManagementException e) {
							throw new UnexpectedAlterTableException(
									"The Structure of " + sourceObjectType.getName() + "' is about to change. " + "Use ARGUMENT to force Alter Table.", e);
						}
					}
				}
			}

			targetObjectTypesArray.remove(sourceObjectType);
		}

		// delete "deleted" attributes
		if (targetObjectTypesArray.size() > 0) {

			// ObjectTypeBean[] arr = new ObjectTypeBean[1];
			// targetObjectTypesArray.toArray(arr);

			ObjectManagementManager.deleteObjectTypes(GenticsContentFactory.getHandle(targetDS),
					(ObjectTypeBean[]) targetObjectTypesArray.toArray(new ObjectTypeBean[targetObjectTypesArray.size()]), true);

			removedObjectTypes = targetObjectTypesArray.size();
			logger.info("Removed Object Count: " + removedObjectTypes);

		}
	}

	/**
	 * Do the sync
	 * @return informational message (how many objects were sync'ed)
	 * @throws NodeException when something went wrong
	 * @throws SQLException for errors accessing source or target db
	 */
	public String doSync() throws NodeException, SQLException {

		logger.info("Starting CRSync.");
		// TODO count objects
		// log time meanwhile
		long startTime = System.currentTimeMillis();
		Long[] syncCount = new Long[] { new Long(0), new Long(0)};

		// save treshholds
		int sourcePfAT = sourceDS.getPrefetchAttributesThreshold();
		int targetPfAT = targetDS.getPrefetchAttributesThreshold();

		sourceDS.setPrefetchAttributesThreshold(PREFETCH_ATTRIBUTE_TRESHHOLD);
		targetDS.setPrefetchAttributesThreshold(PREFETCH_ATTRIBUTE_TRESHHOLD);

		try {

			if (sourceTransaction) {
				DB.startTransaction(sourceDS.getHandle().getDBHandle());
			}
			if (targetTransaction) {
				DB.startTransaction(targetDS.getHandle().getDBHandle());
			}

			// save last updatetimestamp
			// remember last rule in contentstatus "lastcrsyncrule",
			// match current rule, do full sync (ts 0) when they differ.
			targetUpdateTS = 0;
			String lastcrsyncrule = targetDS.getStringContentStatus("lastcrsyncrule");

			if (lastcrsyncrule != null) {
				lastcrsyncrule = lastcrsyncrule.trim();
			}

			if (rule != null) {
				rule = rule.trim();
			}

			if (logger.isInfoEnabled()) {
				if (StringUtils.isEmpty(rule)) {
					logger.info("Syncing without rule");
				} else {
					logger.info("Current sync rule: " + rule);
				}

				if (StringUtils.isEmpty(lastcrsyncrule)) {
					logger.info("Last sync was without rule");
				} else {
					logger.info("Last sync rule: " + lastcrsyncrule);
				}
			}
			// overwrite targetUpdateTS if rules match
			if (StringUtils.isEqual(rule, lastcrsyncrule)) {
				targetUpdateTS = targetDS.getLastUpdate();
				logger.info("Sync rules are unchanged, syncing changes since " + targetUpdateTS + " (which was the last sync timestamp)");
			} else {
				logger.info("Sync rule changed, ignoring last sync timestamp and doing full sync");
			}

			long sourceUpdateTS = sourceDS.getLastUpdate();

			if (logger.isInfoEnabled()) {
				logger.info("Last update of source cr was @ " + sourceUpdateTS);
			}

			// if (targetUpdateTS == -1) {
			// throw new NodeException("Got LastUpdate " + targetUpdateTS);
			// }

			// sync Contentrepository - (Table-)Structure
			syncCRStructure();

			// only sync datasources if update timestamp (or noderule) have not changed.
			if (sourceUpdateTS != targetUpdateTS || sourceUpdateTS < 1) {
				// sync Contentrepository - Data
				syncCount = syncCRData();

				// repair id_counters in target db
				if (!test) {
					targetDS.repairIdCounters(null);
				}

				// "touch" contentstatus
				if (!test) {
					targetDS.setLastUpdate(sourceUpdateTS);
					logger.info("Last update of target ds was set to " + sourceUpdateTS);
				}
				// remember last rule in contentstatus "lastcrsyncrule",
				if (!test) {
					targetDS.setContentStatus("lastcrsyncrule", rule);
					logger.info("Last crsync rule is now " + (StringUtils.isEmpty(rule) ? "[empty]" : rule));
				}
			} else {
				logger.info("last update timestamp from source and target are identical. not syncing.");
			}

			if (targetTransaction) {
				DB.commitTransaction(targetDS.getHandle().getDBHandle());
			}

			if (sourceTransaction) {
				DB.commitTransaction(sourceDS.getHandle().getDBHandle());
			}

		} catch (NodeException ex) {
			if (targetTransaction) {
				DB.rollbackTransaction(targetDS.getHandle().getDBHandle());
			}
			if (sourceTransaction) {
				DB.rollbackTransaction(sourceDS.getHandle().getDBHandle());
			}
			throw ex;
		} catch (OutOfMemoryError e) {
			logger.fatal("The sync ran out of memory, please consult http://www.gentics.com/infoportal/ (CRSync) for more information on how to avoid this!");
			throw e;
		} finally {
			// restore PrefetchAttributesThreshold
			sourceDS.setPrefetchAttributesThreshold(sourcePfAT);
			targetDS.setPrefetchAttributesThreshold(targetPfAT);

		}

		long endTime = System.currentTimeMillis();
		long runTime = endTime - startTime;

		return "CRSync finished in " + runTime + " ms. Synced " + syncCount[0] + " added/modified object(s) and " + syncCount[1] + " deleted object(s).";
	}

	/**
	 * private helper method to sync all data
	 * @throws NodeException
	 */
	private Long[] syncCRData() throws NodeException {
		// 0) if removeNonMatchingObjects && rule
		// remove all objects not matching the rule
		// 1) source -> target sync
		// ###### cycle all modified objects by objecttype
		// foreach objecttype objecttypelist
		// get objectlist where object.obj_type = curenttype && update >=
		// lastupdate limit 1000
		// foreach object from objectlist
		// target.save(obj)
		// 2) reverse: target -> source (remove targetObjects which do not exist
		// in source)

		Map data = new HashMap();
		Expression objectRule = null;
		DatasourceFilter objectFilter = null;
		Collection objects = null;
		long modifiedCount = 0;
		long deletedCount = 0;

		// test for empty sourceDS
		objectRule = PortalConnectorFactory.createExpression("true");

		if (!allowEmpty && sourceDS.getCount(sourceDS.createDatasourceFilter(objectRule)) == 0) {
			throw new UnexptectedEmptySourceException("Did not find any Objects in the Source-Repository.");
		}

		// prepare rule string
		String ruleString;

		if (StringUtils.isEmpty(rule)) {
			ruleString = "true";
		} else {
			ruleString = rule;
		}

		/*
		 * *** this is 0)
		 * **************************************************************************
		 */
		// TODO gti cannot match "node_id == NULL" values
		if (delete && !"true".equals(ruleString)) {
			logger.info("-delete flag and -rule was given. Removing objects from target repository, that do not match the rule");

			if (!test) {
				objectRule = PortalConnectorFactory.createExpression("!(" + ruleString + ")");

				// create a datasource filter from the expression
				objectFilter = targetDS.createDatasourceFilter(objectRule);
				DatasourceInfo deleteInfo = targetDS.delete(objectFilter);

				logger.info("Removed " + deleteInfo.getAffectedRecordCount() + " objects from target repository.");
			} else {
				logger.info("Test mode: Not removing objects from target repositoy");
			}
		} else {
			if (!delete && !"true".equals(ruleString)) {
				logger.info("-delete flag not given: target repository might contain objects after the sync that do not match the given rule");
			}
		}

		/*
		 * *** this is 1)
		 * **************************************************************************
		 */
		// get all ObjectTypes from source - collection of ObjectTypeBean
		Collection sourceObjectTypesArray = loadObjectTypesFromDatasource(sourceDS, allowEmpty);

		// get all ObjectTypes from target - collection of ObjectTypeBean
		Collection targetObjectTypesArray = loadObjectTypesFromDatasource(targetDS, allowEmpty);

		int numObjectTypes = sourceObjectTypesArray.size();
		int currentObjectType = 0;

		// cycle all ContentObjectTypes (folder, page, etc.)
		for (Iterator it = sourceObjectTypesArray.iterator(); it.hasNext();) {
			// load ObjectType from source matching the given rule
			ObjectTypeBean sourceObjectType = (ObjectTypeBean) it.next();

			// get the target object type
			ObjectTypeBean targetObjectType = getObjectTypeBean(targetObjectTypesArray, sourceObjectType.getType().intValue());

			currentObjectType++;

			if (forceContentIds == null) {
				objectRule = PortalConnectorFactory.createExpression(
						"object.obj_type == data.obj_type && object.updatetimestamp > data.updatetimestamp && (" + ruleString + ")");
			} else {
				objectRule = PortalConnectorFactory.createExpression(
						"object.obj_type == data.obj_type && ((object.updatetimestamp > data.updatetimestamp && (" + ruleString
						+ ")) || object.contentid CONTAINSONEOF data.forceContentIds)");
			}
			// .createExpression("object.obj_type == data.obj_type &&
			// object.updatetimestamp > data.updatetimestamp");

			// create a datasource filter from the expression
			objectFilter = sourceDS.createDatasourceFilter(objectRule);

			// set variables to the datasource filter
			data.clear();
			data.put("obj_type", sourceObjectType.getType());
			data.put("updatetimestamp", new Long(targetUpdateTS));
			if (forceContentIds != null) {
				data.put("forceContentIds", forceContentIds);
			}

			objectFilter.addBaseResolvable("data", new MapResolver(data));

			// get the first 1000 objects (sorted by contentid) matching the
			// filter, prefill attributes, collection of Resolvable
			int counter = 0;
			int modifiedSubCount = 0;

			objectFilter.getMainFilterPart();
			objectFilter.getExpressionString();

			String[] attributeNames;
			ObjectAttributeBean[] lobAttributes = null;

			// array of attributetypes, which are lob attributes and only
			// optimized in the target (when the ignoreoptimized flag is set)
			ObjectAttributeBean[] onlyTargetOptimizedLobAttributes = null;

			final Map lobAttributeMap = new HashMap();

			if (useLobStreams) {
				logger.info("Using LOB memory optimization ....");
				attributeNames = objectAttributeBeanArrayToStringArray(sourceObjectType.getNonLobAttributeTypes(), true);
				lobAttributes = sourceObjectType.getLobAttributeTypes();

				if (ignoreOptimized) {
					// get all lob attributes in the target
					List onlyTarget = new Vector(Arrays.asList(targetObjectType.getLobAttributeTypes()));

					for (Iterator iterator = onlyTarget.iterator(); iterator.hasNext();) {
						ObjectAttributeBean aType = (ObjectAttributeBean) iterator.next();

						// remove attribute types which are not optimized (in the target)
						if (!aType.getOptimized()) {
							iterator.remove();
						} else {
							// get the attribute type in the source
							ObjectAttributeBean sourceAType = getAttributeTypeBean(sourceObjectType, aType.getName());

							if (sourceAType == null) {
								// remove if not existent in source
								iterator.remove();
							} else if (sourceAType.getOptimized()) {
								// remove if optimized in the source
								iterator.remove();
							}
						}
					}
					onlyTargetOptimizedLobAttributes = (ObjectAttributeBean[]) onlyTarget.toArray(new ObjectAttributeBean[onlyTarget.size()]);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("attribute names: " + Arrays.asList(attributeNames).toString());
				}
				// put the lob attributes into a map for easy lookup
				for (int i = 0; i < lobAttributes.length; i++) {
					lobAttributeMap.put(lobAttributes[i].getName(), lobAttributes[i]);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("lob attribute names: " + lobAttributeMap.keySet());
				}
			} else {
				attributeNames = objectAttributeBeanArrayToStringArray(sourceObjectType.getAttributeTypes(), true);
				if (logger.isDebugEnabled()) {
					logger.debug("all attribute names: " + Arrays.asList(attributeNames).toString());
				}
			}

			// count total number of objects to sync
			int objectsToSync = 0;

			if (progressLogger.isInfoEnabled()) {
				objectsToSync = sourceDS.getCount(objectFilter);
				progressLogger.info(
						"Start syncing " + objectsToSync + " objects of type {" + sourceObjectType.getType() + "} (" + currentObjectType + "/" + numObjectTypes + ")");
			}
            
			// open our connections
			boolean targettransaction = false;
			boolean sourcetransaction = false;
			PoolConnection targetpoolconn = null;
			DBHandle targetdb = targetDS.getHandle().getDBHandle();
			PoolConnection sourcepoolconn = null;
			DBHandle sourcedb = sourceDS.getHandle().getDBHandle();

			try {
				Connection targetconn = null;
				Connection sourceconn = null;

				if (useLobStreams) {
					// First to the target database
					targetpoolconn = DB.getOpenConnection(targetdb);
					if (targetpoolconn != null) {
						targettransaction = true;
					} else {
						targetpoolconn = DB.getPoolConnection(targetdb);
					}
					targetconn = targetpoolconn.getConnection();
       
					sourcepoolconn = DB.getOpenConnection(sourcedb);
					// to the source database
					if (sourcepoolconn != null) {
						sourcetransaction = true;
					} else {
						sourcepoolconn = DB.getPoolConnection(sourcedb);
					}
					sourceconn = sourcepoolconn.getConnection();
				}

				// do the above
				while (true) {
					objects = sourceDS.getResultForcePrefill(objectFilter, attributeNames, counter, batchSize,
							new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)});
    
					// exit if there are no more objects left to sync
					if (objects.size() == 0) {
						if (progressLogger.isInfoEnabled()) {
							progressLogger.info("Synced all objects of type {" + sourceObjectType.getType() + "}");
						}
						break;
					}
					modifiedCount += objects.size();
					modifiedSubCount += objects.size();
    
					// replace all attributes of type obj_type with the contentids of the objects
					removeObjectLinks(objects, sourceObjectType.getAttributeTypes());
    
					if (logger.isDebugEnabled()) {
						logger.debug("Modified: " + objects.toString());
					}
    
					// before storing the objects, we set their custom
					// updatetimestamp (this ensures that the sync'ed objects have
					// the same updatetimestamp as the source objects)
					for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
						GenticsContentObject syncedObject = (GenticsContentObject) iterator.next();

						syncedObject.setCustomUpdatetimestamp(syncedObject.getUpdateTimestamp());
					}
    
					if (!test) {
						// if data modifier exists, modify all objects before
						// storing them
						if (this.dataModifier != null) {
							for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
								Changeable changeable = (Changeable) iterator.next();

								this.dataModifier.modify(changeable);
							}
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Storing the following objects: ");
							for (Iterator i = objects.iterator(); i.hasNext();) {
								GenticsContentObjectImpl co = (GenticsContentObjectImpl) i.next();

								logger.debug("   " + co.getContentId() + "  modified attrs {" + Arrays.asList(co.getModifiedAttributeNames()) + "}");
							}
						}
						targetDS.store(objects);
						if (useLobStreams && lobAttributes.length > 0) {
							// now store the large objects ...
                            
                            
							// first get all contentids of the updated objects
							String[] contentIds = new String[objects.size()];
							int j = 0;

							for (Iterator i = objects.iterator(); i.hasNext();) {
								GenticsContentObject syncedObject = (GenticsContentObject) i.next();

								contentIds[j++] = syncedObject.getContentId();
							}
                            
							// if there are optimized lob objects ..
							boolean hasoptimized = false;
							StringBuffer cols = new StringBuffer("SELECT id,contentid");
							StringBuffer targetCols = new StringBuffer("SELECT id,contentid"); 

							for (int i = 0; i < lobAttributes.length; i++) {
								ObjectAttributeBean lobAttribute = lobAttributes[i];

								if (lobAttribute.getOptimized()) {
									if (ignoreOptimized) {
										// ignore optimized flag is set, so the target attribute type may not be optimized
										ObjectAttributeBean targetAttributeType = getAttributeTypeBean(targetObjectType, lobAttribute.getName());

										if (targetAttributeType.getOptimized()) {
											// only add this attribute type if also optimized in the target
											cols.append(',').append(lobAttribute.getQuickname());
											targetCols.append(',').append(targetAttributeType.getQuickname());
											hasoptimized = true;
										}
									} else {
										cols.append(',').append(lobAttribute.getQuickname());
										targetCols.append(',').append(lobAttribute.getQuickname());
										hasoptimized = true;
									}
								}
							}
							// we have optimized attributes.. sync them
							if (hasoptimized) {
								StringBuffer optimizedwhere = new StringBuffer(" WHERE contentid IN (");

								optimizedwhere.append(StringUtils.repeat("?", contentIds.length, ","));
								optimizedwhere.append(") ORDER BY contentid ");

								String sqlstmtSource = cols.append(" FROM ").append(sourcedb.getContentMapName()).append(optimizedwhere).toString();
								String sqlstmtTarget = targetCols.append(" FROM ").append(targetdb.getContentMapName()).append(optimizedwhere).toString();
								PreparedStatement osourcestmt = sourceconn.prepareStatement(sqlstmtSource);
								PreparedStatement otargetstmt;

								try {
									otargetstmt = targetconn.prepareStatement(sqlstmtTarget, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
								} catch (SQLException e) {
									logger.warn("Unable to prepare statement - maybe we are using mssql drivers ? try TYPE_SCROLL_SENSITIVE", e);
									otargetstmt = targetconn.prepareStatement(sqlstmtTarget, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
								}

								for (int i = 0; i < contentIds.length; i++) {
									osourcestmt.setString(i + 1, contentIds[i]);
									otargetstmt.setString(i + 1, contentIds[i]);
								}

								ResultSet osourcers = osourcestmt.executeQuery();
								ResultSet otargetrs = otargetstmt.executeQuery();
                                
								while (osourcers.next()) {
									if (!otargetrs.next()) {
										throw new RuntimeException("Object removed from target datasource, or added in source datasource during sync !");
									}
									String sourcecontentid = osourcers.getString("contentid");
									String targetcontentid = otargetrs.getString("contentid");
                                    
									if (!sourcecontentid.equals(targetcontentid)) {
										throw new RuntimeException("Object removed from target datasource, or added in source datasource during sync !");
									}
                                    
									// iterate over all optimized attributes and copy their values
									for (int i = 0; i < lobAttributes.length; i++) {
										ObjectAttributeBean lobAttribute = lobAttributes[i];

										if (lobAttribute.getOptimized()) {
											String colname = lobAttribute.getQuickname();
											String targetColName = colname;

											if (ignoreOptimized) {
												ObjectAttributeBean targetLobAttribute = getAttributeTypeBean(targetObjectType, lobAttribute.getName());

												if (!targetLobAttribute.getOptimized()) {
													continue;
												}
												targetColName = targetLobAttribute.getQuickname();
											}
                                            
											switch (lobAttribute.getAttributetype()) {
											case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
												// Clob clob = osourcers.getClob(colname);
												// //                                                    updaters.updateClob("value_clob", clob);
												// otargetrs.updateAsciiStream(colname, clob.getAsciiStream(), (int) clob.length());
												// we are not using streams here ..
												// first get the value from the source

												Object value = osourcers.getObject(colname);

												if (value instanceof String) {
													String stringValue = (String) value;

													// update it as string
													otargetrs.updateCharacterStream(targetColName, new StringReader(stringValue), stringValue.length());
													// otargetrs.updateString(targetColName, (String)value);
												} else if (value != null) {
													// update it as object (whatever that will be)
													otargetrs.updateObject(targetColName, value);
												} else {
													// update it as null
													otargetrs.updateNull(targetColName);
												}
												break;

											case GenticsContentAttribute.ATTR_TYPE_BLOB: {
												// otargetrs.updateObject(colname, osourcers.getObject(colname));
												// Blob blob = osourcers.getBlob(colname);
												// otargetrs.updateBlob(colname, blob);
												Blob blob = osourcers.getBlob(colname);

												otargetrs.updateBinaryStream(targetColName, blob.getBinaryStream(), (int) blob.length());
												break;
											}

											case GenticsContentAttribute.ATTR_TYPE_BINARY:
												otargetrs.updateObject(targetColName, osourcers.getObject(colname));
												// Blob blob = osourcers.getBlob(colname);
												// otargetrs.updateBlob(colname, blob);
												break;

											default:
												throw new RuntimeException(
														"Error while synchronizing LOBs - unknown attribute type: " + lobAttribute.getAttributetype());
											}
										}
                                        
									}
									otargetrs.updateRow();
								}
                                
								DB.close(osourcers);
								DB.close(otargetrs);
								DB.close(osourcestmt);
								DB.close(otargetstmt);
							}
                            
							// build sql query to find all lob attributes ...
							String select = "SELECT id, contentid, name, sortorder, value_blob, value_clob, value_bin FROM ";
							String selecttarget = "SELECT id, contentid, name, sortorder FROM ";
							StringBuffer sql = new StringBuffer(" WHERE contentid IN (");

							sql.append(StringUtils.repeat("?", contentIds.length, ","));
							sql.append(") AND name IN (");
							sql.append(StringUtils.repeat("?", lobAttributes.length, ","));
							sql.append(") ORDER BY contentid, name, sortorder");
                            
							PreparedStatement sourcestmt = sourceconn.prepareStatement(
									new StringBuffer(select).append(sourcedb.getContentAttributeName()).append(sql).toString());
							PreparedStatement targetstmt;
							PreparedStatement insertstmt;
							String targetsqlstmt = new StringBuffer(selecttarget).append(targetdb.getContentAttributeName()).append(sql).toString();
							// String insertsqlstmt = "SELECT id, contentid, name, sortorder, value_blob, value_clob, value_bin FROM "+targetdb.getContentAttributeName()+" WHERE 1=2";
							boolean useForwardOnlyResultSet = true;

							try {
								targetstmt = targetconn.prepareStatement(targetsqlstmt, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
								// insertstmt = targetconn.prepareStatement(insertsqlstmt, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
							} catch (SQLException e) {
								logger.warn("Unable to prepare statements - maybe we are using mssql drivers ? try with TYPE_SCROLL_SENSITIVE", e);
								targetstmt = targetconn.prepareStatement(targetsqlstmt, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
								// insertstmt = targetconn.prepareStatement(insertsqlstmt, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
							}
							// we are using statements here because the
							// resultsets would store updated values forever.
							// (leading to out of memory errors - maybe
							// TYPE_FORWARD_ONLY should solve this problem - at
							// least for updates ?)
							PreparedStatement targetlobinsert = targetconn.prepareStatement(
									"INSERT INTO " + targetdb.getContentAttributeName()
									+ " (contentid, name, value_clob, value_blob, value_bin, sortorder) VALUES (?,?,?,?,?,?)");
							PreparedStatement targetlobupdate = targetconn.prepareStatement(
									"UPDATE " + targetdb.getContentAttributeName() + " SET value_clob = ?, value_blob = ?, value_bin = ?, sortorder = ? WHERE id = ?");
							// ResultSet insertrs = insertstmt.executeQuery();
                            
							List l = new ArrayList();

							l.addAll(Arrays.asList(contentIds));
                            
							int paramc = 1;

							for (int i = 0; i < contentIds.length; i++) {
								sourcestmt.setString(paramc, contentIds[i]);
								targetstmt.setString(paramc, contentIds[i]);
								paramc++;
							}
							for (int i = 0; i < lobAttributes.length; i++) {
								sourcestmt.setString(paramc, lobAttributes[i].getName());
								targetstmt.setString(paramc, lobAttributes[i].getName());
								l.add(lobAttributes[i].getName());
								paramc++;
							}

							if (logger.isDebugEnabled()) {
								logger.debug("Retrieving values with  : " + DB.debugSql(sql.toString(), l.toArray()));
							}
                            
							try {
								sourcestmt.setFetchSize(Integer.MIN_VALUE);
								targetstmt.setFetchSize(Integer.MIN_VALUE);
							} catch (SQLException e) {
								// we are probably using any sane database .. so set fetch size to 1
								sourcestmt.setFetchSize(1);
								targetstmt.setFetchSize(1);
							}
                            
							ResultSet sourcers = sourcestmt.executeQuery();
							ResultSet targetrs = targetstmt.executeQuery();
                            
							if (logger.isDebugEnabled() && !useForwardOnlyResultSet) {
								logger.debug("ALL TARGETS:");
								while (targetrs.next()) {
									logger.debug("  -- contentid:{" + targetrs.getString("contentid") + "} name:{" + targetrs.getString("name") + "}");
								}
								targetrs.beforeFirst();
							}
                            
							String targetcontentid = null;
							String targetname = null;
							// flag to mark whether we need to proceed in the target
							boolean proceedInTarget = true;

							while (sourcers.next()) {
								String sourcecontentid = sourcers.getString("contentid");
								String sourcename = sourcers.getString("name");

								logger.debug("in loop: sourcers.next: {" + sourcecontentid + "}:{" + sourcename + "}");
                                
								ObjectAttributeBean lobAttribute = (ObjectAttributeBean) lobAttributeMap.get(sourcename);

								if (proceedInTarget) {
									if (targetrs.next()) {
										targetcontentid = targetrs.getString("contentid");
										targetname = targetrs.getString("name");
    
										logger.debug("in loop: targetrs.next: {" + targetcontentid + "}:{" + targetname + "}");
										if (!(sourcecontentid.equals(targetcontentid) && sourcename.equals(targetname))) {
											// maybe the target database contains additional attributes ...
											int t;

											while ((t = sourcecontentid.compareTo(targetcontentid)) > 0 || (t == 0 && sourcename.compareTo(targetname) > 0)) {
												if (logger.isDebugEnabled()) {
													logger.debug("in loop: deleting attribute {" + targetname + "} from contentid {" + targetcontentid + "}");
												}
												deleteOneRow(targetrs);
												if (!targetrs.next()) {
													break;
												}
												targetcontentid = targetrs.getString("contentid");
												targetname = targetrs.getString("name");
												if (logger.isDebugEnabled()) {
													logger.debug("in loop: targetrs.next2: {" + targetcontentid + "}:{" + targetname + "}");
												}
											}
											// we did not find the given attribute in the target datasource..
										}
									} else {
										targetcontentid = null;
										targetname = null;
									}
								}
								boolean needsinsert = false;

								if (targetcontentid != null && sourcecontentid != null && sourcecontentid.equals(targetcontentid)
										&& sourcename.equals(targetname)) {
									// we found a matching record in the target
									// recordset -> the current target record is
									// consumed and we need to proceed to the
									// next record in the next iteration step
									proceedInTarget = true;
								} else {
									// we did not find the record in the target
									// recordset -> the record will be inserted
									// and we will NOT proceed to the next
									// record, since the previously read target
									// record is not yet consumed
									proceedInTarget = false;
									needsinsert = true;
								}
                                
								if (logger.isDebugEnabled()) {
									logger.debug("synchronizing LOB {" + lobAttribute.getName() + "} for {" + sourcecontentid + "} insert:" + needsinsert);
								}
								String type;
								PreparedStatement stmt = targetlobupdate;
								int lobcol;

								if (needsinsert) {
									stmt = targetlobinsert;
									stmt.setString(1, sourcecontentid);
									stmt.setString(2, sourcename);
									stmt.setNull(3, Types.LONGVARCHAR); // value_clob
									stmt.setNull(4, Types.LONGVARBINARY); // value_blob
									stmt.setNull(5, Types.LONGVARBINARY); // value_bin
									stmt.setObject(6, sourcers.getObject("sortorder")); // sortorder
									lobcol = 3;
								} else {
									stmt.setNull(1, Types.LONGVARCHAR); // value_clob
									stmt.setNull(2, Types.LONGVARBINARY); // value_blob
									stmt.setNull(3, Types.LONGVARBINARY); // value_bin
									stmt.setObject(4, sourcers.getObject("sortorder")); // sortorder
									stmt.setInt(5, targetrs.getInt("id")); // id
									lobcol = 1;
								}
								switch (lobAttribute.getAttributetype()) {
								case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
									// SORRY for the chaos in here !!
									// a small explaination:
									// - setCharacterStream(..) -> uses setString internally - just uses more useless memory !
									// - setAsciiStream(..) -> requires the number of _bytes_ as third parameter ... i have no idea how we should now that one ..
									// - setString(..) -> requires memory > twice as large as the size of the object.
									// FIXME clean me up once all databases work ..
                                        
									type = "value_clob";
									// updaters.updateString("value_clob", sourcers.getString("value_clob"));
									// clobs can't be updated in place.. so we a statement to update these values later..
                                        
									// see: http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
									// The Clob implementation does not allow in-place modification (they are copies, as reported by the DatabaseMetaData.locatorsUpdateCopies()  method). Because of this, you should use the PreparedStatement.setClob() method to save changes back to the database. The JDBC API does not have a ResultSet.updateClob() method.
									// Clob clob = sourcers.getClob(type);
									String value = sourcers.getString(type);

									// //                                        updaters.updateClob("value_clob", clob);
									// System. out.println("clob.length: " + clob.length());
									stmt.setString(lobcol, value);
									// Clob clob = sourcers.getClob(type);
									// stmt.setCharacterStream(lobcol, clob.getCharacterStream(), (int) clob.length());
									// stmt.setAsciiStream(lobcol, clob.getAsciiStream(), (int) clob.length());
                                        

									// updaters.updateAsciiStream(type, clob.getAsciiStream(), (int) clob.length());
									// updaters.updateObject("value_clob", updaters.getObject("value_clob"));
									break;

								case GenticsContentAttribute.ATTR_TYPE_BLOB: {
									type = "value_blob";
									// stmt.setObject(lobcol+1, sourcers.getObject(type));
									Blob blob = sourcers.getBlob(type);

									if (blob == null) {
										stmt.setNull(lobcol + 1, Types.BLOB);
									} else {
										stmt.setBinaryStream(lobcol + 1, blob.getBinaryStream(), (int) blob.length());
									}
									// Blob blob = sourcers.getBlob(type);
									// stmt.setBlob(lobcol+1, blob);
									break;
								}

								case GenticsContentAttribute.ATTR_TYPE_BINARY:
									type = "value_bin";
									stmt.setObject(lobcol + 2, sourcers.getObject(type));
									// Blob blob = sourcers.getBlob(type);
									// stmt.setBlob(lobcol+2, blob);
									break;

								default:
									throw new RuntimeException("Error while synchronizing LOBs - unknown attribute type: " + lobAttribute.getAttributetype());
								}
                                
								stmt.executeUpdate();
                                
								// updaters.updateObject("sortorder", sourcers.getObject("sortorder"));
								//
								if (needsinsert) {
									// updaters.insertRow();
									if (logger.isDebugEnabled()) {
										logger.debug("inserted row with contentid {" + sourcecontentid + "} and name {" + sourcename + "} col {" + type + "}");
									}
									// updaters.moveToCurrentRow();
									// } else {
									// updaters.updateRow();
								}
							}
							if (logger.isDebugEnabled() && !useForwardOnlyResultSet) {
								boolean isLast = targetrs.isLast();
                                
								logger.debug("after sync - islast?:{" + isLast + "}");
							}
							while (targetrs.next()) {
								String name = targetrs.getString("name");
								String contentid = targetrs.getString("contentid");

								if (logger.isDebugEnabled()) {
									logger.debug("deleting attribute {" + name + "} from contentid {" + contentid + "}");
								}
								deleteOneRow(targetrs);
							}
                            
							// DB.close(insertrs);
							DB.close(targetrs);
							DB.close(sourcers);
							// DB.close(insertstmt);
							DB.close(targetstmt);
							DB.close(sourcestmt);
							DB.close(targetlobinsert);
							DB.close(targetlobupdate);
						}

						// when using lob attributes and ignoreoptimized
						// flag is set, we need to check for optimized lob
						// attributes in the target, which are not optimized in
						// the source
						if (useLobStreams && ignoreOptimized && !ObjectTransformer.isEmpty(onlyTargetOptimizedLobAttributes)) {
							// prepare the where clause
							String whereClause = "WHERE contentid in (" + StringUtils.repeat("?", objects.size(), ",") + ")";

							// resolve the contentids from all modified objects
							Map dataMap = new HashMap();

							dataMap.put("data", objects);
							Collection contentIds = ObjectTransformer.getCollection(PropertyResolver.resolve(new MapResolver(dataMap), "data.contentid"),
									Collections.EMPTY_LIST);

							// prepare the query params, first parameter will be the attribute name, second will be 1 and then all contentids
							Object[] queryParams = new Object[contentIds.size() + 2];
							int paramCounter = 1;

							queryParams[paramCounter++] = new Integer(1);

							// fill in all contentids
							for (Iterator iterator = contentIds.iterator(); iterator.hasNext();) {
								queryParams[paramCounter++] = iterator.next();
							}

							// update values for the optimized column
							for (int i = 0; i < onlyTargetOptimizedLobAttributes.length; i++) {
								String typeColumn = DatatypeHelper.getTypeColumn(onlyTargetOptimizedLobAttributes[i].getAttributetype());
								String quickColumn = onlyTargetOptimizedLobAttributes[i].getQuickname();
								String attributeName = onlyTargetOptimizedLobAttributes[i].getName();

								// modify the query params for the attribute
								queryParams[0] = attributeName;

								// execute the statement which will copy all
								// values for the modified object from table
								// contentattribute to the quick column in
								// contentmap
								DB.update(targetdb,
										"UPDATE " + targetdb.getContentMapName() + " SET " + quickColumn + " = (SELECT " + typeColumn + " FROM "
										+ targetdb.getContentAttributeName() + " a WHERE a.contentid = " + targetdb.getContentMapName()
										+ ".contentid AND a.name = ? AND (a.sortorder = ? OR a.sortorder IS NULL)) " + whereClause,
										queryParams);
							}
						}
					}

					if (progressLogger.isInfoEnabled()) {
						progressLogger.info("Synced " + modifiedSubCount + "/" + objectsToSync + " object of type {" + sourceObjectType.getType() + "}");
					}
    
					// set new start
					counter += batchSize;
				}
                
				// FIXME close connections
			} catch (Throwable e1) {
				logger.debug("Error while synchronizing data", e1);
				throw new RuntimeException("Error while synchronizing data", e1);
			} finally {
				try {
					if (!targettransaction) {
						DB.safeRelease(targetdb, targetpoolconn);
					}
					if (!sourcetransaction) {
						DB.safeRelease(sourcedb, sourcepoolconn);
					}
				} catch (SQLException e) {
					throw new RuntimeException("Error while releasing connections", e);
				}
			}

			// /* *** this is 2)
			// **************************************************************************
			// */
			// check targetObjects in source -> try to find obsolete ones
			if (targetUpdateTS <= 0) {
				// the lastupdate timestamp in the target cr is 0, so we needed
				// to do a full sync and will need to check all objects for
				// obsoletion
				objectRule = PortalConnectorFactory.createExpression("object.obj_type == data.obj_type && (" + ruleString + ")");
			} else {
				objectRule = PortalConnectorFactory.createExpression(
						"object.obj_type == data.obj_type && object.updatetimestamp <= data.updatetimestamp && (" + ruleString + ")");
			}

			// create a datasource filter from the expression
			objectFilter = targetDS.createDatasourceFilter(objectRule);

			// set variables to the datasource filter
			data.clear();
			data.put("obj_type", sourceObjectType.getType());
			data.put("updatetimestamp", new Long(targetUpdateTS));

			objectFilter.addBaseResolvable("data", new MapResolver(data));

			// get the first 1000 objects (sorted by contentid) matching the
			// filter, prefill attributes, collection of Resolvable
			objects = null;

			counter = 0;
			modifiedSubCount = 0;
			objectFilter.getMainFilterPart();
			objectFilter.getExpressionString();

			DatasourceFilter sourceObjectFilter = sourceDS.createDatasourceFilter(
					PortalConnectorFactory.createExpression(
							"object.obj_type == data.obj_type && object.obj_id CONTAINSONEOF data.objects.obj_id && (" + ruleString + ")"));
			Map sourceData = new HashMap();

			sourceData.put("obj_type", sourceObjectType.getType());
			sourceObjectFilter.addBaseResolvable("data", new MapResolver(sourceData));

			int objectsToCheck = 0;

			if (progressLogger.isInfoEnabled()) {
				objectsToCheck = targetDS.getCount(objectFilter);
				progressLogger.info(
						"Start removing obsolete objects of type {" + sourceObjectType.getType() + "} (need to check " + objectsToCheck + " objects)");
			}

			// we count the number of deleted objects here
			int deletedObjectsPerType = 0;

			while (true) {
				objects = targetDS.getResult(objectFilter, null, counter, obsoletionCheckBatchSize,
						new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)});

				// exit if there are no more objects left to sync
				if (objects.size() == 0) {
					if (progressLogger.isInfoEnabled()) {
						progressLogger.info("Removed " + deletedObjectsPerType + " obsolete objects of type {" + sourceObjectType.getType() + "}");
					}
					break;
				}
				int checkedObjects = objects.size();

				// get all objects from the source ds with the id's from the target objects
				sourceData.put("objects", objects);
				Collection sourceObjects = sourceDS.getResult(sourceObjectFilter, null);

				// get the objects from the target ds which are no longer present in the source
				objects.removeAll(sourceObjects);
				deletedCount += objects.size();
				deletedObjectsPerType += objects.size();
				if (!test) {
					// delete the obsolete objects from the target ds
					targetDS.delete(objects);
				}

				if (progressLogger.isInfoEnabled()) {
					progressLogger.info(
							"Checked " + (counter + checkedObjects) + "/" + objectsToCheck + " objects of type {" + sourceObjectType.getType() + "} for obsoletion");
				}

				// set new beginning
				counter += obsoletionCheckBatchSize;
			}

			if (progressLogger.isInfoEnabled()) {
				progressLogger.info("Finished syncing objects of type {" + sourceObjectType.getType() + "} (" + currentObjectType + "/" + numObjectTypes + ")");
			}
		}

		return new Long[] { new Long(modifiedCount), new Long(deletedCount)};
	}

	/**
	 * a helper method which deletes one row from the resultset and assures that the 'pointer' is BEFORE the next row.
	 */
	private int deleteOneRow(ResultSet targetrs) throws SQLException {
		int rowBefore = targetrs.getRow();

		targetrs.deleteRow();

		// mysql workaround.
		try {
			// this is the case for msyql and mssql
			if (rowBefore == targetrs.getRow()) {
				// mssql throws an exception now
				targetrs.getString("name");
				// mysql needs a 'previous' so we can continue
				targetrs.previous();
				logger.debug("We are syncing into mysql database - going to previous row after delete...");
			}
		} catch (SQLException e) {// ignore mssql exception.
		}
		return rowBefore;
	}

	/**
	 * Get contentids from the collection of objects
	 * @param objects collection of objects
	 * @return collection of contentids
	 */
	private Collection getContentIds(Collection objects) {
		Map objectsMap = new HashMap();

		objectsMap.put("objects", objects);
		try {
			return (Collection) PropertyResolver.resolve(new MapResolver(objectsMap), "objects.contentid");
		} catch (UnknownPropertyException e) {
			return Collections.EMPTY_LIST;
		}
	}

	/**
	 * Convert an Array[] of ObjectAttributeBeans to a String[] Array containing
	 * all element names
	 * @param arr ObjectAttributeBean[] Array
	 * @param omitForeignLinkAttributes TODO
	 * @return ObjectAttributeBeans converted into a String[] Array
	 */
	private String[] objectAttributeBeanArrayToStringArray(ObjectAttributeBean[] arr,
			boolean omitForeignLinkAttributes) {
		String[] values = new String[arr.length];

		int valueCounter = 0;

		for (int i = 0; i < arr.length; i++) {
			ObjectAttributeBean oab = arr[i];

			if (!omitForeignLinkAttributes || oab.getAttributetype() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				values[valueCounter++] = oab.getName();
			}
		}

		// when less attributes stored in the array, we reduce the array to its
		// actual size
		if (valueCounter < arr.length) {
			String[] lessValues = new String[valueCounter];

			System.arraycopy(values, 0, lessValues, 0, valueCounter);
			values = lessValues;
		}

		// finally, we search for attribute names containing a "." and escape it with "\"
		for (int i = 0; i < values.length; i++) {
			if (values[i].indexOf('.') >= 0) {
				values[i] = values[i].replaceAll("\\.", "\\\\\\.");
			}
		}

		return values;
	}

	/**
	 * Load all ObjectTypes from a given Datasource
	 * @param ds Datasource to load ObjectTypes from
	 * @param allowEmpty force continue on empty repository?
	 * @return Collection of ObjectTypeBeans
	 * @throws NodeException
	 */
	private Collection<ObjectTypeBean> loadObjectTypesFromDatasource(Datasource ds, boolean allowEmpty) throws NodeException {
		DBHandle dbHandle = GenticsContentFactory.getHandle(ds);

		Collection<ObjectTypeBean> objectTypes = ObjectManagementManager.loadObjectTypes(dbHandle);

		// load all attributeTypes
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbHandle);

		if (attributeTypes.size() == 0 && !allowEmpty) {
			throw new UnexptectedEmptySourceException("Did not find any AttributeTypes in the source-repository.");
		}

		// load the attributetypes into the objecttypes
		ObjectManagementManager.setReferences(objectTypes, attributeTypes);

		return objectTypes;
	}

	/**
	 * Remove all object links (replace them with the contentids)
	 * @param objects collection of objects to modify
	 * @param attrs attributes
	 * @throws InsufficientPrivilegesException
	 */
	private void removeObjectLinks(Collection objects, ObjectAttributeBean[] attrs) throws InsufficientPrivilegesException {
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ) {
				for (Iterator iter = objects.iterator(); iter.hasNext();) {
					Changeable object = (Changeable) iter.next();

					object.setProperty(attrs[i].getName(), transformToContentIds(object.get(attrs[i].getName())));
				}
			}
		}
	}

	/**
	 * Transform the given object/collection of objects into contentids
	 * @param object object or collection of objects
	 * @return contentid or collection of contentids
	 */
	private Object transformToContentIds(Object object) {
		if (object instanceof Collection) {
			Collection contentids = new Vector();
			Collection objectColl = (Collection) object;

			for (Iterator iter = objectColl.iterator(); iter.hasNext();) {
				Resolvable obj = (Resolvable) iter.next();

				contentids.add(obj.get("contentid"));
			}

			return objectColl;
		} else if (object instanceof Resolvable) {
			return ((Resolvable) object).get("contentid");
		} else {
			return object;
		}
	}

	/**
	 * Get the objecttype bean of given type
	 * @param objectTypeBeans collection of object type beans
	 * @param type given type
	 * @return bean of the given type or null if not found
	 */
	private ObjectTypeBean getObjectTypeBean(Collection objectTypeBeans, int type) {
		for (Iterator iterator = objectTypeBeans.iterator(); iterator.hasNext();) {
			ObjectTypeBean objectTypeBean = (ObjectTypeBean) iterator.next();

			if (objectTypeBean.getType().intValue() == type) {
				return objectTypeBean;
			}
		}

		return null;
	}

	/**
	 * Get the attribute type with given name from the object type
	 * @param objectType given object type
	 * @param name name of the attribute type
	 * @return attribute type or null if not found
	 */
	private ObjectAttributeBean getAttributeTypeBean(ObjectTypeBean objectType, String name) {
		ObjectAttributeBean[] attributeTypes = objectType.getAttributeTypes();

		for (int i = 0; i < attributeTypes.length; i++) {
			if (attributeTypes[i].getName().equals(name)) {
				return attributeTypes[i];
			}
		}

		return null;
	}

	/**
	 * Get the array of object which are present in both arrays, the instances are taken from the first array
	 * @param array1 first array
	 * @param array2 second array
	 * @return intersection
	 */
	private Object[] getIntersection(Object[] array1, Object[] array2) {
		List list1 = new Vector(Arrays.asList(array1));
		List list2 = Arrays.asList(array2);

		list1.retainAll(list2);

		return (Object[]) list1.toArray(new Object[list1.size()]);
	}

	/**
	 * Get the difference of the given arrays (all objects contained in the first array, but not in the second)
	 * @param array1 first array
	 * @param array2 second array
	 * @return difference of first array and second array
	 */
	private Object[] getDifference(Object[] array1, Object[] array2) {
		List list1 = new Vector(Arrays.asList(array1));
		List list2 = Arrays.asList(array2);

		list1.removeAll(list2);

		return (Object[]) list1.toArray(new Object[list1.size()]);
	}

	/**
	 * Add all objects from the second array to the first array (if not already there)
	 * @param array1 first array
	 * @param array2 second array
	 * @return union of first array and second array
	 */
	private Object[] getUnion(Object[] array1, Object[] array2) {
		List list1 = new Vector(Arrays.asList(array1));
		List list2 = Arrays.asList(array2);

		for (Iterator iterator = list2.iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();

			if (!list1.contains(o)) {
				list1.add(o);
			}
		}

		return (Object[]) list1.toArray(new Object[list1.size()]);
	}
}
