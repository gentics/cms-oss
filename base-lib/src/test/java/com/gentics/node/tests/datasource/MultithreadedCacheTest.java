package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.DatasourceType;
import com.gentics.api.portalnode.connector.DuplicateIdException;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentAttributeImpl;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import org.junit.experimental.categories.Category;

/**
 * Test cases for multithreaded cache access
 */
@Category(BaseLibTest.class)
public class MultithreadedCacheTest {
	public final static String HANDLE_ID = "testhandle";

	public final static String DATASOURCE_ID = "testds";

	public final static int OBJECT_TYPE = 1;

	public final static int OBJECT_ID = 1;

	public final static String CONTENTID = String.format("%d.%d", OBJECT_TYPE, OBJECT_ID);

	public final static List<String> ATTRIBUTE_NAMES = Arrays.asList("attribute0", "attribute1", "attribute2", "attribute3", "attribute4", "attribute5",
			"attribute6", "attribute7", "attribute8", "attribute9");

	public final static int NUM_READ_ITERATIONS = 100000;

	public final static int NUM_READ_THREADS = 100;

	public final static int NUM_UPDATE_ITERATIONS = 10000;

	public final static int NUM_UPDATE_THREADS = 100;

	protected final static Random rand = new Random();

	/**
	 * Create datasource and object type
	 * @throws DuplicateIdException
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws DuplicateIdException, NodeException {
		Map<String, String> handleProperties = new HashMap<String, String>();
		handleProperties.put("type", "jdbc");
		handleProperties.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProperties.put("url", "jdbc:hsqldb:mem:" + MultithreadedCacheTest.class.getSimpleName());
		handleProperties.put("shutDownCommand", "SHUTDOWN");
		PortalConnectorFactory.registerHandle(HANDLE_ID, HandleType.sql, handleProperties);

		Map<String, String> dsProperties = new HashMap<String, String>();
		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("cache", "true");
		PortalConnectorFactory.registerDatasource(DATASOURCE_ID, DatasourceType.contentrepository, dsProperties, Arrays.asList(HANDLE_ID));

		CNWriteableDatasource ds = PortalConnectorFactory.createDatasource(CNWriteableDatasource.class, DATASOURCE_ID);
		ObjectTypeBean type = new ObjectTypeBean(OBJECT_TYPE, "testtype", false);
		for (String name : ATTRIBUTE_NAMES) {
			type.addAttributeType(new ObjectAttributeBean(name, GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, OBJECT_TYPE, 0, null, null, null, false, false));
		}
		ObjectManagementManager.saveObjectType(ds, type, true, true);
	}

	/**
	 * Get random attribute name
	 * @return random attribute name
	 */
	protected static String getRandomAttributeName() {
		return ATTRIBUTE_NAMES.get(rand.nextInt(ATTRIBUTE_NAMES.size()));
	}

	/**
	 * Test accessing and updating attribute caches in multiple threads
	 * @throws NodeException
	 * @throws PortalCacheException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testAttributeCache() throws NodeException, PortalCacheException, InterruptedException, ExecutionException {
		final CNWriteableDatasource ds = PortalConnectorFactory.createDatasource(CNWriteableDatasource.class, DATASOURCE_ID);
		assertNotNull("Datasource must not be null", ds);

		Map<String, Object> data = new HashMap<>();
		data.put("contentid", CONTENTID);
		final GenticsContentObject contentObject = (GenticsContentObject) ds.create(data, -1, false);

		ExecutorService cacheUpdateService = Executors.newCachedThreadPool();
		List<Future<Boolean>> updateFutures = new ArrayList<>();
		for (int i = 0; i < NUM_UPDATE_THREADS; i++) {
			updateFutures.add(cacheUpdateService.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					for (int i = 0; i < NUM_UPDATE_ITERATIONS; i++) {
						for (String attributeName : ATTRIBUTE_NAMES) {
							GenticsContentAttributeImpl attribute = new GenticsContentAttributeImpl(contentObject, ds.getHandle().getDBHandle(),
									attributeName, new Object[] {"bla"}, GenticsContentAttribute.ATTR_TYPE_TEXT, false, false);
							GenticsContentFactory.cacheAttribute(ds, contentObject, attribute);
						}
						for (String attributeName : ATTRIBUTE_NAMES) {
							GenticsContentFactory.uncacheAttribute(ds, contentObject, attributeName);
						}
					}
					return true;
				}
			}));
		}

		ExecutorService cacheReadService = Executors.newCachedThreadPool();
		List<Future<Boolean>> readFutures = new ArrayList<>();
		for (int i = 0; i < NUM_READ_THREADS; i++) {
			readFutures.add(cacheReadService.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					for (int i = 0; i < NUM_READ_ITERATIONS; i++) {
						GenticsContentFactory.getCachedAttribute(ds, contentObject, getRandomAttributeName());
					}
					return true;
				}
			}));
		}

		for (Future<Boolean> future : readFutures) {
			assertTrue("Job should return true", future.get());
		}
		for (Future<Boolean> future : updateFutures) {
			assertTrue("Job should return true", future.get());
		}
	}
}
