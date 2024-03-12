/*
 * @author norbert
 * @date 02.02.2011
 * @version $Id: Feature.java,v 1.11.2.2.2.1 2011-03-18 15:14:33 norbert Exp $
 */
package com.gentics.contentnode.etc;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.db.SQLExecutor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import org.apache.commons.lang.StringUtils;

/**
 * Enumeration of all (new) features. The feature names in the backend must be
 * all lowercase and the enum here all uppercase.
 * @author norbert
 */
public enum Feature {
	MULTICHANNELLING(false),
	TAG_IMAGE_RESIZER(false),
	PAGEVAR_ALL_CONTENTGROUPS(false),
	DATASOURCE_PERM(false),
	HTTP_AUTH_LOGIN(false),
	ALOHA_ANNOTATE_EDITABLES(false),
	COPY_TAGS(false),
	CR_FILESYSTEM_ATTRIBUTES(false),
	CONTENTFILE_AUTO_OFFLINE(true),
	ALWAYS_LOCALIZE(true),
	ROLES(false),
	PUBLISH_STATS(false),
	RESUMABLE_PUBLISH_PROCESS(false),
	DISABLE_INSTANT_DELETE(true),
	PUBLISH_FOLDER_STARTPAGE(true),
	PUBLISH_INHERITED_SOURCE(false),
	PUBLISH_CACHE(false),
	OMIT_PUBLISH_TABLE(false),
	CONTENTGROUP3(false),
	MOVE_PERM_WITH_EDIT(false),
	WASTEBIN(false, Node.TYPE_NODE, PermHandler.PERM_NODE_WASTEBIN),
	ATTRIBUTE_DIRTING(false),
	MULTITHREADED_PUBLISHING(false),
	INVALIDPAGEURLMSG(false),
	MCCR(false),
	DEVTOOLS(false),
	NICE_URLS(false),
	ELASTICSEARCH(false),
	MESH_CONTENTREPOSITORY(false),
	INSTANT_CR_PUBLISHING(false),
	PUB_DIR_SEGMENT(false),
	CLUSTER(false),
	SUSPEND_SCHEDULER(false),
	INSECURE_SCHEDULER_COMMAND(false),
	CONTENTGROUP3_PAGEFILENAME(false),
	CONTENTGROUP3_PAGEFILENAME_NO_APACHEFILENAME(false),
	MANAGELINKURL(false),
	MANAGELINKURL_ONLYFORPUBLISH(false),
	DSFALLBACK(false),
	DS_EMPTY_CS(false),
	GET_FILENAME_AS_PAGENAME(false),
	FILENAME_FORCETOLOWER(false),
	LIVEEDIT_TAG_PERCONSTRUCT(false),
	LIVE_URLS(false),
	LIVE_URLS_PER_NODE(true),
	VIEW_PERMS(false),
	DS_FOLDER_PERM(false),
	DS_FOLDER_WORKFLOW(false),
	HTML_IMPORT(false),
	CHANNELSYNC(false),
	CONSTRUCT_CATEGORIES(false),
	USERSNAP(false),
	LINK_CHECKER(true, true, 0),
	OBJTAG_SYNC(false),
	HIDE_MANUAL(false),
	FORMS(true),
	ASSET_MANAGEMENT(true),
	FOLDER_BASED_TEMPLATE_SELECTION(false),
	CONTENT_STAGING(false, false, 0),
	KEYCLOAK(false),
	UPLOAD_FILE_PROPERTIES(true),
	UPLOAD_IMAGE_PROPERTIES(true),
	TAGTYPEMIGRATION(false);

	/**
	 * Service loader for implementations of {@link FeatureService}
	 */
	private final static ServiceLoader<FeatureService> loader = ServiceLoader.load(FeatureService.class);

	/**<w
	 * Flag to mark features, that can be activated/deactivated per node
	 */
	private boolean perNode;

	/**
	 * Flag to mark perNode features that can only be set on the master node and will be inherited to channels
	 */
	private boolean inheritable;

	/**
	 * Object Type on which the permission needs to be set (0 for no permission bit setting)
	 */
	private int permObjType;

	/**
	 * Required permission bits to be set for groups 1 and 2 on all instances of the given object type
	 */
	private Permission perm;

	/**
	 * Create a Feature
	 * @param perNode true if the feature can be activated per node, false if not
	 */
	Feature(boolean perNode) {
		this(perNode, false, 0);
	}

	/**
	 * Create a Feature
	 * @param perNode true if the feature can be activated per node, false if not
	 * @param inheritable true if the feature is inherited from master to channel
	 */
	Feature(boolean perNode, boolean inheritable) {
		this(perNode, inheritable, 0);
	}

	/**
	 * Create a Feature with required permission bits set on objects of specified type
	 * @param perNode true if the feature can be activated per node, false if not
	 * @param permObjType object type for permission setting
	 * @param permBits required permission bits
	 */
	Feature(boolean perNode, int permObjType, int...permBits) {
		this(perNode, false, permObjType, permBits);
	}

