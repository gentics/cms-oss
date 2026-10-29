package com.gentics.contentnode.tests.rest.seo;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for page filename generation depending on node SEO settings
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.PUBLISH_CACHE })
public class PageSEOFilenameTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node masterNode;

	private static Node channel;

	private static Template template;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: omit extension {0}, language code {1}, master {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean omitExtension : Arrays.asList(true, false)) {
			for (PageLanguageCode pageLanguageCode : PageLanguageCode.values()) {
				for (boolean master : Arrays.asList(true, false)) {
					data.add(new Object[] { omitExtension, pageLanguageCode, master });
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		masterNode = supply(() -> createNode("hostname", "Node name", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		channel = supply(() -> createChannel(masterNode, "Channel", "channel", "/"));
		template = supply(() -> createTemplate(masterNode.getFolder(), "Template"));
	}

	protected static String suggestFilename(Folder folder, String pageName, String language) throws NodeException {
		return supply(t -> {
			com.gentics.contentnode.object.Page page = t.createObject(com.gentics.contentnode.object.Page.class);
			page.setFolderId(folder.getId());
			page.setTemplateId(template.getId());
			if (language != null) {
				page.setLanguage(getLanguage(language));
			}
			page.setName(pageName);
			return PageFactory.suggestFilename(page);
		});
	}

	protected static boolean isFilenameAvailable(Folder folder, String fileName, String language, boolean master) throws NodeException {
		return supply(t -> {
			com.gentics.contentnode.object.Page page = t.createObject(com.gentics.contentnode.object.Page.class);
			page.setTemplateId(template.getId());
			page.setFolderId(folder.getId());
			page.setFilename(fileName);
			if (language != null) {
				page.setLanguage(getLanguage(language));
			}
			if (!master) {
				page.setChannelInfo(channel.getId(), null);
			}

			return PageFactory.getFilenameObstructor(page) == null;
		});
	}

	@Parameter(0)
	public boolean omitExtension;

	@Parameter(1)
	public PageLanguageCode pageLanguageCode;

	@Parameter(2)
	public boolean master;

	@Before
	public void setup() throws NodeException {
		masterNode = update(masterNode, n -> {
			n.setOmitPageExtension(omitExtension);
			n.setPageLanguageCode(pageLanguageCode);
		});
	}

	/**
	 * Clean data
	 * @throws NodeException
	 */
	@After
	public void clean() throws NodeException {
		operate(() -> clear(channel));
		operate(() -> clear(masterNode));
	}

	/**
	 * Test autogeneration of filename when page is created
	 * @throws NodeException
	 */
	@Test
	public void testAutogeneratePageCreate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "New-Page.en" : "New-Page.en.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "New-Page" : "New-Page.html";
			break;
		}

		String fileName = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getFileName();
		});
		assertThat(fileName).as("Autogenerated filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test sanitation of requested filename, when page is created
	 * @throws NodeException
	 */
	@Test
	public void testSanitizePageCreate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			// Note: language codes in the filename are not enforced, if the requested filename does not contain them
			expectedFilename = omitExtension ? "new-page" : "new-page.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "new-page" : "new-page.html";
			break;
		}

		String fileName = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setFileName("new page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getFileName();
		});
		assertThat(fileName).as("Sanitized filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test filename, which is made unique, when page is created
	 * @throws NodeException
	 */
	@Test
	public void testSanitizeDuplicatePageCreate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "duplicate1.en" : "duplicate1.en.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "duplicate1" : "duplicate1.html";
			break;
		}

		String otherFilename = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("duplicate")
				);
			assertResponseOK(response);
			return response.getPage().getFileName();
		});

		String fileName = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setFileName(otherFilename)
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getFileName();
		});
		assertThat(fileName).as("Sanitized filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test autogeneration of filename when page is updated
	 * @throws NodeException
	 */
	@Test
	public void testAutogeneratePageUpdate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "Modified-Page.en" : "Modified-Page.en.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "Modified-Page" : "Modified-Page.html";
			break;
		}

		int pageId = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getId();
		});

		String fileName = execute(id -> {
			Page update = new Page();
			update.setName("Modified Page");
			PageSaveRequest request = new PageSaveRequest(update);
			request.setDeriveFileName(true);
			assertResponseOK(getPageResource().save(Integer.toString(id), request));
			return getPageResource().load(Integer.toString(id), false, false, false, false, false, false, false, false, false, false, null, null).getPage().getFileName();
		}, pageId);
		assertThat(fileName).as("Autogenerated filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test sanitation of requested filename, when page is updated
	 * @throws NodeException
	 */
	@Test
	public void testSanitizePageUpdate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			// Note: language codes in the filename are not enforced, if the requested filename does not contain them
			expectedFilename = omitExtension ? "modified-page" : "modified-page.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "modified-page" : "modified-page.html";
			break;
		}

		int pageId = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setFileName("new page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getId();
		});

		String fileName = execute(id -> {
			Page update = new Page();
			update.setName("Modified Page");
			update.setFileName("modified page");
			PageSaveRequest request = new PageSaveRequest(update);
			assertResponseOK(getPageResource().save(Integer.toString(id), request));
			return getPageResource().load(Integer.toString(id), false, false, false, false, false, false, false, false, false, false, null, null).getPage().getFileName();
		}, pageId);
		assertThat(fileName).as("Sanitized filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test filename, which is made unique, when page is updated
	 * @throws NodeException
	 */
	@Test
	public void testSanitizeDuplicatePageUpdate() throws NodeException {
		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "duplicate1.en" : "duplicate1.en.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "duplicate1" : "duplicate1.html";
			break;
		}

		String otherFilename = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("duplicate")
				);
			assertResponseOK(response);
			return response.getPage().getFileName();
		});

		int pageId = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("New Page")
					.setFileName("new page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage().getId();
		});

		String fileName = execute(id -> {
			Page update = new Page();
			update.setName("Modified Page");
			update.setFileName(otherFilename);
			PageSaveRequest request = new PageSaveRequest(update);
			assertResponseOK(getPageResource().save(Integer.toString(id), request));
			return getPageResource().load(Integer.toString(id), false, false, false, false, false, false, false, false, false, false, null, null).getPage().getFileName();
		}, pageId);

		assertThat(fileName).as("Sanitized filename").isEqualTo(expectedFilename);
	}

	/**
	 * Test filename uniqueness when translating pages
	 * @throws NodeException
	 */
	@Test
	public void testUniquenessTranslate() throws NodeException {
		// create a page
		Page page = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("Page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage();
		});

		// translate page
		Page translation = execute(id -> {
			PageLoadResponse response = getPageResource().translate(id, "de", false, null);
			assertResponseOK(response);
			return response.getPage();
		}, page.getId());

		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "Page.de" : "Page.de.html";
			break;
		case NONE:
			expectedFilename = omitExtension ? "Page1" : "Page1.html";
			break;
		case PATH:
		default:
			expectedFilename = omitExtension ? "Page" : "Page.html";
			break;
		}

		assertThat(translation.getFileName()).as("Filename of the translation").isEqualTo(expectedFilename);
	}

	/**
	 * Test filename uniqueness when updating a language variant
	 * @throws NodeException
	 */
	@Test
	public void testUniquenessUpdateLanguageVariant() throws NodeException {
		// create a page
		Page page = supply(() -> {
			PageLoadResponse response = getPageResource().create(
				new PageCreateRequest()
					.setFolderId(Integer.toString(masterNode.getFolder().getId()))
					.setTemplateId(template.getId())
					.setLanguage("en")
					.setPageName("Page")
					.setNodeId(master ? masterNode.getId() : channel.getId())
				);
			assertResponseOK(response);
			return response.getPage();
		});

		// translate page
		Page translation = execute(id -> {
			PageLoadResponse response = getPageResource().translate(id, "de", false, null);
			assertResponseOK(response);
			return response.getPage();
		}, page.getId());

		// update the page
		String updatedFilename = execute(id -> {
			Page update = new Page();
			update.setName("Modified");
			PageSaveRequest request = new PageSaveRequest(update);
			request.setDeriveFileName(true);
			GenericResponse response = getPageResource().save(String.valueOf(id), request);
			assertResponseOK(response);

			return getPageResource().load(Integer.toString(id), false, false, false, false, false, false, false, false, false, false, null, null).getPage().getFileName();
		}, page.getId());

		String expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "Modified.en" : "Modified.en.html";
			break;
		case NONE:
		case PATH:
		default:
			expectedFilename = omitExtension ? "Modified" : "Modified.html";
			break;
		}

		assertThat(updatedFilename).as("Updated filename of the translation").isEqualTo(expectedFilename);

		// update the translation
		String updatedTranslationFilename = execute(id -> {
			Page update = new Page();
			update.setName("Modified");
			PageSaveRequest request = new PageSaveRequest(update);
			request.setDeriveFileName(true);
			GenericResponse response = getPageResource().save(String.valueOf(id), request);
			assertResponseOK(response);

			return getPageResource().load(Integer.toString(id), false, false, false, false, false, false, false, false, false, false, null, null).getPage().getFileName();
		}, translation.getId());

		expectedFilename = null;
		switch (pageLanguageCode) {
		case FILENAME:
			expectedFilename = omitExtension ? "Modified.de" : "Modified.de.html";
			break;
		case NONE:
			expectedFilename = omitExtension ? "Modified1" : "Modified1.html";
			break;
		case PATH:
		default:
			expectedFilename = omitExtension ? "Modified" : "Modified.html";
			break;
		}

		assertThat(updatedTranslationFilename).as("Updated filename of the translation").isEqualTo(expectedFilename);
	}

	/**
	 * Test uniqueness check with a file obstructor
	 * @throws NodeException
	 */
	@Test
	public void testMasterFileObstructor() throws NodeException {
		Folder folder1 = create(Folder.class, f -> {
			f.setName("Folder 1");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});
		Folder folder2 = create(Folder.class, f -> {
			f.setName("Folder 2");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});
		// create obstructor file
		supply(() -> createFile(folder1, "obstructor.html", "Testdata".getBytes()));

		boolean expectAvailable = false;
		switch (pageLanguageCode) {
		case PATH:
			expectAvailable = true;
			break;
		default:
			expectAvailable = false;
			break;
		}
		assertThat(isFilenameAvailable(folder2, "obstructor.html", "de", master)).as("Filename is available").isEqualTo(expectAvailable);
	}

	/**
	 * Check uniqueness check with a nice url obstructor (nice url does not contain language code)
	 * @throws NodeException
	 */
	@Test
	public void testMasterNiceUrlObstructor1() throws NodeException {
		Folder folder1 = create(Folder.class, f -> {
			f.setName("Folder 1");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});
		Folder folder2 = create(Folder.class, f -> {
			f.setName("Folder 2");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});

		String filename = suggestFilename(folder1, "Page", "de");

		// create page with obstructing nice url
		create(com.gentics.contentnode.object.Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Obstructor Page");
			p.setNiceUrl("/path(test/" + filename);
		});

		boolean expectAvailable = false;
		switch (pageLanguageCode) {
		case PATH:
			expectAvailable = true;
			break;
		default:
			expectAvailable = false;
			break;
		}
		assertThat(isFilenameAvailable(folder2, filename, "de", master)).as("Filename is available").isEqualTo(expectAvailable);
	}

	/**
	 * Check uniqueness check with a nice url obstructor (nice url contains language code)
	 * @throws NodeException
	 */
	@Test
	public void testMasterNiceUrlObstructor2() throws NodeException {
		Folder folder1 = create(Folder.class, f -> {
			f.setName("Folder 1");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});
		Folder folder2 = create(Folder.class, f -> {
			f.setName("Folder 2");
			f.setMotherId(masterNode.getFolder().getId());
			f.setPublishDir("/path(test");
		});

		String filename = suggestFilename(folder1, "Page", "de");

		// create page with obstructing nice url
		create(com.gentics.contentnode.object.Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Obstructor Page");
			p.setNiceUrl("/de/path(test/" + filename);
		});

		boolean expectAvailable = false;
		switch (pageLanguageCode) {
		case PATH:
			expectAvailable = false;
			break;
		default:
			expectAvailable = true;
			break;
		}
		assertThat(isFilenameAvailable(folder2, filename, "de", master)).as("Filename is available").isEqualTo(expectAvailable);
	}

	/**
	 * Test uniqueness with a publish page
	 * @throws NodeException
	 */
	@Test
	public void testFilenameAvailablePublishedPage() throws NodeException {
		Folder folder = supply(() -> masterNode.getFolder());

		// create and publish a page
		com.gentics.contentnode.object.Page publishedPage = update(
				create(com.gentics.contentnode.object.Page.class, p -> {
					TransactionManager.getCurrentTransaction().setTimestamp(1000L);
					p.setFolderId(folder.getId());
					p.setTemplateId(template.getId());
					p.setName("Published Page");
					p.setLanguage(getLanguage("en"));
				}), p -> {
					TransactionManager.getCurrentTransaction().setTimestamp(2000L);
					p.publish();
				});
		String publishedFilename = publishedPage.getFilename();

		// modify the page (change filename)
		publishedPage = execute(p -> {
			com.gentics.contentnode.object.Page update = TransactionManager.getCurrentTransaction().getObject(p, true);
			update.setFilename("other");
			update.save();
			return p.reload();
		}, publishedPage);

		String currentFilename = publishedPage.getFilename();

		boolean expectAvailable = false;
		switch (pageLanguageCode) {
		case PATH:
			expectAvailable = true;
			break;
		default:
			expectAvailable = false;
			break;
		}
		assertThat(isFilenameAvailable(folder, publishedFilename, "de", master)).as("Published filename is available").isEqualTo(expectAvailable);
		assertThat(isFilenameAvailable(folder, currentFilename, "de", master)).as("Current filename is available").isEqualTo(expectAvailable);
	}
}
