package com.gentics.contentnode.rest.resource.impl.scheduler;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.model.response.scheduler.JobStatus;
import com.gentics.lib.util.ClassHelper;

/**
 * Resolvable subclass of JobStatus
 */
public class ResolvableJobStatus extends JobStatus implements Resolvable {

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		// simply call the getter on the object
		try {
			return ClassHelper.invokeGetter(this, key);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean canResolve() {
		return true;
	}
}
