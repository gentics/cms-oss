package com.gentics.contentnode.factory.object;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.OverviewSetting;
import com.gentics.lib.etc.StringUtils;

/**
 * Encapsulation of the overview part settings (stored in part.info_text)
 */
public class OverviewPartSetting {
	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<OverviewPartSetting, OverviewSetting, OverviewSetting> NODE2REST = (
			nodeModel, restModel) -> {
		restModel.setHideSortOptions(nodeModel.isHideSortOption());
		restModel.setStickyChannel(nodeModel.isStickyChannel());
		restModel.setListTypes(nodeModel.getRestrictedObjectTypes().stream().map(ObjectType::getRestModel)
				.sorted((t1, t2) -> org.apache.commons.lang3.StringUtils.compare(t1.name(), t2.name()))
				.collect(Collectors.toList()));
		restModel.setSelectTypes(nodeModel.getRestrictedSelectionTypes().stream().map(SelectionType::getRestModel)
				.sorted((t1, t2) -> org.apache.commons.lang3.StringUtils.compare(t1.name(), t2.name()))
				.collect(Collectors.toList()));
		return restModel;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<OverviewPartSetting, OverviewSetting> TRANSFORM2REST = nodeModel -> {
		return NODE2REST.apply(nodeModel, new OverviewSetting());
	};

	/**
	 * Function to transform the rest model into the node model
	 */
	public final static BiFunction<OverviewSetting, OverviewPartSetting, OverviewPartSetting> REST2NODE = (from, to) -> {
		to.setHideSortOption(from.isHideSortOptions());
		to.setStickyChannel(from.isStickyChannel());
		if (from.getListTypes() != null) {
			Set<ObjectType> toSet = from.getListTypes().stream().map(type -> ObjectType.get(type)).filter(type -> type != null).collect(Collectors.toSet());
			Set<ObjectType> set = to.getRestrictedObjectTypes();
			set.clear();
			set.addAll(toSet);
		}
		if (from.getSelectTypes() != null) {
			Set<SelectionType> toSet = from.getSelectTypes().stream().map(type -> SelectionType.get(type)).filter(type -> type != null).collect(Collectors.toSet());
			Set<SelectionType> set = to.getRestrictedSelectionTypes();
			set.clear();
			set.addAll(toSet);
		}
		return to;
	};

	/**
	 * Set of restricted object types
	 */
	protected Set<ObjectType> restrictedObjectTypes = new HashSet<>();

	/**
	 * Set of restricted selection types
	 */
	protected Set<SelectionType> restrictedSelectionTypes = new HashSet<>();

	/**
	 * Flag whether sort options shall be hidden
	 */
	protected boolean hideSortOption;

	/**
	 * Flag for sticky channel
	 */
	protected boolean stickyChannel;

	/**
	 * Get the (modifiable) restricted object types
	 * @return set of restricted object types
	 */
	public Set<ObjectType> getRestrictedObjectTypes() {
		return restrictedObjectTypes;
	}

	/**
	 * Get the (modifiable) restricted selection types
	 * @return set of restricted selection types
	 */
	public Set<SelectionType> getRestrictedSelectionTypes() {
		return restrictedSelectionTypes;
	}

	/**
	 * Check whether sort options shall be hidden
	 * @return flag for hiding sort options
	 */
	public boolean isHideSortOption() {
		return hideSortOption;
	}

	/**
	 * Set whether sort options shall be hidden
	 * @param hideSortOption flag for hiding sort options
	 */
	public void setHideSortOption(boolean hideSortOption) {
		this.hideSortOption = hideSortOption;
	}

	/**
	 * Check whether using sticky channel
	 * @return flag for sticky channel
	 */
	public boolean isStickyChannel() {
		return stickyChannel;
	}

	/**
	 * Set whether using sticky channel
	 * @param stickyChannel flag for sticky channel
	 */
	public void setStickyChannel(boolean stickyChannel) {
		this.stickyChannel = stickyChannel;
	}

	/**
	 * Create empty instance
	 */
	public OverviewPartSetting() {
	}

	/**
	 * Create a setting with info from the given part
	 * @param part part
	 */
	public OverviewPartSetting(Part part) throws NodeException {
		assertPart(part);

		// parse data from info_text
		String infoText = ObjectTransformer.getString(part.getInfoText(), "");
		String[] parts = StringUtils.splitString(infoText, ";");

		if (parts.length > 0) {
			restrictedObjectTypes.addAll(Arrays.asList(StringUtils.splitString(parts[0], ",")).stream().map(ttype -> ObjectType.get(ttype))
					.filter(Objects::nonNull).collect(Collectors.toSet()));
		}
		if (parts.length > 1) {
			restrictedSelectionTypes.addAll(Arrays.asList(StringUtils.splitString(parts[1], ',')).stream().map(code -> SelectionType.get(code))
					.filter(Objects::nonNull).collect(Collectors.toSet()));
		}
		if (parts.length > 2) {
			hideSortOption = ObjectTransformer.getBoolean(parts[2], hideSortOption);
		}
		if (parts.length > 3) {
			stickyChannel = ObjectTransformer.getBoolean(parts[3], stickyChannel);
		}
	}

	/**
	 * Set the overview part settings into the given part
	 * @param part
	 * @throws NodeException
	 */
	public void setTo(Part part) throws NodeException {
		assertPart(part);

		StringBuilder infoText = new StringBuilder();
		infoText.append(restrictedObjectTypes.stream().map(type -> Integer.toString(type.getTtype())).collect(Collectors.joining(",")));
		infoText.append(";");
		infoText.append(restrictedSelectionTypes.stream().map(type -> Integer.toString(type.getCode())).collect(Collectors.joining(",")));
		infoText.append(";");
		infoText.append(hideSortOption ? "1" : "");
		infoText.append(";");
		infoText.append(stickyChannel ? "1" : "");

		part.setInfoText(infoText.toString());
	}

	/**
	 * Assert that the part is not null and has the correct type (overview)
	 * @param part part
	 * @throws NodeException
	 */
	protected void assertPart(Part part) throws NodeException {
		if (part == null) {
			throw new NodeException("Cannot handle overview settings for null part");
		}
		if (part.getPartTypeId() != OverviewPartType.TYPE_ID) {
			throw new NodeException("Cannot handle overview settings for part of type " + part.getPartTypeId() + ". Type must be " + OverviewPartType.TYPE_ID);
		}
	}

	/**
	 * Enum holding all possible object types of overviews
	 */
	public static enum ObjectType {
		page(Page.TYPE_PAGE, ListType.PAGE), file(File.TYPE_FILE, ListType.FILE), image(ImageFile.TYPE_IMAGE, ListType.IMAGE), folder(Folder.TYPE_FOLDER, ListType.FOLDER);

		/**
		 * TType
		 */
		private int ttype;

		/**
		 * REST Model
		 */
		private ListType restModel;

		/**
		 * Create instance
		 * @param ttype ttype
		 * @param restModel REST Model
		 */
		private ObjectType(int ttype, ListType restModel) {
			this.ttype = ttype;
			this.restModel = restModel;
		}

		/**
		 * Get the ttype
		 * @return ttype
		 */
		public int getTtype() {
			return ttype;
		}

		/**
		 * Get the REST Model
		 * @return REST Model
		 */
		public ListType getRestModel() {
			return restModel;
		}

		/**
		 * Get the object type for the given ttype String
		 * @param ttypeString ttype String
		 * @return object type or null
		 */
		public static ObjectType get(String ttypeString) {
			int ttype = ObjectTransformer.getInt(ttypeString, 0);
			for (ObjectType val : ObjectType.values()) {
				if (val.ttype == ttype) {
					return val;
				}
			}

			return null;
		}

		/**
		 * Get the instance from the rest model
		 * @param restModel rest model
		 * @return instance or null, if not found
		 */
		public static ObjectType get(ListType restModel) {
			for (ObjectType val : ObjectType.values()) {
				if (val.restModel == restModel) {
					return val;
				}
			}

			return null;
		}
	}

	/**
	 * Enum holding all possible selection types
	 */
	public static enum SelectionType {
		folder(1, SelectType.FOLDER), single(2, SelectType.MANUAL), parent(3, SelectType.AUTO);

		/**
		 * Internal code
		 */
		private int code;

		/**
		 * REST Model
		 */
		private SelectType restModel;

		/**
		 * Create instance
		 * @param code internal code
		 * @param restModel REST Model
		 */
		private SelectionType(int code, SelectType restModel) {
			this.code = code;
			this.restModel = restModel;
		}

		/**
		 * Get the internal code
		 * @return internal code
		 */
		public int getCode() {
			return code;
		}

		/**
		 * Get the REST Model
		 * @return REST Model
		 */
		public SelectType getRestModel() {
			return restModel;
		}

		/**
		 * Get the selection type for the given code string or null
		 * @param codeString code string
		 * @return selection type or null
		 */
		public static SelectionType get(String codeString) {
			int code = ObjectTransformer.getInt(codeString, 0);
			for (SelectionType val : SelectionType.values()) {
				if (val.code == code) {
					return val;
				}
			}

			return null;
		}

		/**
		 * Get the instance from the rest model
		 * @param restModel rest model
		 * @return instance or null, if not found
		 */
		public static SelectionType get(SelectType restModel) {
			for (SelectionType val : SelectionType.values()) {
				if (val.restModel == restModel) {
					return val;
				}
			}

			return null;
		}
	}
}
