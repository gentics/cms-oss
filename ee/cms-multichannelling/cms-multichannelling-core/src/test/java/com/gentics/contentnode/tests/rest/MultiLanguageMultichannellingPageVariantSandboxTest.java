package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test creating page variants in context of multilanguage and multichannelling
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
public class MultiLanguageMultichannellingPageVariantSandboxTest {

	private final Channel createObject;
	private final Channel localizeIn;
	private final boolean localizeBoth;
	private final Channel variantSource;
	private final Channel variantDestination;
	private final boolean sameFolder;
	private final boolean localFolder;
	private final boolean sourceIsLocalized;

	private static int testCounter = 0;
	private static ContentLanguage klingon;
	private static ContentLanguage tengwar;
	private static ChannelStructure channels;

	public MultiLanguageMultichannellingPageVariantSandboxTest(Channel createObject, Channel localizeIn, boolean localizeBoth, Channel variantSource, Channel variantDestination, boolean sameFolder, boolean localFolder) {
		this.createObject = createObject;
		this.localizeIn = localizeIn;
		this.localizeBoth = localizeBoth;
		this.variantSource = variantSource;
		this.variantDestination = variantDestination;
		this.sameFolder = sameFolder;
		this.localFolder = localFolder;
		this.sourceIsLocalized = localizeIn != Channel.NONE && (localizeIn == variantSource
				|| variantSource.channelOf(localizeIn));

		testCounter++;
	}

