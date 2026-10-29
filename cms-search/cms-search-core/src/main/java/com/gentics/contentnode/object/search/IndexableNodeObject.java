package com.gentics.contentnode.object.search;

import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.StackResolvableNodeObject;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * Interface for {@link NodeObject} implementations that are indexable
 */
public interface IndexableNodeObject<T extends StackResolvableNodeObject> extends NodeObject, StackResolvable {
	/**
	 * Get the name of the document type
	 * @return document type
	 */
	IndexType getIndexType();

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	T getWrapped();

	/**
	 * Get the language code, if the {@link IndexType} supports languages and the object has a language, otherwise return null
	 * @return language (may be null)
	 * @throws NodeException
	 */
	default String getIndexLanguage() throws NodeException {
		return null;
	}

	/**
	 * Get the language codes, if the {@link IndexType} supports languages and the object contains all language variants, otherwise return null
	 * @return language (may be null)
	 * @throws NodeException
	 */
	default Set<String> getIndexLanguages() throws NodeException {
		return null;
	}

	/**
	 * Get the channel of this object, if one set and multichannelling is supported.
	 * @return current channel of this object
	 * @throws NodeException
	 */
	Node getChannel() throws NodeException;

	/**
	 * Check whether the page is a master or a localized copy
	 * @return true for master pages, false for localized copies
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;
}
