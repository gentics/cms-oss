/*
 * @author herbert
 * @date 27.03.2006
 * @version $Id: CNWritableDatasourceTest.java,v 1.2 2010-09-28 17:08:12 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.jcs.admin.JCSAdminBean;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.PropertySetter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.node.testutils.NodeTestUtils;
import com.gentics.portalconnector.tests.AbstractLegacyNavigationDumpTest;
import com.gentics.testutils.GenericTestUtils;
import org.junit.experimental.categories.Category;

/**
 * Simple test for a CNWriteable datasource + cache
 */
@Category(BaseLibTest.class)
public class CNWritableDatasourceTest extends AbstractLegacyNavigationDumpTest {
	private CNWriteableDatasource ds;

	private NodeLogger logger;

	public static final String TEST_CONTENTID = "10007.10994";

	public static final String TEST_RULETREE = "object.obj_type == 10007";

	public static final String TEST_OBJECT_NAME = "Sitemap PAT3";

	public static final String TEST_VALUE = "TESTVALUE";

	public static final String TEST_MULTIVALUE = "testmultivalue";

	/**
	 * number of concurrent threads for the {@link #testConcurrentObjectCreation()} test.
	 */
	public final static int CONCURRENT_THREADS = 100;

	/**
	 * number of concurrent processes for the {@link #testMultiProcessObjectCreation()} test
	 */
	public final static int CONCURRENT_PROCESSES = 20;

	/**
	 * maximum allowed runtime for the test {@link #testMultiProcessObjectCreation()} (5 minutes)
	 */
	public final static int CONCURRENT_TEST_MAXRUNTIME = 5 * 60 * 1000;

	@Before
	public void setUp() throws Exception {

		logger = NodeLogger.getNodeLogger(this.getClass());
		GenericTestUtils.initConfigPathForCache();

		prepareTestDatabase(getClass().getSimpleName());
		insertDumpIntoDatabase();

		Properties handleProperties = getTestDatabase().getSettings();

		// turn on the cache
		Map dsProps = new HashMap(handleProperties);

		dsProps.put("cache", Boolean.toString(true));

		SQLHandle handle;

		// lookup existing handles in acvtivehandles map
		handle = null;
		// not found, create new handle
		if (handle == null) {
			handle = new SQLHandle("mumu");
			handle.init(handleProperties);
		}
		ds = new CNWriteableDatasource(null, new SimpleHandlePool(handle), dsProps);
	}

	@After
	public void tearDown() throws Exception {
		printCacheStatisticsAndClear();

		// repair all id_counters
		ds.repairIdCounters(null);
		removeTestDatabase();

	}

	protected void printCacheStatisticsAndClear() throws Exception {
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		String[] names = cacheManager.getCacheNames();

		logger.info("Count of CacheNames: " + names.length);
		logger.info("==========================================");
		for (int i = 0; i < names.length; i++) {
			logger.info("Name: " + names[i]);
			CompositeCache cache = cacheManager.getCache(names[i]);

			logger.info(" `- Bytes            : " + new JCSAdminBean().getByteCount(cache));
			logger.info(" `- Size             : " + cache.getSize());
			logger.info(" `- RAM HitCount     : " + cache.getHitCountRam());
			logger.info(" `- MissCountNotFound: " + cache.getMissCountNotFound());
			logger.info(" `- MissCountExpired : " + cache.getMissCountExpired());
			// logger.info(" `- Stats: " + cache.getStats());
			logger.info(" `- MaxObjects       : " + cache.getCacheAttributes().getMaxObjects());
			cache.removeAll();
		}
		logger.info("did tests on " + ds.getHandlePool().getHandle());
	}

	/**
	 * Tests the cache by asking for hit and miss counts
	 *
	 * @throws Exception
	 */
	@Test
	public void testSpecificCache() throws Exception {
		String dsRule = "object.obj_type == 10002";

		mytestSpecificCachePerformance(dsRule);
		long[] mintimes = null;

		for (int i = 0; i < 10; i++) {
			if (i == 5) {
				Thread.sleep(2000);
			}
			printCacheStatisticsAndClear();
			long[] times = mytestSpecificCachePerformance(dsRule);

			logger.info("Times[0]: " + times[0]);
			logger.info("Times[1]: " + times[1]);
			if (mintimes == null) {
				mintimes = times;
			} else {
				if (mintimes[0] > times[0]) {
					mintimes[0] = times[0];
				}
				if (mintimes[1] > times[1]) {
					mintimes[1] = times[1];
				}
			}
		}
		logger.info(" ... " + mintimes[1] + " vs. " + mintimes[0]);
		assertTrue(
				"Verifying that cached Queries were fast enough. (With cache: {" + mintimes[1] + "} Without cache: {" + mintimes[0] + "} - has to be 5 times faster)",
				(mintimes[1] * 5 < mintimes[0]));
	}

