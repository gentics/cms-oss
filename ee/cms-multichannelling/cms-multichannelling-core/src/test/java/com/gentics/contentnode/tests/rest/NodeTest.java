package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.getPermPattern;
import static com.gentics.contentnode.rest.util.MiscUtils.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for node management REST API
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class NodeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static UserGroup group;
	private static SystemUser user;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
	}

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			for (Node node : t.getObjects(Node.class, select("SELECT id FROM node ORDER BY id DESC", DBUtils.IDS))) {
				node.delete();
			}
		});
	}

	/**
	 * Test creating nodes
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		String nodeName = "Node name";
		String host = "node.host.com";
		NodeLoadResponse response = assertRequiredPermissions(group, user, () -> {
			com.gentics.contentnode.rest.model.Node node = new com.gentics.contentnode.rest.model.Node();
			node.setName(nodeName);
			node.setHost(host);
			node.setUtf8(true);
			node.setEditorVersion(1);
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);
			return new NodeResourceImpl().add(request);
		}, Triple.of(PermHandler.TYPE_CONTENTNODE, 1, PermHandler.PERM_VIEW), Triple.of(PermHandler.TYPE_CONTENTNODE, 1, PermHandler.PERM_NODE_CREATE));
		operate(t -> {
			Node node = t.getObject(Node.class, response.getNode().getId());
			assertThat(node).as("Node").hasFieldOrPropertyWithValue("host", host);
			assertThat(node.getFolder()).as("Root folder").hasFieldOrPropertyWithValue("name", nodeName);
		});
	}

	/**
	 * Test loading nodes readonly
	 * @throws NodeException
	 */
	@Test
	public void testGet() throws NodeException {
		Node node = supply(() -> createNode());
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		assertRequiredPermissions(group, user, () -> new NodeResourceImpl().get(Integer.toString(node.getId()), false),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW));
	}

	/**
	 * Test loading nodes for update
	 * @throws NodeException
	 */
	@Test
	public void testGetForUpdate() throws NodeException {
		Node node = supply(() -> createNode());
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		assertRequiredPermissions(group, user, () -> new NodeResourceImpl().get(Integer.toString(node.getId()), true),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_UPDATE));
	}

	/**
	 * Test deleting nodes
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		Node node = supply(() -> createNode());
		Integer rootFolderId = supply(() -> node.getFolder().getId());
		assertRequiredPermissions(group, user, () -> new NodeResourceImpl().delete(Integer.toString(node.getId()), 0),
				Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, rootFolderId, PermHandler.PERM_FOLDER_DELETE));

		Node deleted = execute(Node::reload, node);
		assertThat(deleted).as("Deleted node").isNull();
	}

	/**
	 * Test that deleting the master node is not possible, if a channel exists
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Es wurde ein Master-Node zum Löschen ausgewählt, von dem Channels erstellt wurden. Der Node kann nicht gelöscht werden.")
	public void testDeleteMasterNode() throws NodeException {
		Node node = supply(() -> createNode());
		supply(() -> createChannel(node, "Channel", "channel", "/"));

		operate(() -> new NodeResourceImpl().delete(Integer.toString(node.getId()), 0));
	}

	/**
	 * Test that deleting a node is not possible, if that would produce dangling constructs
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Ein oder mehr Nodes konnten nicht gelöscht werden da folgende Tagtypen ausschließlich mit diesen verknüpft sind:\n" + 
			"\n" + 
			"shorttext\n" + 
			"\n" + 
			"Um den Löschvorgang fortzusetzen müssen die Tagtypen zuvor neu verknüpft oder entfernt werden.")
	public void testDeleteDanglingConstructs() throws NodeException {
		Node node = supply(() -> createNode());
		supply(() -> createConstruct(node, ShortTextPartType.class, "shorttext", "text"));

		operate(() -> new NodeResourceImpl().delete(Integer.toString(node.getId()), 0));
	}

	/**
	 * Test listing nodes
	 * @throws NodeException
	 */
	@Test
	public void testList() throws NodeException {
		Node withPerm = supply(() -> createNode());
		Node withoutPerm = supply(() -> createNode());

		NodeLoadResponse withPermResponse = supply(() -> new NodeResourceImpl().get(Integer.toString(withPerm.getId()), false));

		operate(() -> setPermissions(TypePerms.node, withPerm.getFolder().getId(), Arrays.asList(group),
				getPermPattern(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true))), true));
		operate(() -> setPermissions(TypePerms.node, withoutPerm.getFolder().getId(), Arrays.asList(group),
				getPermPattern(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(false))), true));

		NodeList nodeList = supply(user, () -> new NodeResourceImpl().list(null, null, null, new PermsParameterBean().setPerms(true), null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node list").usingElementComparatorOnFields("id").containsOnly(withPermResponse.getNode());
		assertThat(nodeList.getPerms()).as("Permissions Map").containsOnlyKeys(withPerm.getId());
		assertThat(nodeList.getPerms().get(withPerm.getId())).as("Permissions for node").contains(Permission.view, Permission.read);
	}
}
