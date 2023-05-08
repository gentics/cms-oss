/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: AbstractGenericFunction.java,v 1.7 2006-08-23 15:32:18 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.PropertyResolver;

/**
 * Abstract base class for all generic functions. Provides helper methods and
 * basic implementation.
 */
public abstract class AbstractGenericFunction implements Function {

	/**
	 * constant for the function support provided by Portal.Node (only
	 * evaluation)
	 */
	final public static Class[] EVALUATIONCLASS = new Class[] { ExpressionEvaluator.class};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		// this is not a named function (but subclasses may be and should
		// overwrite this).
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		// all functions provide support for the same classes
		return AbstractGenericFunction.EVALUATIONCLASS;
	}

	/**
	 * Convenience method to throw an exception "Unknown type ... found"
	 * @param type type which was not found
	 * @throws EvaluationException since that's what this method does
	 */
	protected void unknownTypeFound(int type) throws EvaluationException {
		throw new EvaluationException("Unknown type {" + type + "} found");
	}

	/**
	 * Convert the given function type into a human readable form (for logging
	 * messages).
	 * @param type function type
	 * @return human readable form of the function type
	 */
	public final static String getFunctionTypeName(int type) {
		// normalize the type
		if (type < 0 || type > (FUNCTIONTYPE_NAMES.length - 1)) {
			type = FUNCTIONTYPE_NAMES.length - 1;
		}
		return FUNCTIONTYPE_NAMES[type];
	}

	/**
	 * Check whether the generated value type is compatible with the expected
	 * value type. The given types must be each one of ({@link ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link ExpressionEvaluator#OBJECTTYPE_STRING}).
	 * @param generatedValueType generated value type
	 * @param expectedValueType expected value type
	 * @throws EvaluationException when the value types are not compatible
	 */
	protected final static void assertCompatibleValueType(int generatedValueType,
			int expectedValueType) throws EvaluationException {
		if (expectedValueType != ExpressionEvaluator.OBJECTTYPE_ANY && generatedValueType != expectedValueType) {
			throw new EvaluationException(
					"Function generates value of type {" + ExpressionEvaluator.getValuetypeName(generatedValueType) + "} but caller expected value of type {"
					+ ExpressionEvaluator.getValuetypeName(expectedValueType) + "}");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#supportStaticEvaluation()
	 */
	public boolean supportStaticEvaluation() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		throw new EvaluationException("Function {" + getClass().getName() + "} does not implement evaluation");
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.filtergenerator.FilterPart,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		throw new FilterGeneratorException("Function {" + getClass().getName() + "} does not implement filter generation");
	}
}
