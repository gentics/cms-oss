package com.gentics.contentnode.factory.url;

import com.gentics.contentnode.render.RenderUrlFactory;

/**
 * Abstract Implementation of a RenderUrlFactory
 */
public abstract class AbstractRenderUrlFactory implements RenderUrlFactory {

	/**
	 * Setting for the link management
	 */
	private LinkManagement linkManagement = LinkManagement.FEATURE;

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderUrlFactory#getLinkManagement()
	 */
	public LinkManagement getLinkManagement() {
		return linkManagement;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderUrlFactory#setLinkManagement(com.gentics.lib.render.RenderUrlFactory.LinkManagement)
	 */
	public void setLinkManagement(LinkManagement linkManagement) {
		this.linkManagement = linkManagement;
	}
}
