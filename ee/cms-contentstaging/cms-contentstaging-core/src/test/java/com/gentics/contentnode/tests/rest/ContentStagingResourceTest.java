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
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createformDataBodyPart;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Condition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
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
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.publish.mesh.form.FormPublishTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingResourceTest {

	private static final String PACKAGE_SUBROOT = "package";

	private static final String CONTENT_ROOT = "content";

	private static final String DEFAULT_PACKAGE_DESCRIPTION = "Content Package Number One";

	private static final String DEFAULT_PACKAGE_PREFIX = "Content Package ";

	public final static String IMAGE_NAME = "image-dpi66x44-res311x211.jpg";

	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	/**
	 * Database context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup group;
	private static SystemUser user;
	private static Node node1;
	private static Node node2;
	private static Node sourceChannel;
	private static Node targetChannel;
	private static Page page1;
	private static Page page2;
	private static Page page31;
	private static Page page32;
	private static Folder folder1;
	private static Page page11;
	private static ImageFile image1;
	private static com.gentics.contentnode.object.File file1;
	private static Form form1;

	private static java.io.File dbFilesDir;

	@BeforeClass
	public static void setupOnce() throws Exception {
		{
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.CONTENT_STAGING.toString().toLowerCase(), true);

			dbFilesDir = new java.io.File(
					prefs.getProperty("filepath") + java.io.File.separator + CONTENT_ROOT + java.io.File.separator + "content-packages");
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

		Template template = supply(() -> createTemplate(node1.getFolder(), "Template"));

		page1 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node1.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1");

			p.getContent().addContentTag(noNodeConstructId);
			p.getContent().addContentTag(node2ConstructId);
		}));

		page2 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 2");
			p.getContent().addContentTag(node1ConstructId);
			p.getContent().addContentTag(bothNodeConstructId);
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

		page11 = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1 of Folder 1");

			p.getContent().addContentTag(noNodeConstructId);
			p.getContent().addContentTag(node2ConstructId);
		}));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(ImageResourceTest.class.getResourceAsStream(IMAGE_NAME), out);
		image1 = (ImageFile) supply(() -> createImage(node1.getFolder(), IMAGE_NAME, out.toByteArray()));
		file1 = supply(() -> ContentNodeTestDataUtils.createFile(folder1, "blubb.txt", "Original content".getBytes(), node1));
		form1 = supply(() -> create(Form.class, f -> {
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node1.getFolder().getId());

			try {
				f.setData(mapper.readValue(FormPublishTest.class.getResource("form_data.json"), ObjectNode.class));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}));

		page31 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 31");
			p.getContent().addContentTag(node1ConstructId);
			p.getContent().addContentTag(bothNodeConstructId);
		}));
		page32 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node2.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 32");
			p.getContent().addContentTag(node1ConstructId);
			p.getContent().addContentTag(bothNodeConstructId);
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

	@Test
	public void testList() throws Exception {
		ContentPackageListResponse response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).request().get(ContentPackageListResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));

		assertResponseOK(response);
		assertThat(response.getItems()).as("Content Packages").isEmpty();

		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		directCreate("Content Package 2", "Content Package Number Two");

		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).request().get(ContentPackageListResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));

		assertResponseOK(response);
		assertThat(response.getItems()).as("Content Packages").hasSize(2);
	}

	@Test
	public void testCreate() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		StagedContentPackage model = new StagedContentPackage();
		model.setName(packageNameHuman);
		model.setDescription(DEFAULT_PACKAGE_DESCRIPTION);
		int start = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
		ContentPackageLoadResponse response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).request().put(Entity.json(model), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_CREATE));
		assertContentPackageResponse(response, start, start + 60, packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
	}

	@Test(expected = ClientErrorException.class)
	public void testCreateNameBusy() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);

		StagedContentPackage model = new StagedContentPackage();
		model.setName(packageNameHuman);
		model.setDescription(DEFAULT_PACKAGE_DESCRIPTION);
		assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).request().put(Entity.json(model), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_CREATE));
	}

	@Test
	public void testRead() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		String sanitizedName = supply(() -> FileFactory.sanitizeName(packageNameHuman));
		try {
			assertRequiredPermissions(group, "tester", "tester", restContext, 
					target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(sanitizedName).request().get(ContentPackageLoadResponse.class), 
					ResponseCode.NOTFOUND,
					Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
					Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
					Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));
			fail();
		} catch (NotFoundException e) {
		}

		int start = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
		directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		ContentPackageLoadResponse response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(sanitizedName).request().get(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));
		assertContentPackageResponse(response, start, start + 120, packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
	}

	@Test
	public void testUpdateSameName() throws Exception {
		testUpdate("One Number Package Content", Optional.empty());
	}

	@Test
	public void testUpdateRename() throws Exception {
		testUpdate("One Package Number Content", Optional.of("1 Package Content"));
	}

	@Test(expected = EntityNotFoundException.class)
	public void testDelete() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse cr = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String sanitizedName = cr.getContentPackage().getName();
		GenericResponse response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(sanitizedName).request().delete(GenericResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_DELETE));
		assertResponseOK(response);
		directRead(sanitizedName);
	}

	@Test
	public void testManageFolder() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		NodeObject folder11 = supply(() -> folder1.getParentObject());

		// Add non recursively
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("folder").path(folder1Id)
					.request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder11, PermType.read));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Remove
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("folder").path(folder1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Remove again (double delete test)
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("folder").path(folder1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Add recursively
		// REST API call currently fails because of omissive behavior of adding the inaccessible recursive content
//		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
//				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("folder").path(folder1Id)
//					.request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
//				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
//				ContentNodeTestUtils.perm(folder1, PermType.read),
//				ContentNodeTestUtils.perm(folder11, PermType.read),
//				//ContentNodeTestUtils.perm(page11, PermType.readitems),
//				ContentNodeTestUtils.perm(page11, PermType.read));
		response = supply(() -> {
			PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString());
			PermHandler.setPermissions(com.gentics.contentnode.object.File.TYPE_FILE, file1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString());
			PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());
			PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString());
			return new ContentStagingResourceImpl().add(packageNameInternal, "folder", folder1Id, true);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
	}

	@Test
	public void testManagePage() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page11.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page11.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String page11Id = supply(() -> page11.getGlobalId().toString());
		Folder folder1 = supply(() -> page11.getFolder());
		Folder node11 = supply(() -> page11.getNode().getFolder());

		// Add page
//		response = supply(() -> {
//			return new ContentStagingResourceImpl().add(packageName, "page", page11Id, false);
//		});
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("page").path(page11Id)
					.request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(node11, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status
		{
			PageLoadResponse pageLoad = supply(() -> {
				return new PageResourceImpl().load(page11Id, false, false, false, false, false, false, false, false, false, true, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
			//assertThat(pageLoad.getStagingStatus().isRecent()).isTrue();
		}
		// Remove page
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("page").path(page11Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status again
		{
			PageLoadResponse pageLoad = supply(() -> {
				return new PageResourceImpl().load(page11Id, false, false, false, false, false, false, false, false, false, true, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
		}
	}

	@Test
	public void testManageNode() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String node1Id = supply(() -> node1.getGlobalId().toString());
		Folder folder1 = supply(() -> node1.getFolder());

		// Add node
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("node").path(node1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read));
//		response = supply(() -> {
//			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
//		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Remove node
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("node").path(node1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(0);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(0);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
	}

	@Test
	public void testManageFile() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String file1Id = supply(() -> file1.getGlobalId().toString());
		NodeObject folder11 = supply(() -> folder1.getParentObject());

		// Add file
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("file").path(file1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems),
				ContentNodeTestUtils.perm(folder11, PermType.read));

//		response = supply(() -> {
//			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
//		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status
		{
			FileLoadResponse pageLoad = supply(() -> {
				return new FileResourceImpl().load(file1Id, false, false, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
		}
		// Remove file
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("file").path(file1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status again
		{
			FileLoadResponse pageLoad = supply(() -> {
				return new FileResourceImpl().load(file1Id, false, false, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
		}				
	}

	@Test
	public void testManageImage() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(ImageFile.TYPE_IMAGE, image1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, image1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String img1Id = supply(() -> image1.getGlobalId().toString());
		Folder folder1 = supply(() -> image1.getFolder());

		// Add image
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("image").path(img1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems));

//		response = supply(() -> {
//			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
//		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status
		{
			ImageLoadResponse pageLoad = supply(() -> {
				return new ImageResourceImpl().load(img1Id, false, false, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
		}
		// Remove image
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("image").path(img1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.readitems));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status again
		{
			ImageLoadResponse pageLoad = supply(() -> {
				return new ImageResourceImpl().load(img1Id, false, false, node1.getId(), packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
		}				
	}

	@Test
	public void testManageForm() throws Exception {
		operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.FORMS.toString().toLowerCase(), true);
			node1.activateFeature(Feature.FORMS);
		});
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String form1Id = supply(() -> form1.getGlobalId().toString());
		Folder folder1 = supply(() -> form1.getFolder());

		// Add form
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("form").path(form1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.viewform));

//		response = supply(() -> {
//			return new ContentStagingResourceImpl().add(packageName, "page", page11Id, false);
//		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		// Staging status
		{
			FormResponse pageLoad = supply(() -> {
				return new FormResourceImpl().get(form1Id, packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
			//assertThat(pageLoad.getStagingStatus().isRecent()).isTrue();
		}

		// Remove form
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageNameInternal).path("form").path(form1Id).request().delete(ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT),
				ContentNodeTestUtils.perm(folder1, PermType.read),
				ContentNodeTestUtils.perm(folder1, PermType.viewform));
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(0);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Staging status again
		{
			FormResponse pageLoad = supply(() -> {
				return new FormResourceImpl().get(form1Id, packageNameInternal);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
		}
	}

	@Test
	public void testExport() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageNameInternal, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageNameInternal);
		});	
//		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
//				target -> target.path("content").path("package").path(packageName).path("export").request().post(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
//				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)/*,
//				Triple.of(Node.TYPE_NODE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Page.TYPE_PAGE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Folder.TYPE_FOLDER, 1, PermHandler.PERM_VIEW)*/);
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);
	}

	@Test
	public void testImport() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		assertResponseOK(new ContentStagingResourceImpl().importIntoCms(packageNameInternal, 0));
		response = supply(() -> new ContentStagingResourceImpl().read(packageNameInternal));
