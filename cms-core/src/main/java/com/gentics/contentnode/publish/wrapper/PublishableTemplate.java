package com.gentics.contentnode.publish.wrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.object.AbstractTemplate;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of {@link Template} that wraps the REST model of a template.
 * Instances of this class are created and cached while publish process (if versioned publishing and multithreaded publishing is used)
 */
public class PublishableTemplate extends AbstractTemplate {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7407651564550202958L;

	/**
	 * Cache region
	 */
	public final static String CACHEREGION = "gentics-publishable-templates";

	/**
	 * Cache
	 */
	public static PortalCache cache;

	/**
	 * Logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(PublishableTemplate.class);

	/**
	 * Filled references
	 */
	protected final static Collection<Reference> fillRefs = Arrays.asList(Reference.TEMPLATE_TAGS, Reference.TEMPLATE_SOURCE, Reference.OBJECT_TAGS);

	/**
	 * Wrapped template
	 */
	protected com.gentics.contentnode.rest.model.Template wrappedTemplate;

	/**
	 * Markup Language
	 */
	protected PublishableMarkupLanguage markupLanguage;

	/**
	 * Creation date
	 */
	protected ContentNodeDate cDate;

	/**
	 * Edit date
	 */
	protected ContentNodeDate eDate;

	/**
	 * Map of object tags
	 */
	protected Map<String, ObjectTag> objectTags;

	/**
	 * Map of all template tags
	 */
	protected Map<String, TemplateTag> templateTags;

	/**
	 * Map of the private tags (template tags that are not editable in page)
	 */
	protected Map<String, TemplateTag> privateTemplateTags;

	/**
	 * Template's channel
	 */
	protected Node channel;

	static {
		try {
			cache = PortalCache.getCache(CACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing cache for region {" + CACHEREGION + "}, will not use object cache", e);
		}
	}

	/**
	 * Get an instance of the publishable template. First try the cache, if not found in the cache, put it there
	 * @param templateId template id
	 * @return publishable template or null if the template does not exist
	 * @throws NodeException
	 */
	public static PublishableTemplate getInstance(int templateId) throws NodeException {
		PublishableTemplate template = null;
		if (cache != null) {
			try {
				template = (PublishableTemplate)cache.get(templateId);
			} catch (PortalCacheException e) {
			}
		}

		if (template == null) {
			com.gentics.contentnode.rest.model.Template model = createModel(templateId);
			if (model != null) {
				template = new PublishableTemplate(templateId, model);
				if (cache != null) {
					try {
						cache.put(templateId, template);
					} catch (PortalCacheException e) {
					}
				}
			}
		}

		return template;
	}

	/**
	 * Create the model and put it into the cache
	 * @param pageId page id
	 * @return model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Template createModel(int templateId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean versionedPublishDisabled = t.isDisableVersionedPublish();
		t.setDisableVersionedPublish(true);
		Template template = null;
		try {
			template = t.getObject(Template.class, templateId, -1, false);
			if (template == null) {
				return null;
			}
		} finally {
			t.setDisableVersionedPublish(versionedPublishDisabled);
		}
		com.gentics.contentnode.rest.model.Template model = ModelBuilder.getTemplate(template, fillRefs);

		return model;
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
	 * Get the prepared PublishData or fail, if not prepared
	 * @return prepared PublishData
	 * @throws NodeException
	 */
	protected static PublishData getPublishData() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PublishData publishData = t.getPublishData();
		if (publishData == null) {
			throw new NodeException("Cannot get PublishData: must be prepared");
		}

		return publishData;
	}

