package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for page objecttags, which are synchronized
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
@RunWith(value = Parameterized.class)
public class ObjectTagSyncTest {
	public final static String PART_NAME = "part";

	public final static String OBJTAG_NAME = "sync";

	public final static String ORIGINAL_TAG_VALUE = "original";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static ContentLanguage german;
	private static ContentLanguage english;
	private static Node node;
	private static Node channel;
	private static Template template;
	private static Integer constructId;
	private static ObjectTagDefinition objProp;

	@Parameters(name = "{index}: feature {0}, syncContentset {1}, syncVariants {2}, syncChannelset {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(true, false)) {
			for (boolean syncContentset : Arrays.asList(true, false)) {
				for (boolean syncVariants : Arrays.asList(true, false)) {
					for (boolean syncChannelset : Arrays.asList(true, false)) {
						data.add(new Object[] { feature, syncContentset, syncVariants, syncChannelset });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		german = supply(() -> getLanguage("de"));
		english = supply(() -> getLanguage("en"));

		node = supply(() -> createNode("node", "Node", PublishTarget.NONE, german, english));
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "construct", PART_NAME));
		objProp = supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Synchronized", OBJTAG_NAME));
	}

	/**
	 * Translate the source page into the given language
	 * @param source source page
	 * @param language translation language
	 * @return translation
	 * @throws NodeException
	 */
	protected static Page translate(Page source, ContentLanguage language) throws NodeException {
		PageLoadResponse response = getPageResource().translate(source.getId(), language.getCode(), false, null);
		assertResponseOK(response);

		return supply(t -> t.getObject(Page.class, response.getPage().getId()));
	}

	/**
	 * Create a pagevariant
	 * @param source source page
	 * @return variant
	 * @throws NodeException
	 */
	protected static Page variant(Page source) throws NodeException {
		PageCreateRequest request = new PageCreateRequest();
		request.setFolderId(Integer.toString(supply(() -> node.getFolder().getId())));
		request.setTemplateId(template.getId());
		request.setVariantId(source.getId());
		PageLoadResponse response = getPageResource().create(request);
		assertResponseOK(response);

		return supply(t -> t.getObject(Page.class, response.getPage().getId()));
	}

	/**
	 * Update the object tag in the given page
	 * @param page page
	 * @param newContent new content of the object tag
	 * @return updated page
	 * @throws NodeException
	 */
	protected static Page updateObjectTag(Page page, String newContent) throws NodeException {
		return update(page, upd -> {
			getPartType(ShortTextPartType.class, upd.getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject().setValueText(newContent);
		});
	}

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public boolean syncContentset;

	@Parameter(2)
	public boolean syncVariants;

	@Parameter(3)
	public boolean syncChannelset;

	private Pair<String, Page> germanPage;

	private Pair<String, Page> localizedGermanPage;

	private Pair<String, Page> englishPage;

	private Pair<String, Page> localizedEnglishPage;

	private Pair<String, Page> variant;

	/**
	 * Prepare test setup and data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences()
				.setFeature(Feature.OBJTAG_SYNC, feature));
		objProp = supply(() -> update(objProp, upd -> {
			upd.setSyncContentset(syncContentset);
			upd.setSyncVariants(syncVariants);
			upd.setSyncChannelset(syncChannelset);
		}));

		assertThat(NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)).as("Feature activated")
				.isEqualTo(feature);
		assertThat(objProp).hasFieldOrPropertyWithValue("syncContentset", syncContentset)
				.hasFieldOrPropertyWithValue("syncVariants", syncVariants)
				.hasFieldOrPropertyWithValue("syncChannelset", syncChannelset);

		// create the source page
		germanPage = MutablePair.of("Source page", supply(() -> update(createPage(node.getFolder(), template, "Page", null, german), upd -> {
			ObjectTag objectTag = upd.getObjectTag(OBJTAG_NAME);
			objectTag.setEnabled(true);
			getPartType(ShortTextPartType.class, objectTag, PART_NAME).getValueObject().setValueText(ORIGINAL_TAG_VALUE);
		})));

		// localize source page
		localizedGermanPage = MutablePair.of("Localized page", execute(p -> localize(p, channel), germanPage.getRight()));

		// create translation
		englishPage = MutablePair.of("Translated page", translate(germanPage.getRight(), english));

		// localize translation
		localizedEnglishPage = MutablePair.of("Localized translated page", execute(p -> localize(p, channel), englishPage.getRight()));

		// create page variant
		variant = MutablePair.of("Variant", variant(germanPage.getRight()));
	}

	@Test
	public void testCreateVariant() throws NodeException {
		Map<Pair<String, Page>, Boolean> expectedTagSynchronized = new HashMap<>();
		expectedTagSynchronized.put(localizedGermanPage, true); // localized copies always get the objecttags copied
		expectedTagSynchronized.put(englishPage, true); // translations always get the objecttags copied
		expectedTagSynchronized.put(localizedEnglishPage, true);
		expectedTagSynchronized.put(variant, feature && syncVariants);

		for (Map.Entry<Pair<String, Page>, Boolean> entry : expectedTagSynchronized.entrySet()) {
			if (entry.getValue()) {
				consume(p -> {
					assertThat(p.getRight().getObjectTag(OBJTAG_NAME)).as("Object tag of " + p.getLeft()).isNotNull().isEnabled(true);
					assertThat(getPartType(ShortTextPartType.class, p.getRight().getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject()).as("Object tag of " + p.getLeft()).hasText("original");
				}, entry.getKey());
			} else {
				consume(p -> {
					assertThat(p.getRight().getObjectTag(OBJTAG_NAME)).as("Object tag of " + p.getLeft()).isNotNull().isEnabled(false);
					assertThat(getPartType(ShortTextPartType.class, p.getRight().getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject()).as("Object tag of " + p.getLeft()).hasText("");
				}, entry.getKey());
			}
		}
	}

	@Test
	public void testUpdateVariantWithTag() throws NodeException {
		// change object tag of source page
		germanPage.setValue(updateObjectTag(germanPage.getRight(), "new tag content"));

		Map<Pair<String, Page>, String> expectedTagContents = new HashMap<>();
		expectedTagContents.put(localizedGermanPage, (feature && syncChannelset) ? "new tag content" : "original");
		expectedTagContents.put(englishPage, (feature && syncContentset) ? "new tag content" : "original");
		expectedTagContents.put(localizedEnglishPage, (feature && syncContentset && syncChannelset) ? "new tag content" : "original");
		expectedTagContents.put(variant, (feature && syncVariants) ? "new tag content" : "");

		for (Map.Entry<Pair<String, Page>, String> entry : expectedTagContents.entrySet()) {
			String description = "Object tag of " + entry.getKey().getLeft();
			consume(p -> {
				p = p.reload();
				assertThat(p.getObjectTag(OBJTAG_NAME)).as(description).isNotNull();
				assertThat(getPartType(ShortTextPartType.class, p.getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject()
						.getValueText()).as(description).isEqualTo(entry.getValue());
			}, entry.getKey().getRight());
		}
	}

	@Test
	public void testUpdateVariantWithoutTag() throws NodeException {
		// deactivate sync
		objProp = update(objProp, upd -> {
			upd.setSyncContentset(false);
		});

		// delete the objecttag in the translation
		englishPage.setValue(update(englishPage.getRight(), upd -> upd.getObjectTags().remove(OBJTAG_NAME)));

		// reset sync setting to test setup
		objProp = update(objProp, upd -> {
			upd.setSyncContentset(syncContentset);
		});

		// update source page
		germanPage.setValue(update(germanPage.getValue(), upd -> {
			getPartType(ShortTextPartType.class, upd.getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject().setValueText("new tag content");
		}));

		// assert
		if (feature && syncContentset) {
			consume(p -> {
				p = p.reload();
				assertThat(p.getObjectTag(OBJTAG_NAME)).isNotNull();
				assertThat(getPartType(ShortTextPartType.class, p.getObjectTag(OBJTAG_NAME), PART_NAME).getValueObject()).hasText("new tag content");
			}, englishPage.getRight());
		} else {
			consume(p -> {
				p = p.reload();
				assertThat(p.getObjectTag(OBJTAG_NAME)).isNull();
			}, englishPage.getRight());
		}
	}

	@Test
	public void testDeleteTag() throws NodeException {
		// delete the objecttag in the source page
		germanPage.setValue(update(germanPage.getRight(), upd -> upd.getObjectTags().remove(OBJTAG_NAME)));

		// assert
		Map<Pair<String, Page>, Boolean> expectedTagDeleted = new HashMap<>();
		expectedTagDeleted.put(localizedGermanPage, feature && syncChannelset);
		expectedTagDeleted.put(englishPage, feature && syncContentset);
		expectedTagDeleted.put(localizedEnglishPage, feature && syncContentset && syncChannelset);
		expectedTagDeleted.put(variant, feature && syncVariants);

		for (Map.Entry<Pair<String, Page>, Boolean> entry : expectedTagDeleted.entrySet()) {
			String description = "Object tag of " + entry.getKey().getLeft();
			if (entry.getValue()) {
				consume(p -> {
					p = p.reload();
					assertThat(p.getObjectTag(OBJTAG_NAME)).as(description).isNull();
				}, entry.getKey().getRight());
			} else {
				consume(p -> {
					p = p.reload();
					assertThat(p.getObjectTag(OBJTAG_NAME)).as(description).isNotNull();
				}, entry.getKey().getRight());
			}
		}
	}

	@Test
	public void testDisableEnableTag() throws NodeException {
		// disable tag in source page
		germanPage.setValue(update(germanPage.getRight(), upd -> upd.getObjectTag(OBJTAG_NAME).setEnabled(false)));

		// assert
		if (feature && syncContentset) {
			consume(p -> assertThat(p.getObjectTag(OBJTAG_NAME)).isEnabled(false), englishPage.getRight());
		} else {
			consume(p -> assertThat(p.getObjectTag(OBJTAG_NAME)).isEnabled(true), englishPage.getRight());
		}

		// enable tag in source page
		germanPage.setValue(update(germanPage.getRight(), upd -> upd.getObjectTag(OBJTAG_NAME).setEnabled(true)));

		// assert again
		consume(p -> assertThat(p.getObjectTag(OBJTAG_NAME)).isEnabled(true), englishPage.getRight());
	}

	@Test
	public void testDeletePage() throws NodeException {
		// delete the source page (without wastebin)
		consume(p -> p.delete(), germanPage.getRight());

		// english page should still have the object tag
		consume(p -> assertThat(p.getObjectTag(OBJTAG_NAME)).as("Object tag of translation").isNotNull(), englishPage.getRight());
	}
}
