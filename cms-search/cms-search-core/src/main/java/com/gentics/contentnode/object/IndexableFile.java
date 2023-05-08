package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;

/**
 * {@link IndexableNodeObject} wrapper for {@link File}
 */
public class IndexableFile extends StackResolvableNodeObjectWrapper<File> implements IndexableNodeObject<File> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 5496846305718483279L;

	/**
	 * Function to wrap a {@link File}
	 */
	public static Function<NodeObject, IndexableNodeObject<?>> wrap = f -> f == null ? null : new IndexableFile(File.class.cast(f));

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped object
	 */
	protected IndexableFile(File wrapped) {
		super(wrapped);
	}

	@Override
	public IndexType getIndexType() {
		return wrapped.isImage() ? IndexType.image : IndexType.file;
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
