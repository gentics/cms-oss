package com.gentics.contentnode.tests.search;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static com.gentics.contentnode.tests.utils.FormRESTUtils.getFormResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.util.PermPattern;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for indexing of forms
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.ELASTICSEARCH, Feature.WASTEBIN, Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormIndexerTest extends AbstractIndexerTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(map -> {
		map.setProperty(Indexer.THREADS_PARAM, "1");
	});

	private static Node node;

	private static Node channel;

	private static Node otherNode;

	private static SystemUser user;

	private static UserGroup group;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		ContentLanguage german = supply(() -> getLanguage("de"));

		node = supply(() -> createNode("master", "Master", PublishTarget.NONE, german));
		operate(() -> node.activateFeature(Feature.FORMS));
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		otherNode = supply(() -> createNode("other", "Other", PublishTarget.NONE, german));
		operate(() -> otherNode.activateFeature(Feature.FORMS));

		user = supply(t -> t.getObject(SystemUser.class, 1));

		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
	}

	@Before
	public void setup() throws NodeException {
		String esUrl = String.format("http://%s:%d/", elasticsearch.getHost(), elasticsearch.getMappedPort(9200));
		NodeConfigRuntimeConfiguration.getPreferences().setProperty(Indexer.ELASTICSEARCH_URL_PARAM, esUrl);

		System.out.printf("Elasticsearch URL: %s\n", esUrl);

		Trx.operate(() -> {
			try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
				for (Node n : Arrays.asList(node)) {
					for (Folder f : n.getFolder().getChildFolders()) {
						f.delete(true);
					}
					for (Form f : n.getFolder().getForms(null)) {
						f.delete(true);
					}
				}

				clear(otherNode);

				List<Node> toClean = new ArrayList<>();
				toClean.addAll(node.getAllChannels());
				toClean.removeAll(Arrays.asList(node, channel));
				for (Node node : toClean) {
					node.delete(true);
				}
			}
		});

		operate(() -> doSetPermissions(TypePerms.folder, node.getFolder().getId(), new SetPermsRequest()
				.setGroupId(group.getId()).setPerm(PermHandler.EMPTY_PERM).setRoleIds(Collections.emptySet())));

		group = execute(g -> {
			g = g.reload();
			for (UserGroup subGroup : g.getChildGroups()) {
				subGroup.delete();
			}
			return g.reload();
		}, group);
	}

	@Test
	public void testCreate() throws NodeException {
		indexTest(() -> {
			return supply(() -> createForm());
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});
	}

	@Test
	public void testUpdate() throws NodeException {
		indexTest(() -> {
			Form object = supply(() -> createForm());
			object = update(object, upd -> {
				upd.setName("mod" + upd.getName());
			});
			object = execute(Form::reload, object);
			return object;
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});
	}

	@Test
	public void testDelete() throws NodeException {
		indexTest(() -> {
			Form object = supply(() -> createForm());
			consume(o -> o.delete(true), object);
			return object;
		}, obj -> {
			assertNotIndexed(obj);
		});
	}

	@Test
	public void testDeleteIntoWastebin() throws NodeException {
		AtomicInteger objectCount = new AtomicInteger(0);
		Set<Integer> ids = new HashSet<>();
		indexTest(() -> {
			Form object = supply(() -> createForm());
			objectCount.set(IndexType.form.count().apply("de"));
			ids.clear();
			ids.addAll(IndexType.form.ids().apply("de"));
			consume(NodeObject::delete, object);
			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel));
			assertThat(IndexType.form.count().apply("de")).as("Object count").isEqualTo(objectCount.get());
			assertThat(IndexType.form.ids().apply("de")).as("Object IDS").containsOnlyElementsOf(ids);
		});
	}

	@Test
	public void testRemoveFromWastebin() throws NodeException {
		indexTest(() -> {
			Form object = supply(() -> createForm());
			consume(NodeObject::delete, object);
			consume(obj -> {
				try (WastebinFilter f = WastebinFilter.get(true, node)) {
					obj.reload().delete(true);
				}
			}, object);
			return object;
		}, obj -> {
			assertNotIndexed(obj);
		});
	}

	@Test
	public void testRestore() throws NodeException {
		indexTest(() -> {
			Form object = supply(() -> createForm());
			consume(NodeObject::delete, object);
			consume(obj -> {
				try (WastebinFilter f = WastebinFilter.get(true, node)) {
					obj.reload().restore();
				}
			}, object);
			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel));
		});
	}

	@Test
	public void testDisinheritFolder() throws NodeException {
		indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form object = supply(() -> createForm(folder));

			operate(() -> MCTestedType.folder.disinherit(folder, channel));

			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node));
		});
	}

	@Test
	public void testReinheritFolder() throws NodeException {
		indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form object = supply(() -> createForm(folder));

			operate(() -> MCTestedType.folder.disinherit(folder, channel));

			operate(() -> MCTestedType.folder.reinherit(folder, channel));

			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel));
		});
	}

	@Test
	public void testExcludeFolder() throws NodeException {
		indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form object = supply(() -> createForm(folder));

			operate(() -> MCTestedType.folder.exclude(folder));

			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node));
		});
	}

	@Test
	public void testIncludeFolder() throws NodeException {
		indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form object = supply(() -> createForm(folder));

			operate(() -> MCTestedType.folder.exclude(folder));

			operate(() -> MCTestedType.folder.include(folder));

			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel));
		});
	}

	@Test
	public void testDeleteFolderToWastebin() throws NodeException {
		Pair<Folder, Form> pair = indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form form = supply(() -> createForm(folder));
			return Pair.of(folder, form);
		}, p -> {
			assertIndexed(p.getRight(), body(p.getRight(), node, channel));
		});

		indexTest(() -> {
			consume(f -> f.delete(), pair.getLeft());
			return pair.getRight();
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel));
		});
	}

	@Test
	public void testDeleteFolder() throws NodeException {
		Pair<Folder, Form> pair = indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form form = supply(() -> createForm(folder));
			return Pair.of(folder, form);
		}, p -> {
			assertIndexed(p.getRight(), body(p.getRight(), node, channel));
		});

		indexTest(() -> {
			consume(f -> f.delete(true), pair.getLeft());
			return pair.getRight();
		}, obj -> {
			assertNotIndexed(obj);
		});
	}

	@Test
	public void testMoveFolderToOtherNode() throws NodeException {
		Pair<Folder, Form> pair = indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form form = supply(() -> createForm(folder));
			return Pair.of(folder, form);
		}, p -> {
			assertIndexed(p.getRight(), body(p.getRight(), node, channel));
		});

		indexTest(() -> {
			try (Trx trx = new Trx(user)) {
				assertResponseCodeOk(TestedType.folder.move(pair.getLeft(), otherNode.getFolder()));
				trx.success();
			}

			return pair.getRight();
		}, obj -> {
			assertIndexed(obj, body(obj, otherNode));
		});
	}

	@Test
	public void testMoveFormToOtherNode() throws NodeException {
		Pair<Folder, Form> pair = indexTest(() -> {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Subfolder"));
			Form form = supply(() -> createForm(folder));
			return Pair.of(folder, form);
		}, p -> {
			assertIndexed(p.getRight(), body(p.getRight(), node, channel));
		});

		// move form
		indexTest(() -> {
			FormResponse response = supply(() -> getFormResource().move(Integer.toString(pair.getRight().getId()),
					Integer.toString(otherNode.getFolder().getId())));
			assertResponseOK(response);

			return pair.getRight();
		}, obj -> {
			assertIndexed(obj, body(obj, otherNode));
		});
	}

	@Test
	public void testPublish() throws NodeException {
		Form form = indexTest(() -> {
			return supply(() -> createForm());
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});

		indexTest(() -> {
			return update(form, Form::publish);
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertOnline(expected, node.getId(), channel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testTakeOffline() throws NodeException {
		Form form = indexTest(() -> {
			return supply(() -> createForm());
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});

		indexTest(() -> {
			return update(form, Form::publish);
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.getTransaction().getObject(form, true).takeOffline();
				trx.success();
			}

			Form offline = execute(Form::reload, form);
			return offline;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testModifyPublished() throws NodeException {
		Form form = indexTest(() -> {
			Form created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = createForm();
				trx.success();
			}
			return created;
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(form, true).publish();
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("modified").asBoolean()).as("form is modified").isFalse();
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				Form update = trx.getTransaction().getObject(form, true);
				update.setName("mod" + update.getName());
				update.save();
				trx.success();
			}
			Form modified = execute(Form::reload, form);
			return modified;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("modified").asBoolean()).as("form is modified").isTrue();
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testPlannedPublish() throws NodeException {
		// create form with timestamp 1 (not planned)
		Form form = indexTest(() -> {
			Form created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = createForm();
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, plan form to be published at timestamp 3
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(form, true).publish(3, false);
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isTrue();
			assertThat(expected.get("publishAt").asInt()).as("publish at").isEqualTo(3);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 3, run publish process (should publish the form)
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				try {
					testContext.publish(3);
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected, node.getId(), channel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testPlannedOffline() throws NodeException {
		// create form with timestamp 1 (not planned)
		Form form = indexTest(() -> {
			Form created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = createForm();
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, publish form
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(form, true).publish();
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected, node.getId(), channel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 3, plan form to be published at timestamp 4
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(form, true).takeOffline(4);
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isTrue();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt").asInt()).as("offline at").isEqualTo(4);
			assertOnline(expected, node.getId(), channel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 4, run publish process (should take form offline)
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(4);
				try {
					testContext.publish(4);
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}
			Form published = execute(Form::reload, form);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel);
			assertThat(expected.get("planned").asBoolean()).as("form is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testChangePermission() throws NodeException {
		Form testObject = indexTest(() -> {
			// create a test object
			return supply(() -> createForm());
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// grant read permission to the testgroup
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId())
							.setPerm(new PermPattern().grant(PermType.read).grant(PermType.viewform).toString())));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel);
			assertGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// revoke read permission to the testgroup
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId()).setPerm(new PermPattern().revoke(PermType.viewform).toString())));
			return testObject;
		}, object -> {
			// assert that testgroup is no longer allowed to see the testobject now
			JsonNode expected = body(object, node, channel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testCreateSubGroup() throws NodeException {
		Form testObject = indexTest(() -> {
			// create a test object
			return supply(() -> createForm());
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// grant read permission to the testgroup
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId())
							.setPerm(new PermPattern().grant(PermType.read).grant(PermType.viewform).toString())));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel);
			assertGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// create subgroup, which inherits the permissions
			UserGroup subGroup = supply(() -> create(UserGroup.class, gr -> {
				gr.setMotherId(group.getId());
				gr.setName("Subgroup");
			}));

			return Pair.of(testObject, subGroup);
		}, pair -> {
			Form object = pair.getLeft();
			UserGroup subGroup = pair.getRight();

			// assert that testgroup and new subgroup are allowed to see the testobject now
			JsonNode expected = body(object, node, channel);
			assertGroupIds(expected, group.getId(), subGroup.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testRemoveGroup() throws NodeException {
		Form testObject = indexTest(() -> {
			// create a test object
			return supply(() -> createForm());
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		Pair<?, UserGroup> testPair = indexTest(() -> {
			UserGroup subGroup = supply(() -> create(UserGroup.class, gr -> {
				gr.setMotherId(group.getId());
				gr.setName("Subgroup");
			}));

			// grant read permission to the testgroup and all subgroups
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId()).setSubGroups(true)
							.setPerm(new PermPattern().grant(PermType.read).grant(PermType.viewform).toString())));
			return Pair.of(testObject, subGroup);
		}, pair -> {
			Form object = pair.getLeft();
			UserGroup subGroup = pair.getRight();

			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel);
			assertGroupIds(expected, group.getId(), subGroup.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// remove the subgroup
			consume(UserGroup::delete, testPair.getRight());
			return testObject;
		}, object -> {
			// assert that subgroup is no longer allowed to see the testobject
			JsonNode expected = body(object, node, channel);
			assertGroupIds(expected, group.getId());
			assertNotGroupIds(expected, testPair.getRight().getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testAddChannel() throws NodeException {
		Form testObject = indexTest(() -> {
			return supply(() -> createForm());
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});

		indexTest(() -> {
			Node testChannel = supply(() -> createChannel(node, "Testchannel", "testchannel", "/"));
			return Pair.of(testObject, testChannel);
		}, pair -> {
			Form object = pair.getLeft();
			Node testChannel = pair.getRight();
			assertIndexed(object, body(object, node, channel, testChannel));
		});
	}

	@Test
	public void testRemoveChannel() throws NodeException {
		Node testChannel = supply(() -> createChannel(node, "Testchannel", "testchannel", "/"));

		Form testObject = indexTest(() -> {
			return supply(() -> createForm());
		}, object -> {
			assertIndexed(object, body(object, node, channel, testChannel));
		});

		indexTest(() -> {
			consume(c -> c.delete(true), testChannel);
			return testObject;
		}, object -> {
			assertIndexed(object, body(object, node, channel));
		});
	}

	/**
	 * Create a form in the root folder of the node
	 * @return form
	 * @throws NodeException
	 */
	protected Form createForm() throws NodeException {
		return createForm(node.getFolder());
	}

	/**
	 * Create a form in the given folder
	 * @param folder folder
	 * @return form
	 * @throws NodeException
	 */
	protected Form createForm(Folder folder) throws NodeException {
		return create(Form.class, form -> {
			form.setFolderId(folder.getId());
			form.setName("Tested");
			form.setDescription("Test Form");
			form.setLanguages(Arrays.asList("de"));
			try {
				form.setData(AbstractIndexerTest.mapper.readValue(getClass().getResource("form_data.json"), ObjectNode.class));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		});
	}
}