	public long[] mytestSpecificCachePerformance(String dsRule) throws Exception {
		long[] time = new long[2];
		Collection coll = NodeTestUtils.getObjects(ds, dsRule, 0, 400);
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		CompositeCache attributeCache = cacheManager.getCache("gentics-portal-contentrepository-atts");
		CompositeCache objectCache = cacheManager.getCache("gentics-portal-contentrepository-objects");

		logger.debug("Loaded collection - " + coll.size());
		long p1 = System.currentTimeMillis();

		mytestSpecificCacheQueryObjects(coll);
		time[0] = (System.currentTimeMillis() - p1);
		logger.debug("Duration         : " + time[0]);
		logger.debug("Collsize         : " + coll.size());
		logger.debug("AttributeCache   : " + attributeCache.getSize());
		logger.debug("ObjectCache      : " + objectCache.getSize());
		int objectHitCount = objectCache.getHitCountRam();

		coll = NodeTestUtils.getObjects(ds, dsRule, 0, 400);
		logger.debug("--------------");
		logger.debug("Object HitCount  : " + (objectCache.getHitCountRam() - objectHitCount) + "  (before: " + objectHitCount + ")");
		int hitCount = attributeCache.getHitCountRam();
		int missCount = attributeCache.getMissCountNotFound();

		p1 = System.currentTimeMillis();
		mytestSpecificCacheQueryObjects(coll);
		time[1] = (System.currentTimeMillis() - p1);
		logger.debug("--------------");
		logger.debug("Duration         : " + time[1]);
		logger.debug("Collsize         : " + coll.size());
		logger.debug("AttributeCache   : " + attributeCache.getSize());
		logger.debug("ObjectCache      : " + objectCache.getSize());
		logger.debug("RAM Hits         : " + (attributeCache.getHitCountRam() - hitCount));
		logger.debug("Misses           : " + (attributeCache.getMissCountNotFound() - missCount));

		assertEquals("Verifying that we have 0 misses.", 0, (attributeCache.getMissCountNotFound() - missCount));

		return time;
	}

	public void mytestSpecificCacheQueryObjects(Collection coll) {
		Iterator i = coll.iterator();

		while (i.hasNext()) {
			Resolvable r = (Resolvable) i.next();

			r.getProperty("name");
			// r.getProperty("obj_type");
			// r.getProperty("contentid");
			// r.getProperty("startpage");
			r.getProperty("folder_id");
		}
	}

	/**
	 * Tests if the query cache gets cleared correctly if an attribute is changed. (Actually the same as {@link #testQueryCache()} but
	 * through the datasource, not through JDBC)
	 *
	 * @throws Exception
	 */
	@Test
	public void testQueryCacheClear() throws Exception {
		String dsRule = "object.name == \"Aktuelles\"";
		String testName = "TESTNAME";
		int originalResults = 9;
		String originalContentId = "10002.10002";

		Collection coll = NodeTestUtils.getObjects(ds, dsRule);

		assertEquals("Test if original content is correct.", originalResults, coll.size());

		Resolvable r = getObjectById(coll, originalContentId);

		assertNotNull("Test if we got the right object.", r);

		r = (Resolvable) ((DatasourceRow) r).toObject();
		((Changeable) r).setProperty("name", testName);
		((WriteableDatasource) ds).store(Collections.singleton(r));

		coll = NodeTestUtils.getObjects(ds, dsRule);
		assertEquals("Test if cache was cleared.", originalResults - 1, coll.size());

		((Changeable) r).setProperty("name", "Aktuelles");
		((WriteableDatasource) ds).store(Collections.singleton(r));

		coll = NodeTestUtils.getObjects(ds, dsRule);
		assertEquals("Test if object is back to normal", originalResults, coll.size());
	}

	/**
	 * Tests the Query Cache by manipulating the data directly through JDBC.
	 *
	 * @throws Exception
	 */
	@Test
	public void testQueryCache() throws Exception {
		String dsRule = "object.name == \"Aktuelles\"";
		String testName = "TESTNAME";
		int originalResults = 9;
		String originalContentId = "10002.10002";

		Collection coll = NodeTestUtils.getObjects(ds, dsRule);

		assertEquals("Test if original content is correct.", originalResults, coll.size());

		Resolvable r = getObjectById(coll, originalContentId);

		assertNotNull("Test if we got the right object.", r);

		DB.update(ds.getHandle().getDBHandle(), "UPDATE " + ds.getHandle().getDBHandle().getContentAttributeName() + " SET value_text = ? WHERE contentid = ? AND name = 'name'",
				new Object[] { testName, originalContentId });
		DB.update(ds.getHandle().getDBHandle(), "UPDATE " + ds.getHandle().getDBHandle().getContentMapName() + " SET quick_name = ? WHERE contentid = ?",
				new Object[] { testName, originalContentId });

		coll = NodeTestUtils.getObjects(ds, dsRule);
		assertEquals("Test if query was cached successfully.", originalResults, coll.size());

		DB.update(ds.getHandle().getDBHandle(), "UPDATE " + ds.getHandle().getDBHandle().getContentAttributeName() + " SET value_text = ? WHERE contentid = ? AND name = 'name'",
				new Object[] { "Aktuelles", originalContentId });
		DB.update(ds.getHandle().getDBHandle(), "UPDATE " + ds.getHandle().getDBHandle().getContentMapName() + " SET quick_name = ? WHERE contentid = ?", new Object[] {
			"Aktuelles", originalContentId });
	}

