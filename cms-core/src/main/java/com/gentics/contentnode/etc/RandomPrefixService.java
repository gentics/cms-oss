package com.gentics.contentnode.etc;

import java.util.Random;

/**
 * Service that generates a random HEX prefix
 */
public class RandomPrefixService implements PrefixService  {
	private static final int PREFIX_LENGTH = 4;

	/**
	 * Method responsible for getting a prefix that can be used globally
	 * @return a string in HEX with {@link #PREFIX_LENGTH} characters
	*/
	public String getGlobalPrefix() {
		return generateRandomPrefix();
	}

	private String generateRandomPrefix() {
		Random r = new Random();
		StringBuffer sb = new StringBuffer();
		while (sb.length() < PREFIX_LENGTH) {
			sb.append(Integer.toHexString(r.nextInt()));
		}

		return sb.toString().substring(0, PREFIX_LENGTH);
	}

}
