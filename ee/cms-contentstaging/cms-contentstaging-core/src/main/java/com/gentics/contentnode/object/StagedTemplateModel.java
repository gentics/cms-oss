package com.gentics.contentnode.object;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedTemplateModel extends AbstractModel {

	private String type;

	private String source;

	private List<StagedTemplateTagModel> templateTags;

	public String getType() {
		return type;
	}

	public StagedTemplateModel setType(String type) {
		this.type = type;
		return this;
	}

	public String getSource() {
		return source;
	}

	public StagedTemplateModel setSource(String source) {
		this.source = source;
		return this;
	}

	public List<StagedTemplateTagModel> getTemplateTags() {
		return templateTags;
	}

	public StagedTemplateModel setTemplateTags(List<StagedTemplateTagModel> templateTags) {
		this.templateTags = templateTags;
		return this;
	}
}
