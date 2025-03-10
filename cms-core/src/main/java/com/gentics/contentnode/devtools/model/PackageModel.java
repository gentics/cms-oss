package com.gentics.contentnode.devtools.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageModel {
	private String subpackages;

	public String getSubpackages() {
		return subpackages;
	}

	public void setSubpackages(String subpackages) {
		this.subpackages = subpackages;
	}
}
