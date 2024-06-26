/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ObjectTag.java,v 1.15.6.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.model.ObjectTagModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;

/**
 * This is a special tag, which is linked to any objectcontainer as
 * object-property. TODO define ObjectTagDefinition object, which contains
 * keyname, construct, inheritable, object-class for each keyname, add getter.
 */
@TType(Tag.TYPE_OBJECTTAG)
public abstract class ObjectTag extends Tag {
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -7428180731103497047L;

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<ObjectTag, ObjectTagModel, ObjectTagModel> NODE2DEVTOOL = (from, to) -> {
		Tag.NODE2DEVTOOL.apply(from, to);
		return to;
	};

	protected ObjectTag(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	public String getTypeKeyword() {
		return "objtag";
	}

	/**
	 * Check if this objecttag exists in another tag
	 * @return true for embedded objecttags
	 */
	public abstract boolean isIntag();

	/**
	 * Get the ID of the parent object tag
	 * @return ID or 0
	 */
	@FieldGetter("intag")
	public abstract Integer getInTagId();

	/**
	 * Set the ID of the parent object tag
	 * @param intagId  ID or 0 for no parent object
	 * @throws ReadOnlyException
	 */
	@FieldSetter("intag")
	public void setIntagId(Integer intagId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the parent object tag
	 * @return Object tag or null if none
	 * @throws NodeException
	 */
	public abstract ObjectTag getInTagObject() throws NodeException;

	/**
	 * check if this objecttag should be inherited to sub-objects.
	 * @return true, if this object should be inherited.
	 */
	@FieldGetter("inheritable")
	public abstract boolean isInheritable();

	/**
	 * Set whether this objecttag should be inherited to sub-objects
	 * @param inheritable true for inherited objecttags
	 * @throws ReadOnlyException
	 */
	@FieldSetter("inheritable")
	public void setInheritable(boolean inheritable) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether this objecttag is required
	 * @return true for required objecttags
	 */
	@FieldGetter("required")
	public abstract boolean isRequired();

	/**
	 * Set whether the objecttag is required
	 * @param required true for required objecttags
	 * @throws ReadOnlyException
	 */
	@FieldSetter("required")
	public void setRequired(boolean required) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the objType of the container
	 * @return objType of the container
	 */
	@FieldGetter("obj_type")
	public abstract int getObjType();

	/**
	 * Set the objType of the container
	 * @param objType objType
	 * @throws ReadOnlyException
	 * @throws NodeException 
	 */
	@FieldSetter("obj_type")
	public void setObjType(int objType) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	public TagContainer getContainer() {
		try {
			NodeObject nodeObject = getNodeObject();

			if (nodeObject instanceof TagContainer) {
				return (TagContainer) nodeObject;
			} else {
				return null;
			}
		} catch (NodeException e) {
			logger.error("Error while getting container for " + this, e);
			return null;
		}
	}

	/**
	 * Get the container of the ObjectTag as ObjectTagContainer
	 * @return container
	 * @throws NodeException
	 */
	public ObjectTagContainer getObjectTagContainer() throws NodeException {
		NodeObject nodeObject = getNodeObject();
		if (nodeObject instanceof ObjectTagContainer) {
			return (ObjectTagContainer) nodeObject;
		} else {
			throw new NodeException(String.format("Cannot get container of %s as ObjectTagContainer. Container is %s", this, nodeObject));
		}
	}

	public String getStackHashKey() {
		return "objtag:" + getHashKey();
	}

	/**
	 * Get the object owning this objecttag
	 * @return object holding the tag
	 * @throws NodeException
	 */
	public abstract NodeObject getNodeObject() throws NodeException;

	/**
	 * Get the object tag definition. This method will return null if the tag does not have a proper object tag definition.
	 * @return object tag definition (may be null)
	 * @throws NodeException
	 */
	public abstract ObjectTagDefinition getDefinition() throws NodeException;

	/**
	 * Get the category, or null if none assigned
	 * @return category (may be null)
	 * @throws NodeException
	 */
	public ObjectTagDefinitionCategory getCategory() throws NodeException {
		ObjectTagDefinition def = getDefinition();
		if (def != null) {
			return def.getCategory();
		} else {
			return null;
		}
	}

	/**
	 * Get display name
	 * @return display name
	 * @throws NodeException
	 */
	public String getDisplayName() throws NodeException {
		ObjectTagDefinition def = getDefinition();
		if (def != null) {
			return def.getName();
		} else {
			return getName();
		}
	}

	/**
	 * Set the node object owning this objecttag
	 * @param owner owning node object
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public void setNodeObject(NodeObject owner) throws NodeException, ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Set the parent object tag
	 * @param objectTag  Parent object tag
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public void setInTagObject(ObjectTag objectTag) throws NodeException, ReadOnlyException {
		failReadOnly();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode.events.DependencyObject,
	 *      java.lang.String[], int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask,
			int depth, int channelId) throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);

		// objecttag was updated or created?
		if (Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.CREATE)) {
			// trigger event for the owning object
			NodeObject owner = getNodeObject();
			String name = getName();

			if (owner != null) {
				owner.triggerEvent(new DependencyObject(owner, (NodeObject) null), new String[] { name}, Events.UPDATE, depth + 1, 0);
			}
		}
	}

	@Override
	public Integer getTType() {
		return Tag.TYPE_OBJECTTAG;
	}

	/**
	 * Migrate the object tag to the construct of the object property definition, it it is different
	 * @return true if the object tag was migrated, false if not
	 * @throws NodeException
	 */
	public boolean migrateToDefinedConstruct() throws NodeException {
		ObjectTagDefinition definition = getDefinition();
		if (definition == null) {
			return false;
		}
		Construct definedConstruct = definition.getObjectTag().getConstruct();
		if (!definedConstruct.equals(getConstruct())) {
			migrateToConstruct(definedConstruct);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Synchronize this object tag in the variants configured if the feature is enabled
	 * @throws NodeException
	 */
	public abstract void sync() throws NodeException;

	/**
	 * Check whether the object tag is in sync with the object tags of the required variants and return IDs of all other object tags, which were checked
	 * @return IDs of all object tags, which were also checked (including this)
	 * @throws NodeException
	 */
	public abstract Set<Integer> checkSync() throws NodeException;

	/**
	 * When this object tag has to be synchronized with variants, get all variants with the respective object tags.
	 * @return set of pairs of node object and object tag
	 * @throws NodeException
	 */
	public abstract Set<Pair<NodeObject, ObjectTag>> getSyncVariants() throws NodeException;

	/**
	 * Check whether this object tag has the same content as the other
	 * @param other other object tag
	 * @return true iff tags have same content
	 * @throws NodeException
	 */
	public abstract boolean hasSameContent(ObjectTag other) throws NodeException;
}
