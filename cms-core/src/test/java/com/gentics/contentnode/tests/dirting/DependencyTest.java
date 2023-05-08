package com.gentics.contentnode.tests.dirting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyManager.DependencyImpl;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;

/**
 * Test cases for dependency handling
 */
public class DependencyTest {
	protected static Set<Integer> channels(Integer...channelIds) {
		return new TreeSet<>(Arrays.asList(channelIds));
	}

	@Test
	public void testParseNullDependentProperty() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, null, "0,1,2");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(0, 1, 2);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").isEmpty();
	}

	@Test
	public void testParseEmptyDependentProperty() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, "", "0,1,2");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(0, 1, 2);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").isEmpty();
	}

	@Test
	public void testParseOldSingleDependentProperty() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, "target", "0,1,2");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(0, 1, 2);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").containsOnly(
				entry("target", channels(0, 1, 2)));
	}

	@Test
	public void testParseOldMultipleDependentProperties() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, "target1,target2,target3", "3,4");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(3, 4);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").containsOnly(
				entry("target1", channels(3, 4)),
				entry("target2", channels(3, 4)),
				entry("target3", channels(3, 4)));
	}

	@Test
	public void testParseOldJsonDependentProperty() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, "{\"0\":[\"target1\",\"target2\"],\"5\":[\"target2\",\"target3\"]}", "0,5,6");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(0, 5, 6);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").containsOnly(
				entry("target1", channels(0)),
				entry("target2", channels(0, 5)),
				entry("target3", channels(5)));
	}

	@Test
	public void testParseNewJsonDependentProperty() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6, 4711, "{\"target1\":[0],\"target2\":[0,5],\"target3\":[5]}", "0,5,6");
		assertThat(dep.getChannelIds()).as("Dependency Channel IDs").containsOnly(0, 5, 6);
		assertThat(dep.getStoredDependentProperties()).as("Stored dependent Properties").containsOnly(
				entry("target1", channels(0)),
				entry("target2", channels(0, 5)),
				entry("target3", channels(5)));
	}

	@Test
	public void testDepPropForStoring() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6);
		dep.addDependentProperty(0, "target5");
		dep.addDependentProperty(3, "target1");
		dep.addDependentProperty(2, "target3");
		dep.addDependentProperty(0, "target2");
		dep.addDependentProperty(3, "target1");
		dep.addDependentProperty(2, "target1");
		assertThat(dep.getDepPropForStoring()).as("Dependent properties for storing").isEqualTo("{\"target1\":[2,3],\"target2\":[0],\"target3\":[2],\"target5\":[0]}");
	}

	@Test
	public void testDepPropForStoringSimpleFormat() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6);
		for (int channelId : Arrays.asList(0, 1, 2, 3)) {
			for (String prop : Arrays.asList("target1", "target2", "target3", "target4", "target5")) {
				dep.addDependentProperty(channelId, prop);
			}
		}
		assertThat(dep.getDepPropForStoring()).as("Dependent properties for storing").isEqualTo("target1,target2,target3,target4,target5");
	}

	@Test
	public void testChannelsForStoring() throws NodeException {
		DependencyImpl dep = new DependencyManager.DependencyImpl(new DependencyObject(Page.class), "source",
				new DependencyObject(Folder.class), 6);
		dep.addChannelId(4);
		dep.addChannelId(2);
		dep.addChannelId(5);
		dep.addChannelId(0);
		dep.addChannelId(4);
		assertThat(dep.getChannelsForStoring()).as("Channels for storing").isEqualTo("0,2,4,5");
	}
}
