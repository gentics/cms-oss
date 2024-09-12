package com.gentics.contentnode.publish.wrapper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.object.ExtensiblePublishableObjectService;
import com.gentics.contentnode.object.AbstractPage;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.publish.PublishablePageStatistics;
import com.gentics.contentnode.publish.PublishablePageStatistics.Item;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.response.DiffResponse;
import com.gentics.contentnode.rest.resource.DiffResource;
import com.gentics.contentnode.rest.resource.impl.DiffResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of {@link Page} that wraps the REST model of a page.
 * The REST model contains all data that make up the content of a page (including the object tags).
 * When instances of this class are used for publishing, gathering the data for rendering is much more efficient.
 * Instances of this class will reflect the published version of a page and will be used, if
 * <ol>
 * <li>versioned publishing is used</li>
 * <li>multithreaded publishing is used.</li>
 * </ol>
 * 
 * The REST models will be created when a page is published (by an editor) and cached.
 */
public class PublishablePage extends AbstractPage {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4517998991139169361L;

	/**
	 * Name of the cache region
	 */
	public final static String CACHEREGION = "gentics-publishable-objects";

	/**
	 * Cache instance
	 */
	public static PortalCache cache;

	/**
	 * Logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(PublishablePage.class);

	/**
	 * Statistics
	 */
	protected static PublishablePageStatistics stats;

	/**
	 * Filled references
	 */
	protected final static Collection<Reference> fillRefs = Arrays.asList(Reference.CONTENT_TAGS, Reference.OBJECT_TAGS, Reference.DISINHERITED_CHANNELS);

	static {
		try {
			cache = PortalCache.getCache(CACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing cache for region {" + CACHEREGION + "}, will not use object cache", e);
		}
	}

	/**
	 * Get the cached instance or null if not cached
	 * @param pageId page id
	 * @return page model
	 * @throws NodeException
	 */
	protected static PublishablePage getFromCache(int pageId) throws NodeException {
		PublishablePage cachedInstance = null;
		if (cache != null) {
			if (stats != null) {
				stats.get(Item.CACHE).start();
			}
			try {
				Object cachedObject = cache.get(pageId);
				if (cachedObject instanceof PublishablePage) {
					cachedInstance = (PublishablePage)cachedObject;
				} else if (cachedObject instanceof com.gentics.contentnode.rest.model.Page) {
					if (stats != null) {
						stats.get(Item.CREATE).start();
					}
					try {
						cachedInstance = new PublishablePage(pageId, (com.gentics.contentnode.rest.model.Page)cachedObject);
					} finally {
						if (stats != null) {
							stats.get(Item.CREATE).stop();
						}
					}
					if (stats != null) {
						stats.get(Item.PUT).start();
					}
					try {
						cache.put(pageId, cachedInstance);
					} finally {
						if (stats != null) {
							stats.get(Item.PUT).stop();
						}
					}
				}
			} catch (PortalCacheException e) {
			} finally {
				if (stats != null) {
					stats.get(Item.CACHE).stop();
				}
			}
		}

		return cachedInstance;
	}

	/**
	 * Transform the given page model to JSON notation
	 * @param page given page
	 * @return JSON notation
	 * @throws Exception
	 */
	protected static String getJSON(com.gentics.contentnode.rest.model.Page page) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		StringWriter sw = new StringWriter();

		JsonGenerator jg = new JsonFactory().createJsonGenerator(sw);

		jg.useDefaultPrettyPrinter();

		// let the tags be sorted
		page.setTags(new TreeMap<String, Tag>(page.getTags()));
		for (Tag tag : page.getTags().values()) {
			tag.setProperties(new TreeMap<String, com.gentics.contentnode.rest.model.Property>(tag.getProperties()));
		}
		mapper.writeValue(jg, page);

		return sw.toString();
	}

	/**
	 * Clear the cache
	 */
	public static void clearCache() {
		if (cache != null) {
			try {
				cache.clear();
			} catch (PortalCacheException e) {
			}
		}
	}

	/**
	 * Remove the given page from the cache
	 * @param pageId page id
	 */
	public static void removeFromCache(int pageId) {
		if (cache != null) {
			try {
				cache.remove(pageId);
			} catch (PortalCacheException e) {
			}
		}
	}

