/*
 * @author johannes2
 * @date 03.07.2009
 * @version $Id: GCNPermission.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.nodecopy.util;

import com.gentics.contentnode.perm.PermHandler.Permission;

public class GCNPermission extends Permission {

	private int usergroupId; 
	private int objectType;
	private int objectId;
    
	public GCNPermission(int usergroupId, int objectType, int objectId) {
		super();
		this.usergroupId = usergroupId;
		this.objectType = objectType;
		this.objectId = objectId;
		bits = new byte[32];
	}
 
	/**
	 * Create an instance of the Permission with the given bits (set as
	 * string of "0" and "1")
	 * @param bitsAsString bits
	 */
	public GCNPermission(int usergroupId, int objectType, int objectId, String bitsAsString) {
		this(usergroupId, objectType, objectId);
        
		if (bitsAsString.matches("^[0-1]*$")) {
			int c = bitsAsString.length();

			bits = new byte[bitsAsString.length()];
			for (int i = 0; i < c; ++i) {
				bits[i] = "1".equals(bitsAsString.substring(i, i + 1)) ? (byte) 1 : (byte) 0;
			}
		}
	}

	public int getUsergroupId() {
		return this.usergroupId;
	}

	public int getObjectType() {
		return this.objectType;
	}

	public int getObjectId() {
		return this.objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	public void setObjectType(int objectType) {
		this.objectType = objectType;
	}

	public void setUserGroup(int usergroupId) {
		this.usergroupId = usergroupId;
	}
    
	public void grantAll() {
		for (int i = 0; i < bits.length; i++) {
			bits[i] = (byte) 1;
		}
	}
    
	public void setPermission(int permissionType, boolean value) {
		bits[permissionType] = value ? (byte) 1 : (byte) 0;
	}
    
}
