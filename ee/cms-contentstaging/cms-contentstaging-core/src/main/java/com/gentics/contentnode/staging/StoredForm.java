package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;

/**
 * Storage entity for the form, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedForm.TYPE_NAME)
public class StoredForm extends StagedForm implements StoredModel {

	private static final long serialVersionUID = 4937436950124555948L;

	public static final BiFunction<StagedForm, StoredForm, StoredForm> REST2NODE = (from, to) -> {
		to.clone(from);
		return to;
	};

	public static final BiFunction<StoredForm, StagedForm, StagedForm> NODE2REST = (from, to) -> {
		to.clone(from);
		return to;
	};

	@Override
	public void makeSafe() {
		setData(null);
	}
}
