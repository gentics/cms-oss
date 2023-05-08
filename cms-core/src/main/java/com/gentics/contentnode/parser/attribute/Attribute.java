/*
 * @author Stefan Hepp
 * @date 21.01.2006
 * @version $Id: Attribute.java,v 1.3 2007-08-17 10:37:24 norbert Exp $
 */
package com.gentics.contentnode.parser.attribute;

import com.gentics.contentnode.parser.expression.Expression;

/**
 * This contains infos of an attribute of a tag.
 */
public class Attribute {

	private String key;
	private String value;
	private Expression expression;

	/**
	 * Default constructor to create a new attribute.
	 *
	 * @param key the key of the attribute.
	 * @param value the value of the attribute, or null if it does not have one.
	 * @param expression an expression of this attribute, or null if it does not have one.
	 */
	public Attribute(String key, String value, Expression expression) {
		this.key = key;
		this.value = value;
		this.expression = expression;
	}

	/**
	 * Default constructor to create a new attribute.
	 *
	 * @param key the key of the attribute.
	 * @param value the value of the attribute, or null if it does not have one.
	 */
	public Attribute(String key, String value) {
		this.key = key;
		this.value = value;
		this.expression = null;
	}

	/**
	 * Get the key of this attribute.
	 * @return the key of this attribute.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the value of this attribute.
	 * @return the value of the attribute, or null if it does not have one.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Check if the attribute has a value.
	 * @return true, if the value is set, else false.
	 */
	public boolean hasValue() {
		return value != null;
	}

	/**
	 * Get the expression stored with this attribute.
	 * @return the expression or null if it does not have one.
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * Check if the attribute has an expression.
	 * @return true, if the expression is set, else false.
	 */
	public boolean hasExpression() {
		return expression != null;
	}
    
	public String toString() {
		return "Attribute [key={" + key + "},value={" + value + "},expression={" + value + "}]";
	}
}
