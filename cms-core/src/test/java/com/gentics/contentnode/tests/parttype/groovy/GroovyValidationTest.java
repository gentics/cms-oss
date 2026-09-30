package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;

import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for the validation of groovy scripts in values of the {@link GroovyPartType}
 */
public class GroovyValidationTest extends AbstractGroovyTest {
	/**
	 * Script without errors
	 */
	protected final static String VALID_SCRIPT = "def sum = 1 + 1\nreturn sum";

	/**
	 * Script with a syntax error in line 2
	 */
	protected final static String SYNTAX_ERROR_SCRIPT = "def sum = 1 + 1\nreturn sum +";

	/**
	 * Script importing a class, which does not exist
	 */
	protected final static String UNKNOWN_IMPORT_SCRIPT = "import does.not.Exist\nreturn Exist.name";

	/**
	 * Script importing a class from the testpackage (which is assigned to the node)
	 */
	protected final static String TESTPACKAGE_IMPORT_SCRIPT = "import testpackage.Class1\nreturn Class1.simple()";

	/**
	 * Script importing a class from the otherpackage (which is not assigned to the node)
	 */
	protected final static String OTHERPACKAGE_IMPORT_SCRIPT = "import otherpackage.Class2\nreturn new Class2()";

	@ClassRule
	public static PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Other node, which has no packages assigned
	 */
	protected static Node otherNode;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		AbstractGroovyTest.setupOnce();

