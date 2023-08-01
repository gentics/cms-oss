package com.gentics.contentnode.tests.publish.disinherit;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling inheritance over the REST API with permission checks
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(Parameterized.class)
public class DisinheritRestTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static int channelId;
	private static Template template;
	private static int nodeRootFolderId;
	private static UserGroup group;
	private static SystemUser user;

	@Parameter(0)
	public MCTestedType testedType;

	@Parameter(1)
	public boolean recursive;

	private Folder folder;
	private Integer folderId;
	private LocalizableNodeObject<?> testObject;
	private int testObjectId;
	private int subFolderId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		channelId = supply(() -> channel.getId());
		supply(() -> createChannel(node, "Foreign Channel", "foreign.channel", "/"));
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		nodeRootFolderId = supply(() -> node.getFolder().getId());

		// Create test user, assigned to the test group with restriction to the node and channel (but not the foreign channel)
		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
		user = update(user, u -> {
			Set<Integer> groupRestrictions = u.getGroupNodeRestrictions().computeIfAbsent(group.getId(), key -> new HashSet<>());
			groupRestrictions.add(node.getId());
			groupRestrictions.add(channel.getId());
		});
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: test {0}, recursive {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType testedType : MCTestedType.values()) {
			if (testedType == MCTestedType.folder) {
				for (Boolean recursive : Arrays.asList(true, false)) {
					data.add(new Object[] { testedType, recursive });
				}
			} else {
				data.add(new Object[] { testedType, false });
			}
		}
		return data;
	}

	/**
	 * Create basic test data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Test Root Folder");
		});
		folderId = supply(() -> folder.getId());

		testObject = supply(() -> testedType.create(folder, template));
		testObjectId = execute(NodeObject::getId, testObject);

		if (recursive && testObject instanceof Folder) {
			LocalizableNodeObject<?> subFolder = supply(() -> MCTestedType.folder.create((Folder) testObject, template));
			subFolderId = execute(NodeObject::getId, subFolder);
		} else {
			subFolderId = 0;
		}
	}

	/**
	 * Remove test data
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		consume(Folder::delete, folder);
		folder = null;
		folderId = 0;
	}

	/**
	 * Test getting the info for an inherited object
	 * @throws NodeException
	 */
	@Test
	public void getInfoInherited() throws NodeException {
		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isFalse();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isFalse();
		assertThat(response.getDisinherit()).as("Disinherited").isEmpty();
		assertThat(response.getInheritable()).as("Inheritable").containsOnly(channelId);
	}

	/**
	 * Test getting the info for an excluded object
	 * @throws NodeException
	 */
	@Test
	public void getInfoExcluded() throws NodeException {
		consume(o -> testedType.exclude(o), testObject);

		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isTrue();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isFalse();
		assertThat(response.getDisinherit()).as("Disinherited").isEmpty();
		assertThat(response.getInheritable()).as("Inheritable").containsOnly(channelId);
	}

	/**
	 * Test getting the info for an object, which will be disinherited by default
	 * @throws NodeException
	 */
	@Test
	public void getInfoDisinheritedDefault() throws NodeException {
		consume(o -> testedType.disinheritDefault(o), testObject);

		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isFalse();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isTrue();
		assertThat(response.getDisinherit()).as("Disinherited").isEmpty();
		assertThat(response.getInheritable()).as("Inheritable").containsOnly(channelId);
	}

	/**
	 * Test getting the info for a disinherited object
	 * @throws NodeException
	 */
	@Test
	public void getInfoDisinherited() throws NodeException {
		consume(o -> testedType.disinherit(o, channel), testObject);

		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isFalse();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isFalse();
		assertThat(response.getDisinherit()).as("Disinherited").containsOnly(channelId);
		assertThat(response.getInheritable()).as("Inheritable").containsOnly(channelId);
	}

	/**
	 * Test getting the info for an object in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void getInfoInDisinheritedFolder() throws NodeException {
		// disinherit the folder
		consume(f -> MCTestedType.folder.disinherit(f, channel), folder);

		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isFalse();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isFalse();
		assertThat(response.getDisinherit()).as("Disinherited").containsOnly(channelId);
		assertThat(response.getInheritable()).as("Inheritable").isEmpty();
	}

	/**
	 * Test getting the info for an object in an excluded folder
	 * @throws NodeException
	 */
	@Test
	public void getInfoInExcludedFolder() throws NodeException {
		// exclude the folder
		consume(f -> MCTestedType.folder.exclude(f), folder);

		DisinheritResponse response = null;
		if (testObject instanceof Folder) {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			response = assertRequiredPermissions(group, user, () -> {
				return testedType.getInheritance(testObject);
			},
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}

		assertThat(response.isExclude()).as("Excluded").isTrue();
		assertThat(response.isDisinheritDefault()).as("Disinherit default").isFalse();
		assertThat(response.getDisinherit()).as("Disinherited").isEmpty();
		assertThat(response.getInheritable()).as("Inheritable").isEmpty();
	}

	/**
	 * Test excluding an object
	 * @throws NodeException
	 */
	@Test
	public void exclude() throws NodeException {
		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Test including an object
	 * @throws NodeException
	 */
	@Test
	public void include() throws NodeException {
		consume(o -> testedType.exclude(o), testObject);

		if (testObject instanceof Folder) {
			assertRequiredPermissions(group, user, () -> testedType.include(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			assertRequiredPermissions(group, user, () -> testedType.include(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}
	}

	/**
	 * Test disinheriting by default
	 * @throws NodeException
	 */
	@Test
	public void disinheritByDefault() throws NodeException {
		if (testObject instanceof Folder) {
			assertRequiredPermissions(group, user, () -> testedType.disinheritDefault(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			assertRequiredPermissions(group, user, () -> testedType.disinheritDefault(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}
	}

	/**
	 * Test reinheriting by default
	 * @throws NodeException
	 */
	@Test
	public void reinheritByDefault() throws NodeException {
		consume(o -> testedType.disinheritDefault(o), testObject);

		if (testObject instanceof Folder) {
			assertRequiredPermissions(group, user, () -> testedType.reinheritDefault(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			assertRequiredPermissions(group, user, () -> testedType.reinheritDefault(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}
	}

	/**
	 * Test disinheriting
	 * @throws NodeException
	 */
	@Test
	public void disinherit() throws NodeException {
		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.disinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.disinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.disinherit(testObject, channel),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Test reinheriting
	 * @throws NodeException
	 */
	@Test
	public void reinherit() throws NodeException {
		consume(o -> testedType.disinherit(o, channel), testObject);

		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Try reinheriting an object in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void tryReinheritInDisinheritedFolder() throws NodeException {
		// disinherit the folder
		consume(f -> MCTestedType.folder.disinherit(f, channel), folder);

		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel), ResponseCode.INVALIDDATA,
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel), ResponseCode.INVALIDDATA,
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel), ResponseCode.INVALIDDATA,
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Reinherit an object in an excluded folder
	 * @throws NodeException
	 */
	@Test
	public void reinheritInExcludedFolder() throws NodeException {
		// exclude the folder
		consume(f -> MCTestedType.folder.exclude(f), folder);

		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.reinherit(testObject, channel),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Exclude an object in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void excludeInDisinheritedFolder() throws NodeException {
		// disinherit the folder
		consume(f -> MCTestedType.folder.disinherit(f, channel), folder);

		if (testObject instanceof Folder) {
			if (recursive) {
				assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, subFolderId, PermHandler.PERM_FOLDER_DELETE));
			} else {
				assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
						Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW),
						Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_FOLDER_DELETE));
			}
		} else {
			assertRequiredPermissions(group, user, () -> testedType.exclude(testObject),
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_DELETE));
		}
	}

	/**
	 * Try including an object in an excluded folder
	 * @throws NodeException
	 */
	@Test
	public void tryIncludeInExcludedFolder() throws NodeException {
		// exclude the folder
		consume(f -> MCTestedType.folder.exclude(f), folder);

		if (testObject instanceof Folder) {
			assertRequiredPermissions(group, user, () -> testedType.include(testObject), ResponseCode.INVALIDDATA,
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, testObjectId, PermHandler.PERM_VIEW));
		} else {
			assertRequiredPermissions(group, user, () -> testedType.include(testObject), ResponseCode.INVALIDDATA,
					Triple.of(Node.TYPE_NODE, nodeRootFolderId, PermHandler.PERM_INHERITANCE),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_VIEW),
					Triple.of(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW));
		}
	}
}
