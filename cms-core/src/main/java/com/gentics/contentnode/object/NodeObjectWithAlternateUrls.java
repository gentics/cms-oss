package com.gentics.contentnode.object;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.etc.StringUtils;

/**
 * Interface for {@link NodeObject} implementations that contain alternate URLs
 */
public interface NodeObjectWithAlternateUrls extends NodeObject {
	/**
	 * Function that will return the "path" portion of the nice URL.
	 * If the url is empty, this will return null.
	 */
	public static Function<String, String> PATH = url -> {
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		int lastSlashIndex = url.lastIndexOf('/');
		return url.substring(0, lastSlashIndex + 1);
	};

	/**
	 * Function that will return the "name" portion of the nice URL.
	 * If the url is empty, this will return null.
	 */
	public static Function<String, String> NAME = url -> {
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		int lastSlashIndex = url.lastIndexOf('/');
		return url.substring(lastSlashIndex + 1);
	};

	/**
	 * Get the (optional) nice URL of the object
	 * @return nice URL or null if not set
	 */
	@FieldGetter("nice_url")
	String getNiceUrl();

	/**
	 * Set the nice URL of the object
	 * @param niceUrl nice URL
	 * @throws NodeException
	 */
	@FieldSetter("nice_url")
	default void setNiceUrl(String niceUrl) throws NodeException {
		throw new ReadOnlyException();
	}

	/**
	 * Get the set of alternate URLs for the object. This will never return null, but could return an empty set, if no alternate URLs are set, or the feature nice_urls is not activated
	 * @return set of alternate URLs
	 * @throws NodeException
	 */
	@FieldGetter("alternate_urls")
	Set<String> getAlternateUrls() throws NodeException;

	/**
	 * Set the set of alternate URLs for the object. Setting to null does not have any effect, to remove the currently set alternate URLs, this method must be called with an empty set.
	 * @param alternateUrls set of alternate URLs to set.
	 * @throws NodeException
	 */
	@FieldSetter("alternate_urls")
	default void setAlternateUrls(Set<String> alternateUrls) throws NodeException {
		throw new ReadOnlyException();
	}

	/**
	 * Convenience method to set the urls as variable parameters
	 * @param urls alternate URLs to set
	 * @throws NodeException
	 */
	default void setAlternateUrls(String...urls) throws NodeException {
		setAlternateUrls(new HashSet<>(Arrays.asList(urls)));
	}

	/**
	 * Get a the nice URL and all alternate URLs as combined set.
	 * @return set containing the nice URL (if not null) and all alternate URLs
	 * @throws NodeException
	 */
	default Set<String> getNiceAndAlternateUrls() throws NodeException {
		Set<String> combinedSet = new TreeSet<>();
		if (getNiceUrl() != null) {
			combinedSet.add(getNiceUrl());
		}
		combinedSet.addAll(getAlternateUrls());
		return combinedSet;
	}

	/**
	 * If the object supports versioning, get the {@link TableVersion} instance of the alternate URLs.
	 * Otherwise return null.
	 * @return table version instance or null
	 * @throws NodeException
	 */
	default TableVersion getAlternateUrlTableVersion() throws NodeException {
		return null;
	}
}
