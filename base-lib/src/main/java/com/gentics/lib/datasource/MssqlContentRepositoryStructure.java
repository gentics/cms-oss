/*
 * @author alexander
 * @date 18.09.2007
 * @version $Id: MssqlContentRepositoryStructure.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ColumnDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ConstraintDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.IndexDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.SQLDatatype;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.TableDefinition;
import com.gentics.lib.db.DBHandle;

/**
 * MSSQL specific implementation for the content repository structure check
 * @author alexander
 */
public class MssqlContentRepositoryStructure extends AbstractContentRepositoryStructure {

	public MssqlContentRepositoryStructure(DBHandle handle, String handleId, boolean upperCase,
			boolean versioning, boolean multichannelling) {
		super(handle, handleId, upperCase, versioning, multichannelling);

		this.structureDefinition.setShorttextDatatype(new SQLDatatype(Types.VARCHAR, "[nvarchar](32)", 32, 32, true, true, new SQLDatatype(-9, "[nvarchar](32)", 32, true)));
		this.structureDefinition.setTextDatatype(new SQLDatatype(Types.VARCHAR, "[nvarchar](255)", 255, 255, true, true, new SQLDatatype(-9, "[nvarchar](255)", 255, true)));
		this.structureDefinition.setClobDatatype(new SQLDatatype(Types.LONGVARCHAR, "[ntext]", 1073741823, 0, false, false, new SQLDatatype(-16, "[ntext]", 1073741823, false, false)));
		this.structureDefinition.setTinyintegerDatatype(new SQLDatatype(Types.TINYINT, "[tinyint]", 4, false));
		this.structureDefinition.setIntegerDatatype(new SQLDatatype(Types.INTEGER, "[int]", 10, false));
		this.structureDefinition.setLongDatatype(new SQLDatatype(Types.BIGINT, "[bigint]", 19, false));
		this.structureDefinition.setDoubleDatatype(
				new SQLDatatype(Types.FLOAT, "[float]", 53, 53, true, false, new SQLDatatype(Types.DOUBLE, "float", 15, false)));
		this.structureDefinition.setDateDatatype(new SQLDatatype(Types.TIMESTAMP, "[datetime]", 23, false));
		this.structureDefinition.setBlobDatatype(new SQLDatatype(Types.LONGVARBINARY, "[varbinary](max)", 2147483647, 0, false, false, new SQLDatatype(Types.VARBINARY, "[varbinary](max)", 2147483647, false, false)));

		this.canAutorepairIncorrectColumn = true;
		this.canAutorepairMissingColumn = true;
		this.canAutorepairMissingIndex = true;
		this.canAutorepairMissingTable = true;
		this.canAutorepairIndex = true;
	}

	private String getColumnCreateStatement(ColumnDefinition columnDefinition) {
		StringBuffer statement = new StringBuffer();

		statement.append("[").append(columnDefinition.getColumnName()).append("]  ").append(columnDefinition.getDataType().getSqlTypeName());
		if (!columnDefinition.isNullable()) {
			statement.append(" NOT NULL");
		}
		if (columnDefinition.isAutoIncrement()) {
			statement.append(" IDENTITY(1,1)");
		} else {
			String defaultValue = columnDefinition.getDefaultValue();

			if (defaultValue == null) {
				if (columnDefinition.isNullable()) {
					statement.append(" CONSTRAINT DF_").append(columnDefinition.getTableName()).append("_").append(columnDefinition.getColumnName());
					statement.append(" DEFAULT NULL");
				}
			} else {
				statement.append(" CONSTRAINT DF_").append(columnDefinition.getTableName()).append("_").append(columnDefinition.getColumnName());
				statement.append(" DEFAULT '").append(defaultValue).append("'");
			}
		}

		return statement.toString();
	}

