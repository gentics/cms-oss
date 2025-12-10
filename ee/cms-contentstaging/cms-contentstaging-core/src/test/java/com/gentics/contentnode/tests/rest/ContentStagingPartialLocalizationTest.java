package com.gentics.contentnode.tests.rest;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.LocalizationType;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test partial localization status for importing from a content package.
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.MULTICHANNELLING, Feature.PARTIAL_MULTICHANNELLING })
public class ContentStagingPartialLocalizationTest extends AbstractContentStagingTest {
	private final static String TAGNAME = "testtag";
	private final static String OTHER_TAGNAME = "othertesttag";
	private final static String PARTNAME = "text";

	private static Node node;
	private static Node channel;
	private static Folder folder;
	private static Template template;
	private static int constructId;

	@BeforeClass
	public static void setupOnce() throws Exception {
		var t = TransactionManager.getCurrentTransaction();
		var prefs = t.getNodeConfig().getDefaultPreferences();

		prefs.setFeature(Feature.CONTENT_STAGING.toString().toLowerCase(), true);

		dbFilesDir = Paths.get(ConfigurationValue.CONTENT_PACKAGES_PATH.get()).toFile();
		dbFilesDir.mkdirs();

		testContext.getContext().getTransaction().commit();
		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		node = supply(() -> ContentNodeTestDataUtils.createNode());
		channel = supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		constructId = supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "construct", PARTNAME));
		template = supply(() -> ContentNodeTestDataUtils.createTemplate(
				node.getFolder(),
				"<node " + TAGNAME + ">\n<node " + OTHER_TAGNAME + ">",
				"Template",
				ContentNodeTestDataUtils.createTemplateTag(constructId, OTHER_TAGNAME, true, false),
				ContentNodeTestDataUtils.createTemplateTag(constructId, TAGNAME, true, false)));
	}

	@Test
	public void testImport() throws Exception {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 1");
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage = loadPage(localizedPage.getId());
		var localizedPageGlobalId = localizedPage.getGlobalId();

		var packageName = "MyPackage";

		directCreate(packageName, DEFAULT_PACKAGE_DESCRIPTION);
		var csRes = new ContentStagingResourceImpl();

		supply(() -> csRes.add(packageName, "page", localizedPageGlobalId, true));

		page = supply(t -> t.getObject(Page.class, localizedPageGlobalId));

		consume(p -> p.delete(true), page);
		supply(() -> csRes.importIntoCms(packageName, 100000000, false));

		page = supply(t -> t.getObject(Page.class, localizedPageGlobalId));

		assertThat(page)
			.as("Re-imported page")
			.isNotNull();

		var localizedTag = page.getContentTag(TAGNAME);

		assertThat(localizedTag.isInherited())
			.as("Localized tag inherited")
			.isFalse();

		var inheritedTag = page.getContentTag(OTHER_TAGNAME);

		assertThat(inheritedTag.isInherited())
			.as("Inherited tag inherited")
			.isTrue();
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
			var contentTag = p.getContent().addContentTag(constructId);
			var templateTag = p.getContentTag(TAGNAME);
			var textPart = templateTag.getValues().getByKeyname(PARTNAME);

			textPart.setValueText("<node " + contentTag.getName() + ">");
			embeddedTagNames.add(contentTag.getName());

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
