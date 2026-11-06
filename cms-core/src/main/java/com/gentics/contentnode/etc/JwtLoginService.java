package com.gentics.contentnode.etc;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.log.NodeLogger;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login service that accepts a JWT as password. The username <em>must</em> be {@code support}, the actual user
 * information is taken from the JWT.
 *
 * <p>
 *     If the user defined in the JWT does not yet exist it will be created and put into the node admin group.
 * </p>
 */
public class JwtLoginService extends AbstractLoginService {

	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_GIVEN_NAME = "given_name";
	private static final String CLAIM_FAMILY_NAME = "family_name";
	private static final String JWT_ISSUER = "Gentics";
	private static final String PATH_PUBLIC_KEY = "keys/support-login-public-key.jwk";
	private static final int GROUP_ID = 2;

	private static final NodeLogger logger = NodeLogger.getNodeLogger(JwtLoginService.class);

	private static ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

	@Override
	public LoginResponse login(String username, String password, ContentNodeFactory factory, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		if (!"support".equals(username)) {
			return getInvalidCredentialsResponse();
		}

		String email;
		String firstName;
		String lastName;

		try {
			var claims = getJwtProcessor().process(password, null);

			email = claims.getStringClaim(CLAIM_EMAIL);
			firstName = claims.getStringClaim(CLAIM_GIVEN_NAME);
			lastName = claims.getStringClaim(CLAIM_FAMILY_NAME);
		} catch (Exception e) {
			logger.error("Could not parse JWT: %s".formatted(e.getMessage()), e);

			return getInvalidCredentialsResponse();
		}

		SystemUser systemUser;

		try {
			var t = TransactionManager.getCurrentTransaction();

			systemUser = ((SystemUserFactory) t.getObjectFactory(SystemUser.class)).getSystemUser(email, null, false);

			if (systemUser == null) {
				systemUser = t.createObject(SystemUser.class);

				systemUser.setLogin(email);
				systemUser.setPassword("JWK");
				systemUser.setSupportUser(true);
				systemUser.setEmail(email);
				systemUser.setFirstname(firstName);
				systemUser.setLastname(lastName);
				systemUser.setActive(true);
				systemUser.getUserGroups().add(t.getObject(UserGroup.class, GROUP_ID));
				systemUser.save();

				t.commit(false);
			}
		} catch (NodeException e) {
			logger.error("Could not get or create support user: %s".formatted(e.getMessage()), e);

			return getInvalidCredentialsResponse();
		}

		var response = new LoginResponse();

		try {
			systemUser = performLogin(systemUser.getLogin(), null, response, false, factory);

			createUserSession(systemUser, servletRequest, servletResponse);
			response.setUser(ModelBuilder.getUser(systemUser));
		} catch (NodeException e) {
			logger.error("Could not create user session for \"%s\": %s".formatted(systemUser.getLogin(), e.getMessage()), e);

			return getInvalidCredentialsResponse();
		}

		response.setResponseInfo(ResponseInfo.ok("Support login successful"));

		return response;
	}

	/**
	 * Create a {@link LoginResponse} with a message that the credentials are invalid.
	 *
	 * @return A {@code LoginResponse} with a message that the credentials are invalid.
	 */
	private LoginResponse getInvalidCredentialsResponse() {
		var response = new LoginResponse();

		response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE,  "Invalid credentials"));
		response.setMessages(Collections.singletonList(new Message(Message.Type.CRITICAL, I18NHelper.get("login.failed"))));

		return response;
	}


	private synchronized static JWTProcessor<SecurityContext> getJwtProcessor() throws MalformedURLException {
		if (jwtProcessor == null) {
			jwtProcessor = new DefaultJWTProcessor<>();

			var keySource = JWKSourceBuilder.create(new File(PATH_PUBLIC_KEY).toURI().toURL()).build();
			var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
			var claimsVerifier = new DefaultJWTClaimsVerifier<>(
				new JWTClaimsSet.Builder().issuer(JWT_ISSUER).build(),
				new HashSet<>(Arrays.asList(
					JWTClaimNames.ISSUED_AT,
					JWTClaimNames.EXPIRATION_TIME,
					CLAIM_GIVEN_NAME,
					CLAIM_FAMILY_NAME,
					CLAIM_EMAIL)));

			jwtProcessor.setJWSKeySelector(keySelector);
			jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);
		}

		return jwtProcessor;
	}
}
