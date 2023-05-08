package com.gentics.contentnode.tests.parttype;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.NavigationPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for rendering different settings of the navigation parttype
 */
@RunWith(value = Parameterized.class)
public class NavigationPartTypeRenderingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Construct navconst;

	private static Node node;

	private static Template template;

	private static Page page;

	private static Construct checkBoxConstruct;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();

		String navTemplate = com.gentics.lib.etc.StringUtils.readStream(NavigationPartTypeRenderingTest.class.getResourceAsStream("navigation.vm"));

		navconst = supply(() -> {
			return create(Construct.class, navconst -> {
				navconst.setName("navconst", 1);
				navconst.setAutoEnable(true);
				navconst.setIconName("karl");
				navconst.setKeyword("navconst");

				navconst.getParts().add(create(Part.class, navpart -> {
					navpart.setPartTypeId(getPartTypeId(NavigationPartType.class));
					navpart.setKeyname("nav");
					navpart.setHidden(false);
				}, false));
				navconst.getParts().add(create(Part.class, objectsPart -> {
					objectsPart.setPartTypeId(getPartTypeId(ShortTextPartType.class));
					objectsPart.setKeyname("objects");
					objectsPart.setHidden(true);
					objectsPart.setDefaultValue(create(Value.class, v -> {
						v.setValueText("");
					}, false));
				}, false));
				navconst.getParts().add(create(Part.class, templatePart -> {
					templatePart.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
					templatePart.setKeyname("template");
					templatePart.setHidden(true);
					templatePart.setDefaultValue(create(Value.class, v -> {
						v.setValueText(navTemplate);
					}, false));
				}, false));
				navconst.getParts().add(create(Part.class, sitemapPart -> {
					sitemapPart.setPartTypeId(getPartTypeId(CheckboxPartType.class));
					sitemapPart.setKeyname("sitemap");
					sitemapPart.setHidden(true);
					sitemapPart.setDefaultValue(create(Value.class, v -> {
						v.setValueText("1");
					}, false));
				}, false));
				navconst.getParts().add(create(Part.class, disableHiddenPart -> {
					disableHiddenPart.setPartTypeId(getPartTypeId(CheckboxPartType.class));
					disableHiddenPart.setKeyname("disable_hidden");
					disableHiddenPart.setHidden(true);
					disableHiddenPart.setDefaultValue(create(Value.class, v -> {
						v.setValueText("0");
					}, false));
				}, false));
			});
		});

		checkBoxConstruct = supply(() -> {
			return create(Construct.class, booleanConstruct -> {
				booleanConstruct.setName("check", 1);
				booleanConstruct.setAutoEnable(true);
				booleanConstruct.setIconName("bla");
				booleanConstruct.setKeyword("check");

				booleanConstruct.getParts().add(create(Part.class, checkpart -> {
					checkpart.setPartTypeId(getPartTypeId(CheckboxPartType.class));
					checkpart.setKeyname("check");
					checkpart.setHidden(false);
					checkpart.setEditable(1);
				}, false));
			});
		});

		operate(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, checkBoxConstruct.getId(), "navhidden", "navhidden"));
		operate(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, checkBoxConstruct.getId(), "navhidden", "navhidden"));
		operate(() -> createObjectPropertyDefinition(File.TYPE_FILE, checkBoxConstruct.getId(), "navhidden", "navhidden"));
		operate(() -> createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, checkBoxConstruct.getId(), "navhidden", "navhidden"));

		node = supply(() -> createNode("hostname", "Node Name", PublishTarget.NONE));
		template = supply(() -> create(Template.class, template -> {
			template.setFolderId(node.getFolder().getId());
			template.setName("Template");
			template.setSource("<node navigation>");

			template.getTemplateTags().put("navigation", create(TemplateTag.class, tt -> {
				tt.setConstructId(navconst.getId());
				tt.setName("navigation");
				tt.setEnabled(true);
				tt.setPublic(false);
			}, false));
		}));

		// create folder structure
		Folder root = supply(() -> node.getFolder());
		Folder folder1 = supply(() -> createFolder(node.getFolder(), "Folder 1"));
		Folder folder1_1 = supply(() -> createFolder(folder1, "Folder 1.1"));
		Folder folder1_2 = supply(() -> createFolder(folder1, "Folder 1.2"));
		Folder folder2 = supply(() -> createFolder(node.getFolder(), "Folder 2"));
		Folder folder2_1 = supply(() -> createFolder(folder2, "Folder 2.1"));
		Folder folder2_2 = supply(() -> createFolder(folder2, "Folder 2.2"));
		Folder hidden = supply(() -> update(createFolder(node.getFolder(), "Hidden"), upd -> {
			upd.getObjectTag("navhidden").setEnabled(true);
			upd.getObjectTag("navhidden").getValues().getByKeyname("check").setValueText("1");
		}));

		// create rendered page
		page = supply(() -> update(createPage(node.getFolder(), template, "NavPage"), Page::publish));

		// create other objects in the folders
		for (Folder f : Arrays.asList(root, folder1, folder1_1, folder1_2, folder2, folder2_1, folder2_2, hidden)) {
			// pages
			operate(() -> update(createPage(f, template, "Page 1"), Page::publish));
			operate(() -> update(createPage(f, template, "Page 2"), Page::publish));
			operate(() -> createPage(f, template, "Offline Page"));
			operate(() -> update(createPage(f, template, "Hidden Page"), upd -> {
				upd.getObjectTag("navhidden").setEnabled(true);
				upd.getObjectTag("navhidden").getValues().getByKeyname("check").setValueText("1");
			}));

			// images
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			FileUtil.inputStreamToOutputStream(GenericTestUtils.getPictureResource("blume.jpg"), data);
			byte[] imageBytes = data.toByteArray();
			operate(() -> createImage(f, "image1.jpg", imageBytes));
			operate(() -> createImage(f, "image2.jpg", imageBytes));
			operate(() -> update(createImage(f, "hidden_image.jpg", imageBytes), upd -> {
				upd.getObjectTag("navhidden").setEnabled(true);
				upd.getObjectTag("navhidden").getValues().getByKeyname("check").setValueText("1");
			}));

			// files
			operate(() -> createFile(f, "file1.txt", "File data".getBytes()));
			operate(() -> createFile(f, "file2.txt", "File data".getBytes()));
			operate(() -> update(createFile(f, "hidden_file.txt", "File data".getBytes()), upd -> {
				upd.getObjectTag("navhidden").setEnabled(true);
				upd.getObjectTag("navhidden").getValues().getByKeyname("check").setValueText("1");
			}));
		}
	}

	@Parameters(name = "{index}: all: {0}, folders: {1}, pages: {2}, images: {3}, files: {4}, disable_hidden: {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean all : Arrays.asList(true, false)) {
			for (boolean folders : Arrays.asList(true, false)) {
				for (boolean pages : Arrays.asList(true, false)) {
					for (boolean images : Arrays.asList(true, false)) {
						for (boolean files : Arrays.asList(true, false)) {
							if (all && (folders || pages || images || files)) {
								continue;
							}
							for (boolean disableHidden : Arrays.asList(true, false)) {
								data.add(new Object[] {all, folders, pages, images, files, disableHidden});
							}
						}
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean all;

	@Parameter(1)
	public boolean folders;

	@Parameter(2)
	public boolean pages;

	@Parameter(3)
	public boolean images;

	@Parameter(4)
	public boolean files;

	@Parameter(5)
	public boolean disableHidden;

	private String expected;

	@Before
	public void setup() throws NodeException, IOException {
		List<String> objects = new ArrayList<>();
		if (all) {
			objects.add("all");
		}
		if (folders) {
			objects.add("folders");
		}
		if (pages) {
			objects.add("pages");
		}
		if (images) {
			objects.add("images");
		}
		if (files) {
			objects.add("files");
		}
		String objectsString = objects.isEmpty() ? "none" : StringUtils.join(objects, ",");
		String filename = String.format("nav_%s%s.txt", objectsString.replaceAll(",", "_"), disableHidden ? "_disable_hidden" : "");
		expected = com.gentics.lib.etc.StringUtils
				.readStream(NavigationPartTypeRenderingTest.class.getResourceAsStream(filename));

		navconst = Trx.execute(c -> update(c, upd -> {
			upd.getValues().getByKeyname("objects").setValueText(objectsString);
			upd.getValues().getByKeyname("disable_hidden").setValueText(disableHidden ? "1" : "0");
		}), navconst);
	}

	@Test
	public void testRender() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.render()).as("Rendered navigation").isEqualTo(expected);
			trx.success();
		}
	}
}
