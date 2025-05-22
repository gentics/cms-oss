/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: PageFactory.java,v 1.60.2.1.2.13.2.12 2011-03-18 17:07:17 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import static com.gentics.api.lib.etc.ObjectTransformer.getInteger;
import static com.gentics.contentnode.i18n.I18NHelper.getLocation;
import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.CollectionUtils;

import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.url.AlternateUrlsContainer;
import com.gentics.contentnode.factory.url.PageAlternateUrlsContainer;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.AbstractPage;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Page.OnlineStatusChange;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserGroup.ReductionType;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.object.page.PageCopyOpResultInfo;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolver;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.string.CNStringUtils;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.contentnode.factory.object.TableVersion;
import com.gentics.contentnode.factory.object.TableVersion.Diff;
import com.gentics.contentnode.factory.object.TableVersion.Join;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

import io.reactivex.Observable;

/**
 * An objectfactory which can create {@link Page} and {@link Content} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Page.class, name = "page"),
	@DBTable(clazz = Content.class, name = "content") })
public class PageFactory extends AbstractFactory {
	/**
	 * Name of the transaction attribute, that stored contents that are locked by this transaction
	 */
	protected final static String LOCKED_CONTENTS_IN_TRX = "PageFactory.lockedContentInTrx";

	/**
	 * Logcmd Info when pages are put into the wastebin
	 */
	protected final static String LOGCMD_WASTEBIN_INFO = "Wastebin";

	/**
	 * Cache key for 2nd level cache isOnline flag
	 */
	public static final String ONLINE_CACHE_KEY = "pageonline";

	public static final String DELAY_PUBLISH_CACHE_KEY = "pagepublishdelay";

	public static final String STATUS_CACHE_KEY = "pagestatus";

	/**
	 * Key of the transaction attribute, that stores pdates for pages
	 */
	public static final String PDATE_KEY = "page.pdate";

	/**
	 * SQL Statement to insert a new page
	 */
	protected final static String INSERT_PAGE_SQL = "INSERT INTO page (name, nice_url, description, filename, priority, "
			+ "content_id, template_id, folder_id, creator, cdate, custom_cdate, editor, edate, custom_edate, contentgroup_id, contentset_id, "
			+ "channelset_id, "
			+ "channel_id, sync_page_id, sync_timestamp, is_master, mc_exclude, disinherit_default, uuid, modified) VALUES "
			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

	protected final static String INSERT_CONTENTSET_SQL = "INSERT INTO contentset (id, uuid) VALUES (NULL, ?)";

	/**
	 * SQL Statement to update a new page
	 */
	protected final static String UPDATE_PAGE_SQL = "UPDATE page SET name = ?, nice_url = ?, description = ?, filename = ?, priority = ?, "
			+ "content_id = ?, template_id = ?, folder_id = ?, editor = ?, custom_cdate = ?, edate = ?, custom_edate = ?, contentgroup_id = ?, "
			+ "contentset_id = ?, channelset_id = ?, channel_id = ?, sync_page_id = ?, sync_timestamp = ?, modified = 1 "
			+ "WHERE id = ?";

	protected final static String INSERT_CONTENT_SQL = "INSERT INTO content (creator, cdate, editor, edate, locked, locked_by, uuid) VALUES " + "(?, ?, ?, ?, ?, ?, ?)";

	protected final static String UPDATE_CONTENT_SQL = "UPDATE content SET editor = ?, edate = ? WHERE id = ?";

	/**
	 * SQL Statement to select page data
	 */
	protected final static String SELECT_PAGE_SQL = createSelectStatement("page");

	/**
	 * SQL Statement for batchloading pages
	 */
	protected final static String BATCHLOAD_PAGE_SQL = createBatchLoadStatement("page");

	/**
	 * SQL Statement to select content data
	 */
	protected final static String SELECT_CONTENT_SQL = createSelectStatement("content");

	/**
	 * SQL Statement for batchloading contents
	 */
	protected final static String BATCHLOAD_CONTENT_SQL = createBatchLoadStatement("content");

	/**
	 * SQL Statement to select versioned page data
	 */
	protected final static String SELECT_VERSIONED_PAGE_SQL = "SELECT page.* "
			+ "FROM page_nodeversion page "
			+ "WHERE page.id = ? AND nodeversiontimestamp = (SELECT MAX(nodeversiontimestamp) FROM page_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Params for the selection of versioned page data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_PAGE_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * SQL Statement to update the deleted flag of a page
	 */
	protected final static String UPDATE_PAGE_DELETEFLAG = "UPDATE page SET deleted = ?, deletedby = ? WHERE id = ?";

	/**
	 * Indexed status attributes
	 */
	protected final static String[] INDEXED_STATUS_ATTRIBUTES = { "queued", "modified", "online", "planned", "published", "publisherId", "publishAt",
			"offlineAt", "queuedPublishAt", "queuedOfflineAt" };

	/**
	 * Loader for {@link PageService}s
	 */
	protected final static ServiceLoaderUtil<PageService> pageFactoryServiceLoader = ServiceLoaderUtil
			.load(PageService.class);

	/**
	 * Implementation class of a page, contains internal data and implements getters for them
	 */
	private static class FactoryPage extends AbstractPage implements DisinheritableInternal<Page> {
		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 2178067666614792862L;

		protected final static String OBJECTTAGS = "objecttags";

		protected String name;
		protected String niceUrl;
		protected String description;
		protected String filename;
		protected Integer contentId;

		protected int priority = 1;
		protected Integer templateId;
		protected Integer folderId;
		protected Integer contentSetId;
		protected List<Integer> objectTagIds;
		protected Integer languageId;
		protected ContentNodeDate cDate = new ContentNodeDate(0);
		protected ContentNodeDate customCDate = new ContentNodeDate(0);
		protected ContentNodeDate eDate = new ContentNodeDate(0);
		protected ContentNodeDate customEDate = new ContentNodeDate(0);
		protected ContentNodeDate pDate = new ContentNodeDate(0);
		protected Integer creatorId = 0;
		protected Integer editorId = 0;
		protected Integer publisherId = 0;

		/**
		 * page ids of the language variants per channel
		 */
		protected Map<Integer, List<Integer>> languageVariantIds = new HashMap<Integer, List<Integer>>();

		/**
		 * node specific page ids of the language variants per channel
		 */
		protected Map<Integer, List<Integer>> nodeSpecificLanguageVariantIds = new HashMap<Integer, List<Integer>>();

		/**
		 * MD5 hash of the languages assigned to nodes
		 */
		protected Map<Integer, String> nodeLanguagesMD5 = new HashMap<Integer, String>();

		/**
		 * Publish At time
		 */
		protected ContentNodeDate timePub;

		/**
		 * Publish At Version
		 */
		protected Integer timePubVersion;

		/**
		 * Id of the user who put the page into queue for publish/publishAt
		 */
		protected int pubQueueUserId;

		/**
		 * Queued Publish At time
		 */
		protected ContentNodeDate timePubQueue;

		/**
		 * Queued Publish At Version
		 */
		protected Integer timePubVersionQueue;

		/**
		 * Take Offline time
		 */
		protected ContentNodeDate timeOff;

		/**
		 * Id of the user who put the page into queue for offline/offlinehAt
		 */
		protected int offQueueUserId;

		/**
		 * Queued take offline time
		 */
		protected ContentNodeDate timeOffQueue;

		/**
		 * id of the channelset, if the object is master or local copy in multichannelling
		 */
		protected int channelSetId;

		/**
		 * Map holding the localized variants of this page
		 */
		protected Map<Wastebin, Map<Integer, Integer>> channelSet;

		/**
		 * id of the channel, if the object is a local copy in multichannelling
		 */
		protected int channelId;

		/**
		 * True if this object is the master, false if it is a localized copy
		 */
		protected boolean master = true;

		/**
		 * id of the language variant, this page is synchronized with. 0 if the
		 * page is not synchronized with a language variant
		 */
		protected int syncPageId;

		/**
		 * timestamp of the language variant version, this page is synchronized
		 * with or null
		 */
		protected ContentNodeDate syncTimestamp = new ContentNodeDate(0);

		/**
		 * whether multichannelling inheritance is excluded for this page
		 */
		protected boolean excluded = false;

		/**
		 * Indicates whether this page is disinherited by default in new channels (default: false).
		 */
		protected boolean disinheritDefault = false;

		/**
		 * ids of channels that are disinherited for this page
		 */
		protected Set<Integer> disinheritedChannelIds = null;

		/**
		 * Timestamp of deletion, if the object was deleted (and pub into the wastebin), 0 if object is not deleted
		 */
		protected int deleted;

		/**
		 * ID of the user who put the object into the wastebin, 0 if not deleted
		 */
		protected int deletedBy;

		/**
		 * modified flag
		 */
		protected boolean pageModified;

		/**
		 * Map of alternate URLs to their internal IDs in the DB (maps to null, if entry is new)
		 */
		protected PageAlternateUrlsContainer alternateUrlsContainer;

		/**
		 * Create a new, empty instance of a page
		 * @param info
		 */
		protected FactoryPage(NodeObjectInfo info) {
			super(null, info);
			timePub = new ContentNodeDate(0);
			timePubQueue = new ContentNodeDate(0);
			timeOff = new ContentNodeDate(0);
			timeOffQueue = new ContentNodeDate(0);
		}

		/**
		 * @param folderId
		 *		  The folder ID of the page. Will be ignored if the given NodeObjectInfo indicates
		 *		  that this page object is not current. If this page object is created from versioned
		 *		  data and not current, the folderId will be delegated to the current page object.
		 */
		public FactoryPage(Integer id, NodeObjectInfo info, String name, String niceUrl, String description, String filename, Integer templateId,
				Integer folderId, Integer contentId, int priority, Integer contentSetId, Integer languageId, List<Integer> objectTagIds,
				ContentNodeDate cDate, ContentNodeDate customCDate, ContentNodeDate eDate, ContentNodeDate customEDate, ContentNodeDate pDate, int creatorId, int editorId, int publisherId, ContentNodeDate timePub,
				Integer timePubVersion, int pubQueueUserId, ContentNodeDate timePubQueue, Integer timePubVersionQueue, ContentNodeDate timeOff,
				int offQueueUserId, ContentNodeDate timeOffQueue, Integer channelSetId, Integer channelId, int syncPageId, ContentNodeDate syncTimestamp,
				boolean master, boolean excluded, boolean disinheritDefault, int deleted, int deletedBy, boolean pageModified, int udate, GlobalId globalId) {
			super(id, info);
			this.name = name;
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				this.niceUrl = niceUrl;
			} else {
				this.niceUrl = null;
			}
			this.description = description;
			this.filename = filename;
			this.templateId = templateId;

			// -1 is checked in getFolderId() to indicate no value is set with setFolderId()
			this.folderId = info.isCurrentVersion() ? folderId : -1;

			this.contentId = contentId;
			this.priority = priority;


			this.contentSetId = contentSetId;
			this.languageId = languageId;
			this.objectTagIds = objectTagIds != null ? new Vector<>(objectTagIds) : null;
			this.cDate = cDate;
			this.customCDate = customCDate;
			this.eDate = eDate;
			this.customEDate = customEDate;
			this.pDate = pDate;
			this.creatorId = new Integer(creatorId);
			this.editorId = new Integer(editorId);
			this.publisherId = new Integer(publisherId);
			this.timePub = timePub;
			this.timePubVersion = timePubVersion;
			this.pubQueueUserId = pubQueueUserId;
			this.timePubQueue = timePubQueue;
			this.timePubVersionQueue = timePubVersionQueue;
			this.timeOff = timeOff;
			this.offQueueUserId = offQueueUserId;
			this.timeOffQueue = timeOffQueue;
			this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			this.channelId = ObjectTransformer.getInt(channelId, 0);
			this.syncPageId = syncPageId;
			this.syncTimestamp = syncTimestamp;
			this.master = master;
			this.excluded = excluded;
			this.disinheritDefault = disinheritDefault;
			this.deleted = deleted;
			this.deletedBy = deletedBy;
			this.pageModified = pageModified;
			this.udate = udate;
			this.globalId = globalId;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#canDeleteContent()
		 */
		protected boolean canDeleteContent() throws NodeException {
			return getPageFactory().canDeleteContent(this);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#performDelete()
		 */
		protected void performDelete() throws NodeException {
			getPageFactory().deletePage(this);
		}

		@Override
		protected void putIntoWastebin() throws NodeException {
			if (isDeleted()) {
				return;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			// we use a linked hashset, because we want to have the master page treated last
			// the reason for this is that while taking the pages offline, links to the pages are checked
			// (for the master page) and while the link to the master page is checked, all localized copies already
			// have to be put into the wastebin, so that the master page is visible in all channels
			Set<Page> toPutIntoWastebin = new LinkedHashSet<Page>();
			toPutIntoWastebin.add(this);
			if (isMaster()) {
				toPutIntoWastebin.addAll(t.getObjects(Page.class, getChannelSet().values(), false, false));

				// now we remove and re-add the master page, to move it to the last position
				toPutIntoWastebin.remove(this);
				toPutIntoWastebin.add(this);
			}

			// for pages that are online before, we need to check for invalid links
			List<Integer> checkedPageIds = Observable.fromIterable(toPutIntoWastebin).filter(Page::isOnline).map(Page::getId).toList().blockingGet();
			if (!checkedPageIds.isEmpty()) {
				Map<Integer, Page> deletedPages = toPutIntoWastebin.stream().collect(Collectors.toMap(Page::getId, java.util.function.Function.identity()));
				PageFactory.checkInvalidLinks(checkedPageIds, deletedPages, new CNI18nString("deleted_lc"));
			}

			for (Page p : toPutIntoWastebin) {
				// take page offline.
				DBUtils.update("UPDATE page SET online = ? WHERE id = ?", 0, p.getId());

				// mark page as being deleted
				DBUtils.update(UPDATE_PAGE_DELETEFLAG, t.getUnixTimestamp(), t.getUserId(), p.getId());

				ActionLogger.logCmd(ActionLogger.WASTEBIN, Page.TYPE_PAGE, p.getId(), null, "Page.delete()");
				t.dirtObjectCache(Page.class, p.getId(), true);

				try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
					p = p.reload();
				}

				t.addTransactional(TransactionalTriggerEvent.deleteIntoWastebin(getFolder().getNode(), p));
			}

			// if the page is a localized copy, it was hiding other pages (which are now "created")
			if (!isMaster()) {
				unhideFormerHiddenObjects(Page.TYPE_PAGE, getId(), getChannel(), getChannelSet());
			}
		}

		@Override
		public void restore() throws NodeException {
			if (!isDeleted()) {
				return;
			}

			// we need to restore the folder of this page as well (if in wastebin)
			getFolder().restore();

			// if this is a localized copy, we need to restore its master first
			// (if the master is in the wastebin too)
			if (!isMaster()) {
				getMaster().restore();
			}

			// restore the object
			Transaction t = TransactionManager.getCurrentTransaction();
			DBUtils.executeUpdate(UPDATE_PAGE_DELETEFLAG, new Object[] {0, 0, getId()});
			deleted = 0;
			deletedBy = 0;
			ActionLogger.logCmd(ActionLogger.WASTEBINRESTORE, Page.TYPE_PAGE, getId(), null, "Page.restore()");
			channelSet = null;
			t.dirtObjectCache(Page.class, getId(), true);
			t.addTransactional(new TransactionalTriggerEvent(this, null, Events.CREATE));

			// make the name unique
			FactoryPage editablePage = t.getObject(this, true);
			DisinheritUtils.makeUniqueDisinheritable(editablePage, SeparatorType.blank, Page.MAX_NAME_LENGTH);
			makePageFilenameUnique(editablePage);
			editablePage.save();
			editablePage.unlock();
		}

		private PageFactory getPageFactory() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return (PageFactory) t.getObjectFactory(Page.class);
		}

		public Template getTemplate() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Template template = (Template) t.getObject(Template.class, templateId);

			// If the page object is versioned, the templateId may refer to a template that doesn't
			// exist anymore. If this is the case, we try to get the current template instead.
			if (null == template && !super.getObjectInfo().isCurrentVersion()) {
				template = t.getCurrentObject(Page.class, getId()).getTemplate();
				logger.warn(
						"Versioned page {" + getId() + "," + super.getObjectInfo().getVersionTimestamp() + "} " + " refers to missing template {" + templateId + "}."
						+ " Using current template instead.");
			} else {
				// check for data consistency
				assertNodeObjectNotNull(template, templateId, "Template");
			}
			return template;
		}

		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			// use level2 cache
			Transaction t = TransactionManager.getCurrentTransaction();
			@SuppressWarnings("unchecked")
			Map<String, ObjectTag> objectTags = (Map<String, ObjectTag>) t.getFromLevel2Cache(this, OBJECTTAGS);

			if (objectTags == null) {
				objectTags = loadObjectTags();
				t.putIntoLevel2Cache(this, OBJECTTAGS, objectTags);
			}
			return objectTags;
		}

		public String getName() {
			return name;
		}

		@Override
		public String getNiceUrl() {
			return niceUrl;
		}

		public String getDescription() {
			return description;
		}

		public String getFilename() {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();
				boolean mc = t.getNodeConfig().getDefaultPreferences().getFeature("extension_from_template");

				if (mc) {
					return getFilenameWithTemplateExt();
				}
			} catch (TransactionException e) {
				logger.error("Unable to read config of multichannel feature from transaction");
			}

			return filename;
		}

		/**
		 * retrieve the filename as usual, but with it's extension
		 * generated from the template. So, if you've got a page with
		 * a filename like "Page.php" and you use a template with it's
		 * markup set to html, you will get "Page.html" instead
		 * @return String filename with extension from template
		 */
		public String getFilenameWithTemplateExt() {
			try {
				return filename.replaceFirst("\\.\\w*$", "") + "." + getTemplate().getMarkupLanguage().getExtension();
			} catch (NodeException e) {
				logger.error("Unable to append template markup language extension", e);
				return filename;
			}
		}

		public int getPriority() {
			return priority;
		}

		@Override
		public boolean isOnline() throws NodeException {
			return PageFactory.isOnline(this);
		}

		@Override
		public boolean isModified() throws NodeException {
			return pageModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#isPublishDelayed()
		 */
		public boolean isPublishDelayed() throws NodeException {
			return PageFactory.isPublishDelayed(this);
		}

		@Override
		public Integer getFolderId() throws NodeException {
			if (!super.getObjectInfo().isCurrentVersion() && new Integer(-1).equals(folderId)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Page currentPage = t.getCurrentObject(Page.class, getId());

				if (currentPage == null) {
					throw new NodeException("Data inconsistent: current version for page {" + getId()
							+ "} does not exist (channel ID {" + t.getChannelId() + "})!");
				}
				return currentPage.getFolderId();
			} else {
				return folderId;
			}
		}

		@Override
		public Folder getFolder() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = t.getObject(Folder.class, getFolderId());

			if (folder == null) {
				throw new NodeException("Data inconsistent: Folder {" + getFolderId() + "} of page {" + getId()
						+ "} does not exist (channel ID {" + t.getChannelId() + "})!");
			}
			return folder;
		}

		public Content getContent() throws NodeException, ReadOnlyException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// get the content from the transaction. When the page is fetched for editing, also get the content for editing.
			// when the page is a versioned one, also get the versioned content
			Content content = getObjectInfo().isEditable()
					? (Content) t.getObject(Content.class, contentId, getObjectInfo().isEditable())
					: (Content) t.getObject(Content.class, contentId, getObjectInfo().getVersionTimestamp());

			// check for data consistency
			assertNodeObjectNotNull(content, contentId, "Content");
			return content;
		}

		public SystemUser getCreator() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "SystemUser");
			return creator;
		}

		public SystemUser getEditor() throws NodeException {
			SystemUser editor = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(editor, editorId, "SystemUser");
			return editor;
		}

		public SystemUser getPublisher() throws NodeException {
			SystemUser publisher = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, publisherId);

			// check for data consistency (empty publisher is allowed, if the page is not yet published)
			assertNodeObjectNotNull(publisher, publisherId, "SystemUser", true);
			return publisher;
		}

		public List<Page> getLanguageVariants(boolean considerNodeLanguages) throws NodeException {
			return loadLanguageVariants(considerNodeLanguages);
		}

		public Page getLanguageVariant(String code) throws NodeException {
			return getLanguageVariant(code, null);
		}

		public Page getLanguageVariant(String code, Integer nodeId) throws NodeException {
			if (code == null) {
				return null;
			}

			List<Page> languages;
			if (nodeId != null) {
				languages = loadLanguageVariantsForNode(nodeId);
			} else {
				languages = loadLanguageVariants(true);
			}

			for (int i = 0; i < languages.size(); i++) {
				Page page = (Page) languages.get(i);
				final ContentLanguage lang = page.getLanguage();

				if (lang != null) {
					if (code.equals(lang.getCode())) {
						return page;
					}
				}

			}
			return null;
		}

		public ContentLanguage getLanguage() throws NodeException {
			ContentLanguage language = (ContentLanguage) TransactionManager.getCurrentTransaction().getObject(ContentLanguage.class, languageId);

			// check data consistency
			assertNodeObjectNotNull(language, languageId, "ContentLanguage", true);
			return language;
		}

		/**
		 * Retrieve language variants
		 * @param considerNodeLanguages if true, node contentgroup sortorder is
		 *        considered. If false we'll use the old (php) approach, which
		 *        will care sh*t 'bout activated node languages
		 * @return list of language variants (pages)
		 * @throws NodeException
		 */
		private List<Page> loadLanguageVariants(boolean considerNodeLanguages) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<Integer> ids = null;
			Node node = getChannel();
			if (node == null) {
				node = getNode();
			}

			Integer nodeId = node.getId();

			if (considerNodeLanguages) {
				// when the id's were not fetched, or the node languages changed, get the language variants now
				synchronized (this) {
					if (nodeSpecificLanguageVariantIds.get(nodeId) == null || !StringUtils.isEqual(nodeLanguagesMD5.get(nodeId), node.getLanguagesMD5())) {
						nodeSpecificLanguageVariantIds.put(nodeId, loadLanguageVariantsIds(true, nodeId));
						nodeLanguagesMD5.put(nodeId, node.getLanguagesMD5());
					}
				}
				ids = nodeSpecificLanguageVariantIds.get(nodeId);
			} else {
				// when the id's were not fetched, do this now
				synchronized (this) {
					if (languageVariantIds.get(nodeId) == null) {
						languageVariantIds.put(nodeId, loadLanguageVariantsIds(false, nodeId));
					}
				}
				ids = languageVariantIds.get(nodeId);
			}

			return t.getObjects(Page.class, ids);
		}

		/**
		 * Retrieve language variants for a specific node/channel;
		 * currently, node languages are always considered, and there will be a call to
		 * loadLanguageVariantsIds in every case (less efficient but will consider new language variants)
		 * @param nodeId node for which languages variants shall be loaded. if null, use the page's node
		 * @return list of language variants (pages)
		 * @throws NodeException
		 */
		private List<Page> loadLanguageVariantsForNode(Integer nodeId) throws NodeException {
			if (nodeId == null) {
				nodeId = getFolder().getNode().getId();
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			List<Integer> ids = null;
			Node node = t.getObject(Node.class, nodeId);

			synchronized (this) {
				nodeSpecificLanguageVariantIds.put(nodeId, loadLanguageVariantsIds(true, nodeId));
				nodeLanguagesMD5.put(nodeId, node.getLanguagesMD5());
			}
			ids = nodeSpecificLanguageVariantIds.get(nodeId);

			return t.getObjects(Page.class, ids);
		}

		/**
		 * retrieve language variants page ids
		 * @param considerNodeLanguages if true, node contentgroup sortorder is
		 *        considered. If false we'll use the old (php) approach, which
		 *        will care sh*t 'bout activated node languages
		 * @param nodeId id of the node (channel) for which language variants shall be fetched, if multichannelling is used
		 * @return list of ids of the language variants
		 * @throws NodeException
		 */
		protected List<Integer> loadLanguageVariantsIds(boolean considerNodeLanguages, Integer nodeId) throws NodeException {

			if (ObjectTransformer.getInt(contentSetId, 0) <= 0) {
				return Collections.emptyList();
			}

			if (nodeId == null) {
				nodeId = getFolder().getNode().getId();
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;
			List<Integer> ids = new ArrayList<Integer>();
			boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);

			try {
				StringBuffer sql = new StringBuffer("SELECT page.id ");

				if (multichannelling) {
					sql.append(", page.channel_id, page.channelset_id, page.mc_exclude, page_disinherit.channel_id disinherited_node ");
				}

				if (considerNodeLanguages) {
					sql.append("FROM page ")
							.append("JOIN node_contentgroup on  node_contentgroup.contentgroup_id = page.contentgroup_id ")
							.append("JOIN folder ON page.folder_id = folder.id AND folder.node_id = node_contentgroup.node_id ");
					if (multichannelling) {
						sql.append("LEFT JOIN page_disinherit on page_disinherit.page_id = page.id ");
					}
					sql.append("WHERE page.contentset_id = ? ")
					.append("ORDER BY node_contentgroup.sortorder ASC");
				} else {
					sql.append("FROM page ");
					if (multichannelling) {
						sql.append("LEFT JOIN page_disinherit on page_disinherit.page_id = page.id ");
					}
					sql.append("WHERE contentset_id = ?");
				}
				stmt = t.prepareStatement(sql.toString());
				stmt.setInt(1, contentSetId.intValue());

				rs = stmt.executeQuery();

				if (multichannelling) {
					// do the multichannelling fallback here

					MultiChannellingFallbackList fallbackList = new MultiChannellingFallbackList(t.getObject(Node.class, nodeId));
					final HashMap<Integer, Integer> preservedOrder = new HashMap<Integer,Integer>();


					while (rs.next()) {
						preservedOrder.put(rs.getInt("id"),rs.getRow());
						fallbackList.addObject(rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), rs.getBoolean("mc_exclude"), rs.getInt("disinherited_node"));
					}
					ids = fallbackList.getObjectIds();

					if (considerNodeLanguages) {
						// Multichanneling fallback does not preserve the order of its objects, so we
						// will look up their order in a HashMap derived from the original result set.
						Collections.sort(ids, new Comparator<Object>() {
							public int compare(Object o1, Object o2) {
								int idx1 = preservedOrder.containsKey(o1) ? preservedOrder.get(o1) : -1;
								int idx2 = preservedOrder.containsKey(o2) ? preservedOrder.get(o2) : -1;
								if (idx1 < idx2) {
									return -1;
								} else if (idx1 > idx2) {
									return 1;
								} else {
									return 0;
								}
							}
						});
					}
				} else {
					// just collect the ids
					while (rs.next()) {
						ids.add(new Integer(rs.getInt("id")));
					}
				}

			} catch (SQLException e) {
				throw new NodeException("Could not load contentobjects.", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}

			return ids;
		}

		private synchronized void loadObjectTagIds() throws NodeException {
			if (objectTagIds == null) {
				if (!isEmptyId(getId())) {
					objectTagIds = DBUtils.select("SELECT t.id as id FROM objtag t"
								+ " LEFT JOIN construct c ON c.id = t.construct_id"
								+ " WHERE t.obj_id = ? AND t.obj_type = ? AND c.id IS NOT NULL", ps -> {
									ps.setInt(1, getId().intValue());
									ps.setInt(2, Page.TYPE_PAGE);
								}, DBUtils.IDLIST);
				} else {
					objectTagIds = new ArrayList<>();
				}
			}

		}

		private Map<String, ObjectTag> loadObjectTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			Map<String, ObjectTag> objectTags = new HashMap<String, ObjectTag>();

			loadObjectTagIds();

			List<ObjectTag> tags;

			tags = t.getObjects(ObjectTag.class, objectTagIds, getObjectInfo().isEditable());
			for (Iterator<?> it = tags.iterator(); it.hasNext();) {
				ObjectTag tag = (ObjectTag) it.next();

				String name = tag.getName();

				if (name.startsWith("object.")) {
					name = name.substring(7);
				}

				objectTags.put(name, tag);
			}

			// when the page is editable, get all objecttags which are assigned to the page's node
			if (getObjectInfo().isEditable() && getFolderId() != null) {
				addMissingObjectTags(objectTags);

				// migrate object tags to new constructs, if they were changed
				for (ObjectTag tag : objectTags.values()) {
					tag.migrateToDefinedConstruct();
				}
			}

			return objectTags;
		}

		/**
		 * Adds object tag defintions to the given map objecttags
		 * @param objectTags
		 * @throws NodeException
		 */
		protected void addMissingObjectTags(Map<String, ObjectTag> objectTags) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement pst = null;
			ResultSet res = null;
			List<Integer> objTagDefIds = new Vector<Integer>();

			try {
				// get all objtag definitions for type "page" which are assigned to the page's node (or not restricted to nodes)
				pst = t.prepareStatement(
						"SELECT DISTINCT objtag.id id FROM objtag"
						+ " LEFT JOIN objprop ON objtag.id = objprop.objtag_id"
						+ " LEFT JOIN objprop_node ON objprop.id = objprop_node.objprop_id"
						+ " LEFT JOIN construct c ON c.id = objtag.construct_id"
						+ " WHERE objtag.obj_id = 0 AND objtag.obj_type = ?"
						+ " AND (objprop_node.node_id = ? OR objprop_node.node_id IS NULL) AND c.id IS NOT NULL");
				pst.setObject(1, Page.TYPE_PAGE_INTEGER);
				// Get Master Node in case the node is a Channel
				Node masterNode;
				try (NoMcTrx nmc = new NoMcTrx()) {
					masterNode = getFolder().getNode().getMaster();
				}
				pst.setObject(2, masterNode.getId());

				res = pst.executeQuery();
				// collect the id's
				while (res.next()) {
					objTagDefIds.add(getInteger(res.getObject("id"), null));
				}
			} catch (SQLException e) {
				throw new NodeException("Error while getting editable objtags", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}

			if (objTagDefIds.size() > 0) {
				// get the objtag (definition)
				List<ObjectTag> objTagDefs = t.getObjects(ObjectTag.class, objTagDefIds);

				for (Iterator<ObjectTag> i = objTagDefs.iterator(); i.hasNext();) {
					ObjectTag def = (ObjectTag) i.next();
					// get the name (without object. prefix)
					String defName = def.getName();

					if (defName.startsWith("object.")) {
						defName = defName.substring(7);
					}

					// if no objtag of that name exists for the page,
					// generate a copy and add it to the map of objecttags
					if (!objectTags.containsKey(defName)) {
						ObjectTag newObjectTag = (ObjectTag) def.copy();

						newObjectTag.setNodeObject(this);
						newObjectTag.setEnabled(false);
						objectTags.put(defName, newObjectTag);
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			String additional = "";

			try {
				Folder folder = getFolder();
				Node node = folder.getNode();

				additional = "," + node.toString();
			} catch (Exception e) {
				// Ignore for now ..
				if (logger.isDebugEnabled()) {
					logger.debug("Ignored exception " + e.getLocalizedMessage());
				}
			}

			return "Page {name:" + getName() + ",pageid:" + getId() + additional + "}";
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public ContentNodeDate getCustomCDate() {
			return customCDate;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public ContentNodeDate getCustomEDate() {
			return customEDate;
		}

		/**
		 * Set the pdate to the given value. Also store the pdate as transaction attribute, so that
		 * subsequent calls to {@link #getPDate()} in the same transaction are guaranteed to return this value.
		 * @param timestamp timestamp of the new pdate
		 */
		protected void setPDate(int timestamp) {
			if (this.pDate.getIntTimestamp() != timestamp) {
				this.pDate = new ContentNodeDate(timestamp);
				try {
					Transaction t = TransactionManager.getCurrentTransaction();
					t.getAttributes().put(getPDateAttrName(), timestamp);
				} catch (TransactionException e) {
				}
			}
		}

		/**
		 * Get the pdate of the page. If a value is stored as transaction attribute, it will modify the current pdate.
		 * @return pdate
		 */
		public ContentNodeDate getPDate() {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();
				Integer timestamp = getInteger(t.getAttributes().get(getPDateAttrName()), null);
				if (timestamp != null && this.pDate.getIntTimestamp() != timestamp.intValue()) {
					this.pDate = new ContentNodeDate(timestamp);
				}
			} catch (TransactionException e) {
			}
			return pDate;
		}

		/**
		 * Get the transaction attribute name, that stores the pdate for this page
		 * @return attribute name
		 */
		protected String getPDateAttrName() {
			return PDATE_KEY + "|" + getId();
		}

		public Integer getLanguageId() {
			return languageId;
		}

		public String getLanguageVariantsPHPSerialized() {
			return PageFactory.getLanguageVariantsPHPSerialized(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// dirt the content cache
			t.dirtObjectCache(Content.class, contentId, false);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#getLastPublishedVersion()
		 */
		public int getLastPublishedVersion(String[] property) throws NodeException {
			if (!ObjectTransformer.isEmpty(property)) {
				for (String prop : property) {
					if (prop.startsWith(PDATE_PROPERTY_PREFIX)) {
						try {
							return Integer.parseInt(prop.substring(PDATE_PROPERTY_PREFIX.length()), 10);
						} catch (NumberFormatException e) {
							logger.error("Error while getting last pdate from " + prop + " for page " + getId(), e);
						}
					}
				}
			}
			logger.warn("Could not get last pdate for page " + getId() + " from property, falling back to reading from publish table.");
			final int[] lastPublishedVersion = new int[] { 0 };
			DBUtils.executeStatement("SELECT min(pdate) pdate FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt)
						throws SQLException {
					stmt.setInt(1, ObjectTransformer.getInt(getId(), 0)); // page_id = ?
					stmt.setInt(2, 1);	// active = ?
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
					if (rs.next()) {
						lastPublishedVersion[0] = rs.getInt("pdate");
					}
				}
			});
			return lastPublishedVersion[0];
		}

		/**
		 * Get the set of ids of contenttags, which were modified between the given timestamps
		 * @param firsttimestamp first versiontimestamp
		 * @param lasttimestamp last versiontimestamp
		 * @return set of ids of modified contenttags
		 * @throws NodeException
		 */
		public Set<Integer> getModifiedContenttags(int firsttimestamp, int lasttimestamp) throws NodeException {
			Set<Integer> contentTagIds = new HashSet<Integer>();

			TableVersion contentTagVersion = getContentTagTableVersion();

			List<Diff> contentTagDiff = contentTagVersion.getDiff(new Object[] { contentId}, firsttimestamp, lasttimestamp);

			for (Diff diff : contentTagDiff) {
				contentTagIds.add(diff.getId());
				}

			// get the diff for the value
			TableVersion valueVersion = getValueTableVersion();

			getContentTagDiff(valueVersion, "value", firsttimestamp, lasttimestamp, contentTagIds, null);

			// get the diff for ds
			TableVersion dsVersion = getDSTableVersion();

			getContentTagDiff(dsVersion, "ds", firsttimestamp, lasttimestamp, contentTagIds, null);

			// get the diff for ds_obj
			TableVersion dsObjVersion = getDSObjTableVersion();

			getContentTagDiff(dsObjVersion, "ds_obj", firsttimestamp, lasttimestamp, contentTagIds, null);

			// get the diff for datasource
			TableVersion datasourceVersion = getDatasourceTableVersion();
			getContentTagDiff(datasourceVersion, "datasource", firsttimestamp, lasttimestamp, contentTagIds,
					"SELECT distinct contenttag_id FROM value, datasource WHERE value.part_id IN (" + getDatasourcePartIds()
							+ ") AND value.value_ref = datasource.id AND datasource.id IN");

			// get the diff for datasource_value
			TableVersion datasourceValueVersion = getDatasourceValueTableVersion();
			getContentTagDiff(datasourceValueVersion, "datasource_value", firsttimestamp, lasttimestamp, contentTagIds,
					"SELECT distinct contenttag_id FROM value, datasource_value WHERE value.part_id IN (" + getDatasourcePartIds()
							+ ") AND value.value_ref = datasource_value.datasource_id AND datasource_value.id IN");

			return contentTagIds;
		}

		/**
		 * Helper method to fetching modified contenttags by diffing a sub table
		 * @param tableVersion table version of the sub table
		 * @param table name of the sub table
		 * @param firsttimestamp first timestamp for the diff
		 * @param lasttimestamp last timestamp for the diff
		 * @param contentTagIds list of contenttag ids (new found contenttag ids are added here)
		 * @param sql SQL statement for selecting the contenttag_id for the modified records. If null, the contenttag_id is supposed to be a field in the table itself (no joins needed)
		 *        If the SQL statement is given, it must select the contenttag_id and must end with "... id in" (list of ids will be added in the method)
		 * @return modified list of contentTagIds
		 * @throws NodeException
		 */
		private Set<Integer> getContentTagDiff(TableVersion tableVersion, String table,
				int firsttimestamp, int lasttimestamp, final Set<Integer> contentTagIds, String sql) throws NodeException {
			List<Diff> diffList = tableVersion.getDiff(new Object[] { contentId}, firsttimestamp, lasttimestamp);

			if (diffList.size() > 0) {
				// make sure to only add every id once
				final Set<Integer> ids = new HashSet<Integer>();

				for (Diff diff : diffList) {
						ids.add(diff.getId());
					}

				if (sql == null) {
					sql = "SELECT distinct contenttag_id FROM " + table + " WHERE id in";
				}

				DBUtils.executeStatement(sql + " (" + StringUtils.repeat("?", ids.size(), ",") + ")",
						new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int propCounter = 1;
					for (Integer id : ids) {
							stmt.setInt(propCounter++, id);
					}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							contentTagIds.add(rs.getInt("contenttag_id"));
						}
					}
				});
				}

			return contentTagIds;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#dirtPage()
		 */
		public boolean dirtPage(int channelId) throws NodeException {
			// dirt the object
			Collection<PublishQueue.Entry> entries = PublishQueue.dirtObject(this, Action.DEPENDENCY, channelId);
			ActionLogger.logCmd(ActionLogger.PAGEPUB, TransactionManager.getCurrentTransaction().getTType(Page.class), getId(), getFolderId(),
					"page dirted by JP");

			return !entries.isEmpty();
		}

		@Override
		public ContentNodeDate getTimePub() {
			return timePub;
		}

		@Override
		public NodeObjectVersion getTimePubVersion() throws NodeException {
			if (timePubVersion != null) {
				return loadPageVersion(timePubVersion);
			} else {
				return null;
			}
		}

		@Override
		public SystemUser getPubQueueUser() throws NodeException {
			if (pubQueueUserId != 0) {
				return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, pubQueueUserId);
			} else {
				return null;
			}
		}

		@Override
		public ContentNodeDate getTimePubQueue() {
			return timePubQueue;
		}

		@Override
		public NodeObjectVersion getTimePubVersionQueue() throws NodeException {
			if (timePubVersionQueue != null) {
				return loadPageVersion(timePubVersionQueue);
			} else {
				return null;
			}
		}

		/**
		 * Load the page version with given id
		 * @param id page version id
		 * @return page version instance or null, if not found
		 * @throws NodeException
		 */
		protected NodeObjectVersion loadPageVersion(int id) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return DBUtils.select("SELECT * FROM nodeversion WHERE id = ?", ps -> ps.setInt(1, id), rs -> {
				if (rs.next()) {
					return new NodeObjectVersion(rs.getInt("id"), rs.getString("nodeversion"), (SystemUser) t.getObject(SystemUser.class, rs.getInt("user_id")),
							new ContentNodeDate(rs.getInt("timestamp")), rs.isLast(), rs.getBoolean("published"));
				} else {
					return null;
				}
			});
		}

		@Override
		public ContentNodeDate getTimeOff() {
			return timeOff;
		}

		@Override
		public SystemUser getOffQueueUser() throws NodeException {
			if (offQueueUserId != 0) {
				return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, offQueueUserId);
			} else {
				return null;
			}
		}

		@Override
		public ContentNodeDate getTimeOffQueue() {
			return timeOffQueue;
		}

		@Override
		public void clearTimePub() throws NodeException {
			timePub = new ContentNodeDate(0);
			timePubVersion = null;
			DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ? WHERE id = ?", 0, null, getId());

			recalculateModifiedFlag();

			TransactionManager.getCurrentTransaction().dirtObjectCache(Page.class, getId());
		}

		@Override
		public void clearTimeOff() throws NodeException {
			timeOff = new ContentNodeDate(0);
			DBUtils.update("UPDATE page SET time_off = ? WHERE id = ?", 0, getId());
			TransactionManager.getCurrentTransaction().dirtObjectCache(Page.class, getId());
		}

		@Override
		public void clearQueue() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			pubQueueUserId = 0;
			offQueueUserId = 0;
			timePubQueue = new ContentNodeDate(0);
			timePubVersionQueue = null;
			timeOffQueue = new ContentNodeDate(0);
			DBUtils.update("UPDATE page SET pub_queue = ?, off_queue = ?, time_pub_queue = ?, time_pub_version_queue = ?, time_off_queue = ? WHERE id = ?", 0,
					0, 0, null, 0, getId());
			t.dirtObjectCache(Page.class, getId());

			// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
			t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
		}

		@Override
		public boolean handleTimemanagement() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean handled = false;

			int transactionTime = t.getUnixTimestamp();
			int publishAtTime = getTimePub().getIntTimestamp();
			NodeObjectVersion timePubVersion = getTimePubVersion();
			int offlineAtTime = getTimeOff().getIntTimestamp();

			boolean publishAtDue = publishAtTime > 0 && publishAtTime <= transactionTime && timePubVersion != null;
			boolean offlineAtDue = offlineAtTime > 0 && offlineAtTime <= transactionTime;

			if (publishAtDue && offlineAtDue) {
				// determine whether to publish at or to take offline (whichever comes later)
				if (publishAtTime > offlineAtTime) {
					publishVersion(timePubVersion);
				} else {
					takeOffline();
				}
				clearTimePub();
				clearTimeOff();
				handled = true;
			} else if (publishAtDue) {
				publishVersion(timePubVersion);
				clearTimePub();
				handled = true;
			} else if (offlineAtDue) {
				takeOffline();
				clearTimeOff();
				handled = true;
			}

			if (handled) {
				// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			}

			return handled;
		}

		/**
		 * Set the given page version as published
		 * @param version version to publish
		 * @throws NodeException
		 */
		protected void publishVersion(NodeObjectVersion version) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean wasOnline = isOnline();
			int oldPDate = getPDate().getIntTimestamp();
			int newPdate = t.getUnixTimestamp();

			DBUtils.update("UPDATE nodeversion SET published = ? WHERE id = ?", 1, version.getId());
			DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND id != ?",
					new Object[] { Page.TYPE_PAGE, getId(), version.getId() });

			DBUtils.update("UPDATE page SET online = ?, pdate = ?, publisher = ? WHERE id = ?", 1, newPdate,
					version.getEditor().getId(), getId());
			setPDate(newPdate);

			recalculateModifiedFlag();

			PublishData publishData = t.getPublishData();
			if (publishData != null) {
				publishData.setOnline(this, true);
			}
			t.putIntoLevel2Cache(this, ONLINE_CACHE_KEY, true);

			// trigger events
			String[] props = null;
			int eventMask = Events.EVENT_CN_PAGESTATUS;

			if (!wasOnline) {
				// page was offline before, so its "online" status changed (from off to on)
				props = new String[] { "online"};
				eventMask |= Events.UPDATE;
			} else {
				// set the old pdate as event property
				props = new String[] { PDATE_PROPERTY_PREFIX + Integer.toString(oldPDate, 10) };
			}

			// when the published status of the page changes, we dirt the property
			// "pages" of the folder
			if (!wasOnline) {
				if (DependencyManager.isDependencyTriggering()) {
					triggerEvent(new DependencyObject(getFolder()), new String[] { "pages"}, Events.UPDATE, 0, 0);
				} else {
					t.addTransactional(new TransactionalTriggerEvent(Folder.class, getFolderId(), new String[] { "pages"}, Events.UPDATE));
				}
			}

			// trigger an event for the changed status
			if (DependencyManager.isDependencyTriggering()) {
				triggerEvent(new DependencyObject(this), props, eventMask, 0, 0);
			} else {
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), props, eventMask));
			}

			// dirt the page cache
			t.dirtObjectCache(Page.class, getId());

			onPublish(this, wasOnline, t.getUserId());
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#getContentsetId()
		 */
		public Integer getContentsetId() {
			return contentSetId;
		}

		@Override
		public NodeObjectVersion[] loadVersions() throws NodeException {
			return PageFactory.loadPageVersions(ObjectTransformer.getInt(getId(), 0));
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setDescription(java.lang.String)
		 */
		public String setDescription(String description) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setDescription(java.lang.String)
		 */
		public String setFilename(String filename) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setLanguage(com.gentics.contentnode.object.ContentLanguage)
		 */
		public ContentLanguage setLanguage(ContentLanguage language) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setLanguageId(java.lang.Integer)
		 */
		public Integer setLanguageId(Integer languageId) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setName(java.lang.String)
		 */
		public String setName(String name) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setPriority(int)
		 */
		public int setPriority(int priority) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#restoreVersion(com.gentics.contentnode.object.PageVersion, boolean)
		 */
		public void restoreVersion(NodeObjectVersion toRestore, boolean pageTable) throws NodeException {
			failReadOnly();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public Page copy() throws NodeException {
			return new EditableFactoryPage(this, getFactory().getFactoryHandle(Page.class).createObjectInfo(Page.class, true), true);
		}

		/**
		 * Generates a new contentset id
		 * @param globalId global id
		 *
		 * @return Newly created contentSetId
		 * @throws NodeException
		 */
		protected Integer createContentSetId(GlobalId globalId) throws NodeException {
			List<Integer> contentSetId = DBUtils.executeInsert(INSERT_CONTENTSET_SQL, new Object[] { ObjectTransformer.getString(globalId, "") });
			if (contentSetId.size() == 1) {
				return contentSetId.get(0);
			} else {
				throw new NodeException("Error while inserting new contentset, could not get the insertion id.");
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#copyTo(com.gentics.contentnode.object.Folder, boolean, java.lang.Integer, java.lang.Integer)
		 */
		public PageCopyOpResult copyTo(Integer sourceChannelId, Folder targetFolder, boolean createCopy, Integer contentSetId, Integer targetChannelId) throws NodeException {
			final Transaction t = TransactionManager.getCurrentTransaction();
			try (ChannelTrx trx = new ChannelTrx()) {
				if (targetChannelId == null) {
					targetChannelId = 0;
				}
				if (targetFolder == null) {
					return PageCopyOpResult.fail("op.no_targetfolder");
				}
				targetFolder = targetFolder.getMaster();
				// Validate whether targetChannelId is valid
				if (targetChannelId != 0) {
					Node targetNode = t.getObject(Node.class, targetChannelId);
					if (targetNode == null) {
						throw new NodeException("Could not load node for targetChannelId {" + targetChannelId + "}.");
					}
					if (targetNode.isChannel()) {
						if (MultichannellingFactory.getChannelVariant(targetFolder, targetNode) == null) {
							return PageCopyOpResult.fail("op.folder_not_inherited_by_channel", targetFolder.getName(), ObjectTransformer.getString(targetNode.getId(), "?"));
						}
					} else {
						// targetchannel is a node
						targetChannelId = 0;
					}
				}

				final NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

				// Check for read permissions of the source page
				try (ChannelTrx trx1 = new ChannelTrx(sourceChannelId)) {
					if (!t.canView(this)) {
						return PageCopyOpResult.fail("page_copy.missing_view_perm_for_source_page", getLocation(this), this.getFolder().getName());
					}
				}

				// Check create permissions for new pages in folder
				try (ChannelTrx trx1 = new ChannelTrx(targetChannelId)) {
					if (!t.canCreate(targetFolder, Page.class, this.getLanguage())) {
						return PageCopyOpResult.fail("page_copy.missing_create_perms_for_target", I18NHelper.getLocation(targetFolder));
					}
				}

				// Map that stores information which language variation source page
				// id was mapped to which new page id
				Map<Integer, Integer> updateSyncInfoPages = new HashMap<Integer, Integer>();

				// this will be the language of the target page
				ContentLanguage targetLanguage = getLanguage();
				boolean doCopyThis = true;

				// copy all pages in contentset if no contentset is given..
				PageCopyOpResult result = new PageCopyOpResult();

				if (contentSetId == null || contentSetId == 0) {
					contentSetId = createContentSetId(null);
					if (prefs.isFeature(Feature.CONTENTGROUP3)) {
						// If we come here, we need to update the syncinfo (later)
						// in the following loop, all other language variants of the
						// page will be copied, so we keep track of the page copies.
						// this array will map the old page ids to the new page ids

						List<Page> pageVariants = new ArrayList<>(getLanguageVariants(true));

						// when copying into another node, we check whether all required languages exist in the target node
						if (!getOwningNode().equals(targetFolder.getOwningNode())) {
							// get the languages available in the target node
							List<ContentLanguage> targetNodeLanguages = targetFolder.getOwningNode().getLanguages();

							// remove the page variants with languages, that do not exist in the target node
							for (Iterator<Page> iterator = pageVariants.iterator(); iterator.hasNext();) {
								Page variant = iterator.next();
								ContentLanguage variantLanguage = variant.getLanguage();
								if (variantLanguage != null) {
									if (!targetNodeLanguages.contains(variantLanguage)) {
										iterator.remove();
									}
								}
							}

							// when the page to copy does not have a language, but the target node has languages, we let the copy have the first
							// language assigned to the node
							if (targetLanguage == null && !targetNodeLanguages.isEmpty()) {
								targetLanguage = targetNodeLanguages.get(0);
							}

							if (pageVariants.isEmpty()) {
								// we found no language, which exists in the target node, so we will copy only the selected page and
								// give it the first language of the target node. If the target node does not have languages, we give the copy no language
								if (targetNodeLanguages.isEmpty()) {
									targetLanguage = null;
								} else {
									targetLanguage = targetNodeLanguages.get(0);
								}
							} else if (!pageVariants.contains(this)) {
								// if the allowed page languages do not contain the selected page, we will not copy it
								doCopyThis = false;
							}
						}

						Collections.sort(pageVariants, new PageComparator("filename", "desc"));
						for (Page languageVariant : pageVariants) {
							// Don't invoke copy again for the initial copy call for
							// the page
							if (this.equals(languageVariant)) {
								continue;
							}

							PageCopyOpResult copyLanguageVariationOpResult = languageVariant.copyTo(sourceChannelId, targetFolder, createCopy, contentSetId, targetChannelId);

							// Return early with the failure when the copy call
							// was not successful
							if (!copyLanguageVariationOpResult.isOK()) {
								return copyLanguageVariationOpResult;
							}

							// For now we always assume that the copy call only
							// copies one page.
							if (copyLanguageVariationOpResult.getCopyInfos().size() != 1) {
								throw new NodeException("The recursive copy call should not create more than one copy. Something went wrong.");
							}
							PageCopyOpResultInfo info = copyLanguageVariationOpResult.getCopyInfos().get(0);
							if (logger.isDebugEnabled()) {
								logger.debug("Invoked copy call. Resulting Info: {" + info + "}");
							}
							Page createdPageCopy = info.getCreatedPageCopy();
							if (createdPageCopy == null) {
								throw new NodeException("The recursive copy call did finish successful but no copy was created.");
							}
							updateSyncInfoPages.put(getInteger(languageVariant.getId(), -1), getInteger(createdPageCopy.getId(), -1));
							// Merge the data (not the status)
							result.mergeData(copyLanguageVariationOpResult);

						}
					}
				}

				final Integer fTargetFolderId = getInteger(targetFolder.getId(),-1);
				String pageName = getName();

				// Check whether a page with the same name in the target folder
				// already exists.
				// TODO check whether getContent().getPages().getName() comparison
				// is faster. This would be nicer
				if (createCopy) {

					int i = 0;
					final AtomicBoolean pageNameCollisionFound = new AtomicBoolean(true);
					String determinedName = pageName;
					while (i == 0 || pageNameCollisionFound.get()) {
						String tmp = "";
						if (i != 0) {
							tmp = " " + (i + 1);
						}
						CNI18nString i18n = new CNI18nString("copy_of");
						i18n.addParameter(tmp);
						i18n.addParameter(pageName);
						determinedName = i18n.toString();
						final String sql = "SELECT count(*) c FROM page WHERE deleted = 0 AND name = ? AND folder_id = ?";
						final String currentName = determinedName;
						DBUtils.executeStatement(sql, new SQLExecutor() {
							@Override
							public void prepareStatement(PreparedStatement stmt) throws SQLException {
								stmt.setString(1, currentName);
								stmt.setInt(2, fTargetFolderId);
							}

							@Override
							public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
								while (rs.next()) {
									if (rs.getInt("c") == 0) {
										pageNameCollisionFound.set(false);
										return;
									}
								}
							}
						});
						i++;
					}
					pageName = determinedName;

				} else {

					final AtomicInteger foundPageId = new AtomicInteger(-1);
					final String currentPageName = pageName;
					final Integer fContentSetId = contentSetId;

					// Check for pages with given name in the target folder
					StringBuilder sqlBuilder = new StringBuilder();
					sqlBuilder.append("SELECT id FROM page WHERE deleted = 0 AND name = ? AND folder_id =  ? ");
					if (prefs.isFeature(Feature.CONTENTGROUP3)) {
						sqlBuilder.append(" AND contentset_id != ?");
					}
					DBUtils.executeStatement(sqlBuilder.toString(), new SQLExecutor() {

						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setString(1, currentPageName);
							stmt.setInt(2, fTargetFolderId);
							if (prefs.isFeature(Feature.CONTENTGROUP3)) {
								stmt.setInt(3, fContentSetId);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								foundPageId.set(rs.getInt(1));
								return;
							}
						}
					});
					// We found a conflict. Load the page and return a failure
					if (foundPageId.get() != -1) {
						Page foundPage = t.getObject(Page.class, foundPageId.get());
						if (foundPage != null) {
							return PageCopyOpResult.fail("page_copy.name_conflict_detected", I18NHelper.getLocation(this), I18NHelper.getLocation(targetFolder));
						}
					}
				}

				if (doCopyThis) {
					// Create the copy and modify the copy according to the given
					// parameters
					Page pageCopy = copy();

					// Reset the contentset id to set a new one
					pageCopy.resetContentsetId();
					pageCopy.setContentsetId(contentSetId);

					pageCopy.setName(pageName);
					if (targetLanguage == null) {
						pageCopy.setLanguageId(0);
					} else {
						pageCopy.setLanguage(targetLanguage);
					}
					pageCopy.setFolderId(targetFolder.getId());

					// Autogenerate channelset id when copying into master node folders
					pageCopy.setChannelInfo(targetChannelId, null);

					pageCopy.save(false);
					result.addPageCopyInfo(this, pageCopy, targetFolder, targetChannelId);

					if (prefs.isFeature(Feature.MULTICHANNELLING) && isMaster()) {
						ChannelTreeSegment originalSegment = new ChannelTreeSegment(this, true);
						ChannelTreeSegment newSegment = originalSegment.addRestrictions(targetFolder.isExcluded(), targetFolder.getDisinheritedChannels());
						pageCopy.changeMultichannellingRestrictions(newSegment.isExcluded(), newSegment.getRestrictions(), false);
						logger.debug("calling setDisinheritDefault() for pageCopy: " + (getMaster().isDisinheritDefault() || targetFolder.isDisinheritDefault()));
						pageCopy.setDisinheritDefault(
							getMaster().isDisinheritDefault() || targetFolder.isDisinheritDefault(),
							false);
					}

					// Generate new filename if needed
					makePageFilenameUnique(pageCopy);
					pageCopy.save();
					pageCopy.unlock();

					if (updateSyncInfoPages.size() > 0) {
						updateSyncInfoPages.put(getInteger(getId(), -1), getInteger(pageCopy.getId(), -1));
					}
				}

				// Check whether new language variants have been copied. In this
				// case the sync info has to be updated.
				// now we possibly need to update the syncinfo for the new created
				// pages (including all language variants)
				for (Map.Entry<Integer, Integer> mapping : updateSyncInfoPages.entrySet()) {
					final Integer oldPageId = mapping.getKey();
					final Integer newPageId = mapping.getValue();

					Page oldPage = t.getObject(Page.class, oldPageId);
					Integer oldSyncPageId = oldPage.getSyncPageId();
					if (oldSyncPageId == null) {
						continue;
					}
					final Integer newSyncPageId = updateSyncInfoPages.get(oldSyncPageId);
					if (newSyncPageId == null) {
						continue;
					}
					// The original page was sync'ed with another page
					Integer oldSyncTimestamp = 0;
					if (oldPage.getSyncTimestamp() != null) {
						oldSyncTimestamp = oldPage.getSyncTimestamp().getTimestamp();
					}

					final AtomicInteger newSyncTimestamp = new AtomicInteger(0);
					if (oldSyncTimestamp != null) {
						// The original page was synchronized with a
						// specific version, check whether it was the
						// latest version
						Page syncPageObject = t.getObject(Page.class, oldSyncPageId);
						NodeObjectVersion[] versions = syncPageObject.getVersions();
						if (versions != null && versions.length >= 1) {
							NodeObjectVersion lastversion = versions[versions.length - 1];
							Integer latestVersionTimestamp = lastversion.getDate().getTimestamp();
							if (latestVersionTimestamp == oldSyncTimestamp) {
								// The original page was in sync
								// with  the latest version of another
								// page, so the copy must also be.
								newSyncTimestamp.set(t.getUnixTimestamp());
							}
						}
					}

					// update sync_info for the copy
					String sql = "UPDATE page SET sync_page_id = ?, sync_timestamp = ? WHERE id = ?";
					DBUtils.executeStatement(sql, new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, newSyncPageId);
							stmt.setInt(2, newSyncTimestamp.get());
							stmt.setInt(3, newPageId);
						}
					});

				}

				ActionLogger.logCmd(ActionLogger.COPY, TYPE_PAGE, getId(), targetFolder.getId(), "page_copy_java: {" + "channel_id: " + targetChannelId + "}");
				t.dirtObjectCache(t.getClass(Folder.TYPE_FOLDER), targetFolder.getId());
				return result;
			}
		}

		@Override
		public void purgeOlderVersions(NodeObjectVersion oldestKeptVersion) throws NodeException {
			if (oldestKeptVersion == null) {
				throw new NodeException("Could not purge older versions, since no oldest version was given");
			}
			// TODO check whether purging is save (or an older version is the published one or is used somewhere)
			purgeOlderPageVersions(this, oldestKeptVersion.getDate().getIntTimestamp());
			// Finally  clear the page version since we need to clear the cache
			pageVersions = null;
		}

		/**
		 * Internal method to load the channelset for the object
		 * @return map of channels and their localized variant of this page
		 * @throws NodeException
		 */
		protected synchronized Map<Wastebin, Map<Integer, Integer>> loadChannelSet() throws NodeException {
			if (!isEmptyId(channelSetId) && MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
			}
			// check for incomplete channelsets
			if (channelSet != null && !channelSet.keySet().containsAll(Arrays.asList(Wastebin.INCLUDE, Wastebin.EXCLUDE, Wastebin.ONLY))) {
				channelSet = null;
			}
			if (channelSet == null) {
				channelSet = MultichannellingFactory.loadChannelset(getObjectInfo().getObjectClass(), channelSetId);
			}
			return channelSet;
		}

		/**
		 * Update the non versioned data from the current version
		 * @param fCurrentVersion current version
		 */
		protected synchronized void updateNonVersionedData(FactoryPage fCurrentVersion) {
			// update the folderId
			folderId = fCurrentVersion.folderId;
			// update the channel information (only if the current version is set)
			channelId = fCurrentVersion.channelId;
			if (fCurrentVersion.channelSetId > 0) {
				channelSetId = fCurrentVersion.channelSetId;
			}
			channelSet = fCurrentVersion.channelSet;
			// update time management
			timePub = fCurrentVersion.timePub;
			timePubVersion = fCurrentVersion.timePubVersion;
			timePubQueue = fCurrentVersion.timePubQueue;
			timePubVersionQueue = fCurrentVersion.timePubVersionQueue;
			timeOff = fCurrentVersion.timeOff;
			timeOffQueue = fCurrentVersion.timeOffQueue;
			pDate = fCurrentVersion.pDate;

			excluded = fCurrentVersion.excluded;
			disinheritDefault = fCurrentVersion.disinheritDefault;
			if (fCurrentVersion.disinheritedChannelIds != null) {
				disinheritedChannelIds = new HashSet<Integer>(fCurrentVersion.disinheritedChannelIds);
			} else {
				disinheritedChannelIds = null;
			}

			deleted = fCurrentVersion.deleted;
			deletedBy = fCurrentVersion.deletedBy;
		}

		@Override
		public Map<Integer, Integer> getChannelSet() throws NodeException {
			Map<Wastebin, Map<Integer, Integer>> cSet = loadChannelSet();

			return new HashMap<>(cSet.get(TransactionManager.getCurrentTransaction().getWastebinFilter()));
		}

		@Override
		public Map<Integer, Integer> getHiddenPageIds() throws NodeException {
			Map<Wastebin, Map<Integer, Integer>> cSet = loadChannelSet();

			if (isMaster()) {
				return Collections.emptyMap();
			} else {
				Map<Integer, Integer> hiddenPages = new HashMap<Integer, Integer>(
						cSet.get(TransactionManager.getCurrentTransaction().getWastebinFilter()));
				hiddenPages.values().remove(getId());
				hiddenPages.keySet().retainAll(getMasterNodeIds());

				return hiddenPages;
			}
		}

		@Override
		public Map<Integer, Integer> getHidingPageIds() throws NodeException {
			Map<Wastebin, Map<Integer, Integer>> cSet = loadChannelSet();

			Map<Integer, Integer> hidingPages = new HashMap<Integer, Integer>(cSet.get(TransactionManager.getCurrentTransaction().getWastebinFilter()));
			hidingPages.values().remove(getId());
			hidingPages.keySet().removeAll(getMasterNodeIds());

			return hidingPages;
		}

		/**
		 * Get the node IDs of the master nodes of the page's node/channel
		 * @return set of master node IDs. This will always contain 0
		 * @throws NodeException
		 */
		protected Set<Integer> getMasterNodeIds() throws NodeException {
			Set<Integer> masterNodeIds = new HashSet<Integer>();
			masterNodeIds.add(0);
			Node channel = getChannel();
			if (channel != null) {
				for (Node master : channel.getMasterNodes()) {
					masterNodeIds.add(master.getId());
				}
			}
			return masterNodeIds;
		}

		@Override
		public Node getChannel() throws NodeException {
			if (!isEmptyId(channelId)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return (Node) t.getObject(Node.class, channelId, -1, false);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#getNodeOrChannel()
		 */
		public Node getOwningNode() throws NodeException {
			try (NoMcTrx noMc = new NoMcTrx()) {
				return getFolder().getOwningNode();
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId()
		 */
		public Integer getChannelSetId() throws NodeException {
			if (isEmptyId(channelSetId)) {
				throw new NodeException(this + " does not have a valid channelset_id");
			}
			return channelSetId;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#isInherited()
		 */
		public boolean isInherited() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				// multichannelling is not used, so the page cannot be inherited
				return false;
			}

			// determine the current channel id
			Node channel = t.getChannel();

			if (channel == null || !channel.isChannel()) {
				return false;
			}

			// the page is inherited if its channelid is different from the current channel
			return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(this.channelId, -1);
		}

		@Override
		public PublishWorkflow getWorkflow() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;
			ResultSet res = null;
			Integer workflowId = null;

			try {
				pst = t.prepareStatement("SELECT id FROM publishworkflow WHERE page_id = ?");
				pst.setObject(1, getId());
				res = pst.executeQuery();

				if (res.next()) {
					workflowId = res.getInt("id");
				}
			} catch (SQLException e) {
				throw new NodeException("Error while getting workflow for " + this, e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}

			if (workflowId != null) {
				return (PublishWorkflow) t.getObject(PublishWorkflow.class, workflowId, getObjectInfo().isEditable());
			} else {
				return null;
			}
		}

		@Override
		public Page createVariant(Folder folder) throws NodeException {
			return createVariant(folder, null);
		}

		@Override
		public Page createVariant(Folder folder, Node channel)  throws ReadOnlyException, NodeException {
			if (folder == null) {
				throw new NodeException("Cannot create page variant without folder");
			}
			int channelId = 0;
			if (channel != null && channel.isChannel()) {
				channelId = channel.getId();
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			// create a new page
			EditableFactoryPage variant = (EditableFactoryPage) t.createObject(Page.class);

			// set the contentid to be the same
			variant.setContentId(contentId);

			// Reset the editable content to be sure not invalid content is used
			variant.editableContent = null;

			// set the language from the original page
			variant.setLanguageId(getLanguageId());

			// set the folder id
			variant.setFolderId(folder.getId());

			// set the page name
			if (folder.equals(getFolder())) {
				// set "page variant of" as page name
				CNI18nString name = new CNI18nString("Variant{0} of page {1}");

				name.addParameters(new String[] { "", getName()});
				variant.setName(name.toString());
			} else {
				variant.setName(getName());
			}

			// set the template
			variant.setTemplateId(getTemplate().getId());

			// create variants for all languages, if the feature is activated
			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.PAGEVAR_ALL_CONTENTGROUPS)) {
				List<Page> languageVariants = getLanguageVariants(true);

				for (Page languageVariant : languageVariants) {
					if (!languageVariant.equals(this)) {
						EditableFactoryPage tmpVariant = (EditableFactoryPage) t.createObject(Page.class);

						tmpVariant.setContentId(languageVariant.getContent().getId());
						tmpVariant.setLanguageId(languageVariant.getLanguageId());
						tmpVariant.setFolderId(folder.getId());
						if (folder.equals(languageVariant.getFolder())) {
							// set "page variant of" as page name
							CNI18nString name = new CNI18nString("Variant{0} of page {1}");

							name.addParameters(new String[] { "", languageVariant.getName()});
							tmpVariant.setName(name.toString());
						} else {
							tmpVariant.setName(languageVariant.getName());
						}
						tmpVariant.setTemplateId(languageVariant.getTemplate().getId());
						tmpVariant.setChannelInfo(channelId, tmpVariant.getChannelSetId());
						variant.addLanguageVariant(tmpVariant);

						// clone all object properties, that are synchronized between variants
						if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
							for (Entry<String, ObjectTag> entry : languageVariant.getObjectTags().entrySet()) {
								if (entry.getValue().getDefinition().isSyncVariants()) {
									tmpVariant.getObjectTag(entry.getKey()).copyFrom(entry.getValue());
								}
							}
						}
					}
				}
			}
			variant.setChannelInfo(channelId, variant.getChannelSetId());

			// clone all object properties, that are synchronized between variants
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
				for (Entry<String, ObjectTag> entry : getObjectTags().entrySet()) {
					if (entry.getValue().getDefinition().isSyncVariants()) {
						variant.getObjectTag(entry.getKey()).copyFrom(entry.getValue());
					}
				}
			}

			return variant;
		}

		@Override
		public Page getSynchronizedWith() throws NodeException {
			if (syncPageId == 0) {
				return null;
			} else {
				Transaction t = TransactionManager.getCurrentTransaction();
				// first get the current version of the page (to check whether the page still exists)
				Page syncPage = t.getObject(Page.class, syncPageId);

				if (syncPage == null) {
					return null;
				} else {
					// when the current version exists, we get the sync version
					return t.getObject(Page.class, syncPageId, syncTimestamp.getIntTimestamp());
				}
			}
		}

		@Override
		public boolean isMaster() throws NodeException {
			return master;
		}

		@Override
		public boolean isInSync() throws NodeException {
			Page syncPage = getSynchronizedWith();

			if (syncPage == null) {
				// page not synchronized with another -> return true (nothing to check, so the check succeeds)
				return true;
			} else {
				// page is synchronized with another -> check whether it is synchronized with the current version
				NodeObjectVersion pageVersion = syncPage.getVersion();

				return pageVersion != null && pageVersion.isCurrent();
			}
		}

		@Override
		public void synchronizeWithPage(Page page) throws NodeException {
			failReadOnly();
		}

		@Override
		public void synchronizeWithPageVersion(Page page, int versionTimestamp) throws NodeException {
			failReadOnly();
		}

		@Override
		public void clearVersions() {
			// if this is not the current version of the page, we clear the
			// pageversions from the current version, because page versions might
			// change over time and old versions of the page will not be dirted in
			// the cache
			try {
				if (!getObjectInfo().isCurrentVersion()) {
					Transaction t = TransactionManager.getCurrentTransaction();
					// hack: we want to get the current version, even if currently
					// publishing, so for getting the object here, we temporarily set
					// the rendertyp to preview here
					RenderType renderType = t.getRenderType();
					int oldEditMode = RenderType.EM_PREVIEW;

					if (renderType != null) {
						oldEditMode = renderType.getEditMode();
						renderType.setEditMode(RenderType.EM_PREVIEW);
					}
					Page currentVersion = (Page) t.getObject(Page.class, getId());

					if (renderType != null) {
						// don't forget to restore the original editmode
						renderType.setEditMode(oldEditMode);
					}
					// check if the current version exists
					if (currentVersion instanceof FactoryPage) {
						((FactoryPage) currentVersion).pageVersions = null;
					}
				}
			} catch (NodeException ignored) {
			}
			pageVersions = null;
		}

		@Override
		public boolean needsContenttagMigration(Template template, List<String> tagnames, boolean force) throws NodeException {
			for (ContentTag contentTag : getContent().getContentTags().values()) {
				if (tagnames != null && !tagnames.contains(contentTag.getName())) {
					continue;
				}

				if (mustDeleteContenttag(contentTag, template) || mustMigrateContenttag(contentTag, template, force)) {
					return true;
				}
			}
			for (TemplateTag templateTag : template.getTemplateTags().values()) {
				if (!templateTag.isPublic()) {
					continue;
				}
				if (tagnames != null && !tagnames.contains(templateTag.getName())) {
					continue;
				}
				if (getContent().getContentTag(templateTag.getName()) == null) {
					return true;
				}
			}

			return false;
		}

		@Override
		public Integer getSyncPageId() throws NodeException {
			return this.syncPageId;
		}

		@Override
		public void setSyncPageId(Integer pageId) throws NodeException {
			failReadOnly();
		}

		@Override
		public ContentNodeDate getSyncTimestamp() {
			return this.syncTimestamp;
		}

		@Override
		public void setSyncTimestamp(ContentNodeDate date) throws NodeException {
			failReadOnly();
		}

		@Override
		public boolean isExcluded() {
			return excluded;
		}

		@Override
		public void setExcluded(boolean value) throws ReadOnlyException {
			this.excluded = value;
		}

		/**
		 * Indicates whether this page should be disinherited by default
		 * in newly created channels.
		 *
		 * @return <code>true</code> if this page should be disinherited in
		 *		new channels, <code>false</code>otherwise.
		 * @throws NodeException On internal errors
		 */
		@Override
		public boolean isDisinheritDefault() throws NodeException {
			if (isMaster()) {
				return disinheritDefault;
			} else {
				return getMaster().isDisinheritDefault();
			}
		}

		/**
		 * Set whether this page should be disinherited by default in
		 * newly created channels.
		 *
		 * The flag will be set to the same value for all language
		 * variants of this page.
		 *
		 * @see DisinheritUtils#updateDisinheritDefault
		 *
		 * @param value Set to <code>true</code> if this page should be
		 *		disinheried by default in new channels.
		 * @param recursive Unused for pages.
		 */
		@Override
		public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException {
			if (!isMaster()) {
				return;
			}

			boolean valueChanged = DisinheritUtils.updateDisinheritDefault(this, value);

			if (!valueChanged) {
				return;
			}

			disinheritDefault = value;

			if (languageId != null && languageId != 0) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Integer channelId = getChannel() != null ? getChannel().getId() : null;
				List<Integer> variantIds = loadLanguageVariantsIds(false, channelId);
				List<Page> variants = t.getObjects(Page.class, variantIds);

				for (Page variant: variants) {
					if (!variant.isMaster()) {
						variant = variant.getMaster();
					}

					if (variant.equals(this)) {
						continue;
					}

					DisinheritUtils.updateDisinheritDefault((FactoryPage) variant, value);
					t.dirtObjectCache(Page.class, variant.getId());
				}
			}
		}

		@Override
		public Set<Node> getDisinheritedChannels() throws NodeException {
			return DisinheritUtils.loadDisinheritedChannelsInternal(this);
		}

		@Override
		public Set<Integer> getOriginalDisinheritedNodeIds() {
			return disinheritedChannelIds;
		}

		@Override
		public void setOriginalDisinheritedNodeIds(Set<Integer> nodeIds) {
			this.disinheritedChannelIds = nodeIds;
		}

		@Override
		public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException {
			internalChangeMultichannellingRestrictions(excluded, disinheritedNodes, true);
		}

		/**
		 * Implementation of changeMultichannellingRestrictions(boolean, Set<Node>) that takes into account that language variants must have the same multichannelling restrictions
		 * @param excluded whether the page should be excluded
		 * @param disinheritedNodes the disinherited channels for this object
		 * @param recurseDisinheritInfo whether to apply the change recursively to page variants
		 * @throws NodeException
		 */
		private void internalChangeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recurseDisinheritInfo) throws NodeException {

			boolean disinheritingInfoChanged = false;
			disinheritingInfoChanged = DisinheritUtils.updateDisinheritedNodeAssociations(this, excluded, disinheritedNodes, false);

			// When modifying a page that has language variants, the language
			// variants must be accessible in the same channels, so we make sure
			// that it is so.
			if (languageId != null && languageId != 0 && disinheritingInfoChanged && recurseDisinheritInfo) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Integer channelId = getChannel() != null ? getChannel().getId() : null;
				List<Integer> lVariantsIDs = loadLanguageVariantsIds(false, channelId);
				List<Page> lVariants = t.getObjects(Page.class, lVariantsIDs);
				for (Page variant : lVariants) {
					if (variant.equals(this)) {
						continue;
					}
					((FactoryPage)variant).internalChangeMultichannellingRestrictions(excluded, disinheritedNodes, false);
					t.dirtObjectCache(Page.class, variant.getId());
				}
			}
		}

		@Override
		public OpResult move(Folder target, int targetChannelId, boolean allLanguages) throws NodeException {
			List<Page> languageVariants = null;
			ContentLanguage targetLanguage = getLanguage();

			try (ChannelTrx trx = new ChannelTrx(getChannel())) {
				languageVariants = getLanguageVariants(false);
			}
			if (allLanguages) {
				// when moving to another node, check whether all languages are activated in the other node
				if (!getOwningNode().equals(target.getOwningNode())) {
					Set<ContentLanguage> pageLanguages = new HashSet<>();
					for (Page variant : languageVariants) {
						ContentLanguage variantLanguage = variant.getLanguage();
						if (variantLanguage != null) {
							pageLanguages.add(variantLanguage);
						}
					}
					List<ContentLanguage> targetNodeLanguages = target.getOwningNode().getLanguages();
					pageLanguages.removeAll(targetNodeLanguages);

					// when the page to move does not have a language, but the target node has languages, we let the page have the first
					// language assigned to the node
					if (targetLanguage == null && !targetNodeLanguages.isEmpty()) {
						targetLanguage = targetNodeLanguages.get(0);
					}

					// special case: the page to move has exactly one language and the target node does not have languages at all
					if (targetNodeLanguages.isEmpty() && pageLanguages.size() == 1) {
						targetLanguage = null;
					} else if (!pageLanguages.isEmpty()) {
						String missingLanguageNames = pageLanguages.stream().map(ContentLanguage::getName).sorted().collect(Collectors.joining(", "));
						if (pageLanguages.size() == 1) {
							return PageCopyOpResult.fail("page.move.missing_language", missingLanguageNames);
						} else {
							return PageCopyOpResult.fail("page.move.missing_languages", missingLanguageNames);
						}
					}
				}

				for (Page langVariant : languageVariants) {
					if (langVariant.equals(this)) {
						continue;
					}
					OpResult result = moveObject(langVariant, target, targetChannelId, true);
					if (!result.isOK()) {
						return result;
					}
				}
			}
			// if moving only a single language variant, and other language
			// variants exist, we change the contentset_id of the moved page
			if (!allLanguages && !languageVariants.isEmpty()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				contentSetId = createContentSetId(null);
				DBUtils.executeUpdate("UPDATE page SET contentset_id = ? WHERE id = ?", new Object[] {contentSetId, getId()});

				for (Page page : languageVariants) {
					t.dirtObjectCache(Page.class, page.getId());
				}
			}

			// change the language of the page (if necessary)
			if (!Objects.equals(getLanguage(), targetLanguage)) {
				if (targetLanguage == null) {
					DBUtils.update("UPDATE page SET contentgroup_id = ? WHERE id = ?", 0, getId());
				} else {
					DBUtils.update("UPDATE page SET contentgroup_id = ? WHERE id = ?", targetLanguage.getId(), getId());
				}
				TransactionManager.getCurrentTransaction().dirtObjectCache(Page.class, getId());
			}
			return moveObject(this, target, targetChannelId, false);
		}

		@Override
		public boolean isDeleted() {
			return deleted > 0;
		}

		@Override
		public int getDeleted() {
			return deleted;
		}

		@Override
		public SystemUser getDeletedBy() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, deletedBy);
		}

		@Override
		public void takeOffline(int timestamp) throws ReadOnlyException, NodeException {
			if (timestamp == 0) {
				doTakeOffline();
			} else {
				// set the planned offline time and clear queue for planned offline time
				Transaction t = TransactionManager.getCurrentTransaction();
				DBUtils.update("UPDATE page SET time_off = ?, off_queue = ?, time_off_queue = ? WHERE id = ?", timestamp, 0, 0, getId());
				timeOff = new ContentNodeDate(timestamp);
				offQueueUserId = 0;
				timeOffQueue = new ContentNodeDate(0);
				t.dirtObjectCache(Page.class, getId());

				// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			}
		}

		@Override
		public void queuePublish(SystemUser user, int timestamp, NodeObjectVersion version) throws ReadOnlyException, NodeException {
			requireNonNull(user, "Cannot queue taking publishing page without user");
			Transaction t = TransactionManager.getCurrentTransaction();

			if (timestamp > 0 && version == null) {
				// create new major version (which is not yet the published version)
				createPageVersion(this, true, false, null);
				NodeObjectVersion[] versions = getVersions();
				if (versions.length > 0) {
					version = versions[0];
				}
			} else if (timestamp <= 0) {
				// create a new minor version (if necessary)
				createPageVersion(this, false, null);
			}
			DBUtils.update("UPDATE page SET pub_queue = ?, time_pub_queue = ?, time_pub_version_queue = ? WHERE id = ?", user.getId(), timestamp,
					version != null ? version.getId() : null, getId());

			pubQueueUserId = user.getId();
			timePubQueue = new ContentNodeDate(timestamp);
			timePubVersionQueue = version != null ? version.getId() : null;
			t.dirtObjectCache(Page.class, getId());

			// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
			t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));

			// log placing the page in the queue
			ActionLogger.logCmd(ActionLogger.PAGEQUEUE, TYPE_PAGE, getId(), getFolderId(), "");
		}

		@Override
		public void queueOffline(SystemUser user, int timestamp) throws ReadOnlyException, NodeException {
			requireNonNull(user, "Cannot queue taking offline page without user");
			Transaction t = TransactionManager.getCurrentTransaction();

			DBUtils.update("UPDATE page SET off_queue = ?, time_off_queue = ? WHERE id = ?", user.getId(), timestamp, getId());
			offQueueUserId = user.getId();
			timeOffQueue = new ContentNodeDate(timestamp);
			t.dirtObjectCache(Page.class, getId());

			// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
			t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));

			// log placing the page in the queue
			ActionLogger.logCmd(ActionLogger.PAGEQUEUE, TYPE_PAGE, getId(), getFolderId(), "");
		}

		/**
		 * Take the page offline immediately
		 * @throws NodeException
		 */
		protected void doTakeOffline() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// get an existing workflow
			PublishWorkflow workflow = getWorkflow();

			boolean onlineBefore = isOnline();

			// set offline, clear planned offline time and queue for offline
			DBUtils.update("UPDATE page SET online = ?, time_off = ?, off_queue = ?, time_off_queue = ? WHERE id = ?", 0, 0, 0, 0, getId());

			// if page was online before, check for invalid links now
			if (onlineBefore) {
				PageFactory.checkInvalidLinks(Arrays.asList(getId()), null, new CNI18nString("taken offline"));
			}

			ActionLogger.logCmd(ActionLogger.PAGEOFFLINE, Page.TYPE_PAGE, getId(), null, "Page.offline()");

			// inform users watching the page in a workflow
			if (workflow != null) {
				// // get all watching users
				// List<SystemUser> watchingUsers = new Vector<SystemUser>();
				// List<PublishWorkflowStep> steps = workflow.getSteps();
				// for (PublishWorkflowStep step : steps) {
				// SystemUser creator = step.getCreator();
				// if (!watchingUsers.contains(creator)) {
				// watchingUsers.add(creator);
				// }
				// }
				//
				// // inform watching users
				// if (watchingUsers.size() > 0) {
				// MessageSender messageSender = new MessageSender();
				// t.addTransactional(messageSender);
				// CNI18nString i18n = new CNI18nString("The page <pageid {0}> has been published.");
				// i18n.addParameter(getId().toString());
				// String message = i18n.toString();
				// for (SystemUser watcher : watchingUsers) {
				// messageSender.sendMessage(new Message(t.getUserId(), ObjectTransformer
				// .getInt(watcher.getId(), 0), message));
				// }
				// }

				// delete the workflow
				workflow.delete();
			}

			// unlock the page
			unlock();

			// trigger event
			String[] props = null;
			int eventMask = Events.EVENT_CN_PAGESTATUS;

			if (onlineBefore) {
				// page was online before, so its "online" status changed (from on to off)
				props = new String[] { "online"};
				eventMask |= Events.UPDATE;
			}
			if (DependencyManager.isDependencyTriggering()) {
				triggerEvent(new DependencyObject(this), props, eventMask, 0, 0);
			} else {
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), props, eventMask));
			}

			// dirt the page cache
			t.dirtObjectCache(Page.class, getId());

			onTakeOffline(this, onlineBefore, t.getUserId());
		}

		/**
		 * Check whether the contenttag must be deleted, because it came from a templatetag
		 * that is no longer editable in page or no longer exists in the given template
		 * @param contentTag contenttag to check
		 * @param template template to check against
		 * @return true iff the contenttag must be deleted
		 * @throws NodeException
		 */
		protected boolean mustDeleteContenttag(ContentTag contentTag, Template template) throws NodeException {
			if (contentTag == null) {
				return false;
			}
			// only contenttags that came from the template must be checked
			if (!contentTag.comesFromTemplate()) {
				return false;
			}
			TemplateTag templateTag = template.getTemplateTag(contentTag.getName());
			// templatetag with same name no longer exists, so contenttag must also be deleted
			if (templateTag == null) {
				return true;
			}
			// if the templatetag is no longer editable in page, contenttag must be deleted
			return !templateTag.isPublic();
		}

		/**
		 * Check whether the contenttag must be migrated, because it came from a templatetag that uses a different construct now
		 * @param contentTag contenttag to check
		 * @param template template to check against
		 * @param force true to accept migration of incompatible tags
		 * @return true iff the contenttag must be migrated
		 * @throws NodeException
		 */
		protected boolean mustMigrateContenttag(ContentTag contentTag, Template template, boolean force) throws NodeException {
			if (contentTag == null) {
				return false;
			}

			TemplateTag templateTag = template.getTemplateTag(contentTag.getName());
			if (templateTag == null) {
				return false;
			}

			return templateTag.isPublic() && !contentTag.getConstruct().equals(templateTag.getConstruct())
					&& (force || contentTag.getConstruct().canConvertTo(templateTag.getConstruct()));
		}

		/**
		 * Recalculate the modified flag, depending on whether the last version is the published version (or the version planned to be published)
		 * and whether there are unversioned changes
		 * @return true iff the modified flag was changed
		 * @throws NodeException
		 */
		protected boolean recalculateModifiedFlag() throws NodeException {
			this.pageVersions = null;
			boolean newModified = pageModified;
			NodeObjectVersion timePubVersion = getTimePubVersion();
			NodeObjectVersion lastVersion = getLastVersion();
			NodeObjectVersion publishedPageVersion = getPublishedVersion();

			if (publishedPageVersion == null && timePubVersion == null) {
				// when there is no published version at all (i.e. page is "new"), the page is never considered modified
				newModified = false;
			} else {
				if (lastVersion == null) {
					// when no last version exists, the page is not modified
					newModified = false;
				} else {
					// if the last version is not the published one, the page is considered modified
					newModified = !lastVersion.isPublished() && !lastVersion.equals(timePubVersion);
				}

				// if not yet considered modified, we check whether there is a diff between the last version and current version
	 			if (!newModified && lastVersion != null) {
					TableVersion pageVersion = getPageTableVersion();

					if (!pageVersion.getDiff(new Object[] {getId()}, lastVersion.getDate().getIntTimestamp(), -1).isEmpty()) {
						newModified = true;
					} else {
						if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
							TableVersion niceUrlVersion = getAlternateUrlTableVersion();
							if (!niceUrlVersion.getDiff(new Object[] {getId()}, lastVersion.getDate().getIntTimestamp(), -1).isEmpty()) {
								newModified = true;
							}
						}

						if (!newModified) {
							List<TableVersion> contentIdBasedVersions = getContentIdBasedTableVersions(false);
							for (TableVersion version : contentIdBasedVersions) {
								if(!version.getDiff(new Object[] {getContent().getId()}, lastVersion.getDate().getIntTimestamp(), -1).isEmpty()) {
									newModified = true;
									break;
								}
							}
						}
					}
				}
			}

			if (pageModified != newModified) {
				DBUtils.update("UPDATE page SET modified = ? WHERE id = ?", newModified, getId());
				pageModified = newModified;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Load alternate URLs if not done so before
		 * @throws NodeException
		 */
		private synchronized void loadAlternateUrls() throws NodeException {
			if (alternateUrlsContainer == null) {
				alternateUrlsContainer = new PageAlternateUrlsContainer(this);
			}
		}

		@Override
		public Set<String> getAlternateUrls() throws NodeException {
			loadAlternateUrls();
			return alternateUrlsContainer.getAlternateUrls();
		}

		@Override
		public TableVersion getAlternateUrlTableVersion() throws NodeException {
			return PageFactory.getAlternateUrlTableVersion();
		}

		@Override
		public void folderInheritanceChanged() throws NodeException {
		}

		@Override
		public List<ExtensiblePublishableObjectService<Page>> getServices() {
			return StreamSupport.stream(pageFactoryServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	/**
	 * Class for implementation of an editable page
	 */
	private static class EditableFactoryPage extends FactoryPage {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 7697802183476265805L;

		/**
		 * editable copy of this page's content
		 */
		private EditableFactoryContent editableContent;

		/**
		 * editable copy of this page's objecttags
		 */
		private Map<String, ObjectTag> editableObjectTags;

		/**
		 * Flag to mark whether the page has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Flag to mark whether the channelset of this page was changed, or not
		 */
		private boolean channelSetChanged = false;

		/**
		 * Editable version of the publish workflow
		 */
		private PublishWorkflow editableWorkflow;

		/**
		 * List of added language variants (will be saved together with the page)
		 */
		private List<EditableFactoryPage> addedLanguageVariants;

		/**
		 * Global contentset id
		 */
		protected GlobalId globalContentsetId;

		/**
		 * Create a new empty instance of a page
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected EditableFactoryPage(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
			// also generate an empty content
			Transaction t = TransactionManager.getCurrentTransaction();

			editableContent = (EditableFactoryContent) t.createObject(Content.class);
			editableContent.setPage(this);
			description = "";
			languageId = 0;
		}

		/**
		 * Constructor for creating a copy of the given page
		 * @param page page
		 * @param info info about the copy
		 * @param asNewPage true when the editable copy shall represent a new
		 *        page, false if it shall be the editable version of the same
		 *        page
		 * @throws NodeException when an internal error occurred
		 * @throws ReadOnlyException when the page could not be fetched for
		 *         update
		 */
		protected EditableFactoryPage(FactoryPage page, NodeObjectInfo info, boolean asNewPage) throws ReadOnlyException, NodeException {
			// set some values differently, depending on whether this is a new page
			super(asNewPage ? null : page.getId(), info, page.name, page.niceUrl, page.description, page.filename, page.templateId, page.getFolderId(),
					asNewPage ? null : page.contentId, page.priority, page.contentSetId, page.languageId, asNewPage ? null : page.objectTagIds,
					page.cDate, page.customCDate, page.eDate, page.customEDate, page.pDate, page.creatorId, page.editorId, page.publisherId, page.timePub, asNewPage ? null : page.timePubVersion,
					page.pubQueueUserId, page.timePubQueue, asNewPage ? null : page.timePubVersionQueue, page.timeOff, page.offQueueUserId, page.timeOffQueue, asNewPage ? 0 : page.channelSetId, page.channelId,
					asNewPage ? 0 : page.syncPageId, asNewPage ? new ContentNodeDate(0) : page.syncTimestamp, asNewPage ? true : page.master, page.excluded,
					page.disinheritDefault, asNewPage ? 0 : page.deleted, asNewPage ? 0 : page.deletedBy, asNewPage ? true : page.pageModified, asNewPage ? -1 : page.getUdate(),
					asNewPage ? null : page.getGlobalId());

			if (asNewPage) {
				editableContent = (EditableFactoryContent) page.getContent().copy();

				// copy the objecttags
				Map<String, ObjectTag> originalObjectTags = page.getObjectTags();

				editableObjectTags = new HashMap<String, ObjectTag>(originalObjectTags.size());
				for (Iterator<Map.Entry<String, ObjectTag>> i = originalObjectTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, ObjectTag> entry = i.next();

					editableObjectTags.put(entry.getKey(), (ObjectTag) entry.getValue().copy());
				}
				addMissingObjectTags(editableObjectTags);

				// copy the alternate URLs (which are not stored in the page table)
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
					setAlternateUrls(page.getAlternateUrls());
				}

				modified = true;
			} else {
				// get any currently existing workflow for the page
				PublishWorkflow workflow = getWorkflow();

				if (workflow != null) {
					if (!workflow.allowsEditing()) {
						throw new ReadOnlyException("Workflow of the page does not allow to edit the page", "page.readonly.workflow");
					}
				}

				// check whether the content is currently locked
				boolean wasLocked = super.getContent().isLocked();

				try {
					// get the (editable) content, this will also lock the content, or
					// fail if the content is already locked by someone else
					Content content = getContent();
					if (!wasLocked) {
						// in case there were some changes on the page or the content, we create a new version
						// the editor of the version will be the last editor of the page (not the current user)
						// the timestamp will be the timestamp of the last change on the page or the content (not objecttags, since they are not versioned)
						int versionTimestamp = Math.max(page.getUdate(), content.getEffectiveUdate());
						createPageVersion(page, false, page.getEditor().getId(), versionTimestamp);
					}
				} finally {}
			}
		}

		/**
		 * Set the content id for the page
		 * @param contentId content id
		 */
		public void setContentId(Integer contentId) throws ReadOnlyException {
			if (this.contentId == null) {
				this.contentId = contentId;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryPage#getContent()
		 */
		public Content getContent() throws NodeException, ReadOnlyException {
			// we need to store the editable copy of the content here, since
			// otherwise the modifications made in the content would be lost
			if (editableContent == null) {
				editableContent = (EditableFactoryContent) super.getContent();
			}

			editableContent.setPage(this);
			return editableContent;
		}

		@Override
		public void setContent(Content content) throws ReadOnlyException,
					NodeException {
			if (!(content instanceof EditableFactoryContent)) {
				throw new NodeException("Cannot set a non-editable content to an editable page");
			}
			// get the (editable) content of the page
			getContent();

			if (!isEmptyId(editableContent.getId()) && (ObjectTransformer.getInt(editableContent.getId(), 0) != ObjectTransformer.getInt(content.getId(), 0))) {
				throw new NodeException("Cannot set page content for " + this + ": content already set to " + editableContent);
			}
			this.editableContent = (EditableFactoryContent) content;
			setContentId(this.editableContent.getId());
		}

		@Override
		public PublishWorkflow getWorkflow() throws NodeException {
			if (editableWorkflow == null) {
				editableWorkflow = super.getWorkflow();
			}
			return editableWorkflow;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryPage#getObjectTags()
		 */
		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			if (editableObjectTags == null) {
				editableObjectTags = super.getObjectTags();
			}

			return editableObjectTags;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setName(java.lang.String)
		 */
		public String setName(String name) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.name, name)) {
				String oldName = this.name;

				this.modified = true;
				this.name = name;
				return oldName;
			} else {
				return this.name;
			}
		}

		@Override
		public void setNiceUrl(String niceUrl) throws NodeException {
			assertEditable();
			niceUrl = AlternateUrlsContainer.NORMALIZER.apply(niceUrl);

			if (!StringUtils.isEqual(this.niceUrl, niceUrl)) {
				this.niceUrl = niceUrl;
				this.modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setDescription(java.lang.String)
		 */
		public String setDescription(String description) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.description, description)) {
				String oldDescription = this.description;

				this.modified = true;
				this.description = description;
				return oldDescription;
			} else {
				return this.description;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setFilename(java.lang.String)
		 */
		public String setFilename(String filename) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.filename, filename)) {
				String oldFilename = this.filename;

				this.modified = true;
				this.filename = filename;
				return oldFilename;
			} else {
				return this.filename;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setPriority(int)
		 */
		public int setPriority(int priority) throws ReadOnlyException {
			assertEditable();
			if (priority <= 0) {
				priority = 1;
			}
			if (this.priority != priority) {
				int oldPriority = this.priority;

				this.modified = true;
				this.priority = priority;
				return oldPriority;
			} else {
				return this.priority;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setLanguage(com.gentics.contentnode.object.ContentLanguage)
		 */
		public ContentLanguage setLanguage(ContentLanguage language) throws ReadOnlyException {
			assertEditable();
			ContentLanguage oldLanguage = null;

			try {
				oldLanguage = getLanguage();
			} catch (NodeException e) {}

			if (language == null && oldLanguage != null) {
				modified = true;
				languageId = 0;
			} else if (language != null && !language.equals(oldLanguage)) {
				modified = true;
				languageId = language.getId();
			}

			return oldLanguage;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setLanguageId(java.lang.Object)
		 */
		public Integer setLanguageId(Integer languageId) throws ReadOnlyException {
			assertEditable();
			Integer oldLanguageId = this.languageId;

			if (ObjectTransformer.getInt(this.languageId, 0) != ObjectTransformer.getInt(languageId, 0)) {
				modified = true;
				this.languageId = languageId;
			}

			return oldLanguageId;
		}

		/**
		 * Synchronize the contenttags with the current template. This will do
		 * the following:
		 * <ol>
		 * <li>Get all the (current) contenttags of the content</li>
		 * <li>Get all the changeable templatetags of the template</li>
		 * <li>If the associated contenttag for a changeable templatetag is not
		 * found, generate it. It will be filled with the default value, filled
		 * in the template</li>
		 * <li>If the associated contenttag for a changeable templatetag already
		 * exists, generate missing values</li>
		 * </ol>
		 * Currently, there is no check, whether existing contenttags are
		 * compatible with their "master" templatetag. Also no "migration" is
		 * done, if the contenttag is not compatible.
		 *
		 * @return true when contenttags were changed (in the object) and need
		 *         to be saved, false if not
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected boolean synchronizeContentTagsWithTemplate() throws ReadOnlyException, NodeException {
			Template template = getTemplate();
			// get the contenttags
			Map<String, ContentTag> contentTags = Collections.emptyMap();

			if (!isEmptyId(contentId)) {
				contentTags = getContent().getContentTags();
			}
			boolean modifiedContentTags = false;

			// generate contenttags out of the templatetags of the new template
			Map<String, TemplateTag> templateTags = template.getTemplateTags();

			for (Iterator<TemplateTag> i = templateTags.values().iterator(); i.hasNext();) {
				TemplateTag tTag = i.next();

				if (tTag.isPublic()) {
					// we found a template tag, which is editable, so check the contenttag
					ContentTag cTag = contentTags.get(tTag.getName());

					if (cTag == null) {
						cTag = editableContent.addContentTag(tTag);
						modifiedContentTags = true;

					} else {
						// get the values (which will add missing values and set the tag to be modified if necessary)
						if (!modifiedContentTags) {
							for (Value v : cTag.getValues()) {
								if (isEmptyId(v.getId())) {
								modifiedContentTags = true;
									break;
							}
						}
						}
						// TODO check whether cTag is compatible with tTag, if not... migrate
					}
				}
			}

			return modifiedContentTags;
		}

		/**
		 * Check whether the page's content or any of the object tags contains new values (which were created because the construct contains new editable values)
		 * @return true iff new values were found
		 * @throws NodeException
		 */
		protected boolean containsNewValues() throws NodeException {
			if (isEmptyId(contentId)) {
				return false;
			}
			// check contenttags
			for (ContentTag contentTag : getContent().getContentTags().values()) {
				for (Value value : contentTag.getValues()) {
					if (isEmptyId(value.getId())) {
						return true;
					}
				}
			}
			// check objecttags
			for (ObjectTag objectTag : getObjectTags().values()) {
				for (Value value : objectTag.getValues()) {
					if (isEmptyId(value.getId())) {
						return true;
					}
				}
			}

			// no new values found
			return false;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setSyncPageId(java.lang.Integer)
		 */
		public void setSyncPageId(Integer pageId) {
			this.syncPageId = pageId;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryPage#
		 * setSyncTimestamp(com.gentics.contentnode.etc.ContentNodeDate)
		 */
		public void setSyncTimestamp(ContentNodeDate date) throws NodeException {
			this.syncTimestamp = date;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setTemplateId(java.lang.Integer, boolean)
		 */
		public Integer setTemplateId(Integer templateId, boolean syncTags) throws ReadOnlyException, NodeException {
			if (ObjectTransformer.getInt(this.templateId, 0) != ObjectTransformer.getInt(templateId, 0)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Template newTemplate = t.getObject(Template.class, templateId);

				if (newTemplate == null) {
					throw new NodeException("Cannot set template id {" + templateId + "} for {" + this + "}: Template does not exist");
				}

				// get the master template
				newTemplate = newTemplate.getMaster();

				Integer oldTemplateId = this.templateId;

				this.templateId = newTemplate.getId();
				modified = true;

				if (syncTags) {
					synchronizeContentTagsWithTemplate();
				}

				return oldTemplateId;
			} else {
				return this.templateId;
			}
		}

		@Override
		public void migrateContenttags(Template template, List<String> tagnames, boolean force) throws NodeException {
			super.migrateContenttags(template, tagnames, force);

			for (Iterator<ContentTag> i = getContent().getContentTags().values().iterator(); i.hasNext();) {
				ContentTag contentTag = i.next();
				if (tagnames != null && !tagnames.contains(contentTag.getName())) {
					continue;
				}
				if (mustDeleteContenttag(contentTag, template)) {
					i.remove();
				} else if (mustMigrateContenttag(contentTag, template, force)) {
					TemplateTag templateTag = template.getTemplateTag(contentTag.getName());
					contentTag.migrateToConstruct(templateTag.getConstruct());
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#setFolderId(java.lang.Object)
		 */
		public void setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
			// always set the folder id of the master folder
			Transaction t = TransactionManager.getCurrentTransaction();
			try (NoMcTrx nmt = new NoMcTrx()){
				Folder folder = t.getObject(Folder.class, folderId);
				folderId = folder.getMaster().getId();
			}

			if (ObjectTransformer.getInt(this.getFolderId(), 0) != ObjectTransformer.getInt(folderId, 0)) {
				Integer oldFolderId = this.getFolderId();

				this.folderId = folderId;
				modified = true;
			}
		}

		@Override
		public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
			// ignore this call, if the channelinfo is already set identical
			if (ObjectTransformer.getInt(this.channelId, 0) == ObjectTransformer.getInt(channelId, 0)
					&& this.channelSetId == ObjectTransformer.getInt(channelSetId, 0)) {
				return;
			}

			// check whether the page is new
			if (!isEmptyId(getId()) && (!allowChange || this.channelSetId != ObjectTransformer.getInt(channelSetId, 0))) {
				// the page is not new, so we must not set the channel
				// information
				throw new NodeException("Cannot change channel information for {" + this + "}, because the page is not new");
			}

			if (ObjectTransformer.getInt(channelId, 0) != 0) {
				// check whether channel exists
				Node channel = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId, -1, false);
				if (channel == null) {
					throw new NodeException("Error while setting channel information: channel {" + channelId + "} does not exist");
				}
				// if the channel exists, but is not channel, we set the channelId to 0 instead
				if (!channel.isChannel()) {
					channelId = 0;
				}
			}

			int iChannelId = ObjectTransformer.getInt(channelId, 0);
			int iChannelSetId = ObjectTransformer.getInt(channelSetId, 0);

			// check whether channelId was set to 0 and channelSetId not
			if (iChannelId == 0 && iChannelSetId != 0) {
				throw new NodeException(
						"Error while setting channel information: channelId was set to {" + channelId + "} and channelSetId was set to {" + channelSetId
						+ "}, which is not allowed (when creating master objects in non-channels, the channelSetId must be autogenerated)");
			}
			// set the data
			this.channelId = ObjectTransformer.getInt(channelId, 0);

			// set the channelset id
			if (iChannelSetId == 0) {
				this.channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			} else {
				this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			}
			channelSet = null;

			// set the "master" flag to false, because we are not yet sure,
			// whether this object is a master or not
			this.master = false;

			// now get the master object
			Page master = getMaster();

			if (master == this) {
				this.master = true;
			} else {
				this.master = false;
			}

			modified = true;
			channelSetChanged = true;
		}

		@Override
		public void modifyChannelId(Integer channelId) throws ReadOnlyException,
					NodeException {
			if (isEmptyId(getId())) {
				throw new NodeException("Cannot modify the channelId for a new page");
			}
			if (isEmptyId(this.channelId)) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, since the page does not belong to a channel");
			}
			if (!isMaster()) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, because it is no master");
			}

			// read the channelset
			Map<Integer, Integer> channelSet = getChannelSet();
			Integer oldChannelId = this.channelId;

			if (isEmptyId(channelId)) {
				this.channelId = 0;
				modified = true;
			} else {
				List<Node> masterNodes = getChannel().getMasterNodes();
				boolean foundMasterNode = false;

				for (Node node : masterNodes) {
					if (node.getId().equals(channelId)) {
						foundMasterNode = true;
						break;
					}
				}

				if (!foundMasterNode) {
					throw new NodeException(
							"Cannot modify the channelId for {" + this + "} to {" + channelId + "}, because this is no master channel of the page's channel");
				}

				this.channelId = ObjectTransformer.getInt(channelId, 0);
				modified = true;
			}

			// modify the channelset, since this object moved from one channel to another
			channelSet.remove(oldChannelId);
			channelSet.put(this.channelId, getId());
			channelSetChanged = true;
		}

		@Override
		public void setGlobalContentsetId(GlobalId globalId) throws ReadOnlyException, NodeException {
			if (this.globalContentsetId == null || !this.globalContentsetId.equals(globalId)) {
				this.globalContentsetId = globalId;
				if (this.globalContentsetId != null) {
					Integer localId = this.globalContentsetId.getLocalId(C.Tables.CONTENTSET);

					if (!isEmptyId(localId)) {
						if (ObjectTransformer.getInt(this.contentSetId, -1) != ObjectTransformer.getInt(localId, -1)) {
							this.contentSetId = localId;
							this.modified = true;
						}
					}
				}
			}
		}

		@Override
		public void resetContentsetId() throws ReadOnlyException, NodeException {
			this.contentSetId = null;
			this.modified = true;
		}

		@Override
		public void setContentsetId(Integer id) throws ReadOnlyException,
					NodeException {
			if (ObjectTransformer.getInt(this.contentSetId, -1) != ObjectTransformer.getInt(id, -1)) {
				if (!isEmptyId(this.contentSetId)) {
					throw new NodeException("Cannot set contentset id to " + id + ": already set to " + this.contentSetId);
				}
				this.contentSetId = id;
				this.modified = true;
			}
		}

		@Override
		public void setCustomCDate(int timestamp) {
			modified |= customCDate.getIntTimestamp() != timestamp;
			customCDate = new ContentNodeDate(timestamp);
		}

		@Override
		public void setCustomEDate(int timestamp) {
			modified |= customEDate.getIntTimestamp() != timestamp;
			customEDate = new ContentNodeDate(timestamp);
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException,
					NodeException {
			return save(true);
		}

		@Override
		public boolean save(boolean createVersion, boolean updateEditor) throws InsufficientPrivilegesException, NodeException {
			return save(createVersion, updateEditor, true);
		}

		/**
		 * Actual implementation of Page.save(boolean)
		 *
		 * @param createVersion
		 *            true if a page version shall be created, false if not.
		 * @param updateEditor true to update editor and edate, false to leave untouched
		 * @param recurseDisinheritInfo
		 *            if true and "excluded" or "disinheritedChannels" have
		 *            changed, these changes are propagated to this page's
		 *            language variants.
		 * @return whether any changes have been performed to this object
		 * @throws InsufficientPrivilegesException
		 * @throws NodeException
		 */
		private boolean save(boolean createVersion, boolean updateEditor, boolean recurseDisinheritInfo) throws InsufficientPrivilegesException, NodeException {
			// first check whether the page is editable
			assertEditable();
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = modified;
			boolean isNew = this.id == null;

			// save the content (and the whales ;-) )
			boolean contentModified = getContent().save();
			isModified |= contentModified;
			setContentId(getContent().getId());

			// now check whether the object has been modified
			if (modified || isNew) {
				// object is modified, so update it
				savePageObject(this, updateEditor);
				modified = false;
				pageModified = true;
			} else if (isModified) {
				if (updateEditor) {
					// set the editor data
					editorId = t.getUserId();
					eDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				DBUtils.update("UPDATE page SET editor = ?, edate = ?, modified = ? WHERE id = ?", editorId, eDate.getIntTimestamp(), 1, getId());
				pageModified = true;
			}

			// save all the objecttags and check which tags no longer exist (and need to be removed)
			Map<String, ObjectTag> tags = getObjectTags();
			List<Integer> tagIdsToRemove = new Vector<Integer>();

			if (objectTagIds != null) {
				tagIdsToRemove.addAll(objectTagIds);
			}
			for (Iterator<ObjectTag> i = tags.values().iterator(); i.hasNext();) {
				ObjectTag tag = i.next();

				tag.setNodeObject(this);
				isModified |= tag.save();

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// eventually remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<ObjectTag> tagsToRemove = t.getObjects(ObjectTag.class, tagIdsToRemove);

				for (Iterator<ObjectTag> i = tagsToRemove.iterator(); i.hasNext();) {
					ObjectTag tagToRemove = i.next();

					tagToRemove.delete();
				}
				isModified = true;
			}

			if (isModified) {
				// the page was modified, check whether it has a workflow
				PublishWorkflow workflow = getWorkflow();

				if (workflow != null) {
					PublishWorkflowStep currentStep = workflow.getCurrentStep();

					currentStep.setPageModified(true);
					currentStep.save();
				}

				// remove the page from the queue
				if (pubQueueUserId != 0) {
					DBUtils.update("UPDATE page SET pub_queue = 0 WHERE id = ?", getId());
					pubQueueUserId = 0;
				}
			}

			// also save added language variants
			if (!ObjectTransformer.isEmpty(addedLanguageVariants)) {
				for (EditableFactoryPage variant : addedLanguageVariants) {
					variant.contentSetId = contentSetId;
					variant.save();
				}

				addedLanguageVariants = null;
			}

			// generate a version, if requested
			if (createVersion) {
				createPageVersion(this, false, null);
				recalculateModifiedFlag();
			}

			// dirt the page cache
			t.dirtObjectCache(Page.class, getId());

			// if we added a new language variant, we also need to dirt the other language variants so that their list of language variants gets updated
			if (isNew) {
				List<Integer> languageVariantIds;
				if (getChannel() != null) {
					languageVariantIds = loadLanguageVariantsIds(false, getChannel().getId());
				} else {
					languageVariantIds = loadLanguageVariantsIds(false, null);
				}
				for (Integer languageVariantId : languageVariantIds) {
					t.dirtObjectCache(Page.class, languageVariantId);
				}
			}

			// if the channelset changed, we need to dirt all other pages of the channelset as well
			if (channelSetChanged || MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
				Map<Integer, Integer> locChannelSet = getChannelSet();

				// dirt caches for all pages in the channelset
				for (Map.Entry<Integer, Integer> channelSetEntry : locChannelSet.entrySet()) {
					t.dirtObjectCache(Page.class, channelSetEntry.getValue());
				}

				channelSetChanged = false;
			}

			if (isModified) {
				// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), null, Events.NOTIFY));

				onSave(this, isNew, contentModified, t.getUserId());
			}

			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryPage#restoreVersion(com.gentics.contentnode.object.PageVersion, boolean)
		 */
		public void restoreVersion(NodeObjectVersion toRestore, boolean pageTable) throws NodeException {
			// first check whether the page is editable
			assertEditable();
			Transaction t = TransactionManager.getCurrentTransaction();

			// restore the page version
			restorePageVersion(this, toRestore.getDate().getIntTimestamp(), toRestore.getNumber(), pageTable);

			// dirt the page cache
			t.dirtObjectCache(Page.class, getId());

			onSave(t.getObject(this), false, true, t.getUserId());
		}

		@Override
		public void restoreTagVersion(ContentTag tag, int versionTimestamp) throws NodeException {
			if (tag == null) {
				return;
			}
			assertEditable();

			List<TableVersion> contentIdBasedTableVersions = getContentIdBasedTableVersions(true);

			Object[] idParam = new Object[] {tag.getId()};
			for (TableVersion version : contentIdBasedTableVersions) {
				version.restoreVersion(idParam, versionTimestamp);
			}

			// dirt the tag cache
			TransactionManager.getCurrentTransaction().dirtObjectCache(ContentTag.class, tag.getId());
		}

		@Override
		public void publish(int timestamp, NodeObjectVersion version, boolean updatePublisher) throws ReadOnlyException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());

			// get an existing workflow
			PublishWorkflow workflow = getWorkflow();

			boolean onlineBefore = isOnline();

			// remember the old pdate
			int oldPDate = getPDate().getIntTimestamp();

			// publishAt or publish
			if (timestamp > 0) {
				// if no version was specified, we create a new major version and use that
				if (version == null) {
					// before creating the new version, we set the publisher, so that it is contained in the version
					// otherwise the page would be "modified"
					if (updatePublisher && user != null) {
						DBUtils.update("UPDATE page SET publisher = ? WHERE id = ?", user.getId(), getId());
					}

					createPageVersion(this, true, false, null);
					NodeObjectVersion[] versions = getVersions();
					if (versions.length > 0) {
						version = versions[0];
					}
				}

				DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ?, pub_queue = ?, time_pub_queue = ?, time_pub_version_queue = ? WHERE id = ?", timestamp,
						version != null ? version.getId() : null, 0, 0, null, getId());

				timePub = new ContentNodeDate(timestamp);
				timePubVersion = version != null ? version.getId() : null;

				ActionLogger.logCmd(ActionLogger.PAGETIME, TYPE_PAGE, getId(), getFolderId(), String.format("Page scheduled for publishing @%d", timestamp));
			} else {
				int pdate = TransactionManager.getCurrentTransaction().getUnixTimestamp();

				// Note: delay_publish is set to 0, because when the page was delayed for publishing,
				// the delay should be cleared when it is manually published again.
				Map<String, Object> data = new HashMap<>();
				data.put("ddate", pdate);
				data.put("pub_queue", 0);
				data.put("time_pub", 0);
				data.put("time_pub_version", null);
				data.put("delay_publish", 0);
				data.put("online", 1);

				if (updatePublisher) {
					data.put("pdate", pdate);
					this.pDate = new ContentNodeDate(pdate);
					if (user != null) {
						data.put("publisher", user.getId());
						publisherId = getInteger(user.getId(), publisherId);
					}
					setPDate(pdate);
				}

				Map<String, Object> id = new HashMap<>();
				id.put("id", getId());

				DBUtils.updateOrInsert("page", id, data);

				// create a page version (if necessary) and mark it as the published version (in any case)
				createPageVersion(this, true, null);

				// fix the page versions of page variants
				fixVariantVersions(this);

				ActionLogger.logCmd(ActionLogger.PAGEPUB, TYPE_PAGE, getId(), getFolderId(), "Page published");
			}

			recalculateModifiedFlag();

			// handle the time management
			handleTimemanagement();

			// when the page was published, we need to handle workflows
			if (timestamp == 0) {
				// inform users watching the page in a workflow
				if (workflow != null) {
					// get all watching users
					List<SystemUser> watchingUsers = new Vector<SystemUser>();
					List<PublishWorkflowStep> steps = workflow.getSteps();

					for (PublishWorkflowStep step : steps) {
						SystemUser creator = step.getCreator();

						if (!watchingUsers.contains(creator)) {
							watchingUsers.add(creator);
						}
					}

					// inform watching users
					if (watchingUsers.size() > 0) {
						MessageSender messageSender = new MessageSender();

						t.addTransactional(messageSender);
						CNI18nString i18n = new CNI18nString("The page <pageid {0}> has been published.");

						i18n.addParameter(getId().toString());

						for (SystemUser watcher : watchingUsers) {
							try (LangTrx lTrx = new LangTrx(watcher)) {
								messageSender.sendMessage(new Message(t.getUserId(), ObjectTransformer.getInt(watcher.getId(), 0), i18n.toString()));
							}
						}
					}

					// delete the workflow
					workflow.delete();
				}
			}

			// unlock the page
			unlock();

			// dirt the page cache
			t.dirtObjectCache(Page.class, getId());

			// when the page was published, we trigger the events now
			if (timestamp == 0) {
				// trigger event
				String[] props = null;
				int eventMask = Events.EVENT_CN_PAGESTATUS;

				if (!onlineBefore) {
					// page was offline before, so its "online" status changed (from off to on)
					props = new String[] { "online"};
					eventMask |= Events.UPDATE;
				} else if (timestamp == 0) {
					// set the old pdate as event property
					props = new String[] { PDATE_PROPERTY_PREFIX + Integer.toString(oldPDate, 10) };
				}
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), props, eventMask));

				if (timestamp == 0) {
					onPublish(this, onlineBefore, t.getUserId());
				}
			} else {
				// we need to sent the NOTIFY event for the page in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Page.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			}
		}

		/**
		 * Add a language variant to the page
		 * @param variant
		 */
		protected void addLanguageVariant(EditableFactoryPage variant) {
			if (addedLanguageVariants == null) {
				addedLanguageVariants = new Vector<EditableFactoryPage>();
			}
			addedLanguageVariants.add(variant);
		}

		@Override
		public void synchronizeWithPage(Page page) throws NodeException {
			if (page == null) {
				synchronizeWithPageVersion(null, 0);
			} else {
				NodeObjectVersion pageVersion = page.getVersion();

				if (pageVersion != null) {
					synchronizeWithPageVersion(page, pageVersion.getDate().getIntTimestamp());
				} else {
					synchronizeWithPageVersion(page, 0);
				}
			}
		}

		@Override
		public void synchronizeWithPageVersion(Page page, int versionTimestamp) throws NodeException {
			assertEditable();
			if (page == null) {
				if (syncPageId != 0) {
					modified = true;
				}
				syncPageId = 0;
				syncTimestamp = new ContentNodeDate(0);
			} else {
				// check whether the given page is a language variant (and not
				// the page itself)
				if (equals(page)) {
					throw new NodeException("Cannot synchronize a page with itself");
				} else {
					int pageContentSetId = ObjectTransformer.getInt(page.getContentsetId(), 0);
					int ownContentSetId = ObjectTransformer.getInt(getContentsetId(), 0);

					if (pageContentSetId == 0 || ownContentSetId == 0 || pageContentSetId != ownContentSetId) {
						throw new NodeException(
								"Cannot synchronize a page with another page, that is no language variant (contentset ids: " + ownContentSetId + " vs. "
								+ pageContentSetId + ")");
					}
					int pageId = ObjectTransformer.getInt(page.getId(), 0);
					ContentNodeDate pageVersionTime = new ContentNodeDate(versionTimestamp);

					if (syncPageId != pageId || !pageVersionTime.equals(syncTimestamp)) {
						syncPageId = pageId;
						syncTimestamp = new ContentNodeDate(pageVersionTime.getIntTimestamp());
						modified = true;
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			Page oPage = (Page) original;

			// copy Meta information
			setName(oPage.getName());
			setFilename(oPage.getFilename());
			setDescription(oPage.getDescription());
			setPriority(oPage.getPriority());
			setTemplateId(oPage.getTemplate().getId());

			// copy object tags
			Map<String, ObjectTag> thisOTags = getObjectTags();
			Map<String, ObjectTag> originalOTags = oPage.getObjectTags();

			for (Map.Entry<String, ObjectTag> entry : originalOTags.entrySet()) {
				String tagName = entry.getKey();
				ObjectTag originalTag = entry.getValue();

				if (thisOTags.containsKey(tagName)) {
					// found the tag in this page, copy the original tag over it
					thisOTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisOTags.put(tagName, (ObjectTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original page
			for (Iterator<Map.Entry<String, ObjectTag>> i = thisOTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, ObjectTag> entry = i.next();

				if (!originalOTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}

			// copy the content
			getContent().copyFrom(oPage.getContent());
		}

		@Override
		public Integer getChannelSetId() throws NodeException {
			// check if the object is new and does not have a channelset_id
			if (isEmptyId(channelSetId) && isEmptyId(id)) {
				// create a new channelset_id
				channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			}

			return super.getChannelSetId();
		}

		@Override
		public void setAlternateUrls(Set<String> niceUrls) throws NodeException {
			getAlternateUrls();
			this.modified |= this.alternateUrlsContainer.set(niceUrls);
		}
	}

	/**
	 * Implementation class for a Content
	 */
	private static class FactoryContent extends Content {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -8458306860920975000L;

		protected List<Integer> tagIds;
		private List<Integer> pageIds;

		protected int nodeId;

		protected int locked;

		/**
		 * Id of the user, who locked the content
		 */
		protected int lockedBy;

		protected final static String CONTENTTAGS = "contenttags";

		/**
		 * Create a new, empty instance of a page
		 * @param info
		 */
		protected FactoryContent(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryContent(Integer id, NodeObjectInfo info, int locked, int lockedBy, List<Integer> tagIds, int nodeId, int udate, GlobalId globalId) {
			super(id, info);
			this.tagIds = tagIds != null ? new Vector<>(tagIds) : null;
			pageIds = null;
			this.locked = locked;
			this.lockedBy = lockedBy;
			this.udate = udate;
			this.globalId = globalId;
			this.nodeId = nodeId;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Content#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PageFactory pageFactory = (PageFactory) t.getObjectFactory(Content.class);

			pageFactory.deleteContent(this);
		}

		public Map<String, ContentTag> getContentTags() throws NodeException {
			// use level2 cache
			Transaction t = TransactionManager.getCurrentTransaction();
			@SuppressWarnings("unchecked")
			Map<String, ContentTag> contentTags = (Map<String, ContentTag>) t.getFromLevel2Cache(this, CONTENTTAGS);

			if (contentTags == null) {
				contentTags = loadContentTags();
				t.putIntoLevel2Cache(this, CONTENTTAGS, contentTags);
			}
			return contentTags;
		}

		public List<Page> getPages() throws NodeException {
			return loadPages();
		}

		/**
		 * Load the ids of the content tags of this content (if not done before)
		 * @throws NodeException
		 */
		private synchronized void loadContentTagIds() throws NodeException {
			if (tagIds == null) {

				// when the content is new, it has no contenttags to load
				if (isEmptyId(getId())) {
					tagIds = new ArrayList<>();
				} else {
					if (getObjectInfo().isCurrentVersion()) {
						// get the current tags
						tagIds = DBUtils.select("SELECT t.id as id FROM contenttag t"
								+ " LEFT JOIN construct c ON c.id = t.construct_id"
								+ " WHERE t.content_id = ? AND c.id IS NOT NULL", ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
					} else {
						// get the tags of the versioned content
						int versionTimestamp = getObjectInfo().getVersionTimestamp();

						tagIds = DBUtils.select(
								"SELECT id FROM contenttag_nodeversion WHERE content_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? GROUP BY id",
								ps -> {
									ps.setInt(1, getId());
									ps.setInt(2, versionTimestamp);
									ps.setInt(3, versionTimestamp);
								}, DBUtils.IDLIST);
					}
				}

			}
		}

		private Map<String, ContentTag> loadContentTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, ContentTag> contentTags = new HashMap<String, ContentTag>();

			// load the ids (if not done before)
			loadContentTagIds();

			Collection<ContentTag> tags;

			tags = getObjectInfo().isEditable()
					? t.getObjects(ContentTag.class, tagIds, getObjectInfo().isEditable())
					: t.getObjects(ContentTag.class, tagIds, getObjectInfo().getVersionTimestamp());
			for (ContentTag tag : tags) {
				contentTags.put(tag.getName(), tag);
			}

			return contentTags;
		}

		private synchronized void loadPageIds() throws NodeException {
			if (pageIds == null) {
				if (isEmptyId(getId())) {
					pageIds = new ArrayList<>();
				} else {
					pageIds = DBUtils.select("SELECT id FROM page WHERE content_id = ?", ps -> ps.setInt(1, getId()),
							DBUtils.IDLIST);
				}
			}
		}

		private List<Page> loadPages() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadPageIds();
			return t.getObjects(Page.class, pageIds);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Content {" + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
		 */
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			final List<Integer> contentTagIds = new Vector<Integer>();
			final List<Integer> valueIds = new Vector<Integer>();
			final int contentId = ObjectTransformer.getInt(getId(), 0);

			// we will now select all contenttag and value id's for the page in a single sql statement.
			DBUtils.executeStatement("SELECT c.id tag_id, v.id value_id FROM contenttag c LEFT JOIN value v ON c.id = v.contenttag_id WHERE c.content_id = ?",
					new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, contentId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int contentTagId = rs.getInt("tag_id");
						int valueId = rs.getInt("value_id");

						if (!contentTagIds.contains(contentTagId)) {
							contentTagIds.add(contentTagId);
						}
						valueIds.add(valueId);
					}
				}
			});

			// get the tags now, this will make sure that it is not necessary to load the tags in single sql statements
			t.getObjects(ContentTag.class, contentTagIds);
			t.getObjects(Value.class, valueIds);

			for (Integer tagId :  contentTagIds) {
				t.dirtObjectCache(ContentTag.class, tagId, false);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#unlock()
		 */
		public void unlock() throws NodeException {
			// unlock the content, if locked for this user.
			Transaction t = TransactionManager.getCurrentTransaction();

			// check whether contents shall be unlocked immediately or on trx commit
			if (ObjectTransformer.getBoolean(t.getAttributes().get(NodeFactory.UNLOCK_AT_TRX_COMMIT), false)) {
				t.addTransactional(new UnlockContentTransactional(ObjectTransformer.getInt(getId(), -1)));
				// dirt the cache
				t.dirtObjectCache(Content.class, getId());
			} else {
				PreparedStatement pst = null;

				try {
					pst = t.prepareUpdateStatement("UPDATE content SET locked = 0, locked_by = 0 WHERE id = ? AND locked_by = ?");
					pst.setInt(1, ObjectTransformer.getInt(getId(), -1));
					pst.setInt(2, t.getUserId());
					pst.executeUpdate();
					ActionLogger.logCmd(ActionLogger.UNLOCK, Content.TYPE_CONTENT, getId(), 0, "unlock-java");
					for (Page page : getPages()) {
						ActionLogger.logCmd(ActionLogger.UNLOCK, Page.TYPE_PAGE, page.getId(), 0, "unlock-java");
					}
				} catch (SQLException e) {
					throw new NodeException("Error while unlocking {" + this + "}", e);
				} finally {
					t.closeStatement(pst);

					// dirt the cache
					t.dirtObjectCache(Content.class, getId());

					// set the content to be unlocked
					unsetContentLocked(getInteger(getId(), null));
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryContent(this, getFactory().getFactoryHandle(Content.class).createObjectInfo(Content.class, true), true);
		}

		@Override
		public boolean isLocked() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			int lockTimeout = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("lock_time"), 600);

			return (locked != 0 && (t.getUnixTimestamp() - locked) < lockTimeout);
		}

		@Override
		public ContentNodeDate getLockedSince() throws NodeException {
			if (isLocked()) {
				return new ContentNodeDate(locked);
			} else {
				return null;
			}
		}

		@Override
		public SystemUser getLockedBy() throws NodeException {
			if (isLocked()) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return t.getObject(SystemUser.class, lockedBy);
			} else {
				return null;
			}
		}

		@Override
		public Node getNode() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(Node.class, nodeId);
		}
	}

	/**
	 * Implementation class for an editable Content
	 */
	private static class EditableFactoryContent extends FactoryContent {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -7109410296671136295L;

		/**
		 * editable copies of the contenttags in this content
		 */
		private Map<String, ContentTag> contentTags = null;

		/**
		 * Flag to mark whether the content has been modified
		 */
		protected boolean modified = false;

		/**
		 * Page of this content
		 */
		protected Page page = null;

		/**
		 * Create a new empty instance of a content
		 * @param info info about the content
		 */
		protected EditableFactoryContent(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor for getting a copy of the given object
		 * @param content content to copy
		 * @param info info about the copy
		 * @param asNewContent true when the copy shall be a new content, false if
		 *        it shall be the editable version of the content
		 */
		protected EditableFactoryContent(FactoryContent content, NodeObjectInfo info, boolean asNewContent) throws NodeException {
			super(asNewContent ? null : content.getId(), info, asNewContent ? -1 : content.locked, asNewContent ? -1 : content.lockedBy,
					asNewContent ? null : content.tagIds, content.nodeId, asNewContent ? -1 : content.getUdate(), asNewContent ? null : content.getGlobalId());
			if (asNewContent) {
				// copy the contenttags
				Map<String, ContentTag> originalContentTags = content.getContentTags();

				contentTags = new HashMap<String, ContentTag>(originalContentTags.size());
				for (Iterator<Map.Entry<String, ContentTag>> i = originalContentTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, ContentTag> entry = i.next();

					contentTags.put(entry.getKey(), (ContentTag) entry.getValue().copy());
				}
				modified = true;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryContent#getContentTags()
		 */
		public Map<String, ContentTag> getContentTags() throws NodeException {
			if (contentTags == null) {
				contentTags = super.getContentTags();
			}

			for (ContentTag tag : contentTags.values()) {
				tag.setContent(this);
			}
			return contentTags;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryContent#addContentTag(int)
		 */
		public ContentTag addContentTag(int constructId) throws ReadOnlyException,
					NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// get the construct
			Construct construct = (Construct) t.getObject(Construct.class, constructId);

			if (construct == null) {
				throw new NodeException("Cannot add contenttag to {" + this + "}: invalid constructId {" + constructId + "} provided");
			}

			Set<String> reservedNames = new HashSet<String>();

			if (getId() != null) {
				// collect reserved template tag names from all templates used
				for (Page p : getPages()) {
					Template tpl = p.getTemplate();
					for (Entry<String, TemplateTag> tt : tpl.getTemplateTags().entrySet()) {
						if (!tt.getValue().isPublic()) {
							reservedNames.add(tt.getKey());
						}
					}
				}
			}

			// create a new tag instance
			ContentTag tag = (ContentTag) t.createObject(ContentTag.class);

			// set the content
			tag.setContent(this);

			// set the construct id
			tag.setConstructId(constructId);

			// eventually enable the tag
			if (construct.isAutoEnable()) {
				tag.setEnabled(true);
			}

			// find a unique name for the new tag
			Set<String> contentTagNames = getContentTags().keySet();
			// take the construct's baseword and add numbers
			String keyWord = construct.getKeyword();
			String tagName = null;
			int counter = 0;

			do {
				counter++;
				tagName = keyWord + counter;
			} while (contentTagNames.contains(tagName) || reservedNames.contains(tagName));

			tag.setName(tagName);

			// add the tag to the map of tags
			Map<String, ContentTag> tags = getContentTags();

			tags.put(tagName, tag);

			// return the tag
			return tag;
		}

		/**
		 * Add a new contenttag to the content, which is based on the given templatetag
		 * @param templateTag template tag
		 * @return new contenttag
		 * @throws NodeException
		 */
		protected ContentTag addContentTag(TemplateTag templateTag) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// create a new tag instance
			ContentTag tag = t.createObject(ContentTag.class);

			// make it a clone of the templateTag
			tag.clone(templateTag);

			// add the tag to the map of tags
			Map<String, ContentTag> tags = getContentTags();

			tags.put(templateTag.getName(), tag);

			// return the tag
			return tag;
		}

		/**
		 * Set the page of the content
		 * @param page page
		 */
		protected void setPage(Page page) {
			this.page = page;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			// first check whether the page is editable
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = this.modified;

			// save the content, if necessary
			if (isModified) {
				saveContentObject(this);
				this.modified = false;
			}

			// save all the contenttags and check which tags no longer exist (and need to be removed)
			Map<String, ContentTag> tags = getContentTags();
			List<Integer> tagIdsToRemove = new Vector<>();

			if (tagIds != null) {
				tagIdsToRemove.addAll(tagIds);
			}
			for (Iterator<ContentTag> i = tags.values().iterator(); i.hasNext();) {
				ContentTag tag = i.next();

				tag.setContentId(getId());
				isModified |= tag.save();

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<ContentTag> tagsToRemove = t.getObjects(ContentTag.class, tagIdsToRemove);

				for (Iterator<ContentTag> i = tagsToRemove.iterator(); i.hasNext();) {
					NodeObject tagToRemove = i.next();

					tagToRemove.delete();
				}
				isModified = true;
			}

			if (isModified) {
				t.dirtObjectCache(Content.class, getId());
			}

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);

			Content oContent = (Content) original;

			// copy content tags
			Map<String, ContentTag> thisCTags = getContentTags();
			Map<String, ContentTag> originalCTags = oContent.getContentTags();

			for (Map.Entry<String, ContentTag> entry : originalCTags.entrySet()) {
				String tagName = entry.getKey();
				ContentTag originalTag = entry.getValue();

				if (thisCTags.containsKey(tagName)) {
					// found the tag in this content, copy the original tag over it
					thisCTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisCTags.put(tagName, (ContentTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original content
			for (Iterator<Map.Entry<String, ContentTag>> i = thisCTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, ContentTag> entry = i.next();

				if (!originalCTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}
		}

		@Override
		public List<Page> getPages() throws NodeException {
			List<Page> pages = super.getPages();
			if (page != null && !pages.contains(page)) {
				pages = new ArrayList<>(pages);
				pages.add(page);
			}
			return pages;
		}
	}

	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(AbstractFactory.class);

	public PageFactory() {
		super();
	}

	@Override
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		// page supports versioning
		return Page.class.equals(clazz) || Content.class.equals(clazz);
	}

	@Override
	public void updateNonVersionedData(NodeObject versionedObject,
			NodeObject currentVersion) throws NodeException {
		if (versionedObject instanceof FactoryPage && currentVersion instanceof FactoryPage) {
			FactoryPage fVersionedObject = (FactoryPage) versionedObject;
			FactoryPage fCurrentVersion = (FactoryPage) currentVersion;

			fVersionedObject.updateNonVersionedData(fCurrentVersion);
		}
	}

	/**
	 * Deletes a content but instead of directly deleting
	 * @param content The content to delete
	 */
	public void deleteContent(FactoryContent content) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		@SuppressWarnings("unchecked")
		Collection<Content> deleteList = getDeleteList(t, Content.class);

		deleteList.add(content);
	}

	/**
	 * Deletes a page but instead of directly deleting it the action is cached and performed on flush.
	 * @param page The page to delete
	 * @throws NodeException
	 */
	public void deletePage(FactoryPage page) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		@SuppressWarnings("unchecked")
		Collection<Page> deleteList = getDeleteList(t, Page.class);

		deleteList.add(page);

		// when multichannelling is active and the page is a master, also get all
		// localized objects and remove them
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && page.isMaster()) {
			Map<Integer, Integer> channelSet = page.getChannelSet();

			for (Integer pageId : channelSet.values()) {
				Page locPage = (Page) t.getObject(Page.class, pageId, -1, false);

				if (!deleteList.contains(locPage)) {
					deleteList.add(locPage);
				}
			}
		}
	}

	/**
	 * Checks if the content of a page can be deleted or if it is referenced by another page.
	 * @param page Page which should be deleted
	 * @return true if the content of the page can be safely deleted
	 * @throws NodeException
	 */
	public boolean canDeleteContent(FactoryPage page) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		@SuppressWarnings("unchecked")
		Collection<Page> deleteList = getDeleteList(t, Page.class);
		Collection<Page> toDelete = new LinkedList<Page>(deleteList);

		toDelete.add(page);

		Collection<Page> variants = page.getPageVariants();

		return toDelete.containsAll(variants);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#flush()
	 */
	public void flush() throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();

		// Flush the pages to delete
		if (!isEmptyDeleteList(t, Page.class)) {
			@SuppressWarnings("unchecked")
			Collection<Page> deleteList = getDeleteList(t, Page.class);

			// prepare a map id -> Page of the deleted pages (will be used when checking dead links)
			final Map<Integer, Page> deletedPages = new HashMap<>(deleteList.size());

			List<Integer> pageIds = new LinkedList<>();

			// log command and trigger event
			for (Iterator<Page> it = deleteList.iterator(); it.hasNext();) {
				Page page = (Page) it.next();

				pageIds.add(page.getId());
				deletedPages.put(page.getId(), page);
				ActionLogger.logCmd(ActionLogger.DEL, Page.TYPE_PAGE, page.getId(), null, "Page.delete()");

				String meshLanguage = "en";
				try {
					meshLanguage = MeshPublisher.getMeshLanguage(page);
				} catch (NodeException e) {
					// this might happen, when the language is also deleted in the same transaction
					Integer languageId = page.getLanguageId();
					if (languageId != null) {
						// so if the page had a language, we try to find it in the deletelist for languages
						AbstractFactory languageFactory = (AbstractFactory) t.getObjectFactory(ContentLanguage.class);
						Collection<ContentLanguage> deletedLanguanges = languageFactory.getDeleteList(ContentLanguage.class);
						if (CollectionUtils.isNotEmpty(deletedLanguanges)) {
							meshLanguage = deletedLanguanges.stream().filter(lang -> Objects.equals(lang.getId(), languageId))
									.findFirst().map(ContentLanguage::getCode).orElse(meshLanguage);
						}
					}
				}

				Events.trigger(page, new String[] { ObjectTransformer.getString(page.getFolder().getNode().getId(), ""),
						MeshPublisher.getMeshUuid(page), meshLanguage }, Events.DELETE);

				// if the folder is a localized copy, it was hiding other folders (which are now "created")
				if (!page.isMaster()) {
					unhideFormerHiddenObjects(Page.TYPE_PAGE, page.getId(), page.getChannel(), page.getChannelSet());
				}
			}

			// delete the page from every list
			DBUtils.selectAndDelete("ds_obj",
					"SELECT ds_obj.id AS id FROM ds_obj, ds WHERE " + "ds_obj.templatetag_id = ds.templatetag_id AND "
					+ "ds_obj.contenttag_id = ds.contenttag_id AND " + "ds_obj.objtag_id = ds.objtag_id AND " + "ds.o_type = " + Page.TYPE_PAGE + " AND "
					+ "ds.is_folder != 1 AND " + "ds_obj.o_id IN",
					pageIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion","SELECT ds_obj.id AS id FROM ds_obj_nodeversion ds_obj, ds_nodeversion ds WHERE " +
			// "ds_obj.templatetag_id = ds.templatetag_id AND " +
			// "ds_obj.contenttag_id = ds.contenttag_id AND " +
			// "ds_obj.objtag_id = ds.objtag_id AND " +
			// "ds.o_type = " + Page.TYPE_PAGE + " AND " +
			// "ds.is_folder != 1 AND " +
			// "ds_obj.o_id IN", pageIds);

			// send messages to the editors of pages and templates which will have invalid references after deleting the pages
			checkInvalidLinks(pageIds, deletedPages, new CNI18nString("deleted_lc"));

			// Update URL- and tag parts which are referencing to this page
			final List<Integer> urlPartIds = new Vector<>();
			final List<Integer> tagPartIds = new Vector<>();

			DBUtils.executeStatement("SELECT id FROM part WHERE type_id = 4", new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						urlPartIds.add(new Integer(rs.getInt("id")));
					}
				}
			});
			DBUtils.executeStatement("SELECT id FROM part WHERE type_id = 11", new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						tagPartIds.add(new Integer(rs.getInt("id")));
					}
				}
			});
			String pageIdSql = buildIdSql(pageIds);

			// URL parts
			if (!urlPartIds.isEmpty()) {
				final Vector<Integer> valueIds = new Vector<>();

				// Dirt the cache of the values
				DBUtils.executeMassStatement("SELECT id FROM value WHERE value_ref IN " + pageIdSql + " AND part_id IN", urlPartIds, 1, new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Integer id = rs.getInt("id");

							t.dirtObjectCache(Value.class, id);
							valueIds.add(id);
						}
					}
				});
				// Update the values
				if (!valueIds.isEmpty()) {
					DBUtils.executeMassStatement("UPDATE value SET value_ref = 0 WHERE id IN", valueIds, 1, null);
				}
			}

			// Tag parts
			if (!tagPartIds.isEmpty()) {
				final Vector<Integer> valueIds = new Vector<>();

				// Dirt the cache of the values
				DBUtils.executeMassStatement("SELECT id FROM value WHERE info IN " + pageIdSql + " AND value_text = 'p' AND part_id IN", tagPartIds, 1,
						new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Integer id = rs.getInt("id");
							t.dirtObjectCache(Value.class, id);
							valueIds.add(id);
						}
					}
				});
				// Update the values
				if (!valueIds.isEmpty()) {
					DBUtils.executeMassStatement("UPDATE value SET value_ref = 0, info = 0 WHERE id IN", valueIds, 1, null);
				}
			}
			// Delete from dependency map
			flushDelete("DELETE FROM dependencymap WHERE mod_type = " + Page.TYPE_PAGE + " AND mod_id IN", Page.class);
			flushDelete("DELETE FROM dependencymap WHERE dep_type = " + Page.TYPE_PAGE + " AND dep_id IN", Page.class);

			// delete the page and versions
			// flushDelete("DELETE FROM page_nodeversion WHERE id IN", Page.class);
			flushDelete("DELETE FROM page WHERE ID IN", Page.class);
		}

		// Flush the contents to delete
		if (!getDeleteList(t, Content.class).isEmpty()) {
			flushDelete("DELETE from content WHERE id IN", Content.class);
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends NodeObject>[] getProvidedClasses() {
		return new Class[] { Page.class, Content.class};
	}

	public int getTType(Class<? extends NodeObject> clazz) {
		if (Page.class.equals(clazz)) {
			return Page.TYPE_PAGE;
		}
		if (Content.class.equals(clazz)) {
			return Content.TYPE_CONTENT;
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (Page.class.equals(clazz)) {
			return (T) new EditableFactoryPage(handle.createObjectInfo(Page.class, true));
		} else if (Content.class.equals(clazz)) {
			return (T) new EditableFactoryContent(handle.createObjectInfo(Content.class, true));
		} else {
			return null;
		}

	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (Page.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_PAGE_SQL, SELECT_VERSIONED_PAGE_SQL, SELECT_VERSIONED_PAGE_PARAMS);
		} else if (Content.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_CONTENT_SQL, null, null);
		}
		return null;
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		// TODO also do versioned queries
		final String idSql = buildIdSql(ids);

		if (Page.class.equals(clazz)) {
			String[] preloadSql = new String[] { "SELECT obj_id AS id, id AS id2 FROM objtag WHERE obj_type = " + Page.TYPE_PAGE + " AND obj_id IN " + idSql };

			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_PAGE_SQL + idSql, preloadSql);
		} else if (Content.class.equals(clazz)) {
			String[] preloadSql = new String[] { "SELECT content_id AS id, id AS id2 FROM contenttag WHERE content_id IN " + idSql };

			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_CONTENT_SQL + idSql, preloadSql);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {

		if (Page.class.equals(clazz)) {
			return (T) loadPageObject(id, info, rs, idLists);
		} else if (Content.class.equals(clazz)) {
			return (T) loadContentObject(id, info, rs, idLists);
		}
		return null;
	}

	/**
	 * Save the given page object
	 * @param page page
	 * @param updateEditor true to update editor and edate
	 * @throws NodeException
	 */
	private static void savePageObject(EditableFactoryPage page, boolean updateEditor) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();

		@SuppressWarnings("unchecked")
		Map<String, String> sanitizeCharacters = nodePreferences.getPropertyMap("sanitize_character");
		String replacementChararacter = nodePreferences.getProperty("sanitize_replacement_character");
		String[] preservedCharacters = nodePreferences.getProperties("sanitize_allowed_characters");

		// make sure that the page has a channelset_id
		page.getChannelSetId();

		boolean isNew = AbstractPage.isEmptyId(page.getId());

		// make sure, that the channelId is set correctly
		if (isNew && page.master) {
			page.channelId = MultichannellingFactory.correctChannelId(ObjectTransformer.getInt(page.folderId, 0), page.channelId);
		}

		// set the editor data
		if (isNew || updateEditor) {
			page.editorId = t.getUserId();
			page.eDate = new ContentNodeDate(t.getUnixTimestamp());
		}

		if (!StringUtils.isEmpty(page.description)) {
			page.description = page.description.substring(0, Math.min(page.description.length(), Page.MAX_DESCRIPTION_LENGTH));
		}

		if (isNew) {
			// set the creator data
			page.creatorId = t.getUserId();
			page.cDate = new ContentNodeDate(t.getUnixTimestamp());

			boolean fixPageName = false;
			boolean fixPageFilename = false;

			// when the page name is not set, we set it to empty string (will update it later with the page id)
			if (StringUtils.isEmpty(page.name)) {
				page.name = "";
				fixPageName = true;
			}

			page.name = StringUtils.stripTags(page.name.trim());
			page.name = page.name.substring(0, Math.min(page.name.length(), Page.MAX_NAME_LENGTH));

			// when the filename is not set, we set it to empty string (will update it later)
			if (StringUtils.isEmpty(page.filename)) {
				page.filename = "";
				fixPageFilename = true;
			}

			// generate a new contentset id
			if (ObjectTransformer.isEmpty(page.contentSetId)) {
				page.contentSetId  = page.createContentSetId(page.globalContentsetId);
				page.globalContentsetId = null;
			}

			String defaultExtension = getDefaultPageFileNameExtension(page, page.filename);
			page.filename = FileUtil.sanitizeName(page.filename, defaultExtension, sanitizeCharacters, replacementChararacter, preservedCharacters);

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!ObjectTransformer.isEmpty(page.niceUrl)) {
					page.niceUrl = makePageNiceURLUnique(page, page.niceUrl);
				}
				if (page.alternateUrlsContainer != null && !page.alternateUrlsContainer.isEmpty()) {
					page.alternateUrlsContainer.modify(url -> makePageNiceURLUnique(page, url));
				}
			} else {
				page.niceUrl = null;
			}

			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_PAGE_SQL,
					new Object[] {
							page.name, page.niceUrl, page.description, page.filename, page.priority, page.contentId, page.templateId, page.getFolderId(),
							page.creatorId, page.cDate.getIntTimestamp(), page.customCDate.getIntTimestamp(), page.editorId, page.eDate.getIntTimestamp(),
							page.customEDate.getIntTimestamp(), page.languageId, page.contentSetId, page.channelSetId, page.channelId, page.syncPageId,
							page.syncTimestamp.getIntTimestamp(), page.master, page.excluded, page.disinheritDefault,
							ObjectTransformer.getString(page.getGlobalId(), "") });

			if (keys.size() != 1) {
				throw new NodeException("Error while inserting new page, could not get the insertion id");
			}

			// set the new page id
			page.setId(keys.get(0));
			synchronizeGlobalId(page);

			Folder folderMaster;
			try (NoMcTrx nmt = new NoMcTrx()) {
				folderMaster = page.getFolder().getMaster();
			}
			// if the new page is a master object, we initially get the restriction settings of the folder
			// if it is a localized copy, we get the restriction settings from the master page
			Disinheritable<?> restrictionSource = page.isMaster() ? folderMaster : page.getMaster();
			ChannelTreeSegment visibility = new ChannelTreeSegment(restrictionSource, true);

			// master pages will be further restricted, identically to their language variants
			if (page.isMaster()) {
				List<Page> langVariants = page.getLanguageVariants(false);
				for (Page langPage : langVariants) {
					if (page.equals(langPage)) {
						continue;
					}
					Page langPageMaster = langPage.getMaster();
					visibility = visibility.addRestrictions(langPageMaster.isExcluded(), langPageMaster.getDisinheritedChannels());
				}
			}
			// this call will save the restriction settings, if the new page is a master
			// if the new page is a localized copy, it will just check for restriction consistency (e.g. it will forbid creation of
			// a localized copy, if the master is not inherited in the channel)
			DisinheritUtils.saveNewDisinheritedAssociations(page, visibility.isExcluded(), visibility.getRestrictions());

			page.setDisinheritDefault(
				page.disinheritDefault || restrictionSource.isDisinheritDefault(),
				false);

			// we need to fix the page name
			if (fixPageName) {
				page.name = ObjectTransformer.getString(page.getId(), null);
			} else {
				String newPagename = DisinheritUtils.makeUniqueDisinheritable(page, SeparatorType.blank, Page.MAX_NAME_LENGTH);

				if (!StringUtils.isEqual(page.name, newPagename)) {
					page.name = newPagename;
					// make sure, that the page name will be saved
					fixPageName = true;
				}
			}

			// we need to fix the filename
			if (fixPageFilename) {
				fixPageFilename(page);
			} else {
				// or at least make it unique
				String oldName = page.getFilename();
				makePageFilenameUnique(page);

				if (!StringUtils.isEqual(page.filename, oldName)) {
					// ensure, that that filename will be saved
					fixPageFilename = true;
				}
			}

			// save the page (again)
			if (fixPageName || fixPageFilename) {
				DBUtils.executeUpdate(UPDATE_PAGE_SQL,
						new Object[] {
					page.name, page.niceUrl, page.description, page.filename, page.priority, page.contentId, page.templateId, page.getFolderId(), page.editorId,
					page.customCDate.getIntTimestamp(), page.eDate.getIntTimestamp(), page.customEDate.getIntTimestamp(), page.languageId, page.contentSetId,
					page.channelSetId, page.channelId, page.syncPageId, page.syncTimestamp.getIntTimestamp(), page.getId()
				});
			}

			// dirt the cache of the folder
			t.dirtObjectCache(Folder.class, page.getFolderId());

			// dirt caches for all language variants
			List<Page> languageVariants = page.getLanguageVariants(false);

			for (Page languageVariant : languageVariants) {
				t.dirtObjectCache(Page.class, languageVariant.getId());
			}

			ActionLogger.logCmd(ActionLogger.CREATE, Page.TYPE_PAGE, page.getId(), page.getFolderId(), "cmd_page_create-java");
		} else {
			// when the page name is set to something different than the page id
			// and the filename starts with the page id, we also need to fix the
			// page filename
			String pageId = ObjectTransformer.getString(page.getId(), "");

			page.name = StringUtils.stripTags(page.name.trim());
			page.name = page.name.substring(0, Math.min(page.name.length(), Page.MAX_NAME_LENGTH));

			boolean autogeneratePageFilename = false;

			if (StringUtils.isEmpty(page.filename)) {
				// We also need to autogenerate the filename, when it is currently empty.
				autogeneratePageFilename = true;
			} else if (!StringUtils.isEqual(page.name, pageId) && page.filename.startsWith(pageId)) {
				// A page name has been set, but the filename is still the page ID.
				autogeneratePageFilename = true;
			} else {
				String defaultExtension = getDefaultPageFileNameExtension(page, page.filename);
				page.filename = FileUtil.sanitizeName(page.filename, defaultExtension, sanitizeCharacters, replacementChararacter, preservedCharacters);
			}

			// make the page name unique
			DisinheritUtils.makeUniqueDisinheritable(page, SeparatorType.blank, Page.MAX_NAME_LENGTH);

			// we need to fix the filename
			if (autogeneratePageFilename) {
				fixPageFilename(page);
			} else {
				// or at least make it unique
				makePageFilenameUnique(page);
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!ObjectTransformer.isEmpty(page.niceUrl)) {
					page.niceUrl = makePageNiceURLUnique(page, page.niceUrl);
				}
				if (page.alternateUrlsContainer != null && !page.alternateUrlsContainer.isEmpty()) {
					page.alternateUrlsContainer.modify(url -> makePageNiceURLUnique(page, url));
				}
			} else {
				page.niceUrl = null;
			}

			DBUtils.executeUpdate(UPDATE_PAGE_SQL,
					new Object[] {
				page.name, page.niceUrl, page.description, page.filename, page.priority, page.contentId, page.templateId, page.getFolderId(), t.getUserId(),
				page.customCDate.getIntTimestamp(), t.getUnixTimestamp(), page.customEDate.getIntTimestamp(), page.languageId, page.contentSetId,
				page.channelSetId, page.channelId, page.syncPageId, page.syncTimestamp.getIntTimestamp(), page.getId()
			});

			ActionLogger.logCmd(ActionLogger.EDIT, Page.TYPE_PAGE, page.getId(), page.getFolderId(), "cmd_page_update-java");
		}

		// store alternate URLs.
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS) && page.alternateUrlsContainer != null) {
			page.alternateUrlsContainer.save();
			page.alternateUrlsContainer = null;
		}
	}

	/**
	 * Get the default filename extension included in the page name.
	 * This can be the language code plus the markup language or the markup language alone
	 *
	 * @param page page
	 * @param fileName filename
	 * @return the page filename extension
	 * @throws NodeException
	 */
	public static String getDefaultPageFileNameExtension(Page page, String fileName) throws NodeException {
		if (StringUtils.isEmpty(fileName)) {
			return fileName;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		String defaultPageFileNameExtension = "";
		String markupLanguageExtension = null;
		Node node = page.getOwningNode();

		if (page.getTemplate().getMarkupLanguage() != null && !node.isOmitPageExtension()) {
			markupLanguageExtension = page.getTemplate().getMarkupLanguage().getExtension();
		}

		// get the language id with the  markup language extension or the markup language extension alone
		if (page.getLanguage() != null && prefs.getFeature("contentgroup3_pagefilename") && node.getPageLanguageCode() == PageLanguageCode.FILENAME) {
			String languageExtension = page.getLanguage().getCode();
			if (markupLanguageExtension != null && fileName.endsWith(languageExtension + "." + markupLanguageExtension)) {
				defaultPageFileNameExtension = languageExtension + "." + markupLanguageExtension;
			} else {
				defaultPageFileNameExtension = languageExtension;
			}
		} else if (markupLanguageExtension != null && fileName.endsWith(markupLanguageExtension)) {
			defaultPageFileNameExtension = markupLanguageExtension;
		}

		return defaultPageFileNameExtension;
	}

	/**
	 * Fix the filename of the given page
	 * @param page page which needs the filename to be fixed
	 * @throws NodeException
	 */
	private static void fixPageFilename(EditableFactoryPage page) throws NodeException {
		String fileName = suggestFilename(page);

		// now make the filename unique
		page.setFilename(fileName);
		makePageFilenameUnique(page);
	}

	/**
	 * Ensures the specified page has a unique filename
	 *
	 * @param page the page to work on
	 * @throws NodeException
	 */
	private static void makePageFilenameUnique(Page page) throws NodeException {
		boolean foundUniqueFilename = false;
		Pattern fileNamePattern = Pattern.compile("([^.]*?)(0*)((?:[1-9][0-9]*)?)(?:\\.(.+))?");

		try {
			Folder pageFolder;
			try (NoMcTrx nmt = new NoMcTrx()) {
				pageFolder = page.getFolder();
			}
			Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(pageFolder, new ChannelTreeSegment(page, false));

			Matcher fileNameMatcher = fileNamePattern.matcher(page.getFilename());
			if (!fileNameMatcher.matches()) {
				throw new NodeException("Error while making proposed filename {" + page.getFilename() + "} for {" + page
						+ "} unique. Filename does not match the expected pattern.");
			}
			String baseFileName = fileNameMatcher.group(1);
			String leadingZeros = fileNameMatcher.group(2);
			int counterLength = fileNameMatcher.group(3).length();
			String extension = fileNameMatcher.group(4);

			String searchFilenamePattern = CNStringUtils.escapeRegex(baseFileName) + "([0-9]*)";
			if (!StringUtils.isEmpty(extension)) {
				searchFilenamePattern += "\\." + CNStringUtils.escapeRegex(extension);
			}

			Set<String> filenameCollisions = DisinheritUtils.getUsedFilenames(page, searchFilenamePattern, pcf, null);

			// Update the filename multiple times and repeat the filename collision checks
			while (!foundUniqueFilename) {

				// int collisionCount = checkFilenameCollisions(page, fileName);
				if (filenameCollisions.contains(page.getFilename().toLowerCase())) {
					// we found a page with conflicting filename
					fileNameMatcher = fileNamePattern.matcher(page.getFilename());

					if (fileNameMatcher.matches()) {
						baseFileName = fileNameMatcher.group(1);
						leadingZeros = fileNameMatcher.group(2);
						counterLength = fileNameMatcher.group(3).length();
						BigInteger counter = counterLength > 0 ? new BigInteger(fileNameMatcher.group(3)) : BigInteger.ZERO;

						extension = fileNameMatcher.group(4);

						String newCounter = String.valueOf(counter.add(BigInteger.ONE));

						// We remove a leading zero if we get a new digit
						if (newCounter.length() > counterLength && leadingZeros.length() > 0) {
							leadingZeros = leadingZeros.substring(1);
						}

						// If the filename is too long, we first try to remove
						// digits from the basename and if that doesn't help
						// from the extension
						while (baseFileName.length() + leadingZeros.length() + newCounter.length() + (extension == null ? 0 : 1 + extension.length()) > 64) {
							if (baseFileName.length() > 0) {
								baseFileName = baseFileName.substring(0, baseFileName.length() - 1);
								continue;
							}
							if (extension != null && extension.length() > 1) {
								extension = extension.substring(0, extension.length() - 1);
								continue;
							}
							throw new NodeException("Error while making proposed filename {" + page.getFilename() + "} for {" + page + "} unique. Cannot shorten filename.");
						}

						page.setFilename(baseFileName + leadingZeros + newCounter + (extension == null ? "" : "." + extension));
					} else {
						throw new NodeException("Error while making proposed filename {" + page.getFilename() + "} for {" + page + "} unique. Filename does not match the expected pattern.");
					}
				} else {
					// we found no conflicting page
					foundUniqueFilename = true;
				}
			}
		} catch (NodeException e) {
			throw new NodeException("Error while fixing filename for {" + page + "}", e);
		}
	}

	/**
	 * Ensures the specified page has a unique Nice URL (if any)
	 *
	 * @param page page to work on
	 * @param niceUrl nice URL to make unique
	 * @return unique nice URL
	 * @throws NodeException
	 */
	private static String makePageNiceURLUnique(Page page, String niceUrl) throws NodeException {
		// nothing to do, when the page has no nice URL
		if (StringUtils.isEmpty(niceUrl)) {
			return niceUrl;
		}

		Pattern extNiceUrlPattern = Pattern.compile("^(?<path>.*)(?<number>[1-9][0-9]+)?(?<extension>\\.[^\\./]+)$");
		Pattern niceUrlPattern = Pattern.compile("^(?<path>.*)(?<number>[1-9][0-9]+)?$");

		String pathPart = niceUrl;
		int counter = 0;
		String extension = "";

		Matcher m = extNiceUrlPattern.matcher(pathPart);
		if (m.matches()) {
			pathPart = m.group("path");
			counter = Integer.parseInt(ObjectTransformer.getString(m.group("number"), "0"));
			extension = ObjectTransformer.getString(m.group("extension"), "");
		} else {
			m = niceUrlPattern.matcher(pathPart);
			if (m.matches()) {
				pathPart = m.group("path");
				counter = Integer.parseInt(ObjectTransformer.getString(m.group("number"), "0"));
			}
		}

		Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(
				NodeObjectWithAlternateUrls.PATH.apply(niceUrl), new ChannelTreeSegment(page, false));
		NodeObject obstructor = DisinheritUtils.getObjectUsingNiceURL(page, niceUrl, pcf, null);
		while (obstructor != null) {
			counter++;
			String toAdd = String.format("%d%s", counter, extension);
			if (pathPart.length() + toAdd.length() > Page.MAX_NICE_URL_LENGTH) {
				niceUrl = String.format("%s%s", pathPart.substring(0, Page.MAX_NICE_URL_LENGTH - toAdd.length()), toAdd);
			} else {
				niceUrl = String.format("%s%s", pathPart, toAdd);
			}
			obstructor = DisinheritUtils.getObjectUsingNiceURL(page, niceUrl, pcf, null);
		}

		return niceUrl;
	}

	/**
	 * Save the content to the database
	 * @param content content to save
	 * @throws NodeException
	 */
	private static void saveContentObject(EditableFactoryContent content) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = Content.isEmptyId(content.getId());

		if (isNew) {
			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_CONTENT_SQL, new Object[] {
				t.getUserId(), t.getUnixTimestamp(), t.getUserId(), t.getUnixTimestamp(), t.getUnixTimestamp(), t.getUserId(), ObjectTransformer.getString(content.getGlobalId(), "")
			});

			if (keys.size() == 1) {
				// set the new page id
				content.setId(keys.get(0));
				synchronizeGlobalId(content);

				setContentLocked(keys.get(0));
			} else {
				throw new NodeException("Error while inserting new content, could not get the insertion id");
			}
		} else {
			DBUtils.executeUpdate(UPDATE_CONTENT_SQL, new Object[] {
				t.getUserId(), t.getUnixTimestamp(), content.getId()
			});
		}
	}

	private Page loadPageObject(Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {
		String name = rs.getString("name");
		String niceUrl = rs.getString("nice_url");
		String description = rs.getString("description");
		String filename = rs.getString("filename");
		Integer templateId = new Integer(rs.getInt("template_id"));
		Integer folderId = info.isCurrentVersion() ? new Integer(rs.getInt("folder_id")) : -1;
		Integer contentId = new Integer(rs.getInt("content_id"));
		int priority = rs.getInt("priority");
		Integer contentSetId = new Integer(rs.getInt("contentset_id"));
		Integer languageId = new Integer(rs.getInt("contentgroup_id"));
		ContentNodeDate cDate = new ContentNodeDate(rs.getInt("cdate"));
		ContentNodeDate eDate = new ContentNodeDate(rs.getInt("edate"));
		ContentNodeDate pDate = new ContentNodeDate(rs.getInt("pdate"));
		ContentNodeDate timePub = new ContentNodeDate(rs.getInt("time_pub"));
		Integer timePubVersion = rs.getInteger("time_pub_version");
		int pubQueueUserId = rs.getInt("pub_queue");
		ContentNodeDate timePubQueue = new ContentNodeDate(rs.getInt("time_pub_queue"));
		Integer timePubVersionQueue = rs.getInteger("time_pub_version_queue");
		ContentNodeDate timeOff = new ContentNodeDate(rs.getInt("time_off"));
		int offQueueUserId = rs.getInt("off_queue");
		ContentNodeDate timeOffQueue = new ContentNodeDate(rs.getInt("time_off_queue"));
		int creatorId = rs.getInt("creator");
		int editorId = rs.getInt("editor");
		int publisherId = rs.getInt("publisher");
		boolean exclude = rs.getBoolean("mc_exclude");
		boolean disinheritDefault = rs.getBoolean("disinherit_default");
		List<Integer> tagIds = idLists != null ? idLists[0] : null;
		Integer channelSetId = new Integer(rs.getInt("channelset_id"));
		Integer channelId = new Integer(rs.getInt("channel_id"));
		int syncPageId = rs.getInt("sync_page_id");
		ContentNodeDate syncTimestamp = new ContentNodeDate(rs.getInt("sync_timestamp"));
		boolean master = rs.getBoolean("is_master");

		return new FactoryPage(id, info, name, niceUrl, description, filename, templateId, folderId, contentId, priority, contentSetId, languageId, tagIds,
				cDate, new ContentNodeDate(rs.getInt("custom_cdate")), eDate, new ContentNodeDate(rs.getInt("custom_edate")), pDate, creatorId, editorId,
				publisherId, timePub, timePubVersion, pubQueueUserId, timePubQueue, timePubVersionQueue, timeOff, offQueueUserId, timeOffQueue, channelSetId,
				channelId, syncPageId, syncTimestamp, master, exclude, disinheritDefault, rs.getInt("deleted"), rs.getInt("deletedby"),
				rs.getBoolean("modified"), getUdate(rs), getGlobalId(rs, "page"));
	}

	private Content loadContentObject(Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {
		List<Integer> tagIds = idLists != null ? idLists[0] : null;
		int locked = rs.getInt("locked");
		int lockedBy = rs.getInt("locked_by");

		return new FactoryContent(id, info, locked, lockedBy, tagIds, rs.getInt("node_id"), getUdate(rs), getGlobalId(rs, "content"));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T getEditableCopy(final T object, NodeObjectInfo info) throws NodeException,
				ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryPage) {
			try {
				EditableFactoryPage editableCopy = new EditableFactoryPage((FactoryPage) object, info, false);

				// synchronize the contenttags with the template
				if (editableCopy.synchronizeContentTagsWithTemplate() || editableCopy.containsNewValues()) {
					// contenttags were changed, so we need to save the page now
					editableCopy.save(true, false);
				}

				return (T) editableCopy;
			} catch (ReadOnlyException e) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Page page = (Page)object;
				Node channel = page.getChannel();
				t.setChannelId(channel != null ? channel.getId() : 0);
				try {
					throw new ReadOnlyException(e.getMessage(), "page.readonly.locked", I18NHelper.getName(page));
				} finally {
					t.resetChannel();
				}
			}
		} else if (object instanceof FactoryContent) {
			// get and store the current transaction
			Transaction currentTransaction = TransactionManager.getCurrentTransaction();

			if (!isContentLocked(getInteger(object.getId(), null))) {
				// start a new transaction
				Transaction tmpTransaction = TransactionManager.getTransaction(currentTransaction);

				TransactionManager.setCurrentTransaction(tmpTransaction);
				PreparedStatement pst = null;
				ResultSet rs = null;

				boolean locked = false;
				boolean setLocked = false;
				// this will be set to true, if the user already owned the lock (and the lock was not too old)
				boolean alreadyLocked = false;
				int lockedBy = -1;
				int lockTime = -1;
				int now = (int)(System.currentTimeMillis()/1000);

				// check the locks and eventually lock the content (do this in another transaction)
				try {
					pst = tmpTransaction.prepareStatement("SELECT locked, locked_by FROM content WHERE id = ? FOR UPDATE");
					pst.setObject(1, object.getId());
					rs = pst.executeQuery();

					if (rs.next()) {
						lockTime = rs.getInt("locked");
						lockedBy = rs.getInt("locked_by");

						int lockTimeout = ObjectTransformer.getInt(
								tmpTransaction.getNodeConfig().getDefaultPreferences().getProperty("lock_time"), 600);

						if (lockTime != 0 && lockedBy > 0 && (now - lockTime) < lockTimeout
								&& (currentTransaction.getUserId() != lockedBy)) {
							locked = true;
						} else if (lockedBy == currentTransaction.getUserId() && lockTime != 0 && (now - lockTime) < lockTimeout) {
							alreadyLocked = true;
						}
					}

					tmpTransaction.closeResultSet(rs);
					rs = null;
					tmpTransaction.closeStatement(pst);
					pst = null;

					if (!locked) {
						// now lock the content
						pst = tmpTransaction.prepareUpdateStatement("UPDATE content SET locked = ?, locked_by = ? WHERE id = ?");
						pst.setInt(1, now);
						pst.setInt(2, currentTransaction.getUserId());
						pst.setObject(3, object.getId());

						pst.executeUpdate();

						// dirt the cache
						currentTransaction.dirtObjectCache(Content.class, object.getId());

						// set the content to be locked
						setLocked = true;
					} else {
						throw new ReadOnlyException(
								"Could not lock {" + object + "} for user {" + currentTransaction.getUserId() + "}, since it is locked for user {" + lockedBy
								+ "} since {" + lockTime + "}",
								"page.readonly.locked", I18NHelper.getName(object));
					}
				} catch (SQLException e) {
					throw new NodeException("Error while locking {" + object + "} for editing for user {" + currentTransaction.getUserId() + "}", e);
				} finally {
					tmpTransaction.closeResultSet(rs);
					tmpTransaction.closeStatement(pst);

					// commit this transaction
					tmpTransaction.commit(true);

					// restore the original transaction
					TransactionManager.setCurrentTransaction(currentTransaction);

					// now set the page to be locked, it's important to do this after
					// the initial transaction has been restored, because this info is stored
					// in the transaction (as attributes)
					if (setLocked) {
						setContentLocked(getInteger(object.getId(), null));

						if (!alreadyLocked) {
							// we also write the logcmd entry with the original transation to preserve timestamps
							ActionLogger.logCmd(ActionLogger.LOCK, Content.TYPE_CONTENT, object.getId(), 0, "lock-java");
							for (Page page : ((FactoryContent) object).getPages()) {
								ActionLogger.logCmd(ActionLogger.LOCK, Page.TYPE_PAGE, page.getId(), 0, "lock-java");
					}
				}
					}
				}
			} else {
				currentTransaction.dirtObjectCache(Content.class, object.getId());
			}

			return (T) new EditableFactoryContent((FactoryContent) object, info, false);
		} else {
			throw new NodeException("PageFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Fix the page versions for all page variants of the given page. This
	 * handles the situation where a page was modified (new version of that page
	 * was created), but the page variants have no new versions, so they will
	 * never be able to render the new contents, if versioned publishing is
	 * used.
	 * @param page page
	 * @throws NodeException
	 */
	public static void fixVariantVersions(Page page) throws NodeException {
		if (page == null) {
			return;
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		// get all page variants
		List<Page> pageVariants = page.getPageVariants();

		for (Page variant : pageVariants) {
			if (page.equals(variant)) {
				// don't do it for the page itself
				continue;
			}

			boolean onlineAndNotModified = variant.isOnline() && !variant.isModified();
			int variantPDate = variant.getPDate().getTimestamp();
			boolean changedPDate = false;

			if (page.getPublisher() != null && onlineAndNotModified) {
				variantPDate = page.getPDate().getIntTimestamp();
				DBUtils.executeUpdate("UPDATE page SET publisher = ?, pdate = ? WHERE id = ?",
						new Object[] { page.getPublisher().getId(), variantPDate, variant.getId() });
				changedPDate = true;
			}

			boolean createdVersion = createPageVersion(variant, onlineAndNotModified, null);
			if (createdVersion || changedPDate) {
				t.addTransactional(new TransactionalTriggerEvent(Page.class, variant.getId(), new String[] { Page.PDATE_PROPERTY_PREFIX
						+ Integer.toString(variantPDate, 10) }, Events.EVENT_CN_PAGESTATUS));
				t.dirtObjectCache(Page.class, variant.getId(), true);
			}
		}
	}

	/**
	 * Create a version of the page (after it was saved)
	 * @param page page for which a version shall be created
	 * @param publishedVersion true if the last version shall be marked as published version, false if not
	 * @param userId ID of the user for which the versions shall be created. If this is null, the transaction user will be used. If this also is null or zero, the last editor of the page will be used
	 * @return true iff a page version was created
	 * @throws NodeException
	 */
	public static boolean createPageVersion(Page page, boolean publishedVersion, Integer userId) throws NodeException {
		return createPageVersion(page, publishedVersion, publishedVersion, userId, TransactionManager.getCurrentTransaction().getUnixTimestamp());
	}

	/**
	 * Create a version of the page (after it was saved)
	 * @param page page for which a version shall be created
	 * @param majorVersion true to create a major version
	 * @param publishedVersion true if the last version shall be marked as published version, false if not
	 * @param userId ID of the user for which the versions shall be created. If this is null, the transaction user will be used. If this also is null or zero, the last editor of the page will be used
	 * @return true iff a page version was created
	 * @throws NodeException
	 */
	public static boolean createPageVersion(Page page, boolean majorVersion, boolean publishedVersion, Integer userId) throws NodeException {
		return createPageVersion(page, majorVersion, publishedVersion, userId, TransactionManager.getCurrentTransaction().getUnixTimestamp());
	}

	/**
	 * Create a version of the page (after it was saved)
	 * @param page page for which a version shall be created
	 * @param publishedVersion true if the last version shall be marked as published version, false if not
	 * @param userId ID of the user for which the versions shall be created. If this is null, the transaction user will be used. If this also is null or zero, the last editor of the page will be used
	 * @param versionTimestamp timestamp for the version
	 * @return true iff a page version was created
	 * @throws NodeException
	 */
	public static boolean createPageVersion(Page page, boolean publishedVersion, Integer userId, int versionTimestamp) throws NodeException {
		return createPageVersion(page, publishedVersion, publishedVersion, userId, versionTimestamp);
	}

	/**
	 * Create a version of the page (after it was saved)
	 * @param page page for which a version shall be created
	 * @param majorVersion true if the version shall be a major version, false if not
	 * @param publishedVersion true if the last version shall be marked as published version, false if not
	 * @param userId ID of the user for which the versions shall be created. If this is null, the transaction user will be used. If this also is null or zero, the last editor of the page will be used
	 * @param versionTimestamp timestamp for the version
	 * @return true iff a page version was created
	 * @throws NodeException
	 */
	public static boolean createPageVersion(Page page, boolean majorVersion, boolean publishedVersion, Integer userId, int versionTimestamp) throws NodeException {
		// only major version can be published versions
		if (!majorVersion) {
			publishedVersion = false;
		}
		Long pageId = ObjectTransformer.getLong(page.getId(), null);

		if (pageId == null) {
			throw new NodeException("Error while creating versions for {" + page + "}: Could not get id");
		}
		Long contentId = ObjectTransformer.getLong(page.getContent().getId(), null);

		if (contentId == null) {
			throw new NodeException("Error while creating versions for {" + page + "}: Could not get content id");
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		String user = ObjectTransformer.getString(t.getUserId(), "");

		if (userId != null) {
			user = ObjectTransformer.getString(userId, user);
		}

		if ("0".equals(user) || StringUtils.isEmpty(user)) {
			user = ObjectTransformer.getString(page.getEditor().getId(), user);
		}

		NodeObjectVersion[] pageVersions = page.getVersions();

		// get an instance of the page version number generator
		// TODO make this configurable
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();
		NodeObjectVersion lastVersion = null;

		if (!ObjectTransformer.isEmpty(pageVersions)) {
			lastVersion = pageVersions[0];
			// ensure that the new version does not have an older timestamp than the last version
			versionTimestamp = Math.max(versionTimestamp, lastVersion.getDate().getIntTimestamp());
		}

		// get the table version instances
		TableVersion pageVersion = getPageTableVersion();
		List<TableVersion> contentIdBasedVersions = getContentIdBasedTableVersions(false);

		// create the versions
		boolean versionCreated = false;

		versionCreated = pageVersion.createVersion2(pageId, versionTimestamp, user) || versionCreated;
		for (TableVersion version : contentIdBasedVersions) {
			versionCreated = version.createVersion2(contentId, versionTimestamp, user) || versionCreated;
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			TableVersion niceUrlVersion = getAlternateUrlTableVersion();
			versionCreated = niceUrlVersion.createVersion2(pageId, versionTimestamp, user) || versionCreated;
		}

		// when we did not create a new version, we check, whether a new version already exists
		if (!versionCreated && lastVersion != null) {
			int lastVersionTimestamp = 0;
			Version[] versions = pageVersion.getVersions(pageId);

			if (!ObjectTransformer.isEmpty(versions)) {
				lastVersionTimestamp = Math.max(lastVersionTimestamp, versions[versions.length - 1].getTimestamp());
			}

			for (TableVersion tableVersion : contentIdBasedVersions) {
				versions = tableVersion.getVersions(contentId);
				if (!ObjectTransformer.isEmpty(versions)) {
					lastVersionTimestamp = Math.max(lastVersionTimestamp, versions[versions.length - 1].getTimestamp());
				}
			}

			// when we found newer entries in the versioned data, we also need to create a new nodeversion
			if (lastVersion.getDate().getIntTimestamp() < lastVersionTimestamp) {
				versionCreated = true;
			}
		}

		// when a version was created, also store the information in the
		// nodeversion table
		if (versionCreated && (lastVersion == null || lastVersion.getDate().getIntTimestamp() != versionTimestamp)) {
			PreparedStatement st = null;
			ResultSet res = null;

			try {
				// check whether the current verion of the page is the master
				// translation of another page variant. If yes, inform the last
				// editors of that pages that this version has changed
				NodeObjectVersion currentPageVersion = page.getVersion();

				if (currentPageVersion != null) {
					Collection<Integer> syncedPageIds = new Vector<Integer>();

					st = t.prepareStatement("SELECT id FROM page WHERE sync_page_id = ? AND sync_timestamp = ?");
					st.setInt(1, ObjectTransformer.getInt(page.getId(), -1));
					st.setInt(2, currentPageVersion.getDate().getIntTimestamp());
					res = st.executeQuery();
					while (res.next()) {
						syncedPageIds.add(res.getInt("id"));
					}

					t.closeResultSet(res);
					t.closeStatement(st);

					// now get the pages from the ids
					List<Page> syncedPages = t.getObjects(Page.class, syncedPageIds);

					if (!syncedPages.isEmpty()) {
						MessageSender messageSender = new MessageSender();

						for (Page syncedPage : syncedPages) {
							CNI18nString message = new CNI18nString("notification.translationmaster.changed");

							message.addParameters(
									new String[] {
								String.valueOf(page.getId()), page.getLanguage().getName(), String.valueOf(syncedPage.getId()),
								syncedPage.getLanguage().getName()
							});
							try (LangTrx lTrx = new LangTrx(syncedPage.getEditor())) {
							messageSender.sendMessage(
									new Message(t.getUserId(), ObjectTransformer.getInt(syncedPage.getEditor().getId(), 0), message.toString()));
						}
						}
						t.addTransactional(messageSender);
					}
				}

				// insert the new nodeversion into the table
				// TODO: generate and store the version number here (Note: when
				// doing this, also all existing nodeversion entries must be
				// fixed via changelog to have the page versions stored in the
				// db and the page versions in method handleResultSet() must be
				// taken from there)
				st = t.prepareInsertStatement("INSERT INTO nodeversion (timestamp, user_id, o_type, o_id, published, nodeversion) VALUES (?, ?, ?, ?, ?, ?)");
				st.setInt(1, versionTimestamp);
				st.setInt(2, Integer.parseInt(user));
				st.setInt(3, Page.TYPE_PAGE);
				st.setObject(4, pageId);
				st.setBoolean(5, publishedVersion);
				String nextVersionNumber = lastVersion != null
						? gen.getNextVersionNumber(lastVersion.getNumber(), majorVersion)
						: gen.getFirstVersionNumber(majorVersion);

				st.setString(6, nextVersionNumber);
				st.executeUpdate();

				ActionLogger.logCmd(majorVersion ? ActionLogger.MAJORVERSION : ActionLogger.VERSION, Page.TYPE_PAGE, page.getId(), 0,
						"created version " + nextVersionNumber);

				// if the created nodeversion shall be the published one, mark all others as "not published"
				if (publishedVersion) {
					DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND timestamp != ?",
							new Object[] { Page.TYPE_PAGE, pageId, versionTimestamp });
				}
			} catch (SQLException e) {
				throw new NodeException("Error while adding new page version", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(st);
			}
		} else if (majorVersion) {
			if (lastVersion != null && !lastVersion.isMajor()) {
				// generate next major version
				String nextVersionNumber = gen.makePublishedVersion(lastVersion.getNumber());

				DBUtils.updateWithPK("nodeversion", "id", "nodeversion = ?", new Object[] { nextVersionNumber },
						"o_type = ? AND o_id = ? AND timestamp = ?", new Object[] { Page.TYPE_PAGE, pageId, lastVersion.getDate().getIntTimestamp() });

				ActionLogger.logCmd(ActionLogger.MAJORVERSION, Page.TYPE_PAGE, page.getId(), 0,
						"updated version " + lastVersion.getNumber() + " to " + nextVersionNumber);

				versionCreated = true;
			}
			if (publishedVersion && lastVersion != null && !lastVersion.isPublished()) {
				// mark version as published
				DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 1 },
						"o_type = ? AND o_id = ? AND timestamp = ?", new Object[] { Page.TYPE_PAGE, pageId, lastVersion.getDate().getIntTimestamp() });

				// mark all other versions to not be the published one
				DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND timestamp != ?",
						new Object[] { Page.TYPE_PAGE, pageId, lastVersion.getDate().getIntTimestamp() });

				versionCreated = true;
			}
		}

		if (versionCreated) {
			page.clearVersions();
		}

		return versionCreated;
	}

	/**
	 * Restore the page version with given timestamp for the given page
	 * @param page page for which a version shall be returned
	 * @param versionTimestamp timestamp of the version to be restored
	 * @param versionNumber version number
	 * @param pageTable true when also the page table shall be restored
	 * @throws TransactionException
	 * @throws NodeException
	 */
	private static void restorePageVersion(EditableFactoryPage page, int versionTimestamp, String versionNumber, boolean pageTable) throws TransactionException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObjectVersion currentVersion = page.getVersion();

		Long pageId = ObjectTransformer.getLong(page.getId(), null);

		if (pageId == null) {
			throw new NodeException("Error while restoring version for {" + page + "}: Could not get id");
		}
		Long contentId = ObjectTransformer.getLong(page.getContent().getId(), null);

		if (contentId == null) {
			throw new NodeException("Error while restoring version for {" + page + "}: Could not get content id");
		}

		// only restore the page table, if asked to do so. Restoring the page table is done "manually" here, because
		// TableVersion would do a delete/insert, which would remove referening page_disinherit entries (due to referential integrity)
		if (pageTable) {
			Object[] idData = new Object[] { pageId};
			TableVersion pageVersion = getPageTableVersion();
			SimpleResultRow dataToRestoreRow = null;
			SimpleResultProcessor dataToRestore = pageVersion.getVersionData(idData, versionTimestamp);

			if (dataToRestore.size() > 0) {
				dataToRestoreRow = dataToRestore.getRow(1);

				// create statement to update the page table
				List<String> versionedColumns = pageVersion.getVersionedColumns("page");
				StringBuilder sql = new StringBuilder("UPDATE page ");
				List<Object> data = new ArrayList<Object>();
				boolean first = true;
				for (String col : versionedColumns) {
					if (!"template_id".equals(col)) {
						if (first) {
							sql.append("SET");
							first = false;
						} else {
							sql.append(",");
			}
						sql.append(" ").append(col).append(" = ? ");
						data.add(dataToRestoreRow.getObject(col));
					}
				}
				sql.append("WHERE id = ?");
				data.add(pageId);
				DBUtils.executeUpdate(sql.toString(), (Object[]) data.toArray(new Object[data.size()]));
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			TableVersion niceUrlVersion = getAlternateUrlTableVersion();
			niceUrlVersion.restoreVersion(pageId, versionTimestamp);
		}

		// get the table version instances
		List<TableVersion> contentIdBasedTableVersions = getContentIdBasedTableVersions(false);

		PageVersionRestoreProcessor restoreProcessor = new PageVersionRestoreProcessor(contentId);
		contentIdBasedTableVersions.forEach(tv -> tv.setRestoreProcessor(restoreProcessor));

		// restore the versions
		for (TableVersion version : contentIdBasedTableVersions) {
			version.restoreVersion(contentId, versionTimestamp);
		}

		// create a new page version
		if (createPageVersion(page, false, t.getUserId())) {
			// get the version of the page
		NodeObjectVersion newVersion = page.getVersion();

			recalculateModifiedFlag(page);
			t.dirtObjectCache(Page.class, page.getId());

			if (currentVersion == null || !currentVersion.getNumber().equals(newVersion.getNumber())) {
				ActionLogger.logCmd(ActionLogger.RESTORE, Page.TYPE_PAGE, page.getId(), versionTimestamp, "restored version " + versionNumber);
			}
		}
	}

	/**
	 * Purge page versions older than the given timestamp
	 * @param page page for which the old versions shall be kept
	 * @param versionTimestamp timestamp of the oldest version that shall be kept
	 * @throws NodeException
	 */
	private static void purgeOlderPageVersions(FactoryPage page, int versionTimestamp) throws NodeException {
		Long pageId = ObjectTransformer.getLong(page.getId(), null);

		if (pageId == null) {
			throw new NodeException("Error while purging versions for {" + page + "}: Could not get id");
		}
		Long contentId = ObjectTransformer.getLong(page.getContent().getId(), null);

		if (contentId == null) {
			throw new NodeException("Error while purging versions for {" + page + "}: Could not get content id");
		}

		// get the table version instances
		TableVersion pageVersion = getPageTableVersion();
		List<TableVersion> contentIdBasedTableVersions = getContentIdBasedTableVersions(false);

		// purge the versions
		pageVersion.purgeVersions(pageId, versionTimestamp);
		for (TableVersion version : contentIdBasedTableVersions) {
			version.purgeVersions(contentId, versionTimestamp);
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			TableVersion niceUrlVersion = getAlternateUrlTableVersion();
			niceUrlVersion.purgeVersions(pageId, versionTimestamp);
		}

		// remove the nodeversion entries
		DBUtils.executeUpdate("DELETE FROM nodeversion WHERE o_type = ? AND o_id = ? AND timestamp < ?",
				new Object[] { Page.TYPE_PAGE, pageId, versionTimestamp });
	}

	/**
	 * Get a list of {@link TableVersion} instances for all tables, for which the records can be identified by the content_id
	 * @param singleTag true if instances are used for a single tag, false if used for all tags in the page
	 * @return list of TableVersion instances
	 * @throws NodeException
	 */
	private static List<TableVersion> getContentIdBasedTableVersions(boolean singleTag) throws NodeException {
		return Arrays.asList(getContentTagTableVersion(singleTag), getValueTableVersion(singleTag), getDSTableVersion(singleTag),
				getDSObjTableVersion(singleTag), getDatasourceTableVersion(singleTag), getDatasourceValueTableVersion(singleTag));
	}

	/**
	 * Get the table version object for values (all values in the page)
	 * @return table version object for values
	 * @throws NodeException
	 */
	private static TableVersion getValueTableVersion() throws NodeException {
		return getValueTableVersion(false);
	}

	/**
	 * Get the table version object for values
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for values
	 * @throws NodeException
	 */
	private static TableVersion getValueTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(Value.class).getDeletedIds(Value.class);
		FilteringTableVersion valueVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		valueVersion.setAutoIncrement(true);
		valueVersion.setTable("value");
		valueVersion.setJoin("contenttag", "id", "contenttag_id");
		if (singleTag) {
			valueVersion.setWherePart("contenttag.id = ?");
		} else {
			valueVersion.setWherePart("contenttag.content_id = ?");
		}
		return valueVersion;
	}

	/**
	 * Get the table version object for contenttags of a page
	 * @return table version object for contenttags
	 * @throws NodeException
	 */
	private static TableVersion getContentTagTableVersion() throws NodeException {
		return getContentTagTableVersion(false);
	}

	/**
	 * Get the table version object for contenttags
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for contenttags
	 * @throws NodeException
	 */
	private static TableVersion getContentTagTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(ContentTag.class).getDeletedIds(ContentTag.class);
		FilteringTableVersion contentTagVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		contentTagVersion.setAutoIncrement(true);
		contentTagVersion.setTable("contenttag");
		contentTagVersion.setJoin("construct", "id", "construct_id");
		if (singleTag) {
			contentTagVersion.setWherePart("gentics_main.id = ? AND construct.id IS NOT NULL");
		} else {
			contentTagVersion.setJoin("content", "id", "content_id");
			// Don't restore tags where the construct got deleted in the meanwhile
			contentTagVersion.setWherePart("content.id = ? AND construct.id IS NOT NULL");
		}
		return contentTagVersion;
	}

	/**
	 * Get the table version object for pages
	 * @return table version object for pages
	 * @throws NodeException
	 */
	private static TableVersion getPageTableVersion() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		TableVersion pageVersion = new TableVersion();

		pageVersion.setAutoIncrement(true);
		pageVersion.setTable("page");
		pageVersion.setWherePart("gentics_main.id = ?");
		return pageVersion;
	}

	/**
	 * Get the table version object for page_alt_url
	 * @return table version object for page_alt_url
	 * @throws NodeException
	 */
	private static TableVersion getAlternateUrlTableVersion() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		TableVersion niceUrlVersion = new TableVersion();

		niceUrlVersion.setAutoIncrement(true);
		niceUrlVersion.setTable("page_alt_url");
		niceUrlVersion.setWherePart("gentics_main.page_id = ?");
		return niceUrlVersion;
	}

	/**
	 * Get the table version object for ds'ses
	 * @return table version object for ds'ses
	 * @throws NodeException
	 */
	private static TableVersion getDSTableVersion() throws NodeException {
		return getDSTableVersion(false);
	}

	/**
	 * Get the table version object for ds'ses
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for ds'ses
	 * @throws NodeException
	 */
	private static TableVersion getDSTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(Overview.class).getDeletedIds(Overview.class);
		FilteringTableVersion dsVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		dsVersion.setAutoIncrement(true);
		dsVersion.setTable("ds");
		dsVersion.setJoin("contenttag", "id", "contenttag_id");
		if (singleTag) {
			dsVersion.setWherePart("contenttag.id = ?");
		} else {
			dsVersion.setWherePart("contenttag.content_id = ?");
		}
		return dsVersion;
	}

	/**
	 * Get the table version object for dsobj's
	 * @return table version object for dsobj's
	 * @throws NodeException
	 */
	private static TableVersion getDSObjTableVersion() throws NodeException {
		return getDSObjTableVersion(false);
	}

	/**
	 * Get the table version object for dsobj's
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for dsobj's
	 * @throws NodeException
	 */
	private static TableVersion getDSObjTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(OverviewEntry.class).getDeletedIds(OverviewEntry.class);
		FilteringTableVersion dsObjVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		dsObjVersion.setAutoIncrement(true);
		dsObjVersion.setTable("ds_obj");
		dsObjVersion.setJoin("contenttag", "id", "contenttag_id");
		if (singleTag) {
			dsObjVersion.setWherePart("contenttag.id = ?");
		} else {
			dsObjVersion.setWherePart("contenttag.content_id = ?");
		}
		return dsObjVersion;
	}

	/**
	 * Get the table version object for datasources
	 * @return table version object
	 * @throws NodeException
	 */
	private static TableVersion getDatasourceTableVersion() throws NodeException {
		return getDatasourceTableVersion(false);
	}

	/**
	 * Get a comma separated list of all part ids that belong to DatasourcePartType parts. If no such part exists, return -1
	 * @return comma separated list of DatasourcePartType parts
	 * @throws NodeException
	 */
	private static String getDatasourcePartIds() throws NodeException {
		final Set<Integer> partIds = new HashSet<Integer>();
		DBUtils.executeStatement("SELECT id FROM part WHERE type_id = 32", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					partIds.add(rs.getInt("id"));
				}
			}
		});

		if (partIds.isEmpty()) {
			return "-1";
		} else {
			return StringUtils.merge((Integer[]) partIds.toArray(new Integer[partIds.size()]), ",");
		}
	}

	/**
	 * Get the table version object for datasources
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for datasources
	 * @throws NodeException
	 */
	private static TableVersion getDatasourceTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(Datasource.class).getDeletedIds(Datasource.class);
		FilteringTableVersion datasourceVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		datasourceVersion.setAutoIncrement(true);
		datasourceVersion.setTable("datasource");

		datasourceVersion.addJoin(new TableVersion.Join("id", "value", "value_ref"));
		datasourceVersion.addJoin(new TableVersion.Join("value", "contenttag_id", "contenttag", "id"));

		if (singleTag) {
			datasourceVersion.setWherePart("value.part_id IN (" + getDatasourcePartIds() + ") AND contenttag.id = ?");
		} else {
			datasourceVersion.setWherePart("value.part_id IN (" + getDatasourcePartIds() + ") AND contenttag.content_id = ?");
		}

		return datasourceVersion;
	}

	/**
	 * Get the table version object for datasource values
	 * @return table version object for datasource values
	 * @throws NodeException
	 */
	private static TableVersion getDatasourceValueTableVersion() throws NodeException {
		return getDatasourceValueTableVersion(false);
	}

	/**
	 * Get the table version object for datasource values
	 * @param singleTag true if instance is used for a single tag, false if used for all tags in the page
	 * @return table version object for datasource values
	 * @throws NodeException
	 */
	private static TableVersion getDatasourceValueTableVersion(boolean singleTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<Integer> deletedIds = t.getObjectFactory(DatasourceEntry.class).getDeletedIds(DatasourceEntry.class);
		FilteringTableVersion datasourceValueVersion = new FilteringTableVersion().filter(row -> deletedIds.contains(row.getInt("id")));

		datasourceValueVersion.setAutoIncrement(true);
		datasourceValueVersion.setTable("datasource_value");

		datasourceValueVersion.addJoin(new Join("datasource_id", "datasource", "id"));
		datasourceValueVersion.addJoin(new Join("datasource", "id", "value", "value_ref"));
		datasourceValueVersion.addJoin(new Join("value", "contenttag_id", "contenttag", "id"));

		if (singleTag) {
			datasourceValueVersion.setWherePart("value.part_id IN (" + getDatasourcePartIds() + ") AND contenttag.id = ?");
		} else {
			datasourceValueVersion.setWherePart("value.part_id IN (" + getDatasourcePartIds() + ") AND contenttag.content_id = ?");
		}

		return datasourceValueVersion;
	}

	/**
	 * Get the list of locked content ids
	 * @return list of locked content ids
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private static List<Integer> getLockedContentIds() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<String, Object> attributes = t.getAttributes();
		Object data = attributes.get(LOCKED_CONTENTS_IN_TRX);
		List<Integer> contentIds = null;

		if (data instanceof List) {
			contentIds = (List<Integer>) data;
		} else {
			contentIds = new Vector<Integer>();
			attributes.put(LOCKED_CONTENTS_IN_TRX, contentIds);
		}

		return contentIds;
	}

	/**
	 * Set the given content id to be locked in this transaction
	 * @param contentId content id
	 * @throws NodeException
	 */
	public static void setContentLocked(Integer contentId) throws NodeException {
		if (contentId == null) {
			return;
		}
		List<Integer> contentIds = getLockedContentIds();

		if (!contentIds.contains(contentId)) {
			contentIds.add(contentId);
		}
	}

	/**
	 * Check whether the given content id is locked in this transaction
	 * @param contentId content id
	 * @return true if it is locked, false if not
	 * @throws NodeException
	 */
	public static boolean isContentLocked(Integer contentId) throws NodeException {
		if (contentId == null) {
			return false;
		}
		List<Integer> contentIds = getLockedContentIds();

		return contentIds.contains(contentId);
	}

	/**
	 * Unset the given content id as locked
	 * @param contentId content id
	 * @throws NodeException
	 */
	public static void unsetContentLocked(Integer contentId) throws NodeException {
		if (contentId == null) {
			return;
		}
		List<Integer> contentIds = getLockedContentIds();

		contentIds.remove(contentId);
	}

	/**
	 * Load the page versions for the given page
	 * @param pageId page id
	 * @return Array of page versions
	 * @throws NodeException
	 */
	public static NodeObjectVersion[] loadPageVersions(final int pageId) throws NodeException {
		final List<NodeObjectVersion> pageVersionList = new ArrayList<NodeObjectVersion>();

		// first load all versions
		DBUtils.executeStatement(
				"SELECT id, timestamp, user_id, published, nodeversion FROM nodeversion WHERE o_type = ? AND o_id = ? ORDER BY timestamp ASC",
				new SQLExecutor() {
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, Page.TYPE_PAGE);
				stmt.setInt(2, pageId);
			}

			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();

				while (rs.next()) {
					int userId = rs.getInt("user_id");

					pageVersionList.add(
							new NodeObjectVersion(rs.getInt("id"), rs.getString("nodeversion"), (SystemUser) t.getObject(SystemUser.class, new Integer(userId)),
							new ContentNodeDate(rs.getInt("timestamp")), rs.isLast(), rs.getBoolean("published")));
				}
			}
		}, Transaction.UPDATE_STATEMENT);

		// check whether version numbers are all set
		boolean generateVersionNumbers = false;

		for (NodeObjectVersion pageVersion : pageVersionList) {
			if (pageVersion.getNumber() == null) {
				generateVersionNumbers = true;
				break;
			}
		}

		if (generateVersionNumbers) {
			// now generate version numbers if necessary
			DBUtils.executeStatement("SELECT timestamp FROM logcmd WHERE cmd_desc_id = ? AND o_type = ? AND o_id = ?", new SQLExecutor() {

				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, ActionLogger.PAGEPUB);
					stmt.setInt(2, Page.TYPE_PAGE);
					stmt.setInt(3, pageId);
				}

				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					int minorversion = 0;
					int majorversion = 0;

					Iterator<NodeObjectVersion> i = pageVersionList.iterator();

					if (!i.hasNext()) {
						// not a single page version.
						return;
					}
					NodeObjectVersion version = i.next();
					NodeObjectVersion nextVersion = null;

					while (version != null) {
						if (i.hasNext()) {
							nextVersion = (NodeObjectVersion) i.next();
						}
						minorversion++;
						while (rs.next()) {
							int timestamp = rs.getInt("timestamp");

							// iterate over all publishes to find out if current version is a major version.
							if (timestamp < version.getDate().getIntTimestamp()) {
								// multiple publish clicks for the same version ..
								logger.debug("multiple publish clicks for the same version");
							} else if (nextVersion == null || timestamp < nextVersion.getDate().getIntTimestamp()) {
								minorversion = 0;
								break;
							} else {
								// ooops, we went one too far. go back.
								rs.previous();
								break;
							}
						}
						// if minor version was reset, increase major version.
						if (minorversion == 0) {
							version.setMajor(true);
							majorversion++;
						}
						version.setNumber(majorversion + "." + minorversion);

						version = nextVersion;
						nextVersion = null;
					}
				}
			}, Transaction.UPDATE_STATEMENT);

			// store the generated version numbers back
			for (NodeObjectVersion pageVersion : pageVersionList) {
				DBUtils.executeUpdate("UPDATE nodeversion SET nodeversion = ? WHERE id = ?", new Object[] { pageVersion.getNumber(), pageVersion.getId() });
			}
		}

		// reverse list to have versions descending.
		Collections.reverse(pageVersionList);
		return (NodeObjectVersion[]) pageVersionList.toArray(new NodeObjectVersion[pageVersionList.size()]);
	}

	/**
	 * Check whether the given page is online
	 * @param page page
	 * @return true if the page is online, false if not
	 * @throws NodeException
	 */
	public static boolean isOnline(final Page page) throws NodeException {
		Transaction currentTransaction = TransactionManager.getCurrentTransaction();

		// Add as a dependency (needed e.g. for single page overviews)
		RenderType renderType = currentTransaction.getRenderType();
		if (renderType != null && !renderType.areDependenciesCleared() && renderType.doHandleDependencies()) {
			renderType.addDependency(page, "online");
		}

		// try prepared publish data
		PublishData publishData = currentTransaction.getPublishData();
		if (publishData != null) {
			return publishData.isPageOnline(page);
		}

		// try the cache:
		Object isonline = currentTransaction.getFromLevel2Cache(page, ONLINE_CACHE_KEY);
		if (isonline instanceof Boolean) {
			return ((Boolean) isonline).booleanValue();
		}

		final boolean[] result = { false};

		// this statement is deliberately declared as UPDATE_STATEMENT, so that it will get the writable connection for reading the online status.
		// the reason is: the online status might have been changed using the writable connection and we want to read the updated status
		DBUtils.executeStatement("SELECT online FROM page WHERE id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(page.getId(), 0)); // id = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					result[0] = rs.getInt("online") == OnlineStatusChange.ONLINE.code;
				}
			}
		}, Transaction.UPDATE_STATEMENT);
		currentTransaction.putIntoLevel2Cache(page, ONLINE_CACHE_KEY, result[0]);

		return result[0];
	}

	/**
	 * Check whether publishing is delayed for the given page
	 * @param page page
	 * @return true if publishing is delayed, false if not
	 * @throws NodeException
	 */
	public static boolean isPublishDelayed(final Page page) throws NodeException {
		// First try the cache:
		Object delay = TransactionManager.getCurrentTransaction().getFromLevel2Cache(page, DELAY_PUBLISH_CACHE_KEY);

		if (delay != null && delay instanceof Boolean) {
			return ((Boolean) delay).booleanValue();
		}

		// get it from DB if cache didn't work
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement st = null;
		ResultSet res = null;

		try {
			st = t.prepareStatement("SELECT delay_publish FROM page WHERE id = ?");
			st.setObject(1, page.getId());
			res = st.executeQuery();

			if (res.next()) {
				boolean value = res.getInt("delay_publish") == 1;

				TransactionManager.getCurrentTransaction().putIntoLevel2Cache(page, DELAY_PUBLISH_CACHE_KEY, new Boolean(value));
				return value;
			} else {
				throw new NodeException("Could not get delay_publish for {" + page + "}: page not found in db!");
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting delay_publish for {" + page + "}");
		} finally {
			t.closeResultSet(res);
			t.closeStatement(st);
		}
	}

	/**
	 * Get the PHP serialized language variants for the given page
	 * @param page page
	 * @return PHP serialized language variants
	 */
	public static String getLanguageVariantsPHPSerialized(Page page) {
		// this is for compatibility with GCN 3.6
		if (ObjectTransformer.getInt(page.getContentsetId(), 0) <= 0) {
			return "";
		}
		List<Page> langVars;

		try {
			langVars = page.getLanguageVariants(false);
		} catch (NodeException e) {
			logger.error("could not retrieve language variants", e);
			return "";
		}
		String ser = "a:" + langVars.size() + ":{";
		// a:2:{i:1;a:2:{i:0;s:59:"/node_head/index.php?sid=WPH45a53LB9XsXY&do=14001&live=1988";i:1;s:14:"Tagtest - Page";}
		// i:2;a:2:{i:0;s:59:"/node_head/index.php?sid=WPH45a53LB9XsXY&do=14001&live=2134";i:1;s:19:"Tagtest - Page [en]";}}
		String encoding = "UTF-8";

		try {
			StackResolver stack = TransactionManager.getCurrentTransaction().getRenderType().getStack();
			Object obj = stack.getRootObject();
			Node node = null;

			if (obj instanceof Page) {
				node = ((Page) obj).getFolder().getNode();
			} else if (obj instanceof Folder) {
				node = ((Folder) obj).getNode();
			} else if (obj instanceof ContentFile) {
				node = ((ContentFile) obj).getFolder().getNode();
			}
			if (node != null) {
				if (!node.isUtf8()) {
					encoding = FilePublisher.getNonUTF8Encoding().name();
					logger.debug("Using the following encoding {" + encoding + "}");
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to get node for root object {" + obj.getClass().getName() + "}");
			}
		} catch (Exception e) {
			logger.error("Error while fetching root object from stack to retrieve correct encoding.", e);
		}
		for (Iterator<Page> iter = langVars.iterator(); iter.hasNext();) {
			Page p = iter.next();
			String url = p.get("url").toString();
			String name = p.getName();

			try {
				ser += "i:" + p.get("sprach_id") + ";a:2:{i:0;s:" + url.getBytes(encoding).length + ":\"" + url + "\";i:1;s:"
						+ name.getBytes(encoding).length + ":\"" + name + "\";}";
			} catch (Exception e) {
				logger.error("Error while generating serialized php", e);
				return "";
			}
		}
		ser += "}";
		return ser;
	}

	/**
	 * Check whether the page's proposed name is available
	 * @param page page with proposed name
	 * @return the other object using the name or null if the name is available
	 * @throws NodeException
	 */
	public static NodeObject isNameAvailable(Page page) throws NodeException {
		if (page == null) {
			throw new NodeException("Cannot check name availability without page");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(page, false);
		return DisinheritUtils.getObjectUsingName(page, objectSegment);
	}

	/**
	 * Check whether the page's proposed filename is available
	 * @param page page with proposed filename
	 * @return the other object using the filename or null if the filename is available
	 * @throws NodeException
	 */
	public static NodeObject isFilenameAvailable(Page page) throws NodeException {
		if (page == null) {
			throw new NodeException("Cannot check filename availability without page");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(page, false);
		Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(page.getFolder(), objectSegment);
		return DisinheritUtils.getObjectUsingFilename(page, pcf, null);
	}

	/**
	 * Check whether the page's proposed nice URL is available
	 * @param page page for which the nice URL is proposed
	 * @param niceUrl proposed nice URL
	 * @return first found conflicting object or null if the nice URL is available
	 * @throws NodeException
	 */
	public static NodeObject isNiceUrlAvailable(Page page, String niceUrl) throws NodeException {
		if (page == null) {
			throw new NodeException("Cannot check nice URL availability without page");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(page, false);
		Set<Folder> pcf = DisinheritUtils
				.getFoldersWithPotentialObstructors(NodeObjectWithAlternateUrls.PATH.apply(niceUrl), objectSegment);
		return DisinheritUtils.getObjectUsingNiceURL(page, niceUrl, pcf, null);
	}

	/**
	 * Check invalid links due to pages being deleted or going offline
	 * @param pageIds list of page IDs that are deleted or go offline
	 * @param deletedPages map containing deleted pages (may be empty or null)
	 * @param action i18n String for the action
	 * @throws NodeException
	 */
	public static void checkInvalidLinks(final List<Integer> pageIds, final Map<Integer, Page> deletedPages, final CNI18nString action) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.INVALIDPAGEURLMSG)) {
			return;
		}
		final boolean forceNode2 = t.getNodeConfig().getDefaultPreferences().getFeature("force_node2");

		// send messages to the editors of pages and templates which will have invalid references after deleting the pages
		String sqlPt1 = "SELECT v.value_ref as id, ct.content_id, ct.name FROM contenttag ct, value v, part p WHERE ct.id = v.contenttag_id AND v.part_id = p.id AND p.type_id = 4 AND  v.value_ref IN ";
		String sqlPt2 = " GROUP BY ct.content_id, ct.name";
		final MessageSender messageSender = new MessageSender();

		t.addTransactional(messageSender);
		DBUtils.executeMassStatement(sqlPt1, sqlPt2, pageIds, 1, new SQLExecutor() {
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					final int targetPageId = rs.getInt("id");
					final int contentId = rs.getInt("content_id");
					final String tagName = rs.getString("name");

					// potential performance speedup possible with using a batch statement
					String sql = "SELECT page.id AS pid, folder.id AS fid, page.name AS pname, folder.name AS fname, nodefolder.name AS nname, page.editor "
							+ "FROM page, folder, folder AS nodefolder WHERE " + "page.folder_id = folder.id"
							+ " AND page.deleted = 0" + " AND page.online = 1 AND nodefolder.node_id = folder.node_id"
							+ " AND nodefolder.mother = 0" + " AND content_id = ?";

					DBUtils.executeStatement(sql, new SQLExecutor() {
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, contentId);
						}

						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							CNI18nString message = null;

							while (rs.next()) {
								try (LangTrx lTrx = new LangTrx(rs.getInt("editor"))) {
									if (forceNode2) {
										message = new CNI18nString("notification.linkfrompage.invalid.atleastone");
										message.addParameters(
												new String[] {
											rs.getString("pname"), rs.getString("pid"), rs.getString("fname"), rs.getString("nname"), rs.getString("name"),
											action.toString()
										});
									} else {
										int linkPageId = rs.getInt("pid");
										Page linkPage = t.getObject(Page.class, linkPageId, -1, false);
										Page targetPage = null;

										if (linkPage == null) {
											continue;
										}

										if (deletedPages != null) {
											targetPage = deletedPages.get(targetPageId);

											if (targetPage == null) {
												targetPage = t.getObject(Page.class, targetPageId);
												NodeLogger.getNodeLogger(PageFactory.class).warn(
														"Could not find deleted page {" + targetPageId + "} in the map, getting from DB.");
											}
										} else {
											targetPage = t.getObject(Page.class, targetPageId);
										}

										if (targetPage != null && !targetPage.isDeleted()) {
											// check multichannelling visibility
											Node linkPageNode = linkPage.getChannel() != null ? linkPage.getChannel() : linkPage.getNode();
											// if the target page was not visible from the link page's channel, we do not need to send messages
											// (nothing really changes for the link page)
											Page channelVariantTargetPage = MultichannellingFactory.getChannelVariant(targetPage, linkPageNode);
											if (channelVariantTargetPage == null) {
												continue;
											}
											if (!channelVariantTargetPage.equals(targetPage)
													&& (deletedPages == null || !deletedPages.containsValue(channelVariantTargetPage))) {
												continue;
											}

											String linkQualifiedName = "";
											String targetQualifiedName = "";

											try (ChannelTrx trx = new ChannelTrx(linkPage.getChannel())) {
												linkQualifiedName = linkPage.getQualifiedName();
											}

											try (ChannelTrx trx = new ChannelTrx(targetPage.getChannel())) {
												targetQualifiedName = targetPage.getQualifiedName();
											}

											message = new CNI18nString("notification.linkfrompage.invalid");
											message.addParameters(new String[] {
												tagName,
												linkQualifiedName,
												String.valueOf(linkPageId),
												targetQualifiedName,
												String.valueOf(targetPageId),
												action.toString()
											});
										}
									}

									if (message != null) {
										messageSender.sendMessage(new Message(t.getUserId(), rs.getInt("editor"), message.toString()));
									}
								}
							}
						}
					});
				}
			}
		});
	}

	/**
	 * Get an editable clone of the page, without locking the page. This is used to render a preview of a page, where the preview data is posted (as REST Model)
	 * @param page page
	 * @return editable clone
	 * @throws NodeException
	 */
	public static Page getEditableClone(Page page) throws NodeException {
		EditableFactoryPage clone = (EditableFactoryPage)page.copy();
		clone.setId(page.getId());
		return clone;
	}

	/**
	 * Suggest a filename for the page
	 * @param page page
	 * @return suggested filename (not necessarily unique)
	 * @throws NodeException
	 */
	public static String suggestFilename(Page page) throws NodeException {
		return suggestFilename(
			page,
			p -> p.getOwningNode().isOmitPageExtension() ? "" : p.getTemplate().getMarkupLanguage().getExtension());
	}

	/**
	 * Suggest a filename for the page
	 * @param page page
	 * @param extensionProvider provider for the extension to be used
	 * @return suggested filename (not necessarily unique)
	 * @throws NodeException
	 */
	public static String suggestFilename(Page page, Function<Page, String> extensionProvider) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		Map<String, String> sanitizeCharacters = prefs.getPropertyMap("sanitize_character");
		String replacementCharacter = prefs.getProperty("sanitize_replacement_character");
		String[] preservedCharacters = prefs.getProperties("sanitize_allowed_characters");

		// first get the extension from the extension provider
		String ext = extensionProvider.apply(page);

		String fileName = null;

		if (prefs.isFeature(Feature.GET_FILENAME_AS_PAGENAME)) {
			// get current name
			fileName = ObjectTransformer.getString(page.getName(), "");
			// make the filename lowercase
			if (prefs.isFeature(Feature.FILENAME_FORCETOLOWER)) {
				fileName = fileName.toLowerCase();
			}
			// convert the filename into a suitable one
			// to be safe: add the known extension
			if (!StringUtils.isEmpty(ext)) {
				fileName = fileName + "." + ext;
			}
			fileName = FileUtil.sanitizeName(fileName, ext, sanitizeCharacters, replacementCharacter, preservedCharacters);
			int extensionSize = 1 + ext.length();
			if (fileName.length() > extensionSize && fileName.endsWith("." + ext)) {
				fileName = fileName.substring(0, fileName.length() - extensionSize);
			}

			// calculate the allowed length of the new filename (excluding
			// extension)
			// the total filename must not exceed 64 characters, save one for
			// the dot, the length of the extension and another 2 characters for
			// making the filename unique later (which would add numbers)
			int newFileNameLength = 64 - 1 - ext.length() - 2;

			if (fileName.length() > newFileNameLength) {
				fileName = fileName.substring(0, newFileNameLength);
			}
			// if filename is empty now, make it equal to the page id
			if (StringUtils.isEmpty(fileName)) {
				fileName = ObjectTransformer.getString(page.getId(), "");
			}
			// add the extension
			if (!StringUtils.isEmpty(ext)) {
				fileName += "." + ext;
			}
		} else {
			fileName = ObjectTransformer.getString(page.getId(), "");
			if (!StringUtils.isEmpty(ext)) {
				fileName += "." + ext;
			}
		}

		// eventually add the language code to the filename
		if (prefs.isFeature(Feature.CONTENTGROUP3_PAGEFILENAME)
				&& page.getLanguage() != null
				&& page.getOwningNode().getPageLanguageCode() == PageLanguageCode.FILENAME) {
			if (prefs.isFeature(Feature.CONTENTGROUP3_PAGEFILENAME_NO_APACHEFILENAME)) {
				Matcher m = Pattern.compile("(.*)\\.([^\\.]*)").matcher(fileName);

				if (m.matches()) {
					String tempName = m.group(1);
					String tempExt = m.group(2);
					// calculate the new maximum for the filename length: the
					// resulting filename must not be longer than 62 characters,
					// may contain up to 4 additional characters (2 for dots
					// between filename, languagecode and extension, 2 for
					// numbers to make the name unique) and will contain the
					// language code and the extension
					int newFileNameLength = 62 - 4 - page.getLanguage().getCode().length() - tempExt.length();

					// construct the new total filename: cut the base filename
					// if necessary and add language code and extension
					fileName = tempName.substring(0, Math.min(newFileNameLength, tempName.length())) + "." + page.getLanguage().getCode() + "." + tempExt;
				} else {
					fileName += "." + page.getLanguage().getCode();
				}
			} else {
				fileName += "." + page.getLanguage().getCode();
			}
		}

		return fileName;
	}

	/**
	 * Get map of systemusers to permhandler (for the systemuser) containing all systemusers in supergroups of the current user's groups,
	 * that have publish permission on the page. From the user's groups only those granting edit permission on the page are considered
	 * @param page page
	 * @return map of publishers
	 * @throws NodeException
	 */
	public static Map<SystemUser, PermHandler> getPublishers(Page page) throws NodeException {
		Node nodeForPermission = page.getChannel();
		if (nodeForPermission == null) {
			nodeForPermission = page.getOwningNode();
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.getObject(SystemUser.class, t.getUserId());

		List<UserGroup> groups = new ArrayList<>(user.getUserGroups(nodeForPermission));

		for (Iterator<UserGroup> i = groups.iterator(); i.hasNext();) {
			UserGroup group = i.next();
			PermHandler handlerForGroup = new PermHandler();

			handlerForGroup.initForGroup(ObjectTransformer.getInt(group.getId(), -1));
			if (!handlerForGroup.canEdit(page)) {
				i.remove();
			}
		}

		// now get all super groups with permission to publish the page
		List<UserGroup> parents = new ArrayList<>();

		for (UserGroup group : groups) {
			for (UserGroup parent : group.getParents()) {
				if (!parents.contains(parent)) {
					parents.add(parent);
				}
			}
		}
		// filter by permission
		for (Iterator<UserGroup> i = parents.iterator(); i.hasNext();) {
			UserGroup parent = i.next();
			PermHandler handlerForGroup = new PermHandler();

			handlerForGroup.initForGroup(ObjectTransformer.getInt(parent.getId(), -1));
			if (!handlerForGroup.canPublish(page)) {
				i.remove();
			}
		}

		// reduce the list to never contain a group together with one of
		// its parents
		parents = UserGroup.reduceUserGroups(parents, ReductionType.CHILD);

		// finally, get all users of the parents
		Map<SystemUser, PermHandler> parentUsers = new HashMap<>();

		for (UserGroup parent : parents) {
			List<SystemUser> members = parent.getMembers();

			for (SystemUser member : members) {
				if (!parentUsers.containsKey(member)) {
					PermHandler permHandler = new PermHandler();
					permHandler.initForUser(ObjectTransformer.getInt(member.getId(), 0));
					parentUsers.put(member, permHandler);
				}
			}
		}

		return parentUsers;
	}

	/**
	 * Recalculate the "modified" flag for the page. If the flag is changed, this is persistet in the DB
	 * @param page page
	 * @return true iff the "modified" flag changed
	 * @throws NodeException
	 */
	public static boolean recalculateModifiedFlag(Page page) throws NodeException {
		if (page instanceof FactoryPage) {
			return ((FactoryPage) page).recalculateModifiedFlag();
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#batchLoadVersionedObjects(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> Set<T> batchLoadVersionedObjects(FactoryHandle handle, Class<T> clazz, Class<? extends NodeObject> mainClazz, Map<Integer, Integer> timestamps) throws NodeException {
		if (Page.class.equals(clazz)) {
			Set<T> preparedPages = new HashSet<T>();
			Map<Integer, Integer> contentTimestamps = new HashMap<Integer, Integer>(timestamps.size());
			List<Integer> ids = new ArrayList<Integer>(timestamps.size());
			ids.addAll(timestamps.keySet());
			final Map<Integer, FactoryDataRow> currentData = new HashMap<Integer, FactoryDataRow>();

			// get the non versioned data
			DBUtils.executeMassStatement(BATCHLOAD_PAGE_SQL, ids, 1, new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						currentData.put(rs.getInt("id"), new FactoryDataRow(rs));
					}
				}
			});

			Map<Integer, Map<Integer, FactoryDataRow>> versionedData = getVersionedData("SELECT *, id gentics_obj_id FROM page_nodeversion WHERE id IN", timestamps);
			for (Map.Entry<Integer, Map<Integer, FactoryDataRow>> rowMapEntry : versionedData.entrySet()) {
				Integer objId = rowMapEntry.getKey();
				int versionTimestamp = timestamps.get(objId);
				Map<Integer, FactoryDataRow> rowMap = rowMapEntry.getValue();
				for (Map.Entry<Integer, FactoryDataRow> entry : rowMap.entrySet()) {
					FactoryDataRow row = entry.getValue();
					FactoryDataRow currentRow = currentData.get(objId);
					FactoryDataRow combined = null;
					if (currentRow != null) {
						combined = currentRow;
						combined.getValues().putAll(row.getValues());
					} else {
						combined = row;
					}
					try {
						Page page = loadResultSet(Page.class, objId, handle.createObjectInfo(Page.class, versionTimestamp), combined, null);
						preparedPages.add((T)page);
						handle.putObject(Page.class, page, versionTimestamp);

						// when we happen to have page variants, we might overwrite the content_id with different version timestamps here.
						// the result will be, that the versioned data of only one of the page variants will be prepared
						contentTimestamps.put(combined.getInt("content_id"), versionTimestamp);
					} catch (SQLException e) {
						throw new NodeException("Error while batchloading pages", e);
					}
				}
			}
			// now continue preparing the data for the contenttags
			Transaction t = TransactionManager.getCurrentTransaction();
			t.prepareVersionedObjects(ContentTag.class, Content.class, contentTimestamps);

			return preparedPages;
		} else {
			return Collections.emptySet();
		}
	}

}
