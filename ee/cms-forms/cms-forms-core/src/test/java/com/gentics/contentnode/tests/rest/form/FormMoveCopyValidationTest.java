package com.gentics.contentnode.tests.rest.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.MultiFolderMoveRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the validations, which prevent
 * <ul>
 * <li>moving forms (by moving folders) into nodes, which do not have all form languages activated</li>
 * <li>moving or copying pages with linked forms into other nodes</li>
 * </ul>
 */
@GCNFeature(set = { Feature.WASTEBIN })
@ConfigBy(FormMoveCopyValidationTest.class)
public class FormMoveCopyValidationTest implements ConfigModifier {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Key of the form type, which maps external language codes to internal language codes
	 */
	public final static String MAPPED_TYPE = "mapped";

	/**
	 * Language mapping of the form type {@link #MAPPED_TYPE} (external code -> internal code)
	 */
	public final static Map<String, String> LANGUAGE_MAPPING = Map.of("deu", "de", "eng", "en");

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
	private static Node allLanguagesNode;

	/**
	 * Other node with language "de" only
	 */
	private static Node germanNode;

	/**
	 * Root folders of the nodes (loaded once, because {@link Node#getFolder()} requires a transaction)
	 */
	private static Folder nodeFolder;

	private static Folder allLanguagesNodeFolder;

	private static Folder germanNodeFolder;

	private static Template template;

	private static SystemUser systemUser;

