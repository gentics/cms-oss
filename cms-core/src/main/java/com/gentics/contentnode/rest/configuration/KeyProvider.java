package com.gentics.contentnode.rest.configuration;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;

/**
 * Static KeyProvider class.
 * Implements:
 * <ol>
 *   <li>Generation and storing of key-pair</li>
 *   <li>Providing public key as JWK</li>
 *   <li>Singing a JWT with the private key</li>
 * </ol>
 */
public final class KeyProvider {
	/**
	 * Logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(KeyProvider.class);

	/**
	 * Default name of the Keyfile
	 */
	public final static String DEFAULT_KEYFILE_NAME = "private-key.jwk";

	/**
	 * Name of the system property for overriding the default keyfile path
	 */
	public final static String KEYFILE_PARAM_NAME = "com.gentics.contentnode.private-key.path";

	/**
	 * Key-pair instance
	 */
	private static RSAKey instance;

	/**
	 * Initialize the key-pair. This will either load the key-pair from the file (if it exists) or create a new key and store it in the key file.
	 * @param configPath configuration path
	 * @throws NodeException
	 */
	public final static void init(String configPath) throws NodeException {
		File keyFile = new File(System.getProperty(KEYFILE_PARAM_NAME, String.format("%s%s", configPath, DEFAULT_KEYFILE_NAME)));

		try {
			if (keyFile.exists()) {
				// keyfile exists, so read it
				instance = (RSAKey) JWK.parse(FileUtils.readFileToString(keyFile));
			} else {
				// create new keypair
				KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

				// create JWK containing private and public key
				instance = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate()).keyUse(KeyUse.SIGNATURE)
						.keyID(UUID.randomUUID().toString()).build();

				FileUtils.writeStringToFile(keyFile, instance.toJSONString());
			}
		} catch (IOException | ParseException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Sign the JWT, which is currently generated with the builder
	 * @param jwt jwt builder
	 * @return jwt builder after signing the jwt
	 * @throws NodeException
	 */
	public final static JwtBuilder sign(JwtBuilder jwt) throws NodeException {
		try {
			return jwt.signWith(instance.toPrivateKey());
		} catch (InvalidKeyException | JOSEException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Set the signing key
	 * @param jwt parser builder
	 * @return parser builder with signing key set
	 * @throws NodeException
	 */
	public final static JwtParserBuilder signedWith(JwtParserBuilder jwt) throws NodeException {
		try {
			return jwt.setSigningKey(instance.toPublicKey());
		} catch (JOSEException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Get the public key as JWK
	 * @return JWK
	 * @throws NodeException
	 */
	public final static JsonNode getPublicKey() throws NodeException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(instance.toPublicJWK().toJSONString(), JsonNode.class);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}
}
