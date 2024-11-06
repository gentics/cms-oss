package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.checkNotNull;
import static com.gentics.contentnode.devtools.Synchronizer.mapper;
import static com.gentics.contentnode.i18n.I18NHelper.forI18nMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionCategoryModel;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionModel;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.model.devtools.ObjectPropertyInPackage;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Synchronizer implementation of ObjectTagDefinitions
 */
public class ObjectTagDefinitionSynchronizer extends AbstractSynchronizer<ObjectTagDefinition, ObjectTagDefinitionModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<ObjectTagDefinition>, ObjectPropertyInPackage> TRANSFORM2REST = object -> {
		ObjectPropertyInPackage restModel = new ObjectPropertyInPackage();
		ObjectTagDefinition.NODE2REST.apply(object.object, restModel);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Lambda that embeds the rest model of the construct into the rest model of the object property definition
	 */
	public final static Consumer<ObjectPropertyInPackage> EMBED_CONSTRUCT = restModel -> {
		ObjectTagDefinition.EMBED_CONSTRUCT.accept(restModel);
	};


	/**
	 * Lambda that embeds the rest model of the category into the rest model of the object property definition
	 */
	public final static Consumer<ObjectPropertyInPackage> EMBED_CATEGORY = restModel -> {
		ObjectTagDefinition.EMBED_CATEGORY.accept(restModel);
	};


