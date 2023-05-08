/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: MultiSelectPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 30 - Select (multiple)
 */
public class MultiSelectPartType extends SelectPartType {
	/**
	 * Type ID
	 */
	public final static int TYPE_ID = 30;

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -5895777349699922185L;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public MultiSelectPartType(Value value) throws NodeException {
		super(value);
	}

	/**
	 * Set the entries to be selected
	 * @param entries array of entries to be selected
	 * @throws NodeException
	 */
	public void setSelected(DatasourceEntry...entries) throws NodeException {
		Value value = getValueObject();
		value.setValueText(Arrays.asList(entries).stream().map(e -> Integer.toString(e.getDsid())).collect(Collectors.joining("|-|")));
		readSelection(true);
	}

	@Override
	public Type getPropertyType() {
		return Type.MULTISELECT;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		List<SelectOption> selectedOptions = property.getSelectedOptions();
		StringBuilder valueText = new StringBuilder();

		if (selectedOptions != null) {
			for (SelectOption option : selectedOptions) {
				if (valueText.length() > 0) {
					valueText.append("|-|");
				}

				if (option.getId() == -1) {
					// Siteminder datasource options are saved by value, not by ID
					valueText.append(option.getValue());
				} else {
					valueText.append(option.getId());
				}
			}
		}
		Value value = getValueObject();
		value.setValueRef(null == property.getDatasourceId() ? 0 : property.getDatasourceId());
		value.setValueText(valueText.toString());
		readSelection(true);
	}
}
