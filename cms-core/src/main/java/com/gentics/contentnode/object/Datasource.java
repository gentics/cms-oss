/*
 * @author norbert
 * @date 22.02.2007
 * @version $Id: Datasource.java,v 1.2.10.1 2011-03-08 12:28:11 norbert Exp $
 */
package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.model.DatasourceModel;
import com.gentics.contentnode.devtools.model.DatasourceTypeModel;
import com.gentics.contentnode.devtools.model.DatasourceValueModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.DatasourceType;
import com.gentics.contentnode.rest.util.MiscUtils;

/**
 * Class for instances of datasources
 */
@SuppressWarnings("serial")
//TODO this seems to be wrong, check the implications when this is changed
@TType(Datasource.TYPE_DATASOURCE)
public abstract class Datasource extends AbstractContentObject implements SynchronizableNodeObject, NamedNodeObject {

	/**
	 * constant for the ttype
	 */
	public final static int TYPE_DATASOURCE = 10024;

	public final static Integer TYPE_DATASOURCE_INTEGER = new Integer(TYPE_DATASOURCE);

	public final static int TYPE_SINGLE_DATASOURCE = 10027;

	public final static Integer TYPE_SINGLE_DATASOURCE_INTEGER = new Integer(TYPE_SINGLE_DATASOURCE);

	/**
	 * Maximum name length
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.Datasource, Datasource, Datasource> REST2NODE = (restModel, datasource) -> {
		if (!StringUtils.isBlank(restModel.getName())) {
			datasource.setName(restModel.getName());
		}
		DatasourceType type = restModel.getType();
		if (type != null) {
			switch (type) {
			case SITEMINDER:
				datasource.setSourceType(SourceType.siteminderDS);
				break;
			case STATIC:
				datasource.setSourceType(SourceType.staticDS);
				break;
			default:
				break;
			}
		}
		return datasource;
	};

	/**
	 * Lambda that transforms the node model of a datasource to its rest model
	 */
	public final static BiFunction<Datasource, com.gentics.contentnode.rest.model.Datasource, com.gentics.contentnode.rest.model.Datasource> NODE2REST = (
			nodeDatasource, restModel) -> {
		restModel.setId(ObjectTransformer.getInt(nodeDatasource.getId(), 0));
		restModel.setGlobalId(nodeDatasource.getGlobalId() != null ? nodeDatasource.getGlobalId().toString() : null);
		restModel.setName(nodeDatasource.getName());
		switch (nodeDatasource.getSourceType()) {
		case siteminderDS:
			restModel.setType(DatasourceType.SITEMINDER);
			break;
		case staticDS:
			restModel.setType(DatasourceType.STATIC);
			break;
		default:
			break;
		}

		return restModel;
	};

	/**
	 * Lambda that transforms the node model of a datasource to its rest model
	 */
	public final static Function<Datasource, com.gentics.contentnode.rest.model.Datasource> TRANSFORM2REST = nodeDatasource -> {
		return NODE2REST.apply(nodeDatasource, new com.gentics.contentnode.rest.model.Datasource());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Datasource, DatasourceModel, DatasourceModel> NODE2DEVTOOL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setName(from.getName());
		to.setType(DatasourceTypeModel.fromValue(from.getSourceTypeVal()));

		unwrap(() -> {
			to.setValues(from.getEntries().stream().map(entry -> {
				return wrap(() -> DatasourceEntry.NODE2DEVTOOL.apply(entry, new DatasourceValueModel()));
			}).collect(Collectors.toList()));
		});

		return to;
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<Datasource>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<Datasource>>();
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName(), "name"));
		resolvableProperties.put("sourceType", new NodeObjectProperty<>((o, key) -> DatasourceTypeModel.fromValue(o.getSourceTypeVal()), "sourceType"));
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<Datasource> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * @param id object id
	 * @param info object info
	 */
	public Datasource(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the datasource entries
	 * @return datasource entries
	 * @throws NodeException
	 */
	public abstract List<DatasourceEntry> getEntries() throws NodeException;

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the datasource's udate
		int udate = getUdate();
		// check the entries udate
		List<? extends DatasourceEntry> entries = getEntries();

		for (DatasourceEntry entry : entries) {
			udate = Math.max(udate, entry.getEffectiveUdate());
		}
		return udate;
	}

	/**
	 * Get the source type
	 * @return source type
	 */
	public abstract SourceType getSourceType();

	/**
	 * Set a new source type
	 * @param sourceType source type
	 * @throws ReadOnlyException
	 */
	public void setSourceType(SourceType sourceType) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the value of the source type
	 * @return value of the source type
	 */
	@FieldGetter("source_type")
	public int getSourceTypeVal() {
		SourceType sourceType = getSourceType();

		if (sourceType != null) {
			return sourceType.getVal();
		} else {
			return 0;
		}
	}

	/**
	 * Set the source type by value
	 * @param sourceTypeVal source type value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("source_type")
	public void setSourceTypeVal(int sourceTypeVal) throws ReadOnlyException {
		setSourceType(SourceType.getForVal(sourceTypeVal));
	}

	/**
	 * Get the param id
	 * @return param id
	 */
	@FieldGetter("param_id")
	public abstract Integer getParamId();

	/**
	 * Set the paramId
	 * @param paramId paramId
	 * @throws ReadOnlyException
	 */
	@FieldSetter("param_id")
	public void setParamId(Integer paramId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the datasource name
	 * @return datasource name
	 */
	@FieldGetter("name")
	public abstract String getName();

	/**
	 * Set a new name
	 * @param name name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	public void setName(String name) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether the datasource is an internal one.
	 * Internal datasources are just used by a single tag with part of type "Datasource"
	 * @return true for internal datasources, false for a normal one
	 */
	public boolean isInternal() {
		return ObjectTransformer.isEmpty(getName());
	}

	/**
	 * Enumeration for types of datasources
	 */
	public static enum SourceType {

		/**
		 * Static Datasource
		 */
		staticDS(1), /**
		 * SiteMinder Datasource
		 */ siteminderDS(2);

		/**
		 * The value
		 */
		protected int val;

		/**
		 * Create an instance with given value
		 * @param val value
		 */
		private SourceType(int val) {
			this.val = val;
		}

		/**
		 * Get the value
		 * @return value
		 */
		public int getVal() {
			return val;
		}

		/**
		 * Get the SourceType for the value
		 * @param val value
		 * @return SourceType or null
		 */
		public static SourceType getForVal(int val) {
			for (SourceType sourceType : values()) {
				if (sourceType.getVal() == val) {
					return sourceType;
				}
			}
			return null;
		}
	}

	/**
	 * Get the parts referencing this datasource
	 * @return list of parts
	 * @throws NodeException
	 */
	public abstract List<Part> getParts() throws NodeException;

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		List<DatasourceEntry> entries = getEntries();

		for (DatasourceEntry entry : entries) {
			entry.delete();
		}
		performDelete();
	}

	/**
	 * Performs the delete of the entry
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	/**
	 * Check whether the datasource has the same values as the other datasource
	 * @param other datasource
	 * @return true iff the datasources have the same content
	 * @throws NodeException
	 */
	public boolean hasSameContent(Datasource other) throws NodeException {
		return MiscUtils.equals(getEntries(), other.getEntries(), (e1, e2) -> e1.hasSameContent(e2));
	}
}
