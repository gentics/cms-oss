package com.gentics.contentnode.publish;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.contentnode.publish.CnMapPublishHandler2;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublishHandlerInfoMBean;
import com.gentics.contentnode.jmx.PublishHandlerStatus;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Static class for keeping track of all publish handlers (instances of {@link CnMapPublishHandler}
 */
public class PublishHandlerStore {

	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PublishHandlerStore.class);

	/**
	 * Map of publish handlers (per contentrepository id and md5 hash)
	 */
	protected static Map<Integer, Map<String, PublishHandlerWrapper>> publishHandlers = new HashMap<Integer, Map<String, PublishHandlerWrapper>>();

	/**
	 * Add the publish handler instances configured for the given contentmap. New publish handler instances will be taken into service (by calling
	 * {@link CnMapPublishHandler#init(Map)} on them), still existing ones are just added, old ones are taken out of service (by calling
	 * {@link CnMapPublishHandler#destroy()})
	 * @param contentMap contentmap
	 * @throws NodeException 
	 */
	public static void addPublishHandlers(final ContentMap contentMap) throws NodeException {
		// collect the md5 hashes of the found publish handlers here
		final List<String> foundMD5Hashes = new Vector<String>();

		DBUtils.executeStatement("SELECT name, javaclass, properties FROM cr_publish_handler WHERE contentrepository_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, contentMap.getId());
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					String name = rs.getString("name");
					String javaClass = rs.getString("javaclass");
					String properties = rs.getString("properties");

					try {
						String md5Hash = getMD5Hash(name, javaClass, properties);

						contentMap.addPublishHandler(getPublishHandler(contentMap, md5Hash, name, javaClass, properties));
						foundMD5Hashes.add(md5Hash);
					} catch (CnMapPublishException e) {
						throw new NodeException("Error while initialize publish handlers for " + contentMap, e);
					}
				}
			}
		});

		// now take the no longer found publish handlers out of service
		destroyInactiveHandlers(contentMap, foundMD5Hashes);
	}

	/**
	 * Destroy and remove all publish handler instances
	 */
	public static void destroyAllPublishHandlers() {
		for (Map<String, PublishHandlerWrapper> handlerMap : publishHandlers.values()) {
			for (PublishHandlerWrapper namedHandler : handlerMap.values()) {
				if (logger.isInfoEnabled()) {
					logger.info("Destroying publish handler '" + namedHandler.name + "'");
				}
				namedHandler.destroy();
				if (logger.isInfoEnabled()) {
					logger.info("Destroyed publish handler '" + namedHandler.name + "'");
				}
			}
		}
		publishHandlers.clear();
	}

	/**
	 * Get the instance of the publish handler
	 * @param contentMap
	 * @param name
	 * @param javaClass
	 * @param properties
	 * @return
	 * @throws NodeException
	 */
	protected static CnMapPublishHandler getPublishHandler(ContentMap contentMap, String md5Hash, String name, String javaClass, String properties) throws CnMapPublishException {
		Map<String, PublishHandlerWrapper> handlerMap = getHandlerMap(contentMap);
		PublishHandlerWrapper wrapper = handlerMap.get(md5Hash);

		if (wrapper == null) {
			try {
				wrapper = createWrapper(name, createAndInitializeHandler(contentMap, name, javaClass, properties));
			} catch (NotCompliantMBeanException e) {
				throw new CnMapPublishException(e);
			}
			handlerMap.put(md5Hash, wrapper);
		}

		return wrapper;
	}

	/**
	 * Create the appropriate wrapper
	 * @param name handler name
	 * @param instance handler instance
	 * @return wrapper
	 * @throws NotCompliantMBeanException
	 */
	protected static PublishHandlerWrapper createWrapper(String name, CnMapPublishHandler instance) throws NotCompliantMBeanException {
		if (instance instanceof CnMapPublishHandler2) {
			return new PublishHandlerWrapper2(name, (CnMapPublishHandler2)instance);
		} else {
			return new PublishHandlerWrapper(name, instance);
		}
	}

	/**
	 * Create an initialize an instance of the publish handler
	 * @param contentMap content map
	 * @param name name
	 * @param javaClass java class
	 * @param properties properties
	 * @return initialized publish handler instance
	 * @throws CnMapPublishException if the publish handler fails to be initialized
	 */
	protected static CnMapPublishHandler createAndInitializeHandler(ContentMap contentMap, String name, String javaClass, String properties) throws CnMapPublishException {
		// try to load the given class
		try {
			Class<?> clazz = Class.forName(javaClass);

			// check whether the class implements the interface
			if (!CnMapPublishHandler.class.isAssignableFrom(clazz)) {
				throw new CnMapPublishException(
						"Configured publish event handler class '" + javaClass + "' of event handler '" + name + "' for contentrepository '" + contentMap.getId()
						+ "' does not implement '" + CnMapPublishHandler.class.getName() + "'");
			} else {
				if (logger.isInfoEnabled()) {
					logger.info("Creating publish handler '" + name + "' for '" + contentMap + "' as instance of '" + javaClass + "'");
				}
				CnMapPublishHandler publishHandler = (CnMapPublishHandler) clazz.newInstance();
				Properties handlerProps = new Properties();

				if (!StringUtils.isEmpty(properties)) {
					handlerProps.load(new ByteArrayInputStream(properties.getBytes("ISO-8859-1")));
				}
				// put the datasource ID into the properties
				handlerProps.put(CnMapPublishHandler.DS_ID, contentMap.getWritableDatasource().getId());
				if (logger.isInfoEnabled()) {
					logger.info("Initializing publish handler '" + name + "' for '" + contentMap + "'");
				}
				publishHandler.init(handlerProps);
				if (logger.isInfoEnabled()) {
					logger.info("Initialized publish handler '" + name + "' for '" + contentMap + "'");
				}
				return publishHandler;
			}
		} catch (CnMapPublishException e) {
			throw e;
		} catch (Exception e) {
			throw new CnMapPublishException("Error while initializing event handler '" + name + "'", e);
		}
	}

	/**
	 * Destroy the publish handlers, that are no longer active.
	 * @param contentMap content map
	 * @param activeMD5Hashes list of md5 hashes of the still active publish handlers
	 */
	protected static void destroyInactiveHandlers(ContentMap contentMap, List<String> activeMD5Hashes) {
		Map<String, PublishHandlerWrapper> handlerMap = getHandlerMap(contentMap);

		for (Map.Entry<String, PublishHandlerWrapper> entry : handlerMap.entrySet()) {
			if (!activeMD5Hashes.contains(entry.getKey())) {
				if (logger.isInfoEnabled()) {
					logger.info("Destroying publish handler '" + entry.getValue().name + "' for '" + contentMap + "'");
				}
				entry.getValue().destroy();
				if (logger.isInfoEnabled()) {
					logger.info("Destroyed publish handler '" + entry.getValue().name + "' for '" + contentMap + "'");
				}
			}
		}
		handlerMap.keySet().retainAll(activeMD5Hashes);
	}

	/**
	 * Get the handler map for the given contentmap (create one if not done before)
	 * @param contentMap contentmap
	 * @return map of publish handlers
	 */
	protected static Map<String, PublishHandlerWrapper> getHandlerMap(ContentMap contentMap) {
		Map<String, PublishHandlerWrapper> handlerMap = publishHandlers.get(contentMap.getId());

		if (handlerMap == null) {
			handlerMap = new HashMap<String, PublishHandlerWrapper>();
			publishHandlers.put(contentMap.getId(), handlerMap);
		}
		return handlerMap;
	}

	/**
	 * Get the md5 hash over the name, javaClass and properties
	 * @param name
	 * @param javaClass
	 * @param properties
	 * @return md5 hash
	 */
	protected static String getMD5Hash(String name, String javaClass, String properties) {
		StringBuffer buffer = new StringBuffer(name);

		buffer.append("\n").append(javaClass);
		buffer.append("\n").append(properties);
		return StringUtils.md5(buffer.toString());
	}

	/**
	 * Helper class for encapsulating a publish handler instance with its name
	 */
	protected static class PublishHandlerWrapper extends StandardMBean implements CnMapPublishHandler, PublishHandlerInfoMBean {

		/**
		 * Name
		 */
		protected String name;

		/**
		 * Instance
		 */
		protected CnMapPublishHandler instance;

		/**
		 * Number of created objects
		 */
		protected long created = 0;

		/**
		 * Average creation time
		 */
		protected long totalCreateTime = 0;

		/**
		 * Number of updated objects
		 */
		protected long updated = 0;

		/**
		 * Average update time
		 */
		protected long totalUpdateTime = 0;

		/**
		 * Number of deleted objects
		 */
		protected long deleted = 0;

		/**
		 * Average delete time
		 */
		protected long totalDeleteTime = 0;

		/**
		 * Current status
		 */
		protected PublishHandlerStatus status = PublishHandlerStatus.IDLE;

		/**
		 * Create an instance
		 * @param name name
		 * @param instance instance
		 * @throws NotCompliantMBeanException 
		 */
		public PublishHandlerWrapper(String name, CnMapPublishHandler instance) throws NotCompliantMBeanException {
			super(PublishHandlerInfoMBean.class);
			this.name = name;
			this.instance = instance;
			MBeanRegistry.registerMBean(this, "Publish", "Publish Handler " + name);
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#init(java.util.Map)
		 */
		public void init(@SuppressWarnings("rawtypes") Map parameters) throws CnMapPublishException {
			try {
				status = PublishHandlerStatus.INIT;
				instance.init(parameters);
			} finally {
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#open(long)
		 */
		public void open(long timestamp) throws CnMapPublishException {
			try {
				status = PublishHandlerStatus.OPEN;
				instance.open(timestamp);
			} finally {
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#createObject(com.gentics.api.lib.resolving.Resolvable)
		 */
		public void createObject(Resolvable object) throws CnMapPublishException {
			long start = System.currentTimeMillis();

			try {
				status = PublishHandlerStatus.CREATEOBJECT;
				created++;
				instance.createObject(object);
			} finally {
				totalCreateTime += (System.currentTimeMillis() - start);
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#updateObject(com.gentics.api.lib.resolving.Resolvable)
		 */
		public void updateObject(Resolvable object) throws CnMapPublishException {
			long start = System.currentTimeMillis();

			try {
				status = PublishHandlerStatus.UPDATEOBJECT;
				updated++;
				instance.updateObject(object);
			} finally {
				totalUpdateTime += (System.currentTimeMillis() - start);
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#deleteObject(com.gentics.api.lib.resolving.Resolvable)
		 */
		public void deleteObject(Resolvable object) throws CnMapPublishException {
			long start = System.currentTimeMillis();

			try {
				status = PublishHandlerStatus.DELETEOBJECT;
				deleted++;
				instance.deleteObject(object);
			} finally {
				totalDeleteTime += (System.currentTimeMillis() - start);
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#commit()
		 */
		public void commit() {
			try {
				status = PublishHandlerStatus.COMMIT;
				instance.commit();
			} finally {
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#rollback()
		 */
		public void rollback() {
			try {
				status = PublishHandlerStatus.ROLLBACK;
				instance.rollback();
			} finally {
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#close()
		 */
		public void close() {
			try {
				status = PublishHandlerStatus.CLOSE;
				instance.close();
			} finally {
				status = PublishHandlerStatus.IDLE;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#destroy()
		 */
		public void destroy() {
			try {
				status = PublishHandlerStatus.DESTROY;
				instance.destroy();
			} finally {
				status = PublishHandlerStatus.IDLE;
				MBeanRegistry.unregisterMBean("Publish", "Publish Handler " + name);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getName()
		 */
		public String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getJavaClass()
		 */
		public String getJavaClass() {
			return instance.getClass().getName();
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getCreated()
		 */
		public long getCreated() {
			return created;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getUpdated()
		 */
		public long getUpdated() {
			return updated;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getDeleted()
		 */
		public long getDeleted() {
			return deleted;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getAvgCreateTime()
		 */
		public long getAvgCreateTime() {
			return created == 0 ? 0 : totalCreateTime / created;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getAvgUpdateTime()
		 */
		public long getAvgUpdateTime() {
			return updated == 0 ? 0 : totalUpdateTime / updated;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getAvgDeleteTime()
		 */
		public long getAvgDeleteTime() {
			return deleted == 0 ? 0 : totalDeleteTime / deleted;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.jmx.PublishHandlerInfoMBean#getStatus()
		 */
		public String getStatus() {
			return status.toString();
		}
	}

	/**
	 * Wrapper implementation of {@link CnMapPublishHandler2}
	 */
	protected static class PublishHandlerWrapper2 extends PublishHandlerWrapper implements CnMapPublishHandler2 {
		/**
		 * Instance
		 */
		protected CnMapPublishHandler2 instance2;

		/**
		 * Create an instance
		 * @param name name
		 * @param instance instance
		 * @throws NotCompliantMBeanException
		 */
		public PublishHandlerWrapper2(String name, CnMapPublishHandler2 instance) throws NotCompliantMBeanException {
			super(name, instance);
			this.instance2 = instance;
		}

		@Override
		public void updateObject(Resolvable object, Resolvable original, Set<String> attributes) throws CnMapPublishException {
			long start = System.currentTimeMillis();

			try {
				status = PublishHandlerStatus.UPDATEOBJECT;
				updated++;
				instance2.updateObject(object, original, attributes);
			} finally {
				totalUpdateTime += (System.currentTimeMillis() - start);
				status = PublishHandlerStatus.IDLE;
			}
		}
	}
}
