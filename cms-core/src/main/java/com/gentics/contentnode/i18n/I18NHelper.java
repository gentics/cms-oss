package com.gentics.contentnode.i18n;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.I18nNamedNodeObject;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.lib.i18n.CNI18nString;

public final class I18NHelper {

	/**
	 * Returns the name of the given object
	 * @param object object
	 * @return name
	 * @throws NodeException
	 */
	public static String getName(NodeObject object) throws NodeException {
		return getName(object, true).orElseThrow(() -> new NodeException("Could not determine name for " + object));
	}
	/**
	 * Returns the name of the given object, optionally throwing a {@link NodeException}.
	 * @param object object
	 * @param throwIfFailed if not set, the possible exception is logged but not rethrown.
	 * @return name
	 * @throws NodeException
	 */
	public static Optional<String> getName(NodeObject object, boolean throwIfFailed) throws NodeException {
		if (object == null) {
			if (throwIfFailed) {
				throw new NodeException("Could not determine name for object. Given object was null.");
			} else {
				return Optional.empty();
			}
		}
		String result = null;
		if (object instanceof NamedNodeObject) {
			result = ((NamedNodeObject) object).getName();
		} else if (object instanceof I18nNamedNodeObject) {
			result = ((I18nNamedNodeObject) object).getName().toString();
		} else if (object instanceof ObjectTagDefinition) {
			result = ((ObjectTagDefinition) object).getName();
		} else if (object instanceof Construct) {
			result = ((Construct) object).getName().toString();
		} else if (object instanceof Datasource) {
			result = ((Datasource) object).getName();
		} else {
			result = object.toString();
		}
		return Optional.of(result);
	}

	/**
	 * Returns the path to the given object. If the object is deleted (in the wastebin), the phrase <code>(in the wastebin)</code> will be appended
	 * @param object object for which the path should be returned
	 * @return path to the object
	 * @throws NodeException
	 */
	public static String getPath(NodeObject object) throws NodeException {
		return getPath(object, true);
	}

	/**
	 * Returns the path to the given object.
	 * @param object object for which the path should be returned
	 * @param addWastebinNote true to add the phrase <code>(in the wastebin)</code> for objects in the wastebin
	 * @return path to the object
	 * @throws NodeException
	 */
	public static String getPath(NodeObject object, boolean addWastebinNote) throws NodeException {
		if (object == null) {
			throw new NodeException("Can't determine path for object. Given object was null.");
		}
		StringBuilder path = new StringBuilder();

		Node channel = null;
		if (object instanceof LocalizableNodeObject<?>) {
			channel = ((LocalizableNodeObject<?>) object).getChannel();
		}

		try (ChannelTrx trx = new ChannelTrx(channel)) {
			NodeObject current = object;
			
			while (current != null) {
				String name = null;
				if (current instanceof LocalizableNodeObject<?>) {
					name = ((LocalizableNodeObject<?>) current).getName();
				} else {
					name = current.toString();
				}
				
				if (!ObjectTransformer.isEmpty(name)) {
					path.insert(0, name);
					path.insert(0, "/");
				}
				
				current = current.getParentObject();
			}
		}

		if (addWastebinNote && object.isDeleted()) {
			I18nString inTheWastebin = new CNI18nString("in.wastebin");
			path.append(" (").append(inTheWastebin).append(")");
		}

		return path.toString();
	}

	/**
	 * Returns a string contains user friendly information about the location
	 * and name of the object.
	 * 
	 * @param object
	 *            Object for which the location should be returned.
	 * @throws NodeException
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 */
	public static String getLocation(LocalizableNodeObject<?> object) throws NodeException {
		if (object == null) {
			throw new NodeException("Can't determine location for object. Given object was null.");
		}
		String channelInfo = "";
		if (object.getChannel() != null) {
			channelInfo = " " + object.getChannel().getFolder().getName() + " " + object.getChannel().getId();
		}
		return object.getName() + channelInfo;
	}

	/**
	 * Return the location of the given rest page
	 * 
	 * @param page
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 * @throws NodeException
	 */
	public static String getLocation(Page page) throws NodeException {
		if (page == null) {
			throw new NodeException("Can't determine location for page. Given object was null.");
		}
		return page.getName() + " " + new CNI18nString("in_folder") + " " + page.getFolderId();

	}

	/**
	 * Returns the location of the given rest folder. The location contains the
	 * name and the id of the folder.
	 * 
	 * @param folder
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 * @throws NodeException
	 */
	public static String getLocation(Folder folder) throws NodeException {
		if (folder == null) {
			throw new NodeException("Can't determine location for folder. Given object was null.");
		}
		return folder.getName() + " " + folder.getId();

	}

