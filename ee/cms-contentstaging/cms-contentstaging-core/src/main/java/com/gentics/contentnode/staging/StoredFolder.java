package com.gentics.contentnode.staging;

import java.util.Collections;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Storage folder entity for Content Staging. Currently mirrors the REST API folder entity;
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedFolder.TYPE_NAME)
public class StoredFolder extends StagedFolder implements StoredModel {

	private static final long serialVersionUID = 2144865542004355956L;

	public static final BiFunction<StagedFolder, StoredFolder, StoredFolder> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredFolder, StagedFolder, StagedFolder> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	@Override
	public void makeSafe() {
		setObjectTags(Collections.emptyMap());
	}
}
