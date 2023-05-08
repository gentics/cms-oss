package com.gentics.lib.datasource.mccr;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.datasource.AbstractDatasource;
import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.functions.SubRuleFunction;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper.AttributeCache;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceAndOrFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceCalcFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceComparisonFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceConcatFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceExtendedComparisonFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceFilter;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceIsEmptyFunction;
import com.gentics.lib.datasource.mccr.filter.MCCRDatasourceUnaryFunction;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.functions.FunctionRegistry;
import com.gentics.lib.expressionparser.functions.FunctionRegistryException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.parser.rule.DefaultRuleTree;

/**
 * Implementation of a multichannelling aware datasource
 */
@SuppressWarnings("deprecation")
public class MCCRDatasource extends AbstractDatasource implements MultichannellingDatasource {

	/**
	 * parameter name for attribute path
	 */
	public final static String ATTRIBUTE_PATH = "attribute.path";

	/**
	 * parameter name for enabling cache
	 */
	public final static String CACHE = "cache";

	/**
	 * parameter name for enabling syncchecking of the cache
	 */
	public final static String CACHE_SYNCCHECKING = "cache.syncchecking";

	/**
	 * parameter name for enabling differential syncchecking
	 */
	public final static String CACHE_SYNCCHECKING_DIFFERENTIAL = "cache.syncchecking.differential";

	/**
	 * parameter name for cache warming initialization
	 */
	public final static String CACHE_WARMING_ON_INIT = "cache.warming.onInit";

	/**
	 * parameter name for cache warming filter
	 */
	public final static String CACHE_WARMING_FILTER = "cache.warming.filter";
    
	/**
	 * parameter name for cache warming attributes
	 */
	public final static String CACHE_WARMING_ATTRIBUTES = "cache.warming.attributes";

	/**
	 * logger object
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCCRDatasource.class);

	/**
	 * Stores the last update timestamp for each datasource id.
	 * key: datasource id, value: Long[] { timestamp of when it was last refreshed, update timestamp}
	 */
	protected static Map<String, Long[]> lastUpdateTimestamp = new HashMap<String, Long[]>();

	/**
	 * Handle pool
	 */
	protected HandlePool pool;

	/**
	 * Init parameters
	 */
	protected Map<String, String> parameters;

	/**
	 * datasource filter that was generated from a set ruleTree (in case the
	 * ruleTree is not in compatibility mode)
	 */
	private DatasourceFilter datasourceFilter;

	/**
	 * Names of the attributes to be prefetched
	 */
	private String[] prefetchedAttributes;

	/**
	 * Currently selected channels as map of integer (root node id) -> channel paths (first entry is always the root node, last entry is the selected channel)
	 */
	protected ThreadLocal<Map<Integer, List<DatasourceChannel>>> selectedChannels = new ThreadLocal<Map<Integer, List<DatasourceChannel>>>();

	/**
	 * Channel tree. This will be read whenever a channel is set.
	 */
	protected ThreadLocal<ChannelTree> channelStructure = new ThreadLocal<ChannelTree>();

	/**
	 * path where filesystem attributes should be stored
	 */
	private String attributePath = null;

	/**
	 * Flag to mark cacheable datasources
	 */
	private boolean cacheEnabled = false;

	/**
	 * when special cache settings are defined for attributes, this map is
	 * filled: values are the attribute names, keys are either Boolean.FALSE (if
	 * attribute shall not be cached), or the name of the custom cache region as
	 * String
	 */
	private Map<String, AttributeCache> attributeCacheSettings = null;

	/**
	 * Flag if sync checking was enabled.
	 */
	private boolean cacheSyncChecking = false;

	/**
	 * Flag if the sync checking shall be done differential or not When
	 * differential sync checking is enabled, the background job will determine,
	 * which objects really changed since the last change, and will only clear
	 * those objects (and attributes) from the cache
	 */
	private boolean differentialSyncChecking = true;

	/**
	 * flag to check if cache warming is enabled at startup
	 */
	private boolean cacheWarmingOnInit = true;

	/**
	 * filter used to determine which objects are cached by default
	 */
	private String cacheWarmingFilter = "true";

	/**
	 * Variant of the cache warming filter that restricts by updatetimestamp
	 */
	private String cacheWarmingFilterUpdate;

	/**
	 * list of attributes that should be cached
	 */
	private String cacheWarmingAttributes[] = null;

	/**
	 * Whether attributes shall be prefetched, default: true
	 */
	private boolean prefetchAttributes = true;

	/**
	 * Threshold for # of total attributes to prefetch (if less attributes, try
	 * fetching from cache first), default: 1000
	 */
	private int prefetchAttributesThreshold = 1000;

	/**
	 * Threshold for # of cache misses. If less than
	 * {@link #prefetchAttributesThreshold} are to be prefetched, but more than
	 * {@link #prefetchAttributesCacheMissThreshold} cache misses occur, the
	 * attributes are prefetched en block. default: 100
	 */
	private int prefetchAttributesCacheMissThreshold = 100;

