package com.gentics.contentnode.version;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for CMP component version numbers.
 *
 * <p>
 *     The patch level number is optional, and leaving it out is taken into
 *     account for {@link #compareTo(CmpProductVersion) comparison}.
 * </p>
 */
public class CmpProductVersion implements Comparable<CmpProductVersion> {

	/**
	 * Pattern for version numbers.
	 *
	 * <p>
	 *     The {@code major} and {@code minor} parts are required, the
	 *     {@code patchLevel} together with the SNAPSHOT postfix is optional.
	 * </p>
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("(?<major>[0-9]+)\\.(?<minor>[0-9]+)(?:\\.(?<patchLevel>[0-9]+)(?<snapshot>-SNAPSHOT)?)?(.*)");

	private final int major;
	private final int minor;
	private final int patchLevel;
	private final boolean snapshot;

	public CmpProductVersion(String version) {
		if (version != null) {
			Matcher matcher = VERSION_PATTERN.matcher(version);

			if (matcher.matches()) {
				this.major = Integer.parseInt(matcher.group("major"));
				this.minor = Integer.parseInt(matcher.group("minor"));

				String patchLevel = matcher.group("patchLevel");

				this.patchLevel = StringUtils.isEmpty(patchLevel)
					? -1
					: Integer.parseInt(patchLevel);

				this.snapshot = StringUtils.isNotEmpty(matcher.group("snapshot"));

				return;
			}
		}

		this.major = this.minor = this.patchLevel = -1;
		this.snapshot = false;
	}

	@Override
	public int compareTo(CmpProductVersion other) {
		int cmp;

		if ((cmp = Integer.compare(this.major, other.major)) != 0) {
			return cmp;
		}

		if ((cmp = Integer.compare(this.minor, other.minor)) != 0) {
			return cmp;
		}

		// When either version number has no patch level, only major and minor
		// matter, so they are considered equal.
		if (this.patchLevel == -1 || other.patchLevel == -1) {
			return 0;
		}

		if ((cmp = Integer.compare(this.patchLevel, other.patchLevel)) != 0) {
			return cmp;
		}

		// For booleans false is "less than" true, but we want version
		// x.y.z-SNAPSHOT to be considered "less than" x.y.z, so we switch
		// the sign after the comparison.
		return -Boolean.compare(this.snapshot, other.snapshot);
	}

	/**
	 * Check whether this version is greater or equal than the other version.
	 *
	 * @see #compareTo(CmpProductVersion)
	 * @param other Version to compare to
	 * @return {@code true} if this version is greater than or equal to the
	 * 		other version, and {@code false} otherwise
	 */
	public boolean isGreaterOrEqual(CmpProductVersion other) {
		return compareTo(other) >= 0;
	}

	/**
	 * Check if this version is in the given range.
	 *
	 * @param range The version range to check against
	 * @return {@code true} when this version is in the specified range, and
	 * 		{@code false} otherwise
	 */
	public boolean inRange(ProductVersionRange range) {
		return range != null
			&& compareTo(new CmpProductVersion(range.getMinVersion())) >= 0
			&& compareTo(new CmpProductVersion(range.getMaxVersion())) <= 0;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder()
			.append(major)
			.append('.')
			.append(minor);

		if (patchLevel >= 0) {
			ret.append('.').append(patchLevel);

			if (snapshot) {
				ret.append("-SNAPSHOT");
			}
		}

		return ret.toString();
	}
}
