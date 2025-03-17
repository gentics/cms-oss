package com.gentics.contentnode.auth.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.contentnode.auth.filter.SsoUserCreatedCallback;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;

/**
 * Abstract base class for SSO Authentication Filters.
 * Provides the common functionality and can be subclassed for specific Implementation Filters.
 */
public abstract class AbstractSSOFilter implements Filter {

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Name of the filter config parameter containing the expression holding the initial groups
	 */
	public final static String INIT_GROUPS_PARAM = "initGroups";

	/**
	 * Name of the config parameter containing whether the usergroups should synchronizing if parameter is true
	 */
	public final static String INIT_GROUPS_SYNC = "syncGroups";

	protected static Boolean syncGroups = false;

	public final static String INIT_CALLBACK = "userCreatedCallback";

	/**
	 * Lock Manager for the sso lock (by login name)
	 */
	private final static KeyLockManager ssoLock = KeyLockManagers.newLock();

	protected Map<String, Map<Integer, Set<Integer>>> initGroupsMapping = new HashMap<>();

	/**
	 * Pattern for definition of groups, that are restricted to nodes
	 */
	protected Pattern groupNodeRestrictionPattern = Pattern.compile("([0-9]+)\\|([0-9\\~]+)");

	protected SsoUserCreatedCallback userCreatedCallback;

	/* (non-Javadoc)
	 * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void init(FilterConfig config) throws ServletException {
		String initGroupsPath = ObjectTransformer.getString(config.getInitParameter(INIT_GROUPS_PARAM), "");

		if (StringUtils.isEmpty(initGroupsPath)) {
			throw new ServletException("init-param " + INIT_GROUPS_PARAM + " is empty or missing");
		}

		var initGroupsDef = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap(initGroupsPath);

		try (Trx trx = new Trx()) {
			prepareInitGroupsMapping(initGroupsDef);
			trx.success();
		} catch (NodeException e) {
			throw new ServletException("Could not create init groups mapping: %s".formatted(e.getMessage()), e);
		}

		syncGroups = ObjectTransformer.getBoolean(config.getInitParameter(INIT_GROUPS_SYNC), false);

		String callbackClassName = ObjectTransformer.getString(config.getInitParameter(INIT_CALLBACK), "");

		if (callbackClassName.isEmpty()) {
			return;
		}

		Class<?> callbackClass;
		Class<SsoUserCreatedCallback> superClass = SsoUserCreatedCallback.class;

		try {
			callbackClass = Class.forName(callbackClassName, true, AbstractSSOFilter.class.getClassLoader());

			if (!superClass.isAssignableFrom(callbackClass)) {
				throw new ServletException("Specified callback class " + callbackClassName + " is not an implementation of " + superClass.getSimpleName());
			}

			userCreatedCallback = ((Class<SsoUserCreatedCallback>) callbackClass).newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new ServletException("Could not create instance of callback class", e);
		}
	}

	/**
	 * Create the mapping from user attributes to user groups and restrictions from the @{code init_groups} configuration.
	 * @param initGroupsDef The {@code init_groups} configuration.
	 */
	private void prepareInitGroupsMapping(Map<String, Object> initGroupsDef) throws NodeException {
		for (var entry: initGroupsDef.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			var groupMapping = initGroupsMapping.computeIfAbsent(key, k -> new HashMap<>());

			if (value instanceof String groupName) {
				addInitGroupMapping(groupMapping, groupName, null);
			} else if (value instanceof List<?> groupList) {
				for (var groupDef: groupList) {
					if (groupDef instanceof String groupName) {
						addInitGroupMapping(groupMapping, groupName, null);
					} else if (groupDef instanceof Map<?, ?> groupDefWithRestrictions) {
						addInitGroupMappingWithRestrictions(key, groupMapping, groupDefWithRestrictions);
					}
				}
			} else if (logger.isWarnEnabled()){
				logger.warn("Unexpected value for key '%s' (%s): %s".formatted(key, value == null ? "N/A" : value.getClass(), value));
			}
		}
	}