	/**
	 * Get an instance of the publishable page. First try the cache, if not found in the cache, put it there
	 * @param pageId page id
	 * @return publishable page or null if the page does not exist or is not published
	 * @throws NodeException
	 */
	public static PublishablePage getInstance(int pageId) throws NodeException {
		return getInstance(pageId, false);
	}

	/**
	 * Get an instance of the publishable page. First try the cache, if not found in the cache, put it there
	 * @param pageId page id
	 * @return publishable page or null if the page does not exist or is not published
	 * @throws NodeException
	 */
	public static PublishablePage getInstance(int pageId, boolean ignoreCache) throws NodeException {
		if (stats != null) {
			stats.get(Item.GET).start();
		}
		try {
			PublishablePage instance = null;
			if (!ignoreCache) {
				instance = getFromCache(pageId);
			}
			if (instance == null) {
				com.gentics.contentnode.rest.model.Page model = createModel(pageId);
				if (model != null) {
					if (stats != null) {
						stats.get(Item.CREATE).start();
					}
					try {
						instance = new PublishablePage(pageId, model);
					} finally {
						if (stats != null) {
							stats.get(Item.CREATE).stop();
						}
					}
					if (cache != null) {
						if (stats != null) {
							stats.get(Item.PUT).start();
						}
						try {
							cache.put(pageId, instance);
						} catch (PortalCacheException e) {
						} finally {
							if (stats != null) {
								stats.get(Item.PUT).stop();
							}
						}
					}
				}
			}

			return instance;
		} finally {
			if (stats != null) {
				stats.get(Item.GET).stop();
			}
		}
	}

	/**
	 * Create the model and put it into the cache
	 * @param pageId page id
	 * @return model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Page createModel(int pageId) throws NodeException {
		if (stats != null) {
			stats.get(Item.MODEL).start();
		}

		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.rest.model.Page model = null;

			t.setChannelId(null);
			boolean versionedPublishDisabled = t.isDisableVersionedPublish();
			t.setDisableVersionedPublish(true);
			Page publishedPage = null;
			try (HandleDependenciesTrx depTrx = new HandleDependenciesTrx(false)) {
				publishedPage = t.getObject(Page.class, pageId, -1, false);
				if (publishedPage == null) {
					return null;
				}
				NodeObjectVersion version = publishedPage.getPublishedVersion();
				if (version != null) {
					Map<Integer, Integer> timestamps = new HashMap<Integer, Integer>(1);
					timestamps.put(pageId, version.getDate().getIntTimestamp());
					t.prepareVersionedObjects(Page.class, Page.class, timestamps);
					publishedPage = t.getObject(Page.class, pageId, version.getDate().getIntTimestamp(), false);
				}
				// sometimes, the published page may be null, even though the page returned a published version.
				if (publishedPage == null) {
					return null;
				}
				model = ModelBuilder.getPage(publishedPage, fillRefs);
				model.setPublishedVersion(ModelBuilder.getPageVersion(publishedPage.getPublishedVersion()));
			} finally {
				t.resetChannel();
				t.setDisableVersionedPublish(versionedPublishDisabled);
			}

			return model;
		} finally {
			if (stats != null) {
				stats.get(Item.MODEL).stop();
			}
		}
	}

	/**
	 * Enable/disable statistics
	 * @param enable true to enable, false to disable
	 */
	public static void enableStatistics(boolean enable) {
		if (enable && stats == null) {
			stats = new PublishablePageStatistics();
		} else if (!enable) {
			stats = null;
		}
	}

	/**
	 * Get the stats item, if enabled
	 * @return stats item or null
	 */
	public static PublishablePageStatistics getStatistics() {
		return stats;
	}

