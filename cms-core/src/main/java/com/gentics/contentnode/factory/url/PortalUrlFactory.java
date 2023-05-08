/*
 * @author Stefan Hepp
 * @date 25.12.2005
 * @version $Id: PortalUrlFactory.java,v 1.3 2007-01-03 12:20:14 norbert Exp $
 */
package com.gentics.contentnode.factory.url;

import com.gentics.contentnode.render.RenderUrl;

/**
 * This factory creates url which work in a portal-environment.
 * The created urls are not to be written to a static filesystem or db, but used in
 * the current, active user session, and are therefore complete portlet-urls, not plinks.
 * For plink rendering, see {@link StaticUrlFactory}.
 *
 * TODO implement.. create links using portlet-urls? -> this should be moved to portal.node
 */
public class PortalUrlFactory extends AbstractRenderUrlFactory {

	public PortalUrlFactory() {}

	public RenderUrl createRenderUrl(Class targetObjClass, Integer targetObjId) {
		return null;
	}
}
