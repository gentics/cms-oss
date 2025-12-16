package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFileSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFolderSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getPageSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.microschemas;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.ImageHeightPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.ImageWidthPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.SelectClassPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.lib.util.FileUtil;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.testutils.GenericTestUtils;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@RunWith(value = Parameterized.class)
@Category(MeshTest.class)
public class MeshMicronodePublishTest {
	/**
	 * Minimum typeId
	 */
	public final static int MIN_TYPE_ID = 1;

	/**
	 * Maximum typeId
	 */
	public final static int MAX_TYPE_ID = 40;

	/**
	 * Unused typeIds
	 */
	public final static List<Integer> UNUSED_TYPE_IDS = Arrays.asList(5, 7, 12, 14, 28);

	/**
	 * Valueless typeIds
	 */
	public final static List<Integer> VALUELESS_TYPE_IDS = Arrays.asList(23, 33, 34, 35);

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	/**
	 * Name of the part
	 */
	public final static String PART_NAME = "part";

	/**
	 * Name of the template tag
	 */
	public final static String TEMPLATE_TAG_NAME = "ttag";

	@ClassRule
	public final static DBTestContext context = new DBTestContext();

	@ClassRule
	public final static MeshContext mesh = new MeshContext();

	/**
	 * Map containing the tag consumer (which fill the tag) for every tested part type ID
	 */
	public final static Map<Integer, Consumer<Tag>> TAG_CONSUMER = new HashMap<>();

	private static Node node;

	private static Integer crId;

	private static Template template;

	private static Page targetPage;

	private static ImageFile targetImage;

	private static File targetFile;

	private static Folder targetFolder;

	private static Integer constructId;

	private static Datasource datasource;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	static {
		Consumer<Tag> textType = tag -> {
			tag.getValues().getByKeyname(PART_NAME).setValueText("Tested Text Value");
		};
		Consumer<Tag> empty = tag -> {};
		Consumer<Tag> file = tag -> {
			getPartType(FileURLPartType.class, tag, PART_NAME).setTargetFile(targetFile);
		};
		Consumer<Tag> folder = tag -> {
			getPartType(FolderURLPartType.class, tag, PART_NAME).setTargetFolder(targetFolder);
		};

		TAG_CONSUMER.put(1, textType);
		TAG_CONSUMER.put(2, textType);
		TAG_CONSUMER.put(3, textType);
		TAG_CONSUMER.put(4, tag -> {
			getPartType(PageURLPartType.class, tag, PART_NAME).setTargetPage(targetPage);
		});
		TAG_CONSUMER.put(6, tag -> {
			getPartType(ImageURLPartType.class, tag, PART_NAME).setTargetImage(targetImage);
		});
		TAG_CONSUMER.put(8, file);
		TAG_CONSUMER.put(9, textType);
		TAG_CONSUMER.put(10, textType);
		TAG_CONSUMER.put(11, tag -> {
			getPartType(PageTagPartType.class, tag, PART_NAME).setPageTag(targetPage, targetPage.getContentTags().values().iterator().next());
		});
		TAG_CONSUMER.put(13, tag -> {
			fillOverview(tag, PART_NAME, "<node page.name>", Page.class, Overview.SELECTIONTYPE_SINGLE, 10, Overview.ORDER_NAME, Overview.ORDERWAY_DESC, false,
					Arrays.asList(targetPage));
		});
		TAG_CONSUMER.put(15, tag -> {
			ChangeableListPartType partType = getPartType(ChangeableListPartType.class, tag, PART_NAME);
			partType.setOrdered();
			partType.setLines("one", "two", "three", "changeable");
		});
		TAG_CONSUMER.put(16, tag -> {
			getPartType(UnorderedListPartType.class, tag, PART_NAME).setLines("one", "two", "three", "unordered");
		});
		TAG_CONSUMER.put(17, tag -> {
			getPartType(OrderedListPartType.class, tag, PART_NAME).setLines("one", "two", "three", "ordered");
		});
		TAG_CONSUMER.put(18, tag -> {
			getPartType(ImageHeightPartType.class, tag, PART_NAME).getValueObject().setValueText("120");
		});
		TAG_CONSUMER.put(19, tag -> {
			getPartType(ImageWidthPartType.class, tag, PART_NAME).getValueObject().setValueText("160");
		});
		TAG_CONSUMER.put(20, tag -> {
			getPartType(TemplateTagPartType.class, tag, PART_NAME).setTemplateTag(template, template.getTemplateTag(TEMPLATE_TAG_NAME));
		});
		TAG_CONSUMER.put(21, textType);
		TAG_CONSUMER.put(22, textType);
		TAG_CONSUMER.put(23, empty);
		TAG_CONSUMER.put(24, tag -> {
			getPartType(SelectClassPartType.class, tag, PART_NAME).getValueObject().setValueText("zelledunkel");
		});
		TAG_CONSUMER.put(25, folder);
		TAG_CONSUMER.put(26, textType);
		TAG_CONSUMER.put(27, textType);
		TAG_CONSUMER.put(29, tag -> {
			getPartType(SingleSelectPartType.class, tag, PART_NAME).setSelected(datasource.getEntries().get(1));
		});
		TAG_CONSUMER.put(30, tag -> {
			getPartType(MultiSelectPartType.class, tag, PART_NAME).setSelected(datasource.getEntries().get(0), datasource.getEntries().get(2));
		});
		TAG_CONSUMER.put(31, tag -> {
			getPartType(CheckboxPartType.class, tag, PART_NAME).setChecked(true);
		});
		TAG_CONSUMER.put(32, tag -> {
			List<DatasourceEntry> entries = getPartType(DatasourcePartType.class, tag, PART_NAME).getDatasource().getEntries();
			entries.add(create(DatasourceEntry.class, entry -> {
				entry.setKey("key1");
				entry.setValue("value1");
			}, false));
			entries.add(create(DatasourceEntry.class, entry -> {
				entry.setKey("key2");
				entry.setValue("value2");
			}, false));
			entries.add(create(DatasourceEntry.class, entry -> {
				entry.setKey("key3");
				entry.setValue("value3");
			}, false));
		});
		TAG_CONSUMER.put(33, empty);
		TAG_CONSUMER.put(34, empty);
		TAG_CONSUMER.put(35, empty);
		TAG_CONSUMER.put(36, textType);
		TAG_CONSUMER.put(37, textType);
		TAG_CONSUMER.put(38, file);
		TAG_CONSUMER.put(39, folder);
		TAG_CONSUMER.put(40, tag -> {
			getPartType(NodePartType.class, tag, PART_NAME).setNode(node);
		});
	}

