package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.DatasourceSynchronizer;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for synchronizing datasource values
 */
public class DatasourceValueSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private PackageSynchronizer pack;

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);
		Synchronizer.disable();

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	/**
	 * Test that dsid's are preserved when synchronizing with devtool packages
	 * @throws NodeException
	 */
	@Test
	public void testDsId() throws NodeException {
		Datasource ds = Trx.supply(() -> createDatasource("Test Datasource", Arrays.asList("one", "two", "three")));
		GlobalId globalDsId = Trx.execute(Datasource::getGlobalId, ds);

		try (Trx trx = new Trx()) {
			ds = update(ds, update -> {
				update.getEntries().remove(1);
			});
			trx.success();
		}

		List<Integer> dsIdList = Trx.execute(d -> d.getEntries().stream().map(DatasourceEntry::getDsid).collect(Collectors.toList()), ds);

		// add to package
		Trx.consume(sync -> pack.synchronize(sync, true), ds);

		// delete the datasource
		Trx.consume(delete -> {
			update(delete, update -> {
				update.delete(true);
			});
		}, ds);

		// sync from FS
		assertThat(Trx.supply(() -> pack.syncAllFromFilesystem(Datasource.class))).as("number of synchronized datasources").isEqualTo(1);

		// reload datasource
		ds = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Datasource.class, globalDsId);
		});

		// assert
		List<Integer> syncedDsIdList = Trx.execute(d -> d.getEntries().stream().map(DatasourceEntry::getDsid).collect(Collectors.toList()), ds);
		assertThat(syncedDsIdList).as("Entry dsIds").containsExactlyElementsOf(dsIdList);
	}

	/**
	 * Test that synchronizing an entry with dsid 0 from a devtool package preserves the dsid
	 * @throws NodeException
	 */
	@Test
	public void testOldDsId() throws NodeException {
		Datasource ds = supply(() -> createDatasource("Test Datasource", Arrays.asList("one", "two")));
		GlobalId globalDsId = execute(Datasource::getGlobalId, ds);

		consume(id -> {
			DBUtils.updateWithPK("datasource_value", "id", "dsid = ?", new Object[] {0}, "datasource_id = ? AND dskey = ?", new Object[] {id, "one"});
			DBUtils.updateWithPK("datasource_value", "id", "dsid = ?", new Object[] {1}, "datasource_id = ? AND dskey = ?", new Object[] {id, "two"});
			TransactionManager.getCurrentTransaction().clearNodeObjectCache();
		}, ds.getId());

		ds = execute(Datasource::reload, ds);

		List<Integer> dsIdList = execute(d -> d.getEntries().stream().map(DatasourceEntry::getDsid).collect(Collectors.toList()), ds);

		// add to package
		consume(sync -> pack.synchronize(sync, true), ds);

		// delete the datasource
		consume(delete -> {
			update(delete, update -> {
				update.delete(true);
			});
		}, ds);

		// sync from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(Datasource.class))).as("number of synchronized datasources").isEqualTo(1);

		// reload datasource
		ds = supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Datasource.class, globalDsId);
		});

		// assert
		List<Integer> syncedDsIdList = execute(d -> d.getEntries().stream().map(DatasourceEntry::getDsid).collect(Collectors.toList()), ds);
		assertThat(syncedDsIdList).as("Entry dsIds").containsExactlyElementsOf(dsIdList);
	}

	/**
	 * Test that when synchronizing from a devtool package without dsid's the generated dsid's are greater than old dsid's
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testOldPackage() throws NodeException, IOException {
		// create a datasource with fixed global ID, having two entries (will have dsid's 1, 2)
		GlobalId globalDsId = new GlobalId("8371.d9a4a0c1-43fa-11e9-a383-0a58ac1002e6");
		Datasource ds = supply(t -> {
			Map<String, String> data = new HashMap<>();
			data.put("three", "three");
			data.put("four", "four");
			Datasource datasource = t.createObject(Datasource.class);
			datasource.setSourceType(SourceType.staticDS);
			datasource.setName("Test Datasource");
			datasource.setGlobalId(globalDsId);
			fillDatasource(datasource, data);

			return datasource.reload();
		});

		// copy the old datasource file into the devtool package (contains two other entries without dsid's)
		try (InputStream dsStream = getClass().getResourceAsStream("datasource.json")) {
			File packageDir = pack.getPackagePath().toFile();
			File datasourcesDir = new File(packageDir, PackageSynchronizer.DATASOURCES_DIR);
			File dsDir = new File(datasourcesDir, "Test Datasource");
			File structureFile = new File(dsDir, DatasourceSynchronizer.STRUCTURE_FILE);
			FileUtils.copyInputStreamToFile(dsStream, structureFile);
		}

		// sync from filesystem, this will remove the two original entries and create the two from the file
		operate(() -> pack.syncAllFromFilesystem(Datasource.class));

		// reload datasource to get new data
		ds = execute(Datasource::reload, ds);

		// assert that the new entries get 3 and 4 as dsid
		List<Integer> syncedDsIdList = execute(d -> d.getEntries().stream().map(DatasourceEntry::getDsid).collect(Collectors.toList()), ds);
		assertThat(syncedDsIdList).as("Entry dsIds").containsExactly(3, 4);
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}
}
