/*
 * @author norbert
 * @date 07.05.2008
 * @version $Id: DummyGenticsContentObject.java,v 1.2 2010-09-28 17:01:27 norbert Exp $
 */
package com.gentics.lib.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.datasource.VersionedObject;

/**
 * @author norbert
 */
public class DummyGenticsContentObject implements GenticsContentObject {
	protected String contentid;

	protected int objType;

	protected int objId;

	/**
	 * Create an instance of the dummy object
	 * @param contentid contentid
	 */
	public DummyGenticsContentObject(String contentid) {
		if (contentid == null) {
			this.contentid = contentid;
			this.objType = 0;
			this.objId = 0;
		} else {
			this.contentid = contentid;
			int dotIndex = contentid.indexOf('.');

			if (dotIndex < 0) {
				objType = 0;
				objId = ObjectTransformer.getInt(contentid, 0);
			} else {
				objType = ObjectTransformer.getInt(contentid.substring(0, dotIndex), 0);
				objId = ObjectTransformer.getInt(contentid.substring(dotIndex + 1, contentid.length()), 0);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#exists()
	 */
	public boolean exists() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getAccessedAttributeNames(boolean)
	 */
	public String[] getAccessedAttributeNames(boolean omitMetaAttributes) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getAttribute(java.lang.String)
	 */
	public GenticsContentAttribute getAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getAttributeDefinitions()
	 */
	public List getAttributeDefinitions() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getContentId()
	 */
	public String getContentId() {
		return contentid;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getCustomUpdatetimestamp()
	 */
	public int getCustomUpdatetimestamp() {
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getDatasource()
	 */
	public Datasource getDatasource() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getModifiedAttributeNames()
	 */
	public String[] getModifiedAttributeNames() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getMotherContentId()
	 */
	public String getMotherContentId() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getMotherObjectId()
	 */
	public int getMotherObjectId() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getMotherObjectType()
	 */
	public int getMotherObjectType() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getObjectId()
	 */
	public int getObjectId() {
		return objId;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getObjectType()
	 */
	public int getObjectType() {
		return objType;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#getUpdateTimestamp()
	 */
	public int getUpdateTimestamp() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setAttribute(java.lang.String,
	 *      java.lang.Object[])
	 */
	public void setAttribute(String name, Object[] values) throws NodeIllegalArgumentException,
				CMSUnavailableException {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setAttribute(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setAttribute(String name, Object value) throws NodeIllegalArgumentException,
				CMSUnavailableException {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setCustomUpdatetimestamp(int)
	 */
	public void setCustomUpdatetimestamp(int timestamp) throws NodeIllegalArgumentException {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setMotherContentId(java.lang.String)
	 */
	public void setMotherContentId(String motherContentId) throws NodeIllegalArgumentException {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setMotherObject(com.gentics.lib.content.GenticsContentObject)
	 */
	public void setMotherObject(GenticsContentObject motherObject) {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setObjectId(int)
	 */
	public void setObjectId(int obj_id) {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Changeable#setProperty(java.lang.String,
	 *      java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		if ("contentid".equals(key)) {
			return getContentId();
		} else if ("obj_type".equals(key)) {
			return new Integer(getObjectType());
		} else if ("obj_id".equals(key)) {
			return new Integer(getObjectId());
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getCurrentVersion()
	 */
	public VersionedObject getCurrentVersion() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersion(int)
	 */
	public VersionedObject getVersion(int versionTimestamp) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersionDate()
	 */
	public Date getVersionDate() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersionTimestamp()
	 */
	public int getVersionTimestamp() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#getVersions()
	 */
	public Version[] getVersions() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isCurrentVersion()
	 */
	public boolean isCurrentVersion() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isFutureVersion()
	 */
	public boolean isFutureVersion() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isPastVersion()
	 */
	public boolean isPastVersion() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#isVersioned()
	 */
	public boolean isVersioned() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersionedObject#setVersionTimestamp(int)
	 */
	public void setVersionTimestamp(int versionTimestamp) {}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return contentid;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.content.GenticsContentObject#setPrefetchedAttribute(com.gentics.lib.content.DatatypeHelper.AttributeType, java.lang.Object)
	 */
	public void setPrefetchedAttribute(AttributeType attributeType, Object value) {}
    
	public void setAttributeNeedsSortorderFixed(String name) throws NodeIllegalArgumentException,
				CMSUnavailableException {}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getStreamableProperties()
	 */
	public Collection<String> getStreamableProperties() {
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#isStreamable(java.lang.String)
	 */
	public boolean isStreamable(String name) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getNumStreams(java.lang.String)
	 */
	public int getNumStreams(String name) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.StreamingResolvable#getInputStream(java.lang.String, int)
	 */
	public InputStream getInputStream(String name, int n) throws IOException,
				ArrayIndexOutOfBoundsException {
		throw new IOException("Attribute '" + name + "' is not streamable");
	}
}
