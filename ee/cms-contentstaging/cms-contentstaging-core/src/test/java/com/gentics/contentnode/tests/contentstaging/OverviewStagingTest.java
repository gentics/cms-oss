package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for content staging of overviews
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS })
public class OverviewStagingTest extends AbstractContentStagingTest<Page> {
	private Construct overviewConstruct;

	/**
	 * Create test instance
	 */
	public OverviewStagingTest() {
		super(Page.class, "page");
	}

	@Before
	public void setup() throws NodeException, IOException {
		super.setup();

		overviewConstruct = create(Construct.class, c -> {
			c.setName("Overview Construct", 1);
			c.setKeyword("overviewconstruct");
			c.setAutoEnable(true);
			List<Part> parts = c.getParts();
			// overview
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("overview");
				p.setPartTypeId(getPartTypeId(OverviewPartType.class));
			}).doNotSave().build());
		}).build();
	}

	/**
	 * Test adding a page to a content package, which contains an overview where at least one entry does not exist
	 * @throws NodeException
	 */
	@Test
	public void testExportOverviewWithInexistent() throws NodeException {
		Page overviewPage = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Overview page");

			ContentTag overviewTag = p.getContent().addContentTag(overviewConstruct.getId());
			Overview overview = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, overviewTag, "overview").getOverview();
			overview.setObjectClass(Page.class);
			overview.setSelectionType(Overview.SELECTIONTYPE_SINGLE);
			List<OverviewEntry> entries = overview.getOverviewEntries();

			entries.add(create(OverviewEntry.class, e -> {
				e.setObjectId(4711);
			}).doNotSave().build());
		}).build();

		String packageName = "TestPackage";
		createContentPackage(packageName);
		addObjectToContentPackage(packageName, overviewPage);
	}
}
