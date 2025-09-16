package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.MCCRSync;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Tests for writing data into contentrepositories
 */
public class AbstractMCCRDatasourceWriteTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(AbstractMCCRDatasourceWriteTest.class);

	/**
	 * Assertion appender
	 */
	private static AssertionAppender appender;
	
	/**
	 * Currently used datasource instance
	 */
	private WritableMCCRDatasource ds;

	/**
	 * Target Datasource for sync tests
	 */
	private WritableMCCRDatasource targetDS;

	/**
	 * Currently tested attribute
	 */
	protected String attribute;
	
	/**
	 * Map of expected data for master channel
	 */
	public final static Map<String, Object> EXPECTEDDATA_MASTER = new HashMap<String, Object>();
	
	/**
	 * Map of expected data for channel with ID 2
	 */
	public final static Map<String, Object> EXPECTEDDATA_CHANNEL_1 = new HashMap<String, Object>();

	/**
	 * Map of expected data for channel with ID 3
	 */
	public final static Map<String, Object> EXPECTEDDATA_CHANNEL_2 = new HashMap<String, Object>();
	
	/**
	 * Map of expected modified data
	 */
	public final static Map<String, Object> EXPECTEDMODDATA = new HashMap<String, Object>();
	
	/**
	 * Map of excluded test cases (keys are the testdb identifiers, values are the attribute names)
	 */
	public final static Map<String, List<String>> EXCLUDEDTESTS = new HashMap<String, List<String>>();

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * List that will store expected data for all channels. The sequence number
	 * in the list corresponds to the ID of the channel.
	 */
	public final static List<Map<String, Object>> DATA = new ArrayList<Map<String, Object>>();

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FS_BASEPATH = "/tmp/MCCRDatasourceWriteTest_" + TestEnvironment.getRandomHash(8);

	/**
	 * Target directory for filesystem attributes
	 */
	public static String targetDirectory = "target_";

	/**
	 * Flag that indicates if different content IDs should be used in the current test iteration.
	 */
	private final boolean useDifferentContentId;

	/**
	 * Flag that indicates if different attribute values should be used in the current test iteration.
	 */
	private final boolean useDifferentAttrValue;

	/**
	 * The ID of the channel that should be ignored in the current test iteration. If 0 - no channel will be ignored.
	 */
	private final int ignoreChannelId;

	/**
	 * The ID of the channel in which we should update object's attribute value.
	 */
	private final int channelIdToUpdate;
	
	protected static final boolean[] FALSE_TRUE = new boolean[] { false, true};
	
	static {
		// prepare expected test data
		try {
			// Expected test data for the master
			EXPECTEDDATA_MASTER.put("text", StringUtils.repeat("The quick brown fox-", 15));
			EXPECTEDDATA_MASTER.put("link", "1000.1012");
			EXPECTEDDATA_MASTER.put("int", 42);
			EXPECTEDDATA_MASTER.put("longtext", "The quick brown fox jumps over the lazy dog");
			EXPECTEDDATA_MASTER.put("blob", "The quick brown fox".getBytes("UTF-8"));
			EXPECTEDDATA_MASTER.put("long", 16777216L);
			EXPECTEDDATA_MASTER.put("double", 3.1415926);
			EXPECTEDDATA_MASTER.put("date", getTimestamp(1972, 9, 25, 10, 35, 0));

			EXPECTEDDATA_MASTER.put("text_opt", StringUtils.repeat("The quick brown fox", 15));
			EXPECTEDDATA_MASTER.put("link_opt", "1000.1012");
			EXPECTEDDATA_MASTER.put("int_opt", 42);
			EXPECTEDDATA_MASTER.put("long_opt", 16777216L);
			EXPECTEDDATA_MASTER.put("double_opt", 3.1415926);
			EXPECTEDDATA_MASTER.put("date_opt", getTimestamp(1972, 9, 25, 10, 35, 0));

			EXPECTEDDATA_MASTER.put("text_multi",
					Arrays.asList(StringUtils.repeat("The quick brown fox - 1", 15), StringUtils.repeat("The quick brown fox - 2", 15)));
			EXPECTEDDATA_MASTER.put("link_multi", Arrays.asList("1000.1012", "1000.1013"));
			EXPECTEDDATA_MASTER.put("int_multi", Arrays.asList(421, 422));
			EXPECTEDDATA_MASTER.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1", "The quick brown fox jumps over the lazy dog - 2"));
			EXPECTEDDATA_MASTER.put("blob_multi", Arrays.asList("The quick brown fox - 1".getBytes("UTf-8"), "The quick brown fox - 2".getBytes("UTf-8")));
			EXPECTEDDATA_MASTER.put("long_multi", Arrays.asList(167772161L, 167772162L));
			EXPECTEDDATA_MASTER.put("double_multi", Arrays.asList(3.14159261, 3.14159262));
			EXPECTEDDATA_MASTER.put("date_multi", Arrays.asList(getTimestamp(1972, 9, 25, 10, 35, 1), getTimestamp(1972, 9, 25, 10, 35, 2)));

			EXPECTEDDATA_MASTER.put("longtext_fs", EXPECTEDDATA_MASTER.get("longtext"));
			EXPECTEDDATA_MASTER.put("blob_fs", EXPECTEDDATA_MASTER.get("blob"));

			EXPECTEDDATA_MASTER.put("longtext_fs_multi", EXPECTEDDATA_MASTER.get("longtext_multi"));
			EXPECTEDDATA_MASTER.put("blob_fs_multi", EXPECTEDDATA_MASTER.get("blob_multi"));

			DATA.add(EXPECTEDDATA_MASTER);
			
			// Expected test data for the first sub-channel: master -> channel
			EXPECTEDDATA_CHANNEL_1.put("text", "The quick brown fox - 2");
			EXPECTEDDATA_CHANNEL_1.put("link", "1000.1013");
			EXPECTEDDATA_CHANNEL_1.put("int", 43);
			EXPECTEDDATA_CHANNEL_1.put("longtext", "The quick brown fox jumps over the lazy dog - 2");
			EXPECTEDDATA_CHANNEL_1.put("blob", "The quick brown fox - 2".getBytes("UTF-8"));
			EXPECTEDDATA_CHANNEL_1.put("long", 16777217L);
			EXPECTEDDATA_CHANNEL_1.put("double", 3.1415927);
			EXPECTEDDATA_CHANNEL_1.put("date", getTimestamp(1972, 9, 25, 10, 35, 1));

			EXPECTEDDATA_CHANNEL_1.put("text_opt", "The quick brown fox - 2");
			EXPECTEDDATA_CHANNEL_1.put("link_opt", "1000.1013");
			EXPECTEDDATA_CHANNEL_1.put("int_opt", 43);
			EXPECTEDDATA_CHANNEL_1.put("long_opt", 16777217L);
			EXPECTEDDATA_CHANNEL_1.put("double_opt", 3.1415927);
			EXPECTEDDATA_CHANNEL_1.put("date_opt", getTimestamp(1972, 9, 25, 10, 35, 1));

			EXPECTEDDATA_CHANNEL_1.put("text_multi", Arrays.asList("The quick brown fox - 1.2", "The quick brown fox - 2.2"));
			EXPECTEDDATA_CHANNEL_1.put("link_multi", Arrays.asList("1000.1012", "1000.1014"));
			EXPECTEDDATA_CHANNEL_1.put("int_multi", Arrays.asList(521, 522));
			EXPECTEDDATA_CHANNEL_1.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1.2", "The quick brown fox jumps over the lazy dog - 2.2"));
			EXPECTEDDATA_CHANNEL_1.put("blob_multi", Arrays.asList("The quick brown fox - 1.2".getBytes("UTf-8"), "The quick brown fox - 2.2".getBytes("UTf-8")));
			EXPECTEDDATA_CHANNEL_1.put("long_multi", Arrays.asList(167772162L, 167772163L));
			EXPECTEDDATA_CHANNEL_1.put("double_multi", Arrays.asList(3.14159262, 3.14159263));
			EXPECTEDDATA_CHANNEL_1.put("date_multi", Arrays.asList(getTimestamp(1972, 9, 25, 10, 35, 2), getTimestamp(1972, 9, 25, 10, 35, 3)));

			EXPECTEDDATA_CHANNEL_1.put("longtext_fs", EXPECTEDDATA_CHANNEL_1.get("longtext"));
			EXPECTEDDATA_CHANNEL_1.put("blob_fs", EXPECTEDDATA_CHANNEL_1.get("blob"));

			EXPECTEDDATA_CHANNEL_1.put("longtext_fs_multi", EXPECTEDDATA_CHANNEL_1.get("longtext_multi"));
			EXPECTEDDATA_CHANNEL_1.put("blob_fs_multi", EXPECTEDDATA_CHANNEL_1.get("blob_multi"));

			DATA.add(EXPECTEDDATA_CHANNEL_1);
			
			// Expected test data for the second sub-channel: master -> channel -> channel
			EXPECTEDDATA_CHANNEL_2.put("text", "The quick brown fox - 3");
			EXPECTEDDATA_CHANNEL_2.put("link", "1000.1014");
			EXPECTEDDATA_CHANNEL_2.put("int", 44);
			EXPECTEDDATA_CHANNEL_2.put("longtext", "The quick brown fox jumps over the lazy dog - 3");
			EXPECTEDDATA_CHANNEL_2.put("blob", "The quick brown fox - 3".getBytes("UTF-8"));
			EXPECTEDDATA_CHANNEL_2.put("long", 16777218L);
			EXPECTEDDATA_CHANNEL_2.put("double", 3.1415928);
			EXPECTEDDATA_CHANNEL_2.put("date", getTimestamp(1972, 9, 25, 10, 35, 2));
			
			EXPECTEDDATA_CHANNEL_2.put("text_opt", "The quick brown fox - 3");
			EXPECTEDDATA_CHANNEL_2.put("link_opt", "1000.1014");
			EXPECTEDDATA_CHANNEL_2.put("int_opt", 44);
			EXPECTEDDATA_CHANNEL_2.put("long_opt", 16777218L);
			EXPECTEDDATA_CHANNEL_2.put("double_opt", 3.1415928);
			EXPECTEDDATA_CHANNEL_2.put("date_opt", getTimestamp(1972, 9, 25, 10, 35, 2));

			EXPECTEDDATA_CHANNEL_2.put("text_multi", Arrays.asList("The quick brown fox - 1.3", "The quick brown fox - 2.3"));
			EXPECTEDDATA_CHANNEL_2.put("link_multi", Arrays.asList("1000.1012", "1000.1005"));
			EXPECTEDDATA_CHANNEL_2.put("int_multi", Arrays.asList(423, 424));
			EXPECTEDDATA_CHANNEL_2.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1.3", "The quick brown fox jumps over the lazy dog - 2.3"));
			EXPECTEDDATA_CHANNEL_2.put("blob_multi", Arrays.asList("The quick brown fox - 1.3".getBytes("UTf-8"), "The quick brown fox - 2.3".getBytes("UTf-8")));
			EXPECTEDDATA_CHANNEL_2.put("long_multi", Arrays.asList(167772163L, 167772164L));
			EXPECTEDDATA_CHANNEL_2.put("double_multi", Arrays.asList(3.14159263, 3.14159264));
			EXPECTEDDATA_CHANNEL_2.put("date_multi", Arrays.asList(getTimestamp(1972, 9, 25, 10, 35, 3), getTimestamp(1972, 9, 25, 10, 35, 4)));

			EXPECTEDDATA_CHANNEL_2.put("longtext_fs", EXPECTEDDATA_CHANNEL_2.get("longtext"));
			EXPECTEDDATA_CHANNEL_2.put("blob_fs", EXPECTEDDATA_CHANNEL_2.get("blob"));

			EXPECTEDDATA_CHANNEL_2.put("longtext_fs_multi", EXPECTEDDATA_CHANNEL_2.get("longtext_multi"));
			EXPECTEDDATA_CHANNEL_2.put("blob_fs_multi", EXPECTEDDATA_CHANNEL_2.get("blob_multi"));

			DATA.add(EXPECTEDDATA_CHANNEL_1);
			
			// Expected data after we do modification (update). 
			EXPECTEDMODDATA.put("text", "The quick green fox");
			EXPECTEDMODDATA.put("link", "1000.1013");
			EXPECTEDMODDATA.put("int", 99);
			EXPECTEDMODDATA.put("longtext", "The quick green fox jumps over the lazy dog");
			EXPECTEDMODDATA.put("blob", "The quick green fox".getBytes("UTf-8"));
			EXPECTEDMODDATA.put("long", 16777217L);
			EXPECTEDMODDATA.put("double", 2.71828);
			EXPECTEDMODDATA.put("date", getTimestamp(1972, 11, 26, 10, 35, 0));

			EXPECTEDMODDATA.put("text_opt", "The quick green fox");
			EXPECTEDMODDATA.put("link_opt", "1000.1013");
			EXPECTEDMODDATA.put("int_opt", 99);
			EXPECTEDMODDATA.put("long_opt", 16777217L);
			EXPECTEDMODDATA.put("double_opt", 2.71828);
			EXPECTEDMODDATA.put("date_opt", getTimestamp(1972, 11, 26, 10, 35, 0));

			EXPECTEDMODDATA.put("text_multi", Arrays.asList("The quick green fox - 1", "The quick green fox - 2"));
			EXPECTEDMODDATA.put("link_multi", Arrays.asList("1000.1013", "1000.1014"));
			EXPECTEDMODDATA.put("int_multi", Arrays.asList(991, 992));
			EXPECTEDMODDATA.put("longtext_multi",
					Arrays.asList("The quick green fox jumps over the lazy dog - 1", "The quick green fox jumps over the lazy dog - 2"));
			EXPECTEDMODDATA.put("blob_multi", Arrays.asList("The quick green fox - 1".getBytes("UTf-8"), "The quick green fox - 2".getBytes("UTf-8")));
			EXPECTEDMODDATA.put("long_multi", Arrays.asList(167772171L, 167772172L));
			EXPECTEDMODDATA.put("double_multi", Arrays.asList(2.718281, 2.718282));
			EXPECTEDMODDATA.put("date_multi", Arrays.asList(getTimestamp(1972, 11, 26, 10, 35, 1), getTimestamp(1972, 11, 26, 10, 35, 2)));

			EXPECTEDMODDATA.put("longtext_fs", EXPECTEDMODDATA.get("longtext"));
			EXPECTEDMODDATA.put("blob_fs", EXPECTEDMODDATA.get("blob"));

			EXPECTEDMODDATA.put("longtext_fs_multi", EXPECTEDMODDATA.get("longtext_multi"));
			EXPECTEDMODDATA.put("blob_fs_multi", EXPECTEDMODDATA.get("blob_multi"));

			appender = new AssertionAppender();
			NodeLogger.getRootLogger().addAppender(appender);
		} catch (Exception e) {
			logger.error("Error while initializing data", e);
		}
	}

	/**
	 * Get a sql timestamp object for the given date
	 * @param year year
	 * @param month month (1 is january)
	 * @param day day in the month
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 * @return timestamp object
	 */
	protected static Timestamp getTimestamp(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();

		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * Default constructor used for parameterized junit tests
	 * @param testDatabase test database
	 * @param attribute name of the tested attribute
	 * @throws SQLUtilException 
	 * @throws JDBCMalformedURLException
	 */
	protected AbstractMCCRDatasourceWriteTest(TestDatabase testDatabase, String attribute, boolean useDiffetentContentId, 
			boolean useDifferentAttrValue, int ignoreChannelId, int channelIdToUpdate) throws SQLUtilException, JDBCMalformedURLException {
		super(testDatabase);
		this.attribute = attribute;
		this.useDifferentContentId = useDiffetentContentId;
		this.useDifferentAttrValue = useDifferentAttrValue;
		this.ignoreChannelId = ignoreChannelId;
		this.channelIdToUpdate = channelIdToUpdate;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		targetDirectory = "target_" + TestEnvironment.getRandomHash(8);
	}

	/**
	 * Setup the test database if not done before
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		appender.reset();
		TestDatabase testDatabase = getTestDatabase();

		if (!databases.containsKey(testDatabase.getIdentifier())) {
			databases.put(testDatabase.getIdentifier(), testDatabase);
			logger.debug("Creating test database.");
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			logger.debug("Created database with name: {" + sqlUtils.getTestDatabase().getDBName() + "}");

			String definitionFileName = "mccr_tests_structure.xml";

			sqlUtils.connectDatabase();
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			sqlUtils.createDatabase();
			
			ds = createWritableDataSource();
			ObjectManagementManager.importTypes(ds, getClass().getResourceAsStream(definitionFileName));
			
			createChannelStructure();
			
			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = createWritableDataSource();

			// clean the data in the cr
			DBHandle dbHandle = ds.getHandle();

			// thanks to referential integrity in the DB, it is sufficient to delete the objects from contentmap
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName());
			ds.clearCaches();
		}

		// make sure that the filesystem basepath exists and is empty
		File basePath = new File(FS_BASEPATH);

		basePath.mkdirs();
		FileUtil.cleanDirectory(basePath);

		// create the target datasource
		createTargetDatasource();

		// create the link targets
		ds.setUpdateTimestampOnWrite(false);
		Collection<Resolvable> linkTargets = new ArrayList<Resolvable>();
		Map<String, Object> data = new HashMap<String, Object>();

		for (int channelId = 1; channelId <= 3; channelId++) {
			for (Integer channelsetId : Arrays.asList(1012, 1013, 1014)) {
				data.clear();
				data.put("contentid", "1000." + channelsetId);
				data.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, channelsetId);
				data.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, channelId);
				MCCRObject linkTarget = (MCCRObject) ds.create(data);

				linkTarget.setUpdateTimestamp(1);
				linkTargets.add(linkTarget);
			}
		}
		ds.store(linkTargets);
	}

	/**
	 * Teardown
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (targetDS != null) {
			DB.update(targetDS.getHandle(), "DELETE FROM channel");
			FileUtils.cleanDirectory(new File(targetDS.getAttributePath()));
			targetDS = null;
		}
	}

	/**
	 * Clean the databases when finished with all tests
	 * @throws Exception
	 */
	@AfterClass
	public static void tearDownOnce() throws Exception {
		logger.debug("Cleanup of test databases.");
		PortalConnectorFactory.destroy();
		for (TestDatabase testDatabase : databases.values()) {
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			sqlUtils.connectDatabase();
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Check whether a specific testDB/attribute conbination is excluded
	 * @param testDB test database
	 * @param attribute attribute name
	 * @return true if the test is excluded, false if not
	 */
	public static boolean isExcluded(TestDatabase testDB, String attribute) {
		if (!EXCLUDEDTESTS.containsKey(testDB.getIdentifier())) {
			return false;
		} else {
			return EXCLUDEDTESTS.get(testDB.getIdentifier()).contains(attribute);
		}
	}

	/**
	 * Create channel structure for the tests: master -> channel1 -> channel2
	 * 
	 * @throws Exception
	 */
	public void createChannelStructure() throws Exception {
		ChannelTree tree = new ChannelTree();
		
		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(1, "Master"));

		tree.getChildren().add(master);
		
		ChannelTreeNode channel1 = new ChannelTreeNode(new DatasourceChannel(2, "Channel 1"));

		master.getChildren().add(channel1);
		
		ChannelTreeNode channel2 = new ChannelTreeNode(new DatasourceChannel(3, "Channel 2"));

		channel1.getChildren().add(channel2);
		
		ds.saveChannelStructure(tree);
	}
	
	/**
	 * Test reading and writing objects to/from content repository.
	 * 
	 * Test a couple of cases for each parameter variation:
	 * 
	 * - test if the stored data for each attribute is equal to the red data - do that for different conditions:
	 * --> same or different contentIDs in channels
	 * --> same or different attribute values in channels
	 * --> missing object in one of the channels
	 * 
	 * - test if the stored data has no redundancies and work correctly - do that for all the cases presented in the 
	 * previous paragraph while updating the current attribute data in each channel (one at the time). 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadingWritingObjects() throws Exception {
		dataWriteReadTest(Value.REAL);
	}

	/**
	 * Test writing and reading null
	 * @throws Exception
	 */
	@Test
	public void testWriteNull() throws Exception {
		dataWriteReadTest(Value.NULL);
	}

	/**
	 * Test writing and reading empty strings
	 * @throws Exception
	 */
	@Test
	public void testWriteEmptyString() throws Exception {
		dataWriteReadTest(Value.EMPTYSTRING);
	}

	/**
	 * Test writing and reading when the attribute is omit
	 * @throws Exception
	 */
	@Test
	public void testWriteOmit() throws Exception {
		dataWriteReadTest(Value.OMIT);
	}

	/**
	 * Do the data write and read test
	 * @param value type of the inserted value
	 * @throws Exception
	 */
	protected void dataWriteReadTest(Value value) throws Exception {
		// We need to store the reference to the attribute we have to change after storing it in all channels. 
		Changeable attributeToChange = null;
		
		String firstStoredObjectId = null;

		ds.setUpdateTimestampOnWrite(false);

		// Add object in three channels and check if the object is really there
		
		// Iterate through channel IDs
		for (int i = 1; i <= 3; i++) {
			if (i == ignoreChannelId) {
				continue; // Ignored channel ID
			}
			
			/* Generate the content ID based on the current channel if the flag if the contentIds 
			 * should be different for each channel
			 */
			String contentId = useDifferentContentId ? "1000." + i : "1000.1";

			if (firstStoredObjectId == null) {
				firstStoredObjectId = contentId;
			}
			
			/* Get the expected data map for the correct channel (if the data flag indicates the 
			 * data should be different - if not, use the map for master node instead)
			 */
			Map<String, Object> currentData = useDifferentAttrValue ? DATA.get(i - 1) : DATA.get(0);
			
			// Initialize parameters for storing the object - we store contentId, channelSetId and the current attribute
			Map<String, Object> params = new HashMap<String, Object>();

			params.put("contentid", contentId);
			params.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, 99);
			// Set the current channel ID
			params.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, i);
			switch (value) {
			case REAL:
				params.put(attribute, currentData.get(attribute));
				break;

			case NULL:
				params.put(attribute, null);
				break;

			case EMPTYSTRING:
				params.put(attribute, "");
				break;

			case OMIT:
				break;
			}

			// Create the object we want to store
			Changeable object = ds.create(params);

			assertNotNull("Check object", object);
			
			// Do the actual storing
			ds.store(Collections.singleton(object));
			
			/* Check if we are processing the channel in which we should update the attribute and save the 
			 * reference to the stored attribute.
			 */
			if (channelIdToUpdate == i) {
				attributeToChange = object;
			}
		}
		
		// Read the object from the channel that need to be updated
		ds.setChannel(channelIdToUpdate);
		
		ds.clearCaches();

		// Update attribute data and store the attribute again
		if (value == Value.REAL) {
			attributeToChange.setProperty(attribute, EXPECTEDMODDATA.get(attribute));
		}
		ds.store(Collections.singleton(attributeToChange));

		ds.clearCaches();
		
		// Iterate through channel IDs
		for (int i = 1; i <= 3; i++) {

			// Set the current channel ID
			ds.setChannel(i);
			
			/* Use the first stored cotentId (for the most upper parent node) if the contentIds 
			 * should be different for each channel, otherwise use a static ID for all channels.
			 */
			String contentId = useDifferentContentId ? firstStoredObjectId : "1000.1";
			
			if (i == ignoreChannelId) {
				
				// Read the object
				Resolvable obj = PortalConnectorFactory.getContentObject(contentId, ds);

				assertNull("Object should be null", obj);
				
				continue; // Ignored channel ID
			}
			
			/* Get the expected data map for the correct channel (if the data flag indicates the 
			 * data should be different - if not, use the map for master node instead)
			 */
			Map<String, Object> currentData = useDifferentAttrValue ? DATA.get(i - 1) : DATA.get(0);

			// Check if we are processing the channel in which we should update the attribute.
			if (channelIdToUpdate == i) {
				currentData = EXPECTEDMODDATA;
			}

			// Read the object for specified contentId in the current channel.
			Resolvable obj = PortalConnectorFactory.getContentObject(contentId, ds);

			switch (value) {
			case REAL:
				assertData("Check data", currentData.get(attribute), obj.get(attribute));
				break;

			case NULL:
			case EMPTYSTRING:
			case OMIT:
				if (attribute.endsWith("_multi")) {
					Object readValue = obj.get(attribute);

					if (readValue instanceof Collection<?>) {
						assertEquals("Check array size", 0, ((Collection) readValue).size());
					} else {
						fail("Expected reading empty array");
					}
				} else {
					assertNull("Check data", obj.get(attribute));
				}
				break;
			}
		}
	}

	/**
	 * Test a sync run into an empty content repository
	 * @throws Exception
	 */
	@Test
	public void testSync() throws Exception {
		// We need to store the reference to the attribute we have to change after storing it in all channels. 
		Changeable attributeToChange = null;
		
		String firstStoredObjectId = null;

		ds.setUpdateTimestampOnWrite(false);

		// Add object in three channels and check if the object is really there

		// Iterate through channel IDs
		for (int i = 1; i <= 3; i++) {
			if (i == ignoreChannelId) {
				continue; // Ignored channel ID
			}
			
			/* Generate the content ID based on the current channel if the flag if the contentIds 
			 * should be different for each channel
			 */
			String contentId = useDifferentContentId ? "1000." + i : "1000.1";

			if (firstStoredObjectId == null) {
				firstStoredObjectId = contentId;
			}
			
			/* Get the expected data map for the correct channel (if the data flag indicates the 
			 * data should be different - if not, use the map for master node instead)
			 */
			Map<String, Object> currentData = useDifferentAttrValue ? DATA.get(i - 1) : DATA.get(0);
			
			// Initialize parameters for storing the object - we store contentId, channelSetId and the current attribute
			Map<String, Object> params = new HashMap<String, Object>();

			params.put("contentid", contentId);
			params.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, 99);
			params.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, i);
			params.put(attribute, currentData.get(attribute));

			// Create the object we want to store
			Changeable object = ds.create(params);

			assertNotNull("Check object", object);
			
			// Do the actual storing
			((MCCRObject) object).setUpdateTimestamp(1);
			ds.store(Collections.singleton(object));
			ds.setLastUpdate(((MCCRObject) object).getChannelId(), 1);

			/* Check if we are processing the channel in which we should update the attribute and save the 
			 * reference to the stored attribute.
			 */
			if (channelIdToUpdate == i) {
				attributeToChange = object;
			}
		}

		// now run a sync into the target datasource
		MCCRSync sync = new MCCRSync(ds, targetDS, false, true, true, true, true, 100, null);

		sync.doSync();

		// Iterate through channel IDs
		for (int i = 1; i <= 3; i++) {

			// Set the current channel ID
			targetDS.setChannel(i);

			/* Use the first stored cotentId (for the most upper parent node) if the contentIds 
			 * should be different for each channel, otherwise use a static ID for all channels.
			 */
			String contentId = useDifferentContentId ? firstStoredObjectId : "1000.1";

			if (i == ignoreChannelId) {
				// Read the object
				Resolvable obj = PortalConnectorFactory.getContentObject(contentId, targetDS);

				assertNull("Object should be null", obj);

				continue; // Ignored channel ID
			}

			/* Get the expected data map for the correct channel (if the data flag indicates the 
			 * data should be different - if not, use the map for master node instead)
			 */
			Map<String, Object> currentData = useDifferentAttrValue ? DATA.get(i - 1) : DATA.get(0);

			// Read the object for specified contentId in the current channel.
			Resolvable obj = PortalConnectorFactory.getContentObject(contentId, targetDS);

			assertData("Check data", currentData.get(attribute), obj.get(attribute));
		}

		// Read the object from the channel that need to be updated
		ds.setChannel(channelIdToUpdate);
		
		ds.clearCaches();

		// Update attribute data and store the attribute again
		attributeToChange.setProperty(attribute, EXPECTEDMODDATA.get(attribute));
		((MCCRObject) attributeToChange).setUpdateTimestamp(2);
		ds.store(Collections.singleton(attributeToChange));
		ds.setLastUpdate(((MCCRObject) attributeToChange).getChannelId(), 2);

		ds.clearCaches();

		// now run a sync again into the target datasource
		sync = new MCCRSync(ds, targetDS, false, true, true, true, true, 100, null);
		sync.doSync();

		// Iterate through channel IDs
		for (int i = 1; i <= 3; i++) {

			// Set the current channel ID
			targetDS.setChannel(i);
			
			/* Use the first stored cotentId (for the most upper parent node) if the contentIds 
			 * should be different for each channel, otherwise use a static ID for all channels.
			 */
			String contentId = useDifferentContentId ? firstStoredObjectId : "1000.1";
			
			if (i == ignoreChannelId) {
				
				// Read the object
				Resolvable obj = PortalConnectorFactory.getContentObject(contentId, targetDS);

				assertNull("Object should be null", obj);
				
				continue; // Ignored channel ID
			}
			
			/* Get the expected data map for the correct channel (if the data flag indicates the 
			 * data should be different - if not, use the map for master node instead)
			 */
			Map<String, Object> currentData = useDifferentAttrValue ? DATA.get(i - 1) : DATA.get(0);

			// Check if we are processing the channel in which we should update the attribute.
			if (channelIdToUpdate == i) {
				currentData = EXPECTEDMODDATA;
			}

			// Read the object for specified contentId in the current channel.
			Resolvable obj = PortalConnectorFactory.getContentObject(contentId, targetDS);

			assertData("Check data", currentData.get(attribute), obj.get(attribute));
		}
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 * @param message message
	 * @param expected expected data
	 * @param data actual data
	 */
	protected void assertData(String message, Object expected, Object actual) {
		if (expected instanceof byte[] && actual instanceof byte[]) {
			byte[] expectedBytes = (byte[]) expected;
			byte[] actualBytes = (byte[]) actual;

			assertEquals(message + " length", expectedBytes.length, actualBytes.length);
			for (int i = 0; i < expectedBytes.length; i++) {
				assertEquals(message + " data[" + i + "]", expectedBytes[i], actualBytes[i]);
			}
		} else if (expected instanceof List && actual instanceof List) {
			List<?> expectedColl = (List<?>) expected;
			List<?> actualColl = (List<?>) actual;

			assertEquals(message + " length", expectedColl.size(), actualColl.size());
			for (int i = 0; i < expectedColl.size(); i++) {
				assertData(message + " data[" + i + "]", expectedColl.get(i), actualColl.get(i));
			}
		} else if (expected instanceof Resolvable || actual instanceof Resolvable) {
			assertData(message, expected.toString(), actual.toString());
		} else {
			// get the attribute
			try {
				// for attributes of type "text (short)" we probably need to truncate the expected content
				ObjectAttributeBean attributeType = MCCRHelper.getAttributeType(ds, 1000, attribute);

				if (expected instanceof String && attributeType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_TEXT) {
					expected = ds.getHandle().getStringLengthManipulator().truncate((String) expected, 255);
				}
			} catch (DatasourceException e) {
				fail("Could not get attribute type");
			}
			assertEquals(message, expected, actual);
		}
	}

	/**
	 * Create multichannelling datasource
	 * @return datasource
	 */
	@SuppressWarnings("unused")
	private MultichannellingDatasource createDataSource() {
		return createDataSource(false);
	}
	
	/**
	 * Create writable multichannelling datasource
	 * @return datasource
	 */
	private WritableMCCRDatasource createWritableDataSource() {
		return (WritableMCCRDatasource) createDataSource(true);
	}
	
	/**
	 * Create a multichannelling datasource
	 * @return datasource
	 */
	private MultichannellingDatasource createDataSource(boolean requestWritableDatasource) {
		Map<String, String> handleProperties = testDatabase.getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, FS_BASEPATH);

		MultichannellingDatasource ds = null;

		// Get normal or writable datasource
		if (requestWritableDatasource) {
			ds = PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);
		} else {
			ds = PortalConnectorFactory.createMultichannellingDatasource(handleProperties, dsProperties);
		}

		if (ds == null) {
			fail("Creation of the datasource failed: " + appender.getErrors());
			return null;
		} else {
			return ds;
		}
	}

	/**
	 * Create the target datasource for the sync process
	 */
	private void createTargetDatasource() {
		File commonBase = new File(".", targetDirectory);
		File targetBase = new File(commonBase, "targetds");

		targetBase.mkdirs();

		Map<String, String> targetHandleProps = new HashMap<String, String>();

		targetHandleProps.put("type", "jdbc");

		targetHandleProps.put("driverClass", "org.hsqldb.jdbcDriver");
		targetHandleProps.put("url", "jdbc:hsqldb:mem:target");
		targetHandleProps.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> targetDSProps = new HashMap<String, String>();

		targetDSProps.put("sanitycheck2", "true");
		targetDSProps.put("autorepair2", "true");
		targetDSProps.put("cache", "false");
		targetDSProps.put("attribute.path", targetBase.getAbsolutePath());
		targetDSProps.put("prefetchAttribute.threshold", "0");		

		targetDS = (WritableMCCRDatasource) PortalConnectorFactory.createWritableMultichannellingDatasource(targetHandleProps, targetDSProps);
	}

	/**
	 * Enumeration of values, that can be inserted
	 */
	public enum Value {

		/**
		 * Real value
		 */
		REAL, /**
		 * null
		 */ NULL, /**
		 * empty string
		 */ EMPTYSTRING, /**
		 * Completely omit the attribute
		 */ OMIT
	}
}
