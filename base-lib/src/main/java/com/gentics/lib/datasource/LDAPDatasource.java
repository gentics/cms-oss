package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.AbstractCacheableDatasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.datasource.functions.LDAPAndOrFunction;
import com.gentics.lib.datasource.functions.LDAPBinaryDummyFunction;
import com.gentics.lib.datasource.functions.LDAPComparisonFunction;
import com.gentics.lib.datasource.functions.LDAPExtendedComparisonFunction;
import com.gentics.lib.datasource.functions.LDAPIsEmptyFunction;
import com.gentics.lib.datasource.functions.LDAPNotFunction;
import com.gentics.lib.datasource.functions.LDAPUnaryDummyFunction;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.functions.FunctionRegistry;
import com.gentics.lib.expressionparser.functions.FunctionRegistryException;
import com.gentics.lib.ldap.LDAP;
import com.gentics.lib.ldap.LDAPResultProcessor;
import com.gentics.lib.ldap.SimpleLDAPResultProcessor;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.parser.rule.CompareOperator;
import com.gentics.lib.parser.rule.Condition;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.parser.rule.functions.Function;
import com.gentics.lib.parser.rule.functions.FunctionOperand;
import com.gentics.lib.parser.rule.functions.IsEmptyFunction;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

/**
 * @author haymo
 */

public class LDAPDatasource extends AbstractCacheableDatasource {

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * scope of the ldap search
	 */
	private int scope = LDAPConnection.SCOPE_ONE;

	/**
	 * flag if scope was defined for this datasource (or if the default value is used)
	 */
	private boolean scopeWasSet = false;

	private RuleTree ruleTree;

	/**
	 * datasource filter that was generated from a set ruleTree (in case the
	 * ruleTree is not in compatibility mode)
	 */
	private DatasourceFilter datasourceFilter;

	private HandlePool handlePool;

	private String searchBase;

	// attributes to search
	private String[] queryAttrs;

	/**
	 * The maximum number of results returned by getResults()
	 */
	private int maxResults = 1000;

	/**
	 * name of the attribute that will map entry DNs
	 */
	private String dnAttributeName = null;

	/**
	 * name of the datasource parameter that contains the dn attribute name
	 */
	public final static String DNATTRIBUTENAMEPARAMETER = "dnattribute";

	/**
	 * name of the datasource parameter that contains the names of binary attributes
	 */
	public final static String BINARYATTRIBUTENAMEPARAMETER = "binaryattributes";

	/**
	 * name of the binary attributes
	 */
	private List binaryAttributes = new Vector();

	static {
		NodeLogger logger = NodeLogger.getNodeLogger(LDAPDatasource.class);
		// add all functions to generate the DatasourceFilter into the function registry
		FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

		try {
			// add basic functions
			functionRegistry.registerFunction(LDAPAndOrFunction.class.getName());
			functionRegistry.registerFunction(LDAPComparisonFunction.class.getName());
			functionRegistry.registerFunction(LDAPExtendedComparisonFunction.class.getName());
			functionRegistry.registerFunction(LDAPNotFunction.class.getName());
			functionRegistry.registerFunction(LDAPBinaryDummyFunction.class.getName());
			functionRegistry.registerFunction(LDAPUnaryDummyFunction.class.getName());
			// add named functions
			// TODO check this
			// functionRegistry.registerFunction(LDAPConcatFunction.class.getName());
			functionRegistry.registerFunction(LDAPIsEmptyFunction.class.getName());
		} catch (FunctionRegistryException e) {
			logger.error("Error while registering functions", e);
		}
	}

