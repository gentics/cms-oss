/*
 * @author Stefan Hepp
 * @date 12.12.2005
 * @version $Id: NodeObjectInfo.java,v 1.4.20.1.2.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.object;

import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.NodeFactory;

/**
 * Interface to store objectType-infos like editable, version, editUser.
 */
public interface NodeObjectInfo {

	/**
	 * Get the class of the object by which it was requested from the factory.
	 * This is not the class of the factory-implementation.
	 *
	 * @return the class of the object.
	 */
	Class<? extends NodeObject> getObjectClass();

	/**
	 * Check, if this object is an editable instance.
	 * @return true, if this object is editable, else false.
	 */
	boolean isEditable();

	/**
	 * get the edituser-id of the current object.
	 * If this object is not editable, this must return 0.
	 *
	 * @return userId of the user who edits this object, else 0
	 */
	int getEditUserId();

	/**
	 * get a hashkey for this type of object-instance.
	 * This is used for internal caching.
	 *
	 * @return an identifier for this type of object.
	 */
	String getHashKey();

	/**
	 * get the factory with which this object was created.
	 * @return the factory of this object.
	 */
	NodeFactory getFactory();

	/**
	 * get the current configuration used to create this object.
	 * @return the configuration used for this object.
	 */
	NodeConfig getConfiguration();

	/**
	 * Get the versiontimestamp of the version. -1 will be returned for the current version.
	 * @return versiontimestamp (-1 for current version)
	 */
	int getVersionTimestamp();

	/**
	 * Check whether the object is the current version
	 * @return true for the current version, false for another version
	 */
	boolean isCurrentVersion();

	/**
	 * Get subinfo object. The info will point to the same NodeFactory and NodeConfig and will
	 * have the same setting for versiontimestamp and editable
	 * @param clazz object class
	 * @return instance for a subobject
	 */
	NodeObjectInfo getSubInfo(Class<? extends NodeObject> clazz);
}
