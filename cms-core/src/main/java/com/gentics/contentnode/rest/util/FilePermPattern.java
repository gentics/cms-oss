package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Variant of {@link PermPattern} which handles file role permission bits
 */
public class FilePermPattern extends PermPattern {
	@Override
	protected int getBit(PermType type) throws NodeException {
		int bit = type.getFileRoleBit();
		if (bit < 0) {
			throw new NodeException(String.format("%s does not have a file role bit", type));
		}
		return bit;
	}
}
