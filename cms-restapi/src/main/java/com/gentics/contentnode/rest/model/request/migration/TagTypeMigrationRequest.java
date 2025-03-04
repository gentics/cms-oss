package com.gentics.contentnode.rest.model.request.migration;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;

/**
 * Tag Type Migration Request
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class TagTypeMigrationRequest implements Serializable {

	private static final long serialVersionUID = -7258394759780953903L;

	/**
	 * Tag Type Migration mappings
	 */
	private List<TagTypeMigrationMapping> mappings;

	/**
	 * List of enabled pre processors
	 */
	private List<MigrationPreProcessor> enabledPreProcessors;

	/**
	 * List of enabled post processors
	 */
	private List<MigrationPostProcessor> enabledPostProcessors;

	/**
	 * Flag that indicates whether the migration should use the mapping as a template for other pages. When set the migration will retrive all pages that use the same template like the
	 * first page in the objectIds list.
	 */
	private boolean handlePagesByTemplate = false;

	/**
	 * Flag that indicates whether the migration should be extended to the pages of all nodes, if {@link #handlePagesByTemplate} is true.
	 */
	private boolean handleAllNodes = false;

	/**
	 * Flag to indicate whether pages shall be handled for a global ttm
	 */
	private boolean handleGlobalPages = false;

	/**
	 * Flag to indicate whether templates shall be handled for a global ttm
	 */
	private boolean handleGlobalTemplates = false;

	/**
	 * Flag to indicate whether object tag definitions shall be handled for a global ttm
	 */
	private boolean handleGlobalObjTagDefs = false;

	/**
	 * Flag that indicates whether the migration should prevent the invocation of trigger events. This will speed up the final phase of the migration but it is mandatory to republish all pages manually. 
	 */
	private boolean preventTriggerEvent = false;

	/**
	 * The type of objects to be migrated
	 */
	private String type;

	/**
	 * List of IDs of the objects the migration will be applied to
	 */
	private List<Integer> objectIds;

	/**
	 * List of IDs of nodes, to which a global TTM shall be restricted
	 */
	private List<Integer> restrictedNodeIds;

	/**
	 * Check whether the trigger events should be prevented.
	 * 
	 * @return true if trigger events should be prevented, otherwise false
	 */
	public boolean isPreventTriggerEvent() {
		return this.preventTriggerEvent;
	}

	/**
	 * Sets the prevent tigger event option
	 * 
	 * @param handlePagesByTemplate
	 */
	public void setPreventTriggerEvent(boolean preventTriggerEvent) {
		this.preventTriggerEvent = preventTriggerEvent;
	}

	/**
	 * Returns whether the handlePagesByTemplate flag was enabled for this request.
	 * 
	 * @return true if the migration should use the page as a refernce to fetch
	 * all the template's pages, otherwise false 
	 */
	public boolean isHandlePagesByTemplate() {
		return this.handlePagesByTemplate;
	}

	/**
	 * Sets the handlePagesByTemplate option which specifies whether the migration 
	 * should be applied to ALL pages using this page's template.
	 * 
	 * @param handlePagesByTemplate
	 */
	public void setHandlePagesByTemplate(boolean handlePagesByTemplate) {
		this.handlePagesByTemplate = handlePagesByTemplate;
	}

	/**
	 * Returns whether the handleAllNodes flag was enabled
	 * @return true if pages of all nodes shall be handled, false if only of the same node as the selected page
	 */
	public boolean isHandleAllNodes() {
		return handleAllNodes;
	}

	/**
	 * Set the handleAllNodes flag
	 * @param handleAllNodes true to handle pages of all nodes, false for only the same node as the selected page
	 */
	public void setHandleAllNodes(boolean handleAllNodes) {
		this.handleAllNodes = handleAllNodes;
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
	 * Create an empty instance
	 */
	public TagTypeMigrationRequest() {}

	/**
	 * Return the list of {@link TagTypeMigrationMapping}
	 * 
	 * @return
	 */
	public List<TagTypeMigrationMapping> getMappings() {
		return mappings;
	}

	/**
	 * Set the list of {@link TagTypeMigrationMapping}
	 * 
	 * @param mappings
	 */
	public void setMappings(List<TagTypeMigrationMapping> mappings) {
		this.mappings = mappings;
	}

	/**
	 * Return the type of the objects. This can either be 'page','template' or 'objtagdef'
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type for the migration job. This can either be 'page','template' or 'objtagdef'
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the list of object ids
	 * 
	 * @return
	 */
	public List<Integer> getObjectIds() {
		return objectIds;
	}

	/**
	 * Set the list of object ids
	 * 
	 * @param objectIds
	 */
	public void setObjectIds(List<Integer> objectIds) {
		this.objectIds = objectIds;
	}

	/**
	 * Whether a global migration shall handle pages
	 * @return true to handle pages
	 */
	public boolean isHandleGlobalPages() {
		return handleGlobalPages;
	}

	/**
	 * Set whether a global migration shall handle pages
	 * @param handleGlobalPages true to handle pages
	 */
	public void setHandleGlobalPages(boolean handleGlobalPages) {
		this.handleGlobalPages = handleGlobalPages;
	}

	/**
	 * Whether a global migration shall handle templates
	 * @return true to handle templates
	 */
	public boolean isHandleGlobalTemplates() {
		return handleGlobalTemplates;
	}

	/**
	 * Set whether a global migration shall handle templates
	 * @param handleGlobalTemplates true to handle templates
	 */
	public void setHandleGlobalTemplates(boolean handleGlobalTemplates) {
		this.handleGlobalTemplates = handleGlobalTemplates;
	}

	/**
	 * Whether a global migration shall handle object property definitions
	 * @return true to handle object property definitions
	 */
	public boolean isHandleGlobalObjTagDefs() {
		return handleGlobalObjTagDefs;
	}

	/**
	 * Set whether a global migration shall handle object property definitions
	 * @param handleGlobalObjTagDefs true to handle object property definitions
	 */
	public void setHandleGlobalObjTagDefs(boolean handleGlobalObjTagDefs) {
		this.handleGlobalObjTagDefs = handleGlobalObjTagDefs;
	}

	/**
	 * List of IDs of nodes, to which a global migration shall be restricted. Empty to apply global migration to all nodes
	 * @return list of node IDs
	 */
	public List<Integer> getRestrictedNodeIds() {
		return restrictedNodeIds;
	}

	/**
	 * Set list of node IDs to which a global migration shall be restricted
	 * @param restrictedNodeIds list of node IDs
	 */
	public void setRestrictedNodeIds(List<Integer> restrictedNodeIds) {
		this.restrictedNodeIds = restrictedNodeIds;
	}
}
