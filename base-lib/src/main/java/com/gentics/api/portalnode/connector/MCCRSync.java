package com.gentics.api.portalnode.connector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager.ObjectTypeDiff;
import com.gentics.lib.datasource.object.ObjectManagementManager.TypeDiff;
import com.gentics.lib.db.DB;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Command line tool to synchronize two multichannelling aware content repositories.
 */
public class MCCRSync {

	/**
	 * this is the default batchsize
	 */
	private static final int SYNC_AT_ONE_TIME = 100;

	/**
	 * this is the number of objects, which are checked for obsoletion in one step
	 */
	private static final int CHECK_OBSOLETE_BATCHSIZE = 1000;

	private static final String PARAM_ALLOWALTERTABLE = "allowaltertable";

	private static final String PARAM_ALLOWEMPTY = "allowempty";

	private static final String PARAM_TEST = "test";

	private static final String PARAM_TRANSACTION = "transaction";

	private static final String PARAM_SANITYCHECK2 = "sanitycheck2";

	private static final String PARAM_AUTOREPAIR2 = "autorepair2";

	private static final String PARAM_BATCHSIZE = "batchsize";

	private static final String PARAM_DELETION_BATCHSIZE = "deletionbatchsize";

	private static final String PARAM_DATAMODIFIER = "datamodifier";

	private static final String PARAM_IGNOREOPTIMIZED = "ignoreoptimized";

	/**
	 * use this logger for test-run output, use method .info() only
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(MCCRSync.class);

	/**
	 * logger for output of progress info
	 */
	private static NodeLogger progressLogger = NodeLogger.getNodeLogger(MCCRSync.class.getName() + "Progress");

	/**
	 * source contentrepository/datasource
	 */
	private MCCRDatasource sourceDS = null;

	/**
	 * target contentrepository/datasource
	 */
	private WritableMCCRDatasource targetDS = null;

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

	private long targetUpdateTS = -1;

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
	 * Start the sync process with the given command line arguments
	 * 
	 * @param args
	 *            command line arguments
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

		MultichannellingDatasource sourceDS = null;
		WritableMultichannellingDatasource targetDS = null;
		boolean sourceTransaction = false;
		boolean targetTransaction = false;
		CRSyncModifier modifier = null;

