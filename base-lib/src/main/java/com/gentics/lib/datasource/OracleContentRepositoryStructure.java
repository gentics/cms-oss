/*
 * @author alexander
 * @date 18.09.2007
 * @version $Id: OracleContentRepositoryStructure.java,v 1.1.4.1 2011-04-07 09:57:48 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ColumnDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ConstraintDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.IndexDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.SQLDatatype;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.TableDefinition;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;

/**
 * Oracle specific implementation for the content repository structure check
 * @author alexander
 */
public class OracleContentRepositoryStructure extends AbstractContentRepositoryStructure {

	public OracleContentRepositoryStructure(DBHandle handle, String handleId,
			boolean upperCase, boolean versioning, boolean multichannelling) {
		super(handle, handleId, upperCase, versioning, multichannelling);

		this.structureDefinition.setShorttextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR2(32)", 32, true));
		this.structureDefinition.setTextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR2(255)", 255, true));
		this.structureDefinition.setClobDatatype(new SQLDatatype(Types.CLOB, "CLOB", 4000, false, false));
		this.structureDefinition.setTinyintegerDatatype(new SQLDatatype(Arrays.asList(Types.DECIMAL, Types.NUMERIC), "NUMBER(4,0)", 4, false));
		this.structureDefinition.setIntegerDatatype(new SQLDatatype(Arrays.asList(Types.DECIMAL, Types.NUMERIC), "NUMBER(11,0)", 11, false));
		this.structureDefinition.setLongDatatype(new SQLDatatype(Arrays.asList(Types.DECIMAL, Types.NUMERIC), "NUMBER(20,0)", 20, false));
		this.structureDefinition.setDoubleDatatype(new SQLDatatype(Arrays.asList(Types.DECIMAL, Types.NUMERIC), "NUMBER", 22, false));
		this.structureDefinition.setDateDatatype(new SQLDatatype(Types.DATE, "DATE", 7, 7, true, false, new SQLDatatype(Types.TIMESTAMP, "DATE", 7, false)));
		this.structureDefinition.setBlobDatatype(new SQLDatatype(Types.BLOB, "BLOB", 4000, false, false));

		this.canAutorepairIncorrectColumn = true;
		this.canAutorepairMissingColumn = true;
		this.canAutorepairMissingIndex = true;
		this.canAutorepairMissingTable = true;
		this.canAutorepairIndex = true;
	}

	protected List<String> getColumnAddStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		Object[] columnCreateStatement = getColumnCreateStatement(columnDefinition, null);

		buffer.append("ALTER TABLE ").append(columnDefinition.getTableName()).append(" ADD ");
		buffer.append(columnCreateStatement[0]);

		List<String> returnStatements = new Vector<String>();

		returnStatements.add(buffer.toString());
		returnStatements.addAll((List<String>) columnCreateStatement[1]);

