package com.gentics.contentnode.object.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;


/**
 * Abstract base class for all kinds of comparators
 */
public abstract class AbstractComparator {

	/**
	 * An empty {@link ContentNodeDate}.
	 */
	protected static final ContentNodeDate NO_DATE = new ContentNodeDate(0);

	/**
	 * Attributes for use with the {@link FileComparator} and the  {@link TemplateComparator}
	 */
	public static enum SortAttribute {
		NAME("name"), EDIT_DATE("edate", "pedate"), CREATE_DATE("cdate", "pcdate"), PUBLISH_DATE("pdate"), TYPE("type"), SIZE("size", "filesize"),
		TEMPLATE("tname", "template"), FILENAME("filename"), FOLDER("fname", "folder"), PRIORITY("priority"), MASTER_NODE("masternode"),
		EXCLUDED("excluded"), DELETED_DATE("deletedat"), DELETED_BY("deletedby"), PATH("path"), PUBLISH_DIR("publishdir"), CREATOR("creator"),
		EDITOR("editor"), PUBLISHER("publisher"), NODE("node"), CUSTOM_EDIT_DATE("customedate"), CUSTOM_CREATE_DATE("customcdate"),
		CUSTOM_OR_DEFAULT_CREATE_DATE("customordefaultcdate"), CUSTOM_OR_DEFAULT_EDIT_DATE("customordefaultedate");

		Set<String> attributes;

		SortAttribute(String...attributes) {
			this.attributes = new HashSet<>(Arrays.asList(attributes));
		}
	}

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(AbstractComparator.class);

	/**
	 * Sorted attribute
	 */
	protected SortAttribute attribute = SortAttribute.NAME;

	/**
	 * the sortway, which may be 1 or -1
	 * 1 is for ascending, while -1 is for descending
	 */
	protected int way = 1;

	/**
	 * Parse the given sort parameter into a list of pairs consisting of attribute and sortorder (true for "asc", false for "desc")
	 * @param sort comma separated list of sorted attributes. Each attribute may be prefixed with + (sort ascending) or - (sort descending)
	 * @return list of pairs of attribute and sortorder
	 */
	protected static List<Pair<String, Boolean>> parse(String sort) {
		List<Pair<String, Boolean>> parsedList = new ArrayList<>();

		if (!ObjectTransformer.isEmpty(sort)) {
			String[] sortedFields = sort.split(",");

			for (String field : sortedFields) {
				Pair<String, Boolean> parsed = parseField(field);
				if (parsed != null) {
					parsedList.add(parsed);
				}
			}
		}
		return parsedList;
	}

	/**
	 * Parse a single field (with optional sortorder prefix)
	 * @param field field
	 * @return pair of attribute an sortorder
	 */
	protected static Pair<String, Boolean> parseField(String field) {
		String attr = null;
		boolean ascending = true;
		field = field.trim();
		if (ObjectTransformer.isEmpty(field)) {
			return null;
		}
		if (field.startsWith("+")) {
			if (field.length() == 1) {
				return null;
			}
			attr = field.substring(1);
		} else if (field.startsWith("-")) {
			if (field.length() == 1) {
				return null;
			}
			attr = field.substring(1);
			ascending = false;
		} else {
			attr = field;
		}

		return Pair.of(attr, ascending);
	}

	/**
	 * Generates a new Comparator 
	 * @param attribute the attribute to sort by. May be one of
	 *      name (default), edate, type or size
	 * @param way sort way, may be "asc" or "desc" - defaults to asc
	 */
	public AbstractComparator(String attribute, String way) {
		String lowerCaseAttribute = ObjectTransformer.getString(attribute, "").toLowerCase();
		this.attribute = Stream.of(SortAttribute.values()).filter(sortAttribute -> sortAttribute.attributes.contains(lowerCaseAttribute)).findFirst()
				.orElse(SortAttribute.NAME);

		if ("desc".equals(way.toLowerCase())) {
			this.way = -1;
		}
	}

	/**
	 * Compare exclusion/disinheriting status of two objects.
	 * <ol>
	 * <li>Objects that are neither excluded nor disinherited</li>
	 * <li>Objects that are disinherited</li>
	 * <li>Objects that are excluded</li>
	 * </ol>
	 * @param o1 object1
	 * @param o2 object2
	 * @return -1, 0 or 1
	 */
	protected int compareMCExclusion(Disinheritable<?> o1, Disinheritable<?> o2) throws NodeException {
		if (o1.isExcluded()) {
			if (o2.isExcluded()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (o2.isExcluded()) {
				return -1;
			} else {
				return ((Disinheritable<?>) o1.getMaster()).getDisinheritedChannels().size()
						- ((Disinheritable<?>) o2.getMaster()).getDisinheritedChannels().size();
			}
		}
	}

	/**
	 * Compare objects by user reference
	 * @param o1 first object
	 * @param o2 second object
	 * @param extractor function that extracts the user object from the objects
	 * @return -1, 0 or 1
	 */
	protected <T> int compareUser(T o1, T o2, Function<T, SystemUser> extractor) {
		try {
			SystemUser user1 = extractor.apply(o1);
			SystemUser user2 = extractor.apply(o2);

			return StringUtils.mysqlLikeCompare(
					user1 == null ? "" : user1.getFullName(),
					user2 == null ? "" : user2.getFullName()) * way;
		} catch (NodeException e) {
			return 0;
		}
	}

	/**
	 * Compare the objects by node/channel (name)
	 * @param o1 first object
	 * @param o2 second object
	 * @return -1, 0 or 1
	 */
	protected int compareNode(Disinheritable<?> o1, Disinheritable<?> o2) {
		try (NoMcTrx trx = new NoMcTrx()) {
			Node n1 = o1.getChannel();
			if (n1 == null) {
				n1 = o1.getOwningNode();
			}
			Node n2 = o2.getChannel();
			if (n2 == null) {
				n2 = o2.getOwningNode();
			}

			return StringUtils.mysqlLikeCompare(
					(n1 == null || n1.getFolder() == null) ? "" : n1.getFolder().getName(), 
					(n2 == null || n2.getFolder() == null) ? "" : n2.getFolder().getName()) * way;
		} catch (NodeException e) {
			return 0;
		}
	}

	/**
	 * Compare objects by a {@link ContentNodeDate}
	 * @param o1 first object
	 * @param o2 second object
	 * @param extractor function that extracts the date object from the objects
	 * @return -1, 0 or 1
	 */
	protected <T> int compareDate(T o1, T o2, Function<T, ContentNodeDate> extractor) {
		try {
			ContentNodeDate date1 = Optional.ofNullable(extractor.apply(o1)).orElse(NO_DATE);
			ContentNodeDate date2 = Optional.ofNullable(extractor.apply(o2)).orElse(NO_DATE);

			return date1.compareTo(date2) * way;
		} catch (NodeException e) {
			return 0;
		}
	}
}
