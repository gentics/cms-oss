package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

public interface Validator {

	boolean isValid(String text) throws NodeException;

}

