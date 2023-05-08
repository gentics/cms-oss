/*
 * @author alexander
 * @date 09.05.2008
 * @version $Id: ContentRepositoryStructureDefinition.java,v 1.1.4.2 2011-04-07 09:57:48 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * This class defines the structure of a content repository in a database
 * independent way.
 * @author alexander
 */
public class ContentRepositoryStructureDefinition {

	/**
	 * A log4j logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ContentRepositoryStructureDefinition.class);

	/**
	 * The default datatype for a short text
	 */
	protected SQLDatatype shorttextDatatype = new SQLDatatype(Types.VARCHAR, "VARCHAR(32)", 32, false);

	/**
	 * The default datatype for a normal text
	 */
	protected SQLDatatype textDatatype = new SQLDatatype(Types.VARCHAR, "VARCHAR(255)", 255, false);

	/**
	 * The default datatype for a long text
	 */
	protected SQLDatatype clobDatatype = new SQLDatatype(Types.LONGVARCHAR, "LONGVARCHAR", 0, false);

	/**
	 * The default datatype for a tiny integer
	 */
	protected SQLDatatype tinyintegerDatatype = new SQLDatatype(Types.TINYINT, "TINYINT", 4, false);

	/**
	 * The default datatype for an integer
	 */
	protected SQLDatatype integerDatatype = new SQLDatatype(Types.INTEGER, "INTEGER", 11, false);

	/**
	 * The default datatye for a long
	 */
	protected SQLDatatype longDatatype = new SQLDatatype(Types.BIGINT, "BIGINT", 20, false);

	/**
	 * The default datatype for a double
	 */
	protected SQLDatatype doubleDatatype = new SQLDatatype(Types.DOUBLE, "DOUBLE", 22, false);

	/**
	 * The default datatye for a date
	 */
	protected SQLDatatype dateDatatype = new SQLDatatype(Types.TIMESTAMP, "TIMESTAMP", 0, false);

	/**
	 * The default datatype for binary content
	 */
	protected SQLDatatype blobDatatype = new SQLDatatype(Types.BLOB, "BLOB", 0, false);

	/**
	 * The default name of the "contentattribute" table
	 */
	protected String contentAttributeName = "contentattribute";

	/**
	 * The default name of the "contentattribute_nodeversion" table
	 */
	protected String contentAttributeNodeversionName = "contentattribute_nodeversion";

	/**
	 * The default name of the "contentattributetype" table
	 */
	protected String contentAttributeTypeName = "contentattributetype";

	/**
	 * The default name of the "object_attribute" table
	 */
	protected String objectAttributeName = "object_attribute";

	/**
	 * The default name of the "contentmap" table
	 */
	protected String contentMapName = "contentmap";

	/**
	 * The default name of the "contentmap_nodeversion" table
	 */
	protected String contentMapNodeversionName = "contentmap_nodeversion";

	/**
	 * The default name of the "contentobject" table
	 */
	protected String contentObjectName = "contentobject";

	/**
	 * The default name of the "contentstatus" table
	 */
	protected String contentStatusName = "contentstatus";

	/**
	 * The default name of the "channel" table
	 */
	protected String channelName = "channel";

	/**
	 * True if the database system uses uppercase identifiers
	 */
	protected boolean useUppercaseIdentifiers = false;

	/**
	 * True to use versioning (i.e. check and create the "_nodeversion" tables)
	 */
	protected boolean versioning = false;

	/**
	 * True to make the content repository multichannelling aware
	 */
	protected boolean multichannelling = false;

	/**
	 * Create a new content repository definition
	 * @param databaseHandle the database handle for which to create the
	 *        definition (reads tablenames from the handle)
	 * @param useUppercaseIdentifiers true to only use uppercase identifiers
	 * @param versioning true to create and check "_nodeversion" tables
	 * @param multichannelling true make the content repository multichannelling aware
	 */
	public ContentRepositoryStructureDefinition(DBHandle databaseHandle,
			boolean useUppercaseIdentifiers, boolean versioning, boolean multichannelling) {
		this.useUppercaseIdentifiers = useUppercaseIdentifiers;
		// TODO check that versioning and multichannelling cannot both be true
		this.versioning = versioning;
		this.multichannelling = multichannelling;

		// read custom table names from handle
		contentAttributeName = databaseHandle.getContentAttributeName();
		contentAttributeNodeversionName = databaseHandle.getContentAttributeName() + "_nodeversion";
		contentAttributeTypeName = databaseHandle.getContentAttributeTypeName();
		contentMapName = databaseHandle.getContentMapName();
		contentMapNodeversionName = databaseHandle.getContentMapName() + "_nodeversion";
		contentObjectName = databaseHandle.getContentObjectName();
		contentStatusName = databaseHandle.getContentStatusName();

		if (useUppercaseIdentifiers) {
			contentAttributeName = contentAttributeName.toUpperCase();
			contentAttributeNodeversionName = contentAttributeNodeversionName.toUpperCase();
			contentAttributeTypeName = contentAttributeTypeName.toUpperCase();
			contentMapName = contentMapName.toUpperCase();
			contentMapNodeversionName = contentMapNodeversionName.toUpperCase();
			contentObjectName = contentObjectName.toUpperCase();
			contentStatusName = contentStatusName.toUpperCase();
			objectAttributeName = objectAttributeName.toUpperCase();
			channelName = channelName.toUpperCase();
		}
	}

