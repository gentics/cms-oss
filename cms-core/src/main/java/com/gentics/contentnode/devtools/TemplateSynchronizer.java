package com.gentics.contentnode.devtools;

import static com.gentics.contentnode.devtools.Synchronizer.fixSorting;
import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.model.TemplateModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.devtools.TemplateInPackage;
import com.gentics.lib.util.FileUtil;
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
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;

/**
 * Synchronizer implementation for templates
 */
public class TemplateSynchronizer extends AbstractSynchronizer<Template, TemplateModel> {
	/**
	 * Transform the node object into its REST model
	 */
	public final static Function<PackageObject<Template>, TemplateInPackage> TRANSFORM2REST = object -> {
		TemplateInPackage restModel = new TemplateInPackage();
		Template.NODE2REST.apply(object.object, restModel);
		restModel.setPackageName(object.packageName);
		return restModel;
	};

	/**
	 * Name of the subdir containing the templatetags
	 */
	public final static String TEMPLATETAGS_DIR = "templatetags";

	/**
	 * Name of the subdir containing the objecttags
	 */
	public final static String OBJECTTAGS_DIR = "objecttags";

	/**
	 * Possible template source filenames
	 */
	protected Set<String> templateSourceFilenames;

	/**
	 * Create an instance
	 * @param packageSynchronizer package synchronizer
	 * @param basePath base path
	 */
	public TemplateSynchronizer(PackageSynchronizer packageSynchronizer, Path basePath) throws NodeException {
		super(packageSynchronizer, Template.class, TemplateModel.class, basePath);

		try (Trx trx = new Trx()) {
			templateSourceFilenames = new HashSet<>();
			DBUtils.executeStatement("SELECT ext FROM ml", Transaction.SELECT_STATEMENT, null, rs -> {
				while (rs.next()) {
					templateSourceFilenames.add("source." + rs.getString("ext"));
				}
			});
			trx.success();
		}
	}

	@Override
	public File getSyncTarget(Template object) throws NodeException {
		if (object.isMaster()) {
			return super.getSyncTarget(object);
		} else {
			File masterSyncTarget = getSyncTarget(object.getMaster());
			String name = getSyncTargetName(object);
			String channelName = object.getChannel().getFolder().getName();
			return new File(new File(new File(masterSyncTarget, MainPackageSynchronizer.CHANNELS_DIR), channelName), name);
		}
	}

	@Override
	public String getSyncTargetName(Template object) throws NodeException {
		return object.getName();
	}

	@Override
	public File getCurrentSyncLocation(Template object) throws NodeException {
		if (object.isMaster()) {
			return super.getCurrentSyncLocation(object);
		}

		// localized copies must have the master synchronized
		File masterSyncLocation = super.getCurrentSyncLocation(object.getMaster());
		if (masterSyncLocation == null) {
			// TODO i18n
			throw new NodeException();
		} else {
			File channelsDir = new File(masterSyncLocation, MainPackageSynchronizer.CHANNELS_DIR);
			if (!channelsDir.isDirectory()) {
				return null;
			} else {
				for (File channelDir : channelsDir.listFiles(File::isDirectory)) {
					for (File dir : channelDir.listFiles(File::isDirectory)) {
						File structureFile = new File(dir, STRUCTURE_FILE);
						if (structureFile.isFile()) {
							try {
								TemplateModel model = parseStructureFile(dir.toPath());
								if (object.getGlobalId().toString().equals(model.getGlobalId())) {
									return dir;
								}
							} catch (NodeException e) {
								Synchronizer.logger.warn(String.format("Error while parsing %s", structureFile), e);
							}
						}
					}
				}
				return null;
			}
		}
	}

