package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Multichannelling test for staging of pages
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class PageMultichannellingStagingTest extends AbstractMultichannellingContentStagingTest<Page> {
	/**
	 * Create test instance
	 */
	public PageMultichannellingStagingTest() {
		super(Page.class, "page");
	}

	/**
	 * Test importing language variants of a page in the master node
	 * @throws NodeException
	 */
	@Test
	public void testPageWithLanguagesInMaster() throws NodeException {
		// create german page in master
		Page germanPage = createMasterObject(supply(() -> node.getFolder()));
		String germanPageGlobalId = execute(o -> o.getGlobalId().toString(), germanPage);

		// create english translation
		Page englishPage = createTranslation(germanPage);
		String englishPageGlobalId = execute(o -> o.getGlobalId().toString(), englishPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishPageGlobalId, false));

		// delete objects in CMS
		consume(f -> f.delete(true), germanPage);
		germanPage = supply(t -> t.getObject(clazz, germanPageGlobalId));
		assertThat(germanPage).as("Deleted object").isNull();

		consume(f -> f.delete(true), englishPage);
		englishPage = supply(t -> t.getObject(clazz, englishPageGlobalId));
		assertThat(englishPage).as("Deleted object").isNull();

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanPage = supply(t -> t.getObject(clazz, germanPageGlobalId));
		englishPage = supply(t -> t.getObject(clazz, englishPageGlobalId));
		consume(this::assertMasterObject, germanPage, execute(Node::getFolder, node), "de");
		consume(this::assertMasterObject, englishPage, execute(Node::getFolder, node), "en");
		consume(this::assertLanguageVariants, germanPage, englishPage);

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, germanPage, englishPage);
		expectAllDirted(sideChannel, germanPage, englishPage);
		expectAllDirted(channel, germanPage, englishPage);
		expectAllDirted(subChannel, germanPage, englishPage);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing language variants of a page in the channel
	 * @throws NodeException
	 */
	@Test
	public void testPageWithLanguagesInChannel() throws NodeException {
		// create german page in channel
		Page germanPage = createLocalObject(supply(() -> node.getFolder()));
		String germanPageGlobalId = execute(o -> o.getGlobalId().toString(), germanPage);

		// create english translation
		Page englishPage = createTranslation(germanPage);
		String englishPageGlobalId = execute(o -> o.getGlobalId().toString(), englishPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishPageGlobalId, false));

		// delete objects in CMS
		consume(f -> f.delete(true), germanPage);
		germanPage = supply(t -> t.getObject(clazz, germanPageGlobalId));
		assertThat(germanPage).as("Deleted object").isNull();

		consume(f -> f.delete(true), englishPage);
		englishPage = supply(t -> t.getObject(clazz, englishPageGlobalId));
		assertThat(englishPage).as("Deleted object").isNull();

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanPage = supply(t -> t.getObject(clazz, germanPageGlobalId));
		englishPage = supply(t -> t.getObject(clazz, englishPageGlobalId));
		try (Trx trx = new Trx()) {
			assertLocalObject(germanPage, channel, node.getFolder(), "de");
			assertLocalObject(englishPage, channel, node.getFolder(), "en");
			trx.success();
		}
		consume(this::assertLanguageVariants, germanPage, englishPage);

		testContext.waitForDirtqueueWorker();

		expectAllDirted(channel, germanPage, englishPage);
		expectAllDirted(subChannel, germanPage, englishPage);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing language variants with localized copies
	 * @throws NodeException
	 */
	@Test
	public void testPageWithLanguagesInMasterAndChannel() throws NodeException {
		// create german page in master
		Page germanMasterPage = createMasterObject(supply(() -> node.getFolder()));
		String germanMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), germanMasterPage);

		// localize
		Page germanLocalizedPage = localizeObject(germanMasterPage);
		String germanLocalizedPageGlobalId = execute(o -> o.getGlobalId().toString(), germanLocalizedPage);

		// create english master translation
		Page englishMasterPage = createTranslation(germanMasterPage);
		String englishMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), englishMasterPage);

		// translate localized
		Page englishLocalizedPage = createTranslation(germanLocalizedPage);
		String englishLocalizedPageGlobalId = execute(o -> o.getGlobalId().toString(), englishLocalizedPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanMasterPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishMasterPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanLocalizedPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishLocalizedPageGlobalId, false));

		// delete objects in CMS
		germanLocalizedPage = delete(germanLocalizedPage);
		englishLocalizedPage = delete(englishLocalizedPage);
		germanMasterPage = delete(germanMasterPage);
		englishMasterPage = delete(englishMasterPage);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanMasterPage = supply(t -> t.getObject(clazz, germanMasterPageGlobalId));
		germanLocalizedPage = supply(t -> t.getObject(clazz, germanLocalizedPageGlobalId));
		englishMasterPage = supply(t -> t.getObject(clazz, englishMasterPageGlobalId));
		englishLocalizedPage = supply(t -> t.getObject(clazz, englishLocalizedPageGlobalId));
		try (Trx trx = new Trx()) {
			assertLocalizedObject(germanMasterPage, germanLocalizedPage, node.getFolder(), "de");
			assertLocalizedObject(englishMasterPage, englishLocalizedPage, node.getFolder(), "en");
			assertLanguageVariants(germanMasterPage, englishMasterPage);
			assertLanguageVariants(germanLocalizedPage, englishLocalizedPage);
			trx.success();
		}

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, germanMasterPage, englishMasterPage);
		expectAllDirted(sideChannel, germanMasterPage, englishMasterPage);
		expectAllDirted(channel, germanLocalizedPage, englishLocalizedPage);
		expectAllDirted(subChannel, germanLocalizedPage, englishLocalizedPage);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing language variants where a conflicting language variant exists in the master
	 * @throws NodeException
	 */
	@Test
	public void testConflictingLanguageInMaster() throws NodeException {
		// create german page in master
		Page germanMasterPage = createMasterObject(supply(() -> node.getFolder()));
		String germanMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), germanMasterPage);

		// translate
		Page englishMasterPage = createTranslation(germanMasterPage);
		String englishMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), englishMasterPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanMasterPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishMasterPageGlobalId, false));

		// delete english translation
		englishMasterPage = delete(englishMasterPage);

		// translate again (which creates a conflicting page language)
		Page conflictingEnglishMasterPage = createTranslation(germanMasterPage);
		String conflictingEnglishMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), conflictingEnglishMasterPage);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanMasterPage = supply(t -> t.getObject(clazz, germanMasterPageGlobalId));
		englishMasterPage = supply(t -> t.getObject(clazz, englishMasterPageGlobalId));
		conflictingEnglishMasterPage = supply(t -> t.getObject(clazz, conflictingEnglishMasterPageGlobalId));
		try (Trx trx = new Trx()) {
			assertMasterObject(germanMasterPage, node.getFolder(), "de");
			assertMasterObject(englishMasterPage, node.getFolder(), "en");
			assertLanguageVariants(germanMasterPage, englishMasterPage);
			assertThat(conflictingEnglishMasterPage).as("Conflicting language Variant").isNull();
			trx.success();
		}

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, germanMasterPage, englishMasterPage);
		expectAllDirted(sideChannel, germanMasterPage, englishMasterPage);
		expectAllDirted(channel, germanMasterPage, englishMasterPage);
		expectAllDirted(subChannel, germanMasterPage, englishMasterPage);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing language variants where a conflicting language variant exists in the channel
	 * @throws NodeException
	 */
	@Test
	public void testConflictingLanguageInChannel() throws NodeException {
		// create german page in channel
		Page germanLocalPage = createLocalObject(supply(() -> node.getFolder()));
		String germanLocalPageGlobalId = execute(o -> o.getGlobalId().toString(), germanLocalPage);

		// translate
		Page englishLocalPage = createTranslation(germanLocalPage);
		String englishLocalPageGlobalId = execute(o -> o.getGlobalId().toString(), englishLocalPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanLocalPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishLocalPageGlobalId, false));

		// delete english translation
		englishLocalPage = delete(englishLocalPage);

		// translate again (which creates a conflicting page language)
		Page conflictingEnglishLocalPage = createTranslation(germanLocalPage);
		String conflictingEnglishLocalPageGlobalId = execute(o -> o.getGlobalId().toString(), conflictingEnglishLocalPage);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanLocalPage = supply(t -> t.getObject(clazz, germanLocalPageGlobalId));
		englishLocalPage = supply(t -> t.getObject(clazz, englishLocalPageGlobalId));
		conflictingEnglishLocalPage = supply(t -> t.getObject(clazz, conflictingEnglishLocalPageGlobalId));
		try (Trx trx = new Trx()) {
			assertLocalObject(germanLocalPage, channel, node.getFolder(), "de");
			assertLocalObject(englishLocalPage, channel, node.getFolder(), "en");
			assertLanguageVariants(germanLocalPage, englishLocalPage);
			assertThat(conflictingEnglishLocalPage).as("Conflicting language Variant").isNull();
			trx.success();
		}

		testContext.waitForDirtqueueWorker();

		expectAllDirted(channel, germanLocalPage, englishLocalPage);
		expectAllDirted(subChannel, germanLocalPage, englishLocalPage);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing language variants where a conflicting language variant exists in the master and the channel
	 * @throws NodeException
	 */
	@Test
	public void testConflictingLanguageInMasterAndChannel() throws NodeException {
		// create german page in master
		Page germanMasterPage = createMasterObject(supply(() -> node.getFolder()));
		String germanMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), germanMasterPage);

		// translate
		Page englishMasterPage = createTranslation(germanMasterPage);
		String englishMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), englishMasterPage);

		// localize german page
		Page germanLocalizedPage = localizeObject(germanMasterPage);
		String germanLocalizedPageGlobalId = execute(o -> o.getGlobalId().toString(), germanLocalizedPage);

		// localize english page
		Page englishLocalizedPage = localizeObject(englishMasterPage);
		String englishLocalizedPageGlobalId = execute(o -> o.getGlobalId().toString(), englishLocalizedPage);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanMasterPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishMasterPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanLocalizedPageGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishLocalizedPageGlobalId, false));

		// delete english translation in channel and master
		englishLocalizedPage = delete(englishLocalizedPage);
		englishMasterPage = delete(englishMasterPage);

		// translate again (which creates a conflicting page language)
		Page conflictingEnglishMasterPage = createTranslation(germanMasterPage);
		String conflictingEnglishMasterPageGlobalId = execute(o -> o.getGlobalId().toString(), conflictingEnglishMasterPage);

		// localize again
		Page conflictingEnglishLocalizedPage = localizeObject(conflictingEnglishMasterPage);
		String conflictingEnglishLocalizedPageGlobalId = execute(o -> o.getGlobalId().toString(), conflictingEnglishLocalizedPage);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		germanMasterPage = supply(t -> t.getObject(clazz, germanMasterPageGlobalId));
		englishMasterPage = supply(t -> t.getObject(clazz, englishMasterPageGlobalId));
		germanLocalizedPage = supply(t -> t.getObject(clazz, germanLocalizedPageGlobalId));
		englishLocalizedPage = supply(t -> t.getObject(clazz, englishLocalizedPageGlobalId));
		conflictingEnglishMasterPage = supply(t -> t.getObject(clazz, conflictingEnglishMasterPageGlobalId));
		conflictingEnglishLocalizedPage = supply(t -> t.getObject(clazz, conflictingEnglishLocalizedPageGlobalId));
		try (Trx trx = new Trx()) {
			assertLocalizedObject(germanMasterPage, germanLocalizedPage, node.getFolder(), "de");
			assertLocalizedObject(englishMasterPage, englishLocalizedPage, node.getFolder(), "en");
			assertLanguageVariants(germanMasterPage, englishMasterPage);
			assertLanguageVariants(germanLocalizedPage, englishLocalizedPage);

			assertThat(conflictingEnglishMasterPage).as("Conflicting language Variant in master").isNull();
			assertThat(conflictingEnglishLocalizedPage).as("Conflicting language Variant in channel").isNull();
			trx.success();
		}

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, germanMasterPage, englishMasterPage);
		expectAllDirted(sideChannel, germanMasterPage, englishMasterPage);
		expectAllDirted(channel, germanLocalizedPage, englishLocalizedPage);
		expectAllDirted(subChannel, germanLocalizedPage, englishLocalizedPage);
		assertObjectsAreDirted();
	}

	@Override
	protected Page createMasterObject(Folder folder) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Master Page");
			p.setLanguage(getLanguage("de"));
		}).publish().build();
	}

	@Override
	protected Page localizeObject(Page master) throws NodeException {
		Page p = execute(m -> localize(m, channel), master);
		return update(p, upd -> {}).publish().build();
	}

	@Override
	protected Page createLocalObject(Folder folder) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Local Page");
			p.setChannelInfo(channel.getId(), null);
			p.setLanguage(getLanguage("de"));
		}).publish().build();
	}

	@Override
	protected Page createExcludedObject(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), false)).build();
	}

	@Override
	protected Page createDisinheritedObject(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(false, Collections.singleton(channel), false))
				.build();
	}

	@Override
	protected Page createDefaultDisinherited(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.setDisinheritDefault(true, false)).build();
	}

	/**
	 * Create the english translation of the given page
	 * @param page page to translate
	 * @return english translation
	 * @throws NodeException
	 */
	protected Page createTranslation(Page page) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(page.getFolderId());
			p.setTemplateId(template.getId());
			p.setName("Translation of %s".formatted(page.getName()));
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(page.getContentsetId());
			if (page.getChannel() != null) {
				if (page.isMaster()) {
					p.setChannelInfo(page.getChannel().getId(), null);
				} else {
					Page masterTranslation = page.getMaster().getLanguageVariant("en");
					assertThat(masterTranslation).as("Translation of the master page").isNotNull();
					p.setChannelInfo(page.getChannel().getId(), masterTranslation.getChannelSetId());
				}
			}
		}).publish().build();
	}

	/**
	 * Assert that the object in the CMS exists and is a master in the given folder using the given language
	 * @param object object to check
	 * @param folder asserted folder
	 * @param languageCode asserted language code
	 * @throws NodeException
	 */
	protected void assertMasterObject(Page object, Folder folder, String languageCode) throws NodeException {
		assertThat(object).as("Staged master page")
			.isNotNull()
			.isMaster()
			.isInheritedIn(channel)
			.hasParent(folder)
			.hasLanguage(languageCode);
	}

	@Override
	protected void assertMasterObject(Page object, Folder folder) throws NodeException {
		assertMasterObject(object, folder, "de");
	}

	/**
	 * Assert that the given objects are master and localized copy in the given folder using the given language
	 * @param masterObject master
	 * @param localizedObject localized copy
	 * @param folder asserted folder
	 * @param languageCode asserted language code
	 * @throws NodeException
	 */
	protected void assertLocalizedObject(Page masterObject, Page localizedObject, Folder folder, String languageCode) throws NodeException {
		assertThat(localizedObject).as("Staged localized page")
			.isNotNull()
			.isNotMaster()
			.hasChannel(channel)
			.hasMaster(masterObject)
			.hasParent(folder)
			.hasLanguage(languageCode);
	}

	@Override
	protected void assertLocalizedObject(Page masterObject, Page localizedObject, Folder folder) throws NodeException {
		assertLocalizedObject(masterObject, localizedObject, folder, "de");
	}

	/**
	 * Assert that the object in the CMS exists and is local to the channel in the given folder using the given language
	 * @param object object to check
	 * @param channel expected channel
	 * @param folder asserted folder
	 * @param languageCode asserted language code
	 * @throws NodeException
	 */
	protected void assertLocalObject(Page object, Node channel, Folder folder, String languageCode) throws NodeException {
		assertThat(object).as("Staged local page")
			.isNotNull()
			.hasChannel(channel)
			.isMaster()
			.hasParent(folder)
			.hasLanguage(languageCode);
	}

	@Override
	protected void assertLocalObject(Page object, Node channel, Folder folder) throws NodeException {
		assertLocalObject(object, channel, folder, "de");
	}

	@Override
	protected void assertExcluded(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isExcluded()
			.isDisinheritedIn()
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDisinherited(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn(channel)
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDefaultDisinherited(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn()
			.isDisinheritedByDefault();
	}

	/**
	 * Assert that the given pages are language variants of each other
	 * @param de german language variant
	 * @param en english language variant
	 * @throws NodeException
	 */
	protected void assertLanguageVariants(Page de, Page en) throws NodeException {
		assertThat(de).as("Staged german page")
			.isNotNull()
			.hasLanguage("de")
			.hasLanguageVariant("en", en);
		assertThat(en).as("Staged english page")
			.isNotNull()
			.hasLanguage("en")
			.hasLanguageVariant("de", de);
	}
}
