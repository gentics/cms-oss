package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getObjectDirectoryName;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getObjects;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.loadObjects;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.FileMonitorWatcher;
import com.gentics.contentnode.devtools.IFileWatcher;
import com.gentics.contentnode.devtools.WatchServiceWatcher;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class BigPackageSyncTest {
	/**
	 * Maximum wait time for sync
	 */
	public final static int MAX_WAIT_MS = 60000;

	/**
	 * Event delay
	 */
	public final static int EVENT_DELAY = 10000;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Big Package root directory
	 */
	public static File bigPackageRoot;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: filemonitor {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Class<? extends IFileWatcher> monitorClass : Arrays.asList(FileMonitorWatcher.class, WatchServiceWatcher.class)) {
			data.add(new Object[] { monitorClass });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}
		bigPackageRoot = new File(ConfigurationValue.PACKAGES_PATH.get(), "bigpackage");

		// remove some objects to avoid naming conflicts
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			// pages
			for (Page page : t.getObjects(Page.class, DBUtils.select("SELECT id FROM page WHERE is_master = 1", DBUtils.IDS), true)) {
				page.delete(true);
			}
			// nodes
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM folder WHERE mother = 0", DBUtils.IDS), true)) {
				node.delete(true);
			}
		});
	}

	@Parameter(0)
	public Class<? extends IFileWatcher> monitorClass;

	@Before
	public void setup() throws Exception {
		// remove some objects to avoid naming conflicts
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			// templates
			for (Template template : t.getObjects(Template.class, DBUtils.select("SELECT id FROM template WHERE is_master = 1", DBUtils.IDS), true)) {
				template.delete(true);
			}
			// object property definitions
			for (ObjectTagDefinition def : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS),
					true)) {
				def.delete(true);
			}
			// constructs
			for (Construct construct : t.getObjects(Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDS), true)) {
				construct.delete(true);
			}
			// datasources
			for (Datasource datasource : t.getObjects(Datasource.class, DBUtils.select("SELECT id FROM datasource", DBUtils.IDS), true)) {
				datasource.delete(true);
			}
		});

		// configure the synchronizer to use the monitorClass
		syncContext.setMonitorClass(monitorClass);

		// create clean package directory
		bigPackageRoot.mkdirs();
	}

	/**
	 * Test creating a big package in the filesystem
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {
		File sourceDir = new File(getClass().getResource("packages/bigpackage/").toURI());

		Map<String, String> constructMap = getObjects(new File(sourceDir, "constructs"));
		Map<String, String> datasourceMap = getObjects(new File(sourceDir, "datasources"));
		Map<String, String> objectPropertyMap = getObjects(new File(sourceDir, "objectproperties"));
		Map<String, String> templateMap = getObjects(new File(sourceDir, "templates"));

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, bigPackageRoot);
		}

		// check that all objects were found
		Collection<String> synchronizedConstructs = Trx.supply(() -> loadObjects(Construct.class, constructMap.keySet(), c -> getObjectDirectoryName(c)));
		assertThat(synchronizedConstructs).as("synchronized constructs").containsOnlyElementsOf(constructMap.values());

		Collection<String> synchronizedDatasources = Trx.supply(() -> loadObjects(Datasource.class, datasourceMap.keySet(), c -> getObjectDirectoryName(c)));
		assertThat(synchronizedDatasources).as("synchronized datasources").containsOnlyElementsOf(datasourceMap.values());

		Collection<String> synchronizedObjectProperties = Trx.supply(() -> loadObjects(ObjectTagDefinition.class, objectPropertyMap.keySet(),
				c -> getObjectDirectoryName(c)));
		assertThat(synchronizedObjectProperties).as("synchronized object properties").containsOnlyElementsOf(objectPropertyMap.values());

		Collection<String> synchronizedTemplates = Trx.supply(() -> loadObjects(Template.class, templateMap.keySet(), c -> getObjectDirectoryName(c)));
		assertThat(synchronizedTemplates).as("synchronized templates").containsOnlyElementsOf(templateMap.values());
	}
}
