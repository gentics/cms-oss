package com.gentics.contentnode.perm;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Class bundling a {@link PermType} with i18n keys for label, description and category, and with required features.
 */
public class NamedPerm {
	/**
	 * Permission type
	 */
	private PermType type;

	/**
	 * I18n key for the label
	 */
	private String labelKey;

	/**
	 * I18n key for the description
	 */
	private String descriptionKey;

	/**
	 * I18n key for the category
	 */
	private String categoryKey;

	/**
	 * Required features
	 */
	private Feature[] features = new Feature[0];

	/**
	 * Generic read permission
	 */
	public static NamedPerm read = NamedPerm.of(PermType.read, "459.show");

	/**
	 * Generic setperm permission
	 */
	public static NamedPerm setperm = NamedPerm.of(PermType.setperm, "assign_user_permissions");

	/**
	 * Generic create permission
	 */
	public static NamedPerm create = NamedPerm.of(PermType.create, "338.create");

	/**
	 * Generic delete permission
	 */
	public static NamedPerm delete = NamedPerm.of(PermType.delete, "340.delete");

	/**
	 * Generic update permission
	 */
	public static NamedPerm update = NamedPerm.of(PermType.update, "339.edit");

	/**
	 * Create an instance as copy of another instance
	 * @param orig instance to copy
	 * @return copy
	 */
	public static NamedPerm copy(NamedPerm orig) {
		NamedPerm perm = new NamedPerm();
		perm.type = orig.type;
		perm.labelKey = orig.labelKey;
		perm.descriptionKey = orig.descriptionKey;
		perm.categoryKey = orig.categoryKey;
		return perm;
	}

	/**
	 * Create instance
	 * @param type permission
	 * @param labelKey i18n key of the label
	 * @return instance
	 */
	public static NamedPerm of(PermType type, String labelKey) {
		NamedPerm perm = new NamedPerm();
		perm.type = type;
		perm.labelKey = labelKey;
		return perm;
	}

	/**
	 * Create instance
	 * @param type permission
	 * @param labelKey i18n key of the label
	 * @param descriptionKey i18n key of the description
	 * @return instance
	 */
	public static NamedPerm of(PermType type, String labelKey, String descriptionKey) {
		NamedPerm perm = new NamedPerm();
		perm.type = type;
		perm.labelKey = labelKey;
		perm.descriptionKey = descriptionKey;
		return perm;
	}

	/**
	 * Create instance
	 * @param type permission
	 * @param labelKey i18n key of the label
	 * @param descriptionKey i18n key of the description
	 * @param categoryKey i18n key of the category
	 * @param features required features
	 * @return instance
	 */
	public static NamedPerm of(PermType type, String labelKey, String descriptionKey, String categoryKey, Feature...features) {
		NamedPerm perm = new NamedPerm();
		perm.type = type;
		perm.labelKey = labelKey;
		perm.descriptionKey = descriptionKey;
		perm.categoryKey = categoryKey;
		perm.features = features;
		return perm;
	}

	/**
	 * Get the permission type
	 * @return permission type
	 */
	public PermType getType() {
		return type;
	}

	/**
	 * Translated label
	 * @return label
	 */
	public String getLabelI18n() {
		return I18NHelper.get(labelKey);
	}

	/**
	 * Translated description (if key was set), may be null
	 * @return description or null
	 */
	public String getDescriptionI18n() {
		return descriptionKey != null ? I18NHelper.get(descriptionKey) : null; 
	}

	/**
	 * Translated category (if key was set), may be null
	 * @return category or null
	 */
	public String getCategoryI18n() {
		return categoryKey != null ? I18NHelper.get(categoryKey) : null;
	}

	/**
	 * Check whether the permission setting is active.
	 * A permission setting is not active, if it requires one or more features and not all of the features are activated
	 * @return active flag
	 */
	public boolean isActive() {
		for (Feature feature : features) {
			if (!NodeConfigRuntimeConfiguration.isFeature(feature)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Set the i18n key of the category
	 * @param categoryKey i18n key
	 * @return fluent API
	 */
	public NamedPerm category(String categoryKey) {
		this.categoryKey = categoryKey;
		return this;
	}

	/**
	 * Set the i18n key of the description
	 * @param descriptionKey i18n key
	 * @return fluent API
	 */
	public NamedPerm description(String descriptionKey) {
		this.descriptionKey = descriptionKey;
		return this;
	}

	/**
	 * Set required features
	 * @param features required features
	 * @return fluent API
	 */
	public NamedPerm feature(Feature...features) {
		this.features = features;
		return this;
	}
}
