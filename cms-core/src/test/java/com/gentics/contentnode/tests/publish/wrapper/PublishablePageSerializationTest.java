package com.gentics.contentnode.tests.publish.wrapper;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.publish.wrapper.PublishableTemplate;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.PUBLISH_CACHE })
public class PublishablePageSerializationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Integer constructId;
	private static Integer selectConstructId;
	private static Template template;
	private static Page page;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = supply(() -> createNode());

		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "short", "text"));
		selectConstructId = supply(() -> createConstruct(node, SingleSelectPartType.class, "select", "ds"));
		operate(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Test Property", "testproperty"));
		operate(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, selectConstructId, "Test Property 2", "selectproperty"));
		operate(() -> createObjectPropertyDefinition(Template.TYPE_TEMPLATE, constructId, "Test Property", "testproperty"));
		operate(() -> createObjectPropertyDefinition(Template.TYPE_TEMPLATE, selectConstructId, "Test Property 2", "selectproperty"));
		template = create(Template.class, tpl -> {
			tpl.setFolderId(node.getFolder().getId());
			tpl.setName("Template");
			tpl.setSource("<node content>");
			tpl.getTemplateTags().put("content", create(TemplateTag.class, tTag -> {
				tTag.setConstructId(constructId);
				tTag.setEnabled(true);
				tTag.setName("content");
				tTag.setPublic(true);
			}, false));
		});
		page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
		});
	}

	@Test
	public void testPage() throws NodeException, IOException, ClassNotFoundException {
		PublishablePage publishablePage = supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH)) {
				return PublishablePage.getInstance(page.getId(), true);
			}
		});
		byte[] data = serialize(publishablePage);

		PublishablePage deserializedPage = deserialize(data);

		assertThat((Page) deserializedPage).as("Deserialized page").usingRecursiveComparison().usingOverriddenEquals()
				.ignoringFields("logger", "info").isEqualTo(publishablePage);
	}

	@Test
	public void testTemplate() throws NodeException, IOException, ClassNotFoundException {
		PublishableTemplate publishableTemplate = supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH)) {
				return PublishableTemplate.getInstance(template.getId());
			}
		});

		byte[] data = serialize(publishableTemplate);

		PublishableTemplate deserializedTemplate = deserialize(data);

		assertThat((Template) deserializedTemplate).as("Deserialized template").usingRecursiveComparison()
				.usingOverriddenEquals().ignoringFields("logger", "info").isEqualTo(deserializedTemplate);

	}

	/**
	 * Serialize object
	 * @param object object to serialize
	 * @return serialized data
	 * @throws IOException
	 */
	protected <T> byte[] serialize(T object) throws IOException {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
			out.writeObject(object);
			return byteOut.toByteArray();
		}
	}

	/**
	 * Deserialize object
	 * @param data serialized data
	 * @return object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	protected <T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(byteIn)) {
			return (T) in.readObject();
		}
	}
}
