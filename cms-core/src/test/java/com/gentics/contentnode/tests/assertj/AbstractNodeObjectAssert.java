package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;

/**
 * Abstract NodeObject Assert
 *
 * @param <S> "self" type
 * @param <A> type of the "actual" NodeObject
 */
public abstract class AbstractNodeObjectAssert<S extends AbstractObjectAssert<S, A>, A extends NodeObject> extends AbstractObjectAssert<S, A> {
	/**
	 * Create an instance
	 * @param actual actual instance
	 * @param selfType self type
	 */
	protected AbstractNodeObjectAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	/**
	 * Assert global ID
	 * @param globalId global ID
	 * @return fluent API
	 */
	public S has(GlobalId globalId) {
		assertThat(actual.getGlobalId()).as(descriptionText() + " globalId").isEqualTo(globalId);
		return myself;
	}

	/**
	 * Assert local ID
	 * @param localId local ID
	 * @return fluent API
	 */
	public S has(int localId) {
		assertThat(actual.getId()).as(descriptionText() + " localId").isNotNull().isEqualTo(localId);
		return myself;
	}

	/**
	 * Assert new
	 * @param flag flag
	 * @return fluent API
	 */
	public S isNew(boolean flag) {
		assertThat(AbstractContentObject.isEmptyId(actual.getId())).as(descriptionText() + " new").isEqualTo(flag);
		return myself;
	}

	/**
	 * Assert deleted
	 * @param flag flag
	 * @return fluent API
	 */
	public S isDeleted(boolean flag) {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isEqualTo(flag);
		return myself;
	}

	/**
	 * Assert that the object depends on a property of another object
	 * @param sourceObject dependency source
	 * @param sourceProperty source property
	 * @param channelId channel ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public S dependsOn(NodeObject sourceObject, String sourceProperty, int channelId) throws NodeException {
		Dependency dependency = DependencyManager.createDependency(sourceObject, null, sourceProperty, actual, null, Events.UPDATE);
		assertThat(DependencyManager.getDependenciesForObject(actual, null, null)).as(String.format("Dependencies of %s", descriptionText()))
				.contains(dependency);
		return myself;
	}

	/**
	 * Assert that the object does not depend on a property of another object
	 * @param sourceObject dependency
	 * @param sourceProperty attribute of the dependency
	 * @param channelId channel ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public S doesNotDependOn(NodeObject sourceObject, String sourceProperty, int channelId) throws NodeException {
		Dependency dependency = DependencyManager.createDependency(sourceObject, null, sourceProperty, actual, null, Events.UPDATE);
		assertThat(DependencyManager.getDependenciesForObject(actual, null, null)).as(String.format("Dependencies of %s", descriptionText()))
				.doesNotContain(dependency);
		return myself;
	}

	/**
	 * Assert that the object depends on a property of another object for the target property
	 * @param targetProperty target property
	 * @param sourceObject dependency source
	 * @param sourceProperty source property
	 * @param channelId channel ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public S dependsOn(String targetProperty, NodeObject sourceObject, String sourceProperty, int channelId) throws NodeException {
		Dependency dependency = DependencyManager.createDependency(sourceObject, null, sourceProperty, actual, null, Events.UPDATE);
		assertThat(DependencyManager.getDependenciesForObject(actual, null, targetProperty)).as(String.format("Dependencies of %s", descriptionText()))
				.contains(dependency);
		return myself;
	}

	/**
	 * Assert that the object does not depend on a property of another object for the target property
	 * @param targetProperty target property
	 * @param sourceObject dependency
	 * @param sourceProperty attribute of the dependency
	 * @param channelId channel ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public S doesNotDependOn(String targetProperty, NodeObject sourceObject, String sourceProperty, int channelId) throws NodeException {
		Dependency dependency = DependencyManager.createDependency(sourceObject, null, sourceProperty, actual, null, Events.UPDATE);
		assertThat(DependencyManager.getDependenciesForObject(actual, null, targetProperty)).as(String.format("Dependencies of %s", descriptionText()))
				.doesNotContain(dependency);
		return myself;
	}

	/**
	 * Assert that the object has the given parent
	 * @param parent parent
	 * @return fluent API
	 * @throws NodeException
	 */
	public S hasParent(Folder parent) throws NodeException {
		assertThat(actual.getParentObject()).as(descriptionText() + " parent").isEqualTo(parent);
		return myself;
	}
}
