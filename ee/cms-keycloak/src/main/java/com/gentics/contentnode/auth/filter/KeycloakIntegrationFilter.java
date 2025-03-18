package com.gentics.contentnode.auth.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.base.MapResolver;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Keycloak integration filter for the Gentics CMS.
 *
 * When a Authorization Bearer Token is found in the request it will be verified with the keys loaded from keycloak,
 * and the necessary user information is extracted from it.
 *
 * The user will be newly created if it does not yet exist.
 *
 * Then a new session will be created and the session secret (as cookie)
 * and session id (as request parameter) will be added to the request, before
 * continuing with the next filter/servlet in the filter chain.
 */
public class KeycloakIntegrationFilter extends AbstractSSOFilter {
	/**
	 * Name of the init parameter containing the keycloak URL
	 */
	public final static String INIT_KEYCLOAK_URL_PARAM = "keycloakUrl";

	/**
	 * Name of the init parameter containing the expected issuer
	 */
	public final static String INIT_KEYCLOAK_ISSUER = "keycloakIssuer";

	/**
	 * Name of the JWT claim for the preferred username
	 */
	protected final static String PREFERRED_USERNAME_CLAIM = "preferred_username";

	/**
	 * Name of the JWT claim for the given name
	 */
	protected static final String GIVEN_NAME_CLAIM = "given_name";

	/**
	 * Name of the family name claim
	 */
	protected static final String FAMILY_NAME_CLAIM = "family_name";

	/**
	 * Name of the email claim
	 */
	protected static final String EMAIL_CLAIM = "email";

	/**
	 * Name of the realm_access claim
	 */
	protected static final String REALM_ACCESS_CLAIM = "realm_access";

	/**
	 * Name of the resource_access claim
	 */
	protected static final String RESOURCE_ACCESS_CLAIM = "resource_access";

	/**
	 * Name of the roles claim
	 */
	protected static final String ROLES_CLAIM = "roles";

	/**
	 * Pattern for extracting the Authorization Bearer Token
	 */
	protected final static Pattern AUTHORIZATION_BEARER_PATTERN = Pattern.compile("Bearer (?<token>.*)");

	/**
	 * Keycloak Certs URL
	 */
	protected String keycloakCertsUrl;

	/**
	 * Expected issuer
	 */
	protected String issuer;

	/**
	 * Processor for processing the tokens
	 */
	protected ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

	/**
	 * Key Source (which will be keycloak)
	 */
	protected JWKSource<SecurityContext> keySource;

	@Override
	public void init(FilterConfig config) throws ServletException {
		super.init(config);

		keycloakCertsUrl = config.getInitParameter(INIT_KEYCLOAK_URL_PARAM);
		if (StringUtils.isBlank(keycloakCertsUrl)) {
			throw new ServletException("Cannot create filter: keycloakUrl must not be empty");
		}

		issuer = config.getInitParameter(INIT_KEYCLOAK_ISSUER);
		if (StringUtils.isBlank(issuer)) {
			throw new ServletException("Cannot create filter: keycloakIssuer must not be empty");
		}

		try {
			jwtProcessor = new DefaultJWTProcessor<>();

			keySource = JWKSourceBuilder
					.create(new URL(keycloakCertsUrl))
					.retrying(true)
					.build();

			JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
			jwtProcessor.setJWSKeySelector(keySelector);

			jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
					new JWTClaimsSet.Builder().issuer(issuer).build(),
					new HashSet<>(Arrays.asList(
							JWTClaimNames.ISSUED_AT,
							JWTClaimNames.EXPIRATION_TIME,
							PREFERRED_USERNAME_CLAIM))));
		} catch (MalformedURLException e) {
			throw new ServletException(String.format("Error while setting up keycloak with URL %s", keycloakCertsUrl), e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = HttpServletRequest.class.cast(request);
		String token = getAuthoriationBearerToken(httpRequest);
		if (StringUtils.isBlank(token)) {
			logger.warn("No Authorization Bearer Token found in request");
		} else {
			try {
				JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

				Map<String, Object> attributes = new HashMap<>();
				attributes.put("firstname", claimsSet.getStringClaim(GIVEN_NAME_CLAIM));
				attributes.put("lastname", claimsSet.getStringClaim(FAMILY_NAME_CLAIM));
				attributes.put("email", claimsSet.getStringClaim(EMAIL_CLAIM));

				Map<String, Object> roles = new HashMap<>();
				Map<String, Set<String>> access = new HashMap<>();

				Map<String, Object> claims = claimsSet.getClaims();
				if (claims.containsKey(RESOURCE_ACCESS_CLAIM)) {
					Map<?, ?> resourceAccess = ObjectTransformer.get(Map.class, claims.get(RESOURCE_ACCESS_CLAIM));
					for (Entry<?, ?> entry : resourceAccess.entrySet()) {
						String key = ObjectTransformer.getString(entry.getKey(), null);
						handleRolesSet(ObjectTransformer.get(Map.class, entry.getValue()), r -> access.put(key, r));
					}
				}
				if (claims.containsKey(REALM_ACCESS_CLAIM)) {
					handleRolesSet(ObjectTransformer.get(Map.class, claims.get(REALM_ACCESS_CLAIM)), r -> roles.put("realm", r));
				}

				roles.put("resource", new MapResolver(access));
				attributes.put("roles", new MapResolver(roles));

				request = doSSOLogin(httpRequest, claimsSet.getStringClaim(PREFERRED_USERNAME_CLAIM), attributes);
			} catch (ParseException | BadJOSEException | JOSEException e) {
				logger.error("Error while parsing Authorization Bearer Token", e);
			}
		}

		chain.doFilter(request, response);
	}

	protected void handleRolesSet(Map<?, ?> propertiesMap, Consumer<Set<String>> rolesConsumer) {
		String[] roles = ObjectTransformer.getStringArray(propertiesMap.get(ROLES_CLAIM), new String[0]);
		if (roles.length > 0) {
			rolesConsumer.accept(new HashSet<>(Arrays.asList(roles)));
		}
	}

	protected String getAuthoriationBearerToken(HttpServletRequest httpRequest) {
		String authHeader = httpRequest.getHeader("Authorization");
		if (StringUtils.isBlank(authHeader)) {
			return null;
		}
		Matcher matcher = AUTHORIZATION_BEARER_PATTERN.matcher(authHeader);
		if (!matcher.matches()) {
			return null;
		} else {
			return matcher.group("token");
		}
	}
}
