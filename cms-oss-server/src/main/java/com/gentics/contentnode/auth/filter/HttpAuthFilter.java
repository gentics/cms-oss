package com.gentics.contentnode.auth.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.etc.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication Filter for HTTP Authentication
 */
public class HttpAuthFilter extends AbstractSSOFilter {

	/**
	 * Names of the attributes fetched from the headers
	 */
	protected final static String[] ATTR_NAMES = { "firstname", "lastname", "email", "group"};

	/**
	 * Expression to determine the initial groups
	 */
	protected EvaluableExpression initGroupsExpression;

	@Override
	protected String getDefaultInitGroupExpression() {
		return "attr.group";
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		super.init(config);

		String initGroupsExpressionString = ObjectTransformer.getString(config.getInitParameter(INIT_GROUPS_PARAM), getDefaultInitGroupExpression());

		if (StringUtils.isEmpty(initGroupsExpressionString)) {
			throw new ServletException("init-param " + INIT_GROUPS_PARAM + " is empty or missing");
		}

		try {
			ExpressionParser parser = ExpressionParser.getInstance();

			initGroupsExpression = (EvaluableExpression) parser.parse(initGroupsExpressionString);
		} catch (ParserException e) {
			throw new ServletException("Unable to parse " + INIT_GROUPS_PARAM, e);
		}
	}