	/**
	 * Threshold for # of cache misses in % of the number of attributes to be
	 * prefetched. If less than {@link #prefetchAttributesThreshold} are to be
	 * prefetched, but more than {@link #prefetchAttributesCacheMissThreshold}
	 * or {@link #prefetchAttributesCacheMissThresholdPerc} (whichever gives the
	 * smaller number) cache misses occur, the attributes are prefetched en
	 * block. Default: 20%
	 */
	private int prefetchAttributesCacheMissThresholdPerc = 20;

	static {
		// add all functions to generate the DatasourceFilter into the function registry
		FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

		try {
			// add basic functions
			functionRegistry.registerFunction(MCCRDatasourceAndOrFunction.class.getName());
			functionRegistry.registerFunction(MCCRDatasourceCalcFunction.class.getName());
			functionRegistry.registerFunction(MCCRDatasourceComparisonFunction.class.getName());
			functionRegistry.registerFunction(MCCRDatasourceExtendedComparisonFunction.class.getName()); // TODO migrate
			functionRegistry.registerFunction(MCCRDatasourceUnaryFunction.class.getName());
			// add named functions
			functionRegistry.registerFunction(MCCRDatasourceConcatFunction.class.getName());
			functionRegistry.registerFunction(MCCRDatasourceIsEmptyFunction.class.getName());
			functionRegistry.registerFunction(SubRuleFunction.class.getName());
		} catch (FunctionRegistryException e) {
			logger.error("Error while registering functions", e);
		}
	}

