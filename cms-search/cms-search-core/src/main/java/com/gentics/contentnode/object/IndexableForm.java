package com.gentics.contentnode.object;

import java.util.HashSet;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;

/**
 * {@link IndexableNodeObject} wrapper for {@link Form}
 */
public class IndexableForm extends StackResolvableNodeObjectWrapper<Form> implements IndexableNodeObject<Form> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 3386846686272258060L;

	/**
	 * Function to wrap a {@link Form}
	 */
	public static Function<NodeObject, IndexableNodeObject<?>> wrap = f -> f == null ? null : new IndexableForm(Form.class.cast(f));

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped object
	 */
	protected IndexableForm(Form wrapped) {
		super(wrapped);
	}

	@Override
	public IndexType getIndexType() {
		return IndexType.form;
	}

	@Override
	public Set<String> getIndexLanguages() throws NodeException {
		return new HashSet<>(wrapped.getLanguages());
	}

	@Override
	public Node getChannel() throws NodeException {
		return null;
	}

	@Override
	public boolean isMaster() throws NodeException {
		return true;
	}
}
