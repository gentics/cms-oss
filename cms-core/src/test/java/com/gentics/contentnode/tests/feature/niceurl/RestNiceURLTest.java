package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for setting nice and alternate URLs for pages, files and images over the REST API
 */
@GCNFeature(set = { Feature.NICE_URLS })
public class RestNiceURLTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Page obstructor;

	private static String folderId;

	private static int templateId;

	private static String duplicateModifiedMessage;

	private static String duplicatePublishedMessage;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		obstructor = Builder.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Obstructor");
			p.setNiceUrl("/published/nice/url");
		}).at(1).publish().build();

		obstructor = Builder.update(obstructor, upd -> {
			upd.setNiceUrl("/modified/nice/url");
		}).at(2).build();

		folderId = execute(n -> Integer.toString(n.getFolder().getId()), node);
		templateId = execute(t -> t.getId(), template);

		duplicateModifiedMessage = String.format(
				"Es existiert bereits ein Objekt vom Typ Seite mit der URL /modified/nice/url:<br/> /dummyNode/Obstructor (ID: %d)",
				obstructor.getId());

		duplicatePublishedMessage = String.format(
				"Es existiert bereits ein Objekt vom Typ Seite, das mit der URL /published/nice/url veröffentlicht wurde:<br/> /dummyNode/Obstructor (ID: 82)",
				obstructor.getId());
	}

	protected static SortedSet<String> urls(String...urls) {
		return new TreeSet<>(Arrays.asList(urls));
	}

	@Test
	public void testCreatePage() throws NodeException {
		operate(() -> {
			PageCreateRequest request = new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId)
					.setNiceUrl("/new/page/nice").setAlternateUrls(urls("/new/page/alt3", "/new/page/alt2", "/new/page/alt1"));
			PageLoadResponse response = getPageResource().create(request);
			assertResponseOK(response);

			assertThat(response.getPage()).as("Created page").isNotNull().hasFieldOrPropertyWithValue("niceUrl",
					"/new/page/nice").hasFieldOrPropertyWithValue("alternateUrls", urls("/new/page/alt1", "/new/page/alt2", "/new/page/alt3"));
		});
	}

	@Test
	public void testCreatePageDuplicate() throws NodeException {
		operate(() -> {
			// check nice URL
			PageCreateRequest request = new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId)
					.setNiceUrl("/modified/nice/url");
			PageLoadResponse response = getPageResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while creating page: %s", duplicateModifiedMessage),
					new Message(Type.CRITICAL, duplicateModifiedMessage));

			// check alternate URL
			request = new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId)
					.setAlternateUrls(urls("/modified/nice/url"));
			response = getPageResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while creating page.",
					new Message(Type.CRITICAL, duplicateModifiedMessage));
		});
	}

	@Test
	public void testCreatePageDuplicatePublished() throws NodeException {
		operate(() -> {
			// check nice URL
			PageCreateRequest request = new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId)
					.setNiceUrl("/published/nice/url");
			PageLoadResponse response = getPageResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while creating page: %s", duplicatePublishedMessage),
					new Message(Type.CRITICAL, duplicatePublishedMessage));

			// check alternate URLs
			request = new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId)
					.setAlternateUrls(urls("/published/nice/url"));
			response = getPageResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while creating page.",
					new Message(Type.CRITICAL, duplicatePublishedMessage));
		});
	}

	@Test
	public void testUpdatePage() throws NodeException {
		com.gentics.contentnode.rest.model.Page page = supply(() -> {
			PageLoadResponse response = getPageResource().create(new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId));
			assertResponseOK(response);
			return response.getPage();
		});

		consume(p -> {
			p.setNiceUrl("/updated/page/nice");
			p.setAlternateUrls(urls("/updated/page/alt3", "/updated/page/alt2", "/updated/page/alt1"));
			PageSaveRequest request = new PageSaveRequest(p);

			GenericResponse response = getPageResource().save(Integer.toString(p.getId()), request);
			assertResponseOK(response);

			PageLoadResponse loadResponse = getPageResource().load(Integer.toString(p.getId()), false, false, false,
					false, false, false, false, false, false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getPage()).as("Updated page").isNotNull().hasFieldOrPropertyWithValue("niceUrl",
					"/updated/page/nice").hasFieldOrPropertyWithValue("alternateUrls", urls("/updated/page/alt1", "/updated/page/alt2", "/updated/page/alt3"));

			// set empty
			p.setNiceUrl("");
			p.setAlternateUrls(urls());

			response = getPageResource().save(Integer.toString(p.getId()), request);
			assertResponseOK(response);

			loadResponse = getPageResource().load(Integer.toString(p.getId()), false, false, false, false, false, false,
					false, false, false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getPage()).as("Updated page").isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", null).hasFieldOrPropertyWithValue("alternateUrls", urls());
		}, page);
	}

	@Test
	public void testUpdatePageDuplicate() throws NodeException {
		com.gentics.contentnode.rest.model.Page page = supply(() -> {
			PageLoadResponse response = getPageResource().create(new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId));
			assertResponseOK(response);
			return response.getPage();
		});

		consume(p -> {
			// check nice URL
			p.setNiceUrl("/modified/nice/url");
			GenericResponse response = getPageResource().save(Integer.toString(p.getId()), new PageSaveRequest(p));
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving page: %s", duplicateModifiedMessage),
					new Message(Type.CRITICAL, duplicateModifiedMessage));

			// check alternate URL
			p.setNiceUrl(null);
			p.setAlternateUrls(urls("/modified/nice/url"));
			response = getPageResource().save(Integer.toString(p.getId()), new PageSaveRequest(p));
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving page.",
					new Message(Type.CRITICAL, duplicateModifiedMessage));
		}, page);
	}

	@Test
	public void testUpdatePageDuplicatePublished() throws NodeException {
		com.gentics.contentnode.rest.model.Page page = supply(() -> {
			PageLoadResponse response = getPageResource().create(new PageCreateRequest().setFolderId(folderId).setTemplateId(templateId));
			assertResponseOK(response);
			return response.getPage();
		});

		consume(p -> {
			// check nice URL
			p.setNiceUrl("/published/nice/url");
			GenericResponse response = getPageResource().save(Integer.toString(p.getId()), new PageSaveRequest(p));
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving page: %s", duplicatePublishedMessage),
					new Message(Type.CRITICAL, duplicatePublishedMessage));

			// check alternate URL
			p.setNiceUrl(null);
			p.setAlternateUrls(urls("/published/nice/url"));
			response = getPageResource().save(Integer.toString(p.getId()), new PageSaveRequest(p));
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving page.",
					new Message(Type.CRITICAL, duplicatePublishedMessage));
		}, page);
	}

	@Test
	public void testUpdateFile() throws NodeException {
		File file = Builder.create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).build();

		consume(id -> {
			com.gentics.contentnode.rest.model.File restFile = new com.gentics.contentnode.rest.model.File();
			restFile.setNiceUrl("/updated/file/nice");
			restFile.setAlternateUrls(urls("/updated/file/alt3", "/updated/file/alt2", "/updated/file/alt1"));
			FileSaveRequest request = new FileSaveRequest();
			request.setFile(restFile);

			GenericResponse response = getFileResource().save(id, request);
			assertResponseOK(response);

			FileLoadResponse loadResponse = getFileResource().load(Integer.toString(id), false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getFile()).as("Updated file").isNotNull().hasFieldOrPropertyWithValue("niceUrl",
					"/updated/file/nice").hasFieldOrPropertyWithValue("alternateUrls", urls("/updated/file/alt1", "/updated/file/alt2", "/updated/file/alt3"));

			// set empty
			restFile.setNiceUrl("");
			restFile.setAlternateUrls(urls());

			response = getFileResource().save(id, request);
			assertResponseOK(response);

			loadResponse = getFileResource().load(Integer.toString(id), false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getFile()).as("Updated file").isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", null).hasFieldOrPropertyWithValue("alternateUrls", urls());
		}, file.getId());
	}

	@Test
	public void testUpdateFileDuplicate() throws NodeException {
		File file = Builder.create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).build();

		consume(id -> {
			com.gentics.contentnode.rest.model.File restFile = new com.gentics.contentnode.rest.model.File();
			FileSaveRequest request = new FileSaveRequest();
			request.setFile(restFile);

			// check nice URL
			restFile.setNiceUrl("/modified/nice/url");
			GenericResponse response = getFileResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving file: %s", duplicateModifiedMessage),
					new Message(Type.CRITICAL, duplicateModifiedMessage));

			// check alternate URL
			restFile.setNiceUrl(null);
			restFile.setAlternateUrls(urls("/modified/nice/url"));
			response = getFileResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving file.",
					new Message(Type.CRITICAL, duplicateModifiedMessage));
		}, file.getId());
	}

	@Test
	public void testUpdateFileDuplicatePublished() throws NodeException {
		File file = Builder.create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).build();

		consume(id -> {
			com.gentics.contentnode.rest.model.File restFile = new com.gentics.contentnode.rest.model.File();
			FileSaveRequest request = new FileSaveRequest();
			request.setFile(restFile);

			// check nice URL
			restFile.setNiceUrl("/published/nice/url");
			GenericResponse response = getFileResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving file: %s", duplicatePublishedMessage),
					new Message(Type.CRITICAL, duplicatePublishedMessage));

			// check alternate URL
			restFile.setNiceUrl(null);
			restFile.setAlternateUrls(urls("/published/nice/url"));
			response = getFileResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving file.",
					new Message(Type.CRITICAL, duplicatePublishedMessage));
		}, file.getId());
	}

	@Test
	public void testUpdateImage() throws NodeException {
		ImageFile image = Builder.create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		consume(id -> {
			Image restImage = new Image();
			restImage.setNiceUrl("/updated/image/nice");
			restImage.setAlternateUrls(urls("/updated/image/alt3", "/updated/image/alt2", "/updated/image/alt1"));
			ImageSaveRequest request = new ImageSaveRequest();
			request.setImage(restImage);

			GenericResponse response = getImageResource().save(id, request);
			assertResponseOK(response);

			ImageLoadResponse loadResponse = getImageResource().load(Integer.toString(id), false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getImage()).as("Updated image").isNotNull().hasFieldOrPropertyWithValue("niceUrl",
					"/updated/image/nice").hasFieldOrPropertyWithValue("alternateUrls", urls("/updated/image/alt1", "/updated/image/alt2", "/updated/image/alt3"));

			// set empty
			restImage.setNiceUrl("");
			restImage.setAlternateUrls(urls());

			response = getImageResource().save(id, request);
			assertResponseOK(response);

			loadResponse = getImageResource().load(Integer.toString(id), false, false, null, null);
			assertResponseOK(loadResponse);
			assertThat(loadResponse.getImage()).as("Updated image").isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", null).hasFieldOrPropertyWithValue("alternateUrls", urls());
		}, image.getId());
	}

	@Test
	public void testUpdateImageDuplicate() throws NodeException {
		ImageFile image = Builder.create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		consume(id -> {
			Image restImage = new Image();
			ImageSaveRequest request = new ImageSaveRequest();
			request.setImage(restImage);

			// check nice URL
			restImage.setNiceUrl("/modified/nice/url");
			GenericResponse response = getImageResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving image: %s", duplicateModifiedMessage),
					new Message(Type.CRITICAL, duplicateModifiedMessage));

			// check alternate URL
			restImage.setNiceUrl(null);
			restImage.setAlternateUrls(urls("/modified/nice/url"));
			response = getImageResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving image.",
					new Message(Type.CRITICAL, duplicateModifiedMessage));
		}, image.getId());
	}

	@Test
	public void testUpdateImageDuplicatePublished() throws NodeException {
		ImageFile image = Builder.create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		consume(id -> {
			Image restImage = new Image();
			ImageSaveRequest request = new ImageSaveRequest();
			request.setImage(restImage);

			// check nice URL
			restImage.setNiceUrl("/published/nice/url");
			GenericResponse response = getImageResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					String.format("Error while saving image: %s", duplicatePublishedMessage),
					new Message(Type.CRITICAL, duplicatePublishedMessage));

			// check alternate URL
			restImage.setNiceUrl(null);
			restImage.setAlternateUrls(urls("/published/nice/url"));
			response = getImageResource().save(id, request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving image.",
					new Message(Type.CRITICAL, duplicatePublishedMessage));
		}, image.getId());
	}
}
