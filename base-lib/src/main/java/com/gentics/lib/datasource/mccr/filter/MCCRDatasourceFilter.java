package com.gentics.lib.datasource.mccr.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.CNDatasourceFilter;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;

/**
 * Implementation of
 * {@link com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter}
 * for the {@link com.gentics.lib.datasource.mccr.MCCRDatasource}.
 */
public class MCCRDatasourceFilter extends CNDatasourceFilter {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8631216416084559973L;

	/**
	 * Meta columns of the table contentmap
	 */
	protected final static String[] META_COLUMNS = { "id", "channel_id", "channelset_id", "obj_id", "obj_type", "updatetimestamp"};

	/**
	 * Supported datasource classes
	 */
	public final static Class<?>[] MCCRFILTER_FUNCTION = new Class[] { MCCRDatasource.class};

	/**
	 * Placeholder for the channel id in the list of parameters
	 */
	public final static Object CHANNEL_ID = new Object();

	/**
	 * Template for foreign attribute join with an optimized attribute.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>quickColumn</li> Name of the quick column of the optimized attribute</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentmap cm2 on (cm2.quick_link = cm1.contentid AND cm2.channel_id IN (?, ?))</code>
	 */
	protected final static String foreignOptimizedJoinTemplate = "left join ${contentmap} ${cmAlias} on (${cmAlias}.${quickColumn} = ${mainAlias}.contentid AND ${cmAlias}.channel_id IN (${channelIds}))";

	/**
	 * Template for foreign attribute join with a normal attribute.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentattribute ca2 on (ca2.value_text = cm1.contentid AND ca2.name = ?) left join contentmap cm2 on (ca2.map_id = cm2.id and cm2.channel_id IN (?, ?))</code>
	 */
	protected final static String foreignNormalJoinTemplate = "left join ${contentattribute} ${caAlias} on (${caAlias}.value_text = ${mainAlias}.contentid AND ${caAlias}.name = ?)\nleft join ${contentmap} ${cmAlias} on (${caAlias}.map_id = ${cmAlias}.id and ${cmAlias}.channel_id IN (${channelIds}))";

	/**
	 * Template for the nested optimized join statement part.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentmap cm2 on (cm2.contentid = ca1.value_text AND cm2.channel_id IN (?, ?))</code>
	 */
	protected final static String nestedOptimizedJoinTemplate = "left join ${contentmap} ${cmAlias} on (${cmAlias}.contentid = ${joinColumn} AND ${cmAlias}.channel_id IN (${channelIds}))";

	/**
	 * Template for the nested normal join statement part.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>caAlias</li> Alias of the contentattribute table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentmap cm2 on (cm2.contentid = ca1.value_text AND cm2.channel_id IN (?, ?)) left join contentattribute ca2 on (cm2.id = ca2.map_id AND ca2.name = ?)</code>
	 */
	protected final static String nestedNormalJoinTemplate = "left join ${contentmap} ${cmAlias} on (${cmAlias}.contentid = ${joinColumn} AND ${cmAlias}.channel_id IN (${channelIds})) left join ${contentattribute} ${caAlias} on (${cmAlias}.id = ${caAlias}.map_id AND ${caAlias}.name = ?)";

	/**
	 * Template for the nested foreign link join statement part.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentattribute ca1 on (ca1.value_text = cm1.contentid AND ca1.name = ?) left join contentmap cm2 on (ca1.map_id = cm2.id and cm2.channel_id IN (?, ?))</code>
	 */
	protected final static String nestedForeignLinkJoinTemplate = "left join ${contentattribute} ${caAlias} on (${caAlias}.value_text = ${joinColumn} AND ${caAlias}.name = ?) left join ${contentmap} ${cmAlias} on (${caAlias}.map_id = ${cmAlias}.id and ${cmAlias}.channel_id IN (${channelIds}))";

	/**
	 * Create an instance with the handle
	 * @param dbHandle db handle
	 */
	public MCCRDatasourceFilter(DBHandle dbHandle) {
		super(dbHandle);
	}

