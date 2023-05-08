/*
 * @author herbert
 * @date 29.03.2006
 * @version $Id: PLinkReplacer.java,v 1.5 2006-05-29 11:22:09 laurin Exp $
 */
package com.gentics.api.portalnode.connector;

/**
 * Can be used in combination with
 * {@link PortalConnectorHelper#replacePLinks(String, PLinkReplacer)} to replace
 * plinks to real URLs.
 * @see com.gentics.api.portalnode.connector.PortalConnectorHelper#replacePLinks(String,
 *      PLinkReplacer)
 */
public interface PLinkReplacer {

	/**
	 * Has to be implemented by users. It gets the content id of the plink and
	 * should return a URL valid for your portal.
	 * @param plink the plink to which this plink points. You can retrieve the
	 *        content id using {@link PLinkInformation#getContentId()}
	 * @return URL which will replace the plink tag
	 * @see PLinkInformation
	 * @see PortalConnectorHelper
	 */
	public String replacePLink(PLinkInformation plink);
}