	private String getIndexCreateStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		if (indexDefinition.isPrimary()) {
			buffer.append("ALTER TABLE ").append(indexDefinition.getTableName()).append(" ADD CONSTRAINT [").append(indexDefinition.getIndexName()).append(
					"] PRIMARY KEY CLUSTERED (");
		} else {
			buffer.append("CREATE ");
			if (indexDefinition.isUnique()) {
				buffer.append("UNIQUE ");
			}
			buffer.append("INDEX [").append(indexDefinition.getIndexName()).append("] ON [").append(indexDefinition.getTableName()).append("] (");
		}
		for (Iterator<String> iterator = indexDefinition.getColumnNames().values().iterator(); iterator.hasNext();) {
			String columnName = (String) iterator.next();

			buffer.append("[").append(columnName).append("]");
			if (iterator.hasNext()) {
				buffer.append(",");
			}
		}
		buffer.append(")");
		if (indexDefinition.isPrimary()) {
			buffer.append(" WITH (PAD_INDEX = OFF, IGNORE_DUP_KEY = OFF) ON [PRIMARY]");
		}
		return buffer.toString();

	}

	@Override
	protected List<String> getTableCreateStatement(TableDefinition tableDefinition) {
		List<String> preStatement = new Vector<String>();
		List<String> statement = new Vector<String>();
		List<String> postStatement = new Vector<String>();

		preStatement.add("SET ANSI_NULLS ON");
		preStatement.add("SET QUOTED_IDENTIFIER ON");
		preStatement.add("SET ANSI_PADDING ON");

		StringBuffer tableCreateStatement = new StringBuffer();

		tableCreateStatement.append("CREATE TABLE [dbo].[").append(tableDefinition.getTableName()).append("] (");

		for (Iterator<ColumnDefinition> iterator = tableDefinition.getColumns().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = iterator.next();

			tableCreateStatement.append(getColumnCreateStatement(columnDefinition));
			if (iterator.hasNext()) {
				tableCreateStatement.append(",");
			}
		}

		for (IndexDefinition indexDefinition : tableDefinition.getIndices().values()) {
			postStatement.add(getIndexCreateStatement(indexDefinition));
		}
		tableCreateStatement.append("\n) ON [PRIMARY]\n");

		statement.add(tableCreateStatement.toString());

		List<String> returnVector = new Vector<String>();

		returnVector.addAll(preStatement);
		returnVector.addAll(statement);
		returnVector.addAll(postStatement);

		return returnVector;
	}

	@Override
	protected List<String> getColumnAddStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(columnDefinition.getTableName()).append(" ADD ").append(getColumnCreateStatement(columnDefinition));

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	@Override
	protected List<String> getColumnAlterStatement(ColumnDefinition oldColumn, ColumnDefinition newColumn) {
		StringBuffer buffer = new StringBuffer();
		List<String> returnVector = new Vector<String>();

		boolean isDefaultTypeDifferent = newColumn.getDefaultValue() != oldColumn.getDefaultValue();
		boolean isQuickColumn = newColumn.getColumnName().startsWith("quick");

		// drop constraint if old column was not nullable and had constraint or when the default value changes (for quick columns only)
		if (!oldColumn.isNullable() || (isDefaultTypeDifferent && isQuickColumn)) {
			buffer.append("ALTER TABLE ").append(oldColumn.getTableName()).append(" DROP CONSTRAINT DF_").append(oldColumn.getTableName())
					.append("_").append(oldColumn.getColumnName());
			returnVector.add(buffer.toString());
		}

		// alter column
		buffer = new StringBuffer();
		buffer.append("ALTER TABLE ").append(oldColumn.getTableName()).append(" ALTER COLUMN ").append("[").append(newColumn.getColumnName())
				.append("]  ").append(newColumn.getDataType().getSqlTypeName());
		if (newColumn.isAutoIncrement()) {
			buffer.append(" IDENTITY(1,1)");
		}
		if (!newColumn.isNullable()) {
			buffer.append(" NOT NULL");
		} else {
			buffer.append(" NULL ");
		}
		returnVector.add(buffer.toString());

		if (!newColumn.isNullable() && !newColumn.isAutoIncrement() || (isDefaultTypeDifferent && isQuickColumn)) {
			buffer = new StringBuffer();
			buffer.append("ALTER TABLE ").append(newColumn.getTableName()).append(" ADD CONSTRAINT DF_").append(newColumn.getTableName()).append("_")
					.append(newColumn.getColumnName()).append(" DEFAULT '").append(newColumn.getDefaultValue()).append("' FOR ")
					.append(newColumn.getColumnName());
			returnVector.add(buffer.toString());
		}
		return returnVector;
	}

	@Override
	protected List<String> getIndexAddStatement(IndexDefinition indexDefinition) {
		List<String> returnVector = new Vector<String>();

		returnVector.add(getIndexCreateStatement(indexDefinition));
		return returnVector;
	}

	@Override
	protected String getDefaultValue(String defaultValue) {
		// mssql returns a null default value as (NULL)
		if ("(NULL)".equals(defaultValue)) {
			defaultValue = null;
		}
		// mssql returns default values like (('0'))
		// strip all unnecessary characters
		if (defaultValue != null) {
			defaultValue = defaultValue.replaceAll("\\(", "").replaceAll("\\)", "").trim();
			defaultValue = defaultValue.replaceAll("'", "");
		}
		return defaultValue;
	}

	@Override
	protected List<String> getIndexDropStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		if (indexDefinition.isPrimary() || indexDefinition.isUnique()) {
			buffer.append("ALTER TABLE ").append(indexDefinition.getTableName()).append(" DROP CONSTRAINT ").append(indexDefinition.getIndexName());
		} else {
			buffer.append("DROP INDEX ").append(indexDefinition.getTableName()).append(".").append(indexDefinition.getIndexName());
		}

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	@Override
	protected List<String> getConstraintCreateStatements(ConstraintDefinition existing, ConstraintDefinition reference) {
		List<String> statements = new Vector<String>();
		StringBuffer buffer = new StringBuffer();

		if (existing != null) {
			buffer.append("ALTER TABLE ").append(existing.getTableName()).append(" DROP CONSTRAINT ").append(existing.getConstraintName());
			statements.add(buffer.toString());
			buffer.delete(0, buffer.length());
		}

		if (reference != null) {
			buffer.append("ALTER TABLE ").append(reference.getTableName()).append(" ADD CONSTRAINT ").append(reference.getConstraintName());
			buffer.append(" FOREIGN KEY ").append(" (").append(reference.getColumnName()).append(")");
			buffer.append(" REFERENCES ").append(reference.getForeignTableName()).append(" (").append(reference.getForeignColumnName()).append(")");
			if (reference.getOnDelete() != null) {
				buffer.append(" ON DELETE ").append(reference.getOnDelete());
			}
			if (reference.getOnUpdate() != null) {
				buffer.append(" ON UPDATE ").append(reference.getOnUpdate());
			}
			statements.add(buffer.toString());
		}

		return statements;
	}
}
