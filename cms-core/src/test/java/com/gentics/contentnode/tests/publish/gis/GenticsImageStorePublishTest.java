package com.gentics.contentnode.tests.publish.gis;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishFS;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishGISFS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.image.CNGenticsImageStore;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.lib.etc.StringUtils;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.parameter.client.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for publishing GIS images into the filesystem
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT, Feature.TAG_IMAGE_RESIZER, Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
public class GenticsImageStorePublishTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext meshContext = new MeshContext().withImageManipulationMode(ImageManipulationMode.MANUAL);

	private static Node node;

	private static Construct gisConstruct;

	private static Template template;

	private static Integer crId;

	@Parameters(name = "{index}: segments {0}, mesh {1}, project per node {2}, publish image variants {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean publishImageVariant : Arrays.asList(false, true)) {
			for (boolean segments : Arrays.asList(true, false)) {
				for (boolean mesh : Arrays.asList(true, false)) {
					for (boolean projectPerNode : Arrays.asList(true, false)) {
						if (!mesh && projectPerNode) {
							continue;
						}
						data.add(new Object[] { segments, mesh, projectPerNode, publishImageVariant });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		// delete all existing nodes
		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = supply(() -> createNode("hostname", "Node Name", PublishTarget.FILESYSTEM));

		crId = createMeshCR(meshContext, MESH_PROJECT_NAME);

		node = update(node, n -> {
			n.setContentrepositoryId(crId);
			n.setUrlRenderWayFiles(UrlRenderWay.STATIC_WITH_DOMAIN.getValue());
			n.setUrlRenderWayPages(UrlRenderWay.STATIC_WITH_DOMAIN.getValue());
			n.setPublishDir("pubdir");
			n.setBinaryPublishDir("binary");
		});

		gisConstruct = supply(() -> create(Construct.class, construct -> {
			construct.setAutoEnable(true);
			construct.setIconName("icon");
			construct.setKeyword("gistag");
			construct.setName("gistag", 1);
			construct.getNodes().add(node);

			// image part
			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(true);
				part.setKeyname("image");
				part.setName("image", 1);
				part.setPartTypeId(getPartTypeId(ImageURLPartType.class));
				part.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));

			// template part
			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(0);
				part.setHidden(true);
				part.setKeyname("template");
				part.setName("template", 1);
				part.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				part.setDefaultValue(create(Value.class, v -> {
					v.setValueText("#gtx_gis($cms.tag.parts.image.target, {\"width\": 50, \"mode\": \"smart\"})   $cms.tag.parts.image");
				}, false));
			}, false));

			// vtl part
			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(0);
				part.setHidden(false);
				part.setKeyname("vtl");
				part.setName("vtl", 1);
				part.setPartTypeId(getPartTypeId(VelocityPartType.class));
				part.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));
		}));

		template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setName("Template");
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setMlId(1);
			tmpl.setSource("<node gistag>");

			tmpl.getTags().put("gistag", create(TemplateTag.class, tag -> {
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName("gistag");
				tag.setConstructId(gisConstruct.getId());
			}, false));
		}));
	}

	protected static String getExpectedUrl(File file) {
		return "http://" + context.getPubDir().toPath().relativize(file.toPath()).toString()
				.replaceAll(Pattern.quote("\\"), "/");
	}

	@Parameter(0)
	public boolean segments;

	@Parameter(1)
	public boolean mesh;

	@Parameter(2)
	public boolean projectPerNode;

	@Parameter(3)
	public boolean publishImageVariants;

	@Before
	public void clean() throws NodeException {
		if (mesh) {
			cleanMesh(meshContext.client());
		}
		operate(() -> clear(node));

		node = update(node, n -> {
			n.setPublishFilesystem(!mesh);
			n.setPubDirSegment(segments);
			n.setPublishContentmap(mesh);
			n.setContentrepositoryId(mesh ? crId : 0);
			n.setPublishImageVariants(publishImageVariants);
		});

		ContentRepository cr = supply(t -> t.getObject(ContentRepository.class, crId));
		update(cr, upd -> {
			upd.setProjectPerNode(projectPerNode);
		});
	}

	@Test
	public void testPublishGISFile() throws Exception {
		Folder folder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("testfolder");
		}));

		ImageFile image = (ImageFile) supply(() -> {
			try {
				return createImage(folder, "blume.jpg",
						IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		});

		Page page = supply(() -> create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");

			getPartType(ImageURLPartType.class, p.getContentTag("gistag"), "image").setTargetImage(image);
		}));

		update(page, Page::publish);

		// run publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			File imageFile = assertPublishFS(context.getPubDir(), image, node, !mesh);
			assertPublishFS(context.getPubDir(), page, node, !mesh);
			File gisFile = assertPublishGISFS(context.getPubDir(), image, node, "50", "auto", "smart", !mesh);

			String source = DBUtils.select("SELECT source FROM publish WHERE page_id = ? AND node_id = ? AND active = ?", ps -> {
				ps.setInt(1, page.getId());
				ps.setInt(2, node.getId());
				ps.setBoolean(3, true);
			}, DBUtils.firstString("source"));

			String gisUrl = getExpectedUrl(gisFile);
			String imageUrl = getExpectedUrl(imageFile);

			assertThat(gisUrl + "   " + imageUrl).as("URLs").isEqualTo(source);

			if (mesh && publishImageVariants) {
				Matcher m = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(gisUrl);
				while (m.find()) {
					ImageManipulationParametersImpl imageManipulationParameters = new ImageManipulationParametersImpl();
					imageManipulationParameters.setWidth(m.group("width"));
					imageManipulationParameters.setHeight(m.group("height"));
					String mode = m.group("mode");
					imageManipulationParameters.setResizeMode(StringUtils.isEmpty(mode) ? null : ResizeMode.get(mode) );
					String cropMode = m.group("cropmode");
					imageManipulationParameters.setCropMode(StringUtils.isEmpty(cropMode) ? null : CropMode.get(cropMode));
					String topleft_x = m.group("tlx");
					String topleft_y = m.group("tly");
					String cropwidth = m.group("cw");
					String cropheight = m.group("ch");
					if (StringUtils.isInteger(topleft_x) && StringUtils.isInteger(topleft_y) && StringUtils.isInteger(cropwidth) && StringUtils.isInteger(cropheight)) {
						imageManipulationParameters.setRect(Integer.parseInt(topleft_x), Integer.parseInt(topleft_y), Integer.parseInt(cropwidth), Integer.parseInt(cropheight));
					}
					ImageVariantsResponse variants = meshContext.client().getNodeBinaryFieldImageVariants(node.getMeshProject(), MeshPublisher.getMeshUuid(image), "binarycontent").blockingGet();
					assertThat(variants.getVariants().size()).isEqualTo(1);
				}
			}

			trx.success();
		}
	}
}
