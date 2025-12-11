package com.gentics.contentnode.tests.multichannelling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.LocalizationType;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@GCNFeature(set = {Feature.PARTIAL_MULTICHANNELLING})
public class PartialLocalizationsRenderTest {

	public enum LocalizationTarget {
		NONE,
		ROOT_EMPTY,
		ROOT_WITH_EMBEDDED,
		EMBEDDED
	}

	public final static List<String> RENDER_MODES = Arrays.asList("publish", "preview", "aloha_readonly", "aloha");

	@Parameters(name = "{index}: localize target: {0}, render mode: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (var feature: LocalizationTarget.values()) {
			for (var renderMode: RENDER_MODES) {
				data.add(new Object[] {feature, renderMode});
			}
		}

		return data;
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private final static String TAGNAME = "testtag";
	private final static String OTHER_TAGNAME = "othertesttag";
	private final static String PARTNAME = "text";

	private static Node node;
	private static Node channel;
	private static Folder folder;
	private static Template template;
	private static int constructId;
	private static SystemUser systemUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> ContentNodeTestDataUtils.createNode());
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));
		channel = supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		constructId = supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "construct", PARTNAME));
		template = supply(() -> ContentNodeTestDataUtils.createTemplate(
			node.getFolder(),
			"<node " + TAGNAME + ">\n<node " + OTHER_TAGNAME + ">",
			"Template",
			ContentNodeTestDataUtils.createTemplateTag(constructId, OTHER_TAGNAME, true, false),
			ContentNodeTestDataUtils.createTemplateTag(constructId, TAGNAME, true, false)));
		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Parameter
	public LocalizationTarget localizationTarget;

	@Parameter(1)
	public String editMode;


	@Test
	public void testRender() throws NodeException {
		var page = createPage("Testpage");

		var localizedRestPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);

		switch (localizationTarget) {
			case ROOT_EMPTY:
				execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), OTHER_TAGNAME), localizedRestPage);

				break;

			case ROOT_WITH_EMBEDDED: // Fall through
			case EMBEDDED:
				execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedRestPage);

				break;
		}

		var localizedPage = supply(t -> t.getObject(Page.class, localizedRestPage.getId(), true));

		operate(() -> {
			switch (localizationTarget) {
				case ROOT_EMPTY: {
					var templateTag = localizedPage.getContentTag(OTHER_TAGNAME);
					var templateTagTextPart = templateTag.getValues().getByKeyname(PARTNAME);

					templateTagTextPart.setValueText("LOCALIZED_ROOT_TWO");

				} break;

				case ROOT_WITH_EMBEDDED: {
					var templateTag = localizedPage.getContentTag(TAGNAME);
					var templateTagTextPart = templateTag.getValues().getByKeyname(PARTNAME);
					var oldValue = templateTagTextPart.getValueText();

					templateTagTextPart.setValueText(oldValue.replace("MASTER_ROOT", "LOCALIZED_ROOT"));
				} break;

				case EMBEDDED: {
					var localizedEmbeddedTagName = localizedPage.getTags().keySet().stream()
						.filter(t -> !t.equals(TAGNAME) && !t.equals(OTHER_TAGNAME))
						.toList()
						.get(0);
					var contentTag = localizedPage.getContentTag(localizedEmbeddedTagName);
					var contentTagTextPart = contentTag.getValues().getByKeyname(PARTNAME);

					contentTagTextPart.setValueText("LOCALIZED_EMBEDDED");
				} break;
			}

			localizedPage.save();
		});

		operate(systemUser, () -> {
			try (var renderTrx = new RenderTypeTrx(RenderType.parseEditMode(editMode))) {
				assertRenderResult(localizedPage);
			}
		});
	}

	/**
	 * Verify that the page is rendered as expected.
	 * @param page The page to render.
	 */
	private void assertRenderResult(Page page) throws NodeException {
		var renderResult = new RenderResult();
		var result = page.render(renderResult);
		var renderedEmbedded = getExpectedRenderedTag(page, LocalizationTarget.EMBEDDED, "");
		var renderedRootOne = getExpectedRenderedTag(page, LocalizationTarget.ROOT_WITH_EMBEDDED, renderedEmbedded);
		var renderedRootTwo = getExpectedRenderedTag(page, LocalizationTarget.ROOT_EMPTY, "");
		var expectedResult = renderedRootOne + "\n" + renderedRootTwo;
		var pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.MULTILINE);

		// Remove the HTML head since it contains Aloha config which is a JSON with unpredictable field order.
		result = pattern.matcher(result).replaceAll("");

		if (editMode.equals("aloha_readonly") || editMode.equals("aloha")) {
			expectedResult = "<!DOCTYPE html>\n<body>\n" + expectedResult + "</body>\n";
		}

		assertThat(result).isEqualTo(expectedResult);
	}

	/**
	 * Get the expected result for rendering the specified tag.
	 *
	 * @param page The rendered page.
	 * @param renderedTag The tag to render.
	 * @param contents The embedded contents of the tag (may be empty).
	 * @return The expected result for rendering the specified tag.
	 */
	private String getExpectedRenderedTag(Page page, LocalizationTarget renderedTag, String contents) throws NodeException {
		switch (renderedTag) {
			case ROOT_EMPTY: {
				if (localizationTarget == LocalizationTarget.ROOT_EMPTY) {
					if (editMode.equals("aloha")) {
						var tag = page.getContentTag(OTHER_TAGNAME);

						return String.format(
								"<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"othertesttag\" data-gcn-i18n-constructname=\"construct\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">LOCALIZED_ROOT_TWO</div>",
								page.getId(),
								tag.getId(),
								tag.getId());
					}

					return "LOCALIZED_ROOT_TWO";
				}

				return "MASTER_ROOT_TWO";
			}

			case ROOT_WITH_EMBEDDED:
				if (localizationTarget == LocalizationTarget.ROOT_WITH_EMBEDDED || localizationTarget == LocalizationTarget.EMBEDDED) {
					var rootState = localizationTarget == LocalizationTarget.ROOT_WITH_EMBEDDED ? "LOCALIZED" : "MASTER";

					if (editMode.equals("aloha")) {
						var tag = page.getContentTag(TAGNAME);

						return String.format(
								"<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"testtag\" data-gcn-i18n-constructname=\"construct\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s_ROOT_ONE: %s</div>",
								page.getId(),
								tag.getId(),
								tag.getId(),
								rootState,
								contents);
					}

					return String.format("%s_ROOT_ONE: %s", rootState, contents);
				}

				return String.format("MASTER_ROOT_ONE: %s", contents);

			case EMBEDDED:
				contents = localizationTarget == LocalizationTarget.EMBEDDED ? "LOCALIZED_EMBEDDED" : "MASTER_EMBEDDED";

				if ((localizationTarget == LocalizationTarget.ROOT_WITH_EMBEDDED || localizationTarget == LocalizationTarget.EMBEDDED) && editMode.equals("aloha")) {
					var tagName = page.getTags().keySet().stream()
							.filter(t -> !t.equals(TAGNAME) && !t.equals(OTHER_TAGNAME))
							.findFirst()
							.orElse("UNKNOWN_TAG");
					var tag = page.getContentTag(tagName);

					return String.format(
							"<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"%s\" data-gcn-i18n-constructname=\"construct\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s</div>",
							page.getId(),
							tag.getId(),
							tag.getName(),
							tag.getId(),
							contents);

				}

				return contents;

			default:
				return "UNEXPECTED_TAG";
		}
	}

	/**
	 * Create a page with {@link #TAGNAME} as content tag and an embedded tag.
	 * @param name The name of the page.
	 * @return The created page.
	 */
	private static Page createPage(String name) throws NodeException {
		return createPage(name, new ArrayList<>());
	}

	/**
	 * Create a page with {@link #TAGNAME} as content tag and an embedded tag.
	 *
	 * <p>
	 *     The name of the embedded tag is added to the list of embedded tag names.
	 * </p>
	 *
	 * @param name The name of the page.
	 * @param embeddedTagNames The list of embedded tag names.
	 * @return The created page.
	 */
	private static Page createPage(String name, List<String> embeddedTagNames) throws NodeException {
		var page = supply(() -> ContentNodeTestDataUtils.createPage(folder, template, name, node));

		return update(page, p -> {
			var embeddedTag = p.getContent().addContentTag(constructId);
			var embeddedTagTextPart = embeddedTag.getValues().getByKeyname(PARTNAME);

			embeddedTagNames.add(embeddedTag.getName());
			embeddedTagTextPart.setValueText("MASTER_EMBEDDED");

			var templateTagOne = p.getContentTag(TAGNAME);
			var templateTagOneTextPart = templateTagOne.getValues().getByKeyname(PARTNAME);

			templateTagOneTextPart.setValueText("MASTER_ROOT_ONE: <node " + embeddedTag.getName() + ">");

			var templateTagTwo = p.getContentTag(OTHER_TAGNAME);
			var templateTagTwoTextPart = templateTagTwo.getValues().getByKeyname(PARTNAME);

			templateTagTwoTextPart.setValueText("MASTER_ROOT_TWO");

			p.save();
		});
	}
	/**
	 * Localize the given page with the given localization type.
	 *
	 * @see #loadPage(int)
	 * @param page The page to localize.
	 * @param localizationType The desired localization type.
	 * @param expectedLocalizationResult The actually expected localization type of the result.
	 * @return The REST model of the localized page.
	 */
	private static com.gentics.contentnode.rest.model.Page localizePage(Page page, LocalizationType localizationType, LocalizationType expectedLocalizationResult) throws NodeException {
		var localizeRequest = new PageLocalizeRequest();

		localizeRequest
				.setLocalizationType(localizationType)
				.setChannelId(channel.getId());

		var localizeResponse = execute(m -> new MCPageResourceImpl().localize(m.getId(), localizeRequest), page);
		var expectedMessage = String.format("Die Seite wurde erfolgreich %slokalisiert.", expectedLocalizationResult == LocalizationType.FULL ? "" : "partiell ");

		assertResponse(localizeResponse, ResponseCode.OK, expectedMessage);

		var loadedPage = loadPage(page.getId());

		Assertions.assertThat(loadedPage.getLocalizationType())
				.as("Localization type")
				.isEqualTo(expectedLocalizationResult);

		return loadedPage;
	}

	/**
	 * Load the page with the given id from the channel.
	 * @param id The page ID.
	 * @return The REST model of the page.
	 */
	private static com.gentics.contentnode.rest.model.Page loadPage(int id) throws NodeException {
		return loadPage(id, channel);
	}

	/**
	 * Load the page with the given id from the given node.
	 * @param id The page ID.
	 * @return The REST model of the page.
	 */
	private static com.gentics.contentnode.rest.model.Page loadPage(int id, Node node) throws NodeException {
		var loadRequest = new MultiPageLoadRequest();

		loadRequest.setIds(Collections.singletonList(id));
		loadRequest.setNodeId(node.getId());

		var loadResponse = supply(() -> new PageResourceImpl().load(loadRequest, false));

		assertThat(loadResponse.getPages()).hasSize(1);

		return loadResponse.getPages().get(0);
	}
}
