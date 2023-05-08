/*
 * @author stefan.hurjui @date Apr 22, 2005
 * @version $Id: ObjectManagementManager.java,v 1.3 2005/05/09 13:11:35 stefan1
 *          Exp $
 */
package com.gentics.lib.datasource.object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.input.CountingInputStream;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.VersionedObject;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.object.jaxb.Definition;
import com.gentics.lib.datasource.object.jaxb.Objecttype;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DB.ColumnDefinition;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;

public class ObjectManagementManager {

	public static Map<String, String> attributeTypes;

	/**
	 * constant for the pre-save attributetype check for the name. check for
	 * other attributes in the same objecttype with the same name
	 */
	public final static int ATTRIBUTECHECK_NAME = 1;

	/**
	 * constant for the pre-save attributetype check for the type. check for
	 * other attributes with the same name but different configuration
	 */
	public final static int ATTRIBUTECHECK_TYPE = 2;

	/**
	 * Constant for the jaxb package for the exportable objects
	 */
	public final static String JAXB_PACKAGE = "com.gentics.lib.datasource.object.jaxb";

	protected final static NodeLogger logger = NodeLogger.getNodeLogger(ObjectManagementManager.class);

	protected final static List<String> PROTECTED_ATTRIBUTENAMES = new Vector<String>();

	static {
		attributeTypes = new HashMap<String, String>();
		attributeTypes.put("1", "String");
		attributeTypes.put("2", "Object Link");
		attributeTypes.put("3", "Integer");
		attributeTypes.put("5", "Long String");
		attributeTypes.put("6", "Binary");
		attributeTypes.put("7", "Foreign Link");
		attributeTypes.put("8", "Long Integer");
		attributeTypes.put("9", "Double");
		attributeTypes.put("10", "Date");

		// prepare the protected attribute names
		PROTECTED_ATTRIBUTENAMES.addAll(DatatypeHelper.getDefaultColumnTypes().keySet());
		PROTECTED_ATTRIBUTENAMES.add(VersionedObject.VERSIONTIMESTAMP_PROPERTY);
	}

	/**
	 * default constructor
	 */
	protected ObjectManagementManager() {}

	/**
	 * @param dbHandle database handler
	 * @return next id that will be associated with the new object
	 */
	public static String getNextObjectType(DBHandle dbHandle) {
		try {
			// TODO use the first available number (as parameter to the function
			// call)
			SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

			DB.query(dbHandle, "SELECT max(type) as maxType FROM " + dbHandle.getContentObjectName(), resultProcessor);
			for (Iterator<SimpleResultRow> i = resultProcessor.iterator(); i.hasNext();) {
				SimpleResultRow row = i.next();
				long maxType = row.getLong("maxType");

				if (maxType > 0) {
					return Long.toString(maxType + 1);
				} else {
					return "1";
				}
			}
		} catch (SQLException e) {
			logger.error("error while computing next free object type", e);
		}
		return null;
	}

	/**
	 * Save the given objecttype into the database for the given datasource
	 * Attributes are not saved
	 * @param datasource datasource to change
	 * @param objectType object type to save/update
	 * @return true when the operation succeeded, false if not
	 * @throws ObjectManagementException when structural changes are needed
	 */
	public static boolean saveObjectType(CNDatasource datasource, ObjectTypeBean objectType) throws ObjectManagementException {
		return saveObjectType(datasource, objectType, false, false, false);
	}

	/**
	 * Save the given objecttype into the database for the given datasource
	 * 
	 * @param datasource datasource to change
	 * @param objectType object type to save/update
	 * @param saveAttributes true when also the attributes of the objecttypes shall be saved
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 * @return true when the operation succeeded, false if not
	 * @throws ObjectManagementException when structural changes are needed but forceStructureChange is false
	 */
	public static boolean saveObjectType(CNDatasource datasource, ObjectTypeBean objectType, boolean saveAttributes,
			boolean forceStructureChange) throws ObjectManagementException {
		return saveObjectType(datasource, objectType, saveAttributes, forceStructureChange, false);
	}

