package com.gentics.contentnode.object.parttype;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Part Type for linkink internal forms
 */
public class CmsFormPartType extends AbstractPartType {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5891996840774671325L;

	private final static Set<String> resolvableKeys = SetUtils.unmodifiableSet("target");

	/**
	 * Create instance
	 * @param value value
	 * @throws NodeException
	 */
	public CmsFormPartType(Value value) throws NodeException {
		super(value);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	@Override
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	@Override
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		return isRequired() && getTarget() == null;
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof CmsFormPartType) {
			return Objects.equals(getTarget(), ((CmsFormPartType) other).getTarget());
		} else {
			return false;
		}
	}

	@Override
	public Type getPropertyType() {
		return Type.CMSFORM;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		if (property.getFormId() != null) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Form form = t.getObject(Form.class, property.getFormId());
			setTarget(form);
		}
	}

	@Override
	public String render(RenderResult renderResult, String template) throws NodeException {
		super.render(renderResult, template);
		Form form = getTarget();
		if (form != null) {
			return Integer.toString(form.getId());
		} else {
			return null;
		}
	}

	/**
	 * Get the target form, if set, otherwise null
	 * @return target form or null
	 * @throws NodeException
	 */
	public Form getTarget() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(Form.class, getValueObject().getValueRef());
	}

	/**
	 * Set the target form
	 * @param form target form
	 * @throws NodeException
	 */
	public void setTarget(Form form) throws NodeException {
		Value value = getValueObject();
		if (form != null) {
			value.setValueRef(form.getId());
		} else {
			value.setValueRef(0);
		}
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		Form form = getTarget();
		if (form != null) {
			property.setFormId(form.getId());
		} else {
			property.setFormId(0);
		}
	}
}
