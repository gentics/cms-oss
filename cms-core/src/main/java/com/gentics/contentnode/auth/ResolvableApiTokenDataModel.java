package com.gentics.contentnode.auth;

import com.gentics.api.lib.resolving.IResolvableBean;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.model.token.ApiTokenDataModel;

/**
 * Extension to {@link ApiTokenDataModel} which makes it {@link Resolvable} by implementing {@link IResolvableBean}
 */
public class ResolvableApiTokenDataModel extends ApiTokenDataModel implements IResolvableBean {

	private static final long serialVersionUID = -7524893769004229167L;

}
