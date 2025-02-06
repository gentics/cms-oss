package com.gentics.contentnode.object;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.devtools.model.TemplateTagModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedTemplateTagModel extends TemplateTagModel {
	private List<StagedValueModel> values;

	public List<StagedValueModel> getValues() {
		return values;
	}

	public StagedTemplateTagModel setValues(List<StagedValueModel> values) {
		this.values = values;
		return this;
	}
}
