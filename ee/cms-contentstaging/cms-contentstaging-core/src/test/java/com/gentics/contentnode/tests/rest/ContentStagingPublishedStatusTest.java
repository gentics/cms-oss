package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.request.MultiPagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingPublishedStatusTest extends AbstractContentStagingTest {

	@Test
	public void testKeepUnpublishedStatus() throws Exception {
		Page page111 = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1 of Folder 1");
		}));
		GlobalId id111 = supply(() -> page111.getGlobalId());

		supply(() -> new PageResourceImpl().publish(id111.toString(), page111.getNode().getId(), new MultiPagePublishRequest()));
		assertTrue(supply(() -> page111.<Page>reload().isOnline()));

		directCreate("MyPackage", DEFAULT_PACKAGE_DESCRIPTION);

		supply(() -> new PageResourceImpl().takeOffline(id111.toString(), new PageOfflineRequest()));
		assertFalse(supply(() -> page111.<Page>reload().isOnline()));

		supply(() -> new ContentStagingResourceImpl().add("MyPackage", "folder", page111.getNode().getFolder().getGlobalId().toString(), true));
		supply(() -> new ContentStagingResourceImpl().exportFromCms("MyPackage", 1000));

		operate(() -> page111.delete(true));

		supply(() -> new ContentStagingResourceImpl().importIntoCms("MyPackage", 1000));
		assertFalse(supply(trx -> trx.getObject(Page.class, id111).isOnline()));
	}
}
