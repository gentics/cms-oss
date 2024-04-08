package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for rendering the preview of an overview
 */
@RunWith(Parameterized.class)
public class RenderOverviewPreviewTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Construct overviewConstruct;
	private static Template template;
	private static Folder folder;
	private static Page page1;
	private static Page page2;
	private static Page page3;
	private static Page overviewPage;
	private static AtomicReference<String> contentTagName = new AtomicReference<>();
	private static int contentTagId;
	private static String constructName;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode());

		overviewConstruct = Builder.create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setIconName("");
			c.setKeyword("overview");
			c.setMayBeSubtag(true);
			c.setName("Overview", 1);
			c.getParts().add(Builder.create(Part.class, p -> {
				p.setEditable(1);
				p.setKeyname("ds");
				p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(OverviewPartType.class));
			}).doNotSave().build());
		}).build();

		template = Builder.create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setName("Template");
			t.setSource("");
		}).build();

		folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
		}).build();

		// create some target objects
		page1 = Builder.create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setName("Page One");
			p.setTemplateId(template.getId());
		}).at(1).publish().build();

		page2 = Builder.create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setName("Page Two");
			p.setTemplateId(template.getId());
		}).at(2).publish().build();

		page3 = Builder.create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setName("Page Three");
			p.setTemplateId(template.getId());
		}).at(3).publish().build();

		overviewPage = Builder.create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setName("Overview Page");
			p.setTemplateId(template.getId());
			contentTagName.set(p.getContent().addContentTag(overviewConstruct.getId()).getName());
		}).build();

		contentTagId = execute(p -> p.getContentTag(contentTagName.get()).getId(), overviewPage);
		constructName = execute(c -> c.getName().toString(), overviewConstruct);
	}

	@Parameters(name = "{index}: listType {0}, selectType {1}, orderBy {2}, orderDirection {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {ListType.PAGE, SelectType.MANUAL, OrderBy.SELF, OrderDirection.ASC, "Page One|Page Two|Page Three|"});
		data.add(new Object[] {ListType.PAGE, SelectType.MANUAL, OrderBy.SELF, OrderDirection.DESC, "Page Three|Page Two|Page One|"});
		data.add(new Object[] {ListType.PAGE, SelectType.MANUAL, OrderBy.ALPHABETICALLY, OrderDirection.ASC, "Page One|Page Three|Page Two|"});
		data.add(new Object[] {ListType.PAGE, SelectType.MANUAL, OrderBy.ALPHABETICALLY, OrderDirection.DESC, "Page Two|Page Three|Page One|"});
		return data;
	}

	@Parameter(0)
	public ListType listType;

	@Parameter(1)
	public SelectType selectType;

	@Parameter(2)
	public OrderBy orderBy;

	@Parameter(3)
	public OrderDirection orderDirection;

	@Parameter(4)
	public String expectedOverviewContent;

	@Test
	public void test() throws NodeException {
		PageLoadResponse loadResponse = supply(() -> new PageResourceImpl().load(String.valueOf(overviewPage.getId()), false, false, false, false, false, false, false, false, false, false, null, null));
		ContentNodeRESTUtils.assertResponseOK(loadResponse);
		com.gentics.contentnode.rest.model.Page pageModel = loadResponse.getPage();

		Tag tag = pageModel.getTags().get(contentTagName.get());
		Property prop = tag.getProperties().get("ds");

		Overview overview = new Overview();
		overview.setListType(listType);
		overview.setSelectType(selectType);
		overview.setOrderBy(orderBy);
		overview.setOrderDirection(orderDirection);

		switch(selectType) {
		case AUTO:
			break;
		case FOLDER:
			overview.setSelectedItemIds(Arrays.asList(folder.getId()));
			break;
		case MANUAL:
			switch (listType) {
			case PAGE:
				overview.setSelectedItemIds(Arrays.asList(page1.getId(), page2.getId(), page3.getId()));
				break;
			default:
				fail(String.format("ListType %s has not been implemented", listType));
				break;
			}
			break;
		default:
			fail(String.format("SelectType %s has not been implemented", selectType));
			break;
		}

		switch (listType) {
		case PAGE:
			overview.setSource("<node page.name>|");
			break;
		default:
			fail(String.format("ListType %s has not been implemented", listType));
			break;
		}

		prop.setOverview(overview);

		SystemUser systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		PageRenderResponse response = supply(systemUser, () -> new PageResourceImpl().renderTag(contentTagName.get(), null, null, LinksType.backend, pageModel));
		ContentNodeRESTUtils.assertResponseOK(response);

		assertThat(response.getContent()).as("Overview preview").isEqualTo(getExpectedContent(expectedOverviewContent));
	}

	protected String getExpectedContent(String overviewContent) {
		return String.format(
				"<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"%s\" data-gcn-i18n-constructname=\"%s\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s</div>",
				overviewPage.getId(), contentTagId, contentTagName.get(), constructName, contentTagId, overviewContent);
	}
}