	@Override
	protected void internalSyncToFilesystem(Template object, Path folder) throws NodeException {
		try {
			TemplateModel model = transform(object, new TemplateModel());

			// source file
			File sourceFile = new File(folder.toFile(), getTemplateSourceFileName(object));
			stringToFile(object.getSource(), sourceFile);

			// tags
			Set<File> dirs = new HashSet<>();
			File templateTagsFile = new File(folder.toFile(), TEMPLATETAGS_DIR);
			if (templateTagsFile.isDirectory()) {
				dirs.addAll(Arrays.asList(templateTagsFile.listFiles(File::isDirectory)));
			}
			for (TemplateTag tag : object.getTags().values()) {
				File tagDir = new File(templateTagsFile, tag.getName());
				tagDir.mkdirs();
				dirs.remove(tagDir);

				Set<File> files = new HashSet<>(Arrays.asList(tagDir.listFiles((dir, name) -> isPartFilename(name))));

				// values
				for (Value value : tag.getValues()) {
					File outFile = new File(tagDir, getProposedFilename(value.getPart()));
					storeContents(value, outFile);
					files.remove(outFile);
				}

				// remove superfluous files in the directory
				files.forEach(File::delete);
			}

			// object tags
			File objectTagsFile = new File(folder.toFile(), OBJECTTAGS_DIR);
			if (objectTagsFile.isDirectory()) {
				dirs.addAll(Arrays.asList(objectTagsFile.listFiles(File::isDirectory)));
			}
			for (ObjectTag tag : object.getObjectTags().values()) {
				File tagDir = new File(objectTagsFile, tag.getName());
				tagDir.mkdirs();
				dirs.remove(tagDir);

				Set<File> files = new HashSet<>(Arrays.asList(tagDir.listFiles((dir, name) -> isPartFilename(name))));

				// values
				for (Value value : tag.getValues()) {
					File outFile = new File(tagDir, getProposedFilename(value.getPart()));
					storeContents(value, outFile);
					files.remove(outFile);
				}

				// remove superfluous files in the directory
				files.forEach(File::delete);
			}

			// structure file
			try {
				// parse existing model (if structure file exists)
				TemplateModel existing = parseStructureFile(folder);
				// fix sorting of templatetags and objecttags according to existing model
				fixSorting(model, existing, TemplateModel::getTemplateTags, t -> Arrays.asList(t.getGlobalId(), t.getName()));
				fixSorting(model, existing, TemplateModel::getObjectTags, t -> Arrays.asList(t.getGlobalId(), t.getName()));
			} catch (Exception e) {
				// ignore exception (which is thrown, when structure file does not exist, are cannot be parsed)
			}
			jsonToFile(model, new File(folder.toFile(), STRUCTURE_FILE));

			// remove superfluous directories
			dirs.forEach(dir -> {
				try {
					FileUtils.deleteDirectory(dir);
				} catch (Exception e) {
					Synchronizer.logger.error("Error while deleting superfluous directory " + dir, e);
				}
			});

			Synchronizer.registerAll(folder);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected Template internalSyncFromFilesystem(Template object, Path folder, Template master) throws NodeException {
		TemplateModel model = parseStructureFile(folder);

		// TODO check consistency of rest model
		String folderName = Normalizer.normalize(folder.getFileName().toString(), Normalizer.Form.NFC);
		if (!ObjectTransformer.isEmpty(model.getName()) && !ObjectTransformer.equals(model.getName(), folderName)) {
			throw new NodeException(String.format("Cannot synchronize %s into cms: name must be %s, but was %s", folder, folderName, model.getName()));
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		t.getAttributes().put(NodeFactory.UNLOCK_AT_TRX_COMMIT, true);

		// when the model is bound to a channel, we check whether the channel exists.
		if (!ObjectTransformer.isEmpty(model.getChannelId())) {
			Node channel = t.getObject(Node.class, model.getChannelId());
			if (channel == null) {
				Synchronizer.logger.debug(String.format("Omit synchronizing of %s because channel %s does not exist", folder, model.getChannelId()));
				return null;
			}
		}

		Template editable = null;

		try {
			if (object == null) {
				editable = t.createObject(Template.class);
			} else {
				Template localMaster = null;
				editable = t.getObject(object, true);
				if (!editable.isMaster()) {
					localMaster = editable.getMaster();
					if (!localMaster.equals(master)) {
						throw new NodeException(String.format(
								"Cannot synchronize %s into cms: local master %s and master in package %s are not identical",
								folder, localMaster, master));
					}
				}
			}

			// normalize and synchronize
			model.setName(folderName);
			model.setDescription(ObjectTransformer.getString(model.getDescription(), ""));
			transform(model, editable, false, master);

			// read template source
			File sourceFile = new File(folder.toFile(), getTemplateSourceFileName(editable));
			if (!sourceFile.exists()) {
				throw new NodeException("Error while synchronizing " + object + ": " + sourceFile + " does not exist");
			}
			try (InputStream in = new FileInputStream(sourceFile); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				FileUtil.pooledBufferInToOut(in, out);
				editable.setSource(out.toString("UTF8"));
			} catch (IOException e) {
				throw new NodeException(e);
			}

			Map<GlobalId, MissingValueReference> missingReferences = new HashMap<>();

			// tag values
			for (TemplateTag tag : editable.getTemplateTags().values()) {
				for (Value value : tag.getValues()) {
					File inFile = new File(new File(new File(folder.toFile(), TEMPLATETAGS_DIR), tag.getName()), getProposedFilename(value.getPart()));
					if (inFile.exists()) {
						readContents(inFile, value, isJsonFile(value.getPart()), missingReferences);
					} else {
						Synchronizer.logger.info("Not synchronizing " + value + " of " + tag + ", because " + inFile + " does not exist");
					}
				}
			}

			// objecttag values
			for (ObjectTag tag : editable.getObjectTags().values()) {
				for (Value value : tag.getValues()) {
					File inFile = new File(new File(new File(folder.toFile(), OBJECTTAGS_DIR), tag.getName()), getProposedFilename(value.getPart()));
					if (inFile.exists()) {
						readContents(inFile, value, isJsonFile(value.getPart()), missingReferences);
					} else {
						Synchronizer.logger.info("Not synchronizing " + value + " of " + tag + ", because " + inFile + " does not exist");
					}
				}
			}

			editable.save();

			// Create missing reference entries for imported template and object tags.

			for (TemplateTag tag: editable.getTemplateTags().values()) {
				for (Value value : tag.getValues()) {
					MissingValueReference missing = missingReferences.get(value.getPart().getGlobalId());

					if (missing != null) {
						addMissingReference(value.getGlobalId().toString(), missing.getName(), missing.getTargetGlobalId());
					}
				}
			}

			for (ObjectTag tag : editable.getObjectTags().values()) {
				for (Value value : tag.getValues()) {
					MissingValueReference missing = missingReferences.get(value.getPart().getGlobalId());

					if (missing != null) {
						addMissingReference(value.getGlobalId().toString(), missing.getName(), missing.getTargetGlobalId());
					}
				}
			}

		} finally {
			if (editable != null) {
				editable.unlock();
			}
		}

		// if object is master, check for localized copies
		object = t.getObject(editable);
		if (object.isMaster()) {
			File channelsDir = new File(folder.toFile(), MainPackageSynchronizer.CHANNELS_DIR);
			if (channelsDir.isDirectory()) {
				for (File channelDir : channelsDir.listFiles(File::isDirectory)) {
					for (File dir : channelDir.listFiles(File::isDirectory)) {
						syncFromFilesystem(dir.toPath(), object);
					}
				}
			}
		}

		return object;
	}

	@Override
	public boolean isHandled(String filename) {
		if (ObjectTransformer.isEmpty(filename)) {
			return false;
		}
		return STRUCTURE_FILE.equals(filename) || isPartFilename(filename) || isTemplateSourceFilename(filename);
	}

	@Override
	protected TemplateModel transform(Template from, TemplateModel to) throws NodeException {
		Synchronizer.checkNotNull(from, to);

		return Template.NODE2DEVTOOL.apply(from, to);
	}

	@Override
	protected Template transform(TemplateModel from, Template to, boolean shallow) throws NodeException {
		return transform(from, to, shallow, null);
	}

	@Override
	protected void assign(Template object, Node node, boolean isNew) throws NodeException {
		node.addTemplate(object);
	}

	/**
	 * Transform the REST model into a template
	 * @param from REST model
	 * @param to template
	 * @param shallow true to create a shallow copy
	 * @param master optional master, if the object is a localized copy
	 * @return template
	 * @throws NodeException
	 */
	protected Template transform(TemplateModel from, Template to, boolean shallow, Template master) throws NodeException {
		Synchronizer.checkNotNull(from, to);
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!shallow && from.getChannelId() != null && AbstractContentObject.isEmptyId(to.getId())) {
			Node channel = t.getObject(Node.class, from.getChannelId());
			if (channel == null) {
				throw new NodeException(String.format("Could not find channel %s", from.getChannelId()));
			}
			if (!channel.isChannel()) {
				throw new NodeException(String.format("Channel %s is no channel", channel));
			}
			if (master != null) {
				to.setChannelInfo(channel.getId(), master.getChannelSetId());
			} else {
				to.setChannelInfo(channel.getId(), null);
			}
		}

		to.setDescription(from.getDescription());
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setName(from.getName());

		DBUtils.executeStatement("SELECT id FROM ml WHERE name = ?", Transaction.SELECT_STATEMENT, st -> {
			st.setString(1, from.getType());
		}, rs -> {
			while (rs.next()) {
				to.setMlId(rs.getInt("id"));
			}
		});

		if (!shallow) {
			// transform tags
			Map<String, TemplateTag> templateTags = to.getTemplateTags();
			templateTags.clear();
			Map<String, ObjectTag> objectTags = to.getObjectTags();
			objectTags.clear();

			unwrap(() -> {
				if (from.getTemplateTags() != null) {
					from.getTemplateTags().forEach(tag -> {
						wrap(() -> {
							TemplateTag editableTag = t.getObject(TemplateTag.class, tag.getGlobalId(), true);
							if (editableTag == null) {
								editableTag = t.createObject(TemplateTag.class);
							}
							transform(tag, editableTag);
							templateTags.put(editableTag.getName(), editableTag);
						});
					});
				}

				if (from.getObjectTags() != null) {
					from.getObjectTags().forEach(tag -> {
						wrap(() -> {
							ObjectTag editableTag = t.getObject(ObjectTag.class, tag.getGlobalId(), true);
							if (editableTag == null) {
								editableTag = t.createObject(ObjectTag.class);
							}
							transform(tag, editableTag);
							objectTags.put(editableTag.getName(), editableTag);
						});
					});
				}
			});
		}

		return to;
	}

	/**
	 * Get the name of the template source file
	 * @param template template
	 * @return name of the file
	 * @throws NodeException
	 */
	protected String getTemplateSourceFileName(Template template) throws NodeException {
		return "source." + template.getMarkupLanguage().getExtension();
	}

	/**
	 * Check whether the given filename is a possible template source filename
	 * @param filename filename to check
	 * @return true iff the filename is a possible template source filename
	 */
	protected boolean isTemplateSourceFilename(String filename) {
		return templateSourceFilenames.contains(filename);
	}
}
