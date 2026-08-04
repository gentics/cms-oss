package com.gentics.contentnode.tests.dirting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test whether dependent pages are dirted correctly when using the
 * <code>#gtx_channel</code> helper.
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class ChannelHelperDirtingTest {
	private static final String HBS_PART_NAME = "hbs";
	private static final String TAG_NAME = "channeldirectivetest";
	private static final String PART_NAME_NODE = "targetnode";
	private static final String PART_NAME_PAGE = "pageurl";

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private Node master;
	private Node channel1;
	private Node channel2;
	private Folder srcFolder;
	private Page masterPage;
	private Page dependentPage;
	private Page dependencySourcePage;

	@Before
	public void setup() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		master = Creator.createNode("Master Node", "masternode", "/master/", "/master/", null);
		channel1 = Creator.createNode("Channel 1", "channel1", "/channel1/", "/channel1/", null, master);
		channel2 = Creator.createNode("Channel 2", "channel2", "/channel2/", "/channel2/", null, master);
		srcFolder = Creator.createFolder(master.getFolder(), "Home", "/home/");

		int hbsConstructId = ContentNodeTestDataUtils.createConstruct(master, HandlebarsPartType.class, HBS_PART_NAME, HBS_PART_NAME);
		Construct construct = t.getObject(Construct.class, hbsConstructId, true);
		List<Part> parts = construct.getParts();

		for (Part part : parts) {
			if (HBS_PART_NAME.equals(part.getKeyname())) {
				String tpl = "(Helper: {{#gtx_channel cms.tag.parts.%1$s}}<a href=\"{{cms.tag.parts.%2$s}}\">Page ID : {{cms.tag.parts.%2$s.id}}{{/gtx_channel}})"
						.formatted(PART_NAME_NODE, PART_NAME_PAGE);
				part.getDefaultValue().setValueText(tpl);

				break;
			}
		}

		Part part = Creator.createSimplePart(Part.NODE, PART_NAME_NODE, 1);

		part.setHidden(true);
		parts.add(part);

		part = Creator.createSimplePart(Part.URLPAGE, PART_NAME_PAGE, 1);
		part.setHidden(true);
		parts.add(part);
		construct.save();

		Template template = Creator.createTemplate("Template", "ChannelDirective dependency test: <node " + TAG_NAME + ">", srcFolder);
		TemplateTag tt = ContentNodeTestDataUtils.createTemplateTag(construct.getId(), TAG_NAME, true, true);

		template.getTags().put(TAG_NAME, tt);
		template.save();

		masterPage = ContentNodeTestDataUtils.createPage(srcFolder, template, "Master page");
		masterPage = t.getObject(masterPage, true);

		dependentPage = ContentNodeTestDataUtils.localize(masterPage, channel1);
		dependentPage = t.getObject(dependentPage, true);
		dependentPage.setName("Localized Page 1");
		dependentPage.setFilename("Localized_Page_1.html");
		dependentPage.save();

		dependencySourcePage = ContentNodeTestDataUtils.localize(masterPage, channel2);
		dependencySourcePage = t.getObject(dependencySourcePage, true);
		dependencySourcePage.setName("Localized Page 2");
		dependencySourcePage.setFilename("Localized_Page_2.html");
		dependencySourcePage.save();

		Tag tag = dependentPage.getContentTag(TAG_NAME);

		ContentNodeTestDataUtils.getPartType(PageURLPartType.class, tag, PART_NAME_PAGE).setTargetPage(dependencySourcePage);
		ContentNodeTestDataUtils.getPartType(NodePartType.class, tag, PART_NAME_NODE).setNode(channel2);

		dependentPage.save();
		dependentPage = t.getObject(dependentPage, true);

		masterPage.publish();
		dependentPage.publish();
		dependencySourcePage.publish();

		t.commit(false);
		testContext.publish(true);

		final Map<Integer, String> sources = new HashMap<>();

		DBUtils.executeStatement("SELECT page_id, source FROM publish WHERE page_id >= ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(masterPage.getId(), 0));
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					sources.put(rs.getInt("page_id"), rs.getString("source"));
				}
			}
		});
	}

	@Test
	public void testPageDirtedOnLinkTargetPublish() throws Exception {
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, channel1);

		assertTrue("Publish queue must contain no dirted objects", dirtedPageIds.isEmpty());

		dependencySourcePage.publish();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
		}

		List<Dependency> deps = DependencyManager.getDependenciesForObject(dependentPage, null, null);
		DependencyObject depSrcPage = new DependencyObject(dependencySourcePage);
		DependencyObject depPage = new DependencyObject(dependentPage);

		List<Dependency> expected = new ArrayList<>();
		for (String prop : Arrays.asList("id", "filename", "folder", "online")) {
			Dependency requiredDep = DependencyManager.createDependency(depSrcPage, prop, depPage, Events.UPDATE);
			requiredDep.addChannelId(channel1.getId());
			expected.add(requiredDep);
		}

		assertThat(deps).as("Page dependencies").usingElementComparatorOnFields("source", "dependent", "sourceProperty", "dependentProperties", "channelIds")
				.containsAll(expected);
	}
}
