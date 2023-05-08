package com.gentics.lib.datasource;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.resolving.ChangeableBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.DataSourceConnector;
import com.gentics.lib.db.NonPoolingConnector;
import com.gentics.lib.db.SimpleDerbyConnector;
import com.gentics.lib.db.SimpleHsqlConnector;
import com.gentics.lib.db.SimpleMysqlConnector;
import com.gentics.lib.db.SimpleOracleConnector;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.etc.StringUtils;

/**
 * A datasource handle for SQL databases.
 * 
 * @author haymo
 * @date 04.08.2004
 */
public class SQLHandle extends AbstractDatasourceHandle {

	private DBHandle handle;
    
	/**
	 * string representation of the sql handle
	 */
	private String stringRepresentation;

	/**
	 * Connection pool (if one is used)
	 */
	private GenericObjectPool connectionPool;

	/**
	 * name of the handle parameter containing the name of the handle
	 */
	public final static String PARAM_NAME = "gtx_name";

	/**
	 * name of the handle parameter containing the used db schema
	 */
	public final static String PARAM_DBSCHEMA = "dbschema";

	/**
	 * Name of the handle parameter for the fetchsize
	 */
	public final static String PARAM_FETCHSIZE = "fetchsize";

	/**
	 * Array of parameters that should be ignored during initialization of the datasource
	 */
	private final static String[] SKIP_PARAMETERS = new String[]{ PARAM_FETCHSIZE, PARAM_DBSCHEMA };

	/**
	 * Create an instance of the SQLHandle
	 * @param id id of the handle
	 */
	public SQLHandle(String id) {
		super(id);
	}

	/**
	 * @deprecated will be removed.
	 */
	public final int connect() {
		// connect to the server
		return 0;
	}

	/**
	 * @deprecated will be removed.
	 */
	public final void disconnect() {// disconnect with the server
	}

	public void close() {
		DB.closeConnector(handle);
		if (connectionPool != null) {
			try {
				connectionPool.close();
			} catch (Exception e) {
				logger.error("Error while closing connection pool", e);
			}
			connectionPool = null;
		}
	}

	public abstract static class ParameterBean extends ChangeableBean {
		public String type = "mysql";
		public String name = "";
		public String portalConfigurationPath;

		public boolean cachedbmetadata = false;

		public void setType(String type) {
			this.type = type;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setPortalConfigurationPath(String portalConfigurationPath) {
			this.portalConfigurationPath = portalConfigurationPath;
		}

		public void setCachedbmetadata(Object cachedbmetadata) {
			this.cachedbmetadata = ObjectTransformer.getBoolean(cachedbmetadata, false);
		}

		public boolean getCachedbmetadata() {
			return cachedbmetadata;
		}
	}

	public static class SQLParameterBean extends ParameterBean {
		private static final long serialVersionUID = -1444302146351656550L;

		public String host = "localhost";
		public String database = "node";
		public String username = "node";
		public String passwd = "";
		public int port = 42006;
		public int poolsize = 20;

		public void setDatabase(String database) {
			this.database = database;
		}

		public void setPasswd(String passwd) {
			this.passwd = passwd;
		}

		public void setPoolsize(int poolsize) {
			this.poolsize = poolsize;
		}

		public void setPoolsize(String poolsize) {
			setPoolsize(Integer.parseInt(poolsize));
		}

		public void setPort(int port) {
			this.port = port;
		}

		public void setPort(String port) {
			setPort(Integer.parseInt(port));
		}

		public void setHost(String server) {
			this.host = server;
		}

		public void setUsername(String username) {
			this.username = username;
		}
	}

	public static class JDBCParameterBean extends ParameterBean {
		private static final long serialVersionUID = -4094980586112781922L;

		/**
		 * prefix for driver properties
		 */
		public final static String DRIVER_PROP_PREFIX = "driver.";

		/**
		 * prefix for pooling properties
		 */
		public final static String POOLING_PROP_PREFIX = "pooling.";

		public String driverClass = "";
		public String url = "";
		public String username = null;
		public String passwd = "";
		public int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
		public byte whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
		public long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
		public int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
		public int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
		public boolean testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;
		public boolean testOnReturn = GenericObjectPool.DEFAULT_TEST_ON_RETURN;
		public long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
		public int numTestsPerEvictionRun = GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
		public long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
		public boolean testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;
		public String validationQuery = null;

		/**
		 * driver properties (if any set)
		 */
		public Properties driverProperties = new Properties();

		/**
		 * Shutdown command (sent to the database when the handle is disconnected)
		 */
		public String shutDownCommand = null;

