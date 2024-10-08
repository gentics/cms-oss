/*
 * @author herbert
 * @date Jul 8, 2008
 * @version $Id: HsqlContentRepositoryStructure.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ColumnDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ConstraintDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.IndexDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.SQLDatatype;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.TableDefinition;
import com.gentics.lib.db.DBHandle;

/**
 * Simple try to get HSQL to work with our nice content repository ..<br>
 * TODO clean me up ! - some code is duplicated (copied from
 * {@link MysqlContentRepositoryStructure} - sry, i was in a hurry!)
 */
public class HsqlContentRepositoryStructure extends AbstractContentRepositoryStructure {

	public HsqlContentRepositoryStructure(DBHandle handle, String handleId, boolean upperCase,
			boolean versioning, boolean multichannelling) {
		super(handle, handleId, upperCase, versioning, multichannelling);

		this.structureDefinition.setClobDatatype(new SQLDatatype(Types.LONGVARCHAR, "LONGVARCHAR ", 16277215, 255, true, false));
		this.structureDefinition.setBlobDatatype(new SQLDatatype(Types.LONGVARBINARY, "LONGVARBINARY", 2147483647, false, false));
		this.structureDefinition.setShorttextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR(32)", 32, true));
		this.structureDefinition.setTextDatatype(new SQLDatatype(Types.VARCHAR, "VARCHAR(255)", 255, true));
		this.structureDefinition.setTinyintegerDatatype(new SQLDatatype(Types.TINYINT, "TINYINT", 4, false));
		this.structureDefinition.setIntegerDatatype(new SQLDatatype(Types.INTEGER, "INTEGER", 11, false));
		this.structureDefinition.setLongDatatype(new SQLDatatype(Types.BIGINT, "BIGINT", 20, false));
		this.structureDefinition.setDoubleDatatype(new SQLDatatype(Types.DOUBLE, "DOUBLE", 22, false));
		this.structureDefinition.setDateDatatype(new SQLDatatype(Types.TIMESTAMP, "DATETIME", 19, false));

		this.canAutorepairIncorrectColumn = true;
		this.canAutorepairMissingColumn = true;
		this.canAutorepairMissingIndex = true;
		this.canAutorepairMissingTable = true;
		this.canAutorepairIndex = true;
	}
    
	protected String escapeIdentifier(String identifier) {
		return identifier;
	}
    
	protected String getColumnCreateStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(escapeIdentifier(columnDefinition.getColumnName())).append(' ');
		buffer.append(columnDefinition.getDataType().getSqlTypeName());

		if (columnDefinition.isAutoIncrement()) {
			buffer.append(" GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY");
		} else {
			if (columnDefinition.isNullable()) {
				buffer.append(" DEFAULT NULL");
			} else {
				buffer.append(" DEFAULT '").append(columnDefinition.getDefaultValue()).append("' NOT NULL");
			}
		}

