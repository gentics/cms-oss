/*
 * @author Stefan Hepp
 * @date 25.12.2005
 * @version $Id: RenderUrlFactory.java,v 1.3 2007-01-03 12:20:16 norbert Exp $
 */
package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;

/**
 * RenderUrlFactory is used to create new urls.
 * @see RenderUrl
 */
public interface RenderUrlFactory {

	/**
	 * Create a new renderUrl with the given target information.
	 *
	 * @param targetObjClass the class of the target nodeobject to link to.
	 * @param targetObjId the id of the target nodeobject to link to.
	 * @return a new renderurl, with mode 'link'.
	 */
	RenderUrl createRenderUrl(Class targetObjClass, Integer targetObjId) throws NodeException;

	/**
	 * Get the current link management setting
	 * @return link management setting
	 */
	public LinkManagement getLinkManagement();

	/**
	 * Set the link management setting
	 * @param linkManagement link management
	 */
	public void setLinkManagement(LinkManagement linkManagement);

	/**
	 * Possible Settings for link management when rendering URLs to pages.
	 * The Link Management will render # as links to pages, that are offline
	 */
	public static enum LinkManagement {

		/**
		 * Link Management will always be done
		 */
		ON, /**
		 * Link Management will never be done
		 */ OFF, /**
		 * Link Management will be done depending on the feature "managelinkurl"
		 */ FEATURE;
	}
}
