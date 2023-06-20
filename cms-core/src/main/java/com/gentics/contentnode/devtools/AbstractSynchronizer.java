package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.model.DefaultValueModel;
import com.gentics.contentnode.devtools.model.ObjectTagModel;
import com.gentics.contentnode.devtools.model.OverviewModel;
import com.gentics.contentnode.devtools.model.TagModel;
import com.gentics.contentnode.devtools.model.TemplateTagModel;
import com.gentics.contentnode.distributed.DistributionSync;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.AbstractModel;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.lib.util.FileUtil;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;
import fi.iki.santtu.md5.MD5OutputStream;
import io.reactivex.Flowable;

/**
 * Abstract synchronizer implementation
 *
 * @param <T> class of the synchronized NodeObject
 * @param <U> class of the Model
 */
public abstract class AbstractSynchronizer <T extends SynchronizableNodeObject, U extends AbstractModel> {
	/**
	 * Name of the structure file
	 */
	public final static String STRUCTURE_FILE = "gentics_structure.json";

	/**
	 * Package synchronizer
	 */
	protected PackageSynchronizer packageSynchronizer;

	/**
	 * Class of handled objects
	 */
	protected Class<T> clazz;

	/**
	 * Class of model objects
	 */
	protected Class<U> modelClazz;

	/**
	 * Base path for objects
	 */
	protected Path basePath;

	/**
	 * Cache for objects
	 */
	protected Cache cache = new Cache();

	/**
	 * Set storing the currently synchronized objects
	 */
	private static Set<SynchronizableNodeObject> objectsCurrentlySynchronized = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Sync object to avoid distributing the clearCache call to other members if running, because it was distributed from another member before
	 */
	private final static DistributionSync sync = new DistributionSync();

	/**
	 * Create an instance with the given package synchronizer
	 * @param packageSynchronizer package synchronizer
	 * @param clazz class of handled objects
	 * @param modelClazz class of models
	 * @param basePath base path
	 * @throws NodeException
	 */
	public AbstractSynchronizer(PackageSynchronizer packageSynchronizer, Class<T> clazz, Class<U> modelClazz, Path basePath) throws NodeException {
		this.packageSynchronizer = packageSynchronizer;
		this.clazz = clazz;
		this.modelClazz = modelClazz;
		this.basePath = basePath;
	}

	/**
	 * Transform the given model into the node object
	 * @param from model
	 * @param to node object (which will be modified)
	 * @param shallow true to create a shallow copy (not containing e.g. tags, ...)
	 * @return modified node object
	 * @throws NodeException
	 */
	protected abstract T transform(U from, T to, boolean shallow) throws NodeException;

	/**
	 * Transform the given node object into the REST model
	 * @param from node object
	 * @param to model
	 * @return model
	 * @throws NodeException
	 */
	protected abstract U transform(T from, U to) throws NodeException;

	/**
	 * Implementation to synchronize the object from the CMS to the filesystem
	 * @param object object
	 * @param folder target folder
	 * @throws NodeException
	 */
	protected abstract void internalSyncToFilesystem(T object, Path folder) throws NodeException;

	/**
	 * Assign all objects in the package (that also exist in the CMS) to the node
	 * @param node node
	 * @throws NodeException
	 */
	protected void assignAll(Node node) throws NodeException {
		for (T object : getObjects()) {
			if (!AbstractContentObject.isEmptyId(object.getId())) {
				assign(object, node, false);
			}
		}
	}

	/**
	 * Assign the object to the node
	 * @param object object
	 * @param node node
	 * @param isNew true iff object is new
	 * @throws NodeException
	 */
	protected abstract void assign(T object, Node node, boolean isNew) throws NodeException;

