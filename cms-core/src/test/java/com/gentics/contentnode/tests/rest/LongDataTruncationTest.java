package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for automatic truncation of too long data
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.NICE_URLS, Feature.PUB_DIR_SEGMENT })
public class LongDataTruncationTest {
	protected final static String LONG_NAME = StringUtils.repeat("a", 260);

	protected final static String SHORT_NAME = StringUtils.repeat("a", 10);

	protected final static String MAX_ALLOWED_NAME = StringUtils.repeat("a", Page.MAX_NAME_LENGTH);

	protected final static String MAX_ALLOWED_FILENAME = StringUtils.repeat("a", File.MAX_NAME_LENGTH);

	protected final static String MAX_ALLOWED_KEYWORD = StringUtils.repeat("a", Construct.MAX_KEYWORD_LENGTH);

	protected final static String SHORT_CODE = StringUtils.repeat("a", 2);

	protected final static String MAX_ALLOWED_CODE = StringUtils.repeat("a", 5);

	protected final static String SHORT_PUB_DIR = "/" + StringUtils.repeat("a", 8) + "/";

	protected final static String LONG_PUB_DIR = "/" + StringUtils.repeat("a", 258) + "/";

	protected final static String MAX_ALLOWED_PUB_DIR = "/" + StringUtils.repeat("a", Folder.MAX_NAME_LENGTH - 2) + "/";

	protected final static String SHORT_NICE_URL = "/" + StringUtils.repeat("a", 9);

	protected final static String LONG_NICE_URL = "/" + StringUtils.repeat("a", 259);

