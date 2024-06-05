package com.gentics.contentnode.rest.model.request;

import com.gentics.contentnode.rest.model.TranslationDto;

public class TranslationRequest extends TranslationDto {

	private static final long serialVersionUID = 8481093705987769677L;

	public String targetLanguage;

	public String getTargetLanguage() {
		return targetLanguage;
	}

	public void setTargetLanguage(String targetLanguage) {
		this.targetLanguage = targetLanguage;
	}

}
