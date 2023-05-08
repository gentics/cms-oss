package com.gentics.lib.content;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.content.FilesystemAttributeStatistics.Item;
import com.gentics.lib.content.GenticsContentFactory.StoragePathInfo;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;

/**
 * Class to encapsulate attribute data written into the filesystem
 */
public class FilesystemAttributeValue implements Serializable {
	/**
	 * Logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(FilesystemAttributeValue.class);

	/**
	 * Column names, that are used to store filesystem attribute data
	 */
	public final static String[] COLUMN_NAMES = { "value_text", "value_clob", "value_long"};

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2687135576565969869L;

	/**
	 * Stats Item for measuring statistics for saving filesystem attribute values
	 */
	private static FilesystemAttributeStatistics stats = null;

	/**
	 * Storage path of the data (relative to the basepath)
	 */
	protected String storagePath;

	/**
	 * MD5 hash of the data stored in the file
	 */
	protected String md5;

	/**
	 * Binary length of the data stored in the file
	 */
	protected long length;

	/**
	 * Data of the attribute, if stored and not yet written into the file
	 */
	protected transient Object data;

	/**
	 * If this flag is set, the data is supposed to be also set, but is not
	 * different from the already stored data (md5 sum and length were
	 * identical). In this case, when saving, we only check whether the file
	 * really exists (and create it if not)
	 */
	protected transient boolean checkFileExistence = false;

	/**
	 * If this flag is set, saving the data will not fail when a {@link FileNotFoundException}
	 * is thrown while saving this data instance.
	 */
	protected transient boolean continueIfFileNotFound = false;

	/**
	 * Get the stats item if capturing statistics is enabled, null if not
	 * @return stats item or null
	 */
	public static FilesystemAttributeStatistics getStatistics() {
		return stats;
	}

	/**
	 * Enable/disable statistics
	 * @param enable true to enable, false to disable
	 */
	public static synchronized void enableStatistics(boolean enable) {
		if (enable && stats == null) {
			stats = new FilesystemAttributeStatistics();
		} else if (!enable) {
			stats = null;
		}
	}

	/**
	 * Create an empty instance
	 */
	public FilesystemAttributeValue() {
		this(null, null, -1);
	}

	/**
	 * Create an instance with the given storage path
	 * @param storagePath storage path
	 * @param md5 md5 hash of the binary data
	 * @param length length of the binary data
	 */
	public FilesystemAttributeValue(String storagePath, String md5, long length) {
		this.storagePath = storagePath;
		this.md5 = md5;
		this.length = length;
	}

	/**
	 * Set the flag to continue when a {@link FileNotFoundException} is thrown while saving
	 * @param continueIfFileNotFound true to continue, false to fail
	 */
	public void setContinueIfFileNotFound(boolean continueIfFileNotFound) {
		this.continueIfFileNotFound = continueIfFileNotFound;
	}

