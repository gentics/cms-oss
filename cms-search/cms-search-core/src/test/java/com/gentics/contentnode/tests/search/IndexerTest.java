package com.gentics.contentnode.tests.search;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.RolePermissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.util.PermPattern;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.ELASTICSEARCH, Feature.WASTEBIN, Feature.NICE_URLS })
@RunWith(value = Parameterized.class)
public class IndexerTest extends AbstractIndexerTest {
	private static final Logger log = LoggerFactory.getLogger(IndexerTest.class);
	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(map -> {
		map.setProperty(Indexer.THREADS_PARAM, "1");
	});


	private static Node node;

	private static Node channel;

	private static Node subchannel;

	private static Node sidechannel;

	private static Template template;

	private static SystemUser user;

	private static UserGroup group;

	private static Role role;

	@Parameter(0)
	public MCTestedType type;

	private static HashSet<Integer> roleIds;

	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		subchannel = supply(() -> createChannel(channel, "Subchannel", "subchannel", "/"));
		sidechannel = supply(() -> createChannel(node, "Sidechannel", "sidechannel", "/"));
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		user = supply(t -> t.getObject(SystemUser.class, 1));

		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));

		role = supply(() -> create(Role.class, r -> {
			r.setName("Role", 1);
			r.setDescription("Test Role", 1);
		}));

		roleIds = new HashSet<>(Arrays.asList(role.getId()));
	}

	@Before
	public void setup() throws NodeException {
		String esUrl = String.format("http://%s:%d/", elasticsearch.getHost(), elasticsearch.getMappedPort(9200));
		NodeConfigRuntimeConfiguration.getPreferences().setProperty(Indexer.ELASTICSEARCH_URL_PARAM, esUrl);

		Trx.operate(() -> {
			try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
				List<Node> toClean = new ArrayList<>();
				toClean.add(node);
				toClean.addAll(node.getAllChannels());
				for (Node n : toClean) {
				for (Folder f : n.getFolder().getChildFolders()) {
					f.delete(true);
				}
				for (Page p : n.getFolder().getPages()) {
					p.delete(true);
				}
				for (File f : n.getFolder().getFilesAndImages()) {
					f.delete(true);
				}
			}

				toClean.removeAll(Arrays.asList(node, channel, subchannel, sidechannel));
				for (Node node : toClean) {
					node.delete(true);
				}
			}
		});

		operate(() -> doSetPermissions(TypePerms.folder, node.getFolder().getId(), new SetPermsRequest()
				.setGroupId(group.getId()).setPerm(PermHandler.EMPTY_PERM).setRoleIds(Collections.emptySet())));

		operate(() -> PermHandler.setRolePermissions(role.getId(),
				new RolePermissions().setFilePerm(Permissions.get()).setPagePerm(0, Permissions.get())));

		group = execute(g -> {
			g = g.reload();
			for (UserGroup subGroup : g.getChildGroups()) {
				subGroup.delete();
			}
			return g.reload();
		}, group);
	}

	@Test
	public void testCreateInMaster() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			if (object instanceof NodeObjectWithAlternateUrls) {
				NodeObjectWithAlternateUrls altUrlObject = (NodeObjectWithAlternateUrls) object;
				altUrlObject = update(altUrlObject, upd -> {
					upd.setNiceUrl("/this/is/nice");
					upd.setAlternateUrls("/alternate/nice", "/alternate/nicer", "/alternate/nicest");
				});
				object = execute(NodeObject::reload, object);
			}
			return object;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testCreateInChannel() throws NodeException {
		indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template, channel));
		}, object -> {
			assertIndexed(object, body(object, channel, subchannel));
		});
	}

	@Test
	public void testUpdate() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			object = update(object, upd -> {
				upd.setName("mod" + upd.getName());
			});
			object = execute(LocalizableNodeObject::reload, object);
			return object;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testDelete() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(o -> o.delete(true), object);
			return object;
		}, obj -> {
			assertNotIndexed(obj);
		});
	}

	@Test
	public void testDeleteIntoWastebin() throws NodeException {
		AtomicInteger objectCount = new AtomicInteger(0);
		indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			objectCount.set(getObjectCount(IndexType.wrap(object).get()));
			consume(NodeObject::delete, object);
			return object;
		}, object -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
			assertThat(getObjectCount(IndexType.wrap(object).get())).as("Object count").isEqualTo(objectCount.get());
		});
	}

	@Test
	public void testRemoveFromWastebin() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
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
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(NodeObject::delete, object);
			consume(obj -> {
				try (WastebinFilter f = WastebinFilter.get(true, node)) {
					obj.reload().restore();
				}
			}, object);
			return object;
		}, obj -> {
			assertIndexed(obj, body(obj, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testLocalize() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, sidechannel));
			assertIndexed(localized, body(localized, channel, subchannel));
		});
	}

	@Test
	public void testDeleteLocalized() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);
			consume(o -> o.delete(true), localized);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, channel, subchannel, sidechannel));
			assertNotIndexed(localized);
		});
	}

	@Test
	public void testDeleteLocalizedIntoWastebin() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);
			consume(NodeObject::delete, localized);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, channel, subchannel, sidechannel));
			assertIndexed(localized, body(localized, channel, subchannel));
		});
	}

	@Test
	public void testRemoveLocalizedFromWastebin() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);
			consume(NodeObject::delete, localized);
			consume(obj -> {
				try (WastebinFilter f = WastebinFilter.get(true, node)) {
					obj.reload().delete(true);
				}
			}, localized);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, channel, subchannel, sidechannel));
			assertNotIndexed(localized);
		});
	}

	@Test
	public void testRestoreLocalized() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);
			consume(NodeObject::delete, localized);
			consume(obj -> {
				try (WastebinFilter f = WastebinFilter.get(true, node)) {
					obj.reload().restore();
				}
			}, localized);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, sidechannel));
			assertIndexed(localized, body(localized, channel, subchannel));
		});
	}

	@Test
	public void testDisinherit() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(m -> type.disinherit(m, channel), master);
			return master;
		}, master -> {
			assertIndexed(master, body(master, node, sidechannel));
		});
	}

	@Test
	public void testReinherit() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(m -> type.disinherit(m, channel), master);
			consume(m -> type.reinherit(m, channel), master);
			return master;
		}, master -> {
			assertIndexed(master, body(master, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testExclude() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(m -> type.exclude(m), master);
			return master;
		}, master -> {
			assertIndexed(master, body(master, node));
		});
	}

	@Test
	public void testInclude() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			consume(m -> type.exclude(m), master);
			consume(m -> type.include(m), master);
			return master;
		}, master -> {
			assertIndexed(master, body(master, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testMoveInNode() throws NodeException {
		Folder target = supply(() -> createFolder(node.getFolder(), "Target"));
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));
			LocalizableNodeObject<?> localized = execute(o -> type.localize(o, channel), master);

			try (Trx trx = new Trx(user)) {
				assertResponseCodeOk(type.move(master, target));
				trx.success();
			}

			master = Trx.execute(NodeObject::reload, master);
			localized = Trx.execute(NodeObject::reload, localized);
			return Pair.of(master, localized);
		}, pair -> {
			LocalizableNodeObject<?> master = pair.getLeft();
			LocalizableNodeObject<?> localized = pair.getRight();
			assertIndexed(master, body(master, node, sidechannel));
			assertIndexed(localized, body(localized, channel, subchannel));
		});
	}

	@Test
	public void testMoveIntoChannel() throws NodeException {
		indexTest(() -> {
			LocalizableNodeObject<?> master = supply(() -> type.create(node.getFolder(), "Tested", template));

			try (Trx trx = new Trx(user)) {
				assertResponseCodeOk(type.move(master, node.getFolder(), channel));
				trx.success();
			}

			master = Trx.execute(NodeObject::reload, master);
			return master;
		}, master -> {
			assertIndexed(master, body(master, channel, subchannel));
		});
	}

	@Test
	public void testSetOnline() throws NodeException {
		// test is only valid for files and images
		if (type != MCTestedType.file && type != MCTestedType.image) {
			return;
		}
		LocalizableNodeObject<?> fileObject = indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			operate(() -> {
				FileOnlineStatus.setOnlineStatus(fileObject.getId(), node.getId(), true);
				FileOnlineStatus.setOnlineStatus(fileObject.getId(), subchannel.getId(), true);
			});
			return fileObject;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testSetOffline() throws NodeException {
		// test is only valid for files and images
		if (type != MCTestedType.file && type != MCTestedType.image) {
			return;
		}
		LocalizableNodeObject<?> fileObject = indexTest(() -> {
			LocalizableNodeObject<?> object = supply(() -> type.create(node.getFolder(), "Tested", template));
			operate(() -> {
				FileOnlineStatus.setOnlineStatus(object.getId(), node.getId(), true);
				FileOnlineStatus.setOnlineStatus(object.getId(), subchannel.getId(), true);
			});
			return object;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			operate(() -> {
				FileOnlineStatus.setOnlineStatus(fileObject.getId(), subchannel.getId(), false);
			});
			return fileObject;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});
	}

	@Test
	public void testPublish() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		Page page = (Page) indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			return update(page, Page::publish);
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testTakeOffline() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		Page page = (Page) indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			return update(page, Page::publish);
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.getTransaction().getObject(page, true).takeOffline();
				trx.success();
			}

			Page offline = execute(Page::reload, page);
			return offline;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testModifyPublished() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("modified").asBoolean()).as("page is modified").isFalse();
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				Page update = trx.getTransaction().getObject(page, true);
				update.setName("mod" + update.getName());
				update.save();
				trx.success();
			}
			Page modified = execute(LocalizableNodeObject::reload, page);
			return modified;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("modified").asBoolean()).as("page is modified").isTrue();
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testPlannedPublish() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not planned)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, plan page to be published at timestamp 3
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish(3, null);
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isTrue();
			assertThat(expected.get("publishAt").asInt()).as("publish at").isEqualTo(3);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 3, run publish process (should publish the page)
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
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testPlannedOffline() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not planned)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, publish page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 3, plan page to be published at timestamp 4
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).takeOffline(4);
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isTrue();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt").asInt()).as("offline at").isEqualTo(4);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 4, run publish process (should take page offline)
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
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("planned").asBoolean()).as("page is planned").isFalse();
			assertThat(expected.get("publishAt")).as("publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("offlineAt")).as("offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testAcceptQueuedPublish() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, queue page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).queuePublish(user);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 3, approve
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page approved = execute(LocalizableNodeObject::reload, page);
			return approved;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testDenyQueuedPublish() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, queue page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).queuePublish(user);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 3, deny request
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).clearQueue();
				trx.success();
			}
			Page denied = execute(LocalizableNodeObject::reload, page);
			return denied;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testQueuedPlannedPublish() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertThat(expected.get("queuedPublishAt")).as("page queued publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("queuedOfflineAt")).as("page queued offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, queue page to be published at 3
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).queuePublish(user, 3, null);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertThat(expected.get("queuedPublishAt").asInt()).as("page queued publish at").isEqualTo(3);
			assertThat(expected.get("queuedOfflineAt")).as("page queued offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testAcceptQueuedOffline() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, publish page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 3, queue page to got offline
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).queueOffline(user);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 4, approve
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).takeOffline();
				trx.success();
			}
			Page approved = execute(LocalizableNodeObject::reload, page);
			return approved;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testDenyQueuedOffline() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, publish page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 3, queue page to got offline
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).queueOffline(user);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 4, deny request
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(4);
				trx.getTransaction().getObject(page, true).clearQueue();
				trx.success();
			}
			Page denied = execute(LocalizableNodeObject::reload, page);
			return denied;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testQueuedPlannedOffline() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page with timestamp 1 (not queued)
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertThat(expected.get("queuedPublishAt")).as("page queued publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("queuedOfflineAt")).as("page queued offline at").isEqualTo(NullNode.instance);
			assertOnline(expected);
			assertIndexed(object, expected);
		});

		// at timestamp 2, publish page
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				trx.getTransaction().getObject(page, true).publish();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isFalse();
			assertThat(expected.get("queuedPublishAt")).as("page queued publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("queuedOfflineAt")).as("page queued offline at").isEqualTo(NullNode.instance);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});

		// at timestamp 3, queue page to got offline at timestamp 4
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(3);
				trx.getTransaction().getObject(page, true).queueOffline(user, 4);
				trx.success();
			}
			Page queued = execute(LocalizableNodeObject::reload, page);
			return queued;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("queued").asBoolean()).as("page is queued").isTrue();
			assertThat(expected.get("queuedPublishAt")).as("page queued publish at").isEqualTo(NullNode.instance);
			assertThat(expected.get("queuedOfflineAt").asInt()).as("page queued offline at").isEqualTo(4);
			assertOnline(expected, node.getId(), channel.getId(), subchannel.getId(), sidechannel.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testCustomCreationDate() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page at timestamp 1
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("created").asInt()).as("page created").isEqualTo(1);
			assertThat(expected.get("systemCreationDate").asInt()).as("page systemCreationDate").isEqualTo(1);
			assertThat(expected.get("customCreationDate")).as("page customCreationDate").isEqualTo(NullNode.instance);
			assertIndexed(object, expected);
		});

		// update page at timestamp 2 to have customCDate 3
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				Page editable = trx.getTransaction().getObject(page, true);
				editable.setCustomCDate(3);
				editable.save();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("created").asInt()).as("page created").isEqualTo(3);
			assertThat(expected.get("systemCreationDate").asInt()).as("page systemCreationDate").isEqualTo(1);
			assertThat(expected.get("customCreationDate").asInt()).as("page customCreationDate").isEqualTo(3);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testCustomEditDate() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.page) {
			return;
		}

		// create page at timestamp 1
		Page page = (Page) indexTest(() -> {
			LocalizableNodeObject<?> created = null;
			try (Trx trx = new Trx()) {
				trx.at(1);
				created = type.create(node.getFolder(), "Tested", template);
				trx.success();
			}
			return created;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("edited").asInt()).as("page edited").isEqualTo(1);
			assertThat(expected.get("systemEditDate").asInt()).as("page systemEditDate").isEqualTo(1);
			assertThat(expected.get("customEditDate")).as("page customEditDate").isEqualTo(NullNode.instance);
			assertIndexed(object, expected);
		});

		// update page at timestamp 2 to have customCDate 3
		indexTest(() -> {
			try (Trx trx = new Trx()) {
				trx.at(2);
				Page editable = trx.getTransaction().getObject(page, true);
				editable.setCustomEDate(3);
				editable.save();
				trx.success();
			}
			Page published = execute(LocalizableNodeObject::reload, page);
			return published;
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertThat(expected.get("edited").asInt()).as("page edited").isEqualTo(3);
			assertThat(expected.get("systemEditDate").asInt()).as("page systemEditDate").isEqualTo(2);
			assertThat(expected.get("customEditDate").asInt()).as("page customEditDate").isEqualTo(3);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testBlacklistedFile() throws NodeException {
		// test is only valid for pages
		if (type != MCTestedType.file) {
			return;
		}

		indexTest(() -> {
			try (InputStream data = getClass().getResourceAsStream("testfile.zip")) {
				return supply(() -> createFile(node.getFolder(), "testfile.zip", data, null));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}, object -> {
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testChangePermission() throws NodeException {
		LocalizableNodeObject<?> testObject = indexTest(() -> {
			// create a test object
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// grant read permission to the testgroup
			if (testObject instanceof Folder) {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getId(), new SetPermsRequest()
						.setGroupId(group.getId()).setPerm(new PermPattern().grant(PermType.read).toString())));
			} else {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
						new SetPermsRequest().setGroupId(group.getId())
								.setPerm(new PermPattern().grant(PermType.read).grant(PermType.readitems).toString())));
			}
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// revoke read permission to the testgroup
			if (testObject instanceof Folder) {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getId(), new SetPermsRequest()
						.setGroupId(group.getId()).setPerm(new PermPattern().revoke(PermType.read).toString())));
			} else {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
						new SetPermsRequest().setGroupId(group.getId()).setPerm(new PermPattern().revoke(PermType.readitems).toString())));
			}
			return testObject;
		}, object -> {
			// assert that testgroup is no longer allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testChangeRoleAssignment() throws NodeException {
		// test is not valid for folders
		if (type == MCTestedType.folder) {
			return;
		}

		// set role permissions
		operate(() -> PermHandler.setRolePermissions(role.getId(),
				new RolePermissions().setFilePerm(Permissions.get(PermType.readitems.getFileRoleBit())).setPagePerm(0,
						Permissions.get(PermType.readitems.getPageRoleBit()))));

		// grant view permission on the folder to the test group
		operate(() -> doSetPermissions(TypePerms.folder, node.getFolder().getId(), new SetPermsRequest()
				.setGroupId(group.getId()).setPerm(Permissions.get(PermType.read.getBit()).toString())));

		LocalizableNodeObject<?> testObject = indexTest(() -> {
			// create a test object
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			// assert that test group is not allowed to see test object
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// set the role for the group
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId())
							.setRoleIds(roleIds)));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// remove the role from the group
			operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
					new SetPermsRequest().setGroupId(group.getId())
							.setRoleIds(Collections.emptySet())));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testChangeRolePermission() throws NodeException {
		// test is not valid for folders
		if (type == MCTestedType.folder) {
			return;
		}

		// grant view permission on the folder to the test group and assign the role
		operate(() -> doSetPermissions(TypePerms.folder, node.getFolder().getId(),
				new SetPermsRequest().setGroupId(group.getId()).setRoleIds(roleIds)
						.setPerm(Permissions.get(PermType.read.getBit()).toString())));

		LocalizableNodeObject<?> testObject = indexTest(() -> {
			// create a test object
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			// assert that test group is not allowed to see test object
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// change the role permissions
			operate(() -> PermHandler.setRolePermissions(role.getId(),
					new RolePermissions().setFilePerm(Permissions.get(PermType.readitems.getFileRoleBit())).setPagePerm(0,
							Permissions.get(PermType.readitems.getPageRoleBit()))));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// change the role permissions
			operate(() -> PermHandler.setRolePermissions(role.getId(),
					new RolePermissions().setFilePerm(Permissions.get()).setPagePerm(0, Permissions.get())));
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testCreateSubGroup() throws NodeException {
		LocalizableNodeObject<?> testObject = indexTest(() -> {
			// create a test object
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// grant read permission to the testgroup
			if (testObject instanceof Folder) {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getId(), new SetPermsRequest()
						.setGroupId(group.getId()).setPerm(new PermPattern().grant(PermType.read).toString())));
			} else {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
						new SetPermsRequest().setGroupId(group.getId())
								.setPerm(new PermPattern().grant(PermType.read).grant(PermType.readitems).toString())));
			}
			return testObject;
		}, object -> {
			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
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
			LocalizableNodeObject<?> object = pair.getLeft();
			UserGroup subGroup = pair.getRight();

			// assert that testgroup and new subgroup are allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId(), subGroup.getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testRemoveGroup() throws NodeException {
		LocalizableNodeObject<?> testObject = indexTest(() -> {
			// create a test object
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			// assert that testgroup is not allowed to see the testobject (and this is indexed)
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertNotGroupIds(expected, group.getId());
			assertIndexed(object, expected);
		});

		Pair<?, UserGroup> testPair = indexTest(() -> {
			UserGroup subGroup = supply(() -> create(UserGroup.class, gr -> {
				gr.setMotherId(group.getId());
				gr.setName("Subgroup");
			}));

			// grant read permission to the testgroup and all subgroups
			if (testObject instanceof Folder) {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getId(), new SetPermsRequest()
						.setGroupId(group.getId()).setSubGroups(true).setPerm(new PermPattern().grant(PermType.read).toString())));
			} else {
				operate(() -> doSetPermissions(TypePerms.folder, testObject.getParentObject().getId(),
						new SetPermsRequest().setGroupId(group.getId()).setSubGroups(true)
								.setPerm(new PermPattern().grant(PermType.read).grant(PermType.readitems).toString())));
			}
			return Pair.of(testObject, subGroup);
		}, pair -> {
			LocalizableNodeObject<?> object = pair.getLeft();
			UserGroup subGroup = pair.getRight();

			// assert that testgroup is allowed to see the testobject now
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId(), subGroup.getId());
			assertIndexed(object, expected);
		});

		indexTest(() -> {
			// remove the subgroup
			consume(UserGroup::delete, testPair.getRight());
			return testObject;
		}, object -> {
			// assert that subgroup is no longer allowed to see the testobject
			JsonNode expected = body(object, node, channel, subchannel, sidechannel);
			assertGroupIds(expected, group.getId());
			assertNotGroupIds(expected, testPair.getRight().getId());
			assertIndexed(object, expected);
		});
	}

	@Test
	public void testAddChannel() throws NodeException {
		LocalizableNodeObject<?> testObject = indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});

		indexTest(() -> {
			Node testChannel = supply(() -> createChannel(node, "Testchannel", "testchannel", "/"));
			return Pair.of(testObject, testChannel);
		}, pair -> {
			LocalizableNodeObject<?> object = pair.getLeft();
			Node testChannel = pair.getRight();
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel, testChannel));
		});
	}

	@Test
	public void testRemoveChannel() throws NodeException {
		Node testChannel = supply(() -> createChannel(node, "Testchannel", "testchannel", "/"));

		LocalizableNodeObject<?> testObject = indexTest(() -> {
			return supply(() -> type.create(node.getFolder(), "Tested", template));
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel, testChannel));
		});

		indexTest(() -> {
			consume(c -> c.delete(true), testChannel);
			return testObject;
		}, object -> {
			assertIndexed(object, body(object, node, channel, subchannel, sidechannel));
		});
	}
}
