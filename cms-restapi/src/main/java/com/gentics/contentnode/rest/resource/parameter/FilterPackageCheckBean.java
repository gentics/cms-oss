package com.gentics.contentnode.rest.resource.parameter;

import com.gentics.contentnode.rest.model.devtools.dependency.Type;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlEnum;

/**
 * Parameter bean to filter the result of a package check
 */
public class FilterPackageCheckBean {
	/**
	 * Type filter parameter
	 */
	@QueryParam("type")
	public Set<Type> type;

	/**
	 * Query string for filtering
	 */
	@QueryParam("filter")
	@DefaultValue("INCOMPLETE")
	public Filter completeness;


	/**
	 * Set the completnes filter
	 * @param filterValue filter param
	 * @return fluent API
	 */
	public FilterPackageCheckBean withCompletenessFilter(String filterValue) {
		this.completeness = Filter.fromString(filterValue);
		return this;
	}

	/**
	 * Set the type filter
	 * @param filter filter param
	 * @return
	 */
	public FilterPackageCheckBean withTypeFilter(Set<Type> filter) {
		this.type = filter;
		return this;
	}

	/**
	 * The dependency filter enum
	 */
	@XmlEnum()
	public enum Filter {
		INCOMPLETE,
		ALL;

		/**
		 * Utility method to obtain the type enum from a given string (case-insensitive)
		 *
		 * @param value the value that should be converted to the type
		 * @return the type enum
		 */
		public static Filter fromString(String value) {
			String toUpper = value.toUpperCase();
			try {
				return valueOf(toUpper);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

}
