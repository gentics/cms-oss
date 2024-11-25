/*
 * CNDatasource.java
 *
 * Created on 17. August 2004, 12:53
 */

package com.gentics.lib.datasource;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.AbstractVersioningDatasource;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.HandlePool;
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
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.ChangeableBean;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.functions.CNDatasourceAndOrFunction;
import com.gentics.lib.datasource.functions.CNDatasourceCalcFunction;
import com.gentics.lib.datasource.functions.CNDatasourceComparisonFunction;
import com.gentics.lib.datasource.functions.CNDatasourceConcatFunction;
import com.gentics.lib.datasource.functions.CNDatasourceExtendedComparisonFunction;
import com.gentics.lib.datasource.functions.CNDatasourceIsEmptyFunction;
import com.gentics.lib.datasource.functions.CNDatasourceUnaryFunction;
import com.gentics.lib.datasource.functions.SubRuleFunction;
import com.gentics.lib.datasource.simple.SimpleAttribute;
import com.gentics.lib.datasource.simple.SimpleDatasource;
import com.gentics.lib.datasource.simple.SimpleObject;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.DatabaseMetaDataHandler;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.etc.CacheTimeoutListener;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.etc.TimedCache;
import com.gentics.lib.expressionparser.functions.FunctionRegistry;
import com.gentics.lib.expressionparser.functions.FunctionRegistryException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.parser.rule.CompareOperator;
import com.gentics.lib.parser.rule.Condition;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.parser.rule.FunctionAsStringOperand;
import com.gentics.lib.parser.rule.ObjectOperand;
import com.gentics.lib.parser.rule.Operand;
import com.gentics.lib.parser.rule.PropertyOperand;
import com.gentics.lib.parser.rule.SubRuleResolver;
import com.gentics.lib.parser.rule.functions.ConcatFunction;
import com.gentics.lib.parser.rule.functions.Function;
import com.gentics.lib.parser.rule.functions.FunctionOperand;
import com.gentics.lib.parser.rule.functions.IsEmptyFunction;
import com.gentics.lib.parser.rule.functions.SubruleFunction;

/**
 * The CNDatasource is a Datasource Implementation for accessing the
 * Content.Node Database Structure which capsules all the needed statements and
 * joins into this interface. A CN Dataset is assigned to a contentid. This
 * contentid can have multiple attributes. The attributes are dynamic. A
 * ContentID consists of a Type part + "." + an ID part. The Type part is
 * important for having different categories of objects like (draft, articles,
 * images, and so on)
 * @author Dietmar
 */
public class CNDatasource extends AbstractVersioningDatasource implements SimpleDatasource {

	/**
	 * Logger object
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(CNDatasource.class);

	/**
	 * versioning object for the content map
	 */
	protected TableVersion contentMapVersion;

	/**
	 * versioning object for one contentattribute
	 */
	protected TableVersion contentAttributeVersion;

	/**
	 * versioning object for all contentattributes
	 */
	protected TableVersion allContentAttributeVersion;

	/**
	 * A reference to a handle pool for accessing the DB Handles for this
	 * Datasource
	 */
	private HandlePool handlePool;

	/**
	 * the saved rule tree for the pxnl
	 */
	private RuleTree ruleTree;

	/**
	 * datasource filter that was generated from a set ruleTree (in case the
	 * ruleTree is not in compatibility mode)
	 */
	private DatasourceFilter datasourceFilter;

	/**
	 * this hashmap stores the mappings of the table aliases (important for the
	 * FROM clause and the condition creation)
	 */
	private LinkedHashMap TableAliasMap = null;

	//
	/**
	 * count_attribute_position saves an integer for creating the variables that
	 * are then stored in the AliasMap
	 */
	private int count_attribute_position = 0;

	/**
	 * JoinParamerst are saved in a this ArrayList they will be connected one by
	 * one. It is therefore important to write the pxnl conditions in a
	 * performant order
	 */
	private ArrayList JoinParams = null;

	/* !MOD 20040831 DG added string array for attribute Names */
	
	/**
	 * saves the Columns to prefetch
	 */
	private String[] PrefetchColumns = null;

	/**
	 * tables that are required for versioning
	 */
	private String[] requiredVersioningTables = null;

	/**
	 * fields that are required for versioning in the versioning tables
	 */
	private final static String[] requiredVersioningFields = new String[] {
		"id", "nodeversiontimestamp", "nodeversion_user", "nodeversionlatest",
		"nodeversionremoved", "nodeversion_autoupdate"};

	/**
	 * Name of the cacheregion for queryresults
	 */
	protected final static String RESULTSCACHEREGION = "gentics-portal-contentrepository-results";

	/**
	 * name of the datasource configuration parameter to enable the
	 * compatibility mode that treats integers as strings
	 */
	protected final static String INTEGERASSTRING = "compatibility.integerasstring";

	/**
	 * name of the datasource configuration parameter to enable the
	 * compatibility mode that fetches illegal object links as dummy objects instead
	 * of null
	 */
	public final static String ILLEGALLINKSNOTNULL = "compatibility.illegallinksnotnull";

	/**
	 * The cache for queryresults
	 */
	protected static PortalCache queryResultsCache = null;

	/**
	 * pattern for interpretation of custom attribute cache settings
	 */
	protected static Pattern customAttributeCacheSettingsPattern = null;

	/**
	 * name of the datasource configuration parameter to enable prefetching of
	 * optimized attributes when initializing contentobjects or doing filters
	 */
	public final static String AUTOPREFETCH = "autoprefetch";

	/**
	 * name of the datasource configuration parameter to configure the
	 * prefetched optimized attributes
	 */
	public final static String AUTOPREFETCHATTRIBUTES = "autoprefetch.attributes";

	static {
		customAttributeCacheSettingsPattern = Pattern.compile("cache\\.attribute\\.([^.]+)(\\.region)?");
		NodeLogger logger = NodeLogger.getNodeLogger(GenticsContentFactory.class);
		// add all functions to generate the DatasourceFilter into the function registry
		FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

		try {
			// add basic functions
			functionRegistry.registerFunction(CNDatasourceAndOrFunction.class.getName());
			functionRegistry.registerFunction(CNDatasourceCalcFunction.class.getName());
			functionRegistry.registerFunction(CNDatasourceComparisonFunction.class.getName());
			functionRegistry.registerFunction(CNDatasourceExtendedComparisonFunction.class.getName());
			functionRegistry.registerFunction(CNDatasourceUnaryFunction.class.getName());
			// add named functions
			functionRegistry.registerFunction(CNDatasourceConcatFunction.class.getName());
			functionRegistry.registerFunction(CNDatasourceIsEmptyFunction.class.getName());
			functionRegistry.registerFunction(SubRuleFunction.class.getName());
		} catch (FunctionRegistryException e) {
			logger.error("Error while registering functions", e);
		}

		try {
			// get the portal cache for queryresults
			queryResultsCache = PortalCache.getCache(RESULTSCACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		} catch (NoClassDefFoundError e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		}
	}

	/**
	 * This Cache saves the the optimized columns for some time. If the time
	 * runs out it will refetch them from the datasource
	 * @todo use a datasource property for defining the timeout of this cache
	 */
	private TimedCache optimizedMapCache = new TimedCache(1000 * 60 * 1, new CacheTimeoutListener() {
		public Object updateCacheObject(Object o) {
			try {
				return getOptimizeMap();
			} catch (Exception e) {
				NodeLogger.getLogger(getClass()).error("error while updating cache object", e);
				return o;
			}
		}
	});

	/**
	 * parameter name for attribute path
	 */
	public final static String ATTRIBUTE_PATH = "attribute.path";

	/**
	 * path where attributes should be stored
	 */
	private String attributePath = null;
    
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
	 * flag to check if cache warming is enabled at startup
	 */
	private boolean cacheWarmingOnInit = false;

	/**
	 * filter used to determine which objects are cached by default
	 */
	private String cacheWarmingFilter = null;
    
	/**
	 * list of attributes that should be cached
	 */
	private String cacheWarmingAttributes[] = null;
    
	/**
	 * DatasourceFilter generated from the cacheWarmingFilter
	 */
	private DatasourceFilter filter = null;
    
	/**
	 * flag to check whether versioning is supported or not
	 */
	private boolean versioning = false;

	/**
	 * initialization parameters (for later use)
	 */
	private Map parameters;

	/**
	 * timestamp for getting versioned results (if {@link #versioning}is true).
	 * set to -1 for getting current results
	 */
	private int timestamp = -1;

	/**
	 * currently used timestamp for getting versioned results
	 */
	private Integer currentTimestamp;

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
	private Map attributeCacheSettings = null;

	/**
	 * Flag to mark whether foreing linked attribute shall be cached (only when
	 * datasource supports caching at all)
	 */
	private boolean cacheForeignLinkAttributes = false;

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
	 * flag to enable the compatibility mode for treating integers as strings
	 */
	protected boolean integerAsString = false;

	/**
	 * flag to enable fetching illegal object links as dummy objects (instead of null)
	 */
	protected boolean getIllegalLinksAsDummyObjects = false;
    
	/**
	 * Stores the last update timestamp for each datasource id.
	 * key: datasource id, value: Object[] { timestamp of when it was last refreshed (Long), update timestamp (Long) }
	 */
	protected static Map lastUpdateTimestamp = new HashMap();

	/**
	 * flag to enable/disable automatic prefetching of optimized attributes
	 */
	protected boolean autoprefetch = false;

	/**
	 * names of the configured prefetch attributes, null when all optimized
	 * attributes shall be prefetched
	 */
	protected String[] autoPrefetchAttributes;

	/**
	 * Database product name
	 */
	protected String databaseProductName;

	/* CONSTRUCTOR AREA */
	
	/**
	 * The constructor creates an instance of a CNDatasource -> this is called
	 * by the datasource factory.
	 * @param id Id for this datasource.
	 * @param pool The handle pool for the CN Datasource handles. This should
	 *        usually be the RandomHandlePool.
	 * @param parameters loading parameters for the datasource from the xml
	 *        datasource descriptor files
	 */
	public CNDatasource(String id, HandlePool pool, Map parameters) {
		super(id);
		this.handlePool = pool;

		requiredVersioningTables = new String[0];
		try {
			// set the db names
			DBHandle dbHandle = getHandle().getDBHandle();
			databaseProductName = dbHandle.getDatabaseProductName();
			if (parameters != null) {
				// set the custom table names
				dbHandle.setTableNames(ObjectTransformer.getString(parameters.get("table.contentstatus"), null),
						ObjectTransformer.getString(parameters.get("table.contentobject"), null),
						ObjectTransformer.getString(parameters.get("table.contentattributetype"), null),
						ObjectTransformer.getString(parameters.get("table.contentmap"), null),
						ObjectTransformer.getString(parameters.get("table.contentattribute"), null), null);
			} else {
				// no parameters given at all, we use the default table names
				dbHandle.setTableNames(null, null, null, null, null, null);
			}
			requiredVersioningTables = new String[] {
					dbHandle.getContentAttributeName() + "_nodeversion", dbHandle.getContentMapName() + "_nodeversion"};
		} catch (Exception e) {
			logger.error("Error while checking table names - Datasource {" + id + "} will not function properly", e);
		}

		this.parameters = parameters;
		if (parameters != null) {
			versioning = ObjectTransformer.getBoolean(parameters.get("versioning"), false);
			cacheEnabled = ObjectTransformer.getBoolean(parameters.get("cache"), false);
			cacheSyncChecking = ObjectTransformer.getBoolean(parameters.get("cache.syncchecking"), false);
			differentialSyncChecking = ObjectTransformer.getBoolean(parameters.get("cache.syncchecking.differential"), differentialSyncChecking);

			// load parameter for file system
			attributePath = ObjectTransformer.getString(parameters.get(ATTRIBUTE_PATH), null);

			// load parameters for cache warming
			cacheWarmingOnInit = ObjectTransformer.getBoolean(parameters.get(CACHE_WARMING_ON_INIT), true);
			cacheWarmingFilter = ObjectTransformer.getString(parameters.get(CACHE_WARMING_FILTER), "true");
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

			if (cacheEnabled) {
				cacheForeignLinkAttributes = ObjectTransformer.getBoolean(parameters.get("cache.foreignlinkattributes"), false);
				attributeCacheSettings = getCustomCacheSettings(parameters);
			}

			integerAsString = ObjectTransformer.getBoolean(parameters.get(INTEGERASSTRING), integerAsString);

			getIllegalLinksAsDummyObjects = ObjectTransformer.getBoolean(parameters.get(ILLEGALLINKSNOTNULL), getIllegalLinksAsDummyObjects);

			// configuration settings for autoprefetch
			autoprefetch = ObjectTransformer.getBoolean(parameters.get(AUTOPREFETCH), autoprefetch);
			// also get eventually configured autoprefetch.attributes
			String configuredAttributes = ObjectTransformer.getString(parameters.get(AUTOPREFETCHATTRIBUTES), null);

			if (configuredAttributes != null) {
				autoPrefetchAttributes = configuredAttributes.split(",");
				// trim the configured attribute names
				for (int i = 0; i < autoPrefetchAttributes.length; i++) {
					autoPrefetchAttributes[i] = autoPrefetchAttributes[i].trim();
				}
			}
			// TODO output debug log about the autoprefetching

		} else {
			versioning = false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("datasource " + (versioning ? "does" : "does not") + " support versioning");
		}
	}

	/* CONSTRUCTOR AREA END */

	/**
	 * Interpret the given parameters and create the custom attribute cache settings map
	 * @param parameters given datasource parameters
	 * @return map holding the custom attribute cache settings or null if none found
	 */
	public static Map getCustomCacheSettings(Map parameters) {
		Map customCacheSettings = null;

		// now check all parameters for custom attribute cache settings
		for (Iterator iterator = parameters.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = ObjectTransformer.getString(entry.getKey(), null);

			if (!StringUtils.isEmpty(key) && key.startsWith("cache.attribute.")) {
				// found a special attribute setting
				Matcher matcher = customAttributeCacheSettingsPattern.matcher(key);

				if (matcher.matches()) {
					String attributeName = matcher.group(1);
					boolean regionSetting = !StringUtils.isEmpty(matcher.group(2));

					if (customCacheSettings == null) {
						// only create the map when needed
						customCacheSettings = new HashMap();
					}

					Object currentSetting = customCacheSettings.get(attributeName);

					if (currentSetting instanceof Boolean && !((Boolean) currentSetting).booleanValue()) {// ignore any further settings, caching is deactivated
						// for this attribute
					} else {
						if (regionSetting) {
							customCacheSettings.put(attributeName, ObjectTransformer.getString(entry.getValue(), null));
						} else {
							boolean active = ObjectTransformer.getBoolean(entry.getValue(), true);

							if (!active) {
								customCacheSettings.put(attributeName, Boolean.FALSE);
							}
						}
					}
				}
			}
		}

		return customCacheSettings;
	}

	/* GENERAL FUNCTION AREA */
	
	/**
	 * returns the saved handle pool
	 * @return the saved handle pool
	 */
	public HandlePool getHandlePool() {
		return this.handlePool;
	}

	/**
	 * This method returns the sql handle of the current handle pool. It assumes
	 * that the handles are actually SQLHandles and casts it. If this are not
	 * SQL Handles this will occure in an Error.
	 * @return the sql handle from the handle pool
	 * @todo use this method throughout the whole module
	 */
	public SQLHandle getHandle() throws CMSUnavailableException {
		DatasourceHandle handle = handlePool.getHandle();
		if (handle instanceof SQLHandle) {
			return (SQLHandle)handle;
		} else {
			throw new CMSUnavailableException("Could not find a valid handle for " + this);
		}
	}

	/**
	 * returns the optimized map. A Map which contains the "attribute" to
	 * "optimized column" name mapping
	 * @throws SQLException if an error occurs during fetching the optimized
	 *         columns
	 * @return the optimized map
	 * @todo this should be used everywhere within the CNDatasource
	 */
	public HashMap getOptimizeMap() throws SQLException, CMSUnavailableException {
		HashMap map = new HashMap();
		SimpleResultProcessor rs = new SimpleResultProcessor();

		DB.query(getHandle().getDBHandle(),
				"SELECT name, quickname FROM " + getHandle().getDBHandle().getContentAttributeTypeName() + " WHERE optimized=1", rs);
		Iterator it = rs.iterator();

		while (it.hasNext()) {
			SimpleResultRow row = (SimpleResultRow) it.next();

			map.put(row.getString("name"), row.getString("quickname"));
		}
		// add std columns
		map.putAll(DatatypeHelper.getDefaultQuickColumns());
		return map;
	}

	/**
	 * returns the Optimized Column Names. This Method is important for usage of
	 * the optimized Constraints of the CN Attribute Map
	 * @param attrib The looked up Attribute
	 * @return The optimized Column Name or null if no optimized Column exists
	 *         for this Attribute
	 */
	public String getOptimizedColName(String attrib) {
		try {
			return DatatypeHelper.getQuickColumn(getHandle().getDBHandle(), attrib);
		} catch (CMSUnavailableException e) {
			logger.error("Error while fetching quickname for attribute {" + attrib + "}", e);
			return null;
		}
		// try {
		// // HashMap optimizedMap = getOptimizeMap();
		// HashMap optimizedMap = (HashMap) optimizedMapCache.get();
		// if (optimizedMap != null) {
		// return (String) optimizedMap.get(attrib);
		// }
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }
		// return null;
	}

