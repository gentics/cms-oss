package com.gentics.contentnode.object.parttype;

import java.util.Objects;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 40 - Node
 */
public class NodePartType extends AbstractPartType {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -1457491512350096672L;

	public NodePartType(Value value) throws NodeException {
		super(value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	@Override
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isMandatoryAndNotFilledIn()
	 */
	@Override
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}

		return getValueObject().getValueRef() <= 0;
	}

	/**
	 * Get the property named <code>key</code>.
	 *
	 * The properties <code>id</code> and <code>name</code> will be resolved directly,
	 * all other properties will be relayed to the {@link Node} this part type represents.
	 *
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#get(java.lang.String)
	 *
	 * @param key The key of the property to get.
	 * @return The property named <code>key</code> or <code>null</code> if no such property exists.
	 */
	@Override
	public Object get(String key) {
		int nodeId = getValueObject().getValueRef();
		Node node = null;

		if ("id".equals(key)) {
			return nodeId;
		}

		try (ChannelTrx trx = new ChannelTrx(nodeId)) {
			node = TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);

			if (node == null) {
				logger.error("Cannot resolve \"" + key + "\": Node {id: " + nodeId + "} not found");

				return null;
			}

			if ("name".equals(key)) {
				return node.getFolder().getName();
			}

			return node.get(key);
		} catch (NodeException e) {
			logger.error("Cannot resolve \"" + key + "\" on Node {id: " + nodeId + "}", e);
		}

		return null;
	}

	/**
	 * Renders the ID of the {@link Node} this part type represents.
	 *
	 * Note that this function will return <code>null</code> if the <code>Node</code>
	 * with the ID stored in this part type does not exist.
	 *
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#render(com.gentics.contentnode.render.RenderResult, java.lang.String)
	 */
	@Override
	public String render(RenderResult result, String template) {
		int id = getValueObject().getValueRef();
		String ret = null;

		try (ChannelTrx trx = new ChannelTrx(id)) {
			if (TransactionManager.getCurrentTransaction().getObject(Node.class, id) == null) {
				logger.warn("The node with ID " + id + " does not exist");
			} else {
				ret = String.valueOf(id);
			}
		} catch (NodeException e) {
			logger.error("Could not get node with ID " + id, e);
		}

		return ret;
	}

	/**
	 * Set the node this part should represent.
	 *
	 * @param node The node this part should represent.
	 * @throws ReadOnlyException When the value is read-only.
	 */
	public void setNode(Node node) throws ReadOnlyException {
		getValueObject().setValueRef(node == null ? 0 : ObjectTransformer.getInt(node.getId(), 0));
	}

	/**
	 * Get the node this part represents.
	 *
	 * @return The node this part represents.
	 */
	public Node getNode() {
		int nodeId = getValueObject().getValueRef();

		if (nodeId <= 0) {
			return null;
		}

		try {
			return TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);
		} catch (NodeException e) {
			logger.error("Error while getting target", e);

			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Value value = getValueObject();

		return "NodePartType {value id: " + value.getId() + " value ref: " + value.getValueRef() + "}";
	}

	@Override
	public Type getPropertyType() {
		return Type.NODE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		NodeObject node = getNode();

		if (node != null) {
			property.setNodeId(ObjectTransformer.getInteger(node.getId(), null));
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		getValueObject().setValueRef(ObjectTransformer.getInt(property.getNodeId(), 0));
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof NodePartType) {
			return Objects.equals(getNode(), ((NodePartType) other).getNode());
		} else {
			return false;
		}
	}
}
