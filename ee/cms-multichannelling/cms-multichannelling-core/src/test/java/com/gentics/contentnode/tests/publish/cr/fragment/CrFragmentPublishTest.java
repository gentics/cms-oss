package com.gentics.contentnode.tests.publish.cr.fragment;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;

/**
 * Test cases for publishing into CRs using CR Fragments
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR, Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class CrFragmentPublishTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node nodeWithCR;

	private static Node nodeWithMCCR;

	private static Node nodeWithMeshCR;

	private static Integer meshCrId;

	private static CrFragment crFragment;

	private static Folder folderCR;

	private static Folder folderMCCR;

	private static Folder folderMeshCR;

	private static CrFragment crFragmentClone;

	/**
	 * Setup data and run publish process
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		nodeWithCR = Trx.supply(() -> createNode("cr", "Node with CR", PublishTarget.CONTENTREPOSITORY));
		nodeWithMCCR = Trx.supply(() -> createNode("mccr", "Node with MCCR", PublishTarget.CONTENTREPOSITORY, true, false));
		nodeWithMeshCR = Trx.supply(() -> createNode("meshCr", "Node with Mesh CR", PublishTarget.NONE));
		meshCrId = createMeshCR(mesh, MESH_PROJECT_NAME);

		nodeWithMeshCR = Trx.execute(n -> update(n, upd -> {
			upd.setPublishContentmap(true);
			upd.setContentrepositoryId(meshCrId);
		}), nodeWithMeshCR);

		crFragment = Trx.supply(() -> create(CrFragment.class, cr -> {
			cr.setName("Fragment");
			CrFragmentEntry entry = create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("from_fragment");
				e.setTagname("folder.name");
			}, false);
			cr.getEntries().add(entry);
		}));

		crFragmentClone = Trx.supply(() -> create(CrFragment.class, cr -> {
			cr.setName("FragmentClone");
			CrFragmentEntry entry = create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("from_fragment");
				e.setTagname("folder.name");
			}, false);
			cr.getEntries().add(entry);
		}));

		Trx.operate(t -> update(t.getObject(ContentRepository.class, meshCrId), cr -> {
			cr.getAssignedFragments().addAll(Arrays.asList(crFragment, crFragmentClone));
		}));

		Trx.operate(t -> update(t.getObject(ContentRepository.class, nodeWithCR.getContentrepositoryId()), cr -> {
			cr.getAssignedFragments().addAll(Arrays.asList(crFragment, crFragmentClone));
		}));

		Trx.operate(t -> update(t.getObject(ContentRepository.class, nodeWithMCCR.getContentrepositoryId()), cr -> {
			cr.getAssignedFragments().addAll(Arrays.asList(crFragment, crFragmentClone));
		}));

		folderCR = Trx.supply(() -> createFolder(nodeWithCR.getFolder(), "Folder"));
		folderMCCR = Trx.supply(() -> createFolder(nodeWithMCCR.getFolder(), "Folder"));
		folderMeshCR = Trx.supply(() -> createFolder(nodeWithMeshCR.getFolder(), "Folder"));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}
	}

	/**
	 * Disable instant publishing before every test run
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(t -> update(nodeWithCR.getContentRepository(), cr -> cr.setInstantPublishing(false)));
		Trx.operate(t -> update(nodeWithMCCR.getContentRepository(), cr -> cr.setInstantPublishing(false)));
		Trx.operate(t -> update(t.getObject(ContentRepository.class, meshCrId), cr -> cr.setInstantPublishing(false)));
	}

	/**
	 * Test publishing an object into a normal CR
	 * @throws NodeException
	 */
	@Test
	public void testPublishCR() throws NodeException {
		Trx.operate(() -> ContentNodeTestUtils.assertPublishCR(folderCR, nodeWithCR, true, r -> {
			assertThat(r.get("from_fragment")).as("Field from fragment").isNotNull().isEqualTo("Folder");
		}));
	}

	/**
	 * Test instant publishing an object into a normal CR
	 * @throws NodeException
	 */
	@Test
	public void testInstantPublishCR() throws NodeException {
		Trx.operate(t -> update(nodeWithCR.getContentRepository(), cr -> cr.setInstantPublishing(true)));

		Folder instantFolder = Trx.supply(() -> createFolder(nodeWithCR.getFolder(), "Instant Folder"));
		Trx.operate(() -> ContentNodeTestUtils.assertPublishCR(instantFolder, nodeWithCR, true, r -> {
			assertThat(r.get("from_fragment")).as("Field from fragment").isNotNull().isEqualTo("Instant Folder");
		}));
	}

	/**
	 * Test publishing an object into a MCCR
	 * @throws NodeException
	 */
	@Test
	public void testPublishMCCR() throws NodeException {
		Trx.operate(() -> ContentNodeTestUtils.assertPublishCR(folderMCCR, nodeWithMCCR, true, r -> {
			assertThat(r.get("from_fragment")).as("Field from fragment").isNotNull().isEqualTo("Folder");
		}));
	}

	/**
	 * Test instant publishing an object into a MCCR
	 * @throws NodeException
	 */
	@Test
	public void testInstantPublishMCCR() throws NodeException {
		Trx.operate(t -> update(nodeWithMCCR.getContentRepository(), cr -> cr.setInstantPublishing(true)));

		Folder instantFolder = Trx.supply(() -> createFolder(nodeWithMCCR.getFolder(), "Instant Folder"));
		Trx.operate(() -> ContentNodeTestUtils.assertPublishCR(instantFolder, nodeWithMCCR, true, r -> {
			assertThat(r.get("from_fragment")).as("Field from fragment").isNotNull().isEqualTo("Instant Folder");
		}));
	}

	/**
	 * Test publishing an object into a Mesh CR
	 * @throws NodeException
	 */
	@Test
	public void testPublishMeshCR() throws NodeException {
		Trx.operate(() -> assertObject("", mesh.client(), MESH_PROJECT_NAME, folderMeshCR, true, n -> {
			StringFieldImpl fieldFromFragment = n.getFields().getStringField("from_fragment");
			assertThat(fieldFromFragment).as("Field from fragment").isNotNull();
			assertThat(fieldFromFragment.getString()).as("Field from fragment").isEqualTo("Folder");
		}));
	}

	/**
	 * Test instant publishing an object into a Mesh CR
	 * @throws NodeException
	 */
	@Test
	public void testInstantPublishMeshCR() throws NodeException {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, meshCrId), cr -> cr.setInstantPublishing(true)));

		Folder instantFolder = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(nodeWithMeshCR.getFolder().getId());
			f.setName("Instant Folder");
			f.setPublishDir("bla");
		}));

		Trx.operate(() -> assertObject("", mesh.client(), MESH_PROJECT_NAME, instantFolder, true, n -> {
			StringFieldImpl fieldFromFragment = n.getFields().getStringField("from_fragment");
			assertThat(fieldFromFragment).as("Field from fragment").isNotNull();
			assertThat(fieldFromFragment.getString()).as("Field from fragment").isEqualTo("Instant Folder");
		}));
	}
}