	/**
	 * Check validity of the PublishCache for the given page
	 * @param pageId page id
	 * @param diff stringbuilder, that will get the diff appended, may be null
	 * @return true if the publish cache is valid, false if not
	 * @throws EntityNotFoundException if either the page was not found (is not published) or the page is not cached
	 * @throws NodeException
	 */
	public static boolean check(int pageId, StringBuilder diff) throws EntityNotFoundException, NodeException {
		if (cache == null) {
			throw new NodeException("Cache not initialized");
		}
		com.gentics.contentnode.rest.model.Page currentModel = createModel(pageId);
		if (currentModel == null) {
			throw new EntityNotFoundException("Page " + pageId + " not found. Maybe page is not published.");
		}
		try {
			PublishablePage cachedInstance = getFromCache(pageId);
			if (cachedInstance != null) {
				String content1 = getJSON(currentModel);
				String content2 = getJSON(cachedInstance.wrappedPage);
				if (StringUtils.isEqual(content1, content2)) {
					return true;
				} else {
					if (diff != null) {
						DiffResource diffResource = new DiffResourceImpl();
						DiffRequest req = new DiffRequest();
						req.setContent1(content1);
						req.setContent2(content2);
						DiffResponse resp = diffResource.diffSource(req);
						diff.append(resp.getDiff());
					}
					return false;
				}
			} else {
				throw new EntityNotFoundException("Page " + pageId + " was not found in cache");
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException("Error while checking publish cache", e);
		}
	}

	/**
	 * Wrapped rest model
	 */
	protected com.gentics.contentnode.rest.model.Page wrappedPage;

	/**
	 * Content wrapper
	 */
	protected PublishableContent content;

	/**
	 * creation date
	 */
	protected ContentNodeDate cDate;

	/**
	 * Custom creation date
	 */
	protected ContentNodeDate customCDate;

	/**
	 * edit date
	 */
	protected ContentNodeDate eDate;

	/**
	 * Custom edit date
	 */
	protected ContentNodeDate customEDate;

	/**
	 * publish date
	 */
	protected ContentNodeDate pDate;

	/**
	 * unpublish date
	 */
	protected ContentNodeDate unpublishedDate;

	/**
	 * Map of object tags
	 */
	protected Map<String, ObjectTag> objectTags;

	/**
	 * Map of content tags
	 */
	protected Map<String, ContentTag> contentTags;

	/**
	 * Channel of the page
	 */
	protected transient Node channel;

	/**
	 * Create an instance
	 * @param id
	 * @param info
	 * @param wrappedPage
	 */
	protected PublishablePage(Integer id, com.gentics.contentnode.rest.model.Page wrappedPage) throws NodeException {
		super(id, null);
		this.wrappedPage = wrappedPage;
		this.content = new PublishableContent(wrappedPage.getContentId());
		cDate = new ContentNodeDate(wrappedPage.getCdate());
		customCDate = new ContentNodeDate(ObjectTransformer.getInt(wrappedPage.getCustomCdate(), cDate.getIntTimestamp()));
		eDate = new ContentNodeDate(wrappedPage.getEdate());
		customEDate = new ContentNodeDate(ObjectTransformer.getInt(wrappedPage.getCustomEdate(), eDate.getIntTimestamp()));
		pDate = new ContentNodeDate(wrappedPage.getPdate());

		// make objectTags and contentTags
		objectTags = new HashMap<String, ObjectTag>();
		contentTags = new HashMap<String, ContentTag>();
		for (Map.Entry<String, Tag> tagEntry : wrappedPage.getTags().entrySet()) {
			switch(tagEntry.getValue().getType()) {
			case CONTENTTAG:
				contentTags.put(tagEntry.getKey(),
						new PublishableContentTag(tagEntry.getValue()));
				break;
			case OBJECTTAG:
				String name = tagEntry.getKey();
				if (name.startsWith("object.")) {
					name = name.substring(7);
				}
				objectTags.put(name, new PublishableObjectTag(tagEntry.getValue(), this));
				break;
			default:
				break;
			}
		}
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		if (info == null) {
			int versionTimestamp = -1;
			com.gentics.contentnode.rest.model.PageVersion publishedVersion = wrappedPage.getPublishedVersion();
			if (publishedVersion != null) {
				versionTimestamp = publishedVersion.getTimestamp();
			}
			info = new PublishableNodeObjectInfo(Page.class, versionTimestamp);
		}

		return info;
	}

	@Override
	public GlobalId getGlobalId() {
		return new GlobalId(wrappedPage.getGlobalId());
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId()
	 */
	public Integer getChannelSetId() throws NodeException {
		return wrappedPage.getChannelSetId();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getOwningNode()
	 */
	public Node getOwningNode() throws NodeException {
		try (NoMcTrx noMc = new NoMcTrx()) {
		return getFolder().getOwningNode();
	}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	public NodeObject copy() throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public OpResult move(Folder target, int targetChannelId, boolean allLanguages) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	protected boolean canDeleteContent() throws NodeException {
		failReadOnly();
		return false;
	}

	@Override
	protected void performDelete() throws NodeException {
		failReadOnly();
	}

	@Override
	protected void putIntoWastebin() throws NodeException {
		failReadOnly();
	}

	@Override
	public Template getTemplate() throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Template.class, wrappedPage.getTemplateId());
	}

	@Override
	public String getName() {
		return wrappedPage.getName();
	}

	@Override
	public String setName(String name) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public String getNiceUrl() {
		return wrappedPage.getNiceUrl();
	}

	@Override
	public String getDescription() {
		return wrappedPage.getDescription();
	}

	@Override
	public String setDescription(String description) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public String getFilename() {
		return wrappedPage.getFileName();
	}

	@Override
	public String setFilename(String filename) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public int getPriority() {
		return wrappedPage.getPriority();
	}

	@Override
	public int setPriority(int priority) throws ReadOnlyException {
		failReadOnly();
		return 0;
	}

	@Override
	public boolean isOnline() throws NodeException {
		return PageFactory.isOnline(this);
	}

	@Override
	public boolean isModified() throws NodeException {
		return false;
	}

	@Override
	public boolean isPublishDelayed() throws NodeException {
		return PageFactory.isPublishDelayed(this);
	}

	@Override
	public Folder getFolder() throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Folder.class, getFolderId());
	}

