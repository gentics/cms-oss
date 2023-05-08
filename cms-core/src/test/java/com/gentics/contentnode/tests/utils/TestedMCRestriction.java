package com.gentics.contentnode.tests.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Node;

/**
 * Enum for tested multichannelling restrictions
 */
public enum TestedMCRestriction {
	/**
	 * Object is fully inherited in all channels
	 */
	inherited,

	/**
	 * Object is disinherited from some channels
	 */
	disinherited,

	/**
	 * Object is completely excluded from inheritance
	 */
	excluded;

	/**
	 * Set the restrictions on the given object
	 * @param object object to restrict
	 * @param channels optional list of channels to disinherit (at least one channel must be given to disinherit)
	 * @throws NodeException
	 */
	public void set(Disinheritable<?> object, Node... channels) throws NodeException {
		switch (this) {
		case inherited:
			object.changeMultichannellingRestrictions(false, Collections.emptySet(), true);
			break;
		case disinherited:
			if (channels.length == 0) {
				throw new NodeException("Cannot disinherit object, when no channels are given");
			}
			object.changeMultichannellingRestrictions(false, new HashSet<>(Arrays.asList(channels)), true);
			break;
		case excluded:
			object.changeMultichannellingRestrictions(true, Collections.emptySet(), true);
			break;
		}
	}
}
