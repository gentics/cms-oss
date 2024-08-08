package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import java.util.Optional;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.tests.rest.file.BinaryDataResource;
import com.gentics.contentnode.testutils.RESTAppContext;

public class CustomFileMetaDateTest extends CustomMetaDateTest<com.gentics.contentnode.object.File, File> {

	/**
	 * REST Application used as binary data provider
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));

	@Override
	public File createMetaDated(int createTime) throws NodeException {
		File file = null;

		try (Trx trx = new Trx()) {
			trx.at(createTime);

			FileCreateRequest request = new FileCreateRequest();
			request.setFolderId(node.getFolder().getId());
			request.setSourceURL(appContext.getBaseUri() + "binary");

			FileUploadResponse response = new FileResourceImpl().create(request);
			assertResponseCodeOk(response);
			file = response.getFile();

			trx.success();
		}

		return file;
	}

	@Override
	public File updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate,
			Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException {
		try (Trx trx = new Trx()) {
			trx.at(updateTime);

			File update = new File();
			FileSaveRequest request = new FileSaveRequest();
			request.setFile(update);
			maybeDate.ifPresent(cdate -> request.getFile().setCdate(cdate));
			maybeEDate.ifPresent(edate -> request.getFile().setEdate(edate));
			maybeCustomCDate.ifPresent(cdate -> request.getFile().setCustomCdate(cdate));
			maybeCustomEDate.ifPresent(edate -> request.getFile().setCustomEdate(edate));
			GenericResponse response = new FileResourceImpl().save(id, request);
			assertResponseCodeOk(response);

			trx.success();
		}

		return loadFile(String.valueOf(id));
	}
	/**
	 * Load the page with ID
	 * @param pageId global or local ID
	 * @return page
	 * @throws NodeException
	 */
	protected File loadFile(String pageId) throws NodeException {
		return supply(() -> {
			FileLoadResponse response = new FileResourceImpl().load(pageId, false, false, 0, null);
			assertResponseCodeOk(response);
			return response.getFile();
		});
	}
}
