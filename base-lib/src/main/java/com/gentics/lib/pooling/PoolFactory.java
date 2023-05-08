/*
 * @author stefan
 * @date Mar 8, 2006
 * @version $Id$
 */
package com.gentics.lib.pooling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.lib.log.NodeLogger;

/**
 * The PoolFactory can be used to create new preconfigured pools.
 * To create a pool, get a new poolfactory which uses a given configuration.
 * Don't forget to set an poolableobjectfactory to your new pool.
 */
public class PoolFactory {

	private static class MyPoolFactory {

		public static final String PARAM_CLASS = "class";
        
		public static final String PARAM_MAX_ACTIVE = "maxactive";
		public static final String PARAM_MAX_IDLE = "maxidle";
		public static final String PARAM_MAX_WAIT = "maxwait";
		public static final String PARAM_MIN_IDLE = "minidle";
        
		public static final String PARAM_TEST_ON_BORROW = "testonborrow";
		public static final String PARAM_TEST_ON_RETURN = "testonreturn";
		public static final String PARAM_TEST_WHILE_IDLE = "testwhileidle";

		public static final String PARAM_EXHAUSTED_ACTION = "onexhaustedaction";

		public static final String EXHAUSTED_ACTION_FAIL = "fail";
		public static final String EXHAUSTED_ACTION_BLOCK = "block";
		public static final String EXHAUSTED_ACTION_GROW = "grow";

		private final Map params;

		protected MyPoolFactory(Map params) {
			this.params = params;            
		}
        
		protected String getParam(String name) {
			return (String) params.get(name);
		}

		protected String getParam(String name, String defaultValue) {
			return (String) (params.containsKey(name) ? params.get(name) : defaultValue);
		}

		protected int getIntParam(String name, int defaultValue) {

			if (!params.containsKey(name)) {
				return defaultValue;
			}

			int val = defaultValue;

			try {
				val = Integer.parseInt((String) params.get(name));
			} catch (NumberFormatException e) {
				getLogger().warn("Could not parse int value {" + params.get(name) + ") of param {" + name + "}, using default value {" + defaultValue + "}");
			}

			return val;
		}

		/**
		 * get the class of the pool to use.
		 * @param defaultClass the class to use if not set in the configparams.
		 * @return the class of the pool.
		 */
		protected Class getPoolClass(Class defaultClass) {
            
			Class clazz;
            
			if (params.containsKey(PARAM_CLASS)) {
				try {
					clazz = Class.forName((String) params.get(PARAM_CLASS));
				} catch (ClassNotFoundException e) {
					getLogger().error("Could not  create a pool: Unknown class {" + params.get(PARAM_CLASS) + "}.", e);
					return null;
				}
			} else {  
				clazz = defaultClass;
			}
            
			return clazz;
		}
        
