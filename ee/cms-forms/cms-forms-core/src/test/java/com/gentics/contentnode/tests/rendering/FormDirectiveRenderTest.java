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
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.forms.Migration;
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
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.Controls;
import com.gentics.contentnode.rest.model.formtype.FormFlow;
import com.gentics.contentnode.rest.model.formtype.FormFlowStep;
import com.gentics.contentnode.rest.model.formtype.FormFlows;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the FormDirective
 */
@RunWith(value = Parameterized.class)
@ConfigBy(FormDirectiveRenderTest.class)
public class FormDirectiveRenderTest implements ConfigModifier {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Construct vtlFormConstruct;

	private static Construct hbsFormConstruct;

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
	public final static Map<String, String> VTL_TEMPLATES = new HashMap<>();

	/**
	 * Tested hbs templates
	 */
	public final static Map<String, String> HBS_TEMPLATES = new HashMap<>();

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

		String preview = "";
		try (InputStream in = FormDirectiveRenderTest.class.getResourceAsStream("preview.txt")) {
			StringWriter out = new StringWriter();
			IOUtils.copy(in, out, Charset.forName("UTF-8"));
			preview = out.toString();
		} catch (IOException e) {
			fail(e);
		}

		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form)");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form}}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "id";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form.target.id)");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form.target.id}}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "string";
		VTL_TEMPLATES.put(key, "#gtx_form(\"$cms.tag.parts.form\")");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form}}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "noform";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"noForm\": \"No Form\"})");
		HBS_TEMPLATES.put(key, "{{#gtx_form cms.tag.parts.form}}{{{this}}}{{else}}No Form{{/gtx_form}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "No Form");
		EXPECTED_PREVIEW.put(key, preview);

		key = "php";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"type\": \"php\"})");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form type=\"php\"}}}");
		EXPECTED_PUBLISHED.put(key, "@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'pluginName' => 'forms', 'language' => $node['language']])");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "span";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"previewRoot\": \"span\"})");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form previewRoot=\"span\"}}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "resultsJava";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"showResults\": true})");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form showResults=true}}}");
		EXPECTED_PUBLISHED.put(key, "<eval>{ renderForm \"%s\" pluginName=\"forms\" language=data.node.language showResults=true }</eval>");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);

		key = "resultsPhp";
		VTL_TEMPLATES.put(key, "#gtx_form($cms.tag.parts.form, {\"type\": \"php\", \"showResults\": true})");
		HBS_TEMPLATES.put(key, "{{{gtx_form cms.tag.parts.form type=\"php\" showResults=true}}}");
		EXPECTED_PUBLISHED.put(key, "@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'pluginName' => 'forms', 'language' => $node['language'], 'showResults' => true])");
		EXPECTED_NOFORM.put(key, "");
		EXPECTED_PREVIEW.put(key, preview);
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
					for (String vtlTemplate : VTL_TEMPLATES.keySet()) {
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

		vtlFormConstruct = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("vtl_form");
			c.setName("vtl_form", 1);
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

		hbsFormConstruct = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("hbs_form");
			c.setName("hbs_form", 1);
			c.getNodes().add(node);

			// HBS part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("hbs");
				p.setName("hbs", 1);
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
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
			tmpl.setSource("no test selected");
			tmpl.getTemplateTags().put("vtl_form", create(TemplateTag.class, tt -> {
				tt.setConstructId(vtlFormConstruct.getId());
				tt.setEnabled(true);
				tt.setName("vtl_form");
				tt.setPublic(true);
			}, false));
			tmpl.getTemplateTags().put("hbs_form", create(TemplateTag.class, tt -> {
				tt.setConstructId(hbsFormConstruct.getId());
				tt.setEnabled(true);
				tt.setName("hbs_form");
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
								getPartType(CmsFormPartType.class, upd.getTag("vtl_form"), "form").setTarget(form);
								getPartType(CmsFormPartType.class, upd.getTag("hbs_form"), "form").setTarget(form);
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
		vtlFormConstruct = update(vtlFormConstruct, upd -> {
			upd.getValues().getByKeyname(TEMPLATE_PARTNAME).setValueText(VTL_TEMPLATES.get(templateCode));
		});

		// set tested hbs template
		hbsFormConstruct = update(hbsFormConstruct, upd -> {
			upd.getValues().getByKeyname("hbs").setValueText(HBS_TEMPLATES.get(templateCode));
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
	public void testRenderVtl() throws NodeException {
		template = update(template, t -> {
			t.setSource("<node vtl_form>");
		});

		operate(systemUser, () -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, feature);
					RenderTypeTrx rt = new RenderTypeTrx(editModeInt)) {
				RenderResult renderResult = new RenderResult();
				String content = page.render(renderResult);

				// strip away the head, which probably was rendered for aloha editor (we don't want to test it, because it contains
				// JSON and the order of the properties might vary on different systems)
				Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);
				content = pattern.matcher(content).replaceAll("");
				content = content.replaceAll("\r\n", "\n");

				assertThat(renderResult.getReturnCode()).as("Render result").isEqualTo("OK");
				assertThat(content).as("Rendered content").isEqualTo(getExpectedContent("vtl_form"));
			}
		});
	}

	@Test
	public void testRenderHbs() throws NodeException {
		template = update(template, t -> {
			t.setSource("<node hbs_form>");
		});

		operate(systemUser, () -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.FORMS, feature);
					RenderTypeTrx rt = new RenderTypeTrx(editModeInt)) {
				RenderResult renderResult = new RenderResult();
				String content = page.render(renderResult);

				// strip away the head, which probably was rendered for aloha editor (we don't want to test it, because it contains
				// JSON and the order of the properties might vary on different systems)
				Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);
				content = pattern.matcher(content).replaceAll("");
				content = content.replaceAll("\r\n", "\n");

				assertThat(renderResult.getReturnCode()).as("Render result").isEqualTo("OK");
				assertThat(content).as("Rendered content").isEqualTo(getExpectedContent("hbs_form"));
			}
		});
	}

	/**
	 * Get the expected content
	 * @return expected content
	 * @throws NodeException
	 */
	protected String getExpectedContent(String tagName) throws NodeException {
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
				String sid = TransactionManager.getCurrentTransaction().getSession().getSessionId();
				String uuid = MeshPublisher.getMeshUuid(form);

				renderedTag = EXPECTED_PREVIEW.get(templateCode).formatted(form.getId(), sid, language, uuid, language);
				break;
			}
		}

		switch (editModeInt) {
		case RenderType.EM_ALOHA:
			int pageId = page.getId();
			int tagId = execute(p -> p.getContentTag(tagName).getId(), page);
			renderedTag = String.format(
					"<!DOCTYPE html>\n<body>\n<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"%s\" data-gcn-i18n-constructname=\"%s\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s</div></body>\n",
					pageId, tagId, tagName, tagName, tagId, renderedTag);
			break;
		case RenderType.EM_ALOHA_READONLY:
			renderedTag = String.format("<!DOCTYPE html>\n<body>\n%s</body>\n", renderedTag);
			break;
		}

		return renderedTag;
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		ObjectMapper mapper = new ObjectMapper();
		YAMLMapper yamlMapper = new YAMLMapper();

		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		// prepare forms config
		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("de", "CMS Formular", "en", "CMS Form"),
				"forms");
		ControlConfig stringControl = new ControlConfig(Map.of("de", "Eingabefeld"));

		StringSetting stringSetting = new StringSetting("rich", Map.of("de", "Reicher Text"));
		stringSetting.backend = true;
		stringSetting.richContent = true;
		stringControl.settings = Arrays.asList(stringSetting);

		formsConfig.controls = new Controls(Map.of("string", stringControl));

		// add a dummy flow
		formsConfig.flows = new FormFlows();
		formsConfig.flows.add(new FormFlow("testFlow", Map.of("de", "Test Ablauf"), "test.flow", List.of(new FormFlowStep("test.flow.step", "test.flow.step.button", "test.flow.step.react.class"))));

		try {
			formsConfig.pluginConfiguration = mapper.readValue("{\"password\": \"supersecret\"}", ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
		formTypeConfiguration.put(FormTypeConfigurationModifier.GENERIC_TYPE, formsConfig);

		// prepare andp config
		FormTypeConfiguration andpConfig = new FormTypeConfiguration(Map.of("de", "ANDP Formular", "en", "ANDP Form"), "acta-nova");
		andpConfig.external = true;
		try {
			andpConfig.pluginConfiguration = mapper.readValue("{\"password\": \"supersecret\"}", ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
		formTypeConfiguration.put(FormTypeConfigurationModifier.ANDP_TYPE, andpConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);

		// add default translations
		try (InputStream in = Migration.class.getResourceAsStream(Migration.FORMGEN_TRANSLATIONS_FILENAME)) {
			prefs.set(FormConfigProvider.TRANSLATIONS_CONFIG_PATH, yamlMapper.readValue(in, FormGenTranslations.class));
		} catch (IOException e) {
			throw new NodeException("Error while reading translations", e);
		}
	}
}
