package com.gentics.contentnode.mcp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Definition of the fields of an object that an MCP tool can change, for reporting which of them
 * actually changed ({@code changedFields}): take a {@link #snapshot} of the object before the
 * change, and {@link Snapshot#diff} it against the object after the change.
 *
 * <p>
 * Values are compared with {@link Objects#equals}, so {@code null} and {@code ""} differ, and
 * values must implement {@code equals} (strings, boxed numbers, lists of those, records - not
 * mutable REST model objects, which are compared by reference). A snapshot keeps the values
 * returned by the getters, not copies of them.
 * </p>
 *
 * <pre>
 * static final ChangedFields&lt;Page&gt; CHANGED_FIELDS = ChangedFields.&lt;Page&gt;builder()
 * 		.field("name", Page::getName).field("fileName", Page::getFileName).build();
 *
 * ChangedFields.Snapshot&lt;Page&gt; before = CHANGED_FIELDS.snapshot(loaded);
 * // ... change and reload the page ...
 * List&lt;String&gt; changedFields = before.diff(reloaded);
 * </pre>
 *
 * @param <T> type of the object
 */
public final class ChangedFields<T> {
	private final List<Field<T>> fields;

	private ChangedFields(List<Field<T>> fields) {
		this.fields = List.copyOf(fields);
	}

	/**
	 * Create a builder
	 * @param <T> type of the object
	 * @return builder
	 */
	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Get the names of the fields, in declaration order
	 * @return field names
	 */
	public List<String> names() {
		return fields.stream().map(Field::name).toList();
	}

	/**
	 * Take the snapshot of the given object
	 * @param object object
	 * @return snapshot
	 */
	public Snapshot<T> snapshot(T object) {
		Objects.requireNonNull(object, "Cannot take a snapshot of null");
		return new Snapshot<>(this, values(object));
	}

	/**
	 * Get the values of all fields of the object
	 * @param object object
	 * @return values in declaration order, may contain nulls
	 */
	private List<Object> values(T object) {
		List<Object> values = new ArrayList<>(fields.size());
		for (Field<T> field : fields) {
			values.add(field.getter().apply(object));
		}
		return Collections.unmodifiableList(values);
	}

	/**
	 * Snapshot of the field values of an object
	 * @param <T> type of the object
	 */
	public static final class Snapshot<T> {
		private final ChangedFields<T> definition;

		private final List<Object> values;

		private Snapshot(ChangedFields<T> definition, List<Object> values) {
			this.definition = definition;
			this.values = values;
		}

		/**
		 * Get the names of the fields whose value in the given object differs from this snapshot
		 * @param after object after the change
		 * @return changed field names, in declaration order
		 */
		public List<String> diff(T after) {
			Objects.requireNonNull(after, "Cannot diff against null");
			List<Object> afterValues = definition.values(after);
			List<String> changed = new ArrayList<>();
			for (int i = 0; i < values.size(); i++) {
				if (!Objects.equals(values.get(i), afterValues.get(i))) {
					changed.add(definition.fields.get(i).name());
				}
			}
			return changed;
		}
	}

	/**
	 * Builder for {@link ChangedFields}
	 * @param <T> type of the object
	 */
	public static final class Builder<T> {
		private final List<Field<T>> fields = new ArrayList<>();

		private Builder() {
		}

		/**
		 * Add a field
		 * @param name field name, as reported by {@link Snapshot#diff} (typically the name of the
		 *        tool argument that changes it)
		 * @param getter getter for the field value
		 * @return fluent API
		 * @throws IllegalArgumentException if the name is blank or already used
		 */
		public Builder<T> field(String name, Function<? super T, ?> getter) {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("Field name must not be blank");
			}
			if (fields.stream().anyMatch(field -> field.name().equals(name))) {
				throw new IllegalArgumentException("Field '%s' is already defined".formatted(name));
			}
			fields.add(new Field<>(name, Objects.requireNonNull(getter, "Getter must not be null")));
			return this;
		}

		/**
		 * Build the definition
		 * @return definition
		 * @throws IllegalStateException if no field was added
		 */
		public ChangedFields<T> build() {
			if (fields.isEmpty()) {
				throw new IllegalStateException("At least one field must be defined");
			}
			return new ChangedFields<>(fields);
		}
	}

	/**
	 * A field
	 * @param <T> type of the object
	 * @param name field name
	 * @param getter getter for the field value
	 */
	private record Field<T>(String name, Function<? super T, ?> getter) {
	}
}
