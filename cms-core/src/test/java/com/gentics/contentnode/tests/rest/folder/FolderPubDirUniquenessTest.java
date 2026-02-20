package com.gentics.contentnode.tests.rest.folder;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createRestFileUploadMultiPart;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createformDataBodyPart;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Tests for uniqueness of paths between folders and other objects (pages/files)
 */
@GCNFeature(set = Feature.PUB_DIR_SEGMENT)
@RunWith(value = Parameterized.class)
public class FolderPubDirUniquenessTest {
	protected final static String CONFLICT_NAME = "confl.ict";
	protected final static String NO_CONFLICT_NAME = "no.test.confl.ict";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;
	private static SystemUser systemUser;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: pubDirSegments {0}, language {1}, on {2}, faileOnDuplicate {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean pubDirSegments : Arrays.asList(true, false)) {
			for (String language : Arrays.asList(null, "de", "en")) {
				for (TestType testType : TestType.values()) {
					for (boolean failOnDuplicate : Arrays.asList(true, false)) {
						data.add(new Object[] { pubDirSegments, language, testType, failOnDuplicate });
					}
				}
			}
		}
//		data.add(new Object[] { true, "de", TestType.create, false });
		return data;
	}

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("test.node.hostname");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
			n.setOmitPageExtension(true);
			n.getLanguages().add(getLanguage("de"));
			n.getLanguages().add(getLanguage("en"));
		}).build();

		template = create(Template.class, t -> {
			t.setName("Template");
			t.setFolderId(node.getFolder().getId());
		}).build();

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Parameter(0)
	public boolean pubDirSegments;

	@Parameter(1)
	public String language;

	@Parameter(2)
	public TestType testType;

	@Parameter(3)
	public boolean failOnDuplicate;

	protected String pubDir;

	protected I18nMap pubDirI18n;

	protected String noConflictPubDir;

	protected I18nMap noConflictPubDirI18n;

	@Before
	public void setup() throws NodeException {
		node = update(node, n -> {
			n.setPubDirSegment(pubDirSegments);
		}).build();

		pubDir = StringUtils.isEmpty(language) ? CONFLICT_NAME : "no.confl.ict.default";
		pubDirI18n = new I18nMap();
		operate(() -> {
			pubDirI18n.put("de", Strings.CI.equals(language, "de") ? CONFLICT_NAME : "no.confl.ict.de");
			pubDirI18n.put("en", Strings.CI.equals(language, "en") ? CONFLICT_NAME : "no.confl.ict.en");
		});

		noConflictPubDir = "no.confl.ict.default";
		operate(() -> {
			noConflictPubDirI18n = new I18nMap().put("de", "no.confl.ict.de").put("en", "no.confl.ict.en");
		});
	}

	@After
	public void clean() throws NodeException {
		operate(() -> {
			clear(node);
		});
	}

	/**
	 * Test creating a page with filename equal to a folder pub dir segment
	 * @throws NodeException
	 */
	@Test
	public void testPageFilenameWithFolderPubDir() throws NodeException {
		Folder folder = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder");
			create.setPublishDir(pubDir);
			create.setPublishDirI18n(pubDirI18n);
		}).build();

		PageLoadResponse pageResponse = supply(() -> {
			PageCreateRequest createRequest = new PageCreateRequest();
			createRequest.setFolderId(String.valueOf(node.getFolder().getId()));
			createRequest.setTemplateId(template.getId());
			createRequest.setFileName(testType == TestType.create ? CONFLICT_NAME : NO_CONFLICT_NAME);
			createRequest.setFailOnDuplicate(failOnDuplicate);
			return getPageResource().create(createRequest);
		});

		if (pubDirSegments && testType == TestType.create) {
			if (failOnDuplicate) {
				String message = execute(
						f -> I18NHelper.get("error.filename.exists.folder", CONFLICT_NAME, I18NHelper.getPath(f)), folder);
				assertResponse(pageResponse, ResponseCode.INVALIDDATA, "Error while creating page: %s".formatted(message),
						new Message().setType(Type.CRITICAL).setMessage(message));
			} else {
				assertResponseOK(pageResponse);	
				assertThat(pageResponse.getPage().getFileName()).as("Page filename").isNotEqualTo(CONFLICT_NAME);
			}
		} else {
			assertResponseOK(pageResponse);
		}

		if (testType == TestType.update) {
			GenericResponse saveResponse = execute(page -> {
				PageSaveRequest updateRequest = new PageSaveRequest(page);
				page.setFileName(CONFLICT_NAME);
				updateRequest.setFailOnDuplicate(failOnDuplicate);
				return getPageResource().save(Integer.toString(page.getId()), updateRequest);
			}, pageResponse.getPage());

			if (pubDirSegments) {
				if (failOnDuplicate) {
					String message = execute(
							f -> I18NHelper.get("error.filename.exists.folder", CONFLICT_NAME, I18NHelper.getPath(f)), folder);
					assertResponse(saveResponse, ResponseCode.INVALIDDATA, "Error while saving page %d: %s".formatted(pageResponse.getPage().getId(), message),
							new Message().setType(Type.CRITICAL).setMessage(message));
				} else {
					assertResponseOK(saveResponse);
					PageLoadResponse loadResponse = execute(page -> {
						return new PageResourceImpl().load(page.getGlobalId(), false, false, false, false, false, false,
								false, false, false, false, null, null);
					}, pageResponse.getPage());
					assertThat(loadResponse.getPage().getFileName()).as("Page filename").isNotEqualTo(CONFLICT_NAME);
				}
			} else {
				assertResponseOK(saveResponse);
			}
		}
	}

	/**
	 * Test creating a folder with pub dir segment equal to a page filename
	 * @throws NodeException
	 */
	@Test
	public void testFolderPubDirWithPageFilename() throws NodeException {
		Page page = create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			create.setTemplateId(template.getId());
			create.setFilename(CONFLICT_NAME);
			create.setName("Conflicting Page");
		}).build();

		FolderLoadResponse folderResponse = supply(() -> {
			FolderCreateRequest createRequest = new FolderCreateRequest();
			createRequest.setMotherId(String.valueOf(node.getFolder().getId()));
			createRequest.setName("Folder");
			createRequest.setPublishDir(testType == TestType.create ? pubDir : noConflictPubDir);
			createRequest.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(testType == TestType.create ? pubDirI18n : noConflictPubDirI18n));
			createRequest.setFailOnDuplicate(failOnDuplicate);
			return getFolderResource().create(createRequest);
		});

		if (pubDirSegments && testType == TestType.create) {
			if (failOnDuplicate) {
				String message = execute(p -> I18NHelper.get("error.pubdir.exists.page", CONFLICT_NAME, I18NHelper.getPath(p)), page);
				assertResponse(folderResponse, ResponseCode.INVALIDDATA, "Error while creating folder: %s".formatted(message),
						new Message().setType(Type.CRITICAL).setMessage(message));
			} else {
				assertResponseOK(folderResponse);
				assertThat(folderResponse.getFolder().getPublishDir()).as("Folder pubdir").isNotEqualTo(CONFLICT_NAME);
				assertThat(folderResponse.getFolder().getPublishDirI18n().get("en")).as("Folder pubdir in en").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
				assertThat(folderResponse.getFolder().getPublishDirI18n().get("de")).as("Folder pubdir in de").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
			}
		} else {
			assertResponseOK(folderResponse);
		}

		if (testType == TestType.update) {
			GenericResponse saveResponse = execute(folder -> {
				FolderSaveRequest updateRequest = new FolderSaveRequest();
				updateRequest.setFailOnDuplicate(failOnDuplicate);
				updateRequest.setFolder(folder);
				folder.setPublishDir(pubDir);
				folder.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(pubDirI18n));
				return getFolderResource().save(folder.getGlobalId(), updateRequest);
			}, folderResponse.getFolder());

			if (pubDirSegments) {
				if (failOnDuplicate) {
					String message = execute(p -> I18NHelper.get("error.pubdir.exists.page", CONFLICT_NAME, I18NHelper.getPath(p)), page);
					assertResponse(saveResponse, ResponseCode.INVALIDDATA, "Error while saving folder %s: %s".formatted(folderResponse.getFolder().getGlobalId(), message),
							new Message().setType(Type.CRITICAL).setMessage(message));
				} else {
					assertResponseOK(saveResponse);
					FolderLoadResponse loadResponse = execute(folder -> {
						return getFolderResource().load(folder.getGlobalId(), false, false, false, null, null);
					}, folderResponse.getFolder());
					assertThat(loadResponse.getFolder().getPublishDir()).as("Folder pubdir").isNotEqualTo(CONFLICT_NAME);
					assertThat(loadResponse.getFolder().getPublishDirI18n().get("en")).as("Folder pubdir in en").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
					assertThat(loadResponse.getFolder().getPublishDirI18n().get("de")).as("Folder pubdir in de").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
				}
			} else {
				assertResponseOK(saveResponse);
			}
		}
	}

	@Test
	public void testFileFilenameWithFolderPubDir() throws NodeException {
		Folder folder = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder");
			create.setPublishDir(pubDir);
			create.setPublishDirI18n(pubDirI18n);
		}).build();

		FileUploadResponse fileResponse = supply(systemUser, () -> {
			try {
				MultiPart request = createRestFileUploadMultiPart(
						testType == TestType.create ? CONFLICT_NAME : NO_CONFLICT_NAME, node.getFolder().getId(),
						node.getId(), "", false, "contents");
				request.bodyPart(createformDataBodyPart("form-data; name=\"failOnDuplicate\"",
						(failOnDuplicate ? "true" : "false"), null));

				return getFileResource().create(request);
			} catch (ParseException e) {
				throw new NodeException(e);
			}
		});

		if (pubDirSegments && testType == TestType.create) {
			if (failOnDuplicate) {
				String message = execute(
						f -> I18NHelper.get("error.filename.exists.folder", CONFLICT_NAME, I18NHelper.getPath(f)), folder);
				assertResponse(fileResponse, ResponseCode.INVALIDDATA, "Error while creating file: %s".formatted(message),
						new Message().setType(Type.CRITICAL).setMessage(message));
			} else {
				assertResponseOK(fileResponse);
				assertThat(fileResponse.getFile().getName()).as("File name").isNotEqualTo(CONFLICT_NAME);
			}
		} else {
			assertResponseOK(fileResponse);
		}

		if (testType == TestType.update) {
			GenericResponse saveResponse = execute(file -> {
				FileSaveRequest updateRequest = new FileSaveRequest();
				file.setName(CONFLICT_NAME);
				updateRequest.setFile(file);
				updateRequest.setFailOnDuplicate(failOnDuplicate);
				return getFileResource().save(file.getId(), updateRequest);
			}, fileResponse.getFile());

			if (pubDirSegments) {
				if (failOnDuplicate) {
					String message = execute(
							f -> I18NHelper.get("error.filename.exists.folder", CONFLICT_NAME, I18NHelper.getPath(f)), folder);
					assertResponse(saveResponse, ResponseCode.INVALIDDATA, "Error while saving file %d: %s".formatted(fileResponse.getFile().getId(), message),
							new Message().setType(Type.CRITICAL).setMessage(message));
				} else {
					assertResponseOK(saveResponse);
					FileLoadResponse loadResponse = execute(file -> {
						return getFileResource().load(file.getGlobalId(), false, false, null, null);
					}, fileResponse.getFile());
					assertThat(loadResponse.getFile().getName()).as("File name").isNotEqualTo(CONFLICT_NAME);
				}
			} else {
				assertResponseOK(saveResponse);
			}
		}
	}

	@Test
	public void testFolderPubDirWithFileFilename() throws NodeException, IOException{
		File file;
		try (InputStream in = new ByteArrayInputStream("contents".getBytes())) {
			file = create(File.class, create -> {
				create.setFolder(node, node.getFolder());
				create.setName(CONFLICT_NAME);
				create.setFileStream(in);
			}).build();
		}

		FolderLoadResponse folderResponse = supply(() -> {
			FolderCreateRequest createRequest = new FolderCreateRequest();
			createRequest.setMotherId(String.valueOf(node.getFolder().getId()));
			createRequest.setName("Folder");
			createRequest.setPublishDir(testType == TestType.create ? pubDir : noConflictPubDir);
			createRequest.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(testType == TestType.create ? pubDirI18n : noConflictPubDirI18n));
			createRequest.setFailOnDuplicate(failOnDuplicate);
			return getFolderResource().create(createRequest);
		});

		if (pubDirSegments && testType == TestType.create) {
			if (failOnDuplicate) {
				String message = execute(f -> I18NHelper.get("error.pubdir.exists.file", CONFLICT_NAME, I18NHelper.getPath(f)), file);
				assertResponse(folderResponse, ResponseCode.INVALIDDATA, "Error while creating folder: %s".formatted(message),
						new Message().setType(Type.CRITICAL).setMessage(message));
			} else {
				assertResponseOK(folderResponse);
				assertThat(folderResponse.getFolder().getPublishDir()).as("Folder pubdir").isNotEqualTo(CONFLICT_NAME);
				assertThat(folderResponse.getFolder().getPublishDirI18n().get("en")).as("Folder pubdir in en").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
				assertThat(folderResponse.getFolder().getPublishDirI18n().get("de")).as("Folder pubdir in de").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
			}
		} else {
			assertResponseOK(folderResponse);
		}

		if (testType == TestType.update) {
			GenericResponse saveResponse = execute(folder -> {
				FolderSaveRequest updateRequest = new FolderSaveRequest();
				updateRequest.setFailOnDuplicate(failOnDuplicate);
				updateRequest.setFolder(folder);
				folder.setPublishDir(pubDir);
				folder.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(pubDirI18n));
				return getFolderResource().save(folder.getGlobalId(), updateRequest);
			}, folderResponse.getFolder());

			if (pubDirSegments) {
				if (failOnDuplicate) {
					String message = execute(p -> I18NHelper.get("error.pubdir.exists.file", CONFLICT_NAME, I18NHelper.getPath(p)), file);
					assertResponse(saveResponse, ResponseCode.INVALIDDATA, "Error while saving folder %s: %s".formatted(folderResponse.getFolder().getGlobalId(), message),
							new Message().setType(Type.CRITICAL).setMessage(message));
				} else {
					assertResponseOK(saveResponse);
					FolderLoadResponse loadResponse = execute(folder -> {
						return getFolderResource().load(folder.getGlobalId(), false, false, false, null, null);
					}, folderResponse.getFolder());
					assertThat(loadResponse.getFolder().getPublishDir()).as("Folder pubdir").isNotEqualTo(CONFLICT_NAME);
					assertThat(loadResponse.getFolder().getPublishDirI18n().get("en")).as("Folder pubdir in en").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
					assertThat(loadResponse.getFolder().getPublishDirI18n().get("de")).as("Folder pubdir in de").isNotEmpty().isNotEqualTo(CONFLICT_NAME);
				}
			} else {
				assertResponseOK(saveResponse);
			}
		}
	}

	/**
	 * Test type
	 */
	protected static enum TestType {
		/**
		 * Object is created
		 */
		create,

		/**
		 * Object is updated
		 */
		update
	}
}
