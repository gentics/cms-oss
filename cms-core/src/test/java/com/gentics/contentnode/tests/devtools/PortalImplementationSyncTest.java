package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.AbstractSynchronizer;
import com.gentics.contentnode.devtools.FileMonitorWatcher;
import com.gentics.contentnode.devtools.IFileWatcher;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.PortalImplementationSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.WatchServiceWatcher;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.rest.model.PortalImplementationModel.Type;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Test cases for synchronizing Portal Implementations (including their files) between the CMS and devtool packages
 */
@GCNFeature(set = { Feature.DEVTOOLS })
@RunWith(value = Parameterized.class)
public class PortalImplementationSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	/**
	 * Maximum wait time for sync
	 */
	public final static int MAX_WAIT_MS = 10000;

	/**
	 * Delay for sync events
	 */
	public final static int EVENT_DELAY = 1000;

	/**
	 * Contents of a PNG file (PNG signature followed by an IHDR chunk header), as ISO-8859-1 string (to be lossless)
	 */
	public final static String PNG_CONTENTS = new String(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0x0d, 'I', 'H', 'D', 'R' },
			StandardCharsets.ISO_8859_1);

	/**
	 * Counter for unique keywords
	 */
	protected static AtomicInteger counter = new AtomicInteger();

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected PackageSynchronizer pack;

	/**
	 * Get the test parameters
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

	/**
	 * Tested file watcher implementation
	 */
	@Parameter(0)
	public Class<? extends IFileWatcher> monitorClass;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}
	}

	@Before
	public void setup() throws NodeException {
		syncContext.setMonitorClass(monitorClass);
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	/**
	 * Test synchronizing a Portal Implementation with files into the package
	 * @throws Exception
	 */
	@Test
	public void testSyncToFilesystem() throws Exception {
		Synchronizer.disable();
		PortalImplementation impl = createWithFiles();

		operate(() -> pack.synchronize(impl, true));

		File implDir = implDir(impl);
		@SuppressWarnings("unchecked")
		Map<String, Object> structure = new ObjectMapper().readValue(new File(implDir, AbstractSynchronizer.STRUCTURE_FILE), Map.class);
		assertThat(structure).as("Structure file").containsOnly(
				Map.entry("globalId", supply(() -> impl.getGlobalId().toString())),
				Map.entry("keyword", impl.getKeyword()),
				Map.entry("name", impl.getName()),
				Map.entry("description", "Description"),
				Map.entry("type", "PHP"));

		assertThat(filesInPackage(impl)).as("Files in package").containsExactlyEntriesOf(new TreeMap<>(Map.of(
				"index.php", "<?php echo 'index'; ?>",
				"src/a.php", "<?php echo 'a'; ?>",
				"src/lib/empty.txt", "",
				"assets/style.css", "body {}")));
	}

	/**
	 * Test that changes in the files of a Portal Implementation are synchronized into the package
	 * @throws Exception
	 */
	@Test
	public void testFileChangesSynchronized() throws Exception {
		PortalImplementation impl = createWithFiles();
		operate(() -> pack.synchronize(impl, true));

		// change contents of an existing file, create a new file
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			operate(t -> {
				PortalImplementationFile file = t.getObject(impl.getFile("src/a.php"), true);
				file.setContents(stream("<?php echo 'changed'; ?>"));
				file.save();
				createFile(impl, "src/new.php", "<?php echo 'new'; ?>");
			});
		}
		assertThat(filesInPackage(impl)).as("Files in package after change").containsExactlyEntriesOf(new TreeMap<>(Map.of(
				"index.php", "<?php echo 'index'; ?>",
				"src/a.php", "<?php echo 'changed'; ?>",
				"src/new.php", "<?php echo 'new'; ?>",
				"src/lib/empty.txt", "",
				"assets/style.css", "body {}")));

		// move a file and delete a file
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			operate(t -> {
				PortalImplementationFile file = t.getObject(impl.getFile("src/lib/empty.txt"), true);
				file.setPath("other/empty.txt");
				file.save();
				t.getObject(impl.getFile("assets/style.css"), true).delete();
			});
		}
		assertThat(filesInPackage(impl)).as("Files in package after move and delete").containsExactlyEntriesOf(new TreeMap<>(Map.of(
				"index.php", "<?php echo 'index'; ?>",
				"src/a.php", "<?php echo 'changed'; ?>",
				"src/new.php", "<?php echo 'new'; ?>",
				"other/empty.txt", "")));
		assertThat(new File(filesDir(impl), "src/lib")).as("Empty folder").doesNotExist();
		assertThat(new File(filesDir(impl), "assets")).as("Empty folder").doesNotExist();
	}

	/**
	 * Test that deleting a single file in the CMS (without any other change) is synchronized into the package
	 * @throws Exception
	 */
	@Test
	public void testFileDeletionSynchronized() throws Exception {
		PortalImplementation impl = createWithFiles();
		addToPackage(impl);

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			operate(t -> t.getObject(impl.getFile("src/a.php"), true).delete());
		}
		assertThat(filesInPackage(impl)).as("Files in package after deletion").containsOnlyKeys("index.php", "src/lib/empty.txt", "assets/style.css");
	}

	/**
	 * Test that deleting a folder (all files in it) in the CMS is synchronized into the package
	 * @throws Exception
	 */
	@Test
	public void testFolderDeletionSynchronized() throws Exception {
		PortalImplementation impl = createWithFiles();
		addToPackage(impl);

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			operate(t -> {
				for (PortalImplementationFile file : impl.getFiles("src")) {
					t.getObject(file, true).delete();
				}
			});
		}
		assertThat(filesInPackage(impl)).as("Files in package after deletion").containsOnlyKeys("index.php", "assets/style.css");
		assertThat(new File(filesDir(impl), "src")).as("Deleted folder").doesNotExist();
	}

	/**
	 * Test that deleting a file in the package is synchronized into the CMS
	 * @throws Exception
	 */
	@Test
	public void testFileDeletionInFilesystemSynchronized() throws Exception {
		PortalImplementation impl = createWithFiles();
		addToPackage(impl);

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			Files.delete(new File(filesDir(impl), "src/a.php").toPath());
		}
		operate(t -> assertThat(filesInCms(t.getObject(PortalImplementation.class, impl.getId()))).as("Files in CMS after deletion")
				.containsOnlyKeys("index.php", "src/lib/empty.txt", "assets/style.css"));
	}

	/**
	 * Test that deleting a folder in the package is synchronized into the CMS
	 * @throws Exception
	 */
	@Test
	public void testFolderDeletionInFilesystemSynchronized() throws Exception {
		PortalImplementation impl = createWithFiles();
		addToPackage(impl);

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.deleteDirectory(new File(filesDir(impl), "src"));
		}
		operate(t -> assertThat(filesInCms(t.getObject(PortalImplementation.class, impl.getId()))).as("Files in CMS after deletion")
				.containsOnlyKeys("index.php", "assets/style.css"));
	}

	/**
	 * Test that deleting the Portal Implementation removes it from the package
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		PortalImplementation impl = createWithFiles();
		operate(() -> pack.synchronize(impl, true));
		File implDir = implDir(impl);
		assertThat(implDir).as("Directory in package").isDirectory();

		operate(t -> t.getObject(impl, true).delete());

		assertThat(implDir).as("Directory in package after deletion").doesNotExist();
		assertThat(supply(() -> pack.getObjects(PortalImplementation.class))).as("Portal Implementations in package").isEmpty();
	}

	/**
	 * Test creating a Portal Implementation from the package
	 * @throws Exception
	 */
	@Test
	public void testCreateFromFilesystem() throws Exception {
		Synchronizer.disable();
		PortalImplementation impl = createWithFiles();
		GlobalId globalId = supply(() -> impl.getGlobalId());
		operate(() -> pack.synchronize(impl, true));
		File filesDir = filesDir(impl);

		operate(t -> t.getObject(impl, true).delete());
		PortalImplementation deleted = supply(t -> t.getObject(PortalImplementation.class, globalId));
		assertThat(deleted).as("Deleted Portal Implementation").isNull();

		// add a file in the filesystem
		writeFile(new File(filesDir, "assets/img/logo.png"), PNG_CONTENTS);

		operate(() -> assertThat(pack.syncAllFromFilesystem(PortalImplementation.class)).as("Number of synchronized Portal Implementations").isEqualTo(1));

		operate(t -> {
			PortalImplementation synced = t.getObject(PortalImplementation.class, globalId);
			assertThat(synced).as("Portal Implementation after sync").isNotNull();
			assertThat(synced.getKeyword()).as("Keyword").isEqualTo(impl.getKeyword());
			assertThat(synced.getName()).as("Name").isEqualTo(impl.getName());
			assertThat(synced.getDescription()).as("Description").isEqualTo("Description");
			assertThat(synced.getType()).as("Type").isEqualTo(Type.PHP);

			assertThat(filesInCms(synced)).as("Files in CMS").containsExactlyEntriesOf(new TreeMap<>(Map.of(
					"index.php", "<?php echo 'index'; ?>",
					"src/a.php", "<?php echo 'a'; ?>",
					"src/lib/empty.txt", "",
					"assets/style.css", "body {}",
					"assets/img/logo.png", PNG_CONTENTS)));
			assertThat(synced.getFile("assets/style.css").getMimetype()).as("Detected mimetype").isEqualTo("text/css");
			assertThat(synced.getFile("assets/img/logo.png").getMimetype()).as("Detected mimetype").isEqualTo("image/png");
		});
	}

	/**
	 * Test updating a Portal Implementation from the package: changed files are updated, removed files deleted and unchanged files are not touched
	 * @throws Exception
	 */
	@Test
	public void testUpdateFromFilesystem() throws Exception {
		Synchronizer.disable();
		PortalImplementation impl = createWithFiles();
		operate(() -> pack.synchronize(impl, true));
		File filesDir = filesDir(impl);
		int unchangedId = supply(() -> impl.getFile("index.php").getId());
		int changedId = supply(() -> impl.getFile("src/a.php").getId());

		writeFile(new File(filesDir, "src/a.php"), "<?php echo 'changed in fs'; ?>");
		Files.delete(new File(filesDir, "assets/style.css").toPath());

		operate(() -> assertThat(pack.syncAllFromFilesystem(PortalImplementation.class)).as("Number of synchronized Portal Implementations").isEqualTo(1));

		operate(t -> {
			PortalImplementation synced = t.getObject(PortalImplementation.class, impl.getId());
			assertThat(filesInCms(synced)).as("Files in CMS").containsExactlyEntriesOf(new TreeMap<>(Map.of(
					"index.php", "<?php echo 'index'; ?>",
					"src/a.php", "<?php echo 'changed in fs'; ?>",
					"src/lib/empty.txt", "")));
			assertThat(synced.getFile("src/a.php").getId()).as("ID of changed file").isEqualTo(changedId);
			assertThat(synced.getFile("index.php").getId()).as("ID of unchanged file").isEqualTo(unchangedId);
		});
		assertThat(countEdits(unchangedId)).as("Number of updates of the unchanged file").isEqualTo(0);
		assertThat(countEdits(changedId)).as("Number of updates of the changed file").isEqualTo(1);
	}

	/**
	 * Test that synchronizing from the filesystem fails, if the keyword does not match the folder name
	 * @throws Exception
	 */
	@Test
	public void testKeywordMismatch() throws Exception {
		Synchronizer.disable();
		PortalImplementation impl = createWithFiles();
		operate(() -> pack.synchronize(impl, true));

		File structureFile = new File(implDir(impl), AbstractSynchronizer.STRUCTURE_FILE);
		String structure = Files.readString(structureFile.toPath());
		Files.writeString(structureFile.toPath(), structure.replace("\"" + impl.getKeyword() + "\"", "\"otherkeyword\""));

		assertThatThrownBy(() -> operate(() -> pack.syncAllFromFilesystem(PortalImplementation.class))).isInstanceOf(NodeException.class)
				.hasMessageContaining("keyword must be " + impl.getKeyword());
	}

	/**
	 * Test that synchronizing from the filesystem fails without changes, if a file has an invalid path
	 * @throws Exception
	 */
	@Test
	public void testInvalidPathInFilesystem() throws Exception {
		Synchronizer.disable();
		PortalImplementation impl = createWithFiles();
		operate(() -> pack.synchronize(impl, true));
		File filesDir = filesDir(impl);

		Files.delete(new File(filesDir, "index.php").toPath());
		writeFile(new File(filesDir, "src/back\\slash.php"), "invalid");

		assertThatThrownBy(() -> operate(() -> pack.syncAllFromFilesystem(PortalImplementation.class))).isInstanceOf(NodeException.class);

		operate(t -> {
			PortalImplementation reloaded = t.getObject(PortalImplementation.class, impl.getId());
			assertThat(filesInCms(reloaded)).as("Files in CMS").containsOnlyKeys("index.php", "src/a.php", "src/lib/empty.txt", "assets/style.css");
		});
	}

	/**
	 * Add the Portal Implementation to the package and wait until all filesystem events caused by this have been handled
	 * @param impl Portal Implementation
	 * @throws Exception
	 */
	protected void addToPackage(PortalImplementation impl) throws Exception {
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			operate(() -> pack.synchronize(impl, true));
		}
	}

	/**
	 * Create a Portal Implementation with some files
	 * @return Portal Implementation
	 * @throws NodeException
	 */
	protected PortalImplementation createWithFiles() throws NodeException {
		int index = counter.incrementAndGet();
		PortalImplementation impl = supply(() -> Builder.create(PortalImplementation.class, created -> {
			created.setKeyword("portal" + index);
			created.setName("Portal " + index);
			created.setDescription("Description");
			created.setType(Type.PHP);
		}).build());
		operate(() -> {
			createFile(impl, "index.php", "<?php echo 'index'; ?>");
			createFile(impl, "src/a.php", "<?php echo 'a'; ?>");
			createFile(impl, "src/lib/empty.txt", "");
			createFile(impl, "assets/style.css", "body {}");
		});
		return impl;
	}

	/**
	 * Create a file in the Portal Implementation. Must be called with a transaction
	 * @param impl Portal Implementation
	 * @param path path
	 * @param contents contents
	 * @throws NodeException
	 */
	protected void createFile(PortalImplementation impl, String path, String contents) throws NodeException {
		Builder.create(PortalImplementationFile.class, file -> {
			file.setPortalImplementationId(impl.getId());
			file.setPath(path);
			file.setContents(stream(contents));
		}).build();
	}

	/**
	 * Get a stream of the UTF-8 encoded string
	 * @param contents string
	 * @return stream
	 */
	protected static ByteArrayInputStream stream(String contents) {
		return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Write the string into the file (create parent directories)
	 * @param file file
	 * @param contents contents
	 * @throws IOException
	 */
	protected static void writeFile(File file, String contents) throws IOException {
		file.getParentFile().mkdirs();
		Files.writeString(file.toPath(), contents, StandardCharsets.ISO_8859_1);
	}

	/**
	 * Get the directory of the Portal Implementation in the package
	 * @param impl Portal Implementation
	 * @return directory
	 */
	protected File implDir(PortalImplementation impl) {
		return new File(new File(new File(syncContext.getPackagesRoot(), PACKAGE_NAME), PackageSynchronizer.PORTAL_IMPLEMENTATIONS_DIR), impl.getKeyword());
	}

	/**
	 * Get the files directory of the Portal Implementation in the package
	 * @param impl Portal Implementation
	 * @return directory
	 */
	protected File filesDir(PortalImplementation impl) {
		return new File(implDir(impl), PortalImplementationSynchronizer.FILES_DIR);
	}

	/**
	 * Get the files of the Portal Implementation in the package
	 * @param impl Portal Implementation
	 * @return sorted map of relative paths to contents
	 * @throws IOException
	 */
	protected Map<String, String> filesInPackage(PortalImplementation impl) throws IOException {
		Path filesDir = filesDir(impl).toPath();
		Map<String, String> files = new TreeMap<>();
		try (Stream<Path> stream = Files.walk(filesDir)) {
			for (Path path : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
				files.put(filesDir.relativize(path).toString().replace(File.separatorChar, '/'), Files.readString(path));
			}
		}
		return files;
	}

	/**
	 * Get the files of the Portal Implementation in the CMS. Must be called with a transaction
	 * @param impl Portal Implementation
	 * @return sorted map of paths to contents
	 * @throws NodeException
	 */
	protected Map<String, String> filesInCms(PortalImplementation impl) throws NodeException {
		Map<String, String> files = new TreeMap<>();
		for (PortalImplementationFile file : impl.getFiles(null)) {
			try {
				files.put(file.getPath(), Files.readString(file.getBinFile().toPath(), StandardCharsets.ISO_8859_1));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}
		return files;
	}

	/**
	 * Count the logged updates of the file with given ID
	 * @param fileId file ID
	 * @return number of logged updates
	 * @throws NodeException
	 */
	protected int countEdits(int fileId) throws NodeException {
		return supply(() -> DBUtils.select("SELECT COUNT(*) c FROM logcmd WHERE o_type = ? AND o_id = ? AND cmd_desc_id = ?", ps -> {
			ps.setInt(1, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE);
			ps.setInt(2, fileId);
			ps.setInt(3, ActionLogger.EDIT);
		}, rs -> rs.next() ? rs.getInt("c") : 0));
	}
}
