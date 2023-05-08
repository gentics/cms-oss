package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.setRenderType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.scheduler.PurgeVersionsJob;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.log.NodeLogger;

public class PurgeVersionsSandboxTest extends AbstractPageVersioningTest {

	/**
	 * Logger
	 */
	static NodeLogger logger = NodeLogger.getNodeLogger(PurgeVersionsSandboxTest.class);

	@After
	public void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.TAG_IMAGE_RESIZER.toString().toLowerCase(), false);

		Transaction currentTransaction = TransactionManager.getCurrentTransactionOrNull();
		if (currentTransaction != null) {
			currentTransaction.commit();
		}
	}

	/**
	 * Test that the purge versions job does not purge the last published version (and younger versions)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePageVersion() throws Exception {
		String name = "Create Page Version";
		String filename = "create_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// 1. Create a page and check whether it has a page version with number 0.1 (timestamp 1)
		Page page = createPage(1000, VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Integer pageId = page.getId();
		logger.debug("Created page with id {" + pageId + "}");

		// 2. Modify and save the page (timestamp 2)
		page = modifyContentAndSavePage(page, 2000);
		verifyNodeVersions(page, "0.2, 0.1");

		// 3. Modify and save the page (timestamp 3)
		page = modifyContentAndSavePage(page, 3000);
		verifyNodeVersions(page, "0.3, 0.2, 0.1");

		// 4. Publish the page (timestamp 4)
		page = Trx.execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4000);
			return update(p, upd -> upd.publish());
		}, page);
		verifyNodeVersions(page, "1.0, 0.3, 0.2, 0.1");

		// 5. Modify and save the page (timestamp 5)
		page = modifyContentAndSavePage(page, 5000);
		verifyNodeVersions(page, "1.1, 1.0, 0.3, 0.2, 0.1");

		// 6. Execute the purgeversions job
		Trx.operate(() -> {
			startPurgeVersionsJob(1000);
		});

		// 7. Modify the page and save it (timestamp 6)
		page = modifyContentAndSavePage(page, 6000);
		verifyNodeVersions(page, "1.2, 1.1, 1.0");
	}

	/**
	 * Test that rendering the published version of a page still works after the purge versions job
	 * @throws Exception
	 */
	@Test
	public void testPurgeVersionConsistency() throws Exception {
		String name = "Create Page Version 2";
		String filename = "create_page_version_2.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// 1. Create a page and check whether it has a page version with number 0.1 (timestamp 1)
		Page page = createPage(1000, VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Integer pageId = page.getId();
		logger.debug("Created page with id {" + pageId + "}");

		// 2. Modify and save the page (timestamp 2)
		page = modifyContentAndSavePage(page, 2000);
		verifyNodeVersions(page, "0.2, 0.1");

		// 3. Modify and save the page (timestamp 3)
		page = modifyContentAndSavePage(page, 3000);
		verifyNodeVersions(page, "0.3, 0.2, 0.1");

		// 4. Publish the page (timestamp 4)
		page = Trx.execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4000);
			return update(p, upd -> upd.publish());
		}, page);
		verifyNodeVersions(page, "1.0, 0.3, 0.2, 0.1");

		// 6. render a live preview of the page and check whether it renders the modified content
		Trx.consume(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(5000);
			try {
				setRenderType(RenderType.EM_PREVIEW);
			} catch (Exception e) {
				throw new NodeException(e);
			}
			assertNotNull("Check whether the created page was found", p);
			assertTrue("Check whether the current version of the page was fetched now", p.getObjectInfo().getVersionTimestamp() < 0);
			RenderResult renderResult = new RenderResult();
			String livePreview = p.render(renderResult);

			assertEquals("Check preview content of the modified page", "Modified Content: Modified Content: Name: Modified Modified " + name + ", Filename: " + filename + ", Version: 1.0",
					livePreview);
		}, page);

		// 7. Execute the purgeversions job
		startPurgeVersionsJob(-1);

		// 8. render the page for publishing and check whether it is the unmodified version of the page
		page = Trx.execute(p -> p.getPublishedObject(), page);
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		Trx.consume(p -> {
			RenderResult renderResult = new RenderResult();
			try {
				setRenderType(RenderType.EM_PREVIEW);
			} catch (Exception e) {
				throw new NodeException(e);
			}
			String publishedPage = p.render(renderResult);

			assertEquals("Check published content of the modified page", "Modified Content: Modified Content: Name: Modified Modified " + name + ", Filename: " + filename + ", Version: 1.0",
					publishedPage);
		}, page);
	}

	@Test
	@GCNFeature(set = { Feature.WASTEBIN })
	public void testPurgeWastebin() throws NodeException {
		String name = "Create Page Version";
		String filename = "create_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// 1. Create a page and check whether it has a page version with number 0.1 (timestamp 1)
		Page page = createPage(1000, VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Node node = Trx.execute(p -> p.getOwningNode(), page);
		Integer pageId = page.getId();
		logger.debug("Created page with id {" + pageId + "}");

		// 2. Modify and save the page (timestamp 2)
		page = modifyContentAndSavePage(page, 2000);
		verifyNodeVersions(page, "0.2, 0.1");

		// 3. Modify and save the page (timestamp 3)
		page = modifyContentAndSavePage(page, 3000);
		verifyNodeVersions(page, "0.3, 0.2, 0.1");

		// 4. Publish the page (timestamp 4)
		page = Trx.execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4000);
			return update(p, upd -> upd.publish());
		}, page);
		verifyNodeVersions(page, "1.0, 0.3, 0.2, 0.1");

		// 5. Modify and save the page (timestamp 5)
		page = modifyContentAndSavePage(page, 5000);
		verifyNodeVersions(page, "1.1, 1.0, 0.3, 0.2, 0.1");

		// 6. delete the page (put it into wastebin)
		deletePage(page);

		// 7. Execute the purgeversions job
		Trx.operate(() -> {
			startPurgeVersionsJob(1000);
		});

		try (Trx trx = new Trx(); WastebinFilter filter = WastebinFilter.get(true, node)) {
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		verifyNodeVersions(page, "1.1, 1.0");
	}

	/**
	 * Test purging versions, when the page has a planned version
	 * @throws NodeException
	 */
	@Test
	public void testPlannedVersion() throws NodeException {
		Template template = supply(t -> t.getObject(Template.class, VERSIONS_TEMPLATE_ID));
		Folder folder = supply(t -> t.getObject(Folder.class, VERSIONS_FOLDER_ID));

		int createTime = 1;
		int firstUpdateTime = 2;
		int publishTime = 3;
		int publishAtTime = 10;
		int secondUpdateTime = 4;
		int thirdUpdateTime = 5;
		int purgeTime = 6;

		Page page = null;
		try (Trx trx = new Trx()) {
			trx.at(createTime);
			page = ContentNodeTestDataUtils.createPage(folder, template, "Planned page");
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(firstUpdateTime);
			page = update(page, upd -> upd.setName("First modification"));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(publishTime);
			page = update(page, upd -> upd.publish(publishAtTime, null));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(secondUpdateTime);
			page = update(page, upd -> upd.setName("Second modification"));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			trx.at(thirdUpdateTime);
			page = update(page, upd -> upd.setName("Third modification"));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			assertThat(page).as("Tested page").hasVersions(
					new NodeObjectVersion().setNumber("1.2").setCurrent(true).setDate(thirdUpdateTime),
					new NodeObjectVersion().setNumber("1.1").setDate(secondUpdateTime),
					new NodeObjectVersion().setNumber("1.0").setDate(publishTime),
					new NodeObjectVersion().setNumber("0.2").setDate(firstUpdateTime),
					new NodeObjectVersion().setNumber("0.1").setDate(createTime));
			trx.success();
		}

		operate(() -> {
			startPurgeVersionsJob(purgeTime);
		});

		page = execute(Page::reload, page);

		try (Trx trx = new Trx()) {
			assertThat(page).as("Tested page").hasVersions(
					new NodeObjectVersion().setNumber("1.2").setCurrent(true).setDate(thirdUpdateTime),
					new NodeObjectVersion().setNumber("1.1").setDate(secondUpdateTime),
					new NodeObjectVersion().setNumber("1.0").setDate(publishTime));
			trx.success();
		}
	}

	/**
	 * Verifies that the page has exactly the versions that were listed within given the expected versions array
	 * 
	 * @param page
	 * @param expectedVersions
	 * @throws NodeException
	 */
	public void verifyNodeVersions(Page page, String expectedVersions) throws NodeException {
		String pageVersionsString = new String();
		NodeObjectVersion[] pageVersions = Trx.supply(() -> page.getVersions());

		for (NodeObjectVersion version : pageVersions) {
			pageVersionsString += version.getNumber();
			pageVersionsString += ", ";
		}

		// Remove last two character
		pageVersionsString = pageVersionsString.substring(0, pageVersionsString.length() - 2);
		assertEquals("The page {" + page.getId() + "} versions i got from the page did not match the expected versions.", expectedVersions, pageVersionsString);
	}

	/**
	 * Modify content and name of the page
	 * @param page page
	 * @param timestamp timestamp of the transaction
	 * @return modified page
	 * @throws NodeException
	 */
	public Page modifyContentAndSavePage(Page page, long timestamp) throws NodeException {
		return Trx.execute(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(timestamp);
			p = t.getObject(p, true);
			p.setName("Modified " + p.getName());
			Value value = (Value) p.getContentTag(CONTENTTAG_NAME).getValues().get(PART_NAME);
			value.setValueText("Modified Content: " + value.getValueText());
			p.save();

			return t.getObject(p);
		}, page);
	}

	/**
	 * Create new page
	 * @param timestamp timestamp
	 * @param templateId template ID
	 * @param folderId folder ID
	 * @param name name
	 * @param filename filename
	 * @param content content
	 * @return page
	 * @throws NodeException
	 */
	public Page createPage(long timestamp, int templateId, int folderId, String name, String filename, String content) throws NodeException {
		return Trx.supply(t -> {
			t.setTimestamp(timestamp);
			return create(Page.class, p -> {
				p.setTemplateId(templateId);
				p.setFolderId(folderId);
				p.setName(name);
				p.setFilename(filename);

				// set the content
				ContentTag contentTag = p.getContentTag(CONTENTTAG_NAME);

				assertNotNull("Check whether the contenttag exists", contentTag);
				Value value = (Value) contentTag.getValues().get(PART_NAME);

				assertNotNull("Check whether the part exists", value);
				value.setValueText(content);
				contentTag.setEnabled(true);
			});
		});
	}

	/**
	 * Delete the page
	 * @param page page to delete
	 * @throws NodeException
	 */
	public void deletePage(Page page) throws NodeException {
		try (Trx trx = new Trx()) {
			update(page, Page::delete);
			trx.success();
		}
	}

	/**
	 * Starts the purge versions job that will remove all older versions.
	 * 
	 * @throws NodeException
	 */
	public void startPurgeVersionsJob(int purgeTimestamp) throws NodeException {
		int newPurgeTimestamp = purgeTimestamp;
		if (newPurgeTimestamp == -1) {
			newPurgeTimestamp = (int) (System.currentTimeMillis() / 1000L);
		}

		PurgeVersionsJob job = new PurgeVersionsJob();
		job.purgePages(newPurgeTimestamp);
	}
}
