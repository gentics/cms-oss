package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class ContentPackageListParameterBean {
	@QueryParam("stats")
	@DefaultValue("true")
	public boolean stats = true;
}
