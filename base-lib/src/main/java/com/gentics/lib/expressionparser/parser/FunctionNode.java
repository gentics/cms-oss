/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: FunctionNode.java,v 1.12 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.parser;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.expressionparser.functions.AbstractFunctionRegistry.FunctionStore;
import com.gentics.lib.expressionparser.functions.AbstractGenericFunction;
import com.gentics.lib.expressionparser.functions.FunctionRegistry;
import com.gentics.lib.expressionparser.functions.FunctionRegistryException;

/**
 * Abstract base class for all nodes that will call a function to evaluate or
 * generate the filter part.
 */
public abstract class FunctionNode extends SimpleNode implements EvaluableExpression {

	/**
	 * for storing the "static" status of this node
	 */
	protected Boolean isStatic = null;

	/**
	 * for storing the "variable" status of this node
	 */
	protected Boolean isVariable = null;

	/**
	 * Create the functionNode
	 * @param i
	 */
	public FunctionNode(int i) {
		super(i);
	}

	/**
	 * Create the functionNode
	 * @param p parser
	 * @param i
	 */
	public FunctionNode(Parser p, int i) {
		super(p, i);
	}

	/**
	 * Get the function from the function store that is registered for the node.
	 * @param store function store
	 * @return function instance or null if no function instance has been
	 *         registered
	 */
	protected abstract Function getFunction(FunctionStore store);

	/**
	 * Get the function type for the registered function implementing this node.
	 * The function type must be one of ({@link Function#TYPE_ADD},
	 * {@link Function#TYPE_AND}, {@link Function#TYPE_CONTAINSNONE},
	 * {@link Function#TYPE_CONTAINSNONE}, {@link Function#TYPE_DIV},
	 * {@link Function#TYPE_EQUAL}, {@link Function#TYPE_GREATER},
	 * {@link Function#TYPE_GREATEROREQUAL}, {@link Function#TYPE_LIKE},
	 * {@link Function#TYPE_MINUS}, {@link Function#TYPE_MOD},
	 * {@link Function#TYPE_MULT}, {@link Function#TYPE_NAMEDFUNCTION},
	 * {@link Function#TYPE_NOT}, {@link Function#TYPE_OR},
	 * {@link Function#TYPE_PLUS}, {@link Function#TYPE_SMALLER},
	 * {@link Function#TYPE_SMALLEROREQUAL}, {@link Function#TYPE_SUB},
	 * {@link Function#TYPE_UNEQUAL}),
	 * @return function type
	 * @see Function#getTypes()
	 */
	protected abstract int getType();

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#evaluate(com.gentics.api.lib.expressionparser.ExpressionQueryRequest, int)
	 */
	public Object evaluate(ExpressionQueryRequest request, int expectedValueType) throws ExpressionParserException {
		// get the function instance for evaluation
		Function function = getFunctionInstance(ExpressionEvaluator.class);

		// evaluate the function and return the value
		return function.evaluate(getType(), request, getFunctionOperands(function), expectedValueType);
	}

	/**
	 * Get the function instance for the given class. The class must be either
	 * {@link ExpressionEvaluator} or a descendant of
	 * {@link com.gentics.api.lib.datasource.Datasource} for generation of a
	 * filter.
	 * @param clazz class for the function store
	 * @return function instance
	 * @throws EvaluationException
	 */
	protected Function getFunctionInstance(Class clazz) throws EvaluationException {
		try {
			// get the function
			Function function = getFunction(FunctionRegistry.getInstance().getFunctionStore(clazz));

			if (function != null) {
				return function;
			} else {
				throw new EvaluationException(
						"No function registered for {" + getReadableFunctionName() + "} of class {" + (clazz != null ? clazz.getName() : "null") + "}");
			}
		} catch (FunctionRegistryException e) {
			throw new EvaluationException(
					"Error while getting registered function for {" + getReadableFunctionName() + "} of class {" + (clazz != null ? clazz.getName() : "null") + "}", e);
		}
	}

	/**
	 * Get the evaluation function.
	 * @return evaluation function or null.
	 * @throws EvaluationException
	 */
	protected Function getEvaluationFunction() throws EvaluationException {
		try {
			// get the function
			return getFunction(FunctionRegistry.getInstance().getFunctionStore(ExpressionEvaluator.class));
		} catch (FunctionRegistryException e) {
			throw new EvaluationException(
					"Error while getting registered function for {" + getReadableFunctionName() + "} of class {" + ExpressionEvaluator.class.getName() + "}", e);
		}
	}

