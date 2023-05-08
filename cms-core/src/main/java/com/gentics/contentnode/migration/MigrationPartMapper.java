package com.gentics.contentnode.migration;

import static com.gentics.contentnode.object.Part.BREADCRUMB;
import static com.gentics.contentnode.object.Part.CHECKBOX;
import static com.gentics.contentnode.object.Part.DATASOURCE;
import static com.gentics.contentnode.object.Part.DHTMLEDITOR;
import static com.gentics.contentnode.object.Part.FILELOCALPATH;
import static com.gentics.contentnode.object.Part.FILEUPLOAD;
import static com.gentics.contentnode.object.Part.FOLDERUPLOAD;
import static com.gentics.contentnode.object.Part.HTML;
import static com.gentics.contentnode.object.Part.HTMLCUSTOMFORM;
import static com.gentics.contentnode.object.Part.HTMLLONG;
import static com.gentics.contentnode.object.Part.JAVAEDITOR;
import static com.gentics.contentnode.object.Part.LIST;
import static com.gentics.contentnode.object.Part.LISTORDERED;
import static com.gentics.contentnode.object.Part.LISTUNORDERED;
import static com.gentics.contentnode.object.Part.NAVIGATION;
import static com.gentics.contentnode.object.Part.OVERVIEW;
import static com.gentics.contentnode.object.Part.SELECTIMAGEHEIGHT;
import static com.gentics.contentnode.object.Part.SELECTIMAGEWIDTH;
import static com.gentics.contentnode.object.Part.SELECTMULTIPLE;
import static com.gentics.contentnode.object.Part.SELECTSINGLE;
import static com.gentics.contentnode.object.Part.TABLEEXT;
import static com.gentics.contentnode.object.Part.TAGGLOBAL;
import static com.gentics.contentnode.object.Part.TAGPAGE;
import static com.gentics.contentnode.object.Part.TAGTEMPLATE;
import static com.gentics.contentnode.object.Part.TEXT;
import static com.gentics.contentnode.object.Part.TEXTCUSTOMFORM;
import static com.gentics.contentnode.object.Part.TEXTHMTL;
import static com.gentics.contentnode.object.Part.TEXTHTMLLONG;
import static com.gentics.contentnode.object.Part.TEXTSHORT;
import static com.gentics.contentnode.object.Part.URLFILE;
import static com.gentics.contentnode.object.Part.URLFOLDER;
import static com.gentics.contentnode.object.Part.URLIMAGE;
import static com.gentics.contentnode.object.Part.URLPAGE;
import static com.gentics.contentnode.object.Part.VELOCITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.lib.log.NodeLogger;

/**
 * This class is used to compile the allowed part mappings between two tagtypes
 * 
 * @author johannes2
 * 
 */
public class MigrationPartMapper {

	protected static NodeLogger logger = NodeLogger.getNodeLogger(MigrationPartMapper.class);

	private static final int[] STRING_VALUE_PART_TYPES = {
		DHTMLEDITOR, HTMLCUSTOMFORM, HTMLLONG, JAVAEDITOR, TEXT, TEXTCUSTOMFORM, TEXTSHORT, TEXTHMTL,
		TEXTHTMLLONG, HTML, LIST, LISTORDERED, LISTUNORDERED };

	public static Map<Part, List<Part>> getPossiblePartTypeMappings(Construct fromTagType, Construct toTagType) throws NodeException {

		Map<Part, List<Part>> possiblePartMappings = new HashMap<Part, List<Part>>();

		// Iterate over all parts of the source tagtype(fromConstruct)
		for (Part fromPart : fromTagType.getParts()) {

			List<Part> allowedPartsForPossibleMapping = new ArrayList<Part>();

			// determine which parts from the target(toConstruct) tagtype can mapped onto the current part
			for (Part toPart : toTagType.getParts()) {
				if (isPartMappable(toPart, fromPart)) {
					allowedPartsForPossibleMapping.add(toPart);
				}
			}
			possiblePartMappings.put(fromPart, allowedPartsForPossibleMapping);
		}

		return possiblePartMappings;

	}

