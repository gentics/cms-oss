package com.gentics.contentnode.tests.parttype.groovy;

import static com.gentics.contentnode.factory.Trx.operate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;
import com.gentics.contentnode.utils.GroovyUtils;

import groovy.lang.GroovyClassLoader;

/**
 * Test cases for availability of scripts from devtool packages
 */
public class GroovyPackageScriptTest extends AbstractGroovyTest {
	public final static String EXPECTED_ORIGINAL_SCRIPT_RESULT = "This is the script from testpackage called with param [%s]";

	public final static String EXPECTED_MODIFIED_SCRIPT_RESULT = "This is the modified script from testpackage called with param [%s]";

	/**
	 * Name of the script file
	 */
	public final static String SCRIPTFILE_NAME = "script.groovy";

	/**
	 * Fully qualified name of the class (compiled from the script file)
	 */
	public final static String CLASS_NAME = "testpackage.script";

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(TESTPACKAGE_NAME);
	}

	/**
	 * Test that adding a package to a node makes the scripts of the packages available in the {@link GroovyClassLoader}
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testAddPackageToNode() throws NodeException, IOException {
		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), List.of(SCRIPTFILE_NAME));

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();

			assertClass(gcl, CLASS_NAME, false);
		}

		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			assertClass(gcl, CLASS_NAME, true);
		}
	}

	/**
	 * Test that removing a package from a node removes the scripts of the package from the {@link GroovyClassLoader}
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testRemovePackageFromNode() throws NodeException, IOException {
		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), List.of(SCRIPTFILE_NAME));
		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			assertClass(gcl, CLASS_NAME, true);
		}

		operate(() -> Synchronizer.removePackage(node, TESTPACKAGE_NAME));

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			assertClass(gcl, CLASS_NAME, false);
		}
	}

	/**
	 * Test that changing the contents of a script gets the script re-compiled
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testChangeScriptInPackage() throws NodeException, IOException {
		prepareScripts(Synchronizer.getPackage(TESTPACKAGE_NAME), List.of(SCRIPTFILE_NAME));
		operate(() -> Synchronizer.addPackage(node, TESTPACKAGE_NAME));
		Class<?> originalClass;

		// load the class and execute the script
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			originalClass = assertClass(gcl, CLASS_NAME, true);

			String result = ObjectTransformer.getString(GroovyUtils.call(gcl, CLASS_NAME, script -> {
				script.setProperty("param", "call #1");
			}), null);

			assertThat(result).as("Script result").isEqualTo(EXPECTED_ORIGINAL_SCRIPT_RESULT.formatted("call #1"));
		}

		// load the class again (should be the same) and execute the script again
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			Class<?> scriptClass = assertClass(gcl, CLASS_NAME, true);

			assertThat(scriptClass).as("Script class").isEqualTo(originalClass);

			String result = ObjectTransformer.getString(GroovyUtils.call(gcl, CLASS_NAME, script -> {
				script.setProperty("param", "call #2");
			}), null);

			assertThat(result).as("Script result").isEqualTo(EXPECTED_ORIGINAL_SCRIPT_RESULT.formatted("call #2"));
		}

		modifyScript(Synchronizer.getPackage(TESTPACKAGE_NAME), SCRIPTFILE_NAME, content -> {
			return content.replace("This is the script", "This is the modified script");
		});

		// load the class again (should be different) and execute the script again
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(true)) {
			GroovyClassLoader gcl = rTrx.get().getCompilationUnit(node).getClassLoader();
			Class<?> scriptClass = assertClass(gcl, CLASS_NAME, true);

			assertThat(scriptClass).as("Script class").isNotEqualTo(originalClass);

			String result = ObjectTransformer.getString(GroovyUtils.call(gcl, CLASS_NAME, script -> {
				script.setProperty("param", "call #3");
			}), null);

			assertThat(result).as("Script result").isEqualTo(EXPECTED_MODIFIED_SCRIPT_RESULT.formatted("call #3"));
		}
	}
}
