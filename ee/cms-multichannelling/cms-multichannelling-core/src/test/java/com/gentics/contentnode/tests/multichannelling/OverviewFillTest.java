package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

import io.reactivex.Flowable;

/**
 * Test cases for filling overviews in multichannelling environments
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
@RunWith(value = Parameterized.class)
public class OverviewFillTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Integer overviewConstructId;
	private static Template template;
	private static Page overviewPage;
	private static String overviewTagName;
	private static Folder inheritedFolder;
	private static Folder masterFolder;
	private static Folder localizedFolder;
	private static Folder localFolder;
	private static Page inheritedPage;
	private static Page masterPage;
	private static Page localizedPage;
	private static Page localPage;
	private static File inheritedFile;
	private static File masterFile;
	private static File localizedFile;
	private static File localFile;
	private static File inheritedImage;
	private static File masterImage;
	private static File localizedImage;
	private static File localImage;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		overviewPage = supply(() -> update(createPage(node.getFolder(), template, "Overview"), upd -> {
			ContentTag tag = upd.getContent().addContentTag(overviewConstructId);
			overviewTagName = tag.getName();
		}));

		// folders for selection
		inheritedFolder = supply(() -> createFolder(node.getFolder(), "Inherited Folder"));
		masterFolder = supply(() -> createFolder(node.getFolder(), "Master Folder"));
		localizedFolder = supply(() -> localize(masterFolder, channel));
		localFolder = supply(() -> createFolder(node.getFolder(), "Local Folder", channel));

		// pages for selection
		inheritedPage = supply(() -> update(createPage(node.getFolder(), template, "Inherited Page"), Page::publish));
		masterPage = supply(() -> update(createPage(node.getFolder(), template, "Master Page"), Page::publish));
		localizedPage = supply(() -> localize(masterPage, channel));
		localPage = supply(() -> update(createPage(node.getFolder(), template, "Local Page", channel), Page::publish));

		// files for selection
		inheritedFile = supply(() -> createFile(node.getFolder(), "inherited_file.txt", "bla".getBytes()));
		masterFile = supply(() -> createFile(node.getFolder(), "master_file.txt", "bla".getBytes()));
		localizedFile = supply(() -> localize(masterFile, channel));
		localFile = supply(() -> createFile(node.getFolder(), "local_file.txt", "bla".getBytes(), channel));

		// images for selection
		InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg");
		byte[] imageData = IOUtils.toByteArray(inputStream);
		inheritedImage = supply(() -> createImage(node.getFolder(), "inherited_image.jpg", imageData));
		masterImage = supply(() -> createImage(node.getFolder(), "master_image.jpg", imageData));
		localizedImage = supply(() -> localize(masterImage, channel));
		localImage = supply(() -> createImage(node.getFolder(), "local_image.jpg", imageData, channel));
	}

	@Parameters(name = "{index}: selecttype {0}, listtype {1}, stickychannel {2}, filledOverview: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (SelectType selectType : Arrays.asList(SelectType.FOLDER, SelectType.MANUAL)) {
			for (ListType listType : Arrays.asList(ListType.FILE, ListType.IMAGE, ListType.FOLDER, ListType.PAGE)) {
				for (Boolean stickyChannel : Arrays.asList(true, false)) {
					for (Boolean emptyOverview : Arrays.asList(true, false)) {
						data.add(new Object[] { selectType, listType, stickyChannel, emptyOverview });
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public SelectType selectType;

	@Parameter(1)
	public ListType listType;

	@Parameter(2)
	public boolean stickyChannel;

	@Parameter(3)
	public boolean filledOverview;

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			update(t.getObject(Construct.class, overviewConstructId), upd -> {
				Part p = upd.getParts().get(0);
				OverviewPartSetting setting = new OverviewPartSetting(p);
				setting.setStickyChannel(stickyChannel);
				setting.setTo(p);
			});
			overviewPage = update(overviewPage, upd -> {
				com.gentics.contentnode.object.Overview overview = getPartType(OverviewPartType.class, upd.getContentTag(overviewTagName), "ds").getOverview();

				List<OverviewEntry> entries = overview.getOverviewEntries();

				entries.clear();

				if (filledOverview) {
					OverviewEntry entry = t.createObject(OverviewEntry.class);

					if (stickyChannel) {
						entry.setNodeId(node.getId());
					}

					entry.setObjectId(getSelectedIds(true).get(0));

					entries.add(entry);
				}
			});
		});
	}

	/**
	 * Test setting the selection to inherited, localized and local objects. Check that the selection is stored for the master objects
	 * @throws NodeException
	 */
	@Test
	public void testSelection() throws NodeException {
		List<Integer> selectedItemIds = new ArrayList<>();
		List<NodeIdObjectId> selectedNodeItemIds = new ArrayList<>();
		List<Integer> expectedItemIds = new ArrayList<>();
		List<NodeIdObjectId> expectedNodeItemIds = new ArrayList<>();

		if (stickyChannel) {
			selectedNodeItemIds.addAll(Flowable.zip(Flowable.fromArray(node.getId(), channel.getId()), Flowable.fromIterable(getSelectedIds()),
					(nodeId, objectId) -> new NodeIdObjectId(nodeId, objectId)).toList().blockingGet());
			expectedNodeItemIds.addAll(Flowable.zip(Flowable.fromArray(node.getId(), channel.getId()), Flowable.fromIterable(getExpectedIds()),
					(nodeId, objectId) -> new NodeIdObjectId(nodeId, objectId)).toList().blockingGet());
		} else {
			selectedItemIds.addAll(getSelectedIds());
			expectedItemIds.addAll(getExpectedIds());
		}

		String id = Integer.toString(overviewPage.getId());
		operate(() -> {
			PageLoadResponse loadResponse = getPageResource().load(id, true, false, false, false, false, false, false, false, false, false, null, null);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Page page = loadResponse.getPage();
			assertThat(page.getTags()).as("Page tags").containsKey(overviewTagName);
			Tag tag = page.getTags().get(overviewTagName);
			assertThat(tag.getProperties()).as("Tag properties").containsKey("ds");
			Property prop = tag.getProperties().get("ds");
			assertThat(prop.getOverview()).as("Overview").isNotNull();
			Overview overview = prop.getOverview();

			// change the selection and save
			overview.setListType(listType);
			overview.setSelectType(selectType);
			if (stickyChannel) {
				overview.setSelectedNodeItemIds(selectedNodeItemIds);
			} else {
				overview.setSelectedItemIds(selectedItemIds);
			}
			GenericResponse saveResponse = getPageResource().save(id, new PageSaveRequest(page));
			assertResponseOK(saveResponse);

			// load page again and assert
			loadResponse = getPageResource().load(id, true, false, false, false, false, false, false, false, false, false, null, null);
			assertResponseOK(loadResponse);
			page = loadResponse.getPage();
			assertThat(page.getTags()).as("Page tags").containsKey(overviewTagName);
			tag = page.getTags().get(overviewTagName);
			assertThat(tag.getProperties()).as("Tag properties").containsKey("ds");
			prop = tag.getProperties().get("ds");
			assertThat(prop.getOverview()).as("Overview").isNotNull();
			overview = prop.getOverview();

			if (stickyChannel) {
				assertThat(overview.getSelectedNodeItemIds()).as("Selection").containsExactlyElementsOf(expectedNodeItemIds);
			} else {
				assertThat(overview.getSelectedItemIds()).as("Selection").containsExactlyElementsOf(expectedItemIds);
			}
		});
	}

	protected List<Integer> getSelectedIds() {
		return getSelectedIds(false);
	}

	protected List<Integer> getSelectedIds(boolean master) {
		Flowable<LocalizableNodeObject<?>> ret;

		switch(selectType) {
		case FOLDER:
			ret = Flowable.fromArray(localizedFolder, inheritedFolder, localFolder);

			break;

		case MANUAL:
			switch (listType) {
			case FILE:
				ret = Flowable.fromArray(localizedFile, inheritedFile, localFile);

				break;
			case IMAGE:
				ret = Flowable.fromArray(localizedImage, inheritedImage, localImage);

				break;
			case FOLDER:
				ret = Flowable.fromArray(localizedFolder, inheritedFolder, localFolder);

				break;
			case PAGE:
				ret = Flowable.fromArray(localizedPage, inheritedPage, localPage);

				break;
			default:
				fail("Unexpected list type " + listType);
				return null;
			}

			break;

		default:
			fail("Unexpected select type " + selectType);
			return null;
		}

		if (master) {
			ret = ret.map(LocalizableNodeObject::getMaster);
		}

		return ret.map(LocalizableNodeObject::getId).toList().blockingGet();
	}

	protected List<Integer> getExpectedIds() {
		switch(selectType) {
		case FOLDER:
			return Arrays.asList(masterFolder.getId(), inheritedFolder.getId(), localFolder.getId());
		case MANUAL:
			switch (listType) {
			case FILE:
				return Arrays.asList(masterFile.getId(), inheritedFile.getId(), localFile.getId());
			case IMAGE:
				return Arrays.asList(masterImage.getId(), inheritedImage.getId(), localImage.getId());
			case FOLDER:
				return Arrays.asList(masterFolder.getId(), inheritedFolder.getId(), localFolder.getId());
			case PAGE:
				return Arrays.asList(masterPage.getId(), inheritedPage.getId(), localPage.getId());
			default:
				fail("Unexpected list type " + listType);
				return null;
			}
		default:
			fail("Unexpected select type " + selectType);
			return null;
		}
	}
}
