package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Content staging package status for an entity.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
public class StagingStatus implements Serializable {

	private static final long serialVersionUID = -1196387900759469701L;

	private String packageName;
	private boolean included = false;

	/**
	 * Get content staging package name.
	 * 
	 * @return
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Set content staging package name.
	 * 
	 * @param packageName
	 * @return fluent API
	 */
	public StagingStatus setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	/**
	 * Is the entity included in the given package?
	 * 
	 * @return
	 */
	public boolean isIncluded() {
		return included;
	}

	/**
	 * Set the entity inclusion status in the given package.
	 * 
	 * @param included
	 * @return fluent API
	 */
	public StagingStatus setIncluded(boolean included) {
		this.included = included;
		return this;
	}

	@Override
	public String toString() {
		return String.format("StagingStatus: package %s, included %b", packageName, included);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StagingStatus) {
			StagingStatus other = (StagingStatus) obj;
			return Objects.equals(packageName, other.packageName) && Objects.equals(included, other.included);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, included);
	}
}
