/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: FileURLPartType.java,v 1.3 2008-08-20 14:01:14 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 8 - URL (file)
 */
public class FileURLPartType extends UrlPartType {

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public FileURLPartType(Value value) throws NodeException {
		super(value, UrlPartType.TARGET_FILE);
	}

	@Override
	public int getInternal() {
		// file urls are always "internal"
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.UrlPartType#get(java.lang.String)
	 */
	public Object get(String key) {
		if ("size".equals(key)) {
			NodeObject target = getTarget();

			if (target instanceof ContentFile) {
				return ((ContentFile) target).getFormattedSize();
			} else {
				return null;
			}
		} else {
			return super.get(key);
		}
	}

	/**
	 * Get the target file
	 * @return target file (or null)
	 * @throws NodeException
	 */
	public File getTargetFile() throws NodeException {
		return (File) getTarget();
	}

	/**
	 * Set the target file or null to unset
	 * @param file target file or null
	 * @throws NodeException
	 */
	public void setTargetFile(File file) throws NodeException {
		Value value = getValueObject();

		// if a file is set, we replace it by its master
		if (file != null) {
			file = file.getMaster();
		}

		// set the file id
		value.setValueRef(file == null ? 0 : ObjectTransformer.getInt(file.getId(), 0));
	}

	@Override
	public Type getPropertyType() {
		return Type.FILE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		if (getInternal() == 1) {
			property.setFileId(getValueObject().getValueRef());
			Node node = getNode();
			if (node != null) {
				property.setNodeId(node.getId());
			}
		} else {
			property.setStringValue(getValueObject().getValueText());
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		setTargetFile(t.getObject(File.class, property.getFileId()));
		setNode(t.getObject(Node.class, property.getNodeId()));
	}
}
