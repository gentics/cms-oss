package com.gentics.contentnode.etc;

import static com.gentics.contentnode.etc.Feature.ALOHA_ANNOTATE_EDITABLES;
import static com.gentics.contentnode.etc.Feature.ALWAYS_LOCALIZE;
import static com.gentics.contentnode.etc.Feature.ASSET_MANAGEMENT;
import static com.gentics.contentnode.etc.Feature.ATTRIBUTE_DIRTING;
import static com.gentics.contentnode.etc.Feature.CHANNELSYNC;
import static com.gentics.contentnode.etc.Feature.CONSTRUCT_CATEGORIES;
import static com.gentics.contentnode.etc.Feature.CONTENTFILE_AUTO_OFFLINE;
import static com.gentics.contentnode.etc.Feature.CONTENTGROUP3;
import static com.gentics.contentnode.etc.Feature.CONTENTGROUP3_PAGEFILENAME;
import static com.gentics.contentnode.etc.Feature.CONTENTGROUP3_PAGEFILENAME_NO_APACHEFILENAME;
import static com.gentics.contentnode.etc.Feature.COPY_TAGS;
import static com.gentics.contentnode.etc.Feature.CR_FILESYSTEM_ATTRIBUTES;
import static com.gentics.contentnode.etc.Feature.DATASOURCE_PERM;
import static com.gentics.contentnode.etc.Feature.DEVTOOLS;
import static com.gentics.contentnode.etc.Feature.DISABLE_INSTANT_DELETE;
import static com.gentics.contentnode.etc.Feature.DSFALLBACK;
import static com.gentics.contentnode.etc.Feature.DS_EMPTY_CS;
import static com.gentics.contentnode.etc.Feature.DS_FOLDER_PERM;
import static com.gentics.contentnode.etc.Feature.DS_FOLDER_WORKFLOW;
import static com.gentics.contentnode.etc.Feature.FILENAME_FORCETOLOWER;
import static com.gentics.contentnode.etc.Feature.FOLDER_BASED_TEMPLATE_SELECTION;
import static com.gentics.contentnode.etc.Feature.GET_FILENAME_AS_PAGENAME;
import static com.gentics.contentnode.etc.Feature.HIDE_MANUAL;
import static com.gentics.contentnode.etc.Feature.HTML_IMPORT;
import static com.gentics.contentnode.etc.Feature.HTTP_AUTH_LOGIN;
import static com.gentics.contentnode.etc.Feature.INSECURE_SCHEDULER_COMMAND;
import static com.gentics.contentnode.etc.Feature.INSTANT_CR_PUBLISHING;
import static com.gentics.contentnode.etc.Feature.INVALIDPAGEURLMSG;
import static com.gentics.contentnode.etc.Feature.LIVEEDIT_TAG_PERCONSTRUCT;
import static com.gentics.contentnode.etc.Feature.LIVE_URLS;
import static com.gentics.contentnode.etc.Feature.LIVE_URLS_PER_NODE;
import static com.gentics.contentnode.etc.Feature.MANAGELINKURL;
import static com.gentics.contentnode.etc.Feature.MANAGELINKURL_ONLYFORPUBLISH;
import static com.gentics.contentnode.etc.Feature.MESH_CONTENTREPOSITORY;
import static com.gentics.contentnode.etc.Feature.MOVE_PERM_WITH_EDIT;
import static com.gentics.contentnode.etc.Feature.MULTITHREADED_PUBLISHING;
import static com.gentics.contentnode.etc.Feature.NICE_URLS;
import static com.gentics.contentnode.etc.Feature.OBJTAG_SYNC;
import static com.gentics.contentnode.etc.Feature.OMIT_PUBLISH_TABLE;
import static com.gentics.contentnode.etc.Feature.PAGEVAR_ALL_CONTENTGROUPS;
import static com.gentics.contentnode.etc.Feature.PUBLISH_CACHE;
import static com.gentics.contentnode.etc.Feature.PUBLISH_FOLDER_STARTPAGE;
import static com.gentics.contentnode.etc.Feature.PUBLISH_INHERITED_SOURCE;
import static com.gentics.contentnode.etc.Feature.PUBLISH_STATS;
import static com.gentics.contentnode.etc.Feature.PUB_DIR_SEGMENT;
import static com.gentics.contentnode.etc.Feature.RESUMABLE_PUBLISH_PROCESS;
import static com.gentics.contentnode.etc.Feature.ROLES;
import static com.gentics.contentnode.etc.Feature.SUSPEND_SCHEDULER;
import static com.gentics.contentnode.etc.Feature.TAG_IMAGE_RESIZER;
import static com.gentics.contentnode.etc.Feature.UPLOAD_FILE_PROPERTIES;
import static com.gentics.contentnode.etc.Feature.UPLOAD_IMAGE_PROPERTIES;
import static com.gentics.contentnode.etc.Feature.USERSNAP;
import static com.gentics.contentnode.etc.Feature.VIEW_PERMS;
import static com.gentics.contentnode.etc.Feature.WASTEBIN;
import static com.gentics.contentnode.etc.Feature.WEBP_CONVERSION;

import java.util.Arrays;
import java.util.List;

/**
 * Service that provides the basic features
 */
public class BasicFeatureService implements FeatureService {
	/**
	 * List of basic features
	 */
	private List<Feature> provided = Arrays.asList(TAG_IMAGE_RESIZER, PAGEVAR_ALL_CONTENTGROUPS,
			DATASOURCE_PERM, HTTP_AUTH_LOGIN, ALOHA_ANNOTATE_EDITABLES, COPY_TAGS,
			CR_FILESYSTEM_ATTRIBUTES, CONTENTFILE_AUTO_OFFLINE, ALWAYS_LOCALIZE, ROLES, PUBLISH_STATS,
			RESUMABLE_PUBLISH_PROCESS, DISABLE_INSTANT_DELETE, PUBLISH_FOLDER_STARTPAGE, PUBLISH_INHERITED_SOURCE,
			PUBLISH_CACHE, OMIT_PUBLISH_TABLE, CONTENTGROUP3, MOVE_PERM_WITH_EDIT, WASTEBIN, ATTRIBUTE_DIRTING,
			MULTITHREADED_PUBLISHING, INVALIDPAGEURLMSG, DEVTOOLS, NICE_URLS,
			MESH_CONTENTREPOSITORY, INSTANT_CR_PUBLISHING, PUB_DIR_SEGMENT, SUSPEND_SCHEDULER,
			INSECURE_SCHEDULER_COMMAND, CONTENTGROUP3_PAGEFILENAME, CONTENTGROUP3_PAGEFILENAME_NO_APACHEFILENAME,
			MANAGELINKURL, MANAGELINKURL_ONLYFORPUBLISH, DSFALLBACK, DS_EMPTY_CS,
			GET_FILENAME_AS_PAGENAME, FILENAME_FORCETOLOWER, LIVEEDIT_TAG_PERCONSTRUCT, LIVE_URLS, LIVE_URLS_PER_NODE,
			VIEW_PERMS, DS_FOLDER_PERM, DS_FOLDER_WORKFLOW, HTML_IMPORT, CHANNELSYNC, CONSTRUCT_CATEGORIES, USERSNAP,
			OBJTAG_SYNC, HIDE_MANUAL, ASSET_MANAGEMENT, FOLDER_BASED_TEMPLATE_SELECTION, WEBP_CONVERSION,
			UPLOAD_FILE_PROPERTIES, UPLOAD_IMAGE_PROPERTIES);

	@Override
	public boolean isProvided(Feature feature) {
		return provided.contains(feature);
	}
}