	/**
	 * return a name that is unique to the test method call
	 * @return the name
	 */
	private String getName() {
		return "unq" + testCounter;
	}
	/**
	 * Create parameters for test class.
	 * @return list of parameters
	 */
	@Parameters(name = "{index}: createObject: {0}, localizeIn: {1}, localizeBoth {2}, variantSource: {3}, variantDestination: {4}, sameFolder: {5}, localFolder: {6}")
	public static Collection<Object[]> parameters() {
		List<Object[]> result = new ArrayList<>();
		boolean[] bools = { true, false };
		for (Channel createObject : Channel.mainTree) {
			for (Channel localizeIn : Channel.values()) {
				for (boolean localizeBoth : bools) {
					for (Channel variantSource : Channel.mainTree) {
						for (Channel variantDestination : Channel.values()) {
							for (boolean sameFolder : bools) {
								for (boolean localFolder : bools) {
									if (isValidParameters(createObject, localizeIn, localizeBoth, variantSource, variantDestination, sameFolder, localFolder)) {
										result.add(new Object[] { createObject, localizeIn, localizeBoth, variantSource, variantDestination, sameFolder, localFolder });
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	private static boolean isValidParameters(Channel createObject, Channel localizeIn, boolean localizeBoth, Channel variantSource, Channel variantDestination, boolean sameFolder, boolean localFolder) {
		if (variantDestination.isOtherTree() && sameFolder) {
			return false;
		}
		if (variantSource == Channel.NONE || variantDestination == Channel.NONE) {
			return false;
		}
		if (localizeIn != Channel.NONE && !localizeIn.channelOf(createObject)) {
			return false;
		}
		if (localizeIn == Channel.NONE && localizeBoth) {
			return false;
		}
		if (variantSource != createObject && !variantSource.channelOf(createObject)) {
			return false;
		}
		if (sameFolder && localFolder) {
			return false;
		}
		if (variantDestination.isMaster() && localFolder) {
			return false;
		}
		return true;
	}

	@ClassRule
	public static DBTestContext context = new DBTestContext();
	private static Folder sourceFolder;
	private static Folder mainRoot;
	private static Folder otherRoot;
	private static Map<Channel, Folder> localFolders = new HashMap<>();
	private static Template template;

	@BeforeClass
	public static void setupClass() throws Exception {
		Trx.operate(() -> {
			context.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
			klingon = Creator.createLanguage("Klingon", "tlh");
			tengwar = Creator.createLanguage("Tengwar", "teng");
			channels = new ChannelStructure(klingon, tengwar);
			sourceFolder = Creator.createFolder(channels.getNode(Channel.MASTER).getFolder(), "source", "/src");
			mainRoot = Creator.createFolder(channels.getNode(Channel.MASTER).getFolder(), "dst", "/dst");
			otherRoot = Creator.createFolder(channels.getNode(Channel.OTHERMASTER).getFolder(), "dst", "/dst");
			for (Channel c : Channel.mainTree) {
				if (!c.isMaster())  {
					localFolders.put(c, Creator.createFolder(mainRoot, "local" + channels.getNode(c).getId(), "/dst", channels.getNode(c)));
				}
			}
			for (Channel c : Channel.otherTree) {
				if (!c.isMaster())  {
					localFolders.put(c, Creator.createFolder(otherRoot, "local" + channels.getNode(c).getId(), "/dst", channels.getNode(c)));
				}
			}
			template = Creator.createTemplate("asdf", "xxx", mainRoot);
		});
	}

	@Test
	public void testCreateVariant() throws NodeException {

		// Set up original page + translation
		Page originalPage = Trx.supply(() -> Creator.createPage(getName() + "x", sourceFolder, template, tengwar, channels.getNode(createObject)));

		PageLoadResponse translateResult = Trx.supply(() -> {
			PageLoadResponse res = getPageResource().translate(originalPage.getId(), klingon.getCode(), false, channels.getNode(createObject).getId());
			ContentNodeRESTUtils.assertResponseOK(res);
			return res;
		});
		Page translatedPage = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(Page.class, translateResult.getPage().getId()));
		Page localizedPage = null;
		Page localizedTranslatedPage = null;

		// Localize
		if (localizeIn != Channel.NONE) {
			localizedPage = Trx.supply(() -> {
				Page loc = (Page) originalPage.copy();
				loc.setChannelInfo(channels.getNode(localizeIn).getId(), originalPage.getChannelSetId());
				loc.save();
				return loc;
			});

			if (localizeBoth) {
				localizedTranslatedPage = Trx.supply(() -> {
					Page loc = (Page) translatedPage.copy();
					loc.setChannelInfo(channels.getNode(localizeIn).getId(), translatedPage.getChannelSetId());
					loc.save();
					return loc;
				});
			}
		}

		// Create variant
		PageLoadResponse variantResult = Trx.supply(() -> {
			PageCreateRequest pcr = new PageCreateRequest();
			pcr.setVariantChannelId(channels.getNode(variantSource).getId());
			pcr.setNodeId(channels.getNode(variantDestination).getId());
			pcr.setVariantId(originalPage.getId());
			if (sameFolder) {
				pcr.setFolderId(String.valueOf(sourceFolder.getId()));
			} else {
				if (localFolder) {
					pcr.setFolderId(localFolders.get(variantDestination).getId().toString());
				} else {
					pcr.setFolderId((variantDestination.isMainTree() ? mainRoot : otherRoot).getId().toString());
				}
			}
			PageLoadResponse res = getPageResource().create(pcr);

			// Check results
			ContentNodeRESTUtils.assertResponseOK(res);
			return res;
		});

		Page locPage = localizedPage;
		Page locTransPage = localizedTranslatedPage;
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page resultPage;
			Page klingonVariant;
			Page tengwarVariant;
			List<Page> variants;
			try (ChannelTrx ct = new ChannelTrx(channels.getNode(variantDestination).getId())) {
				resultPage = t.getObject(Page.class, variantResult.getPage().getId());
				variants = resultPage.getLanguageVariants(false);
				klingonVariant = resultPage.getLanguageVariant(klingon.getCode());
				tengwarVariant = resultPage.getLanguageVariant(tengwar.getCode());
			}
			assertEquals("Must have the right number of translations", 2, variants.size());
			if (sourceIsLocalized) {
				assertEquals("Variant must use the content of the localized page", tengwarVariant.getContent().getId(), locPage.getContent().getId());
				if (!localizeBoth) {
					assertEquals("Variant tranlation must use the content of the translated page", klingonVariant.getContent().getId(), translatedPage.getContent().getId());
				} else {
					assertEquals("Variant tranlation must use the content of the localized translated page", klingonVariant.getContent().getId(), locTransPage.getContent().getId());
				}
			} else {
				assertEquals("Variant tranlation must use the content of the original page", tengwarVariant.getContent().getId(), originalPage.getContent().getId());
				assertEquals("Variant tranlation must use the content of the translated page", klingonVariant.getContent().getId(), translatedPage.getContent().getId());
			}
		});
	}
}