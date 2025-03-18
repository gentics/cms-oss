package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for getting files/images of folder
 */
public class FileListParameterBean {

	/**
	 * node id for this folder - for use with multichannelling
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * true to only return inherited files in the given node, false to only get local/localized files, null to get local and inherited files
	 */
	@QueryParam("inherited")
	public Boolean inherited;

	/**
	 * true to only return online files, false to only get offline files, null to get both online and offline files
	 */
	@QueryParam("online")
	public Boolean online;

	/**
	 * true to only return broken files (where the binary data is missing), false to only get non-broken files, null to get both
	 */
	@QueryParam("broken")
	public Boolean broken;

	/**
	 * true if only used files that are referenced somewhere shall be fetched,
	 * false for unused files. If "usedIn" is not specified, this filter will
	 * only check in the current channel
	 */
	@QueryParam("used")
	public Boolean used;

	/**
	 * optional list of channel IDs for extending the "used" filter to multiple channels.
	 */
	@QueryParam("usedIn")
	public List<Integer> usedIn;

	/**
	 * True to add the folder information to the returned objects
	 */
	@QueryParam("folder")
	@DefaultValue("false")
	public boolean folder = false;

	/**
	 * optional regular expression to get files with a nice URL.
	 */
	@QueryParam("niceurl")
	public String niceUrl;

	public FileListParameterBean setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public FileListParameterBean setInherited(Boolean inherited) {
		this.inherited = inherited;
		return this;
	}

	public FileListParameterBean setOnline(Boolean online) {
		this.online = online;
		return this;
	}

	public FileListParameterBean setBroken(Boolean broken) {
		this.broken = broken;
		return this;
	}

	public FileListParameterBean setUsed(Boolean used) {
		this.used = used;
		return this;
	}

	public FileListParameterBean setUsedIn(List<Integer> usedIn) {
		this.usedIn = usedIn;
		return this;
	}


	public FileListParameterBean setFolder(boolean folder) {
		this.folder = folder;
		return this;
	}

	public FileListParameterBean setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
		return this;
	}
}
