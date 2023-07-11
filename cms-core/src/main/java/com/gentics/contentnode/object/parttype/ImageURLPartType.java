/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: ImageURLPartType.java,v 1.3 2008-08-20 14:01:14 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 6 - URL (image)
 */
public class ImageURLPartType extends UrlPartType {

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public ImageURLPartType(Value value) throws NodeException {
		super(value, UrlPartType.TARGET_IMAGE);
	}

	@Override
	public int getInternal() {
		// image urls are always "internal"
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
	 * Get the target image
	 * @return target image (or null)
	 * @throws NodeException
	 */
	public ImageFile getTargetImage() throws NodeException {
		return (ImageFile) getTarget();
	}

	/**
	 * Set the target image or null to unset
	 * @param image target file or null
	 * @throws NodeException
	 */
	public void setTargetImage(ImageFile image) throws NodeException {
		Value value = getValueObject();

		// if a image is set, we replace it by its master
		if (image != null) {
			image = (ImageFile) image.getMaster();
		}

		// set the image id
		value.setValueRef(image == null ? 0 : ObjectTransformer.getInt(image.getId(), 0));
	}

	@Override
	public Type getPropertyType() {
		return Type.IMAGE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		if (getInternal() == 1) {
			property.setImageId(getValueObject().getValueRef());
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

		setTargetImage(t.getObject(ImageFile.class, property.getImageId(), -1, false));
		setNode(t.getObject(Node.class, property.getNodeId()));
	}
}
