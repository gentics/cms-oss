package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.etc.StringUtils;

/**
 * Utility class to create a permission pattern.
 * The string representation consists of 32 characters, each one of
 * <ul>
 * <li><b>.</b>: Do not change bit</li>
 * <li><b>1</b>: Set bit to '1' (granting permission)</li>
 * <li><b>0</b>: Set bit to '0' (revoking permission)</li>
 * </ul>
 */
public class PermPattern {
	/**
	 * Pattern representation
	 */
	private char[] characters = StringUtils.repeat(".", 32).toCharArray();

	/**
	 * Grant the given permission (set the according bit in the pattern to '1').
	 * @param type permission type
	 * @return fluent API
	 * @throws NodeException
	 */
	public PermPattern grant(PermType type) throws NodeException {
		characters[getBit(type)] = '1';
		return this;
	}

	/**
	 * Revoke the given permission (set the according bit in the pattern to '0').
	 * @param type permission type
	 * @return fluent API
	 * @throws NodeException
	 */
	public PermPattern revoke(PermType type) throws NodeException {
		characters[getBit(type)] = '0';
		return this;
	}

	@Override
	public String toString() {
		return String.valueOf(characters);
	}

	/**
	 * Get the bit of the permission type
	 * @param type permission type
	 * @return bit
	 * @throws NodeException
	 */
	protected int getBit(PermType type) throws NodeException {
		if (type == null) {
			throw new NodeException("Cannot get permission bit of null type");
		}
		return type.getBit();
	}
}
