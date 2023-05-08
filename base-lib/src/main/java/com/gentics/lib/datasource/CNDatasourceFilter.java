/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: CNDatasourceFilter.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.filtergenerator.AbstractDatasourceFilter;
import com.gentics.lib.expressionparser.filtergenerator.ConstantFilterPart;
import com.gentics.lib.expressionparser.parser.ASTName;

/**
 * Implementation of
 * {@link com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter}
 * for the {@link com.gentics.lib.datasource.CNDatasource}.
 */
public class CNDatasourceFilter extends AbstractDatasourceFilter {
	public final static Class[] CNDATASOURCEFILTER_FUNCTION = new Class[] { CNDatasource.class};

	/**
	 * name of the columns, that are grouped
	 */
	protected final static String[] GROUPBY_COLUMNS = { "obj_id", "obj_type", "updatetimestamp", "mother_obj_id", "mother_obj_type"};

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 2213010897954811989L;

	/**
	 * Template for foreign attribute join with an optimized attribute.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>quickColumn</li> Name of the quick column of the optimized attribute</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentmap cm2 on (cm2.quick_link = cm1.contentid)</code>
	 */
	protected final static String foreignOptimizedJoinTemplate = "left join ${contentmap} ${cmAlias} on (${cmAlias}.${quickColumn} = ${mainAlias}.contentid)";

	/**
	 * Template for foreign attribute join with an optimized attribute for versioned queries.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>quickColumn</li> Name of the quick column of the optimized attribute</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentmap_nodeversion cm2 on (cm2.quick_link = cm1.contentid AND cm2.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM contentmap_nodeversion WHERE nodeversiontimestamp <= ? AND id = cm2.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )</code>
	 */
	protected final static String foreignOptimizedVersionedJoinTemplate = "left join ${contentmap}_nodeversion ${cmAlias} on (${cmAlias}.${quickColumn} = ${mainAlias}.contentid "
			+ " AND ${cmAlias}.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ${contentmap}_nodeversion WHERE nodeversiontimestamp <= ? AND id = ${cmAlias}.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )";

	/**
	 * Template for foreign attribute join with a normal attribute.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentattribute ca2 on (ca2.value_text = cm1.contentid AND ca2.name = ?)</code>
	 */
	protected final static String foreignNormalJoinTemplate = "left join ${contentattribute} ${caAlias} on (${caAlias}.value_text = ${mainAlias}.contentid AND ${caAlias}.name = ?)";

	/**
	 * Template for foreign attribute join with a normal attribute for versioned queries.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>mainAlias</i> Alias of the main table</li>
	 * </ul>
	 * <code>left join contentattribute_nodeversion ca2 on (ca2.value_text = cm1.contentid AND ca2.name = ? AND ca2.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM contentattribute_nodeversion WHERE nodeversiontimestamp <= ? AND id = ca2.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )</code>
	 */
	protected final static String foreignNormalVersionedJoinTemplate = "left join ${contentattribute}_nodeversion ${caAlias} on (${caAlias}.value_text = ${mainAlias}.contentid AND ${caAlias}.name = ? AND ${caAlias}.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ${contentattribute}_nodeversion WHERE "
			+ "nodeversiontimestamp <= ? AND id = ${caAlias}.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )";

	/**
	 * Template for the nested optimized join statement part.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentmap cm2 on (cm2.contentid = ca1.value_text)</code>
	 */
	protected final static String nestedOptimizedJoinTemplate = "left join ${contentmap} ${cmAlias} on (${cmAlias}.contentid = ${joinColumn})";

	/**
	 * Template for the nested optimized join statement part for versioned queries.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentmap</i> Name of the contentmap table</li>
	 * <li><i>cmAlias</i> Alias of the contentmap table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentmap_nodeversion cm2 on (cm2.contentid = ca1.value_text AND cm2.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM contentmap_nodeversion WHERE nodeversiontimestamp <= ? AND id = cm2.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )</code>
	 */
	protected final static String nestedOptimizedVersionedJoinTemplate = "left join ${contentmap}_nodeversion ${cmAlias} on (${cmAlias}.contentid = ${joinColumn} AND ${cmAlias}.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ${contentmap}_nodeversion WHERE nodeversiontimestamp <= ? AND id = ${cmAlias}.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )";

	/**
	 * Template for the nested foreign link join statement part.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentattribute ca2 on (ca2.value_text = cm1.contentid AND ca2.name = ?)</code>
	 */
	protected final static String nestedForeignLinkJoinTemplate = "left join ${contentattribute} ${caAlias} on (${caAlias}.value_text = ${joinColumn} AND ${caAlias}.name = ?)";

