package com.gentics.contentnode.tests.devtools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.AbstractSynchronizer;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;

/**
 * Static helper class for devtool specific tests
 */
public class DevToolTestUtils {
	/**
	 * Extract the globalId from the structure file found in the given object directory or the structure file
	 * @param file either the object directory, or the structure file itself
	 * @return global ID
	 * @throws NodeException
	 */
	public static String getGlobalId(File file) throws NodeException {
		assertThat(file).as("File").isNotNull();

		if (file.isDirectory()) {
			return getGlobalId(new File(file, AbstractSynchronizer.STRUCTURE_FILE));
		} else {
			assertThat(file).as("Structure File").hasName(AbstractSynchronizer.STRUCTURE_FILE);

			// extract the globalid from the gentics_structure.json file
			ObjectMapper mapper = new ObjectMapper();
			try (InputStream in = new FileInputStream(file)) {
				JsonNode structure = mapper.readTree(in);
				return structure.get("globalId").asText();
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}
	}

	/**
	 * Try cleaning the directory multiple times, because on some filesystems, it might take
	 * a while until all handles are freed
	 * @param directory directory to clean
	 * @param maxWaitMs maximum time to wait in ms
	 * @param waitSleepMs time to sleep in between tries in ms
	 * @throws InterruptedException
	 */
	public static void clean(File directory, int maxWaitMs, int waitSleepMs) throws InterruptedException {
		boolean success = false;
		long start = System.currentTimeMillis();
		while (!success && (System.currentTimeMillis() - start) < maxWaitMs) {
			try {
				FileUtils.cleanDirectory(directory);
				success = true;
			} catch (IOException e) {
				Thread.sleep(waitSleepMs);
			}
		}
		if (!success) {
			fail(String.format("Failed to clean %s in %d ms", directory.getAbsolutePath(), maxWaitMs));
		}
	}

	/**
	 * Get the name of the object directory for the given object
	 * @param o object
	 * @return name of the object directory
	 * @throws NodeException
	 */
	public static String getObjectDirectoryName(SynchronizableNodeObject o) throws NodeException {
		if (o instanceof Construct) {
			Construct construct = (Construct) o;
			return construct.getKeyword();
		} else if (o instanceof Datasource) {
			Datasource datasource = (Datasource) o;
			return datasource.getName();
		} else if (o instanceof ObjectTagDefinition) {
			ObjectTagDefinition objectTagDefinition = (ObjectTagDefinition) o;
			ObjectTag tag = objectTagDefinition.getObjectTag();
			String typeString = ObjectTagDefinitionTypeModel.fromValue(tag.getObjType()).toString();
			return String.format("%s.%s", typeString, tag.getName());
		} else if (o instanceof Template) {
			Template template = (Template) o;
			return template.getName();
		} else if (o instanceof CrFragment) {
			CrFragment fragment = (CrFragment) o;
			return fragment.getName();
		} else if (o instanceof ContentRepository) {
			ContentRepository cr = (ContentRepository) o;
			return cr.getName();
		} else {
			throw new NodeException(String.format("Unexpected object %s", o));
		}
	}

	/**
	 * Get the structure file for the given object in the package (even if it does not exist)
	 * @param packageRoot root directory of the package
	 * @param o object
	 * @return structure file
	 * @throws NodeException
	 */
	public static File getStructureFile(File packageRoot, SynchronizableNodeObject o) throws NodeException {
		String dir = MainPackageSynchronizer.directoryMap.get(o.getObjectInfo().getObjectClass());
		assertThat(dir).as(String.format("container directory for %s", o)).isNotNull();
		File objectContainer = new File(packageRoot, dir);
		if (o.isMaster()) {
			return new File(objectContainer, getObjectDirectoryName(o) + "/" + AbstractSynchronizer.STRUCTURE_FILE);
		} else {
			String channelName = ((LocalizableNodeObject<?>)o).getChannel().getFolder().getName();
			return new File(objectContainer, getObjectDirectoryName(o.getMaster()) + "/" + MainPackageSynchronizer.CHANNELS_DIR + "/" + channelName + "/"
					+ getObjectDirectoryName(o) + "/" + AbstractSynchronizer.STRUCTURE_FILE);
		}
	}

	/**
	 * Get a map of globalId to object name of objects found in the given object container directory
	 * @param objectContainer object container directory
	 * @return object map
	 * @throws NodeException
	 */
	public static Map<String, String> getObjects(File objectContainer) throws NodeException {
		Map<String, String> objectMap = new HashMap<>();
		for (File objectDir : objectContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."))) {
			objectMap.put(getGlobalId(objectDir), objectDir.getName());
		}
		return objectMap;
	}

	/**
	 * Load the objects with given globalIds, transform them and return
	 * @param clazz object class
	 * @param globalIds collection of global IDs
	 * @param transform transformator
	 * @return collection of transformed objects
	 * @throws NodeException
	 */
	public static <T extends SynchronizableNodeObject, R> Collection<R> loadObjects(Class<T> clazz, Collection<String> globalIds,
			Function<T, R> transform) throws NodeException {
		return TransactionManager.getCurrentTransaction().getObjectsByStringIds(clazz, globalIds).stream().map(c -> {
			try {
				return transform.apply(c);
			} catch (Exception e) {
				fail(String.format("Could not determine object directory name for %s", c));
				return null;
			}
		}).collect(Collectors.toList());
	}

	/**
	 * Write the given object as JSON to the file
	 * @param object object to write
	 * @param file file to write to
	 * @throws NodeException
	 */
	public static void jsonToFile(Object object, File file) throws NodeException {
		try (OutputStream out = new FileOutputStream(file); JsonGenerator jg = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
			ObjectMapper mapper = new ObjectMapper();
			DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
			jg.setPrettyPrinter(prettyPrinter);
			mapper.setSerializationInclusion(Include.NON_EMPTY);
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.writeValue(jg, object);
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + object + " to fs", e);
		}
	}
}