	public LDAPDatasource(String id, HandlePool handle, Map parameters) {
		super(id);
		this.handlePool = handle;
		// set search base dn if in parameters.
		if (parameters.containsKey("binddn")) {
			setSearchBase((String) parameters.get("binddn"));
		} else if (parameters.containsKey("searchDN")) {
			setSearchBase((String) parameters.get("searchDN"));
		} else {
			NodeLogger.getLogger(getClass()).error("LDAPDatasource::init: No parameter {searchDN} defined.");
			// setSearchBase( (String) handle.getBindDN() );
		}
        
		if (parameters.containsKey("maxResults")) {
			this.maxResults = ObjectTransformer.getInt(parameters.get("maxResults"), maxResults);
		}

		// set the scope, when in parameters
		if (parameters.containsKey("scope")) {
			setScope(parameters.get("scope").toString());
		}

		// get the configured name of the DN attribute (defaults to null)
		dnAttributeName = ObjectTransformer.getString(parameters.get(DNATTRIBUTENAMEPARAMETER), null);

		setCacheEnabled(ObjectTransformer.getBoolean(parameters.get("cache"), false));

		// get the configured names of binary attributes
		String binAttrs = ObjectTransformer.getString(parameters.get(BINARYATTRIBUTENAMEPARAMETER), null);

		if (binAttrs != null) {
			String[] binAttrsVec = binAttrs.split(",");

			for (int i = 0; i < binAttrsVec.length; i++) {
				String binAttr = binAttrsVec[i].trim();

				if (!StringUtils.isEmpty(binAttr) && !binaryAttributes.contains(binAttr)) {
					binaryAttributes.add(binAttr);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getHandlePool()
	 */
	public HandlePool getHandlePool() {
		return this.handlePool;
	}

	public void setSearchBase(String searchBase) {
		this.searchBase = searchBase;
	}

	public int getCount2() throws DatasourceNotAvailableException {
		// TODO: maybe find a better implementation here!
		// check whether the set ruleTree is in compatibility mode
		if (ruleTree != null) {
			Expression expression = ruleTree.getExpression();

			if (expression != null) {
				// the ruleTree was not in compatibility mode, so we forward
				// this call to the call using the expression (new usage)
				try {
					return getCount(datasourceFilter, null);
				} catch (DatasourceException e) {
					logger.error("Error while counting results", e);
					throw new DatasourceNotAvailableException("Error while counting results", e);
				}
			}
		}

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_LDAP_GETCOUNT, ruleTree.getRuleString());
			// get all results and simply count them
			Collection results = getResult();

			return results.size();
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_LDAP_GETCOUNT, ruleTree.getRuleString());
		}
	}

	private String translateFunctionOperand(FunctionOperand operand) {
		StringBuffer ret = new StringBuffer();
		Function curFunction = operand.getFunction();
		java.util.Vector params = operand.getParams();

		if (curFunction instanceof IsEmptyFunction) {
			// appendIsEmptyFunction((IsEmptyFunction) CurFunction, );
			// IsEmptyFunction has only one parameter
			Object o = params.get(0);

			ret.append(operand.getValue());
			ret.append("=");
			// ret.append( c.getRightOperand().getValue() );
		}
		return ret.toString();
	}

	/**
	 * see for more details:
	 * http://www.cse.ohio-state.edu/cgi-bin/rfc/rfc2254.html TODO: implement
	 * (!)contains
	 * @param c
	 * @return
	 */
	private String translateCondition(Condition c) {

		StringBuffer str = new StringBuffer(128);

		int curType = c.getOperator().getType();

		/* !MOD 20041208 DG added like query */
		// LDAP does not have a specific like operator - uses equals with * -->
		// change types
		if (curType == CompareOperator.TYPE_LIKE) {
			curType = CompareOperator.TYPE_EQ;
		} else if (curType == CompareOperator.TYPE_NOTLIKE) {
			curType = CompareOperator.TYPE_NEQ;
		}

		/* !MOD 20041208 DG END */

		switch (curType) {
		case CompareOperator.TYPE_EQ:
			str.append(c.getLeftOperand().getValue());
			str.append("=");
			str.append(c.getRightOperand().getValue());
			break;

		case CompareOperator.TYPE_NEQ:
			str.append("!(");
			str.append(c.getLeftOperand().getValue());
			str.append("=");
			str.append(c.getRightOperand().getValue());
			str.append(")");
			break;

		case CompareOperator.TYPE_GT:
			str.append("&(");
			str.append(c.getLeftOperand().getValue());
			str.append(">=");
			str.append(c.getRightOperand().getValue());
			str.append(")(!(");
			str.append(c.getLeftOperand().getValue());
			str.append("=");
			str.append(c.getRightOperand().getValue());
			str.append("))");
			break;

		case CompareOperator.TYPE_GTEQ:
			str.append(c.getLeftOperand().getValue());
			str.append(">=");
			str.append(c.getRightOperand().getValue());
			break;

		case CompareOperator.TYPE_LT:
			str.append("&(");
			str.append(c.getLeftOperand().getValue());
			str.append("<=");
			str.append(c.getRightOperand().getValue());
			str.append(")(!(");
			str.append(c.getLeftOperand().getValue());
			str.append("=");
			str.append(c.getRightOperand().getValue());
			str.append("))");
			break;

		case CompareOperator.TYPE_LTEQ:
			str.append(c.getLeftOperand().getValue());
			str.append("<=");
			str.append(c.getRightOperand().getValue());
			break;

		case CompareOperator.TYPE_CONTAINS: {
			String left = c.getLeftOperand().getValue();
			String[] right = c.getRightOperand().getValues();

			if (right.length > 0) {
				str.append("|");
				for (int i = 0; i < right.length; ++i) {
					str.append("(");
					str.append(left);
					str.append("=");
					str.append(right[i]);
					str.append(")");
				}
			}
			break;
		}

		case CompareOperator.TYPE_NOTCONTAINS: {
			String left = c.getLeftOperand().getValue();
			String[] right = c.getRightOperand().getValues();

			if (right.length > 0) {
				str.append("&");
				for (int i = 0; i < right.length; ++i) {
					str.append("(!(");
					str.append(left);
					str.append("=");
					str.append(right[i]);
					str.append("))");
				}
			}
			break;
		}

		default:
			break;
		}
		return str.toString();
	}

	private String translateOperator(LogicalOperator o) {
		StringBuffer str = new StringBuffer(8);

		switch (o.getType()) {
		case LogicalOperator.TYPE_AND:
			str.append("&");
			break;

		case LogicalOperator.TYPE_OR:
			str.append("|");
			break;

		default:
			break;
		}
		return str.toString();
	}

	private void ruleToLdapCondition(Iterator it, StringBuffer ldapSearch,
			LogicalOperator lastType, int offset) {

		while (it.hasNext()) {
			Object o = it.next();

			if (o instanceof Condition) {
				Condition c = (Condition) o;

				ldapSearch.append("(");
				ldapSearch.append(this.translateCondition(c));
				ldapSearch.append(")");

			} else if (o instanceof RuleTree) {
				RuleTree rt = (RuleTree) o;

				// first insert (
				// insert condition before ( on same offset
				ruleToLdapCondition(rt.iterator(), ldapSearch, null, ldapSearch.length());

			} else if (o instanceof LogicalOperator) {
				LogicalOperator l = (LogicalOperator) o;

				if (lastType == null) {
					lastType = l;
				} else if (lastType.getType() != l.getType()) {

					// insert condition on offset
					ldapSearch.insert(offset, this.translateOperator(lastType));
					// insert ( before condition on same offset
					ldapSearch.insert(offset, "(");
					ldapSearch.append(")");
					ruleToLdapCondition(it, ldapSearch, l, offset);
					return;
					// throw new IllegalStateException("LogicalOperator change
					// not allowed. indicates missing parenthesis");
				} else {
					lastType = l;
				}
			} /* !MOD 20041207 DG added FunctionOperands */ else if (o instanceof FunctionOperand) {
				FunctionOperand function = (FunctionOperand) o;

				ldapSearch.append("(");
				ldapSearch.append(this.translateFunctionOperand(function));
				ldapSearch.append(")");
			}

			/* !MOD 20041207 DG added FunctionOperands */
		}
		if (lastType != null) {
			// insert condition on offset
			ldapSearch.insert(offset, this.translateOperator(lastType));
			// insert ( before condition on same offset
			ldapSearch.insert(offset, "(");
			ldapSearch.append(")");
		}
	}

	public void setRuleTree(RuleTree ruleTree) {
		this.ruleTree = ruleTree;

		// reset the datasourcefilter
		datasourceFilter = null;
		if (ruleTree != null && ruleTree instanceof DefaultRuleTree) {
			Expression expression = ruleTree.getExpression();

			if (expression != null) {
				// the ruletree contained an expression, so it is not in
				// compatibility mode
				try {
					datasourceFilter = createDatasourceFilter(expression, ((DefaultRuleTree) ruleTree).getResolvablePropertyMap());
				} catch (ExpressionParserException e) {
					logger.error("Error while generating datasource filter out of expression", e);
				}
			}
		}
	}

	public Collection getResult() throws DatasourceNotAvailableException {
		return getResult(-1, -1, null, -1);
	}

	public Collection getResult(String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		return getResult(-1, -1, sortBy, sortOrder);
	}

	/**
	 * sets query attributes to search for
	 * @param names
	 */
	public void setAttributeNames(String names[]) {

		this.queryAttrs = names;

	}

	public Collection getResult(int start, int count, String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		// call the extended getResult with null as specific parameter map
		return getResult(start, count, sortBy, sortOrder, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {
		if (ruleTree != null) {
			Expression expression = ruleTree.getExpression();

			if (expression != null) {
				DatasourceRecordSet recordSet = new CompatibilityRecordSet();

				// the ruleTree was not in compatibility mode, so we forward
				// this call to the call using the expression (new usage)
				Sorting[] sorting = createSortingObjects(sortBy, sortOrder);
				Collection result = null;

				try {
					result = getResult(datasourceFilter, queryAttrs, start, count, sorting, specificParameters);
				} catch (DatasourceException e) {
					logger.error("Error while getting result", e);
				}

				// convert the collection of GenticsContentObjects into a DatasourceRecordSet
				if (result != null) {
					recordSet.addAll(result);
				}

				// return the result
				return recordSet;
			}
		}

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_LDAP_GETRESULT, ruleTree.getRuleString());
	
			// initialize the used searchBase with the default value
			String usedSearchBase = searchBase;
	
			// check an alternative searchBase (or searchDB) given in the specific
			// parameters
			if (specificParameters != null && specificParameters.containsKey("searchDN") && specificParameters.get("searchDN") != null) {
				usedSearchBase = specificParameters.get("searchDN").toString();
			}
			if (usedSearchBase == null) {
				logger.error("No searchDN set for LDAPDatasource.");
				return Collections.EMPTY_LIST;
			}
	
			SimpleLDAPResultProcessor resultProc = new SimpleLDAPResultProcessor(dnAttributeName, binaryAttributes);
			LDAPDatasourceRecordSet res;

			try {
				// RRE 2004.09.29 LDAP Failover:
				// ldapHandle will be fetched from handlePool, HandlePool can
				// contain more ldap handles
				LDAPHandle ldapHandle = (LDAPHandle) this.handlePool.getHandle();

				if (ldapHandle == null) {
					throw new DatasourceNotAvailableException("No usable handle found for ldap datasource");
				}

				// initialize the used scope with the default value
				int usedScope = scope;

				// check for an alternative scope given in the specific
				// parameters
				if (specificParameters != null && specificParameters.containsKey("scope")) {
					usedScope = parseScope((String) specificParameters.get("scope"), scope);
				} else {
					// If scope was not set for this datasource, use scope of LDAPHandle.
					if (!scopeWasSet && ldapHandle.getSearchScope() != -1) {
						usedScope = ldapHandle.getSearchScope();
					}
				}

				StringBuffer filter = new StringBuffer(500);

				ruleToLdapCondition(this.ruleTree.iterator(), filter, null, 0);
				// System.out.println("Filter: "+filter);
	
				if (logger.isDebugEnabled()) {
					logger.debug("executing LDAP query with searchBase {" + usedSearchBase + "}, filter {" + filter + "}, scope {" + usedScope + "}");
				}
	
				if (count == -1) {
					count = maxResults;
				}
				String filterString = filter.toString();
				// Try retrieving results from cache.
				DatasourceResultCacheKeyBase cacheKey = null;

				if (isCacheEnabled()) {
					// Look for results in the cache ...
					cacheKey = getCacheKey(filterString, (Object[]) null, start, count, createSortingObjects(sortBy, sortOrder), specificParameters);
					Collection results = getCachedResult(cacheKey);

					if (results != null) {
						// Got it from cache.. so return it.
						return results;
					}
				}
				LDAP.query(ldapHandle, filterString, usedSearchBase, usedScope, sortBy, sortOrder, resultProc, this.queryAttrs, start, count);
	
				res = new LDAPDatasourceRecordSet(resultProc);
                
				if (isCacheEnabled()) {
					// Cache the result for later usage.
					putCachedResult(cacheKey, res);
				}
			} catch (NodeIllegalArgumentException e) {
	
				throw new DatasourceNotAvailableException("LDAPDatasource getResult an NodeIllegalArgumentException occured : " + e.getMessage());
	
			} catch (LDAPException e) {
	
				throw new DatasourceNotAvailableException("LDAPDatasource getResult an LDAPException occured : " + e.getMessage(), e);
	
			} catch (DatasourceNotAvailableException e) {
	
				throw new DatasourceNotAvailableException("LDAPDatasource getResult an LDAPException occured : " + e.getMessage(), e);
	
			}

			return res;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_LDAP_GETRESULT, ruleTree.getRuleString());
		}
	}

