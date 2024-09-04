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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
		return ListUtils.union(getGenericTestCases(), Arrays.asList(
			new Object[] { loadHelper("folder_children.js"), "English Test Page,Subfolder,Test Page,blume.jpg,testfile.txt", Arrays.asList(Pair.of("testPage", "name"), Pair.of("englishPage", "name"), Pair.of("subFolder", "name"), Pair.of("testFile", "name"), Pair.of("testImage", "name"))},

			// file properties
			new Object[] { "cms.folder.files[0].name", "testfile.txt", null },
			new Object[] { "cms.folder.files[0].description", "This is the test file", null },
			new Object[] { "cms.folder.files[0].size", "8", null },
			new Object[] { "cms.folder.files[0].sizeb", "8", null },
			new Object[] { "cms.folder.files[0].sizekb", "0.1", null },
			new Object[] { "cms.folder.files[0].sizemb", "0.1", null },
			new Object[] { "cms.folder.files[0].folder.name", "Testfolder", null },
			new Object[] { "cms.folder.files[0].extension", "txt", null },
			new Object[] { "cms.folder.files[0].creator.firstname", "Creator-First", null },
			new Object[] { "cms.folder.files[0].editor.firstname", "Editor-First", null },
			new Object[] { "cms.folder.files[0].createtimestamp", Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.files[0].createdate", creationdate, null },
			new Object[] { "cms.folder.files[0].edittimestamp", Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.files[0].editdate", editdate, null },
			new Object[] { "cms.folder.files[0].type", "text/plain", null },
			new Object[] { "cms.folder.files[0].url", "/node/pub/dir/bin/test/testfile.txt", null },
			new Object[] { "cms.folder.files[0].isfile", "true", null },
			new Object[] { "cms.folder.files[0].isimage", "false", null },
			new Object[] { "cms.folder.files[0].ismaster", "true", null },
			new Object[] { "cms.folder.files[0].inherited", "false", null },

			// image properties
			new Object[] { "cms.folder.images[0].name", "blume.jpg", null },
			new Object[] { "cms.folder.images[0].description", "This is the test image", null },
			new Object[] { "cms.folder.images[0].size", "190399", null },
			new Object[] { "cms.folder.images[0].sizeb", "190399", null },
			new Object[] { "cms.folder.images[0].sizekb", "186.0", null },
			new Object[] { "cms.folder.images[0].sizemb", "0.2", null },
			new Object[] { "cms.folder.images[0].folder.name", "Testfolder", null },
			new Object[] { "cms.folder.images[0].extension", "jpg", null },
			new Object[] { "cms.folder.images[0].creator.firstname", "Creator-First", null },
			new Object[] { "cms.folder.images[0].editor.firstname", "Editor-First", null },
			new Object[] { "cms.folder.images[0].createtimestamp", Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.images[0].createdate", creationdate, null },
			new Object[] { "cms.folder.images[0].edittimestamp", Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.images[0].editdate", editdate, null },
			new Object[] { "cms.folder.images[0].type", "image/jpeg", null },
			new Object[] { "cms.folder.images[0].url", "/node/pub/dir/bin/test/blume.jpg", null },
			new Object[] { "cms.folder.images[0].width", "1160", null },
			new Object[] { "cms.folder.images[0].height", "1376", null },
			new Object[] { "cms.folder.images[0].dpix", "600", null },
			new Object[] { "cms.folder.images[0].dpiy", "600", null },
			new Object[] { "cms.folder.images[0].dpi", "600", null },
			new Object[] { "cms.folder.images[0].fpx", "0.5", null },
			new Object[] { "cms.folder.images[0].fpy", "0.5", null },
			new Object[] { "cms.folder.images[0].isfile", "false", null },
			new Object[] { "cms.folder.images[0].isimage", "true", null },
			new Object[] { "cms.folder.images[0].ismaster", "true", null },
			new Object[] { "cms.folder.images[0].inherited", "false", null },

			new Object[] { "undefined", "undefined", null}
		));
	}

	@Parameter(0)
	public String testedHelper;

	@Parameter(1)
	public String expectedResult;

	@Parameter(2)
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

		if (!StringUtils.startsWith(testedHelper, "function")) {
			testedHelper = String.format("function resolve(cms) { return %s;}", testedHelper);
		}

		FileUtils.writeStringToFile(helperFile, testedHelper, "UTF-8");

		testPage = update(testPage, p -> {
			getPartType(HandlebarsPartType.class, p.getContentTag("testtag"), "hb").setText("{{testpackage.resolve cms}}");
		}).at(editTimestamp).as(editor).unlock().build();

		testPage = update(testPage, p -> {
		}).at(publishTimestamp).as(publisher).unlock().publish().build();
	}

	@Test
	public void testRender() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(testPage.render()).as("Rendered page").isEqualTo(expectedResult);
			trx.success();
		}

		assertDependencies(expectedDependencies);
	}
}