//		status = assertRequiredPermissions(group, "tester", "tester", restContext, 
//				target -> target.path("content").path("package").path(packageName).path("import").request().post(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
//				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)/*,
//				Triple.of(Node.TYPE_NODE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Page.TYPE_PAGE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Folder.TYPE_FOLDER, 1, PermHandler.PERM_VIEW)*/);
		while (response.getImport() != null && response.getImport().isRunning()) {
			response = supply(() -> new ContentStagingResourceImpl().read(packageNameInternal));
		}
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);
	}

	@Test
	public void testSelfReferencedOverview() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page31.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page31.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page32.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page32.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "folder", node2.getFolder().getGlobalId().toString(), true);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(3);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		supply(() -> new PageResourceImpl().delete(page31.getGlobalId().toString(), page31.getNode().getId()));
		supply(() -> new PageResourceImpl().delete(page32.getGlobalId().toString(), page32.getNode().getId()));

		assertThatThrownBy(() -> supply(() -> PageResourceImpl.getPage(page31.getGlobalId().toString(), ObjectPermission.view))).isInstanceOf(EntityNotFoundException.class);
		assertThatThrownBy(() -> supply(() -> PageResourceImpl.getPage(page32.getGlobalId().toString(), ObjectPermission.view))).isInstanceOf(EntityNotFoundException.class);

		supply(() -> new ContentStagingResourceImpl().importIntoCms(packageNameInternal, 5000));

		page31 = supply(() -> PageResourceImpl.getPage(page31.getGlobalId().toString(), ObjectPermission.view));
		page32 = supply(() -> PageResourceImpl.getPage(page32.getGlobalId().toString(), ObjectPermission.view));
		assertNotNull(page31);
		assertNotNull(page32);

		operate(() -> {
			Overview overview31 = getPartType(OverviewPartType.class, page31.getTag("both1"), "ds").getOverview();
			assertEquals("Page 31 does not contain expected number of items", 2, overview31.getOverviewEntries().size());
			assertFalse("Page 31 contains no expected values [Page 31] [Page 32]", overview31.getOverviewEntries().stream().anyMatch(entry -> entry.getObjectId().intValue() != page31.getId().intValue() && entry.getObjectId().intValue() != page32.getId().intValue()));
		});
		operate(() -> {
			Overview overview32 = getPartType(OverviewPartType.class, page32.getTag("both1"), "ds").getOverview();
			assertEquals("Page 32 does not contain expected number of items", 2, overview32.getOverviewEntries().size());
			assertFalse("Page 32 contains no expected values [Page 31] [Page 32]", overview32.getOverviewEntries().stream().anyMatch(entry -> entry.getObjectId().intValue() != page31.getId().intValue() && entry.getObjectId().intValue() != page32.getId().intValue()));
		});
	}

	@Test
	public void testDownloadUpload() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);
		
