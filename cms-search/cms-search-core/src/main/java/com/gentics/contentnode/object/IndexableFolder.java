package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;

/**
 * {@link IndexableNodeObject} wrapper for {@link Folder}
 */
public class IndexableFolder extends StackResolvableNodeObjectWrapper<Folder> implements IndexableNodeObject<Folder> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 7078045984781384526L;

	/**
	 * Function to wrap a {@link Folder}
	 */
	public static Function<NodeObject, IndexableNodeObject<?>> wrap = f -> f == null ? null : new IndexableFolder(Folder.class.cast(f));

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped object
	 */
	protected IndexableFolder(Folder wrapped) {
		super(wrapped);
	}

	@Override
	public IndexType getIndexType() {
		return IndexType.folder;
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
