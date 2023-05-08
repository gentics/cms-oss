package com.gentics.lib.datasource.mccr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.PreparedBatchStatement;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper.CacheDummy;
import com.gentics.lib.datasource.mccr.MCCRStats.Item;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.SimpleUpdateProcessor;
import com.gentics.lib.db.StringLengthManipulator;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

/**
 * Static helper class for multichannelling aware content repositories
 */
public class MCCRHelper {
	public final static int BATCHLOAD_MAX_OBJECTS = 1000;

	/**
	 * Comparator instance for comparing/sorting MCCRObject instances
	 */
	public final static Comparator<MCCRObject> COMPARATOR = new ObjectComparator();

	/**
	 * Insert statement for table contentattribute including all available columns
	 */
	protected final static String BATCH_INSERT = "INSERT INTO {tablename} (map_id, name, sortorder, value_text, value_int, value_blob, value_clob, value_long, value_double, value_date, updatetimestamp) VALUES ("
			+ StringUtils.repeat("?", 11, ",") + ")";

	/**
	 * Types of the parameters in {@link #BATCH_INSERT}
	 */
	protected final static List<Integer> BATCH_INSERT_TYPES = Arrays.asList(Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER,
			Types.BLOB, Types.LONGVARCHAR, Types.BIGINT, Types.DOUBLE, Types.TIMESTAMP, Types.INTEGER);

	/**
	 * Update statement for table contentattribute including all available columns
	 */
	protected final static String BATCH_UPDATE = "UPDATE {tablename} SET value_text = ?, value_int = ?, value_blob = ?, value_clob = ?, value_long = ?, value_double = ?, value_date = ?, updatetimestamp = ? WHERE id = ?";

	/**
	 * Types of the parameters in {@link #BATCH_UPDATE}
	 */
	protected final static List<Integer> BATCH_UPDATE_TYPES = Arrays.asList(Types.VARCHAR, Types.INTEGER, Types.BLOB, Types.LONGVARCHAR, Types.BIGINT,
			Types.DOUBLE, Types.TIMESTAMP, Types.INTEGER, Types.INTEGER);

	/**
	 * Delete statement for table contentattribute
	 */
	protected final static String BATCH_DELETE = "DELETE FROM {tablename} WHERE id = ?";

	/**
	 * Map containing the indices for attributes to be used in {@link #BATCH_INSERT}
	 */
	protected final static Map<String, Integer> BATCH_INSERT_COLUMNS = new HashMap<String, Integer>();

	/**
	 * Map containing the indices for attributes to be used in {@link #BATCH_UPDATE}
	 */
	protected final static Map<String, Integer> BATCH_UPDATE_COLUMNS = new HashMap<String, Integer>();

