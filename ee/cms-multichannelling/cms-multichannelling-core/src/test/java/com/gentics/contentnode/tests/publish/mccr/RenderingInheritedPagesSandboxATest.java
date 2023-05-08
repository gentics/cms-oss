package com.gentics.contentnode.tests.publish.mccr;

import java.util.Collection;
import java.util.Vector;

import org.junit.runners.Parameterized.Parameters;

public class RenderingInheritedPagesSandboxATest extends AbstractRenderingInheritedPagesSandboxTest {

	/**
	 * Create a parameterized instance of the test case
	 * 
	 * @param pageCreationSettings where the master page is created
	 * @param dependencySettings where the dependency is created
	 * @param dependencyLocalizationSettings where the dependency is localized
	 */
	public RenderingInheritedPagesSandboxATest(PageCreationSettings pageCreationSettings, DependencySettings dependencySettings,
			DependencyLocalizationSettings dependencyLocalizationSettings) {
		super(pageCreationSettings, dependencySettings, dependencyLocalizationSettings, true);
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

		for (PageCreationSettings creationSetting : PageCreationSettings.values()) {
			for (DependencySettings dependency : DependencySettings.values()) {
				for (DependencyLocalizationSettings localizationSetting : DependencyLocalizationSettings.values()) {
					if (localizationSetting == DependencyLocalizationSettings.CHANNEL) {
						continue;
					}
					if (localizationSetting == DependencyLocalizationSettings.SUBCHANNEL) {
						continue;
					}
					testData.add(new Object[] { creationSetting, dependency, localizationSetting });
				}
			}
		}
		// testData.add(new Object[] {PageCreationSettings.MASTER, DependencySettings.OVERVIEW_PAGE, DependencyLocalizationSettings.CHANNEL});
		return testData;
	}

}
