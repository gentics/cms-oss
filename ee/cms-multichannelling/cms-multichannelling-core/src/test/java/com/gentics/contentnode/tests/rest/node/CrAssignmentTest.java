package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.clearNodeObjectCache;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryFragmentResourceImpl;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for assigning nodes to CRs
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR, Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT, Feature.DEVTOOLS })
public class CrAssignmentTest {
	public final static String CR_PROPERTY = "contentrepositoryId";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;
	private static Node channel1;
	private static Node channel2;
	private static Node sameHostNode;
	private static Node sameSegmentNode;
	private static ContentRepository meshCr;
	private static ContentRepository ppNmeshCr;
	private static ContentRepository cr1;
	private static ContentRepository cr2;
	private static ContentRepository mccr;

	private static UserGroup group;

	private static SystemUser user;

	/**
	 * Create test nodes and CRs
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> update(createNode("Node 1", "host1", "", "", false, false, false), upd -> {
			upd.setPubDirSegment(true);
		}));
		operate(() -> update(node.getFolder(), upd -> upd.setPublishDir("segment")));
		channel1 = supply(() -> createChannel(node, "Channel 1", "channel1", "/"));
		channel2 = supply(() -> createChannel(node, "Channel 2", "channel2", "/"));
		sameHostNode = supply(() -> update(createNode("Node 2", "host1", "", "", false, false, false), upd -> {
			upd.setPubDirSegment(true);
		}));
		operate(() -> update(sameHostNode.getFolder(), upd -> upd.setPublishDir("othersegment")));
		sameSegmentNode = supply(() -> update(createNode("Node 3", "host2", "", "", false, false, false), upd -> {
			upd.setPubDirSegment(true);
		}));
		operate(() -> update(sameSegmentNode.getFolder(), upd -> upd.setPublishDir("segment")));

		meshCr = supply(t -> t.getObject(ContentRepository.class, createMeshCR("meshhost", 4711, "meshproject")));
		ppNmeshCr = supply(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, createMeshCR("meshhost", 4711, "meshproject"));
			return update(cr, upd -> {
				upd.setProjectPerNode(true);
			});
		});
		cr1 = supply(() -> createContentRepository("CR1", false, false, "bla"));
		cr2 = supply(() -> createContentRepository("CR2", false, false, "bla"));
		mccr = supply(() -> createContentRepository("MCCR", true, false, "bla"));

		group = supply(() -> createUserGroup("Test", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("first", "last", null, "tester", "tester", Arrays.asList(group)));

		Synchronizer.start();
	}

	/**
	 * Reset all assignments
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Before
	public void setup() throws NodeException, PortalCacheException {
		operate(() -> DBUtils.executeUpdate("UPDATE node SET contentrepository_id = 0", null));
		clearNodeObjectCache();
		reload();
	}

	/**
	 * Test that required permissions are correctly checked
	 * @throws NodeException
	 */
	@Test
	public void testPermissions() throws NodeException {
		Integer rootFolderId = supply(() -> node.getFolder().getId());

		assertRequiredPermissions(group, user, () -> {
			com.gentics.contentnode.rest.model.Node n = new com.gentics.contentnode.rest.model.Node();
			n.setUtf8(true);
			n.setContentRepositoryId(cr1.getId());
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(n);
			return getNodeResource().save(Integer.toString(node.getId()), request);
		},
			Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW),
			Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_UPDATE),
			Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
			Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
			Triple.of(ContentRepository.TYPE_CONTENTREPOSITORIES, 1, PermHandler.PERM_VIEW),
			Triple.of(ContentRepository.TYPE_CONTENTREPOSITORY, cr1.getId(), PermHandler.PERM_VIEW),
			Triple.of(ContentRepository.TYPE_CONTENTREPOSITORY, cr1.getId(), PermHandler.PERM_CONTENTREPOSITORY_UPDATE));
	}

	/**
	 * Test that assigning the master to an mccr will assign all channels
	 * @throws NodeException
	 */
	@Test
	public void testNodeToMccr() throws NodeException {
		assertResponseOK(assign(node.getId(), mccr.getId()));

		assertCr(node, mccr);
		assertCr(channel1, mccr);
		assertCr(channel2, mccr);
	}

	/**
	 * Test that unassigning the master from an mccr will unassign all channels
	 * @throws NodeException
	 */
	@Test
	public void testUnassignMasterFromMccr() throws NodeException {
		assertResponseOK(assign(node.getId(), mccr.getId()));

		assertCr(node, mccr);
		assertCr(channel1, mccr);
		assertCr(channel2, mccr);

		assertResponseOK(assign(node.getId(), 0));

		assertCr(node, null);
		assertCr(channel1, null);
		assertCr(channel2, null);
	}

	/**
	 * Test assigning a master node to a CR
	 * @throws NodeException
	 */
	@Test
	public void testNodeToCr() throws NodeException {
		assertResponseOK(assign(node.getId(), cr1.getId()));

		assertCr(node, cr1);
		assertCr(channel1, null);
		assertCr(channel2, null);
	}

	/**
	 * Test assigning a master node to a CR
	 * @throws Exception 
	 */
	@Test
	public void testNodeToCrDevtoolsFragments() throws Exception {
		List<String> crFrs = supply(() -> IntStream.range(0, 4).mapToObj(i -> {
			ContentRepositoryFragmentModel crFr = new ContentRepositoryFragmentModel();
			crFr.setName("test_cr_fragment_" + i);
			try {
				new ContentRepositoryFragmentResourceImpl().add(crFr);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return crFr.getName();
		}).collect(Collectors.toList()));
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add("devtools_package");
			for (String crFr: crFrs) {
				resource.addCrFragment("devtools_package", crFr);
			}
			resource.addPackage(node.getId().toString(), "devtools_package");
			trx.success();
		}
		assertResponseOK(assign(node.getId(), cr1.getId()));

		assertCr(node, cr1);
		assertCr(channel1, null);
		assertCr(channel2, null);
		try (Trx trx = new Trx()) {
			assertThat(cr1.getAssignedFragments().stream().map(CrFragment::getName).sorted()).containsExactlyElementsOf(crFrs);
		}
	}

	/**
	 * Test assigning a channel to a CR
	 * @throws NodeException
	 */
	@Test
	public void testChannelToCr() throws NodeException {
		assertResponseOK(assign(channel1.getId(), cr1.getId()));

		assertCr(node, null);
		assertCr(channel1, cr1);
		assertCr(channel2, null);
	}

	/**
	 * Test that it is not possible to assign multiple nodes/channels to the same CR
	 * @throws NodeException
	 */
	@Test
	public void testNodeAndChannelToSameCr() throws NodeException {
		assertResponseOK(assign(node.getId(), cr1.getId()));
		assertResponse(assign(channel1.getId(), cr1.getId()), ResponseCode.INVALIDDATA, "node saving failed",
				new Message(Message.Type.CRITICAL, supply(() -> I18NHelper.get("cr_channel_master_no_assign"))));
	}

	/**
	 * Test assigning master node and channel to different CRs
	 * @throws NodeException
	 */
	@Test
	public void testNodeAndChannelToDifferentCrs() throws NodeException {
		assertResponseOK(assign(node.getId(), cr1.getId()));
		assertResponseOK(assign(channel1.getId(), cr2.getId()));

		assertCr(node, cr1);
		assertCr(channel1, cr2);
		assertCr(channel2, null);
	}

	/**
	 * Test assigning the master to a mesh CR, that does not use "projectPerNode" (channels must not be assigned)
	 * @throws NodeException
	 */
	@Test
	public void testMasterToMeshCr() throws NodeException {
		assertResponseOK(assign(node.getId(), meshCr.getId()));

		assertCr(node, meshCr);
		assertCr(channel1, null);
		assertCr(channel2, null);
	}

	/**
	 * Test assigning the master to a mesh CR, that uses "projectPerNode" (channels must also be assigned)
	 * @throws NodeException
	 */
	@Test
	public void testMasterToMeshCrProjectPerNode() throws NodeException {
		assertResponseOK(assign(node.getId(), ppNmeshCr.getId()));

		assertCr(node, ppNmeshCr);
		assertCr(channel1, ppNmeshCr);
		assertCr(channel2, ppNmeshCr);
	}

	/**
	 * Test unassigning the master from a mesh CR, that uses "projectPerNode" will not unassign all channels
	 * @throws NodeException
	 */
	@Test
	public void testUnassignMasterFromMeshCrProjectPerNode() throws NodeException {
		assertResponseOK(assign(node.getId(), ppNmeshCr.getId()));

		assertCr(node, ppNmeshCr);
		assertCr(channel1, ppNmeshCr);
		assertCr(channel2, ppNmeshCr);

		assertResponseOK(assign(node.getId(), 0));

		assertCr(node, null);
		assertCr(channel1, ppNmeshCr);
		assertCr(channel2, ppNmeshCr);
	}

	/**
	 * Test assigning only a channel to a mesh CR
	 * @throws NodeException
	 */
	@Test
	public void testChannelToMeshCr() throws NodeException {
		assertResponseOK(assign(channel2.getId(), meshCr.getId()));

		assertCr(node, null);
		assertCr(channel1, null);
		assertCr(channel2, meshCr);
	}

	/**
	 * Test assigning the master to mesh CR and a channel to another CR
	 * @throws NodeException
	 */
	@Test
	public void testCrAndMeshCr() throws NodeException {
		assertResponseOK(assign(node.getId(), meshCr.getId()));
		assertResponseOK(assign(channel1.getId(), cr1.getId()));

		assertCr(node, meshCr);
		assertCr(channel1, cr1);
		assertCr(channel2, null);
	}

	/**
	 * Test assigning the master to mesh CR and a channel to another CR
	 * @throws NodeException
	 */
	@Test
	public void testCrAndMeshCrProjectPerNode() throws NodeException {
		assertResponseOK(assign(node.getId(), ppNmeshCr.getId()));
		assertResponseOK(assign(channel1.getId(), cr1.getId()));

		assertCr(node, ppNmeshCr);
		assertCr(channel1, cr1);
		assertCr(channel2, ppNmeshCr);
	}

	/**
	 * Test conflict check for nodes with the same root segment when assigning to a mesh CR without projectPerNode
	 * @throws NodeException
	 */
	@Test
	public void testMeshCrConflictingSegment() throws NodeException {
		assertResponseOK(assign(node.getId(), meshCr.getId()));
		assertResponse(assign(sameSegmentNode.getId(), meshCr.getId()), ResponseCode.INVALIDDATA, "node saving failed",
				new Message(Message.Type.CRITICAL, supply(() -> I18NHelper.get("rest.node.conflict.meshcr", getNodeNames(node, sameSegmentNode)).toString())));
	}

	/**
	 * Test that assigning nodes with same root segment to a mesh CR with projectPerNode does not lead to a conflict
	 * @throws NodeException
	 */
	@Test
	public void testMeshCrNoSegmentConflict() throws NodeException {
		assertResponseOK(assign(node.getId(), ppNmeshCr.getId()));
		assertResponseOK(assign(sameSegmentNode.getId(), ppNmeshCr.getId()));
		assertResponseOK(assign(sameHostNode.getId(), ppNmeshCr.getId()));
	}

	/**
	 * Reload nodes/channels
	 * @throws NodeException
	 */
	protected void reload() throws NodeException {
		node = supply(node::reload);
		channel1 = supply(channel1::reload);
		channel2 = supply(channel2::reload);
	}

	/**
	 * Assert node assignment
	 * @param n node
	 * @param cr contentrepository (null if node is expected to not be assigned)
	 * @throws NodeException
	 */
	protected void assertCr(Node n, ContentRepository cr) throws NodeException {
		operate(() -> assertThat(n).as(I18NHelper.getName(n)).hasFieldOrPropertyWithValue(CR_PROPERTY, cr != null ? cr.getId() : 0));
	}

	/**
	 * Assign the node to a CR
	 * @param nodeId node ID
	 * @param crId CR ID
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse assign(int nodeId, int crId) throws NodeException {
		try {
			return supply(() -> {
				com.gentics.contentnode.rest.model.Node n = new com.gentics.contentnode.rest.model.Node();
				n.setUtf8(true);
				n.setContentRepositoryId(crId);
				NodeSaveRequest request = new NodeSaveRequest();
				request.setNode(n);
				return getNodeResource().save(Integer.toString(nodeId), request);
			});
		} finally {
			reload();
		}
	}

	/**
	 * Get comma separated list of names of the given nodes
	 * @param nodes nodes
	 * @return comma separated list of node names
	 * @throws NodeException
	 */
	protected String getNodeNames(Node...nodes) throws NodeException {
		Set<Node> nodeSet = new HashSet<>(Arrays.asList(nodes));
		List<String> nodeNames = new ArrayList<>();
		for (Node n : nodeSet) {
			nodeNames.add(supply(() -> I18NHelper.getName(n)));
		}
		return nodeNames.stream().collect(Collectors.joining(","));
	}
}
