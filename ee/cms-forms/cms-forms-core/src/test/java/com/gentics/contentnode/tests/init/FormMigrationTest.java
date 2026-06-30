package com.gentics.contentnode.tests.init;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.forms.Migration;
import com.gentics.contentnode.init.MigrateForms;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for form migration
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormMigrationTest.class)
@RunWith(Parameterized.class)
public class FormMigrationTest implements ConfigModifier {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	public final static String GENERIC_TYPE = "generic";
	public final static String POLL_TYPE = "poll";

	public final static String FORM_NAME = "Test Form";

	public final static String FORM_DESCRIPTION = "This is the Test Form";

	public final static int SUCCESS_PAGE_ID = 4711;

	public final static int SUCCESS_NODE_ID = 42;

	private static Node node;

	protected static ObjectMapper mapper = new ObjectMapper();

	@Parameters(name = "{index}: form: {0}")
	public static Collection<Object[]> data() {
		return Stream
				.of("form/simple.json", "form/success.json", "form/mailsettings.json", "form/pages.json",
						"form/recaptcha.json", "form/friendlycaptcha.json",
						"form/inactive_recaptcha.json", "form/inactive_friendlycaptcha.json",
						"controls/input.json", "controls/checkbox.json", "controls/textarea.json",
						"controls/select.json", "controls/file.json", "controls/usermail.json",
						"blocks/sectionheadline.json", "blocks/text.json",
						"blocks/spacer.json", "blocks/conditionalcontainer.json", "blocks/group.json")
				.map(fileName -> new Object[] { fileName }).collect(Collectors.toList());
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		node = update(node, n -> {
			n.activateFeature(Feature.FORMS);
		}).build();

		operate(() -> {
			FormFactory.addFormTypeToNode(GENERIC_TYPE, node.getId());
			FormFactory.addFormTypeToNode(POLL_TYPE, node.getId());
		});
	}

	@Parameter(0)
	public String fileName;

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			clear(node);
		});
	}

	@Test
	public void testMigration() throws NodeException, IOException {
		String originalFormData = getFile(fileName);
		String migratedFormData = getFile(Strings.CI.replace(fileName, ".json", "_migrated.json"));

		int formId = supply(() -> {
			List<Integer> ids = DBUtils.executeInsert(
					"INSERT INTO form (name, description, folder_id, languages, data, formtype, success_page_id, success_node_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					new Object[] { FORM_NAME, FORM_DESCRIPTION, node.getFolder().getId(), "[\"de\", \"en\"]",
							originalFormData, FormFactory.UNMIGRATED_FORMS, SUCCESS_PAGE_ID, SUCCESS_NODE_ID });

			assertThat(ids).as("Form ID List").hasSize(1);
			return ids.get(0);
		});

		MigrateForms migrator = new MigrateForms();
		operate(() -> migrator.execute());

		Form migratedForm = execute(id -> TransactionManager.getCurrentTransaction().getObject(Form.class, id), formId);
		GCNAssertions.assertThat(migratedForm).as("Migrated Form").hasFormType(GENERIC_TYPE).hasFormData(migratedFormData);
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		Migration typeMigrationTool = new Migration(false);
		try {
			formTypeConfiguration.put(GENERIC_TYPE, typeMigrationTool.migrate(GENERIC_TYPE, mapper.readTree(getFile("form-editor.json"))));
			formTypeConfiguration.put(POLL_TYPE, typeMigrationTool.migrate(POLL_TYPE, mapper.readTree(getFile("form-poll-editor.json"))));
		} catch (IOException e) {
			throw new NodeException(e);
		}

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}

	protected String getFile(String path) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(path)) {
			StringWriter out = new StringWriter();
			IOUtils.copy(in, out, Charset.forName("UTF-8"));
			return out.toString();
		}
	}
}
