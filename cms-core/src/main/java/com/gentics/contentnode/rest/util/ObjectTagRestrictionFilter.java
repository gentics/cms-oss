package com.gentics.contentnode.rest.util;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;

/**
 * Filter for node restrictions of object tags. A tag only matches if it is not
 * restricted to nodes, or is restricted to the given node.
 */
public class ObjectTagRestrictionFilter extends AbstractNodeObjectFilter {
	private Node node;

	/**
	 * Create instance for the given node (which must not be null or a channel)
	 * @param node node
	 */
	public ObjectTagRestrictionFilter(Node node) {
		this.node = node;
	}

	@Override
	public boolean matches(NodeObject object) throws NodeException {
		if (object instanceof ObjectTag) {
			try (NoMcTrx noMc = new NoMcTrx()) {
				ObjectTag tag = (ObjectTag) object;
				ObjectTagDefinition def = tag.getDefinition();
				// no definition found, so no restriction can be active
				if (def == null) {
					return true;
				}
				List<Node> restricted = def.getNodes();
				// object property is not restricted
				if (restricted.isEmpty()) {
					return true;
				}
				// check whether object property is allowed for the node
				return restricted.contains(node);
			}
		}
		return false;
	}
}
