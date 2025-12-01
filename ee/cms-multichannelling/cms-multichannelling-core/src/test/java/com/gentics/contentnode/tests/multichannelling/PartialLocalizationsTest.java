package com.gentics.contentnode.tests.multichannelling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.LocalizationType;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for partial localization of pages.
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PARTIAL_MULTICHANNELLING })
public class PartialLocalizationsTest {
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

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
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

	/**
	 * Test that the partial localization flag is ignored when the feature is not active for the node.
	 */
	@Test
	public void testFeatureDisabledIgnorePartial() throws NodeException {
		node = update(node, n -> n.deactivateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = supply(() -> ContentNodeTestDataUtils.createPage(folder, template, "Page 1", node));

		localizePage(page, LocalizationType.PARTIAL, LocalizationType.FULL);
	}

	/**
	 * Verify that page is fully localized when feature is inactive and tags are not inherited.
	 */
	@Test
	public void testFeatureDisabledTags() throws NodeException {
		node = update(node, n -> n.deactivateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 2");
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.FULL);

		assertThat(localizedPage.getTags())
			.as("Number of tags")
			.hasSize(3);

		for (var tag: localizedPage.getTags().values()) {
			assertThat(tag.getInherited())
				.as("Tag " + tag.getName() + " not inherited")
				.isFalse();
		}
	}

	/**
	 * Verify that page is partially localized when feature is active and tags are inherited.
	 */
	@Test
	public void testFeatureEnabled() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 3");
		var masterTagNames = supply(() -> page.getContent().getContentTags().keySet());
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);

		assertThat(localizedPage.getTags())
			.as("Number of tags")
			.hasSize(3);

		for (var tag: localizedPage.getTags().values()) {
			assertThat(tag.getInherited())
				.as("Tag " + tag.getName() + " inherited")
				.isTrue();
		}

		var localizedTagNames = localizedPage.getTags().keySet();

