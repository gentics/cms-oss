package com.gentics.contentnode.object.cr;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.NodeObjectWithModel;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;

/**
 * Interface for CR Fragment NodeObjects
 */
@TType(CrFragment.TYPE_CR_FRAGMENT)
public interface CrFragment extends NodeObjectWithModel<ContentRepositoryFragmentModel>, SynchronizableNodeObject, Resolvable, NamedNodeObject {
	public static final int TYPE_CR_FRAGMENTS = 10300;

	public static final int TYPE_CR_FRAGMENT = 10301;

	/**
	 * Get the name
	 * @return name
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Set the name
	 * @param name name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	default void setName(String name) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the entries of the CR fragment
	 * @return list of entries
	 * @throws NodeException
	 */
	List<CrFragmentEntry> getEntries() throws NodeException;
}
