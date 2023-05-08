package com.gentics.contentnode.tests.publish.mccr;

import java.util.Collection;
import java.util.Vector;

import org.junit.runners.Parameterized.Parameters;

/**
 * Variant for the test for reusing inherited page sources, that has the feature turned off
 */
public class RenderingInheritedPagesSandboxCTest extends AbstractRenderingInheritedPagesSandboxTest {

	/**
	 * Create a parameterized instance of the test case
	 * 
	 * @param pageCreationSettings where the master page is created
	 * @param dependencySettings where the dependency is created
	 * @param dependencyLocalizationSettings where the dependency is localized
	 */
	public RenderingInheritedPagesSandboxCTest(PageCreationSettings pageCreationSettings, DependencySettings dependencySettings,
			DependencyLocalizationSettings dependencyLocalizationSettings) {
		super(pageCreationSettings, dependencySettings, dependencyLocalizationSettings, false);
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: pageCreationSettings {0}, dependencySettings {1}, dependencyLocalizationSettings {2}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (DependencySettings dependency : DependencySettings.values()) {
			testData.add(new Object[] { PageCreationSettings.MASTER, dependency, DependencyLocalizationSettings.NONE });
		}
		return testData;
	}
}
