package com.gentics.contentnode.rest.model.devtools.dependency;

/**
 * A reference of a dependency.
 */
public class ReferenceDependency extends AbstractDependencyModel {

	/**
	 * A flag indicating whether the object is in the current package.
	 */
	private Boolean isInPackage;
	/**
	 * A flag indicating whether the object is located in a different package.
	 */
	private Boolean isInOtherPackage;

	/**
	 * Returns true if the object is in the current package, false otherwise.
	 *
	 * @return Whether the object is in the current package.
	 */
	public Boolean getIsInPackage() {
		return isInPackage;
	}

	/**
	 * Sets whether the object is in the current package.
	 *
	 * @param isInPackage Whether the object is in the current package.
	 * @return fluent Api
	 */
	public ReferenceDependency withIsInPackage(Boolean isInPackage) {
		this.isInPackage = isInPackage;
		return this;
	}

	/**
	 * Gets whether the object is located in a different package.
	 *
	 * @return True if the object is located in a different package, false otherwise.
	 */
	public Boolean getIsInOtherPackage() {
		return isInOtherPackage;
	}

	/**
	 * Sets whether the object is located in a different package.
	 *
	 * @param isInOtherPackage True if the object is located in a different package, false otherwise.
	 * @return fluent Api
	 */
	public ReferenceDependency withIsInOtherPackage(Boolean isInOtherPackage) {
		this.isInOtherPackage = isInOtherPackage;
		return this;
	}

}

