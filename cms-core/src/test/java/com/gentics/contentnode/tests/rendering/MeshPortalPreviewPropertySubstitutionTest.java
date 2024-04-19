package com.gentics.contentnode.tests.rendering;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT })
public class MeshPortalPreviewPropertySubstitutionTest extends MeshPortalPreviewTestBase {
	/**
	 * Name of the system property
	 */
	public final static String SYSTEM_PROPERTY_NAME = "NODE_PREVIEWURL_TEST";

	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(PreviewResource.class).build()));

	@Override
	protected RESTAppContext getRestAppContext() {
		return appContext;
	}

	@Override
	protected void setMeshPreviewUrl(Node node, String previewUrl) throws ReadOnlyException {
		// set via a system property
		if (previewUrl != null) {
			System.setProperty(SYSTEM_PROPERTY_NAME, previewUrl);
		} else {
			System.clearProperty(SYSTEM_PROPERTY_NAME);
		}
		node.setMeshPreviewUrlProperty("${sys:" + SYSTEM_PROPERTY_NAME + "}");
	}
}
