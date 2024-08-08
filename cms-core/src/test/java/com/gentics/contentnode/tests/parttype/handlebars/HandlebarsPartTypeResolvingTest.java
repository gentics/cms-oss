package com.gentics.contentnode.tests.parttype.handlebars;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLTextPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for resolving attributes of other part types
 */
@RunWith(value = Parameterized.class)
public class HandlebarsPartTypeResolvingTest {
	public final static String TAG_NAME = "testtag";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	@Parameters(name = "{index}: template {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = Arrays.asList(
			new Object[] { CheckboxPartType.class, "checked" },
			new Object[] { CmsFormPartType.class, "target" },
			new Object[] { DatasourcePartType.class, "items, key, keys, selection, value, values" },
			new Object[] { MultiSelectPartType.class, "items, key, keys, selection, value, values" },
			new Object[] { SingleSelectPartType.class, "items, key, keys, selection, value, values" },
			new Object[] { ChangeableListPartType.class, "count, lines, ordered" },
			new Object[] { OrderedListPartType.class, "count, lines" },
			new Object[] { UnorderedListPartType.class, "count, lines" },
			new Object[] { NodePartType.class, "id, name" },
			new Object[] { OverviewPartType.class, "count, items, listType, maxItems, orderBy, orderDirection, recursive, selectType" },
			new Object[] { PageTagPartType.class, "container, id, page_id, tag" },
			new Object[] { TemplateTagPartType.class, "container, id, page_id, tag" },
			new Object[] { HTMLPartType.class, "text" },
			new Object[] { HTMLTextPartType.class, "text" },
			new Object[] { LongHTMLPartType.class, "text" },
			new Object[] { LongHTMLTextPartType.class, "text" },
			new Object[] { NormalTextPartType.class, "text" },
			new Object[] { ShortTextPartType.class, "text" },
			new Object[] { FileURLPartType.class , "internal, node, nodeId, target, url" },
			new Object[] { FolderURLPartType.class, "internal, node, nodeId, target, url" },
			new Object[] { ImageURLPartType.class, "internal, node, nodeId, target, url"},
			new Object[] { PageURLPartType.class, "internal, node, nodeId, target, url" }
		);
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Test Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("test.node.hostname");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();
	}

	@Parameter(0)
	public Class<? extends PartType> testedClass;

	@Parameter(1)
	public String expectedResolvableKeys;

	protected Construct construct;

	protected Template template;

	protected Page page;

	@Before
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
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		construct = update(construct, c -> {
			getPartType(HandlebarsPartType.class, c, "hb").setText("{{#each cms.tag.parts.otherpart}}{{@key}}{{#unless @last}}, {{/unless}}{{/each}}");
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

	@After
	public void teardown() throws NodeException {
		if (page != null) {
			consume(p -> p.delete(true), page);
			page = null;
		}

		if (template != null) {
			consume(t -> t.delete(true), template);
			template = null;
		}

		if (construct != null) {
			consume(c -> c.delete(true), construct);
			construct = null;
		}
	}

	@Test
	public void test() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.render()).as("Rendered page").isEqualTo(expectedResolvableKeys);
			trx.success();
		}
	}
}
