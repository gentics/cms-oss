package com.gentics.contentnode.etc;

import java.util.List;

/**
 * Interface for different validatior implementations
 */
public interface Validator {

	/**
	 * Checks a given html block or page for validity
	 *
	 * @param html the html/text input
	 * @return whether the given html text is valid
	 */
	boolean isValid(String html);


	/**
	 * Validate the given html and return a list of errors
	 *
	 * @param html the html/text input
	 * @return a list of errors.
	 */
	List<String> validateAndCollectErrors(String html);

}