	/**
	 * Create an instance
	 * @param id template id
	 * @param info object info
	 * @param wrappedTemplate wrapped template
	 */
	public PublishableTemplate(Integer id, com.gentics.contentnode.rest.model.Template wrappedTemplate) throws NodeException {
		super(id, null);
		this.wrappedTemplate = wrappedTemplate;
		markupLanguage = new PublishableMarkupLanguage(this);
		cDate = new ContentNodeDate(wrappedTemplate.getCdate());
		eDate = new ContentNodeDate(wrappedTemplate.getEdate());

		// make objectTags and templateTags
		objectTags = new HashMap<String, ObjectTag>();
		templateTags = new HashMap<String, TemplateTag>();
		privateTemplateTags = new HashMap<String, TemplateTag>();

		for (Map.Entry<String, com.gentics.contentnode.rest.model.TemplateTag> tagEntry : wrappedTemplate.getTemplateTags().entrySet()) {
			TemplateTag tag = new PublishableTemplateTag(tagEntry.getValue());
			templateTags.put(tagEntry.getKey(), tag);
			if (!ObjectTransformer.getBoolean(tagEntry.getValue().getEditableInPage(), false)) {
				privateTemplateTags.put(tagEntry.getKey(), tag);
			}
		}

		for (Map.Entry<String, Tag> tagEntry : wrappedTemplate.getObjectTags().entrySet()) {
			String name = tagEntry.getKey();
			if (name.startsWith("object.")) {
				name = name.substring(7);
			}
			objectTags.put(name, new PublishableObjectTag(tagEntry.getValue(), this));
		}
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		if (info == null) {
			info = new PublishableNodeObjectInfo(Template.class, -1);
		}
		return info;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId()
	 */
	public Integer getChannelSetId() throws NodeException {
		return wrappedTemplate.getChannelSetId();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getOwningNode()
	 */
	public Node getOwningNode() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	public NodeObject copy() throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public String getName() {
		return wrappedTemplate.getName();
	}

	@Override
	public String getDescription() {
		return wrappedTemplate.getDescription();
	}

	@Override
	public String getSource() {
		return wrappedTemplate.getSource();
	}

	@Override
	public Integer getMlId() {
		return wrappedTemplate.getMarkupLanguage().getId();
	}

	@Override
	public MarkupLanguage getMarkupLanguage() throws NodeException {
		return markupLanguage;
	}

	@Override
	public boolean isLocked() throws NodeException {
		return false;
	}

	@Override
	public boolean isLockedByCurrentUser() throws NodeException {
		return false;
	}

	@Override
	public SystemUser getLockedBy() throws NodeException {
		return null;
	}

	@Override
	public boolean isUnlinkable(Folder folder) throws NodeException {
		failReadOnly();
		return false;
	}

	@Override
	public boolean isInherited() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			// multichannelling is not used, so the template cannot be inherited
			return false;
		}

		// determine the current channel id
		Node channel = t.getChannel();

		if (channel == null || !channel.isChannel()) {
			return false;
		}

		// the template is inherited if its channelid is different from the current channel
		return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(wrappedTemplate.getChannelId(), -1);
	}

	@Override
	public boolean isMaster() throws NodeException {
		return wrappedTemplate.isMaster();
	}

	@Override
	public SystemUser getCreator() throws NodeException {
		return getSystemUser(wrappedTemplate.getCreator());
	}

	@Override
	public SystemUser getEditor() throws NodeException {
		return getSystemUser(wrappedTemplate.getEditor());
	}

	@Override
	public ContentNodeDate getCDate() {
		return cDate;
	}

	@Override
	public ContentNodeDate getEDate() {
		return eDate;
	}

	@Override
	protected void performDelete() throws NodeException {
		failReadOnly();
	}

	@Override
	public Map<String, TemplateTag> getTemplateTags() throws NodeException {
		return templateTags;
	}

	@Override
	protected Map<String, ObjectTag> loadObjectTags() throws NodeException {
		return objectTags;
	}

	@Override
	public Folder getFolder() throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(Folder.class, wrappedTemplate.getFolderId());
	}

	@Override
	public List<Folder> getFolders() throws NodeException {
		return Collections.emptyList();
	}

	@Override
	public Set<Integer> getFolderIds() throws NodeException {
		return Collections.emptySet();
	}

	@Override
	public Map<String, TemplateTag> getPrivateTemplateTags() throws NodeException {
		return privateTemplateTags;
	}

	@Override
	public Map<Integer, Integer> getChannelSet() throws NodeException {
		return getPublishData().getChannelset(this);
	}

	@Override
	public Node getChannel() throws NodeException {
		Integer channelId = wrappedTemplate.getChannelId();
		if (channelId == null) {
			return null;
		}
		if (channel == null) {
			channel = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId, -1, false);
		}
		return channel;
	}

	@Override
	public Integer getTemplategroupId() {
		return null;
	}

	@Override
	public List<Page> getPages() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Node> getNodes() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Node> getAssignedNodes() throws NodeException {
		// dummy implementation
		return null;
	}

	@Override
	public UpdatePagesResult updatePages(int commitAfter, int maxMessages, List<String> tagnames, boolean force) throws NodeException {
		failReadOnly();
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
	public boolean isDeleted() {
		return false;
	}

	/**
	 * Wrapper implementation of TemplateTag
	 */
	protected class PublishableTemplateTag extends TemplateTag {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 8819331146767715158L;

		/**
		 * Wrapped tag
		 */
		protected com.gentics.contentnode.rest.model.TemplateTag tag;

		/**
		 * Value List
		 */
		protected PublishableValueList valueList;

		/**
		 * Create an instance
		 * @param tag wrapped tag
		 */
		private PublishableTemplateTag(com.gentics.contentnode.rest.model.TemplateTag tag) throws NodeException {
			super(tag.getId(), PublishableTemplate.this.getObjectInfo().getSubInfo(TemplateTag.class));
			this.tag = tag;
			this.valueList = new PublishableValueList(tag, this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			failReadOnly();
			return null;
		}

		@Override
		public Template getTemplate() throws NodeException {
			return PublishableTemplate.this;
		}

		@Override
		public boolean getMandatory() {
			return ObjectTransformer.getBoolean(tag.getMandatory(), false);
		}

		@Override
		public boolean isPublic() {
			return ObjectTransformer.getBoolean(tag.getEditableInPage(), false);
		}

		@Override
		public Integer getTemplategroupId() {
			// TODO Auto-generated method stub
			return null;
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
			return "PublishableTemplateTag {" + getName() + ", " + getId() + "}";
		}
	}
}
