package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localizeImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for getting total usage info
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class TotalUsageInfoTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;
	private static int pageConstructId;
	private static int fileConstructId;
	private static int imageConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		pageConstructId = supply(() -> createConstruct(node, PageURLPartType.class, "pageurl", "url"));
		fileConstructId = supply(() -> createConstruct(node, FileURLPartType.class, "fileurl", "url"));
		imageConstructId = supply(() -> createConstruct(node, ImageURLPartType.class, "imageurl", "url"));
		template = create(Template.class, tmpl -> {
			tmpl.setName("TestTemplate");
			tmpl.setMlId(1);
			tmpl.getFolders().add(node.getFolder());
		}).build();
	}

	@After
	public void cleanData() throws NodeException {
		operate(() -> {
			clear(channel);
				clear(node);
		});
	}

	/**
	 * Test getting the total usage info for a page
	 * @throws NodeException
	 */
	@Test
	public void testPage() throws NodeException {
		// create target page in master
		Page targetMasterPage = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Target Page");
		}).build();

		// localize the target page
		Page targetChannelPage = supply(() -> localize(targetMasterPage, channel));

		// create source page in master
		create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Source Page");

			ContentTag tag = p.getContent().addContentTag(pageConstructId);
			getPartType(PageURLPartType.class, tag, "url").setTargetPage(targetMasterPage);
		}).build();

		// get usageinfo in master
		operate(() -> {
			TotalUsageResponse response = getPageResource().getTotalPageUsage(Arrays.asList(targetMasterPage.getId()), node.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in master").containsOnly(entry(targetMasterPage.getId(), new TotalUsageInfo().setTotal(1).setPages(1)));
		});

		// get usageinfo in channel
		operate(() -> {
			TotalUsageResponse response = getPageResource().getTotalPageUsage(Arrays.asList(targetChannelPage.getId()), channel.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in channel").containsOnly(entry(targetChannelPage.getId(), new TotalUsageInfo().setTotal(1).setPages(1)));
		});
	}

	/**
	 * Test getting the total usage info for a file
	 * @throws NodeException
	 */
	@Test
	public void testFile() throws NodeException {
		// create target file in master
		File targetMasterFile = Builder.create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).build();

		// localize the target file
		File targetChannelFile = supply(() -> localize(targetMasterFile, channel));

		// create source page in master
		create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Source Page");

			ContentTag tag = p.getContent().addContentTag(fileConstructId);
			getPartType(FileURLPartType.class, tag, "url").setTargetFile(targetMasterFile);
		}).build();

		// get usageinfo in master
		operate(() -> {
			TotalUsageResponse response = getFileResource().getTotalUsageInfo(Arrays.asList(targetMasterFile.getId()), node.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in master")
					.containsOnly(entry(targetMasterFile.getId(), new TotalUsageInfo().setTotal(1).setPages(1)
							.setTemplates(0).setFolders(0).setImages(0).setFiles(0)));
		});

		// get usageinfo in channel
		operate(() -> {
			TotalUsageResponse response = getFileResource().getTotalUsageInfo(Arrays.asList(targetChannelFile.getId()), channel.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in channel")
					.containsOnly(entry(targetChannelFile.getId(), new TotalUsageInfo().setTotal(1).setPages(1)
							.setTemplates(0).setFolders(0).setImages(0).setFiles(0)));
		});
	}

	/**
	 * Test getting the total usage info for an image
	 * @throws NodeException
	 */
	@Test
	public void testImage() throws NodeException {
		// create target image in master
		ImageFile targetMasterImage = Builder.create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		// localize the target image
		ImageFile targetChannelImage = supply(() -> localizeImage(channel, targetMasterImage));

		// create source page in master
		create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Source Page");

			ContentTag tag = p.getContent().addContentTag(imageConstructId);
			getPartType(ImageURLPartType.class, tag, "url").setTargetImage(targetMasterImage);
		}).build();

		// get usageinfo in master
		operate(() -> {
			TotalUsageResponse response = getImageResource().getTotalFileUsageInfo(Arrays.asList(targetMasterImage.getId()), node.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in master")
					.containsOnly(entry(targetMasterImage.getId(), new TotalUsageInfo().setTotal(1).setPages(1)
							.setTemplates(0).setFolders(0).setImages(0).setFiles(0)));
		});

		// get usageinfo in channel
		operate(() -> {
			TotalUsageResponse response = getImageResource().getTotalFileUsageInfo(Arrays.asList(targetChannelImage.getId()), node.getId());
			assertResponseOK(response);
			assertThat(response.getInfos()).as("Usage Info in channel")
					.containsOnly(entry(targetChannelImage.getId(), new TotalUsageInfo().setTotal(1).setPages(1)
							.setTemplates(0).setFolders(0).setImages(0).setFiles(0)));
		});
	}
}
