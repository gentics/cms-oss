/*
 * @author norbert
 * @date 07.12.2007
 * @version $Id: EvalFunction.java,v 1.3 2009-12-16 16:12:06 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.filtergenerator.NestedFilterPart;

/**
 * The eval() function evaluates its operand as expression and returns the return value
 */
public class EvalFunction extends AbstractGenericUnaryFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#evaluate(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// first get the operand as string (because it should contain an
		// expression)
		String expressionString = ObjectTransformer.getString(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY), null);

		// when the expression is null or empty, the result value is the
		// "expression" (also null or empty string)
		if (StringUtils.isEmpty(expressionString)) {
			return expressionString;
		}

		try {
			// now parse the expression
			Expression expression = ExpressionParser.getInstance().parse(expressionString);

			// and evaluate the expression
			if (expression instanceof EvaluableExpression) {
				Object result = ((EvaluableExpression) expression).evaluate(new ExpressionQueryRequest(request.getResolver(), request.getParameters()),
						ExpressionEvaluator.OBJECTTYPE_ANY);

				return result;
			} else {
				throw new EvaluationException("Could not evaluate expression {" + expression.getExpressionString() + "}");
			}

		} catch (Exception e) {
			throw new EvaluationException("Error in eval() function", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// FIXME currently this is never really called when used in a datasource filter (because it is always evaluated)
		filterPart.addFilterPartGenerator(new EvalFilterPartGenerator(operand[0]));
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType(int)
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "eval";
	}

	/**
	 * Generator for the filter part
	 */
	public class EvalFilterPartGenerator implements FilterPartGenerator {

		/**
		 * generated serial version uid
		 */
		private static final long serialVersionUID = -7185578110547414652L;

		/**
		 * operand
		 */
		private EvaluableExpression operand; 

		public EvalFilterPartGenerator(EvaluableExpression operand) {
			this.operand = operand;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator#getFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
		 */
		public FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException {
			// first get the operand as string (because it should contain an
			// expression)
			String expressionString = ObjectTransformer.getString(operand.evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY), null);

			// when the expression is null or empty, the result value is the
			// "expression" (also null or empty string)
			if (StringUtils.isEmpty(expressionString)) {
				// TODO test this
				return null;
			}

			try {
				// now parse the expression
				Expression expression = ExpressionParser.getInstance().parse(expressionString);

				FilterPart filterPart = new NestedFilterPart(request.getFilter());

				// and evaluate the expression
				if (expression instanceof EvaluableExpression) {
					((EvaluableExpression) expression).generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					return filterPart;
				} else {
					throw new EvaluationException("Could not evaluate expression {" + expression.getExpressionString() + "}");
				}

			} catch (Exception e) {
				throw new EvaluationException("Error in eval() function", e);
			}
		}
	}
}
