package com.gentics.contentnode.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.object.cr.CrFragmentEntryWrapper;
import com.gentics.contentnode.rest.model.CRElasticsearchModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.TagmapEntryInconsistencyModel;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Class for content repositories
 */
@SuppressWarnings("serial")
@TType(ContentRepository.TYPE_CONTENTREPOSITORY)
public abstract class ContentRepository extends AbstractContentObject implements SynchronizableNodeObject, NamedNodeObject {

	/**
	 * TType for contentrepositories in general
	 */
	public final static int TYPE_CONTENTREPOSITORIES = 10207;

	/**
	 * TType for contentrepository
	 */
	public final static int TYPE_CONTENTREPOSITORY = 10208;

	/**
	 * Constant for the datacheck status "Unchecked"
	 */
	public final static int DATACHECK_STATUS_UNCHECKED = -1;

	/**
	 * Constant for the datacheck status "Error"
	 */
	public final static int DATACHECK_STATUS_ERROR = 0;

	/**
	 * Constant for the datacheck status "OK"
	 */
	public final static int DATACHECK_STATUS_OK = 1;

	/**
	 * Constant for the datacheck status "Running"
	 */
	public final static int DATACHECK_STATUS_RUNNING = 2;

	/**
	 * Constant for the datacheck status "Queued"
	 */
	public final static int DATACHECK_STATUS_QUEUED = 3;

