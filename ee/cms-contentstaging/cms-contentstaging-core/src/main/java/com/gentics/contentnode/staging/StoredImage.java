package com.gentics.contentnode.staging;

import java.util.Collections;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Storage entity for the image, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedImage.TYPE_NAME)
public class StoredImage extends StagedImage implements StoredModel {

	private static final long serialVersionUID = 8948803662162752664L;

	public static final BiFunction<StagedImage, StoredImage, StoredImage> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredImage, StagedImage, StagedImage> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	@Override
	public void makeSafe() {
		setObjectTags(Collections.emptyMap());
	}
}