		try {
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
				targetDS = parseDSFromArgs(line, "target");
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
				Class<?> dataModifierClass = null;

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

		try {
			MCCRSync cs = new MCCRSync(sourceDS, targetDS, line.hasOption(PARAM_TEST), line.hasOption(PARAM_ALLOWEMPTY), line.hasOption(PARAM_ALLOWALTERTABLE),
					sourceTransaction, targetTransaction, ObjectTransformer.getInt(line.getOptionValue(PARAM_BATCHSIZE), SYNC_AT_ONE_TIME), modifier);

			cs.setObsoletionCheckBatchSize(ObjectTransformer.getInt(line.getOptionValue(PARAM_DELETION_BATCHSIZE), CHECK_OBSOLETE_BATCHSIZE));
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

	/**
	 * Private helper method to print the help screen and exit
	 */
	static private void printHelpAndExit() {
		HelpFormatter formatter = new HelpFormatter();

		System.out.println("Example: MCCRSync -target target.properties \\\n" + "                  -source source.properties \\\n" + "                  -test");
		System.out.println(
				"Example: MCCRSync -source source.properties \\\n" + "                  -target_url jdbc:mariadb://localhost:3306/crsynctarget \\\n"
				+ "                  -target_driverClass com.mysql.jdbc.Driver \\\n" + "                  -target_username root \\\n"
				+ "                  -target_passwd secret \\\n" + "                  -allowAlterTable \\\n" + "                  -allowEmpty \\\n"
				+ "                  -delete");
		System.out.println("");

		formatter.printHelp("MCCRSync", createOptions(true));

		System.exit(0);
	}

	/**
	 * Private helper method to create all options
	 * 
	 * @param hideUndocumented
	 *            true to hide undocumented features (for help)
	 * @return
	 */
	@SuppressWarnings("static-access")
	static private Options createOptions(boolean hideUndocumented) {
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
		options.addOption(OptionBuilder.withArgName("password").hasOptionalArg().withDescription("source datasource password").create("target_passwd"));
		// options.addOption("target_passwd", true, "target datasource password");
		options.addOption("target_ds", true, "target datasource properties file");

		// flaags
		options.addOption(PARAM_TEST, false, "dry run and tell changes");
		options.addOption(PARAM_ALLOWALTERTABLE, false,
				"allow structural changes (quick columns). Without this flag the sync will fail when the table structure differs. Note that the source database might be locked during altering the sql table structure. Also note that data might be lost when altering the target database due to structure incompatibilities.");
		options.addOption(PARAM_ALLOWEMPTY, false,
				"allow empty source-repository. Without this flag the sync will fail when the source is empty, to prevent unintended deletions on productive environments.");
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
			options.addOption("source_" + PARAM_SANITYCHECK2, false, "sanity check 2 for source database.");
			options.addOption("target_" + PARAM_SANITYCHECK2, false, "sanity check 2 for target datasource.");
			options.addOption("source_" + PARAM_AUTOREPAIR2, false, "auto repair 2 for source database.");
			options.addOption("target_" + PARAM_AUTOREPAIR2, false, "auto repair 2 for target datasource.");
			options.addOption(PARAM_DELETION_BATCHSIZE, true,
					"maximum number of objects checked for obsoletion in a single step (default: " + CHECK_OBSOLETE_BATCHSIZE + ").");
		}

		return options;
	}

	/**
	 * Parse Datasource Information from CommandLine line evaluate -prefix before -prefix_*
	 * 
	 * @param line
	 *            Commandline to parse Datasource Information from
	 * @param dsName
	 *            Prefix of the Datasource ("e.g. target_* or source_*)
	 * @return Created Datasource
	 * @throws FileNotFoundException
	 */
	static private WritableMultichannellingDatasource parseDSFromArgs(CommandLine line, String dsName) throws FileNotFoundException {
		Map<String, String> handleProps = null;

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

			handleProps = new HashMap<String, String>();
			for (Object k : dsProp.keySet()) {
				String key = ObjectTransformer.getString(k, null);

				handleProps.put(key, dsProp.getProperty(key));
			}
		} else if (line.hasOption(dsName + "_url") && line.hasOption(dsName + "_driverClass") && line.hasOption(dsName + "_username")) {
			// check for source.url, source.driverClass, source.username,
			// source.passwd
			handleProps = new HashMap<String, String>();
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
		Map<String, String> dsProps = new HashMap<String, String>();

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

			for (Object k : datasourceProperties.keySet()) {
				String key = ObjectTransformer.getString(k, null);

				dsProps.put(key, datasourceProperties.getProperty(key));
			}
		}

		// honor test mode for target, and never change source.
		dsProps.put("autorepair", "false");
		dsProps.put("sanitycheck", "false");

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

		return PortalConnectorFactory.createWritableMultichannellingDatasource(handleProps, dsProps);
	}

	static private void logFatalMessageAndExit(String message) {
		logFatalMessageAndExit(message, null);
	}

	static private void logFatalMessageAndExit(String message, Exception exception) {
		logger.fatal(message, exception);
		System.exit(1);
	}

