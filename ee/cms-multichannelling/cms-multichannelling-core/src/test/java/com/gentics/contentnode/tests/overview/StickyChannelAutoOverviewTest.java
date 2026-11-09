package com.gentics.contentnode.tests.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;


/**
 * Test cases for automatic overviews with sticky channel
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUBLISH_CACHE })
@RunWith(value = Parameterized.class)
public class StickyChannelAutoOverviewTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	protected static Node node;

	protected static Node channel;

	protected static Template template;

	protected static Integer stickyConstructId;

	protected static Integer nonStickyConstructId;

	protected static Folder inheritedFolder;

	protected static Folder masterFolder;

	protected static Folder channelFolder;

	protected static Folder localFolder;

	protected static Map<MCTestedType, ObjectContainer> objectsMap = new HashMap<>();

	@BeforeClass
	public static void setup() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template"));

		// create sticky overview tagtype
		stickyConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, "stickyds", "ds"));
		Trx.consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentNodeTestDataUtils.update(t.getObject(Construct.class, id), c -> {
				Part part = c.getParts().get(0);
				OverviewPartSetting setting = new OverviewPartSetting(part);
				setting.setStickyChannel(true);
				setting.setTo(part);
			});
		}, stickyConstructId);

		// create non-sticky overview tagtype
		nonStickyConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, "nonstickyds", "ds"));

		// create inherited, localized and local folders
		inheritedFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Inherited"));
		masterFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Master"));
		channelFolder = Trx.supply(() -> ContentNodeTestDataUtils.update(ContentNodeTestDataUtils.localize(masterFolder, channel), f -> f.setName("Channel")));
		localFolder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Local", channel);

		// for each type, create inherited, localized and local objects in each of the folders
		for (MCTestedType type : MCTestedType.values()) {
			String namePostFix = type.toString();
			ObjectContainer objects = objectsMap.computeIfAbsent(type, key -> new ObjectContainer());

			// create objects in inherited folder
			objects.inheritedInInherited = Trx.supply(() -> type.publish(type.create(inheritedFolder, "Inherited-Inherited " + namePostFix, template)));
			LocalizableNodeObject<?> masterInInheritedFolder = Trx.supply(() -> type.publish(type.create(inheritedFolder, "Inherited-Master " + namePostFix, template)));
			objects.masterInInherited = masterInInheritedFolder;
			objects.channelInInherited = Trx.supply(() -> type.publish(ContentNodeTestDataUtils.update(type.localize(masterInInheritedFolder, channel),
					o -> o.setName("Inherited-Channel " + namePostFix))));
			objects.localInInherited = Trx.supply(() -> type.publish(type.create(inheritedFolder, "Inherited-Local " + namePostFix, template, channel)));

			// create objects in localized folder
			objects.inheritedInMaster = Trx.supply(() -> type.publish(type.create(masterFolder, "Master-Inherited " + namePostFix, template)));
			LocalizableNodeObject<?> masterInMasterFolder = Trx.supply(() -> type.publish(type.create(masterFolder, "Master-Master " + namePostFix, template)));
			objects.masterInMaster = masterInMasterFolder;
			objects.channelInMaster = Trx.supply(() -> type.publish(ContentNodeTestDataUtils.update(type.localize(masterInMasterFolder, channel),
					o -> o.setName("Master-Channel " + namePostFix))));
			objects.localInMaster = Trx.supply(() -> type.publish(type.create(masterFolder, "Master-Local " + namePostFix, template, channel)));

			// create local object in local folder
			objects.localInLocal = Trx.supply(() -> type.publish(type.create(localFolder, "Local-Local " + namePostFix, template, channel)));
		}
	}

	@Parameters(name = "{index}: sticky: {0}, type: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean stickyChannel : Arrays.asList(true, false)) {
			for (MCTestedType type : MCTestedType.values()) {
				data.add(new Object[] { stickyChannel, type });
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean stickyChannel;

	@Parameter(1)
	public MCTestedType type;

	@Test
	public void testGetOverviewObjects() throws NodeException {
		Page page = ContentNodeTestDataUtils.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			ContentTag tag = p.getContent().addContentTag(getConstructId());

			OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, "ds");
			Overview overview = overviewPartType.getOverview();

			overview.setObjectClass(type.getClazz());
			overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
			overview.setMaxObjects(0);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			overview.setRecursion(false);

			List<OverviewEntry> entries = overview.getOverviewEntries();
			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(inheritedFolder.getId());
				entry.setNodeId(node.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));

			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(masterFolder.getId());
				entry.setNodeId(node.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));

			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(inheritedFolder.getId());
				entry.setNodeId(channel.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));

			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(masterFolder.getId());
				entry.setNodeId(channel.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));

			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(localFolder.getId());
				entry.setNodeId(channel.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));
		});

		Trx.consume(p -> {
			try (RenderTypeTrx rtTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
				ContentTag tag = p.getContentTags().values().iterator().next();

				OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, "ds");
				Overview overview = overviewPartType.getOverview();

				ObjectContainer objects = objectsMap.get(type);
				assertNotNull("Did not find test objects for " + type, objects);

				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					List<NodeObject> selectedObjects = new ArrayList<>(overview.getSelectedObjects(node.getFolder(), null));

					// assert correct elements
					if (stickyChannel) {
						assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
								objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
								objects.localInLocal);
					} else {
						assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.inheritedInMaster, objects.masterInMaster);
					}

					// assert correct sorting
					assertThat(selectedObjects).isSortedAccordingTo((o1, o2) -> {
						return StringUtils.mysqlLikeCompare(((LocalizableNodeObject<?>) o1).getName(), ((LocalizableNodeObject<?>) o2).getName());
					});
				}

				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					List<NodeObject> selectedObjects = new ArrayList<>(overview.getSelectedObjects(node.getFolder(), null));

					// assert correct elements
					if (stickyChannel) {
						assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
								objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
								objects.localInLocal);
					} else {
						assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.channelInInherited, objects.localInInherited,
							objects.inheritedInMaster, objects.channelInMaster, objects.localInMaster, objects.localInLocal);
					}

					// assert correct sorting
					assertThat(selectedObjects).isSortedAccordingTo((o1, o2) -> {
						return StringUtils.mysqlLikeCompare(((LocalizableNodeObject<?>) o1).getName(), ((LocalizableNodeObject<?>) o2).getName());
					});
				}
			}
		}, page);
	}

	@Test
	public void testGetRecursiveOverviewObjects() throws NodeException {
		Page page = ContentNodeTestDataUtils.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			ContentTag tag = p.getContent().addContentTag(getConstructId());

			OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, "ds");
			Overview overview = overviewPartType.getOverview();

			overview.setObjectClass(type.getClazz());
			overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
			overview.setMaxObjects(0);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			overview.setRecursion(true);

			List<OverviewEntry> entries = overview.getOverviewEntries();
			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(node.getFolder().getId());
				entry.setNodeId(node.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));

			entries.add(ContentNodeTestDataUtils.create(OverviewEntry.class, entry -> {
				entry.setObjectId(node.getFolder().getId());
				entry.setNodeId(channel.getId());
				entry.setObjectOrder(entries.size() + 1);
			}, false));
		});

		Trx.consume(p -> {
			try (RenderTypeTrx rtTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
				ContentTag tag = p.getContentTags().values().iterator().next();

				OverviewPartType overviewPartType = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, "ds");
				Overview overview = overviewPartType.getOverview();

				ObjectContainer objects = objectsMap.get(type);
				assertNotNull("Did not find test objects for " + type, objects);

				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					List<NodeObject> selectedObjects = new ArrayList<>(overview.getSelectedObjects(node.getFolder(), null));

					// assert correct elements
					if (stickyChannel) {
						if (type == MCTestedType.folder) {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
									objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
									objects.localInLocal, inheritedFolder, masterFolder, channelFolder, localFolder);
						} else {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
									objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
									objects.localInLocal);
						}
					} else {
						if (type == MCTestedType.folder) {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.inheritedInMaster,
									objects.masterInMaster, inheritedFolder, masterFolder);
						} else {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.inheritedInMaster,
									objects.masterInMaster);
						}
					}

					// assert correct sorting
					assertThat(selectedObjects).isSortedAccordingTo((o1, o2) -> {
						return StringUtils.mysqlLikeCompare(((LocalizableNodeObject<?>) o1).getName(), ((LocalizableNodeObject<?>) o2).getName());
					});
				}

				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					List<NodeObject> selectedObjects = new ArrayList<>(overview.getSelectedObjects(node.getFolder(), null));

					// assert correct elements
					if (stickyChannel) {
						if (type == MCTestedType.folder) {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
									objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
									objects.localInLocal, inheritedFolder, masterFolder, channelFolder, localFolder);
						} else {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.masterInInherited, objects.channelInInherited,
									objects.localInInherited, objects.inheritedInMaster, objects.masterInMaster, objects.channelInMaster, objects.localInMaster,
									objects.localInLocal);
						}
					} else {
						if (type == MCTestedType.folder) {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.channelInInherited, objects.localInInherited,
									objects.inheritedInMaster, objects.channelInMaster, objects.localInMaster, objects.localInLocal, inheritedFolder, channelFolder,
									localFolder);
						} else {
							assertThat(selectedObjects).containsOnly(objects.inheritedInInherited, objects.channelInInherited, objects.localInInherited,
									objects.inheritedInMaster, objects.channelInMaster, objects.localInMaster, objects.localInLocal);
						}
					}

					// assert correct sorting
					assertThat(selectedObjects).isSortedAccordingTo((o1, o2) -> {
						return StringUtils.mysqlLikeCompare(((LocalizableNodeObject<?>) o1).getName(), ((LocalizableNodeObject<?>) o2).getName());
					});
				}
			}
		}, page);
	}

	/**
	 * Get the construct ID of the construct to be used
	 * @return construct ID
	 */
	protected int getConstructId() {
		return stickyChannel ? stickyConstructId : nonStickyConstructId;
	}

	protected static class ObjectContainer {
		protected NodeObject inheritedInInherited;
		protected NodeObject masterInInherited;
		protected NodeObject channelInInherited;
		protected NodeObject localInInherited;

		protected NodeObject inheritedInMaster;
		protected NodeObject masterInMaster;
		protected NodeObject channelInMaster;
		protected NodeObject localInMaster;

		protected NodeObject localInLocal;
	}
}
