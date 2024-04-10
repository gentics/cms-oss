package com.gentics.contentnode.update;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Comparable encapsulation of CMS version
 */
public class CMSVersion implements Comparable<CMSVersion> {
	/**
	 * Pattern for version numbers
	 */
	protected static final Pattern pattern = Pattern.compile("(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patchLevel>[0-9]+)(?<snapshot>-SNAPSHOT)?(.*)");

	/**
	 * Pattern for update files (containing version number)
	 */
	protected static final Pattern updateFilePattern = Pattern
			.compile("(.*)(?<major>\\d)\\.(?<minor>\\d{1,2})\\.(?<patchLevel>\\d{1,2})(?<snapshot>-SNAPSHOT)?-?([a-zA-Z]\\w*)?-?(?<build>(\\d+\\.?\\d+)-(\\d+))?(.*)");

	/**
	 * Major version
	 */
	protected int major;

	/**
	 * Minor version
	 */
	protected int minor;

	/**
	 * Patchlevel version
	 */
	protected int patchLevel;

	/**
	 * True for snapshot version
	 */
	protected boolean snapshot = false;

	/**
	 * Parse the file and return a version instance, if the name matches the required pattern. Return null otherwise
	 * @param file file
	 * @return version instance or null
	 */
	public static CMSVersion parse(File file) {
		String name = file.getName();
		Matcher m = updateFilePattern.matcher(name);
		if (m.matches()) {
			CMSVersion version = new CMSVersion();
			version.major = Integer.parseInt(m.group("major"));
			version.minor = Integer.parseInt(m.group("minor"));
			version.patchLevel = Integer.parseInt(m.group("patchLevel"));
			version.snapshot = !StringUtils.isAllBlank(m.group("snapshot"), m.group("build"));
			return version;
		} else {
			return null;
		}
	}

	/**
	 * Create empty instance (version 0.0.0)
	 */
	public CMSVersion() {
	}

	/**
	 * Create instance
	 * @param versionString version string
	 */
	public CMSVersion(String versionString) {
		Matcher matcher = pattern.matcher(versionString);
		if (matcher.matches()) {
			major = Integer.parseInt(matcher.group("major"));
			minor = Integer.parseInt(matcher.group("minor"));
			patchLevel = Integer.parseInt(matcher.group("patchLevel"));
			snapshot = !StringUtils.isBlank(matcher.group("snapshot"));
		}
	}

	/**
	 * Create instance
	 * @param major major version
	 * @param minor minor version
	 * @param patchLevel patch level
	 */
	public CMSVersion(int major, int minor, int patchLevel) {
		this.major = major;
		this.minor = minor;
		this.patchLevel = patchLevel;
	}

	@Override
	public int compareTo(CMSVersion o) {
		int majorCompare = Integer.compare(major, o.major);
		if (majorCompare != 0) {
			return majorCompare;
		}
		int minorCompare = Integer.compare(minor, o.minor);
		if (minorCompare != 0) {
			return minorCompare;
		}
		int patchCompare = Integer.compare(patchLevel, o.patchLevel);
		if (patchCompare != 0) {
			return patchCompare;
		}

		return -Boolean.compare(snapshot, o.snapshot);
	}

	/**
	 * Check whether this version is greater or equal than the other version
	 * @param o other version
	 * @return if this is greater or equal than the given version
	 */
	public boolean isGreaterOrEqual(CMSVersion o) {
		return compareTo(o) >= 0;
	}

	/**
	 * Check whether this version is greater than the other version
	 * @param o other version
	 * @return if this is greater than the given version
	 */
	public boolean isGreater(CMSVersion o) {
		return compareTo(o) > 0;
	}

	@Override
	public String toString() {
		return String.format("%d.%d.%d%s", major, minor, patchLevel, snapshot ? "-SNAPSHOT": "");
	}

	/**
	 * Return only the major.minor part of the version
	 * @return major.minor
	 */
	public String getMajorMinor() {
		return String.format("%d.%d", major, minor);
	}

	/**
	 * Get major version number
	 * @return major version
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Get minor version number
	 * @return minor version
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Get patch level
	 * @return patch level
	 */
	public int getPatchLevel() {
		return patchLevel;
	}

	/**
	 * Check whether the version is a snapshot
	 * @return true for snapshot version
	 */
	public boolean isSnapshot() {
		return snapshot;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CMSVersion) {
			return compareTo((CMSVersion) obj) == 0;
		} else {
			return false;
		}
	}
}
