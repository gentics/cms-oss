package com.gentics.contentnode.publish.mesh;

import java.util.HashSet;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.rest.util.AbstractNodeObjectFilter;

/**
 * Abstract base class for Micronode Filters
 */
public abstract class AbstractMeshMicronodeFilter extends AbstractNodeObjectFilter {
	/**
	 * Whitelist
	 */
	protected Set<String> whiteList = new HashSet<>();

	/**
	 * Blacklist
	 */
	protected Set<String> blackList = new HashSet<>();

	/**
	 * Create an instance for the given array of keywords
	 * @param keywords keywords
	 */
	public AbstractMeshMicronodeFilter(String[] keywords) {
		for (String keyword : keywords) {
			if (keyword.startsWith("-")) {
				blackList.add(keyword.replaceAll("\\+", "").replaceAll("\\-", ""));
			} else {
				whiteList.add(keyword.replaceAll("\\+", "").replaceAll("\\-", ""));
			}
		}
	}

	@Override
	public boolean matches(NodeObject object) throws NodeException {
		if (object instanceof Construct) {
			return matches(((Construct)object).getKeyword());
		} else if(object instanceof Tag) {
			return matches(((Tag)object).getConstruct().getKeyword());
		} else {
			return false;
		}
	}

	/**
	 * Check whether the keyword matches
	 * @param keyword keyword to match
	 * @return true iff keyword matches
	 */
	protected abstract boolean matches(String keyword);
}
