package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.ObjectPropertyType;

/**
 * Parameter bean for filtering object properties
 */
public class ObjectPropertyParameterBean {
	/**
	 * Filter by object type(s)
	 */
	@QueryParam("type")
	public Set<ObjectPropertyType> types;
}
