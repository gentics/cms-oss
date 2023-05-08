package com.gentics.contentnode.tests.rest.tag;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.DHTMLPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.FilePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.FormPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.ImageHeightPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.ImageWidthPartType;
import com.gentics.contentnode.object.parttype.JavaEditorPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLTextPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SelectClassPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.TablePartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for transforming tags into their REST Model and vice versa
 */
@RunWith(value = Parameterized.class)
public class TagModelTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;
	private static TemplateTag targetTemplateTag;

	private static Page targetPage;
	private static ContentTag targetTag;
	private static ImageFile targetImage;
	private static File targetFile;
	private static Folder targetFolder;
	private static Form targetForm;

	private final static String PART_KEYNAME = "part";

	private final static String TEST_TEXT = "This is the test text";

	private final static String formUuid = UUID.randomUUID().toString();

	private static Map<Class<? extends PartType>, Integer> constructMap = new HashMap<>();

	private static Integer singleSelectDsId;
	private static Integer multiSelectDsId;
	private static Integer variableDsId;

	private final static Consumer<Property> normalTextAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.STRING)
			.hasFieldOrPropertyWithValue("stringValue", TEST_TEXT);
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> richTextAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.RICHTEXT)
			.hasFieldOrPropertyWithValue("stringValue", TEST_TEXT);
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> pageUrlAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.PAGE)
			.hasFieldOrPropertyWithValue("pageId", targetPage.getId())
			.hasFieldOrPropertyWithValue("nodeId", node.getId());
		assertAllElseNull(property, "pageId", "nodeId");
	};

	private final static Consumer<Property> externalPageAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.PAGE)
			.hasFieldOrPropertyWithValue("stringValue", "http://www.gentics.com/");
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> imageUrlAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.IMAGE)
			.hasFieldOrPropertyWithValue("imageId", targetImage.getId())
			.hasFieldOrPropertyWithValue("nodeId", node.getId());
		assertAllElseNull(property, "imageId", "nodeId");
	};

	private final static Consumer<Property> fileUrlAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.FILE)
			.hasFieldOrPropertyWithValue("fileId", targetFile.getId())
			.hasFieldOrPropertyWithValue("nodeId", node.getId());
		assertAllElseNull(property, "fileId", "nodeId");
	};

	private final static Consumer<Property> folderUrlAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.FOLDER)
			.hasFieldOrPropertyWithValue("folderId", targetFolder.getId())
			.hasFieldOrPropertyWithValue("nodeId", node.getId());
		assertAllElseNull(property, "folderId", "nodeId");
	};

	private final static Consumer<Property> nodeAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.NODE)
			.hasFieldOrPropertyWithValue("nodeId", node.getId());
		assertAllElseNull(property, "nodeId");
	};

	private final static Consumer<Property> overviewAsserter = property -> {
		com.gentics.contentnode.rest.model.Overview overview = new com.gentics.contentnode.rest.model.Overview();
		overview.setListType(ListType.PAGE);
		overview.setMaxItems(100);
		overview.setOrderBy(OrderBy.CDATE);
		overview.setOrderDirection(OrderDirection.ASC);
		overview.setRecursive(false);
		overview.setSelectType(SelectType.FOLDER);
		overview.setSelectedItemIds(Arrays.asList(targetFolder.getId()));
		overview.setSource("source");
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.OVERVIEW)
			.hasFieldOrPropertyWithValue("overview", overview);
		assertAllElseNull(property, "overview");
	};

	private final static Consumer<Property> clAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.LIST)
			.hasFieldOrPropertyWithValue("booleanValue", true)
			.hasFieldOrPropertyWithValue("stringValues", Arrays.asList("one", "two", "three"));
		assertAllElseNull(property, "booleanValue" ,"stringValues");
	};

	private final static Consumer<Property> olAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.ORDEREDLIST)
			.hasFieldOrPropertyWithValue("stringValues", Arrays.asList("one", "two", "three"));
		assertAllElseNull(property, "stringValues");
	};

	private final static Consumer<Property> ulAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.UNORDEREDLIST)
			.hasFieldOrPropertyWithValue("stringValues", Arrays.asList("one", "two", "three"));
		assertAllElseNull(property, "stringValues");
	};

	private final static Consumer<Property> ttAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.TEMPLATETAG)
			.hasFieldOrPropertyWithValue("templateTagId", targetTemplateTag.getId())
			.hasFieldOrPropertyWithValue("templateId", template.getId());
		assertAllElseNull(property, "templateTagId", "templateId");
	};

	private final static Consumer<Property> pageTagAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.PAGETAG)
			.hasFieldOrPropertyWithValue("contentTagId", targetTag.getId())
			.hasFieldOrPropertyWithValue("pageId", targetPage.getId());
		assertAllElseNull(property, "contentTagId", "pageId");
	};

	private final static Consumer<Property> fileAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.LOCALFILE)
			.hasFieldOrPropertyWithValue("stringValue", TEST_TEXT);
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> tableAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.TABLE)
			.hasFieldOrPropertyWithValue("stringValue", TEST_TEXT);
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> singleSelectAsserter = property -> {
		SelectOption one = new SelectOption().setId(1).setKey("one").setValue("one");
		SelectOption two = new SelectOption().setId(2).setKey("two").setValue("two");
		SelectOption three = new SelectOption().setId(3).setKey("three").setValue("three");
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.SELECT)
			.hasFieldOrPropertyWithValue("datasourceId", singleSelectDsId)
			.hasFieldOrPropertyWithValue("options", Arrays.asList(one, two, three))
			.hasFieldOrPropertyWithValue("selectedOptions", Arrays.asList(one));
		assertAllElseNull(property, "datasourceId", "options", "selectedOptions");
	};

	private final static Consumer<Property> multiSelectAsserter = property -> {
		SelectOption one = new SelectOption().setId(1).setKey("one").setValue("one");
		SelectOption two = new SelectOption().setId(2).setKey("two").setValue("two");
		SelectOption three = new SelectOption().setId(3).setKey("three").setValue("three");
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.MULTISELECT)
			.hasFieldOrPropertyWithValue("datasourceId", multiSelectDsId)
			.hasFieldOrPropertyWithValue("options", Arrays.asList(one, two, three))
			.hasFieldOrPropertyWithValue("selectedOptions", Arrays.asList(one, three));
		assertAllElseNull(property, "datasourceId", "options", "selectedOptions");
	};

	private final static Consumer<Property> datasourceAsserter = property -> {
		SelectOption four = new SelectOption().setId(1).setKey("four").setValue("four");
		SelectOption five = new SelectOption().setId(2).setKey("five").setValue("five");
		SelectOption six = new SelectOption().setId(3).setKey("six").setValue("six");
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.DATASOURCE)
			.hasFieldOrPropertyWithValue("datasourceId", variableDsId)
			.hasFieldOrPropertyWithValue("options", Arrays.asList(four, five, six));
		assertAllElseNull(property, "datasourceId", "options");
	};

	private final static Consumer<Property> formAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.FORM)
			.hasFieldOrPropertyWithValue("stringValue", formUuid);
		assertAllElseNull(property, "stringValue");
	};

	private final static Consumer<Property> checkBoxAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.BOOLEAN)
			.hasFieldOrPropertyWithValue("booleanValue", true);
		assertAllElseNull(property, "booleanValue");
	};

	private final static Consumer<Property> cmsFormAsserter = property -> {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("type", Type.CMSFORM)
			.hasFieldOrPropertyWithValue("formId", targetForm.getId());
		assertAllElseNull(property, "formId");
	};

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());

		Integer testConstructId = supply(() -> createConstruct(node, ShortTextPartType.class, "short", "part"));

		for (TestDefinition testDef : TestDefinition.values()) {
			if (!constructMap.containsKey(testDef.clazz)) {
				Integer constructId = supply(
						() -> createConstruct(node, testDef.clazz, testDef.clazz.getSimpleName().toLowerCase(), PART_KEYNAME));
				constructMap.put(testDef.clazz, constructId);
				if (testDef == TestDefinition.SingleSelectPartType) {
					singleSelectDsId = supply(t -> getPartType(SingleSelectPartType.class, t.getObject(Construct.class, constructId), PART_KEYNAME).getDatasourceId());
				} else if (testDef == TestDefinition.MultiSelectPartType) {
					multiSelectDsId = supply(t -> getPartType(MultiSelectPartType.class, t.getObject(Construct.class, constructId), PART_KEYNAME).getDatasourceId());
				}
			}
		}

		template = supply(() -> update(createTemplate(node.getFolder(), "Template"), tmpl -> {
			tmpl.getTemplateTags().put("tagname", create(TemplateTag.class, tt -> {
				tt.setConstructId(testConstructId);
				tt.setName("tagname");
			}, false));
		}));
		targetTemplateTag = supply(() -> template.getTemplateTag("tagname"));

		targetPage = supply(() -> update(createPage(node.getFolder(), template, "Target Page"), p -> {
			p.getContent().addContentTag(testConstructId);
		}));
		targetTag = supply(() -> targetPage.getContent().getContentTags().values().iterator().next());
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		FileUtil.inputStreamToOutputStream(GenericTestUtils.getPictureResource("blume.jpg"), data);
		targetImage = (ImageFile) supply(() -> createImage(node.getFolder(), "blume.jpg", data.toByteArray()));
		targetFile = supply(() -> createFile(node.getFolder(), "testfile.txt", "Testfile data".getBytes()));
		targetFolder = supply(() -> createFolder(node.getFolder(), "Folder"));
		targetForm = supply(() -> create(Form.class, f -> {
			f.setName("Testform");
			f.setFolderId(node.getFolder().getId());
		}));
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() throws NodeException {
		Collection<Object[]> data = new ArrayList<>();

		for (TestDefinition testDef : TestDefinition.values()) {
			data.add(new Object[] { testDef });
		}
		return data;
	}

	private static void assertAllElseNull(Property property, String... nonNullProperties) {
		List<String> attributes = new ArrayList<>(Arrays.asList("stringValue", "booleanValue", "fileId", "imageId",
				"folderId", "pageId", "stringValues", "options", "selectedOptions", "datasourceId", "overview",
				"templateId", "contentTagId", "templateTagId", "nodeId", "formId"));
		attributes.removeAll(Arrays.asList(nonNullProperties));
		for (String attributeName : attributes) {
			assertThat(property).as("Property").hasFieldOrPropertyWithValue(attributeName, null);
		}
	}

	private static void assertCommonAttributes(Property property, Value value) {
		assertThat(property).as("Property")
			.hasFieldOrPropertyWithValue("id", value.getId())
			.hasFieldOrPropertyWithValue("globalId", value.getGlobalId().toString())
			.hasFieldOrPropertyWithValue("partId", value.getPartId());
	}

	@Parameter(0)
	public TestDefinition testDefinition;

	@Test
	public void testTag2Model() throws NodeException {
		assertThat(testDefinition.generator).as("Generator").isNotNull();
		assertThat(testDefinition.asserter).as("Asserter").isNotNull();
		AtomicReference<String> tagName = new AtomicReference<>();

		Page page = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Test page");
			ContentTag tag = p.getContent().addContentTag(constructMap.get(testDefinition.clazz));
			tagName.set(tag.getName());
			testDefinition.generator.accept(tag.getValues().getByKeyname(PART_KEYNAME).getPartType());
		});

		Value value = supply(() -> page.getContentTag(tagName.get()).getValues().getByKeyname(PART_KEYNAME));

		if (testDefinition == TestDefinition.DatasourcePartType) {
			variableDsId = supply(() -> getPartType(DatasourcePartType.class, page.getContentTag(tagName.get()), PART_KEYNAME).getDatasourceId());
		}

		Tag tagModel = supply(() -> ModelBuilder.getPage(page, Arrays.asList(Reference.CONTENT_TAGS))).getTags().get(tagName.get());
		assertThat(tagModel).as("REST Model of Tag").isNotNull();
		assertThat(tagModel.getProperties()).as("Properties of REST Model of Tag").containsKey(PART_KEYNAME);
		assertCommonAttributes(tagModel.getProperties().get(PART_KEYNAME), value);
		testDefinition.asserter.accept(tagModel.getProperties().get(PART_KEYNAME));
	}

	public enum TestDefinition {
		NormalTextPartType(NormalTextPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), normalTextAsserter),
		ShortTextPartType(ShortTextPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), normalTextAsserter),
		HTMLTextPartType(HTMLTextPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		HTMLPartType(HTMLPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		LongHTMLTextPartType(LongHTMLTextPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		LongHTMLPartType(LongHTMLPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		JavaEditorPartType(JavaEditorPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		DHTMLPartType(DHTMLPartType.class, (Consumer<TextPartType>) partType -> partType.setText(TEST_TEXT), richTextAsserter),
		PageURLPartTypeInternal(PageURLPartType.class, (Consumer<PageURLPartType>) partType -> {
			partType.setTargetPage(targetPage);
			partType.setNode(node);
		}, pageUrlAsserter),
		PageURLPartTypeExternal(PageURLPartType.class, (Consumer<PageURLPartType>) partType -> {
			partType.setExternalTarget("http://www.gentics.com/");
		}, externalPageAsserter),
		ImageURLPartType(ImageURLPartType.class, (Consumer<ImageURLPartType>) partType -> {
			partType.setTargetImage(targetImage);
			partType.setNode(node);
		}, imageUrlAsserter),
		FileURLPartType(FileURLPartType.class, (Consumer<FileURLPartType>) partType -> {
			partType.setTargetFile(targetFile);
			partType.setNode(node);
		}, fileUrlAsserter),
		FolderURLPartType(FolderURLPartType.class, (Consumer<FolderURLPartType>) partType -> {
			partType.setTargetFolder(targetFolder);
			partType.setNode(node);
		}, folderUrlAsserter),
		NodePartType(NodePartType.class, (Consumer<NodePartType>) partType -> {
			partType.setNode(node);
		}, nodeAsserter),
		PageTagPartType(PageTagPartType.class, (Consumer<PageTagPartType>) partType -> {
			partType.setPageTag(targetPage, targetTag);
		}, pageTagAsserter),
		TemplateTagPartType(TemplateTagPartType.class, (Consumer<TemplateTagPartType>) partType -> {
			partType.setTemplateTag(template, targetTemplateTag);
		}, ttAsserter),
		OverviewPartType(OverviewPartType.class, (Consumer<OverviewPartType>) partType -> {
			Overview overview = partType.getOverview();
			overview.setMaxObjects(100);
			overview.setObjectClass(Page.class);
			overview.setOrderKind(Overview.ORDER_CDATE);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			overview.setRecursion(false);
			overview.setSelectionType(Overview.SELECTIONTYPE_FOLDER);
			OverviewHelper.addOverviewEntriesToOverview(overview, Arrays.asList(targetFolder.getId()));
			partType.getValueObject().setValueText("source");
		}, overviewAsserter),
		ChangeableListPartType(ChangeableListPartType.class, (Consumer<ChangeableListPartType>) partType -> {
			partType.setOrdered();
			partType.setLines("one", "two", "three");
		}, clAsserter),
		UnorderedListPartType(UnorderedListPartType.class, (Consumer<UnorderedListPartType>) partType -> {
			partType.setLines("one", "two", "three");
		}, ulAsserter),
		OrderedListPartType(OrderedListPartType.class, (Consumer<OrderedListPartType>) partType -> {
			partType.setLines("one", "two", "three");
		}, olAsserter),
		ImageHeightPartType(ImageHeightPartType.class, partType -> partType.getValueObject().setValueText(TEST_TEXT), normalTextAsserter),
		ImageWidthPartType(ImageWidthPartType.class, partType -> partType.getValueObject().setValueText(TEST_TEXT), normalTextAsserter),
		SelectClassPartType(SelectClassPartType.class, partType -> partType.getValueObject().setValueText(TEST_TEXT), normalTextAsserter),
		FilePartType(FilePartType.class, partType -> partType.getValueObject().setValueText(TEST_TEXT), fileAsserter),
		TablePartType(TablePartType.class, partType -> partType.getValueObject().setValueText(TEST_TEXT), tableAsserter),
		SingleSelectPartType(SingleSelectPartType.class, (Consumer<SingleSelectPartType>) partType -> {
			partType.setSelected(partType.getItems().get(0));
		}, singleSelectAsserter),
		MultiSelectPartType(MultiSelectPartType.class, (Consumer<MultiSelectPartType>) partType -> {
			partType.setSelected(partType.getItems().get(0), partType.getItems().get(2));
		}, multiSelectAsserter),
		CheckboxPartType(CheckboxPartType.class, (Consumer<CheckboxPartType>) partType -> partType.setChecked(true), checkBoxAsserter),
		DatasourcePartType(DatasourcePartType.class, (Consumer<DatasourcePartType>) partType -> {
			fillDatasource(partType.getDatasource(), Arrays.asList("four", "five", "six"));
		}, datasourceAsserter),
		FormPartType(FormPartType.class, (Consumer<FormPartType>) partType -> {
			partType.setFormUUID(formUuid);
		}, formAsserter),
		CmsFormPartType(CmsFormPartType.class, (Consumer<CmsFormPartType>) partType -> {
			partType.setTarget(targetForm);
		}, cmsFormAsserter),
		;

		Class<? extends PartType> clazz;

		Consumer<? extends PartType> generator;

		Consumer<Property> asserter;

		TestDefinition(Class<? extends PartType> clazz, Consumer<? extends PartType> generator, Consumer<Property> asserter) {
			this.clazz = clazz;
			this.generator = generator;
			this.asserter = asserter;
		}
	}
}
