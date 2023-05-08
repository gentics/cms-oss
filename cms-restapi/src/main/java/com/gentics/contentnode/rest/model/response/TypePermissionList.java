package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.perm.TypePermissions;

/**
 * List of permissions
 */
@XmlRootElement
public class TypePermissionList extends AbstractListResponse<TypePermissions> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9070650214909996512L;

	/**
	 * List of type permissions
	 * @return list
	 */
	@Override
	public List<TypePermissions> getItems() {
		return super.getItems();
	}
}
