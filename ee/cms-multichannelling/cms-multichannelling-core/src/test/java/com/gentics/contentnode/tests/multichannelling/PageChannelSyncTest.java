package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for channel sync of pages
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class PageChannelSyncTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test channel
	 */
	private static Node channel;

	/**
	 * Test template
	 */
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/"));
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Test synchronization of pages, where contenttags use a construct that has
	 * multiple parts without keyword
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testSynchronizeWithEmptyKeywords() throws NodeException {
		// create the evil construct
		Construct construct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct");
			c.setName("construct", 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("");
				p.setName("Part 1", 1);
				p.setPartTypeId(getPartTypeId(HTMLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("");
				p.setName("Part 2", 1);
				p.setPartTypeId(getPartTypeId(NormalTextPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));
		}));

		// create test page
		Page masterPage = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));
		// create contenttag
		Trx.operate(() -> {
			update(masterPage, p -> {
				p.getContent().addContentTag(construct.getId()).setName("construct1");
			});
		});

		// localize test page
		Page localizedCopy = Trx.supply(() -> localize(masterPage, channel));

		// fill data to the localized copy
		Trx.operate(() -> {
			update(localizedCopy, p -> {
				ContentTag tag = p.getContentTag("construct1");
				for (Value v : tag.getValues()) {
					v.setValueText("data from local");
				}
			});
		});

		// push localized copy to master
		Trx.operate(() -> {
			update(localizedCopy, p -> {
				p.pushToMaster(node);
			});
		});

		// check whether the master has the data now
		Trx.operate(() -> {
			ContentTag tag = masterPage.getContentTag("construct1");
			for (Value v : tag.getValues()) {
				assertEquals("Check value in master", "data from local", v.getValueText());
			}
		});
	}

	/**
	 * Test synchronization of pages, where contenttags with the same name use
	 * different (but compatible) constructs
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testSynchronizeDifferentConstructs() throws NodeException {
		Construct diffOldConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("diffold");
			c.setName("diffold", 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("text");
				p.setName("Part 1", 1);
				p.setPartTypeId(getPartTypeId(HTMLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));
		}));

		Construct diffNewConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("diffnew");
			c.setName("diffnew", 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("text");
				p.setName("Part 1", 1);
				p.setPartTypeId(getPartTypeId(NormalTextPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));
		}));

		// create test page
		Page masterPage = Trx.supply(() -> createPage(node.getFolder(), template, "Page"));

		// localize test page
		Page localizedCopy = Trx.supply(() -> localize(masterPage, channel));

		// create contenttag in master
		Trx.operate(() -> {
			update(masterPage, p -> {
				ContentTag tag = p.getContent().addContentTag(diffOldConstruct.getId());
				tag.setName("diff");
				for (Value v : tag.getValues()) {
					v.setValueText("data in master");
				}
			});
		});

		// create different contenttag in channel
		Trx.operate(() -> {
			update(localizedCopy, p -> {
				ContentTag tag = p.getContent().addContentTag(diffNewConstruct.getId());
				tag.setName("diff");
				for (Value v : tag.getValues()) {
					v.setValueText("data in channel");
				}
			});
		});

		// push localized copy to master
		Trx.operate(() -> {
			update(localizedCopy, p -> {
				p.pushToMaster(node);
			});
		});

		// check whether the master has the data now
		Trx.operate(() -> {
			ContentTag tag = masterPage.getContentTag("diff");
			for (Value v : tag.getValues()) {
				assertEquals("Check value in master", "data in channel", v.getValueText());
			}
		});

	}
}
