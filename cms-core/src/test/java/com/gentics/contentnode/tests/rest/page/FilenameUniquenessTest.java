package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for filename uniqueness of pages
 */
public class FilenameUniquenessTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			t.commit();
		}
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			for (Page p : node.getFolder().getPages()) {
				p.delete(true);
			}
		});
	}

	/**
	 * Test setting page filename to a value, which is used by the published, but not current version of another page
	 * @throws NodeException
	 */
	@Test
	public void testPageWithOldVersion() throws NodeException {
		Page page1 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page A");
			p.setFilename("page_a.html");
		}), Page::publish));

		Page page2 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page B");
			p.setFilename("page_b.html");
		}), Page::publish));

		ContentNodeTestUtils.waitForNextSecond();

		page1 = Trx.execute(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page editableP = t.getObject(p, true);
			editableP.setFilename("page_a_old.html");
			editableP.save();

			return t.getObject(p);
		}, page1);

		page2 = Trx.execute(p -> update(p, upd -> {
			upd.setFilename("page_a.html");
			upd.publish();
		}), page2);

		String fileName1 = Trx.execute(Page::getFilename, page1);
		String fileName2 = Trx.execute(Page::getFilename, page2);

		assertThat(fileName1).as("Filename of Page A").isEqualTo("page_a_old.html");
		assertThat(fileName2).as("Filename of Page B").isEqualTo("page_a1.html");
	}

	/**
	 * Test setting page filename to a value, which is used by the formerly published, but not current version of another page, which has been taken offline.
	 * It should be possible to reuse a filename that the other page had in the published version, if the other page is offline
	 * @throws NodeException
	 */
	@Test
	public void testPageWithOldVersionOffline() throws NodeException {
		Page page1 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page A");
			p.setFilename("page_a.html");
		}), Page::publish));

		Page page2 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page B");
			p.setFilename("page_b.html");
		}), Page::publish));

		ContentNodeTestUtils.waitForNextSecond();

		page1 = Trx.execute(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page editableP = t.getObject(p, true);
			editableP.setFilename("page_a_old.html");
			editableP.save();
			editableP.takeOffline();

			return t.getObject(p);
		}, page1);

		page2 = Trx.execute(p -> update(p, upd -> {
			upd.setFilename("page_a.html");
			upd.publish();
		}), page2);

		String fileName1 = Trx.execute(Page::getFilename, page1);
		String fileName2 = Trx.execute(Page::getFilename, page2);

		assertThat(fileName1).as("Filename of Page A").isEqualTo("page_a_old.html");
		assertThat(fileName2).as("Filename of Page B").isEqualTo("page_a.html");
	}

	/**
	 * Test setting page filename to a value, which is used by the published, but not current version of another page via REST API
	 * @throws NodeException
	 */
	@Test
	public void testPageWithOldVersionViaRest() throws NodeException {
		Page page1 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page A");
			p.setFilename("page_a.html");
		}), Page::publish));

		Page page2 = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page B");
			p.setFilename("page_b.html");
		}), Page::publish));

		ContentNodeTestUtils.waitForNextSecond();

		page1 = Trx.execute(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page editableP = t.getObject(p, true);
			editableP.setFilename("page_a_old.html");
			editableP.save();

			return t.getObject(p);
		}, page1);

		final String message = Trx.execute(p -> {
			CNI18nString msg = new CNI18nString("a page with this filename already exists");
			msg.addParameter(I18NHelper.getPath(p));
			return msg.toString();
		}, page1);

		Trx.consume(p -> {
			PageResource pageResource = ContentNodeRESTUtils.getPageResource();
			com.gentics.contentnode.rest.model.Page pageUpdate = new com.gentics.contentnode.rest.model.Page();
			pageUpdate.setFileName("page_a.html");
			PageSaveRequest save = new PageSaveRequest(pageUpdate);
			save.setFailOnDuplicate(true);

			GenericResponse response = pageResource.save(p.getId().toString(), save);

			ContentNodeRESTUtils.assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving page " + p.getId() + ": " + message,
					new Message(Message.Type.CRITICAL, message));
		}, page2);
	}
}
