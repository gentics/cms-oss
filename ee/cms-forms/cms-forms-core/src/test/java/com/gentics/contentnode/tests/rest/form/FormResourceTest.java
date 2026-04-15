package com.gentics.contentnode.tests.rest.form;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.FormAssertions.assertThat;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.perm;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.waitForNextSecond;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.client.ObjectMapperProvider;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.model.form.FormListResponse;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ItemVersionListResponse;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.RestExceptionChecker;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;

/**
 * Test cases for forms
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.WASTEBIN })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormResourceTest {
	public final static String EXTERNAL_FORM_ID = "this-is-an-external-id";

	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static Node node;

	private static Folder nodeRootFolder;

	private static Node channel;

	private static Folder testFolder;

	private static Folder localizedFolder;

	private static Folder localFolder;

	private static Node noLangNode;

	private static Folder noLangTestFolder;

	private static Node otherNode;

	private static Folder otherNodeRootFolder;

	private static Folder otherNodeTestFolder;

	private static UserGroup testGroup;

	private static SystemUser testUser;

	@Parameters(name = "{index}: feature {0}, feature for node {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(false, true)) {
			for (boolean nodeFeature : Arrays.asList(false, true)) {
				if (!feature && nodeFeature) {
					continue;
				}
				data.add(new Object[] { feature, nodeFeature });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode("Master", "Master", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		nodeRootFolder = execute(Node::getFolder, node);
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		testFolder = supply(() -> createFolder(node.getFolder(), "Folder"));
		localizedFolder = supply(() -> localize(testFolder, channel));

		localFolder = supply(() -> createFolder(node.getFolder(), "Local", channel));

		noLangNode = supply(() -> createNode("NoLanguage", "NoLanguage", PublishTarget.NONE));
		noLangTestFolder = supply(() -> createFolder(noLangNode.getFolder(), "Folder"));

		otherNode = supply(() -> createNode("Other", "Other", PublishTarget.NONE, getLanguage("de")));
		otherNodeRootFolder = execute(Node::getFolder, otherNode);
		consume(n -> FormFactory.addFormTypeToNode(FormTypeConfigurationModifier.GENERIC_TYPE, n.getId()), otherNode);
		otherNodeTestFolder = supply(() -> createFolder(otherNode.getFolder(), "Folder"));

		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("formtester", "formtester", null, "formtester", "formtester",
				Arrays.asList(testGroup)));
	}

	@Before
	public void setup() throws NodeException {
		NodeConfigRuntimeConfiguration.getPreferences().setFeature(Feature.FORMS, true);
		operate(() -> {
			if (nodeFeature) {
				node.activateFeature(Feature.FORMS);
				noLangNode.activateFeature(Feature.FORMS);
				otherNode.activateFeature(Feature.FORMS);
			} else {
				node.deactivateFeature(Feature.FORMS);
				noLangNode.deactivateFeature(Feature.FORMS);
				otherNode.deactivateFeature(Feature.FORMS);
			}
		});
		NodeConfigRuntimeConfiguration.getPreferences().setFeature(Feature.FORMS, feature);
		consume(n -> FormFactory.addFormTypeToNode(FormTypeConfigurationModifier.GENERIC_TYPE, n.getId()), node);

		node = Builder.update(node, n -> {
			n.setDefaultFormFolder(null);
		}).build();

		if (!feature || !nodeFeature) {
			exceptionChecker.expect(NotAllowedException.class, "Das erforderliche Feature forms ist nicht aktiviert.");
		}
	}

	@After
	public void tearDown() throws NodeException {
		testFolder = execute(Folder::reload, testFolder);
		operate(() -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				clear(testFolder);
				clear(noLangTestFolder);
				clear(otherNodeTestFolder);
			}
		});
	}

	@Rule
	public RestExceptionChecker exceptionChecker = new RestExceptionChecker();

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public boolean nodeFeature;

	@Test
	public void testList() throws NodeException, RestException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));

		// delete all existing permissions for the group
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { testGroup.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(testGroup.getId()));
		// set the permissions (only to view the folder, but not to view forms in the folder)
		operate(() -> setPermissions(Folder.TYPE_FOLDER, testFolder.getId(), Arrays.asList(testGroup), new Permission(PermType.read.getBit()).toString()));

		FormListResponse response = null;
		try (LoggedInClient client = restContext.client("formtester", "formtester")) {
			response = client.get().base().path("form").queryParam("folderId", testFolder.getId()).queryParam("recursive", true).request().get(FormListResponse.class);
		}
		assertResponseOK(response);
		assertThat(response.getItems()).as("Forms list").isEmpty();

		// set the permissions (view folder and view forms)
		operate(() -> setPermissions(Folder.TYPE_FOLDER, testFolder.getId(), Arrays.asList(testGroup), new Permission(PermType.read.getBit(), PermType.viewform.getBit()).toString()));

		try (LoggedInClient client = restContext.client("formtester", "formtester")) {
			response = client.get().base().path("form").queryParam("folderId", testFolder.getId()).queryParam("recursive", true).request().get(FormListResponse.class);
		}
		assertResponseOK(response);
		FormModel expected = new FormModel();
		expected.setId(form.getId());
		assertThat(response.getItems()).as("Forms list").usingElementComparatorOnFields("id").containsOnly(expected);
	}

	@Test
	public void testLoad() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));

		assertThat(response.getItem()).as("Loaded form")
			.hasName("Test Form")
			.hasFormType(FormTypeConfigurationModifier.GENERIC_TYPE)
			.hasFormTypeName("CMS Formulare")
			.isOffline()
			.isNotModified()
			.hasVersion("0.1")
			.isNotLocked();
	}

	@Test
	public void testLoadLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));

		assertThat(response.getItem()).as("Loaded form").hasName("Test Form").isOffline().isNotModified().hasVersion("0.1").isLocked();
	}

	@Test
	public void testLoadInvalid() throws NodeException {
		if (feature) {
			exceptionChecker.expect(NotFoundException.class, "Das angegebene Formular wurde nicht gefunden.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("4711").request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
	}

	@Test
	public void testCreate() throws NodeException {
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem())
			.as("Created form")
			.isNotNull()
			.hasFormType(FormTypeConfigurationModifier.GENERIC_TYPE)
			.hasName("Test Form")
			.hasVersion("0.1")
			.isNotModified()
			.isOffline()
			.isNotDeleted()
			.isLocked();
	}

	@Test
	public void testCreateNoName() throws NodeException {
		if (feature) {
			exceptionChecker.expect(BadRequestException.class, "Das Feld 'Name' darf nicht leer sein.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			request.setFolderId(testFolder.getId());
			request.setLanguages(Arrays.asList("de"));
			return target.path("form").request().post(Entity.json(request), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateNoLanguage() throws NodeException {
		if (feature) {
			exceptionChecker.expect(BadRequestException.class, "Das Feld 'Sprache' darf nicht leer sein.");
		}

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateInvalidLanguage() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Unbekannte Sprache bla.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("bla"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateLanguageNotAssigned() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Die Sprache Français ist im Node Master nicht aktiviert.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("fr"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateNoFormType() throws NodeException {
		if (feature) {
			exceptionChecker.expect(BadRequestException.class, "Das Feld 'Formulartyp' darf nicht leer sein.");
		}

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateInvalidFormType() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Unbekannter Formulartyp bla.");
		}

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType("bla");
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateFormTypeNotAssigned() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Formulartyp %s ist im Node %s nicht aktiviert.".formatted(FormTypeConfigurationModifier.GENERIC_TYPE, "Master"));
		}

		consume(n -> FormFactory.removeFormTypeFromNode(FormTypeConfigurationModifier.GENERIC_TYPE, n.getId()), node);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateNoFolder() throws NodeException {
		if (feature) {
			exceptionChecker.expect(BadRequestException.class, "Das Feld 'Ordner' darf nicht leer sein.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			request.setName("Test Form");
			request.setLanguages(Arrays.asList("de"));
			return target.path("form").request().post(Entity.json(request), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateNoFolderWithNodeId() throws NodeException {
		if (feature) {
			exceptionChecker.expect(BadRequestException.class, "Das Feld 'Ordner' darf nicht leer sein.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			request.setName("Test Form");
			request.setLanguages(Arrays.asList("de"));
			return target.path("form").queryParam("nodeId", node.getId()).request().post(Entity.json(request), FormResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateInDefaultFolder() throws NodeException {
		node = Builder.update(node, n -> {
			n.setDefaultFormFolder(testFolder);
		}).build();

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").queryParam("nodeId", node.getId()).request().post(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.hasFolder(testFolder.getId()).isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateInvalidFolder() throws NodeException {
		if (feature) {
			exceptionChecker.expect(NotFoundException.class, "Der angegebene Ordner wurde nicht gefunden.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			request.setName("Test Form");
			request.setFolderId(4711);
			request.setLanguages(Arrays.asList("de"));
			return target.path("form").request().post(Entity.json(request), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.createform));
	}

	@Test
	public void testCreateLocalizedFolder() throws NodeException {
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(localizedFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasFolder(testFolder.getId());
	}

	@Test
	public void testCreateLocalFolder() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(BadRequestException.class,
					"Im Ordner /Channel/Local können keine Formulare erstellt werden, weil er zu einem Channel gehört.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			request.setName("Test Form");
			request.setFolderId(localFolder.getId());
			request.setLanguages(Arrays.asList("de"));
			return target.path("form").request().post(Entity.json(request), FormResponse.class);
		}, perm(localFolder, PermType.read), perm(localFolder, PermType.viewform), perm(localFolder, PermType.createform));
	}

	@Test
	public void testCreateNodeWithoutLanguage() throws NodeException {
		if (feature && nodeFeature) {
			exceptionChecker.expect(BadRequestException.class,
					"Im Node NoLanguage können keine Formulare erstellt werden, weil er keine Sprachen hat.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(noLangTestFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(noLangTestFolder, PermType.read), perm(noLangTestFolder, PermType.viewform),
				perm(noLangTestFolder, PermType.createform));
	}

	@Test
	public void testCreateWithExternalId() throws NodeException {
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					request.setExternalId(EXTERNAL_FORM_ID);
					return target.path("form").queryParam("nodeId", Integer.toString(node.getId())).request().post(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.hasExternalId(EXTERNAL_FORM_ID).isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testCreateWithExternalIdTwice() throws NodeException {
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					request.setExternalId(EXTERNAL_FORM_ID);
					return target.path("form").queryParam("nodeId", Integer.toString(node.getId())).request().post(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem())
			.as("Created form")
			.isNotNull()
			.hasName("Test Form")
			.hasVersion("0.1")
			.hasExternalId(EXTERNAL_FORM_ID)
			.isNotModified()
			.isOffline()
			.isNotDeleted()
			.isLocked();

		int formId = response.getItem().getId();

		ContentNodeTestUtils.waitForNextSecond();

		response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form with different name");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					request.setExternalId(EXTERNAL_FORM_ID);
					return target.path("form").queryParam("nodeId", Integer.toString(node.getId())).request().post(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.updateform));
		assertThat(response.getItem())
			.as("Updated form")
			.isNotNull()
			.hasName("Test Form with different name")
			.hasVersion("0.2")
			.hasExternalId(EXTERNAL_FORM_ID)
			.hasId(formId)
			.isNotModified()
			.isOffline()
			.isNotDeleted()
			.isLocked();
	}

	@Test
	public void testCreateWithExternalIdOtherNode() throws NodeException {
		Form otherForm = create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(otherNodeTestFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		});

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(testFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					request.setExternalId(EXTERNAL_FORM_ID);
					return target.path("form").queryParam("nodeId", Integer.toString(node.getId())).request().post(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.createform));
		assertThat(response.getItem()).as("Created form").isNotNull().hasName("Test Form").hasVersion("0.1")
				.hasExternalId(EXTERNAL_FORM_ID).doesNotHave(attribute("id", otherForm.getId())).isNotModified().isOffline().isNotDeleted().isLocked();
	}

	@Test
	public void testUpdate() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		waitForNextSecond();

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().hasName("Updated Name").hasVersion("0.2");
	}

	@Test
	public void testUpdateLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			FormModel request = new FormModel();
			request.setName("Updated Name");
			return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testUpdateInvalidLanguage() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Unbekannte Sprache bla.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					request.setLanguages(Arrays.asList("de", "bla"));
					return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testUpdateLanguageNotAssigned() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class, "Die Sprache Français ist im Node Master nicht aktiviert.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					request.setLanguages(Arrays.asList("de", "fr"));
					return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testUpdateWithExternalId() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		}));

		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		waitForNextSecond();

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					return target.path("form").path(EXTERNAL_FORM_ID).queryParam("nodeId", Integer.toString(node.getId())).request().put(Entity.json(request), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().hasName("Updated Name").hasVersion("0.2");
	}

	@Test
	public void testPartialUpdate() throws NodeException, JsonMappingException, JsonProcessingException {
		JsonMapper mapper = JsonMapper.builder().build();

		JsonNode initialData = mapper.readTree("{\"schema\": {\"properties\": {}}}");
		JsonNode updateData = mapper.readTree("{\"uiSchema\": {\"formname\": {\"de\": \"Test Form\", \"en\": \"Test Form\"}}}");

		JsonNode finalData = mapper.readTree("{\"schema\": {\"properties\": {}}, \"uiSchema\": {\"formname\": {\"de\": \"Test Form\", \"en\": \"Test Form\"}}}");

		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
			f.setLanguages(Arrays.asList("de"));
			f.setData(initialData);
		}));

		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		waitForNextSecond();

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					request.setData(updateData);
					return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().hasName("Updated Name").hasVersion("0.2").hasFormData(finalData);
	}

	@Test
	public void testLock() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("lock").request().put(Entity.json(""), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().isLocked();
	}

	@Test
	public void testLockLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("lock").request().put(Entity.json(""), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testLockWithExternalId() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		consume(Form::unlock, form);
		form = execute(Form::reload, form);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("lock").queryParam("nodeId", Integer.toString(node.getId())).request().put(Entity.json(""), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().isLocked();
	}

	@Test
	public void testUnlock() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("lock").request().delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().isNotLocked();
	}

	@Test
	public void testUnlockLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("lock").request().delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testUnlockWithExternalId() throws NodeException {
		supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("lock").queryParam("nodeId", Integer.toString(node.getId())).request().delete(FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().isNotLocked();
	}

	@Test
	public void testDelete() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).request().delete(GenericResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.deleteform));

		form = execute(Form::reload, form);
		assertThat(form).as("Deleted form").isNull();

		try (Trx trx = new Trx(); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			Form inWastebin = trx.getTransaction().getObject(Form.class, formId);
			assertThat(inWastebin).as("Form in wastebin").isNotNull().isDeleted();
		}
	}

	@Test
	public void testDeleteLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).request().delete(GenericResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.deleteform));
	}

	@Test
	public void testDeleteWithExternalId() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).queryParam("nodeId", Integer.toString(node.getId())).request().delete(GenericResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.deleteform));

		form = execute(Form::reload, form);
		assertThat(form).as("Deleted form").isNull();

		try (Trx trx = new Trx(); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			Form inWastebin = trx.getTransaction().getObject(Form.class, formId);
			assertThat(inWastebin).as("Form in wastebin").isNotNull().isDeleted();
		}
	}

	@Test
	public void testRemoveFromWastebin() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::delete, form);

		Integer rootFolderId = supply(() -> node.getFolder().getId());

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path("wastebin").path(formId).request().delete(GenericResponse.class);
				},
				Triple.of(Node.TYPE_NODE, rootFolderId, PermType.wastebin.getBit()),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));

		try (Trx trx = new Trx(); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			Form deleted = trx.getTransaction().getObject(Form.class, formId);
			assertThat(deleted).as("Deleted Form").isNull();
		}
	}

	@Test
	public void testRemoveFromWastebinWithExternalId() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::delete, form);

		Integer rootFolderId = supply(() -> node.getFolder().getId());

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path("wastebin").path(EXTERNAL_FORM_ID).queryParam("nodeId", Integer.toString(node.getId())).request().delete(GenericResponse.class);
				},
				Triple.of(Node.TYPE_NODE, rootFolderId, PermType.wastebin.getBit()),
				perm(nodeRootFolder, PermType.read),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));

		try (Trx trx = new Trx(); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			Form deleted = trx.getTransaction().getObject(Form.class, formId);
			assertThat(deleted).as("Deleted Form").isNull();
		}
	}

	@Test
	public void testRestoreFromWastebin() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::delete, form);

		Integer rootFolderId = supply(() -> node.getFolder().getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path("wastebin").path(formId).path("restore").request().post(Entity.json(""), FormResponse.class);
				},
				Triple.of(Node.TYPE_NODE, rootFolderId, PermType.wastebin.getBit()),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItem()).as("Restored form").isNotDeleted();
	}

	@Test
	public void testRestoreFromWastebinWithExternalId() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		consume(Form::delete, form);

		Integer rootFolderId = supply(() -> node.getFolder().getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path("wastebin").path(EXTERNAL_FORM_ID).path("restore").queryParam("nodeId", Integer.toString(node.getId())).request().post(Entity.json(""), FormResponse.class);
				},
				Triple.of(Node.TYPE_NODE, rootFolderId, PermType.wastebin.getBit()),
				perm(nodeRootFolder, PermType.read),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItem()).as("Restored form").isNotDeleted();
	}

	@Test
	public void testRestoreFromWastebinDuplicateName() throws NodeException {
		Form form = createNoVersions(true, null);
		String formId = Integer.toString(form.getId());
		consume(Form::delete, form);

		// create a duplicate
		Form duplicate = createNoVersions(true, null);
		assertThat(duplicate).as("Duplicate").hasFieldOrPropertyWithValue("name", "Test Form");

		Integer rootFolderId = supply(() -> node.getFolder().getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path("wastebin").path(formId).path("restore").request().post(Entity.json(""), FormResponse.class);
				},
				Triple.of(Node.TYPE_NODE, rootFolderId, PermType.wastebin.getBit()),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItem()).as("Restored form").isNotDeleted().hasName("Test Form 1");
	}

	@Test
	public void testPublish() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOnline().isNotPlanned().isNotModified()
				.isNotLocked().hasVersion("1.0");
	}

	@Test
	public void testPublishWithExternalId() throws NodeException {
		supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(EXTERNAL_FORM_ID).path("online").queryParam("nodeId", Integer.toString(node.getId())).request().put(Entity.json(""), FormResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOnline().isNotPlanned().isNotModified()
				.isNotLocked().hasVersion("1.0");
	}

	@Test
	public void testPublishAtPast() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		int past = (int) (System.currentTimeMillis() / 1000) - 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("online").queryParam("at", past).request()
							.put(Entity.json(""), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOnline().isNotPlanned().isNotModified()
				.isNotLocked().hasVersion("1.0");
	}

	@Test
	public void testPublishAtFuture() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", future).request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(future, "1.0").hasVersion("1.0");
	}

	@Test
	public void testRepeatPublishAt() throws NodeException {
		Form form = supply(testUser, t -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", future).request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(future, "1.0").hasVersion("1.0");

		waitForNextSecond();

		// modify the form
		form = update(form, upd -> {
			upd.setName("Modified " + upd.getName());
		});
		consume(Form::unlock, form);

		// make another "publish at" call
		int farFuture = future + 1000;
		response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", farFuture).request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		// expect a new version to be planned
		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(farFuture, "2.0").hasVersion("2.0");
	}

	@Test
	public void testChangePublishAt() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", future).request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(future, "1.0").hasVersion("1.0");

		waitForNextSecond();

		// modify the form
		form = update(form, upd -> {
			upd.setName("Modified " + upd.getName());
		});
		consume(Form::unlock, form);

		// make another "publish at" call, but keep the version
		int farFuture = future + 1000;
		response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", farFuture).queryParam("keepVersion", "true").request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		// expect the previous "publish at" version to be re-scheduled
		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(farFuture, "1.0").hasVersion("1.1");
	}

	@Test
	public void testClearPublishAt() throws NodeException {
		Form form = supply(testUser, () -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").queryParam("at", future).request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isPlannedOnline(future, "1.0").hasVersion("1.0");

		response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("publish").path("at").request().delete(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));

		assertThat(response.getItem()).as("Published form").isNotNull().isOffline().isNotModified().isNotLocked()
				.isNotPlanned().hasVersion("1.0");
	}

	@Test
	public void testPublishLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").request().put(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
	}

	@Test
	public void testModifyPublished() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		waitForNextSecond();

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setName("Updated Name");
					return target.path("form").path(formId).request().put(Entity.json(request), FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.updateform));
		assertThat(response.getItem()).as("Updated form").isNotNull().hasName("Updated Name").isOnline().isModified()
				.isLocked().hasVersion("1.1");
	}

	@Test
	public void testTakeOffline() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("online").request().delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOffline();
	}

	@Test
	public void testTakeOfflineWithExternalId() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("online").queryParam("nodeId", Integer.toString(node.getId())).request().delete(FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOffline();
	}

	@Test
	public void testTakeOfflinePast() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		int past = (int) (System.currentTimeMillis() / 1000) - 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("online").queryParam("at", past).request()
							.delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOffline().isNotPlanned();
	}

	@Test
	public void testTakeOfflineFuture() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("online").queryParam("at", future).request()
							.delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOnline().isPlannedOffline(future);
	}

	@Test
	public void testClearTakeOfflineAt() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		int future = (int) (System.currentTimeMillis() / 1000) + 3600;
		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("online").queryParam("at", future).request()
							.delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOnline().isPlannedOffline(future);

		response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("offline").path("at").request()
							.delete(FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.publishform));
		assertThat(response.getItem()).as("Offline form").isNotNull().isOnline().isNotPlanned();
	}

	@Test
	public void testTakeOfflineLocked() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolderId(testFolder.getId());
		}));
		String formId = Integer.toString(form.getId());
		consume(Form::publish, form);
		form = execute(Form::reload, form);

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		// lock the form
		try (Trx trx = new Trx()) {
			trx.getTransaction().getObject(form, true);
			trx.success();
		}
		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("online").request().delete(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.publishform));
	}

	@Test
	public void testListVersions() throws NodeException {
		Form form = createWithVersions(false, null);
		String formId = Integer.toString(form.getId());

		ItemVersionListResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("version").request()
							.get(ItemVersionListResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItems()).as("Versions").usingElementComparatorOnFields("timestamp", "number")
				.containsExactly(new ItemVersion().setNumber("1.1").setTimestamp(4),
						new ItemVersion().setNumber("1.0").setTimestamp(3),
						new ItemVersion().setNumber("0.2").setTimestamp(2),
						new ItemVersion().setNumber("0.1").setTimestamp(1));
	}

	@Test
	public void testListVersionsWithExternalId() throws NodeException {
		createWithVersions(false, EXTERNAL_FORM_ID);

		ItemVersionListResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("version").queryParam("nodeId", Integer.toString(node.getId())).request()
							.get(ItemVersionListResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItems()).as("Versions").usingElementComparatorOnFields("timestamp", "number")
				.containsExactly(new ItemVersion().setNumber("1.1").setTimestamp(4),
						new ItemVersion().setNumber("1.0").setTimestamp(3),
						new ItemVersion().setNumber("0.2").setTimestamp(2),
						new ItemVersion().setNumber("0.1").setTimestamp(1));
	}

	@Test
	public void testLoadVersion() throws NodeException {
		Form form = createWithVersions(false, null);
		String formId = Integer.toString(form.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("version").path("0.2").request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItem()).as("Form version").isNotNull().hasVersion("0.2").hasName("First Update");
	}

	@Test
	public void testLoadVersionWithExternalId() throws NodeException {
		createWithVersions(false, EXTERNAL_FORM_ID);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(EXTERNAL_FORM_ID).path("version").path("0.2").queryParam("nodeId", Integer.toString(node.getId())).request().get(FormResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
		assertThat(response.getItem()).as("Form version").isNotNull().hasVersion("0.2").hasName("First Update");
	}

	@Test
	public void testLoadInvalidVersion() throws NodeException {
		Form form = createWithVersions(false, null);
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(NotFoundException.class, String.format("Das Formular %s hat keine Version %s.", formId, "1.3"));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("version").path("1.3").request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
	}

	@Test
	public void testLoadVersionOfInvalidForm() throws NodeException {
		if (feature) {
			exceptionChecker.expect(NotFoundException.class, "Das angegebene Formular wurde nicht gefunden.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("4711").path("version").path("0.1").request().get(FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform));
	}

	@Test
	public void testRestoreVersion() throws NodeException {
		Form form = createWithVersions(true, null);
		String formId = Integer.toString(form.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("version").path("1.0").path("restore").request()
					.post(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));

		assertThat(response.getItem()).as("Restored form").hasName("Second Update").hasVersion("1.2").isModified().isOnline();
	}

	@Test
	public void testRestoreVersionWithExternalId() throws NodeException {
		createWithVersions(true, EXTERNAL_FORM_ID);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(EXTERNAL_FORM_ID).path("version").path("1.0").path("restore").queryParam("nodeId", Integer.toString(node.getId())).request()
					.post(Entity.json(""), FormResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));

		assertThat(response.getItem()).as("Restored form").hasName("Second Update").hasVersion("1.2").isModified().isOnline();
	}

	@Test
	public void testRestoreInvalidVersion() throws NodeException {
		Form form = createWithVersions(true, null);
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(NotFoundException.class, String.format("Das Formular %s hat keine Version %s.", formId, "3.5"));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("version").path("3.5").path("restore").request()
					.post(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testRestoreVersionOfInvalidForm() throws NodeException {
		if (feature) {
			exceptionChecker.expect(NotFoundException.class, "Das angegebene Formular wurde nicht gefunden.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("4711").path("version").path("3.5").path("restore").request()
					.post(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testRestoreVersionLocked() throws NodeException {
		Form form = createWithVersions(false, null);
		String formId = Integer.toString(form.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular %s kann nicht bearbeitet werden, weil es gerade von einem anderen Benutzer gesperrt ist.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(formId).path("version").path("0.1").path("restore").request()
					.post(Entity.json(""), FormResponse.class);
		}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform), perm(testFolder, PermType.updateform));
	}

	@Test
	public void testDeleteFolderIntoWastebin() throws NodeException {
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(subFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(subFolder, PermType.read), perm(subFolder, PermType.viewform),
				perm(subFolder, PermType.createform));

		Form form = execute(id -> TransactionManager.getCurrentTransaction().getObject(Form.class, id), response.getItem().getId());
		assertThat(form).as("Form").isNotNull().isNotDeleted();

		consume(Folder::delete, subFolder);
		form = execute(id -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				return t.getObject(Form.class, id);
			}
		}, response.getItem().getId());
		assertThat(form).as("Form").isNotNull().isDeleted();
	}

	@Test
	public void testRemoveFolderFromWastebin() throws NodeException {
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));
		String subFolderId = execute(f -> Integer.toString(f.getId()), subFolder);

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(subFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(subFolder, PermType.read), perm(subFolder, PermType.viewform),
				perm(subFolder, PermType.createform));

		Form form = execute(id -> TransactionManager.getCurrentTransaction().getObject(Form.class, id), response.getItem().getId());
		assertThat(form).as("Form").isNotNull().isNotDeleted();

		consume(id -> assertResponseOK(getFolderResource().delete(id, null, null)), subFolderId);
		consume(id -> assertResponseOK(getFolderResource().deleteFromWastebin(id, 0)), subFolderId);

		form = execute(id -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				return t.getObject(Form.class, id);
			}
		}, response.getItem().getId());
		assertThat(form).as("Form").isNull();
	}

	@Test
	public void testDeleteFolder() throws NodeException {
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					FormModel request = new FormModel();
					request.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
					request.setName("Test Form");
					request.setFolderId(subFolder.getId());
					request.setLanguages(Arrays.asList("de"));
					return target.path("form").request().post(Entity.json(request), FormResponse.class);
				}, perm(subFolder, PermType.read), perm(subFolder, PermType.viewform),
				perm(subFolder, PermType.createform));

		Form form = execute(id -> TransactionManager.getCurrentTransaction().getObject(Form.class, id), response.getItem().getId());
		assertThat(form).as("Form").isNotNull().isNotDeleted();

		consume(f -> f.delete(true), subFolder);
		form = execute(id -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				return t.getObject(Form.class, id);
			}
		}, response.getItem().getId());
		assertThat(form).as("Form").isNull();
	}

	@Test
	public void testMoveForm() throws NodeException {
		Form form = createNoVersions(true, null);
		String formId = Integer.toString(form.getId());
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));
		String folderId = Integer.toString(subFolder.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("folder").path(folderId).request().put(Entity.json(""),
							FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(subFolder, PermType.read),
				perm(subFolder, PermType.viewform), perm(subFolder, PermType.createform));

		assertThat(response.getItem()).as("Moved form").isNotNull().hasFolder(subFolder.getId());
	}

	@Test
	public void testMoveFormWithExternalId() throws NodeException {
		createNoVersions(true, EXTERNAL_FORM_ID);
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));
		String folderId = Integer.toString(subFolder.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("folder").path(folderId).queryParam("nodeId", Integer.toString(node.getId())).request().put(Entity.json(""),
							FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(subFolder, PermType.read),
				perm(subFolder, PermType.viewform), perm(subFolder, PermType.createform));

		assertThat(response.getItem()).as("Moved form").isNotNull().hasFolder(subFolder.getId());
	}

	@Test
	public void testMoveFormOtherNode() throws NodeException {
		if (feature) {
			// activate the feature in the source node
			operate(() -> node.activateFeature(Feature.FORMS));
		}

		Form form = createNoVersions(true, null);
		String formId = Integer.toString(form.getId());
		String folderId = Integer.toString(otherNodeTestFolder.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("folder").path(folderId).request().put(Entity.json(""),
							FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(otherNodeTestFolder, PermType.read),
				perm(otherNodeTestFolder, PermType.viewform), perm(otherNodeTestFolder, PermType.createform));

		assertThat(response.getItem()).as("Moved form").isNotNull().hasFolder(otherNodeTestFolder.getId());
	}

	@Test
	public void testMoveFormOtherNodeWithExternalId() throws NodeException {
		if (feature) {
			// activate the feature in the source node
			operate(() -> node.activateFeature(Feature.FORMS));
		}

		createNoVersions(true, EXTERNAL_FORM_ID);
		String folderId = Integer.toString(otherNodeTestFolder.getId());

		FormResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(EXTERNAL_FORM_ID).path("folder").path(folderId)
							.queryParam("nodeId", Integer.toString(node.getId())).request()
							.put(Entity.json(""), FormResponse.class);
				}, perm(nodeRootFolder, PermType.read), perm(otherNodeRootFolder, PermType.read),
				perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(otherNodeTestFolder, PermType.read),
				perm(otherNodeTestFolder, PermType.viewform), perm(otherNodeTestFolder, PermType.createform));

		assertThat(response.getItem()).as("Moved form").isNotNull().hasFolder(otherNodeTestFolder.getId());
	}

	@Test
	public void testMoveFormNoLanguage() throws NodeException {
		if (feature) {
			// activate the feature in the source node
			operate(() -> node.activateFeature(Feature.FORMS));
		}

		Form form = createNoVersions(true, null);
		String formId = Integer.toString(form.getId());
		String folderId = Integer.toString(noLangTestFolder.getId());

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					String.format("Das Formular Test Form kann nicht in den Node NoLanguage verschoben werden, weil dieser keine Sprachen hat.", formId));
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("folder").path(folderId).request().put(Entity.json(""),
							FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(noLangTestFolder, PermType.read),
				perm(noLangTestFolder, PermType.viewform), perm(noLangTestFolder, PermType.createform));
	}

	@Test
	public void testMoveFormDuplicateName() throws NodeException {
		Form form = createNoVersions(true, null);
		String formId = Integer.toString(form.getId());
		Folder subFolder = supply(() -> createFolder(testFolder, "Subfolder"));
		String folderId = Integer.toString(subFolder.getId());

		// create duplicate
		createNoVersions(subFolder, true, null);

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					"Das Formular Test Form kann nicht in den Ordner /Master/Folder/Subfolder verschoben werden, weil es darin bereits ein gleichnamiges Formular gibt.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext,
				target -> {
					return target.path("form").path(formId).path("folder").path(folderId).request().put(Entity.json(""),
							FormResponse.class);
				}, perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(subFolder, PermType.read),
				perm(subFolder, PermType.viewform), perm(subFolder, PermType.createform));
	}

	@Test
	public void testMoveFormDuplicateExternalId() throws NodeException {
		createNoVersions(true, EXTERNAL_FORM_ID);

		create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setExternalId(EXTERNAL_FORM_ID);
			f.setName("Other Test Form");
			f.setFolderId(otherNodeTestFolder.getId());
			f.setLanguages(Arrays.asList("de"));
		});

		if (feature && nodeFeature) {
			exceptionChecker.expect(ClientErrorException.class,
					"Das Formular Test Form kann nicht in den Ordner /Other/Folder verschoben werden, weil es im Zielnode bereits ein Formular mit gleicher externer ID gibt.");
		}

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path(EXTERNAL_FORM_ID).path("folder")
					.path(Integer.toString(otherNodeTestFolder.getId()))
					.queryParam("nodeId", Integer.toString(node.getId())).request()
					.put(Entity.json(""), FormResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(otherNodeRootFolder, PermType.read), perm(testFolder, PermType.read), perm(testFolder, PermType.viewform),
				perm(testFolder, PermType.deleteform), perm(otherNodeTestFolder, PermType.read),
				perm(otherNodeTestFolder, PermType.viewform), perm(otherNodeTestFolder, PermType.createform));
	}

	/**
	 * Create a form in the {@link #testFolder}
	 * @param unlock true to unlock the form
	 * @param externalId optional external ID
	 * @return form
	 * @throws NodeException
	 */
	protected Form createNoVersions(boolean unlock, String externalId) throws NodeException {
		return createNoVersions(testFolder, unlock, externalId);
	}

	/**
	 * Create a form in the given folder
	 * @param folder folder
	 * @param unlock true to unlock the form
	 * @param externalId optional external ID
	 * @return form
	 * @throws NodeException
	 */
	protected Form createNoVersions(Folder folder, boolean unlock, String externalId) throws NodeException {
		Form form = supply(t -> {
			t.setTimestamp(1000);
			return create(Form.class, f -> {
				f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
				f.setExternalId(externalId);
				f.setName("Test Form");
				f.setFolderId(folder.getId());
			});
		});

		if (unlock) {
			consume(Form::unlock, form);
		}
		form = execute(Form::reload, form);

		return form;
	}

	/**
	 * Create a form with a couple of versions
	 * @param unlock true to unlock the form
	 * @param externalId optional external ID
	 * @return form
	 * @throws NodeException
	 */
	protected Form createWithVersions(boolean unlock, String externalId) throws NodeException {
		Form form = supply(t -> {
			t.setTimestamp(1000);
			return create(Form.class, f -> {
				f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
				f.setExternalId(externalId);
				f.setName("Test Form");
				f.setFolderId(testFolder.getId());
			});
		});

		form = execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(2000);
			return update(f, upd -> {
				upd.setName("First Update");
			});
		}, form);

		form = execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(3000);
			return update(f, upd -> {
				upd.setName("Second Update");
			});
		}, form);
		consume(Form::publish, form);

		form = execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4000);
			return update(f, upd -> {
				upd.setName("Third Update");
			});
		}, form);

		if (unlock) {
			consume(Form::unlock, form);
		}
		form = execute(Form::reload, form);

		return form;
	}
}
