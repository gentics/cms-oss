/*
 * @author tobiassteiner
 * @date Feb 6, 2011
 * @version $Id: QueryStringUtils.java,v 1.1.2.1 2011-02-10 13:43:37 tobiassteiner Exp $
 */
package com.gentics.lib.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Provides static utility methods for query strings.
 */
public class QueryStringUtils {

	/**
	 * @param queryString a query string like provided by the QUERY_STRING
	 *   CGI environment variable.
	 *   
	 * @return a map of parameter name to parameter values. Since a query
	 *   string may contain multiple values for each name, the values of
	 *   the map are arrays (without the first '?' character). The
	 *   parameter entries in the map will have the same order as those
	 *   in the query string, but if multiple values for a parameter are
	 *   encountered, they will be added to the first occurrence. 
	 *   This map is mutable and can be altered at will.
	 *   
	 * @throws UnsupportedEncodingException 
	 */
	public static Map<String, String[]> parseQueryString(String queryString, String encoding) throws UnsupportedEncodingException {
		// LinkedHashMap to preserve order
		Map<String, String[]> params = new LinkedHashMap<String, String[]>();
		StringTokenizer tokenizer = new StringTokenizer(queryString, "&");

		while (tokenizer.hasMoreTokens()) {
			String nameToValue = tokenizer.nextToken();
			int eqOffset = nameToValue.indexOf('=');
			String paramName;
			String paramValue;

			if (-1 != eqOffset) {
				paramName = nameToValue.substring(0, eqOffset);
				// skip the equals
				paramValue = nameToValue.substring(eqOffset + 1);
			} else {
				// no equals character. use the empty string as value.
				// this may loose information, since it isn't known
				// whether the original query string had an equals
				// character or not.
				paramName = nameToValue;
				paramValue = "";
			}
			String key = URLDecoder.decode(paramName, encoding);
			String value = URLDecoder.decode(paramValue, encoding);
			String[] values = params.get(key);

			if (null == values) {
				values = new String[] { value };
			} else {
				// usually there will only be a single value, so the following 
				// operation shouldn't be unduly inefficient in practice.
				String[] extendedValues = new String[values.length + 1];

				System.arraycopy(values, 0, extendedValues, 0, values.length);
				extendedValues[extendedValues.length - 1] = value;
				values = extendedValues;
			}
			params.put(key, values);
		}
		return params;
	}
    
	/**
	 * @param params a map of parameter names to parameter values. 
	 * @return a query string like provided by the QUERY_STRING CGI
	 *   environment variable (without the first '?' character).
	 * @throws UnsupportedEncodingException 
	 */
	public static String buildQueryString(Map<String, String[]> params, String encoding) throws UnsupportedEncodingException {
		StringBuilder queryString = new StringBuilder();

		for (Map.Entry<String, String[]> param : params.entrySet()) {
			String key = URLEncoder.encode(param.getKey(), encoding);

			for (String valueUnencoded : param.getValue()) {
				String value = URLEncoder.encode(valueUnencoded, encoding);

				if (0 < queryString.length()) {
					queryString.append("&");
				}
				queryString.append(key).append("=").append(value);
			}
		}
		return queryString.toString();
	}
}
