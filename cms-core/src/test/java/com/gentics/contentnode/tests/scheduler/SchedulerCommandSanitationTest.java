package com.gentics.contentnode.tests.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tests for command sanitation when {@link com.gentics.contentnode.etc.Feature#INSECURE_SCHEDULER_COMMAND} is
 * <em>not</em> active.
 */
@RunWith(Parameterized.class)
public class SchedulerCommandSanitationTest {

	@Parameters(name = "{index}: original {0}, sanitized {1}")
	public static Collection<Object[]> data() {
		String commandPrefix = ConfigurationValue.SCHEDULER_COMMANDS_PATH.get();
		Collection<Object[]> data = new ArrayList<>();

		data.add(new Object[] { "../../bin/rm -Rf /", commandPrefix + "1....binrm-Rf"});
		data.add(new Object[] { "/bin/rm -Rf /", commandPrefix + "binrm-Rf"});
		data.add(new Object[] { "//not-sure-if-double-slashes-are-a-problem-but-just-to-be-sure.sh", commandPrefix + "not-sure-if-double-slashes-are-a-problem-but-just-to-be-sure.sh"});
		data.add(new Object[] { "evil-script.sh", commandPrefix + "evil-script.sh"});
		data.add(new Object[] { "legal-command.sh; ../../bin/rm -Rf /", commandPrefix + "legal-command.sh....binrm-Rf"});

		return data;
	}

	@Parameter
	public String originalCommand;

	@Parameter(1)
	public String sanitizedCommand;

	private SchedulerTask task;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		// Close the default database transaction.
		testContext.getContext().getTransaction().commit();
	}

	@Before
	public void setup() throws NodeException {
		task = Builder.create(
			SchedulerTask.class,
			t -> {
				t.setName(String.format("Test task: %s -> %s", originalCommand, sanitizedCommand));
				t.setCommand(originalCommand);
			})
		.build();
	}

	@Test
	public void testCommandSanitation() throws NodeException {
		assertThat(task.getSanitizedCommand())
			.as("Sanitized command")
			.isEqualTo(sanitizedCommand);
	}
}
