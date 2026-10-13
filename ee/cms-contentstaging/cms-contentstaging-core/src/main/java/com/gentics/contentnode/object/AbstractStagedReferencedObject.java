package com.gentics.contentnode.object;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.rest.model.Property;

/**
 * Abstract wrapper implementation for implementations of {@link StagedReferencedObject}
 *
 * @param <T> node object type
 */
public abstract class AbstractStagedReferencedObject<T extends NodeObject> implements StagedReferencedObject<T> {
	/**
	 * Get a "normalized" version of the given string for hashing. If the string is null, return an empty string. Otherwise replace all CRLF by LF
	 * @param value value to "normalize" for hashing
	 * @return "normalized" value
	 */
	public static String getStringForHashing(String value) {
		if (value == null) {
			return "";
		} else {
			return StringUtils.replace(value, "\r\n", "\n");
		}
	}

	/**
	 * Get a "normalized" version of the given list of strings. See {@link #getStringForHashing(String)}.
	 * @param values values to "normalize"
	 * @return "normalized" values.
	 */
	public static List<String> getStringsForHashing(List<String> values) {
		if (values == null) {
			return null;
		} else {
			return values.stream().map(AbstractStagedReferencedObject::getStringForHashing).collect(Collectors.toList());
		}
	}

	/**
	 * Function that transforms a value into the model, which is used to calculate the implementation hash
	 */
	public final static BiFunction<Value, StagedValueModel, StagedValueModel> VALUE2MODEL = (from, to) -> {
		Transaction t = TransactionManager.getCurrentTransaction();

		Property property = from.getPartType().toProperty();
		to.setStringValue(getStringForHashing(property.getStringValue()));
		to.setStringValues(getStringsForHashing(property.getStringValues()));
		to.setBooleanValue(property.getBooleanValue());
		to.setDatasourceId(t.getGlobalId(Datasource.class, property.getDatasourceId()));
		switch(property.getType()) {
		case SELECT:
		case MULTISELECT:
			to.setOptions(property.getSelectedOptions());
			break;
		case DATASOURCE:
			to.setOptions(property.getOptions());
			break;
		default:
			break;
		}

		return to;
	};

	/**
	 * Wrapped object
	 */
	protected T wrapped;

	/**
	 * Create a wrapper
	 * @param wrapped wrapped object
	 */
	public AbstractStagedReferencedObject(T wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public GlobalId getGlobalId() {
		return wrapped.getGlobalId();
	}

	@Override
	public String toString() {
		String id = getGlobalId().toString();
		if (this.wrapped != null) {
			return String.format("StagedReference {%s, %s}", id, this.wrapped.toString());
		} else {
			return String.format("StagedReference {%s}", id);
		}
	}
}