		assertThat(localizedTagNames)
			.as("Localized tag names")
			.containsExactlyElementsOf(masterTagNames);
	}

	@Test
	public void testLocalizeTagInInheritedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 4");
		var localizeResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), page);
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", TAGNAME, page.getId());

		assertResponse(localizeResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagInFullyLocalizedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 5");

		localizePage(page, LocalizationType.FULL, LocalizationType.FULL);

		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), page);

		// The response must state that the whole page was localized, not partially localized since the feature is not active.
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", TAGNAME, page.getId());

		assertResponse(localizeTagResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagInPartiallyLocalizedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 6");
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");
	}

	@Test
	public void testLocalizeTagInPartiallyLocalizedPageTwice() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 6");
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage1 = loadPage(localizedPage.getId());

		localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Tag already localized");

		var reloadedLocalizedPage2 = loadPage(localizedPage.getId());

		assertThat(reloadedLocalizedPage2.getTags().keySet())
			.as("Localized tag names")
			.containsExactlyElementsOf(reloadedLocalizedPage1.getTags().keySet());

		var localizedRootTagValue1 = reloadedLocalizedPage1.getTags().get(TAGNAME).getProperties().get("text").getStringValue();
		var localizedRootTagValue2 = reloadedLocalizedPage2.getTags().get(TAGNAME).getProperties().get("text").getStringValue();

		assertThat(localizedRootTagValue2)
			.as("Localized root tag value after second localizeTag call")
			.isEqualTo(localizedRootTagValue1);
	}

	@Test
	public void testLocalizeEmbeddedTag() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 7", embeddedTagNames);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), embeddedTagNames.get(0)), localizedPage);
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: tag is not a root tag", embeddedTagNames.get(0), localizedPage.getId());

		assertResponse(localizeTagResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagWithEmbeddedTag() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 8", embeddedTagNames);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage = loadPage(localizedPage.getId());
		var localizedEmbeddedTagNames = reloadedLocalizedPage.getTags().keySet().stream()
			.filter(t -> !t.equals(TAGNAME) && !t.equals(OTHER_TAGNAME))
			.collect(Collectors.toList());
		var numLocalizedEmbeddedTags = localizedEmbeddedTagNames.size();

		localizedEmbeddedTagNames.removeAll(embeddedTagNames);

		// Removing the original embedded tag names must not change the number of embedded tags in the localized page.
		assertThat(localizedEmbeddedTagNames)
			.as("Localized embedded tag names")
			.hasSize(numLocalizedEmbeddedTags);

		var inheritedRootTagValue = localizedPage.getTags().get(TAGNAME).getProperties().get("text").getStringValue();
		var localizedRootTagValue = reloadedLocalizedPage.getTags().get(TAGNAME).getProperties().get("text").getStringValue();

		assertThat(inheritedRootTagValue)
			.as("Inherited root tag value")
			.isEqualTo(String.format("<node %s>", embeddedTagNames.get(0)));
		assertThat(localizedRootTagValue)
			.as("Localized root tag value")
			.isEqualTo(String.format("<node %s>", localizedEmbeddedTagNames.get(0)));
	}

	@Test
	public void testDeleteInheritedTag() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 9", embeddedTagNames);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);

		localizedPage.getTags().remove(TAGNAME);

		var saveRequest = new PageSaveRequest();

		saveRequest.setPage(localizedPage);
		saveRequest.setDelete(Collections.singletonList(TAGNAME));

		var saveResponse = supply(() -> new PageResourceImpl().save(localizedPage.getId().toString(), saveRequest));

		assertResponse(saveResponse, ResponseCode.PERMISSION);
		assertThat(saveResponse.getResponseInfo().getResponseMessage())
			.as("Response message")
			.endsWith("is readonly and cannot be modified");
	}

	@Test
	public void testDeleteLocalizedRootTag() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 10", embeddedTagNames);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage = loadPage(localizedPage.getId());

		reloadedLocalizedPage.getTags().remove(TAGNAME);

		var saveRequest = new PageSaveRequest();

		saveRequest.setPage(reloadedLocalizedPage);
		saveRequest.setDelete(Collections.singletonList(TAGNAME));

		var saveResponse = supply(() -> new PageResourceImpl().save(localizedPage.getId().toString(), saveRequest));

		assertResponse(saveResponse, ResponseCode.OK, "saved page with id: " + reloadedLocalizedPage.getId());

		reloadedLocalizedPage = loadPage(localizedPage.getId());

		assertThat(reloadedLocalizedPage.getTags().keySet())
			.as("Page tags")
			.containsExactly(TAGNAME, OTHER_TAGNAME, embeddedTagNames.get(0));
	}

	@Test
	public void testMoveEmbeddedTagAndDeleteRoot() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 11", embeddedTagNames);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage = loadPage(localizedPage.getId());
		var tagToRemove = reloadedLocalizedPage.getTags().remove(TAGNAME);
		var tagValue = tagToRemove.getProperties().get("text").getStringValue();

		reloadedLocalizedPage.getTags().get(OTHER_TAGNAME).getProperties().get("text").setStringValue(tagValue);

		var saveRequest = new PageSaveRequest();

		saveRequest.setPage(reloadedLocalizedPage);
		saveRequest.setDelete(Collections.singletonList(TAGNAME));

		var saveResponse = supply(() -> new PageResourceImpl().save(localizedPage.getId().toString(), saveRequest));

		assertResponse(saveResponse, ResponseCode.OK, "saved page with id: " + reloadedLocalizedPage.getId());

		reloadedLocalizedPage = loadPage(localizedPage.getId());

		assertThat(reloadedLocalizedPage.getTags().keySet())
				.as("Page tags")
				.containsExactly(TAGNAME, embeddedTagNames.get(0));
	}

	@Test
	public void testDependencyInheritedTag() throws Exception {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var dirtedPageIds = supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, channel));

		assertTrue("Publish queue must contain no dirted objects", dirtedPageIds.isEmpty());

		var page = createPage("Master page 12");
		var localizedRestPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL);
		var localizedPage = supply(t -> t.getObject(Page.class, localizedRestPage.getId()));
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedRestPage);

		operate(t -> {
			t.getObject(page, true).publish();
			t.getObject(localizedPage, true).publish();
		});

		try (Trx trx = new Trx()) {
			testContext.publish(false);
		}

		operate(() -> PublishQueue.undirtObjects(null, Page.TYPE_PAGE, null, -1, -1));

		update(page, p -> {
			p.getContentTag(TAGNAME).getValues().getByKeyname(PARTNAME).setValueText("new text");
			p.save();
		});

		operate(() -> testContext.waitForDirtqueueWorker());

		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node)))
			.as("Dirted page IDs")
			.containsExactly(page.getId(), localizedPage.getId());
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
			var templateTag = p.getContent().getContentTag(TAGNAME);
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