		return returnStatements;
	}

	protected List<String> getColumnAlterStatement(ColumnDefinition oldColumn,
			ColumnDefinition newColumn) {
		StringBuffer buffer = new StringBuffer();
		Object[] columnCreateStatements = getColumnCreateStatement(newColumn, oldColumn);
		String columnCreateStatement = (String) columnCreateStatements[0];

		buffer.append("ALTER TABLE \"").append(oldColumn.getTableName()).append("\" MODIFY (").append(columnCreateStatement).append(")");

		List<String> returnStatements = new Vector<String>();

		returnStatements.add(buffer.toString());
		returnStatements.addAll((List<String>) columnCreateStatements[1]);

		return returnStatements;
	}

	protected List<String> getIndexAddStatement(IndexDefinition indexDefinition) {
		List<String> returnVector = new Vector<String>();

		returnVector.add(getIndexCreateStatement(indexDefinition));
		return returnVector;
	}

	protected List<String> getTableCreateStatement(TableDefinition tableDefinition) {
		StringBuffer statement = new StringBuffer();
		List<String> postStatement = new Vector<String>();

		statement.append("CREATE TABLE \"").append(tableDefinition.getTableName()).append("\" (\n");
		for (Iterator<ColumnDefinition> iterator = tableDefinition.getColumns().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = iterator.next();
			Object[] columnCreateStatement = getColumnCreateStatement(columnDefinition, null);

			statement.append((String) columnCreateStatement[0]);
			postStatement.addAll((List<String>) columnCreateStatement[1]);
			if (iterator.hasNext()) {
				statement.append(",\n");
			}
		}
		for (Iterator<IndexDefinition> iterator = tableDefinition.getIndices().values().iterator(); iterator.hasNext();) {
			IndexDefinition indexDefinition = iterator.next();
			String indexCreateStatement = getIndexCreateStatement(indexDefinition);

			postStatement.add(indexCreateStatement);
		}
		statement.append(")");

		List<String> returnVector = new Vector<String>();

		returnVector.add(statement.toString());
		returnVector.addAll(postStatement);

		return returnVector;
	}

	private Object[] getColumnCreateStatement(ColumnDefinition columnDefinition,
			ColumnDefinition oldColumnDefinition) {
		StringBuffer statement = new StringBuffer();
		List<String> postStatement = new Vector<String>();

		statement.append("\"").append(columnDefinition.getColumnName()).append("\" ").append(columnDefinition.getDataType().getSqlTypeName());
		if (columnDefinition.isAutoIncrement()) {
			String tableName = columnDefinition.getTableName();
			String triggerName = tableName + "_trigger";
			String sequenceName = tableName + "_sequence";

			if (upperCase) {
				triggerName = triggerName.toUpperCase();
				sequenceName = sequenceName.toUpperCase();
			}

			StringBuffer sequenceBuffer = new StringBuffer();

			sequenceBuffer.append("CREATE SEQUENCE \"").append(sequenceName).append(
					"\" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE");
			postStatement.add(sequenceBuffer.toString());

			StringBuffer triggerBuffer = new StringBuffer();

			triggerBuffer.append("CREATE OR REPLACE TRIGGER \"").append(triggerName).append("\"\nBEFORE INSERT ON ").append(tableName).append(" REFERENCING NEW AS NEW FOR EACH ROW BEGIN SELECT ").append(sequenceName).append(
					".nextval INTO :NEW.ID FROM dual; END;");
			postStatement.add(triggerBuffer.toString());
			StringBuffer triggerEnableBuffer = new StringBuffer();

			triggerEnableBuffer.append("ALTER TRIGGER \"").append(triggerName).append("\" ENABLE");
			postStatement.add(triggerEnableBuffer.toString());
		}

		if (!columnDefinition.isAutoIncrement()) {
			// check whether the default value is different than the one of the old column definition (if any)
			boolean addDefault = true;
			String defaultValue = columnDefinition.getDefaultValue();

			if (oldColumnDefinition != null) {
				if (StringUtils.isEqual(defaultValue, oldColumnDefinition.getDefaultValue())) {
					addDefault = false;
				}
			}
			if (addDefault) {
				if (defaultValue == null) {
					if (columnDefinition.isNullable()) {
						statement.append(" DEFAULT NULL");
					}
				} else {
					statement.append(" DEFAULT '").append(defaultValue).append("'");
				}
			}

			// check whether the "NOT NULL" clause shall be added
			boolean addNotNull = !columnDefinition.isNullable();

			if (oldColumnDefinition != null) {
				if (oldColumnDefinition.isNullable() == columnDefinition.isNullable()) {
					addNotNull = false;
				}
			}
			if (addNotNull) {
				statement.append(" NOT NULL");
			}
		}

		return new Object[] { statement.toString(), postStatement};
	}

	private String getIndexCreateStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		if (indexDefinition.isPrimary()) {
			buffer.append("ALTER TABLE \"" + indexDefinition.getTableName() + "\" ADD CONSTRAINT \"" + indexDefinition.getIndexName() + "\" PRIMARY KEY (");
		} else {
			buffer.append("CREATE ");
			if (indexDefinition.isUnique()) {
				buffer.append("UNIQUE ");
			}
			buffer.append("INDEX \"").append(indexDefinition.getIndexName()).append("\" ON \"").append(indexDefinition.getTableName()).append("\" (");
		}
		for (Iterator<String> iterator = indexDefinition.getColumnNames().values().iterator(); iterator.hasNext();) {
			String columnName = (String) iterator.next();

			buffer.append("\"").append(columnName).append("\"");
			if (iterator.hasNext()) {
				buffer.append(",");
			}
		}
		buffer.append(")");
		if (indexDefinition.isPrimary()) {
			buffer.append(" ENABLE");
		}

		return buffer.toString();
	}

	protected String getSchemaName(DatabaseMetaData databaseMetaData, boolean uppercase) {
		try {
			String tableSchema = databaseMetaData.getUserName();

			if (upperCase) {
				tableSchema = tableSchema.toUpperCase();
			}

			return tableSchema;
		} catch (SQLException sqle) {
			return null;
		}
	}

	protected String getDefaultValue(String defaultValue) {
		if (defaultValue != null) {
			defaultValue = defaultValue.trim();
		}
		if ("NULL".equalsIgnoreCase(defaultValue)) {
			defaultValue = null;
		}
		if (defaultValue != null) {
			defaultValue = defaultValue.trim();
			defaultValue = defaultValue.replaceAll("'", "");
		}
		return defaultValue;
	}

	protected List<String> getIndexDropStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		if (indexDefinition.isPrimary()) {
			buffer.append("ALTER TABLE ").append(indexDefinition.getTableName()).append(" DROP PRIMARY KEY");
		} else {
			buffer.append("DROP INDEX \"").append(indexDefinition.getIndexName()).append("\"");
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
			statements.add(buffer.toString());
		}

		return statements;
	}
}
