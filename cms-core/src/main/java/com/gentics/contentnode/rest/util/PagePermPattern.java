package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Variant of {@link PermPattern} which handles page role permission bits
 */
public class PagePermPattern extends PermPattern {
	@Override
	protected int getBit(PermType type) throws NodeException {
		int bit = type.getPageRoleBit();
		if (bit < 0) {
			throw new NodeException(String.format("%s does not have a page role bit", type));
		}
		return bit;
	}
}
