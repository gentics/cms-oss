package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.datasource.AbstractDatasource;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.CompareOperator;
import com.gentics.lib.parser.rule.Condition;
import com.gentics.lib.parser.rule.Operand;
import com.gentics.lib.parser.rule.StringOperand;
import com.gentics.lib.parser.rule.functions.Function;
import com.gentics.lib.parser.rule.functions.FunctionOperand;
import com.gentics.lib.parser.rule.functions.IsEmptyFunction;

/**
 * @author haymo
 * @date 04.08.2004
 * @version $Id: SQLDatasource.java,v 1.31 2006-09-19 14:19:33 norbert Exp $
 */
public class SQLDatasource extends AbstractDatasource {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	private RuleTree ruleTree;

	private SQLHandle handle;

	private String table;

	private String[] attributNames;

	public SQLDatasource(String id, SQLHandle handle, Map parameters) {
		super(id);
		this.handle = handle;
		if (parameters.containsKey("table")) {
			this.table = (String) parameters.get("table");
		}
		// TODO Eroorhandling
	}

	public int getCount2() throws DatasourceNotAvailableException {
		throw new UnsupportedOperationException("not yet implemented!");
	}

	public Datasource getInstance(DatasourceHandle handle, Map parameters) throws NodeIllegalArgumentException {
		if (!(handle instanceof SQLHandle)) {
			throw new NodeIllegalArgumentException("Expected SQL Handle.");
		}
		return new SQLDatasource(getId(), (SQLHandle) handle, parameters);
	}

	private void ruleToSqlCondition(RuleTree tree, StringBuffer sql) {
		Iterator it = tree.iterator();

		while (it.hasNext()) {
			Object o = it.next();

			if (o instanceof Condition) {
				// TODO static/dynamic
				// TODO replace c.getOperator with </>/=/...

				Condition c = (Condition) o;

				// rewrite conditions
				rewriteCondition(c);

				sql.append("( ");
				sql.append(c.getLeftOperand());
				sql.append(" ");
				sql.append(c.getOperator());
				sql.append(" ");
				sql.append(c.getRightOperand());
				sql.append(" )");

			} else if (o instanceof RuleTree) {

				RuleTree rt = (RuleTree) o;

				sql.append("( ");
				ruleToSqlCondition(rt, sql);
				sql.append(" )");

			} else if (o instanceof LogicalOperator) {

				// TODO replace l.getType with AND/OR/...
				// TODO throw exception on operator change (see ruleToLdap)
				LogicalOperator l = (LogicalOperator) o;

				sql.append(" ");
				sql.append(l.toString());
				sql.append(" ");
			} else if (o instanceof FunctionOperand) {
				sql.append(writeFunctionOperand((FunctionOperand) o));
			}
		}
	}

	/* !MOD 20041208 DG added function operand handling */
	protected String writeFunctionOperand(FunctionOperand function) {
		StringBuffer ret = new StringBuffer();
		Function curFunction = function.getFunction();
		java.util.Vector params = function.getParams();

		if (curFunction instanceof IsEmptyFunction) {
			// get parameter
			ret.append("(");
			ret.append(params.get(0));
			ret.append(" IS NULL OR ");
			ret.append(params.get(0));
			ret.append(" = '')");
		}
		return ret.toString();
	}

	protected Condition rewriteCondition(Condition c) {
		// check if this is a like query -- rewrite parts of it
		if ((c.getOperator().getType() == CompareOperator.TYPE_NOTLIKE) || (c.getOperator().getType() == CompareOperator.TYPE_LIKE)) {
			boolean changed = false;

			Operand rightOperand = c.getRightOperand();

			if (rightOperand instanceof StringOperand) {
				StringOperand sOp = (StringOperand) rightOperand;
				String newValue = sOp.getValue().replace('*', '%');

				rightOperand = new StringOperand(newValue);
				changed = true;
			}

			Operand leftOperand = c.getLeftOperand();

			if (leftOperand instanceof StringOperand) {
				StringOperand sOp = (StringOperand) leftOperand;
				String newValue = sOp.getValue().replace('*', '%');

				leftOperand = new StringOperand(newValue);
				changed = true;
			}

			// if an operand has changed, create new condition
			if (changed) {
				c = new Condition(leftOperand, rightOperand, c.getOperator());
			}
		}

		return c;
	}

	public void setRuleTree(RuleTree ruleTree) {
		this.ruleTree = ruleTree;
	}

	public void setAttributeNames(String names[]) {
		this.attributNames = names;
	}

	public String[] getAttributeNames() {
		return this.attributNames;
	}

	public String getTable() {
		return table;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {
		// default implementation ignores extra parameters
		StringBuffer sql = new StringBuffer(500);

		sql.append("SELECT * FROM ");
		sql.append(this.table);
		sql.append(" WHERE ");
		ruleToSqlCondition(this.ruleTree, sql);
		if (sortBy != null) {
			sql.append(" ORDER BY ");
			sql.append(sortBy);
			if (sortOrder == SORTORDER_DESC) {
				sql.append(" DESC");
			} else if (sortOrder == SORTORDER_ASC) {
				sql.append(" ASC");
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(sql);
		}
		SimpleResultProcessor resultProc = new SimpleResultProcessor();
		SQLDatasourceRecordSet res = new SQLDatasourceRecordSet(resultProc);

		return res;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getHandlePool()
	 */
	public HandlePool getHandlePool() {
		// TODO Auto-generated method stub
		return new SimpleHandlePool(this.handle);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("if you have time, please implement me!");
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		// FIXME implement this
		throw new FilterGeneratorException("Not yet implemented");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start, int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		// FIXME implement this
		throw new DatasourceException("Not yet implemented");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		// FIXME implement this
		throw new DatasourceException("Not yet implemented");
	}
}
