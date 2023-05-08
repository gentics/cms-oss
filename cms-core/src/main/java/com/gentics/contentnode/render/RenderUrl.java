/*
 * @author Stefan Hepp
 * @date 31.01.2006
 * @version $Id: RenderUrl.java,v 1.4 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;

/**
 * A RenderUrl can be used to create a new url to a nodeobject. The syntax of the created
 * url depends on the implementation. RenderUrls are created by a RenderUrlFactory.
 * @see RenderUrlFactory
 */
public interface RenderUrl {

	/**
	 * linkway include host in link
	 */
	final static int LINK_HOST = 0x01;

	/**
	 * linkway allow relative links
	 */
	final static int LINK_REL = 0x02;

	/**
	 * linkway allow auto-select of relative or absolute link-mode.
	 */
	final static int LINK_AUTO = 0x04;

	/**
	 * linkway create plinks
	 */
	final static int LINK_PORTAL = 0x10;

	/**
	 * Shortcut for old content.node linkway 'host'
	 */
	final static int LINKWAY_HOST = LINK_HOST;

	/**
	 * Shortcut for old content.node linkway 'abs'
	 */
	final static int LINKWAY_ABS = 0x00;

	/**
	 * Shortcut for old content.node linkway 'auto'
	 */
	final static int LINKWAY_AUTO = LINK_AUTO;

	/**
	 * Shortcut for old content.node linkway 'portal'
	 */
	final static int LINKWAY_PORTAL = LINK_PORTAL;

	/**
	 * Link mode for standard links to objects.
	 */
	final static int MODE_LINK = 0;

	/**
	 * Link mode for edit-links for objects.
	 */
	final static int MODE_EDIT = 1;

	/**
	 * Set the mode for the target. This may be edit or link.
	 * @param mode the mode for the link.
	 */
	void setMode(int mode);

	/**
	 * Set the source from which this link points. This is needed for relative links.
	 * @param source the origin of the link.
	 * @throws NodeException 
	 */
	void setSourceObject(NodeObject source) throws NodeException;

	/**
	 * get the current link mode.
	 * @return
	 */
	int getMode();

	/**
	 * get the class of the target nodeobject.
	 * @return the class of the target nodeobject.
	 */
	Class getTargetClass();

	/**
	 * get the id of the target nodeobject.
	 * @return the id of the target nodeobject.
	 */
	Object getTargetId();

	/**
	 * get the source from which this links points.
	 * @return the origin of the link, or null if not set.
	 */
	NodeObject getSourceObject();

	/**
	 * create a new url with the current information. 
	 * @return the url as string.
	 */
	String toString();

	/**
	 * Get the current link management setting
	 * @return link management setting
	 */
	public LinkManagement getLinkManagement();
}
