package com.gentics.lib.expressionparser.functions;

import com.gentics.lib.datasource.functions.FilterFunction;
import com.gentics.lib.datasource.functions.MatchesFunction;
import com.gentics.lib.log.NodeLogger;

public class FunctionRegistry extends AbstractFunctionRegistry {

	/**
	 * Singleton instance
	 */
	private static FunctionRegistry instance;

	/**
	 * Get the singleton instance of the function registry
	 * 
	 * @return instance of the function registry
	 */
	public static FunctionRegistry getInstance() {
		if (instance == null) {
			instance = new FunctionRegistry();
			registerFunctions(instance);
		}
		return instance;
	}

	/**
	 * Registers all provided functions and the configured custom functions.
	 * 
	 * @param registry
	 *            The instance that should be used to register the functions
	 * @return
	 */
	protected static void registerFunctions(AbstractFunctionRegistry registry) {
		try {
			// register all provided functions here
			registry.registerFunction(GenericAndOrFunction.class.getName());
			registry.registerFunction(GenericComparisonFunction.class.getName());
			registry.registerFunction(GenericExtendedComparisonFunction.class.getName());
			registry.registerFunction(GenericCalcFunction.class.getName());
			registry.registerFunction(GenericUnaryFunction.class.getName());
			registry.registerFunction(ConcatFunction.class.getName());
			registry.registerFunction(IsEmptyFunction.class.getName());
			registry.registerFunction(IfFunction.class.getName());
			registry.registerFunction(DoFunction.class.getName());
			registry.registerFunction(FromArrayFunction.class.getName());
			registry.registerFunction(MatchesFunction.class.getName());
			registry.registerFunction(InsertFunction.class.getName());
			registry.registerFunction(EchoFunction.class.getName());
			registry.registerFunction(EvalFunction.class.getName());
			registry.registerFunction(SetFunction.class.getName());
			registry.registerFunction(ForeachFunction.class.getName());
			registry.registerFunction(GetFunction.class.getName());
			registry.registerFunction(FilterFunction.class.getName());
		} catch (FunctionRegistryException e) {
			NodeLogger.getNodeLogger(AbstractFunctionRegistry.class).error("Error while registering the provided functions", e);
		}
	}

	/**
	 * Reset the singleton
	 */
	protected static void reset() {
		instance = null;
	}
}
