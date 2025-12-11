package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.i18n.CNI18nString;
import org.apache.commons.lang3.StringUtils;

/**
 * A job that unlocalized a root tag in a page.
 */
public class UnlocalizeTagJob extends AbstractBackgroundJob {

	/** ID of the channel to unlocalize in. */
	protected Integer channelId;

	/** ID of the page to unlicalize the tag for. */
	private int pageId;

	/** ID of the tag to unlocalize. */
	private String tagId;

	@Override
	public String getJobDescription() {
		return new CNI18nString("page.unlocalize_root_tag").toString();
	}

	@Override
	protected void processAction() throws NodeException {
		var t = TransactionManager.getCurrentTransaction();
		var channel = t.getObject(Node.class, channelId);

		if (channel == null) {
			throw new NodeException("Could not find channel {" + channelId + "}");
		}

		// if the "channel" is not really one, we do nothing
		if (!channel.isChannel()) {
			return;
		}

		try (var cTrx = new ChannelTrx(channel)) {
			var page = t.getObject(Page.class, pageId, true);

			if (!t.canView(page)) {
				return;
			}

			if (page.isMaster() && !t.canEdit(page)) {
				throw new InsufficientPrivilegesException("Cannot unlocalize tag {" + tagId + "} on page {" + pageId + "} in channel {" + channelId + "}");
			}

			var tags = page.getContentTags();

			if (!tags.containsKey(tagId)) {
				throw new EntityNotFoundException("Tag {" + tagId + "} not found on page {" + pageId + "} in channel {" + channelId + "}");
			}

			var toRemove = new HashSet<String>();

			toRemove.add(tagId);
			toRemove.addAll(MiscUtils.getEmbeddedTagNames(tags, tagId));

			for (var otherRootTag: getRootTags(tags)) {
				if (otherRootTag.equals(tagId)) {
					continue;
				}

				toRemove.removeAll(MiscUtils.getEmbeddedTagNames(tags, otherRootTag));
			}

			for (var tagName: toRemove) {
				tags.remove(tagName);
			}

			page.save();
			page.unlock();
		}
	}

	/**
	 * Get the names of all root tags.
	 * @param tags The content tags of the page.
	 * @return The names of all root tags.
	 */
	private Collection<String> getRootTags(Map<String, ContentTag> tags) {
		var rootTags = new HashSet<String>();

		for (var entry: tags.entrySet()) {
			if (entry.getValue().comesFromTemplate()) {
				rootTags.add(entry.getKey());
			}
		}

		return rootTags;
	}

	/**
	 * Get the channel id.
	 * @return The channel id.
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * Set the channel id.
	 * @param channelId The channel id.
	 * @return Fluent API
	 */
	public UnlocalizeTagJob setChannelId(Integer channelId) {
		this.channelId = channelId;

		return this;
	}

	/**
	 * Get the page id.
	 * @return The page id.
	 */
	public int getPageId() {
		return pageId;
	}

	/**
	 * Set the page id.
	 * @param pageId The page id.
	 * @return Fluent API
	 */
	public UnlocalizeTagJob setPageId(int pageId) {
		this.pageId = pageId;

		return this;
	}

	/**
	 * Get the tag id.
	 * @return The tag id.
	 */
	public String getTagId() {
		return tagId;
	}

	/**
	 * Set the tag id.
	 * @param tagId The tag id.
	 * @return Fluent API
	 */
	public UnlocalizeTagJob setTagId(String tagId) {
		this.tagId = tagId;

		return this;
	}
}
