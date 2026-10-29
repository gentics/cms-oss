package com.gentics.contentnode.tests.linkchecker;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Objects;
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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkList;
import com.gentics.contentnode.rest.model.linkchecker.ReplaceExternalLinkRequest;
import com.gentics.contentnode.rest.model.linkchecker.ReplaceScope;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.LinkCheckerResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for search and replace of URLs with the LinkChecker
 */
@GCNFeature(set = { Feature.LINK_CHECKER, Feature.WASTEBIN })
@RunWith(value = Parameterized.class)
public class LinkCheckerReplaceTest {
	public final static String PART = "url";

	public final static String LINK_TAG = "pageurl1";

	public final static String OTHER_LINK_TAG = "pageurl2";

	public final static String DIFFERENT_LINK_TAG = "pageurl3";

	public final static String LINK_ONLY_URL = "http://replace.link.only/";

	public final static String PAGE_URL = "http://replace.in.page/";

	public final static String NODE_URL = "http://replace.in.node/";

	public final static String GLOBAL_URL = "http://replace.global/";

	public final static long TIMESTAMP_CREATE = System.currentTimeMillis() - 60*1000L;

	public static final long TIMESTAMP_PUBLISH = TIMESTAMP_CREATE + 1000L;

	public static final long TIMESTAMP_MODIFY = TIMESTAMP_CREATE + 2000L;

	public static final long TIMESTAMP_DELETE = TIMESTAMP_CREATE + 3000L;

	public final static Map<ReplaceScope, String> urlMap = new HashMap<>();

	static {
		urlMap.put(ReplaceScope.link, LINK_ONLY_URL);
		urlMap.put(ReplaceScope.page, PAGE_URL);
		urlMap.put(ReplaceScope.node, NODE_URL);
		urlMap.put(ReplaceScope.global, GLOBAL_URL);
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static UserGroup testGroup;
	private static SystemUser testUser;

	private static Node node;
	private static Node otherNode;
	private static FolderSet nodeFolders;
	private static FolderSet otherNodeFolders;

	private static Integer urlConstructId;
	private static Template template;

	private static Map<ReplaceScope, Page> sourcePages = new HashMap<>();

	/**
	 * Get test variation data
	 * @return variation data
	 */
	@Parameters(name = "{index}: scope {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (ReplaceScope scope : ReplaceScope.values()) {
			data.add(new Object[] { scope });
		}
		return data;
	}

	/**
	 * Setup the test data. Generate pages containing external links, and also does the replacing
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("Test", "User", null, "testuser", "testuser", Arrays.asList(testGroup)));

		node = supply(() -> createNode());
		operate(() -> node.activateFeature(Feature.LINK_CHECKER));
		grantPermissions(supply(() -> node.getFolder()), PermType.read, PermType.readitems, PermType.updateitems);

		otherNode = supply(() -> createNode());
		operate(() -> otherNode.activateFeature(Feature.LINK_CHECKER));
		grantPermissions(supply(() -> otherNode.getFolder()), PermType.read, PermType.readitems);

		urlConstructId = supply(() -> createConstruct(node, PageURLPartType.class, "pageurl", PART));
		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setName("Template");
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.getTags().put(LINK_TAG, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(urlConstructId);
				tTag.setEnabled(true);
				tTag.setName(LINK_TAG);
				tTag.setPublic(true);
			}, false));
			tmpl.getTags().put(OTHER_LINK_TAG, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(urlConstructId);
				tTag.setEnabled(true);
				tTag.setName(OTHER_LINK_TAG);
				tTag.setPublic(true);
			}, false));
			tmpl.getTags().put(DIFFERENT_LINK_TAG, create(TemplateTag.class, tTag -> {
				tTag.setConstructId(urlConstructId);
				tTag.setEnabled(true);
				tTag.setName(DIFFERENT_LINK_TAG);
				tTag.setPublic(true);
			}, false));
		}));

		// create folders and pages with URLs
		nodeFolders = new FolderSet(node);
		otherNodeFolders = new FolderSet(otherNode);

		// create the source pages (for which the "replace" will be called)
		for (ReplaceScope scope : ReplaceScope.values()) {
			String url = urlMap.get(scope);
			String alternateUrl = url + "different";
			sourcePages.put(scope,
					createPage(node, supply(() -> node.getFolder()), "Source Page", url, alternateUrl, false, 0, false, true, false));
		}

		// replace URLs
		for (Map.Entry<ReplaceScope, Page> entry : sourcePages.entrySet()) {
			ReplaceScope scope = entry.getKey();
			Page sourcePage = entry.getValue();
			operate(testUser, t -> {
				ExternalLinkList list = new LinkCheckerResourceImpl().getLinks(Integer.toString(sourcePage.getId()));
				assertResponseOK(list);
				assertThat(list.getItems()).as("Links in page").isNotEmpty();
				Optional<ExternalLink> sourceLink = list.getItems().stream().filter(link -> Objects.areEqual(LINK_TAG, link.getContenttagName())).findFirst();
				assertThat(sourceLink).as("Source link").isPresent();

				GenericResponse updateResponse = new LinkCheckerResourceImpl().updateLink(Integer.toString(sourcePage.getId()), sourceLink.get().getId(),
						0, new ReplaceExternalLinkRequest().setUrl(urlMap.get(scope) + "updated").setScope(scope));
				assertResponseOK(updateResponse);
			});
		}

		// reload all pages
		nodeFolders.reload();
		otherNodeFolders.reload();
	}

	/**
	 * Grant the given permissions on the folder to the test group
	 * @param folder folder
	 * @param perms permissions to grant
	 * @throws NodeException
	 */
	protected static void grantPermissions(Folder folder, PermType... perms) throws NodeException {
		operate(t -> {
			Permissions p = Permissions.set(Permissions.get(), perms);
			MiscUtils.setPermissions(folder, Arrays.asList(testGroup), Permissions.toString(p), Collections.emptySet(), t.getPermHandler(), false);
		});
	}

