/*
 * @author clemens
 * @date 02.02.2007
 * @version $Id: DatasourceEntry.java,v 1.6 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.model.DatasourceValueModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.SelectOption;

/**
 * represents a single object from a static datasource, which is loaded from the
 * datasource_value table.<br/>
 * Instances of these class are used in two ways:
 * <ol>
 * <li>As entries that are selectable (those entries are put in the cache)</li>
 * <li>As selected entries (those are copies of the latter ones)</li>
 * </ol>
 * @author clemens
 */
@TType(DatasourceEntry.TYPE_DATASOURCEENTRY)
public abstract class DatasourceEntry extends AbstractContentObject implements StackResolvable, NamedNodeObject {

	private static final long serialVersionUID = 3486757154441107721L;

	public static final int TYPE_DATASOURCEENTRY = 10028;

	/**
	 * Function that transforms the rest model into the node object
	 */
	public final static BiFunction<DatasourceEntryModel, DatasourceEntry, DatasourceEntry> REST2NODE = (entry, nodeEntry) -> {
		if (entry.getDsId() != null) {
			nodeEntry.setDsid(entry.getDsId());
		}
		if (entry.getKey() != null) {
			nodeEntry.setKey(entry.getKey());
		}
		if (entry.getValue() != null) {
			nodeEntry.setValue(entry.getValue());
		}
		return nodeEntry;
	};

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<DatasourceEntry, DatasourceEntryModel, DatasourceEntryModel> NODE2REST = (
			entry, model) -> {
		model.setGlobalId(entry.getGlobalId().toString());
		model.setId(entry.getId());
		model.setDsId(entry.getDsid());
		model.setKey(entry.getKey());
		model.setValue(entry.getValue());
		return model;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<DatasourceEntry, DatasourceEntryModel> TRANSFORM2REST = entry -> {
		return NODE2REST.apply(entry, new DatasourceEntryModel());
	};

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<DatasourceEntry, SelectOption, SelectOption> NODE2REST_SELECTOPTION = (
			entry, option) -> {
		option.setId(entry.getDsid());
		option.setKey(entry.getKey());
		option.setValue(entry.getValue());
		return option;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<DatasourceEntry, SelectOption> TRANSFORM2REST_SELECTOPTION = entry -> {
		return NODE2REST_SELECTOPTION.apply(entry, new SelectOption());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<DatasourceEntry, DatasourceValueModel, DatasourceValueModel> NODE2DEVTOOL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setKey(from.getKey());
		to.setValue(from.getValue());
		to.setDsId(from.getDsid());

		return to;
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	static {
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("value", new Property(new String[] { "value"}) {
			public Object get(DatasourceEntry entry, String key) {
				return entry.getValue();
			}
		});
		Property keyProperty = new Property(new String[] { "dskey"}) {
			public Object get(DatasourceEntry entry, String key) {
				return entry.getKey();
			}
		};

		resolvableProperties.put("name", keyProperty);
		resolvableProperties.put("key", keyProperty);
		resolvableProperties.put("nr", new Property(new String[] { "sorder"}) {
			public Object get(DatasourceEntry entry, String key) {
				return entry.getNr();
			}
		});
		resolvableProperties.put("dsid", new Property(new String[] { "dsid"}) {
			public Object get(DatasourceEntry entry, String key) {
				return entry.getDsid();
			}
		});

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected DatasourceEntry(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * Get the number of the entry in the list of selected entries (starting
	 * with 1), or -1 if the entry is not selected
	 * @return nr of the entry or -1
	 */
	public abstract int getNr();

	/**
	 * get the id of the datasource used
	 * @return id of corresponding datasource
	 */
	public abstract int getDatasourceId();

	/**
	 * Set the datasource id
	 * @param datasourceId datasource id
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setDatasourceId(int datasourceId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the datasource
	 * @return datasource
	 * @throws NodeException
	 */
	public abstract Datasource getDatasource() throws NodeException;

	/**
	 * get sortorder of current item
	 * @return sortorder
	 */
	@FieldGetter("sorder")
	public abstract int getSortOrder();

	/**
	 * Set the sortorder
	 * @param sortOrder sortorder
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sorder")
	public void setSortOrder(int sortOrder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the entry's key
	 * @return key of this entry
	 */
	@FieldGetter("dskey")
	public abstract String getKey();

	/**
	 * Set the key
	 * @param key key
	 * @throws ReadOnlyException
	 */
	@FieldSetter("dskey")
	public void setKey(String key) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the value of this entry
	 * @return value of this entry
	 */
	@FieldGetter("value")
	public abstract String getValue();

	/**
	 * Set the value
	 * @param value value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("value")
	public void setValue(String value) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * retrieve the dsid of this entry which represents the entry's internal id
	 * for this datasource
	 * @return dsid value of this entry
	 */
	@FieldGetter("dsid")
	public abstract int getDsid();

	/**
	 * Set the dsid
	 * @param dsId dsid
	 * @throws ReadOnlyException
	 */
	@FieldSetter("dsid")
	public void setDsid(int dsId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get properies by keyname
	 */
	public Object get(String key) {
		Property prop = resolvableProperties.get(key);

		if (prop != null) {
			return prop.get(this, key);
		} else {
			return super.get(key);
		}
	}
    
	/**
	 * 
	 */
	public String toString() {
		return "DatasourceEntry {" + getId() + "}";
	}

	/**
	 * Get a copy for selection
	 * @param selectionNr selection nr of the copy
	 * @return copy of the entry
	 */
	public abstract DatasourceEntry getSelectionCopy(int selectionNr);

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		performDelete();
	}

	@Override
	public String getName() {
		return getKey();
	}

	/**
	 * Performs the delete of the entry
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	/**
	 * Check whether the datasource entry has the same values as the other datasource entry
	 * @param other datasource entry
	 * @return true iff the datasource entries have the same content
	 * @throws NodeException
	 */
	public boolean hasSameContent(DatasourceEntry other) throws NodeException {
		return Objects.equals(getDsid(), other.getDsid()) && Objects.equals(getKey(), other.getKey())
				&& Objects.equals(getSortOrder(), other.getSortOrder()) && Objects.equals(getValue(), other.getValue());
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 */
		public abstract Object get(DatasourceEntry object, String key);
	}
}