	/**
	 * Get the function in a human readable form
	 * @return function name (human readable)
	 */
	protected String getReadableFunctionName() {
		return AbstractGenericFunction.getFunctionTypeName(getType());
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#generateFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, int)
	 */
	public void generateFilterPart(ExpressionQueryRequest request, FilterPart filterPart,
			int expectedValueType) throws ExpressionParserException {
		// get the evaluation function
		Function evalFunction = getEvaluationFunction();

		if (isStatic(request.getFilter()) && evalFunction != null) {
			// the function is pure static from here on, so just evaluate
			// and add the result as literal
			filterPart.addLiteral(evalFunction.evaluate(getType(), request, getFunctionOperands(evalFunction), expectedValueType), expectedValueType);
		} else if (isVariable(request.getFilter()) || evalFunction == null) {
			// when the function contains variables, add the filter part

			// get the function instance for the filter
			Function function = getFunctionInstance(request.getFilter().getDatasourceClass());

			function.generateFilterPart(getType(), request, filterPart, getFunctionOperands(function), expectedValueType);
		} else {
			// the function is not static (contains names that need to be
			// resolved) but does not contain any variables.
			filterPart.addFunctionToEvaluate(evalFunction, getType(), getFunctionOperands(evalFunction), expectedValueType);
		}
	}

	/**
	 * Get the function operands.
	 * @param function function registered to operate this node
	 * @return array of function operands
	 * @throws EvaluationException
	 */
	protected EvaluableExpression[] getFunctionOperands(Function function) throws EvaluationException {
		// get the required minimum and maximum number of parameters
		int minParameters = function.getMinParameters();
		int maxParameters = function.getMaxParameters();

		if (children == null) {
			if (minParameters > 0) {
				throw new EvaluationException(
						"Function " + function.getClass().getName() + " (registered for {" + getReadableFunctionName() + "}) expected at least " + minParameters
						+ " operands but none where given!");
			}
			return new EvaluableExpression[0];
		}

		// check the correct number of parameters
		if (children.length < minParameters) {
			throw new EvaluationException(
					"Function " + function.getClass().getName() + " (registered for {" + getReadableFunctionName() + "}) expected at least " + minParameters
					+ " operands but only " + children.length + " where given!");
		}
		if (maxParameters >= 0 && children != null && children.length > maxParameters) {
			throw new EvaluationException(
					"Function " + function.getClass().getName() + " (registered for {" + getReadableFunctionName() + "}) expected at most " + maxParameters
					+ " operands but " + children.length + " where given!");
		}

		EvaluableExpression[] operands = new EvaluableExpression[children != null ? children.length : 0];

		if (children != null) {
			for (int i = 0; i < children.length; ++i) {
				if (!(children[i] instanceof EvaluableExpression)) {
					// this will rarely happen (would mean that the expression node
					// does not implement the interface)
					throw new EvaluationException(
							"Operand " + i + " (registered for {" + getReadableFunctionName() + "}) in call to function " + function.getClass().getName()
							+ " is illegal!");
				} else {
					operands[i] = (EvaluableExpression) children[i];
				}
			}
		}

		return operands;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.EvaluableExpression#isStatic()
	 */
	public boolean isStatic(DatasourceFilter filter) throws ExpressionParserException {
		if (isStatic == null) {
			try {
				Function evalFunction = getFunctionInstance(ExpressionEvaluator.class);
				boolean tempIsStatic = evalFunction.supportStaticEvaluation();

				if (children != null) {
					for (int i = 0; i < children.length && tempIsStatic; ++i) {
						if (children[i] instanceof EvaluableExpression) {
							tempIsStatic &= ((EvaluableExpression) children[i]).isStatic(filter);
						}
					}
				}
				isStatic = Boolean.valueOf(tempIsStatic);
			} catch (EvaluationException e) {
				// no eval function registered, check whether the filter
				// function exists, if not we throw an exception
				getFunctionInstance(filter.getDatasourceClass());
				// filter function exists, but is not static
				isStatic = Boolean.FALSE;
			}
		}
		return isStatic.booleanValue();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.EvaluableExpression#isVariable()
	 */
	public boolean isVariable(DatasourceFilter filter) throws ExpressionParserException {
		if (isVariable == null) {
			// when no function is registered for the datasource class, this will fail
			boolean tempIsVariable = false;

			try {
				getFunctionInstance(filter.getDatasourceClass());
				if (children != null) {
					for (int i = 0; i < children.length && !tempIsVariable; ++i) {
						if (children[i] instanceof EvaluableExpression) {
							tempIsVariable |= ((EvaluableExpression) children[i]).isVariable(filter);
						}
					}
				}
			} catch (EvaluationException e) {
				tempIsVariable = false;
			}

			isVariable = Boolean.valueOf(tempIsVariable);
		}

		return isVariable.booleanValue();
	}

	/**
	 * Check whether the given function supports evaluation
	 * @param function function to check
	 * @return true when the function supports evaluation, false if not
	 */
	public static boolean functionSupportsEvaluation(Function function) {
		Class[] supportedClasses = function.getSupportedDatasourceClasses();

		if (supportedClasses != null) {
			for (int i = 0; i < supportedClasses.length; i++) {
				if (ExpressionEvaluator.class.isAssignableFrom(supportedClasses[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.Expression#getExpressionString()
	 */
	public String getExpressionString() {
		if (parent instanceof Expression) {
			return ((Expression) parent).getExpressionString();
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#allowsNullValues(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public boolean allowsNullValues(DatasourceFilter filter) throws ExpressionParserException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#getExpectedValueType(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public int getExpectedValueType(DatasourceFilter filter) throws ExpressionParserException {
		Function function = null;

		if (isStatic(filter)) {
			function = getFunctionInstance(ExpressionEvaluator.class);
		} else if (isVariable(filter)) {
			function = getFunctionInstance(filter.getDatasourceClass());
		}

		if (function != null) {
			return function.getExpectedValueType(getType());
		} else {
			return ExpressionEvaluator.OBJECTTYPE_ANY;
		}
	}
}
