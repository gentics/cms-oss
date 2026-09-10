package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.FormAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.FormRESTUtils.getFormResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.publish.mesh.FormsPluginStatusResponse;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.publish.protocol.PublishProtocolUtil;
import com.gentics.contentnode.publish.protocol.PublishState;
import com.gentics.contentnode.publish.protocol.PublishType;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormRESTUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for publishing forms into Mesh.
 * These test cases require a Mesh instance with deployed forms-plugin
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@Category(MeshTest.class)
@RunWith(Parameterized.class)
public class FormPublishTest extends AbstractFormPublishTest {

	protected final static String IMAGE_NAME = "image-dpi66x44-res311x211.jpg";

	@Parameters(name = "{index}: type: {0}, noIndex: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String formType : Arrays.asList(FormTypeConfigurationModifier.GENERIC_TYPE,
				FormTypeConfigurationModifier.ANDP_TYPE)) {
			for (boolean noIndex : List.of(true, false)) {
				data.add(new Object[] { formType, noIndex });
			}
		}
		return data;
	}

	@Parameter(0)
	public String formType;

	@Parameter(1)
	public boolean noIndex;

	/**
	 * Name of the mesh project
	 */
	protected String meshProjectName;

	/**
	 * Name of the plugin to use
	 */
	protected String pluginName;

	@Before
	public void setup() throws NodeException {
		super.setup();
		operate(() -> {
			FormFactory.removeFormTypeFromNode(FormTypeConfigurationModifier.GENERIC_TYPE, node.getId());
			FormFactory.removeFormTypeFromNode(FormTypeConfigurationModifier.ANDP_TYPE, node.getId());
			FormFactory.addFormTypeToNode(formType, node.getId());
		});
		operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setNoFormsIndex(noIndex);
		}));

		meshProjectName = supply(() -> MeshPublisher.getMeshProjectName(node));
		pluginName = supply(() -> FormConfigProvider.getConfig().get(formType).pluginName);
	}

	/**
	 * Get the form data from the resource file
	 * @param appendix appendix of the file (after e.g. generic_ or andp_)
	 * @return form data
	 * @throws NodeException
	 */
	protected JsonNode getFormData(String appendix) throws NodeException {
		try (InputStream in = getClass().getResourceAsStream("%s_%s".formatted(formType, appendix))) {
			return mapper.readValue(in, ObjectNode.class);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Get the form data from the resource file, modified by the given modifier
	 * @param appendix appendix of the file (after e.g. generic_ or andp_)
	 * @param modifier content modifier
	 * @return form data
	 * @throws NodeException
	 */
	protected JsonNode getFormData(String appendix, Function<String, String> modifier) throws NodeException {
		try (InputStream in = getClass().getResourceAsStream("%s_%s".formatted(formType, appendix))) {
			String content = IOUtils.toString(in, Charset.forName("UTF-8"));
			content = modifier.apply(content);
			return mapper.readValue(content, ObjectNode.class);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Test whether the forms plugin is activated for the project
	 * @throws Exception
	 */
	@Test
	public void testActivate() throws Exception {
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// check whether plugin is activated for the Mesh Project now
		FormsPluginStatusResponse response = mesh.client()
				.get("/%s/plugins/%s/active".formatted(meshProjectName, pluginName), FormsPluginStatusResponse.class)
				.blockingGet();
		assertThat(response.getActive()).as("Activation status").isTrue();
	}

	/**
	 * Test moving contents from a folder and then deleting that folder
	 * @throws Exception
	 */
	@Test
	public void testMoveContentsIntoOtherNode() throws Exception {
		// create other unattended node
		Node otherNode1 = Trx.supply(() -> {
			Map<String, ContentLanguage> languages = Trx.supply(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				return t.getObjects(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS)).stream()
						.collect(Collectors.toMap(ContentLanguage::getCode, Function.identity()));
			});
			Node node = createNode("otherNode", "OtherNode", PublishTarget.CONTENTREPOSITORY, languages.get("de"), languages.get("en"));
			node.activateFeature(Feature.FORMS);
			return node;
		});

		Trx.consume(n -> {
			FormFactory.addFormTypeToNode(formType, n.getId());
		}, otherNode1);

		try {
			Node otherNode = update(otherNode1, n -> {
				n.setContentrepositoryId(crId);
				n.activateFeature(Feature.FORMS);
			});

			Trx.operate(trx -> {
				template.getNodes().add(otherNode);
				formConstruct.getNodes().add(otherNode);
				trx.getObject(Construct.class, hbsConstructId).getNodes().add(otherNode);
			});

			Form form = supply(() -> create(Form.class, f -> {
				f.setFormType(formType);
				f.setName("Test Form");
				f.setLanguages(Arrays.asList("de", "en"));
				f.setFolderId(node.getFolder().getId());
				f.setData(getFormData("form_data.json"));
			}));
			consume(Form::publish, form);

			// publish
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			// assert
			FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

			// move form to other node
			Form sameFormOtherNode = supply(trx -> {
				FormResponse result = getFormResource().move(form.getGlobalId().toString(), otherNode.getFolder().getGlobalId().toString(), null);
				assertThat(result.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
				return trx.getObject(Form.class, form.getId());
			});

			// publish
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			// assert
			FormRESTUtils.assertForm(mesh.client(), "OtherNode", sameFormOtherNode, true, true);
		} finally {
			operate(() -> otherNode1.delete(true));
		}
	}

	/**
	 * Test publishing a form
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				StringBuilder result = new StringBuilder();
				boolean crIsClean = mp.checkDataConsistency(false, result);
				assertThat(crIsClean).as("Data check result: " + result.toString()).isTrue();
			}
		});
	}


	@Test
	public void testPublishedFormShouldAddLogEntryWithOnlineState() throws Exception {
		var form = publishForm();

		var formPublishLogEntry = Trx.supply(() ->  PublishProtocolUtil.getPublishLogEntryByObjectId(
				PublishType.FORM.toString(), form.getId()));

		assertThat(formPublishLogEntry.getType()).as("Publish log TYPE").isEqualTo(
				PublishType.FORM.toString());
		assertThat(formPublishLogEntry.getState()).as("Publish log STATE").isEqualTo(
				PublishState.ONLINE.getValue());
	}

	@Test
	public void testUnpublishedFormShouldAddLogEntryWithOfflineState() throws Exception {
		var form = publishForm();
		Trx.operate(()-> form.takeOffline());

		var formPublishLogEntry = Trx.supply(() ->  PublishProtocolUtil.getPublishLogEntryByObjectId(
				PublishType.FORM.toString(), form.getId()));
		var formPublishLogEntries = Trx.supply(PublishProtocolUtil::getPublishLogEntries);
		formPublishLogEntries = formPublishLogEntries.stream()
				.filter(entry -> entry.getType().equals(PublishType.FORM.toString()))
				.toList();


		assertThat(formPublishLogEntry.getType()).as("Publish log TYPE").isEqualTo(
				PublishType.FORM.toString());
		assertThat(formPublishLogEntry.getState()).as("Publish log STATE").isEqualTo(
				PublishState.OFFLINE.getValue());
		Assertions.assertThat(formPublishLogEntries.size()).isGreaterThanOrEqualTo(2);

	}


	private Form publishForm() throws Exception {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		return form;
	}


	/**
	 * Test creating a channel, after forms have been published to the node.
	 * The channel must not have any influence on the data consistency (forms are only published to the default channel).
	 * @throws Exception
	 */
	@Test
	public void testCreateChannel() throws Exception {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// create a channel
		Node channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		channel = update(channel, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(crId);
			n.activateFeature(Feature.FORMS);
		});

		// run publish process again
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert data consistency
		operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				StringBuilder result = new StringBuilder();
				boolean crIsClean = mp.checkDataConsistency(false, result);
				assertThat(crIsClean).as("Data check result: " + result.toString()).isTrue();
			}
		});
	}

	/**
	 * Test publishing a modified form
	 * @throws Exception
	 */
	@Test
	public void testPublishModified() throws Exception {
		Form form = supply(t -> create(Form.class, f -> {
			t.setTimestamp(1000L);
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		consume(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(2000L);
			f.publish();
		}, form);

		execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(3000L);
			return update(f, upd -> {
				upd.setName("Modified Test Form");
			});
		}, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		FormModel formModel = execute(id -> getFormResource().get(id, null, null), Integer.toString(form.getId())).getItem();
		assertThat(formModel).as("Form").isOnline().isModified().hasVersion("1.1");

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true, response -> {
			// TODO add asserter
		});
	}

	/**
	 * Test time management
	 * @throws Exception
	 */
	@Test
	public void testPublishAt() throws Exception {
		// create form with timestamp 1
		Form form = supply(t -> create(Form.class, f -> {
			t.setTimestamp(1000L);
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish at timestamp 4
		consume(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(2000L);
			f.publish(4, false);
		}, form);

		// run publish process with timestamp 3
		try (Trx trx = new Trx()) {
			context.publish(3);
			trx.success();
		}

		// form must not be published
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);

		FormModel formModel = execute(id -> getFormResource().get(id, null, null), Integer.toString(form.getId())).getItem();
		assertThat(formModel).as("Form").isOffline().isPlannedOnline(4, "1.0");

		// run publish process with timestamp 5
		try (Trx trx = new Trx()) {
			context.publish(5);
			trx.success();
		}

		// form must be published
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);
	}

	/**
	 * Test taking a form offline.
	 * @implNote taking a form offline will not really take the node offline in Mesh, but will set the "online" flag to false
	 * @throws Exception
	 */
	@Test
	public void testTakeOffline() throws Exception {
		// create form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish form
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// take form offline
		consume(Form::takeOffline, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, false);
	}

	/**
	 * Test time management for taking offline
	 * @throws Exception
	 */
	@Test
	public void testTakeOfflineAt() throws Exception {
		// create form with timestamp 1
		Form form = supply(t -> create(Form.class, f -> {
			t.setTimestamp(1000L);
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		// publish at timestamp 2
		consume(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(2000L);
			f.publish();
		}, form);

		// run publish process at timestamp 3
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// take offline at timestamp 6
		consume(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4);
			f.takeOffline(6);
		}, form);

		FormModel formModel = execute(id -> getFormResource().get(id, null, null), Integer.toString(form.getId())).getItem();
		assertThat(formModel).as("Form").isOnline().isPlannedOffline(6);

		// run publish process at timestamp 5
		try (Trx trx = new Trx()) {
			context.publish(5);
			trx.success();
		}

		// form must still be online
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// run publish process at timestamp 7
		try (Trx trx = new Trx()) {
			context.publish(7);
			trx.success();
		}

		// form must still be offline now
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, false);
	}

	/**
	 * Test putting a form into the wastebin
	 * @throws Exception
	 */
	@Test
	public void testDeleteIntoWastebin() throws Exception {
		// create form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		// publish form
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		consume(Form::delete, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);
	}

	/**
	 * Test deleting a form
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		// create form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		// publish form
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		consume(f -> f.delete(true), form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);
	}

	/**
	 * Test deleting a form, after data has been posted
	 * @throws Exception
	 */
	@Test
	public void testDeleteWithData() throws Exception {
		if (!Strings.CI.equals(pluginName, "forms")) {
			return;
		}

		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		String meshUuid = execute(f -> MeshPublisher.getMeshUuid(f), form);
		String projectName = supply(() -> MeshPublisher.getMeshProjectName(node));

		// POST some data
		postFormData(projectName, pluginName, meshUuid, Pair.of("b0bac784_007f_4831_9098_987aa7828ff8", "Blubb"));

		// delete the form in the CMS
		consume(f -> f.delete(true), form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);
	}

	/**
	 * Test removing the english version of a form
	 * @throws Exception
	 */
	@Test
	public void testRemoveEnglishVersion() throws Exception {
		Form form = null;
		try (Trx trx = new Trx()) {
			trx.at(1);
			form = create(Form.class, f -> {
				f.setFormType(formType);
				f.setName("Test Form");
				f.setLanguages(Arrays.asList("de", "en"));
				f.setFolderId(node.getFolder().getId());
				f.setData(getFormData("form_data.json"));
			});
			trx.success();
		}
		consume(Form::publish, form);

		log.info("Run publish process to publish created form.");
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		log.info("Finished publish process for created form.");

		try (Trx trx = new Trx()) {
			trx.at(2);
			form = update(form, f -> {
				f.setLanguages(Arrays.asList("de"));
			});
			trx.success();
		}
		consume(Form::publish, form);

		log.info("Run publish process to publish updated form.");
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		log.info("Finished publish process for updated form.");

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true, response -> {
			// TODO assert
		});
	}

	/**
	 * Test removing the german version of a form
	 * @throws Exception
	 */
	@Test
	public void testRemoveGermanVersion() throws Exception {
		Form form = null;
		try (Trx trx = new Trx()) {
			trx.at(1);
			form = create(Form.class, f -> {
				f.setFormType(formType);
				f.setName("Test Form");
				f.setLanguages(Arrays.asList("de", "en"));
				f.setFolderId(node.getFolder().getId());
				f.setData(getFormData("form_data.json"));
			});
			trx.success();
		}
		consume(Form::publish, form);

		log.info("Run publish process to publish created form.");
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		log.info("Finished publish process for created form.");

		try (Trx trx = new Trx()) {
			trx.at(2);
			form = update(form, f -> {
				f.setLanguages(Arrays.asList("en"));
			});
			trx.success();
		}
		consume(Form::publish, form);

		log.info("Run publish process to publish updated form.");
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		log.info("Finished publish process for updated form.");

		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true, response -> {
			// TODO assert
		});
	}

	/**
	 * Test that publishing a form and taking offline a form will also dirt pages with references to the form
	 * @throws Exception
	 */
	@Test
	public void testPublishReference() throws Exception {
		Folder folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		// create form, but do not publish
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(folder.getId());
		}));

		// create and publish page
		Page page = supply(() -> update(createPage(folder, template, "Page"), upd -> {
			getPartType(CmsFormPartType.class, upd.getTag("form"), "form").setTarget(form);
		}));
		consume(p -> update(p, Page::publish), page);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// we expect "No Form" (form is offline)
		operate(() -> assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), page, true, nodeResponse -> {
			assertThat(nodeResponse.getFields().getStringField("content").getString()).isEqualTo("No Form");
		}));

		// publish form
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// we expect form rendered for page
		operate(() -> assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), page, true,
				nodeResponse -> {
					assertThat(nodeResponse.getFields().getStringField("content").getString())
							.isEqualTo(String.format("<eval>{ renderForm \"%s\" pluginName=\"%s\" language=data.node.language }</eval>",
									MeshPublisher.getMeshUuid(form), pluginName));
				}));

		// take form offline
		consume(Form::takeOffline, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// we expect form rendered for page
		operate(() -> assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), page, true, nodeResponse -> {
			assertThat(nodeResponse.getFields().getStringField("content").getString()).isEqualTo("No Form");
		}));
	}

	/**
	 * Test publishing the URL of a successPage
	 * @throws Exception
	 */
	@Test
	public void testPublishSuccessPage() throws Exception {
		if (!Strings.CI.equals(pluginName, "forms")) {
			return;
		}
		Folder folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		// create and publish page
		Page pageDe = supply(() -> createPage(folder, template, "Page", null, getLanguage("de")));
		consume(p -> update(p, Page::publish), pageDe);
		Page pageEn = supply(t -> {
			PageLoadResponse response = getPageResource().translate(pageDe.getId(), "en", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});
		consume(p -> update(p, Page::publish), pageEn);

		// create and publish form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(folder.getId());
			ObjectNode data = mapper.createObjectNode();
			data.put("successPageId", pageDe.getId());
			f.setData(data);
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPublishedForm(form, "form_successpage_de_en.json");

		// take german page variant offline
		consume(p -> update(p, Page::takeOffline), pageDe);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPublishedForm(form, "form_successpage_en.json");
	}

	/**
	 * Test publishing the URL of a successPage in a channel
	 * @throws Exception
	 */
	@Test
	public void testPublishSuccessPageChannel() throws Exception {
		if (!Strings.CI.equals(pluginName, "forms")) {
			return;
		}

		Node channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		Folder folder = supply(() -> createFolder(node.getFolder(), "Folder"));

		Folder localizedFolder = (Folder) supply(() -> TestedType.folder.localize(folder, channel));
		localizedFolder = update(localizedFolder, upd -> {
			upd.setPublishDir("/home_in_channel/");
		});

		// create and publish page
		Page pageDe = supply(() -> createPage(folder, template, "Page", null, getLanguage("de")));
		consume(p -> update(p, Page::publish), pageDe);
		Page pageEn = supply(t -> {
			PageLoadResponse response = getPageResource().translate(pageDe.getId(), "en", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});
		consume(p -> update(p, Page::publish), pageEn);

		// create and publish form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(folder.getId());
			ObjectNode data = mapper.createObjectNode()
				.put("successPageId", pageDe.getId())
				.put("successNodeId", channel.getId());
			f.setData(data);
		}));
		consume(Form::publish, form);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPublishedForm(form, "form_successpage_channel_de_en.json");
	}

	/**
	 * Test instant publishing
	 * @throws Exception
	 */
	@Test
	public void testInstantPublish() throws Exception {
		prepareForInstantPublishing();

		// create the form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);
	}

	/**
	 * Handle taking offline with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testInstantTakeOffline() throws Exception {
		prepareForInstantPublishing();

		// create the form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// take form offline
		consume(Form::takeOffline, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, false);
	}

	/**
	 * Handle taking offline with instant publishing, but instant delete disabled for the node
	 * @throws Exception
	 */
	@Test
	public void testInstantTakeOfflineDisabled() throws Exception {
		prepareForInstantPublishing();

		operate(() -> node.activateFeature(Feature.DISABLE_INSTANT_DELETE));

		// create form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// take form offline
		consume(Form::takeOffline, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);
	}

	/**
	 * Test putting a form into the wastebin with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testInstantDeleteIntoWastebin() throws Exception {
		prepareForInstantPublishing();

		// create form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// delete the form into the wastebine
		consume(Form::delete, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);
	}

	/**
	 * Test putting a form into the wastebin with instant publishing, but instant delete disabled for the node
	 * @throws Exception
	 */
	@Test
	public void testInstantDeleteIntoWastebinDisabled() throws Exception {
		prepareForInstantPublishing();
		operate(() -> node.activateFeature(Feature.DISABLE_INSTANT_DELETE));

		// create the form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// delete the form into the wastebin
		consume(Form::delete, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);
	}

	/**
	 * Test deleting a form with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testInstantDelete() throws Exception {
		prepareForInstantPublishing();

		// create the form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// delete the form
		consume(f -> f.delete(true), form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, false, null);
	}

	/**
	 * Test deleting a form with instant publishing, but with instant deleted disabled for the node
	 * @throws Exception
	 */
	@Test
	public void testInstantDeleteDisabled() throws Exception {
		prepareForInstantPublishing();
		operate(() -> node.activateFeature(Feature.DISABLE_INSTANT_DELETE));

		// create the form
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(getFormData("form_data.json"));
		}));

		// publish the form
		consume(Form::publish, form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);

		// delete the form
		consume(f -> f.delete(true), form);
		FormRESTUtils.assertForm(mesh.client(), meshProjectName, form, true, true);
	}
}
