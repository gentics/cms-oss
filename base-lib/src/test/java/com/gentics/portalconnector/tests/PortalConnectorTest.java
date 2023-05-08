package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class PortalConnectorTest extends AbstractLegacyNavigationDumpTest {
	private final static String TEST_CONTENTID = "10007.7865";
	private NodeLogger logger;

	@Before
	public void setUp() throws Exception {
		prepareTestDatabase(getClass().getSimpleName());

		this.logger = NodeLogger.getNodeLogger(this.getClass());
	}

	@After
	public void tearDown() throws Exception {
		removeTestDatabase();
	}

	public void testPortalConnectorWithRenamedTable() throws URISyntaxException, IOException {
		System.setProperty("com.gentics.portalnode.confpath", new File(new URI(this.getClass().getResource("cache.ccf").toString())).getParent());

		Properties handleProperties = getTestDatabase().getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "true");
		dsProperties.put("autorepair", "false");
		dsProperties.put("table.contentattributetype", "contentattributetype_renamed");
		Datasource ds = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		// ds must be null because sanity check failed
		assertNull(ds);
	}

	@Test
	public void testPortalConnectorWithCache() throws URISyntaxException, IOException, DatasourceNotAvailableException, SQLUtilException,
				JDBCMalformedURLException {
		System.setProperty("com.gentics.portalnode.confpath", new File(new URI(this.getClass().getResource("cache.ccf").toString())).getParent());
		insertDumpIntoDatabase();

		Properties handleProperties = getTestDatabase().getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("cache", "true");
		dsProperties.put("sanitycheck", "false");
		Datasource ds = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		// Making sure nothing is cached yet.
		CompositeCacheManager cacheManager = CompositeCacheManager.getInstance();
		CompositeCache resultsCache = cacheManager.getCache("gentics-portal-contentrepository-results");
		CompositeCache objectsCache = cacheManager.getCache("gentics-portal-contentrepository-objects");
		CompositeCache attsCache = cacheManager.getCache("gentics-portal-contentrepository-atts");

		resultsCache.removeAll();
		objectsCache.removeAll();
		attsCache.removeAll();

		int objectHitCount = objectsCache.getHitCountRam();
		int attsHitCount = attsCache.getHitCountRam();
		Resolvable contentobject = PortalConnectorFactory.getContentObject(TEST_CONTENTID, ds);

		assertEquals("Portal.Server.Production", contentobject.get("name"));
		Resolvable contentobject2 = PortalConnectorFactory.getContentObject(TEST_CONTENTID, ds);

		contentobject.get("name");
		contentobject2.get("name");
		contentobject.get("name");
		contentobject2.get("editor");
		contentobject.get("editor");
		assertEquals("We should have one object - cache hit already", 1, objectsCache.getHitCountRam() - objectHitCount);
		assertEquals("We should have 4 attribute cache hits.", 4, attsCache.getHitCountRam() - attsHitCount);
	}

	@Test
	public void testMultipleDatasources() throws Exception {
		insertDumpIntoDatabase();
		System.setProperty("com.gentics.portalnode.confpath", new File(new URI(this.getClass().getResource("cache.ccf").toString())).getParent());

		Properties handleProperties = getTestDatabase().getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("cache", "true");
		dsProperties.put("cache.syncchecking", "true");
		dsProperties.put("sanitycheck", "false");
		Datasource ds = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		Datasource ds2 = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		Thread.sleep(1000);
	}

	public void testMultiThreading() {
		Runnable run = new Runnable() {

			public void run() {
				Properties handleProperties = getTestDatabase().getSettings();

				logger.info("running");
				try {

					handleProperties.setProperty("maxActive", "1");
					Map dsProperties = new HashMap(handleProperties);

					dsProperties.put("sanitycheck", "false");
					Datasource ds = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

					for (int i = 0; i < 10; i++) {
						Thread.sleep(1000);
						Resolvable contentobject = PortalConnectorFactory.getContentObject(TEST_CONTENTID, ds);

						assertEquals("Portal.Server.Production", contentobject.get("name"));
					}

				} catch (Exception e) {
					logger.error("error .. :(", e);
					throw new RuntimeException(e);
				}
			}

		};

		Thread[] threads = new Thread[3];

		for (int i = 0; i < 3; i++) {
			threads[i] = new Thread(run);
			threads[i].start();
		}

		boolean running = true;

		while (running) {
			try {
				Thread.sleep(1000);
				running = false;
				for (int i = 0; i < 3; i++) {
					if (threads[i].isAlive()) {
						running = true;
					}
				}
			} catch (InterruptedException e) {
				logger.error("Error while waiting for threads to quit.", e);
			}
		}

	}

}
