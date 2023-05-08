package com.gentics.contentnode.devtools.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public enum ObjectTagDefinitionTypeModel {
	folder(Folder.TYPE_FOLDER), template(Template.TYPE_TEMPLATE), page(Page.TYPE_PAGE), file(File.TYPE_FILE), image(ImageFile.TYPE_IMAGE);

	/**
	 * Get the type for the given value
	 * @param value value
	 * @return type or null
	 */
	public static ObjectTagDefinitionTypeModel fromValue(int value) {
		for (ObjectTagDefinitionTypeModel type : values()) {
			if (type.typeValue == value) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Type value
	 */
	private int typeValue;

	/**
	 * Create an instance
	 * @param typeValue type value
	 */
	private ObjectTagDefinitionTypeModel(int typeValue) {
		this.typeValue = typeValue;
	}

	/**
	 * Get the type value
	 * @return type value
	 */
	public int getTypeValue() {
		return typeValue;
	}
}