	/**
	 * Tests the cache by requesting an object, and changing it directly in the DB without that the gentics content object knows about.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCache() throws Exception {
		String testName = "TESTNAME";
		String dsRule = "object.obj_type == 10002";
		Collection coll = NodeTestUtils.getObjects(ds, dsRule);
		Resolvable r = (Resolvable) coll.iterator().next();
		String contentid = (String) r.get("contentid");
		String name = (String) r.get("name");

		logger.info("Contentid: " + contentid);
		logger.info("Name: " + name);
		DB.update(ds.getHandle().getDBHandle(),
				"UPDATE " + ds.getHandle().getDBHandle().getContentMapName() + " SET quick_name = ? where contentid = ?", new Object[] { testName, contentid }, null);

		Resolvable newResolvable = PortalConnectorFactory.getContentObject(contentid, ds);

		assertEquals(name, newResolvable.get("name"));

		DB.update(ds.getHandle().getDBHandle(),
				"UPDATE " + ds.getHandle().getDBHandle().getContentMapName() + " SET quick_name = ? where contentid = ?", new Object[] { name, contentid }, null);

	}

	@Test
	public void testCreateObjects() throws Exception {
		String testName = "JUSTANEVILTEST";

		assertTrue(((WriteableDatasource) ds).canWrite());
		Collection coll = NodeTestUtils.getObjects(ds, "object.name == \"" + testName + "\"");

		assertEquals("Test if we really got zero results fo r that name.", 0, coll.size());
		Map map = new HashMap();

		map.put("obj_type", "10007");
		map.put("name", testName);
		Changeable changeable = ((WriteableDatasource) ds).create(map);

		logger.info("Changeable - name: " + changeable.get("name") + "  contentid: " + changeable.get("contentid"));
		((WriteableDatasource) ds).store(Collections.singleton(changeable));
		logger.info("Changeable - name: " + changeable.get("name") + "  contentid: " + changeable.get("contentid"));

		coll = NodeTestUtils.getObjects(ds, "object.name == \"" + testName + "\"");
		assertEquals("Test if object was created (and query not cached)", 1, coll.size());

		((WriteableDatasource) ds).delete(Collections.singleton(changeable));

		coll = NodeTestUtils.getObjects(ds, "object.name == \"" + testName + "\"");
		assertEquals("Test if object was successfully deleted (and query not cached)", 0, coll.size());
	}

	@Test
	public void testShortTestTruncationSimple() throws Exception {
		String testName = "This is a very long name of a page. " + "It is so long that it won't fit into the table column. "
				+ "Hopefully the datasource will take care of it and will truncate it. " + "Otherwise this name will cause some ugly exceptions. I'm not kidding! "
				+ "Have you ever tried to store something this long into a varchar(255)? " + "Trust me, you don't want to see it."
				+ "Ok, I think this should be enough to get over the specified 255" + "of a text(short).";

		assertTrue(((WriteableDatasource) ds).canWrite());
		Map map = new HashMap();

		map.put("obj_type", "10007");
		map.put("name", testName);
		Changeable changeable = ((WriteableDatasource) ds).create(map);
		Collection result = ((WriteableDatasource) ds).store(Collections.singleton(changeable)).getAffectedRecords();

		assertTrue("Test if the object was stored correctly.", !result.isEmpty());

		Collection coll = NodeTestUtils.getObjects(ds, "object.contentid == \"" + changeable.get("contentid") + "\"");

		assertEquals("Test if object was successfully retrieved", 1, coll.size());
		Resolvable r = (Resolvable) coll.iterator().next();

		String storedValue = ObjectTransformer.getString(r.get("name"), null);

		assertNotNull("The stored value should not be null.", storedValue);
		assertTrue("The stored value should not be empty", !storedValue.equals(""));
		assertTrue("The stored value should contain at least the first few caracters of the name", storedValue.startsWith("This is a very long name of a page."));
		// remove the object
		((WriteableDatasource) ds).delete(Collections.singleton(changeable));
		coll = NodeTestUtils.getObjects(ds, "object.contentid == \"" + changeable.get("contentid") + "\"");
		assertEquals("Test if object was successfully deleted (and query not cached)", 0, coll.size());
	}

	@Test
	public void testShortTestTruncationSpecialChars() throws Exception {
		String testName = "ÖÄÜß\u00D1 \u00F4 \u00CB This is a very long name of a page with special characters. "
				+ "It is so long that it won't fit into the table column. " + "Hopefully the datasource will take care of it and will truncate it. "
				+ "Otherwise this name will cause some ugly exceptions. I'm not kidding! " + "Have you ever tried to store something this long into a varchar(255)? "
				+ "Trust me, you don't want to see it." + "Ok, I think this should be enough to get over the specified 255" + "of a text(short).";

		assertTrue(((WriteableDatasource) ds).canWrite());
		Map map = new HashMap();

		map.put("obj_type", "10007");
		map.put("name", testName);
		Changeable changeable = ((WriteableDatasource) ds).create(map);
		Collection result = ((WriteableDatasource) ds).store(Collections.singleton(changeable)).getAffectedRecords();

		assertTrue("Test if the object was stored correctly.", !result.isEmpty());

		Collection coll = NodeTestUtils.getObjects(ds, "object.contentid == \"" + changeable.get("contentid") + "\"");

		assertEquals("Test if object was successfully retrieved", 1, coll.size());
		Resolvable r = (Resolvable) coll.iterator().next();

		String storedValue = ObjectTransformer.getString(r.get("name"), null);

		assertNotNull("The stored value should not be null.", storedValue);
		assertTrue("The stored value should not be empty", !storedValue.equals(""));
		assertTrue("The stored value should contain at least the first few caracters of the name",
				storedValue.startsWith("ÖÄÜß\u00D1 \u00F4 \u00CB This is a"));
		// remove the object
		((WriteableDatasource) ds).delete(Collections.singleton(changeable));
		coll = NodeTestUtils.getObjects(ds, "object.contentid == \"" + changeable.get("contentid") + "\"");
		assertEquals("Test if object was successfully deleted (and query not cached)", 0, coll.size());
	}

	/**
	 * Test retrieving objects through a foreign link attribute.
	 *
	 * @throws Exception
	 */
	@Test
	public void testForeignLinkAttribute() throws Exception {
		String dsRule = "object.obj_type == 10002";
		int subfolderIdCount = 11;

		Collection objs = NodeTestUtils.getObjects(ds, dsRule);
		Resolvable r = getObjectById(objs, "10002.10001");

		logger.info(r.get("contentid"));
		Collection test = (Collection) r.get("subfolder_id");

		assertEquals(subfolderIdCount, test.size());

		Changeable subfolder = (Changeable) getObjectById(test, "10002.10002");

		assertEquals("Aktuelles", subfolder.get("name"));

		subfolder.setProperty("folder_id", null);

		((WriteableDatasource) ds).store(Collections.singleton(subfolder));
		test = (Collection) r.get("subfolder_id");
		assertEquals(subfolderIdCount, test.size());

		test = (Collection) PortalConnectorFactory.getContentObject(r.get("contentid").toString(), ds).get("subfolder_id");
		logger.info("Count: " + test.size());
		assertEquals("Test if the foreign link attributes got updated (from new object)", subfolderIdCount - 1, test.size());

		subfolder.setProperty("folder_id", r.get("contentid"));
		((WriteableDatasource) ds).store(Collections.singleton(subfolder));

		test = (Collection) r.get("subfolder_id");
		logger.info("Count: " + test.size());
		assertEquals(subfolderIdCount, test.size());

		// Verifying last update.
		test = (Collection) PortalConnectorFactory.getContentObject(r.get("contentid").toString(), ds).get("subfolder_id");
		assertEquals(subfolderIdCount, test.size());
	}

