package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.PortalImplementationModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PortalImplementationFileFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.rest.model.devtools.PortalImplementationInPackage;

/**
 * Synchronizer implementation for Portal Implementations.<br>
 * The attributes of the Portal Implementation are stored in the structure file, the files of the Portal Implementation
 * are stored as plain files in the subfolder {@link #FILES_DIR} (folders in the Portal Implementation are subfolders there).
 */
public class PortalImplementationSynchronizer extends AbstractSynchronizer<PortalImplementation, PortalImplementationModel> {
	/**
	 * Name of the subfolder containing the files of the Portal Implementation
	 */
	public final static String FILES_DIR = "files";

	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<PortalImplementation>, PortalImplementationInPackage> TRANSFORM2REST = object -> {
		PortalImplementation impl = object.object;
		PortalImplementationInPackage restModel = new PortalImplementationInPackage();
		com.gentics.contentnode.rest.model.PortalImplementationModel model = PortalImplementation.TRANSFORM2REST.apply(impl);
		restModel.setId(model.getId());
		restModel.setGlobalId(model.getGlobalId());
		restModel.setKeyword(model.getKeyword());
		restModel.setName(model.getName());
		restModel.setDescription(model.getDescription());
		restModel.setType(model.getType());
		restModel.setCreator(model.getCreator());
		restModel.setCdate(model.getCdate());
		restModel.setEditor(model.getEditor());
		restModel.setEdate(model.getEdate());
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public PortalImplementationSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, PortalImplementation.class, PortalImplementationModel.class, basePath);
	}

	@Override
	protected PortalImplementation transform(PortalImplementationModel from, PortalImplementation to, boolean shallow) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		if (!ObjectTransformer.isEmpty(from.getGlobalId())) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setKeyword(from.getKeyword());
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setType(from.getType());

		return to;
	}

	@Override
	protected PortalImplementationModel transform(PortalImplementation from, PortalImplementationModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		if (from.getGlobalId() != null) {
			to.setGlobalId(from.getGlobalId().toString());
		}
		to.setKeyword(from.getKeyword());
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setType(from.getType());

		return to;
	}

	@Override
	protected void internalSyncToFilesystem(PortalImplementation object, Path folder) throws NodeException {
		jsonToFile(transform(object, new PortalImplementationModel()), new File(folder.toFile(), STRUCTURE_FILE));

		Path filesDir = folder.resolve(FILES_DIR);
		Map<String, Path> existing = getFilesOnDisk(filesDir);

		try {
			for (PortalImplementationFile file : object.getFiles(null)) {
				Path target = existing.remove(file.getPath());
				if (target == null) {
					target = toFilesystemPath(filesDir, file.getPath());
				}
				Path source = file.getBinFile().toPath();
				// only write the file, if the contents changed (to avoid unnecessary filesystem events)
				if (!Files.isRegularFile(target) || Files.size(source) != Files.size(target) || Files.mismatch(source, target) != -1L) {
					Files.createDirectories(target.getParent());
					Files.copy(source, target, REPLACE_EXISTING);
				}
			}

			// remove files, which do not exist in the CMS
			for (Path obsolete : existing.values()) {
				Files.delete(obsolete);
			}

			// remove folders, which became empty
			if (Files.isDirectory(filesDir)) {
				List<Path> dirs;
				try (Stream<Path> stream = Files.walk(filesDir)) {
					dirs = stream.filter(p -> !p.equals(filesDir) && Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
							.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
				}
				for (Path dir : dirs) {
					try (Stream<Path> children = Files.list(dir)) {
						if (children.findAny().isEmpty()) {
							Files.delete(dir);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new NodeException(String.format("Error while synchronizing the files of %s into %s", object, filesDir), e);
		}
	}

	@Override
	protected PortalImplementation internalSyncFromFilesystem(PortalImplementation object, Path folder, PortalImplementation master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		PortalImplementationModel model;
		try {
			model = mapper().readValue(structureFile, PortalImplementationModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		String folderName = Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC);
		if (!ObjectTransformer.isEmpty(model.getKeyword()) && !ObjectTransformer.equals(model.getKeyword(), folderName)) {
			throw new NodeException(
					String.format("Cannot synchronize %s into cms: keyword must be %s, but was %s", structureFile, folderName, model.getKeyword()));
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		PortalImplementation editable = null;
		if (object == null) {
			editable = t.createObject(PortalImplementation.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		model.setKeyword(folderName);
		transform(model, editable, false);

		editable.save();

		object = t.getObject(editable);

		syncFilesFromFilesystem(object, folder.resolve(FILES_DIR));

		return object;
	}

	/**
	 * Synchronize the files from the filesystem into the given Portal Implementation.
	 * Files are identified by their path, only files with changed contents are updated, files that do not exist in the filesystem are deleted.
	 * @param impl Portal Implementation
	 * @param filesDir directory containing the files
	 * @throws NodeException
	 */
	protected void syncFilesFromFilesystem(PortalImplementation impl, Path filesDir) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<String, Path> onDisk = getFilesOnDisk(filesDir);

		// validate all paths first, so that the synchronization fails before anything was changed
		for (String path : onDisk.keySet()) {
			PortalImplementationFileFactory.validatePath(path);
		}

		try {
			for (PortalImplementationFile file : impl.getFiles(null)) {
				Path source = onDisk.remove(file.getPath());
				if (source == null) {
					t.getObject(file, true).delete();
				} else {
					Path binFile = file.getBinFile().toPath();
					if (!Files.isRegularFile(binFile) || Files.size(source) != Files.size(binFile) || Files.mismatch(source, binFile) != -1L) {
						PortalImplementationFile editable = t.getObject(file, true);
						try (InputStream in = Files.newInputStream(source)) {
							editable.setContents(in);
						}
						editable.save();
					}
				}
			}

			for (Map.Entry<String, Path> entry : onDisk.entrySet()) {
				PortalImplementationFile file = t.createObject(PortalImplementationFile.class);
				file.setPortalImplementationId(impl.getId());
				file.setPath(entry.getKey());
				try (InputStream in = Files.newInputStream(entry.getValue())) {
					file.setContents(in);
				}
				file.save();
			}
		} catch (IOException e) {
			throw new NodeException(String.format("Error while synchronizing the files of %s from %s", impl, filesDir), e);
		}
	}

	/**
	 * Get the regular files (no symbolic links) contained in the given directory (recursively)
	 * @param filesDir directory
	 * @return map of relative paths (segments separated by /, normalized to NFC) to the files. Empty, if the directory does not exist
	 * @throws NodeException
	 */
	protected Map<String, Path> getFilesOnDisk(Path filesDir) throws NodeException {
		Map<String, Path> files = new HashMap<>();
		if (!Files.isDirectory(filesDir)) {
			return files;
		}
		try (Stream<Path> stream = Files.walk(filesDir)) {
			stream.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).forEach(p -> {
				String relativePath = StreamSupport.stream(filesDir.relativize(p).spliterator(), false).map(Path::toString)
						.collect(Collectors.joining(PortalImplementationFile.SEPARATOR));
				files.put(Normalizer.normalize(relativePath, Normalizer.Form.NFC), p);
			});
		} catch (IOException e) {
			throw new NodeException(String.format("Error while reading files from %s", filesDir), e);
		}
		return files;
	}

	/**
	 * Get the filesystem path for the given file path
	 * @param filesDir directory containing the files
	 * @param path file path (segments separated by /)
	 * @return filesystem path
	 */
	protected Path toFilesystemPath(Path filesDir, String path) {
		Path target = filesDir;
		for (String segment : path.split(PortalImplementationFile.SEPARATOR)) {
			target = target.resolve(segment);
		}
		return target;
	}

	@Override
	public boolean isHandled(String filename) {
		if (ObjectTransformer.isEmpty(filename)) {
			return false;
		}
		return filename.equals(STRUCTURE_FILE);
	}

	@Override
	public String getSyncTargetName(PortalImplementation object) throws NodeException {
		return object.getKeyword();
	}

	@Override
	protected void assign(PortalImplementation object, Node node, boolean isNew) throws NodeException {
		// Portal Implementations contained in a package are assigned to all nodes, the package is assigned to
		node.addPortalImplementation(object);
	}
}
