package com.gentics.contentnode.publish.cr;

import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * TagmapEntryRenderer implementation for Roles
 */
public class MeshRoleRenderer implements TagmapEntryRenderer {
	public final static String MAPNAME = "mesh_permission_role";

	protected int objectType;

	protected String tagname;

	/**
	 * Create an instance
	 * @param objectType object type
	 * @param tagname tagname
	 */
	public MeshRoleRenderer(int objectType, String tagname) {
		this.objectType = objectType;
		this.tagname = tagname;
	}

	@Override
	public String getMapname() {
		return MAPNAME;
	}

	@Override
	public String getTagname() {
		return tagname;
	}

	@Override
	public int getObjectType() {
		return objectType;
	}

	@Override
	public int getAttributeType() {
		return GenticsContentAttribute.ATTR_TYPE_TEXT;
	}

	@Override
	public int getTargetType() {
		return 0;
	}

	@Override
	public boolean isMultivalue() {
		return true;
	}

	@Override
	public Object getRenderedTransformedValue(RenderType renderType, RenderResult renderResult, BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer)
			throws NodeException {
		// first resolve the entry into something which can be rendered
		Object value = CnMapPublisher.resolveTagmapEntry(renderType, renderResult, this);
		if (value instanceof Tag) {
			Tag tag = (Tag)value;
			ValueList values = tag.getValues();
			for (Value v : values) {
				PartType partType = v.getPartType();
				if (partType instanceof SelectPartType) {
					return ((SelectPartType) partType).getSelection().stream().map(e -> e.getValue()).collect(Collectors.toList());
				}
			}
		}
		return TagmapEntryRenderer.super.getRenderedTransformedValue(renderType, renderResult, linkTransformer);
	}

	@Override
	public boolean canSkip() {
		// the MeshRoleRenderer must not be skipped. Otherwise the MeshPublisher would set the default permissions.
		return false;
	}
}
