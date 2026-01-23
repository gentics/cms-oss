package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.init.SyncDefaultPackage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.QueryCountAsserter;

/**
 * Tests that count the number of queries necessary to manage pages with many (1000) tags.
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.WASTEBIN)
public class PageHandlingQueryCountTest {
	public final static String TEXT_CONSTRUCT_KEYWORD = "alohatext";

	public final static String LINK_CONSTRUCT_KEYWORD = "gtxalohapagelink";

	public final static String TEXT_TAG_NAME = "html";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(c -> {
		System.setProperty("com.gentics.portalnode.confpath", "src/test/resources/largecache");
	});

	private static Node node;

	private static Template template;

	private static Integer textConstructId;

	private static Integer linkConstructId;

	private static Integer folderId;

	private static Integer templateId;

	private static SystemUser systemUser;

	@Parameters(name = "{index}: tags {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int numTags : Arrays.asList(10, 100, 1000)) {
			data.add(new Object[] { numTags });
		}
		return data;
	}

	@BeforeClass
	public final static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		// delete some old data
		operate(() -> {
			DBUtils.update("DELETE FROM ds_obj_nodeversion WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM ds_obj WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM ds_nodeversion WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM ds WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM value_nodeversion WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM value WHERE contenttag_id != 0");
			DBUtils.update("DELETE FROM contenttag_nodeversion");
			DBUtils.update("DELETE FROM contenttag");
			DBUtils.update("DELETE FROM page_nodeversion");
			DBUtils.update("DELETE FROM page");
			DBUtils.update("DELETE FROM nodeversion");
		});

		operate(() -> new SyncDefaultPackage().execute());

		textConstructId = supply(() -> DBUtils.select("SELECT id FROM construct WHERE keyword = ?", stmt -> {
			stmt.setString(1, TEXT_CONSTRUCT_KEYWORD);
		}, DBUtils.firstInt("id")));

		linkConstructId = supply(() -> DBUtils.select("SELECT id FROM construct WHERE keyword = ?", stmt -> {
			stmt.setString(1, LINK_CONSTRUCT_KEYWORD);
		}, DBUtils.firstInt("id")));

		node = supply(() -> ContentNodeTestDataUtils.createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Test Template"));

		template = update(template, upd -> {
			TemplateTag tag = create(TemplateTag.class, c -> {
				c.setConstructId(textConstructId);
				c.setEnabled(true);
				c.setName(TEXT_TAG_NAME);
				c.setPublic(true);
			}).doNotSave().build();

			upd.getTemplateTags().put(TEXT_TAG_NAME, tag);
			upd.setSource("<node %s>".formatted(TEXT_TAG_NAME));
		}).build();

		folderId = supply(() -> node.getFolder().getId());
		templateId = supply(() -> template.getId());

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Parameter(0)
	public int numTags;

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			clear(node);
		});
	}

	@Test
	public void testCreate() throws NodeException {
		try (QueryCountAsserter asserter = QueryCountAsserter.allow(140); Trx trx = trxAt(1000)) {
			createPage();
		}
	}

	@Test
	public void testCreateTags() throws NodeException {
		// number of allowed statements for inserting depends on the number of tags
		// base is 170 statements
		// each tag is inserted into contenttag and contenttag_nodeversion (so number of tags times 2)
		// batched in groups of 100 inserts each
		// each tag has 8 values, inserted into value and value_nodeversion (so number of tags times 16)
		// batched in groups of 100 inserts each
		int allowed = 170 + numTags * 16 / 100 + numTags * 2 / 100;

		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(allowed); Trx trx = trxAt(2000)) {
			createTags(page);
		}
	}

	@Test
	public void testUpdateTags() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		// number of allowed statements for updating values depends on the number of tags (values)
		// base is 180 statements
		// each updated value requires a single update statement
		int allowed = 180 + numTags;

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(allowed); Trx trx = trxAt(3000)) {
			updateTags(page);
		}
	}

	@Test
	public void testPutIntoWastebin() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(100); Trx trx = trxAt(3000)) {
			putIntoWastebin(page);
		}
	}

	@Test
	public void testRemoveFromWastebin() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});
		operate(t -> {
			t.setTimestamp(3000 * 1000);
			putIntoWastebin(page);
		});

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(100); Trx trx = trxAt(4000)) {
			removeFromWastebin(page);
		}
	}

	@Test
	public void testRestoreFromWastebin() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});
		operate(t -> {
			t.setTimestamp(3000 * 1000);
			putIntoWastebin(page);
		});

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(150); Trx trx = trxAt(4000)) {
			restoreFromWastebin(page);
		}
	}

	@Test
	public void testLoadPreview() throws NodeException, PortalCacheException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(50); Trx trx = trxAt(3000)) {
			load(page, false);
		}
	}

	@Test
	public void testLoadEdit() throws NodeException, PortalCacheException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(150); Trx trx = trxAt(3000)) {
			load(page, true);
		}
	}

	@Test
	public void testCancelNoChanges() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		operate(() -> load(page, true));

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(120); Trx trx = trxAt(3000)) {
			GenericResponse response = getPageResource().cancel(page.getId(), null);
			assertResponseCodeOk(response);
		}
	}

	@Test
	public void testRenderPreview() throws NodeException, PortalCacheException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(50); Trx trx = trxAt(systemUser, 3000)) {
			render(page, false);
		}
	}

	@Test
	public void testRenderEdit() throws NodeException, PortalCacheException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(150); Trx trx = trxAt(systemUser, 3000)) {
			render(page, true);
		}
	}

	@Test
	public void testRenderVersion() throws NodeException, PortalCacheException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});
		operate(t -> {
			t.setTimestamp(3000 * 1000);
			updateTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(50); Trx trx = trxAt(systemUser, 4000)) {
			renderVersion(page, 2000);
		}
	}

	@Test
	public void testRestoreVersion() throws NodeException {
		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});
		operate(t -> {
			t.setTimestamp(3000 * 1000);
			updateTags(page);
		});

		int allowed = 200 + numTags * 8 / 100;

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(allowed); Trx trx = trxAt(4000)) {
			restoreVersion(page, 2000);
		}
	}

	@Test
	public void testCopy() throws NodeException, PortalCacheException {
		// number of allowed statements for inserting depends on the number of tags
		// base is 190 statements
		// each tag is inserted into contenttag and contenttag_nodeversion (so number of tags times 2)
		// batched in groups of 100 inserts each
		// each tag has 8 values, inserted into value and value_nodeversion (so number of tags times 16)
		// batched in groups of 100 inserts each
		int allowed = 190 + numTags * 16 / 100 + numTags * 2 / 100;

		Page page = supply(t -> {
			t.setTimestamp(1000 * 1000);
			return createPage();
		});
		operate(t -> {
			t.setTimestamp(2000 * 1000);
			createTags(page);
		});

		ContentNodeTestUtils.clearNodeObjectCache();

		try (QueryCountAsserter asserter = QueryCountAsserter.allow(allowed); Trx trx = trxAt(3000)) {
			copy(page);
		}
	}

	protected Trx trxAt(int timestamp) throws NodeException {
		return trxAt(null, timestamp);
	}

	protected Trx trxAt(SystemUser user, int timestamp) throws NodeException {
		Trx trx = user != null ? new Trx(user) : new Trx();
		trx.at(timestamp);
		return trx;
	}

	protected Page createPage() throws NodeException {
		PageCreateRequest request = new PageCreateRequest();
		request.setFolderId(String.valueOf(folderId));
		request.setTemplateId(templateId);

		PageLoadResponse response = getPageResource().create(request);
		assertResponseCodeOk(response);
		return response.getPage();
	}

	protected void createTags(Page page) throws NodeException {
		StringBuilder content = new StringBuilder();

		for (int i = 1; i <= numTags; i++) {
			String tagName = "tag_%d".formatted(i);
			Tag tag = new Tag();
			tag.setActive(true);
			tag.setConstructId(linkConstructId);
			tag.setName(tagName);
			tag.setType(Type.CONTENTTAG);

			Map<String, Property> properties = new HashMap<>();
			tag.setProperties(properties);

			Property text = new Property();
			text.setType(Property.Type.STRING);
			text.setStringValue("link");
			properties.put("text", text);

			Property url = new Property();
			url.setType(Property.Type.PAGE);
			url.setStringValue("https://gentics.com/");
			properties.put("url", url);

			page.getTags().put(tagName, tag);
			content.append("<node %s><br>".formatted(tagName));
		}
		page.getTags().get(TEXT_TAG_NAME).getProperties().get("text").setStringValue(content.toString());

		// update to create the tags
		PageSaveRequest updateRequest = new PageSaveRequest(page);
		updateRequest.setUnlock(true);
		GenericResponse response = getPageResource().save(String.valueOf(page.getId()), updateRequest);
		assertResponseCodeOk(response);
	}

	protected void updateTags(Page page) throws NodeException {
		// update to change the tags
		page.getTags().values().stream().filter(t -> !Strings.CI.equals(t.getName(), TEXT_TAG_NAME)).forEach(t -> {
			t.getProperties().get("text").setStringValue("newlink");
		});

		PageSaveRequest updateRequest = new PageSaveRequest(page);
		updateRequest.setUnlock(true);

		GenericResponse response = getPageResource().save(String.valueOf(page.getId()), updateRequest);
		assertResponseCodeOk(response);
	}

	protected void putIntoWastebin(Page page) throws NodeException {
		GenericResponse response = getPageResource().delete(String.valueOf(page.getId()), null, true);
		assertResponseCodeOk(response);
	}

	protected void removeFromWastebin(Page page) throws NodeException {
		GenericResponse response = getPageResource().deleteFromWastebin(String.valueOf(page.getId()), 0);
		assertResponseCodeOk(response);
	}

	protected void restoreFromWastebin(Page page) throws NodeException {
		GenericResponse response = getPageResource().restoreFromWastebin(String.valueOf(page.getId()), 0);
		assertResponseCodeOk(response);
	}

	protected void load(Page page, boolean editMode) throws NodeException {
		PageLoadResponse response = getPageResource().load(String.valueOf(page.getId()), editMode, editMode, editMode, editMode,
				editMode, editMode, editMode, editMode, editMode, editMode, null, null);
		assertResponseCodeOk(response);
	}

	protected void render(Page page, boolean editMode) throws NodeException {
		PageRenderResponse response = getPageResource().render(String.valueOf(page.getId()), null, null, editMode, null, LinksType.frontend, false,
				false, false, null);
		assertResponseCodeOk(response);
	}

	protected void renderVersion(Page page, int versionTimestamp) throws NodeException {
		PageRenderResponse response = getPageResource().render(String.valueOf(page.getId()), null, null, false, null, LinksType.frontend, false,
				false, false, versionTimestamp);
		assertResponseCodeOk(response);
	}

	protected void restoreVersion(Page page, int versionTimestamp) throws NodeException {
		PageLoadResponse response = getPageResource().restoreVersion(String.valueOf(page.getId()), versionTimestamp);
		assertResponseCodeOk(response);
	}

	protected void copy(Page page) throws NodeException {
		PageCopyRequest request = new PageCopyRequest();
		request.setCreateCopy(true);
		request.setSourcePageIds(Arrays.asList(page.getId()));
		request.setTargetFolders(Arrays.asList(new TargetFolder(folderId, 0)));
		PageCopyResponse response = getPageResource().copy(request, 0);
		assertResponseCodeOk(response);
	}
}
