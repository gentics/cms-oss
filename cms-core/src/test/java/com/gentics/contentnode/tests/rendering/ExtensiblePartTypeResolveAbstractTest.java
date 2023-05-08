/*
 * @author johannes
 * @date 13.10.2008
 * @version $Id: ExtensiblePartTypeResolveTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.rendering;

import com.gentics.api.contentnode.parttype.AbstractExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;

public class ExtensiblePartTypeResolveAbstractTest extends AbstractExtensiblePartType {

	public String render() throws NodeException {
		String param1 = resolveToString("otherpart");

		return param1;
	}
}
