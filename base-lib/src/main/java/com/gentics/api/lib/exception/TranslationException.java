package com.gentics.api.lib.exception;

import java.io.Serial;

public class TranslationException extends NodeException {

	@Serial
	private static final long serialVersionUID = -4897057478604156346L;

	public TranslationException() {
		super();
	}

	public TranslationException(String message) {
		super(message);
	}

}
