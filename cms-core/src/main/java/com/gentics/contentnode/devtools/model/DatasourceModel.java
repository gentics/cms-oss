package com.gentics.contentnode.devtools.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasourceModel extends AbstractModel {
	private DatasourceTypeModel type;

	private String name;

	private List<DatasourceValueModel> values;

	public DatasourceTypeModel getType() {
		return type;
	}

	public void setType(DatasourceTypeModel type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DatasourceValueModel> getValues() {
		return values;
	}

	public void setValues(List<DatasourceValueModel> values) {
		this.values = values;
	}
}
