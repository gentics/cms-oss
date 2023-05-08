/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: PageURLPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 4 - URL (page)
 */
public class PageURLPartType extends UrlPartType {
	
	public static int EXTERNAL_URL_TYPE = 4;
	public static int EXTERNAL_URL_INFO = 0;

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public PageURLPartType(Value value) throws NodeException {
		super(value, UrlPartType.TARGET_PAGE);
	}

	/**
	 * Get the target page for internal links or null for external links
	 * @return target page or null
	 * @throws NodeException
	 */
	public Page getTargetPage() throws NodeException {
		return (Page) getTarget();
	}

	/**
	 * Get the external target URL or null if this is an internal link
	 * @return external target URL or null
	 */
	public String getExternalTarget() {
		return getValueObject().getValueText();
	}

	/**
	 * Set the target page (and make this an internal link)
	 * @param page target page (may be null to unset)
	 * @throws NodeException
	 */
	public void setTargetPage(Page page) throws NodeException {
		Value value = getValueObject();

		// first make this an internal link
		value.setInfo(1);

		// if a page is set, we replace it by its master
		if (page != null) {
			page = page.getMaster();
		}

		// set the page id
		value.setValueRef(page == null ? 0 : ObjectTransformer.getInt(page.getId(), 0));

		// unset the valuetext, if it has not been set to a node before
		if (getNode() == null) {
			value.setValueText(null);
		}
	}

	/**
	 * Set the external target (which makes this an external link)
	 * @param target target URL
	 * @throws NodeException
	 */
	public void setExternalTarget(String target) throws NodeException {
		Value value = getValueObject();

		// first make this an external link
		value.setInfo(0);

		// set the target URL
		value.setValueText(target);

		// unset the page id
		value.setValueRef(0);
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.PAGE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		if (getInternal() == 1) {
			property.setPageId(getValueObject().getValueRef());
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
		if (property.getPageId() != null) {
			setTargetPage(t.getObject(Page.class, property.getPageId()));
			setNode(t.getObject(Node.class, property.getNodeId()));
		} else {
			setExternalTarget(property.getStringValue());
		}
	}
}
