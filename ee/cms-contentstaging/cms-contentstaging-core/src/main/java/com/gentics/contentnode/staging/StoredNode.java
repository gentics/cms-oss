package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

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
	
	public static final BiFunction<StagedNode, StoredNode, StoredNode> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredNode, StagedNode, StagedNode> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};
}
