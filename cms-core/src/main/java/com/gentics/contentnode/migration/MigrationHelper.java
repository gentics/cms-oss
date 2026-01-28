package com.gentics.contentnode.migration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper class for tag type migration
 * 
 * @author Taylor
 */
public class MigrationHelper {

	/**
	 * Relative path to migration logs
	 */
	private static final String LOG_DIR = "migration";

	/**
	 * Path to tag type logs
	 */
	public static final String MIGRATION_APPENDER_NAME = "MigrationAppender";

	/**
	 * Prefix added to tag type migration log filenames
	 */
	private static final String MIGRATION_LOG_PREFIX = "migration_";

	/**
	 * Pattern used to format dates used in the filenames of tag type migration logs
	 */
	private static final String MIGRATION_LOG_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MigrationHelper.class);

	/**
	 * Configure the log appender for logging progress of the tag type migration to a file
	 * 
	 * @param logger
	 *            the logger to configure
	 * @return logger with file appender enabled to log to the tag type migration log
	 * @throws NodeException
	 */
	public static NodeLogger configureLog(NodeLogger logger) throws NodeException {
		// Remove any previously configured tag type migration appenders
		NodeLogger.removeAppenderFromConfig(MIGRATION_APPENDER_NAME, logger);

		File logDir = getLogDir();

		// Use the current date and time to create the filename of the log
		SimpleDateFormat sdfDate = new SimpleDateFormat(MIGRATION_LOG_DATE_FORMAT);
		Date now = new Date();
		String strDate = sdfDate.format(now);

		// create file appender
		String logPath = logDir.getAbsolutePath() + File.separator + "migration_" + strDate + ".log";
		FileAppender appender = FileAppender.newBuilder()
			.setName(MIGRATION_APPENDER_NAME)
			.setLayout(PatternLayout.newBuilder().withPattern("%d %-5p %m%n").build())
			.withFileName(logPath)
			.withAppend(true)
			.build();

		logger.addAppender(appender, Level.DEBUG);
		logger.setLevel(Level.DEBUG);

		return logger;
	}

	/**
	 * Check if a tag type migration is currently being performed
	 * 
	 * @return true if tag type migration is being performed, false otherwise
	 */
	public static boolean isTagTypeMigrationExecuting() {
		return Operator.getCurrentlyRunningJobs().stream()
				.filter(restCallable -> restCallable.getWrapped() instanceof AbstractMigrationJob).findAny()
				.isPresent();
	}

	/**
	 * Retrieve a list of parts for a given tag type
	 * 
	 * @param id
	 *            the id of the tag type whose parts should be retrieved
	 * @param t
	 *            the current transaction
	 * @return list of parts for the given tag type
	 */
	public static List<Part> fetchPartsForTagtype(String id, Transaction t) {

		try {
			Construct object = (Construct) t.getObject(com.gentics.contentnode.object.Construct.class, ObjectTransformer.getInteger(id, null));

			return object.getParts();
		} catch (NullPointerException e) {
			logger.error("Error while getting parts, tag type {" + id + "} was not found", e);
		} catch (NodeException e) {
			logger.error("Error while getting parts for tag type {" + id + "}", e);
		}

		return null;
	}

	/**
	 * Retrieve a list of tags for a given list of pages
	 * 
	 * @param pageIds
	 *            list of page IDs to retrieve tags for
	 * @return list of tags for the given pages
	 * @throws NodeException
	 */
	public static List<Construct> fetchAllTagTypesForPages(List<Integer> pageIds) throws NodeException {
		Set<Construct> tagTypes = new HashSet<>();

		for (Integer pageId : pageIds) {

			// Get the page for the current id
			Page page = PageResourceImpl.getPage(String.valueOf(pageId), true, ObjectPermission.view);
			List<Tag> pageTags = new ArrayList<Tag>(page.getTags().values());

			// Add constructs of all page tags to the set
			for (Tag tag : pageTags) {
				tagTypes.add(tag.getConstruct());
			}
		}

		// return as list
		return new ArrayList<Construct>(tagTypes);
	}

	/**
	 * Retrieve a list of tags for a given list of templates
	 * 
	 * @param templateIds
	 *            list of page IDs to retrieve tags for
	 * @return list of tags for the given templates
	 * @throws NodeException
	 */
	public static List<Construct> fetchAllTagTypesForTemplates(List<Integer> templateIds, Transaction t) throws NodeException {
		Set<Construct> tagTypes = new HashSet<>();

		for (Integer templateId : templateIds) {

			// Get the template for the current id
			Template template = t.getObject(Template.class, templateId);

			if (template == null) {
				I18nString message = new CNI18nString("template.notfound");

				message.setParameter("0", templateId);
				throw new EntityNotFoundException(message.toString());
			}

			// Add all new template tags to the set of tag types
			List<Tag> templateTags = new Vector<Tag>(template.getTags().values());

			for (Tag tag : templateTags) {
				tagTypes.add(tag.getConstruct());
			}
		}

		// return as list
		return new ArrayList<Construct>(tagTypes);
	}

	/**
	 * Retrieve a list of tags for a given list of object properties
	 * 
	 * @param objectIds
	 *            list of page IDs to retrieve tags for
	 * @return list of tags for the given templates
	 * @throws NodeException
	 */
	public static List<Construct> fetchAllTagTypesForOEDef(List<Integer> objectIds, Transaction t) throws NodeException {
		Set<Construct> tagTypes = new HashSet<Construct>();

		for (Integer objectId : objectIds) {
			// Get the object tag defintion for the current id
			ObjectTagDefinition object = t.getObject(ObjectTagDefinition.class, objectId);

			if (object == null) {
				I18nString message = new CNI18nString("objectproperty.notfound");

				message.setParameter("0", objectId);
				throw new EntityNotFoundException(message.toString());
			}

			// Add all new object tags to the set of tag types
			List<Tag> objectTags = new Vector<Tag>(object.getObjectTags());

			for (Tag tag : objectTags) {
				tagTypes.add(tag.getConstruct());
			}
		}

		return new ArrayList<Construct>(tagTypes);
	}

	/**
	 * Get all tagtypes
	 * @param t transaction
	 * @return list of constructs
	 * @throws NodeException
	 */
	public static List<Construct> fetchAllTagTypes(Transaction t) throws NodeException {
		Set<Construct> tagTypes = new HashSet<>();

		IntegerColumnRetriever ids = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM construct", ids);

		// This will be an expensive operation on installations with many
		// constructs and the cache is not (yet) populated.
		tagTypes.addAll(t.getObjects(Construct.class, ids.getValues()));

		// return as list
		return new ArrayList<Construct>(tagTypes);
	}

	/**
	 * Replace references to a part value
	 * 
	 * @param logger
	 *            the tag type migration logger
	 * @param content
	 *            the value_text of the value where references should be replaced
	 * @param sourceKeyname
	 *            the keyword to replace
	 * @param targetKeyname
	 *            the keyword to use for replacement
	 * @return the value_text of the value with all occurrences of sourceKeyname replaced with targetKeyname
	 */
	public static String replaceReferences(NodeLogger logger, Part part, Integer valueId, String content, String sourceKeyname, String targetKeyname) {

		final String VELOCITY_REGEX = "(\\$cms.*parts.*)" + sourceKeyname + "( )";
		final String NODE_REPLACE_REGEX = "(<node.*)" + sourceKeyname + "(.*>)";

		List<String> regularExpressions = new ArrayList<String>();

		regularExpressions.add(VELOCITY_REGEX);
		regularExpressions.add(NODE_REPLACE_REGEX);

		// Apply all replacements
		for (String regularExpression : regularExpressions) {

			if (content != null && targetKeyname != null) {
				content = content.replaceAll(regularExpression, "$1" + targetKeyname + "$2");
				logger.debug("Reference to {" + sourceKeyname + "} in value {" + valueId + "} was updated to {" + targetKeyname + "}.");
			} else {
				logger.debug(
						"Reference to {" + sourceKeyname + "} was found in value {" + valueId + "} but was not updated because the part is marked for deletion.");
			}

		}

		return content;
	}

	/**
	 * Check for references to parts of a tag and update them if the part is not being deleted (newKeyname is not null)
	 * 
	 * @param transaction
	 *            the transaction to use
	 * @param logger
	 *            the tag type migration logger
	 * @param tag
	 *            the tag whose part references need to be updated
	 * @param oldKeyname
	 *            the old keyname of the part being updated
	 * @param newKeyname
	 *            the new keyname of the part being updated
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public static void checkReferences(Transaction transaction, NodeLogger logger, Tag tag, String oldKeyname, String newKeyname) throws ReadOnlyException, NodeException {

		// If the keyword names of the source and destination parts are the same, references do not need to be updated
		if (oldKeyname.equals(newKeyname)) {
			return;
		}

		// Check all other values for references to the keyname
		for (Value value : tag.getValues()) {

			Part part = transaction.getObject(Part.class, value.getPartId());
			Integer valueId = ObjectTransformer.getInteger(value.getId(), null);
			String valueText = value.getValueText();

			// Perform text replacement on the value's value_text field
			valueText = replaceReferences(logger, part, valueId, valueText, oldKeyname, newKeyname);

			// Set the new value_text and save the value
			value.setValueText(valueText);
			value.save();
		}
	}

	/**
	 * Migrate a tag type according to a given configuration
	 * 
	 * @param transaction
	 *            the transaction to use
	 * @param logger
	 *            the configured tag type migration logger
	 * @param tag
	 *            the tag type to migrate
	 * @param mapping
	 *            the configuration object containing the mapping information
	 * 
	 * @return true if the migration was successful, false otherwise
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	public static void migrateTag(Transaction transaction, NodeLogger logger, Tag tag, TagTypeMigrationMapping mapping) throws ReadOnlyException,
				NodeException {

		// Iterate through the mappings and apply the configuration to the corresponding values in the tag type
		for (MigrationPartMapping partMapping : mapping.getPartMappings()) {
			Part fromPart = transaction.getObject(Part.class, partMapping.getFromPartId());
			Part toPart = transaction.getObject(Part.class, partMapping.getToPartId());
			Integer fromPartId = ObjectTransformer.getInteger(fromPart.getId(), null);

			// Update all part references of each value according to the mapping
			for (Value value : tag.getTagValues()) {
				Integer valuePartId = ObjectTransformer.getInteger(value.getPartId(), null);
				boolean editable = value.getPart().isEditable();

				if (valuePartId.intValue() == fromPartId.intValue()) {

					if (editable) {
						// Check if part is mapped to null
						if (partMapping.getToPartId() == null && partMapping.isMarkedAsNotMapped()) {
							logger.info("Part {" + partMapping.getFromPartId() + "} of value {" + value.getId() + "} is unmapped. This value will be deleted.");
							checkReferences(transaction, logger, tag, value.getPart().getKeyname(), null);
						} else if (partMapping.getToPartId() == null) {
							throw new NodeException(
									"Invalid mapping: part {" + partMapping.getFromPartId() + "} of value {" + value.getId()
									+ "} is mapped to null, but it was not marked as unmapped.");
						} else {
							// Create new value to replace the old one
							Value newValue = (Value) transaction.createObject(Value.class);
							
							newValue.setContainer(tag);
							newValue.setPartId(toPart.getId());
							newValue.setInfo(value.getInfo());
							newValue.setValueRef(value.getValueRef());
							newValue.setValueText(value.getValueText());
							newValue.save();
							
							// Update all references to value
							checkReferences(transaction, logger, tag, value.getPart().getKeyname(), newValue.getPart().getKeyname());
						}
						
						// Delete old value
						value.delete();
					} else {
						// the part is not editable, so just update the references
						if (toPart == null) {
							checkReferences(transaction, logger, tag, fromPart.getKeyname(), null);
						} else {
							checkReferences(transaction, logger, tag, fromPart.getKeyname(), toPart.getKeyname());
						}
					}
				}
			}
		}

		// Update the construct id of the tag type
		tag.setConstructId(mapping.getToTagTypeId());

		// Handle tag type migrations in which the target tag types contain more parts than the source tag type
		Construct toConstruct = transaction.getObject(Construct.class, mapping.getToTagTypeId(), true);
		Construct fromConstruct = transaction.getObject(Construct.class, mapping.getFromTagTypeId(), true);
		int toPartsCount = toConstruct.getParts().size();
		int fromPartsCount = fromConstruct.getParts().size();

		// If the number of parts in the source tag is less than the number of parts in the destination tag, default values must be created
		if (fromPartsCount < toPartsCount) {

			// Create a list of all static parts for the destination tag type, since these do not need to be created and will be ignored below
			List<Integer> staticPartIds = new ArrayList<Integer>();
			ValueList values = toConstruct.getTagValues();

			for (Value value : values) {
				if (value.isStatic()) {
					staticPartIds.add(ObjectTransformer.getInt(value.getPartId(), 0));
				}
			}

			// Check which values are missings
			for (Part part : toConstruct.getParts()) {
				if (!isPartMappedTo(part, mapping)) {
					// Create value using default values for its part
					Value defaultValue = part.getDefaultValue();
					Value value = (Value) transaction.createObject(Value.class);

					value.setPartId(part.getId());
					value.setContainer(tag);
					value.setInfo(defaultValue.getInfo());
					value.setValueRef(defaultValue.getValueRef());
					value.setValueText(defaultValue.getValueText());

					// Do not create values for static parts
					if (!staticPartIds.contains(ObjectTransformer.getInt(value.getPartId(), 0))) {
						value.save();
						logger.info("Created missing part {" + part.getKeyname() + "} using default values.");
					}
				}
			}
		}
	}

	/**
	 * Check if a given TagTypeMigrationMapping contains a mapping to a given part
	 * 
	 * @param part
	 *            the part to check for a mapping for
	 * @param mapping
	 *            the mapping to examine
	 * @return true if a mapping is found to the part, false otherwise
	 * @throws NodeException
	 */
	private static boolean isPartMappedTo(Part part, TagTypeMigrationMapping mapping) throws NodeException {
		Integer partId = ObjectTransformer.getInteger(part.getId(), null);

		for (MigrationPartMapping partMapping : mapping.getPartMappings()) {

			// Skip the current part mapping if it maps to null
			if (partMapping.getToPartId() == null) {
				continue;
			}

			if (partMapping.getToPartId().intValue() == partId.intValue()) {
				return true;
			}
		}
		return false;
	}

	public static String getTtmLogDateFormat() {
		return MIGRATION_LOG_DATE_FORMAT;
	}

	public static String getTtmLogPrefix() {
		return MIGRATION_LOG_PREFIX;
	}

	public static File getLogDir() throws NodeException {

		File path = new File(ConfigurationValue.LOGS_PATH.get(), LOG_DIR);

		// create log directory if it doesn't exist already
		if (!path.exists() && !path.mkdirs()) {
			throw new NodeException("Could not create log dir {" + path + "}");
		}
		return path;
	}

	/**
	 * Get the path to the log file of the fileappender which is assigned to the given logger (if any)
	 * @param logger logger
	 * @return file path or null, if no fileappender found
	 */
	public static String getLogPath(NodeLogger logger) {
		LoggerContext context = LoggerContext.getContext(false);
		Configuration config = context.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
		Appender appender = loggerConfig.getAppenders().getOrDefault(MigrationHelper.MIGRATION_APPENDER_NAME, null);

		if (appender instanceof FileAppender) {
			FileAppender fileAppender = (FileAppender) appender;
			File logFile = new File(fileAppender.getFileName());

			return logFile.getName();
		} else {
			return null;
		}
	}
}
