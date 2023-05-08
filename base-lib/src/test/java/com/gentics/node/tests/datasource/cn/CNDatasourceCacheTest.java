/*
 * @author norbert
 * @date 30.05.2008
 * @version $Id: CNDatasourceCacheTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.node.tests.crsync.DBCon;
import com.gentics.node.testutils.NodeTestUtils;
import com.gentics.testutils.GenericTestUtils;
import org.junit.experimental.categories.Category;

/**
 * @author norbert
 */
@Category(BaseLibTest.class)
public class CNDatasourceCacheTest extends AbstractVersioningTest {

	private NodeLogger logger;

	private CNDatasource ds;

	public final static String TESTEDOBJECT = "10007.14580";

	public final static String CUSTOM_CACHED_ATTRIBUTE = "description";

	public final static String CUSTOM_CACHE_REGION = "description-region";

	public final static String DEFAULT_CACHED_ATTRIBUTE = "name";

	public final static String NOT_CACHED_ATTRIBUTE = "content";

	private DBCon db;

	@Before
	public void setUp() throws Exception {
		logger = NodeLogger.getNodeLogger(this.getClass());
		GenericTestUtils.initConfigPathForCache();

		// for usage of jndi datasources:
		// handleProperties.load(DatasourceAccess.class.getResourceAsStream("jndi-handle.properties"));

		// turn on the cache
		Map dsProps = new HashMap(handleProperties);

		dsProps.put("cache", "true");
		dsProps.put("cache.attribute." + CUSTOM_CACHED_ATTRIBUTE + ".region", CUSTOM_CACHE_REGION);
		dsProps.put("cache.attribute." + NOT_CACHED_ATTRIBUTE, "false");
		CNWriteableDatasource dsw = (CNWriteableDatasource) NodeTestUtils.createWriteableDatasource(handleProperties, dsProps);

		ds = (CNDatasource) dsw;
		ds.clearCaches();

		db = new DBCon(handleProperties.getProperty("driverClass"), handleProperties.getProperty("url"), handleProperties.getProperty("username"),
				handleProperties.getProperty("passwd"));

		logger.info("doing tests on " + ds.getHandlePool().getHandle());

		prepareContentRepository(db, dsw);
		insertObject(dsw);
	}

