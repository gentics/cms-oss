package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse.SyncDependencies;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCTemplateResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for the <code>.../RESOURCE/usage/syncableObjects</code> endpoints.
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class SyncableObjectsTest {

	/**
	 * Channel structure for the tests.
	 */
	enum Channel {
		Master,
		Channel,
		Subchannel
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static byte[] imageData;

	private static UserGroup adminGroup;
	private static SystemUser testUser;
	private static Node master;
	private static Node channel;
	private static Node subchannel;
	private static Integer pageUrlConstructId;
	private static Integer fileUrlConstructId;
	private static Integer imageUrlConstructId;
	private static Template template;
	private static Map<Channel, List<NodeObject>> objPropDependencies = new HashMap<>();
	private static Map<Channel, List<NodeObject>> contentDependencies = new HashMap<>();

	@Parameter(0)
	public PushToMasterRequest.Type type;
	@Parameter(1)
	public Channel srcChannel;
	@Parameter(2)
	public Channel dstChannel;

	@Parameters(name = "{index}: object type {0}, source channel {1}, target channel {2}")
	public static Collection<Object[]> data() throws Exception {
		Collection<Object[]> data = new ArrayList<>();

		for (PushToMasterRequest.Type type : Arrays.asList(PushToMasterRequest.Type.page)) { // PushToMasterRequest.Type.values()) {
			Channel[] channels = Channel.values();

			for (Channel src : channels) {
				for (Channel dst : channels) {
					if (src.compareTo(dst) <= 0) {
						continue;
					}

					data.add(new Object[] { type, src, dst });
				}
			}
		}

		return data;
	}

	/**
	 * Creates the channel structure as well as used constructs and object properties and
	 * several objects in each channel for used as dependencies.
	 *
	 * @throws NodeException When an object cannot be created
	 * @throws IOException When the test image cannot be loaded
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		TransactionManager.getCurrentTransaction().commit();

		master = Trx.supply(() -> createNode("master", "master", "/master", null, false, false, false));
		channel = Trx.supply(() -> createChannel(master, "channel", "channel", "/channel"));
		subchannel = Trx.supply(() -> createChannel(channel, "subchannel", "subchannel", "/subchannel"));

		pageUrlConstructId = Trx.supply(() -> createConstruct(master, PageURLPartType.class, "page", "target"));
		fileUrlConstructId = Trx.supply(() -> createConstruct(master, FileURLPartType.class, "file", "target"));
		imageUrlConstructId = Trx.supply(() -> createConstruct(master, ImageURLPartType.class, "image", "target"));

		imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));

		Trx.operate(() -> {
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, pageUrlConstructId, "linkedPage", "linkedPage");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, fileUrlConstructId, "linkedFile", "linkedFile");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, imageUrlConstructId, "linkedImage", "linkedImage");

			ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, pageUrlConstructId, "linkedPage", "linkedPage");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, fileUrlConstructId, "linkedFile", "linkedFile");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, imageUrlConstructId, "linkedImage", "linkedImage");

			ContentNodeTestDataUtils.createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, pageUrlConstructId, "linkedPage", "linkedPage");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, fileUrlConstructId, "linkedFile", "linkedFile");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, imageUrlConstructId, "linkedImage", "linkedImage");

			ContentNodeTestDataUtils.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, pageUrlConstructId, "linkedPage", "linkedPage");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, fileUrlConstructId, "linkedFile", "linkedFile");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, imageUrlConstructId, "linkedImage", "linkedImage");
		});

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			adminGroup = t.getObject(UserGroup.class, 2);
			assertNotNull("AdminGroup must exist", adminGroup);

			testUser = ContentNodeTestDataUtils.createSystemUser("test", "test", "", "test", "test", Arrays.asList(adminGroup));
			trx.success();
		}

		template = Trx.supply(() -> createTemplate(master.getFolder(), "Template"));

		for (Channel c : Arrays.asList(Channel.Channel, Channel.Subchannel)) {
			Node node = c == Channel.Channel ? channel : subchannel;

			List<NodeObject> deps = Arrays.asList(
				Trx.supply(() -> createPage(node.getFolder(), template, "OP dependency in " + c, node)),
				Trx.supply(() -> createFile(node.getFolder(), "op-dependency-in-" + c + ".txt", "Testcontent".getBytes(), node)),
				Trx.supply(() -> createImage(node.getFolder(), "op-dependency-in-" + c + ".jpg", imageData, node)));

			objPropDependencies.put(c, deps);

			deps = Arrays.asList(
				Trx.supply(() -> createPage(node.getFolder(), template, "Content dependency in " + c, node)),
				Trx.supply(() -> createFile(node.getFolder(), "content-dependency-in-" + c + ".txt", "Testcontent".getBytes(), node)),
				Trx.supply(() -> createImage(node.getFolder(), "content-dependency-in-" + c + ".jpg", imageData, node)));
			contentDependencies.put(c,  deps);
		}
	}

	/**
	 * Add a content tag to <code>page</code> linking to <code>used</code>.
	 *
	 * The ID of <code>used</code> is added to the appropriate collection of IDs.
	 *
	 * @param page The page to add a link to <code>used</code> in
	 * @param used The object to link in <code>page</code>
	 * @param expectedPageIds The list of expected page IDs for this test
	 * @param expectedFileIds The list of expected file IDs for this test
	 * @param expectedImageIds The list of expected image IDs for this test
	 * @throws NodeException On errors
	 */
	private static void useInContent(
			Page page,
			NodeObject used,
			Collection<Integer> expectedPageIds,
			Collection<Integer> expectedFileIds,
			Collection<Integer> expectedImageIds) throws NodeException {
		if (used instanceof Page) {
			Tag tag = page.getContent().addContentTag(pageUrlConstructId);

			if (expectedPageIds != null) {
				expectedPageIds.add(used.getId());
			}

			getPartType(PageURLPartType.class, tag, "target").setTargetPage((Page) used);
		} else if (used instanceof ContentFile) {
			if (((ContentFile) used).isImage()) {
				Tag tag = page.getContent().addContentTag(imageUrlConstructId);

				if (expectedImageIds != null) {
					expectedImageIds.add(used.getId());
				}

				getPartType(ImageURLPartType.class, tag, "target").setTargetImage((ImageFile) used);
			} else {
				Tag tag = page.getContent().addContentTag(fileUrlConstructId);

				if (expectedFileIds != null) {
					expectedFileIds.add(used.getId());
				}

				getPartType(FileURLPartType.class, tag, "target").setTargetFile((File) used);
			}
		} else {
			throw new NodeException("Unexpected object type: " + used.getClass().getName());
		}
	}

	/**
	 * Convenience method for {@link #useInObjectProperty(ObjectTagContainer, NodeObject, Collection, Collection, Collection)}
	 * passing <code>null</code> for all the ID collections.
	 * @param page The page to add a link to <code>used</code> in
	 * @param used The object to link in <code>page</code>
	 * @throws NodeException On errors
	 */
	private static void useInContent(Page page, NodeObject used) throws NodeException {
		useInContent(page, used, null, null, null);
	}

	/**
	 * Add an object property to <code>obj</code> linking to <code>used</code>.
	 *
	 * The ID of <code>used</code> is added to the appropriate collection of IDs.
	 *
	 * @param obj The object to add a link to <code>used</code> in
	 * @param used The object to link in <code>obj</code>
	 * @param expectedPageIds The list of expected page IDs for this test
	 * @param expectedFileIds The list of expected file IDs for this test
	 * @param expectedImageIds The list of expected image IDs for this test
	 * @throws NodeException On errors
	 */
	private static void useInObjectProperty(
			ObjectTagContainer obj,
			NodeObject used,
			Collection<Integer> expectedPageIds,
			Collection<Integer> expectedFileIds,
			Collection<Integer> expectedImageIds) throws NodeException {
		if (used instanceof Page) {
			if (expectedPageIds != null) {
				expectedPageIds.add(used.getId());
			}

			getPartType(PageURLPartType.class, obj.getObjectTag("linkedPage"), "target")
				.setTargetPage((Page) used);
		} else if (used instanceof ContentFile) {
			if (((ContentFile) used).isImage()) {
				if (expectedImageIds != null) {
					expectedImageIds.add(used.getId());
				}

				getPartType(ImageURLPartType.class, obj.getObjectTag("linkedImage"), "target")
					.setTargetImage((ImageFile) used);
			} else {
				if (expectedFileIds != null) {
					expectedFileIds.add(used.getId());
				}

				getPartType(FileURLPartType.class, obj.getObjectTag("linkedFile"), "target")
					.setTargetFile((File) used);
			}
		} else {
			throw new NodeException("Unexpected object type: " + used.getClass().getName());
		}

	}

	/**
	 * Convenience method for {@link #useInObjectProperty(ObjectTagContainer, NodeObject, Collection, Collection, Collection)}
	 * passing <code>null</code> for all the ID collections.
	 *
	 * @param obj The object to add a link to <code>used</code> in
	 * @param used The object to link in <code>obj</code>
	 * @throws NodeException On error
	 */
	private static void useInObjectProperty(ObjectTagContainer obj, NodeObject used) throws NodeException {
		useInObjectProperty(obj, used, null, null, null);
	}

	/**
	 * Get the actual {@link Node} for the specified channel value.
	 *
	 * @param c The channel to get the <code>Node</code> object for
	 * @return The actual <code>Node</code> object for the specified channel
	 * @throws Exception On errors
	 */
	private Node getNode(Channel c) throws NodeException {
		switch (c) {
		case Master:
			return master;

		case Channel:
			return channel;

		case Subchannel:
			return subchannel;

		default:
			throw new NodeException("Unexpected source channel: " + srcChannel);
		}
	}

	/**
	 * Create an object for in the specified channel.
	 *
	 * The object will have the current {@link #type} for this test.
	 *
	 * @param objChannel The channel for the created object
	 * @return A new object in the specified channel
	 * @throws Exception On errors
	 */
	private NodeObject createObject(Channel objChannel) throws NodeException {
		return Trx.supply(() -> {
			Node node = getNode(objChannel);
			Folder folder = node.getFolder();
			String name = UUID.randomUUID().toString();

			switch (type) {
				case page:
					return createPage(folder, template, name, node);

				case file:
					return createFile(folder, name, "Test file contents".getBytes(), node);

				case image:
					return createImage(folder, name, imageData, node);

				case template:
					return createTemplate(folder, name, node);

				default:
					return null;
			}
		});
	}

	/**
	 * A convenience method for {@link #createObject(Channel)} to create an
	 * object in the current {@link #srcChannel}.
	 *
	 * @return An object in the current <code>srcChannel</code>
	 * @throws Exception On errors
	 */
	private NodeObject createObject() throws NodeException {
		return createObject(srcChannel);
	}

	/**
	 * Calls the <code>getSyncableObjects()</code> method of the respective resource.
	 *
	 * @param objId The ID of the objects to gather symchronisation dependencies for
	 * @return The lists of synchronisable objects linked in the specified object
	 * @throws Exception On errors
	 */
	private ReferencedSyncableObjectsListResponse makeRequest(Integer objId) throws NodeException {
		switch (type) {
			case page:
				return new MCPageResourceImpl().getSyncableObjects(
					Arrays.asList(objId),
					getNode(srcChannel).getId(),
					getNode(dstChannel).getId());

			case file:
				return new MCFileResourceImpl().getSyncableObjects(
					Arrays.asList(objId),
					getNode(srcChannel).getId(),
					getNode(dstChannel).getId());

			case image:
				return new MCImageResourceImpl().getSyncableObjects(
					Arrays.asList(objId),
					getNode(srcChannel).getId(),
					getNode(dstChannel).getId());

			case template:
				return new MCTemplateResourceImpl().getSyncableObjects(
					Arrays.asList(objId),
					getNode(srcChannel).getId(),
					getNode(dstChannel).getId());

			default:
				throw new NodeException("Unexpected type: " + type);
		}
	}

	/**
	 * Checks if the response contains all expected dependencies.
	 *
	 * @param rs The response to check
	 * @param expectedPageIds The expected page IDs
	 * @param expectedFileIds The expected file IDs
	 * @param expectedImageIds The expected image IDs
	 * @param expectedDeps The expected indirect dependencies
	 */
	private void checkResponse(
			ReferencedSyncableObjectsListResponse rs,
			List<Integer> expectedPageIds,
			List<Integer> expectedFileIds,
			List<Integer> expectedImageIds,
			Map<ItemType, SyncDependencies> expectedDeps) {

		List<Integer> foundPages = rs.getPages().stream().map(p -> p.getId()).collect(Collectors.toList());
		List<Integer> foundFiles = rs.getFiles().stream().map(p -> p.getId()).collect(Collectors.toList());
		List<Integer> foundImages = rs.getImages().stream().map(p -> p.getId()).collect(Collectors.toList());

		assertTrue(
			"Expected page IDs: " + expectedPageIds.size() + ", found: " + foundPages.size(),
			foundPages.containsAll(expectedPageIds));
		assertTrue(
			"Expected file IDs: " + expectedFileIds.size() + ", found: " + foundFiles.size(),
			foundFiles.containsAll(expectedFileIds));
		assertTrue(
			"Expected image IDs: " + expectedImageIds.size() + ", found: " + foundImages.size(),
			foundImages.containsAll(expectedImageIds));
	}

	/**
	 * Several objects from the same channel are linked directly in the target object.
	 *
	 * @throws Exception On errors
	 */
	@Test
	public void testDirectDependencyOnly() throws Exception {
		List<Integer> expectedPageIds = new ArrayList<>();
		List<Integer> expectedFileIds = new ArrayList<>();
		List<Integer> expectedImageIds = new ArrayList<>();
		Map<ItemType, SyncDependencies> expectedDeps = new HashMap<>();

		NodeObject obj = createObject();
		NodeObject using = Trx.supply(() -> update(obj, p -> {
			for (NodeObject dep : objPropDependencies.get(srcChannel)) {
				useInObjectProperty(
					(ObjectTagContainer) p,
					dep,
					expectedPageIds,
					expectedFileIds,
					expectedImageIds);
			}

			if (type == PushToMasterRequest.Type.page) {
				for (NodeObject dep : contentDependencies.get(srcChannel)) {
					useInContent(
						(Page) p,
						dep,
						expectedPageIds,
						expectedFileIds,
						expectedImageIds);
				}
			}
		}));

		Trx.operate(() -> checkResponse(makeRequest(using.getId()), expectedPageIds, expectedFileIds, expectedImageIds, expectedDeps));
	}

	/**
	 * The target object contains a link to another object which in turn has links to
	 * several other objects in the same channel.
	 *
	 * @throws Exception On errors
	 */
	@Test
	public void testIndirectDepedency() throws Exception {
		List<Integer> expectedPageIds = new ArrayList<>();
		List<Integer> expectedFileIds = new ArrayList<>();
		List<Integer> expectedImageIds = new ArrayList<>();
		Map<ItemType, SyncDependencies> expectedDeps = new HashMap<>();

		NodeObject linked = createObject();
		NodeObject linkedWithDeps = Trx.supply(() -> update(linked, p -> {
			for (NodeObject dep : objPropDependencies.get(srcChannel)) {
				useInObjectProperty(
					(ObjectTagContainer) p,
					dep,
					expectedPageIds,
					expectedFileIds,
					expectedImageIds);
			}

			if (type == PushToMasterRequest.Type.page) {
				for (NodeObject dep : contentDependencies.get(srcChannel)) {
					useInContent(
						(Page) p,
						dep,
						expectedPageIds,
						expectedFileIds,
						expectedImageIds);
				}
			}
		}));

		Map<Integer, Integer> pageDeps = new HashMap<>();

		for (Integer pageId : expectedPageIds) {
			pageDeps.put(pageId, getNode(srcChannel).getId());
		}

		Map<Integer, Integer> fileDeps = new HashMap<>();

		for (Integer fileId : expectedFileIds) {
			fileDeps.put(fileId, getNode(srcChannel).getId());
		}

		Map<Integer, Integer> imageDeps = new HashMap<>();

		for (Integer imageId : expectedImageIds) {
			imageDeps.put(imageId, getNode(srcChannel).getId());
		}

		Map<ItemType, Map<Integer, Integer>> deps = new HashMap<>();

		deps.put(ItemType.page, pageDeps);
		deps.put(ItemType.file, fileDeps);
		deps.put(ItemType.image, imageDeps);

		SyncDependencies syncDeps = new SyncDependencies();

		syncDeps.put(linkedWithDeps.getId(), deps);

		expectedDeps.put(ItemType.valueOf(type.toString()), syncDeps);

		expectedPageIds.clear();
		expectedFileIds.clear();
		expectedImageIds.clear();

		NodeObject obj = createObject();
		NodeObject using = Trx.supply(() -> update(obj, p -> {
			useInObjectProperty(
				(ObjectTagContainer) p,
				linkedWithDeps,
				expectedPageIds,
				expectedFileIds,
				expectedImageIds);

			if (type == PushToMasterRequest.Type.page) {
				useInContent(
					(Page) p,
					linkedWithDeps,
					expectedPageIds,
					expectedFileIds,
					expectedImageIds);
			}
		}));

		Trx.operate(() -> checkResponse(makeRequest(using.getId()), expectedPageIds, expectedFileIds, expectedImageIds, expectedDeps));
	}

	/**
	 * The linked object already exists in the destination channel.
	 *
	 * @throws Exception On error
	 */
	@Test
	public void testExistsInTarget() throws Exception {
		NodeObject linked = createObject(dstChannel);
		List<Integer> expectedPageIds = new ArrayList<>();
		List<Integer> expectedFileIds = new ArrayList<>();
		List<Integer> expectedImageIds = new ArrayList<>();
		Map<ItemType, SyncDependencies> expectedDeps = new HashMap<>();

		NodeObject using = Trx.supply(() -> update(linked, p -> {
			if (type == PushToMasterRequest.Type.page) {
				useInContent((Page) p, linked);
			} else {
				useInObjectProperty((ObjectTagContainer) p, linked);
			}
		}));

		Trx.operate(() -> checkResponse(makeRequest(using.getId()), expectedPageIds, expectedFileIds, expectedImageIds, expectedDeps));
	}

	/**
	 * The linked object is a localised whose master object is in the channel
	 * "between" the source and the destination channel.
	 *
	 * @throws Exception On errors
	 */
	@Test
	public void testObscured() throws Exception {
		List<Integer> expectedPageIds = new ArrayList<>();
		List<Integer> expectedFileIds = new ArrayList<>();
		List<Integer> expectedImageIds = new ArrayList<>();
		Map<ItemType, SyncDependencies> expectedDeps = new HashMap<>();

		if (srcChannel != Channel.Subchannel || dstChannel != Channel.Master) {
			return;
		}

		NodeObject localised;
		NodeObject masterObj = createObject(Channel.Channel);

		switch (type) {
		case page:
			localised = Trx.supply(() -> ContentNodeTestDataUtils.localize((Page) masterObj, getNode(srcChannel)));
			break;

		case file:
			localised = Trx.supply(() -> ContentNodeTestDataUtils.localize((File) masterObj, getNode(srcChannel)));
			break;

		case image:
			localised = Trx.supply(() -> ContentNodeTestDataUtils.localize((ImageFile) masterObj, getNode(srcChannel)));
			break;

		case template:
			localised = Trx.supply(() -> ContentNodeTestDataUtils.localize((Template) masterObj, getNode(srcChannel)));
			break;

		default:
			throw new Exception("Unexpected type: " + type);
		}

		NodeObject obj = createObject();
		NodeObject using = Trx.supply(() -> update(obj, p -> {
			if (type == PushToMasterRequest.Type.page) {
				useInContent((Page) p, localised);
			} else {
				useInObjectProperty((ObjectTagContainer) p, localised);
			}
		}));

		Trx.operate(() -> checkResponse(makeRequest(using.getId()), expectedPageIds, expectedFileIds, expectedImageIds, expectedDeps));
	}

	@Test
	public void testNoPermission() throws Exception {
		List<Integer> expectedPageIds = new ArrayList<>();
		List<Integer> expectedFileIds = new ArrayList<>();
		List<Integer> expectedImageIds = new ArrayList<>();
		Map<ItemType, SyncDependencies> expectedDeps = new HashMap<>();

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			testUser = t.getObject(SystemUser.class, testUser.getId(), true);

			Set<Integer> nodeIds = new HashSet<>();

			nodeIds.add(master.getId());
			nodeIds.add(channel.getId());
			nodeIds.add(subchannel.getId());

			testUser.getGroupNodeRestrictions().put(2, Collections.singleton(getNode(srcChannel).getId()));
			testUser.save();

			testUser = t.getObject(SystemUser.class, testUser.getId());
			trx.success();
		}

		NodeObject linked = createObject();
		NodeObject obj = createObject();
		NodeObject using = Trx.supply(() -> update(obj, o -> {
			if (type == PushToMasterRequest.Type.page) {
				useInContent((Page) o, linked);
			} else {
				useInObjectProperty((ObjectTagContainer) o, linked);
			}
		}));

		try (Trx trx = new Trx(null, ObjectTransformer.getInt(testUser.getId(), 0))) {
			checkResponse(makeRequest(using.getId()), expectedPageIds, expectedFileIds, expectedImageIds, expectedDeps);
			trx.success();
		}
	}
}