	/**
	 * gets the table name for the column. If the column was already used before
	 * it will use the saved table name. else it will create a new table name
	 * @param ColumnName the column name that is wanted
	 * @return the table name for usage
	 */
	private String getNewTableName(String ColumnName) {
		int dotPos = ColumnName.indexOf('.');
		String ret = null;

		// when the column name contains dots (.) we assume it to be an
		// attribute of a linked object
		// all attributes but the rightmost have to be linked objects
		while (dotPos >= 0) {
			String subPart = ColumnName.substring(0, dotPos);

			Object entry = TableAliasMap.get(subPart);

			if (entry == null) {
				// no entry
				count_attribute_position++;
				// go and create a new table alias
				ret = "ca" + count_attribute_position;
				TableAliasMap.put(subPart, ret);
			} else {
				ret = (String) entry;
			}

			dotPos = ColumnName.indexOf('.', dotPos + 1);
		}

		// check if there is already an entry for this object column name
		// this is to avoid duplicate joins
		Object entry = TableAliasMap.get(ColumnName);

		if (entry == null) {
			// no entry
			count_attribute_position++;
			// go and create a new table alias
			ret = "ca" + count_attribute_position;
			TableAliasMap.put(ColumnName, ret);
		} else {
			ret = (String) entry;
		}
		return ret;
	}

	/**
	 * Method to set the rule tree of the parsed pxnl for the datasource
	 * @param ruleTree The parsed Rule Tree representing the PXNL statement
	 */
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

	/* GENERAL FUNCTION AREA END */

	/* EXECUTION FUNCTION AREA */
	
	/**
	 * returns the CNDatasource Object for the current set attributes
	 * @return the DatasourceRecordSet that math with the set Attributes
	 */
	public Collection getResult() throws DatasourceNotAvailableException {
		return getResult(-1, -1, null, -1, timestamp);
	}