		return buffer.toString();
	}

	protected List<String> getTableCreateStatement(TableDefinition tableDefinition) {
		StringBuffer tableCreateStatement = new StringBuffer();
		String createTable = "CREATE TABLE ";

		if (ObjectTransformer.getBoolean(System.getProperty("com.gentics.autorepair2.hsql.createcachedtables"), false)) {
			createTable = "CREATE CACHED TABLE ";
		}
		tableCreateStatement.append(createTable).append(escapeIdentifier(tableDefinition.getTableName())).append(" (");
		for (Iterator<ColumnDefinition> iterator = tableDefinition.getColumns().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = (ColumnDefinition) iterator.next();

			tableCreateStatement.append("\n").append(getColumnCreateStatement(columnDefinition));
			if (iterator.hasNext()) {
				tableCreateStatement.append(",");
			}
		}
		
		for (IndexDefinition indexDefinition : tableDefinition.getIndices().values()) {
			if (indexDefinition.getColumnDefinitions().size() == 1 && indexDefinition.getColumnDefinitions().values().iterator().next().autoIncrement) {
				continue;
			}
			
			String constraintDefinition = getIndexCreateStatement(indexDefinition);
			
			if (constraintDefinition.length() > 0) {
				tableCreateStatement.append(",\n");
				tableCreateStatement.append(constraintDefinition);
			}
		}
		
		tableCreateStatement.append(");");

		List<String> returnVector = new Vector<String>();

		returnVector.add(tableCreateStatement.toString());

		for (IndexDefinition indexDefinition : tableDefinition.getIndices().values()) {
			String createIndexStatement = getIndexStatement(indexDefinition);

			if (createIndexStatement.length() > 0) {
				returnVector.add(createIndexStatement);
			}
		}
        
		return returnVector;
	}
    
	protected String getIndexStatement(IndexDefinition indexDefinition) {
		if (indexDefinition.primary || indexDefinition.unique) {
			return "";
		}
    	
		StringBuffer buffer = new StringBuffer();
    	
		buffer.append("CREATE INDEX ").append(indexDefinition.getIndexName()).append(" ON ");
		buffer.append(escapeIdentifier(indexDefinition.getTableName())).append(' ');
		buffer.append(constructColumnList(indexDefinition)).append(';');
         
		return buffer.toString();
	}
    
	protected String getIndexCreateStatement(IndexDefinition indexDefinition) {
		if (!indexDefinition.primary && !indexDefinition.unique) {
			return "";
		}
    	
		StringBuffer buffer = new StringBuffer();
        
		if (indexDefinition.indexName != null) {
			String name = indexDefinition.uppercase ? escapeIdentifier(indexDefinition.indexName).toUpperCase() : escapeIdentifier(indexDefinition.indexName);

			buffer.append("CONSTRAINT ").append(name).append(' ');
		}
        
		if (indexDefinition.isPrimary()) {
			buffer.append("PRIMARY KEY ").append(constructColumnList(indexDefinition));
		} else {
			if (indexDefinition.isUnique()) {
				buffer.append("UNIQUE ").append(constructColumnList(indexDefinition));
			}
			// buffer.append("KEY ").append(escapeIdentifier(indexDefinition.getIndexName())).append(" (");
		}

		// for (Iterator<ColumnDefinition> iterator = indexDefinition.getColumnDefinitions().values().iterator(); iterator.hasNext();) {
		// ColumnDefinition columnDefinition = (ColumnDefinition) iterator.next();
		// buffer.append(escapeIdentifier(columnDefinition.getColumnName()));
		// if (columnDefinition.getDataType().needsKeyLength()) {
		// buffer.append("(").append(columnDefinition.getDataType().getKeyLength())
		// .append(")");
		// }
		// if (iterator.hasNext()) {
		// buffer.append(", ");
		// }
		// }
		// buffer.append(")");

		return buffer.toString();
	}
    
	private String constructColumnList(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer("(");
    	
		for (Iterator<ColumnDefinition> iterator = indexDefinition.getColumnDefinitions().values().iterator(); iterator.hasNext();) {
			ColumnDefinition columnDefinition = (ColumnDefinition) iterator.next();

			buffer.append(escapeIdentifier(columnDefinition.getColumnName()));
			if (columnDefinition.getDataType().needsKeyLength()) {
				buffer.append("(").append(columnDefinition.getDataType().getKeyLength()).append(")");
			}
			if (iterator.hasNext()) {
				buffer.append(", ");
			}
		}
		buffer.append(")");
        
		return buffer.toString();
	}

	protected List<String> getIndexAddStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		// String indexCreateStatement = getIndexCreateStatement(indexDefinition);
		if (indexDefinition.isPrimary()) {
			buffer.append("ALTER TABLE ").append(escapeIdentifier(indexDefinition.getTableName())).append(" ADD CONSTRAINT ").append(indexDefinition.getIndexName()).append(
					" PRIMARY KEY ");
		} else {
			buffer.append("CREATE ");
			if (indexDefinition.isUnique()) {
				buffer.append(" UNIQUE ");
			}
			buffer.append(" INDEX ").append(indexDefinition.getIndexName()).append(" ON ");
			buffer.append(escapeIdentifier(indexDefinition.getTableName()));
			// buffer.append(indexCreateStatement);
		}
		buffer.append(" (");
		SortedMap<Integer, String> columnNames = indexDefinition.getColumnNames();

		for (Iterator<String> i = columnNames.values().iterator(); i.hasNext();) {
			String name = (String) i.next();

			buffer.append(escapeIdentifier(name));
			if (i.hasNext()) {
				buffer.append(',');
			}
		}
		buffer.append(')');

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	protected List<String> getColumnAlterStatement(ColumnDefinition oldColumn, ColumnDefinition newColumn) {
		StringBuffer buffer = new StringBuffer();

		if (!oldColumn.getColumnName().equals(newColumn.getColumnName())) {
			buffer.append("ALTER TABLE ").append(oldColumn.getTableName()).append(" ALTER COLUMN ").append(oldColumn.getColumnName()).append(" RENAME TO ").append(newColumn.getColumnName()).append(
					";");
		}
		buffer.append("ALTER TABLE ").append(oldColumn.getTableName()).append(" ALTER COLUMN ").append(getColumnCreateStatement(newColumn));

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}
    
	protected List<String> getIndexDropStatement(IndexDefinition indexDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(indexDefinition.getTableName()).append(" DROP");
		if (indexDefinition.isPrimary()) {
			buffer.append(" PRIMARY KEY");
		} else {
			buffer = new StringBuffer("DROP INDEX ").append(escapeIdentifier(indexDefinition.getIndexName()));
			// buffer.append(" INDEX ").append(escapeIdentifier(indexDefinition.getIndexName()));
		}

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	protected String getDefaultValue(String defaultValue) {
		// mssql returns a null default value as (NULL)
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

	@Override
	protected List<String> getColumnAddStatement(ColumnDefinition columnDefinition) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("ALTER TABLE ").append(columnDefinition.getTableName()).append(" ADD ").append(getColumnCreateStatement(columnDefinition));

		if (columnDefinition.isAutoIncrement()) {
			buffer.append(" PRIMARY KEY");
		}

		List<String> returnVector = new Vector<String>();

		returnVector.add(buffer.toString());
		return returnVector;
	}

	@Override
	protected List<String> getConstraintCreateStatements(
			ConstraintDefinition existing, ConstraintDefinition reference) {
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

}
