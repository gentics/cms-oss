package com.gentics.contentnode.auth;

import com.gentics.api.lib.resolving.IResolvableBean;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.model.token.APITokenDataModel;

/**
 * Extension to {@link APITokenDataModel} which makes it {@link Resolvable} by implementing {@link IResolvableBean}
 */
public class ResolvableAPITokenDataModel extends APITokenDataModel implements IResolvableBean {

	private static final long serialVersionUID = -7524893769004229167L;

}
