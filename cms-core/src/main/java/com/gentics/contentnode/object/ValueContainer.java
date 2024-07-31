/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ValueContainer.java,v 1.18 2010-05-04 13:26:19 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * The value container is a base implementation for all objects which hold a list of values.
 */
public abstract class ValueContainer extends AbstractContentObject implements StackResolvable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8185809276266616377L;

	public static final String[] RENDER_KEYS = new String[] { "tag"};

	protected final static Set<String> resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, SetUtils.hashSet("values", "parts", "count"));

	public ValueContainer(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * get a list of all values of this container.
	 * @return the list of all values of this container.
	 * @throws NodeException TODO
	 */
	public abstract ValueList getValues() throws NodeException;

	/**
	 * get the construct linked to this container.
	 * @return the construct of this container.
	 * @throws NodeException TODO
	 */
	public abstract Construct getConstruct() throws NodeException;

	/**
	 * Get the construct ID
	 * @return construct ID
	 * @throws NodeException
	 */
	public abstract Integer getConstructId() throws NodeException;

	/**
	 * return a keyname of the type of this container.
	 * Example: 'contenttag' for contenttags.
	 **/
	public abstract String getTypeKeyword();

	/**
	 * get all values of this container, merged with the static default values
	 * of the construct.
	 * @return a valuelist of all static and non-static values of this container.
	 * @throws NodeException TODO
	 */
	public ValueList getTagValues() throws NodeException {

		ValueList myValues = getValues();

		List parts = getConstruct().getParts();

		EditableValueList myTagValues = new EditableValueList(getId() + "-" + super.get("ttype"));

		for (int i = 0; i < parts.size(); i++) {
			Part part = (Part) parts.get(i);

			Value value = myValues.getByPartId(part.getId());
			PartType partType = null;

			if (value != null) {
				// we only need the part type if we have a value..
				partType = part.getPartType(value);
			}

			// normal parts have the normal defaultvalue behaviour
			if ((part.isEditable() || partType instanceof OverviewPartType) && value != null) {
				myTagValues.addValue(value);
			} else if (!part.isEditable()) {
				// only add default value when part is not editable
				myTagValues.addValue(part.getDefaultValue());
			}

		}

		return myTagValues;
	}

	public Object get(String key) {
		try {
			Object value = null;
			ValueList values = null;

			try {
				values = getTagValues();
			} catch (NodeException e) {
				logger.error("Error while resolving {" + key + "}", e);
			}

			// only try to resolve the part by its name, when this is allowed
			if (values != null && resolvePartsWithShortCuts()) {
				value = values.get(key);
			}
			if (value == null) {
				if ("values".equals(key) || "parts".equals(key)) {
					return values;
				} else if ("count".equals(key)) {
					return values != null ? getOverviewCountFromPart(values) : null;
				} else if ("tag".equals(key)) {
					return this;
				} else {
					return super.get(key);
				}
			} else {
				return value;
			}
		} catch (NodeException e) {
			logger.error("Error while resolving {" + key + "}", e);
			return null;
		}
	}

	protected Object getOverviewCountFromPart(ValueList values) throws NodeException {
		for (Value value : values) {
			PartType part = value.getPartType();

			if (part instanceof OverviewPartType) {
				return part.get("count");
			}
		}

		return null;
	}
    
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return this;
	}

	public String[] getStackKeywords() {
		return RENDER_KEYS;
	}

	public Resolvable getShortcutResolvable() throws NodeException {
		return getTagValues();
	}

	/**
	 * Method to determine whether parts shall be resolved by their shortcuts or
	 * not
	 * @return true when the parts shall be resolvable by their shortcuts, false
	 *         if not
	 */
	protected abstract boolean resolvePartsWithShortCuts();

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the container's udate
		int udate = getUdate();
		// check the values
		ValueList values = getValues();

		for (Value v : values) {
			udate = Math.max(udate, v.getEffectiveUdate());
		}
		return udate;
	}
}