	/**
	 * Synchronize the object from the CMS to the filesystem
	 * @param object object
	 * @param folder target folder
	 * @throws NodeException
	 */
	public final void syncToFilesystem(T object, Path folder) throws NodeException {
		if (object == null) {
			return;
		}
		if (objectsCurrentlySynchronized.contains(object)) {
			Synchronizer.logger.debug("Omit additional Sync for " + object + " to " + folder + " because it is currently synchronized");
			return;
		}

		try {
			Synchronizer.logger.debug("Sync " + object + " to " + folder);
			objectsCurrentlySynchronized.add(object);
			internalSyncToFilesystem(object, folder);

			// assign synchronized object to all nodes, to which the package is assigned
			for (Node node : packageSynchronizer.getNodes()) {
				assign(object, node, false);
			}

			// clear cache
			clearCache();

			TransactionManager.getCurrentTransaction().addTransactional(new AbstractTransactional() {
				@Override
				public void onDBCommit(Transaction t) throws NodeException {
				}

				@Override
				public boolean onTransactionCommit(Transaction t) {
					Synchronizer.callListeners(object, folder);
					return false;
				}});

		} finally {
			objectsCurrentlySynchronized.remove(object);
			Synchronizer.logger.debug("Finished Sync " + object + " to " + folder);
		}
	}

	/**
	 * Implementation to synchronize the object from the filesystem to the CMS.
	 * This method will be called with a running transaction.
	 * @param object object
	 * @param folder source folder
	 * @param master optional master, when the synchronized object is a localized copy
	 * @return synchronized object
	 * @throws NodeException
	 */
	protected abstract T internalSyncFromFilesystem(T object, Path folder, T master) throws NodeException;

	/**
	 * Synchronize the object from the filesystem to the CMS.
	 * This method will be called with a running transaction.
	 * @param object object
	 * @param folder source folder
	 * @param master optional master, when the synchronized object is a localized copy
	 * @throws NodeException
	 */
	public final void syncFromFilesystem(T object, Path folder, T master) throws NodeException {
		if (object != null && objectsCurrentlySynchronized.contains(object)) {
			Synchronizer.logger.debug("Omit additional Sync for " + folder + " to " + object + " because it is currently synchronized");
			return;
		}

		try {
			if (object != null) {
				objectsCurrentlySynchronized.add(object);
			}
			Synchronizer.logger.debug("Sync " + folder + " to " + (object != null ? object : "new object"));
			T syncedObject = internalSyncFromFilesystem(object, folder, master);

			// assign synchronized object to all nodes, to which the package is assigned
			for (Node node : packageSynchronizer.getNodes()) {
				assign(syncedObject, node, object == null);
			}

			// clear cache
			clearCache();

			TransactionManager.getCurrentTransaction().addTransactional(new AbstractTransactional() {
				@Override
				public void onDBCommit(Transaction t) throws NodeException {
				}

				@Override
				public boolean onTransactionCommit(Transaction t) {
					Synchronizer.callListeners(folder, syncedObject);
					return false;
				}});
		} finally {
			objectsCurrentlySynchronized.remove(object);
			Synchronizer.logger.debug("Finished Sync " + folder + " to " + (object != null ? object : "new object"));
		}
	}

	/**
	 * Find the object, that will be synchronized with the given folder.
	 * This method will be called with a running transaction.
	 * @param folder folder
	 * @return found object or null, if the object does not exist
	 * @throws NodeException
	 */
	public T findObject(Path folder) throws NodeException {
		U model = null;
		try {
			model = parseStructureFile(folder);
		} catch (NodeException e) {
			return null;
		}
		return TransactionManager.getCurrentTransaction().getObject(clazz, model.getGlobalId());
	}

