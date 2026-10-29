package com.gentics.contentnode.tests.dirting;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.ContentNodeLanguageProviderWrapper;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for storing dependencies
 */
public class DependencySandboxTest  {

	private static final String FILEURL_TAGNAME = "imageurl";

	@Rule
	public DBTestContext testContext = new DBTestContext();

	protected Page page;

	@Before
	public void setUp() throws Exception {
		LanguageProviderFactory.reset();
		LanguageProviderFactory.getInstance().registerProviderWrapper(new ContentNodeLanguageProviderWrapper());

		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		testContext.getContext().startTransaction();
		DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] {1});
		TransactionManager.getCurrentTransaction().commit(false);
	}

	/**
	 * Test storing dependencies for an inherited page
	 * @throws Exception
	 */
	@Test
	public void testInheritedPage() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "master", "/", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "channel", "channel", "/");

		Template template = createTemplate("Template", "bla", master.getFolder());
		Page page = createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		testContext.getContext().startTransaction();
		testContext.publish(false);

		// check the dependencies in the DB
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check dependencies in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(master.getFolder(), "pub_dir", page, Events.UPDATE)
		);

		// check dependencies in channel
		assertThat(DependencyManager.getDependenciesForObject(page, channel.getId(), null)).as("Dependencies in channel").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(channel.getFolder(), "pub_dir", page, Events.UPDATE)
		);
	}

	/**
	 * Test localizing a page in a channel
	 * @throws Exception
	 */
	@Test
	public void testLocalizingPage() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "master", "/", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "channel", "channel", "/");

		Template template = createTemplate("Template", "bla", master.getFolder());
		Page page = createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		// run a publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// now localize the page in the channel
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// create a copy of the page
		Page localCopy = (Page) page.copy();
		localCopy.setChannelInfo(channel.getId(), page.getChannelSetId());
		localCopy.save();
		localCopy.publish();
		t.commit(false);

		// run a publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check dependencies for the master page in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").containsOnly(
				// master page depends on the template source in the master node
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				// publish path dependencies
				DependencyManager.createDependency(master.getFolder(), "pub_dir", page, Events.UPDATE)
		);

		// check dependencies for the channel page
		assertThat(DependencyManager.getDependenciesForObject(localCopy, channel.getId(), null)).as("Dependencies in channel").containsOnly(
				// channel page depends on the template source in the channel node
				DependencyManager.createDependency(template, "source", localCopy, Events.UPDATE),
				// publish path dependencies
				DependencyManager.createDependency(channel.getFolder(), "pub_dir", localCopy, Events.UPDATE)
		);
	}

	/**
	 * Test removing a localized copy
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizingPage() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "master", "/", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "channel", "channel", "/");

		Template template = createTemplate("Template", "bla", master.getFolder());
		Page page = createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		// run a publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// now localize the page in the channel
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// create a copy of the page
		Page localCopy = (Page) page.copy();
		localCopy.setChannelInfo(channel.getId(), page.getChannelSetId());
		localCopy.save();
		localCopy.publish();
		t.commit(false);

		// run a publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// now remove the localized copy
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		localCopy.delete();
		t.commit(false);

		// run a publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check dependencies in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(master.getFolder(), "pub_dir", page, Events.UPDATE)
		);

		// check dependencies in channel
		assertThat(DependencyManager.getDependenciesForObject(page, channel.getId(), null)).as("Dependencies in channel").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(channel.getFolder(), "pub_dir", page, Events.UPDATE)
		);
	}

	/**
	 * Test localizing the dependency (template)
	 * @throws Exception
	 */
	@Test
	public void testLocalizingTemplate() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "master", "/", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "channel", "channel", "/");

		Template template = createTemplate("Template", "bla", master.getFolder());
		Page page = createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		// run an initial publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// localize the template
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		Template localCopy = (Template) template.copy();
		localCopy.setChannelInfo(channel.getId(), template.getChannelSetId());
		localCopy.save();
		t.commit(false);

		// run another publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// check the dependencies in the DB
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check dependencies in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(master.getFolder(), "pub_dir", page, Events.UPDATE)
		);

		// check dependencies in channel
		assertThat(DependencyManager.getDependenciesForObject(page, channel.getId(), null)).as("Dependencies in channel").containsOnly(
				DependencyManager.createDependency(localCopy, "source", page, Events.UPDATE),
				DependencyManager.createDependency(channel.getFolder(), "pub_dir", page, Events.UPDATE)
		);
	}

	/**
	 * Test unlocalizing a localized dependency (template)
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizingTemplate() throws Exception {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "master", "/", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "channel", "channel", "/");

		Template template = createTemplate("Template", "bla", master.getFolder());
		Page page = createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		// run an initial publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// localize the template
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		Template localCopy = (Template) template.copy();
		localCopy.setChannelInfo(channel.getId(), template.getChannelSetId());
		localCopy.save();
		t.commit(false);

		// run another publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// now remove the localized template
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();
		localCopy.delete();
		t.commit(false);

		// run another publish process
		testContext.getContext().startTransaction();
		testContext.publish(false);

		// check the dependencies in the DB
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// check dependencies in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(master.getFolder(), "pub_dir", page, Events.UPDATE)
		);

		// check dependencies in channel
		assertThat(DependencyManager.getDependenciesForObject(page, channel.getId(), null)).as("Dependencies in channel").containsOnly(
				DependencyManager.createDependency(template, "source", page, Events.UPDATE),
				DependencyManager.createDependency(channel.getFolder(), "pub_dir", page, Events.UPDATE)
		);
	}

	/**
	 * Test if a dependency is added if a fileurl is a plink
	 * @throws Exception
	 */
	@Test
	public void testFileUrlDependency() throws Exception {

		//Ensure that a plink will be written by setting the contentnode linkway to "portal"
		testContext.getContext().startTransaction();
		LanguageProviderFactory.reset();
		NodeConfigRuntimeConfiguration.getDefault();

		Transaction t = TransactionManager.getCurrentTransaction();

		//create a master node that publishes into a contentrepository
		Node master = ContentNodeTestDataUtils.createNode("Master", "Master node", PublishTarget.CONTENTREPOSITORY);

		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(master.getFolder().getId());
		folder.setName("Test Folder");
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);

		Template template = createTemplate("Template", "bla <node " + FILEURL_TAGNAME + "1>", folder);
		t.commit(false);

		//create a image file in the cr
		File file = t.createObject(File.class);
		file.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		file.setName("blume-weigelie.jpg");
		file.setFolderId(master.getFolder().getId());
		file.save();
		t.commit(false);

		// create a page with a tagtype with a fileurl that links to the image in the cr
		page = createPage(folder, template, "Page", false);

		int fileUrlconstructId = ContentNodeTestDataUtils.createConstruct(master, FileURLPartType.class, FILEURL_TAGNAME, FILEURL_TAGNAME);
		ContentTag tag = page.getContent().addContentTag(fileUrlconstructId);
		tag.setName(FILEURL_TAGNAME + "1");
		tag.setEnabled(true);

		FileURLPartType fileUrlPartType = ContentNodeTestDataUtils.getPartType(FileURLPartType.class, tag, FILEURL_TAGNAME);
		fileUrlPartType.setTargetFile(file);
		page.save();
		t.commit(false);

		// do a publish run
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);
		testContext.publish(false);

		// ensure the plink is found in the page
		// get the published content of the source page
		final List<String> sources = new ArrayList<String>();

		DBUtils.executeStatement("SELECT source FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(page.getId(), 0));
				stmt.setInt(2, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					sources.add(rs.getString("source"));
				}
			}
		});

		boolean plinkFound = false;
		for (String s : sources) {
			if (s.matches(".*?<plink\\s[^><]*id=\"10008." + file.getId() + "\".*?/>")) {
				plinkFound = true;
			}
		}
		Assert.assertTrue("No plink found", plinkFound);

		// check dependencies in master
		assertThat(DependencyManager.getDependenciesForObject(page, master.getId(), null)).as("Dependencies in master").contains(
				DependencyManager.createDependency(file, "id", page, Events.UPDATE)
		);
	}

	/**
	 * Create a new template
	 * @param name name
	 * @param source source
	 * @param folder folder
	 * @return template
	 * @throws Exception
	 */
	protected Template createTemplate(String name, String source, Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setName(name);
		template.setSource(source);
		template.setFolderId(folder.getId());
		template.setMlId(1);
		template.save();
		t.commit(false);

		return t.getObject(Template.class, template.getId());
	}

	/**
	 * Create a new page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @return page
	 * @throws Exception
	 */
	protected Page createPage(Folder folder, Template template, String name) throws Exception {
		return createPage(folder, template,name, true);
	}

	/**
	 * Create a new page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @param readonly read only
	 * @return page
	 * @throws Exception
	 */
	protected Page createPage(Folder folder, Template template, String name, boolean readonly) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		page.setName(name);
		page.save();
		t.commit(false);

		return t.getObject(Page.class, page.getId(),!readonly);
	}
}