	/**
	 * Tests changing the link attribute when this changed attribute is part of the query.
	 */
	@Test
	public void testLinkAttributeWithChangedQueryAttribute() throws Exception {
		String foldername = "Backup";
		String foldernameTest = "TESTVALUE";
		String dsRule = "object.folder_id.name == \"Backup\"";

		Collection objs = NodeTestUtils.getObjects(ds, dsRule);

		assertEquals(2, objs.size());
		Iterator i = objs.iterator();

		i.next();
		Resolvable r = (Resolvable) i.next();

		logger.info("Res: " + r.get("contentid") + " - " + r.get("name"));

		Changeable folder = (Changeable) r.get("folder_id");

		logger.info("Folder: " + folder.get("contentid"));
		assertEquals(foldername, folder.get("name"));
		folder.setProperty("name", foldernameTest);
		((WriteableDatasource) ds).store(Collections.singleton(folder));
		assertEquals(foldernameTest, new PropertyResolver(r).resolve("folder_id.name"));

		objs = NodeTestUtils.getObjects(ds, dsRule);
		// I shouldn't get the object this time ..
		assertEquals(0, objs.size());

		folder.setProperty("name", foldername);
		((WriteableDatasource) ds).store(Collections.singleton(folder));
		assertEquals(foldername, new PropertyResolver(r).resolve("folder_id.name"));
	}

