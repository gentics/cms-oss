package com.gentics.contentnode.rest.model.devtools;

import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.ObjectProperty;
import java.util.Set;

import java.util.function.Consumer;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Package
 */
@XmlRootElement
public class Package {
	private String name;

	private String description;

	private int constructs;

	private Construct construct;

	private String category;

	private int templates;

	private int datasources;

	private int objectProperties;

	private int crFragments;

	private int contentRepositories;

	private Set<Package> subPackages;

	/**
	 * Create empty package
	 */
	public Package() {
	}

	/**
	 * Create package with name
	 * @param name name
	 */
	public Package(String name) {
		this.name = name;
	}

	/**
	 * Package name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the package name
	 * @param name package name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Package description
	 * @return package description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Number of constructs contained in the package
	 * @return number of constructs
	 */
	public int getConstructs() {
		return constructs;
	}

	/**
	 * Set the number of constructs
	 * @param constructs number of constructs
	 */
	public void setConstructs(int constructs) {
		this.constructs = constructs;
	}

	/**
	 * Number of templates contained in the package
	 * @return number of templates
	 */
	public int getTemplates() {
		return templates;
	}

	/**
	 * Set the number of templates
	 * @param templates number of templates
	 */
	public void setTemplates(int templates) {
		this.templates = templates;
	}

	/**
	 * Number of datasources contained in the package
	 * @return number of datasources
	 */
	public int getDatasources() {
		return datasources;
	}

	/**
	 * Set the number of datasources
	 * @param datasources number of datasources
	 */
	public void setDatasources(int datasources) {
		this.datasources = datasources;
	}

	/**
	 * Number of object properties contained in the package
	 * @return number of object properties
	 */
	public int getObjectProperties() {
		return objectProperties;
	}

	/**
	 * Set the number of object properties
	 * @param objectProperties numer of object properties
	 */
	public void setObjectProperties(int objectProperties) {
		this.objectProperties = objectProperties;
	}

	/**
	 * Number of ContentRepository Fragments contained in the package
	 * @return number of ContentRepository Fragments
	 */
	public int getCrFragments() {
		return crFragments;
	}

	/**
	 * Set the number of ContentRepository Fragments
	 * @param crFragments number of ContentRepository Fragments
	 */
	public void setCrFragments(int crFragments) {
		this.crFragments = crFragments;
	}

	/**
	 * Number of ContentRepositories contained in the package
	 * @return number of ContentRepositories
	 */
	public int getContentRepositories() {
		return contentRepositories;
	}

	/**
	 * Set the number of ContentRepositories
	 * @param contentRepositories number ofr ContentRepositories
	 */
	public void setContentRepositories(int contentRepositories) {
		this.contentRepositories = contentRepositories;
	}

	/**
	 * Get set of sub packages
	 * @return sub packages
	 */
	public Set<Package> getSubPackages() {
		return subPackages;
	}

	/**
	 * Set set of sub packages
	 * @param subPackages sub packages
	 */
	public void setSubPackages(Set<Package> subPackages) {
		this.subPackages = subPackages;
	}

	/**
	 * Get the referenced construct
	 * @return the referenced construct
	 */
	public Construct getConstruct() {
		return construct;
	}

	/**
	 * Set the referenced construct
	 * @param construct the referenced construct
	 */
	public void setConstruct(Construct construct) {
		this.construct = construct;
	}

	/**
	 * Get the name of the package category
	 * @return category name of the package
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Set the name of the package category
	 */
	public void setCategory(String category) {
		this.category = category;
	}
}
