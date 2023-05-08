/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: PageTagPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 11 - Tag (page)
 */
public class PageTagPartType extends TagPartType {

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public PageTagPartType(Value value) throws NodeException {
		super(value, TagPartType.TYPE_PAGE);
	}

	/**
	 * Set the pagetag
	 * @param page page to set
	 * @param tag tag in the page (either a contenttag or a templatetag)
	 * @throws NodeException
	 */
	public void setPageTag(Page page, Tag tag) throws NodeException {
		Value value = getValueObject();

		if (page == null || tag == null) {
			// unset the data
			value.setInfo(0);
			value.setValueRef(0);
			value.setValueText("p");
		} else {
			// replace page by its master
			Page originalPage = page;

			page = page.getMaster();

			if (!originalPage.equals(page)) {
				// we also need to replace the tag

				if (tag instanceof ContentTag) {
					ContentTag newTag = page.getContentTag(tag.getName());

					if (newTag != null) {
						tag = newTag;
					}
				} else if (tag instanceof TemplateTag) {
					TemplateTag newTag = page.getTemplate().getTemplateTag(tag.getName());

					if (newTag != null) {
						tag = newTag;
					}
				}
			}

			// set the data
			value.setInfo(ObjectTransformer.getInt(page.getId(), 0));
			value.setValueRef(ObjectTransformer.getInt(tag.getId(), 0));
			if (tag instanceof ContentTag) {
				value.setValueText("p");
			} else {
				value.setValueText("t");
			}
		}
	}

	@Override
	public Type getPropertyType() {
		return Type.PAGETAG;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		Value nodeValue = getValueObject();
		property.setPageId(nodeValue.getInfo());
		if ("p".equals(nodeValue.getValueText())) {
			property.setContentTagId(nodeValue.getValueRef());
		} else if ("t".equals(nodeValue.getValueText())) {
			property.setTemplateTagId(nodeValue.getValueRef());
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Tag pageTag = null;

		if (property.getContentTagId() != null) {
			pageTag = t.getObject(ContentTag.class, property.getContentTagId());
		} else if (property.getTemplateTagId() != null) {
			pageTag = t.getObject(TemplateTag.class, property.getTemplateTagId());
		}
		// set page and tag
		setPageTag(t.getObject(Page.class, property.getPageId()), pageTag);
	}
}
