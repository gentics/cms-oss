package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.client.Entity;

import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.FileCopyRequest;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;

@GCNFeature(set = { Feature.NICE_URLS, Feature.MULTICHANNELLING })
public class FileResourceTest {

	private static final String TEST_USER = "tester";

	private static DBTestContext testContext = new DBTestContext();
	private static RESTAppContext restContext = new RESTAppContext(RESTAppContext.Type.jetty);
	@ClassRule
	public static RESTAppContext binarySourceContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static Node node;
	private static Node channel;
	private static Folder folder;
	private static Folder targetFolder;
	private static UserGroup testGroup;
	private static SystemUser testUser;
	private static SystemUser systemUser;

	private static final Map<String, Object> localStorage = new HashMap<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode("Master", "Master", ContentNodeTestDataUtils.PublishTarget.NONE, ContentNodeTestDataUtils.getLanguage("de"), ContentNodeTestDataUtils.getLanguage("en")));
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "testfolder"));
		targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "targetfolder"));
		testGroup = Trx.supply(() -> ContentNodeTestDataUtils.createUserGroup("Testgroup", ContentNodeTestDataUtils.NODE_GROUP_ID));

		Trx.operate(t -> {
			Folder f = t.getObject(folder, true);

			f.setPublishDir("/testfolder/");
			f.save();

			f = t.getObject(targetFolder, true);
			f.setPublishDir("/targetfolder/");
			f.save();
		});

		testUser = Trx.supply(() -> ContentNodeTestDataUtils.createSystemUser("tester", "tester", null, TEST_USER, TEST_USER, Arrays.asList(testGroup)));
		systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Test
	public void testCopyInSameFolder() throws NodeException {
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile contents".getBytes()));
		com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
		FileCopyRequest copyRequest = new FileCopyRequest();

		fileModel.setId(file.getId());
		fileModel.setName(file.getName());
		copyRequest.setFile(fileModel);

		FileUploadResponse response = ContentNodeTestUtils.assertRequiredPermissions(
			testGroup,
			TEST_USER,
			TEST_USER,
			restContext,
			target -> target.path("file").path("copy").request().post(Entity.json(copyRequest), FileUploadResponse.class),
			ContentNodeTestUtils.perm(folder, PermType.read),
			ContentNodeTestUtils.perm(folder, PermType.readitems),
			ContentNodeTestUtils.perm(folder, PermType.createitems));
		com.gentics.contentnode.rest.model.File responseFile = response.getFile();

		Assert.assertEquals(responseFile.getChannelId(), 0);
		Assert.assertEquals(responseFile.getMasterNodeId(), node.getId());
		Assert.assertEquals(responseFile.getName(), "testfile_1.txt");

		Trx.operate(() -> file.delete(true));
		Trx.operate(t -> t.getObject(File.class, responseFile.getId(), true).delete(true));
	}

	@Test
	public void testCopyInTargetFolder() throws NodeException {
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile contents".getBytes()));
		com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
		FileCopyRequest copyRequest = new FileCopyRequest();

		fileModel.setId(file.getId());
		copyRequest.setFile(fileModel);
		copyRequest.setNewFilename("new-filename.txt");
		copyRequest.setTargetFolder(new TargetFolder(targetFolder.getId(), node.getId()));

		FileUploadResponse response = ContentNodeTestUtils.assertRequiredPermissions(
			testGroup,
			TEST_USER,
			TEST_USER,
			restContext,
			target -> target.path("file").path("copy").request().post(Entity.json(copyRequest), FileUploadResponse.class),
			ContentNodeTestUtils.perm(folder, PermType.read),
			ContentNodeTestUtils.perm(folder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.read),
			ContentNodeTestUtils.perm(targetFolder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.createitems));

		com.gentics.contentnode.rest.model.File responseFile = response.getFile();

		Assert.assertEquals(responseFile.getChannelId(), 0);
		Assert.assertEquals(responseFile.getMasterNodeId(), node.getId());
		Assert.assertEquals(responseFile.getName(), "new-filename.txt");

		Trx.operate(t -> t.getObject(File.class, file.getId(), true).delete(true));
		Trx.operate(t -> t.getObject(File.class, responseFile.getId(), true).delete(true));
	}

	@Test
	public void testCopyInTargetFolderInChannel() throws NodeException {
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile contents".getBytes()));
		com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
		FileCopyRequest copyRequest = new FileCopyRequest();

		fileModel.setId(file.getId());
		fileModel.setName(file.getName());
		copyRequest.setFile(fileModel);
		copyRequest.setTargetFolder(new TargetFolder(targetFolder.getId(), channel.getId()));

		FileUploadResponse response = ContentNodeTestUtils.assertRequiredPermissions(
			testGroup,
			TEST_USER,
			TEST_USER,
			restContext,
			target -> target.path("file").path("copy").request().post(Entity.json(copyRequest), FileUploadResponse.class),
			ContentNodeTestUtils.perm(folder, PermType.read),
			ContentNodeTestUtils.perm(folder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.read),
			ContentNodeTestUtils.perm(targetFolder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.createitems));

		com.gentics.contentnode.rest.model.File responseFile = response.getFile();

		Assert.assertEquals(responseFile.getChannelId(), channel.getId());
		Assert.assertEquals(responseFile.getMasterNodeId(), channel.getId());
		Assert.assertEquals(responseFile.getName(), "testfile.txt");


		Trx.operate(() -> file.delete(true));
		Trx.operate(t -> t.getObject(File.class, responseFile.getId(), true).delete(true));
	}

	/**
	 * Check that nice URL, alternate URLS and additional "copyright" property is set as object property.
	 */
	@Test
	public void testCreateWithProperties() throws NodeException {
		String objPropName = "copyright";
		String objPropValue = "Copyright";

		FileUploadResponse response = createWithProperties(objPropName, objPropValue);

		Map<String, Tag> tags = response.getFile().getTags();

		assertThat(tags)
			.as("File tags")
			.hasSize(1);

		String objPropKey = "object." + objPropName;

		assertThat(tags.containsKey(objPropKey))
			.as("File has object property " + objPropName)
			.isTrue();

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName + " is active")
			.matches(Tag::getActive, "is active");

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName)
			.matches(tag -> tag.getProperties().get("text").getStringValue().equals(objPropValue), "Object property value is \"Copyright\"");

		Trx.operate(t -> t.getObject(File.class, response.getFile().getId(), true).delete(true));
	}
	@Test
	public void testCopyWithPropertiesSameFolder() throws NodeException {
		String objPropName = "copyright";
		String objPropValue = "Copyright";

		FileUploadResponse response = createWithProperties(objPropName, objPropValue);
		FileCopyRequest copyRequest = new FileCopyRequest();
		com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
		com.gentics.contentnode.rest.model.File file = response.getFile();

		fileModel.setId(file.getId());
		fileModel.setName(file.getName());
		copyRequest.setFile(fileModel);

		response = ContentNodeTestUtils.assertRequiredPermissions(
			testGroup,
			TEST_USER,
			TEST_USER,
			restContext,
			target -> target.path("file").path("copy").request().post(Entity.json(copyRequest), FileUploadResponse.class),
			ContentNodeTestUtils.perm(folder, PermType.read),
			ContentNodeTestUtils.perm(folder, PermType.readitems),
			ContentNodeTestUtils.perm(folder, PermType.createitems));
		com.gentics.contentnode.rest.model.File responseFile = response.getFile();

		Assert.assertEquals(responseFile.getChannelId(), 0);
		Assert.assertEquals(responseFile.getMasterNodeId(), node.getId());
		Assert.assertEquals(responseFile.getName(), "testfile_1.txt");

		Map<String, Tag> tags = responseFile.getTags();

		assertThat(tags)
			.as("File tags")
			.hasSize(1);

		String objPropKey = "object." + objPropName;

		assertThat(tags.containsKey(objPropKey))
			.as("File has object property " + objPropName)
			.isTrue();

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName + " is active")
			.matches(Tag::getActive, "is active");

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName)
			.matches(tag -> tag.getProperties().get("text").getStringValue().equals(objPropValue), "Object property value is \"Copyright\"");

		Trx.operate(t -> t.getObject(File.class, file.getId(), true).delete(true));
		Trx.operate(t -> t.getObject(File.class, responseFile.getId(), true).delete(true));
	}

	@Test
	public void testCopyWithPropertiesTargetFolder() throws NodeException {
		String objPropName = "copyright";
		String objPropValue = "Copyright";

		FileUploadResponse response = createWithProperties(objPropName, objPropValue);
		FileCopyRequest copyRequest = new FileCopyRequest();
		com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
		com.gentics.contentnode.rest.model.File file = response.getFile();

		fileModel.setId(file.getId());
		copyRequest.setFile(fileModel);
		copyRequest.setNewFilename("new-filename-obj.txt");
		copyRequest.setTargetFolder(new TargetFolder(targetFolder.getId(), node.getId()));

		response = ContentNodeTestUtils.assertRequiredPermissions(
			testGroup,
			TEST_USER,
			TEST_USER,
			restContext,
			target -> target.path("file").path("copy").request().post(Entity.json(copyRequest), FileUploadResponse.class),
			ContentNodeTestUtils.perm(folder, PermType.read),
			ContentNodeTestUtils.perm(folder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.read),
			ContentNodeTestUtils.perm(targetFolder, PermType.readitems),
			ContentNodeTestUtils.perm(targetFolder, PermType.createitems));

		com.gentics.contentnode.rest.model.File responseFile = response.getFile();

		Assert.assertEquals(responseFile.getChannelId(), 0);
		Assert.assertEquals(responseFile.getMasterNodeId(), node.getId());
		Assert.assertEquals(responseFile.getName(), "new-filename-obj.txt");

		Map<String, Tag> tags = responseFile.getTags();

		assertThat(tags)
			.as("File tags")
			.hasSize(1);

		String objPropKey = "object." + objPropName;

		assertThat(tags.containsKey(objPropKey))
			.as("File has object property " + objPropName)
			.isTrue();

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName + " is active")
			.matches(Tag::getActive, "is active");

		assertThat(tags.get(objPropKey))
			.as("Object property " + objPropName)
			.matches(tag -> tag.getProperties().get("text").getStringValue().equals(objPropValue), "Object property value is \"Copyright\"");

		Trx.operate(t -> t.getObject(File.class, file.getId(), true).delete(true));
		Trx.operate(t -> t.getObject(File.class, responseFile.getId(), true).delete(true));
	}

	/**
	 * Create a file from a URL with additional properties.
	 * @param filename The filename of the new file.
	 * @param properties The properties to set (needs existing object properties with the corresponding keynames).
	 * @return The response for the created file.
	 */
	private FileUploadResponse createFromUrl(String filename, String niceUrl, Set<String> alternateUrls, Pair<String, String>...properties) throws NodeException {
		FileCreateRequest request = new FileCreateRequest();

		request.setFolderId(folder.getId());
		request.setName(filename);
		request.setNodeId(node.getId());
		request.setOverwriteExisting(false);
		request.setSourceURL(binarySourceContext.getBaseUri() + "binary");
		request.setNiceURL(niceUrl);
		request.setAlternateURLs(alternateUrls);

		for (Pair<String, String> property : properties) {
			request.getProperties().put(property.getKey(), property.getValue());
		}

		return getFileResource().create(request);
	}

	private FileUploadResponse createWithProperties(String objPropName, String objPropValue) throws NodeException {
		String hex = Long.toHexString(System.currentTimeMillis());
		String niceUrl = "/nice/testfile" + hex + ".txt";
		Set<String> alternateUrls = new HashSet<>(Arrays.asList("/foo/testfile" + hex + ".txt", "/bar/testfile" + hex + ".txt"));
		Integer constructId = Trx.supply(() ->
			ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "text_short", "text"));
		ObjectTagDefinition def = (ObjectTagDefinition) localStorage.computeIfAbsent(objPropName, key -> {
			try {
				return Trx.supply(() ->
					ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, constructId, objPropValue, objPropName));
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		});
		FileUploadResponse response = Trx.supply(systemUser, () -> createFromUrl("testfile.txt", niceUrl, alternateUrls, Pair.of(objPropName, objPropValue)));

		ContentNodeRESTUtils.assertResponseOK(response);

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			assertThat(response.getFile().getNiceUrl())
				.as("Saved nice URL")
				.isEqualTo(niceUrl);
	
			assertThat(response.getFile().getAlternateUrls())
				.as("Saved alternate URLs")
				.contains(alternateUrls.toArray(new String[0]));
		}
		return response;
	}
}