	/**
	 * Create static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> createNode("node1", "Node1", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(true);
		}));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		Map<String, String> dsEntries = new HashMap<>();
		dsEntries.put("key1", "value1");
		dsEntries.put("key2", "value2");
		dsEntries.put("key3", "value3");
		datasource = Trx.supply(() -> createDatasource("", dsEntries));

		constructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "text", "text"));

		template = Trx.supply(() -> update(createTemplate(node.getFolder(), "Template"), upd -> {
			TemplateTag tag = create(TemplateTag.class, created -> {
				created.setConstructId(constructId);
				created.setEnabled(true);
				created.setName(TEMPLATE_TAG_NAME);
				created.setPublic(false);
			}, false);

			upd.getTemplateTags().put(TEMPLATE_TAG_NAME, tag);
		}));

		targetFolder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		targetPage = Trx.supply(() -> update(update(createPage(node.getFolder(), template, "Target"), upd -> {
			upd.getContent().addContentTag(constructId).setName("text1");
		}), Page::publish));
		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		targetImage = (ImageFile) Trx.supply(() -> createImage(node.getFolder(), "image.jpg", imageData));
		targetFile = Trx.supply(() -> createFile(node.getFolder(), "file.txt", "File contents".getBytes()));
	}

	@Parameters(name = "{index}: parttype {0}, entry {1}")
	public static Collection<Object[]> data() throws NodeException {
		Collection<Object[]> data = new ArrayList<>();
		for (int typeId = MIN_TYPE_ID; typeId <= MAX_TYPE_ID; typeId++) {
			if (UNUSED_TYPE_IDS.contains(typeId)) {
				continue;
			}
			for (boolean entry : Arrays.asList(true, false)) {
				data.add(new Object[] { typeId, entry });
			}
		}
		return data;
	}

	@Parameter(0)
	public int typeId;

	@Parameter(1)
	public boolean entry;

	/**
	 * Tested construct
	 */
	protected Construct construct;

