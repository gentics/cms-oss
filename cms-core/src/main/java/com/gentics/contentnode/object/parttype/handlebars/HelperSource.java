package com.gentics.contentnode.object.parttype.handlebars;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.render.GisRendering;
import com.gentics.contentnode.render.GisRendering.CropInfo;
import com.gentics.contentnode.render.GisRendering.ResizeInfo;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.render.RenderableResolvable.Scope;
import com.gentics.contentnode.resolving.ResolvableMapWrappable;
import com.gentics.contentnode.resolving.ResolvableMapWrapper;
import com.gentics.contentnode.resolving.ResolvableMapWrapper.RenderContext;
import com.gentics.lib.render.Renderable;
import com.github.jknack.handlebars.Options;

/**
 * Source for helpers used when rendering a {@link HandlebarsPartType}
 */
public class HelperSource {
	/**
	 * Render helper
	 * @param renderable renderable to render
	 * @param options options
	 * @return rendered renderable
	 */
	public static String gtx_render(Object value, Options options) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		RenderResult renderResult = t.getRenderResult();

		if (value instanceof ResolvableMapWrapper mapWrapper) {
			ResolvableMapWrappable wrapped = mapWrapper.getWrapped();
			try (RenderContext renderContext = mapWrapper.withContext()) {
				if (wrapped instanceof Tag tag) {
					return RenderUtils.renderTag(tag, renderType, renderResult);
				} else if (wrapped instanceof Renderable renderable) {
					return renderable.render();
				} else if (wrapped != null) {
					return wrapped.toString();
				} else {
					return null;
				}
			}
		}
		if (value instanceof RenderableResolvable renderable) {
			if (renderable.getWrappedObject() instanceof Tag tag) {
				try (Scope scope = renderable.scope()) {
					return RenderUtils.renderTag(tag, renderType, renderResult);
				}
			} else {
				return renderable.toString();
			}
		} else if (value instanceof Tag tag) {
			return RenderUtils.renderTag(tag, renderType, renderResult);
		} else if (value instanceof Renderable renderable) {
			return renderable.render();
		} else if (value != null) {
			return value.toString();
		}
		return null;
	}

	/**
	 * Edit helper
	 * @param renderable renderable to render in edit mode
	 * @param options options
	 * @return rendered renderable
	 * @throws NodeException
	 */
	public static String gtx_edit(Object renderable, Options options) throws NodeException {
		RenderType renderType = null;
		boolean editModeSet = false;
		int currentEditMode = 0;

		try {
			// switch back to the original rendermode, if the rendermode was changed for rendering the velocity tag
			renderType = TransactionManager.getCurrentTransaction().getRenderType();
			// we do this by getting rendermde.editMode from the CMSResolver, which will also take into consideration, whether
			// we are rendering a foreign object or not (not edit mode for foreign objects)
			int editMode = Optional.ofNullable(renderType.getCMSResolver()).map(cms -> {
				try {
					return ObjectTransformer.getInt(PropertyResolver.resolve(cms, "rendermode.editMode", false), -1);
				} catch (UnknownPropertyException e) {
					return -1;
				}
			}).orElse(-1);

			currentEditMode = renderType.getEditMode();

			if (editMode > 0 && editMode != currentEditMode) {
				renderType.setEditMode(editMode);
				editModeSet = true;
			}
			return gtx_render(renderable, options);
		} finally {
			// if the original rendermode was restored for rendering the directive, we switch back to the rendermode,
			// that was chosen for rendering the velocity tag
			if (editModeSet) {
				renderType.setEditMode(currentEditMode);
			}
		}
	}

	/**
	 * Sort helper
	 * @param objects objects to be sorted
	 * @param sortBy sort by
	 * @param sortOrder sort order
	 * @param options options
	 * @return sorted objects
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> gtx_sort(Object objects, String sortBy, String sortOrder, Options options) {
		Collection<Object> objectsToSort = null;
		if (objects instanceof Collection<?>) {
			objectsToSort = (Collection<Object>)objects;
		} else if (objects instanceof Map<?, ?>) {
			objectsToSort = ((Map<Object, Object>)objects).values();
		}

		if (StringUtils.isBlank(sortBy) || CollectionUtils.isEmpty(objectsToSort)) {
			return objectsToSort;
		}

		int iSortOrder = Datasource.SORTORDER_ASC;
		if (StringUtils.equalsIgnoreCase(sortOrder, "desc")) {
			iSortOrder = Datasource.SORTORDER_DESC;
		}

		ResolvableComparator comparator = new ResolvableComparator(sortBy, iSortOrder);
		List<Object> sortedList = new ArrayList<>(objectsToSort);

		Collections.sort(sortedList, comparator);

		return sortedList;
	}

	/**
	 * Channel helper
	 * @param context context
	 * @param options options
	 * @return rendered block
	 * @throws IOException
	 * @throws NodeException
	 */
	public static CharSequence gtx_channel(Object context, Options options) throws IOException, NodeException {
		Node node = null;
		try (final NoMcTrx nMcTrx = new NoMcTrx()) {
			node = RenderUtils.getObject(context, Node.class, NodePartType.class, NodePartType::getNode);
		}
		if (node == null) {
			return options.fn();
		}

		try (final ChannelTrx trx = new ChannelTrx(node)) {
			return options.fn();
		}
	}

	/**
	 * Gis helper
	 * @param context context
	 * @param options options
	 * @return rendered gis URL
	 * @throws NodeException
	 * @throws IOException
	 */
	public static CharSequence gtx_gis(Object context, Options options) throws NodeException, IOException {
		ImageFile image = RenderUtils.getObject(context, ImageFile.class, ImageURLPartType.class, ImageURLPartType::getTargetImage);
		ResizeInfo resizeInfo = new ResizeInfo(RenderUtils.getParameters(options, GisRendering.WIDTH_ARG, GisRendering.HEIGHT_ARG,
				GisRendering.MODE_ARG, GisRendering.TYPE_ARG));
		Map<Object, Object> cropParameters = RenderUtils.getParametersWithPrefix(options, "crop_", GisRendering.WIDTH_ARG, GisRendering.HEIGHT_ARG, GisRendering.X_ARG, GisRendering.Y_ARG);
		CropInfo cropInfo = cropParameters.isEmpty() ? null : new CropInfo(cropParameters);

		StringWriter writer = new StringWriter();
		GisRendering.render(image, resizeInfo, cropInfo, writer);

		return writer.toString();
	}

	/**
	 * I18n helper
	 * @param object optional context object to resolve from
	 * @param options options
	 * @return rendered translation or null
	 * @throws NodeException
	 */
	public static CharSequence gtx_i18n(Object object, Options options) throws NodeException {
		List<String> languages = ListUtils.union(RenderUtils.getLanguages(), Collections.singletonList("default"));
		Resolvable resolvableObject = object instanceof Resolvable ? Resolvable.class.cast(object) : null;

		for (String code : languages) {
			String translation = options.hash(code, RenderUtils.getRenderedResolved(resolvableObject, code));
			if (StringUtils.isNotBlank(translation)) {
				return translation;
			}
		}

		return null;
	}
}
