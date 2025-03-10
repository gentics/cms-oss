package com.gentics.contentnode.rest.model.devtools.dependency;


import java.util.List;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single package dependency that can be used for a package consistency check
 */
@XmlRootElement
public class PackageDependency extends AbstractDependencyModel {

	/**
	 * The list of package dependencies that are referenced by this object.
	 */
	private List<ReferenceDependency> referenceDependencies;

	public PackageDependency() {

	}

	/**
	 * Gets the list of dependencies that are referenced by this object.
	 *
	 * @return The list of object dependencies.
	 */
	public List<ReferenceDependency> getReferenceDependencies() {
		return referenceDependencies;
	}


	/**
	 * Set the list of dependencies that are referenced by this object.
	 * @return fluent Api
	 */
	public PackageDependency withReferenceDependencies(List<ReferenceDependency> referenceDependencies) {
		this.referenceDependencies = referenceDependencies;
		return this;
	}


}

