package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.OverviewSetting;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.SelectSetting;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.ConstructCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ConstructCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.DatasourceEntryListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PagedConstructListResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ConstructParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the {@link ConstructResource}
 */
public class ConstructResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static UserGroup group;
	private static SystemUser user;
	private static Node node1;
	private static Node node2;
	private static Template template;
	private static Page page1;
	private static Page page2;
	private static Datasource firstDs;
	private static Datasource secondDs;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		operate(t -> {
			for (Page page : t.getObjects(Page.class, DBUtils.select("SELECT id FROM page", DBUtils.IDS))) {
				page.delete(true);
			}
			for (Template template : t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS))) {
				template.delete(true);
			}
			for (ObjectTag objectTag : t.getObjects(ObjectTag.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id != 0", DBUtils.IDS))) {
				objectTag.delete();
			}
			for (ObjectTagDefinition objectProperty : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS))) {
				objectProperty.delete();
			}
			for (Construct construct : t.getObjects(Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDS))) {
				construct.delete();
			}
		});

		node1 = supply(() -> createNode());
		node2 = supply(() -> createNode());

		Integer noNodeConstructId = supply(() -> createConstruct(null, ShortTextPartType.class, "nonode", "text"));
		Integer node1ConstructId = supply(() -> createConstruct(node1, LongHTMLPartType.class, "node1", "text"));
		Integer node2ConstructId = supply(() -> createConstruct(node2, CheckboxPartType.class, "node2", "text"));
		Integer bothNodeConstructId = supply(t -> {
			Integer constructId = createConstruct(null, OverviewPartType.class, "both", "ds");
			update(t.getObject(Construct.class, constructId), c -> {
				c.getNodes().add(node1);
				c.getNodes().add(node2);
			});
			return constructId;
		});

		template = supply(() -> createTemplate(node1.getFolder(), "Template"));

		page1 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node1.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1");

			p.getContent().addContentTag(noNodeConstructId);
			p.getContent().addContentTag(node2ConstructId);
		}));

		page2 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 2");
		}));

		firstDs = supply(() -> createDatasource("firstds", Arrays.asList("one", "two", "three")));
		secondDs = supply(() -> createDatasource("secondds", Arrays.asList("four", "five", "six")));
	}

	@Before
	public void setup() throws NodeException {
		// delete pages
		List<Page> preservedPages = Arrays.asList(page1, page2);
		operate(t -> {
			for (Page page : t.getObjects(Page.class, DBUtils.select("SELECT id FROM page", DBUtils.IDS))) {
				if (!preservedPages.contains(page)) {
					page.delete(true);
				}
			}
		});

		// delete templates
		List<Template> preservedTemplates = Arrays.asList(template);
		operate(t -> {
			for (Template tmpl : t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS))) {
				if (!preservedTemplates.contains(tmpl)) {
					tmpl.delete(true);
				}
			}
		});

		// delete object property definitions
		operate(t -> {
			for (ObjectTagDefinition def : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS))) {
				def.delete(true);
			}
		});

		// delete constructs
		List<String> preserve = Arrays.asList("nonode", "node1", "node2", "both");
		operate(t -> {
			for (Construct construct : t.getObjects(Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDS))) {
				if (!preserve.contains(construct.getKeyword())) {
					construct.delete();
				}
			}
		});
	}

	@Test
	public void testSystemConstructs() throws NodeException {
		PagedConstructListResponse response = supply(() -> new ConstructResourceImpl().list(null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getItems()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testSystemConstructsPerms() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		PagedConstructListResponse response = supply(user, () -> new ConstructResourceImpl().list(null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getItems()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
		assertThat(response.getPerms()).as("Construct permissions").isNull();
	}

	@Test
	public void testSystemConstructsChangeable() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.changeable = true;
		PagedConstructListResponse response = supply(user, () -> new ConstructResourceImpl().list(null, null, null, constructFilter, null, null));
		assertResponseOK(response);

		assertThat(response.getItems()).as("System constructs").usingElementComparatorOnFields("keyword").containsOnly(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"));
	}

	@Test
	public void testSystemConstructsPermInfo() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		PermsParameterBean perms = new PermsParameterBean();
		perms.perms = true;
		PagedConstructListResponse response = supply(user, () -> new ConstructResourceImpl().list(null, null, null, null, perms, null));
		assertResponseOK(response);

		assertThat(response.getItems()).as("System constructs").usingElementComparatorOnFields("keyword").containsOnly(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
		assertThat(response.getPerms()).as("Construct permissions").isNotNull();
		assertThat(getConstructPermissions(response, "nonode")).as("Permissions of construct nonode").containsOnly(Permission.view, Permission.edit, Permission.delete);
		assertThat(getConstructPermissions(response, "node1")).as("Permissions of construct node1").containsOnly(Permission.view);
		assertThat(getConstructPermissions(response, "node2")).as("Permissions of construct node2").containsOnly(Permission.view, Permission.edit, Permission.delete);
		assertThat(getConstructPermissions(response, "both")).as("Permissions of construct both").containsOnly(Permission.view);
	}

	@Test
	public void testSystemConstructsNotChangeable() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.changeable = false;
		PagedConstructListResponse response = supply(user, () -> new ConstructResourceImpl().list(null, null, null, constructFilter, null, null));
		assertResponseOK(response);

		assertThat(response.getItems()).as("System constructs").usingElementComparatorOnFields("keyword").containsOnly(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testNodeConstructs() throws NodeException {
		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.nodeId = supply(() -> node1.getId());
		PagedConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(null, null, null, constructFilter, null, null),
				Triple.of(Node.TYPE_NODE, supply(() -> node1.getFolder().getId()), PermHandler.PERM_VIEW));
		assertResponseOK(response);

		assertThat(response.getItems()).as("Node constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testPageConstructs1() throws NodeException {
		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.pageId = supply(() -> page1.getId());
		Integer folderId = supply(() -> node1.getFolder().getId());

		PagedConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(null, null, null, constructFilter, null, null),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_PAGE_VIEW));
		assertResponseOK(response);

		assertThat(response.getItems()).as("Page 1 constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testPageConstructs2() throws NodeException {
		ConstructParameterBean constructFilter = new ConstructParameterBean();
		constructFilter.pageId = supply(() -> page2.getId());
		Integer folderId = supply(() -> node2.getFolder().getId());

		PagedConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(null, null, null, constructFilter, null, null),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_PAGE_VIEW));
		assertResponseOK(response);

		assertThat(response.getItems()).as("Page 2 constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldSystemConstructs() throws NodeException {
		ConstructListResponse response = supply(
				() -> new ConstructResourceImpl().list(0, -1, null, null, null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldSystemConstructsPerms() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		ConstructListResponse response = supply(user,
				() -> new ConstructResourceImpl().list(0, -1, null, null, null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldSystemConstructsChangeable() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		ConstructListResponse response = supply(user,
				() -> new ConstructResourceImpl().list(0, -1, null, true, null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"));
	}

	@Test
	public void testOldSystemConstructsNotChangeable() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		operate(() -> setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group),
				PermHandler.EMPTY_PERM));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		ConstructListResponse response = supply(user,
				() -> new ConstructResourceImpl().list(0, -1, null, false, null, null, null, null, null, null));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("System constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldNodeConstructs() throws NodeException {
		ConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(0, -1, null, null, null, node1.getId(), null, null, null, null),
				Triple.of(Node.TYPE_NODE, supply(() -> node1.getFolder().getId()), PermHandler.PERM_VIEW));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("Node constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldPageConstructs1() throws NodeException {
		Integer folderId = supply(() -> node1.getFolder().getId());

		ConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(0, -1, null, null, page1.getId(), null, null, null, null, null),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_PAGE_VIEW));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("Page 1 constructs").usingElementComparatorOnFields("keyword").contains(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("nonode"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node1"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testOldPageConstructs2() throws NodeException {
		Integer folderId = supply(() -> node2.getFolder().getId());

		ConstructListResponse response = assertRequiredPermissions(group, user,
				() -> new ConstructResourceImpl().list(0, -1, null, null, page2.getId(), null, null, null, null, null),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_PAGE_VIEW));
		assertResponseOK(response);

		assertThat(response.getConstructs()).as("Page 2 constructs").usingElementComparatorOnFields("keyword").containsOnly(
				new com.gentics.contentnode.rest.model.Construct().setKeyword("node2"),
				new com.gentics.contentnode.rest.model.Construct().setKeyword("both"));
	}

	@Test
	public void testCreate() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);

		// check the list contents
		PagedConstructListResponse constructList = supply(() -> new ConstructResourceImpl().list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(), new ConstructParameterBean(), null, null));
		assertResponseCodeOk(constructList);
		assertThat(constructList.getItems()).as("Constructs of node").usingElementComparatorOnFields("id").contains(response.getConstruct());
	}


	@Test(expected = NodeException.class)
	public void testCreateNoNode() throws NodeException {
		createRandomConstruct();
	}

	@Test
	public void testDelete() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);

		// check the list contents
		PagedConstructListResponse constructList = supply(() -> new ConstructResourceImpl().list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(), new ConstructParameterBean(), null, null));
		assertResponseCodeOk(constructList);
		assertThat(constructList.getItems()).as("Constructs of node").usingElementComparatorOnFields("id").contains(response.getConstruct());

		// deletion
		GenericResponse deleted = supply(() -> new ConstructResourceImpl().delete(response.getConstruct().getId().toString()));
		assertResponseCodeOk(deleted);

		// check the list contents
		constructList = supply(() -> new ConstructResourceImpl().list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(), new ConstructParameterBean(), null, null));
		assertResponseCodeOk(constructList);
		assertThat(constructList.getItems()).as("Constructs of node").usingElementComparatorOnFields("id").doesNotContain(response.getConstruct());
	}

	/**
	 * Test that deleting a construct, which is used in a template fails with a proper error message
	 * @throws NodeException
	 */
	@Test
	public void testDeleteUsedInTemplate() throws NodeException {
		int constructId = Builder.create(Construct.class, c -> {
			c.setKeyword("deleteme");
			c.setIconName("icon.png");
			c.setName("Lösch mich", 1);
			c.setName("Delete Me", 2);
		}).save().build().getId();

		Builder.create(Template.class, tmpl -> {
			tmpl.setFolderId(node1.getFolder().getId());
			tmpl.setName("Template using construct");
			TemplateTag tag = Builder.create(TemplateTag.class, t -> {
				t.setConstructId(constructId);
				t.setName("tag");
			}).doNotSave().build();
			tmpl.getTemplateTags().put("tag", tag);
		}).save().build();

		exceptionChecker.expect(EntityInUseException.class, String.format("Der Tagtyp %s kann nicht gelöscht werden, weil er noch verwendet wird.", "Lösch mich"));
		supply(() -> new ConstructResourceImpl().delete(String.valueOf(constructId)));
	}

	/**
	 * Test that deleting a construct, which is used in a page (as content tag) fails with a proper error message
	 * @throws NodeException
	 */
	@Test
	public void testDeleteUsedInPage() throws NodeException {
		int constructId = Builder.create(Construct.class, c -> {
			c.setKeyword("deleteme");
			c.setIconName("icon.png");
			c.setName("Lösch mich", 1);
			c.setName("Delete Me", 2);
		}).save().build().getId();

		Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node1.getFolder().getId());
			p.getContent().addContentTag(constructId);
		}).save().build();

		exceptionChecker.expect(EntityInUseException.class, String.format("Der Tagtyp %s kann nicht gelöscht werden, weil er noch verwendet wird.", "Lösch mich"));
		supply(() -> new ConstructResourceImpl().delete(String.valueOf(constructId)));
	}

	/**
	 * Test that deleting a construct, which is used in an object property, fails with a proper error message
	 * @throws NodeException
	 */
	@Test
	public void testDeleteUsedInObjectProperty() throws NodeException {
		int constructId = Builder.create(Construct.class, c -> {
			c.setKeyword("deleteme");
			c.setIconName("icon.png");
			c.setName("Lösch mich", 1);
			c.setName("Delete Me", 2);
		}).save().build().getId();

		Builder.create(ObjectTagDefinition.class, o -> {
			ObjectTag tag = o.getObjectTag();
			tag.setConstructId(constructId);
			tag.setName("object.reference");
			tag.setObjType(Folder.TYPE_FOLDER);
		}).build();

		exceptionChecker.expect(EntityInUseException.class, String.format("Der Tagtyp %s kann nicht gelöscht werden, weil er noch verwendet wird.", "Lösch mich"));
		supply(() -> new ConstructResourceImpl().delete(String.valueOf(constructId)));
	}

	@Test
	public void testLoad() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);

		// read it
		ConstructLoadResponse read = supply(() -> new ConstructResourceImpl().get(response.getConstruct().getId().toString(), null));
		assertResponseCodeOk(read);
		assertEquals(read.getConstruct().getId(), response.getConstruct().getId());
		assertEquals(read.getConstruct().getNameI18n(), response.getConstruct().getNameI18n());
		assertEquals(read.getConstruct().getKeyword(), response.getConstruct().getKeyword());
	}

	@Test
	public void givenConstructWithReferencedCategoryRequest_shouldHaveEmbeddedCategoryInResponse() throws NodeException {
		com.gentics.contentnode.rest.model.Construct createdConstruct = createRandomConstruct(node1).getConstruct();
		ConstructCategory createdCategory = createRandomConstructCategory().getConstructCategory();

		createdConstruct.setCategoryId(createdCategory.getId());
		createdConstruct.setCategory(createdCategory);
		new ConstructResourceImpl().update(createdConstruct.getId().toString(), createdConstruct, Collections.emptyList());

		com.gentics.contentnode.rest.model.Construct retrievedConstruct = new ConstructResourceImpl().get(
				createdConstruct.getId().toString(),
				new EmbedParameterBean().withEmbed("category")).getConstruct();


		org.assertj.core.api.Assertions.assertThat(retrievedConstruct).as("Referenced category id should match")
				.hasFieldOrPropertyWithValue("categoryId", createdCategory.getId());
		org.assertj.core.api.Assertions.assertThat(retrievedConstruct.getCategory()).as("Referenced category should match")
				.hasFieldOrPropertyWithValue("globalId", createdCategory.getGlobalId());
		org.assertj.core.api.Assertions.assertThat(retrievedConstruct.getCategory()).as("Referenced category should match")
				.hasFieldOrPropertyWithValue("name", createdCategory.getName());
	}


	@Test
	public void givenListConstructsWithReferencedCategoryRequest_shouldHaveEmbeddedCategoriesInResponse() throws NodeException {
		com.gentics.contentnode.rest.model.Construct createdConstruct = createRandomConstruct(node1).getConstruct();
		ConstructCategory createdCategory = createRandomConstructCategory().getConstructCategory();

		createdConstruct.setCategoryId(createdCategory.getId());
		createdConstruct.setCategory(createdCategory);
		new ConstructResourceImpl().update(createdConstruct.getId().toString(), createdConstruct, Collections.emptyList());


		PagedConstructListResponse response = supply(() -> new ConstructResourceImpl().list(
				null, null, null, null, null, new EmbedParameterBean().withEmbed("category")));
		assertResponseOK(response);

		com.gentics.contentnode.rest.model.Construct retrievedConstruct = response.getItems().stream()
				.filter(construct -> construct.getId() == createdConstruct.getId())
				.findAny()
				.get();


		org.assertj.core.api.Assertions.assertThat(retrievedConstruct).as("Referenced category id should match")
				.hasFieldOrPropertyWithValue("categoryId", createdCategory.getId());
		org.assertj.core.api.Assertions.assertThat(retrievedConstruct.getCategory()).as("Referenced category should match")
				.hasFieldOrPropertyWithValue("globalId", createdCategory.getGlobalId());
		org.assertj.core.api.Assertions.assertThat(retrievedConstruct.getCategory()).as("Referenced category should match")
				.hasFieldOrPropertyWithValue("name", createdCategory.getName());
	}

	@Test
	public void testUpdate() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);
		com.gentics.contentnode.rest.model.Construct construct = response.getConstruct();
		String oldKw = construct.getKeyword();

		construct.setKeyword(MiscUtils.getRandomNameOfLength(8));
		construct.setDescription(MiscUtils.getRandomNameOfLength(24), "en");
		assertNotEquals(oldKw, construct.getKeyword());

		String newKw = construct.getKeyword();
		Map<String, String> description = construct.getDescriptionI18n();
		Map<String, String> name = construct.getNameI18n();
		Integer id = construct.getId();

		// update it
		ConstructLoadResponse updated = new ConstructResourceImpl().update(construct.getId().toString(), construct, Collections.emptyList());
		assertResponseCodeOk(updated);
		assertEquals(updated.getConstruct().getId(), id);
		assertEquals(updated.getConstruct().getNameI18n(), name);
		assertEquals(updated.getConstruct().getKeyword(), newKw);
		assertEquals(updated.getConstruct().getDescriptionI18n(), description);
	}

	@Test
	public void whenUpdatingConstructPartsOnly_constructPropertiesShouldNotChange() throws NodeException {
		final List nodeList = Collections.singletonList(execute(Node::getId, node1));

		com.gentics.contentnode.rest.model.Construct construct = new com.gentics.contentnode.rest.model.Construct();
		construct.setKeyword("testtag");
		construct.setIcon("etc.gif");
		construct.setName("Select", "en");

		construct.setMayBeSubtag(true);
		construct.setMayContainSubtags(true);
		construct.setAutoEnable(true);
		construct.setNewEditor(true);
		construct.setVisibleInMenu(true);

		List<Part> parts = this.createConstructParts();
		construct.setParts(parts);
		ConstructLoadResponse createdConstruct = new ConstructResourceImpl().create(construct, nodeList);
		assertResponseOK(createdConstruct);
		assertThat(createdConstruct.getConstruct()).as("Created construct").isNotNull();

		// Update only parts
		com.gentics.contentnode.rest.model.Construct updateConstruct = new com.gentics.contentnode.rest.model.Construct();
		Part textPart = new Part();
		textPart.setTypeId(supply(() -> getPartTypeId(ShortTextPartType.class)));
		textPart.setName("input2");


		List<Part> updatedParts = new ArrayList<>();
		updatedParts.addAll(parts);
		updatedParts.add(textPart);
		updateConstruct.setParts(updatedParts);

		final Integer constructId = createdConstruct.getConstruct().getId();
		ConstructLoadResponse updateResponse = new ConstructResourceImpl().update(constructId.toString(), updateConstruct, nodeList);

		// should not affect construct properties
		assertResponseOK(updateResponse);
		assertThat(updateResponse.getConstruct().getParts()).hasSize(3);
		assertThat(updateResponse.getConstruct().getMayBeSubtag()).isTrue();
		assertThat(updateResponse.getConstruct().getMayContainSubtags()).isTrue();
		Assertions.assertThat(updateResponse.getConstruct().getNewEditor()).isTrue();
		assertThat(updateResponse.getConstruct().getAutoEnable()).isTrue();
	}

	@Test
	public void whenUpdatingConstructProperties_constructPartsShouldNotChange() throws NodeException {
		final List nodeList = Collections.singletonList(execute(Node::getId, node1));

		com.gentics.contentnode.rest.model.Construct construct = new com.gentics.contentnode.rest.model.Construct();
		construct.setKeyword("testtag");
		construct.setIcon("etc.gif");
		construct.setName("Select", "en");

		List<Part> parts = this.createConstructParts();
		construct.setParts(parts);

		ConstructLoadResponse createdConstruct = new ConstructResourceImpl().create(construct, nodeList);
		assertResponseOK(createdConstruct);
		assertThat(createdConstruct.getConstruct()).as("Created construct").isNotNull();

		// Update only properties
		com.gentics.contentnode.rest.model.Construct updateConstruct = new com.gentics.contentnode.rest.model.Construct();
		updateConstruct.setMayBeSubtag(true);
		updateConstruct.setMayContainSubtags(true);
		updateConstruct.setAutoEnable(true);
		updateConstruct.setNewEditor(true);
		updateConstruct.setVisibleInMenu(true);

		final Integer constructId = createdConstruct.getConstruct().getId();
		ConstructLoadResponse updateResponse = new ConstructResourceImpl().update(constructId.toString(), updateConstruct, nodeList);

		// should not affect parts
		assertResponseOK(updateResponse);
		assertThat(updateResponse.getConstruct().getParts()).hasSize(2);
		assertThat(updateResponse.getConstruct().getMayBeSubtag()).isTrue();
		assertThat(updateResponse.getConstruct().getMayContainSubtags()).isTrue();
		Assertions.assertThat(updateResponse.getConstruct().getNewEditor()).isTrue();
		assertThat(updateResponse.getConstruct().getAutoEnable()).isTrue();
	}

	private List<Part> createConstructParts() throws NodeException {
		Part selectPart = new Part();
		selectPart.setTypeId(supply(() -> getPartTypeId(SingleSelectPartType.class)));

		SelectSetting setting = new SelectSetting();
		selectPart.setSelectSettings(setting);

		Part textPart = new Part();
		textPart.setTypeId(supply(() -> getPartTypeId(ShortTextPartType.class)));
		textPart.setName("input");
		textPart.setMandatory(false);

		return Arrays.asList(selectPart, textPart);
	}
	@Test
	public void testUpdateReassign() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);
		com.gentics.contentnode.rest.model.Construct construct = response.getConstruct();

		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		String newKw = construct.getKeyword();
		Map<String, String> description = construct.getDescriptionI18n();
		Map<String, String> name = construct.getNameI18n();
		Integer id = construct.getId();

		// update it
		ConstructLoadResponse updated = supply(() -> new ConstructResourceImpl().update(construct.getId().toString(), construct, Collections.singletonList(node2.getId())));
		assertResponseCodeOk(updated);
		assertEquals(updated.getConstruct().getId(), id);
		assertEquals(updated.getConstruct().getNameI18n(), name);
		assertEquals(updated.getConstruct().getKeyword(), newKw);
		assertEquals(updated.getConstruct().getDescriptionI18n(), description);

		assertThat(loadNodeConstructs(node1).getItems()).as("Node 1 constructs").usingElementComparatorOnFields("id").doesNotContain(updated.getConstruct());
		assertThat(loadNodeConstructs(node2).getItems()).as("Node 2 constructs").usingElementComparatorOnFields("id").contains(updated.getConstruct());
	}

	@Test
	public void testListCategories() throws NodeException {
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		List<ConstructCategory> categories = new ArrayList<>();
		for (int i = 1; i <= 8; i++) {
			categories.add(supply(() -> createRandomConstructCategory().getConstructCategory()));
		}

		ConstructCategoryListResponse response = supply(user,
				() -> new ConstructResourceImpl().listCategories(null, null, null, null));
		assertResponseOK(response);
		assertThat(response.getItems()).as("Object Property categories").usingElementComparatorOnFields("id").containsAll(categories);
	}

	@Test
	public void testCreateCategory() throws NodeException {
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryLoadResponse response = createRandomConstructCategory();

		// check the list contents
		ConstructCategoryListResponse opList = supply(user, () -> new ConstructResourceImpl().listCategories(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Construct categories").usingElementComparatorOnFields("id").contains(response.getConstructCategory());
	}

	@Test
	public void testDeleteCategory() throws NodeException {
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryLoadResponse response = createRandomConstructCategory();

		// deletion
		GenericResponse deleted = supply(() -> new ConstructResourceImpl().deleteCategory(response.getConstructCategory().getId().toString()));
		assertResponseCodeOk(deleted);

		// check the list contents
		ConstructCategoryListResponse opList = supply(user, () -> new ConstructResourceImpl().listCategories(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Construct categories").usingElementComparatorOnFields("id").doesNotContain(response.getConstructCategory());
	}

	@Test
	public void testLoadCategory() throws NodeException {
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryLoadResponse response = createRandomConstructCategory();

		// read it
		ConstructCategoryLoadResponse read = supply(() -> new ConstructResourceImpl().getCategory(response.getConstructCategory().getId().toString()));
		assertResponseCodeOk(read);
		assertEquals(read.getConstructCategory().getId(), response.getConstructCategory().getId());
		assertEquals(read.getConstructCategory().getName(), response.getConstructCategory().getName());
		assertEquals(read.getConstructCategory().getNameI18n(), response.getConstructCategory().getNameI18n());
	}

	@Test
	public void testUpdateCategory() throws NodeException {
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryLoadResponse response = createRandomConstructCategory();
		ConstructCategory opc = response.getConstructCategory();
		String oldEnName = opc.getNameI18n().get("en");

		opc.setName(MiscUtils.getRandomNameOfLength(12), "en");
		assertNotEquals(oldEnName, opc.getNameI18n().get("en"));

		Map<String, String> newName = opc.getNameI18n();
		Integer id = opc.getId();

		// update it
		ConstructCategoryLoadResponse updated = new ConstructResourceImpl().updateCategory(opc.getId().toString(), opc);
		assertResponseCodeOk(updated);
		assertEquals(updated.getConstructCategory().getId(), id);
		assertEquals(updated.getConstructCategory().getNameI18n(), newName);
	}

	@Test
	public void testUpdateCategorySortOrder() throws NodeException {
		operate(() -> setPermissions(
			com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY,
			Arrays.asList(group),
			new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		List<String> categoryIds = new ArrayList<>();

		for (int ii = 0; ii < 5; ii++) {
			ConstructCategoryLoadResponse response = createRandomConstructCategory();

			categoryIds.add(response.getConstructCategory().getId().toString());
		}

		Collections.reverse(categoryIds);

		IdSetRequest sortRequest = new IdSetRequest();

		sortRequest.setIds(categoryIds);

		ConstructResourceImpl resource = new ConstructResourceImpl();
		ConstructCategoryListResponse sorted = resource.sortCategories(sortRequest);

		assertResponseCodeOk(sorted);
		assertThat(sorted.getItems().stream().map(category -> category.getId().toString()).collect(Collectors.toList()))
			.as("Sorted construct categories")
			.containsExactlyElementsOf(categoryIds);

		ConstructCategoryListResponse sortedList = resource.listCategories(
			new SortParameterBean().setSort("+sortorder"),
			new FilterParameterBean(),
			new PagingParameterBean(),
			new EmbedParameterBean());

		assertThat(sortedList.getItems().stream().map(category -> category.getId().toString()).collect(Collectors.toList()))
			.as("Sorted construct categories listing")
			.containsExactlyElementsOf(categoryIds);

		Collections.reverse(categoryIds);

		ConstructCategoryListResponse reverseSortedList = resource.listCategories(
			new SortParameterBean().setSort("-sortorder"),
			new FilterParameterBean(),
			new PagingParameterBean(),
			new EmbedParameterBean());

		assertThat(reverseSortedList.getItems().stream().map(category -> category.getId().toString()).collect(Collectors.toList()))
			.as("Unsorted construct categories listing")
			.containsExactlyElementsOf(categoryIds);

	}

	@Test
	public void testLinkUnlink() throws NodeException {
		ConstructLoadResponse response = createRandomConstruct(node1);
		com.gentics.contentnode.rest.model.Construct construct = response.getConstruct();
		String oldKw = construct.getKeyword();

		construct.setKeyword(MiscUtils.getRandomNameOfLength(8));
		construct.setDescription(MiscUtils.getRandomNameOfLength(24), "en");
		assertNotEquals(oldKw, construct.getKeyword());

		BulkLinkUpdateRequest link = new BulkLinkUpdateRequest()
				.setTargetIds(Collections.singleton(construct.getId().toString()))
				.setIds(new HashSet<>(Arrays.asList(node1.getId(), node2.getId())));

		// link it
		GenericResponse updated = supply(() -> new ConstructResourceImpl().link(link));
		assertResponseCodeOk(updated);

		PagedConstructListResponse c1list = supply(user, () -> {
			try {
				return new NodeResourceImpl().getConstructs(node1.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(c1list);
		PagedConstructListResponse c2list = supply(user, () -> {
			try {
				return new NodeResourceImpl().getConstructs(node2.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(c2list);

		assertThat(c1list.getItems())
			.as("Node 1 construct IDs").usingElementComparatorOnFields("id").contains(construct);
		assertThat(c2list.getItems())
			.as("Node 2 construct IDs").usingElementComparatorOnFields("id").contains(construct);

		operate(() -> {
			NodeList nodes = new ConstructResourceImpl().listConstructNodes(construct.getId().toString());
			assertResponseCodeOk(nodes);
			assertThat(nodes.getItems()).as("Construct nodes").usingElementComparatorOnFields("id").contains(Node.TRANSFORM2REST.apply(node1), Node.TRANSFORM2REST.apply(node2));
		});

		// unlink
		updated = supply(() -> new ConstructResourceImpl().unlink(link));
		assertResponseCodeOk(updated);

		c1list = supply(user, () -> {
			try {
				return new NodeResourceImpl().getConstructs(node1.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(c1list);
		c2list = supply(user, () -> {
			try {
				return new NodeResourceImpl().getConstructs(node2.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(c2list);

		assertThat(c1list.getItems())
			.as("Node 1 construct IDs").usingElementComparatorOnFields("id").doesNotContain(construct);
		assertThat(c2list.getItems())
			.as("Node 2 construct IDs").usingElementComparatorOnFields("id").doesNotContain(construct);

		operate(() -> {
			NodeList nodes = new ConstructResourceImpl().listConstructNodes(construct.getId().toString());
			assertResponseCodeOk(nodes);
			assertThat(nodes.getItems()).as("Construct nodes").usingElementComparatorOnFields("id").doesNotContain(Node.TRANSFORM2REST.apply(node1), Node.TRANSFORM2REST.apply(node2));
		});
	}

	/**
	 * Test creating a construct with a select part
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithSelectPart() throws NodeException {
		com.gentics.contentnode.rest.model.Construct create = new com.gentics.contentnode.rest.model.Construct();
		create.setKeyword("select");
		create.setIcon("bla");
		create.setName("Select", "en");
		Part selectPart = new Part();
		selectPart.setTypeId(supply(() -> getPartTypeId(SingleSelectPartType.class)));
		SelectSetting setting = new SelectSetting();
		int datasourceId = execute(Datasource::getId, firstDs);
		String template = "template";
		setting.setDatasourceId(datasourceId);
		setting.setTemplate(template);
		selectPart.setSelectSettings(setting);
		create.setParts(Arrays.asList(selectPart));
		ConstructLoadResponse response = new ConstructResourceImpl().create(create, Arrays.asList(execute(Node::getId, node1)));
		assertResponseOK(response);

		assertThat(response.getConstruct()).as("Created construct").isNotNull();
		assertThat(response.getConstruct().getParts()).as("Part list").isNotNull().hasSize(1);
		assertThat(response.getConstruct().getParts().get(0)).as("Part")
			.hasFieldOrPropertyWithValue("type", Type.SELECT);
		assertThat(response.getConstruct().getParts().get(0).getSelectSettings()).as("Part SelectSettings")
			.isNotNull()
			.hasFieldOrPropertyWithValue("template", template)
			.hasFieldOrPropertyWithValue("datasourceId", datasourceId);
	}

	/**
	 * Test updating a construct with a select part
	 * @throws NodeException
	 */
	@Test
	public void testUpdateWithSelectPart() throws NodeException {
		int datasourceId = execute(Datasource::getId, firstDs);
		String template = "template";

		com.gentics.contentnode.rest.model.Construct create = new com.gentics.contentnode.rest.model.Construct();
		create.setKeyword("select");
		create.setIcon("bla");
		create.setName("Select", "en");
		Part selectPart = new Part();
		selectPart.setTypeId(supply(() -> getPartTypeId(SingleSelectPartType.class)));
		SelectSetting setting = new SelectSetting();
		setting.setDatasourceId(datasourceId);
		setting.setTemplate(template);
		selectPart.setSelectSettings(setting);
		create.setParts(Arrays.asList(selectPart));

		// set first option of datasource as default selection
		List<SelectOption> options = getDatasourceSelectOptions(datasourceId);
		Property defaultProperty = new Property();
		defaultProperty.setSelectedOptions(Arrays.asList(options.get(0)));
		selectPart.setDefaultProperty(defaultProperty);

		ConstructLoadResponse response = new ConstructResourceImpl().create(create, Arrays.asList(execute(Node::getId, node1)));
		assertResponseOK(response);

		com.gentics.contentnode.rest.model.Construct update = response.getConstruct();
		datasourceId = execute(Datasource::getId, secondDs);
		template = "modified template";
		update.getParts().get(0).getSelectSettings().setDatasourceId(datasourceId);
		update.getParts().get(0).getSelectSettings().setTemplate(template);

		// set first option of other datasource as default selection
		options = getDatasourceSelectOptions(datasourceId);
		defaultProperty = new Property();
		defaultProperty.setSelectedOptions(Arrays.asList(options.get(0)));
		selectPart.setDefaultProperty(defaultProperty);

		response = new ConstructResourceImpl().update(response.getConstruct().getGlobalId(), update, null);

		assertThat(response.getConstruct()).as("Updated construct").isNotNull();
		assertThat(response.getConstruct().getParts()).as("Part list").isNotNull().hasSize(1);
		assertThat(response.getConstruct().getParts().get(0)).as("Part")
			.hasFieldOrPropertyWithValue("type", Type.SELECT);
		assertThat(response.getConstruct().getParts().get(0).getSelectSettings()).as("Part SelectSettings")
			.isNotNull()
			.hasFieldOrPropertyWithValue("template", template)
			.hasFieldOrPropertyWithValue("datasourceId", datasourceId);
		assertThat(response.getConstruct().getParts().get(0).getDefaultProperty().getSelectedOptions())
				.as("Default selection").containsOnly(options.get(0));
	}

	/**
	 * Test creating a construct with an overview part
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithOverviewPart() throws NodeException {
		com.gentics.contentnode.rest.model.Construct create = new com.gentics.contentnode.rest.model.Construct();
		create.setKeyword("overview");
		create.setIcon("bla");
		create.setName("Overview", "en");
		Part overviewPart = new Part();
		overviewPart.setTypeId(supply(() -> getPartTypeId(OverviewPartType.class)));

		OverviewSetting setting = new OverviewSetting();
		setting.setHideSortOptions(true);
		setting.setListTypes(Arrays.asList(ListType.FILE, ListType.IMAGE));
		setting.setSelectTypes(Arrays.asList(SelectType.FOLDER, SelectType.AUTO));
		overviewPart.setOverviewSettings(setting);

		create.setParts(Arrays.asList(overviewPart));
		ConstructLoadResponse response = new ConstructResourceImpl().create(create, Arrays.asList(execute(Node::getId, node1)));
		assertResponseOK(response);

		assertThat(response.getConstruct()).as("Created construct").isNotNull();
		assertThat(response.getConstruct().getParts()).as("Part list").isNotNull().hasSize(1);
		assertThat(response.getConstruct().getParts().get(0)).as("Part")
			.hasFieldOrPropertyWithValue("type", Type.OVERVIEW);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings()).as("Part OverviewSettings")
			.isNotNull()
			.hasFieldOrPropertyWithValue("hideSortOptions", true)
			.hasFieldOrPropertyWithValue("stickyChannel", false);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings().getListTypes()).as("Part ListTypes")
			.isNotNull()
			.containsOnly(ListType.FILE, ListType.IMAGE);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings().getSelectTypes()).as("Part SelectTypes")
			.isNotNull()
			.containsOnly(SelectType.FOLDER, SelectType.AUTO);
	}

	/**
	 * Test updating a construct with an overview part
	 * @throws NodeException
	 */
	@Test
	public void testUpdateWithOverviewPart() throws NodeException {
		com.gentics.contentnode.rest.model.Construct create = new com.gentics.contentnode.rest.model.Construct();
		create.setKeyword("overview");
		create.setIcon("bla");
		create.setName("Overview", "en");
		Part overviewPart = new Part();
		overviewPart.setTypeId(supply(() -> getPartTypeId(OverviewPartType.class)));

		OverviewSetting setting = new OverviewSetting();
		setting.setHideSortOptions(true);
		setting.setListTypes(Arrays.asList(ListType.FILE, ListType.IMAGE));
		setting.setSelectTypes(Arrays.asList(SelectType.FOLDER, SelectType.AUTO));
		overviewPart.setOverviewSettings(setting);

		create.setParts(Arrays.asList(overviewPart));
		ConstructLoadResponse response = new ConstructResourceImpl().create(create, Arrays.asList(execute(Node::getId, node1)));
		assertResponseOK(response);

		com.gentics.contentnode.rest.model.Construct update = response.getConstruct();
		setting = update.getParts().get(0).getOverviewSettings();
		setting.setHideSortOptions(false);
		setting.setStickyChannel(true);
		setting.setListTypes(Arrays.asList(ListType.PAGE, ListType.FOLDER));
		setting.setSelectTypes(Arrays.asList(SelectType.MANUAL));

		response = new ConstructResourceImpl().update(response.getConstruct().getGlobalId(), update, null);

		assertThat(response.getConstruct()).as("Updated construct").isNotNull();
		assertThat(response.getConstruct().getParts()).as("Part list").isNotNull().hasSize(1);
		assertThat(response.getConstruct().getParts().get(0)).as("Part")
			.hasFieldOrPropertyWithValue("type", Type.OVERVIEW);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings()).as("Part OverviewSettings")
			.isNotNull()
			.hasFieldOrPropertyWithValue("hideSortOptions", false)
			.hasFieldOrPropertyWithValue("stickyChannel", true);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings().getListTypes()).as("Part ListTypes")
			.isNotNull()
			.containsOnly(ListType.PAGE, ListType.FOLDER);
		assertThat(response.getConstruct().getParts().get(0).getOverviewSettings().getSelectTypes()).as("Part SelectTypes")
			.isNotNull()
			.containsOnly(SelectType.MANUAL);
	}

	/**
	 * Test updating a select part into a multiselect part and set multiple selected options.
	 * @throws NodeException
	 */
	@Test
	public void testUpdateSelectPartType() throws NodeException {
		// create construct with single select part
		int datasourceId = execute(Datasource::getId, firstDs);
		String template = "template";

		// load the datasource over REST
		List<SelectOption> selectOptions = getDatasourceSelectOptions(datasourceId);
		SelectOption firstOption = selectOptions.get(0);
		SelectOption secondOption = selectOptions.get(1);

		com.gentics.contentnode.rest.model.Construct create = new com.gentics.contentnode.rest.model.Construct();
		create.setKeyword("select");
		create.setIcon("bla");
		create.setName("Select", "en");
		Part selectPart = new Part();
		selectPart.setTypeId(supply(() -> getPartTypeId(SingleSelectPartType.class)));
		SelectSetting setting = new SelectSetting();
		setting.setDatasourceId(datasourceId);
		setting.setTemplate(template);
		selectPart.setSelectSettings(setting);
		create.setParts(Arrays.asList(selectPart));

		// by default, select a single option
		Property defaultProperty = new Property();
		defaultProperty.setSelectedOptions(Arrays.asList(firstOption));
		selectPart.setDefaultProperty(defaultProperty);

		ConstructLoadResponse response = new ConstructResourceImpl().create(create, Arrays.asList(execute(Node::getId, node1)));
		assertResponseOK(response);

		assertThat(response.getConstruct().getParts().get(0).getDefaultProperty().getSelectedOptions()).as("Default selection").containsOnly(firstOption);

		// now update to be multiselect and have multiple options set by default
		com.gentics.contentnode.rest.model.Construct update = response.getConstruct();
		update.getParts().get(0).setTypeId(supply(() -> getPartTypeId(MultiSelectPartType.class)));
		update.getParts().get(0).getDefaultProperty().setSelectedOptions(Arrays.asList(firstOption, secondOption));

		response = new ConstructResourceImpl().update(update.getGlobalId(), update, null);
		assertResponseOK(response);

		assertThat(response.getConstruct().getParts().get(0).getType()).as("Updated type").isEqualTo(Type.MULTISELECT);
		assertThat(response.getConstruct().getParts().get(0).getDefaultProperty().getSelectedOptions()).as("Default selection").containsOnly(firstOption, secondOption);
	}

	/**
	 * Get the entries of the given datasource as list of SelectOption
	 * @param datasourceId datasource ID
	 * @return list of select options
	 * @throws NodeException
	 */
	protected List<SelectOption> getDatasourceSelectOptions(int datasourceId) throws NodeException {
		DatasourceEntryListResponse entriesResponse = new DatasourceResourceImpl().listEntries(Integer.toString(datasourceId));
		assertResponseOK(entriesResponse);
		return entriesResponse.getItems().stream()
				.map(entry -> new SelectOption().setId(entry.getDsId()).setKey(entry.getKey()).setValue(entry.getValue()))
				.collect(Collectors.toList());
	}

	protected ConstructLoadResponse createRandomConstruct(Node... nodes) throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));
		operate(() -> setPermissions(Node.TYPE_NODE, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));
		operate(() -> setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));

		for (Node node : nodes) {
			operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(group),
					new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_NODE_CONSTRUCT_MODIFY).toString()));
		}
		ConstructLoadResponse response = supply(user, () -> {
			com.gentics.contentnode.rest.model.Construct construct = new com.gentics.contentnode.rest.model.Construct()
					.setIcon("icon")
					.setName(MiscUtils.getRandomNameOfLength(10), "en")
					.setKeyword(MiscUtils.getRandomNameOfLength(8));
			return new ConstructResourceImpl().create(construct, Arrays.stream(nodes).map(Node::getId).collect(Collectors.toList()));
		});

		assertResponseCodeOk(response);
		assertThat(response.getConstruct()).as("Created construct").isNotNull();
		assertThat(response.getConstruct().getId()).as("Created construct ID").isNotNull();

		for (Node node : nodes) {
			PagedConstructListResponse nodeConstruct = loadNodeConstructs(node);
			assertThat(nodeConstruct.getItems()).as("Node constructs").usingElementComparator((a, b) -> a.getId().compareTo(b.getId())).contains(response.getConstruct());
		}

		return response;
	}

	protected PagedConstructListResponse loadNodeConstructs(Node node) throws NodeException {
		return supply(() -> {
			try {
				return new NodeResourceImpl().getConstructs(Integer.toString(node.getId()),
						new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
	}

	protected ConstructCategoryLoadResponse createRandomConstructCategory() throws NodeException {
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Construct.TYPE_CONSTRUCTS_INTEGER, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));
		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORIES, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryLoadResponse response = supply(user, () -> {
			ConstructCategory op = new ConstructCategory()
					.setName(MiscUtils.getRandomNameOfLength(10), "en");
			return new ConstructResourceImpl().createCategory(op);
		});

		assertResponseCodeOk(response);
		assertThat(response.getConstructCategory()).as("Created construct category").isNotNull();
		assertThat(response.getConstructCategory().getId()).as("Created construct category ID").isNotNull();

		operate(() -> setPermissions(com.gentics.contentnode.object.ConstructCategory.TYPE_CONSTRUCT_CATEGORY, response.getConstructCategory().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_CONSTRUCT_UPDATE).toString()));

		ConstructCategoryListResponse allCategories = supply(user,
				() -> new ConstructResourceImpl().listCategories(null, null, null, null));
		assertResponseOK(allCategories);
		assertThat(allCategories.getItems()).as("Construct categories").usingElementComparator((a, b) -> a.getId().compareTo(b.getId())).contains(response.getConstructCategory());

		return response;
	}

	/**
	 * Get the construct with given keyword from the response. This method asserts that the construct is found
	 * @param response response
	 * @param keyword construct keyword
	 * @return construct
	 */
	protected com.gentics.contentnode.rest.model.Construct getConstruct(PagedConstructListResponse response, String keyword) {
		Optional<com.gentics.contentnode.rest.model.Construct> optionalConstruct = response.getItems().stream().filter(c -> StringUtils.equals(c.getKeyword(), keyword)).findFirst();
		assertThat(optionalConstruct).as("Construct with keyword " + keyword).isNotEmpty();
		return optionalConstruct.get();
	}

	/**
	 * Get the permissions for the construct contained in the response. This method asserts that the construct and its permissions are both contained in the response
	 * @param response response
	 * @param keyword construct keyword
	 * @return permissions
	 */
	protected Set<Permission> getConstructPermissions(PagedConstructListResponse response, String keyword) {
		com.gentics.contentnode.rest.model.Construct construct = getConstruct(response, keyword);
		assertThat(response.getPerms()).as("Permissions in response").isNotNull().containsKey(construct.getId());
		return response.getPerms().get(construct.getId());
	}
}
