package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;

/**
 * {@link IndexableNodeObject} wrapper for {@link Page}
 */
public class IndexablePage extends StackResolvableNodeObjectWrapper<Page> implements IndexableNodeObject<Page> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 7493789435045223404L;

	/**
	 * Function to wrap a {@link Page}
	 */
	public static Function<NodeObject, IndexableNodeObject<?>> wrap = f -> f == null ? null : new IndexablePage(Page.class.cast(f));

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped object
	 */
	protected IndexablePage(Page wrapped) {
		super(wrapped);
	}

	@Override
	public IndexType getIndexType() {
		return IndexType.page;
	}

	@Override
	public String getIndexLanguage() throws NodeException {
		ContentLanguage language = wrapped.getLanguage();
		if (language != null) {
			return language.getCode();
		} else {
			return null;
		}
	}

	@Override
	public Node getChannel() throws NodeException {
		return wrapped.getChannel();
	}

	@Override
	public boolean isMaster() throws NodeException {
		return wrapped.isMaster();
	}
}