	/**
	 * Create a Feature with required permission bits set on objects of specified type
	 * @param perNode true if the feature can be activated per node, false if not
	 * @param inheritable true if the feature is inherited from master to channel
	 * @param permObjType object type for permission setting
	 * @param permBits required permission bits
	 */
	 Feature(boolean perNode, boolean inheritable, int permObjType, int...permBits) {
		this.perNode = perNode;
		this.inheritable = inheritable;
		this.permObjType = permObjType;
		this.perm = new Permission(permBits);
	}

	/**
	 * Get the feature by it's lowercase name
	 * @param featureName lowercase name
	 * @return feature or null, if not found
	 */
	public static Feature getByName(String featureName) {
		if (StringUtils.isBlank(featureName)) {
			return null;
		} else {
			try {
				return Feature.valueOf(StringUtils.upperCase(featureName));
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	/**
	 * Check whether the feature can be activated per node
	 * @return true if it can be activated per node, false if not
	 */
	public boolean isPerNode() {
		return perNode;
	}

	/**
	 * Check whether the feature is inherited from master node to channels
	 * @return true if inheritable
	 */
	public boolean isInheritable() {
		return inheritable;
	}

	/**
	 * Fix the required permissions on group 2
	 * @throws NodeException
	 */
	public void fixRequiredPermissions() throws NodeException {
		if (permObjType <= 0 || ObjectTransformer.isEmpty(perm)) {
			return;
		}

		try (Trx trx = new Trx()) {
			Map<Integer, Permission> perms = new HashMap<>();
			if (permObjType == Node.TYPE_NODE) {
				IntegerColumnRetriever nodeIds = new IntegerColumnRetriever("folder_id");
				DBUtils.executeStatement("SELECT folder_id FROM node", nodeIds);
				if (!nodeIds.getValues().isEmpty()) {
					DBUtils.executeStatement(
							"SELECT o_id, perm FROM perm WHERE usergroup_id = 2 AND o_type = ? AND o_id IN ("
									+ StringUtils.repeat("?", ",", nodeIds.getValues().size()) + ")", new SQLExecutor() {
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int paramIndex = 1;
							stmt.setInt(paramIndex++, Folder.TYPE_FOLDER);
							for (Integer nodeId : nodeIds.getValues()) {
								stmt.setInt(paramIndex++, nodeId);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								Permission orig = new Permission(rs.getString("perm"));
								Permission modified = new Permission(orig.toString());
								modified.mergeBits(perm.toString());;
								if (!orig.equals(modified)) {
									perms.put(rs.getInt("o_id"), modified);
								}
							}
						}
					});
				}
			} else {
				DBUtils.executeStatement("SELECT o_id, perm FROM perm WHERE usergroup_id = 2 AND o_type = ?", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, permObjType);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Permission orig = new Permission(rs.getString("perm"));
							Permission modified = new Permission(orig.toString());
							modified.mergeBits(perm.toString());;
							if (!orig.equals(modified)) {
								perms.put(rs.getInt("o_id"), modified);
							}
						}
					}
				});
			}

			if (!perms.isEmpty()) {
				List<UserGroup> groups = Collections.singletonList(TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
				for (Map.Entry<Integer, Permission> entry : perms.entrySet()) {
					PermHandler.setPermissions(permObjType, entry.getKey(), groups, entry.getValue().toString());
					if (permObjType == Node.TYPE_NODE) {
						PermHandler.setPermissions(Folder.TYPE_FOLDER, entry.getKey(), groups, entry.getValue().toString());
					}
				}
			}
			trx.success();
		}
	}

	/**
	 * Get the name of the feature (lowercase)
	 * @return name of the feature
	 */
	public String getName() {
		return name().toLowerCase();
	}

	/**
	 * Check whether the feature is activated and the license is sufficient
	 * @return true iff feature is activated
	 */
	public boolean isActivated() {
		return NodeConfigRuntimeConfiguration.isFeature(this) &&  isAvailable();
	}

	/**
	 * Check whether the feature is activated for the node and the license is sufficient
	 * @return true iff feature is activated
	 */
	public boolean isActivated(Node node) {
		return NodeConfigRuntimeConfiguration.isFeature(this, node) && isAvailable();
	}

	/**
	 * Check whether the feature should be activated, but is not available
	 * @return true iff feature cannot be activated, because it is not available
	 */
	public boolean activatedButNotAvailable() {
		return NodeConfigRuntimeConfiguration.isFeature(this) && !isAvailable();
	}


	/**
	 * Check whether the feature is available
	 * @return true iff feature is available
	 */
	public boolean isAvailable() {
		// the feature is available, if at least one of the FeatureService implementations provides it
		return StreamSupport.stream(loader.spliterator(), false).filter(service -> service.isProvided(this)).findFirst()
				.isPresent();
	}
}
