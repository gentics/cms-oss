package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.Builder.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import java.util.Iterator;
import java.util.List;


import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;


import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Sandbox Tests that edit constructs
 */
public class ConstructEditSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * The valid page id to be saved
	 */
	private static final int PAGE_ID = 1;

	/**
	 * The id of the node, to which the construct is assigned
	 */
	private static final int NODE_ID = 1;

	/**
	 * ID of the text parttype
	 */
	private static final int TEXT_PARTTYPE_ID = 1;

	/**
	 * Name of the unmagic part
	 */
	private static String UNMAGIC_PART_NAME = "unmagic";

	/**
	 * Name of the text part
	 */
	private static String TEXT_PART_NAME = "text";

	/**
	 * Name of the new part
	 */
	private static String NEW_PART_NAME = "new";

	/**
	 * ID of the tag
	 */
	private Integer tagId;

	/**
	 * Name of the tag
	 */
	private String tagName;

	/**
	 * The construct id of the created tag (construct is created for the test)
	 */
	private Integer editedConstructId;

	@Before
	public void setUp() throws Exception {

		// create a construct with two editable parts (like the construct with id 4 in the test data)
		editedConstructId = create(Construct.class, c -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			c.setAutoEnable(true);
			c.setKeyword("magictest");
			c.setName("magictest", 1);
			c.getNodes().add(t.getObject(Node.class, NODE_ID));
			c.getParts().add(createPart(t, UNMAGIC_PART_NAME, 1));
			c.getParts().add(createPart(t, TEXT_PART_NAME, 2));
		}).build().getId();

		// set the rendertype to "preview"
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW)) {
			Transaction t = trx.getTransaction();

			// create a tag in the page
			Page page = t.getObject(Page.class, PAGE_ID, true);
			ContentTag newTag = page.getContent().addContentTag(editedConstructId);

			newTag.getValues().getByKeyname(UNMAGIC_PART_NAME).setValueText("unmagic");
			newTag.getValues().getByKeyname(TEXT_PART_NAME).setValueText("text");
			page.save();
			t.commit(false);

			tagId = newTag.getId();
			tagName = newTag.getName();

			// load the page and its content (make sure all is in the cache)
			t.getObject(Page.class, PAGE_ID).getContentTags();

			// load the tag and its values (make sure all is in the cache)
			t.getObject(ContentTag.class, tagId).getValues();
			trx.success();
		}
	}

	/**
	 * Remove the tag from the page and delete the construct created for the test
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		if (tagName != null) {
			try (Trx trx = new Trx()) {
				Page page = trx.getTransaction().getObject(Page.class, PAGE_ID, true);
				page.getContentTags().remove(tagName);
				page.save();
				page.unlock();
				trx.success();
			}
		}
		if (editedConstructId != null) {
			try (Trx trx = new Trx()) {
				Construct construct = trx.getTransaction().getObject(Construct.class, editedConstructId);
				if (construct != null) {
					construct.delete(true);
				}
				trx.success();
			}
		}
	}

	/**
	 * Create an editable short text part
	 * @param t transaction
	 * @param keyname part keyname
	 * @param partOrder part order
	 * @return part
	 * @throws NodeException
	 */
	private Part createPart(Transaction t, String keyname, int partOrder) throws NodeException {
		Part part = t.createObject(Part.class);
		part.setEditable(1);
		part.setHidden(false);
		part.setKeyname(keyname);
		part.setName(keyname, 1);
		part.setPartOrder(partOrder);
		part.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(ShortTextPartType.class));
		part.setDefaultValue(t.createObject(Value.class));
		return part;
	}

	/**
	 * Test adding a new part to a construct
	 * @throws Exception
	 */
	@Test
	public void addPartTest() throws Exception {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW)) {
			Transaction t = trx.getTransaction();

			// add a new part
			Construct construct = t.getObject(Construct.class, editedConstructId, true);
			List<Part> parts = construct.getParts();
			Part newPart = t.createObject(Part.class);

			newPart.setEditable(1);
			newPart.setHidden(false);
			newPart.setPartTypeId(TEXT_PARTTYPE_ID);
			newPart.setKeyname(NEW_PART_NAME);
			parts.add(newPart);
			construct.save();
			t.commit(false);

			ContentTag contentTag = t.getObject(ContentTag.class, tagId);
			ValueList values = contentTag.getValues();

			assertEquals("Check # of tag values", 2, values.size());

			contentTag = t.getObject(ContentTag.class, tagId, true);
			values = contentTag.getValues();
			assertEquals("Check # of editable tag values", 3, values.size());
			trx.success();
		}
	}

	/**
	 * Test removing a part from a construct. This test will especially test,
	 * whether removing the part has any negative impact on tags, that are
	 * cached, and are not dirted in the cache. The implementation of this was
	 * changed to no longer dirt the caches of tags when a construct changes
	 * (because of possible performance impacts), but to ignore values that
	 * reference parts that no longer exist.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deletePartTest() throws Exception {
		// check values before
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentTag contentTag = t.getObject(ContentTag.class, tagId);
			ValueList values = contentTag.getValues();

			assertEquals("Check # of tag values before part is removed", 2, values.size());
		});

		// delete a part
		Integer partNameId = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			Construct construct   = t.getObject(Construct.class, editedConstructId, true);
			List<Part> parts      = construct.getParts();
			Integer partNameIdInt = null;

			for (Iterator<Part> iterator = parts.iterator(); iterator.hasNext();) {
				Part part = iterator.next();

				if (UNMAGIC_PART_NAME.equals(part.getKeyname())) {
					partNameIdInt = part.getNameId();
					iterator.remove();
				}
			}
			construct.save();

			return partNameIdInt;
		});

		// check tag values
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			ContentTag contentTag = t.getObject(ContentTag.class, tagId);
			ValueList values = contentTag.getValues();

			assertEquals("Check # of tag values after part is removed", 1, values.size());
		});

		// Check dicuser entries
		Trx.operate(() -> {
			assertNotNull("Check if partNameId is not null", partNameId);
			assertEquals("Check # of dicuser entries for the deleted part", 0, CNDictionary.getDicuserEntries(partNameId).size());
		});
	}

	/**
	 * Test deleting a part with publish cache enabled
	 * @throws Exception
	 */
	@Test
	public void testDeletePartPublishCache() throws Exception {
		try (FeatureClosure publishCache = new FeatureClosure(Feature.PUBLISH_CACHE, true)) {
			// publish page
			Trx.operate(() -> {
				TransactionManager.getCurrentTransaction().getObject(Page.class, PAGE_ID, true).publish();
			});

			// check publish cache of page
			Trx.operate(() -> {
				try (PublishCacheTrx pTrx = new PublishCacheTrx(true); RenderTypeTrx rtTrx = RenderTypeTrx.publish()) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Page page = t.getObject(Page.class, PAGE_ID, -1, false);
					assertNotNull("Check page", page);
					ContentTag tag = page.getContentTag(tagName);
					assertNotNull("Check tag", tag);
					assertEquals("Check # of tag values from publish cache", 2, tag.getValues().size());
					for (Value value : tag.getValues()) {
						value.getPart();
					}
				}
			});

			// delete a part
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();

				Construct construct   = t.getObject(Construct.class, editedConstructId, true);
				List<Part> parts      = construct.getParts();

				parts.removeIf(p -> UNMAGIC_PART_NAME.equals(p.getKeyname()));
				construct.save();
			});

			// check publish cache of page
			Trx.operate(() -> {
				try (PublishCacheTrx pTrx = new PublishCacheTrx(true); RenderTypeTrx rtTrx = RenderTypeTrx.publish()) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Page page = t.getObject(Page.class, PAGE_ID, -1, false);
					assertNotNull("Check page", page);
					ContentTag tag = page.getContentTag(tagName);
					assertNotNull("Check tag", tag);
					assertEquals("Check # of tag values from publish cache", 1, tag.getValues().size());
					for (Value value : tag.getValues()) {
						value.getPart();
					}
				}
			});
		}
	}

	/**
	 * Test for replacing the default value of a part and changing the value in the same transaction.
	 * This kind of modification may be done in an import, if postponed references need to be set in a second step
	 * @throws Exception
	 */
	@Test
	public void testReplaceDefaultValue() throws Exception {
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Node node = ContentNodeTestDataUtils.createNode("test", "Test", PublishTarget.NONE);
			int constructId = ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "testconstruct", "part");

			// start to edit the construct
			Construct construct = t.getObject(Construct.class, constructId, true);
			// first replace the default value
			construct.getParts().get(0).setDefaultValue(t.createObject(Value.class));
			construct.save();

			// dirt the cache and continue editing the construct
			t.dirtObjectCache(Construct.class, constructId);
			construct = t.getObject(Construct.class, constructId, true);
			construct.getParts().get(0).getDefaultValue().setValueText("Test text");
			construct.save();
			t.commit(false);

			construct = t.getObject(Construct.class, constructId);
			assertEquals("Check value text", "Test text", construct.getParts().get(0).getDefaultValue().getValueText());
			trx.success();
		}
	}

	/**
	 * Test replacing a part (with editing it again in the same transaction)
	 * @throws Exception
	 */
	@Test
	public void testReplacePart() throws Exception {
		int constructId = 0;
		try (Trx trx = new Trx()) {
			Node node = ContentNodeTestDataUtils.createNode("test", "Test", PublishTarget.NONE);
			constructId = ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "testconstruct", "part");
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			// replace the part with a new one (which should change the global ID)
			Construct construct = t.getObject(Construct.class, constructId, true);
			construct.getParts().clear();

			Part part = t.createObject(Part.class);
			part.setEditable(1);
			part.setHidden(false);
			part.setKeyname("part");
			part.setName("part", 1);
			part.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(LongHTMLPartType.class));
			part.setDefaultValue(t.createObject(Value.class));
			construct.getParts().add(part);
			construct.save();

			// edit the part again
			construct = t.getObject(Construct.class, constructId, true);
			construct.getParts().get(0).getDefaultValue().setValueText("Modified Default Text");
			construct.save();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Construct construct = t.getObject(Construct.class, constructId);
			assertEquals("Check default value", "Modified Default Text", construct.getParts().get(0).getDefaultValue().getValueText());
		}
	}
}
