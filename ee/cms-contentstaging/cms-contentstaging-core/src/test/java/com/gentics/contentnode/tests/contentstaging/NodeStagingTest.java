package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.perm;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Tests for staging of nodes/channels
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.PUB_DIR_SEGMENT, Feature.MESH_CONTENTREPOSITORY, Feature.DEVTOOLS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class NodeStagingTest implements ContentStagingMethods {
	/**
	 * Name of the test package
	 */
	public final static String PACKAGE_NAME = "TestPackage";

	/**
	 * Object mapper
	 */
	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	/**
	 * Database context
	 */
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test group
	 */
	protected static UserGroup testGroup;

	/**
	 * Test user
	 */
	protected static SystemUser testUser;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(testGroup)));
	}

	@Before
	public void setup() throws NodeException, IOException {
		makeClean();
	}

	/**
	 * Test importing node metadata (name, description, https, pubdir segments, publish settings)
	 * @throws NodeException
	 */
	@Test
	public void testStagedNodeData() throws NodeException {
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();

		// create package
		createContentPackage(PACKAGE_NAME);

		// test boolean attributes
		testBooleanAttribute("https", node, Node::isHttps, Node::setHttps);
		testBooleanAttribute("pubDirSegment", node, Node::isPubDirSegment, Node::setPubDirSegment);
		testBooleanAttribute("publishFilesystemFiles", node, Node::doPublishFilesystemFiles, Node::setPublishFilesystemFiles);
		testBooleanAttribute("publishFilesystemPages", node, Node::doPublishFilesystemPages, Node::setPublishFilesystemPages);
		testBooleanAttribute("publishContentMapFolders", node, Node::doPublishContentMapFolders, Node::setPublishContentMapFolders);
		testBooleanAttribute("publishContentMapPages", node, Node::doPublishContentMapPages, Node::setPublishContentMapPages);
		testBooleanAttribute("publishContentMapFiles", node, Node::doPublishContentMapFiles, Node::setPublishContentMapFiles);
		testBooleanAttribute("omitPageExtension", node, Node::isOmitPageExtension, Node::setOmitPageExtension);

		// test non-boolean attributes
		testAttribute("folder.name", node, n -> n.getFolder().getName(), (n, name) -> n.getFolder().setName(name), "Original Name", "Alternative Name");
		testAttribute("folder.description", node, n -> n.getFolder().getDescription(), (n, description) -> n.getFolder().setDescription(description), "Original Description", "Alternative Description");
		testAttribute("publishDir", node, Node::getPublishDir, Node::setPublishDir, "/node/pub/dir", "/node/alternate/pub/dir");
		testAttribute("binaryPublishDir", node, Node::getBinaryPublishDir, Node::setBinaryPublishDir, "/node/bin/pub/dir", "/node/bin/alternate/pub/dir");
		testAttribute("pageLanguageCode", node, Node::getPageLanguageCode, Node::setPageLanguageCode, PageLanguageCode.FILENAME, PageLanguageCode.PATH);
		testAttribute("pageUrls", node, n -> UrlRenderWay.fromValue(n.getUrlRenderWayPages()), (n, v) -> n.setUrlRenderWayPages(v.getValue()), UrlRenderWay.PORTAL, UrlRenderWay.STATIC_DYNAMIC);
		testAttribute("fileUrls", node, n -> UrlRenderWay.fromValue(n.getUrlRenderWayFiles()), (n, v) -> n.setUrlRenderWayFiles(v.getValue()), UrlRenderWay.STATIC_WITH_DOMAIN, UrlRenderWay.STATIC_WITHOUT_DOMAIN);
		testAttribute("meshProjectName", node, Node::getMeshProjectName, Node::setMeshProjectName, "", "alternative_mesh_project");

		// test staging of properties, which support property substitution
		testAttribute("meshPreviewUrl", node, Node::getMeshPreviewUrlProperty, Node::setMeshPreviewUrlProperty, "${sys:mesh.preview.url}", "${sys:mesh.alternative.preview.url}");
	}

	/**
	 * Test that unstaged attributes are not changed
	 * @throws NodeException
	 */
	@Test
	public void testUnstagedNodeData() throws NodeException {
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();

		// create package
		createContentPackage(PACKAGE_NAME);

		testUnstagedAttribute("hostname", node, Node::getHostname, Node::setHostname, "original.host.name", "alternative.host.name");
		testUnstagedAttribute("meshPreviewUrl", node, Node::getMeshPreviewUrl, Node::setMeshPreviewUrl, "http://mesh.url/", "http://alternative.mesh.url/");
		testUnstagedAttribute("insecurePreviewUrl", node, Node::isInsecurePreviewUrl, Node::setInsecurePreviewUrl, true, false);
		testUnstagedAttribute("publishDisabled", node, Node::isPublishDisabled, Node::setPublishDisabled, true, false);
	}

	/**
	 * Test that activated features are staged
	 * @throws NodeException
	 */
	@Test
	public void testFeatures() throws NodeException {
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();

		// create package
		createContentPackage(PACKAGE_NAME);

		for (NodeFeature nodeFeature : NodeFeature.values()) {
			Feature feature = Feature.getByName(nodeFeature.name());
			if (feature != null && feature.isActivated()) {
				testFeature(node, feature);
			}
		}
	}

	/**
	 * Test that activated content languages are staged
	 * @throws NodeException
	 */
	@Test
	public void testContentLanguages() throws NodeException {
		ContentLanguage german = getLanguage("de");
		ContentLanguage english = getLanguage("en");
		ContentLanguage french = getLanguage("fr");

		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");

			n.getLanguages().add(german);
		}).build();
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// export node into package
		String germanPackage = "German";
		createContentPackage(germanPackage);
		assertResponseOK(new ContentStagingResourceImpl().add(germanPackage, "node", nodeGlobalId.toString(), false));

		// add english
		node = update(node, n -> {
			n.getLanguages().add(english);
		}).build();

		// export node into another package
		String germanEnglishPackage = "GermanEnglish";
		createContentPackage(germanEnglishPackage);
		assertResponseOK(new ContentStagingResourceImpl().add(germanEnglishPackage, "node", nodeGlobalId.toString(), false));

		// add french
		node = update(node, n -> {
			n.getLanguages().add(french);
		}).build();

		// export node into yet another package
		String germanEnglishFrenchPackage = "GermanEnglishFrench";
		createContentPackage(germanEnglishFrenchPackage);
		assertResponseOK(new ContentStagingResourceImpl().add(germanEnglishFrenchPackage, "node", nodeGlobalId.toString(), false));

		// reorder languages
		node = update(node, n -> {
			List<ContentLanguage> languages = n.getLanguages();
			languages.clear();
			languages.add(french);
			languages.add(english);
			languages.add(german);
		}).build();

		// export node into yet another package (again)
		String frenchEnglishGermanPackage = "FrenchEnglishGerman";
		createContentPackage(frenchEnglishGermanPackage);
		assertResponseOK(new ContentStagingResourceImpl().add(frenchEnglishGermanPackage, "node", nodeGlobalId.toString(), false));

		// delete node
		consume(n -> n.delete(true), node);

		// import german and english
		importContentPackage(germanEnglishPackage);

		// reload and assert
		try (Trx trx = new Trx()) {
			node = trx.getTransaction().getObject(Node.class, nodeGlobalId);
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasLanguages(german, english);
			trx.success();
		}

		// import german
		importContentPackage(germanPackage);

		// reload and assert
		try (Trx trx = new Trx()) {
			node = trx.getTransaction().getObject(Node.class, nodeGlobalId);
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasLanguages(german);
			trx.success();
		}

		// import german, english and french
		importContentPackage(germanEnglishFrenchPackage);

		// reload and assert
		try (Trx trx = new Trx()) {
			node = trx.getTransaction().getObject(Node.class, nodeGlobalId);
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasLanguages(german, english, french);
			trx.success();
		}

		// import french, english, german
		importContentPackage(frenchEnglishGermanPackage);

		// reload and assert
		try (Trx trx = new Trx()) {
			node = trx.getTransaction().getObject(Node.class, nodeGlobalId);
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasLanguages(french, english, german);
			trx.success();
		}
	}

	/**
	 * Test that construct assignment is staged
	 * @throws NodeException
	 */
	@Test
	public void testConstructs() throws NodeException {
		testAssignment(() -> {
			return create(Construct.class, c -> {
				c.setKeyword("first");
				c.setName("First Construct", 1);
			}).build();
		}, () -> {
			return create(Construct.class, c -> {
				c.setKeyword("second");
				c.setName("Second Construct", 1);
			}).build();
		}, c -> c.delete(true), Node::addConstruct, (n, c) -> {
			assertThat(n).as("Staged Node")
				.isNotNull()
				.hasConstructs(c.toArray(new Construct[c.size()]));
		});
	}

	/**
	 * Test that object property definition assignment is staged
	 * @throws NodeException
	 */
	@Test
	public void testObjectPropertyDefinitions() throws NodeException {
		// create construct
		Construct construct = create(Construct.class, c -> {
			c.setKeyword("construct");
			c.setName("Construct", 1);
		}).build();
		int constructId = execute(Construct::getId, construct);

		testAssignment(() -> {
			return create(ObjectTagDefinition.class, oe -> {
				oe.setTargetType(Folder.TYPE_FOLDER);
				oe.setName("First Object Property", 1);
				ObjectTag objectTag = oe.getObjectTag();

				objectTag.setConstructId(constructId);
				objectTag.setEnabled(true);
				objectTag.setName("object.first");
				objectTag.setObjType(Folder.TYPE_FOLDER);
			}).build();
		}, () -> {
			return create(ObjectTagDefinition.class, oe -> {
				oe.setTargetType(Folder.TYPE_FOLDER);
				oe.setName("Second Object Property", 1);
				ObjectTag objectTag = oe.getObjectTag();

				objectTag.setConstructId(constructId);
				objectTag.setEnabled(true);
				objectTag.setName("object.second");
				objectTag.setObjType(Folder.TYPE_FOLDER);
			}).build();
		}, prop -> {
			for (ObjectTag tag : prop.getObjectTags()) {
				tag.delete(true);
			}
			prop.delete(true);
		}, (n, prop) -> {
			update(prop, update -> {
				update.getNodes().add(n);
			}).build();
		}, (n, props) -> {
			assertThat(n).as("Staged node")
				.isNotNull()
				.hasObjectProperties(props.toArray(new ObjectTagDefinition[props.size()]));
		});
	}

	/**
	 * Test that contentrepository assignemtn is staged
	 * @throws NodeException
	 */
	@Test
	public void testContentRepository() throws NodeException {
		// create test CR
		ContentRepository cr = create(ContentRepository.class, c -> {
			c.setCrType(ContentRepositoryModel.Type.mesh);
			c.setName("Test CR");
		}).build();
		int crId = execute(ContentRepository::getId, cr);

		// create test node
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");

			n.setContentrepositoryId(crId);
		}).build();
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// create package and add node
		createContentPackage(PACKAGE_NAME);
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// delete node
		consume(Node::delete, node);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		try (Trx trx = new Trx()) {
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasContentRepository(cr);
			trx.success();
		}

		// unset CR
		node = update(node, n -> {
			n.setContentrepositoryId(null);
		}).build();
		try (Trx trx = new Trx()) {
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasContentRepository(null);
			trx.success();
		}

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		try (Trx trx = new Trx()) {
			assertThat(node).as("Staged node")
				.isNotNull()
				.hasContentRepository(cr);
			trx.success();
		}
	}

	/**
	 * Test that (devtool) package assignment is staged
	 * @throws NodeException
	 */
	@Test
	public void testPackages() throws NodeException {
		Synchronizer.start();
		try {
			testAssignment(() -> {
				Synchronizer.addPackage("first_package");
				return "first_package";
			}, () -> {
				Synchronizer.addPackage("second_package");
				return "second_package";
			}, p -> {
				Synchronizer.removePackage(p);
			}, (n, p) -> {
				Synchronizer.addPackage(n, p);
			}, (n, packages) -> {
				assertThat(n).as("Staged node")
					.isNotNull()
					.hasPackages(packages.toArray(new String[packages.size()]));
			});
		} finally {
			Synchronizer.stop();
		}
	}

	/**
	 * Test importing a node with multiple channels
	 * @throws NodeException
	 */
	@Test
	public void testChannels() throws NodeException {
		// create the global IDs of the node and channels in a way that when sorted, the channels would come before their masters
		GlobalId nodeGlobalId = new GlobalId("4444." + UUID.randomUUID().toString());
		GlobalId channelGlobalId = new GlobalId("3333." + UUID.randomUUID().toString());
		GlobalId subChannelGlobalId = new GlobalId("2222." + UUID.randomUUID().toString());
		GlobalId sideChannelGlobalId = new GlobalId("1111." + UUID.randomUUID().toString());

		// create test node
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
			n.setGlobalId(nodeGlobalId);
		}).build();

		Node channel = execute(m -> create(Node.class, n -> {
			Folder channelRootFolder = create(Folder.class, f -> {
				f.setName("Channel");
				f.setPublishDir("/");
				f.setChannelMaster(m.getFolder());
			}).doNotSave().build();

			n.setFolder(channelRootFolder);
			n.setHostname("channel");
			n.setPublishDir("/");
			n.setGlobalId(channelGlobalId);
		}).build(), node);

		Node subChannel = execute(m -> create(Node.class, n -> {
			Folder channelRootFolder = create(Folder.class, f -> {
				f.setName("SubChannel");
				f.setPublishDir("/");
				f.setChannelMaster(m.getFolder());
			}).doNotSave().build();

			n.setFolder(channelRootFolder);
			n.setHostname("subchannel");
			n.setPublishDir("/");
			n.setGlobalId(subChannelGlobalId);
		}).build(), channel);

		Node sideChannel = execute(m -> create(Node.class, n -> {
			Folder channelRootFolder = create(Folder.class, f -> {
				f.setName("SideChannel");
				f.setPublishDir("/");
				f.setChannelMaster(m.getFolder());
			}).doNotSave().build();

			n.setFolder(channelRootFolder);
			n.setHostname("sidechannel");
			n.setPublishDir("/");
			n.setGlobalId(sideChannelGlobalId);
		}).build(), node);

		// create package and add node with channels
		createContentPackage(PACKAGE_NAME);
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", channelGlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", subChannelGlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", sideChannelGlobalId.toString(), false));

		// delete node/channel
		consume(Node::delete, sideChannel);
		consume(Node::delete, subChannel);
		consume(Node::delete, channel);
		consume(Node::delete, node);

		// import
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "importIntoCms",
				new Class<?>[] { String.class, Long.TYPE, Boolean.TYPE }, new Object[] { PACKAGE_NAME, 0, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(TypePerms.content, PermType.read),
				perm(TypePerms.content, PermType.create));

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		channel = supply(t -> t.getObject(Node.class, channelGlobalId));
		subChannel = supply(t -> t.getObject(Node.class, subChannelGlobalId));
		sideChannel = supply(t -> t.getObject(Node.class, sideChannelGlobalId));
		try (Trx trx = new Trx()) {
			assertThat(node).as("Staged node")
				.isNotNull()
				.isNoChannel();
			assertThat(channel).as("Staged channel")
				.isNotNull()
				.isChannel()
				.isChannelOf(node);
			assertThat(subChannel).as("Staged subchannel")
				.isNotNull()
				.isChannel()
				.isChannelOf(channel);
			assertThat(sideChannel).as("Staged sidechannel")
				.isNotNull()
				.isChannel()
				.isChannelOf(node);
			trx.success();
		}
	}

	/**
	 * Variant of {@link #testAttribute(String, Node, Function, BiConsumer, Object, Object)} for boolean attributes
	 * @param attributeName attribute name
	 * @param node node to test on
	 * @param getter getter for reading the attribute value
	 * @param setter setter for changing the attribute value
	 * @throws NodeException
	 */
	protected void testBooleanAttribute(String attributeName, Node node, Function<Node, Boolean> getter, BiConsumer<Node, Boolean> setter) throws NodeException {
		testAttribute(attributeName, node, getter, setter, true, false);
	}

	/**
	 * Test staging of an attribute
	 * @param <T> type of the attribute value
	 * @param attributeName attribute name (used for descriptions of the assertions)
	 * @param node node to test on
	 * @param getter getter for reading the attribute value
	 * @param setter setter for changing the attribute value
	 * @param initialValue initial attribute value
	 * @param alternativeValue alternative attribute value
	 * @throws NodeException
	 */
	protected <T> void testAttribute(String attributeName, Node node, Function<Node, T> getter,
			BiConsumer<Node, T> setter, T initialValue, T alternativeValue) throws NodeException {
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// set attribute to initial value
		node = update(node, n -> setter.accept(n, initialValue)).build();

		// export with initial value
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// set attribute to alternative value
		node = update(node, n -> setter.accept(n, alternativeValue)).build();
		T value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Changed value for " + attributeName).isEqualTo(alternativeValue);

		// import initial value
		importContentPackage(PACKAGE_NAME);

		// reload and assert initial value
		node = execute(NodeObject::reload, node);
		value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Staged value for " + attributeName).isEqualTo(initialValue);

		// set attribute to alternative value
		node = update(node, n -> setter.accept(n, alternativeValue)).build();
		value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Changed value for " + attributeName).isEqualTo(alternativeValue);

		// export alternative value
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// set attribute to initial value
		node = update(node, n -> setter.accept(n, initialValue)).build();
		value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Changed value for " + attributeName).isEqualTo(initialValue);

		// import alternative value
		importContentPackage(PACKAGE_NAME);

		// reload and assert alternative value
		node = execute(NodeObject::reload, node);
		value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Staged value for " + attributeName).isEqualTo(alternativeValue);
	}

	/**
	 * Test that the attribute is not staged
	 * @param <T> type of the attribute value
	 * @param attributeName attribute name (used for descriptions of the assertions)
	 * @param node node to test on
	 * @param getter getter for reading the attribute value
	 * @param setter setter for changing the attribute value
	 * @param initialValue initial attribute value
	 * @param alternativeValue alternative attribute value
	 * @throws NodeException
	 */
	protected <T> void testUnstagedAttribute(String attributeName, Node node, Function<Node, T> getter,
			BiConsumer<Node, T> setter, T initialValue, T alternativeValue) throws NodeException {
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// set attribute to initial value
		node = update(node, n -> setter.accept(n, initialValue)).build();

		// export "with initial value"
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// set attribute to alternative value
		node = update(node, n -> setter.accept(n, alternativeValue)).build();
		T value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Changed value for " + attributeName).isEqualTo(alternativeValue);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert alternative value
		node = execute(NodeObject::reload, node);
		value = execute(n -> getter.apply(n), node);
		assertThat(value).as("Unmodified value for " + attributeName).isEqualTo(alternativeValue);
	}

	/**
	 * Test activating/deactivating the feature by staging
	 * @param node node
	 * @param feature feature
	 * @throws NodeException
	 */
	protected void testFeature(Node node, Feature feature) throws NodeException {
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// activate the feature
		node = update(node, n -> n.activateFeature(feature)).build();

		// export with feature activated
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// deactivate feature
		node = update(node, n -> n.deactivateFeature(feature)).build();
		List<Feature> features = execute(Node::getFeatures, node);
		assertThat(features).as("Activated features").doesNotContain(feature);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		features = execute(Node::getFeatures, node);
		assertThat(features).as("Activated features").contains(feature);

		// deactivate the feature
		node = update(node, n -> n.deactivateFeature(feature)).build();

		// export with feature deactivated
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// activate
		node = update(node, n -> n.activateFeature(feature)).build();

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		features = execute(Node::getFeatures, node);
		assertThat(features).as("Activated features").doesNotContain(feature);
	}

	/**
	 * Generic assignment test
	 * @param <T> type of assigned entities
	 * @param createFirst supplier to create the first assigned entity
	 * @param createSecond supplier to create the second assigned entity
	 * @param delete consumer to delete an entity
	 * @param assign consumer for assigning
	 * @param asserter asserter
	 * @throws NodeException
	 */
	protected <T> void testAssignment(Supplier<T> createFirst, Supplier<T> createSecond, Consumer<T> delete,
			BiConsumer<Node, T> assign, BiConsumer<Node, List<T>> asserter) throws NodeException {
		// create test node
		Node node = create(Node.class, n -> {
			n.setFolder(create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();
		GlobalId nodeGlobalId = execute(Node::getGlobalId, node);

		// create two instances of the assigned object
		T first = createFirst.supply();
		T second = createSecond.supply();

		// assign first instance to node
		consume(assign, node, first);

		// create package and add node
		createContentPackage(PACKAGE_NAME);
		assertResponseOK(new ContentStagingResourceImpl().add(PACKAGE_NAME, "node", nodeGlobalId.toString(), false));

		// delete node
		consume(Node::delete, node);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		try (Trx trx = new Trx()) {
			asserter.accept(node, Arrays.asList(first));
			trx.success();
		}

		// assign second instance
		consume(assign, node, second);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		try (Trx trx = new Trx()) {
			asserter.accept(node, Arrays.asList(first, second));
			trx.success();
		}

		// delete first instance
		consume(delete, first);

		// import
		importContentPackage(PACKAGE_NAME);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		try (Trx trx = new Trx()) {
			asserter.accept(node, Arrays.asList(second));
			trx.success();
		}
	}
}
