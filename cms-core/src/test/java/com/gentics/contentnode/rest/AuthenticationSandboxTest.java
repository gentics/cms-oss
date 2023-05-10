package com.gentics.contentnode.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gentics.contentnode.rest.resource.impl.RsaAuthenticationResourceImpl;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.init.BcryptPasswords;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.HashPasswordRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.LoginWithRsaRequest;
import com.gentics.contentnode.rest.model.response.HashPasswordResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.AuthenticationResource;
import com.gentics.contentnode.rest.resource.impl.AuthenticationResourceImpl;
import com.gentics.contentnode.security.RsaTokenValidator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.etc.StringUtils;

/**
 * Tests saving of a page into GCN by using the REST API.
 *
 * @author Bernhard Kaszt
 */
public class AuthenticationSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * A test user for testing
	 */
	private SystemUser systemUser = null;

	/**
	 * A test password
	 */
	public String TESTPASSWORD = "monkey";

	/**
	 * Path to the private key used to test the RSA login
	 */
	final static public String RSALOGIN_KEY_PRIVATE =
			"/com/gentics/testutils/resources/rsalogin/private_key.der";

	/**
	 * Path to the public key used to test the RSA login
	 */
	final static public String RSALOGIN_KEY_PUBLIC =
			"/com/gentics/testutils/resources/rsalogin/public_key.der";

	/**
	 * User test Group ID
	 * Test user has to be in some group
	 */
	public int groupId = 5;

	/**
	 * Initializes the client and the WebResource
	 */
	@Before
	public void setUp() throws Exception {
		// Make sure all stored passwords are "bcrypted"
		BcryptPasswords bcryptPasswords = new BcryptPasswords();
		bcryptPasswords.execute();

		testContext.getContext().startTransaction();

		systemUser = createDummyUser(new BigInteger(130, new SecureRandom()).toString(32), TESTPASSWORD);
	}

	/**
	 * Test a normal login with valid username and password
	 *
	 * @throws Exception
	 */
	@Test
	public void testLogin() throws Exception {
		setSystemUserPasswordAndTestLogin(
				SystemUserFactory.hashPassword(TESTPASSWORD, 0), TESTPASSWORD, ResponseCode.OK);
	}

	/**
	 * Test login by passing an invalid password
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithInvalidPassword() throws Exception {
		setSystemUserPasswordAndTestLogin(
				SystemUserFactory.hashPassword(TESTPASSWORD, 0),
				"obviouslyWrongPassword", ResponseCode.NOTFOUND);
	}

	/**
	 * Test login with a bcrypt(md5()) encoded password
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithLegacyPassword() throws Exception {
		String md5HashedPassword = StringUtils.md5(systemUser.getId() + TESTPASSWORD).toLowerCase();
		String legacyEncodedPassword =
				"leg-" + SystemUserFactory.hashPassword(md5HashedPassword, 0);
		systemUser.setPassword(legacyEncodedPassword);
		systemUser.save();
		TransactionManager.getCurrentTransaction().commit(false);

		testLogin(systemUser.getLogin(), TESTPASSWORD, ResponseCode.OK, "");
	}

	/**
	 * Test login with an old md5 password (should fail)
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithOldMd5Password() throws Exception {
		systemUser.setPassword(StringUtils.md5(systemUser.getId() + TESTPASSWORD).toLowerCase());
		systemUser.save();
		TransactionManager.getCurrentTransaction().commit(false);

		testLogin(systemUser.getLogin(), TESTPASSWORD, ResponseCode.NOTFOUND, "");
	}

	/**
	 * Test login using a special RSA token int the form
	 * "rsa-rsahexsignature|salt|expirationtimestamp"
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithRsaToken() throws Exception {
		AuthenticationResourceImpl authenticationResource = getAuthenticationResource();
		RsaAuthenticationResourceImpl rsaAuthenticationResource  = new RsaAuthenticationResourceImpl();
		rsaAuthenticationResource.initialize();
		RsaTokenValidator rsaTokenValidator = rsaAuthenticationResource.getRsaTokenValidator();

		// Load the private key from generic test utils
		InputStream privateKeyStream = AuthenticationSandboxTest.class.getResourceAsStream(RSALOGIN_KEY_PRIVATE);
		if (privateKeyStream == null) {
			throw new FileNotFoundException("Private key {" + RSALOGIN_KEY_PRIVATE + "} not found within classpath.");
		}
		PrivateKey privateKey = rsaTokenValidator.loadPrivateKey(privateKeyStream);

		// Load the public key from generic test utils
		InputStream inputStream = AuthenticationSandboxTest.class.getResourceAsStream(RSALOGIN_KEY_PUBLIC);
		if (inputStream == null) {
			throw new FileNotFoundException("Public key {" + RSALOGIN_KEY_PUBLIC + "} not found within classpath.");
		}
		rsaTokenValidator.setPublicKey(rsaTokenValidator.loadPublicKey(inputStream));

		String salt = new BigInteger(130, new SecureRandom()).toString(32);
		int currentTimestamp = (int)(System.currentTimeMillis() / 1000L);
		int expirationTimestamp = currentTimestamp + 3600; // 1 hour

		// Create a new request and fill the data
		LoginWithRsaRequest request = new LoginWithRsaRequest();
		request.setUsername("system");
		request.setSalt(salt);
		request.setExpirationTimestamp(expirationTimestamp);
		request.setRsaSignature(
				RsaTokenValidator.generateSignature(privateKey, salt, expirationTimestamp));

		// Check if the login with a valid signature returns OK
		LoginResponse response = rsaAuthenticationResource.loginWithRsa(request, "0");

		assertEquals("Login response code has to be OK", ResponseCode.OK,
				response.getResponseInfo().getResponseCode());

		// Check if an invalid signature returns NOTFOUND
		request.setRsaSignature(
				RsaTokenValidator.generateSignature(privateKey, "nonesense", expirationTimestamp));
		response = rsaAuthenticationResource.loginWithRsa(request, "0");

		assertEquals("Login response code has to be NOTFOUND", ResponseCode.NOTFOUND,
				response.getResponseInfo().getResponseCode());

		// The login with an expirationTimestamp of 1 year in the future must fail
		expirationTimestamp = currentTimestamp + 31536000;
		request.setExpirationTimestamp(expirationTimestamp);
		request.setRsaSignature(
				RsaTokenValidator.generateSignature(privateKey, salt, expirationTimestamp));
		response = rsaAuthenticationResource.loginWithRsa(request, "0");

		assertEquals("Login with invalid timestamp response code has to be NOTFOUND",
				ResponseCode.NOTFOUND, response.getResponseInfo().getResponseCode());

		// The login with an expired timestamp must fail
		expirationTimestamp = currentTimestamp - 1000;
		request.setExpirationTimestamp(expirationTimestamp);
		request.setRsaSignature(
				RsaTokenValidator.generateSignature(privateKey, salt, expirationTimestamp));
		response = rsaAuthenticationResource.loginWithRsa(request, "0");

		assertEquals("Login with expired timestamp response code has to be NOTFOUND",
				ResponseCode.NOTFOUND, response.getResponseInfo().getResponseCode());

		// Login with /auth/login, using a "rsa-"-token-password
		expirationTimestamp = currentTimestamp + 3600; // 1 hour
		String rsaTokenPassword = "rsa-"
				+ RsaTokenValidator.generateSignature(privateKey, salt, expirationTimestamp)
				+ "|" + salt
				+ "|" + expirationTimestamp;

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setLogin("system");
		loginRequest.setPassword(rsaTokenPassword);
		response = authenticationResource.login(loginRequest, "0");

		assertEquals("Login with RSA token password has to be OK",
				ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test login with a null password
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithNullPassword() throws Exception {
		setSystemUserPasswordAndTestLogin(
				SystemUserFactory.hashPassword(TESTPASSWORD, 0), null, ResponseCode.NOTFOUND);
	}

	/**
	 * Test login with an empty password
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoginWithEmptyPassword() throws Exception {
		setSystemUserPasswordAndTestLogin(
				SystemUserFactory.hashPassword(TESTPASSWORD, 0), "", ResponseCode.NOTFOUND);
	}

	/**
	 * Hashes a password with BCrypt and checks the resulting hash
	 *
	 * @throws Exception
	 */
	@Test
	public void testHashPassword() throws Exception {
		AuthenticationResource authenticationResource = getAuthenticationResource();

		HashPasswordRequest hashPasswordRequest = new HashPasswordRequest();
		hashPasswordRequest.setPassword(TESTPASSWORD);
		HashPasswordResponse response =
				authenticationResource.hashPassword(null, hashPasswordRequest, 0);

		assertEquals("Response code has to be OK", ResponseCode.OK,
				response.getResponseInfo().getResponseCode());

		String hash = response.getHash();
		assertTrue("Hash has to start with $", hash.startsWith("$"));
		assertTrue("Hash has to be exactly 60 characters long", hash.length() == 60);
	}

	/*
	 * Creates a new user
	 *
	 * @param userName          User name
	 * @param passwordHashToset Raw password hash
	 * @return SystemUser objects
	 */
	protected SystemUser createDummyUser(String userName, String passwordHashToset) throws Exception {
		Transaction transaction = TransactionManager.getCurrentTransaction();
		SystemUser systemUser = transaction.createObject(SystemUser.class);

		systemUser.setLogin(userName);
		systemUser.setFirstname("Max");
		systemUser.setLastname("Mustermann");
		systemUser.setEmail("test@localhost");
		systemUser.setActive(true);
		systemUser.setLastname("Mustermann");
		systemUser.setPassword(passwordHashToset);

		// Add the user to the node group
		List<UserGroup> userGroups = systemUser.getUserGroups();
		userGroups.add(transaction.getObject(UserGroup.class, groupId));

		systemUser.save();
		transaction.commit(false);

		return systemUser;
	}

	/*
	 * Sets the password for the class-global user instance and
	 * does a login
	 *
	 * @param passwordHashToset   Hash to set
	 * @param actualPassword      Plain text
	 * @param responseCode        Response Code (ie. ResponseCode.OK)
	 */
	protected void setSystemUserPasswordAndTestLogin(
			String passwordHashToset, String actualPassword, ResponseCode responseCode)
					throws Exception  {
		systemUser.setPassword(passwordHashToset);
		systemUser.save();
		TransactionManager.getCurrentTransaction().commit(false);

		String sessionId = testLogin(systemUser.getLogin(), actualPassword, responseCode, "");

		if (sessionId.length() > 15) {
			// Woops, this also contains the secret, let's strip it!
			sessionId = sessionId.substring(0, sessionId.length() - 15);
		}

		// Login again with the existing session ID, just to see if that works too
		testLogin(systemUser.getLogin(), actualPassword, responseCode, sessionId);
	}

	/*
	 * Sends a login request and returns the SID
	 *
	 * @param userName      Login user name
	 * @param password      Plain text password
	 * @param responseCode  Response Code (ie. ResponseCode.OK)
	 * @param sessionId     If to re-use an existing sessionId
	 */
	protected String testLogin(String userName, String password, ResponseCode responseCode, String sessionId)
			throws TransactionException {
		AuthenticationResource authenticationResource = getAuthenticationResource();

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setLogin(userName);
		loginRequest.setPassword(password);
		LoginResponse response = authenticationResource.login(loginRequest, sessionId);

		assertEquals("Login response code has to match expected value", responseCode,
				response.getResponseInfo().getResponseCode());

		if (responseCode != ResponseCode.OK) {
			return "0";
		}

		return response.getSid();
	}

	/**
	 * Get an authentication resource
	 *
	 * @return RsaAuthenticationResourceImpl
	 * @throws TransactionException
	 */
	protected AuthenticationResourceImpl getAuthenticationResource() throws TransactionException {
		AuthenticationResourceImpl authenticationResoure = new AuthenticationResourceImpl();
		authenticationResoure.setTransaction(TransactionManager.getCurrentTransaction());
		authenticationResoure.initialize();

		return authenticationResoure;
	}
}
