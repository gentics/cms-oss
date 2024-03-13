package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.fixSorting;
import static com.gentics.contentnode.devtools.Synchronizer.mapper;
import static com.gentics.contentnode.devtools.Synchronizer.unwrap;
import static com.gentics.contentnode.devtools.Synchronizer.wrap;

import com.gentics.contentnode.etc.Consumer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.ConstructCategoryModel;
import com.gentics.contentnode.devtools.model.ConstructModel;
import com.gentics.contentnode.devtools.model.PartModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.devtools.ConstructInPackage;
import com.gentics.lib.util.FileUtil;

/**
 * Synchronizer implementation for constructs
 */
public class ConstructSynchronizer extends AbstractSynchronizer<Construct, ConstructModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<Construct>, ConstructInPackage> TRANSFORM2REST = object -> {
		ConstructInPackage restModel = new ConstructInPackage();
		Construct.NODE2REST.apply(object.object, restModel);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Embed the construct category into the REST model
	 */
	public final static Consumer<ConstructInPackage> EMBED_CATEGORY = (restConstruct) -> {
		Transaction t = TransactionManager.getCurrentTransaction();

		ConstructCategory constructCategory = t.getObject(ConstructCategory.class, restConstruct.getCategoryId());
		if (constructCategory == null) {
			return;
		}

		restConstruct.setCategory(ConstructCategory.TRANSFORM2REST.apply(constructCategory));
	};


	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public ConstructSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, Construct.class, ConstructModel.class, basePath);
	}

	@Override
	public boolean isHandled(String filename) {
		if (ObjectTransformer.isEmpty(filename)) {
			return false;
		}
		return filename.equals(STRUCTURE_FILE) || isPartFilename(filename);
	}

	@Override
	public String getSyncTargetName(Construct object) throws NodeException {
		return object.getKeyword();
	}

	/**
	 * Checks if part keywords have to be generated for this construct and if the present keywords are unique.
	 *
	 * Only parts with <code>part.isValueless() == false</code> will be considered.
	 *
	 * @param object The construct which parts should be checked
	 * @return <code>true</code> if there is at least one part with values but without a keyword, and <code>false</code> otherwise.
	 * @throws NodeException When there are multiple value parts with the same keyword
	 */
	private boolean checkPartKeynames(Construct object) throws NodeException {
		boolean createPartKeyname = false;
		Set<String> keynames = new HashSet<>();

		for (Part p : object.getParts()) {
			if (!p.isValueless()) {
				String keyname = p.getKeyname();

				if (ObjectTransformer.isEmpty(keyname)) {
					createPartKeyname = true;
				} else if (keynames.contains(keyname)) {
					String msg = String.format(
						"Cannot synchronize %s into filesystem: keywords of parts must be unique, but multiple parts with keyword %s found",
						object.toString(),
						keyname);

					throw new NodeException(msg);
				} else {
					keynames.add(keyname);
				}
			}
		}

		return createPartKeyname;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void internalSyncToFilesystem(Construct object, Path folder) throws NodeException {
		// first check whether the construct contains any parts that have no keyword, or duplicate keywords.
		Transaction t = TransactionManager.getCurrentTransaction();

		if (checkPartKeynames(object)) {
			object = t.getObject(object, true);
			for (Part p : object.getParts()) {
				if (!p.isValueless() && ObjectTransformer.isEmpty(p.getKeyname())) {
					p.setKeyname(p.getGlobalId().toString());
				}
			}

			object.save();
			object.unlock();
			t.commit(false);
			object = t.getObject(object);
		}

		// convert to (clean) rest model
		ConstructModel model = transform(object, new ConstructModel());

		// convert default values to "part." files
		Set<File> files = new HashSet<>();
		files.addAll(Arrays.asList(folder.toFile().listFiles(f -> f.getName().startsWith("part."))));

		for (Part part : object.getParts()) {
			if (part.isValueless()) {
				continue;
			}

			String partName = part.getKeyname();

			// for some parts, we sync data into files
			switch (part.getPartTypeId()) {
			case Part.SELECTSINGLE:
			case Part.SELECTMULTIPLE:
			case Part.DATASOURCE:
			case Part.HTMLCUSTOMFORM:
			case Part.TEXTCUSTOMFORM:
				File additionalPartFile = new File(folder.toFile(), "part." + partName + ".template.html");
				stringToFile(ObjectTransformer.getString(part.getInfoText(), ""), additionalPartFile);
				files.remove(additionalPartFile);
				break;
			case Part.OVERVIEW:
				OverviewPartSetting overviewSetting = new OverviewPartSetting(part);
				File additionalOverviewPartFile = new File(folder.toFile(), "part." + partName + ".ds.json");
				Map<?, ?> transformed = mapper().convertValue(overviewSetting, Map.class);

				try (InputStream in = new FileInputStream(additionalOverviewPartFile)) {
					// parse existing file into a generic Map
					// we cannot use the model OverviewPartSetting here, because the properties would be returned as Sets, which cannot be sorted
					Map<?, ?> existing = mapper().readValue(in, Map.class);
					// fix sorting of restricted object types
					fixSorting(transformed, existing, json -> {
						Object o = json.get("restrictedObjectTypes");
						if (o instanceof List) {
							return (List<String>) o;
						}
						return null;
					}, string -> Arrays.asList(string));
					// fix sorting of restricted selection types
					fixSorting(transformed, existing, json -> {
						Object o = json.get("restrictedSelectionTypes");
						if (o instanceof List) {
							return (List<String>) o;
						}
						return null;
					}, string -> Arrays.asList(string));
				} catch (Exception e) {
					// ignore exception (which is thrown, when file does not exist, are cannot be parsed)
				}

				jsonToFile(transformed, additionalOverviewPartFile);
				files.remove(additionalOverviewPartFile);
				break;
			default:
				break;
			}

			File outFile = new File(folder.toFile(), getProposedFilename(part));
			storeContents(part.getDefaultValue(), outFile);
			files.remove(outFile);
		}

		// remove superfluous part files
		files.stream().forEach(file -> file.delete());

		// output the remainer into the gentics_structure.json file
		jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));
	}

	@Override
	protected Construct internalSyncFromFilesystem(Construct object, Path folder, Construct master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		ConstructModel model;
		try {
			model = mapper().readValue(structureFile, ConstructModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		// TODO check consistency of rest model
		String folderName = Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC);
		if (!ObjectTransformer.isEmpty(model.getKeyword()) && !ObjectTransformer.equals(model.getKeyword(), folderName)) {
			throw new NodeException(String.format("Cannot synchronize %s into cms: keyword must be %s, but was %s", structureFile, folderName, model.getKeyword()));
		}

		if (ObjectTransformer.isEmpty(model.getName()) || !model.getName().containsKey("en") || !model.getName().containsKey("en")) {
			throw new NodeException("Cannot synchronize " + structureFile + " into cms: name must be given");
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		Construct editable = null;
		if (object == null) {
			editable = t.createObject(Construct.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		model.setKeyword(folderName);
		model.setIcon(ObjectTransformer.getString(model.getIcon(), ""));
		transform(model, editable, false);

		Map<GlobalId, MissingValueReference> missingReferences = new HashMap<>();

		for (Part part : editable.getParts()) {
			if (part.isValueless()) {
				continue;
			}

			String partName = part.getKeyname();
			if (ObjectTransformer.isEmpty(partName)) {
				throw new NodeException("Error while synchronizing " + object + ": " + part + " has no keyword");
			}

			// for some parts, we sync data into files
			switch (part.getPartTypeId()) {
			case Part.SELECTSINGLE:
			case Part.SELECTMULTIPLE:
			case Part.DATASOURCE:
			case Part.HTMLCUSTOMFORM:
			case Part.TEXTCUSTOMFORM:
				File additionalPartFile = new File(folder.toFile(), "part." + partName + ".template.html");
				if (!additionalPartFile.exists()) {
					throw new NodeException("Error while synchronizing " + object + ": " + additionalPartFile + " does not exist");
				}
				try (InputStream in = new FileInputStream(additionalPartFile); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					FileUtil.pooledBufferInToOut(in, out);
					part.setInfoText(out.toString("UTF8"));
				} catch (IOException e) {
					throw new NodeException(e);
				}
				break;
			case Part.OVERVIEW:
				File additionalOverviewPartFile = new File(folder.toFile(), "part." + partName + ".ds.json");
				if (additionalOverviewPartFile.exists()) {
					try (InputStream in = new FileInputStream(additionalOverviewPartFile)) {
						mapper().readValue(in, OverviewPartSetting.class).setTo(part);
					} catch (IOException e) {
						throw new NodeException("Error while parsing " + additionalOverviewPartFile, e);
					}
				}
				break;
			default:
				break;
			}

			File inFile = new File(folder.toFile(), getProposedFilename(part));
			if (!inFile.exists()) {
				throw new NodeException("Error while synchronizing " + object + ": " + inFile + " does not exist");
			}

			// for new parts, we create a new default value
			if (Part.isEmptyId(part.getId()) || Part.isEmptyId(part.getDefaultValue().getId())) {
				Value defaultValue = t.createObject(Value.class);
				defaultValue.setPart(part);
				part.setDefaultValue(defaultValue);
				defaultValue.setContainer(editable);
			}
			readContents(inFile, part.getDefaultValue(), isJsonFile(part), missingReferences);

			// for datasource based parts, we need to set the datasource in the part as well
			switch (part.getPartTypeId()) {
			case Part.SELECTSINGLE:
			case Part.SELECTMULTIPLE:
				part.setInfoInt(part.getDefaultValue().getValueRef());
				break;
			}
		}

		// transform construct category (if set)
		ConstructCategoryModel categoryModel = model.getCategory();
		if (categoryModel != null) {
			ConstructCategory editableCategory = t.getObject(ConstructCategory.class, categoryModel.getGlobalId(), true);
			if (editableCategory == null) {
				editableCategory = t.createObject(ConstructCategory.class);
			}
			transform(categoryModel, editableCategory);
			editableCategory.save();
			editable.setConstructCategoryId(editableCategory.getId());
		}

		editable.save();

		for (Part part : editable.getParts()) {
			MissingValueReference missing = missingReferences.get(part.getGlobalId());

			if (missing != null) {
				addMissingReference(part.getDefaultValue().getGlobalId().toString(), missing.getName(), missing.getTargetGlobalId());
			}
		}

		return t.getObject(editable);
	}

	@Override
	protected ConstructModel transform(Construct from, ConstructModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return Construct.NODE2DEVTOOL.apply(from, to);
	}

	@Override
	protected Construct transform(ConstructModel from, Construct to, boolean shallow) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		I18NHelper.forI18nMap(from.getDescription(), (translation, id) -> to.setDescription(translation, id));
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setHopeditHook(from.getHopeditHook());
		to.setIconName(from.getIcon());
		to.setKeyword(from.getKeyword());
		to.setLiveEditorTagName(from.getLiveEditorTagName());
		I18NHelper.forI18nMap(from.getName(), (translation, id) -> to.setName(translation, id));
		to.setAutoEnable(from.isAutoEnable());
		to.setMayBeSubtag(from.isMayBeSubtag());
		to.setMayContainSubtags(from.isMayContainsSubtags());
		if (from.getEditOnInsert() != null) {
			to.setEditOnInsert(from.getEditOnInsert());
		}
		if (from.getEditorControlStyle() != null) {
			to.setEditorControlStyle(from.getEditorControlStyle());
		}
		if (from.getEditorControlsInside() != null) {
			to.setEditorControlInside(from.getEditorControlsInside());
		}
		to.setNewEditor(from.isNewEditor());
		to.setExternalEditorUrl(from.getExternalEditorUrl());

		// transform parts
		if (!shallow) {
			List<Part> parts = to.getParts();
			parts.clear();

			unwrap(() -> {
				if (from.getParts() != null) {
					from.getParts().forEach(p -> {
						wrap(() -> {
							Part editablePart = t.getObject(Part.class, p.getGlobalId(), true);
							if (editablePart == null) {
								editablePart = t.createObject(Part.class);
							}
							transform(p, editablePart);
							parts.add(editablePart);
						});
					});
				}
			});

			// sort parts
			parts.sort((p1, p2) -> p1.getPartOrder() - p2.getPartOrder());
		} else {
			if (from.getCategory() != null) {
				ConstructCategory category = t.getObject(ConstructCategory.class, from.getCategory().getGlobalId());
				if (category != null) {
					to.setConstructCategoryId(category.getId());
				}
			}
		}

		return to;
	}

	@Override
	protected void assign(Construct object, Node node, boolean isNew) throws NodeException {
		node.addConstruct(object);
	}

	/**
	 * Transform the given REST model into the part
	 * @param from REST model
	 * @param to part
	 * @return part
	 * @throws NodeException
	 */
	protected Part transform(PartModel from, Part to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		to.setEditable(from.isEditable() ? (from.isInlineEditable() ? 2 : 1) : 0);
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setHidden(!from.isVisible());
		to.setKeyname(ObjectTransformer.getString(from.getKeyword(), ""));
		to.setMlId(from.getMlId());
		I18NHelper.forI18nMap(from.getName(), (translation, id) -> to.setName(translation, id));
		to.setPartOrder(from.getOrder());
		to.setPartTypeId(from.getTypeId());
		to.setPolicy(from.getPolicy());
		to.setRequired(from.isRequired());
		to.setHideInEditor(from.isHideInEditor());
		to.setExternalEditorUrl(from.getExternalEditorUrl());

		switch (to.getPartTypeId()) {
		case Part.TEXT:
		case Part.TEXTHMTL:
		case Part.HTML:
		case Part.TEXTSHORT:
		case Part.TEXTHTMLLONG:
		case Part.HTMLLONG:
			to.setInfoInt(from.getRegexId());
			break;
		default:
			break;
		}

		switch (to.getPartTypeId()) {
		case Part.LIST:
		case Part.LISTORDERED:
		case Part.LISTUNORDERED:
			to.setInfoText(from.getHtmlClass());
			break;
		default:
			break;
		}

		return to;
	}

	/**
	 * Transform the given REST model into a category
	 * @param from REST model
	 * @param to category
	 * @return category
	 * @throws NodeException
	 */
	protected ConstructCategory transform(ConstructCategoryModel from, ConstructCategory to) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		I18NHelper.forI18nMap(from.getName(), (translation, id) -> to.setName(translation, id));
		to.setSortorder(from.getSortOrder());
		return to;
	}
}
