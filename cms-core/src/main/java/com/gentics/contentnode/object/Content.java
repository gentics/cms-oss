/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Content.java,v 1.13.4.1.2.2 2011-03-07 16:53:54 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.TType;
import com.gentics.lib.base.MapResolver;

/**
 * The Content contains the tags and content-specific attributes of a page.
 */
@TType(Content.TYPE_CONTENT)
public abstract class Content extends AbstractContentObject implements TagContainer {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2686463571884058545L;

	public static final int TYPE_CONTENT = 10015;

	protected Content(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * get all contenttags with their names.
	 * @return all contenttags as String->ContentTag.
	 * @throws NodeException
	 */
	public abstract Map<String, ContentTag> getContentTags() throws NodeException;

	/**
	 * get all pages which use this content.
	 * @return the list of all pages using this content.
	 * @throws NodeException
	 */
	public abstract List<Page> getPages() throws NodeException;

	public abstract boolean isPartiallyLocalized();
	public abstract Content setPartiallyLocalized(boolean partiallyLocalized) throws NodeException;

	/**
	 * get a contenttag by name.
	 * @param name the name of the tag.
	 * @return the contenttag, or null if not found.
	 * @throws NodeException TODO
	 */
	public ContentTag getContentTag(String name) throws NodeException {
		return (ContentTag) getContentTags().get(name);
	}

	/**
	 * Get the contenttag by id
	 * @param id tag ID
	 * @return the contenttag, or null if not found
	 * @throws NodeException
	 */
	public ContentTag getContentTag(int id) throws NodeException {
		return getContentTags().values().stream().filter(tag -> Objects.equals(tag.getId(), id)).findFirst().orElse(null);
	}

	/**
	 * get a contenttag by name.
	 * @param name the name of the tag.
	 * @return the contenttag, or null if not found.
	 */
	public Tag getTag(String name) throws NodeException {
		return getContentTag(name);
	}

	/**
	 * get all contenttags with their names.
	 * @return all contenttags as String->ContentTag.
	 */
	public Map<String, ? extends Tag> getTags() throws NodeException {
		return getContentTags();
	}

	/**
	 * check, if the content is currenty locked by a user.
	 * @return true, if the content is currently locked.
	 */
	public abstract boolean isLocked() throws NodeException;

	/**
	 * Get the date, since when the content is locked, or null if it is not locked
	 * @return date of content locking or null
	 * @throws NodeException
	 */
	public abstract ContentNodeDate getLockedSince() throws NodeException;

	/**
	 * Get the user, by which the content is currently locked (if
	 * {@link #isLocked()} returns true) or null if the content is not currently
	 * locked.
	 * @return a user or null
	 * @throws NodeException
	 */
	public abstract SystemUser getLockedBy() throws NodeException;

	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return this;
	}

	public String[] getStackKeywords() {
		return Page.RENDER_KEYS;
	}

	public Resolvable getShortcutResolvable() throws NodeException {
		return new MapResolver(getTags());
	}

	public String getStackHashKey() {
		return "content:" + getHashKey();
	}

	@Override
	public void delete(boolean force) throws NodeException {
		for (ContentTag tag : getContentTags().values()) {
			tag.delete();
		}
		performDelete();
	}

	/**
	 * Performs the delete for the content
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	/*
	 * (non-Javadoc)
	 * @see
	 * com.gentics.contentnode.object.AbstractContentObject#triggerEvent(com
	 * .gentics.contentnode.events.DependencyObject, java.lang.String[], int,
	 * int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask,
			int depth, int channelId) throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);

		// when the event UPDATE | CN_CONTENT was triggered, someone
		// synchronized templatetags with their pages, so we need to dirt all
		// tags of the content and all pages
		if (Events.isEvent(eventMask, Events.UPDATE) && Events.isEvent(eventMask, Events.EVENT_CN_CONTENT)) {
			// trigger UPDATE on all tags of the content
			for (ContentTag tag : getContentTags().values()) {
				tag.triggerEvent(new DependencyObject(tag, (NodeObject) null), null, Events.UPDATE, depth + 1, 0);
			}

			// and additionally trigger UPDATE on all pages of the content
			for (Page page : getPages()) {
				page.triggerEvent(new DependencyObject(page, (NodeObject) null), null, Events.UPDATE, depth + 1, 0);
			}
		}
	}

	/**
	 * Add a new ContentTag to this content and return the instance (which is editable)
	 * @param constructId construct id
	 * @return the new contenttag
	 * @throws NodeException in case of other errors
	 */
	public ContentTag addContentTag(int constructId) throws NodeException {
		failReadOnly();
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the content's udate
		int udate = getUdate();
		// and also check udates for tags
		Map<String, ? extends Tag> tags = getTags();

		for (Tag tag : tags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		return udate;
	}

	/**
	 * Get the node
	 * @return node
	 * @throws NodeException
	 */
	public abstract Node getNode() throws NodeException;
}
