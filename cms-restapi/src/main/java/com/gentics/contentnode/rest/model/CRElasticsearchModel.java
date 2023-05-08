package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model of the Elasticsearch specific configuration for Mesh CRs
 */
public class CRElasticsearchModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8610264447055720875L;

	protected JsonNode page;

	protected JsonNode folder;

	protected JsonNode file;

	public JsonNode getPage() {
		return page;
	}

	public void setPage(JsonNode page) {
		this.page = page;
	}

	public JsonNode getFolder() {
		return folder;
	}

	public void setFolder(JsonNode folder) {
		this.folder = folder;
	}

	public JsonNode getFile() {
		return file;
	}

	public void setFile(JsonNode file) {
		this.file = file;
	}
}