	/**
	 * Creates new Sorting objects from the old sortBy, sortOrder parameters of {@link #getResult(int, int, String, int)}
	 * @param sortBy
	 * @param sortOrder
	 * @return
	 */
	private Sorting[] createSortingObjects(String sortBy, int sortOrder) {
		Sorting[] sorting = null;

		if (!StringUtils.isEmpty(sortBy)) {
			String[] sortedColumns = sortBy.split(",");

			sorting = new Sorting[sortedColumns.length];
			for (int i = 0; i < sortedColumns.length; i++) {
				sorting[i] = new Sorting(sortedColumns[i].trim(), sortOrder);
			}
		}
		return sorting;
	}

	/**
	 * set the scope for the ldap datasource
	 * @param newScope may be "BASE", "ONE" or "SUB"
	 */
	public void setScope(String newScope) {
		setScope(parseScope(newScope));
	}

	/**
	 * parse the given scope value into a scope setting
	 * @param scopeValue string, which should be "BASE", "ONE" or "SUB"
	 * @return the scope value or SCOPE_ONE if the string could not be
	 *         interpreted
	 */
	public static int parseScope(String scopeValue) {
		return parseScope(scopeValue, LDAPConnection.SCOPE_ONE);
	}

	/**
	 * parse the given scope value into a scope setting
	 * @param scopeValue string, which should be "BASE", "ONE" or "SUB"
	 * @param defaultScope the scope to be returned, when the string could not
	 *        be interpreted
	 * @return the scope value or the default if the string could not be
	 *         interpreted
	 */
	public static int parseScope(String scopeValue, int defaultScope) {
		if ("base".equalsIgnoreCase(scopeValue)) {
			return LDAPConnection.SCOPE_BASE;
		} else if ("one".equalsIgnoreCase(scopeValue)) {
			return LDAPConnection.SCOPE_ONE;
		} else if ("sub".equalsIgnoreCase(scopeValue)) {
			return LDAPConnection.SCOPE_SUB;
		} else {
			return defaultScope;
		}
	}

