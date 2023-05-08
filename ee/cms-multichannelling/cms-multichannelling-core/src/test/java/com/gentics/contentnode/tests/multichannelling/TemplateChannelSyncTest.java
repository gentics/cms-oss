package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for channelsync of templates
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class TemplateChannelSyncTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node master;
	private static Node channel;

	/**
	 * Test data setup
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		master = Trx.supply(() -> createNode());
		channel = Trx.supply(() -> createChannel(master, "Channel", "channel", "/"));
	}

	/**
	 * Test pushing a template to the master from a localized folder
	 * @throws NodeException
	 */
	@Test
	public void testPushFromLocalizedFolder() throws NodeException {
		// create a folder and localize it
		Folder folder = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(master.getFolder().getId());
			f.setName("Testfolder");
		}));
		Trx.execute(f -> localize(f, channel), folder);

		// create local template in the localized folder
		Template template = Trx.execute(f -> create(Template.class, t -> {
			t.addFolder(f);
			t.setName("Template");
			t.setSource("");
			t.setChannelInfo(channel.getId(), null);
		}), folder);

		// reload folder
		folder = Trx.execute(f -> TransactionManager.getCurrentTransaction().getObject(f), folder);

		// the template must be visible in the channel only
		try (Trx trx = new Trx()) {
			try (ChannelTrx cTrx = new ChannelTrx(master)) {
				assertThat(folder.getTemplates()).as("Templates in master").isEmpty();
			}
			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				assertThat(folder.getTemplates()).as("Template in channel").containsOnly(template);
			}
		}

		// push template to master. we do this in the scope of the channel (which actually should not make any difference)
		Trx.operate(() -> {
			try (ChannelTrx trx = new ChannelTrx(channel)) {
				TransactionManager.getCurrentTransaction().getObject(template).pushToMaster(master);
			}
		});

		// reload folder
		folder = Trx.execute(f -> TransactionManager.getCurrentTransaction().getObject(f), folder);

		// the template must be visible in master and channel now
		try (Trx trx = new Trx()) {
			try (ChannelTrx cTrx = new ChannelTrx(master)) {
				assertThat(folder.getTemplates()).as("Templates in master").containsOnly(template);
			}
			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				assertThat(folder.getTemplates()).as("Template in channel").containsOnly(template);
			}
		}
	}
}