	@Override
	public Integer getFolderId() throws NodeException {
		return getPublishData().getFolderId(this);
	}

	@Override
	public Content getContent() throws NodeException {
		return content;
	}

	@Override
	public SystemUser getCreator() throws NodeException {
		return getSystemUser(wrappedPage.getCreator());
	}

	@Override
	public SystemUser getEditor() throws NodeException {
		return getSystemUser(wrappedPage.getEditor());
	}

	@Override
	public SystemUser getPublisher() throws NodeException {
		return getSystemUser(wrappedPage.getPublisher());
	}

	@Override
	public SystemUser getUnpublisher() throws NodeException {
		return null;
	}


	/**
	 * Get the given user as SystemUser
	 * @param user user
	 * @return SystemUser or null
	 * @throws NodeException
	 */
	protected SystemUser getSystemUser(User user) throws NodeException {
		if (user != null) {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, user.getId());
		} else {
			return null;
		}
	}

	@Override
	public List<Page> getLanguageVariants(boolean considerNodeLanguages) throws NodeException {
		Integer nodeId = null;
		if (considerNodeLanguages) {
			Node node = getFolder().getNode();
			nodeId = ObjectTransformer.getInteger(node.getId(), 0);
		}
		return getPublishData().getLanguageVariants(this, nodeId);
	}

	@Override
	public Page getLanguageVariant(String code, Integer nodeId) throws NodeException {
		if (ObjectTransformer.isEmpty(code)) {
			return null;
		}
		List<Page> languageVariants = getPublishData().getLanguageVariants(this, ObjectTransformer.getInteger(nodeId, null));
		for (Page langVar : languageVariants) {
			if (code.equals(langVar.getLanguage().getCode())) {
				return langVar;
			}
		}

		return null;
	}

	@Override
	public Page getLanguageVariant(String code) throws NodeException {
		return getLanguageVariant(code, null);
	}

	@Override
	public ContentLanguage getLanguage() throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(ContentLanguage.class, wrappedPage.getContentGroupId());
	}

	@Override
	public ContentLanguage setLanguage(ContentLanguage language) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public Integer getLanguageId() {
		return wrappedPage.getContentGroupId();
	}

	@Override
	public Integer setLanguageId(Integer languageId) throws ReadOnlyException {
		failReadOnly();
		return null;
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

	@Override
	public ContentNodeDate getPDate() {
		return pDate;
	}

	@Override
	public ContentNodeDate getUnpublishedDate() {
		return unpublishedDate;
	}

	@Override
	public String getLanguageVariantsPHPSerialized() {
		return PageFactory.getLanguageVariantsPHPSerialized(this);
	}

	@Override
	public Set<Integer> getModifiedContenttags(int firsttimestamp, int lasttimestamp) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public int getLastPublishedVersion(String[] property) throws NodeException {
		failReadOnly();
		return 0;
	}

	@Override
	public boolean dirtPage(int channelId) throws NodeException {
		failReadOnly();
		return false;
	}

	@Override
	public boolean handleTimemanagement() throws NodeException {
		failReadOnly();
		return false;
	}

	@Override
	public ContentNodeDate getTimePub() {
		return new ContentNodeDate(wrappedPage.getTimeManagement().getAt());
	}

	@Override
	public NodeObjectVersion getTimePubVersion() {
		// TODO
		return null;
	}

	@Override
	public SystemUser getPubQueueUser() throws NodeException {
		return null;
	}

	@Override
	public ContentNodeDate getTimePubQueue() {
		return new ContentNodeDate(0);
	}

	@Override
	public NodeObjectVersion getTimePubVersionQueue() throws NodeException {
		return null;
	}

	@Override
	public ContentNodeDate getTimeOff() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SystemUser getOffQueueUser() throws NodeException {
		return null;
	}

	@Override
	public ContentNodeDate getTimeOffQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearTimePub() throws NodeException {
		failReadOnly();
	}

	@Override
	public void clearTimeOff() throws NodeException {
		failReadOnly();
	}

	@Override
	public void clearQueue() throws NodeException {
		failReadOnly();
	}

	@Override
	public Integer getContentsetId() {
		return wrappedPage.getContentSetId();
	}

	@Override
	public NodeObjectVersion getVersion() throws NodeException {
		com.gentics.contentnode.rest.model.PageVersion restVersion = wrappedPage.getPublishedVersion();
		if (restVersion != null) {
			SystemUser editor = null;
			if (restVersion.getEditor() != null) {
				editor = TransactionManager.getCurrentTransaction().getObject(SystemUser.class,
						restVersion.getEditor().getId());
			}
			return new NodeObjectVersion(0, restVersion.getNumber(), editor, new ContentNodeDate(restVersion.getTimestamp()), false, true);
		} else {
			return null;
		}
	}

	@Override
	public Page getPublishedObject() throws NodeException {
		return this;
	}

	@Override
	public NodeObjectVersion getPublishedVersion() throws NodeException {
		return getVersion();
	}

	@Override
	public NodeObjectVersion[] loadVersions() throws NodeException {
		return PageFactory.loadPageVersions(wrappedPage.getId());
	}

	@Override
	public void restoreVersion(NodeObjectVersion toRestore, boolean pageTable) throws NodeException {
		failReadOnly();
	}

	@Override
	public void purgeOlderVersions(NodeObjectVersion oldestKeptVersion) throws NodeException {
		failReadOnly();
	}

	/**
	 * Get the prepared PublishData or fail, if not prepared
	 * @return prepared PublishData
	 * @throws NodeException
	 */
	protected PublishData getPublishData() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PublishData publishData = t.getPublishData();
		if (publishData == null) {
			throw new NodeException("Cannot get channel information for " + this + ": PublishData must be prepared");
		}

		return publishData;
	}

	@Override
	public Map<Integer, Integer> getChannelSet() throws NodeException {
		return getPublishData().getChannelset(this);
	}

	@Override
	public Map<Integer, Integer> getHidingPageIds() throws NodeException {
		return getPublishData().getHidingPageIds(this);
	}

	@Override
	public Map<Integer, Integer> getHiddenPageIds() throws NodeException {
		return getPublishData().getHiddenPageIds(this);
	}

	@Override
	public Node getChannel() throws NodeException {
		Integer channelId = wrappedPage.getChannelId();
		if (channelId == null) {
			return null;
		}
		if (channel == null) {
			channel = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId, -1, false);
		}
		return channel;
	}

	@Override
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
		return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(wrappedPage.getChannelId(), -1);
	}

	@Override
	public boolean isMaster() throws NodeException {
		return wrappedPage.isMaster();
	}

	@Override
	public PublishWorkflow getWorkflow() throws NodeException {
		return null;
	}

	@Override
	public void publish(int timestamp, NodeObjectVersion version) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public Page createVariant(Folder folder) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public Page getSynchronizedWith() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInSync() throws NodeException {
		// TODO Auto-generated method stub
		return false;
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
	public boolean needsContenttagMigration(Template template, List<String> tagnames, boolean force) throws NodeException {
		failReadOnly();
		return false;
	}

	public Map<String, ObjectTag> getObjectTags() throws NodeException {
		return objectTags;
	}

	@Override
	public String toString() {
		return "PublishablePage {name:" + getName() + ",pageid:" + getId() + "}";
	}

	/**
	 * Wrapper implementation for a page's content.
	 */
	protected class PublishableContent extends Content {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3269967135021654665L;

		/**
		 * Create an instance
		 * @param id content id
		 */
		protected PublishableContent(Integer id) {
			super(id, null);
		}

		@Override
		public NodeObjectInfo getObjectInfo() {
			if (info == null) {
				info = PublishablePage.this.getObjectInfo().getSubInfo(Content.class);
			}

			return info;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			failReadOnly();
			return null;
		}

		@Override
		public Map<String, ContentTag> getContentTags() throws NodeException {
			return contentTags;
		}

		@Override
		public List<Page> getPages() throws NodeException {
			return getPublishData().getPageVariants(PublishablePage.this);
		}

		@Override
		public boolean isLocked() throws NodeException {
			return false;
		}

		@Override
		public ContentNodeDate getLockedSince() throws NodeException {
			return null;
		}

		@Override
		public SystemUser getLockedBy() throws NodeException {
			return null;
		}

		@Override
		protected void performDelete() throws NodeException {
			failReadOnly();
		}

		@Override
		public Node getNode() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * Wrapper implementation of a ContentTag
	 */
	protected class PublishableContentTag extends ContentTag {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -8043749843963730794L;

		/**
		 * Wrapped tag
		 */
		protected Tag tag;

		/**
		 * Value list
		 */
		protected PublishableValueList valueList;

		/**
		 * Create an instance
		 * @param tag wrapped tag
		 */
		private PublishableContentTag(Tag tag) throws NodeException {
			super(tag.getId(), null);
			this.tag = tag;
			this.valueList = new PublishableValueList(tag, this);
		}

		@Override
		public NodeObjectInfo getObjectInfo() {
			if (info == null) {
				info = PublishablePage.this.getObjectInfo().getSubInfo(ContentTag.class);
			}

			return info;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			failReadOnly();
			return null;
		}
		
		@Override
		protected Content getContent() throws NodeException {
			return content;
		}
		
		@Override
		protected void performDelete() throws NodeException {
			failReadOnly();
		}
		
		@Override
		public ValueList getValues() throws NodeException {
			valueList.checkDeletedParts();
			return valueList;
		}

		@Override
		public Construct getConstruct() throws NodeException {
			Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, tag.getConstructId());
			assertNodeObjectNotNull(construct, tag.getConstructId(), "Construct");
			return construct;
		}

		@Override
		public Integer getConstructId() throws NodeException {
			return tag.getConstructId();
		}

		@Override
		public String getName() {
			return tag.getName();
		}

		@Override
		public boolean isEnabled() {
			return tag.getActive();
		}

		@Override
		public int getEnabledValue() {
			return tag.getActive() ? 1 : 0;
		}

		@Override
		public String toString() {
			return "PublishableContentTag {" + getName() + ", " + getId() + "}";
		}

		@Override
		public boolean comesFromTemplate() {
			return false;
		}
	}

	@Override
	public PageCopyOpResult copyTo(Integer sourceNodeId, Folder targetFolder, boolean createCopy, Integer csId, Integer targetChannelId) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public Integer getSyncPageId() throws NodeException {
		return null;
	}

	@Override
	public void setSyncPageId(Integer pageId) throws NodeException {
		failReadOnly();
	}

	@Override
	public ContentNodeDate getSyncTimestamp() {
		return null;
	}

	@Override
	public void setSyncTimestamp(ContentNodeDate date) throws NodeException {
		failReadOnly();
	}

	@Override
	public boolean isExcluded() {
		return wrappedPage.isExcluded();
	}

	@Override
	public boolean isDisinheritDefault() {
		return wrappedPage.isDisinheritDefault();
	}

	@Override
	public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException {
	}

	@Override
	public Set<Node> getDisinheritedChannels() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> nodeIds = new HashSet<>();
		for (com.gentics.contentnode.rest.model.Node c : wrappedPage.getDisinheritedChannels()) {
			nodeIds.add(c.getId());
		}
		return new HashSet<>(t.getObjects(Node.class, nodeIds));
	}

	@Override
	public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException {
	}

	@Override
	public Page createVariant(Folder folder, Node channel) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public Set<String> getAlternateUrls() throws NodeException {
		return wrappedPage.getAlternateUrls();
	}

	@Override
	public void clearVersions() {
	}

	@Override
	public void folderInheritanceChanged() throws NodeException {
	}

	@Override
	public List<? extends ExtensiblePublishableObjectService<Page>> getServices() {
		return Collections.emptyList();
	}
}
