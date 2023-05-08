package com.gentics.contentnode.rest.util;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.lib.etc.StringUtils;

public class RequestParamHelper {

	/**
	 * Helper class to test if an attribute is in the given embed params.
	 * @param embed Comma separated list of attributes
	 * @param attribute Attribute to test
	 * @return true if given attribute is in the list
	 */
	public static boolean embeddedParameterContainsAttribute(EmbedParameterBean embed, String attribute) {
		if (embed != null && !StringUtils.isEmpty(embed.embed)) {
			for (String embedAttrs : embed.embed.split(",")) {
				if (StringUtils.isEqual(embedAttrs.trim(), attribute)) {
					return true;
				}
			}
		}
		return false;
	}

}
