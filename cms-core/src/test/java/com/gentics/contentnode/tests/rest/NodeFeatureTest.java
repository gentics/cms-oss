package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FeatureService;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.exception.FeatureRequiredException;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.NodeFeatureModel;
import com.gentics.contentnode.rest.model.request.NodeFeatureRequest;
import com.gentics.contentnode.rest.model.response.FeatureModelList;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for "per node" features
 */
@RunWith(value = Parameterized.class)
public class NodeFeatureTest {
	/**
	 * Feature service loader
	 */
	public static ServiceLoaderUtil<FeatureService> featureServiceLoader = ServiceLoaderUtil.load(FeatureService.class);

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node featureNode;

	private static Node noFeatureNode;

	private static UserGroup group;

	private static SystemUser user;

	/**
	 * Get the list of available features (which can be activated by node)
	 * @return list of available features
	 */
	public static List<Feature> availableFeatures() {
		return Stream.of(Feature.values()).filter(Feature::isPerNode).filter(feature -> StreamSupport
				.stream(featureServiceLoader.spliterator(), false).anyMatch(service -> service.isProvided(feature))).collect(Collectors.toList());
	}

	/**
	 * Get the list of available node features
	 * @return list of available node features
	 */
	public static List<NodeFeature> availableNodeFeatures() {
		List<Feature> availableFeatures = availableFeatures();

		return Stream.of(NodeFeature.values())
				.filter(nodeFeature -> availableFeatures.contains(Feature.valueOf(nodeFeature.name().toUpperCase())))
				.collect(Collectors.toList());
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
		try (Trx trx = new Trx()) {
			featureNode = ContentNodeTestDataUtils.createNode();
			noFeatureNode = ContentNodeTestDataUtils.createNode();
			trx.success();
		}

		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
	}

