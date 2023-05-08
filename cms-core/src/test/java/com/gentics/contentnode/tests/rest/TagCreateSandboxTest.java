/**
 * 
 */
package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.MultiTagCreateRequest;
import com.gentics.contentnode.rest.model.request.TagCreateRequest;
import com.gentics.contentnode.rest.model.request.page.CreatedTag;
import com.gentics.contentnode.rest.model.response.MultiTagCreateResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.etc.StringUtils;

/**
 * Test creating tags
 * 
 * @author norbert
 */
public class TagCreateSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * The valid page id to be saved
	 */
	private static final String PAGE_ID = "1";

	/**
	 * The construct id of the created tag
	 */
	private static final int CONSTRUCT_ID = 4;

	/**
	 * The name of the construct
	 */
	private static final String CONSTRUCT_NAME = "magictest";

	/**
	 * Magic value
	 */
	private static final String MAGIC_VALUE = "This is the magic value of the new tag";

	@Before 
	public void setup() throws NodeException {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("magic_part_value", true);
	}

	/**
	 * Test creating a new tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTagCreation() throws Exception {
		TagCreateResponse response = null;
		ContentTagCreateRequest request = new ContentTagCreateRequest();

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		response = pageResource.createTag(PAGE_ID, CONSTRUCT_ID, null, request);

		assertEquals("Check for the correct response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

		assertEquals("Check whether the tag is enabled", Boolean.TRUE, response.getTag().getActive());

		assertEquals("Check the construct id", ObjectTransformer.getInteger(CONSTRUCT_ID, null), response.getTag().getConstructId());
	}

	/**
	 * Test creating a new tag with a magic value
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMagicValue() throws Exception {
		TagCreateResponse response = null;
		ContentTagCreateRequest request = new ContentTagCreateRequest();

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		request.setMagicValue(MAGIC_VALUE);
		response = pageResource.createTag(PAGE_ID, CONSTRUCT_ID, null, request);

		assertEquals("Check for the correct response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

		// check whether the magic value was stored
		Property textProperty = response.getTag().getProperties().get("text");

		assertEquals("Check the type of the magic value", Type.STRING, textProperty.getType());
		assertEquals("Check the magic value", MAGIC_VALUE, textProperty.getStringValue());

		// check whether the unmagic value is empty
		Property unmagicProperty = response.getTag().getProperties().get("unmagic");

		assertEquals("Check the type of the unmagic value", Type.STRING, unmagicProperty.getType());
		assertTrue("Check whether the unmagic value is empty", StringUtils.isEmpty(unmagicProperty.getStringValue()));
	}

	/**
	 * Check whether new tags are created with the expected names
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNewTagName() throws Exception {
		TagCreateResponse response = null;

		for (int i = 1; i <= 12; i++) {
			ContentTagCreateRequest request = new ContentTagCreateRequest();

			PageResourceImpl pageResource = new PageResourceImpl();
			pageResource.setTransaction(TransactionManager.getCurrentTransaction());
			response = pageResource.createTag(PAGE_ID, CONSTRUCT_ID, null, request);

			assertEquals("Check for the correct response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

			// now check whether the name of the created tag is like expected
			assertEquals("Check tag name", CONSTRUCT_NAME + i, response.getTag().getName());
		}
	}

	/**
	 * Check if copying a contenttag via REST-API works if the order of part creation is different
	 * from the order of their values and their PartType is different (SUP-370)
	 * @throws NodeException 
	 */
	@Test
	public void testTagCopy() throws NodeException {
		Transaction t = testContext.startSystemUserTransaction();

		Node node = Creator.createNode("blargh", "blargh", "/", "/", Collections.<ContentLanguage> emptyList());
		Template tpl = Creator.createTemplate("tpl1", "hallo", node.getFolder());

		Part part1 = Creator.createTextPartUnsaved("part1", 1, 1, "1");
		part1.setPartOrder(2);
		com.gentics.contentnode.object.Construct c = Creator.createConstruct("testcopytag", "karl", "testcopytag", Arrays.asList(part1));

		com.gentics.contentnode.object.Page p = Creator.createPage("testpage", node.getFolder(), tpl, null);
		PageResourceImpl pri = new PageResourceImpl();
		pri.setTransaction(t);
		TagCreateResponse response = pri.createTag(String.valueOf(p.getId()), (Integer) c.getId(), null, new ContentTagCreateRequest());
		String tagname = response.getTag().getName();
		ContentNodeRESTUtils.assertResponseOK(response);
		t.commit(false);

		c = t.getObject(com.gentics.contentnode.object.Construct.class, c.getId(), true);
		Part part2 = Creator.createTextPartUnsaved("part2", 31, 1, "1");
		part2.setPartTypeId(31);
		part2.setPartOrder(1);
		c.getParts().add(part2);
		c.save();
		t.commit(false);

		ContentTagCreateRequest request = new ContentTagCreateRequest();
		request.setCopyPageId(String.valueOf(p.getId()));
		request.setCopyTagname(tagname);
		response = pri.createTag(String.valueOf(p.getId()), null, null, request);
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	/**
	 * Check creating multiple tags in a single request
	 * @throws NodeException
	 */
	@Test
	public void createMultipleTags() throws Exception {
		testContext.startSystemUserTransaction();

		Node node = ContentNodeTestDataUtils.createNode();
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template");

		com.gentics.contentnode.object.Page page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page");
		String pageId = ObjectTransformer.getString(page.getId(), null);

		int constructId = ContentNodeTestDataUtils.createConstruct(node, HTMLTextPartType.class, "test", "text");

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		MultiTagCreateRequest request = new MultiTagCreateRequest();
		Map<String, TagCreateRequest> create = new HashMap<String, TagCreateRequest>();
		addTagCreateRequest(create, "eins", constructId, null, "one");
		addTagCreateRequest(create, "zwei", null, "test", "two");
		addTagCreateRequest(create, "drei", constructId, null, "three");
		request.setCreate(create);
		MultiTagCreateResponse response = pageResource.createTags(pageId, request);
		ContentNodeRESTUtils.assertResponseOK(response);

		Map<String, CreatedTag> created = response.getCreated();
		assertCreated(pageId, created, "eins", constructId, "text", "one");
		assertCreated(pageId, created, "zwei", constructId, "text", "two");
		assertCreated(pageId, created, "drei", constructId, "text", "three");
	}

	/**
	 * Create multiple tags with special characters in the magic value
	 * @throws Exception
	 */
	@Test
	public void createMultipleTagsWithSpecialContent() throws Exception {
		Map<String, String> tested = new HashMap<String, String>();
		tested.put("comma", "a,b");
		tested.put("semicolon", "a;b");
		tested.put("colon", "a:b");
		tested.put("smaller", "a<b");
		tested.put("greater", "a>b");
		tested.put("mixed", "a<,;:>b");
		tested.put("newline", "a\nb");
		testContext.startSystemUserTransaction();

		Node node = ContentNodeTestDataUtils.createNode();
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template");

		com.gentics.contentnode.object.Page page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page");
		String pageId = ObjectTransformer.getString(page.getId(), null);

		int constructId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "test", "text");

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		MultiTagCreateRequest request = new MultiTagCreateRequest();
		Map<String, TagCreateRequest> create = new HashMap<String, TagCreateRequest>();
		for (Map.Entry<String, String> testedEntry : tested.entrySet()) {
			addTagCreateRequest(create, testedEntry.getKey(), constructId, null, testedEntry.getValue());
		}
		request.setCreate(create);
		MultiTagCreateResponse response = pageResource.createTags(pageId, request);
		ContentNodeRESTUtils.assertResponseOK(response);

		Map<String, CreatedTag> created = response.getCreated();
		for (Map.Entry<String, String> testedEntry : tested.entrySet()) {
			assertCreated(pageId, created, testedEntry.getKey(), constructId, "text", testedEntry.getValue());
		}
	}

	/**
	 * Add a {@link TagCreateRequest} instance to the given map
	 * @param map map where to add the instance
	 * @param id key of the instance
	 * @param constructId construct ID (may be null)
	 * @param keyword construct keyword (may be null)
	 * @param text magic value (may be null)
	 */
	protected void addTagCreateRequest(Map<String, TagCreateRequest> map, String id, Integer constructId, String keyword, String text) {
		TagCreateRequest request = new TagCreateRequest();
		if (constructId != null) {
			request.setConstructId(constructId);
		}
		if (keyword != null) {
			request.setKeyword(keyword);
		}
		if (text != null) {
			request.setMagicValue(text);
		}
		map.put(id, request);
	}

	/**
	 * Assert that the tag was created correctly
	 * @param pageId page ID
	 * @param created map containing the created tags
	 * @param id ID of the tag (from the request)
	 * @param constructId expected construct ID
	 * @param partKeyword keyword of the part to check
	 * @param text expected value of the part
	 */
	protected void assertCreated(String pageId, Map<String, CreatedTag> created, String id, Integer constructId, String partKeyword, String text) {
		assertTrue("Tag " + id + " should have been created", created.containsKey(id));
		CreatedTag createdTag = created.get(id);
		assertEquals("Check construct ID", constructId, createdTag.getTag().getConstructId());
		assertNotNull("Part " + partKeyword + " should exist", createdTag.getTag().getProperties().get(partKeyword));
		assertEquals("Check value of part " + partKeyword, text, createdTag.getTag().getProperties().get(partKeyword).getStringValue());
		StringBuilder expectedHtml = new StringBuilder();
		expectedHtml.append("<div data-gcn-pageid=\"").append(pageId).append("\" data-gcn-tagid=\"").append(createdTag.getTag().getId())
				.append("\" data-gcn-tagname=\"").append(createdTag.getTag().getName())
				.append("\" data-gcn-i18n-constructname=\"test\" class=\"aloha-block\" id=\"GENTICS_BLOCK_").append(createdTag.getTag().getId()).append("\">")
				.append(text).append("</div>");
		assertEquals("Check rendered tag", expectedHtml.toString(), createdTag.getHtml());
	}
}
