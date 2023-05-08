/*
 * @author alexander
 * @date 17.09.2007
 * @version $Id: MysqlContentRepositoryStructure.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
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
 * MySQL specific implementation for the content repository structure check
 * @author alexander
 */
public class MysqlContentRepositoryStructure extends AbstractContentRepositoryStructure {

	public MysqlContentRepositoryStructure(DBHandle handle, String handleId, boolean upperCase,
			boolean versioning, boolean multichannelling) {
		super(handle, handleId, upperCase, versioning, multichannelling);

		this.structureDefinition.setShorttextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR(32)", 32, true));
		this.structureDefinition.setTextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR(255)", 255, true));
		this.structureDefinition.setClobDatatype(new SQLDatatype(Types.LONGVARCHAR, "MEDIUMTEXT", 16277215, 255, true, false));
		this.structureDefinition.setTinyintegerDatatype(new SQLDatatype(Types.TINYINT, "TINYINT", 4, false));
		this.structureDefinition.setIntegerDatatype(new SQLDatatype(Types.INTEGER, "INTEGER", 11, false));
		this.structureDefinition.setLongDatatype(new SQLDatatype(Types.BIGINT, "BIGINT", 20, false));
		this.structureDefinition.setDoubleDatatype(new SQLDatatype(Types.DOUBLE, "DOUBLE", 22, false));
		this.structureDefinition.setDateDatatype(new SQLDatatype(Types.TIMESTAMP, "DATETIME", 19, false));
		this.structureDefinition.setBlobDatatype(new SQLDatatype(Types.LONGVARBINARY, "LONGBLOB", 2147483647, false, false));

		this.canAutorepairIncorrectColumn = true;
		this.canAutorepairMissingColumn = true;
		this.canAutorepairMissingIndex = true;
		this.canAutorepairMissingTable = true;
		this.canAutorepairIndex = true;
	}
    
	protected String escapeIdentifier(String identifier) {
		return "`" + identifier + "`";
	}

	protected String getColumnCreateStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(escapeIdentifier(columnDefinition.getColumnName())).append(' ');
		buffer.append(columnDefinition.getDataType().getSqlTypeName());

		if (!columnDefinition.isNullable()) {
			buffer.append(" NOT NULL");
		}
		if (columnDefinition.isAutoIncrement()) {
			buffer.append(" AUTO_INCREMENT");
		} else {
			String defaultValue = columnDefinition.getDefaultValue();

			if (defaultValue == null) {
				if (columnDefinition.isNullable()) {
					buffer.append(" DEFAULT NULL");
				}
			} else {
				buffer.append(" DEFAULT '").append(defaultValue).append("'");
			}
		}

		return buffer.toString();
	}

	protected String getIndexCreateStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		if (indexDefinition.isPrimary()) {
			buffer.append("PRIMARY KEY (");
		} else {
			if (indexDefinition.isUnique()) {
				buffer.append("UNIQUE ");
			}
			buffer.append("KEY ").append(escapeIdentifier(indexDefinition.getIndexName())).append(" (");
		}

		for (Iterator<ColumnDefinition> iterator = indexDefinition.getColumnDefinitions().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = (ColumnDefinition) iterator.next();

			buffer.append(escapeIdentifier(columnDefinition.getColumnName()));
			if (columnDefinition.getDataType().needsKeyLength()) {
				buffer.append("(").append(columnDefinition.getDataType().getKeyLength()).append(")");
			}
			if (iterator.hasNext()) {
				buffer.append(",");
			}
		}
		buffer.append(")");

		return buffer.toString();
	}

	protected List<String> getIndexAddStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();
		String indexCreateStatement = getIndexCreateStatement(indexDefinition);

		buffer.append("ALTER TABLE ");
		buffer.append(escapeIdentifier(indexDefinition.getTableName()));
		buffer.append(" ADD ");
		buffer.append(indexCreateStatement);

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	protected List<String> getTableCreateStatement(TableDefinition tableDefinition) {
		StringBuffer tableCreateStatement = new StringBuffer();

		tableCreateStatement.append("CREATE TABLE ").append(escapeIdentifier(tableDefinition.getTableName())).append(" (");
		for (Iterator<ColumnDefinition> iterator = tableDefinition.getColumns().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = (ColumnDefinition) iterator.next();

			tableCreateStatement.append("\n").append(getColumnCreateStatement(columnDefinition));
			if (iterator.hasNext()) {
				tableCreateStatement.append(",");
			}
		}
		for (IndexDefinition indexDefinition : tableDefinition.getIndices().values()) {
			tableCreateStatement.append(",\n");
			tableCreateStatement.append(getIndexCreateStatement(indexDefinition));
		}
		tableCreateStatement.append(") ENGINE=InnoDB DEFAULT charset=UTF8");

		List<String> returnVector = new Vector<String>();

		returnVector.add(tableCreateStatement.toString());
		return returnVector;
	}

	@Override
	protected List<String> getColumnAddStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(columnDefinition.getTableName()).append(" ADD ").append(getColumnCreateStatement(columnDefinition));

		if (columnDefinition.isAutoIncrement()) {
			buffer.append(", ADD PRIMARY KEY (").append(escapeIdentifier(columnDefinition.getColumnName())).append(")");
		}

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	@Override
	protected List<String> getColumnAlterStatement(ColumnDefinition oldColumn,
			ColumnDefinition newColumn) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(oldColumn.getTableName()).append(" CHANGE ").append(oldColumn.getColumnName()).append(" ").append(
				getColumnCreateStatement(newColumn));

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	@Override
	protected List<String> getIndexDropStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(indexDefinition.getTableName()).append(" DROP");
		if (indexDefinition.isPrimary()) {
			buffer.append(" PRIMARY KEY");
		} else {
			buffer.append(" INDEX ").append(escapeIdentifier(indexDefinition.getIndexName()));
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
			buffer.append("ALTER TABLE ").append(existing.getTableName()).append(" DROP FOREIGN KEY ").append(existing.getConstraintName());
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

	@Override
	protected String getDefaultValue(String defaultValue) {
		if ("NULL".equalsIgnoreCase(defaultValue)) {
			defaultValue = null;
		} else if ("'NULL'".equalsIgnoreCase(defaultValue)) {
			defaultValue = null;
		} else if ("\"NULL\"".equalsIgnoreCase(defaultValue)) {
			defaultValue = null;
		} else if ("''".equals(defaultValue)) {
			defaultValue = "";
		}
		return defaultValue;
	}
}
