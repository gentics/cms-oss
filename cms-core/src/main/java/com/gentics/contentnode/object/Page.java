/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Page.java,v 1.73.4.6.2.7 2011-03-23 14:55:06 johannes2 Exp $
 */
package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.ContentLanguageTrx;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.perm.CreatePermType;
import com.gentics.contentnode.factory.perm.DeletePermType;
import com.gentics.contentnode.factory.perm.EditPermType;
import com.gentics.contentnode.factory.perm.PublishPermType;
import com.gentics.contentnode.factory.perm.ViewPermType;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * This is the page object of the object layer.
 */
@TType(Page.TYPE_PAGE)
@ViewPermType(PermType.readitems)
@CreatePermType(PermType.createitems)
@EditPermType(PermType.updateitems)
@DeletePermType(PermType.deleteitems)
@PublishPermType(PermType.publishpages)
public interface Page extends GCNRenderable, StageableVersionedNodeObject, StageableChanneledNodeObject, CustomMetaDateNodeObject,
		TagContainer, ObjectTagContainer, LocalizableNodeObject<Page>, Disinheritable<Page>, Resolvable, NodeObjectWithAlternateUrls, PublishableNodeObjectInFolder, NamedNodeObject, StackResolvableNodeObject {

	/**
	 * The desired online status of a page.
	 */
	public enum OnlineStatusChange {
		/** No explicit change to the online status. The status will be infered from the page status, or be left alone completely. */
		NO_CHANGE(-1),
		/** The page is to go offline explicitly. */
		OFFLINE(0),
		/** The page should be online. */
		ONLINE(1),
		/** The page should go offline due to timemanagement. */
		TIME_MANAGEMENT(2);

		/**
		 * The numerical code stored in the database.
		 *
		 * <strong>Note:</strong> The only valid values to be stored in the database are <code>OFFLINE</code>, <code>TIME_MANAGEMENT</code>,
		 * and <code>ONLINE</code>.
		 */
		public final int code;

		/**
		 * Checks if the given status corresponds to the desired <code>OnlineStatus</code>.
		 *
		 * For <code>NO_CHANGE</code> any status is fine, for <code>OFFLINE</code> and <code>TIME_MANAGEMENT</code>
		 * the status must be <code>false</code> and for <code>ONLINE</code> it must be <code>true</code>.
		 *
		 * @param status Whether the page is currently online
		 * @return <code>true</code> when the given status corresponds to the enum value, and <code>false</code> otherwise.
		 */
		public boolean matches(boolean status) {
			switch (this) {
				case NO_CHANGE:
					return true;

				case OFFLINE:
				case TIME_MANAGEMENT:
					return !status;

				case ONLINE:
					return status;

				default:
					return false;
			}
		}

		/**
		 * When the current status is <code>NO_CHANGE</code> return the corresponding
		 * online status derived from the page status.
		 *
		 * @param pageStatus The current page status
		 * @return The needed online status corresponding to the page status
		 */
		public OnlineStatusChange fromPageStatus(int pageStatus) {
			if (this != NO_CHANGE) {
				return this;
			}

			switch (pageStatus) {
			case Page.STATUS_TOPUBLISH:
			case Page.STATUS_PUBLISHED:
				return OnlineStatusChange.ONLINE;

			case Page.STATUS_OFFLINE:
				return OFFLINE;

			case Page.STATUS_TIMEMANAGEMENT:
				return TIME_MANAGEMENT;

			default:
				return NO_CHANGE;
			}

		}

		private OnlineStatusChange(int code) {
			this.code = code;
		}
	}

	/**
	 * Prefix of the event property that contains the last pdate of a republished page
	 */
	public static final String PDATE_PROPERTY_PREFIX = "pdate:";

	/**
	 * This "status" is returned by the {@link #getStatus()} method, when the
	 * page was not found in the database.  Value: {@value}
	 */
	public static final int STATUS_NOTFOUND = -1;

	/**
	 * Page is locally modified and not yet (re-)published. Value: {@value}
	 */
	public static final int STATUS_MODIFIED = 0;

	/**
	 * Page is marked to be published (dirty). Value: {@value}
	 */
	public static final int STATUS_TOPUBLISH = 1;

	/**
	 * Page is published and online. Value: {@value}
	 */
	public static final int STATUS_PUBLISHED = 2;

	/**
	 * Page is offline. Value: {@value}
	 */
	public static final int STATUS_OFFLINE = 3;

	/**
	 * Page is in the queue (publishing of the page needs to be affirmed). Value: {@value}
	 */
	public static final int STATUS_QUEUE = 4;

	/**
	 * Page is in timemanagement and outside of the defined timespan (currently offline). Value: {@value}
	 */
	public static final int STATUS_TIMEMANAGEMENT = 5;

	/**
	 * Page is to be published at a given time (not yet). Value: {@value}
	 */
	public static final int STATUS_TOPUBLISH_AT = 6;

	public static final String[] RENDER_KEYS = new String[] { "page", "seite", "folder", "ordner", "node", "object"};

	/**
	 * The ttype of the page object. Value: {@value}
	 */
	public static final int TYPE_PAGE = 10007;
    
	/**
	 * The ttype of the page object as integer.
	 */
	public static final Integer TYPE_PAGE_INTEGER = new Integer(TYPE_PAGE);

	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Maximum length for descriptions
	 */
	public final static int MAX_DESCRIPTION_LENGTH = 255;

	/**
	 * Maximum length for nice URLs
	 */
	public final static int MAX_NICE_URL_LENGTH = 255;

	/**
	 * Deletes this page in all languages
	 */
	void deleteAllLanguages() throws InsufficientPrivilegesException, NodeException;

	/**
	 * Create a copy of the page in the specified target folder. This method
	 * will not commit or rollback the transaction in any case. Please invoke
	 * those calls afterwards. This method will automatically copy language
	 * variants as well when no contentSetId has been specified.
	 * @param sourceChannelId source channel ID
	 * @param targetFolder
	 *            The target folder to which the pages should be copied.
	 * @param createCopy
	 *            Whether to create a new page when an existing page with the
	 *            same filename was found in the target folder. In this case
	 *            "Copy of" will be prepended to the name of the page.
	 * @param contentSetId
	 *            ContentSetId that should be used for created copys. When set
	 *            to 0 or null a new contentset will automatically be created
	 *            and all language variants of the page will be copied.
	 * @param targetChannelId
	 *            The target channel id in which the page should be copied.
	 * 
	 * @return Page copy operation result object
	 * @throws NodeException
	 */
	PageCopyOpResult copyTo(Integer sourceChannelId, Folder targetFolder, boolean createCopy, Integer contentSetId, Integer targetChannelId) throws NodeException;

	/**
	 * returns a resolvable for the given keyword without adding any dependencies.
	 */
	public default Resolvable getKeywordResolvableWithoutDependencies(String keyword) throws NodeException {
		if ("ordner".equals(keyword) || "folder".equals(keyword)) {
			return getFolder();
		}
		if ("node".equals(keyword)) {
			return getFolder().getNode();
		}
		return this;
	}

	public default String[] getStackKeywords() {
		return RENDER_KEYS;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public default String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult(), null, null, null, null);
	}

	public default String render(RenderResult renderResult) throws NodeException {
		return render(renderResult, null, null, null, null);
	}

	/**
	 * Render the page content and the given tagmap entries. The rendered page content will be returned and the rendered tagmap entries will be set as values into the given map
	 * @param renderResult render result
	 * @param tagmapEntries tagmap entries to render (may be null)
	 * @param linkTransformer function that transforms values to links
	 * @return rendered content
	 * @throws NodeException
	 */
	public default String render(RenderResult renderResult, Map<TagmapEntryRenderer, Object> tagmapEntries,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer) throws NodeException {
		return render(renderResult, tagmapEntries, null, linkTransformer, null);
	}

	/**
	 * Render the page content and the given tagmap entries. The rendered page content will be returned and the rendered tagmap entries will be set as values into the given map
	 * @param renderResult render result
	 * @param tagmapEntries tagmap entries to render (may be null)
	 * @param attributes optional list of attributes to render
	 * @param linkTransformer function that transforms values to links
	 * @param times int array that will get the render times set (if not null)
	 * @return rendered content or null if attributes was not empty and did not contain "content"
	 * @throws NodeException
	 */
	default String render(RenderResult renderResult, Map<TagmapEntryRenderer, Object> tagmapEntries, Set<String> attributes,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer, long[] times) throws NodeException {
		return render(null, renderResult, tagmapEntries, attributes, linkTransformer, times);
	}

	/**
	 * Render the page content and the given tagmap entries with the given template. The rendered page content will be returned and the rendered tagmap entries will be set as values into the given map
	 * @param template (may be null to render the page's template)
	 * @param renderResult render result
	 * @param tagmapEntries tagmap entries to render (may be null)
	 * @param attributes optional list of attributes to render
	 * @param linkTransformer function that transforms values to links
	 * @param times int array that will get the render times set (if not null)
	 * @return rendered content or null if attributes was not empty and did not contain "content"
	 * @throws NodeException
	 */
	String render(String template, RenderResult renderResult, Map<TagmapEntryRenderer, Object> tagmapEntries, Set<String> attributes,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer, long[] times) throws NodeException;

	/**
	 * Returns the sync timestamp of the page.
	 * @return
	 */
	ContentNodeDate getSyncTimestamp();

	/**
	 * Set the sync timestamp of the page.
	 * 
	 * @param date
	 * @throws NodeException 
	 */
	void setSyncTimestamp(ContentNodeDate date) throws NodeException;

	/**
	 * Return the sync page id of the page.
	 *
	 * @return
	 * @throws NodeException
	 */
	Integer getSyncPageId() throws NodeException;


	/**
	 * Set the sync page id of the page.
	 *
	 * @param pageId The sync page id
	 * @throws NodeException
	 */
	void setSyncPageId(Integer pageId) throws NodeException;

	/**
	 * Get the template of the page.
	 * @return the template of the page.
	 * @throws NodeException TODO
	 */
	Template getTemplate() throws NodeException;

	/**
	 * Set the template id of the page. This will synchronize the templatetags with the contenttags (i.e. generate contenttags, if they are missing)
	 * @param templateId new template id
	 * @return old template id
	 * @throws ReadOnlyException when the page was not fetched for update
	 * @throws NodeException When an illegal template should be set
	 */
	public default Integer setTemplateId(Integer templateId) throws ReadOnlyException, NodeException {
		return setTemplateId(templateId, true);
	}

	/**
	 * Set the template id of the page. If the flag syncTags is true, this will synchronize the templatetags with the contenttags (i.e. generate contenttags, if they are missing)
	 * @param templateId new template id
	 * @param syncTags true if tags shall be synchronized, false if not
	 * @return old template id
	 * @throws ReadOnlyException when the page was not fetched for update
	 * @throws NodeException When an illegal template should be set
	 */
	Integer setTemplateId(Integer templateId, boolean syncTags) throws ReadOnlyException, NodeException;

	/**
	 * get the name of the page.
	 * @return the name of the page.
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * @return fully qualified name of this page with full path information
	 * @throws NodeException
	 */
	String getQualifiedName() throws NodeException;

	/**
	 * Set the name of the page
	 * @param name new name
	 * @return previous name
	 * @throws ReadOnlyException when the page was not fetched for updating
	 */
	@FieldSetter("name")
	String setName(String name) throws ReadOnlyException;

	/**
	 * get the description of the page.
	 * @return the description of the page.
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * Set the description of the page
	 * @param description new description
	 * @return old description
	 * @throws ReadOnlyException when the page was not fetched for updating
	 */
	@FieldSetter("description")
	String setDescription(String description) throws ReadOnlyException;

	/**
	 * get the filename of the page.
	 * @return the filename of the page.
	 */
	@FieldGetter("filename")
	String getFilename();

	/**
	 * Set the filename of the page
	 * @param filename new filename
	 * @return old filename
	 * @throws ReadOnlyException when the page was not fetched for updating
	 */
	@FieldSetter("filename")
	String setFilename(String filename) throws ReadOnlyException;

	/**
	 * get the priority of the page.
	 * @return the priority of the page.
	 */
	@FieldGetter("priority")
	int getPriority();

	/**
	 * Set the priority of the page
	 * @param priority new priority
	 * @return old priority
	 * @throws ReadOnlyException when the page was not fetched for updating
	 */
	@FieldSetter("priority")
	int setPriority(int priority) throws ReadOnlyException;

	/**
	 * Check whether the page is queued (for publishing or taking offline)
	 * @return true iff page is queued
	 * @throws NodeException
	 */
	default boolean isQueued() throws NodeException {
		return getPubQueueUser() != null || getOffQueueUser() != null;
	}

	/**
	 * Check whether publishing is delayed for this page
	 * @return true when publishing is delayed for the page, false if not
	 * @throws NodeException
	 */
	boolean isPublishDelayed() throws NodeException;

	/**
	 * get the content of the page.
	 * @return the content used by this page.
	 * @throws NodeException TODO
	 */
	Content getContent() throws NodeException;

	/**
	 * retrieve page creator
	 * @return creator of the page
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * retrieve page editor
	 * @return last editor of the page
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * get a list of all language variants of this page, sorted by the sortorder
	 * of the contentlanguage.
	 * @param considerNodeLanguages decide if Node language settings (enabled,
	 *        sortorder) shall be considered (set to true), or not (set to
	 *        false, oldschool php-style)
	 * @return a list of Pages.
	 * @throws NodeException TODO
	 */
	List<Page> getLanguageVariants(boolean considerNodeLanguages) throws NodeException;

	/**
	 * Retrieve language variant for a specific language code and a given node / channel
	 * @param code return pages with the given languagecode
	 * @param nodeId if a nodeId is specified, the method returns the languagevariant that is closest
	 * to the given channel (using multichannelling fallback)
	 * @return the language variant (page) for the given languagecode / node
	 * @throws NodeException
	 */
	Page getLanguageVariant(String code, Integer nodeId) throws NodeException;

	/**
	 * get a language-variant of a page in a given language.
	 * @param code the language code of the page to get.
	 * @return the page in the requested language, or null if not translated.
	 * @throws NodeException TODO
	 */
	Page getLanguageVariant(String code) throws NodeException;

	/**
	 * get the language of this page.
	 * @return the contentlanguage of this page, or null if not set.
	 * @throws NodeException TODO
	 */
	ContentLanguage getLanguage() throws NodeException;

	/**
	 * Set the language of this page
	 * @param language new language
	 * @return old language
	 * @throws ReadOnlyException
	 */
	ContentLanguage setLanguage(ContentLanguage language) throws ReadOnlyException;

	/**
	 * get the language id of this page
	 * @return language id
	 */
	Integer getLanguageId();

	/**
	 * Set the language id of this page
	 * @param languageId new language id
	 * @return old language id
	 * @throws ReadOnlyException
	 */
	Integer setLanguageId(Integer languageId) throws ReadOnlyException;

	/**
	 * get a collection of pages keyed by language code
	 * @return collection of pages
	 * @throws NodeException when there are problems retrieving language variants
	 */
	Collection<Page> getLanguages() throws NodeException;

	/**
	 * emulate PHP's serialize function for arrays to turn the list of language variants into a string 
	 * @return PHP-like serialized string of language variants
	 */
	String getLanguageVariantsPHPSerialized();

	/**
	 * Get the contenttag with given name
	 * @param name name
	 * @return contenttag
	 * @throws NodeException
	 */
	ContentTag getContentTag(String name) throws NodeException;

	/**
	 * get all page variants of this page.
	 * @return a list of all pages using the same content.
	 * @throws NodeException TODO
	 */
	public default List<Page> getPageVariants() throws NodeException {
		return getContent().getPages();
	}

	@Override
	Map<String, Tag> getTags() throws NodeException;

	/**
	 * Get the set of ids of contenttags, which were modified between the given
	 * timestamps
	 * @param firsttimestamp first versiontimestamp
	 * @param lasttimestamp last versiontimestamp
	 * @return set of ids of modified contenttags
	 * @throws NodeException
	 */
	Set<Integer> getModifiedContenttags(int firsttimestamp, int lasttimestamp) throws NodeException;

	/**
	 * Get the last published version for this page. Either get from the given dirt properties (if they contain the pdate) or from the publish table
	 * @param property dirted properties
	 * @return timestamp of the past published version or 0
	 * @throws NodeException
	 */
	int getLastPublishedVersion(String[] property) throws NodeException;

	/**
	 * Dirt the page (modify status from 2 to 1)
	 * @param channelId channel id
	 * @throws NodeException
	 * @return true when the page was dirted, false if not
	 */
	boolean dirtPage(int channelId) throws NodeException;

	/**
	 * Get the user, who put the page into queue for being published (immediately or at a time)
	 * @return user or null
	 * @throws NodeException
	 */
	SystemUser getPubQueueUser() throws NodeException;

	/**
	 * Get queued Publish At time
	 * @return Queue Publish At time
	 */
	ContentNodeDate getTimePubQueue();

	/**
	 * Get queued Publish At version
	 * @return queued Publish At version
	 * @throws NodeException
	 */
	NodeObjectVersion getTimePubVersionQueue() throws NodeException;

	/**
	 * Clear queued publish/take offline requests
	 * @throws NodeException
	 */
	void clearQueue() throws NodeException;

	/**
	 * Get the user, who put the page into queue for being taken offline (immediately or at a time)
	 * @return user or null
	 * @throws NodeException
	 */
	SystemUser getOffQueueUser() throws NodeException;

	/**
	 * Get queued scheduled time for taking the page offline
	 * @return ContentNodeDate instance
	 */
	ContentNodeDate getTimeOffQueue();

	Integer getContentsetId();

	/**
	 * Set the global id of the contentset
	 * @param globalId global id
	 * @throws ReadOnlyException
	 */
	void setGlobalContentsetId(GlobalId globalId) throws ReadOnlyException, NodeException;

	/**
	 * Set the contentset id
	 * @param id contentset id
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setContentsetId(Integer id) throws ReadOnlyException, NodeException;

	/**
	 * Reset the contentset id to null. This is useful when you want to create a new contentset id for the page.
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void resetContentsetId() throws ReadOnlyException, NodeException;

	@Override
	default void restoreVersion(NodeObjectVersion toRestore) throws NodeException {
		restoreVersion(toRestore, false);
	}

	/**
	 * Restore the given page version
	 * @param toRestore page version to restore
	 * @param pageTable true if the page table shall be restored, false if not
	 * @throws NodeException
	 */
	void restoreVersion(NodeObjectVersion toRestore, boolean pageTable) throws NodeException;

	/**
	 * Restore a version of the given tag in the page
	 * @param tag tag to restore
	 * @param versionTimestamp version timestamp to restore
	 * @throws NodeException
	 */
	void restoreTagVersion(ContentTag tag, int versionTimestamp) throws NodeException;

	@Override
	Page getPublishedObject() throws NodeException;

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getObject()
	 */
	public default Page getObject() {
		return this;
	}

	/**
	 * Get the channelset of this page. The keys will be the channel ids
	 * (node_ids) and the values will be the page ids. When there is no multichannelling, the map will be empty.
	 * @return channelset of this page.
	 * @throws NodeException
	 */
	Map<Integer, Integer> getChannelSet() throws NodeException;

	/**
	 * Get the ids of pages which might hide this page in subchannels. Keys are
	 * the channel ids and values are the page ids. In other words: get the ids
	 * of pages in the same channelset, which are created for subchannels of the
	 * page's chanhel. When there is no multichannelling, the map will be empty.
	 * @return map of pages eventually hiding this page
	 * @throws NodeException
	 */
	Map<Integer, Integer> getHidingPageIds() throws NodeException;

	/**
	 * Get the ids of pages which eventually are hidden by this page in
	 * subchannels. Keys are the channel ids and values are the page ids. In
	 * other words: get the ids of pages in the same channelset, which are
	 * created one of the masters of the page's channel. When there is no
	 * multichannelling, the map will be empty.
	 * @return map of pages eventually hidden by this page.
	 * @throws NodeException
	 */
	Map<Integer, Integer> getHiddenPageIds() throws NodeException;

	/**
	 * Get the channel of this page, if one set and multichannelling is supported.
	 * @return current channel of this page
	 * @throws NodeException
	 */
	Node getChannel() throws NodeException;

	/**
	 * Set the channel information for the page. The channel information consist
	 * of the channelId and the channelSetId. The channelId identifies the
	 * channel, for which the object is valid. If set to 0 (which is the
	 * default), the object is not a localized copy and no local object in a
	 * channel, but is a normal object in a node. The channelSetId groups the
	 * master object and all its localized copies in channel together. This
	 * method may only be called for new pages.
	 * 
	 * @param channel
	 *            id new channel id. If set to 0, this object will be a master
	 *            object in a node and the channelSetId must be given as null
	 *            (which will create a new channelSetId)
	 * @param channelSetId
	 *            id of the channelset. If set to null, a new channelSetId will
	 *            be generated and the object will be a master (in a node or in
	 *            a channel)
	 * @throws ReadOnlyException when the object is not editable
	 * @throws NodeException in case of other errors
	 */
	public default void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {
		setChannelInfo(channelId, channelSetId, false);
	}

	/**
	 * Check whether the page is inherited from a master (in multichannel) into
	 * the current channel
	 * @return true when the page is inherited, false if not or
	 *         multichannelling is disabled
	 * @throws NodeException
	 */
	boolean isInherited() throws NodeException;

	/**
	 * Check whether the page is a localized
	 * 
	 * @return true if this page is localized, false otherwise
	 * @throws NodeException
	 */
	public default boolean isLocalized() throws NodeException {
		return !isInherited() && getMaster() != this;
	}

	/**
	 * Check whether the page is a master or a localized copy
	 * @return true for master pages, false for localized copies
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;

	/**
	 * Get the master page, if this page is a localized copy. If this
	 * page is not a localized copy or multichannelling is not activated,
	 * returns this page
	 * 
	 * @return master page for localized copies or this page
	 * @throws NodeException
	 */
	public default Page getMaster() throws NodeException {
		return MultichannellingFactory.getMaster(this);
	}

	/**
	 * Gets master Folder Name
	 * @return
	 * @throws NodeException
	 */
	public default String getMasterNodeFolderName() throws NodeException {
		return getChannelMasterNode().getFolder().getName();
	}

	/**
	 * Gets the channel master Node or the master.
	 * @return channel master node or the master in case is not a channel.
	 * @throws NodeException
	 */
	Node getChannelMasterNode() throws NodeException;

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getNextHigherObject()
	 */
	public default Page getNextHigherObject() throws NodeException {
		return MultichannellingFactory.getNextHigherObject(this);
	}

	/**
	 * Push this page into the given master
	 * @param master master node to push this page to
	 * @return target page
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	Page pushToMaster(Node master) throws ReadOnlyException, NodeException;

	/**
	 * Get the publish workflow of the page (if it has one) or null
	 * @return publish workflow or null
	 * @throws NodeException
	 */
	PublishWorkflow getWorkflow() throws NodeException;

	/**
	 * Publish the page. This will set the status to 1, if necessary inform users
	 * watching the page in a workflow and remove existing workflows.
	 * @throws ReadOnlyException if the page was not fetched for editing
	 * @throws NodeException
	 */
	public default void publish() throws ReadOnlyException, NodeException {
		publish(0, null);
	}

	@Override
	default void publish(int at, boolean keepTimePubVersion) throws ReadOnlyException, NodeException {
		publish(at, keepTimePubVersion ? getTimePubVersion() : null);
	}

	/**
	 * Publish the page. This will set the status to 1, if necessary inform users
	 * watching the page in a workflow and remove existing workflows.
	 * @param timestamp timestamp to publish the page at a specific time
	 * @param version version that will be published at the timestamp
	 * @throws ReadOnlyException if the page was not fetched for editing
	 * @throws NodeException
	 */
	public default void publish(int timestamp, NodeObjectVersion version) throws ReadOnlyException, NodeException {
		publish(timestamp, version, true);
	}

	/**
	 * Publish the page. This will set the status to 1, if necessary inform users
	 * watching the page in a workflow and remove existing workflows.
	 * @param timestamp timestamp to publish the page at a specific time
	 * @param version version that will be published at the timestamp
	 * @param updatePublisher true to update the publisher
	 * @throws ReadOnlyException if the page was not fetched for editing
	 * @throws NodeException
	 */
	void publish(int timestamp, NodeObjectVersion version, boolean updatePublisher) throws ReadOnlyException, NodeException;

	/**
	 * Queue publishing the page
	 * @param user user (must not be null)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	default void queuePublish(SystemUser user) throws ReadOnlyException, NodeException {
		queuePublish(user, 0, null);
	}

	/**
	 * Queue publishing the page
	 * @param user user (must not be null)
	 * @param timestamp optional timestamp to queue publishAt (0 for publish)
	 * @param version version to queue for publishing at timestamp
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void queuePublish(SystemUser user, int timestamp, NodeObjectVersion version) throws ReadOnlyException, NodeException;

	/**
	 * Queue taking the page offline
	 * @param user (must not be null)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	default void queueOffline(SystemUser user) throws ReadOnlyException, NodeException {
		queueOffline(user, 0);
	}

	/**
	 * Queue taking the page offline
	 * @param user (must not be null)
	 * @param timestamp optional timestamp for offlineAt (0 for offline)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void queueOffline(SystemUser user, int timestamp) throws ReadOnlyException, NodeException;

	/**
	 * Create a new page variant of the page in the same folder.
	 * The name of the page will be "Page variant of [original page name]"
	 * @return page variant of the page (not yet saved)
	 * @throws NodeException
	 */
	public default Page createVariant() throws NodeException {
		return createVariant(getFolder());
	}

	/**
	 * Create a new page variant of the page in the given folder. If the folder
	 * is the folder of this page, the name will be "Page variant of [original
	 * page name]". If the folder is different, the variant will have the same
	 * name
	 * @param folder folder where to create the page variant
	 * @return page variant of the page (not yet saved)
	 * @throws NodeException
	 */
	Page createVariant(Folder folder) throws NodeException;

	/**
	 * Create a new page variant of the page in the given folder. If the folder
	 * is the folder of this page, the name will be "Page variant of [original
	 * page name]". If the folder is different, the variant will have the same
	 * name
	 * @param folder folder where to create the page variant
	 * @param channel the channel where to create the page variant
	 * @return page variant of the page (not yet saved)
	 * @throws NodeException
	 */
	Page createVariant(Folder folder, Node channel) throws NodeException;

	/**
	 * Get the page (language variant) with which this page is synchronized, or
	 * null if this page is not synchronized with another language variant
	 * @return page or null
	 * @throws NodeException
	 */
	Page getSynchronizedWith() throws NodeException;

	/**
	 * Check whether this page is in sync with its "master" language variant.
	 * This method returns true if
	 * <ul>
	 * <li>The page is not synchronized with another language variant</li>
	 * <li>The page is synchronized with the latest version of another language
	 * variant</li>
	 * </ul>
	 * and returns false if
	 * <ul>
	 * <li>The page is synchronized with a version of another language variant,
	 * which no longer is the latest version</li>
	 * </ul>
	 * 
	 * @return true or false
	 * @throws NodeException
	 */
	boolean isInSync() throws NodeException;

	/**
	 * Synchronize this page with the latest version of the given page, which
	 * must be a language variant of this page. If the given page is
	 * not a language variant of this page, an exception is thrown.
	 * If null is given, the synchronization information is removed
	 * @param page
	 *            page to synchronize with or null to remove synchronization information
	 * @throws NodeException
	 */
	void synchronizeWithPage(Page page) throws NodeException;

	/**
	 * Synchronize this page with the given version of the given page, which must be a language variant of this page.
	 * @param page page to synchornize with
	 * @param versionTimestamp version timestamp to synchronize with
	 * @throws NodeException
	 */
	void synchronizeWithPageVersion(Page page, int versionTimestamp) throws NodeException;

	/**
	 * Set the content for a new created page. This method will fail,
	 * if a content shall be set to a page, which already has a saved content.
	 * @param content content to set
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setContent(Content content) throws ReadOnlyException, NodeException;

	/**
	 * Set the content id for a new created page.
	 * @param contentId content id
	 * @throws ReadOnlyException
	 */
	void setContentId(Integer contentId) throws ReadOnlyException;

	/**
	 * Save the page, optionally create a page version.
	 * @param createVersion true if a page version shall be created, false if not.
	 * @return true if the page was modified
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	public default boolean save(boolean createVersion) throws InsufficientPrivilegesException, NodeException {
		return save(createVersion, true);
	}

	/**
	 * Save the page, optionally create a page version
	 * @param createVersion true if a page version shall be created, false if not.
	 * @param updateEditor true to update editor and edate, false to leave untouched
	 * @return true if the page was modified
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	boolean save(boolean createVersion, boolean updateEditor) throws InsufficientPrivilegesException, NodeException;

	/**
	 * Migrate contenttags that came from templatetags, in one of those cases
	 * <ul>
	 * <li>The templatetag is no longer editable in tag (contenttag will be removed)</li>
	 * <li>The templatetag does not exist any more (contenttag will be removed)</li>
	 * <li>The templatetag uses a different construct (contenttag will be changed)</li>
	 * </ul>
	 * @param template The template with the newly changed tagtypes. This parameter is needed, because
	 * when using multichanneling, getTemplate() might not be in the desired channel.
	 * @param tagnames optional list of tagnames to check
	 * @param force true to force incompatible migrations
	 */
	void migrateContenttags(Template template, List<String> tagnames, boolean force) throws NodeException;

	/**
	 * Check if a page needs to migrate at least one contenttag to conform to the template.
	 * A contenttag needs to be migrated (or created), in one of these cases:
	 * <ul>
	 * <li>The contenttag came from a templatetag, which is no longer editable in page</li>
	 * <li>The contenttag came from a templatetag, which no longer exists</li>
	 * <li>The contenttag came from a templatetag, which uses a different construct now</li>
	 * <li>The contenttag does not exist for a templatetag, which is editable in page</li>
	 * </ul>
	 * @param template template
	 * @param tagnames optional list of tagnames to migrate
	 * @param force true to force incompatible migrations. If false, this only returns true if all contenttags needing migration are compatible with their target construct.
	 */
	boolean needsContenttagMigration(Template template, List<String> tagnames, boolean force) throws NodeException;

	/**
	 * Move this page into the given folder
	 * @param target target folder
	 * @param targetChannelId target channel (0 to move into the master node)
	 * @param allLanguages true to move all languages of the page, false to move only this page
	 * @return operation result
	 * @throws NodeException
	 */
	OpResult move(Folder target, int targetChannelId, boolean allLanguages) throws NodeException;

	@Override
	public default Node getNode() throws NodeException {
		Folder folder;
		try (NoMcTrx noMc = new NoMcTrx()) {
			folder = getFolder();
		}

		return folder.getNode();
	}

	/**
	 * Return true if this object is deleted (and put into the wastebin), false if not
	 * @return true for deleted objects, false otherwise
	 */
	boolean isDeleted();

	@Override
	default String getFullPublishPath(boolean trailingSlash, boolean includeNodePublishDir) throws NodeException {
		return getFullPublishPath(getFolder(), trailingSlash, includeNodePublishDir);
	}

	@Override
	default String getFullPublishPath(Folder folder, boolean trailingSlash, PageLanguageCode pageLanguageCode,
			boolean includeNodePublishDir) throws NodeException {
		List<String> segments = new ArrayList<>();

		if (includeNodePublishDir) {
			Node node = folder.getNode();
			ContentRepository cr = node.getContentRepository();

			if (cr == null || !cr.ignoreNodePublishDir()) {
				segments.add(node.getPublishDir());
			}
		}

		ContentLanguage language = getLanguage();
		if (language != null && pageLanguageCode == PageLanguageCode.PATH) {
			segments.add(language.getCode());
		}

		// we need to set the page language into the rendertype, because translating the folder pub_dir depends on it
		try (ContentLanguageTrx cLTrx = new ContentLanguageTrx(language)) {
			segments.add(folder.getPublishPath());
		}

		return FilePublisher.getPath(
			true,
			trailingSlash,
			segments.toArray(new String[0]));
	}

	@Override
	default String getSuffix() {
		return ".page";
	}

	@Override
	default Optional<Node> maybeGetChannel() throws NodeException {
		return Optional.ofNullable(getChannel());
	}

	@Override
	default void setFolder(Node node, Folder parent) throws NodeException {
		if (isNew()) {
			setFolderId(parent.getId());
		} else {
			OpResult moveResult = move(parent, Optional.ofNullable(node).map(Node::getId).orElse(0), true);
			if (!moveResult.isOK()) {
				String message = moveResult.getMessages().stream().map(NodeMessage::getMessage).collect(Collectors.joining(", "));
				throw new NodeException(message);
			}
		}
	}

	@Override
	default Node getOwningNode() throws NodeException {
		try (NoMcTrx noMc = new NoMcTrx()) {
			return getFolder().getOwningNode();
		}
	}

	@Override
	default int getRoleBit(PermType permType) {
		return permType.getPageRoleBit();
	}

	@Override
	default int getRoleCheckId() {
		return ObjectTransformer.getInt(getLanguageId(), -1);
	}

	@Override
	default Optional<StageableVersionedNodeObject> maybeVersioned() {
		return Optional.of(this);
	}

	@Override
	default Optional<Collection<? extends StageableNodeObject>> maybeGetVariants() throws NodeException {
		Collection<Page> languages = getLanguages();
		if (ObjectTransformer.isEmpty(languages)) {
			return Optional.empty();
		} else {
			return Optional.of(languages);
		}
	}
}
