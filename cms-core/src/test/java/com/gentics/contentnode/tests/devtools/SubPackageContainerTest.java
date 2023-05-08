package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.jsonToFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.model.PackageModel;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for directory names of sub package containers
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class SubPackageContainerTest {
	/**
	 * Maximum wait time for sync
	 */
	public final static int MAX_WAIT_MS = 10000;

	/**
	 * Event delay
	 */
	public final static int EVENT_DELAY = 1000;

	/**
	 * sleep time between sync checks
	 */
	public final static int WAIT_SLEEP_MS = 100;

	protected static final Map<String, Boolean> subPackageDirNames = new HashMap<>();

	static {
		subPackageDirNames.put(".", false);
		subPackageDirNames.put("..", false);
		subPackageDirNames.put("../bla", false);
		subPackageDirNames.put("one/two/three", false);
		subPackageDirNames.put(PackageSynchronizer.CONSTRUCTS_DIR, false);
		subPackageDirNames.put(PackageSynchronizer.DATASOURCES_DIR, false);
		subPackageDirNames.put(PackageSynchronizer.OBJECTPROPERTIES_DIR, false);
		subPackageDirNames.put(PackageSynchronizer.TEMPLATES_DIR, false);
		subPackageDirNames.put(PackageSynchronizer.FILES_DIR, false);
		subPackageDirNames.put("node_modules", true);
		subPackageDirNames.put("subpackages", true);
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: dir {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String dir : subPackageDirNames.keySet()) {
			data.add(new Object[] { dir });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}
	}

	@Parameter(0)
	public String dir;

	/**
	 * Test invalid subpackage directory names
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {
		String packageName = "mainpackage";

		// generate the package
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
		}

		// configure the subpackages path
		boolean valid = subPackageDirNames.get(dir);

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			PackageModel model = new PackageModel();
			model.setSubpackages(dir);
			jsonToFile(model, new File(packageDir, MainPackageSynchronizer.GENTICS_PACKAGE_JSON));
		}

		if (valid) {
			assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as(
					String.format("Container for subpackages in %s", dir)).isNotNull();
		} else {
			assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as(
					String.format("Container for subpackages in %s", dir)).isNull();
		}
	}
}