	/**
	 * Set the sql data type for a short text
	 * @param sqlDataType the data type used for short text
	 */
	public void setShorttextDatatype(SQLDatatype sqlDataType) {
		shorttextDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for a short text
	 * @return the sql data type for a short text
	 */
	public SQLDatatype getShorttextDatatype() {
		return shorttextDatatype;
	}

	/**
	 * Set the sql data type for a normal text
	 * @param sqlDataType the sql data type for a normal text
	 */
	public void setTextDatatype(SQLDatatype sqlDataType) {
		textDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for a normal text
	 * @return the sql data type for a normal text
	 */
	public SQLDatatype getTextDatatype() {
		return textDatatype;
	}

	/**
	 * Set the sql data type for a long text
	 * @param sqlDataType the sql data type for a long text
	 */
	public void setClobDatatype(SQLDatatype sqlDataType) {
		clobDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for a long text
	 * @return the sql data type for a long text
	 */
	public SQLDatatype getClobDatatype() {
		return clobDatatype;
	}

	/**
	 * Set the sql data type for tiny intefers
	 * @param sqlDataType the sql data type for tiny integers
	 */
	public void setTinyintegerDatatype(SQLDatatype sqlDataType) {
		tinyintegerDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for tiny integers
	 * @return the sql data type for tiny integers
	 */
	public SQLDatatype getTinyintegerDatatype() {
		return tinyintegerDatatype;
	}

	/**
	 * Set the sql data type for integers
	 * @param sqlDataType the sql data type for integers
	 */
	public void setIntegerDatatype(SQLDatatype sqlDataType) {
		integerDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for integers
	 * @return the sql data type for integers
	 */
	public SQLDatatype getIntegerDatatype() {
		return integerDatatype;
	}

	/**
	 * Set the sql data type for longs
	 * @param sqlDataType the sql data type for longs
	 */
	public void setLongDatatype(SQLDatatype sqlDataType) {
		longDatatype = sqlDataType;
	}

	/**
	 * Get the sql datatype for longs
	 * @return the sql data type for longs
	 */
	public SQLDatatype getLongDatatype() {
		return longDatatype;
	}

	/**
	 * Set the sql data type for doubles
	 * @param sqlDataType the sql data type for doubles
	 */
	public void setDoubleDatatype(SQLDatatype sqlDataType) {
		doubleDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for doubles
	 * @return the sql data type for doubles
	 */
	public SQLDatatype getDoubleDatatype() {
		return doubleDatatype;
	}

	/**
	 * Set the sql data type for dates
	 * @param sqlDataType the sql data type for dates
	 */
	public void setDateDatatype(SQLDatatype sqlDataType) {
		dateDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for dates
	 * @return the sql data type for dates
	 */
	public SQLDatatype getDateDatatype() {
		return dateDatatype;
	}

	/**
	 * Set the sql data type for binary data
	 * @param sqlDataType the sql data type for binary data
	 */
	public void setBlobDatatype(SQLDatatype sqlDataType) {
		blobDatatype = sqlDataType;
	}

	/**
	 * Get the sql data type for binary data
	 * @return the sql data type for binary data
	 */
	public SQLDatatype getBlobDatatype() {
		return blobDatatype;
	}

	/**
	 * Get all needed tables (reference structure)
	 * @return the reference table structure
	 */
	public Map<String, TableDefinition> getReferenceTables() {
		Map<String, TableDefinition> referenceTables = new LinkedHashMap<String, TableDefinition>();

		if (multichannelling) {
			referenceTables.put(channelName, getChannelTable());
			referenceTables.put(contentObjectName, getMCContentObject());
			referenceTables.put(contentAttributeTypeName, getMCContentAttributeType());
			referenceTables.put(objectAttributeName, getMCObjectAttribute());
			referenceTables.put(contentMapName, getMCContentMap());
			referenceTables.put(contentAttributeName, getMCContentAttribute());
		} else {
			referenceTables.put(contentObjectName, getContentObject());
			referenceTables.put(contentAttributeTypeName, getContentAttributeType());
			referenceTables.put(contentMapName, getContentMap());
			referenceTables.put(contentAttributeName, getContentAttribute());
			referenceTables.put(contentStatusName, getContentStatus());
			if (versioning) {
				referenceTables.put(contentMapNodeversionName, getContentMapNodeversion());
				referenceTables.put(contentAttributeNodeversionName, getContentAttributeNodeversion());
			}
		}
		return referenceTables;
	}

	/**
	 * Get all needed constraints
	 * @return constraints
	 */
	public List<ConstraintDefinition> getReferenceConstraints() {
		List<ConstraintDefinition> constraints = new Vector<ContentRepositoryStructureDefinition.ConstraintDefinition>();

		if (multichannelling) {
			// object_attribute.object_type -> contentobject.type
			constraints.add(
					new ConstraintDefinition(objectAttributeName, "object_type", contentObjectName, "type", ConstraintAction.CASCADE, ConstraintAction.CASCADE,
					useUppercaseIdentifiers));
			// object_attribute.attribute_name -> contentattributetype.name
			constraints.add(
					new ConstraintDefinition(objectAttributeName, "attribute_name", contentAttributeTypeName, "name", ConstraintAction.CASCADE,
					ConstraintAction.CASCADE, useUppercaseIdentifiers));

			// contentmap.channel_id -> channel.id
			constraints.add(
					new ConstraintDefinition(contentMapName, "channel_id", channelName, "id", ConstraintAction.CASCADE, ConstraintAction.CASCADE,
					useUppercaseIdentifiers));
			// contentmap.obj_type -> contentobject.type
			constraints.add(
					new ConstraintDefinition(contentMapName, "obj_type", contentObjectName, "type", ConstraintAction.CASCADE, ConstraintAction.CASCADE,
					useUppercaseIdentifiers));

			// contentattribute.map_id -> contentmap.id
			constraints.add(
					new ConstraintDefinition(contentAttributeName, "map_id", contentMapName, "id", ConstraintAction.CASCADE, ConstraintAction.CASCADE,
					useUppercaseIdentifiers));
			// contentattribute.name -> contentattributetype.name
			constraints.add(
					new ConstraintDefinition(contentAttributeName, "name", contentAttributeTypeName, "name", ConstraintAction.CASCADE, ConstraintAction.CASCADE,
					useUppercaseIdentifiers));
		}

		return constraints;
	}

	/**
	 * Get the "contentattribute" table definition
	 * @return the "contentattribute" table definition
	 */
	public TableDefinition getContentAttribute() {
		ColumnDefinition id = new ColumnDefinition(contentAttributeName, "id", integerDatatype, false, null, true, true, useUppercaseIdentifiers);
		ColumnDefinition contentId = new ColumnDefinition(contentAttributeName, "contentid", shorttextDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition name = new ColumnDefinition(contentAttributeName, "name", textDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition valueText = new ColumnDefinition(contentAttributeName, "value_text", textDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueBin = new ColumnDefinition(contentAttributeName, "value_bin", blobDatatype, true, null, false, false, useUppercaseIdentifiers);
		ColumnDefinition valueInt = new ColumnDefinition(contentAttributeName, "value_int", integerDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition sortorder = new ColumnDefinition(contentAttributeName, "sortorder", integerDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueBlob = new ColumnDefinition(contentAttributeName, "value_blob", blobDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueClob = new ColumnDefinition(contentAttributeName, "value_clob", clobDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueLong = new ColumnDefinition(contentAttributeName, "value_long", longDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueDouble = new ColumnDefinition(contentAttributeName, "value_double", doubleDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueDate = new ColumnDefinition(contentAttributeName, "value_date", dateDatatype, true, null, false, true, useUppercaseIdentifiers);

		ColumnDefinition[] cols = null;

		if (multichannelling) {
			ColumnDefinition channelId = new ColumnDefinition(contentAttributeName, "channel_id", integerDatatype, false, "0", false, true,
					useUppercaseIdentifiers);

			cols = new ColumnDefinition[] {
				id, contentId, name, channelId, valueText, valueBin, valueInt, sortorder, valueBlob, valueClob, valueLong,
				valueDouble, valueDate};
		} else {
			cols = new ColumnDefinition[] {
				id, contentId, name, valueText, valueBin, valueInt, sortorder, valueBlob, valueClob, valueLong, valueDouble,
				valueDate};
		}

		IndexDefinition idx1 = new IndexDefinition(contentAttributeName, "contentattribute_idx1", new ColumnDefinition[] { id}, true, true,
				useUppercaseIdentifiers);
		IndexDefinition idx2 = new IndexDefinition(contentAttributeName, "contentattribute_idx2", new ColumnDefinition[] { contentId}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx3 = new IndexDefinition(contentAttributeName, "contentattribute_idx3", new ColumnDefinition[] { name}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentAttributeName, "contentattribute_idx4", new ColumnDefinition[] { contentId, name}, false, false,
				useUppercaseIdentifiers);

		TableDefinition contentAttribute = new TableDefinition(contentAttributeName, cols, new IndexDefinition[] { idx1, idx2, idx3, idx4 }, true,
				useUppercaseIdentifiers);

		return contentAttribute;
	}

	/**
	 * Get the "contentattribute" table definition for multichannelling content repositories
	 * @return the "contentattribute" table definition
	 */
	public TableDefinition getMCContentAttribute() {
		ColumnDefinition id = new ColumnDefinition(contentAttributeName, "id", integerDatatype, false, null, true, true, useUppercaseIdentifiers);
		ColumnDefinition mapId = new ColumnDefinition(contentAttributeName, "map_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition name = new ColumnDefinition(contentAttributeName, "name", textDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition sortorder = new ColumnDefinition(contentAttributeName, "sortorder", integerDatatype, true, null, false, true, useUppercaseIdentifiers);

		ColumnDefinition valueText = new ColumnDefinition(contentAttributeName, "value_text", textDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueInt = new ColumnDefinition(contentAttributeName, "value_int", integerDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueBlob = new ColumnDefinition(contentAttributeName, "value_blob", blobDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueClob = new ColumnDefinition(contentAttributeName, "value_clob", clobDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueLong = new ColumnDefinition(contentAttributeName, "value_long", longDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition valueDouble = new ColumnDefinition(contentAttributeName, "value_double", doubleDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueDate = new ColumnDefinition(contentAttributeName, "value_date", dateDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition updateTimestamp = new ColumnDefinition(contentAttributeName, "updatetimestamp", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		// TODO add shared_id

		ColumnDefinition[] cols = new ColumnDefinition[] {
			id, mapId, name, sortorder, valueText, valueInt, valueBlob, valueClob, valueLong, valueDouble,
			valueDate, updateTimestamp};

		IndexDefinition idx1 = new IndexDefinition(contentAttributeName, "contentattribute_idx1", new ColumnDefinition[] { id }, true, true,
				useUppercaseIdentifiers);
		IndexDefinition idx2 = new IndexDefinition(contentAttributeName, "contentattribute_idx2", new ColumnDefinition[] { mapId }, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx3 = new IndexDefinition(contentAttributeName, "contentattribute_idx3", new ColumnDefinition[] { name }, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentAttributeName, "contentattribute_idx4", new ColumnDefinition[] { mapId, name }, false, false,
				useUppercaseIdentifiers);

		TableDefinition contentAttribute = new TableDefinition(contentAttributeName, cols, new IndexDefinition[] { idx1, idx2, idx3, idx4 }, true,
				useUppercaseIdentifiers);

		return contentAttribute;
	}

	/**
	 * Get the "contentattribute_nodeversion" table definition
	 * @return the "contentattribute_nodeversion" table definition
	 */
	public TableDefinition getContentAttributeNodeversion() {
		ColumnDefinition id = new ColumnDefinition(contentAttributeNodeversionName, "id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition contentId = new ColumnDefinition(contentAttributeNodeversionName, "contentid", shorttextDatatype, false, "", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition name = new ColumnDefinition(contentAttributeNodeversionName, "name", textDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition valueText = new ColumnDefinition(contentAttributeNodeversionName, "value_text", textDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueBin = new ColumnDefinition(contentAttributeNodeversionName, "value_bin", blobDatatype, true, null, false, false,
				useUppercaseIdentifiers);
		ColumnDefinition valueInt = new ColumnDefinition(contentAttributeNodeversionName, "value_int", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition sortOrder = new ColumnDefinition(contentAttributeNodeversionName, "sortorder", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueBlob = new ColumnDefinition(contentAttributeNodeversionName, "value_blob", blobDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueClob = new ColumnDefinition(contentAttributeNodeversionName, "value_clob", clobDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueLong = new ColumnDefinition(contentAttributeNodeversionName, "value_long", longDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueDouble = new ColumnDefinition(contentAttributeNodeversionName, "value_double", doubleDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition valueDate = new ColumnDefinition(contentAttributeNodeversionName, "value_date", dateDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionTimestamp = new ColumnDefinition(contentAttributeNodeversionName, "nodeversiontimestamp", integerDatatype, true, null, false,
				true, useUppercaseIdentifiers);
		ColumnDefinition nodeversionUser = new ColumnDefinition(contentAttributeNodeversionName, "nodeversion_user", textDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionLatest = new ColumnDefinition(contentAttributeNodeversionName, "nodeversionlatest", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionRemoved = new ColumnDefinition(contentAttributeNodeversionName, "nodeversionremoved", integerDatatype, true, null, false,
				true, useUppercaseIdentifiers);
		ColumnDefinition nodeversionAutoupdate = new ColumnDefinition(contentAttributeNodeversionName, "nodeversion_autoupdate", tinyintegerDatatype, false, "0",
				false, true, useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx1", new ColumnDefinition[] { contentId},
				false, false, useUppercaseIdentifiers);
		IndexDefinition idx2 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx2", new ColumnDefinition[] { id}, false,
				false, useUppercaseIdentifiers);
		IndexDefinition idx3 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx3", new ColumnDefinition[] { name}, false,
				false, useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx4", new ColumnDefinition[] { sortOrder},
				false, false, useUppercaseIdentifiers);
		IndexDefinition idx5 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx5",
				new ColumnDefinition[] { contentId, name}, false, false, useUppercaseIdentifiers);
		IndexDefinition idx6 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx6",
				new ColumnDefinition[] { contentId, name, valueText}, false, false, useUppercaseIdentifiers);
		IndexDefinition idx7 = new IndexDefinition(contentAttributeNodeversionName, "contentattribute_nodeversion_idx7",
				new ColumnDefinition[] { id, nodeversionTimestamp}, false, false, useUppercaseIdentifiers);

		TableDefinition contentAttributeNodeversion = new TableDefinition(contentAttributeNodeversionName,
				new ColumnDefinition[] {
			id, contentId, name, valueText, valueBin, valueInt, sortOrder, valueBlob, valueClob, valueLong, valueDouble, valueDate,
			nodeversionTimestamp, nodeversionUser, nodeversionLatest, nodeversionRemoved, nodeversionAutoupdate},
				new IndexDefinition[] { idx1, idx2, idx3, idx4, idx5, idx6, idx7}, true, useUppercaseIdentifiers);

		return contentAttributeNodeversion;
	}

	/**
	 * Get the "contentattributetype" table definition
	 * @return the "contentattributetype" table definition
	 */
	public TableDefinition getContentAttributeType() {
		ColumnDefinition name = new ColumnDefinition(contentAttributeTypeName, "name", textDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition attributeType = new ColumnDefinition(contentAttributeTypeName, "attributetype", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition optimized = new ColumnDefinition(contentAttributeTypeName, "optimized", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition quickName = new ColumnDefinition(contentAttributeTypeName, "quickname", textDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition multiValue = new ColumnDefinition(contentAttributeTypeName, "multivalue", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition objectType = new ColumnDefinition(contentAttributeTypeName, "objecttype", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition linkedObjectType = new ColumnDefinition(contentAttributeTypeName, "linkedobjecttype", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition foreignLinkAttribute = new ColumnDefinition(contentAttributeTypeName, "foreignlinkattribute", textDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition foreignLinkAttributeRule = new ColumnDefinition(contentAttributeTypeName, "foreignlinkattributerule", clobDatatype, true, null, false,
				true, useUppercaseIdentifiers);
		ColumnDefinition excludeVersioning = new ColumnDefinition(contentAttributeTypeName, "exclude_versioning", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition filesystem = new ColumnDefinition(contentAttributeTypeName, "filesystem", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		ColumnDefinition[] cols = null;

		if (multichannelling) {
			ColumnDefinition searchable = new ColumnDefinition(contentAttributeTypeName, "searchable", integerDatatype, false, "0", false, true,
					useUppercaseIdentifiers);

			cols = new ColumnDefinition[] {
				name, attributeType, optimized, quickName, multiValue, objectType, linkedObjectType, foreignLinkAttribute,
				foreignLinkAttributeRule, excludeVersioning, filesystem, searchable};
		} else {
			cols = new ColumnDefinition[] {
				name, attributeType, optimized, quickName, multiValue, objectType, linkedObjectType, foreignLinkAttribute,
				foreignLinkAttributeRule, excludeVersioning, filesystem};
		}

		TableDefinition contentAttributeType = new TableDefinition(contentAttributeTypeName, cols, new IndexDefinition[] {}, true, useUppercaseIdentifiers);

		return contentAttributeType;
	}

	/**
	 * Get the "contentattributetype" table definition for multichannelling content repository
	 * @return the "contentattributetype" table definition
	 */
	public TableDefinition getMCContentAttributeType() {
		ColumnDefinition name = new ColumnDefinition(contentAttributeTypeName, "name", textDatatype, false, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition type = new ColumnDefinition(contentAttributeTypeName, "type", integerDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition optimized = new ColumnDefinition(contentAttributeTypeName, "optimized", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition quickName = new ColumnDefinition(contentAttributeTypeName, "quickname", textDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition multiValue = new ColumnDefinition(contentAttributeTypeName, "multivalue", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition filesystem = new ColumnDefinition(contentAttributeTypeName, "filesystem", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition searchable = new ColumnDefinition(contentAttributeTypeName, "searchable", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition linkedObjectType = new ColumnDefinition(contentAttributeTypeName, "linkedobjecttype", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition foreignLinkAttribute = new ColumnDefinition(contentAttributeTypeName, "foreignlinkattribute", textDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition foreignLinkAttributeRule = new ColumnDefinition(contentAttributeTypeName, "foreignlinkattributerule", clobDatatype, true, null, false,
				true, useUppercaseIdentifiers);

		ColumnDefinition[] cols = new ColumnDefinition[] {
			name, type, optimized, quickName, multiValue, filesystem, searchable, linkedObjectType,
			foreignLinkAttribute, foreignLinkAttributeRule };

		TableDefinition contentAttributeType = new TableDefinition(contentAttributeTypeName, cols,
				new IndexDefinition[] { new IndexDefinition(contentAttributeTypeName, "idx1", new ColumnDefinition[] { name }, true, true, useUppercaseIdentifiers) },
				true, useUppercaseIdentifiers);

		return contentAttributeType;
	}

	/**
	 * Get the "object_attribute" table definition
	 * @return
	 */
	public TableDefinition getMCObjectAttribute() {
		ColumnDefinition objectType = new ColumnDefinition(objectAttributeName, "object_type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition attributeName = new ColumnDefinition(objectAttributeName, "attribute_name", textDatatype, false, "", false, true,
				useUppercaseIdentifiers);

		// add constraints
		return new TableDefinition(objectAttributeName, new ColumnDefinition[] { objectType, attributeName },
				new IndexDefinition[] {
			new IndexDefinition(objectAttributeName, "object_attribute_idx1", new ColumnDefinition[] { objectType, attributeName }, true,
					true, useUppercaseIdentifiers) }, true, useUppercaseIdentifiers);
	}

	/**
	 * Get the "contentmap" table definition
	 * @return the "contentmap" table definition
	 */
	public TableDefinition getContentMap() {
		ColumnDefinition id = new ColumnDefinition(contentMapName, "id", integerDatatype, false, null, true, true, useUppercaseIdentifiers);
		ColumnDefinition contentId = new ColumnDefinition(contentMapName, "contentid", shorttextDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition motherId = new ColumnDefinition(contentMapName, "motherid", shorttextDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition objId = new ColumnDefinition(contentMapName, "obj_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition objType = new ColumnDefinition(contentMapName, "obj_type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition motherObjId = new ColumnDefinition(contentMapName, "mother_obj_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition motherObjType = new ColumnDefinition(contentMapName, "mother_obj_type", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition updateTimestamp = new ColumnDefinition(contentMapName, "updatetimestamp", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		ColumnDefinition channelId = null;
		ColumnDefinition channelsetId = null;
		ColumnDefinition[] cols = null;

		if (multichannelling) {
			channelId = new ColumnDefinition(contentMapName, "channel_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
			channelsetId = new ColumnDefinition(contentMapName, "channelset_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
			cols = new ColumnDefinition[] { id, contentId, motherId, objId, objType, motherObjId, motherObjType, updateTimestamp, channelId, channelsetId};
		} else {
			cols = new ColumnDefinition[] { id, contentId, motherId, objId, objType, motherObjId, motherObjType, updateTimestamp};
		}

		// TODO more indices if multichannelling?
		IndexDefinition idx1 = new IndexDefinition(contentMapName, "contentmap_idx1", new ColumnDefinition[] { id}, true, true, useUppercaseIdentifiers);
		IndexDefinition idx2 = null;

		if (multichannelling) {
			idx2 = new IndexDefinition(contentMapName, "contentmap_idx2", new ColumnDefinition[] { contentId, channelId }, false, true, useUppercaseIdentifiers);
		} else {
			idx2 = new IndexDefinition(contentMapName, "contentmap_idx2", new ColumnDefinition[] { contentId }, false, true, useUppercaseIdentifiers);
		}
		IndexDefinition idx3 = new IndexDefinition(contentMapName, "contentmap_idx3", new ColumnDefinition[] { objId }, false, false, useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentMapName, "contentmap_idx4", new ColumnDefinition[] { objType }, false, false, useUppercaseIdentifiers);
		IndexDefinition idx5 = new IndexDefinition(contentMapName, "contentmap_idx5", new ColumnDefinition[] { motherId, contentId }, false, false,
				useUppercaseIdentifiers);

		TableDefinition contentMap = new TableDefinition(contentMapName, cols, new IndexDefinition[] { idx1, idx2, idx3, idx4, idx5 }, true,
				useUppercaseIdentifiers);

		return contentMap;
	}

	/**
	 * Get the "contentmap" table definition for multichannelling content repositories
	 * @return the "contentmap" table definition
	 */
	public TableDefinition getMCContentMap() {
		ColumnDefinition id = new ColumnDefinition(contentMapName, "id", integerDatatype, false, null, true, true, useUppercaseIdentifiers);
		ColumnDefinition channelId = new ColumnDefinition(contentMapName, "channel_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition channelsetId = new ColumnDefinition(contentMapName, "channelset_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition objId = new ColumnDefinition(contentMapName, "obj_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition objType = new ColumnDefinition(contentMapName, "obj_type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition contentId = new ColumnDefinition(contentMapName, "contentid", shorttextDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition updateTimestamp = new ColumnDefinition(contentMapName, "updatetimestamp", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		ColumnDefinition[] cols = new ColumnDefinition[] { id, channelId, channelsetId, objId, objType, contentId, updateTimestamp};

		IndexDefinition idx1 = new IndexDefinition(contentMapName, "contentmap_idx1", new ColumnDefinition[] { id}, true, true, useUppercaseIdentifiers);
		IndexDefinition idx2 = new IndexDefinition(contentMapName, "contentmap_idx2", new ColumnDefinition[] { channelId, channelsetId}, false, true,
				useUppercaseIdentifiers);
		IndexDefinition idx3 = new IndexDefinition(contentMapName, "contentmap_idx3", new ColumnDefinition[] { objId }, false, false, useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentMapName, "contentmap_idx4", new ColumnDefinition[] { objType }, false, false, useUppercaseIdentifiers);
		IndexDefinition idx5 = new IndexDefinition(contentMapName, "contentmap_idx5", new ColumnDefinition[] { contentId }, false, false,
				useUppercaseIdentifiers);

		TableDefinition contentMap = new TableDefinition(contentMapName, cols, new IndexDefinition[] { idx1, idx2, idx3, idx4, idx5 }, true,
				useUppercaseIdentifiers);

		return contentMap;
	}

	/**
	 * Get the "contentmap_nodeversion" table definition
	 * @return the "contentmap_nodeversion" table definition
	 */
	public TableDefinition getContentMapNodeversion() {
		ColumnDefinition id = new ColumnDefinition(contentMapNodeversionName, "id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition contentId = new ColumnDefinition(contentMapNodeversionName, "contentid", shorttextDatatype, false, "", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition objId = new ColumnDefinition(contentMapNodeversionName, "obj_id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition objType = new ColumnDefinition(contentMapNodeversionName, "obj_type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition motherId = new ColumnDefinition(contentMapNodeversionName, "motherid", shorttextDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition motherObjId = new ColumnDefinition(contentMapNodeversionName, "mother_obj_id", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition motherObjType = new ColumnDefinition(contentMapNodeversionName, "mother_obj_type", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition updateTimestamp = new ColumnDefinition(contentMapNodeversionName, "updatetimestamp", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionTimestamp = new ColumnDefinition(contentMapNodeversionName, "nodeversiontimestamp", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionUser = new ColumnDefinition(contentMapNodeversionName, "nodeversion_user", textDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionLatest = new ColumnDefinition(contentMapNodeversionName, "nodeversionlatest", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionRemoved = new ColumnDefinition(contentMapNodeversionName, "nodeversionremoved", integerDatatype, true, null, false, true,
				useUppercaseIdentifiers);
		ColumnDefinition nodeversionAutoupdate = new ColumnDefinition(contentMapNodeversionName, "nodeversion_autoupdate", tinyintegerDatatype, false, "0",
				false, true, useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx1", new ColumnDefinition[] { id}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx2 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx2", new ColumnDefinition[] { contentId}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx3 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx3", new ColumnDefinition[] { objId}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx4 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx4", new ColumnDefinition[] { objType}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx5 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx5", new ColumnDefinition[] { motherId}, false, false,
				useUppercaseIdentifiers);
		IndexDefinition idx6 = new IndexDefinition(contentMapNodeversionName, "contentmap_nodeversion_idx6", new ColumnDefinition[] { motherId, contentId},
				false, false, useUppercaseIdentifiers);

		TableDefinition contentMapNodeversion = new TableDefinition(contentMapNodeversionName,
				new ColumnDefinition[] {
			id, contentId, objId, objType, motherId, motherObjId, motherObjType, updateTimestamp, nodeversionTimestamp, nodeversionUser,
			nodeversionLatest, nodeversionRemoved, nodeversionAutoupdate}, new IndexDefinition[] { idx1, idx2, idx3, idx4, idx5, idx6}, true,
				useUppercaseIdentifiers);

		return contentMapNodeversion;
	}

	/**
	 * Get the "contentobject" table definition
	 * @return the "contentobject" table definition
	 */
	public TableDefinition getContentObject() {
		ColumnDefinition name = new ColumnDefinition(contentObjectName, "name", shorttextDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition type = new ColumnDefinition(contentObjectName, "type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition idCounter = new ColumnDefinition(contentObjectName, "id_counter", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition excludeVersioning = new ColumnDefinition(contentObjectName, "exclude_versioning", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(contentObjectName, "contentobject_idx1", new ColumnDefinition[] { type}, true, true, useUppercaseIdentifiers);

		TableDefinition contentObject = new TableDefinition(contentObjectName, new ColumnDefinition[] { name, type, idCounter, excludeVersioning},
				new IndexDefinition[] { idx1}, true, useUppercaseIdentifiers);

		return contentObject;
	}

	/**
	 * Get the "contentobject" table definition for multichannelling contentrepository
	 * @return the "contentobject" table definition
	 */
	public TableDefinition getMCContentObject() {
		ColumnDefinition name = new ColumnDefinition(contentObjectName, "name", shorttextDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition type = new ColumnDefinition(contentObjectName, "type", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(contentObjectName, "contentobject_idx1", new ColumnDefinition[] { type}, true, true, useUppercaseIdentifiers);

		TableDefinition contentObject = new TableDefinition(contentObjectName, new ColumnDefinition[] { name, type}, new IndexDefinition[] { idx1}, true,
				useUppercaseIdentifiers);

		return contentObject;
	}

	/**
	 * Get the "contentstatus" table definition
	 * @return the "contentstatus" table definition
	 */
	public TableDefinition getContentStatus() {
		ColumnDefinition name = new ColumnDefinition(contentStatusName, "name", textDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition intValue = new ColumnDefinition(contentStatusName, "intvalue", integerDatatype, true, null, false, true, useUppercaseIdentifiers);
		ColumnDefinition stringValue = new ColumnDefinition(contentStatusName, "stringvalue", clobDatatype, true, null, false, true, useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(contentStatusName, "contentstatus_idx1", new ColumnDefinition[] { name}, true, true, useUppercaseIdentifiers);

		TableDefinition contentStatus = new TableDefinition(contentStatusName, new ColumnDefinition[] { name, intValue, stringValue},
				new IndexDefinition[] { idx1}, true, useUppercaseIdentifiers);

		return contentStatus;
	}

	/**
	 * Get the table definition for the table "channel"
	 * @return table definition for "channel"
	 */
	public TableDefinition getChannelTable() {
		ColumnDefinition id = new ColumnDefinition(channelName, "id", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition name = new ColumnDefinition(channelName, "name", textDatatype, false, "", false, true, useUppercaseIdentifiers);
		ColumnDefinition mpttLeft = new ColumnDefinition(channelName, "mptt_left", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition mpttRight = new ColumnDefinition(channelName, "mptt_right", integerDatatype, false, "0", false, true, useUppercaseIdentifiers);
		ColumnDefinition updateTimestamp = new ColumnDefinition(channelName, "updatetimestamp", integerDatatype, false, "0", false, true,
				useUppercaseIdentifiers);

		IndexDefinition idx1 = new IndexDefinition(channelName, "channel_idx1", new ColumnDefinition[] { id }, true, true, useUppercaseIdentifiers);

		return new TableDefinition(channelName, new ColumnDefinition[] { id, name, mpttLeft, mpttRight, updateTimestamp }, new IndexDefinition[] { idx1 }, true,
				useUppercaseIdentifiers);
	}

	/**
	 * Internal class representing a database table
	 * @author alexander
	 */
	public static class TableDefinition {

		/**
		 * The name of the table
		 */
		protected String tableName;

		/**
		 * A map of all columns in the table
		 */
		protected Map<String, ColumnDefinition> columns;

		/**
		 * A map of all indices in the table
		 */
		protected Map<String, IndexDefinition> indices;

		/**
		 * True if table is required, false if not
		 */
		protected boolean required;

		/**
		 * True if all identifiers should be returned uppercase, false if not
		 */
		protected boolean uppercase;

		/**
		 * Create a new table definition
		 * @param tableName the name of the table
		 * @param columns an array of all columns in the table
		 * @param indices an array of all indices in the table
		 * @param required true if table is required, false if not
		 * @param uppercase true if all identifiers should be converted to
		 *        uppercase, false if not
		 */
		public TableDefinition(String tableName, ColumnDefinition[] columns,
				IndexDefinition[] indices, boolean required, boolean uppercase) {
			if (uppercase) {
				tableName = tableName.toUpperCase();
			}
			this.tableName = tableName;
			this.columns = new LinkedHashMap<String, ColumnDefinition>();
			if (columns != null) {
				for (int i = 0; i < columns.length; i++) {
					ColumnDefinition columnDefinition = columns[i];

					this.columns.put(columnDefinition.getColumnName(), columnDefinition);
				}
			}
			this.indices = new LinkedHashMap<String, IndexDefinition>();
			if (indices != null) {
				for (int i = 0; i < indices.length; i++) {
					IndexDefinition indexDefinition = indices[i];

					this.indices.put(indexDefinition.getIndexName(), indexDefinition);
				}
			}
			this.required = required;
			this.uppercase = uppercase;
		}

		/**
		 * @return all columns in the table
		 */
		public Map<String, ColumnDefinition> getColumns() {
			return columns;
		}

		/**
		 * Get a specific column
		 * @param columnName the name of the column
		 * @return the column definition, if it exists. null otherwise.
		 */
		public ColumnDefinition getColumn(String columnName) {
			if (columnName == null) {
				return null;
			}
			if (uppercase) {
				columnName = columnName.toUpperCase();
			}
			return columns.get(columnName);
		}

		/**
		 * Add a column definition to the table
		 * @param columnDefinition the column definition to add
		 */
		public void setColumn(ColumnDefinition columnDefinition) {
			columns.put(columnDefinition.getColumnName(), columnDefinition);
		}

		/**
		 * @return add indices in the table
		 */
		public Map<String, IndexDefinition> getIndices() {
			return indices;
		}

		/**
		 * Get an index by name
		 * @param indexName the name of the index
		 * @return an index with the given name. if none exists, return null
		 */
		public IndexDefinition getIndex(String indexName) {
			return indices.get(indexName);
		}

		/**
		 * Get an index by indexed columns
		 * @param columns the columns (column names) over which the index is
		 *        created
		 * @return an index with the given columns. if none exists, return null
		 */
		public IndexDefinition getIndex(Map<Integer, String> columns) {
			if (uppercase) {
				for (Entry<Integer, String> entry : columns.entrySet()) {
					entry.setValue(entry.getValue().toUpperCase());
				}
			}
			for (IndexDefinition indexDefinition : indices.values()) {
				if (indexDefinition.getColumnNames().equals(columns)) {
					return indexDefinition;
				}
			}
			return null;
		}

		/**
		 * Get all indices in which the column is inlcuded
		 * @param column the column to list all indices for
		 * @return all indices the column is inluded in
		 */
		public Collection<IndexDefinition> getAllIndices(ColumnDefinition column) {
			Collection<IndexDefinition> allIndices = new Vector<IndexDefinition>();

			for (IndexDefinition indexDefinition : indices.values()) {
				if (indexDefinition.containsColumn(column)) {
					allIndices.add(indexDefinition);
				}
			}
			return allIndices;
		}

		/**
		 * Add an index to the table
		 * @param indexDefinition the index definition
		 */
		public void addIndex(IndexDefinition indexDefinition) {
			indices.put(indexDefinition.getIndexName(), indexDefinition);
		}

		/**
		 * @return the name of the table
		 */
		public String getTableName() {
			return tableName;
		}
	}

	/**
	 * Inner class representing a column in a table
	 * @author alexander
	 */
	public static class ColumnDefinition {

		/**
		 * The name of the table
		 */
		protected String tableName;

		/**
		 * The name of the column
		 */
		protected String columnName;

		/**
		 * True if column can contain null values, false if not
		 */
		protected boolean nullable;

		/**
		 * The default value for the column
		 */
		protected String defaultValue;

		/**
		 * True if column is required, false if not
		 */
		protected boolean required;

		/**
		 * True if column is an autoincrement column, false if not
		 */
		protected boolean autoIncrement;

		/**
		 * The sql datatype of the column
		 */
		protected SQLDatatype dataType;

		/**
		 * True if all identifiers should be treated as uppercase, false if not
		 */
		protected boolean uppercase;

		/**
		 * eventually existing alternative definition of the column (in case
		 * different database or connector versions report the same columns in
		 * different ways)
		 */
		protected ColumnDefinition alternativeDefinition;

		/**
		 * Create a new column definition
		 * @param tableName the name of the table for the column
		 * @param columnName the name of the column
		 * @param dataType the sql datatype of the column
		 * @param nullable true if column can contain null values, false if not
		 * @param defaultValue the default value for the column
		 * @param autoIncrement true if column is an autoincrement column, false
		 *        if not
		 * @param required true if column is required, false if not
		 * @param uppercase true if all identifiers should be treated as
		 *        uppercase, false if not
		 */
		public ColumnDefinition(String tableName, String columnName, SQLDatatype dataType,
				boolean nullable, String defaultValue, boolean autoIncrement, boolean required,
				boolean uppercase) {
			this(tableName, columnName, dataType, nullable, defaultValue, autoIncrement, required, uppercase, null);
		}

		/**
		 * Create a new column definition
		 * @param tableName the name of the table for the column
		 * @param columnName the name of the column
		 * @param dataType the sql datatype of the column
		 * @param nullable true if column can contain null values, false if not
		 * @param defaultValue the default value for the column
		 * @param autoIncrement true if column is an autoincrement column, false
		 *        if not
		 * @param required true if column is required, false if not
		 * @param uppercase true if all identifiers should be treated as
		 *        uppercase, false if not
		 * @param alternativeDefinition alternative definition, may be null
		 */
		public ColumnDefinition(String tableName, String columnName, SQLDatatype dataType,
				boolean nullable, String defaultValue, boolean autoIncrement, boolean required,
				boolean uppercase, ColumnDefinition alternativeDefinition) {
			if (uppercase) {
				tableName = tableName.toUpperCase();
				columnName = columnName.toUpperCase();
			}
			this.tableName = tableName;
			this.columnName = columnName;
			this.dataType = dataType;
			this.nullable = nullable;
			this.defaultValue = defaultValue;
			this.autoIncrement = autoIncrement;
			this.required = required;
			this.uppercase = uppercase;
			this.alternativeDefinition = alternativeDefinition;
		}

		/**
		 * @return the name of the column
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * @return the default value for the column
		 */
		public String getDefaultValue() {
			return defaultValue;
		}

		/**
		 * @return true if column can contain null values, false if not
		 */
		public boolean isNullable() {
			return nullable;
		}

		/**
		 * @return the name of the table
		 */
		public String getTableName() {
			return tableName;
		}

		/**
		 * @return true if column is an autoincrement column, false if not
		 */
		public boolean isAutoIncrement() {
			return this.autoIncrement;
		}

		/**
		 * @return the sql datatype of the column
		 */
		public SQLDatatype getDataType() {
			return dataType;
		}

		/**
		 * Get an alternative definition (if one exists) or null
		 * @return alternative definition or null
		 */
		public ColumnDefinition getAlternativeDefinition() {
			return alternativeDefinition;
		}

		/**
		 * Compare two colum definitions. Two columns are equal if: they belong
		 * to the same table, have the same name, have the same datatype, the
		 * same default value and the same nullable value
		 * @param other the column definition to compare
		 * @return true if the definitions equal, false if not
		 */
		public boolean equals(ColumnDefinition other) {
			return equals(other, true);
		}

		/**
		 * Compare two colum definitions. Two columns are equal if: they belong
		 * to the same table, have the same name, have the same datatype, the
		 * same default value and the same nullable value
		 * @param other the column definition to compare
		 * @param logErrors whether differences in the column definition shall be logged as errors or not
		 * @return true if the definitions equal, false if not
		 */
		public boolean equals(ColumnDefinition other, boolean logErrors) {
			if (other == null) {
				return false;
			}

			// check for equality of alternative definitions first
			ColumnDefinition alternativeOther = other.getAlternativeDefinition();
			ColumnDefinition[][] comparePairs = { { this, alternativeOther}, { alternativeDefinition, other}};

			for (int i = 0; i < comparePairs.length; i++) {
				ColumnDefinition[] defPair = comparePairs[i];

				if (defPair[0] != null && defPair[1] != null) {
					if (defPair[0].equals(defPair[1], false)) {
						return true;
					}
				}
			}

			if (!getTableName().equals(other.getTableName())) {
				if (logErrors) {
					logger.error(
							"Columns {" + this.getColumnName() + "} are different. Reference table name {" + getTableName() + "}, check table name {"
							+ other.getTableName() + "}.");
				}
				return false;
			}

			if (!getColumnName().equals(other.getColumnName())) {
				if (logErrors) {
					logger.error(
							"Columns {" + this.getColumnName() + "} are different. Reference column name {" + getColumnName() + "}, check column name {"
							+ other.getColumnName() + "}.");
				}
				return false;
			}

			if (!getDataType().equals(other.getDataType())) {
				if (logErrors) {
					logger.error(
							"Columns {" + this.getColumnName() + "} are different. Reference datatype {" + getDataType().toString() + "}, check datatype {"
							+ other.getDataType().toString() + "}.");
				}
				return false;
			}

			if (isNullable() != other.isNullable()) {
				if (logErrors) {
					logger.error(
							"Columns {" + this.getColumnName() + "} are different. Reference nullable {" + isNullable() + "}, check nullable {" + other.isNullable()
							+ "}.");
				}
				return false;
			}

			if (getDefaultValue() == null) {
				if (other.getDefaultValue() != null) {
					if (logErrors) {
						logger.error(
								"Columns {" + this.getColumnName() + "} are different. Reference default value is null, check default value {"
								+ other.getDefaultValue() + "}.");
					}
					return false;
				}
			} else if (!getDefaultValue().equals(other.getDefaultValue())) {
				if (logErrors) {
					logger.error(
							"Columns {" + this.getColumnName() + "} are different. Reference default {" + getDefaultValue() + "}, check default {"
							+ other.getDefaultValue() + "}.");
				}
				return false;
			}

			return true;
		}

	}

	/**
	 * Inner class representing an index definition
	 * @author alexander
	 */
	public static class IndexDefinition {

		/**
		 * The name of the index
		 */
		protected String indexName;

		/**
		 * A sorted map of the column definitions
		 */
		protected SortedMap<Integer, ColumnDefinition> columnDefinitions;

		/**
		 * The name of the table
		 */
		protected String tableName;

		/**
		 * True if index is a primary key, false if not
		 */
		protected boolean primary;

		/**
		 * True if index is a unique index, false if not
		 */
		protected boolean unique;

		/**
		 * True if all identifiers should be treated as uppercase, false if not
		 */
		protected boolean uppercase;

		/**
		 * Create a new index definition
		 * @param tableName the name of the table
		 * @param indexName the name of the index
		 * @param columnNames the column names for the index
		 * @param primary true if the index is a primary key, false if not
		 * @param unique true if index is a unique index, false if not
		 * @param uppercase true if all identifiers should be treated as
		 *        uppercase, false if not
		 */
		public IndexDefinition(String tableName, String indexName,
				ColumnDefinition[] columnDefinitions, boolean primary, boolean unique,
				boolean uppercase) {
			if (uppercase) {
				indexName = indexName.toUpperCase();
				tableName = tableName.toUpperCase();
			}
			this.indexName = indexName;
			this.columnDefinitions = new TreeMap<Integer, ColumnDefinition>();
			for (int i = 0; i < columnDefinitions.length; i++) {
				ColumnDefinition column = columnDefinitions[i];

				this.columnDefinitions.put(new Integer(i + 1), column);
			}
			this.tableName = tableName;
			this.primary = primary;
			this.unique = unique;
			this.uppercase = uppercase;
		}

		/**
		 * @return the name of the index
		 */
		public String getIndexName() {
			return indexName;
		}

		/**
		 * @return the columns of the index
		 */
		public SortedMap<Integer, ColumnDefinition> getColumnDefinitions() {
			return columnDefinitions;
		}

		/**
		 * @return the names of the columns of the index
		 */
		public SortedMap<Integer, String> getColumnNames() {
			SortedMap<Integer, String> names = new TreeMap<Integer, String>();

			for (Entry<Integer, ColumnDefinition> entry : columnDefinitions.entrySet()) {
				names.put(entry.getKey(), entry.getValue().getColumnName());
			}
			return names;
		}

		/**
		 * Return true if the index included the specified column
		 * @param column the column to look for
		 * @return true if the index contains the column, false if not
		 */
		public boolean containsColumn(ColumnDefinition column) {
			for (Entry<Integer, ColumnDefinition> entry : columnDefinitions.entrySet()) {
				if (column.getColumnName().equalsIgnoreCase(entry.getValue().getColumnName())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @return a comma separated list of all columns in the index
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();

			buffer.append("Index {").append(indexName).append("} over columns {");
			for (Iterator<ColumnDefinition> iterator = columnDefinitions.values().iterator(); iterator.hasNext();) {
				ColumnDefinition column = iterator.next();

				buffer.append(column.getColumnName());
				if (iterator.hasNext()) {
					buffer.append(",");
				}
			}
			buffer.append("}");
			buffer.append(" Primary {").append(primary ? "true" : "false").append("}");
			buffer.append(" Unique {").append(unique ? "true" : "false").append("}");
			return buffer.toString();
		}

		/**
		 * @return the name of the table
		 */
		public String getTableName() {
			return tableName;
		}

		/**
		 * @return true if the index definies a primary key, false if not
		 */
		public boolean isPrimary() {
			return primary;
		}

		/**
		 * @return true if index is a unique key, false if not
		 */
		public boolean isUnique() {
			return unique;
		}

		/**
		 * Add a column to the index definition at index
		 * @param columnDefinition the definition of the column
		 * @param index the position of the column in the index
		 */
		public void setColumnDefinition(ColumnDefinition columnDefinition, int index) {
			this.columnDefinitions.put(new Integer(index), columnDefinition);
		}

		/**
		 * Compares two index definitions. Two index definitions equal if they
		 * contain the same columns in the same order.
		 * @param other the index definition to compare
		 * @return true if the definitions equal, false if not
		 */
		public boolean equals(IndexDefinition other) {
			if (other == null) {
				logger.error("Index {" + this.getIndexName() + "} in table {" + this.getTableName() + "} is missing. Should be: " + this.toString());
				return false;
			}
			if (!getColumnNames().equals(other.getColumnNames())) {
				logger.error(
						"Index {" + other.getIndexName() + "} in table {" + other.getTableName() + "} does not contain the right columns. Should be: "
						+ this.toString());
				return false;
			}
			if (isPrimary() && !other.isPrimary()) {
				logger.error("Index {" + other.getIndexName() + "} in table {" + other.getTableName() + "} is no primary index. Should be: " + this.toString());
				return false;
			}
			if (!isPrimary() && other.isPrimary()) {
				logger.error("Index {" + other.getIndexName() + "} in table {" + other.getTableName() + "} is primary index. Should be: " + this.toString());
				return false;
			}
			if (isUnique() && !other.isUnique()) {
				logger.error("Index {" + other.getIndexName() + "} in table {" + other.getTableName() + "} is no unique index. Should be: " + this.toString());
				return false;
			}
			if (!isUnique() && other.isUnique()) {
				logger.error("Index {" + other.getIndexName() + "} in table {" + other.getTableName() + "} is unique index. Should be: " + this.toString());
				return false;
			}
			return true;
		}
	}

	/**
	 * Inner class repesenting a constraint definition
	 */
	public static class ConstraintDefinition {

		/**
		 * Constraint name
		 */
		protected String constraintName;

		/**
		 * Table name
		 */
		protected String tableName;

		/**
		 * Column name
		 */
		protected String columnName;

		/**
		 * Foreign table name
		 */
		protected String foreignTableName;

		/**
		 * Foreign column name
		 */
		protected String foreignColumnName;

		/**
		 * on delete action
		 */
		protected ConstraintAction onDelete;

		/**
		 * on update action
		 */
		protected ConstraintAction onUpdate;

		/**
		 * Create a constraint
		 * @param tableName table name
		 * @param columnName column name
		 * @param foreignTableName foreign table name
		 * @param foreignColumnName foreign column name
		 * @param onDelete on delete action
		 * @param onUpdate on update action
		 * @param uppercase true if using uppercase identifiers
		 */
		public ConstraintDefinition(String tableName, String columnName, String foreignTableName,
				String foreignColumnName, ConstraintAction onDelete, ConstraintAction onUpdate, boolean uppercase) {
			StringBuffer buffer = new StringBuffer();

			buffer.append("gtx_").append(tableName.substring(0, Math.min(tableName.length(), 12))).append("_").append(
					columnName.substring(0, Math.min(columnName.length(), 12)));
			this.constraintName = buffer.toString();
			if (uppercase) {
				this.constraintName = this.constraintName.toUpperCase();
			}
			this.tableName = tableName;
			if (uppercase) {
				this.tableName = this.tableName.toUpperCase();
			}
			this.columnName = columnName;
			if (uppercase) {
				this.columnName = this.columnName.toUpperCase();
			}
			this.foreignTableName = foreignTableName;
			if (uppercase) {
				this.foreignTableName = this.foreignTableName.toUpperCase();
			}
			this.foreignColumnName = foreignColumnName;
			if (uppercase) {
				this.foreignColumnName = this.foreignColumnName.toUpperCase();
			}
			this.onDelete = onDelete;
			this.onUpdate = onUpdate;
		}

		/**
		 * @return the constraintName
		 */
		public String getConstraintName() {
			return constraintName;
		}

		/**
		 * @return the tableName
		 */
		public String getTableName() {
			return tableName;
		}

		/**
		 * @return the columnName
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * @return the foreignTableName
		 */
		public String getForeignTableName() {
			return foreignTableName;
		}

		/**
		 * @return the foreignColumnName
		 */
		public String getForeignColumnName() {
			return foreignColumnName;
		}

		/**
		 * @return the onDelete
		 */
		public ConstraintAction getOnDelete() {
			return onDelete;
		}

		/**
		 * @return the onUpdate
		 */
		public ConstraintAction getOnUpdate() {
			return onUpdate;
		}

		/**
		 * Check whether the constraint definitions are equal
		 * @param other other constraint definition
		 * @return true if they are equal, false if not
		 */
		public boolean equals(ConstraintDefinition other) {
			if (other == null) {
				logger.error("Constraint {" + constraintName + "} in table {" + tableName + "} is missing.");
				return false;
			}
			if (!StringUtils.isEqual(tableName, other.tableName) || !StringUtils.isEqual(columnName, other.columnName)
					|| !StringUtils.isEqual(foreignTableName, other.foreignTableName) || !StringUtils.isEqual(foreignColumnName, other.foreignColumnName)) {
				logger.error(
						"Constraint {" + constraintName + "} should have " + tableName + "." + columnName + " reference " + foreignTableName + "." + foreignColumnName
						+ " but has " + other.tableName + "." + other.columnName + " reference " + other.foreignTableName + "." + other.foreignColumnName);
				return false;
			}
			if (onDelete != other.onDelete) {
				logger.error("Constraint {" + constraintName + "} should have on delete action " + onDelete + " but has " + other.onDelete);
				return false;
			}
			if (onUpdate != other.onUpdate) {
				logger.error("Constraint {" + constraintName + "} should have on update action " + onUpdate + " but has " + other.onUpdate);
				return false;
			}
			return true;
		}
	}

	/**
	 * Possible constraint actions
	 */
	public static enum ConstraintAction {
		RESTRICT("RESTRICT", DatabaseMetaData.importedKeyRestrict), CASCADE("CASCADE", DatabaseMetaData.importedKeyCascade), SET_NULL("SET NULL", DatabaseMetaData.importedKeySetNull), NO_ACTION("NO ACTION", DatabaseMetaData.importedKeyNoAction);

		/**
		 * Action
		 */
		private String action;

		/**
		 * Value given back by jdbc driver
		 */
		private int jdbcValue;

		/**
		 * Create an instance
		 * @param action action
		 */
		private ConstraintAction(String action, int jdbcValue) {
			this.action = action;
			this.jdbcValue = jdbcValue;
		}

		@Override
		public String toString() {
			return action;
		}

		/**
		 * Get the action by the JDBC value
		 * @param jdbcValue JDBC value
		 * @return action or null
		 */
		public static ConstraintAction getAction(int jdbcValue) {
			for (ConstraintAction action : values()) {
				if (action.jdbcValue == jdbcValue) {
					return action;
				}
			}

			return null;
		}
	}

	/**
	 * Inner class representing a sql datatype
	 * @author alexander
	 */
	public final static class SQLDatatype {

		/**
		 * The sql type as returned by JDBC
		 */
		private Set<Integer> sqlType;

		/**
		 * The name of the sql type
		 */
		private String sqlTypeName;

		/**
		 * The length of the type
		 */
		private int length;

		/**
		 * The length of an index on this datatype
		 */
		private int keyLength;

		/**
		 * True if the type supports an index, false if not
		 */
		private boolean supportsIndex;

		/**
		 * True if the length should be checked, false if not
		 */
		private boolean checkLength;

		/**
		 * eventually existing alternative sql datatype (in case
		 * different database or connector versions report the same columns in
		 * different ways)
		 */
		private SQLDatatype alternativeType;

		/**
		 * Create a new sql datatype
		 * @param sqlType the sql datatype
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param checkLength true if length should be checked, false if not
		 */
		public SQLDatatype(int sqlType, String sqlTypeName, int length, boolean checkLength) {
			this(sqlType, sqlTypeName, length, length, true, checkLength, null);
		}

		/**
		 * Create a new sql datatype
		 * @param sqlType the sql datatypes
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param checkLength true if length should be checked, false if not
		 */
		public SQLDatatype(Collection<Integer> sqlTypes, String sqlTypeName, int length, boolean checkLength) {
			this(sqlTypes, sqlTypeName, length, length, true, checkLength, null);
		}

		/**
		 * Create a new sql datatype
		 * @param sqlType the sql datatype
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param supportsIndex true if the type supports indexing
		 * @param checkLength true if length should be checked, false if not
		 */
		public SQLDatatype(int sqlType, String sqlTypeName, int length, boolean supportsIndex,
				boolean checkLength) {
			this(sqlType, sqlTypeName, length, length, supportsIndex, checkLength, null);
		}

		/**
		 * Create a new sql datatype
		 * @param sqlType the sql datatype
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param keyLength the length of the index on this type
		 * @param supportsIndex true if the type supports indexing
		 * @param checkLength true if length should be checked, false if not
		 */
		public SQLDatatype(int sqlType, String sqlTypeName, int length, int keyLength,
				boolean supportsIndex, boolean checkLength) {
			this(sqlType, sqlTypeName, length, keyLength, supportsIndex, checkLength, null);
		}

		/**
		 * Create a new sql datatype
		 * @param sqlType the sql datatype
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param keyLength the length of the index on this type
		 * @param supportsIndex true if the type supports indexing
		 * @param checkLength true if length should be checked, false if not
		 * @param alternativeType alternative datatype (may be null)
		 */
		public SQLDatatype(int sqlType, String sqlTypeName, int length, int keyLength,
				boolean supportsIndex, boolean checkLength, SQLDatatype alternativeType) {
			this(Arrays.asList(sqlType), sqlTypeName, length, keyLength, supportsIndex, checkLength, alternativeType);
		}

		/**
		 * Create a new sql datatype
		 * @param sqlTypes the sql datatype(s)
		 * @param sqlTypeName the sql type name
		 * @param length the length of the type
		 * @param keyLength the length of the index on this type
		 * @param supportsIndex true if the type supports indexing
		 * @param checkLength true if length should be checked, false if not
		 * @param alternativeType alternative datatype (may be null)
		 */
		public SQLDatatype(Collection<Integer> sqlType, String sqlTypeName, int length, int keyLength,
				boolean supportsIndex, boolean checkLength, SQLDatatype alternativeType) {
			this.sqlType = new HashSet<>(sqlType);
			this.sqlTypeName = sqlTypeName;
			this.length = length;
			this.keyLength = keyLength;
			this.supportsIndex = supportsIndex;
			this.checkLength = checkLength;
			this.alternativeType = alternativeType;
		}

		/**
		 * @return the sql types
		 */
		public Set<Integer> getSqlType() {
			return sqlType;
		}

		/**
		 * @return the sql type name
		 */
		public String getSqlTypeName() {
			return sqlTypeName;
		}

		/**
		 * @return the length of the datatype
		 */
		public int getLength() {
			return length;
		}

		/**
		 * @return the key length for this datatype
		 */
		public int getKeyLength() {
			return keyLength;
		}

		/**
		 * @return if the datatype needs a specific key length
		 */
		public boolean needsKeyLength() {
			return keyLength != length;
		}

		/**
		 * Return true if the datatype supports indexing, false if not
		 * @return true if the datatype supports indexing, false if not
		 */
		public boolean supportsIndex() {
			return supportsIndex;
		}

		/**
		 * @return true if the length should be checked, false if not
		 */
		public boolean checkLength() {
			return checkLength;
		}

		/**
		 * Compare two sql datatypes
		 * @param other the sql datatype to compare
		 * @return true if datatypes equal, false if not
		 */
		public boolean equals(SQLDatatype other) {
			// first compare eventually existing alternative types
			SQLDatatype[][] comparePairs = { { this, other.alternativeType}, { alternativeType, other}};

			for (int i = 0; i < comparePairs.length; i++) {
				SQLDatatype[] defPair = comparePairs[i];

				if (defPair[0] != null && defPair[1] != null) {
					if (defPair[0].equals(defPair[1])) {
						return true;
					}
				}
			}

			if (!Collections.disjoint(getSqlType(), other.getSqlType()) && (!(checkLength() || other.checkLength()) || (getLength() == other.getLength()))) {
				return true;
			}
			return false;
		}

		/**
		 * @return the string representation of the sql datatype
		 */
		public String toString() {
			return "'" + sqlTypeName + "'(" + length + "): " + sqlType;
		}

	}

}
