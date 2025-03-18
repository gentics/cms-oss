package com.gentics.contentnode.staging;

import java.util.Collections;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Storage entity for the page, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedPage.TYPE_NAME)
public class StoredPage extends StagedPage implements StoredModel {

	private static final long serialVersionUID = -2612850855501620690L;

	public static final BiFunction<StagedPage, StoredPage, StoredPage> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredPage, StagedPage, StagedPage> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	@Override
	public void makeSafe() {
		setTags(Collections.emptyMap());
		setObjectTags(Collections.emptyMap());
	}
}
