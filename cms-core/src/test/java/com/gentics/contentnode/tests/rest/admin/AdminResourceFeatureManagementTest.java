package com.gentics.contentnode.tests.rest.admin;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.AdminResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

@GCNFeature(set = { Feature.TAG_IMAGE_RESIZER, Feature.WEBP_CONVERSION })
public class AdminResourceFeatureManagementTest {

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

	@BeforeClass
	public static void setup() throws NodeException {
		{
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.TAG_IMAGE_RESIZER.toString().toLowerCase(), true);
			prefs.setFeature(Feature.LIVE_URLS_PER_NODE.toString().toLowerCase(), true);
		}
		testContext.getContext().getTransaction().commit();
	}

	@Test
	public void testFeatureTagImageResizer() throws NodeException {
		GenericResponse response = supply(() -> new AdminResourceImpl().setFeature(Feature.TAG_IMAGE_RESIZER.getName(), false, null));
		assertResponseOK(response);
		boolean enabled = supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			return prefs.getFeature(Feature.TAG_IMAGE_RESIZER.getName());
		});
		assertThat(enabled).isFalse();

		response = supply(() -> new AdminResourceImpl().setFeature(Feature.TAG_IMAGE_RESIZER.getName(), true, null));
		assertResponseOK(response);
		enabled = supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			return prefs.getFeature(Feature.TAG_IMAGE_RESIZER.getName());
		});
		assertThat(enabled).isTrue();
	}
}
