/*
 * @author norbert
 * @date 30.09.2010
 * @version $Id: JSONResolvable.java,v 1.2 2010-11-09 09:58:59 clemens Exp $
 */
package com.gentics.api.lib.resolving;

import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.gentics.lib.log.NodeLogger;

/**
 * Bridge class between {@link JSONObject} and {@link Resolvable}. Wraps an
 * instance of {@link JSONObject} and resolves properties.
 */
public class JSONResolvable implements Resolvable {

	/**
	 * wapped JSON object
	 */
	protected JSONObject jsonObject;

	/**
	 * Create an instance of the JSON Resolvable wrapper
	 * @param jsonObject wrapped JSON object
	 */
	public JSONResolvable(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		try {
			if (jsonObject.isNull(key)) {
				return null;
			} else {
				return transformValue(jsonObject.get(key));
			}
		} catch (JSONException e) {
			NodeLogger.getNodeLogger(getClass()).warn("Error while resolving key {" + key + "}", e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/**
	 * Helper method to transform the given value from JSON to Resolvable
	 * @param value value to transform
	 * @return transformed value
	 * @throws JSONException if transformation fails
	 */
	protected static Object transformValue(Object value) throws JSONException {
		if (value instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) value;
			List valueList = new Vector();

			for (int i = 0; i < jsonArray.length(); ++i) {
				valueList.add(transformValue(jsonArray.get(i)));
			}
			return valueList;
		} else if (value instanceof JSONObject) {
			return new JSONResolvable((JSONObject) value);
		} else {
			return value;
		}
	}
}
