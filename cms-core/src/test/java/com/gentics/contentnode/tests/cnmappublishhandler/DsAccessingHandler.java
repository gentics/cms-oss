package com.gentics.contentnode.tests.cnmappublishhandler;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;

/**
 * CnMapPublishHandler Implementation that will access the datasource
 */
public class DsAccessingHandler extends LogHandler {
	protected String dsId;

	@Override
	public void init(Map parameters) throws CnMapPublishException {
		super.init(parameters);
		dsId = ObjectTransformer.getString(parameters.get(DS_ID), null);
	}

	@Override
	public void updateObject(Resolvable object) throws CnMapPublishException {
		super.updateObject(object);
		try {
			Datasource ds = PortalConnectorFactory.createDatasource(dsId);
			Resolvable originalObject = PortalConnectorFactory.getContentObject(ObjectTransformer.getString(object.get("contentid"), null), ds);
			assertNotNull("Could not get original object from ds", originalObject);
			logger.info("change " + originalObject.get("name") + " to " + object.get("name"));
		} catch (NodeException e) {
			throw new CnMapPublishException("Error while handling object", e);
		}
	}
}
