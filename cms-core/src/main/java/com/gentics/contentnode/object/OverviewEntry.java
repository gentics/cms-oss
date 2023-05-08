/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: OverviewEntry.java,v 1.3 2007-01-03 12:20:14 norbert Exp $
 */
package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;

/**
 * This holds a single selected object for an overview.
 * The type of the selected objects depends on the settings of the overview.
 */
@SuppressWarnings("serial")
public abstract class OverviewEntry extends AbstractContentObject {

	protected OverviewEntry(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the id of the selected object.
	 * @return the id of the selected object.
	 */
	public abstract Integer getObjectId();

	/**
	 * Get the selected object
	 * @return selected object
	 * @throws NodeException
	 */
	public abstract NodeObject getObject() throws NodeException;

	/**
	 * Set the id of the selected object
	 * @param objectId id of the selected object
	 * @throws ReadOnlyException
	 */
	public void setObjectId(Integer objectId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the order number by which the elements are sorted when the ordertype is {@link Overview#ORDER_SELECT}.
	 * @return
	 */
	@FieldGetter("obj_order")
	public abstract int getObjectOrder();

	/**
	 * Set the order number
	 * @param objectOrder order number
	 * @throws ReadOnlyException
	 */
	@FieldSetter("obj_order")
	public void setObjectOrder(int objectOrder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the container tag which holds this overview. TODO check fallback if
	 * ds is defined in construct/template and not editable.
	 * @return the tag which holds this tag.
	 * @throws NodeException
	 */
	public abstract Tag getContainer() throws NodeException;

	/**
	 * Set the container tag
	 * @param container container tag
	 * @throws ReadOnlyException
	 */
	public void setContainer(Tag container) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the id of the user who authorized the selection.
	 * @return the id of the user who authorized the selection, or 0 if not authorized.
	 */
	public abstract Integer getAuthorizeUserId();

	/**
	 * Set the id of the user who authorized the selection
	 * @param aUserId id of the authorizing user
	 * @throws ReadOnlyException
	 */
	public void setAuthorizeUserId(Integer aUserId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * To be frank, I don't know, what ds_id is. It does not seem
	 * to be the id of the datasource as one may expect.
	 *
	 * @return the ds_id.
	 */
	public abstract Object getDatasourceId();

	/**
	 * This updates the reference of the OverviewEntry
	 * @param overview Overview containing this entry
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setOverview(Overview overview) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the overview
	 * @return overview
	 * @throws NodeException
	 */
	public abstract Overview getOverview() throws NodeException;

	/**
	 * Get the nodeId for which the object was selected
	 * @return node ID
	 */
	@FieldGetter("node_id")
	public abstract int getNodeId();

	/**
	 * Set the nodeId for the object
	 * @param nodeId node ID
	 * @throws ReadOnlyException
	 */
	@FieldSetter("node_id")
	public void setNodeId(int nodeId) throws ReadOnlyException {
		failReadOnly();
	}
}
