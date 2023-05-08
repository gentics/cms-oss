package com.gentics.contentnode.rest.util;

import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;

/**
 * Static helper class to create an instance of {@link AbstractListResponse} out
 * of a list of instances with filtering, sorting and paging
 *
 * @param <T> Type of input objects
 * @param <R> Type of output objects (REST Models)
 */
public class ListBuilder <T extends Object, R extends Object> {
	/**
	 * Create an instance for the list of objects
	 * @param <T> Type of input objects (NodeObjects)
	 * @param <R> Type of output objects (REST Models)
	 * @param objects list of objects
	 * @param transform transformer that transforms the NodeObjects to their REST Models
	 * @return ListBuilder instance
	 */
	public static <T extends Object, R extends Object> ListBuilder<T, R> from(Collection<T> objects,
			Function<T, R> transform) {
		return new ListBuilder<>(objects, transform);
	}

	/**
	 * List of input objects
	 */
	protected List<T> objects;

	/**
	 * Transformer
	 */
	protected Function<T, R> transform;

	/**
	 * Offset for paging
	 */
	protected int offset = 0;

	/**
	 * Maximum length for paging (-1 means: no maximum length)
	 */
	protected int length = -1;

	/**
	 * Optional comparator for sorting
	 */
	protected Comparator<T> comparator;

	/**
	 * List of filters
	 */
	protected List<Filter<T>> filters = new ArrayList<>();

	/**
	 * List of embed consumers
	 */
	protected List<Consumer<R>> embedConsumers = new ArrayList<>();

	/**
	 * Optional permission function, that will determine the current user's permissions on the
	 * given object and will return a pair consisting of the object ID and the set of granted permissions.
	 */
	protected Function<T, Pair<Integer, Set<Permission>>> permissionFunction;

	/**
	 * Create an instance
	 * @param objects list of objects
	 * @param transform transformer that transforms the NodeObjects to their REST Models
	 */
	protected ListBuilder(Collection<T> objects, Function<T, R> transform) {
		this.objects = new ArrayList<>(objects);
		this.transform = transform;
	}

	/**
	 * Add paging
	 * @param page page to be shown (1 is the first page)
	 * @param pageSize maximum number of objects in the result (-1: no limit)
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> page(int page, int pageSize) {
		if (pageSize >= 0) {
			this.offset = (page - 1) * pageSize;
			this.length = pageSize;
		} else {
			this.offset = 0;
			this.length = -1;
		}
		return this;
	}

	/**
	 * Add paging
	 * @param paging paging parameters
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> page(PagingParameterBean paging) {
		if (paging != null) {
			return page(paging.page, paging.pageSize);
		} else {
			return this;
		}
	}

	/**
	 * Filter the result
	 * @param filter filter instance (null for no filtering)
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> filter(Filter<T> filter) {
		if (filter != null) {
			filters.add(filter);
		}
		return this;
	}

	/**
	 * Sort the result
	 * @param comparator comparator for sorting (null for no sorting)
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> sort(Comparator<T> comparator) {
		this.comparator = comparator;
		return this;
	}

	/**
	 * Add the given consumer to the list of consumers, if the embed parameter bean contains the given attribute
	 * @param embed embed parameter bean
	 * @param attribute attribute
	 * @param consumer consumer
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> embed(EmbedParameterBean embed, String attribute, Consumer<R> consumer) {
			if (embeddedParameterContainsAttribute(embed, attribute)) {
				embedConsumers.add(consumer);
			}

		return this;
	}

	/**
	 * Add the permission function
	 * @param permissionFunction permission function
	 * @return instance for chaining
	 */
	public ListBuilder<T, R> perms(Function<T, Pair<Integer, Set<Permission>>> permissionFunction) {
		this.permissionFunction = permissionFunction;
		return this;
	}

	/**
	 * Transform the input list into the list of REST models, use filtering, sorting and paging as set before
	 * @param list list instance that will be filled with data
	 * @return list instance
	 * @throws NodeException
	 */
	public <U extends AbstractListResponse<R>> U to(U list) throws NodeException {
		// filter
		for (Filter<T> filter : filters) {
			filter.filter(objects);
		}

		// sort
		if (comparator != null) {
			objects.sort(comparator);
		}

		// page
		int numItems = objects.size();
		int[] range = calculateRange();
		reduceList();

		// transform objects and optionally attach permissions
		List<R> transformed = new ArrayList<>();
		Map<Integer, Set<Permission>> perms = null;
		if (permissionFunction != null) {
			perms = new HashMap<>();
			list.setPerms(perms);
		}
		for (T object : objects) {
			R transformedObject = transform.apply(object);
			for (Consumer<R> consumer : embedConsumers) {
				consumer.accept(transformedObject);
			}
			transformed.add(transformedObject);

			if (permissionFunction != null) {
				Pair<Integer, Set<Permission>> permInfo = permissionFunction.apply(object);
				if (permInfo != null && ObjectTransformer.getInt(permInfo.getKey(), 0) != 0) {
					perms.put(permInfo.getKey(), permInfo.getValue());
				}
			}
		}
		list.setItems(transformed);
		list.setNumItems(numItems);
		list.setHasMoreItems(range[1] < numItems);
		list.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
		return list;
	}

	/**
	 * Returns a tuple representing a range's start and indices, constrained to
	 * a given size.
	 *
	 * If a length of -1 is specified, then `size` will be the end index of the
	 * range.
	 *
	 * @param offset The offset index from which to begin the range.
	 * @param length The length of the range.
	 * @return The start index and end index of a range.
	 */
	private int[] calculateRange() {
		int from = Math.min(Math.max(0, offset), objects.size());
		int to = (length < 0) ? objects.size() : Math.min(from + length, objects.size());

		return new int[] { from, to};
	}

	/**
	 * Reduce the given list by skipping skipCount elements at the start and trimming to no more than maxItems elements
	 * @param list list of objects to be reduced
	 * @param skipCount number of elements to be skipped
	 * @param maxItems maximum number of items to be returned
	 */
	protected void reduceList() {
		// skip offset elements from the start
		if (offset > 0) {
			offset = Math.min(offset, objects.size());
			for (int i = 0; i < offset; i++) {
				objects.remove(0);
			}
		}

		if (length >= 0) {
			// remove elements from the end, until no more than length are present in the list
			while (objects.size() > 0 && objects.size() > length) {
				objects.remove(objects.size() - 1);
			}
		}
	}
}
