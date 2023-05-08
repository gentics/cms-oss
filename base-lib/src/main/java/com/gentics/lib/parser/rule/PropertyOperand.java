/*
 * @author norbert
 * @date 01.03.2005
 * @version $Id: PropertyOperand.java,v 1.7 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.parser.rule;

import java.util.Collection;
import java.util.Iterator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;

/**
 * TODO comment this
 * @author norbert
 */
public class PropertyOperand implements Operand {

	/**
	 * The ruletree which holds and uses this propertyoperand
	 */
	private RuleTree ruleTree;

	/**
	 * resolver used for resolving
	 */
	private PropertyResolver resolver;

	/**
	 * attribute (path) that hs to be resolved
	 */
	private String attribute;

	/**
	 * prefix of the object (for which the resolver was set)
	 */
	private String objectPrefix;

	/**
	 * create instance of the operand
	 * @param objectPrefix prefix of the object
	 * @param ruleTree rule tree
	 * @param attribute attribute path
	 */
	public PropertyOperand(final String objectPrefix, final RuleTree ruleTree,
			final String attribute) {
		this.objectPrefix = objectPrefix;
		this.ruleTree = ruleTree;
		this.attribute = attribute;
	}

	/**
	 * create instance of the operand
	 * @param objectPrefix prefix of the object
	 * @param resolver resolver
	 * @param attribute attribute path
	 */
	public PropertyOperand(final String objectPrefix, final PropertyResolver resolver,
			final String attribute) {
		this.objectPrefix = objectPrefix;
		this.resolver = resolver;
		this.attribute = attribute;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#getValue()
	 */
	public String getValue() {
		String[] values = getValues();

		if (values.length > 0) {
			return values[0];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#getValues()
	 */
	public String[] getValues() {
		try {
			Object prop = resolver != null ? resolver.resolve(attribute) : (ruleTree != null ? ruleTree.resolve(objectPrefix, attribute) : null);

			// Object prop = resolver.resolve(attribute);
			// Object prop = ruleTree.resolve(objectPrefix, attribute);
			if (prop instanceof Collection) {
				Collection c = (Collection) prop;
				Iterator it = c.iterator();
				String[] ret = new String[c.size()];
				int i = 0;

				while (it.hasNext()) {
					ret[i++] = ObjectTransformer.getString(it.next(), null);
				}
				return ret;
			} else {
				return new String[] { ObjectTransformer.getString(prop, null) };
			}
		} catch (UnknownPropertyException e) {
			e.printStackTrace();
			return new String[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#setInvalidateListener(com.gentics.lib.base.InvalidationListener)
	 */
	public void setInvalidateListener(InvalidationListener listener) {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#removeListener()
	 */
	public void removeListener() {}

	public String toString() {
		return "[" + objectPrefix + ":name=" + attribute + "]";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#deepCopy(com.gentics.api.lib.rule.RuleTree)
	 */
	public Operand deepCopy(RuleTree ruleTree) {
		// TODO what to do if resolver is set ?
		return new PropertyOperand(objectPrefix, ruleTree, attribute);
	}
}
