package com.gentics.contentnode.tests.rendering;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;

/**
 * Test cases for the "portal preview" of a Mesh portal
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT })
public class MeshPortalPreviewSslTest extends MeshPortalPreviewTestBase {

	@BeforeClass
	public static void setupOnce() throws Exception {
		MeshPortalPreviewTestBase.setupOnce();

		node = Trx.execute(MeshPortalPreviewSslTest::enableInsecurePreviewUrl, node);
	}

	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(RESTAppContext.Type.grizzlySsl, new ResourceConfig().registerResources(Resource.builder(PreviewResource.class).build()))
		.baseUriPattern("https://localhost:%d/CNPortletapp/rest/");

	@Override
	protected RESTAppContext getRestAppContext() {
		return appContext;
	}

	private static Node enableInsecurePreviewUrl(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		node = t.getObject(node, true);
		node.setInsecurePreviewUrl(true);
		node.save();

		return node;
	}
}
