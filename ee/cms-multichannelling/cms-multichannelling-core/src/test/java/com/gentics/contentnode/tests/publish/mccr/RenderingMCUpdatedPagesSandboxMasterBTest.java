package com.gentics.contentnode.tests.publish.mccr;

import java.util.Collection;
import java.util.Vector;

import org.junit.runners.Parameterized.Parameters;

public class RenderingMCUpdatedPagesSandboxMasterBTest extends AbstractRenderingMCUpdatedPagesSandboxTest {

	public static final boolean UPDATEINMASTER = false;

	public RenderingMCUpdatedPagesSandboxMasterBTest(PageCreationSettings pageCreationSettings, DependencySettings dependencySettings,
			DependencyLocalizationSettings dependencyLocalizationSettings, boolean updateInMaster) {
		super(pageCreationSettings, dependencySettings, dependencyLocalizationSettings, updateInMaster);
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: pageCreationSettings {0}, dependencySettings {1}, dependencyLocalizationSettings {2}, updateInMaster {3}")
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
			for (DependencyLocalizationSettings localizationSetting : DependencyLocalizationSettings.values()) {
				testData.add(new Object[] { PageCreationSettings.MASTER, dependency, localizationSetting, UPDATEINMASTER });
			}
		}

		return testData;
	}

}
