package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createForm;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertDirtedObjects;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueueStats;
import com.gentics.contentnode.rest.model.ContentMaintenanceAction;
import com.gentics.contentnode.rest.model.ContentMaintenanceType;
import com.gentics.contentnode.rest.model.DirtQueueEntry;
import com.gentics.contentnode.rest.model.DirtQueueSummaryEntry;
import com.gentics.contentnode.rest.model.request.ContentMaintenanceActionRequest;
import com.gentics.contentnode.rest.model.response.DirtQueueEntryList;
import com.gentics.contentnode.rest.model.response.DirtQueueSummaryResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeSettingsResponse;
import com.gentics.contentnode.rest.model.response.PublishQueueCounts;
import com.gentics.contentnode.rest.model.response.PublishQueueResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.admin.ObjectCount;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

/**
 * Test cases for content maintenance using the REST API
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUBLISH_CACHE, Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class ContentMaintenanceTest {
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup group;

	private static UserGroup groupWithNodePermissions;

	private static Node nodeWithPermission;

	private static Node nodeWithoutPermission;

	private static Node channelWithPermission;

	private static Node channelWithoutPermission;

	private static Integer crId;

	private static int[] nodeIds;

	private static Map<Integer, Set<Integer>> folderIds = new HashMap<>();

	private static Map<Integer, Set<Integer>> fileIds = new HashMap<>();

	private static Map<Integer, Set<Integer>> pageIds = new HashMap<>();

	private static Map<Integer, Set<Integer>> formIds = new HashMap<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		groupWithNodePermissions = supply(() -> createUserGroup("Group with Node permissions", NODE_GROUP_ID));
		supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group, groupWithNodePermissions)));

		nodeWithPermission = supply(() -> createNode(Feature.FORMS));

		channelWithPermission = supply(() -> createChannel(nodeWithPermission, "Channel", "channel", "/", PublishTarget.CONTENTREPOSITORY));

		crId = supply(() -> channelWithPermission.getContentrepositoryId());

		Template template = supply(() -> createTemplate(nodeWithPermission.getFolder(), "Template"));

		nodeWithoutPermission = supply(() -> createNode(Feature.FORMS));
		channelWithoutPermission = supply(() -> createChannel(nodeWithoutPermission, "Channel2", "channel2", "/"));

		nodeIds = supply(() -> new int[] { nodeWithPermission.getId(), channelWithPermission.getId(),
				nodeWithoutPermission.getId(), channelWithoutPermission.getId() });

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission, nodeWithoutPermission, channelWithoutPermission)) {
				folderIds.computeIfAbsent(node.getId(), key -> new HashSet<>()).add(node.getFolder().getId());
			}
		});

		// create content in both nodes
		for (Node node : Arrays.asList(nodeWithPermission, nodeWithoutPermission)) {
			int nodeId = node.getId();
			int channelId = node.equals(nodeWithPermission) ? channelWithPermission.getId() : channelWithoutPermission.getId();

			// create 4 folders
			AtomicLong cDate = new AtomicLong(10 * 1000);
			for (String name : Arrays.asList("Folder1", "Folder2", "Folder3", "Folder4")) {
				operate(t -> {
					t.setTimestamp(cDate.getAndAdd(10 * 1000));
					Integer folderId = createFolder(node.getFolder(), name).getId();
					folderIds.computeIfAbsent(nodeId, key -> new HashSet<>()).add(folderId);
					folderIds.computeIfAbsent(channelId, key -> new HashSet<>()).add(folderId);
				});
			}

			// create 4 pages and publish them
			cDate.set(10 * 1000);
			for (String name : Arrays.asList("Page1", "Page2", "Page3", "Page4")) {
				operate(t -> {
					t.setTimestamp(cDate.getAndAdd(10 * 1000));
					Integer pageId = update(createPage(node.getFolder(), template, name), Page::publish).getId();
					pageIds.computeIfAbsent(nodeId, key -> new HashSet<>()).add(pageId);
					pageIds.computeIfAbsent(channelId, key -> new HashSet<>()).add(pageId);
				});
			}

			// create 4 files
			cDate.set(10 * 1000);
			for (String name : Arrays.asList("file1.txt", "file2.txt", "file3.txt", "file4.txt")) {
				operate(t -> {
					t.setTimestamp(cDate.getAndAdd(10 * 1000));
					Integer fileId = createFile(node.getFolder(), name, "file contents".getBytes()).getId();
					fileIds.computeIfAbsent(nodeId, key -> new HashSet<>()).add(fileId);
					fileIds.computeIfAbsent(channelId, key -> new HashSet<>()).add(fileId);
				});
			}

			// create 4 forms
			cDate.set(10 * 1000);
			for (String name : Arrays.asList("form1.txt", "form2.txt", "form3.txt", "form4.txt")) {
				operate(t -> {
					t.setTimestamp(cDate.getAndAdd(10 * 1000));

					Form form = createForm(node.getFolder(), name);
					Integer formId = form.getId();

					form.publish();
					formIds.computeIfAbsent(nodeId, key -> new HashSet<>()).add(formId);
					formIds.computeIfAbsent(channelId, key -> new HashSet<>()).add(formId);
				});
			}
		}

		// remove all other permissions on the second group
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { groupWithNodePermissions.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(groupWithNodePermissions.getId()));

		// set only the permission to view the node
		operate(() -> setPermissions(Node.TYPE_NODE, nodeWithPermission.getFolder().getId(), Arrays.asList(groupWithNodePermissions),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			DBUtils.update("DELETE FROM dirtqueue");
			DBUtils.update("DELETE FROM publishqueue");
		});
	}

	@Test
	public void testListPublishQueue() throws NodeException {
		// republish everything
		operate(() -> {
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		PublishQueueStats.get().refresh();

		PublishQueueResponse response = testWithPermissions(target -> {
			return target.path("admin").path("content").path("publishqueue").request().get(PublishQueueResponse.class);
		});

		assertThat(response.getNodes()).containsOnly(
				entry(nodeWithPermission.getId(), new PublishQueueCounts().setFiles(new ObjectCount().setToPublish(4))
						.setFolders(new ObjectCount().setToPublish(5)).setPages(new ObjectCount().setToPublish(4))),
				entry(channelWithPermission.getId(),
						new PublishQueueCounts().setFiles(new ObjectCount().setToPublish(4))
								.setFolders(new ObjectCount().setToPublish(5))
								.setPages(new ObjectCount().setToPublish(4))));
	}

	@Test
	public void testDirtAll() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest().setAction(ContentMaintenanceAction.dirt);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request), GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 5);
				assertDirtedObjects(Page.class, node, 4);
				assertDirtedObjects(File.class, node, 4);
			}
			for (Node node : Arrays.asList(nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtNodes() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setNodes(
							new HashSet<>(Arrays.asList(nodeWithPermission.getId(), channelWithoutPermission.getId())));
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission)) {
				assertDirtedObjects(Folder.class, node, 5);
				assertDirtedObjects(Page.class, node, 4);
				assertDirtedObjects(File.class, node, 4);
			}
			for (Node node : Arrays.asList(channelWithPermission, nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtCDate() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setStart(20).setEnd(30);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 2);
				assertDirtedObjects(Page.class, node, 2);
				assertDirtedObjects(File.class, node, 2);
			}
			for (Node node : Arrays.asList(nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtCDateStart() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setStart(20);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 4);
				assertDirtedObjects(Page.class, node, 3);
				assertDirtedObjects(File.class, node, 3);
			}
			for (Node node : Arrays.asList(nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtCDateEnd() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setEnd(30);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 3);
				assertDirtedObjects(Page.class, node, 3);
				assertDirtedObjects(File.class, node, 3);
			}
			for (Node node : Arrays.asList(nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtTypes() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt)
					.setTypes(new HashSet<>(Arrays.asList(ContentMaintenanceType.file, ContentMaintenanceType.page, ContentMaintenanceType.form)));
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 4);
				assertDirtedObjects(File.class, node, 4);
			}

			// Forms are only handled in the master, they must be dirtied there but not in the channel.
			assertDirtedObjects(Form.class, nodeWithPermission, 4);
			assertDirtedObjects(Form.class, channelWithPermission, 0);

			for (Node node : Arrays.asList(nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
				assertDirtedObjects(Form.class, node, 0);
			}
		});
	}

	@Test
	public void testDirtAttributes() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt)
					.setAttributes(new HashSet<>(Arrays.asList("name", "description")));
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertThat(PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node))
						.containsOnly(entries(folderIds.get(node.getId()), "name", "description"));
				assertThat(PublishQueue.getDirtedObjectIdsWithAttributes(File.class, false, node))
						.containsOnly(entries(fileIds.get(node.getId()), "name", "description"));
				assertThat(PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, node))
						.containsOnly(entries(pageIds.get(node.getId()), "name", "description"));
			}
		});
	}

	@Test
	public void testDirtNoPublishCache() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertThat(getPublishQueueCount(node.getId(), Folder.TYPE_FOLDER, PublishQueue.Action.DEPENDENCY)).isEqualTo(5);
				assertThat(getPublishQueueCount(node.getId(), File.TYPE_FILE, PublishQueue.Action.DEPENDENCY)).isEqualTo(4);
				assertThat(getPublishQueueCount(node.getId(), Page.TYPE_PAGE, PublishQueue.Action.DEPENDENCY)).isEqualTo(4);

				assertThat(getPublishQueueCount(node.getId(), Folder.TYPE_FOLDER, PublishQueue.Action.MODIFY)).isEqualTo(0);
				assertThat(getPublishQueueCount(node.getId(), File.TYPE_FILE, PublishQueue.Action.MODIFY)).isEqualTo(0);
				assertThat(getPublishQueueCount(node.getId(), Page.TYPE_PAGE, PublishQueue.Action.MODIFY)).isEqualTo(0);
			}
		});
	}

	@Test
	public void testDirtPublishCache() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setClearPublishCache(true);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(nodeWithPermission, channelWithPermission)) {
				assertThat(getPublishQueueCount(node.getId(), Folder.TYPE_FOLDER, PublishQueue.Action.DEPENDENCY)).isEqualTo(0);
				assertThat(getPublishQueueCount(node.getId(), File.TYPE_FILE, PublishQueue.Action.DEPENDENCY)).isEqualTo(0);
				assertThat(getPublishQueueCount(node.getId(), Page.TYPE_PAGE, PublishQueue.Action.DEPENDENCY)).isEqualTo(0);

				assertThat(getPublishQueueCount(node.getId(), Folder.TYPE_FOLDER, PublishQueue.Action.MODIFY)).isEqualTo(5);
				assertThat(getPublishQueueCount(node.getId(), File.TYPE_FILE, PublishQueue.Action.MODIFY)).isEqualTo(4);
				assertThat(getPublishQueueCount(node.getId(), Page.TYPE_PAGE, PublishQueue.Action.MODIFY)).isEqualTo(4);
			}
		});
	}

	@Test
	public void testDirtContentRepository() throws NodeException {
		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.dirt).setContentRepositories(Collections.singleton(crId));
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			for (Node node : Arrays.asList(channelWithPermission)) {
				assertDirtedObjects(Folder.class, node, 5);
				assertDirtedObjects(Page.class, node, 4);
				assertDirtedObjects(File.class, node, 4);
			}
			for (Node node : Arrays.asList(nodeWithPermission, nodeWithoutPermission, channelWithoutPermission)) {
				assertDirtedObjects(Folder.class, node, 0);
				assertDirtedObjects(Page.class, node, 0);
				assertDirtedObjects(File.class, node, 0);
			}
		});
	}

	@Test
	public void testDelayAll() throws NodeException {
		// republish everything
		operate(() -> {
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.delay);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			assertThat(getPublishQueueCounts()).containsOnly(
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(true),
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false));
		});
	}

	@Test
	public void testRepublishDelayed() throws NodeException {
		// republish everything
		operate(() -> {
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		// delay everything
		operate(() -> {
			PublishQueue.delayDirtedObjects(nodeIds, true, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.delayDirtedObjects(nodeIds, true, File.TYPE_FILE, null, 0, 0);
			PublishQueue.delayDirtedObjects(nodeIds, true, Page.TYPE_PAGE, null, 0, 0);
		});

		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.publishDelayed);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			assertThat(getPublishQueueCounts()).containsOnly(
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(true),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(true));
		});
	}

	@Test
	public void testMarkPublished() throws NodeException {
		// republish everything
		operate(() -> {
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		testWithPermissions(target -> {
			ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
					.setAction(ContentMaintenanceAction.markPublished);
			return target.path("admin").path("content").path("publishqueue").request().post(Entity.json(request),
					GenericResponse.class);
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		operate(() -> {
			assertThat(getPublishQueueCounts()).containsOnly(
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(nodeWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Folder.TYPE_FOLDER)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(5).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(Page.TYPE_PAGE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false),
					new PublishQueueCount().setNodeId(channelWithoutPermission.getId()).setObjType(File.TYPE_FILE)
							.setAction(PublishQueue.Action.DEPENDENCY).setCount(4).setDelay(false));
		});
	}

	@Test
	public void testDirtQueueSummary() throws NodeException {
		operate(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			new QueueEntry(10, 15, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(11, 38, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(12, 4711, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);

			new QueueEntry(13, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory);

			new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).setFailed(new Exception());

			new QueueEntry(15, 0, Page.TYPE_PAGE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);
			new QueueEntry(16, 0, File.TYPE_FILE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);

			new QueueEntry(17).store(factory);
		});

		DirtQueueSummaryResponse response = testWithPermissions(target -> target.path("admin").path("content")
				.path("dirtqueue").path("summary").request().get(DirtQueueSummaryResponse.class));

		assertThat(response.getItems()).as("Summary entries").containsExactly(
				new DirtQueueSummaryEntry().setLabel("Abh\u00E4ngigkeitsberechnung").setCount(3),
				new DirtQueueSummaryEntry().setLabel("ContentRepository Wartungsaktion").setCount(1),
				new DirtQueueSummaryEntry().setLabel("Wartungsaktion").setCount(2),
				new DirtQueueSummaryEntry().setLabel("Publizierung").setCount(1));
	}

	@Test
	public void given_deleteDirtQueuesEntriesRequest_should_deleteFailedEntries() throws NodeException {
		generateDirtQueueEntries();
		executeDeleteDirtqueueEntriesRequestWithPermissionTest(false, 0, 100);

		operate(() -> {
			int num = DBUtils.select("SELECT COUNT(*) c FROM `dirtqueue`", null, DBUtils.firstInt("c"));

			assertThat(num).as("Number of dirtqueue entries").isEqualTo(2);
		});
	}


	@Test
	public void given_deleteDirtQueuesEntriesRequest_should_deleteAllFailedEntriesBetweenTimestamp()
			throws NodeException {
		generateDirtQueueEntries();
		executeDeleteDirtqueueEntriesRequestWithPermissionTest(true, 12, 16);

		operate(() -> {
			int num = DBUtils.select("SELECT COUNT(*) c FROM `dirtqueue`", null, DBUtils.firstInt("c"));

			assertThat(num).as("Number of dirtqueue entries").isEqualTo(5);
		});
	}

	@Test
	public void given_deleteDirtQueuesEntriesRequest_should_deleteNonFailedEntries()
			throws NodeException {
		generateDirtQueueEntries();
		executeDeleteDirtqueueEntriesRequestWithPermissionTest(false, null, null);

		operate(() -> {
			int num = DBUtils.select("SELECT COUNT(*) c FROM `dirtqueue`", null, DBUtils.firstInt("c"));

			assertThat(num).as("Number of dirtqueue entries").isEqualTo(2);
		});
	}

	@Test
	public void given_deleteDirtQueuesEntriesRequest_should_deleteEntriesFromStartTimestamp()
			throws NodeException {
		generateDirtQueueEntries();
		executeDeleteDirtqueueEntriesRequestWithPermissionTest(null, 13, null);

		operate(() -> {
			int num = DBUtils.select("SELECT COUNT(*) c FROM `dirtqueue`", null, DBUtils.firstInt("c"));

			assertThat(num).as("Number of dirtqueue entries").isEqualTo(3);
		});
	}


	private void executeDeleteDirtqueueEntriesRequestWithPermissionTest(Boolean failed, Integer start,
			Integer end) throws NodeException {
		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin")
					.path("content")
					.path("dirtqueue")
					.queryParam("failed", failed)
					.queryParam("start", start)
					.queryParam("end", end)
					.request()
					.delete(GenericResponse.class);

			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});
	}

	private void generateDirtQueueEntries() throws NodeException {
		operate(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();

			new QueueEntry(10, 15, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(11, 38, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(12, 4711, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(13, -1, -1, Events.DATACHECK_CR, new String[]{}, 0, null)
					.store(factory);
			new QueueEntry(14, 0, Page.TYPE_PAGE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory).setFailed(new NodeException("Unable to delete"));
			new QueueEntry(15, 0, File.TYPE_FILE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory).setFailed(new NodeException("Unable to delete"));
			new QueueEntry(16).store(factory);
		});
	}

	@Test
	public void testDirtQueue() throws NodeException {
		Exception exception = new Exception();
		StringWriter writer = new StringWriter();
		exception.printStackTrace(new PrintWriter(writer));
		String failReason = writer.toString();

		int firstEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			int id = new QueueEntry(10, 15, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory).getId();
			new QueueEntry(11, 38, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(12, 4711, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);

			new QueueEntry(13, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory);

			new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).setFailed(exception);

			new QueueEntry(15, 0, Page.TYPE_PAGE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);
			new QueueEntry(16, 0, File.TYPE_FILE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);

			new QueueEntry(17).store(factory);

			return id;
		});

		DirtQueueEntryList response = testWithPermissions(target -> target.path("admin").path("content").path("dirtqueue").request()
				.get(DirtQueueEntryList.class));

		assertThat(response.getItems()).as("Dirt queue entries").containsExactly(
				new DirtQueueEntry().setId(firstEntryId++).setObjType(Folder.TYPE_FOLDER).setObjId(15).setTimestamp(10)
						.setLabel("Abh\u00E4ngigkeitsberechnung").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(Folder.TYPE_FOLDER).setObjId(38).setTimestamp(11)
						.setLabel("Abh\u00E4ngigkeitsberechnung").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(Folder.TYPE_FOLDER).setObjId(4711)
						.setTimestamp(12).setLabel("Abh\u00E4ngigkeitsberechnung").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(-1).setObjId(-1).setTimestamp(13)
						.setLabel("ContentRepository Wartungsaktion").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(-1).setObjId(-1).setTimestamp(14)
						.setLabel("ContentRepository Wartungsaktion").setFailed(true).setFailReason(failReason),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(Page.TYPE_PAGE).setObjId(0).setTimestamp(15)
						.setLabel("Wartungsaktion").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(File.TYPE_FILE).setObjId(0).setTimestamp(16)
						.setLabel("Wartungsaktion").setFailed(false),
				new DirtQueueEntry().setId(firstEntryId++).setObjType(-1).setObjId(-1).setTimestamp(17)
						.setLabel("Publizierung").setFailed(false));
	}

	@Test
	public void testDirtQueueFailed() throws NodeException {
		Exception exception = new Exception();
		StringWriter writer = new StringWriter();
		exception.printStackTrace(new PrintWriter(writer));
		String failReason = writer.toString();

		int failedEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			new QueueEntry(10, 15, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(11, 38, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);
			new QueueEntry(12, 4711, Folder.TYPE_FOLDER, Events.UPDATE, null, 0, null)
					.store(factory);

			new QueueEntry(13, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory);

			int id = new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).setFailed(exception).getId();

			new QueueEntry(15, 0, Page.TYPE_PAGE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);
			new QueueEntry(16, 0, File.TYPE_FILE, Events.MAINTENANCE_PUBLISH, null, 0, null)
					.store(factory);

			new QueueEntry(17).store(factory);

			return id;
		});

		DirtQueueEntryList response = testWithPermissions(target -> target.path("admin").path("content")
				.path("dirtqueue").queryParam("failed", "true").request().get(DirtQueueEntryList.class));

		assertThat(response.getItems()).as("Dirt queue entries")
				.containsExactly(new DirtQueueEntry().setId(failedEntryId).setObjType(-1).setObjId(-1).setTimestamp(14)
						.setLabel("ContentRepository Wartungsaktion").setFailed(true).setFailReason(failReason));
	}

	@Test
	public void testDeleteFailed() throws NodeException {
		int failedEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			int id = new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).setFailed(new Exception()).getId();
			return id;
		});

		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue").path(Integer.toString(failedEntryId)).request()
					.delete(GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});

		operate(() -> {
			int num = DBUtils.select("SELECT COUNT(*) c FROM dirtqueue WHERE id = ?", stmt -> {
				stmt.setInt(1, failedEntryId);
			}, DBUtils.firstInt("c"));

			assertThat(num).as("Number of dirtqueue entries").isEqualTo(0);
		});
	}

	@Test(expected = ClientErrorException.class)
	public void testDeleteNotFailed() throws NodeException {
		int notFailedEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			int id = new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).getId();
			return id;
		});

		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue").path(Integer.toString(notFailedEntryId)).request()
					.delete(GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});
	}

	@Test(expected = NotFoundException.class)
	public void testDeleteUnknown() throws NodeException {
		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue")
					.path(Integer.toString(5711)).request().delete(GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});
	}

	@Test
	public void testRedoFailed() throws NodeException {
		int failedEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			int id = new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).setFailed(new Exception()).getId();
			return id;
		});

		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue").path(Integer.toString(failedEntryId)).path("redo").request()
					.put(Entity.json(""), GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});

		operate(() -> {
			QueueEntry entry = DBUtils.select("SELECT * FROM dirtqueue WHERE id = ?", stmt -> {
				stmt.setInt(1, failedEntryId);
			}, rs -> {
				if (rs.next()) {
					return new QueueEntry(rs);
				} else {
					return null;
				}
			});

			assertThat(entry).as("Dirtqueue entry").isNotNull().hasFieldOrPropertyWithValue("failed", false);
		});
	}

	@Test(expected = ClientErrorException.class)
	public void testRedoNotFailed() throws NodeException {
		int notFailedEntryId = supply(() -> {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();
			int id = new QueueEntry(14, -1, -1, Events.DATACHECK_CR, new String[] {}, 0, null)
					.store(factory).getId();
			return id;
		});

		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue").path(Integer.toString(notFailedEntryId)).path("redo").request()
					.put(Entity.json(""), GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});
	}

	@Test(expected = NotFoundException.class)
	public void testRedoUnknown() throws NodeException {
		testWithUpdatePermissions(target -> {
			GenericResponse response = target.path("admin").path("content").path("dirtqueue")
					.path(Integer.toString(5711)).path("redo").request().put(Entity.json(""), GenericResponse.class);
			if (response == null) {
				response = new GenericResponse(null, ResponseInfo.ok(""));
			}
			return response;
		});
	}

	@Test
	public void testReloadConfiguration() throws NodeException, URISyntaxException {
		// load the global node setting "reloaded" (which will not be set)
		NodeSettingsResponse settingsResponse = supply(() -> new NodeResourceImpl().settings(Integer.toString(nodeWithPermission.getId())));
		assertThat(settingsResponse.getData()).as("Node Settings").isNull();

		// set the config file
		java.io.File configFile = new java.io.File(ContentMaintenanceTest.class.getResource("reload_config.yml").toURI());
		System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), configFile.getAbsolutePath());

		// reload config
		testWithPermissions(target -> target.path("admin").path("config").path("reload").request().put(Entity.json(""),
				GenericResponse.class));

		// global node setting "reloaded" must be set to true now
		settingsResponse = supply(() -> new NodeResourceImpl().settings(Integer.toString(nodeWithPermission.getId())));
		assertThat(settingsResponse.getData()).as("Node Settings").isNotNull();
		assertThat(settingsResponse.getData().get("reloaded").asBoolean()).as("Reloaded").isTrue();
	}

	/**
	 * Perform the given action for the user and check for required permissions
	 * @param <T> type of response
	 * @param action performed action
	 * @return action response
	 * @throws NodeException
	 */
	protected <T extends GenericResponse> T testWithPermissions(Function<WebTarget, T> action) throws NodeException {
		return assertRequiredPermissions(group, "tester", "tester", restContext, action,
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENT_MAINTENANCE, 1, PermHandler.PERM_VIEW));
	}

	/**
	 * Perform the given action for the user and check for required permissions
	 * @param <T> type of response
	 * @param action performed action
	 * @return action response
	 * @throws NodeException
	 */
	protected <T extends GenericResponse> T testWithUpdatePermissions(Function<WebTarget, T> action) throws NodeException {
		return assertRequiredPermissions(group, "tester", "tester", restContext, action,
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENT_MAINTENANCE, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENT_MAINTENANCE, 1, PermHandler.PERM_CONTENT_MAINTENANCE_UPDATE));
	}

	/**
	 * Create map entries mapping the ids to an attribute set
	 * @param ids ids to be used as keys
	 * @param attributes attributes, to be used as set for every entry
	 * @return array of map entries
	 */
	@SuppressWarnings("unchecked")
	protected Map.Entry<Integer, Set<String>>[] entries(Set<Integer> ids, String... attributes) {
		return ids.stream().map(id -> entry(id, new HashSet<>(Arrays.asList(attributes))))
				.toArray(size -> new Map.Entry[size]);
	}

	/**
	 * Get the number of publishqueue entries
	 * @param nodeId node ID
	 * @param objType object type
	 * @param action dirt action
	 * @return number of entries
	 * @throws NodeException
	 */
	protected int getPublishQueueCount(int nodeId, int objType, PublishQueue.Action action) throws NodeException {
		return DBUtils.select(
				"SELECT COUNT(*) c FROM publishqueue WHERE channel_id = ? AND obj_type = ? AND action = ?", stmt -> {
					stmt.setInt(1, nodeId);
					stmt.setInt(2, objType);
					stmt.setString(3, action.toString());
				}, DBUtils.firstInt("c"));
	}

	/**
	 * Get a list of publish queue counts, grouped by nodeId, objType, action and delay
	 * @return list of counts for groups
	 * @throws NodeException
	 */
	protected List<PublishQueueCount> getPublishQueueCounts() throws NodeException {
		return DBUtils.select(
				"SELECT channel_id, obj_type, action, delay, COUNT(*) c FROM publishqueue GROUP BY channel_id, obj_type, action, delay",
				rs -> {
					List<PublishQueueCount> list = new ArrayList<>();
					while (rs.next()) {
						list.add(new PublishQueueCount(rs));
					}
					return list;
				});
	}

	/**
	 * Class containing the count for a group of publish queue entries
	 */
	protected static class PublishQueueCount {
		protected int nodeId;

		protected int objType;

		protected PublishQueue.Action action;

		protected boolean delay;

		protected int count;

		/**
		 * Create an empty instance
		 */
		public PublishQueueCount() {
		}

		/**
		 * Create an instance for the current record in the record set
		 * @param rs record set
		 * @throws SQLException
		 */
		public PublishQueueCount(ResultSet rs) throws SQLException {
			nodeId = rs.getInt("channel_id");
			objType = rs.getInt("obj_type");
			action = PublishQueue.Action.valueOf(rs.getString("action"));
			delay = rs.getBoolean("delay");
			count = rs.getInt("c");
		}

		/**
		 * Set the node ID
		 * @param nodeId node ID
		 * @return fluent API
		 */
		public PublishQueueCount setNodeId(int nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		/**
		 * Set the object type
		 * @param objType object type
		 * @return fluent API
		 */
		public PublishQueueCount setObjType(int objType) {
			this.objType = objType;
			return this;
		}

		/**
		 * Set the action
		 * @param action action
		 * @return fluent API
		 */
		public PublishQueueCount setAction(PublishQueue.Action action) {
			this.action = action;
			return this;
		}

		/**
		 * Set delay flag
		 * @param delay flag
		 * @return fluent API
		 */
		public PublishQueueCount setDelay(boolean delay) {
			this.delay = delay;
			return this;
		}

		/**
		 * Set the record count
		 * @param count count
		 * @return fluent API
		 */
		public PublishQueueCount setCount(int count) {
			this.count = count;
			return this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PublishQueueCount) {
				PublishQueueCount other = (PublishQueueCount) obj;
				return Objects.equals(nodeId, other.nodeId) && Objects.equals(objType, other.objType)
						&& Objects.equals(action, other.action) && Objects.equals(delay, other.delay)
						&& Objects.equals(count, other.count);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(nodeId, objType, action, delay, count);
		}

		@Override
		public String toString() {
			return String.format("nodeId: %d, objType: %d, action: %s, delay: %b, count: %d", nodeId, objType, action,
					delay, count);
		}
	}
}
