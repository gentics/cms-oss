package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.LinkRequest;
import com.gentics.contentnode.rest.model.request.MultiLinkRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for linking and unlinking templates using the REST API
 */
@RunWith(value = Parameterized.class)
public class TemplateLinkSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	/**
	 * Test data
	 */
	protected Node node;

	protected Folder folder_1;
	protected Folder folder_2;
	protected Folder folder_1_1;
	protected Folder folder_1_2;
	protected Folder folder_2_1;
	protected Folder folder_2_2;
	protected Template template_1;
	protected Template template_2;

	/**
	 * True to handle multiple templates at once, false to handle only a single template
	 */
	@Parameter(0)
	public boolean multiTemplate;

	/**
	 * True to handle multiple folders at once, false to handle only a single folder
	 */
	@Parameter(1)
	public boolean multiFolder;

	/**
	 * True to handle folders recursively
	 */
	@Parameter(2)
	public boolean recursive;

	/**
	 * True to handle the root folder, false for handling only non-root folders
	 */
	@Parameter(3)
	public boolean rootFolder;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: multiTemplate {0}, multiFolder {1}, recursive {2}, rootFolder {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean multiTemplate : Arrays.asList(true, false)) {
			for (boolean multiFolder : Arrays.asList(true, false)) {
				for (boolean recursive : Arrays.asList(true, false)) {
					for (boolean rootFolder : Arrays.asList(true, false)) {
						data.add(new Object[] {multiTemplate, multiFolder, recursive, rootFolder});
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Setup test data.
	 */
	@Before
	public void setUp() throws Exception {

		// create a test node
		node = supply(() -> createNode("test", "test", "/", null, false, false));

		// create some folders
		folder_1 = supply(() -> createFolder(node.getFolder(), "Folder 1"));
		folder_1_1 = supply(() -> createFolder(folder_1, "Folder 1.1"));
		folder_1_2 = supply(() -> createFolder(folder_1, "Folder 1.2"));
		folder_2 = supply(() -> createFolder(node.getFolder(), "Folder 2"));
		folder_2_1 = supply(() -> createFolder(folder_2, "Folder 2.1"));
		folder_2_2 = supply(() -> createFolder(folder_2, "Folder 2.2"));

		// create templates and link to the root folder
		template_1 = supply(() -> createTemplate("Template 1", "Template source", node.getFolder()));
		template_2 = supply(() -> createTemplate("Template 2", "Template source", node.getFolder()));
	}

	/**
	 * Test linking templates to folders
	 * @throws Exception
	 */
	@Test
	public void testLink() throws Exception {
		SystemUser user = supply(t -> t.getObject(SystemUser.class, 1));

		operate(user, () -> {
			// do the link request (according to the test setup)
			TemplateResourceImpl res = new TemplateResourceImpl();
			Set<String> folderIds = new HashSet<String>();
			GenericResponse response = null;
			if (rootFolder) {
				folderIds.add(ObjectTransformer.getString(node.getFolder().getId(), ""));
			} else {
				folderIds.add(ObjectTransformer.getString(folder_1.getId(), ""));
			}
			if (multiFolder) {
				folderIds.add(ObjectTransformer.getString(folder_2.getId(), ""));
			}
			if (multiTemplate) {
				MultiLinkRequest req = new MultiLinkRequest();
				req.setTemplateIds(new HashSet<String>(Arrays.asList(ObjectTransformer.getString(template_1.getId(), ""),
						ObjectTransformer.getString(template_2.getId(), ""))));
				req.setFolderIds(folderIds);
				req.setRecursive(recursive);

				response = res.link(req);
			} else {
				LinkRequest req = new LinkRequest();
				req.setFolderIds(folderIds);
				req.setRecursive(recursive);

				response = res.link(ObjectTransformer.getString(template_1.getId(), ""), req);
			}

			// check that the request succeeded and that the result is as expected
			assertResponse(response);
		});

		// we need to fetch the templates again, since data has changed
		template_1 = execute(Template::reload, template_1);
		template_2 = execute(Template::reload, template_2);

		// determine the expected folders for template 1 and check whether the template
		// is linked to exactly those folders
		operate(() -> {
			List<Folder> template1Folders = new ArrayList<Folder>();
			template1Folders.add(node.getFolder());
			if (!rootFolder || recursive) {
				template1Folders.add(folder_1);
			}
			if (recursive) {
				template1Folders.add(folder_1_1);
				template1Folders.add(folder_1_2);
			}
			if (multiFolder || (rootFolder && recursive)) {
				template1Folders.add(folder_2);
				if (recursive) {
					template1Folders.add(folder_2_1);
					template1Folders.add(folder_2_2);
				}
			}
			assertTemplateLinking(template_1, template1Folders);

			// determine the expected folders for template 2 and check whether the template
			// is linked to exactly those folders
			List<Folder> template2Folders = new ArrayList<Folder>();
			template2Folders.add(node.getFolder());
			if (multiTemplate) {
				if (!rootFolder || recursive) {
					template2Folders.add(folder_1);
				}
				if (recursive) {
					template2Folders.add(folder_1_1);
					template2Folders.add(folder_1_2);
				}
				if (multiFolder || (rootFolder && recursive)) {
					template2Folders.add(folder_2);
					if (recursive) {
						template2Folders.add(folder_2_1);
						template2Folders.add(folder_2_2);
					}
				}
			}
			assertTemplateLinking(template_2, template2Folders);
		});
	}

	/**
	 * Test unlinking templates from folders
	 * @throws Exception
	 */
	@Test
	public void testUnlink() throws Exception {
		// prepare by linking templates to all folders
		template_1 = update(template_1, tmpl -> {
			tmpl.getFolders().addAll(Arrays.asList(folder_1, folder_1_1, folder_1_2, folder_2, folder_2_1, folder_2_2));
		});
		template_2 = update(template_2, tmpl -> {
			tmpl.getFolders().addAll(Arrays.asList(folder_1, folder_1_1, folder_1_2, folder_2, folder_2_1, folder_2_2));
		});

		SystemUser user = supply(t -> t.getObject(SystemUser.class, 1));

		operate(user, () -> {
			if (rootFolder && recursive) {
				// when we tried to unlink from the root folder recursively (meaning: from all folders)
				// the request is expected to fail, because the template(s) cannot be unlinked from all
				// its folders
				exceptionChecker.expect(EntityInUseException.class);
			}

			// do the unlink request (according to the test setup)
			TemplateResourceImpl res = new TemplateResourceImpl();
			Set<String> folderIds = new HashSet<String>();
			GenericResponse response = null;
			if (rootFolder) {
				folderIds.add(ObjectTransformer.getString(node.getFolder().getId(), ""));
			} else {
				folderIds.add(ObjectTransformer.getString(folder_1.getId(), ""));
			}
			if (multiFolder) {
				folderIds.add(ObjectTransformer.getString(folder_2.getId(), ""));
			}
			if (multiTemplate) {
				MultiLinkRequest req = new MultiLinkRequest();
				req.setTemplateIds(new HashSet<String>(Arrays.asList(ObjectTransformer.getString(template_1.getId(), ""),
						ObjectTransformer.getString(template_2.getId(), ""))));
				req.setFolderIds(folderIds);
				req.setRecursive(recursive);

				response = res.unlink(req);
			} else {
				LinkRequest req = new LinkRequest();
				req.setFolderIds(folderIds);
				req.setRecursive(recursive);

				response = res.unlink(ObjectTransformer.getString(template_1.getId(), ""), req);
			}

			assertResponse(response);
		});

		template_1 = execute(Template::reload, template_1);
		template_2 = execute(Template::reload, template_2);

		// determine the expected folders for template 1 and check whether the template
		// is linked to exactly those folders
		operate(() -> {
			List<Folder> template1Folders = new ArrayList<Folder>(
					Arrays.asList(node.getFolder(), folder_1, folder_1_1, folder_1_2, folder_2, folder_2_1, folder_2_2));
			if (rootFolder && !recursive) {
				template1Folders.remove(node.getFolder());
				if (multiFolder) {
					template1Folders.remove(folder_2);
				}
			} else if (!rootFolder) {
				template1Folders.remove(folder_1);
				if (recursive) {
					template1Folders.removeAll(Arrays.asList(folder_1_1, folder_1_2));
				}
				if (multiFolder) {
					template1Folders.remove(folder_2);
					if (recursive) {
						template1Folders.removeAll(Arrays.asList(folder_2_1, folder_2_2));
					}
				}
			}
			assertTemplateLinking(template_1, template1Folders);

			// determine the expected folders for template 2 and check whether the template
			// is linked to exactly those folders
			List<Folder> template2Folders = new ArrayList<Folder>(
					Arrays.asList(node.getFolder(), folder_1, folder_1_1, folder_1_2, folder_2, folder_2_1, folder_2_2));
			if (multiTemplate) {
				if (rootFolder && !recursive) {
					template2Folders.remove(node.getFolder());
					if (multiFolder) {
						template2Folders.remove(folder_2);
					}
				} else if (!rootFolder) {
					template2Folders.remove(folder_1);
					if (recursive) {
						template2Folders.removeAll(Arrays.asList(folder_1_1, folder_1_2));
					}
					if (multiFolder) {
						template2Folders.remove(folder_2);
						if (recursive) {
							template2Folders.removeAll(Arrays.asList(folder_2_1, folder_2_2));
						}
					}
				}
			}
			assertTemplateLinking(template_2, template2Folders);
		});
	}

	/**
	 * Create a new folder
	 * @param mother mother folder
	 * @param name name
	 * @return new folder
	 * @throws NodeException
	 */
	protected Folder createFolder(Folder mother, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder newFolder = t.createObject(Folder.class);
		newFolder.setMotherId(mother.getId());
		newFolder.setName(name);
		newFolder.setPublishDir("/");
		newFolder.save();

		t.commit(false);
		return t.getObject(Folder.class, newFolder.getId());
	}

	/**
	 * Create a new template
	 * @param name name
	 * @param source source
	 * @param folder folder
	 * @return new template
	 * @throws NodeException
	 */
	protected Template createTemplate(String name, String source, Folder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Template newTemplate = t.createObject(Template.class);
		newTemplate.setName(name);
		newTemplate.setSource(source);
		newTemplate.getFolders().add(folder);
		newTemplate.save();
		t.commit(false);

		return newTemplate;
	}

	/**
	 * Assert that the response has {@link ResponseCode#OK}
	 * @param response response
	 */
	protected void assertResponse(GenericResponse response) {
		assertResponse(response, true);
	}

	/**
	 * Assert whether the response succeeded or failed (depending on expectSuccess flag)
	 * @param response response to check
	 * @param expectSuccess true if success is expected, false if failure is expected
	 */
	protected void assertResponse(GenericResponse response, boolean expectSuccess) {
		assertNotNull("Response was null", response);
		ResponseInfo info = response.getResponseInfo();
		assertNotNull("Response contained no ResponseInfo", info);
		if (expectSuccess) {
			assertTrue("Response failed with message " + info.getResponseMessage(), info.getResponseCode() == ResponseCode.OK);
		} else {
			assertFalse("Response succeeded but was expected to fail", info.getResponseCode() == ResponseCode.OK);
		}
	}

	/**
	 * Assert that the given template is linked exactly to the given list of folders
	 * @param template template
	 * @param folders list of folders
	 * @throws NodeException
	 */
	protected void assertTemplateLinking(Template template, List<Folder> folders) throws NodeException {
		List<Folder> linkedToFolders = template.getFolders();

		for (Folder linkedFolder : linkedToFolders) {
			assertTrue("Template is linked to folder " + linkedFolder, folders.contains(linkedFolder));
		}

		for (Folder expectedFolder : folders) {
			assertTrue("Template is not linked to folder " + expectedFolder, linkedToFolders.contains(expectedFolder));
		}
	}
}
