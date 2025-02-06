package com.gentics.contentnode.staging;

import java.util.Collections;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Storage entity for the file, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedFile.TYPE_NAME)
public class StoredFile extends StagedFile implements StoredModel {

	private static final long serialVersionUID = -1935799142638489825L;

	public static final BiFunction<StagedFile, StoredFile, StoredFile> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredFile, StagedFile, StagedFile> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	@Override
	public void makeSafe() {
		setObjectTags(Collections.emptyMap());
	}
}