	/**
	 * Create a page containing external URLs
	 * @param node node
	 * @param folder folder
	 * @param name page name
	 * @param url external URL (which might be replaced)
	 * @param alternateUrl other external URL (which never will be replaced)
	 * @param publish true to publish the page
	 * @param publishAt optional publish at to plan the page
	 * @param modify true to modify the page (after publishing)
	 * @param unlock true to unlock the page
	 * @param delete true to delete the page (put into wastebin)
	 * @return page
	 * @throws NodeException
	 */
	protected static Page createPage(Node node, Folder folder, String name, String url, String alternateUrl,
			boolean publish, int publishAt, boolean modify, boolean unlock, boolean delete) throws NodeException {
		return supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(TIMESTAMP_CREATE);

			Page created = create(Page.class, c -> {
				c.setFolderId(folder.getId());
				c.setTemplateId(template.getId());
				c.setName(name);
				if (node != null && node.isChannel()) {
					c.setChannelInfo(node.getId(), c.getChannelSetId());
				}
				getPartType(PageURLPartType.class, c.getContentTag(LINK_TAG), PART).setExternalTarget(url);
				getPartType(PageURLPartType.class, c.getContentTag(OTHER_LINK_TAG), PART).setExternalTarget(url);
				getPartType(PageURLPartType.class, c.getContentTag(DIFFERENT_LINK_TAG), PART).setExternalTarget(alternateUrl);
			});

			if (publish || unlock) {
				t.setTimestamp(TIMESTAMP_PUBLISH);
				Page editable = t.getObject(created, true);
				if (publish) {
					editable.publish(publishAt, null);
				}
				if (unlock) {
					editable.unlock();
				}
				t.commit(false);
			}

			if (modify) {
				t.setTimestamp(TIMESTAMP_MODIFY);
				Page update = t.getObject(created, true);
				update.setName("Modified" + update.getName());
				if (unlock) {
					update.unlock();
				}
				update.save();
			}

			if (delete) {
				t.setTimestamp(TIMESTAMP_DELETE);
				created.delete();
			}

			try (WastebinFilter wf = new WastebinFilter(Wastebin.INCLUDE)) {
				return created.reload();
			}
		});
	}

	/**
	 * Assert that the tags in the page contain the given URLs
	 * @param page page
	 * @param url expected URL in the {@link #LINK_TAG}
	 * @param otherUrl expected URL in the {@link #OTHER_LINK_TAG}
	 * @param differentUrl expected URL in the {@link #DIFFERENT_LINK_TAG}
	 * @throws NodeException
	 */
	protected static void assertUrls(Page page, String url, String otherUrl, String differentUrl) throws NodeException {
		operate(() -> {
			assertThat(getPartType(PageURLPartType.class, page.getContentTag(LINK_TAG), PART).getExternalTarget()).as("Link URL").isEqualTo(url);
			assertThat(getPartType(PageURLPartType.class, page.getContentTag(OTHER_LINK_TAG), PART).getExternalTarget()).as("Other Link URL").isEqualTo(otherUrl);
			assertThat(getPartType(PageURLPartType.class, page.getContentTag(DIFFERENT_LINK_TAG), PART).getExternalTarget()).as("Different Link URL").isEqualTo(differentUrl);
		});
	}

	/**
	 * Test parameter "scope"
	 */
	@Parameter(0)
	public ReplaceScope scope;

	/**
	 * Expected unchanged URL
	 */
	protected String unchanged;

	/**
	 * Expected changed URL
	 */
	protected String changed;

	/**
	 * Expected different URL
	 */
	protected String different;

	/**
	 * Setup expected URLs
	 */
	@Before
	public void setup() {
		unchanged = urlMap.get(scope);
		changed = urlMap.get(scope) + "updated";
		different = urlMap.get(scope) + "different";
	}

	/**
	 * Test the source page itself
	 * @throws NodeException
	 */
	@Test
	public void testPage() throws NodeException {
		assertUrls(sourcePages.get(scope), changed, scope == ReplaceScope.link ? unchanged : changed, different);
	}

	/**
	 * Test pages with edit permission in the same node
	 * @throws NodeException
	 */
	@Test
	public void testEditSameNode() throws NodeException {
		PageSet pageSet = nodeFolders.pages.get(Pair.of(nodeFolders.edit, scope));
		boolean expectChange = scope == ReplaceScope.node || scope == ReplaceScope.global;

		// check offline page
		assertUrls(pageSet.offlinePage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Test pages with only view permission in the same node
	 * @throws NodeException
	 */
	@Test
	public void testViewSameNode() throws NodeException {
		PageSet pageSet = nodeFolders.pages.get(Pair.of(nodeFolders.view, scope));

		// check offline page
		assertUrls(pageSet.offlinePage, unchanged, unchanged, different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, unchanged, unchanged, different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, unchanged, unchanged, different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, unchanged, unchanged, different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Test pages without view permission in the same node
	 * @throws NodeException
	 */
	@Test
	public void testNoViewSameNode() throws NodeException {
		PageSet pageSet = nodeFolders.pages.get(Pair.of(nodeFolders.noView, scope));

		// check offline page
		assertUrls(pageSet.offlinePage, unchanged, unchanged, different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, unchanged, unchanged, different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, unchanged, unchanged, different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, unchanged, unchanged, different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Test pages with edit permission in another node
	 * @throws NodeException
	 */
	@Test
	public void testEditOtherNode() throws NodeException {
		PageSet pageSet = otherNodeFolders.pages.get(Pair.of(otherNodeFolders.edit, scope));
		boolean expectChange = scope == ReplaceScope.global;

		// check offline page
		assertUrls(pageSet.offlinePage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, expectChange ? changed : unchanged, expectChange ? changed : unchanged,
				different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Test pages with view permission in another node
	 * @throws NodeException
	 */
	@Test
	public void testViewOtherNode() throws NodeException {
		PageSet pageSet = otherNodeFolders.pages.get(Pair.of(otherNodeFolders.view, scope));

		// check offline page
		assertUrls(pageSet.offlinePage, unchanged, unchanged, different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, unchanged, unchanged, different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, unchanged, unchanged, different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, unchanged, unchanged, different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Test pages without view permission in another node
	 * @throws NodeException
	 */
	@Test
	public void testNoViewOtherNode() throws NodeException {
		PageSet pageSet = otherNodeFolders.pages.get(Pair.of(otherNodeFolders.noView, scope));

		// check offline page
		assertUrls(pageSet.offlinePage, unchanged, unchanged, different);
		assertThat(pageSet.offlinePage).as("Offline page").isOffline();

		// check published page
		assertUrls(pageSet.publishedPage, unchanged, unchanged, different);
		assertThat(pageSet.publishedPage).as("Published page").isOnline().isNotModified();

		// check modified page
		assertUrls(pageSet.modifiedPage, unchanged, unchanged, different);
		assertThat(pageSet.modifiedPage).as("Modified page").isOnline().isModified();

		// check planned page
		assertUrls(pageSet.plannedPage, unchanged, unchanged, different);
		assertThat(pageSet.plannedPage).as("Planned page").isOffline().isPlanned();

		// check locked page
		assertUrls(pageSet.lockedPage, unchanged, unchanged, different);

		// check deleted page
		assertUrls(pageSet.deletedPage, unchanged, unchanged, different);
	}

	/**
	 * Collection of folders containing various pages
	 */
	protected static class FolderSet {
		/**
		 * Folder with edit permission for pages
		 */
		protected Folder edit;

		/**
		 * Folder with only view permission for pages
		 */
		protected Folder view;
	
		/**
		 * Folder without view permission for pages
		 */
		protected Folder noView;
	
		/**
		 * Page sets per folder and replace scope
		 */
		protected Map<Pair<Folder, ReplaceScope>, PageSet> pages = new HashMap<>();

		/**
		 * Create folders and pages in the node
		 * @param node node
		 * @throws NodeException
		 */
		public FolderSet(Node node) throws NodeException {
			edit = supply(() -> createFolder(node.getFolder(), "Edit"));
			grantPermissions(edit, PermType.read, PermType.readitems, PermType.updateitems);
	
			view = supply(() -> createFolder(node.getFolder(), "View"));
			grantPermissions(view, PermType.read, PermType.readitems);
	
			noView = supply(() -> createFolder(node.getFolder(), "No View"));
			grantPermissions(noView, PermType.read);
	
			for (Folder f : Arrays.asList(edit, view, noView)) {
				for (ReplaceScope scope : ReplaceScope.values()) {
					String url = urlMap.get(scope);
					pages.put(Pair.of(f, scope), new PageSet(node, f, url, url + "different"));
				}
			}
		}

		/**
		 * Reload all pages
		 * @throws NodeException
		 */
		protected void reload() throws NodeException {
			for (PageSet set : pages.values()) {
				set.reload();
			}
		}
	}

	/**
	 * Collection of pages (in a folder)
	 */
	protected static class PageSet {
		/**
		 * Offline page
		 */
		protected Page offlinePage;

		/**
		 * Published page
		 */
		protected Page publishedPage;

		/**
		 * Published and modified page
		 */
		protected Page modifiedPage;

		/**
		 * Planned page
		 */
		protected Page plannedPage;

		/**
		 * Locked page
		 */
		protected Page lockedPage;

		/**
		 * Deleted page (in wastebin)
		 */
		protected Page deletedPage;

		/**
		 * Create pages
		 * @param node node
		 * @param folder folder
		 * @param url primary URL (might be replaced)
		 * @param alternateUrl different URL (never will be replaced)
		 * @throws NodeException
		 */
		public PageSet(Node node, Folder folder, String url, String alternateUrl) throws NodeException {
			offlinePage = createPage(node, folder, "Offline Page", url, alternateUrl, false, 0, false, true, false);
			publishedPage = createPage(node, folder, "Published Page", url, alternateUrl, true, 0, false, true, false);
			modifiedPage = createPage(node, folder, "Modified Page", url, alternateUrl, true, 0, true, true, false);
			plannedPage = createPage(node, folder, "Planned Page", url, alternateUrl, true,
					(int) (System.currentTimeMillis() / 1000) + 86400, false, true, false);
			lockedPage = createPage(node, folder, "Locked Page", url, alternateUrl, false, 0, false, false, false);
			deletedPage = createPage(node, folder, "Deleted Page", url, alternateUrl, false, 0, false, true, true);
		}

		/**
		 * Reload all pages
		 * @throws NodeException
		 */
		protected void reload() throws NodeException {
			offlinePage = execute(Page::reload, offlinePage);
			publishedPage = execute(Page::reload, publishedPage);
			modifiedPage = execute(Page::reload, modifiedPage);
			plannedPage = execute(Page::reload, plannedPage);
			lockedPage = execute(Page::reload, lockedPage);
			deletedPage = execute(p -> {
				try (WastebinFilter wb = new WastebinFilter(Wastebin.INCLUDE)) {
					return p.reload();
				}
			}, deletedPage);
		}
	}
}
