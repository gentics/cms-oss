package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.fixSorting;
import static com.gentics.contentnode.devtools.Synchronizer.mapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.AbstractCRModel;
import com.gentics.contentnode.devtools.model.MeshCRModel;
import com.gentics.contentnode.devtools.model.NonMeshCRModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.devtools.ContentRepositoryInPackage;

/**
 * Synchronizer implementation for ContentRepositories
 */
public class ContentRepositorySynchronizer extends AbstractSynchronizer<ContentRepository, AbstractCRModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<ContentRepository>, ContentRepositoryInPackage> TRANSFORM2REST = object -> {
		ContentRepositoryInPackage restModel = new ContentRepositoryInPackage();
		ContentRepository.NODE2REST.apply(object.object, restModel);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public ContentRepositorySynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, ContentRepository.class, AbstractCRModel.class, basePath);
	}

	@Override
	protected ContentRepository transform(AbstractCRModel from, ContentRepository to, boolean shallow) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!ObjectTransformer.isEmpty(from.getGlobalId())) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}

		ContentRepository.REST2NODE.apply(from, to);

		if (!shallow) {
			List<TagmapEntry> entries = to.getEntries();
			entries.clear();

			for (TagmapEntryModel model : from.getEntries()) {
				TagmapEntry editableEntry = t.getObject(TagmapEntry.class, model.getGlobalId(), true);
				if (editableEntry == null) {
					editableEntry = t.createObject(TagmapEntry.class);
					if (!ObjectTransformer.isEmpty(model.getGlobalId())) {
						editableEntry.setGlobalId(new GlobalId(model.getGlobalId()));
					}
				}
				editableEntry = TagmapEntry.REST2NODE.apply(model, editableEntry);
				entries.add(editableEntry);
			}
		}

		return to;
	}

	@Override
	protected AbstractCRModel transform(ContentRepository from, AbstractCRModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		ContentRepository.NODE2REST.apply(from, to);

		List<TagmapEntryModel> entries = new ArrayList<>();
		for (TagmapEntry entry : from.getEntries()) {
			TagmapEntryModel model = TagmapEntry.NODE2REST.apply(entry, new TagmapEntryModel());
			model.setId(null);
			entries.add(model);
		}
		to.setEntries(entries);

		return to;
	}

	@Override
	protected void internalSyncToFilesystem(ContentRepository object, Path folder) throws NodeException {
		if (object == null) {
			throw new NodeException("Cannot transform from null");
		}

		// convert to (clean) rest model
		AbstractCRModel model = null;

		switch (object.getCrType()) {
		case mesh:
			model = new MeshCRModel();
			break;
		default:
			model = new NonMeshCRModel();
			break;
		}

		model = transform(object, model);

		try {
			// parse existing model (if structure file exists)
			AbstractCRModel existing = parseStructureFile(folder);
			// fix sorting of entries according to existing model
			fixSorting(model, existing, AbstractCRModel::getEntries, t -> Arrays.asList(t.getGlobalId(), t.getMapname()));
		} catch (Exception e) {
			// ignore exception (which is thrown, when structure file does not exist, are cannot be parsed)
		}

		jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));
	}

	@Override
	protected ContentRepository internalSyncFromFilesystem(ContentRepository object, Path folder, ContentRepository master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		AbstractCRModel model;
		try {
			model = mapper().readValue(structureFile, AbstractCRModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		String folderName = Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC);
		if (!ObjectTransformer.isEmpty(model.getName()) && !ObjectTransformer.equals(model.getName(), folderName)) {
			throw new NodeException(String.format("Cannot synchronize %s into cms: name must be %s, but was %s", structureFile, folderName, model.getName()));
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		ContentRepository editable = null;
		if (object == null) {
			editable = t.createObject(ContentRepository.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		model.setName(folderName);
		transform(model, editable, false);

		editable.save();

		return t.getObject(editable);
	}

	@Override
	public boolean isHandled(String filename) {
		if (ObjectTransformer.isEmpty(filename)) {
			return false;
		}
		return filename.equals(STRUCTURE_FILE);
	}

	@Override
	public String getSyncTargetName(ContentRepository object) throws NodeException {
		return object.getName();
	}

	@Override
	protected void assign(ContentRepository object, Node node, boolean isNew) throws NodeException {
		// assign CR to nodes of the package (but only if node does not have a CR yet and package only contains a single CR)
		if (packageSynchronizer.getObjects(ContentRepository.class).size() == 1) {
			if (node.getContentRepository() == null) {
				Transaction t = TransactionManager.getCurrentTransaction();

				Node editableNode = t.getObject(node, true);
				editableNode.setContentrepositoryId(object.getId());
				editableNode.save();
			}
		}
	}
}