	protected final static String MAX_ALLOWED_NICE_URL = "/" + StringUtils.repeat("a", Page.MAX_NICE_URL_LENGTH - 1);

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Integer constructId;

	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestType type : TestType.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = Trx.supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));
	}

	@Parameter(0)
	public TestType type;

	/**
	 * Clean data of previous tests
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		node = Trx.supply(() -> update(node, n -> {
			n.setPubDirSegment(type == TestType.folderPubDirSegment);
		}));
		Trx.operate(() -> {
			Transaction trx = TransactionManager.getCurrentTransaction();
			for (Page p : node.getFolder().getPages()) {
				p.delete(true);
			}
			for (Folder f : node.getFolder().getChildFolders()) {
				f.delete(true);
			}
			for (File f : node.getFolder().getFilesAndImages()) {
				f.delete(true);
			}
			for (Template t : node.getFolder().getTemplates()) {
				if (!t.equals(template)) {
					t.delete(true);
				}
			}
			for (Construct c : node.getConstructs()) {
				if (!c.getId().equals(constructId)) {
					c.delete(true);
				}
			}
			for (ContentRepository c : trx.getObjects(ContentRepository.class, DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS))) {
				c.delete(true);
			}
			for (Datasource ds : trx.getObjects(Datasource.class, DBUtils.select("SELECT id FROM datasource", DBUtils.IDS))) {
				ds.delete(true);
			}
			for (ContentLanguage lang : node.getLanguages()) {
				lang.delete(true);
			}
			for (ObjectTagDefinition def : trx.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS))) {
				if (def.getNodes().contains(node)) {
					def.delete(true);
				}
			}
		});
	}

	/**
	 * Test creating object with long attribute value
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {
		NodeObject object = Trx.supply(type.createLong);
		assertThat(type.get.apply(object)).as("Saved attribute").isEqualTo(type.maxAllowed);
	}

	/**
	 * Test uniqueness when creating object with long attribute value
	 * @throws Exception
	 */
	@Test
	public void testCreateUnique() throws Exception {
		if (type.unique) {
			NodeObject object1 = Trx.supply(type.createLong);
			NodeObject object2 = Trx.supply(type.createLong);

			assertThat(type.get.apply(object2)).as("Second attribute").isNotEqualTo(type.get.apply(object1));
		}
	}

	/**
	 * Test updating object with long attribute value
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		NodeObject object = Trx.supply(type.createShort);
		assertThat(type.get.apply(object)).as("Saved attribute").isEqualTo(type.shortValue);

		NodeObject updated = Trx.execute(type.updateLong, object);

		assertThat(type.get.apply(updated)).as("Updated attribute").isEqualTo(type.maxAllowed);
	}

	/**
	 * Test uniqueness when updating object with long attribute value
	 * @throws Exception
	 */
	@Test
	public void testUpdateUnique() throws Exception {
		if (type.unique) {
			NodeObject object1 = Trx.supply(type.createLong);
			NodeObject object2 = Trx.supply(type.createShort);

			NodeObject updated = Trx.execute(type.updateLong, object2);

			assertThat(type.get.apply(updated)).as("Updated attribute").isNotEqualTo(type.get.apply(object1));
		}
	}

	/**
	 * Enumeration of tested attributes
	 */
	public static enum TestType {
		pageName(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(LONG_NAME);
		}), () -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(SHORT_NAME);
		}), page -> update((Page) page, p -> p.setName(LONG_NAME)), o -> ((Page) o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		pageFileName(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setFilename(LONG_NAME);
		}), () -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setFilename(SHORT_NAME);
		}), o -> update((Page) o, p -> p.setFilename(LONG_NAME)), o -> ((Page) o).getFilename(), SHORT_NAME, MAX_ALLOWED_FILENAME, true),

		pageDescription(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setDescription(LONG_NAME);
		}), () -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setDescription(SHORT_NAME);
		}), page -> update((Page) page, p -> p.setDescription(LONG_NAME)), o -> ((Page) o).getDescription(), SHORT_NAME, MAX_ALLOWED_NAME, false),

		pageNiceUrl(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setNiceUrl(LONG_NICE_URL);
		}), () -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setNiceUrl(SHORT_NICE_URL);
		}), page -> update((Page) page, p -> p.setNiceUrl(LONG_NICE_URL)), o -> ((Page) o).getNiceUrl(), SHORT_NICE_URL, MAX_ALLOWED_NICE_URL, true),

		folderName(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(LONG_NAME);
		}), () -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
		}), o -> update((Folder)o, f -> f.setName(LONG_NAME)), o -> ((Folder)o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		folderPubDir(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setPublishDir(LONG_PUB_DIR);
		}), () -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setPublishDir(SHORT_PUB_DIR);
		}), o -> update((Folder)o, f -> f.setPublishDir(LONG_PUB_DIR)), o -> ((Folder)o).getPublishDir(), SHORT_PUB_DIR, MAX_ALLOWED_PUB_DIR, false),

		folderPubDirSegment(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setPublishDir(LONG_NAME);
		}), () -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setPublishDir(SHORT_NAME);
		}), o -> update((Folder)o, f -> f.setPublishDir(LONG_NAME)), o -> ((Folder)o).getPublishDir(), SHORT_NAME, MAX_ALLOWED_FILENAME, true),

		folderDescription(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setDescription(LONG_NAME);
		}), () -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setDescription(SHORT_NAME);
		}), o -> update((Folder)o, f -> f.setDescription(LONG_NAME)), o -> ((Folder)o).getDescription(), SHORT_NAME, MAX_ALLOWED_NAME, false),

		fileName(() -> create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(LONG_NAME);
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}), () -> create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}), o -> update((File)o, f -> f.setName(LONG_NAME)), o -> ((File)o).getName(), SHORT_NAME, MAX_ALLOWED_FILENAME, true),

		fileDescription(() -> create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setDescription(LONG_NAME);
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}), () -> create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(SHORT_NAME);
			f.setDescription(SHORT_NAME);
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}), o -> update((File)o, f -> f.setDescription(LONG_NAME)), o -> ((File)o).getDescription(), SHORT_NAME, MAX_ALLOWED_NAME, false),

		templateName(() -> create(Template.class, t -> {
			t.addFolder(node.getFolder());
			t.setName(LONG_NAME);
			t.setSource("");
		}), () -> create(Template.class, t -> {
			t.addFolder(node.getFolder());
			t.setName(SHORT_NAME);
			t.setSource("");
		}), o -> update((Template)o, t -> t.setName(LONG_NAME)), o -> ((Template)o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		templateDescription(() -> create(Template.class, t -> {
			t.addFolder(node.getFolder());
			t.setName(SHORT_NAME);
			t.setDescription(LONG_NAME);
			t.setSource("");
		}), () -> create(Template.class, t -> {
			t.addFolder(node.getFolder());
			t.setName(SHORT_NAME);
			t.setDescription(SHORT_NAME);
			t.setSource("");
		}), o -> update((Template)o, t -> t.setDescription(LONG_NAME)), o -> ((Template)o).getDescription(), SHORT_NAME, MAX_ALLOWED_NAME, false),

		constructKeyword(() -> create(Construct.class, c -> {
			c.setKeyword(LONG_NAME);
			c.getNodes().add(node);
		}), () -> create(Construct.class, c -> {
			c.setKeyword(SHORT_NAME);
			c.getNodes().add(node);
		}), o -> update((Construct)o, c -> c.setKeyword(LONG_NAME)), o -> ((Construct)o).getKeyword(), SHORT_NAME, MAX_ALLOWED_KEYWORD, true),

		contentRepositoryName(() -> create(ContentRepository.class, cr -> {
			cr.setName(LONG_NAME);
		}), () -> create(ContentRepository.class, cr -> {
			cr.setName(SHORT_NAME);
		}), o -> update((ContentRepository)o, cr -> cr.setName(LONG_NAME)), o -> ((ContentRepository)o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		datasourceName(() -> create(Datasource.class, ds -> {
			ds.setName(LONG_NAME);
			ds.setSourceType(Datasource.SourceType.staticDS);
		}), () -> create(Datasource.class, ds -> {
			ds.setName(SHORT_NAME);
			ds.setSourceType(Datasource.SourceType.staticDS);
		}), o -> update((Datasource)o, ds -> ds.setName(LONG_NAME)), o -> ((Datasource)o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		languageName(() -> create(ContentLanguage.class, lang -> {
			lang.setName(LONG_NAME);
			lang.setCode("a");
			lang.getNodes().add(node);
		}), () -> create(ContentLanguage.class, lang -> {
			lang.setName(SHORT_NAME);
			lang.setCode("a");
			lang.getNodes().add(node);
		}), o -> update((ContentLanguage)o, lang -> lang.setName(LONG_NAME)), o -> ((ContentLanguage)o).getName(), SHORT_NAME, MAX_ALLOWED_NAME, true),

		languageCode(() -> create(ContentLanguage.class, lang -> {
			lang.setName(SHORT_NAME);
			lang.setCode(LONG_NAME);
			lang.getNodes().add(node);
		}), () -> create(ContentLanguage.class, lang -> {
			lang.setName(SHORT_NAME);
			lang.setCode(SHORT_CODE);
			lang.getNodes().add(node);
		}), o -> update((ContentLanguage)o, lang -> lang.setCode(LONG_NAME)), o -> ((ContentLanguage)o).getCode(), SHORT_CODE, MAX_ALLOWED_CODE, true),

		objTagDefinitionName(() -> create(ObjectTagDefinition.class, def -> {
			def.setTargetType(Page.TYPE_PAGE);
			def.setName("a", 1);
			ObjectTag objectTag = def.getObjectTag();
			objectTag.setConstructId(constructId);
			objectTag.setName(LONG_NAME);
			objectTag.setObjType(Page.TYPE_PAGE);
			def.getNodes().add(node);
		}), () -> create(ObjectTagDefinition.class, def -> {
			def.setTargetType(Page.TYPE_PAGE);
			def.setName("a", 1);
			ObjectTag objectTag = def.getObjectTag();
			objectTag.setConstructId(constructId);
			objectTag.setName(SHORT_NAME);
			objectTag.setObjType(Page.TYPE_PAGE);
			def.getNodes().add(node);
		}), o -> update((ObjectTagDefinition)o, def -> def.getObjectTag().setName(LONG_NAME)), o -> Trx.supply(() -> ((ObjectTagDefinition)o).getObjectTag().getName()), SHORT_NAME, MAX_ALLOWED_NAME, true);

		/**
		 * Supplier to create an object with too long attribute
		 */
		Supplier<? extends NodeObject> createLong;

		/**
		 * Supplier to create an object with short attribute
		 */
		Supplier<? extends NodeObject> createShort;

		/**
		 * Function to update an object with too long attribute
		 */
		Function<NodeObject, NodeObject> updateLong;

		/**
		 * Function to get the current attribute value
		 */
		Function<NodeObject, String> get;

		/**
		 * Short value
		 */
		String shortValue;

		/**
		 * Max allowed value
		 */
		String maxAllowed;

		/**
		 * Flag whether the attribute is required to be unique
		 */
		boolean unique;

		/**
		 * Create test type instance
		 * @param createLong
		 * @param createShort
		 * @param updateLong
		 * @param get
		 * @param shortValue
		 * @param maxAllowed
		 * @param unique
		 */
		TestType(Supplier<? extends NodeObject> createLong, Supplier<? extends NodeObject> createShort, Function<NodeObject, NodeObject> updateLong,
				Function<NodeObject, String> get, String shortValue, String maxAllowed, boolean unique) {
			this.createLong = createLong;
			this.createShort = createShort;
			this.updateLong = updateLong;
			this.get = get;
			this.shortValue = shortValue;
			this.maxAllowed = maxAllowed;
			this.unique = unique;
		}
	}
}
