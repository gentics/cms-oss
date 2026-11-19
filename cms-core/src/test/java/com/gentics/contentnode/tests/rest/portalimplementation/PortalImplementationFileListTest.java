package com.gentics.contentnode.tests.rest.portalimplementation;

import static com.gentics.contentnode.factory.Trx.supply;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.rest.model.PortalImplementationFileModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.PortalImplementationResource;
import com.gentics.contentnode.rest.resource.impl.PortalImplementationResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PortalImplementationResource#listFiles(String, FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class PortalImplementationFileListTest extends AbstractListSortAndFilterTest<PortalImplementationFileModel> {
	/**
	 * Counter to make paths unique
	 */
	private static AtomicInteger counter = new AtomicInteger();

	/**
	 * Folders, in which the files are created
	 */
	private static List<String> folders = Arrays.asList("", "src", "src/lib", "assets");

	/**
	 * Mimetypes of the files
	 */
	private static List<String> mimetypes = Arrays.asList("application/x-httpd-php", "text/css", "text/plain", "image/png");

	/**
	 * ID of the portal implementation containing the files
	 */
	private static Integer portalImplementationId;

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<PortalImplementationFileModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", file -> AbstractListSortAndFilterTest.addLeadingZeros(file.getId())),
				Pair.of("globalId", PortalImplementationFileModel::getGlobalId),
				Pair.of("path", PortalImplementationFileModel::getPath),
				Pair.of("name", PortalImplementationFileModel::getName),
				Pair.of("folder", PortalImplementationFileModel::getFolder),
				Pair.of("mimetype", PortalImplementationFileModel::getMimetype)
		);
		List<Pair<String, Function<PortalImplementationFileModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", file -> AbstractListSortAndFilterTest.addLeadingZeros(file.getId())),
				Pair.of("globalId", PortalImplementationFileModel::getGlobalId),
				Pair.of("path", PortalImplementationFileModel::getPath),
				Pair.of("name", PortalImplementationFileModel::getName),
				Pair.of("folder", PortalImplementationFileModel::getFolder),
				Pair.of("mimetype", PortalImplementationFileModel::getMimetype),
				Pair.of("filesize", file -> AbstractListSortAndFilterTest.addLeadingZeros((int) file.getFilesize())),
				Pair.of("cdate", file -> AbstractListSortAndFilterTest.addLeadingZeros(file.getCdate())),
				Pair.of("edate", file -> AbstractListSortAndFilterTest.addLeadingZeros(file.getEdate()))
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected PortalImplementationFileModel createItem() throws NodeException {
		if (portalImplementationId == null) {
			portalImplementationId = supply(() -> Builder.create(PortalImplementation.class, impl -> {
				impl.setKeyword("filelist");
				impl.setName("File List");
				impl.setType(PortalImplementationModel.Type.PHP);
			}).build().getId());
		}

		int index = counter.incrementAndGet();
		String folder = getRandomEntry(folders);
		String name = randomStringGenerator.generate(5, 10) + "_" + index + ".txt";
		byte[] contents = randomStringGenerator.generate(1, 100).getBytes();

		return supply(() -> PortalImplementationFile.TRANSFORM2REST.apply(Builder.create(PortalImplementationFile.class, file -> {
			file.setPortalImplementationId(portalImplementationId);
			file.setPath(folder.isEmpty() ? name : folder + "/" + name);
			file.setMimetype(getRandomEntry(mimetypes));
			file.setContents(new ByteArrayInputStream(contents));
		}).build()));
	}

	@Override
	protected AbstractListResponse<PortalImplementationFileModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		try {
			return new PortalImplementationResourceImpl().listFiles(Integer.toString(portalImplementationId), filter, sort, paging);
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}
}
