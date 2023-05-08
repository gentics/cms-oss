package com.gentics.contentnode.factory.object;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;

/**
 * Default Implementation for page version numbers
 */
public class DefaultPageVersionNumberGenerator implements
		PageVersionNumberGenerator {

	/**
	 * Pattern for version numbers
	 */
	public final static Pattern VERSION_NUMBER_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)");

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.PageVersionNumberGenerator#getFirstVersionNumber(boolean)
	 */
	public String getFirstVersionNumber(boolean published) throws NodeException {
		return published ? "1.0" : "0.1";
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.PageVersionNumberGenerator#getNextVersionNumber(java.lang.String, boolean)
	 */
	public String getNextVersionNumber(String lastVersionNumber,
			boolean published) throws NodeException {
		if (ObjectTransformer.isEmpty(lastVersionNumber)) {
			return getFirstVersionNumber(published);
		}

		int[] numParts = parseVersionNumber(lastVersionNumber);
		int major = numParts[0];
		int minor = numParts[1];

		StringBuffer versionNumber = new StringBuffer(3);

		if (published) {
			versionNumber.append(major + 1).append(".0");
		} else {
			versionNumber.append(major).append(".").append(minor + 1);
		}

		return versionNumber.toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.PageVersionNumberGenerator#makePublishedVersion(java.lang.String)
	 */
	public String makePublishedVersion(String versionNumber) throws NodeException {
		if (ObjectTransformer.isEmpty(versionNumber)) {
			return getFirstVersionNumber(true);
		}

		int[] numParts = parseVersionNumber(versionNumber);
		int major = numParts[0];
		int minor = numParts[1];

		// if the minor part is 0, we return the same version number (it is a published version)
		if (minor == 0) {
			return versionNumber;
		}

		// create the next published version number
		StringBuffer publishedVersionNumber = new StringBuffer(3);

		publishedVersionNumber.append(major + 1).append(".0");

		return publishedVersionNumber.toString();
	}

	/**
	 * Helper method to parse the given version number into major and minor part
	 * @param versionNumber version number to parse
	 * @return Array of ints containing major (index 0) and minor (index 1)
	 * @throws NodeException if the version number could not be parsed
	 */
	protected int[] parseVersionNumber(String versionNumber) throws NodeException {
		Matcher matcher = VERSION_NUMBER_PATTERN.matcher(versionNumber);

		if (!matcher.matches()) {
			throw new NodeException("Error while parsing version number: {" + versionNumber + "} does not match expected pattern!");
		}

		return new int[] { ObjectTransformer.getInt(matcher.group(1), 0), ObjectTransformer.getInt(matcher.group(2), 0) };
	}
}