	/**
	 * Initialize the sync with source and target datasource. For details see commandline help (-help).
	 * 
	 * @param source
	 *            source datasource
	 * @param target
	 *            target datasource
	 * @param test
	 *            test - only simulate the changes
	 * @param allowEmpty
	 *            allow empty Source-Repository
	 * @param allowAlterTable
	 *            allow structural changes to contentrepository
	 * @param sourceTransaction
	 *            use transactions in source database
	 * @param targetTransaction
	 *            use transactions in target database
	 * @param batchSize
	 *            batch size (maximum number of objects to be synced at once)
	 * @param modifier
	 *            modify objects before storing them
	 * @throws DatasourceException
	 *             no source or target datasource given
	 */
	public MCCRSync(MultichannellingDatasource source, WritableMultichannellingDatasource target, boolean test, boolean allowEmpty,
			boolean allowAlterTable, boolean sourceTransaction, boolean targetTransaction, int batchSize, CRSyncModifier modifier) throws DatasourceException {

		if (source == null) {
			throw new DatasourceException("No Source found");
		}
		if (target == null) {
			throw new DatasourceException("No Target Datasource");
		}

		// save information
		this.sourceDS = (MCCRDatasource) source;
		this.targetDS = (WritableMCCRDatasource) target;

		this.test = test;
		this.allowEmpty = allowEmpty;
		this.allowAlterTable = allowAlterTable;

		this.sourceTransaction = sourceTransaction;
		this.targetTransaction = targetTransaction;

		this.dataModifier = modifier;
		this.batchSize = batchSize;
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
	 * Do the sync
	 * @return informational message (how many objects were sync'ed)
	 * @throws NodeException when something went wrong
	 * @throws SQLException in case of errors accessing source or target database
	 */
	public String doSync() throws NodeException, SQLException {
		logger.info("Starting CRSync.");

		// we don't want to write the updatetimestamp everytime we write an object
		targetDS.setUpdateTimestampOnWrite(false);

		// TODO count objects
		// log time meanwhile
		long startTime = System.currentTimeMillis();
		SyncCount syncCount = new SyncCount();

		try {
			if (sourceTransaction) {
				DB.startTransaction(sourceDS.getHandle());
			}
			if (targetTransaction) {
				DB.startTransaction(targetDS.getHandle());
			}

			// save last updatetimestamp
			targetUpdateTS = targetDS.getLastUpdate(true);
			logger.info("Syncing changes since " + targetUpdateTS + " (which was the last sync timestamp)");

			long sourceUpdateTS = sourceDS.getLastUpdate(true);

			if (logger.isInfoEnabled()) {
				logger.info("Last update of source cr was @ " + sourceUpdateTS);
			}

			// sync Contentrepository - (Table-)Structure
			syncCRStructure();

			// only sync datasources if update timestamp (or noderule) have not changed.
			if (sourceUpdateTS != targetUpdateTS || sourceUpdateTS < 1) {
				// sync Channel structure
				syncChannelStructure();

				// sync Contentrepository - Data
				syncCRData(syncCount);
			} else {
				logger.info("last update timestamp from source and target are identical. not syncing.");
			}

			if (targetTransaction) {
				DB.commitTransaction(targetDS.getHandle());
			}

			if (sourceTransaction) {
				DB.commitTransaction(sourceDS.getHandle());
			}

		} catch (NodeException ex) {
			if (targetTransaction) {
				DB.rollbackTransaction(targetDS.getHandle());
			}
			if (sourceTransaction) {
				DB.rollbackTransaction(sourceDS.getHandle());
			}
			throw ex;
		} catch (OutOfMemoryError e) {
			logger.error("The sync ran out of memory, please consult http://www.gentics.com/infoportal/ (CRSync) for more information on how to avoid this!");
			throw e;
		} finally {}

		long endTime = System.currentTimeMillis();
		long runTime = endTime - startTime;

		return "MCCRSync finished in " + runTime + " ms. Synced " + syncCount.modified + " added/modified object(s) and " + syncCount.deleted
				+ " deleted object(s).";
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
		Collection<ObjectTypeBean> sourceObjectTypes = ObjectManagementManager.loadObjectTypes(sourceDS, true);

		// check for empty sourceDS
		if (sourceObjectTypes.size() == 0 && !allowEmpty) {
			throw new UnexptectedEmptySourceException("Did not find any ObjectTypes in the Source-Repository.");
		}

		// load all ObjectTypes from target - collection of ObjectTypeBean
		Collection<ObjectTypeBean> targetObjectTypes = ObjectManagementManager.loadObjectTypes(targetDS, true);

		// calculate the diff
		TypeDiff diff = ObjectManagementManager.getDiff(targetObjectTypes, sourceObjectTypes);

		if (progressLogger.isInfoEnabled()) {
			progressLogger.info("Synchronizing object types");
		}

		if (!test) {
			// save all added objecttypes
			for (ObjectTypeBean newObjectType : diff.getAddedObjectTypes()) {
				// this will force the objecttype to be inserted
				newObjectType.setOldType((Integer) null);
				ObjectManagementManager.save(targetDS, newObjectType, true, allowAlterTable, ignoreOptimized);
			}

			// remove all deleted objecttypes
			for (ObjectTypeBean deletedObjectType : diff.getDeletedObjectTypes()) {
				ObjectManagementManager.delete(targetDS, deletedObjectType, allowAlterTable);
			}

			// save all modified objecttypes
			for (ObjectTypeDiff typeDiff : diff.getModifiedObjectTypes()) {
				// and save the modified objecttype (adds and saves modified attributes)
				ObjectManagementManager.save(targetDS, typeDiff.getModifiedObjectType(), true, allowAlterTable, ignoreOptimized);
			}

			// remove unused attribute types
			ObjectManagementManager.cleanUnusedAttributeTypes(targetDS);
		}
	}

	/**
	 * Synchronize the channel structure
	 * @throws NodeException
	 */
	private void syncChannelStructure() throws NodeException {
		ChannelTree channelStructure = sourceDS.getChannelStructure();

		if (progressLogger.isInfoEnabled()) {
			progressLogger.info("Synchronizing channel structure");
		}

		if (!test) {
			targetDS.saveChannelStructure(channelStructure);
		}
	}

	/**
	 * Go though all channels and sync the data
	 * @param counter counter
	 * @throws NodeException
	 */
	private void syncCRData(SyncCount counter) throws NodeException {
		ChannelTree tree = sourceDS.getChannelStructure();

		syncCRDataForChannel(tree.getChildren(), counter);
	}

	/**
	 * Recursively synchronize the data for the list of channels
	 * @param channels list of channels
	 * @param counter counter
	 * @throws NodeException
	 */
	private void syncCRDataForChannel(List<ChannelTreeNode> channels, SyncCount counter) throws NodeException {
		for (ChannelTreeNode channel : channels) {
			// set the channel in source and target datasource
			sourceDS.setChannel(channel.getChannel().getId());
			targetDS.setChannel(channel.getChannel().getId());
			// synchronize the data
			if (progressLogger.isInfoEnabled()) {
				progressLogger.info("Synchronizing objects for channel " + channel.getChannel().getName());
			}
			syncCRDataForChannel(channel.getChannel(), counter);
			// continue with the children
			syncCRDataForChannel(channel.getChildren(), counter);
		}
	}

	/**
	 * Synchronize the data for the currently set channel
	 * @param channel channel
	 * @param counter counter
	 * @throws NodeException
	 */
	private void syncCRDataForChannel(DatasourceChannel channel, SyncCount counter) throws NodeException {
		long lastChannelUpdate = targetDS.getLastChannelUpdate(channel.getId());

		Map<String, Object> data = new HashMap<String, Object>();
		Expression objectRule = null;
		DatasourceFilter objectFilter = null;
		List<MCCRObject> objects = null;

		// test for empty sourceDS
		objectRule = PortalConnectorFactory.createExpression("true");

		if (!allowEmpty && sourceDS.getCount(sourceDS.createDatasourceFilter(objectRule)) == 0) {
			throw new UnexptectedEmptySourceException("Did not find any Objects in the Source-Repository.");
		}

		/*
		 * *** this is 1)
		 * **************************************************************************
		 */
		// get all ObjectTypes from source - collection of ObjectTypeBean
		Collection<ObjectTypeBean> sourceObjectTypes = ObjectManagementManager.loadObjectTypes(sourceDS, false);

		int numObjectTypes = sourceObjectTypes.size();
		int currentObjectType = 0;

		// cycle all ContentObjectTypes (folder, page, etc.)
		for (ObjectTypeBean sourceObjectType : sourceObjectTypes) {
			currentObjectType++;

			objectRule = PortalConnectorFactory.createExpression(
					"object.obj_type == data.obj_type && object.updatetimestamp > data.updatetimestamp && object.channel_id == data.channel_id");

			// create a datasource filter from the expression
			objectFilter = sourceDS.createDatasourceFilter(objectRule);

			// set variables to the datasource filter
			data.clear();
			data.put("obj_type", sourceObjectType.getType());
			data.put("updatetimestamp", lastChannelUpdate);
			data.put("channel_id", channel.getId());

			objectFilter.addBaseResolvable("data", new MapResolver(data));

			// get the first 1000 objects (sorted by contentid) matching the
			// filter, prefill attributes, collection of Resolvable
			int startIndex = 0;
			int modifiedSubCount = 0;

			objectFilter.getMainFilterPart();
			objectFilter.getExpressionString();

			List<String> attributeNames = new ArrayList<String>(sourceObjectType.getAttributeTypesMap().keySet());

			if (logger.isDebugEnabled()) {
				logger.debug("all attribute names: " + attributeNames.toString());
			}

			// count total number of objects to sync
			int objectsToSync = 0;

			if (progressLogger.isInfoEnabled()) {
				objectsToSync = sourceDS.getCount(objectFilter);
				progressLogger.info(
						"Start syncing " + objectsToSync + " objects of type {" + sourceObjectType.getType() + "} (" + currentObjectType + "/" + numObjectTypes + ")");
			}

			try {
				// do the above
				while (true) {
					objects = sourceDS.getResult(MCCRObject.class, objectFilter, null, startIndex, batchSize,
							new Datasource.Sorting[] { new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) }, null);
					MCCRHelper.batchLoadAttributes(sourceDS, objects, attributeNames, false);

					// exit if there are no more objects left to sync
					if (objects.size() == 0) {
						if (progressLogger.isInfoEnabled()) {
							progressLogger.info("Synced all objects of type {" + sourceObjectType.getType() + "}");
						}
						break;
					}
					counter.modified += objects.size();
					modifiedSubCount += objects.size();

					// replace all attributes of type obj_type with the contentids of the objects
					removeObjectLinks(objects, sourceObjectType.getAttributeTypesList());

					if (logger.isDebugEnabled()) {
						logger.debug("Modified: " + objects.toString());
					}

					if (!test) {
						// if data modifier exists, modify all objects before
						// storing them
						if (dataModifier != null) {
							for (MCCRObject o : objects) {
								dataModifier.modify(o);
							}
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Storing the following objects: ");
							for (MCCRObject o : objects) {
								logger.debug("   " + o.get("contentid"));
							}
						}
						targetDS.store(objects);
					}

					if (progressLogger.isInfoEnabled()) {
						progressLogger.info("Synced " + modifiedSubCount + "/" + objectsToSync + " object of type {" + sourceObjectType.getType() + "}");
					}

					// set new start
					startIndex += batchSize;
				}
			} catch (Throwable e1) {
				logger.debug("Error while synchronizing data", e1);
				throw new RuntimeException("Error while synchronizing data", e1);
			} finally {}

			// /* *** this is 2)
			// **************************************************************************
			// */
			// check targetObjects in source -> try to find obsolete ones
			if (lastChannelUpdate <= 0) {
				// the lastupdate timestamp in the target cr is 0, so we needed
				// to do a full sync and will need to check all objects for
				// obsoletion
				objectRule = PortalConnectorFactory.createExpression("object.obj_type == data.obj_type && object.channel_id == data.channel_id");
			} else {
				objectRule = PortalConnectorFactory.createExpression(
						"object.obj_type == data.obj_type && object.updatetimestamp <= data.updatetimestamp && object.channel_id == data.channel_id");
			}

			// create a datasource filter from the expression
			objectFilter = targetDS.createDatasourceFilter(objectRule);

			// set variables to the datasource filter
			data.clear();
			data.put("obj_type", sourceObjectType.getType());
			data.put("updatetimestamp", new Long(lastChannelUpdate));
			data.put("channel_id", channel.getId());

			objectFilter.addBaseResolvable("data", new MapResolver(data));

			// get the first 1000 objects (sorted by contentid) matching the
			// filter, prefill attributes, collection of Resolvable
			objects = null;

			startIndex = 0;
			modifiedSubCount = 0;
			objectFilter.getMainFilterPart();
			objectFilter.getExpressionString();

			DatasourceFilter sourceObjectFilter = sourceDS.createDatasourceFilter(
					PortalConnectorFactory.createExpression(
							"object.obj_type == data.obj_type && object.channel_id == data.channel_id && object.obj_id CONTAINSONEOF data.objects.obj_id"));
			Map<String, Object> sourceData = new HashMap<String, Object>();

			sourceData.put("obj_type", sourceObjectType.getType());
			sourceData.put("channel_id", channel.getId());
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
				objects = targetDS.getResult(MCCRObject.class, objectFilter, null, startIndex, obsoletionCheckBatchSize,
						new Datasource.Sorting[] { new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) }, null);

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
				Collection<Resolvable> sourceObjects = sourceDS.getResult(sourceObjectFilter, null);

				// get the objects from the target ds which are no longer present in the source
				objects.removeAll(sourceObjects);
				counter.deleted += objects.size();
				deletedObjectsPerType += objects.size();
				if (!test) {
					// delete the obsolete objects from the target ds
					targetDS.delete(objects);
				}

				if (progressLogger.isInfoEnabled()) {
					progressLogger.info(
							"Checked " + (startIndex + checkedObjects) + "/" + objectsToCheck + " objects of type {" + sourceObjectType.getType() + "} for obsoletion");
				}

				// set new beginning
				startIndex += obsoletionCheckBatchSize;
			}

			if (progressLogger.isInfoEnabled()) {
				progressLogger.info("Finished syncing objects of type {" + sourceObjectType.getType() + "} (" + currentObjectType + "/" + numObjectTypes + ")");
			}
		}

