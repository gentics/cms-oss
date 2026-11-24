package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the validation, which prevents moving or copying pages into other nodes,
 * when the pages link to forms (via tags with parts of type {@link CmsFormPartType}).
 */
public class CrossNodePageLinkedFormTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Name of the tag (and part) linking to forms
	 */
	public final static String FORM_TAG = "formtag";

	/**
	 * Name of the object property linking to forms
	 */
	public final static String FORM_OBJECT_PROPERTY = "formprop";

	/**
	 * Source node with languages "de" and "en"
	 */
	private static Node node;

	/**
	 * Other node with languages "de" and "en"
	 */
	private static Node otherNode;

	/**
	 * Other node with language "de" only
	 */
	private static Node germanNode;

	/**
	 * Root folders of the nodes
	 */
	private static Folder nodeFolder;

	private static Folder otherNodeFolder;

	private static Folder germanNodeFolder;

	private static Template template;

	private static SystemUser systemUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("source", "Source", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		otherNode = supply(() -> createNode("other", "Other", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		germanNode = supply(() -> createNode("german", "GermanOnly", PublishTarget.NONE, getLanguage("de")));
		nodeFolder = supply(() -> node.getFolder());
		otherNodeFolder = supply(() -> otherNode.getFolder());
		germanNodeFolder = supply(() -> germanNode.getFolder());

		int constructId = supply(() -> createConstruct(node, CmsFormPartType.class, FORM_TAG, FORM_TAG));
		operate(t -> {
			Construct construct = t.getObject(Construct.class, constructId, true);
			construct.getNodes().add(otherNode);
			construct.getNodes().add(germanNode);
			construct.save();
		});

		template = supply(() -> update(createTemplate(nodeFolder, "Template"), tmpl -> {
			tmpl.setSource("<node " + FORM_TAG + ">");
			tmpl.getTemplateTags().put(FORM_TAG, create(TemplateTag.class, tt -> {
				tt.setConstructId(constructId);
				tt.setEnabled(true);
				tt.setName(FORM_TAG);
				tt.setPublic(true);
			}, false));
		}));

		operate(() -> createObjectTagDefinition(FORM_OBJECT_PROPERTY, Page.TYPE_PAGE, constructId, node, otherNode, germanNode));

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			for (Node n : Arrays.asList(node, otherNode, germanNode)) {
				clear(n);
			}
		});
	}

	// ==================== Moving pages ====================

	/**
	 * Test moving a page with a linked form (in a content tag) into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithLinkedFormToOtherNode() throws NodeException {
		Page page = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		GenericResponse response = movePages(otherNodeFolder, true, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		assertThat(getPageFolderId(page)).as("Folder of page").isEqualTo(nodeFolder.getId());
	}

	/**
	 * Test moving a page with a linked form (in an object tag) into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithFormInObjectTagToOtherNode() throws NodeException {
		Form form = createForm(nodeFolder, "Test Form");
		Page page = supply(() -> update(createPage(nodeFolder, template, "PropPage"), upd -> {
			upd.getObjectTag(FORM_OBJECT_PROPERTY).setEnabled(true);
			getPartType(CmsFormPartType.class, upd.getObjectTag(FORM_OBJECT_PROPERTY), FORM_TAG).setTarget(form);
		}));

		GenericResponse response = movePages(otherNodeFolder, true, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "PropPage", "object." + FORM_OBJECT_PROPERTY);

		assertThat(getPageFolderId(page)).as("Folder of page").isEqualTo(nodeFolder.getId());
	}

	/**
	 * Test moving a page with a linked form within the same node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithLinkedFormWithinNode() throws NodeException {
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));
		Page page = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		GenericResponse response = movePages(target, true, page);
		assertResponseOK(response);

		assertThat(getPageFolderId(page)).as("Folder of moved page").isEqualTo(target.getId());
	}

	/**
	 * Test moving a page with a form tag, which does not link to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithEmptyFormTagToOtherNode() throws NodeException {
		Page page = supply(() -> createPage(nodeFolder, template, "Page"));

		GenericResponse response = movePages(otherNodeFolder, true, page);
		assertResponseOK(response);

		assertThat(getPageFolderId(page)).as("Folder of moved page").isEqualTo(otherNodeFolder.getId());
	}

	/**
	 * Test moving all languages of a page into another node, where only a language variant links to a form
	 * @throws NodeException
	 */
	@Test
	public void testMoveAllLanguagesWithVariantLinkingFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Page englishPage = createLanguageVariantWithLinkedForm(germanPage, "en");

		GenericResponse response = movePages(otherNodeFolder, true, germanPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		assertThat(getPageFolderId(germanPage)).as("Folder of german page").isEqualTo(nodeFolder.getId());
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(nodeFolder.getId());
	}

	/**
	 * Test moving only a single language variant (without linked form) into another node, while another language variant links to a form
	 * @throws NodeException
	 */
	@Test
	public void testMoveSingleLanguageWithoutFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Page englishPage = createLanguageVariantWithLinkedForm(germanPage, "en");

		GenericResponse response = movePages(otherNodeFolder, false, germanPage);
		assertResponseOK(response);

		assertThat(getPageFolderId(germanPage)).as("Folder of moved german page").isEqualTo(otherNodeFolder.getId());
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(nodeFolder.getId());
	}

	/**
	 * Test moving only a single language variant, which links to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testMoveSingleLanguageWithFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Page englishPage = createLanguageVariantWithLinkedForm(germanPage, "en");

		GenericResponse response = movePages(otherNodeFolder, false, englishPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		assertThat(getPageFolderId(germanPage)).as("Folder of german page").isEqualTo(nodeFolder.getId());
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(nodeFolder.getId());
	}

	/**
	 * Test moving multiple pages into another node, where one of the pages has a linked form
	 * @throws NodeException
	 */
	@Test
	public void testMoveMultiplePagesOneWithLinkedFormToOtherNode() throws NodeException {
		Page okPage = supply(() -> createPage(nodeFolder, template, "OkPage"));
		Page formPage = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		GenericResponse response = movePages(otherNodeFolder, true, okPage, formPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		assertThat(getPageFolderId(formPage)).as("Folder of page with form").isEqualTo(nodeFolder.getId());
	}

	// ==================== Copying pages ====================

	/**
	 * Test copying a page with a linked form into another node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLinkedFormToOtherNode() throws NodeException {
		Page page = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		PageCopyResponse response = copyPages(otherNodeFolder, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		assertThat(getPageCount(otherNodeFolder)).as("Pages in target folder").isEqualTo(0);
	}

	/**
	 * Test copying a page with a linked form within the same node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLinkedFormWithinNode() throws NodeException {
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));
		Page page = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		PageCopyResponse response = copyPages(target, page);
		assertResponseOK(response);

		assertThat(getPageCount(target)).as("Pages in target folder").isEqualTo(1);
	}

	/**
	 * Test copying a page with a form tag, which does not link to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithEmptyFormTagToOtherNode() throws NodeException {
		Page page = supply(() -> createPage(nodeFolder, template, "Page"));

		PageCopyResponse response = copyPages(otherNodeFolder, page);
		assertResponseOK(response);

		assertThat(getPageCount(otherNodeFolder)).as("Pages in target folder").isEqualTo(1);
	}

	/**
	 * Test copying a page into another node, where only a language variant links to a form.
	 * The language variants are copied as well, so the copy must fail.
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithVariantLinkingFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		createLanguageVariantWithLinkedForm(germanPage, "en");

		PageCopyResponse response = copyPages(otherNodeFolder, germanPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		assertThat(getPageCount(otherNodeFolder)).as("Pages in target folder").isEqualTo(0);
	}

	/**
	 * Test copying a page into another node, where a language variant links to a form, but the language of that
	 * variant is not activated in the target node. That variant will not be copied, so the copy succeeds.
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithVariantLinkingFormInLanguageMissingInTargetNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		createLanguageVariantWithLinkedForm(germanPage, "en");

		PageCopyResponse response = copyPages(germanNodeFolder, germanPage);
		assertResponseOK(response);

		List<Page> copies = getPages(germanNodeFolder);
		assertThat(copies).as("Pages in target folder").hasSize(1);
		assertThat(supply(() -> copies.get(0).getLanguage().getCode())).as("Language of copy").isEqualTo("de");
	}

	/**
	 * Test copying multiple pages into another node, where one of the pages has a linked form
	 * @throws NodeException
	 */
	@Test
	public void testCopyMultiplePagesOneWithLinkedFormToOtherNode() throws NodeException {
		Page okPage = supply(() -> createPage(nodeFolder, template, "OkPage"));
		Page formPage = createPageWithLinkedForm(nodeFolder, "FormPage", null);

		PageCopyResponse response = copyPages(otherNodeFolder, okPage, formPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		assertThat(getPages(otherNodeFolder)).as("Pages in target folder").extracting(Page::getName).doesNotContain("FormPage");
	}

	// ==================== Helpers ====================

	/**
	 * Create a form
	 * @param folder folder
	 * @param name form name
	 * @return form
	 * @throws NodeException
	 */
	protected Form createForm(Folder folder, String name) throws NodeException {
		return supply(() -> create(Form.class, f -> {
			f.setFormType("generic");
			f.setName(name);
			f.setFolderId(folder.getId());
		}));
	}

	/**
	 * Create a page, which links to a new form in its content tag
	 * @param folder folder
	 * @param name page name
	 * @param language optional language code
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPageWithLinkedForm(Folder folder, String name, String language) throws NodeException {
		Form form = createForm(folder, name + " Form");
		return supply(() -> update(createPage(folder, template, name, null, language != null ? getLanguage(language) : null), upd -> {
			getPartType(CmsFormPartType.class, upd.getContentTag(FORM_TAG), FORM_TAG).setTarget(form);
		}));
	}

	/**
	 * Create a language variant of the given page, which links to a new form in its content tag
	 * @param page page
	 * @param language language code of the variant
	 * @return language variant
	 * @throws NodeException
	 */
	protected Page createLanguageVariantWithLinkedForm(Page page, String language) throws NodeException {
		Form form = createForm(nodeFolder, "Variant Form " + language);
		return supply(t -> {
			Page variant = (Page) t.getObject(Page.class, page.getId()).copy();
			variant.setContentsetId(page.getContentsetId());
			variant.setLanguage(getLanguage(language));
			variant.setFilename("page_" + language + ".html");
			getPartType(CmsFormPartType.class, variant.getContentTag(FORM_TAG), FORM_TAG).setTarget(form);
			variant.save();
			return t.getObject(Page.class, variant.getId());
		});
	}

	/**
	 * Move the given pages via the REST API
	 * @param target target folder
	 * @param allLanguages true to move all language variants
	 * @param pages pages to move
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse movePages(Folder target, boolean allLanguages, Page... pages) throws NodeException {
		return supply(systemUser, () -> {
			MultiObjectMoveRequest request = new MultiObjectMoveRequest();
			request.setFolderId(target.getId());
			request.setAllLanguages(allLanguages);
			request.setIds(Arrays.stream(pages).map(p -> Integer.toString(p.getId())).collect(Collectors.toList()));
			return getPageResource().move(request);
		});
	}

	/**
	 * Copy the given pages via the REST API
	 * @param target target folder
	 * @param pages pages to copy
	 * @return response
	 * @throws NodeException
	 */
	protected PageCopyResponse copyPages(Folder target, Page... pages) throws NodeException {
		return supply(systemUser, () -> {
			PageCopyRequest request = new PageCopyRequest();
			request.setCreateCopy(true);
			request.setSourcePageIds(Arrays.stream(pages).map(Page::getId).collect(Collectors.toList()));
			request.setTargetFolders(Arrays.asList(new TargetFolder(target.getId(), 0)));
			return getPageResource().copy(request, 0);
		});
	}

	/**
	 * Assert that the response contains a message, which contains all given fragments
	 * @param response response
	 * @param fragments expected fragments
	 */
	protected void assertMessageContains(GenericResponse response, String... fragments) {
		assertThat(response.getMessages()).as("Response messages").isNotEmpty();
		List<String> messages = response.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());
		assertThat(messages).as("Response messages").anySatisfy(message -> {
			for (String fragment : fragments) {
				assertThat(message).contains(fragment);
			}
		});
	}

	protected int getPageFolderId(Page page) throws NodeException {
		return supply(t -> t.getObject(Page.class, page.getId()).getFolder().getId());
	}

	protected List<Page> getPages(Folder folder) throws NodeException {
		return supply(t -> t.getObject(Folder.class, folder.getId()).getPages());
	}

	protected int getPageCount(Folder folder) throws NodeException {
		return getPages(folder).size();
	}
}
