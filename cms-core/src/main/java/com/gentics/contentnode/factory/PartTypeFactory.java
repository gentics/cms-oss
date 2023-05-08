/*
 * @author Stefan Hepp
 * @date 23.1.2006
 * @version $Id: PartTypeFactory.java,v 1.18.20.3 2011-03-10 10:08:56 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import com.gentics.api.contentnode.parttype.ExtensiblePartType;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ExtensiblePartTypeWrapper;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.lib.etc.StringUtils;

/**
 * The parttype factory can be used to get a parttype implementation by
 * parttype-id.
 */
public class PartTypeFactory {

	private static PartTypeFactory factory;
    
	private static final ConcurrentHashMap<Integer, PartTypeInfo> partTypeInfoCache = new ConcurrentHashMap<Integer, PartTypeInfo>();

	private PartTypeFactory() {}

	/**
	 * get a static instance of the parttypefactory.
	 *
	 * TODO do I need configuration here (for user parts) -> need the current configuration key here (parameter).
	 *
	 * @return a reference to the static instance of the parttype factory.
	 */
	public static PartTypeFactory getInstance() {
		if (factory == null) {
			factory = new PartTypeFactory();
		}
		return factory;
	}

	/**
	 * Check whether the part with given id is valueless or not
	 * @param typeId type id
	 * @return true when the parttype is valueless, false if not
	 * @throws NodeException
	 */
	public boolean isValueless(int typeId) throws NodeException {
		PartTypeInfo partTypeInfo = getPartTypeInfo(typeId);

		// check if classname is set
		if (null == partTypeInfo) {// TODO: error or just return null
		} else {

			// when the class implements ExtensiblePartType, we create a wrapper
			if (ExtensiblePartType.class.isAssignableFrom(partTypeInfo.getClazz())) {
				return true;
			}
		}
        
		return false;
	}

	private PartTypeInfo getPartTypeInfo(int partTypeId) throws NodeException {
		// see if its in the cache
		PartTypeInfo partTypeInfo = partTypeInfoCache.get(partTypeId);

		if (null != partTypeInfo) {
			return partTypeInfo;
		}

		// if not, retrieve it and put it in the cache.
		// please note, that this doesn't do any synchronization, since
		// it doesn't matter if multiple threads load and put the same value
		// at the same time (which can only possibly happen when the value
		// is first loaded). The thread-visibility is taken care of by the
		// ConcurrentHashMap.
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement p = null;
		ResultSet res = null;

		try {
			p = t.prepareStatement("SELECT name, javaclass FROM type WHERE id = ?");
			p.setInt(1, partTypeId);

			res = p.executeQuery();

			if (res.next()) {
				Class partTypeClass = null;
				String partTypeClassName = res.getString("javaclass");
				String partTypeName = res.getString("name");

				res.close();

				if (StringUtils.isEmpty(partTypeClassName)) {
					return null;
				}
                
				// try to load the class TODO: check for classloader here
				try {
					partTypeClass = Class.forName(partTypeClassName);
				} catch (ClassNotFoundException e) {
					throw new NodeException("No class for partType " + partTypeId, e);
				}

				partTypeInfo = new PartTypeInfo(partTypeClass, partTypeName);
				partTypeInfoCache.put(partTypeId, partTypeInfo);
				return partTypeInfo;
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new NodeException("SQLException while loading javaclass for id " + partTypeId, e);
			// TODO error handling
		} finally {
			t.closeResultSet(res);
			t.closeStatement(p);
		}
	}

	/**
	 * create a new PartType for a given parttype-id.
	 * @param typeId the parttype-id of the parttype.
	 * @param value the value which should be rendered by the parttype.
	 * @return a new parttype, or null if the parttype is unknown.
	 */
	public PartType getPartType(int typeId, Value value) throws NodeException {
        
		PartTypeInfo partTypeInfo = getPartTypeInfo(typeId);

		if (null == partTypeInfo) {
			// TODO: error or just return null?
			return null;
		}
        
		// when the class implements ExtensiblePartType, we create a wrapper
		if (ExtensiblePartType.class.isAssignableFrom(partTypeInfo.getClazz())) {
			ExtensiblePartType wrappedPartType;
            
			try {
				wrappedPartType = (ExtensiblePartType) partTypeInfo.getClazz().newInstance();
			} catch (Exception e) {
				throw new NodeException("Can't create new instance for part type " + typeId, e);
			}

			ExtensiblePartTypeWrapper wrapper = new ExtensiblePartTypeWrapper(value, wrappedPartType);

			wrapper.setAnnotationClass(partTypeInfo.getAnnotationClass());
			return wrapper;
		} else {
            
			// class must implement PartType
			if (!PartType.class.isAssignableFrom(partTypeInfo.getClazz())) {// TODO error handling
			}

			try {
				// get the desired constructor
				Constructor constructor = partTypeInfo.getClazz().getConstructor(new Class[] { Value.class});
				PartType partType = (PartType) constructor.newInstance(value);

				partType.setAnnotationClass(partTypeInfo.getAnnotationClass());
				return partType;
			} catch (Exception e) {
				throw new NodeException("Unable to create new instance for part type " + typeId, e);
			}
		}
	}

	/**
	 * Check whether the given part matches the parttype
	 * @param part part
	 * @param partType parttype
	 * @return true iff part and parttype match
	 * @throws NodeException
	 */
	public boolean matches(Part part, PartType partType) throws NodeException {
		if (part == null || partType == null) {
			return false;
		}

		PartTypeInfo partTypeInfo = getPartTypeInfo(part.getPartTypeId());
		if (partTypeInfo == null) {
			return false;
		}

		if (ExtensiblePartType.class.isAssignableFrom(partTypeInfo.getClazz())) {
			return ExtensiblePartTypeWrapper.class.equals(partType.getClass());
		} else {
			return partTypeInfo.getClazz().equals(partType.getClass());
		}
	}

	/**
	 * Class for parttype info (class and annotation name)
	 */
	protected class PartTypeInfo {

		/**
		 * Parttype class
		 */
		protected Class<?> clazz;

		/**
		 * Parttype annotation class
		 */
		protected String annotationClass;

		/**
		 * Create an instance
		 * @param clazz class
		 * @param name name (will be transformed to the annotation class)
		 */
		public PartTypeInfo(Class<?> clazz, String name) {
			this.clazz = clazz;
			this.annotationClass = buildAnnotationClass(name);
		}

		/**
		 * Build the annotation class out of the type name
		 * @param name type name
		 * @return annotation class
		 */
		protected String buildAnnotationClass(String name) {
			if (ObjectTransformer.isEmpty(name)) {
				return name;
			}
			return name.replaceAll("\\(|\\)|\\/|", "").replaceAll("[^a-zA-Z_]", "_").toLowerCase();
		}

		/**
		 * Get the parttype class
		 * @return class
		 */
		public Class<?> getClazz() {
			return clazz;
		}

		/**
		 * Get the annotation class
		 * @return annotation class
		 */
		public String getAnnotationClass() {
			return annotationClass;
		}
	}
}
