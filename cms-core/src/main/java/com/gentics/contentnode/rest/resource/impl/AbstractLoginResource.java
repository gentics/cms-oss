package com.gentics.contentnode.rest.resource.impl;

import com.gentics.contentnode.etc.LoginService;
import com.gentics.contentnode.etc.ServiceLoaderUtil;

public abstract class AbstractLoginResource extends AbstractContentNodeResource {

	/**
	 * Service loader that finds implementations of the LoginService interface
	 */
	protected final static ServiceLoaderUtil<LoginService> LOGIN_SERVICE_LOADER = ServiceLoaderUtil.load(LoginService.class);
}
