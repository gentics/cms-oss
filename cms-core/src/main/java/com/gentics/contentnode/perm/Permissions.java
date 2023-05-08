package com.gentics.contentnode.perm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.FilePrivileges;
import com.gentics.contentnode.rest.model.PagePrivileges;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.util.PermPattern;

public class Permissions {
	/**
	 * Stored permission settings
	 */
	protected static Map<String, Permissions> storedPerms = new HashMap<String, Permissions>();

	/**
	 * here are the bits stored
	 */
	protected byte[] bits = new byte[32];

	/**
	 * Function to transform the permission into the REST Model of file privileges
	 */
	public final static Function<Permissions, FilePrivileges> TRANSFORM2FILE = perms -> {
		FilePrivileges filePerms = new FilePrivileges();
		for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
			filePerms.set(priv, check(perms, priv.getRoleBit()));
		}
		return filePerms;
	};

	/**
	 * Function to transform the permission into the REST Model of page privileges
	 */
	public final static Function<Permissions, PagePrivileges> TRANSFORM2PAGE = perms -> {
		PagePrivileges pagePerms = new PagePrivileges();
		for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
			pagePerms.set(priv, check(perms, priv.getRoleBit()));
		}
		return pagePerms;
	};

	/**
	 * Get the Permissions object for the given perm bit string
	 * @param permBits perm bit string
	 * @return permissions object
	 * @throws NodeException
	 */
	public static synchronized Permissions get(String permBits) throws NodeException {
		Permissions perm = storedPerms.get(permBits);
		if (perm == null) {
			if (permBits.equals(PermHandler.EMPTY_PERM)) {
				return null;
			} else if (permBits.matches("^[0-1]{32}$")) {
				perm = new Permissions(permBits);
				storedPerms.put(permBits, perm);
			} else {
				throw new NodeException("Illegal permission bits string: " + permBits);
			}
		}
		return perm;
	}

	/**
	 * Get the Permissions object for the given permission bits
	 * @param permBits list of permission bits, that are set
	 * @return permissions object
	 * @throws NodeException
	 */
	public static Permissions get(int... permBits) throws NodeException {
		Set<Integer> bitSet = new HashSet<Integer>();
		for (int permBit : permBits) {
			bitSet.add(permBit);
		}

		StringBuilder str = new StringBuilder(32);
		for (int i = 0; i < 32; i++) {
			str.append(bitSet.contains(i) ? "1" : "0");
		}

		return get(str.toString());
	}

	/**
	 * Merge the given list of permissions into one
	 * @param perms list of permissions to merge
	 * @return merged permissions
	 */
	public static Permissions merge(List<Permissions> perms) {
		Permissions merged = new Permissions();
		for (Permissions p : perms) {
			merged.merge(p);
		}

		return merged;
	}

	/**
	 * Merge the given permissions into one
	 * @param perms permissions to merge
	 * @return merged permissions
	 */
	public static Permissions merge(Permissions... perms) {
		Permissions merged = new Permissions();
		for (Permissions p : perms) {
			merged.merge(p);
		}

		return merged;
	}

	/**
	 * Check whether the given perms have the bit set
	 * @param perms perms to check, may be null
	 * @param bit bit to check
	 * @return true iff perms were not null and bit set
	 */
	public static boolean check(Permissions perms, int bit) {
		if (perms != null) {
			return perms.check(bit);
		} else {
			return false;
		}
	}

	/**
	 * Return new permissions instance based on the given perms with the given bit set
	 * @param perms perms (may be null for "empty")
	 * @param bit bit to set
	 * @return possibly changed permissions (never null, since at least the given bit is set)
	 * @throws NodeException
	 */
	public static Permissions set(Permissions perms, int bit) throws NodeException {
		if (perms == null) {
			// perms empty, so return Permissions with just the bit set
			return get(bit);
		} else if (perms.check(bit)) {
			// bit already set, nothing to change
			return perms;
		} else {
			// clone original perms, set the bit and return
			// use Permissions.get() here to get stored perm instance
			return get(new Permissions(perms.toString()).set(bit).toString());
		}
	}

	/**
	 * Return new permissions instance based on the given perms with all bits of the given types set
	 * @param perms perms (may be null for "empty")
	 * @param types types to set
	 * @return possibly changed permissions (may be null for "empty")
	 * @throws NodeException
	 */
	public static Permissions set(Permissions perms, PermType...types) throws NodeException {
		Permissions updated = perms;
		for (PermType type : types) {
			updated = set(updated, type.getBit());
		}
		return updated;
	}

	/**
	 * Return new permissions instance (or null for "empty") based on the given perms with the given bit unset
	 * @param perms perms (may be null for "empty")
	 * @param bit bit to unset
	 * @return possibly changed permissions (may be null for "empty")
	 * @throws NodeException
	 */
	public static Permissions unset(Permissions perms, int bit) throws NodeException {
		if (perms == null) {
			// perms already empty, so nothing to do
			return perms;
		} else if (!perms.check(bit)) {
			// bit not set, so nothing to do
			return perms;
		} else {
			// clone original perms, unset the bit and return
			// use Permissions.get() here to get stored perm instance (or null instead empty perms)
			return get(new Permissions(perms.toString()).unset(bit).toString());
		}
	}

	/**
	 * Return a new instance with bits from the original perms changed according to the pattern
	 * @param perms original perms
	 * @param pattern change pattern. '1' will set the bit, '0' will unset the bit, everything else (like '.') will leave the bit untouched
	 * @return modified permissions instance
	 * @throws NodeException
	 */
	public static Permissions change(Permissions perms, String pattern) throws NodeException {
		Permissions mutableClone = new Permissions(toString(perms));
		for (int bit = 0; bit < Math.min(pattern.length(), 32); bit++) {
			switch(pattern.charAt(bit)) {
			case '1':
				mutableClone.set(bit);
				break;
			case '0':
				mutableClone.unset(bit);
				break;
			}
		}

		return get(mutableClone.toString());
	}

	/**
	 * Return a new instance with bits from the original perms changed according to the pattern
	 * @param perms original perms
	 * @param pattern change pattern.
	 * @return modified permissions instance
	 * @throws NodeException
	 */
	public static Permissions change(Permissions perms, PermPattern pattern) throws NodeException {
		return change(perms, pattern.toString());
	}

	/**
	 * Get a string representation of the given perms (which may be null for "no permission")
	 * @param perms permissions (may be null)
	 * @return string representation
	 */
	public static String toString(Permissions perms) {
		return perms == null ? PermHandler.EMPTY_PERM : perms.toString();
	}

	/**
	 * Create an empty permission setting
	 */
	private Permissions() {
	}

	/**
	 * Create a new Permissions instance
	 * @param permBits permission bits as string
	 */
	private Permissions(String permBits) {
		int c = permBits.length();

		bits = new byte[permBits.length()];
		for (int i = 0; i < c; ++i) {
			bits[i] = "1".equals(permBits.substring(i, i + 1)) ? (byte) 1 : (byte) 0;
		}
	}

	/**
	 * Merge the given permissions into this one
	 * @param p permissions to merge
	 */
	private void merge(Permissions p) {
		for (int i = 0; i < bits.length; i++) {
			bits[i] |= p.bits[i];
		}
	}

	/**
	 * Modify instance by setting the given bit
	 * @param bit bit to set
	 * @return fluent API
	 */
	private Permissions set(int bit) {
		if (bit < bits.length) {
			bits[bit] = (byte)1;
		}
		return this;
	}

	/**
	 * Modify instance by unsetting the given bit
	 * @param bit bit to unset
	 * @return fluent API
	 */
	private Permissions unset(int bit) {
		if (bit < bits.length) {
			bits[bit] = (byte)0;
		}
		return this;
	}

	/**
	 * Check whether the given permission bit is set
	 * @param bit permission bit
	 * @return true when the bit is set, false if not
	 */
	public boolean check(int bit) {
		if (bit < 0 || bit >= bits.length) {
			return false;
		} else {
			return bits[bit] == (byte) 1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Permissions) {
			Permissions other = (Permissions) obj;
			for (int i = 0; i < bits.length; i++) {
				if (bits[i] != other.bits[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(32);
		for (int i = 0; i < bits.length; i++) {
			b.append(bits[i]);
		}
		return b.toString();
	}
}
