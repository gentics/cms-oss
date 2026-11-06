package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;

/**
 * {@link IndexableNodeObject} wrapper for {@link ImageFile}
 */
public class IndexableImageFile extends StackResolvableNodeObjectWrapper<ImageFile> implements IndexableNodeObject<ImageFile> {
	/**
	 * Serial Version UUId
	 */
	private static final long serialVersionUID = -6047168677991891201L;

	/**
	 * Function to wrap a {@link ImageFile}
	 */
	public static Function<NodeObject, IndexableNodeObject<?>> wrap = f -> f == null ? null : new IndexableImageFile(ImageFile.class.cast(f));

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped object
	 */
	protected IndexableImageFile(ImageFile wrapped) {
		super(wrapped);
	}

	@Override
	public IndexType getIndexType() {
		return IndexType.image;
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
