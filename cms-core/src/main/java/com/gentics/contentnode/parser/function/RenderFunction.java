package com.gentics.contentnode.parser.function;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.lib.expressionparser.functions.AbstractGenericFunction;
import com.gentics.lib.render.Renderable;

/**
 * Render function (calls {@link Renderable#render()} on operands that implement {@link Renderable} and returns all other operands unchanged)
 */
public class RenderFunction extends AbstractGenericFunction {
	@Override
	public Object evaluate(int functionType, ExpressionQueryRequest request, EvaluableExpression[] operand, int expectedValueType)
			throws ExpressionParserException {
		Object value = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

		if (value instanceof Renderable) {
			try {
				return ((Renderable) value).render();
			} catch (NodeException e) {
				throw new EvaluationException(e);
			}
		} else {
			return value;
		}
	}

	@Override
	public String getName() {
		return "render";
	}

	@Override
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	@Override
	public int getMinParameters() {
		return 1;
	}

	@Override
	public int getMaxParameters() {
		return 1;
	}

	@Override
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
