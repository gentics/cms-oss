/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: Privilege.java,v 1.1.6.1 2011-03-17 13:38:55 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Enumeration for privileges (permissions)
 * @author norbert
 */
@XmlEnum(String.class)
public enum Privilege implements Serializable {

	/**
	 * Permission to view a folder
	 */
	viewfolder(0),

	/**
	 * Permission to create folders
	 */
	createfolder(8),

	/**
	 * Permission to update a folder
	 */
	updatefolder(9),

	/**
	 * Permission to delete a folder
	 */
	deletefolder(10),

	/**
	 * Permission to assign permissions
	 */
	assignpermissions(1),

	/**
	 * Permission to view pages
	 */
	viewpage(11, 10007, 10),

	/**
	 * Permission to create pages
	 */
	createpage(12, 10007, 11),

	/**
	 * Permission to update pages
	 */
	updatepage(13, 10007, 12),

	/**
	 * Permission to delete pages
	 */
	deletepage(14, 10007, 13),

	/**
	 * Permission to publish pages
	 */
	publishpage(19, 10007, 14),

	/**
	 * Permission to translate pages
	 */
	translatepage(-1, 10007, 15),

	/**
	 * Permission to view files/images
	 */
	viewfile(11, 10008, 10),

	/**
	 * Permission to create files/images
	 */
	createfile(12, 10008, 11),

	/**
	 * Permission to update files
	 */
	updatefile(13, 10008, 12),

	/**
	 * Permission to delete files
	 */
	deletefile(14, 10008, 13),

	/**
	 * Permission to view templates
	 */
	viewtemplate(15),

	/**
	 * Permission to create templates
	 */
	createtemplate(16),

	/**
	 * Permission to link templates
	 */
	linktemplate(21),

	/**
	 * Permission to update templates
	 */
	updatetemplate(17),

	/**
	 * Permission to delete templates
	 */
	deletetemplate(18),

	/**
	 * Permission to update tag types
	 */
	updatetagtypes(20),

	/**
	 * Permission to change object inheritance
	 */
	inheritance(29),

	/**
	 * Permission to import pages
	 */
	importpage(23),

	/**
	 * Permission to link workflows
	 */
	linkworkflow(22),

	/**
	 * Permission to synchronize objects between channels
	 */
	synchronizechannel(27),

	/**
	 * Permission to view the wastebin
	 */
	wastebin(28);

	/**
	 * Permission Bit
	 */
	private int permBit;

	/**
	 * Check type for role bit
	 */
	private int roleCheckType;

	/**
	 * Role bit
	 */
	private int roleBit;

	/**
	 * Create an instance
	 * @param permBit permission bit
	 */
	private Privilege(int permBit) {
		this(permBit, -1, -1);
	}

	/**
	 * Create an instance with permbit and rolebit
	 * @param permBit
	 * @param roleBit
	 */
	private Privilege(int permBit, int roleCheckType, int roleBit) {
		this.permBit = permBit;
		this.roleCheckType = roleCheckType;
		this.roleBit = roleBit;
	}

	/**
	 * Get the perm bit
	 * @return perm bit
	 */
	public int getPermBit() {
		return permBit;
	}

	/**
	 * Get the role check type
	 * @return role check type
	 */
	public int getRoleCheckType() {
		return roleCheckType;
	}

	/**
	 * Get the role bit
	 * @return role bit
	 */
	public int getRoleBit() {
		return roleBit;
	}

	/**
	 * Privileges available on folders
	 */
	private static Set<Privilege> validForFolder = new HashSet<>(Arrays.asList(assignpermissions, importpage, linktemplate, publishpage,
			linkworkflow, viewfolder, viewfile, viewpage, viewtemplate, createfolder, createpage, createfile, createtemplate, updatefolder, updatepage,
			updatefile, updatetemplate, deletefolder, deletepage, deletefile, deletetemplate, translatepage));

	/**
	 * Privileges available on nodes
	 */
	private static Set<Privilege> validForNode = new HashSet<>(Arrays.asList(updatetagtypes, synchronizechannel, wastebin, inheritance));

	/**
	 * Get the privilege by perm bit
	 * @param permBit perm bit
	 * @return privilege or null
	 */
	public static Privilege getPrivilege(int permBit) {
		for (Privilege p : Privilege.values()) {
			if (p.permBit == permBit) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Get the privileges available on objects of the given type
	 * @param objType object type
	 * @return set of available privileges
	 */
	public static Set<Privilege> getAvailable(int objType) {
		switch (objType) {
		case 10001:
			return validForNode;
		case 10002:
			return validForFolder;
		default:
			return Collections.emptySet();
		}
	}

	/**
	 * Get the privileges for the given role check type
	 * @param roleCheckType role check type
	 * @return set of privileges
	 */
	public static Set<Privilege> forRoleCheckType(int roleCheckType) {
		return Stream.of(values()).filter(priv -> priv.roleCheckType == roleCheckType).collect(Collectors.toSet());
	}
}
