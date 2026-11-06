package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TagStatus;
import com.gentics.contentnode.rest.model.response.TagStatusResponse;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for synchronization of pages with the editable tags in a template
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class TemplateSyncTest {
	public final static String TAG_NAME = "migrate";

	public final static String MISSING_TAG_NAME = "missing";

	public final static String TAG_CONTENT = "Tag content";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node node;
	private static Node channel;
	private static Integer sourceConstructId;
	private static Integer compatibleConstructId;
	private static Integer differentKeywordConstructId;
	private static Integer incompatibleConstructId;

	private static Template sourceTemplate;

	private static Template compatibleTemplate;

	private static Template differentKeywordTemplate;

	private static Template incompatibleKeywordTemplate;

	private static Template templateWithTwoTags;

	private static UserGroup testGroup;

	private static SystemUser testUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		sourceConstructId = supply(() -> createConstruct(node, NormalTextPartType.class, "source", "text"));
		compatibleConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "compatible", "text"));
		differentKeywordConstructId = supply(() -> createConstruct(node, ShortTextPartType.class, "differentkeyword", "shorttext"));
		incompatibleConstructId = supply(() -> createConstruct(node, PageURLPartType.class, "incompatible", "text"));

		sourceTemplate = template("Source Template", sourceConstructId);
		compatibleTemplate = template("Compatible Template", compatibleConstructId);
		differentKeywordTemplate = template("Different keyword Template", differentKeywordConstructId);
		incompatibleKeywordTemplate = template("Incompatible keyword Template", incompatibleConstructId);

		templateWithTwoTags = supply(() -> create(Template.class, template -> {
			template.getFolders().add(node.getFolder());
			template.setName("Two-tag Template");
			template.setMlId(1);
			template.setSource("");
			template.getTemplateTags().put(TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(compatibleConstructId);
				tag.setName(TAG_NAME);
				tag.setEnabled(true);
				tag.setPublic(true);
			}, false));
			template.getTemplateTags().put(MISSING_TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(compatibleConstructId);
				tag.setName(MISSING_TAG_NAME);
				tag.setEnabled(true);
				tag.setPublic(true);
			}, false));
		}));

		testGroup = supply(() -> createUserGroup("Testgroup", 1));
		testUser = supply(() -> createSystemUser("Test", "Test", null, "test", "test", Arrays.asList(testGroup)));
	}

	/**
	 * Create template with a single tag
	 * @param name template name
	 * @param constructId construct ID
	 * @return template
	 * @throws NodeException
	 */
	protected static Template template(String name, int constructId) throws NodeException {
		return supply(() -> create(Template.class, template -> {
			template.getFolders().add(node.getFolder());
			template.setName(name);
			template.setMlId(1);
			template.setSource("");
			template.getTemplateTags().put(TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(constructId);
				tag.setName(TAG_NAME);
				tag.setEnabled(true);
				tag.setPublic(true);
			}, false));
		}));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW).toString()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_TEMPLATE_VIEW).toString()));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			for (Node n : Arrays.asList(node, channel)) {
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					for (Page page : node.getFolder().getPages()) {
						page.delete(true);
					}
				}
			}
		});
	}

	/**
	 * Test migration of compatible tag
	 * @throws NodeException
	 */
	@Test
	public void testCompatible() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));

		assertMigration(compatibleTemplate, false, 1, 1, 0);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setInSync(1));

		page = execute(Page::reload, page);

		String migratedValue = execute(p -> getPartType(LongHTMLPartType.class, p.getContentTag(TAG_NAME), "text").getText(), page);
		assertThat(migratedValue).as("Migrated value").isEqualTo(TAG_CONTENT);
	}

	/**
	 * Test migration of incompatible tag with "force"
	 * @throws NodeException
	 */
	@Test
	public void testIncompatible() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(incompatibleKeywordTemplate.getId());
		});

		assertTagStatus(incompatibleKeywordTemplate, new TagStatus().setName(TAG_NAME).setConstructId(incompatibleConstructId).setOutOfSync(1).setIncompatible(1));

		assertMigration(incompatibleKeywordTemplate, false, 0, 1, 0);

		assertTagStatus(incompatibleKeywordTemplate, new TagStatus().setName(TAG_NAME).setConstructId(incompatibleConstructId).setOutOfSync(1).setIncompatible(1));

		assertMigration(incompatibleKeywordTemplate, true, 1, 1, 0);
	}

	/**
	 * Test that tag with different part keyword is not compatible
	 * @throws NodeException
	 */
	@Test
	public void testDifferentKeyword() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(differentKeywordTemplate.getId());
		});

		assertTagStatus(differentKeywordTemplate, new TagStatus().setName(TAG_NAME).setConstructId(differentKeywordConstructId).setOutOfSync(1).setIncompatible(1));

		assertMigration(differentKeywordTemplate, false, 0, 1, 0);

		assertTagStatus(differentKeywordTemplate, new TagStatus().setName(TAG_NAME).setConstructId(differentKeywordConstructId).setOutOfSync(1).setIncompatible(1));
	}

	/**
	 * Test migration of missing tag
	 * @throws NodeException
	 */
	@Test
	public void testMissing() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.getContent().getTags().remove(TAG_NAME);
		});

		assertTagStatus(sourceTemplate, new TagStatus().setName(TAG_NAME).setConstructId(sourceConstructId).setMissing(1));

		assertMigration(sourceTemplate, false, 1, 1, 0);

		page = execute(Page::reload, page);

		String migratedValue = execute(p -> getPartType(NormalTextPartType.class, p.getContentTag(TAG_NAME), "text").getText(), page);
		assertThat(migratedValue).as("Migrated value").isEmpty();
	}

	/**
	 * Test migration of localized page
	 * @throws NodeException
	 */
	@Test
	public void testLocalized() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		execute(p -> localize(p, channel), page);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(2));

		assertMigration(compatibleTemplate, false, 2, 2, 0);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setInSync(2));
	}

	/**
	 * Test migration of local page
	 * @throws NodeException
	 */
	@Test
	public void testLocal() throws NodeException {
		Page page = sourcePage(channel);

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));

		assertMigration(compatibleTemplate, false, 1, 1, 0);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setInSync(1));
	}

	/**
	 * Test migration with missing template permission
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = InsufficientPrivilegesException.class)
	public void testMissingTemplatePermission() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString()));

		try (Trx trx = new Trx(testUser)) {
			TagStatusResponse tagStatusResponse = new TemplateResourceImpl().tagStatus(Integer.toString(compatibleTemplate.getId()), new SortParameterBean(),
					new FilterParameterBean(), new PagingParameterBean());

			assertResponseCode(tagStatusResponse, ResponseCode.PERMISSION);
		}
	}

	/**
	 * Test that migration omits pages without permission
	 * @throws NodeException
	 */
	@Test
	public void testMissingPagePermission() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
			p.unlock();
		});

		try (Trx trx = new Trx(testUser)) {
			assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));
			CNI18nString message = new CNI18nString("rest.page.nopermission");
			message.setParameter("0", page.getId().toString());
			assertMigration(compatibleTemplate, false, 0, 1, 1,
					new DefaultNodeMessage(Level.ERROR, Template.class, message.toString()));
			assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));
		}
	}

	/**
	 * Test that migration omits pages locked by another user
	 * @throws NodeException
	 */
	@Test
	public void testLocked() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_TEMPLATE_VIEW)
						.toString()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(testGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_TEMPLATE_VIEW)
						.toString()));

		try (Trx trx = new Trx(testUser)) {
			assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));

			String message = I18NHelper.get("page.readonly.locked", I18NHelper.getName(page));

			assertMigration(compatibleTemplate, false, 0, 1, 1, new DefaultNodeMessage(Level.ERROR, Template.class, message));
			assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));
		}
	}

	/**
	 * Test that page will remain published when migrated
	 * @throws NodeException
	 */
	@Test
	public void testPublished() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(templateWithTwoTags.getId(), false);
			p.publish();
		});

		TagStatusResponse tagStatusResponse = supply(() -> new TemplateResourceImpl().tagStatus(Integer.toString(templateWithTwoTags.getId()), new SortParameterBean(), new FilterParameterBean(),
				new PagingParameterBean()));
		assertResponseCodeOk(tagStatusResponse);

		assertThat(tagStatusResponse.getItems()).as("Tag status").usingElementComparatorOnFields("name", "inSync", "outOfSync", "incompatible", "missing")
				.containsOnly(new TagStatus().setName(TAG_NAME).setInSync(0).setOutOfSync(1).setIncompatible(0).setMissing(0),
						new TagStatus().setName(MISSING_TAG_NAME).setInSync(0).setOutOfSync(0).setIncompatible(0).setMissing(1));

		assertMigration(templateWithTwoTags, false, 1, 1, 0);

		page = execute(Page::reload, page);
		assertThat(execute(Page::isModified, page)).as("Page modified after migration").isEqualTo(false);
		assertThat(execute(Page::isOnline, page)).as("Page online after migration").isEqualTo(true);
	}

	/**
	 * Test filtered migration for specific tags
	 * @throws NodeException
	 */
	@Test
	public void testFilter() throws NodeException {
		Page page = sourcePage();

		page = update(page, p -> {
			p.setTemplateId(compatibleTemplate.getId());
		});

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));

		assertMigration(compatibleTemplate, Arrays.asList("bla", "blubb"), false, 0, 1, 0);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setOutOfSync(1));

		assertMigration(compatibleTemplate, Arrays.asList(TAG_NAME), false, 1, 1, 0);

		assertTagStatus(compatibleTemplate, new TagStatus().setName(TAG_NAME).setConstructId(compatibleConstructId).setInSync(1));
	}

	/**
	 * Assert that the tag status of the template contains the given TagStatus instances.
	 * The method will either use the existing transaction or create a new one, if none exists.
	 * TagStatus instances are checked for the following fields "name", "constructId", "inSync", "outOfSync", "incompatible" and "missing"
	 * @param template template
	 * @param status expected tagstatus instances
	 * @throws NodeException
	 */
	protected void assertTagStatus(Template template, TagStatus...status)
			throws NodeException {
		Supplier<TagStatusResponse> supplier = () -> {
			return new TemplateResourceImpl().tagStatus(Integer.toString(template.getId()), new SortParameterBean(), new FilterParameterBean(),
					new PagingParameterBean());
		};

		TagStatusResponse tagStatusResponse = TransactionManager.getCurrentTransactionOrNull() == null ? supply(supplier) : supplier.supply();

		assertResponseCodeOk(tagStatusResponse);
		assertThat(tagStatusResponse.getItems()).as("Tag status")
				.usingElementComparatorOnFields("name", "constructId", "inSync", "outOfSync", "incompatible", "missing")
				.containsOnly(status);
	}

	/**
	 * Migrate pages for the template and assert the result.
	 * The method will either use the existing transaction or create a new one, if none exists.
	 * @param template template
	 * @param force true to force migration of incompatible tags
	 * @param expectedNumPagesUpdated updated pages
	 * @param expectedTotalPages total pages
	 * @param expectedNumErrors errors
	 * @param messages messages
	 * @throws NodeException
	 */
	protected void assertMigration(Template template, boolean force, int expectedNumPagesUpdated, int expectedTotalPages, int expectedNumErrors,
			NodeMessage... messages) throws NodeException {
		assertMigration(template, null, force, expectedNumPagesUpdated, expectedTotalPages, expectedNumErrors, messages);
	}

	/**
	 * Migrate pages for the template and assert the result.
	 * The method will either use the existing transaction or create a new one, if none exists.
	 * @param template template
	 * @param tagnames list of tagnames to migrate (null for all tags)
	 * @param force true to force migration of incompatible tags
	 * @param expectedNumPagesUpdated updated pages
	 * @param expectedTotalPages total pages
	 * @param expectedNumErrors errors
	 * @param messages messages
	 * @throws NodeException
	 */
	protected void assertMigration(Template template, List<String> tagnames, boolean force, int expectedNumPagesUpdated, int expectedTotalPages, int expectedNumErrors,
			NodeMessage... messages) throws NodeException {
		Supplier<UpdatePagesResult> supplier = () -> template.updatePages(1, 5, tagnames, force);
		UpdatePagesResult migrationResult = TransactionManager.getCurrentTransactionOrNull() == null ? supply(supplier) : supplier.supply();

		assertThat(migrationResult).as("Migration result").has(attribute("numPagesUpdated", expectedNumPagesUpdated))
				.has(attribute("totalPages", expectedTotalPages)).has(attribute("numErrors", expectedNumErrors));

		if (messages.length > 0) {
			assertThat(migrationResult.getMessages()).as("Migration result messages").usingElementComparatorOnFields("type", "message", "throwable")
					.containsOnly(messages);
		}
	}

	/**
	 * Create a source page in the master node
	 * @return page
	 * @throws NodeException
	 */
	protected Page sourcePage() throws NodeException {
		return sourcePage(null);
	}

	/**
	 * Create a source page in the master node or channel
	 * @param channel channel (may be null)
	 * @return page
	 * @throws NodeException
	 */
	protected Page sourcePage(Node channel) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(sourceTemplate.getId());
			if (channel != null) {
				p.setChannelInfo(channel.getId(), null);
			}

			getPartType(NormalTextPartType.class, p.getContentTag(TAG_NAME), "text").getValueObject().setValueText(TAG_CONTENT);
		});
	}
}
