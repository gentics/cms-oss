package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for select settings
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class SelectSetting implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4091439143956890856L;

	private int datasourceId;

	private String template;

	private List<SelectOption> options;

	/**
	 * Create empty instance
	 */
	public SelectSetting() {
	}

	/**
	 * Datasource ID
	 * @return datasource ID
	 */
	public int getDatasourceId() {
		return datasourceId;
	}

	/**
	 * Set the datasource ID
	 * @param datasourceId datasource ID
	 */
	public void setDatasourceId(int datasourceId) {
		this.datasourceId = datasourceId;
	}

	/**
	 * Rendering template
	 * @return rendering template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Set the rendering template
	 * @param template template
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Selectable options of the datasource
	 * @return option list
	 */
	public List<SelectOption> getOptions() {
		return options;
	}

	/**
	 * Set the option list
	 * @param options option list
	 */
	public void setOptions(List<SelectOption> options) {
		this.options = options;
	}
}
