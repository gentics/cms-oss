package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.MULTICHANNELLING })
public class ContentStagingDataRemovalTest extends AbstractContentStagingTest {

	@Test
	public void testExportNoObject() throws Exception {
		operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.FORMS.toString().toLowerCase(), true);
			node1.activateFeature(Feature.FORMS);
		});
		Page page1bis = supply(() -> create(Page.class, p -> {
			p.setFolderId(node1.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1-bis");
		}));
		Node node1bis = supply(() -> createNode());
		String packageNameHuman = DEFAULT_PACKAGE_PREFIX + Long.toString(System.currentTimeMillis());
		ContentPackageLoadResponse response = directCreate(packageNameHuman, DEFAULT_PACKAGE_DESCRIPTION);
		String packageName = response.getContentPackage().getName();
		operate(() -> PermHandler.setPermissions(Page.TYPE_PAGE, page1bis.getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, page1bis.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node1.getFolder().getId(), Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString()));
		String folder1Id = supply(() -> folder1.getGlobalId().toString());
		String page1bisId = supply(() -> page1bis.getGlobalId().toString());
		String file1Id = supply(() -> file1.getGlobalId().toString());
		String img1Id = supply(() -> image1.getGlobalId().toString());
		String form1Id = supply(() -> form1.getGlobalId().toString());
		String node1bisId = supply(() -> node1bis.getGlobalId().toString());
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "page", page1bisId, false);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "folder", folder1Id, true);
		});
		response = supply(() -> {
			return new ContentStagingResourceImpl().add(packageName, "node", node1bisId, false);
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
		assertThat(response.getContentPackage().getNodes()).isEqualTo(2);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(3);
		assertThat(response.getContentPackage().getPages()).isEqualTo(2);
		assertThat(response.getContentPackage().getImages()).isEqualTo(1);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(1);
		assertThat(response.getContentPackage().getForms()).isEqualTo(1);

		// Delete page
		assertResponseOK(supply(() -> new PageResourceImpl().delete(page1bisId, node1.getId().intValue(), null)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getPages()).isEqualTo(1);

		// Delete file
		assertResponseOK(supply(() -> new FileResourceImpl().delete(file1Id, node1.getId().intValue(), null)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getFiles()).isEqualTo(0);

		// Delete image
		assertResponseOK(supply(() -> new ImageResourceImpl().delete(img1Id, node1.getId().intValue(), null)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getImages()).isEqualTo(0);

		// Delete form
		assertResponseOK(supply(() -> new FormResourceImpl().delete(form1Id, null, null)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getForms()).isEqualTo(0);

		// Delete folder
		assertResponseOK(supply(() -> new FolderResourceImpl().delete(folder1Id, node1.getId().intValue(), null)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(2);

		// Delete node
		assertResponseOK(supply(() -> new NodeResourceImpl().delete(node1bisId, 0)));
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		response = supply(() -> {
			return new ContentStagingResourceImpl().read(packageName);
		});	
		assertResponseOK(response);
		assertThat(response.getContentPackage().getFolders()).isEqualTo(1);
		assertThat(response.getContentPackage().getNodes()).isEqualTo(1);
	}


}
