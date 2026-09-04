package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.testutils.DBTestContext;

import groovy.lang.GroovyClassLoader;

/**
 * Abstract base class for groovy tests
 */
public abstract class AbstractGroovyTest {
	protected final static String TESTPACKAGE_NAME = "testpackage";

	protected final static String OTHERPACKAGE_NAME = "otherpackage";

	protected static final String GROOVY_TAGNAME = "testtag";

	protected static final String HBS_PART_NAME = "hbs";

	protected static final String GROOVY_PART_NAME = "script";

	protected static final String PAGE_PART_NAME = "page";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node node;

	protected static Construct groovyConstruct;

	protected static Template template;

	protected static Folder homeFolder;

	protected static Folder testFolder;

	protected static Page testPage;

	protected static Page targetPage;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();

		ContentLanguage german = getLanguage("de");
		ContentLanguage english = getLanguage("en");

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Test Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("test.node.hostname");
			n.setPublishDir("/node/pub/dir");
			n.setBinaryPublishDir("/node/pub/dir/bin");
			n.getLanguages().add(german);
			n.getLanguages().add(english);
		}).build();

		groovyConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_groovy");
			c.setName("Construct with Groovy", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(HBS_PART_NAME);
				p.setName("Handlebars", 1);
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(GroovyPartType.class));
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(GROOVY_PART_NAME);
				p.setName("Groovy", 1);
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname(PAGE_PART_NAME);
				p.setName("Page", 1);
			}).doNotSave().build());
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Test Template");
			t.setSource("<node testtag>");

			t.getTemplateTags().put(GROOVY_TAGNAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(groovyConstruct.getId());
				tag.setEnabled(true);
				tag.setName(GROOVY_TAGNAME);
				tag.setPublic(true);
			}).doNotSave().build());
		}).unlock().build();

		homeFolder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Home");
			f.setPublishDir("/home");
		}).build();

		testFolder = create(Folder.class, f -> {
			f.setMotherId(homeFolder.getId());
			f.setName("Testfolder");
			f.setPublishDir("/test");
		}).build();

		testPage = create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("Test Page");
			p.setLanguage(german);
		}).unlock().build();

		targetPage = create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("Target Page");
			p.setLanguage(english);
		}).publish().unlock().build();
	}

	/**
	 * Prepare the scripts by copying them into the package
	 * @param synchronizer package synchronizer
	 * @param scriptFileNames list of filenames
	 * @throws IOException
	 */
	protected static void prepareScripts(MainPackageSynchronizer synchronizer, List<String> scriptFileNames) throws IOException {
		String packageName = synchronizer.getName();
		File packageRoot = synchronizer.getPackagePath().toFile();
		File scriptsRoot = new File(packageRoot, "scripts");
		if (!scriptsRoot.exists()) {
			assertThat(scriptsRoot.mkdirs()).as("Creation of dirs " + scriptsRoot + " succeded").isTrue();
		}

		for (String name : scriptFileNames) {
			try (InputStream in = GroovyRenderingTest.class.getResourceAsStream("%s/%s".formatted(packageName, name));
					OutputStream out = new FileOutputStream(new File(scriptsRoot, name))) {
				if (in != null) {
					IOUtils.copy(in, out);
				}
			}
		}
	}

	/**
	 * Modify the content of the script file in the given package
	 * @param synchronizer package synchronizer
	 * @param scriptFileName name of the script file
	 * @param modifier modified function
	 * @throws IOException
	 */
	protected static void modifyScript(MainPackageSynchronizer synchronizer, String scriptFileName,
			Function<String, String> modifier) throws IOException {
		File packageRoot = synchronizer.getPackagePath().toFile();
		File scriptsRoot = new File(packageRoot, "scripts");

		String original;
		try (InputStream in = new FileInputStream(new File(scriptsRoot, scriptFileName));
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			IOUtils.copy(in, out);
			original = out.toString(StandardCharsets.UTF_8);
		}

		String modified = modifier.apply(original);

		assertThat(modified).as("Modified script content").isNotEqualTo(original);

		try (Reader in = new StringReader(modified);
				OutputStream out = new FileOutputStream(new File(scriptsRoot, scriptFileName))) {
			IOUtils.copy(in, out, StandardCharsets.UTF_8);
		}
	}

	/**
	 * Read the file with given name. This will fail, when the file does not exist
	 * @param fileName file name
	 * @return file content
	 * @throws IOException
	 */
	protected String readFile(String fileName) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = getClass().getResourceAsStream(fileName)) {
			IOUtils.copy(in, out);
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Read the file with given name. When the file does not exist, return the default content
	 * @param fileName file name
	 * @param defaultContent default content
	 * @return file or default content
	 * @throws IOException
	 */
	protected String readFile(String fileName, String defaultContent) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = getClass().getResourceAsStream(fileName)) {
			if (in == null) {
				return defaultContent;
			}
			IOUtils.copy(in, out);
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Assert that the rendered testPage has the expected dependencies
	 * @param expectedDependencies expected dependencies
	 * @throws NodeException
	 */
	protected void assertDependencies(List<Pair<String, String>> expectedDependencies) throws NodeException {
		if (CollectionUtils.isNotEmpty(expectedDependencies)) {
			operate(() -> {
				for (Pair<String, String> dep : expectedDependencies) {
					// the left part denotes the field name of the test class containing the object from which the test page depends
					String fieldName = dep.getLeft();
					// the right part denotes the property
					String property = dep.getRight();
					try {
						Field field = AbstractGroovyTest.class.getDeclaredField(fieldName);
						NodeObject nodeObject = ObjectTransformer.get(NodeObject.class, field.get(null));
						assertThat(testPage).dependsOn(nodeObject, property, 0);
					} catch (Exception e) {
						throw new NodeException(e);
					}
				}
			});
		}
	}

	/**
	 * Assert existence/nonexistence of a class in the class loader by loading it
	 * @param gcl class loader
	 * @param name class name (fully qualified)
	 * @param expectExistence true to expect existence
	 * @return the class if it exists or null
	 */
	protected Class<?> assertClass(GroovyClassLoader gcl, String name, boolean expectExistence) {
		try {
			Class<?> clazz = gcl.loadClass(name);
			if (!expectExistence) {
				fail("Unexpectedly loaded class %s".formatted(name));
			}
			return clazz;
		} catch (ClassNotFoundException e) {
			if (expectExistence) {
				fail("Could not find class %s".formatted(name));
			}
		}
		return null;
	}
}
