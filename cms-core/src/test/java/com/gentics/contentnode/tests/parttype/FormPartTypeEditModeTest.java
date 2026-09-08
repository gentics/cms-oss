package com.gentics.contentnode.tests.parttype;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.FormPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for rendering {@link FormPartType} and {@link CmsFormPartType}.
 *
 * <p>In {@link RenderType#EM_ALOHA} (the page is actually being edited), both part types must
 * render a placeholder, since forms cannot be sensibly displayed while Aloha Editor is active.
 * In every other render mode (e.g. the read-only {@link RenderType#EM_ALOHA_READONLY} preview or
 * {@link RenderType#EM_PUBLISH}), the real form reference (UUID resp. form ID) must be rendered,
 * so that it can be turned into the actual form by the portal.</p>
 */
public class FormPartTypeEditModeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private final static String PART_KEYNAME = "part";

	private final static String formUuid = UUID.randomUUID().toString();

	private static Page page;

	private static Form form;

	private static String formTagName;

	private static String cmsFormTagName;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		Node node = supply(() -> createNode());
		int formConstructId = supply(() -> createConstruct(node, FormPartType.class, "form", PART_KEYNAME));
		int cmsFormConstructId = supply(() -> createConstruct(node, CmsFormPartType.class, "cmsform", PART_KEYNAME));
		Template template = supply(() -> createTemplate(node.getFolder(), "Template"));

		form = supply(() -> create(Form.class, f -> {
			f.setName("Testform");
			f.setFormType("generic");
			f.setFolderId(node.getFolder().getId());
		}));

		page = supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Test page");

			ContentTag formTag = p.getContent().addContentTag(formConstructId);
			((FormPartType) formTag.getValues().getByKeyname(PART_KEYNAME).getPartType()).setFormUUID(formUuid);
			formTagName = formTag.getName();

			ContentTag cmsFormTag = p.getContent().addContentTag(cmsFormConstructId);
			((CmsFormPartType) cmsFormTag.getValues().getByKeyname(PART_KEYNAME).getPartType()).setTarget(form);
			cmsFormTagName = cmsFormTag.getName();
		}));
	}

	@Test
	public void testFormPartTypeEditMode() throws NodeException {
		assertThat(renderFormTag(RenderType.EM_ALOHA)).as("Rendered form part in edit mode")
				.isEqualTo(new CNI18nString("form.editmode.placeholder").toString());
	}

	@Test
	public void testFormPartTypeReadOnlyMode() throws NodeException {
		assertThat(renderFormTag(RenderType.EM_ALOHA_READONLY)).as("Rendered form part in preview mode")
				.isEqualTo(formUuid);
	}

	@Test
	public void testFormPartTypePublishMode() throws NodeException {
		assertThat(renderFormTag(RenderType.EM_PUBLISH)).as("Rendered form part when publishing")
				.isEqualTo(formUuid);
	}

	@Test
	public void testCmsFormPartTypeEditMode() throws NodeException {
		assertThat(renderCmsFormTag(RenderType.EM_ALOHA)).as("Rendered cms form part in edit mode")
				.isEqualTo(new CNI18nString("form.editmode.placeholder").toString());
	}

	@Test
	public void testCmsFormPartTypeReadOnlyMode() throws NodeException {
		assertThat(renderCmsFormTag(RenderType.EM_ALOHA_READONLY)).as("Rendered cms form part in preview mode")
				.isEqualTo(Integer.toString(form.getId()));
	}

	@Test
	public void testCmsFormPartTypePublishMode() throws NodeException {
		assertThat(renderCmsFormTag(RenderType.EM_PUBLISH)).as("Rendered cms form part when publishing")
				.isEqualTo(Integer.toString(form.getId()));
	}

	/**
	 * Render the {@link FormPartType} tag in the given edit mode
	 * @param editMode edit mode to render in
	 * @return rendered value
	 * @throws NodeException
	 */
	private String renderFormTag(int editMode) throws NodeException {
		return supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(editMode)) {
				FormPartType partType = (FormPartType) page.getContentTag(formTagName).getValues().getByKeyname(PART_KEYNAME)
						.getPartType();
				return partType.render(new RenderResult(), null);
			}
		});
	}

	/**
	 * Render the {@link CmsFormPartType} tag in the given edit mode
	 * @param editMode edit mode to render in
	 * @return rendered value
	 * @throws NodeException
	 */
	private String renderCmsFormTag(int editMode) throws NodeException {
		return supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(editMode)) {
				CmsFormPartType partType = (CmsFormPartType) page.getContentTag(cmsFormTagName).getValues().getByKeyname(PART_KEYNAME)
						.getPartType();
				return partType.render(new RenderResult(), null);
			}
		});
	}
}
