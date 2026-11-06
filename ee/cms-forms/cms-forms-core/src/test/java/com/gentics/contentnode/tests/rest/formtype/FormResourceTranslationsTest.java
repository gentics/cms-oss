package com.gentics.contentnode.tests.rest.formtype;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier.GENERIC_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslationsForLanguage;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslationsResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.RestExceptionChecker;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;

/**
 * Test cases for translation management for form type configurations
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.FORMS })
@ConfigBy(FormResourceTranslationsTest.class)
@RunWith(Parameterized.class)
public class FormResourceTranslationsTest implements ConfigModifier {
	private final static String LOGIN = "formtester";

	private final static String PASSWORD = "formtester";

	private final static FormGenTranslations GLOBAL = new FormGenTranslations();

	private final static FormGenTranslations FOR_FORMTYPE = new FormGenTranslations();

	private final static String INVALID_TYPE = "invalid";

	private final static int CUSTOM_TOOL_ID = 47;

	private final static String CUSTOM_TOOL_NAME_DE = "Formular Übersetzungen";

	private final static String CUSTOM_TOOL_NAME_EN = "Form Translations";

	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup testGroup;

	private static Node nodeWithoutFeature;

	private static Node nodeWithoutPermission;

	private static Node node1;

	private static Node node2;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		supply(() -> createSystemUser("formtester", "formtester", null, LOGIN, PASSWORD,
				Arrays.asList(testGroup)));

		GLOBAL.putAll(Map.of("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en"),
				"global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en")));
		FOR_FORMTYPE.putAll(Map.of("formtype_key_one",
				Map.of("de", "formtype_trans_one_de", "en", "formtype_trans_one_en"), "formtype_key_two",
				Map.of("de", "formtype_trans_two_de", "en", "formtype_trans_two_en"), "global_key_one",
				Map.of("en", "global_trans_one_en_formtype", "fr", "global_trans_one_fr_formtype")));

		nodeWithoutFeature = supply(() -> createNode("node.without.feature", "Node without Feature", PublishTarget.NONE,
				getLanguage("de"), getLanguage("it")));
		nodeWithoutPermission = supply(() -> createNode("node.without.permission", "Node without Permission",
				PublishTarget.NONE, getLanguage("de"), getLanguage("fr")));
		consume(n -> n.activateFeature(Feature.FORMS), nodeWithoutPermission);
		node1 = supply(() -> createNode("node1", "Node1", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		consume(n -> n.activateFeature(Feature.FORMS), node1);
		node2 = supply(() -> createNode("node2", "Node2", PublishTarget.NONE, getLanguage("de"), getLanguage("es")));
		consume(n -> n.activateFeature(Feature.FORMS), node2);
	}

	@Parameters(name = "{index}: feature {0}, tool {1}, permission {2}, admin {3}, set globally {4}, set for formtype {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (boolean feature : List.of(true, false)) {
			for (boolean tool : List.of(true, false)) {
				for (boolean permission : List.of(true, false)) {
					for (boolean admin : List.of(true, false)) {
						for (boolean setGlobally : List.of(true, false)) {
							for (boolean setForFormType : List.of(true, false)) {
								data.add(new Object[] { feature, tool, permission, admin, setGlobally, setForFormType });
							}
						}
					}
				}
			}
		}

		return data;
	}

	@Rule
	public RestExceptionChecker exceptionChecker = new RestExceptionChecker();

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public boolean tool;

	@Parameter(2)
	public boolean permission;

	@Parameter(3)
	public boolean admin;

	@Parameter(4)
	public boolean setGlobally;

	@Parameter(5)
	public boolean setForFormType;

	@Before
	public void setup() throws NodeException {
		NodePreferences preferences = NodeConfigRuntimeConfiguration.getPreferences();
		preferences.setFeature(Feature.FORMS, feature);

		List<Object> customTools = preferences.getPropertyObject("custom_tools");
		if (tool) {
			customTools.add(Map.of("id", CUSTOM_TOOL_ID, "key", FormResourceImpl.TRANSLATIONS_CUSTOM_TOOL_KEY, "name",
					Map.of("de", CUSTOM_TOOL_NAME_DE, "en", CUSTOM_TOOL_NAME_EN)));
		} else {
			customTools.clear();
		}

		if (setGlobally) {
			preferences.getPropertyMap("forms").put("formGenTranslations", GLOBAL);
		} else {
			preferences.getPropertyMap("forms").remove("formGenTranslations");
		}

		FormTypeConfiguration config = preferences.getPropertyObject("forms.types.%s".formatted(FormTypeConfigurationModifier.GENERIC_TYPE));
		if (setForFormType) {
			config.formGenTranslations = FOR_FORMTYPE;
		} else {
			config.formGenTranslations = null;
		}

		// remove all permissions
		operate(() -> DBUtils.update("DELETE FROM perm WHERE usergroup_id = ?", testGroup.getId()));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(testGroup.getId()));

		if (admin) {
			operate(() -> setPermissions(PermHandler.TYPE_CUSTOM_TOOLS, 0, List.of(testGroup),
					Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_CHANGE_PERM).toString()));
		}
		if (permission) {
			operate(() -> setPermissions(PermHandler.TYPE_CUSTOM_TOOL, CUSTOM_TOOL_ID, List.of(testGroup),
					Permissions.get(PermHandler.PERM_VIEW).toString()));
		}

		for (Node node : List.of(nodeWithoutFeature, node1, node2)) {
			operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), List.of(testGroup)));
		}

		if (!feature) {
			operate(() -> {
				exceptionChecker.expect(NotAllowedException.class, I18NHelper.get("rest.feature.required", Feature.FORMS.getName()));
			});
		} else if (!tool) {
			operate(() -> {
				exceptionChecker.expect(NotFoundException.class, I18NHelper.get("custom_tool.notfound", FormResourceImpl.TRANSLATIONS_CUSTOM_TOOL_KEY));
			});
		} else if (!admin && !permission) {
			operate(() -> {
				exceptionChecker.expect(ForbiddenException.class, I18NHelper.get("custom_tool.nopermission", CUSTOM_TOOL_NAME_DE));
			});
		}

		// re-initialize the form config provider
		new FormConfigProvider().init(preferences);

		operate(() -> {
			DBUtils.update("DELETE FROM form_translations");
		});
	}

	@Test
	public void testReadGlobal() throws NodeException, RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FormGenTranslationsResponse response = client.get().base().path("form").path("translations").request()
					.get(FormGenTranslationsResponse.class);

			if (setGlobally) {
				assertThat(response.getItem()).as("Global translations")
						.isEqualTo(Map.of("global_key_one",
								Map.of("de", "global_trans_one_de", "en", "global_trans_one_en"), "global_key_two",
								Map.of("de", "global_trans_two_de", "en", "global_trans_two_en")));
			} else {
				assertThat(response.getItem()).as("Global translations").isEmpty();
			}
		}
	}

	@Test
	public void testSaveGlobal() throws NodeException, RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FormGenTranslations request = new FormGenTranslations();
			request.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "fr", "global_trans_one_fr"));
			request.put("unknown_key", Map.of("de", "unknown_trans_de"));

			FormGenTranslations expected = new FormGenTranslations();
			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "en", "global_trans_one_en",
						"fr", "global_trans_one_fr"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));
			}

			// update
			FormGenTranslationsResponse response = client.get().base().path("form").path("translations").request().post(Entity.json(request),
					FormGenTranslationsResponse.class);

			assertThat(response.getItem()).as("Modified global translations").isEqualTo(expected);

			// read for formtype (should see global overwrites)
			response = client.get().base().path("form").path("types").path(GENERIC_TYPE).path("translations").request()
					.get(FormGenTranslationsResponse.class);
			expected.clear();
			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "en", "global_trans_one_en",
						"fr", "global_trans_one_fr"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));
			}
			if (setForFormType) {
				if (setGlobally) {
					expected.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "en",
							"global_trans_one_en_formtype", "fr", "global_trans_one_fr_formtype"));
				} else {
					expected.put("global_key_one", Map.of("en", "global_trans_one_en_formtype", "fr", "global_trans_one_fr_formtype"));
				}
				expected.put("formtype_key_one", Map.of("de", "formtype_trans_one_de", "en", "formtype_trans_one_en"));
				expected.put("formtype_key_two", Map.of("de", "formtype_trans_two_de", "en", "formtype_trans_two_en"));
			}

			assertThat(response.getItem()).as("Formtype translations with global overwrites").isEqualTo(expected);

			// update again (remove overwrites)
			request.clear();
			expected.clear();
			response = client.get().base().path("form").path("translations").request().post(Entity.json(request),
					FormGenTranslationsResponse.class);
			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));
			}

			assertThat(response.getItem()).as("Modified global translations").isEqualTo(expected);

		}
	}

	@Test
	public void testReadForFormType() throws NodeException, RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FormGenTranslationsResponse response = client.get().base().path("form").path("types").path(GENERIC_TYPE)
					.path("translations").request().get(FormGenTranslationsResponse.class);

			FormGenTranslations expected = new FormGenTranslations();
			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));
			}
			if (setForFormType) {
				if (setGlobally) {
					expected.put("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en_formtype",
							"fr", "global_trans_one_fr_formtype"));
				} else {
					expected.put("global_key_one",
							Map.of("en", "global_trans_one_en_formtype", "fr", "global_trans_one_fr_formtype"));
				}
				expected.put("formtype_key_one", Map.of("de", "formtype_trans_one_de", "en", "formtype_trans_one_en"));
				expected.put("formtype_key_two", Map.of("de", "formtype_trans_two_de", "en", "formtype_trans_two_en"));
			}

			assertThat(response.getItem()).as("Formtype translations").isEqualTo(expected);
		}
	}

	@Test
	public void testSaveForFormType() throws NodeException, RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FormGenTranslations request = new FormGenTranslations();
			request.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "fr", "global_trans_one_fr"));
			request.put("formtype_key_two", Map.of("de", "formtype_trans_two_de_modified", "fr", "formtype_trans_two_fr"));
			request.put("unknown_key", Map.of("de", "unknown_trans_de"));

			// update
			FormGenTranslationsResponse response = client.get().base().path("form").path("types").path(GENERIC_TYPE)
					.path("translations").request().post(Entity.json(request), FormGenTranslationsResponse.class);

			FormGenTranslations expected = new FormGenTranslations();
			Map<String, String> de = new HashMap<>();
			Map<String, String> en = new HashMap<>();
			Map<String, String> fr = new HashMap<>();

			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "en", "global_trans_one_en",
						"fr", "global_trans_one_fr"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));

				de.put("global_key_one", "global_trans_one_de_modified");
				de.put("global_key_two", "global_trans_two_de");

				en.put("global_key_one", "global_trans_one_en");
				en.put("global_key_two", "global_trans_two_en");

				fr.put("global_key_one", "global_trans_one_fr");
			}
			if (setForFormType) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de_modified", "en", "global_trans_one_en_formtype",
						"fr", "global_trans_one_fr"));
				expected.put("formtype_key_one", Map.of("de", "formtype_trans_one_de", "en", "formtype_trans_one_en"));
				expected.put("formtype_key_two", Map.of("de", "formtype_trans_two_de_modified", "en",
						"formtype_trans_two_en", "fr", "formtype_trans_two_fr"));

				de.put("global_key_one", "global_trans_one_de_modified");
				de.put("formtype_key_one", "formtype_trans_one_de");
				de.put("formtype_key_two", "formtype_trans_two_de_modified");

				en.put("global_key_one", "global_trans_one_en_formtype");
				en.put("formtype_key_one", "formtype_trans_one_en");
				en.put("formtype_key_two", "formtype_trans_two_en");

				fr.put("global_key_one", "global_trans_one_fr");
				fr.put("formtype_key_two", "formtype_trans_two_fr");
			}

			assertThat(response.getItem()).as("Modified formtype translations").isEqualTo(expected);

			// get the effective translations for all languages
			FormGenTranslationsForLanguage deTranslations = client.get().base().path("form").path("types").path(GENERIC_TYPE).path("translations").path("de").request()
					.get(FormGenTranslationsForLanguage.class);
			assertThat(deTranslations).as("German translations").isEqualTo(de);
			FormGenTranslationsForLanguage enTranslations = client.get().base().path("form").path("types").path(GENERIC_TYPE).path("translations").path("en").request()
					.get(FormGenTranslationsForLanguage.class);
			assertThat(enTranslations).as("English translations").isEqualTo(en);
			FormGenTranslationsForLanguage frTranslations = client.get().base().path("form").path("types").path(GENERIC_TYPE).path("translations").path("fr").request()
					.get(FormGenTranslationsForLanguage.class);
			assertThat(frTranslations).as("Frensh translations").isEqualTo(fr);

			// update again (remove overwrites)
			request.clear();
			expected.clear();
			response = client.get().base().path("form").path("types").path(GENERIC_TYPE).path("translations").request().post(Entity.json(request),
					FormGenTranslationsResponse.class);

			if (setGlobally) {
				expected.put("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en"));
				expected.put("global_key_two", Map.of("de", "global_trans_two_de", "en", "global_trans_two_en"));
			}
			if (setForFormType) {
				if (setGlobally) {
					expected.put("global_key_one", Map.of("de", "global_trans_one_de", "en", "global_trans_one_en_formtype",
							"fr", "global_trans_one_fr_formtype"));
				} else {
					expected.put("global_key_one",
							Map.of("en", "global_trans_one_en_formtype", "fr", "global_trans_one_fr_formtype"));
				}
				expected.put("formtype_key_one", Map.of("de", "formtype_trans_one_de", "en", "formtype_trans_one_en"));
				expected.put("formtype_key_two", Map.of("de", "formtype_trans_two_de", "en", "formtype_trans_two_en"));
			}

			assertThat(response.getItem()).as("Modified formtype translations").isEqualTo(expected);
		}
	}

	@Test
	public void testReadForInvalidFormType() throws NodeException, RestException {
		if (!exceptionChecker.expectsException() || (feature && tool && (admin || permission))) {
			operate(() -> {
				exceptionChecker.expect(NotFoundException.class, I18NHelper.get("form_type_configuration.notfound", INVALID_TYPE));
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			client.get().base().path("form").path("types").path(INVALID_TYPE).path("translations").request()
					.get(FormGenTranslationsResponse.class);
		}
	}

	@Test
	public void testSaveForInvalidFormType() throws NodeException, RestException {
		if (!exceptionChecker.expectsException() || (feature && tool && (admin || permission))) {
			operate(() -> {
				exceptionChecker.expect(NotFoundException.class, I18NHelper.get("form_type_configuration.notfound", INVALID_TYPE));
			});
		}

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FormGenTranslations request = new FormGenTranslations();
			client.get().base().path("form").path("types").path(INVALID_TYPE).path("translations").request()
					.post(Entity.json(request), FormGenTranslationsResponse.class);
		}
	}

	@Test
	public void testLoadTranslationLanguages() throws NodeException, RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			LanguageList response = client.get().base().path("form").path("translations").path("languages").request()
					.get(LanguageList.class);

			List<com.gentics.contentnode.rest.model.ContentLanguage> expected = supply(() -> {
				List<ContentLanguage> langs = List.of(getLanguage("de"), getLanguage("en"), getLanguage("es"));
				List<com.gentics.contentnode.rest.model.ContentLanguage> models = new ArrayList<>();
				for (ContentLanguage l : langs) {
					models.add(ContentLanguage.TRANSFORM2REST.apply(l));
				}

				return models;
			});

			assertThat(response.getItems()).as("Translation languages").usingRecursiveFieldByFieldElementComparator().hasSameElementsAs(expected);
		}
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("de", "CMS Formular", "en", "CMS Form"),
				"forms");

		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();
		formTypeConfiguration.put(GENERIC_TYPE, formsConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);

		prefs.set("custom_tools", new ArrayList<>());
	}
}
