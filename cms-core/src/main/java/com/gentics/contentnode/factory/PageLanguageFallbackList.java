/*
 * @author norbert
 * @date 09.02.2011
 * @version $Id: PageLanguageFallbackList.java,v 1.1.2.2 2011-02-10 13:27:41 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.lib.db.SQLExecutor;

/**
 * This class implements the fallback mechanism for language variants:
 * <ol>
 * <li>An instance is created for a given language (id) and a Node (if no Node is given, the fallback list will just filter the pages with the given language)</li>
 * <li>Then, pages are added</li>
 * <li>For every added page, it is checked, whether a better fitting language is already added. If yes, the added page is ignored, if not, the page is added and possibly already added pages of the same contentset are removed</li>
 * </ol>
 * @author norbert
 */
public class PageLanguageFallbackList {

	/**
	 * Map representing the language priority order. Every contentgroup id is
	 * mapped to an ordinal number (lower is better)
	 */
	protected Map<ContentLanguage, Integer> languagePriorityOrder = new HashMap<ContentLanguage, Integer>();

	/**
	 * list of pages that were added without a contentgroup id (no fallback is
	 * done for those objects)
	 */
	protected List<PageInstance> pagesWithoutContentgroup = new ArrayList<>();

	/**
	 * Map of pages. Keys are the contentgroup ids, values contain the pages and
	 * page order. This map is used when sticky channel is false
	 */
	protected Map<Integer, PageInstance> pagesWithContentgroup = new HashMap<>();

	/**
	 * Map of page maps per node. This map is used when sticky channel is true
	 */
	protected Map<Integer, Map<Integer, PageInstance>> pagesWithContentgroupPerNode = new HashMap<>();

	/**
	 * If this flag is set to true, every added page will be checked for view permission
	 */
	protected boolean checkViewPermission = false;

	/**
	 * Flag whether to use a lightweigth page list
	 */
	protected boolean useLightWeightPageList = false;

	/**
	 * Flag whether to use sticky channel
	 */
	protected boolean stickyChannel = false;

	/**
	 * Page counter
	 */
	protected int pageCounter = 0;

