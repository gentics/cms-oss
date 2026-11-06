package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for creating pages in a MC Environment
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class PageMCCreateTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node masterNode;

	private static Node channel;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		masterNode = supply(() -> createNode("hostname", "Node name", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		channel = supply(() -> createChannel(masterNode, "Channel", "channel", "/"));
		template = supply(() -> createTemplate(masterNode.getFolder(), "Template"));
	}

	/**
	 * Clean data
	 * @throws NodeException
	 */
	@After
	public void clean() throws NodeException {
		operate(() -> clear(channel));
		operate(() -> clear(masterNode));
	}

	/**
	 * Test creating a page in a folder, which is disinherited from a channel
	 * @throws NodeException
	 */
	@Test
	public void testCreatePageInDisinheritedFolder() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setName("Testfolder");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/testfolder");
		});
		folder = update(folder, f -> f.getTemplates().add(template));

		consume(f -> MCTestedType.folder.disinherit(f, channel), folder);

		try (Trx trx = new Trx()) {
			PageCreateRequest request = new PageCreateRequest();
			request.setFailOnDuplicate(true);
			request.setFileName("test.html");
			request.setNodeId(masterNode.getId());
			request.setFolderId(Integer.toString(folder.getId()));
			request.setTemplateId(template.getId());
			PageLoadResponse response = getPageResource().create(request);
			assertResponseOK(response);
			trx.success();
		}
	}
}