		// we are done writing, so set the last updatetimestamp
		targetDS.setLastUpdate(channel.getId(), sourceDS.getLastChannelUpdate(channel.getId()));
	}

	/**
	 * Remove all object links (replace them with the contentids)
	 * @param objects collection of objects to modify
	 * @param attrs attributes
	 * @throws InsufficientPrivilegesException
	 */
	private void removeObjectLinks(Collection<? extends Changeable> objects, List<ObjectAttributeBean> attrs) throws InsufficientPrivilegesException {
		for (ObjectAttributeBean attr : attrs) {
			if (attr.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ) {
				for (Changeable object : objects) {
					object.setProperty(attr.getName(), transformToContentIds(object.get(attr.getName())));
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
			Collection<Object> contentids = new ArrayList<Object>();
			Collection<?> objectColl = (Collection<?>) object;

			for (Object obj : objectColl) {
				if (obj instanceof Resolvable) {
					contentids.add(((Resolvable) obj).get("contentid"));
				} else if (object != null) {
					contentids.add(ObjectTransformer.getString(object, null));
				}
			}

			return objectColl;
		} else if (object instanceof Resolvable) {
			return ((Resolvable) object).get("contentid");
		} else {
			return object;
		}
	}

	/**
	 * Counter for synchronized objects
	 */
	protected class SyncCount {

		/**
		 * Number of modified objects
		 */
		long modified = 0;

		/**
		 * Number of deleted objects
		 */
		long deleted = 0;
	}
}