	/**
	 * Add the mapping from group ID to node restrictions defined by {@code groupDefWithRestrictions} and add it to
	 * {@code groupMapping}.
	 *
	 * <p>
	 *     The {@code groupDefWithRestrictions} must contain a value for the key {@code group} which is the desirec
	 *     group name for the mapping and a value for the key {@code nodes} which contains a list of node names or
	 *     global node IDs wo which the group assignment should be restricted.
	 * </p>
	 *
	 * @param key The user attribute that is currently processed (used only for logging).
	 * @param groupMapping The mapping to add the contents of {@code groupDefWithRestrictions} to.
	 * @param groupDefWithRestrictions A mapping containing the group name and a list of node restrictions.
	 * @throws NodeException
	 */
	private void addInitGroupMappingWithRestrictions(String key, Map<Integer, Set<Integer>> groupMapping, Map<?, ?> groupDefWithRestrictions) throws NodeException {
		var groupName = Optional.ofNullable(groupDefWithRestrictions.get("group"))
			.map(Object::toString)
			.orElse("");

		if (groupName.isEmpty()) {
			logger.warn("Group definition with restrictions for '%s' does not contain a group name".formatted(key));

			return;
		}

		var nodesValue = groupDefWithRestrictions.get("nodes");

		if (nodesValue instanceof String nodeName) {
			addInitGroupMapping(groupMapping, groupName, getNodeIds(Collections.singletonList(nodeName)));
		} else if (nodesValue instanceof List<?> nodesList) {
			var nodeNames = nodesList.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.toList();

			addInitGroupMapping(groupMapping, groupName, getNodeIds(nodeNames));
		} else if (nodesValue == null) {
			addInitGroupMapping(groupMapping, groupName, null);
		} else if (logger.isWarnEnabled()) {
			logger.warn("Unexpected value for 'nodes' in mapping '%s': %s".formatted(key, nodesValue));
		}
	}

	/**
	 * Resolve the group ID for {@code groupName} and if found, add the mapping from the group ID to the given
	 * {@code restrictions} to the existing {@code mapping}.
	 * @param mapping The existing mappings.
	 * @param groupName The group name to be resolved and added to the mapping.
	 * @param restrictions The node restrictions for group assignment.
	 */
	private void addInitGroupMapping(Map<Integer, Set<Integer>> mapping, String groupName, Set<Integer> restrictions) throws NodeException {
		getGroupId(groupName).ifPresent(groupId -> mapping.put(groupId, restrictions));
	}

	/**
	 * Get the ID for the given group name.
	 * @param groupName The group name to search the ID for.
	 * @return An {@link Optional} containing the found group ID or an empty optional if there is no group with the
	 * 		specified name.
	 */
	private Optional<Integer> getGroupId(String groupName) throws NodeException {
		var groupIds = DBUtils.select(
			"SELECT `id` FROM `usergroup` WHERE name = ?",
			stmt -> stmt.setString(1, groupName),
			DBUtils.IDS);

		if (groupIds.isEmpty()) {
			logger.warn("No group found with name '" + groupName + "'");
		} else if (groupIds.size() > 1) {
			logger.warn("More than one group found with name '" + groupName + "'");
		}

		return groupIds.stream().findFirst();
	}

	/**
	 * Get the node IDs for the given list of node names or global node IDs.
	 *
	 * <p>
	 *     <em>Note:</em> If some of the names or global IDs cannot be found, a warning will be logged and the
	 *     result will contain only the IDs that were found.
	 * </p>
	 *
	 * @param nodeNamesOrGlobalIds A list of node names or global node IDs. The list may contain node names and global
	 * 		IDs at the same time.
	 * @return A {@link Set} containing the found node IDs.
	 */
	private Set<Integer> getNodeIds(List<String> nodeNamesOrGlobalIds) throws NodeException {
		var nodeIds = new HashSet<Integer>(nodeNamesOrGlobalIds.size());
		var remaining = new HashSet<String>();

		try (Trx trx = new Trx()) {
			for (var nodeId: nodeNamesOrGlobalIds) {
				var node = trx.getTransaction().getObject(Node.class, nodeId);

				if (node != null) {
					nodeIds.add(node.getId());
				} else {
					remaining.add(nodeId);
				}
			}
		}

		var numRemaining = remaining.size();
		var placeHolders = String.join(", ", Collections.nCopies(numRemaining, "?"));

		DBUtils.select(
			"SELECT n.id FROM node n JOIN folder f ON n.folder_id = f.id WHERE f.name in (%s)".formatted(placeHolders),
			stmt ->  {
				for (var i = 1; i <= numRemaining; i++) {
					stmt.setString(i, nodeNamesOrGlobalIds.get(i - 1));
				}
			},
			rs -> {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}

				return nodeIds;
			});

