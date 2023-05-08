package com.gentics.contentnode.tests.rendering;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.ClassRule;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT })
public class MeshPortalPreviewNoSslTest extends MeshPortalPreviewTestBase {

	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(PreviewResource.class).build()));

	@Override
	protected RESTAppContext getRestAppContext() {
		return appContext;
	}
}
