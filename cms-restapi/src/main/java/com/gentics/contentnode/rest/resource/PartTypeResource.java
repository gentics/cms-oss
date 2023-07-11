package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import java.util.List;
import javax.ws.rs.BeanParam;

public abstract class PartTypeResource {

	/**
	 * List Part types
	 * @param filter filter parameters
	 * @return list of part type
	 * @throws Exception in case of errors
	 */
	public abstract List<PartType> list(
			@BeanParam FilterParameterBean filter) throws Exception;

}