	private static String englishName;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("source", "Source", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		allLanguagesNode = supply(() -> createNode("all", "AllLanguages", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		germanNode = supply(() -> createNode("german", "GermanOnly", PublishTarget.NONE, getLanguage("de")));
		nodeFolder = supply(() -> node.getFolder());
		allLanguagesNodeFolder = supply(() -> allLanguagesNode.getFolder());
		germanNodeFolder = supply(() -> germanNode.getFolder());

		int constructId = supply(() -> createConstruct(node, CmsFormPartType.class, FORM_TAG, FORM_TAG));
		operate(t -> {
			Construct construct = t.getObject(Construct.class, constructId, true);
			construct.getNodes().add(allLanguagesNode);
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

		operate(() -> createObjectTagDefinition(FORM_OBJECT_PROPERTY, Page.TYPE_PAGE, constructId, node, allLanguagesNode,
				germanNode));

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
		englishName = supply(() -> getLanguage("en").getName());
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		// add the default form types
		new FormTypeConfigurationModifier().accept(prefs);

		// add a form type with language mapping
		FormTypeConfigurationMap formTypes = prefs.getPropertyObject(FormConfigProvider.TYPES_CONFIG_PATH);
		FormTypeConfiguration mappedConfig = new FormTypeConfiguration(Map.of("de", "Formular mit Sprachmapping", "en", "Form with language mapping"),
				"forms");
		mappedConfig.languageMapping = LANGUAGE_MAPPING;
		formTypes.put(MAPPED_TYPE, mappedConfig);
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				for (Node n : Arrays.asList(node, allLanguagesNode, germanNode)) {
					clear(n);
				}
			}
		});
	}

	// ==================== Moving forms (via folders) into other nodes ====================

	/**
	 * Test moving a folder containing a form into a node, which has all form languages activated
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormToNodeWithAllLanguages() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "Test Form", "de", "en");

		GenericResponse response = moveFolders(allLanguagesNodeFolder, folder);
		assertResponseOK(response);

		assertThat(getFormNodeId(form)).as("Node of moved form").isEqualTo(allLanguagesNode.getId());
	}

	/**
	 * Test moving a folder containing a form into a node, which does not have all form languages activated
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormToNodeWithMissingLanguage() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "Test Form", "de", "en");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Test Form", englishName);

		assertThat(getFolderNodeId(folder)).as("Node of folder").isEqualTo(node.getId());
		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder containing forms, which only use languages activated in the target node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormUsingOnlyAvailableLanguages() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "Test Form", "de");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponseOK(response);

		assertThat(getFormNodeId(form)).as("Node of moved form").isEqualTo(germanNode.getId());
	}

	/**
	 * Test moving a folder into a node with missing languages, when the form is located in a subfolder
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormInSubfolderToNodeWithMissingLanguage() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Folder subFolder = supply(() -> createFolder(folder, "Sub"));
		Form form = createForm(subFolder, "Nested Form", "de", "en");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Nested Form", englishName);

		assertThat(getFolderNodeId(folder)).as("Node of folder").isEqualTo(node.getId());
		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder into a node with missing languages, when the form is in the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithDeletedFormToNodeWithMissingLanguage() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "Deleted Form", "de", "en");
		consume(Form::delete, form);

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Deleted Form", englishName);

		assertThat(getFolderNodeId(folder)).as("Node of folder").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder without forms into a node with fewer languages
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithoutForms() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "NoForms"));

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponseOK(response);

		assertThat(getFolderNodeId(folder)).as("Node of moved folder").isEqualTo(germanNode.getId());
	}

	/**
	 * Test moving multiple folders at once, where one folder contains a form with a language missing in the target node
	 * @throws NodeException
	 */
	@Test
	public void testMoveMultipleFoldersOneWithMissingLanguage() throws NodeException {
		Folder okFolder = supply(() -> createFolder(nodeFolder, "OkForms"));
		Form okForm = createForm(okFolder, "German Form", "de");
		Folder failFolder = supply(() -> createFolder(nodeFolder, "FailForms"));
		Form failForm = createForm(failFolder, "Bilingual Form", "de", "en");

		GenericResponse response = moveFolders(germanNodeFolder, okFolder, failFolder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Bilingual Form", englishName);

		// the folder with the invalid form must not be moved
		assertThat(getFolderNodeId(failFolder)).as("Node of folder with invalid form").isEqualTo(node.getId());
		assertThat(getFormNodeId(failForm)).as("Node of invalid form").isEqualTo(node.getId());

		// the folders are moved one after the other, so the valid folder is still moved
		assertThat(getFolderNodeId(okFolder)).as("Node of folder with valid form").isEqualTo(germanNode.getId());
		assertThat(getFormNodeId(okForm)).as("Node of valid form").isEqualTo(germanNode.getId());
	}

	/**
	 * Test that the failure message names the moved folder, the target node and all forms with missing languages
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithMultipleInvalidFormsMessage() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "MultiForms"));
		createForm(folder, "First Bilingual Form", "de", "en");
		createForm(folder, "Second Bilingual Form", "de", "en");
		createForm(folder, "German Form", "de");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "MultiForms", "GermanOnly", "First Bilingual Form", "Second Bilingual Form",
				englishName);
		assertMessageNotContains(response, "German Form");

		assertThat(getFolderNodeId(folder)).as("Node of folder").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder containing a form without languages into a node with fewer languages
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormWithoutLanguages() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "No Language Form");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponseOK(response);

		assertThat(getFormNodeId(form)).as("Node of moved form").isEqualTo(germanNode.getId());
	}

	/**
	 * Test moving a folder containing a form with a language code, which does not exist in the system.
	 * The unknown code must be part of the failure message
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormWithUnknownLanguage() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Form form = createForm(folder, "Unknown Language Form", "de", "xx");

		GenericResponse response = moveFolders(allLanguagesNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Unknown Language Form", "xx");

		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder containing a form of a type with language mapping, where all mapped languages are activated in the target node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithMappedFormLanguagesAvailable() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		// "deu" is mapped to "de", which is activated in the target node
		Form form = createTypedForm(folder, MAPPED_TYPE, "Mapped Form", "deu");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponseOK(response);

		assertThat(getFormNodeId(form)).as("Node of moved form").isEqualTo(germanNode.getId());
	}

	/**
	 * Test moving a folder containing a form of a type with language mapping, where a mapped language is missing in the target node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithMappedFormLanguageMissing() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		// "eng" is mapped to "en", which is not activated in the target node
		Form form = createTypedForm(folder, MAPPED_TYPE, "Mapped Form", "deu", "eng");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		// the message contains the name of the internal language
		assertMessageContains(response, "Mapped Form", englishName);

		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	/**
	 * Test that the language mapping of a form type is not applied to forms of other types
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithUnmappedExternalLanguageCode() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		// the generic type has no language mapping, so "deu" is not known in the target node
		Form form = createForm(folder, "Generic Form", "deu");

		GenericResponse response = moveFolders(germanNodeFolder, folder);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Generic Form", "deu");

		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	/**
	 * Test moving a folder containing a form with missing languages within the same node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderWithFormWithinNode() throws NodeException {
		Folder folder = supply(() -> createFolder(nodeFolder, "Forms"));
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));
		Form form = createForm(folder, "Unknown Language Form", "de", "xx");

		GenericResponse response = moveFolders(target, folder);
		assertResponseOK(response);

		int motherId = supply(t -> t.getObject(Folder.class, folder.getId()).getMother().getId());
		assertThat(motherId).as("Mother of moved folder").isEqualTo(target.getId());
		assertThat(getFormNodeId(form)).as("Node of form").isEqualTo(node.getId());
	}

	// ==================== Moving pages ====================

	/**
	 * Test moving a page with a linked form (in a content tag) into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithLinkedFormToOtherNode() throws NodeException {
		Page page = createPageWithLinkedForm(nodeFolder, "Page");

		GenericResponse response = movePages(allLanguagesNodeFolder, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Page", FORM_TAG);

		assertThat(getPageFolderId(page)).as("Folder of page").isEqualTo(getFolderId(nodeFolder));
	}

	/**
	 * Test moving a page with a linked form (in an object tag) into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithFormInObjectTagToOtherNode() throws NodeException {
		Form form = createForm(nodeFolder, "Test Form", "de", "en");
		Page page = supply(() -> update(createPage(nodeFolder, template, "Page"), upd -> {
			upd.getObjectTag(FORM_OBJECT_PROPERTY).setEnabled(true);
			getPartType(CmsFormPartType.class, upd.getObjectTag(FORM_OBJECT_PROPERTY), FORM_TAG).setTarget(form);
		}));

		GenericResponse response = movePages(allLanguagesNodeFolder, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Page", "object." + FORM_OBJECT_PROPERTY);

		assertThat(getPageFolderId(page)).as("Folder of page").isEqualTo(getFolderId(nodeFolder));
	}

	/**
	 * Test moving a page with a linked form within the same node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithLinkedFormWithinNode() throws NodeException {
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));
		Page page = createPageWithLinkedForm(nodeFolder, "Page");

		GenericResponse response = movePages(target, page);
		assertResponseOK(response);

		assertThat(getPageFolderId(page)).as("Folder of moved page").isEqualTo(target.getId());
	}

	/**
	 * Test moving a page with a form tag, which does not link to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithoutLinkedFormToOtherNode() throws NodeException {
		Page page = supply(() -> createPage(nodeFolder, template, "Page"));

		GenericResponse response = movePages(allLanguagesNodeFolder, page);
		assertResponseOK(response);

		assertThat(getPageFolderId(page)).as("Folder of moved page").isEqualTo(getFolderId(allLanguagesNodeFolder));
	}

	/**
	 * Test moving only a single language variant (without linked form) into another node, while another language variant links to a form
	 * @throws NodeException
	 */
	@Test
	public void testMoveSingleLanguageWithoutLinkedFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Page englishPage = createLanguageVariantWithLinkedForm(germanPage, "en");

		GenericResponse response = movePages(allLanguagesNodeFolder, false, germanPage);
		assertResponseOK(response);

		assertThat(getPageFolderId(germanPage)).as("Folder of moved german page").isEqualTo(getFolderId(allLanguagesNodeFolder));
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(getFolderId(nodeFolder));
	}

	/**
	 * Test moving only a single language variant, which links to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testMoveSingleLanguageWithLinkedFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Page englishPage = createLanguageVariantWithLinkedForm(germanPage, "en");

		GenericResponse response = movePages(allLanguagesNodeFolder, false, englishPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		int sourceFolderId = getFolderId(nodeFolder);
		assertThat(getPageFolderId(germanPage)).as("Folder of german page").isEqualTo(sourceFolderId);
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(sourceFolderId);
	}

	/**
	 * Test moving multiple pages into another node, where one of the pages has a linked form
	 * @throws NodeException
	 */
	@Test
	public void testMoveMultiplePagesOneWithLinkedFormToOtherNode() throws NodeException {
		Page okPage = supply(() -> createPage(nodeFolder, template, "OkPage"));
		Page formPage = createPageWithLinkedForm(nodeFolder, "FormPage");

		GenericResponse response = movePages(allLanguagesNodeFolder, okPage, formPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		// the whole request is rolled back, so none of the pages is moved
		int sourceFolderId = getFolderId(nodeFolder);
		assertThat(getPageFolderId(okPage)).as("Folder of page without form").isEqualTo(sourceFolderId);
		assertThat(getPageFolderId(formPage)).as("Folder of page with form").isEqualTo(sourceFolderId);
	}

	/**
	 * Test moving a page into another node, where only a language variant has a linked form
	 * @throws NodeException
	 */
	@Test
	public void testMovePageWithLanguageVariantWithLinkedFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		Form form = createForm(nodeFolder, "Test Form", "de", "en");
		Page englishPage = supply(t -> {
			Page variant = (Page) t.getObject(Page.class, germanPage.getId()).copy();
			variant.setContentsetId(germanPage.getContentsetId());
			variant.setLanguage(getLanguage("en"));
			variant.setFilename("page_en.html");
			getPartType(CmsFormPartType.class, variant.getContentTag(FORM_TAG), FORM_TAG).setTarget(form);
			variant.save();
			return t.getObject(Page.class, variant.getId());
		});

		// moving all languages fails
		GenericResponse response = movePages(allLanguagesNodeFolder, germanPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		int sourceFolderId = getFolderId(nodeFolder);
		assertThat(getPageFolderId(germanPage)).as("Folder of german page").isEqualTo(sourceFolderId);
		assertThat(getPageFolderId(englishPage)).as("Folder of english page").isEqualTo(sourceFolderId);
	}

	// ==================== Copying pages ====================

	/**
	 * Test copying a page with a linked form into another node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLinkedFormToOtherNode() throws NodeException {
		Page page = createPageWithLinkedForm(nodeFolder, "Page");

		PageCopyResponse response = copyPages(allLanguagesNodeFolder, page);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "Page", FORM_TAG);

		assertThat(getPageCount(allLanguagesNodeFolder)).as("Pages in target folder").isEqualTo(0);
	}

	/**
	 * Test copying a page with a linked form within the same node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLinkedFormWithinNode() throws NodeException {
		Folder target = supply(() -> createFolder(nodeFolder, "Target"));
		Page page = createPageWithLinkedForm(nodeFolder, "Page");

		PageCopyResponse response = copyPages(target, page);
		assertResponseOK(response);

		assertThat(getPageCount(target)).as("Pages in target folder").isEqualTo(1);
	}

	/**
	 * Test copying a page with a form tag, which does not link to a form, into another node
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithoutLinkedFormToOtherNode() throws NodeException {
		Page page = supply(() -> createPage(nodeFolder, template, "Page"));

		PageCopyResponse response = copyPages(allLanguagesNodeFolder, page);
		assertResponseOK(response);

		assertThat(getPageCount(allLanguagesNodeFolder)).as("Pages in target folder").isEqualTo(1);
	}

	/**
	 * Test copying a page into another node, where only a language variant links to a form.
	 * The language variants are copied as well, so the copy must fail.
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLanguageVariantWithLinkedFormToOtherNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		createLanguageVariantWithLinkedForm(germanPage, "en");

		PageCopyResponse response = copyPages(allLanguagesNodeFolder, germanPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, FORM_TAG);

		assertThat(getPageCount(allLanguagesNodeFolder)).as("Pages in target folder").isEqualTo(0);
	}

	/**
	 * Test copying a page into another node, where a language variant links to a form, but the language of that variant
	 * is not activated in the target node. That variant is not copied, so the copy succeeds.
	 * @throws NodeException
	 */
	@Test
	public void testCopyPageWithLanguageVariantWithLinkedFormInLanguageMissingInTargetNode() throws NodeException {
		Page germanPage = supply(() -> createPage(nodeFolder, template, "Page", null, getLanguage("de")));
		createLanguageVariantWithLinkedForm(germanPage, "en");

		PageCopyResponse response = copyPages(germanNodeFolder, germanPage);
		assertResponseOK(response);

		assertThat(getPageCount(germanNodeFolder)).as("Pages in target folder").isEqualTo(1);
	}

	/**
	 * Test copying multiple pages into another node, where one of the pages has a linked form
	 * @throws NodeException
	 */
	@Test
	public void testCopyMultiplePagesOneWithLinkedFormToOtherNode() throws NodeException {
		Page okPage = supply(() -> createPage(nodeFolder, template, "OkPage"));
		Page formPage = createPageWithLinkedForm(nodeFolder, "FormPage");

		PageCopyResponse response = copyPages(allLanguagesNodeFolder, okPage, formPage);
		assertResponse(response, ResponseCode.FAILURE);
		assertMessageContains(response, "FormPage", FORM_TAG);

		// the whole request is rolled back, so no copies are created
		assertThat(getPageCount(allLanguagesNodeFolder)).as("Pages in target folder").isEqualTo(0);
	}

	// ==================== Helpers ====================

	/**
	 * Create a form
	 * @param folder folder
	 * @param name form name
	 * @param languages form languages
	 * @return form
	 * @throws NodeException
	 */
	protected Form createForm(Folder folder, String name, String... languages) throws NodeException {
		return createTypedForm(folder, FormTypeConfigurationModifier.GENERIC_TYPE, name, languages);
	}

	/**
	 * Create a form of the given type
	 * @param folder folder
	 * @param formType form type
	 * @param name form name
	 * @param languages form languages
	 * @return form
	 * @throws NodeException
	 */
	protected Form createTypedForm(Folder folder, String formType, String name, String... languages) throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(formType);
			f.setName(name);
			f.setLanguages(Arrays.asList(languages));
			f.setFolderId(folder.getId());
		}));
		consume(Form::unlock, form);
		return form;
	}

	/**
	 * Create a language variant of the given page, which links to a new form in its content tag
	 * @param page page
	 * @param language language code of the variant
	 * @return language variant
	 * @throws NodeException
	 */
	protected Page createLanguageVariantWithLinkedForm(Page page, String language) throws NodeException {
		Form form = createForm(nodeFolder, "Variant Form " + language, "de", "en");
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
	 * Create a page, which links to a new form in its content tag
	 * @param folder folder
	 * @param name page name
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPageWithLinkedForm(Folder folder, String name) throws NodeException {
		Form form = createForm(folder, name + " Form", "de", "en");
		return supply(() -> update(createPage(folder, template, name), upd -> {
			getPartType(CmsFormPartType.class, upd.getContentTag(FORM_TAG), FORM_TAG).setTarget(form);
		}));
	}

	/**
	 * Move the given folders via the REST API
	 * @param target target folder
	 * @param folders folders to move
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse moveFolders(Folder target, Folder... folders) throws NodeException {
		return supply(systemUser, () -> {
			try {
				if (folders.length == 1) {
					FolderMoveRequest request = new FolderMoveRequest();
					request.setFolderId(target.getId());
					request.setForegroundTime(600);
					return getFolderResource().move(Integer.toString(folders[0].getId()), request);
				} else {
					MultiFolderMoveRequest request = new MultiFolderMoveRequest();
					request.setFolderId(target.getId());
					request.setForegroundTime(600);
					request.setIds(Arrays.stream(folders).map(f -> Integer.toString(f.getId())).collect(Collectors.toCollection(HashSet::new)));
					return getFolderResource().move(request);
				}
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
	}

	/**
	 * Move the given pages (all languages) via the REST API
	 * @param target target folder
	 * @param pages pages to move
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse movePages(Folder target, Page... pages) throws NodeException {
		return movePages(target, true, pages);
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

	/**
	 * Assert that none of the response messages contains the given fragment
	 * @param response response
	 * @param fragment fragment
	 */
	protected void assertMessageNotContains(GenericResponse response, String fragment) {
		List<String> messages = response.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());
		assertThat(messages).as("Response messages").noneSatisfy(message -> assertThat(message).contains(fragment));
	}

	protected int getFolderId(Folder folder) {
		return folder.getId();
	}

	protected int getFolderNodeId(Folder folder) throws NodeException {
		return supply(t -> t.getObject(Folder.class, folder.getId()).getOwningNode().getId());
	}

	protected int getFormNodeId(Form form) throws NodeException {
		return supply(t -> {
			try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				return t.getObject(Form.class, form.getId()).getFolder().getOwningNode().getId();
			}
		});
	}

	protected int getPageFolderId(Page page) throws NodeException {
		return supply(t -> t.getObject(Page.class, page.getId()).getFolder().getId());
	}

	protected int getPageCount(Folder folder) throws NodeException {
		return supply(t -> t.getObject(Folder.class, folder.getId()).getPages().size());
	}
}
