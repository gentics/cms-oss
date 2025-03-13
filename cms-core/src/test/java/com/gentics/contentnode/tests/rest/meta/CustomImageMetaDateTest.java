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
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.tests.rest.file.BinaryDataImageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.RESTAppContext;

public class CustomImageMetaDateTest extends CustomMetaDateTest<com.gentics.contentnode.object.ImageFile, Image, FileCreateRequest> {

	/**
	 * REST Application used as binary data provider
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataImageResource.class).build()));

	@Override
	public Image createMetaDated(int createTime, Optional<Consumer<FileCreateRequest>> maybeInflater) throws NodeException {
		Image file = null;
		GenericResponse response;
		try (Trx trx = new Trx(systemUser)) {
			trx.at(createTime);

			FileCreateRequest request = new FileCreateRequest();
			request.setFolderId(node.getFolder().getId());
			request.setName(BinaryDataImageResource.ImageType.JPG.filename());
			request.setSourceURL(appContext.getBaseUri() + "binary");
			if (maybeInflater.isPresent()) {
				maybeInflater.get().accept(request);
			}

			response = ContentNodeRESTUtils.getFileResource().create(request);
			assertResponseCodeOk(response);
			trx.success();
		}
		try (Trx trx = new Trx(systemUser)) {
			response = ContentNodeRESTUtils.getImageResource().load(String.valueOf(((FileUploadResponse) response).getFile().getId()), false, false, null, null);
			assertResponseCodeOk(response);
			file = ((ImageLoadResponse) response).getImage();

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
		testSortOverviewByCDate(com.gentics.contentnode.object.ImageFile.class, "[CDate-ImageFile-100.jpg, 100][CDate-ImageFile-300.jpg, 300][CDate-ImageFile-400.jpg, 600][CDate-ImageFile-200.jpg, 800]");
	}

	/**
	 * Test sorting an overview by edate
	 * @throws NodeException
	 */
	@Test
	public void testSortOverviewByEDate() throws NodeException {
		testSortOverviewByEDate(com.gentics.contentnode.object.ImageFile.class, "[EDate-ImageFile-200.jpg, 200][EDate-ImageFile-400.jpg, 400][EDate-ImageFile-300.jpg, 700][EDate-ImageFile-100.jpg, 900]");
	}

	@Override
	public Image updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate,
			Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			trx.at(updateTime);

			Image update = new Image();
			ImageSaveRequest request = new ImageSaveRequest();
			request.setImage(update);
			maybeDate.ifPresent(cdate -> request.getImage().setCdate(cdate));
			maybeEDate.ifPresent(edate -> request.getImage().setEdate(edate));
			maybeCustomCDate.ifPresent(cdate -> request.getImage().setCustomCdate(cdate));
			maybeCustomEDate.ifPresent(edate -> request.getImage().setCustomEdate(edate));
			GenericResponse response = new ImageResourceImpl().save(id, request);
			assertResponseCodeOk(response);

			trx.success();
		}

		return loadImage(String.valueOf(id));
	}

	/**
	 * Load the page with ID
	 * @param pageId global or local ID
	 * @return page
	 * @throws NodeException
	 */
	protected Image loadImage(String pageId) throws NodeException {
		return supply(() -> {
			ImageLoadResponse response = ContentNodeRESTUtils.getImageResource().load(pageId, false, false, 0, null);
			assertResponseCodeOk(response);
			return response.getImage();
		});
	}


	@Override
	protected void updateName(FileCreateRequest model, String name) {
		model.setName(name);
	}
}
