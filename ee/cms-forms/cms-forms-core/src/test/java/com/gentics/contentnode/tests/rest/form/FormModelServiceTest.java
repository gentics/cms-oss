package com.gentics.contentnode.tests.rest.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.PublishableContentItem;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.util.FormModelService;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for transforming forms into the REST Model with {@link ModelBuilder#getForm(Form)}, which uses the {@link FormModelService}
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormModelServiceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Test Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		consume(n -> {
			n.activateFeature(Feature.FORMS);
		}, node);
	}

	/**
	 * Test that {@link ModelBuilder#getForm(Form)} returns the same REST Model as {@link FormResourceImpl#TRANSFORM2REST}
	 * @throws NodeException
	 */
	@Test
	public void testGetForm() throws NodeException {
		Form form = supply(() -> create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setFolderId(node.getFolder().getId());
			f.setName("Test Form");
			f.setDescription("Test Description");
		}));

		PublishableContentItem model = supply(() -> ModelBuilder.getForm(form));
		FormModel expected = supply(() -> FormResourceImpl.TRANSFORM2REST.apply(form));

		assertThat(model).as("Form model").isInstanceOf(FormModel.class).usingRecursiveComparison().isEqualTo(expected);
		assertThat(((FormModel) model).getName()).as("Form name").isEqualTo("Test Form");
	}
}
