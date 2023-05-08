/*
 * @author Dietmar
 * @date 10.03.2005
 * @version $Id: Encryption.java,v 1.1 2005-03-10 21:48:15 dietmar Exp $
 */
package com.gentics.lib.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Dietmar TODO To change the template for this generated type comment
 *         go to Window - Preferences - Java - Code Style - Code Templates
 */
public final class Encryption {

	private static String defaultEncoding = "8859_15";

	private Encryption() {}

	public static String encrypt(String encryption, String message, String encoding) {
		if (message != null) {
			try {
				// encode
				MessageDigest md5 = MessageDigest.getInstance(encryption);

				md5.update(message.getBytes(encoding));
				byte[] digest = md5.digest();

				// create new Message
				message = new String(digest, encoding);
			} catch (UnsupportedEncodingException ex) {} catch (NoSuchAlgorithmException ex) {}
		}
		return message;
	}

	public static String encrypt(String encryption, String message) {
		return encrypt(encryption, message, defaultEncoding);
	}

	public static String md5(String Message) {
		return md5(Message, defaultEncoding);
	}

	/*
	 * The algorithm names in this section can be specified when generating an
	 * instance of MessageDigest. MD2: The MD2 message digest algorithm as
	 * defined in RFC 1319. MD5: The MD5 message digest algorithm as defined in
	 * RFC 1321. SHA-1: The Secure Hash Algorithm, as defined in Secure Hash
	 * Standard, NIST FIPS 180-1. SHA-256, SHA-384, and SHA-512: New hash
	 * algorithms for which the draft Federal Information Processing Standard
	 * 180-2, Secure Hash Standard (SHS) is now available. SHA-256 is a 256-bit
	 * hash function intended to provide 128 bits of security against collision
	 * attacks, while SHA-512 is a 512-bit hash function intended to provide 256
	 * bits of security. A 384-bit hash may be obtained by truncating the
	 * SHA-512 output.
	 */
	public static String md5(String message, String encoding) {
		return encrypt("MD5", message, encoding);
	}
}
