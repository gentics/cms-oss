package com.gentics.contentnode.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.filter.AbstractSSOFilter;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Test filter extending the {@link AbstractSSOFilter} which exposes a method to
 * get the user groups for a specific set of user attributes.
 */
public class TestSsoFilter extends AbstractSSOFilter {

	/** Counter for created users.
	 *
	 * Whenever {@link com.gentics.contentnode.auth.filter.AbstractSSOFilter#getSystemUser(java.lang.String, java.util.Map) getSystemUser()}
	 * is called a new user should be created to prevent collisions. The running counter is appended to each call to
	 * {@code getSystemUser()}.
	 */
	private int createdUserCount = 0;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// The tests don't use doFilter() but all call getSystemUser() with a non-existing username and the SSO attributes.
		throw new NotImplementedException();
	}

	public Map<Integer, Set<Integer>> getUserGroups(Map<String, Object> attributes) throws NodeException {
		var user = getSystemUser("non-existing-user-%04d".formatted(++createdUserCount), attributes);
		var restrictions = new HashMap<>(user.getGroupNodeRestrictions());

		for (var group: user.getUserGroups()) {
			restrictions.putIfAbsent(group.getId(), null);
		}

		return restrictions;
	}
}
