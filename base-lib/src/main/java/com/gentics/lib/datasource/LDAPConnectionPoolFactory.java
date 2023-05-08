package com.gentics.lib.datasource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.pooling.PoolFactoryInterface;
import com.gentics.lib.pooling.Poolable;
import com.gentics.lib.pooling.PoolingException;
import com.gentics.lib.pooling.SimpleEmbeddedPoolObject;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSocketFactory;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 11.09.2004
 */
public class LDAPConnectionPoolFactory implements PoolFactoryInterface, LDAPSocketFactory {
	int ldapPort = LDAPConnection.DEFAULT_PORT;

	int searchScope = LDAPConnection.SCOPE_ONE;

	int ldapVersion = LDAPConnection.LDAP_V3;

	String ldapHost = "";

	String bindDN = "";

	String password = "";

	int timeout;

	int socketTimeout;

	public LDAPConnectionPoolFactory(String ldapHost, int ldapPort, String bindDN,
			String password, int timeout, int socketTimeout) {
		this.ldapPort = ldapPort;
		this.ldapHost = ldapHost;
		this.bindDN = bindDN;
		this.password = password;
		this.timeout = timeout;
		this.socketTimeout = socketTimeout;
	}

	public String getBindDN() {
		return bindDN;
	}

	private LDAPConnection connect() throws Exception {
		LDAPConnection conn = new LDAPConnection(this);
		LDAPConstraints constr = new LDAPConstraints();

		constr.setTimeLimit(this.timeout);
		conn.setConstraints(constr);
		conn.connect(ldapHost, ldapPort);
		// bind to the server
		conn.bind(ldapVersion, bindDN, password.getBytes("UTF8"));
		return conn;
	}

	public void destroyObject(Poolable object) throws PoolingException {
		LDAPConnection conn = (LDAPConnection) object.getObject();

		try {
			if (conn != null) {
				conn.disconnect();
			}
		} catch (LDAPException e) {
			e.printStackTrace();
		}
	}

	public Poolable createObject() throws PoolingException {
		// connect to the server
		LDAPConnection conn;

		try {
			conn = connect();
		} catch (Exception e) {
			throw new PoolingException("Error while creating ldap handle", e);
		}

		if (conn == null) {
			return new SimpleEmbeddedPoolObject(null);
		} else {
			return new SimpleEmbeddedPoolObject(conn);

		}
	}

	public void reinitObject(Poolable object) throws PoolingException {
		// TODO: test connection, and re-init if necessary
		SimpleEmbeddedPoolObject poolObject = (SimpleEmbeddedPoolObject) object;
		LDAPConnection conn = (LDAPConnection) object.getObject();

		if (!conn.isConnected() || !conn.isConnectionAlive()) {
			try {
				conn = connect();
			} catch (Exception e) {
				throw new PoolingException("Error while Re-Initialization of ldap handle", e);
			}
			poolObject.setObject(conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.novell.ldap.LDAPSocketFactory#createSocket(java.lang.String, int)
	 */
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		Socket socket = new Socket();

		socket.connect(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port), socketTimeout);
		socket.setSoTimeout(socketTimeout);
		return socket;
	}
}