	/**
	 * Pattern for the folder name
	 */
	public final static Pattern FOLDER_NAME_PATTERN = Pattern.compile("^(?<type>folder|template|page|file|image)\\.object\\.(?<name>\\w+)$");

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 * @throws NodeException
	 */
	public ObjectTagDefinitionSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, ObjectTagDefinition.class, ObjectTagDefinitionModel.class, basePath);
	}

	@Override
	public String getSyncTargetName(ObjectTagDefinition object) throws NodeException {
		ObjectTag tag = object.getObjectTag();
		String typeString = ObjectTagDefinitionTypeModel.fromValue(tag.getObjType()).toString();
		return String.format("%s.%s", typeString, tag.getName());
	}

	@Override
	protected void internalSyncToFilesystem(ObjectTagDefinition object, Path folder) throws NodeException {
		// convert to (clean) rest model
		ObjectTagDefinitionModel model = transform(object, new ObjectTagDefinitionModel());
		jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));
	}

	@Override
	protected ObjectTagDefinition internalSyncFromFilesystem(ObjectTagDefinition object, Path folder, ObjectTagDefinition master) throws NodeException {
		// read the structure file
		File structureFile = new File(folder.toFile(), STRUCTURE_FILE);
		if (!structureFile.exists() || !structureFile.isFile()) {
			throw new NodeException("Cannot synchronize " + folder + " to " + (object != null ? object : "new object") + ": " + STRUCTURE_FILE
					+ " not found");
		}
		ObjectTagDefinitionModel model;
		try {
			model = mapper().readValue(structureFile, ObjectTagDefinitionModel.class);
		} catch (IOException e) {
			throw new NodeException("Error while parsing " + structureFile, e);
		}

		// TODO check consistency of rest model

		Transaction t = TransactionManager.getCurrentTransaction();

		ObjectTagDefinition editable = null;
		if (object == null) {
			editable = t.createObject(ObjectTagDefinition.class);
		} else {
			editable = t.getObject(object, true);
		}

		// normalize and synchronize
		transform(model, editable, false);

		// transform category (if set)
		ObjectTagDefinitionCategoryModel categoryModel = model.getCategory();
		if (categoryModel != null) {
			ObjectTagDefinitionCategory editableCategory = t.getObject(ObjectTagDefinitionCategory.class, categoryModel.getGlobalId(), true);
			if (editableCategory == null) {
				editableCategory = t.createObject(ObjectTagDefinitionCategory.class);
			}
			transform(categoryModel, editableCategory);
			editableCategory.save();
			editable.setCategoryId(editableCategory.getId());
		}

		editable.save();

		List<String> nodesIds = model.getNodeIds();
		List<Node> nodesToUpdate = new ArrayList<>();

		// Load nodes to which the object property should be linked and
		// check permissions to edit the node.
		if (nodesIds != null) {
			for (String nodeId : nodesIds) {
				Node node = MiscUtils.load(Node.class, nodeId, false, ObjectPermission.edit);

				if (node == null) {
					continue;
				}

				nodesToUpdate.add(t.getObject(node, true));
			}
		}

		// Commit the transaction so that the new object property gets an ID,
		// otherwise node.addObjectTagDefinition() will not work.
		t.commit(false);

		for (Node node : nodesToUpdate) {
			node.addObjectTagDefinition(editable);
			node.save();
		}

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
	protected ObjectTagDefinitionModel transform(ObjectTagDefinition from, ObjectTagDefinitionModel to) throws NodeException {
		checkNotNull(from, to);

		return ObjectTagDefinition.NODE2DEVTOOL.apply(from, to);
	}

	@Override
	protected ObjectTagDefinition transform(ObjectTagDefinitionModel from, ObjectTagDefinition to, boolean shallow) throws NodeException {
		checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setTargetType(from.getType().getTypeValue());
		to.getObjectTag().setObjType(from.getType().getTypeValue());
		to.getObjectTag().setName(from.getKeyword());
		if (from.getRequired() != null) {
			to.getObjectTag().setRequired(from.getRequired());
		}
		if (from.getInheritable() != null && to.getTargetType() == Folder.TYPE_FOLDER) {
			to.getObjectTag().setInheritable(from.getInheritable());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
			if (to.getTargetType() == Page.TYPE_PAGE && from.getSyncContentset() != null) {
				to.setSyncContentset(from.getSyncContentset());
			}
			if (to.getTargetType() == Page.TYPE_PAGE && from.getSyncVariants() != null) {
				to.setSyncVariants(from.getSyncVariants());
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) && from.getSyncChannelset() != null) {
				to.setSyncChannelset(from.getSyncChannelset());
			}
		}

		forI18nMap(from.getName(), (translation, id) -> to.setName(translation, id));

		forI18nMap(from.getDescription(), (translation, id) -> to.setDescription(translation, id));

		if (!shallow) {
			String constructId = from.getConstructId();
			Construct construct = t.getObject(Construct.class, constructId);
			if (construct == null) {
				throw new NodeException("Construct " + constructId + " does not exist");
			}
			to.getObjectTag().setConstructId(construct.getId());
		} else {
			if (from.getCategory() != null) {
				ObjectTagDefinitionCategory category = t.getObject(ObjectTagDefinitionCategory.class, from.getCategory().getGlobalId());
				if (category != null) {
					to.setCategoryId(category.getId());
				}
			}
		}

		return to;
	}

	@Override
	protected void assign(ObjectTagDefinition object, Node node, boolean isNew) throws NodeException {
		// if the object tag definition is new or already restricted to nodes, we add it to the nodes to which the package is assigned
		List<Node> nodes = object.getNodes();
		if ((isNew || !nodes.isEmpty()) && !nodes.contains(node)) {
			ObjectTagDefinition editable = TransactionManager.getCurrentTransaction().getObject(object, true);
			nodes = editable.getNodes();
			nodes.add(node);
			editable.save();
			editable.unlock();
		}
	}

	/**
	 * Transform the given object property category into its REST model
	 * @param from category
	 * @param to REST model
	 * @return REST model
	 * @throws NodeException
	 */
	protected ObjectTagDefinitionCategoryModel transform(ObjectTagDefinitionCategory from, ObjectTagDefinitionCategoryModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		return ObjectTagDefinitionCategory.NODE2DEVTOOL.apply(from, to);
	}

	/**
	 * Transform the given REST model into a category
	 * @param from REST model
	 * @param to category
	 * @return category
	 * @throws NodeException
	 */
	protected ObjectTagDefinitionCategory transform(ObjectTagDefinitionCategoryModel from, ObjectTagDefinitionCategory to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		forI18nMap(from.getName(), (translation, id) -> to.setName(translation, id));
		to.setSortorder(from.getSortOrder());
		return to;
	}
}
