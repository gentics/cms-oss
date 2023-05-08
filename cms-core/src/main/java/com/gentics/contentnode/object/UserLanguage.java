package com.gentics.contentnode.object;

import java.util.Locale;

import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.rest.model.UILanguage;


/**
 * A {@link UserLanguage} is the language that a user selects for the
 * interface.
 * 
 * The language id the user has currently selected is stored in the user's
 * sesssion.
 * 
 * I18n related methods will make use of instances of this class. 
 */
@TType(UserLanguage.TYPE_USER_LANGUAGE)
public abstract class UserLanguage extends AbstractContentObject implements NamedNodeObject {
	private static final long serialVersionUID = 2491770135911687721L;

	/**
	 * This value is also set int public.inc.php as T_USER_LANGUAGE.
	 */
	public static final int TYPE_USER_LANGUAGE = 10210;

	/**
	 * Transform an instance to its REST model
	 */
	public final static Function<UserLanguage, UILanguage> TRANSFORM2REST = lang -> {
		return new UILanguage().setCode(lang.getCode()).setName(lang.getName());
	};

	/**
	 * Create an instance
	 * @param id id
	 * @param info object info
	 */
	protected UserLanguage(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * @return the name of the language.
	 */
	public abstract String getName();

	/**
	 * @return the {@link Locale} this user language refers to. If
	 *   this language is not active, null may be returned.
	 */
	public abstract Locale getLocale();
    
	/**
	 * @return whether this language is active or not.
	 *   inactive languages will not be availabe to the user for selection
	 *   (e.g. the "Meta" language)
	 */
	public abstract boolean isActive();

	/**
	 * Get code of the language
	 * @return code
	 */
	public abstract String getCode();

	/**
	 * Get the optional country code (may be null)
	 * @return country code or null
	 */
	public abstract String getCountry();
}