		/**
		 * @param validationQuery The validationQuery to set.
		 */
		public void setValidationQuery(String validationQuery) {
			this.validationQuery = validationQuery;
		}

		/**
		 * @param maxActive The maxActive to set.
		 */
		public void setMaxActive(String maxActive) {
			this.maxActive = ObjectTransformer.getInt(maxActive, this.maxActive);
		}

		/**
		 * @param maxIdle The maxIdle to set.
		 */
		public void setMaxIdle(String maxIdle) {
			this.maxIdle = ObjectTransformer.getInt(maxIdle, this.maxIdle);
		}

		/**
		 * @param maxWait The maxWait to set.
		 */
		public void setMaxWait(String maxWait) {
			this.maxWait = ObjectTransformer.getLong(maxWait, this.maxWait);
		}

		/**
		 * @param minEvictableIdleTimeMillis The minEvictableIdleTimeMillis to set.
		 */
		public void setMinEvictableIdleTimeMillis(String minEvictableIdleTimeMillis) {
			this.minEvictableIdleTimeMillis = ObjectTransformer.getLong(minEvictableIdleTimeMillis, this.minEvictableIdleTimeMillis);
		}

		/**
		 * @param minIdle The minIdle to set.
		 */
		public void setMinIdle(String minIdle) {
			this.minIdle = ObjectTransformer.getInt(minIdle, this.minIdle);
		}

		/**
		 * @param numTestsPerEvictionRun The numTestsPerEvictionRun to set.
		 */
		public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
			this.numTestsPerEvictionRun = numTestsPerEvictionRun;
		}

		/**
		 * @param testOnBorrow The testOnBorrow to set.
		 */
		public void setTestOnBorrow(String testOnBorrow) {
			this.testOnBorrow = ObjectTransformer.getBoolean(testOnBorrow, this.testOnBorrow);
		}

		/**
		 * @param testOnReturn The testOnReturn to set.
		 */
		public void setTestOnReturn(String testOnReturn) {
			this.testOnReturn = ObjectTransformer.getBoolean(testOnReturn, this.testOnReturn);
		}

		/**
		 * @param testWhileIdle The testWhileIdle to set.
		 */
		public void setTestWhileIdle(String testWhileIdle) {
			this.testWhileIdle = ObjectTransformer.getBoolean(testWhileIdle, this.testWhileIdle);
		}

		/**
		 * @param timeBetweenEvictionRunsMillis The timeBetweenEvictionRunsMillis to set.
		 */
		public void setTimeBetweenEvictionRunsMillis(String timeBetweenEvictionRunsMillis) {
			this.timeBetweenEvictionRunsMillis = ObjectTransformer.getLong(timeBetweenEvictionRunsMillis, this.timeBetweenEvictionRunsMillis);
		}

		/**
		 * @param whenExhaustedAction The whenExhaustedAction to set.
		 */
		public void setWhenExhaustedAction(String whenExhaustedAction) {
			if ("block".equalsIgnoreCase(whenExhaustedAction)) {
				this.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
			} else if ("fail".equalsIgnoreCase(whenExhaustedAction)) {
				this.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
			} else if ("grow".equalsIgnoreCase(whenExhaustedAction)) {
				this.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
			}
		}

		/**
		 * @param passwd The passwd to set.
		 */
		public void setPasswd(String passwd) {
			this.passwd = passwd;
		}

		/**
		 * @param username The username to set.
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * @param driverClass The driverClass to set.
		 */
		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}