//		Response file = assertRequiredPermissions(group, "tester", "tester", restContext, 
//				target -> target.path("content").path("package").path(packageName).path("zip").request().get(Response.class), 
//				(r, c) -> {
//					assertThat(r.getStatus()).isEqualTo(c);
//				},
//				409,
//				200,
//				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
//				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_MODIFYCONTENT)/*,
//				Triple.of(Node.TYPE_NODE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Page.TYPE_PAGE, 1, PermHandler.PERM_VIEW),
//				Triple.of(Folder.TYPE_FOLDER, 1, PermHandler.PERM_VIEW)*/);
		File file = supply(() -> {
			try {
				File outputZip = File.createTempFile(Long.toString(System.currentTimeMillis()), ".zip");
				FileOutputStream fos = new FileOutputStream(outputZip);
				((StreamingOutput) new ContentStagingResourceImpl().download(packageNameInternal).getEntity()).write(fos);
				fos.close();
				return outputZip;
			} catch (Throwable e) {
				if (e.getCause() instanceof NodeException) {
					throw (NodeException) e.getCause();
				} else {
					throw new IllegalStateException(e);
				}
			}
		});
		assertThat(file.length()).isGreaterThan(0);
		try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
			assertThat(zip.available()).isGreaterThan(0);
		}
		MultiPart uploadMultiPart = null;
		try (Trx trx = new Trx(createSession(user.getLogin()), true);
				PropertyTrx localServer = new PropertyTrx("cn_local_server", "http://localhost:" + restContext.getPort());
				FormDataMultiPart multiPart = new FormDataMultiPart()) {
			multiPart.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_ID_QUERY_PARAM_NAME + "\"",
							trx.getTransaction().getSessionId(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_SECRET_COOKIE_NAME + "\"",
							trx.getTransaction().getSession().getSessionSecret(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"fileName\"", file.getName(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"fileBinaryData\"; filename=\"" + file.getName() + "\"",
							file, MediaType.APPLICATION_OCTET_STREAM_TYPE));
			ContentPackageLoadResponse fileUploadResponse = new ContentStagingResourceImpl().upload(packageNameInternal, multiPart);
			assertResponseOK(fileUploadResponse);
		} finally {
			if (uploadMultiPart != null) {
				uploadMultiPart.close();
			}
		}

		// Create another package
		response = directCreate("Another content package", "Another package description");
		String newPackageName = response.getContentPackage().getName();
		// Upload existing package under a new name
		try (Trx trx = new Trx(createSession(user.getLogin()), true);
				PropertyTrx localServer = new PropertyTrx("cn_local_server", "http://localhost:" + restContext.getPort());
				FormDataMultiPart multiPart = new FormDataMultiPart()) {
			multiPart.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_ID_QUERY_PARAM_NAME + "\"",
							trx.getTransaction().getSessionId(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"" + SessionToken.SESSION_SECRET_COOKIE_NAME + "\"",
							trx.getTransaction().getSession().getSessionSecret(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"fileName\"", file.getName(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"fileBinaryData\"; filename=\"" + file.getName() + "\"",
							file, MediaType.APPLICATION_OCTET_STREAM_TYPE));
			ContentPackageLoadResponse fileUploadResponse = new ContentStagingResourceImpl().upload(newPackageName, multiPart);
			assertResponseOK(fileUploadResponse);
		} finally {
			if (uploadMultiPart != null) {
				uploadMultiPart.close();
	}
		}
		ContentPackageListResponse list = supply(() -> {
			return new ContentStagingResourceImpl().list(null, null, null);
		});
		assertThat(list.getItems())
			.isNotEmpty()
			.hasSize(7)
			.areAtMost(1, new Condition<StagedContentPackage>(pkg -> pkg.getName().equals(newPackageName), "names match"))
			.areAtMost(1, new Condition<StagedContentPackage>(pkg -> pkg.getName().equals(packageNameInternal), "names match"));
	}

	@Test
	public void testStatus() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page11.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page11.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String page11Id = supply(() -> page1.getGlobalId().toString());
		String page21Id = supply(() -> page2.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());		

		// Add 'em all 
		ContentStagingResourceImpl impl = new ContentStagingResourceImpl();
		response = supply(() -> {
			impl.add(packageNameInternal, "page", page11Id, false);
			//impl.add(packageName, "page", page21Id, false);
			impl.add(packageNameInternal, "form", form1Id, false);
			impl.add(packageNameInternal, "file", file1Id, false);
			return impl.add(packageNameInternal, "image", img1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);
		assertThat(response.getContentPackage().getChannels()).isEqualTo(0);
	
		StagingStatusResponse status = supply(() -> {
			IdTypeMapRequest request = new IdTypeMapRequest();
			Map<String, List<String>> ids = new HashMap<>();
			ids.put("page", Arrays.asList(page11Id, page21Id));
			ids.put("form", Collections.singletonList(form1Id));
			ids.put("file", Collections.singletonList(file1Id));
			ids.put("image", Collections.singletonList(img1Id));
			request.setIds(ids);
			return impl.status(packageNameInternal, request);
		});
		assertResponseOK(status);
		assertThat(status.getStagingStatus().get(img1Id)).isNotNull().matches(s -> s.isIncluded());
		assertThat(status.getStagingStatus().get(file1Id)).isNotNull().matches(s -> s.isIncluded());
		assertThat(status.getStagingStatus().get(form1Id)).isNotNull().matches(s -> s.isIncluded());
		assertThat(status.getStagingStatus().get(page11Id)).isNotNull().matches(s -> s.isIncluded());
		assertThat(status.getStagingStatus().get(page21Id)).isNotNull().matches(s -> !s.isIncluded());
	}

	@Test
	public void testPageChannel() throws Exception {
		Page page22 = supply(() -> localize(page2, targetChannel));
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageNameInternal = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page11.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page11.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String page21Id = supply(() -> page2.getGlobalId().toString());
		String page22Id = supply(() -> page22.getGlobalId().toString());

		// Add page
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "page", page21Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
		assertThat(response.getContentPackage().getChannels()).isEqualTo(0);
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageNameInternal, "page", page22Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(2);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
		assertThat(response.getContentPackage().getChannels()).isEqualTo(1);
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageNameInternal, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageNameInternal);
		});
		assertResponseOK(new ContentStagingResourceImpl().importIntoCms(packageNameInternal, 0));
		response = supply(() -> new ContentStagingResourceImpl().read(packageNameInternal));
		while (response.getImport() != null && response.getImport().isRunning()) {
			response = supply(() -> new ContentStagingResourceImpl().read(packageNameInternal));
		}
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(2);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
		assertThat(response.getContentPackage().getChannels()).isEqualTo(1);
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

	protected void testUpdate(String description, Optional<String> maybeNewName) throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);

		String newName = maybeNewName.orElse(response.getContentPackage().getName());
		String sanitizedName = supply(() -> FileFactory.sanitizeName(packageNameHuman));
		StagedContentPackage model = new StagedContentPackage();
		model.setName(newName);
		model.setDescription(description);

		int start = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(sanitizedName).request().post(Entity.json(model), ContentPackageLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_UPDATE));

		assertContentPackageResponse(response, start, start + 60, newName, description);
		assertContentPackageResponse(directRead(supply(() -> FileFactory.sanitizeName(newName))), start, start + 120, newName, description);
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
