package com.gentics.contentnode.tests.parttype.handlebars;

import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;

/**
 * Test cases for rendering in handlebars
 */
@RunWith(value = Parameterized.class)
public class HandlebarsPartTypeTemplateRenderingTest extends AbstractHandlebarsPartTypeRenderingTest {
	@Parameters(name = "{index}: template {0}")
	public static Collection<Object[]> data() {
		return ListUtils.union(getGenericTestCases(), Arrays.asList(
			new Object[] { "{{#each cms.folder.folders }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "Subfolder", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "folders"), Pair.of("subFolder", "name")) },
			new Object[] { "{{#each (sort cms.folder.pages 'name' 'desc') }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "Test Page,English Test Page", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "pages"), Pair.of("testPage", "name"), Pair.of("englishPage", "name")) },
			new Object[] { "{{#each (sort cms.folder.pages 'name') }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "English Test Page,Test Page", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "pages"), Pair.of("testPage", "name"), Pair.of("englishPage", "name")) },
			new Object[] { "{{#each cms.folder.files }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "testfile.txt", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "files"), Pair.of("testFile", "name"))},
			new Object[] { "{{#each cms.folder.images }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "blume.jpg", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "images"), Pair.of("testImage", "name"))},
			new Object[] { "{{#each (sort cms.folder.filesandimages 'type' 'asc') }}{{ url }} ({{ type }}){{#unless @last}},{{/unless}}{{/each}}", "/node/pub/dir/bin/test/blume.jpg (image/jpeg),/node/pub/dir/bin/test/testfile.txt (text/plain)", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "filesandimages"), Pair.of("testFile", "url"), Pair.of("testImage", "url"))},
			new Object[] { "{{#each (sort cms.folder.filesandimages 'type' 'desc') }}{{ url }} ({{ type }}){{#unless @last}},{{/unless}}{{/each}}", "/node/pub/dir/bin/test/testfile.txt (text/plain),/node/pub/dir/bin/test/blume.jpg (image/jpeg)", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "filesandimages"), Pair.of("testFile", "url"), Pair.of("testImage", "url"))},
			new Object[] { "{{#each (sort cms.folder.children 'name' 'asc')}}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "blume.jpg,English Test Page,Subfolder,testfile.txt,Test Page", null},
			new Object[] { "{{#each cms.page.versions }}{{ number }}{{#unless @last}},{{/unless}}{{/each}}", "1.0,0.2,0.1", null},
			new Object[] { "{{#each cms.page.tags }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "testtag", null },
			new Object[] { "{{#each (sort cms.page.languageset.pages 'language.code' 'asc') }}{{ language.code }}{{#unless @last}},{{/unless}}{{/each}}", "de,en", null },
			new Object[] { "{{#each (sort cms.page.languageset.pages 'language.code' 'desc') }}{{ language.code }}{{#unless @last}},{{/unless}}{{/each}}", "en,de", null },
			new Object[] { "{{#with cms.page.languageset.pages.de}}{{ name }}{{/with}}", "Test Page", null },
			new Object[] { "{{#with cms.page.languageset.pages.en}}{{ name }}{{/with}}", "English Test Page", null },
			new Object[] { "{{#each cms.page.pagevariants }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "Test Page", null },
			new Object[] { "{{#each cms.page.template.tags }}{{ name }}{{#unless @last}},{{/unless}}{{/each}}", "testtag", null },
			new Object[] { "{{render cms.page.object.secondpage}}", "Contents of second from page", null},

			// file properties
			new Object[] { "cms.folder.files.[0].name", "testfile.txt", null },
			new Object[] { "cms.folder.files.[0].description", "This is the test file", null },
			new Object[] { "cms.folder.files.[0].size", "8", null },
			new Object[] { "cms.folder.files.[0].sizeb", "8", null },
			new Object[] { "cms.folder.files.[0].sizekb", "0.1", null },
			new Object[] { "cms.folder.files.[0].sizemb", "0.1", null },
			new Object[] { "cms.folder.files.[0].folder.name", "Testfolder", null },
			new Object[] { "cms.folder.files.[0].extension", "txt", null },
			new Object[] { "cms.folder.files.[0].creator.firstname", "Creator-First", null },
			new Object[] { "cms.folder.files.[0].editor.firstname", "Editor-First", null },
			new Object[] { "cms.folder.files.[0].createtimestamp", Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.files.[0].createdate", creationdate, null },
			new Object[] { "cms.folder.files.[0].edittimestamp", Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.files.[0].editdate", editdate, null },
			new Object[] { "cms.folder.files.[0].type", "text/plain", null },
			new Object[] { "cms.folder.files.[0].url", "/node/pub/dir/bin/test/testfile.txt", null },
			new Object[] { "cms.folder.files.[0].isfile", "true", null },
			new Object[] { "cms.folder.files.[0].isimage", "false", null },
			new Object[] { "cms.folder.files.[0].ismaster", "true", null },
			new Object[] { "cms.folder.files.[0].inherited", "false", null },

			// image properties
			new Object[] { "cms.folder.images.[0].name", "blume.jpg", null },
			new Object[] { "cms.folder.images.[0].description", "This is the test image", null },
			new Object[] { "cms.folder.images.[0].size", "190399", null },
			new Object[] { "cms.folder.images.[0].sizeb", "190399", null },
			new Object[] { "cms.folder.images.[0].sizekb", "186.0", null },
			new Object[] { "cms.folder.images.[0].sizemb", "0.2", null },
			new Object[] { "cms.folder.images.[0].folder.name", "Testfolder", null },
			new Object[] { "cms.folder.images.[0].extension", "jpg", null },
			new Object[] { "cms.folder.images.[0].creator.firstname", "Creator-First", null },
			new Object[] { "cms.folder.images.[0].editor.firstname", "Editor-First", null },
			new Object[] { "cms.folder.images.[0].createtimestamp", Integer.toString(creationTimestamp), null },
			new Object[] { "cms.folder.images.[0].createdate", creationdate, null },
			new Object[] { "cms.folder.images.[0].edittimestamp", Integer.toString(editTimestamp), null },
			new Object[] { "cms.folder.images.[0].editdate", editdate, null },
			new Object[] { "cms.folder.images.[0].type", "image/jpeg", null },
			new Object[] { "cms.folder.images.[0].url", "/node/pub/dir/bin/test/blume.jpg", null },
			new Object[] { "cms.folder.images.[0].width", "1160", null },
			new Object[] { "cms.folder.images.[0].height", "1376", null },
			new Object[] { "cms.folder.images.[0].dpix", "600", null },
			new Object[] { "cms.folder.images.[0].dpiy", "600", null },
			new Object[] { "cms.folder.images.[0].dpi", "600", null },
			new Object[] { "cms.folder.images.[0].fpx", "0.5", null },
			new Object[] { "cms.folder.images.[0].fpy", "0.5", null },
			new Object[] { "cms.folder.images.[0].isfile", "false", null },
			new Object[] { "cms.folder.images.[0].isimage", "true", null },
			new Object[] { "cms.folder.images.[0].ismaster", "true", null },
			new Object[] { "cms.folder.images.[0].inherited", "false", null }

		));
	}

	@Parameter(0)
	public String testedTemplate;

	@Parameter(1)
	public String expectedResult;

	@Parameter(2)
	public List<Pair<String, String>> expectedDependencies;

	@Before
	public void setup() throws NodeException {
		if (!StringUtils.startsWith(testedTemplate, "{{")) {
			testedTemplate = String.format("{{ %s }}", testedTemplate);
		}
		testPage = update(testPage, p -> {
			getPartType(HandlebarsPartType.class, p.getContentTag("testtag"), "hb").setText(testedTemplate);
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