	static {
		BATCH_INSERT_COLUMNS.put("map_id", 0);
		BATCH_INSERT_COLUMNS.put("name", 1);
		BATCH_INSERT_COLUMNS.put("sortorder", 2);
		BATCH_INSERT_COLUMNS.put("value_text", 3);
		BATCH_INSERT_COLUMNS.put("value_int", 4);
		BATCH_INSERT_COLUMNS.put("value_blob", 5);
		BATCH_INSERT_COLUMNS.put("value_clob", 6);
		BATCH_INSERT_COLUMNS.put("value_long", 7);
		BATCH_INSERT_COLUMNS.put("value_double", 8);
		BATCH_INSERT_COLUMNS.put("value_date", 9);
		BATCH_INSERT_COLUMNS.put("updatetimestamp", 10);

		BATCH_UPDATE_COLUMNS.put("value_text", 0);
		BATCH_UPDATE_COLUMNS.put("value_int", 1);
		BATCH_UPDATE_COLUMNS.put("value_blob", 2);
		BATCH_UPDATE_COLUMNS.put("value_clob", 3);
		BATCH_UPDATE_COLUMNS.put("value_long", 4);
		BATCH_UPDATE_COLUMNS.put("value_double", 5);
		BATCH_UPDATE_COLUMNS.put("value_date", 6);
		BATCH_UPDATE_COLUMNS.put("updatetimestamp", 7);
		BATCH_UPDATE_COLUMNS.put("id", 8);
	}

	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCCRHelper.class);

	/**
	 * Threadlocal data maps. Can be filled by calling
	 * {@link #prepareForUpdate(Collection)} and reset by calling
	 * {@link #resetPreparedForUpdate()}
	 */
	protected static ThreadLocal<Map<Integer, Map<String, List<SimpleResultRow>>>> dataMaps = new ThreadLocal<Map<Integer, Map<String, List<SimpleResultRow>>>>();

	/**
	 * Statistics
	 */
	protected static MCCRStats stats;

	/**
	 * Enable/disable statistics
	 * @param enable true to enable, false to disable
	 */
	public static void enableStatistics(boolean enable) {
		if (enable && stats == null) {
			stats = new MCCRStats();
		} else if (!enable) {
			stats = null;
		}
	}

	/**
	 * Get the statistics, if enabled or null
	 * @return statistics or null
	 */
	public static MCCRStats getStatistics() {
		return stats;
	}

	/**
	 * Get the channel structure from the db
	 * @param ds datasource
	 * @throws DatasourceException
	 */
	public static ChannelTree getChannelStructure(MCCRDatasource ds) throws DatasourceException {
		ChannelTree cached = MCCRCacheHelper.getChannelTree(ds);

		if (cached != null) {
			return cached;
		}

		final ChannelTree tree = new ChannelTree();
		final Stack<MPTTInfo> infoStack = new Stack<MPTTInfo>();
		DBHandle handle = ds.getHandle();

		try {
			DB.query(handle, "SELECT * FROM channel ORDER BY mptt_left ASC", null, new ResultProcessor() {

				/* (non-Javadoc)
				 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
				 */
				public void process(ResultSet rs) throws SQLException {
					while (rs.next()) {
						int id = rs.getInt("id");
						String name = rs.getString("name");
						int left = rs.getInt("mptt_left");
						int right = rs.getInt("mptt_right");
						MPTTInfo info = new MPTTInfo(new ChannelTreeNode(new DatasourceChannel(id, name)), left, right);

						if (infoStack.isEmpty()) {
							// we apparently found the root node
							infoStack.push(info);
						} else {
							// we found a non-root node

							// traverse up the stack to the immediate mother
							while (right > infoStack.peek().right) {
								infoStack.pop();
							}

							if (infoStack.peek().node.getChannel().getId() == 0) {
								// the node must be added to the tree root
								tree.getChildren().add(info.node);
							} else {
								// the node must be added to the current top on the stack
								infoStack.peek().node.getChildren().add(info.node);
							}

							// finally add the current node on the info stack
							infoStack.push(info);
						}
					}
				}

				/* (non-Javadoc)
				 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
				 */
				public void takeOver(ResultProcessor p) {}
			});
		} catch (SQLException e) {
			throw new DatasourceException("Error while reading tree structure", e);
		}

		// put into cache
		MCCRCacheHelper.put(ds, tree);

		return tree;
	}

	/**
	 * Save the given channel structure
	 * @param ds datasource
	 * @param tree channel structure tree
	 * @throws DatasourceException
	 */
	public static void saveChannelStructure(MCCRDatasource ds, ChannelTree tree) throws DatasourceException {
		if (tree == null) {
			throw new DatasourceException("Cannot set empty channel structure");
		}

		DBHandle handle = ds.getHandle();

		// check for duplicate or illegal IDs
		recursiveCheckDuplicateIDs(tree.getChildren(), new Vector<Integer>());

		List<MPTTInfo> flatInfo = makeFlat(tree);
		List<Integer> savedIds = new Vector<Integer>();

		try {
			SimpleResultProcessor proc = new SimpleResultProcessor();

			for (MPTTInfo info : flatInfo) {
				int id = info.node.getChannel().getId();
				String name = info.node.getChannel().getName();

				proc.clear();
				DB.query(handle, "SELECT * FROM channel WHERE id = ?", new Object[] { id }, proc);
				
				if (proc.size() > 0) {
					
					// We know the IDs are unique so the first row should be the only row
					SimpleResultRow dbInfo = proc.getRow(1);
					
					// If there is difference in channel data - update it.
					if (name == null || !name.equals(dbInfo.getString("name")) || info.left != dbInfo.getInt("mptt_left")
							|| info.right != dbInfo.getInt("mptt_right")) {
						
						DB.update(handle, "UPDATE channel SET mptt_left = ?, mptt_right = ?, name = ? WHERE id = ?", 
								new Object[] { info.left, info.right, name, id });
					}
				} else {
					DB.update(handle, "INSERT INTO channel (id, name, mptt_left, mptt_right) VALUES (?, ?, ?, ?)",
							new Object[] { id, name, info.left, info.right });
				}
				savedIds.add(id);
			}

			if (!savedIds.isEmpty()) {
				DB.update(handle, "DELETE FROM channel WHERE id NOT IN (" + StringUtils.repeat("?", savedIds.size(), ",") + ")",
						(Object[]) savedIds.toArray(new Object[savedIds.size()]));
			}

			// put saved tree in the cache
			MCCRCacheHelper.put(ds, tree);
		} catch (SQLException e) {
			throw new DatasourceException("Error while saving channel structure", e);
		}
	}

	/**
	 * Get the channel with given ID
	 * @param tree tree
	 * @param channelId channel id
	 * @return channel (never null)
	 * @throws DatasourceException id the channel could not be found
	 */
	public static DatasourceChannel getChannel(ChannelTree tree, int channelId) throws DatasourceException {
		if (tree == null) {
			throw new UnknownChannelException("Could not find channel " + channelId);
		}
		DatasourceChannel channel = recursiveFindChannel(tree.getChildren(), channelId);

		if (channel == null) {
			throw new UnknownChannelException("Could not find channel " + channelId);
		}
		return channel;
	}

	/**
	 * Get the channel path to the given channel
	 * @param ds datasource
	 * @param channelId channel id
	 * @return channel path
	 * @throws DatasourceException
	 */
	public static List<DatasourceChannel> getChannelPath(MCCRDatasource ds, int channelId) throws DatasourceException {
		// get the whole tree
		ChannelTree tree = getChannelStructure(ds);

		// prepare the channel path
		List<DatasourceChannel> channelPath = new ArrayList<DatasourceChannel>();

		// use the recursive method to build the channel tree
		recursiveBuildChannelPath(tree.getChildren(), channelId, channelPath);
		return channelPath;
	}

	/**
	 * Get all object types (containing all attribute types)
	 * @param ds datasource
	 * @param mutable true to get changeable instances, false for immutable
	 * @return map of object types, keys are the type IDs
	 * @throws DatasourceException
	 */
	public static Map<Integer, ObjectTypeBean> getObjectTypes(MCCRDatasource ds, boolean mutable) throws DatasourceException {
		Map<Integer, ObjectTypeBean> cachedObjectTypes = MCCRCacheHelper.getTypes(ds, mutable);

		if (cachedObjectTypes != null) {
			return cachedObjectTypes;
		}
		DBHandle dbHandle = ds.getHandle();
		final Map<Integer, ObjectTypeBean> objectTypes = new HashMap<Integer, ObjectTypeBean>();

		try {
			DB.query(dbHandle,
					"SELECT co.type co_type, co.name co_name, cat.* FROM " + dbHandle.getContentObjectName()
					+ " co LEFT JOIN object_attribute oa ON co.type = oa.object_type LEFT JOIN " + dbHandle.getContentAttributeTypeName()
					+ " cat ON oa.attribute_name = cat.name",
					new ResultProcessor() {
				public void process(ResultSet rs) throws SQLException {
					while (rs.next()) {
						// get the object type (create one if not yet created)
						int coType = rs.getInt("co_type");
						String coName = rs.getString("co_name");
						ObjectTypeBean objType = objectTypes.get(coType);

						if (objType == null) {
							objType = new ObjectTypeBean(coType, coName, false);
							objectTypes.put(coType, objType);
						}
						
						ObjectAttributeBean attrType = new ObjectAttributeBean(rs.getString("name"), rs.getInt("type"), rs.getBoolean("optimized"),
								rs.getString("quickname"), rs.getBoolean("multivalue"), coType, rs.getInt("linkedobjecttype"), null,
								rs.getString("foreignlinkattribute"), rs.getString("foreignlinkattributerule"), false, rs.getBoolean("filesystem"));

						objType.addAttributeType(attrType);
					}
				}
				
				public void takeOver(ResultProcessor p) {}
			});
		} catch (SQLException e) {
			throw new DatasourceException("Error while reading object types", e);
		}

		// put into cache
		MCCRCacheHelper.put(ds, objectTypes);

		return objectTypes;
	}

	/**
	 * Get the attribute types for the given object type
	 * @param ds datasource
	 * @param objectType object type
	 * @return list of attribute types
	 * @throws DatasourceException
	 */
	public static Collection<ObjectAttributeBean> getAttributeTypes(MCCRDatasource ds, int objectType) throws DatasourceException {
		return getAttributeTypeMap(ds, objectType).values();
	}

	/**
	 * Get the attribute types for the given object type
	 * @param ds datasource
	 * @param objectType object type
	 * @param optimized true to get only the optimized attribute types, false to get only the not optimized
	 * @return list of attribute types
	 * @throws DatasourceException
	 */
	public static Collection<ObjectAttributeBean> getAttributeTypes(MCCRDatasource ds, int objectType, boolean optimized) throws DatasourceException {
		Collection<ObjectAttributeBean> types = new ArrayList<ObjectAttributeBean>(getAttributeTypes(ds, objectType));
		for (Iterator<ObjectAttributeBean> i = types.iterator(); i.hasNext(); ) {
			ObjectAttributeBean type = i.next();
			if (type.getOptimized() != optimized) {
				i.remove();
			}
		}
		return types;
	}

	/**
	 * Get the map of all attribute types
	 * @param ds datasource
	 * @return map of attribute types
	 * @throws DatasourceException
	 */
	public static Map<String, ObjectAttributeBean> getAttributeTypeMap(MCCRDatasource ds) throws DatasourceException {
		Map<String, ObjectAttributeBean> map = new HashMap<String, ObjectAttributeBean>();
		Map<Integer, ObjectTypeBean> objectTypes = getObjectTypes(ds, false);

		for (ObjectTypeBean objectType : objectTypes.values()) {
			map.putAll(objectType.getAttributeTypesMap());
		}
		return map;
	}

	/**
	 * Get the map of attribute types
	 * @param ds datasource
	 * @param optimized true to only get optimized attribute types, false to only get not optimized attribute types
	 * @return map of attribute types
	 * @throws DatasourceException
	 */
	public static Map<String, ObjectAttributeBean> getAttributeTypesMap(MCCRDatasource ds, boolean optimized) throws DatasourceException {
		Map<String, ObjectAttributeBean> map = new HashMap<String, ObjectAttributeBean>(getAttributeTypeMap(ds));
		for (Iterator<Map.Entry<String, ObjectAttributeBean>> i = map.entrySet().iterator(); i.hasNext(); ) {
			ObjectAttributeBean type = i.next().getValue();
			if (type.getOptimized() != optimized) {
				i.remove();
			}
		}
		return map;
	}

	/**
	 * Get the map of attribute types for the given object type
	 * @param ds datasource
	 * @param objectType object type
	 * @return map of attribute types
	 * @throws DatasourceException
	 */
	public static Map<String, ObjectAttributeBean> getAttributeTypeMap(MCCRDatasource ds, int objectType) throws DatasourceException {
		Map<Integer, ObjectTypeBean> objectTypes = getObjectTypes(ds, false);
		ObjectTypeBean typeBean = objectTypes.get(objectType);

		if (typeBean == null) {
			return Collections.emptyMap();
		} else {
			return typeBean.getAttributeTypesMap();
		}
	}

	/**
	 * Get the map of attribute types for the given object type
	 * @param ds datasource
	 * @param objectType object type
	 * @param optimized true to only get optimized attribute types, false to only get not optimized attribute types
	 * @return map of attribute types
	 * @throws DatasourceException
	 */
	public static Map<String, ObjectAttributeBean> getAttributeTypesMap(MCCRDatasource ds, int objectType, boolean optimized) throws DatasourceException {
		Map<String, ObjectAttributeBean> map = new HashMap<String, ObjectAttributeBean>(getAttributeTypeMap(ds, objectType));
		for (Iterator<Map.Entry<String, ObjectAttributeBean>> i = map.entrySet().iterator(); i.hasNext(); ) {
			ObjectAttributeBean type = i.next().getValue();
			if (type.getOptimized() != optimized) {
				i.remove();
			}
		}
		return map;
	}

	/**
	 * Get the attribute type
	 * @param ds datasource
	 * @param objectType object type
	 * @param name name
	 * @return attribute type or null if not found
	 * @throws DatasourceException
	 */
	public static ObjectAttributeBean getAttributeType(MCCRDatasource ds, int objectType, String name) throws DatasourceException {
		return getAttributeTypeMap(ds, objectType).get(name);
	}

	/**
	 * Get a reusable data files in the given absolute path for a file with given md5 and length.
	 * If no file can be reused, return null.
	 * @param absolutePath absolute storage path
	 * @param md5 md5
	 * @param length length
	 * @return reusable data file or null
	 */
	public static File getReusableDataFile(String absolutePath, String md5, long length) {
		File dir = new File(absolutePath, StoragePathInfo.getDirectory(md5));
		if (!dir.exists() || !dir.isDirectory()) {
			return null;
		}

		final String prefix = md5 + "." + length;

		// get cached filename and check whether the file really exists
		String fileName = MCCRCacheHelper.getFSAttributeFile(absolutePath, prefix);
		if (!ObjectTransformer.isEmpty(fileName)) {
			File cachedFile = new File(dir, fileName);
			if (cachedFile.exists()) {
				return cachedFile;
			}
		}

		// get candidates by listing files in the filesystem
		File[] candidates = dir.listFiles(new FilenameFilter() {
			/* (non-Javadoc)
			 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
			 */
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		});

		if (ObjectTransformer.isEmpty(candidates)) {
			MCCRCacheHelper.putFSAttributeFile(absolutePath, prefix, null);
			return null;
		} else {
			MCCRCacheHelper.putFSAttributeFile(absolutePath, prefix, candidates[0].getName());
			return candidates[0];
		}
	}

	/**
	 * Try to create a link to the source file
	 * @param source source file
	 * @param link link
	 * @throws DatasourceException
	 */
	public static void createLink(File source, File link) throws DatasourceException {
		try {
			Files.createLink(FileSystems.getDefault().getPath(link.getAbsolutePath()), FileSystems.getDefault().getPath(source.getAbsolutePath()));
		} catch (IOException e) {
			throw new DatasourceException("Error while creating link from " + link + " to " + source);
		}
	}

	/**
	 * Move the data from the filesystem into the DB for the given attribute type
	 * @param ds datasource
	 * @param attrType attribute type
	 * @throws DatasourceException
	 */
	public static void moveDataFromFS2DB(MCCRDatasource ds, ObjectAttributeBean attrType) throws DatasourceException {
		final String basepath = ds.getAttributePath();

		if (ObjectTransformer.isEmpty(basepath)) {
			throw new DatasourceException(
					"Error while moving data from filesystem to database for attribute type " + attrType.getName() + ": basepath is not set for the datasource");
		}

		// set column to get data from or write data to
		DBHandle handle = ds.getHandle();

		try {
			// get all records, that need to be updated
			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(handle, "SELECT id FROM " + handle.getContentAttributeName() + " WHERE name = ?", new Object[] { attrType.getName()}, proc);

			// prepare statements
			String selectSQL = "SELECT value_text FROM " + handle.getContentAttributeName() + " WHERE id = ?";
			String updateSQL = null;

			if (attrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG) {
				updateSQL = "UPDATE " + handle.getContentAttributeName() + " SET value_clob = ?, value_text = ?, value_long = ?, value_blob = ? WHERE id = ?";
			} else {
				updateSQL = "UPDATE " + handle.getContentAttributeName() + " SET value_blob = ?, value_text = ?, value_long = ?, value_clob = ? WHERE id = ?";
			}

			for (SimpleResultRow row : proc) {
				int caId = row.getInt("id");
				SimpleResultProcessor attrData = new SimpleResultProcessor();

				DB.query(handle, selectSQL, new Object[] { caId }, attrData);
				for (SimpleResultRow attr : attrData) {
					String filePath = attr.getString("value_text");

					if (ObjectTransformer.isEmpty(filePath)) {
						// no data to move, just set everything to null
						DB.update(handle, updateSQL, new Object[] { null, null, null, null, caId});
					} else {
						// move data to DB
						File dataFile = new File(basepath, filePath);

						if (dataFile.exists()) {
							ByteArrayOutputStream data = new ByteArrayOutputStream();
							FileInputStream in = new FileInputStream(dataFile);

							FileUtil.pooledBufferInToOut(in, data);
							FileUtil.close(in);
							if (attrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG) {
								// string value
								DB.update(handle, updateSQL, new Object[] { data.toString("UTF-8"), null, null, null, caId});
							} else {
								// binary value
								DB.update(handle, updateSQL, new Object[] { data.toByteArray(), null, null, null, caId});
							}

							// remove the file (on commit)
							DB.removeFileOnCommit(handle, dataFile);
						} else {
							throw new IOException("File " + dataFile.getAbsolutePath() + " does not exist.");
						}
					}
				}
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while moving data from filesystem to database for attribute type " + attrType.getName(), e);
		}
	}

	/**
	 * Move the data from the database to the filesystem for the given attribute type
	 * @param ds datasource
	 * @param attrType attribute type
	 * @throws DatasourceException
	 */
	public static void moveDataFromDB2FS(MCCRDatasource ds, ObjectAttributeBean attrType) throws DatasourceException {
		String basepath = ds.getAttributePath();

		if (ObjectTransformer.isEmpty(basepath)) {
			throw new DatasourceException(
					"Error while moving data from filesystem to database for attribute type " + attrType.getName() + ": basepath is not set for the datasource");
		}

		// set column to get data from or write data to
		DBHandle handle = ds.getHandle();

		try {
			// get all records, that need to be updated
			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(handle, "SELECT id FROM " + handle.getContentAttributeName() + " WHERE name = ?", new Object[] { attrType.getName()}, proc);

			// prepare statements
			String selectSQL = "SELECT cm.*, ca.* FROM " + handle.getContentAttributeName() + " ca LEFT JOIN " + handle.getContentMapName()
					+ " cm ON ca.map_id = cm.id WHERE ca.id = ?";

			for (SimpleResultRow row : proc) {
				int caId = row.getInt("id");
				SimpleResultProcessor attrData = new SimpleResultProcessor();

				DB.query(handle, selectSQL, new Object[] { caId }, attrData);
				for (SimpleResultRow attr : attrData) {
					int sortOrder = attr.getInt("sortorder");
					MCCRObject obj = new MCCRObject(ds, attr.getInt("channel_id"), attr.getInt("channelset_id"),
							new ContentId(attr.getInt("obj_type"), attr.getInt("obj_id")));

					obj.id = attr.getInt("map_id");
					FilesystemAttributeValue fsValue = new FilesystemAttributeValue();

					if (attrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG) {
						// string value
						fsValue.setData(attr.getString("value_clob"));
					} else {
						// binary value
						fsValue.setData(ObjectTransformer.getBinary(attr.getObject("value_blob"), null));
					}
					fsValue.saveData(handle, basepath, obj, attrType.getName(), sortOrder, caId, null, null);
				}
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while moving data from filesystem to database for attribute type " + attrType.getName(), e);
		}
	}

	/**
	 * Load the given list of attributes into the given list of objects by using as view SQL statements as possible
	 * @param ds datasource
	 * @param objects list of objects
	 * @param attributes list of attributes
	 * @param checkCaches check caches first. If enough values found in the cache, no prefilling for that attribute is done
	 * @throws DatasourceException
	 */
	public static void batchLoadAttributes(MCCRDatasource ds, List<MCCRObject> objects, List<String> attributes, boolean checkCaches) throws DatasourceException {
		DBHandle handle = ds.getHandle();
		final Map<String, ObjectAttributeBean> attrTypes = getAttributeTypeMap(ds);

		// A map map of all optimized attributes of all object types
		// This is used later for checking if the fetched quick columns
		// are actually valid for certain object types.
		final Map<Object, Map<String, ObjectAttributeBean>> objectTypesOptimizedAttributesMap
				= new HashMap<Object, Map<String, ObjectAttributeBean>>();

		Set<Integer> objectTypes = new HashSet<Integer>();
		for (MCCRObject object : objects) {
			objectTypes.add(object.contentId.objType);
		}

		// Get the optimized attributes for each object type
		for (Integer objectType : objectTypes) {
			objectTypesOptimizedAttributesMap.put(objectType, getAttributeTypesMap(ds, objectType, true));
		}

		// collect names of the prefilled attributes
		final List<String> names = new ArrayList<String>();
		// collect column names of the table contentattribute to load data from
		List<String> columnNames = new ArrayList<String>();
		// collect quick column names of the table contentmap to load data from
		List<String> quickColumNames = new ArrayList<String>();
		// collect optimized attributes
		final List<ObjectAttributeBean> optimizedAttributes = new ArrayList<ObjectAttributeBean>();
		// ... and all non optimized attributes
		final List<String> nonOptimizedNames = new ArrayList<String>();

		// collect sub attributes for link attributes
		Map<String, List<String>> subAttributesMap = new HashMap<String, List<String>>();

		for (String attribute : attributes) {
			if (StringUtils.isEmpty(attribute)) {
				continue;
			}
			String name = null;

			if (attribute.contains(".")) {
				// attributes of linked/foreign linked objects
				String[] splitAttribute = attribute.split("\\.", 2);

				name = splitAttribute[0];

				// name must be the name of a link attribute
				ObjectAttributeBean linkAttribute = getAttributeTypeMap(ds).get(name);

				if (linkAttribute == null) {
					logger.warn("Cannot batch load attribute " + attribute + ": attribute " + name + " does not exist");
					continue;
				} else if (linkAttribute.getAttributetype() != GenticsContentAttribute.ATTR_TYPE_OBJ) {
					logger.warn("Cannot batch load attribute " + attribute + ": attribute " + name + " is not link attribute");
					continue;
				}

				List<String> subAttributes = subAttributesMap.get(name);

				if (subAttributes == null) {
					subAttributes = new Vector<String>();
					subAttributesMap.put(name, subAttributes);
				}
				subAttributes.add(splitAttribute[1]);
			} else {
				name = attribute;
			}
			ObjectAttributeBean attrType = attrTypes.get(name);

			if (!names.contains(name)) {
				if (attrType != null) {

					// now check the caches, if enough attribute values are cached, we will not prefetch the attribute
					if (checkCaches && areEnoughAttributesInCache(ds, objects, name)) {
						continue;
					}

					names.add(name);

					if (attrType.getOptimized()) {
						String quickName = attrType.getQuickname();
						if (!quickColumNames.contains(quickName)) {
							quickColumNames.add(quickName);
						}
						if (!optimizedAttributes.contains(attrType)) {
							optimizedAttributes.add(attrType);
						}
					} else {
						nonOptimizedNames.add(name);
						String columnName = DatatypeHelper.getTypeColumn(attrType.getAttributetype());
	
						if (!ObjectTransformer.isEmpty(columnName) && !columnNames.contains(columnName)) {
							columnNames.add(columnName);
						}
						if (attrType.isFilesystem()) {
							for (String fsColumnName : FilesystemAttributeValue.COLUMN_NAMES) {
								if (!columnNames.contains(fsColumnName)) {
									columnNames.add(fsColumnName);
								}
							}
						}
					}
				} else {
					// log a warning
					logger.warn("Did not find attribute {" + name + "} for prefilling, omiting it.");
				}
			}
		}

		// found no columns to load attributes from
		if (columnNames.isEmpty() && quickColumNames.isEmpty()) {
			return;
		}

		StringBuilder loadSQLPrefix = new StringBuilder();
		loadSQLPrefix.append("SELECT map_id, name, sortorder, ").append(StringUtils.merge(columnNames.toArray(), ", ")).append(" FROM ").append(handle.getContentAttributeName()).append(
				" ");
		loadSQLPrefix.append("WHERE name IN (").append(StringUtils.repeat("?", nonOptimizedNames.size(), ",")).append(") AND map_id IN ");
		String loadSQLPostfix = " ORDER BY map_id, name, sortorder";

		StringBuilder quickLoadSQLPrefix = new StringBuilder();
		quickLoadSQLPrefix.append("SELECT id, ").append(StringUtils.merge(quickColumNames.toArray(), ", ")).append(" FROM ").append(handle.getContentMapName()).append(" ");
		quickLoadSQLPrefix.append("WHERE id IN ");

		// loading the attributes will be done in batches not larger than BATCHLOAD_MAX_OBJECTS objects
		int startIndex = 0;
		int endIndex = Math.min(BATCHLOAD_MAX_OBJECTS, objects.size());
		final Map<Integer, MCCRObject> objectMap = new HashMap<Integer, MCCRObject>(BATCHLOAD_MAX_OBJECTS);
		final List<Integer> unTouchedObjects = new ArrayList<Integer>(BATCHLOAD_MAX_OBJECTS);

		// prepare the map that will hold all attributes, that have been set for an object
		final Map<String, Boolean> setAttributes = new HashMap<String, Boolean>(nonOptimizedNames.size());

		for (String name : nonOptimizedNames) {
			setAttributes.put(name, Boolean.FALSE);
		}

		try {
			while (startIndex < objects.size()) {
				// clear the object map
				objectMap.clear();

				// clear the list of untouched objects
				unTouchedObjects.clear();

				for (int i = startIndex; i < endIndex; i++) {
					MCCRObject object = objects.get(i);
					objectMap.put(object.id, object);
				}

				// load the optimized attributes
				if (!quickColumNames.isEmpty()) {
					StringBuilder loadSQL = new StringBuilder(quickLoadSQLPrefix.toString());
					loadSQL.append("(").append(StringUtils.repeat("?", endIndex - startIndex, ",")).append(")");

					// prepare parameters
					List<Object> params = new Vector<Object>();
					for (int i = startIndex; i < endIndex; i++) {
						MCCRObject object = objects.get(i);
						params.add(object.id);
					}

					// perform the statement
					DB.query(handle, loadSQL.toString(), params.toArray(), new ResultProcessor() {
						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
						 */
						public void takeOver(ResultProcessor p) {}

						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
						 */
						public void process(ResultSet rs) throws SQLException {
							while (rs.next()) {
								int id = rs.getInt("id");
								MCCRObject object = objectMap.get(id);

								for (ObjectAttributeBean attr : optimizedAttributes) {
									// Check if the attribute is actually mapped for this object type
									Map <String, ObjectAttributeBean> attributesForObjectType
											= objectTypesOptimizedAttributesMap.get(object.contentId.objType);

									if (attributesForObjectType != null
											&& attributesForObjectType.containsKey(attr.getName())) {
										Object value = readAttributeValue(rs, attr.getQuickname());
										setAttributeValue(object, attr, Collections.singletonList(value), true);
									}
								}
							}
						}
					});
				}

				// load the not optimized attributes
				if (!columnNames.isEmpty()) {
					// prepare the statement
					StringBuilder loadSQL = new StringBuilder(loadSQLPrefix.toString());

					loadSQL.append("(").append(StringUtils.repeat("?", endIndex - startIndex, ",")).append(")");
					loadSQL.append(loadSQLPostfix);
	
					// prepare parameters
					List<Object> params = new Vector<Object>();
	
					params.addAll(nonOptimizedNames);
					for (int i = startIndex; i < endIndex; i++) {
						MCCRObject object = objects.get(i);
						params.add(object.id);
						unTouchedObjects.add(object.id);
					}
	
					// perform the statement
					DB.query(handle, loadSQL.toString(), params.toArray(), new ResultProcessor() {
	
						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
						 */
						public void takeOver(ResultProcessor p) {}
	
						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
						 */
						public void process(ResultSet rs) throws SQLException {
							ObjectAttributeBean attr = null;
							List<Object> values = new Vector<Object>();
							MCCRObject object = null;
	
							while (rs.next()) {
								int cmId = rs.getInt("map_id");
								String name = rs.getString("name");
	
								if (object != null && attr != null && (object.id != cmId || !StringUtils.isEqual(attr.getName(), name))) {
									// set the current value
									setAttributeValue(object, attr, values, true);
									setAttributes.put(attr.getName(), Boolean.TRUE);
	
									// switch the object (if its new)
									if (object.id != cmId) {
										// first check whether attributes have not yet been set
										for (Map.Entry<String, Boolean> entry : setAttributes.entrySet()) {
											if (!entry.getValue().booleanValue()) {
												// set the attribute to "null"
												setAttributeValue(object, attrTypes.get(entry.getKey()), Collections.emptyList(), true);
											}
										}
	
										object = null;
	
										// reset the map of attributes, that have been set
										for (String attrName : setAttributes.keySet()) {
											setAttributes.put(attrName, Boolean.FALSE);
										}
									}
	
									// switch the attribute
									if (!StringUtils.isEqual(attr.getName(), name)) {
										attr = null;
									}
								}
	
								if (object == null) {
									object = objectMap.get(cmId);
									unTouchedObjects.remove((Integer)cmId);
									values.clear();
								}
								if (attr == null) {
									attr = attrTypes.get(name);
									values.clear();
								}
	
								// get the current value and add it to the values list
								Object value = getAttributeValue(attr, rs);
	
								if (value != null) {
									values.add(value);
								}
							}
	
							// don't forget to add the last value
							if (object != null && attr != null && !values.isEmpty()) {
								setAttributeValue(object, attr, values, true);
								setAttributes.put(attr.getName(), Boolean.TRUE);
	
								// check attributes that have not been set to the last object
								for (Map.Entry<String, Boolean> entry : setAttributes.entrySet()) {
									if (!entry.getValue().booleanValue()) {
										// set the attribute to "null"
										setAttributeValue(object, attrTypes.get(entry.getKey()), Collections.emptyList(), true);
									}
								}
							}
							// now prefetch the untouched objects with null values
							for (Integer objId : unTouchedObjects) {
								MCCRObject obj = objectMap.get(objId);
								for (String name : nonOptimizedNames) {
									setAttributeValue(obj, attrTypes.get(name), Collections.emptyList(), true);
								}
							}
						}
					});
				}


				// if we need to batchload attributes of linked objects, we need to get the linked objects now
				for (Map.Entry<String, List<String>> entry : subAttributesMap.entrySet()) {
					String linkAttribute = entry.getKey();
					List<String> prefetchAttributes = entry.getValue();

					// collect the contentids of the linked objects here
					Set<String> contentIds = new HashSet<String>(objectMap.size());

					for (MCCRObject obj : objectMap.values()) {
						Collection<?> linkedContentIds = ObjectTransformer.getCollection(obj.attributes.get(linkAttribute), Collections.emptyList());

						for (Object linked : linkedContentIds) {
							String linkedContentId = ObjectTransformer.getString(linked, null);

							if (!ObjectTransformer.isEmpty(linkedContentId)) {
								contentIds.add(linkedContentId);
							}
						}
					}

					// use the datasource to get the linked objects
					DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.contentid CONTAINSONEOF data.contentids"));
					Map<String, Object> dataMap = new HashMap<String, Object>();

					dataMap.put("contentids", contentIds);
					filter.addBaseResolvable("data", new MapResolver(dataMap));
					ds.getResult(filter, (String[]) prefetchAttributes.toArray(new String[prefetchAttributes.size()]));
				}

				// calculate next start and end indices
				startIndex = endIndex;
				endIndex = Math.min(startIndex + BATCHLOAD_MAX_OBJECTS, objects.size());
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while batchloading attributes", e);
		}
	}

	/**
	 * Prepare the given list of objects to be updated. Calling this method will
	 * read all attribute data for the objects in a single SQL Statement and
	 * store the results in a threadlocal variable.
	 * When updating any of the objects, the existing data can then be fetched without need for additional SQL statements.
	 * After usage, the threadlocal variable should be cleared by calling {@link #resetPreparedForUpdate()}.
	 * @param objects list of objects, that need to be prepared for update
	 * @throws DatasourceException
	 */
	public static void prepareForUpdate(Collection<? extends Resolvable> objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			dataMaps.set(null);
		} else {
			List<MCCRObject> mccrObjects = new ArrayList<MCCRObject>(objects.size());
			for (Resolvable o : objects) {
				if (o instanceof MCCRObject) {
					mccrObjects.add((MCCRObject)o);
				}
			}
			dataMaps.set(getDataMaps(mccrObjects));
		}
	}

	/**
	 * Reset the threadlocal variable, that was filled by a call to {@link #prepareForUpdate(List)}.
	 */
	public static void resetPreparedForUpdate() {
		dataMaps.set(null);
	}

	/**
	 * Normalize the value to be stored in the given attribute
	 * @param ds datasource
	 * @param object object
	 * @param attrType attribute type
	 * @param value value
	 * @param multivalue true if this method is called for a single value of a multivalue attribute
	 * @return normalized value
	 * @throws  
	 */
	@SuppressWarnings("deprecation")
	protected static Object normalizeValueForStoring(MCCRDatasource ds, MCCRObject object, ObjectAttributeBean attrType, Object value, boolean multivalue) throws DatasourceException {
		// null or empty string is a null value
		if (value == null || "".equals(value)) {
			return null;
		}
		if (attrType != null) {
			if (attrType.getMultivalue() && !multivalue) {
				Collection<?> values = ObjectTransformer.getCollection(value, Collections.emptyList());
				Collection<Object> normalizedValues = new Vector<Object>(values.size());

				for (Object v : values) {
					normalizedValues.add(normalizeValueForStoring(ds, object, attrType, v, true));
				}

				return normalizedValues;
			} else {
				if (attrType.isFilesystem()) {
					if (value != null && !(value instanceof FilesystemAttributeValue)) {
						FilesystemAttributeValue newValue = new FilesystemAttributeValue();

						newValue.setData(value);
						value = newValue;
					}

					return value;
				} else {
					// determine by type
					switch (attrType.getAttributetype()) {
					case GenticsContentAttribute.ATTR_TYPE_TEXT:
						value = ObjectTransformer.getString(value, null);
						// for short-test, we need to check whether the value is too long (and probably have to truncate it)
						if (value instanceof String) {
							StringLengthManipulator lengthManipulator = ds.getHandle().getStringLengthManipulator();
							int oldLength = lengthManipulator.getLength((String) value);

							if (oldLength > 255) {
								value = lengthManipulator.truncate((String) value, 255);
								int newLength = lengthManipulator.getLength((String) value);

								MCCRHelper.logger.warn(
										"Truncated value for attribute " + attrType.getName() + " for object " + object + " from " + oldLength + " to " + newLength);
							}
						}
						if (ObjectTransformer.isEmpty(value)) {
							value = null;
						}
						break;

					case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
						value = ObjectTransformer.getString(value, null);
						break;

					case GenticsContentAttribute.ATTR_TYPE_BINARY:
					case GenticsContentAttribute.ATTR_TYPE_BLOB:
						value = ObjectTransformer.getBinary(value, null);
						break;

					case GenticsContentAttribute.ATTR_TYPE_INTEGER:
						if (value instanceof Date) {
							value = ObjectTransformer.getTimestamp(value, 0);
						} else if (value instanceof Boolean) {
							value = ((Boolean) value).booleanValue() ? 1 : 0;
						} else {
							value = ObjectTransformer.getInteger(value, null);
						}
						break;

					case GenticsContentAttribute.ATTR_TYPE_LONG:
						value = ObjectTransformer.getLong(value, null);
						break;

					case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
						value = ObjectTransformer.getDouble(value, null);
						break;

					case GenticsContentAttribute.ATTR_TYPE_DATE:
						value = ObjectTransformer.getDate(value, null);
						break;

					case GenticsContentAttribute.ATTR_TYPE_OBJ:
						// first get the contentid (as string)
						if (value instanceof Resolvable) {
							value = ((Resolvable) value).get("contentid");
						} else {
							value = ObjectTransformer.getString(value, null);
						}
						break;

					default:
						break;
					}
					return value;
				}
			}
		} else {
			return value;
		}
	}

	/**
	 * Normalize the given value for output
	 * @param ds datasource
	 * @param object object
	 * @param attrType attribute type
	 * @param value attribute value
	 * @param multivalue true when called for a single value of a multivalue attribute
	 * @return normalized value
	 */
	@SuppressWarnings("deprecation")
	protected static Object normalizeValueForOutput(MCCRDatasource ds, MCCRObject object, ObjectAttributeBean attrType, Object value, boolean multivalue) {
		try {
			if (attrType != null) {
				if (attrType.getMultivalue() && !multivalue) {
					Collection<?> values = ObjectTransformer.getCollection(value, Collections.emptyList());
					Collection<Object> normalizedValues = new Vector<Object>(values.size());

					for (Object v : values) {
						normalizedValues.add(normalizeValueForOutput(ds, object, attrType, v, true));
					}

					return normalizedValues;
				} else {
					if (attrType.isFilesystem() && value instanceof FilesystemAttributeValue) {
						FilesystemAttributeValue fsValue = (FilesystemAttributeValue) value;

						switch (attrType.getAttributetype()) {
						case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
							value = getString(ds, object, fsValue);
							break;

						case GenticsContentAttribute.ATTR_TYPE_BLOB:
							value = getBinary(ds, object, fsValue);
							break;
						}
						return value;
					} else {
						// determine by type
						switch (attrType.getAttributetype()) {
						case GenticsContentAttribute.ATTR_TYPE_TEXT:
						case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
							value = ObjectTransformer.getString(value, null);
							break;

						case GenticsContentAttribute.ATTR_TYPE_BINARY:
						case GenticsContentAttribute.ATTR_TYPE_BLOB:
							value = ObjectTransformer.getBinary(value, null);
							break;

						case GenticsContentAttribute.ATTR_TYPE_INTEGER:
							if (value instanceof Date) {
								value = ObjectTransformer.getTimestamp(value, 0);
							} else {
								value = ObjectTransformer.getInteger(value, null);
							}
							break;

						case GenticsContentAttribute.ATTR_TYPE_LONG:
							value = ObjectTransformer.getLong(value, null);
							break;

						case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
							value = ObjectTransformer.getDouble(value, null);
							break;

						case GenticsContentAttribute.ATTR_TYPE_DATE:
							value = ObjectTransformer.getDate(value, null);
							break;

						case GenticsContentAttribute.ATTR_TYPE_OBJ:
							// get the currently set channels
							List<DatasourceChannel> currentChannels = ds.getChannels();
							try {
								// set the channel of the object, because links from objects to other objects of the same channel structure
								// always go to objects in the same channel
								ds.setChannel(object.channelId);
								if (value instanceof ContentId) {
									value = ds.getObjectByContentId((ContentId) value);
								} else if (value != null) {
									value = ds.getObjectByContentId(ObjectTransformer.getString(value, null));
								}
							} catch (DatasourceException e) {
								if (MCCRDatasource.logger.isInfoEnabled()) {
									MCCRDatasource.logger.info("Linked object " + value + " for attribute " + attrType.getName() + " of " + object + " does not exist");
								}
								value = null;
							} finally {
								// reset the previously set channels
								if (!ObjectTransformer.isEmpty(currentChannels)) {
									for (DatasourceChannel channel : currentChannels) {
										ds.setChannel(channel.getId());
									}
								}
							}
							break;

						default:
							break;
						}
						return value;
					}
				}
			} else {
				return value;
			}
		} catch (Exception e) {
			// log an error
			MCCRDatasource.logger.error("Error while transforming value for attribute " + attrType + " for object " + object, e);
			// for filesystem attribute values, we will output null
			if (value instanceof FilesystemAttributeValue) {
				return null;
			}
			return value;
		}
	}


	/**
	 * Get the value as string
	 * @param ds datasource
	 * @param object object
	 * @param fsValue filesystem value object
	 * @return string
	 * @throws IOException
	 */
	protected static String getString(MCCRDatasource ds, MCCRObject object, FilesystemAttributeValue fsValue) throws IOException {
		InputStream in = null;
		try {
			in = fsValue.getInputStream(ds.getHandle(), ds.getAttributePath(), object);
			return StringUtils.readStream(in, "UTF-8");
		} catch (DatasourceException e) {
			throw new IOException(e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {// ignored
			}
		}
	}

	/**
	 * Get the value as binary
	 * @param ds datasource
	 * @param object object
	 * @param fsValue filesystem value object
	 * @return binary
	 * @throws IOException
	 */
	protected static byte[] getBinary(MCCRDatasource ds, MCCRObject object, FilesystemAttributeValue fsValue) throws IOException {
		InputStream in = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			in = fsValue.getInputStream(ds.getHandle(), ds.getAttributePath(), object);
			FileUtil.pooledBufferInToOut(in, out);
		} catch (DatasourceException e) {
			throw new IOException(e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {// ignored
			}
		}
		return out.toByteArray();
	}

	/**
	 * Check, whether anough attributes for the given objects are found in the cache to omit prefetching of that attribute
	 * @param ds datasource
	 * @param objects list of objects
	 * @param attribute attribute to check
	 * @return true if enough attributes were found in the cache (no prefetching will be done), false if prefetching of the attribute is necessary
	 */
	protected static boolean areEnoughAttributesInCache(MCCRDatasource ds, List<MCCRObject> objects, String attribute) {
		int prefetchThreshold = ds.getPrefetchAttributesThreshold();

		// when less attributes shall be prefetched than the threshold says,
		// try to fetch them from the cache first
		if (prefetchThreshold >= 0 && objects.size() <= prefetchThreshold) {
			// get the cache miss threshold
			int prefetchAttributesCacheMissThreshold = ds.getPrefetchAttributesCacheMissThreshold(objects.size());
			// count the cache misses
			int cacheMisses = 0;

			// for all objects, try to get the value from the cache
			for (MCCRObject object : objects) {
				// if no value is stored in the cache, it is a cache miss
				if (MCCRCacheHelper.get(object, attribute) == null) {
					cacheMisses++;
				}
				// if too many cache misses, we need to prefetch
				if (cacheMisses >= prefetchAttributesCacheMissThreshold) {
					return false;
				}
			}

			// we found enough attributes in the cache, so no prefetching
			return true;
		} else {
			// we need to prefetch more attributes than the threshold, so prefetch
			return false;
		}
	}

	/**
	 * Recursively find the channel with given ID
	 * @param nodes list of tree nodes
	 * @param channelId channel id
	 * @return channel or null if not found
	 */
	protected static DatasourceChannel recursiveFindChannel(List<ChannelTreeNode> nodes, int channelId) {
		for (ChannelTreeNode node : nodes) {
			if (node.getChannel().getId() == channelId) {
				return node.getChannel();
			} else {
				DatasourceChannel channel = recursiveFindChannel(node.getChildren(), channelId);

				if (channel != null) {
					return channel;
				}
			}
		}

		return null;
	}

	/**
	 * Recursively build the channel path. The channel path is a list of channels, where the first entry is a root node, every subsequent entry is a
	 * direct child of its predecessor and the last entry is the channel with given id
	 * @param nodes list of channel tree nodes of the current level
	 * @param channelId id of the channel to which the channel path shall lead
	 * @param channelPath channel path built so far, this list will be modified to contain the complete channel path in the end
	 * @return true when we reached the channel with given id, false if not
	 */
	protected static boolean recursiveBuildChannelPath(List<ChannelTreeNode> nodes, int channelId, List<DatasourceChannel> channelPath) {
		// iterate the nodes
		for (ChannelTreeNode node : nodes) {
			// add the current channel into the channel path
			channelPath.add(node.getChannel());

			if (node.getChannel().getId() == channelId) {
				// we found the channel, so we are done
				return true;
			} else {
				// do the recursion step
				if (recursiveBuildChannelPath(node.getChildren(), channelId, channelPath)) {
					// we were on the right track, so we are done now
					return true;
				}
			}

			// the node we tried did not lead to the correct channel, so we try the next one
			// first remove the last node from the channel path
			channelPath.remove(channelPath.size() - 1);
		}

		// none of the nodes lead to the channel we searched for, so we failed to find it
		return false;
	}

	/**
	 * Recursively check for duplicate IDs
	 * @param nodes list of nodes
	 * @param ids currently found ids
	 * @throws DatasourceException
	 */
	protected static void recursiveCheckDuplicateIDs(List<ChannelTreeNode> nodes, List<Integer> ids) throws DatasourceException {
		for (ChannelTreeNode node : nodes) {
			if (ids.contains(node.getChannel().getId())) {
				throw new DatasourceException("Found duplicate channel ID " + node.getChannel().getId());
			} else if (node.getChannel().getId() <= 0) {
				throw new DatasourceException("Found illegal ID " + node.getChannel().getId());
			} else {
				ids.add(node.getChannel().getId());
			}
			recursiveCheckDuplicateIDs(node.getChildren(), ids);
		}
	}

	/**
	 * Make a flat list out of the channel tree. Calculate left and right boundaries for MPTT.
	 * @param tree tree
	 * @return flat list
	 */
	protected static List<MPTTInfo> makeFlat(ChannelTree tree) {
		List<MPTTInfo> flat = new Vector<MPTTInfo>();
		MPTTInfo root = new MPTTInfo(new ChannelTreeNode(new DatasourceChannel(0, "Root")), 1, 0);

		flat.add(root);
		root.right = recursiveMakeFlat(tree.getChildren(), flat, 2);
		return flat;
	}

	/**
	 * Recursive method to make a flat list for the given list of adjacent nodes
	 * @param nodes list of adjacent nodes
	 * @param flat flat list, where the new info shall be added
	 * @param nextLeft next value for the left boundary of the first node in nodes
	 * @return next free boundary value
	 */
	protected static int recursiveMakeFlat(List<ChannelTreeNode> nodes, List<MPTTInfo> flat, int nextLeft) {
		for (ChannelTreeNode node : nodes) {
			MPTTInfo info = new MPTTInfo(node, nextLeft, 0);

			flat.add(info);
			int nextRight = recursiveMakeFlat(node.getChildren(), flat, nextLeft + 1);

			info.right = nextRight;
			nextLeft = nextRight + 1;
		}
		return nextLeft;
	}

	/**
	 * Initialize the object (if not done so before)
	 * @param object object
	 * @return true if the object is persisted, false if not
	 * @throws DatasourceException
	 */
	protected static boolean initialize(MCCRObject object) throws DatasourceException {
		// when an internal id was set, the object is persisted
		if (object.id > 0) {
			return true;
		} else if (object.id == 0) {
			// now we need to check, whether the object exists. The object might be given with channelId/channelsetId/contentId or channelId/channelsetId or
			// channelId/contentId

			try {
				if (object.channelsetId > 0) {
					initWithChannelsetId(object);
				} else if (object.contentId.objType > 0 && object.contentId.objId > 0) {
					initWithContentid(object);
				} else {
					// insufficient data, so the object does not exist
					object.id = -1;
				}

				return object.id > 0;
			} catch (SQLException e) {
				throw new DatasourceException("Error while reading " + object + " from DB", e);
			}
		} else {
			return false;
		}
	}

	/**
	 * Initialize the object based on the given channelsetId
	 * If the object has a channelId set (> 0), the object will be search in that channel, if not, the object will be searched in all currently selected channels.
	 * @throws SQLException
	 */
	protected static void initWithChannelsetId(MCCRObject object) throws SQLException, DatasourceException {
		SimpleResultProcessor dataProc = new SimpleResultProcessor();
		// check with channelId/channelsetId

		// get all selected channels
		List<DatasourceChannel> channels = object.ds.getChannels();

		if (channels.isEmpty()) {
			// object does not exist (there are no channels)
			object.id = -1;
			return;
		}

		if (object.channelId > 0) {
			// search in specific channel

			// do the query
			DB.query(object.ds.getHandle(), "SELECT * FROM contentmap WHERE channel_id = ? AND channelset_id = ?",
					new Object[] { object.channelId, object.channelsetId }, dataProc);
		} else {
			// search in all selected channels
			List<Object> params = new ArrayList<Object>(channels.size() + 1);

			for (DatasourceChannel channel : channels) {
				params.add(channel.getId());
			}
			params.add(object.channelsetId);

			// do the query
			DB.query(object.ds.getHandle(),
					"SELECT * FROM contentmap WHERE channel_id IN (" + StringUtils.repeat("?", channels.size(), ", ") + ") AND channelset_id = ?",
					(Object[]) params.toArray(new Object[params.size()]), dataProc);
		}
		if (dataProc.size() > 0) {
			SimpleResultRow data = dataProc.getRow(1);

			// check whether objType was set as expected
			if (object.contentId.objType > 0 && object.contentId.objType != data.getInt("obj_type")) {
				object.id = -1;
			} else {
				object.id = data.getInt("id");
				object.channelId = data.getInt("channel_id");
				object.contentId.objType = data.getInt("obj_type");
				if (object.contentId.objId <= 0) {
					object.contentId.objId = data.getInt("obj_id");
				} else if (data.getInt("obj_id") != object.contentId.objId) {
					object.storedObjId = data.getInt("obj_id");
				}
				// let the contentId renew
				object.contentId.generateString();

				initializeQuickAttributes(object, data);
			}
		} else {
			// object does not exist
			object.id = -1;
		}
	}

	/**
	 * Initialize the object based on the given objType/objId (= contentid)
	 * @throws SQLException
	 */
	protected static void initWithContentid(MCCRObject object) throws SQLException, DatasourceException {
		SimpleResultProcessor dataProc = new SimpleResultProcessor();
		List<List<DatasourceChannel>> channelPaths = object.ds.getChannelPaths();

		if (channelPaths.isEmpty()) {
			// object does not exist (there are no channels)
			object.id = -1;
			return;
		}

		List<Integer> searchedChannelIds = new ArrayList<Integer>();
		List<Integer> selectedChannelIds = new ArrayList<Integer>();

		for (List<DatasourceChannel> channelPath : channelPaths) {
			for (DatasourceChannel ch : channelPath) {
				searchedChannelIds.add(ch.getId());
			}
			if (!channelPath.isEmpty()) {
				selectedChannelIds.add(channelPath.get(channelPath.size() - 1).getId());
			}
		}
		// when objType and objId are given (together with channelId), we get all entries with that contentid and look whether we find an appropriate for the
		// channel
		List<Object> params = new Vector<Object>();

		params.add(object.contentId.objType);
		params.add(object.contentId.objId);
		params.addAll(searchedChannelIds);
		DB.query(object.ds.getHandle(),
				"SELECT * FROM " + object.ds.getHandle().getContentMapName() + " WHERE obj_type = ? AND obj_id = ? AND channel_id IN ("
				+ StringUtils.repeat("?", searchedChannelIds.size(), ",") + ")",
				(Object[]) params.toArray(new Object[params.size()]),
				dataProc);
		
		if (dataProc.size() > 0) {
			for (SimpleResultRow row : dataProc) {
				if (selectedChannelIds.contains(row.getInt("channel_id"))) {
					// found the object in the channel
					object.id = row.getInt("id");
					object.channelsetId = row.getInt("channelset_id");
					object.channelId = row.getInt("channel_id");
					initializeQuickAttributes(object, row);
					break;
				} else {
					// simply store the channelsetId
					object.channelsetId = row.getInt("channelset_id");
				}
			}
			
			if (object.id == 0) {
				// when the id is still 0, we did not find the object in the correct channel, so get the object using the channelset_id
				initWithChannelsetId(object);
			}
		} else {
			// object does not exist
			object.id = -1;
		}
	}

	/**
	 * Initialize the object by its id
	 * @param object object
	 * @throws DatasourceException
	 */
	protected static void initWithId(MCCRObject object) throws DatasourceException {
		SimpleResultProcessor dataProc = new SimpleResultProcessor();

		try {
			DB.query(object.ds.getHandle(), "SELECT * FROM " + object.ds.getHandle().getContentMapName() + " WHERE id = ?", new Object[] { object.id}, dataProc);
			if (dataProc.size() > 0) {
				SimpleResultRow data = dataProc.getRow(1);

				object.channelId = data.getInt("channel_id");
				object.channelsetId = data.getInt("channelset_id");
				object.contentId = new ContentId(data.getInt("obj_type"), data.getInt("obj_id"));
				initializeQuickAttributes(object, data);
			} else {
				object.id = -1;
			}
		} catch (SQLException e) {
			throw new DatasourceException("Error while initializing object " + object.id, e);
		}
	}

	/**
	 * Initialize the quick attributes from data of the given row
	 * @param data data row
	 * @throws DatasourceException
	 */
	protected static void initializeQuickAttributes(MCCRObject object, SimpleResultRow data) throws DatasourceException {
		Collection<ObjectAttributeBean> attrTypes = MCCRHelper.getAttributeTypes(object.ds, object.contentId.objType);
		Map<String, Object> dataMap = data.getMap();

		for (ObjectAttributeBean attr : attrTypes) {
			// don't overwrite attributes, that are already set
			if (object.attributes.containsKey(attr.getName())) {
				continue;
			}

			// ignore all non-optimized attributes
			if (!attr.getOptimized()) {
				continue;
			}

			// omit if data not present in resultrow
			if (!dataMap.containsKey(attr.getQuickname())) {
				continue;
			}

			try {
				object.setProperty(attr.getName(), dataMap.get(attr.getQuickname()));
			} catch (InsufficientPrivilegesException e) {}
		}
	}

	/**
	 * Initialize the given attribute (if it exists)
	 * @param object object
	 * @param attributeName attribute name
	 * @throws DatasourceException
	 */
	protected static void initializeAttribute(MCCRObject object, String attributeName) throws DatasourceException {
		final ObjectAttributeBean attrType = getAttributeType(object.ds, object.contentId.objType, attributeName);

		if (attrType != null) {
			// get data for the foreignlink attribute
			if (attrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				try {
					// construct the filter to get the foreign link
					StringBuilder filterExpressionString = new StringBuilder("object.obj_type == linkdata.type AND object.");

					filterExpressionString.append(attrType.getForeignlinkattribute()).append(" CONTAINSONEOF data.contentid");
					if (!StringUtils.isEmpty(attrType.getForeignlinkattributerule())) {
						filterExpressionString.append(" AND (").append(attrType.getForeignlinkattributerule()).append(")");
					}
					DatasourceFilter filter = object.ds.createDatasourceFilter(ExpressionParser.getInstance().parse(filterExpressionString.toString()));

					filter.addBaseResolvable("data", object);
					filter.addBaseResolvable("linkdata", new Resolvable() {

						/* (non-Javadoc)
						 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
						 */
						public Object getProperty(String key) {
							return get(key);
						}

						/* (non-Javadoc)
						 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
						 */
						public Object get(String key) {
							if ("type".equals(key)) {
								return attrType.getLinkedobjecttype();
							} else {
								return null;
							}
						}

						/* (non-Javadoc)
						 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
						 */
						public boolean canResolve() {
							return true;
						}
					});
					Collection<Resolvable> linkingObjects = object.ds.getResult(filter, null);

					setAttributeValue(object, attrType, new ArrayList<Object>(linkingObjects), false);
					return;
				} catch (Exception e) {
					if (e instanceof DatasourceException) {
						throw (DatasourceException) e;
					} else {
						throw new DatasourceException("Error while reading attribute {" + attributeName + "} for {" + object + "}", e);
					}
				}
			}

			// get attribute value from cache
			Object cachedValue = MCCRCacheHelper.get(object, attributeName);

			if (cachedValue != null) {
				try {
					if (cachedValue instanceof CacheDummy) {
						cachedValue = null;
					}
					object.setProperty(attributeName, cachedValue);
				} catch (InsufficientPrivilegesException e) {}
				return;
			}

			try {
				// for optimized attributes, request from contentmap
				DBHandle handle = object.ds.getHandle();
				if (attrType.getOptimized()) {
					final List<Object> values = new Vector<Object>();
					final String quickName = attrType.getQuickname();

					DB.query(handle, "SELECT " + quickName + " FROM " + handle.getContentMapName() + " WHERE id = ?", new Object[] {object.id}, new ResultProcessor() {
						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
						 */
						public void process(ResultSet rs) throws SQLException {
							while (rs.next()) {
								Object value = readAttributeValue(rs, quickName);

								if (value != null) {
									values.add(value);
								}
							}
						}

						/* (non-Javadoc)
						 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
						 */
						public void takeOver(ResultProcessor p) {
						}
					});

					setAttributeValue(object, attrType, values, true);

					return;
				} else {
				final String dataColumn = DatatypeHelper.getTypeColumn(attrType.getAttributetype());
				String selectColumns = dataColumn;

				if (attrType.isFilesystem()) {
					selectColumns = "value_text, value_clob, value_long";
				}
				final List<Object> values = new Vector<Object>();

				DB.query(handle, "SELECT " + selectColumns + " FROM " + handle.getContentAttributeName() + " WHERE map_id = ? AND name = ? ORDER BY sortorder",
						new Object[] { object.id, attributeName }, new ResultProcessor() {
					public void takeOver(ResultProcessor p) {}

					/*
					 * (non-Javadoc)
					 * 
					 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
					 */
					public void process(ResultSet rs) throws SQLException {
						while (rs.next()) {
							Object value = getAttributeValue(attrType, rs);

							if (value != null) {
								values.add(value);
							}
						}
					}
				});

				setAttributeValue(object, attrType, values, true);

				return;
				}
			} catch (SQLException e) {
				throw new DatasourceException("Error while reading attribute {" + attributeName + "} for {" + object + "}", e);
			}
		}
	}

	/**
	 * Insert the given object into the datasource
	 * @param obj object
	 * @throws DatasourceException
	 */
	protected static void insert(MCCRObject obj) throws DatasourceException {
		DBHandle handle = obj.ds.getHandle();

		// insert the object
		try {
			SimpleUpdateProcessor keyExtractor = new SimpleUpdateProcessor();

			// get the updatetimestamp once, so that the object and all attribute values will have the same one
			int updateTimestamp = obj.getUpdateTimestamp();

			// prepare statement and data for inserting
			StringBuffer insertSQL = new StringBuffer("INSERT INTO ").append(handle.getContentMapName()).append(
					" (channel_id, channelset_id, obj_id, obj_type, contentId, updatetimestamp");
			List<Object> insertData = new Vector<Object>();

			insertData.add(obj.channelId);
			insertData.add(obj.channelsetId);
			insertData.add(obj.contentId.objId);
			insertData.add(obj.contentId.objType);
			insertData.add(obj.contentId.contentId);
			insertData.add(updateTimestamp);

			// get optimized attributes and store them here
			Collection<ObjectAttributeBean> attrTypes = getAttributeTypes(obj.ds, obj.contentId.objType);

			for (ObjectAttributeBean attrType : attrTypes) {
				// omit non optimized attributes
				if (!attrType.getOptimized()) {
					continue;
				}

				Object value = obj.attributes.get(attrType.getName());

				if (value != null) {
					insertSQL.append(", ").append(attrType.getQuickname());
					insertData.add(value);
				}
			}

			// finish the insert statement
			insertSQL.append(") VALUES (").append(StringUtils.repeat("?", insertData.size(), ",")).append(")");

			DB.update(handle, insertSQL.toString(), (Object[]) insertData.toArray(new Object[insertData.size()]), null, keyExtractor, true, new String[] { "id"});
			SimpleResultProcessor keys = keyExtractor.getGeneratedKeys();

			if (keys.size() == 0) {
				throw new DatasourceException("Error while inserting object" + obj + ": could not extract the autogenerated id");
			}
			Map<String, Object> keyMap = keys.getRow(1).getMap();

			if (keyMap.containsKey("insert_id")) {
				obj.id = ObjectTransformer.getInt(keyMap.get("insert_id"), -1);
			} else if (keyMap.containsKey("generated_key")) {
				obj.id = ObjectTransformer.getInt(keyMap.get("generated_key"), -1);
			} else if (keyMap.containsKey("generated_keys")) {
				obj.id = ObjectTransformer.getInt(keyMap.get("generated_keys"), -1);
			} else if (keyMap.containsKey("id")) {
				obj.id = ObjectTransformer.getInt(keyMap.get("id"), -1);
			}
			if (obj.id <= 0) {
				throw new DatasourceException("Error while inserting object" + obj + ": could not extract the autogenerated id");
			}

			// now insert the attributes
			for (ObjectAttributeBean attrType : attrTypes) {
				if (attrType.getOptimized()) {
					// optimized attributes are not stored in contentattribute table
					continue;
				}
				if (attrType.isFilesystem()) {
					String basePath = obj.ds.getAttributePath();

					if (ObjectTransformer.isEmpty(basePath)) {
						throw new DatasourceException(
								"Error while saving attribute '" + attrType.getName() + "' for object '" + obj + "' into filesystem: basepath is empty");
					}

					// iterate over the values (might be streams)
					List<FilesystemAttributeValue> values = getFSValues(obj, attrType.getName());
					int sortOrder = 0;

					for (FilesystemAttributeValue fsValue : values) {
						if (sortOrder > 0 && !attrType.getMultivalue()) {
							logger.warn("Omit superfluous filesystem attribute value for single value attribute");
							continue;
						}
						fsValue.saveData(handle, basePath, obj, attrType.getName(), sortOrder, -1, null, null);
						sortOrder++;
					}
				} else {
					String column = DatatypeHelper.getTypeColumn(attrType.getAttributetype());

					if (attrType.getMultivalue()) {
						Collection<?> values = ObjectTransformer.getCollection(obj.attributes.get(attrType.getName()), null);

						if (!ObjectTransformer.isEmpty(values)) {
							int sortOrder = 0;

							for (Object value : values) {
								DB.update(handle,
									"INSERT INTO " + handle.getContentAttributeName() + " (map_id, name, sortorder, " + column
									+ ", updatetimestamp) VALUES (?, ?, ?, ?, ?)",
									new Object[] { obj.id, attrType.getName(), sortOrder, value, updateTimestamp });
								sortOrder++;
							}
						}
					} else {
						Object value = obj.attributes.get(attrType.getName());

						if (value != null) {
							DB.update(handle,
								"INSERT INTO " + handle.getContentAttributeName() + " (map_id, name, sortorder, " + column
								+ ", updatetimestamp) VALUES (?, ?, ?, ?, ?)",
								new Object[] { obj.id, attrType.getName(), 0, value, updateTimestamp });
						}
					}
				}
			}

			// clear the results caches
			MCCRCacheHelper.clearResults(obj.ds);
		} catch (SQLException e) {
			throw new DatasourceException("Error while inserting object " + obj, e);
		}
	}

	/**
	 * Do batch insert for the given collection of objects
	 * @param ds datasource
	 * @param objects collection of objects
	 * @param batchInsertForContentmap TODO
	 * @throws DatasourceException
	 */
	protected static void insert(MCCRDatasource ds, Collection<MCCRObject> objects, boolean batchInsertForContentmap) throws DatasourceException {
		DBHandle handle = ds.getHandle();

		// if the database does not support batch updates, we do single updates now
		if (!handle.supportsBatchUpdates()) {
			for (MCCRObject obj : objects) {
				insert(obj);
			}

			return;
		}

		// prepare statement and data for insertion
		StringBuilder insertSQLBuilder = new StringBuilder("INSERT INTO ").append(handle.getContentMapName()).append(
				" (channel_id, channelset_id, obj_id, obj_type, contentId, updatetimestamp");
		Map<String, ObjectAttributeBean> optimizedAttributes = getAttributeTypesMap(ds, true);
		Map<String, ObjectAttributeBean> nonOptimizedAttributes = getAttributeTypesMap(ds, false);
		Map<String, Integer> batchInsertColumns = new HashMap<String, Integer>();
		int colCounter = 6;
		for (ObjectAttributeBean attr : optimizedAttributes.values()) {
			insertSQLBuilder.append(", ").append(attr.getQuickname());
			batchInsertColumns.put(attr.getQuickname(), colCounter++);
		}
		insertSQLBuilder.append(") VALUES (").append(StringUtils.repeat("?", 6 + batchInsertColumns.size(), ",")).append(")");
		String insertSQL = insertSQLBuilder.toString();

		// insert the objects
		// Note: this is not done with a batch, because not all JDBC drivers support returning of generated keys for batches
		try {
			SimpleUpdateProcessor keyExtractor = new SimpleUpdateProcessor();

			Collection<Object[]> paramsColl = new ArrayList<Object[]>();
			for (MCCRObject obj : objects) {
				// get the updatetimestamp once, so that the object and all attribute values will have the same one
				int updateTimestamp = obj.getUpdateTimestamp();
				obj.setUpdateTimestamp(updateTimestamp);

				Object[] params = new Object[6 + batchInsertColumns.size()];
				params[0] = obj.channelId;
				params[1] = obj.channelsetId;
				params[2] = obj.contentId.objId;
				params[3] = obj.contentId.objType;
				params[4] = obj.contentId.contentId;
				params[5] = updateTimestamp;

				for (ObjectAttributeBean attrType : optimizedAttributes.values()) {
					params[batchInsertColumns.get(attrType.getQuickname())] = obj.attributes.get(attrType.getName());
				}
				if (batchInsertForContentmap) {
					paramsColl.add(params);
				} else {
					keyExtractor = new SimpleUpdateProcessor();
					DB.update(handle, insertSQL, params, null, keyExtractor, true, new String[] {"id"});

					SimpleResultProcessor keys = keyExtractor.getGeneratedKeys();

					if (keys.size() != 1) {
						throw new DatasourceException("Error while inserting objects: could not extract the autogenerated ids");
					}
					Map<String, Object> keyMap = keys.getRow(1).getMap();

					if (keyMap.containsKey("insert_id")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("insert_id"), -1);
					} else if (keyMap.containsKey("generated_key")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("generated_key"), -1);
					} else if (keyMap.containsKey("generated_keys")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("generated_keys"), -1);
					} else if (keyMap.containsKey("id")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("id"), -1);
					}
					if (obj.id <= 0) {
						throw new DatasourceException("Error while inserting object" + obj + ": could not extract the autogenerated id");
					}
				}
			}
   
			if (batchInsertForContentmap) {
				DB.batchUpdate(handle, insertSQL, paramsColl, null);
				
				SimpleResultProcessor keys = keyExtractor.getGeneratedKeys();
				
				if (keys.size() != paramsColl.size()) {
					throw new DatasourceException("Error while inserting objects: could not extract the autogenerated ids");
				}
				int rowCounter = 1;
				
				for (MCCRObject obj : objects) {
					Map<String, Object> keyMap = keys.getRow(rowCounter++).getMap();
					
					if (keyMap.containsKey("insert_id")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("insert_id"), -1);
					} else if (keyMap.containsKey("generated_key")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("generated_key"), -1);
					} else if (keyMap.containsKey("generated_keys")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("generated_keys"), -1);
					} else if (keyMap.containsKey("id")) {
						obj.id = ObjectTransformer.getInt(keyMap.get("id"), -1);
					}
					if (obj.id <= 0) {
						throw new DatasourceException("Error while inserting object" + obj + ": could not extract the autogenerated id");
					}
				}
			}

			// now insert the attributes
			PreparedBatchStatement insert = new PreparedBatchStatement(BATCH_INSERT.replace("{tablename}", handle.getContentAttributeName()),
					BATCH_INSERT_TYPES, BATCH_INSERT_COLUMNS);

			for (MCCRObject obj : objects) {
				for (ObjectAttributeBean attrType : nonOptimizedAttributes.values()) {
					if (attrType.isFilesystem()) {
						String basePath = obj.ds.getAttributePath();

						if (ObjectTransformer.isEmpty(basePath)) {
							throw new DatasourceException(
									"Error while saving attribute '" + attrType.getName() + "' for object '" + obj + "' into filesystem: basepath is empty");
						}

						// iterate over the values (might be streams)
						List<FilesystemAttributeValue> values = getFSValues(obj, attrType.getName());
						int sortOrder = 0;

						for (FilesystemAttributeValue fsValue : values) {
							if (sortOrder > 0 && !attrType.getMultivalue()) {
								logger.warn("Omit superfluous filesystem attribute value for single value attribute");
								continue;
							}
							fsValue.saveData(handle, basePath, obj, attrType.getName(), sortOrder, -1, insert, null);
							sortOrder++;
						}
					} else {
						String column = DatatypeHelper.getTypeColumn(attrType.getAttributetype());

						if (attrType.getMultivalue()) {
							Collection<?> values = ObjectTransformer.getCollection(obj.attributes.get(attrType.getName()), null);
							
							if (!ObjectTransformer.isEmpty(values)) {
								int sortOrder = 0;
								
								for (Object value : values) {
									Map<String, Object> data = new HashMap<String, Object>();
									data.put("map_id", obj.id);
									data.put("name", attrType.getName());
									data.put("sortorder", sortOrder);
									data.put(column, value);
									data.put("updatetimestamp", obj.updateTimestamp);
									insert.add(data);
									sortOrder++;
								}
							}
						} else {
							Object value = obj.attributes.get(attrType.getName());
							
							if (value != null) {
								Map<String, Object> data = new HashMap<String, Object>();
								data.put("map_id", obj.id);
								data.put("name", attrType.getName());
								data.put("sortorder", 0);
								data.put(column, value);
								data.put("updatetimestamp", obj.updateTimestamp);
								insert.add(data);
							}
						}
					}
				}
			}
			insert.execute(handle);

			// clear the results caches
			MCCRCacheHelper.clearResults(ds);
		} catch (SQLException e) {
			throw new DatasourceException("Error while inserting objects", e);
		}
	}

	/**
	 * Update the given object in the datasource
	 * @param obj object
	 * @return true if the object was really updated, false if it was unchanged or did not exist
	 * @throws DatasourceException
	 */
	protected static boolean update(MCCRObject obj) throws DatasourceException {
		if (obj.exists()) {
			try {
				int updatetimestamp = obj.getUpdateTimestamp();
				DBHandle handle = obj.ds.getHandle();

				Map<String, List<SimpleResultRow>> oldDataMap = getDataMap(obj);
				boolean modified = false;

				// if the object now has a different object id as currently stored, it is modified
				if (obj.storedObjId > 0 && obj.storedObjId != obj.contentId.objId) {
					modified = true;
				}

				// get all attribute types
				Collection<ObjectAttributeBean> attrTypes = getAttributeTypes(obj.ds, obj.contentId.objType);
				List<ObjectAttributeBean> modifiedOptimized = new Vector<ObjectAttributeBean>();

				for (ObjectAttributeBean attr : attrTypes) {
					boolean modAttr = updateAttribute(obj, attr, oldDataMap.get(attr.getName()), updatetimestamp, null, null, null);

					modified |= modAttr;
					if (modAttr && attr.getOptimized()) {
						// remember the modified optimized attributes
						modifiedOptimized.add(attr);
					}
				}

				// store the modified optimized attributes (and updatetimestamp)
				if (modified) {
					StringBuffer sql = new StringBuffer("UPDATE ").append(handle.getContentMapName()).append(
							" SET obj_id = ?, contentid = ?, updatetimestamp = ?");
					List<Object> params = new Vector<Object>();

					params.add(obj.contentId.objId);
					params.add(obj.contentId.contentId);
					params.add(updatetimestamp);

					for (ObjectAttributeBean attr : modifiedOptimized) {
						sql.append(", ").append(attr.getQuickname()).append(" = ?");
						params.add(obj.attributes.get(attr.getName()));
					}

					sql.append(" WHERE id = ?");
					params.add(obj.id);

					DB.update(obj.ds.getHandle(), sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
					obj.storedObjId = 0;

					// clear the caches of the object
					MCCRCacheHelper.clear(obj, false);

					// clear the results cache
					MCCRCacheHelper.clearResults(obj.ds);
				}

				return modified;
			} catch (SQLException e) {
				throw new DatasourceException("Error while updating object " + obj, e);
			}
		} else {
			return false;
		}
	}

	/**
	 * Do a batch update for the given collection of objects
	 * @param ds datasource
	 * @param objects collection of objects
	 * @return changed objects per channel
	 * @throws DatasourceException
	 */
	protected static Map<Integer, Integer> update(MCCRDatasource ds, Collection<MCCRObject> objects) throws DatasourceException {
		DBHandle handle = ds.getHandle();
		Map<Integer, Integer> changedChannels = new HashMap<Integer, Integer>();

		// if the database does not support batch updates, we do single updates now
		if (!handle.supportsBatchUpdates()) {
			for (MCCRObject obj : objects) {
				boolean updated = update(obj);
				if (updated) {
					changedChannels.put(obj.channelId, ObjectTransformer.getInt(changedChannels.get(obj.channelId), 0) + 1);
				}
			}

			return changedChannels;
		}

		if (stats != null) {
			stats.get(Item.UPDATEOBJ).start();
		}

		PreparedBatchStatement insert = new PreparedBatchStatement(BATCH_INSERT.replace("{tablename}", handle.getContentAttributeName()), BATCH_INSERT_TYPES,
				BATCH_INSERT_COLUMNS);
		PreparedBatchStatement update = new PreparedBatchStatement(BATCH_UPDATE.replace("{tablename}", handle.getContentAttributeName()), BATCH_UPDATE_TYPES,
				BATCH_UPDATE_COLUMNS);
		PreparedBatchStatement delete = new PreparedBatchStatement(BATCH_DELETE.replace("{tablename}", handle.getContentAttributeName()), null, null);

		// collect statements for updating contentmap
		Map<String, PreparedBatchStatement> updateCnMap = new HashMap<String, PreparedBatchStatement>();
		StringBuilder keyBuilder = new StringBuilder();

		try {
			for (MCCRObject obj : objects) {
				if (obj.exists()) {
					try {
						int updatetimestamp = obj.getUpdateTimestamp();
						
						Map<String, List<SimpleResultRow>> oldDataMap = getDataMap(obj);
						boolean modified = false;
						
						// if the object now has a different object id as currently stored, it is modified
						if (obj.storedObjId > 0 && obj.storedObjId != obj.contentId.objId) {
							modified = true;
						}
						
						// get all attribute types
						Collection<ObjectAttributeBean> attrTypes = getAttributeTypes(obj.ds, obj.contentId.objType);
						List<ObjectAttributeBean> modifiedOptimized = new Vector<ObjectAttributeBean>();
						
						keyBuilder.delete(0, keyBuilder.length());
						for (ObjectAttributeBean attr : attrTypes) {
							boolean modAttr = updateAttribute(obj, attr, oldDataMap.get(attr.getName()), updatetimestamp, insert, update, delete);
							
							modified |= modAttr;
							if (modAttr && attr.getOptimized()) {
								// remember the modified optimized attributes
								modifiedOptimized.add(attr);
								keyBuilder.append(attr.getName()).append(",");
							}
						}
						
						// store the modified optimized attributes (and updatetimestamp)
						if (modified) {
							String sqlKey = StringUtils.md5(keyBuilder.toString());
							PreparedBatchStatement updateCnMapStmt = updateCnMap.get(sqlKey);
							
							if (updateCnMapStmt == null) {
								// create statement
								StringBuffer sql = new StringBuffer("UPDATE ").append(handle.getContentMapName()).append(
										" SET obj_id = ?, contentid = ?, updatetimestamp = ?");
								
								for (ObjectAttributeBean attr : modifiedOptimized) {
									sql.append(", ").append(attr.getQuickname()).append(" = ?");
								}
								
								sql.append(" WHERE id = ?");
								
								updateCnMapStmt = new PreparedBatchStatement(sql.toString(), null, null);
								updateCnMap.put(sqlKey, updateCnMapStmt);
							}
							
							List<Object> params = new Vector<Object>();
							
							params.add(obj.contentId.objId);
							params.add(obj.contentId.contentId);
							params.add(updatetimestamp);
							
							for (ObjectAttributeBean attr : modifiedOptimized) {
								params.add(obj.attributes.get(attr.getName()));
							}
							
							params.add(obj.id);
							
							updateCnMapStmt.add((Object[]) params.toArray(new Object[params.size()]));
							obj.storedObjId = 0;
							
							// clear the caches of the object
							MCCRCacheHelper.clear(obj, false);
							
							changedChannels.put(obj.channelId, ObjectTransformer.getInt(changedChannels.get(obj.channelId), 0) + 1);
						}
					} catch (SQLException e) {
						throw new DatasourceException("Error while updating object " + obj, e);
					}
				}
			}
		} finally {
			if (stats != null) {
				stats.get(Item.UPDATEOBJ).stop();
			}
		}

		try {
			if (stats != null) {
				stats.get(Item.BATCHEDSQL).start();
			}

			// perform the batched statements
			for (PreparedBatchStatement updateCnMapStmt : updateCnMap.values()) {
				updateCnMapStmt.execute(handle);
			}

			insert.execute(handle);
			update.execute(handle);
			delete.execute(handle);
		} catch (SQLException e) {
			throw new DatasourceException("Error while updating objects", e);
		} finally {
			if (stats != null) {
				stats.get(Item.BATCHEDSQL).stop();
			}
		}

		if (!changedChannels.isEmpty()) {
			// clear the results cache, if something changed
			MCCRCacheHelper.clearResults(ds);
		}

		return changedChannels;
	}

	/**
	 * Get the map of data maps for the given objects
	 * @param objects list of objects
	 * @return map of data maps
	 * @throws DatasourceException
	 */
	protected static Map<Integer, Map<String, List<SimpleResultRow>>> getDataMaps(List<MCCRObject> objects) throws DatasourceException {
		if (ObjectTransformer.isEmpty(objects)) {
			return Collections.emptyMap();
		}
		try {
			Map<Integer, Map<String, List<SimpleResultRow>>> dataMaps = new HashMap<Integer, Map<String, List<SimpleResultRow>>>();

			MCCRDatasource ds = objects.get(0).ds;
			DBHandle handle = ds.getHandle();
			final SimpleResultProcessor oldData = new SimpleResultProcessor();

			Collection<Integer> ids = new ArrayList<Integer>(objects.size());
			for (MCCRObject obj : objects) {
				ids.add(obj.id);
			}

			// TODO do not sort here, but sort "intelligently" while aggregating data
			DB.query(handle, "SELECT * FROM " + handle.getContentAttributeName() + " WHERE map_id IN (" + StringUtils.repeat("?", ids.size(), ",")
					+ ") ORDER BY map_id, name, sortorder", ids.toArray(new Integer[ids.size()]), oldData);

			// get values of optimized attributes from contentmap table
			final Map<String, ObjectAttributeBean> optimizedTypes = getAttributeTypesMap(ds, true);
			if (!optimizedTypes.isEmpty()) {
				// remove data read from contentattribute, that belong to optimized attributes.
				// if values for optimized attributes are still present in contentattribute, they are
				// left-overs and must be ignored
				for (Iterator<SimpleResultRow> i = oldData.asList().iterator(); i.hasNext();) {
					SimpleResultRow row = i.next();
					if (optimizedTypes.containsKey(row.getString("name"))) {
						i.remove();
					}
				}

				StringBuilder sql = new StringBuilder("SELECT id");
				for (ObjectAttributeBean type : optimizedTypes.values()) {
					sql.append(", ").append(type.getQuickname());
				}
				sql.append(" FROM ").append(handle.getContentMapName()).append(" WHERE id IN (");
				sql.append(StringUtils.repeat("?", ids.size(), ","));
				sql.append(")");
				
				DB.query(handle, sql.toString(), ids.toArray(new Integer[ids.size()]), new ResultProcessor() {
					/* (non-Javadoc)
					 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
					 */
					public void takeOver(ResultProcessor p) {
					}
					
					/* (non-Javadoc)
					 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
					 */
					public void process(ResultSet rs) throws SQLException {
						Map<String, Object> dataMap = new HashMap<String, Object>();
						while (rs.next()) {
							Integer id = rs.getInt("id");
							for (ObjectAttributeBean type : optimizedTypes.values()) {
								dataMap.clear();
								String typeColumn = DatatypeHelper.getTypeColumn(type.getAttributetype());
								String quickColumn = type.getQuickname();
								Object value = rs.getObject(quickColumn);
								if (value != null) {
									dataMap.put("map_id", id);
									dataMap.put("name", type.getName());
									dataMap.put(typeColumn, value);
									oldData.addRow(dataMap);
								}
							}
						}
					}
				});
			}

			// re-organize the results list into nested maps
			for (SimpleResultRow data : oldData) {
				Integer id = data.getInt("map_id");
				String name = data.getString("name");
				Map<String, List<SimpleResultRow>> dataMap = dataMaps.get(id);

				if (dataMap == null) {
					dataMap = new HashMap<String, List<SimpleResultRow>>();
					dataMaps.put(id, dataMap);
				}

				List<SimpleResultRow> attrRows = dataMap.get(name);

				if (attrRows == null) {
					attrRows = new ArrayList<SimpleResultRow>();
					dataMap.put(name, attrRows);
				}
				attrRows.add(data);
			}

			return dataMaps;
		} catch (SQLException e) {
			throw new DatasourceException("Error while getting data for objects", e);
		}
	}

	/**
	 * Get the current attribute values for the object from the DB as Map (keys are the attribute names)
	 * @param obj object
	 * @return attribute data as map
	 * @throws DatasourceException
	 */
	protected static Map<String, List<SimpleResultRow>> getDataMap(MCCRObject obj) throws DatasourceException {
		Map<String, List<SimpleResultRow>> dataMap = null;
		// first try the already prepared data maps
		Map<Integer, Map<String, List<SimpleResultRow>>> storedDataMaps = dataMaps.get();
		if (storedDataMaps != null) {
			dataMap = storedDataMaps.get(obj.id);
		}

		// not prepared, so get the datamap for the object
		if (dataMap == null) {
			dataMap = getDataMaps(Collections.singletonList(obj)).get(obj.id);
		}

		// no data found for the object, so return an empty map
		if (dataMap == null) {
			dataMap = Collections.emptyMap();
		}

		return dataMap;
	}

	/**
	 * Get the attribute value from the prepared data maps (if available)
	 * @param obj object
	 * @param attributeName attribute name
	 * @return attribute value or null if not available
	 * @throws DatasourceException
	 */
	protected static Object getFromDataMap(MCCRObject obj, String attributeName) throws DatasourceException {
		final ObjectAttributeBean attrType = getAttributeType(obj.ds, obj.contentId.objType, attributeName);
		if (attrType == null) {
			return null;
		}

		Map<Integer, Map<String, List<SimpleResultRow>>> storedDataMaps = dataMaps.get();
		if (storedDataMaps == null) {
			return null;
		}
		Map<String, List<SimpleResultRow>> dataMap = storedDataMaps.get(obj.id);
		if (dataMap == null) {
			return null;
		}

		List<SimpleResultRow> preparedData = dataMap.get(attributeName);
		if (ObjectTransformer.isEmpty(preparedData)) {
			return null;
		}

		if (attrType.getMultivalue()) {
			List<Object> values = new ArrayList<Object>();
			for (SimpleResultRow row : preparedData) {
				values.add(getAttributeValue(attrType, row));
			}
			return values;
		} else {
			return getAttributeValue(attrType, preparedData.get(0));
		}
	}

	/**
	 * Update the given attribute for the object. If the collections for parameter sets are null, the statements will be executed immediately.
	 * Otherwise the parameters for executing batch statements will be prepared and added to the collection.
	 * @param object object
	 * @param attr attribute
	 * @param oldData list of current row data (null if data not stored)
	 * @param updatetimestamp update timestamp
	 * @param insertParams PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_INSERT} (if not null)
	 * @param updateParams PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_UPDATE} (if not null)
	 * @param deleteParams PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_DELETE} (if not null)
	 * @return true if data was stored, false if not
	 * @throws DatasourceException
	 */
	protected static boolean updateAttribute(MCCRObject object, ObjectAttributeBean attr, List<SimpleResultRow> oldData, int updatetimestamp,
			PreparedBatchStatement insert, PreparedBatchStatement update, PreparedBatchStatement delete) throws SQLException, DatasourceException {
		// when attribute is not stored in object, we do nothing
		if (!object.attributes.containsKey(attr.getName())) {
			return false;
		}

		boolean modified = false;

		if (ObjectTransformer.isEmpty(oldData)) {
			if (attr.getMultivalue()) {
				Collection<?> values = ObjectTransformer.getCollection(object.attributes.get(attr.getName()), Collections.emptyList());
				int sortOrder = 0;

				for (Object value : values) {
					modified |= storeAttributeValue(object, attr, value, null, sortOrder, updatetimestamp, insert, update, delete);
					sortOrder++;
				}
			} else {
				Object value = object.attributes.get(attr.getName());

				modified |= storeAttributeValue(object, attr, value, null, 0, updatetimestamp, insert, update, delete);
			}
		} else {
			if (attr.getMultivalue()) {
				Collection<?> values = ObjectTransformer.getCollection(object.attributes.get(attr.getName()), Collections.emptyList());
				int sortOrder = 0;

				for (Object value : values) {
					if (oldData.size() > sortOrder) {
						modified |= storeAttributeValue(object, attr, value, oldData.get(sortOrder), sortOrder, updatetimestamp, insert, update, delete);
					} else {
						modified |= storeAttributeValue(object, attr, value, null, sortOrder, updatetimestamp, insert, update, delete);
					}
					sortOrder++;
				}
				while (sortOrder < oldData.size()) {
					modified |= storeAttributeValue(object, attr, null, oldData.get(sortOrder), sortOrder, updatetimestamp, insert, update, delete);
					sortOrder++;
				}
			} else {
				Object value = object.attributes.get(attr.getName());

				modified |= storeAttributeValue(object, attr, value, oldData.get(0), 0, updatetimestamp, insert, update, delete);
				int sortOrder = 1;

				while (sortOrder < oldData.size()) {
					modified |= storeAttributeValue(object, attr, null, oldData.get(sortOrder), sortOrder, updatetimestamp, insert, update, delete);
					sortOrder++;
				}
			}
		}

		// if modified, update the cache
		if (modified) {
			MCCRCacheHelper.put(object, attr.getName(), object.attributes.get(attr.getName()));
		}

		return modified;
	}

	/**
	 * Store an attribute value in the DB. If the collections for parameter sets are null, the statements will be executed immediately.
	 * Otherwise the parameters for executing batch statements will be prepared and added to the collection.
	 * @param object object
	 * @param attr attribute
	 * @param newValue value to store (may be null to remove)
	 * @param oldData old data
	 * @param sortOrder sortorder
	 * @param updatetimestamp update timestamp
	 * @param insert PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_INSERT} (if not null)
	 * @param update PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_UPDATE} (if not null)
	 * @param delete PreparedBatchStatement that will receive the parameter sets for {@link #BATCH_DELETE} (if not null)
	 * @return true if something was changed, false if not
	 * @throws SQLException
	 */
	protected static boolean storeAttributeValue(MCCRObject object, ObjectAttributeBean attr, Object newValue, SimpleResultRow oldData,
			int sortOrder, int updatetimestamp, PreparedBatchStatement insert, PreparedBatchStatement update, PreparedBatchStatement delete) throws SQLException, DatasourceException {
		if (stats != null) {
			stats.get(Item.STOREATTR).start();
		}
		try {
			DBHandle handle = object.ds.getHandle();
			String dataColumn = DatatypeHelper.getTypeColumn(attr.getAttributetype());
			Map<String, Object> data = new HashMap<String, Object>();
			
			if (newValue != null) {
				if (attr.isFilesystem()) {
					if (newValue instanceof FilesystemAttributeValue) {
						FilesystemAttributeValue newFsValue = (FilesystemAttributeValue) newValue;
						int caId = -1;
						if (oldData != null) {
							caId = oldData.getInt("id");
							FilesystemAttributeValue oldFsValue = new FilesystemAttributeValue(oldData.getString("value_text"), oldData.getString("value_clob"),
									oldData.getLong("value_long"));
							
							oldFsValue.setData(newFsValue.getData(), newFsValue.getMD5(), newFsValue.getLength());
							newFsValue = oldFsValue;
						}
						return newFsValue.saveData(handle, object.ds.getAttributePath(), object, attr.getName(), sortOrder, caId, insert, update);
					} else {
						throw new DatasourceException("Cannot save data of " + newValue.getClass() + " into filesystem attribute");
					}
				} else {
					if (oldData == null) {
						// optimized attributes are not stored in contentattribute table
						if (!attr.getOptimized()) {
							// old data does not exist, so we need to insert
							if (insert == null) {
								StringBuffer sqlBuf = new StringBuffer("INSERT INTO ").append(handle.getContentAttributeName());
								
								sqlBuf.append(" (map_id, name, sortorder, updatetimestamp, ").append(dataColumn).append(") VALUES (?, ?, ?, ?, ?)");
								
								DB.update(handle, sqlBuf.toString(), new Object[] { object.id, attr.getName(), sortOrder, updatetimestamp, newValue});
							} else {
								data.clear();
								data.put("map_id", object.id);
								data.put("name", attr.getName());
								data.put("sortorder", sortOrder);
								data.put(dataColumn, newValue);
								data.put("updatetimestamp", updatetimestamp);
								insert.add(data);
							}
						}
						return true;
					} else if (!newValue.equals(normalizeValueForStoring(object.ds, object, attr, oldData.getObject(dataColumn), true))) {
						// optimized attributes are not stored in contentattribute table
						if (!attr.getOptimized()) {
							// old data does exist, but is different, we need to update
							if (update == null) {
								StringBuffer sqlBuf = new StringBuffer("UPDATE ").append(handle.getContentAttributeName());
								
								sqlBuf.append(" SET ").append(dataColumn).append(" = ?, updatetimestamp = ? WHERE id = ?");
								
								DB.update(handle, sqlBuf.toString(), new Object[] { newValue, updatetimestamp, oldData.getInt("id")});
							} else {
								data.clear();
								data.put(dataColumn, newValue);
								data.put("updatetimestamp", updatetimestamp);
								data.put("id", oldData.getInt("id"));
								update.add(data);
							}
						}
						return true;
					} else {
						// old data and new value are equal, so do nothing
						return false;
					}
				}
			} else {
				// new data does not exist
				if (oldData != null) {
					if (attr.isFilesystem()) {
						// remove the file
						DB.removeFileOnCommit(handle, new File(object.ds.getAttributePath(), oldData.getString("value_text")));
					}
					
					// optimized attributes are not stored in contentattribute table
					if (!attr.getOptimized()) {
						// old data exists, remove it
						if (delete == null) {
							StringBuffer sqlBuf = new StringBuffer("DELETE FROM ").append(handle.getContentAttributeName()).append(" WHERE id = ? ");
							
							DB.update(handle, sqlBuf.toString(), new Object[] { oldData.getInt("id")});
						} else {
							delete.add(new Object[] { oldData.getInt("id") });
						}
					}
					return true;
				} else {
					// old data and new value do not exist, so do nothing
					return false;
				}
			}
		} finally {
			if (stats != null) {
				stats.get(Item.STOREATTR).stop();
			}
		}
	}

	/**
	 * Delete the given object from the datasource
	 * @param obj object
	 * @throws DatasourceException
	 */
	protected static void delete(MCCRObject obj) throws DatasourceException {
		if (obj.exists()) {
			try {
				DBHandle handle = obj.ds.getHandle();
				String basePath = obj.ds.getAttributePath();

				// get all filesystem attributes
				Collection<ObjectAttributeBean> attrTypes = getAttributeTypes(obj.ds, obj.contentId.objType);
				List<String> fsAttrs = new ArrayList<String>();

				for (ObjectAttributeBean attrType : attrTypes) {
					if (attrType.isFilesystem()) {
						fsAttrs.add(attrType.getName());
					}
				}

				if (!fsAttrs.isEmpty()) {
					SimpleResultProcessor proc = new SimpleResultProcessor();
					Object[] params = new Object[fsAttrs.size() + 1];
					int counter = 0;

					params[counter++] = obj.id;
					for (String fs : fsAttrs) {
						params[counter++] = fs;
					}
					DB.query(handle,
							"SELECT value_text FROM " + handle.getContentAttributeName() + " WHERE map_id = ? AND name IN ("
							+ StringUtils.repeat("?", fsAttrs.size(), ",") + ")",
							params,
							proc);
					for (SimpleResultRow row : proc) {
						String storagePath = row.getString("value_text");
						DB.removeFileOnCommit(handle, new File(basePath, storagePath));
					}
				}

				DB.update(handle, "DELETE FROM " + handle.getContentMapName() + " WHERE id = ?", new Object[] { obj.id}, null);

				// clear object cache
				MCCRCacheHelper.clear(obj, false);

				// clear the results caches
				MCCRCacheHelper.clearResults(obj.ds);
			} catch (SQLException e) {
				throw new DatasourceException("Error while deleting object " + obj, e);
			}
		}
	}

	/**
	 * Get the attribute values for a filesystem attribute
	 * @param obj object
	 * @param type attribute type
	 * @return values as filesystem values
	 * @throws DatasourceException
	 */
	protected static List<FilesystemAttributeValue> getFSValues(MCCRObject obj, String attrName) throws DatasourceException {
		Collection<?> values = ObjectTransformer.getCollection(obj.attributes.get(attrName), null);

		if (ObjectTransformer.isEmpty(values)) {
			return Collections.emptyList();
		}
		List<FilesystemAttributeValue> retVal = new Vector<FilesystemAttributeValue>();

		for (Object value : values) {
			if (value instanceof FilesystemAttributeValue) {
				retVal.add((FilesystemAttributeValue) value);
			} else {
				throw new DatasourceException("Found value of " + value.getClass() + " in attribute " + attrName + " that stores in the filesystem");
			}
		}
		return retVal;
	}

	/**
	 * Get the attribute value from the current row of the resultset
	 * @param attrType attribute type
	 * @param rs resultset
	 * @return current value (may be null)
	 * @throws SQLException
	 */
	protected static Object getAttributeValue(ObjectAttributeBean attrType, ResultSet rs) throws SQLException {
		String dataColumn = DatatypeHelper.getTypeColumn(attrType.getAttributetype());

		if (attrType.isFilesystem()) {
			return new FilesystemAttributeValue(rs.getString("value_text"), rs.getString("value_clob"), rs.getLong("value_long"));
		} else {
			return readAttributeValue(rs, dataColumn);
		}
	}

	/**
	 * Get the attribute value from the given row
	 * @param attrType attribut type
	 * @param row result row
	 * @return value (may be null)
	 */
	protected static Object getAttributeValue(ObjectAttributeBean attrType, SimpleResultRow row) {
		if (attrType.isFilesystem()) {
			return new FilesystemAttributeValue(row.getString("value_text"), row.getString("value_clob"), row.getLong("value_long"));
		} else {
			return row.getObject(DatatypeHelper.getTypeColumn(attrType.getAttributetype()));
		}
	}

	/**
	 * Read the attribute value from the given recordset
	 * @param rs recordset
	 * @param dataColumn data column
	 * @return attribute value
	 * @throws SQLException
	 */
	protected static Object readAttributeValue(ResultSet rs, String dataColumn) throws SQLException {
			Object value = rs.getObject(dataColumn);

			if (value instanceof Clob) {
				Clob clob = (Clob) value;

				value = clob.getSubString(1, (int) clob.length());
			} else if (value instanceof Blob) {
				Blob blob = (Blob) value;

				// check for very large data
				long length = blob.length();

				if (length > Integer.MAX_VALUE) {
					throw new SQLException("binary data with length > " + Integer.MAX_VALUE + " bytes not supported");
				} else {
					int iLength = (int) length;

					value = blob.getBytes(1, iLength);
				}
			}
			return value;
		}

	/**
	 * Set the attribute value
	 * @param object object
	 * @param attrType attribute type
	 * @param values list of attribute values
	 * @param cache true if the attribute shall also be cached, false if not
	 */
	protected static void setAttributeValue(MCCRObject object, ObjectAttributeBean attrType, List<Object> values, boolean cache) {
		Object value = null;

		if (attrType.getMultivalue()) {
			value = values;
		} else {
			if (values.size() > 0) {
				value = values.get(0);
			}
		}
		try {
			object.setProperty(attrType.getName(), value);
			if (cache) {
				MCCRCacheHelper.put(object, attrType.getName(), value);
			}
		} catch (InsufficientPrivilegesException e) {}
	}

	/**
	 * Inner helper class for the mptt info
	 */
	protected static class MPTTInfo {

		/**
		 * Node
		 */
		protected ChannelTreeNode node;

		/**
		 * Left value
		 */
		protected int left;

		/**
		 * Right value
		 */
		protected int right;

		/**
		 * Create an instance
		 * @param node node
		 * @param left left value
		 * @param right right value
		 */
		public MPTTInfo(ChannelTreeNode node, int left, int right) {
			this.node = node;
			this.left = left;
			this.right = right;
		}

		@Override
		public String toString() {
			return node + "|" + left + "|" + right;
		}
	}

	/**
	 * Comparator for MCCRObjects
	 */
	protected static class ObjectComparator implements Comparator<MCCRObject> {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(MCCRObject o1, MCCRObject o2) {
			return o1.id - o2.id;
		}
	}

	/**
	 * Implementation of the storage path for filesystem attributes stored in an MCCR
	 */
	public static class StoragePathInfo {
		/**
		 * Pattern for the filename is [md5sum].[length].[obj_type].[map_id].[name].[sortorder]
		 */
		protected static Pattern FILE_PATTERN = Pattern.compile("([0-9a-f]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([^\\.]+)\\.([0-9]+)");

		/**
		 * MD5 Sum
		 */
		protected String md5;

		/**
		 * Length
		 */
		protected long length;

		/**
		 * Object type
		 */
		protected int objType;

		/**
		 * Internal ID
		 */
		protected int id;

		/**
		 * Attribute name
		 */
		protected String name;

		/**
		 * Sort Order
		 */
		protected int sortOrder;

		/**
		 * Get the directory for the file
		 * @param md5 md5
		 * @return directory (relative to the base dir)
		 */
		public static String getDirectory(String md5) {
			StringBuilder dir = new StringBuilder();

			dir.append(md5.substring(0, 2));
			dir.append("/");
			dir.append(md5.substring(2, 4));
			dir.append("/");

			return dir.toString();
		}

		/**
		 * Create an instance based on the storage path
		 * @param storagePath storage path
		 * @throws IllegalArgumentException
		 */
		public StoragePathInfo(String storagePath) throws IllegalArgumentException {
			File file = new File(storagePath);
			String fileName = file.getName();
			Matcher matcher = FILE_PATTERN.matcher(fileName);

			if (!matcher.matches()) {
				throw new IllegalArgumentException("[" + fileName + "] does not match the storage path pattern " + FILE_PATTERN.pattern());
			}

			md5 = matcher.group(1);
			length = ObjectTransformer.getLong(matcher.group(2), 0);
			objType = ObjectTransformer.getInt(matcher.group(3), 0);
			id = ObjectTransformer.getInt(matcher.group(4), 0);
			name = matcher.group(5);
			sortOrder = ObjectTransformer.getInt(matcher.group(6), 0);
		}

		/**
		 * Create an instance with given data
		 * @param md5 md5
		 * @param length length
		 * @param objType object type
		 * @param id object id
		 * @param name attribute name
		 * @param sortOrder sort order
		 */
		public StoragePathInfo(String md5, long length, int objType, int id, String name, int sortOrder) {
			this.md5 = md5;
			this.length = length;
			this.objType = objType;
			this.id = id;
			this.name = name;
			this.sortOrder = sortOrder;
		}

		/**
		 * Get the path (relative to the base path)
		 * @return storage path
		 */
		public String getPath() {
			StringBuilder path = new StringBuilder();

			// generate full path by taking the base path and adding two subfolders
			// based on the md5 hash
			path.append(getDirectory(md5));

			// generate the filename
			path.append(md5);
			path.append(".").append(Long.toString(length));
			path.append(".").append(Integer.toString(objType));
			path.append(".").append(Integer.toString(id));
			path.append(".").append(name);
			path.append(".").append(Integer.toString(sortOrder));

			return path.toString();
		}

		/**
		 * Get the attribute name
		 * @return attribute name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get the sortorder
		 * @return sortorder
		 */
		public int getSortOrder() {
			return sortOrder;
		}
	}
}
