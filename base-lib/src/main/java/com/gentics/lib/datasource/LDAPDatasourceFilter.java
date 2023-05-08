/*
 * @author norbert
 * @date 25.07.2006
 * @version $Id: LDAPDatasourceFilter.java,v 1.10 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.Map;

import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.filtergenerator.AbstractDatasourceFilter;
import com.gentics.lib.expressionparser.filtergenerator.ConstantFilterPart;

/**
 * Implementation of
 * {@link com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter} for
 * {@link com.gentics.lib.datasource.LDAPDatasource}s.
 */
public class LDAPDatasourceFilter extends AbstractDatasourceFilter {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -4867239636273516874L;

	public final static Class[] LDAPDATASOURCEFILTER_FUNCTION = new Class[] { LDAPDatasource.class};

	/**
	 * Create an instance of the filter
	 *
	 */
	public LDAPDatasourceFilter() {
		super();
	}

	/**
	 * Create an instance of the filter using shared resolvables
	 * @param sharedResolvables shared resolvables
	 */
	public LDAPDatasourceFilter(Map sharedResolvables) {
		super(sharedResolvables);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getVariableName(java.lang.String)
	 */
	public String getVariableName(String expressionName, int expectedValueType) throws FilterGeneratorException {
		// string leading "object."
		if (expressionName != null && expressionName.startsWith("object.")) {
			expressionName = expressionName.substring(7);
		}

		// check for empty names
		if (StringUtils.isEmpty(expressionName)) {
			throw new FilterGeneratorException("Cannot add empty variable");
		}

		if (expressionName.indexOf('.') >= 0) {
			throw new FilterGeneratorException("Filtering nested objects is not supported");
		}

		return expressionName;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getDatasourceClass()
	 */
	public Class getDatasourceClass() {
		return LDAPDatasource.class;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#generateLiteralFilterPart(java.lang.Object, int)
	 */
	public FilterPart generateLiteralFilterPart(Object literal, int expectedValueType) throws FilterGeneratorException {
		if (expectedValueType == ExpressionEvaluator.OBJECTTYPE_BOOLEAN) {
			try {
				Boolean b = ExpressionEvaluator.getAsBoolean(literal);

				// generate logical true and false constraint
				return new ConstantFilterPart(this, b.booleanValue() ? "(objectClass=*)" : "(!(objectClass=*))", null);
			} catch (EvaluationException e) {
				throw new FilterGeneratorException(e);
			}
		}

		try {
			if (literal instanceof byte[]) {
				return new ConstantFilterPart(this, escapeBinary((byte[]) literal), null);
			} else {
				String stringValue = ExpressionEvaluator.getAsString(literal);

				if (stringValue != null) {
					if (expectedValueType == ExpressionEvaluator.OBJECTTYPE_WILDCARDSTRING) {
						// TODO fail if _ found in string (unsupported)
						// wildcards are: % (convert to *) and _ (also convert to *)
						stringValue = stringValue.replaceAll("%", "*").replaceAll("_", "*");
					} else {
						stringValue = escapeString(stringValue);
					}
				}

				return new ConstantFilterPart(this, stringValue, null);
			}
		} catch (EvaluationException e) {
			throw new FilterGeneratorException(e);
		}
	}

	/**
	 * Convert the given binary data according to RFC 2254, Section 4. For
	 * details, see <a href="http://www.rfc-editor.org/rfc/rfc2254.txt">RFC 2554</a>
	 * @param input binary input to escape
	 * @return escaped input
	 */
	protected final static String escapeBinary(byte[] input) {
		StringBuffer output = new StringBuffer(input.length * 3);

		for (int i = 0; i < input.length; i++) {
			int integer = input[i];

			if (integer < 0) {
				integer += 256;
			}
			String hexString = Integer.toHexString(integer);

			output.append("\\");
			if (hexString.length() == 1) {
				output.append("0");
			}
			output.append(hexString);
		}

		return output.toString();
	}

	/**
	 * Convert the given literal string such that special characters are
	 * escaped. (according to RFC 2254, Section 4). For details, see
	 * <a href="http://www.rfc-editor.org/rfc/rfc2254.txt">RFC 2554</a>
	 * @param input string to escape
	 * @return escaped string
	 */
	protected final static String escapeString(String input) {
		if (StringUtils.isEmpty(input)) {
			return input;
		}

		StringBuffer output = new StringBuffer(input.length());

		for (int i = 0; i < input.length(); ++i) {
			char c = input.charAt(i);

			switch (c) {
			case '*':
			case '(':
			case ')':
			case '\\':
			case '\0':
				String hexString = Integer.toHexString(c);

				output.append("\\");
				if (hexString.length() == 1) {
					output.append("0");
				}
				output.append(hexString);
				break;

			default:
				output.append(c);
			}
		}

		return output.toString();
	}

	/**
	 * Get the merged filter containing the ldap statement for selecting objects
	 * @param request expression request
	 * @return merged filter
	 * @throws ExpressionParserException
	 */
	public MergedFilter getSelectStatement(ExpressionQueryRequest request) throws ExpressionParserException {
		MergedFilter mergedFilter = new MergedFilter(request);

		getMainFilterPart().mergeInto(mergedFilter);
		return mergedFilter;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#allowsNullValues(java.lang.String)
	 */
	public boolean allowsNullValues(String attributeName) throws FilterGeneratorException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#getValueType(java.lang.String)
	 */
	public int getValueType(String attributeName) throws FilterGeneratorException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
