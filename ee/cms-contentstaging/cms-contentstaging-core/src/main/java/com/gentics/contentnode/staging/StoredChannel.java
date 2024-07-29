package com.gentics.contentnode.staging;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.object.Node.UrlRenderWay;

/**
 * Stored channel info for Content Staging package entity.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType("channel")
public class StoredChannel extends StagedChannel implements StoredModel {

	private static final long serialVersionUID = 8974036508418574000L;

	private UrlRenderWay pageUrls;
	private UrlRenderWay fileUrls;

	public static final BiFunction<StagedChannel, StoredChannel, StoredChannel> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredChannel, StagedChannel, StagedChannel> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	public UrlRenderWay getPageUrls() {
		return pageUrls;
	}

	public void setPageUrls(UrlRenderWay pageUrls) {
		this.pageUrls = pageUrls;
	}

	public UrlRenderWay getFileUrls() {
		return fileUrls;
	}

	public void setFileUrls(UrlRenderWay fileUrls) {
		this.fileUrls = fileUrls;
	}

	@XmlTransient
	@JsonIgnore
	public void clone(StagedChannel origin) {
		super.clone(origin);
		if (origin != null && origin instanceof StoredChannel) {
			StoredChannel so = (StoredChannel) origin;
			this.pageUrls = so.pageUrls;
			this.fileUrls = so.fileUrls;
		}		
	}
}
