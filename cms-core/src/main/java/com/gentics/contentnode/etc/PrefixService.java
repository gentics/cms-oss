package com.gentics.contentnode.etc;

/**
 * Interface for implementations that are responsible to get or generate prefixes.
 */
public interface PrefixService {

	/**
	 * Method to get or generate a prefix
	 * @return a prefix of arbitrary length
	 */
	String getGlobalPrefix();

}
