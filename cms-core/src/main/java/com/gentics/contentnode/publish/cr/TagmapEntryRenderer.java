package com.gentics.contentnode.publish.cr;

import static com.gentics.contentnode.devtools.Synchronizer.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.parser.tag.struct.ParseStructRenderer;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * Interface for a published tagmap entry
 */
public interface TagmapEntryRenderer {
	/**
	 * Get the map name
	 * @return map name
	 */
	String getMapname();

	/**
	 * Get the tag name
	 * @return tag name
	 */
	String getTagname();

	/**
	 * Check whether the tagmap entry is for links
	 * @return true for links
	 */
	default boolean isLinkAttribute() {
		switch (getAttributeType()) {
		case 2:
		case 7:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Get the object type
	 * @return object type
	 */
	int getObjectType();

	/**
	 * Get the attribute type
	 * @return attribute type
	 */
	int getAttributeType();

	/**
	 * Get the target object type for link attributes
	 * @return target object type
	 */
	int getTargetType();

	/**
	 * Check whether the tagmap entry is multivalue
	 * @return true for multivalue
	 */
	boolean isMultivalue();

	/**
	 * Transform the value, so that it can be stored in the CR
	 * @param value value to transform
	 * @param linkTransformer function that transforms a value to a link
	 * @return transformed value
	 */
	default Object transformValue(Object value, BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer) {
		// when this is a mulitvalue, every single value must be transformed
		if (value instanceof Collection) {
			return ((Collection<?>) value).stream().map(v -> transformValue(v, linkTransformer)).collect(Collectors.toList());
		} else if (value instanceof Object[]) {
			return Arrays.asList((Object[]) value).stream().map(v -> transformValue(v, linkTransformer)).collect(Collectors.toList());
		} else if (isLinkAttribute() && !ObjectTransformer.isEmpty(value)) {
			try {
				if (linkTransformer == null) {
					throw new NodeException(String.format("Cannot render tagmap entry %s -> %s of type %d: no link transformer given", getTagname(),
							getMapname(), getAttributeType()));
				}
				return linkTransformer.apply(this, value);
			} catch (NodeException e) {
				logger.error(String.format("Error while rendering tagmap entry %s -> %s", getTagname(), getMapname()), e);
				return null;
			}
		} else if (value instanceof DatasourceEntry) {
			// we want to write the value of the datasource entry .. not the toString ..
			return ((DatasourceEntry) value).getValue();
		} else {
			return value;
		}
	}

	/**
	 * Get the rendered, transformed value of this tagmap entry. 
	 * This method will make sure that tags are rendered the same way, like they would be rendered within page contents.
	 * @param renderType rendertype
	 * @param renderResult renderresult
	 * @param linkTransformer function that transforms a value to a link
	 * @return rendered, transformed value of this tagmap entry
	 * @throws NodeException
	 */
	default Object getRenderedTransformedValue(RenderType renderType, RenderResult renderResult,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer) throws NodeException {
		// first resolve the entry into something which can be rendered
		Object value = CnMapPublisher.resolveTagmapEntry(renderType, renderResult, this);
		// next transform the value
		Object transformedValue = null;

		if (value instanceof Tag) {
			if (AttributeType.getForType(getAttributeType()) == AttributeType.micronode) {
				// for attributes of type "micronode", a tag just stays a tag (will be transformed to the micronode later)
				transformedValue = value;
			} else {
				Tag tag = (Tag) value;
				StringBuffer source = new StringBuffer();
				// use the ParseStructRenderer to render that tag, because this will correctly render that tag in edit mode (if required)
				ParseStructRenderer.renderClosedTag(tag, source, new ArrayList<>(), new ArrayList<>(), renderResult);

				transformedValue = transformValue(source.toString(), linkTransformer);
			}
		} else if (value instanceof Page && !isLinkAttribute()) {
			Page page = (Page) value;
			TemplateRenderer eRenderer = RendererFactory.getRenderer(RendererFactory.RENDERER_METAEDITABLE);

			String source = eRenderer.render(renderResult, page.getTemplate().getSource());
			TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

			// add dependency on the templates source
			if (renderType.doHandleDependencies()) {
				DependencyObject depObject = new DependencyObject(page.getTemplate(), (NodeObject) null);
				renderType.addDependency(depObject, "source");
			}
			transformedValue = transformValue(renderer.render(renderResult, source), linkTransformer);
		} else if (value instanceof GCNRenderable && !isLinkAttribute()) {
			RenderResult result = new RenderResult();

			transformedValue = transformValue(((GCNRenderable) value).render(result), linkTransformer);
		} else {
			transformedValue = transformValue(value, linkTransformer);
		}

		return transformedValue;
	}

	/**
	 * Return whether the tagmap entry may be skipped (when using attribute dirting)
	 * @return true iff the tagmap entry may be skipped
	 */
	boolean canSkip();

	/**
	 * Determine, whether the renderer can be skipped for the given set of modified attributes.
	 * The default implementation returns true, when all of these apply:
	 * <ol>
	 * <li>{@link #canSkip()} returns true</li>
	 * <li>attributes is not empty or null</li>
	 * <li>attributes does not contain the mapname of the renderer</li>
	 * </ol>
	 * @param attributes set of attributes
	 * @return true iff it can be skipped, false if not
	 */
	default boolean skip(Set<String> attributes) {
		// renderer cannot be skipped or no attributes given => do not skip
		if (!canSkip() || ObjectTransformer.isEmpty(attributes)) {
			return false;
		}

		// mapname of entry is listed in attributes => do not skip
		if (attributes.contains(getMapname())) {
			return false;
		}

		return true;
	}
}
