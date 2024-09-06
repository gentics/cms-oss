package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.rest.model.EditorControlStyle;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Special test cases for synchronizing constructs.
 *
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class ConstructSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected static Node node;

	protected PackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}

		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	/**
	 * Test if the synchronization fails when multiple tagparts have the same keyword.
	 */
	@Test
	public void testUniquePartKeynames() throws Exception {
		String keyname = "text";
		int cid = Trx.supply(() -> {
			int id = ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "construct", keyname);
			Construct c = TransactionManager.getCurrentTransaction().getObject(Construct.class, id, true);

			c.getParts().add(Creator.createSimplePart(1, keyname, 1));
			c.save();

			return id;
		});

		Trx.operate(() -> {
			try {
				Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, cid);

				pack.synchronize(construct, true);
				fail(String.format("Adding %s to the package was expected to fail, but succeeded", construct));
			} catch (NodeException e) {
				// this is expected behaviour
			}
		});

		Trx.operate(() -> assertThat(pack.getObjects(Construct.class)).as("Constructs in package").isEmpty());
	}

	/**
	 * Test sync of externalEditorUrl property
	 * @throws NodeException
	 */
	@Test
	public void testExternalEditorUrl() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);
			created.setExternalEditorUrl("/external/url.html");
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			GCNAssertions.assertThat(afterSync).as("Construct after sync").isNotNull().hasFieldOrPropertyWithValue("externalEditorUrl", "/external/url.html");
		});
	}

	/**
	 * Test sync of editOnInsert property
	 * @throws NodeException
	 */
	@Test
	public void testEditOnInsert() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);
			created.setEditOnInsert(true);
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			GCNAssertions.assertThat(afterSync)
				.as("Construct after sync")
				.isNotNull()
				.hasFieldOrPropertyWithValue("editOnInsert", true);
		});
	}

	/**
	 * Test sync of editorControlStyle property
	 * @throws NodeException
	 */
	@Test
	public void testEditorControlStyle() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);
			created.setEditorControlStyle(EditorControlStyle.CLICK);
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			GCNAssertions.assertThat(afterSync)
				.as("Construct after sync")
				.isNotNull()
				.hasFieldOrPropertyWithValue("editorControlStyle", EditorControlStyle.CLICK);
		});
	}

	/**
	 * Test sync of editorControlInside property
	 * @throws NodeException
	 */
	@Test
	public void testEditorControlInside() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);
			created.setEditorControlInside(true);
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			GCNAssertions.assertThat(afterSync)
				.as("Construct after sync")
				.isNotNull()
				.hasFieldOrPropertyWithValue("editorControlInside", true);
		});
	}

	/**
	 * Test sync of part.hideInEditor
	 * @throws NodeException
	 */
	@Test
	public void testPartHideInEditor() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);

			created.getParts().add(create(Part.class, part -> {
				part.setKeyname("part");
				part.setPartTypeId(getPartTypeId(ShortTextPartType.class));
				part.setHideInEditor(true);
			}, false));
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			assertThat(afterSync).as("Construct after sync").isNotNull();
			GCNAssertions.assertThat(afterSync.getParts().get(0)).as("Part after sync").hasFieldOrPropertyWithValue("hideInEditor", true);
		});
	}

	/**
	 * Test sync of part.externalEditorUrl
	 * @throws NodeException
	 */
	@Test
	public void testPartExternalEditorUrl() throws NodeException {
		Synchronizer.disable();

		Construct construct = supply(() -> create(Construct.class, created -> {
			created.setAutoEnable(true);
			created.setKeyword("keyword");
			created.setName("Name", 1);

			created.getParts().add(create(Part.class, part -> {
				part.setKeyname("part");
				part.setPartTypeId(getPartTypeId(ShortTextPartType.class));
				part.setExternalEditorUrl("/external/url.html");
			}, false));
		}));
		GlobalId globalId = supply(() -> construct.getGlobalId());

		consume(c -> pack.synchronize(c, true), construct);

		operate(() -> update(construct, Construct::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		operate(t -> {
			Construct afterSync = t.getObject(Construct.class, globalId);
			assertThat(afterSync).as("Construct after sync").isNotNull();
			GCNAssertions.assertThat(afterSync.getParts().get(0)).as("Part after sync").hasFieldOrPropertyWithValue("externalEditorUrl", "/external/url.html");
		});
	}

	/**
	 * Test synchronization of construct with part of type Datasource (with values)
	 * @throws NodeException
	 */
	@Test
	public void testDatasourcePart() throws NodeException {
		Synchronizer.disable();

		// create construct with datasource part
		Map<String, String> entries = new HashMap<>();
		entries.put("one", "eins");
		entries.put("two", "zwei");
		entries.put("three", "drei");

		Construct dsConstruct = supply(t -> {
			int dsConstructId = createConstruct(node, DatasourcePartType.class, "datasource", "part");
			return update(t.getObject(Construct.class, dsConstructId), c -> {
				DatasourcePartType partType = getPartType(DatasourcePartType.class, c, "part");
				List<DatasourceEntry> items = partType.getItems();
				for (Map.Entry<String, String> entry : entries.entrySet()) {
					items.add(create(DatasourceEntry.class, e -> {
						e.setKey(entry.getKey());
						e.setValue(entry.getValue());
					}, false));
				}
			});
		});
		GlobalId globalId = execute(Construct::getGlobalId, dsConstruct);
		List<DatasourceEntry> items = execute(c -> {
			return getPartType(DatasourcePartType.class, c, "part").getItems();
		}, dsConstruct);

		// add to package
		consume(c -> pack.synchronize(c, true), dsConstruct);

		// delete locally
		consume(del -> update(del, Construct::delete), dsConstruct);

		// sync from package
		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		// assert that values exist
		dsConstruct = supply(t -> t.getObject(Construct.class, globalId));
		assertThat(dsConstruct).as("Synchronized construct").isNotNull();

		Trx.consume((c, i) -> {
			assertThat(getPartType(DatasourcePartType.class, c, "part").getItems()).as("Datasource items after sync").usingElementComparatorOnFields("key", "value")
					.containsExactlyElementsOf(i);
		}, dsConstruct, items);
	}

	/**
	 * Test changing types of parts when synchronizing constructs
	 * @throws NodeException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testChangePartType() throws NodeException, IOException, URISyntaxException {
		Synchronizer.disable();
		File packageRootDir = pack.getPackagePath().toFile();

		// copy files from first package
		FileUtils.copyDirectory(new File(getClass().getResource("packages/construct_old").toURI()), packageRootDir);

		// sync from package
		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		// assert construct
		List<PackageObject<Construct>> constructs = supply(() -> pack.getObjects(Construct.class));
		assertThat(constructs).as("Constructs in package").hasSize(1);
		Trx.consume(globalId -> {
			Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, globalId);
			assertThat(construct).as("Construct").isNotNull();
			assertThat(construct.getValues().getByKeyname("one").getPart()).as("Part one").hasTypeId(21);
			assertThat(construct.getValues().getByKeyname("two").getPart()).as("Part two").hasTypeId(4);
		}, constructs.get(0).getGlobalId());

		// copy files from second package
		FileUtils.copyDirectory(new File(getClass().getResource("packages/construct_new").toURI()), packageRootDir);

		// sync from package
		operate(() -> assertThat(pack.syncAllFromFilesystem(Construct.class)).as("Number of synchronized constructs").isEqualTo(1));

		// assert construct
		constructs = supply(() -> pack.getObjects(Construct.class));
		assertThat(constructs).as("Constructs in package").hasSize(1);
		Trx.consume(globalId -> {
			Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, globalId);
			assertThat(construct).as("Construct").isNotNull();
			assertThat(construct.getValues().getByKeyname("one").getPart()).as("Part one").hasTypeId(4);
			assertThat(construct.getValues().getByKeyname("two").getPart()).as("Part two").hasTypeId(21);
		}, constructs.get(0).getGlobalId());
	}
}
