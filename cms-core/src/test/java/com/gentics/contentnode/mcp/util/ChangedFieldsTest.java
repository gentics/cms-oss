package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link ChangedFields}. The use for pages is covered by
 * {@code UpdatePagePropertiesToolTest}.
 */
public class ChangedFieldsTest {
	/**
	 * Mutable test object, like the REST model objects
	 */
	private static class Item {
		String name;

		Integer size;

		List<String> tags;

		Item(String name, Integer size, List<String> tags) {
			this.name = name;
			this.size = size;
			this.tags = tags;
		}
	}

	private static final ChangedFields<Item> FIELDS = ChangedFields.<Item>builder().field("name", i -> i.name)
			.field("size", i -> i.size).field("tags", i -> i.tags).build();

	@Test
	public void testNames() {
		assertThat(FIELDS.names()).containsExactly("name", "size", "tags");
	}

	@Test
	public void testNothingChanged() {
		assertThat(FIELDS.snapshot(new Item("a", 1, List.of("x"))).diff(new Item("a", 1, List.of("x")))).isEmpty();
	}

	@Test
	public void testChangedFieldsInDeclarationOrder() {
		assertThat(FIELDS.snapshot(new Item("a", 1, List.of("x"))).diff(new Item("b", 1, List.of("y"))))
				.containsExactly("name", "tags");
	}

	@Test
	public void testNullTransitions() {
		ChangedFields.Snapshot<Item> withNulls = FIELDS.snapshot(new Item(null, null, null));

		assertThat(withNulls.diff(new Item(null, null, null))).isEmpty();
		assertThat(withNulls.diff(new Item("", 0, List.of()))).containsExactly("name", "size", "tags");
		assertThat(FIELDS.snapshot(new Item("a", 1, List.of())).diff(new Item(null, null, null)))
				.containsExactly("name", "size", "tags");
	}

	@Test
	public void testSnapshotIsTakenWhenCreated() {
		Item item = new Item("a", 1, null);
		ChangedFields.Snapshot<Item> before = FIELDS.snapshot(item);

		// changing the same instance after the snapshot is detected
		item.name = "b";

		assertThat(before.diff(item)).containsExactly("name");
	}

	@Test
	public void testSnapshotCanBeDiffedRepeatedly() {
		ChangedFields.Snapshot<Item> before = FIELDS.snapshot(new Item("a", 1, null));

		assertThat(before.diff(new Item("b", 1, null))).containsExactly("name");
		assertThat(before.diff(new Item("a", 2, null))).containsExactly("size");
	}

	@Test
	public void testNullObjectIsRejected() {
		assertThatThrownBy(() -> FIELDS.snapshot(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> FIELDS.snapshot(new Item("a", 1, null)).diff(null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testDuplicateFieldIsRejected() {
		assertThatThrownBy(() -> ChangedFields.<Item>builder().field("name", i -> i.name).field("name", i -> i.size))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'name'");
	}

	@Test
	public void testBlankFieldNameIsRejected() {
		assertThatThrownBy(() -> ChangedFields.<Item>builder().field(" ", i -> i.name))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ChangedFields.<Item>builder().field(null, i -> i.name))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testNullGetterIsRejected() {
		assertThatThrownBy(() -> ChangedFields.<Item>builder().field("name", null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testEmptyDefinitionIsRejected() {
		assertThatThrownBy(() -> ChangedFields.<Item>builder().build()).isInstanceOf(IllegalStateException.class);
	}
}
