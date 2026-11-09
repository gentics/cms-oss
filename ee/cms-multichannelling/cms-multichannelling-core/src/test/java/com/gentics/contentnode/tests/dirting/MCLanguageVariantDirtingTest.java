package com.gentics.contentnode.tests.dirting;

import static org.junit.Assert.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.util.FileUtil;

/**
 * Page dirting tests for changes of language variants in multichannelling environments
 */
@RunWith(value = Parameterized.class)
public class MCLanguageVariantDirtingTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	private DependentPage dep;
	private LanguageVariant langVar;

	private Node master;
	private Node channel;
	private Node subChannel;
	private Node sideChannel;
	private Template template;
	private ContentLanguage german;
	private ContentLanguage english;
	private Page dependentPage;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: Dependent Page {0}, Language Variant {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (DependentPage dep : DependentPage.values()) {
			for (LanguageVariant langVar : LanguageVariant.values()) {
				data.add(new Object[] {dep, langVar});
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param dep setting for the dependent page
	 * @param langVar setting for the languag variant
	 */
	public MCLanguageVariantDirtingTest(DependentPage dep, LanguageVariant langVar) {
		this.dep = dep;
		this.langVar = langVar;
	}

	@Before
	public void setup() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);

		final Map<String, Integer> languageIds = new HashMap<String, Integer>();
		DBUtils.executeStatement("SELECT id, code FROM contentgroup", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					languageIds.put(rs.getString("code"), rs.getInt("id"));
				}
			}
		});
		
		Transaction t = TransactionManager.getCurrentTransaction();
		master = ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.NONE);
		
		german = t.getObject(ContentLanguage.class, languageIds.get("de"), true);
		assertNotNull("German language must exist", german);
		german.getNodes().add(master);
		german.save();
		t.commit(false);
		german = t.getObject(ContentLanguage.class, german.getId());
		english = t.getObject(ContentLanguage.class, languageIds.get("en"), true);
		assertNotNull("English language must exist", english);
		english.getNodes().add(master);
		english.save();
		t.commit(false);
		english = t.getObject(ContentLanguage.class, english.getId());
		
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
		subChannel = ContentNodeTestDataUtils.createChannel(channel, "Subchannel", "subchannel", "/");
		sideChannel = ContentNodeTestDataUtils.createChannel(master, "Sidechannel", "sidechannel", "/");

		int hbsConstructId = ContentNodeTestDataUtils.createConstruct(master, HandlebarsPartType.class, "hbs", "hbs");

		template = t.createObject(Template.class);
		template.setName("Template");
		template.setSource("<node hbs>");
		template.addFolder(master.getFolder());
		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(hbsConstructId);
		templateTag.setEnabled(true);
		templateTag.setName("hbs");
		templateTag.setPublic(true);
		template.getTemplateTags().put("hbs", templateTag);
		template.save();
		t.commit(false);

		dependentPage = t.createObject(Page.class);
		dependentPage.setFolderId(master.getFolder().getId());
		dependentPage.setName("Dependent Page");
		dependentPage.setTemplateId(template.getId());
		dependentPage.setLanguage(german);
		dependentPage.getContentTag("hbs").getValues().getByKeyname("hbs").setValueText(FileUtil.stream2String(getClass().getResourceAsStream("languageset.hbs"), "UTF-8"));
		dependentPage.save();
		dependentPage.publish();
		t.commit(false);
	}

	/**
	 * Test dirting after a language variant is created
	 * @throws Exception
	 */
	@Test
	public void testLanguageVariantCreation() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page dependentPageInChannel = dependentPage;
		if (dep == DependentPage.LOCALIZED) {
			dependentPageInChannel = (Page)dependentPage.copy();
			dependentPageInChannel.setChannelInfo(channel.getId(), dependentPage.getChannelSetId());
			dependentPageInChannel.save();
			dependentPageInChannel.publish();
			t.commit(false);
		}

		Page langVariant = null;
		if (langVar == LanguageVariant.LOCALIZED) {
			langVariant = t.createObject(Page.class);
			langVariant.setFolderId(master.getFolder().getId());
			langVariant.setName("Language Variant");
			langVariant.setTemplateId(template.getId());
			langVariant.setContentsetId(dependentPage.getContentsetId());
			langVariant.setLanguage(english);
			langVariant.save();
			langVariant.publish();
			t.commit(false);
		}

		testContext.publish(false);
		t = testContext.startSystemUserTransaction();

		Page langVariantInChannel = null;
		switch (langVar) {
		case LOCALIZED:
			langVariantInChannel = (Page)langVariant.copy();
			langVariantInChannel.setChannelInfo(channel.getId(), langVariant.getChannelSetId());
			langVariantInChannel.save();
			langVariantInChannel.publish();
			t.commit(false);
			break;
		case LOCAL:
			langVariantInChannel = t.createObject(Page.class);
			langVariantInChannel.setFolderId(master.getFolder().getId());
			langVariantInChannel.setName("Language Variant");
			langVariantInChannel.setTemplateId(template.getId());
			langVariantInChannel.setContentsetId(dependentPage.getContentsetId());
			langVariantInChannel.setChannelInfo(channel.getId(), langVariantInChannel.getChannelSetId());
			langVariantInChannel.setLanguage(english);
			langVariantInChannel.save();
			langVariantInChannel.publish();
			t.commit(false);
			break;
		}

		testContext.waitForDirtqueueWorker();

		ContentNodeTestUtils.assertDirtedObjectsNonStrict(Page.class, master);
		ContentNodeTestUtils.assertDirtedObjects(Page.class, channel, dependentPageInChannel, langVariantInChannel);
		ContentNodeTestUtils.assertDirtedObjects(Page.class, subChannel, dependentPageInChannel, langVariantInChannel);
		ContentNodeTestUtils.assertDirtedObjectsNonStrict(Page.class, sideChannel);
	}

	/**
	 * Setting for the dependent page
	 */
	public static enum DependentPage {
		INHERITED,
		LOCALIZED
	}

	/**
	 * Setting for the created language variant
	 */
	public static enum LanguageVariant {
		LOCALIZED,
		LOCAL
	}
}