	/**
	 * Check if a tag type migration mapping is valid
	 * 
	 * @param mappings
	 *            a list of mappings
	 * @param t
	 *            the current transaction
	 * @return
	 * @throws NodeException
	 */
	public static boolean isMappingValid(List<TagTypeMigrationMapping> mappings, Transaction t) throws NodeException {

		// Check each mapping
		for (TagTypeMigrationMapping mapping : mappings) {
			Construct fromConstruct = t.getObject(Construct.class, mapping.getFromTagTypeId());

			// Check all parts for the from construct one by one
			for (Part part : fromConstruct.getParts()) {

				// Skip parts that are not mappable. We'll just ignore those
				if (!hasPartMappableValue(part)) {
					continue;
				}

				boolean isPartContainedInMapping = false;
				List<Integer> toParts = new ArrayList<Integer>();

				for (MigrationPartMapping partMapping : mapping.getPartMappings()) {
					Part fromPart = t.getObject(Part.class, partMapping.getFromPartId());

					// Part mappings that contain a unmapped from part are valid. We can just omit any futher checks
					if (partMapping.isMarkedAsNotMapped()) {
						isPartContainedInMapping = true;
						continue;
					}
					
					Part toPart = t.getObject(Part.class, partMapping.getToPartId());
					Integer fromPartId = ObjectTransformer.getInteger(fromPart.getId(), null);

					// Check for valid part mappings
					if (!MigrationPartMapper.isPartMappable(toPart, fromPart)) {
						logger.error("Part {" + fromPartId + "} cannot be mapped to part {" + ObjectTransformer.getInteger(toPart.getId(), null) + "}");
						return false;
					}

					// The fromPart was found in the mapping
					if (part.getId() == fromPart.getId()) {
						isPartContainedInMapping = true;
					}

					// Check for duplicate toPart mappings, unless the part is being mapped to null
					if (toPart != null) {
						if (!toParts.contains(partMapping.getToPartId())) {
							toParts.add(partMapping.getToPartId());
						} else {
							logger.error("Invalid mapping: multiple parts were mapped to {" + partMapping.getToPartId() + "}.");
							return false;
						}
					}

				}

				// Check that all parts of source tag type are contained in the mapping
				if (!isPartContainedInMapping) {
					logger.error(
							"Incomplete mapping found. Part {" + part.getKeyname() + "} with ID {" + part.getId() + "} of tag type {" + fromConstruct.getName()
							+ "} was not present in mapping.");
					return false;
				}
			}

		}
		return true;
	}

	/**
	 * Checks whether the part has a mappable value. Some parts like the velocity parttype have no mappable value. Legacy part type may have a value but we treat them
	 * like they have no value.
	 * 
	 * @param part
	 * @return
	 */
	private static boolean hasPartMappableValue(Part part) {
		switch (part.getPartTypeId()) {
		case BREADCRUMB:
		case NAVIGATION:
		case TABLEEXT:
		case TAGGLOBAL:
		case FILELOCALPATH:
		case VELOCITY:
		case SELECTIMAGEHEIGHT:
		case SELECTIMAGEWIDTH:
			return false;

		case CHECKBOX:
		case DATASOURCE:
		case DHTMLEDITOR:
		case HTMLCUSTOMFORM:
		case HTMLLONG:
		case JAVAEDITOR:
		case TEXT:
		case TEXTSHORT:
		case TEXTHMTL:
		case TEXTHTMLLONG:
		case TEXTCUSTOMFORM:
		case HTML:
		case LIST:
		case LISTORDERED:
		case LISTUNORDERED:
		case FOLDERUPLOAD:
		case URLFOLDER:
		case OVERVIEW:
		case SELECTMULTIPLE:
		case SELECTSINGLE:
		case TAGPAGE:
		case TAGTEMPLATE:
		case URLFILE:
		case FILEUPLOAD:
		case URLIMAGE:
		case URLPAGE:
			return true;

		default:
			// UNKNOWN or custom part type
			// TODO Logging
			return false;
		}
	}