	public Collection getResult(String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		return getResult(-1, -1, sortBy, sortOrder, timestamp);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	
	/**
	 * returns the CNDatasource Object for the current set attributes
	 * @param start defines an offset of all the returned records that are
	 *        actually displayed (startpoint of records that are returned)
	 *        default is 0
	 * @param count amount of records to return
	 * @param sortBy an attribute name by which the sorting shall happen
	 * @param sortOrder the kind of order that shall happen for the sortBy Field
	 *        (Ascending, Descending, Default by Datasource)
	 * @param specificParameters A map of specific parameters
	 * @return the DatasourceRecordSet that math with the set Attributes
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {
		// default implementation ignores extra parameters
		return getResult(start, count, sortBy, sortOrder, timestamp);
	}

	/**
	 * returns the CNDatasource Object for the current set attributes
	 * @param start defines an offset of all the returned records that are
	 *        actually displayed (startpoint of records that are returned)
	 *        default is 0
	 * @param count amount of records to return
	 * @param sortBy an attribute name by which the sorting shall happen
	 * @param sortOrder the kind of order that shall happen for the sortBy Field
	 *        (Ascending, Descending, Default by Datasource)
	 * @return the DatasourceRecordSet that math with the set Attributes
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		return getResult(start, count, sortBy, sortOrder, timestamp);
	}

	/**
	 * returns the CNDatasource Object for the current set attributes
	 * @param start defines an offset of all the returned records that are
	 *        actually displayed (startpoint of records that are returned)
	 *        default is 0
	 * @param count amount of records to return
	 * @param sortBy an attribute name by which the sorting shall happen
	 * @param sortOrder the kind of order that shall happen for the sortBy Field
	 *        (Ascending, Descending,
	 * @param versionTimestamp timestamp for getting versioned results (-1 for
	 *        current results) Default by Datasource)
	 * @return the DatasourceRecordSet that math with the set Attributes
	 */
	public DatasourceRecordSet getResult(int start, int count, String sortBy, int sortOrder,
			int versionTimestamp) throws DatasourceNotAvailableException {
		// check whether the set ruleTree is in compatibility mode
		if (ruleTree != null) {
			DatasourceRecordSet recordSet = new CNDatasourceRecordSet(this);
			Expression expression = ruleTree.getExpression();

			if (expression != null) {
				// the ruleTree was not in compatibility mode, so we forward
				// this call to the call using the expression (new usage)
				Sorting[] sorting = null;

				if (!StringUtils.isEmpty(sortBy)) {
					String[] sortedColumns = sortBy.split(",");

					sorting = new Sorting[sortedColumns.length];
					for (int i = 0; i < sortedColumns.length; i++) {
						sorting[i] = new Sorting(sortedColumns[i].trim(), sortOrder);
					}
				}
				Collection result = null;

				try {
					result = getResult(datasourceFilter, PrefetchColumns, start, count, sorting, versionTimestamp);
				} catch (DatasourceException e) {
					throw new DatasourceNotAvailableException("Error while getting result", e);
				}

				// convert the collection of GenticsContentObjects into a DatasourceRecordSet
				if (result != null) {
					for (Iterator iter = result.iterator(); iter.hasNext();) {
						GenticsContentObject element = (GenticsContentObject) iter.next();
						DatasourceRow row = new CNDatasourceRow(element);

						recordSet.addRow(row);
					}
				}

				// return the result
				return recordSet;
			}
		}

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, ruleTree.getRuleString());

			// start and end time
			long StartMillis = System.currentTimeMillis();

			StringBuffer sql = new StringBuffer(100);
			StringBuffer optimizedSql = new StringBuffer(100);
			SimpleResultProcessor ConditionStringProc = new SimpleResultProcessor();

			ConditionStringProc.setLimit(start, start + count);
			ArrayList params = new ArrayList();
			ArrayList optimizedParams = new ArrayList();

			// set the current timestamp
			currentTimestamp = new Integer(versionTimestamp);

			JoinParams = new ArrayList();
			// sql.append("SELECT DISTINCT cm.contentid \nFROM ");
			sql.append("SELECT cm.obj_id, cm.obj_type, cm.updatetimestamp, cm.mother_obj_id, cm.mother_obj_type \nFROM ");

			// init from clauses
			TableAliasMap = new LinkedHashMap();
			count_attribute_position = 0;

			// when no ruletree is set, there will be no WhereClause
			String WhereClause = ruleTree != null ? ruleToCNCondition(this.ruleTree, params, optimizedSql, optimizedParams) : "";

			WhereClause = getVersionWhereClause(versionTimestamp, params, WhereClause);

			/*
			 * prepare order by string. This needs to be done before creating
			 * the from clause, as necessary tables might need to be joined
			 */
			String[] orderByColumns = prepareOrderByColumns(sortBy);
			String OrderByPart = prepareOrderBy(orderByColumns, sortOrder);

			String GroupByPart = getGroupByPart(orderByColumns);

			// create from clause
			String FromClause = getFromClause();

			// rewrite params with join params
			params = rewriteParams(params);

			sql.append(FromClause);
			if (WhereClause.length() > 0) {
				sql.append(" \nWHERE ");
				sql.append(WhereClause);
			}

			// group by stuff
			if (GroupByPart.trim().length() > 0) {
				sql.append(" \nGROUP BY ");
				sql.append(GroupByPart);
			}

			// append order by part
			if (OrderByPart.trim().length() > 0) {
				sql.append(" \nORDER BY ");
				sql.append(OrderByPart);
			}

			// transform the sql statement into a string
			String sqlString = sql.toString();
			// transform the params into an array
			Object[] paramsArray = params.toArray();
			// prepare the record set
			CNDatasourceRecordSet res = null;

			try {
				// TODO better use the ruletree + all relevant data as cachekey,
				// since creation of the sql statement is not necessary but time
				// intensive
				if (isCacheEnabled() && queryResultsCache != null) {
					// caching is enabled
					Object cacheKey = getCacheKey(sqlString, paramsArray, start, count);
					SimpleResultProcessor cachedResult = null;

					if (cacheKey != null) {
						try {
							cachedResult = (SimpleResultProcessor) queryResultsCache.getFromGroup(toString(), cacheKey);
						} catch (Exception e) {
							logger.warn("Error while getting cached query result", e);
						}
					}

					if (cachedResult != null) {
						// we found a cached result, now use it
						ConditionStringProc = cachedResult;
						res = new CNDatasourceRecordSet(this, ConditionStringProc, versionTimestamp);
					} else {
						// no cached result found, do the query and cache the
						// result
						if (getDBResult(getHandle().getDBHandle(), sqlString, paramsArray, ConditionStringProc)) {
							try {
								res = new CNDatasourceRecordSet(this, ConditionStringProc, versionTimestamp);
								queryResultsCache.putIntoGroup(toString(), cacheKey, ConditionStringProc);
							} catch (PortalCacheException e) {
								logger.warn("Error while caching query results", e);
							}
						}
					}
				} else {
					// caching is disabled
					if (getDBResult(getHandle().getDBHandle(), sqlString, paramsArray, ConditionStringProc)) {
						res = new CNDatasourceRecordSet(this, ConditionStringProc, versionTimestamp);
					}
				}
			} catch (NodeIllegalArgumentException e) {
				throw new DatasourceNotAvailableException("Error while fetching results", e);
			} catch (CMSUnavailableException e) {
				throw new DatasourceNotAvailableException("Error while fetching results", e);
			}

			// reset the current timestamp
			currentTimestamp = null;

			long EndMillis = System.currentTimeMillis();

			if (logger.isDebugEnabled()) {
				logger.debug("==============>>>>==========================================");
				logger.debug("==============>>>> CNDATASOURCE: (" + StartMillis + "," + EndMillis + "," + (EndMillis - StartMillis) + ")");
				logger.debug("==============>>>>==========================================");
			}
			if (res == null) {
				return new CNDatasourceRecordSet(this);
			}

			// prefill objects when attribute names are given and prefetching is on
			if (prefetchAttributes && !ObjectTransformer.isEmpty(PrefetchColumns)) {
				try {
					GenticsContentFactory.prefillContentObjects(this, res, PrefetchColumns, versionTimestamp);
				} catch (Exception ex) {
					logger.warn("Error while prefetching attributes", ex);
				}
			}

			return res;
		} catch (CMSUnavailableException e) {
			throw new DatasourceNotAvailableException("", e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, ruleTree.getRuleString());
		}
	}
    
	/**
	 * Creates the "Where" clause for getCount(....) and getResult(....)
	 * @param versionTimestamp version timestamp to be used.
	 * @param params the params of the query.
	 * @param whereClause the where clause before adding version where.
	 * @return the new where clause
	 */
	private String getVersionWhereClause(int versionTimestamp, ArrayList params,
			String whereClause) throws CMSUnavailableException {
		// add the versioning part to the where clause if a versioned query
		// is
		// done
		if (versioning && versionTimestamp >= 0) {
			String versionWhereClause = "cm.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM " + getHandle().getDBHandle().getContentMapName()
					+ "_nodeversion WHERE " + getHandle().getDBHandle().getContentMapName()
					+ "_nodeversion.id = cm.id AND nodeversiontimestamp <= ? AND (nodeversionremoved > ? OR nodeversionremoved = 0))";

			// add the timestamp twice to the params (at the first position)
			params.add(0, currentTimestamp);
			params.add(0, currentTimestamp);
			if (whereClause.length() > 0) {
				whereClause = versionWhereClause + " AND " + whereClause;
			} else {
				whereClause = versionWhereClause;
			}
		}
		return whereClause;
	}

	/**
	 * This Method will create the Order By String of the SELECT statement. It
	 * will explode the string by "," and check each column for optimized
	 * column, usage and needed joins. it will also append the sort order
	 * (ascending, descending)
	 * @param OrderByParameters the string specifying the order by attributes
	 *        delimited by ","
	 * @param SortOrder the sort order as constant String
	 *        {@see com.gentics.lib.datasource.Datasource}
	 * @return the order by string needed for the statement
	 */
	private String prepareOrderBy(String OrderByParameters, int SortOrder) {
		StringBuffer ret = new StringBuffer();

		boolean first = true;

		// first split orderbyparameters
		if (OrderByParameters != null) {
			java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(OrderByParameters, ",");

			while (tokenizer.hasMoreElements()) {
				String CurOrderColumn = tokenizer.nextToken().trim();

				// NOP: order columns may be given with prefix "object.", if so,
				// we strip this prefix
				// TODO think about this fix
				if (CurOrderColumn.startsWith("object.")) {
					CurOrderColumn = CurOrderColumn.substring(7);
				}
				// if this is not first
				if (!first) {
					// append colon
					ret.append(',');
				}
				// check if this is an optimized column?
				String OptOrderColumn = getOptimizedColName(CurOrderColumn);

				if (OptOrderColumn != null) {
					// optimized
					ret.append("cm.");
					ret.append(OptOrderColumn);
				} else {
					// not optimized
					// get table for this column
					String Alias = getNewTableName(CurOrderColumn);

					ret.append(Alias);
					ret.append(".");
					// get the value column in contentattribute (of the last
					// contentattribute of the path)
					ret.append(getDataTypeColumn(CurOrderColumn.substring(CurOrderColumn.lastIndexOf('.') + 1)));
				}
				// check SortOrder
				if (SortOrder == SORTORDER_DESC) {
					ret.append(" DESC");
				} else if (SortOrder == SORTORDER_ASC) {
					ret.append(" ASC");
				}
				first = false;
			}
		}
		return ret.toString();
	}

	/**
	 * This Method will create the Order By String of the SELECT statement. It
	 * will explode the string by "," and check each column for optimized
	 * column, usage and needed joins. it will also append the sort order
	 * (ascending, descending)
	 * @param orderByParameters array of prepared order by columns
	 * @param sortOrder the sort order as constant String
	 *        {@see com.gentics.lib.datasource.Datasource}
	 * @return the order by string needed for the statement
	 */
	private String prepareOrderBy(String[] orderByParameters, int sortOrder) {
		StringBuffer ret = new StringBuffer();

		boolean first = true;

		// first split orderbyparameters
		if (orderByParameters != null) {
			for (int i = 0; i < orderByParameters.length; ++i) {
				// if this is not first
				if (!first) {
					// append colon
					ret.append(',');
				}
				ret.append(orderByParameters[i]);
				// check SortOrder
				if (sortOrder == SORTORDER_DESC) {
					ret.append(" DESC");
				} else if (sortOrder == SORTORDER_ASC) {
					ret.append(" ASC");
				}
				first = false;
			}
		}
		return ret.toString();
	}

	/**
	 * Get array of orderby columns
	 * @param orderbyParameters string with the orderby parameters as csv
	 * @return array of sortby columns
	 */
	private String[] prepareOrderByColumns(String orderbyParameters) {
		String[] orderColumns = null;

		if (orderbyParameters != null && orderbyParameters.length() > 0) {
			orderColumns = orderbyParameters.split(",");
			for (int i = 0; i < orderColumns.length; ++i) {
				orderColumns[i] = orderColumns[i].trim();
				if (orderColumns[i].startsWith("object.")) {
					orderColumns[i] = orderColumns[i].substring(7);
				}

				String optOrderColumn = getOptimizedColName(orderColumns[i]);

				if (optOrderColumn != null) {
					// optimized
					orderColumns[i] = "cm." + optOrderColumn;
				} else {
					// not optimized
					// get table for this column
					String alias = getNewTableName(orderColumns[i]);

					// get the value column in contentattribute (of the last
					// contentattribute of the path)
					orderColumns[i] = alias + "." + getDataTypeColumn(orderColumns[i].substring(orderColumns[i].lastIndexOf('.') + 1));
				}
			}
		} else {
			orderColumns = new String[0];
		}

		return orderColumns;
	}

	/**
	 * returns a fixed group by part for the SELECT statement
	 * @param sortColumns
	 * @return the group by part for the select statment
	 */
	private String getGroupByPart(String[] sortColumns) {
		StringBuffer groupBy = new StringBuffer();

		if (sortColumns != null) {
			for (int i = 0; i < sortColumns.length; ++i) {
				if (groupBy.length() > 0) {
					groupBy.append(",");
				}
				groupBy.append(sortColumns[i]);
			}
		}

		if (groupBy.length() > 0) {
			groupBy.append(",");
		}
		groupBy.append("cm.obj_id, cm.obj_type, cm.updatetimestamp, cm.mother_obj_id, cm.mother_obj_type");
		return groupBy.toString();
	}

	/**
	 * Creates the From Clause from the saved TableAliasMap
	 * @return the from clause
	 */
	private String getFromClause() throws CMSUnavailableException {
		// do we have to fetch the data from the _nodeversion tables?
		boolean fetchFromNodeversion = currentTimestamp != null && currentTimestamp.intValue() >= 0 && versioning;

		StringBuffer ConditionString = fetchFromNodeversion
				? new StringBuffer(getHandle().getDBHandle().getContentMapName() + "_nodeversion cm")
				: new StringBuffer(getHandle().getDBHandle().getContentMapName() + " cm");

		if (TableAliasMap.size() > 0) {
			ConditionString = new StringBuffer();
			boolean first = true;
			// get all the needed columns
			java.util.Set ks = TableAliasMap.keySet();

			// for (int i = 0; i < ks.size()-1; i++) {
			// ConditionString.append("(");
			// }
			if (fetchFromNodeversion) {
				ConditionString.append(getHandle().getDBHandle().getContentMapName() + "_nodeversion cm");
			} else {
				ConditionString.append(getHandle().getDBHandle().getContentMapName() + " cm");
			}
			Iterator keyIterator = ks.iterator();

			while (keyIterator.hasNext()) {
				// get the column for
				String ColumnNameValue = (String) keyIterator.next();
				// get key value (table name)
				String CurKey = (String) TableAliasMap.get(ColumnNameValue);

				// add to from clause

				// choose the joined table depending on whether a query by
				// timestamp shall be done or not
				if (fetchFromNodeversion) {
					// query by timestamp, so use contentattribute_nodeversion
					ConditionString.append(" left join " + getHandle().getDBHandle().getContentAttributeName() + "_nodeversion");
				} else {
					// normal query
					ConditionString.append(" left join " + getHandle().getDBHandle().getContentAttributeName());
				}
				ConditionString.append(" as ");
				ConditionString.append(CurKey);

				// join condition
				if (ColumnNameValue.indexOf('.') >= 0) {
					// this is an attribute of a linked object, so join with
					// another table
					String otherKey = (String) TableAliasMap.get(ColumnNameValue.substring(0, ColumnNameValue.lastIndexOf('.')));

					ConditionString.append(" on (");
					ConditionString.append(CurKey);
					ConditionString.append(".contentid = ").append(otherKey).append(".value_text AND ");
					ConditionString.append(CurKey);
					ConditionString.append(".name = ?");
					// add parameter
					JoinParams.add(ColumnNameValue.substring(ColumnNameValue.lastIndexOf(".") + 1));

				} else {
					ConditionString.append(" on (");
					ConditionString.append(CurKey);
					ConditionString.append(".contentid = cm.contentid AND ");
					ConditionString.append(CurKey);
					ConditionString.append(".name = ?");
					// add parameter
					JoinParams.add(ColumnNameValue);
				}

				if (fetchFromNodeversion) {
					// TODO: make nodeversion join for linked attributes!!
					// for a query by timestamp, add another on-clause to select
					// the attribute-version
					// at the given timestamp (using a subselect)
					ConditionString.append(" AND ").append(CurKey).append(".nodeversiontimestamp = ");
					ConditionString.append(
							"(SELECT max(nodeversiontimestamp) FROM " + getHandle().getDBHandle().getContentAttributeName()
							+ "_nodeversion WHERE nodeversiontimestamp <= ? AND contentid = cm.contentid and id = ");
					ConditionString.append(CurKey).append(".id AND (");
					ConditionString.append("nodeversionremoved = 0 OR nodeversionremoved > ?))");

					// add more parameters
					JoinParams.add(currentTimestamp);
					JoinParams.add(currentTimestamp);
				}

				ConditionString.append(" ) ");
				// ConditionString.append(")");

				first = false;
			}
		}

		return ConditionString.toString();
	}

	/**
	 * counts the available contentrows for the given constraints
	 * @return counted rows
	 */
	public int getCount2() throws DatasourceNotAvailableException {
		return getCount(timestamp);
	}

	/**
	 * Counts the available contentrows that are available for the given join
	 * constraints.
	 * @param versionTimestamp timestamp for versioned queries
	 * @return the counted rows for the given joins
	 */
	public int getCount(int versionTimestamp) throws DatasourceNotAvailableException {
		// check whether the set ruleTree is in compatibility mode
		if (ruleTree != null) {
			Expression expression = ruleTree.getExpression();

			if (expression != null) {
				// the ruleTree was not in compatibility mode, so we forward
				// this call to the call using the expression (new usage)
				try {
					return getCount(datasourceFilter, versionTimestamp);
				} catch (DatasourceException e) {
					throw new DatasourceNotAvailableException("Error while counting results", e);
				}
			}
		}

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETCOUNT, ruleTree.getRuleString());
			int count = 0;

			// set the current timestamp
			currentTimestamp = new Integer(versionTimestamp);

			StringBuffer sql = new StringBuffer(500);
			StringBuffer optimizedSql = new StringBuffer(500);
			SimpleResultProcessor srp = new SimpleResultProcessor();
			ArrayList params = new ArrayList();
			ArrayList optimizedParams = new ArrayList();

			JoinParams = new ArrayList();
			sql.append("SELECT count( distinct cm.contentid ) as c \nFROM ");

			// init from clauses
			TableAliasMap = new LinkedHashMap();
			count_attribute_position = 0;

			String WhereClause = ruleToCNCondition(this.ruleTree, params, optimizedSql, optimizedParams);

			WhereClause = getVersionWhereClause(versionTimestamp, params, WhereClause);
            
			// create from clause
			String FromClause = getFromClause();

			// rewrite params with join params
			params = rewriteParams(params);

			sql.append(FromClause);
			if (WhereClause.length() > 0) {
				sql.append(" \nWHERE ");
				sql.append(WhereClause);
			}

			// prepare the sql string
			String sqlString = sql.toString();
			// prepare the params array
			Object[] paramsArray = params.toArray();

			try {
				if (isCacheEnabled() && queryResultsCache != null) {
					// caching is enabled
					Object cacheKey = getCacheKey(sqlString, paramsArray, -1, -1);
					Integer cachedResult = null;

					if (cacheKey != null) {
						try {
							cachedResult = (Integer) queryResultsCache.getFromGroup(toString(), cacheKey);
						} catch (Exception e) {
							logger.warn("Error while getting cached query result", e);
						}
					}

					if (cachedResult != null) {
						// we found a cached result, now use it
						count = cachedResult.intValue();
					} else {
						// no cached result found, do the query and cache the
						// result
						DB.query(getHandle().getDBHandle(), sqlString, paramsArray, srp);
						count = srp.getRow(1).getInt("c");
						try {
							queryResultsCache.putIntoGroup(toString(), cacheKey, new Integer(count));
						} catch (PortalCacheException e) {
							logger.warn("Error while caching query results", e);
						}
					}
				} else {
					// caching is disabled
					DB.query(getHandle().getDBHandle(), sqlString, paramsArray, srp);
					count = srp.getRow(1).getInt("c");
				}
			} catch (Exception e) {
				throw new DatasourceNotAvailableException("Error while counting results", e);
			}

			// reset the current timestamp
			currentTimestamp = null;

			return count;
		} catch (CMSUnavailableException e) {
			throw new DatasourceNotAvailableException("", e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_GETCOUNT, ruleTree.getRuleString());
		}
	}

	/**
	 * returns a String representing the SQL operator for the given OperatorType
	 * @param operatorType the operator type
	 * @return the SQL Operator String
	 */
	private String returnSQLOperator(int operatorType) {
		String ret = "";

		switch (operatorType) {
		case CompareOperator.TYPE_EQ:
			ret = "=";
			break;

		case CompareOperator.TYPE_NEQ:
			ret = "<>";
			break;

		case CompareOperator.TYPE_GT:
			ret = ">";
			break;

		case CompareOperator.TYPE_LT:
			ret = "<";
			break;

		case CompareOperator.TYPE_GTEQ:
			ret = ">=";
			break;

		case CompareOperator.TYPE_LTEQ:
			ret = "<=";
			break;

		case CompareOperator.TYPE_ISNULL:
			ret = " IS NULL ";
			break;

		case CompareOperator.TYPE_ISNOTNULL:
			ret = " IS NOT NULL ";
			break;

		case CompareOperator.TYPE_LIKE:
			ret = " LIKE ";
			break;

		case CompareOperator.TYPE_NOTLIKE:
			ret = " NOT LIKE ";
			break;

		}
		return ret;
	}

	/**
	 * returns a String representing the negated SQL operator for the given
	 * OperatorType
	 * @param operatorType the operator type
	 * @return the SQL Operator String
	 */
	private String returnSQLOperatorReversed(int operatorType) {
		String ret = "";

		switch (operatorType) {
		case CompareOperator.TYPE_EQ:
			ret = "=";
			break;

		case CompareOperator.TYPE_NEQ:
			ret = "<>";
			break;

		case CompareOperator.TYPE_GT:
			ret = "<=";
			break;

		case CompareOperator.TYPE_LT:
			ret = ">=";
			break;

		case CompareOperator.TYPE_GTEQ:
			ret = "<";
			break;

		case CompareOperator.TYPE_LTEQ:
			ret = ">";
			break;

		/* !MOD 20040824 DG added handling of is NULL and is NOT NULL */
		case CompareOperator.TYPE_ISNOTNULL:
			ret = " IS NULL ";
			break;

		case CompareOperator.TYPE_ISNULL:
			ret = " IS NOT NULL ";
			break;

		case CompareOperator.TYPE_LIKE:
			ret = " NOT LIKE ";
			break;

		case CompareOperator.TYPE_NOTLIKE:
			ret = " LIKE ";
		}
		return ret;
	}

	/* EXECUTION FUNCTION AREA END */

	/* CONDITION CREATION AREA */
	
	/**
	 * The Main Method for appending a condition to the created SQL Statement.
	 * The RuleToCNCondition which converts a given Rule into a CN SQL Statement
	 * calls this method to create the necessary joins and conditions for usage
	 * of the attributes
	 * @param c The Condition to be appended to the ConditionString
	 * @param ConditionString the ConditionString contains the WHERE condition
	 *        representation
	 * @param params _
	 * @param operator the operator to connect the current condition with the
	 *        rest of the condition string
	 * @throws UnsupportedOperationException this exception is thrown if the
	 *         datasource does not accept the operation given
	 */
	private void appendCondition(Condition c, StringBuffer ConditionString, ArrayList params,
			LogicalOperator operator) throws UnsupportedOperationException {
		// first check if this is not a multivalue conditioncheck
		if (c == null) {
			return;
		}
		CompareOperator CurOperator = c.getOperator();
		Operand left = c.getLeftOperand();
		Operand right = c.getRightOperand();
		int OperatorType = CurOperator.getType();

		/* !MOD 20041217 added rewriting of function operand */
		boolean modified = false;

		if (left instanceof FunctionOperand) {
			// rewrite this function operand into a static operand???
			left = rewriteFunctionOperand((FunctionOperand) left);
			modified = true;
		}
		if (right instanceof FunctionOperand) {
			right = rewriteFunctionOperand((FunctionOperand) right);
			modified = true;
		}
		if (modified) {
			c = new Condition(left, right, c.getOperator());
		}

		/* !MOD 20041217 */

		// check operator types
		if (isMultiValue(OperatorType)) {
			// multi value comparison - join anyway and create join constraints
			appendMultiValue(ConditionString, params, c, operator);
		} else {

			/* !MOD 20041217 DG added function operand validation */
			boolean bothObjectOperands = false;
			ObjectOperand leftObjectOperand = null;
			ObjectOperand rightObjectOperand = null;

			// check operands
			if (left instanceof ObjectOperand) {
				leftObjectOperand = (ObjectOperand) left;
			}

			if (right instanceof ObjectOperand) {
				rightObjectOperand = (ObjectOperand) right;
			}
			// if none of the operands is an object operand - throw exception
			boolean isStaticComparison = false;

			if (leftObjectOperand == null && rightObjectOperand == null) {

				/* !MOD 20041209 DG added static comparisions */
				isStaticComparison = true;
				// throw new UnsupportedOperationException("This datasource does
				// not yet support inter-static comparison");
			}

			if (leftObjectOperand != null && rightObjectOperand != null) {
				bothObjectOperands = true;
			}
			// single value comparison

			// check if both are objects
			if (bothObjectOperands) {
				String LeftOptimizedName = getOptimizedColName(leftObjectOperand.getValue());
				String RightOptimizedName = getOptimizedColName(rightObjectOperand.getValue());

				if ((LeftOptimizedName != null) && (RightOptimizedName != null)) {
					// check if both are optimized fields
					// call method to add simple value comparison with optimized
					// operands
					appendFullOptimizedDynamicCondition(ConditionString, LeftOptimizedName, RightOptimizedName, operator, OperatorType);
				} else if ((LeftOptimizedName != null) || (RightOptimizedName != null)) {
					String OptimizedName = (LeftOptimizedName != null) ? LeftOptimizedName : RightOptimizedName;
					ObjectOperand NotOptimizedOperand = (LeftOptimizedName == null) ? leftObjectOperand : rightObjectOperand;

					// if one is an optimized field
					// call method to add simple value comparison with one
					// optimized operand
					appendHalfOptimizedDynamicCondition(ConditionString, params, OptimizedName, NotOptimizedOperand, operator, OperatorType);
				} else {
					// if none is an optimized field
					// call method to add simple value comparison with no
					// optimized operand
					appendNotOptimizedDynamicCondition(ConditionString, params, leftObjectOperand, rightObjectOperand, operator, OperatorType);
				}
			} else if (isStaticComparison) {
				// This is a static comparison
				appendStaticComparisonCondition(ConditionString, params, c, operator);
			} else {
				// This is neither a optimized nor a static comparison
				ObjectOperand UsedOperand = leftObjectOperand != null ? leftObjectOperand : rightObjectOperand;
				Operand StaticOperand = (leftObjectOperand == null) ? left : right;
				// check if the object operand field is an optimized value field
				String OptimizedFieldName = getOptimizedColName(UsedOperand.getValue());

				if (OptimizedFieldName != null) {
					// if yes - simply add to where clause
					// appendOptimizedSimpleCondition();
					appendOperator(ConditionString, operator);
					appendOptimizedStaticPartCondition(ConditionString, params, OperatorType, OptimizedFieldName, StaticOperand.getValue());
				} else {
					// if no -- add join and value addition
					appendOperator(ConditionString, operator);
					appendNotOptimizedStaticPartCondition(ConditionString, params, OperatorType, UsedOperand.getValue(), StaticOperand.getValue());
					// appendNotOptimized();
				}
			}
		}
	}

	/**
	 * write the given FunctionOperand to an Operand that is resolvable by the
	 * datasource
	 * @param op given FunctionOperand
	 * @return Operand compatible with the datasource
	 * @throws UnsupportedOperationException when the given FunctionOperand
	 *         cannot be rewritten
	 */
	private Operand rewriteFunctionOperand(FunctionOperand op) throws UnsupportedOperationException {
		Operand ret = null;
		// check for the type of this function
		Function function = op.getFunction();

		if (function instanceof ConcatFunction) {
			StringBuffer buffer = new StringBuffer(256);
			// go through all paramters
			Vector params = op.getParams();

			if (params.size() > 1) {
				// set together the operand
				boolean first = true;

				buffer.append("CONCAT(");
				Iterator it = params.iterator();

				while (it.hasNext()) {
					if (!first) {
						buffer.append(",");
					}
					Object ob = it.next();

					if (ob instanceof Operand) {
						Operand oper = (Operand) ob;

						if (oper instanceof ObjectOperand) {
							ObjectOperand objectOperand = (ObjectOperand) oper;
							// now we need to find out if this object operand is
							// a optimized column
							String OptColName = getOptimizedColName(objectOperand.getValue());

							if (OptColName == null) {
								// not optimized
								// getAlias();
								String AliasTable = getNewTableName(objectOperand.getValue());

								buffer.append(AliasTable);
								buffer.append(".");
								buffer.append(getDataTypeColumn(objectOperand.getValue()));
							} else {
								// optimized
								buffer.append("cm.");
								buffer.append(OptColName);
							}
						} else {
							// use getValue
							buffer.append("'" + oper.getValue() + "'");
						}
					} else {
						// treat this is a string - should not happen
						buffer.append("'" + ob.toString() + "'");
					}
					first = false;
				}
				buffer.append(")");
			} else {// add only the normal operand
				// not possible
				// @todo: check if not possible
			}
			ret = new FunctionAsStringOperand(buffer.toString());
		} else if (function instanceof SubruleFunction) {
			// rewrite the subrule function to a property operator that resolves
			// a subrule
			Vector functionParameters = op.getParams();

			if (functionParameters.size() >= 2) {
				try {
					// get the subrule string and replace all references to o.
					// with references to object.
					String subruleString = functionParameters.get(1).toString().replaceAll("(^|[^a-zA-Z\\.0-9_\"'])subobject\\.", "$1object.");

					NodeLogger.getLogger(getClass()).debug("creation subrule " + subruleString);
					// create a new ruletree by cloning the original one
					RuleTree subruleTree = (RuleTree) ruleTree.clone();

					ret = new PropertyOperand(functionParameters.get(0).toString(),
							new PropertyResolver(new SubRuleResolver(subruleTree, (Datasource) this.clone(), subruleString)), functionParameters.get(0).toString());
				} catch (Exception e) {
					throw new UnsupportedOperationException("SubruleFunction not supported in CNDatasource", e);
				}
			} else {
				throw new UnsupportedOperationException("SubruleFunction with less than 2 parameters not supported in CNDatasource");
			}
		}
		return ret;
	}

	/**
	 * appends a multivalue comparison "CONTAINS"
	 * @param ConditionString the conditionstring to append to
	 * @param params
	 * @deprecated the parameters
	 * @param c the condition
	 * @param operator the operator to connect this multivalue addition to the
	 *        constraing
	 * @throws UnsupportedOperationException not supported -> thrown if this
	 *         operator is not supported
	 */
	private void appendMultiValueContains(StringBuffer ConditionString, ArrayList params,
			Condition c, LogicalOperator operator) throws UnsupportedOperationException {
		// REMOVED DG 20040819
		Operand left = c.getLeftOperand();
		Operand right = c.getRightOperand();
		ObjectOperand leftObjectOperand = null;
		ObjectOperand rightObjectOperand = null;

		// check operands
		if (left instanceof ObjectOperand) {
			leftObjectOperand = (ObjectOperand) left;
		}
		if (right instanceof ObjectOperand) {
			rightObjectOperand = (ObjectOperand) right;
		}
		// three possibilities
		if ((leftObjectOperand != null) && (rightObjectOperand != null)) {
			throw new UnsupportedOperationException("CONTAINS is not implemented for 2 Object-Operands!");
			// 1) both object operands
			// NOT IMPLEMENTED

		}
		if ((leftObjectOperand == null) && (rightObjectOperand != null)) {
			// 2) left static / right object operand
			// solution for this is an replacement with
			// left_static = right_object_operand

			// get static value
			String Value = left.getValue();

			// check if the right object operand is optimized
			String OptimizedName = getOptimizedColName(rightObjectOperand.getValue());

			if (OptimizedName != null) {
				// optimized
				appendOperator(ConditionString, operator);
				appendOptimizedStaticPartCondition(ConditionString, params, CompareOperator.TYPE_EQ, OptimizedName, Value);
			} else {
				// not optimized
				appendOperator(ConditionString, operator);
				appendNotOptimizedStaticPartCondition(ConditionString, params, CompareOperator.TYPE_EQ, rightObjectOperand.getValue(), Value);
			}
		}
		if ((leftObjectOperand != null) && (rightObjectOperand == null)) {
			// 3) left object operand / right static
			// create a whole bunch of or statements for static objects
			String[] Values = right.getValues();

			// only if Values.length bigger than 0
			if ((Values != null) && (Values.length > 0)) {
				boolean first = true;
				String OptimizedName = getOptimizedColName(leftObjectOperand.getValue());

				appendOperator(ConditionString, operator);
				ConditionString.append("(");
				// check if object operand is an optimized value
				if (OptimizedName != null) {
					// optimized .. simply add to conditions with or statements
					// add a new condition with the operator

					for (int i = 0; i < Values.length; i++) {
						if (!first) {
							ConditionString.append(" OR ");
						}
						ConditionString.append("(");
						ConditionString.append("cm.");
						ConditionString.append(OptimizedName);
						ConditionString.append(returnSQLOperator(CompareOperator.TYPE_EQ));
						// ConditionString.append(Values[i]);
						ConditionString.append(" ? ");
						params.add(Values[i]);
						ConditionString.append(")");
						// this is not first anymore
						first = false;
					}
				} else {
					// not optimized ... join values and add to conditions
					// first join
					// join the notoptimized operator
					String Alias = getNewTableName(leftObjectOperand.getValue());

					// second add constraint additions
					for (int i = 0; i < Values.length; i++) {
						if (!first) {
							ConditionString.append(" OR ");
						}

						// add condition
						ConditionString.append(Alias);
						ConditionString.append(".");
						ConditionString.append(getDataTypeColumn(leftObjectOperand.getValue()));
						ConditionString.append(returnSQLOperator(CompareOperator.TYPE_EQ));

						// ConditionString.append(Values[i]);
						ConditionString.append(" ? ");
						params.add(Values[i]);
						first = false;
					}
				}

				ConditionString.append(")");
			} else {
				// NOP: when no values are given to the right of CONTAINS, the
				// condition is forced to be "false"
				appendOperator(ConditionString, operator);
				ConditionString.append("1 ").append(returnSQLOperator(CompareOperator.TYPE_EQ)).append(" 2");
			}
		}
		if (leftObjectOperand == null && rightObjectOperand == null) {
			// left and right operands are both static -> do the comparison
			// immediately
			String[] leftValues = left.getValues();
			String[] rightValues = right.getValues();

			// do the static evaluation and add something true or false to the
			// condition string, depending on the static result
			appendOperator(ConditionString, operator);
			ConditionString.append("1 ").append(returnSQLOperator(CompareOperator.TYPE_EQ));
			if (evalStaticContains(leftValues, rightValues)) {
				ConditionString.append(" 1");
			} else {
				ConditionString.append(" 2");
			}
		}
	}

	/**
	 * do a static "contains" evaluation
	 * @param left values on the left of the operator
	 * @param right values on the right of the operator
	 * @return true when the left values contain at least one of the values on
	 *         the right, false if not
	 */
	private boolean evalStaticContains(String[] left, String[] right) {
		// loop throught the right values and try to find one that is contained
		// on the left side
		for (int i = 0; i < right.length; i++) {
			for (int j = 0; j < left.length; j++) {
				if (right[i].equals(left[j])) {
					// we found one on the right that is contained on the left
					// -> evals to true
					return true;
				}
			}
		}

		// no match found -> evals to false
		return false;
	}

	/**
	 * appends a multivalue comparison NOT "CONTAINS"
	 * @param ConditionString the conditionstring to append to
	 * @param params
	 * @deprecated the parameters
	 * @param c the condition
	 * @param operator the operator to connect this multivalue addition to the
	 *        constraing
	 * @throws UnsupportedOperationException not supported -> thrown if this
	 *         operator is not supported
	 */
	private void appendMultiValueContainsNOT(StringBuffer ConditionString, ArrayList params,
			Condition c, LogicalOperator operator) throws UnsupportedOperationException {
		// REMOVED DG 20040819
		Operand left = c.getLeftOperand();
		Operand right = c.getRightOperand();
		ObjectOperand leftObjectOperand = null;
		ObjectOperand rightObjectOperand = null;

		// check operands
		if (left instanceof ObjectOperand) {
			leftObjectOperand = (ObjectOperand) left;
		}
		if (right instanceof ObjectOperand) {
			rightObjectOperand = (ObjectOperand) right;
		}

		// three oportunities
		if ((leftObjectOperand != null) && (rightObjectOperand != null)) {
			// if both are Object Operands
			// throw not supported yet exception
			throw new UnsupportedOperationException("CONTAINSNOT is not implemented for 2 Object-Operands!");
		} else if ((leftObjectOperand == null) && (rightObjectOperand != null)) {
			// left is static - right = ObjectOperand
			// TODO -- this does not seem to be correct ...
			// get static value
			String Value = left.getValue();

			// check if the right object operand is optimized
			String OptimizedName = getOptimizedColName(rightObjectOperand.getValue());

			if (OptimizedName != null) {
				// optimized
				appendOperator(ConditionString, operator);
				appendOptimizedStaticPartCondition(ConditionString, params, CompareOperator.TYPE_NEQ, OptimizedName, Value);
			} else {
				// not optimized
				appendOperator(ConditionString, operator);
				appendNotOptimizedStaticPartCondition(ConditionString, params, CompareOperator.TYPE_NEQ, rightObjectOperand.getValue(), Value);
			}
		} else if ((leftObjectOperand != null) && (rightObjectOperand == null)) {
			// left is object operator and right = static
			// create a whole bunch of AND statements for static objects
			String[] Values = right.getValues();

			// only if Values.length bigger than 0
			if ((Values != null) && (Values.length > 0)) {
				boolean first = true;
				String OptimizedName = getOptimizedColName(leftObjectOperand.getValue());

				appendOperator(ConditionString, operator);
				ConditionString.append("(");
				// check if object operand is an optimized value
				if (OptimizedName != null) {
					// optimized .. simply add to conditions with or statements
					// add a new condition with the operator
					for (int i = 0; i < Values.length; i++) {
						if (!first) {
							ConditionString.append(" AND ");
						}
						ConditionString.append("(");
						ConditionString.append("cm.");
						ConditionString.append(OptimizedName);
						ConditionString.append(" ");
						ConditionString.append(returnSQLOperator(CompareOperator.TYPE_NEQ));
						ConditionString.append(" ? ");
						params.add(Values[i]);
						ConditionString.append(")");
						// this is not first anymore
						first = false;
					}
				} else {
					// not optimized ... join values and add to conditions
					// first join
					// join the notoptimized operator
					String Alias = getNewTableName(leftObjectOperand.getValue());

					// second add constraint additions
					for (int i = 0; i < Values.length; i++) {
						if (!first) {
							ConditionString.append(" AND ");
						}

						// add condition
						ConditionString.append(Alias);
						ConditionString.append(".");
						ConditionString.append(getDataTypeColumn(leftObjectOperand.getValue()));
						ConditionString.append(" ");
						ConditionString.append(returnSQLOperator(CompareOperator.TYPE_NEQ));
						ConditionString.append(" ? ");
						params.add(Values[i]);
						first = false;
					}
				}
				ConditionString.append(")");
			} else {
				// force condition to be true
				appendOperator(ConditionString, operator);
				ConditionString.append(" 1 = 1 ");
			}
		} else if (leftObjectOperand == null && rightObjectOperand == null) {
			// left and right operands are both static -> do the comparison
			// immediately
			String[] leftValues = left.getValues();
			String[] rightValues = right.getValues();

			// do the static evaluation and add something true or false to the
			// condition string, depending on the static result
			appendOperator(ConditionString, operator);
			ConditionString.append("1 ").append(returnSQLOperator(CompareOperator.TYPE_EQ));
			if (!evalStaticContains(leftValues, rightValues)) {
				ConditionString.append(" 1");
			} else {
				ConditionString.append(" 2");
			}
		}
	}

	/**
	 * appends a multivalue comparison, currently only contains supported MOD
	 * 20041119 DG refactored to switch statement
	 * @param ConditionString the conditionstring to append to
	 * @param params
	 * @deprecated the parameters
	 * @param c the condition
	 * @param operator the operator to connect this multivalue addition to the
	 *        constraing
	 * @throws UnsupportedOperationException not supported -> thrown if this
	 *         operator is not supported
	 */
	private void appendMultiValue(StringBuffer ConditionString, ArrayList params, Condition c,
			LogicalOperator operator) throws UnsupportedOperationException {
		// TODO Implementation
		int type = c.getOperator().getType();

		switch (type) {
		case CompareOperator.TYPE_CONTAINS: {
			// check if left operator
			appendMultiValueContains(ConditionString, params, c, operator);
			// CONTAINS (INNER JOIN)
			break;
		}

		case CompareOperator.TYPE_NOTCONTAINS: {
			// NOT CONTAINS (LEFT OUTER JOIN)
			appendMultiValueContainsNOT(ConditionString, params, c, operator);
			break;
		} /* !MOD 20041119 DG added ISEMPTY CONSTRAINT and refactored */
		}
	}

	/*
	 * private void appendSingleObjectOperator(StringBuffer ConditionString,
	 * SingleObjectCondition c, LogicalOperator operator) throws
	 * UnsupportedOperationException { switch(c.getOperator().getType() ){ case
	 * SingleObjectOperator.TYPE_ISEMPTY:{ // @todo implement ISEMTPY break; }
	 * case SingleObjectOperator.TYPE_NOTISEMPTY:{ // @todo implement NOTISEMTP
	 * break; } } }
	 */

	/**
	 * Append a Full optimized condition. This means that both columns of the
	 * condition are optimized column fields. this is a very effective
	 * comparison in db terms
	 * @param ConditionString the condition to append to
	 * @param left left column - already the optimized column
	 * @param right right column - already the optimized column
	 * @param operator logical operator to connect this condition to the rest of
	 *        the clause
	 * @param OperatorType the operator used within the condition
	 */
	private void appendFullOptimizedDynamicCondition(StringBuffer ConditionString, String left,
			String right, LogicalOperator operator, int OperatorType) {
		// simply add to conditionstring
		appendOperator(ConditionString, operator);
		ConditionString.append("cm.");
		ConditionString.append(left);
		ConditionString.append(returnSQLOperator(OperatorType));
		ConditionString.append("cm.");
		ConditionString.append(right);
	}

	private void appendStaticComparisonCondition(StringBuffer ConditionString,
			ArrayList params, Condition condition, LogicalOperator operator) {
		appendOperator(ConditionString, operator);
		Operand left = condition.getLeftOperand();
		Operand right = condition.getRightOperand();

		// NOP: static values are quoted (they may be strings and unquoted the
		// database will complain)
		// TODO: actually, to avoid this hardcoded quoting (and encoding the
		// quote character in the strings), this should be implemented with
		// preparedStatement
		// (inserting ? here and setting the values later with
		// PreparedStatement.setXXX(), see DB.query())

		// ConditionString.append(" ? ");
		// params.add(left.getValue());

		if (left instanceof FunctionAsStringOperand) {
			ConditionString.append(left.getValue());
		} else {
			ConditionString.append("'");
			ConditionString.append(left.getValue().replaceAll("'", "''"));
			ConditionString.append("'");
			// using bind variables does not always work here, since the
			// database (e.g. hsql) might not understand the type of the bind
			// variables for conditions like '? = ?'
			// ConditionString.append("?");
			// params.add(left.getValue());
		}

		ConditionString.append(returnSQLOperator(condition.getOperator().getType()));

		// ConditionString.append(" ? ");
		// params.add(right.getValue());
		if (right instanceof FunctionAsStringOperand) {
			ConditionString.append(right.getValue());
		} else {
			ConditionString.append("'");
			ConditionString.append(right.getValue().replaceAll("'", "''"));
			ConditionString.append("'");
			// using bind variables does not always work here, since the
			// database (e.g. hsql) might not understand the type of the bind
			// variables for conditions like '? = ?'
			// ConditionString.append("?");
			// params.add(right.getValue());
		}
	}

	/**
	 * This creates a half-optimized constraint. A Half Optimized constraint
	 * contains one optimized column and one not-optimized table column
	 * @param ConditionString the condition to append to
	 * @param params
	 * @deprecated The paramters for this vlaues
	 * @param OptimizedName The name of the optimized column - already the
	 *        optimized name
	 * @param NotOptimizedOperand The not optimized object operand
	 * @param operator logical operator to connect this condition to the rest of
	 *        the clause
	 * @param OperatorType the operator used within the condition
	 */
	private void appendHalfOptimizedDynamicCondition(StringBuffer ConditionString,
			ArrayList params, String OptimizedName, ObjectOperand NotOptimizedOperand,
			LogicalOperator operator, int OperatorType) {
		// join and append
		appendOperator(ConditionString, operator);
		// join the notoptimized operator
		String Alias = getNewTableName(NotOptimizedOperand.getValue());

		// add condition
		ConditionString.append("cm.");
		ConditionString.append(OptimizedName);
		ConditionString.append(returnSQLOperator(OperatorType));
		ConditionString.append(Alias);
		ConditionString.append(".");
		ConditionString.append(getDataTypeColumn(NotOptimizedOperand.getValue()));
		// get type of current value
	}

	/**
	 * appends a full not optimized condition. This describes a condition having
	 * two not optimized columns as parameters
	 * @param ConditionString the conditionstring to append to
	 * @param params
	 * @deprecated parameter
	 * @param LeftOperand Left Operator of this condition - not optimized
	 * @param RightOperand Right Operator of this condition - not optimized
	 * @param operator operator to connect this condition to the conditionstring
	 * @param OperatorType operator type of this condition
	 */
	private void appendNotOptimizedDynamicCondition(StringBuffer ConditionString,
			ArrayList params, ObjectOperand LeftOperand, ObjectOperand RightOperand,
			LogicalOperator operator, int OperatorType) {
		// double join
		// first left operator join
		String AliasLeft = getNewTableName(LeftOperand.getValue());

		// append right operator join
		String AliasRight = getNewTableName(RightOperand.getValue());

		// conditionstring
		appendOperator(ConditionString, operator);
		// build condition
		ConditionString.append("(");
		ConditionString.append(AliasLeft);
		ConditionString.append(getDataTypeColumn(LeftOperand.getValue()));
		// ConditionString.append(".value");
		// operator
		ConditionString.append(returnSQLOperator(OperatorType));
		ConditionString.append(AliasRight);
		ConditionString.append(getDataTypeColumn(RightOperand.getValue()));
		// ConditionString.append(".value");
		ConditionString.append(")");
	}

	/**
	 * appends a condition containing one static variable and one column which
	 * is not optimized
	 * @param ConditionString condition string to append to
	 * @param params
	 * @deprecated parmaters
	 * @param operatorType operator type
	 * @param columnName not optimized column name
	 * @param value static value
	 */
	private void appendNotOptimizedStaticPartCondition(StringBuffer ConditionString,
			ArrayList params, int operatorType, String columnName, String value) {
		// create new table name
		String Alias = getNewTableName(columnName);

		// append join query

		ConditionString.append("(");
		ConditionString.append(Alias);
		ConditionString.append(".");
		// get data type
		ConditionString.append(getDataTypeColumn(columnName));
		// get operator
		ConditionString.append(returnSQLOperator(operatorType));
		if ((operatorType == CompareOperator.TYPE_ISNOTNULL) || (operatorType == CompareOperator.TYPE_ISNULL)) {
			// NOP: do not add a value when the condition is "IS NULL" or "IS
			// NOT NULL"
			// ConditionString.append( value );
			ConditionString.append(" ");
		} else if ((operatorType == CompareOperator.TYPE_LIKE) || (operatorType == CompareOperator.TYPE_NOTLIKE)) {
			// rewrite * to %
			value = value.replace('*', '%');
			ConditionString.append("?");
			params.add(value);
		} else {
			ConditionString.append(" ? ");
			params.add(value);

			/* !MOD 20040915 DG */
			if (value.equals("")) {
				// add not null query
				switch (operatorType) {
				case CompareOperator.TYPE_EQ:
				case CompareOperator.TYPE_GT:
				case CompareOperator.TYPE_GTEQ:
				case CompareOperator.TYPE_LT:
				case CompareOperator.TYPE_LTEQ:
					ConditionString.append(LogicalOperator.OPERATOR_OR.toString());

					ConditionString.append(Alias);
					ConditionString.append(".");
					ConditionString.append(getDataTypeColumn(columnName));

					ConditionString.append(returnSQLOperator(CompareOperator.TYPE_ISNULL));
					break;

				case CompareOperator.TYPE_NEQ:
					ConditionString.append(LogicalOperator.OPERATOR_AND.toString());

					ConditionString.append(Alias);
					ConditionString.append(".");
					ConditionString.append(getDataTypeColumn(columnName));

					ConditionString.append(returnSQLOperator(CompareOperator.TYPE_ISNOTNULL));
					break;
				}
			}
		}

		ConditionString.append(")\n");
	}

	/**
	 * appends a condition consisting of one optimized column and a static
	 * value.
	 * @param ConditionString the condition where this constraint has to be
	 *        appended
	 * @param params
	 * @deprecated parameter
	 * @param operatorType the operator type of this condition
	 * @param columnName the column name of the optimized column
	 * @param value the static value
	 */
	private void appendOptimizedStaticPartCondition(StringBuffer ConditionString,
			ArrayList params, int operatorType, String columnName, String value) {
		boolean isEmpty = false;

		if (value.equals("")) {
			isEmpty = true;
			ConditionString.append(" ( ");
		}
		ConditionString.append("cm.");
		ConditionString.append(columnName);
		ConditionString.append(returnSQLOperator(operatorType));
		if ((operatorType == CompareOperator.TYPE_ISNOTNULL) || (operatorType == CompareOperator.TYPE_ISNULL)) {
			ConditionString.append(value);
			ConditionString.append(" ");
			// params.add(value);
		} else {
			ConditionString.append(" ? ");
			// when the operator is "LIKE", the value has to be rewritten to a
			// LIKE-operand for databases (wildcard * has to be replaced by %)
			if (operatorType == CompareOperator.TYPE_LIKE || operatorType == CompareOperator.TYPE_NOTLIKE) {
				params.add(value.replaceAll("\\*", "%"));
			} else {
				params.add(value);
			}
			// check if the conditionstring is empty
			/* !MOD 20040915 */
			if (isEmpty) {
				ConditionString.append(LogicalOperator.OPERATOR_OR.toString());
				ConditionString.append(" cm.");
				ConditionString.append(columnName);
				ConditionString.append(returnSQLOperator(CompareOperator.TYPE_ISNULL));
			}

			/* !MOD 20040915 */
		}
		if (isEmpty) {
			ConditionString.append(" ) ");
		}
	}

	/* CONDITION CREATION AREA END */

	/* ATTRIBUTE AND DATA TYPE AREA */
	
	/**
	 * The Method for getting Data Type of a specified column
	 * @param columnName the name of the column
	 * @return the data type of this column
	 */
	public String getDataTypeColumn(String columnName) {
		String ret = "";

		try {
			ret = DatatypeHelper.getComplexDatatype(getHandle().getDBHandle(), columnName).getColumn();
		} catch (Exception ex) {
			// TODO -> Remove HACK
			if (columnName.indexOf('.') < 0) {
				NodeLogger.getLogger(getClass()).error("missing attribute '" + columnName + "'", ex);
			}
			ret = "value_text";
		}
		return ret;
	}

	/**
	 * Appends the given Operator to the condition string
	 * @param ConditionString condition string
	 * @param operator given operator
	 */
	private void appendOperator(StringBuffer ConditionString, LogicalOperator operator) {
		if (operator != null) {
			ConditionString.append(" ");
			ConditionString.append(operator.toString());
			ConditionString.append(" ");
		}
	}

	/**
	 * Creates the Condition String for the given Rule Tree. The Condition
	 * String is returned. If the Rule Tree contains an embedded Rule Tree it
	 * will recursively call this Method and connect the Rule Tree Condition
	 * Strings.
	 * @param tree The Rule Tree that needs to be transformed
	 * @param params Parameter
	 * @param optimizedSql
	 * @deprecated --> not used anymore. The optimizing of SQL statements is now
	 *             done as proper part and does not need to be done over an SQL
	 *             Buffer
	 * @param optimizedParams
	 * @deprecated --> not used anymore. The optimizing of SQL statements is now
	 *             done as proper part and does not need to be done over an SQL
	 *             Buffer
	 * @return the condition SQL part for this Rule tree and its subtrees
	 */
	private String ruleToCNCondition(RuleTree tree, ArrayList params,
			StringBuffer optimizedSql, ArrayList optimizedParams) {
		StringBuffer ConditionString = new StringBuffer();
		LogicalOperator lastOperator = null;
		// go through tree and get all subtrees
		Iterator it = tree.iterator();

		while (it.hasNext()) {
			// get the current object
			Object o = it.next();

			// if current object is a rule tree -> instantiate tree
			if (o instanceof RuleTree) {
				// call subprocedure
				RuleTree rt = (RuleTree) o;

				if (rt.size() > 0) {
					if (lastOperator != null) {
						ConditionString.append(lastOperator);
					}

					ConditionString.append(" ( ");
					ConditionString.append(ruleToCNCondition(rt, params, optimizedSql, optimizedParams));
					ConditionString.append(" ) ");
				}
			} // else if(o instanceof SingleObjectCondition){
			/* @todo do single object condition appending */ // }
			else if (o instanceof Condition) {
				Condition c = (Condition) o;

				// rewrite condition parts - checks NULL values
				c = rewriteCondition(c);
				appendCondition(c, ConditionString, params, lastOperator); // params,
				// optimizedSql,
				// optimizedParams);
			} else if (o instanceof LogicalOperator) {
				lastOperator = (LogicalOperator) o;
				// LogicalOperator l = (LogicalOperator) o;
				// ConditionString.append(" ");
				// ConditionString.append(o.toString());
				// ConditionString.append(" ");
			} /* !MOD 20041207 DG added FunctionOperands */ else if (o instanceof FunctionOperand) {
				FunctionOperand function = (FunctionOperand) o;

				appendFunctionOperand(function, ConditionString, lastOperator);
			}
		}
		return ConditionString.toString();
	}

	private void appendFunctionOperand(FunctionOperand function, StringBuffer ConditionString,
			LogicalOperator operator) {
		// first version seperates by function
		Function CurFunction = function.getFunction();
		java.util.Vector params = function.getParams();

		if (CurFunction instanceof IsEmptyFunction) {
			// appendIsEmptyFunction((IsEmptyFunction) CurFunction, );
			// IsEmptyFunction has only one parameter
			Object o = params.get(0);

			appendIsEmptyFunction(o, ConditionString, operator);
		} else if (CurFunction instanceof ConcatFunction) {// function.getValue();
		}
	}

	private void appendIsEmptyFunction(Object o, StringBuffer ConditionString,
			LogicalOperator operator) {
		if (o instanceof ObjectOperand) {
			// add an object operand
			ObjectOperand oOperand = (ObjectOperand) o;
			// check if the object operand is optimized -->
			String OptColName = getOptimizedColName(oOperand.getValue());

			appendOperator(ConditionString, operator);

			ConditionString.append("(");
			if (OptColName != null) {
				// optimized column
				ConditionString.append("cm.");
				ConditionString.append(OptColName);
				ConditionString.append(returnSQLOperator(CompareOperator.TYPE_ISNULL));

				appendOperator(ConditionString, LogicalOperator.OPERATOR_OR);
				ConditionString.append("cm.");
				ConditionString.append(OptColName);
				ConditionString.append(returnSQLOperator(CompareOperator.TYPE_EQ));
				ConditionString.append("''");
			} else {
				// not optimized -> get table name
				String AliasTable = getNewTableName(oOperand.getValue());

				ConditionString.append(AliasTable);
				ConditionString.append(".");
				ConditionString.append(getDataTypeColumn(oOperand.getValue()));
				ConditionString.append(returnSQLOperator(CompareOperator.TYPE_ISNULL));

				appendOperator(ConditionString, LogicalOperator.OPERATOR_OR);
				ConditionString.append(AliasTable);
				ConditionString.append(".");
				ConditionString.append(getDataTypeColumn(oOperand.getValue()));
				ConditionString.append(returnSQLOperator(CompareOperator.TYPE_EQ));
				ConditionString.append("''");
			}
			ConditionString.append(")");

			/*
			 * ME-2004-12-12: NO REFERENCES TO PACKAGES THAT ARE DOWN THE DRAIN! }
			 * else if(o instanceof
			 * com.gentics.portalnode.portal.rule.PortalOperand){ //solve portal
			 * operand via portal reference
			 */
		} else if (o instanceof String) {
			// this is a static string
			// resolve purely
			String str = (String) o;

			if ("".equals(str)) {// IS EMPTY
			} else {// IS NOT EMPTY
			}
		}
	}

	/**
	 * Checks if the given operator type is a multi value operation
	 * @param OperatorType the given multi value
	 * @return operator type
	 */
	private boolean isMultiValue(int OperatorType) {
		boolean ret = false;

		if ((OperatorType == CompareOperator.TYPE_CONTAINS) || (OperatorType == CompareOperator.TYPE_NOTCONTAINS)) {
			ret = true;
		}
		return ret;
	}

	/**
	 * This Method creates the final Condition Structures by rewriting the
	 * current conditions to valid constraints (Error checking) and adding
	 * further information for the conditions.
	 * @param c The condition that needs to be revalidated and rewritten
	 * @return the rewritten condition
	 */
	private Condition rewriteCondition(Condition c) {
		// check if the left or the right is an object operand
		Operand left = c.getLeftOperand();
		Operand right = c.getRightOperand();
		boolean leftModify = false;
		boolean rightModify = false;

		if ((!(left instanceof ObjectOperand)) && (left.getValue() == null)) {
			// left is a static operand and the value is null
			leftModify = true;
		}
		if ((!(right instanceof ObjectOperand)) && (right.getValue() == null)) {
			rightModify = true;
		}

		if (leftModify && rightModify) {// if left and right are both null -> validate condition myself
			// if left == right (IF NULL IS NULL) --> true
			// if left <> right (IF NULL IS NOT NULL) --> false
			// if left > right and so on --> false
			// return no condition
			// c = null;
		} else if (leftModify || rightModify) {
			int compareType = c.getOperator().getType();
			String retString = null;

			switch (compareType) {
			case CompareOperator.TYPE_EQ:
				compareType = CompareOperator.TYPE_ISNULL;
				retString = "";
				break;

			case CompareOperator.TYPE_NEQ:
				compareType = CompareOperator.TYPE_ISNOTNULL;
				retString = "";
				break;

			case CompareOperator.TYPE_CONTAINS:
			case CompareOperator.TYPE_NOTCONTAINS:
				// for contains and notcontains, we do not rewrite (will be
				// taken care lateron)
				return c;

			default:

				/* For the whole rest we deliver operator and empty string; */
				retString = "";
				break;
			}

			// ModifyOperand
			// reset the variables
			if (leftModify) {
				if (right instanceof ObjectOperand) {
					left = new com.gentics.lib.parser.rule.StringOperand(retString);
					c = new Condition(right, left, new CompareOperator(compareType));
				} else {
					left = new com.gentics.lib.parser.rule.StringOperand(retString);
					c = new Condition(left, right, c.getOperator());
				}
			} else if (rightModify) {
				if (left instanceof ObjectOperand) {
					right = new com.gentics.lib.parser.rule.StringOperand(retString);
					c = new Condition(left, right, new CompareOperator(compareType));
				} else {
					right = new com.gentics.lib.parser.rule.StringOperand(retString);
					c = new Condition(left, right, c.getOperator());
				}
			}
		}
		return c;
	}

	/**
	 * rewrites the parameters and adds the join parameters that are necessary
	 * to the conditions
	 * @param params parameters
	 * @return ArrayList
	 */
	private ArrayList rewriteParams(ArrayList params) {
		ArrayList oldParams = params;

		params = new ArrayList(oldParams.size() + JoinParams.size());
		Iterator it = JoinParams.iterator();

		while (it.hasNext()) {
			params.add(it.next());
		}

		it = oldParams.iterator();
		while (it.hasNext()) {
			params.add(it.next());
		}
		return params;
	}

	/**
	 * Sets Query Attributes to search for parameter names
	 * @param names the setted query attributes
	 */
	public void setAttributeNames(String names[]) {
		// TODO implement proper functionality ...
		// check with HM or EM
		// Questions: i need to know which tables each attribute belongs to,
		// i need to join the necessary tables right from the beginning
		// i need to adapt the getResult
		// is this mainly correct?

		// first check if all this columns do exists

		// second save the attribute names anyway
		PrefetchColumns = names;
	}

	/**
	 * returns the set attributes that are available
	 * @return attributes
	 */
	public String[] getAttributeNames() {
		return this.PrefetchColumns;
	}

	/* ATTRIBUTE AND DATA TYPE AREA END */

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return new CNDatasource(getId(), handlePool, parameters);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#isVersioning()
	 */
	public boolean isVersioning() {
		return versioning;
	}

	/**
	 * initialize the table versioning objects (if not yet done)
	 * @throws NodeException 
	 */
	protected void initTableVersioning() throws NodeException {
		// when the tableversion objects are not yet created, do this now
		if (contentMapVersion == null) {
			// versioning contentmap
			contentMapVersion = new TableVersion();
			contentMapVersion.setHandle(getHandle().getDBHandle());
			contentMapVersion.setDatasource(this);
			contentMapVersion.setTable(getHandle().getDBHandle().getContentMapName());
			contentMapVersion.setWherePart("gentics_main.contentid = ?");
		}

		if (contentAttributeVersion == null) {
			// versioning for one contentattributes
			contentAttributeVersion = new TableVersion();
			contentAttributeVersion.setHandle(getHandle().getDBHandle());
			contentAttributeVersion.setDatasource(this);
			contentAttributeVersion.setTable(getHandle().getDBHandle().getContentAttributeName());
			contentAttributeVersion.setWherePart("gentics_main.contentid = ? and gentics_main.name = ?");
		}

		if (allContentAttributeVersion == null) {
			// versioning for all contentattributes
			allContentAttributeVersion = new TableVersion();
			allContentAttributeVersion.setHandle(getHandle().getDBHandle());
			allContentAttributeVersion.setDatasource(this);
			allContentAttributeVersion.setTable(getHandle().getDBHandle().getContentAttributeName());
			allContentAttributeVersion.setWherePart("gentics_main.contentid = ?");
		}
	}

	/**
	 * get the lst of available versions for the given contentid
	 * @param id id of the object
	 * @return list of available versions (empty list if no versioning)
	 */
	public Version[] getVersions(String id) {
		if (isVersioning()) {
			Vector collectedVersions = new Vector();

			// TODO: get also the versions of weak foreign link objects
			try {
				initTableVersioning();
				GenticsContentObject object = GenticsContentFactory.createContentObject(id, this);
				List attributeDefinitions = object.getAttributeDefinitions();

				for (Iterator iter = attributeDefinitions.iterator(); iter.hasNext();) {
					GenticsContentAttribute attribute = (GenticsContentAttribute) iter.next();

					if (attribute.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
						SimpleResultProcessor simpleResultProcessor = new SimpleResultProcessor();

						// we have a foreign link attribute -> get all
						// contentid's of objects that belong to this object
						// (independent of time)
						DB.query(getHandle().getDBHandle(),
								"SELECT contentid FROM " + getHandle().getDBHandle().getContentAttributeName()
								+ "_nodeversion WHERE contentid LIKE ? AND name = ? AND "
								+ DatatypeHelper.getComplexDatatype(getHandle().getDBHandle(), attribute.getForeignLinkAttribute()).getColumn()
								+ " = ? GROUP BY contentid",
								new Object[] {
							attribute.getLinkedObjectType() + ".%", attribute.getForeignLinkAttribute(), id},
								simpleResultProcessor);
						for (Iterator iterator = simpleResultProcessor.iterator(); iterator.hasNext();) {
							SimpleResultRow row = (SimpleResultRow) iterator.next();

							// add the versions of the foreign linked object
							collectedVersions.addAll(allContentAttributeVersion.getVersionsList(row.getString("contentid")));
						}
					}
				}

				// add the versions of the object itself
				collectedVersions.addAll(allContentAttributeVersion.getVersionsList(id));

			} catch (Exception ex) {// TODO what to do in case of error
			}

			// sort the versions
			Collections.sort(collectedVersions);
			// merge the versions
			mergeVersions(collectedVersions);
			return (Version[]) collectedVersions.toArray(new Version[collectedVersions.size()]);
		} else {
			return EMPTY_VERSIONLIST;
		}
	}

	/**
	 * merge versions with identical timestamp into one version
	 * @param versions collection of versions
	 */
	protected void mergeVersions(List versions) {
		Version lastVersion = null;

		for (Iterator iter = versions.iterator(); iter.hasNext();) {
			Version currentVersion = (Version) iter.next();

			if (lastVersion != null && currentVersion.getTimestamp() == lastVersion.getTimestamp()) {
				// we have to merge two versions
				lastVersion.setDiffCount(lastVersion.getDiffCount() + currentVersion.getDiffCount());
				iter.remove();
			} else {
				lastVersion = currentVersion;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#setVersionTimestamp(int)
	 */
	public void setVersionTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.VersioningDatasource#checkRequirements()
	 */
	public boolean checkRequirements() {
		// requirements are: subselects must be supported by the database and
		// the _nodeversion tables must exist
		boolean requirementsMet = true;

		try {
			DBHandle dbHandle = getHandle().getDBHandle();
			final String requirementsLogMessage = "versioning requirements not met: ";
			final boolean[] metaDataError =  {false};
			DB.handleDatabaseMetaData(dbHandle, new DatabaseMetaDataHandler() {
				@Override
				public void handleMetaData(DatabaseMetaData metaData) throws SQLException {
					// check for subqueries in comparisons
					if (!metaData.supportsSubqueriesInComparisons()) {
						logger.error(requirementsLogMessage + "database does not support subqueries in comparisons");
						metaDataError[0] = true;
					}

					// check for subqueries in quantified expressions
					if (!metaData.supportsSubqueriesInQuantifieds()) {
						logger.error(requirementsLogMessage + "database does not support subqueries in quantified expressions");
						metaDataError[0] = true;
					}
				}
			});
			if (metaDataError[0]) {
				requirementsMet = false;
			}

			// check whether _nodeversion tables exist and have all required
			// fields
			for (int i = 0; i < requiredVersioningTables.length; i++) {
				if (!DB.tableExists(dbHandle, requiredVersioningTables[i])) {
					logger.error(requirementsLogMessage + "table " + requiredVersioningTables[i] + " does not exist");
					requirementsMet = false;
				} else {
					// check all fields
					for (int j = 0; j < requiredVersioningFields.length; j++) {
						if (!DB.fieldExists(dbHandle, requiredVersioningTables[i], requiredVersioningFields[j])) {
							logger.error(
									requirementsLogMessage + "field " + requiredVersioningFields[j] + " does not exist in table " + requiredVersioningTables[i]);
							requirementsMet = false;
						}
					}
				}
			}

		} catch (Exception ex) {
			logger.error("error while checking versioning requirements: ", ex);
			requirementsMet = false;
		}

		if (!requirementsMet) {
			logger.error("datasource does not meet the requirements for versioning!");
		} else if (logger.isInfoEnabled()) {
			logger.info("datasource meets all requirements for versioning!");
		}

		return requirementsMet;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#hasChanged(long)
	 */
	public boolean hasChanged(long timestamp) {
		if (cacheEnabled && cacheSyncChecking) {
			// Only if sync checking was enabled check last update timestamp
			long lastUpdate = getLastUpdate(false);

			if (lastUpdate == -1) {
				logger.warn("Unable to determine last update timestamp, altough sync checking was enabled.");
				return true;
			}
			// timestamp are milliseconds, while lastUpdate are seconds..
			return lastUpdate * 1000 > timestamp;
		}
		return true;
	}

	/**
	 * Check whether attributetypes may be excluded from versioning
	 * @return true when versioning exclusion can be stored for attributetypes,
	 *         false if not
	 */
	public boolean isAttributeExcludeVersioningColumn() {
		try {
			return DatatypeHelper.isAttributeExcludeVersioningColumn(getHandle().getDBHandle());
		} catch (CMSUnavailableException e) {
			logger.error("Error while checking handle for versioning exclusion");
			return false;
		}
	}

	/**
	 * Check whether objecttypes may be excluded from versioning
	 * @return true when objecttypes may be excluded, false if not
	 */
	public boolean isObjectExcludeVersioningColumn() {
		try {
			return DatatypeHelper.isObjectExcludeVersioningColumn(getHandle().getDBHandle());
		} catch (CMSUnavailableException e) {
			logger.error("Error while checking handle for versioning exclusion");
			return false;
		}
	}

	/**
	 * Check whether the given objecttype will be versioned at all
	 * @param objectType object type to be checked
	 * @return true when it is versioned, false if not
	 */
	public boolean isObjecttypeVersioned(int objectType) {
		try {
			return isVersioning() && !DatatypeHelper.isObjecttypeExcludeVersioning(getHandle().getDBHandle(), objectType);
		} catch (CMSUnavailableException e) {
			logger.error("Error while checking objectType {" + objectType + "} for versioning capabilities.");
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return handlePool.toString();
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
			clearCaches();
		}
		cacheEnabled = enabled;
	}

	/**
	 * Get the cache key for the queryResult or null none can be generated
	 * @param sqlStatement sql statement
	 * @param params array of parameters for the query
	 * @param start start value of the paging
	 * @param count number of records to be fetched on the page
	 * @return cache key object or null
	 */
	protected Object getCacheKey(String sqlStatement, Object[] params, int start, int count) {
		if (sqlStatement == null || params == null) {
			return null;
		}

		// prepare the stringbuffer. the initial size is a rough estimation of
		// the needed size
		StringBuffer cacheKey = new StringBuffer(toString().length() + sqlStatement.length() + params.length * 5 + 6 + 4);

		cacheKey.append(toString()).append("|");
		cacheKey.append(sqlStatement).append("|");
		for (int i = 0; i < params.length; ++i) {
			cacheKey.append(params[i]).append("|");
		}
		cacheKey.append(start).append("|").append(count);

		return cacheKey.toString();
	}

	/**
	 * Perform the given sql query statement with the given params and process
	 * the results with the given result processor
	 * @param sqlString sql statement as string
	 * @param paramsArray array of params for the sql statement
	 * @param resultProcessor result processor for processing the results
	 * @return true when the query was successful, false if it failed
	 */
	protected boolean getDBResult(DBHandle dbHandle, String sqlString, Object[] paramsArray,
			ResultProcessor resultProcessor) throws DatasourceNotAvailableException {
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
	 * Clear the query results cache
	 */
	public void clearQueryCache() {
		if (isCacheEnabled() && queryResultsCache != null) {
			try {
				queryResultsCache.clearGroup(toString());
			} catch (PortalCacheException e) {
				logger.warn("Error while clearing queryResultsCache", e);
			}
		}
	}

	/**
	 * Clear all caches (query, attributes, objects)
	 */
	public void clearCaches() {
		clearQueryCache();
		try {
			GenticsContentFactory.clearCaches(this);
		} catch (PortalCacheException e) {
			logger.warn("Error while clearing object/attribute cache", e);
		}
	}

	/**
	 * Get the timestamp of the last update. The value of the last update should
	 * be stored in the table "contentstatus" as value for the key "lastupdate"
	 * @param forceRenewal true to force reading the value from the DB
	 * @return the timestamp of the last update or -1 if not available
	 */
	public long getLastUpdate(boolean forceRenewal) {
		if (!forceRenewal) {
			Object[] lastUpdate = (Object[]) lastUpdateTimestamp.get(getId());

			if (lastUpdate != null && lastUpdate.length == 2) {
				long mylastRenewal = ((Long) lastUpdate[0]).longValue();

				// usually it should be renewed every 10 secons through sync
				// checking ... if this is not the case we have a fallback ..
				if (System.currentTimeMillis() - mylastRenewal > 30000) {
					String warnMessage = "Last update of datasource was not synced since more than 30 seconds. (Sync checking not active ?) {" + getId() + "}";

					logger.warn(warnMessage, new NodeException(warnMessage));
				} else {
					long lastSyncChecking = ((Long) lastUpdate[1]).longValue();

					if (lastSyncChecking > 0) {
						return lastSyncChecking;
					}
				}
			}
		}
        
		long lastUpdate = -1;
		SimpleResultProcessor resultProcessor = new SimpleResultProcessor();

		try {
			DBHandle handle = getHandle().getDBHandle();

			if (DB.tableExists(handle, handle.getContentStatusName())) {
				DB.query(handle, "select intvalue from " + handle.getContentStatusName() + " where name = 'lastupdate'", null, resultProcessor);
				if (resultProcessor.size() > 0) {
					lastUpdate = ObjectTransformer.getLong(resultProcessor.getRow(1).getObject("intvalue"), -1);
					lastUpdateTimestamp.put(getId(), new Object[] { new Long(System.currentTimeMillis()), new Long(lastUpdate) });
				}
			}
		} catch (Exception e) {
			logger.warn("Error while checking for last update", e);
		}

		return lastUpdate;
	}
    
	/**
	 * same as getLastUpdate(true)
	 */
	public long getLastUpdate() {
		return getLastUpdate(true);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		return createDatasourceFilter(expression, null);
	}

	/**
	 * Create a datasourcefilter sharing the resolvables from the default rule tree
	 * @param expression expression
	 * @param resolvablesMap resolvables map
	 * @return datasource filter
	 * @throws ExpressionParserException
	 */
	protected DatasourceFilter createDatasourceFilter(Expression expression,
			Map resolvablesMap) throws ExpressionParserException {
		if (expression instanceof EvaluableExpression) {
			CNDatasourceFilter filter = null;

			try {
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_CNDATASOURCEFILTER, expression.getExpressionString());
				if (resolvablesMap == null) {
					filter = new CNDatasourceFilter(getHandle().getDBHandle());
				} else {
					filter = new CNDatasourceFilter(getHandle().getDBHandle(), resolvablesMap);
				}
				filter.setExpressionString(expression.getExpressionString());

				((EvaluableExpression) expression).generateFilterPart(
						new ExpressionQueryRequest(filter, this, new PropertyResolver(new MapResolver(resolvablesMap))), filter.getMainFilterPart(),
						ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			} catch (CMSUnavailableException e) {
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
	 * Like {@link #getResult(DatasourceFilter, String[], int, int, com.gentics.api.lib.datasource.Datasource.Sorting[])}
	 * but forces the prefilling of the given prefillAttributes.
	 * 
	 * Prefilling of attributes is only a performance improvement, and as such, if any errors occur during prefilling,
	 * they may be treated as non-fatal. Calling this method, prefilling will be required, and if an error occurs during
	 * prefilling, a DatasourceException will be thrown.
	 */
	public Collection getResultForcePrefill(
			DatasourceFilter filter,
			String[] prefillAttributes,
			int start,
			int count,
			Sorting[] sortedColumns) throws DatasourceException {
		boolean forcePrefill = true;

		return getResult(filter, prefillAttributes, start, count, sortedColumns, null, -1, forcePrefill);
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[],
	 *      java.util.Map, int)
	 */
	public Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters, final int versionTimestamp) throws DatasourceException {
		boolean forcePrefill = false;

		return getResult(filter, prefillAttributes, start, count, sortedColumns, specificParameters, versionTimestamp, forcePrefill);
	}
    
	/**
	 * Like {@link #getResult(DatasourceFilter, String[], int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], Map, int)}
	 * but adds the forcePrefill parameter.
	 * 
	 * @param forcePrefill if true, will require a successful prefill of all prefillAttributes
	 *    and will throw a DatasourceException if prefilling is unsuccessful for any reason.
	 *    If false, prefilling will be attempted (if prefillAttributes are given) and if
	 *    unsuccessful, an error will be logged, but no exception will be thrown.  
	 */
	private Collection<Resolvable> getResult(
			DatasourceFilter filter,
			String[] prefillAttributes,
			int start,
			int count,
			Sorting[] sortedColumns,
			Map specificParameters,
			final int versionTimestamp,
			boolean forcePrefill) throws DatasourceException {
    	
		// check the filter for compatibility
		CNDatasourceFilter datasourceFilter = getAsCNDatasourceFilter(filter);

		// the metaResult holds simply the data from the database as maps
		SimpleResultProcessor metaResult = null;

		// this will be the final result as collection
		List result = null;

		final int lowerLimit = start;
		final int upperLimit = (start + count);

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, datasourceFilter.getExpressionString());

			ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, start, count, sortedColumns, versionTimestamp, filter.getResolver(),
					specificParameters);

			MergedFilter fullStatement = datasourceFilter.getSelectStatement(request);
			String sqlStatement = fullStatement.getStatement().toString();
			List params = fullStatement.getParams();
			Object[] sqlParams = (Object[]) params.toArray(new Object[params.size()]);

			boolean putResultIntoCache = false;
			boolean resultFetchedFromCache = false;
			Object cacheKey = null;

			// check whether result caching is activated
			if (isCacheEnabled() && queryResultsCache != null) {
				// generate the cachekey
				cacheKey = getCacheKey(sqlStatement, sqlParams, start, count);
				SimpleResultProcessor cachedResult = null;

				if (cacheKey != null) {
					try {
						cachedResult = (SimpleResultProcessor) queryResultsCache.getFromGroup(toString(), cacheKey);
					} catch (Exception e) {
						logger.warn("Error while getting cached query result", e);
					}
				}

				if (cachedResult != null) {
					// we found a cached result, now use it
					metaResult = cachedResult;
					resultFetchedFromCache = true;
				} else {
					// no cached result found, do the query and cache the result
					putResultIntoCache = true;
				}
			}

			// when no result was fetched from the cache, perform the sql
			// statement now
			if (!resultFetchedFromCache) {
				metaResult = new SimpleResultProcessor();
				DBHandle dbHandle = getHandle().getDBHandle();

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

				// when no result was found in the cache, we put the current
				// result into the cache now
				if (putResultIntoCache) {
					try {
						queryResultsCache.putIntoGroup(toString(), cacheKey, metaResult);
					} catch (PortalCacheException e) {
						logger.warn("Error while putting query result into cache", e);
					}
				}
			}

			// convert the metaResult in the final results
			result = new Vector(metaResult.size());
			for (Iterator iter = metaResult.iterator(); iter.hasNext();) {
				SimpleResultRow row = (SimpleResultRow) iter.next();
				// create objects from the row, not only the contentid
				GenticsContentObject object = GenticsContentFactory.createContentObject(row, this, versionTimestamp);

				result.add(object);

				// when autoprefill is used, do this now
				if (isAutoPrefetch()) {
					AttributeType[] autoPrefetchedAttributes = getAutoPrefetchedAttributes();

					for (int i = 0; i < autoPrefetchedAttributes.length; i++) {
						object.setPrefetchedAttribute(autoPrefetchedAttributes[i], row.getObject(autoPrefetchedAttributes[i].getQuickName()));
					}
				}
			}

			// prefill objects when attribute names are given and prefetching is
			// on
			if (prefetchAttributes && !ObjectTransformer.isEmpty(prefillAttributes)) {
				try {
					GenticsContentFactory.prefillContentObjects(this, result, prefillAttributes, versionTimestamp);
				} catch (Exception ex) {
					if (forcePrefill) {
						throw ex;
					} else {
						logger.warn("Error while prefetching attributes", ex);
					}
				}
			}

			// do postprocessing
			datasourceFilter.doPostProcessing(result, request);
		} catch (Exception e) {
			throw new DatasourceException(e);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DATASOURCE_CN_GETRESULT, datasourceFilter.getExpressionString());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      java.util.Map, int)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters,
			final int versionTimestamp) throws DatasourceException {
		// check the filter for compatibility
		CNDatasourceFilter datasourceFilter = getAsCNDatasourceFilter(filter);

		// if the datasource filter contains post processors, we need to count by getting the result first
		if (datasourceFilter.hasPostProcessors()) {
			return getResult(datasourceFilter, null, 0, -1, null, specificParameters, versionTimestamp).size();
		}

		// prepare the count result
		int count = 0;

		try {
			// set the begin mark for the profiler
			RuntimeProfiler.beginMark(ComponentsConstants.DATASOURCE_CN_GETCOUNT, filter.getExpressionString());

			ExpressionQueryRequest request = new ExpressionQueryRequest(filter, this, -1, -1, null, versionTimestamp, filter.getResolver(), specificParameters);

			MergedFilter fullStatement = datasourceFilter.getCountStatement(request);
			String sqlStatement = fullStatement.getStatement().toString();
			List params = fullStatement.getParams();
			Object[] sqlParams = (Object[]) params.toArray(new Object[params.size()]);

			// prepare for caching
			Object cacheKey = null;
			boolean resultFetchedFromCache = false;
			boolean putResultIntoCache = false;

			// check whether result caching is activated
			if (isCacheEnabled() && queryResultsCache != null) {
				// generate the cachekey
				cacheKey = getCacheKey(sqlStatement, sqlParams, -1, -1);
				Integer cachedResult = null;

				if (cacheKey != null) {
					try {
						cachedResult = (Integer) queryResultsCache.getFromGroup(toString(), cacheKey);
					} catch (Exception e) {
						logger.warn("Error while getting cached query result", e);
					}
				}

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

				DB.query(getHandle().getDBHandle(), sqlStatement, sqlParams, srp);
				count = srp.getRow(1).getInt("c");

				if (putResultIntoCache) {
					// we have to put the result into the cache
					try {
						queryResultsCache.putIntoGroup(toString(), cacheKey, new Integer(count));
					} catch (PortalCacheException e) {
						logger.warn("Error while putting count result into cache", e);
					}
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

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#create(java.util.Map,
	 *      int)
	 */
	public Changeable create(Map objectParameters, int versionTimestamp) throws DatasourceException {
		// this method is not implemented for a non-writeable class
		throw new DatasourceException("Not implemented");
	}

	/**
	 * Get whether foreign linked attributes shall be cached
	 * @return true when foreign linked attributes shall be cached, false if not
	 */
	public boolean isCacheForeignLinkAttributes() {
		return cacheForeignLinkAttributes;
	}

	/**
	 * Cast the given datasource filter into a CNDatasourceFilter (if possible),
	 * or throw a DatasourceException when the filter is incompatible.
	 * @param filter datasource filter instance
	 * @return instance of a CNDatasourceFilter
	 * @throws DatasourceException when the filter is incompatible
	 */
	protected static CNDatasourceFilter getAsCNDatasourceFilter(DatasourceFilter filter) throws DatasourceException {
		if (!(filter instanceof CNDatasourceFilter)) {
			throw new DatasourceException("Incompatible filter");
		}
		return (CNDatasourceFilter) filter;
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
				prefetchAttributesCacheMissThresholdPerc > 0 ? numberOfAttributes * 100 / prefetchAttributesCacheMissThresholdPerc : 0);
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

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.ReadableDatasource#getObject(java.lang.String,
	 *      java.lang.String[])
	 */
	public SimpleObject getObject(String id, String[] attributeNames) throws DatasourceException {
		try {
			GenticsContentObject contentObject = GenticsContentFactory.createContentObject(id, this);

			if (contentObject != null) {
				// prefill objects when attribute names are given and prefetching is on
				if (prefetchAttributes && !ObjectTransformer.isEmpty(attributeNames)) {
					try {
						GenticsContentFactory.prefillContentObjects(this, new GenticsContentObject[] { contentObject}, attributeNames);
					} catch (Exception ex) {
						logger.warn("Error while prefetching attributes", ex);
					}
				}

				// finally transform the object
				return transformToDSObject(contentObject, attributeNames);
			} else {
				return null;
			}
		} catch (CMSUnavailableException e) {
			throw new DatasourceException("Error while fetching object", e);
		} catch (NodeIllegalArgumentException e) {
			throw new DatasourceException("Error while fetching object", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.ReadableDatasource#getObjects(java.lang.String,
	 *      java.lang.String[], int, int,
	 *      com.gentics.api.lib.datasource.Datasource.Sorting[])
	 */
	public SimpleObject[] getObjects(DatasourceFilter filter, String[] prefillAttributes, int start, int count,
			Sorting[] sortColumns) throws DatasourceException {
		try {
			Collection result = getResult(filter, prefillAttributes, start, count, sortColumns);

			// transform the result (collection of resolvables) into an
			// array of DSObjects that have the given attributes prefilled
			SimpleObject[] objects = new SimpleObject[result.size()];
			int i = 0;

			for (Iterator iter = result.iterator(); iter.hasNext();) {
				GenticsContentObject element = (GenticsContentObject) iter.next();

				objects[i++] = transformToDSObject(element, prefillAttributes);
			}

			return objects;
		} catch (Exception e) {
			throw new DatasourceException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.simple.SimpleDatasource#getObjectsByID(java.lang.String[],
	 *      java.lang.String[], int, int,
	 *      com.gentics.api.lib.datasource.Datasource.Sorting[])
	 */
	public SimpleObject[] getObjectsByID(String[] ids, String[] prefillAttributes, int start,
			int count, Sorting[] sortColumns) throws DatasourceException {
		if (ids == null) {
			return new SimpleObject[0];
		}

		// create a filter to fetch the objects with given contentids
		try {
			Expression expression = ExpressionParser.getInstance().parse("object.contentid CONTAINSONEOF data.ids");
			DatasourceFilter filter = createDatasourceFilter(expression);
			Collection idCollection = new Vector();

			for (int i = 0; i < ids.length; i++) {
				idCollection.add(ids[i]);
			}
			Map dataMap = new HashMap();

			dataMap.put("ids", idCollection);
			filter.addBaseResolvable("data", new MapResolver(dataMap));

			return getObjects(filter, prefillAttributes, start, count, sortColumns);
		} catch (DatasourceException e) {
			throw e;
		} catch (Exception e) {
			throw new DatasourceException(e);
		}
	}

	/**
	 * Transform the given GenticsContentObject into an instance of
	 * {@link SimpleObject}. Include the attributes listed in attributeNames.
	 * @param object GenticsContentObject to transform
	 * @param attributeNames array containing the attribute names to prefetch
	 * @return instance of DSObject
	 * @throws DatasourceException
	 */
	protected static SimpleObject transformToDSObject(GenticsContentObject object,
			String[] attributeNames) throws DatasourceException {
		SimpleAttribute[] attributes = attributeNames != null ? new SimpleAttribute[attributeNames.length] : new SimpleAttribute[0];

		int aCounter = 0;

		for (int i = 0; i < attributes.length; i++) {
			try {
				// check for non-existent attributes
				GenticsContentAttribute attr = object.getAttribute(attributeNames[i]);

				if (attr != null) {
					attributes[aCounter++] = new CNAttribute((attr), object);
				}
			} catch (CMSUnavailableException e) {
				throw new DatasourceException("Error while fetching objects", e);
			} catch (NodeIllegalArgumentException e) {// ignore this
			}
		}

		// when some attributes where ignored, trim the array to the correct length
		if (aCounter != attributes.length) {
			SimpleAttribute[] newAttributes = new SimpleAttribute[aCounter];

			System.arraycopy(attributes, 0, newAttributes, 0, aCounter);
			attributes = newAttributes;
		}

		return new CNObject(object, attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.ReadableDatasource#getAttribute(java.lang.String,
	 *      java.lang.String)
	 */
	public SimpleAttribute getAttribute(String id, String attributeName) throws DatasourceException {
		try {
			GenticsContentObject contentObject = GenticsContentFactory.createContentObject(id, this);

			if (contentObject != null) {
				GenticsContentAttribute attr = contentObject.getAttribute(attributeName);

				if (attr != null) {
					return new CNAttribute(attr, contentObject);
				} else {
					throw new DatasourceException("Attribute {" + attributeName + "} does not exist");
				}
			} else {
				// object does not exist, throw an exception
				throw new DatasourceException("Object with id {" + id + "} does not exist.");
			}
		} catch (CMSUnavailableException e) {
			throw new DatasourceException("Error while fetching attribute {" + attributeName + "} for object {" + id + "}", e);
		} catch (NodeIllegalArgumentException e) {
			throw new DatasourceException("Error while fetching attribute {" + attributeName + "} for object {" + id + "}", e);
		}
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#isValidAttribute(java.lang.String)
	 */
	public boolean isValidAttribute(String attributeName) throws DatasourceException {
		try {
			DatatypeHelper.getDatatype(getHandle().getDBHandle(), attributeName);
			return true;
		} catch (CMSUnavailableException e) {
			throw new DatasourceException("Unable determine if given attribute {" + attributeName + "} exists.", e);
		} catch (NodeIllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * CNDatasource specific implementation of {@link SimpleObject}.
	 * Implementation is a wrapper for an instance of
	 * {@link GenticsContentObject}.
	 */
	public static class CNObject extends ChangeableBean implements SimpleObject {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 6328440328350943287L;

		/**
		 * wrapped GenticsContentObject
		 */
		private GenticsContentObject object;

		/**
		 * prefetched attributes
		 */
		private SimpleAttribute[] attributes;

		/**
		 * Create an instance of the object wrapping the given
		 * GenticsContentObject and with the given prefetched attributes.
		 * @param object wrapped object
		 * @param attributes prefetched attributes
		 */
		public CNObject(GenticsContentObject object, SimpleAttribute[] attributes) {
			this.object = object;
			this.attributes = attributes;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.datasource.DSObject#getId()
		 */
		public String getId() {
			return object.getContentId();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.datasource.DSObject#getAttributes()
		 */
		public SimpleAttribute[] getAttributes() {
			return attributes;
		}
	}

	/**
	 * CNDatasource specific implementation of {@link SimpleAttribute}.
	 * Implementation is a wrapper for an instance of
	 * {@link GenticsContentAttribute}.
	 */
	public static class CNAttribute implements SimpleAttribute {

		/**
		 * Wrapped instance of GenticsContentAttribute
		 */
		private GenticsContentAttribute attribute;

		/**
		 * Parent object
		 */
		private GenticsContentObject parent;

		/**
		 * Create an instance of the attribute
		 * @param attribute wrapped attribute
		 * @param parent parent object
		 */
		public CNAttribute(GenticsContentAttribute attribute, GenticsContentObject parent) {
			this.attribute = attribute;
			this.parent = parent;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.datasource.DSAttribute#getName()
		 */
		public String getName() {
			return attribute.getAttributeName();
		}

		/**
		 * Get the single value of the attribute or null if not set
		 * @return single attribute value
		 */
		private Object getSingleValue() {
			List values = attribute.getValues();

			if (values != null && values.size() > 0) {
				return values.get(0);
			} else {
				return null;
			}
		}

		private boolean isString() {
			int type = attribute.getRealAttributeType();

			return type == GenticsContentAttribute.ATTR_TYPE_TEXT || type == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG;
		}

		private boolean isInteger() {
			return attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_INTEGER;
		}

		private boolean isLong() {
			return attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_LONG;
		}

		private boolean isDouble() {
			return attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_DOUBLE;
		}

		private boolean isDate() {
			return attribute.getRealAttributeType() == GenticsContentAttribute.ATTR_TYPE_DATE;
		}

		private boolean isBinary() {
			int type = attribute.getRealAttributeType();

			return type == GenticsContentAttribute.ATTR_TYPE_BINARY || type == GenticsContentAttribute.ATTR_TYPE_BLOB;
		}

		private boolean isObject() {
			int type = attribute.getRealAttributeType();

			return type == GenticsContentAttribute.ATTR_TYPE_OBJ || type == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.datasource.DSAttribute#getType()
		 */
		public String getType() {
			// the type can be fetched from the attribute
			if (attribute.isMultivalue()) {
				switch (attribute.getRealAttributeType()) {
				case GenticsContentAttribute.ATTR_TYPE_TEXT:
				case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
					return "multiStringValue";

				case GenticsContentAttribute.ATTR_TYPE_OBJ:
				case GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ:
					return "multiObjectValue";

				case GenticsContentAttribute.ATTR_TYPE_BINARY:
				case GenticsContentAttribute.ATTR_TYPE_BLOB:
					return "multiBinaryValue";

				case GenticsContentAttribute.ATTR_TYPE_INTEGER:
					return "multiIntegerValue";

				case GenticsContentAttribute.ATTR_TYPE_LONG:
					return "multiLongValue";

				case GenticsContentAttribute.ATTR_TYPE_DATE:
					return "multiDateValue";

				case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
					return "multiDoubleValue";

				default:
					return "";
				}
			} else {
				switch (attribute.getRealAttributeType()) {
				case GenticsContentAttribute.ATTR_TYPE_TEXT:
				case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
					return "stringValue";

				case GenticsContentAttribute.ATTR_TYPE_OBJ:
				case GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ:
					return "objectValue";

				case GenticsContentAttribute.ATTR_TYPE_BINARY:
				case GenticsContentAttribute.ATTR_TYPE_BLOB:
					return "binaryValue";

				case GenticsContentAttribute.ATTR_TYPE_INTEGER:
					return "integerValue";

				case GenticsContentAttribute.ATTR_TYPE_LONG:
					return "longValue";

				case GenticsContentAttribute.ATTR_TYPE_DATE:
					return "dateValue";

				case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
					return "doubleValue";

				default:
					return "";
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getString()
		 */
		public String getStringValue() {
			if (!attribute.isMultivalue() && isString()) {
				return ObjectTransformer.getString(getSingleValue(), null);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getInteger()
		 */
		public Integer getIntegerValue() {
			if (!attribute.isMultivalue() && isInteger()) {
				return ObjectTransformer.getInteger(getSingleValue(), null);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getLong()
		 */
		public Long getLongValue() {
			if (!attribute.isMultivalue() && isLong()) {
				return ObjectTransformer.getLong(getSingleValue(), null);
			} else {
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getBinary()
		 */
		public byte[] getBinaryValue() {
			if (!attribute.isMultivalue() && isBinary()) {
				return ObjectTransformer.getBinary(getSingleValue(), null);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getDouble()
		 */
		public Double getDoubleValue() {
			if (!attribute.isMultivalue() && isDouble()) {
				return ObjectTransformer.getDouble(getSingleValue(), null);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getDate()
		 */
		public Calendar getDateValue() {
			if (!attribute.isMultivalue() && isDate()) {
				Date date = ObjectTransformer.getDate(getSingleValue(), null);

				if (date != null) {
					Calendar c = Calendar.getInstance();

					c.setTime(date);
					return c;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.simple.SimpleAttribute#getObject()
		 */
		public SimpleObject getObjectValue() {
			if (!attribute.isMultivalue() && isObject()) {
				Object object = getSingleValue();

				if (object instanceof GenticsContentObject) {
					try {
						return transformToDSObject((GenticsContentObject) object, null);
					} catch (DatasourceException e) {
						NodeLogger.getNodeLogger(CNDatasource.class).error("Error while getting object attribute value", e);
						return null;
					}
				} else if (object instanceof String) {
					try {
						return transformToDSObject(
								GenticsContentFactory.createContentObject(object.toString(), parent.getDatasource(), parent.getVersionTimestamp()), null);
					} catch (Exception e) {
						NodeLogger.getNodeLogger(CNDatasource.class).error("Error while getting object attribute value", e);
						return null;
					}
				} else {
					return null;
				}
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiString()
		 */
		public String[] getMultiStringValue() {
			if (attribute.isMultivalue() && isString()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List stringValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					String stringValue = ObjectTransformer.getString(iter.next(), null);

					if (stringValue != null) {
						stringValues.add(stringValue);
					}
				}

				return (String[]) stringValues.toArray(new String[stringValues.size()]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiInteger()
		 */
		public Integer[] getMultiIntegerValue() {
			if (attribute.isMultivalue() && isInteger()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List integerValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					Integer integerValue = ObjectTransformer.getInteger(iter.next(), null);

					if (integerValue != null) {
						integerValues.add(integerValue);
					}
				}

				return (Integer[]) integerValues.toArray(new Integer[integerValues.size()]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiLong()
		 */
		public Long[] getMultiLongValue() {
			if (attribute.isMultivalue() && isLong()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List longValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					Long longValue = ObjectTransformer.getLong(iter.next(), null);

					if (longValue != null) {
						longValues.add(longValue);
					}
				}

				return (Long[]) longValues.toArray(new Long[longValues.size()]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiBinary()
		 */
		public byte[][] getMultiBinaryValue() {
			if (attribute.isMultivalue() && isBinary()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List binaryValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					byte[] binaryValue = ObjectTransformer.getBinary(iter.next(), null);

					if (binaryValue != null) {
						binaryValues.add(binaryValue);
					}
				}

				return (byte[][]) binaryValues.toArray(new byte[binaryValues.size()][]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiDouble()
		 */
		public Double[] getMultiDoubleValue() {
			if (attribute.isMultivalue() && isDouble()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List doubleValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					Double doubleValue = ObjectTransformer.getDouble(iter.next(), null);

					if (doubleValue != null) {
						doubleValues.add(doubleValue);
					}
				}

				return (Double[]) doubleValues.toArray(new Double[doubleValues.size()]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.DSAttribute#getMultiDate()
		 */
		public Calendar[] getMultiDateValue() {
			if (attribute.isMultivalue() && isDate()) {
				List values = attribute.getValues();
				// remove all values that are not strings
				List calendarValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					Date dateValue = ObjectTransformer.getDate(iter.next(), null);

					if (dateValue != null) {
						Calendar cal = Calendar.getInstance();

						cal.setTime(dateValue);
						calendarValues.add(cal);
					}
				}

				return (Calendar[]) calendarValues.toArray(new Calendar[calendarValues.size()]);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.datasource.simple.SimpleAttribute#getMultiObject()
		 */
		public SimpleObject[] getMultiObjectValue() {
			if (attribute.isMultivalue() && isObject()) {
				List values = attribute.getValues();
				// remove all values that are not objects
				List objectValues = new Vector(values.size());

				for (Iterator iter = values.iterator(); iter.hasNext();) {
					Object object = iter.next();

					if (object instanceof GenticsContentObject) {
						try {
							objectValues.add(transformToDSObject((GenticsContentObject) object, null));
						} catch (DatasourceException e) {
							NodeLogger.getNodeLogger(CNDatasource.class).error("Error while getting object attribute value", e);
						}
					} else if (object instanceof String) {
						try {
							objectValues.add(
									transformToDSObject(
											GenticsContentFactory.createContentObject(object.toString(), parent.getDatasource(), parent.getVersionTimestamp()), null));
						} catch (Exception e) {
							NodeLogger.getNodeLogger(CNDatasource.class).error("Error while getting object attribute value", e);
						}
					} else {
						return null;
					}
				}

				return (SimpleObject[]) objectValues.toArray(new SimpleObject[objectValues.size()]);
			} else {
				return null;
			}
		}
	}

	/**
	 * Get preferred concat function or null if no concat function supported
	 * @return name of the preferred concat function or null
	 */
	public String getPreferredConcatFunction() throws CMSUnavailableException {
		return getHandle().getPreferredConcatFunction();
	}

	/**
	 * Get preferred concat operator or null if no concat operator supported
	 * @return name of the preferred concat operator or null
	 */
	public String getPreferredConcatOperator() throws CMSUnavailableException {
		return getHandle().getPreferredConcatOperator();
	}

	/**
	 * Get the datatype name to be used in a CAST(field AS type) operation to
	 * case a field to a text (if CAST is supported anyway)
	 * @return datatype name to CAST to texts, or null if not supported
	 */
	public String getTextCastName() throws CMSUnavailableException {
		return getHandle().getTextCastName();
	}

	/**
	 * Set a new value for the prefetch attributes threshold
	 * @param prefetchAttributesThreshold prefetch attributes threshold
	 */
	public void setPrefetchAttributesThreshold(int prefetchAttributesThreshold) {
		this.prefetchAttributesThreshold = prefetchAttributesThreshold;
	}

	/**
	 * Get whether integers shall be treated as strings
	 * @return true when integers shall be treated as strings, false if not
	 */
	public boolean isTreatIntegerAsString() {
		return integerAsString;
	}

	/**
	 * Get whether illegal links will be fetched as dummy objects instead of null
	 * @return true when illegal links shall be fetched as strings instead of
	 *         null, false if not
	 */
	public boolean isGetIllegalLinksAsDummyObjects() {
		return getIllegalLinksAsDummyObjects;
	}

	/**
	 * Set a contentstatus value
	 * @param name name of the contentstatus value
	 * @param intValue int value of the contentstatus
	 * @throws DatasourceException
	 */
	public void setContentStatus(String name, int intValue) throws DatasourceException {
		String tableName = "contentstatus";
		try {
			DBHandle dbHandle = getHandle().getDBHandle();
			tableName = dbHandle.getContentStatusName();

			if (DB.tableExists(dbHandle, tableName)) {
				SimpleResultProcessor rp = new SimpleResultProcessor();

				DB.query(dbHandle, "SELECT intvalue FROM " + tableName + " WHERE name = ?", new Object[] { name}, rp);
				if (rp.size() > 0) {
					DB.update(dbHandle, "UPDATE " + tableName + " SET intvalue = ? WHERE name = ?", new Object[] { intValue, name });
				} else {
					DB.update(dbHandle, "INSERT INTO " + tableName + " (name, intvalue) VALUES (?, ?)", new Object[] { name, intValue });
				}
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while setting " + tableName + " {" + name + "} to {" + intValue + "}", e);
		}
	}

	/**
	 * Set a contentstatus value
	 * @param name name of the contentstatus value
	 * @param value value
	 * @throws DatasourceException
	 */
	public void setContentStatus(String name, String value) throws DatasourceException {
		String tableName = "contentstatus";
		try {
			DBHandle dbHandle = getHandle().getDBHandle();
			tableName = dbHandle.getContentStatusName();

			if (DB.tableExists(dbHandle, tableName)) {
				SimpleResultProcessor rp = new SimpleResultProcessor();

				DB.query(dbHandle, "SELECT stringvalue FROM " + tableName + " WHERE name = ?", new Object[] { name }, rp);
				if (rp.size() > 0) {
					DB.update(dbHandle, "UPDATE " + tableName + " SET stringvalue = ? WHERE name = ?", new Object[] { value, name });
				} else {
					DB.update(dbHandle, "INSERT INTO " + tableName + " (name, stringvalue) VALUES (?, ?)", new Object[] { name, value });
				}
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while setting " + tableName + " {" + name + "} to {" + value + "}", e);
		}
	}

	/**
	 * Get a string contentstatus value
	 * @param name name of the contentstatus
	 * @return value or null if not set
	 * @throws DatasourceException
	 */
	public String getStringContentStatus(String name) throws DatasourceException {
		String tableName = "contentstatus";
		try {
			SimpleResultProcessor rp = new SimpleResultProcessor();
			DBHandle dbHandle = getHandle().getDBHandle();
			tableName = dbHandle.getContentStatusName();
			if (DB.tableExists(dbHandle, tableName)) {
				DB.query(dbHandle, "SELECT stringvalue FROM " + tableName + " WHERE name = ?", new Object[] { name }, rp);
				if (rp.size() > 0) {
					return rp.getRow(1).getString("stringvalue");
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while getting stringvalue for " + tableName + " {" + name + "}", e);
		}
	}

	/**
	 * Get an integer contentstatus value
	 * @param name name of the integer contentstatus
	 * @return value or -1 if not set
	 * @throws DatasourceException
	 */
	public int getIntContentStatus(String name) throws DatasourceException {
		String tableName = "contentstatus";
		try {
			SimpleResultProcessor rp = new SimpleResultProcessor();
			DBHandle dbHandle = getHandle().getDBHandle();
			tableName = dbHandle.getContentStatusName();
			if (DB.tableExists(dbHandle, tableName)) {
				DB.query(dbHandle, "SELECT intvalue FROM " + tableName + " WHERE name = ?", new Object[] { name }, rp);
				if (rp.size() > 0) {
					return rp.getRow(1).getInt("intvalue");
				} else {
					return -1;
				}
			} else {
				return -1;
			}
		} catch (Exception e) {
			throw new DatasourceException("Error while getting intvalue for " + tableName + " {" + name + "}", e);
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
			return GenticsContentFactory.ATTRIBUTESCACHEREGION;
		} else if (!attributeCacheSettings.containsKey(attributeName)) {
			// no custom cache settings for this attribute: cache in default
			// region
			return GenticsContentFactory.ATTRIBUTESCACHEREGION;
		} else {
			Object setting = attributeCacheSettings.get(attributeName);

			if (setting instanceof Boolean) {
				return ObjectTransformer.getBoolean(setting, Boolean.TRUE).booleanValue() ? GenticsContentFactory.ATTRIBUTESCACHEREGION : null;
			} else {
				return ObjectTransformer.getString(setting, null);
			}
		}
	}

	/**
	 * Get a list holding all custom attribute cache regions
	 * @return list of all custom attribute cache regions or empty list of non
	 *         used
	 */
	public List getCustomCacheRegions() {
		if (attributeCacheSettings == null) {
			return Collections.EMPTY_LIST;
		} else {
			List customCacheRegions = new Vector();

			for (Iterator iterator = attributeCacheSettings.values().iterator(); iterator.hasNext();) {
				Object setting = (Object) iterator.next();

				if (setting instanceof String) {
					customCacheRegions.add(setting);
				}
			}

			return customCacheRegions;
		}
	}

	/**
	 * Check whether auto prefetching is enabled
	 * @return true for enabled auto prefetching, false if disabled
	 */
	public boolean isAutoPrefetch() {
		return autoprefetch;
	}

	/**
	 * Check whether this datasource has differential sync checking enabled
	 * @return true for differential sync checking, false for normal sync checking
	 */
	public boolean isDifferentialSyncChecking() {
		return differentialSyncChecking;
	}

	/**
	 * Get the attributes that will be auto prefetched
	 * @return attributes that will be auto prefetched
	 */
	public AttributeType[] getAutoPrefetchedAttributes() {
		try {
			DBHandle handle = getHandle().getDBHandle();
			return DatatypeHelper.getAttributeTypes(handle, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, null, null, autoPrefetchAttributes);
		} catch (CMSUnavailableException e) {
			return null;
		}
	}

	/**
	 * Get a collection holding all contentid's of objects that were modified since the given timestamp
	 * @param timestamp timestamp to start checking
	 * @return collection of contentid's of modified objects
	 * @throws SQLException
	 * @throws CMSUnavailableException 
	 */
	public Collection getObjectsModifiedSince(long timestamp) throws SQLException, CMSUnavailableException {
		final Collection modifiedContentIds = new Vector();
		DBHandle handle = getHandle().getDBHandle();

		DB.query(handle, "select contentid from " + handle.getContentMapName() + " where updatetimestamp > ?", new Object[] { new Long(timestamp)},
				new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					modifiedContentIds.add(rs.getString("contentid"));
				}
			}

			public void takeOver(ResultProcessor p) {}
		});
		return modifiedContentIds;
	}

	/**
	 * Check whether Cache Warming on startup is enabled
	 * @return true for enabled, false for disabled
	 */
	public boolean isCacheWarmingOnInit() {
		return cacheWarmingOnInit;
	}

	/**
	 * Return a DatasourceFilter used to select objects for cache warming
	 * @return DatasourceFilter
	 */
	public DatasourceFilter getCacheWarmingFilter() {
		
		if (filter == null) {
			
			Expression filterExpression;
			
			try {
				filterExpression = ExpressionParser.getInstance().parse(cacheWarmingFilter);
				filter = createDatasourceFilter(filterExpression);
			} catch (Exception e) {
				logger.error("Error while parsing filter {" + cacheWarmingFilter + "}", e);
			}
		}
		
		return filter;
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
	 * Get the path configured to store attributes
	 * @return the attributePath
	 */
	public String getAttributePath() {
		return attributePath;
	}

	/**
	 * Get the database product name
	 * @return database product name
	 */
	public String getDatabaseProductName() {
		return databaseProductName;
	}
}
