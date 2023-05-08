/*
 * @author norbert
 * @date 27.06.2006
 * @version $Id: ExpressionParser.java,v 1.10.4.2 2011-04-07 09:57:49 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

import java.io.StringReader;
import java.util.Map;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.parser.ASTStatement;
import com.gentics.lib.expressionparser.parser.Parser;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * This is the expression parser (singleton). The expression parser parses
 * strings into reusable expressions.
 */
public final class ExpressionParser {

	/**
	 * name of the cacheregion for parsed expressions
	 */
	private final static String CACHEREGION = "gentics-parsedexpressions";

	public final static String TREATEMPTYSTRINGASNULL_PARAMETER = "portal.expressionparser.treatEmptyStringAsNull";

	private final static String EXPRESSIONPARSER_PARAMETER = "portal.expressionparser";

	private final static NodeLogger logger = NodeLogger.getNodeLogger(ExpressionParser.class);

	/**
	 * constant result object for assignments
	 */
	public final static Object ASSIGNMENT = new Object();

	/**
	 * singleton instance of the expression parser
	 */
	private static ExpressionParser instance;

	/**
	 * Flag for the expression parser
	 */
	private static boolean treatEmptyStringAsNull = false;

	/**
	 * constant for an empty expression
	 */
	private final static String EMPTYEXPRESSION = "true";

	/**
	 * Constant expression, which is always 'true'
	 */
	public static EvaluableExpression TRUE = null;

	/**
	 * Constant expression, which is always 'false'
	 */
	public static EvaluableExpression FALSE = null;

	/**
	 * static initialization of the singleton instance
	 */
	static {
		instance = new ExpressionParser();
		try {
			TRUE = (EvaluableExpression) instance.parse("true");
			FALSE = (EvaluableExpression) instance.parse("false");
		} catch (Exception e) {
			// this will never happen
			logger.error(
					"Parsing a constant expression failed. Probably the contants ExpressionParser.TRUE and ExpressionParser.FALSE will be null now", e);
		}
	}

	/**
	 * private constructor to ensure singleton.
	 */
	private ExpressionParser() {}

	/**
	 * Get the singleton instance of the expression parser
	 * @return expression parser instance
	 */
	public static ExpressionParser getInstance() {
		return instance;
	}

	/**
	 * Parse the given expression string into an Expression. This method is
	 * threadsafe.
	 * @param expressionString expression string
	 * @return expression
	 * @throws ParserException when the string cannot be parsed into an
	 *         Expression
	 */
	public Expression parse(String expressionString) throws ParserException {
		PortalCache expressionCache = null;
		Expression result = null;
		boolean fetchedFromCache = false;

		// check for empty expressions (empty expressions are considered to be "true")
		if (StringUtils.isEmpty(expressionString)) {
			expressionString = EMPTYEXPRESSION;
		}

		try {
			// get the expression cache
			expressionCache = PortalCache.getCache(CACHEREGION);
			if (expressionCache != null) {
				// and the cached expression
				result = (Expression) expressionCache.get(expressionString);
				if (result != null) {
					fetchedFromCache = true;
					if (logger.isDebugEnabled()) {
						logger.debug("Fetching parsed expression {" + expressionString + "} from cache.");
					}
				} else if (logger.isDebugEnabled()) {
					logger.debug("Parsed Expression {" + expressionString + "} not found in cache.");
				}
			} else if (logger.isDebugEnabled()) {
				logger.debug("Cache region not found, not looking for cached expression.");
			}
		} catch (PortalCacheException e) {
			logger.warn("An error occurred while trying the expression cache region {" + CACHEREGION + "}", e);
		}

		if (result == null) {
			try {
				// expression not found in the cache, so we need to parse it
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_PARSE, expressionString);

				Parser t = new Parser(new StringReader(expressionString));

				result = t.Statement();
				((ASTStatement) result).setExpression(expressionString);
			} catch (Throwable e) {
				throw new ParserException("Error while parsing expression {" + expressionString + "}", e);
			} finally {
				RuntimeProfiler.endMark(ComponentsConstants.EXPRESSIONPARSER_PARSE, expressionString);
			}
		}

		if (expressionCache != null && result != null && !fetchedFromCache) {
			// put the result into the cache
			try {
				expressionCache.put(expressionString, result);
			} catch (PortalCacheException e) {
				logger.warn("An error occurred while putting the parsed expression {" + expressionString + "} into cache region {" + CACHEREGION + "}", e);
			}
		}

		return result;
	}

	/**
	 * Sets the isTreatEmptyStringAsNull flag
	 * @param value true to treat empty strings as null
	 */
	public static void setTreatEmptyStringAsNull(boolean value) {
		treatEmptyStringAsNull = value;
	}

	/**
	 * Check whether empty strings shall be treated as null or not.
	 * @return true when empty strings are treated as null, false if not. Default is false.
	 */
	public static boolean isTreatEmptyStringAsNull() {
		return treatEmptyStringAsNull;
	}

	/**
	 * Check whether compatibility mode is on for the expression parser.
	 * (Default is true). When the compatibility mode is off, the old API
	 * (RuleTree, Rule, ...) will also use the new ExpressionParser.
	 * @return true when the compatibility mode is on, false if not
	 */
	public static boolean isCompatibilityMode() {
		String compat = getExpressionParserMode();

		if ("test".equals(compat)) {
			return false;
		}
		return !ObjectTransformer.getBoolean(compat, true);
	}

	/**
	 * Check whether compatibility test mode is on for the expression parser.
	 * (Default is false). When compatibility test mode is on, the old API will
	 * also use the new ExpressionParser but check changes to the old API
	 * functionality.
	 * @return true when the compatibility test mode is on, false if not
	 */
	public static boolean isCompatibilityTestMode() {
		return "test".equals(getExpressionParserMode());
	}

	/**
	 * Get the expressionparser mode as String
	 * @return expressionparser mode
	 */
	public static String getExpressionParserMode() {
		return getExpressionParserMode(null);
	}
    
	public static String getExpressionParserMode(ExpressionQueryRequest request) {
		if (request != null) {
			Map params = request.getParameters();
			// TODO use a constant for "expressionparsermode" .. but no idea.. where ?
			// If one is created, make sure to also modify XnlIfTag
			String mode = (String) params.get("expressionparsermode");

			if (mode != null) {
				return mode;
			}
		}
		// Object compat = PortalConfiguration.getParameters().get(EXPRESSIONPARSER_PARAMETER);
		// if (compat == null) {
		// compat = System.getProperty(EXPRESSIONPARSER_PARAMETER);
		// }
		//
		// return ObjectTransformer.getString(compat, "true");
		return "true";
	}
}
