package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;

/**
 * Test publishing with groovy scripts
 */
public class GroovyPublishingTest extends AbstractGroovyTest {
	@ClassRule
	public static PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected static String VALID_CODE = "import groovy.transform.Field";

	protected static String BROKEN_CODE = "import bli.bla.blubb";

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		AbstractGroovyTest.setupOnce();

		Synchronizer.addPackage(TESTPACKAGE_NAME);
		Synchronizer.addPackage(OTHERPACKAGE_NAME);
		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));

		node = update(node, upd -> {
			upd.setPublishFilesystem(true);
		}).build();

		testPage = update(testPage, p -> {
			ContentTag contentTag = p.getContentTag(GROOVY_TAGNAME);

			getPartType(HandlebarsPartType.class, contentTag, HBS_PART_NAME).setText("{{gtx_script \"%s.%s\"}}".formatted(TESTPACKAGE_NAME, "script"));
			getPartType(GroovyPartType.class, contentTag, GROOVY_PART_NAME).setText("");
			getPartType(PageURLPartType.class, contentTag, PAGE_PART_NAME).setTargetPage(targetPage);
		}).unlock().build();
	}

	@Before
	public void setup() throws IOException, NodeException {
		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), List.of("script.groovy"));
		prepareScripts(Synchronizer.getPackage(OTHERPACKAGE_NAME), List.of("script.groovy"));

		testPage = update(testPage, p -> {
		}).publish().unlock().build();
	}

	/**
	 * Test that compile errors in a package assigned to a node lets the publish process fail
	 * @throws Exception
	 */
	@Test
	public void testCompileError() throws Exception {
		// modify the script so it does not compile any more
		modifyScript(Synchronizer.getPackage(TESTPACKAGE_NAME), "script.groovy", content -> {
			return Strings.CI.replace(content, VALID_CODE, BROKEN_CODE);
		});

		// publish process must fail now
		try (Trx trx = new Trx()) {
			PublishInfo publishInfo = testContext.getContext().publish(false);

			assertThat(publishInfo).as("Publish process").failed()
					.containsMessage("Error while compiling scripts for %s".formatted(I18NHelper.getName(node)))
					.containsMessage("import bli.bla.blubb");
			trx.success();
		}

		// fix the script
		modifyScript(Synchronizer.getPackage(TESTPACKAGE_NAME), "script.groovy", content -> {
			return Strings.CI.replace(content, BROKEN_CODE, VALID_CODE);
		});

		// publish process must succeed now
		try (Trx trx = new Trx()) {
			PublishInfo publishInfo = testContext.getContext().publish(false);

			assertThat(publishInfo).as("Publish process").succeeded();
			trx.success();
		}
	}

	/**
	 * Test that compile errors in a foreign package do not let the publish process fail
	 * @throws Exception
	 */
	@Test
	public void testCompileErrorInForeignPackage() throws Exception {
		// modify the script so it does not compile any more
		modifyScript(Synchronizer.getPackage(OTHERPACKAGE_NAME), "script.groovy", content -> {
			return Strings.CI.replace(content, VALID_CODE, BROKEN_CODE);
		});

		// publish process must still succeed
		try (Trx trx = new Trx()) {
			PublishInfo publishInfo = testContext.getContext().publish(false);

			assertThat(publishInfo).as("Publish process").succeeded();
			trx.success();
		}
	}
}
