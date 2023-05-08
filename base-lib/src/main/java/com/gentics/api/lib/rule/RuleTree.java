/*
 * @author raoul
 * @date 27.07.2004
 * @version $Id: RuleTree.java,v 1.11 2006-08-04 15:56:08 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.rule;

import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * RuleTree is the interface for parsed rules. Parsed rule trees can be passed
 * to datasources as filters for fetching objects or can be used in rules.
 * @deprecated the old RuleTrees are deprecated, use {@link com.gentics.api.lib.expressionparser.Expression} instead.
 */
public interface RuleTree extends Cloneable {

	/**
	 * get an iterator for all elements top level elements of the ruletree
	 * @return iterator
	 * @deprecated this method will not be implemented in future implementations
	 *             and should not be used
	 */
	public Iterator iterator();

	/**
	 * get the number of top level elements in the ruletree
	 * @return number of elements
	 * @deprecated this method will not be implemented in future implementations
	 *             and should not be used
	 */
	public int size();

	/**
	 * Parse the given string into a ruletree. take care to reuse parsed
	 * ruletrees wherever possible by exchanging the resolver. Parsing Strings
	 * to RuleTrees is cost expensive.
	 * 
	 * @param ruleString
	 *            rule given as string
	 * @throws ParserException
	 *             when the given string is not a valid rule
	 */
	public void parse(String ruleString) throws ParserException;

	/**
	 * get the parsed string of the rule (given via {@link #parse(String)}).
	 * @return the rule as string
	 */
	public String getRuleString();

	/**
	 * Add a property resolver to the map of resolvers. When a resolver is added
	 * for a specific prefix, the rule may contain parts like
	 * [prefix].[property] which will be resolved with the given resolver.
	 * @param objectPrefix object prefix
	 * @param resolver property resolver
	 */
	public void addResolver(String objectPrefix, PropertyResolver resolver);

	/**
	 * Add a property resolver to the map of resolvers. When a resolver is added
	 * for a specific prefix, the rule may contain parts like
	 * [prefix].[property] which will be resolved with the given resolver.
	 * @param objectPrefix object prefix
	 * @param resolvable resolvable object
	 */
	public void addResolver(String objectPrefix, Resolvable resolvable);

	/**
	 * Add resolvable properties as map to the map of resolvers. When a resolver is added
	 * for a specific prefix, the rule may contain parts like
	 * [prefix].[property] which will be resolved with the given resolver.
	 * @param objectPrefix object prefix
	 * @param resolvableMap object properties as map
	 */
	public void addResolver(String objectPrefix, Map resolvableMap);

	/**
	 * Remove a property resolver from the map of resolvers
	 * @param objectPrefix object prefix
	 */
	public void removeResolver(String objectPrefix);

	/**
	 * Resolve the given attribute for a resolver with the given objectPrefix
	 * @param objectPrefix object prefix
	 * @param attribute name of the attribute to resolve
	 * @return value of the attribute
	 * @throws UnknownPropertyException when the property is not known
	 */
	public Object resolve(String objectPrefix, String attribute) throws UnknownPropertyException;

	// Small note to this javadoc ... i've put spaces 
	// to "&&" -> " && " because otherwise DBDoclet would produce wrong javadoc code ;) ("&amp;&")
	/**
	 * adds the given ruletree to the current ruletree, linking it with the
	 * given operator. the given ruletree remains unchanged. example
	 * (descriptive syntax): tree1 = "water == cold" tree2 = "fire == hot";
	 * tree1.concat(tree2, " &amp;&amp; "); tree1 == "water == cold &amp;&amp; fire == hot" tree2 ==
	 * "fire == hot";
	 * @param ruletree the ruletree to add to the current object.
	 * @param operator the operator to link current object and given ruletree.
	 */
	public void concat(RuleTree ruletree, LogicalOperator operator);

	/**
	 * Clone the ruletree
	 * @return a cloned ruletree
	 * @throws CloneNotSupportedException if cloning is not supported
	 */
	Object clone() throws CloneNotSupportedException;
    
	/**
	 * Creates a deep copy of the rule tree and all operands,operators,functions &amp; co
	 * @return the newly created RuleTree, or null if it couldn't be copied for any reason.
	 */
	RuleTree deepCopy();

	/**
	 * Check whether the properties evaluated in this ruletree have changed since the given timestamp
	 * @param timestamp timestamp in ms
	 * @return true when the properties have changed, false if not
	 */
	boolean hasChanged(long timestamp);

	/**
	 * Get the parsed expression of this ruletree. This will return null when
	 * the ruletree is in compatibilityMode.
	 * @return the parsed expression or null
	 */
	Expression getExpression();
}
