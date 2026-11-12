package com.gentics.contentnode.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.lib.log.NodeLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Tokens for the link, with which a user sets their password. The token is a JWT signed with the CMS key, which carries
 * its expiry in {@code exp}. Only its {@code jti} is stored, in {@code systemuser.password_token}, so that the link can
 * be used once, a new link replaces the old one, and setting the password otherwise revokes the link.
 */
public final class PasswordTokens {
	/**
	 * How long a link is valid
	 */
	public static final Duration VALIDITY = Duration.ofHours(24);

	/**
	 * Claim naming the purpose of the token. Required, because the CMS signs the JWTs for custom proxies with the same
	 * key, and those must not be accepted here.
	 */
	static final String CLAIM_PURPOSE = "purpose";

	/**
	 * Value of {@link #CLAIM_PURPOSE}
	 */
	static final String PURPOSE = "set-password";

	/**
	 * Rule for a password set with a token, the same as the password validators of the admin and editor UI
	 */
	static final Pattern PASSWORD = Pattern.compile("^[\\w\\-.@]{4,20}$");

	private static final NodeLogger logger = NodeLogger.getNodeLogger(PasswordTokens.class);

	private PasswordTokens() {
	}

	/**
	 * Create a token for the user and store its ID, which revokes any earlier token of the user
	 * @param userId user ID
	 * @return signed token
	 * @throws NodeException
	 */
	public static String create(int userId) throws NodeException {
		String jti = UUID.randomUUID().toString();
		DBUtils.update("UPDATE systemuser SET password_token = ? WHERE id = ?", jti, userId);

		Date expires = new Date(System.currentTimeMillis() + VALIDITY.toMillis());
		return KeyProvider.sign(Jwts.builder().setSubject(Integer.toString(userId)).setId(jti).setExpiration(expires)
				.claim(CLAIM_PURPOSE, PURPOSE)).compact();
	}

	/**
	 * Verify the token. The signature is checked first, because only then are its claims trustworthy, then its expiry,
	 * then whether it is still the stored token of an active user. The reason for a rejection is logged, but not
	 * returned, so that callers cannot probe tokens.
	 * @param token token
	 * @return the user, empty if the token is not valid
	 * @throws NodeException
	 */
	public static Optional<SystemUser> verify(String token) throws NodeException {
		if (StringUtils.isBlank(token)) {
			return reject("no token given");
		}

		Claims claims;
		try {
			claims = KeyProvider.signedWith(Jwts.parserBuilder()).build().parseClaimsJws(token).getBody();
		} catch (ExpiredJwtException e) {
			return reject("token expired");
		} catch (JwtException | IllegalArgumentException e) {
			return reject("token invalid (%s)".formatted(e.getClass().getSimpleName()));
		}

		try {
			if (!PURPOSE.equals(claims.get(CLAIM_PURPOSE, String.class))) {
				return reject("token has a different purpose");
			}
		} catch (JwtException e) {
			return reject("token has a different purpose");
		}

		// the parser checks exp only when it is present
		if (claims.getExpiration() == null) {
			return reject("token has no expiry");
		}

		int userId;
		try {
			userId = Integer.parseInt(claims.getSubject());
		} catch (NumberFormatException e) {
			return reject("token has no user ID");
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.getObject(SystemUser.class, userId);
		if (user == null || user.getActive() != 1) {
			return reject("user %d does not exist or is inactive".formatted(userId));
		}

		String storedJti = DBUtils.select("SELECT password_token FROM systemuser WHERE id = ?", st -> st.setInt(1, userId),
				rs -> rs.next() ? rs.getString("password_token") : null);
		String jti = claims.getId();
		if (storedJti == null || jti == null || !MessageDigest.isEqual(storedJti.getBytes(StandardCharsets.UTF_8),
				jti.getBytes(StandardCharsets.UTF_8))) {
			return reject("token of user %d was used, replaced or revoked".formatted(userId));
		}

		return Optional.of(user);
	}

	/**
	 * Revoke the token of the user
	 * @param userId user ID
	 * @throws NodeException
	 */
	public static void clear(int userId) throws NodeException {
		DBUtils.update("UPDATE systemuser SET password_token = NULL WHERE id = ?", userId);
	}

	/**
	 * Check whether the password satisfies the rule for passwords set with a token
	 * @param password password
	 * @return true if it does
	 */
	public static boolean isValidPassword(String password) {
		return password != null && PASSWORD.matcher(password).matches();
	}

	/**
	 * Log the reason for rejecting a token
	 * @param reason reason, must not contain the token
	 * @return empty
	 */
	private static Optional<SystemUser> reject(String reason) {
		logger.info("Password token rejected: " + reason);
		return Optional.empty();
	}
}
