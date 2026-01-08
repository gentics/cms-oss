package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for content staging of overviews
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS })
@RunWith(value = Parameterized.class)
public class OverviewStagingTest extends AbstractContentStagingTest<Page> {
	private final static String packageName = "TestPackage";

	private Construct overviewConstruct;

	private Page overviewPage;

	private String overviewTagName;

	private String overviewPageGlobalId;

	private Folder folderOne;

	private Folder folderOneInFolderOne;

	private Folder folderTwoInFolderOne;

	private Page pageOneInFolderOne;

	private Page pageTwoInFolderOne;

	private File fileOneInFolderOne;

	private File fileTwoInFolderOne;

	private ImageFile imageOneInFolderOne;

	private ImageFile imageTwoInFolderOne;

	private Folder folderTwo;

	private Folder folderOneInFolderTwo;

	private Folder folderTwoInFolderTwo;

	private Page pageOneInFolderTwo;

	private Page pageTwoInFolderTwo;

	private File fileOneInFolderTwo;

	private File fileTwoInFolderTwo;

	private ImageFile imageOneInFolderTwo;

	private ImageFile imageTwoInFolderTwo;

	@Parameters(name = "{index}: list {0}, select {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (ListType listType : Arrays.asList(ListType.FILE, ListType.FOLDER, ListType.IMAGE, ListType.PAGE)) {
			for (SelectType selectType : Arrays.asList(SelectType.AUTO, SelectType.FOLDER, SelectType.MANUAL)) {
				data.add(new Object[] { listType, selectType });
			}
		}

