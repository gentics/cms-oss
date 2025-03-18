package com.gentics.contentnode.tests.linkchecker;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.object.Node.TYPE_NODE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_VIEW;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.exception.FeatureRequiredException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkCheckHistoryEntry;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkList;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatistics;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.model.linkchecker.PageList;
import com.gentics.contentnode.rest.model.linkchecker.PageWithExternalLinks;
import com.gentics.contentnode.rest.model.linkchecker.UpdateExternalLinkStatusRequest;
import com.gentics.contentnode.rest.resource.impl.LinkCheckerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.LinkCheckerFilterParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.google.common.base.Objects;

/**
 * Test cases for the link checker
 */
@GCNFeature(set = { Feature.LINK_CHECKER, Feature.WASTEBIN })
public class LinkCheckerTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(ExternalUrlResource.class).build()));

	private static Comparator<PageWithExternalLinks> comparator = (p1, p2) -> {
		if (!Objects.equal(p1.getPage().getId(), p2.getPage().getId())) {
			return -1;
		}
		List<Integer> linkIds1 = p1.getLinks().stream().map(ExternalLink::getId).collect(Collectors.toList());
		List<Integer> linkIds2 = p2.getLinks().stream().map(ExternalLink::getId).collect(Collectors.toList());
		if (!Objects.equal(linkIds1, linkIds2)) {
			return -1;
		}
		if (!Objects.equal(p1.getEditable(), p2.getEditable())) {
			return -1;
		}
		return 0;
	};

	private static Node nodeWithFeature;
	private static Node nodeWithoutFeature;
	private static Construct magicLinkConstruct;
	private static Template template;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static UserGroup testGroup;

	private static SystemUser testUser;

	/**
	 * Create static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupData() throws Exception {
		testContext.getContext().getTransaction().commit();
		nodeWithFeature = supply(() -> createNode());
		consume(n -> n.activateFeature(Feature.LINK_CHECKER), nodeWithFeature);
		nodeWithoutFeature = supply(() -> createNode());

		magicLinkConstruct = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword(AlohaRenderer.MAGICLINK_KEYWORD);
			c.setName("Aloha Link", 1);
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("url");
				p.setName("URL", 1);
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
			}, false));
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("text");
				p.setName("Text", 1);
				p.setPartTypeId(getPartTypeId(ShortTextPartType.class));
			}, false));
		}));
		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setFolderId(nodeWithFeature.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.setName("Template");
			tmpl.setSource("");
			tmpl.getTemplateTags().put("link1", create(TemplateTag.class, tTag -> {
				tTag.setConstructId(magicLinkConstruct.getId());
				tTag.setEnabled(true);
				tTag.setName("link1");
				tTag.setPublic(true);
			}, false));
			tmpl.getTemplateTags().put("link2", create(TemplateTag.class, tTag -> {
				tTag.setConstructId(magicLinkConstruct.getId());
				tTag.setEnabled(true);
				tTag.setName("link2");
				tTag.setPublic(true);
			}, false));
			tmpl.getTemplateTags().put("link3", create(TemplateTag.class, tTag -> {
				tTag.setConstructId(magicLinkConstruct.getId());
				tTag.setEnabled(true);
				tTag.setName("link3");
				tTag.setPublic(true);
			}, false));
		}));

		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(testGroup)));
	}

	/**
	 * Clear test nodes
	 * @throws NodeException
	 */
	@Before
	public void clearData() throws NodeException {
		for (Node n : Arrays.asList(nodeWithFeature, nodeWithoutFeature)) {
			consume(node -> {
				clear(node);
			}, n);
		}

		operate(() -> {
			String perm = new Permission(PERM_VIEW, PERM_PAGE_VIEW).toString();
			setPermissions(TYPE_NODE, nodeWithFeature.getFolder().getId(), Arrays.asList(testGroup), perm);
		});

		LinkChecker.resetRetryAfterMap();
	}

	@After
	public void checkIntegrity() throws NodeException {
		operate(() -> {
			Set<Integer> elsWithoutContent = DBUtils.select("SELECT el.id FROM external_link el LEFT JOIN content c ON el.content_id = c.id WHERE c.id IS NULL",
					DBUtils.IDS);
			assertThat(elsWithoutContent).as("External Links without content").isEmpty();
			Set<Integer> elsWithoutTag = DBUtils.select("SELECT el.id FROM external_link el LEFT JOIN contenttag c ON el.contenttag_id = c.id WHERE c.id IS NULL",
					DBUtils.IDS);
			assertThat(elsWithoutTag).as("External Links without tag").isEmpty();
			Set<Integer> elsWithoutValue = DBUtils.select("SELECT el.id FROM external_link el LEFT JOIN value v ON el.value_id = v.id WHERE v.id IS NULL",
					DBUtils.IDS);
			assertThat(elsWithoutValue).as("External Links without value").isEmpty();
		});
	}

	/**
	 * Test reading external links for page for which the feature is activated
	 * @throws NodeException
	 */
	@Test
	public void testReadWithFeature() throws NodeException {
		Page page = supply(() -> createPage(nodeWithFeature.getFolder(), template, "Page"));
		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").isEmpty();
	}

	/**
	 * Test reading external links for page for which the feature is not activated
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = FeatureRequiredException.class, message = "Das erforderliche Feature link_checker ist nicht aktiviert.")
	public void testReadWithoutFeature() throws NodeException {
		Page page = supply(() -> createPage(nodeWithoutFeature.getFolder(), template, "Page"));
		supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
	}

	/**
	 * Test reading external links for a page with sufficient permissions
	 * @throws NodeException
	 */
	@Test
	public void testReadPermission() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLinkList response = supply(testUser, () -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
	}

	/**
	 * Test reading external links for a page with insufficient permissions
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = InsufficientPrivilegesException.class, message = "Sie haben zu wenig Berechtigungen auf die angegebene Seite.")
	public void testReadNoPermission() throws NodeException {
		operate(() -> {
			String perm = new Permission(PERM_VIEW).toString();
			setPermissions(TYPE_NODE, nodeWithFeature.getFolder().getId(), Arrays.asList(testGroup), perm);
		});

		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLinkList response = supply(testUser, () -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
	}

	/**
	 * Test listing pages with permission
	 * @throws NodeException
	 */
	@Test
	public void testListPermission() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		Collection<ExternalLink> pageLinks = supply(() -> LinkChecker.load(page.getContent().getId()));

		PageList response = supply(testUser, () -> new LinkCheckerResourceImpl().getPages(null, null, null, null, null));
		assertResponseCodeOk(response);

		assertThat(response.getItems()).as("Pages").usingElementComparator(comparator).containsOnly(
				build(page, pageLinks, false));
	}

	/**
	 * Test listing pages without permission
	 * @throws NodeException
	 */
	@Test
	public void testListNoPermission() throws NodeException {
		operate(() -> {
			String perm = new Permission(PERM_VIEW).toString();
			setPermissions(TYPE_NODE, nodeWithFeature.getFolder().getId(), Arrays.asList(testGroup), perm);
		});

		supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		PageList response = supply(testUser, () -> new LinkCheckerResourceImpl().getPages(null, null, null, null, null));
		assertResponseCodeOk(response);

		assertThat(response.getItems()).as("Pages").isEmpty();
	}

	/**
	 * Test finding external links in page while saving
	 * @throws NodeException
	 */
	@Test
	public void testSaveExternalLinks() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
			getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget("http://www.gentics.com/b");
			getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link b");
			getPartType(PageURLPartType.class, p.getContentTag("link3"), "url").setExternalTarget("http://www.gentics.com/c");
			getPartType(ShortTextPartType.class, p.getContentTag("link3"), "text").getValueObject().setValueText("link c");
		}));

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int contenttag2Id = supply(() -> page.getContentTag("link2").getId());
		int contenttag3Id = supply(() -> page.getContentTag("link3").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());
		int value2Id = supply(() -> page.getContentTag("link2").getValues().getByKeyname("url").getId());
		int value3Id = supply(() -> page.getContentTag("link3").getValues().getByKeyname("url").getId());

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastCheckTimestamp(0)
							.setLastStatus(ExternalLinkStatus.unchecked)
							.setText("link a")
							.setUrl("http://www.gentics.com/a")
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastCheckTimestamp(0)
							.setLastStatus(ExternalLinkStatus.unchecked)
							.setText("link b")
							.setUrl("http://www.gentics.com/b")
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastCheckTimestamp(0)
							.setLastStatus(ExternalLinkStatus.unchecked)
							.setText("link c")
							.setUrl("http://www.gentics.com/c")
							.setContenttagId(contenttag3Id)
							.setContenttagName("link3")
							.setPartName("URL")
							.setValueId(value3Id));
	}

	/**
	 * Test updating an external link
	 * @throws NodeException
	 */
	@Test
	public void testUpdateExternalLink() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		ExternalLink externalLink = response.getItems().get(0);

		execute(p -> update(p, upd -> {
			getPartType(PageURLPartType.class, upd.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/b");
			getPartType(ShortTextPartType.class, upd.getContentTag("link1"), "text").getValueObject().setValueText("link b");
		}), page);
		externalLink.setUrl("http://www.gentics.com/b");
		externalLink.setText("link b");

		response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External links after update").usingFieldByFieldElementComparator().containsOnly(externalLink);
	}

	/**
	 * Test removing an external link (making it internal)
	 * @throws NodeException
	 */
	@Test
	public void testRemoveExternalLink() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);

		execute(p -> update(p, upd -> {
			getPartType(PageURLPartType.class, upd.getContentTag("link1"), "url").setTargetPage(upd);
			getPartType(ShortTextPartType.class, upd.getContentTag("link1"), "text").getValueObject().setValueText("link b");
		}), page);

		response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External links after update").isEmpty();
	}

	/**
	 * Test updating the check status of an external link
	 * @throws NodeException
	 */
	@Test
	public void testUpdateStatus() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
			getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget("http://www.gentics.com/b");
			getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link b");
			getPartType(PageURLPartType.class, p.getContentTag("link3"), "url").setExternalTarget("http://www.gentics.com/c");
			getPartType(ShortTextPartType.class, p.getContentTag("link3"), "text").getValueObject().setValueText("link c");
		}));

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int contenttag2Id = supply(() -> page.getContentTag("link2").getId());
		int contenttag3Id = supply(() -> page.getContentTag("link3").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());
		int value2Id = supply(() -> page.getContentTag("link2").getValues().getByKeyname("url").getId());
		int value3Id = supply(() -> page.getContentTag("link3").getValues().getByKeyname("url").getId());

		ExternalLink externalLink = get(page, "http://www.gentics.com/a");

		operate(() -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(page.getId()), new UpdateExternalLinkStatusRequest()
				.setItems(Arrays.asList(new ExternalLink().setId(externalLink.getId()).setLastStatus(ExternalLinkStatus.valid)))));

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);

		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id", "history", "lastCheckTimestamp")
			.containsOnly(
				new ExternalLink()
					.setContentId(contentId)
					.setLastStatus(ExternalLinkStatus.valid)
					.setText("link a")
					.setUrl("http://www.gentics.com/a")
					.setContenttagId(contenttag1Id)
					.setContenttagName("link1")
					.setPartName("URL")
					.setValueId(value1Id),
				new ExternalLink()
					.setContentId(contentId)
					.setLastStatus(ExternalLinkStatus.unchecked)
					.setText("link b")
					.setUrl("http://www.gentics.com/b")
					.setContenttagId(contenttag2Id)
					.setContenttagName("link2")
					.setPartName("URL")
					.setValueId(value2Id),
				new ExternalLink()
					.setContentId(contentId)
					.setLastStatus(ExternalLinkStatus.unchecked)
					.setText("link c")
					.setUrl("http://www.gentics.com/c")
					.setContenttagId(contenttag3Id)
					.setContenttagName("link3")
					.setPartName("URL")
					.setValueId(value3Id));
	}

	/**
	 * Test updating external link status with permission
	 * @throws NodeException
	 */
	@Test
	public void testUpdateStatusPermission() throws NodeException {
		operate(() -> {
			String perm = new Permission(PERM_VIEW, PERM_PAGE_VIEW, PERM_PAGE_UPDATE).toString();
			setPermissions(TYPE_NODE, nodeWithFeature.getFolder().getId(), Arrays.asList(testGroup), perm);
		});

		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLink externalLink = get(page, "http://www.gentics.com/a");

		operate(testUser, () -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(page.getId()), new UpdateExternalLinkStatusRequest()
				.setItems(Arrays.asList(new ExternalLink().setId(externalLink.getId()).setLastStatus(ExternalLinkStatus.valid)))));
	}

	/**
	 * Test updating external link status without permission
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = InsufficientPrivilegesException.class, message = "Sie haben zu wenig Berechtigungen auf die angegebene Seite.")
	public void testUpdateStatusNoPermission() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLink externalLink = get(page, "http://www.gentics.com/a");

		operate(testUser, () -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(page.getId()), new UpdateExternalLinkStatusRequest()
				.setItems(Arrays.asList(new ExternalLink().setId(externalLink.getId()).setLastStatus(ExternalLinkStatus.valid)))));
	}

	/**
	 * Test that when updating the status of an external link, the history will be truncated to the last five entries
	 * @throws NodeException
	 */
	@Test
	public void testHistoryTruncation() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		Collection<ExternalLink> links = supply(() -> LinkChecker.load(page.getContent().getId()));
		for (int timestamp : Arrays.asList(1, 2, 3, 4, 5, 6)) {
			ExternalLinkStatus status = timestamp % 2 == 0 ? ExternalLinkStatus.valid : ExternalLinkStatus.invalid;

			links.forEach(link -> link.setLastStatus(status));
			try (Trx trx = new Trx()) {
				trx.at(timestamp);
				LinkChecker.updateStatus(links);
				trx.success();
			}
		}

		Map<Integer, List<ExternalLinkCheckHistoryEntry>> statusHistoryMap = supply(() -> LinkChecker.loadStatusHistory(links, ExternalLink::getId));
		for (List<ExternalLinkCheckHistoryEntry> history : statusHistoryMap.values()) {
			assertThat(history).as("Status check history").usingFieldByFieldElementComparator().containsExactly(
					new ExternalLinkCheckHistoryEntry().setTimestamp(2).setStatus(ExternalLinkStatus.valid),
					new ExternalLinkCheckHistoryEntry().setTimestamp(3).setStatus(ExternalLinkStatus.invalid),
					new ExternalLinkCheckHistoryEntry().setTimestamp(4).setStatus(ExternalLinkStatus.valid),
					new ExternalLinkCheckHistoryEntry().setTimestamp(5).setStatus(ExternalLinkStatus.invalid),
					new ExternalLinkCheckHistoryEntry().setTimestamp(6).setStatus(ExternalLinkStatus.valid));
		}
	}

	/**
	 * Test that when updating an existing URL the history is cleared and the status is set back to "unchecked"
	 * @throws NodeException
	 */
	@Test
	public void testClearHistoryOnUpdate() throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());

		Collection<ExternalLink> links = supply(() -> LinkChecker.load(page.getContent().getId()));
		for (int timestamp : Arrays.asList(1, 2, 3, 4, 5)) {
			ExternalLinkStatus status = timestamp % 2 == 0 ? ExternalLinkStatus.valid : ExternalLinkStatus.invalid;

			links.forEach(link -> link.setLastStatus(status));
			try (Trx trx = new Trx()) {
				trx.at(timestamp);
				LinkChecker.updateStatus(links);
				trx.success();
			}
		}

		operate(() -> update(page, upd -> {
			getPartType(ShortTextPartType.class, upd.getContentTag("link1"), "text").getValueObject().setValueText("link b");
		}));

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastCheckTimestamp(5)
							.setLastStatus(ExternalLinkStatus.invalid)
							.setText("link b")
							.setUrl("http://www.gentics.com/a")
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id));

		assertThat(response.getItems().get(0).getHistory()).as("Status check history").usingFieldByFieldElementComparator().containsExactly(
				new ExternalLinkCheckHistoryEntry().setTimestamp(1).setStatus(ExternalLinkStatus.invalid),
				new ExternalLinkCheckHistoryEntry().setTimestamp(2).setStatus(ExternalLinkStatus.valid),
				new ExternalLinkCheckHistoryEntry().setTimestamp(3).setStatus(ExternalLinkStatus.invalid),
				new ExternalLinkCheckHistoryEntry().setTimestamp(4).setStatus(ExternalLinkStatus.valid),
				new ExternalLinkCheckHistoryEntry().setTimestamp(5).setStatus(ExternalLinkStatus.invalid));

		operate(() -> update(page, upd -> {
			getPartType(PageURLPartType.class, upd.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/b");
		}));

		response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastCheckTimestamp(0)
							.setLastStatus(ExternalLinkStatus.unchecked)
							.setText("link b")
							.setUrl("http://www.gentics.com/b")
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id));
		assertThat(response.getItems().get(0).getHistory()).as("Status check history").isNullOrEmpty();
	}

	/**
	 * Test filtering pages for "valid" or "invalid" external links
	 * @throws NodeException
	 */
	@Test
	public void testStatusFiltering() throws NodeException {
		Page pageA = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
			getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget("http://www.gentics.com/b");
			getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link b");
		}));

		Page pageB = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
			getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget("http://www.gentics.com/b");
			getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link b");
		}));

		ExternalLink externalLink = get(pageA, "http://www.gentics.com/a");
		externalLink.setLastStatus(ExternalLinkStatus.valid);
		consume(link -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(pageA.getId()),
				new UpdateExternalLinkStatusRequest().setItems(Arrays.asList(link))), externalLink);

		externalLink = get(pageB, "http://www.gentics.com/b");
		externalLink.setLastStatus(ExternalLinkStatus.invalid);
		consume(link -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(pageB.getId()),
				new UpdateExternalLinkStatusRequest().setItems(Arrays.asList(link))), externalLink);

		Collection<ExternalLink> pageALinks = supply(() -> LinkChecker.load(pageA.getContent().getId()));
		Collection<ExternalLink> pageAValidLinks = pageALinks.stream().filter(link -> link.getLastStatus() == ExternalLinkStatus.valid).collect(Collectors.toList());
		Collection<ExternalLink> pageBLinks = supply(() -> LinkChecker.load(pageB.getContent().getId()));
		Collection<ExternalLink> pageBInvalidLinks = pageBLinks.stream().filter(link -> link.getLastStatus() == ExternalLinkStatus.invalid).collect(Collectors.toList());

		PageList response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, null, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Unfiltered pages").usingElementComparator(comparator).containsOnly(
				build(pageA, pageALinks),
				build(pageB, pageBLinks));

		// filter for "valid"
		LinkCheckerFilterParameterBean filter = new LinkCheckerFilterParameterBean();
		filter.status = Collections.singleton(ExternalLinkStatus.valid);
		response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, filter, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Pages with valid links").usingElementComparator(comparator).containsOnly(
				build(pageA, pageAValidLinks));

		// filter for "invalid"
		filter.status = Collections.singleton(ExternalLinkStatus.invalid);
		response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, filter, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Pages with valid links").usingElementComparator(comparator).containsOnly(
				build(pageB, pageBInvalidLinks));
	}

	/**
	 * Test filtering pages by languages
	 * @throws NodeException
	 */
	@Test
	public void testLanguageFiltering() throws NodeException {
		ContentLanguage english = getLanguage("en");
		String englishId = execute(ContentLanguage::getId, english).toString();
		ContentLanguage german = getLanguage("de");
		String germanId = execute(ContentLanguage::getId, german).toString();

		Page englishPage = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setLanguage(english);
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));
		Collection<ExternalLink> englishPageLinks = supply(() -> LinkChecker.load(englishPage.getContent().getId()));
		PageWithExternalLinks englishWithLinks = build(englishPage, englishPageLinks);

		Page germanPage = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setLanguage(german);
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));
		Collection<ExternalLink> germanPageLinks = supply(() -> LinkChecker.load(germanPage.getContent().getId()));
		PageWithExternalLinks germanWithLinks = build(germanPage, germanPageLinks);

		Page noLanguagePage = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));
		Collection<ExternalLink> noLanguagePageLinks = supply(() -> LinkChecker.load(noLanguagePage.getContent().getId()));
		PageWithExternalLinks noLanguageWithLinks = build(noLanguagePage, noLanguagePageLinks);

		// get unfiltered
		PageList response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, null, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Unfiltered pages").usingElementComparator(comparator).containsOnly(
				englishWithLinks,
				germanWithLinks,
				noLanguageWithLinks);

		// filter tests
		LinkCheckerFilterParameterBean filter = new LinkCheckerFilterParameterBean();
		List<Pair<Collection<String>, Collection<PageWithExternalLinks>>> testCases = new ArrayList<>();

		// english by code
		testCases.add(Pair.of(Arrays.asList("en"), Arrays.asList(englishWithLinks)));
		// german by id
		testCases.add(Pair.of(Arrays.asList(germanId), Arrays.asList(germanWithLinks)));
		// mix code and id
		testCases.add(Pair.of(Arrays.asList("de", englishId), Arrays.asList(englishWithLinks, germanWithLinks)));
		// filter for 0 (no language)
		testCases.add(Pair.of(Arrays.asList("0"), Arrays.asList(noLanguageWithLinks)));
		// mix language with no language
		testCases.add(Pair.of(Arrays.asList("de", "0"), Arrays.asList(noLanguageWithLinks, germanWithLinks)));
		// filter for inexistent language
		testCases.add(Pair.of(Arrays.asList("bla"), Collections.emptyList()));

		for (Pair<Collection<String>, Collection<PageWithExternalLinks>> testCase : testCases) {
			filter.language = new HashSet<>(testCase.getLeft());
			response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, filter, null));
			assertResponseCodeOk(response);
			assertThat(response.getItems()).as("Pages filtered by " + testCase.getLeft()).usingElementComparator(comparator)
					.containsOnlyElementsOf(testCase.getRight());
		}
	}

	/**
	 * Test filtering pages by online status
	 * @throws NodeException
	 */
	@Test
	public void testOnlineFiltering() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeWithFeature.getFolder(), "Folder"));
		Page offline = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/404");
		}));
		Page online = supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/200");
		}), Page::publish));

		Collection<ExternalLink> offlineLinks = supply(() -> LinkChecker.load(offline.getContent().getId()));
		Collection<ExternalLink> onlineLinks = supply(() -> LinkChecker.load(online.getContent().getId()));

		PageList response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, null, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Unfiltered pages").usingElementComparator(comparator).containsOnly(
				build(online, onlineLinks),
				build(offline, offlineLinks));

		// filter for "online"
		LinkCheckerFilterParameterBean filter = new LinkCheckerFilterParameterBean();
		filter.online = true;
		response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, filter, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Online pages").usingElementComparator(comparator).containsOnly(
				build(online, onlineLinks));

		// filter for "offline"
		filter.online = false;
		response = supply(() -> new LinkCheckerResourceImpl().getPages(null, null, null, filter, null));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Offline pages").usingElementComparator(comparator).containsOnly(
				build(offline, offlineLinks));
	}

	/**
	 * Test checking a valid URL
	 * @throws NodeException
	 */
	@Test
	public void test200() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/200", true, null);
	}

	/**
	 * Test following redirects
	 * @throws NodeException
	 */
	@Test
	public void test301() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/301", true, null);
	}

	/**
	 * Test checking an invalid URL (not found)
	 * @throws NodeException
	 */
	@Test
	public void test404() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/404", false, "404");
	}

	/**
	 * Test checking a valid URL, that does not allow HEAD requests
	 * @throws NodeException
	 */
	@Test
	public void test200After405() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/get/200", true, null);
	}

	/**
	 * Test checking an invalid URL, that does not allow HEAD requests
	 * @throws NodeException
	 */
	@Test
	public void test404After405() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/get/404", false, "404");
	}

	/**
	 * Test checking a valid URL, that does not allow HEAD requests and has
	 * status "Service Unavailable" (503)
	 * @throws NodeException
	 */
	@Test
	public void test200After503() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/getAfter503/200", true, null);
	}

	/**
	 * Test checking an invalid URL, that does not allow HEAD requests and has
	 * status "Service Unavailable" (503)
	 * @throws NodeException
	 */
	@Test
	public void test404After503() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/getAfter503/404", false, "404");
	}

	/**
	 * Test checking a valid URL, that crashes on HEAD requests with "Internal Server Error" (500)
	 * @throws NodeException
	 */
	@Test
	public void test200After500() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/getAfter500/200", true, null);
	}

	/**
	 * Test checking an invalid URL, that crashes on HEAD requests with "Internal Server Error" (500)
	 * @throws NodeException
	 */
	@Test
	public void test404After500() throws NodeException {
		testCheck(appContext.getBaseUri() + "url/getAfter500/404", false, "404");
	}

	/**
	 * Test checking a URL with unknown host
	 * @throws NodeException
	 */
	@Test
	public void testUnknownHost() throws NodeException {
		testCheck("http://unknown.host.gentics.com/", false, "unknown host");
	}

	/**
	 * Test an invalid URL, with only scheme filled
	 * @throws NodeException
	 */
	@Test
	public void testInvalidUrlSchemeOnly() throws NodeException {
		testInvalidUrl("http:");
	}

	/**
	 * Test checking an incomplete 'http://' URL
	 * @throws NodeException
	 */
	@Test
	public void testIncompleteUrl() throws NodeException {
		testInvalidUrl("http://");
	}

	/**
	 * Test checking URL with broken 'http:host' content
	 * @throws NodeException
	 */
	@Test
	public void testUrlBrokenSchemaNoSlashes() throws NodeException {
		testInvalidUrl("http:gentics.com");
	}

	/**
	 * Test checking URL with broken 'http:///host' content
	 * @throws NodeException
	 */
	@Test
	public void testUrlBrokenSchemaTooManySlashes() throws NodeException {
		testInvalidUrl("http:///gentics.com");
	}

	/**
	 * This tests cutting of the error message at 100+ characters. We do this because error messages longer than
	 * 256 characters can't be written into the database as last reason.
	 */
	@Test
	public void testTooLongLastReason() throws NodeException {
		ExternalLink link = new ExternalLink().setLastReason(
				"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
		assertThat(link.getLastReason()).as("Shortened Last Reason").isEqualTo(
				"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789...");
	}

	/**
	 * Test that callto: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testCallto() throws NodeException {
		testUnchecked("callto:1234567890");
	}

	/**
	 * Test that mailto: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testMailto() throws NodeException {
		testUnchecked("mailto:n.pomaroli@gentics.com");
	}

	/**
	 * Test that javascript: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testJavascript() throws NodeException {
		testUnchecked("javascript:alert('not tested');");
	}

	/**
	 * Test that file: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testFile() throws NodeException {
		testUnchecked("file:c:\\bla.html");
	}

	/**
	 * Test that tel: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testTel() throws NodeException {
		testUnchecked("tel:12345678980");
	}

	/**
	 * Test that skype: links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testSkype() throws NodeException {
		testUnchecked("skype:norbert");
	}

	/**
	 * Test that # links are unchecked
	 * @throws NodeException
	 */
	@Test
	public void testAnchor() throws NodeException {
		testUnchecked("#relative");
	}

	/**
	 * Assert that duplicate URLs are checked only once
	 * @throws NodeException
	 */
	@Test
	public void testDuplicateUrl() throws NodeException {
		String url = appContext.getBaseUri() + "url/count";

		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(url);
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
			getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget(url);
			getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link b");
			getPartType(PageURLPartType.class, p.getContentTag("link3"), "url").setExternalTarget(url);
			getPartType(ShortTextPartType.class, p.getContentTag("link3"), "text").getValueObject().setValueText("link c");
		}));

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int contenttag2Id = supply(() -> page.getContentTag("link2").getId());
		int contenttag3Id = supply(() -> page.getContentTag("link3").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());
		int value2Id = supply(() -> page.getContentTag("link2").getValues().getByKeyname("url").getId());
		int value3Id = supply(() -> page.getContentTag("link3").getValues().getByKeyname("url").getId());

		ExternalUrlResource.counter.set(0);
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setText("link b")
							.setUrl(url)
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setText("link c")
							.setUrl(url)
							.setContenttagId(contenttag3Id)
							.setContenttagName("link3")
							.setPartName("URL")
							.setValueId(value3Id)
						);

		assertThat(ExternalUrlResource.counter.get()).as("Number of requests").isEqualTo(1);
	}

	/**
	 * Test timeout settings
	 * @throws NodeException
	 */
	@Test
	public void testTimeout() throws NodeException {
		try (PropertyTrx pTrx = new PropertyTrx("link_checker.call_timeout", "1")) {
			testCheck(appContext.getBaseUri() + "url/timeout", false, "timeout");
		}
		try (PropertyTrx pTrx = new PropertyTrx("link_checker.call_timeout", "3")) {
			testCheck(appContext.getBaseUri() + "url/timeout", true, null);
		}
	}

	/**
	 * Test that cancelling a page (without actual changes) does not remove external link checks
	 * @throws NodeException
	 */
	@Test
	public void testCancel() throws NodeException {
		String url = appContext.getBaseUri() + "url/200";

		Page page = supply(t -> {
			t.setTimestamp(1000L);
			return create(Page.class, p -> {
				p.setFolderId(nodeWithFeature.getFolder().getId());
				p.setTemplateId(template.getId());
				p.setName("Page");
				getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(url);
				getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");

				getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget(url);
				getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link a");
			});
		});

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int contenttag2Id = supply(() -> page.getContentTag("link2").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());
		int value2Id = supply(() -> page.getContentTag("link2").getValues().getByKeyname("url").getId());

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links before cancel").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id));

		operate(t -> {
			t.setTimestamp(2000L);
			getPageResource().cancel(page.getId(), null);
		});

		response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links after cancel").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id));
	}

	/**
	 * Test that restoring an older page version only removes external link checks of the links that actually change
	 * @throws NodeException
	 */
	@Test
	public void testRestore() throws NodeException {
		String url = appContext.getBaseUri() + "url/200";
		String newUrl = appContext.getBaseUri() + "url/301";

		Page page = supply(t -> {
			t.setTimestamp(1000L);
			return create(Page.class, p -> {
				p.setFolderId(nodeWithFeature.getFolder().getId());
				p.setTemplateId(template.getId());
				p.setName("Page");
				getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(url);
				getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");

				getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget(url);
				getPartType(ShortTextPartType.class, p.getContentTag("link2"), "text").getValueObject().setValueText("link a");
			});
		});

		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int contenttag2Id = supply(() -> page.getContentTag("link2").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());
		int value2Id = supply(() -> page.getContentTag("link2").getValues().getByKeyname("url").getId());

		operate(t -> {
			t.setTimestamp(2000L);
			update(page, p -> {
				getPartType(PageURLPartType.class, p.getContentTag("link2"), "url").setExternalTarget(newUrl);
			});
		});

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links before restore").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(newUrl)
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id));

		operate(t -> {
			t.setTimestamp(3000L);
			getPageResource().restoreVersion(Integer.toString(page.getId()), 1);
		});

		response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links after restore").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.valid)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id),
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(ExternalLinkStatus.unchecked)
							.setLastReason(null)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag2Id)
							.setContenttagName("link2")
							.setPartName("URL")
							.setValueId(value2Id));
	}

	/**
	 * Test getting stats
	 * @throws NodeException
	 */
	@Test
	public void testStats() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeWithFeature.getFolder(), "Folder"));
		supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/200");
		}));
		supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/404");
		}));

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		// get stats
		ExternalLinkStatistics stats = supply(() -> new LinkCheckerResourceImpl().getStats(nodeWithFeature.getId(), null));
		assertThat(stats).as("Node stats").hasFieldOrPropertyWithValue("valid", 1).hasFieldOrPropertyWithValue("invalid", 1);
		stats = supply(() -> new LinkCheckerResourceImpl().getStats(null, null));
		assertThat(stats).as("Global stats").hasFieldOrPropertyWithValue("valid", 1).hasFieldOrPropertyWithValue("invalid", 1);

		// delete folder (together with pages)
		consume(Folder::delete, folder);

		// get stats
		stats = supply(() -> new LinkCheckerResourceImpl().getStats(nodeWithFeature.getId(), null));
		assertThat(stats).as("Node stats").hasFieldOrPropertyWithValue("valid", 0).hasFieldOrPropertyWithValue("invalid", 0);
		stats = supply(() -> new LinkCheckerResourceImpl().getStats(null, null));
		assertThat(stats).as("Global stats").hasFieldOrPropertyWithValue("valid", 0).hasFieldOrPropertyWithValue("invalid", 0);
	}

	/**
	 * Test getting stats with online status restriction
	 * @throws NodeException
	 */
	@Test
	public void testStatsOnline() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeWithFeature.getFolder(), "Folder"));
		supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/200");
		}));
		supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/404");
		}));
		supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/200");
		}), Page::publish));
		supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/404");
		}), Page::publish));
		supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/200");
		}), Page::publish));
		supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(appContext.getBaseUri() + "url/404");
		}), Page::publish));

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		// get all stats
		ExternalLinkStatistics stats = supply(() -> new LinkCheckerResourceImpl().getStats(nodeWithFeature.getId(), null));
		assertThat(stats).as("Node stats").hasFieldOrPropertyWithValue("valid", 3).hasFieldOrPropertyWithValue("invalid", 3);

		// get stats for online pages
		stats = supply(() -> new LinkCheckerResourceImpl().getStats(nodeWithFeature.getId(), true));
		assertThat(stats).as("Node online stats").hasFieldOrPropertyWithValue("valid", 2).hasFieldOrPropertyWithValue("invalid", 2);

		// get stats for offline pages
		stats = supply(() -> new LinkCheckerResourceImpl().getStats(nodeWithFeature.getId(), false));
		assertThat(stats).as("Node online stats").hasFieldOrPropertyWithValue("valid", 1).hasFieldOrPropertyWithValue("invalid", 1);
	}

	@Test
	public void testRateLimiting() throws NodeException, InterruptedException {
		LinkChecker.init();

		// request from "Rate Limiting URL"
		Pair<ExternalLinkStatus, String> result = LinkChecker.check(appContext.getBaseUri() + "url/429");
		// result is "unchecked"
		assertThat(result.getLeft()).as("Check result").isEqualTo(ExternalLinkStatus.unchecked);
		assertThat(result.getRight()).as("Reason").isEqualTo("429");

		// request from 200
		result = LinkChecker.check(appContext.getBaseUri() + "url/200");
		// result is "unchecked" (because host is blocked for some time)
		assertThat(result.getLeft()).as("Check result").isEqualTo(ExternalLinkStatus.unchecked);
		assertThat(result.getRight()).as("Reason").isEqualTo("Blocked due to prior 429");

		// wait some time (11 seconds, which is more than the Retry-After of 10s)
		Thread.sleep(11 * 1000);

		// request from 200 again
		result = LinkChecker.check(appContext.getBaseUri() + "url/200");
		// result is "valid"
		assertThat(result.getLeft()).as("Check result").isEqualTo(ExternalLinkStatus.valid);
		assertThat(result.getRight()).as("Reason").isNull();
	}

	protected void testInvalidUrl(String url) throws NodeException {
		testCheck(url, false, "Invalid URL: " + url);
	}

	/**
	 * Test check for the given url
	 * @param url url
	 * @param valid true iff url is expected to be valid, false if not
	 * @param reason reason (null if expected valid)
	 * @throws NodeException
	 */
	protected void testCheck(String url, boolean valid, String reason) throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(url);
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));
		int contentId = supply(() -> page.getContent().getId());
		int contenttag1Id = supply(() -> page.getContentTag("link1").getId());
		int value1Id = supply(() -> page.getContentTag("link1").getValues().getByKeyname("url").getId());

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").usingElementComparatorIgnoringFields("id", "lastCheckTimestamp", "history")
				.containsOnly(
						new ExternalLink()
							.setContentId(contentId)
							.setLastStatus(valid ? ExternalLinkStatus.valid : ExternalLinkStatus.invalid)
							.setLastReason(reason)
							.setText("link a")
							.setUrl(url)
							.setContenttagId(contenttag1Id)
							.setContenttagName("link1")
							.setPartName("URL")
							.setValueId(value1Id));
	}

	/**
	 * Test that the given url is not checked at all
	 * @param url url
	 * @throws NodeException
	 */
	protected void testUnchecked(String url) throws NodeException {
		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(nodeWithFeature.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget(url);
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		LinkChecker.init();
		LinkChecker.start();
		try {
			LinkChecker.checkLinks(null).blockingAwait();
		} finally {
			LinkChecker.stop();
		}

		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("External Links").isEmpty();
	}

	/**
	 * Get the external link entry for the page with the given url (fails if not found)
	 * @param page page
	 * @param url url
	 * @return external link entry (never null)
	 * @throws NodeException
	 */
	protected ExternalLink get(Page page, String url) throws NodeException {
		ExternalLinkList response = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId())));
		assertResponseCodeOk(response);
		Optional<ExternalLink> optionalLink = response.getItems().stream().filter(link -> Objects.equal(url, link.getUrl())).findFirst();
		assertThat(optionalLink).isPresent();
		return optionalLink.get();
	}

	/**
	 * Build an page with external links reference object
	 * @param page page
	 * @param links collection of external links
	 * @return reference object
	 * @throws NodeException
	 */
	protected PageWithExternalLinks build(Page page, Collection<ExternalLink> links) throws NodeException {
		return build(page, links, true);
	}

	/**
	 * Build an page with external links reference object
	 * @param page page
	 * @param links collection of external links
	 * @param editable editable flag
	 * @return reference object
	 * @throws NodeException
	 */
	protected PageWithExternalLinks build(Page page, Collection<ExternalLink> links, boolean editable) throws NodeException {
		return supply(() -> new PageWithExternalLinks().setPage(ModelBuilder.getPage(page)).setLinks(links).setEditable(editable));
	}

	/**
	 * Test resource
	 */
	@Path("/url")
	public final static class ExternalUrlResource {
		/**
		 * Request counter
		 */
		public static AtomicInteger counter = new AtomicInteger();

		/**
		 * Return requested code (for 301, also add location header for redirection to url/200)
		 * @param code requested code
		 * @return response
		 * @throws URISyntaxException
		 */
		@HEAD
		@Path("/{code}")
		public Response head(@PathParam("code") int code) throws URISyntaxException {
			switch (code) {
			case HttpStatus.SC_MOVED_PERMANENTLY:
				return Response.status(code).location(new URI(appContext.getBaseUri() + "url/200")).build();
			case 429:
				return Response.status(code).header("Retry-After", "10").build();
			default:
				return Response.status(code).build();
			}
		}

		/**
		 * Return requested code
		 * @param code code
		 * @return response
		 */
		@GET
		@Path("/{code}")
		public Response get(@PathParam("code") int code) {
			return Response.status(code).build();
		}

		/**
		 * Return requested code
		 * @param code code
		 * @return response
		 */
		@GET
		@Path("/get/{code}")
		public Response getOnly(@PathParam("code") int code) {
			return Response.status(code).build();
		}

		/**
		 * Always return 405 (Method not allowed) with "allow" header set to "GET"
		 * @param code code
		 * @return response
		 */
		@HEAD
		@Path("/get/{code}")
		public Response headOnly(@PathParam("code") int code) {
			return Response.status(HttpStatus.SC_METHOD_NOT_ALLOWED).allow("GET").build();
		}

		/**
		 * Return requested code
		 * @param code code
		 * @return response
		 */
		@GET
		@Path("/getAfter500/{code}")
		public Response getOnly500(@PathParam("code") int code) {
			return Response.status(code).build();
		}

		/**
		 * Always return 503 (Service Unavailable) with "allow" header set to "GET"
		 * @param code code
		 * @return response
		 */
		@HEAD
		@Path("/getAfter500/{code}")
		public Response headOnly500(@PathParam("code") int code) {
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}

		/**
		 * Return requested code
		 * @param code code
		 * @return response
		 */
		@GET
		@Path("/getAfter503/{code}")
		public Response getOnly503(@PathParam("code") int code) {
			return Response.status(code).build();
		}

		/**
		 * Always return 503 (Service Unavailable) with "allow" header set to "GET"
		 * @param code code
		 * @return response
		 */
		@HEAD
		@Path("/getAfter503/{code}")
		public Response headOnly503(@PathParam("code") int code) {
			return Response.status(HttpStatus.SC_SERVICE_UNAVAILABLE).build();
		}

		/**
		 * Return "ok" response and count requests
		 * @return response
		 */
		@HEAD
		@Path("/count")
		public Response count() {
			counter.incrementAndGet();
			return Response.ok().build();
		}

		/**
		 * Return "ok" response after waiting 2 seconds
		 * @return response
		 * @throws InterruptedException
		 */
		@HEAD
		@Path("/timeout")
		public Response timeout() throws InterruptedException {
			Thread.sleep(2000);
			return Response.ok().build();
		}
	}
}
