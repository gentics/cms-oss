package com.gentics.contentnode.etc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class JwtLoginService extends AbstractLoginService {

	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_GIVEN_NAME = "given_name";
	private static final String CLAIM_FAMILY_NAME = "family_name";
	private static final String JWT_ISSUER = "Gentics";
	private static final String PATH_PUBLIC_KEY = "/jwt/public-key.jwk";
	private static final int GROUP_ID = 2;

	private static final NodeLogger logger = NodeLogger.getNodeLogger(JwtLoginService.class);

	private static PublicKey publicKey = null;

	@Override
	public LoginResponse login(String username, String password, ContentNodeFactory factory, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		if (!"support".equals(username)) {
			return getInvalidCredentialsResponse();
		}

		String email;
		String firstName;
		String lastName;

		try {
			var jwtParser = Jwts.parserBuilder()
					.requireIssuer(JWT_ISSUER)
					.setSigningKey(getPublicKey())
					.build();
			var claims = jwtParser.parseClaimsJws(password).getBody();

			for (var requiredClaim: List.of(CLAIM_EMAIL, CLAIM_GIVEN_NAME, CLAIM_FAMILY_NAME)) {
				if (!claims.containsKey(requiredClaim) || StringUtils.isBlank(claims.get(requiredClaim).toString())) {
					logger.error("Missing claim in support JWT: %s".formatted(requiredClaim));

					return getInvalidCredentialsResponse();
				}
			}

			email = claims.get(CLAIM_EMAIL).toString();
			firstName = claims.get(CLAIM_GIVEN_NAME).toString();
			lastName = claims.get(CLAIM_FAMILY_NAME).toString();
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

			response.setSid(createUserSession(systemUser, servletRequest, servletResponse));
			response.setUser(ModelBuilder.getUser(systemUser));
		} catch (NodeException e) {
			logger.error("Could not create user session for \"%s\": %s".formatted(systemUser.getLogin(), e.getMessage()), e);

			return getInvalidCredentialsResponse();
		}

		response.setResponseInfo(ResponseInfo.ok("Support login successful"));

		return response;
	}

	private LoginResponse getInvalidCredentialsResponse() {
		var response = new LoginResponse();

		response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE,  "Invalid credentials"));
		response.setMessages(Collections.singletonList(new Message(Message.Type.CRITICAL, new CNI18nString("login.failed").toString())));

		return response;
	}


	private static PublicKey getPublicKey() {
		if (publicKey == null) {
			try (InputStream publicKeyInputStream = JwtLoginService.class.getResourceAsStream(PATH_PUBLIC_KEY)) {
				var jwk = (RSAKey) JWK.parse(IOUtils.toString(publicKeyInputStream, StandardCharsets.UTF_8));

				publicKey = jwk.toPublicKey();
			} catch (NullPointerException e) {
				logger.error("Could not find public key \"%s\" in resources".formatted(PATH_PUBLIC_KEY));
			} catch (IOException | ParseException | JOSEException e) {
				logger.error("Could not load public key \"%s\" from resources: %s".formatted(PATH_PUBLIC_KEY, e.getMessage()), e);
			}
		}

		return publicKey;
	}
}
