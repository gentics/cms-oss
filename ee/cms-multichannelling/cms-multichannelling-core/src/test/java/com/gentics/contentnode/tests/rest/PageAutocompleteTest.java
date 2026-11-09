package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the autocomplete REST call
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
public class PageAutocompleteTest {
	private static final String RESTRICTED_MC_PAGE = "Restricted MC Page";

	private static final String RESTRICTED_PAGE = "Restricted page";

	private static final String MATCH_IN_MASTER = "Match Only In Master";

	private static final String MATCH_IN_CHANNEL = "Match Only In Channel";

	private static final String MATCH_IN_MASTER_AND_CHANNEL = "Match In Master And Channel";

	private static final String NAME_POSTFIX = " bla";

	private static final String NAME_PREFIX = "bla ";

	private static final String QUERY_WITH_LIMIT = "Query Me With Limit";

	private static final int NUM_LIMIT_PAGES = 5;

	private static final String QUERY_BY_ID = "Query Me By ID";

	private static final String QUERY_BY_NAME = "Query Me By Name";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node master;

	private static Node channel;

	private static Page queryByNamePage;

	private static Node node;

	private static Page queryByIdPage;

	private static List<Page> limitPages = new ArrayList<Page>(NUM_LIMIT_PAGES);

	private static Page matchInMasterAndChannel;

	private static Page matchInMasterAndChannelCopy;

	private static Page matchInChannel;

	private static Page matchInChannelCopy;

	private static Page matchInMaster;

	private static Page matchInMasterCopy;

	private static UserGroup restrictedGroup;

	private static SystemUser restrictedUser;

	private static Page visiblePage;

	private static Page invisiblePage;

	private static Page invisibleMasterPage;

