package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertDirtedObjectsNonStrict;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
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
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.LocalizeFileCallable;
import com.gentics.contentnode.job.LocalizeFolderCallable;
import com.gentics.contentnode.job.LocalizeImageCallable;
import com.gentics.contentnode.job.LocalizePageCallable;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for dirting when objects are localized
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class LocalizationDirtingTest {
	public final static String TEST_HTML_TAG_NAME = "testhtmltag";

	public final static String TEST_HBS_TAG_NAME = "testhbstag";

	public final static String NODE_PART_NAME = "node";

	public final static String FOLDER_PART_NAME = "folder";

	public final static String PAGE_PART_NAME = "page";

	public final static String FILE_PART_NAME = "file";

	public final static String IMAGE_PART_NAME = "image";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node testNode;

	private static Node testChannel;

	private static Construct testHTMLConstruct;

	private static Template testTemplate;

	private static Construct testHbsConstruct;

	@Parameters(name = "{index}: {0}.{1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType<? extends Object> testedType : List.of(new TestedTypeFolder(), new TestedTypePage(),
				new TestedTypeFile(), new TestedTypeImage())) {
			for (String resolve : List.of("id", "name")) {
				data.add(new Object[] { testedType, resolve });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		testNode = supply(() -> createNode());
		testChannel = supply(() -> createChannel(testNode, "Channel", "channel", "/"));

		testHTMLConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("test_html_construct");
			c.setName("Test HTML Construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(FOLDER_PART_NAME);
				p.setPartTypeId(getPartTypeId(FolderURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(PAGE_PART_NAME);
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(FILE_PART_NAME);
				p.setPartTypeId(getPartTypeId(FileURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(IMAGE_PART_NAME);
				p.setPartTypeId(getPartTypeId(ImageURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("html");
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));

				p.setDefaultValue(create(Value.class, v -> {
					v.setValueText("");
				}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		testHbsConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("test_hbs_construct");
			c.setName("Test Handlebars Construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(NODE_PART_NAME);
				p.setPartTypeId(getPartTypeId(HTMLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(FOLDER_PART_NAME);
				p.setPartTypeId(getPartTypeId(FolderURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(PAGE_PART_NAME);
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(FILE_PART_NAME);
				p.setPartTypeId(getPartTypeId(FileURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(IMAGE_PART_NAME);
				p.setPartTypeId(getPartTypeId(ImageURLPartType.class));
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("hbs");
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));

				p.setDefaultValue(create(Value.class, v -> {
					v.setValueText("");
				}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		testTemplate = create(Template.class, t -> {
			t.setName("Test Template");
			t.setSource("<node %s><node %s>".formatted(TEST_HTML_TAG_NAME, TEST_HBS_TAG_NAME));

			t.getTags().put(TEST_HTML_TAG_NAME, create(TemplateTag.class, tt -> {
				tt.setConstructId(testHTMLConstruct.getId());
				tt.setEnabled(true);
				tt.setName(TEST_HTML_TAG_NAME);
				tt.setPublic(true);
			}).doNotSave().build());
			t.getTags().put(TEST_HBS_TAG_NAME, create(TemplateTag.class, tt -> {
				tt.setConstructId(testHbsConstruct.getId());
				tt.setEnabled(true);
				tt.setName(TEST_HBS_TAG_NAME);
				tt.setPublic(true);
			}).doNotSave().build());
		}).build();
	}

	@Parameter(0)
	public TestedType<NodeObject> testedType;

	@Parameter(1)
	public String resolve;

	@Before
	public void setup() throws NodeException {
		// update constructs to resolve the correct attribute

		testHTMLConstruct = update(testHTMLConstruct, c -> {
			getPartType(LongHTMLPartType.class, c, "html").setText("""
					Folder ID: <node tag.parts.%1$s.target.%5$s>
					Page ID: <node tag.parts.%2$s.target.%5$s>
					File ID: <node tag.parts.%3$s.target.%5$s>
					Image ID: <node tag.parts.%4$s.target.%5$s>
					""".formatted(FOLDER_PART_NAME, PAGE_PART_NAME, FILE_PART_NAME, IMAGE_PART_NAME, resolve));
		}).build();

		testHbsConstruct = update(testHbsConstruct, c -> {
			getPartType(HandlebarsPartType.class, c, "hbs").setText("""
					{{#gtx_channel cms.tag.parts.%1$s.text}}
					Folder ID: {{cms.tag.parts.%2$s.target.%6$s}}
					Page ID: {{cms.tag.parts.%3$s.target.%6$s}}
					File ID: {{cms.tag.parts.%4$s.target.%6$s}}
					Image ID: {{cms.tag.parts.%5$s.target.%6$s}}
					{{/gtx_channel}}
					""".formatted(NODE_PART_NAME, FOLDER_PART_NAME, PAGE_PART_NAME, FILE_PART_NAME, IMAGE_PART_NAME,
					resolve));
		}).build();
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			clear(testChannel);
			clear(testNode);
		});
	}

	/**
	 * Test with direct reference to the target object (via resolving a property from the target directly)
	 * @throws NodeException
	 * @throws Exception
	 */
	@Test
	public void testDirectReference() throws NodeException, Exception {
		// create target object in the node
		NodeObject targetObjectMaster = testedType.createTargetObject();

		// create source page in the node, referencing the target object
		Page sourcePageMaster = create(Page.class, p -> {
			p.setFolderId(testNode.getFolder().getId());
			p.setTemplateId(testTemplate.getId());
			p.setName("Source Page");

			testedType.setReference(p.getContent().getContentTag(TEST_HTML_TAG_NAME), targetObjectMaster);
		}).publish().build();

		// run the publish process to create the dependencies
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// localize the target object
		NodeObject targetObjectChannel = testedType.localize(targetObjectMaster);

		operate(() -> testContext.waitForDirtqueueWorker());

		try (Trx trx = new Trx() ) {
			assertDirtedObjectsNonStrict(testNode, sourcePageMaster);
			assertDirtedObjectsNonStrict(testChannel, sourcePageMaster, targetObjectChannel);
			trx.success();
		}
	}

	/**
	 * Test with reference rendered in the scope of a channel helper for the node
	 * @throws NodeException
	 * @throws Exception
	 */
	@Test
	public void testChannelHelperForNode() throws NodeException, Exception {
		// create target object in the node
		NodeObject targetObjectMaster = testedType.createTargetObject();

		// create source page in the node, referencing the target object
		Page sourcePageMaster = create(Page.class, p -> {
			p.setFolderId(testNode.getFolder().getId());
			p.setTemplateId(testTemplate.getId());
			p.setName("Source Page");

			ContentTag tag = p.getContent().getContentTag(TEST_HBS_TAG_NAME);
			testedType.setReference(tag, targetObjectMaster);
			getPartType(HTMLPartType.class, tag, NODE_PART_NAME).setText(Integer.toString(testNode.getId()));
		}).publish().build();

		// run the publish process to create the dependencies
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// localize the target object
		NodeObject targetObjectChannel = testedType.localize(targetObjectMaster);

		operate(() -> testContext.waitForDirtqueueWorker());

		try (Trx trx = new Trx() ) {
			assertDirtedObjectsNonStrict(testNode, sourcePageMaster);
			assertDirtedObjectsNonStrict(testChannel, sourcePageMaster, targetObjectChannel);
			trx.success();
		}
	}

	/**
	 * Test with reference rendered in the scope of a channel helper for the channel
	 * @throws NodeException
	 * @throws Exception
	 */
	@Test
	public void testChannelHelperForChannel() throws NodeException, Exception {
		// create target object in the node
		NodeObject targetObjectMaster = testedType.createTargetObject();

		// create source page in the node, referencing the target object
		Page sourcePageMaster = create(Page.class, p -> {
			p.setFolderId(testNode.getFolder().getId());
			p.setTemplateId(testTemplate.getId());
			p.setName("Source Page");

			ContentTag tag = p.getContent().getContentTag(TEST_HBS_TAG_NAME);
			testedType.setReference(tag, targetObjectMaster);
			getPartType(HTMLPartType.class, tag, NODE_PART_NAME).setText(Integer.toString(testChannel.getId()));
		}).publish().build();

		// run the publish process to create the dependencies
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// localize the target object
		NodeObject targetObjectChannel = testedType.localize(targetObjectMaster);

		operate(() -> testContext.waitForDirtqueueWorker());

		try (Trx trx = new Trx() ) {
			assertDirtedObjectsNonStrict(testNode, sourcePageMaster);
			assertDirtedObjectsNonStrict(testChannel, sourcePageMaster, targetObjectChannel);
			trx.success();
		}
	}

	/**
	 * Interface for tested types
	 * @param <T> class of the tested type
	 */
	protected static interface TestedType<T extends NodeObject> {
		/**
		 * Create the target object
		 * @return target object
		 * @throws NodeException
		 */
		T createTargetObject() throws NodeException;

		/**
		 * Set the reference to the target object in a tag
		 * @param tag tag to contain the reference
		 * @param targetObject target object
		 * @throws NodeException
		 */
		void setReference(ContentTag tag, T targetObject) throws NodeException;

		/**
		 * Localize an object
		 * @param object object to localize
		 * @return localized object
		 * @throws NodeException
		 */
		T localize(T object) throws NodeException;
	}

	/**
	 * Implementation of {@link TestedType} for {@link Folder}
	 */
	protected static class TestedTypeFolder implements TestedType<Folder> {

		@Override
		public Folder createTargetObject() throws NodeException {
			return create(Folder.class, f -> {
				f.setMotherId(testNode.getFolder().getId());
				f.setName("Target Folder");
			}).build();
		}

		@Override
		public void setReference(ContentTag tag, Folder targetObject) throws NodeException {
			getPartType(FolderURLPartType.class, tag, FOLDER_PART_NAME).setTargetFolder(targetObject);
		}

		@Override
		public Folder localize(Folder object) throws NodeException {
			try (Trx trx = new Trx()) {
				try {
					new LocalizeFolderCallable(object.getId(), testChannel.getId(), false).call();
				} catch (NodeException e) {
					throw e;
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}

			try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(testChannel)) {
				object = object.reload();
				trx.success();
			}

			return object;
		}

		@Override
		public String toString() {
			return "folder";
		}
	}

	/**
	 * Implementation of {@link TestedType} for {@link Page}
	 */
	protected static class TestedTypePage implements TestedType<Page> {

		@Override
		public Page createTargetObject() throws NodeException {
			return create(Page.class, p -> {
				p.setFolderId(testNode.getFolder().getId());
				p.setTemplateId(testTemplate.getId());
				p.setName("Target Page");
			}).publish().build();
		}

		@Override
		public void setReference(ContentTag tag, Page targetObject) throws NodeException {
			getPartType(PageURLPartType.class, tag, PAGE_PART_NAME).setTargetPage(targetObject);
		}

		@Override
		public Page localize(Page object) throws NodeException {
			try (Trx trx = new Trx()) {
				try {
					new LocalizePageCallable(object.getId(), testChannel.getId(), false).call();
				} catch (NodeException e) {
					throw e;
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}

			try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(testChannel)) {
				object = object.reload();
				trx.success();
			}

			return object;
		}

		@Override
		public String toString() {
			return "page";
		}
	}

	/**
	 * Implementation of {@link TestedType} for {@link File}
	 */
	protected static class TestedTypeFile implements TestedType<File> {

		@Override
		public File createTargetObject() throws NodeException {
			return create(File.class, f -> {
				f.setFolderId(testNode.getFolder().getId());
				f.setName("testfile.txt");
				f.setFileStream(new ByteArrayInputStream("File contents".getBytes()));
			}).build();
		}

		@Override
		public void setReference(ContentTag tag, File targetObject) throws NodeException {
			getPartType(FileURLPartType.class, tag, FILE_PART_NAME).setTargetFile(targetObject);
		}

		@Override
		public File localize(File object) throws NodeException {
			try (Trx trx = new Trx()) {
				try {
					new LocalizeFileCallable(object.getId(), testChannel.getId(), false).call();
				} catch (NodeException e) {
					throw e;
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}

			try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(testChannel)) {
				object = object.reload();
				trx.success();
			}

			return object;
		}

		@Override
		public String toString() {
			return "file";
		}
	}

	/**
	 * Implementation of {@link TestedType} for {@link ImageFile}
	 */
	protected static class TestedTypeImage implements TestedType<ImageFile> {

		@Override
		public ImageFile createTargetObject() throws NodeException {
			try (InputStream in = GenericTestUtils.getPictureResource("blume.jpg")) {
				return create(ImageFile.class, f -> {
					f.setFolderId(testNode.getFolder().getId());
					f.setName("blume.jpg");
					f.setFileStream(in);
				}).build();
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}

		@Override
		public void setReference(ContentTag tag, ImageFile targetObject) throws NodeException {
			getPartType(ImageURLPartType.class, tag, IMAGE_PART_NAME).setTargetImage(targetObject);
		}

		@Override
		public ImageFile localize(ImageFile object) throws NodeException {
			try (Trx trx = new Trx()) {
				try {
					new LocalizeImageCallable(object.getId(), testChannel.getId(), false).call();
				} catch (NodeException e) {
					throw e;
				} catch (Exception e) {
					throw new NodeException(e);
				}
				trx.success();
			}

			try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(testChannel)) {
				object = object.reload();
				trx.success();
			}

			return object;
		}

		@Override
		public String toString() {
			return "image";
		}
	}
}
