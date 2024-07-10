package com.gentics.contentnode.tests.parttype;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Tests concerning the NavigationPartType class
 */
public class NavigationPartTypeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * test whether new and offline pages are removed from the navigation
	 * when listing and counting them
	 * @throws Exception
	 */
	@Test
	public void testOfflinePages() throws Exception {
		// Create navigation construct
		Construct nav = Trx.supply(t -> {
			t.setTimestamp(1000L);
			return create(Construct.class, navconst -> {
				navconst.setName("navconst", 1);
				navconst.setAutoEnable(true);
				navconst.setKeyword("navconst");

				navconst.getParts().add(create(Part.class, navpart -> {
					navpart.setPartTypeId(35); // ID of NavigationPartType
					navpart.setKeyname("nav");
					navpart.setHidden(false);
				}, false));
				navconst.getParts().add(create(Part.class, objectsPart -> {
					objectsPart.setPartTypeId(1); // Normal text
					objectsPart.setKeyname("objects");
					objectsPart.setHidden(true);
					objectsPart.setDefaultValue(create(Value.class, v -> {
						v.setValueText("pages,folders");
					}, false));
				}, false));
				navconst.getParts().add(create(Part.class, templatePart -> {
					templatePart.setPartTypeId(1);
					templatePart.setKeyname("template");
					templatePart.setHidden(true);
					templatePart.setDefaultValue(create(Value.class, v -> {
						v.setValueText("$nav.object.name#if($nav.object.isfolder)($nav.subtree)#else{$nav.relativecount}#end");
					}, false));
				}, false));
			});
		});

		// Create Node with root folder
		Node node = Trx.supply(() -> createNode("My Node", "test", "/", "/", false, false));

		// Create template with navigation tag
		Template tpl = Trx.supply(() -> create(Template.class, template -> {
			template.setName("navtemplate");
			template.setSource("<node navtag>");
			template.getTags().put("navtag", create(TemplateTag.class, tt -> {
				tt.setConstructId(nav.getId());
				tt.setName("navtag");
				tt.setEnabled(true);
			}, false));
			template.setFolderId(node.getFolder().getId());
		}));

		// Create page with navigation to look at
		Page lookatPage = Trx.supply(() -> update(createPage(node.getFolder(), tpl, "Online Page"), Page::publish));

		// Create page contained in the navigation and take it offline
		Page offlinePage = Trx.supply(() -> createPage(node.getFolder(), tpl, "Offline Page"));
		// publish page
		Trx.consume(p -> update(p, Page::publish), offlinePage);
		// take offline
		Trx.consume(p -> update(p, Page::takeOffline), offlinePage);

		// Create page contained in the navigation and don't publish it
		Page newPage = Trx.supply(() -> createPage(node.getFolder(), tpl, "Unpublished Page"));

		try (Trx trx = new Trx()) {
			testContext.publish(2);
			trx.success();
		}

		// Check page contents
		try (Trx trx = new Trx()) {
			DBUtils.executeStatement("select source from publish where page_id = ? and node_id = ? and active = 1", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, lookatPage.getId());
					stmt.setInt(2, node.getId());
				}
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					assertTrue("Result expected", rs.next());
					assertEquals("Render result doesn't match", "My Node(Online Page{1})", rs.getString(1));
					assertFalse("Only one result expected", rs.next());
				}
			});
			trx.success();
		}
	}
}