	/**
	 * Create an instance for the handle and shared resolvables
	 * @param dbHandle handle
	 * @param sharedResolvables shared resolvables
	 */
	public MCCRDatasourceFilter(DBHandle dbHandle, Map<?, ?> sharedResolvables) {
		super(dbHandle, sharedResolvables);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getDatasourceClass()
	 */
	public Class<?> getDatasourceClass() {
		return MCCRDatasource.class;
	}

	/**
	 * Merge the from part into the merged filter
	 * @param request query request
	 * @param mergedFilter merged filter
	 * @param countStatement true for count statements, false for select
	 *        statements
	 * @param sortColumns sort columns (may be null)
	 * @param versionTimestamp version timestamp (-1 for current version)
	 * @throws FilterGeneratorException
	 */
	protected void mergeFromPart(ExpressionQueryRequest request, MergedFilter mergedFilter,
			boolean countStatement, Sorting[] sortColumns, int versionTimestamp) throws FilterGeneratorException {
		// calculate the number of selected columns here these are the meta columns
		int numSelectedColumns = META_COLUMNS.length;

		// if (request.getDatasource() instanceof CNDatasource) {
		// CNDatasource cnd = (CNDatasource) request.getDatasource();
		// if (cnd.isAutoPrefetch()) {
		// // add the number of autoprefetched columns
		// numSelectedColumns += cnd.getAutoPrefetchedAttributes().length;
		// }
		// }
		if (sortColumns != null) {
			// add the number of sorted columns
			numSelectedColumns += sortColumns.length;
		}
		// initialize the set of selected columns
		Set<String> selectedColumns = new HashSet<String>(numSelectedColumns);

		String cm = getMainCmAlias();

		if (countStatement) {
			// make a count for countStatements
			// TODO
			mergedFilter.getStatement().append("SELECT count( distinct ").append(getMainCmAlias()).append(".contentid ) c\n");
		} else {
			boolean selectFields = ObjectTransformer.getBoolean(mergedFilter.getRequest().getParameters().get("selectfields"), true);

			if (selectFields) {
				// normal select
				mergedFilter.getStatement().append("SELECT ");
				boolean first = true;

				for (String metaCol : META_COLUMNS) {
					if (first) {
						first = false;
					} else {
						mergedFilter.getStatement().append(", ");
					}
					mergedFilter.getStatement().append(cm).append(".").append(metaCol);
					selectedColumns.add(cm + "." + metaCol);
				}
				// if (request.getDatasource() instanceof CNDatasource) {
				// CNDatasource cnd = (CNDatasource) request.getDatasource();
				// if (cnd.isAutoPrefetch()) {
				// // add the quick columns here, when autoprefetch is
				// // activated
				// AttributeType[] autoPrefetchedAttributes = cnd.getAutoPrefetchedAttributes();
				// for (int i = 0; i < autoPrefetchedAttributes.length; i++) {
				// mergedFilter.getStatement().append(", ").append(cm).append(".").append(autoPrefetchedAttributes[i].getQuickName());
				// selectedColumns.add(cm + "." + autoPrefetchedAttributes[i].getQuickName());
				// }
				// }
				// }
			} else {
				// only select 1 (this is most likely a subselect and does not
				// need to select the fields)
				mergedFilter.getStatement().append("SELECT 1");
			}
		}

		// add the sorted columns here
		if (!countStatement && sortColumns != null) {
			for (int i = 0; i < sortColumns.length; i++) {
				String sortedColumnName = getVariableName(sortColumns[i].getColumnName(), false, ExpressionEvaluator.OBJECTTYPE_ANY);

				if (!selectedColumns.contains(sortedColumnName)) {
					mergedFilter.getStatement().append(", ").append(sortedColumnName);
					selectedColumns.add(sortedColumnName);
				}
			}
		}

		mergedFilter.getStatement().append(" FROM\n");
		mergedFilter.getStatement().append(dbHandle.getContentMapName() + " ").append(cm);

		for (ColumnNameEntry columnNameEntry : columnEntryMap.values()) {
			// omit non-mandatory parts that are also not needed for sorting
			if (!columnNameEntry.isMandatory()) {
				boolean needed = false;
				String attributeName = columnNameEntry.getAttributeName();

				if (sortColumns != null) {
					// check all sorted columns, stop when the first sorted column is found that needs this entry
					for (int i = 0; i < sortColumns.length && !needed; i++) {
						// when the attribute name is part of the sorted column name, we need the entry
						if (sortColumns[i].getColumnName().startsWith(attributeName)) {
							needed = true;
						}
					}
				}

				// when the entry is not needed, omit it
				if (!needed) {
					continue;
				}
			}

			String joinPart = versionTimestamp < 0 ? columnNameEntry.getJoinPart() : columnNameEntry.getVersionedJoinPart();

			if (joinPart != null) {
				mergedFilter.getStatement().append("\n");
				mergedFilter.getStatement().append(joinPart);
				Object[] extraParams = columnNameEntry.getParams();

				if (extraParams != null) {
					for (int i = 0; i < extraParams.length; i++) {
						mergedFilter.getParams().add(extraParams[i]);
					}
				}
			}
		}
	}

	@Override
	protected void addConstantWhere(ExpressionQueryRequest request, MergedFilter filter) throws FilterGeneratorException {
		Datasource ds = request.getDatasource();

		if (ds instanceof MCCRDatasource) {
			try {
				List<DatasourceChannel> channels = ((MCCRDatasource) ds).getChannels();

				if (channels.isEmpty()) {
					filter.getStatement().append("(1 = 2) AND ");
				} else {
					filter.getStatement().append("(").append(getMainCmAlias()).append(".channel_id IN (").append(StringUtils.repeat("?", channels.size(), ", ")).append(
							")) AND ");
					for (DatasourceChannel channel : channels) {
						filter.getParams().add(channel.getId());
					}
				}
			} catch (DatasourceException e) {
				throw new FilterGeneratorException("Error while generating filter", e);
			}
		} else {
			throw new FilterGeneratorException("Datasource is no MCCRDatasource");
		}
	}

	@Override
	protected String getContentMapJoinColumn() {
		return "id";
	}

	@Override
	protected String getContentAttributeJoinColumn() {
		return "map_id";
	}

	@Override
	protected String[] getGroupByColumns() {
		return META_COLUMNS;
	}

	@Override
	protected boolean isMultichannelling() {
		return true;
	}

	@Override
	protected ColumnNameEntry getForeignOptimizedAttribute(String attributeName, String quickColumn, boolean mandatory) {
		String alias = getCmAliasName();

		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", alias);
		data.put("quickColumn", quickColumn);
		data.put("mainAlias", getMainCmAlias());
		// preserve the placeholder ${channelIds}, which will be replaced later (when we actually know the number of channels)
		data.put("channelIds", "${channelIds}");
		String join = StringUtils.resolveMapData(foreignOptimizedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + ".contentid", join, new Object[] { CHANNEL_ID}, null, true, mandatory,
				GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	@Override
	protected ColumnNameEntry getForeignNormalAttribute(String attributeName, String foreignAttribute, boolean mandatory) {
		String caAlias = getCaAliasName();
		String cmAlias = getCmAliasName();

		Map<String, String> data = new HashMap<String, String>(5);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", cmAlias);
		data.put("contentattribute", dbHandle.getContentAttributeName());
		data.put("caAlias", caAlias);
		data.put("mainAlias", getMainCmAlias());
		// preserve the placeholder ${channelIds}, which will be replaced later (when we actually know the number of channels)
		data.put("channelIds", "${channelIds}");
		String join = StringUtils.resolveMapData(foreignNormalJoinTemplate, data);

		return new ColumnNameEntry(attributeName, cmAlias + ".contentid", join, new Object[] { foreignAttribute, CHANNEL_ID }, null, true, mandatory,
				GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	@Override
	protected ColumnNameEntry getNestedOptimizedAttribute(String attributeName, int type, String quickColumn, ColumnNameEntry previousEntry,
			boolean mandatory) {
		String alias = getCmAliasName();

		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", alias);
		data.put("joinColumn", previousEntry.getColumnName());
		// preserve the placeholder ${channelIds}, which will be replaced later (when we actually know the number of channels)
		data.put("channelIds", "${channelIds}");
		String join = StringUtils.resolveMapData(nestedOptimizedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + "." + quickColumn, join, new Object[] { CHANNEL_ID }, null, type == 2, mandatory, type);
	}

	@Override
	protected ColumnNameEntry getNestedNormalAttribute(String attributeName, int type, ColumnNameEntry previousEntry, String lastPart,
			boolean mandatory) {
		// this is a normal, not optimized attribute
		String caX = getCaAliasName();

		// column name: ca2.value_text
		StringBuffer columnName = new StringBuffer(caX);

		columnName.append(".").append(DatatypeHelper.getTypeColumn(type));

		// join
		Map<String, String> dataMap = new HashMap<String, String>(5);

		dataMap.put("contentmap", dbHandle.getContentMapName());
		dataMap.put("contentattribute", dbHandle.getContentAttributeName());
		dataMap.put("cmAlias", getCmAliasName());
		dataMap.put("caAlias", caX);
		dataMap.put("joinColumn", previousEntry.getColumnName());
		// preserve the placeholder ${channelIds}, which will be replaced later (when we actually know the number of channels)
		dataMap.put("channelIds", "${channelIds}");
		String join = StringUtils.resolveMapData(nestedNormalJoinTemplate, dataMap);

		return new ColumnNameEntry(attributeName, columnName.toString(), join, new Object[] { CHANNEL_ID, lastPart }, null, type == 2, mandatory, type);
	}

	@Override
	protected ColumnNameEntry getNestedForeignLinkAttribute(String attributeName, ColumnNameEntry previousEntry, String foreignLinkAttribute,
			boolean mandatory) {
		String caAlias = getCaAliasName();
		String cmAlias = getCmAliasName();

		// FIXME we need to restrict foreign linked objects with the
		// correct object type. also take foreignlinkattribute rules
		// into consideration here
		Map<String, String> data = new HashMap<String, String>(5);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", cmAlias);
		data.put("contentattribute", dbHandle.getContentAttributeName());
		data.put("caAlias", caAlias);
		data.put("joinColumn", previousEntry.getColumnName());
		// preserve the placeholder ${channelIds}, which will be replaced later (when we actually know the number of channels)
		data.put("channelIds", "${channelIds}");
		String join = StringUtils.resolveMapData(nestedForeignLinkJoinTemplate, data);

		return new ColumnNameEntry(attributeName, cmAlias + ".contentid", join, new Object[] { foreignLinkAttribute, CHANNEL_ID }, null, true, mandatory,
				GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	@Override
	protected MergedFilter getMergedFilter(ExpressionQueryRequest request, boolean countStatement) throws ExpressionParserException {
		MergedFilter mergedFilter = super.getMergedFilter(request, countStatement);
		// replace placeholders in the list of parameters
		Datasource datasource = request.getDatasource();

		if (datasource instanceof MCCRDatasource) {
			try {
				List<DatasourceChannel> channels = ((MCCRDatasource) datasource).getChannels();
				List<Integer> channelIds = new ArrayList<Integer>(channels.size());

				for (DatasourceChannel channel : channels) {
					channelIds.add(channel.getId());
				}
				List params = mergedFilter.getParams();
				List<Object> newParams = new ArrayList<Object>(params.size());

				for (Object param : params) {
					if (param == CHANNEL_ID) {
						newParams.addAll(channelIds);
					} else {
						newParams.add(param);
					}
				}
				params.clear();
				params.addAll(newParams);
			} catch (DatasourceException e) {
				throw new FilterGeneratorException("Error while generating filter", e);
			}
		}
		return mergedFilter;
	}

	/**
	 * Creates a new filter which can be used to create subqueries. Both filters
	 * will share the same alias providers and therefore always have unique
	 * alias names. (ie. you can refer to {@link #getMainCmAlias()} in the
	 * subquery)
	 */
	public MCCRDatasourceFilter createMCCRSubFilter() {
		MCCRDatasourceFilter filter = new MCCRDatasourceFilter(dbHandle);

		filter.cmAliasProvider = this.cmAliasProvider;
		filter.caAliasProvider = this.caAliasProvider;
		return filter;
	}
}
