package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.RoleModel;

/**
 * Interface for Roles
 */
@TType(Role.TYPE_ROLE)
public interface Role extends NodeObject, Resolvable, I18nNamedNodeObject {
	public static final int TYPE_ROLE = 109;

	/**
	 * Function that transforms the rest model into the given node model
	 */
	BiFunction<RoleModel, Role, Role> REST2NODE = (restModel, role) -> {
		I18NHelper.forI18nMap(restModel.getNameI18n(), role::setName);
		I18NHelper.forI18nMap(restModel.getDescriptionI18n(), role::setDescription);
		return role;
	};

	/**
	 * Function that transforms the node model the rest model
	 */
	Function<Role, RoleModel> TRANSFORM2REST = role -> new RoleModel().setId(role.getId())
			.setNameI18n(I18NHelper.toI18nMap(role.getName()))
			.setName(role.getName().toString())
			.setDescriptionI18n(I18NHelper.toI18nMap(role.getDescription()))
			.setDescription(role.getDescription().toString());

	/**
	 * get the name of the role.
	 * @return the name
	 */
	I18nString getName();

	/**
	 * Set the name in the given language
	 * @param name name
	 * @param language language
	 * @throws ReadOnlyException
	 */
	default void setName(String name, int language) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the name id of the role
	 * @return name id
	 */
	@FieldGetter("name_id")
	int getNameId();

	/**
	 * Get the role description
	 * @return role description
	 */
	I18nString getDescription();

	/**
	 * Set the description in the given language
	 * @param description description
	 * @param language language
	 * @throws ReadOnlyException
	 */
	default void setDescription(String description, int language) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the description id of the role
	 * @return description id
	 */
	@FieldGetter("description_id")
	int getDescriptionId();
}
