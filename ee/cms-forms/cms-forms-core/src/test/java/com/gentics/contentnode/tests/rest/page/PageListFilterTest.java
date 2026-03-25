package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for filtering page lists
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
@RunWith(Parameterized.class)
public class PageListFilterTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	protected static Node node;

	protected static Template normalTemplate;

	protected static Template formsEmailTemplate;

	@Parameter(0)
	public boolean multichannelling;

	@Parameters(name = "{index}: multichannelling {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean multichannelling : Arrays.asList(true, false)) {
			data.add(new Object[] { multichannelling });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		node = supply(() -> createNode(Feature.FORMS));

		normalTemplate = create(Template.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setName("Normal Template");
			create.setSource("");
			create.setMlId(1);
		}).build();

		formsEmailTemplate = create(Template.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setName("Forms E-Mail Template");
			create.setSource("");
			create.setMlId(21);
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences()
				.setFeature(Feature.MULTICHANNELLING, multichannelling);
	}

	/**
	 * Test filtering for mlIds
	 * @throws NodeException
	 */
	@Test
	public void testMlFilter() throws NodeException {
		Folder folder1 = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder 1");
		}).build();

		Page normalPageInFolder1 = create(Page.class, create -> {
			create.setFolderId(folder1.getId());
			create.setTemplateId(normalTemplate.getId());
		}).build();

		Page formsEmailPageInFolder1 = create(Page.class, create -> {
			create.setFolderId(folder1.getId());
			create.setTemplateId(formsEmailTemplate.getId());
		}).build();

		Folder folder2 = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder 2");
		}).build();

		Page normalPageInFolder2 = create(Page.class, create -> {
			create.setFolderId(folder2.getId());
			create.setTemplateId(normalTemplate.getId());
		}).build();

		Page formsEmailPageInFolder2 = create(Page.class, create -> {
			create.setFolderId(folder2.getId());
			create.setTemplateId(formsEmailTemplate.getId());
		}).build();

		// get all pages
		PageListResponse response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())).setRecursive(true),
				new PageListParameterBean(), new FilterParameterBean(), new SortParameterBean(),
				new PagingParameterBean(), new PublishableParameterBean(), new WastebinParameterBean()));
		assertResponseOK(response);
		assertThat(response.getItems().stream().map(com.gentics.contentnode.rest.model.Page::getId))
				.as("Returned pages").containsOnly(normalPageInFolder1.getId(), formsEmailPageInFolder1.getId(),
						normalPageInFolder2.getId(), formsEmailPageInFolder2.getId());

		// include ml's
		response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())).setRecursive(true),
				new PageListParameterBean().setIncludeMlIds(Arrays.asList(21)), new FilterParameterBean(), new SortParameterBean(),
				new PagingParameterBean(), new PublishableParameterBean(), new WastebinParameterBean()));
		assertResponseOK(response);
		assertThat(response.getItems().stream().map(com.gentics.contentnode.rest.model.Page::getId))
				.as("Returned pages").containsOnly(formsEmailPageInFolder1.getId(), formsEmailPageInFolder2.getId());

		// exclude ml's
		response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())).setRecursive(true),
				new PageListParameterBean().setExcludeMlIds(Arrays.asList(21)), new FilterParameterBean(), new SortParameterBean(),
				new PagingParameterBean(), new PublishableParameterBean(), new WastebinParameterBean()));
		assertResponseOK(response);
		assertThat(response.getItems().stream().map(com.gentics.contentnode.rest.model.Page::getId))
				.as("Returned pages").containsOnly(normalPageInFolder1.getId(), normalPageInFolder2.getId());
	}
}