	/**
	 * Template for the nested foreign link join statement part for versioned queries.<br>
	 * Contains the following variables
	 * <ul>
	 * <li><i>contentattribute</i> Name of the contentattribute table</li>
	 * <li><i>caAlias</i> Alias of the contentattribute table</li>
	 * <li><i>joinColumn</li> Name of the joined column (incl. alias)</li>
	 * </ul>
	 * <code>left join contentattribute_nodeversion ca2 on (ca2.value_text = cm1.contentid AND ca2.name = ? AND ca2.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM contentattribute_nodeversion WHERE nodeversiontimestamp <= ? AND id = ca2.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )</code>
	 */
	protected final static String nestedForeignLinkVersionedJoinTemplate = "left join ${contentattribute}_nodeversion ${caAlias} on (${caAlias}.value_text = ${joinColumn} AND ${caAlias}.name = ? AND ${caAlias}.nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ${contentattribute}_nodeversion WHERE "
			+ "nodeversiontimestamp <= ? AND id = ${caAlias}.id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )";

	/**
	 * database handle
	 */
	protected DBHandle dbHandle;

	/**
	 * Map of all column entries. Keys are the attribute names, values are the
	 * instances of {@link ColumnNameEntry}.
	 */
	protected Map<String, ColumnNameEntry> columnEntryMap = new LinkedHashMap<String, ColumnNameEntry>();

	/**
	 * Counter for table aliases (for joined contentattribute tables)
	 */
	protected UniqueAliasNameProvider caAliasProvider = null;

	/**
	 * Counter for table aliases (for joined contentmap tables)
	 */
	protected UniqueAliasNameProvider cmAliasProvider = null;

	/**
	 * This is the alias for the "main" Cm alias (usually cm1)
	 */
	private String mainCmAlias;

	/**
	 * Create an instance of the cndatasource filter
	 * @param dbHandle database handle
	 */
	public CNDatasourceFilter(DBHandle dbHandle) {
		super();
		this.dbHandle = dbHandle;
	}

