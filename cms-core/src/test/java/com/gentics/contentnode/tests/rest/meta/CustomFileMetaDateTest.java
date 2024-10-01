package com.gentics.contentnode.tests.rest.meta;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import java.util.Optional;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.tests.rest.file.BinaryDataResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.RESTAppContext;

public class CustomFileMetaDateTest extends CustomMetaDateTest<com.gentics.contentnode.object.File, File, FileCreateRequest> {

	/**
	 * REST Application used as binary data provider
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));

	@Override
	public File updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate,
			Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
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

	@Override
	public File createMetaDated(int createTime, Optional<Consumer<FileCreateRequest>> maybeInflater) throws NodeException {
		File file = null;

		try (Trx trx = new Trx(systemUser)) {
			trx.at(createTime);

			FileCreateRequest request = new FileCreateRequest();
			request.setFolderId(node.getFolder().getId());
			request.setSourceURL(appContext.getBaseUri() + "binary");

			if (maybeInflater.isPresent()) {
				maybeInflater.get().accept(request);
			}
			FileUploadResponse response = ContentNodeRESTUtils.getFileResource().create(request);
			assertResponseCodeOk(response);
			file = response.getFile();

			trx.success();
		}

		return file;
	}

	/**
	 * Test sorting an overview by cdate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByCDate() throws NodeException {
		testSortOverviewByCDate(com.gentics.contentnode.object.File.class, "[CDate-File-100.plain, 100][CDate-File-300.plain, 300][CDate-File-400.plain, 600][CDate-File-200.plain, 800]");
	}

	/**
	 * Test sorting an overview by edate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByEDate() throws NodeException {
		testSortOverviewByEDate(com.gentics.contentnode.object.File.class, "[EDate-File-200.plain, 200][EDate-File-400.plain, 400][EDate-File-300.plain, 700][EDate-File-100.plain, 900]");
	}

	/**
	 * Load the page with ID
	 * @param pageId global or local ID
	 * @return page
	 * @throws NodeException
	 */
	protected File loadFile(String pageId) throws NodeException {
		return supply(() -> {
			FileLoadResponse response = ContentNodeRESTUtils.getFileResource().load(pageId, false, false, 0, null);
			assertResponseCodeOk(response);
			return response.getFile();
		});
	}

	@Override
	protected void updateName(FileCreateRequest model, String name) {
		model.setName(name);
	}
}