	@Parameters(name = "{index}: feature {0}, set globally {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Feature feature : availableFeatures()) {
			for (boolean globalFlag : Arrays.asList(true, false)) {
				data.add(new Object[] { feature, globalFlag });
			}
		}
		return data;
	}

	@Parameter(0)
	public Feature feature;

	@Parameter(1)
	public boolean globalFlag;

	protected NodeFeature nodeFeature;

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.executeUpdate("TRUNCATE node_feature", null));
		nodeFeature = NodeFeature.valueOf(feature.getName());
	}

	/**
	 * Test activating the feature for a node
	 * @throws NodeException
	 */
	@Test
	public void testActivate() throws NodeException {
		// generally activate feature, if globalFlag is set
		try (FeatureClosure fc = new FeatureClosure(feature, globalFlag)) {
			if (!globalFlag) {
				exceptionChecker.expect(FeatureRequiredException.class, String.format("Das erforderliche Feature %s ist nicht aktiviert.", nodeFeature.name()));
			}

			// activate the feature for the node
			Trx.operate(() -> {
				NodeResourceImpl resource = new NodeResourceImpl();
				GenericResponse response = resource.activateFeature(featureNode.getId().toString(), nodeFeature);

				if (!globalFlag) {
					ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, String.format("Cannot activate feature %s for %s: feature is not available", feature.getName(), featureNode));
				} else {
				ContentNodeRESTUtils.assertResponseOK(response);
				}
			});

			List<Feature> features = Trx.supply(() -> featureNode.getFeatures());
			if (globalFlag) {
				assertThat(features).as("List of features").containsOnly(feature);
			} else {
				assertThat(features).as("List of features").isEmpty();
			}
			assertThat(Trx.supply(() -> noFeatureNode.getFeatures())).as("List of features").isEmpty();
		}
	}

	/**
	 * Test setting the feature for a node
	 * @throws NodeException
	 */
	@Test
	public void testSet() throws NodeException {
		// generally activate feature, if globalFlag is set
		try (FeatureClosure fc = new FeatureClosure(feature, globalFlag)) {
			// set the feature for the node
			Trx.operate(() -> {
				NodeResourceImpl resource = ContentNodeRESTUtils.getNodeResource();
				NodeFeatureRequest request = new NodeFeatureRequest(Arrays.asList(NodeFeature.valueOf(feature.getName())));
				GenericResponse response = resource.setFeatures(featureNode.getId().toString(), request);

				ContentNodeRESTUtils.assertResponseOK(response);
			});

			List<Feature> features = Trx.supply(() -> featureNode.getFeatures());
			if (globalFlag) {
				assertThat(features).as("List of features").containsOnly(feature);
			} else {
				assertThat(features).as("List of features").isEmpty();
			}
			assertThat(Trx.supply(() -> noFeatureNode.getFeatures())).as("List of features").isEmpty();
		}
	}

	/**
	 * Test deactivating the feature for a node
	 * @throws NodeException
	 */
	@Test
	public void testDeactivate() throws NodeException {
		// generally activate feature, if globalFlag is set
		try (FeatureClosure fc = new FeatureClosure(feature, globalFlag)) {
			if (globalFlag) {
				Trx.operate(() -> DBUtils.executeUpdate("INSERT INTO node_feature SELECT id, ? FROM node", new Object[] {nodeFeature.name()}));
			}

			List<Feature> features = Trx.supply(() -> featureNode.getFeatures());
			if (globalFlag) {
				assertThat(features).as("List of features").containsOnly(feature);
			} else {
				assertThat(features).as("List of features").isEmpty();
			}
			features = Trx.supply(() -> noFeatureNode.getFeatures());
			if (globalFlag) {
				assertThat(features).as("List of features").containsOnly(feature);
			} else {
				assertThat(features).as("List of features").isEmpty();
			}

			if (!globalFlag) {
				exceptionChecker.expect(FeatureRequiredException.class, String.format("Das erforderliche Feature %s ist nicht aktiviert.", nodeFeature.name()));
			}

			// deactivate the feature for the node
			Trx.operate(() -> {
				NodeResourceImpl resource = new NodeResourceImpl();
				GenericResponse response = resource.deactivateFeature(featureNode.getId().toString(), nodeFeature);
				ContentNodeRESTUtils.assertResponseOK(response);
			});

			features = Trx.supply(() -> featureNode.getFeatures());
			assertThat(features).as("List of features").isEmpty();
			features = Trx.supply(() -> noFeatureNode.getFeatures());
			if (globalFlag) {
				assertThat(features).as("List of features").containsOnly(feature);
			} else {
				assertThat(features).as("List of features").isEmpty();
			}
		}
	}

	/**
	 * Test listing available features
	 * @throws NodeException
	 */
	@Test
	public void testList() throws NodeException {
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getPreferences();
		try {
			for (NodeFeature f : availableNodeFeatures()) {
				prefs.setFeature(Feature.valueOf(f.name().toUpperCase()), globalFlag ? f == nodeFeature : f != nodeFeature);
			}

			List<NodeFeatureModel> expected = Trx.supply(() -> availableNodeFeatures().stream().filter(f -> globalFlag ? f == nodeFeature : f != nodeFeature)
					.map(f -> new NodeFeatureModel().setId(f).setName(I18NHelper.get("feature." + f.name()))
							.setDescription(I18NHelper.get("feature." + f.name() + ".help"))).collect(Collectors.toList()));

			Trx.operate(() -> {
				NodeResourceImpl resource = new NodeResourceImpl();
				FeatureModelList response = resource.availableFeatures(null, null, null);
				assertResponseOK(response);
				assertThat(response.getItems()).as("Available features").usingFieldByFieldElementComparator().containsOnlyElementsOf(expected);
			});
		} finally {
			for (NodeFeature f : availableNodeFeatures()) {
				prefs.setFeature(Feature.valueOf(f.name().toUpperCase()), false);
			}
		}
	}

	/**
	 * Test required permission for activating a feature
	 * @throws NodeException
	 */
	@Test
	public void testActivatePermission() throws NodeException {
		Integer folderId = supply(() -> featureNode.getFolder().getId());
		try (FeatureClosure fc = new FeatureClosure(feature, globalFlag)) {
			if (!globalFlag) {
				exceptionChecker.expect(FeatureRequiredException.class, String.format("Das erforderliche Feature %s ist nicht aktiviert.", nodeFeature.name()));
			}

			assertRequiredPermissions(group, user, () -> new NodeResourceImpl().activateFeature(featureNode.getId().toString(), nodeFeature),
					Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_FOLDER_UPDATE));
		}
	}

	/**
	 * Test required permission for deactivating a feature
	 * @throws NodeException
	 */
	@Test
	public void testDectivatePermission() throws NodeException {
		Integer folderId = supply(() -> featureNode.getFolder().getId());
		try (FeatureClosure fc = new FeatureClosure(feature, globalFlag)) {
			if (!globalFlag) {
				exceptionChecker.expect(FeatureRequiredException.class, String.format("Das erforderliche Feature %s ist nicht aktiviert.", nodeFeature.name()));
			}

			assertRequiredPermissions(group, user, () -> new NodeResourceImpl().deactivateFeature(featureNode.getId().toString(), nodeFeature),
					Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW), Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_FOLDER_UPDATE));
		}
	}
}