	private void insertObject(CNWriteableDatasource ds) throws DatasourceException {
		Map attrs = new HashMap();
		Changeable co = null;

		attrs.clear();
		attrs.put("contentid", TESTEDOBJECT);
		attrs.put("name", "Ordner");
		attrs.put("description", "Ordner");
		attrs.put("editor", "rb");
		attrs.put("node_id", "1");
		attrs.put("permissions", Arrays.asList(new String[] { "11", "22" }));
		attrs.put("content", "bla");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);
	}

	/**
	 * store singe changeable (e.g. contentobject) c in datasource ds
	 *
	 * @param ds
	 *            datasource to store c in
	 * @param c
	 *            changeable (e.g. contentobject)
	 * @throws DatasourceException
	 */
	private void storeSingleChangeable(CNWriteableDatasource ds, Changeable c) throws DatasourceException {

		ds.store(Collections.singleton(c));
	}

	/**
	 * setup and prefill database
	 *
	 * @param db
	 *            db to setup (identical to ds)
	 * @param ds
	 *            datasource to setup (identical to db)
	 * @throws Exception
	 */
	private void prepareContentRepository(DBCon db, CNWriteableDatasource ds) throws Exception {

		logger.info("Setup of ContentAttributeTypes");
		DBHandle dbh = GenticsContentFactory.getHandle(ds);

		// set up contentobject, contentattributetypes (folders)
		ObjectManagementManager.createNewObject(dbh, "" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder");

		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("contentid", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("description", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null,
				null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("editor", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0,
				null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("datum", GenticsContentAttribute.ATTR_TYPE_DATE, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("euro", GenticsContentAttribute.ATTR_TYPE_DOUBLE, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null,
				false, false),
				true);

		// set up contentattributetypes (pages)
		ObjectManagementManager.createNewObject(dbh, "" + GenticsContentObject.OBJ_TYPE_PAGE, "page");

		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("contentid", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("description", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null,
				null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("editor", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_PAGE, 0,
				null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("folder_id", GenticsContentAttribute.ATTR_TYPE_OBJ, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE,
				GenticsContentObject.OBJ_TYPE_FOLDER, null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("content", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("datum", GenticsContentAttribute.ATTR_TYPE_DATE, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("binary", GenticsContentAttribute.ATTR_TYPE_BLOB, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("optimizedclob", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, true, "quick_optimizedclob", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("multivalueclob", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null,
				null, null, false, false),
				true);
	}

	/**
	 * Test the object cache
	 *
	 * @throws Exception
	 */
	@Test
	public void testObjectCache() throws Exception {
		// access the object (should be cached now)
		PortalConnectorFactory.getContentObject(TESTEDOBJECT, ds);

		// check whether the object is found in the cache now
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		CompositeCache cache = cacheManager.getCache(GenticsContentFactory.OBJECTCACHEREGION);

		assertEquals("Check whether the object is cached in region {" + GenticsContentFactory.OBJECTCACHEREGION + "} now", 1, cache.getSize());

		// now uncache the object
		ds.clearCaches();

		// check whether the cache is cleared now
		assertEquals("Check whether cache region {" + GenticsContentFactory.OBJECTCACHEREGION + "} is empty now", 0, cache.getSize());
	}

	/**
	 * Test a custom cached attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testCustomAttributeCacheRegion() throws Exception {
		// access the object and the default cached attribute
		Resolvable resolvable = PortalConnectorFactory.getContentObject(TESTEDOBJECT, ds);

		assertNotNull(resolvable);
		resolvable.get(CUSTOM_CACHED_ATTRIBUTE);

		// check whether the object is found in the cache now
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		CompositeCache cache = cacheManager.getCache(CUSTOM_CACHE_REGION);

		assertEquals("Check whether the attribute is cached in region {" + CUSTOM_CACHE_REGION + "} now", 1, cache.getSize());

		// now uncache the object
		ds.clearCaches();

		// check whether the cache is cleared now
		assertEquals("Check whether cache region {" + CUSTOM_CACHE_REGION + "} is empty now", 0, cache.getSize());
	}

	/**
	 * Test the default attribute cache
	 *
	 * @throws Exception
	 */
	@Test
	public void testDefaultAttributeCache() throws Exception {
		// access the object and the default cached attribute
		PortalConnectorFactory.getContentObject(TESTEDOBJECT, ds).get(DEFAULT_CACHED_ATTRIBUTE);

		// check whether the object is found in the cache now
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		CompositeCache cache = cacheManager.getCache(GenticsContentFactory.ATTRIBUTESCACHEREGION);

		assertEquals("Check whether the attribute is cached in region {" + GenticsContentFactory.ATTRIBUTESCACHEREGION + "} now", 1, cache.getSize());

		// now uncache the object
		ds.clearCaches();

		// check whether the cache is cleared now
		assertEquals("Check whether cache region {" + GenticsContentFactory.ATTRIBUTESCACHEREGION + "} is empty now", 0, cache.getSize());
	}

	/**
	 * Test an uncached attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testUncachedAttribute() throws Exception {
		// access the object and the default cached attribute
		Resolvable object = PortalConnectorFactory.getContentObject(TESTEDOBJECT, ds);
		assertNotNull("Uncached attribute must not be empty", object.get(NOT_CACHED_ATTRIBUTE));

		// check whether the object is found in the cache now
		assertNull("Uncached attribute must not be found in cache", GenticsContentFactory.getCachedAttribute(ds, (GenticsContentObject) object, NOT_CACHED_ATTRIBUTE));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		db.closeCON();
		ds.getHandle().close();
		ds.getHandlePool().close();
	}

}
