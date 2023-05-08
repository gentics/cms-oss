/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: SingleSelectPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 29 - Select (single)
 */
public class SingleSelectPartType extends SelectPartType {
	/**
	 * Type ID
	 */
	public final static int TYPE_ID = 29;

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 293318830578927331L;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public SingleSelectPartType(Value value) throws NodeException {
		super(value);
	}

	/**
	 * Set the single entry to be selected
	 * @param entry entry
	 * @throws NodeException
	 */
	public void setSelected(DatasourceEntry entry) throws NodeException {
		Value value = getValueObject();
		if (entry != null) {
			value.setValueText(Integer.toString(entry.getDsid()));
		} else {
			value.setValueText("");
		}
		readSelection(true);
	}

	@Override
	public Type getPropertyType() {
		return Type.SELECT;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		List<SelectOption> selectedOptions = property.getSelectedOptions();

		Value value = getValueObject();
		if (!ObjectTransformer.isEmpty(selectedOptions)) {
			value.setValueText(selectedOptions.get(0).getId().toString());
		} else {
			value.setValueText("");
		}
		value.setValueRef(null == property.getDatasourceId() ? 0 : property.getDatasourceId());
		readSelection(true);
	}
}