		return data;
	}

	@Parameter(0)
	public ListType listType;

	@Parameter(1)
	public SelectType selectType;

	/**
	 * Create test instance
	 */
	public OverviewStagingTest() {
		super(Page.class, "page");
	}

	@SuppressWarnings("incomplete-switch")
	@Before
	public void setup() throws NodeException, IOException {
		super.setup();

		overviewConstruct = create(Construct.class, c -> {
			c.setName("Overview Construct", 1);
			c.setKeyword("overviewconstruct");
			c.setAutoEnable(true);
			List<Part> parts = c.getParts();
			// overview
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("overview");
				p.setPartTypeId(getPartTypeId(OverviewPartType.class));
			}).doNotSave().build());
		}).build();

		folderOne = create(Folder.class, create -> {
			create.setFolder(node, nodeRootFolder);
			create.setName("Folder One");
		}).build();

		folderOneInFolderOne = create(Folder.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("Folder One in Folder One");
		}).build();

		folderTwoInFolderOne = create(Folder.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("Folder Two in Folder One");
		}).build();

		pageOneInFolderOne = create(Page.class, create -> {
			create.setFolder(node, folderOne);
			create.setTemplateId(template.getId());
			create.setName("Page One in Folder One");
		}).build();

		pageTwoInFolderOne = create(Page.class, create -> {
			create.setFolder(node, folderOne);
			create.setTemplateId(template.getId());
			create.setName("Page Two in Folder One");
		}).build();

		fileOneInFolderOne = create(File.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("file-one-in-folder-one.txt");
			create.setFileStream(IOUtils.toInputStream("Contents", Charset.forName("UTF8")));
		}).build();

		fileTwoInFolderOne = create(File.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("file-two-in-folder-one.txt");
			create.setFileStream(IOUtils.toInputStream("Contents", Charset.forName("UTF8")));
		}).build();

		imageOneInFolderOne = create(ImageFile.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("image-one-in-folder-one.jpg");
			create.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		imageTwoInFolderOne = create(ImageFile.class, create -> {
			create.setFolder(node, folderOne);
			create.setName("image-two-in-folder-one.jpg");
			create.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		folderTwo = create(Folder.class, create -> {
			create.setFolder(node, nodeRootFolder);
			create.setName("Folder Two");
		}).build();

		folderOneInFolderTwo = create(Folder.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("Folder One in Folder Two");
		}).build();

		folderTwoInFolderTwo = create(Folder.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("Folder Two in Folder Two");
		}).build();

		pageOneInFolderTwo = create(Page.class, create -> {
			create.setFolder(node, folderTwo);
			create.setTemplateId(template.getId());
			create.setName("Page One in Folder Two");
		}).build();

		pageTwoInFolderTwo =create(Page.class, create -> {
			create.setFolder(node, folderTwo);
			create.setTemplateId(template.getId());
			create.setName("Page Two in Folder Two");
		}).build();

		fileOneInFolderTwo = create(File.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("file-one-in-folder-two.txt");
			create.setFileStream(IOUtils.toInputStream("Contents", Charset.forName("UTF8")));
		}).build();

		fileTwoInFolderTwo = create(File.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("file-two-in-folder-two.txt");
			create.setFileStream(IOUtils.toInputStream("Contents", Charset.forName("UTF8")));
		}).build();

		imageOneInFolderTwo = create(ImageFile.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("image-one-in-folder-two.jpg");
			create.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		imageTwoInFolderTwo = create(ImageFile.class, create -> {
			create.setFolder(node, folderTwo);
			create.setName("image-two-in-folder-two.jpg");
			create.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();

		overviewPage = create(Page.class, create -> {
			create.setFolder(node, nodeRootFolder);
			create.setTemplateId(template.getId());
			create.setName("Overview Page");

			ContentTag overviewTag = create.getContent().addContentTag(overviewConstruct.getId());
			overviewTagName = overviewTag.getName();

			Overview overview = new Overview();
			overview.setListType(listType);
			overview.setSelectType(selectType);
			overview.setOrderBy(Overview.OrderBy.ALPHABETICALLY);
			overview.setOrderDirection(Overview.OrderDirection.ASC);
			overview.setMaxItems(3);
			overview.setRecursive(true);

			List<Integer> selection = new ArrayList<>();
			overview.setSelectedItemIds(selection);
			switch(selectType) {
			case AUTO:
				break;
			case FOLDER:
				selection.add(folderOne.getId());
				break;
			case MANUAL:
				switch (listType) {
				case PAGE:
					selection.add(pageOneInFolderOne.getId());
					selection.add(pageTwoInFolderOne.getId());
					break;
				case FOLDER:
					selection.add(folderOneInFolderOne.getId());
					selection.add(folderTwoInFolderOne.getId());
					break;
				case FILE:
					selection.add(fileOneInFolderOne.getId());
					selection.add(fileTwoInFolderOne.getId());
					break;
				case IMAGE:
					selection.add(imageOneInFolderOne.getId());
					selection.add(imageTwoInFolderOne.getId());
					break;
				}
				break;
			}

			Property prop = new Property();
			prop.setOverview(overview);

			OverviewPartType overviewPartType = getPartType(OverviewPartType.class, overviewTag, "overview");
			overviewPartType.fromProperty(prop);
		}).build();

		overviewPageGlobalId = execute(Page::getGlobalId, overviewPage).toString();
	}

	@Test
	public void testImportNew() throws NodeException {
		// export overview page into package
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);

		// delete page
		consume(p -> p.delete(true), overviewPage);

		// import content package
		importContentPackage(packageName);

		// reload page
		overviewPage = supply(t -> t.getObject(Page.class, overviewPageGlobalId));
		assertOverview();
	}

	@Test
	public void testImportModifiedSettings() throws NodeException {
		// export overview page into package
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);

		// modify the overview by changing the order, maxItems and recursive flag
		overviewPage = update(overviewPage, update -> {
			ContentTag overviewTag = update.getContent().getContentTag(overviewTagName);
			OverviewPartType overviewPartType = getPartType(OverviewPartType.class, overviewTag, "overview");
			Property prop = overviewPartType.toProperty();
			Overview overview = prop.getOverview();

			overview.setOrderBy(OrderBy.CDATE);
			overview.setOrderDirection(OrderDirection.DESC);
			overview.setMaxItems(4711);
			overview.setRecursive(false);

			overviewPartType.fromProperty(prop);
		}).build();

		// import content package
		importContentPackage(packageName);

		// reload page
		overviewPage = supply(t -> t.getObject(Page.class, overviewPageGlobalId));
		assertOverview();
	}

	@SuppressWarnings("incomplete-switch")
	@Test
	public void testImportModifiedItems() throws NodeException {
		// export overview page into package
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);

		// modify the overview by selecting different objects
		overviewPage = update(overviewPage, update -> {
			ContentTag overviewTag = update.getContent().getContentTag(overviewTagName);
			OverviewPartType overviewPartType = getPartType(OverviewPartType.class, overviewTag, "overview");
			Property prop = overviewPartType.toProperty();

			List<Integer> selection = prop.getOverview().getSelectedItemIds();
			selection.clear();
			switch(selectType) {
			case AUTO:
				break;
			case FOLDER:
				selection.add(folderTwo.getId());
				break;
			case MANUAL:
				switch (listType) {
				case PAGE:
					selection.add(pageOneInFolderTwo.getId());
					selection.add(pageTwoInFolderTwo.getId());
					break;
				case FOLDER:
					selection.add(folderOneInFolderTwo.getId());
					selection.add(folderTwoInFolderTwo.getId());
					break;
				case FILE:
					selection.add(fileOneInFolderTwo.getId());
					selection.add(fileTwoInFolderTwo.getId());
					break;
				case IMAGE:
					selection.add(imageOneInFolderTwo.getId());
					selection.add(imageTwoInFolderTwo.getId());
					break;
				}
				break;
			}

			overviewPartType.fromProperty(prop);
		}).build();

		// import content package
		importContentPackage(packageName);

		// reload page
		overviewPage = supply(t -> t.getObject(Page.class, overviewPageGlobalId));
		assertOverview();
	}

	@Test
	public void testImportModifiedItemOrder() throws NodeException {
		// export overview page into package
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);

		// modify the overview by selecting different objects
		overviewPage = update(overviewPage, update -> {
			ContentTag overviewTag = update.getContent().getContentTag(overviewTagName);
			OverviewPartType overviewPartType = getPartType(OverviewPartType.class, overviewTag, "overview");
			Property prop = overviewPartType.toProperty();

			List<Integer> selection = prop.getOverview().getSelectedItemIds();
			Collections.reverse(selection);

			overviewPartType.fromProperty(prop);
		}).build();

		// import content package
		importContentPackage(packageName);

		// reload page
		overviewPage = supply(t -> t.getObject(Page.class, overviewPageGlobalId));
		assertOverview();
	}

	@Test
	public void testImportWithInexistentItem() throws NodeException {
		// modify the overview by adding a non-existent item to the selection
		overviewPage = update(overviewPage, update -> {
			ContentTag overviewTag = update.getContent().getContentTag(overviewTagName);
			OverviewPartType overviewPartType = getPartType(OverviewPartType.class, overviewTag, "overview");
			Property prop = overviewPartType.toProperty();

			List<Integer> selection = prop.getOverview().getSelectedItemIds();
			selection.add(4711);

			overviewPartType.fromProperty(prop);
		}).build();

		// export overview page into package
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);

		// import content package
		importContentPackage(packageName);

		// reload page
		overviewPage = supply(t -> t.getObject(Page.class, overviewPageGlobalId));
		assertOverview();
	}

	@SuppressWarnings("incomplete-switch")
	protected void assertOverview() throws NodeException {
		assertThat(overviewPage).as("Imported page").isNotNull();
		operate(() -> {
			Tag overviewTag = overviewPage.getContent().getTag(overviewTagName);
			assertThat(overviewTag).as("Imported overview tag");
			Property property = getPartType(OverviewPartType.class, overviewTag, "overview").toProperty();

			assertThat(property.getOverview()).as("Imported Overview")
				.isNotNull()
				.hasFieldOrPropertyWithValue("listType", listType)
				.hasFieldOrPropertyWithValue("selectType", selectType)
				.hasFieldOrPropertyWithValue("orderBy", OrderBy.ALPHABETICALLY)
				.hasFieldOrPropertyWithValue("orderDirection", OrderDirection.ASC)
				.hasFieldOrPropertyWithValue("maxItems", 3)
				.hasFieldOrPropertyWithValue("recursive", true);
		});

		List<Integer> importedSelection = supply(() -> getPartType(OverviewPartType.class, overviewPage.getContent().getTag(overviewTagName), "overview")
				.toProperty().getOverview().getSelectedItemIds());

		switch(selectType) {
		case AUTO:
			break;
		case FOLDER:
			assertThat(importedSelection).as("Imported overview selection").containsExactly(folderOne.getId());
			break;
		case MANUAL:
			switch (listType) {
			case PAGE:
				assertThat(importedSelection).as("Imported overview selection").containsExactly(pageOneInFolderOne.getId(), pageTwoInFolderOne.getId());
				break;
			case FOLDER:
				assertThat(importedSelection).as("Imported overview selection").containsExactly(folderOneInFolderOne.getId(), folderTwoInFolderOne.getId());
				break;
			case FILE:
				assertThat(importedSelection).as("Imported overview selection").containsExactly(fileOneInFolderOne.getId(), fileTwoInFolderOne.getId());
				break;
			case IMAGE:
				assertThat(importedSelection).as("Imported overview selection").containsExactly(imageOneInFolderOne.getId(), imageTwoInFolderOne.getId());
				break;
			}
			break;
		}
	}
}
