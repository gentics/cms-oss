package com.gentics.contentnode.rest.model.devtools.dependency;

import com.gentics.contentnode.rest.model.AbstractModel;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Abstract base class for PackageDependency and ReferenceDependency implementations
 */
@XmlRootElement
public abstract class AbstractDependencyModel extends AbstractModel {

	/**
	 * The keyword of the package object
	 */
	private String keyword;

	/**
	 * The name of the package object.
	 */
	private String name;

	/**
	 * The type of the package.
	 */
	private Type dependencyType;


	/**
	 * Gets the name of the package object.
	 *
	 * @return The name of the object.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the package object.
	 *
	 * @param name The new name of the object.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the keyword of the package object.
	 *
	 * @return The name of the object.
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Sets the keyword of the package object.
	 *
	 * @param keyword The new keyword of the object.
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * Gets the type of the package object.
	 *
	 * @return The type of the object.
	 */
	public Type getDependencyType() {
		return dependencyType;
	}

	/**
	 * Sets the type of the package object.
	 *
	 * @param dependencyType The new type of the object.
	 */
	public void setDependencyType(Type dependencyType) {
		this.dependencyType = dependencyType;
	}


	/**
	 * Builder to create a child instance of AbstractDependencyModel instance
	 *
	 * @see AbstractDependencyModel
	 */
	public static class Builder<T extends AbstractDependencyModel> {

		private Class<T> clazz;
		private T dependency;

		public Builder(Class<T> clazz) {
			this.clazz = clazz;
			try {
				this.dependency = clazz.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				// todo: log
			}
		}

		public T build() {
			return dependency;
		}

		public Builder<T> withGlobalId(String globalId) {
			dependency.setGlobalId(globalId);
			return this;
		}

		public Builder<T> withName(String name) {
			dependency.setName(name);
			return this;
		}

		public Builder<T> withKeyword(String keyword) {
			dependency.setKeyword(keyword);
			return this;
		}

		public Builder<T> withType(Type type) {
			dependency.setDependencyType(type);
			return this;
		}

	}

}
