package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
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
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for generating the publish paths of pages and files
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT, Feature.TAG_IMAGE_RESIZER, Feature.MESH_CONTENTREPOSITORY })
public class StaticUrlFactoryTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();
	private static Node node;
	private static ContentRepository meshCr;
	private static Page page;
	private static File file;

	@Parameters(name = "{index}: segments {0}, mesh {1}, project per node {2}, filename {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] { true, true, true, true, "/pubdir/home/folder/page.html", "/pubdir/home/folder/blume.jpg" });
		data.add(new Object[] { true, true, true, false, "/pubdir/home/folder", "/pubdir/home/folder" });

		data.add(new Object[] { true, true, false, true, "/home/folder/page.html", "/home/folder/blume.jpg" });
		data.add(new Object[] { true, true, false, false, "/home/folder", "/home/folder" });

		data.add(new Object[] { false, true, true, true, "/pubdir/folder/page.html", "/pubdir/folder/blume.jpg" });
		data.add(new Object[] { false, true, true, false, "/pubdir/folder", "/pubdir/folder" });

		data.add(new Object[] { false, true, false, true, "/folder/page.html", "/folder/blume.jpg" });
		data.add(new Object[] { false, true, false, false, "/folder", "/folder" });

		data.add(new Object[] { true, false, false, true, "/pubdir/home/folder/page.html", "/binary/home/folder/blume.jpg" });
		data.add(new Object[] { true, false, false, false, "/pubdir/home/folder", "/binary/home/folder" });

		data.add(new Object[] { false, false, false, true, "/pubdir/folder/page.html", "/binary/folder/blume.jpg" });
		data.add(new Object[] { false, false, false, false, "/pubdir/folder", "/binary/folder" });

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Node", PublishTarget.NONE));
		node = update(node, upd -> {
			upd.setPublishDir("pubdir");
			upd.setBinaryPublishDir("binary");
		});

		supply(() -> update(node.getFolder(), upd -> {
			upd.setPublishDir("home");
		}));

		Folder folder = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Folder");
			create.setPublishDir("folder");
		});

		Template template = create(Template.class, create -> {
			create.setFolderId(folder.getId());
			create.setName("Template");
			create.setMlId(1);
			create.setSource("");
		});

		page = create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("Page");
			create.setFilename("page.html");
		});

		file = supply(() -> {
			try {
				return createImage(folder, "blume.jpg",
						IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		});

		meshCr = create(ContentRepository.class, upd -> {
			upd.setCrType(Type.mesh);
			upd.setName("Mesh CR");
		});
	}

	@Parameter(0)
	public boolean segments;

	@Parameter(1)
	public boolean mesh;

	@Parameter(2)
	public boolean projectPerNode;

	@Parameter(3)
	public boolean appendFileName;

	@Parameter(4)
	public String expectedPageUrl;

	@Parameter(5)
	public String expectedFileUrl;

	@Before
	public void setup() throws NodeException {
		node = update(node, upd -> {
			upd.setPubDirSegment(segments);
			upd.setContentrepositoryId(mesh ? meshCr.getId() : 0);
		});

		meshCr = update(meshCr, upd -> {
			upd.setProjectPerNode(projectPerNode);
		});
	}

	/**
	 * Test rendering the page URL
	 * @throws NodeException
	 */
	@Test
	public void testPageUrl() throws NodeException {
		assertThat(supply(() -> StaticUrlFactory.getPublishPath(page, appendFileName))).as("Page publish path").isEqualTo(expectedPageUrl);
	}

	/**
	 * Test rendering the file URL
	 * @throws NodeException
	 */
	@Test
	public void testFileUrl() throws NodeException {
		assertThat(supply(() -> StaticUrlFactory.getPublishPath(file, appendFileName))).as("File publish path").isEqualTo(expectedFileUrl);
	}
}