		/**
		 * try to set all parameters to a given pool using bean-setters. 
		 * @param pool the pool to configure.
		 */
		protected void setPoolParameter(Object pool) {
            
			// TODO implement, use ClassHelper to try to set values
			if (pool instanceof GenericKeyedObjectPool) {

				GenericKeyedObjectPool oPool = (GenericKeyedObjectPool) pool;

				// This is very important, else pool will block when exhausted!
				oPool.setMaxActive(getIntParam(PARAM_MAX_ACTIVE, -1));
				oPool.setMaxIdle(getIntParam(PARAM_MAX_IDLE, 10));
				oPool.setMaxTotal(getIntParam(PARAM_MAX_ACTIVE, -1));
				oPool.setMaxWait(getIntParam(PARAM_MAX_WAIT, -1));

				String exAction = getParam(PARAM_EXHAUSTED_ACTION, EXHAUSTED_ACTION_GROW);

				if (EXHAUSTED_ACTION_GROW.equals(exAction)) {
					oPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);
				} else if (EXHAUSTED_ACTION_FAIL.equals(exAction)) {
					oPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);
				} else if (EXHAUSTED_ACTION_BLOCK.equals(exAction)) {
					oPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
				} else {
					oPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);
					getLogger().warn(
							"Invalid exhaustion action {" + exAction + "}, using " + EXHAUSTED_ACTION_GROW + ".\n" + "Should be one of " + EXHAUSTED_ACTION_GROW
							+ ", " + EXHAUSTED_ACTION_FAIL + ", " + EXHAUSTED_ACTION_BLOCK + ".");
				}

			} else {
				getLogger().warn("Setting of pool configuration not yet available for this pool {" + pool.getClass() + ")");
			}

		}
        
		protected NodeLogger getLogger() {
			return NodeLogger.getNodeLogger(getClass());
		}
	}
    
	private static class MyKeyedObjectPoolFactory extends MyPoolFactory 
			implements KeyedObjectPoolFactory {

		private MyKeyedObjectPoolFactory(Map params) {
			super(params);
		}
        
		public KeyedObjectPool createPool() {
            
			Class clazz = getPoolClass(GenericKeyedObjectPool.class);

			if (clazz == null) {
				return null;
			}
            
			KeyedObjectPool pool;            

			if (KeyedObjectPool.class.isAssignableFrom(clazz)) {
				try {
					pool = (KeyedObjectPool) clazz.newInstance();
				} catch (InstantiationException e) {
					getLogger().error("Could not create a new pool.", e);
					return null;
				} catch (IllegalAccessException e) {
					getLogger().error("Could not create a new pool.", e);
					return null;
				} 
			} else {
				getLogger().error("Could not create pool: Class {" + clazz.getName() + "} is not a KeyedObjectPool.");
				return null;
			}

			setPoolParameter(pool);
            
			return pool;
		}
        
	}

	private static class MyObjectPoolFactory extends MyPoolFactory implements ObjectPoolFactory {
        
		public MyObjectPoolFactory(Map params) {
			super(params);            
		}

		public ObjectPool createPool() {
            
			Class clazz = getPoolClass(GenericObjectPool.class);

			if (clazz == null) {
				return null;
			}
            
			ObjectPool pool;            

			if (ObjectPool.class.isAssignableFrom(clazz)) {
				try {
					pool = (ObjectPool) clazz.newInstance();
				} catch (InstantiationException e) {
					getLogger().error("Could not create a new pool.", e);
					return null;
				} catch (IllegalAccessException e) {
					getLogger().error("Could not create a new pool.", e);
					return null;
				} 
			} else {
				getLogger().error("Could not create pool: Class {" + clazz.getName() + "} is not a KeyedObjectPool.");
				return null;
			}
            
			setPoolParameter(pool);
            
			return pool;
		}
        
	}
    
	/**
	 * get an objectpoolfactory which uses a property-configurationfile.
	 * @param configFile the complete filename of the configuration file to use.
	 * @return a new poolfactory which can be used to create new, preconfigured pools.
	 */
	public static ObjectPoolFactory getObjectPoolFactory(String configFile) {
        
		Properties props = loadPropertiesFile(configFile);
        
		return new PoolFactory.MyObjectPoolFactory(props);
	}
    
	/**
	 * get a keyedobjectpoolfactory which uses a property-configurationfile.
	 * @param configFile the complete filename of the configuration file to use.
	 * @return a new poolfactory which can be used to create new, preconfigured pools.
	 */
	public static KeyedObjectPoolFactory getKeyedObjectPoolFactory(String configFile) {
        
		Properties props = loadPropertiesFile(configFile);
        
		return new PoolFactory.MyKeyedObjectPoolFactory(props);
	}
    
	private static Properties loadPropertiesFile(String configFile) {
        
		Properties props = new Properties();
        
		if (configFile == null) {
			return props;
		}
        
		File file = new File(configFile);

		if (!file.exists()) {
			NodeLogger.getLogger(PoolFactory.class).warn("Could not load pool configuration file {" + configFile + "}: File does not exist.");
		} else {
        
			try {
				props.load(new FileInputStream(file));
                
			} catch (FileNotFoundException e) {
				NodeLogger.getLogger(PoolFactory.class).warn("Could not load pool configuration file {" + configFile + "}", e);
			} catch (IOException e) {
				NodeLogger.getLogger(PoolFactory.class).warn("Could not load pool configuration file {" + configFile + "}", e);
			}
		}
        
		return props;
	}
}