	private static Page visibleChannelPage;

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "The Node", PublishTarget.NONE));

		master =  supply(() -> createNode("master", "1. Master", PublishTarget.NONE));
		channel =  supply(() -> createChannel(master, "2. Channel", "channel", "/Content.Node"));

		Template template = create(Template.class, create -> {
			create.getFolders().add(node.getFolder());
			create.getFolders().add(master.getFolder());
			create.setMlId(1);
			create.setName("Template");
			create.setSource("");
		}).build();

		Folder folder = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder");
			create.setPublishDir("/");
		}).build();

		queryByNamePage = createPage(folder, template, QUERY_BY_NAME);
		queryByIdPage = createPage(folder, template, QUERY_BY_ID);

		for (int i = 0; i < NUM_LIMIT_PAGES; i++) {
			limitPages.add(createPage(folder, template, QUERY_WITH_LIMIT));
		}

		Folder nodeRootFolder = supply(() -> node.getFolder());
		Folder masterRootFolder = supply(() -> master.getFolder());

		matchInMasterAndChannel = createPage(masterRootFolder, template, MATCH_IN_MASTER_AND_CHANNEL);
		matchInMasterAndChannelCopy = localize(matchInMasterAndChannel, channel);

		matchInChannel = createPage(masterRootFolder, template, "Does not match");
		matchInChannelCopy = localize(matchInChannel, channel, MATCH_IN_CHANNEL);

		matchInMaster = createPage(masterRootFolder, template, MATCH_IN_MASTER);
		matchInMasterCopy = localize(matchInMaster, channel, "Does not match");

		restrictedGroup = supply(() -> createUserGroup("Testgroup", 2));
		restrictedUser = supply(() -> createSystemUser("test", "tester", "test.tester@gentics.com", "test", "test", Arrays.asList(restrictedGroup)));

		operate(() -> {
			DBUtils.executeUpdate("INSERT INTO user_group_node (user_group_id, node_id) SELECT id, ? FROM user_group WHERE user_id = ? AND usergroup_id = ?", new Object[] {channel.getId(), restrictedUser.getId(), restrictedGroup.getId()});
			DBUtils.executeUpdate("INSERT INTO user_group_node (user_group_id, node_id) SELECT id, ? FROM user_group WHERE user_id = ? AND usergroup_id = ?", new Object[] {node.getId(), restrictedUser.getId(), restrictedGroup.getId()});
			PermissionStore.getInstance().refreshGroup(ObjectTransformer.getInt(restrictedGroup.getId(), 0));
		});

		operate(() -> {
			PermHandler.setPermissions(Node.TYPE_NODE, ObjectTransformer.getInt(node.getFolder().getId(), 0), Arrays.asList(restrictedGroup), PermHandler.FULL_PERM);
			PermHandler.setPermissions(Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), 0), Arrays.asList(restrictedGroup), PermHandler.EMPTY_PERM);
			PermHandler.setPermissions(Node.TYPE_NODE, ObjectTransformer.getInt(master.getFolder().getId(), 0), Arrays.asList(restrictedGroup), PermHandler.FULL_PERM);
		});

		visiblePage = createPage(nodeRootFolder, template, RESTRICTED_PAGE);
		invisiblePage = createPage(folder, template, RESTRICTED_PAGE);

		invisibleMasterPage = createPage(masterRootFolder, template, RESTRICTED_MC_PAGE);
		visibleChannelPage = localize(invisibleMasterPage, channel);
	}

	/**
	 * Create a new page. Transaction will be committed but not closed.
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @return new page (not editable)
	 * @throws Exception
	 */
	protected static Page createPage(Folder folder, Template template, String name) throws Exception {
		return create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName(NAME_PREFIX + name + NAME_POSTFIX);
		}).unlock().build();
	}

	/**
	 * Localize the given page in the channel.
	 * The transaction will be committed, but not closed.
	 * @param master master page
	 * @param channel channel
	 * @return localized copy (not editable).
	 * @throws Exception
	 */
	protected static Page localize(Page master, Node channel) throws Exception {
		return localize(master, channel, null);
	}

	/**
	 * Localize the given page in the channel with a different name.
	 * The transaction will be committed, but not closed.
	 * @param master master page
	 * @param channel channel
	 * @param channelName name of the localized copy
	 * @return localized copy (not editable)
	 * @throws Exception
	 */
	protected static Page localize(Page master, Node channel, String channelName) throws Exception {
		return supply(t -> {
			Page localizedCopy = (Page) master.copy();
			localizedCopy.setChannelInfo(channel.getId(), master.getChannelSetId());
			if (!ObjectTransformer.isEmpty(channelName)) {
				localizedCopy.setName(channelName);
			}
			localizedCopy.save();
			return t.getObject(Page.class, localizedCopy.getId());
		});
	}

	/**
	 * Test querying by name
	 * @throws Exception
	 */
	@Test
	public void testQueryName() throws Exception {
		doAutocompleteRequest(QUERY_BY_NAME, Arrays.asList(node, supply(() -> queryByNamePage.getFolder()), queryByNamePage));
	}

	/**
	 * Test querying by ID
	 * @throws Exception
	 */
	@Test
	public void testQueryID() throws Exception {
		doAutocompleteRequest(ObjectTransformer.getString(queryByIdPage.getId(), ""), Arrays.asList(node, supply(() -> queryByIdPage.getFolder()), queryByIdPage));
	}

	/**
	 * Test query limit
	 * @throws Exception
	 */
	@Test
	public void testLimit() throws Exception {
		for (int i = 0; i < NUM_LIMIT_PAGES + 1; i++) {
			List<NodeObject> expected = new ArrayList<NodeObject>();
			if (i > 0) {
				expected.add(node);
				expected.add(supply(() -> limitPages.get(0).getFolder()));
				expected.addAll(limitPages.subList(0, i));
			}
			doAutocompleteRequest(QUERY_WITH_LIMIT, i, expected);
		}
	}

	/**
	 * Test querying with too short strings
	 * @throws Exception
	 */
	@Test
	public void testShortQuery() throws Exception {
		doAutocompleteRequest("Me", new ArrayList<NodeObject>());
	}

	/**
	 * Test querying without permission on the page
	 * @throws Exception
	 */
	@Test
	public void testPermissions() throws Exception {
		doAutocompleteRequest(RESTRICTED_PAGE, Arrays.asList(node, supply(() -> invisiblePage.getFolder()), invisiblePage, supply(() -> node.getFolder()), visiblePage));

		try (DBSessionClosure ses = new DBSessionClosure(restrictedUser.getId())) {
			doAutocompleteRequest(RESTRICTED_PAGE, Arrays.asList(node, supply(() -> node.getFolder()), visiblePage));
		}
	}

	/**
	 * Test querying where the page only matches in the master
	 * @throws Exception
	 */
	@Test
	public void testMatchInMaster() throws Exception {
		doAutocompleteRequest(MATCH_IN_MASTER, Arrays.asList(master, supply(() -> master.getFolder()), matchInMaster));
	}

	/**
	 * Test querying where the page only matches in the channel
	 * @throws Exception
	 */
	@Test
	public void testMatchInChannel() throws Exception {
		doAutocompleteRequest(MATCH_IN_CHANNEL, Arrays.asList(channel, supply(() -> channel.getFolder()), matchInChannelCopy));
	}

	/**
	 * Test querying where the page matches in master and channel
	 * @throws Exception
	 */
	@Test
	public void testMatchInMasterAndChannel() throws Exception {
		doAutocompleteRequest(MATCH_IN_MASTER_AND_CHANNEL,
				Arrays.asList(master, supply(() -> master.getFolder()), matchInMasterAndChannel, channel, supply(() -> channel.getFolder()), matchInMasterAndChannelCopy));
	}

	/**
	 * Test querying whith users that are restricted to channels
	 * @throws Exception
	 */
	@Test
	public void testChannelPermissions() throws Exception {
		doAutocompleteRequest(RESTRICTED_MC_PAGE,
				Arrays.asList(master, supply(() -> master.getFolder()), invisibleMasterPage, channel, supply(() -> channel.getFolder()), visibleChannelPage));

		try (DBSessionClosure ses = new DBSessionClosure(restrictedUser.getId())) {
			doAutocompleteRequest(RESTRICTED_MC_PAGE,
					Arrays.asList(channel, supply(() -> channel.getFolder()), visibleChannelPage));
		}
	}

	/**
	 * Do the autocompletion test. Send the query String to the REST API, collect the response and compare with the response that contains the given list of expected objects.
	 * The limit will be set to 15 (default value).
	 * @param query query string
	 * @param expected list of expected objects. This not only contains the Pages, but also Node and Folder objects.
	 * @throws Exception
	 */
	protected void doAutocompleteRequest(String query, List<? extends NodeObject> expected) throws Exception {
		doAutocompleteRequest(query, 15, expected);
	}

	/**
	 * Do the autocompletion test. Send the query String to the REST API, collect the response and compare with the response that contains the given list of expected objects.
	 * @param query query string
	 * @param limit limit of pages returned
	 * @param expected list of expected objects. This not only contains the Pages, but also Node and Folder objects.
	 * @throws Exception
	 */
	protected void doAutocompleteRequest(String query, int limit, List<? extends NodeObject> expected) throws Exception {
		// construct expected response
		StringBuilder expectedResponse = new StringBuilder();

		operate(() -> {
			Node lastNode = null;
			for (NodeObject nodeObject : expected) {
				if (nodeObject instanceof Node) {
					lastNode = (Node)nodeObject;
					expectedResponse.append("<div class=\"ac_node\">").append(((Node)nodeObject).getFolder().getName()).append("</div>");
				} else if (nodeObject instanceof Folder) {
					expectedResponse.append("<div class=\"ac_folder\">").append(((Folder)nodeObject).getName()).append("</div>");
				} else if (nodeObject instanceof Page) {
					if (lastNode == null) {
						fail("Cannot expect a page in the response without a node first");
					}
					expectedResponse.append("<div class=\"ac_page\" page_id=\"").append(nodeObject.getId()).append("\" node_id=\"").append(lastNode.getId())
					.append("\">").append(((Page) nodeObject).getName()).append("</div>\n");
				}
			}
		});

		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		String response = pageResource.autocomplete(query, limit);
		assertEquals("Check expected response", expectedResponse.toString(), response);
	}
}