	/**
	 * Determines whether the toPart can be used for mapping from the fromPart.
	 * 
	 * @param toPart
	 * @param fromPart
	 * @return
	 * @throws NodeException
	 */
	private static boolean isPartMappable(Part toPart, Part fromPart) throws NodeException {

		// Parts mapping to null are always invalid
		if (toPart == null) {
			logger.debug("The toPart for fromPart {" + fromPart + "} is null. The partMapping is invalid.");
			return false;
		}

		final int toPartTypeID = toPart.getPartTypeId();

		switch (fromPart.getPartTypeId()) {
		case BREADCRUMB:
		case NAVIGATION:
		case TABLEEXT:
		case TAGGLOBAL:
		case FILELOCALPATH:
		case VELOCITY:
			// Legacy parts or parts with no value are not mapable
			return false;

		case CHECKBOX:
			// Only Checkbox to Checkbox mappings are allowed
			return toPart.getPartTypeId() == CHECKBOX;

		case DATASOURCE:
			// Only Datasource to Datasource mappings are allowed
			return toPart.getPartTypeId() == DATASOURCE;

		case SELECTIMAGEHEIGHT:
		case SELECTIMAGEWIDTH:
			// Not yet supported
			return false;

		case DHTMLEDITOR:
		case HTMLCUSTOMFORM:
		case HTMLLONG:
		case JAVAEDITOR:
		case TEXT:
		case TEXTSHORT:
		case TEXTHMTL:
		case TEXTHTMLLONG:
		case HTML:
		case LIST:
		case LISTORDERED:
		case LISTUNORDERED:
		case TEXTCUSTOMFORM:
			// Check whether the toPart is also a string value part type
			return isPartAStringValuePartType(toPart);

		case FOLDERUPLOAD:
		case URLFOLDER:
			// Check whether the toPart is also a part that accepts folder ids
			return toPartTypeID == URLFOLDER || toPartTypeID == FOLDERUPLOAD;

		case OVERVIEW:
			// Check whether the toPart is also an overview part type
			// TODO Honour Overview Restrictions stored in part
			return toPartTypeID == OVERVIEW;

		case SELECTMULTIPLE:
			// Check whether the toPart is also a select multiple part type
			if (toPartTypeID == SELECTMULTIPLE) {
				PartType toPartType = toPart.getPartType(toPart.getDefaultValue());
				PartType fromPartType = fromPart.getPartType(fromPart.getDefaultValue());

				if (toPartType.getClass().isAssignableFrom(MultiSelectPartType.class)) {
					Integer toPartDSId = ObjectTransformer.getInteger(((MultiSelectPartType) toPartType).getDatasourceId(), -1);
					Integer fromPartDSId = ObjectTransformer.getInteger(((MultiSelectPartType) fromPartType).getDatasourceId(), -1);

					if (toPartDSId.intValue() == fromPartDSId.intValue()) {
						return true;
					} else {
						return false;
					}
				}
			} else {
				return false;
			}

		case SELECTSINGLE:
			// Check whether the toPart is also a single select part type

			if (toPartTypeID == SELECTSINGLE) {
				PartType toPartType = toPart.getPartType(toPart.getDefaultValue());
				PartType fromPartType = fromPart.getPartType(fromPart.getDefaultValue());

				if (toPartType.getClass().isAssignableFrom(SingleSelectPartType.class)) {
					Integer toPartDSId = ObjectTransformer.getInteger(((SingleSelectPartType) toPartType).getDatasourceId(), -1);
					Integer fromPartDSId = ObjectTransformer.getInteger(((SingleSelectPartType) fromPartType).getDatasourceId(), -1);

					if (toPartDSId.intValue() == fromPartDSId.intValue()) {
						return true;
					} else {
						return false;
					}
				}
			} else {
				return false;
			}

		case TAGPAGE:
			// Check whether the toPart is also a tag page part type
			return toPartTypeID == TAGPAGE;

		case TAGTEMPLATE:
			// Check whether the toPart is also a tag template part type
			return toPartTypeID == TAGTEMPLATE;

		case URLFILE:
		case FILEUPLOAD:
			return toPartTypeID == URLFILE || toPartTypeID == FILEUPLOAD;

		case URLIMAGE:
			return toPartTypeID == URLIMAGE;

		case URLPAGE:
			// TODO consider If mapped to String Value Part and internal link set -> error
			// Check whether the toPart type is a string value part type.
			// In that case we could map the ext url to it.
			if (isPartAStringValuePartType(toPart)) {
				return true;
			} else {
				// Check whether the toPart type is a urlpage part type.
				return toPartTypeID == URLPAGE;
			}

		default:
			// UNKNOWN or custom part type
			// TODO Logging
			return false;
		}

	}

	public static boolean isPartAStringValuePartType(Part part) {
		for (int stringPartTypeID : STRING_VALUE_PART_TYPES) {
			if (stringPartTypeID == part.getPartTypeId()) {
				return true;
			}
		}
		return false;
	}
}
