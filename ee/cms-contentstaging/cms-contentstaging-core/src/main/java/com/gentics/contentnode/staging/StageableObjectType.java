package com.gentics.contentnode.staging;

import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.StageableNodeObject;

/**
 * Node object types, supported by content staging
 * 
 * @author plyhun
 *
 */
public enum StageableObjectType {
	NODE(Node.class, StagedNode.class),
	FOLDER(Folder.class, StagedFolder.class),
	FILE(File.class, StagedFile.class),
	IMAGE(ImageFile.class, StagedImage.class),
	FORM(Form.class, StagedForm.class),
	PAGE(Page.class, StagedPage.class);

	private final Class<? extends StageableNodeObject> nodeObjectClass;
	private final Class<? extends StagingModel> stagingModelClass;

	private StageableObjectType(Class<? extends StageableNodeObject> nodeObjectClass, Class<? extends StagingModel> stagingModelClass) {
		this.stagingModelClass = stagingModelClass;
		this.nodeObjectClass = nodeObjectClass;
	}

	public Class<? extends StageableNodeObject> getNodeObjectClass() {
		return nodeObjectClass;
	}

	public Class<? extends StagingModel> getStagingModelClass() {
		return stagingModelClass;
	}
	
	public String getTypeId() {
		return name().toLowerCase();
	}

	public static final StageableObjectType parseTypeId(String typeId) {
		switch (typeId) {
		case "node":
			return NODE;
		case "folder":
			return FOLDER;
		case "file":
			return FILE;
		case "page":
			return PAGE;
		case "image":
			return IMAGE;
		case "form":
			return FORM;
		default:
			throw new IllegalArgumentException("Unsupported node object type: " + typeId);
		}
	}
}
