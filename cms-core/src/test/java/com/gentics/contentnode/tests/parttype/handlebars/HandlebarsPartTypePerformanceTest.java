package com.gentics.contentnode.tests.parttype.handlebars;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;
import com.gentics.contentnode.testutils.DBTestContext;

@RunWith(value = Parameterized.class)
public class HandlebarsPartTypePerformanceTest {
	public final static String TESTPACKAGE_NAME_PATTERN = "testpackage_%d";

	protected final static List<Integer> tagCounts = Arrays.asList(1, 10, 100, 1000);

	protected final static List<Integer> helperCounts = Arrays.asList(1, 10, 100, 1000);

	protected final static long tolerableRenderDurationMs = 5000;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@ClassRule
	public static PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	static String helperContents;

	static Construct handlebarsConstruct;

	static Node node;

	static Template template;

	static Map<Integer, Page> testPages = new HashMap<>();

	@Parameters(name = "{index}: tags {0}, helpers {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int numTags : tagCounts) {
			for (int numHelpers : helperCounts) {
				data.add(new Object[] { numTags, numHelpers });
			}
		}

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException, URISyntaxException {
		testContext.getContext().getTransaction().commit();

		helperContents = FileUtils.readFileToString(
				new File(HandlebarsPartTypePerformanceTest.class.getResource("big_helper.js").toURI()), "UTF-8");

		for (int numHelpers : helperCounts) {
			String packageName = String.format(TESTPACKAGE_NAME_PATTERN, numHelpers);
			Synchronizer.addPackage(packageName);

			File testPackageRoot = Synchronizer.getPackage(packageName).getPackagePath().toFile();
			File hbRoot = new File(testPackageRoot, "handlebars");
			File helpersRoot = new File(hbRoot, "helpers");
			assertThat(helpersRoot.mkdirs()).as("Creation of dirs " + helpersRoot + " succeded").isTrue();

			for (int i = 0; i < numHelpers; i++) {
				File helperFile = new File(helpersRoot, String.format("resolve%d.js", i));
				FileUtils.writeStringToFile(helperFile, helperContents, "UTF-8");
			}
		}

		handlebarsConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_handlebars");
			c.setName("Construct with Handlebars", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("hb");
				p.setName("Handlebars", 1);
			}).doNotSave().build());
		}).build();

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Test Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("test.node.hostname");
			n.setPublishDir("/");
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Test Template");
			t.setSource("<node testtag>");

			t.getTemplateTags().put("testtag", create(TemplateTag.class, tag -> {
				tag.setConstructId(handlebarsConstruct.getId());
				tag.setEnabled(true);
				tag.setName("testtag");
				tag.setPublic(true);
			}).doNotSave().build());
		}).unlock().build();

		for (int numTags : tagCounts) {
			Page page = create(Page.class, p -> {
				p.setFolderId(node.getFolder().getId());
				p.setTemplateId(template.getId());
				p.setName(String.format("Page with %d tags", numTags));

				Content content = p.getContent();
				StringBuilder testtagContent = new StringBuilder();
				for (int i = 0; i < numTags; i++) {
					ContentTag tag = content.addContentTag(handlebarsConstruct.getId());
					getPartType(HandlebarsPartType.class, tag, "hb").setText("{{ cms.tag.name }}");
					testtagContent.append(String.format("{{gtx_render cms.page.tags.%s }}|", tag.getName()));
				}
				getPartType(HandlebarsPartType.class, p.getContentTag("testtag"), "hb").setText(testtagContent.toString());
			}).unlock().build();

			testPages.put(numTags, page);
		}
	}

	@Rule
	public Stopwatch stopWatch = new Stopwatch();

	@Parameter(0)
	public int numTags;

	@Parameter(1)
	public int numHelpers;

	protected Page testPage;

	protected String expectedContent;

	@Before
	public void setup() throws NodeException, IOException {
		String testedPackage = String.format(TESTPACKAGE_NAME_PATTERN, numHelpers);

		for (int numHelpers : helperCounts) {
			String packageName = String.format(TESTPACKAGE_NAME_PATTERN, numHelpers);
			if (StringUtils.equals(packageName, testedPackage)) {
				operate(() -> Synchronizer.addPackage(node, packageName));
			} else {
				operate(() -> Synchronizer.removePackage(node, packageName));
			}
		}

		testPage = testPages.get(numTags);
		expectedContent = IntStream.range(0, numTags).mapToObj(i -> String.format("construct_with_handlebars%d|", i + 1)).collect(Collectors.joining());
	}

	@Test
	public void testRender() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(testPage.render()).as("Rendered page").isEqualTo(expectedContent);
			trx.success();
		}
		assertThat(stopWatch.runtime(TimeUnit.MILLISECONDS)).as("Test runtime (in ms)").isLessThanOrEqualTo(tolerableRenderDurationMs);
	}
}
