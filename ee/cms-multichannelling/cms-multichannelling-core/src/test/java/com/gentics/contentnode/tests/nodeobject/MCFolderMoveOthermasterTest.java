package com.gentics.contentnode.tests.nodeobject;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for moving folders in multichannelling environments
 * TODO: test also with local objects
 */
@RunWith(value = Parameterized.class)
public class MCFolderMoveOthermasterTest extends AbstractMCFolderMoveTest {
	/**
	 * Create a test instance
	 * @param source source channel of the folder to move
	 * @param target target channel of the folder to move
	 * @param localized localized object
	 * @param pos position of the localized object in the folder tree
	 * @param locchannel channel of the localized object
	 */
	public MCFolderMoveOthermasterTest(Channel source, Channel target, LocalizedObject localized, LocalizationPosition pos, Channel locchannel, Channel disinheritedChannel) {
		super(source, target, localized, pos, locchannel, disinheritedChannel);
	}

	/**
	 * Get the test variation data
	 * @return test variation data
	 */
	@Parameters(name = "{index}: source {0}, target {1}, localized {2}, pos {3}, locchannel {4}, disinherited {5}")
	public static Collection<Object[]> data() {
		return data(Channel.OTHERMASTER, null, LocalizationPosition.FOLDER);
	}
}
