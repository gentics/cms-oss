package com.gentics.contentnode.perm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.PermissionsMap;
import com.gentics.contentnode.rest.model.perm.RolePermissions;
import com.gentics.contentnode.rest.model.response.admin.CustomTool;
import com.gentics.contentnode.rest.model.response.log.ActionLogType;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Enum for types on which permissions can be set
 */
public enum TypePerms {
	setting(11,
			"settings",
			NamedPerm.read,
			NamedPerm.setperm),
	inbox(17,
			"915.inbox",
			NamedPerm.read,
			NamedPerm.setperm,
			NamedPerm.of(PermType.instantmessages, "instant_messages")),

	inboxmessage(31, "sn_message_text", inbox),

	pubqueue(10013,
			"918.queue",
			NamedPerm.read,
			NamedPerm.setperm),
	admin(1,
			"administration",
			NamedPerm.read,
			NamedPerm.setperm),

	useradmin(3,
			"user",
			admin,
			NamedPerm.of(PermType.read, "459.show", null, "user_module"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "user_module"),
			NamedPerm.of(PermType.createuser, "338.create", null, "user"),
			NamedPerm.of(PermType.updateuser, "339.edit", null, "user"),
			NamedPerm.of(PermType.deleteuser, "340.delete", null, "user")),
	groupadmin(4,
			"sn_groups",
			admin,
			NamedPerm.of(PermType.read, "459.show", null, "group_module"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "group_module"),
			NamedPerm.of(PermType.creategroup, "338.create", null, "599.group"),
			NamedPerm.of(PermType.updategroup, "339.edit", null, "599.group"),
			NamedPerm.of(PermType.deletegroup, "340.delete", null, "599.group"),
			NamedPerm.of(PermType.userassignment, "addremove_user", null, "599.group"),
			NamedPerm.of(PermType.updategroupuser, "edit_user", null, "599.group"),
			NamedPerm.of(PermType.setuserperm, "assign_user_permissions", null, "599.group")),

	user(10, "user", useradmin),

	group(6, "412.group", groupadmin),

	permission(510,
			"view_perms",
			Pair.of(Feature.VIEW_PERMS, null),
			admin,
			NamedPerm.read,
			NamedPerm.setperm),

	role(109,
			"roles",
			Pair.of(Feature.ROLES, null),
			admin,
			NamedPerm.of(PermType.read, "459.show", null, "roles"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "roles"),
			NamedPerm.of(PermType.assignroles, "assign roles to groups", null, "roles")),
	autoupdate(141,
			"AutoUpdate",
			admin,
			NamedPerm.read,
			NamedPerm.setperm),
	contentadmin(10010,
			"cn_content_admin",
			admin,
			NamedPerm.of(PermType.read, "459.show", null, "cn_content_admin"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "cn_content_admin"),
			NamedPerm.of(PermType.sysinfo, "show_sysinfo", null, "sysinfo")),

