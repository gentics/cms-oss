package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.runtime.ConfigurationValue;

import jakarta.ws.rs.core.Response.Status;

/**
 * Collection of help methods for all sorts of content staging tests
 */
public interface ContentStagingMethods {
	/**
	 * Make clean.
	 * <ol>
	 * <li>Delete all content-packages</li>
	 * <li>Delete all nodes and channels</li>
	 * <li>Delete all templates</li>
	 * <li>Delete all object properties</li>
	 * <li>Delete all constructs</li>
	 * <li>Delete all contentrepositories</li>
	 * </ol>
	 * @throws NodeException
	 * @throws IOException
	 */
	default void makeClean() throws NodeException, IOException {
		File folder = new File(ConfigurationValue.CONTENT_PACKAGES_PATH.get());
		if (folder.exists()) {
			FileUtils.deleteDirectory(folder);
		}
		folder.mkdirs();

		// delete all existing nodes (with their content), ordered by id descending to find and delete the channels before their master nodes
		List<Integer> nodeIds = supply(t -> DBUtils.select("SELECT id FROM node ORDER BY id DESC", DBUtils.IDLIST));
		for (int id : nodeIds) {
			operate(t -> {
				Node node = t.getObject(Node.class, id);
				node.delete(true);
			});
		}

		// delete all templates
		Set<Integer> templateIds = supply(() -> DBUtils.select("SELECT id FROM template", DBUtils.IDS));
		for (int id : templateIds) {
			operate(t -> {
				Template template = t.getObject(Template.class, id);
				template.delete(true);
			});
		}

		// delete all object tag definitions
		Set<Integer> objTagIds = supply(() -> DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS));
		for (int id : objTagIds) {
			operate(t -> {
				ObjectTagDefinition objectTagDefinition = t.getObject(ObjectTagDefinition.class, id);
				objectTagDefinition.delete(true);
			});
		}

		// delete all constructs
		Set<Integer> constructIds = supply(t -> DBUtils.select("SELECT id FROM construct", DBUtils.IDS));
		for (int id : constructIds) {
			operate(t -> {
				Construct construct = t.getObject(Construct.class, id);
				construct.delete(true);
			});
		}

		// delete all CRs
		for (int id : supply(t -> DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDLIST))) {
			operate(t -> {
				ContentRepository cr = t.getObject(ContentRepository.class, id);
				cr.delete(true);
			});
		}
	}

	/**
	 * Create a content package with the given name
	 * @param name name
	 * @throws NodeException
	 */
	default void createContentPackage(String name) throws NodeException {
		StagedContentPackage contentPackage = new StagedContentPackage();
		contentPackage.setName(name);
		ContentPackageLoadResponse response = new ContentStagingResourceImpl().create(contentPackage);
		assertResponseOK(response);
	}

	/**
	 * Import the content package with given name to the CMS
	 * @param name name
	 * @throws NodeException
	 */
	default void importContentPackage(String name) throws NodeException {
		GenericResponse response = new ContentStagingResourceImpl().importIntoCms(name, 0, false);
		assertResponseOK(response);
	}

	/**
	 * Import the content package with given name to the CMS, assert that this fails with the given error
	 * @param name name
	 * @param status expected HTTP response status
	 * @param responseCode expected response code
	 * @param responseMessage expected response message
	 * @param userMessages expected user messages
	 * @throws NodeException
	 */
	default void tryImportContentPackage(String name, Status status, ResponseCode responseCode, String responseMessage, Message... userMessages) throws NodeException {
		try {
			new ContentStagingResourceImpl().importIntoCms(name, 0, false);
			fail("Import of content package " + name + " was expected to fail");
		} catch (RestMappedException e) {
			assertThat(e.getStatus()).as("Response status").isEqualTo(status);
			// the response must be generated within a transaction, so that the messages are translated into the user's language
			assertResponse(supply(() -> e.getRestResponse()), responseCode, responseMessage, userMessages);
		}
	}

	/**
	 * Add the given object to the content package not recursively
	 * @param packageName name of the content package
	 * @param object object to add
	 * @throws NodeException
	 */
	default void addObjectToContentPackage(String packageName, StageableNodeObject object) throws NodeException {
		addObjectToContentPackage(packageName, object, false);
	}

	/**
	 * Add the given object to the content package
	 * @param packageName name of the content package
	 * @param object object to add
	 * @param recursive true to add recursively
	 * @throws NodeException
	 */
	default void addObjectToContentPackage(String packageName, StageableNodeObject object, boolean recursive) throws NodeException {
		GlobalId objectGlobalId = execute(NodeObject::getGlobalId, object);
		String type = StringUtils.removeStart(object.getSuffix(), ".");
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId.toString(), recursive));
	}
}
