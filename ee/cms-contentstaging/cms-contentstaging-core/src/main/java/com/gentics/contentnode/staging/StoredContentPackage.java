package com.gentics.contentnode.staging;

import java.util.Calendar;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.rest.model.staging.ContentPackageBase;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;

/**
 * Content staging package REST entity.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@StoredType(StagedContentPackage.TYPE_NAME)
public class StoredContentPackage extends ContentPackageBase implements StoredModel {

	private static final long serialVersionUID = -6438718314023524153L;

	/**
	 * Adapter from REST model into the storage model.
	 */
	public static final BiFunction<StagedContentPackage, StoredContentPackage, StoredContentPackage> REST2NODE = (from, to) -> {
		to.clone(from, false);
		to.setTimestamp((int) (Calendar.getInstance().getTimeInMillis() / 1000));
		return to;
	};

	/**
	 * Adapter from the storage model into REST model.
	 */
	public static final BiFunction<StoredContentPackage, StagedContentPackage, StagedContentPackage> NODE2REST = (from, to) -> {
		to.clone(from, true);
		return to;
	};

	@Override
	@XmlTransient
	@JsonIgnore
    public String getTypeName() {
		return StagedContentPackage.TYPE_NAME;
	}

	@Override
	@XmlTransient
	@JsonIgnore
    public String getSuffix() {
		return StringUtils.EMPTY;
	}

	@Override
	public String getId() {
		return StringUtils.EMPTY;
	}

	@Override
	public void setId(String string) {
	}
}
