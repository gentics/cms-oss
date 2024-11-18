package com.gentics.contentnode.object.parttype.handlebars;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.render.FormRendering;
import com.gentics.contentnode.render.GisRendering;
import com.gentics.contentnode.render.GisRendering.CropInfo;
import com.gentics.contentnode.render.GisRendering.ResizeInfo;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.lib.render.Renderable;
import com.gentics.lib.resolving.ResolvableMapWrapper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;

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
	public static String gtx_render(Object renderable, Options options) throws NodeException {
		if (renderable instanceof Renderable) {
			return ((Renderable) renderable).render();
		} else if (renderable != null) {
			return renderable.toString();
		} else {
			return null;
		}
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
			node = getObject(context, Node.class, NodePartType.class, NodePartType::getNode);
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
		ImageFile image = getObject(context, ImageFile.class, ImageURLPartType.class, ImageURLPartType::getTargetImage);
		ResizeInfo resizeInfo = new ResizeInfo(getParameters(options, GisRendering.WIDTH_ARG, GisRendering.HEIGHT_ARG,
				GisRendering.MODE_ARG, GisRendering.TYPE_ARG));
		Map<Object, Object> cropParameters = getParametersWithPrefix(options, "crop_", GisRendering.WIDTH_ARG, GisRendering.HEIGHT_ARG, GisRendering.X_ARG, GisRendering.Y_ARG);
		CropInfo cropInfo = cropParameters.isEmpty() ? null : new CropInfo(cropParameters);

		StringWriter writer = new StringWriter();
		GisRendering.render(image, resizeInfo, cropInfo, writer);

		return writer.toString();
	}

	/**
	 * Form helper
	 * @param context context
	 * @param options options
	 * @return rendered form
	 * @throws NodeException
	 * @throws IOException
	 */
	public static CharSequence gtx_form(Object context, Options options) throws NodeException, IOException {
		Form form = getObject(context, Form.class, CmsFormPartType.class, CmsFormPartType::getTarget);
		FormRendering.Options formRenderingOptions = RenderUtils.getHandlebarsHelperObject(FormRendering.Options.class, options);

		if (options.tagType == TagType.SECTION) {
			if (form != null) {
				StringWriter writer = new StringWriter();
				FormRendering.render(form, formRenderingOptions, writer);
				return options.fn(writer.toString());
			} else {
				return options.inverse();
			}
		} else {
			StringWriter writer = new StringWriter();
			FormRendering.render(form, formRenderingOptions, writer);
			return writer.toString();
		}
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
			String translation = options.hash(code, getRenderedResolved(resolvableObject, code));
			if (StringUtils.isNotBlank(translation)) {
				return translation;
			}
		}

		return null;
	}

	/**
	 * Get the rendered resolved key from the resolvable or null if the resolvable is null.
	 * If the resolved value is a {@link Renderable}, it will be rendered.
	 * @param resolvable resolvable object (may be null)
	 * @param key resolved key (not null)
	 * @return rendered resolved value
	 */
	protected static String getRenderedResolved(Resolvable resolvable, String key) {
		if (resolvable == null) {
			return null;
		}

		Object resolvedValue = resolvable.get(key);
		if (resolvedValue instanceof Renderable) {
			try {
				return Renderable.class.cast(resolvedValue).render();
			} catch (NodeException e) {
				return null;
			}
		} else {
			return ObjectTransformer.getString(resolvedValue, null);
		}
	}

	/**
	 * Get the given parameters from the options as map
	 * @param options options
	 * @param names names to get
	 * @return map of values
	 */
	protected static Map<Object, Object> getParameters(Options options, String...names) {
		Map<Object, Object> map = new HashMap<>();

		for (String name : names) {
			Object value = options.hash(name);
			if (value != null) {
				map.put(name, value);
			}
		}

		return map;
	}

	/**
	 * Get the given parameters from the options as map. The prefix is prepended to each name before getting from options (but will not be prepended to returned keys)
	 * @param options options
	 * @param prefix prefix
	 * @param names names to get
	 * @return map of values
	 */
	protected static Map<Object, Object> getParametersWithPrefix(Options options, String prefix, String...names) {
		Map<Object, Object> map = new HashMap<>();

		for (String name : names) {
			Object value = options.hash(prefix + name);
			if (value != null) {
				map.put(name, value);
			}
		}

		return map;
	}

	/**
	 * Get the value as object of given class.
	 * The given value may be an instance of the class (optionally wrapped into a {@link ResolvableMapWrapper}) or the global or local ID
	 * @param <T> type of the requested class
	 * @param object value to transform
	 * @param classOfT requested class
	 * @return instance of requested class or null
	 * @throws NodeException
	 */
	protected static <T extends NodeObject> T getObject(Object object, Class<T> classOfT) throws NodeException {
		return getObject(object, classOfT, null, null);
	}

	/**
	 * Get the value as object of given class.
	 * The given value may be an instance of the class (optionally wrapped into a {@link ResolvableMapWrapper}), the global or local ID or a value using the given PartType. In the latter case, the valueExtractor is used to get the instance.
	 * @param <T> type of requested class
	 * @param <P> type of optional PartType
	 * @param object value to transform
	 * @param classOfT requested class
	 * @param classOfP optional class of PartType if the value is a Value
	 * @param valueExtractor value extractor to get the instance from the PartType
	 * @return instance of requested class or null
	 * @throws NodeException
	 */
	protected static <T extends NodeObject, P extends PartType> T getObject(Object object, Class<T> classOfT,
			Class<P> classOfP, Function<P, T> valueExtractor) throws NodeException {
		Object unwrapped = unwrapResolvableMapWrapper(object);

		if (classOfT.isInstance(unwrapped)) {
			return classOfT.cast(unwrapped);
		} else if (classOfP != null && valueExtractor != null && unwrapped instanceof Value) {
			PartType partType = Value.class.cast(unwrapped).getPartType();
			if (classOfP.isInstance(partType)) {
				return valueExtractor.apply(classOfP.cast(partType));
			}
		} else if (object != null) {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(classOfT, ObjectTransformer.getString(object, "0"));
		}

		return null;
	}

	/**
	 * Unwrap instances of {@link ResolvableMapWrapper} (recursively)
	 * @param object object to unwrap
	 * @return unwrapped object
	 */
	protected static Object unwrapResolvableMapWrapper(Object object) {
		if (object instanceof ResolvableMapWrapper) {
			return unwrapResolvableMapWrapper(((ResolvableMapWrapper)object).getWrapped());
		} else {
			return object;
		}
	}
}
