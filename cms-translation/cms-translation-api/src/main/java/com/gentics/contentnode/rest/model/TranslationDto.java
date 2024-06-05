package com.gentics.contentnode.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TranslationDto implements Serializable {

	private static final long serialVersionUID = -7483178254386691173L;

	public String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

/*	public TranslationDto withText(String text) {
		this.text = text;
		return this;
	}*/

}