	@Test
	public void testLinkAttribute() throws Exception {
		String foldername = "Backup";
		String foldernameTest = "TESTVALUE";
		Collection objs = NodeTestUtils.getObjects(ds, TEST_RULETREE);

		Iterator i = objs.iterator();

		i.next();
		i.next();

		Resolvable resolvable = (Resolvable) i.next();
		PropertySetter propertyResolver = new PropertySetter(resolvable);
		Object obj = propertyResolver.resolve("folder_id.name");

		assertEquals(foldername, obj);
		Object folderId = propertyResolver.resolve("folder_id.contentid");
		Resolvable folder = (Resolvable) propertyResolver.resolve("folder_id");

		assertEquals(foldername, folder.get("name"));

		Resolvable folderResolvable = PortalConnectorFactory.getContentObject(folderId.toString(), ds);

		((Changeable) folderResolvable).setProperty("name", foldernameTest);
		assertEquals(foldernameTest, folderResolvable.get("name"));
		((WriteableDatasource) ds).store(Collections.singleton(folderResolvable));
		// propertyResolver.setProperty("folder_id.name",foldername_TEST);
		// assertEquals(foldername_TEST,propertyResolver.getProperty("folder_id.name"));

		objs = NodeTestUtils.getObjects(ds, TEST_RULETREE);
		i = objs.iterator();
		i.next();
		i.next();
		Resolvable newResolvable = (Resolvable) i.next();

		// Old object should still have the old value
		assertEquals(foldername, folder.get("name"));
		// New object should have new value
		assertEquals(foldernameTest, new PropertyResolver(newResolvable).resolve("folder_id.name"));
		// Value got from property resolver should also contain new value.
		assertEquals(foldernameTest, propertyResolver.resolve("folder_id.name"));

		// Change it back...
		Changeable changeable = (Changeable) newResolvable.get("folder_id");

		changeable.setProperty("name", foldername);
		assertEquals(foldername, changeable.getProperty("name"));

		((WriteableDatasource) ds).store(Collections.singleton(changeable));

		assertEquals(foldername, propertyResolver.resolve("folder_id.name"));
		assertEquals(foldername, new PropertyResolver(newResolvable).resolve("folder_id.name"));
	}

	/**
	 * a test which verifies writing to a datasource as well as (possible) cache implementations.
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoadObject() throws Exception {

		// First of all load object ...
		Collection objs = NodeTestUtils.getObjects(ds, TEST_RULETREE);
		Iterator i = objs.iterator();
		Resolvable testObj = (Resolvable) i.next();

		// got object.. test if it is the right one ...
		assertEquals(TEST_CONTENTID, testObj.get("contentid"));
		assertEquals(TEST_OBJECT_NAME, testObj.get("name"));

		// retrieve it a second time ...
		objs = NodeTestUtils.getObjects(ds, "object.contentid == '" + testObj.get("contentid") + "'");
		i = objs.iterator();
		DatasourceRow compare = (DatasourceRow) i.next();
		Changeable changeable = (Changeable) compare.toObject();

		assertEquals(TEST_OBJECT_NAME, changeable.get("name"));

		// Now see if we got two different references by modifying the proeprty
		// of one..
		changeable.setProperty("name", TEST_VALUE);
		assertEquals(TEST_VALUE, changeable.get("name"));
		assertNotSame(TEST_VALUE, testObj.get("name"));

		// Now store the value to see if we can retrieve the new value..
		((WriteableDatasource) ds).store(Collections.singletonList(changeable));

		// Retrieve the object again..
		objs = NodeTestUtils.getObjects(ds, "object.contentid == '" + testObj.get("contentid") + "'");
		i = objs.iterator();

		Resolvable newObject = (Resolvable) i.next();

		assertEquals(TEST_VALUE, newObject.get("name"));

		// Now change the value back ...
		Changeable newChangeable = ((Changeable) ((DatasourceRow) newObject).toObject());

		newChangeable.setProperty("name", TEST_OBJECT_NAME);
		((WriteableDatasource) ds).store(Collections.singletonList(newChangeable));

		// and verify that changing the value back was successful.
		objs = NodeTestUtils.getObjects(ds, "object.contentid == '" + testObj.get("contentid") + "'");
		i = objs.iterator();

		Resolvable newObject2 = (Resolvable) i.next();

		assertEquals(TEST_OBJECT_NAME, newObject2.get("name"));

	}

	/**
	 * Test the failure behaviour
	 *
	 * @throws Exception
	 */
	@Test
	public void testFailureBehaviour() throws Exception {
		Datasource invalid = NodeTestUtils.createInvalidDatasource();

		RuleTree rule = new DefaultRuleTree();

		rule.parse("object.obj_type == 1");
		invalid.setRuleTree(rule);
		try {
			invalid.getCount2();
			fail("Calling getCount2() for invalid datasource did not throw an exception");
		} catch (Exception ex) {// this is an expected exception
		}
		try {
			invalid.getResult();
			fail("Calling getResult() for invalid datasource did not throw an exception");
		} catch (Exception ex) {// this is an expected exception
		}
	}

	/**
	 * Test for concurrent object creation. Create 100 Objects concurrently and check whether all of them get different contentid's<br>
	 * NOTE: this test might succeed by chance even if the implementation does not gearantee unique contentid's
	 *
	 * @throws Exception
	 */
	@Test
	public void testConcurrentObjectCreation() throws Exception {
		String testName = "JUSTANEVILTEST";
		List contentIds = new Vector();

		ObjectCreationThread[] threads = new ObjectCreationThread[CONCURRENT_THREADS];

		for (int i = 0; i < CONCURRENT_THREADS; ++i) {
			threads[i] = new ObjectCreationThread(testName, contentIds);
			threads[i].start();
		}

		// wait until all threads finished
		int running = 100;

		while (running > 0) {
			running = 0;
			for (int i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					running++;
				}
			}
			if (running > 0) {
				logger.info(running + " threads still running, waiting a second");
				Thread.sleep(1000);
			}
		}

