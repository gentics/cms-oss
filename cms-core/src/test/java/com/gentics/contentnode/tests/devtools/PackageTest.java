package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.jsonToFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.gentics.contentnode.devtools.FileMonitorWatcher;
import com.gentics.contentnode.devtools.IFileWatcher;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.WatchServiceWatcher;
import com.gentics.contentnode.devtools.model.PackageModel;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling packages
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageTest {
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

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

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
	}

	@Parameter(0)
	public Class<? extends IFileWatcher> monitorClass;

	@Before
	public void setup() throws Exception {
		syncContext.setMonitorClass(monitorClass);
	}

	/**
	 * Test adding packages in the FS
	 * @throws Exception
	 */
	@Test
	public void testAddPackageFS() throws Exception {
		List<String> packageNames = Arrays.asList("one", "two", "three");
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			for (String name : packageNames) {
				File packageDir = new File(syncContext.getPackagesRoot(), name);
				assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
			}
		}
		assertThat(Synchronizer.getPackages()).as("Packages").containsOnlyElementsOf(packageNames);
	}

	/**
	 * Test adding packages as symlinks
	 * @throws Exception
	 */
	@Test
	public void testAddPackageSymlink() throws Exception {
		List<String> packageNames = Arrays.asList("one", "two", "three");
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			for (String name : packageNames) {
				File tmpDir = new File(new File(ConfigurationValue.DBFILES_PATH.get()).getParentFile(),
						UUID.randomUUID().toString());
				assertThat(tmpDir.mkdir()).as("Dir creation success").isTrue();

				File packageDir = new File(syncContext.getPackagesRoot(), name);
				Files.createSymbolicLink(packageDir.toPath(), tmpDir.toPath());
			}
		}
		assertThat(Synchronizer.getPackages()).as("Packages").containsOnlyElementsOf(packageNames);
	}

	/**
	 * Test removing packages in the FS
	 * @throws Exception
	 */
	@Test
	public void testRemovePackageFS() throws Exception {
		testAddPackageFS();

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			File packageDir = new File(syncContext.getPackagesRoot(), "two");
			assertThat(packageDir.delete()).as("Deletion success").isTrue();
		}

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly("one", "three");
	}

	/**
	 * Test renaming packages in the FS
	 * @throws Exception
	 */
	@Test
	public void testRenamePackageFS() throws Exception {
		testAddPackageFS();

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			File packageDir = new File(syncContext.getPackagesRoot(), "two");
			assertThat(packageDir.renameTo(new File(syncContext.getPackagesRoot(), "four"))).as("Dir rename success").isTrue();
		}

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly("one", "three", "four");
	}

	/**
	 * Test starting the synchronizer with existing packages
	 * @throws Exception
	 */
	@Test
	public void testStartWithExistingPackages() throws Exception {
		Synchronizer.stop();

		List<String> packageNames = Arrays.asList("one", "two", "three");
		for (String name : packageNames) {
			File packageDir = new File(syncContext.getPackagesRoot(), name);
			assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
		}
		Synchronizer.start();
		assertThat(Synchronizer.getPackages()).as("Packages").containsOnlyElementsOf(packageNames);
	}

	/**
	 * Test accessing the readme file in a package
	 * @throws Exception
	 */
	@Test
	public void testReadme() throws Exception {
		File packageDir = new File(syncContext.getPackagesRoot(), "test");
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
		}

		File readMeFile = new File(packageDir, MainPackageSynchronizer.README_FILE);
		FileUtils.write(readMeFile, "Package description");

		PackageSynchronizer synchronizer = Synchronizer.getPackage("test");
		assertThat(synchronizer).as("Package").isNotNull();
		assertThat(synchronizer.getDescription()).as("Package description").isEqualTo("<p>Package description</p>\n");
	}

	/**
	 * Test adding packages in the CMS
	 * @throws Exception
	 */
	@Test
	public void testAddPackageCMS() throws Exception {
		List<String> packageNames = Arrays.asList("one", "two", "three");

		for (String name : packageNames) {
			Synchronizer.addPackage(name);
		}

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnlyElementsOf(packageNames);

		for (String name : packageNames) {
			File packageDir = new File(syncContext.getPackagesRoot(), name);
			assertThat(packageDir).as(String.format("Directory of package %s", name)).isDirectory();
		}
	}

	/**
	 * Test removing packages in the CMS
	 * @throws Exception
	 */
	@Test
	public void testRemovePackageCMS() throws Exception {
		testAddPackageCMS();

		Synchronizer.removePackage("two");

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly("one", "three");

		List<String> packageDirectories = Arrays.asList(syncContext.getPackagesRoot().listFiles(file -> file.isDirectory() && !file.getName().startsWith(".")))
				.stream().map(File::getName).collect(Collectors.toList());
		assertThat(packageDirectories).as("Package directories").containsOnly("one", "three");
	}

	/**
	 * Test changing the configuration for subpackages
	 * @throws Exception
	 */
	@Test
	public void testSubPackageContainer() throws Exception {
		String subPackagesDirName = "node_modules";
		String packageName = "mainpackage";

		// generate the package
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
		}

		// package must exist, but must not have a container for subpackages
		assertThat(Synchronizer.getPackage(packageName)).as("Package").isNotNull();
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Container for subpackages").isNull();

		// configure the subpackages path
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			PackageModel model = new PackageModel();
			model.setSubpackages(subPackagesDirName);
			jsonToFile(model, new File(packageDir, MainPackageSynchronizer.GENTICS_PACKAGE_JSON));
		}

		// package must have a subpackage container now
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Container for subpackages").isNotNull();
	}

	/**
	 * Test adding sub packages in the FS
	 * @throws Exception
	 */
	@Test
	public void testAddSubPackageFS() throws Exception {
		String subPackagesDirName = "node_modules";
		String packageName = "mainpackage";

		// generate the package and configure the subpackages path
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		File subPackagesRoot = new File(packageDir, subPackagesDirName);
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
			PackageModel model = new PackageModel();
			model.setSubpackages(subPackagesDirName);
			jsonToFile(model, new File(packageDir, MainPackageSynchronizer.GENTICS_PACKAGE_JSON));
		}

		List<String> packageNames = Arrays.asList("one", "two", "three");
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			for (String name : packageNames) {
				File subPackageDir = new File(subPackagesRoot, name);
				assertThat(subPackageDir.mkdirs()).as("Dir creation success").isTrue();
			}
		}
		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly(packageName);
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Subpackage container").isNotNull();
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer().getPackages()).as("Subpackages").containsOnlyElementsOf(packageNames);
	}

	/**
	 * Test removing subpackages packages in the FS
	 * @throws Exception
	 */
	@Test
	public void testRemoveSubPackageFS() throws Exception {
		String packageName = "mainpackage";
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		String subPackagesDirName = "node_modules";
		File subPackagesRoot = new File(packageDir, subPackagesDirName);

		testAddSubPackageFS();

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			File subPackageDir = new File(subPackagesRoot, "two");
			assertThat(subPackageDir.delete()).as("Deletion success").isTrue();
		}

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly(packageName);
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Subpackage container").isNotNull();
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer().getPackages()).as("Subpackages").containsOnly("one", "three");
	}

	/**
	 * Test renaming subpackages in the FS
	 * @throws Exception
	 */
	@Test
	public void testRenameSubPackageFS() throws Exception {
		String packageName = "mainpackage";
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		String subPackagesDirName = "node_modules";
		File subPackagesRoot = new File(packageDir, subPackagesDirName);

		testAddSubPackageFS();

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			File subPackageDir = new File(subPackagesRoot, "two");
			assertThat(subPackageDir.renameTo(new File(subPackagesRoot, "four"))).as("Dir rename success").isTrue();
		}

		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly(packageName);
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Subpackage container").isNotNull();
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer().getPackages()).as("Subpackages").containsOnly("one", "three", "four");
	}

	/**
	 * Test starting the synchronizer with existing subpackages
	 * @throws Exception
	 */
	@Test
	public void testStartWithExistingSubPackages() throws Exception {
		Synchronizer.stop();

		String subPackagesDirName = "node_modules";
		String packageName = "mainpackage";

		// generate the package and configure the subpackages path
		File packageDir = new File(syncContext.getPackagesRoot(), packageName);
		File subPackagesRoot = new File(packageDir, subPackagesDirName);

		assertThat(packageDir.mkdir()).as("Dir creation success").isTrue();
		PackageModel model = new PackageModel();
		model.setSubpackages(subPackagesDirName);
		jsonToFile(model, new File(packageDir, MainPackageSynchronizer.GENTICS_PACKAGE_JSON));

		List<String> packageNames = Arrays.asList("one", "two", "three");
		for (String name : packageNames) {
			File subPackageDir = new File(subPackagesRoot, name);
			assertThat(subPackageDir.mkdirs()).as("Dir creation success").isTrue();
		}
		Synchronizer.start();
		assertThat(Synchronizer.getPackages()).as("Packages").containsOnly(packageName);
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer()).as("Subpackage container").isNotNull();
		assertThat(Synchronizer.getPackage(packageName).getSubPackageContainer().getPackages()).as("Subpackages").containsOnlyElementsOf(packageNames);
	}
}