	/**
	 * Create an instance
	 * @param id datasource id
	 * @param pool handle pool
	 * @param parameters init parameters
	 */
	public MCCRDatasource(String id, HandlePool pool, Map<String, String> parameters) {
		super(id);
		this.pool = pool;
		this.parameters = parameters;

		if (parameters != null) {
			// load parameter for file system
			attributePath = ObjectTransformer.getString(parameters.get(ATTRIBUTE_PATH), null);
			// caching
			cacheEnabled = ObjectTransformer.getBoolean(parameters.get(CACHE), cacheEnabled);

			if (cacheEnabled) {
				attributeCacheSettings = MCCRCacheHelper.getCustomCacheSettings(parameters);
			}

			// syncchecking
			cacheSyncChecking = ObjectTransformer.getBoolean(parameters.get(CACHE_SYNCCHECKING), cacheSyncChecking);
			// differential sync checking
			differentialSyncChecking = ObjectTransformer.getBoolean(parameters.get(CACHE_SYNCCHECKING_DIFFERENTIAL), differentialSyncChecking);

			// load parameters for cache warming
			cacheWarmingOnInit = ObjectTransformer.getBoolean(parameters.get(CACHE_WARMING_ON_INIT), cacheWarmingOnInit);
			cacheWarmingFilter = "(" + ObjectTransformer.getString(parameters.get(CACHE_WARMING_FILTER), cacheWarmingFilter) + ") AND object.channel_id == data.channelId";
			cacheWarmingFilterUpdate = "(" + cacheWarmingFilter + ") AND object.updatetimestamp > data.timestamp"; 
			String warmingAttributesList = ObjectTransformer.getString(parameters.get(CACHE_WARMING_ATTRIBUTES), null);

			if (!StringUtils.isEmpty(warmingAttributesList)) {
				cacheWarmingAttributes = warmingAttributesList.split(",");
				// trim the attribute names
				for (int i = 0; i < cacheWarmingAttributes.length; i++) {
					cacheWarmingAttributes[i] = cacheWarmingAttributes[i].trim();
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug(CACHE_WARMING_ON_INIT + " is " + (cacheWarmingOnInit ? "enabled" : "disabled"));
				logger.debug(CACHE_WARMING_FILTER + ": " + cacheWarmingFilter);
				logger.debug(CACHE_WARMING_ATTRIBUTES + ": " + warmingAttributesList);
			}

			// load parameters for attribute prefetching
			prefetchAttributes = ObjectTransformer.getBoolean(parameters.get("prefetchAttributes"), prefetchAttributes);
			prefetchAttributesThreshold = ObjectTransformer.getInt(parameters.get("prefetchAttribute.threshold"), prefetchAttributesThreshold);
			prefetchAttributesCacheMissThreshold = ObjectTransformer.getInt(parameters.get("prefetchAttribute.cacheMissThreshold"),
					prefetchAttributesCacheMissThreshold);
			prefetchAttributesCacheMissThresholdPerc = ObjectTransformer.getInt(parameters.get("prefetchAttribute.cacheMissThresholdPerc"),
					prefetchAttributesCacheMissThresholdPerc);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#setRuleTree(com.gentics.api.lib.rule.RuleTree)
	 */
	public void setRuleTree(RuleTree ruleTree) {
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

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#setAttributeNames(java.lang.String[])
	 */
	public void setAttributeNames(String[] names) {
		prefetchedAttributes = names;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(int, int, java.lang.String, int, java.util.Map)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection getResult(int start, int count, String sortBy, int sortOrder, Map specificParameters) throws DatasourceNotAvailableException {
		Sorting[] sorting = null;

		if (!StringUtils.isEmpty(sortBy)) {
			String[] sortedColumns = sortBy.split(",");

			sorting = new Sorting[sortedColumns.length];
			for (int i = 0; i < sortedColumns.length; i++) {
				sorting[i] = new Sorting(sortedColumns[i].trim(), sortOrder);
			}
		}

		try {
			return getResult(datasourceFilter, prefetchedAttributes, start, count, sorting, specificParameters);
		} catch (DatasourceException e) {
			throw new DatasourceNotAvailableException("Error while getting result", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount2()
	 */
	public int getCount2() throws DatasourceNotAvailableException {
		try {
			return getCount(datasourceFilter, null);
		} catch (DatasourceException e) {
			throw new DatasourceNotAvailableException("Error while getting result count", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getHandlePool()
	 */
	public HandlePool getHandlePool() {
		return pool;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.AbstractDatasource#hasChanged(long)
	 */
	public boolean hasChanged(long timestamp) {
		if (cacheEnabled && cacheSyncChecking) {
			// Only if sync checking was enabled check last update timestamp
			long lastUpdate = getLastUpdate(false);

			if (lastUpdate == -1) {
				logger.error("Unable to determine last update timestamp, altough sync checking was enabled.");
				return true;
			}
			// timestamps are milliseconds, while lastUpdate are seconds..
			return lastUpdate * 1000 > timestamp;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.api.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		return createDatasourceFilter(expression, null);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter, java.lang.String[], int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], java.util.Map)
	 */
	public Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes, int start, int count, Sorting[] sortedColumns,
			Map<String, Object> specificParameters) throws DatasourceException {
		return getResult(Resolvable.class, filter, prefillAttributes, start, count, sortedColumns, specificParameters);
	}

	/**
	 * Get the result as list of objects of the given class. When the datasource is incapable of creating objects of the given class, an exception is thrown.
	 * @param clazz class
	 * @param filter datasource filter
	 * @param prefillAttributes attributes to prefill
	 * @param start start index
	 * @param count maximum number of returned objects
	 * @param sortedColumns sorted columns
	 * @param specificParameters specific parameters
	 * @return list of objects
	 * @throws DatasourceException
	 */
	public <T extends Resolvable> List<T> getResult(Class<T> clazz, DatasourceFilter filter, String[] prefillAttributes, int start, int count, Sorting[] sortedColumns,
			Map<String, Object> specificParameters) throws DatasourceException {
		if (!clazz.isAssignableFrom(MCCRObject.class)) {
			throw new DatasourceException("This datasource cannot generate objects of " + clazz);
		}

		// get the list of current channels
		List<DatasourceChannel> currentChannels = getChannels();

		if (ObjectTransformer.isEmpty(currentChannels)) {
			// no channels, no objects
			return Collections.emptyList();
		}

		// check the filter for compatibility
		MCCRDatasourceFilter datasourceFilter = getAsMCCRDatasourceFilter(filter);

		// the metaResult holds simply the data from the database as maps
		SimpleResultProcessor metaResult = null;

		// this will be the final result as collection
		List<T> result = null;

		final int lowerLimit = start;
		final int upperLimit = (start + count);

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, datasourceFilter.getExpressionString());

			ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, start, count, sortedColumns, -1, filter.getResolver(), specificParameters);

			MergedFilter fullStatement = datasourceFilter.getSelectStatement(request);
			String sqlStatement = fullStatement.getStatement().toString();

			// it might be, that the sql statement still contains a placeholder for a list of currently selected channels
			Map<String, String> placeHolders = new HashMap<String, String>(1);

			placeHolders.put("channelIds", StringUtils.repeat("?", currentChannels.size(), ","));
			sqlStatement = StringUtils.resolveMapData(sqlStatement, placeHolders);

			List<?> params = fullStatement.getParams();
			Object[] sqlParams = (Object[]) params.toArray(new Object[params.size()]);

			boolean putResultIntoCache = false;
			boolean resultFetchedFromCache = false;
			String cacheKey = null;

			// check whether result caching is activated
			if (isCacheEnabled()) {
				cacheKey = MCCRCacheHelper.getResultsCacheKey(this, sqlStatement, sqlParams, start, count);
				metaResult = MCCRCacheHelper.getResult(this, cacheKey);
				if (metaResult != null) {
					resultFetchedFromCache = true;
				} else {
					putResultIntoCache = true;
				}
			}

			// when no result was fetched from the cache, perform the sql
			// statement now
			if (!resultFetchedFromCache) {
				metaResult = new SimpleResultProcessor();
				DBHandle dbHandle = getHandle();

				if (dbHandle.getSupportedLimitClause() == DBHandle.SUPPORTED_LIMIT_CLAUSE_LIMIT) {
					StringBuffer sqlStatementBuffer = new StringBuffer(sqlStatement).append(" LIMIT ").append(start < 0 ? 0 : start).append(',');

					if (count == -1) {
						// I have to admit, this looks strange.. but it makes sense ..
						// This number was taken from the mysql manual !!!
						// (although .. without any explanation .. however)
						sqlStatementBuffer.append("18446744073709551615");
					} else {
						sqlStatementBuffer.append(count);
					}
					sqlStatement = sqlStatementBuffer.toString();
					metaResult.setLimit(0, count);
				} else {
					// I have no idea how to handle oracle's ROWNUM here ...
					// so it is exactly handled as if there would be no LIMIT support.
					metaResult.setLimit(lowerLimit, upperLimit);
				}
				// output some logger message
				if (logger.isDebugEnabled()) {
					StringBuffer debugMessage = new StringBuffer();

					debugMessage.append("sql statement: {").append(sqlStatement).append("} with params: {");
					if (sqlParams != null) {
						for (int i = 0; i < sqlParams.length; i++) {
							if (i > 0) {
								debugMessage.append(",");
							}
							debugMessage.append(sqlParams[i]);
						}
					}
					debugMessage.append("}");
					logger.debug(debugMessage.toString());
				}

				getDBResult(dbHandle, sqlStatement, sqlParams, metaResult);

				// when no result was found in the cache, we put the current result into the cache now
				if (putResultIntoCache) {
					MCCRCacheHelper.put(this, cacheKey, metaResult);
				}
			}

			// convert the metaResult in the final results
			result = new ArrayList<T>(metaResult.size());
			for (SimpleResultRow row : metaResult) {
				// create objects from the row, not only the contentid

				MCCRObject object = new MCCRObject(this, row);

				MCCRCacheHelper.put(object);
				result.add((T) object);
			}

			// prefill
			if (prefetchAttributes && !ObjectTransformer.isEmpty(prefillAttributes)) {
				MCCRHelper.batchLoadAttributes(this, (List<MCCRObject>) result, Arrays.asList(prefillAttributes), true);
			}

			// do postprocessing
			datasourceFilter.doPostProcessing((List<Resolvable>) result, request);
		} catch (Exception e) {
			throw new DatasourceException(e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, datasourceFilter.getExpressionString());
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map<String, Object> specificParameters) throws DatasourceException {
		// check the filter for compatibility
		MCCRDatasourceFilter datasourceFilter = getAsMCCRDatasourceFilter(filter);

		// get the list of current channels
		List<DatasourceChannel> currentChannels = getChannels();

		if (ObjectTransformer.isEmpty(currentChannels)) {
			// no channels, no objects
			return 0;
		}

		// when the datasource filter contains post processors, we need toe count by getting the result first
		if (datasourceFilter.hasPostProcessors()) {
			return getResult(datasourceFilter, null, 0, -1, null, specificParameters).size();
		}

		// prepare the count result
		int count = 0;

		try {
			// set the begin mark for the profiler
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETCOUNT, filter.getExpressionString());

			ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, -1, -1, null, -1, filter.getResolver(), specificParameters);

			MergedFilter fullStatement = datasourceFilter.getCountStatement(request);
			String sqlStatement = fullStatement.getStatement().toString();

			// it might be, that the sql statement still contains a placeholder for a list of currently selected channels
			Map<String, String> placeHolders = new HashMap<String, String>(1);

			placeHolders.put("channelIds", StringUtils.repeat("?", currentChannels.size(), ","));
			sqlStatement = StringUtils.resolveMapData(sqlStatement, placeHolders);

			List<?> params = fullStatement.getParams();
			Object[] sqlParams = (Object[]) params.toArray(new Object[params.size()]);

			// prepare for caching
			String cacheKey = null;
			boolean resultFetchedFromCache = false;
			boolean putResultIntoCache = false;

			// check whether result caching is activated
			if (isCacheEnabled()) {
				cacheKey = MCCRCacheHelper.getResultsCacheKey(this, sqlStatement, sqlParams, -1, -1);
				Integer cachedResult = MCCRCacheHelper.getCount(this, cacheKey);

				if (cachedResult != null) {
					// we found a cached result, now use it
					count = cachedResult.intValue();
					resultFetchedFromCache = true;
				} else {
					// no cached result found, do the query and cache the result
					putResultIntoCache = true;
				}
			}

			if (!resultFetchedFromCache) {
				// output some logging message
				if (logger.isDebugEnabled()) {
					StringBuffer debugMessage = new StringBuffer();

					debugMessage.append("sql statement: {").append(sqlStatement).append("} with params: {");
					if (sqlParams != null) {
						for (int i = 0; i < sqlParams.length; i++) {
							if (i > 0) {
								debugMessage.append(",");
							}
							debugMessage.append(sqlParams[i]);
						}
					}
					debugMessage.append("}");
					logger.debug(debugMessage.toString());
				}

				// preform the sql query
				SimpleResultProcessor srp = new SimpleResultProcessor();

				DB.query(getHandle(), sqlStatement, sqlParams, srp);
				count = srp.getRow(1).getInt("c");

				if (putResultIntoCache) {
					// we have to put the result into the cache
					MCCRCacheHelper.put(this, cacheKey, count);
				}
			}

		} catch (Exception e) {
			throw new DatasourceException(e);
		} finally {
			// end mark for the profiler
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_GETCOUNT, filter.getExpressionString());
		}

		return count;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.AbstractDatasource#isValidAttribute(java.lang.String)
	 */
	public boolean isValidAttribute(String attributeName) throws DatasourceException {
		return MCCRHelper.getAttributeTypeMap(this).containsKey(attributeName);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.MultichannellingDatasource#setChannel(int)
	 */
	public DatasourceChannel setChannel(int channelId) throws DatasourceException {
		// check whether the channel is already set
		List<DatasourceChannel> channels = getChannels();
		for (DatasourceChannel channel : channels) {
			if (channel.getId() == channelId) {
				return channel;
			}
		}

		// get the channel selection map (forcing the renewal of the channel structure)
		Map<Integer, List<DatasourceChannel>> channelSelectionMap = getChannelSelectionMap(true);

		// find the channel with given id
		DatasourceChannel newChannel = MCCRHelper.getChannel(channelStructure.get(), channelId);
		// find the channel path
		List<DatasourceChannel> channelPath = MCCRHelper.getChannelPath(this, channelId);

		// set the selected channel (the first element in the channel path is the root node)
		channelSelectionMap.put(channelPath.get(0).getId(), channelPath);
		return newChannel;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.MultichannellingDatasource#getChannels()
	 */
	public List<DatasourceChannel> getChannels() throws DatasourceException {
		Map<Integer, List<DatasourceChannel>> channelSelectionMap = getChannelSelectionMap(false);
		List<DatasourceChannel> selection = new ArrayList<DatasourceChannel>(channelSelectionMap.size());

		for (List<DatasourceChannel> path : channelSelectionMap.values()) {
			if (!path.isEmpty()) {
				selection.add(path.get(path.size() - 1));
			}
		}
		return selection;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.MultichannellingDatasource#getChannelPaths()
	 */
	public List<List<DatasourceChannel>> getChannelPaths() throws DatasourceException {
		return new ArrayList<List<DatasourceChannel>>(getChannelSelectionMap(false).values());
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.MultichannellingDatasource#getChannelStructure()
	 */
	public ChannelTree getChannelStructure() throws DatasourceException {
		return MCCRHelper.getChannelStructure(this);
	}

	/**
	 * Get the threadlocal channel selection map. If none was created before, we create one, set it as threadlocal and initialize it with all currently existing channel structures.
	 * @param forceRenewStructure true if the channel structure shall be renewed, false if not
	 * @return channel selection map
	 * @throws DatasourceException
	 */
	protected Map<Integer, List<DatasourceChannel>> getChannelSelectionMap(boolean forceRenewStructure) throws DatasourceException {
		Map<Integer, List<DatasourceChannel>> channelSelectionMap = selectedChannels.get();

		// when the map has not yet been set, we create a new map now
		if (channelSelectionMap == null) {
			// create the map and set as threadlocal
			channelSelectionMap = new HashMap<Integer, List<DatasourceChannel>>();
			selectedChannels.set(channelSelectionMap);

			// get the whole channel structure
			channelStructure.set(getChannelStructure());
			ChannelTree channelTree = channelStructure.get();

			// for every direct child of the (dummy) root node, we make an entry in the channel selection map
			// this will select the root nodes for every structure
			for (ChannelTreeNode treeNode : channelTree.getChildren()) {
				DatasourceChannel rootNode = treeNode.getChannel();

				channelSelectionMap.put(rootNode.getId(), Arrays.asList(rootNode));
			}
		} else if (forceRenewStructure) {
			// the map already exists, but renewing the channel structure was requested

			channelStructure.set(getChannelStructure());
			ChannelTree channelTree = channelStructure.get();
			List<Integer> foundIds = new ArrayList<Integer>();

			// iterate over all root nodes
			for (ChannelTreeNode treeNode : channelTree.getChildren()) {
				DatasourceChannel rootNode = treeNode.getChannel();

				// if the root node is not yet present in the channel selection map, make an entry (selecting the root node itself)
				if (!channelSelectionMap.containsKey(rootNode.getId())) {
					channelSelectionMap.put(rootNode.getId(), Arrays.asList(rootNode));
				}
				// collect all the existing root nodes
				foundIds.add(rootNode.getId());
			}
			// remove all entries for root nodes that no longer exist
			channelSelectionMap.keySet().retainAll(foundIds);
		}

		return channelSelectionMap;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return new MCCRDatasource(getId(), pool, parameters);
	}

	/**
	 * Get the DBHandle for this datasource
	 * @return DBhandle
	 */
	public DBHandle getHandle() throws DatasourceException {
		DatasourceHandle handle = pool.getHandle();
		if (handle instanceof SQLHandle) {
			return ((SQLHandle) handle).getDBHandle();
		} else {
			throw new DatasourceException("Could not get valid handle for " + this);
		}
	}

	/**
	 * Get the object with given contentid. If the object does not exist in any of the selected channels, an exception is thrown
	 * @param contentId content id
	 * @return the object
	 * @throws DatasourceException
	 */
	public MCCRObject getObjectByContentId(ContentId id) throws DatasourceException {
		// try to get from cache
		MCCRObject object = MCCRCacheHelper.getByContentId(this, id.contentId);

		if (object != null) {
			return object;
		}

		// create new (with empty channelId and contentsetId)
		object = new MCCRObject(this, 0, 0, id);
		if (!object.exists()) {
			throw new DatasourceException("Object with contentid {" + id + "} does not exist in any of the selected channels {" + getChannels() + "}");
		}

		// if the fetched object had a stored object id set, it is a channelset variant of the object with the given contentid and has a different contentid
		if (object.storedObjId != 0) {
			// so we use the stored object id, because that's one, the object really has
			object.contentId.objId = object.storedObjId;
			object.storedObjId = 0;
			// renew the contentID
			object.contentId.generateString();
		}

		// cache object
		MCCRCacheHelper.put(object, id.contentId);

		return object;
	}

	/**
	 * Get the object with given contentid. If the object does not exist in any of the selected channels, an exception is thrown
	 * @param contentId content id
	 * @return the object
	 * @throws DatasourceException
	 */
	public MCCRObject getObjectByContentId(String contentId) throws DatasourceException {
		return getObjectByContentId(new ContentId(contentId));
	}

	/**
	 * Get the object with given id
	 * @param id id
	 * @return object
	 * @throws DatasourceException if the object does not exist (or any other error occurred)
	 */
	public MCCRObject getObjectById(int id) throws DatasourceException {
		// try to get from cache
		MCCRObject object = MCCRCacheHelper.getById(this, id);

		if (object != null) {
			return object;
		}
		// create new and get from DB
		object = new MCCRObject(this, id);
		MCCRHelper.initWithId(object);
		if (object.id <= 0) {
			throw new DatasourceException("Could not find object with id " + id);
		}

		// cache object
		MCCRCacheHelper.put(object);

		return object;
	}

	/**
	 * Get the object with given channelset id
	 * @param channelsetId channelset id
	 * @return object
	 * @throws DatasourceException
	 */
	public MCCRObject getObjectByChannelsetId(int channelsetId) throws DatasourceException {
		// try to get from cache
		MCCRObject object = MCCRCacheHelper.getByChannelsetId(this, channelsetId);

		if (object != null) {
			return object;
		}

		// create object
		object = new MCCRObject(this, 0, channelsetId, null);
		if (!object.exists()) {
			throw new DatasourceException("Object with channelsetId {" + channelsetId + "} does not exist in any of the channels {" + getChannels() + "}");
		}

		// cache object
		MCCRCacheHelper.put(object);

		return object;
	}

	/**
	 * Get a clone of the object. This method will not access the cache or the DB.
	 * @param object object to clone
	 * @param cloneAttributes true if attributes shall also be cloned, false if not
	 * @return clone of the object
	 * @throws DatasourceException
	 */
	public MCCRObject getClone(MCCRObject object, boolean cloneAttributes) throws DatasourceException {
		if (object == null) {
			throw new DatasourceException("Cannot clone null object");
		}
		return new MCCRObject(this, object, cloneAttributes);
	}

	/**
	 * Clear all caches (query, attributes, objects, types, channel structure)
	 */
	public void clearCaches() {
		clearCaches(true);
	}

	/**
	 * Clear all caches for queries, attributes, objects, but only clear caches for types and structure info, if typeCaches is true
	 * @param typeCaches true to also clear the type, structure caches, false if not
	 */
	public void clearCaches(boolean typeCaches) {
		MCCRCacheHelper.clear(this, typeCaches);
	}

	/**
	 * Get the path configured to store attributes
	 * @return the attributePath
	 */
	public String getAttributePath() {
		return attributePath;
	}

	/**
	 * Create a datasourcefilter sharing the resolvables from the default rule tree
	 * @param expression expression
	 * @param resolvablesMap resolvables map
	 * @return datasource filter
	 * @throws ExpressionParserException
	 */
	protected DatasourceFilter createDatasourceFilter(Expression expression,
			Map<?, ?> resolvablesMap) throws ExpressionParserException {
		if (expression instanceof EvaluableExpression) {
			MCCRDatasourceFilter filter = null;

			try {
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_CNDATASOURCEFILTER, expression.getExpressionString());
				if (resolvablesMap == null) {
					filter = new MCCRDatasourceFilter(getHandle());
				} else {
					filter = new MCCRDatasourceFilter(getHandle(), resolvablesMap);
				}
				filter.setExpressionString(expression.getExpressionString());

				((EvaluableExpression) expression).generateFilterPart(
						new ExpressionQueryRequest(filter, this, new PropertyResolver(new MapResolver(resolvablesMap))), filter.getMainFilterPart(),
						ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			} catch (DatasourceException e) {
				throw new FilterGeneratorException(e);
			} finally {
				RuntimeProfiler.endMark(ComponentsConstants.EXPRESSIONPARSER_CNDATASOURCEFILTER, expression.getExpressionString());
			}
			return filter;
		} else {
			FilterGeneratorException e = new FilterGeneratorException("expression is not evaluable");

			e.setExpressionString(expression != null ? expression.toString() : "null");
			throw e;
		}
	}

	/**
	 * Perform the given sql query statement with the given params and process
	 * the results with the given result processor
	 * @param sqlString sql statement as string
	 * @param paramsArray array of params for the sql statement
	 * @param resultProcessor result processor for processing the results
	 * @return true when the query was successful, false if it failed
	 */
	protected boolean getDBResult(DBHandle dbHandle, String sqlString, Object[] paramsArray, ResultProcessor resultProcessor) throws DatasourceNotAvailableException {
		try {
			DB.query(dbHandle, sqlString, paramsArray, resultProcessor);
			return true;
		} catch (SQLException e) {
			throw new DatasourceNotAvailableException("Query {" + DB.debugSql(sqlString, paramsArray) + "} failed", e);
		} catch (IllegalArgumentException e) {
			throw new DatasourceNotAvailableException("Query {" + DB.debugSql(sqlString, paramsArray) + "} failed", e);
		}
	}

	/**
	 * Cast the given datasource filter into a MCCRDatasourceFilter (if possible),
	 * or throw a DatasourceException when the filter is incompatible.
	 * @param filter datasource filter instance
	 * @return instance of a MCCRDatasourceFilter
	 * @throws DatasourceException when the filter is incompatible
	 */
	protected static MCCRDatasourceFilter getAsMCCRDatasourceFilter(DatasourceFilter filter) throws DatasourceException {
		if (!(filter instanceof MCCRDatasourceFilter)) {
			throw new DatasourceException("Incompatible filter");
		}
		return (MCCRDatasourceFilter) filter;
	}

	@Override
	public String toString() {
		return pool.toString();
	}

	/**
	 * Check whether the cache is enabled for this datasource
	 * @return true when the cache is enabled, false if not
	 */
	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * Enable/disable the cache for this datasource.
	 * Disabling the cache will also clear all caches for the datasource
	 * @param enabled true to enable, false for disabling
	 */
	public void setCache(boolean enabled) {
		// when disabling the cache, clear it first
		if (!enabled) {
			clearCaches(false);
		}
		cacheEnabled = enabled;
	}

	/**
	 * Check whether this datasource has differential sync checking enabled
	 * @return true for differential sync checking, false for normal sync checking
	 */
	public boolean isDifferentialSyncChecking() {
		return differentialSyncChecking;
	}

	/**
	 * Check whether Cache Warming on startup is enabled
	 * @return true for enabled, false for disabled
	 */
	public boolean isCacheWarmingOnInit() {
		return cacheWarmingOnInit;
	}

	/**
	 * Return a DatasourceFilter used to select objects for cache warming for a channel
	 * @param channelId channel id
	 * @param timestamp update timestamp (may be 0 to not restrict by updatetimestamp)
	 * @return DatasourceFilter
	 */
	public DatasourceFilter getCacheWarmingFilter(int channelId, long timestamp) {
		DatasourceFilter cacheWarmingFilterInstance = null;
		// when a positive timestamp is given, we restrict to objects newer than that timestamp
		String filter = timestamp > 0 ? cacheWarmingFilterUpdate : cacheWarmingFilter;
		try {
			Map<String, Object> data = new HashMap<String, Object>(2);
			// parse the filter
			cacheWarmingFilterInstance = createDatasourceFilter(ExpressionParser.getInstance().parse(filter));
			// add channel id to parameters
			data.put("channelId", channelId);
			if (timestamp > 0) {
				// add timestamp to parameters
				data.put("timestamp", timestamp);
			}
			// add the parameters to the filter
			cacheWarmingFilterInstance.addBaseResolvable("data", new MapResolver(data));
		} catch (Exception e) {
			logger.error("Error while parsing filter {" + filter + "}", e);
			try {
				cacheWarmingFilterInstance = createDatasourceFilter(ExpressionParser.getInstance().parse("false"));
			} catch (Exception ignored) {}
		}

		return cacheWarmingFilterInstance;
	}

	/**
	 * Get a list of attributes used for the cache warmer
	 * @return string array of attributes
	 */
	public String[] getCacheWarmingAttributes() {
		return cacheWarmingAttributes;
	}
	
	/**
	 * Check whether cache warming is enabled
	 * @return true for enabled, false for disabled
	 */
	public boolean isCacheWarmingActive() {
		// check that cache warming is enabled and that attributes have been set
		return (isCacheEnabled() && (getCacheWarmingAttributes() != null));
	}

	/**
	 * Get the timestamp of the last update from the table "channel"
	 * @param forceRenewal true to force reading the value from the DB
	 * @return the timestamp of the last update or -1 if not available
	 */
	public long getLastUpdate(boolean forceRenewal) {
		if (!forceRenewal) {
			Long[] lastUpdate = lastUpdateTimestamp.get(getId());

			if (lastUpdate != null && lastUpdate.length == 2) {
				long mylastRenewal = lastUpdate[0].longValue();

				// usually it should be renewed every 10 seconds through syncchecking ... if this is not the case we have a fallback ..
				if (System.currentTimeMillis() - mylastRenewal > 30000) {
					String warnMessage = "Last update of datasource was not synced since more than 30 seconds. (Sync checking not active ?) {" + getId() + "}";

					logger.warn(warnMessage, new NodeException(warnMessage));
				} else {
					long lastSyncChecking = lastUpdate[1].longValue();

					if (lastSyncChecking > 0) {
						return lastSyncChecking;
					}
				}
			}
		}

		long lastUpdate = -1;
		SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

		try {
			DBHandle handle = getHandle();

			DB.query(handle, "select max(updatetimestamp) updatetimestamp from channel", resultProcessor);
			if (resultProcessor.size() > 0) {
				lastUpdate = resultProcessor.getRow(1).getLong("updatetimestamp");
				lastUpdateTimestamp.put(getId(), new Long[] { System.currentTimeMillis(), lastUpdate });
			}
		} catch (Exception e) {
			logger.warn("Error while checking for last update", e);
		}

		return lastUpdate;
	}

	/**
	 * Get the last update timestamp for the given channel, or -1 if the channel does not exist
	 * @param channelId channel id
	 * @return last update timestamp for the given channel
	 * @throws DatasourceException
	 */
	public long getLastChannelUpdate(int channelId) throws DatasourceException {
		long lastUpdate = -1;
		SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

		try {
			DBHandle handle = getHandle();

			DB.query(handle, "SELECT updatetimestamp FROM channel WHERE id = ?", new Object[] { channelId}, resultProcessor);
			if (resultProcessor.size() > 0) {
				lastUpdate = resultProcessor.getRow(1).getLong("updatetimestamp");
			}

			return lastUpdate;
		} catch (SQLException e) {
			throw new DatasourceException("Error while getting last update for channel " + channelId, e);
		}
	}

	/**
	 * Get the cache region name for the given attribute or null if the
	 * attribute shall not be cached
	 * @param attributeName name of the attribute in question
	 * @return name of the cache region or null (if attribute shall not be
	 *         cached)
	 */
	public String getAttributeCacheRegion(String attributeName) {
		if (attributeCacheSettings == null) {
			// no custom cache settings at all: all attributes are cached in the
			// default region
			return MCCRCacheHelper.ATTRIBUTESCACHEREGION;
		} else if (!attributeCacheSettings.containsKey(attributeName)) {
			// no custom cache settings for this attribute: cache in default
			// region
			return MCCRCacheHelper.ATTRIBUTESCACHEREGION;
		} else {
			AttributeCache setting = attributeCacheSettings.get(attributeName);

			if (setting.enabled) {
				return setting.region;
			} else {
				return null;
			}
		}
	}

	/**
	 * Get a list holding all custom attribute cache regions
	 * @return list of all custom attribute cache regions or empty list of non
	 *         used
	 */
	public List<String> getCustomCacheRegions() {
		if (attributeCacheSettings == null) {
			return Collections.emptyList();
		} else {
			List<String> customCacheRegions = new ArrayList<String>();

			for (AttributeCache cacheSetting : attributeCacheSettings.values()) {
				if (cacheSetting.enabled && !customCacheRegions.contains(cacheSetting.region)) {
					customCacheRegions.add(cacheSetting.region);
				}
			}
			return customCacheRegions;
		}
	}

	/**
	 * Get the threshold for the total number attributes to prefetch (number of
	 * attributenames X number of objects). If more than this number of
	 * attributes have to be prefetched, this is done in a single sql statement.
	 * Otherwise the attributes are fetched from the cache one by one, until
	 * more than {@link #getPrefetchAttributesCacheMissThreshold()} cache misses
	 * occurred. A value of -1 deactivates fetching the attributes from the
	 * cache (attributes would always be fetched in a single sql statement).
	 * @return threshold for total number of attributes
	 */
	public int getPrefetchAttributesThreshold() {
		return prefetchAttributesThreshold;
	}

	/**
	 * Set a new value for the prefetch attributes threshold
	 * @param prefetchAttributesThreshold prefetch attributes threshold
	 */
	public void setPrefetchAttributesThreshold(int prefetchAttributesThreshold) {
		this.prefetchAttributesThreshold = prefetchAttributesThreshold;
	}

	/**
	 * Get the cache miss threshold for prefetching attributes. If more than
	 * this number of attributes were not found in the cache, all attributes are
	 * prefetched with a single sql statement. A value of -1 deactivates the
	 * fallback to prefetching the attributes in a single sql statement.
	 * @return threshold for allowed cache misses
	 */
	public int getPrefetchAttributesCacheMissThreshold() {
		return prefetchAttributesCacheMissThreshold;
	}

	/**
	 * Set the absolute cache miss threshold for prefetching
	 * @param prefetchAttributesCacheMissThreshold
	 */
	public void setPrefetchAttributesCacheMissThreshold(int prefetchAttributesCacheMissThreshold) {
		this.prefetchAttributesCacheMissThreshold = prefetchAttributesCacheMissThreshold;
	}

	/**
	 * Get the cache miss threshold for prefetching the given number of
	 * attributes. If more than this number of attributes were not found in the
	 * cache, all attributes are prefeched with a single sql statement. A value
	 * of -1 deactivates the fallback to prefetching the attributes in a single
	 * sql statement
	 * @param numberOfAttributes total number of attributes
	 * @return threshold for allowed cache misses
	 */
	public int getPrefetchAttributesCacheMissThreshold(int numberOfAttributes) {
		return Math.min(prefetchAttributesCacheMissThreshold,
				prefetchAttributesCacheMissThresholdPerc > 0 ? numberOfAttributes * prefetchAttributesCacheMissThresholdPerc / 100 : 0);
	}

	/**
	 * Set the percentage value for the cache miss threshold for prefetching
	 * @param prefetchAttributesCacheMissThresholdPerc
	 */
	public void setPrefetchAttributesCacheMissThresholdPerc(int prefetchAttributesCacheMissThresholdPerc) {
		this.prefetchAttributesCacheMissThresholdPerc = prefetchAttributesCacheMissThresholdPerc;
	}
}
