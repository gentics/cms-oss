package com.gentics.contentnode.publish;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.cr.DummyTagmapEntry;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Renderer for the path in the "publish" table
 */
public class PublishtablePathRenderer extends DummyTagmapEntry {
	/**
	 * Page
	 */
	protected Page page;

	/**
	 * Create an instance
	 * @param page page
	 */
	public PublishtablePathRenderer(Page page) {
		super(Page.TYPE_PAGE, null, FolderFactory.DUMMY_DIRT_ATTRIBUTE, GenticsContentAttribute.ATTR_TYPE_TEXT, 0);
		this.page = page;
	}

	@Override
	public Object getRenderedTransformedValue(RenderType renderType, RenderResult renderResult, BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer)
			throws NodeException {
		final Folder folder = page.getFolder();
		final Node node = folder.getNode();

		return FilePublisher.getPath(false, true, node.getHostname(), StaticUrlFactory.getPublishPath(page, false));
	}
}