	/**
	 * Returns the location of the given rest template. The location contains
	 * the template name and the folder id of the template's parent folder.
	 * 
	 * @param template
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 * @throws NodeException
	 */
	public static String getLocation(Template template) throws NodeException {
		if (template == null) {
			throw new NodeException("Can't determine location for template. Given object was null.");
		}
		return template.getName() + " " + new CNI18nString("in_folder") + " " + template.getFolderId();
	}

	/**
	 * Returns the location of the given rest file. The location includes the
	 * filename and the folder id of the parent folder of the file.
	 * 
	 * @param file
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 * @throws NodeException
	 */
	public static String getLocation(File file) throws NodeException {
		if (file == null) {
			throw new NodeException("Can't determine location for file. Given object was null.");
		}
		return file.getName() + " " + new CNI18nString("with_filename") + " " + file.getFolderId();

	}

	/**
	 * Returns the location of the given target folder.
	 * 
	 * @param targetFolder
	 * @return The user friendly i18n string which contains information about
	 *         the location of the object.
	 * @throws NodeException
	 */
	public static String getLocation(TargetFolder targetFolder) throws NodeException {
		if (targetFolder == null) {
			throw new NodeException("Can't determine location for target folder. Given object was null.");
		}
		return "Id: " + targetFolder.getId() + ", ChannelId: " + targetFolder.getChannelId();
	}

	/**
	 * Get a comma separated list of the paths of the given objects. A maximum of maxObjects will be listed by path, if more objects are contained in the list,
	 * the i18n String (and x further) will be appended.
	 * @param objects collection of objects
	 * @param maxObjects maximum number of objects shown in the list
	 * @return comma separated list of object paths
	 * @throws NodeException
	 */
	public static String getPaths(Collection<? extends NodeObject> objects, int maxObjects) throws NodeException {
		StringBuilder out = new StringBuilder();
		int count = 0;
		for (NodeObject o : objects) {
			if (count < maxObjects) {
				if (count > 0) {
					out.append(", ");
				}
				out.append(getPath(o));
			} else {
				I18nString further = new CNI18nString("channelsync.further");
				further.setParameter("0", objects.size() - maxObjects);
				out.append(" (").append(further.toString()).append(")");
				break;
			}
			count++;
		}
		return out.toString();
	}

	/**
	 * Get the translation for the given key with optional parameters
	 * @param key key
	 * @param params optional parameters
	 * @return transation
	 */
	public static String get(String key, String...params) {
		CNI18nString i18n = new CNI18nString(key);
		i18n.addParameters(params);
		return i18n.toString();
	}

	/**
	 * Get the translation from the given translation map
	 * @param i18nMap map
	 * @return translation (null if not found)
	 * @throws NodeException
	 */
	public static String get(Map<String, String> i18nMap) throws NodeException {
		UserLanguage language = UserLanguageFactory.getById(ContentNodeHelper.getLanguageId(), true);
		return i18nMap.get(language.getCode());
	}

	/**
	 * Transform the given dictionary id into all translations (as map)
	 * @param dicId dictionary id
	 * @return map containing the translations (keys are language codes)
	 * @throws NodeException
	 */
	public static Map<String, String> toI18nMap(int dicId) throws NodeException {
		return toI18nMap(new CNI18nString(Integer.toString(dicId)));
	}

	/**
	 * Transform the given string into all translations (as map)
	 * @param string i18n string
	 * @return map containing the translations (keys are language codes)
	 * @throws NodeException
	 */
	public static Map<String, String> toI18nMap(I18nString string) throws NodeException {
		if (string == null) {
			return null;
		}

		Map<String, String> translations = new HashMap<>();

		for (UserLanguage language : UserLanguageFactory.getActive()) {
			try (LangTrx langTrx = new LangTrx(language)) {
				translations.put(langTrx.getCode(), string.toString());
			}
		}

		return translations;
	}

	/**
	 * Transform the given translations map into an array of translations
	 * @param map map
	 * @return map of translations (per language IDs)
	 * @throws NodeException
	 */
	public static Map<Integer, String> fromI18nMap(Map<String, String> map) throws NodeException {
		Map<Integer, String> translations = new TreeMap<>();

		if (!ObjectTransformer.isEmpty(map)) {
			for (UserLanguage lang : UserLanguageFactory.getActive()) {
				translations.put(lang.getId(), map.getOrDefault(lang.getCode(), null));
			}
		}

		return translations;
	}

	/**
	 * For all non-null entries in the translation map, call the consumer with language ID and translation
	 * @param map translation map
	 * @param consumer consumer
	 * @throws NodeException
	 */
	public static void forI18nMap(Map<String, String> map, BiConsumer<String, Integer> consumer) throws NodeException {
		if (!ObjectTransformer.isEmpty(map)) {
			for (UserLanguage lang : UserLanguageFactory.getActive()) {
				String translation = map.getOrDefault(lang.getCode(), null);
				if (translation != null) {
					consumer.accept(translation, lang.getId());
				}
			}
		}
	}
}
