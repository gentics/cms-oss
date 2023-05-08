package com.gentics.contentnode.perm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.exception.CloneFailedException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.FilePrivileges;
import com.gentics.contentnode.rest.model.PagePrivileges;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.RolePermissionsModel;

import gnu.trove.THashMap;

/**
 * Role permissions
 */
public class RolePermissions {
	/**
	 * Transform the role permissions into the rest model
	 */
	public final static Function<RolePermissions, RolePermissionsModel> TRANSFORM2REST = perms -> {
		RolePermissionsModel model = new RolePermissionsModel();
		model.setFile(Permissions.TRANSFORM2FILE.apply(perms != null ? perms.getFilePerm(): null));
		model.setPage(Permissions.TRANSFORM2PAGE.apply(perms != null ? perms.getPagePerm() : null));

		// set language specific privileges
		Set<Integer> languages = DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS);
		model.setPageLanguages(new HashMap<>());

		for (Integer languageId : languages) {
			model.getPageLanguages().put(languageId, Permissions.TRANSFORM2PAGE.apply(perms != null ? perms.getPagePerm(languageId) : null));
		}
		return model;
	};

	/**
	 * Update the given role permission by applying the changes in the rest model
	 */
	public final static BiFunction<RolePermissionsModel, RolePermissions, RolePermissions> REST2NODE = (model, perms) -> {
		if (model.getFile() != null) {
			perms.setFilePerm(changePermissions(model.getFile(), perms.getFilePerm()));
		}
		if (model.getPage() != null) {
			perms.setPagePerm(0, changePermissions(model.getPage(), perms.getPagePerm()));
		}
		if (model.getPageLanguages() != null) {
			for (Entry<Integer, PagePrivileges> entry : model.getPageLanguages().entrySet()) {
				int languageId = entry.getKey();
				PagePrivileges privMap = entry.getValue();
				perms.setPagePerm(languageId, changePermissions(privMap, perms.getPagePerm(languageId)));
			}
		}
		return perms;
	};

	/**
	 * Change the given permission by setting/unsetting bits based on the page privileges
	 * @param privileges page privileges
	 * @param perm permission to change (may be null for "empty")
	 * @return changed permissions (may be null for "empty")
	 * @throws NodeException
	 */
	protected final static Permissions changePermissions(PagePrivileges privileges, Permissions perm)
			throws NodeException {
		Permissions modified = perm;

		if (privileges != null) {
			for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
				modified = setOrUnset(modified, privileges.get(priv), priv.getRoleBit());
			}
		}

		return modified;
	}

	/**
	 * Change the given permission by setting/unsetting bits based on the file privileges
	 * @param privileges file privileges
	 * @param perm permission to change (may be null for "empty")
	 * @return changed permissions (may be null for "empty")
	 * @throws NodeException
	 */
	protected final static Permissions changePermissions(FilePrivileges privileges, Permissions perm)
			throws NodeException {
		Permissions modified = perm;

		if (privileges != null) {
			for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
				modified = setOrUnset(modified, privileges.get(priv), priv.getRoleBit());
			}
		}

		return modified;
	}

	/**
	 * Set or unset the role bit in the given permissions
	 * @param perm original permissions
	 * @param flag true to set, false to unset, null to not change
	 * @param bit role bit
	 * @return permissions with possibly changed role bit
	 * @throws NodeException
	 */
	protected final static Permissions setOrUnset(Permissions perm, Boolean flag, int bit) throws NodeException {
		if (Boolean.TRUE.equals(flag)) {
			return Permissions.set(perm, bit);
		} else if (Boolean.FALSE.equals(flag)) {
			return Permissions.unset(perm, bit);
		} else {
			return perm;
		}
	}

	/**
	 * Permissions entry for file/image permissions
	 */
	private Permissions filePerm;

	/**
	 * Map of page permissions per language. Keys are the language ID, values are the permission entries.
	 * The entry with key 0 is the general page permission for the role
	 */
	@SuppressWarnings("unchecked")
	private Map<Integer, Permissions> pagePerms = Collections.synchronizedMap(new THashMap());

	/**
	 * Create an empty instance
	 * @throws NodeException
	 */
	public RolePermissions() throws NodeException {
		filePerm = Permissions.get(PermHandler.EMPTY_PERM);
	}

	/**
	 * Set the file permissions
	 * @param perm permissions
	 * @return fluent API
	 */
	public RolePermissions setFilePerm(Permissions perm) {
		filePerm = perm;
		return this;
	}

	/**
	 * Get the file permissions
	 * @return file permissions
	 */
	public Permissions getFilePerm() {
		return filePerm;
	}

	/**
	 * Set the page permissions for the language id (or the general page permissions)
	 * @param langId language id (0 for the general page permissions)
	 * @param perm permissions
	 * @return fluent API
	 */
	public RolePermissions setPagePerm(int langId, Permissions perm) {
		pagePerms.put(langId, perm);
		return this;
	}

	/**
	 * Get the general page permissions
	 * @return general page permissions (may be null)
	 */
	public Permissions getPagePerm() {
		return pagePerms.get(0);
	}

	/**
	 * Get the page permissions for the given language
	 * @param langId language id
	 * @return page permissions for the given language (may be null)
	 */
	public Permissions getPagePerm(int langId) {
		return pagePerms.get(langId);
	}

	@Override
	public RolePermissions clone() throws CloneNotSupportedException {
		try {
			RolePermissions clone = new RolePermissions();
			clone.setFilePerm(getFilePerm());
			for (Integer langId : pagePerms.keySet()) {
				clone.setPagePerm(langId, getPagePerm(langId));
			}
			return clone;
		} catch (NodeException e) {
			throw new CloneFailedException(e);
		}
	}
}
