package com.gentics.contentnode.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Callable implementation for localizing a root tag.
 */
public class LocalizeRootTagCallable extends AbstractLocalizeCallable {

	/** The page ID. */
	private final int pageId;

	/** The tag ID. */
	private final String tagId;

	/**
	 * Create an instance for the given page, channel and tag.
	 * @param pageId The page ID.
	 * @param channelId The channel ID.
	 * @param tagId The tag ID.
	 * @param disableInstantPublish Whether to disable instant publishing.
	 */
	public LocalizeRootTagCallable(int pageId, int channelId, String tagId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);

		this.pageId = pageId;
		this.tagId = tagId;
	}

	/**
	 * Create an instance for the given page and channel without disabling instant publishing.
	 * @param pageId The page ID.
	 * @param channelId The channel ID.
	 * @param tagId The tag ID.
	 */
	public LocalizeRootTagCallable(int pageId, int channelId, String tagId) {
		this(pageId, channelId, tagId, false);
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish); ChannelTrx ct = new ChannelTrx(channelId)) {
			var t = TransactionManager.getCurrentTransaction();
			var page = t.getObject(Page.class, pageId, true);
			var content = page.getContent();

			content.setModified(true);

			var tags = content.getContentTags();
			var toBeLocalized = new LinkedList<String>();
			var localizedTags = new HashSet<String>();
			var tagNames = tags.keySet();
			var toBeRenamed = new HashMap<String, List<Value>>();

			toBeLocalized.add(tagId);

			while (!toBeLocalized.isEmpty()) {
				ContentTag localizedTag;
				var origTagKey = toBeLocalized.pop();
				var origTag = tags.get(origTagKey);

				tags.remove(origTagKey);

				if (origTag.comesFromTemplate()) {
					localizedTag = (ContentTag) origTag.copy();
					localizedTag.setName(origTagKey);
					tags.put(origTagKey, localizedTag);
				} else {
					localizedTag = content.addContentTag(origTag.getConstructId());

					var localizedTagName = localizedTag.getName();

					localizedTag.copyFrom(origTag);
					localizedTag.setName(localizedTagName);

					if (toBeRenamed.containsKey(origTagKey)) {
						for (var value: toBeRenamed.get(origTagKey)) {
							var curText = value.getValueText();
							var newText = curText.replace("<node " + origTagKey + ">", "<node " + localizedTag.getName() + ">");

							value.setValueText(newText);
						}
					}
				}

				localizedTags.add(origTagKey);

				var construct = localizedTag.getConstruct();
				var values = localizedTag.getValues();

				for (var part: construct.getParts()) {
					var key = part.getKeyname();
					var val = values.getByKeyname(key);

					if (val == null) {
						continue;
					}

					var text = val.getValueText();

					if (StringUtils.isEmpty(text)) {
						continue;
					}

					for (var embeddedTag: tagNames) {
						if (!localizedTags.contains(embeddedTag) && text.contains("<node " + embeddedTag + ">")) {
							toBeRenamed.computeIfAbsent(embeddedTag, k -> new ArrayList<>()).add(val);
							toBeLocalized.add(embeddedTag);
						}
					}
				}
			}

			page.save();
			page.unlock();

			I18nString message = new CNI18nString("page.localize_root_tag.success");
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, message.toString()));
		}
	}
}
