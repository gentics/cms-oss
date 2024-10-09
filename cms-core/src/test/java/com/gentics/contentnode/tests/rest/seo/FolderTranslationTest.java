package com.gentics.contentnode.tests.rest.seo;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ContentLanguageTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for translating folder names, descriptions, pub_dirs
 */
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT })
public class FolderTranslationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node masterNode;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		masterNode = supply(() -> update(
				createNode("hostname", "Node name", PublishTarget.NONE, getLanguage("de"), getLanguage("en"), getLanguage("fr")), upd -> {
					upd.setPublishDir("");
					upd.getFolder().setPublishDir("/");
				}));
		template = supply(() -> createTemplate(masterNode.getFolder(), "Template"));
	}

	/**
	 * Create a map containing the given keys and values
	 * @param keyAndValue array of keys and values
	 * @return map
	 */
	public static Map<String, String> map(String... keyAndValue) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < keyAndValue.length; i += 2) {
			map.put(keyAndValue[i], keyAndValue[i + 1]);
		}
		return map;
	}

	@Before
	public void setup() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(false);
		});
	}

	/**
	 * Clean data
	 * @throws NodeException
	 */
	@After
	public void clean() throws NodeException {
		operate(() -> clear(masterNode));
	}

	/**
	 * Test creating a folder without translations
	 * @throws NodeException
	 */
	@Test
	public void testCreateNoTranslation() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setDescription("Generic description");
			request.setPublishDir("pub/dir");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertThat(folder).as("Created folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("nameI18n", Collections.emptyMap())
			.hasFieldOrPropertyWithValue("description", "Generic description")
			.hasFieldOrPropertyWithValue("descriptionI18n", Collections.emptyMap())
			.hasFieldOrPropertyWithValue("publishDir", "/pub/dir/")
			.hasFieldOrPropertyWithValue("publishDirI18n", Collections.emptyMap());
	}

	/**
	 * Test creating a folder with translations
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			request.setDescription("Generic Description");
			request.setDescriptionI18n(map("de", "Beschreibung auf Deutsch", "fr", "Description en français"));
			request.setPublishDir("pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertThat(folder).as("Created folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("nameI18n", map("de", "Name auf Deutsch", "en", "Name in English"))
			.hasFieldOrPropertyWithValue("description", "Generic Description")
			.hasFieldOrPropertyWithValue("descriptionI18n", map("de", "Beschreibung auf Deutsch", "fr", "Description en français"))
			.hasFieldOrPropertyWithValue("publishDir", "/pub/dir/")
			.hasFieldOrPropertyWithValue("publishDirI18n", map("de", "/veroe/verz/"));
	}

	/**
	 * Test creating a folder with a translation for a name, which is made unique, because it is already used
	 * @throws NodeException
	 */
	@Test
	public void testCreateMakeNameUnique() throws NodeException {
		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder otherFolder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Other Name in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertThat(otherFolder).as("Created folder")
			.hasFieldOrPropertyWithValue("name", "Other Generic Name")
			.hasFieldOrPropertyWithValue("nameI18n", map("de", "Name auf Deutsch1", "en", "Other Name in English"));
	}

	/**
	 * Test creating a folder with a translation for a name, which is already used
	 * @throws NodeException
	 */
	@Test
	public void testCreateNameDuplicate() throws NodeException {
		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Other Name in English"));
			request.setFailOnDuplicate(true);
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while creating folder: Der Name \"Name auf Deutsch\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet.",
					new Message(Type.CRITICAL, "Der Name \"Name auf Deutsch\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet."));
		});
	}

	/**
	 * Test updating the translated name
	 * @throws NodeException
	 */
	@Test
	public void testUpdateName() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			request.setDescription("Generic Description");
			request.setDescriptionI18n(map("de", "Beschreibung auf Deutsch", "en", "Description in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder updated = execute(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("en", "Updated Name in English", "fr", "Nom en français"));
			request.setFolder(update);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);

			return getFolderResource().load(Integer.toString(id), false, false, false, null, null).getFolder();
		}, folder.getId());

		assertThat(updated).as("Updated Folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("nameI18n", map("en", "Updated Name in English", "fr", "Nom en français"))
			.hasFieldOrPropertyWithValue("description", "Generic Description")
			.hasFieldOrPropertyWithValue("descriptionI18n", map("de", "Beschreibung auf Deutsch", "en", "Description in English"));
	}

	/**
	 * Test making the translated name unique while updating
	 * @throws NodeException
	 */
	@Test
	public void testUpdateMakeNameUnique() throws NodeException {
		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("fr", "Nom en français"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder updated = execute(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("en", "Name in English", "fr", "Nom en français"));
			request.setFolder(update);
			request.setFailOnDuplicate(false);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);

			return getFolderResource().load(Integer.toString(id), false, false, false, null, null).getFolder();
		}, folder.getId());

		assertThat(updated).as("Updated Folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("nameI18n", map("en", "Name in English1", "fr", "Nom en français"));
	}

	/**
	 * Test failed update of translated name due to duplicate
	 * @throws NodeException
	 */
	@Test
	public void testUpdateNameDuplicate() throws NodeException {
		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setNameI18n(map("de", "Name auf Deutsch", "en", "Name in English"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("fr", "Nom en français"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("en", "Name in English", "fr", "Nom en français"));
			request.setFolder(update);
			request.setFailOnDuplicate(true);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving folder " + id
					+ ": Der Name \"Name in English\" wird bereits vom Ordner \"/Node name/Other Generic Name\" verwendet.",
					new Message(Type.CRITICAL,
							"Der Name \"Name in English\" wird bereits vom Ordner \"/Node name/Other Generic Name\" verwendet."));
		}, folder.getId());
	}

	/**
	 * Test creating a folder with name translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testCreateNameInUnsupportedLanguage() throws NodeException {
		operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setNameI18n(map("it", "Nome in italiano"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while creating folder: Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		});
	}

	/**
	 * Test updating a folder with name translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testUpdateNameInUnsupportedLanguage() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("it", "Nome in italiano"));
			request.setFolder(update);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while saving folder " + id
							+ ": Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		}, folder.getId());
	}

	/**
	 * Test creating a folder with a translation for a pub_dir segment, which is made unique, because it is already used
	 * @throws NodeException
	 */
	@Test
	public void testCreateMakePubdirSegmentUnique() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder otherFolder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setPublishDir("other/pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertThat(otherFolder).as("Created folder")
			.hasFieldOrPropertyWithValue("name", "Other Generic Name")
			.hasFieldOrPropertyWithValue("publishDir", "other-pub-dir")
			.hasFieldOrPropertyWithValue("publishDirI18n", map("de", "veroe-verz_1"));
	}

	/**
	 * Test creating a folder with a translation for a pub_dir segment, which is already used
	 * @throws NodeException
	 */
	@Test
	public void testCreatepubdirSegmentDuplicate() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setPublishDir("other/pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			request.setFailOnDuplicate(true);
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while creating folder: Das Verzeichnissegment \"veroe-verz\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet.",
					new Message(Type.CRITICAL, "Das Verzeichnissegment \"veroe-verz\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet."));
		});
	}

	/**
	 * Test updating the translated pub_dir segment
	 * @throws NodeException
	 */
	@Test
	public void testUpdatePubdirSegment() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("pub/dir");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder updated = execute(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setPublishDirI18n(map("en", "publish/directory", "fr", "publier/directoire"));
			request.setFolder(update);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);

			return getFolderResource().load(Integer.toString(id), false, false, false, null, null).getFolder();
		}, folder.getId());

		assertThat(updated).as("Updated Folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("publishDirI18n", map("en", "publish-directory", "fr", "publier-directoire"));
	}

	/**
	 * Test making the translated pub_dir segment unique while updating
	 * @throws NodeException
	 */
	@Test
	public void testUpdateMakePubdirSegmentUnique() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setPublishDirI18n(map("en", "publish/directory", "fr", "publier/directoire"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder updated = execute(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setPublishDirI18n(map("fr", "publier/directoire", "de", "verö/verz"));
			request.setFolder(update);
			request.setFailOnDuplicate(false);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);

			return getFolderResource().load(Integer.toString(id), false, false, false, null, null).getFolder();
		}, folder.getId());

		assertThat(updated).as("Updated Folder")
			.hasFieldOrPropertyWithValue("name", "Generic Name")
			.hasFieldOrPropertyWithValue("publishDirI18n", map("fr", "publier-directoire_1", "de", "veroe-verz"));
	}

	/**
	 * Test failed update of translated pub_dir segment due to duplicate
	 * @throws NodeException
	 */
	@Test
	public void testUpdatePubdirSegmentDuplicate() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Other Generic Name");
			request.setPublishDirI18n(map("en", "publish/directory", "fr", "publier/directoire"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDirI18n(map("de", "verö/verz"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setPublishDirI18n(map("fr", "publier/directoire", "de", "verö/verz"));
			request.setFolder(update);
			request.setFailOnDuplicate(true);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving folder " + id
					+ ": Das Verzeichnissegment \"publier-directoire\" wird bereits vom Ordner \"/Node name/Other Generic Name\" verwendet.",
					new Message(Type.CRITICAL,
							"Das Verzeichnissegment \"publier-directoire\" wird bereits vom Ordner \"/Node name/Other Generic Name\" verwendet."));
		}, folder.getId());
	}

	/**
	 * Test creating a folder with description translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testCreateDescriptionInUnsupportedLanguage() throws NodeException {
		operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setDescriptionI18n(map("it", "Descrizione in italiano"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while creating folder: Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		});
	}

	/**
	 * Test updating a folder with description translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testUpdateDescriptionInUnsupportedLanguage() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setDescriptionI18n(map("it", "Descrizione in italiano"));
			request.setFolder(update);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while saving folder " + id
							+ ": Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		}, folder.getId());
	}

	/**
	 * Test creating a folder with publish directory translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testCreatePublishDirInUnsupportedLanguage() throws NodeException {
		operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDirI18n(map("it", "dir/delle/pub"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while creating folder: Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		});
	}

	/**
	 * Test updating a folder with publish directory translated in an unsupported language
	 * @throws NodeException
	 */
	@Test
	public void testUpdatePublishDirInUnsupportedLanguage() throws NodeException {
		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setPublishDirI18n(map("it", "dir/delle/pub"));
			request.setFolder(update);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA,
					"Error while saving folder " + id
							+ ": Die Sprache Italiano ist dem Node Node name nicht zugewiesen.",
					new Message(Type.CRITICAL, "Die Sprache Italiano ist dem Node Node name nicht zugewiesen."));
		}, folder.getId());
	}

	/**
	 * Test creating a folder with the name translated to german, but the publish dir segment not set
	 * @throws NodeException
	 */
	@Test
	public void testCreateTranslationWithEmptyPubDirSegment() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("path");
			request.setNameI18n(map("de", "Name auf Deutsch"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertDifferentPubDirs(folder);
	}

	/**
	 * Test creating a folder with the name translated to german, requesting the same publish dir segment
	 * @throws NodeException
	 */
	@Test
	public void testCreateTranslationWithSamePubDirSegment() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("path");
			request.setNameI18n(map("de", "Name auf Deutsch"));
			request.setPublishDirI18n(map("de", "path"));
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});

		assertDifferentPubDirs(folder);
	}

	/**
	 * Test adding a translation to a folder with the name translated to german, but the publish dir segment not set
	 * @throws NodeException
	 */
	@Test
	public void testAddTranslationWithEmptyPubDirSegment() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("path");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});
		assertDifferentPubDirs(folder);

		// add translation (without explicit request for a publish dir segment)
		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("de", "Name auf Deutsch"));
			request.setFolder(update);
			request.setFailOnDuplicate(true);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving folder " + id
					+ ": Das Verzeichnissegment \"path\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet.",
					new Message(Type.CRITICAL,
							"Das Verzeichnissegment \"path\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet."));
		}, folder.getId());
		assertDifferentPubDirs(folder);

		// add translation (without explicit request for a publish dir segment)
		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("de", "Name auf Deutsch"));
			request.setFolder(update);
			request.setFailOnDuplicate(false);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);
		}, folder.getId());
		assertDifferentPubDirs(folder);
	}

	/**
	 * Test adding a translation to a folder with the name translated to german, requesting the same publish dir segment
	 * @throws NodeException
	 */
	@Test
	public void testAddTranslationWithSamePubDirSegment() throws NodeException {
		masterNode = update(masterNode, upd -> {
			upd.setPubDirSegment(true);
		});

		Folder folder = supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(Integer.toString(masterNode.getFolder().getId()));
			request.setName("Generic Name");
			request.setPublishDir("path");
			FolderLoadResponse response = getFolderResource().create(request);
			assertResponseOK(response);
			return response.getFolder();
		});
		assertDifferentPubDirs(folder);

		// add translation and let it fail on duplicates
		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("de", "Name auf Deutsch"));
			update.setPublishDirI18n(map("de", "path"));
			request.setFolder(update);
			request.setFailOnDuplicate(true);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponse(response, ResponseCode.INVALIDDATA, "Error while saving folder " + id
					+ ": Das Verzeichnissegment \"path\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet.",
					new Message(Type.CRITICAL,
							"Das Verzeichnissegment \"path\" wird bereits vom Ordner \"/Node name/Generic Name\" verwendet."));
		}, folder.getId());
		assertDifferentPubDirs(folder);

		// add translation and let it make the pub dir segment unique
		consume(id -> {
			FolderSaveRequest request = new FolderSaveRequest();
			Folder update = new Folder();
			update.setNameI18n(map("de", "Name auf Deutsch"));
			update.setPublishDirI18n(map("de", "path"));
			request.setFolder(update);
			request.setFailOnDuplicate(false);
			GenericResponse response = getFolderResource().save(Integer.toString(id), request);
			assertResponseOK(response);
		}, folder.getId());
		assertDifferentPubDirs(folder);
	}

	/**
	 * Assert that the folder does have different pub_dirs for all existing languages
	 * @param folder folder
	 * @throws NodeException
	 */
	protected void assertDifferentPubDirs(Folder folder) throws NodeException {
		// now render the pub_dir of the folder in all available languages and assert that the pub_dir's are all different
		consume(id -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.Folder nodeFolder = t.getObject(com.gentics.contentnode.object.Folder.class, id);
			Set<String> languages = new HashSet<>();
			languages.add(MeshPublisher.getMeshLanguage(nodeFolder));
			languages.addAll(MeshPublisher.getAlternativeMeshLanguages(nodeFolder));

			List<String> pubDirs = new ArrayList<>();
			for (String lang : languages) {
				try (RenderTypeTrx rTrx = RenderTypeTrx.publish(); ContentLanguageTrx langTrx = new ContentLanguageTrx(lang)) {
					pubDirs.add(ObjectTransformer.getString(nodeFolder.get("pub_dir"), null));
				}
			}

			Condition<String> notNull = new Condition<>(s -> s != null, "not null");
			assertThat(pubDirs).as("Translated pub_dirs").are(notNull).doesNotHaveDuplicates();
		}, folder.getId());
	}
}
