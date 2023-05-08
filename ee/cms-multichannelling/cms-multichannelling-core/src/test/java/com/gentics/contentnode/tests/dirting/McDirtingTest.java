package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Special cases for dirting in MC environments
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.WASTEBIN })
public class McDirtingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Node channel;

	private static Construct vtlConstruct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode("master", "Master", PublishTarget.FILESYSTEM));
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/", PublishTarget.FILESYSTEM));

		vtlConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("vtl");
			c.setMlId(1);
			c.setName("Velocity", 1);
			c.setIconName("icon");

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("vtl");
				p.setPartTypeId(getPartTypeId(VelocityPartType.class));
			}, false));

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("template");
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));
		}));
	}

	/**
	 * Special test case:
	 * <ol>
	 * <li>Create folder A in master, localize in channel</li>
	 * <li>Create folder B in master (inherited into channel)</li>
	 * <li>Create page A in folder A</li>
	 * <li>Create page B in folder B with dependency on page.folder of page A</li>
	 * <li>Run publish process (building dependencies)</li>
	 * <li>Exclude folder B from multichannelling</li>
	 * <li>Delete localized copy of folder A. This will trigger a dependency on page B (which does not exist in channel anymore)</li>
	 * </ol>
	 * @throws Exception
	 */
	@Test
	public void testMcDependency() throws Exception {
		// create template with velocity tag
		Template template = Trx.supply(() -> create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Template");
			t.setSource("<node vtltag>");
			TemplateTag tTag = create(TemplateTag.class, tag -> {
				tag.setConstructId(vtlConstruct.getId());
				tag.setEnabled(true);
				tag.setName("vtltag");
				tag.setPublic(true);
			}, false);
			t.getTemplateTags().put("vtltag", tTag);
		}));

		// create folder in master and localize it in channel
		Folder inheritedFolder = Trx.supply(() -> createFolder(node.getFolder(), "Inherited"));
		Folder localizedFolder = Trx.supply(() -> localize(inheritedFolder, channel));

		// create folder in master and exclude from MC
		Folder excludedFolder = Trx.supply(() -> createFolder(node.getFolder(), "Excluded"));

		// create page in inherited folder (source of dependency)
		Page modPage = Trx.supply(() -> update(createPage(inheritedFolder, template, "Source"), Page::publish));

		// create pag in excluded folder, that has dependency on modPage
		Trx.supply(() -> update(createPage(excludedFolder, template, "Target"), p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag("vtltag"), "template").getValueObject()
					.setValueText("$cms.imps.loader.getPage(" + modPage.getId() + ").folder.name");
			p.publish();
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		Trx.operate(() -> update(excludedFolder, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), true)));

		// delete the localized folder
		Trx.operate(() -> {
			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				update(localizedFolder, Folder::delete);
			}
		});
	}
}
