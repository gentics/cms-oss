/*
 * @author herbert
 * @date Sep 30, 2008
 * @version $Id: ForeachFunction.java,v 1.2 2009-12-16 16:12:06 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.PropertySetter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.log.NodeLogger;

/**
 * simple foreach function which allows to iterate over Collection, Map or array.
 * 
 * @author herbert
 */
public class ForeachFunction extends AbstractGenericFunction {
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(ForeachFunction.class);
    
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		PropertyResolver resolver = request.getResolver();
		Object obj = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
		String varname = ObjectTransformer.getString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_STRING), null);
        
		// create a new property resolver which adds the new iterator variable ..
		List ret = new ArrayList();
		Map map = new HashMap();
		MapResolver res = new MapResolver(map);
		PropertyResolver newResolver = null;

		if (resolver instanceof PropertySetter) {
			newResolver = new PropertySetter(new ResolvableCombiner(res, resolver));
		} else {
			newResolver = new PropertyResolver(new ResolvableCombiner(res, resolver));
		}
		request.setResolver(newResolver);
        
		// we support maps, arrays and collections ...
		if (obj instanceof Map) {
			obj = ((Map) obj).values();
		} else if (obj instanceof Object[]) {
			obj = Arrays.asList((Object[]) obj);
		}
		if (obj instanceof Collection) {
			for (Iterator i = ((Collection) obj).iterator(); i.hasNext();) {
				map.put(varname, i.next());
				ret.add(operand[2].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
			}
		} else {
            
			if (obj != null) {
				logger.error("Parameter to foreach loop was not iterable (No Map, array or Collection) {" + obj + "} class: {" + obj.getClass().getName() + "}");
			}
            
			// i disabled this alias behavior for now .. i guess it is too error prone ..
			// // if it is no collection we assume the user wants the function to work as "alias"
			// map.put(varname, obj);
			// ret.add(operand[2].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
		}
		request.setResolver(resolver);
        
		return ret;
	}
    
	/**
	 * Combines two resolvables - the first resolvable will overload the second.
	 * @author herbert
	 */
	private static class ResolvableCombiner implements Resolvable {
        
		private Resolvable res1;
		private Resolvable res2;

		public ResolvableCombiner(Resolvable res1, Resolvable res2) {
			this.res1 = res1;
			this.res2 = res2;
		}

		public boolean canResolve() {
			return res1.canResolve() || res2.canResolve();
		}

		public Object get(String key) {
			Object o = res1.get(key);

			if (o != null) {
				return o;
			}
			return res2.get(key);
		}

		public Object getProperty(String key) {
			return get(key);
		}
        
	}

	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}

	public int getMaxParameters() {
		return 3;
	}

	public int getMinParameters() {
		return 3;
	}

	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}
    
	public String getName() {
		return "foreach";
	}

}