	/**
	 * Save the modified data into the storage path
	 * @param handle db handle
	 * @param basePath base path
	 * @param attribute attribute, for which the data shall be saved
	 * @param sortOrder sortorder value
	 * @return true if data was saved, false if no data was set
	 */
	public boolean saveData(DBHandle handle, String basePath, GenticsContentAttribute attribute, int sortOrder) throws DatasourceException {
		if (!ObjectTransformer.isEmpty(data)) {
			// get the parent object
			GenticsContentObject parent = attribute.getParent();

			if (checkFileExistence) {
				InputStream in = null;
				try {
					in = getInputStream(attribute, basePath);
					checkFileExistence(in, basePath, false);
				} catch (IOException e) {
					throw new DatasourceException("Error while saving data for attribute " + attribute.getAttributeName() + " of " + parent, e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) { // ignored
						}
					}
				}
				return false;
			}

			try (Meter m = new Meter(Item.SAVE)) {
				if (!attribute.isMultivalue()) {
					sortOrder = 0;
				}
				int transactionId = 1;

				// check whether a storage path is set
				if (!ObjectTransformer.isEmpty(storagePath)) {
					// get the next transaction ID
					transactionId = GenticsContentFactory.getTransactionId(storagePath) + 1;

					// mark the existing file to be removed, when the transaction is committed
					DB.removeFileOnCommit(handle, new File(basePath, storagePath));
				}

				// construct and set new storagepath
				storagePath = GenticsContentFactory.getStoragePath(parent.getObjectType() + "." + parent.getObjectId(), attribute.getAttributeName(), sortOrder,
						transactionId);

				File valueFile = new File(basePath, storagePath);

				try {
					File parentFile = valueFile.getParentFile();

					parentFile.mkdirs();
					// write the data into the files
					valueFile.createNewFile();
					DB.removeFileOnRollback(handle, valueFile);

					try (FileOutputStream out = new FileOutputStream(valueFile); InputStream in = getInputStream(attribute, basePath)) {
						// if md5 sum or length are not set, we need to calculate them right now
						if (ObjectTransformer.isEmpty(md5) || length <= 0) {
							// we wrap the original stream with a hashing and a counting input stream
							MD5InputStream md5Stream = new MD5InputStream(in);
							CountingInputStream counting = new CountingInputStream(md5Stream);

							FileUtil.pooledBufferInToOut(counting, out);
							md5 = MD5.asHex(md5Stream.hash()).toLowerCase();
							length = counting.getByteCount();
						} else {
							FileUtil.pooledBufferInToOut(in, out);
						}
					}

					// set the storagepath (and other info) into the DB
					try {
						String updateSql = null;
						Object[] updateData = null;

						if (sortOrder == 0) {
							updateSql = "UPDATE " + handle.getContentAttributeName()
									+ " SET value_text = ?, value_clob = ?, value_long = ?, value_blob = ? WHERE contentid = ? AND name = ? AND sortorder IS NULL";
							updateData = new Object[] { storagePath, md5, length, null, parent.getContentId(), attribute.getAttributeName()};
						} else {
							updateSql = "UPDATE " + handle.getContentAttributeName()
									+ " SET value_text = ?, value_clob = ?, value_long = ?, value_blob = ? WHERE contentid = ? AND name = ? AND sortorder = ?";
							updateData = new Object[] { storagePath, md5, length, null, parent.getContentId(), attribute.getAttributeName(), sortOrder};
						}
						int matched = DB.update(handle, updateSql, updateData);

						if (matched == 0) {
							String insertSql = null;
							Object[] insertData = null;

							if (sortOrder == 0) {
								insertSql = "INSERT INTO " + handle.getContentAttributeName()
										+ " (contentid, name, value_text, value_clob, value_long, sortorder) VALUES (?, ?, ?, ?, ?, NULL)";
								insertData = new Object[] { parent.getContentId(), attribute.getAttributeName(), storagePath, md5, length };
							} else {
								insertSql = "INSERT INTO " + handle.getContentAttributeName()
										+ " (contentid, name, value_text, value_clob, value_long, sortorder) VALUES (?, ?, ?, ?, ?, ?)";
								insertData = new Object[] { parent.getContentId(), attribute.getAttributeName(), storagePath, md5, length, sortOrder };
							}
							DB.update(handle, insertSql, insertData);
						}
					} catch (SQLException e) {
						throw new DatasourceException(
								"Error while saving attribute '" + attribute.getAttributeName() + "' for object '" + parent + "' into filesystem", e);
					}

					return true;
				} catch (FileNotFoundException e) {
					if (continueIfFileNotFound) {
						logger.error("Error while saving data into " + valueFile + ". Continuing anyway", e);
						return false;
					} else {
						throw new DatasourceException("Error while saving data into " + valueFile, e);
					}
				} catch (Exception e) {
					throw new DatasourceException("Error while saving data into " + valueFile, e);
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Save the modified data into the storage path
	 * @param handle db handle
	 * @param basePath base path
	 * @param obj object
	 * @param name attribute name
	 * @param sortOrder sortorder value
	 * @param caId ID of the contentattribute record (if one already exists), -1 if no record exists
	 * @param insert PreparedBatchStatement that will receive the parameter sets for insert statements (if not null)
	 * @param update PreparedBatchStatement that will receive the parameter sets for update statements (if not null)
	 * @return true if data was saved, false if no data was set
	 */
	public boolean saveData(DBHandle handle, String basePath, MCCRObject obj, String name, int sortOrder, int caId, PreparedBatchStatement insert,
			PreparedBatchStatement update) throws DatasourceException {
		if (!ObjectTransformer.isEmpty(data)) {
			if (checkFileExistence) {
				InputStream in = null;
				try (Meter m = new Meter(Item.CHECK)) {
					in = getInputStream(handle, basePath, obj);
					checkFileExistence(in, basePath, true);
				} catch (IOException e) {
					throw new DatasourceException("Error while saving data for attribute " + name + " of " + obj, e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) { // ignored
						}
					}
				}
				return false;
			}

			try (Meter saveMeter = new Meter(Item.SAVE)) {
				// get a reusable data file
				File reusableFile = null;
				File newFile = null;

				// check whether a storage path is set
				String oldStoragePath = storagePath;

				// construct the new storage path
				storagePath = new MCCRHelper.StoragePathInfo(md5, length, ObjectTransformer.getInt(obj.get("obj_type"), 0), obj.getId(), name,
						sortOrder).getPath();

				// if we have an old storage path and it is different from the
				// new storage path, we need to remove the old file (if it exists)
				if (!ObjectTransformer.isEmpty(oldStoragePath) && !StringUtils.isEqual(oldStoragePath, storagePath)) {
					File oldFile = new File(basePath, oldStoragePath);
					DB.removeFileOnCommit(handle, oldFile);
				}

				// check whether we can reuse a file
				if (!ObjectTransformer.isEmpty(md5) && length > 0) {
					try (Meter reuseMeter = new Meter(Item.REUSE)) {
						reusableFile = MCCRHelper.getReusableDataFile(basePath, md5, length);
					}
				}

				newFile = new File(basePath, storagePath);

				try {
					if (!newFile.exists()) {
						if (reusableFile != null) {
							try (Meter linkMeter = new Meter(Item.LINK)) {
								MCCRHelper.createLink(reusableFile, newFile);
							}
						} else {
							try (Meter linkMeter = new Meter(Item.LINK)) {
								// create the parent directories
								File parentFile = newFile.getParentFile();

								parentFile.mkdirs();
								// write the data into the files
								newFile.createNewFile();

								try (FileOutputStream out = new FileOutputStream(newFile); InputStream in = getInputStream(handle, basePath, obj)) {
									// if md5 sum or length are not set, we need to calculate them right now
									if (ObjectTransformer.isEmpty(md5) || length <= 0) {
										// we wrap the original stream with a hashing and a counting input stream
										MD5InputStream md5Stream = new MD5InputStream(in);
										CountingInputStream counting = new CountingInputStream(md5Stream);

										FileUtil.pooledBufferInToOut(counting, out);
										md5 = MD5.asHex(md5Stream.hash()).toLowerCase();
										length = counting.getByteCount();
									} else {
										// simply write the file
										FileUtil.pooledBufferInToOut(in, out);
									}
								}
							}
						}
					}
					DB.removeFileOnRollback(handle, newFile);

					// set the storagepath (and other info) into the DB
					try {
						if (caId > 0) {
							// update
							if (update != null) {
								Map<String, Object> data = new HashMap<String, Object>();
								data.put("id", caId);
								data.put("updatetimestamp", obj.getUpdateTimestamp());
								data.put("value_text", storagePath);
								data.put("value_clob", md5);
								data.put("value_long", length);
								update.add(data);
							} else {
								String updateSql = "UPDATE " + handle.getContentAttributeName()
										+ " SET value_text = ?, value_clob = ?, value_long = ?, value_blob = ?, updatetimestamp = ? WHERE id = ?";
								Object[] updateData = new Object[] { storagePath, md5, length, null, obj.getUpdateTimestamp(), caId };
								DB.update(handle, updateSql, updateData,
										Arrays.asList(Types.VARCHAR, Types.LONGVARCHAR, Types.BIGINT, Types.BLOB, Types.INTEGER, Types.INTEGER), null, true);
							}
						} else {
							// insert
							if (insert != null) {
								Map<String, Object> data = new HashMap<String, Object>();
								data.put("map_id", obj.getId());
								data.put("name", name);
								data.put("sortorder", sortOrder);
								data.put("updatetimestamp", obj.getUpdateTimestamp());
								data.put("value_text", storagePath);
								data.put("value_clob", md5);
								data.put("value_long", length);
								insert.add(data);
							} else {
								String insertSql = null;
								Object[] insertData = null;

								insertSql = "INSERT INTO " + handle.getContentAttributeName()
										+ " (map_id, name, value_text, value_clob, value_long, sortorder) VALUES (?, ?, ?, ?, ?, ?)";
								insertData = new Object[] { obj.getId(), name, storagePath, md5, length, sortOrder };
								DB.update(handle, insertSql, insertData);
							}
						}
					} catch (SQLException e) {
						throw new DatasourceException("Error while saving attribute '" + name + "' for object '" + obj + "' into filesystem", e);
					}

					return true;
			} catch (FileNotFoundException e) {
				if (continueIfFileNotFound) {
					logger.error("Error while saving data into " + newFile + ". Continuing anyway", e);
					return false;
				} else {
					throw new DatasourceException("Error while saving data into " + newFile, e);
				}
				} catch (Exception e) {
					throw new DatasourceException("Error while saving data into " + newFile, e);
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Delete the value for the given attribute
	 * @param handle db handle
	 * @param basePath base path
	 * @param attribute attribute
	 * @param sortOrder value sortorder
	 * @throws DatasourceException
	 */
	public void deleteData(DBHandle handle, String basePath, GenticsContentAttribute attribute, int sortOrder) throws DatasourceException {
		GenticsContentObject parent = attribute.getParent();

		if (!attribute.isMultivalue()) {
			sortOrder = 0;
		}
		if (!ObjectTransformer.isEmpty(storagePath)) {
			DB.removeFileOnCommit(handle, new File(basePath, storagePath));
		}

		try {
			String deleteSql = null;
			Object[] deleteData = null;

			if (sortOrder == 0) {
				deleteSql = "DELETE FROM " + handle.getContentAttributeName() + " WHERE contentid = ? AND name = ? AND sortorder IS NULL";
				deleteData = new Object[] { parent.getContentId(), attribute.getAttributeName()};
			} else {
				deleteSql = "DELETE FROM " + handle.getContentAttributeName() + " WHERE contentid = ? AND name = ? AND sortorder = ?";
				deleteData = new Object[] { parent.getContentId(), attribute.getAttributeName(), sortOrder};
			}
			DB.update(handle, deleteSql, deleteData);
		} catch (SQLException e) {
			throw new DatasourceException("Error while deleting attribute '" + attribute.getAttributeName() + "' for object '" + parent + "'", e);
		}
	}

	/**
	 * Get an input stream to read the data
	 * @param attribute attribute
	 * @return input stream
	 */
	public InputStream getInputStream(GenticsContentAttribute attribute, String basePath) throws IOException {
		CNDatasource ds = (CNDatasource) attribute.getParent().getDatasource();

		try {
			return getInputStream(ds.getHandle().getDBHandle(), basePath, null);
		} catch (CMSUnavailableException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Get an input stream to read the data
	 * @param handle db handle
	 * @param basePath basepath
	 * @param obj instance of the mccr object (if getting the stream for a mccr object, may be null)
	 * @return input stream
	 * @throws IOException
	 */
	public InputStream getInputStream(DBHandle handle, String basePath, MCCRObject obj) throws IOException {
		if (data instanceof File) {
			return new FileInputStream((File) data);
		} else if (data instanceof InputStream) {
			return (InputStream) data;
		} else if (data instanceof String) {
			return new ByteArrayInputStream(((String) data).getBytes("UTF-8"));
		} else if (data instanceof byte[]) {
			return new ByteArrayInputStream((byte[]) data);
		} else if (!ObjectTransformer.isEmpty(storagePath)) {
			File valueFile = new File(basePath, storagePath);

			try {
				return new FileInputStream(valueFile);
			} catch (FileNotFoundException e) {
				// the file was not found, this probably means that the file was removed due to an update, and
				// this instance of FilesystemAttributeValue still refers to the the old storage path

				// we will now handle the FileNotFoundException by reading the storagepath from the DB again
				String origStoragePath = storagePath;

				try {
					String selectSql = null;
					Object[] selectData = null;

					if (obj != null) {
						// get the input stream for a mccr object
						MCCRHelper.StoragePathInfo info = new MCCRHelper.StoragePathInfo(storagePath);
						selectSql = "SELECT value_text FROM " + handle.getContentAttributeName() + " WHERE map_id = ? AND name = ? AND sortorder = ?";
						selectData = new Object[] { obj.getId(), info.getName(), info.getSortOrder()};
					} else {
						// get the input stream for a non mccr object
						StoragePathInfo info = new GenticsContentFactory.StoragePathInfo(storagePath);
						if (info.sortorder == 0) {
							selectSql = "SELECT value_text FROM " + handle.getContentAttributeName() + " WHERE name = ? AND contentid = ? AND sortorder IS NULL";
							selectData = new Object[] { info.name, info.contentId };
						} else {
							selectSql = "SELECT value_text FROM " + handle.getContentAttributeName() + " WHERE name = ? AND contentid = ? AND sortorder = ?";
							selectData = new Object[] { info.name, info.contentId, info.sortorder };
						}
					}
					DB.query(handle, selectSql, selectData, new ResultProcessor() {
						public void takeOver(ResultProcessor p) {}
						
						public void process(ResultSet rs) throws SQLException {
							if (rs.next()) {
								// a storagepath was found, so we store it
								storagePath = rs.getString("value_text");
							}
						}
					});
				} catch (SQLException e1) {
					throw new IOExceptionWithCause("Error while getting input stream", e1);
				}

				// when the storagepath from the DB was different, we now try again
				// to get the inputstream from the file
				if (!StringUtils.isEqual(origStoragePath, storagePath)) {
					return new FileInputStream(new File(basePath, storagePath));
				} else {
					throw e;
				}
			}
		} else {
			return null;
		}
	}

	/**
	 * Set the data, together with md5 sum and length.
	 * If md5sum and length are equal to the currently set md5sum and length, the data is not set (because it is assumed to already be stored)
	 * @param data data
	 * @param md5 md5 sum
	 * @param length length
	 */
	public void setData(Object data, String md5, long length) {
		if (!StringUtils.isEmpty(this.md5) && this.length > 0) {
			if (StringUtils.isEqual(this.md5, md5) && this.length == length) {
				if (!ObjectTransformer.isEmpty(storagePath)) {
					// the data is set identical to the already stored value, so in
					// the call to saveData() we will just check whether the file
					// really exists and recreate it, if it does not
					checkFileExistence = true;
					this.data = data;
				}
				return;
			}
		}
		// the data to a new value, so we need to change it in the filesystem
		checkFileExistence = false;
		this.data = data;
		this.md5 = md5;
		this.length = length;
	}

	/**
	 * Set the data, md5 sum and length will be calculated when saving
	 * @param data data
	 */
	public void setData(Object data) {
		try {
			// when the data was set as file or String or byte array, we will now calculate the md5 sum and length
			InputStream in = null;

			if (data instanceof File) {
				in = new FileInputStream((File) data);
			} else if (data instanceof String) {
				in = new ByteArrayInputStream(((String) data).getBytes("UTF-8"));
			} else if (data instanceof byte[]) {
				in = new ByteArrayInputStream((byte[]) data);
			}

			if (in != null) {
				try (Meter m = new Meter(Item.MD5)) {
					// write the data to /dev/null (we actually just want to stream it through the MD5InputStream and CountingInputStream instances)
					OutputStream out = new NullOutputStream();
					MD5InputStream md5Stream = new MD5InputStream(in);
					CountingInputStream counting = new CountingInputStream(md5Stream);
					
					FileUtil.pooledBufferInToOut(counting, out);
					setData(data, MD5.asHex(md5Stream.hash()).toLowerCase(), counting.getByteCount());
				}
			} else {
				setData(data, null, -1);
			}
		} catch (Exception e) {}
	}

	/**
	 * Get the data
	 * @return data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Get the storage path
	 * @return storage path
	 */
	public String getStoragePath() {
		return storagePath;
	}

	/**
	 * Set the storage path
	 * @param storagePath storage path
	 */
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	/**
	 * Check whether this attribute as data set, that is not yet stored
	 * @return true if modified, false if not
	 */
	public boolean isModified() {
		return data != null;
	}

	/**
	 * Get the md5 hash
	 * @return md5 hash
	 */
	public String getMD5() {
		return md5;
	}

	/**
	 * Set the md5 hash
	 * @param md5 md5 hash
	 */
	public void setMD5(String md5) {
		this.md5 = md5;
	}

	/**
	 * Get the length
	 * @return length
	 */
	public long getLength() {
		return length;
	}

	/**
	 * Set the length
	 * @param length length
	 */
	public void setLength(long length) {
		this.length = length;
	}

	/**
	 * Check whether the data file exists, and if not, create it (and log a warning).
	 * The input stream must be closed by the caller
	 * @param in input stream for the data
	 * @param basePath base path
	 * @param canReuse true if files with same content can be reused (hardlink)
	 * @throws IOException
	 * @throws DatasourceException
	 */
	protected void checkFileExistence(InputStream in, String basePath, boolean canReuse) throws IOException, DatasourceException {
		File valueFile = new File(basePath, storagePath);
		if (!valueFile.exists()) {
			// the value file does not exist (but it should), so we create it
			logger.warn("Value file " + valueFile.getAbsolutePath() + " does not exist, trying to recreate it");

			// create the parent directories
			File parentFile = valueFile.getParentFile();

			parentFile.mkdirs();

			// check whether we can reuse a file
			File reusableFile = null;
			if (canReuse) {
				if (!ObjectTransformer.isEmpty(md5) && length > 0) {
					try (Meter m = new Meter(Item.REUSE)) {
						reusableFile = MCCRHelper.getReusableDataFile(basePath, md5, length);
					}
				}
			}

			if (reusableFile != null) {
				try (Meter m = new Meter(Item.LINK)) {
					MCCRHelper.createLink(reusableFile, valueFile);
				}
			} else {
				try (Meter m = new Meter(Item.FILE)) {
					// write the data into the files
					valueFile.createNewFile();
					try (FileOutputStream out = new FileOutputStream(valueFile)) {
						// simply write the file
						FileUtil.pooledBufferInToOut(in, out);
					}
				}
			}
		}
	}

	/**
	 * AutoCloseable class that will collect statistics, if enabled
	 */
	public static class Meter implements AutoCloseable {
		/**
		 * Stats item
		 */
		protected FilesystemAttributeStatistics.Item item;

		/**
		 * Create an instance to collect stats for the given item (if enabled)
		 * @param item stats item
		 */
		public Meter(FilesystemAttributeStatistics.Item item) {
			this.item = item;
			if (stats != null && item != null) {
				stats.get(item).start();
			}
		}

		@Override
		public void close() {
			if (stats != null && item != null) {
				stats.get(item).stop();
			}
		}
	}
}
