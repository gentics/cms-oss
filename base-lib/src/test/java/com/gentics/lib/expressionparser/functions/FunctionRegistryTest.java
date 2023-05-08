package com.gentics.lib.expressionparser.functions;

import static org.junit.Assert.assertNotNull;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.lib.expressionparser.functions.AbstractFunctionRegistry.FunctionStore;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class FunctionRegistryTest {

	@Before
	public void setUp() {
		FunctionRegistry.reset();
	}

	@Test
	public void testRegisteredFunction() throws FunctionRegistryException {
		FunctionStore store = FunctionRegistry.getInstance().getFunctionStore(ExpressionEvaluator.class);
		assertNotNull(store);
	}

	@Test
	public void testRegisterDummyFunction() throws FunctionRegistryException {
		FunctionRegistry.getInstance().registerFunction(DummyFunction.class.getCanonicalName());
		FunctionRegistry.getInstance().checkFunctionRegistration();
	}

	@Test
	public void testNonRegisteredFunction() throws FunctionRegistryException {
		FunctionStore store = FunctionRegistry.getInstance().getFunctionStore(ExpressionEvaluator.class);
		assertNotNull(store);
	}
}

class DummyFunction extends AbstractGenericFunction {

	@Override
	public int[] getTypes() {
		int[] types = new int[2];
		types[0] = 10007;
		types[1] = 10002;
		return types;
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public int getMaxParameters() {
		return 0;
	}

	@Override
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return 0;
	}

}
