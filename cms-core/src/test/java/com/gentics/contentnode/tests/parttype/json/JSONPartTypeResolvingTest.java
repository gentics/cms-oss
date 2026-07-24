package com.gentics.contentnode.tests.parttype.json;

import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.parttype.handlebars.HandlebarsPartTypeResolvingTest;

/**
 * Test cases for resolving attributes of other part types
 */
@RunWith(value = Parameterized.class)
public class JSONPartTypeResolvingTest extends HandlebarsPartTypeResolvingTest {

	@Parameter(2)
	public String defaultValue;

	@Parameter(3)
	public String handlebars;

	@Before
	@Override
	public void setup() throws NodeException {
		construct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct");
			c.setName("construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("hb");
				p.setName("Handlebars", 1);
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(testedClass));
				p.setEditable(0);
				p.setHidden(true);
				p.setKeyname("otherpart");
				p.setName("Other Part", 1);
				p.setDefaultValue(create(Value.class, v -> v.setValueText(defaultValue)).doNotSave().build());
			}).doNotSave().build());
		}).build();

		construct = update(construct, c -> {
			getPartType(HandlebarsPartType.class, c, "hb").setText(handlebars);
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Template");
			t.setSource(String.format("<node %s>", TAG_NAME));

			t.getTemplateTags().put(TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(construct.getId());
				tag.setEnabled(true);
				tag.setName(TAG_NAME);
				tag.setPublic(false);
			}).doNotSave().build());
		}).unlock().build();

		page = create(Page.class, p -> {
			p.setFolder(node, node.getFolder());
			p.setTemplateId(template.getId());
			p.setName("Page");
		}).unlock().build();
	}

	@Parameters(name = "{index}: keys {1} source {2} content {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = Arrays.asList(
			new Object[] { JSONPartType.class, "whatever, whoever", 
					"{\"whatever\":\"wherever\", \"whoever\":[\"me\", \"notme\"]}",
					HANDLEBARS_CONTENT_ITERATE_KEYS },
			new Object[] { JSONPartType.class, "0, 1", 
					"[\"me\", \"notme\"]", 
					HANDLEBARS_CONTENT_ITERATE_KEYS },
			new Object[] { JSONPartType.class, "", 
					"{}", 
					HANDLEBARS_CONTENT_ITERATE_KEYS },
			new Object[] { JSONPartType.class, "", 
					"[]", 
					HANDLEBARS_CONTENT_ITERATE_KEYS },
			new Object[] { JSONPartType.class, "", 
					"", 
					HANDLEBARS_CONTENT_ITERATE_KEYS },
			new Object[] { JSONPartType.class, "wherever, wherever, who", 
					"{\"whatever\":{\"whoever\": \"wherever\"}, \"whoever\":[{\"whatever\": \"wherever\"}], \"wherever\": [\"what\", \"who\", \"where\"]}", 
					"{{cms.tag.parts.otherpart.whatever.whoever}}, {{cms.tag.parts.otherpart.whoever.0.whatever}}, {{cms.tag.parts.otherpart.wherever.1}}" },
			new Object[] { JSONPartType.class, "wherever, wherever, who", 
					"{\"whatever\":{\"whoever\": \"wherever\"}, \"whoever\":[{\"whatever\": \"wherever\"}], \"wherever\": [\"what\", \"who\", \"where\"]}", 
					"{{json_path cms.tag.parts.otherpart \"$[*]['whoever']\"}}, {{json_path cms.tag.parts.otherpart \"$.whoever[0].whatever\"}}, {{json_path cms.tag.parts.otherpart \"$.wherever[1]\"}}" }
		);
		return data;
	}
}