		Synchronizer.addPackage(TESTPACKAGE_NAME);
		Synchronizer.addPackage(OTHERPACKAGE_NAME);

		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));

		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), List.of("Class1.groovy"));
		prepareScripts(Synchronizer.getPackage(OTHERPACKAGE_NAME), List.of("Class2.groovy"));

		otherNode = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Other Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("other.node.hostname");
			n.setPublishDir("/other");
			n.setBinaryPublishDir("/other/bin");
		}).build();
	}

	/**
	 * Test that a valid script can be saved
	 */
	@Test
	public void testValidScript() throws NodeException {
		assertScriptSaved(VALID_SCRIPT);
	}

	/**
	 * Test that a script importing a class from a package assigned to the node can be saved
	 */
	@Test
	public void testImportFromAssignedPackage() throws NodeException {
		assertScriptSaved(TESTPACKAGE_IMPORT_SCRIPT);
	}

	/**
	 * Test that a new page can be created with a script importing a class from a package assigned to the node
	 */
	@Test
	public void testNewPageImportFromAssignedPackage() throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("New Validation Page");
			getValue(p.getContentTag(GROOVY_TAGNAME)).setValueText(TESTPACKAGE_IMPORT_SCRIPT);
		}).unlock().build();

		assertScriptText(page, TESTPACKAGE_IMPORT_SCRIPT);
	}

	/**
	 * Test that a new tag with a script importing a class from a package assigned to the node can be added to an existing page,
	 * and that a new tag with a script not compiling in the node is rejected
	 */
	@Test
	public void testNewTag() throws NodeException {
		Page page = createPage();

		Page updated = update(page, p -> {
			ContentTag tag = p.getContent().addContentTag(groovyConstruct.getId());
			getValue(tag).setValueText(TESTPACKAGE_IMPORT_SCRIPT);
		}).unlock().build();
		consume(p -> {
			assertThat(p.getContent().getContentTags().values()).as("Tags of the page")
					.anySatisfy(tag -> assertThat(getValue(tag).getValueText()).isEqualTo(TESTPACKAGE_IMPORT_SCRIPT));
		}, updated);

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(page, p -> {
			ContentTag tag = p.getContent().addContentTag(groovyConstruct.getId());
			getValue(tag).setValueText(OTHERPACKAGE_IMPORT_SCRIPT);
		}).unlock().build());
		assertThat(e).as("Thrown exception").isNotNull();
		assertThat(e.getStatus()).as("Response status").isEqualTo(Status.BAD_REQUEST);
		assertThat(e.getLocalizedMessage()).as("Message").contains(supply(() -> I18NHelper.get("validation.groovy.compileerror", I18NHelper.getName(node), "")))
				.contains("otherpackage.Class2");
	}

	/**
	 * Test that saving a script with a syntax error fails with "Bad Request"
	 */
	@Test
	public void testSyntaxError() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> setScript(page, SYNTAX_ERROR_SCRIPT));

		assertBadRequest(e, execute(p -> expectedTagMessagePrefix(p.getContentTag(GROOVY_TAGNAME), node), page));
		assertThat(e.getLocalizedMessage()).as("Message").contains(supply(() -> I18NHelper.get("validation.groovy.error", "2", "", "")).split(",")[0]);
		assertScriptText(page, "");
	}

	/**
	 * Test that saving a script importing an unknown class fails with "Bad Request"
	 */
	@Test
	public void testUnknownImport() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> setScript(page, UNKNOWN_IMPORT_SCRIPT));

		assertBadRequest(e, execute(p -> expectedTagMessagePrefix(p.getContentTag(GROOVY_TAGNAME), node), page));
		assertThat(e.getLocalizedMessage()).as("Message").contains("does.not.Exist");
		assertScriptText(page, "");
	}

	/**
	 * Test that saving a script importing a class from a package, which is not assigned to the node, fails with "Bad Request"
	 */
	@Test
	public void testImportFromNotAssignedPackage() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> setScript(page, OTHERPACKAGE_IMPORT_SCRIPT));

		assertBadRequest(e, execute(p -> expectedTagMessagePrefix(p.getContentTag(GROOVY_TAGNAME), node), page));
		assertThat(e.getLocalizedMessage()).as("Message").contains("otherpackage.Class2");
		assertScriptText(page, "");
	}

	/**
	 * Test that saving a script via {@link PageResourceImpl#save(String, PageSaveRequest)} with a syntax error fails with "Bad Request"
	 */
	@Test
	public void testRestSyntaxError() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class,
				() -> supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, SYNTAX_ERROR_SCRIPT))));

		assertBadRequest(e, execute(p -> expectedTagMessagePrefix(p.getContentTag(GROOVY_TAGNAME), node), page));
		assertScriptText(page, "");
	}

	/**
	 * Test that a template, which is assigned to two nodes, cannot be saved with a script, that only compiles in one of the nodes
	 */
	@Test
	public void testTemplateInTwoNodes() throws NodeException {
		Template twoNodeTemplate = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.addFolder(otherNode.getFolder());
			t.setMlId(1);
			t.setName("Template in two nodes");
			t.setSource("<node " + GROOVY_TAGNAME + ">");

			t.getTemplateTags().put(GROOVY_TAGNAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(groovyConstruct.getId());
				tag.setEnabled(true);
				tag.setName(GROOVY_TAGNAME);
				tag.setPublic(true);
			}).doNotSave().build());
		}).unlock().build();

		// compiles in both nodes
		Template updated = update(twoNodeTemplate, t -> {
			getValue(t.getTemplateTag(GROOVY_TAGNAME)).setValueText(VALID_SCRIPT);
		}).unlock().build();
		consume(t -> {
			assertThat(getValue(t.getTemplateTag(GROOVY_TAGNAME)).getValueText()).as("Stored script").isEqualTo(VALID_SCRIPT);
		}, updated);

		// the testpackage is only assigned to one of the nodes
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(twoNodeTemplate, t -> {
			getValue(t.getTemplateTag(GROOVY_TAGNAME)).setValueText(TESTPACKAGE_IMPORT_SCRIPT);
		}).unlock().build());

		assertBadRequest(e, execute(t -> expectedTagMessagePrefix(t.getTemplateTag(GROOVY_TAGNAME), otherNode), twoNodeTemplate));
		assertThat(e.getLocalizedMessage()).as("Message").contains("testpackage.Class1");
		consume(t -> {
			assertThat(getValue(t.getTemplateTag(GROOVY_TAGNAME)).getValueText()).as("Stored script").isEqualTo(VALID_SCRIPT);
		}, twoNodeTemplate);
	}

	/**
	 * Test that saving a construct with a syntax error in the default value of a groovy part fails with "Bad Request"
	 */
	@Test
	public void testConstructDefaultValueSyntaxError() throws NodeException {
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> createConstruct(SYNTAX_ERROR_SCRIPT, "construct_syntax_error", null));

		String expected = supply(() -> I18NHelper.get("validation.groovy.part.failed", GROOVY_PART_NAME, I18NHelper.get("validation.groovy.compileerror.nonode", "")));
		assertBadRequest(e, expected.substring(0, expected.length() - 1));
	}

	/**
	 * Test that a construct, which is assigned to a node, can be saved with a default value importing a class from a package assigned to the node,
	 * but not, if the construct is also assigned to another node
	 */
	@Test
	public void testConstructDefaultValueImport() throws NodeException {
		Construct construct = createConstruct(TESTPACKAGE_IMPORT_SCRIPT, "construct_import", node);
		consume(c -> {
			assertThat(c.getParts().get(0).getDefaultValue().getValueText()).as("Stored default value").isEqualTo(TESTPACKAGE_IMPORT_SCRIPT);
		}, construct);

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(construct, c -> {
			c.getNodes().add(otherNode);
			c.getParts().get(0).getDefaultValue().setValueText(TESTPACKAGE_IMPORT_SCRIPT + "\n");
		}).build());

		String expected = supply(() -> I18NHelper.get("validation.groovy.part.failed", GROOVY_PART_NAME,
				I18NHelper.get("validation.groovy.compileerror", I18NHelper.getName(otherNode), "")));
		assertBadRequest(e, expected.substring(0, expected.length() - 1));
	}

	/**
	 * Create a construct with a groovy part with the given default value
	 * @param defaultValue default value
	 * @param keyword construct keyword
	 * @param constructNode node to assign the construct to (may be null)
	 * @return construct
	 * @throws NodeException
	 */
	protected Construct createConstruct(String defaultValue, String keyword, Node constructNode) throws NodeException {
		return create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword(keyword);
			c.setName(keyword, 1);
			if (constructNode != null) {
				c.getNodes().add(constructNode);
			}

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(GroovyPartType.class));
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(GROOVY_PART_NAME);
				p.setName("Groovy", 1);
				p.setDefaultValue(create(Value.class, v -> {
					v.setValueText(defaultValue);
				}).doNotSave().build());
			}).doNotSave().build());
		}).build();
	}

	/**
	 * Create a new page using the test template
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage() throws NodeException {
		return create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("Validation Page");
		}).unlock().build();
	}

	/**
	 * Set the script into the groovy tag of the page
	 * @param page page
	 * @param script script
	 * @return updated page
	 * @throws NodeException
	 */
	protected Page setScript(Page page, String script) throws NodeException {
		return update(page, p -> {
			getValue(p.getContentTag(GROOVY_TAGNAME)).setValueText(script);
		}).unlock().build();
	}

	/**
	 * Save the script in a new page and assert that it was stored
	 * @param script script
	 * @throws NodeException
	 */
	protected void assertScriptSaved(String script) throws NodeException {
		Page page = setScript(createPage(), script);
		assertScriptText(page, script);
	}

	/**
	 * Assert that the groovy tag of the page stores the expected script
	 * @param page page
	 * @param expected expected script
	 * @throws NodeException
	 */
	protected void assertScriptText(Page page, String expected) throws NodeException {
		consume(p -> {
			assertThat(getValue(p.getContentTag(GROOVY_TAGNAME)).getValueText()).as("Stored script").isEqualTo(expected);
		}, page);
	}

	/**
	 * Get the expected beginning of the message for a script, which does not compile in the given node (must be called within a transaction).
	 * The compiler errors follow.
	 * @param tag tag
	 * @param failedNode node, in which the script does not compile
	 * @return expected beginning of the message
	 * @throws NodeException
	 */
	protected static String expectedTagMessagePrefix(Tag tag, Node failedNode) throws NodeException {
		String message = I18NHelper.get("validation.groovy.tag.part.failed", tag.getName() + " / " + tag.getId(), GROOVY_PART_NAME,
				I18NHelper.get("validation.groovy.compileerror", I18NHelper.getName(failedNode), ""));
		// remove the trailing "." of the message
		return message.substring(0, message.length() - 1);
	}

	/**
	 * Assert that the exception is a "Bad Request" with a message starting with the expected prefix
	 * @param e exception (may be null, if nothing was thrown)
	 * @param expectedMessagePrefix expected beginning of the message
	 */
	protected void assertBadRequest(RestMappedException e, String expectedMessagePrefix) {
		assertThat(e).as("Thrown exception").isNotNull();
		assertThat(e.getStatus()).as("Response status").isEqualTo(Status.BAD_REQUEST);
		assertThat(e.getResponseCode()).as("Response code").isEqualTo(ResponseCode.INVALIDDATA);
		assertThat(e.getMessageType()).as("Message type").isEqualTo(Message.Type.CRITICAL);
		assertThat(e.getLocalizedMessage()).as("Message").startsWith(expectedMessagePrefix).isNotEqualTo(expectedMessagePrefix + ".");
	}

	/**
	 * Create a save request for the page, which sets the script into the groovy tag
	 * @param page page
	 * @param script script
	 * @return save request
	 */
	protected PageSaveRequest saveRequest(Page page, String script) {
		Property prop = new Property();
		prop.setType(Property.Type.RICHTEXT);
		prop.setStringValue(script);
		com.gentics.contentnode.rest.model.Tag tag = new com.gentics.contentnode.rest.model.Tag();
		tag.setType(com.gentics.contentnode.rest.model.Tag.Type.CONTENTTAG);
		tag.setName(GROOVY_TAGNAME);
		tag.setActive(true);
		tag.setProperties(Map.of(GROOVY_PART_NAME, prop));

		com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
		restPage.setId(page.getId());
		restPage.setTags(Map.of(GROOVY_TAGNAME, tag));
		PageSaveRequest request = new PageSaveRequest();
		request.setPage(restPage);
		return request;
	}

	/**
	 * Get the value of the groovy part from the tag
	 * @param tag tag
	 * @return value
	 * @throws NodeException
	 */
	protected static Value getValue(Tag tag) throws NodeException {
		return tag.getValues().getByKeyname(GROOVY_PART_NAME);
	}
}
