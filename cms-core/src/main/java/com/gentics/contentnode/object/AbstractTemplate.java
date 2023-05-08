package com.gentics.contentnode.object;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoObjectTagSync;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderInfo;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.base.MapResolver;

public abstract class AbstractTemplate extends AbstractContentObject implements Template {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2225801296311644654L;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	static {
		// TODO check correct dependencies
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			public Object get(AbstractTemplate tmpl, String key) {
				return tmpl.getName();
			}
		});
		resolvableProperties.put("description", new Property(new String[] {"description"}) {
			public Object get(AbstractTemplate object, String key) {
				return object.getDescription();
			}
		});
		resolvableProperties.put("tags", new Property(new String[] { "tags"}) {
			public Object get(AbstractTemplate tmpl, String key) {
				try {
					return new MapResolver(tmpl.getTags());
				} catch (NodeException e) {
					tmpl.logger.error("could not create MapResolver from getTags()", e);
					return null;
				}
			}
		});
		resolvableProperties.put("ml", new Property(new String[] { "ml"}) {
			public Object get(AbstractTemplate tmpl, String key) {
				try {
					return tmpl.getMarkupLanguage();
				} catch (NodeException e) {
					tmpl.logger.error("Unable to retrieve MarkupLanguage for Template {" + tmpl + "}", e);
				}
				return null;
			}
		});
		Property folder = new Property(null) {
			public Object get(AbstractTemplate tmpl, String key) {
				return tmpl.getCurrentFolder();
			}
		};

		resolvableProperties.put("folder", folder);  
		resolvableProperties.put("ordner", folder);
		resolvableProperties.put("object", new Property(null) {
			public Object get(AbstractTemplate tmpl, String key) {
				return new ObjectTagResolvable(tmpl, false);
			}
		});       
		resolvableProperties.put("node", new Property(null) {
			public Object get(AbstractTemplate tmpl, String key) {
				try {
					return tmpl.getCurrentFolder().getNode();
				} catch (Exception e) {
					tmpl.logger.error("Could not retrieve property node from current folder.", e);
					return null;
				}
			}
		});       
	}

	protected AbstractTemplate(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Deletes the Template. <br />
	 * Be careful with calling this method directly because it doesn't check if the template is referenced somewhere.
	 * It also doesn't check any permissions or considers putting the template into the wastebin!
	 * It directly deletes the Template, even if references are left somewhere!
	 * Use {@link Folder#unlinkTemplate(Object)} for unlinking templates from a folder.
	 * @param force
	 */
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		// Delete ObjectTags
		Map<String, ObjectTag> objectTags = this.getObjectTags();

		// deleting templates should never cause deletion of objtags in other templates due to synchronization
		try (NoObjectTagSync noSync = new NoObjectTagSync()) {
			for (ObjectTag tag : objectTags.values()) {
				tag.delete();
			}
		}
		// Delete TemplateTags
		Map<String, TemplateTag> templateTags = this.getTemplateTags();

		for (TemplateTag tag : templateTags.values()) {
			tag.delete();
		}
		performDelete();
	}

	/**
	 * Performs the delete of the Template
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	@Override
	public Tag getTag(String name) throws NodeException {
		Tag tag = getTemplateTag(name);

		if (null != tag) {
			return tag;
		}
		return (Tag) getObjectTags().get(name);
	}

	@Override
	public Map<String, TemplateTag> getTags() throws NodeException {
		return getTemplateTags();
	}

	@Override
	public Map<String, ObjectTag> getObjectTags() throws NodeException {
		return loadObjectTags();
	}

	protected abstract Map<String, ObjectTag> loadObjectTags() throws NodeException;

	@Override
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		if ("object".equals(keyword)) {
			return new ObjectTagResolvable(this, true);
		}
		if ("ordner".equals(keyword) || "folder".equals(keyword)) {
			return getCurrentFolder();
		}
		return this;
	}

	/**
	 * if a folder id was provided an expanded set of keywords will be provided
	 * @return string array of keywords
	 */
	public String[] getStackKeywords() {
		String[] renderKeys = RENDER_KEYS;

		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			if (renderType != null && renderType.find(this) == 1) {
				Integer folderId = ObjectTransformer.getInteger(renderType.getParameter(RenderInfo.PARAMETER_CURRENT_FOLDER_ID), null);
				int rKeysLength = RENDER_KEYS.length + 1;

				if (folderId != null) {
					rKeysLength += 2;
				}
				renderKeys = new String[rKeysLength];
				int i;

				for (i = 0; i < RENDER_KEYS.length; i++) {
					renderKeys[i] = RENDER_KEYS[i];
				}
				if (folderId != null) {
					renderKeys[i++] = "folder";
					renderKeys[i++] = "ordner";
				}
				renderKeys[i++] = "object";
			}
		} catch (TransactionException e) {
			return RENDER_KEYS;
		}
		return renderKeys;
	}

	@Override
	public Resolvable getShortcutResolvable() throws NodeException {
		return new MapResolver(getTags());
	}

	@Override
	public String getStackHashKey() {
		return "tpl:" + getHashKey();
	}

	@Override
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	@Override
	public String render(RenderResult renderResult) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		renderType.push(this);

		try {
			TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

			return renderer.render(renderResult, getSource());
		} finally {
			renderType.pop();
		}
	}

	@Override
	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * return the current folder which was passed to the JavaParser via the
	 * folderId parameter. This folder (may) differ from the folder returned by
	 * getFolder() as it reflects the currently active folder, and not the
	 * folder the template was created in.
	 * @return currently active folder
	 */
	protected Folder getCurrentFolder() {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			Integer folderId = ObjectTransformer.getInteger(renderType.getParameter(RenderInfo.PARAMETER_CURRENT_FOLDER_ID), null);

			if (folderId != null) {
				return (Folder) t.getObject(Folder.class, folderId);
			}
		} catch (Exception e) {
			logger.warn("could not retrieve current folder for template {" + this + "}", e);
		}
		return null;
	}

	@Override
	public ObjectTag getObjectTag(String name) throws NodeException {
		return getObjectTag(name, false);
	}

	@Override
	public ObjectTag getObjectTag(String name, boolean fallback) throws NodeException {
		ObjectTag tag = (ObjectTag) getObjectTags().get(name);

		if (fallback && tag == null) {
			Folder folder = getCurrentFolder();

			if (folder != null) {
				return folder.getObjectTag(name, true);
			}
		}
		return tag;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.AbstractContentObject#triggerEvent(com.gentics.contentnode.events.DependencyObject, java.lang.String[], int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask,
			int depth, int channelId) throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);
        
		if (Events.isEvent(eventMask, Events.UPDATE)) {
			// trigger update events for all tags .. 
			Collection<TemplateTag> templateTags = getTemplateTags().values();

			for (TemplateTag tag : templateTags) {
				DependencyObject dep = new DependencyObject(tag, (NodeObject) null);

				tag.triggerEvent(dep, null, Events.UPDATE, depth + 1, 0);
			}
		}
	}

	@Override
	public Template getObject() {
		return this;
	}

	/**
	 * Get the master template, if this template is a localized copy. If this
	 * template is not a localized copy or multichannelling is not activated,
	 * returns this template
	 * 
	 * @return master template for localized copies or this template
	 * @throws NodeException
	 */
	public Template getMaster() throws NodeException {
		return MultichannellingFactory.getMaster(this);
	}

	@Override
	public Template getNextHigherObject() throws NodeException {
		return MultichannellingFactory.getNextHigherObject(this);
	}

	/**
	 * Push this template into the given master
	 * @param master master node to push this template to
	 * @return target template
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public Template pushToMaster(Node master) throws ReadOnlyException, NodeException {
		return MultichannellingFactory.pushToMaster(this, master).getObject();
	}

	@Override
	public int getEffectiveUdate() throws NodeException {
		// get template's udate
		int udate = getUdate();
		// check the templatetags
		Map<String, TemplateTag> tags = getTemplateTags();

		for (Tag tag : tags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		// check the objtags
		Map<String, ObjectTag> oTags = getObjectTags();

		for (Tag tag : oTags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		return udate;
	}

	@Override
	public Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public void addFolder(Folder folder) throws NodeException {
		failReadOnly();
	}

	@Override
	public void setTemplategroupId(Integer templategroupId) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setGlobalTemplategroupId(GlobalId globalId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void modifyChannelId(Integer channelId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public Integer setMlId(Integer mlId) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public String setSource(String source) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public String setName(String name) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public String setDescription(String description) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public boolean save() throws InsufficientPrivilegesException, NodeException {
		return save(true);
	}

	@Override
	public boolean save(boolean syncPages) throws InsufficientPrivilegesException, NodeException {
		failReadOnly();
		return false;
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 */
		public abstract Object get(AbstractTemplate object, String key);
	}
}
