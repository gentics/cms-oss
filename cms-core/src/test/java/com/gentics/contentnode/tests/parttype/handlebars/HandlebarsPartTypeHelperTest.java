package com.gentics.contentnode.tests.parttype.handlebars;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering with the usage of a js helper
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class HandlebarsPartTypeHelperTest extends AbstractHandlebarsPartTypeRenderingTest {
	public final static String TESTPACKAGE_NAME = "testpackage";

	public final static String DEFAULT_TEMPLATE = "{{testpackage.resolve cms}}";

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected static String loadHelper(String name) {
		try {
			return FileUtils.readFileToString(new File(HandlebarsPartTypeHelperTest.class.getResource(name).toURI()), "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Parameters(name = "{index}: template {0}")
	public static Collection<Object[]> data() {
		// we need to insert "null" at the second position to the generic test cases, because this test
		// has an additional parameter "template" (generic test cases will all use the DEFAULT_TEMPLATE)
		List<Object[]> genericTestCases = getGenericTestCases().stream()
				.map(testCase -> new Object[] { testCase[0], null, testCase[1], testCase[2] })
				.collect(Collectors.toList());

		return ListUtils.union(genericTestCases, Arrays.asList(
			new Object[] { loadHelper("folder_children.js"), null, "English Test Page,Subfolder,Test Page,blume.jpg,testfile.txt", Arrays.asList(Pair.of("testPage", "name"), Pair.of("englishPage", "name"), Pair.of("subFolder", "name"), Pair.of("testFile", "name"), Pair.of("testImage", "name"))},

			// tag part (direct)
			new Object[] { "cms.page.tags.urls_construct1.parts.page.internal", null, "true", null },
			new Object[] { "cms.page.tags.urls_construct1.parts.page.url", null, "/node/pub/dir/test/Target-Page.de.html", null },
			new Object[] { "cms.page.tags.urls_construct1.parts.page.target.name", null, "Target Page", null },
			new Object[] { "cms.page.tags.urls_construct1.parts.page.node.host", null, "test.node.hostname", null },
			new Object[] { "cms.page.tags.urls_construct1.parts.extpage.internal", null, "false", null },

			// tag part (indirect)
			new Object[] { "cms.page.tags.get('urls_construct1').parts.get('page').internal", null, "true", null },
			new Object[] { "cms.page.tags.get('urls_construct1').parts.get('page').url", null, "/node/pub/dir/test/Target-Page.de.html", null },
			new Object[] { "cms.page.tags.get('urls_construct1').parts.get('page').target.name", null, "Target Page", null },
			new Object[] { "cms.page.tags.get('urls_construct1').parts.get('page').node.host", null, "test.node.hostname", null },
			new Object[] { "cms.page.tags.get('urls_construct1').parts.get('extpage').internal", null, "false", null },

			// file properties
			new Object[] { "cms.folder.files[0].name", null, "testfile.txt", null },
			new Object[] { "cms.folder.files[0].description", null, "This is the test file", null },
			new Object[] { "cms.folder.files[0].size", null, "8", null },
			new Object[] { "cms.folder.files[0].sizeb", null, "8", null },
			new Object[] { "cms.folder.files[0].sizekb", null, "0.1", null },
			new Object[] { "cms.folder.files[0].sizemb", null, "0.1", null },
			new Object[] { "cms.folder.files[0].folder.name", null, "Testfolder", null },
			new Object[] { "cms.folder.files[0].extension", null, "txt", null },
			new Object[] { "cms.folder.files[0].creator.firstname", null, "Creator-First", null },
			new Object[] { "cms.folder.files[0].editor.firstname", null, "Editor-First", null },
			new Object[] { "cms.folder.files[0].createtimestamp", null, Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.files[0].createdate", null, creationdate, null },
			new Object[] { "cms.folder.files[0].edittimestamp", null, Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.files[0].editdate", null, editdate, null },
			new Object[] { "cms.folder.files[0].type", null, "text/plain", null },
			new Object[] { "cms.folder.files[0].url", null, "/node/pub/dir/bin/test/testfile.txt", null },
			new Object[] { "cms.folder.files[0].isfile", null, "true", null },
			new Object[] { "cms.folder.files[0].isimage", null, "false", null },
			new Object[] { "cms.folder.files[0].ismaster", null, "true", null },
			new Object[] { "cms.folder.files[0].inherited", null, "false", null },

			// image properties
			new Object[] { "cms.folder.images[0].name", null, "blume.jpg", null },
			new Object[] { "cms.folder.images[0].description", null, "This is the test image", null },
			new Object[] { "cms.folder.images[0].size", null, "190399", null },
			new Object[] { "cms.folder.images[0].sizeb", null, "190399", null },
			new Object[] { "cms.folder.images[0].sizekb", null, "186.0", null },
			new Object[] { "cms.folder.images[0].sizemb", null, "0.2", null },
			new Object[] { "cms.folder.images[0].folder.name", null, "Testfolder", null },
			new Object[] { "cms.folder.images[0].extension", null, "jpg", null },
			new Object[] { "cms.folder.images[0].creator.firstname", null, "Creator-First", null },
			new Object[] { "cms.folder.images[0].editor.firstname", null, "Editor-First", null },
			new Object[] { "cms.folder.images[0].createtimestamp", null, Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.images[0].createdate", null, creationdate, null },
			new Object[] { "cms.folder.images[0].edittimestamp", null, Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.images[0].editdate", null, editdate, null },
			new Object[] { "cms.folder.images[0].type", null, "image/jpeg", null },
			new Object[] { "cms.folder.images[0].url", null, "/node/pub/dir/bin/test/blume.jpg", null },
			new Object[] { "cms.folder.images[0].width", null, "1160", null },
			new Object[] { "cms.folder.images[0].height", null, "1376", null },
			new Object[] { "cms.folder.images[0].dpix", null, "600", null },
			new Object[] { "cms.folder.images[0].dpiy", null, "600", null },
			new Object[] { "cms.folder.images[0].dpi", null, "600", null },
			new Object[] { "cms.folder.images[0].fpx", null, "0.5", null },
			new Object[] { "cms.folder.images[0].fpy", null, "0.5", null },
			new Object[] { "cms.folder.images[0].isfile", null, "false", null },
			new Object[] { "cms.folder.images[0].isimage", null, "true", null },
			new Object[] { "cms.folder.images[0].ismaster", null, "true", null },
			new Object[] { "cms.folder.images[0].inherited", null, "false", null },

			new Object[] { "undefined", null, "undefined", null},

			new Object[] {"cms + \"_testHelperValue\"", "{{#with (testpackage.resolve \"outer\") as |outer|}}|{{gtx_render outer}}|{{#with (testpackage.resolve \"inner\") as |inner|}}|{{gtx_render inner}}|{{/with}}{{/with}}", "|outer_testHelperValue||inner_testHelperValue|", null}
		));
	}

	@Parameter(0)
	public String testedHelper;

	@Parameter(1)
	public String template;

	@Parameter(2)
	public String expectedResult;

	@Parameter(3)
	public List<Pair<String, String>> expectedDependencies;

	@Before
	public void setup() throws NodeException, IOException {
		Synchronizer.addPackage(TESTPACKAGE_NAME);

		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));

		File testPackageRoot = Synchronizer.getPackage(TESTPACKAGE_NAME).getPackagePath().toFile();
		File hbRoot = new File(testPackageRoot, "handlebars");
		File helpersRoot = new File(hbRoot, "helpers");
		File helperFile = new File(helpersRoot, "resolve.js");
		assertThat(helpersRoot.mkdirs()).as("Creation of dirs " + helpersRoot + " succeded").isTrue();

		if (!Strings.CS.startsWith(testedHelper, "function")) {
			testedHelper = String.format("function resolve(cms) { return %s;}", testedHelper);
		}

		FileUtils.writeStringToFile(helperFile, testedHelper, "UTF-8");

		testPage = update(testPage, p -> {
			getPartType(HandlebarsPartType.class, p.getContentTag("testtag"), "hb").setText(Optional.ofNullable(template).orElse(DEFAULT_TEMPLATE));
		}).at(editTimestamp).as(editor).unlock().build();

		testPage = update(testPage, p -> {
		}).at(publishTimestamp).as(publisher).unlock().publish().build();
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