	/**
	 * set the scope for the ldap datasource. allowed values are
	 * {@link LDAPConnection#SCOPE_BASE},{@link LDAPConnection#SCOPE_ONE}or
	 * {@link LDAPConnection#SCOPE_SUB}.
	 * @param newScope the new scope
	 */
	public void setScope(int newScope) {
		if (newScope == LDAPConnection.SCOPE_BASE || newScope == LDAPConnection.SCOPE_ONE || newScope == LDAPConnection.SCOPE_SUB) {
			scope = newScope;
			scopeWasSet = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		return createDatasourceFilter(expression, null);
	}

	/**
	 * Create an instance of the datasource filter
	 * @param expression expression
	 * @param resolvablesMap map of resolvables to be used in the filter
	 * @return datasource filter
	 * @throws ExpressionParserException
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression, Map resolvablesMap) throws ExpressionParserException {
		if (expression instanceof EvaluableExpression) {
			LDAPDatasourceFilter filter = null;

			try {
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_LDAPATASOURCEFILTER, expression.getExpressionString());
				filter = resolvablesMap != null ? new LDAPDatasourceFilter(resolvablesMap) : new LDAPDatasourceFilter();
				filter.setExpressionString(expression.getExpressionString());
				((EvaluableExpression) expression).generateFilterPart(
						new ExpressionQueryRequest(filter, this, new PropertyResolver(new MapResolver(resolvablesMap))), filter.getMainFilterPart(),
						ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			} finally {
				RuntimeProfiler.endMark(ComponentsConstants.EXPRESSIONPARSER_LDAPATASOURCEFILTER, expression.getExpressionString());
			}
			return filter;
		} else {
			FilterGeneratorException e = new FilterGeneratorException("expression is not evaluable");

			e.setExpressionString(expression != null ? expression.toString() : "null");
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[],
	 *      java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		return getResult(filter, prefillAttributes, start, count, sortedColumns, specificParameters, false);
	}
    
	/**
	 * internal getResult method which allows caller to bypass cache (used by getCount)
	 * @see #getResult(DatasourceFilter, String[], int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters, boolean forceNoCache) throws DatasourceException {
		// check for filter compatibility
		assertCompatibleFilter(filter);
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_LDAP_GETRESULT, filter.getExpressionString());

			LDAPDatasourceFilter datasourceFilter = (LDAPDatasourceFilter) filter;
			ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, start, count, sortedColumns, -1, filter.getResolver(), specificParameters);
			MergedFilter selectFilter = datasourceFilter.getSelectStatement(request);

			// restrict maximum number of results
			if (count == -1) {
				count = maxResults;
			}
            
			String filterString = selectFilter.getStatement().toString();
            
			DatasourceResultCacheKeyBase cacheKey = null;

			// Try retrieving results from cache.
			if (!forceNoCache && isCacheEnabled()) {
				cacheKey = getCacheKey(filterString, (Object[]) null, start, count, sortedColumns, specificParameters);
				Collection results = getCachedResult(cacheKey);

				if (results != null) {
					return results;
				}
			}

			// get an ldap handle
			LDAPHandle ldapHandle = (LDAPHandle) handlePool.getHandle();

			if (ldapHandle == null) {
				throw new DatasourceNotAvailableException("No usable handle found for ldap datasource");
			}

			// prepare the search parameters
			String usedSearchBase = searchBase;
			int usedScope = scope;

			// when no scope was set for the datasource, but for the handle we use that one
			if (!scopeWasSet && ldapHandle.getSearchScope() != -1) {
				usedScope = ldapHandle.getSearchScope();
			}

			// check for eventually given specific parameters
			if (specificParameters != null) {
				// check an alternative searchBase (or searchDB) given in the
				// specific parameters
				if (specificParameters.containsKey("searchDN") && specificParameters.get("searchDN") != null) {
					usedSearchBase = specificParameters.get("searchDN").toString();
				}

				// check for an alternative scope given in the specific
				// parameters
				if (specificParameters.containsKey("scope")) {
					usedScope = parseScope((String) specificParameters.get("scope"), scope);
				}
			}

			// check whether a searchDN was given
			if (usedSearchBase == null) {
				logger.error("No searchDN set for LDAPDatasource.");
				return Collections.EMPTY_LIST;
			}

			// TODO query attributes
			LDAPResultProcessor resultProcessor = new SimpleLDAPResultProcessor(dnAttributeName, binaryAttributes);

			LDAP.query(ldapHandle, filterString, usedSearchBase, usedScope, sortedColumns, resultProcessor, new String[] {}, start, count);
			Collection results = resultProcessor.getAllLAPRows();

			if (!forceNoCache && isCacheEnabled()) {
				putCachedResult(cacheKey, results);
			}
			return results;
		} catch (Exception e) {
			throw new DatasourceException("Error while filtering results", e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_LDAP_GETRESULT, filter.getExpressionString());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		assertCompatibleFilter(filter);
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_LDAP_GETCOUNT, filter.getExpressionString());
            
			DatasourceResultCacheKeyBase cacheKey = null;

			if (isCacheEnabled()) {
				// if we have caching enabled, generate the query ..
				LDAPDatasourceFilter datasourceFilter = (LDAPDatasourceFilter) filter;
				ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, 0, -1, null, -1, filter.getResolver(), specificParameters);
				MergedFilter selectFilter = datasourceFilter.getSelectStatement(request);

				String filterString = selectFilter.getStatement().toString();

				cacheKey = getCacheKeyForCount(filterString, null, specificParameters);
				Integer count = getCachedCount(cacheKey);

				if (count != null) {
					return ((Integer) count).intValue();
				}
			}
            
			Collection result = getResult(filter, null, 0, -1, null, specificParameters, true);
			int r = result.size();
            
			if (cacheKey != null) {
				putCachedCount(cacheKey, new Integer(r));
			}
			return r;

		} catch (Exception e) {
			throw new DatasourceException("Error while filtering results", e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_LDAP_GETCOUNT, filter.getExpressionString());
		}
	}

	/**
	 * Check whether the given filter is compatible with the LDAPDatasource, if not throw an exception
	 * @param filter filter to test
	 * @throws DatasourceException
	 */
	protected static void assertCompatibleFilter(DatasourceFilter filter) throws DatasourceException {
		if (!(filter instanceof LDAPDatasourceFilter)) {
			throw new DatasourceException("Incompatible filter");
		}
	}

	/**
	 * Implementation for a DatasourceRecordSet to be returned by the old-style
	 * {@link LDAPDatasource#getResult()} methods when the new ExpressionParser
	 * is used.
	 */
	public class CompatibilityRecordSet extends Vector implements DatasourceRecordSet {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 2125799290941221093L;

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DatasourceRecordSet#getRow(int)
		 */
		public DatasourceRow getRow(int rowNum) {
			return (DatasourceRow) get(rowNum);
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DatasourceRecordSet#addRow(com.gentics.api.lib.datasource.DatasourceRow)
		 */
		public void addRow(DatasourceRow dsRow) {
			add(dsRow);
		}
	}
}
