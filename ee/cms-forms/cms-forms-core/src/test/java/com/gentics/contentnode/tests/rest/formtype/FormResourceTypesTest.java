package com.gentics.contentnode.tests.rest.formtype;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.perm;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.RestExceptionChecker;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;

/**
 * Test cases for form type configurations
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormResourceTypesTest {
	private final static String INVALID_FORM_TYPE = "bla";

	private final static String INVALID_NODE_ID = "4711";

	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static Node node;

	private static Folder nodeRootFolder;

	private static UserGroup testGroup;

	private static SystemUser testUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("Master", "Master", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		nodeRootFolder = execute(Node::getFolder, node);
		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("formtester", "formtester", null, "formtester", "formtester",
				Arrays.asList(testGroup)));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				clear(nodeRootFolder);
			}

			DBUtils.update("DELETE FROM node_formtype");
		});
	}

	@Rule
	public RestExceptionChecker exceptionChecker = new RestExceptionChecker();

	/**
	 * Test loading an existing form type configuration
	 * @throws NodeException
	 */
	@Test
	public void testLoadFormType() throws NodeException {
		FormTypeConfigurationResponse response = assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("types").path(FormTypeConfigurationModifier.GENERIC_TYPE).request().get(FormTypeConfigurationResponse.class);
		});

		assertThat(response.getItem()).as("Loaded form type configuration")
				.hasFieldOrPropertyWithValue("type", FormTypeConfigurationModifier.GENERIC_TYPE)
				.hasFieldOrPropertyWithValue("nameI18n", Map.of("de", "CMS Formular", "en", "CMS Form"))
				.hasOnlyFields("type", "flows", "nameI18n", "descriptionI18n", "external", "submitOnce", "pluginName", "blocks",
						"controls", "templateOptions", "userOptions", "userEmailTemplateOptions",
						"adminEmailTemplateOptions", "captchaOptions");
	}

	/**
	 * Test loading an invalid form type configuration
	 * @throws NodeException
	 */
	@Test
	public void testLoadInvalidFormType() throws NodeException {
		exceptionChecker.expect(NotFoundException.class, "Der Formulartyp %s wurde nicht gefunden.".formatted(INVALID_FORM_TYPE));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("types").path(INVALID_FORM_TYPE).request().get(FormTypeConfigurationResponse.class);
		});
	}

	/**
	 * Test assigning a form type configuration to a node
	 * @throws NodeException
	 */
	@Test
	public void testAssignToNode() throws NodeException {
		Set<String> formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").doesNotContain(FormTypeConfigurationModifier.GENERIC_TYPE);

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(Integer.toString(node.getId())).path("types")
					.path(FormTypeConfigurationModifier.GENERIC_TYPE).request()
					.put(Entity.json(""), GenericResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(nodeRootFolder, PermType.updatefolder));

		formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").contains(FormTypeConfigurationModifier.GENERIC_TYPE);
	}

	/**
	 * Test assigning an invalid form type configuration to a node
	 * @throws NodeException
	 */
	@Test
	public void testAssignInvalidToNode() throws NodeException {
		exceptionChecker.expect(NotFoundException.class, "Der Formulartyp %s wurde nicht gefunden.".formatted(INVALID_FORM_TYPE));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(Integer.toString(node.getId())).path("types")
					.path(INVALID_FORM_TYPE).request().put(Entity.json(""), GenericResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(nodeRootFolder, PermType.updatefolder));
	}

	/**
	 * Test assigning a form type configuration to an invalid node
	 * @throws NodeException
	 */
	@Test
	public void testAssignToInvalidNode() throws NodeException {
		exceptionChecker.expect(NotFoundException.class, "Der Node mit der ID %s konnte nicht gefunden werden.".formatted(INVALID_NODE_ID));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(INVALID_NODE_ID).path("types")
					.path(INVALID_FORM_TYPE).request().put(Entity.json(""), GenericResponse.class);
		});
	}

	/**
	 * Test removing a form type configuration from a node
	 * @throws NodeException
	 */
	@Test
	public void testRemoveFromNode() throws NodeException {
		consume(n -> FormFactory.addFormTypeToNode(FormTypeConfigurationModifier.GENERIC_TYPE, n.getId()), node);
		Set<String> formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").contains(FormTypeConfigurationModifier.GENERIC_TYPE);

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(Integer.toString(node.getId())).path("types")
					.path(FormTypeConfigurationModifier.GENERIC_TYPE).request()
					.delete(GenericResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(nodeRootFolder, PermType.updatefolder));

		formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").doesNotContain(FormTypeConfigurationModifier.GENERIC_TYPE);
	}

	/**
	 * Test removing a form type configuration from a node which contains a form of the type
	 * @throws NodeException
	 */
	@Test
	public void testRemoveFromNodeWithForm() throws NodeException {
		consume(n -> FormFactory.addFormTypeToNode(FormTypeConfigurationModifier.GENERIC_TYPE, n.getId()), node);
		Set<String> formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").contains(FormTypeConfigurationModifier.GENERIC_TYPE);

		create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setFolder(node, nodeRootFolder);
		}).build();

		exceptionChecker.expect(ClientErrorException.class, "Formulartyp %s wird im Node %s noch verwendet."
				.formatted("CMS Formular", "Master"));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(Integer.toString(node.getId())).path("types")
					.path(FormTypeConfigurationModifier.GENERIC_TYPE).request()
					.delete(GenericResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(nodeRootFolder, PermType.updatefolder));

		formTypes = execute(n -> FormFactory.getFormTypeAssignment(n.getId()), node);
		assertThat(formTypes).as("Assigned form types").doesNotContain(FormTypeConfigurationModifier.GENERIC_TYPE);
	}

	/**
	 * Test removing an invalid form type configuration from a node
	 * @throws NodeException
	 */
	@Test
	public void testRemoveInvalidFromNode() throws NodeException {
		exceptionChecker.expect(NotFoundException.class, "Der Formulartyp %s wurde nicht gefunden.".formatted(INVALID_FORM_TYPE));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(Integer.toString(node.getId())).path("types")
					.path(INVALID_FORM_TYPE).request().delete(GenericResponse.class);
		}, perm(nodeRootFolder, PermType.read), perm(nodeRootFolder, PermType.updatefolder));
	}

	/**
	 * Test removing a form type configuration from an invalid node
	 * @throws NodeException
	 */
	@Test
	public void testRemoveFromInvalidNode() throws NodeException {
		exceptionChecker.expect(NotFoundException.class, "Der Node mit der ID %s konnte nicht gefunden werden.".formatted(INVALID_NODE_ID));

		assertRequiredPermissions(testGroup, "formtester", "formtester", restContext, target -> {
			return target.path("form").path("nodes").path(INVALID_NODE_ID).path("types")
					.path(INVALID_FORM_TYPE).request().delete(GenericResponse.class);
		});
	}
}