		var expectedNumNodes = nodeNamesOrGlobalIds.size();

		if (nodeIds.size() != expectedNumNodes && logger.isWarnEnabled()) {
			logger.warn("Only %d/%d node IDs where found for [%s]".formatted(nodeIds.size(), expectedNumNodes, String.join(", ", nodeNamesOrGlobalIds)));
		}

		return nodeIds;
	}

	/* (non-Javadoc)
	 * @see jakarta.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {}

	/**
	 * Get the default init group expression
	 * @return default init group expression
	 */
	protected String getDefaultInitGroupExpression() {
		return null;
	}

	/**
	 * Start a new transaction
	 * @return transaction
	 * @throws NodeException
	 */
	protected Transaction startTransaction() throws NodeException {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();

		return factory.startTransaction(true);
	}

	/**
	 * Do the SSO Login:
	 * <ol>
	 *  <li>Create the SystemUser, if not found</li>
	 *  <li>Synchronize the SystemUser data</li>
	 *  <li>Create a new Session for the SystemUser</li>
	 *  <li>Return a wrapped ServletRequest that contains the session identification (session secret cookie and sid as parameter)</li>
	 * </ol>
	 * @param request original request
	 * @param login login name of the user
	 * @param attributes additional parameters
	 * @return wrapped (authenticated) servlet request
	 * @throws ServletException
	 */
	protected ServletRequest doSSOLogin(HttpServletRequest request, String login, Map<String, Object> attributes) throws ServletException {
		if (StringUtils.isBlank(login)) {
			throw new ServletException("Error while SSO: login name was empty");
		}

		try {
			return ssoLock.executeLocked(login, () -> {
				try {
					try (Trx trx = new Trx()) {
						SystemUser systemUser = getSystemUser(login, attributes);

						if (systemUser == null) {
							// Do NOT call trx.success() to prevent the user being saved.
							return request;
						}

						boolean createNewSession = false;

						// now check whether the request is done with a session
						try {
							SessionToken sessionToken = new SessionToken(request);
							Session session = new Session(sessionToken.getSessionId(), trx.getTransaction());

							if (sessionToken.authenticates(session)) {
								// check whether the session belongs to the same user, if
								// not -> do logout for the old session and create a new one
								if (session.getUserId() != ObjectTransformer.getInt(systemUser.getId(), -1)) {
									// do logout for the old session and create a new one
									session.logout();
									createNewSession = true;
								}
							}

						} catch (InvalidSessionIdException e) {
							// no session -> create a new one
							createNewSession = true;
						}

						if (createNewSession) {
							Session session = new Session(systemUser, request.getRemoteAddr(), request.getHeader("user-agent"),
									SessionToken.getSessionSecretFromRequestCookie(request), 0);

							// replace the request with the wrapper and set the session
							// information
							trx.success();
							return new AuthenticatedHttpServletRequestWrapper(request, session);
						} else {
							trx.success();
							return request;
						}
					}
				} catch (NodeException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw new ServletException(e.getCause());
			} else {
				throw new ServletException(e);
			}
		}
	}

	/**
	 * Get the systemuser from the given principal. If none found, create a
	 * systemuser, otherwise synchronize firstname, lastname and email
	 *
	 * @param login The username.
	 * @param attributes The authentication attributes.
	 * @return The loaded or created system user.
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

			var haveGroups = false;

			Collection<UserGroup> groups = systemUser.getUserGroups();
			Map<Integer, Set<Integer>> restrictions = systemUser.getGroupNodeRestrictions();

			for (var key: initGroupsMapping.keySet()) {
				if (!checkAttribute(attributes, key)) {
					continue;
				}

				var groupIdsWithRestrictions = initGroupsMapping.get(key);

				if (groupIdsWithRestrictions == null) {
					continue;
				}

				for (var entry: groupIdsWithRestrictions.entrySet()) {
					var groupId = entry.getKey();
					var groupRestrictions = entry.getValue();

					// We ignore super groups (1 and 2).
					if (groupId <= 2) {
						continue;
					}

					if (logger.isDebugEnabled()) {
						logger.debug("Resolved group: " + groupId);
					}

					var group = t.getObject(UserGroup.class, groupId);

					if (group != null) {
						haveGroups = true;

						if (!groups.contains(group)) {
							groups.add(group);
						}

						if (groupRestrictions != null) {
							if (logger.isDebugEnabled()) {
								logger.debug("Restrict assignment to group " + groupId + " to nodes: " + groupRestrictions);
							}

							restrictions.put(groupId, groupRestrictions);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Assignment to group " + groupId + " is not restricted to nodes");
							}

							restrictions.remove(groupId);
						}
					} else {
						logger.warn("Could not find group " + groupId + " when syncing user " + login);
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
			if (logger.isDebugEnabled()) {
				logger.debug("Synchronizing groups for " + systemUser);
			}

			Collection<UserGroup> groups = systemUser.getUserGroups();
			Map<Integer, Set<Integer>> restrictions = systemUser.getGroupNodeRestrictions();
			Collection<Integer> tmpInitGroups = new ArrayList<Integer>();

			for (var key: initGroupsMapping.keySet()) {
				if (!checkAttribute(attributes, key)) {
					continue;
				}

				var groupIdsWithRestrictions = initGroupsMapping.get(key);

				if (groupIdsWithRestrictions == null) {
					continue;
				}

				for (var entry: groupIdsWithRestrictions.entrySet()) {
					var groupId = entry.getKey();
					var groupRestrictions = entry.getValue();

					if (logger.isDebugEnabled()) {
						logger.debug(systemUser + " must be member of group " + groupId);
					}

					tmpInitGroups.add(groupId);

					// We ignore super groups (1 and 2).
					if (groupId <= 2) {
						continue;
					}

					UserGroup group = t.getObject(UserGroup.class, groupId);

					if (group != null) {
						if (!groups.contains(group)) {
							if (logger.isDebugEnabled()) {
								logger.debug("Adding " + systemUser + " to " + group);
							}

							groups.add(group);
						}

						if (groupRestrictions != null) {
							if (logger.isDebugEnabled()) {
								logger.debug("Restrict assignment to " + group + " to nodes: " + groupRestrictions);
							}

							restrictions.put(groupId, groupRestrictions);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Assignment to " + group + " is not restricted to nodes");
							}

							restrictions.remove(groupId);
						}
					} else if (logger.isWarnEnabled()) {
						logger.warn("Could not find group " + groupId + " when syncing user " + login);
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
	 * Check if the map {@code attributes} contains an entry for the path {@code key}.
	 *
	 * <p>
	 *     If {@code key} contains at least one {@code '.'} character the part before the first {@code '.'} is
	 *     used as a key for {@code attributes}. If there is an assigned value the result of this method depends
	 *     on the type of the value:
	 *     <ol>
	 *         <li>
	 *             Value is a {@link Map}: {@code checkAttributes()} is called
	 *             recursively with the remaining part of {@code key} being searched in the new map.
	 *         </li>
	 *         <li>
	 *             Value is a {@link Collection}: Returns {@code true} if the remaining part of {@code key} is
	 *             a value in the collection and {@code false} otherwise.
	 *         </li>
	 *         <li>
	 *             Value is a {@link String}: Returns {@code true} if the remaining part of {@code key} is equal
	 *             to the value and {@code false} otherwise.
	 *         </li>
	 *     </ol>
	 *     In all other cases this method returns {@code false}.
	 * </p>
	 *
	 * @param attributes The attributes to check.
	 * @param key The key to check for.
	 * @return {@code true} if the {@code key} can be resolved in the {@code attributes}.
	 */
	private boolean checkAttribute(Map<?, ?> attributes, String key) {
		var idx = key.indexOf(".");

		if (idx < 0) {
			return false;
		}

		var keyHead = key.substring(0, idx);
		var value = attributes.get(keyHead);

		if (value instanceof Map<?, ?> subAttributes) {
			return checkAttribute(subAttributes, key.substring(idx + 1));
		} else if (value instanceof Collection<?> collection) {
			var keyTail =  key.substring(idx + 1);

			return collection.contains(keyTail);
		} else if (value instanceof String string) {
			return string.equals(key);
		}

		return false;
	}

	/**
	 * Parse the given value into either a single groupId or a groupId with a set of nodeIds (to restrict the assignment)
	 * @param value value to parse
	 * @return map containing the groupId as key and optionally the set of nodeIds as value (or null as value)
	 */
	protected Map<Integer, Set<Integer>> parseGroupId(String value) {
		Matcher m = groupNodeRestrictionPattern.matcher(value);
		Map<Integer, Set<Integer>> groupWithRestrictions = new HashMap<Integer, Set<Integer>>(1);
		if (m.matches()) {
			int groupId = ObjectTransformer.getInt(m.group(1), -1);
			int[] nodeIds = com.gentics.lib.etc.StringUtils.splitInt(m.group(2), "~");
			Set<Integer> nodeIdSet = new HashSet<Integer>();
			for (int nodeId : nodeIds) {
				nodeIdSet.add(nodeId);
			}
			groupWithRestrictions.put(groupId, nodeIdSet);
		} else {
			int groupId = ObjectTransformer.getInt(value, -1);
			groupWithRestrictions.put(groupId, null);
		}

		return groupWithRestrictions;
	}

	/**
	 * Servlet Request Wrapper for adding session secret (as cookie) and sid (as parameter)
	 */
	public static class AuthenticatedHttpServletRequestWrapper extends HttpServletRequestWrapper {

		/**
		 * Cookies
		 */
		protected Vector<Cookie> cookies;

		/**
		 * Parameter Map
		 */
		protected Map<String, String[]> parameterMap;

		/**
		 * Create an instance wrapping the given request
		 * @param request wrapped request
		 * @param session the current session
		 */
		public AuthenticatedHttpServletRequestWrapper(HttpServletRequest request, Session session) {
			super(request);

			// get the cookies
			cookies = new Vector<Cookie>();
			Cookie[] requestCookies = request.getCookies();

			if (requestCookies != null) {
				for (Cookie c : requestCookies) {
					if (!SessionToken.SESSION_SECRET_COOKIE_NAME.equals(c.getName())) {
						cookies.add(c);
					}
				}
			}

			// create and add the session secret cookie
			Cookie sessionSecretCookie = new Cookie(SessionToken.SESSION_SECRET_COOKIE_NAME, session.getSessionSecret());

			sessionSecretCookie.setPath("/");

			cookies.add(sessionSecretCookie);

			// get the parameters
			parameterMap = new HashMap<String, String[]>(request.getParameterMap().size() + 1);
			for (Object nameO : EnumerationUtils.toList(request.getParameterNames())) {
				String name = ObjectTransformer.getString(nameO, null);

				parameterMap.put(name, request.getParameterValues(name));
			}

			// set the sid as parameter
			parameterMap.put("sid", new String[] { ObjectTransformer.getString(session.getSessionId(), null)});
		}

		/* (non-Javadoc)
		 * @see jakarta.servlet.http.HttpServletRequestWrapper#getCookies()
		 */
		@Override
		public Cookie[] getCookies() {
			return cookies.toArray(new Cookie[cookies.size()]);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return parameterMap;
		}

		@Override
		public String getParameter(String name) {
			String[] values = getParameterValues(name);

			if (values != null && values.length > 0) {
				return values[0];
			} else {
				return null;
			}
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return new IteratorEnumeration(parameterMap.keySet().iterator());
		}

		@Override
		public String[] getParameterValues(String name) {
			return parameterMap.get(name);
		}
	}
}
