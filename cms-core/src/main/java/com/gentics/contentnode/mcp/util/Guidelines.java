package com.gentics.contentnode.mcp.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.devtools.PackageFile;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;

import jakarta.ws.rs.core.MediaType;

/**
 * Guidelines are Markdown files in the internal files of devtools packages, named
 * {@code guidelines/[<prio>_]<name>[.default].md}, with the ID {@code <package>/<name>[.default]}.
 */
public final class Guidelines {
	/**
	 * Directory of the guidelines in the files storage
	 */
	static final String DIR = "guidelines";

	/**
	 * Allowed name of a new guideline
	 */
	public static final String NAME_PATTERN = "^(?!\\d+_)[A-Za-z0-9][A-Za-z0-9_-]{0,127}$";

	/**
	 * Pattern of a file name with a priority
	 */
	private static final Pattern PRIO = Pattern.compile("^(\\d+)_(.*)$");

	/**
	 * Pattern of a file name without a priority
	 */
	private static final Pattern FILE_NAME = Pattern.compile("^([^.].*?)(\\.default)?\\.md$");

	/**
	 * Suffix of the name of a default guideline
	 */
	private static final String DEFAULT_SUFFIX = ".default";

	/**
	 * One guideline
	 * @param id ID
	 * @param packageName package
	 * @param name name
	 * @param isDefault whether it applies to every session of the package's nodes
	 * @param priority priority, may be null
	 * @param fileName name of the file
	 */
	@JsonInclude(Include.NON_NULL)
	public record Guideline(String id, @JsonProperty("package") String packageName, String name,
			@JsonProperty("default") boolean isDefault, Integer priority,
			@JsonIgnore String fileName) {
	}

	private Guidelines() {
	}

	/**
	 * Get the ID of a guideline
	 * @param packageName package
	 * @param name name
	 * @param isDefault whether it is a default guideline
	 * @return ID
	 */
	public static String id(String packageName, String name, boolean isDefault) {
		return packageName + "/" + name + (isDefault ? DEFAULT_SUFFIX : "");
	}

	/**
	 * Get the file name of a guideline
	 * @param name name
	 * @param isDefault whether it is a default guideline
	 * @param priority priority, may be null
	 * @return file name
	 */
	public static String fileName(String name, boolean isDefault, Integer priority) {
		return (priority != null ? priority + "_" : "") + name + (isDefault ? DEFAULT_SUFFIX : "") + ".md";
	}

	/**
	 * Get the guideline a file is
	 * @param packageName package
	 * @param fileName file name
	 * @return guideline, null for a file that does not follow the naming rule
	 */
	public static Guideline ofFile(String packageName, String fileName) {
		Matcher prio = PRIO.matcher(fileName);
		boolean hasPrio = prio.matches();
		Matcher file = FILE_NAME.matcher(hasPrio ? prio.group(2) : fileName);
		if (!file.matches()) {
			return null;
		}
		boolean isDefault = file.group(2) != null;
		return new Guideline(id(packageName, file.group(1), isDefault), packageName, file.group(1), isDefault,
				hasPrio ? Integer.valueOf(prio.group(1)) : null, fileName);
	}

	/**
	 * Get the package of a guideline ID
	 * @param id ID
	 * @return package
	 * @throws IllegalArgumentException if the ID is no {@code <package>/<name>}
	 */
	public static String packageOf(String id) {
		int separator = id.indexOf('/');
		if (separator <= 0 || separator == id.length() - 1 || id.indexOf('/', separator + 1) >= 0) {
			throw new IllegalArgumentException("'%s' is no guideline ID, expected <package>/<name>[.default]".formatted(id));
		}
		return id.substring(0, separator);
	}

	/**
	 * Sort guidelines by priority, those without one last, then by ID, keeping only the first of those with one ID
	 * @param guidelines guidelines
	 * @return sorted guidelines
	 */
	public static List<Guideline> sorted(List<Guideline> guidelines) {
		List<Guideline> sorted = new ArrayList<>(guidelines);
		sorted.sort(Comparator.comparing((Guideline g) -> g.priority() != null ? g.priority() : Integer.MAX_VALUE)
				.thenComparing(Guideline::id));
		List<Guideline> result = new ArrayList<>();
		for (Guideline guideline : sorted) {
			if (result.stream().noneMatch(g -> g.id().equals(guideline.id()))) {
				result.add(guideline);
			}
		}
		return result;
	}

	/**
	 * List the guidelines of a package
	 * @param resource package resource
	 * @param packageName package
	 * @return guidelines, sorted
	 * @throws Exception if the package cannot be read
	 */
	public static List<Guideline> inPackage(PackageResource resource, String packageName) throws Exception {
		List<PackageFile> files;
		try {
			files = resource.listFiles(packageName, PackageSynchronizer.FILES_INTERNAL_DIR, DIR).getItems();
		} catch (EntityNotFoundException e) {
			// a package without the directory has no guidelines, but a missing package must still fail
			resource.listFiles(packageName, PackageSynchronizer.FILES_INTERNAL_DIR, "");
			return List.of();
		}
		List<Guideline> guidelines = new ArrayList<>();
		for (PackageFile file : files) {
			Guideline guideline = file.isDirectory() ? null : ofFile(packageName, file.getName());
			if (guideline != null) {
				guidelines.add(guideline);
			}
		}
		return sorted(guidelines);
	}

	/**
	 * Find a guideline
	 * @param resource package resource
	 * @param id ID
	 * @return guideline
	 * @throws EntityNotFoundException if there is none
	 * @throws Exception if the package cannot be read
	 */
	public static Guideline find(PackageResource resource, String id) throws Exception {
		for (Guideline guideline : inPackage(resource, packageOf(id))) {
			if (guideline.id().equals(id)) {
				return guideline;
			}
		}
		throw new EntityNotFoundException("Guideline '%s' does not exist".formatted(id));
	}

	/**
	 * Read the text of a guideline
	 * @param resource package resource
	 * @param guideline guideline
	 * @return text
	 * @throws Exception if the file cannot be read
	 */
	public static String read(PackageResource resource, Guideline guideline) throws Exception {
		Object file = resource.getFile(guideline.packageName(), PackageSynchronizer.FILES_INTERNAL_DIR,
				DIR + "/" + guideline.fileName()).getEntity();
		return Files.readString(((File) file).toPath(), StandardCharsets.UTF_8);
	}

	/**
	 * Write a guideline file
	 * @param resource package resource
	 * @param packageName package
	 * @param fileName file name
	 * @param content text
	 * @throws Exception if the file cannot be written
	 */
	public static void write(PackageResource resource, String packageName, String fileName, String content)
			throws Exception {
		BodyPart part = new BodyPart(content, MediaType.TEXT_PLAIN_TYPE)
				.contentDisposition(ContentDisposition.type("form-data").fileName(fileName).build());
		try (MultiPart multiPart = new MultiPart()) {
			multiPart.bodyPart(part);
			resource.saveFile(packageName, PackageSynchronizer.FILES_INTERNAL_DIR, DIR, multiPart);
		}
	}

	/**
	 * Delete a guideline file
	 * @param resource package resource
	 * @param guideline guideline
	 * @throws Exception if the file cannot be deleted
	 */
	public static void delete(PackageResource resource, Guideline guideline) throws Exception {
		resource.deleteFile(guideline.packageName(), PackageSynchronizer.FILES_INTERNAL_DIR,
				DIR + "/" + guideline.fileName());
	}
}
