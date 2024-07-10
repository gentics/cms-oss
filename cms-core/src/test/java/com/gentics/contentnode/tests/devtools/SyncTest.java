package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.clean;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getGlobalId;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getStructureFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.FileMonitorWatcher;
import com.gentics.contentnode.devtools.IFileWatcher;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.WatchServiceWatcher;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;


/**
 * Testcases for synchronization of objects to the filesystem (devtools)
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class SyncTest {
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

	/**
	 * Logger
	 */
	public final static NodeLogger logger = NodeLogger.getNodeLogger(SyncTest.class);

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Map containing suppliers to create objects of the specified class
	 */
	protected static Map<Class<? extends SynchronizableNodeObject>, Supplier<? extends SynchronizableNodeObject>> creatorMap = new HashMap<>();

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: filemonitor {0}, type {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Class<? extends IFileWatcher> monitorClass : Arrays.asList(FileMonitorWatcher.class, WatchServiceWatcher.class)) {
			for (Class<? extends SynchronizableNodeObject> typeClass : Arrays.asList(Construct.class, Datasource.class, ObjectTagDefinition.class,
					Template.class, CrFragment.class, ContentRepository.class)) {
				data.add(new Object[] { monitorClass, typeClass });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}

		Node dummyNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		// prepare suppliers that create new objects
		creatorMap.put(
				Construct.class,
				() -> TransactionManager.getCurrentTransaction().getObject(Construct.class,
						createConstruct(dummyNode, HTMLPartType.class, "newconstruct", "part")));
		creatorMap.put(
				Datasource.class,
				() -> createDatasource("newdatasource", Arrays.asList("one", "two", "three")));
		creatorMap.put(
				ObjectTagDefinition.class,
				() -> createObjectPropertyDefinition(Page.TYPE_PAGE, 1, "New OE", "newoe"));
		creatorMap.put(
				Template.class,
				() -> createTemplate(dummyNode.getFolder(), "Testtemplate"));
		creatorMap.put(ContentRepository.class, () -> createContentRepository("Test CR", false, false, "bla"));
		creatorMap.put(CrFragment.class, () -> {
			return create(CrFragment.class, fragment -> {
				fragment.setName("Test Fragment");
				for (String text : Arrays.asList("one", "two", "three")) {
					fragment.getEntries().add(create(CrFragmentEntry.class, entry -> {
						entry.setObjType(Page.TYPE_PAGE);
						entry.setAttributeTypeId(AttributeType.text.getType());
						entry.setTagname(text);
						entry.setMapname(text);
					}, false));
				}
			});
		});
	}

	/**
	 * Test name
	 */
	@Rule
	public TestName testName = new TestName() {
		protected void starting(Description description) {
			super.starting(description);
			logger.debug(StringUtils.repeat("-", 40));
			logger.debug(String.format("Starting %s", getMethodName()));
		}

		@Override
		protected void finished(Description description) {
			super.finished(description);
			logger.debug(String.format("Finished %s", getMethodName()));
		}
	};

	@Parameter(0)
	public Class<? extends IFileWatcher> monitorClass;

	@Parameter(1)
	public Class<? extends SynchronizableNodeObject> typeClass;

	/**
	 * Objects created during the test (needs cleanup)
	 */
	protected Set<NodeObject> objects = new HashSet<>();

	/**
	 * Root directory of the test package
	 */
	protected File testPackageRoot;

	@Before
	public void setup() throws Exception {
		syncContext.setMonitorClass(monitorClass);

		// create clean package directory
//		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			testPackageRoot = new File(syncContext.getPackagesRoot(), "testpackage");
			testPackageRoot.mkdir();
//		}
		syncContext.restart();
	}

	@After
	public void teardown() throws Exception {
		for (NodeObject obj : objects) {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				NodeObject editable = t.getObject(obj, true);
				if (editable != null) {
					editable.delete(true);
				}
			});
		}
		objects.clear();
	}

	/**
	 * Test creation of objects in FS (by copying files)
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectInFS() throws Exception {
		// copy the files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		// iterate over all subdirectories
		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// extract the globalid from the gentics_structure.json file
			String globalId = getGlobalId(objectDir);
			SynchronizableNodeObject object = loadObject(typeClass, globalId);

			Trx.consume(o -> {
				assertThat(new ArrayList<PackageObject<?>>(Synchronizer.getPackage("testpackage").getObjects(typeClass))).as("Synchronizer object list")
						.containsOnly(new PackageObject<>(o));
			}, object);

			Trx.consume(o -> {
				if (o instanceof Construct) {
					Construct construct = (Construct) o;
					assertThat(construct).as("construct")
						.hasKeyword("testconstruct")
						.hasName("de", "Test Tagtyp")
						.hasName("en", "Test Tagtype")
						.hasDescription("de", "Das ist der Test Tagtyp")
						.hasDescription("en", "This is the Test Tagtype")
						.mayBeSubtag(false)
						.mayContainSubtags(true)
						.isAutoEnabled(true);
					assertThat(construct.getParts()).as("parts list").hasSize(1);
					assertThat(construct.getParts().get(0)).as("Part")
						.has(new GlobalId("ABCD.6b5a5aa5-d98b-11e6-8b5d-ea5d8efab9e8"))
						.hasName("de", "HTML Inhalt")
						.hasName("en", "HTML Content")
						.hasKeyword("html")
						.isEditable(true)
						.hasTypeId(21)
						.hasOrder(1)
						.hasMlId(0)
						.isVisible(true)
						.isRequired(false)
						.isInlineEditable(true)
						.hasInfoInt(1005);
					assertThat(construct.getParts().get(0).getDefaultValue()).as("Part value").hasText("<p>Default content</p>");
				} else if (o instanceof Datasource) {
					Datasource datasource = (Datasource) o;
					assertThat(datasource).as("datasource").isStatic().hasName("testdatasource");
					assertThat(datasource.getEntries()).as("datasource entries").hasSize(2);
					assertThat(datasource.getEntries().get(0)).as("first entry")
						.has(new GlobalId("ABCD.1e12d9f3-dbc1-11e6-8b5d-ea5d8efab9e8"))
						.hasKey("First Key")
						.hasValue("First Value");
					assertThat(datasource.getEntries().get(1)).as("second entry")
						.has(new GlobalId("ABCD.2890c7e2-dbc1-11e6-8b5d-ea5d8efab9e8"))
						.hasKey("Second Key")
						.hasValue("Second Value");
				} else if (o instanceof ObjectTagDefinition) {
					ObjectTagDefinition objTagDef = (ObjectTagDefinition) o;
					assertThat(objTagDef).as("object tag definition")
						.isTargetType(Folder.TYPE_FOLDER)
						.hasKeyword("object.testoe")
						.hasName("de", "Test Objekteigenschaft")
						.hasName("en", "Test Objectproperty")
						.hasDescription("de", "Beschreibung der Test Objekteigenschaft")
						.hasDescription("en", "Description of the Test Objectproperty")
						.hasConstruct(new GlobalId("8371.71123"));
				} else if (o instanceof Template) {
					Template template = (Template) o;
					assertThat(template).as("template")
						.hasName("Testtemplate")
						.hasDescription("Description of the template")
						.hasType("HTML")
						.hasSource("Source of the template.");

					assertThat(template.getTemplateTags()).as("template tags").hasSize(1).containsKey("content");
					assertThat(template.getTemplateTags().get("content")).as("template tag 'content'")
						.has(new GlobalId("ABCD.4a74deb3-dbc3-11e6-8b5d-ea5d8efab9e8"))
						.hasName("content")
						.hasConstruct(new GlobalId("8371.71123"))
						.isEditableInPage(true)
						.isMandatory(false);
					assertThat(template.getTemplateTags().get("content").getValues().getByKeyname("text")).as("value of 'content.text'")
						.hasText("Tag content");

					assertThat(template.getObjectTags()).as("object tags").hasSize(1).containsKey("testoe");
					assertThat(template.getObjectTags().get("testoe")).as("object tag 'object.testoe'")
						.has(new GlobalId("ABCD.fd35b2d4-dbeb-11e6-8b5d-ea5d8efab9e8"))
						.hasName("object.testoe")
						.hasConstruct(new GlobalId("8371.71123"))
						.isEnabled(true);
					assertThat(template.getObjectTags().get("testoe").getValues().getByKeyname("text")).as("value of 'object.testoe.text'")
						.hasText("OE content");
				} else if (o instanceof CrFragment) {
					CrFragment fragment = (CrFragment) o;
					assertThat(fragment).as("fragment").hasFieldOrPropertyWithValue("name", "Test")
							.has(new GlobalId("3D6C.ee9084bf-959e-11e8-918d-00155df038f9"));
					assertThat(fragment.getEntries()).as("fragment entries").hasSize(1);
				} else if (o instanceof ContentRepository) {
					ContentRepository cr = (ContentRepository) o;
					assertThat(cr).as("contentrepository").hasFieldOrPropertyWithValue("name", "Mesh CR");
					assertThat(cr.getEntries()).as("contentrepository entries").hasSize(39);
				} else {
					fail(String.format("Unexpected object %s", o));
				}
			}, object);
		}
	}

	/**
	 * Test updating objects in the FS
	 * @throws Exception
	 */
	@Test
	public void testChangeObjectInFS() throws Exception {
		// copy the initial files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}
		logger.debug("Finished setup data");

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// check that the object exists
			loadObject(typeClass, getGlobalId(objectDir));
		}

		syncContext.restart();
		logger.debug("Restarted synchronizer");

		// modify the object in the FS by copying a modified version
		sourceDir = new File(getClass().getResource("packages/testpackage_mod/" + dir).toURI());
		logger.debug("Start updating in FS");
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}
		logger.debug("Updated in FS");

		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// extract the globalid from the gentics_structure.json file
			String globalId = getGlobalId(objectDir);
			SynchronizableNodeObject object = loadObject(typeClass, globalId);

			Trx.consume(o -> {
				assertThat(new ArrayList<PackageObject<?>>(Synchronizer.getPackage("testpackage").getObjects(typeClass)))
						.as("Synchronizer object list").containsOnly(new PackageObject<>(o));
			}, object);

			// assert changes
			Trx.consume(o -> {
				logger.debug(String.format("Check %s", o));
				if (o instanceof Construct) {
					Construct construct = (Construct) o;
					assertThat(construct.getParts().get(0).getDefaultValue()).as("Part value").hasText("<p>Changed Default content</p>");
				} else if (o instanceof Datasource) {
					Datasource datasource = (Datasource) o;
					assertThat(datasource.getEntries().get(0)).as("second entry")
						.has(new GlobalId("ABCD.2890c7e2-dbc1-11e6-8b5d-ea5d8efab9e8"))
						.hasKey("Second Key")
						.hasValue("Second Modified Value");
					assertThat(datasource.getEntries().get(1)).as("first entry")
						.has(new GlobalId("ABCD.1e12d9f3-dbc1-11e6-8b5d-ea5d8efab9e8"))
						.hasKey("First Key")
						.hasValue("First Modified Value");
				} else if (o instanceof ObjectTagDefinition) {
					ObjectTagDefinition objTagDef = (ObjectTagDefinition) o;
					assertThat(objTagDef).as("object tag definition")
						.hasName("de", "Test Objekteigenschaft (neuer Name)")
						.hasName("en", "Test Objectproperty (new name)");
				} else if (o instanceof Template) {
					Template template = (Template) o;
					assertThat(template).as("template")
						.hasSource("Changed source of the template.");
				} else if (o instanceof CrFragment) {
					CrFragment fragment = (CrFragment) o;
					assertThat(fragment.getEntries().get(0)).as("fragment entry").hasFieldOrPropertyWithValue("optimized", true);
				} else if (o instanceof ContentRepository) {
					ContentRepository cr = (ContentRepository) o;
					assertThat(cr).as("contentrepository").hasFieldOrPropertyWithValue("instantPublishing", false);
					assertThat(cr).as("contentrepository").hasFieldOrPropertyWithValue("noPagesIndex", false);
				} else {
					fail(String.format("Unexpected object %s", o));
				}
			}, object);
		}
	}

	/**
	 * Test removing objects from the FS
	 * @throws Exception
	 */
	@Test
	public void testRemoveObjectFromFS() throws Exception {
		// copy the initial files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		Set<SynchronizableNodeObject> objects = new HashSet<>();

		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// check that the object exists
			SynchronizableNodeObject object = loadObject(typeClass, getGlobalId(objectDir));
			objects.add(object);
		}

		syncContext.restart();

		// clean the object container
		clean(objectContainer, MAX_WAIT_MS, WAIT_SLEEP_MS);

		// the object must still exist
		Trx.consume(oSet -> {
			List<SynchronizableNodeObject> loadedList = new ArrayList<>(TransactionManager.getCurrentTransaction().getObjects(typeClass,
					oSet.stream().map(SynchronizableNodeObject::getId).collect(Collectors.toSet())));
			assertThat(loadedList).as("Existing objects").containsOnlyElementsOf(objects);
		}, objects);

		// objects must be removed from package synchronizer
		Trx.operate(() -> {
			assertThat(new ArrayList<PackageObject<?>>(Synchronizer.getPackage("testpackage").getObjects(typeClass)))
					.as("Synchronizer object list").isEmpty();
		});
	}

	/**
	 * Test changing an object in the CMS
	 * @throws Exception
	 */
	@Test
	public void testChangeObjectInCMS() throws Exception {
		// copy the initial files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}
		syncContext.restart();

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		Set<SynchronizableNodeObject> objects = new HashSet<>();

		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// check that the object exists
			SynchronizableNodeObject object = loadObject(typeClass, getGlobalId(objectDir));
			objects.add(object);
		}

		// change the objects
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			for (SynchronizableNodeObject object : objects) {
				Trx.operate(() -> {
					ContentNodeTestDataUtils.update(object, o -> {
						if (o instanceof Construct) {
							Construct construct = (Construct) o;
							construct.setName("Neuer Test Tagtyp", 1);
							construct.getParts().get(0).getDefaultValue().setValueText("<p>New default content</p>");
						} else if (o instanceof Datasource) {
							Datasource datasource = (Datasource) o;
							Collections.reverse(datasource.getEntries());
						} else if (o instanceof ObjectTagDefinition) {
							ObjectTagDefinition objectTagDefinition = (ObjectTagDefinition) o;
							objectTagDefinition.setName("Bla", 2);
						} else if (o instanceof Template) {
							Template template = (Template) o;
							template.setSource("Completely new source");
						} else if (o instanceof CrFragment) {
							CrFragment fragment = (CrFragment) o;
							CrFragmentEntry entry = fragment.getEntries().get(0);
							entry.setMultivalue(!entry.isMultivalue());
						} else if (o instanceof ContentRepository) {
							ContentRepository cr = (ContentRepository) o;
							cr.setInstantPublishing(!cr.isInstantPublishing());
							cr.setNoPagesIndex(!cr.isNoPagesIndex());
						} else {
							fail(String.format("Unexpected object %s", o));
						}
					});
				});
			}
		}

		// check for the changes in the FS
		for (SynchronizableNodeObject object : objects) {
			Trx.consume(o -> {
				try {
					logger.debug(String.format("Check %s in FS", o));
					if (o instanceof Construct) {
						Construct construct = (Construct) o;
						File structureFile = getStructureFile(testPackageRoot, o);

						ObjectMapper mapper = new ObjectMapper();
						logger.debug(String.format("Read file %s", structureFile));
						try (InputStream in = new FileInputStream(structureFile)) {
							JsonNode structure = mapper.readTree(in);
							assertThat(structure.get("name").get("de").asText()).as("Updated german name").isEqualTo("Neuer Test Tagtyp");
						}

						File partFile = new File(objectContainer, construct.getKeyword() + "/part.html.html");
						assertThat(FileUtils.readFileToString(partFile)).as("Updated part content").isEqualTo("<p>New default content</p>");
					} else if (o instanceof Datasource) {
						File structureFile = getStructureFile(testPackageRoot, o);

						ObjectMapper mapper = new ObjectMapper();
						try (InputStream in = new FileInputStream(structureFile)) {
							JsonNode structure = mapper.readTree(in);
							Iterator<JsonNode> iterator = structure.get("values").elements();
							assertThat(iterator.next().get("key").asText()).as("First entry key").isEqualTo("Second Key");
							assertThat(iterator.next().get("key").asText()).as("Second entry key").isEqualTo("First Key");
						}
					} else if (o instanceof ObjectTagDefinition) {
						File structureFile = getStructureFile(testPackageRoot, o);

						ObjectMapper mapper = new ObjectMapper();
						try (InputStream in = new FileInputStream(structureFile)) {
							JsonNode structure = mapper.readTree(in);
							assertThat(structure.get("name").get("en").asText()).as("Updated english name").isEqualTo("Bla");
						}
					} else if (o instanceof Template) {
						Template template = (Template) o;

						File sourceFile = new File(objectContainer, template.getName() + "/source.html");
						assertThat(FileUtils.readFileToString(sourceFile)).as("Updated source").isEqualTo("Completely new source");
					}
				} catch (Exception e) {
					throw new NodeException(e);
				}
			}, object);
		}
	}

	/**
	 * Test that objects are removed from packages, when they are removed from the CMS
	 * @throws Exception
	 */
	@Test
	public void testRemoveObjectInCMS() throws Exception {
		// create a new object
		SynchronizableNodeObject newObject = Trx.supply(creatorMap.get(typeClass));

		// add it to the package
		Trx.operate(() -> Synchronizer.getPackage("testpackage").synchronize(newObject, true));

		// assert that the object was synchronized
		File structureFile = Trx.supply(() -> getStructureFile(testPackageRoot, newObject));
		assertThat(structureFile).as("Structure file").exists();

		// remove the object from the CMS
		Trx.operate(() -> newObject.delete(true));

		// assert that the object was removed from the package
		assertThat(structureFile).as("Structure file").doesNotExist();
	}

	/**
	 * Test recreating objects in the FS, that were deleted in the CMS
	 * @throws Exception
	 */
	@Test
	public void testCreateDeletedObjectInFS() throws Exception {
		// copy the initial files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		Set<SynchronizableNodeObject> objects = new HashSet<>();

		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// check that the object exists
			SynchronizableNodeObject object = loadObject(typeClass, getGlobalId(objectDir));
			objects.add(object);
		}

		// remove files in FS
		Synchronizer.stop();
		clean(objectContainer, MAX_WAIT_MS, WAIT_SLEEP_MS);

		// delete objects in the CMS
		for (SynchronizableNodeObject obj : objects) {
			Trx.operate(() -> {
				obj.delete();
			});
		}
		Synchronizer.start();

		// objects must be gone now
		for (SynchronizableNodeObject obj : objects) {
			Trx.operate(() -> {
				assertThat(TransactionManager.getCurrentTransaction().getObject(obj)).as("deleted object").isNull();
			});
		}

		// recreate
		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		// objects must be there again with the same global, but a new local ID
		for (SynchronizableNodeObject obj : objects) {
			Trx.operate(() -> {
				SynchronizableNodeObject newObj = TransactionManager.getCurrentTransaction().getObject(typeClass, obj.getGlobalId());
				assertThat(newObj).as("recreated object").isNotNull();
				assertThat(newObj.getId()).as("local ID of recreated object").isNotEqualTo(obj.getId());
			});
		}
	}

	/**
	 * Test adding an object to the package
	 * @throws Exception
	 */
	@Test
	public void testAddObjectToPackage() throws Exception {
		// create a new object
		SynchronizableNodeObject newObject = Trx.supply(creatorMap.get(typeClass));

		// add it to the package
		Trx.operate(() -> Synchronizer.getPackage("testpackage").synchronize(newObject, true));

		// assert that the object was synchronized
		File structureFile = Trx.supply(() -> getStructureFile(testPackageRoot, newObject));
		assertThat(structureFile).as("Structure file").exists();

		assertThat(getGlobalId(structureFile.getParentFile())).as("Synchronized global ID").isEqualTo(newObject.getGlobalId().toString());
	}

	/**
	 * Test removing an object from a package
	 * @throws Exception
	 */
	@Test
	public void testRemoveObjectFromPackage() throws Exception {
		// copy the files into the package
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		assertThat(objectContainer).exists().isDirectory();
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isNotEmpty();

		Set<SynchronizableNodeObject> objects = new HashSet<>();

		// iterate over all subdirectories
		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			// extract the globalid from the gentics_structure.json file
			String globalId = getGlobalId(objectDir);
			objects.add(loadObject(typeClass, globalId));
		}

		// remove the objects
		for (SynchronizableNodeObject object : objects) {
			Trx.operate(() -> Synchronizer.getPackage("testpackage").remove(object, false));
		}

		// object container must be empty now
		assertThat(objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))).isEmpty();
	}

	/**
	 * Test whether caches get cleared when objects are added in the FS, even when automatic sync is disabled
	 * @throws Exception
	 */
	@Test
	public void testCacheClearOnAdd() throws Exception {
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);
		objectContainer.mkdirs();

		assertThat(objectContainer).as("Container directory").exists();

		Synchronizer.disable();
		assertThat(Trx.supply(() -> Synchronizer.getPackage("testpackage").getObjects(typeClass))).as("Objects in package").isEmpty();

		// copy the files into the package
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		FileUtils.copyDirectory(sourceDir, objectContainer);

		// wait some time for this check
		List<?> objects = null;
		long start = System.currentTimeMillis();
		do {
			objects = Trx.supply(() -> Synchronizer.getPackage("testpackage").getObjects(typeClass));

			if (objects.isEmpty()) {
				Thread.sleep(WAIT_SLEEP_MS);
			}
		} while (objects.isEmpty() && (System.currentTimeMillis() - start) < MAX_WAIT_MS);

		assertThat(objects).as("Objects in package").hasSize(1);
	}

	/**
	 * Test whether caches get cleared when objects are removed in the FS, even when automatic sync is disabled
	 * @throws Exception
	 */
	@Test
	public void testCacheClearOnRemove() throws Exception {
		String dir = MainPackageSynchronizer.directoryMap.get(typeClass);
		File objectContainer = new File(testPackageRoot, dir);

		// copy the files into the package
		File sourceDir = new File(getClass().getResource("packages/testpackage/" + dir).toURI());

		try (WaitSyncLatch latch = new WaitSyncLatch(EVENT_DELAY, MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
			FileUtils.copyDirectory(sourceDir, objectContainer);
		}

		Synchronizer.disable();
		assertThat(Trx.supply(() -> Synchronizer.getPackage("testpackage").getObjects(typeClass))).as("Objects in package").hasSize(1);

		// remove contents of objectContainer
		FileUtils.cleanDirectory(objectContainer);

		// wait some time for this check
		List<?> objects = null;
		long start = System.currentTimeMillis();
		do {
			objects = Trx.supply(() -> Synchronizer.getPackage("testpackage").getObjects(typeClass));

			if (objects.size() == 1) {
				Thread.sleep(WAIT_SLEEP_MS);
			}
		} while (objects.size() == 1 && (System.currentTimeMillis() - start) < MAX_WAIT_MS);

		assertThat(objects).as("Objects in package").isEmpty();
	}

	/**
	 * Try to load the object
	 * Assert that the object could be loaded
	 * @param clazz object class
	 * @param id object id
	 * @return object (never null)
	 * @throws NodeException
	 */
	protected <T extends NodeObject> T loadObject(Class<T> clazz, String id) throws NodeException {
		T object = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(clazz, id));
		assertThat(object).as(String.format("Object of class %s with ID %s", clazz.getName(), id)).isNotNull();
		objects.add(object);
		return object;
	}
}
