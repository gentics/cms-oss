package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.TemplateInNode;
import com.gentics.contentnode.rest.model.request.LinkRequest;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.TemplateCopyRequest;
import com.gentics.contentnode.rest.model.request.TemplateCreateRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PagedFolderListResponse;
import com.gentics.contentnode.rest.model.response.PagedTemplateListResponse;
import com.gentics.contentnode.rest.model.response.TagList;
import com.gentics.contentnode.rest.model.response.TemplateInNodeResponse;
import com.gentics.contentnode.rest.model.response.TemplateListResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.MCTemplateResourceImpl;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test cases for usage of the {@link TemplateResource}
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class TemplateResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Folder folder;
	private static Node otherNode;
	private static UserGroup testGroup;
	private static SystemUser testUser;
	private static Integer constructId;
	private static ObjectTagDefinition objectProperty;
	private static final String TEMPLATE_DEFINITION_NAME = "templateName";

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		otherNode = supply(() -> createNode());

		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "short", "text"));

		testGroup = supply(() -> createUserGroup("Testgroup", 2));
		testUser = supply(() -> createSystemUser("Test", "Test", null, "testuser", "testuser", Arrays.asList(testGroup)));

		objectProperty = supply(() -> createObjectPropertyDefinition(Template.TYPE_TEMPLATE, constructId, "Template Name", TEMPLATE_DEFINITION_NAME));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			for (Page page : node.getFolder().getPages()) {
				page.delete(true);
			}
		});

		operate(t -> {
			for (Template template : t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS), false, false)) {
				template.delete();
			}
		});

		setNodePermissions();
		setFolderPermissions();
		restrictGroupAssignment();
	}

	@Test
	@Expected(ex = RestMappedException.class)
	public void testCreateNoRequest() throws NodeException {
		operate(() -> new TemplateResourceImpl().create(null));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'template' darf nicht leer sein.")
	public void testCreateNoTemplate() throws NodeException {
		operate(() -> new TemplateResourceImpl().create(new TemplateCreateRequest()));
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Das Feld 'template.folderId' darf nicht leer sein.")
	public void testCreateNoFolder() throws NodeException {
		operate(() -> new TemplateResourceImpl().create(new TemplateCreateRequest().setTemplate(new com.gentics.contentnode.rest.model.Template())));
	}

	@Test
	public void testCreate() throws NodeException {
		TemplateLoadResponse response = supply(() -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			model.setFolderId(folder.getId());

			return new TemplateResourceImpl().create(new TemplateCreateRequest().setTemplate(model));
		});

		assertResponseCodeOk(response);
		assertThat(response.getTemplate()).as("Created template").isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("name",
				response.getTemplate().getId().toString());

		// template must be assigned to folder
		TemplateListResponse templatesInFolder = supply(() -> getFolderResource().getTemplates(
				folder.getId().toString(),
				new InFolderParameterBean(),
				new TemplateListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean()));
		assertResponseCodeOk(templatesInFolder);
		assertThat(templatesInFolder.getTemplates()).as("Templates in folder").usingElementComparatorOnFields("id").containsOnly(response.getTemplate());

		// template must be assigned to node
		PagedTemplateListResponse templatesInNode = supply(() -> getNodeResource().getTemplates(node.getId().toString(), null, null, null, null));
		assertResponseCodeOk(templatesInNode);
		assertThat(templatesInNode.getItems()).as("Templates in node").usingElementComparatorOnFields("id").containsOnly(response.getTemplate());
	}

	@Test
	public void testCreateInChannel() throws NodeException {
		TemplateLoadResponse response = supply(() -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			model.setFolderId(folder.getId());

			return new TemplateResourceImpl().create(new TemplateCreateRequest().setTemplate(model).setNodeId(channel.getId()));
		});

		assertResponseCodeOk(response);

		PagedTemplateListResponse templatesInNode = supply(() -> getNodeResource().getTemplates(node.getId().toString(), null, null, null, null));
		assertResponseCodeOk(templatesInNode);
		assertThat(templatesInNode.getItems()).as("Templates in Master").isEmpty();

		PagedTemplateListResponse templatesInChannel = supply(() -> getNodeResource().getTemplates(channel.getId().toString(), null, null, null, null));
		assertResponseCodeOk(templatesInChannel);
		assertThat(templatesInChannel.getItems()).as("Templates in Channel").usingElementComparatorOnFields("id").containsOnly(response.getTemplate());
	}

	@Test
	public void testCreateInMultipleFolders() throws NodeException {
		TemplateLoadResponse createResponse = supply(() -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();

			return new TemplateResourceImpl().create(new TemplateCreateRequest().setTemplate(model)
					.setFolderIds(new HashSet<>(Arrays.asList(Integer.toString(node.getFolder().getId()), Integer.toString(folder.getId())))));
		});

		int templateId = createResponse.getTemplate().getId();

		PagedFolderListResponse folderListResponse = supply(() -> {
			return new TemplateResourceImpl().folders(Integer.toString(templateId), null, null, null);
		});

		com.gentics.contentnode.rest.model.Folder folder1 = supply(() -> ModelBuilder.getFolder(node.getFolder()));
		com.gentics.contentnode.rest.model.Folder folder2 = supply(() -> ModelBuilder.getFolder(folder));
		assertThat(folderListResponse.getItems()).as("Linked folders").usingElementComparatorIgnoringFields("breadcrumbs", "creator", "editor")
				.containsOnly(folder1, folder2);
	}

	@Test
	public void testCreatePermission() throws NodeException {
		testPermissions("Sie haben zu wenig Berechtigungen auf den angegebenen Ordner.", () -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			model.setFolderId(folder.getId());

			assertResponseCodeOk(new TemplateResourceImpl().create(new TemplateCreateRequest().setTemplate(model)));
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_CREATE);
	}

	@Test
	public void testLoad() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));

		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId()), () -> {
			TemplateLoadResponse response = new TemplateResourceImpl().get(template.getId().toString(), null, false, false);
			assertResponseCodeOk(response);

			assertThat(response.getTemplate()).as("Loaded template").isNotNull().isEqualToComparingFieldByFieldRecursively(
					ModelBuilder.getTemplate(template, Arrays.asList(Reference.TEMPLATE_TAGS, Reference.TEMPLATE_SOURCE, Reference.OBJECT_TAGS)));
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW);
	}

	@Test
	public void givenTemplateTagWithReferencedConstructRequest_shouldHaveEmbeddedConstructInResponse() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));

		consume(Template::unlock, template);
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		String tagName = "content";
		consume(testUser, tmpl-> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			com.gentics.contentnode.rest.model.TemplateTag tagModel = new com.gentics.contentnode.rest.model.TemplateTag();
			tagModel.setActive(true);
			tagModel.setConstructId(constructId);
			tagModel.setEditableInPage(true);
			tagModel.setName(tagName);

			Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateTags = new HashMap<>();
			templateTags.put(tagName, tagModel);
			model.setTemplateTags(templateTags);

			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setTemplate(model);
			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);


		TagList tagList = supply(() -> new TemplateResourceImpl().listTags(
				template.getId().toString(),
				null, null, null,
				new EmbedParameterBean().withEmbed("construct"))) ;

		Tag retrievedTag = tagList.getItems().stream()
				.filter(templateTag -> tagName.equals(templateTag.getName()))
				.findAny().get();

		assertThat(retrievedTag).isNotNull();
		assertThat(retrievedTag).hasFieldOrPropertyWithValue("constructId", constructId);
		assertThat(retrievedTag).hasFieldOrPropertyWithValue("name", tagName);
	}

	@Test
	@Expected(ex=EntityNotFoundException.class, message = "Die Vorlage mit der ID 4711 konnte nicht gefunden werden.")
	public void testLoadNotFound() throws NodeException {
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW);

		operate(testUser, () -> new TemplateResourceImpl().get("4711", null, false, false));
	}

	@Test
	public void testLoadForUpdate() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId()), () -> {
			TemplateLoadResponse response = supply(testUser, () -> {
				return new TemplateResourceImpl().get(template.getId().toString(), null, true, false);
			});

			assertThat(response.getTemplate().isLocked()).as("Template locked").isTrue();
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);
	}

	@Test
	public void testLoadForUpdateLocked() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		operate(t -> t.getObject(template, true));

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		exceptionChecker.expect(ReadOnlyException.class);

		operate(testUser, () -> {
			new TemplateResourceImpl().get(template.getId().toString(), null, true, false);
		});
	}

	@Test
	public void testUnlock() throws NodeException {
		// create template and assert that it is locked
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(t -> assertThat(t).as("Template").isLocked(), template);

		// unlock the template
		consume(t -> {
			assertResponseOK(new TemplateResourceImpl().unlock(t.getId().toString()));
		}, template);

		// reload and assert that it is unlocked
		template = execute(Template::reload, template);
		consume(t -> assertThat(t).as("Template").isNotLocked(), template);
	}

	@Test
	public void testUnlockNotLocked() throws NodeException {
		// create template and unlock it
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);
		template = execute(Template::reload, template);
		consume(t -> assertThat(t).as("Template").isNotLocked(), template);

		// unlock the template
		consume(t -> {
			assertResponseOK(new TemplateResourceImpl().unlock(t.getId().toString()));
		}, template);

		// reload and assert that it is still unlocked
		template = execute(Template::reload, template);
		consume(t -> assertThat(t).as("Template").isNotLocked(), template);
	}

	@Test
	public void testUnlockLockedByOtherUser() throws NodeException {
		// create template and assert that it is locked
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(t -> assertThat(t).as("Template").isLocked(), template);

		// allow the testuser to see the template
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW);

		// try unlocking with another user
		consume(testUser, t -> {
			assertResponseOK(new TemplateResourceImpl().unlock(t.getId().toString()));
		}, template);

		// reload and assert that it is still locked
		template = execute(Template::reload, template);
		consume(t -> assertThat(t).as("Template").isLocked(), template);
	}

	@Test
	public void testUnlockPermissions() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		String globalTemplateId = execute(Template::getGlobalId, template).toString();
		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %s.", globalTemplateId), () -> {
			new TemplateResourceImpl().unlock(globalTemplateId);
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW);
	}

	@Test
	public void testUpdate() throws NodeException {
		String updatedName = "Updated name";
		String updatedSource = "Updated source";
		String updatedDescription = "Updated description";
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		String templateId = Integer.toString(template.getId());
		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId()), () -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			model.setName(updatedName);
			model.setSource(updatedSource);
			model.setDescription(updatedDescription);
			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setTemplate(model);
			assertResponseCodeOk(new TemplateResourceImpl().update(templateId, request));
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		template = execute(Template::reload, template);
		assertThat(template).hasFieldOrPropertyWithValue("name", updatedName).hasFieldOrPropertyWithValue("source", updatedSource)
				.hasFieldOrPropertyWithValue("description", updatedDescription);
	}

	@Test
	public void testAddTag() throws NodeException {
		String tagName = "content";

		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		consume(testUser, tmpl-> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			com.gentics.contentnode.rest.model.TemplateTag tagModel = new com.gentics.contentnode.rest.model.TemplateTag();
			tagModel.setActive(true);
			tagModel.setConstructId(constructId);
			tagModel.setEditableInPage(true);
			tagModel.setName(tagName);

			Map<String, Property> properties = new HashMap<>();
			Property property = new Property();
			property.setType(Type.STRING);
			property.setStringValue("Tag content");
			properties.put("text", property);
			tagModel.setProperties(properties);

			Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateTags = new HashMap<>();
			templateTags.put(tagName, tagModel);
			model.setTemplateTags(templateTags);

			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setTemplate(model);
			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);

		template = execute(Template::reload, template);
		consume(tmpl -> {
			assertThat(tmpl.getTemplateTag(tagName)).isNotNull()
				.hasFieldOrPropertyWithValue("constructId", constructId)
				.hasFieldOrPropertyWithValue("enabled", true)
				.hasFieldOrPropertyWithValue("name", tagName);
			assertThat(tmpl.getTemplateTag(tagName).getValues().getByKeyname("text")).isNotNull().hasFieldOrPropertyWithValue("valueText", "Tag content");
		}, template);
	}

	@Test
	public void testUpdateTag() throws NodeException {
		String tagName = "content";

		Template template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setSource("");
			tmpl.setName("Template");
			tmpl.addFolder(folder);

			tmpl.getTemplateTags().put(tagName, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(constructId);
				tTag.setEnabled(true);
				tTag.setName(tagName);
				tTag.setPublic(true);
				tTag.getValues().getByKeyname("text").setValueText("Before update");
			}, false));
		}));
		consume(Template::unlock, template);

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		consume(testUser, tmpl -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			com.gentics.contentnode.rest.model.TemplateTag tagModel = new com.gentics.contentnode.rest.model.TemplateTag();
			tagModel.setActive(true);
			tagModel.setConstructId(constructId);
			tagModel.setEditableInPage(true);
			tagModel.setName(tagName);

			Map<String, Property> properties = new HashMap<>();
			Property property = new Property();
			property.setType(Type.STRING);
			property.setStringValue("After update");
			properties.put("text", property);
			tagModel.setProperties(properties);

			Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateTags = new HashMap<>();
			templateTags.put(tagName, tagModel);
			model.setTemplateTags(templateTags);

			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setTemplate(model);
			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);

		template = execute(Template::reload, template);
		consume(tmpl -> {
			assertThat(tmpl.getTemplateTag(tagName)).isNotNull()
				.hasFieldOrPropertyWithValue("constructId", constructId)
				.hasFieldOrPropertyWithValue("enabled", true)
				.hasFieldOrPropertyWithValue("name", tagName)
				.hasFieldOrPropertyWithValue("isPublic", true)
				.hasFieldOrPropertyWithValue("mandatory", false);
			assertThat(tmpl.getTemplateTag(tagName).getValues().getByKeyname("text")).isNotNull().hasFieldOrPropertyWithValue("valueText", "After update");
		}, template);

		consume(testUser, tmpl -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			com.gentics.contentnode.rest.model.TemplateTag tagModel = new com.gentics.contentnode.rest.model.TemplateTag();
			tagModel.setConstructId(constructId);
			tagModel.setName(tagName);
			tagModel.setEditableInPage(false);
			tagModel.setMandatory(true);

			Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateTags = new HashMap<>();

			templateTags.put(tagName, tagModel);
			model.setTemplateTags(templateTags);

			TemplateSaveRequest request = new TemplateSaveRequest();

			request.setTemplate(model);

			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);

		TagList tags = execute(
			tmpl -> new TemplateResourceImpl().listTags(tmpl.getId().toString(), new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(), new EmbedParameterBean()),
			template);

		assertThat(tags.getItems())
			.as("Loaded template tags")
			.hasSize(1);
		assertThat(tags.getItems().get(0))
			.as("Template tag")
			.hasFieldOrPropertyWithValue("editableInPage", false)
			.hasFieldOrPropertyWithValue("mandatory", true);
	}

	@Test
	public void testRemoveTag() throws NodeException {
		String tagName = "content";

		Template template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setSource("");
			tmpl.setName("Template");
			tmpl.addFolder(folder);

			tmpl.getTemplateTags().put(tagName, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(constructId);
				tTag.setEnabled(true);
				tTag.setName(tagName);
				tTag.setPublic(true);
			}, false));
		}));
		consume(Template::unlock, template);

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		consume(testUser, tmpl -> {
			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setDelete(Arrays.asList(tagName));
			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);

		template = execute(Template::reload, template);

		consume(tmpl -> {
			assertThat(tmpl.getTemplateTag(tagName)).as("Deleted tag").isNull();
		}, template);
	}

	@Test
	public void testDelete() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		String templateId = Integer.toString(template.getId());
		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId()), () -> {
			Response response = new TemplateResourceImpl().delete(templateId);
			assertThat(response).as("Delete response").isNotNull().hasFieldOrPropertyWithValue("status", 204);
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_DELETE);

		template = execute(Template::reload, template);
		assertThat(template).as("Deleted template").isNull();
	}

	@Test
	@Expected(ex = EntityInUseException.class, message = "Das Template \"Template\" kann nicht vom letzten Ordner entknüpft oder gelöscht werden da es noch Seiten gibt welche dieses Template verwenden.")
	public void testDeletedUsed() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		operate(() -> createPage(node.getFolder(), template, "Page"));

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_DELETE);

		consume(testUser, tmpl -> new TemplateResourceImpl().delete(Integer.toString(tmpl.getId())), template);
	}

	@Test
	public void testLocalize() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		setNodePermissions(PermHandler.PERM_VIEW);

		testPermissions(String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId()), () -> {
			LocalizeRequest request = new LocalizeRequest();
			request.setChannelId(channel.getId());
			request.setForegroundTime(0);
			GenericResponse response = new MCTemplateResourceImpl().localize(Integer.toString(template.getId()), request);
			assertResponseCodeOk(response);
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		Template localized = execute(tmpl -> {
			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				return tmpl.reload();
			}
		}, template);

		assertThat(localized).as("Localized Template").isNotEqualTo(template);
	}

	@Test
	public void testLocalizeNoChannelPermission() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);

		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);
		setNodePermissions(PermHandler.PERM_VIEW);
		restrictGroupAssignment(node);

		exceptionChecker.expect(InsufficientPrivilegesException.class, String.format("Sie haben zu wenig Berechtigungen auf den Node %d.", channel.getId()));

		consume(testUser, tmpl -> {
			LocalizeRequest request = new LocalizeRequest();
			request.setChannelId(channel.getId());
			request.setForegroundTime(0);
			new MCTemplateResourceImpl().localize(Integer.toString(template.getId()), request);
		}, template);
	}

	@Test
	public void testCopy() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));

		testPermissions(bit -> {
			if (bit == PermHandler.PERM_TEMPLATE_CREATE) {
				return "Sie haben zu wenig Berechtigungen auf den angegebenen Ordner.";
			} else {
				return String.format("Keine Berechtigung für die Vorlage mit der ID %d.", template.getId());
			}
		}, () -> {
			TemplateLoadResponse response = new TemplateResourceImpl().copy(Integer.toString(template.getId()),
					new TemplateCopyRequest().setFolderId(folder.getId()));
			assertResponseCodeOk(response);
			assertThat(response.getTemplate()).as("Copy").isNotNull().hasFieldOrPropertyWithValue("name", "Kopie von Template");
			assertThat(response.getTemplate().getId()).as("Id of Copy").isNotEqualTo(template.getId());
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_CREATE);
	}

	@Test
	public void testCopyChannel() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));

		setNodePermissions(PermHandler.PERM_VIEW);
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE, PermHandler.PERM_TEMPLATE_CREATE);

		int copyId = supply(testUser, () -> {
			TemplateLoadResponse response = new TemplateResourceImpl().copy(Integer.toString(template.getId()),
					new TemplateCopyRequest().setFolderId(folder.getId()).setNodeId(channel.getId()));
			assertResponseCodeOk(response);
			assertThat(response.getTemplate()).as("Copy").isNotNull().hasFieldOrPropertyWithValue("name", "Kopie von Template");
			assertThat(response.getTemplate().getId()).as("Id of Copy").isNotEqualTo(template.getId());
			return response.getTemplate().getId();
		});

		consume(id -> {
			Template copy = TransactionManager.getCurrentTransaction().getObject(Template.class, id);
			assertThat(copy.getChannel()).as("Template copy").isEqualTo(channel);
		}, copyId);
	}

	@Test
	public void testTemplateList() throws NodeException {
		doTestTemplateList(Arrays.asList(node.getId(), otherNode.getId()), false, Arrays.asList(node.getId(), otherNode.getId()));
	}

	@Test
	public void testTemplateListWithoutNodeIds() throws NodeException {
		doTestTemplateList(Collections.emptyList(), false, Arrays.asList(node.getId(), otherNode.getId(), channel.getId()));
	}

	@Test
	public void testTemplateListWithoutNodeIdsAndReduce() throws NodeException {
		doTestTemplateList(Collections.emptyList(), true, Arrays.asList(node.getId(), otherNode.getId(), channel.getId()));
	}

	private void doTestTemplateList(List<Integer> nodeIds, boolean reduce, List<Integer> expectedNodeIds) throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));

		update(otherNode, n -> n.addTemplate(template));

		setNodePermissions(PermHandler.PERM_VIEW);
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW);
		TemplateInNodeResponse response = supply(testUser, () -> {
			return new TemplateResourceImpl().list(nodeIds.stream().map(Object::toString).collect(Collectors.toList()),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PermsParameterBean().setPerms(true),
				new TemplateListParameterBean().setReduce(reduce));
		});

		// prepare sorted list of node names
		List<String> sortedNodeNames = supply(() -> Arrays.asList(node.getFolder().getName(), otherNode.getFolder().getName()));
		Collections.sort(sortedNodeNames);

		List<TemplateInNode> expected = new ArrayList<>();

		for (Integer nodeId : expectedNodeIds) {
			expected.add(supply(() -> templateInNode(template.getId(), nodeId, sortedNodeNames)));
		}

		// sort assigned node names in response
		response.getItems().stream().forEach(t -> Collections.sort(t.getAssignedNodes()));

		assertThat(response.getPerms().get(template.getId()))
			.as("Test user permissions")
			.containsExactly(Permission.view);

		if (reduce) {
			assertThat(response.getItems())
				.as("Templates per node")
				.usingElementComparatorOnFields("id", "nodeId", "assignedNodes")
				.hasSize(1)
				.isSubsetOf(expected);
		} else {
			assertThat(response.getItems())
				.as("Templates per node")
				.usingElementComparatorOnFields("id", "nodeId", "assignedNodes")
				.containsOnlyElementsOf(expected);
		}

		// Check again with system user if all permissions are set.
		response = supply(() -> {
			return new TemplateResourceImpl().list(nodeIds.stream().map(Object::toString).collect(Collectors.toList()),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PermsParameterBean().setPerms(true),
				new TemplateListParameterBean().setReduce(reduce));
		});

		assertThat(response.getPerms().get(template.getId()))
			.as("System user permissions")
			.containsExactlyInAnyOrder(Permission.view, Permission.edit, Permission.delete);
	}

	@Test
	public void testTemplateListNoNodePermission() throws NodeException {
		setNodePermissions(node, PermHandler.PERM_VIEW);

		exceptionChecker.expect(InsufficientPrivilegesException.class, String.format("Sie haben zu wenig Berechtigungen auf den Node %d.", otherNode.getId()));

		supply(testUser, () -> {
			return new TemplateResourceImpl().list(Arrays.asList(Integer.toString(node.getId()), Integer.toString(otherNode.getId())),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PermsParameterBean(),
				new TemplateListParameterBean());
		});
	}

	@Test
	public void testLink() throws NodeException {
		Template template = supply(() -> createTemplate(otherNode.getFolder(), "Template"));
		consume(Template::unlock, template);

		setNodePermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		testPermissions("", () -> {
			LinkRequest request = new LinkRequest();
			request.setFolderIds(new HashSet<>(Arrays.asList(Integer.toString(folder.getId()))));
			new TemplateResourceImpl().link(Integer.toString(template.getId()), request);

			Template reloaded = template.reload();
			assertThat(reloaded.getFolders()).as("Linked Folders").contains(folder);
			assertThat(reloaded.getAssignedNodes()).as("Linked Nodes").contains(node);
		}, PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_LINK);
	}

	@Test
	public void givenTemplateTagWithoutChanges_ShouldHaveSameUidAndShouldNotCreateNewTags() throws NodeException {
		Template template = supply(() -> createTemplate(folder, "Template"));
		consume(Template::unlock, template);
		setFolderPermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		ObjectTag objectTag = supply(()-> template.getObjectTag(TEMPLATE_DEFINITION_NAME));
		String uid = objectTag.getGlobalId().toString();

		consume(testUser, tmpl -> {
			com.gentics.contentnode.rest.model.Template model = new com.gentics.contentnode.rest.model.Template();
			model.setDescription("changed description");

			com.gentics.contentnode.rest.model.Tag restTag = ModelBuilder.getTag(objectTag, false);
			Map<String, com.gentics.contentnode.rest.model.Tag> restTags = new HashMap<>();
			restTags.put(objectTag.getName(), restTag);
			model.setObjectTags(restTags);

			TemplateSaveRequest request = new TemplateSaveRequest();
			request.setTemplate(model);
			assertResponseCodeOk(new TemplateResourceImpl().update(tmpl.getId().toString(), request));
		}, template);


		Map<String, ObjectTag> retrievedTemplateObjectTags = template.getObjectTags();
		ObjectTag objectTagRetrieved = retrievedTemplateObjectTags.get(TEMPLATE_DEFINITION_NAME);
		String retrievedUid = objectTagRetrieved.getGlobalId().toString();

		assertThat(retrievedUid).isEqualTo(uid);

		// assert no new object tags are created
		List<Integer> objTagIds =  supply(()-> DBUtils.select("SELECT t.id as id FROM objtag t"
				+ " LEFT JOIN construct c ON c.id = t.construct_id"
				+ " WHERE t.obj_id = ? AND t.obj_type = ? AND c.id IS NOT NULL", ps -> {
			ps.setInt(1, template.getId());
			ps.setInt(2, Template.TYPE_TEMPLATE);
		}, DBUtils.IDLIST));

		assertThat(objTagIds.size()).isEqualTo(1);
	}


	@Test
	public void testUpdateObjectProperty() throws NodeException {
		Template template = supply(() -> createTemplate(otherNode.getFolder(), "Template"));
		consume(Template::unlock, template);
		String globalTemplateId = execute(Template::getGlobalId, template).toString();

		setNodePermissions(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_UPDATE);

		TemplateLoadResponse loadResponse = supply(testUser, () -> new TemplateResourceImpl().get(globalTemplateId, null, true, true));
		assertResponseOK(loadResponse);

		assertThat(loadResponse.getTemplate()).as("Template").hasFieldOrPropertyWithValue("locked", true);
		assertThat(loadResponse.getTemplate().getObjectTags()).as("Template object tags").isNotEmpty();

		GenericResponse saveResponse = execute(testUser, tmpl -> {
			// modify the object tag
			Optional.ofNullable(tmpl.getObjectTags().get("object.templateName")).ifPresent(tag -> {
				tag.setActive(true);
				Optional.ofNullable(tag.getProperties().get("text")).ifPresent(property -> property.setStringValue("This is the template name"));
			});
			TemplateSaveRequest saveRequest = new TemplateSaveRequest(tmpl);
			saveRequest.setUnlock(true);
			return new TemplateResourceImpl().update(globalTemplateId, saveRequest);
		}, loadResponse.getTemplate());
		assertResponseOK(saveResponse);

		// assert modified object tag
		loadResponse = supply(testUser, () -> new TemplateResourceImpl().get(globalTemplateId, null, false, false));
		assertThat(loadResponse.getTemplate()).as("Template").hasFieldOrPropertyWithValue("locked", false);
		assertThat(loadResponse.getTemplate().getObjectTags()).as("Object tags").containsKey("object.templateName");
		Tag tag = loadResponse.getTemplate().getObjectTags().get("object.templateName");
		assertThat(tag.getProperties()).as("Object tag").containsKey("text");
		assertThat(tag.getProperties().get("text").getStringValue()).as("Object tag value").isEqualTo("This is the template name");
	}

	/**
	 * Set node permissions on all nodes
	 * @param permBits permission bits
	 * @throws NodeException
	 */
	protected void setNodePermissions(int... permBits) throws NodeException {
		for (Node n : Arrays.asList(node, otherNode)) {
			setNodePermissions(n, permBits);
		}
	}

	/**
	 * Set node permissions on the given node
	 * @param n node
	 * @param permBits permission bits
	 * @throws NodeException
	 */
	protected void setNodePermissions(Node n, int... permBits) throws NodeException {
		operate(() -> setPermissions(Node.TYPE_NODE, n.getFolder().getId(), Arrays.asList(testGroup), new PermHandler.Permission(permBits).toString()));
	}

	/**
	 * Set folder permissions on the test folder
	 * @param permBits permission bits
	 * @throws NodeException
	 */
	protected void setFolderPermissions(int... permBits) throws NodeException {
		operate(() -> setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(testGroup), new PermHandler.Permission(permBits).toString()));
	}

	/**
	 * Restrict group assignment to channels
	 * @param channels restricted channels
	 * @throws NodeException
	 */
	protected void restrictGroupAssignment(Node... channels) throws NodeException {
		testUser = update(testUser, upd -> {
			Map<Integer, Set<Integer>> restrictionsMap = upd.getGroupNodeRestrictions();
			restrictionsMap.clear();
			for (Node channel : channels) {
				restrictionsMap.computeIfAbsent(testGroup.getId(), key -> new HashSet<>()).add(channel.getId());
			}
		});
	}

	/**
	 * Run a test with different permission settings. Expect, that an {@link InsufficientPrivilegesException} is thrown,
	 * if not all of the folder permission bits are set
	 * @param expectedMessage expected message of the exception
	 * @param test test to run
	 * @param folderPermBits required folder permission bits
	 * @throws NodeException
	 */
	protected void testPermissions(String expectedMessage, Operator test, int... folderPermBits) throws NodeException {
		testPermissions(bit -> expectedMessage, test, folderPermBits);
	}

	/**
	 * Run a test with different permission settings. Expect, that an {@link InsufficientPrivilegesException} is thrown,
	 * if not all of the folder permission bits are set
	 * @param messageProvider provider for the expected message of the exception (based on the missing permission bit)
	 * @param test test to run
	 * @param folderPermBits required folder permission bits
	 * @throws NodeException
	 */
	protected void testPermissions(Function<Integer, String> messageProvider, Operator test, int... folderPermBits)
			throws NodeException {
		// test with less permissions (all combinations)
		for (int bit : folderPermBits) {
			setFolderPermissions(Arrays.stream(folderPermBits).filter(b -> b != bit).toArray());
			try (Trx trx = new Trx(testUser)) {
				test.operate();
				fail(String.format("Expected exception %s was not thrown without bit %d", InsufficientPrivilegesException.class.getName(), bit));
			} catch (NodeException e) {
				if (!InsufficientPrivilegesException.class.equals(e.getClass())) {
					throw e;
				} else {
					String expectedMessage = null;
					if (messageProvider != null) {
						expectedMessage = messageProvider.apply(bit);
					}
					if (!StringUtils.isEmpty(expectedMessage)) {
						assertEquals("Exception message", expectedMessage, e.getLocalizedMessage());
					}
				}
			}
		}

		// test with full permissions
		setFolderPermissions(folderPermBits);

		operate(testUser, test);
	}

	/**
	 * Get reference template in node
	 * @param templateId template ID
	 * @param nodeId node ID
	 * @param nodeNames node names
	 * @return reference object
	 * @throws NodeException
	 */
	protected TemplateInNode templateInNode(int templateId, int nodeId, List<String> nodeNames) throws NodeException {
		TemplateInNode templateInNode = new TemplateInNode();
		templateInNode.setId(templateId);
		templateInNode.setNodeId(nodeId);
		templateInNode.setAssignedNodes(nodeNames);
		return templateInNode;
	}
}
