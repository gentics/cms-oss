package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.TemplateCopyRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for copying tags
 */
public class TagCopyTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);
	private static Node node;
	private static Construct innerConstruct;
	private static Construct outerConstruct;
	private static Construct overviewConstruct;
	private static Template template;

	/**
	 * Filter to find tags using {@link #outerConstruct}
	 */
	private static Predicate<ContentTag> isOuterTag = t -> {
		try {
			return t.getConstruct().equals(outerConstruct);
		} catch (NodeException e) {
			return false;
		}
	};

	/**
	 * Filter to find tags using {@link #innerConstruct}
	 */
	private static Predicate<ContentTag> isInnerTag = t -> {
		try {
			return t.getConstruct().equals(innerConstruct);
		} catch (NodeException e) {
			return false;
		}
	};

	private static SystemUser systemUser;
	private static Page otherPage;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());

		innerConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("inner");
			c.setName("Inner", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("html");
				p.setName("HTML", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {}, false));
			}, false));
		}));

		outerConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("outer");
			c.setName("Outer", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("html");
				p.setName("HTML", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {}, false));
			}, false));

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("bool");
				p.setName("Boolean", 1);
				p.setPartTypeId(getPartTypeId(CheckboxPartType.class));
				p.setDefaultValue(create(Value.class, v -> {}, false));
			}, false));
		}));

		overviewConstruct = Builder.create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("overview");
			c.setName("Overview", 1);

			c.getParts().add(Builder.create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("ds");
				p.setName("Overview", 1);
				p.setPartTypeId(getPartTypeId(OverviewPartType.class));
				p.setDefaultValue(Builder.create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		supply(() -> createObjectTagDefinition("overview", Page.TYPE_PAGE, overviewConstruct.getId(), node));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		systemUser = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(SystemUser.class, 1));

		otherPage = Builder.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Other Page");
		}).publish().build();
	}

	/**
	 * Test copying a tag containing a nested tag
	 * @throws NodeException
	 */
	@Test
	public void testCopyNested() throws NodeException {
		Page page = Trx.supply(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");

			ContentTag innerTag = p.getContent().addContentTag(innerConstruct.getId());
			ContentTag outerTag = p.getContent().addContentTag(outerConstruct.getId());
			getPartType(LongHTMLPartType.class, outerTag, "html").getValueObject().setValueText(String.format("<node %s>", innerTag.getName()));
			getPartType(CheckboxPartType.class, outerTag, "bool").getValueObject().setValueText("1");
		}));

		ContentTag outerTag = Trx.execute(p -> p.getContent().getContentTags().values().stream().filter(isOuterTag).findFirst()
				.orElseThrow(() -> new NodeException("Did not find original outer tag")), page);
		ContentTag innerTag = Trx.execute(p -> p.getContent().getContentTags().values().stream().filter(isInnerTag).findFirst()
				.orElseThrow(() -> new NodeException("Did not find original inner tag")), page);

		TagCreateResponse response = null;
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = ContentNodeRESTUtils.getPageResource();
			ContentTagCreateRequest request = new ContentTagCreateRequest();
			request.setCopyPageId(page.getId().toString());
			request.setCopyTagname(outerTag.getName());
			response = pageResource.createTag(page.getId().toString(), 0, null, request);
			trx.success();
		}

		page = Trx.execute(p -> TransactionManager.getCurrentTransaction().getObject(p), page);

		ContentTag newOuterTag = Trx.execute(p -> p.getContent().getContentTags().values().stream().filter(isOuterTag).filter(t -> !t.equals(outerTag))
				.findFirst().orElseThrow(() -> new NodeException("Did not find new outer tag")), page);
		ContentTag newInnerTag = Trx.execute(p -> p.getContent().getContentTags().values().stream().filter(isInnerTag).filter(t -> !t.equals(innerTag))
				.findFirst().orElseThrow(() -> new NodeException("Did not find new inner tag")), page);

		ContentNodeRESTUtils.assertResponseOK(response);
		assertThat(response.getTag().getId()).isEqualTo(newOuterTag.getId());
		assertThat(response.getTag().getProperties()).containsKeys("bool", "html");
		assertThat(response.getTag().getProperties().get("bool").getBooleanValue()).isTrue();
		assertThat(response.getTag().getProperties().get("html").getStringValue()).isEqualTo(String.format("<node %s>", newInnerTag.getName()));
	}

	/**
	 * Test copying a page containing an overview in a content tag
	 * @throws NodeException
	 */
	@Test
	public void testCopyContentTagOverview() throws NodeException {
		// create source page with overview in content tag
		AtomicReference<String> tagname = new AtomicReference<>();
		Page source = Builder.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");

			ContentTag overviewTag = p.getContent().addContentTag(overviewConstruct.getId());
			fillOverview(overviewTag, "ds", "", Page.class, Overview.SELECTIONTYPE_SINGLE, 5, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(otherPage));
			tagname.set(overviewTag.getName());
		}).build();

		// copy the source page
		Page copy = copyPage(source);

		// make assertions
		consume(p -> {
			assertThat(p).as("Copy").isNotEqualTo(source);

			ContentTag overviewTag = p.getContent().getContentTag(tagname.get());
			assertThat(overviewTag).as("Copied overview tag").isNotNull().hasConstruct(overviewConstruct.getGlobalId());
			Overview overview = getPartType(OverviewPartType.class, overviewTag, "ds").getOverview();

			assertThat(overview).as("Overview")
					.hasFieldOrPropertyWithValue("objectType", Page.TYPE_PAGE)
					.hasFieldOrPropertyWithValue("selectionType", Overview.SELECTIONTYPE_SINGLE)
					.hasFieldOrPropertyWithValue("maxObjects", 5)
					.hasFieldOrPropertyWithValue("orderKind", Overview.ORDER_NAME)
					.hasFieldOrPropertyWithValue("orderWay", Overview.ORDERWAY_ASC);

			List<NodeObject> selected = new ArrayList<>(overview.getSelectedObjects());
			assertThat(selected).as("Overview entries").containsExactly(otherPage);
		}, copy);
	}

	/**
	 * Test copying a page containing an overview in an object tag
	 * @throws NodeException
	 */
	@Test
	public void testCopyObjectTagOverview() throws NodeException {
		// create source page with overview in object tag
		Page source = Builder.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");

			ObjectTag objectTag = p.getObjectTag("overview");
			fillOverview(objectTag, "ds", "", Page.class, Overview.SELECTIONTYPE_SINGLE, 7, Overview.ORDER_CDATE, Overview.ORDERWAY_DESC, true, Arrays.asList(otherPage));
		}).build();

		// copy the source page
		Page copy = copyPage(source);

		// make assertions
		consume(p -> {
			assertThat(p).as("Copy").isNotEqualTo(source);

			ObjectTag overviewTag = p.getObjectTag("overview");
			assertThat(overviewTag).as("Copied overview tag").isNotNull().hasConstruct(overviewConstruct.getGlobalId());
			Overview overview = getPartType(OverviewPartType.class, overviewTag, "ds").getOverview();

			assertThat(overview).as("Overview")
					.hasFieldOrPropertyWithValue("objectType", Page.TYPE_PAGE)
					.hasFieldOrPropertyWithValue("selectionType", Overview.SELECTIONTYPE_SINGLE)
					.hasFieldOrPropertyWithValue("maxObjects", 7)
					.hasFieldOrPropertyWithValue("orderKind", Overview.ORDER_CDATE)
					.hasFieldOrPropertyWithValue("orderWay", Overview.ORDERWAY_DESC);

			List<NodeObject> selected = new ArrayList<>(overview.getSelectedObjects());
			assertThat(selected).as("Overview entries").containsExactly(otherPage);
		}, copy);
	}

	/**
	 * Test copyiong a template containing an overview in a template tag
	 * @throws NodeException
	 */
	@Test
	public void testCopyTemplateTagOverview() throws NodeException {
		// create template with overview tag
		String tagname = "overviewtag";
		Template source = Builder.create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Source Template");

			t.getTemplateTags().put(tagname, Builder.create(TemplateTag.class, tag -> {
				tag.setConstructId(overviewConstruct.getId());
				tag.setEnabled(true);
				tag.setName(tagname);
			}).doNotSave().build());

			fillOverview(t.getTemplateTag(tagname), "ds", "", Page.class, Overview.SELECTIONTYPE_SINGLE, 12,
					Overview.ORDER_SELECT, Overview.ORDERWAY_DESC, true, Arrays.asList(otherPage));
		}).build();

		// copy the source template
		Template copy = copyTemplate(source);

		// make assertions
		consume(t -> {
			assertThat(t).as("Copy").isNotEqualTo(source);

			TemplateTag overviewTag = copy.getTemplateTag(tagname);
			assertThat(overviewTag).as("Copied overview tag").isNotNull().hasConstruct(overviewConstruct.getGlobalId());
			Overview overview = getPartType(OverviewPartType.class, overviewTag, "ds").getOverview();

			assertThat(overview).as("Overview")
					.hasFieldOrPropertyWithValue("objectType", Page.TYPE_PAGE)
					.hasFieldOrPropertyWithValue("selectionType", Overview.SELECTIONTYPE_SINGLE)
					.hasFieldOrPropertyWithValue("maxObjects", 12)
					.hasFieldOrPropertyWithValue("orderKind", Overview.ORDER_SELECT)
					.hasFieldOrPropertyWithValue("orderWay", Overview.ORDERWAY_DESC);

			List<NodeObject> selected = new ArrayList<>(overview.getSelectedObjects());
			assertThat(selected).as("Overview entries").containsExactly(otherPage);
		}, copy);
	}

	/**
	 * Copy the given page
	 * @param source source page
	 * @return copy
	 * @throws NodeException
	 */
	protected Page copyPage(Page source) throws NodeException {
		AtomicInteger pageCopyId = new AtomicInteger();
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = ContentNodeRESTUtils.getPageResource();
			PageCopyRequest request = new PageCopyRequest();
			request.setCreateCopy(true);
			request.setSourcePageIds(Arrays.asList(source.getId()));
			request.setTargetFolders(Arrays.asList(new TargetFolder(node.getFolder().getId(), 0)));

			PageCopyResponse response = pageResource.copy(request, 0);
			assertResponseOK(response);
			assertThat(response.getPages()).as("Copied pages").hasSize(1);
			pageCopyId.set(response.getPages().get(0).getId());
			trx.success();
		}

		return supply(t -> t.getObject(Page.class, pageCopyId.get()));
	}

	/**
	 * Copy the given template
	 * @param source source template
	 * @return copy
	 * @throws NodeException
	 */
	protected Template copyTemplate(Template source) throws NodeException {
		AtomicInteger templateCopyId = new AtomicInteger();
		try (Trx trx = new Trx(systemUser)) {
			TemplateResourceImpl templateResource = new TemplateResourceImpl();
			TemplateCopyRequest request = new TemplateCopyRequest();
			request.setFolderId(node.getFolder().getId());

			TemplateLoadResponse response = templateResource.copy(Integer.toString(source.getId()), request);
			assertResponseOK(response);
			assertThat(response.getTemplate()).isNotNull();
			templateCopyId.set(response.getTemplate().getId());
			trx.success();
		}

		return supply(t -> t.getObject(Template.class, templateCopyId.get()));
	}
}