	@Before
	public void setup() throws NodeException {
		if (entry) {
			Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.getEntries().add(create(TagmapEntry.class, entry -> {
					entry.setObject(Page.TYPE_PAGE);
					entry.setAttributeTypeId(AttributeType.micronode.getType());
					entry.setTagname("page.tags");
					entry.setMapname("tags");
					entry.setMultivalue(true);
				}, false));
			}));
		}
	}

	/**
	 * Clean all data from previous test runs
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		cleanMesh(mesh.client());

		Trx.operate(() -> {
			for (Folder f : node.getFolder().getChildFolders()) {
				if (!f.equals(targetFolder)) {
					f.delete(true);
				}
			}

			for (File f : node.getFolder().getFilesAndImages()) {
				if (!f.equals(targetFile) && !f.equals(targetImage)) {
					f.delete(true);
				}
			}

			for (Page p : node.getFolder().getPages()) {
				if (!p.equals(targetPage)) {
					p.delete(true);
				}
			}
		});

		if (construct != null) {
			Trx.operate(() -> {
				construct.delete(true);
			});
			construct = null;
		}

		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.getEntries().removeIf(entry -> entry.getAttributetype() == AttributeType.micronode);
			});
		});
	}

	@Test
	public void test() throws NodeException, InterruptedException, IOException {
		String projectName = Trx.supply(() -> node.getFolder().getName());

		// create construct
		construct = Trx.supply(() -> {
			return create(Construct.class, c -> {
				c.setAutoEnable(true);
				c.setKeyword("construct");
				c.setName("Test Construct", 1);
				c.getNodes().add(node);

				Part part = create(Part.class, p -> {
					p.setEditable(1);
					p.setHidden(false);
					p.setKeyname(PART_NAME);
					p.setName("Test Part", 1);
					p.setPartTypeId(typeId);
					p.setDefaultValue(create(Value.class, v -> {}, false));

					if (typeId == 29 || typeId == 30) {
						p.setInfoInt(datasource.getId());
					}
				}, false);

				c.getParts().add(part);
			});
		});

		boolean repaired = Trx.supply(t -> {
			return t.getObject(ContentRepository.class, crId).checkStructure(true);
		});

		if (!repaired) {
			String checkResult = Trx.supply(t -> {
				return t.getObject(ContentRepository.class, crId).getCheckResult();
			});
			fail(String.format("CR Check failed. %s", checkResult));
		}

		List<MicroschemaResponse> microschemas = microschemas(mesh.client()).test().await().assertComplete().values();

		String expectedMicroschemaName = SCHEMA_PREFIX + "_construct";
		Optional<MicroschemaResponse> optionalMicroschema = microschemas.stream().filter(ms -> StringUtils.equals(expectedMicroschemaName, ms.getName()))
				.findFirst();
		if (entry && !VALUELESS_TYPE_IDS.contains(typeId)) {
			assertThat(optionalMicroschema).isNotEmpty();

			assertThat(optionalMicroschema.get().getFields()).as("Microschema Fields").usingFieldByFieldElementComparator()
					.containsOnlyElementsOf(getExpectedFields());
		} else {
			assertThat(optionalMicroschema).isEmpty();
		}

		if (entry) {
			if (Arrays.asList(4, 11, 13).contains(typeId)) {
				Trx.operate(() -> Events.trigger(targetPage, null, Events.EVENT_CN_PAGESTATUS));
			}
			if (Arrays.asList(8, 38).contains(typeId)) {
				Trx.operate(() -> Events.trigger(targetFile, null, Events.UPDATE));
			}
			if (Arrays.asList(6).contains(typeId)) {
				Trx.operate(() -> Events.trigger(targetImage, null, Events.UPDATE));
			}
			if (Arrays.asList(25, 39).contains(typeId)) {
				Trx.operate(() -> Events.trigger(targetFolder, null, Events.UPDATE));
			}
		}

		Page page = Trx.supply(() -> update(create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(template.getId());

			ContentTag tag = create.getContent().addContentTag(construct.getId());
			TAG_CONSUMER.get(typeId).accept(tag);
		}), Page::publish));

		assertObject("Published page", mesh.client(), projectName, page, true, node -> {
			MicronodeFieldList field = node.getFields().getMicronodeFieldList("tags");

			if (!entry) {
				assertThat(field).as("Field").isNull();
			} else if (VALUELESS_TYPE_IDS.contains(typeId)) {
				assertThat(field).as("Field").isNotNull();
				assertThat(field.getItems()).as("Micronodes").isEmpty();
			} else {
				assertThat(field).as("Field").isNotNull();
				assertThat(field.getItems()).as("Micronodes").hasSize(1);
				String expectedField;
				try {
					expectedField = FileUtil.stream2String(getClass().getResourceAsStream(String.format("micronode_%d.json", typeId)), "UTF-8");

					String micronodeUuid = field.getItems().get(0).getUuid();
					String microschemaUuid = field.getItems().get(0).getMicroschema().getUuid();
					String versionUuid = field.getItems().get(0).getMicroschema().getVersionUuid();
					String targetFolderUuid = MeshPublisher.getMeshUuid(targetFolder);
					String targetPageUuid = MeshPublisher.getMeshUuid(targetPage);
					String targetFileUuid = MeshPublisher.getMeshUuid(targetFile);
					String targetImageUuid = MeshPublisher.getMeshUuid(targetImage);
					String templateId = Integer.toString(template.getId());
					String nodeId = Integer.toString(MeshMicronodePublishTest.node.getId());

					assertThat(field.toJson().replaceAll("\r\n", "\n")).as("Field JSON")
							.isEqualTo(expectedField.replaceAll("#MICRONODE_UUID#", micronodeUuid).replaceAll("#MICROSCHEMA_UUID#", microschemaUuid)
									.replaceAll("#TARGET_PAGE_UUID#", targetPageUuid).replaceAll("#TARGET_FILE_UUID#", targetFileUuid)
									.replaceAll("#TARGET_IMAGE_UUID#", targetImageUuid).replaceAll("#TEMPLATE_ID#", templateId)
									.replaceAll("#TARGET_FOLDER_UUID#", targetFolderUuid).replaceAll("#NODE_ID#", nodeId).replaceAll("#VERSION_UUID#", versionUuid));
				} catch (IOException e) {
					throw new NodeException(e);
				}
			}
		});
	}

	/**
	 * Get list of expected field schemas
	 * @return list of field schemas
	 */
	protected List<FieldSchema> getExpectedFields() {
		switch (typeId) {
		case 4: // URL (page)
			return Arrays.asList(new NodeFieldSchemaImpl().setAllowedSchemas(getPageSchemaName(SCHEMA_PREFIX)).setName("part_internal").setLabel("Test Part"),
					new StringFieldSchemaImpl().setName("part_external").setLabel("Test Part"));
		case 6: // URL (image)
		case 8: // URL (file)
		case 38: // Upload (file)
			return Arrays.asList(new NodeFieldSchemaImpl().setAllowedSchemas(getFileSchemaName(SCHEMA_PREFIX)).setName("part").setLabel("Test Part"));
		case 11: // Tag (page)
			return Arrays.asList(new NodeFieldSchemaImpl().setAllowedSchemas(getPageSchemaName(SCHEMA_PREFIX)).setName("part_page").setLabel("Test Part"),
					new StringFieldSchemaImpl().setName("part_tag").setLabel("Test Part"));
		case 13: // Overview
			return Arrays.asList(
					new StringFieldSchemaImpl().setAllowedValues("PAGE", "FOLDER", "FILE", "IMAGE", "UNDEFINED").setName("part_listType").setLabel("Test Part"),
					new StringFieldSchemaImpl().setAllowedValues("FOLDER", "MANUAL", "AUTO", "UNDEFINED").setName("part_selectType").setLabel("Test Part"),
					new StringFieldSchemaImpl().setAllowedValues("ASC", "DESC", "UNDEFINED").setName("part_orderDir").setLabel("Test Part"),
					new StringFieldSchemaImpl().setAllowedValues("ALPHABETICALLY", "PRIORITY", "PDATE", "EDATE", "CDATE", "FILESIZE", "SELF", "UNDEFINED")
							.setName("part_orderBy").setLabel("Test Part"),
					new ListFieldSchemaImpl().setListType(FieldTypes.NODE.toString())
							.setAllowedSchemas(getFileSchemaName(SCHEMA_PREFIX), getFolderSchemaName(SCHEMA_PREFIX), getPageSchemaName(SCHEMA_PREFIX))
							.setName("part_items").setLabel("Test Part"),
					new ListFieldSchemaImpl().setListType(FieldTypes.NUMBER.toString()).setName("part_nodeIds").setLabel("Test Part"),
					new StringFieldSchemaImpl().setName("part_source").setLabel("Test Part"),
					new BooleanFieldSchemaImpl().setName("part_recursive").setLabel("Test Part"),
					new NumberFieldSchemaImpl().setName("part_maxItems").setLabel("Test Part"));
		case 15: // List
			return Arrays.asList(new BooleanFieldSchemaImpl().setName("part_ordered").setLabel("Test Part"),
					new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()).setName("part").setLabel("Test Part"));
		case 16: // List (unordered)
		case 17: // List (ordered)
			return Arrays.asList(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()).setName("part").setLabel("Test Part"));
		case 20: // Tag (template)
			return Arrays.asList(new NumberFieldSchemaImpl().setName("part_template").setLabel("Test Part"),
					new StringFieldSchemaImpl().setName("part_tag").setLabel("Test Part"));
		case 25: // URL (folder)
		case 39: // Upload (folder)
			return Arrays.asList(new NodeFieldSchemaImpl().setAllowedSchemas(getFolderSchemaName(SCHEMA_PREFIX)).setName("part").setLabel("Test Part"));
		case 30: // Select (multiple)
		case 32: // Datasource
			return Arrays.asList(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()).setName("part").setLabel("Test Part"));
		default:
			return Arrays.asList(new StringFieldSchemaImpl().setName("part").setLabel("Test Part"));
		}
	}
}
