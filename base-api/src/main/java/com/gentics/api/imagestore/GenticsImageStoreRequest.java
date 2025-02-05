package com.gentics.api.imagestore;

import java.util.Map;

import jakarta.servlet.http.Cookie;

/**
 * Bean for accessing and modifying request parameters before they are sent to the GenticsImageStore
 * @author Taylor
 *
 */
public class GenticsImageStoreRequest {
	
	/**
	 * Path to the image called by the request to the GenticsImageStore
	 */
	private String imageUri;
	
	/**
	 * Query parameters passed with the request to the GenticsImageStore as a single String
	 */
	private String queryString;
	
	/**
	 * List of cookies sent along with the request to the GenticsImageStore
	 */
	private Cookie[] cookies;
	
	/**
	 * Key/value Map of the headers included with the request to the GenticsImageStore
	 */
	private Map<String, String> headers;
	
	/**
	 * Returns the path to the image called by the request to the GenticsImageStore
	 * @return Path to the image
	 */
	public String getImageUri() {
		return imageUri;
	}
	
	/**
	 * @param imageUri image path to set
	 */
	public void setImageUri(String imageUri) {
		this.imageUri = imageUri;
	}
	
	/**
	 * Returns all query parameters sent along with the request to the GenticsImageStore as a String
	 * @return Query String
	 */
	public String getQueryString() {
		return queryString;
	}
	
	/**
	 * @param queryString the String of query parameters to set
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	/**
	 * Returns a list of cookies included with the request to the GenticsImageStore
	 * @return List of cookies
	 */
	public Cookie[] getCookies() {
		return cookies;
	}
	
	/**
	 * @param cookies the list of cookies included to set
	 */
	public void setCookies(Cookie[] cookies) {
		this.cookies = cookies;
	}
	
	/**
	 * Return a key/value Map of headers included with the request to the GenticsImageStore
	 * @return Map of header values 
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}
	
	/**
	 * @param headers the key/value Map of headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

}