		/**
		 * @param url The url to set.
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * @param shutDownCommand The shutDownCommand to set.
		 */
		public void setShutDownCommand(String shutDownCommand) {
			this.shutDownCommand = shutDownCommand;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();

			buffer.append(driverClass).append("|").append(url).append("|").append(username);

			return buffer.toString();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.ChangeableBean#setProperty(java.lang.String, java.lang.Object)
		 */
		public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
			if (name.startsWith(DRIVER_PROP_PREFIX)) {
				driverProperties.setProperty(name.substring(DRIVER_PROP_PREFIX.length()), ObjectTransformer.getString(value, null));
				return true;
			} else if (name.startsWith(POOLING_PROP_PREFIX)) {
				return setProperty(name.substring(POOLING_PROP_PREFIX.length()), value);
			} else {
				return super.setProperty(name, value);
			}
		}
	}

	public static class JNDIParameterBean extends ParameterBean {
		private static final long serialVersionUID = -3207789334973732016L;

		/**
		 * Shutdown command (sent to the database when the handle is disconnected)
		 */
		public String shutDownCommand = null;

		/**
		 * Context
		 */
		public String context = "java:comp/env";

		/**
		 * @param shutDownCommand The shutDownCommand to set.
		 */
		public void setShutDownCommand(String shutDownCommand) {
			this.shutDownCommand = shutDownCommand;
		}

		/**
		 * Set the context
		 * @param context context
		 */
		public void setContext(String context) {
			this.context = context;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "jndi|" + this.name;
		}
	}

	/**
	 * Reads the parameters from the map and sets them using the setters of the given bean.
	 * 
	 * @param parameters The key/value map of parameters
	 * @param parameterBean The bean that should be used to set the parameters 
	 */
	protected void readParameters(Map parameters, ChangeableBean parameterBean) {
		for (Iterator it = parameters.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();

			// omit the name parameter
			if (PARAM_NAME.equals(key)) {
				continue;
			}

			// skip all parameters from the skip parameters array
			if (key != null && Arrays.asList(SKIP_PARAMETERS).contains(key)) {
				continue;
			}
			boolean successSetProperty = false;

			try {
				successSetProperty = parameterBean.setProperty(key, parameters.get(key));
			} catch (InsufficientPrivilegesException e) {
				logger.error("error while setting configuration parameter for handle {" + id + "}", e);
			}
			if (!successSetProperty) {
				logger.error("Configuration parameter {" + key + "} could not be set for handle {" + id + "}");
			}
		}
	}

	public void init(Map parameters) {
		String type = ObjectTransformer.getString(parameters.get("type"), "mysql");

		// get the name
		String handleName = ObjectTransformer.getString(parameters.get(PARAM_NAME), null);

		if ("mysql".equals(type) || "mariadb".equals(type)) {
			SQLParameterBean params = new SQLParameterBean();

			readParameters(parameters, params);
			this.handle = DB.addConnector(
					new SimpleMysqlConnector(params.host, params.database, params.username, params.passwd, params.port, params.poolsize, true, type), handleName, null,
					params.getCachedbmetadata());
		} else if ("oracle".equals(type)) {
			SQLParameterBean params = new SQLParameterBean();

			readParameters(parameters, params);
			this.handle = DB.addConnector(new SimpleOracleConnector(params.host, params.database, params.username, params.passwd, params.port, params.poolsize),
					handleName, null, params.getCachedbmetadata());
		} else if ("derby".equals(type)) {
			SQLParameterBean params = new SQLParameterBean();

			readParameters(parameters, params);
			handle = DB.addConnector(new SimpleDerbyConnector(params.database, params.username, params.passwd, params.poolsize), handleName, null,
					params.getCachedbmetadata());
		} else if ("hsql".equals(type)) {
			SQLParameterBean params = new SQLParameterBean();

			readParameters(parameters, params);
			params.database = StringUtils.resolveSystemProperties(params.database);
			handle = DB.addConnector(new SimpleHsqlConnector(params.database, params.username, params.passwd, params.poolsize), handleName, null,
					params.getCachedbmetadata());
		} else if ("jdbc".equals(type)) {
			// type "jdbc" will use jakarta commons DBCP for pooling of the database connections
			JDBCParameterBean params = new JDBCParameterBean();

			readParameters(parameters, params);

			if (params.driverClass.length() == 0) {
				logger.error("No driverClass configured for handle {" + id + "} of type {jdbc}");
			} else if (params.url.length() == 0) {
				logger.error("No url configured for handle {" + id + "} of type {jdbc}");
			} else {
				try {
					// load the driver
					Class.forName(params.driverClass);
					// create a pool for the jdbc connection
					connectionPool = new GenericObjectPool(null, params.maxActive, params.whenExhaustedAction, params.maxWait, params.maxIdle, params.minIdle,
							params.testOnBorrow, params.testOnReturn, params.timeBetweenEvictionRunsMillis, params.numTestsPerEvictionRun,
							params.minEvictableIdleTimeMillis, params.testWhileIdle);

					if (logger.isDebugEnabled()) {
						logger.debug("Create connection pool with url {" + params.url + "}");
					}
					params.url = StringUtils.resolveSystemProperties(params.url);
					// create the connection factory
					ConnectionFactory connectionFactory = null;

					if (params.driverProperties.isEmpty()) {
						// this is the normal ds-handle mode 
						if (params.username != null) {
							connectionFactory = new DriverManagerConnectionFactory(params.url, params.username, params.passwd);
						} else {
							connectionFactory = new DriverManagerConnectionFactory(params.url, null);
						}
					} else {
						// this is the new mode, with "driver." properties set,
						// which will be passed to the driver

						// check whether username/passwd were set (warn that they will be ignored)
						if (!StringUtils.isEmpty(params.username)) {
							logger.warn(
									"handle parameter username is set for handle id {" + id
									+ "}, but will be ignored, since at least one {driver.} parameter is set. Set parameter \"driver.user\" instead");
						}
						if (!StringUtils.isEmpty(params.passwd)) {
							logger.warn(
									"handle parameter passwd is set for handle id {" + id
									+ "}, but will be ignored, since at least one {driver.} parameter is set. Set parameter \"driver.password\" instead");
						}

						connectionFactory = new DriverManagerConnectionFactory(params.url, params.driverProperties);
					}
					// create the connection factory
					PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null,
							params.validationQuery, false, true);
					// and the datasource
					PoolingDataSource dataSource = new PoolingDataSource(connectionPool);

					// create an register the DataSourceConnector
					handle = DB.addConnector(new DataSourceConnector(dataSource, params.shutDownCommand), handleName, null, params.getCachedbmetadata());

					// set the string representation
					stringRepresentation = params.toString();
				} catch (ClassNotFoundException ex) {
					logger.error("Error while initializing SQLHandle", ex);
				}
			}
		} else if ("nonpoolingjdbc".equals(type)) {
			// type "nonpoolingjdbc" will create new connections upon each usage
			JDBCParameterBean params = new JDBCParameterBean();

			readParameters(parameters, params);

			if (params.driverClass.length() == 0) {
				logger.error("No driverClass configured for handle {" + id + "} of type {jdbc}");
			} else if (params.url.length() == 0) {
				logger.error("No url configured for handle {" + id + "} of type {jdbc}");
			} else {
				try {
					// load the driver
					Class.forName(params.driverClass);

					params.url = StringUtils.resolveSystemProperties(params.url);

					// create an register the NonPoolingConnector
					handle = DB.addConnector(new NonPoolingConnector(params.url, params.username, params.passwd), handleName, null, params.getCachedbmetadata());

					// set the string representation
					stringRepresentation = params.toString();
				} catch (ClassNotFoundException ex) {
					logger.error("Error while initializing SQLHandle", ex);
				}
			}
		} else if ("jndi".equals(type)) {
			// type "jndi" uses a DataSource provided by the application server
			JNDIParameterBean params = new JNDIParameterBean();

			readParameters(parameters, params);
			try {
				Context initCtx = new InitialContext();
				Context envCtx = (Context) initCtx.lookup(params.context);
				DataSource ds = (DataSource) envCtx.lookup(params.name);

				handle = DB.addConnector(new DataSourceConnector(ds, params.shutDownCommand), handleName, null, params.getCachedbmetadata());
				stringRepresentation = params.toString();
				logger.debug("Initialized jndi datasource. StringRepresentation: {" + stringRepresentation + "}");
			} catch (Exception e) {
				logger.error("Error while initializing SQLHandle {" + id + "} of type {jndi}", e);
			}
		} else {
			logger.error("Unknown type {" + type + "} found for SQLHandle {" + id + "}");
		}

		if (handle != null) {
			handle.setSqlHandle(this);

			// set the db schema name (if configured)
			handle.setDbSchema(ObjectTransformer.getString(parameters.get(PARAM_DBSCHEMA), null));

			// set the fetch size, if configured
			handle.setFetchSize(ObjectTransformer.getInt(parameters.get(PARAM_FETCHSIZE), 0));

			// do a feature test for the db handle
			handle.testFeatures();
		}
	}

	public DBHandle getDBHandle() {
		return this.handle;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return stringRepresentation;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.DatasourceHandle#isAlive()
	 */
	public boolean isAlive() {
		String dummyStatement = this.handle.getDummyStatement();
		if (!StringUtils.isEmpty(dummyStatement)) {
			SimpleResultProcessor rp = new SimpleResultProcessor();
			try {
				DB.query(this.handle, this.handle.getDummyStatement(), rp);
				return true;
			} catch (SQLException e) {
				// if an excpetion occurs, the db handle is not alive anymore
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * Get preferred concat function or null if no concat function supported
	 * @return name of the preferred concat function or null
	 */
	public String getPreferredConcatFunction() {
		return handle != null ? handle.getPreferredConcatFunction() : null;
	}

	/**
	 * Get preferred concat operator or null if no concat operator supported
	 * @return name of the preferred concat operator or null
	 */
	public String getPreferredConcatOperator() {
		return handle != null ? handle.getPreferredConcatOperator() : null;
	}

	/**
	 * Get the datatype name to be used in a CAST(field AS type) operation to
	 * case a field to a text (if CAST is supported anyway)
	 * @return datatype name to CAST to texts, or null if not supported
	 */
	public String getTextCastName() {
		return handle != null ? handle.getTextCastName() : null;
	}
}
