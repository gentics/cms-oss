package com.gentics.contentnode.publish.wrapper;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.rest.model.Tag;

/**
 * Wrapper for the REST Model of a list of properties.
 * Instances of this class will be used when versioned publishing is active and multithreaded publishing is used.
 * See {@link PublishablePage} for details.
 */
public class PublishableValueList extends AbstractCollection<Value> implements ValueList, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2749876991923450053L;

	/**
	 * Wrapped tag
	 */
	protected Tag tag;

	/**
	 * Container (which will be a wrapper for the tag)
	 */
	protected ValueContainer container;

	/**
	 * List of values
	 */
	protected List<Value> values;

	/**
	 * Create an instance
	 * @param tag wrapped tag
	 * @param container value container
	 */
	public PublishableValueList(Tag tag, ValueContainer container) throws NodeException {
		this.tag = tag;
		this.container = container;
		values = new ArrayList<Value>(tag.getProperties().size());
		for (Map.Entry<String, com.gentics.contentnode.rest.model.Property> entry : tag.getProperties().entrySet()) {
			PublishableValue value = new PublishableValue(container, entry.getValue());
			if (value.getPart(false) != null) {
				values.add(value);
			}
		}
	}

	@Override
	public Set<String> getResolvableKeys() {
		return SetUtils.union(tag.getProperties().keySet(), SetUtils.hashSet("unique_tag_id"));
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		if ("unique_tag_id".equals(key)) {
			return container.get(key);
		}
		return getByKeyname(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	@Override
	public Iterator<Value> iterator() {
		return values.iterator();
	}

	@Override
	public int size() {
		return values.size();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueList#get(int)
	 */
	public Value get(int i) {
		return values.get(i);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueList#getByPartOrder(int)
	 */
	public Value getByPartOrder(int partOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueList#getByPartId(java.lang.Object)
	 */
	public Value getByPartId(Object partId) {
		for (Value value : values) {
			if (ObjectTransformer.equals(value.getPartId(), partId)) {
				return value;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueList#getByKeyname(java.lang.String)
	 */
	public Value getByKeyname(String keyname) {
		com.gentics.contentnode.rest.model.Property prop = tag.getProperties().get(keyname);
		if (prop != null) {
			return new PublishableValue(container, prop);
		} else {
			return null;
		}
	}

	@Override
	public Value getById(int id) {
		com.gentics.contentnode.rest.model.Property prop = tag.getProperties().values().stream()
				.filter(p -> Objects.equals(p.getId(), id)).findFirst().orElse(null);
		if (prop != null) {
			return new PublishableValue(container, prop);
		} else {
			return null;
		}
	}

	@Override
	public boolean hasSameValues(ValueList other) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Remove values from the list, for which the part has been deleted
	 * @throws NodeException
	 */
	protected void checkDeletedParts() throws NodeException {
		for (Iterator<Value> valIt = values.iterator(); valIt.hasNext(); ) {
			Value value = valIt.next();
			if (value.getPart(false) == null) {
				valIt.remove();
			}
		}
	}
}