	/* (non-Javadoc)
	 * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
		NodePreferences pref = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();

		if (pref.isFeature(Feature.HTTP_AUTH_LOGIN)) {
			Map httpAuthLogin = pref.getPropertyMap("http_auth_login");
			Map<String, Object> attributes = new HashMap<String, Object>();

			String login = getAuthParam(httpAuthLogin, httpServletRequest, "login");

			for (String attrName : ATTR_NAMES) {
				attributes.put(attrName, getAuthParam(httpAuthLogin, httpServletRequest, attrName));
			}

			// special treatment for group
			String group = ObjectTransformer.getString(attributes.get("group"), null);

			if (!StringUtils.isEmpty(group)) {
				String[] groups = StringUtils.splitString(group, ObjectTransformer.getString(httpAuthLogin.get("splitter"), ";"));

				attributes.put("group", groups);
			}

			if (!StringUtils.isEmpty(login)) {
				request = doSSOLogin(httpServletRequest, login, attributes);
			}
		}

		chain.doFilter(request, response);
	}

	/**
	 * Get the systemuser from the given principal. If none found, create a
	 * systemuser, otherwise synchronize firstname, lastname and email
	 *
	 * @param principal
	 *            principal
	 * @return systemuser
	 * @throws NodeException
	 */
	protected SystemUser getSystemUser(String login, Map<String, Object> attributes) throws NodeException {
		boolean newUser;
		Transaction t = TransactionManager.getCurrentTransaction();
		String firstname = ObjectTransformer.getString(attributes.get("firstname"), "");
		String lastname = ObjectTransformer.getString(attributes.get("lastname"), "");
		String email = ObjectTransformer.getString(attributes.get("email"), "");

		// get the user with given login
		SystemUser systemUser = ((SystemUserFactory) t.getObjectFactory(SystemUser.class))
				.getSystemUser(login, null, false);

		if (systemUser == null) {
			// did not find the systemuser, so create a new one
			systemUser = t.createObject(SystemUser.class);
			// set the login
			systemUser.setLogin(login);
			// set 'SSO' as encrypted password (this will make it impossible to login by user credentials)
			systemUser.setPassword("SSO");
			// set the user active
			systemUser.setActive(true);

			// now use the expression to find the initial groups
			Map<String, Resolvable> data = new HashMap<String, Resolvable>();

			data.put("attr", new MapResolver(attributes));
			data.put("user", systemUser);
			PropertyResolver propertyResolver = new PropertyResolver(new MapResolver(data));
			Collection<?> initGroups = ObjectTransformer.getCollection(
					initGroupsExpression.evaluate(new ExpressionQueryRequest(propertyResolver, null), ExpressionEvaluator.OBJECTTYPE_ANY), null);

			boolean haveGroups = false;

			Collection<UserGroup> groups = systemUser.getUserGroups();
			Map<Integer, Set<Integer>> restrictions = systemUser.getGroupNodeRestrictions();

			for (Object object : initGroups) {
				Map<Integer, Set<Integer>> groupIdsWithRestrictions = parseGroupId(ObjectTransformer.getString(object, ""));

				for (Map.Entry<Integer, Set<Integer>> entry : groupIdsWithRestrictions.entrySet()) {
					int groupId = entry.getKey();
					Set<Integer> groupRestrictions = entry.getValue();

					// we ignore super groups (1 and 2)
					logger.debug("Resolved group: " + groupId);
					if (groupId > 2) {
						UserGroup group = t.getObject(UserGroup.class, groupId);

						if (group != null) {
							haveGroups = true;

							if (!groups.contains(group)) {
								groups.add(group);
							}

							if (groupRestrictions != null) {
								logger.debug("Restrict assignment to group " + groupId + " to nodes: " + groupRestrictions);
								restrictions.put(groupId, groupRestrictions);
							} else {
								logger.debug("Assignment to group " + groupId + " is not restricted to nodes");
								restrictions.remove(groupId);
							}
						} else {
							logger.warn("Could not find group " + groupId + " when syncing user " + login);
						}
					}
				}
			}
			if (!haveGroups) {
				// NOTE: We assume here that the current transaction will be rolled back in the caller.
				logger.error("Group expression did not yield any groups, aborting system user creation");
				return null;
			}

			newUser = true;
		} else {
			// found the systemuser, so get an editable copy
			systemUser = t.getObject(SystemUser.class, systemUser.getId(), true);
			newUser = false;
		}

		// update the user data
		systemUser.setFirstname(firstname);
		systemUser.setLastname(lastname);
		systemUser.setEmail(email);

		if (syncGroups) {
			logger.debug("Synchronizing groups for " + systemUser);

			Map<String, Resolvable> data = new HashMap<String, Resolvable>();

			data.put("attr", new MapResolver(attributes));
			data.put("user", systemUser);
			PropertyResolver propertyResolver = new PropertyResolver(new MapResolver(data));
			Collection<?> initGroups = ObjectTransformer.getCollection(
					initGroupsExpression.evaluate(new ExpressionQueryRequest(propertyResolver, null), ExpressionEvaluator.OBJECTTYPE_ANY), null);
			Collection<UserGroup> groups = systemUser.getUserGroups();
			Map<Integer, Set<Integer>> restrictions = systemUser.getGroupNodeRestrictions();
			Collection<Integer> tmpInitGroups = new ArrayList<Integer>();

			for (Object object : initGroups) {
				Map<Integer, Set<Integer>> groupIdsWithRestrictions = parseGroupId(ObjectTransformer.getString(object, ""));
				for (Map.Entry<Integer, Set<Integer>> entry : groupIdsWithRestrictions.entrySet()) {
					int groupId = entry.getKey();
					Set<Integer> groupRestrictions = entry.getValue();

					logger.debug(systemUser + " must be member of group " + groupId);

					tmpInitGroups.add(groupId);
					// we ignore super groups (1 and 2)
					if (groupId > 2) {
						UserGroup group = t.getObject(UserGroup.class, groupId);

						if (group != null) {
							if (!groups.contains(group)) {
								logger.debug("Adding " + systemUser + " to " + group);
								groups.add(group);
							}

							if (groupRestrictions != null) {
								logger.debug("Restrict assignment to " + group + " to nodes: " + groupRestrictions);
								restrictions.put(groupId, groupRestrictions);
							} else {
								logger.debug("Assignment to " + group + " is not restricted to nodes");
								restrictions.remove(groupId);
							}
						} else {
							logger.warn("Could not find group " + groupId + " when syncing user " + login);
						}
					}
				}

			}

			for (Iterator<UserGroup> i = groups.iterator(); i.hasNext();) {
				UserGroup currentUserGroup = i.next();

				if (!tmpInitGroups.contains(currentUserGroup.getId())) {
					logger.debug("Removing " + systemUser + " from " + currentUserGroup);
					i.remove();
				}
			}
		}
		// save the systemuser
		systemUser.save();

		if (newUser && userCreatedCallback != null) {
			User user = SystemUser.TRANSFORM2REST.apply(systemUser);

			user.setLogin(systemUser.getLogin());
			userCreatedCallback.accept(user, attributes);
		}

		return systemUser;
	}

	/**
	 * Get the a header, as defined in the map
	 * @param httpAuthLogin map defining the header names
	 * @param request request
	 * @param name name of header value
	 * @return header value
	 */
	protected String getAuthParam(Map httpAuthLogin, HttpServletRequest request, String name) {
		String headerName = ObjectTransformer.getString(httpAuthLogin.get(name), null);

		if (StringUtils.isEmpty(headerName)) {
			return null;
		}

		if (headerName.startsWith("HTTP_")) {
			headerName = headerName.substring(5);
			return request.getHeader(headerName);
		} else {
			return null;
		}
	}
}
