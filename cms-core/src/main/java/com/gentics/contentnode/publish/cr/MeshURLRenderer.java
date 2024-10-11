package com.gentics.contentnode.publish.cr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * TagmapEntryRenderer implementation for the gtx_url
 */
public class MeshURLRenderer implements TagmapEntryRenderer {
	/**
	 * Map containing the tagnames for types, which support publishing the URLs
	 */
	private final static Map<Integer, String> tagnameMap = new HashMap<>();

	static {
		tagnameMap.put(Page.TYPE_PAGE, "page.url");
		tagnameMap.put(File.TYPE_FILE, "file.url");
	}

	/**
	 * Check whether the given type is supported
	 * @param objType object type
	 * @return true iff type is supported
	 */
	public static boolean supportsType(int objType) {
		return tagnameMap.containsKey(objType);
	}

	/**
	 * Tagname
	 */
	protected String tagname;

	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Create an instance for the given type
	 * @param objType type
	 * @throws NodeException if type is not supported
	 */
	public MeshURLRenderer(int objType) throws NodeException {
		if (!tagnameMap.containsKey(objType)) {
			throw new NodeException(String.format("Cannot create path renderer for object type %d", objType));
		}
		tagname = tagnameMap.get(objType);
		this.objType = objType;
	}

	@Override
	public String getMapname() {
		return MeshPublisher.FIELD_GTX_URL;
	}

	@Override
	public String getTagname() {
		return tagname;
	}

	@Override
	public int getObjectType() {
		return objType;
	}

	@Override
	public int getAttributeType() {
		return AttributeType.text.getType();
	}

	@Override
	public int getTargetType() {
		return 0;
	}

	@Override
	public boolean isMultivalue() {
		return false;
	}

	@Override
	public boolean canSkip() {
		return true;
	}

	@Override
	public boolean skip(Set<String> attributes) {
		if (objType == File.TYPE_FILE && !ObjectTransformer.isEmpty(attributes) && attributes.contains("name")) {
			return false;
		}
		if (objType == Page.TYPE_PAGE && !ObjectTransformer.isEmpty(attributes) && attributes.contains("filename")) {
			return false;
		}
		return TagmapEntryRenderer.super.skip(attributes);
	}

	@Override
	public Object getRenderedTransformedValue(RenderType renderType, RenderResult renderResult,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer) throws NodeException {
		StackResolvable renderedObject = renderType.getRenderedRootObject();

		// render the URL for pages and files.
		// do not include the node publish path, because this will be set as branch prefix in Mesh
		if (renderedObject instanceof Page) {
			Page page = (Page) renderedObject;
			return String.format("%s%s", page.getFullPublishPath(true, false), page.getFilename());
		} else if (renderedObject instanceof File) {
			File file = (File) renderedObject;
			return String.format("%s%s", file.getFullPublishPath(true, false), file.getFilename());
		} else {
			return "";
		}
	}
}
