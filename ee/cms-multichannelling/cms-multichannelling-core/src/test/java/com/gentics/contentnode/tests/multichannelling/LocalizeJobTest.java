package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.LocalizeJob;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for localizing objects using REST
 */
@GCNFeature(set={Feature.MULTICHANNELLING})
public class LocalizeJobTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;
	private static Page page;
	private static File file;
	private static File image;
	private static Folder folder;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));

		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template"));
		page = Trx.supply(() -> ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page"));
		file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "file.txt", "bla".getBytes()));
		try (InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg")) {
			byte[] data = IOUtils.toByteArray(inputStream);
			image = Trx.supply(() -> ContentNodeTestDataUtils.createImage(node.getFolder(), "blume.jpg", data));
		}
		folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
	}

	@Test
	public void testTemplate() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = LocalizeJob.process(Template.class, Arrays.asList(template.getId()), channel.getId(), 0);
			ContentNodeRESTUtils.assertResponseOK(response);
			template = TransactionManager.getCurrentTransaction().getObject(template);
		});

		Trx.operate(() -> {
			Map<Integer, Integer> channelSet = template.getChannelSet();
			assertTrue("Channelset must contain entry for node", channelSet.containsKey(0));
			assertTrue("Channelset must contain entry for channel", channelSet.containsKey(channel.getId()));
		});
	}

	@Test
	public void testPage() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = LocalizeJob.process(Page.class, Arrays.asList(page.getId()), channel.getId(), 0);
			ContentNodeRESTUtils.assertResponseOK(response);
			page = TransactionManager.getCurrentTransaction().getObject(page);
		});

		Trx.operate(() -> {
			Map<Integer, Integer> channelSet = page.getChannelSet();
			assertTrue("Channelset must contain entry for node", channelSet.containsKey(0));
			assertTrue("Channelset must contain entry for channel", channelSet.containsKey(channel.getId()));
		});
	}

	@Test
	public void testFile() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = LocalizeJob.process(File.class, Arrays.asList(file.getId()), channel.getId(), 0);
			ContentNodeRESTUtils.assertResponseOK(response);
			file = TransactionManager.getCurrentTransaction().getObject(file);
		});

		Trx.operate(() -> {
			Map<Integer, Integer> channelSet = file.getChannelSet();
			assertTrue("Channelset must contain entry for node", channelSet.containsKey(0));
			assertTrue("Channelset must contain entry for channel", channelSet.containsKey(channel.getId()));
		});
	}

	@Test
	public void testImage() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = LocalizeJob.process(ImageFile.class, Arrays.asList(image.getId()), channel.getId(), 0);
			ContentNodeRESTUtils.assertResponseOK(response);
			image = TransactionManager.getCurrentTransaction().getObject(image);
		});

		Trx.operate(() -> {
			Map<Integer, Integer> channelSet = image.getChannelSet();
			assertTrue("Channelset must contain entry for node", channelSet.containsKey(0));
			assertTrue("Channelset must contain entry for channel", channelSet.containsKey(channel.getId()));
		});
	}

	@Test
	public void testFolder() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = LocalizeJob.process(Folder.class, Arrays.asList(folder.getId()), channel.getId(), 0);
			ContentNodeRESTUtils.assertResponseOK(response);
			folder = TransactionManager.getCurrentTransaction().getObject(folder);
		});

		Trx.operate(() -> {
			Map<Integer, Integer> channelSet = folder.getChannelSet();
			assertTrue("Channelset must contain entry for node", channelSet.containsKey(0));
			assertTrue("Channelset must contain entry for channel", channelSet.containsKey(channel.getId()));
		});
	}
}