	/**
	 * Do language fallback for the given page. This will also only consider online pages
	 * @param page page
	 * @param language target language
	 * @param node node
	 * @return fallback page (may be null)
	 * @throws NodeException
	 */
	public static Page doFallback(Page page, ContentLanguage language, Node node) throws NodeException {
		PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(language, node);

		if (page.getLanguage() != null) {
			// page has a language, so add all online page variants
			for (Page variant : page.getLanguageVariants(true)) {
				if (variant.isOnline()) {
					fallbackList.addPage(variant);
				}
			}
		} else if (page.isOnline()) {
			// page has no language, but is online, so we add it
			fallbackList.addPage(page);
		}

		List<Page> pages = fallbackList.getPages();
		if (!pages.isEmpty()) {
			return pages.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Create an instance of the language fallback list
	 * @param language given language
	 * @param node node
	 * @throws NodeException
	 */
	public PageLanguageFallbackList(ContentLanguage language, Node node) throws NodeException {
		this(language, node, false, false);
	}

	/**
	 * Create an instance of the language fallback list
	 * @param language given language
	 * @param node node
	 * @param useLightWeightPageList true to use an instance of {@link LightWeightPageList}
	 * @param stickyChannel true for sticky channel
	 * @throws NodeException
	 */
	public PageLanguageFallbackList(ContentLanguage language, Node node, boolean useLightWeightPageList, boolean stickyChannel) throws NodeException {
		this.useLightWeightPageList = useLightWeightPageList;
		this.stickyChannel = stickyChannel;
		// we build the priority order

		// first add the given contentgroup with highest prio
		int order = 1;

		if (language != null) {
			languagePriorityOrder.put(language, order++);
		}

		if (node != null) {
			// now get all the languages of the node and add them (but not the given
			// language again
			List<ContentLanguage> languages = node.getLanguages();

			// when the node has no languages, we just add ALL existing languages in the order of their ids
			if (ObjectTransformer.isEmpty(languages)) {
				final List<Integer> langIds = new Vector<Integer>();

				DBUtils.executeStatement("SELECT id FROM contentgroup ORDER BY id", new SQLExecutor() {
					@Override
					public void handleResultSet(ResultSet rs) throws SQLException,
								NodeException {
						while (rs.next()) {
							langIds.add(rs.getInt("id"));
						}
					}
				});
				Transaction t = TransactionManager.getCurrentTransaction();

				languages = t.getObjects(ContentLanguage.class, langIds);
			}

			for (ContentLanguage nodeLanguage : languages) {
				if (!nodeLanguage.equals(language)) {
					languagePriorityOrder.put(nodeLanguage, order++);
				}
			}
		}
	}

	/**
	 * Set whether pages shall be checked for view permission
	 * @param checkViewPermission true if pages shall be checked for view permission, false if not
	 */
	public void setCheckViewPermission(boolean checkViewPermission) {
		this.checkViewPermission = checkViewPermission;
	}

	/**
	 * Get whether pages shall be checked for view permission
	 * @return true if pages shall be checked for view permission, false if not
	 */
	public boolean isCheckViewPermission() {
		return checkViewPermission;
	}

	/**
	 * Add a page to the list
	 * @param page page to be added
	 */
	public void addPage(Page page) throws NodeException {
		// the page probably needs to be checked for view permission
		if (checkViewPermission) {
			// no view permission, so simply ignore the page
			if (!PermHandler.ObjectPermission.view.checkObject(page)) {
				return;
			}
		}

		// now check whether the channelSetId is empty
		if (AbstractContentObject.isEmptyId(page.getLanguageId()) || AbstractContentObject.isEmptyId(page.getContentsetId())) {
			PageInstance instance = new PageInstance(page);
			if (!pagesWithoutContentgroup.contains(instance)) {
				pagesWithoutContentgroup.add(instance);
			}
		} else {
			// create an instance
			PageInstance instance = new PageInstance(page);
			Map<Integer, PageInstance> pageMap = null;

			if (stickyChannel) {
				Integer nodeId = page.getNode().getId();
				pageMap = pagesWithContentgroupPerNode.computeIfAbsent(nodeId, key -> new HashMap<>());
			} else {
				pageMap = pagesWithContentgroup;
			}
			// check whether the instance is better then the currently stored
			// with the same contentset id
			if (instance.isBetterThan(pageMap.get(page.getContentsetId()))) {
				pageMap.put(page.getContentsetId(), instance);
			}
		}
	}

	/**
	 * Get the currently stored pages. A call to this method will generate a new
	 * list and therefore should not be called too often
	 * @return currently stored pages
	 */
	public List<Page> getPages() {
		// aggregate the page instances in the list
		List<PageInstance> aggregateList = new ArrayList<>();
		aggregateList.addAll(pagesWithoutContentgroup);
		if (stickyChannel) {
			pagesWithContentgroupPerNode.values().forEach(map -> {
				aggregateList.addAll(map.values());
			});
		} else {
			aggregateList.addAll(pagesWithContentgroup.values());
		}

		// sort by pageOrder
		Collections.sort(aggregateList, (i1, i2) -> i1.pageOrder - i2.pageOrder);

		// transform the page list
		if (useLightWeightPageList) {
			LightWeightPageList pages = new LightWeightPageList(aggregateList.size(), stickyChannel);
			aggregateList.forEach(instance -> pages.addPage(instance.pageId, instance.nodeId));
			return pages;
		} else {
			return aggregateList.stream().map(instance -> instance.page).collect(Collectors.toList());
		}
	}

	/**
	 * Inner class for grouping object id and channel id together
	 */
	protected class PageInstance {
		/**
		 * Page of the instance
		 */
		protected Page page;

		/**
		 * Page ID of the instance
		 */
		protected int pageId;

		/**
		 * Node ID of the instance
		 */
		protected int nodeId;

		/**
		 * Language order of the object
		 */
		protected int languageOrder;

		/**
		 * Page order
		 */
		protected int pageOrder;

		/**
		 * Create a page instance for the given page
		 * @param page page
		 */
		public PageInstance(Page page) throws NodeException {
			this.pageOrder = pageCounter++;
			this.pageId = page.getId();
			this.nodeId = page.getNode().getId();
			if (!useLightWeightPageList) {
				this.page = page;
			}
			ContentLanguage language = page.getLanguage();

			// determine the language priority order
			if (!languagePriorityOrder.containsKey(language)) {
				// if the page has an invalid language, it has highest order (least relevance)
				this.languageOrder = Integer.MAX_VALUE;
			} else {
				this.languageOrder = languagePriorityOrder.get(language);
			}
		}

		/**
		 * Check whether this page instance is better than
		 * the given pageInstance
		 * @param pageInstance page instance to compare with
		 * @return true if this page instance is better and shall be kept in
		 *         the list, false if not
		 */
		public boolean isBetterThan(PageInstance pageInstance) {
			// an instance is always better then no instance
			if (pageInstance == null) {
				return true;
			}
			// smaller channel order is better
			return languageOrder <= pageInstance.languageOrder;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PageInstance) {
				PageInstance other = (PageInstance)obj;
				return pageId == other.pageId && nodeId == other.nodeId;
			} else if (obj instanceof Page) {
				Page other = (Page)obj;
				return pageId == other.getId();
			} else {
				return false;
			}
		}
	}
}
