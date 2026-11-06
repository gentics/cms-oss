package com.gentics.contentnode.tests.nodeobject;

import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.testutils.ChannelStructure.Channel;

/**
 * Test cases for moving pages
 */
@RunWith(value = Parameterized.class)
public class MCPageMoveTargetChannelTest extends AbstractMCPageMoveTest {
	public MCPageMoveTargetChannelTest(Channel source, Channel target, boolean differentFolder, Channel localized, boolean targetExcluded,
			List<Channel> targetDisinherited) {
		super(source, target, differentFolder, localized, targetExcluded, targetDisinherited);
	}

	@Parameters(name = "{index}: source {0}, target {1}, different folder {2}, localized {3}, target excluded {4}, target disinherited {5}")
	public static Collection<Object[]> data() {
		return data(Channel.CHANNEL);
	}
}
