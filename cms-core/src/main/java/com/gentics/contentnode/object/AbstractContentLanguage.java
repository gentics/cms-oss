package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.ReadOnlyException;

public abstract class AbstractContentLanguage extends AbstractContentObject implements ContentLanguage {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5638163014580381220L;

	protected static Map<String, Property> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	static {
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			public Object get(ContentLanguage lang, String key) {
				return lang.getName();
			}
		});
		resolvableProperties.put("code", new Property(new String[] { "code"}) {
			public Object get(ContentLanguage lang, String key) {
				return lang.getCode();
			}
		});

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected AbstractContentLanguage(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	@Override
	public void setName(String name) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setCode(String code) throws ReadOnlyException {
		failReadOnly();
	}

	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 */
		public abstract Object get(ContentLanguage object, String key);
	}
}
