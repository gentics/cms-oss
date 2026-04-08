package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.publish.mesh.form.FormPublishTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

@ConfigBy(FormTypeConfigurationModifier.class)
public class AbstractContentStagingTest {

	protected static final String PACKAGE_SUBROOT = "package";

	protected static final String CONTENT_ROOT = "content";

	protected static final String DEFAULT_PACKAGE_DESCRIPTION = "Content Package Number One";

	protected static final String DEFAULT_PACKAGE_PREFIX = "Content Package ";

	public final static String IMAGE_NAME = "image-dpi66x44-res311x211.jpg";

	public static ObjectMapper mapper = MiscUtils.newObjectMapper();

	/**
	 * Database context
	 */
	protected static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	protected static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	protected static UserGroup group;
	protected static SystemUser user;
	protected static Node node1;
	protected static Node node2;
	protected static Node sourceChannel;
	protected static Node targetChannel;
	protected static Page page1;
	protected static Page page2;
	protected static Page page31;
	protected static Page page32;
	protected static Folder folder1;
	protected static Folder folder2;
	protected static Page page11;
	protected static ImageFile image1;
	protected static com.gentics.contentnode.object.File file1;
	protected static Form form1;
	protected static Template template;

	protected static java.io.File dbFilesDir;

	@BeforeClass
	public static void setupOnce() throws Exception {
		{
			System.out.println("Ho!");
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.CONTENT_STAGING.toString().toLowerCase(), true);

			dbFilesDir = Paths.get(ConfigurationValue.CONTENT_PACKAGES_PATH.get()).toFile();
			dbFilesDir.mkdirs();
		}
		
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		operate(t -> {
			for (Page page : t.getObjects(Page.class, DBUtils.select("SELECT id FROM page", DBUtils.IDS))) {
				page.delete(true);
			}
			for (Template template : t.getObjects(Template.class, DBUtils.select("SELECT id FROM template", DBUtils.IDS))) {
				template.delete(true);
			}
			for (ObjectTag objectTag : t.getObjects(ObjectTag.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id != 0", DBUtils.IDS))) {
				objectTag.delete();
			}
			for (ObjectTagDefinition objectProperty : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS))) {
				objectProperty.delete();
			}
			for (Construct construct : t.getObjects(Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDS))) {
				construct.delete();
			}
		});

		node1 = supply(() -> createNode());
		sourceChannel = supply(() -> createChannel(node1, "Source Channel", "source", "/"));
		node2 = supply(() -> createNode());
		targetChannel = supply(() -> createChannel(node2, "Target Channel", "target", "/"));

		Integer noNodeConstructId = supply(() -> createConstruct(null, ShortTextPartType.class, "nonode", "text"));
		Integer node1ConstructId = supply(() -> createConstruct(node1, LongHTMLPartType.class, "node1", "text"));
		Integer node2ConstructId = supply(() -> createConstruct(node2, CheckboxPartType.class, "node2", "text"));
		Integer bothNodeConstructId = supply(t -> {
			Integer constructId = createConstruct(null, OverviewPartType.class, "both", "ds");
			update(t.getObject(Construct.class, constructId), c -> {
				c.getNodes().add(node1);
				c.getNodes().add(node2);
			});
			return constructId;
		});

		template = supply(() -> createTemplate(node1.getFolder(), "Template"));

		page1 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node1.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1");

			p.getContent().addContentTag(noNodeConstructId).setName("nonode1");
			p.getContent().addContentTag(node2ConstructId).setName("node21");
		}));

		page2 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 2");
			p.getContent().addContentTag(node1ConstructId).setName("node11");
			p.getContent().addContentTag(bothNodeConstructId).setName("both1");
		}));
		page2 = execute(p -> update(p, upd -> {
			Overview overview = getPartType(OverviewPartType.class, upd.getTag("both1"), "ds").getOverview();
			overview.setObjectType(Page.TYPE_PAGE);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			overview.setMaxObjects(3);
			overview.setRecursion(true);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			OverviewHelper.addOverviewEntriesToOverview(overview, List.of(page1.getId()));
		}), page2);

		folder1 = supply(() -> create(Folder.class, f -> {
			f.setMotherId(node1.getFolder().getId());
			f.setName("Folder 1");
		}));

		folder2 = supply(() -> create(Folder.class, f -> {
			f.setMotherId(node1.getFolder().getId());
			f.setName("Folder 2");
		}));

		page11 = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1 of Folder 1");

			p.getContent().addContentTag(noNodeConstructId).setName("nonode1");
			p.getContent().addContentTag(node2ConstructId).setName("node21");
		}));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(ImageResourceTest.class.getResourceAsStream(IMAGE_NAME), out);
		image1 = (ImageFile) supply(() -> createImage(node1.getFolder(), IMAGE_NAME, out.toByteArray()));
		file1 = supply(() -> ContentNodeTestDataUtils.createFile(folder1, "blubb.txt", "Original content".getBytes(), node1));
		form1 = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node1.getFolder().getId());

			try {
				f.setData(mapper.readValue(FormPublishTest.class.getResource("generic_form_data.json"), ObjectNode.class));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}));

		page31 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 31");
			p.getContent().addContentTag(node1ConstructId).setName("node11");
			p.getContent().addContentTag(bothNodeConstructId).setName("both1");
		}));
		page32 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 32");
			p.getContent().addContentTag(node1ConstructId).setName("node11");
			p.getContent().addContentTag(bothNodeConstructId).setName("both1");
		}));
		page31 = execute(p -> update(p, upd -> {
			Overview overview = getPartType(OverviewPartType.class, upd.getTag("both1"), "ds").getOverview();
			overview.setObjectType(Page.TYPE_PAGE);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			overview.setMaxObjects(3);
			overview.setRecursion(true);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			OverviewHelper.addOverviewEntriesToOverview(overview, List.of(upd.getId(), page32.getId()));
		}), page31);
		page32 = execute(p -> update(p, upd -> {
			Overview overview = getPartType(OverviewPartType.class, upd.getTag("both1"), "ds").getOverview();
			overview.setObjectType(Page.TYPE_PAGE);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			overview.setMaxObjects(3);
			overview.setRecursion(true);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			OverviewHelper.addOverviewEntriesToOverview(overview, List.of(upd.getId(), page31.getId()));
		}), page32);
	}

	@AfterClass
	public static void cleanup() throws IOException, NodeException {
		File folder = new File(ConfigurationValue.CONTENT_PACKAGES_PATH.get());
		if (folder.exists()) FileUtils.deleteDirectory(folder);
		//folder.mkdirs();
	}

	protected ContentPackageLoadResponse directCreate(String name, String description) throws Exception {
		StagedContentPackage model = new StagedContentPackage();
		model.setName(name);
		model.setDescription(description);
		int start = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
		ContentPackageLoadResponse response = supply(user,
				() -> new ContentStagingResourceImpl().create(model));
		assertContentPackageResponse(response, start, start + 60, name, description);
		return response;
	}

	protected ContentPackageLoadResponse directRead(String name) throws Exception {
		return supply(user,	() -> new ContentStagingResourceImpl().read(name));
	}

	protected void assertContentPackageResponse(ContentPackageLoadResponse response, int start, int end, String name, String description) throws NodeException {
		String sanitized = supply(() -> FileFactory.sanitizeName(name));
		assertResponseOK(response);
		assertNotNull(response.getContentPackage());
		assertThat(response.getContentPackage().getName()).isEqualTo(sanitized);
		assertThat(response.getContentPackage().getDescription()).isEqualTo(description);
		assertThat(response.getContentPackage().getTimestamp()).isNotNull().isNotZero().isNotNegative().isBetween(start, end);
	}
}
