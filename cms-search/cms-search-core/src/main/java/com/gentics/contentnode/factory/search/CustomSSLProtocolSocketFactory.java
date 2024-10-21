package com.gentics.contentnode.factory.search;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * Custom SSL protocol socket factory using a specified certificate and certificate authority.
 */
public class CustomSSLProtocolSocketFactory implements ProtocolSocketFactory {

	private static SSLContext sslContext;

	private final String certPath;
	private final String caPath;

	/**
	 * Construct new instance using the given certificate and certificate authority.
	 *
	 * @param certPath Path to the PEM file containing the certificate.
	 * @param caPath Path to the PEM file containing the certificate authority.
	 */
	public CustomSSLProtocolSocketFactory(String certPath, String caPath) {
		this.certPath = certPath;
		this.caPath = caPath;
	}

	/**
	 * Create a SSL context using the given certificate and certificate authority.
	 *
	 * <p>
	 *     Both the certificate and the CA have to be in PEM format.
	 * </p>
	 *
	 * @param certPath Path to the PEM file containing the certificate.
	 * @param caPath Path to the PEM file containing the certificate authority.
	 * @return A SSL context using the given certificate.
	 */
	private static SSLContext createSSLContext(String certPath, String caPath) throws NoSuchAlgorithmException, KeyManagementException {
		X509TrustManager trustManager = createTrustManager(certPath, caPath);
		TrustManager[] trustManagers = new TrustManager[] { trustManager };
		SSLContext sslContext = SSLContext.getInstance("SSL");

		sslContext.init(null, trustManagers, new SecureRandom());

		return sslContext;
	}

	/**
	 * Return the prepared SSL context using the configured certificate or create a new one.
	 *
	 * @return The SSL context using the configured certificate.
	 */
	private SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		if (sslContext == null) {
			sslContext = createSSLContext(certPath, caPath);
		}

		return sslContext;
	}

	/**
	 * Create a new trust manager which just contains/trusts the given certificate chain.
	 *
	 * @param certPath Path to the certificate PEM file.
	 * @param caCertPath Path to the CA certificate PEM file.
	 * @return A trust manager only trusting the given certificate.
	 */
	public static X509TrustManager createTrustManager(String certPath, String caCertPath) {
		try {
			String chain = readFile(certPath, "cert") + readFile(caCertPath, "ca");
			InputStream certChain = new ByteArrayInputStream(chain.getBytes());

			return createManager(certChain);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Error while creating custom trust manager", e);
		}
	}

	/**
	 * Create a trust manager using the given certificate chain.
	 * @param certChainStream InputStream with the certificate chain.
	 * @return A trust manager using the given certificate chain.
	 * @throws GeneralSecurityException
	 */
	private static X509TrustManager createManager(InputStream certChainStream) throws GeneralSecurityException {
		// Password is just needed to create a new keystore. The keystore is only used during runtime.
		String randomPassword = Long.toString(new Random().nextLong() & Long.MAX_VALUE, 36);
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(certChainStream);

		if (certificates.isEmpty()) {
			throw new IllegalArgumentException("expected non-empty set of trusted certificates");
		}

		KeyStore keyStore = createEmptyKeyStore(randomPassword);
		int index = 0;

		for (Certificate certificate : certificates) {
			String certificateAlias = Integer.toString(index++);

			keyStore.setCertificateEntry(certificateAlias, certificate);
		}

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		trustManagerFactory.init(keyStore);

		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
		}

		return (X509TrustManager) trustManagers[0];
	}

	/**
	 * Read the given file into a {@code String}.
	 *
	 * <p>
	 *     The {@code name} is only used for more detailed error messages.
	 * </p>
	 *
	 * @param path Path to the file to read.
	 * @param name Name to use in error messages.
	 * @return The contents of the specified file as {@code String}.
	 */
	private static String readFile(String path, String name) {
		try {
			return new String(Files.readAllBytes(Paths.get(path)));
		} catch (IOException e) {
			throw new RuntimeException("Could not load " + name + " from file {" + path + "}", e);
		}
	}


	/**
	 * Create an empty keystore using the given password and the default keystore type.
	 *
	 * @param password The password for the keystore.
	 * @return An empty keystore
	 */
	public static KeyStore createEmptyKeyStore(String password) throws GeneralSecurityException {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			// By convention, 'null' creates an empty key store.
			keyStore.load(null, password.toCharArray());

			return keyStore;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port, localAddress, localPort);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}

		int timeout = params.getConnectionTimeout();
		SocketFactory socketFactory;

		try {
			socketFactory = getSSLContext().getSocketFactory();
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}

		if (timeout == 0) {
			return socketFactory.createSocket(host, port, localAddress, localPort);
		}

		Socket socket = socketFactory.createSocket();
		SocketAddress localAddr = new InetSocketAddress(localAddress, localPort);
		SocketAddress remoteAddr = new InetSocketAddress(host, port);

		socket.bind(localAddr);
		socket.connect(remoteAddr, timeout);

		return socket;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
}
