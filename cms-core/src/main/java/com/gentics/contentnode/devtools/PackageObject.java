package com.gentics.contentnode.devtools;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.lib.etc.StringUtils;

/**
 * Wrapper for objects in a package. Adds the package name to the object
 *
 * @param <T> type of the wrapped object
 */
public class PackageObject <T extends SynchronizableNodeObject> implements NodeObject, Resolvable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3123424303423670992L;

	/**
	 * Wrapped object
	 */
	protected T object;

	/**
	 * Package name
	 */
	protected String packageName;

	/**
	 * Create an instance for the object (with null as package name)
	 * @param object wrapped object
	 */
	public PackageObject(T object) {
		this(object, null);
	}

	/**
	 * Create an instance for the object and package name
	 * @param object wrapped object
	 * @param packageName package name
	 */
	public PackageObject(T object, String packageName) {
		this.object = object;
		this.packageName = packageName;
	}

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	public T getObject() {
		return object;
	}

	/**
	 * Get the package name (may be empty for objects in the main package)
	 * @return package name
	 */
	public String getPackageName() {
		return packageName;
	}

	@Override
	public Integer getId() {
		return object.getId();
	}

	@Override
	public NodeFactory getFactory() {
		return object.getFactory();
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		return object.getObjectInfo();
	}

	@Override
	public Integer getTType() {
		return object.getTType();
	}

	@Override
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
	}

	@Override
	public void delete() throws InsufficientPrivilegesException, NodeException {
	}

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
	}

	@Override
	public boolean isDeleted() {
		return object.isDeleted();
	}

	@Override
	public int getDeleted() {
		return object.getDeleted();
	}

	@Override
	public boolean isRecyclable() {
		return object.isRecyclable();
	}

	@Override
	public SystemUser getDeletedBy() throws NodeException {
		return object.getDeletedBy();
	}

	@Override
	public void restore() throws NodeException {
	}

	@Override
	public boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException {
		return false;
	}

	@Override
	public boolean save() throws InsufficientPrivilegesException, NodeException {
		return false;
	}

	@Override
	public void unlock() throws NodeException {
	}

	@Override
	public void dirtCache() throws NodeException {
	}

	@Override
	public NodeObject getParentObject() throws NodeException {
		return object.getParentObject();
	}

	@Override
	public NodeObject copy() throws NodeException {
		return null;
	}

	@Override
	public <U extends NodeObject> void copyFrom(U original) throws ReadOnlyException, NodeException {
	}

	@Override
	public NodeObject getPublishedObject() throws NodeException {
		return object.getPublishedObject();
	}

	@Override
	public int getUdate() {
		return object.getUdate();
	}

	@Override
	public int getEffectiveUdate() throws NodeException {
		return object.getEffectiveUdate();
	}

	@Override
	public GlobalId getGlobalId() {
		return object.getGlobalId();
	}

	@Override
	public void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException {
	}

	@Override
	public boolean canResolve() {
		return object.canResolve();
	}

	@Override
	public Object get(String key) {
		if ("packageName".equals(key)) {
			return packageName;
		} else {
			return object.get(key);
		}
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PackageObject<?>) {
			PackageObject<?> other = (PackageObject<?>)obj;
			return object.equals(other.object) && StringUtils.isEqual(packageName, other.packageName);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		if (!ObjectTransformer.isEmpty(packageName)) {
			return String.format("%s in %s", object, packageName);
		} else {
			return object.toString();
		}
	}
}
