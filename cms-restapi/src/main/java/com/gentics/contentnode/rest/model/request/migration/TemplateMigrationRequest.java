package com.gentics.contentnode.rest.model.request.migration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationMapping;

@XmlRootElement
public class TemplateMigrationRequest implements Serializable {

	private static final long serialVersionUID = 8400607615487157216L;

	public static final String LINK_FOLDER_OPTION = "linkFolders";

	/**
	 * Default constructor
	 */
	public TemplateMigrationRequest() {}

	/**
	 * List of enabled pre processors
	 */
	private List<MigrationPreProcessor> enabledPreProcessors;

	/**
	 * List of enabled post processors
	 */
	private List<MigrationPostProcessor> enabledPostProcessors;

	private TemplateMigrationMapping mapping;

	private HashMap<String, String> options;

	/**
	 * Returns the mapping for this migration request
	 * 
	 * @return
	 */
	public TemplateMigrationMapping getMapping() {
		return mapping;
	}

	/**
	 * Sets the mapping for this migration request
	 * 
	 * @param mapping
	 */
	public void setMapping(TemplateMigrationMapping mapping) {
		this.mapping = mapping;
	}

	/**
	 * Returns the list of enabled pre processors that were specified for this migration request
	 * 
	 * @return
	 */
	public List<MigrationPreProcessor> getEnabledPreProcessors() {
		return enabledPreProcessors;
	}

	/**
	 * Sets the map of enabled pre processors for this migration request
	 * 
	 * @param enabledPreProcessors
	 */
	public void setEnabledPreProcessors(List<MigrationPreProcessor> enabledPreProcessors) {
		this.enabledPreProcessors = enabledPreProcessors;
	}

	/**
	 * Returns the list of enabled post processors that were specified for this migration request
	 * 
	 * @return
	 */
	public List<MigrationPostProcessor> getEnabledPostProcessors() {
		return enabledPostProcessors;
	}

	/**
	 * Sets the map of enabled post processors for this migration request
	 * 
	 * @param enabledPostProcessors
	 */
	public void setEnabledPostProcessors(List<MigrationPostProcessor> enabledPostProcessors) {
		this.enabledPostProcessors = enabledPostProcessors;
	}

	/**
	 * Returns a key value map which represents the options which were set for this request
	 * 
	 * @return
	 */
	public HashMap<String, String> getOptions() {
		return options;
	}

	/**
	 * Sets the map of options for this request
	 * 
	 * @param options
	 */
	public void setOptions(HashMap<String, String> options) {
		this.options = options;
	}

}
