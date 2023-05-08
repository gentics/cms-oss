package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for automatic filename extensions
 */
@RunWith(Parameterized.class)
public class PageFilenameExtTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private final static String PAGENAME = "Page";

	private final static String FILENAME = "filename";

	private final static String EXTENSION = "txt";

	private final static Operation[] operationOptions = Operation.values();

	private final static Filename[] filenameOptions = Filename.values();

	private final static List<Boolean> featureOptions = Arrays.asList(true, false);

	private final static List<String> languageOptions = Arrays.asList("en", "");

	private final static List<String> templateExtensions = Arrays.asList("html", "xml", "txt");

	private final static List<Boolean> forceOptions = Arrays.asList(false, true);

	private static ContentLanguage english;

	private static Node node;

	private static Map<String, Template> templates = new HashMap<>();

	@Parameters(name = "{index}: operation {0}, filename {1}, contentgroup3_pagefilename {2}, language {3}, template {4}, force {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Operation op : operationOptions) {
			for (Filename filename : filenameOptions) {
				for (boolean contentgroup3Pagefilename : featureOptions) {
					for (String language : languageOptions) {
						for (String template : templateExtensions) {
							for (boolean force : forceOptions) {
								if (op == Operation.update && force) {
									continue;
								}
								data.add(new Object[] { op, filename, contentgroup3Pagefilename, language, template, force });
							}
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Setup static test data
	 * @throws NodeException 
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		english = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(ContentLanguage.class,
				DBUtils.select("SELECT id FROM contentgroup WHERE code = 'en'", DBUtils.IDS).iterator().next()));
		node = Trx.supply(() -> createNode("hostname", "Node", PublishTarget.NONE, english));

		for (String ext : templateExtensions) {
			Integer mlId = Trx.supply(() -> DBUtils.select("SELECT id FROM ml WHERE ext = '" + ext + "'", DBUtils.IDS).iterator().next());

			templates.put(ext, Trx.supply(() -> create(Template.class, t -> {
				t.setName("Template " + ext);
				t.addFolder(node.getFolder());
				t.setMlId(mlId);
				t.setSource("");
			})));
		}
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			for (com.gentics.contentnode.object.Page p : node.getFolder().getPages()) {
				p.delete(true);
			}
		});
	}

	@Parameter(0)
	public Operation operation;

	@Parameter(1)
	public Filename filename;

	@Parameter(2)
	public boolean contentgroup3Pagefilename;

	@Parameter(3)
	public String language;

	@Parameter(4)
	public String template;

	@Parameter(5)
	public boolean force;

	@Test
	public void test() throws Exception {
		try (FeatureClosure f = new FeatureClosure(Feature.CONTENTGROUP3_PAGEFILENAME, contentgroup3Pagefilename)) {
			Page page = null;
			switch (operation) {
			case create:
				page = Trx.supply(() -> {
					PageCreateRequest create = new PageCreateRequest();
					create.setPageName("Page");
					create.setFolderId(node.getFolder().getId().toString());
					create.setTemplateId(templates.get(template).getId());
					create.setFileName(filename.filename);
					if (!"".equals(language)) {
						create.setLanguage(language);
					}

					create.setForceExtension(force);

					PageLoadResponse response = ContentNodeRESTUtils.getPageResource().create(create);
					ContentNodeRESTUtils.assertResponseOK(response);
					return response.getPage();
				});
				break;
			case update:
				page = Trx.supply(() -> {
					PageCreateRequest create = new PageCreateRequest();
					create.setPageName("Page");
					create.setFolderId(node.getFolder().getId().toString());
					create.setTemplateId(templates.get(template).getId());
					create.setFileName("lalala.html");
					if (!"".equals(language)) {
						create.setLanguage(language);
					}

					PageLoadResponse createResponse = ContentNodeRESTUtils.getPageResource().create(create);
					ContentNodeRESTUtils.assertResponseOK(createResponse);
					Page created = createResponse.getPage();

					created.setFileName(filename.filename);

					PageSaveRequest update = new PageSaveRequest(created);
					if (filename == Filename.empty) {
						update.setDeriveFileName(true);
					}

					GenericResponse updateResponse = ContentNodeRESTUtils.getPageResource().save(created.getId().toString(), update);
					ContentNodeRESTUtils.assertResponseOK(updateResponse);

					PageLoadResponse loadResponse = ContentNodeRESTUtils.getPageResource().load(created.getId().toString(), false, false, false, false, false, false, false, false, false, false, null, null);
					ContentNodeRESTUtils.assertResponseOK(loadResponse);

					return loadResponse.getPage();
				});
				break;
			default:
				fail(String.format("Unknown operation %s", operation));
			}

			String baseName = "";
			String languageExtension = "";
			String extension = "";
			switch (filename) {
			case empty:
				baseName = PAGENAME;
				if (contentgroup3Pagefilename && !"".equals(language)) {
					languageExtension = "." + language;
				}
				extension = "." + template;
				break;
			case no_extension:
			case extension:
				baseName = filename.filename;
				if (!baseName.endsWith("." + template)) {
					extension = "." + template;
				}
				break;
			}
			String expectedFilename = (filename != Filename.empty && force) ? baseName : String.format("%s%s%s", baseName, languageExtension, extension);

			assertThat(page.getFileName()).as("Page filename").isEqualTo(expectedFilename);
		}
	}

	public static enum Filename {
		empty(null), no_extension(FILENAME), extension(String.format("%s.%s", FILENAME, EXTENSION));

		String filename;

		Filename(String filename) {
			this.filename = filename;
		}
	}

	/**
	 * Enum for possible operations
	 */
	public static enum Operation {
		create, update
	}
}
