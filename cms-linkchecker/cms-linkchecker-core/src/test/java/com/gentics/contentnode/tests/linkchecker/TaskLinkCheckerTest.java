package com.gentics.contentnode.tests.linkchecker;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.object.Node.TYPE_NODE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_VIEW;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.model.linkchecker.UpdateExternalLinkStatusRequest;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.impl.LinkCheckerResourceImpl;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the virtual tasks of the link checker
 */
@GCNFeature(set = { Feature.LINK_CHECKER })
public class TaskLinkCheckerTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static SystemUser editor;

	@BeforeClass
	public static void setupData() throws Exception {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		consume(n -> n.activateFeature(Feature.LINK_CHECKER), node);

		Construct link = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword(AlohaRenderer.MAGICLINK_KEYWORD);
			c.setName("Aloha Link", 1);
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("url");
				p.setName("URL", 1);
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
			}, false));
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("text");
				p.setName("Text", 1);
				p.setPartTypeId(getPartTypeId(ShortTextPartType.class));
			}, false));
		}));
		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.setName("Template");
			tmpl.setSource("");
			tmpl.getTemplateTags().put("link1", create(TemplateTag.class, tTag -> {
				tTag.setConstructId(link.getId());
				tTag.setEnabled(true);
				tTag.setName("link1");
				tTag.setPublic(true);
			}, false));
		}));

		UserGroup group = supply(() -> createUserGroup("Editors", NODE_GROUP_ID));
		editor = supply(() -> createSystemUser("Task", "Editor", null, "task.editor", "task.editor", Arrays.asList(group)));
		operate(() -> setPermissions(TYPE_NODE, node.getFolder().getId(), Arrays.asList(group),
				new Permission(PERM_VIEW, PERM_PAGE_VIEW, PERM_PAGE_UPDATE).toString()));
	}

	@Before
	public void clearData() throws NodeException {
		consume(n -> clear(n), node);
		consume(n -> n.activateFeature(Feature.LINK_CHECKER), node);
	}

	/**
	 * Test that a page with a broken link is a task
	 * @throws NodeException
	 */
	@Test
	public void testBrokenLinkIsTask() throws NodeException {
		Page page = pageWithLink(ExternalLinkStatus.invalid);

		List<UserTaskModel> tasks = supply(editor, () -> linkCheckerTasks());
		assertThat(tasks).as("Tasks of the editor").extracting(UserTaskModel::getId)
				.containsExactly("linkChecker:page:%d".formatted(page.getId()));
		assertThat(tasks.get(0).getDetails()).as("Details").containsEntry("brokenCount", 1);
		assertThat(tasks.get(0).getActions()).as("Actions of an offline page").containsExactly(TaskAction.startSession);
	}

	/**
	 * Test that a page without broken links is no task
	 * @throws NodeException
	 */
	@Test
	public void testValidLinkIsNoTask() throws NodeException {
		pageWithLink(ExternalLinkStatus.valid);
		assertThat(supply(editor, () -> linkCheckerTasks())).as("Tasks of the editor").isEmpty();
	}

	/**
	 * Test that there are no tasks without the feature
	 * @throws NodeException
	 */
	@Test
	public void testNoTaskWithoutFeature() throws NodeException {
		pageWithLink(ExternalLinkStatus.invalid);
		consume(n -> n.deactivateFeature(Feature.LINK_CHECKER), node);
		assertThat(supply(editor, () -> linkCheckerTasks())).as("Tasks of the editor").isEmpty();
	}

	/**
	 * Create a page with one external link of the given status
	 */
	private Page pageWithLink(ExternalLinkStatus status) throws NodeException {
		Page page = supply(editor, () -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			getPartType(PageURLPartType.class, p.getContentTag("link1"), "url").setExternalTarget("http://www.gentics.com/a");
			getPartType(ShortTextPartType.class, p.getContentTag("link1"), "text").getValueObject().setValueText("link a");
		}));

		ExternalLink link = supply(() -> new LinkCheckerResourceImpl().getLinks(Integer.toString(page.getId()))).getItems().get(0);
		link.setLastStatus(status);
		consume(l -> new LinkCheckerResourceImpl().updateLinkStatus(Integer.toString(page.getId()),
				new UpdateExternalLinkStatusRequest().setItems(Arrays.asList(l))), link);
		return page;
	}

	private static List<UserTaskModel> linkCheckerTasks() throws NodeException {
		TaskFilterParameterBean filter = new TaskFilterParameterBean();
		filter.source = Set.of(TaskSource.linkChecker);
		return new TaskResourceImpl().list(null, null, null, filter).getItems();
	}
}
