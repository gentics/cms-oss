package com.gentics.contentnode.object.parttype.handlebars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.ResolvableComparator;
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
	public static String render(Object renderable, Options options) {
		if (renderable instanceof Renderable) {
			try {
				return ((Renderable) renderable).render();
			} catch (NodeException e) {
				// TODO
				throw new RuntimeException(e);
			}
		} else if (renderable != null) {
			return renderable.toString();
		} else {
			return null;
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
	public static Collection<Object> sort(Object objects, String sortBy, String sortOrder, Options options) {
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
}