	/**
	 * Get the object, that will be synchronized from the given folder as shallow copy (new object, data only from structure file)
	 * @param folder folder
	 * @return shallow copy or null if structure file not present or cannot be parsed
	 * @throws NodeException
	 */
	public T getShallowCopy(Path folder) throws NodeException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			return transform(parseStructureFile(folder), t.createObject(clazz), true);
		} catch (NodeException e) {
			return null;
		}
	}

	/**
	 * Parse the structure file in the given folder into the Model
	 * @param folder folder
	 * @return model
	 * @throws NodeException when the structure file does not exist or cannot be parsed
	 */
	protected U parseStructureFile(Path folder) throws NodeException {
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException(String.format("Cannot synchronize %s: %s not found", folder, STRUCTURE_FILE));
		}
		try (InputStream in = new FileInputStream(structureFile)) {
			return mapper().readValue(in, modelClazz);
		} catch (IOException e) {
			throw new NodeException(String.format("Error while parsing %s", structureFile), e);
		}
	}

	/**
	 * Synchronize to the filesystem
	 * @param folder target folder
	 * @throws NodeException
	 */
	public void syncToFilesystem(Path folder) throws NodeException {
		T object = findObject(folder);
		if (object == null) {
			return;
		}

		syncToFilesystem(object, folder);
	}

	/**
	 * Synchronize from the filesystem
	 * @param folder source folder
	 * @param master optional master, when the synchronized object is a localized copy
	 * @throws NodeException
	 */
	public void syncFromFilesystem(Path folder, T master) throws NodeException {
		syncFromFilesystem(findObject(folder), folder, master);
	}

	/**
	 * Return true if the given file is handled by this synchronizer, false if not
	 * @param filename filename
	 * @return true iff handled
	 */
	public abstract boolean isHandled(String filename);

	/**
	 * Get the (correct) sync target for the given object (independent of whether the object really is synchronized)
	 * @param object object
	 * @return sync target file (never null)
	 * @throws NodeException in case of errors
	 */
	public File getSyncTarget(T object) throws NodeException {
		String dirName = getSyncTargetName(object);
		File syncTarget = new File(basePath.toFile(), dirName);
		if (!dirName.equals(syncTarget.getName())) {
			throw new NodeException(I18NHelper.get("devtools_package.illegal.filename", dirName));
		}
		try {
			syncTarget.toPath();
		} catch (InvalidPathException e) {
			throw new NodeException(I18NHelper.get("devtools_package.illegal.filename", dirName));
		}
		return syncTarget;
	}

	/**
	 * Get the (correct) directory name for the given object (independent of whether the object really is synchronized)
	 * @param object object
	 * @return sync target directory name
	 * @throws NodeException
	 */
	public abstract String getSyncTargetName(T object) throws NodeException;

	/**
	 * Get the priority of objects handled by this synchronizer. Queued objects will be handled ordered by their priority (ascending, so lowest number is highest priority)
	 * @return priority
	 */
	public int getPriority() {
		return Synchronizer.PRIORITIES.get(clazz);
	}

	/**
	 * Get the current sync location for the given object (identified by its global ID).
	 * This might be different from the result of {@link #getSyncTarget(SynchronizableNodeObject)} if e.g. the object was renamed
	 * @param object object
	 * @return current sync location or null if object is not synchronized
	 * @throws NodeException in case of errors
	 */
	public File getCurrentSyncLocation(T object) throws NodeException {
		if (!basePath.toFile().isDirectory()) {
			return null;
		}
		for (File dir : basePath.toFile().listFiles(file -> file.isDirectory())) {
			File structureFile = new File(dir, STRUCTURE_FILE);
			if (structureFile.isFile()) {
				try {
					U model = parseStructureFile(dir.toPath());
					if (object.getGlobalId().toString().equals(model.getGlobalId())) {
						return dir;
					}
				} catch (NodeException e) {
					Synchronizer.logger.warn(String.format("Error while parsing %s", structureFile), e);
				}
			}
		}
		return null;
	}

	/**
	 * Get the objects in the package
	 * @return list of objects
	 * @throws NodeException
	 */
	public List<T> getObjects() throws NodeException {
		if (!basePath.toFile().exists()) {
			return Collections.emptyList();
		}

		return cache.getObjects();
	}

	/**
	 * Get the list of object paths in this synchronizer. This is the list of
	 * paths direct subdirectories of the synchronizer's base path, that contain
	 * at least a structure file
	 *
	 * @return list of object paths
	 * @throws NodeException
	 */
	public List<Path> getObjectPaths() throws NodeException {
		try {
			if (!basePath.toFile().exists()) {
				return Collections.emptyList();
			}
			return Files.walk(basePath, 1).filter(path -> new File(path.toFile(), STRUCTURE_FILE).isFile()).collect(Collectors.toList());
		} catch (IOException e) {
			throw new NodeException(String.format("Error while getting object paths in %s", basePath), e);
		}
	}

	/**
	 * Get the class of handled objects
	 * @return class of handled objects
	 */
	public Class<T> getClazz() {
		return clazz;
	}

	/**
	 * Remove the object, if contained in the package
	 * @param object object to remove
	 * @param force true to force deletion, even if sync is disabled
	 */
	public void remove(T object, boolean force) throws NodeException {
		File toRemove = getCurrentSyncLocation(object);

		if (toRemove != null && toRemove.isDirectory()) {
			try {
				if (force || Synchronizer.isEnabled()) {
					Synchronizer.deregister(toRemove.toPath());
					FileUtils.deleteDirectory(toRemove);
				}

				clearCache();
			} catch (IOException e) {
				Synchronizer.logger.error(String.format("Error while removing %s", object), e);
			}
		}
	}

	/**
	 * Clear the cache of contained objects
	 */
	public void clearCache() {
		cache.invalidate();

		if (sync.allow()) {
			DistributionUtil.executeOther(new ClearCache());
		}
	}

	/**
	 * Determine the md5 sum of the given string
	 * @param string string
	 * @return md5
	 * @throws NodeException
	 */
	protected String getMD5(String string) throws NodeException {
		try (ByteArrayInputStream in = new ByteArrayInputStream(string.getBytes("UTF8")); MD5OutputStream md5 = new MD5OutputStream(new NullOutputStream())) {
			FileUtil.pooledBufferInToOut(in, md5);
			return MD5.asHex(md5.hash()).toLowerCase();
		} catch (IOException e) {
			throw new NodeException("Error while calculating MD5 of " + string, e);
		}
	}

	/**
	 * Determine the md5 sum of the contents of the given file
	 * @param file file
	 * @return md5
	 * @throws NodeException
	 */
	protected String getMD5(File file) throws NodeException {
		try (FileInputStream in = new FileInputStream(file); MD5InputStream md5 = new MD5InputStream(in); OutputStream out = new NullOutputStream()) {
			FileUtil.pooledBufferInToOut(md5, out);
			return MD5.asHex(md5.hash()).toLowerCase();
		} catch (IOException e) {
			throw new NodeException("Error while calculating MD5 of " + file, e);
		}

	}

	/**
	 * Write the given string into the file (if not already identical)
	 * @param string string to write
	 * @param file target file
	 * @throws NodeException
	 */
	protected void stringToFile(String string, File file) throws NodeException {
		if (file.exists()) {
			if (ObjectTransformer.equals(getMD5(string), getMD5(file))) {
				Synchronizer.logger.debug(file + " is already up to date, not sync'ing");
				return;
			}
		}

		try (InputStream in = new ByteArrayInputStream(string.getBytes("UTF8")); OutputStream out = new FileOutputStream(file)) {
			Synchronizer.logger.debug("Updating " + file);
			FileUtil.pooledBufferInToOut(in, out);
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + string + " to fs", e);
		}
	}

	/**
	 * Prettily write the given object as JSON into the given file
	 * @param object object
	 * @param file JSON file
	 * @throws NodeException
	 */
	protected void jsonToFile(Object object, File file) throws NodeException {
		byte[] json = null;
		String jsonMD5 = null;

		// serialize object into json (and generate MD5)
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				MD5OutputStream md5 = new MD5OutputStream(out);
				JsonGenerator jg = new JsonFactory().createGenerator(md5, JsonEncoding.UTF8)) {
			DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
			jg.setPrettyPrinter(prettyPrinter);
			mapper().writeValue(jg, object);

			json = out.toByteArray();
			jsonMD5 = MD5.asHex(md5.hash()).toLowerCase();
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + object + " to fs", e);
		}

		// if file exists, check whether MD5 sums are identical
		if (file.exists()) {
			String fileMD5 = getMD5(file);

			if (ObjectTransformer.equals(jsonMD5, fileMD5)) {
				Synchronizer.logger.debug(file + " is already up to date, not sync'ing");
				return;
			}
		}

		// write json into file
		try (InputStream in = new ByteArrayInputStream(json); OutputStream out = new FileOutputStream(file)) {
			Synchronizer.logger.debug("Updating " + file);
			FileUtil.pooledBufferInToOut(in, out);
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + object + " to fs", e);
		}
	}

	/**
	 * Get the proposed filename for storing values for the given part
	 * @param part part
	 * @return proposed filename
	 */
	protected String getProposedFilename(Part part) {
		switch (Property.Type.get(part.getPartTypeId())) {
		case STRING:
			return "part." + part.getKeyname() + ".txt";
		case RICHTEXT:
			return "part." + part.getKeyname() + ".html";
		default:
			return "part." + part.getKeyname() + ".json";
		}
	}

	/**
	 * Return true, if the part will store its value in JSON format, false otherwise
	 * @param part part
	 * @return true for JSON
	 */
	protected boolean isJsonFile(Part part) {
		switch (Property.Type.get(part.getPartTypeId())) {
		case STRING:
		case RICHTEXT:
			return false;
		default:
			return true;
		}
	}

	/**
	 * Check whether the filename belongs to a part value
	 * @param filename filename
	 * @return true iff the filename belongs to a part value
	 */
	protected boolean isPartFilename(String filename) {
		return filename.startsWith("part.") && (filename.endsWith(".txt") || filename.endsWith(".html") || filename.endsWith(".json"));
	}

	/**
	 * Transform the given localid to the global id for the given class
	 * @param clazz object class
	 * @param localId local id
	 * @return global id or null
	 * @throws NodeException
	 */
	protected String getGlobalId(Class<? extends NodeObject> clazz, Integer localId) throws NodeException {
		if (localId == null) {
			return null;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObject object = t.getObject(clazz, localId);
		if (object == null) {
			return null;
		} else {
			return object.getGlobalId().toString();
		}
	}

	/**
	 * Transform the given global ID to the local ID for the given class or
	 * create create an entry in the {@code missingreference} table if no object
	 * with this global ID exists yet.
	 *
	 * @param clazz Target object class.
	 * @param targetGlobalId Target objects global ID.
	 * @param partGlobalId Global ID of the values part.
	 * @param referenceName Name for the reference if object is missing.
	 * @param missingReferences Map of missing references.
	 * @return Local ID of the object or {@code null}.
	 * @throws NodeException
	 */
	protected Integer getLocalId(Class<? extends NodeObject> clazz, String targetGlobalId, GlobalId partGlobalId, String referenceName, Map<GlobalId, MissingValueReference> missingReferences) throws NodeException {
		Integer localId = getLocalId(clazz, targetGlobalId);

		if (StringUtils.isNotBlank(targetGlobalId) && localId == null && partGlobalId != null) {
			missingReferences.put(partGlobalId, new MissingValueReference(targetGlobalId, referenceName));
		}

		return localId;
	}

	/**
	 * Add an entry in the  missing references table for the given value ID, reference name and target global ID.
	 *
	 * @param valueGlobalId The values global ID.
	 * @param referenceName The reference name.
	 * @param targetGlobalId The target objects global ID.
	 * @throws NodeException When no value with the given global ID exists, or when the new entry cannot be inserted.
	 */
	protected void addMissingReference(String valueGlobalId, String referenceName, String targetGlobalId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int valueId = DBUtils.select(
			"SELECT id FROM `value` WHERE uuid = ?",
			stmt -> stmt.setString(1, valueGlobalId),
			rs -> rs.next() ? rs.getInt(1) : 0);

		if (valueId == 0) {
			throw new EntityNotFoundException("Value not found", "value.notfound", valueGlobalId);
		}

		try {
			PreparedStatement st = t.prepareStatement(
				"SELECT * FROM `missingreference` WHERE source_tablename = ? AND source_id = ? AND reference_name = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE);

			st.setString(1, "value");
			st.setObject(2, valueId);
			st.setObject(3, referenceName);

			ResultSet res = st.executeQuery();

			if (!res.next()) {
				res.moveToInsertRow();
				res.updateString("source_tablename", "value");
				res.updateObject("source_id", valueId);
				res.updateString("reference_name", referenceName);
				res.updateString("target_uuid", targetGlobalId);
				res.insertRow();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Transform the given globalid to the localid for the given class
	 * @param clazz object class
	 * @param globalId global id
	 * @return local id or null
	 * @throws NodeException
	 */
	protected Integer getLocalId(Class<? extends NodeObject> clazz, String globalId) throws NodeException {
		if (ObjectTransformer.isEmpty(globalId)) {
			return null;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObject object = t.getObject(clazz, globalId);
		if (object == null) {
			return null;
		} else {
			return object.getId();
		}
	}

	/**
	 * Store the contents of the property into the given file
	 * @param value value
	 * @param file file
	 * @throws NodeException
	 */
	protected void storeContents(Value value, File file) throws NodeException {
		if (value.getPartType() instanceof TextPartType) {
			if (value.getValueText() != null) {
				stringToFile(value.getValueText(), file);
			} else {
				stringToFile("", file);
			}
		} else {
			jsonToFile(transform(value, new DefaultValueModel()), file);
		}
	}

	/**
	 * Read the default value contents from the file into the value
	 * @param file file
	 * @param value value
	 * @param json true if the value shall be read as JSON, false for text
	 * @param missingReferences Map for missing references.
	 * @throws NodeException
	 */
	protected void readContents(File file, Value value, boolean json, Map<GlobalId, MissingValueReference> missingReferences) throws NodeException {
		if (!json) {
			try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				FileUtil.pooledBufferInToOut(in, out);
				value.setValueText(out.toString("UTF8"));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		} else {
			try (InputStream in = new FileInputStream(file)) {
				transform(mapper().readValue(in, DefaultValueModel.class), value, missingReferences);
			} catch (IOException e) {
				throw new NodeException("Error while parsing " + file, e);
			}
		}
	}

	/**
	 * Transform the value to the REST model
	 * @param from value
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected DefaultValueModel transform(Value from, DefaultValueModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		return Value.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into a value
	 * @param from REST model
	 * @param to value
	 * @param missingReferences Map for missing references.
	 * @return value
	 * @throws NodeException
	 */
	protected Value transform(DefaultValueModel from, Value to, Map<GlobalId, MissingValueReference> missingReferences) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		Property property = to.getPartType().toProperty();
		GlobalId partGlobalId = to.getPart().getGlobalId();

		property.setStringValue(from.getStringValue());
		property.setStringValues(from.getStringValues());
		property.setBooleanValue(from.getBooleanValue());
		property.setFileId(getLocalId(ContentFile.class, from.getFileId(), partGlobalId, "file", missingReferences));
		property.setFolderId(getLocalId(Folder.class, from.getFolderId(), partGlobalId, "folder", missingReferences));
		property.setImageId(getLocalId(ImageFile.class, from.getImageId(), partGlobalId, "image", missingReferences));
		property.setNodeId(getLocalId(Node.class, from.getNodeId(), partGlobalId, "node", missingReferences));
		property.setPageId(getLocalId(Page.class, from.getPageId(), partGlobalId, "page", missingReferences));
		property.setTemplateId(getLocalId(Template.class, from.getTemplateId(), partGlobalId, "template", missingReferences));
		property.setContentTagId(getLocalId(ContentTag.class, from.getContentTagId(), partGlobalId, "contenttag", missingReferences));
		property.setTemplateTagId(getLocalId(TemplateTag.class, from.getTemplateTagId(), partGlobalId, "templatetag", missingReferences));
		property.setDatasourceId(getLocalId(Datasource.class, from.getDatasourceId(), partGlobalId, "datasource", missingReferences));

		switch (property.getType()) {
		case SELECT:
		case MULTISELECT:
			property.setSelectedOptions(from.getOptions());
			break;
		case DATASOURCE:
			property.setOptions(from.getOptions());
			break;
		case OVERVIEW:
			if (to.getContainer() instanceof Tag) {
				property.setOverview(transform(from.getOverview(), property.getOverview()));
			}
			break;
		default:
			break;
		}

		to.getPartType().fromProperty(property);

		return to;
	}

	/**
	 * Transform the given tag into its REST model
	 * @param from tag
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected TagModel transform(Tag from, TagModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return Tag.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into a tag
	 * @param from REST model
	 * @param to tag
	 * @return tag
	 * @throws NodeException
	 */
	protected Tag transform(TagModel from, Tag to) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		String constructId = from.getConstructId();
		Construct construct = t.getObject(Construct.class, constructId);
		if (construct == null) {
			throw new NodeException("Construct " + constructId + " does not exist");
		}
		to.setConstructId(construct.getId());

		String globalId = from.getGlobalId();
		if (!ObjectTransformer.isEmpty(globalId)) {
			to.setGlobalId(new GlobalId(globalId));
		}

		to.setName(from.getName());

		to.setEnabled(from.isActive());

		return to;
	}

	/**
	 * Transform the given templatetag into its REST model
	 * @param from templatetag
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected TemplateTagModel transform(TemplateTag from, TemplateTagModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return TemplateTag.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into a templatetag
	 * @param from REST model
	 * @param to templatetag
	 * @return templatetag
	 * @throws NodeException
	 */
	protected TemplateTag transform(TemplateTagModel from, TemplateTag to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		transform((TagModel)from, (Tag)to);

		to.setPublic(from.isEditableInPage());
		to.setMandatory(from.isMandatory());

		return to;
	}

	/**
	 * Transform the given objecttag into its REST model
	 * @param from objecttag
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected ObjectTagModel transform(ObjectTag from, ObjectTagModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		return ObjectTag.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into the objecttag
	 * @param from REST model
	 * @param to objecttag
	 * @return objecttag
	 * @throws NodeException
	 */
	protected ObjectTag transform(ObjectTagModel from, ObjectTag to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		transform((TagModel)from, (Tag)to);

		return to;
	}

	/**
	 * Transform the given overview into its REST model
	 * @param from overview
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected OverviewModel transform(Overview from, OverviewModel to) throws NodeException {
		return com.gentics.contentnode.object.Overview.REST2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into the overview
	 * @param from REST model
	 * @param to overview
	 * @return overview
	 * @throws NodeException
	 */
	protected Overview transform(OverviewModel from, Overview to) throws NodeException {
		if (from != null) {
			to.setListType(from.getListType());
			to.setMaxItems(from.getMaxItems());
			to.setOrderBy(from.getOrderBy());
			to.setOrderDirection(from.getOrderDirection());
			to.setRecursive(from.isRecursive());
			to.setSelectType(from.getSelectType());
			to.setSource(from.getSource());

			Class<? extends NodeObject> clazz = getSelectionClass(from.getSelectType(), from.getListType());

			if (from.getSelection() != null) {
				if (to.getSelectedNodeItemIds() != null) {
					to.setSelectedNodeItemIds(Flowable.fromIterable(from.getSelection()).map(item -> {
						int nodeId = ObjectTransformer.getInt(getLocalId(Node.class, item.getNodeId()), 0);
						int id = ObjectTransformer.getInt(getLocalId(clazz, item.getId()), 0);
						return new NodeIdObjectId(nodeId, id);
					}).filter(item -> item.getObjectId() > 0).toList().blockingGet());
				} else if (to.getSelectedItemIds() != null) {
					to.setSelectedItemIds(Flowable.fromIterable(from.getSelection()).map(item -> {
						return ObjectTransformer.getInt(getLocalId(clazz, item.getId()), 0);
					}).filter(id -> id > 0).toList().blockingGet());
				} 
			}
		}
		return to;
	}

	/**
	 * Get the class of objects, which are selected in an overview (not necessarily the objects, which are listed in the overview)
	 * @param selectType select type
	 * @param listType list type
	 * @return object class (may be null)
	 */
	protected Class<? extends NodeObject> getSelectionClass(SelectType selectType, ListType listType) {
		// allow for no selection type to be set
		// this can be on purpose (i.e. selection type should only be determined when editing a tag in a page)
		// or if the tag has just been added to a template
		if (selectType == null) return null;

		switch (selectType) {
		case FOLDER:
			return Folder.class;
		case MANUAL:
			// allow for no listType setting
			if (listType == null) return null;

			switch (listType) {
			case FILE:
				return com.gentics.contentnode.object.File.class;
			case FOLDER:
				return Folder.class;
			case IMAGE:
				return ImageFile.class;
			case PAGE:
				return Page.class;
			default:
				return null;
			}
		default:
			return null;
		}
	}

	/**
	 * Task for clearing the synchronizer cache
	 */
	public static class ClearCache implements Runnable, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -7151415166268972857L;

		@Override
		public void run() {
			try (DistributionSync.Trx trx = sync.get()) {
				Synchronizer.clearCache();
			}
		}
	}

	/**
	 * Inner class for encapsulation of the cache (IDs of synchronized objects and shallow copies of not yet synchronized objects)
	 */
	protected class Cache {
		/**
		 * Cache for object IDs
		 */
		protected Set<Integer> objectIds = Collections.synchronizedSet(new HashSet<>());

		/**
		 * Cached shallow copies of object that are not yet synchronized with the cms
		 */
		protected Set<T> shallowCopies = Collections.synchronizedSet(new HashSet<>());

		/**
		 * Flag to mark whether the cache is currently valid
		 */
		protected boolean valid = false;

		/**
		 * Invalidate the cache
		 */
		protected void invalidate() {
			this.valid = false;
		}

		/**
		 * Get the objects
		 * @return list of objects (synchronized and not synchronized)
		 * @throws NodeException in case of errors
		 */
		protected List<T> getObjects() throws NodeException {
			List<T> objects = new ArrayList<>();

			Set<Integer> currentIds = new HashSet<>(objectIds);
			Set<T> currentShallowCopies = new HashSet<>(shallowCopies);

			// if the cache is believed to be valid, we check whether we can still read all CMS objects
			// if not, at least one of the CMS objects must have been deleted, so we invalidate the cache
			if (valid && TransactionManager.getCurrentTransaction().getObjects(clazz, currentIds).size() != currentIds.size()) {
				valid = false;
			}

			if (!valid) {
				currentIds.clear();
				currentShallowCopies.clear();
				for (Path path : getObjectPaths()) {
					T existing = findObject(path);
					if (existing != null) {
						currentIds.add(existing.getId());
					} else {
						T shallowCopy = getShallowCopy(path);
						if (shallowCopy != null) {
							currentShallowCopies.add(shallowCopy);
						}
					}
				}

				objectIds.clear();
				objectIds.addAll(currentIds);
				shallowCopies.clear();
				shallowCopies.addAll(currentShallowCopies);
				valid = true;
			}

			objects.addAll(currentShallowCopies);
			objects.addAll(TransactionManager.getCurrentTransaction().getObjects(clazz, currentIds));

			return objects;
		}
	}
}
