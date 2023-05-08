package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for implementations that generate page version numbers. Generated
 * Version Numbers are arbitrary Strings with up to 10 characters. The methods
 * may also return null if no version number shall be generated
 */
public interface PageVersionNumberGenerator {

	/**
	 * Get the first version number for a new page (e.g. 1.0 for the published version, 0.1 for the unpublished version).
	 * @param published true if the page will be published, false if not (yet)
	 * @return the first version number
	 * @throws NodeException in case of errors
	 */
	public String getFirstVersionNumber(boolean published) throws NodeException;

	/**
	 * Get the next version number after the given one (e.g. 2.0 after 1.4 for the published version, 1.5 after 1.4 for the unpublished version).
	 * @param lastVersionNumber last version number
	 * @param published true if the page will be published, false if not (yet)
	 * @return the next version number
	 * @throws NodeException in case of errors
	 */
	public String getNextVersionNumber(String lastVersionNumber, boolean published) throws NodeException;

	/**
	 * Modify the given version number to make it a published version (e.g. 2.0 from 1.4 or 2.0 from 2.0)
	 * @param versionNumber given (unpublished or published) version number
	 * @return published version number
	 * @throws NodeException in case of errors
	 */
	public String makePublishedVersion(String versionNumber) throws NodeException;
}
