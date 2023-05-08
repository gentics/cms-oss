package com.gentics.contentnode.rest.util;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.etc.StringUtils;

/**
 * Implementation that will filter the NodeOjects by evaluating the given getter
 * Method and comparing the result (which must be a String) with the given
 * reference result or the given pattern.
 */
public class StringFilter extends AbstractNodeObjectFilter {

	/**
	 * Enum for the Case
	 */
	public static enum Case {

		/**
		 * Check will be done case sensitive
		 */
		SENSITIVE, /**
		 * Check will be done case insensitive
		 */ INSENSITIVE
	}

	/**
	 * Reference result. Will be used, when no referencePattern is given
	 */
	protected String referenceValue;

	/**
	 * Reference pattern. Will be used, if set
	 */
	protected Pattern referencePattern;

	/**
	 * Getter method to get the value from the NodeObject
	 */
	protected Method getter;

	/**
	 * Case mode (case sensitive or insensitive)
	 */
	protected Case caseMode;

	public StringFilter(String referenceValue, Method getter,
			boolean supportLike, Case caseMode) {
		this.caseMode = caseMode;
		if (supportLike && !ObjectTransformer.isEmpty(referenceValue) && (referenceValue.contains("%") || referenceValue.contains("_"))) {
			this.referencePattern = Pattern.compile(StringUtils.likeStringToRegex(referenceValue),
					caseMode == Case.INSENSITIVE ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : null);
		} else {
			this.referenceValue = referenceValue;
		}
		this.getter = getter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics
	 * .lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		try {
			if (referencePattern != null) {
				return referencePattern.matcher(ObjectTransformer.getString(getter.invoke(object), "")).matches();
			} else {
				switch (caseMode) {
				case SENSITIVE:
					return ObjectTransformer.getString(getter.invoke(object), "").equals(ObjectTransformer.getString(referenceValue, ""));

				case INSENSITIVE:
					return ObjectTransformer.getString(getter.invoke(object), "").equalsIgnoreCase(ObjectTransformer.getString(referenceValue, ""));

				default:
					return true;
				}
			}
		} catch (Exception e) {
			throw new NodeException("Error while matching {" + object + "}", e);
		}
	}
}
