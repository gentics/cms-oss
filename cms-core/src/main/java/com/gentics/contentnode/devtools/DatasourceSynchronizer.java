package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;
import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.DatasourceModel;
import com.gentics.contentnode.devtools.model.DatasourceTypeModel;
import com.gentics.contentnode.devtools.model.DatasourceValueModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.rest.model.devtools.DatasourceInPackage;

/**
 * Synchronizer implementation of Datasources
 */
public class DatasourceSynchronizer extends AbstractSynchronizer<Datasource, DatasourceModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<Datasource>, DatasourceInPackage> TRANSFORM2REST = object -> {
		DatasourceInPackage restModel = new DatasourceInPackage();
		Datasource.NODE2REST.apply(object.object, restModel);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public DatasourceSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, Datasource.class, DatasourceModel.class, basePath);
	}

	@Override
	public String getSyncTargetName(Datasource object) throws NodeException {
		return object.getName();
	}

	@Override
	protected void internalSyncToFilesystem(Datasource object, Path folder) throws NodeException {
		// convert to (clean) rest model
		DatasourceModel model = transform(object, new DatasourceModel());
		jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));
	}

	@Override
	protected Datasource internalSyncFromFilesystem(Datasource object, Path folder, Datasource master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		DatasourceModel model;
		try {
			model = mapper().readValue(structureFile, DatasourceModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		// TODO check consistency of rest model

		Transaction t = TransactionManager.getCurrentTransaction();

		Datasource editable = null;
		if (object == null) {
			editable = t.createObject(Datasource.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		model.setName(Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC));
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
	protected DatasourceModel transform(Datasource from, DatasourceModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return Datasource.NODE2DEVTOOL.apply(from, to);
	}

	@Override
	protected Datasource transform(DatasourceModel from, Datasource to, boolean shallow) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setName(from.getName());
		to.setSourceTypeVal(from.getType().getTypeValue());

		List<DatasourceEntry> entries = to.getEntries();
		AtomicInteger maxDsId = new AtomicInteger(entries.stream().mapToInt(DatasourceEntry::getDsid).max().orElse(-1));
		entries.clear();

		// add values
		unwrap(() -> {
			if (from.getValues() != null) {
				from.getValues().forEach(v -> {
					wrap(() -> {
						boolean isNew = false;
						DatasourceEntry editableEntry = t.getObject(DatasourceEntry.class, v.getGlobalId(), true);
						if (editableEntry == null) {
							editableEntry = t.createObject(DatasourceEntry.class);
							isNew = true;
						}
						transform(v, editableEntry);
						// set a new DsId when synchronizing from an old package (that did not contain the dsids)
						if (isNew && editableEntry.getDsid() < 0) {
							editableEntry.setDsid(maxDsId.incrementAndGet());
						}
						entries.add(editableEntry);
					});
				});
			}
		});

		return to;
	}

	@Override
	protected void assign(Datasource object, Node node, boolean isNew) throws NodeException {
	}

	/**
	 * Transform the given datasource entry into the REST model
	 * @param from datasource entry
	 * @param to model
	 * @return model
	 * @throws NodeException
	 */
	protected DatasourceValueModel transform(DatasourceEntry from, DatasourceValueModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return DatasourceEntry.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into a datasource entry
	 * @param from model
	 * @param to datasource entry
	 * @return datasource entry
	 * @throws NodeException
	 */
	protected DatasourceEntry transform(DatasourceValueModel from, DatasourceEntry to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setKey(from.getKey());
		to.setValue(from.getValue());
		to.setDsid(from.getDsId());

		return to;
	}
}
