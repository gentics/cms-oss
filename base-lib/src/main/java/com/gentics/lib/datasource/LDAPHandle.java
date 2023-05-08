package com.gentics.lib.datasource;

import java.util.Map;

import com.gentics.api.lib.datasource.DatasourceDefinition;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.pooling.NotPoolObjectException;
import com.gentics.lib.pooling.PoolEmptyException;
import com.gentics.lib.pooling.PoolInterface;
import com.gentics.lib.pooling.PoolWrapper;
import com.gentics.lib.pooling.Poolable;
import com.gentics.lib.pooling.PoolingException;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

/**
 * @author haymo Search Scope The search scope parameter specifies the depth of
 *         the search and can be one of three values: + SCOPE_BASE. Only the
 *         entry specified as the search base is include in the search. This is
 *         used when you already know the DN of the object and you would like to
 *         read its attributes. (The read method may also be used to read the
 *         values of a single entry). + SCOPE_ONE. Objects one level below the
 *         base (but not including the base) are included in the search. If we
 *         specified o=acme as our search base, then entries in the o=acme
 *         container would be included, but not the object o=acme. + SCOPE_SUB.
 *         All objects below the base, including the base itself, are included
 *         in the search. +
 */
public class LDAPHandle extends AbstractDatasourceHandle {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	int poolSize = 20;

	PoolInterface pool = null;

	LDAPConnectionPoolFactory connectionFactory;

	private int searchScope = -1;

	/**
	 * Create an instance with given id
	 * @param id handle id
	 */
	public LDAPHandle(String id) {
		super(id);
	}

	public void close() {
		this.pool.removeAll();
	}

	public String getBindDN() {
		return connectionFactory.getBindDN();
	}
    
	/**
	 * Returns the search scope defined for this LDAPHandle
	 * @return search scope (-1 if not defined)
	 */
	public int getSearchScope() {
		return searchScope;
	}

	public final Poolable connect() {
		if (this.pool == null) {
			return null;
		}
		try {
			lastException = null;
			// calls LDAPConnectionPoolFactory.getInstance()
			return pool.getInstance();
		} catch (PoolEmptyException e) {
			lastException = e;
			logger.error("LDAPHandle-ERROR: Could not get LDAP-connect", e);
		} catch (PoolingException e) {
			lastException = e;
			logger.error("LDAPHandle-ERROR: Could not get LDAP-connect", e);
		} catch (IllegalAccessException e) {
			lastException = e;
			logger.error("LDAPHandle-ERROR: Could not get LDAP-connect", e);
		}
		return null;
	}

	public final void disconnect(Poolable connection) throws LDAPException {
		try {
			this.pool.releaseInstance(connection);
		} catch (NotPoolObjectException e) {
			logger.warn("trying to release LDAP-Connection which is not in pool.");
		}
	}

	public DatasourceDefinition getDatasourceDefinition() {
		return super.getDatasourceDefinition();
	}

	public void init(Map parameters) {
		String ldapHost = "localhost", bindDN = "", password = "";
		int ldapPort = 389, timeout = 0;

		if (parameters.containsKey("host")) {
			ldapHost = (String) parameters.get("host");
		}
		if (parameters.containsKey("binddn")) {
			bindDN = (String) parameters.get("binddn");
		}
		if (parameters.containsKey("password")) {
			password = (String) parameters.get("password");
		}
		if (parameters.containsKey("port")) {
			ldapPort = ObjectTransformer.getInt(parameters.get("port"), 389);
		}
		if (parameters.containsKey("poolsize")) {
			this.poolSize = ObjectTransformer.getInt(parameters.get("poolsize"), 20);
		}
		if (parameters.containsKey("scope")) {
			searchScope = LDAPDatasource.parseScope(parameters.get("scope").toString(), LDAPConnection.SCOPE_ONE);
		}
		timeout = ObjectTransformer.getInt(parameters.get("timeout"), 5000);
		int socketTimeout = ObjectTransformer.getInt(parameters.get("sockettimeout"), 5000);

		// try {
		this.connectionFactory = new LDAPConnectionPoolFactory(ldapHost, ldapPort, bindDN, password, timeout, socketTimeout);
		// this.pool = new SimpleObjectPool(3, this.poolSize, this.connectionFactory, true);
		this.pool = new PoolWrapper("LDAPHandlePool", 3, this.poolSize, this.connectionFactory, true);
		if (logger.isDebugEnabled()) {
			logger.debug(
					"LDAP Connection established to host[" + ldapHost + "] port[" + ldapPort + "] bindDN[" + bindDN + "] <----------------------------------------");
		}
		// } catch (InstantiationException e) {
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// e.printStackTrace();
		// }
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.DatasourceHandle#isAlive()
	 */
	public boolean isAlive() {
		Poolable mycon = null;
		LDAPConnection con = null;

		try {
			lastException = null;
			mycon = pool.getInstance();
			if (mycon != null) {
				con = (LDAPConnection) mycon.getObject();
				return con.isConnected() && con.isConnectionAlive();
			} else {
				return false;
			}
		} catch (Exception e) {
			lastException = e;
			return false;
		} finally {
			if (mycon != null) {
				try {
					disconnect(mycon);
				} catch (LDAPException e) {
					logger.warn("Error while disconnecting LDAPConnection", e);
				}
			}
		}
	}
}
