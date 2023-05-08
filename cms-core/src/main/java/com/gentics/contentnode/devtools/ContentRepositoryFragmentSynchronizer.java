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
import com.gentics.contentnode.devtools.model.EnhancedCRFModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.AbstractFactory;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.devtools.ContentRepositoryFragmentInPackage;

/**
 * Synchronizer implementation for Cr Fragments
 */
public class ContentRepositoryFragmentSynchronizer extends AbstractSynchronizer<CrFragment, EnhancedCRFModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<CrFragment>, ContentRepositoryFragmentInPackage> TRANSFORM2REST = object -> {
		ContentRepositoryFragmentInPackage restModel = new ContentRepositoryFragmentInPackage();
		AbstractFactory.update(restModel, object.object);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public ContentRepositoryFragmentSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, CrFragment.class, EnhancedCRFModel.class, basePath);
	}

	@Override
	protected CrFragment transform(EnhancedCRFModel from, CrFragment to, boolean shallow) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!ObjectTransformer.isEmpty(from.getGlobalId())) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}

		to.fromModel(from);

		if (!shallow) {
			List<CrFragmentEntry> entries = to.getEntries();
			entries.clear();

			if (from.getEntries() != null) {
			for (ContentRepositoryFragmentEntryModel model : from.getEntries()) {
				CrFragmentEntry editableEntry = t.getObject(CrFragmentEntry.class, model.getGlobalId(), true);
				if (editableEntry == null) {
					editableEntry = t.createObject(CrFragmentEntry.class);
					if (!ObjectTransformer.isEmpty(model.getGlobalId())) {
						editableEntry.setGlobalId(new GlobalId(model.getGlobalId()));
					}
				}
				editableEntry.fromModel(model);
				entries.add(editableEntry);
			}
		}
		}

		return to;
	}

	@Override
	protected EnhancedCRFModel transform(CrFragment from, EnhancedCRFModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		AbstractFactory.update(to, from);

		// remove the local id
		to.setId(null);

		List<ContentRepositoryFragmentEntryModel> entries = new ArrayList<>();
		for (CrFragmentEntry entry : from.getEntries()) {
			ContentRepositoryFragmentEntryModel model = AbstractFactory.update(new ContentRepositoryFragmentEntryModel(), entry);
			model.setId(null);
			entries.add(model);
		}
		to.setEntries(entries);

		return to;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.devtools.AbstractSynchronizer#internalSyncToFilesystem(com.gentics.contentnode.devtools.SynchronizableNodeObject, java.nio.file.Path)
	 */
	@Override
	protected void internalSyncToFilesystem(CrFragment object, Path folder) throws NodeException {
		// convert to (clean) rest model
		EnhancedCRFModel model = transform(object, new EnhancedCRFModel());

		try {
			// parse existing model (if structure file exists)
			EnhancedCRFModel existing = parseStructureFile(folder);
			// fix sorting of entries according to existing model
			fixSorting(model, existing, EnhancedCRFModel::getEntries, t -> Arrays.asList(t.getGlobalId(), t.getMapname()));
		} catch (Exception e) {
			// ignore exception (which is thrown, when structure file does not exist, are cannot be parsed)
		}

		jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.devtools.AbstractSynchronizer#internalSyncFromFilesystem(com.gentics.contentnode.devtools.SynchronizableNodeObject, java.nio.file.Path)
	 */
	@Override
	protected CrFragment internalSyncFromFilesystem(CrFragment object, Path folder, CrFragment master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		EnhancedCRFModel model;
		try {
			model = mapper().readValue(structureFile, EnhancedCRFModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		String folderName = Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC);
		if (!ObjectTransformer.isEmpty(model.getName()) && !ObjectTransformer.equals(model.getName(), folderName)) {
			throw new NodeException(String.format("Cannot synchronize %s into cms: name must be %s, but was %s", structureFile, folderName, model.getName()));
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		CrFragment editable = null;
		if (object == null) {
			editable = t.createObject(CrFragment.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		model.setName(folderName);
		transform(model, editable, false);

		editable.save();

		object = t.getObject(editable);

		return object;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.devtools.AbstractSynchronizer#isHandled(java.lang.String)
	 */
	@Override
	public boolean isHandled(String filename) {
		if (ObjectTransformer.isEmpty(filename)) {
			return false;
		}
		return filename.equals(STRUCTURE_FILE);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.devtools.AbstractSynchronizer#getSyncTargetName(com.gentics.contentnode.devtools.SynchronizableNodeObject)
	 */
	@Override
	public String getSyncTargetName(CrFragment object) throws NodeException {
		return object.getName();
	}

	@Override
	protected void assign(CrFragment object, Node node, boolean isNew) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ContentRepository cr = node.getContentRepository();
		if (cr != null) {
			if (!cr.getAssignedFragments().contains(object)) {
				ContentRepository editableCr = t.getObject(cr, true);
				editableCr.getAssignedFragments().add(object);
				editableCr.save();
			}
		}
	}
}
