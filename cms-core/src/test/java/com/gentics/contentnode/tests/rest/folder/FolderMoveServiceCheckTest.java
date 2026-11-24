package com.gentics.contentnode.tests.rest.folder;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.FolderService;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.MultiFolderMoveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.TestFolderMoveCheckService;
import com.gentics.contentnode.testutils.TestFolderMoveCheckService.MoveCheckCall;

/**
 * Test cases for {@link FolderService#checkMoveToOtherNode(Folder, Folder, java.util.Collection)}, which allows
 * registered services to veto moving folders into other nodes.
 * The service implementation used in the tests is {@link TestFolderMoveCheckService}.
 */
public class FolderMoveServiceCheckTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Node otherNode;

	/**
	 * Root folders of the nodes
	 */
	private static Folder nodeFolder;

	private static Folder otherNodeFolder;

	private static SystemUser systemUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("source", "Source", PublishTarget.NONE));
		otherNode = supply(() -> createNode("other", "Other", PublishTarget.NONE));
		nodeFolder = supply(() -> node.getFolder());
		otherNodeFolder = supply(() -> otherNode.getFolder());
		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@After
	public void tearDown() throws NodeException {
		TestFolderMoveCheckService.reset();
		operate(() -> {
			for (Node n : Arrays.asList(node, otherNode)) {
				clear(n);
			}
		});
	}

	/**
	 * Test that the service is called with the moved folder, the target folder and the IDs of the whole moved folder structure,
	 * when a folder is moved into another node
	 * @throws NodeException
	 */
	@Test
	public void testServiceCalledWhenMovingToOtherNode() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Folder"));
		Folder subFolder = supply(() -> createFolder(folder, "Sub"));
		Folder subSubFolder = supply(() -> createFolder(subFolder, "SubSub"));
		Folder sibling = supply(() -> createFolder(nodeFolder, "Sibling"));

		TestFolderMoveCheckService.setHandler((f, target, subFolderIds) -> OpResult.OK);

		GenericResponse response = moveFolders(otherNodeFolder, folder);
		assertResponseOK(response);

		List<MoveCheckCall> calls = TestFolderMoveCheckService.getCalls();
		assertThat(calls).as("Service calls").hasSize(1);
		MoveCheckCall call = calls.get(0);
		assertThat(call.folderId).as("Checked folder").isEqualTo(folder.getId());
		assertThat(call.targetId).as("Target folder").isEqualTo(otherNodeFolder.getId());
		assertThat(call.subFolderIds).as("Folder structure").containsOnly(folder.getId(), subFolder.getId(), subSubFolder.getId())
				.doesNotContain(sibling.getId());

		assertThat(getFolderNodeId(folder)).as("Node of moved folder").isEqualTo(otherNode.getId());
		assertThat(getFolderNodeId(subSubFolder)).as("Node of moved subfolder").isEqualTo(otherNode.getId());
	}

	/**
	 * Test that the service is not called, when a folder is moved within the same node
	 * @throws NodeException
	 */
	@Test
	public void testServiceNotCalledWhenMovingWithinNode() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Folder"));
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));

		TestFolderMoveCheckService.setHandler((f, t, subFolderIds) -> OpResult.fail(Folder.class, "folder.move.localization.error"));

		GenericResponse response = moveFolders(target, folder);
		assertResponseOK(response);

		assertThat(TestFolderMoveCheckService.getCalls()).as("Service calls").isEmpty();
		assertThat(getMotherId(folder)).as("Mother of moved folder").isEqualTo(target.getId());
	}

	/**
	 * Test that a failed result of the service prevents moving the folder structure into another node
	 * @throws NodeException
	 */
	@Test
	public void testServiceVetoPreventsMove() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Folder"));
		Folder subFolder = supply(() -> createFolder(folder, "Sub"));

		TestFolderMoveCheckService.setHandler((f, target, subFolderIds) -> OpResult.fail(Folder.class, "folder.move.localization.error"));

		GenericResponse response = moveFolders(otherNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertThat(response.getMessages()).as("Response messages").isNotEmpty();

		assertThat(TestFolderMoveCheckService.getCalls()).as("Service calls").hasSize(1);
		assertThat(getFolderNodeId(folder)).as("Node of folder").isEqualTo(node.getId());
		assertThat(getMotherId(folder)).as("Mother of folder").isEqualTo(nodeFolder.getId());
		assertThat(getFolderNodeId(subFolder)).as("Node of subfolder").isEqualTo(node.getId());
	}

	/**
	 * Test that a service returning null does not prevent moving the folder
	 * @throws NodeException
	 */
	@Test
	public void testServiceReturningNullAllowsMove() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Folder"));

		TestFolderMoveCheckService.setHandler((f, target, subFolderIds) -> null);

		GenericResponse response = moveFolders(otherNodeFolder, folder);
		assertResponseOK(response);

		assertThat(TestFolderMoveCheckService.getCalls()).as("Service calls").hasSize(1);
		assertThat(getFolderNodeId(folder)).as("Node of moved folder").isEqualTo(otherNode.getId());
	}

	/**
	 * Test moving multiple folders into another node, where the service vetoes only one of them
	 * @throws NodeException
	 */
	@Test
	public void testServiceVetoForOneOfMultipleFolders() throws NodeException {
		Folder okFolder = supply(() -> createFolder(nodeFolder, "Ok"));
		Folder vetoedFolder = supply(() -> createFolder(nodeFolder, "Vetoed"));

		TestFolderMoveCheckService.setHandler((f, target, subFolderIds) -> {
			if (f.getId().equals(vetoedFolder.getId())) {
				return OpResult.fail(Folder.class, "folder.move.localization.error");
			}
			return OpResult.OK;
		});

		GenericResponse response = moveFolders(otherNodeFolder, okFolder, vetoedFolder);
		assertResponse(response, ResponseCode.FAILURE);

		assertThat(TestFolderMoveCheckService.getCalls()).as("Service calls").extracting(call -> call.folderId)
				.contains(vetoedFolder.getId());
		assertThat(getFolderNodeId(vetoedFolder)).as("Node of vetoed folder").isEqualTo(node.getId());
	}

	/**
	 * Test that the default implementation of {@link FolderService#checkMoveToOtherNode(Folder, Folder, java.util.Collection)} allows the move
	 * @throws NodeException
	 */
	@Test
	public void testDefaultImplementationAllowsMove() throws NodeException {
		FolderService defaultService = new FolderService() {
		};
		Folder folder = supply(() -> createFolder(nodeFolder, "Folder"));

		OpResult result = supply(() -> defaultService.checkMoveToOtherNode(folder, otherNodeFolder, Arrays.asList(folder.getId())));
		assertThat(result).as("Result").isNotNull();
		assertThat(result.isOK()).as("Result is OK").isTrue();
	}

	/**
	 * Move the given folders via the REST API
	 * @param target target folder
	 * @param folders folders to move
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse moveFolders(Folder target, Folder... folders) throws NodeException {
		return supply(systemUser, () -> {
			try {
				if (folders.length == 1) {
					FolderMoveRequest request = new FolderMoveRequest();
					request.setFolderId(target.getId());
					request.setForegroundTime(600);
					return getFolderResource().move(Integer.toString(folders[0].getId()), request);
				} else {
					MultiFolderMoveRequest request = new MultiFolderMoveRequest();
					request.setFolderId(target.getId());
					request.setForegroundTime(600);
					request.setIds(Arrays.stream(folders).map(f -> Integer.toString(f.getId())).collect(Collectors.toCollection(HashSet::new)));
					return getFolderResource().move(request);
				}
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
	}

	protected int getFolderNodeId(Folder folder) throws NodeException {
		return supply(t -> t.getObject(Folder.class, folder.getId()).getOwningNode().getId());
	}

	protected int getMotherId(Folder folder) throws NodeException {
		return supply(t -> t.getObject(Folder.class, folder.getId()).getMother().getId());
	}
}
