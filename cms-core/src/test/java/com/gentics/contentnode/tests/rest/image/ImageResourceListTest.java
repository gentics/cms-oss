package com.gentics.contentnode.tests.rest.image;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Sorting and filtering tests for {@link ImageResource#list(InFolderParameterBean, FileListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)}
 */
@GCNFeature(set = { Feature.NICE_URLS })
public class ImageResourceListTest extends AbstractListSortAndFilterTest<Image> {
	private static Node node;

	private static List<String> pictureNames = Arrays.asList("uniqa.jpg", "bild2.jpg", "blume.jpg", "blume2.jpeg",
			"Cammarano.jpg", "dreamtime.jpg", "flower.gif", "Highlight-Bus.jpg", "Highlight-Bus-OK.jpg",
			"Highlight-Luftfahrt.jpeg", "image-dpi66x44-res311x211.jpg", "image-dpi66x44-res311x211.png",
			"image-dpi72x72-res430x180-imageio-bug.jpg", "image-dpi72x72-res3192x714-cmyk.jpg", "konzept.jpg",
			"Lenna.png", "Reference_force_blume2.jpeg", "Reference_force_flower.gif",
			"Reference_force_image-dpi72x72-res430x180-imageio-bug.jpg", "Reference_force_transparent_spider.png",
			"Reference_prop_blume2.jpeg", "Reference_prop_flower.gif", "Reference_prop_Highlight-Luftfahrt.jpeg",
			"Reference_prop_image-dpi72x72-res430x180-imageio-bug.jpg",
			"Reference_prop_image-dpi72x72-res3192x714-cmyk.jpg", "Reference_prop_transparent_spider.png",
			"Reference_simple_image-dpi72x72-res430x180-imageio-bug.jpg", "Reference_smart_blume2.jpeg",
			"Reference_smart_flower.gif", "Reference_smart_image-dpi72x72-res430x180-imageio-bug.jpg",
			"Reference_smart_testimg.bmp", "Reference_smart_transparent.gif", "Reference_smart_transparent_spider.png",
			"testimg.bmp", "transparent.gif", "transparent_spider.png");

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Image, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Image::getName),
				Pair.of("niceUrl", Image::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first()),
				Pair.of("fileSize", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getFileSize())),
				Pair.of("fileType", Image::getFileType)
		);
		List<Pair<String, Function<Image, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Image::getName),
				Pair.of("description", Image::getDescription),
				Pair.of("niceUrl", Image::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first())
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Image createItem() throws NodeException {
		return supply(() -> ModelBuilder.getImage(Builder.create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(randomStringGenerator.generate(5, 10));
			f.setDescription(randomStringGenerator.generate(10, 20));
			f.setFileStream(GenericTestUtils.getPictureResource(getRandomEntry(pictureNames)));
			f.setNiceUrl(randomStringGenerator.generate(5, 10));
			f.setAlternateUrls(randomStringGenerator.generate(5, 10));
		}).build(), Collections.emptyList()));
	}

	@Override
	protected AbstractListResponse<Image> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ImageResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())),
				new FileListParameterBean(), filter, sort, paging, new EditableParameterBean(),
				new WastebinParameterBean());
	}
}
