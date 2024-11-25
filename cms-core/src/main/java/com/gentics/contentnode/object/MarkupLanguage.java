/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: MarkupLanguage.java,v 1.4 2007-03-23 14:28:52 clemens Exp $
 */
package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TType;

/**
 * This is an object for markup languages for content.
 */
@TType(MarkupLanguage.TYPE_MARKUPLANGUAGE)
public abstract class MarkupLanguage extends AbstractContentObject implements NamedNodeObject {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1586638044668587739L;

	public final static int TYPE_MARKUPLANGUAGE = 10201;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<MarkupLanguage>> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<MarkupLanguage, com.gentics.contentnode.rest.model.MarkupLanguage, com.gentics.contentnode.rest.model.MarkupLanguage> NODE2REST = (
			nodeMl, ml) -> {
		ml.setId(ObjectTransformer.getInt(nodeMl.getId(), 0));
		ml.setName(nodeMl.getName());
		ml.setExtension(nodeMl.getExtension());
		ml.setContentType(nodeMl.getContentType());
		if (nodeMl.getFeature() != null) {
			ml.setFeature(nodeMl.getFeature().getName());
		}
		ml.setExcludeFromPublishing(nodeMl.isExcludeFromPublishing());
		return ml;
	};

	/**
	 * Lambda that transforms the node model of a MarkupLanguage into the rest model
	 */
	public final static Function<MarkupLanguage, com.gentics.contentnode.rest.model.MarkupLanguage> TRANSFORM2REST = nodeMl -> {
		return NODE2REST.apply(nodeMl, new com.gentics.contentnode.rest.model.MarkupLanguage());
	};

	static {
		resolvableProperties = new HashMap<>();
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName(), "name"));
		resolvableProperties.put("extension", new NodeObjectProperty<>((o, key) -> o.getExtension(), "extension"));
		resolvableProperties.put("contenttype", new NodeObjectProperty<>((o, key) -> o.getContentType(), "filetype"));
		resolvableProperties.put("feature", new NodeObjectProperty<>((o, key) -> o.getFeature(), "feature"));
		resolvableProperties.put("excludeFromPublishing", new NodeObjectProperty<>((o, key) -> o.isExcludeFromPublishing(), "excludeFromPublishing"));

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected MarkupLanguage(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * get the human readable name of the markup language.
	 * @return the markup language name.
	 */
	public abstract String getName();

	/**
	 * get the default extention for files for this markup language.
	 * @return the extention of files storing content in this markup language.
	 */
	public abstract String getExtension();

	/**
	 * Get the mimetype for this markup language.
	 * @return the mimetype for this markup language.
	 */
	public abstract String getContentType();

	/**
	 * Get the optional feature, to which the markup language is bound (may be null)
	 * @return feature or null
	 */
	public abstract Feature getFeature();

	/**
	 * Get the "excludeFromPublishing" flag, which determines whether pages using templates with this markup language should be excluded from nurmal publishing
	 * @return excludeFromPublishing flag
	 */
	public abstract boolean isExcludeFromPublishing();

	public Object get(String key) {
		NodeObjectProperty<MarkupLanguage> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}
}
