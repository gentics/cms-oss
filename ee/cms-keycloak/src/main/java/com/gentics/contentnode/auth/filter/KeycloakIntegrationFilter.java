package com.gentics.contentnode.auth.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

//import org.keycloak.KeycloakPrincipal;
//import org.keycloak.KeycloakSecurityContext;
//import org.keycloak.representations.AccessToken;
//import org.keycloak.representations.AccessToken.Access;

import com.gentics.lib.base.MapResolver;

/**
 * Keycloak integration filter for the Gentics CMS.
 *
 * When the user principal in the current request is a <code>KeycloakPrincipal</code>,
 * the necessary user information is extracted from its security contexts access token.
 *
 * The user will be newly created if it does not yet exist.
 *
 * Then a new session will be created and the session secret (as cookie)
 * and session id (as request parameter) will be added to the request, before
 * continuing with the next filter/servlet in the filter chain.
 */
public class KeycloakIntegrationFilter extends AbstractSSOFilter {

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//		HttpServletRequest httpRequest = (HttpServletRequest) request;
//		AccessToken token = getToken(httpRequest);
//
//		if (token == null) {
//			logger.warn("Could not get access token from request, skipping SSO login.");
//		} else {
//			Map<String, Object> attributes = new HashMap<>();
//
//			attributes = new HashMap<>();
//			attributes.put("firstname", token.getGivenName());
//			attributes.put("lastname", token.getFamilyName());
//			attributes.put("email", token.getEmail());
//
//			Map<String, Object> roles = new HashMap<>();
//			Map<String, Set<String>> access = new HashMap<>();
//
//			if (token.getResourceAccess() != null) {
//				for (Map.Entry<String, Access> entry : token.getResourceAccess().entrySet()) {
//					access.put(entry.getKey(), entry.getValue().getRoles());
//				}
//			}
//
//			if (token.getRealmAccess() != null) {
//				roles.put("realm", token.getRealmAccess().getRoles());
//			}
//			roles.put("resource", new MapResolver(access));
//
//			attributes.put("roles", new MapResolver(roles));
//
//			request = doSSOLogin(httpRequest, token.getPreferredUsername(), attributes);
//		}
//
		chain.doFilter(request, response);
	}

//	/**
//	 * Get the access token from the supplied request.
//	 *
//	 * @param request The current servlet request
//	 * @return The Keycloack access token or <code>null</code> when the requests
//	 *		user principal is not a <code>KeycloakPrincipal</code>.
//	 */
//	private AccessToken getToken(HttpServletRequest request) {
//		Principal principal = request.getUserPrincipal();
//
//		if (principal instanceof KeycloakPrincipal) {
//			@SuppressWarnings("unchecked")
//			KeycloakSecurityContext ctx = ((KeycloakPrincipal<KeycloakSecurityContext>) principal).getKeycloakSecurityContext();
//
//			if (ctx != null) {
//				return ctx.getToken();
//			}
//		}
//
//		return null;
//	}
			}
