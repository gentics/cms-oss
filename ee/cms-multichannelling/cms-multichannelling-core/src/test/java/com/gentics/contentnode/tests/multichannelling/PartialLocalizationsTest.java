package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.LocalizationType;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

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
	private static Node subChannel;
	private static Folder folder;
	private static Template template;
	private static int constructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> ContentNodeTestDataUtils.createNode());
		channel = supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		subChannel = supply(() -> ContentNodeTestDataUtils.createChannel(channel, "SubChannel", "subchannel", "/"));
		folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		constructId = supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "construct", PARTNAME));
		template = supply(() -> ContentNodeTestDataUtils.createTemplate(
			node.getFolder(),
			"<node " + TAGNAME + ">\n<node " + OTHER_TAGNAME + ">",
			"Template",
			ContentNodeTestDataUtils.createTemplateTag(constructId, OTHER_TAGNAME, true, false),
			ContentNodeTestDataUtils.createTemplateTag(constructId, TAGNAME, true, false)));
	}

	@After
	public void cleanup() throws NodeException {
		operate(() -> {
			DBUtils.update("DELETE FROM publishqueue");
			DBUtils.update("DELETE FROM dirtqueue");
		});
	}

	/**
	 * Test that the partial localization flag is ignored when the feature is not active for the node.
	 */
	@Test
	public void testFeatureDisabledIgnorePartial() throws NodeException {
		node = update(node, n -> n.deactivateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = supply(() -> ContentNodeTestDataUtils.createPage(folder, template, "Page 1", node));

		localizePage(page, LocalizationType.PARTIAL, LocalizationType.FULL, -1);
	}

	/**
	 * Verify that page is fully localized when feature is inactive and tags are not inherited.
	 */
	@Test
	public void testFeatureDisabledTags() throws NodeException {
		node = update(node, n -> n.deactivateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 2", -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.FULL, -1);

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

		var page = createPage("Page 3", -1);
		var masterTagNames = supply(() -> page.getContent().getContentTags().keySet());
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);

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
			.containsOnlyElementsOf(masterTagNames);
	}

	@Test
	public void testLocalizeTagInInheritedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 4", -1);
		var localizeResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), page);
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", TAGNAME, page.getId());

		assertResponse(localizeResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagInFullyLocalizedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 5", -1);

		localizePage(page, LocalizationType.FULL, LocalizationType.FULL, -1);

		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), page);

		// The response must state that the whole page was localized, not partially localized since the feature is not active.
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", TAGNAME, page.getId());

		assertResponse(localizeTagResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagInPartiallyLocalizedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 6", -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");
	}

	@Test
	public void testLocalizeTagInPartiallyLocalizedPageTwice() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var page = createPage("Page 6", -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
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
		var page = createPage("Page 7", embeddedTagNames, -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), embeddedTagNames.get(0)), localizedPage);
		var expectedMessage = String.format("Error while localizing tag {%s} for page {%d}: tag is not a root tag", embeddedTagNames.get(0), localizedPage.getId());

		assertResponse(localizeTagResponse, ResponseCode.FAILURE, expectedMessage);
	}

	@Test
	public void testLocalizeTagWithEmbeddedTag() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 8", embeddedTagNames, -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
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
		var page = createPage("Page 9", embeddedTagNames, -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);

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
		var page = createPage("Page 10", embeddedTagNames, -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		var reloadedLocalizedPage = loadPage(localizedPage.getId());

		reloadedLocalizedPage.getTags().remove(TAGNAME);
		reloadedLocalizedPage.getTags().remove(OTHER_TAGNAME);

		var saveRequest = new PageSaveRequest();

		saveRequest.setPage(reloadedLocalizedPage);
		saveRequest.setDelete(Collections.singletonList(TAGNAME));

		var saveResponse = supply(() -> new PageResourceImpl().save(localizedPage.getId().toString(), saveRequest));

		assertResponse(saveResponse, ResponseCode.OK, "saved page with id: " + reloadedLocalizedPage.getId());

		reloadedLocalizedPage = loadPage(localizedPage.getId());

		assertThat(reloadedLocalizedPage.getTags().keySet())
			.as("Page tags")
			.containsExactlyInAnyOrder(TAGNAME, OTHER_TAGNAME, embeddedTagNames.get(0));
	}

	@Test
	public void testMoveEmbeddedTagAndDeleteRoot() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var embeddedTagNames = new ArrayList<String>();
		var page = createPage("Page 11", embeddedTagNames, -1);
		var localizedPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
		var localizeTagResponse = execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), localizedPage);
		execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), OTHER_TAGNAME), localizedPage);

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
				.containsOnly(OTHER_TAGNAME, TAGNAME, embeddedTagNames.get(0));
	}

	@Test
	public void testDependencyInheritedTag() throws Exception {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var dirtedPageIds = supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, channel));

		assertTrue("Publish queue must contain no dirted objects", dirtedPageIds.isEmpty());

		var page = createPage("Master page 12", 1000);
		var localizedRestPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, 2000);
		var localizedPage = supply(t -> t.getObject(Page.class, localizedRestPage.getId()));

		Builder.update(page, p -> {}).at(3000).publish().build();
		Builder.update(localizedPage, p -> {}).at(3000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
		}

		operate(() -> PublishQueue.undirtObjects(null, Page.TYPE_PAGE, null, -1, -1));

		Builder.update(page, p -> {
			p.getContentTag(TAGNAME).getValues().getByKeyname(PARTNAME).setValueText("new text");
		}).at(4000).publish().build();

		operate(() -> testContext.waitForDirtqueueWorker());

		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node)))
				.as("Dirted page IDs for node").containsOnly(page.getId());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, channel)))
				.as("Dirted page IDs for channel").containsOnly(localizedPage.getId());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, subChannel)))
				.as("Dirted page IDs for subchannel").containsOnly(localizedPage.getId());
	}

	@Test
	public void testDependencyLocalizedTag() throws Exception {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var dirtedPageIds = supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, channel));

		assertTrue("Publish queue must contain no dirted objects", dirtedPageIds.isEmpty());

		var page = createPage("Master page 13", 1000);
		var localizedRestPage = localizePage(page, LocalizationType.PARTIAL, LocalizationType.PARTIAL, 2000);
		var localizedPage = supply(t -> t.getObject(Page.class, localizedRestPage.getId()));
		var localizeTagResponse = execute(m -> {
			TransactionManager.getCurrentTransaction().setTimestamp(3000 * 1000L);
			return new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME);
		}, localizedRestPage);

		assertResponse(localizeTagResponse, ResponseCode.OK, "Der Tag wurde erfolgreich lokalisiert.");

		Builder.update(page, p -> {}).at(4000).publish().build();
		Builder.update(localizedPage, p -> {}).at(4000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
		}

		operate(() -> PublishQueue.undirtObjects(null, Page.TYPE_PAGE, null, -1, -1));

		Builder.update(localizedPage, p -> {
			p.getContentTag(TAGNAME).getValues().getByKeyname(PARTNAME).setValueText("new text");
		}).at(5000).publish().build();

		operate(() -> testContext.waitForDirtqueueWorker());

		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node)))
				.as("Dirted page IDs for node").isEmpty();
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, channel)))
				.as("Dirted page IDs for channel").containsOnly(localizedPage.getId());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, subChannel)))
				.as("Dirted page IDs for subchannel").containsOnly(localizedPage.getId());
	}

	@Test
	public void testLocalizePublishedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		// create a page and publish it
		var masterPage = create(Page.class, create -> {
			create.setFolder(node, folder);
			create.setTemplateId(template.getId());
			create.setName("Master page");
		}).at(1000).publish().build();

		// localize partially
		var localizedRestPage = localizePage(masterPage, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);

		// we assert that the page does not contain any non-inherited content tags
		assertThat(localizedRestPage.getTags().values().stream().filter(t -> t.getType() == Type.CONTENTTAG).filter(t -> Objects.equals(t.getInherited(), false))).isEmpty();
	}

	@Test
	public void testLocalizePublishedAndModifiedPage() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		// create a page and publish it
		var masterPage = create(Page.class, create -> {
			create.setFolder(node, folder);
			create.setTemplateId(template.getId());
			create.setName("Master page");
		}).at(1000).publish().build();

		// modify the page (but do not publish)
		masterPage = Builder.update(masterPage, update -> {
			getPartType(ShortTextPartType.class, update.getContentTag(TAGNAME), PARTNAME).setText("modification");
		}).at(2000).doNotPublish().build();

		// localize partially
		var localizedRestPage = localizePage(masterPage, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);

		// we assert that the page does not contain any non-inherited content tags
		assertThat(localizedRestPage.getTags().values().stream().filter(t -> t.getType() == Type.CONTENTTAG).filter(t -> Objects.equals(t.getInherited(), false))).isEmpty();
	}

	@Test
	public void testPushFullyLocalizedPageOverPartial() throws NodeException {
		node = update(node, n -> n.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		var masterPage = create(Page.class, create -> {
			create.setFolder(node, folder);
			create.setTemplateId(template.getId());
			create.setName("Master Page");

			getPartType(ShortTextPartType.class, create.getContentTag(TAGNAME), PARTNAME).setText("Master content in %s".formatted(TAGNAME));
			getPartType(ShortTextPartType.class, create.getContentTag(OTHER_TAGNAME), PARTNAME).setText("Master content in %s".formatted(OTHER_TAGNAME));
		}).build();

		var channelRestPage = localizePage(masterPage, channel, LocalizationType.PARTIAL, LocalizationType.PARTIAL, -1);
		execute(m -> new MCPageResourceImpl().localizeTag(m.getId(), TAGNAME), channelRestPage);
		var channelPage = execute(id -> TransactionManager.getCurrentTransaction().getObject(Page.class, id, false, false, true),
				channelRestPage.getId());

		channelPage = Builder.update(channelPage, update -> {
			getPartType(ShortTextPartType.class, update.getContentTag(TAGNAME), PARTNAME).setText("Channel content in %s".formatted(TAGNAME));
		}).build();

		var subChannelRestPage = localizePage(masterPage, subChannel, LocalizationType.FULL, LocalizationType.FULL, -1);
		var subChannelPage = execute(id -> TransactionManager.getCurrentTransaction().getObject(Page.class, id, false, false, true),
				subChannelRestPage.getId());
		subChannelPage = Builder.update(subChannelPage, update -> {
			getPartType(ShortTextPartType.class, update.getContentTag(TAGNAME), PARTNAME).setText("Subchannel content in %s".formatted(TAGNAME));
			getPartType(ShortTextPartType.class, update.getContentTag(OTHER_TAGNAME), PARTNAME).setText("Subchannel content in %s".formatted(OTHER_TAGNAME));
		}).build();

		var response = execute(id -> {
			PushToMasterRequest req = new PushToMasterRequest();
			req.setChannelId(subChannel.getId());
			req.setMasterId(channel.getId());
			req.setForegroundTime(0);
			req.setTypes(Arrays.asList(PushToMasterRequest.Type.page));

			return new MCPageResourceImpl().pushToMaster(id, req);
		}, subChannelPage.getId());

		assertResponse(response, ResponseCode.OK);

		channelPage = execute(Page::reload, channelPage);
		assertThat(channelPage).as("Channel Page").isNotNull();
		String content = execute(p -> getPartType(ShortTextPartType.class, p.getContentTag(TAGNAME), PARTNAME).getText(), channelPage);
		assertThat(content).as("Tag %s in Channel".formatted(TAGNAME)).isEqualTo("Subchannel content in %s".formatted(TAGNAME));
		content = execute(p -> getPartType(ShortTextPartType.class, p.getContentTag(OTHER_TAGNAME), PARTNAME).getText(), channelPage);
		assertThat(content).as("Tag %s in Channel".formatted(OTHER_TAGNAME)).isEqualTo("Master content in %s".formatted(OTHER_TAGNAME));

		subChannelPage = execute(Page::reload, subChannelPage);
		assertThat(subChannelPage).as("SubChannel Page").isNull();
	}

	/**
	 * Create a page with {@link #TAGNAME} as content tag and an embedded tag.
	 * @param name The name of the page.
	 * @param timestamp timestamp for creation
	 * @return The created page.
	 */
	private static Page createPage(String name, int timestamp) throws NodeException {
		return createPage(name, new ArrayList<>(), timestamp);
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
	 * @param timestamp timestamp for creation
	 * @return The created page.
	 */
	private static Page createPage(String name, List<String> embeddedTagNames, int timestamp) throws NodeException {
		return Builder.create(Page.class, create -> {
			create.setFolder(node, folder);
			create.setTemplateId(template.getId());
			create.setName(name);

			var contentTag = create.getContent().addContentTag(constructId);

			getPartType(ShortTextPartType.class, create.getContent().getContentTag(TAGNAME), PARTNAME)
					.setText("<node %s>".formatted(contentTag.getName()));

			embeddedTagNames.add(contentTag.getName());
		}).at(timestamp).build();
	}

	/**
	 * Localize the given page with the given localization type.
	 *
	 * @see #loadPage(int) 
	 * @param page The page to localize.
	 * @param localizationType The desired localization type.
	 * @param expectedLocalizationResult The actually expected localization type of the result.
	 * @param timestamp timestamp
	 * @return The REST model of the localized page.
	 */
	private static com.gentics.contentnode.rest.model.Page localizePage(Page page, LocalizationType localizationType, LocalizationType expectedLocalizationResult, int timestamp) throws NodeException {
		return localizePage(page, channel, localizationType, expectedLocalizationResult, timestamp);
	}

	/**
	 * Localize the given page with the given localization type.
	 *
	 * @see #loadPage(int) 
	 * @param page The page to localize.
	 * @param c channel where to localize
	 * @param localizationType The desired localization type.
	 * @param expectedLocalizationResult The actually expected localization type of the result.
	 * @param timestamp timestamp
	 * @return The REST model of the localized page.
	 */
	private static com.gentics.contentnode.rest.model.Page localizePage(Page page, Node c, LocalizationType localizationType, LocalizationType expectedLocalizationResult, int timestamp) throws NodeException {
		var localizeRequest = new PageLocalizeRequest();

		localizeRequest
			.setLocalizationType(localizationType)
			.setChannelId(c.getId());
		localizeRequest.setForegroundTime(0);

		var localizeResponse = execute(m -> {
			if (timestamp > 0) {
				TransactionManager.getCurrentTransaction().setTimestamp(timestamp * 1000L);
			}
			return new MCPageResourceImpl().localize(m.getId(), localizeRequest);
		}, page);
		var expectedMessage = String.format("Die Seite wurde erfolgreich %slokalisiert.", expectedLocalizationResult == LocalizationType.FULL ? "" : "partiell ");

		assertResponse(localizeResponse, ResponseCode.OK, expectedMessage);

		var loadedPage = loadPage(page.getId(), c);

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