		// check whether a thread threw an exception
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getException() != null) {
				throw threads[i].getException();
			}
		}

		// now check for uniqueness of contentids
		Collections.sort(contentIds);
		String oldContentId = null;

		for (Iterator iter = contentIds.iterator(); iter.hasNext();) {
			String contentId = (String) iter.next();

			if (oldContentId != null && oldContentId.equals(contentId)) {
				fail("Found duplicate contentid {" + contentId + "}");
			}
		}
	}

	/**
	 * Test the autocorrection of invalid sortorder values in multivalue attributes
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore("This test is ignored, since this no longer works")
	public void testMultivalueSortorderCorrection() throws Exception {
		CNWriteableDatasource wds = (CNWriteableDatasource) ds;

		// create multivalues with invalid sortorder
		DB.update(wds.getHandle().getDBHandle(),
				"INSERT INTO " + wds.getHandle().getDBHandle().getContentAttributeName() + " (contentid, name, value_int) VALUES (?, ?, ?)",
				new Object[] { TEST_CONTENTID, TEST_MULTIVALUE, new Integer(1) });
		DB.update(wds.getHandle().getDBHandle(),
				"INSERT INTO " + wds.getHandle().getDBHandle().getContentAttributeName() + " (contentid, name, value_int) VALUES (?, ?, ?)",
				new Object[] { TEST_CONTENTID, TEST_MULTIVALUE, new Integer(2) });

		try {
			DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.contentid == '" + TEST_CONTENTID + "'"));
			Collection objs = ds.getResult(filter, null);
			// get the first object
			Iterator i = objs.iterator();
			GenticsContentObject obj = (GenticsContentObject) i.next();

			// read the attribute, check for "needfix" flag (must be set)
			GenticsContentAttribute attribute = obj.getAttribute(TEST_MULTIVALUE);

			assertTrue("Check for invalid sortorder detection", attribute.needsSortorderFixed());

			// write the attribute values
			obj.setAttribute(TEST_MULTIVALUE, new Object[] { new Integer(1), new Integer(2) });
			wds.store(Collections.singleton(obj));

			// TODO check for correct sortorder values in the db
			SimpleResultProcessor resProc = new SimpleResultProcessor();

			DB.query(wds.getHandle().getDBHandle(),
					"SELECT sortorder, value_int FROM " + wds.getHandle().getDBHandle().getContentAttributeName()
					+ " WHERE contentid = ? AND name = ? ORDER BY sortorder ASC",
					new Object[] { TEST_CONTENTID, TEST_MULTIVALUE },
					resProc);
			int sortorder = 0;

			for (Iterator iter = resProc.iterator(); iter.hasNext();) {
				sortorder++;
				SimpleResultRow row = (SimpleResultRow) iter.next();

				assertEquals("Check sortorder in row # " + sortorder, sortorder, row.getInt("sortorder"));
			}

			// re-read the attribute and check the "needfix" flag (must not be
			// set)
			objs = ds.getResult(filter, null);
			// get the first object
			i = objs.iterator();
			obj = (GenticsContentObject) i.next();
			attribute = obj.getAttribute(TEST_MULTIVALUE);

			assertFalse("Check for correct sortorder detection", attribute.needsSortorderFixed());
		} finally {
			// remove the multivalues
			DB.update(wds.getHandle().getDBHandle(),
					"DELETE FROM " + wds.getHandle().getDBHandle().getContentAttributeName() + " WHERE contentid = ? AND name = ?",
					new Object[] { TEST_CONTENTID, TEST_MULTIVALUE });
		}
	}

	@Test
	@Ignore
	public void testMultiProcessObjectCreation() throws Exception {
		String[] cmdArray = new String[] {
			System.getProperty("java.home").replaceAll(" ", "\\ ") + File.separator + "bin" + File.separator + "java",
			"-Djava.class.path=" + System.getProperty("java.class.path").replaceAll(" ", "\\ "), CNMultiProcessObjectCreationTest.class.getName() };

		List contentIds = new Vector();

		SubProcessObjectCreationThread[] threads = new SubProcessObjectCreationThread[CONCURRENT_PROCESSES];

		for (int i = 0; i < CONCURRENT_PROCESSES; ++i) {
			threads[i] = new SubProcessObjectCreationThread(cmdArray, contentIds);
			threads[i].start();
		}

		long started = System.currentTimeMillis();
		// wait until all threads finished
		int running = CONCURRENT_PROCESSES;

		long runningTime = 0;

		while (running > 0 && runningTime < CONCURRENT_TEST_MAXRUNTIME) {
			running = 0;
			for (int i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					running++;
				}
			}
			if (running > 0) {
				logger.info(running + " threads still running, waiting a second");
				Thread.sleep(1000);
			}
			runningTime = System.currentTimeMillis() - started;
		}

		// check whether all threads terminated in time
		if (running > 0) {
			for (int i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					threads[i].interrupt();
				}
			}
			fail(running + " subprocesses where still running after " + CONCURRENT_TEST_MAXRUNTIME + " ms!");
		}

		// check whether a thread threw an exception
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getException() != null) {
				throw threads[i].getException();
			}
		}

		// check for correct number of contentids
		assertEquals("Check # of generated contentids", CNMultiProcessObjectCreationTest.CONCURRENT_THREADS * CONCURRENT_PROCESSES, contentIds.size());

		// now check for uniqueness of contentids
		Collections.sort(contentIds);
		String oldContentId = null;

		for (Iterator iter = contentIds.iterator(); iter.hasNext();) {
			String contentId = (String) iter.next();

			if (oldContentId != null && oldContentId.equals(contentId)) {
				fail("Found duplicate contentid {" + contentId + "}");
			}
		}
	}

	/**
	 * check whether we may safely set the desired id_counter
	 *
	 * @param dbHandle
	 *            dbhandle
	 * @param testType
	 *            test type
	 * @param idCounterValue
	 *            desired idcounter value
	 * @throws Exception
	 */
	protected void checkIDCounterValue(DBHandle dbHandle, String testType, final Integer idCounterValue) throws Exception {
		// check whether we may safely set the desired id_counter
		DB.query(dbHandle, "SELECT max(obj_id) FROM " + dbHandle.getContentMapName() + " WHERE obj_type = " + testType,
				new ResultProcessor() {

			public void process(ResultSet rs) throws SQLException {
				rs.next();
				assertTrue("Check whether the current maximum obj_id (" + rs.getInt(1) + ") is less than " + idCounterValue,
						rs.getInt(1) < idCounterValue.intValue());
			}

			public void takeOver(ResultProcessor p) {}
		});
	}

	/**
	 * Test whether insertion of an object (without providing the contentid) uses the idcounter value
	 *
	 * @throws Exception
	 */
	@Test
	public void testNormalIDCounterSetting() throws Exception {
		DBHandle dbHandle = GenticsContentFactory.getHandle(ds);
		String testName = "JUSTANEVILTEST";
		String testType = "10007";
		Integer idCounterValue = new Integer(1000000);
		String expectedContentId = testType + "." + (idCounterValue.intValue() + 1);

		checkIDCounterValue(dbHandle, testType, idCounterValue);

		// first set the idcounter to a high number
		DB.update(dbHandle, "UPDATE " + dbHandle.getContentObjectName() + " SET id_counter = " + idCounterValue + " WHERE type = " + testType);

		// now insert a new object
		Map objectData = new HashMap();

		objectData.put("obj_type", testType);
		objectData.put("name", testName);
		Changeable object = ds.create(objectData);

		ds.store(Collections.singleton(object));

		assertEquals("Check whether id_counter was used to generate the contentid: ", expectedContentId, object.get("contentid"));
	}

	/**
	 * Test repairing the id counter value
	 *
	 * @throws Exception
	 */
	@Test
	public void testRepairingIDCounter() throws Exception {
		DBHandle dbHandle = GenticsContentFactory.getHandle(ds);
		String testType = "10007";
		Integer idCounterValue = new Integer(1000000);

		checkIDCounterValue(dbHandle, testType, idCounterValue);

		// first set the idcounter to a high number
		DB.update(dbHandle, "UPDATE " + dbHandle.getContentObjectName() + " SET id_counter = " + idCounterValue + " WHERE type = " + testType);

		// now let the datasource repair the idcounter values
		ds.repairIdCounters(null);

		// now check whether the idcounter value equals the maximum of all used
		// obj_id's
		DB.query(dbHandle,
				"SELECT type, id_counter FROM " + dbHandle.getContentObjectName() + " co WHERE id_counter < (SELECT max(obj_id) FROM " + dbHandle.getContentMapName()
				+ " cm WHERE cm.obj_type = co.type)",
				new ResultProcessor() {

			public void process(ResultSet rs) throws SQLException {
				if (rs.next()) {
					fail("Found type " + rs.getString("type") + " with incorrect value for id_counter (" + rs.getInt("id_counter") + ")");
				}
			}

			public void takeOver(ResultProcessor p) {}
		});
	}

	/**
	 * Test whether inserting an object with given contentid automatically repairs the idcounter
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoIDCounterRepairing() throws Exception {
		DBHandle dbHandle = GenticsContentFactory.getHandle(ds);
		String testType = "10007";
		String testName = "JUSTANEVILTEST";
		Integer idCounterValue = new Integer(1000000);
		final Integer expectedIdCounterValue = new Integer(idCounterValue.intValue() + 1);
		String expectedContentId = testType + "." + expectedIdCounterValue;

		checkIDCounterValue(dbHandle, testType, idCounterValue);

		// now insert a new object with expected contentid
		Map objectData = new HashMap();

		objectData.put("name", testName);
		objectData.put("contentid", expectedContentId);
		Changeable object = ds.create(objectData, -1, false);

		ds.store(Collections.singleton(object));

		// check whether the idcounter has been set accordingly
		DB.query(dbHandle, "SELECT id_counter FROM " + dbHandle.getContentObjectName() + " WHERE type = " + testType, new ResultProcessor() {

			public void process(ResultSet rs) throws SQLException {
				rs.next();
				assertEquals("Check whether the id_counter was set accordingly, ", expectedIdCounterValue.intValue(), rs.getInt("id_counter"));
			}

			public void takeOver(ResultProcessor p) {}
		});
	}

	/**
	 * Test whether inserting an object with given contentid does NOT automatically repair the idcounter, if this is disabled
	 *
	 * @throws Exception
	 */
	@Test
	public void testNoAutoIDCounterRepairing() throws Exception {
		// first disable the feature to automatically repair the id_counter
		// values
		ds.setRepairIDCounterOnInsert(false);

		DBHandle dbHandle = GenticsContentFactory.getHandle(ds);
		String testType = "10007";
		String testName = "JUSTANEVILTEST";
		Integer idCounterValue = new Integer(1000000);
		final Integer expectedIdCounterValue = new Integer(idCounterValue.intValue() + 1);
		String expectedContentId = testType + "." + expectedIdCounterValue;

		checkIDCounterValue(dbHandle, testType, idCounterValue);

		// get the current value of the idcounter
		SimpleResultProcessor proc = new SimpleResultProcessor();

		DB.query(dbHandle, "SELECT id_counter FROM " + dbHandle.getContentObjectName() + " WHERE type = " + testType, proc);
		int currentIdCounterValue = proc.getRow(1).getInt("id_counter");

		// now insert a new object with expected contentid
		Map objectData = new HashMap();

		objectData.put("name", testName);
		objectData.put("contentid", expectedContentId);
		Changeable object = ds.create(objectData, -1, false);

		ds.store(Collections.singleton(object));

		// check whether the id_counter value did NOT change
		DB.query(dbHandle, "SELECT id_counter FROM " + dbHandle.getContentObjectName() + " WHERE type = " + testType, proc);
		assertEquals("Check whether the id_counter value did not change", currentIdCounterValue, proc.getRow(1).getInt("id_counter"));
	}

	protected class SubProcessObjectCreationThread extends Thread {
		protected List contentIds;

		protected String[] cmdArray;

		protected Exception exception;

		public SubProcessObjectCreationThread(String[] cmdArray, List contentIds) {
			this.cmdArray = cmdArray;
			this.contentIds = contentIds;
		}

		public void run() {
			Process subProcess = null;

			try {
				subProcess = Runtime.getRuntime().exec(cmdArray);
				BufferedReader reader = new BufferedReader(new InputStreamReader(subProcess.getInputStream()));
				String line = reader.readLine();

				while (!CNMultiProcessObjectCreationTest.TEST_DONE.equals(line) && line != null) {
					if (line.startsWith(CNMultiProcessObjectCreationTest.ERROR_PREFIX)) {
						System.out.println(line);
					}
					if (line.startsWith(CNMultiProcessObjectCreationTest.CONTENTID_PREFIX)) {
						contentIds.add(line.substring(CNMultiProcessObjectCreationTest.CONTENTID_PREFIX.length()));
					}
					line = reader.readLine();
				}

				// finally wait for the subprocess to finish
				if (subProcess.waitFor() != 0) {
					throw new Exception("Subprocess terminated abnormaly");
				}
				System.out.println("subprocess ended");
			} catch (InterruptedException e) {
				// thread was interrupted, so kill the subprocess
				subProcess.destroy();
			} catch (Exception e) {
				exception = e;
			}
		}

		public Exception getException() {
			return exception;
		}
	}

	/**
	 * Thread class for concurrent object creation test
	 */
	protected class ObjectCreationThread extends Thread {
		protected String testName;

		protected List contentIds;

		protected Exception exception;

		/**
		 * Create instance of this thread
		 *
		 * @param testName
		 *            test name
		 * @param contentIds
		 *            list of contentids
		 */
		public ObjectCreationThread(String testName, List contentIds) {
			this.testName = testName;
			this.contentIds = contentIds;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			Map map = new HashMap();

			map.put("obj_type", "10007");
			map.put("name", testName);
			try {
				Changeable changeable = ((WriteableDatasource) ds).create(map);

				((WriteableDatasource) ds).store(Collections.singleton(changeable));
				contentIds.add(changeable.get("contentid"));
			} catch (DatasourceException e) {
				exception = e;
			}
		}

		/**
		 * Get exception (if any was thrown in the thread) or null
		 *
		 * @return exception or null
		 */
		public Exception getException() {
			return exception;
		}
	}

	/**
	 * Get the object with the given contentid from the collection of rows or null if not found
	 *
	 * @param rows
	 *            datasource rows
	 * @param contentId
	 *            contentid to find
	 * @return row or null
	 */
	protected static Resolvable getObjectById(Collection rows, String contentId) {
		if (contentId == null) {
			return null;
		}

		for (Iterator iter = rows.iterator(); iter.hasNext();) {
			Resolvable row = (Resolvable) iter.next();

			if (contentId.equals(row.get("contentid"))) {
				return row;
			}
		}

		return null;
	}
}
