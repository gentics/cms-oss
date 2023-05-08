package com.gentics.contentnode.devtools.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateModel extends AbstractModel {
	private String channelId;

	private String name;

	private String description;

	private String type;

	private List<TemplateTagModel> templateTags;

	private List<ObjectTagModel> objectTags;

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<TemplateTagModel> getTemplateTags() {
		return templateTags;
	}

	public void setTemplateTags(List<TemplateTagModel> templateTags) {
		this.templateTags = templateTags;
	}

	public List<ObjectTagModel> getObjectTags() {
		return objectTags;
	}

	public void setObjectTags(List<ObjectTagModel> objectTags) {
		this.objectTags = objectTags;
	}
}