	/**
	 * Save the given objecttype into the database for the given datasource
	 * 
	 * @param datasource datasource to change
	 * @param objectType object type to save/update
	 * @param saveAttributes true when also the attributes of the objecttypes shall be saved
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 * @param ignoreOptimized true when optimized flags shall be ignored for all contentattributetypes, false if not
	 * @return true when the operation succeeded, false if not
	 * @throws ObjectManagementException when structural changes are needed but forceStructureChange is false
	 */
	public static boolean saveObjectType(CNDatasource datasource, ObjectTypeBean objectType, boolean saveAttributes,
			boolean forceStructureChange, boolean ignoreOptimized) throws ObjectManagementException {
		try {
			DBHandle dbHandle = datasource.getHandle().getDBHandle();
			// check whether the excludeVersioning information can be stored into the datasource
			boolean excludeVersioning = false;

			try {
				excludeVersioning = DatatypeHelper.isObjectExcludeVersioningColumn(dbHandle);
			} catch (Exception ex) {
				logger.error("Error while checking for versioning exclusion");
			}

			if (objectType.getOldType() == null) {
				if (objectType.getType() == null) {
					String nextType = getNextObjectType(dbHandle);

					if (nextType != null) {
						objectType.setType(new Integer(nextType));
						objectType.setOldType(new Integer(nextType));
					} else {
						return false;
					}
				}
				if (excludeVersioning) {
					DB.update(dbHandle,
							"INSERT INTO " + dbHandle.getContentObjectName() + " (name, type, " + DatatypeHelper.EXCLUDE_VERSIONING_FIELD + ") values(?, ?, ?)",
							new Object[] { objectType.getName(), objectType.getType(), Boolean.valueOf(objectType.isExcludeVersioning()) });
				} else {
					DB.update(dbHandle, "INSERT INTO " + dbHandle.getContentObjectName() + " (name, type) values(?, ?)",
							new Object[] { objectType.getName(), objectType.getType() });
				}
			} else {
				if (excludeVersioning) {
					// check whether the versioning needs to be reset
					boolean resetVersioning = false;

					try {
						resetVersioning = objectType.isExcludeVersioning()
								&& !DatatypeHelper.isObjecttypeExcludeVersioning(dbHandle, objectType.getOldType().intValue());
					} catch (CMSUnavailableException e) {
						logger.error("Error while checking for versioning exclusion");
					}

					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentObjectName() + " set name = ?, type = ?, " + DatatypeHelper.EXCLUDE_VERSIONING_FIELD
							+ " = ? where type = ?",
							new Object[] { objectType.getName(), objectType.getType(), Boolean.valueOf(objectType.isExcludeVersioning()), objectType.getOldType() });
					if (resetVersioning) {
						try {
							unversionObjectType(dbHandle, objectType);
						} catch (Exception e) {
							logger.error("Error while reseting versioning data for objecttype {" + objectType.getType() + "}", e);
						}
					}
				} else {
					DB.update(dbHandle, "UPDATE " + dbHandle.getContentObjectName() + " set name = ?, type = ? where type = ?",
							new Object[] { objectType.getName(), objectType.getType(), objectType.getOldType() });
				}
			}

			boolean success = true;

			if (saveAttributes) {
				ObjectAttributeBean[] attributes = objectType.getAttributeTypes();

				for (int i = 0; i < attributes.length && success; i++) {
					success = saveAttributeType(datasource, attributes[i], forceStructureChange, ignoreOptimized);
				}
			}

			DatatypeHelper.clear();
			return success;
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while saving object", ex);
			return false;
		} catch (CMSUnavailableException ex) {
			throw new ObjectManagementException(ex);
		}
	}

	/**
	 * Save the given object type for the datasource
	 * @param ds datasource
	 * @param objectType object type
	 * @param saveAttributes true to also save the assigned attribute types
	 * @param forceStructureChange true to force structure changes
	 * @param ignoreOptimized true to ignore the optimized flag
	 * @return true if saving succeeded, false if not
	 * @throws ObjectManagementException
	 */
	public static boolean save(Datasource ds, ObjectTypeBean objectType, boolean saveAttributes,
			boolean forceStructureChange, boolean ignoreOptimized) throws ObjectManagementException {
		if (ds instanceof CNDatasource) {
			return saveObjectType((CNDatasource) ds, objectType, saveAttributes, forceStructureChange, ignoreOptimized);
		} else if (ds instanceof MCCRDatasource) {
			saveObjectType((MCCRDatasource) ds, objectType, saveAttributes, forceStructureChange, ignoreOptimized);
			((MCCRDatasource) ds).clearCaches();
			return true;
		} else {
			if (ds == null) {
				throw new ObjectManagementException("Cannot save objecttype for null datasource");
			} else {
				throw new ObjectManagementException("Cannot save objecttype for datasource " + ds.getClass());
			}
		}
	}

	/**
	 * Delete the given object type
	 * @param ds datasource
	 * @param objectType object type
	 * @param forceStructureChange true to force structure changes
	 * @throws ObjectManagementException
	 */
	public static void delete(Datasource ds, ObjectTypeBean objectType, boolean forceStructureChange) throws ObjectManagementException {
		if (ds instanceof CNDatasource) {
			try {
				deleteObjectType(((CNDatasource) ds).getHandle().getDBHandle(), objectType, forceStructureChange);
			} catch (CMSUnavailableException e) {
				throw new ObjectManagementException(e);
			}
		} else if (ds instanceof MCCRDatasource) {
			try {
				MCCRDatasource mccrDs = (MCCRDatasource) ds;
				DBHandle handle = mccrDs.getHandle();

				DB.update(handle, "DELETE FROM " + handle.getContentObjectName() + " WHERE type = ?", new Object[] { objectType.getType()});
			} catch (Exception e) {
				throw new ObjectManagementException("Error while deleting object type " + objectType, e);
			}
		} else {
			if (ds == null) {
				throw new ObjectManagementException("Cannot save objecttype for null datasource");
			} else {
				throw new ObjectManagementException("Cannot save objecttype for datasource " + ds.getClass());
			}
		}
	}

	/**
	 * Get a collection of conflicting objecttypes when the given object type
	 * would be saved.
	 * @param dbHandle database handle
	 * @param objectType object type
	 * @return collection of conflicting object types (empty if no conflict) or
	 *         null in case of an error
	 */
	public static Collection<ObjectTypeBean> getConflictingObjectTypes(DBHandle dbHandle, ObjectTypeBean objectType) {
		Collection<ObjectTypeBean> conflictingObjectType = new Vector<ObjectTypeBean>();
		SimpleResultProcessor result = new SimpleResultProcessor();

		try {
			if (objectType.getOldType() == null) {
				// the object type is new and shall be created
				if (objectType.getType() != null) {
					// an object type was requested, check for existing
					// objecttypes with this objecttype
					DB.query(dbHandle, "SELECT * from " + dbHandle.getContentObjectName() + " where type = ?", new Object[] { objectType.getType() }, result);
					if (result.size() > 0) {
						for (int i = 0; i < result.size(); ++i) {
							SimpleResultRow row = result.getRow(i + 1);

							conflictingObjectType.add(
									new ObjectTypeBean(new Integer(row.getString("type")), row.getString("name"),
									row.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD)));
						}
					}
				}
			} else if (!objectType.getOldType().equals(objectType.getType())) {
				// the object type is modified
				DB.query(dbHandle, "SELECT * from " + dbHandle.getContentObjectName() + " where type = ?", new Object[] { objectType.getType() }, result);
				if (result.size() > 0) {
					for (int i = 0; i < result.size(); ++i) {
						SimpleResultRow row = result.getRow(i + 1);

						conflictingObjectType.add(
								new ObjectTypeBean(new Integer(row.getString("type")), row.getString("name"), row.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD)));
					}
				}
			}
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while saving object", ex);
			return null;
		}

		return conflictingObjectType;
	}

	/**
	 * Get a collection of attributes linking to the given objecttype.
	 * @param dbHandle db handle
	 * @param objectType object type
	 * @return Collection of linking attributes, or null when an error occurred
	 */
	public static Collection<ObjectAttributeBean> getLinkingAttributes(DBHandle dbHandle, ObjectTypeBean objectType) {
		Collection<ObjectAttributeBean> linkingAttributes = new Vector<ObjectAttributeBean>();

		try {
			SimpleResultProcessor result = new SimpleResultProcessor();

			DB.query(dbHandle, "SELECT * FROM " + dbHandle.getContentAttributeTypeName() + " WHERE (objecttype != ?) AND (linkedobjecttype = ?)",
					new Object[] { objectType.getType(), objectType.getType() }, result);
			if (result.size() > 0) {
				for (int i = 0; i < result.size(); ++i) {
					SimpleResultRow row = result.getRow(i + 1);

					linkingAttributes.add(
							new ObjectAttributeBean(row.getString("name"), row.getInt("attributetype"), row.getBoolean("optimized"), row.getString("quickname"),
							row.getBoolean("multivalue"), row.getInt("objecttype"), row.getInt("linkedobjecttype"), "", row.getString("foreignlinkattribute"),
							row.getString("foreignlinkattributerule"), row.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD),
							row.getBoolean(DatatypeHelper.FILESYSTEM_FIELD)));
				}
			}
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while doing pre-save check for attributetype", ex);
			return null;
		}

		return linkingAttributes;
	}

	/**
	 * Check whether the objectattribute is valid or not (its name)
	 * 
	 * @param objectAttribute object attribute to check
	 * @return true when the attribute is valid, false if not
	 */
	public static boolean isAttributeValid(ObjectAttributeBean objectAttribute) {
		if (objectAttribute == null) {
			return false;
		} else {
			return !StringUtils.isEmpty(objectAttribute.getName()) && !PROTECTED_ATTRIBUTENAMES.contains(objectAttribute.getName());
		}
	}

	/**
	 * Get an array of conflicting attributes when the given attribute would be
	 * saved.
	 * 
	 * @param dbHandle db handle
	 * @param objectAttribute object attribute that is about to be saved
	 * @param checkType type of the check.
	 * @return Collection of conflicting attributes, or null when an error
	 *         occurred
	 */
	public static Collection<ObjectAttributeBean> getConflictingAttributes(DBHandle dbHandle,
			ObjectAttributeBean objectAttribute, int checkType) {
		Collection<ObjectAttributeBean> conflictingAttributes = new Vector<ObjectAttributeBean>();

		try {
			SimpleResultProcessor result = new SimpleResultProcessor();

			switch (checkType) {
			case ATTRIBUTECHECK_NAME:
				String oldName = objectAttribute.getOldname();
				String newName = objectAttribute.getName();

				if (oldName == null || oldName.length() == 0 || !oldName.equals(newName)) {
					// when the attribute shall be created, get attributes with
					// the same
					// name and same objecttype
					DB.query(dbHandle, "SELECT * FROM " + dbHandle.getContentAttributeTypeName() + " WHERE (name = ? AND objecttype = ?)",
							new Object[] { objectAttribute.getName(), new Integer(objectAttribute.getObjecttype()) }, result);
					if (result.size() > 0) {
						for (int i = 0; i < result.size(); ++i) {
							SimpleResultRow row = result.getRow(i + 1);

							conflictingAttributes.add(
									new ObjectAttributeBean(row.getString("name"), row.getInt("attributetype"), row.getBoolean("optimized"),
									row.getString("quickname"), row.getBoolean("multivalue"), row.getInt("objecttype"), row.getInt("linkedobjecttype"), "",
									row.getString("foreignlinkattribute"), row.getString("foreignlinkattributerule"),
									row.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD), row.getBoolean(DatatypeHelper.FILESYSTEM_FIELD)));
						}
					}
				}
				break;

			case ATTRIBUTECHECK_TYPE:
				// do a consistency check: get other attributes with the same
				// name
				// but different configuration
				// boolean excludeVersioning = false;
				// try {
				// excludeVersioning =
				// DatatypeHelper.isAttributeExcludeVersioningColumn(dbHandle);
				// } catch (Exception ex) {
				// logger.error("Error while checking for versioning exclusion");
				// }

				DB.query(dbHandle,
						"SELECT * FROM " + dbHandle.getContentAttributeTypeName() + " WHERE (name = ? AND objecttype != ?) AND (" + "attributetype != ? OR "
						+ "optimized != ? OR " + "quickname != ? OR " + "multivalue != ? OR " + "linkedobjecttype != ? OR " + "foreignlinkattribute != ? " + "OR "
						+ "foreignlinkattributerule NOT LIKE ?" + ")",
						new Object[] {
					objectAttribute.getName(), new Integer(objectAttribute.getObjecttype()), new Integer(objectAttribute.getAttributetype()),
					Boolean.valueOf(objectAttribute.getOptimized()), objectAttribute.getQuickname(), Boolean.valueOf(objectAttribute.getMultivalue()),
					new Integer(objectAttribute.getLinkedobjecttype()), objectAttribute.getForeignlinkattribute(), objectAttribute.getForeignlinkattributerule() },
						result);
				if (result.size() > 0) {
					for (int i = 0; i < result.size(); ++i) {
						SimpleResultRow row = result.getRow(i + 1);

						conflictingAttributes.add(
								new ObjectAttributeBean(row.getString("name"), row.getInt("attributetype"), row.getBoolean("optimized"), row.getString("quickname"),
								row.getBoolean("multivalue"), row.getInt("objecttype"), row.getInt("linkedobjecttype"), "", row.getString("foreignlinkattribute"),
								row.getString("foreignlinkattributerule"), row.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD),
								row.getBoolean(DatatypeHelper.FILESYSTEM_FIELD)));
					}
				}
				break;
			}

		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while doing pre-save check for attributetype", ex);
			return null;
		}
		return conflictingAttributes;
	}

	/**
	 * Save the given objecttype into the database for the given datasource
	 *
	 * @param datasource datasource to change
	 * @param objectAttribute object attribute to save/update
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 * @return true when the operation succeeded, false if not
	 */
	public static boolean saveAttributeType(CNDatasource datasource,
			ObjectAttributeBean objectAttribute, boolean forceStructureChange) throws ObjectManagementException {
		return saveAttributeType(datasource, objectAttribute, forceStructureChange, false);
	}

	/**
	 * Save the given objecttype into the database for the given datasource
	 *
	 * @param datasource datasource to change
	 * @param objectAttribute object attribute to save/update
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 * @param ignoreOptimized true when optimized flag shall be ignored for the attributetype, false if not
	 * @return true when the operation succeeded, false if not
	 */
	public static boolean saveAttributeType(CNDatasource datasource, ObjectAttributeBean objectAttribute,
			boolean forceStructureChange, boolean ignoreOptimized) throws ObjectManagementException {
		try {
			DBHandle dbHandle = datasource.getHandle().getDBHandle();

			objectAttribute.normalizedAttribute();
			objectAttribute.checkConsistency();

			boolean excludeVersioning = false;
			boolean filesystemColumn = false;

			try {
				excludeVersioning = DatatypeHelper.isAttributeExcludeVersioningColumn(dbHandle);
				filesystemColumn = DatatypeHelper.isAttributeFilesystemColumn(dbHandle);
			} catch (Exception ex) {
				logger.error("Error while checking for versioning exclusion");
			}

			AttributeTypeResultProcessor result = new AttributeTypeResultProcessor(false);

			// first check whether there already exists an attribute with the
			// name and objecttype
			DB.query(dbHandle,
					"select " + dbHandle.getContentAttributeTypeName() + ".*, " + dbHandle.getContentObjectName() + ".name as linkedobjecttypetext from "
					+ dbHandle.getContentAttributeTypeName() + " left join " + dbHandle.getContentObjectName() + " on " + dbHandle.getContentAttributeTypeName()
					+ ".linkedobjecttype = " + dbHandle.getContentObjectName() + ".type WHERE " + dbHandle.getContentAttributeTypeName() + ".objecttype = ? AND "
					+ dbHandle.getContentAttributeTypeName() + ".name = ?",
					new Object[] { new Integer(objectAttribute.getObjecttype()), objectAttribute.getOldname() },
					result);
			if (result.getAttributeTypes().size() > 0) {
				// there already exist a contentattributetype, so do an update
				if (excludeVersioning) {
					boolean resetVersioning = false;

					try {
						resetVersioning = objectAttribute.isExcludeVersioning()
								&& !DatatypeHelper.isAttributeExcludeVersioning(dbHandle, objectAttribute.getObjecttype(), objectAttribute.getOldname());
					} catch (Exception e) {
						logger.error("Error while checking for versioning exclusion");
					}

					// set up the update parameters, depending on whether the
					// optimized flag shall be ignored or not
					Object[] updateParams = null;

					if (ignoreOptimized) {
						updateParams = new Object[] {
							objectAttribute.getName(), new Integer(objectAttribute.getAttributetype()),
							Boolean.valueOf(objectAttribute.getMultivalue()), new Integer(objectAttribute.getLinkedobjecttype()),
							objectAttribute.getForeignlinkattribute(), objectAttribute.getForeignlinkattributerule(),
							Boolean.valueOf(objectAttribute.isExcludeVersioning()), new Integer(objectAttribute.getObjecttype()), objectAttribute.getOldname() };

					} else {
						updateParams = new Object[] {
							objectAttribute.getName(), new Integer(objectAttribute.getAttributetype()),
							Boolean.valueOf(objectAttribute.getOptimized()), objectAttribute.getQuickname(), Boolean.valueOf(objectAttribute.getMultivalue()),
							new Integer(objectAttribute.getLinkedobjecttype()), objectAttribute.getForeignlinkattribute(),
							objectAttribute.getForeignlinkattributerule(), Boolean.valueOf(objectAttribute.isExcludeVersioning()),
							new Integer(objectAttribute.getObjecttype()), objectAttribute.getOldname() };
					}

					// do the update
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET name = ?, attributetype = ?, "
							+ (!ignoreOptimized ? "optimized = ?, quickname = ?, " : "")
							+ "multivalue = ?, linkedobjecttype = ?, foreignlinkattribute = ?, foreignlinkattributerule = ?, "
							+ DatatypeHelper.EXCLUDE_VERSIONING_FIELD + " = ? WHERE objecttype = ? AND name = ?",
							updateParams);
					if (resetVersioning) {
						try {
							unversionAttributeType(dbHandle, objectAttribute);
						} catch (Exception e) {
							logger.error("Error while reseting versioning data for objecttype {" + objectAttribute.getName() + "}", e);
						}
					}
				} else {
					// set up the update parameters, depending on whether the
					// optimized flag shall be ignored or not
					Object[] updateParams = null;

					if (ignoreOptimized) {
						updateParams = new Object[] {
							objectAttribute.getName(), new Integer(objectAttribute.getAttributetype()),
							Boolean.valueOf(objectAttribute.getMultivalue()), new Integer(objectAttribute.getLinkedobjecttype()),
							objectAttribute.getForeignlinkattribute(), objectAttribute.getForeignlinkattributerule(),
							new Integer(objectAttribute.getObjecttype()), objectAttribute.getOldname() };
					} else {
						updateParams = new Object[] {
							objectAttribute.getName(), new Integer(objectAttribute.getAttributetype()),
							Boolean.valueOf(objectAttribute.getOptimized()), objectAttribute.getQuickname(), Boolean.valueOf(objectAttribute.getMultivalue()),
							new Integer(objectAttribute.getLinkedobjecttype()), objectAttribute.getForeignlinkattribute(),
							objectAttribute.getForeignlinkattributerule(), new Integer(objectAttribute.getObjecttype()), objectAttribute.getOldname() };
					}

					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET name = ?, attributetype = ?, "
							+ (!ignoreOptimized ? "optimized = ?, quickname = ?, " : "")
							+ "multivalue = ?, linkedobjecttype = ?, foreignlinkattribute = ?, foreignlinkattributerule = ? WHERE objecttype = ? AND name = ?",
							updateParams);
				}

				ObjectAttributeBean oldAttribute = (ObjectAttributeBean) result.getAttributeTypes().get(0);

				// if the filesystem column exists, update the attribute type
				if (filesystemColumn) {
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET " + DatatypeHelper.FILESYSTEM_FIELD + " = ?"
							+ " WHERE name = ? AND objecttype = ?",
							new Object[] { objectAttribute.isFilesystem(), objectAttribute.getName(), objectAttribute.getObjecttype() });

					// if filesystem flag has changed
					if (oldAttribute.isFilesystem() != objectAttribute.isFilesystem()) {
						// get the attribute base path
						String basepath = datasource.getAttributePath();

						if (ObjectTransformer.isEmpty(basepath)) {
							throw new ObjectManagementException(
									"Error while saving attribute type " + objectAttribute.getName()
									+ ": attribute has or had filesystem flag set, but the basepath is not set for the datasource");
						}
						SimpleResultProcessor queryResult = new SimpleResultProcessor();

						// set column to get data from or write data to
						final String dataColumn = DatatypeHelper.getTypeColumn(objectAttribute.getAttributetype());
						final boolean binary = objectAttribute.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_BLOB;

						// move from filesystem to DB
						if (oldAttribute.isFilesystem() && !objectAttribute.isFilesystem()) {

							// get rows to update
							DB.query(dbHandle, "SELECT * FROM " + dbHandle.getContentAttributeName() + " WHERE name = ? AND contentid LIKE ?",
									new Object[] { objectAttribute.getName(), objectAttribute.getObjecttype() + ".%" }, queryResult);

							// prepare statements for updating
							String updateSingleValueClob = "UPDATE " + dbHandle.getContentAttributeName() + " SET value_text = NULL, value_long = NULL, "
									+ dataColumn + " = ? WHERE name = ? AND contentid = ?";
							String updateMultiValueClob = "UPDATE " + dbHandle.getContentAttributeName() + " SET value_text = NULL, value_long = NULL, "
									+ dataColumn + " = ? WHERE name = ? AND contentid = ? AND sortorder = ?";
							String updateSingleValueBlob = "UPDATE " + dbHandle.getContentAttributeName()
									+ " SET value_text = NULL, value_long = NULL, value_clob = NULL, " + dataColumn + " = ? WHERE name = ? AND contentid = ?";
							String updateMultiValueBlob = "UPDATE " + dbHandle.getContentAttributeName()
									+ " SET value_text = NULL, value_long = NULL, value_clob = NULL, " + dataColumn
									+ " = ? WHERE name = ? AND contentid = ? AND sortorder = ?";

							for (SimpleResultRow row : queryResult) {
								String path = row.getString("value_text");
								String contentid = row.getString("contentid");
								int sortorder = row.getInt("sortorder");

								File file = new File(basepath, path);

								try {

									if (file.exists()) {

										if (dataColumn.equals("value_clob")) {
											FileInputStream in = new FileInputStream(file);
											String content = null;

											try {
												content = StringUtils.readStream(in, "UTF-8");
											} finally {
												FileUtil.close(in);
											}

											if (sortorder > 0) {
												DB.update(dbHandle, updateMultiValueClob, new Object[] {
													content, objectAttribute.getName(), contentid, sortorder });
											} else {
												DB.update(dbHandle, updateSingleValueClob, new Object[] { content, objectAttribute.getName(), contentid });
											}

										} else {
											ByteArrayOutputStream out = new ByteArrayOutputStream();
											FileInputStream in = new FileInputStream(file);

											try {
												FileUtil.pooledBufferInToOut(in, out);
											} finally {
												FileUtil.close(in);
											}
											byte[] content = out.toByteArray();

											if (sortorder > 0) {
												DB.update(dbHandle, updateMultiValueBlob, new Object[] {
													content, objectAttribute.getName(), contentid, sortorder });
											} else {
												DB.update(dbHandle, updateSingleValueBlob, new Object[] { content, objectAttribute.getName(), contentid });
											}
										}

										DB.removeFileOnCommit(dbHandle, file);

									} else {
										throw new IOException("File " + file.getAbsolutePath() + " does not exist.");
									}

								} catch (IOException e) {

									if (path != null) {
										logger.error("Unable to read from source file: " + basepath + path, e);
									}

									DB.update(dbHandle,
											"UPDATE " + dbHandle.getContentAttributeName()
											+ " SET value_text = NULL, value_clob = NULL, value_long = NULL WHERE name = ? AND contentid = ?",
											new Object[] { objectAttribute.getName(), contentid });
								}

							}

							// move from DB to filesystem
						} else if (!oldAttribute.isFilesystem() && objectAttribute.isFilesystem()) {

							try {
								final DBHandle finalHandle = dbHandle;
								final String finalBase = basepath;

								DB.query(dbHandle,
										"SELECT name, contentid, sortorder FROM " + dbHandle.getContentAttributeName() + " WHERE name = ? AND contentid LIKE ?",
										new Object[] { objectAttribute.getName(), objectAttribute.getObjecttype() + ".%"}, queryResult);

								// prepare the statements for selecting the data
								String selectSingleValue = "SELECT " + dataColumn + " FROM " + dbHandle.getContentAttributeName()
										+ " WHERE name = ? AND contentid = ? AND sortorder IS NULL";
								String selectMultiValue = "SELECT " + dataColumn + " FROM " + dbHandle.getContentAttributeName()
										+ " WHERE name = ? AND contentid = ? AND sortorder = ?";

								// prepare the statements for updating the data
								String updateSingleValue = "UPDATE " + finalHandle.getContentAttributeName()
										+ " SET value_text = ?, value_clob = ?, value_long = ?, value_blob = NULL WHERE name = ? AND contentid = ?";
								String updateMultiValue = "UPDATE " + finalHandle.getContentAttributeName()
										+ " SET value_text = ?, value_clob = ?, value_long = ?, value_blob = NULL WHERE name = ? AND contentid = ? AND sortorder = ?";

								for (SimpleResultRow row : queryResult) {
									final String contentid = row.getString("contentid");
									final String name = row.getString("name");
									int sortorder = row.getInt("sortorder");

									// generate storage path
									String path = GenticsContentFactory.getStoragePath(contentid, name, sortorder, 1);
									final File newAttributefile = new File(finalBase, path);
									final FilesystemAttributeValue value = new FilesystemAttributeValue(null, null, 0);

									String sql = null;
									Object[] data = null;

									if (sortorder == 0) {
										sql = selectSingleValue;
										data = new Object[] { name, contentid};
									} else {
										sql = selectMultiValue;
										data = new Object[] { name, contentid, sortorder};
									}

									DB.query(dbHandle, sql, data,
											new ResultProcessor() {
										public void takeOver(ResultProcessor p) {}
												
										public void process(ResultSet rs) throws SQLException {
											if (rs.next()) {
												try {
													InputStream in = binary
															? rs.getBinaryStream(dataColumn)
															: new ByteArrayInputStream(rs.getString(dataColumn).getBytes("UTF-8"));

													if (in != null) {
														// if file does not already exist (it shouldn't), create necessary file structure
														if (!newAttributefile.exists()) {
															String parent = newAttributefile.getParent();
															File parentDir = new File(parent);

															parentDir.mkdirs();
															newAttributefile.createNewFile();
														}
																
														MD5InputStream md5Stream = new MD5InputStream(in);
														CountingInputStream countingStream = new CountingInputStream(md5Stream);
														FileOutputStream out = new FileOutputStream(newAttributefile);

														try {
															FileUtil.pooledBufferInToOut(countingStream, out);
															value.setMD5(MD5.asHex(md5Stream.hash()).toLowerCase());
															value.setLength(countingStream.getByteCount());
														} finally {
															FileUtil.close(out);
															FileUtil.close(in);
														}
													}
												} catch (Exception e) {
													throw new SQLException(
															"Error while migrating attribute " + name + " for " + contentid + ":" + e.getLocalizedMessage());
												}
											}
										}
									});

									if (sortorder > 0) {
										DB.update(finalHandle, updateMultiValue,
												new Object[] { path, value.getMD5(), value.getLength(), name, contentid, sortorder });
									} else {
										DB.update(finalHandle, updateSingleValue, new Object[] { path, value.getMD5(), value.getLength(), name, contentid});
									}
								}
							} catch (Exception e) {
								logger.error("Unable to write to file system", e);
							}
						}
					}
				}

				if (oldAttribute.getMultivalue() && !objectAttribute.getMultivalue()) {// changed from a multivalue attribute to a singlevalue
					// attribute, so remove all multivalues with sortorder >= 2
					// TODO check whether this shall really be done here
					// DB
					// .update(
					// dbHandle,
					// "DELETE FROM "
					// + dbHandle.getContentAttributeName()
					// +
					// " WHERE name = ? AND contentid LIKE ? AND sortorder IS NOT NULL AND sortorder > ?",
					// new Object[] {objectAttribute.getName(),
					// objectAttribute.getObjecttype() + ".%",
					// new Integer(1)});
				}

				if (!oldAttribute.getMultivalue() && objectAttribute.getMultivalue()) {
					// changed from singlevalue to multivalue, so set the
					// sortorder (to 1) where missing)
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeName() + " SET sortorder = 1 WHERE sortorder IS NULL AND name = ? AND contentid LIKE ?",
							new Object[] { objectAttribute.getName(), objectAttribute.getObjecttype() + ".%" });
				}

				// synchronize optimized columns (if they are not ignored) when
				// something has changed
				if (!ignoreOptimized
						&& (oldAttribute.getOptimized() != objectAttribute.getOptimized() || !oldAttribute.getName().equals(objectAttribute.getName())
						|| !DatatypeHelper.getTypeColumn(oldAttribute.getAttributetype()).equals(DatatypeHelper.getTypeColumn(objectAttribute.getAttributetype())))) {
					String productName = DB.getDatabaseProductName(dbHandle);

					syncOptimizedColumn(dbHandle, productName, objectAttribute.getName(), forceStructureChange, false);
					if (!StringUtils.isEqual(oldAttribute.getName(), objectAttribute.getName())) {
						syncOptimizedColumn(dbHandle, productName, oldAttribute.getName(), forceStructureChange, false);
					}
				}
			} else {
				// this contentattributetype does not exist, so make an insert
				if (excludeVersioning) {
					DB.update(dbHandle,
							"INSERT INTO " + dbHandle.getContentAttributeTypeName()
							+ " (name, objecttype, attributetype, optimized, quickname, multivalue, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, "
							+ DatatypeHelper.EXCLUDE_VERSIONING_FIELD + ") VALUES (?,?,?,?,?,?,?,?,?,?)",
							new Object[] {
						objectAttribute.getName(), new Integer(objectAttribute.getObjecttype()), new Integer(objectAttribute.getAttributetype()),
						Boolean.valueOf(ignoreOptimized ? false : objectAttribute.getOptimized()), ignoreOptimized ? null : objectAttribute.getQuickname(),
						Boolean.valueOf(objectAttribute.getMultivalue()), new Integer(objectAttribute.getLinkedobjecttype()),
						objectAttribute.getForeignlinkattribute(), objectAttribute.getForeignlinkattributerule(),
						Boolean.valueOf(objectAttribute.isExcludeVersioning()) });
				} else {
					DB.update(dbHandle,
							"INSERT INTO " + dbHandle.getContentAttributeTypeName()
							+ " (name, objecttype, attributetype, optimized, quickname, multivalue, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule) VALUES (?,?,?,?,?,?,?,?,?)",
							new Object[] {
						objectAttribute.getName(), new Integer(objectAttribute.getObjecttype()), new Integer(objectAttribute.getAttributetype()),
						Boolean.valueOf(ignoreOptimized ? false : objectAttribute.getOptimized()), ignoreOptimized ? null : objectAttribute.getQuickname(),
						Boolean.valueOf(objectAttribute.getMultivalue()), new Integer(objectAttribute.getLinkedobjecttype()),
						objectAttribute.getForeignlinkattribute(), objectAttribute.getForeignlinkattributerule() });
				}

				// if the filesystem column exists, update the attribute type
				if (filesystemColumn) {
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET " + DatatypeHelper.FILESYSTEM_FIELD + " = ?"
							+ " WHERE name = ? AND objecttype = ?",
							new Object[] { objectAttribute.isFilesystem(), objectAttribute.getName(), objectAttribute.getObjecttype() });
				}

				// create the optimized column, when optimized is true
				if (!ignoreOptimized && objectAttribute.getOptimized()) {
					syncOptimizedColumn(dbHandle, DB.getDatabaseProductName(dbHandle), objectAttribute.getName(), forceStructureChange, false);
				}
			}
			DatatypeHelper.clear();
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while saving object", ex);
			return false;
		} catch (CMSUnavailableException e) {
			throw new ObjectManagementException(e);
		}

		return true;
	}

	/**
	 * Get name of the index for the quick column
	 * @param quickColumnName name of the quick column
	 * @return name of the index
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static String getIndexName(String quickColumnName) throws NoSuchAlgorithmException,
				UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("MD5");

		md.update(quickColumnName.getBytes("utf8"));
		return "idxq" + ObjectTransformer.encodeBinary(md.digest()).substring(0, 16);
	}

	/**
	 * synchronize the optimized column for the attributes with given name
	 * @param dbHandle database handle
	 * @param productName database product name
	 * @param attributeName attribute name
	 * @param forceStructureChange true to force structure changes
	 * @param multichannelling true for multichannelling
	 * @throws SQLException
	 * @throws ObjectManagementException when db structure changes would be
	 *         needed, but forceStructureChange is set to false
	 */
	protected static void syncOptimizedColumn(DBHandle dbHandle, String productName, String attributeName,
			boolean forceStructureChange, boolean multichannelling) throws SQLException, ObjectManagementException {
		SimpleResultProcessor result = new SimpleResultProcessor();
		String typeColumn = multichannelling ? "type" : "attributetype";

		DB.query(dbHandle, "SELECT DISTINCT optimized, quickname, " + typeColumn + " FROM " + dbHandle.getContentAttributeTypeName() + " WHERE name = ?",
				new Object[] { attributeName }, result);
		if (result.size() > 0) {
			SimpleResultRow row = (SimpleResultRow) result.iterator().next();

			if (row.getBoolean("optimized")) {
				ColumnDefinition colDef;

				try {
					colDef = DatatypeHelper.getQuickColumnDefinition(dbHandle, productName, row.getInt(typeColumn), row.getString("quickname"));
				} catch (NodeIllegalArgumentException e) {
					throw new ObjectManagementException("Error while retrieving column definition for {" + row.getString("quickname") + "}", e);
				}

				// create the quick column, if it does not yet exist, or if the definition is incorrect
				boolean fieldExists = DB.fieldExists(dbHandle, colDef.getTableName(), colDef.getColumnName());
				boolean colDefValid = fieldExists && DB.checkColumn(dbHandle, colDef);

				if (!fieldExists || !colDefValid) {
					if (!forceStructureChange) {
						throw new ObjectManagementException(
								"Not allowed to create missing optimized column {" + colDef.getColumnName() + "} in table {" + colDef.getTableName()
								+ "} for attribute {" + attributeName + "}.");
					}

					// check whether a _nodeversion table exists
					String nodeversionTable = colDef.getTableName() + "_nodeversion";
					boolean nodeversion = DB.tableExists(dbHandle, nodeversionTable);

					// if the column type changed, just drop the column and
					// recreate it
					if (fieldExists) {
						try {
							String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, colDef.getTableName(),
									getIndexName(colDef.getColumnName()));

							DB.update(dbHandle, dropIndexStatement);
						} catch (Exception e) {
							logger.error("Error while trying to drop index for column {" + colDef.getColumnName() + "}", e);
						}
						String[] dropStatements = colDef.getDropStatements(productName);

						for (String drop : dropStatements) {
							DB.update(dbHandle, drop);
						}
						if (nodeversion) {
							try {
								String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, nodeversionTable,
										getIndexName(colDef.getColumnName()));

								DB.update(dbHandle, dropIndexStatement);
							} catch (Exception e) {
								logger.error("Error while trying to drop index for column {" + colDef.getColumnName() + "} in table {" + nodeversionTable + "}",
										e);
							}
							dropStatements = colDef.getDropStatements(productName, nodeversionTable);
							for (String drop : dropStatements) {
								DB.update(dbHandle, drop);
							}
						}
					}

					// create the column in the normal table
					DB.update(dbHandle, colDef.getCreateStatement(productName));

					if (nodeversion) {
						// create the column in the nodeversion table
						DB.update(dbHandle, colDef.getCreateStatement(productName, nodeversionTable));
					}

					// create index on optimized column
					try {
						String indexName = getIndexName(colDef.getColumnName());
						String indexCreation = null;
						boolean createIndexWithLength = (row.getInt(typeColumn) == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG
								|| row.getInt(typeColumn) == GenticsContentAttribute.ATTR_TYPE_BLOB);

						// if we are creating an index on text-long or blob we
						// must specify length
						if (createIndexWithLength) {
							indexCreation = DatatypeHelper.getDBSpecificIndexCreateStatement(productName, colDef.getTableName(), indexName,
									colDef.getColumnName(), 255);
						} else {
							indexCreation = DatatypeHelper.getDBSpecificIndexCreateStatement(productName, colDef.getTableName(), indexName,
									colDef.getColumnName());
						}
						try {
							DB.update(dbHandle, indexCreation);
						} catch (SQLException e) {
							logger.error("Error while creating index by statement {" + indexCreation + "}. Index must be generated manually!");
						}

						if (nodeversion) {
							// create the index in the nodeversion table
							// if we are creating an index on text-long or blob we must specify length
							if (createIndexWithLength) {
								indexCreation = DatatypeHelper.getDBSpecificIndexCreateStatement(productName, nodeversionTable, indexName,
										colDef.getColumnName(), 255);
							} else {
								indexCreation = DatatypeHelper.getDBSpecificIndexCreateStatement(productName, nodeversionTable, indexName,
										colDef.getColumnName());
							}
							try {
								DB.update(dbHandle, indexCreation);
							} catch (SQLException e) {
								logger.error("Error while creating index by statement {" + indexCreation + "}. Index must be generated manually!");
							}
						}
					} catch (NoSuchAlgorithmException e) {
						logger.warn("Could not create index on new quick column", e);
					} catch (UnsupportedEncodingException e) {
						logger.warn("Could not create index on new quick column", e);
					}

					// update values for the optimized column
					if (multichannelling) {
						DB.update(dbHandle,
								"UPDATE " + dbHandle.getContentMapName() + " SET " + colDef.getColumnName() + " = (SELECT "
								+ DatatypeHelper.getTypeColumn(row.getInt(typeColumn)) + " FROM " + dbHandle.getContentAttributeName() + " a WHERE a.map_id = "
								+ dbHandle.getContentMapName() + ".id AND a.name = ? AND (a.sortorder = ? OR a.sortorder IS NULL))",
								new Object[] { attributeName, new Integer(0) });

						// remove the attribute values from table contentattribute
						DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + " WHERE name = ?", new Object[] { attributeName });
					} else {
						DB.update(dbHandle,
								"UPDATE " + dbHandle.getContentMapName() + " SET " + colDef.getColumnName() + " = (SELECT "
								+ DatatypeHelper.getTypeColumn(row.getInt(typeColumn)) + " FROM " + dbHandle.getContentAttributeName() + " a WHERE a.contentid = "
								+ dbHandle.getContentMapName() + ".contentid AND a.name = ? AND (a.sortorder = ? OR a.sortorder IS NULL))",
								new Object[] { attributeName, new Integer(1) });
					}
				}
			} else {
				// remove the quick column, if it does exist
				ColumnDefinition colDef;

				try {
					colDef = DatatypeHelper.getQuickColumnDefinition(dbHandle, productName, row.getInt(typeColumn),
							ObjectAttributeBean.constructQuickColumnName(attributeName));
				} catch (NodeIllegalArgumentException e) {
					throw new ObjectManagementException("Error while retrieving quick column definition.", e);
				}
				if (DB.fieldExists(dbHandle, colDef.getTableName(), colDef.getColumnName())) {
					if (!forceStructureChange) {
						throw new ObjectManagementException(
								"Not allowed to drop unused column {" + colDef.getColumnName() + "} in table {" + colDef.getTableName() + "} for attribute {"
								+ attributeName + "}.");
					}

					// check whether a _nodeversion table exists
					String nodeversionTable = colDef.getTableName() + "_nodeversion";
					boolean nodeversion = DB.tableExists(dbHandle, nodeversionTable);

					try {
						if (multichannelling) {
							// mccr's don't store optimized attributes in the contentattribute table, so we need to move the data there

							// first remove any existing data (to prevent duplicates)
							DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + " WHERE name = ?", new Object[] {attributeName});
							
							// now move the data from the quick column into contentattribute
							DB.update(
									dbHandle,
									"INSERT INTO " + dbHandle.getContentAttributeName() + " (map_id, name, sortorder, "
											+ DatatypeHelper.getTypeColumn(row.getInt(typeColumn)) + ", updatetimestamp) SELECT id, ?, ?, "
											+ colDef.getColumnName() + ", updatetimestamp FROM " + dbHandle.getContentMapName(), new Object[] { attributeName, 0 });
						}

						String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, colDef.getTableName(),
								getIndexName(colDef.getColumnName()));

						DB.update(dbHandle, dropIndexStatement);
					} catch (Exception e) {
						logger.error("Error while trying to drop index for column {" + colDef.getColumnName() + "}", e);
					}
					String[] dropColumn = DatatypeHelper.getDBSpecificDropColumnStatements(productName, colDef.getTableName(), colDef.getColumnName());

					if (dropColumn != null) {
						for (int i = 0; i < dropColumn.length; i++) {
							DB.update(dbHandle, dropColumn[i]);
						}
					}

					if (nodeversion) {
						try {
							String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, nodeversionTable,
									getIndexName(colDef.getColumnName()));

							DB.update(dbHandle, dropIndexStatement);
						} catch (Exception e) {
							logger.error("Error while trying to drop index for column {" + nodeversionTable + "}", e);
						}

						// drop the column in the nodeversion table
						dropColumn = DatatypeHelper.getDBSpecificDropColumnStatements(productName, nodeversionTable, colDef.getColumnName());
						if (dropColumn != null) {
							for (int i = 0; i < dropColumn.length; i++) {
								DB.update(dbHandle, dropColumn[i]);
							}
						}
					}
				}
			}
		} else {
			// remove the quick column, if it does exist
			// the attributetype of the column definition is not relevant here
			// (because we want to remove the quick column anyway), so we set it
			// to 1 here
			ColumnDefinition colDef;

			try {
				colDef = DatatypeHelper.getQuickColumnDefinition(dbHandle, productName, 1, ObjectAttributeBean.constructQuickColumnName(attributeName));
			} catch (NodeIllegalArgumentException e) {
				throw new ObjectManagementException("Error while retreiving quick column definition.", e);
			}
			if (DB.fieldExists(dbHandle, colDef.getTableName(), colDef.getColumnName())) {
				if (!forceStructureChange) {
					throw new ObjectManagementException(
							"Not allowed to drop unused column {" + colDef.getColumnName() + "} in table {" + colDef.getTableName() + "} for attribute {"
							+ attributeName + "}.");
				}

				// check whether a _nodeversion table exists
				String nodeversionTable = colDef.getTableName() + "_nodeversion";
				boolean nodeversion = DB.tableExists(dbHandle, nodeversionTable);

				try {
					String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, colDef.getTableName(),
							getIndexName(colDef.getColumnName()));

					DB.update(dbHandle, dropIndexStatement);
				} catch (Exception e) {
					logger.error("Error while trying to drop index for column {" + colDef.getColumnName() + "}", e);
				}
				String[] dropColumn = DatatypeHelper.getDBSpecificDropColumnStatements(productName, colDef.getTableName(), colDef.getColumnName());

				if (dropColumn != null) {
					for (int i = 0; i < dropColumn.length; i++) {
						DB.update(dbHandle, dropColumn[i]);
					}
				}

				if (nodeversion) {
					try {
						String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, nodeversionTable,
								getIndexName(colDef.getColumnName()));

						DB.update(dbHandle, dropIndexStatement);
					} catch (Exception e) {
						logger.error("Error while trying to drop index for column {" + nodeversionTable + "}", e);
					}

					// drop the column in the nodeversion table
					dropColumn = DatatypeHelper.getDBSpecificDropColumnStatements(productName, nodeversionTable, colDef.getColumnName());
					if (dropColumn != null) {
						for (int i = 0; i < dropColumn.length; i++) {
							DB.update(dbHandle, dropColumn[i]);
						}
					}
				}
			}
		}
		DB.clearTableFieldCache();
	}

	/**
	 * @param dbHandle database handler
	 * @param type object type (id)
	 * @param name object name
	 */
	public static boolean createNewObject(DBHandle dbHandle, String type, String name) {
		try {
			DB.update(dbHandle, "INSERT INTO " + dbHandle.getContentObjectName() + " (type, name) values (?, ?)", new Object[] { new Integer(type), name });
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * delete the given list of object types
	 * 
	 * @param dbHandle dbhandle
	 * @param objectTypes array of object types to delete
	 * @param forceStructureChange whether structure changes (removal of quick
	 *        columns are allowed) or not
	 * @throws ObjectManagementException when structure changes are not allowed
	 *         but would be done
	 */
	public static void deleteObjectTypes(DBHandle dbHandle, ObjectTypeBean[] objectTypes, boolean forceStructureChange) throws ObjectManagementException {
		deleteObjectTypes(dbHandle, objectTypes, false, forceStructureChange);
	}

	/**
	 * delete the given list of object types
	 *
	 * @param dbHandle dbhandle
	 * @param objectTypes array of object types to delete
	 * @param deleteDependendAttributetypes whether dependend attributetypes
	 *        shall be removed or not
	 * @param forceStructureChange whether structure changes (removal of quick
	 *        columns are allowed) or not
	 * @throws ObjectManagementException when structure changes are not allowed
	 *         but would be done
	 */
	public static void deleteObjectTypes(DBHandle dbHandle, ObjectTypeBean[] objectTypes,
			boolean deleteDependendAttributetypes, boolean forceStructureChange) throws ObjectManagementException {
		for (int i = 0; i < objectTypes.length; ++i) {
			deleteObjectType(dbHandle, objectTypes[i], deleteDependendAttributetypes, forceStructureChange);
		}
	}

	/**
	 * delete the given object type
	 * @param dbHandle dbhandle
	 * @param objectType object type to delete
	 * @param forceStructureChange whether structure changes (removal of quick
	 *        columns are allowed) or not
	 * @throws ObjectManagementException when structure changes are not allowed
	 *         but would be done
	 */
	public static void deleteObjectType(DBHandle dbHandle, ObjectTypeBean objectType,
			boolean forceStructureChange) throws ObjectManagementException {
		deleteObjectType(dbHandle, objectType, false, forceStructureChange);
	}

	/**
	 * delete the given object type
	 * @param dbHandle dbhandle
	 * @param objectType object type to delete
	 * @param deleteDependendAttributetypes whether dependend attributetypes shall be removed or not
	 * @param forceStructureChange whether structure changes (removal of quick
	 *        columns are allowed) or not
	 * @throws ObjectManagementException when structure changes are not allowed
	 *         but would be done
	 */
	public static void deleteObjectType(DBHandle dbHandle, ObjectTypeBean objectType,
			boolean deleteDependendAttributetypes, boolean forceStructureChange) throws ObjectManagementException {
		try {
			Object[] params = new Object[] { objectType.getType() };

			// remove the object type
			DB.update(dbHandle, "delete from " + dbHandle.getContentObjectName() + " where type = ?", params);
			// get all attributetypes for the object type
			SimpleResultProcessor rs = new SimpleResultProcessor();

			DB.query(dbHandle, "select name from " + dbHandle.getContentAttributeTypeName() + " where objecttype = ?", params, rs);

			// remove the attributetypes for the object type
			DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeTypeName() + " where objecttype = ?", params);

			// remove the data
			// from contentmap
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName() + " WHERE obj_type = ?", params);
			// contentmap_nodeversion
			if (DB.tableExists(dbHandle, dbHandle.getContentMapName() + "_nodeversion")) {
				DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName() + "_nodeversion WHERE obj_type = ?", params);
			}
			// contentattribute
			Object[] objTypePattern = new Object[] { objectType.getType().toString() + "%" };

			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + " WHERE contentid LIKE ?", objTypePattern);
			// contentattribute_nodeversion
			if (DB.tableExists(dbHandle, dbHandle.getContentAttributeName() + "_nodeversion")) {
				DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + "_nodeversion WHERE contentid LIKE ?", objTypePattern);
			}

			String productName = DB.getDatabaseProductName(dbHandle);

			// sync the quick columns for all deleted attributetypes
			for (Iterator<SimpleResultRow> iter = rs.iterator(); iter.hasNext();) {
				SimpleResultRow row = iter.next();

				syncOptimizedColumn(dbHandle, productName, row.getString("name"), forceStructureChange, false);
			}

			if (deleteDependendAttributetypes) {
				// get all dependent attributetypes
				DB.query(dbHandle, "select name from " + dbHandle.getContentAttributeTypeName() + " where linkedobjecttype = ?", params, rs);

				// remove the dependent attributetypes
				DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeTypeName() + " where linkedobjecttype = ?", params);

				// sync the quick columns for all deleted attributetypes
				for (Iterator<SimpleResultRow> iter = rs.iterator(); iter.hasNext();) {
					SimpleResultRow row = (SimpleResultRow) iter.next();

					syncOptimizedColumn(dbHandle, productName, row.getString("name"), forceStructureChange, false);
				}
			}
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error("error while deleting objecttype '" + objectType.getType() + "'", ex);
		}
	}

	/**
	 * delete the given list of attribute types
	 * 
	 * @param dbHandle dbhandle
	 * @param attributeTypes array of attribute types to delete
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 */
	public static void deleteAttributeTypes(DBHandle dbHandle, ObjectAttributeBean[] attributeTypes,
			boolean forceStructureChange) throws ObjectManagementException {
		for (int i = 0; i < attributeTypes.length; ++i) {
			deleteAttributeType(dbHandle, attributeTypes[i], forceStructureChange);
		}
	}

	/**
	 * Delete the given attribute type
	 *
	 * @param dbHandle dbhandle
	 * @param attributeType attribute type to delete
	 * @param forceStructureChange true to enforce structural changes (of quick columns)
	 */
	public static void deleteAttributeType(DBHandle dbHandle, ObjectAttributeBean attributeType, boolean forceStructureChange) throws ObjectManagementException {
		try {
			// remove the attributetype
			DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeTypeName() + " where objecttype = ? and name = ?",
					new Object[] { new Integer(attributeType.getObjecttype()), attributeType.getName() });

			// delete the data
			Object[] dataProps = new Object[] { attributeType.getName(), attributeType.getObjecttype() + "%" };

			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + " WHERE name = ? AND contentid LIKE ?", dataProps);
			if (DB.tableExists(dbHandle, dbHandle.getContentAttributeName() + "_nodeversion")) {
				DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + "_nodeversion WHERE name = ? AND contentid LIKE ?", dataProps);
			}

			if (attributeType.getOptimized()) {
				syncOptimizedColumn(dbHandle, DB.getDatabaseProductName(dbHandle), attributeType.getName(), forceStructureChange, false);
			}
		} catch (SQLException ex) {
			NodeLogger.getLogger(ObjectManagementManager.class).error(
					"error while deleting attributetype '" + attributeType.getName() + "' of objecttype '" + attributeType.getObjecttype() + "'", ex);
		}
	}

	/**
	 * load object types from the given db handle
	 * 
	 * @param dbHandle dbhandle for reading the object types
	 * @return collection of ObjectTypeBeans
	 * @deprecated use {@link #loadObjectTypes(Datasource, boolean)} instead
	 */
	public static Collection<ObjectTypeBean> loadObjectTypes(DBHandle dbHandle) {
		try {
			ObjectTypeResultProcessor resultProcessor = new ObjectTypeResultProcessor();

			DB.query(dbHandle, "select * from " + dbHandle.getContentObjectName(), resultProcessor);
			return resultProcessor.getObjectTypes();
		} catch (Exception ex) {
			logger.error("error while loading contentobjects", ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Load the object types (including their attribute types) from the given datasource
	 * The datasource must be either an instance of {@link CNDatasource} or {@link MCCRDatasource}.
	 * @param ds datasource
	 * @param mutable true to get mutable instances, false for immutable copies
	 * @return list of object types
	 * @throws ObjectManagementException
	 */
	public static Collection<ObjectTypeBean> loadObjectTypes(Datasource ds, boolean mutable) throws ObjectManagementException {
		try {
			if (ds instanceof CNDatasource) {
				CNDatasource cnDs = (CNDatasource) ds;
				DBHandle dbHandle = cnDs.getHandle().getDBHandle();
				ObjectTypeResultProcessor resultProcessor = new ObjectTypeResultProcessor();

				DB.query(dbHandle, "select * from " + dbHandle.getContentObjectName(), resultProcessor);
				List<ObjectTypeBean> objectTypes = resultProcessor.getObjectTypes();

				// set the references to the attribute types
				setReferences(objectTypes, loadAttributeTypes(dbHandle));

				return objectTypes;
			} else if (ds instanceof MCCRDatasource) {
				MCCRDatasource mccrDs = (MCCRDatasource) ds;

				return MCCRHelper.getObjectTypes(mccrDs, mutable).values();
			} else if (ds == null) {
				throw new ObjectManagementException("Cannot get object types for null datasource");
			} else {
				throw new ObjectManagementException("Cannot get object types datasource of " + ds.getClass());
			}
		} catch (Exception e) {
			throw new ObjectManagementException("Error while loading object types for " + ds, e);
		}
	}

	/**
	 * load attribute types from the given db handle
	 * @param dbHandle dbhandle for reading the object types
	 * @return collection of ObjectAttributeBeans
	 */
	public static Collection<ObjectAttributeBean> loadAttributeTypes(DBHandle dbHandle) {
		try {
			AttributeTypeResultProcessor attributeTypeResultProcessor = new AttributeTypeResultProcessor(false);

			DB.query(dbHandle,
					"select " + dbHandle.getContentAttributeTypeName() + ".*, " + dbHandle.getContentObjectName() + ".name as linkedobjecttypetext from "
					+ dbHandle.getContentAttributeTypeName() + " left join " + dbHandle.getContentObjectName() + " on " + dbHandle.getContentAttributeTypeName()
					+ ".linkedobjecttype = " + dbHandle.getContentObjectName() + ".type",
					attributeTypeResultProcessor);
			return attributeTypeResultProcessor.getAttributeTypes();
		} catch (Exception ex) {
			logger.error("error while loading contentattributetypes", ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Check whether the resultset contains the column for excluding from versioning
	 * @param metaData resultset meta data
	 * @return true of the column exists, false if not
	 */
	protected static boolean isExcludeVersioningColumn(ResultSetMetaData metaData) {
		boolean columnFound = false;

		try {
			for (int i = 1; i <= metaData.getColumnCount() && !columnFound; ++i) {
				if (DatatypeHelper.EXCLUDE_VERSIONING_FIELD.equalsIgnoreCase(metaData.getColumnName(i))) {
					columnFound = true;
				}
			}
		} catch (SQLException ex) {}

		return columnFound;
	}

	/**
	 * Check whether the resultset contains the column for storing attribute
	 * data in the filesystem
	 * 
	 * @param metaData
	 *            resultset meta data
	 * @return true of the column exists, false if not
	 */
	protected static boolean isFilesystemColumn(ResultSetMetaData metaData) {
		boolean columnFound = false;

		try {
			for (int i = 1; i <= metaData.getColumnCount() && !columnFound; ++i) {
				if (DatatypeHelper.FILESYSTEM_FIELD.equalsIgnoreCase(metaData.getColumnName(i))) {
					columnFound = true;
				}
			}
		} catch (SQLException ex) {}

		return columnFound;
	}

	/**
	 * Set the references between the objecttypes and attributetypes of the
	 * given sets of objects
	 * 
	 * @param objectTypes
	 *            collection of objecttypes
	 * @param attributeTypes
	 *            collection of attributetypes
	 */
	public static void setReferences(Collection<ObjectTypeBean> objectTypes,
			Collection<ObjectAttributeBean> attributeTypes) {
		for (ObjectTypeBean objectType : objectTypes) {
			// clear all references to attributetypes
			objectType.clearAttributeTypes();
			// set all matching objecttype -> attributetype references
			for (ObjectAttributeBean attribute : attributeTypes) {
				if (attribute.getObjecttype() == objectType.getType().intValue()) {
					objectType.addAttributeType(attribute);
				}
			}
		}
	}

	/**
	 * Internal ResultProcessor class to generate object type beans
	 */
	protected static class ObjectTypeResultProcessor implements ResultProcessor {

		/**
		 * collection of generated object type beans
		 */
		protected List<ObjectTypeBean> objectTypes = new Vector<ObjectTypeBean>();

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		 */
		public void process(ResultSet rs) throws SQLException {
			if (isExcludeVersioningColumn(rs.getMetaData())) {
				while (rs.next()) {
					objectTypes.add(
							new ObjectTypeBean(new Integer(rs.getInt("type")), rs.getString("name"), rs.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD)));
				}
			} else {
				while (rs.next()) {
					objectTypes.add(new ObjectTypeBean(new Integer(rs.getInt("type")), rs.getString("name"), false));
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
		 */
		public void takeOver(ResultProcessor p) {// no need to implement this
		}

		/**
		 * Get the collection of generated object type beans
		 * 
		 * @return collection of object type beans
		 */
		public List<ObjectTypeBean> getObjectTypes() {
			return objectTypes;
		}
	}

	/**
	 * Calculate the diff between two collections of objecttypes (with attributetypes)
	 * 
	 * @param originalObjectTypes collection of original objecttypes
	 * @param newObjectTypes collection of new objecttypes
	 * @return diff between the objecttype collections
	 */
	public static TypeDiff getDiff(Collection<ObjectTypeBean> originalObjectTypes,
			Collection<ObjectTypeBean> newObjectTypes) {
		// everything in new but not in original is added
		Collection<ObjectTypeBean> addedObjectTypes = new Vector<ObjectTypeBean>(newObjectTypes);

		addedObjectTypes.removeAll(originalObjectTypes);

		// everything in original but not in new is deleted
		Collection<ObjectTypeBean> deletedObjectTypes = new Vector<ObjectTypeBean>(originalObjectTypes);

		deletedObjectTypes.removeAll(newObjectTypes);

		// everything in both collections might be modified
		Collection<ObjectTypeBean> modifiedObjectTypes = new Vector<ObjectTypeBean>(originalObjectTypes);

		modifiedObjectTypes.retainAll(newObjectTypes);

		Collection<ObjectTypeDiff> modifiedObjectTypeDiff = new Vector<ObjectTypeDiff>();

		// now check whether the objecttype really changed
		for (ObjectTypeBean originalType : modifiedObjectTypes) {
			// find the matching new objecttype
			for (ObjectTypeBean newType : newObjectTypes) {
				if (originalType.equals(newType)) {
					ObjectTypeDiff diff = getTypeDiff(originalType, newType);

					if (diff != null) {
						modifiedObjectTypeDiff.add(diff);
					}
					break;
				}
			}
		}

		return new TypeDiff(addedObjectTypes, deletedObjectTypes, modifiedObjectTypeDiff);
	}

	/**
	 * Calculate the diff between two objecttypes or null if the objecttypes are identical
	 * @param originalType original objecttype
	 * @param newType new objecttype
	 * @return objecttypediff or null
	 */
	public static ObjectTypeDiff getTypeDiff(ObjectTypeBean originalType, ObjectTypeBean newType) {
		return getTypeDiff(originalType, newType, false);
	}

	/**
	 * Calculate the diff between two objecttypes or null if the objecttypes are identical
	 * @param originalType original objecttype
	 * @param newType new objecttype
	 * @param ignoreOptimized true when optimized flags shall be ignored for all contentattributetypes, false if not
	 * @return objecttypediff or null
	 */
	public static ObjectTypeDiff getTypeDiff(ObjectTypeBean originalType, ObjectTypeBean newType,
			boolean ignoreOptimized) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("Checking differences for type " + originalType);
		}

		// calculate added attributes
		Collection<ObjectAttributeBean> addedAttributes = new Vector<ObjectAttributeBean>(newType.getAttributeTypesList());

		addedAttributes.removeAll(originalType.getAttributeTypesList());

		// calculate deleted attributes
		Collection<ObjectAttributeBean> deletedAttributes = new Vector<ObjectAttributeBean>(originalType.getAttributeTypesList());

		deletedAttributes.removeAll(newType.getAttributeTypesList());

		// calculate modified attributes
		Collection<ObjectAttributeBean> modifiedAttributes = new Vector<ObjectAttributeBean>(newType.getAttributeTypesList());
		List<ObjectAttributeBean> originalAttributes = originalType.getAttributeTypesList();

		for (Iterator<ObjectAttributeBean> iter = modifiedAttributes.iterator(); iter.hasNext();) {
			ObjectAttributeBean newAttribute = iter.next();
			boolean found = false;

			// find the same attribute in the list of original attributes
			for (Iterator<ObjectAttributeBean> iterator = originalAttributes.iterator(); !found && iterator.hasNext();) {
				ObjectAttributeBean origAttribute = iterator.next();

				if (origAttribute.equals(newAttribute)) {
					// found the attribute, check whether the attribute was changed
					found = true;

					if (ObjectAttributeBean.attributesEqual(origAttribute, newAttribute)) {
						iter.remove();
					}
				}
			}

			// remove the attribute also when no matching attribute found (attribute is new)
			if (!found) {
				iter.remove();
			}
		}

		newType.setOldType(originalType.getType());

		// check whether anything was changed
		if (originalType.getType().intValue() == newType.getType().intValue() && StringUtils.isEqual(originalType.getName(), newType.getName())
				&& originalType.isExcludeVersioning() == newType.isExcludeVersioning() && addedAttributes.size() == 0 && deletedAttributes.size() == 0
				&& modifiedAttributes.size() == 0) {
			// nothing changed
			return null;
		} else {
			return new ObjectTypeDiff(originalType, newType, addedAttributes, deletedAttributes, modifiedAttributes);
		}
	}

	/**
	 * Import the type definition for the given multichannelling datasource
	 * @param datasource datasource
	 * @param in input stream
	 * @throws ObjectManagementException
	 */
	public static void importTypes(MCCRDatasource datasource, InputStream in) throws ObjectManagementException {
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectManagementManager.JAXB_PACKAGE);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			unmarshaller.setValidating(false);
			Object importedObject = unmarshaller.unmarshal(in);

			if (!(importedObject instanceof Definition)) {
				throw new ObjectManagementException("Input stream did not contain an object definition");
			}
			Definition importedDefinition = (Definition) importedObject;

			if (importedDefinition.isSetObjectTypes()) {
				Objecttype[] importedObjectTypes = importedDefinition.getObjectTypes();

				// all previous checks went fine, so save the objecttypes and attributetypes now
				for (int i = 0; i < importedObjectTypes.length; ++i) {
					ObjectTypeBean objectType = new ObjectTypeBean(importedObjectTypes[i]);

					// save the objecttype now
					saveObjectType(datasource, objectType, true, true, false);
				}
			}
		} catch (JAXBException e) {
			throw new ObjectManagementException("Error while exporting objecttype definitions", e);
		}
	}

	/**
	 * Save the given object type into the datasource
	 * @param datasource datasource
	 * @param type object type
	 * @param saveAttributes true to also save the attributes
	 * @param forceStructureChange true to force structure changes
	 * @param ignoreOptimized true to ignore changes for the optimized flag
	 * @throws ObjectManagementException
	 */
	protected static void saveObjectType(MCCRDatasource datasource, ObjectTypeBean type, boolean saveAttributes,
			boolean forceStructureChange, boolean ignoreOptimized) throws ObjectManagementException {
		try {
			DBHandle dbHandle = datasource.getHandle();

			if (type.getOldType() == null) {
				if (type.getType() == null) {
					String nextType = getNextObjectType(dbHandle);

					if (nextType != null) {
						type.setType(new Integer(nextType));
						type.setOldType(new Integer(nextType));
					} else {
						throw new ObjectManagementException("Error while saving object type " + type.getName() + ": could not set a new object type");
					}
				}
				DB.update(dbHandle, "INSERT INTO " + dbHandle.getContentObjectName() + " (name, type) values(?, ?)",
						new Object[] { type.getName(), type.getType() });
			} else {
				DB.update(dbHandle, "UPDATE " + dbHandle.getContentObjectName() + " set name = ?, type = ? where type = ?",
						new Object[] { type.getName(), type.getType(), type.getOldType() });
			}

			// save the attributes, if requested
			if (saveAttributes) {
				ObjectAttributeBean[] attributeTypes = type.getAttributeTypes();
				List<String> attrNames = new Vector<String>();

				for (ObjectAttributeBean attrType : attributeTypes) {
					saveAttributeType(datasource, type, attrType, forceStructureChange, ignoreOptimized);
					attrNames.add(attrType.getName());
				}

				// remove assignements to old attribute
				if (attrNames.isEmpty()) {
					DB.update(dbHandle, "DELETE FROM object_attribute WHERE object_type = ?", new Object[] { type.getType()});
				} else {
					List<Object> params = new Vector<Object>();

					params.add(type.getType());
					params.addAll(attrNames);
					DB.update(dbHandle,
							"DELETE FROM object_attribute WHERE object_type = ? AND attribute_name NOT IN (" + StringUtils.repeat("?", attrNames.size(), ",") + ")",
							(Object[]) params.toArray(new Object[params.size()]));
				}
			}
		} catch (Exception e) {
			throw new ObjectManagementException("Error while saving object type " + type.getName(), e);
		}
	}

	/**
	 * Save the given attribute type for the given object type
	 * @param datasource datasource
	 * @param type object type
	 * @param attrType attribute type
	 * @param forceStructureChange true to force structure changes
	 * @param ignoreOptimized true to ignore the optimized flag
	 * @throws ObjectManagementException
	 */
	protected static void saveAttributeType(MCCRDatasource datasource, ObjectTypeBean type, ObjectAttributeBean attrType,
			boolean forceStructureChange, boolean ignoreOptimized) throws ObjectManagementException {
		try {
			DBHandle dbHandle = datasource.getHandle();

			attrType.normalizedAttribute();
			attrType.checkConsistency();

			AttributeTypeResultProcessor result = new AttributeTypeResultProcessor(true);

			// first check whether there already exists an attribute with the name
			DB.query(dbHandle,
					"select " + dbHandle.getContentAttributeTypeName() + ".*, " + dbHandle.getContentObjectName() + ".name as linkedobjecttypetext from "
					+ dbHandle.getContentAttributeTypeName() + " left join " + dbHandle.getContentObjectName() + " on " + dbHandle.getContentAttributeTypeName()
					+ ".linkedobjecttype = " + dbHandle.getContentObjectName() + ".type WHERE " + dbHandle.getContentAttributeTypeName() + ".name = ?",
					new Object[] { attrType.getOldname() },
					result);
			if (result.getAttributeTypes().size() > 0) {
				// there already exist a contentattributetype, so do an update

				// set up the update parameters, depending on whether the
				// optimized flag shall be ignored or not
				if (ignoreOptimized) {
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET name = ?, type = ?, "
							+ "multivalue = ?, filesystem = ?, linkedobjecttype = ?, foreignlinkattribute = ?, foreignlinkattributerule = ? WHERE name = ?",
							new Object[] {
						attrType.getName(), attrType.getAttributetype(), attrType.getMultivalue(), attrType.isFilesystem(),
						attrType.getLinkedobjecttype(), attrType.getForeignlinkattribute(), attrType.getForeignlinkattributerule(), attrType.getOldname() });
				} else {
					DB.update(dbHandle,
							"UPDATE " + dbHandle.getContentAttributeTypeName() + " SET name = ?, type = ?, optimized = ?, quickname = ?, "
							+ "multivalue = ?, filesystem = ?, linkedobjecttype = ?, foreignlinkattribute = ?, foreignlinkattributerule = ? WHERE name = ?",
							new Object[] {
						attrType.getName(), attrType.getAttributetype(), attrType.getOptimized(), attrType.getQuickname(), attrType.getMultivalue(),
						attrType.isFilesystem(), attrType.getLinkedobjecttype(), attrType.getForeignlinkattribute(), attrType.getForeignlinkattributerule(),
						attrType.getOldname() });
				}

				ObjectAttributeBean oldAttribute = (ObjectAttributeBean) result.getAttributeTypes().get(0);

				if (oldAttribute.isFilesystem() && !attrType.isFilesystem()) {
					// move from filesystem to DB
					MCCRHelper.moveDataFromFS2DB(datasource, attrType);
				} else if (!oldAttribute.isFilesystem() && attrType.isFilesystem()) {
					// move from DB to filesystem
					MCCRHelper.moveDataFromDB2FS(datasource, attrType);
				}

				// synchronize optimized columns (if they are not ignored) when
				// something has changed
				if (!ignoreOptimized
						&& (oldAttribute.getOptimized() != attrType.getOptimized() || !oldAttribute.getName().equals(attrType.getName())
						|| !DatatypeHelper.getTypeColumn(oldAttribute.getAttributetype()).equals(DatatypeHelper.getTypeColumn(attrType.getAttributetype())))) {
					String productName = DB.getDatabaseProductName(dbHandle);

					syncOptimizedColumn(dbHandle, productName, attrType.getName(), forceStructureChange, true);
					if (!StringUtils.isEqual(oldAttribute.getName(), attrType.getName())) {
						syncOptimizedColumn(dbHandle, productName, oldAttribute.getName(), forceStructureChange, true);
					}
				}

				// assign the attributetype to the object type (if not already done)
				SimpleResultProcessor proc = new SimpleResultProcessor();

				DB.query(dbHandle, "SELECT * FROM object_attribute WHERE object_type = ? AND attribute_name = ?",
						new Object[] { type.getType(), attrType.getName() }, proc);
				if (proc.size() == 0) {
					DB.update(dbHandle, "INSERT INTO object_attribute (object_type, attribute_name) VALUES (?, ?)",
							new Object[] { type.getType(), attrType.getName() });
				}
			} else {
				// this contentattributetype does not exist, so make an insert
				DB.update(dbHandle,
						"INSERT INTO " + dbHandle.getContentAttributeTypeName()
						+ " (name, type, optimized, quickname, multivalue, filesystem, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule) VALUES (?,?,?,?,?,?,?,?,?)",
						new Object[] {
					attrType.getName(), attrType.getAttributetype(), ignoreOptimized ? false : attrType.getOptimized(),
					ignoreOptimized ? null : attrType.getQuickname(), attrType.getMultivalue(), attrType.isFilesystem(), attrType.getLinkedobjecttype(),
					attrType.getForeignlinkattribute(), attrType.getForeignlinkattributerule() });

				// assign the attributetype to the object type
				// TODO table name!
				DB.update(dbHandle, "INSERT INTO object_attribute (object_type, attribute_name) VALUES (?, ?)",
						new Object[] { type.getType(), attrType.getName() });

				// create the optimized column, when optimized is true
				if (!ignoreOptimized && attrType.getOptimized()) {
					syncOptimizedColumn(dbHandle, DB.getDatabaseProductName(dbHandle), attrType.getName(), forceStructureChange, true);
				}
			}
			DatatypeHelper.clear();
		} catch (Exception e) {
			throw new ObjectManagementException("Error while saving attribute " + attrType.getName(), e);
		}
	}

	/**
	 * Import the object types from the given stream
	 * @param datasource db handle
	 * @param in stream
	 * @throws ObjectManagementException
	 */
	public static void importTypes(CNDatasource datasource, InputStream in) throws ObjectManagementException {
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectManagementManager.JAXB_PACKAGE);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			unmarshaller.setValidating(false);
			Object importedObject = unmarshaller.unmarshal(in);

			if (!(importedObject instanceof Definition)) {
				throw new ObjectManagementException("Input stream did not contain an object definition");
			}
			Definition importedDefinition = (Definition) importedObject;

			DBHandle dbHandle = datasource.getHandle().getDBHandle();

			if (importedDefinition.isSetObjectTypes()) {
				Objecttype[] importedObjectTypes = importedDefinition.getObjectTypes();

				for (int i = 0; i < importedObjectTypes.length; ++i) {
					ObjectTypeBean objectType = new ObjectTypeBean(importedObjectTypes[i]);
					ObjectAttributeBean[] attributeTypes = objectType.getAttributeTypes();

					// check for consistency
					Collection<ObjectTypeBean> conflictingObjectTypes = ObjectManagementManager.getConflictingObjectTypes(dbHandle, objectType);

					if (conflictingObjectTypes == null) {
						throw new ObjectManagementException("Error while checking for conflicts");
					} else if (conflictingObjectTypes.size() > 0) {
						throw new ObjectManagementException("Found conflicts in the object types");
					}

					// also check all attributetypes
					for (int j = 0; j < attributeTypes.length; ++j) {
						Collection<ObjectAttributeBean> conflictingAttributes = ObjectManagementManager.getConflictingAttributes(dbHandle, attributeTypes[j],
								ObjectManagementManager.ATTRIBUTECHECK_NAME);

						if (conflictingAttributes == null) {
							throw new ObjectManagementException("Error while checking for conflicts");
						} else if (conflictingAttributes.size() > 0) {
							throw new ObjectManagementException("Found conflicts in the object attributes");
						}

						// get eventually conflicting attributes in other
						// objecttypes
						conflictingAttributes = ObjectManagementManager.getConflictingAttributes(dbHandle, attributeTypes[j],
								ObjectManagementManager.ATTRIBUTECHECK_TYPE);
						if (conflictingAttributes == null) {
							throw new ObjectManagementException("Error while checking for conflicts");
						} else if (conflictingAttributes.size() > 0) {
							throw new ObjectManagementException("Found conflicts in the object attributes");
						}
					}
				}

				// all previous checks went fine, so save the objecttypes and attributetypes now
				for (int i = 0; i < importedObjectTypes.length; ++i) {
					ObjectTypeBean objectType = new ObjectTypeBean(importedObjectTypes[i]);
					ObjectAttributeBean[] attributeTypes = objectType.getAttributeTypes();

					// save the objecttype now
					if (!saveObjectType(datasource, objectType, false, true)) {
						throw new ObjectManagementException("Could not save object type" + objectType.getName());
					}

					for (int j = 0; j < attributeTypes.length; ++j) {
						// save the attributetype now
						if (!saveAttributeType(datasource, attributeTypes[j], true)) {
							throw new ObjectManagementException("Could not save attribute type " + attributeTypes[j].getName());
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ObjectManagementException("Error while exporting objecttype definitions", e);
		}
	}

	/**
	 * The given objecttype was set to be excluded from versioning. So all
	 * existing versions need to be removed (and the 0 version be generated).
	 * @param dbHandle database handle
	 * @param objectType objecttype
	 * @throws SQLException
	 */
	protected static void unversionObjectType(DBHandle dbHandle, ObjectTypeBean objectType) throws Exception {
		TableVersion contentMapVersion = new TableVersion();

		contentMapVersion.setHandle(dbHandle);
		contentMapVersion.setTable(dbHandle.getContentMapName());
		contentMapVersion.setWherePart("gentics_main.contentid = ?");

		// remove all _nodeversion's and generate the versions with timestamp 0
		DB.update(dbHandle, "delete from " + dbHandle.getContentMapName() + "_nodeversion where obj_type = ?", new Object[] { objectType.getType() });
		// create the initial versions with timestamp 0
		contentMapVersion.createInitialVersions(0, "system", dbHandle.getContentMapName() + ".obj_type = ?", new Object[] { objectType.getType() });

		// also reset versioning for the attributes
		TableVersion allContentAttributeVersion = new TableVersion();

		allContentAttributeVersion.setHandle(dbHandle);
		allContentAttributeVersion.setTable(dbHandle.getContentAttributeName());
		allContentAttributeVersion.setWherePart("gentics_main.contentid = ?");

		// remove all versioned data for all attributes of the objecttype
		DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeName() + "_nodeversion where contentid LIKE ?",
				new Object[] { objectType.getType() + ".%" });
		// create the initial version with timestamp 0
		allContentAttributeVersion.createInitialVersions(0, "system", dbHandle.getContentAttributeName() + ".contentid LIKE ?",
				new Object[] { objectType.getType() + ".%" });
	}

	/**
	 * The given attributetype was set be excluded from versioning. So all
	 * existing versions need to be removed (and the 0 version generated).
	 * @param dbHandle database handle
	 * @param attributeType attributetype
	 * @throws SQLException
	 */
	protected static void unversionAttributeType(DBHandle dbHandle, ObjectAttributeBean attributeType) throws NodeException, SQLException {
		TableVersion allContentAttributeVersion = new TableVersion();

		allContentAttributeVersion.setHandle(dbHandle);
		allContentAttributeVersion.setTable(dbHandle.getContentAttributeName());
		allContentAttributeVersion.setWherePart("gentics_main.contentid = ?");

		// remove all versioned data for the attribute
		DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeName() + "_nodeversion where name = ? AND contentid LIKE ?",
				new Object[] { attributeType.getName(), attributeType.getObjecttype() + ".%" });
		// create the initial version with timestamp 0
		allContentAttributeVersion.createInitialVersions(0, "system",
				dbHandle.getContentAttributeName() + ".name = ? AND " + dbHandle.getContentAttributeName() + ".contentid LIKE ?", new Object[] {
			attributeType.getName(), attributeType.getObjecttype() + ".%" });
	}

	/**
	 * Clean unused attribute types (attribute types definitions that are not linked to any object types)
	 * @param datasource datasource
	 * @throws ObjectManagementException
	 */
	public static void cleanUnusedAttributeTypes(MCCRDatasource datasource) throws ObjectManagementException {
		try {
			DBHandle handle = datasource.getHandle();
			String productName = DB.getDatabaseProductName(handle);
			String cat = handle.getContentAttributeTypeName();
			String oa = "object_attribute";
			SimpleResultProcessor colData = new SimpleResultProcessor();

			DB.query(handle, "SELECT cat.* FROM " + cat + " cat LEFT JOIN " + oa + " oa ON cat.name = oa.attribute_name WHERE oa.object_type IS NULL", null,
					colData);
			for (SimpleResultRow col : colData) {
				if (col.getBoolean("optimized")) {
					// get the column definition
					String attributeName = col.getString("name");
					ColumnDefinition colDef = DatatypeHelper.getQuickColumnDefinition(handle, productName, 1,
							ObjectAttributeBean.constructQuickColumnName(attributeName));

					// drop the index
					String dropIndexStatement = DatatypeHelper.getDBSpecificDropIndexStatement(productName, colDef.getTableName(),
							getIndexName(colDef.getColumnName()));

					DB.update(handle, dropIndexStatement);

					// drop the quick column
					String[] dropColumn = DatatypeHelper.getDBSpecificDropColumnStatements(productName, colDef.getTableName(), colDef.getColumnName());

					if (dropColumn != null) {
						for (int i = 0; i < dropColumn.length; i++) {
							DB.update(handle, dropColumn[i]);
						}
					}
				}
			}

			SimpleResultProcessor names = new SimpleResultProcessor();

			DB.query(handle, "SELECT name FROM " + cat + " cat LEFT JOIN " + oa + " oa ON cat.name = oa.attribute_name WHERE oa.object_type IS NULL", names);
			for (SimpleResultRow nameRow : names) {
				DB.update(handle, "DELETE FROM " + cat + " WHERE name = ?", new Object[] { nameRow.getString("name")});
			}
		} catch (Exception e) {
			throw new ObjectManagementException("Error while cleaning unused attribute types", e);
		}
	}

	/**
	 * Class for definition differences
	 */
	public static class TypeDiff {

		/**
		 * collection of added object types (instances of ObjectTypeBean)
		 */
		private Collection<ObjectTypeBean> addedObjectTypes;

		/**
		 * collection of deleted object types (instances of ObjectTypeBean)
		 */
		private Collection<ObjectTypeBean> deletedObjectTypes;

		/**
		 * collection of modified object types (instance of ObjectTypeDiff)
		 */
		private Collection<ObjectTypeDiff> modifiedObjectTypes;

		/**
		 * Create an instance of the type diff
		 * @param addedObjectTypes collection of added object types
		 * @param deletedObjectTypes collection of deleted object types
		 * @param modifiedObjectTypes collection of modified object types
		 */
		protected TypeDiff(Collection<ObjectTypeBean> addedObjectTypes, Collection<ObjectTypeBean> deletedObjectTypes,
				Collection<ObjectTypeDiff> modifiedObjectTypes) {
			this.addedObjectTypes = addedObjectTypes;
			this.deletedObjectTypes = deletedObjectTypes;
			this.modifiedObjectTypes = modifiedObjectTypes;
		}

		public Collection<ObjectTypeBean> getAddedObjectTypes() {
			return addedObjectTypes;
		}

		public Collection<ObjectTypeBean> getDeletedObjectTypes() {
			return deletedObjectTypes;
		}

		public Collection<ObjectTypeDiff> getModifiedObjectTypes() {
			return modifiedObjectTypes;
		}
	}

	/**
	 * Class for object type differences
	 */
	public static class ObjectTypeDiff {

		/**
		 * instance of the original objecttype bean
		 */
		private ObjectTypeBean originalObjectType;

		/**
		 * instance of the modified objecttype bean
		 */
		private ObjectTypeBean modifiedObjectType;

		/**
		 * added attribute types
		 */
		private Collection<ObjectAttributeBean> addedAttributeTypes;

		/**
		 * deleted attribute types
		 */
		private Collection<ObjectAttributeBean> deletedAttributeTypes;

		/**
		 * modified attribute types
		 */
		private Collection<ObjectAttributeBean> modifiedAttributeTypes;

		/**
		 * Create an instance of the object type diff
		 * @param originalObjectType original object type
		 * @param modifiedObjectType modified object type
		 * @param addedAttributeTypes collection of added attribute types
		 * @param deletedAttributeTypes collection of deleted attribute types
		 * @param modifiedAttributeTypes collection of modified attribute types
		 */
		protected ObjectTypeDiff(ObjectTypeBean originalObjectType, ObjectTypeBean modifiedObjectType,
				Collection<ObjectAttributeBean> addedAttributeTypes,
				Collection<ObjectAttributeBean> deletedAttributeTypes,
				Collection<ObjectAttributeBean> modifiedAttributeTypes) {
			this.originalObjectType = originalObjectType;
			this.modifiedObjectType = modifiedObjectType;
			this.addedAttributeTypes = addedAttributeTypes;
			this.deletedAttributeTypes = deletedAttributeTypes;
			this.modifiedAttributeTypes = modifiedAttributeTypes;
		}

		/**
		 * @return Returns the addedAttributeTypes.
		 */
		public Collection<ObjectAttributeBean> getAddedAttributeTypes() {
			return addedAttributeTypes;
		}

		/**
		 * @return Returns the deletedAttributeTypes.
		 */
		public Collection<ObjectAttributeBean> getDeletedAttributeTypes() {
			return deletedAttributeTypes;
		}

		/**
		 * @return Returns the modifiedAttributeTypes.
		 */
		public Collection<ObjectAttributeBean> getModifiedAttributeTypes() {
			return modifiedAttributeTypes;
		}

		/**
		 * @return Returns the modifiedObjectType.
		 */
		public ObjectTypeBean getModifiedObjectType() {
			return modifiedObjectType;
		}

		/**
		 * @return Returns the originalObjectType.
		 */
		public ObjectTypeBean getOriginalObjectType() {
			return originalObjectType;
		}
	}

	/**
	 * Internal ResultProcessor class to generate attribute type beans
	 */
	protected static class AttributeTypeResultProcessor implements ResultProcessor {

		/**
		 * collection of generated attribute type beans
		 */
		protected List<ObjectAttributeBean> attributeTypes = new Vector<ObjectAttributeBean>();

		protected boolean multichannelling;

		/**
		 * Create an instance
		 * @param multichannelling true if used for multichannelling
		 */
		public AttributeTypeResultProcessor(boolean multichannelling) {
			this.multichannelling = multichannelling;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		 */
		public void process(ResultSet rs) throws SQLException {
			boolean excludeVersionColumn = multichannelling ? false : isExcludeVersioningColumn(rs.getMetaData());
			boolean filesystemColumn = isFilesystemColumn(rs.getMetaData());
			String typeColumn = multichannelling ? "type" : "attributetype";

			while (rs.next()) {
				attributeTypes.add(
						new ObjectAttributeBean(rs.getString("name"), rs.getInt(typeColumn), rs.getBoolean("optimized"), rs.getString("quickname"),
						rs.getBoolean("multivalue"), multichannelling ? 0 : rs.getInt("objecttype"), rs.getInt("linkedobjecttype"),
						rs.getString("linkedobjecttypetext"), rs.getString("foreignlinkattribute"),
						ObjectTransformer.getString(rs.getObject("foreignlinkattributerule"), null),
						excludeVersionColumn ? rs.getBoolean(DatatypeHelper.EXCLUDE_VERSIONING_FIELD) : false,
						filesystemColumn ? rs.getBoolean(DatatypeHelper.FILESYSTEM_FIELD) : false));
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.
		 * ResultProcessor)
		 */
		public void takeOver(ResultProcessor p) {// no need to implement this
		}

		/**
		 * Get the collection of generated attribute type beans
		 * 
		 * @return collection of attribute type beans
		 */
		public List<ObjectAttributeBean> getAttributeTypes() {
			return attributeTypes;
		}
	}

}
