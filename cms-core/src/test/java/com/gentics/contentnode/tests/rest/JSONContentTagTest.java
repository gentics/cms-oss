package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.ObjectModificationException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.tests.utils.TestedType;

@RunWith(value = Parameterized.class)
public class JSONContentTagTest extends AbstractJSONPropertyTest {

	protected static TemplateTag templateTag;

	@Parameters(name = "{index}: restriction {0}")
	public static Collection<Object[]> data() {
		return AbstractJSONPropertyTest.data();
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractJSONPropertyTest.setupOnce();

		templateTag = supply(() -> createTemplateTag(constructId, TAG_KEYWORD, true, false));
		template = update(template, template -> {
			template.getTemplateTags().put(templateTag.getName(), templateTag);
		});
	}

	@Parameter(0)
	public String jsonSchemaRestriction;

	@Parameter(1)
	public String correctAnswer;

	@Before
	public void setup() throws NodeException {
		operate(trx -> {
			Construct construct = trx.getObject(Construct.class, constructId, true);
			for (Part part: construct.getParts()) {
				if (part.getKeyname().equals(PART_KEYWORD)) {
					part.setInfoText(jsonSchemaRestriction);
					break;
				}
			}
			construct.save();
			trx.commit(false);
		});
		operate(() -> clear(node));
	}

	@Test
	public void testContentRight() throws NodeException {
		// create tested object
		LocalizableNodeObject<?> testedObject = supply(() -> TestedType.page.create(node.getFolder(), template));

		testedObject = update(((Page) testedObject), update -> {
			Value value = (Value) update.getContentTag(TAG_KEYWORD).getValues().get(PART_KEYWORD);
			value.setValueText(correctAnswer);
			value.save();
		});
		// assert all tags available and filled
		consume(o -> {
			assertThat(((Value) ((Page) o).getContentTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isEqualTo(correctAnswer);
		}, testedObject);
	}

	@Test(expected = ObjectModificationException.class)
	public void testContentWrong() throws NodeException {
		// create tested object
		LocalizableNodeObject<?> testedObject = supply(() -> TestedType.page.create(node.getFolder(), template));

		testedObject = update(((Page) testedObject), update -> {
			Value value = (Value) update.getContentTag(TAG_KEYWORD).getValues().get(PART_KEYWORD);
			value.setValueText(RANDOM_JSON);
			value.save();
		});
		// assert all tags available and filled
		consume(o -> {
			if (StringUtils.isNotBlank(jsonSchemaRestriction)) {
				assertThat(((Value) ((Page) o).getContentTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isNotEqualTo(RANDOM_JSON);
			} else {
				assertThat(((Value) ((Page) o).getContentTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isEqualTo(RANDOM_JSON);
				// Test passed
				throw new ObjectModificationException(PART_KEYWORD, PART_KEYWORD, PART_KEYWORD);
			}
		}, testedObject);
	}

	@Test
	public void testConstructDefaultValueRight() throws NodeException {
		Construct construct1 = supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			Construct construct = t.createObject(Construct.class);
			construct.setAutoEnable(true);
			construct.setKeyword("construct_with_" + TAG_KEYWORD);
			construct.setName("construct_with_" + TAG_KEYWORD, 1);
			if (node != null) {
				construct.getNodes().add(node);
			}

			Part part = t.createObject(Part.class);
			part.setEditable(1);
			part.setHidden(false);
			part.setKeyname(TAG_KEYWORD);
			part.setName(TAG_KEYWORD, 1);
			part.setPartTypeId(getPartTypeId(JSONPartType.class));

			Value value = t.createObject(Value.class);
			value.setValueText(correctAnswer);
			part.setDefaultValue(value);

			construct.getParts().add(part);

			construct.save();
			t.commit(false);
			return construct;
		});
		consume(o -> {
			assertThat((o.getParts().get(0).getDefaultValue()).getValueText()).isEqualTo(correctAnswer);
		}, construct1);
	}


	@Test(expected = ObjectModificationException.class)
	public void testConstructDefaultValueWrong() throws NodeException {
		Construct construct1 = supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			Construct construct = t.createObject(Construct.class);
			construct.setAutoEnable(true);
			construct.setKeyword("construct_with_" + TAG_KEYWORD);
			construct.setName("construct_with_" + TAG_KEYWORD, 1);
			if (node != null) {
				construct.getNodes().add(node);
			}

			Part part = t.createObject(Part.class);
			part.setInfoText(jsonSchemaRestriction);
			part.setEditable(1);
			part.setHidden(false);
			part.setKeyname(TAG_KEYWORD);
			part.setName(TAG_KEYWORD, 1);
			part.setPartTypeId(getPartTypeId(JSONPartType.class));

			Value value = t.createObject(Value.class);
			value.setValueText(RANDOM_JSON);
			part.setDefaultValue(value);

			construct.getParts().add(part);

			construct.save();
			t.commit(false);
			return construct;
		});
		consume(o -> {
			if (StringUtils.isNotBlank(jsonSchemaRestriction)) {
				assertThat((o.getParts().get(0).getDefaultValue()).getValueText()).isNotEqualTo(RANDOM_JSON);
			} else {
				assertThat((o.getParts().get(0).getDefaultValue()).getValueText()).isEqualTo(RANDOM_JSON);
				// Test passed
				throw new ObjectModificationException(PART_KEYWORD, PART_KEYWORD, PART_KEYWORD);
			}
		}, construct1);
	}

	@Test
	public void testTemplateRight() throws NodeException {
		template = update(template, template -> {
			Value value = (Value) template.getTemplateTags().get(TAG_KEYWORD).getTagValues().get(PART_KEYWORD);
			value.setValueText(correctAnswer);
			value.save();
		});

		// assert all tags available and filled
		consume(o -> {
			assertThat(((Value) o.getTemplateTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isEqualTo(correctAnswer);
		}, template);
	}

	@Test(expected = ObjectModificationException.class)
	public void testTemplateWrong() throws NodeException {
		template = update(template, template -> {
			Value value = (Value) template.getTemplateTags().get(TAG_KEYWORD).getTagValues().get(PART_KEYWORD);
			value.setValueText(RANDOM_JSON);
			value.save();
		});

		// assert all tags available and filled
		consume(o -> {
			if (StringUtils.isNotBlank(jsonSchemaRestriction)) {
				assertThat(((Value) o.getTemplateTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isNotEqualTo(RANDOM_JSON);
			} else {
				assertThat(((Value) o.getTemplateTag(TAG_KEYWORD).getValues().get(PART_KEYWORD)).getValueText()).isEqualTo(RANDOM_JSON);
				// Test passed
				throw new ObjectModificationException(PART_KEYWORD, PART_KEYWORD, PART_KEYWORD);
			}
		}, template);
	}
}
