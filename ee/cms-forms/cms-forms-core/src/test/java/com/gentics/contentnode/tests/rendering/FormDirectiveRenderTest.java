package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.TEMPLATE_PARTNAME;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormRESTUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.ConfigBy;

/**
 * Test cases for the FormDirective
 */
@RunWith(value = Parameterized.class)
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormDirectiveRenderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Construct formConstruct;

	private static Template template;

	private static Form form;

	private static Map<String, Page> pages = new HashMap<>();

	private static SystemUser systemUser;

	/**
	 * Tested render modes
	 */
	public final static List<String> RENDER_MODES = Arrays.asList("publish", "preview", "aloha_readonly", "aloha");

	/**
	 * Tested vtl templates
	 */
	public final static Map<String, String> TEMPLATES = new HashMap<>();

	/**
	 * Expected rendered content (in publish mode)
	 */
	public final static Map<String, String> EXPECTED_PUBLISHED = new HashMap<>();

	/**
	 * Expected rendered content, if no form present (or offline)
	 */
	public final static Map<String, String> EXPECTED_NOFORM = new HashMap<>();

	/**
	 * Expected rendered content in preview mode
	 */
	public final static Map<String, String> EXPECTED_PREVIEW = new HashMap<>();

	/**
	 * Tested page languages
	 */
	public final static List<String> LANGUAGES = Arrays.asList("de", "en", "fr");

	static {
		String key = "default";

		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form)");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "id";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form.target.id)");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "string";
		TEMPLATES.put(key, "#gtx_form(\"$cms.tag.parts.form\")");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "noform";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"noForm\": \"No Form\"})");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "No Form");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "php";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"type\": \"php\"})");
		EXPECTED_PUBLISHED.put(key, "@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'language' => $node['language']])");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "span";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"previewRoot\": \"span\"})");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<span data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></span>");

		key = "resultsJava";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"showResults\": true})");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language showResults=true }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "resultsPhp";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"type\": \"php\", \"showResults\": true})");
		EXPECTED_PUBLISHED.put(key, "@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'language' => $node['language'], 'showResults' => true])");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "contextJava";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"templateContext\": \"customTemplates\"})");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" language=data.node.language templateContext=\"customTemplates\" }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");

		key = "contextPhp";
		TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"type\": \"php\", \"templateContext\": \"customTemplates\"})");
		EXPECTED_PUBLISHED.put(key, "@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'language' => $node['language'], 'templateContext' => 'customTemplates'])");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, "<div data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"gcn-form-preview\"></div>");
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: feature {0}, online {1}, rendermode: {2}, template {3}, language {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (boolean feature : Arrays.asList(true, false)) {
			for (boolean online : Arrays.asList(true, false)) {
				for (String renderMode : RENDER_MODES) {
					for (String vtlTemplate : TEMPLATES.keySet()) {
						for (String language : LANGUAGES) {
							data.add(new Object[] { feature, online, renderMode, vtlTemplate, language });
						}
					}
				}
			}
		}

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en"), getLanguage("fr")));

		node = update(node, n -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, true)) {
				n.activateFeature(Feature.FORMS);
			}
		});

		formConstruct = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("form");
			c.setName("form", 1);
			c.getNodes().add(node);

			// VTL part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("vtl");
				p.setName("vtl", 1);
				p.setPartTypeId(getPartTypeId(VelocityPartType.class));
			}, false));

			// Template part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(true);
				p.setKeyname(TEMPLATE_PARTNAME);
				p.setName(TEMPLATE_PARTNAME, 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
					v.setPart(p);
				}, false));
			}, false));

			// Form part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("form");
				p.setName("form", 1);
				p.setPartTypeId(getPartTypeId(CmsFormPartType.class));
			}, false));
		}));

		template = supply(() -> update(createTemplate(node.getFolder(), "Template"), tmpl -> {
			tmpl.setSource("<node form>");
			tmpl.getTemplateTags().put("form", create(TemplateTag.class, tt -> {
				tt.setConstructId(formConstruct.getId());
				tt.setEnabled(true);
				tt.setName("form");
				tt.setPublic(true);
			}, false));
		}));

		form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());
		}));

		for (String language : LANGUAGES) {
			pages.put(language,
					supply(() -> update(
							createPage(node.getFolder(), template, "Page in " + language, null, getLanguage(language)),
							upd -> {
								getPartType(CmsFormPartType.class, upd.getTag("form"), "form").setTarget(form);
							})));
		}

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public boolean online;

	@Parameter(2)
	public String editMode;

	@Parameter(3)
	public String templateCode;

	@Parameter(4)
	public String language;

	protected Page page;

	protected int editModeInt;

	@Before
	public void setup() throws NodeException {
		// set tested vtl template
		formConstruct = update(formConstruct, upd -> {
			upd.getValues().getByKeyname(TEMPLATE_PARTNAME).setValueText(TEMPLATES.get(templateCode));
		});

		// set online status of form
		operate(() -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, true)) {
				if (online) {
					form = update(form, Form::publish);
				} else {
					form = update(form, Form::takeOffline);
				}
			}
		});

		// select rendered page
		page = pages.get(language);

		// parse edit mode
		editModeInt = RenderType.parseEditMode(editMode);
	}

	@Test
	public void testRender() throws NodeException {
		operate(systemUser, () -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, feature);
					RenderTypeTrx rt = new RenderTypeTrx(editModeInt)) {
				RenderResult renderResult = new RenderResult();
				String content = page.render(renderResult);

				// strip away the head, which probably was rendered for aloha editor (we don't want to test it, because it contains
				// JSON and the order of the properties might vary on different systems)
				Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);
				content = pattern.matcher(content).replaceAll("");

				assertThat(renderResult.getReturnCode()).as("Render result").isEqualTo("OK");
				assertThat(content).as("Rendered content").isEqualTo(getExpectedContent());
			}
		});
	}

	/**
	 * Get the expected content
	 * @return expected content
	 * @throws NodeException
	 */
	protected String getExpectedContent() throws NodeException {
		String renderedTag = "";

		if (editModeInt == RenderType.EM_PUBLISH && !online) {
			// if form not preset (or offline)
			renderedTag = EXPECTED_NOFORM.get(templateCode);
		} else {
			switch (editModeInt) {
			case RenderType.EM_PUBLISH:
				renderedTag = supply(() -> String.format(EXPECTED_PUBLISHED.get(templateCode), MeshPublisher.getMeshUuid(form)));
				break;
			default:
				renderedTag = String.format(EXPECTED_PREVIEW.get(templateCode), form.getId(), language);
				break;
			}
		}

		switch (editModeInt) {
		case RenderType.EM_ALOHA:
			int pageId = page.getId();
			int tagId = execute(p -> p.getContentTag("form").getId(), page);
			renderedTag = String.format(
					"<!DOCTYPE html>\n<body>\n<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"form\" data-gcn-i18n-constructname=\"form\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s</div></body>\n",
					pageId, tagId, tagId, renderedTag);
			break;
		case RenderType.EM_ALOHA_READONLY:
			renderedTag = String.format("<!DOCTYPE html>\n<body>\n%s</body>\n", renderedTag);
			break;
		}

		return renderedTag;
	}
}
