package com.gentics.contentnode.tests.publish.attributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.content.GenticsContentObject;

/**
 * Publish handler implementation that collects the available attributes in updated or created objects.
 * This only works for non-mccr CRs
 */
public class AttributeCollectingPublishHandler implements CnMapPublishHandler {
	/**
	 * Map of accessed attribute names
	 */
	protected static Map<String, Set<String>> accessedAttributes = new HashMap<>();

	/**
	 * Reset collected data
	 */
	public static void reset() {
		accessedAttributes.clear();
	}

	/**
	 * Get the attributes collected for the given object
	 * @param object object
	 * @return set of collected attributes (may be empty, but never null)
	 */
	public static Set<String> get(NodeObject object) {
		int ttype = object.getTType();
		if (ttype == ContentFile.TYPE_IMAGE) {
			ttype = ContentFile.TYPE_FILE;
		}
		String contentId = ttype + "." + object.getId();

		return accessedAttributes.getOrDefault(contentId, Collections.emptySet());
	}

	@Override
	public void init(@SuppressWarnings("rawtypes") Map parameters) throws CnMapPublishException {
	}

	@Override
	public void open(long timestamp) throws CnMapPublishException {
	}

	@Override
	public void createObject(Resolvable object) throws CnMapPublishException {
		handle(object);
	}

	@Override
	public void updateObject(Resolvable object) throws CnMapPublishException {
		handle(object);
	}

	@Override
	public void deleteObject(Resolvable object) throws CnMapPublishException {
	}

	/**
	 * Collect the attributes available in the object
	 * @param object object
	 */
	protected void handle(Resolvable object) {
		if (object instanceof GenticsContentObject) {
			GenticsContentObject gcnObject = (GenticsContentObject) object;
			accessedAttributes.computeIfAbsent(gcnObject.getContentId(), key -> new HashSet<>()).addAll(
					Arrays.asList(gcnObject.getAccessedAttributeNames(false)));
		}
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public void close() {
	}

	@Override
	public void destroy() {
	}
}