	/**
	 * Create an instance that shares the resolvables with the default ruletree
	 * @param dbHandle db handle
	 * @param sharedResolvables shared resolvables map
	 */
	public CNDatasourceFilter(DBHandle dbHandle, Map sharedResolvables) {
		super(sharedResolvables);
		this.dbHandle = dbHandle;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#generateLiteralFilterPart(java.lang.Object,
	 *      int)
	 */
	public FilterPart generateLiteralFilterPart(Object literal, int expectedValueType) throws FilterGeneratorException {
		try {
			// make sure the given literal is the correct type (or convert it,
			// if possible)
			Object typeSafeLiteral = ExpressionEvaluator.getAsType(literal, expectedValueType);

			if (expectedValueType == ExpressionEvaluator.OBJECTTYPE_WILDCARDSTRING && typeSafeLiteral != null) {
				// replace '*' by '%'
				typeSafeLiteral = typeSafeLiteral.toString().replaceAll("\\*", "%");
			}
			StringBuffer sqlStatement = new StringBuffer();
			List params = new Vector();

			// special treatment for collections
			if (typeSafeLiteral instanceof Collection) {
				sqlStatement.append("(");
				Collection col = (Collection) typeSafeLiteral;
				boolean first = true;

				for (Iterator iter = col.iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					if (first) {
						first = false;
					} else {
						sqlStatement.append(",");
					}
					sqlStatement.append("?");
					params.add(element);
				}
				sqlStatement.append(")");
			} else if (typeSafeLiteral instanceof Boolean && expectedValueType == ExpressionEvaluator.OBJECTTYPE_BOOLEAN) {
				// special treatment for booleans (due to problems mit hsqldb
				// when a where clause consists of a single bind variable with
				// boolean value)
				sqlStatement.append(((Boolean) typeSafeLiteral).booleanValue() ? "(1 = 1)" : "(1 = 2)");
			} else {
				sqlStatement.append("?");
				params.add(typeSafeLiteral);
			}

			// generate a constant filterpart
			return new ConstantFilterPart(this, sqlStatement.toString(), (Object[]) params.toArray(new Object[params.size()]));
		} catch (EvaluationException e) {
			throw new FilterGeneratorException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getVariableName(java.lang.String)
	 */
	public String getVariableName(String expressionName, int expectedValueType) throws FilterGeneratorException {
		return getVariableName(expressionName, true, expectedValueType);
	}

	/**
	 * Convert the given expression name into a variable name
	 * @param expressionName expression name
	 * @param mandatory true when the variable will be used in the filter
	 *        clause, false if not (used for sorting)
	 * @param expectedValueType TODO
	 * @return variable name
	 * @throws FilterGeneratorException
	 */
	public String getVariableName(String expressionName, boolean mandatory,
			int expectedValueType) throws FilterGeneratorException {
		if (expressionName == null) {
			return null;
		}
		if (expressionName.startsWith("object.")) {
			expressionName = expressionName.substring(7);
		}

		try {
			ColumnNameEntry entry = getColumnNameEntry(expressionName, mandatory);
			String variableName = entry.getColumnName();

			// may we have to cast here
			if (expectedValueType == ExpressionEvaluator.OBJECTTYPE_STRING || entry.getAttributeType() == 5) {
				String textCastType = dbHandle.getTextCastName();

				if (textCastType != null) {
					if (dbHandle.isSubstrWhenCasting()) {
						variableName = "CAST(SUBSTR(" + variableName + ", 1, 255) AS " + textCastType + ")";
					} else {
						variableName = "CAST(" + variableName + " AS " + textCastType + ")";
					}
				}
			}

			return variableName;
		} catch (FilterGeneratorException e) {
			throw e;
		} catch (Exception e) {
			throw new FilterGeneratorException("Error generating filter for variable object {" + expressionName + "}", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getDatasourceClass()
	 */
	public Class getDatasourceClass() {
		return CNDatasource.class;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer string = new StringBuffer();

		string.append("DatasourceFilter for CNDatasource ").append(getMainFilterPart());
		return string.toString();
	}

	/**
	 * Add a constant where clause to the merged filter
	 * @param request expression query request
	 * @param filter merged filter
	 * @throws FilterGeneratorException
	 */
	protected void addConstantWhere(ExpressionQueryRequest request, MergedFilter filter) throws FilterGeneratorException {}

	/**
	 * Get the columns for grouping
	 * @return column names
	 */
	protected String[] getGroupByColumns() {
		return GROUPBY_COLUMNS;
	}

	/**
	 * Get the merged filter (sql statement and list of parameters) for the
	 * given datasource
	 * @param request expression request
	 * @param countStatement true when the merged filter shall be the count(*)
	 *        statement, false for the fetch statement
	 * @return merged filter containing the sql statement and the list of
	 *         parameters
	 * @throws ExpressionParserException
	 */
	protected MergedFilter getMergedFilter(ExpressionQueryRequest request,
			boolean countStatement) throws ExpressionParserException {
		// prepare the merged filter
		MergedFilter mergedFilter = new MergedFilter(request);

		// begin with the where clause
		mergedFilter.getStatement().append("\nWHERE ");
		addConstantWhere(request, mergedFilter);

		String mainCmAlias = getMainCmAlias();

		// for versioned queries, restrict the versions
		if (request.getVersionTimestamp() >= 0) {
			mergedFilter.getStatement().append(mainCmAlias).append(".nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ").append(dbHandle.getContentMapName() + "_nodeversion WHERE ").append(dbHandle.getContentMapName() + "_nodeversion.id = ").append(mainCmAlias).append(
					".id AND nodeversiontimestamp <= ? AND (nodeversionremoved > ? OR nodeversionremoved = 0)) AND ");
			Integer timestamp = new Integer(request.getVersionTimestamp());

			mergedFilter.getParams().add(timestamp);
			mergedFilter.getParams().add(timestamp);
		}

		// now combine all statement parts
		getMainFilterPart().mergeInto(mergedFilter);

		// we need a group by clause, when this is no count statement and at
		// least one alias for contentattribute table was generated (tables were joined)
		boolean appendGroupBy = !countStatement && numOfCaAliases() > 0 && ObjectTransformer.getBoolean(request.getParameters().get("usegroupby"), true);

		if (appendGroupBy) {
			// merge the group by statement part (not for count statement)
			mergedFilter.getStatement().append("\nGROUP BY ").append(StringUtils.merge(getGroupByColumns(), ", ", mainCmAlias + ".", ""));
		}

		Sorting[] sortColumns = request.getSorting();

		// add group by parts for sorted columns (not for count statements)
		if (!countStatement && sortColumns != null && sortColumns.length > 0) {
			StringBuffer sortBy = new StringBuffer("\nORDER BY ");

			for (int i = 0; i < sortColumns.length; i++) {
				String variableName = getVariableName(sortColumns[i].getColumnName(), false, ExpressionEvaluator.OBJECTTYPE_ANY);

				if (appendGroupBy) {
					mergedFilter.getStatement().append(", ").append(variableName);
				}

				if (i > 0) {
					sortBy.append(", ");
				}
				sortBy.append(variableName).append(" ").append(sortColumns[i].getSortOrder() == Datasource.SORTORDER_DESC ? "DESC" : "ASC");
			}

			mergedFilter.getStatement().append(sortBy);
		}

		MergedFilter fullPart = new MergedFilter(request);

		// merge from part (select .... from ....) at the beginning
		mergeFromPart(request, fullPart, countStatement, sortColumns, request.getVersionTimestamp());

		// finally combine the from part and the rest (from part first)
		fullPart.getStatement().append(mergedFilter.getStatement());
		fullPart.getParams().addAll(mergedFilter.getParams());

		return fullPart;
	}

	/**
	 * Get the merged filter containing the sql statement and parameters for
	 * counting
	 * @param request expression request
	 * @return merged filter for counting
	 * @throws ExpressionParserException
	 */
	public MergedFilter getCountStatement(ExpressionQueryRequest request) throws ExpressionParserException {
		return getMergedFilter(request, true);
	}

	/**
	 * Get the merged filter containing the sql statement and parameters for
	 * selecting
	 * @param request expression request
	 * @return merged filter for selecting
	 * @throws ExpressionParserException
	 */
	public MergedFilter getSelectStatement(ExpressionQueryRequest request) throws ExpressionParserException {
		return getMergedFilter(request, false);
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
		Integer timestamp = new Integer(versionTimestamp);

		// calculate the number of selected columns here
		// these are the meta columns
		int numSelectedColumns = 5;

		if (request.getDatasource() instanceof CNDatasource) {
			CNDatasource cnd = (CNDatasource) request.getDatasource();

			if (cnd.isAutoPrefetch()) {
				// add the number of autoprefetched columns
				numSelectedColumns += cnd.getAutoPrefetchedAttributes().length;
			}
		}
		if (sortColumns != null) {
			// add the number of sorted columns
			numSelectedColumns += sortColumns.length;
		}
		// initialize the set of selected columns
		Set selectedColumns = new HashSet(numSelectedColumns);

		String cm = getMainCmAlias();

		if (countStatement) {
			// make a count for countStatements
			mergedFilter.getStatement().append("SELECT count( distinct ").append(getMainCmAlias()).append(".contentid ) c\n");
		} else {
			boolean selectFields = ObjectTransformer.getBoolean(mergedFilter.getRequest().getParameters().get("selectfields"), true);

			if (selectFields) {
				// normal select
				mergedFilter.getStatement().append("SELECT ").append(cm).append(".obj_id, ").append(cm).append(".obj_type, ").append(cm).append(".updatetimestamp, ").append(cm).append(".mother_obj_id, ").append(cm).append(
						".mother_obj_type");
				selectedColumns.add(cm + ".obj_id");
				selectedColumns.add(cm + ".obj_type");
				selectedColumns.add(cm + ".updatetimestamp");
				selectedColumns.add(cm + ".mother_obj_id");
				selectedColumns.add(cm + ".mother_obj_type");
				if (request.getDatasource() instanceof CNDatasource) {
					CNDatasource cnd = (CNDatasource) request.getDatasource();

					if (cnd.isAutoPrefetch()) {
						// add the quick columns here, when autoprefetch is
						// activated
						AttributeType[] autoPrefetchedAttributes = cnd.getAutoPrefetchedAttributes();

						for (int i = 0; i < autoPrefetchedAttributes.length; i++) {
							mergedFilter.getStatement().append(", ").append(cm).append(".").append(autoPrefetchedAttributes[i].getQuickName());
							selectedColumns.add(cm + "." + autoPrefetchedAttributes[i].getQuickName());
						}
					}
				}
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

		// different base table for versioned data
		if (versionTimestamp < 0) {
			mergedFilter.getStatement().append(dbHandle.getContentMapName() + " ").append(cm);
		} else {
			mergedFilter.getStatement().append(dbHandle.getContentMapName() + "_nodeversion ").append(cm);
		}

		for (Iterator iter = columnEntryMap.values().iterator(); iter.hasNext();) {
			ColumnNameEntry columnNameEntry = (ColumnNameEntry) iter.next();

			// omit non-mandatory parts that are also not needed for
			// sorting
			if (!columnNameEntry.isMandatory()) {
				boolean needed = false;
				String attributeName = columnNameEntry.getAttributeName();

				if (sortColumns != null) {
					// check all sorted columns, stop when the first sorted
					// column is found that needs this entry
					for (int i = 0; i < sortColumns.length && !needed; i++) {
						// when the attribute name is part of the sorted column
						// name, we need the entry
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
				if (versionTimestamp >= 0) {
					mergedFilter.getParams().add(timestamp);
					mergedFilter.getParams().add(timestamp);
				}
			}
		}
	}

	/**
	 * The Method for getting Data Type of a specified column
	 * @param attributeName the name of the column
	 * @return the data type of this column
	 */
	protected String getDataTypeColumn(String attributeName) throws FilterGeneratorException {
		int lastDotPosition = attributeName.lastIndexOf('.');

		if (lastDotPosition >= 0) {
			attributeName = attributeName.substring(lastDotPosition + 1);
		}

		try {
			return DatatypeHelper.getComplexDatatype(dbHandle, attributeName).getColumn();
		} catch (Exception e) {
			throw new FilterGeneratorException("Cannot get datatype column for attribute {" + attributeName + "}", e);
		}
	}

	/**
	 * returns the alias for the contentmap alias from which everything is
	 * selected.
	 */
	public String getMainCmAlias() {
		if (mainCmAlias == null) {
			mainCmAlias = getCmAliasName();
		}
		return mainCmAlias;
	}

	/**
	 * Get a new table alias for contentattribute tables
	 * @return table alias
	 */
	protected String getCaAliasName() {
		if (caAliasProvider == null) {
			caAliasProvider = new UniqueAliasNameProvider("ca");
		}
		return caAliasProvider.getUniqueAlias();
	}

	/**
	 * Get a new table alias for contentmap tables
	 * @return table alias
	 */
	protected String getCmAliasName() {
		if (cmAliasProvider == null) {
			cmAliasProvider = new UniqueAliasNameProvider("cm");
		}
		return cmAliasProvider.getUniqueAlias();
	}

	/**
	 * Count the number of table aliases that were generated
	 * @return number of table aliases
	 */
	protected int numOfAliases() {
		int numOfAliases = 0;

		if (caAliasProvider != null) {
			numOfAliases += caAliasProvider.getUniqueCounter();
		}
		if (cmAliasProvider != null) {
			numOfAliases += cmAliasProvider.getUniqueCounter();
		}
		return numOfAliases;
	}

	/**
	 * Count the number of contentattribute table aliases that were generated
	 * @return number of aliases for contentattribute table
	 */
	protected int numOfCaAliases() {
		int numOfCaAliases = 0;

		if (caAliasProvider != null) {
			numOfCaAliases += caAliasProvider.getUniqueCounter();
		}
		return numOfCaAliases;
	}

	/**
	 * Get the name of the column in table "contentmap", that is joined with table "contentattribute"
	 * @return name of the column
	 */
	protected String getContentMapJoinColumn() {
		return "contentid";
	}

	/**
	 * Get the name of the column in table "contentattribute", that is joined with table "contentmap"
	 * @return name of the column
	 */
	protected String getContentAttributeJoinColumn() {
		return "contentid";
	}

	/**
	 * Get the column name entry for a foreign link attribute, that belongs to an optimized link attribute
	 * @param attributeName attribute name
	 * @param quickColumn quick column of the link attribute
	 * @param mandatory true for mandatory
	 * @return column name entry
	 */
	protected ColumnNameEntry getForeignOptimizedAttribute(String attributeName, String quickColumn, boolean mandatory) {
		String alias = getCmAliasName();

		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", alias);
		data.put("quickColumn", quickColumn);
		data.put("mainAlias", getMainCmAlias());
		String join = StringUtils.resolveMapData(foreignOptimizedJoinTemplate, data);
		String versionedJoin = StringUtils.resolveMapData(foreignOptimizedVersionedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + ".contentid", join, null, versionedJoin, true, mandatory, GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	/**
	 * Get the column name entry for a foreign link attribute, that belongs to a normal link attribute
	 * @param attributeName attribute name
	 * @param foreignAttribute foreign attribute name
	 * @param mandatory true for mandatory
	 * @return column name entry
	 */
	protected ColumnNameEntry getForeignNormalAttribute(String attributeName, String foreignAttribute, boolean mandatory) {
		String alias = getCaAliasName();

		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentattribute", dbHandle.getContentAttributeName());
		data.put("caAlias", alias);
		data.put("mainAlias", getMainCmAlias());
		String join = StringUtils.resolveMapData(foreignNormalJoinTemplate, data);
		String versionedJoin = StringUtils.resolveMapData(foreignNormalVersionedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + ".contentid", join, new Object[] { foreignAttribute }, versionedJoin, true, mandatory,
				GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	/**
	 * Get the column name entry for a nested, optimized attribute
	 * @param attributeName attribute name
	 * @param type attribute type
	 * @param quickColumn quick column
	 * @param previousEntry previous entry
	 * @param mandatory true for mandatory
	 * @return column name entry
	 */
	protected ColumnNameEntry getNestedOptimizedAttribute(String attributeName, int type, String quickColumn, ColumnNameEntry previousEntry, boolean mandatory) {
		String alias = getCmAliasName();

		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentmap", dbHandle.getContentMapName());
		data.put("cmAlias", alias);
		data.put("joinColumn", previousEntry.getColumnName());
		String join = StringUtils.resolveMapData(nestedOptimizedJoinTemplate, data);
		String versionedJoin = StringUtils.resolveMapData(nestedOptimizedVersionedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + "." + quickColumn, join, null, versionedJoin, type == 2, mandatory, type);
	}

	/**
	 * Get the column name entry for a nested, normal (not optimized) attribute
	 * @param attributeName attribute name
	 * @param type attribute type
	 * @param previousEntry previous entry
	 * @param lastPart last part of the attribute name
	 * @param mandatory true for mandatory
	 * @return column name entry
	 */
	protected ColumnNameEntry getNestedNormalAttribute(String attributeName, int type, ColumnNameEntry previousEntry, String lastPart, boolean mandatory) {
		// this is a normal, not optimized attribute
		String alias = getCaAliasName();
		// column name: ca2.value_text
		StringBuffer columnName = new StringBuffer(alias);

		columnName.append(".").append(DatatypeHelper.getTypeColumn(type));

		// join: "left join contentattribute ca2 on (ca2.contentid = ca1.value_text AND ca2.name = ?"
		StringBuffer join = new StringBuffer("left join ");

		join.append(dbHandle.getContentAttributeName()).append(" ").append(alias).append(" on (");
		join.append(alias).append(".").append(getContentAttributeJoinColumn()).append(" = ").append(previousEntry.getColumnName());
		join.append(" AND ").append(alias).append(".name = ?)");

		// versionedJoin:
		StringBuffer versionedJoin = new StringBuffer();

		versionedJoin.append("left join ").append(dbHandle.getContentAttributeName()).append("_nodeversion ").append(alias);
		versionedJoin.append(" on (").append(alias).append(".").append(getContentAttributeJoinColumn()).append(" = ").append(previousEntry.getColumnName());
		versionedJoin.append(" AND ").append(alias).append(".name = ? AND ").append(alias).append(
				".nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM ");
		versionedJoin.append(dbHandle.getContentAttributeName()).append("_nodeversion WHERE nodeversiontimestamp <= ? AND id = ").append(alias).append(
				".id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )");

		return new ColumnNameEntry(attributeName, columnName.toString(), join.toString(), new Object[] { lastPart }, versionedJoin.toString(), type == 2,
				mandatory, type);
	}

	/**
	 * Get the column name entry for a nested
	 * @param attributeName
	 * @param previousEntry
	 * @param foreignLinkAttribute
	 * @param mandatory
	 * @return
	 */
	protected ColumnNameEntry getNestedForeignLinkAttribute(String attributeName, ColumnNameEntry previousEntry, String foreignLinkAttribute, boolean mandatory) {
		String alias = getCaAliasName();

		// FIXME we need to restrict foreign linked objects with the
		// correct object type. also take foreignlinkattribute rules
		// into consideration here
		Map<String, String> data = new HashMap<String, String>(3);

		data.put("contentattribute", dbHandle.getContentAttributeName());
		data.put("caAlias", alias);
		data.put("joinColumn", previousEntry.getColumnName());
		String join = StringUtils.resolveMapData(nestedForeignLinkJoinTemplate, data);
		String versionedJoin = StringUtils.resolveMapData(nestedForeignLinkVersionedJoinTemplate, data);

		return new ColumnNameEntry(attributeName, alias + ".contentid", join, new Object[] { foreignLinkAttribute }, versionedJoin, true, mandatory,
				GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ);
	}

	/**
	 * Get or create a column name entry for the given attribute name
	 * @param attributeName name of the attribute
	 * @param mandatory true when the join has to be done in any case (attribute
	 *        is used in where clause), false for optional joins (attribute is
	 *        only used for filtering)
	 * @return the column name entry instance
	 * @throws Exception
	 */
	protected ColumnNameEntry getColumnNameEntry(String attributeName, boolean mandatory) throws Exception {
		// first check whether the attribute name is already stored
		ColumnNameEntry entry = (ColumnNameEntry) columnEntryMap.get(attributeName);

		if (entry != null) {
			return entry;
		} else {
			int lastDotPosition = attributeName.lastIndexOf('.');

			if (lastDotPosition < 0) {
				// this is a direct attribute of the searched object
				// check type of attribute (quick/linked/normal)
				AttributeType typeInfo = DatatypeHelper.getComplexDatatype(dbHandle, attributeName);

				// check whether the attribute is written into the filesystem
				if (typeInfo.isFilesystem()) {
					// using attributes that write into the filesystem in filters is forbidden
					throw new FilterGeneratorException("Attribute " + attributeName + " cannot be used in a filter, because it is written into the filesystem");
				}

				int type = typeInfo.getType();

				if (typeInfo.isOptimized()) {
					// this is an optimized attribute
					entry = new ColumnNameEntry(attributeName, getMainCmAlias() + "." + typeInfo.getQuickName(), null, null, null, type == 2, mandatory, type);
					columnEntryMap.put(attributeName, entry);
				} else if (type == 7) {
					// this is a foreign linked attribute
					// FIXME we need to restrict foreign linked objects with the
					// correct object type. also take foreignlinkattribute rules
					// into consideration here
					AttributeType linkedAttribute = DatatypeHelper.getComplexDatatype(dbHandle, typeInfo.getForeignLinkedAttribute());

					if (linkedAttribute.isOptimized()) {
						entry = getForeignOptimizedAttribute(attributeName, linkedAttribute.getQuickName(), mandatory);
					} else {
						entry = getForeignNormalAttribute(attributeName, typeInfo.getForeignLinkedAttribute(), mandatory);
					}
					columnEntryMap.put(attributeName, entry);
				} else {
					// this is a normal, not optimized attribute
					String alias = getCaAliasName();

					entry = new ColumnNameEntry(attributeName, alias + "." + DatatypeHelper.getTypeColumn(type),
							"left join " + dbHandle.getContentAttributeName() + " " + alias + " on (" + alias + "." + getContentAttributeJoinColumn() + " = "
							+ getMainCmAlias() + "." + getContentMapJoinColumn() + " AND " + alias + ".name = ?)",
							new Object[] { attributeName},
							"left join " + dbHandle.getContentAttributeName() + "_nodeversion " + alias + " on (" + alias + "." + getContentAttributeJoinColumn()
							+ " = " + getMainCmAlias() + "." + getContentMapJoinColumn() + " AND " + alias + ".name = ? AND " + alias
							+ ".nodeversiontimestamp = (SELECT max(nodeversiontimestamp) FROM " + dbHandle.getContentAttributeName()
							+ "_nodeversion WHERE nodeversiontimestamp <= ? AND id = " + alias + ".id AND (nodeversionremoved = 0 OR nodeversionremoved > ?)) )",
							type == 2,
							mandatory,
							type);
					columnEntryMap.put(attributeName, entry);
				}
			} else {
				// this is a combined attribute, so get the previous entry and
				// combine it with the last one
				ColumnNameEntry previousEntry = getColumnNameEntry(attributeName.substring(0, lastDotPosition), mandatory);

				if (!previousEntry.isLinkAttribute()) {
					// the previous attribute is no link attribute, so this must
					// fail
					throw new FilterGeneratorException(
							"Cannot create filter part for attribute {" + attributeName + "}: {" + previousEntry.getAttributeName()
							+ "} must be a link or foreign link attribute!");
				}
				String lastPart = attributeName.substring(lastDotPosition + 1);

				AttributeType typeInfo = DatatypeHelper.getComplexDatatype(dbHandle, lastPart);
				int type = typeInfo.getType();

				if (typeInfo.isOptimized()) {
					entry = getNestedOptimizedAttribute(attributeName, type, typeInfo.getQuickName(), previousEntry, mandatory);
					columnEntryMap.put(attributeName, entry);
				} else if (type == 7) {
					// this is a foreign linked attribute
					entry = getNestedForeignLinkAttribute(attributeName, previousEntry, typeInfo.getForeignLinkedAttribute(), mandatory);
					columnEntryMap.put(attributeName, entry);
				} else {
					// this is a normal, not optimized attribute
					entry = getNestedNormalAttribute(attributeName, type, previousEntry, lastPart, mandatory);
					columnEntryMap.put(attributeName, entry);
				}
			}
		}
		return entry;
	}

	/**
	 * Creates a new filter which can be used to create subqueries. Both filters
	 * will share the same alias providers and therefore always have unique
	 * alias names. (ie. you can refer to {@link #getMainCmAlias()} in the
	 * subquery)
	 */
	public CNDatasourceFilter createSubFilter() {
		CNDatasourceFilter filter = new CNDatasourceFilter(dbHandle);

		filter.cmAliasProvider = this.cmAliasProvider;
		filter.caAliasProvider = this.caAliasProvider;
		return filter;
	}

	/**
	 * Inner class for storing information about column names.
	 */
	protected class ColumnNameEntry {

		/**
		 * true when the column name entry is mandatory, false if not
		 */
		protected boolean mandatory;

		/**
		 * sql join statement (may be null)
		 */
		protected String joinPart;

		/**
		 * column name of this entry. includes table alias
		 */
		protected String columnName;

		/**
		 * attribute name for which this column name entry is needed
		 */
		protected String attributeName;

		/**
		 * true when this is a link attribute, false for "normal" attributes
		 */
		protected boolean linkAttribute;

		/**
		 * parameters needed for the join
		 */
		protected Object[] params;

		/**
		 * versioned join part
		 */
		protected String versionedJoinPart;

		/**
		 * type of the attribute
		 */
		protected int attributeType;

		/**
		 * @return Returns the attributeName.
		 */
		public String getAttributeName() {
			return attributeName;
		}

		/**
		 * @return Returns the columnName.
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * @return Returns the joinPart.
		 */
		public String getJoinPart() {
			return joinPart;
		}

		/**
		 * @return Returns the linkAttribute.
		 */
		public boolean isLinkAttribute() {
			return linkAttribute;
		}

		/**
		 * attribute type
		 * @return attribute type
		 */
		public int getAttributeType() {
			return attributeType;
		}

		/**
		 * Create a new instance of the column name entry
		 * @param attributeName attribute name
		 * @param columnName column name
		 * @param joinPart join sql statement part
		 * @param params parameters (for the join)
		 * @param versionedJoinPart versioned sql join
		 * @param linkAttribute true for link attributes
		 * @param mandatory true for mandatory joins
		 * @param attributeType type of the attribute
		 */
		public ColumnNameEntry(String attributeName, String columnName, String joinPart,
				Object[] params, String versionedJoinPart, boolean linkAttribute,
				boolean mandatory, int attributeType) {
			this.attributeName = attributeName;
			this.columnName = columnName;
			this.joinPart = joinPart;
			this.linkAttribute = linkAttribute;
			this.params = params;
			this.versionedJoinPart = versionedJoinPart;
			this.mandatory = mandatory;
			this.attributeType = attributeType;
		}

		/**
		 * @return Returns the params.
		 */
		public Object[] getParams() {
			return params;
		}

		/**
		 * @return Returns the versionedJoinPart.
		 */
		public String getVersionedJoinPart() {
			return versionedJoinPart;
		}

		/**
		 * @return Returns the mandatory.
		 */
		public boolean isMandatory() {
			return mandatory;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#allowsNullValues(java.lang.String)
	 */
	public boolean allowsNullValues(String attributeName) throws FilterGeneratorException {
		try {
			return !DatatypeHelper.isMultivalue(dbHandle, attributeName);
		} catch (Exception e) {
			throw new FilterGeneratorException("Error while trying to determine if attribute {" + attributeName + "} is a multi value attribute.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#getValueType(java.lang.String)
	 */
	public int getValueType(String attributeName) throws FilterGeneratorException {
		try {
			int dataType = DatatypeHelper.getDatatype(dbHandle, attributeName);

			switch (dataType) {
			case GenticsContentAttribute.ATTR_TYPE_TEXT:
			case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
				return ExpressionEvaluator.OBJECTTYPE_STRING;

			case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			case GenticsContentAttribute.ATTR_TYPE_LONG:
			case GenticsContentAttribute.ATTR_TYPE_INTEGER:
				return ExpressionEvaluator.OBJECTTYPE_NUMBER;

			case GenticsContentAttribute.ATTR_TYPE_DATE:
				return ExpressionEvaluator.OBJECTTYPE_DATE;

			case GenticsContentAttribute.ATTR_TYPE_BINARY:
			case GenticsContentAttribute.ATTR_TYPE_BLOB:
				return ExpressionEvaluator.OBJECTTYPE_BINARY;

			default:
				return ExpressionEvaluator.OBJECTTYPE_ANY;
			}
		} catch (Exception e) {
			throw new FilterGeneratorException("Error while trying to determine datatype for attribute {" + attributeName + "}", e);
		}
	}

	/**
	 * Check whether the given expression denotes an optimized attribute
	 * @param expr expression
	 * @return true if the expression is an optimized attribute
	 * @throws ExpressionParserException
	 */
	public boolean isOptimized(EvaluableExpression expr) throws ExpressionParserException {
		if (!expr.isVariable(this)) {
			return false;
		}
		if (expr instanceof ASTName) {
			ASTName name = (ASTName) expr;
			String objectName = name.getObjectName();

			if (objectName.startsWith("object.")) {
				objectName = objectName.substring(7);
			}
			try {
				if (DatatypeHelper.getDefaultColumnTypes(isMultichannelling()).containsKey(objectName)) {
					return true;
				}
				AttributeType[] types = DatatypeHelper.getAttributeTypes(dbHandle, null, null, null, null, null, new String[] { objectName});

				for (AttributeType type : types) {
					if (type.isOptimized()) {
						return true;
					}
				}
				return false;
			} catch (CMSUnavailableException e) {
				throw new FilterGeneratorException("Error while checking [" + expr + "]", e);
			}
		} else {
			return false;
		}
	}

	/**
	 * Return true, when the filter belongs to a multichannelling datasource
	 * @return true for multichannelling
	 */
	protected boolean isMultichannelling() {
		return false;
	}
}
