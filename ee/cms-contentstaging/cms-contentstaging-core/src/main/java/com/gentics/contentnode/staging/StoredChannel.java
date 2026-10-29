package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Stored channel info for Content Staging package entity.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedChannel.TYPE_NAME)
public class StoredChannel extends StagedChannel implements StoredModel {

	private static final long serialVersionUID = 8974036508418574000L;

	public static final BiFunction<StagedChannel, StoredChannel, StoredChannel> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredChannel, StagedChannel, StagedChannel> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};
}
