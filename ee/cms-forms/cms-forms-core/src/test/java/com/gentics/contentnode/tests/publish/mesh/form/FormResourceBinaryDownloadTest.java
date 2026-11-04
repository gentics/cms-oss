package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.FormRESTUtils.getFormResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.GCNFeature;

import jakarta.ws.rs.core.StreamingOutput;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for downloading binaries of posted form data via
 * {@link com.gentics.contentnode.rest.resource.impl.FormResourceImpl#downloadDataBinaryField(String, String, String)} and
 * {@link com.gentics.contentnode.rest.resource.impl.FormResourceImpl#downloadDataMultivalueBinaryField(String, String, String, int)}.
 * These test cases require a Mesh instance with deployed forms-plugin
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.DISABLE_INSTANT_DELETE })
@Category(MeshTest.class)
public class FormResourceBinaryDownloadTest extends AbstractFormPublishTest {
	/**
	 * Name of the single value binary element in the form
	 */
	public final static String SINGLE_FIELD = "single_file";

	/**
	 * Name of the multivalue binary element in the form
	 */
	public final static String MULTI_FIELD = "multi_file";

	/**
	 * Name of the image resource used as uploaded binary
	 */
	public final static String IMAGE_NAME = "image-dpi66x44-res311x211.jpg";

	/**
	 * Name of the forms plugin
	 */
	protected String pluginName;

	/**
	 * Name of the mesh project
	 */
	protected String meshProjectName;

	/**
	 * Uploaded file
	 * @param name file name
	 * @param contentType content type
	 * @param data file content
	 */
	protected record Upload(String name, String contentType, byte[] data) {
	}

	@Before
	public void setup() throws NodeException {
		super.setup();
		operate(() -> {
			FormFactory.removeFormTypeFromNode(FormTypeConfigurationModifier.ANDP_TYPE, node.getId());
			FormFactory.addFormTypeToNode(FormTypeConfigurationModifier.GENERIC_TYPE, node.getId());
		});
		meshProjectName = supply(() -> MeshPublisher.getMeshProjectName(node));
		pluginName = supply(() -> FormConfigProvider.getConfig().get(FormTypeConfigurationModifier.GENERIC_TYPE).pluginName);
	}

	/**
	 * Test downloading the binary of a single value binary field
	 * @throws Exception
	 */
	@Test
	public void testDownloadBinaryField() throws Exception {
		Form form = createPublishedForm();
		Upload image = new Upload(IMAGE_NAME, "image/jpeg", readResource(IMAGE_NAME));

		String dataId = submitFiles(form, SINGLE_FIELD, false, List.of(image));

		jakarta.ws.rs.core.Response response = supply(
				() -> getFormResource().downloadDataBinaryField(Integer.toString(form.getId()), dataId, SINGLE_FIELD));

		assertDownload(response, image);
	}

	/**
	 * Test downloading the binaries of a multivalue binary field by index
	 * @throws Exception
	 */
	@Test
	public void testDownloadMultivalueBinaryField() throws Exception {
		Form form = createPublishedForm();
		Upload text = new Upload("first.txt", "text/plain", "First uploaded file".getBytes(StandardCharsets.UTF_8));
		Upload image = new Upload(IMAGE_NAME, "image/jpeg", readResource(IMAGE_NAME));

		String dataId = submitFiles(form, MULTI_FIELD, true, List.of(text, image));

		jakarta.ws.rs.core.Response first = supply(() -> getFormResource()
				.downloadDataMultivalueBinaryField(Integer.toString(form.getId()), dataId, MULTI_FIELD, 0));
		assertDownload(first, text);

		jakarta.ws.rs.core.Response second = supply(() -> getFormResource()
				.downloadDataMultivalueBinaryField(Integer.toString(form.getId()), dataId, MULTI_FIELD, 1));
		assertDownload(second, image);
	}

	/**
	 * Create a form with binary elements, publish it and run the publish process
	 * @return published form
	 * @throws Exception
	 */
	protected Form createPublishedForm() throws Exception {
		ObjectNode formData;
		try (InputStream in = getClass().getResourceAsStream("generic_form_data_binaries.json")) {
			formData = mapper.readValue(in, ObjectNode.class);
		}

		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Binary Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
			f.setData(formData);
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
	 * Submit the given files as values of a binary element of the form to the forms plugin
	 * @param form form
	 * @param fieldName name of the binary element
	 * @param list true if the element is a multivalue element
	 * @param uploads files to upload
	 * @return ID of the created form data
	 * @throws Exception
	 */
	protected String submitFiles(Form form, String fieldName, boolean list, List<Upload> uploads) throws Exception {
		String formUuid = execute(MeshPublisher::getMeshUuid, form);

		ObjectNode dataObject = mapper.createObjectNode();
		dataObject.put("formID", formUuid);
		dataObject.put("hasValidationErrors", false);
		ObjectNode property = dataObject.withObjectProperty("properties").withObjectProperty(fieldName);
		ArrayNode values = list ? property.putArray("values") : null;

		MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		for (int i = 0; i < uploads.size(); i++) {
			Upload upload = uploads.get(i);
			ObjectNode fileInfo = mapper.createObjectNode()
					.put("fileName", upload.name())
					.put("size", upload.data().length)
					.put("type", upload.contentType());
			String partName;
			if (list) {
				values.add(fileInfo);
				partName = "$.properties[\"%s\"].values[%d]".formatted(fieldName, i);
			} else {
				property.set("value", fileInfo);
				partName = "$.properties[\"%s\"].value".formatted(fieldName);
			}
			bodyBuilder.addFormDataPart(partName, upload.name(),
					RequestBody.create(MediaType.parse(upload.contentType()), upload.data()));
		}
		bodyBuilder.addFormDataPart("formdata", dataObject.toString());

		Request request = new Request.Builder()
				.url("http://" + mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080) + "/api/v1/"
						+ meshProjectName + "/plugins/" + pluginName + "/form/" + formUuid + "/submit?lang=en")
				.post(bodyBuilder.build())
				.build();
		try (Response response = okHttpClient.newCall(request).execute()) {
			String body = response.body().string();
			assertThat(response.isSuccessful()).as("POSTing data succeeded (response code is %d, body %s)", response.code(), body)
					.isTrue();
			String dataId = mapper.readValue(body, ObjectNode.class).path("formDataID").asText(null);
			assertThat(dataId).as("Form data ID in response %s", body).isNotBlank();
			return dataId;
		}
	}

	/**
	 * Assert that the response is a successful download of the given file
	 * @param response response
	 * @param expected expected file
	 * @throws IOException
	 */
	protected void assertDownload(jakarta.ws.rs.core.Response response, Upload expected) throws IOException {
		assertThat(response.getStatus()).as("Response status").isEqualTo(200);
		assertThat(response.getHeaderString("Content-Disposition")).as("Content-Disposition")
				.contains("filename=\"%s\"".formatted(expected.name()));
		assertThat(response.getHeaderString("Content-Type")).as("Content-Type").startsWith(expected.contentType());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		((StreamingOutput) response.getEntity()).write(out);
		assertThat(out.toByteArray()).as("Downloaded binary of " + expected.name()).isEqualTo(expected.data());
	}

	/**
	 * Read the resource with given name
	 * @param name resource name
	 * @return resource content
	 * @throws IOException
	 */
	protected byte[] readResource(String name) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(name)) {
			assertThat(in).as("Resource " + name).isNotNull();
			return IOUtils.toByteArray(in);
		}
	}
}
