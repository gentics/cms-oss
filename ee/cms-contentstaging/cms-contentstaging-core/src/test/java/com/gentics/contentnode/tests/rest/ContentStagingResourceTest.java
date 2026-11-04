package com.gentics.contentnode.tests.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.NodeFeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.response.ContentPackageFolderResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.model.staging.StagedError;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ContentPackageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.staging.StagingErrorKind;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Condition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.uri.UriComponent;
import org.junit.After;
import org.junit.Test;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createformDataBodyPart;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.MULTICHANNELLING })
public class ContentStagingResourceTest extends AbstractContentStagingTest {

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

	@After
	public void after() throws IOException, NodeException {
		cleanup();
	}

	@Test
	public void testPaginatedList() throws Exception {
		for (int i = 0; i <= 100; i++) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		}

		for (int i = 0; i <= 11; i++) {
			final int ii = i;
			ContentPackageListResponse response = assertRequiredPermissions(group, "tester", "tester", restContext, 
					target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).queryParam("pageSize", 10).queryParam("page", ii).request().get(ContentPackageListResponse.class), 
					Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
					Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
					Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));

			assertResponseOK(response);
			assertThat(response.getItems()).as("Content Packages " + i).hasSize(i == 11 ? 1 : 10);
			assertThat(response.getHasMoreItems()).as("More items " + i).isEqualTo(i < 11);
			assertThat(response.getNumItems()).as("Total items " + i).isEqualTo(101);
		}
	}

	@Test
	public void testFilteredList() throws Exception {
		for (int i = 0; i <= 100; i++) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			directCreate(packageNameHuman, i + "th " + DEFAULT_PACKAGE_DESCRIPTION);
		}

		for (int i = 0; i <= 5; i++) {
			ContentPackageListResponse response = new ContentStagingResourceImpl().list(
					new FilterParameterBean().setQuery("8th "), null,
					new PagingParameterBean().setPage(i).setPageSize(2), new ContentPackageListParameterBean());
			assertResponseOK(response);
			assertThat(response.getItems()).as("Content Packages " + i).hasSize(i <= 5 ? 2 : 0);
			assertThat(response.getHasMoreItems()).as("More items " + i).isEqualTo(i < 5);
			assertThat(response.getNumItems()).as("Total items " + i).isEqualTo(10);
		}
	}

	@Test
	public void testSortedList() throws Exception {
		for (int i = 0; i <= 100; i++) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			directCreate(packageNameHuman, i + "th " + DEFAULT_PACKAGE_DESCRIPTION);
		}

		for (int i = 0; i <= 11; i++) {
			ContentPackageListResponse response = new ContentStagingResourceImpl().list(null,
					new SortParameterBean().setSort("-description"),
					new PagingParameterBean().setPage(i).setPageSize(10), new ContentPackageListParameterBean());
			assertResponseOK(response);
			assertThat(response.getHasMoreItems()).as("More items " + i).isEqualTo(i < 11);
			assertThat(response.getNumItems()).as("Total items " + i).isEqualTo(101);
			assertThat(response.getItems()).as("Content Packages " + i).hasSize(i == 11 ? 1 : 10).isSortedAccordingTo((a,b) -> b.getDescription().compareTo(a.getDescription()));
		}
	}

	@Test
	public void testFilteredSortedList() throws Exception {
		for (int i = 0; i <= 100; i++) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			directCreate(packageNameHuman, i + "th " + DEFAULT_PACKAGE_DESCRIPTION);
		}

		for (int i = 0; i <= 5; i++) {
			ContentPackageListResponse response = new ContentStagingResourceImpl().list(
					new FilterParameterBean().setQuery("8th "), new SortParameterBean().setSort("-description"),
					new PagingParameterBean().setPage(i).setPageSize(2), new ContentPackageListParameterBean());
			assertResponseOK(response);
			assertThat(response.getHasMoreItems()).as("More items " + i).isEqualTo(i < 5);
			assertThat(response.getNumItems()).as("Total items " + i).isEqualTo(10);
			assertThat(response.getItems()).as("Content Packages " + i).hasSize(i <= 5 ? 2 : 0).isSortedAccordingTo((a,b) -> b.getDescription().compareTo(a.getDescription()));
		}
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
		String packageName = response.getContentPackage().getName();
		Integer timestamp = response.getContentPackage().getTimestamp();
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		NodeObject folder11 = supply(() -> folder1.getParentObject());

		// Add non recursively
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("folder").path(folder1Id)
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Remove
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("folder").path(folder1Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Remove again (double delete test)
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("folder").path(folder1Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

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
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();
	}

	@Test
	public void testManagePage() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		Integer timestamp = response.getContentPackage().getTimestamp();
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
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("page").path(page11Id)
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status
		{
			PageLoadResponse pageLoad = supply(() -> {
				return new PageResourceImpl().load(page11Id, false, false, false, false, false, false, false, false, false, true, node1.getId(), packageName);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
			//assertThat(pageLoad.getStagingStatus().isRecent()).isTrue();
		}
		// Remove page
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("page").path(page11Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status again
		{
			PageLoadResponse pageLoad = supply(() -> {
				return new PageResourceImpl().load(page11Id, false, false, false, false, false, false, false, false, false, true, node1.getId(), packageName);
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
		String packageName = response.getContentPackage().getName();
		Integer timestamp = response.getContentPackage().getTimestamp();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String node1Id = supply(() -> node1.getGlobalId().toString());
		Folder folder1 = supply(() -> node1.getFolder());

		// Add node
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("node").path(node1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Remove node
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("node").path(node1Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();
	}

	@Test
	public void testManageFile() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		Integer timestamp = response.getContentPackage().getTimestamp();
		String packageName = response.getContentPackage().getName();
//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String file1Id = supply(() -> file1.getGlobalId().toString());
		NodeObject folder11 = supply(() -> folder1.getParentObject());

		// Add file
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("file").path(file1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status
		{
			FileLoadResponse pageLoad = supply(() -> {
				return new FileResourceImpl().load(file1Id, false, false, node1.getId(), packageName);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
		}
		// Remove file
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("file").path(file1Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status again
		{
			FileLoadResponse pageLoad = supply(() -> {
				return new FileResourceImpl().load(file1Id, false, false, node1.getId(), packageName);
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
		String packageName = response.getContentPackage().getName();
		Integer timestamp = response.getContentPackage().getTimestamp();
//		operate(() -> PermHandler.setPermissions(ImageFile.TYPE_IMAGE, image1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, image1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String img1Id = supply(() -> image1.getGlobalId().toString());
		Folder folder1 = supply(() -> image1.getFolder());

		// Add image
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("image").path(img1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status
		{
			ImageLoadResponse pageLoad = supply(() -> {
				return new ImageResourceImpl().load(img1Id, false, false, node1.getId(), packageName);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
		}
		// Remove image
		response = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("image").path(img1Id).request().delete(ContentPackageLoadResponse.class), 
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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
		timestamp = response.getContentPackage().getTimestamp();

		// Staging status again
		{
			ImageLoadResponse pageLoad = supply(() -> {
				return new ImageResourceImpl().load(img1Id, false, false, node1.getId(), packageName);
			});
			assertResponseOK(pageLoad);
			assertThat(pageLoad.getStagingStatus()).isNotNull();
			assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
		}				
	}

	@Test
	public void testManageForm() throws Exception {
		try (var formsClosure = new NodeFeatureClosure(Feature.FORMS, node1, true)) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
			String packageName = response.getContentPackage().getName();
			Integer timestamp = response.getContentPackage().getTimestamp();
	//		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
	//		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
	//		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
			String form1Id = supply(() -> form1.getGlobalId().toString());
			Folder folder1 = supply(() -> form1.getFolder());

			// Add form
			response = assertRequiredPermissions(group, "tester", "tester", restContext,
					target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("form").path(form1Id).request().put(Entity.json(StringUtils.EMPTY), ContentPackageLoadResponse.class),
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
			assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
			timestamp = response.getContentPackage().getTimestamp();

			// Staging status
			{
				FormResponse pageLoad = supply(() -> {
					return new FormResourceImpl().get(form1Id, null, packageName);
				});
				assertResponseOK(pageLoad);
				assertThat(pageLoad.getStagingStatus()).isNotNull();
				assertThat(pageLoad.getStagingStatus().isIncluded()).isTrue();
				//assertThat(pageLoad.getStagingStatus().isRecent()).isTrue();
			}

			// Remove form
			response = assertRequiredPermissions(group, "tester", "tester", restContext,
					target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("form").path(form1Id).request().delete(ContentPackageLoadResponse.class),
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
			assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
			timestamp = response.getContentPackage().getTimestamp();

			// Staging status again
			{
				FormResponse pageLoad = supply(() -> {
					return new FormResourceImpl().get(form1Id, null, packageName);
				});
				assertResponseOK(pageLoad);
				assertThat(pageLoad.getStagingStatus()).isNotNull();
				assertThat(pageLoad.getStagingStatus().isIncluded()).isFalse();
			}
		}
	}

	@Test
	public void testExport() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
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

	/**
	 * Test that a failing import responds with an HTTP error status, and that the response contains the cause.
	 * The package contains a node with activated feature "forms", which is generally deactivated, when the package is imported.
	 * @throws Exception
	 */
	@Test
	public void testFailingImportRespondsWithErrorStatus() throws Exception {
		try (var formsFeature = new NodeFeatureClosure(Feature.FORMS, node1, true)) {
			String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
			String packageName = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION).getContentPackage().getName();
			String node1Id = supply(() -> node1.getGlobalId().toString());
			assertResponseOK(supply(() -> new ContentStagingResourceImpl().add(packageName, "node", node1Id, false)));
			assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));

			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, false); LoggedInClient client = systemClient()) {
				Response response = client.get().base().path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("import")
						.request().post(Entity.json(StringUtils.EMPTY));
				assertThat(response.getStatusInfo().toEnum()).as("Response status").isEqualTo(Status.INTERNAL_SERVER_ERROR);

				GenericResponse importResponse = response.readEntity(GenericResponse.class);
				assertThat(importResponse.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(ResponseCode.FAILURE);
				assertThat(importResponse.getResponseInfo().getResponseMessage()).as("Response message")
						.contains("Cannot activate feature forms", "feature is not available");
			}
		}
	}

	/**
	 * Test that an import, which fails because a referenced object does not exist, responds with HTTP status 404
	 * The package contains a page, which uses a template that is deleted, before the package is imported.
	 * @throws Exception
	 */
	@Test
	public void testImportMissingReferenceRespondsWithNotFound() throws Exception {
		Template testedTemplate = supply(() -> createTemplate(node1.getFolder(), "Tested Template"));
		Page stagedPage = supply(() -> create(Page.class, p -> {
			p.setFolderId(node1.getFolder().getId());
			p.setTemplateId(testedTemplate.getId());
			p.setName("Staged Page");
		}));
		String templateId = supply(() -> testedTemplate.getGlobalId().toString());

		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		String packageName = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION).getContentPackage().getName();
		String stagedPageId = supply(() -> stagedPage.getGlobalId().toString());
		assertResponseOK(supply(() -> new ContentStagingResourceImpl().add(packageName, "page", stagedPageId, false)));

		// delete the staged page and the template
		operate(t -> t.getObject(Page.class, stagedPage.getId()).delete(true));
		operate(t -> t.getObject(Template.class, testedTemplate.getId()).delete(true));

		try (LoggedInClient client = systemClient()) {
			Response response = client.get().base().path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("import")
					.request().post(Entity.json(StringUtils.EMPTY));
			assertThat(response.getStatusInfo().toEnum()).as("Response status").isEqualTo(Status.NOT_FOUND);

			GenericResponse importResponse = response.readEntity(GenericResponse.class);
			String responseMessage = String.format("Could not find template with id %s", templateId);
			String message = supply(() -> I18NHelper.get("template.notfound", templateId));
			assertResponse(importResponse, ResponseCode.NOTFOUND, responseMessage, new Message(Message.Type.WARNING, message));
			assertThat(importResponse.getResponseInfo().getResponseMessage()).as("Response message").isEqualTo(responseMessage);
		}
	}

	/**
	 * Set the password of the system user and get a client, which is logged in as system user
	 * @return logged in client
	 * @throws Exception
	 */
	protected LoggedInClient systemClient() throws Exception {
		SystemUser systemUser = supply(t -> t.getObject(SystemUser.class, 1));
		update(systemUser, upd -> {
			upd.setPassword(SystemUserFactory.hashPassword("system", upd.getId()));
		}).build();
		return restContext.client("system", "system");
	}

	@Test
	public void testListFolder() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		ContentPackageFolderResponse folderResponse = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("list").path("/").request().get(ContentPackageFolderResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));
		assertResponseOK(folderResponse);
		assertThat(folderResponse.getNodes()).hasSize(1);
		assertThat(folderResponse.getFolders()).hasSize(0);
		assertThat(folderResponse.getPages()).hasSize(0);
		assertThat(folderResponse.getImages()).hasSize(0);
		assertThat(folderResponse.getFiles()).hasSize(0);
		assertThat(folderResponse.getForms()).hasSize(0);

		folderResponse = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("list")
					.path(supply(() -> node1.getFolder().getName())).request().get(ContentPackageFolderResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));
		assertResponseOK(folderResponse);
		assertThat(folderResponse.getNodes()).hasSize(0);
		assertThat(folderResponse.getFolders()).hasSize(1);
		assertThat(folderResponse.getPages()).hasSize(0);
		assertThat(folderResponse.getImages()).hasSize(1);
		assertThat(folderResponse.getFiles()).hasSize(0);
		assertThat(folderResponse.getForms()).hasSize(1);

		folderResponse = assertRequiredPermissions(group, "tester", "tester", restContext, 
				target -> target.path(CONTENT_ROOT).path(PACKAGE_SUBROOT).path(packageName).path("list")
					.path(supply(() -> node1.getFolder().getName())).path(supply(() -> folder1.getName())).request().get(ContentPackageFolderResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_CONTENTSTAGING, 1, PermHandler.PERM_CONTENTPACKAGE_VIEW));
		assertResponseOK(folderResponse);
		assertThat(folderResponse.getNodes()).hasSize(0);
		assertThat(folderResponse.getFolders()).hasSize(0);
		assertThat(folderResponse.getPages()).hasSize(1);
		assertThat(folderResponse.getImages()).hasSize(0);
		assertThat(folderResponse.getFiles()).hasSize(1);
		assertThat(folderResponse.getForms()).hasSize(0);
	}

	@Test
	public void testProbeImport() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		Integer node1ExtraConstructId = supply(() -> createConstruct(node1, LongHTMLPartType.class, "node1", "text"));
		Page page3 = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Page 3");
			p.getContent().addContentTag(node1ExtraConstructId);
		}));
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		String page3Id = supply(() -> page3.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "page", page3Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(2);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		// Drop the page
		supply(() -> new PageResourceImpl().delete(page3Id, node1.getId(), null));
		// Drop its construct --> inconsistency!
		String node1ExtraConstructGlobalId = supply(t -> {
			Construct c = t.getObject(Construct.class, node1ExtraConstructId);
			String gid = c.getGlobalId().toString();
			c.delete();
			return gid;
		});

		// The probe import should never fail because of the bad imported data
		assertResponseOK(supply(() -> {
			return new ContentStagingResourceImpl().importIntoCms(packageName, 0, true);
		}));

		// List all
		ContentPackageFolderResponse cplResponse = supply(() -> {
			return new ContentStagingResourceImpl().folderList(packageName, UriComponent.decodePath(folder1.getName(), true), null);
		});
		assertResponseOK(cplResponse);
		assertThat(cplResponse.getPages().values().stream().flatMap(List::stream).filter(p -> p.getId().equals(page3Id)).map(p -> p.getErrors().stream().map(StagedError::getError).collect(Collectors.toList())).findAny().orElse(Collections.emptyList()))
			.isNotEmpty()
			.contains(supply(()  -> I18NHelper.get("construct.notfound", node1ExtraConstructGlobalId)));

		// List mismatches only
		cplResponse = supply(() -> {
			return new ContentStagingResourceImpl().folderList(packageName, UriComponent.decodePath(folder1.getName(), true), StagingErrorKind.EXCEPTION.toString());
		});
		assertResponseOK(cplResponse);
		assertThat(cplResponse.getPages().values().stream().flatMap(List::stream).filter(p -> p.getId().equals(page3Id)).map(p -> p.getErrors().stream().map(StagedError::getError).collect(Collectors.toList())).findAny().orElse(Collections.emptyList()))
			.isNotEmpty()
			.contains(supply(()  -> I18NHelper.get("construct.notfound", node1ExtraConstructGlobalId)));

		// List something else
		cplResponse = supply(() -> {
			return new ContentStagingResourceImpl().folderList(packageName, UriComponent.decodePath(folder1.getName(), true), StagingErrorKind.NO_BINARY.toString());
		});
		assertResponseOK(cplResponse);
		assertThat(cplResponse.getPages().values().stream().flatMap(List::stream).filter(p -> p.getId().equals(page3Id)).map(p -> p.getErrors().stream().map(StagedError::getError).collect(Collectors.toList())).findAny().orElse(Collections.emptyList()))
			.isEmpty();
	}

	@Test
	public void testImport() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		assertResponseOK(new ContentStagingResourceImpl().importIntoCms(packageName, 0, false));
		response = supply(() -> new ContentStagingResourceImpl().read(packageName));
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
			response = supply(() -> new ContentStagingResourceImpl().read(packageName));
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
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page31.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page31.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page32.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page32.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node2.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", node2.getFolder().getGlobalId().toString(), true);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getPages()).isEqualTo(3);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		supply(() -> new PageResourceImpl().delete(page31.getGlobalId().toString(), page31.getNode().getId(), null));
		supply(() -> new PageResourceImpl().delete(page32.getGlobalId().toString(), page32.getNode().getId(), null));

		assertThatThrownBy(() -> supply(() -> PageResourceImpl.getPage(page31.getGlobalId().toString(), false, ObjectPermission.view))).isInstanceOf(EntityNotFoundException.class);
		assertThatThrownBy(() -> supply(() -> PageResourceImpl.getPage(page32.getGlobalId().toString(), false, ObjectPermission.view))).isInstanceOf(EntityNotFoundException.class);

		supply(() -> new ContentStagingResourceImpl().importIntoCms(packageName, 5000, false));

		page31 = supply(() -> PageResourceImpl.getPage(page31.getGlobalId().toString(), false, ObjectPermission.view));
		page32 = supply(() -> PageResourceImpl.getPage(page32.getGlobalId().toString(), false, ObjectPermission.view));
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
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1Id = supply(() -> node1.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "file", file1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "image", img1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "form", form1Id, false);
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
				((StreamingOutput) new ContentStagingResourceImpl().download(packageName).getEntity()).write(fos);
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
			multiPart
					.bodyPart(createformDataBodyPart("form-data; name=\"fileName\"", file.getName(), null))
					.bodyPart(createformDataBodyPart("form-data; name=\"fileBinaryData\"; filename=\"" + file.getName() + "\"",
							file, MediaType.APPLICATION_OCTET_STREAM_TYPE));
			ContentPackageLoadResponse fileUploadResponse = new ContentStagingResourceImpl().upload(packageName, multiPart);
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
			multiPart.bodyPart(createformDataBodyPart("form-data; name=\"fileName\"", file.getName(), null))
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
			return new ContentStagingResourceImpl().list(null, null, null, new ContentPackageListParameterBean());
		});
		assertThat(list.getItems())
			.isNotEmpty()
			.hasSize(2)
			.areAtMost(1, new Condition<StagedContentPackage>(pkg -> pkg.getName().equals(newPackageName), "names match"))
			.areAtMost(1, new Condition<StagedContentPackage>(pkg -> pkg.getName().equals(packageName), "names match"));
	}

	@Test
	public void testStatus() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
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
			impl.add(packageName, "page", page11Id, false);
			//impl.add(packageName, "page", page21Id, false);
			impl.add(packageName, "form", form1Id, false);
			impl.add(packageName, "file", file1Id, false);
			return impl.add(packageName, "image", img1Id, false);
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
			return impl.status(packageName, request);
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
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page11.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page11.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String page21Id = supply(() -> page2.getGlobalId().toString());
		String page22Id = supply(() -> page22.getGlobalId().toString());

		// Add page
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "page", page21Id, false);
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
			return new ContentStagingResourceImpl().add(packageName, "page", page22Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(2);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);
		assertThat(response.getContentPackage().getChannels()).isEqualTo(1);
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});
		assertResponseOK(new ContentStagingResourceImpl().importIntoCms(packageName, 0, false));
		response = supply(() -> new ContentStagingResourceImpl().read(packageName));
		while (response.getImport() != null && response.getImport().isRunning()) {
			response = supply(() -> new ContentStagingResourceImpl().read(packageName));
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

	@Test
	public void testPageMoved() throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		String page11Id = supply(() -> page11.getGlobalId().toString());

		// Add page
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "page", page11Id, false);
		});
		assertResponseOK(response);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		page11 = supply(() -> {
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setAllLanguages(true);
			request.setFolderId(folder2.getId());
			assertResponseOK(new PageResourceImpl().move(page11Id, request));
			return PageResourceImpl.getPage(page11Id, false);
		});

		response = supply(() -> {
			ContentStagingResourceImpl impl = new ContentStagingResourceImpl();
			assertResponseOK(impl.exportFromCms(packageName, 0L));
			return impl.read(packageName);
		});
		assertResponseOK(response);
		{
			// This throws on duplicated pages, which should not happen on page move
			response.setContentPackage(new StagedContentPackage());
			Path rootPath = dbFilesDir.toPath().resolve(packageName);
			ContentStagingResourceImpl.fillContentPackageStatistic(rootPath, response.getContentPackage(), true);
		}
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(3); // An extra folder is left after old page location
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		page11 = supply(() -> {
			ObjectMoveRequest request = new ObjectMoveRequest();
			request.setAllLanguages(true);
			request.setFolderId(folder1.getId());
			assertResponseOK(new PageResourceImpl().move(page11Id, request));
			return PageResourceImpl.getPage(page11Id, false);
		});
	}

	protected void testUpdate(String description, Optional<String> maybeNewName) throws Exception {
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		Integer timestamp = response.getContentPackage().getTimestamp();

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
		assertThat(response.getContentPackage().getTimestamp()).isGreaterThanOrEqualTo(timestamp);
	}
}