	/**
	 * Maximum name length
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Function that transforms the node model into the given rest model
	 */
	public final static BiFunction<ContentRepository, ContentRepositoryModel, ContentRepositoryModel> NODE2REST = (nodeCR, cr) -> {
		cr.setId(nodeCR.getId());
		cr.setGlobalId(nodeCR.getGlobalId() != null ? nodeCR.getGlobalId().toString() : null);
		cr.setName(nodeCR.getName());
		cr.setCrType(nodeCR.getCrType());
		cr.setDbType(nodeCR.getDbType());
		cr.setUsername(nodeCR.getUsername());
		if (nodeCR.isPasswordProperty()) {
			cr.setPasswordType(ContentRepositoryModel.PasswordType.property);
			cr.setPasswordProperty(nodeCR.getPassword());
		} else if (ObjectTransformer.isEmpty(nodeCR.getPassword())) {
			cr.setPasswordType(ContentRepositoryModel.PasswordType.none);
		} else {
			cr.setPasswordType(ContentRepositoryModel.PasswordType.value);
		}
		cr.setUrl(nodeCR.getUrl());
		cr.setHttp2(nodeCR.isHttp2());
		cr.setBasepath(nodeCR.getBasepath());
		cr.setInstantPublishing(nodeCR.isInstantPublishing());
		cr.setPermissionInformation(nodeCR.isPermissionInformation());
		cr.setPermissionProperty(nodeCR.getPermissionProperty());
		cr.setDefaultPermission(nodeCR.getDefaultPermission());
		cr.setLanguageInformation(nodeCR.isLanguageInformation());
		cr.setDiffDelete(nodeCR.isDiffDelete());

		if (!StringUtils.isEmpty(nodeCR.getElasticsearch())) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				cr.setElasticsearch(mapper.readValue(nodeCR.getElasticsearch(), CRElasticsearchModel.class));
			} catch (IOException e) {
				NodeLogger.getNodeLogger(ContentRepository.class).error(String.format("Error while deserializing '%s'", nodeCR.getElasticsearch()), e);
			}
		}

		cr.setProjectPerNode(nodeCR.isProjectPerNode());
		cr.setVersion(nodeCR.getVersion());

		cr.setCheckDate(nodeCR.getCheckDate().getIntTimestamp());
		cr.setCheckStatus(Status.from(nodeCR.getCheckStatus()));
		cr.setCheckResult(nodeCR.getCheckResult());

		cr.setStatusDate(nodeCR.getStatusDate().getIntTimestamp());

		cr.setDataStatus(Status.from(nodeCR.getDataStatus()));
		cr.setDataCheckResult(nodeCR.getDataCheckResult());
		return cr;
	};

	/**
	 * Function that transforms the rest model into the node object
	 */
	public final static BiFunction<ContentRepositoryModel, ContentRepository, ContentRepository> REST2NODE = (cr, nodeCR) -> {
		if (cr.getName() != null) {
			nodeCR.setName(cr.getName());
		}
		if (cr.getCrType() != null) {
			nodeCR.setCrType(cr.getCrType());
		}
		if (cr.getDbType() != null) {
			nodeCR.setDbType(cr.getDbType());
		}
		if (cr.getUsername() != null) {
			nodeCR.setUsername(cr.getUsername());
		}
		if (cr.getPasswordType() != null) {
			switch(cr.getPasswordType()) {
			case none:
				nodeCR.setPassword(null);
				nodeCR.setPasswordProperty(false);
				break;
			case property:
				if (cr.getPasswordProperty() != null) {
					nodeCR.setPassword(cr.getPasswordProperty());
					nodeCR.setPasswordProperty(true);
				}
				break;
			case value:
				if (cr.getPassword() != null) {
					nodeCR.setPassword(cr.getPassword());
					nodeCR.setPasswordProperty(false);
				}
				break;
			}
		}
		if (cr.getHttp2() != null) {
			nodeCR.setHttp2(cr.getHttp2());
		}
		if (cr.getUrl() != null) {
			nodeCR.setUrl(cr.getUrl());
		}
		if (cr.getBasepath() != null) {
			nodeCR.setBasepath(cr.getBasepath());
		}
		if (cr.getInstantPublishing() != null) {
			nodeCR.setInstantPublishing(cr.getInstantPublishing());
		}
		if (cr.getPermissionInformation() != null) {
			nodeCR.setPermissionInformation(cr.getPermissionInformation());
		}
		if (cr.getPermissionProperty() != null) {
			nodeCR.setPermissionProperty(cr.getPermissionProperty());
		}
		if (cr.getDefaultPermission() != null) {
			nodeCR.setDefaultPermission(cr.getDefaultPermission());
		}
		if (cr.getLanguageInformation() != null) {
			nodeCR.setLanguageInformation(cr.getLanguageInformation());
		}
		if (cr.getDiffDelete() != null) {
			nodeCR.setDiffDelete(cr.getDiffDelete());
		}
		if (cr.getElasticsearch() != null) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				nodeCR.setElasticsearch(mapper.writeValueAsString(cr.getElasticsearch()));
			} catch (JsonProcessingException e) {
				NodeLogger.getNodeLogger(ContentRepository.class).error("Error while serializing elasticsearch settings", e);
			}
		}
		if (cr.getProjectPerNode() != null) {
			nodeCR.setProjectPerNode(cr.getProjectPerNode());
		}
		if (cr.getVersion() != null) {
			nodeCR.setVersion(cr.getVersion());
		}
		return nodeCR;
	};

	/**
	 * Lambda that transforms the node model of a cr into the rest model
	 */
	public final static Function<ContentRepository, ContentRepositoryModel> TRANSFORM2REST = nodeCR -> {
		return NODE2REST.apply(nodeCR, new ContentRepositoryModel());
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<ContentRepository>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<ContentRepository>>();
		resolvableProperties.put("globalId", new NodeObjectProperty<>((o, key) -> o.getGlobalId().toString()));
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName()));
		resolvableProperties.put("crType", new NodeObjectProperty<>((o, key) -> o.getCrType().toString()));
		resolvableProperties.put("dbType", new NodeObjectProperty<>((o, key) -> o.getDbType()));
		resolvableProperties.put("username", new NodeObjectProperty<>((o, key) -> o.getUsername()));
		resolvableProperties.put("url", new NodeObjectProperty<>((o, key) -> o.getUrl()));
		resolvableProperties.put("basepath", new NodeObjectProperty<>((o, key) -> o.getBasepath()));
		resolvableProperties.put("http2", new NodeObjectProperty<>((o, key) -> o.isHttp2()));
		resolvableProperties.put("instantPublishing", new NodeObjectProperty<>((o, key) -> o.isInstantPublishing()));
		resolvableProperties.put("languageInformation", new NodeObjectProperty<>((o, key) -> o.isLanguageInformation()));
		resolvableProperties.put("permissionInformation", new NodeObjectProperty<>((o, key) -> o.isPermissionInformation()));
		resolvableProperties.put("permissionProperty", new NodeObjectProperty<>((o, key) -> o.getPermissionProperty()));
		resolvableProperties.put("diffDelete", new NodeObjectProperty<>((o, key) -> o.isDiffDelete()));

		resolvableProperties.put("checkDate", new NodeObjectProperty<>((o, key) -> o.getCheckDate().getIntTimestamp()));
		resolvableProperties.put("checkStatus", new NodeObjectProperty<>((o, key) -> o.getCheckStatus()));
		resolvableProperties.put("checkResult", new NodeObjectProperty<>((o, key) -> o.getCheckResult()));
		resolvableProperties.put("statusDate", new NodeObjectProperty<>((o, key) -> o.getStatusDate().getIntTimestamp()));
		resolvableProperties.put("dataStatus", new NodeObjectProperty<>((o, key) -> o.getDataStatus()));
		resolvableProperties.put("dataCheckResult", new NodeObjectProperty<>((o, key) -> o.getDataCheckResult()));
	}

	/**
	 * Create an instance
	 * @param id id
	 * @param info info
	 */
	protected ContentRepository(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<ContentRepository> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * Get the name
	 * @return name
	 */
	@FieldGetter("name")
	public abstract String getName();

	/**
	 * Set the name
	 * @param name name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	public void setName(String name) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the CR type
	 * @return CR type
	 */
	@FieldGetter("crtype")
	public abstract Type getCrType();

	/**
	 * Set the CR type
	 * @param type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("crtype")
	public void setCrType(Type type) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the DB Type
	 * @return dbType (for crType {@link Type#cr} or {@link Type#mccr})
	 */
	@FieldGetter("dbtype")
	public abstract String getDbType();

	/**
	 * Set the DB Type (for crType {@link Type#cr} or {@link Type#mccr})
	 * @param dbType DB Type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("dbtype")
	public void setDbType(String dbType) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the username
	 * @return username
	 */
	@FieldGetter("username")
	public abstract String getUsername();

	/**
	 * Set the username
	 * @param username username
	 * @throws ReadOnlyException
	 */
	@FieldSetter("username")
	public void setUsername(String username) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the password
	 * @return password
	 */
	@FieldGetter("password")
	public abstract String getPassword();

	/**
	 * Set the password
	 * @param password password
	 * @throws ReadOnlyException
	 */
	@FieldSetter("password")
	public void setPassword(String password) throws ReadOnlyException {
		failReadOnly();
	}

	@FieldGetter("password_is_property")
	public abstract boolean isPasswordProperty();

	@FieldSetter("password_is_property")
	public void setPasswordProperty(boolean passwordProperty) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the HTTP/2 usage flag
	 * @return flag
	 */
	@FieldGetter("http2")
	public abstract boolean isHttp2();

	/**
	 * Set the HTTP/2 flag
	 * @param http2 flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("http2")
	public void setHttp2(boolean http2) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the connection URL
	 * @return connection URL
	 */
	@FieldGetter("url")
	public abstract String getUrl();

	/**
	 * Set the connection URL
	 * @param url connection URL
	 * @throws ReadOnlyException
	 */
	@FieldSetter("url")
	public void setUrl(String url) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether this contentrepository has instant publishing
	 * @return true for instant publishing
	 */
	@FieldGetter("instant_publishing")
	public abstract boolean isInstantPublishing();

	/**
	 * Set whether this contentrepository has instant publishing
	 * @param instantPublishing true for instant publishing
	 * @throws ReadOnlyException
	 */
	@FieldSetter("instant_publishing")
	public void setInstantPublishing(boolean instantPublishing) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether this contentrepository contains language information
	 * @return true for language information
	 */
	@FieldGetter("language_information")
	public abstract boolean isLanguageInformation();

	/**
	 * Set whether this contentrepository contains language information
	 * @param languageInformation true for language information
	 * @throws ReadOnlyException
	 */
	@FieldSetter("language_information")
	public void setLanguageInformation(boolean languageInformation) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether this contentrepository contains permission information
	 * @return true for permission information
	 */
	@FieldGetter("permission_information")
	public abstract boolean isPermissionInformation();

	/**
	 * Set whether this contentrepository contains permission information
	 * @param permissionInformation true for permission information
	 * @throws ReadOnlyException
	 */
	@FieldSetter("permission_information")
	public void setPermissionInformation(boolean permissionInformation) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the permission property for Mesh CRs
	 * @return name of the permission property
	 */
	@FieldGetter("permission_property")
	public abstract String getPermissionProperty();

	/**
	 * Set the permission property for Mesh CRs
	 * @param permissionProperty name of the permission property
	 * @throws ReadOnlyException
	 */
	@FieldSetter("permission_property")
	public void setPermissionProperty(String permissionProperty) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the default permission for Mesh CRs
	 * @return default permission
	 */
	@FieldGetter("default_permission")
	public abstract String getDefaultPermission();

	/**
	 * Set the default permissions for Mesh CRs
	 * @param defaultPermission default permission
	 * @throws ReadOnlyException
	 */
	@FieldSetter("default_permission")
	public void setDefaultPermission(String defaultPermission) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the flag for differential delete
	 * @return true for differential delete
	 */
	@FieldGetter("diffdelete")
	public abstract boolean isDiffDelete();

	/**
	 * Set the flag for differential delete
	 * @param diffDelete true for differential delete
	 * @throws ReadOnlyException
	 */
	@FieldSetter("diffdelete")
	public void setDiffDelete(boolean diffDelete) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the basepath for attributes, that are stored in the filesystem
	 * @return basepath for filesystem attributes
	 */
	@FieldGetter("basepath")
	public abstract String getBasepath();

	/**
	 * Set the basepath for attributes, that are stored in the filesystem
	 * @param basepath basepath for filesystem attributes
	 * @throws ReadOnlyException
	 */
	@FieldSetter("basepath")
	public void setBasepath(String basepath) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether the contentrepository is multichannelling aware
	 * @return true for multichannelling aware content repositories
	 */
	@FieldGetter("multichannelling")
	public boolean isMultichannelling() {
		switch (getCrType()) {
		case mccr:
			return true;
		case mesh:
			return isProjectPerNode();
		default:
			return false;
		}
	}

	/**
	 * Set whether the contentrepository is multichannelling aware
	 * @param multichannelling true for multichannelling
	 * @throws ReadOnlyException
	 */
	@FieldSetter("multichannelling")
	public void setMultichannelling(boolean multichannelling) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get elasticsearch specific configuration for Mesh CR
	 * The configuration is expected to be in JSON format and contain the configuration portions for "page", "folder" and "file" as property values of the root object
	 * @return elasticsearch configuration
	 */
	@FieldGetter("elasticsearch")
	public abstract String getElasticsearch();

	/**
	 * Set elasticsearch specific configuration for Mesh CR
	 * @param elasticsearch
	 * @throws ReadOnlyException
	 */
	@FieldSetter("elasticsearch")
	public void setElasticsearch(String elasticsearch) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether every assigned Node shall be published into its own project (for CRs of type {@link ContentRepositoryModel.Type#mesh}).
	 * @return true to publish every node into its own project
	 */
	@FieldGetter("project_per_node")
	public abstract boolean isProjectPerNode();

	/**
	 * Set whether every assigned Node shall be published into its own project
	 * @param projectPerNode true for publishing nodes into projects
	 * @throws ReadOnlyException
	 */
	@FieldSetter("project_per_node")
	public void setProjectPerNode(boolean projectPerNode) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the contentrepository version
	 * @return version
	 */
	@FieldGetter("version")
	public abstract String getVersion();

	/**
	 * Set the contentrepository version
	 * @param version version
	 * @throws ReadOnlyException
	 */
	@FieldSetter("version")
	public void setVersion(String version) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the list of tagmap entries for this contentrepository.
	 * This includes only the tagmap entries, which are exclusive for this CR, but not the entries from CR Fragments assigned to this CR.
	 * For the full list, call {@link #getAllEntries()}
	 * @return list of tagmap entries
	 * @throws NodeException
	 */
	public abstract List<TagmapEntry> getEntries() throws NodeException;

	/**
	 * Get the full list of tagmap entries for this contentrepository.
	 * This also includes the entries from CR Fragments assigned to this CR.
	 * For getting only the exclusive entries, call {@link #getEntries()}
	 * @return list of all tagmap entries
	 * @throws NodeException
	 */
	public List<TagmapEntry> getAllEntries() throws NodeException {
		List<TagmapEntry> entries = new ArrayList<>(getEntries());

		for (CrFragment crFragment : getAssignedFragments()) {
			for (CrFragmentEntry crFragmentEntry : crFragment.getEntries()) {
				entries.add(new CrFragmentEntryWrapper(this, crFragmentEntry));
			}
		}

		return entries;
	}

	/**
	 * Get a tagmap entry by its map name
	 * @param name map name
	 * @return tag map entry
	 * @throws NodeException
	 */
	public TagmapEntry getEntryByMapName(String name) throws NodeException {
		List<TagmapEntry> entries = getEntries();

		for (TagmapEntry entry : entries) {
			if (entry.getMapname().equals(name)) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Get the segment entry for folders
	 * @return segment entry for folders or null, if not found
	 * @throws NodeException
	 */
	public TagmapEntry getFolderSegmentEntry() throws NodeException {
		return getAllEntries().stream().filter(entry -> entry.getObject() == Folder.TYPE_FOLDER && entry.isSegmentfield()).findAny().orElse(null);
	}

	/**
	 * Get the set of assigned {@link CrFragment} instances
	 * @return set of assigned CrFragments
	 * @throws NodeException
	 */
	public abstract Set<CrFragment> getAssignedFragments() throws NodeException;

	@Override
	public int getEffectiveUdate() throws NodeException {
		// get the content's udate
		int udate = getUdate();
		// and also check udates for entries
		List<TagmapEntry> entries = getEntries();

		for (TagmapEntry tagmapEntry : entries) {
			udate = Math.max(udate, tagmapEntry.getEffectiveUdate());
		}

		return udate;
	}

	/**
	 * Get the list of nodes to which this content repository is assigned
	 * @return list of nodes
	 * @throws NodeException
	 */
	public abstract List<Node> getNodes() throws NodeException;

	/**
	 * Get the ContentMap for the ContentRepository. Calling this method will also register SQL Handles and Datasources for the ContentRepository
	 * @return ContentMap instance
	 * @throws NodeException
	 */
	public abstract ContentMap getContentMap() throws NodeException;

	/**
	 * Get the date of the last structure check
	 * @return date of last structure check
	 */
	public abstract ContentNodeDate getCheckDate();

	/**
	 * Get the checkstatus for the CR
	 * @return checkstatus
	 * @throws NodeException
	 */
	public abstract int getCheckStatus();

	/**
	 * Get the last check result
	 * @return last check result
	 */
	public abstract String getCheckResult();

	/**
	 * Get the status date (which is the date of the last publish process into this contentrepository)
	 * @return status date
	 */
	public abstract ContentNodeDate getStatusDate();

	/**
	 * Get the data status
	 * @return data status
	 */
	public abstract int getDataStatus();

	/**
	 * Get the last data check result
	 * @return last data check result
	 */
	public abstract String getDataCheckResult();

	/**
	 * Check the structure of the CR (which also checks connectivity)
	 * @param repair true if invalid structure shall be repaired, false for check only
	 * @return true if the structure is valid
	 * @throws NodeException
	 */
	public abstract boolean checkStructure(boolean repair) throws NodeException;

	/**
	 * Queue data check for the CR
	 * @param clean true to let invalid data be cleaned
	 * @throws NodeException
	 */
	public abstract void checkData(boolean clean) throws NodeException;

	/**
	 * Check entry consistency and return the list of found inconsistencies. Finds the following inconsistencies:
	 * <ol>
	 * <li>Consistency of the entry itself</li>
	 * <li>Duplicate mapnames for the same type</li>
	 * <li>CR/MCCR: Identical mapnames for different types with different settings</li>
	 * <li>Uniqueness of segmentfield per type</li>
	 * <li>Uniqueness of displayfield per type</li>
	 * </ol>
	 * @return list of inconsistencies
	 * @throws NodeException
	 */
	public List<TagmapEntryInconsistencyModel> checkEntryConsistency() throws NodeException {
		boolean fsAttributes = NodeConfigRuntimeConfiguration.isFeature(Feature.CR_FILESYSTEM_ATTRIBUTES);

		List<TagmapEntryInconsistencyModel> inconsistencies = new ArrayList<>();
		List<TagmapEntry> allEntries = getAllEntries();

		// organize entries in maps
		Map<Integer, Map<String, Set<TagmapEntry>>> typeNameMap = new HashMap<>();
		Map<String, Set<TagmapEntry>> nameMap = new HashMap<>();
		Map<Integer, Set<TagmapEntry>> typeMap = new HashMap<>();
		for (TagmapEntry entry : allEntries) {
			typeNameMap.computeIfAbsent(entry.getObject(), type -> new HashMap<>()).computeIfAbsent(entry.getMapname(), mapname -> new HashSet<>()).add(entry);
			nameMap.computeIfAbsent(entry.getMapname(), name -> new HashSet<>()).add(entry);
			typeMap.computeIfAbsent(entry.getObject(), type -> new HashSet<>()).add(entry);

			int type = entry.getObject();
			String name = entry.getMapname();
			// check entry consistency
			if (getCrType() != Type.mesh) {
				if (entry.isOptimized() && entry.isMultivalue()) {
					inconsistencies.add(createInconcistency(Collections.singleton(entry), "cr.entries.inconsistency.optimized_multivalue",
							I18NHelper.get("cr.entry.type." + type), name));
				}
				if (fsAttributes && entry.isOptimized() && entry.isFilesystem()) {
					inconsistencies.add(createInconcistency(Collections.singleton(entry), "cr.entries.inconsistency.optimized_filesystem",
							I18NHelper.get("cr.entry.type." + type), name));
				}
				if (fsAttributes && entry.isFilesystem() && entry.getAttributeTypeId() != AttributeType.longtext.type
						&& entry.getAttributeTypeId() != AttributeType.binary.type) {
					inconsistencies.add(createInconcistency(Collections.singleton(entry), "cr.entries.inconsistency.filesystem_type",
							I18NHelper.get("cr.entry.type." + type), name, entry.getAttributetype().toString()));
				}
			}
		}

		// check for duplicate tagmap entries
		for (Map.Entry<Integer, Map<String, Set<TagmapEntry>>> typeEntry : typeNameMap.entrySet()) {
			int type = typeEntry.getKey();
			Map<String, Set<TagmapEntry>> mapForType = typeEntry.getValue();
			for (Entry<String, Set<TagmapEntry>> nameEntry : mapForType.entrySet()) {
				String name = nameEntry.getKey();
				Set<TagmapEntry> tagmapEntries = nameEntry.getValue();
				AttributeType attrType = tagmapEntries.iterator().next().getAttributetype();

				if (!MiscUtils.areEqual(tagmapEntries, getCheckedAttributes(attrType))) {
					inconsistencies
							.add(createInconcistency(tagmapEntries, "cr.entries.inconsistency.duplicate", I18NHelper.get("cr.entry.type." + type), name));
				}
			}
		}

		if (getCrType() != Type.mesh) {
			// check for inconsistently defined entries (same name, different type)
			for (Map.Entry<String, Set<TagmapEntry>> nameEntry : nameMap.entrySet()) {
				String name = nameEntry.getKey();
				Set<TagmapEntry> nameSet = nameEntry.getValue();
				AttributeType attrType = nameSet.iterator().next().getAttributetype();

				switch(attrType) {
				case link:
				case foreignlink:
					if (!MiscUtils.areEqual(nameSet, "attributeType", "optimized", "multivalue", "targetType")) {
						inconsistencies.add(createInconcistency(nameSet, "cr.entries.inconsistency.unequal", name));
					}
					break;
				default:
					if (!MiscUtils.areEqual(nameSet, "attributeType", "optimized", "multivalue")) {
						inconsistencies.add(createInconcistency(nameSet, "cr.entries.inconsistency.unequal", name));
					}
					break;
				}
			}
		}

		if (getCrType() == Type.mesh) {
			// check for unique segmentfield and displayfield
			for (Map.Entry<Integer, Set<TagmapEntry>> typeEntry : typeMap.entrySet()) {
				int type = typeEntry.getKey();
				Set<TagmapEntry> typeSet = typeEntry.getValue();
				Set<TagmapEntry> segmentFields = typeSet.stream().filter(TagmapEntry::isSegmentfield).collect(Collectors.toSet());
				if (segmentFields.size() > 1) {
					inconsistencies.add(createInconcistency(segmentFields, "cr.entries.inconsistency.segmentfield", I18NHelper.get("cr.entry.type." + type)));
				}

				Set<TagmapEntry> displayFields = typeSet.stream().filter(TagmapEntry::isDisplayfield).collect(Collectors.toSet());
				if (displayFields.size() > 1) {
					inconsistencies.add(createInconcistency(displayFields,  "cr.entries.inconsistency.displayfield", I18NHelper.get("cr.entry.type." + type)));
				}
			}
		}

		return inconsistencies;
	}

	/**
	 * Get the attributes, which need to be checked for the given attribute type
	 * @param attrType attribute type
	 * @return array containing attributes to be checked
	 * @throws NodeException
	 */
	protected String[] getCheckedAttributes(AttributeType attrType) throws NodeException {
		switch (attrType) {
		case binary:
		case oldbinary:
		case bool:
		case date:
		case integer:
		case longtext:
		case text:
			switch (getCrType()) {
			case cr:
			case mccr:
				return new String[] { "tagname", "mapname", "object", "attributeType", "multivalue", "optimized", "filesystem" };
			case mesh:
				return new String[] { "tagname", "mapname", "object", "attributeType", "multivalue", "segmentfield", "displayfield", "urlfield", "elasticsearch" };
			default:
				throw new NodeException();
			}
		case micronode:
			return new String[] { "tagname", "mapname", "object", "attributeType", "multivalue", "segmentfield", "displayfield", "urlfield", "micronodeFilter", "elasticsearch" };
		case link:
			switch (getCrType()) {
			case cr:
			case mccr:
				return new String[] { "tagname", "mapname", "object", "attributeType", "multivalue", "optimized", "filesystem", "targetType" };
			case mesh:
				return new String[] { "tagname", "mapname", "object", "attributeType", "multivalue",  "targetType", "segmentfield", "displayfield", "urlfield", "elasticsearch" };
			default:
				throw new NodeException();
			}
		case foreignlink:
			return new String[] {"tagname", "mapname", "object", "attributeType", "multivalue", "optimized", "filesystem", "targetType", "foreignlinkAttribute", "foreignlinkAttributeRule"};
		default:
			throw new NodeException();
		}
	}

	/**
	 * Create an inconsistency
	 * @param entries entries forming the inconsistency
	 * @param descriptionI18n i18n key of the description
	 * @param params optional parameters contained in the description
	 * @return inconsistency model
	 */
	protected TagmapEntryInconsistencyModel createInconcistency(Collection<TagmapEntry> entries, String descriptionI18n, String... params) {
		TagmapEntryInconsistencyModel inconsistency = new TagmapEntryInconsistencyModel();
		inconsistency.setDescription(I18NHelper.get(descriptionI18n, params));
		for (TagmapEntry entry : entries) {
			inconsistency.getEntries().add(entry.getGlobalId().toString());
		}
		return inconsistency;
	}

	/**
	 * Add a tagmap entry
	 * @param tagName tagname
	 * @param mapName mapname
	 * @param object object type
	 * @param targetType target type (for link attributes)
	 * @param type attribute type
	 * @param multivalue true for multivalue
	 * @param stat true for static
	 * @param optimized true for optimized
	 * @param foreignLinkAttribute foreign link attribute
	 * @throws NodeException
	 */
	public void addEntry(String tagName, String mapName, int object, int targetType, AttributeType type, boolean multivalue, boolean stat,
			boolean optimized, String foreignLinkAttribute) throws NodeException {
		failReadOnly();
	}

	/**
	 * Add a tagmap entry (for Mesh CRs)
	 * @param tagName tagname
	 * @param mapName mapname
	 * @param object object type
	 * @param targetType target type (for link attributes)
	 * @param type attribute type
	 * @param multivalue true for multivalue
	 * @param stat true for static
	 * @param segmentfield true for segmentfield
	 * @param displayfield true for displayfield
	 * @param urlfield true for urlfield
	 * @throws NodeException
	 */
	public void addEntry(String tagName, String mapName, int object, int targetType, AttributeType type, boolean multivalue, boolean stat, boolean segmentfield,
			boolean displayfield, boolean urlfield) throws NodeException {
		failReadOnly();
	}

	/**
	 * Check whether the CR must contain the given object
	 * @param object object
	 * @return true iff the CR must contain the object
	 * @throws NodeException
	 */
	public boolean mustContain(NodeObject object) throws NodeException {
		if (object instanceof Folder) {
			return mustContain((Folder)object);
		} else if (object instanceof Page) {
			return mustContain((Page)object);
		} else if (object instanceof ContentFile) {
			return mustContain((ContentFile)object);
		} else if (object instanceof Form) {
			return mustContain((Form) object);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the CR must contain the given object in the node
	 * @param object object
	 * @param node node
	 * @return true iff the CR must contain the object
	 * @throws NodeException
	 */
	public boolean mustContain(NodeObject object, Node node) throws NodeException {
		if (object instanceof Folder) {
			return mustContain((Folder)object, node);
		} else if (object instanceof Page) {
			return mustContain((Page)object, node);
		} else if (object instanceof ContentFile) {
			return mustContain((ContentFile)object, node);
		} else if (object instanceof Form) {
			return mustContain((Form) object, node);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the CR must contain the given folder
	 * @param folder folder
	 * @return true iff the CR must contain the folder
	 * @throws NodeException
	 */
	public boolean mustContain(Folder folder) throws NodeException {
		if (folder == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// Check if the node of the folder doesn't publish folders into CR
		if (!folder.getNode().doPublishContentMapFolders()) {
			return false;
		}

		// iterate over all nodes, that are published into this CR
		for (Node node : getNodes()) {
			if (mustContain(folder, node)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check whether the CR must contain the given folder for the node
	 * @param folder folder
	 * @param node node
	 * @return true iff the CR must contain the folder for the node
	 * @throws NodeException
	 */
	public boolean mustContain(Folder folder, Node node) throws NodeException {
		if (folder == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// Check if the node doesn't publish folders into CR
		if (!node.doPublishContentMapFolders()) {
			return false;
		}

		// check whether the object is visible in the node
		return MultichannellingFactory.isVisibleInNode(node, folder);
	}

	/**
	 * Check whether the CR must contain the given file
	 * @param file file
	 * @return true iff the CR must contain the file
	 * @throws NodeException
	 */
	public boolean mustContain(ContentFile file) throws NodeException {
		if (file == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// Check if the node of the file doesn't publish files into CR
		if (!file.getNode().doPublishContentMapFiles()) {
			return false;
		}

		// iterate over all nodes, that are published into this CR
		for (Node node : getNodes()) {
			if (mustContain(file, node)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check whether the CR must contain the given file for the node
	 * @param file file
	 * @param node node
	 * @return true iff the CR must contain the file for the node
	 * @throws NodeException
	 */
	public boolean mustContain(ContentFile file, Node node) throws NodeException {
		if (file == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// Check if the node doesn't publish files into CR
		if (!node.doPublishContentMapFiles()) {
			return false;
		}

		boolean isVisible = false;
		boolean isOnline = false;

		// check whether the object is visible in the node
		if (MultichannellingFactory.isVisibleInNode(node, file)) {
			isVisible = true;
		}

		// check whether the file is marked online in the node
		if (FileOnlineStatus.isOnline(file, node)) {
			isOnline = true;
		}

		return isVisible && isOnline;
	}

	/**
	 * Check whether the CR must contain the given page
	 * @param page page
	 * @return true iff the CR must contain the page
	 * @throws NodeException
	 */
	public boolean mustContain(Page page) throws NodeException {
		if (page == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// if the page is offline, it is not published into any CR
		if (!page.isOnline()) {
			return false;
		}

		// Check if the node of the page doesn't publish pages into CR
		if (!page.getNode().doPublishContentMapPages()) {
			return false;
		}

		// iterate over all nodes, that are published into this CR
		for (Node node : getNodes()) {
			// check whether the object is visible in the node
			if (mustContain(page, node)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check whether the CR must contain the given page for the node
	 * @param page page
	 * @param node node
	 * @return true iff the CR must contain the page for the node
	 * @throws NodeException
	 */
	public boolean mustContain(Page page, Node node) throws NodeException {
		if (page == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// if the page is offline, it is not published into any CR
		if (!page.isOnline()) {
			return false;
		}

		// Check if the node doesn't publish pages into CR
		if (!node.doPublishContentMapPages()) {
			return false;
		}

		return MultichannellingFactory.isVisibleInNode(node, page, pId -> {
			Page variant = TransactionManager.getCurrentTransaction().getObject(Page.class, pId, -1, false);
			// if the page is not online and is modified, we will ignore it
			return variant.isOnline() || !variant.isModified();
		});
	}

	/**
	 * Check whether the CR must contain the given form
	 * @param form form
	 * @return true iff the CR must contain the form
	 * @throws NodeException
	 */
	public boolean mustContain(Form form) throws NodeException {
		if (form == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// if the form is offline, it is not published into any CR
		if (!form.isOnline()) {
			return false;
		}

		// only mesh CRs can hold forms
		if (getCrType() != Type.mesh) {
			return false;
		}

		// iterate over all nodes, that are published into this CR
		for (Node node : getNodes()) {
			// check whether the object is visible in the node
			if (mustContain(form, node)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check whether the CR must contain the given form for the node
	 * @param form form
	 * @param node node
	 * @return true iff the CR must contain the form for the node
	 * @throws NodeException
	 */
	public boolean mustContain(Form form, Node node) throws NodeException {
		if (form == null) {
			// non-existent objects must not exist in the CR
			return false;
		}

		// if the form is offline, it is not published into any CR
		if (!form.isOnline()) {
			return false;
		}

		// only mesh CRs can hold forms
		if (getCrType() != Type.mesh) {
			return false;
		}

		return Objects.equals(form.getOwningNode(), node);
	}

	/**
	 * Get nodes, which are assigned to the CR and have a conflict with one of the given nodes
	 * @param nodes optional array of nodes. If none given, the nodes currently assigned to the CR will be tested
	 * @return map of conflict message (i18n key) to set of conflicting nodes
	 * @throws NodeException
	 */
	public Map<String, Set<Node>> getConflictingNodes(Node... nodes) throws NodeException {
		Set<Node> assignedNodes = new HashSet<>(getNodes());
		List<Node> testedNodes = Arrays.asList(nodes);
		// remove all tested nodes (which would be the objects, currently persisted in the DB)
		assignedNodes.removeAll(testedNodes);
		// add all tested nodes again (which would be possibly modified copies of the nodes)
		assignedNodes.addAll(testedNodes);

		Map<String, Set<Node>> conflicts = new HashMap<>();

		// for mesh CRs without "projectPerNode", no two nodes must have the same base path
		if (getCrType() == Type.mesh && !isProjectPerNode()) {
			TagmapEntry folderSegmentEntry = getFolderSegmentEntry();
			if (folderSegmentEntry != null && "folder.pub_dir".equals(folderSegmentEntry.getTagname())) {
				// create a map of publish directories -> set of nodes using it
				Map<String, Set<Node>> nodesPerPublishDir = new HashMap<>();
				for (Node node : assignedNodes) {
					String pubDir = node.getFolder().getPublishDir();
					nodesPerPublishDir.computeIfAbsent(pubDir, key -> new HashSet<>()).add(node);
				}

				// check whether any of the found publish paths is used by more than one node
				for (Map.Entry<String, Set<Node>> entry : nodesPerPublishDir.entrySet()) {
					// we find a conflict only, if either no nodes were given to the method (which tests all already assigned nodes), or if
					// the conflict includes any of the given nodes
					if (entry.getValue().size() > 1 && (testedNodes.isEmpty() || !Collections.disjoint(entry.getValue(), testedNodes))) {
						conflicts.put("rest.node.conflict.meshcr", entry.getValue());
					}
				}
			}
		}

		return conflicts;
	}

	/**
	 * Check whether nodes should ignore the node publish directory, when publishing into this CR.
	 * @return true iff node publish directory should be ignored
	 */
	public boolean ignoreNodePublishDir() {
		return getCrType() == Type.mesh && !isProjectPerNode();
	}
}
