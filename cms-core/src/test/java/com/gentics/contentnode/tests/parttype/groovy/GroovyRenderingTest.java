package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;

/**
 * Test cases for rendering groovy scripts
 */
@RunWith(value = Parameterized.class)
public class GroovyRenderingTest extends AbstractGroovyTest {
	protected final static List<String> TESTPACKAGE_SCRIPTS = List.of("class1.groovy", "class2.groovy", "script.groovy");

	protected final static List<String> OTHERPACKAGE_SCRIPTS = List.of("class1.groovy", "class2.groovy", "script.groovy");

	@ClassRule
	public static PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		AbstractGroovyTest.setupOnce();

		Synchronizer.addPackage(TESTPACKAGE_NAME);
		Synchronizer.addPackage(OTHERPACKAGE_NAME);

		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));
		operate(() -> Synchronizer.addPackage(node, OTHERPACKAGE_NAME));

		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), TESTPACKAGE_SCRIPTS);
		prepareScripts(Synchronizer.getPackage(OTHERPACKAGE_NAME), OTHERPACKAGE_SCRIPTS);
	}

	@Parameters(name = "{index}: script {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String script : List.of("simple", "foldername", "import_class1", "import_class2", "direct_script",
				"direct_script_with_param", "load_page_script", "load_page_class")) {
			data.add(new Object[] { script });
		}
		return data;
	}

	@Parameter(0)
	public String testCase;

	protected String expectedResult;

	protected List<Pair<String, String>> expectedDependencies = new ArrayList<>();

	@Before
	public void setup() throws NodeException, IOException {
		String scriptContent = readFile("%s.groovy".formatted(testCase), "");
		String hbsTemplate = readFile("%s.hbs".formatted(testCase), "{{cms.tag.parts.script.execute}}");
		expectedResult = readFile("%s.result".formatted(testCase), "");

		String dependencies = readFile("%s.dependencies".formatted(testCase), "");
		if (StringUtils.isNotBlank(dependencies)) {
			for (String line : IOUtils.readLines(dependencies)) {
				String[] parts = StringUtils.split(line, ':');
				if (parts.length == 2) {
					expectedDependencies.add(Pair.of(parts[0], parts[1]));
				}
			}
		}

		testPage = update(testPage, p -> {
			ContentTag contentTag = p.getContentTag(GROOVY_TAGNAME);

			getPartType(HandlebarsPartType.class, contentTag, HBS_PART_NAME).setText(hbsTemplate);
			getPartType(GroovyPartType.class, contentTag, GROOVY_PART_NAME).setText(scriptContent);
			getPartType(PageURLPartType.class, contentTag, PAGE_PART_NAME).setTargetPage(targetPage);
		}).publish().unlock().build();
	}

	@Test
	public void testRender() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			assertThat(testPage.render()).as("Rendered page").isEqualTo(expectedResult);
			trx.success();
		}

		assertDependencies(expectedDependencies);
	}
}
