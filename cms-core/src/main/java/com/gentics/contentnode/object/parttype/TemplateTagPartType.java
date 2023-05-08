/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: TemplateTagPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 20 - Tag (template)
 */
public class TemplateTagPartType extends TagPartType {

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public TemplateTagPartType(Value value) throws NodeException {
		super(value, TagPartType.TYPE_TEMPLATE);
	}

	/**
	 * Set the tempalte and template tag
	 * @param template template
	 * @param tag template tag
	 */
	public void setTemplateTag(Template template, TemplateTag tag) throws NodeException {
		Value value = getValueObject();

		if (template == null || tag == null) {
			// unset the data
			value.setInfo(0);
			value.setValueRef(0);
			value.setValueText("t");
		} else {
			// replace by the master template
			Template originalTemplate = template;

			template = template.getMaster();

			if (!originalTemplate.equals(template)) {
				// we also need to replace the tag
				tag = template.getTemplateTag(tag.getName());
			}

			value.setInfo(ObjectTransformer.getInt(template.getId(), 0));
			value.setValueRef(ObjectTransformer.getInt(tag.getId(), 0));
			value.setValueText("t");
		}
	}

	@Override
	public Type getPropertyType() {
		return Type.TEMPLATETAG;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		Value nodeValue = getValueObject();
		property.setTemplateId(nodeValue.getInfo());
		property.setTemplateTagId(nodeValue.getValueRef());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		setTemplateTag(t.getObject(Template.class, property.getTemplateId()),
				t.getObject(TemplateTag.class, property.getTemplateTagId()));
	}
}