	objpropadmin(12,
			"object properties",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "objectproperty"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "objectproperty")),
	objproptype(14,
			id -> {
				if (id == null) {
					return I18NHelper.get("object properties");
				}
				switch (id) {
				case Folder.TYPE_FOLDER:
					return I18NHelper.get("402.folder");
				case Template.TYPE_TEMPLATE:
					return I18NHelper.get("templates");
				case Page.TYPE_PAGE:
					return I18NHelper.get("pages");
				case ImageFile.TYPE_IMAGE:
					return I18NHelper.get("images");
				case File.TYPE_FILE:
					return I18NHelper.get("files");
				default:
					return null;
				}
			},
			null,
			objpropadmin,
			(id, channelId) -> Stream.of(Folder.TYPE_FOLDER, Template.TYPE_TEMPLATE, Page.TYPE_PAGE, ImageFile.TYPE_IMAGE, File.TYPE_FILE).collect(Collectors.toList()),
			null,
			false,
			false,
			NamedPerm.of(PermType.read, "459.show", null, "objectproperty"), NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "objectproperty"), NamedPerm.of(PermType.update, "339.edit", null, "objectproperty")),
	objprop(40,
			id -> {
				if (id == null) {
					return I18NHelper.get("objectproperty definition");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				ObjectTagDefinition def = t.getObject(ObjectTagDefinition.class, id);
				if (def != null) {
					return def.getName();
				} else {
					return null;
				}
			},
			null,
			objproptype,
			(type, channelId) -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				List<ObjectTagDefinition> defList = new ArrayList<>(t.getObjects(ObjectTagDefinition.class,
						DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND obj_id = 0", st -> st.setInt(1, type), DBUtils.IDLIST)));
				defList.sort(new Comparator<ObjectTagDefinition>() {
					@Override
					public int compare(ObjectTagDefinition o1, ObjectTagDefinition o2) {
						return com.gentics.lib.etc.StringUtils.mysqlLikeCompare(o1.getName(), o2.getName());
					}});
				return defList.stream().map(ObjectTagDefinition::getId).collect(Collectors.toList());
			},
			null,
			false,
			false,
			NamedPerm.of(PermType.read, "459.show", null, "objectproperty"), NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "objectproperty"), NamedPerm.of(PermType.update, "339.edit", null, "objectproperty")),

	objpropcategory(108,
			"object property categories",
			objpropadmin,
			NamedPerm.of(PermType.read, "459.show", null, "object property categories"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "object property categories")),
	objpropmaintenance(140,
			"object property maintenance",
			objpropadmin,
			NamedPerm.read,
			NamedPerm.setperm),

	constructadmin(10003,
			"tagtype",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "tag_type_module"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "tag_type_module"),
			NamedPerm.of(PermType.update, "339.edit", null, "tag_type_module")),

	construct(10004, "construct", constructadmin),

	part(10025, "part", construct),

	constructcategoryadmin(10211,
			"construct categories",
			Pair.of(Feature.CONSTRUCT_CATEGORIES, null),
			constructadmin,
			NamedPerm.of(PermType.read, "459.show", null, "construct_category"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "construct_category")),

	constructcategory(10203,
			id -> {
				if (id == null) {
					return I18NHelper.get("construct_category");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				ConstructCategory cat = t.getObject(ConstructCategory.class, id);
				if (cat != null) {
					return cat.getName().toString();
				} else {
					return null;
				}
			},
			Pair.of(Feature.CONSTRUCT_CATEGORIES, null),
			constructcategoryadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM construct_category ORDER BY sortorder", DBUtils.IDLIST),
			null,
			false, false, NamedPerm.of(PermType.read, "459.show", null, "construct_category")),

	datasourceadmin(10024,
			"datasources",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "datasources"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "datasources")),

	datasource(10027,
			id -> {
				if (id == null) {
					return I18NHelper.get("datasource");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				Datasource datasource = t.getObject(Datasource.class, id);
				if (datasource != null) {
					return datasource.getName();
				} else {
					return null;
				}
			},
			Pair.of(Feature.DATASOURCE_PERM, datasourceadmin),
			datasourceadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM datasource", DBUtils.IDLIST),
			null,
			false,
			false, NamedPerm.of(PermType.read, "459.show", null, "datasource"), NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "datasource")),

	languageadmin(10023,
			"languages",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "languages"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "languages")),

	language(10031, "language", languageadmin),

	maintenance(10032,
			"maintenance",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "maintenance"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "maintenance"),
			NamedPerm.of(PermType.update, "339.edit", null, "maintenance")),
	contentmapbrowser(10200,
			"contentmap browser",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "contentmap browser"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "contentmap browser")),
	contentrepositoryadmin(10207,
			"ContentRepositories",
			contentadmin,
			NamedPerm.of(PermType.read, "459.show", null, "ContentRepositories"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "ContentRepositories"),
			NamedPerm.of(PermType.create, "338.create", null, "ContentRepositories")),

	contentrepository(10208,
			id -> {
				if (id == null) {
					return I18NHelper.get("crtype.cr");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				ContentRepository cr = t.getObject(ContentRepository.class, id);
				if (cr != null) {
					return cr.getName();
				} else {
					return null;
				}
			},
			null,
			contentrepositoryadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM contentrepository ORDER BY name", DBUtils.IDLIST),
			null,
			false,
			false,
			NamedPerm.of(PermType.read, "459.show", null, "ContentRepository"),
			NamedPerm.of(PermType.setperm, "assign_user_permissions", null, "ContentRepository"), NamedPerm.of(PermType.update, "339.edit", null, "ContentRepository"), NamedPerm.of(PermType.delete, "340.delete", null, "ContentRepository")),

	devtooladmin(12000,
			"devtools_packages",
			Pair.of(Feature.DEVTOOLS, null),
			contentadmin,
			NamedPerm.copy(NamedPerm.read).category("devtools_packages"),
			NamedPerm.copy(NamedPerm.setperm).category("devtools_packages")),
	crfragmentadmin(10300,
			"cr_fragments",
			contentadmin, 
			NamedPerm.read,
			NamedPerm.setperm,
			NamedPerm.create),

	crfragment(10301,
			id -> {
				if (id == null) {
					return I18NHelper.get("cr_fragment");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				CrFragment fragment = t.getObject(CrFragment.class, id);
				if (fragment != null) {
					return fragment.getName();
				} else {
					return null;
				}
			},
			null,
			crfragmentadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM cr_fragment ORDER BY name", DBUtils.IDLIST),
			null,
			false,
			false,
			NamedPerm.copy(NamedPerm.read).category("cr_fragment"),
			NamedPerm.copy(NamedPerm.setperm).category("cr_fragment"), NamedPerm.copy(NamedPerm.update).category("cr_fragment"), NamedPerm.copy(NamedPerm.delete).category("cr_fragment")),

	searchindexmaintenance(10400,
			"search.maintenance",
			contentadmin,
			NamedPerm.read,
			NamedPerm.setperm),
	contentstagingadmin(10401,
			"content.staging",
			Pair.of(Feature.CONTENT_STAGING, null),
			contentadmin,
			NamedPerm.read,
			NamedPerm.create,
			NamedPerm.update,
			NamedPerm.delete,
			NamedPerm.setperm,
			NamedPerm.of(PermType.modifycontent, "content.staging.modify.content", null, "userright")),
	systemmaintenance(1042,
			"system maintenance",
			admin,
			NamedPerm.read,
			NamedPerm.setperm),

	errorlog(16,
			"sn_error",
			admin,
			NamedPerm.copy(NamedPerm.read).category("Error.Log"),
			NamedPerm.copy(NamedPerm.setperm).category("Error.Log"),
			NamedPerm.of(PermType.deleteerrorlog, "340.delete", null, "Logs")),
	actionlog(8,
			"logs",
			admin,
			NamedPerm.copy(NamedPerm.read).category("log_action"),
			NamedPerm.copy(NamedPerm.setperm).category("log_action")),
	scheduler(36,
			"scheduler",
			admin,
			NamedPerm.copy(NamedPerm.read).category("Task.Admin"),
			NamedPerm.copy(NamedPerm.setperm).category("Task.Admin"),
			NamedPerm.of(PermType.suspendscheduler, "suspend_scheduler", null, "Task.Admin"),
			NamedPerm.of(PermType.readtasks, "459.show", null, "sn_tasks"),
			NamedPerm.of(PermType.updatetasks, "339.edit", null, "sn_tasks"),
			NamedPerm.of(PermType.readschedules, "459.show", null, "scheduler_schedules"),
			NamedPerm.of(PermType.updateschedules, "339.edit", null, "scheduler_schedules")),

	schedulertaskadmin(162,
			id -> I18NHelper.get("scheduler_tasks"),
			null,
			scheduler,
			null,
			null,
			false,
			false),
	schedulertask(160,
			id -> {
				if (id == null) {
					return I18NHelper.get("scheduler_tasks");
				}
				return DBUtils.select("SELECT name FROM scheduler_task WHERE id = ?", st -> st.setInt(1, id), DBUtils.firstString("name"));
			},
			null,
			schedulertaskadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM scheduler_task ORDER BY name", DBUtils.IDLIST),
			(bit, id) -> {
				if (bit == PermType.read.getBit()) {
					PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					if (permHandler.checkPermissionBits(scheduler.type(), null, PermType.readtasks.getBit())) {
						return true;
					}
				}

				return null;
			},
			false,
			false,
			NamedPerm.read),

	schedulerscheduleadmin(163,
			id -> I18NHelper.get("scheduler_schedules"),
			null,
			scheduler,
			null,
			null,
			false,
			false),
	schedulerschedule(161,
			id -> {
				if (id == null) {
					return I18NHelper.get("scheduler_schedules");
				}
				return DBUtils.select("SELECT name FROM scheduler_schedule WHERE id = ?", st -> st.setInt(1, id), DBUtils.firstString("name"));
			},
			null,
			schedulerscheduleadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM scheduler_schedule ORDER BY name", DBUtils.IDLIST),
			(bit, id) -> {
				if (bit == PermType.read.getBit()) {
					PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					if (permHandler.checkPermissionBits(scheduler.type(), null, PermType.readschedules.getBit())) {
						return true;
					}
				}

				return null;
			},
			false,
			false,
			NamedPerm.read),

	tasktemplateadmin(41,
			id -> I18NHelper.get("task templates"),
			null,
			scheduler,
			null,
			null,
			false,
			false
			),

	tasktemplate(38,
			id -> {
				if (id == null) {
					return I18NHelper.get("sn_task_template");
				}
				return DBUtils.select("SELECT name FROM tasktemplate WHERE id = ?", st -> st.setInt(1, id),
						DBUtils.firstString("name"));
			},
			null,
			tasktemplateadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM tasktemplate ORDER BY name", DBUtils.IDLIST),
			(bit, id) -> {
				if (bit == PermType.read.getBit()) {
					PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					if (permHandler.checkPermissionBits(scheduler.type(), null, PermType.readtasktemplates.getBit())) {
						return true;
					}
				}
				return null;
			},
			false, false, NamedPerm.read),

	taskadmin(43,
			id -> I18NHelper.get("sn_tasks"),
			null,
			scheduler,
			null,
			null,
			false,
			false
			),

	task(37,
			id -> {
				if (id == null) {
					return I18NHelper.get("sn_task");
				}
				return DBUtils.select("SELECT name FROM task WHERE id = ?", st -> st.setInt(1, id),
						DBUtils.firstString("name"));
			},
			null,
			taskadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM task ORDER BY name", DBUtils.IDLIST),
			(bit, id) -> {
				if (bit == PermType.read.getBit()) {
					PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					if (permHandler.checkPermissionBits(scheduler.type(), null, PermType.readtasks.getBit())) {
						return true;
					}
				}
				return null;
			},
			false, false, NamedPerm.read),

	jobadmin(42,
			id -> I18NHelper.get("task jobs"),
			null,
			scheduler,
			null,
			null,
			false,
			false
			),

	job(39,
			id -> {
				if (id == null) {
					return I18NHelper.get("sn_task_date");
				}
				return DBUtils.select("SELECT name FROM job WHERE id = ?", st -> st.setInt(1, id),
						DBUtils.firstString("name"));
			},
			null,
			jobadmin,
			(id, channelId) -> DBUtils.select("SELECT id FROM job ORDER BY name", DBUtils.IDLIST),
			(bit, id) -> {
				if (bit == PermType.read.getBit()) {
					PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					if (permHandler.checkPermissionBits(scheduler.type(), null, PermType.readjobs.getBit())) {
						return true;
					}
				}
				return null;
			},
			false, false, NamedPerm.read),

	workflowadmin(24,
			"sn_workflow.admin",
			admin,
			NamedPerm.copy(NamedPerm.read).category("Workflow.Admin"),
			NamedPerm.copy(NamedPerm.setperm).category("Workflow.Admin")),
	customtooladmin(90000,
			"tree.custom_tools",
			admin,
			NamedPerm.read,
			NamedPerm.setperm),
	usersnap(90100,
			"Usersnap",
			Pair.of(Feature.USERSNAP, null),
			admin,
			NamedPerm.read,
			NamedPerm.setperm),

	@SuppressWarnings("rawtypes")
	customtool(90001,
			id -> {
				NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
				Collection<?> customTools = ObjectTransformer.getCollection(prefs.getPropertyObject("custom_tools"),
						Collections.emptyList());
				CustomTool tool = customTools.stream().map(o -> {
					if (o instanceof Map) {
						return ModelBuilder.getCustomTool((Map<?, ?>)o);
					} else {
						return null;
					}
				}).filter(t -> id.intValue() == t.getId()).findFirst().orElse(null);
				if (tool != null) {
					return I18NHelper.get(tool.getName());
				} else {
					return null;
				}
			},
			null,
			customtooladmin,
			(id, channelId) -> {
				// custom tools come from the configuration
				NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
				Collection<?> customTools = ObjectTransformer.getCollection(prefs.getPropertyObject("custom_tools"),
						Collections.emptyList());
				return customTools.stream().map(o -> {
					if (o instanceof Map) {
						return ObjectTransformer.getInt(((Map)o).get("id"), -1);
					} else {
						return null;
					}
				}).filter(tool -> tool != null).collect(Collectors.toList());
			}, (bit, id) -> {
				// admins for the custom tools can see all
				if (customtooladmin.canSetPerms()) {
					return true;
				}
				return null;
			},
			false, false, NamedPerm.read),

	content(10000,
			"content.node",
			NamedPerm.copy(NamedPerm.read).category("content.node"),
			NamedPerm.copy(NamedPerm.setperm).category("content.node"),
			NamedPerm.copy(NamedPerm.create).category("node")),
	node(10001,
			id -> {
				if (id == null) {
					return I18NHelper.get("401.node");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				Folder folder = t.getObject(Folder.class, id, -1, false);
				if (folder != null && folder.isRoot()) {
					return folder.getName();
				} else {
					I18nString message = new CNI18nString("folder.notfound");
					throw new EntityNotFoundException(message.toString());
				}
			},
			null,
			content,
			(id, channelId) -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				List<Folder> folders = new ArrayList<>(t.getObjects(Folder.class, DBUtils.select("SELECT id FROM folder WHERE mother = 0", DBUtils.IDLIST)));
				Collections.sort(folders, new FolderComparator("name", "asc"));
				return folders.stream().map(Folder::getId).collect(Collectors.toList());
			},
			(bit, id) -> {
				if (bit == PermHandler.PERM_CHANGE_PERM) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Folder folder = t.getObject(Folder.class, id, -1, false);
					if (folder != null && folder.isChannelRoot()) {
						return false;
					}
				}
				return null;
			},
			false,
			true,
			NamedPerm.copy(NamedPerm.read).category("402.folder"),
			NamedPerm.copy(NamedPerm.setperm).category("402.folder"),
			NamedPerm.copy(NamedPerm.create).category("402.folder"),
			NamedPerm.of(PermType.updatefolder, "339.edit", null, "402.folder"),
			NamedPerm.of(PermType.deletefolder, "340.delete", null, "402.folder"),
			NamedPerm.of(PermType.linkoverview, "uebersicht verknuepfen", null, "402.folder", Feature.DS_FOLDER_PERM),
			NamedPerm.of(PermType.createoverview, "uebersicht einfuegen", null, "402.folder", Feature.DS_FOLDER_WORKFLOW),
			NamedPerm.of(PermType.readitems, "459.show", null, "pages.images.files"),
			NamedPerm.of(PermType.createitems, "338.create", null, "pages.images.files"),
			NamedPerm.of(PermType.updateitems, "339.edit", null, "pages.images.files"),
			NamedPerm.of(PermType.deleteitems, "340.delete", null, "pages.images.files"),
			NamedPerm.of(PermType.importitems, "action_import", null, "pages.images.files", Feature.HTML_IMPORT),
			NamedPerm.of(PermType.publishpages, "cn_publish", null, "379.pages"),
			NamedPerm.of(PermType.translatepages, "translate", null, "379.pages"),

			NamedPerm.of(PermType.viewform, "459.show", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.createform, "338.create", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.updateform, "339.edit", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.deleteform, "340.delete", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.publishform, "cn_publish", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.formreport, "form.permission.reports", "form.permission.reports.description", "feature.forms", Feature.FORMS),

			NamedPerm.of(PermType.readtemplates, "459.show", null, "templates"),
			NamedPerm.of(PermType.createtemplates, "338.create", null, "templates"),
			NamedPerm.of(PermType.updatetemplates, "339.edit", null, "templates"),
			NamedPerm.of(PermType.deletetemplates, "340.delete", null, "templates"),
			NamedPerm.of(PermType.linktemplates, "930.link", null, "templates"),
			NamedPerm.of(PermType.updateconstructs, "changecreate_tag_types", null, "tag_types"),
			NamedPerm.of(PermType.channelsync, "channelsync", null, "multichannelling", Feature.MULTICHANNELLING, Feature.CHANNELSYNC),
			NamedPerm.of(PermType.updateinheritance, "inheritance", null, "multichannelling", Feature.MULTICHANNELLING),
			NamedPerm.of(PermType.wastebin, "wastebin.allow", null, "wastebin", Feature.WASTEBIN)),
	folder(10002,
			id -> {
				if (id == null) {
					return I18NHelper.get("folder");
				}
				Transaction t = TransactionManager.getCurrentTransaction();
				Folder folder = t.getObject(Folder.class, id, -1, false);
				if (folder != null) {
					return folder.getName();
				} else {
					return null;
				}
			},
			null,
			node,
			(id, channelId) -> {
				try (ChannelTrx cTrx = new ChannelTrx(channelId)) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Folder folder = t.getObject(Folder.class, id);
					if (folder != null) {
						List<Folder> children = new ArrayList<>(folder.getChildFolders());
						Collections.sort(children, new FolderComparator("name", "asc"));
						return children.stream().map(Folder::getId).collect(Collectors.toList());
					} else {
						return Collections.emptyList();
					}
				}
			},
			(bit, id) -> {
				if (bit == PermHandler.PERM_CHANGE_PERM) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Folder folder = t.getObject(Folder.class, id);
					if (folder != null && (!folder.isMaster() || folder.isInherited())) {
						return false;
					}
				}
				return null;
			},
			true,
			true,
			NamedPerm.copy(NamedPerm.read).category("402.folder"),
			NamedPerm.copy(NamedPerm.setperm).category("402.folder"),
			NamedPerm.copy(NamedPerm.create).category("402.folder"),
			NamedPerm.of(PermType.updatefolder, "339.edit", null, "402.folder"),
			NamedPerm.of(PermType.deletefolder, "340.delete", null, "402.folder"),
			NamedPerm.of(PermType.linkoverview, "uebersicht verknuepfen", null, "402.folder", Feature.DS_FOLDER_PERM),
			NamedPerm.of(PermType.createoverview, "uebersicht einfuegen", null, "402.folder", Feature.DS_FOLDER_WORKFLOW),
			NamedPerm.of(PermType.readitems, "459.show", null, "pages.images.files"),
			NamedPerm.of(PermType.createitems, "338.create", null, "pages.images.files"),
			NamedPerm.of(PermType.updateitems, "339.edit", null, "pages.images.files"),
			NamedPerm.of(PermType.deleteitems, "340.delete", null, "pages.images.files"),
			NamedPerm.of(PermType.importitems, "action_import", null, "pages.images.files", Feature.HTML_IMPORT),
			NamedPerm.of(PermType.publishpages, "cn_publish", null, "379.pages"),
			NamedPerm.of(PermType.translatepages, "translate", null, "379.pages"),

			NamedPerm.of(PermType.viewform, "459.show", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.createform, "338.create", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.updateform, "339.edit", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.deleteform, "340.delete", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.publishform, "cn_publish", null, "feature.forms", Feature.FORMS),
			NamedPerm.of(PermType.formreport, "form.permission.reports", "form.permission.reports.description", "feature.forms", Feature.FORMS),

			NamedPerm.of(PermType.readtemplates, "459.show", null, "templates"),
			NamedPerm.of(PermType.createtemplates, "338.create", null, "templates"),
			NamedPerm.of(PermType.updatetemplates, "339.edit", null, "templates"),
			NamedPerm.of(PermType.deletetemplates, "340.delete", null, "templates"),
			NamedPerm.of(PermType.linktemplates, "930.link", null, "templates")),
	template(10006, "template", folder),
	page(10007, "page", folder),
	file(10008, "file", folder),
	image(10011, "image", folder),
	form(10050, "form", folder),
	objtag(10113, "objectproperty", objprop),
	pagecontent(10015, "content", page);

	/**
	 * Transform the type into its REST model
	 */
	public final static BiFunction<TypePerms, ActionLogType, ActionLogType> NODE2REST = (type, model) -> {
		return model.setName(type.name()).setLabel(type.getLabel());
	};

	/**
	 * Transform the type into its REST model
	 */
	public final static Function<TypePerms, ActionLogType> TRANSFORM2REST = type -> {
		return NODE2REST.apply(type, new ActionLogType());
	};

	/**
	 * Map of parent -&gt; children
	 */
	private static Map<TypePerms, List<TypePerms>> childMap = new HashMap<>();

	/**
	 * List of root types (types that have no parent)
	 */
	private static List<TypePerms> rootTypes = new ArrayList<>();

	/**
	 * List of inactive types (types that are no longer used and will be removed in the future)
	 */
	private static List<TypePerms> inactive = Arrays.asList(TypePerms.tasktemplateadmin, TypePerms.taskadmin, TypePerms.jobadmin);

	/**
	 * Numeric type
	 */
	private int type;

	/**
	 * Label Function
	 */
	private Function<Integer, String> labelFunction;

	/**
	 * Feature
	 */
	private Feature feature;

	/**
	 * Replacement type (if type is not active)
	 */
	private TypePerms replacement;

	/**
	 * Parent type (may be null)
	 */
	private TypePerms parent;

	/**
	 * Function that returns the instance IDs, if the type has instances (like e.g. folder).
	 * Null if the type does not have instances (like e.g. admin). If the parent type has instances, the function will be called
	 * with the ID of the parent instance.
	 */
	private BiFunction<Integer, Integer, List<Integer>> instanceFunction;

	/**
	 * Permission bits, the type supports
	 */
	private List<NamedPerm> bits;

	/**
	 * True if the type supports bit 0 (view), which means that groups can be granted the view the type.
	 * Types that do not support bit 0 will check bit 0 of their parent instance instead.
	 */
	private boolean viewBit;

	/**
	 * True if the type supports bit 1 (set permissions), which means that groups can be granted the permission to change permissions
	 * on the type. Types that do not support bit 1 will check bit 1 of their parent instance instead.
	 */
	private boolean permBit;

	/**
	 * True if the type is hierarchical
	 */
	private boolean hierarchical;

	/**
	 * True if the type supports roles
	 */
	private boolean roles;

	/**
	 * Optional function for checking permission bits.
	 * The function will get the bit to check (first parameter) and optionally the instance ID, if checking on an ID
	 */
	private BiFunction<Integer, Integer, Boolean> checkerFunction;

	static {
		// compute the entries of the child map
		for (TypePerms permType : TypePerms.values()) {
			if (permType.parent != null) {
				childMap.computeIfAbsent(permType.parent, key -> new ArrayList<>()).add(permType);
			} else {
				rootTypes.add(permType);
			}
			if (permType.hierarchical) {
				childMap.computeIfAbsent(permType, key -> new ArrayList<>()).add(permType);
			}
		}
	}

	/**
	 * Get the instance for the given type string. The type string can either be the name of the enum, or (if numerical) the type number.
	 * @param type type name or number
	 * @return instance or null if not found
	 */
	public static TypePerms get(String type) {
		try {
			return valueOf(type);
		} catch (IllegalArgumentException iae) {
			try {
				int intType = normalize(Integer.parseInt(type));
				for (TypePerms tType : values()) {
					if (tType.type == intType) {
						return tType;
					}
				}
				return null;
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}

	/**
	 * Get the list of active root types
	 * @return list of active root types
	 */
	public static List<TypePerms> getRootTypes() {
		return rootTypes.stream().filter(TypePerms::isActive).collect(Collectors.toList());
	}

	/**
	 * Normalize the object type for permission checks
	 * @param objType object type
	 * @return normalized object type
	 */
	public static int normalize(int objType) {
		switch(objType) {
		case Folder.TYPE_INHERITED_FOLDER:
			return Folder.TYPE_FOLDER;
		case Node.TYPE_CHANNEL:
			return Node.TYPE_NODE;
		default:
			return objType;
		}
	}

	/**
	 * Create an instance with type and parent only.
	 * This will be used for types that can be used to query specific instance permissions, but do not support
	 * setting individual permissions (e.g. user, group, page, file, image, template, ...).
	 * @param type numerical type
	 * @param parent parent instance
	 */
	private TypePerms(int type, TypePerms parent) {
		this(type, id -> null, null, parent, (id, channelId) -> Collections.emptyList(), (bit, id) -> {
			return null;
		}, false, false);
	}

	/**
	 * Create an instance with type, labelKey and parent only.
	 * This will be used for types that can be used to query specific instance permissions, but do not support
	 * setting individual permissions (e.g. user, group, page, file, image, template, ...).
	 * @param type numerical type
	 * @param parent parent instance
	 */
	private TypePerms(int type, String labelKey, TypePerms parent) {
		this(type, id -> I18NHelper.get(labelKey), null, parent, (id, channelId) -> Collections.emptyList(), (bit, id) -> {
			return null;
		}, false, false);
	}

	/**
	 * Create an instance
	 * @param type numerical type
	 * @param labelKey label key
	 * @param bits array of supported bits
	 */
	@SafeVarargs
	private TypePerms(int type, String labelKey, NamedPerm...bits) {
		this(type, id -> I18NHelper.get(labelKey), null, null, null, null, false, false, bits);
	}

	/**
	 * Create an instance
	 * @param type numerical type
	 * @param labelKey label key
	 * @param featureAndReplacement optional feature and replacement type
	 * @param bits array of supported bits
	 */
	@SafeVarargs
	private TypePerms(int type, String labelKey, Pair<Feature, TypePerms> featureAndReplacement, NamedPerm...bits) {
		this(type, id -> I18NHelper.get(labelKey), featureAndReplacement, null, null, null, false, false, bits);
	}

	/**
	 * Create an instance
	 * @param type numerical type
	 * @param labelKey label key
	 * @param parent parent instance
	 * @param bits array of supported bits
	 */
	@SafeVarargs
	private TypePerms(int type, String labelKey, TypePerms parent, NamedPerm...bits) {
		this(type, id -> I18NHelper.get(labelKey), null, parent, null, null, false, false, bits);
	}

	/**
	 * Create an instance
	 * @param type numerical type
	 * @param labelKey label key
	 * @param featureAndReplacement optional feature and replacement type
	 * @param parent parent instance
	 * @param bits array of supported bits
	 */
	@SafeVarargs
	private TypePerms(int type, String labelKey, Pair<Feature, TypePerms> featureAndReplacement, TypePerms parent, NamedPerm...bits) {
		this(type, id -> I18NHelper.get(labelKey), featureAndReplacement, parent, null, null, false, false, bits);
	}

	/**
	 * Create an instance
	 * @param type numerical type
	 * @param labelFunction label function
	 * @param featureAndReplacement optional feature and replacement type
	 * @param parent parent instance
	 * @param instanceFunction optional instance function (null for types that have no instances)
	 * @param checkerFunction optional permission bit checker function
	 * @param hierarchical flag for hierarchical type (instances of the type can have children of the same type)
	 * @param roles flag for types supporting roles
	 * @param bits array of supported bits
	 */
	@SafeVarargs
	private TypePerms(int type, Function<Integer, String> labelFunction, Pair<Feature, TypePerms> featureAndReplacement, TypePerms parent, BiFunction<Integer, Integer, List<Integer>> instanceFunction,
			BiFunction<Integer, Integer, Boolean> checkerFunction, boolean hierarchical, boolean roles, NamedPerm... bits) {
		this.type = type;
		this.labelFunction = labelFunction;
		if (featureAndReplacement != null) {
			this.feature = featureAndReplacement.getLeft();
			this.replacement = featureAndReplacement.getRight();
		}
		this.parent = parent;
		this.instanceFunction = instanceFunction;
		this.checkerFunction = checkerFunction;
		this.hierarchical = hierarchical;
		this.roles = roles;
		this.bits = Arrays.asList(bits);
		for (int i = 0; i < bits.length; i++) {
			if (bits[i].getType().getBit() == PermHandler.PERM_VIEW) {
				viewBit = true;
			} else if (bits[i].getType().getBit() == PermHandler.PERM_CHANGE_PERM) {
				permBit = true;
			}
		}
	}

	/**
	 * Get the numerical type
	 * @return numerical type
	 */
	public int type() {
		return type;
	}

	/**
	 * Check whether the type is active.
	 * A type is active if it does not depend on a feature or the feature is active
	 * @return true for active
	 */
	public boolean isActive() {
		if (inactive.contains(this)) {
			return false;
		}
		if (feature != null) {
			return NodeConfigRuntimeConfiguration.isFeature(feature);
		} else {
			return true;
		}
	}

	/**
	 * Check whether the type has instances
	 * @return true iff type has instances
	 */
	public boolean hasInstances() {
		return instanceFunction != null;
	}

	/**
	 * Check whether the instance with given ID exists
	 * @param id instance ID
	 * @return true iff instance exists
	 * @throws NodeException
	 */
	public boolean hasInstance(int id) throws NodeException {
		// some types have no instances at all
		if (!hasInstances()) {
			return false;
		}

		// check for types, that have an object class
		Class<? extends NodeObject> objectClass = getObjectClass();
		if (objectClass != null) {
			// load the instance
			NodeObject instance = TransactionManager.getCurrentTransaction().getObject(objectClass, id);
			return instance != null;
		}

		// check whether there is a label for the instance
		return !StringUtils.isEmpty(getLabel(id));
	}

	/**
	 * Get the list of instance IDs for the given parent ID (if the parent type has instances)
	 * @param id parent ID (ignored, if parent has no instances)
	 * @param channelId optional channel ID
	 * @return ID list (empty list if type has no instances)
	 * @throws NodeException
	 */
	public List<Integer> getInstanceIds(Integer id, Integer channelId) throws NodeException {
		if (instanceFunction != null) {
			return instanceFunction.apply(id, channelId);
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Get the supported permission bits
	 * @return bit list
	 */
	public List<NamedPerm> getBits() {
		return getBits(false);
	}

	/**
	 * Get the supported permission bits
	 * @param includeInactive true to include inactive perms
	 * @return bit list
	 */
	public List<NamedPerm> getBits(boolean includeInactive) {
		Stream<NamedPerm> stream = bits.stream();
		if (!includeInactive) {
			stream = stream.filter(NamedPerm::isActive);
		}
		return stream.collect(Collectors.toList());
	}

	/**
	 * Make a pattern out of the given permissions. All bits in the given string, that are not supported by the type
	 * will be replaced with '.'
	 * @param perms perms
	 * @return pattern
	 */
	public String pattern(String perms) {
		return pattern(perms, false);
	}

	/**
	 * Make a pattern out of the given permissions. All bits in the given string, that are not supported by the type
	 * will be replaced with '.'
	 * @param perms perms
	 * @param includeInactive true to include inactive perms
	 * @return pattern
	 */
	public String pattern(String perms, boolean includeInactive) {
		char[] patternCharacters = StringUtils.repeat('.', perms.length()).toCharArray();
		for (NamedPerm perm : getBits(includeInactive)) {
			int bit = perm.getType().getBit();
			if (bit < 0) {
				continue;
			}
			if (bit < patternCharacters.length) {
				patternCharacters[bit] = perms.charAt(bit);
			}
		}
		return new String(patternCharacters);
	}

	/**
	 * Get the parent type
	 * @return parent type (may be null)
	 */
	public TypePerms getParent() {
		return parent;
	}

	/**
	 * Get the child types
	 * @return set of child types (empty if type has no children)
	 */
	public List<TypePerms> getChildTypes() {
		return childMap.getOrDefault(this, Collections.emptyList()).stream().filter(TypePerms::isActive).collect(Collectors.toList());
	}

	/**
	 * Check whether the type has (visible) children, which might either be visible child types or child instances
	 * @return true for children
	 * @throws NodeException
	 */
	public boolean hasChildren() throws NodeException {
		return hasChildren(null, null);
	}

	/**
	 * Check whether the type has (visible) children, which might either be visible child types or child instances
	 * @param id optional instance ID
	 * @param channelId optional channel ID
	 * @return true for children
	 * @throws NodeException
	 */
	public boolean hasChildren(Integer id, Integer channelId) throws NodeException {
		for (TypePerms type : getChildTypes()) {
			if (type.hasInstances()) {
				for (Integer childId : type.getInstanceIds(id, channelId)) {
					if (type.canView(childId)) {
						return true;
					}
				}
			} else {
				if (type.canView()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the current user can see the type.
	 * @return true iff current user can see the type
	 * @throws NodeException
	 */
	public boolean canView() throws NodeException {
		return canView(null);
	}

	/**
	 * Check whether the current user can see the type/instance.
	 * @param id optional ID, if the type has instances
	 * @return true iff current user can see the type/instance.
	 * @throws NodeException
	 */
	public boolean canView(Integer id) throws NodeException {
		Boolean result = checkerFunction != null ? checkerFunction.apply(PermHandler.PERM_VIEW, id) : null;
		if (result != null) {
			return result.booleanValue();
		} else if (viewBit) {
			PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
			return permHandler.checkPermissionBits(type, id, PermHandler.PERM_VIEW);
		} else if (parent != null) {
			return parent.canView();
		} else {
			return false;
		}
	}

	/**
	 * Check whether the current user can set permissions on the type.
	 * If the type does not support bit 1, the call will be propagated to the parent type.
	 * If the type does not have a parent, the method will return false
	 * @return true iff current user can set permissions
	 * @throws NodeException
	 */
	public boolean canSetPerms() throws NodeException {
		return canSetPerms(null);
	}

	/**
	 * Check whether the current user can set permissions on the type/instance.
	 * If the type does not support bit 1, the call will be propagated to the parent type.
	 * If the type does not have a parent, the method will return false
	 * @param id optional ID, if the type has instances
	 * @return true iff current user can set permissions.
	 * @throws NodeException
	 */
	public boolean canSetPerms(Integer id) throws NodeException {
		Boolean result = checkerFunction != null ? checkerFunction.apply(PermHandler.PERM_CHANGE_PERM, id) : null;
		if (result != null) {
			return result.booleanValue();
		} else if (permBit) {
			// the type has its own permBit, so check it
			PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
			return permHandler.checkPermissionBits(type, id, PermHandler.PERM_VIEW, PermHandler.PERM_CHANGE_PERM);
		} else if (parent != null) {
			// the type has no own permBit, but it has a parent, so check for the parent
			return parent.canSetPerms();
		} else {
			return false;
		}
	}

	/**
	 * Get translated label for the type
	 * @return translated label (may be null)
	 * @throws NodeException
	 */
	public String getLabel() throws NodeException {
		return getLabel(null);
	}

	/**
	 * Get translated label for the type/instance
	 * @param id optional instance id
	 * @return translated label (may be null)
	 * @throws NodeException
	 */
	public String getLabel(Integer id) throws NodeException {
		if (labelFunction != null) {
			return labelFunction.apply(id);
		} else {
			return null;
		}
	}

	/**
	 * Check whether the type supports roles
	 * @return true for roles
	 */
	public boolean isRoles() {
		return roles && NodeConfigRuntimeConfiguration.isFeature(Feature.ROLES);
	}

	/**
	 * Get the object class, if the type has one. Otherwise null
	 * @return object class (may be null)
	 * @throws NodeException
	 */
	public Class<? extends NodeObject> getObjectClass() throws NodeException {
		return TransactionManager.getCurrentTransaction().getClass(type);
	}

	/**
	 * Get the permissions for the current user for this type
	 * @return permission pair
	 * @throws NodeException
	 */
	public PermissionPair getTypePermissions() throws NodeException {
		if (isActive()) {
			return TransactionManager.getCurrentTransaction().getPermHandler().getPermissions(type(), null, -1, 0);
		} else if (replacement != null) {
			return replacement.getTypePermissions();
		} else {
			return new PermissionPair();
		}
	}

	/**
	 * Get the permissions for the current user for an instance of this type
	 * @param objId instance ID
	 * @param roleCheckType optional type for checking roles
	 * @param languageId optional language ID
	 * @return permission pair
	 * @throws NodeException
	 */
	public PermissionPair getInstancePermissions(int objId, int roleCheckType, int languageId) throws NodeException {
		if (isActive()) {
			return TransactionManager.getCurrentTransaction().getPermHandler().getPermissions(type(), objId, roleCheckType, languageId);
		} else if (replacement != null) {
			return replacement.getTypePermissions();
		} else {
			return new PermissionPair();
		}
	}

	/**
	 * Get the permissions map for the type and optional object ID
	 * @param type permissions type
	 * @param objId optional object ID (if type does not support instances, this should be null)
	 * @return permissions map
	 * @throws NodeException
	 */
	public PermissionsMap getPermissionMap(Integer objId) throws NodeException {
		if (isActive()) {
			if (!hasInstances()) {
				objId = null;
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			PermHandler permHandler = t.getPermHandler();
			PermissionsMap map = new PermissionsMap();
			Map<PermType, Boolean> permissions = new TreeMap<>();
			map.setPermissions(permissions);

			switch (this) {
			case node:
			case folder:
				Folder folder = t.getObject(Folder.class, objId);
				if (folder == null) {
					return null;
				}
				
				// add role permissions
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.ROLES)) {
					RolePermissions rolePermissions = new RolePermissions();
					map.setRolePermissions(rolePermissions);
					
					BiFunction<PermissionPair, Function<PermType, Integer>, Map<PermType, Boolean>> roleMapFunction = (pair, bitExtractor) -> {
						Map<PermType, Boolean> roleMap = new TreeMap<>();
						for (NamedPerm perm : getBits()) {
							PermType permType = perm.getType();
							int bit = bitExtractor.apply(permType);
							if (bit < 0) {
								continue;
							}
							roleMap.put(permType, pair.checkPermissionBits(-1, bit));
						}
						return roleMap;
					};
					
					// global page permissions
					rolePermissions.setPage(roleMapFunction.apply(permHandler.getPermissions(type(), objId, Page.TYPE_PAGE, 0), PermType::getPageRoleBit));
					// global file permissions
					rolePermissions.setFile(roleMapFunction.apply(permHandler.getPermissions(type(), objId, File.TYPE_FILE, 0), PermType::getFileRoleBit));
					
					// get all language specific permissions
					List<ContentLanguage> langs = folder.getNode().getLanguages();
					if (!ObjectTransformer.isEmpty(langs)) {
						Map<String, Map <PermType, Boolean>> langMap = new TreeMap<>();
						rolePermissions.setPageLanguages(langMap);
						for (ContentLanguage lang : langs) {
							langMap.put(lang.getCode(), roleMapFunction.apply(permHandler.getPermissions(type(), objId, Page.TYPE_PAGE, lang.getId()), PermType::getPageRoleBit));
						}
					}
				}
				break;
			default:
				break;
			}

			PermissionPair permissionPair = permHandler.getPermissions(type(), objId, -1, -1);
			for (NamedPerm perm : getBits()) {
				PermType permType = perm.getType();
				if (permType.getBit() < 0) {
					continue;
				}
				permissions.put(permType, permissionPair.checkPermissionBits(permType.getBit(), -1));
			}

			return map;
		} else if (replacement != null) {
			return replacement.getPermissionMap(objId);
		} else {
			return new PermissionsMap();
		}
	}

	@Override
	public String toString() {
		return String.format("%s (%d)", name(), type);
	}
}
