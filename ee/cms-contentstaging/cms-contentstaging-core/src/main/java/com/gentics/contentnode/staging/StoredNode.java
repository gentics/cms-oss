package com.gentics.contentnode.staging;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.object.Node.UrlRenderWay;

/**
 * Storage entity for the node, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedNode.TYPE_NAME)
public class StoredNode extends StagedNode implements StoredModel {

	private static final long serialVersionUID = 3693393007670673734L;

	private UrlRenderWay pageUrls;
	private UrlRenderWay fileUrls;
	
	public static final BiFunction<StagedNode, StoredNode, StoredNode> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredNode, StagedNode, StagedNode> NODE2REST = (from, to) -> {
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
	public void clone(StagedNode origin) {
		super.clone(origin);
		if (origin != null && origin instanceof StoredNode) {
			StoredNode so = (StoredNode) origin;
			this.pageUrls = so.pageUrls;
			this.fileUrls = so.fileUrls;
		}		
	}
}
