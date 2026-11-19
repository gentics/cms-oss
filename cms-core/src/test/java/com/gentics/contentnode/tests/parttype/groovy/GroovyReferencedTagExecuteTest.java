package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;

/**
 * Test cases for executing the groovy part of a tag, which is referenced from (and therefore resolved while rendering) another tag
 */
public class GroovyReferencedTagExecuteTest extends AbstractGroovyTest {
	/**
	 * Name of the folder object property, which contains a groovy part
	 */
	protected final static String OBJECT_PROPERTY_NAME = "roles";

	/**
	 * Keyname of the text part in the object property construct
	 */
	protected final static String TEXT_PART_NAME = "text";

	/**
	 * Text of the object property of the test folder
	 */
	protected final static String FOLDER_OBJECT_PROPERTY_TEXT = "Text of the folder object property";

	/**
	 * Text of the object property of the home folder (parent of the test folder)
	 */
	protected final static String HOME_FOLDER_OBJECT_PROPERTY_TEXT = "Text of the home folder object property";

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		AbstractGroovyTest.setupOnce();

		Construct objectPropertyConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_groovy_and_text");
			c.setName("Construct with Groovy and Text", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(GroovyPartType.class));
				p.setEditable(0);
				p.setHidden(true);
				p.setKeyname(GROOVY_PART_NAME);
				p.setName("Groovy", 1);
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(ShortTextPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(TEXT_PART_NAME);
				p.setName("Text", 1);
			}).doNotSave().build());
		}).build();

		// the groovy script returns the text of the tag, which it belongs to, and the name of the folder in context
		objectPropertyConstruct = update(objectPropertyConstruct, c -> {
			getPartType(GroovyPartType.class, c, GROOVY_PART_NAME)
					.setText("return cms.tag.parts.%s?.text + \" in \" + cms.folder.name".formatted(TEXT_PART_NAME));
		}).build();

		final int constructId = objectPropertyConstruct.getId();
		supply(() -> createObjectTagDefinition(OBJECT_PROPERTY_NAME, Folder.TYPE_FOLDER, constructId, node));

		testFolder = update(testFolder, f -> {
			ObjectTag objectTag = f.getObjectTag(OBJECT_PROPERTY_NAME);
			getPartType(ShortTextPartType.class, objectTag, TEXT_PART_NAME).setText(FOLDER_OBJECT_PROPERTY_TEXT);
			objectTag.setEnabled(true);
		}).build();

		homeFolder = update(homeFolder, f -> {
			ObjectTag objectTag = f.getObjectTag(OBJECT_PROPERTY_NAME);
			getPartType(ShortTextPartType.class, objectTag, TEXT_PART_NAME).setText(HOME_FOLDER_OBJECT_PROPERTY_TEXT);
			objectTag.setEnabled(true);
		}).build();
	}

	/**
	 * Test executing the groovy part of the folder object property from the handlebars part of a tag in the page.
	 * The groovy script must be executed in the context of the object property (cms.tag must be the object property)
	 * @throws NodeException
	 */
	@Test
	public void testExecuteFolderObjectPropertyFromHandlebars() throws NodeException {
		assertRendered("{{cms.folder.object.%s.parts.%s.execute}}".formatted(OBJECT_PROPERTY_NAME, GROOVY_PART_NAME),
				"%s in %s".formatted(FOLDER_OBJECT_PROPERTY_TEXT, "Testfolder"));
	}

	/**
	 * Test executing the groovy part of the object property of another folder (the parent of the page's folder) from the handlebars part of a tag in the page.
	 * The groovy script must be executed in the context of the referenced object property and the referenced folder (like when rendering the object property)
	 * @throws NodeException
	 */
	@Test
	public void testExecuteOtherFolderObjectPropertyFromHandlebars() throws NodeException {
		assertRendered("{{cms.folder.parent.object.%s.parts.%s.execute}}".formatted(OBJECT_PROPERTY_NAME, GROOVY_PART_NAME),
				"%s in %s".formatted(HOME_FOLDER_OBJECT_PROPERTY_TEXT, "Home"));
	}

	/**
	 * Set the given handlebars template into the tag of the test page and assert that the page renders to the expected result
	 * @param hbsTemplate handlebars template
	 * @param expected expected result
	 * @throws NodeException
	 */
	protected void assertRendered(String hbsTemplate, String expected) throws NodeException {
		testPage = update(testPage, p -> {
			ContentTag contentTag = p.getContentTag(GROOVY_TAGNAME);
			getPartType(HandlebarsPartType.class, contentTag, HBS_PART_NAME).setText(hbsTemplate);
		}).unlock().build();

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			assertThat(testPage.render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}
	}
}
